package engine.visual.scene;

import engine.visual.Overlay;
import engine.visual.Render;

public interface Scene {

    String id();

    void initialize();

    void update(float deltaSeconds);

    void render(Overlay overlay, Render textRender);

    void destroy();
}
