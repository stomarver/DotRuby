package engine.visual;

import engine.visual.scene.Prompt;
import engine.visual.scene.PortalGridScene;
import engine.visual.scene.RotatingCubeScene;
import engine.visual.scene.SceneIds;
import engine.visual.scene.SceneManager;

public final class Manager {

    private final Render textRender = new Render();
    private final PerformanceOverlay performanceOverlay = new PerformanceOverlay();
    private final SceneManager sceneManager = new SceneManager();
    private boolean textRenderLoaded;

    public void initialize() {
        sceneManager.register(new Prompt());
        sceneManager.register(new RotatingCubeScene());
        sceneManager.register(new PortalGridScene());
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
