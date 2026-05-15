package engine.visual.scene.temp;

import engine.util.resource.Unloader;
import engine.visual.Overlay;
import engine.visual.Render;
import engine.visual.scene.SceneIds;
import engine.visual.scene.SceneTemplate;
import engine.visual.scene.SceneType;
import engine.visual.shader.Shader;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13.GL_TEXTURE2;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.memAllocFloat;
import static org.lwjgl.system.MemoryUtil.memFree;

public final class PortalGridScene extends SceneTemplate {

    private static final int SHADOW_MAP_SIZE = 4096;
    private static final float CAMERA_NEAR = 0.1f;
    private static final float CAMERA_FAR = 100f;

    private enum LightingMode {
        STENCIL_VOLUMES("Stencil volumes (doom-like multipass)"),
        CASCADED_SHADOW_MAPPING("Cascaded shadow mapping");

        private final String label;

        LightingMode(String label) {
            this.label = label;
        }
    }

    private final Shader shader = new Shader();
    private final Matrix4f[] lightViewProj = {new Matrix4f(), new Matrix4f(), new Matrix4f()};
    private final float[] cascadeSplits = {10f, 30f, 75f};

    private float phase;
    private LightingMode lightingMode = LightingMode.STENCIL_VOLUMES;

    private int litProgram;
    private int depthProgram;
    private int darkenProgram;
    private int vao;
    private int vbo;
    private int vertexCount;
    private int shadowFbo;
    private int[] shadowTextures;

    public PortalGridScene() {
        super(SceneIds.PORTAL_GRID, SceneType.TWO_D_AND_THREE_D);
    }

    @Override
    public void initialize(Unloader resources) {
        phase = 0f;
        lightingMode = LightingMode.STENCIL_VOLUMES;
        createPrograms();
        createGeometry();
        createShadowResources();
        resources.track(this::destroyGpu);
    }

    @Override
    public void update(float deltaSeconds) {
        phase += deltaSeconds;
    }

