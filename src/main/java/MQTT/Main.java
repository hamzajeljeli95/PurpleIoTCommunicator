package MQTT;


import MQTT.Beans.Device;
import MQTT.Beans.MQTTBroker;
import com.jakewharton.fliptables.FlipTableConverters;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    static String username, password, gatewaytag;
    static Scanner input;
    static boolean goodconfig = false;
    static MQTTBroker broker;
    static List<Device> trustedSensors;
    static MqttClient client;

    public static void main(String[] args) {
        trustedSensors = new ArrayList<Device>();
        input = new Scanner(System.in);
        /**
         * Rading Configuration from the user
         */
        System.out.println("*** Purple IoT MQTT Broker Communicator 0.0.1 Started ! ***\n\n");
        while (goodconfig == false) {
            System.out.print("Please enter your Purple IoT Username : ");
            username = input.nextLine();
            System.out.print("Please enter your Purple IoT Password : ");
            password = input.nextLine();
            System.out.print("Please enter your Project's Gateway Tag : ");
            gatewaytag = input.nextLine();
            try {
                client = new MqttClient("tcp://41.229.118.249:33820", gatewaytag);
            } catch (MqttException e) {
                e.printStackTrace();
                System.err.println("Exiting");
                input.nextLine();
                System.exit(-1);
            }
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(username);
            options.setPassword(password.toCharArray());
            options.setConnectionTimeout(30);
            try {
                client.connect(options);
            } catch (MqttException e) {

            }
            if (client.isConnected()) {
                System.out.println("Configuration finished !");
                try {
                    client.disconnect();
                } catch (MqttException e) {

                }
                broker = new MQTTBroker(trustedSensors, client, options);
                goodconfig = true;
            } else {
                System.out.println("Incorrect configuration. Retrying ... \n\n");
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
            clearScreen();
        }
        processUserChoice(printcli());
    }

    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public static int printcli() {
        System.out.println("*** MAIN MENU ***\nCurrent IP Address is : " + getIpAddress() + ".\nBroker Status : " + brokerStatus() + ".\nPurple IoT Client Connected : " + client.isConnected() + "\n\nPlease choose an action : ");
        System.out.println("1) START/STOP MQTT Broker.");
        System.out.println("2) Show authorized sensors status.");
        System.out.println("3) Add authorized sensor.");
        System.out.println("4) Remove authorized sensor.");
        System.out.println("5) Exit.\n");
        System.out.print("CLI> ");
        try {
            return Integer.valueOf(input.nextLine());
        } catch (Exception e) {
            return -1;
        }
    }

    public static String getIpAddress() {
        try {
            final DatagramSocket socket = new DatagramSocket();
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            return socket.getLocalAddress().getHostAddress();
        } catch (Exception e) {
            return "0.0.0.0";
        }
    }

    public static String brokerStatus() {
        if (broker.isBrokerUp()) {
            return "Running";
        } else {
            return "Closed";
        }
    }

    public static void processUserChoice(int choice) {
        switch (choice) {
            case 1:
                startstopbroker();
                break;
            case 2:
                show();
                break;
            case 3:
                addDevice();
                break;
            case 4:
                removeDevice();
                break;
            case 5:
                initshutdown();
                break;
            default:
                System.out.println("Incorrect choice.");
                break;
        }
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
        clearScreen();
        processUserChoice(printcli());
    }

    private static void addDevice() {
        System.out.print("Device ID : ");
        String Device = input.nextLine();
        boolean deviceExists = false;
        for (Device D : trustedSensors) {
            if (D.getDeviceID().equals(Device)) {
                deviceExists = true;
                break;
            }
        }
        if (deviceExists == false) {
            trustedSensors.add(new Device(Device));
            System.out.println("Device (" + Device + ") added !");
        } else {
            System.out.println("Device (" + Device + ") exists already !");
        }
    }

    private static void removeDevice() {
        System.out.print("Device ID : ");
        String Device = input.nextLine();
        boolean done = false;
        for (Device D : trustedSensors) {
            if (D.getDeviceID().equals(Device)) {
                trustedSensors.remove(D);
                done = true;
                break;
            }
        }
        if (done) {
            System.out.println("Device (" + Device + ") removed !");
        } else {
            System.out.println("Device (" + Device + ") not found !");
        }
    }

    private static void show() {
        if (trustedSensors.size() > 0) {
            System.out.println(FlipTableConverters.fromIterable(trustedSensors, Device.class));
        } else {
            System.out.println("No sensors found.");
        }
        System.out.println("Press ENTER to continue ...");
        input.nextLine();
    }

    private static void startstopbroker() {
        if (broker.isBrokerUp()) {
            broker.closeServer();
        } else {
            broker.AcceptConnections();
        }
    }

    public static void initshutdown() {
        if (broker.isBrokerUp()) {
            broker.closeServer();
        }
        if (client.isConnected()) {
            try {
                client.disconnect();
            } catch (MqttException e) {

            }
        }
        System.exit(0);
    }
}
