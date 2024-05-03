package com.safa.payment.service.gateway.Telr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.safa.payment.dto.common.HostedPaymentRequestDto;
import com.safa.payment.dto.common.HostedPaymentResponseDto;
import com.safa.payment.dto.telr.TransactionDetails;
import com.safa.payment.exception.InternalPaymentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Base64;

@Service
@Slf4j
public class TelrRestService {

    private final RestTemplate restTemplate;

    @Value("${telr.hosted-payment.url}")
    String hostedPaymentUrl;

    @Value("${telr.transaction-details.url:https://secure.innovatepayments.com/tools/api/xml/transaction/%s}")
    private String transactionDetailsUrl;

    @Value("${telr.cancel-repeat-payment-agreement.url:https://secure.innovatepayments.com/tools/api/xml/agreement/%s}")
    private String cancelRepeatPaymentAgreementUrl;

    /**
     * Telr service apis access id
     * See documentation https://telr.com/support/knowledge-base/service-api/
     */
    @Value("${telr.service-api.merchant-id:}")
    private String telrServiceApiMerchantId;

    /**
     * Telr service apis secret key
     * See documentation https://telr.com/support/knowledge-base/service-api/
     */
    @Value("${telr.service-api.api-key:}")
    private String telrServiceApiKey;


    public TelrRestService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    public HostedPaymentResponseDto sendHostedPaymentRequest(HostedPaymentRequestDto hostedPaymentRequestDto) {
        log.info(
                "Sending request to Telr hosted payment gateway in order to get page URL, request details: {}",
                hostedPaymentRequestDto.toString());
        return this.restTemplate.postForObject(
                hostedPaymentUrl, hostedPaymentRequestDto, HostedPaymentResponseDto.class);
    }

    /**
     * Get payment transaction details from telr using payment transaction reference id
     * see documentation https://telr.com/support/knowledge-base/service-api/
     */
    public TransactionDetails getPaymentTransactionDetails(String transactionReference) {
        String transactionDetailsUrlWithReference = String.format(transactionDetailsUrl, transactionReference);
        HttpHeaders headers = createTelrServiceApisAuthHeader();

        log.info("Sending request to get payment transaction details for transaction reference: {}", transactionReference);

        try {
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> responseEntity = restTemplate.exchange(transactionDetailsUrlWithReference, HttpMethod.GET, requestEntity, String.class);
            String xmlResponse = responseEntity.getBody();

            // Parse the XML response into the TransactionDetails class
            ObjectMapper xmlMapper = new XmlMapper();

            return xmlMapper.readValue(xmlResponse, TransactionDetails.class);
        } catch (RestClientException e) {
            log.error("Error occurred while getting payment transaction details, transaction reference: {}", transactionReference, e);
            throw new InternalPaymentException(String.format("Error occurred while getting payment transaction details, transaction reference %s", transactionReference), e);
        } catch (IOException e) {
            log.error("Failed parsing response from get transaction details API, transaction reference: {}", transactionReference, e);
            throw new InternalPaymentException("Failed parsing response from get transaction details API", e);
        }
    }


    /**
     * Cancel repeat payment agreement by agreement Id
     * see documentation https://telr.com/support/knowledge-base/service-api/
     */
    public void cancelRepeatPaymentAgreement(String agreementId) {
        String url = String.format(cancelRepeatPaymentAgreementUrl, agreementId);
        HttpHeaders headers = createTelrServiceApisAuthHeader();

        log.info("Sending request to cancel repeat payment, agreement id: {}", agreementId);

        try {
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<Void> responseEntity = restTemplate.exchange(url, HttpMethod.DELETE, requestEntity, Void.class);

            if (responseEntity.getStatusCode().isError()) {
                String responseBody = responseEntity.getBody() != null ? responseEntity.getBody().toString() : "";
                log.error("Cancelling payment agreement failed, agreementId ({}), response: {}", agreementId, responseBody);
                throw new InternalPaymentException(String.format("Cancelling payment agreement failed, agreement id %s, response %s", agreementId, responseBody));
            }
        } catch (RestClientException e) {
            log.error("Error occurred while cancelling payment agreement, agreementId: {}", agreementId, e);
            throw new InternalPaymentException(String.format("Error occurred while cancelling payment agreement, agreement id %s", agreementId), e);
        }
    }


    /**
     * Create Authorization for telr service apis, based on basic authentication of api merchant id and api key
     * see documentation https://telr.com/support/knowledge-base/service-api/
     */
    private HttpHeaders createTelrServiceApisAuthHeader() {
        HttpHeaders headers = new HttpHeaders();
        String credentials = telrServiceApiMerchantId + ":" + telrServiceApiKey;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
        headers.set("Authorization", "Basic " + encodedCredentials);
        return headers;
    }

}
