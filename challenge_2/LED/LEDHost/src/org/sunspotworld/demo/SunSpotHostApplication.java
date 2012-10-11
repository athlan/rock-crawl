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
    public void run() throws Exception {
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
             throw e;
        }
                
        
                while(true){
                    BufferedReader reader = new BufferedReader(new FileReader("/Library/Tomcat/webapps/remote/commands.txt"));
                    String num = reader.readLine();
                    String color = reader.readLine();
                    reader.close();
                    new BufferedWriter(new FileWriter("/Library/Tomcat/webapps/remote/commands.txt")).close();

                    try {
                        
                        System.out.println("Read: " + num);
                        int number = Integer.parseInt(num);
                        
                        
                        


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
                    dg.reset();
                    rCon.receive(dg);
                    System.out.println(dg.readInt());
                }
    }    

    
    
    /**
     * Start up the host application.
     *
     * @param args any command line arguments
     */
    public static void main(String[] args) throws Exception {
        SunSpotHostApplication app = new SunSpotHostApplication();
        app.run();
        System.exit(0);
    }

}
