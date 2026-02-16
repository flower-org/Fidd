package com.fidd.view.rest.controller;

import java.util.Comparator;

// Comparator for alphabetical sorting
public class AlphabeticalComparator implements Comparator<String> {
    final boolean isAscending;

    public AlphabeticalComparator(boolean isAscending) {
        this.isAscending = isAscending;
    }

    public int compare(String filename1, String filename2) {
        return isAscending ? filename1.compareTo(filename2) : filename2.compareTo(filename1);
    }
}
