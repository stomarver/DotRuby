package engine.visual.scene;

import engine.util.res.Unloader;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

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
import static org.lwjgl.opengl.GL20.glDrawArrays;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20.glGetProgrami;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderi;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL30.GL_FLOAT;
import static org.lwjgl.opengl.GL30.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL30.GL_TRIANGLES;
import static org.lwjgl.opengl.GL30.glBindBuffer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glBufferData;
import static org.lwjgl.opengl.GL30.glDeleteBuffers;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenBuffers;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

public final class RotatingCubeScene extends SceneTemplate {

    private static final float[] VERTICES = {
            // pos               // color
            -0.6f, -0.6f, 0.0f,  1f, 0.2f, 0.2f,
             0.6f, -0.6f, 0.0f,  0.2f, 1f, 0.3f,
             0.0f,  0.6f, 0.0f,  0.2f, 0.4f, 1f
    };

    private static final String VERTEX_SHADER = """
            #version 330 core
            layout(location = 0) in vec3 aPosition;
            layout(location = 1) in vec3 aColor;
            uniform mat4 uRotation;
            out vec3 vColor;
            void main() {
                gl_Position = uRotation * vec4(aPosition, 1.0);
                vColor = aColor;
            }
            """;

    private static final String FRAGMENT_SHADER = """
            #version 330 core
            in vec3 vColor;
            out vec4 fragColor;
            void main() {
                fragColor = vec4(vColor, 1.0);
            }
            """;

    private int vao;
    private int vbo;
    private int program;
    private int rotationUniform;
    private float angle;

    public RotatingCubeScene() {
        super(SceneIds.ROTATING_CUBE, SceneType.THREE_D);
    }

    @Override
    public void initialize(Unloader resources) {
        angle = 0f;
        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        rotationUniform = glGetUniformLocation(program, "uRotation");

        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, VERTICES, GL_STATIC_DRAW);

        int stride = Float.BYTES * 6;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, Float.BYTES * 3L);
        glEnableVertexAttribArray(1);
        glBindVertexArray(0);
    }

    @Override
    public void update(float deltaSeconds) {
        angle += deltaSeconds;
    }

    @Override
    public void render3D() {
        glUseProgram(program);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer matrix = stack.mallocFloat(16);
            float c = (float) Math.cos(angle);
            float s = (float) Math.sin(angle);
            matrix.put(new float[]{
                    c, -s, 0f, 0f,
                    s, c, 0f, 0f,
                    0f, 0f, 1f, 0f,
                    0f, 0f, 0f, 1f
            }).flip();
            glUniformMatrix4fv(rotationUniform, false, matrix);
        }

        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glBindVertexArray(0);
    }

    @Override
    public void destroy() {
        if (vbo != 0) {
            glDeleteBuffers(vbo);
            vbo = 0;
        }
        if (vao != 0) {
            glDeleteVertexArrays(vao);
            vao = 0;
        }
        if (program != 0) {
            glDeleteProgram(program);
            program = 0;
        }
    }

    private static int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = compileShader(GL_VERTEX_SHADER, vertexSource);
        int fragmentShader = compileShader(GL_FRAGMENT_SHADER, fragmentSource);
        int shaderProgram = glCreateProgram();
        glAttachShader(shaderProgram, vertexShader);
        glAttachShader(shaderProgram, fragmentShader);
        glLinkProgram(shaderProgram);

        if (glGetProgrami(shaderProgram, GL_LINK_STATUS) == 0) {
            String message = glGetProgramInfoLog(shaderProgram);
            glDeleteShader(vertexShader);
            glDeleteShader(fragmentShader);
            glDeleteProgram(shaderProgram);
            throw new IllegalStateException("Shader link failed: " + message);
        }

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
        return shaderProgram;
    }

    private static int compileShader(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);

        if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0) {
            String message = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            throw new IllegalStateException("Shader compile failed: " + message);
        }

        return shader;
    }
}
