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

    private enum LightingMode {
        STENCIL_VOLUMES("Stencil volumes + stencil shadows"),
        CASCADED_SHADOW_MAPPING("Cascaded shadow mapping");

        private final String label;

        LightingMode(String label) {
            this.label = label;
        }
    }

    private float phase;
    private LightingMode lightingMode = LightingMode.STENCIL_VOLUMES;

    private final Shader shaderCompiler = new Shader();
    private int litProgram;
    private int depthProgram;
    private int vao;
    private int vbo;
    private int vertexCount;

    private int shadowFbo;
    private int[] shadowTextures;
    private final Matrix4f[] lightViewProj = {new Matrix4f(), new Matrix4f()};

    public PortalGridScene() {
        super(SceneIds.PORTAL_GRID, SceneType.TWO_D_AND_THREE_D);
    }

    @Override
    public void initialize(Unloader resources) {
        phase = 0f;
        lightingMode = LightingMode.STENCIL_VOLUMES;
        setupPrograms();
        setupGeometry();
        setupShadowMaps();
        resources.track(this::destroyGpu);
    }

    @Override
    public void update(float deltaSeconds) {
        phase += deltaSeconds;
    }

    @Override
    public void render3D() {
        Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(60f), 16f / 9f, 0.1f, 80f);
        Vector3f eye = new Vector3f((float) Math.sin(phase * 0.3f) * 11f, 7f, (float) Math.cos(phase * 0.3f) * 11f);
        Matrix4f view = new Matrix4f().lookAt(eye, new Vector3f(0f, 1.4f, 0f), new Vector3f(0f, 1f, 0f));
        Matrix4f mvp = new Matrix4f(projection).mul(view);
        Vector3f lightPos = new Vector3f(8f, 10f, 6f);

        glEnable(GL_DEPTH_TEST);

        if (lightingMode == LightingMode.STENCIL_VOLUMES) {
            renderStencilVolumes(mvp, lightPos);
        } else {
            renderCascadedShadowMaps(projection, view, mvp, lightPos);
        }
    }

    @Override
    public void render2D(Overlay overlay, Render textRender) {
        textRender.drawText(overlay, "Prototype Portal Grid", 16f, 16f, 1f);
        textRender.drawText(overlay, "F - switch lighting mode", 16f, 40f, 1f);
        textRender.drawText(overlay, "Mode: " + lightingMode.label, 16f, 64f, 1f);
        textRender.drawText(overlay, "Geometry: cubes + spheres + plane + isolated quad + isolated triangle", 16f, 88f, 0.85f);
    }

    public void toggleLightingMode() {
        lightingMode = lightingMode == LightingMode.STENCIL_VOLUMES
                ? LightingMode.CASCADED_SHADOW_MAPPING
                : LightingMode.STENCIL_VOLUMES;
    }

    private void renderStencilVolumes(Matrix4f mvp, Vector3f lightPos) {
        glClear(GL_STENCIL_BUFFER_BIT);
        glEnable(GL_STENCIL_TEST);
        glStencilMask(0xFF);
        glStencilFunc(GL_ALWAYS, 0, 0xFF);

        glColorMask(false, false, false, false);
        glDepthMask(false);
        glStencilOpSeparate(GL_FRONT, GL_KEEP, GL_INCR_WRAP, GL_KEEP);
        glStencilOpSeparate(GL_BACK, GL_KEEP, GL_DECR_WRAP, GL_KEEP);
        drawScene(mvp, lightPos, 0, false, 0f);

        glColorMask(true, true, true, true);
        glDepthMask(true);
        glStencilFunc(GL_EQUAL, 0, 0xFF);
        glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
        drawScene(mvp, lightPos, 0, false, 0f);
        glDisable(GL_STENCIL_TEST);
    }

    private void renderCascadedShadowMaps(Matrix4f projection, Matrix4f view, Matrix4f mvp, Vector3f lightPos) {
        float[] splits = {18f, 55f};
        Vector3f lightDir = new Vector3f(-0.6f, -1f, -0.35f).normalize();

        glViewport(0, 0, 1024, 1024);
        glBindFramebuffer(GL_FRAMEBUFFER, shadowFbo);
        glUseProgram(depthProgram);

        for (int i = 0; i < 2; i++) {
            Matrix4f lightView = new Matrix4f().lookAt(new Vector3f(lightDir).mul(-22f), new Vector3f(0f, 0f, 0f), new Vector3f(0f, 1f, 0f));
            Matrix4f lightProj = new Matrix4f().ortho(-24f, 24f, -24f, 24f, 0.1f, splits[i]);
            lightViewProj[i].set(lightProj.mul(lightView));

            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, shadowTextures[i], 0);
            glClear(GL_DEPTH_BUFFER_BIT);
            setMat4(depthProgram, "uMvp", lightViewProj[i]);
            glBindVertexArray(vao);
            glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, 960, 540);

        drawScene(mvp, lightPos, 2, true, splits[0]);
    }

    private void drawScene(Matrix4f mvp, Vector3f lightPos, int shadowMapCount, boolean useShadows, float splitNear) {
        glUseProgram(litProgram);
        setMat4(litProgram, "uMvp", mvp);
        glUniform3f(glGetUniformLocation(litProgram, "uLightPos"), lightPos.x, lightPos.y, lightPos.z);
        glUniform1i(glGetUniformLocation(litProgram, "uShadowMode"), useShadows ? 1 : 0);
        glUniform1f(glGetUniformLocation(litProgram, "uSplitNear"), splitNear);

        if (shadowMapCount > 0) {
            setMat4(litProgram, "uLightMvp0", lightViewProj[0]);
            setMat4(litProgram, "uLightMvp1", lightViewProj[1]);
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, shadowTextures[0]);
            glUniform1i(glGetUniformLocation(litProgram, "uShadow0"), 0);
            glActiveTexture(GL_TEXTURE1);
            glBindTexture(GL_TEXTURE_2D, shadowTextures[1]);
            glUniform1i(glGetUniformLocation(litProgram, "uShadow1"), 1);
        }

        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        glBindVertexArray(0);
        glUseProgram(0);
    }

    private void setupPrograms() {
        litProgram = shaderCompiler.program(VS, FS);
        depthProgram = shaderCompiler.program(DEPTH_VS, DEPTH_FS);
    }

    private void setupGeometry() {
        float[] vertices = GeometryFactory.buildDemoGeometry();
        vertexCount = vertices.length / 9;
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 9 * Float.BYTES, 0L);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 9 * Float.BYTES, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 3, GL_FLOAT, false, 9 * Float.BYTES, 6L * Float.BYTES);
        glEnableVertexAttribArray(2);
        glBindVertexArray(0);
    }

    private void setupShadowMaps() {
        shadowFbo = glGenFramebuffers();
        shadowTextures = new int[] {glGenTextures(), glGenTextures()};
        for (int tex : shadowTextures) {
            glBindTexture(GL_TEXTURE_2D, tex);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, 1024, 1024, 0, GL_DEPTH_COMPONENT, GL_FLOAT, 0L);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        }
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    private void destroyGpu() {
        if (vbo != 0) glDeleteBuffers(vbo);
        if (vao != 0) glDeleteVertexArrays(vao);
        if (litProgram != 0) glDeleteProgram(litProgram);
        if (depthProgram != 0) glDeleteProgram(depthProgram);
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

    private static final String VS = """
            #version 330 core
            layout (location = 0) in vec3 aPos;
            layout (location = 1) in vec3 aColor;
            layout (location = 2) in vec3 aNormal;
            uniform mat4 uMvp;
            out vec3 vColor;
            out vec3 vPos;
            out vec3 vNormal;
            void main() {
                gl_Position = uMvp * vec4(aPos, 1.0);
                vColor = aColor;
                vPos = aPos;
                vNormal = aNormal;
            }
            """;
    private static final String FS = """
            #version 330 core
            in vec3 vColor;
            in vec3 vPos;
            in vec3 vNormal;
            uniform vec3 uLightPos;
            uniform int uShadowMode;
            uniform sampler2D uShadow0;
            uniform sampler2D uShadow1;
            uniform mat4 uLightMvp0;
            uniform mat4 uLightMvp1;
            uniform float uSplitNear;
            out vec4 fragColor;
            float sampleShadow(sampler2D shadowTex, mat4 lightMvp) {
                vec4 lightSpace = lightMvp * vec4(vPos, 1.0);
                vec3 ndc = lightSpace.xyz / lightSpace.w;
                vec2 uv = ndc.xy * 0.5 + 0.5;
                float depth = ndc.z * 0.5 + 0.5;
                float mapDepth = texture(shadowTex, uv).r;
                return depth - 0.002 > mapDepth ? 0.35 : 1.0;
            }
            void main() {
                vec3 n = normalize(vNormal);
                vec3 l = normalize(uLightPos - vPos);
                float diff = max(dot(n, l), 0.15);
                float shadow = 1.0;
                if (uShadowMode == 1) {
                    shadow = vPos.z < uSplitNear ? sampleShadow(uShadow0, uLightMvp0) : sampleShadow(uShadow1, uLightMvp1);
                }
                fragColor = vec4(vColor * diff * shadow, 1.0);
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
}
