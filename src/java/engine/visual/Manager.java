package engine.visual;

import engine.visual.scene.Prompt;
import engine.visual.scene.PortalGridScene;
import engine.visual.scene.RotatingCubeScene;
import engine.visual.scene.ShadowScene;
import engine.visual.scene.SceneIds;
import engine.visual.scene.SceneManager;
import engine.visual.scene.SceneBinding;
import engine.visual.scene.SceneBindings;

import java.util.HashMap;
import java.util.Map;

public final class Manager {

    private final Render textRender = new Render();
    private final PerformanceOverlay performanceOverlay = new PerformanceOverlay();
    private final SceneManager sceneManager = new SceneManager();
    private boolean textRenderLoaded;
    private final Map<Integer, String> sceneByHotkey = new HashMap<>();

    public void initialize() {
        sceneManager.register(new Prompt());
        sceneManager.register(new RotatingCubeScene());
        sceneManager.register(new PortalGridScene());
        sceneManager.register(new ShadowScene());
        loadSceneBindings();
        sceneManager.activate(SceneIds.LOG_PROMPT);
        syncSceneResources();
    }

    public void activateSceneByHotkey(int sceneHotkey) {
        String sceneId = sceneByHotkey.get(sceneHotkey);
        if (sceneId != null) {
            sceneManager.activate(sceneId);
            syncSceneResources();
        }
    }

    private void loadSceneBindings() {
        sceneByHotkey.clear();
        for (SceneBinding binding : SceneBindings.load()) {
            if (binding.hotkey() > 0) {
                sceneByHotkey.put(binding.hotkey(), binding.id());
            }
        }
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

    private void syncSceneResources() {
        if (!textRenderLoaded) {
            textRender.load();
            textRenderLoaded = true;
        }
    }
}
