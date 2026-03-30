package engine.visual.scene;

public interface Scene {

    String id();

    void initialize();

    void update(float deltaSeconds);

    void render();

    void destroy();
}
