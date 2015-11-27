package com.mds;

import java.awt.*;
import javax.swing.*;

import com.mds.Log;

import lombok.*;

public class Window extends JFrame {
    private Canvas canvas;
    private Chart chart = null;
    private MapDatabase mapDatabase;

    public Window() {
        setPreferredSize(new Dimension(640, 480));
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        chart = new Chart();
        chart.setMd(mapDatabase);
        add(chart);

//        canvas = new Canvas();
//        add(canvas);
        pack();
    }

    public void setChartState(String s) {
        if(chart != null)
            chart.setCurState(s);
    }

    public void setMapDatabase(MapDatabase md) {
        if(chart != null)
            chart.setMd(md);
    }
}
