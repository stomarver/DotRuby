package engine.visual.scene.demo;

import engine.visual.scene.Scene;

public final class RotatingCubeScene implements Scene {

    private float rotationY;

    @Override
    public String id() {
        return "demo.rotating-cube";
    }

    @Override
    public void initialize() {
        rotationY = 0f;
    }

    @Override
    public void update(float deltaSeconds) {
        rotationY += deltaSeconds * 1.0f;
    }

    @Override
    public void render() {
        // TODO integrate mesh/material/shader pipeline and draw cube with rotationY.
    }

    @Override
    public void destroy() {
        // TODO release scene resources when demo pipeline is connected.
    }

    public float rotationY() {
        return rotationY;
    }
}
