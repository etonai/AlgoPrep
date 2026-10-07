package com.algoprep;

import com.algoprep.bridge.ChatGptBridge;
import com.algoprep.browser.BrowserClient;
import com.algoprep.browser.BrowserKeyboardHandler;
import com.algoprep.browser.BrowserPanel;
import com.algoprep.browser.DomBridge;
import com.algoprep.config.AppConfig;
import com.algoprep.config.SettingsStore;
import com.algoprep.display.FontScaleModel;
import com.algoprep.problem.ProblemWorkspace;
import com.algoprep.theme.NativeThemeModel;
import com.algoprep.ui.StatusReporter;
import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.impl.progress.ConsoleProgressHandler;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;

import javax.swing.*;
import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Main {

    public static void main(String[] args) throws Exception {
        AppConfig config = new AppConfig();
        AppState  appState = new AppState();

        // The store can report a bad settings file before the window exists, so messages are
        // buffered by the reporter until AppFrame attaches the status label.
        StatusReporter statusReporter = new StatusReporter();
        SettingsStore settings = new SettingsStore(Path.of(config.getSettingsFilePath()), statusReporter::report);

        NativeThemeModel themeModel = new NativeThemeModel();
        themeModel.setTheme(settings.getTheme());
        themeModel.addListener((previous, current) -> settings.setTheme(current));

        // The display text size is remembered across restarts. A missing or invalid saved value
        // means the normal size.
        FontScaleModel fontScale = new FontScaleModel();
        fontScale.restore(settings.getFontScalePercent());
        fontScale.addListener(() -> settings.setFontScalePercent(fontScale.percent()));

        // Scans the saved PROBLEMS directory and restores the last selection, if still found
        ProblemWorkspace problems = new ProblemWorkspace(settings, statusReporter::report);

        System.out.println("AlgoPrep starting...");
        System.out.println("  Profile  : " + config.getProfilePath());
        System.out.println("  Settings : " + config.getSettingsFilePath());
        System.out.println("  Config   : " + config.getConfigFilePath());
        System.out.println("  Target   : " + config.getTargetUrl());
        System.out.println("  Staging  : " + config.getDefaultStagingRootPath());

        // Build JCEF — preserving the exact init order proven in ChatStory DC001
        CefAppBuilder builder = new CefAppBuilder();
        builder.setInstallDir(new File("jcef-bundle"));
        builder.setProgressHandler(new ConsoleProgressHandler());
        builder.getCefSettings().windowless_rendering_enabled = false;
        builder.getCefSettings().cache_path = config.getProfilePath();

        CefApp cefApp;
        try {
            cefApp = builder.build();
        } catch (Exception e) {
            System.err.println("JCEF initialization failed: " + e.getMessage());
            System.err.println("See BUILDING.md for setup instructions.");
            System.exit(1);
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Disposing CefApp...");
            CefApp.getInstance().dispose();
        }, "cef-shutdown"));

        // Wire up client, bridge, and browser in the order that ChatStory DC001 proved works
        CefClient     client        = cefApp.createClient();
        DomBridge     domBridge     = new DomBridge(client);
        BrowserClient browserClient = new BrowserClient(appState, domBridge);
        client.addLoadHandler(browserClient);

        // Register keyboard handler before createBrowser so JCEF wires the native callback
        Map<Integer, Runnable> browserShortcuts = new ConcurrentHashMap<>();
        client.addKeyboardHandler(new BrowserKeyboardHandler(browserShortcuts));

        CefBrowser    browser       = client.createBrowser(config.getTargetUrl(), false, false);
        BrowserPanel  browserPanel  = new BrowserPanel(browser);
        ChatGptBridge chatBridge    = new ChatGptBridge(domBridge, browser, appState);

        SwingUtilities.invokeLater(() ->
                new AppFrame(appState, browserPanel, browser, chatBridge, settings, problems,
                        config.getDefaultStagingRootPath(), themeModel, fontScale,
                        statusReporter, browserShortcuts));
    }
}
