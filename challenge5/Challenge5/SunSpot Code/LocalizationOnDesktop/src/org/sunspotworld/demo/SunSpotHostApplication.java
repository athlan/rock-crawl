	/*
 * ThermoOnDesktop.java
 *
 * Created on Sep 19, 2012 10:03:13 PM;
 */
package org.sunspotworld.demo;

import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.util.IEEEAddress;
import javax.microedition.io.Datagram;
import javax.microedition.io.DatagramConnection;
import com.sun.spot.io.j2me.radiostream.*;
import com.sun.spot.io.j2me.radiogram.*;

import java.io.*;
import javax.microedition.io.*;

import com.sun.spot.client.DummySpotClientUI;
import com.sun.spot.client.SerialPortWrapper;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SunSpotHostApplication {

    public static final String BROADCAST_PORT = "112";

    public void run() {
        long ourAddr = RadioFactory.getRadioPolicyManager().getIEEEAddress();
        System.out.println("Our radio address = " + IEEEAddress.toDottedHex(ourAddr));
    }

    public void startReceiverThread() {

        RadiogramConnection dgConnection = null;
        Datagram dg = null;
        double Distance;
        long CurrentBeacon;
        String CurrentBeaconHex;

        try {
            dgConnection = (RadiogramConnection) Connector.open("radiogram://:" + BROADCAST_PORT);
            dg = dgConnection.newDatagram(dgConnection.getMaximumLength());
        } catch (IOException e) {
            System.out.println("Can not open radiogram receiver connection");
            e.printStackTrace();
            return;
        }

        System.out.println("Running...");
        while (true) {
            try {
                dg.reset();
                dgConnection.receive(dg);
              
                CurrentBeacon = dg.readLong();
                Distance = dg.readDouble();
                CurrentBeaconHex = IEEEAddress.toDottedHex(CurrentBeacon);
                
                System.out.println("Beacon = "+ CurrentBeaconHex  + ",Distance=" + Distance);
                
                try {
                    File file = new File("../../Matlab Code/SpotDistance.txt");
                    
                    BufferedWriter outb = new BufferedWriter(new FileWriter(file, true));
                    outb.write("[java] SunSPOT:" + CurrentBeaconHex + "," + Distance + ",");
                    outb.newLine();
                    outb.close();
                } catch (IOException e) {
                    System.out.println("Writel File Error1");
                }


            } catch (IOException e) {
                System.out.println("No datagram received");
            }
        }
    }

    /**
     * Start up the host application.
     *
     * @param args any command line arguments
     */
    public static void main(String[] args) {
        SunSpotHostApplication app = new SunSpotHostApplication();
        app.run();
        app.startReceiverThread();
        System.exit(0);
    }
}
