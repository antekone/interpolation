package com.mds;

import javax.swing.*;
import java.awt.*;
import java.awt.font.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;

import lombok.*;
import lombok.*;
import lombok.extern.slf4j.*;

@Slf4j
class Chart extends JPanel {
    private @Setter MapDatabase md;
    private javax.swing.Timer repaintTimer;
    private String curState;
    private volatile boolean currentlyPainting = false;

    public Chart() {
        final Chart self = this;
        setPreferredSize(new Dimension(800, 200));

        setCurState("waiting");

        ActionListener repainter = new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                self.repaint();
            }
        };

        repaintTimer = new javax.swing.Timer(500, repainter);
        repaintTimer.start();
    }

    private int counter = 0;
    public void paintComponent(Graphics g0) {
        if(currentlyPainting)
            return;

        currentlyPainting = true;

        try {
            Graphics2D g = (Graphics2D) g0;
            ++counter;

            g.setColor(Color.white);
            g.fillRect(0, 0, getWidth(), getHeight());

            FontRenderContext frc = g.getFontRenderContext();
            String text = String.format("frame %d", counter);
            Rectangle2D textBounds = g.getFont().getStringBounds(text, frc);

            g.setColor(Color.black);
            g.drawString(text, 2, 2 + (int) textBounds.getHeight());

            if(md != null) {
                drawLst(g);
            }
        } catch(InterruptedException e) {
        } finally {
            currentlyPainting = false;
        }
    }

    public void drawLst(Graphics2D g) throws InterruptedException {
        g.setColor(Color.black);

        if(md.getMaxFileSize() == 0)
            return;

        int slices = getWidth() / 5;
        for(int i = 0; i < slices; i++) {
            double prc = (double) i / slices;
            long off = (long) (prc * (double) md.getMaxFileSize());
            long count = md.getCountForOffset(2 * 1024 * 1024, off);

            long fill = 100 - (count * 100 / (2*1024*1024));
            g.fillRect(i*5, getHeight() - (int) fill, 5, (int) fill);

//            log.info("x={}-{}, slice={}/{}, prc={}, off={}, count={}, fill={}%", i*5, i*5+5, i, slices, prc, off, count, fill);
        }
    }

    public String getCurState() {
        String cs;
        synchronized(Chart.class) {
            cs = curState;
        }
        return cs;
    }

    public void setCurState(String s) {
        synchronized(Chart.class) {
            curState = s;
        }
    }
}
