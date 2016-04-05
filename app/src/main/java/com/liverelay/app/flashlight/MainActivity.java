package com.liverelay.app.flashlight;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;

import java.io.IOException;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int OFF_TIME = 5 * 60 * 1000;
    private static final int REQ_CAMERA_PERMISSION = 1;

    private ImageView mPowerButton;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private boolean isOn = false;
    private Camera mCamera;
    private Handler autoOffHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mPowerButton = (ImageView) findViewById(R.id.power_button_image_view);
        mPowerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        mSurfaceView = (SurfaceView) this.findViewById(R.id.dummy_surface_view);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);

        autoOffHandler = new Handler();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int i1, int i2, int i3) {
        mSurfaceHolder = holder;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSurfaceHolder = holder;
        if (requestPermission()) {
            initialize();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mSurfaceHolder = null;
    }

    private void toggle() {
        if (isOn) {
            turnOff();
        } else {
            if (requestPermission()) {
                turnOn();
            }
        }
    }

    private void initialize() {
        try {
            if (mCamera == null)
                mCamera = Camera.open();
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.startPreview();
            toggle();
        } catch (IOException e) {
            Log.w(TAG, "Failed on surfaceCreated: " + mSurfaceHolder);
        }
    }

    private void turnOn() {
        if (turnOnFlashLight()) {
            // Hide UI first
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.hide();
            }

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mPowerButton.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

            mPowerButton.setColorFilter(Color.argb(255, 96, 224, 96));
            isOn = true;
        }
    }

    @SuppressLint("InlinedApi")
    private void turnOff() {
        turnOffFlashLight();
        mPowerButton.setColorFilter(Color.argb(255, 240, 240, 240));

        // Show the system bar
        mPowerButton.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        isOn = false;
    }

    public boolean turnOnFlashLight() {
        if (mCamera == null) {
            mCamera = Camera.open();
            mCamera.startPreview();
        }
        autoOffHandler.removeCallbacksAndMessages(null);
        autoOffHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                turnOff();
            }
        }, OFF_TIME);
        return setFlashMode(true);
    }

    public boolean turnOffFlashLight() {
        autoOffHandler.removeCallbacksAndMessages(null);
        return setFlashMode(false);
    }

    private boolean setFlashMode(boolean setOn) {
        try {
            Camera.Parameters parameter = mCamera.getParameters();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
                parameter.setFlashMode(setOn ? Camera.Parameters.FLASH_MODE_TORCH
                        : Camera.Parameters.FLASH_MODE_OFF);
            } else {
                parameter.set("flash-mode", setOn ? "torch" : "off");
            }
            mCamera.setParameters(parameter);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Failed to setFlashMode " + setOn, e);
        }
        return false;
    }

    private boolean requestPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED)
            return true;

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA}, REQ_CAMERA_PERMISSION);
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQ_CAMERA_PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initialize();
                } else {
                }
                return;
            }
        }
    }
}
