/*
 * Copyright (c) 2006-2010 Sun Microsystems, Inc.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to 
 * deal in the Software without restriction, including without limitation the 
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or 
 * sell copies of the Software, and to permit persons to whom the Software is 
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
 * DEALINGS IN THE SOFTWARE.
 */
package org.sunspotworld.demo;

import com.sun.spot.peripheral.Spot;
import com.sun.spot.peripheral.TimeoutException;
import com.sun.spot.peripheral.radio.IProprietaryRadio;
import com.sun.spot.peripheral.radio.IRadioPolicyManager;
import com.sun.spot.resources.Resources;
import com.sun.spot.resources.transducers.ILed;
import com.sun.spot.resources.transducers.ISwitch;
import com.sun.spot.resources.transducers.ITriColorLED;
import com.sun.spot.resources.transducers.LEDColor;
import com.sun.spot.resources.transducers.ITriColorLEDArray;
import com.sun.spot.io.j2me.radiogram.Radiogram;
import com.sun.spot.io.j2me.radiogram.RadiogramConnection;
import com.sun.spot.util.IEEEAddress;

import java.io.IOException;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.io.DatagramConnection;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

public class SunSpotApplication extends MIDlet {

    private static final String VERSION = "1.0";
    private static final int INITIAL_CHANNEL_NUMBER = IProprietaryRadio.DEFAULT_CHANNEL;
    private static final short PAN_ID = IRadioPolicyManager.DEFAULT_PAN_ID;
    private static final String SENDBROADCAST_PORT = "48";
    private static final String RECBROADCAST_PORT = "112";
    private static final int RADIO_TEST_PACKET = 110;
    private static final int CHECK_PER_SECOND = 30;
    private static final int PACKET_INTERVAL = 1000 / CHECK_PER_SECOND;
    private ITriColorLEDArray leds = (ITriColorLEDArray) Resources.lookup(ITriColorLEDArray.class);
    private ITriColorLED sendLED = leds.getLED(6);
    private ITriColorLED recvLED = leds.getLED(7);
    private ITriColorLED Beacon1LED = leds.getLED(1);
    private ITriColorLED Beacon2LED = leds.getLED(2);
    private ITriColorLED Beacon3LED = leds.getLED(3);
    private ITriColorLED Beacon4LED = leds.getLED(4);
    private LEDColor red = new LEDColor(50, 0, 0);
    private LEDColor green = new LEDColor(0, 20, 0);
    private LEDColor blue = new LEDColor(0, 0, 50);
    private LEDColor white = new LEDColor(255, 255, 255);
    private int channel = INITIAL_CHANNEL_NUMBER;
    private int power = 32;                             // Start with max transmit power
    private int CurrentRssi = 0;
    private double LastDistance = 0;
    private double CurrentDistance = 0;
    private String CurrentBeacon = "0";

    /**
     * Return bright or dim red.
     *
     * @returns appropriately bright red LED settings
     */
    private LEDColor getRed() {
        return red;
    }

    /**
     * Return bright or dim green.
     *
     * @returns appropriately bright green LED settings
     */
    private LEDColor getGreen() {
        return green;
    }

