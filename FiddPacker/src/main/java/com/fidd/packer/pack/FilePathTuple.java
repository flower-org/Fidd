package com.fidd.packer.pack;

import java.io.File;

public record FilePathTuple(File file, String relativePath) {
    @Override
    public String toString() {
        return "File: " + file.getAbsolutePath() + " | Relative Path: " + relativePath + " | Size: " + file.length() + " bytes";
    }

    public static String getRelativePath(File file, String rootPath) {
        String absolutePath = file.getAbsolutePath();
        return absolutePath.replace(rootPath + File.separator, ""); // Remove root path
    }
}