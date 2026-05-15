package engine.visual.scene;

import engine.visual.shader.Shader;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public final class TriangleRenderer3D {

    private static final String VERTEX_SHADER = """
            #version 330 core
            layout (location = 0) in vec3 aPosition;
            layout (location = 1) in vec3 aColor;

            out vec3 vColor;

            void main() {
                gl_Position = vec4(aPosition, 1.0);
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

    private final Shader shader = new Shader();
    private int programId;
    private int vaoId;
    private int vboId;

    public void init() {
        if (programId != 0) {
            return;
        }

        programId = shader.program(VERTEX_SHADER, FRAGMENT_SHADER);
        vaoId = glGenVertexArrays();
        vboId = glGenBuffers();

        glBindVertexArray(vaoId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, new float[] {0f, 0f, 0f, 1f, 1f, 1f}, GL_DYNAMIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 6 * Float.BYTES, 0L);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 6 * Float.BYTES, 3L * Float.BYTES);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void draw(float[] positionAndColorInterleaved) {
        if (programId == 0 || vaoId == 0 || vboId == 0) {
            throw new IllegalStateException("TriangleRenderer3D is not initialized");
        }
        if (positionAndColorInterleaved == null || positionAndColorInterleaved.length < 18 || (positionAndColorInterleaved.length % 6 != 0)) {
            throw new IllegalArgumentException("Triangle data must be interleaved as xyzrgb and contain at least one triangle");
        }

        glUseProgram(programId);
        glBindVertexArray(vaoId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, positionAndColorInterleaved, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glDrawArrays(GL_TRIANGLES, 0, positionAndColorInterleaved.length / 6);
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
