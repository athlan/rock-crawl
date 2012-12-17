/*
 * SunSpotApplication.java
 *
 * Created on Nov 15, 2012 12:32:28 PM;
 */
package org.sunspotworld;

import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.resources.Resources;
import com.sun.spot.resources.transducers.IAnalogInput;
import com.sun.spot.resources.transducers.ILightSensor;
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
    private static final int LR_SERVO_HIGH = 500;
    private static final int LIGHT_STEP = 200;
    private int turningPeriod = 3000;
    private int lightIndication = 0;         //ranges from 0 - 740
    private int environLight = 100;
    private int baselineRight = 0;
    private int baselineLeft = 0;
    private EDemoBoard eDemo = EDemoBoard.getInstance();
    private IAnalogInput rightSensor = eDemo.getAnalogInputs()[EDemoBoard.A0];
    private IAnalogInput leftSensor = eDemo.getAnalogInputs()[EDemoBoard.A1];
    private IAnalogInput frontSensor = eDemo.getAnalogInputs()[EDemoBoard.A2];
    private ISwitch sw1 = (ISwitch) Resources.lookup(ISwitch.class, "SW1");
    private ISwitch sw2 = (ISwitch) Resources.lookup(ISwitch.class, "SW2");
    private Servo frontBackServo = new Servo(eDemo.getOutputPins()[EDemoBoard.H0]);
    private Servo leftRightServo = new Servo(eDemo.getOutputPins()[EDemoBoard.H1]);
    private ITriColorLEDArray leds = (ITriColorLEDArray) Resources.lookup(ITriColorLEDArray.class);
    private ILightSensor light = eDemo.getLightSensor();

    protected void startApp() throws MIDletStateChangeException {
        System.out.println("Hello, world");
        BootloaderListenerService.getInstance().start();   // monitor the USB (if connected) and recognize commands from host

        long ourAddr = RadioFactory.getRadioPolicyManager().getIEEEAddress();
        System.out.println("Our radio address = " + IEEEAddress.toDottedHex(ourAddr));

        ITriColorLED led = leds.getLED(0);
        while (true) {
            led.setRGB(100, 0, 0);                    // set color to moderate red
            while (sw1.isOpen()) {
                blinkLed(led);
                stop();// wait 1 second
                goStraight();
                System.out.println("Ready");
            }
            recordBaseline();
            led.setRGB(0, 0, 100);
            led.setOn();
            while (sw2.isOpen()) { //Start when you press the switch

                if (checkCorner()) {
                    turnCorner();
                } else if (getFrontValue() < 17) {
                    backUp();
                } else {
                    System.out.println(baselineRight + " " + getRightValue());
                    if (getLeftValue() < baselineLeft - 10) {
                        turnRight();
                    } else if (getRightValue() < baselineRight - 10) {
                        turnLeft();
                    } else {
                        goStraight();
                    }

                }
                System.out.println("Rollin Out");
                goForward();
                blinkLed(led);
            }
        }
    }

    protected void pauseApp() {
        // This is not currently called by the Squawk VM
    }

    private void backUp() {
        System.out.println("B-b-b-back it up");
        stop();
        Utils.sleep(250);
        System.out.println("Turn values" + getLeftValue() + " " + getRightValue());
        if(getLeftValue() < getRightValue() - 15) {
            turnLeft();
        } else if (getRightValue() < getLeftValue() -15) {
            turnRight();
        } else {
            goStraight();
        }
        Utils.sleep(500);
        long time = System.currentTimeMillis() + turningPeriod/2;

        while (System.currentTimeMillis() < time) {
            goBackward();
            Utils.sleep(250);
        }
        stop();
        goStraight();
        Utils.sleep(250);
    }

    private boolean checkCorner() {
        try {
            lightIndication = light.getValue();         //ranges from 0 - 740
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        if (lightIndication > environLight + LIGHT_STEP) {

            leds.getLED(5).setRGB(100, 0, 0);
            leds.getLED(5).setOn();
            return true;
        } else {
            leds.getLED(5).setOff();
            return false;
        }
    }

    private void turnCorner() {
        System.out.println("Ermahgerd Light! Turnin nowh");
        stop();
        Utils.sleep(500);
        turnLeft();
        Utils.sleep(1000);

        long time = System.currentTimeMillis() + turningPeriod;

        while (System.currentTimeMillis() < time) {
            goForward();
            Utils.sleep(250);
        }
        goStraight();
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
        //System.out.println("Forward");
        frontBackServo.setValue(SERVO_CENTER_VALUE - FB_SERVO_HIGH);
    }

    private void goBackward() {
        //System.out.println("Backward");
        frontBackServo.setValue(SERVO_CENTER_VALUE + FB_SERVO_HIGH);
    }

    private void stop() {
        frontBackServo.setValue(SERVO_CENTER_VALUE);
        try {
            Thread.sleep(200);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    private int getFrontValue() {
        double value = 0;
        for (int ii = 0; ii < 5; ii++) {
            try {
                double volts = frontSensor.getVoltage();
                value += (18.67 / (volts + 0.167));
            } catch (IOException ex) {
                ex.printStackTrace();
                return 10000;
            }
        }
        return (int) (value/5);
        
    }

    private int getLeftValue() {

        double value = 0;
        for (int ii = 0; ii < 5; ii++) {
            try {
                value += (leftSensor.getVoltage() / .009766 * 2.4);
            } catch (IOException ex) {
                ex.printStackTrace();
                return -1;
            }
        }

        return (int) (value / 5);
    }

    private int getRightValue() {
        double value = 0;
        for (int ii = 0; ii < 5; ii++) {
            try {
                value += (rightSensor.getVoltage() / .009766 * 2.4);
            } catch (IOException ex) {
                ex.printStackTrace();
                return -1;
            }
        }

        return (int) (value / 5);
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

    public void getEnvironLight() throws IOException {
        environLight = light.getAverageValue(25);

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

    private void blinkLed(ITriColorLED led) {
        led.setOn();
        Utils.sleep(250);
        led.setOff();
        Utils.sleep(250);
    }
}
