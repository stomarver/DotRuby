package engine.display;

public class Window {

    private final Manager displayManager;

    public Window() {
        this(Config.defaults());
    }

    public Window(Config config) {
        this.displayManager = new Manager(config);
    }

    public void run() {
        create();
        loop();
        destroy();
    }

    public void create() {
        displayManager.createWindow();
    }

    public void loop() {
        while (!displayManager.shouldClose()) {
            displayManager.clearFrame();
            displayManager.updateFrame();
        }
    }

    public void destroy() {
        displayManager.destroyWindow();
    }

    public long getHandle() {
        return displayManager.getWindowHandle();
    }

    public Manager getDisplayManager() {
        return displayManager;
    }
}
