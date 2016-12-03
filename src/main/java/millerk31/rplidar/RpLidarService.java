package millerk31.rplidar;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;

import millerk31.ioio.scribbler.MyApp;
import millerk31.ioio.scribbler.MyIoioService;
import millerk31.ioio.scribbler.R;
import millerk31.ioio.scribbler.SecondActivity;
import millerk31.rplidar.RpLidar;

import static millerk31.ioio.scribbler.MyIoioService.IOIO_CONNECTED_INTENT_MSG;
import static millerk31.ioio.scribbler.MyIoioService.IOIO_DISCONNECTED_INTENT_MSG;
import static millerk31.ioio.scribbler.MyIoioService.LIDAR_CONNECTED_INTENT_MSG;
import static millerk31.ioio.scribbler.MyIoioService.LIDAR_DISCONNECTED_INTENT_MSG;

/**
 * Created by Kevin on 12/2/2016.
 * https://developer.android.com/reference/android/app/Service.html
 */

public class RpLidarService extends Service {
    private RpLidar rpLidar;
    boolean isIoioBound = false;
    Messenger ioioMessenger = null;

    //    private static final int MSG_START_HELLO        = -1;
//    private static final int MSG_CONNECT_REQUEST    = 0;
//    private static final int MSG_CONNECT_REPLY      = 1;
//    private static final int MSG_COMMAND_COMPLETE   = 2;
//    private static final int MSG_STATUS_REQUEST     = 3;   //request LIDAR status
//    private static final int MSG_STATUS_REPLY       = 4;  //arg1 == 1 if true
//    private static final int MSG_STOP               = 5;  //request LIDAR status
//    private static final int MSG_SCAN               = 6;  //request LIDAR status
//    private static final int MSG_FORCE_SCAN         = 7;  //request LIDAR status
//    private static final int MSG_RESET              = 8;  //request LIDAR status
//    private static final int MSG_GET_INFO           = 9;  //request LIDAR status
//    private static final int MSG_INFO_REPLY         = 10;  //request LIDAR status
//    private static final int MSG_GET_HEALTH         = 11;  //request LIDAR status
//    private static final int MSG_HEALTH_REPLY       = 12;  //request LIDAR status
//    private static final int MSG_GET_SAMPLERATE     = 13;  //request LIDAR status
//    private static final int MSG_SAMPLERATE_REPLY   = 14;  //request LIDAR status
//    private static final int MSG_ON_REQUEST         = 15;  //request LIDAR status
//    private static final int MSG_OFF_REQUEST        = 16;  //request LIDAR status
//    private static final int MSG_ON_REPLY           = 17;  //arg1 == 1 if true
//    private static final int MSG_OFF_REPLY          = 18;  //arg1 == 1 if true
    private static final int MSG_SPEED_REQ          = 19;  //arg1 == speed 0.0 - 1.0

    //create IntentFilters for receiving broadcast messages
    IntentFilter ioioConnectFilter = new IntentFilter(IOIO_CONNECTED_INTENT_MSG);
    IntentFilter ioioCisconnectFilter = new IntentFilter(IOIO_DISCONNECTED_INTENT_MSG);
    //IntentFilter scribblerConnectFilter = new IntentFilter(SCRIBBLER_CONNECTED_INTENT_MSG);
    //IntentFilter scribblerDisconnectFilter = new IntentFilter(SCRIBBLER_DISCONNECTED_INTENT_MSG);
    IntentFilter lidarConnectFilter = new IntentFilter(LIDAR_CONNECTED_INTENT_MSG);
    IntentFilter lidarDisconnectFilter = new IntentFilter(LIDAR_DISCONNECTED_INTENT_MSG);

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    /** For showing and hiding our notification. */
    NotificationManager mNM;
    /** Keeps track of all current registered clients. */
    ArrayList<Messenger> mClients = new ArrayList<Messenger>();
    /** Holds last value set by a client. */
    int mValue = 0;

