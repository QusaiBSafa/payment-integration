package com.safa.payment.service.gateway.Noon;

import com.safa.payment.common.PaymentGateway;
import com.safa.payment.common.PaymentReferenceType;
import com.safa.payment.common.PaymentTransactionPaymentStatus;
import com.safa.payment.common.noon.NoonPaymentActionType;
import com.safa.payment.common.noon.NoonPaymentStatus;
import com.safa.payment.dto.SlackErrorMessageDto;
import com.safa.payment.dto.common.HostedPaymentOutGoingDto;
import com.safa.payment.dto.noon.*;
import com.safa.payment.entity.Money;
import com.safa.payment.entity.PaymentTransaction;
import com.safa.payment.entity.PurchaseOrder;
import com.safa.payment.exception.InternalPaymentException;
import com.safa.payment.exception.PaymentTransactionException;
import com.safa.payment.repository.PaymentTransactionRepository;
import com.safa.payment.service.KafkaProducerService;
import com.safa.payment.service.PromoService;
import com.safa.payment.service.PurchaseOrderService;
import com.safa.payment.service.RestService;
import com.safa.payment.service.gateway.CommonPaymentTransactionService;
import com.safa.payment.util.PaymentUtil;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * This service responsible for creating payment url and handle the communication with payment
 * transaction repository
 *
 * @author Amoon
 */
@Slf4j
@Service
@Transactional
public class NoonPaymentTransactionService extends CommonPaymentTransactionService {
    public static final String SEPARATOR = ",";
    public static final String API_OPERATION = "INITIATE";
    public static final String WEB = "WEB";
    public static final String EN = "en";
    public static final String ORDER = "order";
    public static final String RESULT = "result";
    public static final String POST_URL = "postUrl";
    public static final String CHECKOUT_DATA = "checkoutData";
    public static final String PAY = "pay";
    public static final String ID = "id";
    public static final String ERROR_CODE = "errorCode";
    public static final String ALPHABETICAL_REGEX = "[\\d\\W_]";
    public static final String HMAC_SHA_512 = "HmacSHA512";
    public static final String MERCHANT_REF = "MerchantRef";
    public static final String PROCESSING_NOON_PAYMENT_WEBHOOK_REQUEST_FAILED = "Processing Noon payment webhook request failed, %s";

    private final RestTemplate restTemplate;


    @Value("${noon.hosted-payment.url:}")
    String hostedPaymentUrl;
    @Value("${noon.authKey:}")
    String authKey;
    @Value("${noon.environment.mode:}")
    String environmentMode;
    @Value("${noon.secretKey:}")
    String noonSecretKey;

    @Value("${noon.returnUrl:}")
    String returnUrl;

    @Value("${noon.styleProfile:}")
    String styleProfile;


    @Autowired
    @Lazy
    public NoonPaymentTransactionService(
            PaymentTransactionRepository paymentTransactionRepository,
            KafkaProducerService kafkaProducerService,
            PaymentUtil paymentUtil,
            PurchaseOrderService purchaseOrderService,
            RestService restService,
            ApplicationEventPublisher eventPublisher, PromoService promoService, RestTemplateBuilder restTemplateBuilder) {
        super(paymentTransactionRepository, kafkaProducerService, paymentUtil, purchaseOrderService, restService, promoService, eventPublisher);
        this.restTemplate = restTemplateBuilder.build();
    }

