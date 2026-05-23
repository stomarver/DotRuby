package engine.util;

public final class EngineConstraints {
    private EngineConstraints() {}

    public static void requireHermeticSpace(boolean condition, String details) {
        if (!condition) {
            throw new IllegalStateException("Engine constraint violation: space must be hermetic. " + details);
        }
    }

    public static void requireIntegerScale(float value, String context) {
        float rounded = Math.round(value);
        if (Math.abs(value - rounded) > 0.0001f) {
            throw new IllegalStateException("Engine constraint violation: fractional text scale is forbidden in " + context + ": " + value);
        }
    }

    public static void requirePixelAligned(double value, String context) {
        double rounded = Math.rint(value);
        if (Math.abs(value - rounded) > 0.0001d) {
            throw new IllegalStateException("Engine constraint violation: subpixel coordinates are forbidden in " + context + ": " + value);
        }
    }
}
