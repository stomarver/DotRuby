package engine.visual.scene;

import engine.util.resource.Unloader;
import engine.visual.Overlay;
import engine.visual.Render;
import engine.visual.shader.Shader;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.memAllocFloat;
import static org.lwjgl.system.MemoryUtil.memFree;

public final class PortalGridScene extends SceneTemplate {

    private enum LightingMode { STENCIL_VOLUMES("Stencil volumes + stencil shadows"), CASCADED_SHADOW_MAPPING("Cascaded shadow mapping");
        private final String label; LightingMode(String label) { this.label = label; } }

    private float phase;
    private LightingMode lightingMode = LightingMode.STENCIL_VOLUMES;

    private final Shader shaderCompiler = new Shader();
    private int litProgram, depthProgram, volumeProgram;
    private int vao, vbo, vertexCount;
    private int shadowFbo;
    private int[] shadowTextures;
    private final Matrix4f[] lightViewProj = {new Matrix4f(), new Matrix4f(), new Matrix4f()};

    public PortalGridScene() { super(SceneIds.PORTAL_GRID, SceneType.TWO_D_AND_THREE_D); }

    @Override public void initialize(Unloader resources) {
        phase = 0f; lightingMode = LightingMode.STENCIL_VOLUMES;
        litProgram = shaderCompiler.program(VS, FS);
        depthProgram = shaderCompiler.program(DEPTH_VS, DEPTH_FS);
        volumeProgram = shaderCompiler.program(VOLUME_VS, VOLUME_FS);
        setupGeometry(); setupShadowMaps();
        resources.track(this::destroyGpu);
    }
    @Override public void update(float deltaSeconds) { phase += deltaSeconds; }

    @Override public void render3D() {
        Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(60f), 16f / 9f, 0.1f, 100f);
        Vector3f eye = new Vector3f((float) Math.sin(phase * 0.3f) * 11f, 7f, (float) Math.cos(phase * 0.3f) * 11f);
        Matrix4f view = new Matrix4f().lookAt(eye, new Vector3f(0f, 1.4f, 0f), new Vector3f(0f, 1f, 0f));
        Matrix4f mvp = new Matrix4f(projection).mul(view);
        Vector3f lightPos = new Vector3f(8f, 10f, 6f);

        buildCascades(lightPos);
        if (lightingMode == LightingMode.STENCIL_VOLUMES) renderStencilVolumes(mvp, view, lightPos);
        else renderCascadedShadowMaps(mvp, view, lightPos);

