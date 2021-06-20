/*
 * This file is part of MOS
 * <p>
 * Copyright (c) 2021 by cooder.org
 * <p>
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package org.cooder.mos.device;

import java.io.IOException;
import java.io.RandomAccessFile;

import org.cooder.mos.fs.fat16.Layout;

public class FileDisk implements IDisk {

    private RandomAccessFile raf;

    public FileDisk(String path) throws IOException {
        raf = new RandomAccessFile(path, "rwd");
        long capacity = capacity();
        if (raf.length() != capacity) {
            raf.setLength(capacity);
        }
    }

    @Override
    public int sectorSize() {
        return Layout.PER_SECTOR_SIZE;
    }

    @Override
    public int sectorCount() {
        return (int) (capacity() / sectorSize());
    }

    @Override
    public long capacity() {
        return 2 * 1024 * 1024 * 1024L;  // 2G
    }
    
    @Override
    public void readSector(int sectorIdx, byte[] buffer) {
        byte[] data = readSector(sectorIdx);
        System.arraycopy(data, 0, buffer, 0, buffer.length);
    }

    @Override
    public byte[] readSector(int sectorIdx) {
        byte[] buffer = new byte[sectorSize()];
        int pos = sectorIdx * sectorSize();
        try {
            raf.seek(pos);
            int ret = raf.read(buffer);
            if (ret < 0) {
                throw new IllegalStateException();
            }
            return buffer;
        } catch (IOException e) {
            throw new IllegalStateException();
        }
    }

    @Override
    public void writeSector(int sectorIdx, byte[] sectorData) {
        int pos = sectorIdx * sectorSize();
        try {
            raf.seek(pos);
            raf.write(sectorData);
        } catch (IOException e) {
            throw new IllegalStateException();
        }
    }

    @Override
    public void clear() throws IOException {
        raf.setLength(0);
        raf.setLength(capacity());
    }
    
    @Override
    public void close() throws IOException {
        raf.close();
    }
}
