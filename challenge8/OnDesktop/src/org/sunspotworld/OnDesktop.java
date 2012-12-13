/*
 * SunSpotHostApplication.java
 *
 * Created on Dec 12, 2012 9:19:09 PM;
 */
package org.sunspotworld;

import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.peripheral.radio.IRadioPolicyManager;
import com.sun.spot.io.j2me.radiostream.*;
import com.sun.spot.io.j2me.radiogram.*;
import com.sun.spot.peripheral.Spot;
import com.sun.spot.util.IEEEAddress;

import java.io.*;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.microedition.io.*;

/**
 * Sample Sun SPOT host application
 */
public class OnDesktop {

    private void clearFiles() {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(speedFileName));
            writer.write("\n"); 
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(OnDesktop.class.getName()).log(Level.SEVERE, null, ex);
        } 
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(directionFileName));
            writer.write("\n"); 
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(OnDesktop.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static class Speeds {

        private static final int SLOW = 1;
        private static final int FAST = 2;

        public static int valueOf(String val) {
            if ("SLOW".equalsIgnoreCase(val)) {
                return SLOW;
            } else if ("Fast".equalsIgnoreCase(val)) {
                return FAST;
            }
            return 0;
        }
    }

    private static class Directions {

        private static final int FORWARD = 5;
        private static final int RIGHT = 6;
        private static final int LEFT = 7;
        private static final int BACKWARD = 8;
        private static final int STOP = 9;
        private static final int CENTER = 10;

        public static int valueOf(String val) {
            if ("Forward".equalsIgnoreCase(val)) {
                return FORWARD;
            } else if ("Right".equalsIgnoreCase(val)) {
                return RIGHT;
            } else if ("Left".equalsIgnoreCase(val)) {
                return LEFT;
            } else if ("Backward".equalsIgnoreCase(val)) {
                return BACKWARD;
            } else if ("Stop".equalsIgnoreCase(val)) {
                return STOP;
            } else if("Center".equalsIgnoreCase(val)) {
                return CENTER;
            }
            return -1;
        }
    }
    private static final String speedFileName = "/tmp/speed";
    private static final String directionFileName = "/tmp/command";
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

    private boolean sendSpeed;
    private boolean sendDirection;
    private boolean appRun;
    private int speed;
    private int direction;

    public void start() {
        speed = Speeds.SLOW;
        direction = Directions.STOP;
        appRun = true;
        sendSpeed = sendDirection = true;
        clearFiles(); 
        
        initialize();
        openRadios();
        startRadioThreads();
        startReaderThreads();
        
        new Thread() {
            public void run() {
                getResponse();
            }
        }.start();
        while (appRun) {
            pause(2500);
        }
    }

    public void startRadioThreads() {
        new Thread() {
            public void run() {
                sendSpeedValue();
            }
        }.start();
        new Thread() {
            public void run() {
                sendDirectionValue();
            }
        }.start();
    }
    
    public void startReaderThreads() {
        new Thread() {
            public void run() {
                readSpeedValue();
            }
        }.start();
        new Thread() {
            public void run() {
                readDirectionValue();
            }
        }.start();
    }

    public void sendSpeedValue() {
        try {
            Radiogram xdg = (Radiogram) speedRadio.newDatagram(speedRadio.getMaximumLength());

            while (appRun) {
                if (sendSpeed) {
                    sendSpeed = false;
                    print("Speed change.... Sending Speed Value of " + speed);
                    xdg.reset();
                    xdg.writeInt(speed);
                    speedRadio.send(xdg);
                }
                pause(250);
            }
        } catch (IOException ex) {
            Logger.getLogger(OnDesktop.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendDirectionValue() {
        try {
            Radiogram xdg = (Radiogram) directionRadio.newDatagram(directionRadio.getMaximumLength());

            while (appRun) {
                if (sendDirection) {
                    sendDirection = false;
                    print("Direction change.... Sending Direction value of " + direction);
                    xdg.reset();
                    xdg.writeInt(direction);
                    directionRadio.send(xdg);
                }
                pause(250);
            }
        } catch (IOException ex) {
            Logger.getLogger(OnDesktop.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void readSpeedValue() {
        File speedFile = new File(speedFileName);

        while (appRun) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(speedFile));
                String line = reader.readLine();
                reader.close();
                int speedVal = Speeds.valueOf(line);
                if (speed != speedVal) {
                    print("Reading a change of speed from " + speed + " to " + speedVal);
                    speed = speedVal;
                    sendSpeed = true;
                }

            } catch (IOException ex) {
                Logger.getLogger(OnDesktop.class.getName()).log(Level.SEVERE, null, ex);
            }
            pause(250);
        }
    }
    
    public void readDirectionValue() {
        File directionFile = new File(directionFileName);
        
        while(appRun) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(directionFile));
                String line = reader.readLine();
                reader.close();
                int directionVal = Directions.valueOf(line);
                if (direction != directionVal) {
                    print("Reading a change of direction from " + direction + " to " + directionVal);
                    direction = directionVal;
                    sendDirection = true;
                }                
                
            } catch (IOException ex) {
                Logger.getLogger(OnDesktop.class.getName()).log(Level.SEVERE, null, ex);
            } 
            pause(250);            
        }
    }
    
    public void getResponse() {
        try {        
            Radiogram xdg = (Radiogram) responseRadio.newDatagram(responseRadio.getMaximumLength());
            while (appRun) {
                xdg.reset();
                responseRadio.receive(xdg);
                print("Response is: " + xdg.readInt() + " Time: " + (new Date(System.currentTimeMillis())).toString());
                pause(500);
            }
        } catch (IOException ex) {
            Logger.getLogger(OnDesktop.class.getName()).log(Level.SEVERE, null, ex);
        }
            
        
    }

    public void openRadios() {
        if (directionRadio == null) {
            try {
                directionRadio = (RadiogramConnection) Connector.open("radiogram://broadcast:" + DIRECTION_PORT);
            } catch (IOException ex) {
                Logger.getLogger(OnDesktop.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (speedRadio == null) {
            try {
                speedRadio = (RadiogramConnection) Connector.open("radiogram://broadcast:" + SPEED_PORT);
            } catch (IOException ex) {
                Logger.getLogger(OnDesktop.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if(responseRadio == null) {
            try {
                responseRadio = (RadiogramConnection) Connector.open("radiogram://:" + RESPONSE_PORT);
            } catch (IOException ex) {
                Logger.getLogger(OnDesktop.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Print out our radio address.
     */
    public void run() {
        long ourAddr = RadioFactory.getRadioPolicyManager().getIEEEAddress();
        System.out.println("Our radio address = " + IEEEAddress.toDottedHex(ourAddr));
        System.exit(0);
    }

    /**
     * Start up the host application.
     *
     * @param args any command line arguments
     */
    public static void main(String[] args) {
        OnDesktop app = new OnDesktop();
        app.start();
    }

    private void initialize() {
        myAddr = RadioFactory.getRadioPolicyManager().getIEEEAddress();
       // rpm.setChannelNumber(CHANNEL_NUMBER);
      //  rpm.setPanId(PAN_ID);
       // rpm.setOutputPower(power - 32);
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
