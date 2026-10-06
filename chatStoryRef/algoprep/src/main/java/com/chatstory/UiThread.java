package com.chatstory;

import javax.swing.SwingUtilities;

public final class UiThread {

    public static void run(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) task.run();
        else SwingUtilities.invokeLater(task);
    }

    private UiThread() {}
}
