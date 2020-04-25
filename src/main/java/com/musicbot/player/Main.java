package com.musicbot.player;

import com.musicbot.player.audio.GuildMusicManager;
import com.musicbot.player.client.YoutubeClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;

import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class Main extends ListenerAdapter {
    private static final String TOKEN = "NTk5NTU5OTAwNTUyNDk1MTE0.XqPMDg.LsUueegyR2jkMhCTyNabJ3u1H10";
    public static void main(String[] args) throws LoginException {
        JDABuilder builder = new JDABuilder(AccountType.BOT)
                .addEventListeners(new Main())
                .setToken(TOKEN);

        builder.build();
    }

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        super.onMessageReceived(event);
        if (event.getAuthor().isBot()) {
            return;
        }
    }

    private final Map<Long, GuildMusicManager> musicManagers;
    private final AudioPlayerManager playerManager;

    private JsonNode searchList = null;

    private Main() {
        this.musicManagers = new HashMap<>();
        this.playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);
    }


    private synchronized GuildMusicManager getGuildAudioPlayer(Guild guild) {
        long guildId = Long.parseLong(guild.getId());
        GuildMusicManager musicManager = musicManagers.get(guildId);

        if (musicManager == null) {
            musicManager = new GuildMusicManager(playerManager);
            musicManagers.put(guildId, musicManager);
        }

        guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

        return musicManager;
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        String[] command = event.getMessage().getContentRaw().split(" ", 2);

        if ("!play".equals(command[0]) && command.length == 2) {
            loadAndPlay(event.getChannel(), command[1]);
        } else if ("!skip".equals(command[0])) {
            skipTrack(event.getChannel());
        } else if ("!search".equals(command[0]) && command.length == 2) {
            YoutubeClient client = new YoutubeClient();
            this.searchList = client.search(command[1]);

            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < searchList.size(); i++) {
                String title = searchList.get(i).get("snippet").get("title").asText();
                sb.append((i + 1) + ". " + title + "\n\n");
            }

            EmbedBuilder embedBuilder = new EmbedBuilder()
                    .setColor(Color.RED)
                    .setTitle("검색결과")
                    .setDescription(sb.toString());

            MessageEmbed embed = embedBuilder.build();

            event.getChannel().sendMessage(embed).queueAfter(0, TimeUnit.SECONDS, message -> {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        searchList = null;
                        message.delete().queue();
                    }
                }, 10000);
            });

        } else {
            try {
                int index = Integer.parseInt(command[0]);

                if (this.searchList != null) {
                    if (index >= 1 & index <= 10) {
                        JsonNode selectedNode = this.searchList.get(index - 1);

                        System.out.println("selectedNode :: " + selectedNode);
                        String url = "https://www.youtube.com/watch?v=" + selectedNode.get("id").get("videoId").asText();

                        System.out.println("url :: " + url);
                        loadAndPlay(event.getChannel(), url);
                    }
                }
            } catch (Exception e) {
                return;
            }
            return;
        }

        super.onGuildMessageReceived(event);
    }

    private void loadAndPlay(final TextChannel channel, final String trackUrl) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());

        playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                channel.sendMessage("Adding to queue " + track.getInfo().title).queue();

                play(channel.getGuild(), musicManager, track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                AudioTrack firstTrack = playlist.getSelectedTrack();

                if (firstTrack == null) {
                    firstTrack = playlist.getTracks().get(0);
                }

                channel.sendMessage("Adding to queue " + firstTrack.getInfo().title + " (first track of playlist " + playlist.getName() + ")").queue();

                play(channel.getGuild(), musicManager, firstTrack);
            }

            @Override
            public void noMatches() {
                channel.sendMessage("Nothing found by " + trackUrl).queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                channel.sendMessage("Could not play: " + exception.getMessage()).queue();
            }
        });
    }

    private void play(Guild guild, GuildMusicManager musicManager, AudioTrack track) {
        connectToFirstVoiceChannel(guild.getAudioManager());

        musicManager.scheduler.queue(track);
    }

    private void skipTrack(TextChannel channel) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
        musicManager.scheduler.nextTrack();

        channel.sendMessage("Skipped to next track.").queue();
    }

    private static void connectToFirstVoiceChannel(AudioManager audioManager) {
        if (!audioManager.isConnected() && !audioManager.isAttemptingToConnect()) {
            for (VoiceChannel voiceChannel : audioManager.getGuild().getVoiceChannels()) {
                audioManager.openAudioConnection(voiceChannel);
                break;
            }
        }
    }
}
