package com.safa.payment.service.gateway.Telr;

import com.safa.payment.common.PaymentReferenceType;
import com.safa.payment.common.PaymentTransactionPaymentStatus;
import com.safa.payment.dto.common.HostedPaymentOutGoingDto;
import com.safa.payment.dto.common.HostedPaymentResponseDto;
import com.safa.payment.dto.telr.*;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;

import static com.safa.payment.util.PaymentUtil.CREATE_METHOD;

/**
 * This service responsible for creating payment url for Telr payments and handle the communication with payment
 * transaction repository
 *
 * @author Qusai Safa
 */
@Service
@Transactional
@Slf4j
public class TelrPaymentTransactionService extends CommonPaymentTransactionService {

    public static final String SEPARATOR = ":";
    private final Logger logger = LoggerFactory.getLogger(TelrPaymentTransactionService.class);
    private final TelrRestService telrRestService;
    @Value("${telr.transaction.secret}")
    private String telrTransactionSecret;

    @Value("${payment.telr.test:true}")
    private boolean isTest;

    @Value("${telr.store}")
    private int store;

    @Value("${telr.authkey}")
    private String authKey;


    @Autowired
    @Lazy
    public TelrPaymentTransactionService(
            PaymentTransactionRepository paymentTransactionRepository,
            KafkaProducerService kafkaProducerService,
            PaymentUtil paymentUtil,
            PurchaseOrderService purchaseOrderService,
            TelrRestService telrRestService, RestService restService, PromoService promoService, ApplicationEventPublisher eventPublisher) {
        super(paymentTransactionRepository, kafkaProducerService, paymentUtil, purchaseOrderService, restService, promoService, eventPublisher);
        this.telrRestService = telrRestService;
    }

    /**
     * create hosted payment url from the purchase order reference id and reference type
     *
     * @see <a href="https://telr.com/support/knowledge-base/hosted-payment-page-integration-guide/">...</a>
     */
    public String createHostedPaymentUrl(final PurchaseOrder purchaseOrder) {
        final List<PaymentTransaction> paymentTransactions = purchaseOrder.getPaymentTransactions();
        validateOrderTransactionsStatus(paymentTransactions, purchaseOrder);
        // To support multiple request in case if the first payment failed.
        int requestNumber = 1;
        if (!CollectionUtils.isEmpty(paymentTransactions)) {
            requestNumber = paymentTransactions.size();
        }
        HostedPaymentRequestDto telrRequestDto =
                buildHostedPaymentThirdPartyRequest(purchaseOrder, requestNumber);
        HostedPaymentResponseDto hostedPaymentResponse =
                this.telrRestService.sendHostedPaymentRequest(telrRequestDto);
        validatePaymentIncomingResponse(hostedPaymentResponse);
        return hostedPaymentResponse.getOrder().getUrl();
    }
    

