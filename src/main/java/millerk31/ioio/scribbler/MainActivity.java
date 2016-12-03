package millerk31.ioio.scribbler;

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
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

import millerk31.myro.Scribbler;
import millerk31.rplidar.RpLidarService;

import static millerk31.ioio.scribbler.MyIoioService.IOIO_CONNECTED_INTENT_MSG;
import static millerk31.ioio.scribbler.MyIoioService.IOIO_DISCONNECTED_INTENT_MSG;
import static millerk31.ioio.scribbler.MyIoioService.LIDAR_CONNECTED_INTENT_MSG;
import static millerk31.ioio.scribbler.MyIoioService.LIDAR_DISCONNECTED_INTENT_MSG;
import static millerk31.ioio.scribbler.MyIoioService.SCRIBBLER_CONNECTED_INTENT_MSG;
import static millerk31.ioio.scribbler.MyIoioService.SCRIBBLER_DISCONNECTED_INTENT_MSG;
import static millerk31.ioio.scribbler.MyApp.thdIoioService;
import static millerk31.ioio.scribbler.MyApp.thdLidarService;

public class MainActivity extends Activity {

    Scribbler scribbler = Scribbler.getInstance();

    private ToggleButton toggleButton_;
    private TextView tvIpAddress;
    private TextView tvRxData;
    private Button btnGetInfo;
    private Button btnMotorsOn;
    private Button btnMotorsOff;
    private Button btn05;
    private Button btn00;
    private Button btn0F;
    private Button btnClear;
    private Button btnSong;
    private RadioGroup rgRepeat;

    boolean isIoioBound = false;
    Messenger messenger = null;

    private String ipAddress;

    //create IntentFilters for receiving broadcast messages
    IntentFilter ioioConnectFilter = new IntentFilter(IOIO_CONNECTED_INTENT_MSG);
    IntentFilter ioioCisconnectFilter = new IntentFilter(IOIO_DISCONNECTED_INTENT_MSG);
    IntentFilter scribblerConnectFilter = new IntentFilter(SCRIBBLER_CONNECTED_INTENT_MSG);
    IntentFilter scribblerDisconnectFilter = new IntentFilter(SCRIBBLER_DISCONNECTED_INTENT_MSG);
    IntentFilter lidarConnectFilter = new IntentFilter(LIDAR_CONNECTED_INTENT_MSG);
    IntentFilter lidarDisconnectFilter = new IntentFilter(LIDAR_DISCONNECTED_INTENT_MSG);

    private boolean repeatFlag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String TAG = "MainActivity.onCreate";
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //start the IOIO Service
//        startService(new Intent(this, MyIoioService.class));
        Log.d(TAG,"Creating thread for IOIO service...");
        thdIoioService = new Thread() {
            public void run() {
                startService(new Intent(getApplicationContext(), MyIoioService.class));
            }
        };
        thdIoioService.setName("thdIoioService");
        thdIoioService.start();

        //start the Lidar Service
        Log.d(TAG,"Creating thread for Lidar service...");
        thdLidarService = new Thread() {
            public void run() {
                getApplicationContext().bindService(
                        new Intent(getApplicationContext(), RpLidarService.class),
                        lidarServiceConnection,
                        Context.BIND_AUTO_CREATE);
            }
        };
        thdLidarService.setName("thdLidarService");
        thdLidarService.start();




