package engine.visual.scene;

import engine.util.res.ResourceDisposer;
import engine.visual.Overlay;
import engine.visual.Render;

public abstract class SceneTemplate implements Scene {

    private final String id;
    private final SceneType type;

    protected SceneTemplate(String id, SceneType type) {
        this.id = id;
        this.type = type;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public SceneType type() {
        return type;
    }

    @Override
    public void initialize(ResourceDisposer resources) {
    }

    @Override
    public void update(float deltaSeconds) {
    }

    @Override
    public void render3D() {
    }

    @Override
    public void render2D(Overlay overlay, Render textRender) {
    }

    @Override
    public void destroy() {
    }
}
