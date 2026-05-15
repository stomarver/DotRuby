package engine.visual.scene;

import engine.util.resource.Unloader;
import engine.visual.Overlay;
import engine.visual.Render;
import engine.visual.shader.Shader;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_F;
import static org.lwjgl.glfw.GLFW.glfwGetCurrentContext;
import static org.lwjgl.glfw.GLFW.glfwGetKey;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public final class ShadowPrototypeScene extends SceneTemplate {

    private enum ShadowMode { STENCIL_VOLUMES, CSM }

    private static final float[] GEOMETRY = {
            // plane
            -8f,0f,-8f, 0f,1f,0f,   8f,0f,-8f, 0f,1f,0f,   8f,0f,8f, 0f,1f,0f,
            8f,0f,8f, 0f,1f,0f,    -8f,0f,8f, 0f,1f,0f,   -8f,0f,-8f, 0f,1f,0f,
            // cube (one)
            -1,0,-1, 0,0,-1, 1,0,-1,0,0,-1, 1,2,-1,0,0,-1,
            1,2,-1,0,0,-1, -1,2,-1,0,0,-1, -1,0,-1,0,0,-1,
    };

    private final Shader shader = new Shader();
    private int litProgram;
    private int depthProgram;
    private int vao;
    private int vbo;
    private int shadowFbo;
    private int shadowTex;
    private ShadowMode mode = ShadowMode.STENCIL_VOLUMES;
    private boolean fPressedPrev;
    private float time;

    public ShadowPrototypeScene() { super(SceneIds.SHADOW_PROTOTYPE, SceneType.TWO_D_AND_THREE_D); }

    @Override
    public void initialize(Unloader resources) {
        litProgram = shader.program(VS, FS_LIT);
        depthProgram = shader.program(VS_DEPTH, FS_DEPTH);

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, GEOMETRY, GL_STATIC_DRAW);
        glVertexAttribPointer(0,3,GL_FLOAT,false,6 * Float.BYTES,0L);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1,3,GL_FLOAT,false,6 * Float.BYTES,3L * Float.BYTES);
        glEnableVertexAttribArray(1);
        glBindVertexArray(0);

        shadowFbo = glGenFramebuffers();
        shadowTex = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, shadowTex);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, 1024, 1024, 0, GL_DEPTH_COMPONENT, GL_FLOAT, 0L);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);
        glBindFramebuffer(GL_FRAMEBUFFER, shadowFbo);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, shadowTex, 0);
        glDrawBuffer(GL_NONE);
        glReadBuffer(GL_NONE);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        resources.track(this::destroy);
    }

    @Override
    public void update(float deltaSeconds) {
        time += deltaSeconds;
        boolean fNow = glfwGetKey(glfwGetCurrentContext(), GLFW_KEY_F) == GLFW_PRESS;
        if (fNow && !fPressedPrev) {
            mode = mode == ShadowMode.STENCIL_VOLUMES ? ShadowMode.CSM : ShadowMode.STENCIL_VOLUMES;
        }
        fPressedPrev = fNow;
    }

    @Override
    public void render3D() {
        float lx = (float) (Math.sin(time * 0.7f) * 5.0f);
        float lz = (float) (Math.cos(time * 0.7f) * 5.0f);

        glBindFramebuffer(GL_FRAMEBUFFER, shadowFbo);
        glViewport(0, 0, 1024, 1024);
        glClear(GL_DEPTH_BUFFER_BIT);
        glUseProgram(depthProgram);
        setMat(depthProgram, "uLightVP", Mat.ortho(-10,10,-10,10,0.1f,30f));
        setMat(depthProgram, "uModel", Mat.identity());
        draw();

        int[] vp = new int[4];
        glGetIntegerv(GL_VIEWPORT, vp);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0,0, Math.max(1,vp[2]), Math.max(1,vp[3]));

        glClearColor(0.08f,0.08f,0.12f,1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
        glUseProgram(litProgram);
        setMat(litProgram, "uVP", Mat.perspective((float) Math.toRadians(60), 16f / 9f, 0.1f, 80f));
        setMat(litProgram, "uModel", Mat.identity());
        glUniform3f(glGetUniformLocation(litProgram, "uLightPos"), lx, 6f, lz);
        glUniform1i(glGetUniformLocation(litProgram, "uMode"), mode == ShadowMode.CSM ? 1 : 0);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, shadowTex);
        glUniform1i(glGetUniformLocation(litProgram, "uShadow"), 0);
        draw();
    }

    private void draw() {
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, GEOMETRY.length / 6);
        glBindVertexArray(0);
    }

    @Override
    public void render2D(Overlay overlay, Render textRender) {
        textRender.drawText(overlay, "M: Shadow Scene", 16f, 16f, 1f);
        textRender.drawText(overlay, "F: Toggle mode = " + mode.name(), 16f, 32f, 1f);
    }

    @Override
    public void destroy() {
        if (shadowTex != 0) glDeleteTextures(shadowTex);
        if (shadowFbo != 0) glDeleteFramebuffers(shadowFbo);
        if (vbo != 0) glDeleteBuffers(vbo);
        if (vao != 0) glDeleteVertexArrays(vao);
        if (litProgram != 0) glDeleteProgram(litProgram);
        if (depthProgram != 0) glDeleteProgram(depthProgram);
        shadowTex = shadowFbo = vbo = vao = litProgram = depthProgram = 0;
    }

    private static void setMat(int program, String name, float[] m) { glUniformMatrix4fv(glGetUniformLocation(program, name), false, m); }

    private static final class Mat {
        static float[] identity(){ return new float[]{1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1}; }
        static float[] perspective(float f,float a,float n,float ff){ float t=(float)Math.tan(f*0.5f); return new float[]{1/(a*t),0,0,0,0,1/t,0,0,0,0,-(ff+n)/(ff-n),-1,0,0,-(2*ff*n)/(ff-n),0}; }
        static float[] ortho(float l,float r,float b,float t,float n,float f){ return new float[]{2/(r-l),0,0,0,0,2/(t-b),0,0,0,0,-2/(f-n),0,-(r+l)/(r-l),-(t+b)/(t-b),-(f+n)/(f-n),1}; }
    }

    private static final String VS = "#version 330 core\nlayout(location=0) in vec3 aPos; layout(location=1) in vec3 aNorm; uniform mat4 uVP; uniform mat4 uModel; out vec3 vPos; out vec3 vNorm; void main(){ vec4 w=uModel*vec4(aPos,1.0); vPos=w.xyz; vNorm=mat3(uModel)*aNorm; gl_Position=uVP*w; }";
    private static final String FS_LIT = "#version 330 core\nin vec3 vPos; in vec3 vNorm; out vec4 fragColor; uniform vec3 uLightPos; uniform int uMode; uniform sampler2DShadow uShadow; void main(){ vec3 n=normalize(vNorm); vec3 l=normalize(uLightPos-vPos); float d=max(dot(n,l),0.0); float amb=0.15; float shade=uMode==1?0.9:0.55; fragColor=vec4(vec3(amb+d*shade),1.0);} ";
    private static final String VS_DEPTH = "#version 330 core\nlayout(location=0) in vec3 aPos; uniform mat4 uLightVP; uniform mat4 uModel; void main(){ gl_Position=uLightVP*uModel*vec4(aPos,1.0);} ";
    private static final String FS_DEPTH = "#version 330 core\nvoid main(){}";
}
