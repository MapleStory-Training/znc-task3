/*
 * This file is part of MOS
 * <p>
 * Copyright (c) 2021 by cooder.org
 * <p>
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package org.cooder.mos.shell.command;

import java.io.IOException;

import org.cooder.mos.Utils;
import org.cooder.mos.api.FileOutputStream;
import org.cooder.mos.api.MosFile;
import org.cooder.mos.fs.FileSystem;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "touch")
public class Touch extends MosCommand {
    @Parameters(paramLabel = "<path>")
    private String path;

    @Override
    public int runCommand() {
        String[] paths = shell.absolutePath(path);
        FileOutputStream fos = null;
        try {
            MosFile file = new MosFile(paths);
            if (!file.exist()) {
                fos = new FileOutputStream(new MosFile(paths), FileSystem.WRITE);
            }
        } catch (IOException e) {
            err.println(e.getMessage());
        } finally {
            Utils.close(fos);
        }

        return 0;
    }
}
