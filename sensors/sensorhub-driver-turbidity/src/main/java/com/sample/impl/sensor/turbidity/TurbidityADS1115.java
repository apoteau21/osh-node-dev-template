package com.sample.impl.sensor.turbidity;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CConfig;

/**
 * The TurbidityADS1115 class interacts with the turbidity sensor by reading and writing to I2C registers.
 *
 * heavily referenced from CQRobot_ADS1115.py
 *
 * @author Ashley Poteau
 * @since January 10, 2025
 */
public class TurbidityADS1115 {

    private static double coefficient = 0.125;
    private static int myGain = 0x02;

    private Context pi4j = Pi4J.newAutoContext();

    // i2c bus
    I2CConfig I2CBus = I2C.newConfigBuilder(pi4j)
            .bus(1)
            .device(0x48)
            .build();

    private I2C ads1115 = pi4j.create(I2CBus);

    // i2c addresses
    // should be 48.
    private static final int ADS1115_IIC_ADDRESS0 = 0x48;
    private static final int ADS1115_IIC_ADDRESS1 = 0x49;

    // register map
    private static final int ADS1115_REG_POINTER_CONVERT = 0x00; // conversion register
    private static final int ADS1115_REG_POINTER_CONFIG = 0x01; // configuration register
    private static final int ADS1115_REG_POINTER_LOWTHRESH = 0x02; // low threshold register
    private static final int ADS1115_REG_POINTER_HITHRESH = 0x03; // high threshold register

    //------------------------------ config registers (start) --------------------------------
    // added several spaces to improve readability

    // config os
    private static final int ADS1115_REG_CONFIG_OS_NOEFFECT = 0x00; // no effect
    private static final int ADS1115_REG_CONFIG_OS_SINGLE = 0x80; // begin single conversion

    // config mux diff
    private static final int ADS1115_REG_CONFIG_MUX_DIFF_0_1 = 0x00; // differential P = AIN0, N = AIN1 (default)
    private static final int ADS1115_REG_CONFIG_MUX_DIFF_0_3 = 0x10; // differential P = AIN0, N = AIN3
    private static final int ADS1115_REG_CONFIG_MUX_DIFF_1_3 = 0x20; // differential P = AIN1, N = AIN3
    private static final int ADS1115_REG_CONFIG_MUX_DIFF_2_3 = 0x30; // differential P = AIN2, N = AIN3

    // config mux single
    private static final int ADS1115_REG_CONFIG_MUX_SINGLE_0 = 0x40; // single ended P = AIN0, N = GND
    private static final int ADS1115_REG_CONFIG_MUX_SINGLE_1 = 0x50; // single ended P = AIN1, N = GND
    private static final int ADS1115_REG_CONFIG_MUX_SINGLE_2 = 0x60; // single ended P = AIN2, N = GND
    private static final int ADS1115_REG_CONFIG_MUX_SINGLE_3 = 0x70; // single ended P = AIN3, N = GND

    // config pga
    private static final int ADS1115_REG_CONFIG_PGA_6_144V = 0x00; // +/- 6.144v range = gain 2/3
    private static final int ADS1115_REG_CONFIG_PGA_4_096V = 0x02; // +/- 4.096v range = gain 1
    private static final int ADS1115_REG_CONFIG_PGA_2_048V = 0x04; // +/- 2.048v range = gain 2 (default)
    private static final int ADS1115_REG_CONFIG_PGA_1_024V = 0x06; // +/- 1.024v range = gain 4
    private static final int ADS1115_REG_CONFIG_PGA_0_512V = 0x08; // +/- 0.512v range = gain 8
    private static final int ADS1115_REG_CONFIG_PGA_0_256V = 0x0A; // +/- 0.256v range = gain 16

    // config mode
    private static final int ADS1115_REG_CONFIG_MODE_CONTIN = 0x00; // continuous conversion mode
    private static final int ADS1115_REG_CONFIG_MODE_SINGLE = 0x01; // power-down single-shot mode (default)

