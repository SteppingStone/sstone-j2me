/*
 * Copyright (c) 2012 EDC
 * 
 * This file is part of Stepping Stone.
 * 
 * Stepping Stone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Stepping Stone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Stepping Stone.  If not, see <http://www.gnu.org/licenses/gpl.txt>.
 */
package org.edc.sstone.j2me.ui.canvas;

import java.io.IOException;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import org.edc.sstone.log.Log;

import de.enough.polish.util.DeviceInfo;

/**
 * @author Greg Orlowski
 */
public class SplashScreen extends Canvas {

    private String imagePath;

    public SplashScreen(String imagePath) {
        this.imagePath = imagePath;

        // Set full screen mode to draw the splash screen across the
        // full screen and also properly initialize the full screen dimensions
        setFullScreenMode(true);
    }

    protected void paint(Graphics g) {
        setFullScreenMode(true);

        int white = 0xFFFFFF;
        g.setColor(white);
        int w = getWidth();
        int h = getHeight();
        g.fillRect(0, 0, w, h);

        int top = h / 5;

        Image image = null;
        try {
            image = Image.createImage(imagePath);
        } catch (IOException e) {
            Log.warn("Could not create splashscreen image: " + imagePath, e);
        }

        if (image != null) {
            int anchor = Graphics.HCENTER | Graphics.TOP;
            int x = w / 2;
            int y = top;
            int vendor = DeviceInfo.getVendor();
            
            // TODO: add an isEmulator() to DeviceInfo
            if (image.getHeight() > 120 && vendor != DeviceInfo.VENDOR_MICROEMU && vendor != DeviceInfo.VENDOR_SUN_WTK) {
                x = 0;
                y = 0;
                anchor = Graphics.LEFT | Graphics.TOP;
            }
            g.drawImage(image, x, y, anchor);
        } else {
            g.setColor(0x444444);
            g.drawString("Stepping Stone", w / 2, top, Graphics.HCENTER | Graphics.TOP);
        }

    }

}
