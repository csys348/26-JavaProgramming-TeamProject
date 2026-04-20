package oiot.scenario;

import oiot.core.SmartDevice;
import oiot.devices.AirPurifier;
import oiot.devices.Light;
import oiot.devices.SecuritySensor;
import oiot.devices.SmartBlind;
import oiot.devices.Thermostat;
import oiot.system.SmartHomeController;

public class ScenarioEngine {
    private final SmartHomeController controller;

    public ScenarioEngine(SmartHomeController controller) {
        this.controller = controller;
    }

    public void apply(ScenarioPreset preset) {
        for (SmartDevice device : controller.getDevices()) {
            if (preset == ScenarioPreset.HOME) {
                applyHome(device);
            } else if (preset == ScenarioPreset.AWAY) {
                applyAway(device);
            } else if (preset == ScenarioPreset.NIGHT) {
                applyNight(device);
            }
        }
    }

    private void applyHome(SmartDevice device) {
        device.turnOn();
        if (device instanceof Light light) {
            light.setBrightness(70);
        } else if (device instanceof Thermostat thermostat) {
            thermostat.setTargetTemp(24);
        } else if (device instanceof SecuritySensor sensor) {
            sensor.setSensitivity(4);
        } else if (device instanceof AirPurifier purifier) {
            purifier.setFanLevel(3);
        } else if (device instanceof SmartBlind blind) {
            blind.setOpenness(65);
        }
    }

    private void applyAway(SmartDevice device) {
        if (device instanceof SecuritySensor sensor) {
            sensor.turnOn();
            sensor.setSensitivity(9);
            return;
        }
        device.turnOff();
        if (device instanceof Thermostat thermostat) {
            thermostat.setTargetTemp(18);
        } else if (device instanceof SmartBlind blind) {
            blind.setOpenness(20);
        }
    }

    private void applyNight(SmartDevice device) {
        device.turnOn();
        if (device instanceof Light light) {
            light.setBrightness(25);
        } else if (device instanceof Thermostat thermostat) {
            thermostat.setTargetTemp(21);
        } else if (device instanceof SecuritySensor sensor) {
            sensor.setSensitivity(8);
        } else if (device instanceof AirPurifier purifier) {
            purifier.setFanLevel(2);
        } else if (device instanceof SmartBlind blind) {
            blind.setOpenness(0);
        }
    }
}
