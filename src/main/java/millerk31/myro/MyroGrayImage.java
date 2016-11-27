/*
 * Myro/Java license - GPL
 *
 * Myro/Java is a Java implementation of the Myro API, defined by the Institute for Robots in
 * Education (IPRE).  See http://wiki.roboteducation.org for more information.
 *
 * Copyright 2010-2011 Douglas Harms dharms@depauw.edu
 *
 * This file is part of Myro/Java.
 *
 * Myro/Java is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * Myro/Java is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Myro/Java.  If not, see <http://www.gnu.org/licenses/>.
 */
package millerk31.myro;

/**
 * Created by Kevin on 11/5/2016.
 */

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Environment;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import java.lang.Object;
import java.net.URL;

/**
 * Implementation of MyroImage for storing grayscale images.
 *
 * @author Douglas Harms
 * @version Septermber 2010
 */

public class MyroGrayImage extends MyroImage
{
    /**
     * Create a 1x1 grayscale image.  Normally an image will be loaded into this image using
     * loadImage.
     */
    public MyroGrayImage()
    {
        this( 1, 1 );
    }

    /**
     * Create an empty grayscale image of a specified dimension.  The initial grayscale color of all
     * pixels is passed as a parameter.
     *
     * @pre w > 0 and h > 0, 0 &lt;= gray &lt;=255
     *
     * @param w The width of the color image
     * @param h The height of the color image
     * @param gray The initial grayscale level for all pixels
     */
    public MyroGrayImage(int w, int h, int gray)
    {
        assert 0 < w && 0 < h:"width and height must be > 0";

        image = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        width = w;
        height = h;
        imageType = Scribbler.IMAGE_GRAY;

        // set all pixels to the initial grayscale level
        for( MyroPixel p : this )
        {
            p.setGray( gray );
        }
    }

    /**
     * Create an empty grayscale image of a specified dimension.  All pixels will initially be black.
     *
     * @pre w > 0 and h > 0
     *
     * @param w The width of the color image
     * @param h The height of the color image
     */
    public MyroGrayImage(int w, int h)
    {
        this( w, h, 0 );
    }

    /**
     * Creates a grayscale image from a stream of bytes that comprise a jpeg image.  A RuntimeException
     * is thrown if an error occurs decoding the byte stream.
     *
     * @param jpegBuf An array of bytes that somprise a jpeg image.
     */
    public MyroGrayImage( byte[] jpegBuf )
    {
        Bitmap colorImage;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        colorImage = BitmapFactory.decodeByteArray(jpegBuf, 0, jpegBuf.length, options);

        width  = colorImage.getWidth();
        height = colorImage.getHeight();
        imageType = Scribbler.IMAGE_GRAY;


        // convert to grayscale
        int w = colorImage.getWidth();
        int h = colorImage.getHeight();
        image = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                int pixel = colorImage.getPixel(i, j);
                MyroColor color = new MyroColor(Color.red(pixel),Color.green(pixel),Color.blue(pixel));
                MyroColor gray = toGray(color);
                image.setPixel(i, j, gray.getARGB());
            }
        }

    }

    /**
     * Load a grayscale image from a file.  The dimension of the image will be the dimension
     * of the image in the file.
     *
     * @param filename The name of a file containing an image.
     *
     * @return true if the image was successfuly loaded, false if an error occurred
     */
    public boolean loadImage( String filename ) {
        Bitmap colorImage = null;
        File root = Environment.getExternalStorageDirectory();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;

        // try to read from file in working directory
        colorImage = BitmapFactory.decodeFile(root+filename);

        // check that image was read in
        if (colorImage == null)
            return false;

        width = colorImage.getWidth();
        height = colorImage.getHeight();
        imageType = Scribbler.IMAGE_GRAY;


        // convert to grayscale
        int w = colorImage.getWidth();
        int h = colorImage.getHeight();
        image = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                int pixel = colorImage.getPixel(i,j);
                MyroColor color = new MyroColor(Color.red(pixel), Color.green(pixel), Color.blue(pixel));
                MyroColor gray = toGray(color);
                image.setPixel(i, j, gray.getARGB());
            }
        }

        return true;
    }

    /**
     * Returns the RGB grayscale color of pixel (x,y).  All three components of the color will be
     * equal (because it's a grayscale).
     *
     * @pre (x,y) is the coordinate of a pixel in the image.
     *
     * @param x x xoordinate of the pixel
     * @param y y coordinate of the pixel
     * @return The grayscale color of pixel (x,y)
     */
    public MyroColor getColor(int x, int y) {
        assert 0<=x && x<width:"x out of range";
        assert 0<=y && y<height:"y out of range";

        int pixel = image.getPixel(x,y);
        MyroColor color = new MyroColor(Color.red(pixel),Color.green(pixel), Color.blue(pixel));
        return toGray(color);
    }

    /**
     * Returns the grayscale value of pixel (x,y).
     *
     * @pre (x,y) is the coordinate of a pixel in the image.
     *
     * @param x x xoordinate of the pixel
     * @param y y coordinate of the pixel
     * @return The grayscale value of pixel (x,y), in the range 0..255
     */
    public int getGray(int x, int y)
    {
        assert 0<=x && x<width:"x out of range";
        assert 0<=y && y<height:"y out of range";

        int pixel = image.getPixel(x,y);
        MyroColor color = new MyroColor(Color.red(pixel),Color.green(pixel),Color.blue(pixel));
        return color.getRed();
    }

    /**
     * Sets the color of pixel (x,y) to a grayscale color.  The color will be the grayscale color
     * of the color passed to this method.
     *
     * @pre (x,y) is the coordinate of a pixel in the image, color is not null.
     *
     * @param x x xoordinate of the pixel
     * @param y y coordinate of the pixel
     * @param c An RGB Color.  Pixel (x,y) will be set to the grayscale value of this color.
     */
    public void setColor(int x, int y, MyroColor c)
    {
        assert 0<=x && x<width:"x out of range";
        assert 0<=y && y<height:"y out of range";
        assert c!=null : "Color must be non-null";

        MyroColor gray = toGray(c);
        image.setPixel(x,y,gray.getARGB());
    }

    /**
     * Sets the color of pixel (x,y) to a grayscale color.
     *
     * @pre (x,y) is the coordinate of a pixel in the image, grayLevel is between 0 (inclusive)
     * and 255 (inclusive).
     *
     * @param x x xoordinate of the pixel
     * @param y y coordinate of the pixel
     * @param grayLevel A grayscale value between 0 and 255 that pixel (x,y) is set to.
     */
    public void setGray(int x, int y, int grayLevel)
    {
        assert 0<=x && x<width:"x out of range";
        assert 0<=y && y<height:"y out of range";
        assert 0<=grayLevel && grayLevel<=255 : "Grayscale value out of 0..255 range";

        MyroColor gray = new MyroColor( grayLevel, grayLevel, grayLevel );
        image.setPixel(x, y, gray.getARGB() );
    }

}
