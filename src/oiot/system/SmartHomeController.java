package oiot.system;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import oiot.core.SmartDevice;

public class SmartHomeController {
    private final List<SmartDevice> devices = new ArrayList<>();

    public void registerDevice(SmartDevice device) {
        devices.add(device);
    }

    public List<SmartDevice> getDevices() {
        return List.copyOf(devices);
    }

    public Optional<SmartDevice> findDeviceById(String id) {
        return devices.stream()
                .filter(d -> d.getId().equalsIgnoreCase(id))
                .findFirst();
    }

    public void simulateAllDevices() {
        for (SmartDevice device : devices) {
            device.simulateTick();
        }
    }

    public String getSystemStatus() {
        StringBuilder builder = new StringBuilder();
        builder.append("=== O-IoT System Status ===\n");
        for (SmartDevice device : devices) {
            builder.append(device.getStatusSummary()).append('\n');
        }
        return builder.toString();
    }
}
