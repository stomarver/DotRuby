package engine.visual.scene;

import engine.visual.Overlay;
import engine.visual.Render;
import engine.util.res.Unloader;

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

    public void render3DPass() {
        if (activeScene != null && activeScene.type().requires3D()) {
            activeScene.render3D();
        }
    }

    public void render2DPass(Overlay overlay, Render textRender) {
        if (activeScene != null && activeScene.type().requires2D()) {
            activeScene.render2D(overlay, textRender);
        }
    }

    public SceneType activeSceneType() {
        return activeScene == null ? SceneType.TWO_D : activeScene.type();
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
