package millerk31.rplidar;

import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import millerk31.ioio.scribbler.test.IOIOScribblerService;

//import static millerk31.rplidar.RPLidar_protocol.RPLIDAR_CMDFLAG_HAS_PAYLOAD;
//import static millerk31.rplidar.RPLidar_protocol.RPLIDAR_CMD_SYNC_BYTE;
import static millerk31.rplidar.RPTypes.RESULT_FAIL_BIT;


/**
 * Created by Kevin on 10/24/2016.
 */

public class RpLidar {
    private static RpLidar instance = null;

    public static final int MSG_CONNECT            = 0 ;
    public static final int MSG_STATUS_REQUEST     = 9 ;   //request LIDAR status
    public static final int MSG_STATUS_REPLY       = 10;  //arg1 == 1 if true
    public static final int MSG_STOP               = 11;  //request LIDAR status
    public static final int MSG_SCAN               = 12;  //request LIDAR status
    public static final int MSG_FORCE_SCAN         = 11;  //request LIDAR status
    public static final int MSG_RESET              = 12;  //request LIDAR status
    public static final int MSG_GET_INFO           = 11;  //request LIDAR status
    public static final int MSG_INFO_REPLY         = 12;  //request LIDAR status
    public static final int MSG_GET_HEALTH         = 11;  //request LIDAR status
    public static final int MSG_HEALTH_REPLY       = 12;  //request LIDAR status
    public static final int MSG_GET_SAMPLERATE     = 11;  //request LIDAR status
    public static final int MSG_SAMPLERATE_REPLY   = 12;  //request LIDAR status
    public static final int MSG_ON_REQUEST         = 13;  //request LIDAR status
    public static final int MSG_OFF_REQUEST        = 14;  //request LIDAR status
    public static final int MSG_ON_REPLY           = 15;  //arg1 == 1 if true
    public static final int MSG_OFF_REPLY          = 16;  //arg1 == 1 if true
    public static final int MSG_SPEED_REQ          = 17;  //arg1 == speed 0.0 - 1.0

    public static RplidarDeviceHealth healthInfo;
    public static RplidarDeviceInfo deviceInfo;

//    private Queue<Byte> inQueue;
//    private Queue<Byte> outQueue;

    public static Queue<Byte> outQueue;
    public static Queue<Byte> inQueue;

    private static boolean _isconnected = false;
    //private static boolean _isSupportingMotorCtrl = false;

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

    public static RpLidar getInstance(){
        if(instance == null){
            instance = new RpLidar();
        }
        return instance;
    }

    /**
     * Robopeak A2 Lidar object.
     */
    public RpLidar(){
        outQueue = new ConcurrentLinkedQueue<Byte>();
        inQueue = new ConcurrentLinkedQueue<Byte>();

        makeReqList();
        healthInfo = new RplidarDeviceHealth();
        deviceInfo = new RplidarDeviceInfo();
    }

    // open the given serial interface and try to connect to the RPLIDAR
    public int connect(){
        int retval = RPTypes.RESULT_OPERATION_FAIL;

        if(!IOIOScribblerService.ioio_state){
            _isconnected = false;
            return retval;
        }

        outQueue.clear();
        inQueue.clear();

        if (!reset()){
            return retval;
        }

        if(!getInfo()){
            return retval;
        }

        if(!getHealth()){
            return retval;
        }

        _isconnected = true;
        retval =  RPTypes.RESULT_OK;
        return retval;
    }

    // close the currently opened serial interface
    public void end()
    {
        //IOIO handles
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

        RplRequest req = getRplRequest(RequestTypes.STOP);
        if(req==null) return false;

        if (!doRplRequest(req)) {
            return false;
        }

        SystemClock.sleep(1);

        return true;
    }

    /**
     * Reset(reboot) the RPLIDAR core
     * No response is defined, but some testing shows some text is returned. See comments for details
     * @return true if command was placed in queue
     */
    public boolean reset(){
        if(!isOpen()) return false;

        RplRequest req = getRplRequest(RequestTypes.RESET);
        if(req==null) return false;

        if (!doRplRequest(req)) {
            return false;
        }

        long timeoutMs = System.currentTimeMillis()+500;
        while (System.currentTimeMillis() < timeoutMs){
            Thread.yield();
        }
        synchronized (inQueue) {
            inQueue.clear();
        }

        return true;
    }

    // ask the RPLIDAR for its health info
    public boolean getHealth(){
        boolean retval = false;
        if (!isOpen()) return retval;

        RplRequest req = getRplRequest(RequestTypes.GET_HEALTH);
        if(req==null) return retval;

        if (!doRplRequest(req)) {
            return retval;
        }

        retval = true;
        healthInfo.update(req.dataResponse);
        return retval;
    }

