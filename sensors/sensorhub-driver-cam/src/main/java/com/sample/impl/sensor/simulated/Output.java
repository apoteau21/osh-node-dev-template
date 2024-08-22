/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.sample.impl.sensor.simulated;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord; // *****
import net.opengis.swe.v20.DataStream;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.DataBlockMixed;
import org.vast.data.AbstractDataBlock;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter; // *****
import org.sensorhub.api.sensor.SensorException;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import java.util.concurrent.atomic.AtomicBoolean;
import org.sensorhub.impl.sensor.videocam.VideoCamHelper;

/**
 * Output specification and provider for {@link Sensor}.
 *
 * @author ashley poteau
 * @since May 24, 2024
 */
public class Output extends AbstractSensorOutput<Sensor> implements Runnable {

    private static final String SENSOR_OUTPUT_NAME = "Camera Output";
    private static final String SENSOR_OUTPUT_LABEL = "Camera Sensor";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "Taking data from each frame of the camera feed and populate a data structure.";
    private static final String VIDEO_FORMAT = "h264";

    private static final Logger logger = LoggerFactory.getLogger(Output.class);

    private DataComponent dataStruct;
    private DataEncoding dataEncoding;

    private Boolean stopProcessing = false;
    private final Object processingLock = new Object();

    private static final int MAX_NUM_TIMING_SAMPLES = 10;
    private int setCount = 0;
    private final long[] timingHistogram = new long[MAX_NUM_TIMING_SAMPLES];
    private final Object histogramLock = new Object();

    private FrameGrabber frameGrabber;

    private long lastFrameTimeMS;

    protected AtomicBoolean doWork = new AtomicBoolean(false);

    private Thread worker;

    /**
     * Constructor
     *
     * @param parentSensor Sensor driver providing this output
     */
    Output(Sensor parentSensor) {

        super(SENSOR_OUTPUT_NAME, parentSensor);

        logger.debug("Output created");
    }

    /**
     * Initializes the data structure for the output, defining the fields, their ordering,
     * and data types.
     */
    void doInit() {

        logger.debug("Initializing Output");

        lastFrameTimeMS = System.currentTimeMillis(); // get current time in milliseconds

        try {
            frameGrabber = FrameGrabber.createDefault(0);
            frameGrabber.setFormat(VIDEO_FORMAT);

            // bro
            /*frameGrabber.setImageHeight(parentSensor.getConfiguration().videoParameters.videoFrameHeight);
            int videoFrameHeight = frameGrabber.getImageHeight();

            frameGrabber.setImageWidth(parentSensor.getConfiguration().videoParameters.videoFrameWidth);
            int videoFrameWidth = frameGrabber.getImageWidth();
             */

            // literally just gonna hardcode it in
//            int videoFrameHeight = 480;
//            int videoFrameWidth = 640;
//
//            frameGrabber.setImageHeight(videoFrameHeight);
//            frameGrabber.setImageWidth(videoFrameWidth);
//
//            // hallelujah
//
//            logger.debug("Video frame width: {}", videoFrameWidth);
//            logger.debug("Video frame height: {}", videoFrameHeight);
//
//            // Get an instance of SWE Factory suitable to build components
//            VideoCamHelper sweFactory = new VideoCamHelper();
//
//            DataStream outputDef = sweFactory.newVideoOutputMJPEG(getName(), videoFrameWidth, videoFrameHeight);
//
//            dataStruct = (DataRecord) outputDef.getElementType();
//
//            dataStruct.setLabel(SENSOR_OUTPUT_LABEL);
//            dataStruct.setDescription(SENSOR_OUTPUT_DESCRIPTION);
//
//            dataEncoding = outputDef.getEncoding();
//
//            dataEncoding = sweFactory.newTextEncoding(",", "\n");
//
//            logger.debug("Initializing Output Complete");

        } catch (FrameGrabber.Exception e) {
            logger.debug("Failed to establish connection with camera");
        } catch (Exception e) {
            logger.debug("unexpected error during output initialization");
        }

        frameGrabber.setFormat(VIDEO_FORMAT);
        int videoFrameHeight = 480;
        int videoFrameWidth = 640;

        frameGrabber.setImageHeight(videoFrameHeight);
        frameGrabber.setImageWidth(videoFrameWidth);

        logger.debug("Video frame width: {}", videoFrameWidth);
        logger.debug("Video frame height: {}", videoFrameHeight);

        // Get an instance of SWE Factory suitable to build components
        VideoCamHelper sweFactory = new VideoCamHelper();

        DataStream outputDef = sweFactory.newVideoOutputMJPEG(getName(), videoFrameWidth, videoFrameHeight);

        dataStruct = outputDef.getElementType();

        dataStruct.setLabel(SENSOR_OUTPUT_LABEL);
        dataStruct.setDescription(SENSOR_OUTPUT_DESCRIPTION);

        dataEncoding = outputDef.getEncoding();

        //dataEncoding = sweFactory.newTextEncoding(",", "\n");

        logger.debug("Initializing Output Complete");
    }

