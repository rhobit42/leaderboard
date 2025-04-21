package de.krt;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class Main {
    private static final String URL = "https://robertsspaceindustries.com/api/leaderboards/getLeaderboard";

    // 47 = 4.0.1
    private static final int SEASON_START = 47;
    private static final int SEASON_END = 47;

    public static void main(String[] args) {
        //setup
        List<Score> scores = new ArrayList<>();
        List<Score> aggregatedScores = new ArrayList<>();
        Map<String, List<String>> modesAndMAps = setupModeAndMaps();

        //TODO: include star marine after leaderboard-fix (ranking currently by time only)
        for (int i = SEASON_START; i == SEASON_END; i++) {
            String season = String.valueOf(i);
            for (String mode : modesAndMAps.keySet()) {
                List<Score> tempScores = new ArrayList<>();
                List<Score> tempAggregatedScores = new ArrayList<>();
                List<String> maps = modesAndMAps.get(mode);

                // query leaderboard data
                for (String map : maps) {
                    // get best race time for first place
                    String response = getLeaderbordData(season, mode, map, 1, false);
                    int bestTime = parseResponseForFirst(response);
                    //get individual leaderboard-data
                    response = getLeaderbordData(season, mode, map, 25, true);
                    parseResponse(response, tempScores, mode, map, season, bestTime);

                }

                // aggregate scores according to mode characteristics
                if (mode.equals("SB") || mode.equals("DL"))
                    aggregateScoresByTimeWeightedMean(tempScores, tempAggregatedScores);
                if (mode.equals("VS") || mode.equals("PS"))
                    aggregateScoresByBestRank(tempScores, tempAggregatedScores);
                if (mode.equals("CR") || mode.equals("GR"))
                    aggregateScoresByBestRankAndRatio(tempScores, tempAggregatedScores);

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
                Score newScore = new Score(score.getHandle(), score.getSeason(), score.getMode(), STR."\{score.getMode()}-ALL", score.getRank(), score.getTime(), score.getRaceTime(), score.getBestRaceTime());
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
                    Score newScore = new Score(score.getHandle(), score.getSeason(), score.getMode(), score.getMap(), score.getRank(), score.getTime(),score.getRaceTime(), score.getBestRaceTime());
                    aggregation.put(score.getHandle(), newScore);
                }
            }
            else {
                Score newScore = new Score(score.getHandle(), score.getSeason(), score.getMode(), score.getMap(), score.getRank(), score.getTime(), score.getRaceTime(), score.getBestRaceTime());
                aggregation.put(score.getHandle(), newScore);
            }
        }
        for(String handle : aggregation.keySet()) {
            aggregateScores.add(aggregation.get(handle));
        }
    }

    private static void aggregateScoresByBestRankAndRatio(List<Score> scores, List<Score> aggregateScores) {
        Map<String, Score> aggregation = new HashMap<>();
        for (Score score : scores){
            if(aggregation.containsKey(score.getHandle().concat(score.getMap()))){
                Score existingScore = aggregation.get(score.getHandle().concat(score.getMap()));
                if (Double.parseDouble(existingScore.getRank()) > Double.parseDouble(score.getRank())){
                    Score newScore = new Score(score.getHandle(), score.getSeason(), score.getMode(), score.getMap(), score.getRank(), score.getTime(), score.getRaceTime(), score.getBestRaceTime());
                    aggregation.put(score.getHandle().concat(score.getMap()), newScore);
                }
            }
            else {
                Score newScore = new Score(score.getHandle(), score.getSeason(), score.getMode(), score.getMap(), score.getRank(), score.getTime(), score.getRaceTime(), score.getBestRaceTime());
                aggregation.put(score.getHandle().concat(score.getMap()), newScore);
            }
        }
        for(String handle : aggregation.keySet()) {
            aggregateScores.add(aggregation.get(handle));
        }
    }

    private static void parseResponse(String response, List<Score> scores, String mode, String map, String season, int bestTime) {
        JSONObject json = new JSONObject(response);
        JSONObject data = json.getJSONObject("data");
        JSONArray resultSet = data.getJSONArray("resultset");

        for (int i = 0; i < resultSet.length(); i++) {
            JSONObject entry = resultSet.getJSONObject(i);
            String handle = entry.getString("nickname");
            String rank = entry.getString("rank");
            int time = Score.getMillies(getJsonValueAsString(entry, "flight_time"));
            int raceTime = Score.getMilliesForRace(getJsonValueAsString(entry, "best_race"));
            scores.add(new Score(handle, season, mode, map, rank, time, raceTime, bestTime));
        }
    }

    private static int parseResponseForFirst(String response) {
        int time = 0;
        JSONObject json = new JSONObject(response);
        JSONObject data = json.getJSONObject("data");
        JSONArray resultSet = data.getJSONArray("resultset");

        for (int i = 0; i < resultSet.length(); i++) {
            JSONObject entry = resultSet.getJSONObject(i);
            time = Score.getMilliesForRace(getJsonValueAsString(entry, "best_race"));
        }
        return time;
    }

    private static void writeToCSV(List<Score> scores, String filename) {
        try {
            FileWriter csvWriter = new FileWriter(filename + ".csv");
            csvWriter.append("season");
            csvWriter.append(";");
            csvWriter.append("mode");
            csvWriter.append(";");
            csvWriter.append("map");
            csvWriter.append(";");
            csvWriter.append("handle");
            csvWriter.append(";");
            csvWriter.append("rank");
            csvWriter.append(";");
            csvWriter.append("time");
            csvWriter.append(";");
            csvWriter.append("raceTime");
            csvWriter.append(";");
            csvWriter.append("bestRaceTime");
            csvWriter.append(";");
            csvWriter.append("ratio");
            csvWriter.append(";");
            csvWriter.append("raceTimeInRatio");
            csvWriter.append("\n");

            for (Score score : scores) {
                csvWriter.append(score.getSeason());
                csvWriter.append(";");
                csvWriter.append(score.getMode());
                csvWriter.append(";");
                csvWriter.append(score.getMap());
                csvWriter.append(";");
                csvWriter.append(score.getHandle());
                csvWriter.append(";");
                csvWriter.append(score.getRank().replace(".",","));
                csvWriter.append(";");
                csvWriter.append(Score.getTimeFormat(score.getTime()));
                csvWriter.append(";");
                csvWriter.append(Score.getTimeFormat(score.getRaceTime()));
                csvWriter.append(";");
                csvWriter.append(Score.getTimeFormat(score.getBestRaceTime()));
                csvWriter.append(";");
                csvWriter.append(Score.getTimeFormat(score.getRaceTimeRatio()));
                csvWriter.append(";");
                csvWriter.append(Boolean.valueOf(score.isRaceTimeRationInInterval()).toString());
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
        List<String> maps = List.of(
                "BROKEN-MOON"
                , "DYING-STAR"
                , "KAREAH"
                , "JERICHO-STATION"
                , "ARENA");
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
        modesAndMap.put("CR", List.of(
                "NHS-OLD-VANDERVAL"
                , "NHS-RIKKORD"
                , "NHS-DEFFORD-LINK"
                , "NHS-HALLORAN"
                , "CAPLAN-CIRCUIT"
                , "DUNLOW-DERBY"
                , "ICEBREAKER"
                , "LORVILLE-OUTSKIRTS"
                , "MINERS-LAMENT"
                , "THE-SKY-SCRAPER"
                , "YADAR-VALLEY"
                , "PYRO-JUMP"
                , "SNAKE-PIT"
        ));
        //Grav Race
        modesAndMap.put("GR", List.of(
                "SNAKE-PIT"
                , "SNAKE-PIT-REVERSE"
                , "CLIO-ISLANDS"
                , "SHIFTING-SANDS"
                , "RIVERS-EDGE"
        ));

        return modesAndMap;
    }

    private static String getLeaderbordData(String season, String mode, String map, int topX, boolean krtOnly) {
        String response = "";
        try {
            HttpURLConnection conn = getHttpURLConnection(season, mode, map, topX, krtOnly);

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

    private static HttpURLConnection getHttpURLConnection(String season, String mode, String map, int topX, boolean krtOnly) throws URISyntaxException, IOException {
        URL url = new URI(URL).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // The payload that you want to send
        String payload;
        if(krtOnly)
            payload = getLeaderboarURLForKRT(season, mode, map, topX);
        else
            payload = getLeaderboarURL(season, mode, map, topX);

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = payload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        return conn;
    }

    private static String getLeaderboarURLForKRT(String season, String mode, String map, int topX) {
        return STR."{\"mode\": \"\{mode}\", \"map\": \"\{map}\", \"sort\": \"rank_score\", \"org\": \"KRT\", \"type\": \"Account\", \"season\": \"\{season}\", \"page\": \"1\", \"pagesize\": \"\{topX}\"}\n";
    }

    private static String getLeaderboarURL(String season, String mode, String map, int topX) {
        return STR."{\"mode\": \"\{mode}\", \"map\": \"\{map}\", \"sort\": \"rank_score\", \"type\": \"Account\", \"season\": \"\{season}\", \"page\": \"1\", \"pagesize\": \"\{topX}\"}\n";
    }

    private static String getJsonValueAsString(JSONObject json, String key) {
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