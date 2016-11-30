package millerk31.ioio.scribbler.test;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import millerk31.rplidar.RpLidar;

import static millerk31.ioio.scribbler.test.R.id.pnl;

public class LidarActivity extends Activity {
    private RpLidar rpLidar = RpLidar.getInstance();

    private ToggleButton tglLidar;
    private Button btnErase;
    private Button btnSecond;
    private Button btnMain;
    private SeekBar sbMotorSpeed;
    private Button btnScan;
    private TextView tvSpeedDisplay;

    MyPanelAuto panel;

    boolean isBound = false;
    Messenger messenger = null;

    //create IntentFilters for receiving broadcast messages
    IntentFilter connectFilter = new IntentFilter("IOIO_CONNECTED");
    IntentFilter disconnectFilter = new IntentFilter("IOIO_DISCONNECTED");
    IntentFilter lidarConnectFilter = new IntentFilter("LIDAR_CONNECTED");
    IntentFilter lidarDisconnectFilter = new IntentFilter("LIDAR_DISCONNECTED");
    IntentFilter lidarDrawFilter = new IntentFilter("LIDAR_DRAW");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lidar);

        panel = (MyPanelAuto) findViewById(pnl);
        tglLidar = (ToggleButton) findViewById(R.id.tglLidar);
        btnErase = (Button) findViewById(R.id.btnErase);
        btnSecond = (Button) findViewById(R.id.btnSecond);
        btnMain = (Button) findViewById(R.id.btnMain);
        sbMotorSpeed = (SeekBar)  findViewById(R.id.sbMotorSpeed);
        btnScan = (Button) findViewById(R.id.btnScan);
        tvSpeedDisplay = (TextView) findViewById(R.id.tvSpeedDisplay) ;


        //bind to  the IOIO service
        Intent intent = new Intent(this, IOIOScribblerService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        sbMotorSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvSpeedDisplay.setText("Speed: "+progress+"%");
                rpLidar.setLidarSpeed(progress/100.0f);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
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
            msg.replyTo = new Messenger(new LidarActivity.IncomingHandler());
            try {
                messenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

//            msg = Message.obtain(null, IOIOScribblerService.LIDAR_STATUS_REQUEST);
//            msg.replyTo = new Messenger(new LidarActivity.IncomingHandler());
//            try {
//                messenger.send(msg);
//            } catch (RemoteException e) {
//                e.printStackTrace();
//            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("KSM", "activity_lidar.onServiceDisconnect");

            // unbind or process might have crashes
            messenger = null;
            isBound = false;
        }
    };

    @Override
    protected void onResume() {
        sbMotorSpeed.setProgress((int) (rpLidar.getlidarSpeed() * 100));
        enableLidarUi(rpLidar.isconnected());

        //setup broadcast receivers
        registerReceiver(myReceiver, connectFilter);
        registerReceiver(myReceiver, disconnectFilter);
        registerReceiver(myReceiver, lidarConnectFilter);
        registerReceiver(myReceiver, lidarDisconnectFilter);
        registerReceiver(myReceiver, lidarDrawFilter);

        //update UI elements to match IOIO state
        if (isBound) {
            Message msg = new Message();
            msg = Message.obtain(null, IOIOScribblerService.IOIO_STATUS_REQUEST);
            msg.replyTo = new Messenger(new LidarActivity.IncomingHandler());
            try {
                messenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            msg = Message.obtain(null, IOIOScribblerService.IOIO_STATUS_REQUEST);
            msg.replyTo = new Messenger(new LidarActivity.IncomingHandler());
            try {
                messenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        super.onResume();
    }

    @Override
    //make sure service is disconnected from activity
    protected void onDestroy() {
        unbindService(serviceConnection);
        messenger = null;
        isBound = false;

        super.onDestroy();
    }

    @Override
    //disable broadcast receiver when activity is not active
    protected void onPause() {
        try {
            unregisterReceiver(myReceiver);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("Receiver not registered")) {
                // Ignore this exception. This is exactly what is desired
                Log.w("KSM", "Tried to unregister the receiver when it's not registered");
            } else {
                // unexpected, re-throw
                throw e;
            }
        }

        super.onPause();
    }

    //create handler for incoming messages (not broadcasts)
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
//                case IOIOScribblerService.LIDAR_ON_REPLY:
//                    Log.d("KSM", "LIDAR_ON_REPLY message handled");
//                    tglLidar.setChecked(true);
//                    break;
//
//                case IOIOScribblerService.LIDAR_OFF_REPLY:
//                    Log.d("KSM", "LIDAR_OFF_REPLY message handled");
//                    tglLidar.setChecked(false);
//                    break;
//
//                case IOIOScribblerService.LIDAR_STATUS_REPLY:
//                    enableIoioUi(msg.arg1 == 1);
//                    Toast.makeText(getBaseContext(),"LIDAR Status: "+ msg.arg1, Toast.LENGTH_SHORT).show();
//                    Log.d("KSM", "LIDAR_STATUS_REPLY: " + msg.arg1 + " message handled");
//                    break;

                case IOIOScribblerService.IOIO_STATUS_REPLY:
                    enableIoioUi(msg.arg1 == 1);
                    Log.d("KSM", "IOIO_STATUS_REPLY: " + msg.arg1 + " message handled");
                    break;

                case IOIOScribblerService.ERROR_REPLY:
                    Log.d("KSM", "ERROR_REPLY to message type: " + msg.arg1 + " message handled");
                    break;

                default:
                    Log.d("KSM", "UNKNOWN MESSAGE TYPE: " + msg.what);
                    super.handleMessage(msg);
            }
        }

    }

