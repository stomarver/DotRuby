package engine.visual;

import engine.visual.scene.Prompt;
import engine.visual.scene.PortalGridScene;
import engine.visual.scene.RotatingCubeScene;
import engine.visual.scene.Scene;
import engine.visual.scene.SceneIds;
import engine.visual.scene.SceneManager;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class Manager {

    private final Render textRender = new Render();
    private final PerformanceOverlay performanceOverlay = new PerformanceOverlay();
    private final SceneManager sceneManager = new SceneManager();
    private final List<String> orderedSceneIds = new ArrayList<>();
    private boolean textRenderLoaded;

    public void initialize() {
        registerScenesFromScn();
        sceneManager.activate(SceneIds.LOG_PROMPT);
        syncSceneResources();
    }

    public void activateSceneByHotkey(int sceneHotkey) {
        switch (sceneHotkey) {
            case 1 -> sceneManager.activate(SceneIds.LOG_PROMPT);
            case 2 -> sceneManager.activate(SceneIds.ROTATING_CUBE);
            case 3 -> sceneManager.activate(SceneIds.PORTAL_GRID);
            default -> {
            }
        }
        syncSceneResources();
    }

    public void activateSceneByIndex(int sceneIndex) {
        if (sceneIndex < 0 || sceneIndex >= orderedSceneIds.size()) {
            return;
        }
        sceneManager.activate(orderedSceneIds.get(sceneIndex));
        syncSceneResources();
    }

    public void render3D() {
        sceneManager.update(1f / 60f);
        performanceOverlay.apply(performanceOverlay.pollUpdateEvent());
        sceneManager.render3DPass();
    }

    public void render2D(Overlay overlay) {
        sceneManager.render2DPass(overlay, textRender);
        performanceOverlay.render(overlay, textRender);
    }

    public void destroy() {
        sceneManager.destroy();
        if (textRenderLoaded) {
            textRender.destroy();
            textRenderLoaded = false;
        }
    }

    private void registerScenesFromScn() {
        List<Path> manifests = listSceneManifests();
        for (Path manifest : manifests) {
            String sceneId = parseSceneId(manifest);
            Scene scene = createSceneById(sceneId);
            if (scene != null) {
                sceneManager.register(scene);
                orderedSceneIds.add(scene.id());
            }
        }
    }

    private List<Path> listSceneManifests() {
        try {
            URI sceneDir = Manager.class.getResource("/scene").toURI();
            if ("jar".equals(sceneDir.getScheme())) {
                try (FileSystem fs = FileSystems.newFileSystem(sceneDir, Map.of())) {
                    Path pathInJar = fs.getPath("/scene");
                    return Files.list(pathInJar).filter(path -> path.toString().endsWith(".scn")).sorted().toList();
                }
            }
            return Files.list(Path.of(sceneDir)).filter(path -> path.toString().endsWith(".scn")).sorted().toList();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to discover .scn scene manifests", exception);
        }
    }

    private static String parseSceneId(Path manifest) {
        try {
            return Files.readAllLines(manifest).stream()
                    .map(String::trim)
                    .filter(line -> line.startsWith("id="))
                    .map(line -> line.substring(3))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Missing id= in " + manifest));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to parse scene manifest " + manifest, exception);
        }
    }

    private static Scene createSceneById(String sceneId) {
        return switch (sceneId) {
            case SceneIds.LOG_PROMPT -> new Prompt();
            case SceneIds.ROTATING_CUBE -> new RotatingCubeScene();
            case SceneIds.PORTAL_GRID -> new PortalGridScene();
            default -> null;
        };
    }

    private void syncSceneResources() {
        if (!textRenderLoaded) {
            textRender.load();
            textRenderLoaded = true;
        }
    }
}
