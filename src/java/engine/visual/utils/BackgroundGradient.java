package engine.visual.utils;

import engine.visual.shader.Shader;

import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniform2f;
import static org.lwjgl.opengl.GL20.glUseProgram;

public final class BackgroundGradient {

    private static final String VERTEX_SHADER = """
            #version 330 core

            const vec2 positions[3] = vec2[](
                vec2(-1.0, -1.0),
                vec2( 3.0, -1.0),
                vec2(-1.0,  3.0)
            );

            void main() {
                gl_Position = vec4(positions[gl_VertexID], 0.0, 1.0);
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
    private int resolutionLocation;
    private int timeLocation;

    public void init() {
        if (programId != 0) {
            return;
        }

        programId = shader.program(VERTEX_SHADER, FRAGMENT_SHADER);
        resolutionLocation = glGetUniformLocation(programId, "uResolution");
        timeLocation = glGetUniformLocation(programId, "uTime");
    }

    public void render(int framebufferWidth, int framebufferHeight, float timeSeconds) {
        if (programId == 0) {
            throw new IllegalStateException("BackgroundGradient is not initialized");
        }

        glDisable(GL_DEPTH_TEST);
        glUseProgram(programId);
        glUniform2f(resolutionLocation, Math.max(1, framebufferWidth), Math.max(1, framebufferHeight));
        glUniform1f(timeLocation, timeSeconds);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glUseProgram(0);
    }

    public void destroy() {
        if (programId != 0) {
            glDeleteProgram(programId);
            programId = 0;
        }
    }
}
