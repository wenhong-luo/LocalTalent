package cn.localtalent.backend.health;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.localtalent.backend.common.exception.GlobalExceptionHandler;
import cn.localtalent.backend.common.trace.TraceIdFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class HealthControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(new HealthController())
                .addFilter(new TraceIdFilter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnHealthPayloadAndEchoIncomingTraceId() throws Exception {
        mockMvc.perform(get("/health").header("X-Trace-Id", "trace-health-test"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", "trace-health-test"))
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.trace_id").value("trace-health-test"))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    void shouldGenerateTraceIdWhenMissing() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.trace_id", not(blankOrNullString())))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }
}
