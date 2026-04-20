package oiot.core;

public abstract class SmartDevice implements Controllable, MockUpdatable {
    private final String id;
    private final String name;
    private final DeviceType type;
    private boolean powerOn;

    protected SmartDevice(String id, String name, DeviceType type) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.powerOn = false;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public DeviceType getType() {
        return type;
    }

    @Override
    public void turnOn() {
        powerOn = true;
    }

    @Override
    public void turnOff() {
        powerOn = false;
    }

    @Override
    public boolean isOn() {
        return powerOn;
    }

    public abstract String getStatusSummary();
}
