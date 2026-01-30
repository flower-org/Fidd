package com.fidd.view.common;

import java.util.List;

public record PlaylistSettings(List<String> filterIn,
                               List<String> filterOut,
                               PlaylistSort sort,
                               boolean includeSubfolders) {
    public PlaylistSettings() {
        this(List.of(), List.of(), PlaylistSort.NUMERICAL_ASC, false);
    }
}
