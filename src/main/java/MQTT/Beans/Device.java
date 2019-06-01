package MQTT.Beans;

import java.io.Serializable;

public class Device implements Serializable {
    private String DeviceID;
    private boolean isConnected;

    public Device(String deviceID) {
        DeviceID = deviceID;
        isConnected = false;
    }

    public String getDeviceID() {
        return DeviceID;
    }

    public void setDeviceID(String deviceID) {
        DeviceID = deviceID;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }
}
