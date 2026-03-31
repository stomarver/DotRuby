package engine.display.gl;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public final class Mesh {

    public int createVertexArray() {
        return glGenVertexArrays();
    }

    public int createVertexBuffer(float[] vertices) {
        return createVertexBuffer(vertices, GL_STATIC_DRAW);
    }

    public int createVertexBuffer(float[] vertices, int usage) {
        int bufferId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, bufferId);
        glBufferData(GL_ARRAY_BUFFER, vertices, usage);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        return bufferId;
    }

    public void prepareVertexLayout(int vaoId, int vboId, int attributeIndex, int componentCount, int strideBytes, int offsetBytes) {
        glBindVertexArray(vaoId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glEnableVertexAttribArray(attributeIndex);
        glVertexAttribPointer(attributeIndex, componentCount, GL_FLOAT, false, strideBytes, offsetBytes);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void bindVertexArray(int vaoId) {
        glBindVertexArray(vaoId);
    }

    public void unbindVertexArray() {
        glBindVertexArray(0);
    }

    public void deleteVertexArray(int vaoId) {
        glDeleteVertexArrays(vaoId);
    }

    public void deleteVertexBuffer(int vboId) {
        glDeleteBuffers(vboId);
    }
}
