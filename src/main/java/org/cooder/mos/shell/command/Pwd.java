/*
 * This file is part of MOS
 * <p>
 * Copyright (c) 2021 by cooder.org
 * <p>
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package org.cooder.mos.shell.command;

import picocli.CommandLine.Command;

@Command(name = "pwd")
public class Pwd extends MosCommand {

    @Override
    public int runCommand() {
        out.println(shell.currentPath());
        return 0;
    }
}
