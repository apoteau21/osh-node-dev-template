/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2021 Ashley Poteau
 All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.sample.impl.sensor.simulated;

import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorOutput;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
/**
 * Base Sensor Output
 *
 * @author Ashley Poteau
 * @since July 26 2024
 */
public abstract class BaseSensorOutput<SensorType extends ISensorModule<?>> extends AbstractSensorOutput<SensorType> implements Runnable {
    protected Thread workerThread;

    protected AtomicBoolean doWork = new AtomicBoolean(false);

    protected static final int MAX_NUM_TIMING_SAMPLES = 10;

    protected int dataFrameCount = 0;

    protected final long[] timingHistogram = new long[MAX_NUM_TIMING_SAMPLES];

    protected final Object histogramLock = new Object();

    protected long lastDataFrameTimeMillis;

    protected DataComponent dataStruct;

    protected DataEncoding dataEncoding;

    /**public BaseSensorOutput(SensorType parentSensor) throws IOException {
        super((Sensor) parentSensor);
    }*/

    public BaseSensorOutput(String name, SensorType parentSensor) throws IOException {
        super(name, parentSensor);
    }

    /**public BaseSensorOutput() throws IOException {
        super();
    }*/

    @Override
    public DataComponent getRecordDescription() {

        return dataStruct;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {

        return dataEncoding;
    }

    @Override
    public double getAverageSamplingPeriod() {

        long accumulator = 0;

        synchronized (histogramLock) {

            for (int idx = 0; idx < MAX_NUM_TIMING_SAMPLES; ++idx) {

                accumulator += timingHistogram[idx];
            }
        }

        return accumulator / (double) MAX_NUM_TIMING_SAMPLES;
    }

    protected abstract void doInit() throws SensorException;

    protected abstract void doStart() throws SensorException;
}
