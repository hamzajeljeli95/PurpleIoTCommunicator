package MQTT.Beans;

import com.google.gson.JsonObject;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.dns.AddressResolverOptions;
import io.vertx.mqtt.MqttEndpoint;
import io.vertx.mqtt.MqttServer;
import io.vertx.mqtt.MqttServerOptions;
import io.vertx.mqtt.MqttTopicSubscription;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * -          -
 * /!\ SECRET /!\
 * ---        ---
 *
 * @author : Hamza Jeljeli
 * Creation Date : 28/02/19
 * Last Edit : 04/03/19
 * Documentation : https://vertx.io/docs/vertx-mqtt-server/java/
 * Content : This is an MQTT BROKER Based on Vert.x 3.4.2 API, It can handle almost every MQTT operation.
 * The server is running with Logging on console.
 * The work will be done in the handlePublish procedure.
 */

public class MQTTBroker extends AbstractVerticle {

    /**
     * Editabale Variables
     */
    private static int BROKER_PORT = 1883;
    private static String OK_RESPONSE_MESSAGE = "OK";
    private static String NOK_RESPONSE_MESSAGE = "NOT_OK";
    /**
     * END.
     */

    MqttServer mqttServer;
    List<SubscribedDevice> clients;
    boolean isrunning;
    List<Device> trustedSensors;
    MqttClient client;
    MqttConnectOptions options;

    public MQTTBroker(List<Device> trustedSensors, MqttClient client, MqttConnectOptions connectOptions) {
        MqttServerOptions serverOptions = new MqttServerOptions()
                .setPort(BROKER_PORT);
        Vertx vertx = Vertx.vertx(new VertxOptions().
                setAddressResolverOptions(new AddressResolverOptions().addServer("8.8.8.8")));
        vertx.deployVerticle(this);
        isrunning = false;
        mqttServer = MqttServer.create(vertx, serverOptions);
        this.trustedSensors = trustedSensors;
        this.client = client;
        this.options = connectOptions;
    }