        glUseProgram(0); glBindVertexArray(0); glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glDisable(GL_STENCIL_TEST); glDisable(GL_CULL_FACE); glDepthMask(true); glColorMask(true, true, true, true);
        glActiveTexture(GL_TEXTURE1); glBindTexture(GL_TEXTURE_2D, 0);
        glActiveTexture(GL_TEXTURE0); glBindTexture(GL_TEXTURE_2D, 0);
    }

    @Override public void render2D(Overlay overlay, Render textRender) {
        textRender.drawText(overlay, "Prototype Portal Grid", 16f, 16f, 2f);
        textRender.drawText(overlay, "F - switch lighting mode", 16f, 48f, 1.5f);
        textRender.drawText(overlay, "Mode: " + lightingMode.label, 16f, 76f, 1.5f);
        textRender.drawText(overlay, "Geometry: cubes + spheres + plane + isolated quad + isolated triangle", 16f, 104f, 1.5f);
    }

    public void toggleLightingMode() { lightingMode = lightingMode == LightingMode.STENCIL_VOLUMES ? LightingMode.CASCADED_SHADOW_MAPPING : LightingMode.STENCIL_VOLUMES; }

    private void renderStencilVolumes(Matrix4f mvp, Matrix4f view, Vector3f lightPos) {
        // Ambient base
        drawScene(mvp, view, lightPos, true, false);

        // Shadow volumes stencil update (depth-fail style shell extrusion)
        glEnable(GL_STENCIL_TEST); glClear(GL_STENCIL_BUFFER_BIT); glStencilMask(0xFF); glStencilFunc(GL_ALWAYS, 0, 0xFF);
        glColorMask(false, false, false, false); glDepthMask(false); glEnable(GL_CULL_FACE);
        glUseProgram(volumeProgram);
        setMat4(volumeProgram, "uMvp", mvp);
        glUniform3f(glGetUniformLocation(volumeProgram, "uLightPos"), lightPos.x, lightPos.y, lightPos.z);

        glCullFace(GL_FRONT); glStencilOp(GL_KEEP, GL_INCR_WRAP, GL_KEEP); glUniform1f(glGetUniformLocation(volumeProgram, "uExtrude"), 40f); drawGeometry();
        glCullFace(GL_BACK); glStencilOp(GL_KEEP, GL_DECR_WRAP, GL_KEEP); glUniform1f(glGetUniformLocation(volumeProgram, "uExtrude"), 0f); drawGeometry();

        // Lit pass only where stencil == 0
        glColorMask(true, true, true, true); glDepthMask(true); glDisable(GL_CULL_FACE);
        glStencilFunc(GL_EQUAL, 0, 0xFF); glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
        drawScene(mvp, view, lightPos, false, false);
        glDisable(GL_STENCIL_TEST);
    }

    private void renderCascadedShadowMaps(Matrix4f mvp, Matrix4f view, Vector3f lightPos) {
        glViewport(0, 0, 1024, 1024);
        glBindFramebuffer(GL_FRAMEBUFFER, shadowFbo);
        glUseProgram(depthProgram);
        glBindVertexArray(vao);
        for (int i = 0; i < 3; i++) {
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, shadowTextures[i], 0);
            glClear(GL_DEPTH_BUFFER_BIT);
            setMat4(depthProgram, "uMvp", lightViewProj[i]);
            glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, 960, 540);
        drawScene(mvp, view, lightPos, false, true);
    }

    private void drawScene(Matrix4f mvp, Matrix4f view, Vector3f lightPos, boolean ambientOnly, boolean useCsm) {
        glUseProgram(litProgram); setMat4(litProgram, "uMvp", mvp); setMat4(litProgram, "uView", view);
        for (int i = 0; i < 3; i++) setMat4(litProgram, "uLightMvp[" + i + "]", lightViewProj[i]);
        glUniform3f(glGetUniformLocation(litProgram, "uLightPos"), lightPos.x, lightPos.y, lightPos.z);
        glUniform1i(glGetUniformLocation(litProgram, "uAmbientOnly"), ambientOnly ? 1 : 0);
        glUniform1i(glGetUniformLocation(litProgram, "uUseCsm"), useCsm ? 1 : 0);
        glUniform3f(glGetUniformLocation(litProgram, "uCascades"), 8f, 25f, 60f);
        for (int i = 0; i < 3; i++) { glActiveTexture(GL_TEXTURE0 + i); glBindTexture(GL_TEXTURE_2D, shadowTextures[i]); }
        glUniform1i(glGetUniformLocation(litProgram, "uShadow0"), 0);
        glUniform1i(glGetUniformLocation(litProgram, "uShadow1"), 1);
        glUniform1i(glGetUniformLocation(litProgram, "uShadow2"), 2);
        drawGeometry();
    }

    private void buildCascades(Vector3f lightPos) {
        Vector3f lightTarget = new Vector3f(0f, 0f, 0f);
        Matrix4f lightView = new Matrix4f().lookAt(lightPos, lightTarget, new Vector3f(0f, 1f, 0f));
        float[] extents = {16f, 28f, 42f};
        float[] fars = {20f, 45f, 90f};
        for (int i = 0; i < 3; i++) {
            Matrix4f lightProj = new Matrix4f().ortho(-extents[i], extents[i], -extents[i], extents[i], 0.5f, fars[i]);
            lightViewProj[i].set(lightProj).mul(lightView);
        }
    }

    private void setupGeometry() { float[] vertices = GeometryFactory.buildDemoGeometry(); vertexCount = vertices.length / 9; vao = glGenVertexArrays(); vbo = glGenBuffers(); glBindVertexArray(vao); glBindBuffer(GL_ARRAY_BUFFER, vbo); glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW); glVertexAttribPointer(0, 3, GL_FLOAT, false, 9 * Float.BYTES, 0L); glEnableVertexAttribArray(0); glVertexAttribPointer(1, 3, GL_FLOAT, false, 9 * Float.BYTES, 3L * Float.BYTES); glEnableVertexAttribArray(1); glVertexAttribPointer(2, 3, GL_FLOAT, false, 9 * Float.BYTES, 6L * Float.BYTES); glEnableVertexAttribArray(2); glBindVertexArray(0); }
    private void setupShadowMaps() { shadowFbo = glGenFramebuffers(); shadowTextures = new int[] {glGenTextures(), glGenTextures(), glGenTextures()}; for (int tex : shadowTextures) { glBindTexture(GL_TEXTURE_2D, tex); glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, 1024, 1024, 0, GL_DEPTH_COMPONENT, GL_FLOAT, 0L); glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST); glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST); glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE); glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);} glBindTexture(GL_TEXTURE_2D, 0); }
    private void drawGeometry() { glBindVertexArray(vao); glDrawArrays(GL_TRIANGLES, 0, vertexCount); glBindVertexArray(0); }
    private void destroyGpu() { if (vbo != 0) glDeleteBuffers(vbo); if (vao != 0) glDeleteVertexArrays(vao); if (litProgram != 0) glDeleteProgram(litProgram); if (depthProgram != 0) glDeleteProgram(depthProgram); if (volumeProgram != 0) glDeleteProgram(volumeProgram); if (shadowTextures != null) for (int tex : shadowTextures) glDeleteTextures(tex); if (shadowFbo != 0) glDeleteFramebuffers(shadowFbo); }
    @Override public void destroy() {}
    private void setMat4(int program, String uniform, Matrix4f matrix) { FloatBuffer buffer = memAllocFloat(16); matrix.get(buffer); glUniformMatrix4fv(glGetUniformLocation(program, uniform), false, buffer); memFree(buffer); }

    private static final String VS = """
#version 330 core
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aColor;
layout (location = 2) in vec3 aNormal;
uniform mat4 uMvp;
uniform mat4 uView;
out vec3 vColor;
out vec3 vPos;
out vec3 vNormal;
out float vViewDepth;
void main(){
    gl_Position = uMvp * vec4(aPos, 1.0);
    vColor=aColor;
    vPos=aPos;
    vNormal=normalize(aNormal);
    vViewDepth = -(uView * vec4(aPos,1.0)).z;
}
""";
private static final String FS = """
#version 330 core
in vec3 vColor;
in vec3 vPos;
in vec3 vNormal;
in float vViewDepth;
uniform vec3 uLightPos;
uniform int uAmbientOnly;
uniform int uUseCsm;
uniform sampler2D uShadow0;
uniform sampler2D uShadow1;
uniform sampler2D uShadow2;
uniform mat4 uLightMvp[3];
uniform vec3 uCascades;
out vec4 fragColor;
float shadowSample(int idx){
    vec4 ls = uLightMvp[idx] * vec4(vPos,1.0);
    vec3 ndc = ls.xyz / ls.w;
    vec2 uv=ndc.xy*0.5+0.5;
    if(uv.x<0||uv.x>1||uv.y<0||uv.y>1) return 1.0;
    float depth=ndc.z*0.5+0.5;
    float map = idx == 0 ? texture(uShadow0, uv).r : (idx == 1 ? texture(uShadow1, uv).r : texture(uShadow2, uv).r);
    return depth-0.0015 > map ? 0.2 : 1.0;
}
void main(){
    float ambient=0.25;
    if(uAmbientOnly==1){ fragColor=vec4(vColor*ambient,1.0); return; }
    vec3 n=normalize(vNormal);
    vec3 l=normalize(uLightPos-vPos);
    float diff=max(dot(n,l),0.0);
    float shadow=1.0;
    if(uUseCsm==1){
        int c = vViewDepth < uCascades.x ? 0 : (vViewDepth < uCascades.y ? 1 : 2);
        shadow = shadowSample(c);
    }
    float lightTerm = ambient + diff * shadow;
    fragColor=vec4(vColor*lightTerm,1.0);
}
""";
private static final String DEPTH_VS = """
#version 330 core
layout (location = 0) in vec3 aPos;
uniform mat4 uMvp;
void main(){ gl_Position = uMvp * vec4(aPos,1.0); }
""";
private static final String DEPTH_FS = """
#version 330 core
void main(){}
""";
private static final String VOLUME_VS = """
#version 330 core
layout (location = 0) in vec3 aPos;
uniform mat4 uMvp;
uniform vec3 uLightPos;
uniform float uExtrude;
void main(){
    vec3 dir = normalize(aPos - uLightPos);
    vec3 p = aPos + dir * uExtrude;
    gl_Position = uMvp * vec4(p,1.0);
}
""";
private static final String VOLUME_FS = """
#version 330 core
void main(){}
""";
}
