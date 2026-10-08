package com.byd.voiceassistant.presentation.settings;

import com.byd.voiceassistant.context.storage.ModelPreferencesDataStore;
import org.junit.Assert;
import org.junit.Test;

public class SettingsViewModelTest {

    @Test
    public void testModelSelectionUpdatesState() {
        ModelPreferencesDataStore dataStore = new ModelPreferencesDataStore(null);
        SettingsViewModel viewModel = new SettingsViewModel(dataStore);

        viewModel.onGgufModelSelected("/sdcard/Download/qwen2.5-0.5b.gguf");
        Assert.assertEquals("/sdcard/Download/qwen2.5-0.5b.gguf", viewModel.getSelectedGgufPath());

        viewModel.onPiperModelSelected("/sdcard/Download/en_US-lessac.onnx");
        Assert.assertEquals("/sdcard/Download/en_US-lessac.onnx", viewModel.getSelectedPiperPath());

        Assert.assertTrue(viewModel.getDiagnostics().isLlmConfigured());
        Assert.assertTrue(viewModel.getDiagnostics().isPiperConfigured());
    }

    @Test
    public void testCpuThreadSettingClamped() {
        ModelPreferencesDataStore dataStore = new ModelPreferencesDataStore(null);
        SettingsViewModel viewModel = new SettingsViewModel(dataStore);

        viewModel.setCpuThreads(10);
        Assert.assertEquals(3, viewModel.getCpuThreads());

        viewModel.setCpuThreads(1);
        Assert.assertEquals(2, viewModel.getCpuThreads());
    }
}
