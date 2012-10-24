/*
 * Copyright (c) 2006-2010 Sun Microsystems, Inc.
 * Copyright (c) 2010 Oracle
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.sunspotworld.demo;

import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.resources.Resources;
import com.sun.spot.resources.transducers.ISwitch;
import com.sun.spot.resources.transducers.ITriColorLED;
import com.sun.spot.resources.transducers.ITriColorLEDArray;
import com.sun.spot.service.BootloaderListenerService;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Utils;

import javax.microedition.midlet.MIDlet;
//import javax.microedition.midlet.MIDletStateChangeException;

import com.sun.spot.io.j2me.radiogram.*;
import com.sun.spot.sensorboard.peripheral.TriColorLED;

import java.io.IOException;

import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.io.DatagramConnection;
//import javax.microedition.io.DatagramConnection;
import javax.microedition.midlet.MIDletStateChangeException;


/**
 * The startApp method of this class is called by the VM to start the
 * application.
 *
 * The manifest specifies this class as MIDlet-1, which means it will
 * be selected for execution.
 */
public class SunSpotApplication extends MIDlet {

    
    private ITriColorLEDArray ledArray = (ITriColorLEDArray) Resources.lookup(ITriColorLEDArray.class);
    
    protected void startApp() throws MIDletStateChangeException {
        System.out.println("Hello, world");
        BootloaderListenerService.getInstance().start();   // monitor the USB (if connected) and recognize commands from host

        long ourAddr = RadioFactory.getRadioPolicyManager().getIEEEAddress();
        System.out.println("Our radio address = " + IEEEAddress.toDottedHex(ourAddr));
        
        startReceiver();
        startSenderThread();
//        ISwitch sw1 = (ISwitch) Resources.lookup(ISwitch.class, "SW1");
       
        //notifyDestroyed();                      // cause the MIDlet to exit
    }
    
     synchronized public void startSenderThread() {
        new Thread() {
            public void run() {
                // We create a DatagramConnection
                RadiogramConnection dgConnection = null;
                Datagram dg = null;
                try {
                    // The Connection is a broadcast so we specify it in the creation string
                    dgConnection = (RadiogramConnection) Connector.open("radiogram://broadcast:38");
                    // Then, we ask for a datagram with the maximum size allowed
                    dg = dgConnection.newDatagram(dgConnection.getMaximumLength());
                } catch (IOException ex) {
                    System.out.println("Could not open radiogram broadcast connection");
                    ex.printStackTrace();
                    return;
                }
                
                while(true){
                    try {
                         int status = 0;
                        
                        for(int i = 0; i < 8; i++) {
                            if (ledArray.getLED(i).isOn()) {
                                status |= (1 << (7-i));
                            }
                        }
                        
                        dg.reset();
                        dg.writeInt(status);
                        dgConnection.send(dg);
                                               
                        System.out.println("Broadcast is going through");
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    Utils.sleep(500);
                }
            }
        }.start();
    }
    
      public void startReceiver() {
          new Thread() {
              public void run() {
          
                RadiogramConnection dgConnection = null;
                Datagram dg = null;
                int signal = 0;
                
                        
     
                try {
                    dgConnection = (RadiogramConnection) Connector.open("radiogram://:37");
                    // Then, we ask for a datagram with the maximum size allowed
                    dg = dgConnection.newDatagram(dgConnection.getMaximumLength());
                } catch (IOException e) {
                    System.out.println("Could not open radiogram receiver connection");
                    e.printStackTrace();
                    return;
                }
                
                while(true){
                    try {
                        dg.reset();
                        dgConnection.receive(dg);
                        signal=dg.readInt();
                        char color = dg.readChar();
                        System.out.println("Received: " + signal+" from " + dg.getAddress());
                        
                        ITriColorLED led = ledArray.getLED(signal-1);
                        
                        switchLED(led);
                        
                        switch (color) {
                            case 'b': led.setRGB(0, 0, 255); break;
                            case 'g': led.setRGB(0, 255, 0); break;
                            case 'r': led.setRGB(255, 0, 0); break;
                            case 'w': led.setRGB(255, 255, 255); break;
                            default:
                        }
                       
                        
                        
                        
                    } catch (IOException e) {
                       System.out.println("Nothing received");
                    }
                }
              }
          }.start();
      }
    
    
    private void switchLED(ITriColorLED led) {
        if (led.isOn()) {
            led.setOff();
        } else {
            led.setOn();
        }
    }
    
    
    
    
    protected void pauseApp() {
        // This is not currently called by the Squawk VM
    }
    
    /**
     * Called if the MIDlet is terminated by the system.
     * I.e. if startApp throws any exception other than MIDletStateChangeException,
     * if the isolate running the MIDlet is killed with Isolate.exit(), or
     * if VM.stopVM() is called.
     * 
     * It is not called if MIDlet.notifyDestroyed() was called.
     *
     * @param unconditional If true when this method is called, the MIDlet must
     *    cleanup and release all resources. If false the MIDlet may throw
     *    MIDletStateChangeException  to indicate it does not want to be destroyed
     *    at this time.
     */
    protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {
        ledArray.setOff();
    }
}
