package engine.visual.scene.temp;

import engine.util.resource.Unloader;
import engine.util.shader.ShadowMaps;
import engine.util.shader.StencilShadows;
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
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.memAllocFloat;
import static org.lwjgl.system.MemoryUtil.memFree;

public final class PortalGridScene extends SceneTemplate {
    private static final float NEAR = 0.1f;
    private static final float FAR = 80f;
    private static final float AMBIENT = 0.2f;

    private enum LightingMode { STENCIL_VOLUMES("Stencil Volumes"), SHADOW_MAPS("ShadowMaps"); private final String label; LightingMode(String l){label=l;} }

    private final Shader shader = new Shader();
    private final ShadowMaps shadowMaps = new ShadowMaps(4096);
    private final StencilShadows stencilShadows = new StencilShadows(240f);
    private final Matrix4f[] lightVP = {new Matrix4f(),new Matrix4f(),new Matrix4f(),new Matrix4f(),new Matrix4f(),new Matrix4f()};

    private float phase;
    private LightingMode mode = LightingMode.STENCIL_VOLUMES;
    private float[] sceneVertices;
    private int vao, vbo, vertexCount;
    private int volumeVao, volumeVbo, volumeVertexCount;
    private int litProgram, depthProgram, volumeProgram;

    public PortalGridScene() { super(SceneIds.PORTAL_GRID, SceneType.TWO_D_AND_THREE_D); }

    @Override public void initialize(Unloader resources) {
        litProgram = shader.program(LIT_VS, LIT_FS);
        depthProgram = shader.program(DEPTH_VS, DEPTH_FS);
        volumeProgram = shader.program(VOLUME_VS, VOLUME_FS);
        createGeometry();
        createVolumeBuffers();
        shadowMaps.init();
        resources.track(this::destroyGpu);
    }

    @Override public void update(float deltaSeconds) { phase += deltaSeconds; }

