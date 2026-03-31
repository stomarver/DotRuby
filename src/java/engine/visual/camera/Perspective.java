package engine.visual.camera;

import org.joml.Matrix4f;

public final class Perspective {

    private float fovRadians;
    private float aspect;
    private float near;
    private float far;

    public Perspective(float fovRadians, float aspect, float near, float far) {
        this.fovRadians = fovRadians;
        this.aspect = aspect;
        this.near = near;
        this.far = far;
    }

    public Matrix4f projection() {
        return new Matrix4f().perspective(fovRadians, aspect, near, far);
    }

    public void setAspect(float aspect) {
        this.aspect = aspect;
    }
}
