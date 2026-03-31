package engine.visual.scene;

import engine.visual.Overlay;
import engine.visual.Render;
import engine.util.ResourceDisposer;

public interface Scene {

    String id();

    SceneType type();

    void initialize(ResourceDisposer resources);

    void update(float deltaSeconds);

    void render3D();

    void render2D(Overlay overlay, Render textRender);

    void destroy();
}
