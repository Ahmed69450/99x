package com.byd.voiceassistant.ai.router;

import java.util.*;

public class CarCommandCatalog {

    public static class CommandPrototype {
        private final String intentId;
        private final List<String> exemplarPhrases;

        public CommandPrototype(String intentId, List<String> exemplarPhrases) {
            this.intentId = intentId;
            this.exemplarPhrases = exemplarPhrases;
        }

        public String getIntentId() {
            return intentId;
        }

        public List<String> getExemplarPhrases() {
            return exemplarPhrases;
        }
    }

    public static final String INTENT_AC_ON = "ac_on";
    public static final String INTENT_AC_OFF = "ac_off";
    public static final String INTENT_AC_SET_TEMP = "ac_set_temp";
    public static final String INTENT_AC_TEMP_UP = "ac_temp_up";
    public static final String INTENT_AC_TEMP_DOWN = "ac_temp_down";
    public static final String INTENT_WINDOW_OPEN = "window_open";
    public static final String INTENT_WINDOW_CLOSE = "window_close";
    public static final String INTENT_MEDIA_PLAY = "media_play";
    public static final String INTENT_MEDIA_PAUSE = "media_pause";
    public static final String INTENT_MEDIA_NEXT = "media_next";
    public static final String INTENT_MEDIA_PREV = "media_previous";
    public static final String INTENT_SEAT_HEATING = "seat_heating";
    public static final String INTENT_FAN_SPEED = "fan_speed";

    public static List<CommandPrototype> getPrototypes() {
        List<CommandPrototype> list = new ArrayList<>();

        list.add(new CommandPrototype(INTENT_AC_ON, Arrays.asList(
            "turn on the air conditioner", "turn on ac", "enable air conditioner", "start the ac", "turn on climate control"
        )));

        list.add(new CommandPrototype(INTENT_AC_OFF, Arrays.asList(
            "turn off the air conditioner", "turn off ac", "disable air conditioner", "stop the ac", "shut down climate"
        )));

        list.add(new CommandPrototype(INTENT_AC_SET_TEMP, Arrays.asList(
            "set temperature to", "change temperature to", "adjust temperature to", "set ac to degrees", "make cabin temperature"
        )));

        list.add(new CommandPrototype(INTENT_AC_TEMP_UP, Arrays.asList(
            "increase temperature", "make it warmer", "turn up the heat", "raise cabin temperature", "a bit hotter"
        )));

        list.add(new CommandPrototype(INTENT_AC_TEMP_DOWN, Arrays.asList(
            "decrease temperature", "make it cooler", "turn down temperature", "lower cabin temperature", "a bit colder"
        )));

        list.add(new CommandPrototype(INTENT_WINDOW_OPEN, Arrays.asList(
            "open the windows", "roll down windows", "open front window", "open driver window", "ventilate windows"
        )));

        list.add(new CommandPrototype(INTENT_WINDOW_CLOSE, Arrays.asList(
            "close the windows", "roll up windows", "shut the windows", "close all windows"
        )));

        list.add(new CommandPrototype(INTENT_MEDIA_PLAY, Arrays.asList(
            "play music", "resume playback", "start playing music", "continue music"
        )));

        list.add(new CommandPrototype(INTENT_MEDIA_PAUSE, Arrays.asList(
            "pause music", "stop the music", "pause playback", "mute music"
        )));

        list.add(new CommandPrototype(INTENT_MEDIA_NEXT, Arrays.asList(
            "next track", "skip song", "next song", "play next"
        )));

        list.add(new CommandPrototype(INTENT_MEDIA_PREV, Arrays.asList(
            "previous track", "previous song", "go back song"
        )));

        list.add(new CommandPrototype(INTENT_SEAT_HEATING, Arrays.asList(
            "turn on driver seat heating", "driver seat heater", "warm up driver seat", "turn on seat heating", "passenger seat heating"
        )));

        list.add(new CommandPrototype(INTENT_FAN_SPEED, Arrays.asList(
            "set fan speed to", "adjust fan speed", "blower speed to", "fan speed"
        )));

        return list;
    }
}
