/*
 * This file is part of MOS
 * <p>
 * Copyright (c) 2021 by cooder.org
 * <p>
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package org.cooder.mos.shell.command;

import org.cooder.mos.Utils;
import org.cooder.mos.api.MosFile;

import picocli.CommandLine.Command;
import picocli.CommandLine.Help.TextTable;

@Command(name = "ls", aliases = {"ll"})
public class ListCommand extends MosCommand {
    @Override
    public int runCommand() {
        TextTable textTable = forColumnWidths(10, 14, 30);

        String path = shell.currentPath();
        MosFile file = new MosFile(path);
        MosFile[] files = file.listFiles();
        for (MosFile f : files) {
            String size = String.valueOf(f.length());
            String time = Utils.time2String(f.lastModified());
            String name = f.getName() + (f.isDir() ? "/" : "");
            textTable.addRowValues(size, time, name);
        }
        out.println(textTable.toString());
        return 0;
    }
}
