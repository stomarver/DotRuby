package engine.visual.scene;

import engine.util.res.Unloader;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glBufferSubData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
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
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20.glGetProgrami;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderi;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniform3f;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import org.lwjgl.BufferUtils;

public final class ShadowSandboxScene extends SceneTemplate {

    private static final String VERTEX = """
            #version 330 core
            layout(location=0) in vec3 aPos;
            uniform float uTime;
            out float vHeight;
            void main() {
                vec3 p = aPos;
                float wave = sin(uTime + p.x * 2.0) * 0.12;
                p.y += wave;
                vHeight = p.y;
                gl_Position = vec4(p, 1.0);
            }
            """;

    private static final String FRAGMENT = """
            #version 330 core
            in float vHeight;
            out vec4 FragColor;
            uniform vec3 uTint;
            void main() {
                float shade = clamp(0.45 + vHeight * 0.9, 0.2, 1.0);
                FragColor = vec4(uTint * shade, 1.0);
            }
            """;

    private int vao;
    private int vbo;
    private int program;
    private float timer;

    public ShadowSandboxScene() {
        super(SceneIds.SHADOW_SANDBOX, SceneType.THREE_D);
    }

    @Override
    public void initialize(Unloader resources) {
        program = createProgram(VERTEX, FRAGMENT);
        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, 36L * Float.BYTES, GL_DYNAMIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0L);
        glEnableVertexAttribArray(0);
        glBindVertexArray(0);

        timer = 0f;
    }

    @Override
    public void update(float deltaSeconds) {
        timer += deltaSeconds;
    }

    @Override
    public void render3D() {
        glEnable(GL_DEPTH_TEST);
        uploadTriangles();

        glUseProgram(program);
        glUniform1f(glGetUniformLocation(program, "uTime"), timer);
        glUniform3f(glGetUniformLocation(program, "uTint"), 0.90f, 0.70f, 0.35f);
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, 12);

        glUniform3f(glGetUniformLocation(program, "uTint"), 0.35f, 0.80f, 0.95f);
        glDrawArrays(GL_TRIANGLES, 12, 12);

        glBindVertexArray(0);
        glUseProgram(0);
        glDisable(GL_DEPTH_TEST);
    }

    @Override
    public void destroy() {
        if (program != 0) {
            glDeleteProgram(program);
            program = 0;
        }
        if (vbo != 0) {
            glDeleteBuffers(vbo);
            vbo = 0;
        }
        if (vao != 0) {
            glDeleteVertexArrays(vao);
            vao = 0;
        }
    }

    private void uploadTriangles() {
        float z = -0.2f;
        float[] vertices = {
                -0.75f, -0.45f, z,  -0.10f, -0.45f, z,  -0.10f, 0.20f, z,
                -0.75f, -0.45f, z,  -0.10f, 0.20f, z,   -0.75f, 0.20f, z,

                -0.30f, 0.20f, z,   -0.10f, 0.20f, z,   -0.10f, 0.55f, z,
                -0.30f, 0.20f, z,   -0.10f, 0.55f, z,   -0.30f, 0.55f, z,

                0.10f, -0.45f, z,   0.75f, -0.45f, z,   0.75f, 0.20f, z,
                0.10f, -0.45f, z,   0.75f, 0.20f, z,    0.10f, 0.20f, z,

                0.10f, 0.20f, z,    0.30f, 0.20f, z,    0.30f, 0.55f, z,
                0.10f, 0.20f, z,    0.30f, 0.55f, z,    0.10f, 0.55f, z
        };

        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
        buffer.put(vertices).flip();

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, buffer);
    }

    private static int createProgram(String vertexSource, String fragmentSource) {
        int vertex = compileShader(GL_VERTEX_SHADER, vertexSource);
        int fragment = compileShader(GL_FRAGMENT_SHADER, fragmentSource);

        int shaderProgram = glCreateProgram();
        glAttachShader(shaderProgram, vertex);
        glAttachShader(shaderProgram, fragment);
        glLinkProgram(shaderProgram);
        if (glGetProgrami(shaderProgram, GL_LINK_STATUS) == 0) {
            String info = glGetProgramInfoLog(shaderProgram);
            throw new IllegalStateException("Shader link failed: " + info);
        }

        glDeleteShader(vertex);
        glDeleteShader(fragment);
        return shaderProgram;
    }

    private static int compileShader(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0) {
            String info = glGetShaderInfoLog(shader);
            throw new IllegalStateException("Shader compile failed: " + info);
        }
        return shader;
    }
}
