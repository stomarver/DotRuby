package engine.visual.camera;

import org.joml.Matrix4f;

public final class Orthographic {

    private float left;
    private float right;
    private float bottom;
    private float top;
    private float near;
    private float far;

    public Orthographic(float left, float right, float bottom, float top, float near, float far) {
        this.left = left;
        this.right = right;
        this.bottom = bottom;
        this.top = top;
        this.near = near;
        this.far = far;
    }

    public Matrix4f projection() {
        return new Matrix4f().ortho(left, right, bottom, top, near, far);
    }

    public void setBounds(float left, float right, float bottom, float top) {
        this.left = left;
        this.right = right;
        this.bottom = bottom;
        this.top = top;
    }
}
