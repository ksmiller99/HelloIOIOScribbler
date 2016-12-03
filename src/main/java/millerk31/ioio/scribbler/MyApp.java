package millerk31.ioio.scribbler;

import android.app.Application;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import millerk31.myro.Scribbler;
import permissionrequest.SimpleWebServer;


/**
 * Created by Kevin on 10/6/2016.
 */

public class MyApp extends Application {
    SimpleWebServer sws;
    public static String tvRxDataSave1 = "";
    public static String tvRxDataSave2 = "";

    public static Thread thdIoioService = null;
    public static Thread thdLidarService = null;
    public static Thread thdScribblerService = null;

    @Override
    public void onCreate() {
        super.onCreate();
        sws = new SimpleWebServer(8080, this.getAssets());
        sws.start();
    }
}
