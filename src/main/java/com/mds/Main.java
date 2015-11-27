package com.mds;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.swing.*;
import com.mds.Window;
import lombok.*;
import lombok.extern.slf4j.*;
import java.io.*;

@Slf4j
class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        new Main().runMapper();
    }

    private Window w;

    public void runMapper() throws IOException, InterruptedException {
        MapDatabase md = new MapDatabase();
        runGui(md);

        Thread.sleep(1000);

        log.info("indexing...");
        md.calcFreeRegionsSequentially("/tmp/test.dat");
        md.dump();
    }

    public void runGui(MapDatabase md) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                w = new Window();
                w.setChartState("indexing file");
                w.setMapDatabase(md);
                w.setVisible(true);
            }
        });
    }
}
