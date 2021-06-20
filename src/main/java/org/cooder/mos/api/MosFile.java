/*
 * This file is part of MOS
 * <p>
 * Copyright (c) 2021 by cooder.org
 * <p>
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package org.cooder.mos.api;

import java.util.ArrayList;
import java.util.List;

import org.cooder.mos.MosSystem;
import org.cooder.mos.Utils;
import org.cooder.mos.fs.FileDescriptor;

public class MosFile implements IFile {
    private String[] paths;
    private FileDescriptor fd;

    public MosFile(String path) {
        this(Utils.normalizePath(path));
    }

    public MosFile(String[] paths) {
        this.paths = paths;
        fd = MosSystem.fileSystem().find(paths);
    }

    public String[] getPath() {
        return paths;
    }
    
    public String getName() {
        return paths[paths.length - 1];
    }

    @Override
    public boolean exist() {
        return fd != null;
    }

    @Override
    public boolean isDir() {
        return fd != null && fd.isDir();
    }

    @Override
    public boolean mkdir() {
        final int length = paths.length;
        final String name = paths[length - 1];

        if (exist()) {
            if (isDir()) {
                return false;
            } else {
                throw new IllegalStateException(name + ": is a file.");
            }
        }

        final String[] parenPaths = new String[length - 1];
        System.arraycopy(paths, 0, parenPaths, 0, length - 1);
        FileDescriptor parent = MosSystem.fileSystem().find(parenPaths);
        if (parent == null) {
            return false;
        }
        
        this.fd = MosSystem.fileSystem().createDirectory(parent, getName());
        return fd != null;
    }

    @Override
    public boolean delete() {
        if (!exist()) {
            return false;
        }

        MosSystem.fileSystem().delete(fd);
        
        fd = null;
        return true;
    }

    @Override
    public MosFile[] listFiles() {
        String[] children = MosSystem.fileSystem().list(fd);
        
        List<MosFile> list = new ArrayList<MosFile>();
        for (String path : children) {
            list.add(new MosFile(path));
        }
        return list.toArray(new MosFile[0]);
    }

    @Override
    public int length() {
        return fd.getFileSize();
    }

    @Override
    public long lastModified() {
        return fd.getWriteTime();
    }
}
