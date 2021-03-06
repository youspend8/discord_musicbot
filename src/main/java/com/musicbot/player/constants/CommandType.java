package com.musicbot.player.constants;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum CommandType {
    JOIN("join"),
    PLAY("play"),
    SKIP("skip"),
    SEARCH("search"),
    NONE("none");

    private String value;

    public static CommandType findBy(String value) {
        return Arrays.stream(values())
                .filter(x -> value.equals(Command.PREFIX + x.value))
                .findFirst()
                .orElse(NONE);
    }
}
