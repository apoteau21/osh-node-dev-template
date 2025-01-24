/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.sample.impl.sensor.turbidity;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.SWEHelper;

/**
 * Output specification and provider for {@link TurbiditySensor}.
 *
 * @author Ashley Poteau
 * @since Nov 21 2024
 */
public class TurbidityOutput extends AbstractSensorOutput<TurbiditySensor> implements Runnable {

    private static final String SENSOR_OUTPUT_NAME = "TDSOutput";
    private static final String SENSOR_OUTPUT_LABEL = "TDS Output";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "Output data for the TDS Meter Sensor";

    private static final Logger logger = LoggerFactory.getLogger(TurbidityOutput.class);

    private DataRecord dataRecord;
    private DataEncoding dataEncoding;

    private Boolean stopProcessing = false;
    private final Object processingLock = new Object();

    private static final int MAX_NUM_TIMING_SAMPLES = 10;
    private int setCount = 0;
    private final long[] timingHistogram = new long[MAX_NUM_TIMING_SAMPLES];
    private final Object histogramLock = new Object();

    private Thread worker;

    private TurbidityOutput output;

    /**
     * Constructor
     *
     * @param parentSensor Sensor driver providing this output
     */
    TurbidityOutput(TurbiditySensor parentSensor) {

        super(SENSOR_OUTPUT_NAME, parentSensor);

        logger.debug("Output created");
    }

    /**
     * Initializes the data structure for the output, defining the fields, their ordering,
     * and data types.
     */
    void doInit() {

        logger.debug("Initializing Output");

        initializeDataRecord();
        initializeDataEncoding();

        logger.debug("Initializing Output Complete");
    }

    /**
     * Sets the data for the sensor output.
     *
     * @param tdsVal long indicating the levels of total dissolved solids in the water
     */
    public void setData(long tdsVal) {
        long timestamp = System.currentTimeMillis();
        DataBlock dataBlock = latestRecord == null ? dataRecord.createDataBlock() : latestRecord.renew();

        tdsVal = (long) TurbidityVoltageReader.tdsValue;

        dataBlock.setDoubleValue(0, timestamp / 1000d);
        dataBlock.setLongValue(1, tdsVal);

        latestRecord = dataBlock;
        eventHandler.publish(new DataEvent(timestamp, TurbidityOutput.this, dataBlock));
    }

    /**
     * Initializes the data record for the sensor output.
     */
    private void initializeDataRecord() {
        SWEHelper sweHelper = new SWEHelper();

        dataRecord = sweHelper.createRecord()
                .name(SENSOR_OUTPUT_NAME)
                .label(SENSOR_OUTPUT_LABEL)
                .description(SENSOR_OUTPUT_DESCRIPTION)
                .addField("Timestamp", sweHelper.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Timestamp")
                        .description("Time of data collection"))
                .addField("TDS Values", sweHelper.createQuantity()
                        .label("TDS Values (mv, ppm)")
                        .description("The level of Total Dissolved Solids in the water."))
                .build();
    }

    /**
     * Initializes the data encoding for the sensor output.
     */
    private void initializeDataEncoding() {
        dataEncoding = new SWEHelper().newTextEncoding(",", "\n");
    }

    /**
     * Begins processing data for output
     */
    public void doStart() {

        // Instantiate a new worker thread
        worker = new Thread(this, this.name);

        // TODO: Perform other startup

        logger.info("Starting worker thread: {}", worker.getName());

        // Start the worker thread
        worker.start();
    }

    /**
     * Terminates processing data for output
     */
    public void doStop() {

        synchronized (processingLock) {

            stopProcessing = true;
        }

        // TODO: Perform other shutdown procedures
    }

    /**
     * Check to validate data processing is still running
     *
     * @return true if worker thread is active, false otherwise
     */
    public boolean isAlive() {

        return worker.isAlive();
    }

    @Override
    public DataComponent getRecordDescription() {

        return dataRecord;
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

    @Override
    public void run() {

        boolean processSets = true;

        long lastSetTimeMillis = System.currentTimeMillis();

        try {

            while (processSets) {

                DataBlock dataBlock;
                if (latestRecord == null) {

                    dataBlock = dataRecord.createDataBlock();

                } else {

                    dataBlock = latestRecord.renew();
                }

                synchronized (histogramLock) {

                    int setIndex = setCount % MAX_NUM_TIMING_SAMPLES;

                    // Get a sampling time for latest set based on previous set sampling time
                    timingHistogram[setIndex] = System.currentTimeMillis() - lastSetTimeMillis;

                    // Set latest sampling time to now
                    lastSetTimeMillis = timingHistogram[setIndex];
                }

                ++setCount;

                double timestamp = System.currentTimeMillis() / 1000d;

                // TODO: Populate data block
                dataBlock.setDoubleValue(0, timestamp);
                dataBlock.setStringValue(1, "Your data here");

                latestRecord = dataBlock;

                latestRecordTime = System.currentTimeMillis();

                eventHandler.publish(new DataEvent(latestRecordTime, TurbidityOutput.this, dataBlock));

                synchronized (processingLock) {

                    processSets = !stopProcessing;
                }
            }

        } catch (Exception e) {

            logger.error("Error in worker thread: {}", Thread.currentThread().getName(), e);

        } finally {

            // Reset the flag so that when driver is restarted loop thread continues
            // until doStop called on the output again
            stopProcessing = false;

            logger.debug("Terminating worker thread: {}", this.name);
        }
    }
}
