package com.layton.audio.Handler;


import com.layton.audio.record.AmrAudioPlayer;

/**
 * handler the audio data from server.
 */
public class ServerAudioExecute {

    private final AmrAudioPlayer appVoice = AmrAudioPlayer.getInstance();

    public Void execute(Object audioMessage) {
        // get audio bytes from audioMessage object
        byte[] data = new byte[378];
        appVoice.play(new AmrAudioPlayer.AmrData(data));
        return null;
    }
}
