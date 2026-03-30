package engine.visual;

import engine.visual.scene.LogPromptScene;
import engine.visual.scene.PortalGridScene;
import engine.visual.scene.RotatingCubeScene;
import engine.visual.scene.SceneManager;

public final class Manager {

    private final Render textRender = new Render();
    private final SceneManager sceneManager = new SceneManager();

    public void initialize() {
        textRender.load();
        sceneManager.register(new LogPromptScene());
        sceneManager.register(new RotatingCubeScene());
        sceneManager.register(new PortalGridScene());
        sceneManager.activate("scene.log-prompt");
    }

    public void activateSceneByHotkey(int sceneHotkey) {
        switch (sceneHotkey) {
            case 1 -> sceneManager.activate("scene.log-prompt");
            case 2 -> sceneManager.activate("scene.rotating-cube");
            case 3 -> sceneManager.activate("scene.portal-grid");
            default -> {
            }
        }
    }

    public void render(Overlay overlay) {
        sceneManager.update(1f / 60f);
        sceneManager.render(overlay, textRender);
    }

    public void destroy() {
        sceneManager.destroy();
        textRender.destroy();
    }
}