    /**
     * Command to the service to register a client, receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client where callbacks should be sent.
     */
    static final int MSG_REGISTER_CLIENT = 1;

    /**
     * Command to the service to unregister a client, ot stop receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client as previously given with MSG_REGISTER_CLIENT.
     */
    static final int MSG_UNREGISTER_CLIENT = 2;

    /**
     * Command to service to set a new value.  This can be sent to the
     * service to supply a new value, and will be sent by the service to
     * any registered clients with the new value.
     */
    static final int MSG_SET_VALUE = 3;

    static final int MSG_LIDAR_CONNECT_REQ = 4;
    public static final int MSG_LIDAR_RESET_REQ   = 5;
    static final int MSG_LIDAR_INFO_REQ    = 6;
    static final int MSG_LIDAR_HEALTH_REQ  = 7;

    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {
        String TAG = "LidarMessenger.handler";

        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG,"Message received");

            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    break;

                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;

                case MSG_SET_VALUE:
                    mValue = msg.arg1;
                    for (int i=mClients.size()-1; i>=0; i--) {
                        try {
                            mClients.get(i).send(Message.obtain(null,
                                    MSG_SET_VALUE, mValue, 0));
                        } catch (RemoteException e) {
                            // The client is dead.  Remove it from the list;
                            // we are going through the list from back to front
                            // so this is safe to do inside the loop.
                            mClients.remove(i);
                        }
                    }
                    break;

                case MSG_LIDAR_RESET_REQ:
                    rpLidar.connect(MyIoioService.rpUart);
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }

    @Override
    public void onCreate() {
        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Display a notification about us starting.
        showNotification();

        rpLidar = RpLidar.getInstance();

        //bind to  the IOIO service
        if (MyApp.thdIoioService != null) {
            Thread t = new Thread() {
                public void run() {
                    getApplicationContext().bindService(
                            new Intent(getApplicationContext(), MyIoioService.class),
                            ioioServiceConnection,
                            Context.BIND_AUTO_CREATE);
                }
            };
            t.setName("thdIoioService");
            t.start();
        } else {
            bindService(new Intent(this, MyIoioService.class), ioioServiceConnection, Context.BIND_AUTO_CREATE);

        }
    }


    //Outbound IOIO messages go through ioioServiceConnection
    private ServiceConnection ioioServiceConnection = new ServiceConnection() {
        String TAG  = "LidarMessenger.ServConn";
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            isIoioBound = true;

            // Create the Messenger object
            ioioMessenger = new Messenger(service);

            //update UI elements to match IOIO state
            Message msg = Message.obtain(null, MyIoioService.IOIO_STATUS_REQUEST);
            msg.replyTo = new Messenger(new IncomingHandler());
            try {
                ioioMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            msg = Message.obtain(null, MyIoioService.LED_STATUS_REQUEST);
            msg.replyTo = new Messenger(new IncomingHandler());
            try {
                ioioMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("KSM", "Second.onServiceDisconnect");

            // unbind or process might have crashes
            ioioMessenger = null;
            isIoioBound = false;
        }
    };

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        mNM.cancel(R.string.remote_service_started);

        unbindService(ioioServiceConnection);
        isIoioBound = false;

        // Tell the user we stopped.
        Toast.makeText(this, R.string.remote_service_stopped, Toast.LENGTH_SHORT).show();
    }

    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        Log.d("LidarMsgService.onBind","Bound");
        return mMessenger.getBinder();
    }

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.remote_service_started);

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, SecondActivity.class), 0);

        // Set the info for the views that show in the notification panel.
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_lidar)  // the status icon
                .setTicker(text)  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle(getText(R.string.local_service_label))  // the label of the entry
                .setContentText(text)  // the contents of the entry
                .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
                .build();

        // Send the notification.
        // We use a string id because it is a unique number.  We use it later to cancel.
        mNM.notify(R.string.remote_service_started, notification);
    }
}
