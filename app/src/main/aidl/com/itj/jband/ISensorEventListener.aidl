// ISensorEventListener.aidl
package com.itj.jband;

// Declare any non-default types here with import statements

interface ISensorEventListener {
    void onAccelerometerDataReceived(float x, float y, float z);
    void onStepCountReceived(int count);
    void onStepDetected();
}
