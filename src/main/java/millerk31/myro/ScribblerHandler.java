package millerk31.myro;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import millerk31.ioio.scribbler.test.MyApp;

/**
 * Created by Kevin on 11/7/2016.
 */

public class ScribblerHandler {
    private Scribbler scribbler;
    private static final int MSG_START_HELLO = 0;
    private static final int MSG_COMMAND_COMPLETE = 1;
    private static final int MSG_CONNECT_REQUEST = 2;
    private static final int MSG_CONNECT_REPLY = 3;     //arg1 = 1 is true
    private static final int MSG_SONG_REQUEST = 4;
    private static final int MSG_SONG_REPLY = 5;
    private static final int MSG_CLOSE = 6;

    private HandlerThread ht;
    private Handler mS2Handler;
    private Handler mMainHandler;
    private boolean scribblerReady = false;  //ready to accept commannd
    public ScribblerHandler(){
        scribbler = Scribbler.getInstance();
        ht = new HandlerThread("The new thread");
        ht.start();

        scribblerReady = true;
        Log.d("S2Handler", "UI: handler thread started");

        //handles messages to Main thread from scribbler
        mMainHandler = new Handler(){
            public void handleMessage(Message msg){
                switch(msg.what){
                    case MSG_COMMAND_COMPLETE:
                        Log.d("S2Handler", "Main Thread: received notification of command completed ");
                        scribblerReady = true;
                        break;

                    case MSG_CONNECT_REPLY:
                        Log.d("S2Handler", "Main Thread: received notification of scribbler completed ");
                        scribblerReady = true;
                        break;

                    default:
                        break;
                }
            }
        };

        //handles messages from Main thread to scribbler and replies
        mS2Handler = new Handler(ht.getLooper()){

            public void handleMessage (Message msg){
                switch(msg.what){
                    case MSG_START_HELLO:
                        Log.d("S2Handler", "handleMessage " + msg.what + " in " + Thread.currentThread() + " now sleeping");
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Log.d("S2Handler", "Woke up, notifying UI thread...");
                        mMainHandler.sendEmptyMessage(MSG_COMMAND_COMPLETE);

                        break;

//                    case MSG_CONNECT_REQUEST:
//                        Log.d("S2Handler", "Starting connect... "+System.currentTimeMillis()+"in thread "+Thread.currentThread());
//                        boolean res = scribbler.connect(MyApp.s2InQueue, MyApp.s2OutQueue);
//                        Log.d("S2Handler", "Connect complete: "+res+" "+System.currentTimeMillis());
//                        Message rplyMsg = Message.obtain(mS2Handler,MSG_CONNECT_REPLY,res?1:0,0);
//                        rplyMsg.sendToTarget();
//                        break;

                    case MSG_SONG_REQUEST:
                        Bundle b = msg.getData();
                        scribbler.playSong(b.getString("notes"),b.getDouble("speed"));
                        mMainHandler.sendEmptyMessage(MSG_SONG_REQUEST);

                        break;

                    default:
                        break;
                }
            }
        };
    }
    public void sendLongHello(){
        if (scribblerReady){
            scribblerReady = false;
            Log.d("S2Handler", "sending hello " + Thread.currentThread());
            mS2Handler.sendEmptyMessage(MSG_START_HELLO);
        } else {
            Log.d("S2Handler", "Cannot do hello yet - not ready");
        }
    }

    public boolean connectScribbler(){
        if(scribblerReady){
            scribblerReady = false;
            Log.d("S2Handler", "scribbler connect request sending " + Thread.currentThread());
            mS2Handler.sendEmptyMessage(MSG_CONNECT_REQUEST);
        }
        else{
            Log.d("S2Handler","Scribbler is not ready or busy");
        }
        return scribbler.scribbler2Connected();
    }

    public void playSong(String notes, double speed){
        if(scribblerReady) {
            scribblerReady = false;
            Log.d("S2Handler", "Sending song request");
            Bundle b = new Bundle();
            b.putString("notes",notes);
            b.putDouble("speed",speed);
            Message msg = Message.obtain(mS2Handler,MSG_SONG_REQUEST);
            msg.setData(b);
            msg.sendToTarget();
        }
    }

    public void closeScribbler(){
        if(scribblerReady){
            scribblerReady = false;
            mS2Handler.sendEmptyMessage(MSG_CLOSE);
        }
    }
}
