/*
 * This file is part of Dkimi.
 * <p>
 * Copyright (c) 2016-2019 by yanxiyue
 * <p>
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package org.cooder.mos.fs;

import java.io.IOException;

import org.cooder.mos.device.IDisk;

public interface IFileSystem {

    /**
     * 目录路径分隔符
     */
    char separator = '/';

    /**
     * path环境变量分隔符
     */
    char pathSeparator = ':';

    /**
     * 引导启动文件系统
     *
     * @param disk
     */
    void bootstrap(IDisk disk);

    /**
     * 关闭文件系统
     *
     * @throws IOException
     */
    void shutdown() throws IOException;

    /**
     * 格式化
     *
     * @throws IOException
     */
    void format() throws IOException;

    //
    // 文件操作
    //

    /**
     * 读模式
     */
    int READ = 0;

    /**
     * 覆盖写模式
     */
    int WRITE = 1;

    /**
     * 追加写模式
     */
    int APPEND = 2;

    /**
     * @param paths
     * @return
     * @throws IOException
     */
    FileDescriptor find(String[] paths);

    /**
     * 打开一个指定文件
     *
     * @param paths 除去路径分隔符的文件路径
     * @param mode  文件打开模式
     * @return 文件描述符
     * @throws IOException
     */
    FileDescriptor open(String[] paths, int mode) throws IOException;

    /**
     * 关闭一个指定文件
     *
     * @param fd 文件描述符
     * @throws IOException
     */
    void close(FileDescriptor fd) throws IOException;

    /**
     * 读取一个字节
     *
     * @param fd 文件描述符
     * @return 文件的下一个字节, 或当到达文件末尾时返回 <code>-1</code>
     * @throws IOException
     */
    int read(FileDescriptor fd) throws IOException;

    /**
     * 往文件里写入一个字节
     *
     * @param fd 文件描述符
     * @param b  待写入的字节
     * @throws IOException
     */
    void write(FileDescriptor fd, int b) throws IOException;

    /**
     * 将缓冲数据刷盘
     *
     * @param fd 文件描述符
     */
    void flush(FileDescriptor fd);

    /**
     * 删除一个文件
     *
     * @param fd 文件描述符
     * @throws IOException
     */
    void delete(FileDescriptor fd);

    /**
     * 指定目录下创建一个目录
     *
     * @param parent 父目录
     * @param name   目录名
     * @return 新目录描述符
     */
    FileDescriptor createDirectory(FileDescriptor parent, String name);

    /**
     * 返回指定目录下的所有文件路径
     *
     * @param parent 父目录
     * @return
     */
    String[] list(FileDescriptor parent);
}
