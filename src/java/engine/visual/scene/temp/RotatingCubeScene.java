package engine.visual.scene.temp;

import engine.visual.scene.SceneIds;
import engine.visual.scene.SceneTemplate;
import engine.visual.scene.SceneType;

import engine.util.resource.Unloader;
import engine.visual.scene.TriangleRenderer3D;

import java.util.Arrays;

public final class RotatingCubeScene extends SceneTemplate {

    private static final float[][] CUBE_VERTICES = {
            {-1f, -1f, -1f}, {1f, -1f, -1f}, {1f, 1f, -1f}, {-1f, 1f, -1f},
            {-1f, -1f, 1f}, {1f, -1f, 1f}, {1f, 1f, 1f}, {-1f, 1f, 1f}
    };
    private static final int[][] TRIANGLES = {
            {0, 1, 2}, {0, 2, 3},
            {4, 6, 5}, {4, 7, 6},
            {0, 3, 7}, {0, 7, 4},
            {1, 5, 6}, {1, 6, 2},
            {3, 2, 6}, {3, 6, 7},
            {0, 4, 5}, {0, 5, 1}
    };

    private float angle;
    private final TriangleRenderer3D triangleRenderer = new TriangleRenderer3D();

    public RotatingCubeScene() {
        super(SceneIds.ROTATING_CUBE, SceneType.THREE_D);
    }

    @Override
    public void initialize(Unloader resources) {
        angle = 0f;
        triangleRenderer.init();
        resources.track(triangleRenderer::destroy);
    }

    @Override
    public void update(float deltaSeconds) {
        angle += deltaSeconds * 1.5f;
    }

    @Override
    public void render3D() {
        float[][] projected = new float[CUBE_VERTICES.length][3];
        for (int index = 0; index < CUBE_VERTICES.length; index++) {
            projected[index] = project(rotate(CUBE_VERTICES[index], angle));
        }

        int[] order = depthSortedTriangleOrder(projected);
        float[] mesh = new float[order.length * 3 * 6];
        int offset = 0;
        for (int triangleIndex : order) {
            int[] triangle = TRIANGLES[triangleIndex];
            float[] a = projected[triangle[0]];
            float[] b = projected[triangle[1]];
            float[] c = projected[triangle[2]];
            offset = putVertex(mesh, offset, toNdcX(a[0]), toNdcY(a[1]), a[2] / 6f, 1f, 1f, 1f);
            offset = putVertex(mesh, offset, toNdcX(b[0]), toNdcY(b[1]), b[2] / 6f, 1f, 1f, 1f);
            offset = putVertex(mesh, offset, toNdcX(c[0]), toNdcY(c[1]), c[2] / 6f, 1f, 1f, 1f);
        }
        triangleRenderer.draw(mesh);
    }

    private static float[] rotate(float[] vertex, float angle) {
        float x = vertex[0];
        float y = vertex[1];
        float z = vertex[2];

        float cosY = (float) Math.cos(angle);
        float sinY = (float) Math.sin(angle);
        float rx = (x * cosY) - (z * sinY);
        float rz = (x * sinY) + (z * cosY);

        float pitch = angle * 0.6f;
        float cosX = (float) Math.cos(pitch);
        float sinX = (float) Math.sin(pitch);
        float ry = (y * cosX) - (rz * sinX);
        float rzz = (y * sinX) + (rz * cosX);

        return new float[] {rx, ry, rzz};
    }

    private static float[] project(float[] vertex) {
        float distance = 4f;
        float scale = 150f;
        float depthFactor = scale / (vertex[2] + distance);
        float screenX = 480f + (vertex[0] * depthFactor);
        float screenY = 270f + (vertex[1] * depthFactor);
        return new float[] {screenX, screenY, vertex[2]};
    }

    private static int[] depthSortedTriangleOrder(float[][] vertices) {
        float[] depths = new float[TRIANGLES.length];
        Integer[] order = new Integer[TRIANGLES.length];
        for (int index = 0; index < TRIANGLES.length; index++) {
            int[] triangle = TRIANGLES[index];
            depths[index] = (vertices[triangle[0]][2] + vertices[triangle[1]][2] + vertices[triangle[2]][2]) / 3f;
            order[index] = index;
        }

        Arrays.sort(order, (left, right) -> Float.compare(depths[right], depths[left]));
        int[] sorted = new int[order.length];
        for (int index = 0; index < order.length; index++) {
            sorted[index] = order[index];
        }
        return sorted;
    }

    private static float toNdcX(float x) {
        return (x / 480f) - 1f;
    }

    private static float toNdcY(float y) {
        return 1f - (y / 270f);
    }

    private static int putVertex(float[] target,
                                 int offset,
                                 float x,
                                 float y,
                                 float z,
                                 float r,
                                 float g,
                                 float b) {
        target[offset] = x;
        target[offset + 1] = y;
        target[offset + 2] = z;
        target[offset + 3] = r;
        target[offset + 4] = g;
        target[offset + 5] = b;
        return offset + 6;
    }
}
