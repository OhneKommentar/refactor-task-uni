package com.instakill.infrastructure.config;

import com.instakill.common.id.IdGenerator;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UuidGenerator implements IdGenerator {
    @Override
    public UUID generate() {
        return UUID.randomUUID();
    }
}
