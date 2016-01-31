// ISenserService.aidl
package com.itj.jband;

// Declare any non-default types here with import statements
import com.itj.jband.ISensorEventListener;

interface ISenserService {
    void registerSensorEventListener(ISensorEventListener listener);
    void unregisterSensorEventListener(ISensorEventListener listener);
}
