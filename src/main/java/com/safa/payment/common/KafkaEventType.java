package com.safa.payment.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum KafkaEventType {
    CREATE("create"),
    UPDATE("update"),

    DELETE("delete");

    public final String label;
}
