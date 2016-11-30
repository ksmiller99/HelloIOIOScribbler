package millerk31.rplidar;

import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import ioio.lib.api.Uart;
import millerk31.ioio.scribbler.test.IOIOScribblerService;

//import static millerk31.rplidar.RPLidar_protocol.RPLIDAR_CMDFLAG_HAS_PAYLOAD;
//import static millerk31.rplidar.RPLidar_protocol.RPLIDAR_CMD_SYNC_BYTE;
import static millerk31.rplidar.RPTypes.RESULT_BUSY;
import static millerk31.rplidar.RPTypes.RESULT_FAIL_BIT;


/**
 * Created by Kevin on 10/24/2016.
 */

public class RpLidar {
    private static RpLidar instance = null;

    public static RpLidar getInstance(){
        if (instance == null){
            instance = new RpLidar();
        }
        return instance;
    }

//    public static final int MSG_CONNECT            = 0 ;
//    public static final int MSG_STATUS_REQUEST     = 9 ;   //request LIDAR status
//    public static final int MSG_STATUS_REPLY       = 10;  //arg1 == 1 if true
//    public static final int MSG_STOP               = 11;  //request LIDAR status
//    public static final int MSG_SCAN               = 12;  //request LIDAR status
//    public static final int MSG_FORCE_SCAN         = 11;  //request LIDAR status
//    public static final int MSG_RESET              = 12;  //request LIDAR status
//    public static final int MSG_GET_INFO           = 11;  //request LIDAR status
//    public static final int MSG_INFO_REPLY         = 12;  //request LIDAR status
//    public static final int MSG_GET_HEALTH         = 11;  //request LIDAR status
//    public static final int MSG_HEALTH_REPLY       = 12;  //request LIDAR status
//    public static final int MSG_GET_SAMPLERATE     = 11;  //request LIDAR status
//    public static final int MSG_SAMPLERATE_REPLY   = 12;  //request LIDAR status
//    public static final int MSG_ON_REQUEST         = 13;  //request LIDAR status
//    public static final int MSG_OFF_REQUEST        = 14;  //request LIDAR status
//    public static final int MSG_ON_REPLY           = 15;  //arg1 == 1 if true
//    public static final int MSG_OFF_REPLY          = 16;  //arg1 == 1 if true
//    public static final int MSG_SPEED_REQ          = 17;  //arg1 == speed 0.0 - 1.0

    private static RplidarDeviceHealth _healthInfo;
    private static RplidarDeviceInfo _deviceInfo;
    private static float _lidarSpeed = 0.0f;        //duty cycle 0.0 - 1.0
    private static boolean _busy = false;
    private static boolean _stopRequest = false;

    private static Thread _thdGrabScanPoints = null;

    private Uart _uart;
    private InputStream _inputStream;
    private OutputStream _outputStream;

    private static boolean _isconnected = false;

    /**
     * How the Lidar responds to a command type.
     */
    private enum RespMode{
        SINGLE,     //lidar sends one response header and reponse data packet
        MULTIPLE,   //lidar sends one reponse header and multiple data packets until STOP command is received
        NONE};      //lidar send no reponse header or data packet

    /**
     * List to hold all commands
     */
    private ArrayList<RplRequest> rplReqList = new ArrayList<RplRequest>();

    /**
     * Lidar commands.
     */
    private enum RequestTypes {
        STOP,               //(no reponse) Exit the current state and enter the idle state
        RESET,              //(no reponse) Reset(reboot) the RPLIDAR core
        SCAN,               //(multiple reponse) Enter the scanning state
        EXPRESS_SCAN,       //(multiple reponse) Enter the scanning state and working at the highest speed
        FORCE_SCAN,         //(multiple reponse) Enter the scanning state and force data output without checking rotation speed
        GET_INFO,           //(multiple reponse) Send out the device info (e.g. serial number)
        GET_HEALTH,         //(multiple reponse) Send out the device health info
        GET_SAMPLERATE};    //(multiple reponse) Send out single sampling time

    //LIDAR SCAN DATA 1/64th of a degree * 360° = 23040
    public static int[] scanData = new int[23040];



