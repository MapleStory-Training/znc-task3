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
import java.io.InputStream;

import org.cooder.mos.MosSystem;
import org.cooder.mos.fs.FileDescriptor;
import org.cooder.mos.fs.FileSystem;

public class FileInputStream extends InputStream {

    private final FileDescriptor fd;

    public FileInputStream(MosFile file) throws IOException {
        fd = MosSystem.fileSystem().open(file.getPath(), FileSystem.READ);
    }

    @Override
    public int read() throws IOException {
        return MosSystem.fileSystem().read(fd);
    }

    public void close() throws IOException {
        MosSystem.fileSystem().close(fd);
    }
}
