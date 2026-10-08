package com.byd.voiceassistant.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import com.byd.voiceassistant.ai.llm.LlamaCppEngine;
import com.byd.voiceassistant.ai.router.OnnxSemanticRouter;
import com.byd.voiceassistant.audio.capture.AudioRecordSource;
import com.byd.voiceassistant.audio.stt.SpeechRecognitionListener;
import com.byd.voiceassistant.audio.stt.VoskSpeechRecognizer;
import com.byd.voiceassistant.audio.tts.AndroidFallbackTtsEngine;
import com.byd.voiceassistant.audio.tts.PiperTtsEngine;
import com.byd.voiceassistant.audio.tts.TtsEngine;
import com.byd.voiceassistant.audio.wakeword.WakeWordDetector;
import com.byd.voiceassistant.context.location.LocationTimeProvider;
import com.byd.voiceassistant.context.memory.MemoryRetrievalService;
import com.byd.voiceassistant.context.storage.ModelPreferencesDataStore;
import com.byd.voiceassistant.mcp.McpClientEngine;
import com.byd.voiceassistant.mcp.tools.ScreenInspectTool;
import com.byd.voiceassistant.mcp.tools.WebSearchTool;
import com.byd.voiceassistant.orchestrator.VoicePipelineListener;
import com.byd.voiceassistant.orchestrator.VoicePipelineOrchestrator;
import com.byd.voiceassistant.orchestrator.VoiceState;
import com.byd.voiceassistant.presentation.overlay.VoiceOverlayManager;
import com.byd.voiceassistant.presentation.settings.SettingsActivity;
import com.byd.voiceassistant.vehicle.BYDVehicleController;

public class VoiceForegroundService extends Service {

    public static final String CHANNEL_ID = "voice_service_channel";
    public static final int NOTIFICATION_ID = 1001;

    private PowerManager.WakeLock wakeLock;
    private AudioRecordSource audioRecordSource;
    private WakeWordDetector wakeWordDetector;
    private VoskSpeechRecognizer speechRecognizer;
    private VoiceOverlayManager overlayManager;
    private VoicePipelineOrchestrator orchestrator;
    private TtsEngine ttsEngine;
    private Handler mainHandler;
    private volatile boolean isQueryActive = false;

    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
        acquireWakeLock();
        initPipeline();
    }

    private void initPipeline() {
        overlayManager = new VoiceOverlayManager(this);

        ModelPreferencesDataStore prefs = new ModelPreferencesDataStore(this);
        AndroidFallbackTtsEngine fallbackTts = new AndroidFallbackTtsEngine(this);
        ttsEngine = new PiperTtsEngine(prefs.getPiperModelPath(), fallbackTts);

        BYDVehicleController vehicleController = new BYDVehicleController(this);
        OnnxSemanticRouter semanticRouter = new OnnxSemanticRouter();
        LlamaCppEngine llmEngine = new LlamaCppEngine();
        llmEngine.loadModel(prefs.getGgufModelPath());
        llmEngine.setThreadCount(prefs.getCpuThreads());

        McpClientEngine mcpClient = new McpClientEngine();
        mcpClient.registerTool(new WebSearchTool(true));
        mcpClient.registerTool(new ScreenInspectTool(VoiceAccessibilityService::getLastScreenContext));

        LocationTimeProvider locationProvider = new LocationTimeProvider(this);
        MemoryRetrievalService memoryService = new MemoryRetrievalService();

        orchestrator = new VoicePipelineOrchestrator(
            semanticRouter,
            vehicleController,
            llmEngine,
            mcpClient,
            locationProvider,
            memoryService,
            ttsEngine
        );

        orchestrator.addListener(new VoicePipelineListener() {
            @Override
            public void onStateChanged(VoiceState newState) {
                mainHandler.post(() -> {
                    if (overlayManager != null) {
                        overlayManager.updateState(newState);
                    }
                });
            }

            @Override
            public void onTranscriptUpdated(String transcript) {
                mainHandler.post(() -> {
                    if (overlayManager != null) {
                        overlayManager.updateTranscript(transcript);
                    }
                });
            }

            @Override
            public void onResponseGenerated(String response) {
                mainHandler.post(() -> {
                    if (overlayManager != null) {
                        overlayManager.updateResponse(response);
                    }
                });
            }

            @Override
            public void onError(String message) {
            }
        });

        speechRecognizer = new VoskSpeechRecognizer(prefs.getVoskModelPath(), new SpeechRecognitionListener() {
            @Override
            public void onPartialResult(String hypothesis) {
                mainHandler.post(() -> {
                    if (overlayManager != null) {
                        overlayManager.updateTranscript(hypothesis);
                    }
                });
            }

            @Override
            public void onFinalResult(String transcript) {
                isQueryActive = false;
                new Thread(() -> {
                    orchestrator.setScreenContext(VoiceAccessibilityService.getLastScreenContext());
                    orchestrator.processUtterance(transcript);
                }, "VoiceProcessingThread").start();
            }

            @Override
            public void onError(String error) {
                isQueryActive = false;
                mainHandler.post(() -> {
                    if (overlayManager != null) {
                        overlayManager.dismissOverlay();
                    }
                });
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startInForeground("Hands-free wake word listening enabled");
        startAudioListening();
        return START_STICKY;
    }

    private void startInForeground(String statusText) {
        Intent notificationIntent = new Intent(this, SettingsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        Notification notification = builder
            .setContentTitle("BYD Voice Assistant Active")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "BYD Voice Assistant Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Background wake word listening and head unit voice assistant service");
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void acquireWakeLock() {
        try {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BYDVoiceAssistant:WakeLock");
                wakeLock.acquire(10 * 60 * 1000L /* 10 minutes */);
            }
        } catch (Exception ignored) {
        }
    }

    private void startAudioListening() {
        if (audioRecordSource == null) {
            wakeWordDetector = new WakeWordDetector(keyword -> {
                isQueryActive = true;
                mainHandler.post(() -> {
                    if (overlayManager != null) {
                        overlayManager.showOverlay("Listening...");
                    }
                    if (speechRecognizer != null) {
                        speechRecognizer.start();
                    }
                });
            });
            wakeWordDetector.start();

            audioRecordSource = new AudioRecordSource(chunk -> {
                if (isQueryActive && speechRecognizer != null) {
                    speechRecognizer.processAudioChunk(chunk);
                } else if (wakeWordDetector != null) {
                    wakeWordDetector.processPcmChunk(chunk);
                }
            });

            boolean started = audioRecordSource.start();
            if (!started) {
                startInForeground("Microphone unavailable or permission denied");
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (audioRecordSource != null) {
            audioRecordSource.stop();
            audioRecordSource = null;
        }
        if (wakeWordDetector != null) {
            wakeWordDetector.stop();
            wakeWordDetector = null;
        }
        if (speechRecognizer != null) {
            speechRecognizer.stop();
            speechRecognizer = null;
        }
        if (ttsEngine != null) {
            ttsEngine.release();
            ttsEngine = null;
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
