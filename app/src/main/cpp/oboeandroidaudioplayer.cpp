#include <jni.h>
#include <android/log.h>
#include <unistd.h>
#include "audio/AudioRenderer.h"

JavaVM* g_vm;
// Android Prints Log
#define LOGE(FORMAT,...) __android_log_print(ANDROID_LOG_ERROR, "Oboe Native", FORMAT, ##__VA_ARGS__);
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* aReserved)
{
    //store the pointer to virtual machine unless you can do everything you need from OnLoad
    g_vm = vm;
    //this is just to get something in adb logcat
    __android_log_write(ANDROID_LOG_DEBUG, "Oboe Native", "JNI ON LOAD\n");
    //return the version you need, you may also check here if it is supportd
    return JNI_VERSION_1_6;
}

void attachVm2thread(JNIEnv *jni_env){
    int getEnvStat = (*g_vm).GetEnv((void**) &jni_env, JNI_VERSION_1_6);
    if (getEnvStat == JNI_EDETACHED)
    {
        __android_log_print(ANDROID_LOG_DEBUG, "Oboe Native", "getenv not attached");
        jint result=(*g_vm).AttachCurrentThread(&jni_env, nullptr);
        if(result != JNI_OK)
            __android_log_print(ANDROID_LOG_DEBUG, "Oboe Native", "error in attaching");

    }
    else if (getEnvStat == JNI_OK)
        __android_log_print(ANDROID_LOG_DEBUG, "Oboe Native", "already attached\n", JNI_OK);
    else if (getEnvStat == JNI_EVERSION)
        __android_log_print(ANDROID_LOG_DEBUG, "Oboe Native", "get env version not supported");
}


///////////////////////////////////////////////////////////////////

AudioRenderer audioRendererList[100];
int trackIndex = 0;



void refreshAudioRendererList(int startPoint)
{
    //  LOGE("Working");
    for (int i = startPoint; i < 100; i++) {
        audioRendererList[i] = audioRendererList[i+1];
    }

}




extern "C"
JNIEXPORT jint  JNICALL
Java_com_umirtech_oboeandroidaudioplayer_NativeAudioRenderer_createAudioRendererNative(
        JNIEnv *env, jobject thiz,jint channelCount,jint sampleRate,jint audioFormat) {
    attachVm2thread(env);

    jclass cls_foo = (*env).GetObjectClass(thiz);
    // get the method IDs from that class

    jobject thizG = env->NewGlobalRef(thiz);
    auto cls_fooG = static_cast<jclass>(env->NewGlobalRef(cls_foo));

    AudioRenderer audioRenderer;
    audioRenderer.init(channelCount,sampleRate,audioFormat,env,thizG,cls_fooG);
    audioRendererList[trackIndex] = audioRenderer;
    trackIndex++;
    return trackIndex-1;
}


extern "C"
JNIEXPORT jint   JNICALL
Java_com_umirtech_oboeandroidaudioplayer_NativeAudioRenderer_setDefaultStreamValuesNative(
        JNIEnv *env, jclass thiz, jint default_sample_rate, jint default_frames_per_burst) {
    AudioRenderer::setDefaultValues(default_sample_rate,default_frames_per_burst);
    return 0;
}

extern "C"
JNIEXPORT jint   JNICALL
Java_com_umirtech_oboeandroidaudioplayer_NativeAudioRenderer_startAudioRendererNative(
        JNIEnv *env, jobject clazz,jint trackIndexA) {
    if (trackIndexA < 100)
    {
        int r = audioRendererList[trackIndexA].start();
        LOGE("Track %d Started",trackIndexA);
        return r;
    } else{
        return -1;
    }
}

extern "C"
JNIEXPORT jint  JNICALL
Java_com_umirtech_oboeandroidaudioplayer_NativeAudioRenderer_stopAudioRendererNative(
        JNIEnv *env, jobject clazz,jint trackIndexA) {
    if (trackIndexA < 100)
    {
        int r = audioRendererList[trackIndexA].stop();
        return r;
    } else{
        return -1;
    }
}
extern "C"
JNIEXPORT jint  JNICALL
Java_com_umirtech_oboeandroidaudioplayer_NativeAudioRenderer_releaseAudioRendererNative(
        JNIEnv *env, jobject clazz,jint trackIndexA) {
    if (trackIndexA < 100)
    {
        int r = audioRendererList[trackIndexA].release();;
        refreshAudioRendererList(trackIndexA);
        trackIndex--;
        return r;
    } else{
        return -1;
    }
}

extern "C"
JNIEXPORT jint
Java_com_umirtech_oboeandroidaudioplayer_NativeAudioRenderer_writeDataNative(
        JNIEnv *env, jobject thiz,jint trackIndexA, jshortArray audio_data, jint size) {
    if (trackIndexA < 100)
    {
        audioRendererList[trackIndexA].writeAudioData(audio_data,size);
    }
    return 0;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_umirtech_oboeandroidaudioplayer_NativeAudioRenderer_setVolumeNative(
        JNIEnv *env, jobject thiz,jint trackIndexA,jfloat vol) {

    if (trackIndexA < 100)
    {
        audioRendererList[trackIndexA].setVolume(vol);
    }
}
extern "C"
JNIEXPORT jfloat JNICALL
Java_com_umirtech_oboeandroidaudioplayer_NativeAudioRenderer_getVolumeNative(
        JNIEnv *env, jobject thiz, jint trackIndexA) {
    if (trackIndexA < 100)
    {
        return audioRendererList[trackIndexA].getVolume();
    }

    return 0;
}
