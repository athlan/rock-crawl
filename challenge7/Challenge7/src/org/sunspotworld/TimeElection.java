/*
 * SunSpotApplication.java
 *
 * Created on Dec 5, 2012 9:00:14 PM;
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

import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

/**
 * The startApp method of this class is called by the VM to start the
 * application.
 *
 * The manifest specifies this class as MIDlet-1, which means it will be
 * selected for execution.
 */
public class TimeElection extends MIDlet {

    private static final int DISCOVERY_PERIOD = 10000;
    private static final int CHANNEL_NUMBER = 11;
    private static final short PAN_ID = IRadioPolicyManager.DEFAULT_PAN_ID;
    private static final String DISCOVERY_PORT = "42";
    private static final String HEARTBEAT_PORT = "41";
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
    private boolean needDiscovery;
    
   private class MessageType {

        static final int DISCOVERY = 1;
        static final int I_AM_LEADER = 2;
        static final int RESPONSE_TO_LEADER = 3;
        static final int DATA = 4;
    }

    protected void startApp() throws MIDletStateChangeException {
        System.out.println("Hello, world");
        BootloaderListenerService.getInstance().start();   // monitor the USB (if connected) and recognize commands from host

        long ourAddr = RadioFactory.getRadioPolicyManager().getIEEEAddress();
        System.out.println("Our radio address = " + IEEEAddress.toDottedHex(ourAddr));

        ISwitch sw1 = (ISwitch) Resources.lookup(ISwitch.class, "SW1");
        ITriColorLED led = leds.getLED(0);
        led.setRGB(100, 0, 0);                    // set color to moderate red
        while (sw1.isOpen()) {                  // done when switch is pressed
            led.setOn();                        // Blink LED
            Utils.sleep(250);                   // wait 1/4 seconds
            led.setOff();
            Utils.sleep(1000);                  // wait 1 second
        }
        notifyDestroyed();                      // cause the MIDlet to exit
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
            //rcvConn.setTimeout(PACKET_INTERVAL - 5);

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
           // transConn.setTimeout(PACKET_INTERVAL - 5);

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

    private void closeConnection(RadiogramConnection transConn) {
        if (transConn != null) {
            try {
                transConn.close();
            } catch (IOException ex) {
            }
        }
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
}
