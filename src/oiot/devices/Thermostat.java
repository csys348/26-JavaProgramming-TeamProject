package oiot.devices;

import java.util.concurrent.ThreadLocalRandom;
import oiot.core.DeviceType;
import oiot.core.SmartDevice;

public class Thermostat extends SmartDevice {
    private double currentTemp;
    private double targetTemp;

    public Thermostat(String id, String name, double initialTemp) {
        super(id, name, DeviceType.THERMOSTAT);
        this.currentTemp = initialTemp;
        this.targetTemp = initialTemp;
    }

    public void setTargetTemp(double targetTemp) {
        if (targetTemp < 10 || targetTemp > 35) {
            throw new IllegalArgumentException("목표 온도는 10~35도 범위여야 합니다.");
        }
        this.targetTemp = targetTemp;
    }

    public double getCurrentTemp() {
        return currentTemp;
    }

    public double getTargetTemp() {
        return targetTemp;
    }

    @Override
    public void simulateTick() {
        double noise = ThreadLocalRandom.current().nextDouble(-0.2, 0.21);
        if (isOn()) {
            double gap = targetTemp - currentTemp;
            currentTemp += (gap * 0.25) + noise;
        } else {
            currentTemp += noise;
        }
    }

    @Override
    public String getStatusSummary() {
        return String.format("[%s] %s | power=%s, current=%.1fC, target=%.1fC",
                getId(), getName(), isOn() ? "ON" : "OFF", currentTemp, targetTemp);
    }
}
