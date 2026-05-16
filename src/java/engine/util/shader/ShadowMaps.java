package engine.util.shader;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

public final class ShadowMaps {
    private final int size;
    private int fbo;
    private int depthTex;
    private final Matrix4f lightViewProj = new Matrix4f();

    public ShadowMaps(int size) { this.size = size; }

    public void init() {
        fbo = glGenFramebuffers();
        depthTex = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, depthTex);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, size, size, 0, GL_DEPTH_COMPONENT, GL_FLOAT, 0L);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, new float[] {1f,1f,1f,1f});
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public Matrix4f buildLightViewProj(Vector3f lightPos, Vector3f lightTarget) {
        Matrix4f lv = new Matrix4f().lookAt(lightPos, lightTarget, new Vector3f(0f, 1f, 0f));
        Matrix4f lp = new Matrix4f().perspective((float) Math.toRadians(115f), 1f, 0.4f, 80f);
        lightViewProj.set(lp).mul(lv);
        return lightViewProj;
    }

    public void beginDepthPass() {
        glViewport(0, 0, size, size);
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthTex, 0);
        glColorMask(false, false, false, false);
        glClear(GL_DEPTH_BUFFER_BIT);
    }

    public void endDepthPass() {
        glColorMask(true, true, true, true);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, 960, 540);
    }

    public void bindDepthTexture(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, depthTex);
    }

    public int texture() { return depthTex; }

    public void destroy() {
        if (depthTex != 0) glDeleteTextures(depthTex);
        if (fbo != 0) glDeleteFramebuffers(fbo);
        depthTex = 0; fbo = 0;
    }
}
