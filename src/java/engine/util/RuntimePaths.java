package engine.util;

import java.nio.file.Path;

public final class RuntimePaths {

    private static final Path ROOT = Path.of(System.getProperty("user.home"), "Documents", "DotRuby");
    private static final Path CONFIG_DIRECTORY = ROOT.resolve("cfg");
    private static final Path LOG_DIRECTORY = ROOT.resolve("log");

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
}
