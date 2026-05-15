package engine.visual;

import engine.visual.scene.temp.Prompt;
import engine.visual.scene.temp.PortalGridScene;
import engine.visual.scene.temp.RotatingCubeScene;
import engine.visual.scene.SceneIds;
import engine.visual.scene.SceneManager;
import engine.visual.scene.SceneType;

public final class Manager {

    private final Render textRender = new Render();
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
        sceneManager.render3DPass();
    }

    public void render2D(Overlay overlay, float configuredVirtualScale) {
        textRender.setConfiguredVirtualScale(configuredVirtualScale);
        sceneManager.render2DPass(overlay, textRender);
    }

    public void toggleLightingMode() {
        if (sceneManager.activeSceneType().requires3D() && sceneManager.getActiveScene() instanceof engine.visual.scene.temp.PortalGridScene portalGridScene) {
            portalGridScene.toggleLightingMode();
        }
    }

    public void destroy() {
        sceneManager.destroy();
        if (textRenderLoaded) {
            textRender.destroy();
            textRenderLoaded = false;
        }
    }

    private void syncSceneResources() {
        SceneType type = sceneManager.activeSceneType();
        if (type.requires2D() && !textRenderLoaded) {
            textRender.load();
            textRenderLoaded = true;
            return;
        }
        if (!type.requires2D() && textRenderLoaded) {
            textRender.destroy();
            textRenderLoaded = false;
        }
    }
}


