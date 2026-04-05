package engine.visual.scene;

import engine.visual.Overlay;
import engine.visual.Render;
import engine.util.resource.Unloader;

public interface Scene {

    String id();

    SceneType type();

    void initialize(Unloader resources);

    void update(float deltaSeconds);

    void render3D();

    void render2D(Overlay overlay, Render textRender);

    void destroy();
}
