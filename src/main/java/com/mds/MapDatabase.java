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
    private @Getter long maxFileSize = 0;

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

    public long getCountForOffset(int zoomLevel, long offset) throws InterruptedException {
        long offs = Long.MAX_VALUE;
        List<MapEntryZeroes> lst = getArrayForZoomLevel(zoomLevel);
        MapEntryZeroes foundItem = null;

        for(MapEntryZeroes item: lst) {
            long offsetDifference = Math.abs(offset - item.getOffset());
            if(offsetDifference < offs) {
                offs = offsetDifference;
                foundItem = item;
            }
        }

//        log.info("requested count for offset {}, but via interpolation returning count for offset {} = {}", offset, foundItem.getOffset(), foundItem.getCount());
        long count = foundItem.getCount();
        putArrayForZoomLevel(0);
        return count;
    }

    public boolean calcFreeRegionsNonSequentially(String filename) throws IOException, InterruptedException {
        log.info("Will map this file: {}", filename);

        File f = new File(filename);
        long fileSize = f.length();
        maxFileSize = fileSize;
        long origFileSize = fileSize;

        log.info("File size: {}", fileSize);

        int regionCount = 25;
        long chunkSize = fileSize / regionCount;
        long regionOffset = 0;
        long regionEndOffset = 0;
        int readSize = 2 * 1024 * 1024;

        long[] offsets = new long[regionCount];
        long[] originalOffsets = new long[regionCount];

        for(int i = 0; i < regionCount; i++) {
            offsets[i] = regionOffset;
            originalOffsets[i] = regionOffset;

            regionEndOffset = regionOffset + chunkSize;
            regionEndOffset = Math.min(regionEndOffset, fileSize);

            log.info("Region {}, range {}-{}", i, regionOffset, regionEndOffset, readSize);
            regionOffset = regionEndOffset + 1;
        }

        RandomAccessFile raf = new RandomAccessFile(f, "r");
        byte[] buffer = new byte[readSize];

        boolean readSth = false;
        do {
            log.info("--- next iteration ---");
            readSth = false;
            for(int i = 0; i < regionCount; i++) {
                if(offsets[i] == -1)
                    continue;

                readSth = true;
                log.info("offset {}, seeking to {}, reading {} bytes", i, offsets[i], buffer.length);

                raf.seek(offsets[i]);

                int toread = buffer.length;
                if(offsets[i] + toread > origFileSize) {
                    toread = (int) (origFileSize - offsets[i]);
                    log.info("fixed last buffer count to be {} bytes.", toread);
                }

                raf.readFully(buffer, 0, toread);

                Thread.sleep(11);

                MapEntryZeroes mez = new MapEntryZeroes();
                int count = calcEmptyBytes(buffer);
                mez.setOffset(offsets[i]);
                mez.setCount(count);

                List<MapEntryZeroes> lst = getArrayForZoomLevel(readSize);
                lst.add(mez);
                putArrayForZoomLevel(0);

                offsets[i] += readSize;

                if(i != regionCount - 1 && offsets[i] >= originalOffsets[i + 1]) {
                    log.info("offset no. {} is being turned off, because we already finished out chunk size.", i);
                    offsets[i] = -1;
                } else if(i == regionCount - 1 && offsets[i] >= origFileSize) {
                    log.info("last offset no. {} is being turned off.", i);
                    offsets[i] = -1;
                }
            }
        } while(readSth);

        return true;
    }

    public boolean calcFreeRegionsSequentially(String filename) throws IOException, InterruptedException {
        log.info("Will map this file: {}", filename);
        File f = new File(filename);
        long fileSize = f.length();
        long origFileSize = fileSize;
        maxFileSize = origFileSize;

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
            Thread.sleep(11);
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
