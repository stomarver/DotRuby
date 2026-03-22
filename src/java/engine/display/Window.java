package engine.display;

public class Window {

    private final Manager displayManager;
    private final engine.input.Manager inputManager;

    public Window() {
        this(Config.loadDefault(), engine.input.Config.loadDefault(Config.loadDefault().isRawInputEnabled()));
    }

    public Window(Config displayConfig) {
        this(displayConfig, engine.input.Config.loadDefault(displayConfig.isRawInputEnabled()));
    }

    public Window(Config displayConfig, engine.input.Config inputConfig) {
        this.displayManager = new Manager(displayConfig);
        this.inputManager = new engine.input.Manager(inputConfig);
    }

    public void run() {
        create();
        loop();
        destroy();
    }

    public void create() {
        displayManager.createWindow();
        displayManager.applyVSync(displayManager.getVSync());
        displayManager.setFullscreen(displayManager.getFullscreen());
        displayManager.setMode(displayManager.getMode());
        inputManager.bind(displayManager.getWindowHandle(), displayManager);
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
