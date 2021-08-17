package com.layton.audio.record;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaFormat;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 本地语音文件播放工具类
 */
public class AmrAudioPlayer {

    private static final String TAG = "AmrAudioPlayer";

    private int bufferSize;
    private boolean startPlay = false;
    private AudioTrack mAudioTrack;

    private LinkedBlockingQueue<AmrData> channel = new LinkedBlockingQueue<>();

    private ExecutorService singleThreadPool = null;

    private final static AmrAudioPlayer instance = new AmrAudioPlayer();

    public static AmrAudioPlayer getInstance() {
        return instance;
    }

    private AmrAudioPlayer() {
    }

    public void play(AmrData data) {
        channel.offer(data);
        System.out.println("channel size:" + channel.size());
    }

    public void speak(byte[] datas) {
        try {
            mAudioTrack.write(datas, 0, datas.length);
            System.out.println("audio write and flush finished");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start() {
        if (startPlay) {
            return;
        }
        bufferSize = AudioTrack.getMinBufferSize(16000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 16000, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);

        startPlay = true;
        mAudioTrack.play();
        initAmrMediaCodecDecoder();
        singleThreadPool = Executors.newSingleThreadExecutor();
        singleThreadPool.execute(() -> {
            System.out.println("channel size:" + channel.size());
            while (channel.size() <= 0) {
                try {
                    Thread.sleep(500);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            decode();
        });
    }


    /**
     * 录音编码的mime
     */
    private static final String mime = "audio/amr-wb";

    private MediaCodec mediaCodecDecoder;

    private void initAmrMediaCodecDecoder() {
        try {
            MediaFormat mediaFormat = MediaFormat.createAudioFormat(mime, 16000, 1);

            mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bufferSize);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 14250);

            mediaCodecDecoder = MediaCodec.createDecoderByType(mime);
            mediaCodecDecoder.configure(mediaFormat, null, null, 0);
            MediaFormat outputFormat = mediaCodecDecoder.getOutputFormat(); // option B
            System.out.println("mime:" + outputFormat.getString(MediaFormat.KEY_MIME));
            mediaCodecDecoder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void decode() {
        while (startPlay && !Thread.currentThread().isInterrupted()) {
            System.out.println("start decode");
            try {
                //should not set timeoutUs to -1, because the data stream is not continuously.
                int inputBufferId = mediaCodecDecoder.dequeueInputBuffer(1000);
                System.out.println("inputBufferId:" + inputBufferId);
                if (inputBufferId >= 0) {
                    ByteBuffer inputBuffer = mediaCodecDecoder.getInputBuffer(inputBufferId);
                    // fill inputBuffer with valid data
                    AmrData amrData = channel.take();
                    if (amrData != null) {
                        inputBuffer.clear();
                        inputBuffer.put(amrData.data);
                        mediaCodecDecoder.queueInputBuffer(inputBufferId, 0, amrData.data.length,
                                System.nanoTime() / 1000, 0);
                        System.out.println("queue input buffer:" + inputBuffer.capacity());
                    }
                }
                MediaCodec.BufferInfo mInfo = new MediaCodec.BufferInfo();
                int outputBufferId;
                outputBufferId = mediaCodecDecoder.dequeueOutputBuffer(mInfo, 0);
                System.out.println("outputBufferId:" + outputBufferId);
                if (outputBufferId >= 0) {
                    ByteBuffer outputBuffer = mediaCodecDecoder.getOutputBuffer(outputBufferId);
                    MediaFormat bufferFormat = mediaCodecDecoder.getOutputFormat(outputBufferId); // option A
                    System.out.println("mime A:" + bufferFormat.getString(MediaFormat.KEY_MIME));
                    // bufferFormat is identical to outputFormat
                    // outputBuffer is ready to be processed or rendered.
                    byte[] temp = new byte[mInfo.size];
                    System.out.println("decode:" + temp.length);
                    outputBuffer.get(temp);
                    // clear outputBuffer
                    outputBuffer.clear();
                    mediaCodecDecoder.releaseOutputBuffer(outputBufferId, false);

                    speak(temp);
                } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // Subsequent data will conform to new format.
                    // Can ignore if using getOutputFormat(outputBufferId)
                    MediaFormat outputFormat = mediaCodecDecoder.getOutputFormat(); // option B
                    System.out.println("mime B:" + outputFormat.getString(MediaFormat.KEY_MIME));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 停止语音播放
     */
    public void stop() {
        try {
            startPlay = false;
            singleThreadPool.shutdownNow();
            mediaCodecDecoder.stop();
            mediaCodecDecoder.release();
            if ((mAudioTrack != null) && (mAudioTrack.getState() == AudioTrack.STATE_INITIALIZED)) {
                if (mAudioTrack.getPlayState() != AudioTrack.PLAYSTATE_STOPPED) {
                    mAudioTrack.pause();
                    mAudioTrack.flush();
                }
            }
            release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 释放AudioTrack
     */
    public void release() {
        if (mAudioTrack != null) {
            mAudioTrack.release();
            mAudioTrack = null;
        }
    }

    public static class AmrData {
        public byte[] data;

        public AmrData(byte[] data) {
            this.data = data;
        }
    }


}