        tvIpAddress = (TextView)findViewById(R.id.tvIpAddress);
        toggleButton_ = (ToggleButton) findViewById(R.id.ToggleButton);
        tvRxData = (TextView) findViewById(R.id.tvRxData1);
        btnGetInfo = (Button) findViewById(R.id.btnInfo);
        btnMotorsOn = (Button) findViewById(R.id.btnMotorsOn);
        btnMotorsOff = (Button) findViewById(R.id.btnMotorsOff);
        btn05 = (Button) findViewById(R.id.btn05);
        btn00 = (Button) findViewById(R.id.btn00);
        btn0F = (Button) findViewById(R.id.btn0F);
        btnClear = (Button)findViewById(R.id.btnClear);
        btnSong = (Button) findViewById(R.id.btnSong);
        rgRepeat = (RadioGroup)findViewById(R.id.rgRepeat);
        rgRepeat.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(checkedId == R.id.rbRepeatOn){
                    repeatFlag = true;
                } else
                if(checkedId == R.id.rbRepeatOff){
                    repeatFlag = false;
                }
            }
        });

        //assume IOIO is disconnected at start
        enableIoioUi(false);

        //bind to  the IOIO service
            Log.d(TAG,"Binding to existing IOIO service...");
            bindService(new Intent(this, MyIoioService.class), ioioServiceConnection, Context.BIND_AUTO_CREATE);

        //bind to Lidar service
            Log.d(TAG,"Binding to existing Lidar service...");
            bindService(new Intent(this, RpLidarService.class), lidarServiceConnection, Context.BIND_AUTO_CREATE);

        Log.d(TAG, "Finished");
    }

    //Outbound Lidar messages go through lidarServiceConnection
    private ServiceConnection lidarServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("KSM", "Main.Lidar.onServiceConnected");
            isIoioBound = true;

            // Create the Messenger object
            messenger = new Messenger(service);

            //update UI elements to match IOIO state
            Message msg = Message.obtain(null, MyIoioService.IOIO_STATUS_REQUEST);
            msg.replyTo = new Messenger(new IncomingHandler());
            try {
                messenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            msg = Message.obtain(null, MyIoioService.LED_STATUS_REQUEST);
            msg.replyTo = new Messenger(new IncomingHandler());
            try {
                messenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("KSM", "activity_main.onServiceDisconnect");

            // unbind or process might have crashes
            messenger = null;
            isIoioBound = false;
        }
    };

    //Outbound IOIO messages go through ioioServiceConnection
    private ServiceConnection ioioServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("KSM", "Main.ioio.onServiceConnected");
            isIoioBound = true;

            // Create the Messenger object
            messenger = new Messenger(service);

            //update UI elements to match IOIO state
            Message msg = Message.obtain(null, MyIoioService.IOIO_STATUS_REQUEST);
            msg.replyTo = new Messenger(new IncomingHandler());
            try {
                messenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            msg = Message.obtain(null, MyIoioService.LED_STATUS_REQUEST);
            msg.replyTo = new Messenger(new IncomingHandler());
            try {
                messenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("KSM", "activity_main.onServiceDisconnect");

            // unbind or process might have crashes
            messenger = null;
            isIoioBound = false;
        }
    };

    @Override
    protected void onResume() {
        ipAddress = getIPAddress(true);
        if(ipAddress.isEmpty()){
            ipAddress = "None - Check WiFi";
        }
        tvIpAddress.setText("IP Address: "+ipAddress);

        enableScribblerUi(scribbler.scribblerConnected());

        //setup broadcast receivers
        registerReceiver(myReceiver, ioioConnectFilter);
        registerReceiver(myReceiver, ioioCisconnectFilter);
        registerReceiver(myReceiver, scribblerConnectFilter);
        registerReceiver(myReceiver, scribblerDisconnectFilter);
        registerReceiver(myReceiver, lidarConnectFilter);
        registerReceiver(myReceiver, lidarDisconnectFilter);

        //update UI elements to match IOIO state
        if (isIoioBound) {
            Message msg = Message.obtain(null, MyIoioService.IOIO_STATUS_REQUEST);
            msg.replyTo = new Messenger(new IncomingHandler());
            try {
                messenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            msg = Message.obtain(null, MyIoioService.LED_STATUS_REQUEST);
            msg.replyTo = new Messenger(new IncomingHandler());
            try {
                messenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        //restore data to view that was erased when view paused
        tvRxData.setText(MyApp.tvRxDataSave1);

        Log.d("KSM", "Main.onResume completed");
        super.onResume();
    }

    @Override
    //make sure service is disconnected from activity
    protected void onDestroy() {
        unbindService(ioioServiceConnection);
        messenger = null;
        isIoioBound = false;

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

        MyApp.tvRxDataSave1 = tvRxData.getText().toString();

        super.onPause();
    }

    //create handler for incoming messages (not broadcasts)
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case MyIoioService.LED_BLINK_REPLY:
                    Log.d("KSM", "LED_BLINK_REPLY message handled");
                    toggleButton_.setChecked(true);
                    break;

                case MyIoioService.LED_OFF_REPLY:
                    Log.d("KSM", "LED_OFF_REPLY message handled");
                    toggleButton_.setChecked(false);
                    break;

                case MyIoioService.LED_STATUS_REPLY:
                    toggleButton_.setChecked(msg.arg1 == 1);
                    Log.d("KSM", "LED_STATUS_REPLY: " + msg.arg1 + " message handled");
                    break;

                case MyIoioService.IOIO_STATUS_REPLY:
                    enableIoioUi(msg.arg1 == 1);
                    Log.d("KSM", "IOIO_STATUS_REPLY: " + msg.arg1 + " message handled");
                    break;

                case MyIoioService.ERROR_REPLY:
                    Log.d("KSM", "ERROR_REPLY to message type: " + msg.arg1 + " message handled");
                    break;

                default:
                    Log.d("KSM", "UNKNOWN MESSAGE TYPE: " + msg.what);
                    super.handleMessage(msg);
            }
        }
    }

    public void tglOnClick(View v) {
        Log.d("KSM", "MAIN Toggle Button pressed.");
        ToggleButton tgl = (ToggleButton) v;
        int msgType;

        //set message type based on toggle status after clicking
        if (tgl.isChecked())
            msgType = MyIoioService.LED_BLINK_REQUEST;
        else
            msgType = MyIoioService.LED_OFF_REQUEST;

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

    //go to Second activity
    public void btnSecondOnClick(View v) {
        Intent intent = new Intent(this, SecondActivity.class);
        startActivity(intent);
    }

    //to receive broadcasts from MyIoioService
    BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String TAG = "MainActivity.myReceiver";

            Log.d(TAG, "Broadcast intent received");
            if (intent.getAction().equals(IOIO_CONNECTED_INTENT_MSG)) {
                enableIoioUi(true);
                Log.d(TAG, "Broadcast IOIO_CONNECTED_INTENT_MSG intent received");

            } else if (intent.getAction().equals(IOIO_DISCONNECTED_INTENT_MSG)) {
                enableIoioUi(false);
                Log.d(TAG, "Broadcast IOIO_DISCONNECTED_INTENT_MSG intent received");

            }  else if(intent.getAction().equals(SCRIBBLER_CONNECTED_INTENT_MSG)){
                Toast.makeText(getApplicationContext(),"S2 Connected",Toast.LENGTH_SHORT).show();
                enableScribblerUi(true);

            } else if(intent.getAction().equals(SCRIBBLER_DISCONNECTED_INTENT_MSG)){
                Toast.makeText(getApplicationContext(),"S2 Disconnected",Toast.LENGTH_SHORT).show();
                enableScribblerUi(false);

            } else if(intent.getAction().equals(LIDAR_CONNECTED_INTENT_MSG)){
                Toast.makeText(getApplicationContext(),"Lidar Connected",Toast.LENGTH_SHORT).show();
                //enableScribblerUi(true);

            } else if(intent.getAction().equals(LIDAR_DISCONNECTED_INTENT_MSG)){
                Toast.makeText(getApplicationContext(),"Lidar Disconnected",Toast.LENGTH_SHORT).show();
                //enableScribblerUi(false);
            }
        }
    };

    private void enableIoioUi(final boolean enable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                toggleButton_.setEnabled(enable);
             }
        });
    }

    private void enableScribblerUi(final boolean enable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnGetInfo.setEnabled(enable);
                btn00.setEnabled(enable);
                btn0F.setEnabled(enable);
                btn05.setEnabled(enable);
                btnMotorsOff.setEnabled(enable);
                btnMotorsOn.setEnabled(enable);
                btnClear.setEnabled(enable);
                btnSong.setEnabled(enable);
            }
        });
    }

    public void btnGetInfoOnClick(View v){
        String info;
        if (scribbler.scribblerConnected()) {
            info = scribbler.getInfo();
        }else{
            info = "Error: Scribbler not connected";
        }

        tvRxData.setText(tvRxData.getText()+"\n"+info);

        if(repeatFlag){
            new Handler().postDelayed(new Runnable(){
                @Override
                public void run(){
                    btnGetInfoOnClick(btnGetInfo);
                }
            },500);
        }
    }

    public void btnMotorsOnOnClick(View v){
        if(scribbler.scribblerConnected()){
            scribbler.motors(0.3,0.3);
        }else{
            tvRxData.setText(tvRxData.getText()+"\nError: Scribbler not connected");
        }
    }
    public void btnMotorsOffOnClick(View v){
        if(scribbler.scribblerConnected()){
            scribbler.motors(0,0);
        }else{
            tvRxData.setText(tvRxData.getText()+"\nError: Scribbler not connected");
        }
    }

    public void btn05OnClick(View v){

    }

    public void btnCloseOnClick(View v){
        //MyIoioService.s2Handler.closeScribbler();
    }

    public void btn0FOnClick(View v){
     }

    public void btnClearOnClick(View v){
        tvRxData.setText("");
    }

    final protected static char[] decimalArray = "0123456789".toCharArray();
    public static String bytesToDecimal(byte[] bytes) {
        char[] decimalChars = new char[bytes.length * 4];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            decimalChars[j * 4] = decimalArray[v / 100];
            decimalChars[j * 4 + 1] = decimalArray[(v / 10) % 10];
            decimalChars[j * 4 + 2] = decimalArray[v % 10];
            decimalChars[j * 4 + 3] = ' ';
        }
        return new String(decimalChars);
    }

    public void btnSongOnClick(View v){
        if (scribbler.scribblerConnected()) {
            scribbler.playSong("A 1; F# 1; E 1; F# 1; A 1;", .05);
        }else{
            tvRxData.setText(tvRxData.getText()+"\nError: Scribbler not connected");
        }
    }

    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } // for now eat exceptions
        return "";
    }
 }


