package millerk31.rplidar;

/**
 * Created by Kevin on 10/24/2016.
 */

public class RPTypes {
/*
    #pragma once


    #ifdef _WIN32

//fake stdint.h for VC only

    typedef signed   char     int8_t;
    typedef unsigned char     uint8_t;

    typedef __int16           int16_t;
    typedef unsigned __int16  uint16_t;

    typedef __int32           int32_t;
    typedef unsigned __int32  uint32_t;

    typedef __int64           int64_t;
    typedef unsigned __int64  uint64_t;

    #else

            #include <stdint.h>

    #endif


    //based on stdint.h
    typedef int8_t         _s8;
    typedef uint8_t        _u8;

    typedef int16_t        _s16;
    typedef uint16_t       _u16;

    typedef int32_t        _s32;
    typedef uint32_t       _u32;

    typedef int64_t        _s64;
    typedef uint64_t       _u64;

    #define __small_endian

    #ifndef __GNUC__
    #define __attribute__(x)
    #endif


    typedef uint32_t u_result;
*/
    public static final int RESULT_OK                    = 0;
    public static final int RESULT_FAIL_BIT              = 0x80000000;
    public static final int RESULT_ALREADY_DONE          = 0x20;
    public static final int RESULT_INVALID_DATA          = (0x8000 | RESULT_FAIL_BIT);
    public static final int RESULT_OPERATION_FAIL        = (0x8001 | RESULT_FAIL_BIT);
    public static final int RESULT_OPERATION_TIMEOUT     = (0x8002 | RESULT_FAIL_BIT);
    public static final int RESULT_OPERATION_STOP        = (0x8003 | RESULT_FAIL_BIT);
    public static final int RESULT_OPERATION_NOT_SUPPORT = (0x8004 | RESULT_FAIL_BIT);
    public static final int RESULT_FORMAT_NOT_SUPPORT    = (0x8005 | RESULT_FAIL_BIT);
    public static final int RESULT_INSUFFICIENT_MEMORY   = (0x8006 | RESULT_FAIL_BIT);
    public static final int RESULT_BUSY                  = (0x8007 | RESULT_FAIL_BIT);

    public static boolean IS_OK(int x){
         return((x & RESULT_FAIL_BIT) == 0 );
    }

    public static boolean IS_FAIL(int x){
        return ((x & RESULT_FAIL_BIT) !=0);
    }

    //typedef _word_size_t (THREAD_PROC * thread_proc_t ) ( void * );
}
