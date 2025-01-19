package de.krt;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class Score {
    private String handle;
    private String season;
    private String mode;
    private String map;
    private String rank;
    private long time;

    public Score(String handle, String season, String mode, String map, String rank, long time){
        this.handle = handle;
        this.season = season;
        this.mode = mode;
        this.map = map;
        this.rank = rank;
        this.time = time;
    }

    public static long getMillies(String time){
        return LocalTime.parse(time).toSecondOfDay();
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

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
