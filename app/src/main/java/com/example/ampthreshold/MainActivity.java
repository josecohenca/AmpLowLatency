package com.example.ampthreshold;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Process;
import android.text.Html;
import android.text.Spanned;
import android.text.format.Time;
import android.text.method.LinkMovementMethod;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
//import com.google.android.gms.ads.AdListener;
//import com.google.android.gms.ads.AdRequest;
//import com.google.android.gms.ads.AdView;
//import com.google.android.gms.ads.InterstitialAd;
//import com.google.android.gms.location.places.Place;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/* renamed from: ix.com.android.VirtualAmp.VirtualAmp */
public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener, SurfaceHolder.Callback {
    private static final int REQUEST_AUDIO_PERMISSION_RESULT = 55432;
    private static int mAmpProgress = 0;
    static SeekBar mAmpSeekBar;
    static SurfaceView mAmpView;
    private static boolean mEchoEffectOn = false;
    static SeekBar mEchoEffectOnOffSeekBar;
    private static int mEchoEffectProgress = 0;
    static SeekBar mEchoEffectSeekBar;
    private static boolean mFreqModOn = false;
    static SeekBar mFreqModOnOffSeekBar;
    private static int mFreqModProgress = 0;
    static SeekBar mFreqModSeekBar;
    public static Dialog mLoadingScreenDialog;
    static SeekBar mOnOffSeekBar;
    private static boolean mPowerOn = false;

    private static final int MY_PERMISSIONS_REQUEST_CODE = 33123;

    private SurfaceHolder mHolder;


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_AUDIO_PERMISSION_RESULT) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(),
                        "Application will not have audio on record", Toast.LENGTH_SHORT).show();
            }
        }
    }


    protected void checkPermission(){
        final Activity myActivity = this;
        List<String> permissionList = new LinkedList<>();
        if(ContextCompat.checkSelfPermission(myActivity,Manifest.permission.RECORD_AUDIO)!= PackageManager.PERMISSION_GRANTED)
            permissionList.add(Manifest.permission.RECORD_AUDIO);
        if(ContextCompat.checkSelfPermission(myActivity,Manifest.permission.WAKE_LOCK)!= PackageManager.PERMISSION_GRANTED)
            permissionList.add(Manifest.permission.WAKE_LOCK);
        if(ContextCompat.checkSelfPermission(myActivity,Manifest.permission.MODIFY_AUDIO_SETTINGS)!= PackageManager.PERMISSION_GRANTED)
            permissionList.add(Manifest.permission.MODIFY_AUDIO_SETTINGS);
        if(ContextCompat.checkSelfPermission(myActivity,Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)!= PackageManager.PERMISSION_GRANTED)
            permissionList.add(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        if(ContextCompat.checkSelfPermission(myActivity,Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        //if(ContextCompat.checkSelfPermission(mActivity,Manifest.permission.FOREGROUND_SERVICE)!= PackageManager.PERMISSION_GRANTED)
        //    permissionList.add(Manifest.permission.FOREGROUND_SERVICE);

        if(permissionList.size()>0){
            final String[] permissionArray = (String[])permissionList.toArray(new String[0]);
            // Do something, when permissions not granted
            if(ActivityCompat.shouldShowRequestPermissionRationale(myActivity,Manifest.permission.WAKE_LOCK)
                    || ActivityCompat.shouldShowRequestPermissionRationale(myActivity,Manifest.permission.MODIFY_AUDIO_SETTINGS)
                    || ActivityCompat.shouldShowRequestPermissionRationale(myActivity,Manifest.permission.RECORD_AUDIO)
                    || ActivityCompat.shouldShowRequestPermissionRationale(myActivity,Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    //|| ActivityCompat.shouldShowRequestPermissionRationale(mActivity,Manifest.permission.FOREGROUND_SERVICE)
                    || ActivityCompat.shouldShowRequestPermissionRationale(myActivity,Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                // If we should give explanation of requested permissions

                // Show an alert dialog here with request explanation
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Location, Read/Write External" +
                        " Storage permissions are required to do the task.");
                builder.setTitle("Please grant those permissions");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ActivityCompat.requestPermissions(
                                myActivity,
                                permissionArray,
                                MY_PERMISSIONS_REQUEST_CODE
                        );
                    }
                });
                builder.setNeutralButton("Cancel",null);
                AlertDialog dialog = builder.create();
                dialog.show();
            }else{
                // Directly request for required permissions, without explanation
                ActivityCompat.requestPermissions(
                        myActivity,
                        permissionArray,
                        MY_PERMISSIONS_REQUEST_CODE
                );
            }
        }else {
            // Do something, when permissions are already granted
            Toast.makeText(this,"Permissions already granted",Toast.LENGTH_SHORT).show();
        }
    }


    public void openOptionsMenu() {
        super.openOptionsMenu();
        Configuration config = getResources().getConfiguration();
        if ((config.screenLayout & 15) > 3) {
            int originalScreenLayout = config.screenLayout;
            config.screenLayout = 3;
            super.openOptionsMenu();
            config.screenLayout = originalScreenLayout;
            return;
        }
        super.openOptionsMenu();
    }

    private void addOptionalMenuButton() {
        try {
            if (!ViewConfiguration.get(this).hasPermanentMenuKey()) {
                Button buttonoptions = new Button(this);
                buttonoptions.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        com.example.ampthreshold.MainActivity.this.openOptionsMenu();
                    }
                });
                //buttonoptions.setBackgroundResource(R.drawable.ic_menu_moreoverflow_normal_holo_dark);
                RelativeLayout overflowmenubutton = new RelativeLayout(this);
                overflowmenubutton.setGravity(53);
                overflowmenubutton.addView(buttonoptions);
                getWindow().addContentView(overflowmenubutton, new ViewGroup.LayoutParams(-1, -1));
            }
        } catch (NoSuchMethodError e) {
        }
    }

    public void onStart() {
        super.onStart();
    }

    public void onStop() {
        super.onStop();
    }


    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 4) {
            doExit();
        }
        return super.onKeyDown(keyCode, event);
    }

    public void onResume() {
        super.onResume();
        //if (this.mAdView != null) {
        //    this.mAdView.resume();
        //}
    }

    public void onPause() {
        super.onPause();
    }

    /* access modifiers changed from: protected */
    public void onDestroy() {
        super.onDestroy();
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //getWindow().setFlags(Place.TYPE_SUBLOCALITY_LEVEL_2, Place.TYPE_SUBLOCALITY_LEVEL_2);
        setVolumeControlStream(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
        setContentView(R.layout.activity_main);
        //getWindow().setBackgroundDrawable(new ColorDrawable(-16777216));
        drawUI();
        initControls();
        checkPermission();
        addOptionalMenuButton();

        boolean hasLowLatencyFeature =
                getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUDIO_LOW_LATENCY);

        boolean hasProFeature =
                getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUDIO_PRO);

    }

    private void drawUI() {
        mAmpSeekBar = (SeekBar) findViewById(R.id.SeekBar01);
        mAmpSeekBar.setOnSeekBarChangeListener(this);
        mOnOffSeekBar = (SeekBar) findViewById(R.id.SeekBar02);
        mOnOffSeekBar.setOnSeekBarChangeListener(this);
        mEchoEffectOnOffSeekBar = (SeekBar) findViewById(R.id.SeekBar03);
        mEchoEffectOnOffSeekBar.setOnSeekBarChangeListener(this);
        mFreqModOnOffSeekBar = (SeekBar) findViewById(R.id.SeekBar04);
        mFreqModOnOffSeekBar.setOnSeekBarChangeListener(this);
        mEchoEffectSeekBar = (SeekBar) findViewById(R.id.SeekBar05);
        mEchoEffectSeekBar.setOnSeekBarChangeListener(this);
        mFreqModSeekBar = (SeekBar) findViewById(R.id.SeekBar06);
        mFreqModSeekBar.setOnSeekBarChangeListener(this);
        mAmpView = (SurfaceView) findViewById(R.id.SurfaceView01);
    }

    private void initControls() {
        mAmpSeekBar.setProgress(mAmpProgress);
        mEchoEffectSeekBar.setProgress(mEchoEffectProgress);
        mFreqModSeekBar.setProgress(mFreqModProgress);
        if (!mPowerOn) {
            mOnOffSeekBar.setProgress(0);
        } else {
            mOnOffSeekBar.setProgress(1);
        }
        if (!mEchoEffectOn) {
            mEchoEffectOnOffSeekBar.setProgress(0);
        } else {
            mEchoEffectOnOffSeekBar.setProgress(1);
        }
        if (!mFreqModOn) {
            mFreqModOnOffSeekBar.setProgress(0);
        } else {
            mFreqModOnOffSeekBar.setProgress(1);
        }
        this.mHolder = mAmpView.getHolder();
        this.mHolder.addCallback(this);
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        onCreateOptionsMenu(menu);
        return true;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (MainService.isRecording()) {
            menu.add(0, 3, 0, "Stop Recording");//.setIcon(R.drawable.mimic_red);
        } else {
            menu.add(0, 3, 0, "Start Recording");//.setIcon(R.drawable.mimic);
        }
        menu.add(0, 2, 0, "About");//.setIcon(R.drawable.miabout);
        menu.add(0, 1, 0, "Exit");//.setIcon(R.drawable.miexit);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                doExit();
                return true;
            case 2:
                showAbout();
                return true;
            case 3:
                if (MainService.isRecording()) {
                    this.stopRecording();
                    return true;
                }
                this.startRecording();
                return true;
            default:
                return false;
        }
    }


    public void showError02() {
        new AlertDialog.Builder(this).setTitle("Error").setMessage("Access to SDCard failed.\nPlease make sure it is not locked by a pc connection or another app.\n").setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
            }
        }).show();
    }


    public void showAbout() {
        try {
            DialogInterface.OnClickListener vrecButtonListener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface arg0, int arg1) {
                    com.example.ampthreshold.MainActivity.this.startActivity(new Intent("android.intent.action.VIEW", Uri.parse("market://search?q=pname:ix.com.android.VirtualRecorder")));
                }
            };
            DialogInterface.OnClickListener okButtonListener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface arg0, int arg1) {
                }
            };
            CharSequence styledResults = Html.fromHtml(Html.toHtml((Spanned) getResources().getText(R.string.about)));
            AlertDialog infoDialog = new AlertDialog.Builder(this).create();
            infoDialog.setVolumeControlStream(3);
            infoDialog.setTitle("About");
            infoDialog.setMessage(styledResults);
            infoDialog.setButton(-1, "Close", okButtonListener);
            infoDialog.setButton(-2, "Download Virtual Recorder", vrecButtonListener);
            infoDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* access modifiers changed from: private */
    public void doExit() {
        this.stopRecording();
        //if (amp != null) {
        //    try {
        //        amp.requestStopAndQuit();
        //        Thread.sleep(100);
        //        amp = null;
        //    } catch (Exception e) {
        //    }
        //}
        mPowerOn = false;
        mEchoEffectOn = false;
        mFreqModOn = false;
        initControls();
        finish();
    }


    public void startRecording() {
        if(!MainService.isRecording()) {
            Intent serviceIntent = new Intent(this, MainService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        }
    }


    public void stopRecording() {
        if(MainService.isRecording()) {
            Intent serviceIntent = new Intent(this, MainService.class);
            stopService(serviceIntent);
        }
    }

    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (seekBar.getId() == R.id.SeekBar01) {
            if (MainService.isRecording() && fromUser) {
                MainService.setAmplifier((((float) progress) + 100.0f) / 100.0f);
            }
            mAmpProgress = progress;
        } else if (seekBar.getId() == R.id.SeekBar02) {
            if (progress > 0 && !mPowerOn && fromUser) {
                mPowerOn = true;
                startRecording();
                MainService.setAmplifier((((float) mAmpSeekBar.getProgress()) + 100.0f) / 100.0f);
                MainService.setEchoEffectOn(mEchoEffectOn);
                MainService.setEchoEffect(mEchoEffectProgress);
                MainService.setFreqModOn(mFreqModOn);
                MainService.setFreqMod(mFreqModProgress);
                if (MainService.getAmpError()) {
                    showError01();
                }
            } else if (mPowerOn && fromUser) {
                if (MainService.isRecording()) {
                    stopRecording();
                }
                mPowerOn = false;
            }
        } else if (seekBar.getId() == R.id.SeekBar03) {
            if (progress > 0 && fromUser) {
                mEchoEffectOn = true;
                if (MainService.isRecording()) {
                    MainService.setEchoEffectOn(true);
                }
            } else if (fromUser) {
                mEchoEffectOn = false;
                if (MainService.isRecording()) {
                    MainService.setEchoEffectOn(false);
                }
            }
        } else if (seekBar.getId() == R.id.SeekBar04) {
            if (progress > 0 && fromUser) {
                mFreqModOn = true;
                if (MainService.isRecording()) {
                    MainService.setFreqModOn(true);
                }
            } else if (fromUser) {
                mFreqModOn = false;
                if (MainService.isRecording()) {
                    MainService.setFreqModOn(false);
                }
            }
        } else if (seekBar.getId() == R.id.SeekBar05) {
            if (MainService.isRecording() && fromUser) {
                MainService.setEchoEffect(progress);
                mEchoEffectProgress = progress;
            }
        } else if (seekBar.getId() == R.id.SeekBar06 && MainService.isRecording() && fromUser) {
            MainService.setFreqMod(progress);
            mFreqModProgress = progress;
        }
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    public void surfaceCreated(SurfaceHolder holder) {
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    public void showError01() {
        new AlertDialog.Builder(this).setTitle("Error").setMessage("Recording device failed.\nPerhaps your phone does not support it, is crashed or an app is recording already.\nPlease restart your phone.").setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
            }
        }).show();
    }
}
