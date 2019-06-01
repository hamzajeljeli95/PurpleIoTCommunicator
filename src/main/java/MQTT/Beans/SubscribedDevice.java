package MQTT.Beans;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.mqtt.MqttEndpoint;

import java.io.Serializable;

public class SubscribedDevice implements Serializable {
    private MqttEndpoint endpoint;
    private String Topic;
    private MqttQoS QoS;

    public SubscribedDevice(MqttEndpoint endpoint, String topic, MqttQoS qoS) {
        this.endpoint = endpoint;
        Topic = topic;
        QoS = qoS;
    }

    public SubscribedDevice(MqttEndpoint endpoint, String topic) {
        this.endpoint = endpoint;
        Topic = topic;
        QoS = MqttQoS.AT_LEAST_ONCE;
    }

    public SubscribedDevice(MqttEndpoint endpoint) {
        this.endpoint = endpoint;
        QoS = MqttQoS.AT_LEAST_ONCE;
    }

    public MqttEndpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(MqttEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    public String getTopic() {
        return Topic;
    }

    public void setTopic(String topic) {
        Topic = topic;
    }

    public MqttQoS getQoS() {
        return QoS;
    }

    public void setQoS(MqttQoS qoS) {
        QoS = qoS;
    }
}
