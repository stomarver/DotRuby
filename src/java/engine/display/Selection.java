package engine.display;

import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glOrtho;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glVertex2f;

public final class Selection {

    private static final float BORDER_THICKNESS = 2f;

    private boolean active;
    private float startX;
    private float startY;
    private float endX;
    private float endY;

    public void begin(float x, float y) {
        active = true;
        startX = x;
        startY = y;
        endX = x;
        endY = y;
    }

    public void update(float x, float y) {
        if (!active) {
            return;
        }
        endX = x;
        endY = y;
    }

    public void clear() {
        active = false;
    }

    public void render(int renderWidth, int renderHeight) {
        if (!active) {
            return;
        }

        float minX = Math.min(startX, endX);
        float maxX = Math.max(startX, endX);
        float minY = Math.min(startY, endY);
        float maxY = Math.max(startY, endY);
        if (maxX <= minX || maxY <= minY) {
            return;
        }

        glDisable(GL_DEPTH_TEST);
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0.0, renderWidth, renderHeight, 0.0, -1.0, 1.0);

        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        glColor4f(1f, 1f, 1f, 1f);
        drawQuad(minX, minY, maxX, minY + BORDER_THICKNESS);
        drawQuad(minX, maxY - BORDER_THICKNESS, maxX, maxY);
        drawQuad(minX, minY, minX + BORDER_THICKNESS, maxY);
        drawQuad(maxX - BORDER_THICKNESS, minY, maxX, maxY);

        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
    }

    private static void drawQuad(float minX, float minY, float maxX, float maxY) {
        glBegin(GL_QUADS);
        glVertex2f(minX, minY);
        glVertex2f(maxX, minY);
        glVertex2f(maxX, maxY);
        glVertex2f(minX, maxY);
        glEnd();
    }
}
