package com.musicbot.player.audio;

import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import net.dv8tion.jda.api.audio.AudioSendHandler;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

public class AudioHandler extends AudioEventAdapter implements AudioSendHandler {
    public boolean canProvide() {
        return false;
    }

    @Nullable
    public ByteBuffer provide20MsAudio() {
        return null;
    }
}
