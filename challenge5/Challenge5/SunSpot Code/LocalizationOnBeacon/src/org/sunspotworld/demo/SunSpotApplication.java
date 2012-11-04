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
import com.sun.spot.resources.transducers.ILightSensor;
import com.sun.spot.resources.transducers.ITriColorLED;
import com.sun.spot.resources.transducers.LEDColor;
import com.sun.spot.resources.transducers.ITriColorLEDArray;
import com.sun.spot.io.j2me.radiogram.Radiogram;
import com.sun.spot.io.j2me.radiogram.RadiogramConnection;

import java.io.IOException;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;


public class SunSpotApplication extends MIDlet {

    private static final String VERSION = "1.0";
    private static final int INITIAL_CHANNEL_NUMBER = IProprietaryRadio.DEFAULT_CHANNEL;
    private static final short PAN_ID               = IRadioPolicyManager.DEFAULT_PAN_ID;
    private static final String BEACONBROADCAST_PORT  = "112";
    private static final int RADIO_TEST_PACKET      = 110;
    private static final int PACKETS_PER_SECOND     = 2;
    private static final int PACKET_INTERVAL        = 1000 / PACKETS_PER_SECOND;
    
    
    private int channel = INITIAL_CHANNEL_NUMBER;
    private int power = 32;                             // Start with max transmit power
    
	

    
    /**
     * Pause for a specified time.
     *
     * @param time the number of milliseconds to pause
     */
    private void pause (long time) {
        try {
            Thread.currentThread().sleep(time);
        } catch (InterruptedException ex) { /* ignore */ }
    }
    

    /**
     * Initialize any needed variables.
     */
    private void initialize() { 
        
        IRadioPolicyManager rpm = Spot.getInstance().getRadioPolicyManager();
        rpm.setChannelNumber(channel);
        rpm.setPanId(PAN_ID);
        rpm.setOutputPower(power - 32);
        
    }
    

    /**
     * Main application run loop.
     */
    private void run() {
        System.out.println("Radio Signal Strength Test (version " + VERSION + ")");
        System.out.println("Packet interval = " + PACKET_INTERVAL + " msec");
        
        new Thread() {
            public void run () {
                xmitLoop();
            }
        }.start();                      // spawn a thread to transmit packets
       
    }


    /**
     * Loop to continually transmit packets using current power level & channel setting.
     */
    private void xmitLoop () {
        RadiogramConnection txConn = null;
        String ourAddress = System.getProperty("IEEE_ADDRESS");

        while (true) {
            try {
                txConn = (RadiogramConnection)Connector.open("radiogram://0014.4F01.0000.45B3:" + BEACONBROADCAST_PORT);
                txConn.setMaxBroadcastHops(1);      // don't want packets being rebroadcasted
                Datagram xdg = txConn.newDatagram(txConn.getMaximumLength());
                while (true) {
                    long nextTime = System.currentTimeMillis() + PACKET_INTERVAL;
                    
                    xdg.reset();
                    xdg.writeByte(RADIO_TEST_PACKET);
					xdg.writeLong(ourAddress);
                    txConn.send(xdg);
                    long delay = (nextTime - System.currentTimeMillis()) - 2;
                    if (delay > 0) {
                        pause(delay);
                    }
                }
            } catch (IOException ex) {
                // ignore
            } finally {
                if (txConn != null) {
                    try {
                        txConn.close();
                    } catch (IOException ex) { }
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
     * @param unconditional If true the MIDlet must cleanup and release all resources.
     */
    protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {
    }

}
