package engine;

import engine.display.Window;
import engine.input.Manager;

public class Launcher {

    public static void main(String[] args) {
        System.out.println("launching");

        Window window = new Window();
        window.create();

        Manager inputManager = new Manager();
        inputManager.bind(window.getHandle());

        window.loop();
        window.destroy();

        System.out.println("closing");
    }
}
