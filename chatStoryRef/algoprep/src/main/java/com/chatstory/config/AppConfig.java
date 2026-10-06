package com.chatstory.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Read-only configuration resolved at construction time.
 * Startup never fails due to a missing or malformed config file.
 */
public final class AppConfig {

    private static final String APP_NAME    = "ChatStory";
    private static final String DEFAULT_URL = "https://chatgpt.com";

    private final String targetUrl;
    private final String profilePath;
    private final String configFilePath;
    private final String contextFileListPath;
    private final String stagingFolderPath;
    private final String canonConfigPath;
    private final String sessionControllerConfigPath;
    private final String intermediateControllerConfigPath;
    private final String finalControllerConfigPath;
    private final String rulesFileListPath;
    private final String pictureConfigPath;
    private final String redoCountPath;
    private final String snapshotFileConfigPath;

    public AppConfig() {
        String localAppData = System.getenv("LOCALAPPDATA");
        String appData      = System.getenv("APPDATA");
        String userHome     = System.getProperty("user.home");

        // Profile: %LOCALAPPDATA%\ChatStory\profile  (large cache, should not roam)
        String profileBase = (localAppData != null && !localAppData.isBlank())
                ? localAppData : userHome;
        File appLocalDir  = new File(profileBase, APP_NAME);
        File profileDir   = new File(appLocalDir, "profile");
        profilePath = profileDir.getAbsolutePath();

        // Config: %APPDATA%\ChatStory\config.properties  (user settings, may roam)
        String configBase = (appData != null && !appData.isBlank()) ? appData : userHome;
        File appRoamDir   = new File(configBase, APP_NAME);
        File configFile   = new File(appRoamDir, "config.properties");
        configFilePath = configFile.getAbsolutePath();

        contextFileListPath          = new File(appRoamDir, "context-files.json").getAbsolutePath();
        stagingFolderPath            = new File(appLocalDir, "context-staging").getAbsolutePath();
        canonConfigPath              = new File(appRoamDir, "canon-config.json").getAbsolutePath();
        sessionControllerConfigPath      = new File(appRoamDir, "session-controller.json").getAbsolutePath();
        intermediateControllerConfigPath = new File(appRoamDir, "intermediate-controller.json").getAbsolutePath();
        finalControllerConfigPath        = new File(appRoamDir, "final-controller.json").getAbsolutePath();
        rulesFileListPath                = new File(appRoamDir, "rules-files.json").getAbsolutePath();
        pictureConfigPath                = new File(appRoamDir, "picture-config.json").getAbsolutePath();
        redoCountPath                    = new File(appRoamDir, "redo-count.json").getAbsolutePath();
        snapshotFileConfigPath           = new File(appRoamDir, "snapshot-file.json").getAbsolutePath();

        // Create application directories now so JCEF and config reads don't fail
        ensureDir(appLocalDir);
        ensureDir(profileDir);
        if (!appRoamDir.getAbsolutePath().equals(appLocalDir.getAbsolutePath())) {
            ensureDir(appRoamDir);
        }

        targetUrl = readTargetUrl(configFile);
    }

    private String readTargetUrl(File configFile) {
        if (!configFile.exists()) {
            System.out.println("[AppConfig] No config file found at " + configFile.getPath()
                    + " — using default URL");
            return DEFAULT_URL;
        }
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(configFile)) {
            props.load(fis);
            String url = props.getProperty("target.chat.url");
            if (url == null || url.isBlank()) {
                System.out.println("[AppConfig] target.chat.url not set — using default URL");
                return DEFAULT_URL;
            }
            return url.trim();
        } catch (IOException e) {
            System.err.println("[AppConfig] Failed to read config: " + e.getMessage()
                    + " — using default URL");
            return DEFAULT_URL;
        }
    }

    private void ensureDir(File dir) {
        if (!dir.exists() && !dir.mkdirs()) {
            System.err.println("[AppConfig] Could not create directory: " + dir.getAbsolutePath());
        }
    }

    public String getTargetUrl()                   { return targetUrl; }
    public String getProfilePath()                 { return profilePath; }
    public String getConfigFilePath()              { return configFilePath; }
    public String getContextFileListPath()         { return contextFileListPath; }
    public String getStagingFolderPath()           { return stagingFolderPath; }
    public String getCanonConfigPath()             { return canonConfigPath; }
    public String getSessionControllerConfigPath()      { return sessionControllerConfigPath; }
    public String getIntermediateControllerConfigPath() { return intermediateControllerConfigPath; }
    public String getFinalControllerConfigPath()        { return finalControllerConfigPath; }
    public String getRulesFileListPath()                { return rulesFileListPath; }
    public String getPictureConfigPath()                { return pictureConfigPath; }
    public String getRedoCountPath()                    { return redoCountPath; }
    public String getSnapshotFileConfigPath()            { return snapshotFileConfigPath; }
}
