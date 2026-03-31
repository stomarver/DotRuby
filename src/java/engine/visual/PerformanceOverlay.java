package engine.visual;

import engine.util.sys.Specs;

public final class PerformanceOverlay {

    private static final long REFRESH_INTERVAL_MS = 400L;

    private String text = "CPU: <loading>\nGPU: <loading>\nRAM: <loading>";
    private long nextRefreshAtMs;

    public Event pollUpdateEvent() {
        long now = System.currentTimeMillis();
        if (now < nextRefreshAtMs) {
            return null;
        }

        nextRefreshAtMs = now + REFRESH_INTERVAL_MS;
        String updated = "CPU: " + Specs.cpuLoadPercent()
                + "\nGPU: " + Specs.gpuLoadPercent()
                + "\nRAM: " + Specs.ramLoadPercent();
        return new Event(Event.Type.PERFORMANCE_TEXT_UPDATED, updated);
    }

    public void apply(Event event) {
        if (event == null || event.getType() != Event.Type.PERFORMANCE_TEXT_UPDATED) {
            return;
        }
        text = event.getText();
    }

    public void render(Overlay overlay, Render textRender) {
        textRender.drawText(overlay, text, 16f, 520f, 1f);
    }
}
