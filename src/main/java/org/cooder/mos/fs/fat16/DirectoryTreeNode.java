/*
 * This file is part of MOS
 * <p>
 * Copyright (c) 2021 by cooder.org
 * <p>
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package org.cooder.mos.fs.fat16;

import org.cooder.mos.fs.IFileSystem;
import org.cooder.mos.fs.fat16.Layout.DirectoryEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DirectoryTreeNode {
    private DirectoryEntry entry;
    public final DirectoryTreeNode parent;
    private DirectoryTreeNode[] children;
    private int sectorIdx = -1;
    private int sectorOffset = -1;
    private boolean fold = true;
    private String lfName;

    DirectoryTreeNode(DirectoryTreeNode parent, DirectoryEntry entry) {
        this.parent = parent;
        this.entry = entry;
    }

    public DirectoryTreeNode[] getChildren() {
        return children;
    }

    void setChildren(DirectoryTreeNode[] children) {
        this.children = children;
    }

    int getSectorIdx() {
        return sectorIdx;
    }

    void setSectorIdx(int sectorIdx) {
        this.sectorIdx = sectorIdx;
    }

    int getSectorOffset() {
        return sectorOffset;
    }

    void setSectorOffset(int sectorOffset) {
        this.sectorOffset = sectorOffset;
    }

    public DirectoryEntry getEntry() {
        return entry;
    }

    // todo:判断是否通用
    public boolean isDir() {
        return isRoot() || ((entry.attrs & DirectoryEntry.ATTR_MASK_DIR) != 0);
    }

    public boolean isRoot() {
        return entry == null;
    }

    boolean isFold() {
        return fold;
    }

    void unfold() {
        this.fold = false;
    }

    public void fold() {
        this.fold = true;
    }

    public String getName() {
        if (isRoot()) {
            return "/";
        } else {
            if (isLfnEntry(entry)) {
                return byteArray2String(entry.partOne) + byteArray2String(entry.partTwo);
            } else {
                return byteArray2String(entry.fileName);
            }
        }
    }

    public String getLfName() {
        if (isRoot()) {
            return "/";
        } else {
            return lfName;
        }
    }

    void setLfgName(String lfName) {
        this.lfName = lfName;
    }

    private boolean isLfnEntry(DirectoryEntry entry) {
        return (entry.attrs & DirectoryEntry.ATTR_MASK_LFN) == DirectoryEntry.ATTR_MASK_LFN;
    }

    public String getLfPath() {
        if (isRoot()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        if (parent != null) {
            sb.append(parent.getLfPath()).append(IFileSystem.separator);
        }
        String subName = getLfName();
        if (subName.startsWith("/")) {
            return sb.append(subName.substring(1)).toString();
        }
        return sb.append(subName).toString();
    }

    DirectoryTreeNode find(String name) {
        if (!isDir()) {
            throw new IllegalArgumentException();
        }

        if (isFold()) {
            throw new IllegalStateException();
        }

        if (children == null) {
            return null;
        }
        DirectoryTreeNode node;
        List<String> nameList = new ArrayList<>();
        int nameLength = DirectoryEntry.FILE_NAME_LENGTH;
        for (DirectoryTreeNode child : children) {

            if (FAT16.isLfnEntry(child.getEntry())) {
                nameLength += DirectoryEntry.FILE_NAME_LFN_LENGTH;
                nameList.add(child.getName());
            } else {
                node = child;
                nameList.add(child.getName());
                String nodeName = "";
                for (int i = nameList.size() - 1; i >= 0; i--) {
                    String subName = nameList.get(i);
                    if (subName.startsWith("/")) {
                        nodeName += subName.substring(1);
                    } else {
                        nodeName += subName;
                    }
                }
                nameList = new ArrayList<>();
                if (nameEquals(nodeName, name, Math.max(nameLength, name.length()))) {
                    return node;
                }
            }
        }

        return null;
    }

    private static boolean nameEquals(String nodeName, String fileName, int nameLength) {
        return Arrays.equals(string2ByteArray(nodeName, nameLength), string2ByteArray(fileName, nameLength));
    }

    private static byte[] string2ByteArray(String name, int length) {
        byte[] b1 = name.getBytes();
        byte[] b2 = new byte[length];
        System.arraycopy(b1, 0, b2, 0, Math.min(b1.length, length));
        return b2;
    }

    private static String byteArray2String(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte value : b) {
            if (value == 0) {
                break;
            }
            sb.append((char) (value & 0xFF));
        }
        return sb.toString();
    }

    DirectoryTreeNode create(String name, boolean isDir, boolean lfnEntry) {
        DirectoryTreeNode node = nextFreeNode();
        DirectoryEntry entry = node.entry;
        byte[] b;
        if (!lfnEntry) {
            b = string2ByteArray(name, DirectoryEntry.FILE_NAME_LENGTH);
            System.arraycopy(b, 0, entry.fileName, 0, b.length);
            entry.attrs |= isDir ? DirectoryEntry.ATTR_MASK_DIR : 0;
            node.setWriteTime(System.currentTimeMillis());
        } else {
            b = string2ByteArray(name, DirectoryEntry.FILE_NAME_LFN_LENGTH);
            System.arraycopy(b, 0, entry.partOne, 0, DirectoryEntry.FILE_NAME_PART_ONE_LENGTH);
            System.arraycopy(b, DirectoryEntry.FILE_NAME_PART_ONE_LENGTH, entry.partTwo, 0, DirectoryEntry.FILE_NAME_PART_TWO_LENGTH);
            entry.attrs |= DirectoryEntry.ATTR_MASK_LFN;
            // todo:ordinal field
        }

        return node;
    }

    DirectoryTreeNode firstTreeNode() {
        if (!isDir()) {
            throw new IllegalArgumentException();
        }

        if (isFold()) {
            throw new IllegalStateException();
        }

        if (children == null) {
            return null;
        }

        DirectoryTreeNode node;
        for (DirectoryTreeNode child : children) {
            node = child;
            if (!node.isFree()) {
                return node;
            }
        }
        return null;
    }

    private DirectoryTreeNode nextFreeNode() {
        if (!isDir()) {
            throw new IllegalArgumentException();
        }

        if (isFold()) {
            throw new IllegalStateException();
        }

        if (children == null) {
            return null;
        }

        for (DirectoryTreeNode child : children) {
            if (child.isFree()) {
                return child;
            }
        }
        return null;
    }

    private boolean isFree() {
        return entry.fileName[0] == 0 && (entry.attrs & DirectoryEntry.ATTR_MASK_LFN) != DirectoryEntry.ATTR_MASK_LFN;
    }

    public boolean valid() {
        return entry != null && entry.fileName[0] != 0;
    }

    void reset() {
        this.fold = true;
        this.entry = new DirectoryEntry();
        this.children = null;
    }

    public void setFileSize(int fileSize) {
        this.entry.fileSize = fileSize;
    }

    void setWriteTime(long currentTimeMillis) {
        int sec = (int) (currentTimeMillis / 1000);
        this.entry.lastWriteTime = (short) (sec & 0xFFFF);
        this.entry.lastWriteDate = (short) (sec >>> 16 & 0xFFFF);
    }

    public long getWriteTime() {
        long sec = this.entry.lastWriteDate;
        sec = sec << 16;
        sec = sec | (0xFFFF & this.entry.lastWriteTime);
        return sec * 1000;
    }

    int getFileSize() {
        return entry.fileSize;
    }
}
