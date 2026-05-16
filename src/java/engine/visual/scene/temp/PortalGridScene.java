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
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.memAllocFloat;
import static org.lwjgl.system.MemoryUtil.memFree;

public final class PortalGridScene extends SceneTemplate {

    private static final int SHADOW_MAP_SIZE = 4096;
    private static final float NEAR = 0.1f;
    private static final float FAR = 80f;
    private static final float[] CASCADES = {10f, 25f, 55f};

    private enum LightingMode {
        STENCIL_VOLUMES("Stencil Volumes"),
        CASCADED_SHADOW_MAPPING("CSM");
        private final String label;
        LightingMode(String label) { this.label = label; }
    }

    private final Shader shader = new Shader();
    private final Matrix4f[] lightVP = {new Matrix4f(), new Matrix4f(), new Matrix4f()};

    private float phase;
    private LightingMode mode = LightingMode.STENCIL_VOLUMES;

    private int vao, vbo, vertexCount;
    private int litProgram, depthProgram, flatProgram;
    private int shadowFbo;
    private int[] shadowTex;

    public PortalGridScene() { super(SceneIds.PORTAL_GRID, SceneType.TWO_D_AND_THREE_D); }

    @Override public void initialize(Unloader resources) {
        phase = 0f;
        mode = LightingMode.STENCIL_VOLUMES;
        litProgram = shader.program(LIT_VS, LIT_FS);
        depthProgram = shader.program(DEPTH_VS, DEPTH_FS);
        flatProgram = shader.program(FLAT_VS, FLAT_FS);
        createGeometry();
        createShadowMaps();
        resources.track(this::destroyGpu);
    }

    @Override public void update(float deltaSeconds) { phase += deltaSeconds; }

    @Override public void render3D() {
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);

        Vector3f eye = new Vector3f((float)Math.sin(phase * 0.25f) * 11f, 7f, (float)Math.cos(phase * 0.25f) * 11f);
        Matrix4f projection = new Matrix4f().perspective((float)Math.toRadians(60f), 16f / 9f, NEAR, FAR);
        Matrix4f view = new Matrix4f().lookAt(eye, new Vector3f(0f, 1.5f, 0f), new Vector3f(0f, 1f, 0f));
        Matrix4f viewProj = new Matrix4f(projection).mul(view);
        Vector3f lightPos = new Vector3f(8f, 10f, 6f);

        buildCascades(lightPos);
        renderShadowDepth();

        if (mode == LightingMode.CASCADED_SHADOW_MAPPING) {
            renderLit(viewProj, view, lightPos, true);
        } else {
            renderLit(viewProj, view, lightPos, false);
            renderPlanarStencilShadow(viewProj, lightPos);
        }