    @Override public void render3D() {
        glEnable(GL_DEPTH_TEST);
        Vector3f eye = new Vector3f((float)Math.sin(phase * 0.2f) * 13f, 8f, (float)Math.cos(phase * 0.2f) * 13f);
        Matrix4f projection = new Matrix4f().perspective((float)Math.toRadians(60f), 16f/9f, NEAR, FAR);
        Matrix4f view = new Matrix4f().lookAt(eye, new Vector3f(0f, 3f, 0f), new Vector3f(0f,1f,0f));
        Matrix4f vp = new Matrix4f(projection).mul(view);
        Vector3f lightPos = new Vector3f(0f, 7f, 0f);

        Matrix4f[] mats = shadowMaps.buildLightViewProj(lightPos, 0.4f, FAR);
        for (int i=0;i<6;i++) lightVP[i].set(mats[i]);
        renderShadowDepth();

        if (mode == LightingMode.SHADOW_MAPS) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
            renderLit(vp, view, lightPos, true, false);
        } else {
            renderStencilVolumes(vp, view, lightPos);
        }
        resetState();
    }

    @Override public void render2D(Overlay overlay, Render textRender) {
        textRender.drawText(overlay, "Prototype Portal Grid (lamp center)", 16f, 16f, 3f);
        textRender.drawText(overlay, "(F) Mode: " + mode.label, 16f, 48f, 2f);
    }

    public void toggleLightingMode() { mode = mode == LightingMode.STENCIL_VOLUMES ? LightingMode.SHADOW_MAPS : LightingMode.STENCIL_VOLUMES; }

    private void renderStencilVolumes(Matrix4f vp, Matrix4f view, Vector3f lightPos) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
        renderLit(vp, view, lightPos, false, true);

        float[] vol = stencilShadows.buildVolume(sceneVertices, lightPos);
        volumeVertexCount = vol.length / 3;
        glBindBuffer(GL_ARRAY_BUFFER, volumeVbo);
        glBufferData(GL_ARRAY_BUFFER, vol, GL_DYNAMIC_DRAW);

        glEnable(GL_STENCIL_TEST);
        glStencilMask(0xFF);
        glStencilFunc(GL_ALWAYS, 0, 0xFF);
        glColorMask(false,false,false,false);
        glDepthMask(false);
        glEnable(GL_CULL_FACE);

        glUseProgram(volumeProgram);
        setMat4(volumeProgram, "uMvp", vp);
        glBindVertexArray(volumeVao);
        glCullFace(GL_FRONT); glStencilOp(GL_KEEP, GL_DECR_WRAP, GL_KEEP); glDrawArrays(GL_TRIANGLES,0,volumeVertexCount);
        glCullFace(GL_BACK); glStencilOp(GL_KEEP, GL_INCR_WRAP, GL_KEEP); glDrawArrays(GL_TRIANGLES,0,volumeVertexCount);

        glDisable(GL_CULL_FACE);
        glColorMask(true,true,true,true);
        glDepthMask(true);
        glStencilFunc(GL_EQUAL, 0, 0xFF);
        glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
        renderLit(vp, view, lightPos, false, false);
        glDisable(GL_STENCIL_TEST);
    }

    private void renderShadowDepth() {
        glEnable(GL_CULL_FACE);
        glCullFace(GL_FRONT);
        glUseProgram(depthProgram);
        for (int i = 0; i < 6; i++) {
            shadowMaps.beginDepthPass(i);
            setMat4(depthProgram, "uMvp", lightVP[i]);
            drawScene();
            shadowMaps.endDepthPass();
        }
        glDisable(GL_CULL_FACE);
    }

    private void renderLit(Matrix4f vp, Matrix4f view, Vector3f lightPos, boolean useShadows, boolean ambientOnly) {
        glUseProgram(litProgram);
        setMat4(litProgram, "uViewProj", vp);
        setMat4(litProgram, "uView", view);
        glUniform3f(glGetUniformLocation(litProgram, "uLightPos"), lightPos.x, lightPos.y, lightPos.z);
        glUniform3f(glGetUniformLocation(litProgram, "uLampPos"), lightPos.x, lightPos.y, lightPos.z);
        glUniform1i(glGetUniformLocation(litProgram, "uUseShadow"), useShadows ? 1 : 0);
        glUniform1i(glGetUniformLocation(litProgram, "uAmbientOnly"), ambientOnly ? 1 : 0);
        glUniform1f(glGetUniformLocation(litProgram, "uAmbient"), AMBIENT);
        shadowMaps.bindDepthTexture(0);
        glUniform1i(glGetUniformLocation(litProgram, "uShadow"), 0);
        drawScene();
    }

    private void createGeometry() {
        sceneVertices = GeometryFactory.buildDemoGeometry();
        vertexCount = sceneVertices.length / 9;
        vao = glGenVertexArrays(); vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, sceneVertices, GL_STATIC_DRAW);
        glEnableVertexAttribArray(0); glVertexAttribPointer(0,3,GL_FLOAT,false,9*Float.BYTES,0L);
        glEnableVertexAttribArray(1); glVertexAttribPointer(1,3,GL_FLOAT,false,9*Float.BYTES,3L*Float.BYTES);
        glEnableVertexAttribArray(2); glVertexAttribPointer(2,3,GL_FLOAT,false,9*Float.BYTES,6L*Float.BYTES);
        glBindVertexArray(0);
    }
    private void createVolumeBuffers() {
        volumeVao = glGenVertexArrays(); volumeVbo = glGenBuffers();
        glBindVertexArray(volumeVao); glBindBuffer(GL_ARRAY_BUFFER, volumeVbo);
        glBufferData(GL_ARRAY_BUFFER, new float[]{0f,0f,0f}, GL_DYNAMIC_DRAW);
        glEnableVertexAttribArray(0); glVertexAttribPointer(0,3,GL_FLOAT,false,3*Float.BYTES,0L);
        glBindVertexArray(0);
    }
    private void drawScene(){ glBindVertexArray(vao); glDrawArrays(GL_TRIANGLES,0,vertexCount); glBindVertexArray(0);}    
    private void resetState(){ glUseProgram(0); glBindVertexArray(0); glDisable(GL_STENCIL_TEST); glDisable(GL_CULL_FACE); glDepthMask(true); glColorMask(true,true,true,true);}    
    private void destroyGpu(){ if(vbo!=0)glDeleteBuffers(vbo); if(vao!=0)glDeleteVertexArrays(vao); if(volumeVbo!=0)glDeleteBuffers(volumeVbo); if(volumeVao!=0)glDeleteVertexArrays(volumeVao); if(litProgram!=0)glDeleteProgram(litProgram); if(depthProgram!=0)glDeleteProgram(depthProgram); if(volumeProgram!=0)glDeleteProgram(volumeProgram); shadowMaps.destroy(); }
    @Override public void destroy() {}
    private void setMat4(int program, String name, Matrix4f m){ FloatBuffer b= memAllocFloat(16); m.get(b); glUniformMatrix4fv(glGetUniformLocation(program,name),false,b); memFree(b);}    

    private static final String LIT_VS = """
#version 330 core
layout (location=0) in vec3 aPos; layout (location=1) in vec3 aColor; layout (location=2) in vec3 aNormal;
uniform mat4 uViewProj; uniform mat4 uView;
out vec3 vColor; out vec3 vPos; out vec3 vNormal;
void main(){ gl_Position=uViewProj*vec4(aPos,1.0); vColor=aColor; vPos=aPos; vNormal=normalize(aNormal);} 
""";
    private static final String LIT_FS = """
#version 330 core
in vec3 vColor; in vec3 vPos; in vec3 vNormal;
uniform vec3 uLightPos; uniform vec3 uLampPos; uniform int uUseShadow; uniform int uAmbientOnly; uniform float uAmbient; uniform samplerCube uShadow;
out vec4 fragColor;
float shadowFactor(){ vec3 L = vPos - uLampPos; float current = length(L) / 80.0; float closest = texture(uShadow, normalize(L)).r; return current - 0.0015 > closest ? 0.35 : 1.0; }
void main(){ if(uAmbientOnly==1){fragColor=vec4(vColor*uAmbient,1.0); return;} float diff=max(dot(normalize(vNormal),normalize(uLightPos-vPos)),0.0); float sh=uUseShadow==1?shadowFactor():1.0; fragColor=vec4(vColor*(uAmbient+diff*sh),1.0);} 
""";
    private static final String DEPTH_VS = """
#version 330 core
layout (location=0) in vec3 aPos; uniform mat4 uMvp; void main(){ gl_Position=uMvp*vec4(aPos,1.0);} 
""";
    private static final String DEPTH_FS = """
#version 330 core
void main(){}
""";
    private static final String VOLUME_VS = """
#version 330 core
layout (location=0) in vec3 aPos; uniform mat4 uMvp; void main(){ gl_Position=uMvp*vec4(aPos,1.0);} 
""";
    private static final String VOLUME_FS = """
#version 330 core
void main(){}
""";
}
