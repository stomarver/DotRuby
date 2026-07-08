package engine.visual.scene;

import engine.util.resource.Unloader;
import engine.visual.Overlay;
import engine.visual.Render;

public interface Scene {

    String id();

    SceneType type();

    void initialize(Unloader resources);

    void update(float deltaSeconds);

    void render2D(Overlay overlay, Render textRender);

    void destroy();
}
