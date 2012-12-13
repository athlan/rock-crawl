/*
 * SunSpotApplication.java
 *
 * Created on Dec 12, 2012 10:25:05 PM;
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
public class OnSpot extends MIDlet {
    
    private static final int SERVO_CENTER_VALUE = 1500;


    private static class Speeds {

        private static final int SLOW = 1;
        private static final int FAST = 2;
    }

    private static class Directions {

        private static final int FORWARD = 5;
        private static final int RIGHT = 6;
        private static final int LEFT = 7;
        private static final int BACKWARD = 8;
        private static final int STOP = 9;
        private static final int CENTER = 10;
    }
    private static final int CHANNEL_NUMBER = 11;
    private static final short PAN_ID = IRadioPolicyManager.DEFAULT_PAN_ID;
    private int power = 32;                             // Start with max transmit power
    private long myAddr; // own MAC addr (ID)
    private static final String DIRECTION_PORT = "53";
    private static final String SPEED_PORT = "57";
    private static final String RESPONSE_PORT = "42";
    private RadiogramConnection directionRadio;
    private RadiogramConnection speedRadio;
    private RadiogramConnection responseRadio;
    private ITriColorLEDArray leds = (ITriColorLEDArray) Resources.lookup(ITriColorLEDArray.class);
    private int stepValue = 100;

    
    private boolean appRun;
    private int speed;
    private int direction;
    private int respond;

    protected void startApp() throws MIDletStateChangeException {
        speed = Speeds.SLOW;
        direction = Directions.STOP;
        respond = 0;
        appRun = true;
        initialize();
        led(0, LEDColor.WHITE);
        led(0, true);
        openRadios();      
        startRadioThreads();
        new Thread() {
            public void run() {
                frController();
            }
        }.start();
        new Thread() {
            public void run() {
                sendResponse();
            }
        }.start();
        while (appRun) {
            pause(2500);
        }
    }
    
    public void frController() {
        while (appRun) {
            print("Direction is: " + direction);
            led(7, false);
            switch (direction) {
                case Directions.FORWARD:
                    led(1, LEDColor.GREEN);                    
                    break;
                case Directions.BACKWARD:
                    led(1, LEDColor.RED);
                    break;
                case Directions.RIGHT:
                    led(2, LEDColor.GREEN);
                    break;
                case Directions.LEFT:
                    led(2, LEDColor.RED);
                    break;
                case Directions.CENTER:
                    led(2, LEDColor.BLUE);
                    break;
                case Directions.STOP:
                    led(1, LEDColor.BLUE);
                    break;
                default:   
                    led(7, LEDColor.CYAN);
                    led(7, true);
            }
            led(1, true);
            led(2, true);
            pause(250);
        }
    }
       
    
    public void startRadioThreads() {
        new Thread() {
            public void run() {
                getSpeedValue();
            }
        }.start();
        new Thread() {
            public void run() {
                getDirectionValue();
            }
        }.start();        
    }
    
    public void getSpeedValue() {
        try {
            Radiogram xdg = (Radiogram) speedRadio.newDatagram(speedRadio.getMaximumLength());

            while (appRun) {
                xdg.reset();
                led(5, LEDColor.TURQUOISE);
                led(5, true);
                try {
                    speedRadio.receive(xdg);
                    led(5, false);
                    speed = xdg.readInt();
                    respond = speed;
                    if (speed == Speeds.FAST) {
                        led(4, LEDColor.GREEN);
                    } else {
                        led(4, LEDColor.RED);
                    }
                    led(4, true);
                } catch (Exception e) {
                };
                pause(250);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    public void getDirectionValue() {
        try {
            Radiogram xdg = (Radiogram) directionRadio.newDatagram(directionRadio.getMaximumLength());
            
            while (appRun){
                led(6, LEDColor.RED);
                xdg.reset();
                led(6, true);
                try {
                    directionRadio.receive(xdg);
                    led(6, false);
                    direction = xdg.readInt();
                    respond = direction;
                } catch (Exception e) {
                };
                pause(250);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }                
    }
    
    public void sendResponse() {
        try {
            Radiogram xdg = (Radiogram) responseRadio.newDatagram(responseRadio.getMaximumLength());
            while (appRun) {
                xdg.reset();
                xdg.writeInt(respond);
                responseRadio.send(xdg);
                pause(500);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void openRadios() {
        if (directionRadio == null) {
            try {
                directionRadio = (RadiogramConnection) Connector.open("radiogram://:" + DIRECTION_PORT);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        if (speedRadio == null) {
            try {
                speedRadio = (RadiogramConnection) Connector.open("radiogram://:" + SPEED_PORT);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        if (responseRadio == null) {
            try {
                responseRadio = (RadiogramConnection) Connector.open("radiogram://broadcast:" + RESPONSE_PORT);
            } catch (IOException ex) {
                ex.printStackTrace();
            }            
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
    
    private void led(int num, boolean on) {
        if (on) {
            leds.getLED(num).setOn(true);
        } else {
            leds.getLED(num).setOff();
        }
    }
    
    private void led(int num, LEDColor color) {
        leds.getLED(num).setColor(color);
    }

    private void initialize() {
        myAddr = RadioFactory.getRadioPolicyManager().getIEEEAddress();
        //rpm.setChannelNumber(CHANNEL_NUMBER);
        //rpm.setPanId(PAN_ID);
        //rpm.setOutputPower(power - 32);
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

    private void print(String message) {
        System.out.println(message);
    }
}
