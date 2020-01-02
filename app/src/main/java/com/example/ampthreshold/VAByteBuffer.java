package com.example.ampthreshold;


public class VAByteBuffer {
    public static final int mCapacity = 65534;
    private byte[] mBuffer = new byte[mCapacity];
    private int mLength = 0;
    private boolean mLock = false;

    public void setLength(int aLength) {
        this.mLength = aLength;
    }

    public int length() {
        return this.mLength;
    }

    public boolean append(byte[] aData, int aOffset, int aLength) {
        int length = this.mLength;
        if (length + aLength > 65534) {
            return false;
        }
        int aCounter = aOffset;
        for (int i = length; i < length + aLength; i++) {
            this.mBuffer[i] = aData[aCounter];
            aCounter++;
        }
        this.mLength += aLength;
        return true;
    }

    public byte[] getBytes() {
        int length = this.mLength;
        byte[] res = new byte[length];
        for (int i = 0; i < length; i++) {
            res[i] = this.mBuffer[i];
        }
        return res;
    }

    public byte[] getFirstXBytes(int aLength) {
        int length = aLength;
        byte[] res = new byte[length];
        for (int i = 0; i < length; i++) {
            res[i] = this.mBuffer[i];
        }
        return res;
    }

    public void removeXBytesFromBeginning(int x) {
        int length = this.mLength;
        if (length > x) {
            for (int i = 0; i < length - x; i++) {
                this.mBuffer[i] = this.mBuffer[i + x];
            }
            this.mLength -= x;
            if (this.mLength < 0) {
                this.mLength = 0;
                return;
            }
            return;
        }
        this.mLength = 0;
    }

    public boolean isLocked() {
        return this.mLock;
    }

    public void setLock(boolean aLock) {
        this.mLock = aLock;
    }

    public void waitUntilUnlocked() {
        while (this.mLock) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
            }
        }
    }
}

