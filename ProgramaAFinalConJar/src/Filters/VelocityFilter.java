package Filters;

import java.text.DecimalFormat;
import java.nio.ByteBuffer;

/**
 * ****************************************************************************************************************
 * File:MiddleFilter.java Course: 17655 Project: Assignment 1 Copyright:
 * Copyright (c) 2003 Carnegie Mellon University Versions: 2.0 October 2015 -
 * Sample Pipe and Filter code (pv).
 * 
* Description:
 * 
* This class serves as an example for how to use the FilterRemplate to create a
 * standard filter. This particular example is a simple "pass-through" filter
 * that reads data from the filter's input port and writes data out the filter's
 * output port.
 * 
* Parameters: None
 * 
* Internal Methods: None
 * 
*****************************************************************************************************************
 */
public class VelocityFilter extends FilterFramework {

    public void run() {

        int MeasurementLength = 8;      // This is the length of all measurements (including time) in bytes
        int IdLength = 4;               // This is the length of IDs in the byte stream

        byte databyte = 0;              // This is the data byte read from the stream
        int bytesread = 0;              // This is the number of bytes read from the stream

        long measurement;               // This is the word used to store all measurements - conversions are illustrated.
        int id;                         // This is the measurement id
        int i;                  // The byte of data read from the file

        // Next we write a message to the terminal to let the world know we are alive...
        System.out.print("\n" + this.getName() + "::Velocity Reading ");

        while (true) {
            /**
             * ***********************************************************
             * Here we read a byte and write a byte
             * ***********************************************************
             */

            try {
                id = 0;

                for (i = 0; i < IdLength; i++) {
                    databyte = ReadFilterInputPort();   // This is where we read the byte from the stream...

                    id = id | (databyte & 0xFF);        // We append the byte on to ID...

                    if (i != IdLength - 1) // If this is not the last byte, then slide the
                    {                                   // previously appended byte to the left by one byte
                        id = id << 8;                   // to make room for the next byte we append to the ID

                    } // if

                    bytesread++;                        // Increment the byte count
                    WriteFilterOutputPort(databyte);

                } // for
                measurement = 0;

                for (i = 0; i < MeasurementLength; i++) {
                    databyte = ReadFilterInputPort();
                    measurement = measurement | (databyte & 0xFF);  // We append the byte on to measurement...

                    if (i != MeasurementLength - 1) // If this is not the last byte, then slide the
                    {                                               // previously appended byte to the left by one byte
                        measurement = measurement << 8;             // to make room for the next byte we append to the
                        // measurement
                    } // if

                    bytesread++;                                    // Increment the byte count
                    if(id != 1){
                        WriteFilterOutputPort(databyte);
                    } //for
                } // for

                if (id == 1) {
                    DecimalFormat df = new DecimalFormat("######.#####");
                    double nudos = Double.longBitsToDouble(measurement);
                    double kmh = nudos * 1.852;
                    kmh = Double.parseDouble(df.format(kmh));
                    byte[] bytes = ByteBuffer.allocate(8).putDouble(kmh).array();
                    for(int j = 0; j<bytes.length; j++){//for
                        WriteFilterOutputPort(bytes[j]);
                    }//for
                }
            } // try
            catch (EndOfStreamException e) {
                ClosePorts();
                System.out.print("\n" + this.getName() + "::Velocity Exiting; bytes read: " + bytesread);
                break;

            } // catch

        } // while

    } // run

} // MiddleFilter
