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
import com.sun.spot.resources.transducers.IAnalogInput;
import com.sun.spot.resources.transducers.ISwitch;
import com.sun.spot.resources.transducers.ITriColorLED;
import com.sun.spot.resources.transducers.ITriColorLEDArray;
import com.sun.spot.resources.transducers.LEDColor;
import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.sensorboard.peripheral.Servo;
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
public class Repeater extends MIDlet {

    private static final int SERVO_CENTER_VALUE = 1500;
    private static final int LR_SERVO_HIGH = 500;

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
   
    private ITriColorLEDArray leds = (ITriColorLEDArray) Resources.lookup(ITriColorLEDArray.class);
    private ISwitch sw1 = (ISwitch) Resources.lookup(ISwitch.class, "SW1");
    private EDemoBoard eDemo = EDemoBoard.getInstance();
    private Servo frontBackServo = new Servo(eDemo.getOutputPins()[EDemoBoard.H0]);
    private Servo leftRightServo = new Servo(eDemo.getOutputPins()[EDemoBoard.H1]);
    private IAnalogInput frontSensor = eDemo.getAnalogInputs()[EDemoBoard.A0];

    private static final int FASTER = 300;
    private static final int SLOWER = 150;
    private int stepValue = SLOWER;
    private boolean appRun;
    private int speed;
    private int direction;
    private boolean respondSpeed;
    private boolean respondDirection;
    private int fb;
    private boolean somethingInFront;
    
    
    private RadiogramConnection directionTransRadio;
    private RadiogramConnection speedTransRadio;
     private RadiogramConnection speedRecRadio;
    private RadiogramConnection directionRecRadio;

    protected void startApp() throws MIDletStateChangeException {
        led(0, LEDColor.WHITE);
        led(0, true);

        speed = Speeds.SLOW;
        direction = Directions.CENTER;
        respondSpeed = respondDirection =false;
        appRun = true;
        initialize();
        led(0, LEDColor.WHITE);
        led(0, true);
        openRadios();
        startRadioThreads();

//        new Thread() {
//            public void run() {
//                sendResponse();
//            }
//        }.start();
        
       

        while (appRun) {
            pause(2500);
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
        new Thread() {
            public void run() {
                sendDirectionValue();
            }
        }.start();
        new Thread() {
            public void run() {
                sendSpeedValue();
            }
        }.start();
    }

    public void getSpeedValue() {
        try {
            Radiogram xdg = (Radiogram) speedRecRadio.newDatagram(speedRecRadio.getMaximumLength());

            while (appRun) {
                xdg.reset();
                try {
                    speedRecRadio.receive(xdg);
                    speed = xdg.readInt();
                    respondSpeed = true;                    
                    led(6, true);
                    led(6, LEDColor.YELLOW);
                } catch (Exception e) {
                };
                pause(250);
                led(6, false);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    public void sendSpeedValue() {
        try {
            Radiogram xdg = (Radiogram) speedTransRadio.newDatagram(speedTransRadio.getMaximumLength());

            while (appRun) {
                
                if(respondSpeed) {
                    respondSpeed = false;
                    led(1, true);
                    led(1, LEDColor.BLUE);
                    xdg.reset();
                    xdg.writeInt(speed);
                    speedTransRadio.send(xdg);
                }
                pause(250);
                led(1, false);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void getDirectionValue() {
        try {
            Radiogram xdg = (Radiogram) directionRecRadio.newDatagram(directionRecRadio.getMaximumLength());

            while (appRun) {
                xdg.reset();
                try {
                    directionRecRadio.receive(xdg);
                    direction = xdg.readInt();
                    
                    led(3, LEDColor.RED);
                    led(3, true);
                    respondDirection = true;
                } catch (Exception e) {
                };
                pause(250);
                led(3, false);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    public void sendDirectionValue() {
        try {
            Radiogram xdg = (Radiogram) directionTransRadio.newDatagram(directionTransRadio.getMaximumLength());

            while (appRun) {
                if (respondDirection) {
                    respondDirection = false;
                    led(2, true);
                    led(2, LEDColor.GREEN);
                    xdg.reset();
                    xdg.writeInt(direction);
                    directionTransRadio.send(xdg);
                }                                                                                              
                pause(250);
                led(2, false);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    

    public void openRadios() {
        if (directionRecRadio == null) {
            try {
                directionRecRadio = (RadiogramConnection) Connector.open("radiogram://:" + DIRECTION_PORT);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        if (speedRecRadio == null) {
            try {
                speedRecRadio = (RadiogramConnection) Connector.open("radiogram://:" + SPEED_PORT);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        if (speedTransRadio == null) {
            try {
                speedTransRadio = (RadiogramConnection) Connector.open("radiogram://broadcast:" + SPEED_PORT);
                speedTransRadio.setMaxBroadcastHops(1);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        if (directionTransRadio == null) {
            try {
                directionTransRadio = (RadiogramConnection) Connector.open("radiogram://broadcast:" + DIRECTION_PORT);
                directionTransRadio.setMaxBroadcastHops(1);
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
