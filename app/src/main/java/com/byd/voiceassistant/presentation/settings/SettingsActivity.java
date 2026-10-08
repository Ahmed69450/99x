package com.byd.voiceassistant.presentation.settings;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import com.byd.voiceassistant.context.storage.ModelPreferencesDataStore;
import com.byd.voiceassistant.service.VoiceForegroundService;

public class SettingsActivity extends Activity {

    private static final int REQUEST_CODE_GGUF = 101;
    private static final int REQUEST_CODE_PIPER = 102;

    private SettingsViewModel viewModel;
    private TextView tvSelectedGguf;
    private TextView tvSelectedPiper;
    private TextView tvEngineStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String pkg = getPackageName();
        int layoutId = getResources() != null ? getResources().getIdentifier("activity_settings", "layout", pkg) : 0;
        if (layoutId != 0) {
            setContentView(layoutId);
            int ggufTvId = getResources().getIdentifier("tvSelectedGguf", "id", pkg);
            int piperTvId = getResources().getIdentifier("tvSelectedPiper", "id", pkg);
            int statusTvId = getResources().getIdentifier("tvEngineStatus", "id", pkg);
            int ggufBtnId = getResources().getIdentifier("btnSelectGguf", "id", pkg);
            int piperBtnId = getResources().getIdentifier("btnSelectPiper", "id", pkg);

            tvSelectedGguf = findViewById(ggufTvId);
            tvSelectedPiper = findViewById(piperTvId);
            tvEngineStatus = findViewById(statusTvId);

            Button btnSelectGguf = findViewById(ggufBtnId);
            if (btnSelectGguf != null) {
                btnSelectGguf.setOnClickListener(v -> launchFilePicker("*/*", REQUEST_CODE_GGUF));
            }

            Button btnSelectPiper = findViewById(piperBtnId);
            if (btnSelectPiper != null) {
                btnSelectPiper.setOnClickListener(v -> launchFilePicker("*/*", REQUEST_CODE_PIPER));
            }
        }

        ModelPreferencesDataStore dataStore = new ModelPreferencesDataStore(this);
        viewModel = new SettingsViewModel(dataStore);

        updateUiFromViewModel();

        // Start background wake-word foreground service automatically
        try {
            Intent serviceIntent = new Intent(this, VoiceForegroundService.class);
            startService(serviceIntent);
        } catch (Exception ignored) {
        }
    }

    private void launchFilePicker(String mimeType, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(mimeType);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select Model File"), requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            String path = uri.getPath();

            if (requestCode == REQUEST_CODE_GGUF) {
                viewModel.onGgufModelSelected(path);
            } else if (requestCode == REQUEST_CODE_PIPER) {
                viewModel.onPiperModelSelected(path);
            }

            updateUiFromViewModel();
        }
    }

    private void updateUiFromViewModel() {
        if (tvSelectedGguf != null) {
            String gguf = viewModel.getSelectedGgufPath();
            tvSelectedGguf.setText(gguf.isEmpty() ? "No .gguf model selected (using built-in fallback)" : gguf);
        }

        if (tvSelectedPiper != null) {
            String piper = viewModel.getSelectedPiperPath();
            tvSelectedPiper.setText(piper.isEmpty() ? "No Piper model selected (using Android system TTS)" : piper);
        }

        if (tvEngineStatus != null) {
            SettingsViewModel.EngineDiagnostics diag = viewModel.getDiagnostics();
            String status = "Engine A (ONNX Router): " + (diag.isOnnxReady() ? "READY" : "OFFLINE") +
                "\nEngine B (llama.cpp): " + (diag.isLlmConfigured() ? "CONFIGURED (" + viewModel.getCpuThreads() + " threads)" : "STANDBY (built-in fallback)") +
                "\nTTS (Piper): " + (diag.isPiperConfigured() ? "LOADED" : "SYSTEM TTS ACTIVE") +
                "\nHardware Control (BYD DiLink): ACTIVE";
            tvEngineStatus.setText(status);
        }
    }
}