        resetState();
    }

    @Override public void render2D(Overlay overlay, Render textRender) {
        textRender.drawText(overlay, "Prototype Portal Grid", 16f, 16f, 3f);
        textRender.drawText(overlay, "F - switch lighting mode", 16f, 48f, 2f);
        textRender.drawText(overlay, "Mode: " + mode.label, 16f, 76f, 2f);
    }

    public void toggleLightingMode() { mode = mode == LightingMode.STENCIL_VOLUMES ? LightingMode.CASCADED_SHADOW_MAPPING : LightingMode.STENCIL_VOLUMES; }

    private void renderShadowDepth() {
        glViewport(0, 0, SHADOW_MAP_SIZE, SHADOW_MAP_SIZE);
        glBindFramebuffer(GL_FRAMEBUFFER, shadowFbo);
        glUseProgram(depthProgram);
        glColorMask(false, false, false, false);
        glCullFace(GL_FRONT);
        glEnable(GL_CULL_FACE);

        for (int i = 0; i < 3; i++) {
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, shadowTex[i], 0);
            glClear(GL_DEPTH_BUFFER_BIT);
            setMat4(depthProgram, "uMvp", lightVP[i]);
            draw();
        }

        glDisable(GL_CULL_FACE);
        glColorMask(true, true, true, true);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, 960, 540);
    }

    private void renderLit(Matrix4f vp, Matrix4f view, Vector3f lightPos, boolean useCsm) {
        glUseProgram(litProgram);
        setMat4(litProgram, "uViewProj", vp);
        setMat4(litProgram, "uView", view);
        for (int i = 0; i < 3; i++) setMat4(litProgram, "uLightVP[" + i + "]", lightVP[i]);
        glUniform3f(glGetUniformLocation(litProgram, "uLightPos"), lightPos.x, lightPos.y, lightPos.z);
        glUniform1i(glGetUniformLocation(litProgram, "uUseCsm"), useCsm ? 1 : 0);
        glUniform3f(glGetUniformLocation(litProgram, "uCascades"), CASCADES[0], CASCADES[1], CASCADES[2]);

        glActiveTexture(GL_TEXTURE0); glBindTexture(GL_TEXTURE_2D, shadowTex[0]); glUniform1i(glGetUniformLocation(litProgram, "uShadow0"), 0);
        glActiveTexture(GL_TEXTURE1); glBindTexture(GL_TEXTURE_2D, shadowTex[1]); glUniform1i(glGetUniformLocation(litProgram, "uShadow1"), 1);
        glActiveTexture(GL_TEXTURE2); glBindTexture(GL_TEXTURE_2D, shadowTex[2]); glUniform1i(glGetUniformLocation(litProgram, "uShadow2"), 2);

        draw();
    }

    private void renderPlanarStencilShadow(Matrix4f vp, Vector3f lightPos) {
        // Doom3/Quake4-style shadow volume stencil pass (z-fail variant with two-sided operations).
        glEnable(GL_STENCIL_TEST);
        glClear(GL_STENCIL_BUFFER_BIT);
        glStencilMask(0xFF);
        glStencilFunc(GL_ALWAYS, 0, 0xFF);

        glColorMask(false, false, false, false);
        glDepthMask(false);
        glEnable(GL_CULL_FACE);

        glUseProgram(flatProgram);
        setMat4(flatProgram, "uMvp", vp);
        glUniform4f(glGetUniformLocation(flatProgram, "uColor"), 0f, 0f, 0f, 0f);

        glUniform4f(glGetUniformLocation(flatProgram, "uShadowExtrude"), lightPos.x, lightPos.y, lightPos.z, 40f);

        // back faces increment on z-fail
        glCullFace(GL_FRONT);
        glStencilOp(GL_KEEP, GL_INCR_WRAP, GL_KEEP);
        draw();

        // front faces decrement on z-fail
        glCullFace(GL_BACK);
        glStencilOp(GL_KEEP, GL_DECR_WRAP, GL_KEEP);
        draw();

        glColorMask(true, true, true, true);
        glDepthMask(true);
        glDisable(GL_CULL_FACE);

        // darken only shadowed pixels
        glStencilFunc(GL_NOTEQUAL, 0, 0xFF);
        glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glUseProgram(flatProgram);
        setMat4(flatProgram, "uMvp", vp);
        glUniform4f(glGetUniformLocation(flatProgram, "uColor"), 0f, 0f, 0f, 0.42f);
        glUniform4f(glGetUniformLocation(flatProgram, "uShadowExtrude"), 0f, 0f, 0f, 0f);
        draw();

        glDisable(GL_BLEND);
        glDisable(GL_STENCIL_TEST);
    }

    private void buildCascades(Vector3f lightPos) {
        Matrix4f lv = new Matrix4f().lookAt(lightPos, new Vector3f(0f, 0f, 0f), new Vector3f(0f, 1f, 0f));
        float[] ext = {18f, 32f, 48f};
        float[] far = {24f, 50f, 90f};
        for (int i = 0; i < 3; i++) {
            Matrix4f lp = new Matrix4f().ortho(-ext[i], ext[i], -ext[i], ext[i], 1f, far[i]);
            lightVP[i].set(lp).mul(lv);
        }
    }

    private void createGeometry() {
        float[] v = GeometryFactory.buildDemoGeometry();
        vertexCount = v.length / 9;
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, v, GL_STATIC_DRAW);
        glEnableVertexAttribArray(0); glVertexAttribPointer(0, 3, GL_FLOAT, false, 9 * Float.BYTES, 0L);
        glEnableVertexAttribArray(1); glVertexAttribPointer(1, 3, GL_FLOAT, false, 9 * Float.BYTES, 3L * Float.BYTES);
        glEnableVertexAttribArray(2); glVertexAttribPointer(2, 3, GL_FLOAT, false, 9 * Float.BYTES, 6L * Float.BYTES);
        glBindVertexArray(0);
    }

    private void createShadowMaps() {
        shadowFbo = glGenFramebuffers();
        shadowTex = new int[] {glGenTextures(), glGenTextures(), glGenTextures()};
        for (int t : shadowTex) {
            glBindTexture(GL_TEXTURE_2D, t);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, SHADOW_MAP_SIZE, SHADOW_MAP_SIZE, 0, GL_DEPTH_COMPONENT, GL_FLOAT, 0L);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
            glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, new float[] {1f,1f,1f,1f});
        }
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    private void draw() { glBindVertexArray(vao); glDrawArrays(GL_TRIANGLES, 0, vertexCount); glBindVertexArray(0); }

    private void resetState() {
        glUseProgram(0); glBindFramebuffer(GL_FRAMEBUFFER, 0); glBindVertexArray(0);
        glDisable(GL_BLEND); glDisable(GL_STENCIL_TEST); glDisable(GL_CULL_FACE);
        glDepthMask(true); glColorMask(true, true, true, true);
        glActiveTexture(GL_TEXTURE2); glBindTexture(GL_TEXTURE_2D, 0);
        glActiveTexture(GL_TEXTURE1); glBindTexture(GL_TEXTURE_2D, 0);
        glActiveTexture(GL_TEXTURE0); glBindTexture(GL_TEXTURE_2D, 0);
    }

    private void destroyGpu() {
        if (vbo != 0) glDeleteBuffers(vbo);
        if (vao != 0) glDeleteVertexArrays(vao);
        if (litProgram != 0) glDeleteProgram(litProgram);
        if (depthProgram != 0) glDeleteProgram(depthProgram);
        if (flatProgram != 0) glDeleteProgram(flatProgram);
        if (shadowTex != null) for (int t : shadowTex) glDeleteTextures(t);
        if (shadowFbo != 0) glDeleteFramebuffers(shadowFbo);
    }

    @Override public void destroy() {}

    private void setMat4(int program, String u, Matrix4f m) {
        FloatBuffer b = memAllocFloat(16);
        m.get(b);
        glUniformMatrix4fv(glGetUniformLocation(program, u), false, b);
        memFree(b);
    }

    private static final String LIT_VS = """
#version 330 core
layout (location=0) in vec3 aPos;
layout (location=1) in vec3 aColor;
layout (location=2) in vec3 aNormal;
uniform mat4 uViewProj;
uniform mat4 uView;
out vec3 vColor;
out vec3 vPos;
out vec3 vNormal;
out float vDepth;
void main(){ gl_Position=uViewProj*vec4(aPos,1.0); vColor=aColor; vPos=aPos; vNormal=normalize(aNormal); vDepth=-(uView*vec4(aPos,1.0)).z; }
""";
    private static final String LIT_FS = """
#version 330 core
in vec3 vColor; in vec3 vPos; in vec3 vNormal; in float vDepth;
uniform vec3 uLightPos; uniform int uUseCsm; uniform vec3 uCascades; uniform mat4 uLightVP[3];
uniform sampler2D uShadow0; uniform sampler2D uShadow1; uniform sampler2D uShadow2;
out vec4 fragColor;
float readShadow(int i, vec2 uv){ return i==0?texture(uShadow0,uv).r:(i==1?texture(uShadow1,uv).r:texture(uShadow2,uv).r); }
float shadowFactor(int i){ vec4 ls=uLightVP[i]*vec4(vPos,1.0); vec3 ndc=ls.xyz/ls.w; vec2 uv=ndc.xy*0.5+0.5; float d=ndc.z*0.5+0.5; float md=readShadow(i,uv); return d-0.0015>md?0.35:1.0; }
void main(){ float amb=0.2; vec3 n=normalize(vNormal); vec3 l=normalize(uLightPos-vPos); float diff=max(dot(n,l),0.0); float sh=1.0; if(uUseCsm==1){int c=vDepth<uCascades.x?0:(vDepth<uCascades.y?1:2); sh=shadowFactor(c);} fragColor=vec4(vColor*(amb+diff*sh),1.0); }
""";
    private static final String DEPTH_VS = """
#version 330 core
layout (location=0) in vec3 aPos;
uniform mat4 uMvp;
uniform vec4 uShadowExtrude;
void main(){
    vec3 p = aPos;
    if (uShadowExtrude.w > 0.0) {
        vec3 dir = normalize(aPos - uShadowExtrude.xyz);
        p = aPos + dir * uShadowExtrude.w;
    }
    gl_Position=uMvp*vec4(p,1.0);
}
""";
    private static final String DEPTH_FS = """
#version 330 core
void main(){}
""";
    private static final String FLAT_VS = """
#version 330 core
layout (location=0) in vec3 aPos;
uniform mat4 uMvp;
uniform vec4 uShadowExtrude;
void main(){
    vec3 p = aPos;
    if (uShadowExtrude.w > 0.0) {
        vec3 dir = normalize(aPos - uShadowExtrude.xyz);
        p = aPos + dir * uShadowExtrude.w;
    }
    gl_Position=uMvp*vec4(p,1.0);
}
""";
    private static final String FLAT_FS = """
#version 330 core
uniform vec4 uColor; out vec4 fragColor; void main(){ fragColor=uColor; }
""";
}
