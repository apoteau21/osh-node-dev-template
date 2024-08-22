package com.sample.impl.sensor.simulated;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding; // *****
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.impl.sensor.AbstractSensorControl; // *****
import org.vast.data.DataChoiceImpl; // *****
import org.vast.swe.SWEHelper;
import org.sensorhub.api.command.CommandException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sensorhub.api.command.ICommandReceiver;
import org.sensorhub.api.sensor.SensorException;
import javax.validation.constraints.Max;

/**
 * control specs for camera, how far it can pan and tilt
 * ashley poteau
 * may 27 2024
 */
public class Control extends AbstractSensorControl<Sensor> {
    private static final float MIN_ANGLE = 0f;

    private static final float MAX_ANGLE = 160f;

    private static final String SENSOR_CONTROL_NAME = "cam control";

    private static final Logger logger = LoggerFactory.getLogger(Output.class);

    protected DataRecord commandDataStruct;

    public Control(Sensor parentSensor) {
        super(SENSOR_CONTROL_NAME, parentSensor);
    }

    protected static float getMinAngle() {
        return MIN_ANGLE;
    }

    protected static float getMaxAngle() {
        return MAX_ANGLE;
    }

    protected Control(String name, Sensor parentSensor) {
        super(name, parentSensor);
    }

    @Override
    public DataComponent getCommandDescription() {
        return commandDataStruct;
    }

    @Override
    public boolean execCommand(DataBlock command) throws CommandException {
        boolean commandExecuted = true;

        try {

            DataRecord commandData = commandDataStruct.copy();
            commandData.setData(command);

            DataComponent component = commandData.getField("angle");

            //String commandId = component.getName();

            DataBlock data = component.getData();

            getLogger().debug("(1) data = {} {} {}", data, component, commandData);

            float angle = data.getIntValue();

            angle = (angle <= MIN_ANGLE) ? MIN_ANGLE : Math.min(angle, MAX_ANGLE);

            parentSensor.currentAngle = angle;

            parentSensor.tiltTo(angle);

            //parentSensor.panTo(angle);

            // pan to an angle
            /**if (commandId.equalsIgnoreCase("Pan")) {
             parentSensor.panTo(angle);
             } else {
             commandExecuted = false;
             }

             //tilt to an angle
             if (commandId.equalsIgnoreCase("Tilt")) {
             parentSensor.tiltTo(angle);
             } else {
             commandExecuted = false;
             }*/
        } catch (Exception e) {
            throw new CommandException("failed to command CameraSensor module: ", e);
        }
        return commandExecuted;
    }

    protected void init() {
        SWEHelper factory = new SWEHelper();

        /**commandDataStruct = factory.createRecord()
                .name(getName())
                .updatable(true)
                .definition(SWEHelper.getPropertyUri("CameraSensor"))
                .label("CameraSensor")
                .description("A camera sensor")
                .addField("Command",
                        factory.createChoice()
                                .addItem("Pan",
                                        factory.createQuantity()
                                                .name("Angle")
                                                .updatable(true)
                                                .definition(SWEHelper.getPropertyUri("servo-angle"))
                                                .description("The angle in degrees to which the servo is to turn")
                                                .addAllowedInterval(MIN_ANGLE, MAX_ANGLE)
                                                .uomCode("deg")
                                                .value(0.0)
                                                .build())
                                .addItem("Tilt",
                                        factory.createQuantity()
                                                .name("Angle")
                                                .updatable(true)
                                                .definition(SWEHelper.getPropertyUri("servo-angle"))
                                                .description("The angle in degrees to which the servo is to turn")
                                                .addAllowedInterval(MIN_ANGLE, MAX_ANGLE)
                                                .uomCode("deg")
                                                .value(0.0)
                                                .build())
                                .build())
                .build();*/

        commandDataStruct = factory.createRecord()
                .name(getName())
                .updatable(true)
                .definition(SWEHelper.getPropertyUri("CameraSensor"))
                .label("CameraSensor")
                .description("A camera sensor")
                .addField("angle",
                                        factory.createQuantity()
                                                .name("Angle")
                                                .label("tilt")
                                                .updatable(true)
                                                .definition(SWEHelper.getPropertyUri("servo-angle"))
                                                .description("The angle in degrees to which the servo is to turn")
                                                .addAllowedInterval(MIN_ANGLE, MAX_ANGLE)
                                                .uomCode("deg")
                                                .value(parentSensor.currentAngle)
                                                .build())

                .build();


    }


}
