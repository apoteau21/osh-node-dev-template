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
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.SWEHelper;

import org.vosk.Recognizer;
import org.vosk.Model;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Output specification and provider for {@link Sensor}.
 *
 * @author ashley poteau
 * @since july 11 2024
 */
public class Output extends BaseSensorOutput<Sensor> implements Runnable, AudioTranscriptListener {

    private static final String SENSOR_OUTPUT_NAME = "Output";
    private static final String SENSOR_OUTPUT_LABEL = "Audio Output";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "A transcription of audio given by the user.";

    private static final Logger logger = LoggerFactory.getLogger(Output.class);

    private DataRecord dataStruct;
    private DataEncoding dataEncoding;
    private DataBlock latestRecord;
    private long latestRecordTime;

    private Boolean stopProcessing = false;
    private final Object processingLock = new Object();

    private static final int MAX_NUM_TIMING_SAMPLES = 10;
    private int setCount = 0;
    private final long[] timingHistogram = new long[MAX_NUM_TIMING_SAMPLES];
    private final Object histogramLock = new Object();

    private Thread worker;
    private SpeechProcessor speechProcessor;

    BlockingQueue<String> dataBufferQueue = new LinkedBlockingQueue<>();
    private Model model;
    private Recognizer recognizer;
    //String finResult = recognizer.getResult();

    /**
     * Constructor
     *
     * @param parentSensor Sensor driver providing this output
     */
    Output(Sensor parentSensor, LanguageModel languageModel) throws IOException {

        super(SENSOR_OUTPUT_NAME, parentSensor);

        logger.debug("Output created");

        String modelPath = languageModel.getModelPath();
        model = new Model(modelPath);

        recognizer = new Recognizer(model, 16000f);
        //worker = new Thread(this, worker.getName());

        speechProcessor = new SpeechProcessor(SpeechRecognizerType.LIVE, languageModel, null);
        speechProcessor.addListener(this);

    }


    /**
     * Initializes the data structure for the output, defining the fields, their ordering,
     * and data types.
     */
    //@Override
    protected void doInit() {

        logger.debug("Initializing Output");

        // Get an instance of SWE Factory suitable to build components
        SWEHelper sweFactory = new SWEHelper();

        // TODO: Create data record description
        dataStruct = sweFactory.createRecord()
                .name(SENSOR_OUTPUT_NAME)
                .definition(SWEHelper.getPropertyUri("TranscribedText"))
                .label(SENSOR_OUTPUT_LABEL)
                .description(SENSOR_OUTPUT_DESCRIPTION)
                .addField("sampleTime", sweFactory.createTime()
                        .asSamplingTimeIsoUTC()
                        .build())
                .addField("Audio Transcription", sweFactory.createText()
                        .label("Text Transcription")
                        .description("The result of the audio to text transcription engine.")
                        .build())
                .build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");

        logger.debug("Initializing Output Complete");
    }

    /**
     * Begins processing data for output
     */
    //@Override
    public void doStart() {
        logger.debug("Starting");

        // Instantiate a new worker thread. if null create one
        if (worker == null || !worker.isAlive()) {
            worker = new Thread(this);
        }

        // start speech processor thread
        if (speechProcessor != null && !speechProcessor.isAlive()) {
            speechProcessor.start();
            logger.info("SpeechProcessor thread started.");
        }

        logger.info("Starting worker thread: {}", worker.getName());

        doWork.set(true);

        // Start the worker thread
        worker.start();
        logger.debug("Started worker thread: {}", worker.getName());
    }

    /**
     * Terminates processing data for output
     */
    public void doStop() {
        logger.debug("Stopping");

        doWork.set(false);

        if (speechProcessor != null) {
            try {
                speechProcessor.stopProcessingStream(); // Stop the SpeechProcessor
            } catch (IOException e) {
                logger.error("Error stopping SpeechProcessor: {}", e.getMessage());
            }
        }


            logger.debug("Stopped");
    }

