package millerk31.ioio.scribbler.test;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.Uart;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOService;
import millerk31.myro.Scribbler;
import millerk31.myro.ScribblerHandler;
import millerk31.rplidar.RpLidarHandler;
import millerk31.rplidar.RpLidar;

//import static millerk31.ioio.scribbler.test.MyApp.s2InQueue;
//import static millerk31.ioio.scribbler.test.MyApp.s2OutQueue;

public class IOIOScribblerService extends IOIOService {

    private RpLidar rpLidar;
    private RpLidarHandler rplHandler;
    Scribbler scribbler;
    public ScribblerHandler s2Handler;

    final Messenger messenger = new Messenger(new IncomingHandler());

    public static final int IOIO_STATUS_REQUEST = 0;   //request IOIO Status
    public static final int IOIO_STATUS_REPLY = 1;   //request IOIO Status
    public static final int ERROR_REPLY = 2;   //TODO determine error details
    public static final int LED_BLINK_REQUEST = 3;   //request blinking status LED
    public static final int LED_OFF_REQUEST = 4;   //request turning off status LED
    public static final int LED_BLINK_REPLY = 5;   //LED was set to BLINK
    public static final int LED_OFF_REPLY = 6;   //LED was turned off
    public static final int LED_STATUS_REQUEST = 7;   //Status of LED request
    public static final int LED_STATUS_REPLY = 8;   //arg1 == 1 if true

    //    public static final int LIDAR_STATUS_REQUEST     = 9;   //request LIDAR status
//    public static final int LIDAR_STATUS_REPLY       = 10;  //arg1 == 1 if true
//    public static final int LIDAR_STOP               = 11;  //request LIDAR status
//    public static final int LIDAR_SCAN               = 12;  //request LIDAR status
//    public static final int LIDAR_FORCE_SCAN         = 11;  //request LIDAR status
//    public static final int LIDAR_RESET              = 12;  //request LIDAR status
//    public static final int LIDAR_GET_INFO         = 11;  //request LIDAR status
//    public static final int LIDAR_INFO_REPLY       = 12;  //request LIDAR status
//    public static final int LIDAR_GET_HEALTH         = 11;  //request LIDAR status
//    public static final int LIDAR_HEALTH_REPLY       = 12;  //request LIDAR status
//    public static final int LIDAR_GET_SAMPLERATE         = 11;  //request LIDAR status
//    public static final int LIDAR_SAMPLERATE_REPLY       = 12;  //request LIDAR status
//    public static final int LIDAR_ON_REQUEST         = 13;  //request LIDAR status
//    public static final int LIDAR_OFF_REQUEST        = 14;  //request LIDAR status
//    public static final int LIDAR_ON_REPLY           = 15;  //arg1 == 1 if true
//    public static final int LIDAR_OFF_REPLY          = 16;  //arg1 == 1 if true
    public static final int LIDAR_SPEED_REQ = 17;  //arg1 == speed 0.0 - 1.0
    public static final int LIDAR_SPEED_REPLY = 18;  //arg1 == speed 0.0 - 1.0

    public static final int SCRIBBLER_STATUS_REQUEST = 19;   //request Scribbler status
    public static final int SCRIBBLER_STATUS_REPLY = 20;  //arg1 == 1 if true
    public static final int SCRIBBLER_ON_REQUEST = 21;  //request Scribbler status
    public static final int SCRIBBLER_OFF_REQUEST = 22;  //request Scribbler status
    public static final int SCRIBBLER_ON_REPLY = 23;  //arg1 == 1 if true
    public static final int SCRIBBLER_OFF_REPLY = 24;  //arg1 == 1 if true
    public static final int SCRIBBLER_SPEED_REQ = 25;  //arg1 == speed 0.0 - 1.0

    //Intent Strings
    public static final String IOIO_CONNECTED_INTENT_MSG = "IOIO_CONNECTED";
    public static final String IOIO_DISCONNECTED_INTENT_MSG = "IOIO_DISCONNECTED";
    public static final String INPUT_QUEUE_1_INTENT_MSG = "INPUT_QUEUE_1";
    public static final String INPUT_QUEUE_2_INTENT_MSG = "INPUT_QUEUE_2";
    public static final String EXIT_EVERYTHING_INTENT_MSG = "EXIT_EVERYTHING";
    public static final String LIDAR_CONNECTED_INTENT_MSG = "LIDAR_CONNECTED";
    public static final String LIDAR_DISONNECTED_INTENT_MSG = "LIDAR_DISONNECTED";
    public static final String SCRIBBLER_CONNECTED_INTENT_MSG = "SCRIBBLER_CONNECTED";
    public static final String SCRIBBLER_DISCONNECTE_INTENT_MSG = "SCRIBBLER_DISCONNECTED";

