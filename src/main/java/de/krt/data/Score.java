package de.krt.data;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Locale;

public class Score {
    private final double ratio = 1.25;
    private String handle;
    private String season;
    private String mode;
    private String map;
    private String rank;
    private int time;
    private int raceTime;
    private int bestRaceTime;
    private final DecimalFormat df;
    public static int ERROR_VALUE = 999999999;

    public Score(String handle, String season, String mode, String map, String rank, int time, int raceTime, int bestRaceTime){
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setDecimalSeparator('.');
        df = new DecimalFormat("#.00", symbols);
        setHandle(handle);
        setSeason(season);
        setMode(mode);
        setMap(map);
        setRank(df.format(Double.parseDouble(rank)));
        setTime(time);
        setRaceTime(raceTime);
        setBestRaceTime(bestRaceTime);
    }

    public static int getMillies(String time){
        if (time.contains("-")){
            return ERROR_VALUE;
        }
        return LocalTime.parse(time).toSecondOfDay();
    }

    public static int getMilliesForRace(String time){
        String[] parts = time.split("\\.");
        String secondsPart = parts[0];
        StringBuilder milliPart = new StringBuilder(parts.length > 1 ? parts[1] : "0");
        while (milliPart.length() < 3) {
            milliPart.append("0");
        }
        long millis = Long.parseLong(milliPart.toString());

        String[] secondParts = secondsPart.split(":");
        long minutes = Long.parseLong(secondParts[0]);
        long seconds = Long.parseLong(secondParts.length > 1 ? secondParts[1] : "0");
        return (int) (minutes * 60 + seconds + millis / 1000.0);
    }

    public static String getTimeFormat(int time){
        Duration duration = Duration.ofSeconds(time);
        return String.format("%02d:%02d:%02d", duration.toHoursPart(), duration.toMinutesPart(), duration.toSecondsPart());
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getMap() {
        return map;
    }

    public void setMap(String map) {
        this.map = map;
    }

    public String getRank() {
        return df.format(Double.parseDouble(rank));
    }

    public void setRank(String rank) {
        this.rank = df.format(Double.parseDouble(rank));
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getRaceTime() { return raceTime; }

    public void setRaceTime(int raceTime) { this.raceTime = raceTime; }

    public int getBestRaceTime() { return bestRaceTime; }

    public void setBestRaceTime(int bestRaceTime) { this.bestRaceTime = bestRaceTime; }

    public int getRaceTimeRatio() { return Double.valueOf(getBestRaceTime() * ratio).intValue(); }

    public boolean isRaceTimeRationInInterval() { return getRaceTime() > 0 && (ratio * getBestRaceTime()) >= getRaceTime(); }

    /**
     * Add a new rank, weighted my time, to the currently hold rank
     * @param rank the rank
     * @param time the time
     */
    public void adjustRank(String rank, int time){
        double newRank = ((Double.parseDouble(getRank()) * getTime()) + (Double.parseDouble(rank) * time))
                / (getTime() + time);
        setRank(df.format(newRank));
        setTime(getTime() + time);
    }
}
