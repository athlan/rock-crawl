/*
 * SunSpotApplication.java
 *
 * Created on Dec 4, 2012 12:39:32 PM;
 */
package org.sunspotworld;

import com.sun.spot.io.j2me.radiogram.Radiogram;
import com.sun.spot.io.j2me.radiogram.RadiogramConnection;
import com.sun.spot.peripheral.Spot;
import com.sun.spot.peripheral.radio.IRadioPolicyManager;
import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.resources.Resources;
import com.sun.spot.resources.transducers.ISwitch;
import com.sun.spot.resources.transducers.ITriColorLED;
import com.sun.spot.resources.transducers.ITriColorLEDArray;
import com.sun.spot.resources.transducers.LEDColor;
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
public class LeaderElection extends MIDlet {

    private void closeConnection(RadiogramConnection transConn) {
        if (transConn != null) {
            try {
                transConn.close();
            } catch (IOException ex) {
            }
        }
    }

    private class MessageType {

        static final int DISCOVERY = 1;
        static final int I_AM_LEADER = 2;
        static final int RESPONSE_TO_LEADER = 3;
        static final int LEADER_FOLLOWER = 4;
        static final int DATA = 5;
    }
    private static final int NUMBER_OF_SPOTS_ALLOWED = 10;
    private static final int DISCOVERY_PERIOD = 10000;
    private static final int HEART_BEAT_PERIOD = 3000;
    private static final int PACKET_INTERVAL = 3000;
    private static final int CHANNEL_NUMBER = 11;
    private static final short PAN_ID = IRadioPolicyManager.DEFAULT_PAN_ID;
    private static final String DISCOVERY_PORT = "42";
    private static final String HEARTBEAT_PORT = "41";
    private static final String MASTER_SLAVE_PORT = "43";
    private int power = 32;                             // Start with max transmit power
    private long myAddr; // own MAC addr (ID)
    private ITriColorLEDArray leds = (ITriColorLEDArray) Resources.lookup(ITriColorLEDArray.class);
    private ITriColorLED statusLED = leds.getLED(0);
    private LEDColor red = new LEDColor(50, 0, 0);
    private LEDColor green = new LEDColor(0, 50, 0);
    private LEDColor blue = new LEDColor(0, 0, 50);
    private LEDColor white = new LEDColor(50, 50, 50);
    private Vector spots;
    private int[] isthere;
    private boolean doDiscover = false;
    private long leader = 0;
    private boolean doLeaderHeartbeat = false;
    private boolean doNonLeaderHeartbeat = false;
    private boolean doMasterSlave = false;
    private boolean respond = false;
    private long master = 0;
    private long slave = 0;
    private boolean needDiscovery;

    protected void startApp() throws MIDletStateChangeException {
        initialize();
        needDiscovery = true;
        while (needDiscovery) {
            needDiscovery = false;
            doInitialDiscovery();            
            if (leader <= 0) {
                runLeaderElection();
            }
            if (myAddr == leader) {
                leds.getLED(1).setColor(white);
                leds.getLED(1).setOn();
                startLeaderThreads();
            } else {
                startNonLeaderThreads();
            }
            
            pause(1500);
        }
    }

    private void startLeaderThreads() {
        doLeaderHeartbeat = true;
        doNonLeaderHeartbeat = false;
        new Thread() {
            public void run() {
                leaderHeartbeatTransmit();
            }
        }.start();
//        new Thread() {
//            public void run() {
//                leaderResponseReciever();
//            }
//        }.start();
    }

    private void startNonLeaderThreads() {
        doLeaderHeartbeat = false;
        doNonLeaderHeartbeat = true;
        new Thread() {
            public void run() {
                heartBeatReciever();
            }
        }.start();
//        new Thread() {
//            public void run() {
//                responseTransmitter();
//            }
//        }.start();
    }

    private void leaderHeartbeatTransmit() {
        RadiogramConnection transConn = null;
        try {
            transConn = (RadiogramConnection) Connector.open("radiogram://broadcast:" + HEARTBEAT_PORT);
            transConn.setMaxBroadcastHops(1);

            Radiogram xdg = (Radiogram) transConn.newDatagram(transConn.getMaximumLength());

            while (doLeaderHeartbeat) {
                xdg.reset();
                xdg.writeInt(MessageType.I_AM_LEADER);
                xdg.writeLong(myAddr);
                transConn.send(xdg);

                for (int ii = 0; ii < spots.size(); ii++) {
                    isthere[ii]--;
                }
                pause(3000);
            }
        } catch (IOException ex) {
        } finally {
            closeConnection(transConn);
        }
    }

