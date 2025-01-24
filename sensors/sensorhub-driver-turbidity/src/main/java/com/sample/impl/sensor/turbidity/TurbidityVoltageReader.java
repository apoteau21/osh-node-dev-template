package com.sample.impl.sensor.turbidity;

import com.pi4j.context.Context;
import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CConfig;

import java.util.Arrays;

/**
 * The TurbidityVoltageReader class provides the functionality necessary for reading turbidity.
 *
 * @author Ashley Poteau
 * @since Nov 27, 2024
 */
public class TurbidityVoltageReader {

    TurbidityADS1115 ads1115 = new TurbidityADS1115();

    // ads address var
    private static final int ADS1115_ADDRESS = 0x48; // i2c address for ads1115

    private static final int bufferSize = 30;
    private static final double VREF = 5.0;

    private static float temp = 25;
    private static double avgVoltage = 0;
    public static float tdsValue = 0;
    private static int analogBufferIndex = 0;
    private static double bTemp = 0;
    private static int copyIndex = 0;

    public static boolean print = false;

    // buffers
    private static double[] analogBufferTemp = new double[bufferSize];
    private static double[] analogBuffer = new double[bufferSize];

    // get median of buffer
    public static double getMedianNum(int filterLen) {
        Arrays.sort(analogBufferTemp);

        for (int j = 0; j < filterLen - 1; j++) {
            for (int i = 0; i < filterLen - j - 1; i++) {
                if (analogBufferTemp[i] > analogBufferTemp[i + 1]) {
                    bTemp = analogBufferTemp[i];
                    analogBufferTemp[i] = analogBufferTemp[i + 1];
                    analogBufferTemp[i + 1] = bTemp;
                }
            }
        }

        if (filterLen > 0) {
            bTemp = analogBufferTemp[(filterLen - 1) / 2];
        } else {
           bTemp = (analogBufferTemp[(int) (Math.floorDiv(filterLen, 2))] + analogBufferTemp[Math.floorDiv(filterLen, 2) - 1]) / 2;
        }

        return (float) bTemp;
    }

    public void printTDS() throws Exception {
        double analogSampleTimepoint = System.currentTimeMillis() / 1000.0;
        double printTimepoint = System.currentTimeMillis() / 1000.0;

        print = true;

        while (print) {
            if (System.currentTimeMillis() / 1000.0 - analogSampleTimepoint > 0.04) {
                analogSampleTimepoint = System.currentTimeMillis() / 1000.0;
                analogBuffer[analogBufferIndex] = ads1115.readVoltage(1);
                analogBufferIndex = analogBufferIndex + 1;

                if (analogBufferIndex == 30) {
                    analogBufferIndex = 0;
                }
            }

            if (System.currentTimeMillis() / 1000.0 - printTimepoint > 0.8) {
                printTimepoint = System.currentTimeMillis() / 1000.0;

                for (copyIndex = 0; copyIndex < 30; copyIndex++) {
                    analogBufferTemp[copyIndex] = ads1115.readVoltage(1);
                }

                System.out.println("A1: " + getMedianNum(30));

                avgVoltage = getMedianNum(30) * (VREF / 1024.0);

                double compensationCoefficient = 1.0 + 0.02 * (temp - 25.0);

                double compensationVoltage = avgVoltage / compensationCoefficient;

                tdsValue = (float) ((133.42 * Math.pow(compensationVoltage, 3) - 255.86 * Math.pow(compensationVoltage, 2) + 857.39 * compensationVoltage) * 0.5);

                System.out.println("A1: " + tdsValue);
            }
        }
    }
}
