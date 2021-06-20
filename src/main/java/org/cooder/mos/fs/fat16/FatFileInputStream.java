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
import java.io.InputStream;

import org.cooder.mos.device.IDisk;

public class FatFileInputStream extends InputStream {
    private final byte[] buffer = new byte[Layout.PER_SECTOR_SIZE];
    private int pos = 0;
    private int limit = -1;
    private int count = 0;
    private int currentClusterIdx;
    private int currentSectorIdx;
    private IDisk disk;
    private IFAT16 fat;

    public FatFileInputStream(IDisk disk, IFAT16 fat, int startClusterIdx, int sectorIdx, int limit) {
        this.disk = disk;
        this.currentClusterIdx = startClusterIdx;
        this.currentSectorIdx = sectorIdx;
        this.limit = limit;
        this.fat = fat;
        
        disk.readSector(currentSectorIdx, buffer);
    }

    @Override
    public int read() throws IOException {
        if (count >= limit) {
            return -1;
        }

        if (pos >= buffer.length) {
            if (!readNextSector()) {
                return -1;
            }
        }

        count++;
        return buffer[pos++];
    }

    public void close() {
        // no-op
    }

    public int getCount() {
        return count;
    }

    private boolean readNextSector() {
        int next = nextSector();
        if (next < 0) {
            return false;
        }
        
        currentSectorIdx = next;
        disk.readSector(currentSectorIdx, buffer);
        pos = 0;
        return true;
    }

    private int nextSector() {
        int next = -1;
        if (currentSectorIdx == lastSectorIdx(currentClusterIdx)) {
            if (currentClusterIdx == fat.getEndOfChain()) {
                return -1;
            }
            currentClusterIdx = fat.readCluster(currentClusterIdx);
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
