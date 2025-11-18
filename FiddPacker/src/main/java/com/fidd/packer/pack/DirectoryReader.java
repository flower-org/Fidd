package com.fidd.packer.pack;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.fidd.packer.pack.FilePathTuple.getRelativePath;

public class DirectoryReader {
    public static List<FilePathTuple> getDirectoryContents(File directory) {
        if (!directory.exists()) {
            throw new RuntimeException("Directory " + directory + " doesn't exist.");
        }
        if (!directory.isDirectory()) {
            throw new RuntimeException(directory + " is not a directory.");
        }

        String rootPath = directory.getAbsolutePath();
        return getDirectoryContents(directory, rootPath);
    }

    private static List<FilePathTuple> getDirectoryContents(File directory, String rootPath) {
        List<FilePathTuple> tuplesList = new ArrayList<>();

        // Check if the provided path is a directory
        if (directory.exists() && directory.isDirectory()) {
            // List all files and directories in the specified directory
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        // If it's a directory, recursively process it
                        tuplesList.addAll(getDirectoryContents(file, rootPath));
                    } else if (file.isFile()) {
                        // If it's a file, create a tuple and add it to the list
                        String relativePath = getRelativePath(file, rootPath);
                        tuplesList.add(new FilePathTuple(file, relativePath));
                    }
                }
            }
        } else {
            System.out.println("The specified path is not a directory or does not exist.");
        }

        return tuplesList;
    }
}
