package oiot.devices;

import java.util.concurrent.ThreadLocalRandom;
import oiot.core.DeviceType;
import oiot.core.SmartDevice;

public class AirPurifier extends SmartDevice {
    private int fanLevel; // 1-5
    private int airQuality; // 0-100, 높을수록 좋음

    public AirPurifier(String id, String name) {
        super(id, name, DeviceType.AIR_PURIFIER);
        this.fanLevel = 3;
        this.airQuality = 70;
    }

    public void setFanLevel(int fanLevel) {
        if (fanLevel < 1 || fanLevel > 5) {
            throw new IllegalArgumentException("팬 단계는 1~5 범위여야 합니다.");
        }
        this.fanLevel = fanLevel;
    }

    public int getFanLevel() {
        return fanLevel;
    }

    public int getAirQuality() {
        return airQuality;
    }

    @Override
    public void simulateTick() {
        int externalNoise = ThreadLocalRandom.current().nextInt(-3, 4);
        if (isOn()) {
            airQuality += fanLevel + externalNoise;
        } else {
            airQuality += externalNoise - 1;
        }
        airQuality = Math.max(10, Math.min(100, airQuality));
    }

    @Override
    public String getStatusSummary() {
        return String.format("[%s] %s | power=%s, fan=%d, airQuality=%d",
                getId(), getName(), isOn() ? "ON" : "OFF", fanLevel, airQuality);
    }
}
