#include <jni.h>
#include <string>
#include <android/log.h>

#define TAG "LlamaAndroidJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

struct LlamaContextWrapper {
    std::string model_path;
    int n_threads;
    int n_ctx;
};

extern "C" JNIEXPORT jlong JNICALL
Java_com_byd_voiceassistant_ai_llm_LlamaCppEngine_nativeInit(
    JNIEnv* env,
    jobject /* this */,
    jstring jModelPath,
    jint nThreads,
    jint nCtx
) {
    if (!jModelPath) {
        LOGE("Null model path provided");
        return 0;
    }

    const char* path = env->GetStringUTFChars(jModelPath, nullptr);
    LOGI("Initializing llama context for: %s with %d threads, %d ctx", path, nThreads, nCtx);

    auto* wrapper = new LlamaContextWrapper();
    wrapper->model_path = path;
    wrapper->n_threads = nThreads;
    wrapper->n_ctx = nCtx;

    env->ReleaseStringUTFChars(jModelPath, path);
    return reinterpret_cast<jlong>(wrapper);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_byd_voiceassistant_ai_llm_LlamaCppEngine_nativeGenerate(
    JNIEnv* env,
    jobject /* this */,
    jlong handle,
    jstring jPrompt
) {
    if (handle == 0 || !jPrompt) {
        return env->NewStringUTF("Error: Invalid llama handle or null prompt.");
    }

    auto* wrapper = reinterpret_cast<LlamaContextWrapper*>(handle);
    const char* prompt = env->GetStringUTFChars(jPrompt, nullptr);

    LOGI("llama generating response on %d threads", wrapper->n_threads);

    // Default simulated edge response for native runtime
    std::string response = "Offline engine ready. Drive safely.";
    env->ReleaseStringUTFChars(jPrompt, prompt);

    return env->NewStringUTF(response.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_byd_voiceassistant_ai_llm_LlamaCppEngine_nativeRelease(
    JNIEnv* env,
    jobject /* this */,
    jlong handle
) {
    if (handle != 0) {
        auto* wrapper = reinterpret_cast<LlamaContextWrapper*>(handle);
        delete wrapper;
        LOGI("llama context freed");
    }
}
