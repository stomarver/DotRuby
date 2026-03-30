package engine.visual.scene;

import engine.visual.Overlay;
import engine.visual.Render;
import engine.util.ResourceDisposer;

public interface Scene {

    String id();

    void initialize(ResourceDisposer resources);

    void update(float deltaSeconds);

    void render(Overlay overlay, Render textRender);

    void destroy();
}
