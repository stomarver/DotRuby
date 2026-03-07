package engine;

import engine.display.Window;

public class Launcher {

    public static void main(String[] args) {
        System.out.println("launching");

        // Создаём и запускаем основное окно
        Window window = new Window();
        window.run();

        System.out.println("closing");
    }
}