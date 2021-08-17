package com.layton.audio;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


import com.layton.audio.Handler.PlayDataListener;
import com.layton.audio.record.AmrAudioPlayer;
import com.layton.audio.record.EncodeAudio;

import java.util.Arrays;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private EncodeAudio encodeAudio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkAndRequestPermission();
        initAudio();
        setAudioVolumeWithOneHalf(this);
    }

    private void checkAndRequestPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
        }
    }

    private void initAudio() {
        encodeAudio = new EncodeAudio(this);
        encodeAudio.setAmrDataPreparedListener(new PlayDataListener());
        //启动则开启收听录音
        AmrAudioPlayer.getInstance().start();
    }

    /**
     * start the udp service
     */
    private void startUdpService() {

    }


    public void startRecord(View view) {
        encodeAudio.startRecord();

        AmrAudioPlayer.getInstance().start();
    }

    public void stopRecord(View view) {
        encodeAudio.stopRecord();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            System.out.println("grantResults:" + Arrays.toString(grantResults));
        }
    }

    /**
     * 若当前音量低于50%，则调到50%音量
     * @param context the activity context
     */
    public static void setAudioVolumeWithOneHalf(Context context) {
        AudioManager audioManager = ((AudioManager) Objects.requireNonNull(context.getSystemService(Context.AUDIO_SERVICE)));
        int largestVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);

        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
        if (currentVolume <= largestVolume / 2) {
            audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, largestVolume, AudioManager.FLAG_SHOW_UI);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRecord(null);
        AmrAudioPlayer.getInstance().stop();
    }
}