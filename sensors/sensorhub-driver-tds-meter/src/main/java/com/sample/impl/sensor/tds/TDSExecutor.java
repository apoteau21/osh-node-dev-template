package com.sample.impl.sensor.tds;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * TDSExecutor executes the original Python files from the CQRRObot wiki page
 * necessary for using the sensor.
 *
 *  @author Ashley Poteau
 *  @since February 14, 2025
 */
public class TDSExecutor {

    public String line;
    public String voltage;
    public String tdsValue;

    // TDS_RasPI is located in resources directory
    // need to have that directory and both of its contents in the tds-osh-node dir while running the driver for it to work
    String[] adsSetup = {"python3", "TDS_RasPI/CQRobot_ADS1115.py"};
    String[] voltReaderSetup = {"python3", "TDS_RasPI/ADS1115_ReadVoltage.py"};

    public void runPy(String[] command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        Process p = pb.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

        String substr = null;
        while ((line = reader.readLine()) != null) {
            substr = line.substring(3);
            System.out.println("python output:" + substr);

            if (substr.contains("mV")) { // millivolts
                voltage = substr;
                System.out.println("Voltage Output: " + voltage);
            } else if (substr.contains("ppm")) { // parts per mil
                tdsValue = substr;
                System.out.println("TDS Output: " + tdsValue);
            }
        }

        p.waitFor();
    }

    public String getVoltage() {
        return voltage;
    }

    public String getTDSValue() {
        return tdsValue;
    }
}
