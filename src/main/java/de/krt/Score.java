package de.krt;

import java.time.Duration;
import java.time.LocalTime;

public class Score {
    private String handle;
    private String season;
    private String mode;
    private String map;
    private String rank;
    private int time;

    public Score(String handle, String season, String mode, String map, String rank, int time){
        this.handle = handle;
        this.season = season;
        this.mode = mode;
        this.map = map;
        this.rank = rank;
        this.time = time;
    }

    public static int getMillies(String time){
        return LocalTime.parse(time).toSecondOfDay();
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
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public void adjustRank(String rank, int time){
        double newRank = ((Double.parseDouble(getRank()) * getTime()) + (Double.parseDouble(rank) * time))
                / (getTime() + time);
        setRank(String.valueOf(newRank));
        setTime(getTime() + time);
    }
}
