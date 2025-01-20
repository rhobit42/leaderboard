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

        //TODO: include racing
        //TODO: include star marine
        for (int i = 40; i <= 45; i++) {
            String season = String.valueOf(i);
            for (String mode : modesAndMAps.keySet()) {
                List<Score> tempScores = new ArrayList<>();
                List<Score> tempAggregatedScores = new ArrayList<>();
                List<String> maps = modesAndMAps.get(mode);
                for (String map : maps) {
                    String response = getLeaderbordData(season, mode, map);
                    parseResponse(response, tempScores, mode, map, season);
                }
                aggregateScores(tempScores, tempAggregatedScores, mode + "-ALL");
                scores.addAll(tempScores);
                aggregatedScores.addAll(tempAggregatedScores);
            }
        }
        writeToCSV(aggregatedScores, "lb_aggregate");
        writeToCSV(scores, "lb_full");
    }

    private static void aggregateScores(List<Score> scores, List<Score> aggregateScores, String modeAggregate) {
        Map<String, Score> aggregation = new HashMap<>();
        for (Score score : scores){
            if(!score.isRacing() && aggregation.containsKey(score.getHandle())){
                Score existingScore = aggregation.get(score.getHandle());
                existingScore.adjustRank(score.getRank(), score.getTime());
            }
            else {
                Score newScore = new Score(score.getHandle(), score.getSeason(), score.getMode(), modeAggregate, score.getRank(), score.getTime());
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
            int time = Score.getMillies(entry.getString("flight_time"));
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

        /**
        //Racing Maps
        //Classic Race
        modesAndMap.put("CR", List.of("NHS-OLD-VANDERVAL","NHS-RIKKORD", "NHS-DEFFORD-LINK"));
        //Grav Race
        modesAndMap.put("GR", List.of("SNAKE-PIT","SNAKE-PIT-REVERSE", "CLIO-ISLANDS", "SHIFTING-SANDS", "RIVERS-EDGE"));
        */
        return modesAndMap;
    }

    private static String getLeaderbordData(String season, String mode, String map) {
        String response = "";
        try {
            URL url = new URI(URL).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // The payload that you want to send
            String payload = "{" +
                    "\"mode\": \"" + mode + "\", " +
                    "\"map\": \"" + map + "\", " +
                    "\"sort\": \"rank_score\", " +
                    "\"org\": \"KRT\", " +
                    "\"type\": \"Account\", " +
                    "\"season\": \"" + season + "\", " +
                    "\"page\": \"1\", " +
                    "\"type\" : \"Account\"" +
                "}\n";

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

}