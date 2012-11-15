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
 **/

package org.sunspotworld;

import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.sensorboard.IDemoBoard;
import com.sun.spot.sensorboard.peripheral.Servo;
import com.sun.spot.resources.Resources;
import com.sun.spot.resources.transducers.ISwitch;
import com.sun.spot.resources.transducers.ISwitchListener;
import com.sun.spot.resources.transducers.SwitchEvent;
import com.sun.spot.resources.transducers.ITriColorLED;
import com.sun.spot.resources.transducers.ITriColorLEDArray;
import com.sun.spot.resources.transducers.LEDColor;
//import com.sun.spot.resources.transducers.IAccelerometer3D;
import com.sun.spot.io.j2me.radiogram.RadiogramConnection;
import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.resources.Resources;
import com.sun.spot.resources.transducers.IAnalogInput;

import com.sun.spot.util.IEEEAddress;

import com.sun.spot.service.BootloaderListenerService;
import com.sun.spot.util.Utils;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

import org.sunspotworld.common.Globals; //
import org.sunspotworld.common.TwoSidedArray; //
import org.sunspotworld.lib.BlinkenLights;
import org.sunspotworld.lib.LedUtils;
import org.sunspotworld.lib.RadioDataIOStream;


import java.io.IOException;

import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.io.DatagramConnection;

/**
 * This class is used to move a servo car consisting of two servos - one for
 * left wheel and the other for right wheel. To combine these servos properly,
 * this servo car moves forward/backward, turn right/left and rorate
 * clockwise/counterclockwise.
 * 
 * The current implementation has 3 modes and you can change these "moving mode" 
 * by pressing sw1. Mode 1 is "Normal" mode moving the car according to the tilt 
 * of the remote controller. Mode 2 is "Reverse" mode moving the car in 
 * a direction opposite to Mode 1. Mode 3 is "Rotation" mode only rotating the
 * car clockwise or counterclockwise according to the tilt.
 * 
 * @author Tsuyoshi Miyake <Tsuyoshi.Miyake@Sun.COM>
 * @author Yuting Zhang<ytzhang@bu.edu>
 */
public class ServoSPOTonCar extends MIDlet implements ISwitchListener {
    
    private static final int SERVO_CENTER_VALUE = 1500;
//    private static final int SERVO_MAX_VALUE = 2000;
//    private static final int SERVO_MIN_VALUE = 1000;
    private static final int SERVO1_MAX_VALUE = 2000;
    private static final int SERVO1_MIN_VALUE = 1000;
//    private static final int SERVO_HIGH = 500;
//    private static final int SERVO_LOW = 300;
    private static final int SERVO1_LOW = 100;
    private static final int SERVO2_HIGH = 100;
    private static final int SERVO2_LOW = 25;

    // devices
    private EDemoBoard eDemo = EDemoBoard.getInstance();
    private IAnalogInput irRight = eDemo.getAnalogInputs()[EDemoBoard.A0];
    private IAnalogInput irLeft = eDemo.getAnalogInputs()[EDemoBoard.A1];
    private IAnalogInput ultra = eDemo.getAnalogInputs()[EDemoBoard.A2];
   

    private ITriColorLED[] leds = eDemo.getLEDs();
    private ITriColorLEDArray myLEDs = (ITriColorLEDArray) Resources.lookup(ITriColorLEDArray.class);
    
    // 1st servo for left & right direction 
    private Servo servo1 = new Servo(eDemo.getOutputPins()[EDemoBoard.H1]);
    // 2nd servo for forward & backward direction
    private Servo servo2 = new Servo(eDemo.getOutputPins()[EDemoBoard.H0]);
    private BlinkenLights progBlinker = new BlinkenLights(1, 3);
    private BlinkenLights velocityBlinker = new BlinkenLights(4, 6);
    private int current1 = SERVO_CENTER_VALUE;
    private int current2 = SERVO_CENTER_VALUE;
    private int step1 = SERVO1_LOW;
    private int step2 = SERVO2_LOW;
    private int lastTurnDirection = 0;
    private boolean backUp = false;
    private int backUpCounter = 0;
    
    
//    private int servo1ForwardValue;
    
    public ServoSPOTonCar() {
    }
    
    
    /** BASIC STARTUP CODE **/
    
