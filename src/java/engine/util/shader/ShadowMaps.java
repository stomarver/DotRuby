package engine.util.shader;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.GL_TEXTURE_COMPARE_MODE;
import static org.lwjgl.opengl.GL30.*;

public final class ShadowMaps {
    private final int size;
    private int fbo;
    private int depthCubeTex;
    private final Matrix4f[] lightViewProj = {
            new Matrix4f(), new Matrix4f(), new Matrix4f(),
            new Matrix4f(), new Matrix4f(), new Matrix4f()
    };

    public ShadowMaps(int size) { this.size = size; }

    public void init() {
        fbo = glGenFramebuffers();
        depthCubeTex = glGenTextures();
        glBindTexture(GL_TEXTURE_CUBE_MAP, depthCubeTex);
        for (int i = 0; i < 6; i++) {
            glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, GL_DEPTH_COMPONENT24, size, size, 0, GL_DEPTH_COMPONENT, GL_FLOAT, 0L);
        }
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_COMPARE_MODE, GL_NONE);
        glBindTexture(GL_TEXTURE_CUBE_MAP, 0);
    }

    public Matrix4f[] buildLightViewProj(Vector3f lightPos, float near, float far) {
        Matrix4f lp = new Matrix4f().perspective((float) Math.toRadians(90f), 1f, near, far);
        lightViewProj[0].set(lp).mul(new Matrix4f().lookAt(lightPos, new Vector3f(lightPos).add(1,0,0), new Vector3f(0,-1,0)));
        lightViewProj[1].set(lp).mul(new Matrix4f().lookAt(lightPos, new Vector3f(lightPos).add(-1,0,0), new Vector3f(0,-1,0)));
        lightViewProj[2].set(lp).mul(new Matrix4f().lookAt(lightPos, new Vector3f(lightPos).add(0,1,0), new Vector3f(0,0,1)));
        lightViewProj[3].set(lp).mul(new Matrix4f().lookAt(lightPos, new Vector3f(lightPos).add(0,-1,0), new Vector3f(0,0,-1)));
        lightViewProj[4].set(lp).mul(new Matrix4f().lookAt(lightPos, new Vector3f(lightPos).add(0,0,1), new Vector3f(0,-1,0)));
        lightViewProj[5].set(lp).mul(new Matrix4f().lookAt(lightPos, new Vector3f(lightPos).add(0,0,-1), new Vector3f(0,-1,0)));
        return lightViewProj;
    }

    public void beginDepthPass(int face) {
        glViewport(0, 0, size, size);
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_CUBE_MAP_POSITIVE_X + face, depthCubeTex, 0);
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
        glBindTexture(GL_TEXTURE_CUBE_MAP, depthCubeTex);
    }

    public int texture() { return depthCubeTex; }

    public void destroy() {
        if (depthCubeTex != 0) glDeleteTextures(depthCubeTex);
        if (fbo != 0) glDeleteFramebuffers(fbo);
        depthCubeTex = 0; fbo = 0;
    }
}
