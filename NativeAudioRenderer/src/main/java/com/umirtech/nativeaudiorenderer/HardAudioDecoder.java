package com.umirtech.nativeaudiorenderer;

import static java.lang.Thread.sleep;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class HardAudioDecoder {
    private MediaExtractor audioExtractor;
    private MediaCodec audiodecoder;
    private boolean isRunning;
    private Thread thread;
    private final Lock mutex = new ReentrantLock(true);
    private long timeStamp = 0;
    private float volume = 1;
    private long currentSystemTime,seekTime,startTimeInMs;
    private NativeAudioRenderer nativeAudioRenderer;
    private final boolean enableNativeRenderer = true;


    int channelCount,sampleRate;
    public boolean init(String path)
    {
        audioExtractor = new MediaExtractor();
        try {
            audioExtractor.setDataSource(path);
            int audioTrack = -1;

            int trackCount1 = audioExtractor.getTrackCount();
            for (int i = 0; i < trackCount1; i++) {
                MediaFormat trackFormat1 = audioExtractor.getTrackFormat(i);
                String string = trackFormat1.getString(MediaFormat.KEY_MIME);
                if (string.startsWith("audio/")) {
                    audioTrack = i;
                    Log.e("Oboe Native", "AudioTrack" + audioTrack);
                }
            }

            if (audioTrack != -1)
            {
                audioExtractor.selectTrack(audioTrack);
                MediaFormat mf = audioExtractor.getTrackFormat(audioTrack);
                audiodecoder = MediaCodec.createDecoderByType(mf.getString(MediaFormat.KEY_MIME));
                audiodecoder.configure(mf, null, null, 0);

                sampleRate =  mf.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                channelCount = mf.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

                if (enableNativeRenderer)
                {
                    nativeAudioRenderer = new NativeAudioRenderer();
                    nativeAudioRenderer.createAudioRenderer(channelCount,sampleRate,NativeAudioRenderer.AUDIO_FORMAT_PCM_16bit);
                }

                audiodecoder.start();

                return true;
            }else {
                return false;
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;

    }


    public void setVolume(float volume)
    {
        if (enableNativeRenderer)
        {
            nativeAudioRenderer.setVolume(volume);
        }
        this.volume = volume;
    }


    private void startSync() {
       thread =  new Thread(new Runnable() {
            @Override
            public void run() {
                mutex.lock();

                long delay = 0;
                isRunning = true;
                while (isRunning)
                {
                    int inputBufferindex = audiodecoder.dequeueInputBuffer(0);
                    if (inputBufferindex > 0) {
                        ByteBuffer inputBuffer = audiodecoder.getInputBuffer(inputBufferindex);
                        int sampleSize = audioExtractor.readSampleData(inputBuffer, 0);
                        timeStamp = Math.round(audioExtractor.getSampleTime() / 1000f);


                        if (sampleSize > 0) {
                           // Log.d("Canned Editor", "sample size");
                            audiodecoder.queueInputBuffer(inputBufferindex, 0, sampleSize, audioExtractor.getSampleTime(), 0);
                            audioExtractor.advance();
                        }
                        if (sampleSize == -1)
                        {
                            Log.e("Audio Stopped","yes");
                            stop();
                            isRunning = false;
                            mutex.unlock();
                            return;

                        }



                        try {
                            currentSystemTime = System.currentTimeMillis() + seekTime - startTimeInMs;
                            if (timeStamp > currentSystemTime)
                            {
                                //  Log.e("SystemTime Delay", String.valueOf(delay));
                                delay = timeStamp - currentSystemTime;
                                if (delay > 0)
                                {
                                    sleep(delay);
                                }
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }


                    }

                    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                    int outputBufferIndex = audiodecoder.dequeueOutputBuffer(bufferInfo, 0);

                    switch (outputBufferIndex) {
                        case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                            break;
                        case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                            sampleRate =  audiodecoder.getOutputFormat().getInteger(MediaFormat.KEY_SAMPLE_RATE);
                            channelCount = audiodecoder.getOutputFormat().getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                            break;
                        case MediaCodec.INFO_TRY_AGAIN_LATER:
                            break;
                        default:
                            ByteBuffer outputBuffer = audiodecoder.getOutputBuffer(outputBufferIndex);
                            int shortBufferSize = bufferInfo.size-bufferInfo.offset;
                            shortBufferSize/=2;
                            short[] audioData = new short[shortBufferSize];
                            outputBuffer.asShortBuffer().get(audioData);
                          //  Log.e("Sample Rate ", String.valueOf(sampleRate));

                            if (enableNativeRenderer)
                            {
                                nativeAudioRenderer.writeData(audioData,audioData.length);
                            }
                            outputBuffer.clear();
                            audiodecoder.releaseOutputBuffer(outputBufferIndex, false);
                    }

                }
                audiodecoder.flush();
                mutex.unlock();
            }
        });
       thread.start();
    }

    public  void start()
    {
        if (enableNativeRenderer)
        {
            nativeAudioRenderer.startRenderer();
            startSync();
        }


    }


    public boolean isRunning() {
        return isRunning;
    }

    public void stop()
    {
        if (enableNativeRenderer)
        {
            stopSync();
            nativeAudioRenderer.stopRenderer();
        }

    }


    public void stopSync()
    {
        if (thread != null)
        {
            isRunning = false;
        }
    }


    public void seekTo(long ms)
    {
        startTimeInMs = System.currentTimeMillis();

        audioExtractor.seekTo(ms  * 1000,MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        seekTime = 0;
    }


    public void release()
    {
        if (audiodecoder!=null)
        {
            audioExtractor.release();
            audiodecoder.stop();
            audiodecoder.release();
            Log.e("Audio ","Released");
            if (enableNativeRenderer)
            {
                nativeAudioRenderer.releaseAudioRenderer();
            }
        }

    }

}