    /**
     * Robopeak A2 Lidar object.
     */
    public RpLidar(){
        _healthInfo = new RplidarDeviceHealth();
        _deviceInfo = new RplidarDeviceInfo();

        makeReqList();
    }

    // open the given serial interface and try to connect to the RPLIDAR
    public synchronized int connect(Uart uart){
        String TAG = "RpLidar.connect";

        Log.d(TAG,"Connect started");
        if (_busy){
            Log.d(TAG,"Connect return: _busy");

            return RESULT_BUSY;
        }
        _busy = true;

        int retval = RPTypes.RESULT_OPERATION_FAIL;

        if(!IOIOScribblerService.ioio_state){
            _isconnected = false;
            Log.d(TAG,"Return: IOIO not connected");
            _busy = false;
            return retval;
        }

        this._uart = uart;
        _inputStream = _uart.getInputStream();
        _outputStream = _uart.getOutputStream();

        //give time for streams to open
        Thread.yield();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Log.d(TAG,"FAIL: sleep interrupt");
            _busy = false;
            return retval;
        }

        Log.d(TAG,"Flushing");
        _flushInput();
        try {
            _outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG,"FAIL: IO Exception");
            _busy = false;
            return retval;
        }

        Log.d(TAG,"RESET Starting...");
        if (!reset()){
            Log.d(TAG,"FAIL: reset() failed");
            _busy = false;
            return retval;
        }

        //give undocumented reset data response time to be received, then flush it
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Log.d(TAG,"FAIL: sleep interrupt");
            _busy = false;
            return retval;
        }

        Log.d(TAG,"Before flush...");
        try {
            Log.d(TAG,"... inputStream.avail = "+_inputStream.available());
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG,"FAIL: IO Exception");
            _busy = false;
            return retval;
        }

        _flushInput();
        try {
            _outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG,"FAIL: IO Exception");
            _busy = false;
            return retval;
        }

        Log.d(TAG,"After flush...");
        try {
            Log.d(TAG,"... inputStream.avail = "+_inputStream.available());
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG,"FAIL: IO Exception");
            _busy = false;
            return retval;
        }

        Log.d(TAG,"GetInfo started");
        if(!getInfo()){
            Log.d(TAG,"FAIL: GetInfo() Failed");
            _busy =false;
            return retval;
        }

        Log.d(TAG,"GetHealth started");
        if(!getHealth()){
            Log.d(TAG,"FAIL: GetHealth() Failed");
            _busy =false;
            return retval;
        }

        _isconnected = true;
        retval =  RPTypes.RESULT_OK;
        _busy = false;
        Log.d(TAG,"Return: SUCCESS");
        return retval;
    }

    // close the currently opened serial interface
    public void disconnect()
    {
        String TAG = "RpLidar.disconnect";

        Log.d(TAG,"Starting...");
        if(_thdGrabScanPoints != null && _thdGrabScanPoints.isAlive()){
            _thdGrabScanPoints.interrupt();
        }
        _busy = true;
        _outputStream = null;
        _inputStream = null;
        _uart = null;
        _healthInfo = new RplidarDeviceHealth();
        _deviceInfo = new RplidarDeviceInfo();
        _lidarSpeed = 0.0f;
        Log.d(TAG,"Finished");
        _busy = false;
    }

    // check whether the serial interface is opened
    public boolean isOpen()
    {
        return IOIOScribblerService.ioio_state;
    }

    /**
     * Exit the current state and enter the idle state
     * @return true if command was placed in queue
     */
    public boolean stop(){
        if(!isOpen()) return false;

//        RplRequest req = getRplRequest(RequestTypes.STOP);
//        if(req==null) return false;
//
//        if (!doRplRequest(req)) {
//            return false;
//        }

        //STOP command outputs immediately, so no doRplRequest
        try {
            _outputStream.write(new byte[]{(byte)0xA5, 0x25});
            _lidarSpeed = 0.0f;
        } catch (IOException e) {
            e.printStackTrace();
        }

        SystemClock.sleep(1);

        return true;
    }

    /**
     * Reset(reboot) the RPLIDAR core
     * No response is defined, but some testing shows some text is returned. See comments for details
     * @return true if command was placed in queue
     */
    public synchronized boolean reset(){
        String TAG = "RpLidar.reset";
        Log.d(TAG, "Starting...");

        if(!isOpen()) return false;

        RplRequest req = getRplRequest(RequestTypes.RESET);
        if(req==null){
            Log.d(TAG,"FAIL: req = null");
            return false;
        }

        if (!doRplRequest(req)) {
            Log.d(TAG,"FAIL: doRplRequest failed");
            return false;
        }

        //troubleshooting
//        long timeoutMs = System.currentTimeMillis()+1000;
//        while (System.currentTimeMillis() < timeoutMs){
//            Thread.yield();
//        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Log.d(TAG,"FAIL: Sleep interrupted");
            return false;
        }

        Log.d(TAG,"Before flush...");
        try {
            Log.d(TAG,"... inputStream.avail = "+_inputStream.available());
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG,"FAIL: IO Exception");
            return false;
        }

        _flushInput();
        try {
            _outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG,"FAIL: Sleep interrupted");
            return false;
        }

        Log.d(TAG,"After flush...");
        try {
            Log.d(TAG,"... inputStream.avail = "+_inputStream.available());
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG,"FAIL: Sleep interrupted");
            return false;
        }


        Log.d(TAG,"Return: SUCCESS");
        return true;
    }

    // ask the RPLIDAR for its health info
    public synchronized boolean getHealth(){
        boolean retval = false;
        if (!isOpen()) return retval;

        RplRequest req = getRplRequest(RequestTypes.GET_HEALTH);
        if(req==null) return retval;

        if (!doRplRequest(req)) {
            return retval;
        }

        retval = true;
        _healthInfo.update(req.dataResponse);
        return retval;
    }

    public synchronized boolean getInfo(){
        boolean retval = false;
        if (!isOpen()) return retval;

        RplRequest req = getRplRequest(RequestTypes.GET_INFO);
        if(req==null) return retval;

        _flushInput();
        if (!doRplRequest(req)) {
            return retval;
        }

        retval = true;
        _deviceInfo.update(req.dataResponse);

        return retval;
    }

    /**
     * start scanning at slower rate and update buffer until global stop flag is set
     * @return
     */
    public RPLidarScanPoint scan(){
        if (!isOpen()) return null;

        RplRequest req = getRplRequest(RequestTypes.SCAN);
        if(req==null) return null;

        if (!doRplRequest(req)) {
            return null;
        }
        return new RPLidarScanPoint(req.dataResponse);
    }

    public synchronized RplidarDeviceSampleRate getSampleRate(){
        if (!isOpen()) return null;

        RplRequest req = getRplRequest(RequestTypes.GET_SAMPLERATE);
        if(req==null) return null;

        if (!doRplRequest(req)) {
            return null;
        }
        return new RplidarDeviceSampleRate(req.dataResponse);
    }

    public synchronized boolean startScan(){
        if (!isOpen()) return false;
        if (!stop()) return false;

        RplRequest req = getRplRequest(RequestTypes.SCAN);
        if(req==null) return false;

        return waitByteString(req.resDesc,500);
    }

    public synchronized boolean startForceScan(){
        String TAG = "RpLidar.startForcecan";
        Log.d(TAG,"Starting...");

        if (!isOpen()) return false;
        if (!stop()) return false;

        RplRequest req = getRplRequest(RequestTypes.FORCE_SCAN);
        if(req==null) return false;

        _busy = true;
        if (!doRplRequest(req)){
            _busy = false;
            Log.d(TAG,"FAILED doRplRequesg");
            return false;
        }

        grabScanPoints();
        Log.d(TAG,"Exiting...");

        return true;

    }

    private void grabScanPoints(){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                RPLidarScanPoint scanPoint = null;
                while(!_stopRequest){
                    scanPoint = waitScanPoint();
                    if (scanPoint == null) {
                        break;
                    }
                    Log.d("RpLidar.grabScanPoints","ScanPoint: "+scanPoint.angle+" "+scanPoint.distance);
                }
                _busy = false;
            }
        });
        thread.setName("thdGrabScanPoints");
        _thdGrabScanPoints = thread;
        thread.start();
    }

    public synchronized boolean startExpressScan(){
        if (!isOpen()) return false;
        if (!stop()) return false;

        RplRequest req = getRplRequest(RequestTypes.EXPRESS_SCAN);
        if(req==null) return false;

        return waitByteString(req.resDesc,500);
    }

    public synchronized RPLidarScanPoint waitScanPoint(){
        int timeout = 500;
        byte[] b = captureNBytes(5, timeout);
        if(b == null) return null;

        return new RPLidarScanPoint(b);
    }

    /**
     * Sends command, and waits for response if one is defined
     * @param req
     * @return
     */
    boolean doRplRequest(RplRequest req) {
        String TAG = "RpLidar.doRplRequest";
        Log.d(TAG,"Starting...");
        boolean retval = false;

        //place request in out queue
        try {
            Log.d(TAG,"Writing :"+bytesToHex(req.reqPacket));
            _outputStream.write(req.reqPacket);
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("TAG", "Return, Write Fail - Request: "+req.name.toString());
            return retval;
        }

        //return if no response is expected
        if (req.resDesc == null) {
            retval = true;
            Log.d(TAG,"Return - No response expected");
            return retval;
        }

        Thread.yield();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //wait for response header
        if (!waitByteString(req.resDesc, 5000)) {
            retval = false;
            Log.d(TAG,"Return - waitByteStringFailed");
            return retval;
        }

        //multiples handled by caller
        if(req.respMode == RespMode.MULTIPLE){
            Log.d(TAG,"Return - RespMode.MULTIPLE");
            return true;
        }

        //capture data response bytes
        req.dataResponse = null;
        Log.d(TAG,"Wait for response started");
        req.dataResponse = captureNBytes(req.respLen,500);
        if (req.dataResponse ==  null){
            retval = false;
            Log.d(TAG,"Return - waitForNBytes failed");
            return retval;
        }

        //extract data from response

        retval = true;
        Log.d(TAG,"Return - SUCCESS");
        return retval;
    }

    //from #define in RPTypes
    private boolean IS_FAIL(int i){
        return((RESULT_FAIL_BIT & i)!=0);
    }
    private boolean IS_OK(int i){
        return((RESULT_FAIL_BIT & i)==0);
    }


    /**
     * Monitors input queue until byteStr is received or untill timeout
     * @param byteStr   byte[] to be waiting for
     * @param timeoutMs milliseconds to complete before timeout failure
     * @return true if byteStre recieved before timeout, false otherwise
     */
    private synchronized boolean waitByteString(byte[] byteStr, int timeoutMs)
    {
        String TAG = "RpLidar.waitByteString";

        Log.d(TAG,"Starting...");
        int  recvPos = 0;
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Thread.yield(); //give queue time to fill
        timeoutMs *= 10;
        int strLen = byteStr.length;
        Byte currentByte = null;

        boolean retval = false;

        Log.d(TAG,"Before while...");
        try {
            Log.d(TAG,"..._inputStream.avail: "+_inputStream.available());
        } catch (IOException e) {
            e.printStackTrace();
        }
        long timeout = System.currentTimeMillis()+timeoutMs*4;

        while (System.currentTimeMillis() <= timeout) {       //for production
        //while (_inputStream.available()>0) {                        //for debugging
            Thread.yield();
            try {
                if(_inputStream.available()>0) {
                    currentByte = (byte) _inputStream.read();
                }
            } catch (IOException e) {
                Log.d(TAG,"read() Exception: "+e.getMessage());
                e.printStackTrace();
            }
            if (currentByte == null) continue;
            Log.d(TAG,"CurrentByte: "+currentByte+" ByteStr: "+byteStr[recvPos]);
            if (currentByte == byteStr[recvPos]){
                if(++recvPos == strLen){
                    retval = true;
                    Log.d(TAG,"Break: Match");
                    break;
                }
            }else
            {
                recvPos = 0; //no match, start over
            }
        }


        if(!retval){
                Log.d(TAG,"WaitByteString timeout");
        }else{
            Log.d(TAG,"WaitByteString success");
        }


        return retval;
    }

    private synchronized byte[] captureNBytes(int n, int timeoutMs){
        int  recvPos = 0;
        byte[] byteStr = new byte[n];
        Byte currentByte = null;

        byte[] retval = null;

        long timeout = System.currentTimeMillis()+timeoutMs;
        while (System.currentTimeMillis() <= timeout) {
            Thread.yield();
            try {
                if(_inputStream.available()>0) {
                    currentByte = (byte) _inputStream.read();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (currentByte == null) continue;
            byteStr[recvPos] = currentByte;
            if(++recvPos == n){
                retval = byteStr;
                break;
            }
        }

        return retval;
    }

    class RplidarDeviceHealth {
        public byte/*_u8*/   status;
        public int/*_u16*/  error_code;

        RplidarDeviceHealth(){
            status = -1;
            error_code = -1;
        }

        RplidarDeviceHealth(byte[] hb){
            update(hb);
        }

        public void update(byte[] hb){
            status = hb[0];
            error_code = ((hb[2] << 8) | hb[1]);
        }
    }

    class RplidarDeviceInfo{
        public byte model;
        public byte firmware_minor;
        public byte firmware_major;
        public byte hardware;
        public String serialnumber;

        RplidarDeviceInfo(){
            model          = 0;
            firmware_minor = 0;
            firmware_major = 0;
            hardware       = 0;
            serialnumber = "";
        }

        RplidarDeviceInfo(byte[] b){
            update(b);
        }

        public void update(byte[] b){
            model          = b[0];
            firmware_minor = b[1];
            firmware_major = b[2];
            hardware       = b[3];
            serialnumber = bytesToHex(Arrays.copyOfRange(b,4,19));
        }

        public String toString(){
            //Firmware Ver 1.20, HW Ver 1
            //Mode: 24"

            String retval="Firmware Ver "+firmware_major+"."+firmware_minor+"\n";
            retval += "HW Ver "+hardware+"\n";
            retval += "Model "+model+"\n";
            retval += "Serial # "+serialnumber;

            return retval;
        }

    }

    class RplidarDeviceSampleRate{
        int  tStandard;
        int tExpress;

        RplidarDeviceSampleRate(byte[] b){
            tStandard = (b[1] << 8) | b[0];
            tExpress = (b[3] << 8) | b[2];
        }
    }

    private class RplRequest{
        RequestTypes name;
        byte reqPacket[];
        byte resDesc[];
        RespMode respMode;
        int respLen;
        byte[] dataResponse;

        public RplRequest(RequestTypes name, byte[] reqPacket, byte[] resDesc,
                          RespMode respMode, int respLen){

            this.name = name;
            this.reqPacket = reqPacket;
            this.resDesc = resDesc;
            this.respMode = respMode;
            this.respLen = respLen;
            if(respLen >0){
                dataResponse = new byte[respLen];
            }
        }
    }

    private void makeReqList() {

        rplReqList.add(new RplRequest(RequestTypes.STOP,
                new byte[]{(byte)0xA5,0x25},
                null,
                RespMode.NONE,
                0));

        rplReqList.add(new RplRequest(RequestTypes.RESET,
                new byte[]{(byte)0xA5,0x40},
                null,
                RespMode.NONE,
                0

                //undocumented response - may not be in future firmware
//                new byte[]{0x52, 0x50, 0x20, 0x4C, 0x49, 0x44, 0x41, 0x52, 0x20, 0x53, 0x79, 0x73,
//                        0x74, 0x65, 0x6D, 0x2E, 0x0D, 0x0A},
//                RespMode.SINGLE,
//                39
        ));

        //testing shows that after RESET, RPLIDAR returns
        //"RP LIDAR System.
        //Firmware Ver 1.20, HW Ver 1
        //Mode: 24"
        //or in Hex:
        //{52, 50, 20, 4C, 49, 44, 41, 52, 20, 53, 79, 73, 74, 65, 6D, 2E, 0D, 0A,
        // 46, 69, 72, 6D, 77, 61, 72, 65, 20, 56, 65, 72, 20, 31, 2E, 32, 30, 2C,
        // 20, 48, 57, 20, 56, 65, 72, 20, 31, 0D, 0A,
        // 4D, 6F, 64, 65, 3A, 20, 32, 34, 0D, 0A}

        rplReqList.add(new RplRequest(RequestTypes.SCAN,
                new byte[]{(byte) 0xA5, 0x20},
                new byte[]{(byte) 0xA5, 0x5A, 0x05, 0x00, 0x00, 0x40, (byte)0x81},
                RespMode.MULTIPLE,
                5));

        rplReqList.add(new RplRequest(RequestTypes.EXPRESS_SCAN,
                new byte[]{(byte) 0xA5, (byte) 0x82, 0x05, 0x00, 0x00, 0x00, 0x00, 0x00, 0x22},
                new byte[]{(byte) 0xA5, 0x5A, 0x54, 0x00, 0x00, 0x40, (byte)0x82},
                RespMode.MULTIPLE,
                84));

        rplReqList.add(new RplRequest(RequestTypes.FORCE_SCAN,
                new byte[]{(byte) 0xA5, 0x21},
                new byte[]{(byte) 0xA5, 0x5A, 0x05, 0x00, 0x00, 0x40, (byte)0x81},
                RespMode.MULTIPLE,
                5));

        rplReqList.add(new RplRequest(RequestTypes.GET_INFO,
                new byte[]{(byte) 0xA5, 0x50},
                new byte[]{(byte) 0xA5, 0x5A, 0x14, 0x00, 0x00, 0x00, 0x04},
                RespMode.SINGLE,
                20));

        rplReqList.add(new RplRequest(RequestTypes.GET_HEALTH,
                new byte[]{(byte) 0xA5, 0x52},
                new byte[]{(byte) 0xA5, 0x5A, 0x03, 0x00, 0x00, 0x00, 0x06},
                RespMode.SINGLE,
                3));

        rplReqList.add(new RplRequest(RequestTypes.GET_SAMPLERATE,
                new byte[]{(byte) 0xA5, 0x59},
                new byte[]{(byte) 0xA5, 0x5A, 0x04, 0x00, 0x00, 0x00, 0x15},
                RespMode.SINGLE,
                4));

    }

    RplRequest getRplRequest(RequestTypes name){
        for(RplRequest r:rplReqList){
            if (r.name ==name){
                return r;
            }
        }
        return null;
    }


    /**
     * Holds and parses response data from SCAN and FORCE_SCAN requests
     */
    class RPLidarScanPoint
    {
        public float distance;
        public float angle;
        public int quality;
        public boolean  startBit;

        RPLidarScanPoint(){
            distance = -1;
            angle = -1;
            quality = -1;
        }

        RPLidarScanPoint(byte[] b){
            update(b);
        }

        public void update(byte[] b){
            quality = b[0] >> 2;
            startBit = (b[0] | 0xFE) == 0x01;
            angle = (int)(((b[1] >> 1) & (b[2] << 7)))/64.0f;
        }
    }

    class RPLidarXScanPointSet{
        public int sync1;
        public int sync2;
        public int checksum;
        public float startAngle;
        boolean startBit;
        Cabin[] cabins;
    }

    class Cabin{
        float distance1;
        float distance2;
        float angle1;
        float angle2;

        public void update(byte[] b){
            distance1 = (int)((b[0] >> 1) | ((b[1] << 7)))/4.0f;
            distance2 = (int)((b[2] >> 1) | ((b[3] << 7)))/4.0f;
            angle1 = (b[4] | 0x0F)/64.0f;
            angle2 = (b[4] >> 4)/64.0f;

        }
    }

    public boolean isconnected(){
        return _isconnected;
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Read any remaining data from the Scribbler and throw them out.
     */
    private void _flushInput()
    {
        String TAG = "RpLidar._flushInput";

        final int arrSize = 1000;   // assume no more than this many characters in response
        byte[] readBuffer = new byte[arrSize];

        try
        {
            while( _inputStream.available() > 0 )
                _inputStream.read( readBuffer );
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG,e.getLocalizedMessage());
        }
    }

    public boolean setLidarSpeed(float spd){
        boolean retval = true;
        if ((spd >= 0.0f) && (spd <= 1.0f)){
            _lidarSpeed = spd;
        }else{
            retval = false;
            Log.d("Error","Invalid Lidar Speed Request: "+spd+", must be between 0.0 and 1.0 inclusive");
        }
        return retval;
    }

    public float getlidarSpeed(){
        return _lidarSpeed;
    }
    public boolean isBusy(){ return _busy;}
    public void unBusy(){
        _busy = false;}

    public String getDeviceInfo(){
        return _deviceInfo.toString();
    }
    public void requestStop(){_stopRequest = true;}
 }
