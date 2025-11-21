package com.reliaquest.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

@ExtendWith(MockitoExtension.class)
class CorrelationIdFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private CorrelationIdFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        MDC.clear();
    }

    @Test
    void doFilterInternal_headerPresent_usesExistingId() throws Exception {
        String existingId = "existing-correlation-id";
        when(request.getHeader("X-Correlation-ID")).thenReturn(existingId);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader("X-Correlation-ID", existingId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_headerMissing_generatesNewUuid() throws Exception {
        when(request.getHeader("X-Correlation-ID")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(eq("X-Correlation-ID"), captor.capture());

        String generatedId = captor.getValue();
        assertThat(generatedId).isNotNull();
        assertThat(generatedId).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_headerBlank_generatesNewUuid() throws Exception {
        when(request.getHeader("X-Correlation-ID")).thenReturn("   ");

        filter.doFilterInternal(request, response, filterChain);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(eq("X-Correlation-ID"), captor.capture());

        String generatedId = captor.getValue();
        assertThat(generatedId).isNotBlank();
        assertThat(generatedId).doesNotContain(" ");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_setsMdcDuringRequest() throws Exception {
        String existingId = "test-correlation-id";
        when(request.getHeader("X-Correlation-ID")).thenReturn(existingId);

        doAnswer(invocation -> {
            assertThat(MDC.get("correlationId")).isEqualTo(existingId);
            return null;
        })
                .when(filterChain)
                .doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_clearsMdcAfterRequest() throws Exception {
        when(request.getHeader("X-Correlation-ID")).thenReturn("test-id");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void doFilterInternal_clearsMdcEvenOnException() throws Exception {
        when(request.getHeader("X-Correlation-ID")).thenReturn("test-id");
        doThrow(new RuntimeException("Test exception")).when(filterChain).doFilter(request, response);

        try {
            filter.doFilterInternal(request, response, filterChain);
        } catch (RuntimeException e) {
            // Expected
        }

        assertThat(MDC.get("correlationId")).isNull();
    }
}
