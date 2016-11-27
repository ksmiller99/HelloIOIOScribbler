package millerk31.rplidar;

/**
 * Created by Kevin on 10/24/2016.
 */

public class RPLidar_cmd {

    // Commands without payload and response
    public static final byte RPLIDAR_CMD_STOP              =  0x25;
    public static final byte RPLIDAR_CMD_SCAN              =  0x20;
    public static final byte RPLIDAR_CMD_FORCE_SCAN        =  0x21;
    public static final byte RPLIDAR_CMD_RESET             =  0x40;

    // Commands without payload but have response
    public static final byte RPLIDAR_CMD_GET_DEVICE_INFO   =  0x50;
    public static final byte RPLIDAR_CMD_GET_DEVICE_HEALTH =  0x52;

    public static class RPLidar_resp{
        public static final byte RPLIDAR_ANS_TYPE_MEASUREMENT            = (byte)0x81;
        public static final byte RPLIDAR_ANS_TYPE_DEVINFO                = 0x04;
        public static final byte RPLIDAR_ANS_TYPE_DEVHEALTH              = 0x06;
        public static final byte RPLIDAR_STATUS_OK                       = 0x00;
        public static final byte RPLIDAR_STATUS_WARNING                  = 0x01;
        public static final byte RPLIDAR_STATUS_ERROR                    = 0x02;
        public static final byte RPLIDAR_RESP_MEASUREMENT_SYNCBIT        = (0x1<<0);
        public static final short RPLIDAR_RESP_MEASUREMENT_QUALITY_SHIFT = 2;
        public static final byte RPLIDAR_RESP_MEASUREMENT_CHECKBIT       = (0x1<<0);
        public static final short RPLIDAR_RESP_MEASUREMENT_ANGLE_SHIFT   = 1;

    }

    public class _rplidar_response_measurement_node_t {
        byte /*_u8 */   sync_quality;      // syncbit:1;syncbit_inverse:1;quality:6;
        short/*_u16*/   angle_q6_checkbit; // check_bit:1;angle_q6:15;
        short/*_u16*/   distance_q2;
    }


}


