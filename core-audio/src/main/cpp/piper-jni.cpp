#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>

#define TAG "PiperAndroidJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

struct PiperVoiceContext {
    std::string model_path;
};

extern "C" JNIEXPORT jlong JNICALL
Java_com_byd_voiceassistant_audio_tts_PiperTtsEngine_nativeInitPiper(
    JNIEnv* env,
    jobject /* this */,
    jstring jModelPath
) {
    if (!jModelPath) return 0;
    const char* path = env->GetStringUTFChars(jModelPath, nullptr);
    LOGI("Loading Piper ONNX VITS model from: %s", path);

    auto* ctx = new PiperVoiceContext();
    ctx->model_path = path;
    env->ReleaseStringUTFChars(jModelPath, path);
    return reinterpret_cast<jlong>(ctx);
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_byd_voiceassistant_audio_tts_PiperTtsEngine_nativeSynthesize(
    JNIEnv* env,
    jobject /* this */,
    jlong handle,
    jstring jText
) {
    if (handle == 0 || !jText) return nullptr;

    const char* text = env->GetStringUTFChars(jText, nullptr);
    LOGI("Synthesizing speech via Piper for: %s", text);

    // Generate dummy 22050Hz 16-bit PCM buffer (e.g. 100ms tone) for native testing
    int sampleRate = 22050;
    int durationMs = 100;
    int numSamples = (sampleRate * durationMs) / 1000;
    std::vector<int16_t> pcmSamples(numSamples, 0);

    jbyteArray result = env->NewByteArray(numSamples * 2);
    env->SetByteArrayRegion(result, 0, numSamples * 2, reinterpret_cast<const jbyte*>(pcmSamples.data()));

    env->ReleaseStringUTFChars(jText, text);
    return result;
}

extern "C" JNIEXPORT void JNICALL
Java_com_byd_voiceassistant_audio_tts_PiperTtsEngine_nativeReleasePiper(
    JNIEnv* env,
    jobject /* this */,
    jlong handle
) {
    if (handle != 0) {
        auto* ctx = reinterpret_cast<PiperVoiceContext*>(handle);
        delete ctx;
        LOGI("Piper voice context freed");
    }
}
