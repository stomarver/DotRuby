package engine.visual;

import engine.visual.scene.SceneIds;
import engine.visual.scene.SceneManager;
import engine.visual.scene.temp.LayoutLabScene;
import engine.visual.scene.temp.Prompt;

public final class Manager {

    private final Render textRender = new Render();
    private final SceneManager sceneManager = new SceneManager();
    private boolean textRenderLoaded;

    public void initialize() {
        sceneManager.register(new Prompt());
        sceneManager.register(new LayoutLabScene());
        sceneManager.activate(SceneIds.LOG_PROMPT);
        syncSceneResources();
    }

    public void activateSceneByHotkey(int sceneHotkey) {
        switch (sceneHotkey) {
            case 1 -> sceneManager.activate(SceneIds.LOG_PROMPT);
            case 2 -> sceneManager.activate(SceneIds.LAYOUT_LAB);
            default -> {
            }
        }
        syncSceneResources();
    }

    public void update(float deltaSeconds) {
        sceneManager.update(deltaSeconds);
    }

    public void render2D(Overlay overlay, float configuredVirtualScale) {
        textRender.setConfiguredVirtualScale(configuredVirtualScale);
        sceneManager.render2DPass(overlay, textRender);
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
