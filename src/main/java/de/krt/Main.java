package de.krt;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

import de.krt.config.Maps;
import de.krt.config.Season;
import de.krt.data.Score;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class Main {
    private static final String URL = "https://robertsspaceindustries.com/api/leaderboards/getLeaderboard";

    public static void main(String[] args) {
        //setup
        List<Score> scores = new ArrayList<>();
        List<Score> aggregatedScores = new ArrayList<>();
        Map<String, List<String>> modesAndMAps = Maps.setupModeAndMaps();

        int SEASON_START = Season.PATCH_4_0_1.getSeason();
        int SEASON_END = Season.PATCH_4_0_1.getSeason();

        //TODO: include star marine after leaderboard-fix (ranking currently by time only)
        for (int i = SEASON_START; i == SEASON_END; i++) {
            System.out.println(STR."> Start processing season \{SEASON_START}, last season will be \{SEASON_END}.");
            String season = String.valueOf(i);
            for (String mode : modesAndMAps.keySet()) {
                System.out.println(STR.">> Process mode \{mode}.");
                List<Score> tempScores = new ArrayList<>();
                List<Score> tempAggregatedScores = new ArrayList<>();
                List<String> maps = modesAndMAps.get(mode);

                // query leaderboard data
                for (String map : maps) {
                    // get best race time for first place
                    String response = getLeaderbordData(season, mode, map, 1, false);
                    int bestTime = parseResponseForBestTime(response);
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
                    aggregateScoresByBestRankAndMap(tempScores, tempAggregatedScores);

                // gather all results
                scores.addAll(tempScores);
                aggregatedScores.addAll(tempAggregatedScores);
            }
        }
        writeToCSV(aggregatedScores, "lb_aggregate");
        writeToCSV(scores, "lb_full");
    }

    /**
     * Scores are aggregated and stored into a list of aggregated scores, weighted by time
     * @param scores the scores to aggregate
     * @param aggregateScores the list of aggregated scores
     */
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

    /**
     * Scores are stored into a list of scores, only the best score is kept
     * @param scores the scores to aggregate
     * @param aggregateScores the list of aggregated scores
     */
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

    /**
     * Scores are stored into a list of scores, the best score per map is kept
     * @param scores the scores to aggregate
     * @param aggregateScores the list of aggregated scores
     */
    private static void aggregateScoresByBestRankAndMap(List<Score> scores, List<Score> aggregateScores) {
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

    /**
     * Parse a data object  for best time an race time
     * @param response the data object
     * @param scores a list containing the parsed scores
     * @param mode a mode to add to the score
     * @param map a map to add to the score
     * @param season a season to add to the score
     * @param bestTime a best time to add to the score
     */
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

    /**
     * Parses a data object for the globally best time in milliseconds
     * @param response the data object
     * @return time in milliseconds
     */
    private static int parseResponseForBestTime(String response) {
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