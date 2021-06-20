/*
 * This file is part of MOS
 * <p>
 * Copyright (c) 2021 by cooder.org
 * <p>
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package org.cooder.mos.fs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cooder.mos.Utils;
import org.cooder.mos.device.IDisk;
import org.cooder.mos.fs.fat16.DirectoryTreeNode;
import org.cooder.mos.fs.fat16.FAT16;
import org.cooder.mos.fs.fat16.FatFileInputStream;
import org.cooder.mos.fs.fat16.FatFileOutputStream;
import org.cooder.mos.fs.fat16.Layout;

public class FileSystem implements IFileSystem {
    public static final FileSystem instance = new FileSystem();
    
    private IDisk disk;
    private FAT16 fat;
    private final Set<FileDescriptor> OPEN_FILES = new HashSet<FileDescriptor>();
    
    private FileSystem() {}

    @Override
    public void bootstrap(IDisk _disk) {
        disk = _disk;
        fat = new FAT16(disk);
    }
    
    @Override
    public void shutdown() throws IOException {
        fat.close();
        disk.close();
    }

    @Override
    public void format() throws IOException {
        disk.clear();
        fat.format();
    }
    
    @Override
    public FileDescriptor find(String[] paths) {
        DirectoryTreeNode node = findEntryNode(paths);
        if (node == null) {
            return null;
        }
        
        return new FileDescriptor(paths, node);
    }
    
    @Override
    public FileDescriptor open(String[] paths, int mode) throws IOException {
        FileDescriptor descriptor = null;
        DirectoryTreeNode node = findEntryNode(paths);
        if (mode == READ) {
            if (node == null) {
                throw new IOException("No such file or directory");
            }
            descriptor = new FileDescriptor(paths, node);
            descriptor.setFatFileInputStream(createFatFileInputStream(descriptor));
        } else {
            if (node == null) {
                node = createEntry(paths);
            }
            descriptor = new FileDescriptor(paths, node);
            descriptor.setFatFileOutputStream(createFatFileOutputStream(descriptor, mode));
        }

        OPEN_FILES.add(descriptor);
        return descriptor;
    }

    @Override
    public int read(FileDescriptor fd) throws IOException {
        return fd.read();
    }

    @Override
    public void write(FileDescriptor fd, int b) throws IOException {
        fd.write(b);
    }
    
    @Override
    public void delete(FileDescriptor fd) {
        deleteTreeNode(fd.node);
    }
    
    @Override
    public void flush(FileDescriptor fdDescriptor) {
        fdDescriptor.flush();
    }

    @Override
    public void close(FileDescriptor fdDescriptor) {
        fdDescriptor.close();
        OPEN_FILES.remove(fdDescriptor);
    }
    
    @Override
    public FileDescriptor createDirectory(FileDescriptor parent, String name) {
        DirectoryTreeNode node = fat.createTreeNode(parent.node, name, true);
        return new FileDescriptor(Utils.normalizePath(node.getPath()), node);
    }
    
    @Override
    public String[] list(FileDescriptor parent) {
        fat.loadEntries(parent.node);

        List<String> list = new ArrayList<String>();
        DirectoryTreeNode[] nodes = parent.node.getChildren();
        for (DirectoryTreeNode s : nodes) {
            if (s.valid()) {
                list.add(s.getPath());
            }
        }
        return list.toArray(new String[0]);
    }

    private DirectoryTreeNode findEntryNode(String[] paths) {
        if (paths == null || paths.length == 0) {
            return fat.root;
        }

        if (paths.length == 1 && paths[0].equals(separator + "")) {
            return fat.root;
        }

        DirectoryTreeNode parent = fat.root;
        DirectoryTreeNode entry = null;
        for (String name : paths) {
            entry = fat.findSubTreeNode(parent, name);
            if (entry == null) {
                return null;
            }
            parent = entry;
        }
        return entry;
    }
    
    private void deleteTreeNode(DirectoryTreeNode node) {
        if (node.isDir() && !fat.isEmpty(node)) {
            throw new IllegalStateException("directory not empty.");
        }
        
        // remove file data
        if (!node.isDir()) {
            int clusterIdx = node.getEntry().startingCluster;
            fat.markFreeFrom(clusterIdx);
            fat.writeCluster(clusterIdx, FAT16.FREE_CLUSTER);
        }
        
        fat.removeTreeNode(node);
    }

    private DirectoryTreeNode createEntry(String[] paths) {
        DirectoryTreeNode parent = fat.root;
        DirectoryTreeNode entry = null;
        for (int i = 0; i < paths.length; i++) {
            String name = paths[i];
            entry = fat.findSubTreeNode(parent, name);
            if (entry == null) {
                boolean isDir = i < paths.length - 1;
                entry = fat.createTreeNode(parent, name, isDir);
            }
            parent = entry;
        }
        return entry;
    }

    private FatFileInputStream createFatFileInputStream(FileDescriptor fd) {
        int clusterIdx = fd.getStartingCluster();
        int sectorIdx = Layout.getClusterDataStartSector(clusterIdx);
        return new FatFileInputStream(disk, fat, clusterIdx, sectorIdx, fd.getFileSize());
    }

    private FatFileOutputStream createFatFileOutputStream(FileDescriptor fd, int mode) {
        int clusterIdx = fd.getStartingCluster();
        if (mode == WRITE) {
            // clear file content
            fat.markFreeFrom(clusterIdx);
            fat.writeCluster(clusterIdx, fat.getEndOfChain());
            fd.node.setFileSize(0);
            fat.writeDirectoryTreeNode(fd.node);
            
            int sectorIdx = Layout.getClusterDataStartSector(clusterIdx);
            return new FatFileOutputStream(disk, fat, clusterIdx, sectorIdx, 0, fd.node);
        
        } else if (mode == APPEND) {
            int lastClusterIdx = fat.lastClusterFrom(clusterIdx);
            int clusterCount = fat.clusterCountFrom(clusterIdx);
            int fileSize = fd.getFileSize();
            
            // cluster chain full, alloc next cluster
            if (clusterCount*Layout.PER_CLUSTER_SIZE == fileSize) {
                int next = fat.nextFreeCluster(lastClusterIdx);
                lastClusterIdx = next;
            }
            
            int offset = fileSize % Layout.PER_CLUSTER_SIZE;
            int sectors = offset / Layout.PER_SECTOR_SIZE;
            int pos = offset % Layout.PER_SECTOR_SIZE;
            int sectorIdx = Layout.getClusterDataStartSector(lastClusterIdx) + sectors;

            return new FatFileOutputStream(disk, fat, lastClusterIdx, sectorIdx, pos, fd.node);
        }
        return null;
    }
}
