package com.safa.payment.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class KafkaEventDto<T> implements Serializable {

    private Long producerUserId;

    private String eventName;

    private String eventType;

    private Date eventTime;

    private String eventSource;

    private T details;

}
