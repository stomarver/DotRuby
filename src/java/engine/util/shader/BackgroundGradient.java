package engine.util.shader;

import engine.visual.shader.Shader;

import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniform2f;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public final class BackgroundGradient {

    private static final float[] FULLSCREEN_TRIANGLE = {
            -1f, -1f,
            3f, -1f,
            -1f, 3f
    };

    private static final String VERTEX_SHADER = """
            #version 330 core
            layout (location = 0) in vec2 aPosition;

            void main() {
                gl_Position = vec4(aPosition, 0.0, 1.0);
            }
            """;

    private static final String FRAGMENT_SHADER = """
            #version 330 core

            uniform vec2 uResolution;
            uniform float uTime;

            out vec4 fragColor;

            vec3 palette(float t) {
                vec3 darkBlue = vec3(0.02, 0.05, 0.20);
                vec3 blue = vec3(0.00, 0.25, 0.68);
                vec3 cyan = vec3(0.16, 0.76, 0.95);
                vec3 turquoise = vec3(0.10, 0.82, 0.72);

                if (t < 0.33) {
                    return mix(darkBlue, blue, smoothstep(0.0, 0.33, t));
                }
                if (t < 0.66) {
                    return mix(blue, cyan, smoothstep(0.33, 0.66, t));
                }
                return mix(cyan, turquoise, smoothstep(0.66, 1.0, t));
            }

            void main() {
                vec2 uv = gl_FragCoord.xy / max(uResolution, vec2(1.0));

                float vertical = uv.y;
                vertical += 0.08 * sin(uTime * 1.20 + uv.x * 7.5);
                vertical += 0.04 * sin(uTime * 0.85 + uv.x * 13.0 + uv.y * 5.0);
                vertical = clamp(vertical, 0.0, 1.0);

                vec3 color = palette(vertical);
                float pulse = 0.90 + 0.10 * sin(uTime * 1.40 + uv.y * 8.5);
                color *= pulse;

                fragColor = vec4(color, 1.0);
            }
            """;

    private final Shader shader = new Shader();
    private int programId;
    private int vaoId;
    private int vboId;
    private int resolutionLocation;
    private int timeLocation;

    public void init() {
        if (programId != 0) {
            return;
        }

        programId = shader.program(VERTEX_SHADER, FRAGMENT_SHADER);
        resolutionLocation = glGetUniformLocation(programId, "uResolution");
        timeLocation = glGetUniformLocation(programId, "uTime");

        vaoId = glGenVertexArrays();
        vboId = glGenBuffers();

        glBindVertexArray(vaoId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, FULLSCREEN_TRIANGLE, GL_DYNAMIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0L);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void renderFullscreen(int framebufferWidth, int framebufferHeight, float timeSeconds) {
        render(FULLSCREEN_TRIANGLE, framebufferWidth, framebufferHeight, timeSeconds);
    }

    public void render(float[] ndcVertices, int framebufferWidth, int framebufferHeight, float timeSeconds) {
        if (programId == 0 || vaoId == 0 || vboId == 0) {
            throw new IllegalStateException("BackgroundGradient is not initialized");
        }
        if (ndcVertices == null || ndcVertices.length < 6 || (ndcVertices.length % 2 != 0)) {
            throw new IllegalArgumentException("Gradient vertices must contain at least 3 XY points");
        }

        glDisable(GL_DEPTH_TEST);
        glUseProgram(programId);
        glUniform2f(resolutionLocation, Math.max(1, framebufferWidth), Math.max(1, framebufferHeight));
        glUniform1f(timeLocation, timeSeconds);

        glBindVertexArray(vaoId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, ndcVertices, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        glDrawArrays(GL_TRIANGLES, 0, ndcVertices.length / 2);

        glBindVertexArray(0);
        glUseProgram(0);
    }

    public void destroy() {
        if (vboId != 0) {
            glDeleteBuffers(vboId);
            vboId = 0;
        }
        if (vaoId != 0) {
            glDeleteVertexArrays(vaoId);
            vaoId = 0;
        }
        if (programId != 0) {
            glDeleteProgram(programId);
            programId = 0;
        }
    }
}