    // config dr
    private static final int ADS1115_REG_CONFIG_DR_8SPS = 0x00; // 8 samples per second
    private static final int ADS1115_REG_CONFIG_DR_16SPS = 0x20; // 16 samples per second
    private static final int ADS1115_REG_CONFIG_DR_32SPS = 0x40; // 32 samples per second
    private static final int ADS1115_REG_CONFIG_DR_64SPS = 0x60; // 64 samples per second
    private static final int ADS1115_REG_CONFIG_DR_128SPS = 0x80; // 128 samples per second
    private static final int ADS1115_REG_CONFIG_DR_250SPS = 0xA0; // 250 samples per second
    private static final int ADS1115_REG_CONFIG_DR_475SPS = 0xC0; // 475 samples per second
    private static final int ADS1115_REG_CONFIG_DR_860SPS = 0xE0; // 860 samples per second

    // config cmode
    private static final int ADS1115_REG_CONFIG_CMODE_TRAD = 0x00; // traditional comparator with hysteresis (default)
    private static final int ADS1115_REG_CONFIG_CMODE_WINDOW = 0x10; // window comparator

    // config cpol
    private static final int ADS1115_REG_CONFIG_CPOL_ACTVLOW = 0x00; // alert/rdy pin is low when active (default)
    private static final int ADS1115_REG_CONFIG_CPOL_ACTVHI = 0x08; // alert/rdy pin is high when active

    // config clat
    private static final int ADS1115_REG_CONFIG_CLAT_NONLAT = 0x00; // non latching comparator (default)
    private static final int ADS1115_REG_CONFIG_CLAT_LATCH = 0x04; // latching comparator

    // config cque
    private static final int ADS1115_REG_CONFIG_CQUE_1CONV = 0x00; // assert alert/rdy after 1 conversions
    private static final int ADS1115_REG_CONFIG_CQUE_2CONV = 0x01; // assert alert/rdy after 2 conversions
    private static final int ADS1115_REG_CONFIG_CQUE_3CONV = 0x02; // assert alert/rdy after 4 conversions
    private static final int ADS1115_REG_CONFIG_CQUE_NONE = 0x03; // disable the comparator and put alert/rdy in high state (default)

    //----------------------------------- config registers (end) -------------------------------------

    private static int addr_G = ADS1115_IIC_ADDRESS0;
    private static int channel;

    // make get/set methods for registers used in voltage reader.
    // none so far

    public TurbidityADS1115() {}

    public void ADS1115(I2C ads1115) {
        this.ads1115 = ads1115;
    }

    public static double setGain(int gain) {
        myGain = gain;

        if (myGain == ADS1115_REG_CONFIG_PGA_6_144V) {
            return coefficient = 0.1875;
        } else if (myGain == ADS1115_REG_CONFIG_PGA_4_096V) {
            return coefficient = 0.125;
        } else if (myGain == ADS1115_REG_CONFIG_PGA_2_048V) {
            return coefficient = 0.625;
        } else if (myGain == ADS1115_REG_CONFIG_PGA_1_024V) {
            return coefficient = 0.03125;
        } else if (myGain == ADS1115_REG_CONFIG_PGA_0_512V) {
            return coefficient = 0.015625;
        } else if (myGain == ADS1115_REG_CONFIG_PGA_0_256V) {
            return coefficient = 0.0078125;
        } else {
            return coefficient = 0.125;
        }
    }

    public void setAddress(int addr) {
        this.addr_G = addr;
    }

    public int setChannel(int channel) {
        this.channel = channel;
        while (this.channel > 3) {
            this.channel = 0;
        }

        return this.channel;
    }

