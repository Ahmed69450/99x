package com.byd.voiceassistant.presentation.settings;

import com.byd.voiceassistant.context.storage.ModelPreferencesDataStore;

public class SettingsViewModel {

    public static class EngineDiagnostics {
        private final boolean llmConfigured;
        private final boolean piperConfigured;
        private final boolean voskConfigured;
        private final boolean onnxReady;

        public EngineDiagnostics(boolean llmConfigured, boolean piperConfigured, boolean voskConfigured, boolean onnxReady) {
            this.llmConfigured = llmConfigured;
            this.piperConfigured = piperConfigured;
            this.voskConfigured = voskConfigured;
            this.onnxReady = onnxReady;
        }

        public boolean isLlmConfigured() {
            return llmConfigured;
        }

        public boolean isPiperConfigured() {
            return piperConfigured;
        }

        public boolean isVoskConfigured() {
            return voskConfigured;
        }

        public boolean isOnnxReady() {
            return onnxReady;
        }
    }

    private final ModelPreferencesDataStore dataStore;

    public SettingsViewModel(ModelPreferencesDataStore dataStore) {
        this.dataStore = dataStore != null ? dataStore : new ModelPreferencesDataStore(null);
    }

    public void onGgufModelSelected(String path) {
        dataStore.setGgufModelPath(path);
    }

    public String getSelectedGgufPath() {
        return dataStore.getGgufModelPath();
    }

    public void onPiperModelSelected(String path) {
        dataStore.setPiperModelPath(path);
    }

    public String getSelectedPiperPath() {
        return dataStore.getPiperModelPath();
    }

    public void onVoskModelSelected(String path) {
        dataStore.setVoskModelPath(path);
    }

    public String getSelectedVoskPath() {
        return dataStore.getVoskModelPath();
    }

    public void setCpuThreads(int threads) {
        dataStore.setCpuThreads(threads);
    }

    public int getCpuThreads() {
        return dataStore.getCpuThreads();
    }

    public EngineDiagnostics getDiagnostics() {
        boolean llmConfigured = !dataStore.getGgufModelPath().isEmpty();
        boolean piperConfigured = !dataStore.getPiperModelPath().isEmpty();
        boolean voskConfigured = !dataStore.getVoskModelPath().isEmpty();
        boolean onnxReady = true; // Engine A in-memory catalog is always ready

        return new EngineDiagnostics(llmConfigured, piperConfigured, voskConfigured, onnxReady);
    }
}
