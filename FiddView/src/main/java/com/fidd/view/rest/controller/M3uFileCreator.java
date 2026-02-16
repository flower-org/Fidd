package com.fidd.view.rest.controller;

import java.util.List;

public class M3uFileCreator {
    public static String createM3UPlaylist(List<String> filteredLogicalFileNames) {
        StringBuilder m3uBuilder = new StringBuilder();

        // Write the M3U header
        m3uBuilder.append("#EXTM3U\n");
        for (String fileName : filteredLogicalFileNames) {
            // Optional: Add an extended header with file name as title
            m3uBuilder.append("#EXTINF:-1");
                    //.append(",").append(fileName); // -1 indicates unknown duration
            m3uBuilder.append("\n");
            m3uBuilder.append(fileName); // The file path
            m3uBuilder.append("\n");
        }

        return m3uBuilder.toString();
    }
}
