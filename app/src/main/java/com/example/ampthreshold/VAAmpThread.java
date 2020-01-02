package com.example.ampthreshold;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioRouting;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Process;
//import com.google.android.gms.nearby.connection.Connections;
import java.io.FileOutputStream;

/* renamed from: ix.com.android.VirtualAmp.VAAmpThread */
public class VAAmpThread extends Thread {
    private VAByteBuffer echoBuffer = new VAByteBuffer();
    private VAByteBuffer echoBuffer2 = new VAByteBuffer();
    private float mAmplifier = 1.0f;
    private byte[] mBuffer;
    private int mBufferSize;
    private int mEchoEffect = 0;
    private boolean mEchoEffectOn = false;
    public boolean mError = false;
    private int mFreqMod = 0;
    private boolean mFreqModOn = false;
    private FileOutputStream mOutFile = null;
    private AudioTrack mPlayTrack;
    private AudioRecord mRecTrack;
    private boolean mStopped = false;
    private boolean mSilence = false;
    private long mSilenceTime = 0;
    private boolean isBTOn=false;


    public void setBTState(boolean _set){
        isBTOn=_set;
    }

    VAAmpThread() {
    }

    public void initialize() {
        try {
            Process.setThreadPriority(-2);
        } catch (Exception e) {
        }
        int intMinBufferSize = AudioTrack.getMinBufferSize(44100, 2, 2);
        int intMinBufferSizeRec = AudioRecord.getMinBufferSize(44100, 2, 2);
        //if (intMinBufferSize <= 0) {
        //    intMinBufferSize = Connections.MAX_RELIABLE_MESSAGE_LEN;
        //}
        if (intMinBufferSizeRec > intMinBufferSize) {
            this.mBufferSize = intMinBufferSizeRec;
        } else {
            this.mBufferSize = intMinBufferSize;
        }
        this.mPlayTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, 2, 2, this.mBufferSize, 1);
        this.mRecTrack = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, 2, 2, this.mBufferSize);
        this.mBuffer = new byte[this.mBufferSize];
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

    public void run() {
        Integer valueOf = Integer.valueOf(0);
        if (!this.mError) {
            this.mPlayTrack.play();
        }
        while (!this.mStopped) {
            Integer size = Integer.valueOf(this.mRecTrack.read(this.mBuffer, 0, this.mBuffer.length));
            if (size.intValue() == -3 || size.intValue() == -2) {
                this.mStopped = true;
            } else if (size.intValue() == 0) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                if (this.mEchoEffectOn && ((!mSilence && System.currentTimeMillis()-mSilenceTime>5*1000) || mSilence) ) {
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

                if(isBTOn) {
                    this.mPlayTrack.write(this.mBuffer, 0, size.intValue());
                    if (this.mOutFile != null) {
                        try {
                            this.mOutFile.write(halfSample(this.mBuffer), 0, this.mBuffer.length / 2);
                        } catch (Exception e2) {
                            e2.printStackTrace();
                        }
                    }
                }
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

    private byte[] preAmp(byte[] pcmData) {
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
            short oldVal = (short) (i + b);
            short newVal = (short) ((int) (this.mAmplifier * ((float) oldVal)));
            byte pos2 = (byte) (newVal >> 8);
            audioData[i2] = (byte) newVal;
            audioData[i2 + 1] = pos2;
        }
        return audioData;
    }

    private byte[] preFilter(byte[] pcmData) {
        int i;
        int peakFactor = 4*10;
        byte b;
        byte[] audioData = new byte[pcmData.length];
        int countPeaks=0;
        for (int i2 = 0; i2 < pcmData.length - 1; i2 += 2) {
            if (pcmData[i2] < 0) {
                i = (pcmData[i2 + 1] + 1) << 8;
                b = pcmData[i2];
            } else {
                i = pcmData[i2 + 1] << 8;
                b = pcmData[i2];
            }
            short newVal = (short) (i + b);
            if(Math.abs(newVal)>256*32*(this.mEchoEffect/100.0f)){
                countPeaks++;
            }
        }

        for (int i2 = 0; i2 < pcmData.length - 1; i2 += 2) {
            if (pcmData[i2] < 0) {
                i = (pcmData[i2 + 1] + 1) << 8;
                b = pcmData[i2];
            } else {
                i = pcmData[i2 + 1] << 8;
                b = pcmData[i2];
            }
            short newVal = (short) (i + b);
            if(peakFactor*countPeaks<pcmData.length){
                newVal=0;
                mSilence=true;
                mSilenceTime = System.currentTimeMillis();
            }
            else
                mSilence=false;

            byte pos2 = (byte) (newVal >> 8);
            audioData[i2] = (byte) newVal;
            audioData[i2 + 1] = pos2;
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

    public void setOutFile(FileOutputStream aFileOutputStream) {
        this.mOutFile = aFileOutputStream;
    }
}
