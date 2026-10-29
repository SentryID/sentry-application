package dev.sentry.api.domain.accesslog.api;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sentry.api.domain.accesslog.AccessLog;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.PlatformTransactionManager;

class AccessLogServiceTest {

    private final AccessLogRepository accessLogRepository = mock(AccessLogRepository.class);
    private final AccessLogService accessLogService = new AccessLogService(accessLogRepository,
            mock(AccessLogMapper.class), mock(PlatformTransactionManager.class));

    @Test
    void recordAttemptSavesAndTruncatesUserAgent() {
        AccessLog accessLog = new AccessLog();
        accessLog.setUserAgent("x".repeat(600));

        accessLogService.recordAttempt(accessLog);

        verify(accessLogRepository).save(accessLog);
        assertEquals(512, accessLog.getUserAgent().length());
    }

    // CA-F1-10.3, RN-F1-10.6
    @Test
    void recordAttemptNeverFailsTheLogin() {
        when(accessLogRepository.save(any())).thenThrow(new DataAccessResourceFailureException("banco fora"));

        assertDoesNotThrow(() -> accessLogService.recordAttempt(new AccessLog()));
    }
}
