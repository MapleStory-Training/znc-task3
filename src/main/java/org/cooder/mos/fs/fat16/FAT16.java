/*
 * This file is part of MOS
 * <p>
 * Copyright (c) 2021 by cooder.org
 * <p>
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package org.cooder.mos.fs.fat16;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

import org.cooder.mos.device.IDisk;
import org.cooder.mos.fs.fat16.Layout.DirectoryEntry;

public class FAT16 implements IFAT16 {
    public static final int FAT_SIZE = Layout.SECTORS_PER_FAT * Layout.PER_SECTOR_SIZE / 2;
    
    public static final int FREE_CLUSTER = 0x0000;
    public static final int END_OF_CHAIN = 0xFFF8;
    
    private final IDisk disk;
    private final int[] table = new int[FAT_SIZE];
    public final DirectoryTreeNode root = new DirectoryTreeNode(null, null);

    public FAT16(IDisk disk) {
        this.disk = disk;
        reload();
    }

    //
    // File Allocation Table Methods
    //
    
    @Override
    public int getEndOfChain() {
        return 0xFFF8;
    }

    @Override
    public synchronized int nextFreeCluster(int preCluster) {
        for (int i = Layout.HEAD_CLUSTER_COUNT; i < FAT_SIZE; i++) {
            if (table[i] == 0) {
                writeCluster(i, getEndOfChain());
                writeCluster(preCluster, i);
                return i;
            }
        }
        return -1;
    }

    @Override
    public synchronized void markFreeFrom(int clusterIdx) {
        int idx = clusterIdx;
        int v = table[idx];
        while (v != getEndOfChain()) {
            idx = v;
            v = table[idx];
            table[idx] = FREE_CLUSTER;
        }
        
        if (idx != clusterIdx) {
            table[idx] = FREE_CLUSTER;
        }
    }

    @Override
    public synchronized int lastClusterFrom(int clusterIdx) {
        if (clusterIdx < Layout.HEAD_CLUSTER_COUNT) {
            throw new IllegalStateException();
        }
        
        int idx = clusterIdx;
        int v = table[idx];
        while (v != getEndOfChain()) {
            idx = v;
            v = table[idx];
        }
        return idx;
    }
    
    @Override
    public synchronized int clusterCountFrom(int clusterIdx) {
        if (clusterIdx < Layout.HEAD_CLUSTER_COUNT) {
            throw new IllegalStateException();
        }
        
        int idx = clusterIdx;
        int v = table[idx], count = 1;
        while (v != getEndOfChain()) {
            idx = v;
            v = table[idx];
            count++;
        }
        return count;
    }

    @Override
    public synchronized int readCluster(int clusterIdx) {
        return table[clusterIdx];
    }

    @Override
    public synchronized void writeCluster(int clusterIdx, int valueToWrite) {
        if (clusterIdx < 0) return;
        
        table[clusterIdx] = valueToWrite;
        flush(); // TODO
    }
    
    private synchronized void loadFAT() {
        ByteBuffer buffer = ByteBuffer.allocate(Layout.SECTORS_PER_FAT * Layout.PER_SECTOR_SIZE);
        for (int i = 0; i < Layout.SECTORS_PER_FAT; i++) {
            byte[] data = disk.readSector(i + Layout.FAT_REGION_START);
            buffer.put(data);
        }

        buffer.rewind();
        ShortBuffer sb = buffer.asShortBuffer();
        for (int i = 0; i < FAT_SIZE; i++) {
            short value = sb.get();
            table[i] = value & 0xFFFF;
        }
    }
    
    public synchronized void reload() {
        loadFAT();
        loadSubEntries(root);
    }
    
    public synchronized void flush() {
        ByteBuffer buffer = ByteBuffer.allocate(Layout.SECTORS_PER_FAT * Layout.PER_SECTOR_SIZE);
        for (int i = 0; i < FAT_SIZE; i++) {
            short value = (short) (table[i] & 0xFFFF);
            buffer.putShort(value);
        }
        
        buffer.rewind();
        byte[] sectorData = new byte[Layout.PER_SECTOR_SIZE];
        for (int i = 0; i < Layout.SECTORS_PER_FAT; i++) {
            int sectorIdx = i + Layout.FAT_REGION_START;
            buffer.get(sectorData);
            disk.writeSector(sectorIdx, sectorData);
        }
    }

    // 
    // Directory Tree Method.
    // 
    
    @Override
    public void writeDirectoryTreeNode(DirectoryTreeNode node) {
        byte[] entryData = node.getEntry().toBytes();
        byte[] sectorData = disk.readSector(node.getSectorIdx());
        System.arraycopy(entryData, 0, sectorData, node.getSectorOffset(), entryData.length);
        
        disk.writeSector(node.getSectorIdx(), sectorData);
    }
    
    @Override
    public void removeTreeNode(DirectoryTreeNode node) {
        if (node == null || node == root) {
            return;
        }
        
        int clusterIdx = node.getEntry().startingCluster;
        node.reset();
        writeCluster(clusterIdx, FREE_CLUSTER);
        writeDirectoryTreeNode(node);
    }
    
    @Override
    public DirectoryTreeNode findSubTreeNode(DirectoryTreeNode parent, String name) {
        if (parent == null) {
            parent = root;
        }
        
        if(!parent.isDir()) {
            throw new IllegalArgumentException(name + ": not directory");
        }
        
        if (parent.isFold()) {
            loadSubEntries(parent);
        }
        
        return parent.find(name);
    }
    
    public boolean isEmpty(DirectoryTreeNode parent) {
        if (parent == null) {
            parent = root;
        }

        if (!parent.isDir()) {
            throw new IllegalArgumentException(parent.getName() + ": not directory");
        }

        if (parent.isFold()) {
            loadSubEntries(parent);
        }

        return parent.firstTreeNode() == null;
    }
    
    @Override
    public DirectoryTreeNode createTreeNode(DirectoryTreeNode parent, String name, boolean isDir) {
        if (parent == null) {
            parent = root;
        }
        
        if (parent.isFold()) {
            loadSubEntries(parent);
        }
        
        DirectoryTreeNode node = parent.find(name);
        if (node != null) {
            throw new IllegalStateException("file exist.");
        }
        
        node = parent.create(name, isDir);
        
        // update
        DirectoryEntry entry = node.getEntry();
        entry.startingCluster = (short) (nextFreeCluster(-1) & 0xFFFF);
        writeDirectoryTreeNode(node);
        
        return node;
    }
    
    public void loadEntries(DirectoryTreeNode parent) {
        if (parent.isDir() && parent.isFold()) {
            loadSubEntries(parent);
        }
    }
    
    private void loadSubEntries(DirectoryTreeNode parent) {
        int sectorIdx, limit;
        if (parent == root) {
            sectorIdx = Layout.ROOT_DIRECTORY_REGION_START;
            limit = Layout.ROOT_DIRECTORY_REGION_SIZE;
        } else {
            sectorIdx = Layout.getClusterDataStartSector(parent.getEntry().startingCluster);
            limit = Layout.SECTORS_PER_CLUSTER;
        }

        List<DirectoryTreeNode> childre = loadEntries(parent, sectorIdx, limit);
        parent.setChildren(childre.toArray(new DirectoryTreeNode[childre.size()]));
        parent.unfold();
    }
    
    private List<DirectoryTreeNode> loadEntries(DirectoryTreeNode parent, int sectorIdx, int limitSectorCount) {
        List<DirectoryTreeNode> nodes = new ArrayList<DirectoryTreeNode>(limitSectorCount);
        byte[] buffer = new byte[Layout.PER_DIRECTOR_ENTRY_SIZE];
        for (int i = 0; i < limitSectorCount; i++) {
            byte[] sectorData = disk.readSector(sectorIdx + i);
            for (int j = 0; j < sectorData.length; j += Layout.PER_DIRECTOR_ENTRY_SIZE) {
                System.arraycopy(sectorData, j, buffer, 0, Layout.PER_DIRECTOR_ENTRY_SIZE);
                DirectoryEntry entry = DirectoryEntry.from(buffer);
                
                DirectoryTreeNode node = new DirectoryTreeNode(parent, entry);
                node.setSectorIdx(sectorIdx + i);
                node.setSectorOffset(j);
                nodes.add(node);
            }
        }
        return nodes;
    }
    
    //
    // 其他操作
    //
    @Override
    public void format() {
        // fill disk with zero
        byte[] zeros = new byte[Layout.PER_SECTOR_SIZE];
        int count = Layout.DATA_REGION_START;
        for (int i = 0; i < count; i++) {
            disk.writeSector(i, zeros);
        }

        // write boot sector
        disk.writeSector(0, new Layout.BootSector().toBytes());

        reload();
    }
    
    @Override
    public synchronized void close() {
        flush();
    }
}
