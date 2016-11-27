package millerk31.ioio.scribbler.test;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by Kevin on 10/21/2016.
 */

public class MyPanel extends View {
    private Path drawPath;
    private Paint drawPaint;
    private Paint canvasPaint;
    private Canvas drawCanvas;
    private Bitmap canvasBitmap;


    public MyPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupCanvas();
    }

    @Override
    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        canvas.drawBitmap(canvasBitmap,0,0, canvasPaint);
        canvas.drawPath(drawPath,drawPaint);
    }

    public void setupCanvas(){
        drawPaint = new Paint();
        drawPath = new Path();
        drawPaint.setARGB(255, 0, 0, 255);
        canvasPaint = new Paint(Paint.DITHER_FLAG);
    }

    public void erase(){
        int w = canvasBitmap.getWidth();
        int h = canvasBitmap.getHeight();
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        drawCanvas = new Canvas(canvasBitmap);
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh){
        super.onSizeChanged(w, h, oldw, oldh);
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        drawCanvas = new Canvas(canvasBitmap);
    }
    @Override
    public boolean onTouchEvent(MotionEvent event){
        float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                drawPath.moveTo(touchX, touchY);
                break;

            case MotionEvent.ACTION_MOVE:
                drawPath.lineTo(touchX, touchY);
                break;

            case MotionEvent.ACTION_UP:
                drawCanvas.drawPath(drawPath, drawPaint);
                drawPath.reset();
                break;

            default:
                return false;
        }
        invalidate();
        return true;
    }
}
