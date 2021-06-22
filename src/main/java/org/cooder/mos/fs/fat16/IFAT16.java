/*
 * This file is part of MOS
 * <p>
 * Copyright (c) 2021 by cooder.org
 * <p>
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package org.cooder.mos.fs.fat16;

import java.io.Closeable;

public interface IFAT16 extends Closeable {

    //
    // FAT表操作
    //

    /**
     * @return Value representing the end of the chain.
     * Should be {@code 0xFFF8} for FAT16
     */
    int getEndOfChain();

    /**
     * 申请下一块空闲簇，并加入簇链表，调用后对应值被标记为{@code 0xFFF8}
     *
     * @param preCluster 链表尾节点，不存在时传-1
     * @return 下一块空闲的簇索引
     */
    int nextFreeCluster(int preCluster);

    /**
     * 从指定的簇开始清空后续的簇
     *
     * @param clusterIdx 起始簇，调用后起始簇的值为0xFFF8
     */
    void markFreeFrom(int clusterIdx);

    /**
     * 从指定的簇开始的最后一个簇。
     *
     * @param clusterIdx 簇索引
     * @return 最后一个簇索引
     */
    int lastClusterFrom(int clusterIdx);

    /**
     * clusterIdx 簇索引
     *
     * @param clusterIdx
     * @return 从clusterIdx开始的链表长度
     */
    int clusterCountFrom(int clusterIdx);

    /**
     * 从文件分配表（FAT）里读取一个指定簇的值。
     *
     * @param clusterIdx 簇索引. Data clusters start at 0x0002, and end at 0xFFEF
     *                   (for FAT16). Special cluster values are:
     *                   <ul>
     *                   <li>0x0000: Free cluster</li>
     *                   <li>0xFFF8: Last cluster in file - End Of Chain marker</li>
     *                   </ul>
     */
    int readCluster(int clusterIdx);

    /**
     * 向文件分配表（FAT）里指定簇写入一个值。
     *
     * @param clusterIdx   簇索引. Data clusters start at 0x0002, and end at 0xFFEF.
     *                     Special cluster values are:
     *                     <ul>
     *                     <li>0x0000: Free cluster</li>
     *                     <li>0xFFF8: Last cluster in file - End Of Chain</li>
     *                     </ul>
     * @param valueToWrite A two-byte value to write into the table.
     */
    void writeCluster(int clusterIdx, int valueToWrite);

    //
    // 目录操作
    //

    /**
     * 写入一个目录项
     *
     * @param treeNode 目录项节点
     */
    void writeDirectoryTreeNode(DirectoryTreeNode treeNode);

    /**
     * 查找子目录项
     *
     * @param parent   父目录
     * @param filename 子文件名
     * @return
     */
    DirectoryTreeNode findSubTreeNode(DirectoryTreeNode parent, String filename);

    /**
     * 创建一个子目录项
     *
     * @param parent   父目录
     * @param filename 文件名
     * @param isDir    是否目录
     * @return
     */
    DirectoryTreeNode createTreeNode(DirectoryTreeNode parent, String filename, boolean isDir);

    /**
     * 删除一个目录项
     *
     * @param node
     */
    void removeTreeNode(DirectoryTreeNode node);

    //
    // 其他操作
    //

    /**
     * 格式化
     */
    void format();
}
