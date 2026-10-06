package com.chatstory;

import com.chatstory.browser.BrowserClient;
import com.chatstory.browser.BrowserKeyboardHandler;
import com.chatstory.browser.BrowserPanel;
import com.chatstory.browser.DomBridge;
import com.chatstory.bridge.ChatGptBridge;
import com.chatstory.config.AppConfig;
import com.chatstory.canon.CanonFolderStore;
import com.chatstory.picture.PictureFileStore;
import com.chatstory.context.ContextFileStore;
import com.chatstory.controller.FinalControllerStore;
import com.chatstory.controller.IntermediateControllerStore;
import com.chatstory.controller.SessionControllerStore;
import com.chatstory.rules.RulesFileStore;
import com.chatstory.session.RedoCountStore;
import com.chatstory.session.SnapshotFileStore;
import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.impl.progress.ConsoleProgressHandler;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;

import javax.swing.*;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Main {

    public static void main(String[] args) throws Exception {
        AppConfig config = new AppConfig();
        AppState  appState = new AppState();

        ContextFileStore contextFileStore = new ContextFileStore(
                config.getContextFileListPath(), config.getStagingFolderPath());
        CanonFolderStore canonFolderStore = new CanonFolderStore(config.getCanonConfigPath());
        PictureFileStore pictureFileStore = new PictureFileStore(config.getPictureConfigPath());
        SessionControllerStore sessionControllerStore =
                new SessionControllerStore(config.getSessionControllerConfigPath());
        IntermediateControllerStore intermediateControllerStore =
                new IntermediateControllerStore(config.getIntermediateControllerConfigPath());
        FinalControllerStore finalControllerStore =
                new FinalControllerStore(config.getFinalControllerConfigPath());
        RulesFileStore rulesFileStore = new RulesFileStore(config.getRulesFileListPath());
        RedoCountStore redoCountStore = new RedoCountStore(config.getRedoCountPath());
        SnapshotFileStore snapshotFileStore = new SnapshotFileStore(config.getSnapshotFileConfigPath());

        System.out.println("Story Workstation starting...");
        System.out.println("  Profile : " + config.getProfilePath());
        System.out.println("  Config  : " + config.getConfigFilePath());
        System.out.println("  Target  : " + config.getTargetUrl());
        System.out.println("  Context : " + config.getContextFileListPath());
        System.out.println("  Staging : " + config.getStagingFolderPath());

        // Build JCEF — preserving the exact init order proven in DC001
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

        // Wire up client, bridge, and browser in the order that DC001 proved works
        CefClient    client      = cefApp.createClient();
        DomBridge    domBridge   = new DomBridge(client);
        BrowserClient browserClient = new BrowserClient(appState, domBridge);
        client.addLoadHandler(browserClient);

        // Register keyboard handler before createBrowser so JCEF wires the native callback
        Map<Integer, Runnable> browserShortcuts = new ConcurrentHashMap<>();
        client.addKeyboardHandler(new BrowserKeyboardHandler(browserShortcuts));

        CefBrowser   browser     = client.createBrowser(config.getTargetUrl(), false, false);
        BrowserPanel browserPanel = new BrowserPanel(browser);
        ChatGptBridge chatBridge = new ChatGptBridge(domBridge, browser, appState);

        SwingUtilities.invokeLater(() ->
                new AppFrame(appState, browserPanel, browser, chatBridge, client,
                        contextFileStore, canonFolderStore, pictureFileStore,
                        sessionControllerStore, intermediateControllerStore, finalControllerStore,
                        rulesFileStore, redoCountStore, snapshotFileStore, browserShortcuts));
    }
}
