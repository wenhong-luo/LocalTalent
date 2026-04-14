package cn.localtalent.backend.common.trace;

public final class TraceIdContext {

    private static final ThreadLocal<String> TRACE_ID_HOLDER = new ThreadLocal<>();

    private TraceIdContext() {
    }

    public static void setCurrentTraceId(String traceId) {
        TRACE_ID_HOLDER.set(traceId);
    }

    public static String getCurrentTraceId() {
        return TRACE_ID_HOLDER.get();
    }

    public static void clear() {
        TRACE_ID_HOLDER.remove();
    }
}
