package millerk31.ioio.scribbler.test;

import android.app.Application;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import millerk31.myro.Scribbler;


/**
 * Created by Kevin on 10/6/2016.
 */

public class MyApp extends Application {

    //for UARTs

    // MainActivity moves here from its TX EditView
    // IOIO Service takes data from here and sends it to UART1 TX
    //public static Queue<Byte> s2OutQueue = new ConcurrentLinkedQueue<Byte>();

    // IOIO Service takes data from UART1 RX and places it here.
    // MainActivity moves data from here to its RX TextView
    //public static Queue<Byte> s2InQueue = new ConcurrentLinkedQueue<Byte>();

//    public static Queue<Byte> rplOutQueue = new ConcurrentLinkedQueue<Byte>();
//    public static Queue<Byte> rplInQueue = new ConcurrentLinkedQueue<Byte>();


    //save recieve textview data when activity pauses
    //and restore when it resumes
    public static String tvRxDataSave1 = "";
    public static String tvRxDataSave2 = "";

//    public static Scribbler scribbler;// = new Scribbler(s2InQueue, s2OutQueue);
//
//    public static boolean instantiateScribbler(){
//        scribbler = new Scribbler(s2InQueue, s2OutQueue);
//        if(scribbler !=null){
//            return true;
//        }
//        return false;
//    }
}
