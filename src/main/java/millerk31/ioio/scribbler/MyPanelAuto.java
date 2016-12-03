package millerk31.ioio.scribbler;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import millerk31.rplidar.RpLidar;

/**
 * Created by Kevin on 10/22/2016.
 */

public class MyPanelAuto extends View {
    Paint paint = new Paint();
    private int canvasW;
    private int canvasH;
    private int canvasCX;
    private int canvasCY;
    private double lenScale;
    private double angScale = 2*Math.PI/23040d;
    private double len;
    private int drawIndex = 0;

    public MyPanelAuto(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvasW = getWidth();
        canvasH = getHeight();
        canvasCX = canvasW / 2;
        canvasCY = canvasH / 2;
        lenScale = (float)Math.min(canvasCX, canvasCY) / 65536.0f;

        //canvas.drawARGB(255, 255, 255, 0);
        paint.setARGB(255, 0, 0, 255);
        double angle;
        int x, y;
        for (int i = 0; i < 23040; i+=100) {
            angle = (double)i * angScale;
            len = (RpLidar.scanData[i]* lenScale);
            x = (int) (len * Math.cos(angle))+canvasCX;
            y = (int) (len * Math.sin(angle))+canvasCY;
            //Log.d("angle","i: "+i+", angle: "+angle+", len: "+len+", x: "+x+", y: "+y);
            canvas.drawLine(
                    canvasCX,
                    canvasCY,
                    x,
                    y,
                    paint);
//            canvas.drawCircle(
//                    x,
//                    y,
//                    2,
//                    paint);
        }

        invalidate();
    }

}
