/*
 * Copyright (c) 2006-2010 Sun Microsystems, Inc.
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

import com.sun.spot.io.j2me.radiogram.RadiogramConnection;
import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Utils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import javax.microedition.io.*;


/**
 * Sample Sun SPOT host application
 */


public class SunSpotHostApplication {

    /**
     * Print out our radio address.
     */
       synchronized public void startSenderThread() {
        new Thread() {
            public void run() {
            long ourAddr =  RadioFactory.getRadioPolicyManager().getIEEEAddress();
            System.out.println("Our radio address = " + IEEEAddress.toDottedHex(ourAddr));
            RadiogramConnection rCon=null;
            Datagram dg=null;
            int ctl=0;
            try {
                // Open up a server-side broadcast radiogram connection
                // to listen for sensor readings being sent by different SPOTs
                rCon = (RadiogramConnection) Connector.open("radiogram://0014.4F01.0000.45B4:37" );
                dg = rCon.newDatagram(rCon.getMaximumLength());
            } catch (Exception e) {
                System.err.println("setUp caught " + e.getMessage());

            }
                while(true){
                    try{
                        BufferedReader reader = new BufferedReader(new FileReader("/Library/Tomcat/webapps/remote/commands.txt"));
                        String num = reader.readLine();
                        String color = reader.readLine();
                        reader.close();
                        new BufferedWriter(new FileWriter("/Library/Tomcat/webapps/remote/commands.txt")).close();

                            
                            int number = Integer.parseInt(num);
                            System.out.println("Read: " + num);

                            // We send the message (UTF encoded)
    //                        if(ctl==9)ctl=1;
                            if (number > 0 && number < 9){
                                dg.reset();
                                dg.writeInt(number);
                                if(color != null && color.length() > 0) {
                                    char c = color.toLowerCase().charAt(0);
                                    dg.writeChar(c);                                 

                                } else {                            
                                   dg.writeChar('w');
                                }
                                rCon.send(dg);
                            }


                        } catch (IOException ex) {
                            ex.printStackTrace();
                        } catch (NumberFormatException e) {

                        }
                        Utils.sleep(500);                   
                    }
            }   
        }.start();
       }

    
    
    public void startReceiverThread() {
        new Thread() {
            public void run() {
                int tmp;
                RadiogramConnection dgConnection = null;
                Datagram dg = null;
                
                try {
                    dgConnection = (RadiogramConnection) Connector.open("radiogram://:38");
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
                        tmp = dg.readInt();
                        BufferedWriter writer = new BufferedWriter(new FileWriter("/Library/Tomcat/webapps/remote/status.txt"));
                        writer.write(String.valueOf(tmp) + "\n");                                                
                        writer.close();
                        System.out.println("Received: " + tmp + " from " + dg.getAddress());
                    } catch (IOException e) {
                        System.out.println("Nothing received");
                    }
                }
            }
        }.start();
    }
    /**
     * Start up the host application.
     *
     * @param args any command line arguments
     */
    public static void main(String[] args) throws Exception {
        SunSpotHostApplication app = new SunSpotHostApplication();
        app.startReceiverThread();
        app.startSenderThread();
    }

}
