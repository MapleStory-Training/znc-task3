/*
 * This file is part of MOS
 * <p>
 * Copyright (c) 2021 by cooder.org
 * <p>
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package org.cooder.mos.api;

public interface IFile {
    boolean exist();

    boolean isDir();

    boolean mkdir();

    boolean delete();

    MosFile[] listFiles();

    int length();

    long lastModified();
}
