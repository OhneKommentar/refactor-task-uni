package com.instakill.interaction.domain;

public interface DomainEventPublisher {

    void publish(Object event);
}
