/*
 * This file is part of MOS
 * <p>
 * Copyright (c) 2021 by cooder.org
 * <p>
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package org.cooder.mos.device;

import java.io.Closeable;
import java.io.IOException;

public interface IDisk extends Closeable {

    /**
     * 磁盘每个扇区的大小
     */
    int sectorSize();

    /**
     * 磁盘扇区数量
     */
    int sectorCount();

    /**
     * 获取磁盘容量，
     * <b>capacity = sectorCount * sectorSize</b>
     */
    long capacity();

    /**
     * 读取一个指定扇区的数据。
     *
     * @param sectorIdx 扇区索引，起始索引为0，终止索引为 {@code sectorCount()-1}
     * @return 扇区数据，返回的字节数组长度必须等于{@code sectorSize()}
     */
    byte[] readSector(int sectorIdx);

    /**
     * 读取一个指定扇区数据到buffer
     *
     * @param sectorIdx 扇区索引，起始索引为0，终止索引为 {@code sectorCount()-1}
     * @param buffer    字节数组长度必须等于{@code sectorSize()}
     */
    void readSector(int sectorIdx, byte[] buffer);

    /**
     * 写一个指定扇区。
     *
     * @param sectorIdx  扇区索引，起始索引为0，终止索引为 {@code sectorCount()-1}
     * @param sectorData 待写入的数据. 长度必须等于{@code sectorSize()}
     */
    void writeSector(int sectorIdx, byte[] sectorData);

    /**
     * 清空磁盘数据
     *
     * @throws IOException
     */
    void clear() throws IOException;

}
