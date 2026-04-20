package oiot.devices;

import java.util.concurrent.ThreadLocalRandom;
import oiot.core.DeviceType;
import oiot.core.SmartDevice;

public class Light extends SmartDevice {
    private int brightness; // 0-100

    public Light(String id, String name) {
        super(id, name, DeviceType.LIGHT);
        this.brightness = 50;
    }

    public void setBrightness(int brightness) {
        if (brightness < 0 || brightness > 100) {
            throw new IllegalArgumentException("밝기는 0~100 범위여야 합니다.");
        }
        this.brightness = brightness;
    }

    public int getBrightness() {
        return brightness;
    }

    @Override
    public void simulateTick() {
        if (!isOn()) {
            return;
        }
        int delta = ThreadLocalRandom.current().nextInt(-5, 6);
        int next = Math.max(20, Math.min(100, brightness + delta));
        brightness = next;
    }

    @Override
    public String getStatusSummary() {
        return String.format("[%s] %s | power=%s, brightness=%d%%",
                getId(), getName(), isOn() ? "ON" : "OFF", brightness);
    }
}
