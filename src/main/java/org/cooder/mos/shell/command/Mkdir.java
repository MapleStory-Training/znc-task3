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
import org.cooder.mos.fs.IFileSystem;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "mkdir")
public class Mkdir extends MosCommand {
    @Parameters(paramLabel = "<Directory Name>")
    private String name;

    @Override
    public int runCommand() {
        MosFile file = new MosFile(shell.currentPath() + IFileSystem.separator + name);
        file.mkdir();
        return 0;
    }
}
