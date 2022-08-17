package com.umirtech.nativeaudiorenderer;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

public class NativeAudioRenderer {

    public static final int AUDIO_FORMAT_PCM_16bit = 1;
    public static final int AUDIO_FORMAT_PCM_FLOAT = 2;
    public static final int AUDIO_FORMAT_PCM_24bit_PACKED = 3;
    public static final int AUDIO_FORMAT_PCM_32bit = 4;
    public static int DEFAULT_FRAMES_PER_BURST;
    private int index;

    private OnDataRequestCallback onDataRequestCallback;

    static {
        System.loadLibrary("NativeAudioRenderer");
    }

    public static void init(Context context)
    {
        AudioManager myAudioMgr = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        String sampleRateStr = myAudioMgr.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
        int defaultSampleRate = Integer.parseInt(sampleRateStr);
        String framesPerBurstStr = myAudioMgr.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
        int defaultFramesPerBurst = Integer.parseInt(framesPerBurstStr);
        DEFAULT_FRAMES_PER_BURST = defaultFramesPerBurst;
        Log.e("Canned Editor ","Device Default Sample Rate " + defaultSampleRate);
        Log.e("Canned Editor ","Device Default Frames per Burst " + defaultFramesPerBurst);

        setDefaultStreamValuesNative(defaultSampleRate,defaultFramesPerBurst);
    }


    public  void createAudioRenderer(int channelCount,int sampleRate,int audioFormat)
    {
       index = createAudioRendererNative(channelCount,sampleRate,audioFormat);
    }


    public void startRenderer()
    {
        startAudioRendererNative(index);
    }


    public void stopRenderer()
    {
        stopAudioRendererNative(index);
    }



    public void writeData(short[] audioData,int size)
    {
        writeDataNative(index,audioData,size);
    }


    public interface OnDataRequestCallback
    {
        short[] onRequest();
    }

    public void addDataRequestCallBack(OnDataRequestCallback onDataRequestCallback)
    {
        this.onDataRequestCallback = onDataRequestCallback;
    }

    private short[] deliverOnDataRequestFromJNI()
    {
        if (onDataRequestCallback != null)
        {
           return onDataRequestCallback.onRequest();
        }
        return null;
    }

    public void releaseAudioRenderer()
    {
        releaseAudioRendererNative(index);
    }


    public float getVolume() {
        return getVolumeNative(index);
    }

    public void setVolume(float volume) {
        setVolumeNative(index,volume);
    }

    //////////////////////// native Functions ////////////////
    private native int createAudioRendererNative(int channelCount,int sampleRate,int audioFormat);
    private native int startAudioRendererNative(int index);
    private native int stopAudioRendererNative(int index);
    private native int releaseAudioRendererNative(int index);
    private native int writeDataNative(int index,short[] audioData,int size);
    private static native int setDefaultStreamValuesNative(int defaultSampleRate, int defaultFramesPerBurst);
    private native void setVolumeNative(int index,float vol);
    private native float getVolumeNative(int index);

}
