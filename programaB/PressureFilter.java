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
import java.text.DecimalFormat;
import java.nio.ByteBuffer;
import java.util.*;

public class PressureFilter extends FilterFramework {
    Queue<Double> extremosIdentificados = new LinkedList<Double>();
    Double ultimoValorValido = 0d;               // Variable used for save the last measurement of pressure
    Double siguienteValorValido = 0d; //variable used for calculate the valid pressure, this value will be (last value + current value) / 2
    
    public void run() {

        int MeasurementLength = 8;      // This is the length of all measurements (including time) in bytes
        int IdLength = 4;               // This is the length of IDs in the byte stream

        byte databyte = 0;              // This is the data byte read from the stream
        int bytesread = 0;              // This is the number of bytes read from the stream

        long measurement;               // This is the word used to store all measurements - conversions are illustrated.
        int id;                         // This is the measurement id
        int i;                  // The byte of data read from the file
        
        boolean firstTime = true;
        

        // Next we write a message to the terminal to let the world know we are alive...
        System.out.print("\n" + this.getName() + "::Pressure Reading ");

        while (true) {
            /**
             * ***********************************************************
             * Here we read a byte and write a byte
            ************************************************************
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

                        bytesread++;// Increment the byte count
                        if(id != 3){
                            WriteFilterOutputPort(databyte);
                        }
                    } // if
                    //Codigo para trabajar con la presion
                    if (id == 3) {
                        DecimalFormat df = new DecimalFormat("###.######"); //Variable que dará formato de la presión
                        double p = Double.longBitsToDouble(measurement); //Extraccion del valor de la presion
                        p = Double.parseDouble(df.format(p)); //Formateo de la presion
                        
                        //PRIMERA VEZ
                        if(firstTime){
                            
                            //El valor debe ser valido
                            if(isValid(p)){

                                //No existen datos pendientes por enviar
                                if(extremosIdentificados.isEmpty()){
                                    
                                    //Enviamos los datos
                                    //byte [] bytes = ByteBuffer.allocate(8).putDouble(p).array();
                                    //for(int j= 0; j < bytes.length; j++){
                                    //    WriteFilterOutputPort(bytes[j]);
                                    //} //for
                                    System.out.println("Envio de datos: " + p);
                                    ultimoValorValido = p;    
                                    firstTime = false;

                                //Existen datos pendientes por enviar
                                }else{
                                    ultimoValorValido = p;
                                    siguienteValorValido = p;
                                    sendStragglers();
                                    //Enviar ademas el valor valido actual
                                    System.out.println("Envio de datos: " + p);
                                    firstTime = false;
                                }
                                

                            //El valor no es valido
                            }else{

                                //Lo dejamos como pendiente a ser enviado
                                extremosIdentificados.add(p);
                            }

                        //NO ES PRIMERA VEZ
                        }else{
                            
                            //El valor debe ser valido
                            if(isValid(p)){
                                
                                siguienteValorValido = p; //Modificamos el ultimo valor valido de la presion

                                //No existen datos pendientes por enviar
                                if(extremosIdentificados.isEmpty()){
                                    //Enviar el valor valido actual
                                    System.out.println("Envio de datos: " + p);
                                    ultimoValorValido = p; //Modificamos el ultimo valor válido de la presion
                                
                                //Existen datos pendientes por enviar
                                }else{
                                    sendStragglers();
                                    //Enviar ademas el valor valido actual
                                    System.out.println("Envio de datos: " + p);
                                    ultimoValorValido = p;
                                }

                            //El valor no es válido
                            }else{

                                //Lo dejamos pendiente por enviar
                                extremosIdentificados.add(p);
                            }
                        }
                    } //if
                } // try
            catch (EndOfStreamException e) {

                //ENVIAR LO QUE QUEDO


                //Rescatar el ultimo valor valido para calcular el reemplazo
                siguienteValorValido = ultimoValorValido;
                //Enviar los que quedaron
                sendStragglers();
                ClosePorts();
                System.out.print("\n" + this.getName() + "::Pressure Exiting; bytes read: " + bytesread);
                break;
            } // catch
        } // while

    } // run

    private boolean isValid(Double measurement){
        Double extremeMax = 80d;      //Variable used for save de max value of the measurement of pressure
        Double extremeMin = 50d;      //Variable used for save de min value of the measurement of pressure
        return (measurement > extremeMin && measurement < extremeMax);
    }

    private void sendStragglers(){
            Double reemplazo = (ultimoValorValido + siguienteValorValido) / 2;
            while (!extremosIdentificados.isEmpty()) {
                //Aqui se enviarán los datos, tanto originales como los de reemplazo
                System.out.println("Envio de datos: " + reemplazo + " original: " + extremosIdentificados.remove() + "*");
            }
            //Modificamos el ultimo valor válido
            ultimoValorValido = siguienteValorValido;
    }

} // MiddleFilter