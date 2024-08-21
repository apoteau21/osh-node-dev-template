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

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.impl.module.ModuleRegistry;
import java.io.*;

/**
 * Sensor driver providing sensor description, output registration, initialization and shutdown of driver and outputs.
 *
 * @author Ashley Poteau
 * @since July 17, 2024
 */
public class Sensor extends AbstractSensorModule<Config> {

    private static final Logger logger = LoggerFactory.getLogger(Sensor.class);

    private ICommProvider<?> commProvider = null;

    private Output output;

    private SpeechProcessor speechProcessor;

    LanguageModel languageModel = LanguageModel.ENGLISH;

    //@Override
    public void doInit() throws SensorHubException {

        super.doInit();

        // Generate identifiers
        generateUniqueID("[URN]", config.serialNumber);
        generateXmlID("[XML-PREFIX]", config.serialNumber);

        try {
            output = new Output(this, languageModel);
            output.doInit();
            addOutput(output, false);
        }
        catch(IOException e) {
            e.printStackTrace();
            throw new SensorHubException("Failed to initialize output", e);
        }
    }

    //@Override
    public void doStart() throws SensorHubException {

        if (null != output) {

            // Allocate necessary resources and doStart outputs
            output.doStart();
        }

        // TODO: Perform other startup procedures
        try {
            InputStream inputStream = null;

            if (config.speechConfig.commSettings != null) {

                if (commProvider == null) {

                    ModuleRegistry modReg = getParentHub().getModuleRegistry();
                    commProvider = (ICommProvider<?>)modReg.loadSubModule(config.speechConfig.commSettings, true);
                    commProvider.start();

                    inputStream = commProvider.getInputStream();
                }
            } else {
                inputStream = new FileInputStream(config.speechConfig.wavFile);
            }
            speechProcessor = new SpeechProcessor(
                    config.speechConfig.speechRecognizerType,
                    config.speechConfig.languageModel,
                    inputStream);
            speechProcessor.addListener(output);
            speechProcessor.processStream();

        } catch (FileNotFoundException e) {
            logger.error("Failed to get input stream from file due to exception {}", e.getMessage());

            throw new SensorHubException("Failed to start driver due to exception:", e);
        } catch (IOException e) {
            logger.error("Failed to get input stream from commProvider due to exception {}", e.getMessage());

            throw new SensorHubException("Failed to start driver due to exception:", e);
        }
    }

    @Override
    public void doStop() throws SensorHubException {

        if (null != speechProcessor) {

            speechProcessor.removeListener(output);
            try {
                speechProcessor.stopProcessingStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            speechProcessor = null;
        }

        try {
            if (commProvider != null && commProvider.isStarted()) {

                commProvider.stop();
                commProvider.cleanup();
                commProvider = null;
            }
        } finally {
            if (null != output) {

                output.doStop();
                output = null;
            }
        }

    }

    @Override
    public boolean isConnected() {

        // Determine if sensor is connected
        return output != null && output.isAlive();
    }
}
