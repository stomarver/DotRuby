package engine.util.path;

import java.nio.file.Path;

public final class RuntimePaths {

    private static final Path ROOT = Path.of(System.getProperty("user.home"), "Documents", "DotRuby");
    private static final Path CONFIG_DIRECTORY = ROOT.resolve("config");
    private static final Path LOG_DIRECTORY = ROOT.resolve("journal");

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
