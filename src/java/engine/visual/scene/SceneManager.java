package engine.visual.scene;

import java.util.HashMap;
import java.util.Map;

public final class SceneManager {

    private final Map<String, Scene> scenes = new HashMap<>();
    private Scene activeScene;

    public void register(Scene scene) {
        scenes.put(scene.id(), scene);
    }

    public void activate(String id) {
        if (activeScene != null) {
            activeScene.destroy();
        }

        activeScene = scenes.get(id);
        if (activeScene != null) {
            activeScene.initialize();
        }
    }

    public void update(float deltaSeconds) {
        if (activeScene != null) {
            activeScene.update(deltaSeconds);
        }
    }

    public void render() {
        if (activeScene != null) {
            activeScene.render();
        }
    }
}