    public boolean getInfo(){
        boolean retval = false;
        if (!isOpen()) return retval;

        RplRequest req = getRplRequest(RequestTypes.GET_INFO);
        if(req==null) return retval;

        inQueue.clear();
        if (!doRplRequest(req)) {
            return retval;
        }

        retval = true;
        deviceInfo.update(req.dataResponse);

        return retval;
    }

    /**
     * start scanning at slower rate and update buffer until global stop flag is set
     * @return
     */
    public RplidarDeviceInfo scan(){
        if (!isOpen()) return null;

        RplRequest req = getRplRequest(RequestTypes.SCAN);
        if(req==null) return null;

        if (!doRplRequest(req)) {
            return null;
        }
        return new RplidarDeviceInfo(req.dataResponse);
    }

    public RplidarDeviceSampleRate getSampleRate(){
        if (!isOpen()) return null;

        RplRequest req = getRplRequest(RequestTypes.GET_SAMPLERATE);
        if(req==null) return null;

        if (!doRplRequest(req)) {
            return null;
        }
        return new RplidarDeviceSampleRate(req.dataResponse);
    }

    public boolean startScan(){
        if (!isOpen()) return false;
        if (!stop()) return false;

        RplRequest req = getRplRequest(RequestTypes.SCAN);
        if(req==null) return false;

        return waitByteString(req.resDesc,500);
    }

    public boolean startForceScan(){
        if (!isOpen()) return false;
        if (!stop()) return false;

        RplRequest req = getRplRequest(RequestTypes.FORCE_SCAN);
        if(req==null) return false;

        return waitByteString(req.resDesc,500);
    }

    public boolean startExpressScan(){
        if (!isOpen()) return false;
        if (!stop()) return false;

        RplRequest req = getRplRequest(RequestTypes.EXPRESS_SCAN);
        if(req==null) return false;

        return waitByteString(req.resDesc,500);
    }

    public boolean waitScanPoint(){
        int timeout = 500;
        byte[] b = captureNBytes(5, timeout);
        if(b == null) return false;

        //todo more processing
        return true;
    }

    /**
     * Sends command, and waits for response if one is defined
     * @param req
     * @return
     */
    boolean doRplRequest(RplRequest req) {
        boolean retval = false;

        //place request in out queue
        for (Byte b : req.reqPacket) {
            if (!outQueue.add(b)) {
                retval = false;
                return retval;
            }
        }

        //return if no response is expected
        if (req.resDesc == null) {
            retval = true;
            return retval;
        }

        Thread.yield();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //wait for response header
        if (!waitByteString(req.resDesc, 500)) {
            retval = false;
            return retval;
        }

        //capture data response bytes
        req.dataResponse = null;
        req.dataResponse = captureNBytes(req.respLen,500);
        if (req.dataResponse ==  null){
            retval = false;
            return retval;
        }

        //extract data from response

        retval = true;
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
    boolean waitByteString(byte[] byteStr, int timeoutMs)
    {
        int  recvPos = 0;
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Thread.yield(); //give queue time to fill
        timeoutMs *= 10;
        long timeout = System.currentTimeMillis()+timeoutMs;
        int strLen = byteStr.length;
        Byte currentByte;

        boolean retval = false;

        while (System.currentTimeMillis() <= timeout) {       //for production
        //while (!inQueue.isEmpty()) {                   //for debugging
            Thread.yield();
            currentByte = inQueue.poll();
            if (currentByte == null) continue;
            Log.d("RpLidar.java","CurrentByte: "+currentByte+" ByteStr: "+byteStr[recvPos]);
            if (currentByte == byteStr[recvPos]){
                if(++recvPos == strLen){
                    retval = true;
                    break;
                }
            }else
            {
                recvPos = 0; //no match, start over
            }
        }
        if(!retval){
                Log.d("RpLidar.java","WaitByteString timeout");
        }else{
            Log.d("RpLidar.java","WaitByteString success");
        }


        return retval;
    }

    byte[] captureNBytes(int n, int timeoutMs){
        int  recvPos = 0;
        long timeout = System.currentTimeMillis()+timeoutMs;
        byte[] byteStr = new byte[n];
        Byte currentByte;

        byte[] retval = null;

        while (System.currentTimeMillis() <= timeout) {
            currentByte = inQueue.poll();
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

    public void qNotify(){
        // TODO: 10/26/2016
        //object was notified of data in input queue
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

    public String getDeviceInfo(){
        //Firmware Ver 1.20, HW Ver 1
        //Model: 24"

        String retval = "Firmware Ver ";
        retval += (deviceInfo.firmware_major+"."+deviceInfo.firmware_minor+", ");
        retval += "HW Ver ";
        retval += (deviceInfo.hardware+"\n");
        retval += "Model: ";
        retval += (deviceInfo.model+"\n");
        retval += "Serial :";
        retval += (deviceInfo.serialnumber+"\n");

        return retval;
    }

}
