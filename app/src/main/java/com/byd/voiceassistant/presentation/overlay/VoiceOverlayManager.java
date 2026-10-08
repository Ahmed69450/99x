package com.byd.voiceassistant.presentation.overlay;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.WindowManager;
import com.byd.voiceassistant.orchestrator.VoiceState;

public class VoiceOverlayManager {

    private final Context context;
    private WindowManager windowManager;
    private VoiceOverlayView overlayView;
    private boolean isShowing = false;
    private String lastTranscript = "";
    private String lastResponse = "";
    private VoiceState currentVoiceState = VoiceState.IDLE;
    private Handler dismissHandler;

    public VoiceOverlayManager(Context context) {
        this.context = context;
        if (context != null) {
            try {
                this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                this.dismissHandler = new Handler(Looper.getMainLooper());
            } catch (Exception ignored) {
            }
        }
    }

    public synchronized void showOverlay(String initialQuery) {
        this.lastTranscript = initialQuery != null ? initialQuery : "";
        this.isShowing = true;
        this.currentVoiceState = VoiceState.QUERY_CAPTURING;

        if (context == null || windowManager == null) {
            return;
        }

        try {
            if (overlayView == null) {
                overlayView = new VoiceOverlayView(context);
                overlayView.setOnDismissClickListener(this::dismissOverlay);

                int layoutType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_PHONE;

                WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    layoutType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                );
                params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                params.y = 80;

                windowManager.addView(overlayView, params);
            }

            overlayView.updateState(currentVoiceState);
            overlayView.updateTranscript(lastTranscript);
            overlayView.updateResponse("");
        } catch (Exception ignored) {
        }
    }

    public synchronized void updateState(VoiceState state) {
        this.currentVoiceState = state;
        if (overlayView != null) {
            overlayView.updateState(state);
        }

        if (state == VoiceState.IDLE && isShowing) {
            scheduleAutoDismiss(3000);
        }
    }

    public synchronized void updateTranscript(String transcript) {
        this.lastTranscript = transcript != null ? transcript : "";
        if (overlayView != null) {
            overlayView.updateTranscript(lastTranscript);
        }
    }

    public synchronized void updateResponse(String response) {
        this.lastResponse = response != null ? response : "";
        if (overlayView != null) {
            overlayView.updateResponse(lastResponse);
        }
    }

    public synchronized void dismissOverlay() {
        this.isShowing = false;
        if (dismissHandler != null) {
            dismissHandler.removeCallbacksAndMessages(null);
        }
        if (overlayView != null && windowManager != null) {
            try {
                windowManager.removeView(overlayView);
            } catch (Exception ignored) {
            }
            overlayView = null;
        }
    }

    private void scheduleAutoDismiss(long delayMs) {
        if (dismissHandler != null) {
            dismissHandler.postDelayed(this::dismissOverlay, delayMs);
        }
    }

    public boolean isShowing() {
        return isShowing;
    }

    public String getLastTranscript() {
        return lastTranscript;
    }

    public String getLastResponse() {
        return lastResponse;
    }

    public VoiceState getCurrentVoiceState() {
        return currentVoiceState;
    }
}