    public void AcceptConnections() {
        try {
            client.connect(options);
        } catch (MqttException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        mqttServer.endpointHandler(endpoint -> {
            /**
             * Authenticate Sensor
             */
            boolean isfound = false;
            for (Device D : trustedSensors) {
                if (D.getDeviceID().equals(endpoint.clientIdentifier())) {
                    D.setConnected(true);
                    isfound = true;
                    break;
                }
            }
            if (isfound) {
                endpoint.accept(true);
                handleSubscription(endpoint);
                handleUnsubscription(endpoint);
                handlePublish(endpoint);
                handleClientDisconnect(endpoint);
                pingHandler(endpoint);
                handleExceptions(endpoint);
            } else {
                endpoint.accept(false);
            }
            /**
             * END.
             */
        }).listen(ar -> {
            if (ar.succeeded()) {
                clients = new ArrayList<SubscribedDevice>();
                System.out.println("MQTT server is listening on port " + ar.result().actualPort());
            } else {
                System.out.println("Error on starting the server");
                ar.cause().printStackTrace();
            }
        });
        isrunning = true;
    }

    private void handleSubscription(MqttEndpoint endpoint) {
        endpoint.subscribeHandler(subscribe -> {
            List<MqttQoS> grantedQosLevels = new ArrayList<>();
            for (MqttTopicSubscription s : subscribe.topicSubscriptions()) {
                grantedQosLevels.add(s.qualityOfService());
                clients.add(new SubscribedDevice(endpoint, s.topicName(), s.qualityOfService()));
            }
            endpoint.subscribeAcknowledge(subscribe.messageId(), grantedQosLevels);
        });
    }

    private void handleUnsubscription(MqttEndpoint endpoint) {
        endpoint.unsubscribeHandler(unsubscribe -> {
            for (String t : unsubscribe.topics()) {
                for (int i = 0; i < clients.size(); i++) {
                    if ((endpoint.clientIdentifier().equals(clients.get(i).getEndpoint().clientIdentifier())) && (t.equals(clients.get(i).getTopic()))) {
                        clients.remove(i);
                        break;
                    }
                }
            }
            // ack the subscriptions request
            endpoint.unsubscribeAcknowledge(unsubscribe.messageId());
        });
    }

    private void handlePublish(MqttEndpoint endpoint) {
        endpoint.publishAcknowledgeHandler(messageId -> {
            //System.out.println("Received ack for message = " + messageId);
        });
        endpoint.publishReceivedHandler(messageId -> {
            endpoint.publishRelease(messageId);
        });
        endpoint.publishCompleteHandler(messageId -> {
            //System.out.println("Received ack for message = " + messageId);
        });
        endpoint.publishHandler(message -> {
            boolean isfound = false;
            for (Device D : trustedSensors) {
                if (D.getDeviceID().equals(endpoint.clientIdentifier())) {
                    isfound = true;
                    break;
                }
            }
            if (isfound) {
                JsonObject msg = new JsonObject();
                msg.addProperty("SID", endpoint.clientIdentifier());
                msg.addProperty("VAL", message.payload().toString(Charset.defaultCharset()));
                MqttMessage mqttmsg = new MqttMessage();
                mqttmsg.setPayload(msg.toString().getBytes());
                mqttmsg.setRetained(message.isRetain());
                mqttmsg.setQos(message.qosLevel().value());
                try {
                    client.publish(message.topicName(), mqttmsg);
                } catch (MqttException e) {
                    System.err.println("Failed to publish message from " + endpoint.clientIdentifier() + " to Purple IoT Broker. Reason : " + e.getMessage());
                }
                endpoint.publish(message.topicName(),
                        Buffer.buffer(OK_RESPONSE_MESSAGE),
                        message.qosLevel(),
                        message.isDup(),
                        message.isRetain());
                /**
                 * TODO : Handle Data Storage
                 * Using the @message and @endpoint variables to database of your type.
                 * just keep it this way for no persistance.
                 */
                for (SubscribedDevice e : clients) {
                    if ((!e.getEndpoint().clientIdentifier().equals(endpoint.clientIdentifier())) && (e.getTopic().equals(message.topicName())))
                        e.getEndpoint().publish(message.topicName(),
                                Buffer.buffer(message.payload().toString(Charset.defaultCharset())),
                                message.qosLevel(),
                                message.isDup(),
                                message.isRetain());
                }
                if (message.qosLevel() == MqttQoS.AT_LEAST_ONCE) {
                    endpoint.publishAcknowledge(message.messageId());
                } else if (message.qosLevel() == MqttQoS.EXACTLY_ONCE) {
                    //endpoint.publishRelease(message.messageId());
                }
            } else {
                endpoint.publish(message.topicName(),
                        Buffer.buffer(NOK_RESPONSE_MESSAGE),
                        message.qosLevel(),
                        message.isDup(),
                        message.isRetain());
                endpoint.close();
            }
        });
        endpoint.publishReleaseHandler(messageId -> {
            endpoint.publishComplete(messageId);
        });
    }

    private void pingHandler(MqttEndpoint endpoint) {
        endpoint.pingHandler(v -> {
            System.out.println("Ping received from client " + endpoint.clientIdentifier());
        });
    }

    private void handleClientDisconnect(MqttEndpoint endpoint) {
        endpoint.disconnectHandler(h -> {
            for (Device D : trustedSensors) {
                if (D.getDeviceID().equals(endpoint.clientIdentifier())) {
                    D.setConnected(false);
                    break;
                }
            }
        });
    }

    private void handleExceptions(MqttEndpoint endpoint) {
        endpoint.exceptionHandler(Throwable::printStackTrace);
    }

    public void closeServer() {
        mqttServer.close(v -> {
            for (Device D : trustedSensors) {
                D.setConnected(false);
            }
            System.out.println("MQTT server closed.");
        });
        isrunning = false;
    }

    private MqttQoS getQosLevel(int qosLevel) {
        switch (qosLevel) {
            case 0:
                return MqttQoS.AT_LEAST_ONCE;
            case 1:
                return MqttQoS.AT_MOST_ONCE;
            case 2:
                return MqttQoS.EXACTLY_ONCE;
            default:
                return MqttQoS.FAILURE;
        }
    }

    public boolean isBrokerUp() {
        return isrunning;
    }
}

