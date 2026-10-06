package com.algoprep.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Read-only configuration resolved at construction time.
 * Startup never fails due to a missing or malformed config file.
 */
public final class AppConfig {

    private static final String APP_NAME    = "AlgoPrep";
    private static final String DEFAULT_URL = "https://chatgpt.com";

    private final String targetUrl;
    private final String profilePath;
    private final String configFilePath;
    private final String settingsFilePath;
    private final String defaultStagingRootPath;

    public AppConfig() {
        this(System.getenv("LOCALAPPDATA"), System.getenv("APPDATA"), System.getProperty("user.home"));
    }

    /** Test seam: base folders are passed in. A blank localAppData or appData falls back to userHome. */
    AppConfig(String localAppData, String appData, String userHome) {
        // Profile: %LOCALAPPDATA%\AlgoPrep\profile  (large cache, should not roam)
        String profileBase = (localAppData != null && !localAppData.isBlank())
                ? localAppData : userHome;
        File appLocalDir  = new File(profileBase, APP_NAME);
        File profileDir   = new File(appLocalDir, "profile");
        profilePath = profileDir.getAbsolutePath();

        // Settings and config: %APPDATA%\AlgoPrep  (user settings, may roam)
        String configBase = (appData != null && !appData.isBlank()) ? appData : userHome;
        File appRoamDir   = new File(configBase, APP_NAME);
        File configFile   = new File(appRoamDir, "config.properties");
        configFilePath   = configFile.getAbsolutePath();
        settingsFilePath = new File(appRoamDir, "settings.json").getAbsolutePath();

        // Default staging root; AlgoPrep stages into its own subfolder beneath it (Plan section 8.2)
        defaultStagingRootPath = new File(appLocalDir, "upload-staging").getAbsolutePath();

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
        } catch (IOException | IllegalArgumentException e) {
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

    public String getTargetUrl()               { return targetUrl; }
    public String getProfilePath()             { return profilePath; }
    public String getConfigFilePath()          { return configFilePath; }
    public String getSettingsFilePath()        { return settingsFilePath; }
    public String getDefaultStagingRootPath()  { return defaultStagingRootPath; }
}
