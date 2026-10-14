package com.instakill.infrastructure.config;

import com.instakill.common.clock.Clock;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class SystemClock implements Clock {
    @Override
    public Instant now() {
        return Instant.now();
    }
}
