package com.algoprep.studylist;

import java.nio.file.Path;

/** The tab title for a study list file. Pure. */
public final class StudyListTitle {

    private static final int MAX_TAB_LABEL = 20;

    private StudyListTitle() {}

    /** The file name without its extension: {@code Grind75.csv} becomes {@code Grind75}. */
    public static String of(Path file) {
        Path name = file.getFileName();
        String text = name == null ? file.toString() : name.toString();
        int dot = text.lastIndexOf('.');
        return dot > 0 ? text.substring(0, dot) : text;
    }

    /** A title shortened for a tab, so a long file name does not crowd out the other tabs. */
    public static String tabLabel(String title) {
        return title.length() <= MAX_TAB_LABEL ? title : title.substring(0, MAX_TAB_LABEL - 1) + "…";
    }
}
