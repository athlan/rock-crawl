/*
 * SunSpotApplication.java
 *
 * Created on Nov 15, 2012 12:32:28 PM;
 */
package org.sunspotworld;

import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.resources.Resources;
import com.sun.spot.resources.transducers.IAnalogInput;
import com.sun.spot.resources.transducers.ISwitch;
import com.sun.spot.resources.transducers.ITriColorLED;
import com.sun.spot.resources.transducers.ITriColorLEDArray;
import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.sensorboard.peripheral.Servo;
import com.sun.spot.service.BootloaderListenerService;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Utils;
import java.io.IOException;

import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

/**
 * The startApp method of this class is called by the VM to start the
 * application.
 *
 * The manifest specifies this class as MIDlet-1, which means it will be
 * selected for execution.
 */
public class CourseCar extends MIDlet {

    private static final int SERVO_CENTER_VALUE = 1500;
    private static final int FB_SERVO_HIGH = 200;
    private static final int LR_SERVO_HIGH = 200;
    private EDemoBoard eDemo = EDemoBoard.getInstance();
    private IAnalogInput rightSensor = eDemo.getAnalogInputs()[EDemoBoard.A0];
    private IAnalogInput leftSensor = eDemo.getAnalogInputs()[EDemoBoard.A1];
    private IAnalogInput frontSensor = eDemo.getAnalogInputs()[EDemoBoard.A2];
    private int baselineRight = 0;
    private int baselineLeft = 0;
    private ISwitch sw1 = (ISwitch) Resources.lookup(ISwitch.class, "SW1");
    private Servo frontBackServo = new Servo(eDemo.getOutputPins()[EDemoBoard.H1]);
    private Servo leftRightServo = new Servo(eDemo.getOutputPins()[EDemoBoard.H1]);
    private ITriColorLEDArray leds = (ITriColorLEDArray) Resources.lookup(ITriColorLEDArray.class);

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
        stop();
        recordBaseline();
        while (sw1.isOpen()) { //Start when you press the switch
            if (checkCorner()) {
                turnCorner();
            } else {
                if (getLeftValue() < baselineLeft - 5) {
                    turnRight();
                } else if (getRightValue() < baselineRight - 5) {
                    turnLeft();
                } else {
                    goStraight();
                }
                
                goForward();
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
        notifyDestroyed();                      // cause the MIDlet to exit        
    }
    
    protected void pauseApp() {
        // This is not currently called by the Squawk VM
    }

    private boolean checkCorner() {
        return false;
    }
        
    private void turnCorner() {
        
    }

    private void turnLeft() {
        leftRightServo.setValue(SERVO_CENTER_VALUE + LR_SERVO_HIGH);
    }

    private void turnRight() {
        leftRightServo.setValue(SERVO_CENTER_VALUE - LR_SERVO_HIGH);
    }

    private void goStraight() {
        leftRightServo.setValue(SERVO_CENTER_VALUE);
    }

    private void goForward() {
        frontBackServo.setValue(SERVO_CENTER_VALUE - FB_SERVO_HIGH);
        try {
            Thread.sleep(200);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    private void stop() {
        frontBackServo.setValue(SERVO_CENTER_VALUE);
        try {
            Thread.sleep(200);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    private int getLeftValue() {
        try {
            return (int) (leftSensor.getVoltage() / .0098);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return -1;
    }

    private int getRightValue() {
        try {
            return (int) (rightSensor.getVoltage() / .0098);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return -1;
    }

    private int getFrontValue() {
        return 50;
    }

    private void recordBaseline() {
        baselineLeft = getLeftValue();
        baselineRight = getRightValue();

        if (baselineLeft < 0) {
            baselineLeft = 0;
        }
        if (baselineRight < 0) {
            baselineRight = 0;
        }
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
}