    protected void startApp() throws MIDletStateChangeException {
        System.out.println("Hello, world");
        BootloaderListenerService.getInstance().start();  

        
        for (int i = 0; i < myLEDs.size(); i++) {
                        myLEDs.getLED(i).setColor(LEDColor.GREEN);
                        myLEDs.getLED(i).setOn();
                    }
        Utils.sleep(500);
        setServoForwardValue();
        progBlinker.startPsilon();
        velocityBlinker.startPsilon();
        // timeout 1000
        TwoSidedArray robot = new TwoSidedArray(getAppProperty("buddyAddress"), Globals.READ_TIMEOUT);
        try {
            robot.startInput();
        } catch (Exception e) {
            e.printStackTrace();
        }


        velocityBlinker.setColor(LEDColor.BLUE);
        progBlinker.setColor(LEDColor.BLUE);

        boolean error = false;
        while (true) {
            boolean timeoutError = robot.isTimeoutError();
//            int xtilt = 0;
//            int ytilt = 0;
            int go = 0;

            

            if (!timeoutError) {
                //xtilt = robot.getVal(0);
                //ytilt = robot.getVal(1);
                go = robot.getVal(0);
                if (error) {
                    step1 = SERVO1_LOW;
                    step2 = SERVO2_LOW;
                    velocityBlinker.setColor(LEDColor.BLUE);
                    progBlinker.setColor(LEDColor.BLUE);
                    error = false;
                }
                if (go == 0) {
                    backUp = false;
                    backUpCounter = 0;
                }
                try {
                    if (!backUp && ultra.getVoltage() * 1000/9.7 < 10) {
                        backUp = true;
                    }
                } catch (IOException ex) {
                    backUp = false;
                }
                
            if (!backUp){
                try {
                    checkDirection();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            if (backUp) {
                backUpCounter++;
                if (backUpCounter < 50) {
                    straight();
                    backward();
                } else if (backUpCounter < 70) {
                   switch (lastTurnDirection) {
                        case 1: right(); break;
                        case -1: left(); break;
                   }
                   forward();
                } else {
                    switch (lastTurnDirection) {
                        case 1: lastTurnDirection= -1; break;
                        case -1: lastTurnDirection = 1; break;
                   }
                   backUp = false;
                   backUpCounter = 0;
                }
            }else if (go==1) {
                velocityBlinker.setColor(LEDColor.GREEN);
                progBlinker.setColor(LEDColor.GREEN);
                forward();
            }
            else if (go == -1) {
                velocityBlinker.setColor(LEDColor.GREEN);
                progBlinker.setColor(LEDColor.GREEN);
                backward();
            }
            else {   
                velocityBlinker.setColor(LEDColor.WHITE);
                progBlinker.setColor(LEDColor.WHITE);
                stop();
            }
            
            } else {
                velocityBlinker.setColor(LEDColor.RED);
                progBlinker.setColor(LEDColor.RED);
                error = true;
            }
            Utils.sleep(20);
        }
    }
        
    private void setServoForwardValue(){
     
        if (step2 == SERVO2_HIGH) {
            velocityBlinker.setColor(LEDColor.GREEN);
        } else if (step2 == SERVO2_LOW) {
            velocityBlinker.setColor(LEDColor.BLUE);
        }
    }
    
    private void checkDirection() throws IOException {
        if (getDistance(irLeft) < 25 && getDistance(irRight) < 25){
            myLEDs.getLED(0).setColor(LEDColor.RED);
            myLEDs.getLED(7).setColor(LEDColor.RED);
            straight(); 
        }
        else if (getDistance(irLeft) < 25) {
            lastTurnDirection = -1;
        //if (getDistance(irLeft) + 12 < getDistance(irRight) ) {
            myLEDs.getLED(0).setColor(LEDColor.RED);
            myLEDs.getLED(7).setColor(LEDColor.BLUE);
            right();
        } else if (getDistance(irRight) < 25) {
            lastTurnDirection = 1;
        //}else if (getDistance(irRight) + 12 <  getDistance(irLeft)){
            myLEDs.getLED(0).setColor(LEDColor.BLUE);
            myLEDs.getLED(7).setColor(LEDColor.RED);
            left();
        } else {
            myLEDs.getLED(0).setColor(LEDColor.BLUE);
            myLEDs.getLED(7).setColor(LEDColor.BLUE);
            straight();
        }
        
    }

    private double getDistance(IAnalogInput analog) throws IOException {
        double volts = analog.getVoltage();
        return 18.67/(volts+0.167);
    }

    private void straight() {
        servo1.setValue(SERVO_CENTER_VALUE);
        Utils.sleep(20);
    }

    private void left() {
        System.out.println("left");
        current1 = servo1.getValue();
        if (current1 + step1 < SERVO1_MAX_VALUE){
        servo1.setValue(current1+step1);
        Utils.sleep(20);
        } else{
        servo1.setValue(SERVO1_MAX_VALUE);
        Utils.sleep(20);
        }
        
 //       servo2.setValue(0);
    }

    private void right() {
        System.out.println("right");
        current1 = servo1.getValue();
        if (current1-step1 > SERVO1_MIN_VALUE){
        servo1.setValue(current1-step1);
        Utils.sleep(20);
        } else{
            servo1.setValue(SERVO1_MIN_VALUE);
            Utils.sleep(20);
        }
        
 //       servo2.setValue(0);
    }

    private void stop() {
//        System.out.println("stop");
   //     servo1.setValue(0);
        servo2.setValue(SERVO_CENTER_VALUE);
    }

    private void backward() {
        
        
        servo2.setValue(SERVO_CENTER_VALUE + 200);
            
  /*     while(current2 + step2 <SERVO2_MAX_VALUE){
        
         servo2.setValue(current2+step2);
         current2= servo2.getValue();
        Utils.sleep(50);
         
}*/
    }

    private void forward() {
        System.out.println("backward");              
        servo2.setValue(SERVO_CENTER_VALUE - 200);        
        
    }


    public void switchPressed(SwitchEvent sw) {
       
    }

    public void switchReleased(SwitchEvent sw) {
    // do nothing
    }
    
    protected void pauseApp() {
        // This will never be called by the Squawk VM
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
        for (int i = 0; i < myLEDs.size(); i++) {
            myLEDs.getLED(i).setOff();
        }
    }

}

