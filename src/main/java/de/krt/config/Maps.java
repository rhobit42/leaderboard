package de.krt.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Maps {
    public static Map<String, List<String>> setupModeAndMaps() {
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
}
