package cn.localtalent.backend.openapi;

public final class OpenApiContext {

    private static final ThreadLocal<OpenApiRequestContext> HOLDER = new ThreadLocal<>();

    private OpenApiContext() {
    }

    public static void set(OpenApiRequestContext context) {
        HOLDER.set(context);
    }

    public static OpenApiRequestContext current() {
        OpenApiRequestContext context = HOLDER.get();
        if (context == null) {
            throw new IllegalStateException("open api context is missing");
        }
        return context;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
