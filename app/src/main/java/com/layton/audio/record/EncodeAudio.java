package com.layton.audio.record;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;


import com.layton.audio.Handler.AmrDataPreparedListener;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

/**
 * 音频录制及编码
 */
public class EncodeAudio {

    /**
     * 采样率，amr-wb 默认16kHz
     */
    private final int sampleRate = 16000;

    /**
     * 单通道，amr单通道
     */
    private final int channelConfig = AudioFormat.CHANNEL_IN_MONO;

    /**
     * 设置采样数据格式，默认16比特PCM
     */
    private final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

    /**
     * 用于保存录音文件
     */
    private FileOutputStream fos;
    public static int FRAME = 10;
    public static int ONE_AMR_FRAME = 37;
    private byte[] sendBytes = new byte[ONE_AMR_FRAME * FRAME];
    private int count;

    public static final String path = Environment.getExternalStorageDirectory() + "/amraudio/";
    private static final boolean DEBUG = false;

    private AudioRecord mRecorder;

    /**
     * 是否开始录制音频
     */
    private boolean isRecording;

    public boolean isRecording() {
        return isRecording;
    }

    //缓存区
    private byte[] buffer;
    private int bufferSize = 0;

    /**
     * 录音编码的mime
     */
    private static final String mime = "audio/amr-wb";

    /**
     * 编码的key bit rate;
     */
    private final int rate = 14250;

    private MediaCodec mAudioCodec;

    private Context context;

    private AmrDataPreparedListener amrDataPreparedListener;

    public void setAmrDataPreparedListener(AmrDataPreparedListener amrDataPreparedListener) {
        this.amrDataPreparedListener = amrDataPreparedListener;
    }

    public EncodeAudio(Context context) {
        this.context = context;
    }

    private void initRecorder() {
        //音频录制实例化和录制过程中需要用到的数据
        bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
        buffer = new byte[bufferSize];
        Log.e("audio", "buffer length: " + buffer.length);
        //实例化AudioRecord MediaRecorder.AudioSource.VOICE_COMMUNICATION 消除回声
        mRecorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, sampleRate, channelConfig,
                audioFormat, bufferSize);
    }

    /**
     * 开始录制
     */
    public void startRecord() {
        if (isRecording) {
            return;
        }
        if (DEBUG) {
            try {
                //创建文件路径
                File dir = new File(context.getExternalFilesDir(null).getPath() + "/amraudio");
                System.out.println(dir);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                File file = new File(dir + "/222.amr");
                if (file.exists()) {
                    file.delete();
                    file.createNewFile();
                }
                fos = new FileOutputStream(file);
                fos.write("#!AMR-WB\n".getBytes());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        initRecorder();
        mRecorder.startRecording();
        initMediaCodec();
        isRecording = true;
        aacEncoderThread = new Thread(aacEncoderRunnable);
        aacEncoderThread.start();
    }

    /**
     * 相对于上面的音频录制，初始化一个编码器的实例
     */
    public void initMediaCodec() {
        try {
            MediaFormat format = MediaFormat.createAudioFormat(mime, sampleRate, 1);
            format.setInteger(MediaFormat.KEY_BIT_RATE, rate);
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 100 * 1024);
            mAudioCodec = MediaCodec.createEncoderByType(mime);
            //设置为编码器
            mAudioCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

            //同样，在设置录音开始的时候，也要设置编码开始
            mAudioCodec.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final Runnable aacEncoderRunnable = new Runnable() {
        @Override
        public void run() {

            while (!aacEncoderThread.isInterrupted() && isRecording) {
                //音频数据编码
                try {
                    ByteBuffer[] inputBuffers = mAudioCodec.getInputBuffers();
                    ByteBuffer[] outputBuffers = mAudioCodec.getOutputBuffers();
                    int index = mAudioCodec.dequeueInputBuffer(-1);
                    if (index >= 0) {
//                        final ByteBuffer buffer = mAudioCodec.getInputBuffer(index);
                        final ByteBuffer buffer = inputBuffers[index];
                        buffer.clear();
                        int length = mRecorder.read(buffer, bufferSize);
                        if (length > 0) {
                            mAudioCodec.queueInputBuffer(index, 0, length, System.nanoTime() / 1000, 0);
                        }
                    }
                    MediaCodec.BufferInfo mInfo = new MediaCodec.BufferInfo();
                    int outIndex;
                    //每次取出的时候，把所有加工好的都循环取出来
                    do {
                        outIndex = mAudioCodec.dequeueOutputBuffer(mInfo, 0);
                        if (outIndex >= 0) {
//                            ByteBuffer buffer = mAudioCodec.getOutputBuffer(outIndex);
                            ByteBuffer buffer = outputBuffers[outIndex];
                            buffer.position(mInfo.offset);
                            buffer.limit(mInfo.offset + mInfo.size);

                            byte[] temp = new byte[mInfo.size];
                            buffer.get(temp, 0, mInfo.size);
                            if (DEBUG && fos != null) {
                                fos.write(temp);
                            }
                            if (temp.length == 74) {
                                ONE_AMR_FRAME = 74;
                                FRAME = 5;
                            } else if(temp.length > 74) {
                                ONE_AMR_FRAME = temp.length;
                                FRAME = 370 / ONE_AMR_FRAME;
                            }
                            System.out.println(temp.length);
                            mAudioCodec.releaseOutputBuffer(outIndex, false);
                            udpSend(temp);
                        } else if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                            //TODO something
                        } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            //TODO something
                        }
                    } while (outIndex >= 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Log.i("EncodeAudio", "amr Encoder Thread ended ......");
        }
    };
    private Thread aacEncoderThread = null;

    private void udpSend(byte[] temp) {
        if (count > FRAME - 1) {
            count = 0;
            if (amrDataPreparedListener != null) {
                amrDataPreparedListener.prepared(sendBytes);
            }
            sendBytes = null;
            sendBytes = new byte[ONE_AMR_FRAME * FRAME];
        }
        System.arraycopy(temp, 0, sendBytes, ONE_AMR_FRAME * count, temp.length);
        count++;
    }

    /**
     * 中止循环并结束录制
     */
    public void stopRecord() {
        if (!isRecording) {
            return;
        }
        isRecording = false;
        mRecorder.stop();
        mRecorder.release();
        stopEncode();
        try {
            aacEncoderThread.interrupt();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 编码停止，发送编码结束的标志，循环结束后，停止并释放编码器
     */
    private void stopEncode() {
        if (mAudioCodec != null) {
            mAudioCodec.stop();
            mAudioCodec.release();
            mAudioCodec = null;
        }

    }

}
