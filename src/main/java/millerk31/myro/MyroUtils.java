package millerk31.myro;

/**
 * Miscellaneous methods for Myro/Java programs
 *
 * @author Douglas Harms
 * @version 1.0
 */
import java.util.Random;

public class MyroUtils
{

    private static Random _randomSeq;
    private static boolean _newCountDown;
    private static long _startTime;

    // static constructor
    static
    {
        // initialize random number sequence
        _randomSeq = new Random();

        // initialize timeRemaining
        _newCountDown = true;

     }

    /**
     * Cause the current thread to sleep for numSeconds.
     *
     * @pre numSeconds >= 0.0
     *
     * @param numSeconds The length of time to sleep.
     */
    public static void sleep( double numSeconds )
    {
        assert numSeconds >= 0.0 : "numSeconds must be >= 0.0";
        try
        {
            Thread.sleep( (int)(numSeconds * 1000.0) );
        } catch (InterruptedException e) {}

    }

    /**
     * Returns a random integer within a specified range.
     *
     * @pre low &lt= high
     *
     * @param low Low end of range
     * @param high High end of range
     * @return A uniformly distributed random int between low (inclusive) and high (inclusive)
     */
    public static int randomInt( int low, int high )
    {
        assert low <= high : "low cannot be greater than high";

        return _randomSeq.nextInt( high-low+1 ) + low ;
    }

    /**
     * Returns a random double in the range 0.0 (inclusive) and 1.0 (exclusive).
     *
     * @return A uniformly distributed random double between 0.0 (inclusive) and 1.0 (exclusive)
     */
    public static double randomDouble( )
    {
        return _randomSeq.nextDouble();
    }

    /**
     * Controls a while-loop for a specific number of seconds.
     *
     * @param seconds number of seconds to loop
     * @return true iff the specified number of seconds has not elapsed
     */
    public static boolean timeRemaining( double seconds )
    {
        if( _newCountDown )
        {
            _startTime = System.currentTimeMillis();
            _newCountDown = false;
        }

        if( System.currentTimeMillis() <= _startTime+seconds*1000 )
            return true;
        else
        {
            _newCountDown = true;
            return false;
        }
    }

}