    public I2C setSingle() {
        int CONFIG_REG = 0;
        if (this.channel == 0) {
            CONFIG_REG = ADS1115_REG_CONFIG_OS_SINGLE | ADS1115_REG_CONFIG_MUX_SINGLE_0 | myGain | ADS1115_REG_CONFIG_MODE_CONTIN | ADS1115_REG_CONFIG_DR_128SPS | ADS1115_REG_CONFIG_CQUE_NONE;
        } else if (this.channel == 1) {
            CONFIG_REG = ADS1115_REG_CONFIG_OS_SINGLE | ADS1115_REG_CONFIG_MUX_SINGLE_1 | myGain | ADS1115_REG_CONFIG_MODE_CONTIN | ADS1115_REG_CONFIG_DR_128SPS | ADS1115_REG_CONFIG_CQUE_NONE;
        } else if (this.channel == 2) {
            CONFIG_REG = ADS1115_REG_CONFIG_OS_SINGLE | ADS1115_REG_CONFIG_MUX_SINGLE_2 | myGain | ADS1115_REG_CONFIG_MODE_CONTIN | ADS1115_REG_CONFIG_DR_128SPS | ADS1115_REG_CONFIG_CQUE_NONE;
        } else if (this.channel == 3) {
            CONFIG_REG = ADS1115_REG_CONFIG_OS_SINGLE | ADS1115_REG_CONFIG_MUX_SINGLE_3 | myGain | ADS1115_REG_CONFIG_MODE_CONTIN | ADS1115_REG_CONFIG_DR_128SPS | ADS1115_REG_CONFIG_CQUE_NONE;
        }
        byte[] blockData = {(byte) addr_G, ADS1115_REG_POINTER_CONVERT, (byte) CONFIG_REG};

        ads1115.write(blockData);

        return ads1115;
    }

    public I2C setDifferential() {
        int CONFIG_REG = 0;
        if (this.channel == 0) {
            CONFIG_REG = ADS1115_REG_CONFIG_OS_SINGLE | ADS1115_REG_CONFIG_MUX_DIFF_0_1 | myGain | ADS1115_REG_CONFIG_MODE_CONTIN  | ADS1115_REG_CONFIG_DR_128SPS | ADS1115_REG_CONFIG_CQUE_NONE;
        } else if (this.channel == 1) {
            CONFIG_REG = ADS1115_REG_CONFIG_OS_SINGLE | ADS1115_REG_CONFIG_MUX_DIFF_0_3 | myGain | ADS1115_REG_CONFIG_MODE_CONTIN | ADS1115_REG_CONFIG_DR_128SPS | ADS1115_REG_CONFIG_CQUE_NONE;
        } else if (this.channel == 2) {
            CONFIG_REG = ADS1115_REG_CONFIG_OS_SINGLE | ADS1115_REG_CONFIG_MUX_DIFF_1_3 | myGain | ADS1115_REG_CONFIG_MODE_CONTIN | ADS1115_REG_CONFIG_DR_128SPS | ADS1115_REG_CONFIG_CQUE_NONE;
        } else if (this.channel == 3) {
            CONFIG_REG = ADS1115_REG_CONFIG_OS_SINGLE | ADS1115_REG_CONFIG_MUX_DIFF_2_3 | myGain | ADS1115_REG_CONFIG_MODE_CONTIN | ADS1115_REG_CONFIG_DR_128SPS | ADS1115_REG_CONFIG_CQUE_NONE;
        }
        byte[] blockData = {(byte) addr_G, ADS1115_REG_POINTER_CONVERT, (byte) CONFIG_REG};

        ads1115.write(blockData);

        return ads1115;
    }

    public int readValue() throws Exception {
        byte[] data = {(byte) addr_G, ADS1115_REG_POINTER_CONVERT, 2};

        int rawADC = data[0] * 256 + data[1];

        if (rawADC > 32767) {
            rawADC -= 65535;
        }
        rawADC = (int) (rawADC * coefficient);
        return rawADC;
    }

    public int readVoltage(int i) throws Exception {
        this.setChannel(channel);
        this.setSingle();
        Thread.sleep(100);

        int rawADC = this.readValue();

        return rawADC;
    }

    public int ComparatorVoltage() throws Exception {
        this.setChannel(channel);
        this.setSingle();
        Thread.sleep(100);
        int rawADC = this.readValue();

        return rawADC;
    }
}