    /**
     * Create hosted payment url from the purchase order reference id and reference type
     *
     * @see <a *
     * href="https://docs.noonpayments.com/start/introduction/">...</a>
     */
    public String createHostedPaymentUrl(final PurchaseOrder purchaseOrder) {
        final List<PaymentTransaction> paymentTransactions = purchaseOrder.getPaymentTransactions();
        PaymentTransaction latestPaymentTransaction = validateOrderTransactionsStatus(paymentTransactions, purchaseOrder);
        // To support multiple request in case if the first payment failed.
        int requestNumber = 1;
        if (!CollectionUtils.isEmpty(paymentTransactions)) {
            requestNumber = paymentTransactions.size();
        }
        HostedPaymentRequestDto paymentRequestDto =
                buildHostedPaymentThirdPartyRequest(purchaseOrder, latestPaymentTransaction.getId(), requestNumber);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, String.format("Key_%s %s", environmentMode, authKey));
        headers.add(MERCHANT_REF, purchaseOrder.getReferenceId());
        NoonPaymentResponseDto responseDto = sendHostedPaymentRequest(paymentRequestDto, String.format("%s%s", hostedPaymentUrl, ORDER), headers, PaymentGateway.NOON);
        latestPaymentTransaction.setTransactionReference(String.valueOf(responseDto.getOrderId()));
        return responseDto.getPostUrl();
    }

    /**
     * Since Noon support one return url for all cases (failed, success, cancel).
     * After the payment process is completed,
     * This method will be called after Noon call the return url API (redirect api).
     * Based on the status of the payment we redirect the user to the correct status page.
     */
    public String getPaymentStatusPageUrl(long orderId) {
        // Get Payment details for the order
        NoonPaymentOrderDetailsResponseDto paymentDetails = sendGetOrderPaymentDetails(orderId);
        try {
            // If success payment return success payment html page url
            if (NoonPaymentStatus.isSuccessStatus(paymentDetails.getResult().getOrder().getStatus())) {
                return this.paymentUtil.getAuthorizedPageURL();
            } else if (NoonPaymentStatus.isCancelledStatus(paymentDetails.getResult().getOrder().getStatus())) {
                return this.paymentUtil.getCancelledPageURL();
            }
        } catch (Exception ex) {
            log.warn(String.format("Noon Payment: Failed extracting get order details, %s", orderId));
        }
        return this.paymentUtil.getDeclinedPageUrl();
    }

    /**
     * Get payment details(status, user details, card details) using Noon GET ORDER API (order/orderId).
     */
    public NoonPaymentOrderDetailsResponseDto sendGetOrderPaymentDetails(long orderId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.AUTHORIZATION, String.format("Key_%s %s", environmentMode, authKey));
            HttpEntity requestEntity = new HttpEntity(headers);
            ResponseEntity<NoonPaymentOrderDetailsResponseDto> response = this.restTemplate.exchange(String.format("%s%s/%s", hostedPaymentUrl, ORDER, orderId), HttpMethod.GET, requestEntity, NoonPaymentOrderDetailsResponseDto.class);
            return response.getBody();
        } catch (Exception ex) {
            throw new PaymentTransactionException(String.format("Failed getting noon order payment details for order id %d", orderId), ex);
        }
    }

    public NoonPaymentResponseDto sendHostedPaymentRequest(HostedPaymentRequestDto hostedPaymentRequestDto,
                                                           String hostedPaymentUrl,
                                                           HttpHeaders headers, PaymentGateway paymentGateway) {
        log.info(
                "Sending request to hosted payment gateway in order to get page URL, request details: {} {}",
                hostedPaymentRequestDto.toString(), paymentGateway);
        HttpEntity requestEntity = new HttpEntity(hostedPaymentRequestDto, headers);
        ResponseEntity responseEntity = this.restTemplate.exchange(hostedPaymentUrl, HttpMethod.POST, requestEntity, Object.class);
        return handleNoonResponse(responseEntity);
    }

    @Override
    public void cancelPaymentTransaction(PaymentTransaction paymentTransaction) {
        paymentTransaction.setTransactionStatus(PaymentTransactionPaymentStatus.CANCELLED.getShortCode());
        this.paymentTransactionRepository.saveAndFlush(paymentTransaction);
    }

    /**
     * Build Noon's request of created order
     *
     * @param purchaseOrder
     * @param requestNumber
     * @return
     */
    public HostedPaymentRequestDto buildHostedPaymentThirdPartyRequest(
            PurchaseOrder purchaseOrder, Long paymentTransactionId, int requestNumber) {
        // Create noon request
        HostedPaymentRequestDto requestDto = new HostedPaymentRequestDto();
        requestDto.setApiOperation(API_OPERATION);
        ConfigurationDto configurationDto = new ConfigurationDto();
        configurationDto.setLocale(purchaseOrder.getLanguage() != null ? purchaseOrder.getLanguage() : EN);
        configurationDto.setStyleProfile(this.styleProfile);
        if (!purchaseOrder.getReferenceType().equals(PaymentReferenceType.SUBSCRIPTION.getName())) {
            configurationDto.setReturnUrl(this.returnUrl);
        } else { // For subscription set return page to WLP subscription site
            configurationDto.setReturnUrl(this.paymentUtil.getSubscriptionAuthorizedPageURL());
        }
        // Show save card checkbox
        configurationDto.setTokenizeCc(true);
        // Define the type of action you need to apply on payment card, authorize , sale, reverse, refund
        configurationDto.setPaymentAction(String.format("%s,%s", NoonPaymentActionType.AUTHORIZE, NoonPaymentActionType.SALE));
        requestDto.setConfiguration(configurationDto);

        // Set order details
        OrderRequestDto orderRequestDto = new OrderRequestDto();
        //Transaction id
        orderRequestDto.setReference(paymentTransactionId + "-" + requestNumber);
        orderRequestDto.setCategory(PAY);
        orderRequestDto.setChannel(WEB);
        orderRequestDto.setName(NoonPaymentTransactionService.ORDER + "_" + purchaseOrder.getId());
        Money amount = purchaseOrder.getAmount();

        Money amountAfterDiscount = purchaseOrder.getAmountAfterDiscount();
        // If there is a discount, use it as amount
        if (amountAfterDiscount != null) {
            this.paymentUtil.validateAmount(amountAfterDiscount);
            orderRequestDto.setAmount(amountAfterDiscount.getValue().toString());
            orderRequestDto.setCurrency(amountAfterDiscount.getCurrency());
        } else {
            this.paymentUtil.validateAmount(amount);
            orderRequestDto.setAmount(amount.getValue().toString());
            orderRequestDto.setCurrency(amount.getCurrency());
        }
        orderRequestDto.setDescription(
                "Request for purchase order id:" + purchaseOrder.getId());
        requestDto.setOrder(orderRequestDto);

        ShippingDto shippingDto = new ShippingDto();
        AddressDto addressDto = new AddressDto();
        if (purchaseOrder.getFormattedAddress() != null) {
            String street = purchaseOrder.getFormattedAddress().replaceAll(":<", " ");
            // Noon accepts 60 characters length
            addressDto.setStreet(StringUtils.substring(street, 0, street.length() > 59 ? 59 : purchaseOrder.getFormattedAddress().length()));
        }
        if (purchaseOrder.getCity() != null) {
            String city = purchaseOrder.getCity().replaceAll(":<", " ");
            // Noon accepts 60 characters length
            addressDto.setCity(StringUtils.substring(city, 0, city.length() > 30 ? 30 : purchaseOrder.getCity().length()));
        }
        addressDto.setCountry(purchaseOrder.getCountry());

        ContactDto contactDto = new ContactDto();
        contactDto.setPhone(purchaseOrder.getPhoneNumber());
        contactDto.setEmail(purchaseOrder.getEmail());
        String fullName = purchaseOrder.getFullName();
        // Split full name into first and last name
        if (fullName != null) {
            //clear all numbers and special characters
            fullName = fullName.replaceAll(ALPHABETICAL_REGEX + "+", "");
            String[] name = fullName.split(" ");
            contactDto.setFirstName(name[0].substring(0, name[0].length() > 24 ? 24 : name[0].length()));
            contactDto.setLastName(name[name.length - 1].substring(0, name[name.length - 1].length() > 24 ? 24 : name[name.length - 1].length()));
        }
        shippingDto.setAddress(addressDto);
        shippingDto.setContact(contactDto);
        requestDto.setShipping(shippingDto);


        // TODO: Add subscription and payment renewal
        return requestDto;
    }

    public NoonPaymentResponseDto handleNoonResponse(ResponseEntity responseEntity) {
        NoonPaymentResponseDto noonPaymentResponseDto;
        try {
            if (responseEntity.getBody() == null) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "Invalid response from Noon {}");
            }
            LinkedHashMap results = (LinkedHashMap) ((LinkedHashMap) responseEntity.getBody()).get(RESULT);
            noonPaymentResponseDto = new NoonPaymentResponseDto();
            Object errorCode = ((LinkedHashMap) (results.get(ORDER))).get(ERROR_CODE);
            noonPaymentResponseDto.setErrorCode((Integer) errorCode);
            noonPaymentResponseDto.setPostUrl((String) ((LinkedHashMap) (results.get(CHECKOUT_DATA))).get(POST_URL));
            noonPaymentResponseDto.setOrderId((Long) ((LinkedHashMap) (results.get(ORDER))).get(ID));
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Invalid response from Noon " + e.getMessage());
        }
        return noonPaymentResponseDto;
    }

    public void processPaymentTransaction(NoonTransactionIncomingDto transactionIncomingDto, int maxRetries) {
        try {
            validateTransactionSignature(transactionIncomingDto);
            PaymentTransaction oldPaymentTransaction = this.paymentTransactionRepository.findByTransactionReference(transactionIncomingDto.getOrderId());

            if (oldPaymentTransaction == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("No payment transaction found with reference payment order id (%s). Noon payment incoming webhook request (%s)", transactionIncomingDto.getOrderId(), transactionIncomingDto));
            }
            // For noon payment, we receive cancel status when the user click on cancel button in noon payment screen
            // And this will cancel the payment link, so we ignore this status to avoid canceling the payment link.
            if (NoonPaymentStatus.isCancelledStatus(transactionIncomingDto.getOrderStatus())) {
                return;
            }

            PaymentTransaction newPaymentTransaction = new PaymentTransaction();
            // Copy old PaymentTransaction into a new PaymentTransaction
            BeanUtils.copyProperties(oldPaymentTransaction, newPaymentTransaction, ID);
            // Match Noon's status to PaymentTransactionPaymentStatus
            String paymentStatus = NoonPaymentStatus.getPaymentTransactionPaymentStatus(transactionIncomingDto.getOrderStatus()).getShortCode();
            newPaymentTransaction.setTransactionStatus(paymentStatus);
            PurchaseOrder purchaseOrder =
                    purchaseOrderService.findPurchaseOrderById(newPaymentTransaction.getPurchaseOrder().getId(), true);

            if (PaymentTransactionPaymentStatus.isSuccessStatus(paymentStatus)) {
                if (purchaseOrder.getAmountAfterDiscount() != null) {
                    newPaymentTransaction.setTransactionAmount(purchaseOrder.getAmountAfterDiscount().getValue());
                    newPaymentTransaction.setTransactionCurrency(purchaseOrder.getAmountAfterDiscount().getCurrency());
                } else {
                    newPaymentTransaction.setTransactionAmount(purchaseOrder.getAmount().getValue());
                    newPaymentTransaction.setTransactionCurrency(purchaseOrder.getAmount().getCurrency());
                }
            }
            // Update payment transaction and send kafka event
            updateOrSavePaymentTransaction(newPaymentTransaction, oldPaymentTransaction, purchaseOrder);
        } catch (Exception e) {
            if (maxRetries > 0) {
                processPaymentTransaction(transactionIncomingDto, maxRetries - 1);
            } else {
                log.error(e.getLocalizedMessage());
                eventPublisher.publishEvent(new SlackErrorMessageDto(String.format(PROCESSING_NOON_PAYMENT_WEBHOOK_REQUEST_FAILED, e.getLocalizedMessage())));
            }
        }
    }


    public void validateTransactionSignature(NoonTransactionIncomingDto transactionIncomingDto) throws Exception {
        // #1 Create signature message from incoming response
        String signature = transactionIncomingDto.getOrderId()
                + SEPARATOR
                + transactionIncomingDto.getOrderStatus()
                + SEPARATOR
                + transactionIncomingDto.getEventId()
                + SEPARATOR
                + transactionIncomingDto.getEventType()
                + SEPARATOR
                + transactionIncomingDto.getTimeStamp();
        if (transactionIncomingDto.getAttemptNumber() != null) {
            // provided only for orders initiated with the allowedRetry field, after the 1st failed attempt.
            signature = signature
                    + SEPARATOR
                    + transactionIncomingDto.getOriginalOrderId()
                    + SEPARATOR
                    + transactionIncomingDto.getMerchantOrderReference()
                    + SEPARATOR
                    + transactionIncomingDto.getAttemptNumber();
        }
        // #2 Generate signature using secret
        // Convert the secret to bytes
        byte[] secretBytes = this.noonSecretKey.getBytes(StandardCharsets.UTF_8);
        // #3 Create an HMAC-SHA512 key
        SecretKeySpec secretKey = new SecretKeySpec(secretBytes, HMAC_SHA_512);
        // #4  Initialize the HMAC-SHA512 algorithm
        Mac mac = Mac.getInstance(HMAC_SHA_512);
        mac.init(secretKey);
        // #5 Calculate the HMAC
        byte[] hmacBytes = mac.doFinal(signature.getBytes());
        // #6 Encode the HMAC in Base64
        String hashInBase64 = Base64.getEncoder().encodeToString(hmacBytes);

        // #7 Compare incoming signature with generated one
        if (!StringUtils.equals(transactionIncomingDto.getSignature(), hashInBase64)) {
            throw new InternalPaymentException("Invalid payment transaction signature:" + transactionIncomingDto);
        }
    }


    /**
     * Send request to noon payment gateway to get hosted payment page url;
     *
     * @see <a
     * href="https://docs.noonpayments.com/test/evaluating-api">...</a>
     */
    @Override
    public HostedPaymentOutGoingDto getPayNowUrlByReference(
            String referenceId, String referenceType) {
        log.info(
                "Hosted payment request, referenceId {}, referenceType {}", referenceId, referenceType);

        PurchaseOrder purchaseOrder = getPurchaseOrder(referenceId, referenceType, true);
        if (StringUtils.isNotEmpty(purchaseOrder.getUuid())) {
            return new HostedPaymentOutGoingDto(this.paymentUtil.getPayNowUrl(purchaseOrder.getUuid()));
        }
        final String hostedPaymentUrl = createHostedPaymentUrl(purchaseOrder);
        // Send kafka to change the status in backend to requested
        return new HostedPaymentOutGoingDto(hostedPaymentUrl);
    }


}
