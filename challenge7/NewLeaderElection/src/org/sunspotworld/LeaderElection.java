/*
 * SunSpotApplication.java
 *
 * Created on Dec 4, 2012 12:39:32 PM;
 */
package org.sunspotworld;

import com.sun.spot.io.j2me.radiogram.Radiogram;
import com.sun.spot.io.j2me.radiogram.RadiogramConnection;
import com.sun.spot.peripheral.Spot;
import com.sun.spot.peripheral.TimeoutException;
import com.sun.spot.peripheral.radio.IRadioPolicyManager;
import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.resources.Resources;
import com.sun.spot.resources.transducers.IAccelerometer3D;
import com.sun.spot.resources.transducers.ISwitch;
import com.sun.spot.resources.transducers.ISwitchListener;
import com.sun.spot.resources.transducers.ITriColorLED;
import com.sun.spot.resources.transducers.ITriColorLEDArray;
import com.sun.spot.resources.transducers.LEDColor;
import com.sun.spot.resources.transducers.SwitchEvent;
import com.sun.spot.service.BootloaderListenerService;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Utils;
import java.io.IOException;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;

import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

/**
 * The startApp method of this class is called by the VM to start the
 * application.
 *
 * The manifest specifies this class as MIDlet-1, which means it will be
 * selected for execution.
 */
public class LeaderElection extends MIDlet implements ISwitchListener {

    private class MessageType {

        static final int DISCOVERY = 1;
        static final int I_AM_LEADER = 2;
        static final int RESPONSE_TO_LEADER = 3;
        static final int DATA = 4;
    }
    private static final int NUMBER_OF_SPOTS_ALLOWED = 10;
    private static final int DISCOVERY_PERIOD = 10000;
    private static final int HEART_BEAT_PERIOD = 3000;
    private static final int PACKET_INTERVAL = 3000;
    private static final int CHANNEL_NUMBER = 11;
    private static final short PAN_ID = IRadioPolicyManager.DEFAULT_PAN_ID;
    private static final String DISCOVERY_PORT = "42";
    private static final String HEARTBEAT_PORT = "41";
    private static final String DATA_PORT = "52";
    private int power = 32;                             // Start with max transmit power
    private long myAddr; // own MAC addr (ID)
    private ITriColorLEDArray leds = (ITriColorLEDArray) Resources.lookup(ITriColorLEDArray.class);
    private ITriColorLED statusLED = leds.getLED(0);
    private IAccelerometer3D accel = (IAccelerometer3D) Resources.lookup(IAccelerometer3D.class);
    private ISwitch sw1 = (ISwitch) Resources.lookup(ISwitch.class, "SW1");
    private LEDColor red = new LEDColor(50, 0, 0);
    private LEDColor green = new LEDColor(0, 50, 0);
    private LEDColor blue = new LEDColor(0, 0, 50);
    private LEDColor white = new LEDColor(50, 50, 50);
    private LEDColor pink = new LEDColor(100, 0, 100);
    private Vector spots;
    private Vector isThere;
    private boolean doDiscover = false;
    private long leader = 0;
    private boolean doLeaderHeartbeat = false;
    private boolean doNonLeaderHeartbeat = false;
    private boolean doLeaderDataTransfer = false;
    private boolean doDataTransfer = false;
    private boolean respond = false;
    private long master = 0;
    private long slave = 0;
    private boolean needDiscovery;
    private long timeElected;
    private Node myself;
    private boolean doReset = false;
    private boolean infected = false;
    private int tilt = 0;
    private RadiogramConnection heartbeatRecConn;
    private RadiogramConnection heartbeatTransConn;
    private RadiogramConnection discoverRecConn;
    private RadiogramConnection discoverTransConn;
    private RadiogramConnection dataRecConn;
    private RadiogramConnection dataTransConn;

    protected void startApp() throws MIDletStateChangeException {
        initialize();
        sw1.addISwitchListener(this);
        timeElected = Long.MAX_VALUE;
        needDiscovery = true;
        while (true) {
            needDiscovery = false;
            doInitialDiscovery();
            if (leader <= 0) {
                selectLeader();
            }
            if (myAddr == leader) {
                timeElected = System.currentTimeMillis();

                myself = new Leader();
                myself.start();
            } else {
                myself = new Follower();
                myself.start();

            }
            while (!needDiscovery) {
                if (myAddr != leader) {
                    if (tilt > 0) {
                        leds.getLED(2).setColor(blue);
                    } else {
                        leds.getLED(2).setColor(red);
                    }
                    leds.getLED(2).setOn();
                }
                pause(1500);
            }
        }
    }

    private class Leader implements Node {

