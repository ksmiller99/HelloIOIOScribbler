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
import android.os.Environment;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

/**
 * Implementation of MyroImage for storing color images.
 *
 * @author Douglas Harms
 * @version Septermber 2010
 */
public class MyroColorImage extends MyroImage
{
    /**
     * Create a 1x1 color image.  The pixel will be black.
     */
    public MyroColorImage()
    {
        this( 1, 1 );
    }

    /**
     * Create a color image of a specified dimension.  The initial color of all pixels is passed
     * as a parameter.
     *
     * @pre w > 0 and h > 0
     *
     * @param w The width of the color image
     * @param h The height of the color image
     * @param pixelColor The initial color of all pixels
     */
    public MyroColorImage(int w, int h, MyroColor pixelColor ) {
        assert 0 < w && 0 < h:"width and height must be > 0";

        width = w;
        height = h;
        image = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        imageType = Scribbler.IMAGE_COLOR;

        // set all pixels to pixelColor
        for( MyroPixel p : this )
        {
            p.setColor( pixelColor );
        }
    }

    /**
     * Create a color image of a specified dimension.  All pixels will be black.
     *
     * @pre w > 0 and h > 0
     *
     * @param w The width of the color image
     * @param h The height of the color image
     */
    public MyroColorImage(int w, int h )
    {
        this( w, h, new MyroColor(0,0,0) );
    }

    /**
     * Creates a color image from a stream of bytes that comprise a jpeg image.  A RuntimeException
     * is thrown if an error occurs decoding the byte stream.
     *
     * @param jpegBuf An array of bytes that comprise a jpeg image.
     */
    public MyroColorImage( byte[] jpegBuf )
    {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        image = BitmapFactory.decodeByteArray(jpegBuf, 0, jpegBuf.length, options);

        width  = image.getWidth();
        height = image.getHeight();
        imageType = Scribbler.IMAGE_COLOR;
    }

    /**
     * Load a color image from a file.  The dimension of the image will be the dimension
     * of the image in the file.
     *
     * @param filename The name of a file containing an image.
     *
     * @return true if the image was successfully loaded, false if an error occurred
     */
    public boolean loadImage( String filename ) {
        File root = Environment.getExternalStorageDirectory();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        image = BitmapFactory.decodeFile(root+filename);

        // check that image was read in
        if (image == null)
            return false;

        width  = image.getWidth();
        height = image.getHeight();
        imageType = Scribbler.IMAGE_COLOR;

        // all was good, so return true
        return true;
    }

    /**
     * Returns the RGB color of pixel (x,y).
     *
     * @pre (x,y) is the coordinate of a pixel in the image.
     *
     * @param x x xoordinate of the pixel
     * @param y y coordinate of the pixel
     * @return The color of pixel (x,y)
     */
    public MyroColor getColor(int x, int y)
    {
        assert 0<=x && x<width:"x out of range";
        assert 0<=y && y<height:"y out of range";

        int pixel = image.getPixel(x,y);
        return new MyroColor(Color.red(pixel),Color.green(pixel),Color.blue(pixel));
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

        return (int) lum( getColor( x, y ) );
    }

    /**
     * Sets the color of pixel (x,y) to an RGB color.
     *
     * @pre (x,y) is the coordinate of a pixel in the image, color is not null.
     *
     * @param x x xoordinate of the pixel
     * @param y y coordinate of the pixel
     * @param c An RGB Color that pixel (x,y) is set to.
     */
    public void setColor(int x, int y, MyroColor c)
    {
        assert 0<=x && x<width:"x out of range";
        assert 0<=y && y<height:"y out of range";
        assert c!=null : "Color must be non-null";

        image.setPixel(x, y, c.getARGB());
    }

    /**
     * Sets the color of pixel (x,y) to a grayscale color.
     *
     * @pre (x,y) is the coordinate of a pixel in the image, grayLevel is between 0 (inclusive)
     * and 255 (inclusive)
     *
     * @param x x xoordinate of the pixel
     * @param y y coordinate of the pixel
     * @param grayLevel A grayscale value between 0 and 255 that pixel (x,y) is set to.
     */
    public void setGray( int x, int y, int grayLevel)
    {
        assert 0<=x && x<width:"x out of range";
        assert 0<=y && y<height:"y out of range";
        assert 0<=grayLevel && grayLevel<=255 : "Grayscale value out of 0..255 range";

        setColor( x, y, new MyroColor( grayLevel, grayLevel, grayLevel ) );
    }
}
