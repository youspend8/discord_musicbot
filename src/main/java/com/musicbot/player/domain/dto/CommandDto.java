package com.musicbot.player.domain.dto;

import com.musicbot.player.constants.CommandType;
import lombok.*;

@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CommandDto {
    private CommandType commandType;
    private String option;

    public static CommandDto valueOf(String[] array) {
        validate(array);
        return new CommandDto(CommandType.findBy(array[0]), array[1]);
    }

    private static void validate(String[] array) {
        if (array == null || array.length < 2) {
            throw new IllegalArgumentException();
        }
    }
}
