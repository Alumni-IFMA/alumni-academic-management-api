package com.alumni.academic_management_api.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.EnableAsync;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncConfigTest {

    @Test
    void givenAsyncConfig_whenCheckAnnotation_thenHasEnableAsync() {
        EnableAsync annotation = AsyncConfig.class.getAnnotation(EnableAsync.class);
        assertThat(annotation).isNotNull();
    }
}
