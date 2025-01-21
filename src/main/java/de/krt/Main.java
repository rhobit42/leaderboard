package de.krt;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class Main {
    private static final String URL = "https://robertsspaceindustries.com/api/leaderboards/getLeaderboard";

    public static void main(String[] args) {
        //setup
        List<Score> scores = new ArrayList<>();
        List<Score> aggregatedScores = new ArrayList<>();
        Map<String, List<String>> modesAndMAps = setupModeAndMaps();

        //TODO: include star marine after leaderboard-fix (ranking currently by time only)
        // season 36 = SC Alpha 3.16 - start of ranking per map
        for (int i = 36; i <= 45; i++) {
            String season = String.valueOf(i);
            for (String mode : modesAndMAps.keySet()) {
                List<Score> tempScores = new ArrayList<>();
                List<Score> tempAggregatedScores = new ArrayList<>();
                List<String> maps = modesAndMAps.get(mode);

                // query leaderboard data
                for (String map : maps) {
                    String response = getLeaderbordData(season, mode, map);
                    parseResponse(response, tempScores, mode, map, season);
                }

                // aggregate scores according to mode characteristics
                if (mode.equals("SB") || mode.equals("DL"))
                    aggregateScoresByTimeWeightedMean(tempScores, tempAggregatedScores);
                if (mode.equals("VS") || mode.equals("PS") || mode.equals("CR") || mode.equals("GR"))
                    aggregateScoresByBestRank(tempScores, tempAggregatedScores);

                // gather all results
                scores.addAll(tempScores);
                aggregatedScores.addAll(tempAggregatedScores);
            }
        }
        writeToCSV(aggregatedScores, "lb_aggregate");
        writeToCSV(scores, "lb_full");
    }

    private static void aggregateScoresByTimeWeightedMean(List<Score> scores, List<Score> aggregateScores) {
        Map<String, Score> aggregation = new HashMap<>();
        for (Score score : scores){
            if (score.getTime() == Score.ERROR_VALUE){
                score.setMode("ERROR");
                aggregateScores.add(score);
            }
            else if(aggregation.containsKey(score.getHandle())){
                Score existingScore = aggregation.get(score.getHandle());
                existingScore.adjustRank(score.getRank(), score.getTime());
            }
            else {
                Score newScore = new Score(score.getHandle(), score.getSeason(), score.getMode(), STR."\{score.getMode()}-ALL", score.getRank(), score.getTime());
                aggregation.put(score.getHandle(), newScore);
            }
        }
        for(String handle : aggregation.keySet()) {
            aggregateScores.add(aggregation.get(handle));
        }
    }

    private static void aggregateScoresByBestRank(List<Score> scores, List<Score> aggregateScores) {
        Map<String, Score> aggregation = new HashMap<>();
        for (Score score : scores){
            if(aggregation.containsKey(score.getHandle())){
                Score existingScore = aggregation.get(score.getHandle());
                if (Double.parseDouble(existingScore.getRank()) > Double.parseDouble(score.getRank())){
                    Score newScore = new Score(score.getHandle(), score.getSeason(), score.getMode(), score.getMap(), score.getRank(), score.getTime());
                    aggregation.put(score.getHandle(), newScore);
                }
            }
            else {
                Score newScore = new Score(score.getHandle(), score.getSeason(), score.getMode(), score.getMap(), score.getRank(), score.getTime());
                aggregation.put(score.getHandle(), newScore);
            }
        }
        for(String handle : aggregation.keySet()) {
            aggregateScores.add(aggregation.get(handle));
        }
    }

    private static void parseResponse(String response, List<Score> scores, String mode, String map, String season) {
        JSONObject json = new JSONObject(response);
        JSONObject data = json.getJSONObject("data");
        JSONArray resultSet = data.getJSONArray("resultset");

        for (int i = 0; i < resultSet.length(); i++) {
            JSONObject entry = resultSet.getJSONObject(i);
            String handle = entry.getString("nickname");
            String rank = entry.getString("rank");
            int time = Score.getMillies(getJsonAsString(entry, "flight_time"));
            scores.add(new Score(handle, season, mode, map, rank, time));
        }
    }

    private static void writeToCSV(List<Score> scores, String filename) {
        try {
            FileWriter csvWriter = new FileWriter(filename + ".csv");
            csvWriter.append("season");
            csvWriter.append(",");
            csvWriter.append("mode");
            csvWriter.append(",");
            csvWriter.append("map");
            csvWriter.append(",");
            csvWriter.append("handle");
            csvWriter.append(",");
            csvWriter.append("rank");
            csvWriter.append(",");
            csvWriter.append("time");
            csvWriter.append("\n");

            for (Score score : scores) {
                csvWriter.append(score.getSeason());
                csvWriter.append(",");
                csvWriter.append(score.getMode());
                csvWriter.append(",");
                csvWriter.append(score.getMap());
                csvWriter.append(",");
                csvWriter.append(score.getHandle());
                csvWriter.append(",");
                csvWriter.append(score.getRank().replace(".",","));
                csvWriter.append(",");
                csvWriter.append(Score.getTimeFormat(score.getTime()));
                csvWriter.append("\n");
            }

            // Flush and close writer
            csvWriter.flush();
            csvWriter.close();

        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    private static Map<String,List<String>> setupModeAndMaps() {
        Map<String,List<String>> modesAndMap = new HashMap<>();

        //Arena Commander Maps
        List<String> maps = List.of("BROKEN-MOON", "DYING-STAR", "KAREAH", "JERICHO-STATION", "ARENA");
        //DUEL
        modesAndMap.put("DL", maps);
        //Squadron Battle
        modesAndMap.put("SB", maps);
        //Vanduul Swarm
        modesAndMap.put("VS", maps);
        //Pirate Swarm
        modesAndMap.put("PS", maps);

        //Racing Maps
        //Classic Race
        modesAndMap.put("CR", List.of("NHS-OLD-VANDERVAL","NHS-RIKKORD", "NHS-DEFFORD-LINK"));
        //Grav Race
        modesAndMap.put("GR", List.of("SNAKE-PIT","SNAKE-PIT-REVERSE", "CLIO-ISLANDS", "SHIFTING-SANDS", "RIVERS-EDGE"));

        return modesAndMap;
    }

    private static String getLeaderbordData(String season, String mode, String map) {
        String response = "";
        try {
            URL url = new URI(URL).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // The payload that you want to send
            String payload = STR."{\"mode\": \"\{mode}\", \"map\": \"\{map}\", \"sort\": \"rank_score\", \"org\": \"KRT\", \"type\": \"Account\", \"season\": \"\{season}\", \"page\": \"1\", \"pagesize\": \"100\", \"type\" : \"Account\"}\n";

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = payload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Read the response
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            response = content.toString();

            // Close connections
            in.close();
            conn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    private static String getJsonAsString(JSONObject json, String key) {
        Object value = json.get(key);
        if (value instanceof Integer) {
            return Integer.toString((Integer) value);
        } else if (value instanceof String) {
            return (String) value;
        } else {
            throw new IllegalArgumentException("Invalid type");
        }
    }

}