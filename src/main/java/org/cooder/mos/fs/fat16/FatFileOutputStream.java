/*
 * This file is part of MOS
 * <p>
 * Copyright (c) 2021 by cooder.org
 * <p>
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package org.cooder.mos.fs.fat16;

import java.io.IOException;
import java.io.OutputStream;

import org.cooder.mos.device.IDisk;

public class FatFileOutputStream extends OutputStream {
    private final byte[] zeroBuffer = new byte[Layout.PER_SECTOR_SIZE];
    private final byte[] buffer = new byte[Layout.PER_SECTOR_SIZE];
    private int pos = 0;
    private int count = 0;
    private int markCount = 0;
    private int currentClusterIdx;
    private int currentSectorIdx;
    private final IDisk disk;
    private final IFAT16 fat;
    private final DirectoryTreeNode node;

    public FatFileOutputStream(IDisk disk, IFAT16 fat, int startClusterIdx, int startSectorIdx, int pos, DirectoryTreeNode node) {
        this.disk = disk;
        this.currentClusterIdx = startClusterIdx;
        this.currentSectorIdx = startSectorIdx;
        this.fat = fat;
        this.pos = pos;
        this.node = node;
        
        disk.readSector(currentSectorIdx, buffer);
    }

    @Override
    public void write(int b) throws IOException {
        if (pos >= buffer.length) {
            flush();
            resetBuffer();
        }

        buffer[pos++] = (byte) (b & 0xFF);
        count++;
        markCount++;
    }
    
    @Override
    public void flush() {
        // flush data
        disk.writeSector(currentSectorIdx, buffer);
        
        // update file entry
        int fileSize = markCount + node.getFileSize();
        node.setFileSize(fileSize);
        node.setWriteTime(System.currentTimeMillis());
        
        fat.writeDirectoryTreeNode(node);
        
        markCount = 0;
    }
    
    @Override
    public void close() {
        flush();
    }
    
    public int getCount() {
        return count;
    }

    private void resetBuffer() {
        int next = nextSector();
        if (next < 0) {
            throw new IllegalStateException("low disk space");
        }
        currentSectorIdx = next;
        System.arraycopy(zeroBuffer, 0, buffer, 0, buffer.length);
        pos = 0;
    }

    private int nextSector() {
        int next = -1;
        if (currentSectorIdx == lastSectorIdx(currentClusterIdx)) {
            int nextCluster = fat.nextFreeCluster(currentClusterIdx);
            if (nextCluster < 0) {
                return -1;
            }
            
            currentClusterIdx = nextCluster;
            next = firstSectorIdx(currentClusterIdx);
        } else {
            next = currentSectorIdx + 1;
        }
        return next;
    }

    private static int firstSectorIdx(int clusterIdx) {
        return Layout.getClusterDataStartSector(clusterIdx);
    }

    private static int lastSectorIdx(int clusterIdx) {
        return Layout.getClusterDataLastSector(clusterIdx);
    }
}
