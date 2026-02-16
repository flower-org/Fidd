package com.fidd.view.rest.controller;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Comparator for numeric filename prefix
public class NumericPrefixComparator implements Comparator<String> {
    final boolean isAscending;

    public NumericPrefixComparator(boolean isAscending) {
        this.isAscending = isAscending;
    }

    @Override
    public int compare(String filename1, String filename2) {
        Integer prefix1 = getNumericPrefix(filename1);
        Integer prefix2 = getNumericPrefix(filename2);

        // Compare numeric prefixes
        int comparison = isAscending ? prefix1.compareTo(prefix2) : prefix2.compareTo(prefix1);
        if (comparison != 0) {
            return comparison;
        }
        // If prefixes are equal, fall back to alphabetical sort
        return isAscending ? filename1.compareTo(filename2) : filename2.compareTo(filename1);
    }

    // Helper method to extract numeric prefix
    private static Integer getNumericPrefix(String filename) {
        Pattern pattern = Pattern.compile("^\\d+");
        Matcher matcher = pattern.matcher(filename);
        return matcher.find() ? Integer.parseInt(matcher.group()) : Integer.MAX_VALUE;
    }
}