    //IOIO Pin Assignments
    //scribbler
    private static final int UART_1_TX_PIN = 10;
    private static final int UART_1_RX_PIN = 11;
    //RpLidar
    private static final int UART_2_TX_PIN = 12;
    private static final int UART_2_RX_PIN = 13;
    private static final int LIDAR_PWM_MOTOR = 14;
    public static float lidar_speed = 0.0f;

    public static boolean led_blink = true;    //true if LED is in BLINK mode
    private static boolean led_status = false;   //true if LED is on at this moment
    private static boolean ledTimerFlag = false;
    private static boolean fiveSecondTimerFlag = false;
    public static boolean ioio_state = false;
    public static boolean lidar_connected = false;
    public static boolean scribbler_state = false;

    //declare Intents for broadcast messages
    Intent setupIntent;             //IOIO has connected
    Intent disconnectedIntent;      //IOIO has disconnected
    Intent inQueue1Intent;          //external data from s2InQueue
    Intent rplInQueueIntent;        //external data from inQueue2
    Intent exitEverythingIntent;    //close app
    Intent lidarConnectIntent;      //sent when communication with lidar is verified
    Intent lidarDisconnectIntent;
    Intent scribblerConnectIntent;
    Intent scribblerDisconnectIntent;

    private static Uart s2Uart;
    private static Uart rpUart;

    private static InputStream s2In;
    private static OutputStream s2Out;
    private static InputStream rplIn;
    private static OutputStream rplOut;
    private static PwmOutput rplMotor;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d("KSM", "Service onCreate started");
        rplHandler = RpLidarHandler.getInstance();
        rpLidar = RpLidar.getInstance();

        s2Handler = new ScribblerHandler();
        scribbler = Scribbler.getInstance();

