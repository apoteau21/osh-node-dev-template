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

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sample.impl.sensor.turbidity.TurbidityVoltageReader.getMedianNum;

/**
 * Sensor driver providing sensor description, output registration, initialization and shutdown of driver and outputs.
 *
 * @author Ashley Poteau
 * @since Nov 21 2024
 */
public class TurbiditySensor extends AbstractSensorModule<TurbidityConfig> {

    private static final Logger logger = LoggerFactory.getLogger(TurbiditySensor.class);

    private TurbidityOutput output;

    boolean print = TurbidityVoltageReader.print;

    public void doInit() throws SensorHubException {

        super.doInit();

        // Generate identifiers
        generateUniqueID("[URN]", config.serialNumber);
        generateXmlID("[XML-PREFIX]", config.serialNumber);

        // create and initialize output
        output = new TurbidityOutput(this);
        output.doInit();
        addOutput(output, false);
    }

    public void recurringTDS() {
        System.out.println("A1: " + TurbidityVoltageReader.getMedianNum(30));
        System.out.println("A1: " + TurbidityVoltageReader.tdsValue);
    }

    @Override
    public void doStart() throws SensorHubException {

        logger.debug("starting");

        if (null != output) {
            // Allocate necessary resources and start outputs
            output.doStart();
        }

        // TODO: Perform other startup procedures

        print = true;

        while (print) {
            recurringTDS();
            logger.debug("started");
        }
    }

    @Override
    public void doStop() throws SensorHubException {

        if (null != output) {
            output.doStop();
        }

        // TODO: Perform other shutdown procedures
        print = false;
        logger.debug("stopped");
    }

    @Override
    public boolean isConnected() {

        // Determine if sensor is connected
        return output.isAlive();
    }
}