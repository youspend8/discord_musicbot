package com.musicbot.player;

import com.musicbot.player.adaptor.DefaultListenerAdapter;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDABuilder;

import javax.security.auth.login.LoginException;

public class Main {
    private static final String TOKEN = "NTk5NTU5OTAwNTUyNDk1MTE0.XSm9qw.0fbhl6fq-GwxHfJ0RHzdqbrBpLY";

    public static void main(String[] args) throws LoginException, InterruptedException {
        JDABuilder builder = JDABuilder.createDefault(TOKEN)
                .addEventListeners(new DefaultListenerAdapter());

        builder.build().awaitReady();
    }

//    private void skipTrack(TextChannel channel) {
//        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
//        musicManager.scheduler.nextTrack();
//
//        channel.sendMessage("Skipped to next track.").queue();
//    }
}