    /**
     * Begins processing data for output
     */
    public void doStart() throws SensorException {

        // Instantiate a new worker thread
        worker = new Thread(this, this.name);

        // TODO: Perform other startup

        logger.info("Starting worker thread: {}", worker.getName());

        if (frameGrabber != null) {
            try {
                frameGrabber.start();
                doWork.set(true);
                // Start the worker thread
                worker.start();
            } catch(FrameGrabber.Exception e) {
                //e.printStackTrace();
                logger.error("Failed to start FFMPEGFrameGrabber");
                throw new SensorException("Failed to start FFMPEGFrameGrabber");
            }
        } else {
            logger.error("Failed to create FFMEGFrameGrabber");
            throw new SensorException("Failed to create FFMPEGFrameGrabber");
        }
    }

    /**
     * Terminates processing data for output
     */
    //@Override // uncommenting this creates an error....
    public void doStop() {

        synchronized (processingLock) {

            stopProcessing = true;
        }

        // TODO: Perform other shutdown procedures

        if (frameGrabber != null) {
            try {
                /**doWork.set(false);
                worker.join();*/
                frameGrabber.stop();
            } catch(FrameGrabber.Exception e) {
                logger.error("Failed to stop FFMPEGFrameGrabber");
            }
        } else {
            logger.error("Failed to stop FFMPEGFrameGrabber");
        }
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

    @Override
    public void run() {

        boolean processSets = true;

        long lastSetTimeMillis = System.currentTimeMillis();

        try {
            while(processSets) {
                Frame frame = frameGrabber.grab();
                DataBlock dataBlock;
                synchronized (histogramLock) {

                    int setIndex = setCount % MAX_NUM_TIMING_SAMPLES;

                    // Get a sampling time for latest set based on previous set sampling time
                    timingHistogram[setIndex] = System.currentTimeMillis() - lastSetTimeMillis;

                    // Set latest sampling time to now
                    lastFrameTimeMS = timingHistogram[setIndex];
                }
                if (latestRecord == null) {

                    dataBlock = dataStruct.createDataBlock();

                } else {

                    dataBlock = latestRecord.renew();
                }
                double sampleTime = System.currentTimeMillis() / 1000.0;

                dataBlock.setDoubleValue(0, sampleTime);

                AbstractDataBlock frameData = ((DataBlockMixed) dataBlock).getUnderlyingObject()[1];

                BufferedImage image = new Java2DFrameConverter().convert(frame);
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

                byte[] imageData;

                ImageIO.write(image, "jpg", byteArrayOutputStream);
                byteArrayOutputStream.flush();

                imageData = byteArrayOutputStream.toByteArray();
                byteArrayOutputStream.close();

                frameData.setUnderlyingObject(imageData);

                latestRecord = dataBlock;

                latestRecordTime = System.currentTimeMillis();

                eventHandler.publish(new DataEvent(latestRecordTime, Output.this, dataBlock));

                synchronized (processingLock) {
                    processSets = !stopProcessing;
                }
            }



        } catch (IOException e) {
            StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            logger.error("error in worker thread: {} due to exception: {}", Thread.currentThread().getName());
        } finally {
            stopProcessing = false;
        }

        logger.debug("terminating worker {}", Thread.currentThread().getName());
    }
}