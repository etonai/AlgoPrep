package com.chatstory.browser;

import org.cef.browser.CefBrowser;
import org.cef.handler.CefKeyboardHandler;
import org.cef.handler.CefKeyboardHandlerAdapter;
import org.cef.misc.BoolRef;

import javax.swing.SwingUtilities;
import java.util.Map;

public class BrowserKeyboardHandler extends CefKeyboardHandlerAdapter {

    private static final int EVENTFLAG_SHIFT_DOWN = 2;
    private static final int EVENTFLAG_CONTROL_DOWN = 4;
    private static final int CTRL_SHIFT = EVENTFLAG_SHIFT_DOWN | EVENTFLAG_CONTROL_DOWN;

    private final Map<Integer, Runnable> shortcuts;

    public BrowserKeyboardHandler(Map<Integer, Runnable> shortcuts) {
        this.shortcuts = shortcuts;
    }

    @Override
    public boolean onPreKeyEvent(CefBrowser browser, CefKeyboardHandler.CefKeyEvent event,
                                 BoolRef is_keyboard_shortcut) {
        if (event.type != CefKeyboardHandler.CefKeyEvent.EventType.KEYEVENT_RAWKEYDOWN) return false;
        if ((event.modifiers & CTRL_SHIFT) != CTRL_SHIFT) return false;

        Runnable action = shortcuts.get(event.windows_key_code);
        if (action == null) return false;

        SwingUtilities.invokeLater(action);
        return true;
    }
}
