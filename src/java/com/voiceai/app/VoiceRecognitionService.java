package com.voiceai.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionService;
import android.speech.SpeechRecognizer;
import android.util.Log;

/**
 * VoiceRecognitionService - Launches RecognizeActivity overlay for voice input
 * Works with HeliBoard, Gboard, and other keyboards that use RecognitionService
 * API
 */
public class VoiceRecognitionService extends RecognitionService {

    private static final String TAG = "VoiceAI";

    private Handler mainHandler;
    private Callback currentCallback;
    private static VoiceRecognitionService instance;

    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler(Looper.getMainLooper());
        instance = this;
        Log.d(TAG, "VoiceRecognitionService created");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        Log.d(TAG, "VoiceRecognitionService destroyed");
    }

    @Override
    protected void onStartListening(Intent intent, Callback callback) {
        Log.d(TAG, "onStartListening - launching RecognizeActivity overlay");
        currentCallback = callback;

        // Launch RecognizeActivity as an overlay
        // The activity will handle recording and return results via broadcast
        try {
            Intent overlayIntent = new Intent(this, RecognizeActivity.class);
            overlayIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            overlayIntent.putExtra("from_service", true);
            startActivity(overlayIntent);

            // Notify ready for speech
            if (callback != null) {
                try {
                    callback.readyForSpeech(new Bundle());
                } catch (android.os.RemoteException e) {
                    Log.e(TAG, "RemoteException in readyForSpeech", e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error launching RecognizeActivity", e);
            if (callback != null) {
                try {
                    callback.error(SpeechRecognizer.ERROR_RECOGNIZER_BUSY);
                } catch (android.os.RemoteException re) {
                    Log.e(TAG, "RemoteException in error callback", re);
                }
            }
        }
    }

    @Override
    protected void onStopListening(Callback callback) {
        Log.d(TAG, "onStopListening called");
    }

    @Override
    protected void onCancel(Callback callback) {
        Log.d(TAG, "onCancel called");
        currentCallback = null;
    }

    // Called from RecognizeActivity when transcription is complete
    public static void sendResults(String text) {
        if (instance != null && instance.currentCallback != null && text != null && !text.isEmpty()) {
            Log.d(TAG, "VoiceRecognitionService sending results: " + text);

            Bundle results = new Bundle();
            java.util.ArrayList<String> matches = new java.util.ArrayList<>();
            matches.add(text);
            results.putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, matches);

            try {
                instance.currentCallback.results(results);
            } catch (android.os.RemoteException e) {
                Log.e(TAG, "RemoteException sending results", e);
            }
            instance.currentCallback = null;
        }
    }

    // Called from RecognizeActivity on cancel
    public static void sendCancelled() {
        if (instance != null && instance.currentCallback != null) {
            Log.d(TAG, "VoiceRecognitionService sending cancelled");
            try {
                instance.currentCallback.error(SpeechRecognizer.ERROR_NO_MATCH);
            } catch (android.os.RemoteException e) {
                Log.e(TAG, "RemoteException sending error", e);
            }
            instance.currentCallback = null;
        }
    }
}
