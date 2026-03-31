package engine.visual;

import java.nio.file.Path;

public final class ModelLoader {

    public record ModelData(String source, int vertexCount, int indexCount) {
    }

    public ModelData loadObj(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("OBJ path must not be null");
        }

        // Placeholder for upcoming manual OBJ parser integration.
        return new ModelData(path.toString(), 0, 0);
    }
}
