package com.mds;

import com.mds.Log;

import java.util.*;
import javax.swing.*;
import java.awt.*;

public class Canvas extends JPanel {
    Canvas() {
        arrayToDraw = new int[4096];
        Random r = new Random();
        r.setSeed(123);
        for(int i = 0; i < arrayToDraw.length; i++) {
            arrayToDraw[i] = (i + Math.abs(r.nextInt()) % 50) % 255;
        }
    }

    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;
        g.setColor(Color.black);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Color.white);
        g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        drawArrayStream(g);
    }

    private int streamPositionToX(int spos) {
        int boxWidth = 8;
        int boxHeight = 8;

        int x = spos * boxWidth;
        while(x > getWidth()) {
            x -= getWidth();
        }

        return x;
    }

    private int streamPositionToY(int spos) {
        int boxWidth = 8;
        int boxHeight = 8;

        int y = 0;
        int x = spos * boxWidth;
        while(x > getWidth()) {
            x -= getWidth();
            y += boxHeight;
        }

        return y;
    }

    private int[] streamPositionToHilbertXY(int n, int spos) {
        int t = spos;
        int x = 0;
        int y = 0;

        for(int s = 1; s < n; s *= 2) {
            int rx = 1 & (t / 2);
            int ry = 1 & (t ^ rx);
            int[] rotret = rot(s, x, y, rx, ry);
            x = rotret[0];
            y = rotret[1];
            x += s * rx;
            y += s * ry;
//            Log.log("spos=%d,rx=%d, ry=%d, x=%d, y=%d, t=%d, t^rx=%d", spos, rx, ry, x, y, t, t^rx);
            t /= 4;
        }

        return new int[] { x * 8, y * 8 };
    }

    private int[] rot(int n, int x, int y, int rx, int ry) {
        if(ry == 0) {
            if(rx == 1) {
                x = n-1 - x;
                y = n-1 - y;
            }

            int t = x;
            x = y;
            y = t;
        }

        return new int[] { x, y };
    }

    private void drawArrayStream(Graphics2D g) {
        for(int i = 0; i < arrayToDraw.length; ++i) {
            int value = arrayToDraw[i];

            Color c = new Color(value, 0, 0);
            g.setColor(c);

//            int x = streamPositionToX(i);
//            int y = streamPositionToY(i);
            int x, y;
            int[] ret = streamPositionToHilbertXY(256, i);
            x = ret[0];
            y = ret[1];
//            Log.log("x=%d, y=%d", x, y);
            g.fillRect(2 + x, 2 + y, 8, 8);
        }
    }

    private int[] arrayToDraw;
}
