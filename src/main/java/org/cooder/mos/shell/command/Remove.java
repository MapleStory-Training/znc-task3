/*
 * This file is part of MOS
 * <p>
 * Copyright (c) 2021 by cooder.org
 * <p>
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package org.cooder.mos.shell.command;

import org.cooder.mos.api.MosFile;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "rm")
public class Remove extends MosCommand {
    @Parameters(paramLabel = "<path>")
    private String path;

    @Override
    public int runCommand() {
        String[] paths = shell.absolutePath(path);
        MosFile file = new MosFile(paths);
        file.delete();
        return 0;
    }
}