    private void leaderResponseReciever() {
        RadiogramConnection recConn = null;
        try {
            recConn = (RadiogramConnection) Connector.open("radiogram://:" + HEARTBEAT_PORT);
            recConn.setTimeout(PACKET_INTERVAL - 5);

            Radiogram xdg = (Radiogram) recConn.newDatagram(recConn.getMaximumLength());

            while (doLeaderHeartbeat) {
                xdg.reset();
                recConn.receive(xdg);
                int message = xdg.readInt();
                if (message == MessageType.RESPONSE_TO_LEADER) {
                    long srcLeader = xdg.readLong();
                    Long addr = new Long(xdg.readLong());
                    //@TODO if leader is 0 redo discovery
                    if (!spots.contains(addr)) {
                        //need to redo table generation
                    } else {
                        int index = spots.indexOf(addr);
                        isthere[index] = 2;
                    }
                }
                pause(100);
            }
        } catch (IOException ex) {
        } finally {
            closeConnection(recConn);
        }
    }

    /**
     * Receiver for a non-leader to receive the heartbeat from the leader.
     */
    private void heartBeatReciever() {
        RadiogramConnection recConn = null;
        try {
            recConn = (RadiogramConnection) Connector.open("radiogram://:" + HEARTBEAT_PORT);
            recConn.setTimeout(PACKET_INTERVAL - 5);

            Radiogram xdg = (Radiogram) recConn.newDatagram(recConn.getMaximumLength());
            long time = System.currentTimeMillis() + 2 * HEART_BEAT_PERIOD;
            while (doNonLeaderHeartbeat) {
                xdg.reset();
                recConn.receive(xdg);
                int message = xdg.readInt();
                if (message == MessageType.I_AM_LEADER) {
                    long addr = xdg.readLong();
                    if (addr == leader || leader == 0) {
                        time = System.currentTimeMillis() + 2 * HEART_BEAT_PERIOD;
                        respond = true;
                    }
                }
                if (System.currentTimeMillis() < time) {
                    //leader is gone rerun election
                    doNonLeaderHeartbeat = false;
                    needDiscovery = true;
                    break;
                }
                pause(1500);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            closeConnection(recConn);
        }
    }

    private void responseTransmitter() {
        RadiogramConnection transConn = null;
        try {
            transConn = (RadiogramConnection) Connector.open("radiogram://broadcast:" + HEARTBEAT_PORT);
            transConn.setMaxBroadcastHops(1);

            Radiogram xdg = (Radiogram) transConn.newDatagram(transConn.getMaximumLength());
            while (doNonLeaderHeartbeat) {
                if (respond) {
                    respond = false;
                    xdg.reset();
                    xdg.writeInt(MessageType.RESPONSE_TO_LEADER);
                    xdg.writeLong(leader);
                    xdg.writeLong(myAddr);
                    transConn.send(xdg);
                }
                pause(1500);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            closeConnection(transConn);
        }

    }

    private void runLeaderElection() {
        long highest = 0;
        if (spots.size() > 1) {
            for (int ii = 0; ii < spots.size(); ii++) {
                long addr = ((Long) spots.elementAt(ii)).longValue();
                if (addr > highest) {
                    highest = addr;
                }
            }
        }
        leader = highest;
        System.out.println("The leader is: " + IEEEAddress.toDottedHex(leader));
    }

    private void initialize() {
        myAddr = RadioFactory.getRadioPolicyManager().getIEEEAddress();
        statusLED.setColor(red);     // Red = not active
        statusLED.setOn();
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
        long stopTime = System.currentTimeMillis() + DISCOVERY_PERIOD;
        spots = new Vector();
        spots.addElement(new Long(myAddr));
        doDiscover = true;
        startDiscovery();
        while (System.currentTimeMillis() < stopTime) {
            for (int ii = 0; ii < spots.size(); ii++) {
                leds.getLED(7 - ii).setColor(blue);
                leds.getLED(7 - ii).setOn();
            }
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
        RadiogramConnection rcvConn = null;

        try {
            rcvConn = (RadiogramConnection) Connector.open("radiogram://:" + DISCOVERY_PORT);
            rcvConn.setTimeout(PACKET_INTERVAL - 5);

            Radiogram xdg = (Radiogram) rcvConn.newDatagram(rcvConn.getMaximumLength());

            while (doDiscover) {
                xdg.reset();
                rcvConn.receive(xdg);
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
        } finally {
            closeConnection(rcvConn);
        }
    }

    private void runDiscoveryTransmitter() {
        RadiogramConnection transConn = null;
        try {
            transConn = (RadiogramConnection) Connector.open("radiogram://broadcast:" + DISCOVERY_PORT);
            transConn.setMaxBroadcastHops(1);
            transConn.setTimeout(PACKET_INTERVAL - 5);

            Radiogram xdg = (Radiogram) transConn.newDatagram(transConn.getMaximumLength());

            while (doDiscover) {
                xdg.reset();
                xdg.writeInt(MessageType.DISCOVERY);
                xdg.writeLong(myAddr);
                transConn.send(xdg);

                pause(250);
            }
        } catch (IOException ex) {
        } finally {
            closeConnection(transConn);
        }
    }
}
