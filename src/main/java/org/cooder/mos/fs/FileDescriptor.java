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

import org.cooder.mos.Utils;
import org.cooder.mos.fs.fat16.DirectoryTreeNode;
import org.cooder.mos.fs.fat16.FatFileInputStream;
import org.cooder.mos.fs.fat16.FatFileOutputStream;

public class FileDescriptor {
    private final String[] paths;
    private FatFileInputStream fis;
    private FatFileOutputStream fos;
    final DirectoryTreeNode node;

    FileDescriptor(String[] paths, DirectoryTreeNode node) {
        this.node = node;
        this.paths = paths;
    }

    public String[] getPaths() {
        return paths;
    }

    public void write(int b) throws IOException {
        if (fos == null) {
            throw new IllegalStateException();
        }
        fos.write(b);
    }

    public void flush() {
        fos.flush();
    }

    public void close() {
        Utils.close(fis);
        Utils.close(fos);
    }

    public int read() throws IOException {
        if (fis == null) {
            throw new IllegalStateException();
        }
        return fis.read();
    }

    public void setFatFileInputStream(FatFileInputStream fis) {
        this.fis = fis;
    }

    public void setFatFileOutputStream(FatFileOutputStream fos) {
        this.fos = fos;
    }

    public boolean isRoot() {
        return node.isRoot();
    }

    public String getName() {
        return node.getName();
    }

    public String getPath() {
        return node.getPath();
    }

    public String getParentPath() {
        return node.parent.getPath();
    }

    public boolean isDir() {
        return node.isDir();
    }

    public int getFileSize() {
        return node.getEntry().fileSize;
    }

    public long getWriteTime() {
        return node.getWriteTime();
    }

    public int getStartingCluster() {
        return (node.getEntry().startingCluster & 0xFFFF);
    }
}