        //create Intents for broadcast messages
        setupIntent = new Intent(IOIO_CONNECTED_INTENT_MSG);
        disconnectedIntent = new Intent(IOIO_DISCONNECTED_INTENT_MSG);
        inQueue1Intent = new Intent(INPUT_QUEUE_1_INTENT_MSG);
        rplInQueueIntent = new Intent(INPUT_QUEUE_2_INTENT_MSG);
        exitEverythingIntent = new Intent(EXIT_EVERYTHING_INTENT_MSG);
        lidarConnectIntent = new Intent(LIDAR_CONNECTED_INTENT_MSG);
        lidarDisconnectIntent = new Intent(LIDAR_DISONNECTED_INTENT_MSG);
        scribblerConnectIntent = new Intent(SCRIBBLER_CONNECTED_INTENT_MSG);
        scribblerDisconnectIntent = new Intent(SCRIBBLER_DISCONNECTE_INTENT_MSG);
    }

    static class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {

            Messenger rmsgr = msg.replyTo;
            Message rmsg;

            switch (msg.what) {
                case LED_BLINK_REQUEST:
                    Log.d("KSM", "LED_BLINK_REQUEST message handled");

                    if (!ioio_state) {
                        rmsg = Message.obtain(null, ERROR_REPLY, msg.what, 0);
                        Log.d("KSM", "Sending reply message ERROR_REPLY ");
                    } else {
                        rmsg = Message.obtain(null, LED_BLINK_REPLY, 0, 0);
                        Log.d("KSM", "Sending reply message LED_BLINK_REPLY ");
                        led_blink = true;
                    }

                    try {
                        rmsgr.send(rmsg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    break;

                case LED_OFF_REQUEST:
                    Log.d("KSM", "LED_OFF message handled");

                    if (!ioio_state) {
                        rmsg = Message.obtain(null, ERROR_REPLY, msg.what, 0);
                        Log.d("KSM", "Sending reply message ERROR_REPLY ");
                    } else {
                        rmsg = Message.obtain(null, LED_OFF_REPLY, 0, 0);
                        Log.d("KSM", "Sending reply message LED_OFF_REQUEST");
                        led_blink = false;
                    }

                    try {
                        rmsgr.send(rmsg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    break;

                case LED_STATUS_REQUEST:
                    Log.d("KSM", "LED_STATUS_REQUEST message handled");

                    rmsg = Message.obtain(null, LED_STATUS_REPLY, led_blink ? 1 : 0, 0);
                    Log.d("KSM", "Sending LED_STATUS_REPLY");
                    try {
                        rmsgr.send(rmsg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    break;

                case IOIO_STATUS_REQUEST:
                    Log.d("KSM", "IOIO_STATUS_REQUEST message handled");

                    rmsg = Message.obtain(null, IOIO_STATUS_REPLY, ioio_state ? 1 : 0, 0);
                    Log.d("KSM", "Sending reply message IOIO_STATUS_REPLY");
                    try {
                        rmsgr.send(rmsg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    break;

//                case LIDAR_STATUS_REQUEST:
//                    Log.d("KSM", "LIDAR_STATUS_REQUEST message handled");
//
//                    rmsg = Message.obtain(null, LIDAR_STATUS_REPLY, lidar_connected?1:0, 0);
//                    Log.d("KSM", "Sending reply message LIDAR_STATUS_REPLY");
//                    try {
//                        rmsgr.send(rmsg);
//                    } catch (RemoteException e) {
//                        e.printStackTrace();
//                    }
//
//                    break;
//
//                case LIDAR_ON_REQUEST:
//                    Log.d("KSM", "LIDAR_ON_REQUEST message handled");
//
//                    rmsg = Message.obtain(null, LIDAR_ON_REPLY, lidar_connected?1:0, 0);
//                    Log.d("KSM", "Sending reply message LIDAR_ON_REPLY");
//                    try {
//                        rmsgr.send(rmsg);
//                    } catch (RemoteException e) {
//                        e.printStackTrace();
//                    }
//
//                    break;
//
//                case LIDAR_OFF_REQUEST:
//                    Log.d("KSM", "LIDAR_OFF_REQUEST message handled");
//
//                    rmsg = Message.obtain(null, LIDAR_OFF_REPLY, lidar_connected?1:0, 0);
//                    Log.d("KSM", "Sending reply message LIDAR_OFF_REPLY");
//                    try {
//                        rmsgr.send(rmsg);
//                    } catch (RemoteException e) {
//                        e.printStackTrace();
//                    }
//
//                    break;
//
                case LIDAR_SPEED_REQ:
                    lidar_speed = msg.arg1 / 100.0f;

                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }

    @Override
    protected IOIOLooper createIOIOLooper() {
        return new BaseIOIOLooper() {
            private DigitalOutput led_;
            private Timer ledTimer_;
            private Timer fiveSecondTimer_;

            //temp storage from UARTs
            private byte inBytes1[] = new byte[256];
            private byte inBytes2[] = new byte[256];

            //temp debug vars
            int scanIndex = 0;
            int scanIncrement = 0;

            @Override
            protected void setup() throws ConnectionLostException, InterruptedException {
                ledTimer_ = new Timer();
                ledTimer_.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        setLedTimerFlag();
                    }
                }, 0, 500);

                fiveSecondTimer_ = new Timer();
                fiveSecondTimer_.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        setFiveSecondTimerFlag();
                    }
                }, 0, 5000);

                //UART for Scribbler S2 Robot
                s2Uart = ioio_.openUart(UART_1_RX_PIN, UART_1_TX_PIN, 38400, Uart.Parity.NONE, Uart.StopBits.ONE);
                //s2In = s2Uart.getInputStream();
                //s2Out = s2Uart.getOutputStream();

                //UART for RP-LIDAR A2
                rpUart = ioio_.openUart(UART_2_RX_PIN, UART_2_TX_PIN, 115200, Uart.Parity.NONE, Uart.StopBits.ONE);
                rplIn = rpUart.getInputStream();
                rplOut = rpUart.getOutputStream();

                //connect to scribbler
                //run in separate thread so service can continue
                _connectScribbler();

                //Lidar motor control
                rplMotor = ioio_.openPwmOutput(LIDAR_PWM_MOTOR, 25000);
                rplMotor.setDutyCycle(lidar_speed);

                led_ = ioio_.openDigitalOutput(IOIO.LED_PIN);
                ioio_state = true;
                sendBroadcast(setupIntent);
            }

            @Override
            public void loop() throws ConnectionLostException, InterruptedException {
//                rpLidar.scanData[scanIndex] = (65535/10) * scanIncrement;
//                if(++scanIndex == 23040){
//                    scanIndex = 0;
//                    scanIncrement = (++scanIncrement > 10)?0:scanIncrement;
//                }

                rplMotor.setDutyCycle(lidar_speed);

                //connect to scribbler
                if (fiveSecondTimerFlag) {
                    Log.d("S2Handler", "5 second flag set");
                    fiveSecondTimerFlag = false;

                    if (!scribbler_state) {
                        _connectScribbler();
                    }

                    //verify communication with lidar
                    if (!lidar_connected) {
                        Log.d("RplHandler", "IOIO Loop Thread: " + Thread.currentThread());
                        Log.d("RplHandler", "Connect start: " + System.currentTimeMillis());
                        lidar_connected = rplHandler.connectLidar();
                        Log.d("RplHandler", "Connect done: " + lidar_connected + " " + System.currentTimeMillis());
                        sendBroadcast(lidarConnectIntent);
                    }
                }

                if (ledTimerFlag) {
                    led_.write(!led_status);
                    ledTimerFlag = false;
                }

//                try {
//                    if (s2In.available() > 0) {
//                        int len = s2In.read(inBytes1);
//                        for (int i = 0; i < len; ++i)
//                            s2InQueue.add(inBytes1[i]);
//                        //sendBroadcast(inQueue1Intent);
//                    }
//
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//
//                if (!s2OutQueue.isEmpty()) {
//                    while (!s2OutQueue.isEmpty()) {
//                        byte ba = s2OutQueue.poll();
//
//                        try {
//                            s2Out.write(ba);
//                        } catch (IOException e) {
//                            Log.d("loop", "s2Out.write IO exception:\n" + e.getMessage());
//                            e.printStackTrace();
//                        } catch (Exception e) {
//                            Log.d("loop", "Write s2OutQueue: " + e.getMessage());
//                            e.printStackTrace();
//                        }
//                    }
//                }

                try {
                    if (rplIn.available() > 0) {
                        int len = rplIn.read(inBytes2);
                        boolean x;
                        for (int i = 0; i < len; ++i) {
                            x = rpLidar.inQueue.add(inBytes2[i]);
                            if (!x) {
                                int dummy = 0;
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                /*
                //send lidar reset every 5 seconds for debugging
                try {
                    rplOut.write(0xA5);
                    rplOut.write(0x40);
                    Thread.sleep(10);
                } catch (IOException e) {
                    Log.d("loop", "rplOut.write IO exception:\n" + e.getMessage());
                    e.printStackTrace();
                }*/

                if (!rpLidar.outQueue.isEmpty()) {
                    while (true) {
                        Byte ba = rpLidar.outQueue.poll();
                        if (ba == null)
                            break;

                        try {
                            rplOut.write(ba);
                            Log.d("Service", "Writing to Lidar: " + ba.toString());
                        } catch (IOException e) {
                            Log.d("loop", "rplOut.write IO exception:\n" + e.getMessage());
                            e.printStackTrace();
                        } catch (Exception e) {
                            Log.d("loop", "Write outQueue2: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }

            }

            @Override
            public void disconnected() {
                Log.d("KSM", "IOIO Disconnect");
                ioio_state = false;
                lidar_connected = false;
                ledTimer_.cancel();
                ledTimer_ = null;

                s2Uart = null;
                rpUart = null;
                sendBroadcast(disconnectedIntent);
            }

        };
    }

    private void setLedTimerFlag() {
        if (led_blink) {
            led_status = !led_status;
        } else {
            led_status = false;
        }
        ledTimerFlag = true;
        Log.d("S2Handler", "LED Timer in IOIO Loop");
    }

    private void setFiveSecondTimerFlag() {
        fiveSecondTimerFlag = !fiveSecondTimerFlag;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        int result = super.onStartCommand(intent, flags, startId);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (intent != null && intent.getAction() != null
                && intent.getAction().equals("stop")) {
            // User clicked the notification. Need to stop the service.
            nm.cancel(0);
            stopSelf();
        } else {
            // Service starting. Create a notification.
            Notification notification = new Notification(
                    R.drawable.ic_launcher, "IOIO IPC service running",
                    System.currentTimeMillis());
            notification
                    .setLatestEventInfo(this, "IOIO IPC Service", "Running",
                            PendingIntent.getService(this, 0, new Intent(
                                    "stop", null, this, this.getClass()), 0));
            notification.flags |= Notification.FLAG_ONGOING_EVENT;
            nm.notify(0, notification);
        }
        return result;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("KSM", "Service.onBind");
        return messenger.getBinder();
    }

    private void _connectScribbler(){
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run(){
                scribbler_state = scribbler.connect(s2Uart);
                if(scribbler_state){
                    Log.d("Service","************************************ SCRIBBLER CONNECTED ************************************");
                    sendBroadcast(scribblerConnectIntent);
                    scribbler.beep(440,2);
                    scribbler.playSong("A 1; F# 1; E 1; F# 1; A 1;", .05);
                    //s2Handler.playSong();
                }
            }
        });
        thread.start();
    }
}
