package com.safa.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safa.payment.dto.SlackErrorMessageDto;
import com.safa.payment.dto.SlackPostMessage;
import com.safa.payment.dto.telr.ErrorDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class RestService {

    public static final String PAYMENT_SERVICE_ERROR = "Payment Service Error";
    public static final String ICON = ":interrobang:";
    public static final String BEARER = "Bearer ";
    private final RestTemplate restTemplate;

    @Value("${slack.baseUrl:}")
    private String slackUrl;

    @Value("${slack.channel.error:}")
    private String errorChannel;

    @Value("${slack.token:}")
    private String slackToken;

    public RestService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }


    public String postErrorMessage(final String message) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, BEARER + slackToken);
        SlackPostMessage slackPostMessage = new SlackPostMessage(PAYMENT_SERVICE_ERROR, ICON, errorChannel, message);
        ObjectMapper objectMapper = new ObjectMapper();
        HttpEntity<SlackPostMessage> httpEntity = new HttpEntity<>(slackPostMessage, headers);
        var response = restTemplate.exchange(slackUrl, HttpMethod.POST, httpEntity, String.class);
        return response.getBody();
    }


    @EventListener(SlackErrorMessageDto.class)
    public String postErrorMessage(SlackErrorMessageDto slackErrorMessageDto) {
        return this.postErrorMessage(slackErrorMessageDto.getErrorMessage());
    }


}
