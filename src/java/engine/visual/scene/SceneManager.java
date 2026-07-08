package engine.visual.scene;

import engine.util.resource.Unloader;
import engine.visual.Overlay;
import engine.visual.Render;

import java.util.HashMap;
import java.util.Map;

public final class SceneManager {

    private final Map<String, Scene> scenes = new HashMap<>();
    private Scene activeScene;
    private final Unloader activeSceneResources = new Unloader();

    public void register(Scene scene) {
        scenes.put(scene.id(), scene);
    }

    public void activate(String id) {
        if (activeScene != null && activeScene.id().equals(id)) {
            return;
        }

        if (activeScene != null) {
            activeSceneResources.disposeAll();
            activeScene.destroy();
            activeSceneResources.clear();
        }

        activeScene = scenes.get(id);
        if (activeScene != null) {
            activeScene.initialize(activeSceneResources);
        }
    }

    public void update(float deltaSeconds) {
        if (activeScene != null) {
            activeScene.update(deltaSeconds);
        }
    }

    public void render2DPass(Overlay overlay, Render textRender) {
        if (activeScene != null) {
            activeScene.render2D(overlay, textRender);
        }
    }

    public Scene getActiveScene() {
        return activeScene;
    }

    public void destroy() {
        if (activeScene != null) {
            activeSceneResources.disposeAll();
            activeScene.destroy();
            activeScene = null;
        }
        activeSceneResources.clear();
        scenes.clear();
    }
}
