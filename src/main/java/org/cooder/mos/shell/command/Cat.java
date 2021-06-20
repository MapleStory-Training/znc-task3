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
import java.io.InputStreamReader;

import org.cooder.mos.Utils;
import org.cooder.mos.api.FileInputStream;
import org.cooder.mos.api.MosFile;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "cat")
public class Cat extends MosCommand {
    @Parameters(paramLabel = "<path>")
    private String path;

    @Override
    public int runCommand() {
        String[] paths = shell.absolutePath(path);
        MosFile file = new MosFile(paths);
        
        if (!file.exist()) {
            out.println(path + ": No such file or directory");
            return 1;
        }

        if (file.isDir()) {
            out.println(path + ": is a directory");
            return 1;
        }
        
        try {
            InputStreamReader reader = new InputStreamReader(new FileInputStream(file));
            Utils.copyStreamNoCloseOut(reader, out);
        } catch (IOException e) {
            e.printStackTrace();
            return 1;
        }

        return 0;
    }
}
