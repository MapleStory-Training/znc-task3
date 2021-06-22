/*
 * This file is part of MOS
 * <p>
 * Copyright (c) 2021 by cooder.org
 * <p>
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package org.cooder.mos.api;

import java.io.IOException;
import java.io.OutputStream;

import org.cooder.mos.MosSystem;
import org.cooder.mos.fs.FileDescriptor;

public class FileOutputStream extends OutputStream {

    private final FileDescriptor fd;

    public FileOutputStream(MosFile file, int mode) throws IOException {
        if (file.isDir()) {
            throw new IOException(file.getName() + ": is a directory");
        }
        fd = MosSystem.fileSystem().open(file.getPath(), mode);
    }

    @Override
    public void write(int b) throws IOException {
        byte d = (byte) (b & 0xFF);
        MosSystem.fileSystem().write(fd, d);
    }

    public void flush() {
        MosSystem.fileSystem().flush(fd);
    }

    public void close() throws IOException {
        MosSystem.fileSystem().close(fd);
    }
}
