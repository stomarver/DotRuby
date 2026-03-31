package engine.util.res;

import java.util.ArrayDeque;
import java.util.Deque;

public final class Unloader {

    private final Deque<Runnable> disposeStack = new ArrayDeque<>();

    public void track(Runnable disposer) {
        if (disposer != null) {
            disposeStack.push(disposer);
        }
    }

    public void disposeAll() {
        while (!disposeStack.isEmpty()) {
            disposeStack.pop().run();
        }
    }

    public void clear() {
        disposeStack.clear();
    }
}
