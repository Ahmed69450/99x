package com.byd.voiceassistant.context.storage;

import android.content.Context;
import android.content.SharedPreferences;

public class ModelPreferencesDataStore {

    public static final String PREFS_NAME = "byd_model_preferences";
    public static final String KEY_GGUF_MODEL_PATH = "key_gguf_model_path";
    public static final String KEY_PIPER_MODEL_PATH = "key_piper_model_path";
    public static final String KEY_VOSK_MODEL_PATH = "key_vosk_model_path";
    public static final String KEY_CPU_THREADS = "key_cpu_threads";

    private final Context context;
    private String inMemoryGgufPath = "";
    private String inMemoryPiperPath = "";
    private String inMemoryVoskPath = "";
    private int inMemoryThreads = 2;

    public ModelPreferencesDataStore(Context context) {
        this.context = context;
        if (context != null) {
            try {
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                this.inMemoryGgufPath = prefs.getString(KEY_GGUF_MODEL_PATH, "");
                this.inMemoryPiperPath = prefs.getString(KEY_PIPER_MODEL_PATH, "");
                this.inMemoryVoskPath = prefs.getString(KEY_VOSK_MODEL_PATH, "");
                this.inMemoryThreads = prefs.getInt(KEY_CPU_THREADS, 2);
            } catch (Exception ignored) {
            }
        }
    }

    public synchronized void setGgufModelPath(String path) {
        this.inMemoryGgufPath = path != null ? path : "";
        saveString(KEY_GGUF_MODEL_PATH, inMemoryGgufPath);
    }

    public synchronized String getGgufModelPath() {
        return inMemoryGgufPath;
    }

    public synchronized void setPiperModelPath(String path) {
        this.inMemoryPiperPath = path != null ? path : "";
        saveString(KEY_PIPER_MODEL_PATH, inMemoryPiperPath);
    }

    public synchronized String getPiperModelPath() {
        return inMemoryPiperPath;
    }

    public synchronized void setVoskModelPath(String path) {
        this.inMemoryVoskPath = path != null ? path : "";
        saveString(KEY_VOSK_MODEL_PATH, inMemoryVoskPath);
    }

    public synchronized String getVoskModelPath() {
        return inMemoryVoskPath;
    }

    public synchronized void setCpuThreads(int threads) {
        this.inMemoryThreads = Math.max(2, Math.min(3, threads));
        if (context != null) {
            try {
                SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putInt(KEY_CPU_THREADS, inMemoryThreads);
                editor.apply();
            } catch (Exception ignored) {
            }
        }
    }

    public synchronized int getCpuThreads() {
        return inMemoryThreads;
    }

    private void saveString(String key, String value) {
        if (context != null) {
            try {
                SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                editor.putString(key, value);
                editor.apply();
            } catch (Exception ignored) {
            }
        }
    }
}
