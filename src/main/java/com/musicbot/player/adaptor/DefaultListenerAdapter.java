package com.musicbot.player.adaptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.musicbot.player.audio.GuildMusicManager;
import com.musicbot.player.client.YoutubeClient;
import com.musicbot.player.constants.Command;
import com.musicbot.player.domain.dto.CommandDto;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;

import javax.annotation.Nonnull;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class DefaultListenerAdapter extends ListenerAdapter {
    private JsonNode searchList = null;

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        super.onMessageReceived(event);
        if (event.getAuthor().isBot()) {
            return;
        }
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        /**
         * 해당 Case 음악을 검색 후 선택중이라고 간주
         */
        if (this.searchList != null) {
            try {
                int index = Integer.parseInt(event.getMessage().getContentRaw());

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
        if (!event.getMessage().getContentRaw().startsWith(Command.PREFIX)) {
            return;
        }

        System.out.println("event :: " + event.getMessage());

        CommandDto commandDto = CommandDto.valueOf(event.getMessage().getContentRaw().split(" ", 2));

        System.out.println("commandDto.getCommandType() :: " + commandDto.getCommandType());
        switch (commandDto.getCommandType()) {
            case PLAY: {
                String format = "youtube-dl -x --audio-format mp3 --default-search ytsearch \"https://www.youtube.com/watch?v=%s\" -o %s.mp3";
                String exec = String.format(format, commandDto.getOption(), commandDto.getOption());
                System.out.println("exec :: " + exec);
                try {
                    Process process = Runtime.getRuntime().exec(exec);
                    BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    BufferedReader br2 = new BufferedReader(new InputStreamReader(process.getErrorStream()));

                    StringBuilder sb = new StringBuilder();
                    String line = "";
                    StringBuilder sb2 = new StringBuilder();
                    String line2 = "";

                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }

                    while ((line2 = br2.readLine()) != null) {
                        sb2.append(line2);
                    }

                    System.out.println(sb.toString());
                    System.out.println(sb2.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
//                loadAndPlay(event.getChannel(), commandDto.getOption());
            } break;
            case SKIP: {
//                skipTrack(event.getChannel());
            } break;
            case SEARCH: {
                YoutubeClient client = new YoutubeClient();
                this.searchList = client.search(commandDto.getOption());

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
            } break;
        }
        super.onGuildMessageReceived(event);
    }

    private final Map<Long, GuildMusicManager> musicManagers;
    private final AudioPlayerManager playerManager;

    public DefaultListenerAdapter() {
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

    private static void connectToFirstVoiceChannel(AudioManager audioManager) {
        if (!audioManager.isConnected() && !audioManager.isAttemptingToConnect()) {
            for (VoiceChannel voiceChannel : audioManager.getGuild().getVoiceChannels()) {
                audioManager.openAudioConnection(voiceChannel);
                break;
            }
        }
    }
    private void play(Guild guild, GuildMusicManager musicManager, AudioTrack track) {
        connectToFirstVoiceChannel(guild.getAudioManager());

        musicManager.scheduler.queue(track);
    }
}
