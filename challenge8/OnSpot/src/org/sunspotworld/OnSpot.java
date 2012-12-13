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
public class OnSpot extends MIDlet {

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
    private RadiogramConnection speedRadio;
    private RadiogramConnection responseRadio;
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
    private int respond;
    private int fb;
    private boolean somethingInFront;

    protected void startApp() throws MIDletStateChangeException {
        led(0, LEDColor.YELLOW);
        led(0, true);
        while (sw1.isOpen()) {
            stop();
            pause(250);
        }
        speed = Speeds.SLOW;
        fb = Directions.STOP;
        direction = Directions.CENTER;
        respond = 0;
        appRun = true;
        somethingInFront = false;
        initialize();
        led(0, LEDColor.WHITE);
        led(0, true);
        openRadios();
        startRadioThreads();
        startCarControlThreads();

        new Thread() {
            public void run() {
                sendResponse();
            }
        }.start();
        
        new Thread() {
            public void run() {
                checkFront();
            }
        }.start();

        while (sw1.isOpen()) {
            pause(2500);
        }
    }

    public void startCarControlThreads() {
        new Thread() {
            public void run() {
                fbController();
            }
        }.start();
        new Thread() {
            public void run() {
                lrController();
            }
        }.start();
    }

    public void fbController() {
        while (appRun) {
            switch (fb) {
                case Directions.FORWARD:
                    led(1, LEDColor.GREEN);
                    led(2, LEDColor.GREEN);
                    if (somethingInFront) stop();
                    else goForward();
                    break;
                case Directions.BACKWARD:
                    led(1, LEDColor.RED);
                    led(2, LEDColor.RED);
                    goBackward();
                    break;
                case Directions.STOP:
                    led(1, LEDColor.BLUE);
                    led(2, LEDColor.BLUE);
                    stop();
                    break;
                default:
            }
            led(1, true);
            led(2, true);
            pause(250);
        }
    }
    
    public void checkFront() {
        while(appRun) {
            if (getFrontValue() < 40) {
                led(7, LEDColor.RED);
                somethingInFront = true;
            } else {
                led(7, LEDColor.GREEN);
                somethingInFront = false;
            }
            led(7, true);
            pause(500);
        }
    }
    
    public int getFrontValue() {
        double value = 0;
        for (int ii = 0; ii < 5; ii++) {
            try {
                value += (frontSensor.getVoltage() / .009766 * 2.4);
            } catch (IOException ex) {
                ex.printStackTrace();
                return -1;
            }
        }

        return (int) (value / 5);
    }
    

    public void lrController() {
        while (appRun) {
            switch (direction) {
                case Directions.LEFT:
                    led(3, LEDColor.RED);
                    led(4, LEDColor.RED);
                    turnLeft();
                    break;
                case Directions.RIGHT:
                    led(3, LEDColor.GREEN);
                    led(4, LEDColor.GREEN);
                    turnRight();
                    break;
                case Directions.CENTER:
                    led(3, LEDColor.BLUE);
                    led(4, LEDColor.BLUE);
                    goStraight();
                    break;
                default:
            }
            led(3, true);
            led(4, true);
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
                try {
                    speedRadio.receive(xdg);
                    speed = xdg.readInt();
                    respond = speed;
                    if (speed == Speeds.FAST) {
                        stepValue = FASTER;
                        led(6, LEDColor.GREEN);
                    } else {
                        stepValue = SLOWER;
                        led(6, LEDColor.RED);
                    }
                    led(6, true);
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

            while (appRun) {
                xdg.reset();
                try {
                    directionRadio.receive(xdg);
                    int dir = xdg.readInt();
                    switch (dir) {
                        case Directions.LEFT:
                            direction = Directions.LEFT;
                            break;
                        case Directions.RIGHT:
                            direction = Directions.RIGHT;
                            break;
                        case Directions.CENTER:
                            direction = Directions.CENTER;
                            break;
                        case Directions.FORWARD:
                            fb = Directions.FORWARD;
                            break;
                        case Directions.BACKWARD:
                            fb = Directions.BACKWARD;
                            break;
                        case Directions.STOP:
                            fb = Directions.STOP;
                            break;
                        default:
                    }
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

    private void stop() {
        frontBackServo.setValue(SERVO_CENTER_VALUE);
        try {
            Thread.sleep(200);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    private void goForward() {
        //System.out.println("Forward");
        frontBackServo.setValue(SERVO_CENTER_VALUE - stepValue);
    }

    private void goBackward() {
        //System.out.println("Backward");
        frontBackServo.setValue(SERVO_CENTER_VALUE + stepValue);
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
}
