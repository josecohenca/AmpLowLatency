package com.example.ampthreshold;

import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Process;

import java.util.HashMap;
import java.util.Map;

import static android.media.AudioManager.GET_DEVICES_INPUTS;

//import com.google.android.gms.nearby.connection.Connections;


/* renamed from: ix.com.android.VirtualAmp.VAAmpThread */
public class VAAudioThread extends Thread {
    private VAByteBuffer echoBuffer = new VAByteBuffer();
    private VAByteBuffer echoBuffer2 = new VAByteBuffer();
    private float mAmplifier = 1.0f;
    private float[] mBuffer;
    //private byte[] outBuffer;
    private int mBufferSize;
    private int mEchoEffect = 0;
    private boolean mEchoEffectOn = false;
    public boolean mError = false;
    private int mFreqMod = 0;
    private boolean mFreqModOn = false;
    //private FileOutputStream mOutFile = null;
    private AudioTrack mPlayTrack;
    private AudioRecord mRecTrack;
    private boolean mStopped = false;
    private boolean mSilence = false;
    private long mSilenceTime = 0;
    private boolean isBTOn=false;
    private Context myContext;


    public void setBTState(boolean _set){
        isBTOn=_set;
    }

    VAAudioThread(Context _context) {
        this.myContext=_context;
    }

    public void initialize() {
        try {
            Process.setThreadPriority(-2);
        } catch (Exception e) {
        }

        AudioManager am = (AudioManager) this.myContext.getSystemService(Context.AUDIO_SERVICE);

        AudioDeviceInfo[] audioDeviceInfos = am.getDevices(GET_DEVICES_INPUTS);

        Map<String,int[]> sampleRates = new HashMap<>();
        for(AudioDeviceInfo f : audioDeviceInfos){
            sampleRates.put(f.getProductName().toString(),f.getSampleRates());
        }


        String sampleRateStr = am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
        int sampleRate = Integer.parseInt(sampleRateStr);
        if (sampleRate == 0) sampleRate = 48000; // Use a default value if property not found

        String framesPerBuffer = am.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
        int framesPerBufferInt = Integer.parseInt(framesPerBuffer);
        if (framesPerBufferInt == 0) framesPerBufferInt = 256; // Use default

        this.mBufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT);
        this.mBufferSize = (int)(Math.ceil(this.mBufferSize / (double)framesPerBufferInt) * framesPerBufferInt);