        public void start() {
            print("Starting as Leader");
            leds.getLED(1).setColor(white);
            leds.getLED(1).setOn();
            spots.removeElementAt(0);
            isThere = new Vector();
            for (int ii = 0; ii < spots.size(); ii++) {
                isThere.addElement(new Integer(2));
            }
            doLeaderHeartbeat = true;
            doNonLeaderHeartbeat = false;
            doDataTransfer = false;
            doLeaderDataTransfer = true;

            startHeartbeatConnections();
            startDataConnections();
            startThreads();
        }

        public void stop() {
            doLeaderHeartbeat = false;
            doDataTransfer = false;
        }

        public void doAction() {
            doReset = true;
        }

        private void leaderHeartbeatTransmit() {

            try {
                Radiogram xdg = (Radiogram) heartbeatTransConn.newDatagram(heartbeatTransConn.getMaximumLength());

                while (doLeaderHeartbeat) {
                    print("Sending heartbeat");
                    xdg.reset();
                    xdg.writeInt(MessageType.I_AM_LEADER);
                    xdg.writeLong(myAddr);
                    xdg.writeLong(timeElected);
                    heartbeatTransConn.send(xdg);

                    for (int ii = 0; ii < isThere.size(); ii++) {
                        int val = ((Integer) isThere.elementAt(ii)).intValue();
                        if (val <= 0) {
                            spots.removeElementAt(ii);
                            isThere.removeElementAt(ii);
                        } else {
                            isThere.setElementAt(new Integer(val - 1), ii);
                        }
                    }
                    adjustLights();
                    pause(HEART_BEAT_PERIOD);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        private void leaderResponseReciever() {

            try {
                Radiogram xdg = (Radiogram) heartbeatRecConn.newDatagram(heartbeatRecConn.getMaximumLength());

                while (doLeaderHeartbeat) {
                    xdg.reset();
                    try {
                        heartbeatRecConn.receive(xdg);
                        int message = xdg.readInt();
                        if (message == MessageType.RESPONSE_TO_LEADER) {
                            long srcLeader = xdg.readLong();
                            Long addr = new Long(xdg.readLong());
                            if (srcLeader == myAddr) {
                                print("Received Response From: " + IEEEAddress.toDottedHex(addr.longValue()));
                                if (!spots.contains(addr)) {
                                    spots.addElement(addr);
                                    isThere.addElement(new Integer(2));
                                } else {
                                    int index = spots.indexOf(addr);
                                    isThere.setElementAt(new Integer(2), index);
                                }
                            }
                        } else if (message == MessageType.I_AM_LEADER) {
                            long srcLeader = xdg.readLong();
                            long electionTime = xdg.readLong();

                            print("WTF? Leader Message");
                            if (electionTime < timeElected) {
                                print("Bullied out by: " + IEEEAddress.toDottedHex(srcLeader));
                                leader = srcLeader;
                                myself = new Follower();
                                myself.start();
                            }
                        }
                    } catch (TimeoutException t) {
                    }
                    pause(100);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        private void resetTransmitter() {
            try {
                Radiogram xdg = (Radiogram) dataTransConn.newDatagram(dataTransConn.getMaximumLength());

                while (doLeaderDataTransfer) {
                    xdg.reset();

                    if (doReset) {
                        print("Clearing infections.....");
                        xdg.writeInt(MessageType.DATA);
                        xdg.writeLong(myAddr);
                        xdg.writeBoolean(doReset);
                        xdg.writeInt(0);
                        doReset = false;
                        dataTransConn.send(xdg);
                    }
                    pause(500);
                }

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        private void startThreads() {
            new Thread() {
                public void run() {
                    leaderHeartbeatTransmit();
                }
            }.start();
            new Thread() {
                public void run() {
                    leaderResponseReciever();
                }
            }.start();

            new Thread() {
                public void run() {
                    resetTransmitter();
                }
            }.start();
        }
    }

    private void adjustLights() {
        leds.getLED(1).setOff();
        pause(250);
        leds.getLED(1).setOn();
        if (myAddr == leader) {
            print("Number of spots: " + spots.size());
            for (int ii = 2; ii <= (7 - spots.size()); ii++) {
                leds.getLED(ii).setOff();
            }
            for (int ii = 0; ii < spots.size(); ii++) {
                leds.getLED(7 - ii).setColor(blue);
                leds.getLED(7 - ii).setOn();
            }
        }
    }

    private class Follower implements Node {

        public void start() {
            print("Starting as Follower");
            leds.setOff();
            leds.getLED(0).setColor(green);
            leds.getLED(0).setOn();
            leds.getLED(1).setColor(pink);
            leds.getLED(1).setOn();
            spots.removeAllElements();
            doLeaderHeartbeat = false;
            doNonLeaderHeartbeat = true;
            doDataTransfer = true;
            doLeaderDataTransfer = false;
            startHeartbeatConnections();
            startDataConnections();
            startThreads();
        }

        public void stop() {
            doNonLeaderHeartbeat = false;
            doDataTransfer = false;
        }

        public void doAction() {
            infected = true;
        }

        /**
         * Receiver for a non-leader to receive the heartbeat from the leader.
         */
        private void heartBeatReciever() {
            try {

                Radiogram xdg = (Radiogram) heartbeatRecConn.newDatagram(heartbeatRecConn.getMaximumLength());
                long time = System.currentTimeMillis() + 2 * HEART_BEAT_PERIOD;

                while (doNonLeaderHeartbeat) {
                    xdg.reset();

                    try {
                        heartbeatRecConn.receive(xdg);

                        int message = xdg.readInt();
                        if (message == MessageType.I_AM_LEADER) {
                            long addr = xdg.readLong();
                            long electionTime = xdg.readLong();
                            print("Heartbeat from: " + IEEEAddress.toDottedHex(addr));
                            if (electionTime < timeElected) {
                                leader = addr;
                            }
                            if (addr == leader || leader == 0) {
                                time = System.currentTimeMillis() + 2 * HEART_BEAT_PERIOD;
                                respond = true;
                            }
                        }
                    } catch (TimeoutException t) {
                    }
                    if (System.currentTimeMillis() > time) {
                        //leader is gone rerun election   
                        print("Leader is gone!");
                        leader = 0;
                        doNonLeaderHeartbeat = false;
                        needDiscovery = true;
                        myself.stop();
                        break;
                    }
                    pause(HEART_BEAT_PERIOD / 2);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        private void responseTransmitter() {
            try {

                Radiogram xdg = (Radiogram) heartbeatTransConn.newDatagram(heartbeatTransConn.getMaximumLength());
                while (doNonLeaderHeartbeat) {
                    print(respond + "");
                    if (respond) {
                        print("Sending response to: " + IEEEAddress.toDottedHex(leader));
                        respond = false;
                        xdg.reset();
                        xdg.writeInt(MessageType.RESPONSE_TO_LEADER);
                        xdg.writeLong(leader);
                        xdg.writeLong(myAddr);
                        heartbeatTransConn.send(xdg);
                        adjustLights();
                    }
                    pause(1500);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        }

        private void startThreads() {
            new Thread() {
                public void run() {
                    heartBeatReciever();
                }
            }.start();
            new Thread() {
                public void run() {
                    responseTransmitter();
                }
            }.start();
            new Thread() {
                public void run() {
                    infectTransmitter();
                }
            }.start();
            new Thread() {
                public void run() {
                    dataReceiver();
                }
            }.start();
        }

        private void infectTransmitter() {
            try {
                Radiogram xdg = (Radiogram) dataTransConn.newDatagram(dataTransConn.getMaximumLength());

                while (doDataTransfer) {
                    xdg.reset();

                    if (infected) {
                        print("I am infected");
                        pause(3000);
                        if (infected) {
                            print("Did not see a reset -- I am infecting others....muhahaha");
                            xdg.writeInt(MessageType.DATA);
                            xdg.writeLong(myAddr);
                            xdg.writeBoolean(infected);
                            xdg.writeInt(tilt);
                            dataTransConn.send(xdg);
                        }
                    } else {
                        tilt = (accel.getTiltX() > 0) ? 1 : -1;
                    }
                    pause(500);
                }

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        private void dataReceiver() {
            try {
                Radiogram xdg = (Radiogram) dataRecConn.newDatagram(dataRecConn.getMaximumLength());

                while (doDataTransfer) {
                    xdg.reset();

                    dataRecConn.receive(xdg);
                    int message = xdg.readInt();

                    if (message == MessageType.DATA) {
                        long addr = xdg.readLong();
                        boolean action = xdg.readBoolean();
                        int infectedTilt = xdg.readInt();

                        if (addr == leader && action) {
                            print("The leader has cleared my infection!");
                            infected = false;
                            tilt = (accel.getTiltX() > 0) ? 1 : -1;
                        } else {
                            if (action) {
                                print("AHHH! I've been infected by: " + IEEEAddress.toDottedHex(addr));
                                infected = true;
                                tilt = infectedTilt;
                                pause(3000);
                            }
                        }
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void selectLeader() {
        long highest = 0;
        for (int ii = 0; ii < spots.size(); ii++) {
            long addr = ((Long) spots.elementAt(ii)).longValue();
            if (addr > highest) {
                highest = addr;
            }
        }

        leader = highest;
        System.out.println("The leader is: " + IEEEAddress.toDottedHex(leader));
    }

    private void initialize() {
        myAddr = RadioFactory.getRadioPolicyManager().getIEEEAddress();
        IRadioPolicyManager rpm = Spot.getInstance().getRadioPolicyManager();
        rpm.setChannelNumber(CHANNEL_NUMBER);
        rpm.setPanId(PAN_ID);
        rpm.setOutputPower(power - 32);
    }

    /**
     * Pause for a specified time.
     *
     * @param time the number of milliseconds to pause
     */
    private void pause(long time) {
        try {
            Thread.currentThread().sleep(time);
        } catch (InterruptedException ex) { /* ignore */ }
    }

    protected void pauseApp() {
        // This is not currently called by the Squawk VM
    }

    /**
     * Called if the MIDlet is terminated by the system. It is not called if
     * MIDlet.notifyDestroyed() was called.
     *
     * @param unconditional If true the MIDlet must cleanup and release all
     * resources.
     */
    protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {
    }

    private void doInitialDiscovery() {
        print("Go Go Discover");
        startDiscoveryConnections();
        long stopTime = System.currentTimeMillis() + DISCOVERY_PERIOD;
        spots = new Vector();
        spots.addElement(new Long(myAddr));
        doDiscover = true;
        startDiscovery();
        statusLED.setColor(red);     // Red = not active
        statusLED.setOn();
        while (System.currentTimeMillis() < stopTime) {
            print("Number of spots: " + spots.size());
            for (int ii = 2; ii <= (7 - spots.size()); ii++) {
                leds.getLED(ii).setOff();
            }
            for (int ii = 0; ii < spots.size(); ii++) {
                leds.getLED(7 - ii).setColor(blue);
                leds.getLED(7 - ii).setOn();
            }
            leds.getLED(1).setOff();
            pause(250);
            leds.getLED(1).setOn();
            pause(250);
        }
        doDiscover = false;
        leds.getLED(0).setColor(green);
    }

    private void startDiscovery() {
        new Thread() {
            public void run() {
                runDiscoveryTransmitter();
            }
        }.start();
        new Thread() {
            public void run() {
                runDiscoveryReciever();
            }
        }.start();
    }

    private void runDiscoveryReciever() {

        try {


            Radiogram xdg = (Radiogram) discoverRecConn.newDatagram(discoverRecConn.getMaximumLength());

            while (doDiscover) {
                xdg.reset();
                discoverRecConn.receive(xdg);
                int messageType = xdg.readInt();
                if (messageType == MessageType.DISCOVERY) {
                    Long addr = new Long(xdg.readLong());
                    if (!spots.contains(addr)) {
                        spots.addElement(addr);
                    }
                }
                pause(250);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void runDiscoveryTransmitter() {
        try {
            // transConn.setTimeout(PACKET_INTERVAL - 5);

            Radiogram xdg = (Radiogram) discoverTransConn.newDatagram(discoverTransConn.getMaximumLength());

            while (doDiscover) {
                xdg.reset();
                xdg.writeInt(MessageType.DISCOVERY);
                xdg.writeLong(myAddr);
                discoverTransConn.send(xdg);

                pause(250);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void switchPressed(SwitchEvent sw) {
        print("Switch Pressed");
        if (sw1.equals(sw.getSwitch()) && myself != null) {
            print("Do action");
            myself.doAction();
        }
    }

    public void switchReleased(SwitchEvent sw) {
        // do nothing
    }

    private void print(String message) {
        System.out.println(message);
    }

    private void startHeartbeatConnections() {
        if (heartbeatRecConn == null) {
            try {
                heartbeatRecConn = (RadiogramConnection) Connector.open("radiogram://:" + HEARTBEAT_PORT);
                heartbeatRecConn.setTimeout(HEART_BEAT_PERIOD * 2);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        if (heartbeatTransConn == null) {
            try {
                heartbeatTransConn = (RadiogramConnection) Connector.open("radiogram://broadcast:" + HEARTBEAT_PORT);
                heartbeatTransConn.setMaxBroadcastHops(1);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void startDiscoveryConnections() {
        if (discoverRecConn == null) {
            try {
                discoverRecConn = (RadiogramConnection) Connector.open("radiogram://:" + DISCOVERY_PORT);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        if (discoverTransConn == null) {
            try {
                discoverTransConn = (RadiogramConnection) Connector.open("radiogram://broadcast:" + DISCOVERY_PORT);
                discoverTransConn.setMaxBroadcastHops(1);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void startDataConnections() {
        if (dataRecConn == null) {
            try {
                dataRecConn = (RadiogramConnection) Connector.open("radiogram://:" + DATA_PORT);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        if (dataTransConn == null) {
            try {
                dataTransConn = (RadiogramConnection) Connector.open("radiogram://broadcast:" + DATA_PORT);
                dataTransConn.setMaxBroadcastHops(1);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