//    public void tglLidarOnClick(View v) {
//        Log.d("KSM", "LIDAR Toggle Button pressed.");
//        ToggleButton tgl = (ToggleButton) v;
//        int msgType;
//
//        //set message type based on toggle status after clicking
//        if (tgl.isChecked())
//            msgType = IOIOScribblerService.LIDAR_ON_REQUEST;
//        else
//            msgType = IOIOScribblerService.LIDAR_OFF_REQUEST;
//
//        //revert button state so that IOIO can control it via the reply message in case
//        //there is some unknown reason in the service that would prevent the state change
//        tgl.setChecked(!tgl.isChecked());
//
//        Message msg = Message.obtain(null, msgType, 0, 0);
//        msg.replyTo = new Messenger(new LidarActivity.IncomingHandler());
//
//        Log.d("KSM", "Toggle Message " + msgType + " sending...");
//
//        try {
//            messenger.send(msg);
//        } catch (RemoteException e) {
//            e.printStackTrace();
//        }
//    }

    //Erase LIDAR disply
    public void btnEraseOnClick(View v){
        //panel.erase();
    }

    //go to Second activity
    public void btnSecondOnClick(View v) {
        Intent intent = new Intent(this, SecondActivity.class);
        startActivity(intent);
    }

    //go to Main activity
    public void btnMainOnClick(View v) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    //Lidar toggle button
    public void tglOnClick(View v) {
        Log.d("KSM", "LIDAR Toggle Button pressed.");
//        ToggleButton tgl = (ToggleButton) v;
//        int msgType;
//
//        //set message type based on toggle status after clicking
//        if (tgl.isChecked())
//            msgType = IOIOScribblerService.LIDAR_ON_REQUEST;
//        else
//            msgType = IOIOScribblerService.LIDAR_OFF_REQUEST;
//
//        //revert button state so that IOIO can control it via the reply message in case
//        //there is some unknown reason in the service that would prevent the state change
//        tgl.setChecked(!tgl.isChecked());
//
//        Message msg = Message.obtain(null, msgType, 0, 0);
//        msg.replyTo = new Messenger(new LidarActivity.IncomingHandler());
//
//        Log.d("KSM", "Lidar Toggle Message " + msgType + " sending...");
//
//        try {
//            messenger.send(msg);
//        } catch (RemoteException e) {
//            e.printStackTrace();
//        }

    }

    //to receive broadcasts from IOIO
    BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("KSM", "Broadcast intent received");
            if (intent.getAction().equals("IOIO_DISCONNECTED")) {
                enableIoioUi(false);
                Log.d("KSM", "Broadcast DISCONNECTED intent received");

            } else if (intent.getAction().equals("IOIO_CONNECTED")) {
                enableIoioUi(true);
                Log.d("KSM", "Broadcast CONNECTED intent received");

            } else if (intent.getAction().equals("LIDAR_DISCONNECTED")) {
                enableIoioUi(false);
                Log.d("KSM", "Broadcast LIDAR_DISCONNECTED intent received");

            } else if (intent.getAction().equals("LIDAR_CONNECTED")) {
                enableIoioUi(true);
                Log.d("KSM", "Broadcast LIDAR_CONNECTED intent received");

            } else if (intent.getAction().equals("LIDAR_DRAW")) {
                enableIoioUi(true);
                Log.d("KSM", "Broadcast LIDAR_DRAW intent received");
            }

        }
    };

    private void enableIoioUi(final boolean enable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

            }
        });
    }
    private void enableLidarUi(final boolean enable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tglLidar.setEnabled(enable);
                sbMotorSpeed.setEnabled(enable);
                btnScan.setEnabled(enable);
            }
        });
    }

    public void btnPwmOnClick(View v){
        Button btn = (Button) v;
        int speed = Integer.parseInt(btn.getText().toString());

        int msgType = IOIOScribblerService.LIDAR_SPEED_REQ;
        Message msg = Message.obtain(null,msgType,speed,0);
        try {
            messenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void btnScanOnClick(View v){
//        byte[] bytes = {(byte)0xA5, 0x40};
//        for(byte b:bytes)
//            rplidar.outQueue.add(b);
    }



}


