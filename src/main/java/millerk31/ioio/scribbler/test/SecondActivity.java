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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import static millerk31.ioio.scribbler.test.MyApp.tvRxDataSave2;

import millerk31.rplidar.*;

public class SecondActivity extends Activity {

    RpLidar rpLidar = RpLidar.getInstance();

    private ToggleButton toggleButton_;
    private Button btnReset;
    private Button btnStop;
    private Button btnScan;
    private Button btnXScan;
    private Button btnFScan;
    private Button btnInfo;
    private Button btnHealth;
    private Button btnSRate;
    private TextView tvRxData;
    private EditText etTxData;

    boolean isBound = false;
    Messenger messenger = null;

    //create IntentFilters for receiving broadcast messages
    IntentFilter connectFilter = new IntentFilter("IOIO_CONNECTED");
    IntentFilter disconnectFilter = new IntentFilter("IOIO_DISCONNECTED");
    IntentFilter lidarConnectFilter = new IntentFilter("LIDAR_CONNECTED");
    IntentFilter lidarDisconnectFilter = new IntentFilter("LIDAR_DISCONNECTED");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_second);

        toggleButton_ = (ToggleButton) findViewById(R.id.ToggleButton);
        tvRxData = (TextView) findViewById(R.id.tvRxData2);
        btnFScan = (Button) findViewById(R.id.btnFScan);
        btnReset = (Button) findViewById(R.id.btnReset);
        btnScan = (Button) findViewById(R.id.btnScan);
        btnXScan = (Button) findViewById(R.id.btnXScan);
        btnStop = (Button) findViewById(R.id.btnStop);
        btnInfo = (Button) findViewById(R.id.btnInfo);
        btnHealth = (Button) findViewById(R.id.btnHealth);
        btnSRate = (Button) findViewById(R.id.btnSRate);

        //assume IOIO is disconnected at start
        enableUi(false);

        //bind to  the IOIO service
        Intent intent = new Intent(this, IOIOScribblerService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        Log.d("KSM", "Second.onCreate Finished");
    }

    //Outbound messages go through ServiceConnection
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("KSM", "Second.onServiceConnected");
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

            msg = Message.obtain(null, IOIOScribblerService.LED_STATUS_REQUEST);
            msg.replyTo = new Messenger(new IncomingHandler());
            try {
                messenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("KSM", "Second.onServiceDisconnect");

            // unbind or process might have crashes
            messenger = null;
            isBound = false;
        }
    };

    @Override
    protected void onResume() {
        //enableLidarUi(rpLidar.isconnected());

        //setup broadcast receivers
        registerReceiver(myReceiver, connectFilter);
        registerReceiver(myReceiver, disconnectFilter);
        registerReceiver(myReceiver, lidarConnectFilter);
        registerReceiver(myReceiver, lidarDisconnectFilter);

        //update UI elements to match IOIO state
        if (isBound) {
            Message msg = Message.obtain(null, IOIOScribblerService.IOIO_STATUS_REQUEST);
            msg.replyTo = new Messenger(new IncomingHandler());
            try {
                messenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            msg = Message.obtain(null, IOIOScribblerService.LED_STATUS_REQUEST);
            msg.replyTo = new Messenger(new IncomingHandler());
            try {
                messenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        tvRxData.setText(tvRxDataSave2);

        Log.d("KSM", "Second.onResume completed");
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

        tvRxDataSave2 = tvRxData.getText().toString();
        super.onPause();
    }

    //create handler for incoming messages (not broadcasts)
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case IOIOScribblerService.LED_BLINK_REPLY:
                    Log.d("KSM", "LED_BLINK_REPLY message handled");
                    toggleButton_.setChecked(true);
                    break;

                case IOIOScribblerService.LED_OFF_REPLY:
                    Log.d("KSM", "LED_OFF_REPLY message handled");
                    toggleButton_.setChecked(false);
                    break;

                case IOIOScribblerService.LED_STATUS_REPLY:
                    toggleButton_.setChecked(msg.arg1 == 1);
                    Log.d("KSM", "LED_STATUS_REPLY: " + msg.arg1 + " message handled");
                    break;

                case IOIOScribblerService.IOIO_STATUS_REPLY:
                    enableUi(msg.arg1 == 1);
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

    public void tglOnClick(View v) {
        Log.d("KSM", "SECOND Toggle Button pressed.");
        ToggleButton tgl = (ToggleButton) v;
        int msgType;

        //set message type based on toggle status after clicking
        if (tgl.isChecked())
            msgType = IOIOScribblerService.LED_BLINK_REQUEST;
        else
            msgType = IOIOScribblerService.LED_OFF_REQUEST;

        //revert button state so that IOIO can control it via the reply message in case
        //there is some unknown reason in the service that would prevent the state change
        tgl.setChecked(!tgl.isChecked());

        Message msg = Message.obtain(null, msgType, 0, 0);
        msg.replyTo = new Messenger(new IncomingHandler());

        Log.d("KSM", "Toggle Message " + msgType + " sending...");

        try {
            messenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    //go to Main activity
    public void btnMainOnClick(View v) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    //go to Draw activity
    public void btnDrawOnClick(View v) {
        Intent intent = new Intent(this, LidarActivity.class);
        startActivity(intent);
    }

    //to receive broadcasts from IOIO
    BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("KSM", "Broadcast intent received");
            if (intent.getAction().equals("IOIO_DISCONNECTED")) {
                enableUi(false);
                Log.d("KSM", "Broadcast DISCONNECTED intent received");

            } else if (intent.getAction().equals("IOIO_CONNECTED")) {
                enableUi(true);
                Log.d("KSM", "Broadcast CONNECTED intent received");

            } else if (intent.getAction().equals("LIDAR_CONNECTED")) {
                enableUi(true);
                Log.d("KSM", "Broadcast LIDAR CONNECTED intent received");

                tvRxData.setText(rpLidar.getDeviceInfo());
            }

        }
    };

    private void enableUi(final boolean enable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                toggleButton_.setEnabled(enable);
               }
        });
    }

    private void enableLidarUi(final boolean enable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnFScan.setEnabled(enable);
                btnReset.setEnabled(enable);
                btnScan.setEnabled(enable);
                btnXScan.setEnabled(enable);
                btnStop.setEnabled(enable);
                btnInfo.setEnabled(enable);
                btnHealth.setEnabled(enable);
                btnSRate.setEnabled(enable);
            }
        });
    }

    private String bytesToString(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            if (b < 32 || b == 127) {
                builder.append("".format("0x%x", b));
            } else {
                builder.append((char) b);
            }
        }

        return builder.toString();
    }

    public void btnXScanOnClick(View v) {
//        byte[] bytes = {(byte) 0xA5, 0x40};
//        for(byte b :bytes)
//            rplidar.outQueue.add(b);
        //rplidar.startExpressScan();
    }

    public void btnResetOnClick(View v) {
        rpLidar.reset();
    }

    public void btnStopOnClick(View v) {
        //rplidar.stop();
    }

    public void btnFScanOnClick(View v) {
        //rplidar.startForceScan();
    }

    public void btnInfoOnClick(View v) {
        //rplidar.getInfo();
        tvRxData.setText(tvRxData.getText() + rpLidar.getDeviceInfo());
    }

    public void btnHealthOnClick(View v) {
        //rplidar.getHealth();
    }

    public void btnScanOnClick(View v) {
        //rplidar.startScan();
    }

    public void btnSRateOnClick(View v) {
        //rplidar.getSampleRate();
    }

    public void btnUnBusyOnClick(View v){
        rpLidar.unBusy();
    }
}
