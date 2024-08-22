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

import org.sensorhub.api.command.IStreamingControlInterface;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.pi4j.io.gpio.*;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;


/**
 * Sensor driver providing sensor description, output registration, initialization and shutdown of driver and outputs.
 *
 * @author ashley poteau
 * @since may 27 2024
 */
public class Sensor extends AbstractSensorModule<Config> {

    private static final Logger logger = LoggerFactory.getLogger(Sensor.class);

    public Output output;
    public Control control;

    private AtomicBoolean isConnected = new AtomicBoolean(false);

    private GpioPinDigitalOutput panPin;
    private GpioPinDigitalOutput tiltPin;
    private final GpioController gpio = GpioFactory.getInstance();
    Object syncTimeLock = new Object();

    double currentAngle = 0;

    /**@Override
    public boolean isConnected() {
        return isConnected.get();
    }*/

    @Override
    public void doInit() throws SensorHubException, IOException {

        //LoggerFactory.getLogger(Sensor.class);

        super.doInit();
        logger.debug("Initializing");

        // Generate identifiers
        generateUniqueID("[URN]", config.serialNumber);
        generateXmlID("[XML-PREFIX]", config.serialNumber);

        output = new Output(this);
        output.doInit();
        addOutput(output, false);

        // TODO: Perform other initialization


        Control control = new Control("sensor",this);
        //IStreamingControlInterface controlInterface = null; // <- may not be needed idk. cant be null.....
        //IStreamingControlInterface controlInterface;
        //controlInterface.init(); // omg.....
        addControlInput(control);
        control.init();
    }

    // dont think i need this...idk
    /**@Override
    protected void updateSensorDescription() {
        synchronized (sensorDescLock) {
            super.updateSensorDescription();

            if (!sensorDescription.isSetDescription()) {
                sensorDescription.setDescription("HD Camera");

                //ref frame
                SpatialFrame localRefFrame = new SpatialFrameImpl();
                localRefFrame.setId("LOCAL_FRAME");
                localRefFrame
                        .setOrigin("center of the PiBot approx 122.5 mm from plane extending"
                        + "perpendicular to front surface of frame, 117 mm from planes extended " +
                                "from side surfaces of the frame, and 89 m from the plane of contact with the ground");
                localRefFrame.addAxis("x",
                        "The X axis points to the right");
                localRefFrame.addAxis("Y",
                        "The Y axis points upwards");
                localRefFrame.addAxis("Z",
                        "The Z axis points towards the outside of the front facet");
                ((PhysicalSystem) sensorDescription).addlocalReferenceFrame(localRefFrame);

                // sensor
                SpatialFrame cameraSensor = new SpatialFrameImpl();
                cameraSensor.setID("CAMERA_SENSOR_FRAME");
                cameraSensor.setOrigin("63.5 mm on the positive y-axis and 38.1 mm on the negative z-axis "
                + "from the origin of the #LOCAL_FRAME");
                localRefFrame.addAxis("x",
                        "The X axis is in the plane of the facet containing the apertures for the sensors and points to the right");
                localRefFrame.addAxis("Y",
                        "The Y axis is in the plane of the facet containing the apertures for the sensors and points upwards");
                localRefFrame.addAxis("Z",
                        "The Z axis points towards the outside of the facet containing the apertures for the sensors");
                ((PhysicalSystem) sensorDescription).addlocalReferenceFrame(cameraSensor);
            }
            SMLHelper helper = SMLHelper(sensorDescription);
            helper.addSerialNumber(config.serialNumber);
        }
    }*/

    @Override
    public void doStart() throws SensorHubException {

        //super.doStart();
        logger.debug("starting");

        // TODO: Perform other startup procedures

        if (config.panServoPin != GpioEnum.PIN_UNSET) {
            panPin = gpio.provisionDigitalOutputPin(RaspiPin.getPinByAddress(config.panServoPin.getValue()));
        } else {
            panPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_14);
        }
        panPin.setShutdownOptions(true, PinState.LOW);

        if (config.tiltServoPin != GpioEnum.PIN_UNSET) {
            tiltPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_13);
        } else {
            tiltPin.setShutdownOptions(true, PinState.LOW);
        }

        isConnected.set(true);
        logger.debug("started...");

        tiltTo(currentAngle);
        panTo(currentAngle);
    }

    @Override
    public void doStop() throws SensorHubException {
        super.doStop();
        if (null != output) {
            output.doStop();
        }

        // TODO: Perform other shutdown procedures
        // if pan isnt null...
        if (panPin != null) {
            panPin.setState(PinState.LOW);
            gpio.unprovisionPin(panPin);
            panPin = null;
        }

        // if tilt isnt null...
        if (tiltPin != null) {
            tiltPin.setState(PinState.LOW);
            gpio.unprovisionPin(tiltPin);
            tiltPin = null;
        }

        if (null != output) {
            output.doStop();
        }
        isConnected.set(false);
        logger.debug("Stopped!");
    }

    @Override
    public boolean isConnected() {

        // Determine if sensor is connected
        return output.isAlive();
    }

    private void rotateTo(GpioPinDigitalOutput servoPin, double angle) {
        logger.info("pin: {} angle: {}", servoPin.getName(), angle);
        long pulseWidthMicros = Math.round(angle * 11) + 500;
        logger.info("pulseWidth: {}", pulseWidthMicros);

        for (int i = 0; i <= 15; ++i) {
            servoPin.setState(PinState.HIGH);

            long start = System.nanoTime();
            while (System.nanoTime() - start < pulseWidthMicros * 1000);

            servoPin.setState(PinState.LOW);

            start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < (20 - pulseWidthMicros / 1000));
        }
    }

    // rotates pan servo to specified angle
    public void panTo(double angle) {
        rotateTo(panPin, angle);
    }

    // rotates tilt servo to specified angle
    public void tiltTo(double angle) {
        rotateTo(tiltPin, angle);
    }


}
