package com.byd.voiceassistant.audio.capture;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

public class AudioRecordSource {

    public interface AudioChunkListener {
        void onAudioChunk(byte[] chunk);
    }

    public static final int SAMPLE_RATE = 16000;
    public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    public static final int CHUNK_SIZE = 640; // 20ms at 16kHz 16-bit mono

    private final AudioChunkListener listener;
    private AudioRecord audioRecord;
    private Thread recordingThread;
    private volatile boolean isRecording = false;

    public AudioRecordSource(AudioChunkListener listener) {
        this.listener = listener;
    }

    public synchronized boolean start() {
        if (isRecording) {
            return true;
        }

        try {
            int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
            int bufferSize = Math.max(minBufferSize, CHUNK_SIZE * 4);

            audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            );

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                return false;
            }

            audioRecord.startRecording();
            isRecording = true;

            recordingThread = new Thread(() -> {
                byte[] buffer = new byte[CHUNK_SIZE];
                while (isRecording) {
                    int read = audioRecord.read(buffer, 0, buffer.length);
                    if (read > 0 && listener != null) {
                        byte[] chunk = new byte[read];
                        System.arraycopy(buffer, 0, chunk, 0, read);
                        listener.onAudioChunk(chunk);
                    }
                }
            }, "AudioRecordThread");
            recordingThread.start();
            return true;
        } catch (SecurityException ignored) {
            return false;
        } catch (Exception ignored) {
            return false;
        }
    }

    public synchronized void stop() {
        isRecording = false;
        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
            } catch (Exception ignored) {
            }
            audioRecord = null;
        }
        if (recordingThread != null) {
            recordingThread.interrupt();
            recordingThread = null;
        }
    }

    public boolean isRecording() {
        return isRecording;
    }
}
