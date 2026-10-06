package com.chatstory.theme;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NativeThemeModel {

    public interface Listener {
        void onThemeChanged(NativeTheme previous, NativeTheme current);
    }

    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private NativeTheme theme = NativeTheme.DARK;

    public synchronized NativeTheme current() {
        return theme;
    }

    public void setTheme(NativeTheme next) {
        NativeTheme previous;
        synchronized (this) {
            if (next == null || next == theme) {
                return;
            }
            previous = theme;
            theme = next;
        }
        for (Listener listener : listeners) {
            listener.onThemeChanged(previous, next);
        }
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }
}
