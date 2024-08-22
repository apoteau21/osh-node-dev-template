package com.sample.impl.sensor.simulated;

/**
 * enumeration of gpio pins
 * ashley poteau
 * june 5 2024
 */
public enum GpioEnum {

    // 32 pins
    PIN_UNSET(-1, "Not set"),
    PIN_00(0, "GPIO 0"),
    PIN_01(1, "GPIO 1"),
    PIN_02(2, "GPIO 2"),
    PIN_03(3, "GPIO 3"),
    PIN_04(4, "GPIO 4"),
    PIN_05(5, "GPIO 5"),
    PIN_06(6, "GPIO 6"),
    PIN_07(7, "GPIO 7"),
    PIN_08(8, "GPIO 8"),
    PIN_09(9, "GPIO 9"),
    PIN_10(10, "GPIO 10"),
    PIN_11(11, "GPIO 11"),
    PIN_12(12, "GPIO 12"),
    PIN_13(13, "GPIO 13"),
    PIN_14(14, "GPIO 14"),
    PIN_15(15, "GPIO 15"),
    PIN_16(16, "GPIO 16"),
    PIN_17(17, "GPIO 17"),
    PIN_18(18, "GPIO 18"),
    PIN_19(19, "GPIO 19"),
    PIN_20(20, "GPIO 20"),
    PIN_21(21, "GPIO 21"),
    PIN_22(22, "GPIO 22"),
    PIN_23(23, "GPIO 23"),
    PIN_24(24, "GPIO 24"),
    PIN_25(25, "GPIO 25"),
    PIN_26(26, "GPIO 26"),
    PIN_27(27, "GPIO 27"),
    PIN_28(28, "GPIO 28"),
    PIN_29(29, "GPIO 29"),
    PIN_30(30, "GPIO 30"),
    PIN_31(31, "GPIO 31");

    private int value;
    private String name;

    // constructor
    GpioEnum(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public int getValue() {
        return this.value;
    }

    public String toString() {
        return this.name;
    }
}
