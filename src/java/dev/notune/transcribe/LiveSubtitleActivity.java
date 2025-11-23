package dev.notune.transcribe;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

public class LiveSubtitleActivity extends Activity {
    private static final String TAG = "LiveSubtitleActivity";
    private static final int PERMISSION_CODE = 1;
    private static final int OVERLAY_PERMISSION_CODE = 2;
    private MediaProjectionManager mProjectionManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_CODE);
            Toast.makeText(this, "Please grant Overlay permission for Subtitles", Toast.LENGTH_LONG).show();
        } else {
            startProjection();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == OVERLAY_PERMISSION_CODE) {
            if (Settings.canDrawOverlays(this)) {
                startProjection();
            } else {
                Toast.makeText(this, "Overlay permission denied", Toast.LENGTH_SHORT).show();
                finish();
            }
            return;
        }

        if (requestCode == PERMISSION_CODE) {
            if (resultCode != RESULT_OK) {
                Toast.makeText(this, "Screen Capture denied", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            Intent serviceIntent = new Intent(this, LiveSubtitleService.class);
            serviceIntent.setAction(LiveSubtitleService.ACTION_START);
            serviceIntent.putExtra("code", resultCode);
            serviceIntent.putExtra("data", data);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            finish();
        }
    }

    private void startProjection() {
        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), PERMISSION_CODE);
    }
}
