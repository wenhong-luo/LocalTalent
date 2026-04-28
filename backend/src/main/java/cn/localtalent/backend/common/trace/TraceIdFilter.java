package cn.localtalent.backend.common.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String TRACEPARENT_HEADER = "traceparent";
    private static final Pattern TRACEPARENT_TRACE_ID = Pattern.compile("[0-9a-f]{32}");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = traceIdFromTraceparent(request.getHeader(TRACEPARENT_HEADER));
        }
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }

        TraceIdContext.setCurrentTraceId(traceId);
        request.setAttribute(TRACE_ID_HEADER, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            TraceIdContext.clear();
        }
    }

    private String traceIdFromTraceparent(String traceparent) {
        if (traceparent == null || traceparent.isBlank()) {
            return null;
        }
        String[] parts = traceparent.trim().split("-");
        if (parts.length < 4) {
            return null;
        }
        String traceId = parts[1].toLowerCase();
        if (!TRACEPARENT_TRACE_ID.matcher(traceId).matches() || "00000000000000000000000000000000".equals(traceId)) {
            return null;
        }
        return traceId;
    }
}
