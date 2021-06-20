/*
 * This file is part of MOS
 * <p>
 * Copyright (c) 2021 by cooder.org
 * <p>
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package org.cooder.mos;

import java.io.InputStream;
import java.io.PrintStream;

import org.cooder.mos.fs.FileSystem;
import org.cooder.mos.fs.IFileSystem;

public class MosSystem {
    
    private MosSystem() {}
    
    public final static InputStream in = System.in;

    public final static PrintStream out = System.out;

    public final static PrintStream err = System.err;
    
    public static IFileSystem fileSystem() {
        return FileSystem.instance;
    }
}
