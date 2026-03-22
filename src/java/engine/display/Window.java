package engine.display;

public class Window {

    private final Manager displayManager;
    private final engine.input.Manager inputManager;

    public Window() {
        this(Config.defaults());
    }

    public Window(Config config) {
        this.displayManager = new Manager(config);
        this.inputManager = new engine.input.Manager();
    }

    public void run() {
        create();
        loop();
        destroy();
    }

    public void create() {
        displayManager.createWindow();
        inputManager.bind(displayManager.getWindowHandle());
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

    public engine.input.Manager getInputManager() {
        return inputManager;
    }
}