    /**
     * Return bright or dim blue.
     *
     * @returns appropriately bright blue LED settings
     */
    private LEDColor getBlue() {
        return blue;
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

    /**
     * Initialize any needed variables.
     */
    private void initialize() {
        sendLED.setOff();
        recvLED.setOff();
        Beacon1LED.setOff();
        Beacon2LED.setOff();
        Beacon3LED.setOff();
        Beacon4LED.setOff();

        IRadioPolicyManager rpm = Spot.getInstance().getRadioPolicyManager();
        rpm.setChannelNumber(channel);
        rpm.setPanId(PAN_ID);
        rpm.setOutputPower(power - 32);
        CurrentRssi = 0;
        LastDistance = 0;
        CurrentDistance = 0;
    }

    /**
     * Main application run loop.
     */
    private void run() {
        System.out.println("Radio Signal Strength Test (version " + VERSION + ")");
        System.out.println("Packet interval = " + PACKET_INTERVAL + " msec");

        new Thread() {
            public void run() {
                xmitLoop();
            }
        }.start();                      // spawn a thread to transmit packets
        new Thread() {
            public void run () {
                recvLoop();
            }
        }.start();                      // spawn a thread to receive packets
    }

    /**
     * Loop to continually transmit packets using current power level & channel
     * setting.
     */
    private void xmitLoop() {
        DatagramConnection txConn = null;
        Datagram xdg = null;

        try {
            // specify broadcast_port
            txConn = (DatagramConnection) Connector.open("radiogram://broadcast:" + SENDBROADCAST_PORT);

            xdg = txConn.newDatagram(50);
            System.out.println("Maxleng for Packet is : " + txConn.getMaximumLength());
        } catch (IOException ex) {
            System.out.println("Could not open radiogram broadcast connection");
            ex.printStackTrace();
            return;
        }

        while (true) {
            try {
                if(CurrentDistance==LastDistance) continue;
                

                sendLED.setColor(getGreen());
                sendLED.setOn();
                long nextTime = System.currentTimeMillis() + PACKET_INTERVAL;
                xdg.reset();
                xdg.writeLong(IEEEAddress.toLong(CurrentBeacon));
                xdg.writeDouble(CurrentDistance);
                txConn.send(xdg);
                sendLED.setOff();
                LastDistance = CurrentDistance;
                long delay = (nextTime - System.currentTimeMillis()) - 2;
                if (delay > 0) {
                    pause(delay);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

    }

    /**
     * Loop to receive packets and display their RSSI level in the LEDs
     */
    private void recvLoop() {
        RadiogramConnection rcvConn = null;
        int nothing = 0;

        try {
            rcvConn = (RadiogramConnection) Connector.open("radiogram://:" + RECBROADCAST_PORT);
            rcvConn.setTimeout(PACKET_INTERVAL - 5);
            Radiogram rdg = (Radiogram) rcvConn.newDatagram(rcvConn.getMaximumLength());
            while (true) {
                try {
                    rdg.reset();
                    Beacon1LED.setOff();
                    Beacon2LED.setOff();
                    Beacon3LED.setOff();
                    Beacon4LED.setOff();

                    rcvConn.receive(rdg);           // listen for a packet
                    byte packetType = rdg.readByte();
                    if (packetType == RADIO_TEST_PACKET) {
                        recvLED.setColor(getGreen());
                        recvLED.setOn();

                        CurrentBeacon = rdg.getAddress();
                        CurrentRssi = rdg.getRssi();
                        if (CurrentBeacon.equals("0014.4F01.0000.4120")) {
                            Beacon1LED.setColor(getGreen());
                            Beacon1LED.setOn();
                        } else if (CurrentBeacon.equals("0014.4F01.0000.43CC")) {
                            Beacon2LED.setColor(getGreen());
                            Beacon2LED.setOn();
                        } else if (CurrentBeacon.equals("0014.4F01.0000.45B4")) {
                            Beacon3LED.setColor(getGreen());
                            Beacon3LED.setOn();
                        } else if (CurrentBeacon.equals("0014.4F01.0000.4396")) {
                            Beacon4LED.setColor(getGreen());
                            Beacon4LED.setOn();
                        } else {
                            recvLED.setColor(getRed());
                        }



                        CurrentDistance = 0.078 * (CurrentRssi) * (CurrentRssi) + 0.025 * (CurrentRssi) - 4.792;
                        System.out.println(CurrentBeacon + ", Last rssi = " + CurrentRssi + "CurrentDistance" + CurrentDistance);
                        nothing = 0;
                        recvLED.setOff();
                    }
                } catch (TimeoutException tex) {        // timeout - display no packet received
                    recvLED.setColor(getRed());
                    recvLED.setOn();
                    nothing++;
                    if (nothing > 2 * CHECK_PER_SECOND) {
                        recvLED.setColor(getBlue());  // if nothing received eventually turn off LEDs
                    }
                }
            }
        } catch (IOException ex) {
            // ignore
        } finally {
            if (rcvConn != null) {
                try {
                    rcvConn.close();
                } catch (IOException ex) {
                }
            }

        }
    }

    /**
     * MIDlet call to start our application.
     */
    protected void startApp() throws MIDletStateChangeException {
        // Listen for downloads/commands over USB connection
        new com.sun.spot.service.BootloaderListenerService().getInstance().start();
        initialize();
        run();
    }

    /**
     * This will never be called by the Squawk VM.
     */
    protected void pauseApp() {
        // This will never be called by the Squawk VM
    }

    /**
     * Called if the MIDlet is terminated by the system.
     *
     * @param unconditional If true the MIDlet must cleanup and release all
     * resources.
     */
    protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {
    }
}
