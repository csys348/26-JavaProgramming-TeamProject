package oiot.devices;

import java.util.concurrent.ThreadLocalRandom;
import oiot.core.DeviceType;
import oiot.core.SmartDevice;

public class SecuritySensor extends SmartDevice {
    private boolean motionDetected;
    private int sensitivity; // 1-10

    public SecuritySensor(String id, String name) {
        super(id, name, DeviceType.SECURITY_SENSOR);
        this.motionDetected = false;
        this.sensitivity = 5;
    }

    public void setSensitivity(int sensitivity) {
        if (sensitivity < 1 || sensitivity > 10) {
            throw new IllegalArgumentException("민감도는 1~10 범위여야 합니다.");
        }
        this.sensitivity = sensitivity;
    }

    public int getSensitivity() {
        return sensitivity;
    }

    public boolean isMotionDetected() {
        return motionDetected;
    }

    @Override
    public void simulateTick() {
        if (!isOn()) {
            motionDetected = false;
            return;
        }
        int threshold = 92 - (sensitivity * 4); // 민감도가 높을수록 탐지 확률 증가
        int roll = ThreadLocalRandom.current().nextInt(100);
        motionDetected = roll >= threshold;
    }

    @Override
    public String getStatusSummary() {
        return String.format("[%s] %s | power=%s, sensitivity=%d, motion=%s",
                getId(), getName(), isOn() ? "ON" : "OFF", sensitivity, motionDetected ? "DETECTED" : "CLEAR");
    }
}