        this.mPlayTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT, this.mBufferSize, AudioTrack.MODE_STREAM);
        this.mRecTrack = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT, this.mBufferSize);
        this.mBuffer = new float[this.mBufferSize];
        if (this.mRecTrack.getState() == 0) {
            this.mError = true;
            return;
        }
        try {
            this.mRecTrack.startRecording();
            this.mError = false;
        } catch (IllegalStateException e2) {
            this.mError = true;
        }
    }


    private short[] byte2short(byte[] myData) {
        int byteArrsize = myData.length;
        short[] shorts = new short[byteArrsize / 2];

        //writeToFile("", false);
        for (int i = 0; i < byteArrsize; i++) {
            shorts[i / 2] += (i % 2 == 0 ? myData[i] : (short)(myData[i] << 8));
        }
        return shorts;
    }


    private byte[] short2byte(short[] myData) {
        int shortArrsize = myData.length;
        byte[] bytes = new byte[shortArrsize * 2];

        //writeToFile("", false);
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (myData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (myData[i] >> 8);

            //if (i>10000 && i <10100) {
            //    Log.i("Received value:", String.format("Amplitude is:  %d and bytes %d,%d at loop %d", sData[i],bytes[i * 2],bytes[(i * 2) + 1], i));
            //}
            //writeToFile(String.format("%s\n",Long.toString(sData[i] & 0xFFFF)), true);
            //sData[i] = 0;
        }
        return bytes;
    }


    public void run() {
        Integer valueOf = Integer.valueOf(0);
        if (!this.mError) {
            this.mPlayTrack.play();
        }
        while (!this.mStopped) {
            Integer size = Integer.valueOf(this.mRecTrack.read(this.mBuffer, 0, this.mBuffer.length, AudioRecord.READ_BLOCKING));

            //short[] inputData = byte2short(this.mBuffer);
            //short[] outData = new short[inputData.length/2];
            //remix(inputData, outData);
            //this.outBuffer = this.mBuffer;//short2byte(inputData);

            if (size.intValue() == -3 || size.intValue() == -2) {
                this.mStopped = true;
            } else if (size.intValue() == 0) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                if (this.mEchoEffectOn && ((!mSilence && System.currentTimeMillis()-mSilenceTime>1*1000) || mSilence) ) {
                    this.mBuffer = preFilter(this.mBuffer);
                }
                if (this.mAmplifier > 1.0f) {
                    this.mBuffer = preAmp(this.mBuffer);
                }
                //if (this.mFreqModOn) {
                //    this.mBuffer = freqMod(this.mBuffer);
                //}
                //if (this.mEchoEffectOn) {
                //    this.mBuffer = echoEffect(this.mBuffer);
                //}

                //if(isBTOn) {
                //this.outBuffer = this.mBuffer;
                this.mPlayTrack.write(this.mBuffer, 0, this.mBuffer.length,AudioTrack.WRITE_BLOCKING);
                    //if (this.mOutFile != null) {
                    //    try {
                    //        this.mOutFile.write(halfSample(this.mBuffer), 0, this.mBuffer.length / 2);
                    //    } catch (Exception e2) {
                    //        e2.printStackTrace();
                    //    }
                    //}
                //}
            }
        }
        if (!this.mError) {
            this.mPlayTrack.stop();
            this.mPlayTrack.release();
            this.mPlayTrack = null;
            this.mRecTrack.stop();
            this.mRecTrack.release();
            this.mRecTrack = null;
        }
    }

    private float[] preAmp(float[] pcmData) {
        float[] sData = new float[pcmData.length];
        for (int i2 = 0; i2 < pcmData.length - 1; i2 ++) {
            sData[i2] = (this.mAmplifier * pcmData[i2]);
            //sData[i2] = (short) (this.mAmplifier * ((float) sData[i2]));
        }
        return sData;
    }

    private float[] preFilter(float[] pcmData) {
        int i;
        int peakFactor = 4*10;
        byte b;
        float[] audioData = new float[pcmData.length];
        int countPeaks=0;
        for (int i2 = 0; i2 < pcmData.length - 1; i2 ++) {
            if(Math.abs(pcmData[i2])>256*32*(this.mEchoEffect/100.0f)){
                countPeaks++;
            }
        }

        for (int i2 = 0; i2 < pcmData.length - 1; i2 ++) {

            if(peakFactor*countPeaks<pcmData.length){
                audioData[i2]=0;
                mSilence=true;
                mSilenceTime = System.currentTimeMillis();
            }
            else {
                mSilence = false;
                audioData[i2]=pcmData[i2];
            }
        }
        return audioData;
    }

    private byte[] echoEffect(byte[] pcmData) {
        this.echoBuffer.append(pcmData, 0, pcmData.length);
        this.echoBuffer2.append(pcmData, 0, pcmData.length);
        if (this.echoBuffer.length() <= pcmData.length * 2) {
            return pcmData;
        }
        byte[] returnData = echoEffectDoRaw(pcmData, this.echoBuffer, 1.0f);
        if (this.echoBuffer2.length() > returnData.length * 4) {
            return echoEffectDoRaw(returnData, this.echoBuffer2, 0.7f);
        }
        return returnData;
    }

    private byte[] echoEffectDoRaw(byte[] pcmData, VAByteBuffer aEchoBuffer, float aGain) {
        short shortVal;
        int i;
        byte b;
        byte[] audioData = new byte[pcmData.length];
        byte[] echoData = aEchoBuffer.getFirstXBytes(pcmData.length);
        aEchoBuffer.removeXBytesFromBeginning(pcmData.length);
        for (int i2 = 0; i2 < pcmData.length - 1; i2 += 2) {
            if (pcmData[i2] < 0) {
                shortVal = (short) (((pcmData[i2 + 1] + 1) << 8) + pcmData[i2]);
            } else {
                shortVal = (short) ((pcmData[i2 + 1] << 8) + pcmData[i2]);
            }
            if (echoData[i2] < 0) {
                i = (echoData[i2 + 1] + 1) << 8;
                b = echoData[i2];
            } else {
                i = echoData[i2 + 1] << 8;
                b = echoData[i2];
            }
            short newVal = (short) ((int) (((float) shortVal) + ((((float) (this.mEchoEffect * ((short) (i + b)))) * aGain) / 100.0f)));
            byte pos2 = (byte) (newVal >> 8);
            audioData[i2] = (byte) newVal;
            audioData[i2 + 1] = pos2;
        }
        return audioData;
    }

    public void requestStopAndQuit() {
        this.mStopped = true;
    }

    private byte[] freqMod(byte[] pcmData) {
        int i;
        byte b;
        byte[] audioData = new byte[pcmData.length];
        for (int i2 = 0; i2 < pcmData.length - 1; i2 += 2) {
            if (pcmData[i2] < 0) {
                i = (pcmData[i2 + 1] + 1) << 8;
                b = pcmData[i2];
            } else {
                i = pcmData[i2 + 1] << 8;
                b = pcmData[i2];
            }
            short newVal = (short) ((int) (((double) ((short) (i + b))) * Math.cos(((6.283185307179586d * ((double) (this.mFreqMod + 10))) * ((double) i2)) / ((double) (pcmData.length / 2)))));
            byte pos2 = (byte) (newVal >> 8);
            audioData[i2] = (byte) newVal;
            audioData[i2 + 1] = pos2;
        }
        return audioData;
    }

    public void setFreqModOn(boolean aFreqModOn) {
        this.mFreqModOn = aFreqModOn;
    }

    public void setEchoEffect(int aEchoEffect) {
        this.mEchoEffect = aEchoEffect;
    }

    public void setFreqMod(int aFreqMod) {
        this.mFreqMod = aFreqMod;
    }

    public void setEchoEffectOn(boolean aEchoEffectOn) {
        this.mEchoEffectOn = aEchoEffectOn;
    }

    public void setAmplifier(float aAmplifier) {
        this.mAmplifier = aAmplifier;
    }


    private static final int SIGNED_SHORT_LIMIT = 32768;
    private static final int UNSIGNED_SHORT_MAX = 65535;

    public void remix(final short[] inSBuff, final short[] outSBuff) {
        // Down-mix stereo to mono
        // Viktor Toth's algorithm -
        // See: http://www.vttoth.com/CMS/index.php/technical-notes/68
        //      http://stackoverflow.com/a/25102339
        //final int inRemaining = inSBuff.length / 2;
        //final int outSpace = outSBuff.length;

        //final int samplesToBeProcessed = Math.min(inRemaining, outSpace);
        for (int i = 0; i < inSBuff.length / 2; ++i) {
            // Convert to unsigned
            final int a = inSBuff[2*i] + SIGNED_SHORT_LIMIT;
            final int b = inSBuff[2*i+1] + SIGNED_SHORT_LIMIT;
            int m;
            // Pick the equation
            if ((a < SIGNED_SHORT_LIMIT) || (b < SIGNED_SHORT_LIMIT)) {
                // Viktor's first equation when both sources are "quiet"
                // (i.e. less than middle of the dynamic range)
                m = a * b / SIGNED_SHORT_LIMIT;
            } else {
                // Viktor's second equation when one or both sources are loud
                m = 2 * (a + b) - (a * b) / SIGNED_SHORT_LIMIT - UNSIGNED_SHORT_MAX;
            }
            // Convert output back to signed short
            if (m == UNSIGNED_SHORT_MAX + 1) m = UNSIGNED_SHORT_MAX;
            outSBuff[i] = (short) (m - SIGNED_SHORT_LIMIT);
        }
    }


    public boolean getError() {
        return this.mError;
    }

    private byte[] halfSample(byte[] pcmData) {
        int i;
        byte b;
        byte[] audioData = new byte[(pcmData.length / 2)];
        for (int i2 = 0; i2 < pcmData.length - 3; i2 += 4) {
            if (pcmData[i2] < 0) {
                i = (pcmData[i2 + 1] + 1) << 8;
                b = pcmData[i2];
            } else {
                i = pcmData[i2 + 1] << 8;
                b = pcmData[i2];
            }
            short newVal = (short) (i + b);
            byte pos2 = (byte) (newVal >> 8);
            audioData[i2 / 2] = (byte) newVal;
            audioData[(i2 / 2) + 1] = pos2;
        }
        return audioData;
    }

    //public void setOutFile(FileOutputStream aFileOutputStream) {
    //    this.mOutFile = aFileOutputStream;
    //}
}
