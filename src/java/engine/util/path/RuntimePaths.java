package engine.util.path;

import java.nio.file.Path;

public final class RuntimePaths {

    private static final Path ROOT = Path.of(System.getProperty("user.home"), "Documents", "DotRuby");
    private static final Path CONFIG_DIRECTORY = ROOT.resolve("config");
    private static final Path LOG_DIRECTORY = ROOT.resolve("journal");
    private static final Path LEGACY_CONFIG_DIRECTORY = ROOT.resolve("cfg");
    private static final Path LEGACY_LOG_DIRECTORY = ROOT.resolve("log");

    private RuntimePaths() {
    }

    public static Path root() {
        return ROOT;
    }

    public static Path configDirectory() {
        return CONFIG_DIRECTORY;
    }

    public static Path logDirectory() {
        return LOG_DIRECTORY;
    }

    public static Path configPath(String fileName) {
        return CONFIG_DIRECTORY.resolve(fileName);
    }

    public static Path logPath(String fileName) {
        return LOG_DIRECTORY.resolve(fileName);
    }

    public static Path legacyConfigDirectory() {
        return LEGACY_CONFIG_DIRECTORY;
    }

    public static Path legacyLogDirectory() {
        return LEGACY_LOG_DIRECTORY;
    }

    public static Path legacyConfigPath(String fileName) {
        return LEGACY_CONFIG_DIRECTORY.resolve(fileName);
    }

    public static Path legacyLogPath(String fileName) {
        return LEGACY_LOG_DIRECTORY.resolve(fileName);
    }
}
