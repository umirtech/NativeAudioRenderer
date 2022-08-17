
#include "AudioRenderer.h"
#include <android/log.h>
#include <jni.h>

#define LOGE(FORMAT,...) __android_log_print(ANDROID_LOG_ERROR, "Oboe Native", FORMAT, ##__VA_ARGS__);

DataCallbackResult
AudioRenderer::onAudioReady(AudioStream *audioStream, void *audioData, int32_t numFrames) {
    //LOGE("CallBack");
    return DataCallbackResult::Continue;
}

int32_t AudioRenderer::init(int channelCount,int sampleRate,int audioFormat,JNIEnv *envA,jobject thizA,jclass cls_fooA)
{
    env = envA;
    thiz = thizA;
    cls_foo = cls_fooA;
    env->GetJavaVM(&vm);

    java_callback  = (*env).GetMethodID(cls_foo,"deliverOnDataRequestFromJNI","()[S");

    oboe::AudioStreamBuilder builder;
    // The builder set methods can be chained for convenience.
    Result result = builder.setSharingMode(oboe::SharingMode::Shared)
            ->setPerformanceMode(oboe::PerformanceMode::None)
            ->setChannelCount(channelCount)
            ->setSampleRate(sampleRate)
            ->setSampleRateConversionQuality(oboe::SampleRateConversionQuality::Best)
            ->setFormat(getAudioFormat(audioFormat))
            ->openStream(mStream);
   // LOGE("Audio Format %s", convertToText(getAudioFormat(audioFormat)));

    if (result != Result::OK)
    {
        LOGE("Error Oboe");
        return (int32_t) result;
    }
    dataBuffer = (int16_t *) malloc(mStream->getBufferSizeInFrames() * sizeof(int16_t));
    defaultFramesPerBurst = oboe::DefaultStreamValues::FramesPerBurst;
    defaultSampleRate = oboe::DefaultStreamValues::SampleRate;
    return (int32_t) result;
}

int32_t AudioRenderer::start() {
    // Typically, start the stream after querying some stream information, as well as some input from the user
    Result result;

    result = mStream->requestStart();
    return (int32_t) result;
}

int32_t AudioRenderer::stop() {
    // Stop, close and delete in case not already closed.
    Result result = Result::ErrorNoService;
    if (mStream) {
      result =  mStream->stop();
      mStream->flush();
    }
    return (int32_t) result;
}


int32_t AudioRenderer::release() {
    Result result = Result::ErrorNoService;
    result = mStream->close();
    (*vm).AttachCurrentThread(&env, nullptr);
    mStream.reset();
    env->DeleteGlobalRef(thiz);
    env->DeleteGlobalRef(cls_foo);
    free(dataBuffer);
    return (int32_t) result;
}

void AudioRenderer::writeAudioData(jshortArray data,int32_t size) {
    (*vm).AttachCurrentThread(&env, nullptr);
    jshort* sArray = env->GetShortArrayElements( data, nullptr);
    int32_t len = env->GetArrayLength(data);
    for (int i = 0; i < len; i++)
    {
        int16_t s = sArray[i];
        if (volume != 1 && volume != 0)
        {
            int32_t an = s * volume;
            if (an > 32767) an = 32767;
            if (an < -32768) an = -32768;

            dataBuffer[i] = (int16_t) an;
        } else{
            dataBuffer[i] = s;
        }
    }
   // LOGE("Length %d",len);

    Result result = mStream->write(dataBuffer,len/2,0);
    env->ReleaseShortArrayElements( data, sArray, 0);
}

void AudioRenderer::setDefaultValues(int32_t default_sample_rate, int32_t default_frames_per_burst) {
    oboe::DefaultStreamValues::SampleRate = (int32_t) default_sample_rate;
    oboe::DefaultStreamValues::FramesPerBurst = (int32_t) default_frames_per_burst;
}

AudioFormat AudioRenderer::getAudioFormat(int audioFormat) {
    switch (audioFormat) {
        case 1:
            return AudioFormat::I16;
        case 2:
            return AudioFormat::Float;
        case 3:
            return AudioFormat::I24;
        case 4:
            return AudioFormat::I32;
        default:
            return AudioFormat::I16;
    }
}

void AudioRenderer::setVolume(float vol) {
    volume = vol;
}

float AudioRenderer::getVolume() {
    return volume;
}



