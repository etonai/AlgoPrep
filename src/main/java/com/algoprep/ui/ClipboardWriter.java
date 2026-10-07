package com.algoprep.ui;

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

/** Puts text on the system clipboard. Replaceable so tests never touch the real clipboard. */
@FunctionalInterface
public interface ClipboardWriter {

    /** Copies the text. Throws if the clipboard cannot be used. */
    void copy(String text) throws Exception;

    /** The system clipboard. */
    ClipboardWriter SYSTEM = text -> {
        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException("no clipboard available (headless)");
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
    };
}