    @Override
    public void render3D() {
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);

        Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(60f), 16f / 9f, CAMERA_NEAR, CAMERA_FAR);
        Vector3f eye = new Vector3f((float) Math.sin(phase * 0.3f) * 11f, 7f, (float) Math.cos(phase * 0.3f) * 11f);
        Matrix4f view = new Matrix4f().lookAt(eye, new Vector3f(0f, 1.4f, 0f), new Vector3f(0f, 1f, 0f));
        Matrix4f viewProj = new Matrix4f(projection).mul(view);
        Vector3f lightPos = new Vector3f(8f, 10f, 6f);

        buildLightCascades(lightPos);
        renderShadowDepthPass();

        if (lightingMode == LightingMode.CASCADED_SHADOW_MAPPING) {
            renderLitPass(viewProj, view, lightPos, true, false);
        } else {
            renderLitPass(viewProj, view, lightPos, false, false);
            renderStencilDarkeningPass(viewProj, lightPos);
        }

        restoreState();
    }

    @Override
    public void render2D(Overlay overlay, Render textRender) {
        textRender.drawText(overlay, "Prototype Portal Grid", 16f, 16f, 3f);
        textRender.drawText(overlay, "F - switch lighting mode", 16f, 48f, 2f);
        textRender.drawText(overlay, "Mode: " + lightingMode.label, 16f, 76f, 2f);
        textRender.drawText(overlay, "Geometry: cubes + spheres + plane + isolated quad + isolated triangle", 16f, 104f, 2f);
    }

    public void toggleLightingMode() {
        lightingMode = lightingMode == LightingMode.STENCIL_VOLUMES
                ? LightingMode.CASCADED_SHADOW_MAPPING
                : LightingMode.STENCIL_VOLUMES;
    }

    private void renderShadowDepthPass() {
        glViewport(0, 0, SHADOW_MAP_SIZE, SHADOW_MAP_SIZE);
        glBindFramebuffer(GL_FRAMEBUFFER, shadowFbo);
        glUseProgram(depthProgram);
        glColorMask(false, false, false, false);

        for (int cascade = 0; cascade < 3; cascade++) {
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, shadowTextures[cascade], 0);
            glClear(GL_DEPTH_BUFFER_BIT);
            setMat4(depthProgram, "uMvp", lightViewProj[cascade]);
            drawGeometry();
        }

        glColorMask(true, true, true, true);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, 960, 540);
    }

    private void renderLitPass(Matrix4f viewProj, Matrix4f view, Vector3f lightPos, boolean useCsm, boolean ambientOnly) {
        glUseProgram(litProgram);
        setMat4(litProgram, "uViewProj", viewProj);
        setMat4(litProgram, "uView", view);
        for (int i = 0; i < 3; i++) {
            setMat4(litProgram, "uLightMvp[" + i + "]", lightViewProj[i]);
        }
        glUniform3f(glGetUniformLocation(litProgram, "uLightPos"), lightPos.x, lightPos.y, lightPos.z);
        glUniform3f(glGetUniformLocation(litProgram, "uCascades"), cascadeSplits[0], cascadeSplits[1], cascadeSplits[2]);
        glUniform1i(glGetUniformLocation(litProgram, "uUseCsm"), useCsm ? 1 : 0);
        glUniform1i(glGetUniformLocation(litProgram, "uAmbientOnly"), ambientOnly ? 1 : 0);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, shadowTextures[0]);
        glUniform1i(glGetUniformLocation(litProgram, "uShadow0"), 0);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, shadowTextures[1]);
        glUniform1i(glGetUniformLocation(litProgram, "uShadow1"), 1);
        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, shadowTextures[2]);
        glUniform1i(glGetUniformLocation(litProgram, "uShadow2"), 2);

        drawGeometry();
    }

    private void renderStencilDarkeningPass(Matrix4f viewProj, Vector3f lightPos) {
        // Doom3/Quake4 style: ambient already rendered, then build stencil volumes with z-fail, then additive lit on stencil==0.
        glEnable(GL_STENCIL_TEST);
        glClear(GL_STENCIL_BUFFER_BIT);
        glStencilMask(0xFF);
        glStencilFunc(GL_ALWAYS, 0, 0xFF);

        glColorMask(false, false, false, false);
        glDepthMask(false);
        glEnable(GL_CULL_FACE);

        glUseProgram(darkenProgram);
        setMat4(darkenProgram, "uViewProj", viewProj);
        glUniform3f(glGetUniformLocation(darkenProgram, "uLightPos"), lightPos.x, lightPos.y, lightPos.z);
        glUniform1f(glGetUniformLocation(darkenProgram, "uExtrude"), 45f);

        // z-fail stencil ops (Carmack's reverse style)
        glCullFace(GL_FRONT);
        glStencilOp(GL_KEEP, GL_DECR_WRAP, GL_KEEP);
        drawGeometry();

        glCullFace(GL_BACK);
        glStencilOp(GL_KEEP, GL_INCR_WRAP, GL_KEEP);
        drawGeometry();

        // darken only shadowed pixels
        glColorMask(true, true, true, true);
        glDepthMask(true);
        glDisable(GL_CULL_FACE);
        glStencilFunc(GL_NOTEQUAL, 0, 0xFF);
        glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glUseProgram(darkenProgram);
        setMat4(darkenProgram, "uViewProj", viewProj);
        glUniform3f(glGetUniformLocation(darkenProgram, "uLightPos"), lightPos.x, lightPos.y, lightPos.z);
        glUniform1f(glGetUniformLocation(darkenProgram, "uExtrude"), 0f);
        drawGeometry();

        glDisable(GL_BLEND);
        glDisable(GL_STENCIL_TEST);
    }

    private void buildLightCascades(Vector3f lightPos) {
        Matrix4f lightView = new Matrix4f().lookAt(lightPos, new Vector3f(0f, 0f, 0f), new Vector3f(0f, 1f, 0f));
        Matrix4f lightGridRotate = new Matrix4f().rotateZ((float) Math.toRadians(90f));
        lightView.mul(lightGridRotate);
        float[] extents = {20f, 34f, 54f};
        float[] fars = {25f, 50f, 100f};

        for (int i = 0; i < 3; i++) {
            Matrix4f lightProj = new Matrix4f().ortho(-extents[i], extents[i], -extents[i], extents[i], 0.5f, fars[i]);
            lightViewProj[i].set(lightProj).mul(lightView);
        }
    }

    private void createPrograms() {
        litProgram = shader.program(LIT_VS, LIT_FS);
        depthProgram = shader.program(DEPTH_VS, DEPTH_FS);
        darkenProgram = shader.program(DARKEN_VS, DARKEN_FS);
    }

    private void createGeometry() {
        float[] vertices = GeometryFactory.buildDemoGeometry();
        vertexCount = vertices.length / 9;

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 9 * Float.BYTES, 0L);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 9 * Float.BYTES, 3L * Float.BYTES);
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(2, 3, GL_FLOAT, false, 9 * Float.BYTES, 6L * Float.BYTES);
        glBindVertexArray(0);
    }

    private void createShadowResources() {
        shadowFbo = glGenFramebuffers();
        shadowTextures = new int[] {glGenTextures(), glGenTextures(), glGenTextures()};

        for (int tex : shadowTextures) {
            glBindTexture(GL_TEXTURE_2D, tex);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, SHADOW_MAP_SIZE, SHADOW_MAP_SIZE, 0, GL_DEPTH_COMPONENT, GL_FLOAT, 0L);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        }
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    private void drawGeometry() {
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        glBindVertexArray(0);
    }

    private void restoreState() {
        glUseProgram(0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glBindVertexArray(0);
        glDisable(GL_STENCIL_TEST);
        glDisable(GL_BLEND);
        glDisable(GL_CULL_FACE);
        glDepthMask(true);
        glColorMask(true, true, true, true);
        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, 0);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, 0);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    private void destroyGpu() {
        if (vbo != 0) glDeleteBuffers(vbo);
        if (vao != 0) glDeleteVertexArrays(vao);
        if (litProgram != 0) glDeleteProgram(litProgram);
        if (depthProgram != 0) glDeleteProgram(depthProgram);
        if (darkenProgram != 0) glDeleteProgram(darkenProgram);
        if (shadowTextures != null) {
            for (int tex : shadowTextures) glDeleteTextures(tex);
        }
        if (shadowFbo != 0) glDeleteFramebuffers(shadowFbo);
    }

    @Override
    public void destroy() {
    }

    private void setMat4(int program, String uniform, Matrix4f matrix) {
        FloatBuffer buffer = memAllocFloat(16);
        matrix.get(buffer);
        glUniformMatrix4fv(glGetUniformLocation(program, uniform), false, buffer);
        memFree(buffer);
    }

    private static final String LIT_VS = """
#version 330 core
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aColor;
layout (location = 2) in vec3 aNormal;
uniform mat4 uViewProj;
uniform mat4 uView;
out vec3 vColor;
out vec3 vPos;
out vec3 vNormal;
out float vViewDepth;
void main() {
    gl_Position = uViewProj * vec4(aPos, 1.0);
    vColor = aColor;
    vPos = aPos;
    vNormal = normalize(aNormal);
    vViewDepth = -(uView * vec4(aPos, 1.0)).z;
}
""";

    private static final String LIT_FS = """
#version 330 core
in vec3 vColor;
in vec3 vPos;
in vec3 vNormal;
in float vViewDepth;
uniform vec3 uLightPos;
uniform vec3 uCascades;
uniform int uUseCsm;
uniform int uAmbientOnly;
uniform mat4 uLightMvp[3];
uniform sampler2D uShadow0;
uniform sampler2D uShadow1;
uniform sampler2D uShadow2;
out vec4 fragColor;

float shadowAt(int idx, vec2 uv) {
    return idx == 0 ? texture(uShadow0, uv).r : (idx == 1 ? texture(uShadow1, uv).r : texture(uShadow2, uv).r);
}

float sampleShadow(int idx) {
    vec4 lightSpace = uLightMvp[idx] * vec4(vPos, 1.0);
    vec3 ndc = lightSpace.xyz / lightSpace.w;
    vec2 uv = ndc.xy * 0.5 + 0.5;
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) return 1.0;
    float depth = ndc.z * 0.5 + 0.5;
    float mapDepth = shadowAt(idx, uv);
    return depth - 0.0018 > mapDepth ? 0.3 : 1.0;
}

void main() {
    float ambient = 0.22;
    if (uAmbientOnly == 1) {
        fragColor = vec4(vColor * ambient, 1.0);
        return;
    }

    vec3 n = normalize(vNormal);
    vec3 l = normalize(uLightPos - vPos);
    float diffuse = max(dot(n, l), 0.0);

    float shadow = 1.0;
    if (uUseCsm == 1) {
        int cascade = vViewDepth < uCascades.x ? 0 : (vViewDepth < uCascades.y ? 1 : 2);
        shadow = sampleShadow(cascade);
    }

    float lit = ambient + diffuse * shadow;
    fragColor = vec4(vColor * lit, 1.0);
}
""";

    private static final String DEPTH_VS = """
#version 330 core
layout (location = 0) in vec3 aPos;
uniform mat4 uMvp;
void main() {
    gl_Position = uMvp * vec4(aPos, 1.0);
}
""";

    private static final String DEPTH_FS = """
#version 330 core
void main() {}
""";

    private static final String DARKEN_VS = """
#version 330 core
layout (location = 0) in vec3 aPos;
uniform mat4 uViewProj;
uniform vec3 uLightPos;
uniform float uExtrude;
void main() {
    vec3 dir = normalize(aPos - uLightPos);
    vec3 p = aPos + dir * uExtrude;
    gl_Position = uViewProj * vec4(p, 1.0);
}
""";

    private static final String DARKEN_FS = """
#version 330 core
out vec4 fragColor;
void main() {
    fragColor = vec4(0.0, 0.0, 0.0, 0.45);
}
""";
}
