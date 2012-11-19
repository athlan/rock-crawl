package org.sunspotworld

import com.sun.spot.sensorboard.EDemoBoard;
//import com.sun.spot.sensorboard.IDemoBoard;
import com.sun.spot.resources.transducers.IAnalogInput;

import com.sun.spot.resources.Resources;
import com.sun.spot.util.Utils;

import java.io.IOException;

import javax.microedition.midlet.MIDlet;
import com.sun.spot.resources.transducers.ILightSensor;
import com.sun.spot.resources.transducers.ITriColorLED;
import com.sun.spot.resources.transducers.ITriColorLEDArray;

import com.sun.spot.sensorboard.peripheral.Servo;


//import java.io.IOException;
import javax.microedition.io.Connector;
//import javax.microedition.midlet.MIDletStateChangeException;


public class Turing {

    private static final int LIGHT_STEP = 200;
    private int turningPeriod=500;
    
    private EDemoBoard eDemo = EDemoBoard.getInstance();
    
    private ITriColorLEDArray turnLeds       = (ITriColorLEDArray) Resources.lookup(ITriColorLEDArray.class);
    private ILightSensor light = eDemo.getLightSensor();
        
    public int lightIndication = 0;         //ranges from 0 - 740
    public int environLight = 100;
    
    public boolean isCorner(){
        
        lightIndication = lightSensor.getValue();         //ranges from 0 - 740
        
        if(lightIndication>environLight+LIGHT_STEP)
        {
            
            turnLeds.getLED(5).setRGB(100,0,0);
            turnLeds.getLED(5).setOn();
            return true;
        }
        else
        {
            turnLeds.getLED(5).setOff();
            return false;
        }
    }
    public void getEnvironLight(){
        environLight=light.getAverageValue(25);
        
    }
    
    public void decideToTurn(Servo leftRightServo, Servo frontBackServo){
        
        if(isCorner()){            
            frontBackServo.setValue(1500);//stop
            Utils.sleep(1000);
            
            leftRightServo.setValue(2000);//turn left
            Utils.sleep(1000);
            frontBackServo.setValue(1700);//go
            Utils.sleep(turningPeriod);
        }
    }
}