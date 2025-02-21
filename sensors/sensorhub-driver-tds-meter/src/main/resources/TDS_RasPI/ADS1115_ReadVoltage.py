import sys
sys.path.append('../')
import time
from CQRobot_ADS1115 import ADS1115

ADS1115_REG_CONFIG_PGA_6_144V = 0x00
ads1115 = ADS1115()
ads1115.setAddr_ADS1115(0x48)
ads1115.setGain(ADS1115_REG_CONFIG_PGA_6_144V)

VREF = 5.0
temperature = 25

def getMedianNum(analogBuffer):
    analogBuffer.sort()
    if len(analogBuffer) % 2 == 0:
        return (analogBuffer[len(analogBuffer) // 2] + analogBuffer[len(analogBuffer) // 2 - 1]) / 2
    else:
        return analogBuffer[len(analogBuffer) // 2]

# Read sensor once
analogBuffer = [ads1115.readVoltage(1)['r'] for _ in range(30)]
medianVoltage = getMedianNum(analogBuffer)
averageVoltage = medianVoltage * (VREF / 1024.0)

compensationCoefficient = 1.0 + 0.02 * (temperature - 25.0)
compensationVoltage = averageVoltage / compensationCoefficient
tdsValue = (133.42 * compensationVoltage ** 3 - 255.86 * compensationVoltage ** 2 + 857.39 * compensationVoltage) * 0.5

# Print results
print(f"A1:{medianVoltage}mV")
print(f"A1:{tdsValue:.2f}ppm")

