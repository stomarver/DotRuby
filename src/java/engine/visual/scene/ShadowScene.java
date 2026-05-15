package engine.visual.scene;

import engine.util.res.Unloader;

import static org.lwjgl.opengl.GL11.GL_FILL;
import static org.lwjgl.opengl.GL11.GL_FRONT_AND_BACK;
import static org.lwjgl.opengl.GL11.GL_LINE;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glPolygonMode;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20.glGetProgrami;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderi;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public final class ShadowScene extends SceneTemplate {
    private static final String VERT = "#version 330 core\nlayout(location=0) in vec3 aPos;uniform float uAngle;void main(){float c=cos(uAngle),s=sin(uAngle);mat3 r=mat3(c,0,s,0,1,0,-s,0,c);vec3 p=r*aPos;gl_Position=vec4(p*0.5,1.0);}";
    private static final String FRAG = "#version 330 core\nout vec4 FragColor;void main(){FragColor=vec4(0.92,0.88,0.74,1.0);}";

    private int vao;
    private int vbo;
    private int program;
    private float angle;
    private boolean wireframe;

    public ShadowScene() { super("scene.shadow-clean", SceneType.THREE_D); }

    @Override
    public void initialize(Unloader resources) {
        program = createProgram(VERT, FRAG);
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, new float[]{-1,-1,0, 1,-1,0, 0,1,0}, GL_STATIC_DRAW);
        glVertexAttribPointer(0,3,org.lwjgl.opengl.GL11.GL_FLOAT,false,12,0);
        glEnableVertexAttribArray(0);
        resources.track(() -> { glDeleteProgram(program); glDeleteVertexArrays(vao); org.lwjgl.opengl.GL15.glDeleteBuffers(vbo); });
    }

    @Override public void update(float deltaSeconds) { angle += deltaSeconds; wireframe = ((int) (angle * 2f)) % 2 == 0; }

    @Override
    public void render3D() {
        glPolygonMode(GL_FRONT_AND_BACK, wireframe ? GL_LINE : GL_FILL);
        glUseProgram(program);
        glUniform1f(glGetUniformLocation(program, "uAngle"), angle);
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
    }

    private static int createProgram(String vs, String fs) {
        int vertex = compile(GL_VERTEX_SHADER, vs);
        int fragment = compile(GL_FRAGMENT_SHADER, fs);
        int linked = glCreateProgram();
        glAttachShader(linked, vertex);
        glAttachShader(linked, fragment);
        glLinkProgram(linked);
        if (glGetProgrami(linked, GL_LINK_STATUS) == 0) {
            throw new IllegalStateException(glGetProgramInfoLog(linked));
        }
        glDeleteShader(vertex);
        glDeleteShader(fragment);
        return linked;
    }

    private static int compile(int type, String src) {
        int shader = glCreateShader(type);
        glShaderSource(shader, src);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0) {
            throw new IllegalStateException(glGetShaderInfoLog(shader));
        }
        return shader;
    }
}
