#include <oboe/Oboe.h>
#include <math.h>
#include <jni.h>

using namespace oboe;

#ifndef CANNED_VIDEO_EDITOR_AUDIORENDERER_H
#define CANNED_VIDEO_EDITOR_AUDIORENDERER_H

class AudioRenderer: public AudioStreamDataCallback {
public:
    int32_t init(int channelCount,int sampleRate,int audioFormat,JNIEnv *envA,jobject thizA,jclass cls_foo);
    // Call this from Activity onResume()
    int32_t start();
    // Call this from Activity onPause()
    int32_t stop();

    int32_t release();

    void writeAudioData(jshortArray audioData,int32_t size);
    void setVolume(float vol);
    float getVolume();

    DataCallbackResult
    onAudioReady(AudioStream *audioStream, void *audioData, int32_t numFrames) override;

    static void setDefaultValues(int32_t default_sample_rate, int32_t default_frames_per_burst);

private:
    std::shared_ptr<oboe::AudioStream> mStream;
    int32_t defaultSampleRate;
    int32_t defaultFramesPerBurst;
    // Keeps track of where the wave is
    float mPhase = 0.0;
    JNIEnv *env;
    jobject thiz;
    jclass cls_foo;
    jmethodID java_callback;
    JavaVM* vm;

    float volume = 1;
    int16_t *dataBuffer;
    static AudioFormat getAudioFormat(int audioFormat);

};




#endif //CANNED_VIDEO_EDITOR_AUDIORENDERER_H
