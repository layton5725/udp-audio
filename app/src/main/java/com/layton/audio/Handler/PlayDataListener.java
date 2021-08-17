package com.layton.audio.Handler;


import com.layton.audio.record.AmrAudioPlayer;

/**
 * play the audio data locally
 */
public class PlayDataListener implements AmrDataPreparedListener {

    @Override
    public void prepared(byte[] data) {
        AmrAudioPlayer.getInstance().play(new AmrAudioPlayer.AmrData(data));
    }
}
