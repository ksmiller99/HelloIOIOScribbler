package millerk31.rplidar;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import millerk31.ioio.scribbler.test.IOIOScribblerService;
import millerk31.ioio.scribbler.test.MyApp;

/**
 * Created by Kevin on 11/25/2016.
 */

public class RpLidarHandler {
    private static RpLidarHandler instance = null;

    public static RpLidarHandler getInstance(){
        if(instance == null){
            instance = new RpLidarHandler();
        }
        return instance;
    }

    private RpLidar rpLidar;
    private static final int MSG_START_HELLO        = -1;
    private static final int MSG_CONNECT_REQUEST    = 0;
    private static final int MSG_CONNECT_REPLY      = 1;
    private static final int MSG_COMMAND_COMPLETE   = 2;
    private static final int MSG_STATUS_REQUEST     = 3;   //request LIDAR status
    private static final int MSG_STATUS_REPLY       = 4;  //arg1 == 1 if true
    private static final int MSG_STOP               = 5;  //request LIDAR status
    private static final int MSG_SCAN               = 6;  //request LIDAR status
    private static final int MSG_FORCE_SCAN         = 7;  //request LIDAR status
    private static final int MSG_RESET              = 8;  //request LIDAR status
    private static final int MSG_GET_INFO           = 9;  //request LIDAR status
    private static final int MSG_INFO_REPLY         = 10;  //request LIDAR status
    private static final int MSG_GET_HEALTH         = 11;  //request LIDAR status
    private static final int MSG_HEALTH_REPLY       = 12;  //request LIDAR status
    private static final int MSG_GET_SAMPLERATE     = 13;  //request LIDAR status
    private static final int MSG_SAMPLERATE_REPLY   = 14;  //request LIDAR status
    private static final int MSG_ON_REQUEST         = 15;  //request LIDAR status
    private static final int MSG_OFF_REQUEST        = 16;  //request LIDAR status
    private static final int MSG_ON_REPLY           = 17;  //arg1 == 1 if true
    private static final int MSG_OFF_REPLY          = 18;  //arg1 == 1 if true
    private static final int MSG_SPEED_REQ          = 19;  //arg1 == speed 0.0 - 1.0

    private HandlerThread ht;
    private Handler mRplHandler;
    private Handler mMainHandler;
    private boolean rplReady = false;  //ready to accept commannd

    private float _speed = 0;   //PWM spdeed  0.0% - 100.0%

    //IOIO service messenger
    boolean isBound = false;
    Messenger messenger = null;

    private RpLidarHandler(){
        rpLidar = RpLidar.getInstance();
        ht = new HandlerThread("The RPL new thread");
        ht.start();

        rplReady = true;
        Log.d("S2Handler", "UI: RPL handler thread started");

        //handles messages to Main thread from scribbler
        mMainHandler = new Handler(){
            public void handleMessage(Message msg){
                switch(msg.what){
                    case MSG_COMMAND_COMPLETE:
                        Log.d("S2Handler", "Main Thread: received notification of command completed ");
                        rplReady = true;
                        break;

                    case MSG_CONNECT_REPLY:
                        Log.d("S2Handler", "Main Thread: received notification of scribbler completed ");
                        rplReady = true;
                        break;

                    default:
                        break;
                }
            }
        };

        //handles messages from Main thread to scribbler and replies
        mRplHandler = new Handler(ht.getLooper()){

            //test the thread handling for troubleshooting
            public void handleMessage (Message msg){
                switch(msg.what){
                    case MSG_START_HELLO:
                        Log.d("rplHandler", "handleMessage " + msg.what + " in " + Thread.currentThread() + " now sleeping");
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Log.d("S2Handler", "Woke up, notifying UI thread...");
                        mMainHandler.sendEmptyMessage(MSG_COMMAND_COMPLETE);

                        break;

                    case MSG_CONNECT_REQUEST:
                        Log.d("RplHandler", "Starting connect... "+System.currentTimeMillis()+"in thread "+Thread.currentThread());
                        boolean res = (rpLidar.connect()==0);
                        Log.d("S2Handler", "Connect complete: "+res+" "+System.currentTimeMillis());
                        Message rplyMsg = Message.obtain(mMainHandler,MSG_CONNECT_REPLY,res?1:0,0);
                        rplyMsg.sendToTarget();
                        break;

                    default:
                        break;
                }
            }
        };
    }

    //Outbound messages go through ServiceConnection
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("KSM", "Lidar.onServiceConnected");
            isBound = true;

            // Create the Messenger object
            messenger = new Messenger(service);

            //update UI elements to match IOIO state
            Message msg = Message.obtain(null, IOIOScribblerService.IOIO_STATUS_REQUEST);
            msg.replyTo = new Messenger(new IncomingHandler());
            try {
                messenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

//            msg = Message.obtain(null, IOIOScribblerService.LIDAR_STATUS_REQUEST);
//            msg.replyTo = new Messenger(new IncomingHandler());
//            try {
//                messenger.send(msg);
//            } catch (RemoteException e) {
//                e.printStackTrace();
//            }
        }

        //create handler for incoming messages from IOIO (not broadcasts)
        class IncomingHandler extends Handler {
            @Override
            public void handleMessage(Message msg) {

                switch (msg.what) {
                    case IOIOScribblerService.LIDAR_SPEED_REPLY:
                        Log.d("RplHandler", "LIDAR_SPEED_REPLY message handled");
                        Bundle b = msg.getData();
                        _speed = b.getFloat("speed",-1);
                        break;

//                    case IOIOScribblerService.LIDAR_STATUS_REPLY:
//                        enableUi(msg.arg1 == 1);
//                        Toast.makeText(getBaseContext(),"LIDAR Status: "+ msg.arg1, Toast.LENGTH_SHORT).show();
//                        Log.d("KSM", "LIDAR_STATUS_REPLY: " + msg.arg1 + " message handled");
//                        break;

                case IOIOScribblerService.IOIO_STATUS_REPLY:
                    Log.d("RplHandler", "IOIO_STATUS_REPLY: " + msg.arg1 + " message handled");
                    break;

                    case IOIOScribblerService.ERROR_REPLY:
                        Log.d("RplHandler", "ERROR_REPLY to message type: " + msg.arg1 + " message handled");
                        break;

                    default:
                        Log.d("RplHandler", "UNKNOWN MESSAGE TYPE: " + msg.what);
                        super.handleMessage(msg);
                }
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("RplHandler", "activity_lidar.onServiceDisconnect");

            // unbind or process might have crashes
            messenger = null;
            isBound = false;
        }
    };


    public boolean connectLidar(){
        if(rplReady){
            rplReady = false;
            Log.d("RplHandler", "lidar connect request sending " + Thread.currentThread());
            mRplHandler.sendEmptyMessage(MSG_CONNECT_REQUEST);
        }
        else{
            Log.d("RplHandler","Lidar is not ready or busy");
        }
        return rpLidar.isconnected();
    }

    public boolean setSpeed(float spd){
        if(spd < 0f || spd > 100.0f){
            Log.d("RplHandler","Invalid speed request: "+spd);
            return false;
        }

        if(rplReady) {
            rplReady = false;
            Log.d("RplHandler", "lidar speed request sending " + Thread.currentThread());
            Bundle bndl = new Bundle();
            bndl.putFloat("speed", spd);
            Message msg = Message.obtain(mRplHandler, MSG_SPEED_REQ, bndl);
            msg.sendToTarget();
        }else{
            Log.d("RplHandler","Lidar is not ready or busy");
        }
        return true;
    }

    public String getDeviceInfo(){
        return rpLidar.getDeviceInfo();
    }


}
