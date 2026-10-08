package com.byd.voiceassistant.presentation.overlay;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.byd.voiceassistant.R;
import com.byd.voiceassistant.orchestrator.VoiceState;

public class VoiceOverlayView extends LinearLayout {

    private TextView tvVoiceState;
    private TextView tvTranscript;
    private TextView tvAssistantResponse;
    private ImageView btnDismiss;
    private Runnable onDismissClickListener;

    public VoiceOverlayView(Context context) {
        super(context);
        init(context);
    }

    public VoiceOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        if (context != null) {
            LayoutInflater.from(context).inflate(R.layout.view_voice_overlay, this, true);
            tvVoiceState = findViewById(R.id.tvVoiceState);
            tvTranscript = findViewById(R.id.tvTranscript);
            tvAssistantResponse = findViewById(R.id.tvAssistantResponse);
            btnDismiss = findViewById(R.id.btnDismissOverlay);

            if (btnDismiss != null) {
                btnDismiss.setOnClickListener(v -> {
                    if (onDismissClickListener != null) {
                        onDismissClickListener.run();
                    }
                });
            }
        }
    }

    public void setOnDismissClickListener(Runnable listener) {
        this.onDismissClickListener = listener;
    }

    public void updateState(VoiceState state) {
        if (tvVoiceState == null) return;
        switch (state) {
            case WAKE_LISTENING:
            case WAKE_DETECTED:
            case QUERY_CAPTURING:
                tvVoiceState.setText("Listening...");
                break;
            case EVALUATING_ENGINE_A:
            case ENRICHING_CONTEXT_MCP:
            case RUNNING_ENGINE_B_LLM:
                tvVoiceState.setText("Thinking...");
                break;
            case EXECUTING_VEHICLE_CMD:
                tvVoiceState.setText("Controlling Vehicle...");
                break;
            case SPEAKING:
                tvVoiceState.setText("Speaking...");
                break;
            case IDLE:
            default:
                tvVoiceState.setText("Ready");
                break;
        }
    }

    public void updateTranscript(String transcript) {
        if (tvTranscript != null) {
            tvTranscript.setText(transcript != null ? transcript : "");
        }
    }

    public void updateResponse(String response) {
        if (tvAssistantResponse != null) {
            if (response != null && !response.trim().isEmpty()) {
                tvAssistantResponse.setText(response);
                tvAssistantResponse.setVisibility(View.VISIBLE);
            } else {
                tvAssistantResponse.setVisibility(View.GONE);
            }
        }
    }
}