    /**
     * Validate Response for telr API
     */
    public void validatePaymentIncomingResponse(HostedPaymentResponseDto hostedPaymentResponse) {
        if (hostedPaymentResponse == null) {
            logger.error("Empty response from telr gateway");
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "No response from telr gatway");
        }
        ErrorDto error = hostedPaymentResponse.getError();
        if (error != null) {
            logger.error("Error response from telr gatway: {}", error);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, error.getMessage());
        }
    }

    /**
     * Process the payment transaction request that send from telr gateway after payment process
     */
    public void processPaymentTransaction(TelrPaymentTransactionIncomingDto transactionIncomingDto) {
        try {
            validateTransactionSignature(transactionIncomingDto);
            PaymentTransaction newPaymentTransaction = this.paymentUtil.mapTelrTransactionToPaymentTransaction(transactionIncomingDto);
            // Extract cart Id to get reference id, reference type and request number
            CartIdentifier telrCartIdentifer =
                    getTelrCartIdentifier(transactionIncomingDto.getTran_cartid());
            PurchaseOrder purchaseOrder =
                    purchaseOrderService.findByReference(
                            telrCartIdentifer.getReferenceId(), telrCartIdentifer.getReferenceType().getName(), true);
            List<PaymentTransaction> transactions = purchaseOrder.getPaymentTransactions();
            PaymentTransaction oldTransaction = transactions.get(0);
            updateOrSavePaymentTransaction(newPaymentTransaction, oldTransaction, purchaseOrder);
        } catch (Exception e) {
            throw new PaymentTransactionException("Failed handling incoming update request from Telr gateway", e);
        }
    }

    /**
     * @see <a href="https://telr.com/support/knowledge-base/transaction-advice-service/">...</a>
     */
    public void validateTransactionSignature(TelrPaymentTransactionIncomingDto transactionIncomingDto) {
        String signature =
                telrTransactionSecret
                        .concat(SEPARATOR)
                        .concat(transactionIncomingDto.getTran_store())
                        .concat(SEPARATOR)
                        .concat(transactionIncomingDto.getTran_type())
                        .concat(SEPARATOR)
                        .concat(transactionIncomingDto.getTran_class())
                        .concat(SEPARATOR)
                        .concat(transactionIncomingDto.getTran_test())
                        .concat(SEPARATOR)
                        .concat(transactionIncomingDto.getTran_ref())
                        .concat(SEPARATOR)
                        .concat(transactionIncomingDto.getTran_prevref())
                        .concat(SEPARATOR)
                        .concat(transactionIncomingDto.getTran_firstref())
                        .concat(SEPARATOR)
                        .concat(transactionIncomingDto.getTran_order())
                        .concat(SEPARATOR)
                        .concat(transactionIncomingDto.getTran_currency())
                        .concat(SEPARATOR)
                        .concat(transactionIncomingDto.getTran_amount())
                        .concat(SEPARATOR)
                        .concat(transactionIncomingDto.getTran_cartid())
                        .concat(SEPARATOR)
                        .concat(transactionIncomingDto.getTran_desc())
                        .concat(SEPARATOR)
                        .concat(transactionIncomingDto.getTran_status())
                        .concat(SEPARATOR)
                        .concat(transactionIncomingDto.getTran_authcode())
                        .concat(SEPARATOR)
                        .concat(transactionIncomingDto.getTran_authmessage());

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new InternalPaymentException(e);
        }
        byte[] messageDigest = md.digest(signature.getBytes());
        BigInteger no = new BigInteger(1, messageDigest);

        // Convert message digest into hex value
        StringBuilder hashText = new StringBuilder(no.toString(16));

        // Add preceding 0s to make it 40 bit
        while (hashText.length() < 40) {
            hashText.insert(0, "0");
        }
        if (!hashText.toString().equals(transactionIncomingDto.getTran_check())) {
            throw new InternalPaymentException("Invalid payment transaction signature:" + transactionIncomingDto);
        }
    }

    /**
     * Send request to telr payment gateway to get hosted payment page url;
     *
     * @see <a
     * href="https://telr.com/support/knowledge-base/hosted-payment-page-integration-guide/">...</a>
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

    @Override
    public void cancelPaymentTransaction(PaymentTransaction paymentTransaction) {
        paymentTransaction.setTransactionStatus(PaymentTransactionPaymentStatus.CANCELLED.getShortCode());
        this.paymentTransactionRepository.saveAndFlush(paymentTransaction);
    }

    /**
     * Build telr request
     *
     * @param purchaseOrder Order details saved on this microservices
     */
    public HostedPaymentRequestDto buildHostedPaymentThirdPartyRequest(
            PurchaseOrder purchaseOrder, int requestNumber) {
        // Create telr request
        HostedPaymentRequestDto telrRequestDto = new HostedPaymentRequestDto();
        telrRequestDto.setMethod(CREATE_METHOD);
        telrRequestDto.setAuthkey(authKey);
        telrRequestDto.setStore(store);

        if (!purchaseOrder.getReferenceType().equals(PaymentReferenceType.SUBSCRIPTION.getName())) {
            telrRequestDto.setReturnPage(
                    new ReturnPageDto(this.paymentUtil.getAuthorizedPageURL(), this.paymentUtil.getDeclinedPageUrl(), this.paymentUtil.getCancelledPageURL()));
        } else { // For subscription set return page to WLP subscription site
            telrRequestDto.setReturnPage(
                    new ReturnPageDto(this.paymentUtil.getSubscriptionAuthorizedPageURL(), this.paymentUtil.getSubscriptionDeclinedPageUrl(), this.paymentUtil.getSubscriptionCancelledPageURL()));
        }

        // Set order details
        OrderRequestDto orderRequestDto = new OrderRequestDto();
        CartIdentifier telrCartIdentifer =
                new CartIdentifier(
                        PaymentReferenceType.valueOf(purchaseOrder.getReferenceType().toUpperCase()),
                        purchaseOrder.getReferenceId(),
                        requestNumber,
                        this.paymentUtil.getSourceCountryShortCode(), Instant.now().getEpochSecond());
        orderRequestDto.setCartid(telrCartIdentifer.createCartId());
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

        // For subscription and payment renewal
        if (purchaseOrder.getBillingInterval() != null && purchaseOrder.getBillingTerm() != null) {
            RepeatDto repeatDto = new RepeatDto();
            // How long is this agreement
            repeatDto.setTerm(purchaseOrder.getBillingTerm());
            repeatDto.setPeriod("M"); // Monthly
            repeatDto.setInterval(purchaseOrder.getBillingInterval()); // How many payment to take each month
            repeatDto.setStart("next"); // This means the next payment will be after 30 days
            repeatDto.setAmount(amount.getValue().toString());
            repeatDto.setCurrency(amount.getCurrency());
            telrRequestDto.setRepeat(repeatDto);
        }

        orderRequestDto.setTest(isTest ? "1" : "0");
        telrRequestDto.setOrder(orderRequestDto);
        orderRequestDto.setDescription(
                "Payment request for purchase order id:" + purchaseOrder.getId());
        // Set customer details
        CustomerDto customerDto = new CustomerDto();
        customerDto.setRef(purchaseOrder.getCustomerId().toString());
        AddressDto addressDto = new AddressDto();
        addressDto.setLine1(purchaseOrder.getFormattedAddress());
        addressDto.setCity(purchaseOrder.getCity());
        addressDto.setCountry(purchaseOrder.getCountry());
        customerDto.setAddress(addressDto);
        customerDto.setPhone(purchaseOrder.getPhoneNumber());
        customerDto.setEmail(purchaseOrder.getEmail());
        telrRequestDto.setCustomer(customerDto);
        return telrRequestDto;
    }

    public CartIdentifier getTelrCartIdentifier(String cartId) {
        CartIdentifier telrCartIdentifer = new CartIdentifier();
        String[] claims = cartId.split("_");
        // Old flow, only order reference type was supported and one country (AE)
        if (claims.length == 2) {
            telrCartIdentifer.setReferenceId(claims[0]);
            telrCartIdentifer.setReferenceType(PaymentReferenceType.ORDER);
            telrCartIdentifer.setRequestNumber(Integer.parseInt(claims[1]));
            return telrCartIdentifer;
        }
        // New flow different reference type with different countries (AE, EG, ...)
        telrCartIdentifer.setReferenceType(PaymentReferenceType.getByShortCode(claims[0]));
        telrCartIdentifer.setReferenceId(claims[1]);
        telrCartIdentifer.setRequestNumber(Integer.parseInt(claims[2]));
        telrCartIdentifer.setSourceCountryShortCode(claims[3]);
        return telrCartIdentifer;
    }


}
