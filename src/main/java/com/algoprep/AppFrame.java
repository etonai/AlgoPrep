package com.algoprep;

import com.algoprep.bridge.ChatGptBridge;
import com.algoprep.browser.BrowserPanel;
import com.algoprep.config.SettingsStore;
import com.algoprep.display.FontScaleModel;
import com.algoprep.problem.ProblemWorkspace;
import com.algoprep.theme.NativeThemeApplier;
import com.algoprep.theme.NativeThemeModel;
import com.algoprep.ui.DisplayPanel;
import com.algoprep.ui.MainPanel;
import com.algoprep.ui.ProblemsPanel;
import com.algoprep.ui.SettingsPanel;
import com.algoprep.ui.StatusReporter;
import com.algoprep.ui.UploadController;
import org.cef.browser.CefBrowser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Map;

/**
 * Three-pane window: display (left), embedded ChatGPT (middle), control tabs (right).
 * The local panes are built independently of the browser, so they work while ChatGPT is loading,
 * signed out or failed (Plan section 4).
 */
public class AppFrame extends JFrame {

    private static final int FRAME_WIDTH = 1400;
    private static final int FRAME_HEIGHT = 900;
    // About 0.30 / 0.45 / 0.25 of the width (Plan section 4)
    private static final int LEFT_WIDTH = 420;
    private static final int BROWSER_WIDTH = 630;

    private final JLabel statusLabel;
    private final NativeThemeApplier themeApplier = new NativeThemeApplier();

    public AppFrame(AppState appState, BrowserPanel browserPanel, CefBrowser browser,
                    ChatGptBridge chatBridge, SettingsStore settings, ProblemWorkspace problems,
                    String defaultStagingRoot,
                    NativeThemeModel themeModel, FontScaleModel fontScale,
                    StatusReporter statusReporter, Map<Integer, Runnable> browserShortcuts) {
        super("AlgoPrep");

        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        statusLabel = new JLabel(" Starting...");
        statusLabel.setForeground(Color.DARK_GRAY);

        DisplayPanel displayPanel = new DisplayPanel(problems.selection(), settings, themeModel, fontScale);

        // One Upload implementation, shared by the MAIN tab and the Problems tab
        UploadController uploads = new UploadController(appState, settings, chatBridge,
                statusReporter, problems.selection(), defaultStagingRoot);

        JTabbedPane rightTabs = new JTabbedPane();
        rightTabs.addTab("MAIN", new MainPanel(appState, settings, chatBridge, statusReporter,
                problems.selection(), uploads));
        rightTabs.addTab("Problems", new ProblemsPanel(problems, uploads));
        rightTabs.addTab("Settings", new SettingsPanel(themeModel, problems, settings, defaultStagingRoot));

        JButton devToolsBtn = new JButton("DevTools");
        devToolsBtn.setToolTipText("Open Chromium DevTools for this page");
        devToolsBtn.addActionListener(e -> browser.openDevTools());

        JPanel leftTools = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        leftTools.add(devToolsBtn);

        JPanel toolbar = new JPanel(new BorderLayout(6, 0));
        toolbar.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        toolbar.add(leftTools, BorderLayout.WEST);
        toolbar.add(statusLabel, BorderLayout.CENTER);

        JSplitPane browserAndRight = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                browserPanel.getUIComponent(),
                rightTabs);
        browserAndRight.setResizeWeight(0.64);
        browserAndRight.setDividerLocation(BROWSER_WIDTH);

        JSplitPane mainSplit = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                displayPanel,
                browserAndRight);
        mainSplit.setResizeWeight(0.30);
        mainSplit.setDividerLocation(LEFT_WIDTH);

        add(toolbar, BorderLayout.NORTH);
        add(mainSplit, BorderLayout.CENTER);

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
                System.exit(0);
            }
        });

        appState.addListener((prev, current) ->
                UiThread.run(() -> statusLabel.setText(" " + labelFor(current))));
        themeModel.addListener((prev, current) ->
                UiThread.run(() -> themeApplier.apply(this, current)));
        // Delivers anything reported before the window existed (for example a bad settings file)
        statusReporter.attach(message -> UiThread.run(() -> statusLabel.setText(" " + message)));

        Runnable onFocusBrowser = () -> {
            browser.setFocus(true);
            browser.executeJavaScript("document.activeElement.blur();", browser.getURL(), 0);
        };

        // Only recovery and focus for now. The rest of the shortcuts are DC5 (Plan section 12).
        browserShortcuts.put(KeyEvent.VK_X, chatBridge::reset);
        browserShortcuts.put(KeyEvent.VK_B, onFocusBrowser);
        installKeyboardShortcuts(browserShortcuts);

        setVisible(true);
        UiThread.run(() -> themeApplier.apply(this, themeModel.current()));
    }

    private void installKeyboardShortcuts(Map<Integer, Runnable> shortcuts) {
        int ctrlShift = InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK;
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() != KeyEvent.KEY_PRESSED) return false;
            if ((e.getModifiersEx() & ctrlShift) != ctrlShift) return false;
            Runnable action = shortcuts.get(e.getKeyCode());
            if (action == null) return false;
            action.run();
            return true;
        });
    }

    private String labelFor(AppState.State state) {
        switch (state) {
            case Starting:           return "Starting...";
            case LoadingChatGPT:     return "Loading ChatGPT...";
            case NeedsLogin:         return "Please log in to ChatGPT";
            case Ready:              return "Ready";
            case InjectingPrompt:    return "Injecting prompt...";
            case Sending:            return "Sending prompt...";
            case WaitingForResponse: return "Waiting for response...";
            case Complete:           return "Response complete";
            case Error:              return "Error - check console";
            default:                 return state.name();
        }
    }
}
