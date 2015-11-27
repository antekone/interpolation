package com.mds;

import lombok.*;
import lombok.extern.slf4j.*;
import java.util.*;
import java.io.*;
import java.util.concurrent.*;

@Slf4j
public class MapDatabase {
    private HashMap<Integer, List<MapEntryZeroes>> map;
    private Semaphore lock = new Semaphore(1);

    MapDatabase() {
        map = new HashMap<>();
        map.put(1 * 1024 * 1024, new ArrayList<MapEntryZeroes>());
        map.put(2 * 1024 * 1024, new ArrayList<MapEntryZeroes>());
    }

    public List<MapEntryZeroes> getArrayForZoomLevel(int zl) throws InterruptedException {
        lock.acquire();
        return map.get(zl);
    }

    public void putArrayForZoomLevel(int zl) throws InterruptedException {
        lock.release();
    }

    public boolean calcFreeRegionsSequentially(String filename) throws IOException, InterruptedException {
        log.info("Will map this file: {}", filename);
        File f = new File(filename);
        long fileSize = f.length();
        long origFileSize = fileSize;

        log.info("File size: {}", fileSize);

        int regionCount = 25;
        long chunkSize = fileSize / regionCount;
        long regionOffset = 0;
        long regionEndOffset = 0;
        int readSize = 1 * 1024 * 1024;

        for(int i = 0; i < regionCount; i++) {
            regionEndOffset = regionOffset + chunkSize;
            regionEndOffset = Math.min(regionEndOffset, fileSize);

            log.info("Region {}, range {}-{}", i, regionOffset, regionEndOffset, readSize);
            regionOffset = regionEndOffset + 1;
        }

        byte[] buffer = new byte[readSize];

        long processed = 0;
        FileInputStream fis = new FileInputStream(f);

        int state = 0;
        long count2 = 0;
        long oldProcessed = 0;

        while(fileSize > 0) {
            int ret = fis.read(buffer);
            if(readSize != ret) {
                if(processed + ret == origFileSize) {
                    log.info("Done!");
                    return true;
                } else {
                    log.error("fis.read() returned {}, but it should return {}.", ret, readSize);
                    return false;
                }
            }

            fileSize -= readSize;

            int emptyBytes = calcEmptyBytes(buffer);
            int fillPrc = emptyBytes * 100 / readSize;

            lock.acquire();
            List<MapEntryZeroes> lst0 = map.get(readSize);
            List<MapEntryZeroes> lst1 = map.get(2 * readSize);

            MapEntryZeroes mez = new MapEntryZeroes();
            mez.setOffset(processed);
            mez.setCount(emptyBytes);
            lst0.add(mez);

//            log.info("added mez: {} {}", processed, emptyBytes);

            if(state == 0) {
                count2 = emptyBytes;
                oldProcessed = processed;
                state++;
            } else if(state == 1) {
                count2 += emptyBytes;

                MapEntryZeroes mez2 = new MapEntryZeroes();
                mez2.setOffset(oldProcessed);
                mez2.setCount(count2);
                lst1.add(mez2);
                state = 0;

//                log.info("added mez2: {} {}", oldProcessed, count2);
            }

            lock.release();

            processed += readSize;

//            log.info("lst0={}, lst1={}", lst0.size(), lst1.size());
        }

        return true;
    }

    private int calcEmptyBytes(byte[] buf) {
        int count = 0;
        for(int i = 0, len = buf.length; i < len; ++i) {
            if(buf[i] == 0)
                count++;
        }

        return count;
    }

    public void dump() {
        for(Integer zoomLevel: map.keySet()) {
            log.info("zoomLevel: {}", zoomLevel);

            for(final MapEntryZeroes mez: map.get(zoomLevel)) {
                long fill = 100 - (mez.getCount() * 100 / zoomLevel);
                log.info("entry offset={} size={} count={} fill={}%", mez.getOffset(), zoomLevel, mez.getCount(), fill);
            }
        }
    }
}