    /**
     * Check to validate data processing is still running
     *
     * @return true if worker thread is active, false otherwise
     */
    public boolean isAlive() {

        return worker.isAlive();
    }

    //@Override
    public DataComponent getRecordDescription() {

        return dataStruct;
    }

    //@Override
    public DataEncoding getRecommendedEncoding() {

        return dataEncoding;
    }

    //@Override
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

//        boolean processSets = true;

        //long lastSetTimeMillis = System.currentTimeMillis();

//        try {
//
////            while (doWork.get()) {
////
////                //String result = dataBufferQueue.take();
////
////                //String finResult = recognizer != null ? recognizer.getResult() : "Recognizer not initialized";
////
////                String finResult = recognizer.getResult();
////
////                finResult = dataBufferQueue.take();
////
////                DataBlock dataBlock;
////                if (latestRecord == null) {
////
////                    dataBlock = dataStruct.createDataBlock();
////
////                } else {
////
////                    dataBlock = latestRecord.renew();
////                }
////
////                synchronized (histogramLock) {
////
////                    int setIndex = setCount % MAX_NUM_TIMING_SAMPLES;
////
////                    // Get a sampling time for latest set based on previous set sampling time
////                    timingHistogram[setIndex] = System.currentTimeMillis() - lastSetTimeMillis;
////
////                    // Set latest sampling time to now
////                    lastSetTimeMillis = timingHistogram[setIndex];
////                }
////
////                ++setCount;
////
////                double timestamp = System.currentTimeMillis() / 1000d;
////
////                // TODO: Populate data block
////                dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000.0);
////
////                dataBlock.setStringValue(1, finResult); // get result!
////
////                latestRecord = dataBlock;
////
////                latestRecordTime = System.currentTimeMillis();
////
////                eventHandler.publish(new DataEvent(latestRecordTime, Output.this, dataBlock));
////
////                synchronized (processingLock) {
////
////                    processSets = !stopProcessing;
////                }
////            }
//
//        } catch (Exception e) {
//
//            logger.error("Error in worker thread: {}", Thread.currentThread().getName(), e);
//
//        } finally {
//
//            // Reset the flag so that when driver is restarted loop thread continues
//            // until doStop called on the output again
//            stopProcessing = false;
//
//            logger.debug("Terminating worker thread: {}", Thread.currentThread().getName());
//        }
    }

    @Override
    public void onTranscribedAudio(String finResult) {

        boolean processSets = true;
        long lastSetTimeMillis = System.currentTimeMillis();

        try {

            while (doWork.get()) {

                //String result = dataBufferQueue.take();

                //String finResult = recognizer != null ? recognizer.getResult() : "Recognizer not initialized";

                finResult = recognizer.getResult();

                finResult = dataBufferQueue.take();

                DataBlock dataBlock;
                if (latestRecord == null) {

                    dataBlock = dataStruct.createDataBlock();

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

                logger.debug("beginning to populate data block");

                // TODO: Populate data block
                dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000.0);

                dataBlock.setStringValue(1, finResult); // get result!

                latestRecord = dataBlock;

                latestRecordTime = System.currentTimeMillis();

                eventHandler.publish(new DataEvent(latestRecordTime, Output.this, dataBlock));

                synchronized (processingLock) {

                    processSets = !stopProcessing;
                }
            }

            getLogger().info("Output receiving result: {}", finResult);

            dataBufferQueue.put(finResult);

            logger.debug("finished populating data block");

        } catch (InterruptedException e) {

            logger.error("Error in worker thread: {} due to exception: {}", Thread.currentThread().getName(), e.toString());
        } catch (Exception e) {

            logger.error("Error in worker thread: {}", Thread.currentThread().getName(), e);

        } finally {

            // Reset the flag so that when driver is restarted loop thread continues
            // until doStop called on the output again
            stopProcessing = false;

            logger.debug("Terminating worker thread: {}", Thread.currentThread().getName());
        }
    }
}
