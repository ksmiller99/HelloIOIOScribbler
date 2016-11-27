package millerk31.myro;

/**
 * Created by Kevin on 11/5/2016.
 * Myro Java uses AWT, but Android cannot. This class simulates awt.Color
 */

public class MyroColor {

    //each as 255 max value
    public int red;
    public int green;
    public int blue;

    MyroColor(int r, int g, int b){
        red = r;
        green = g;
        blue = b;
    }

    public int getBlue() {
        return blue;
    }

    public int getGreen() {
        return green;
    }

    public int getRed() {
        return red;
    }

    public void setBlue(int blue) {
        this.blue = blue;
    }

    public void setGreen(int green) {
        this.green = green;
    }

    public void setRed(int red) {
        this.red = red;
    }

    public int getARGB(){
        int c = 0xFF | (red << 16) | (green << 8) | blue;
        return c;
    }
}
