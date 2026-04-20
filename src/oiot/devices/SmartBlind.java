package oiot.devices;

import java.util.concurrent.ThreadLocalRandom;
import oiot.core.DeviceType;
import oiot.core.SmartDevice;

public class SmartBlind extends SmartDevice {
    private int openness; // 0-100

    public SmartBlind(String id, String name) {
        super(id, name, DeviceType.BLIND);
        this.openness = 50;
    }

    public void setOpenness(int openness) {
        if (openness < 0 || openness > 100) {
            throw new IllegalArgumentException("개방도는 0~100 범위여야 합니다.");
        }
        this.openness = openness;
    }

    public int getOpenness() {
        return openness;
    }

    @Override
    public void simulateTick() {
        if (!isOn()) {
            return;
        }
        int drift = ThreadLocalRandom.current().nextInt(-2, 3);
        openness = Math.max(0, Math.min(100, openness + drift));
    }

    @Override
    public String getStatusSummary() {
        return String.format("[%s] %s | power=%s, openness=%d%%",
                getId(), getName(), isOn() ? "ON" : "OFF", openness);
    }
}
