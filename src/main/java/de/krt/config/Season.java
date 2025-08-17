package de.krt.config;

public enum Season {
    PATCH_4_0_1(47),
    PATCH_4_2_0(49);

    private final int season;

    Season(int season) {
        this.season = season;
    }

    public int getSeason() {
        return season;
    }

}
