package engine.display;

public class Window {

    private final Manager displayManager;
    private final engine.input.Manager inputManager;
    private final config.Display displayConfig;
    private final config.Input inputConfig;

    public Window() {
        this(Config.defaults(), config.Display.defaults(), config.Input.defaults());
    }

    public Window(Config displayManagerConfig) {
        this(displayManagerConfig, config.Display.defaults(), config.Input.defaults());
    }

    public Window(Config displayManagerConfig, config.Display displayConfig, config.Input inputConfig) {
        this.displayManager = new Manager(displayManagerConfig);
        this.displayConfig = displayConfig;
        this.inputConfig = inputConfig;
        this.inputManager = new engine.input.Manager(
                engine.input.Config.defaults().withRawMouseInput(displayConfig.isRawInputEnabled())
        );
    }

    public void run() {
        create();
        loop();
        destroy();
    }

    public void create() {
        displayManager.createWindow();
        displayManager.applyVSync(displayConfig.getVSync());
        displayManager.setMode(displayConfig.getWindowMode());
        inputManager.bind(displayManager.getWindowHandle(), displayManager, inputConfig, displayConfig);
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
