package engine.visual.scene;

import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SceneBindings {

    private static final Path SCENE_DIR = Path.of("src", "assets", "scene");

    private SceneBindings() {
    }

    public static List<SceneBinding> load() {
        if (!Files.isDirectory(SCENE_DIR)) {
            return List.of();
        }

        try {
            List<SceneBinding> bindings = new ArrayList<>();
            for (Path path : Files.list(SCENE_DIR).filter(file -> file.toString().endsWith(".scn")).sorted().toList()) {
                bindings.add(parse(path));
            }
            return bindings;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load scene bindings from " + SCENE_DIR, exception);
        }
    }

    private static SceneBinding parse(Path path) {
        try {
            Map<String, String> values = new HashMap<>();
            for (String line : Files.readAllLines(path)) {
                String normalized = line.trim();
                if (normalized.isBlank() || normalized.startsWith("#") || !normalized.contains("=")) {
                    continue;
                }
                String[] parts = normalized.split("=", 2);
                values.put(parts[0].trim().toLowerCase(Locale.ROOT), parts[1].trim());
            }
            String id = values.get("id");
            int key = parseHotkey(values.get("hotkey"));
            String description = values.getOrDefault("description", "");
            if (id == null) {
                throw new IllegalArgumentException("Missing id in " + path);
            }
            return new SceneBinding(id, key, description);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to parse scene binding " + path, exception);
        }
    }

    private static int parseHotkey(String value) {
        if (value == null || value.isBlank()) {
            return GLFW.GLFW_KEY_UNKNOWN;
        }
        String token = value.toUpperCase(Locale.ROOT);
        if (token.matches("F\\d+")) {
            int index = Integer.parseInt(token.substring(1));
            return GLFW.GLFW_KEY_F1 + Math.max(0, index - 1);
        }
        return GLFW.GLFW_KEY_UNKNOWN;
    }
}
