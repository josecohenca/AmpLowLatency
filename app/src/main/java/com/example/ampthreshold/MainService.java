package com.example.ampthreshold;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.text.format.Time;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MainService extends Service {
    private String TAG = "VAAmpService";
    public static VAAmpThread amp = null;
    private String notifChannelId = "my_channel_ampservice";
    private NotificationChannel mChannel;
    private NotificationManager mNotificationManager;
    private BluetoothManager mBluetoothManager;
    private static Context myContext;
    private Notification notification;
    private static int FOREGROUND_ID = 3119;
    private static AudioManager mAudioMgr;

    private static boolean mIsRecording = false;
    //private static FileOutputStream mOutFile = null;

    public static boolean isRecording() { return mIsRecording; }
    private final String VR_DIRECTORY = new StringBuilder(String.valueOf(Environment.getExternalStorageDirectory().toString())).append("/VirtualRecorder/").toString();

    /* renamed from: pm */
    private static PowerManager pm = null;

    /* renamed from: wl */
    private static PowerManager.WakeLock wl = null;

    private boolean mAudioPlugRegistered = false;
    //private boolean oldBTState = false;
/*
    @TargetApi(Build.VERSION_CODES.M)
    private AudioDeviceCallback createAudioDeviceCallback() {

        return new AudioDeviceCallback() {
            private HashMap<Integer,Integer> mDevices = new HashMap<>();

            private void onAudioDevicesChanged() {
                boolean newBTState = false;
                for (Map.Entry<Integer,Integer> entry : mDevices.entrySet()){
                    if(entry.getValue()==AudioDeviceInfo.TYPE_BLUETOOTH_A2DP){
                        newBTState=true;
                        break;
                    }
                }
                if(oldBTState!=newBTState) {
                    oldBTState = newBTState;
                    amp.setBTState(oldBTState);
                }
            }

            @RequiresApi(Build.VERSION_CODES.M)
            @Override
            public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
                for (AudioDeviceInfo info : addedDevices) {
                    if (!info.isSink())
                        continue;
                    mDevices.put(info.getId(),info.getType());
                }
                onAudioDevicesChanged();
            }


            @RequiresApi(Build.VERSION_CODES.M)
            @Override
            public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
                for (AudioDeviceInfo info : removedDevices) {
                    if (!info.isSink())
                        continue;
                    mDevices.remove(info.getId());
                }
                onAudioDevicesChanged();
            }
        };
    }

    private final AudioDeviceCallback mAudioDeviceCallback = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? createAudioDeviceCallback() : null;

    @TargetApi(Build.VERSION_CODES.M)
    private void registerAudioPlugV23(boolean register) {
        AudioManager am = (AudioManager) myContext.getSystemService(Context.AUDIO_SERVICE);
        if (register) {
            mAudioDeviceCallback.onAudioDevicesAdded(am.getDevices(AudioManager.GET_DEVICES_OUTPUTS));
            am.registerAudioDeviceCallback(mAudioDeviceCallback, null);
        } else {
            am.unregisterAudioDeviceCallback(mAudioDeviceCallback);
        }
    }

    private void registerAudioPlug(boolean register) {
        if (register == mAudioPlugRegistered)
            return;
        if (mAudioDeviceCallback != null)
            registerAudioPlugV23(register);
        mAudioPlugRegistered = register;
    }

*/
    @Override
    public void onCreate() {
        myContext = this.getApplicationContext();
        mNotificationManager = (NotificationManager) myContext.getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
        //    mBluetoothManager = (BluetoothManager)myContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mAudioMgr = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

        CharSequence name = getText(R.string.remote_service_started);
        int importance = NotificationManager.IMPORTANCE_LOW;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mChannel = new NotificationChannel(notifChannelId, name, importance);
            mNotificationManager.createNotificationChannel(mChannel);
        }

        //registerNetworkCallback();

        CharSequence text = getText(R.string.remote_service_started);

        Intent notifIntent = new Intent(myContext, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(myContext, 0, notifIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Set the info for the views that show in the notification panel.

        notification = new NotificationCompat.Builder(this, notifChannelId)
                .setSmallIcon(R.drawable.miabout)  // the status icon
                .setTicker(text)  // the status text
                .setOngoing(true)
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle(getText(R.string.local_service_label))  // the label of the entry
                .setContentText(text)  // the contents of the entry
                .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
                .build();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        // Start foreground service.
        startForeground(FOREGROUND_ID, notification);
        Log.i(TAG, "Start foreground");
        mIsRecording=true;
        mNotificationManager.notify(FOREGROUND_ID, notification);

        if (pm == null && wl == null) {
            pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VirtualRecorder:");
        }


        /*
        File standardDirectory = new File(VR_DIRECTORY);
        if (!standardDirectory.isDirectory()) {
            standardDirectory.mkdir();
        }
        Time time = new Time();
        time.setToNow();
        String str = "";
        try {
            File oFile = new File(new StringBuilder(String.valueOf(VR_DIRECTORY + time.format("VAmp_%Y%m%d_%H%M%S"))).append(".pcm").toString());
            if (!oFile.exists()) {
                oFile.createNewFile();
            }

            mOutFile = new FileOutputStream(oFile);
        } catch (Exception e) {
            Log.e(TAG, e.toString()+". StackTrace: "+Log.getStackTraceString(e));
        }
        */

        //AudioManager am = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);
        //am.setMode(AudioManager.MODE_IN_COMMUNICATION);
        //am.setSpeakerphoneOn(false);

        amp = new VAAmpThread(myContext);
        amp.initialize();
        amp.start();
        //amp.setOutFile(this.mOutFile);

        //oldBTState=isBTOn();
        //amp.setBTState(oldBTState);

        //registerAudioPlug(true);

        return Service.START_STICKY;
    }

    public static void setAmplifier(float i){
        if(amp!=null) amp.setAmplifier(i);
    }

    public static void setEchoEffectOn(boolean i){
        if(amp!=null) amp.setEchoEffectOn(i);
    }

    public static void setEchoEffect(int i){
        if(amp!=null) amp.setEchoEffect(i);
    }

    public static void setFreqModOn(boolean i){
        if(amp!=null) amp.setFreqModOn(i);
    }

    public static void setFreqMod(int i){
        if(amp!=null) amp.setFreqMod(i);
    }


    public static boolean getAmpError(){
        boolean ret=false;
        if(amp!=null) ret=amp.getError();
        return ret;
    }

    @SuppressWarnings("deprecation")
    public static boolean isBTOn() {
        AudioManager mAudioManager = (AudioManager) myContext.getSystemService(Context.AUDIO_SERVICE);

        // Devices we consider to not be speakers.
        final Integer[] headsetTypes = { AudioDeviceInfo.TYPE_BLUETOOTH_A2DP};
        boolean result = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioDeviceInfo[] devices = mAudioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            for (AudioDeviceInfo device: devices) {
                Log.v("VanillaMusic", "AudioDeviceInfo type = " + device.getType());
                if (Arrays.asList(headsetTypes).contains(device.getType())) {
                    result = true;
                    break;
                }
            }
        } else {
            result = mAudioManager.isBluetoothA2dpOn();
        }
        return result;
    }


    @Override
    public IBinder onBind(Intent arg0) {
        Log.i(TAG, "Service onBind");
        return null;
    }

    @Override
    public void onDestroy() {
        //registerAudioPlug(false);
        mIsRecording=false;
        amp.requestStopAndQuit();
        //if (amp != null) {
        //    amp.setOutFile(null);
        //}
        //try {
        //    this.mOutFile.close();
        //} catch (Exception e) {
        //    Log.e(TAG, e.toString()+". StackTrace: "+Log.getStackTraceString(e));
        //}
        //this.mOutFile = null;

        if (wl != null && wl.isHeld()) {
            wl.release();
        }
    }

}
