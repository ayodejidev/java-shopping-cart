package com.adyen.checkout.api;

import com.adyen.Client;
import com.adyen.enums.Environment;
import com.adyen.model.Amount;
import com.adyen.model.checkout.*;
import com.adyen.service.Checkout;
import com.adyen.service.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@RestController
@RequestMapping("/api")
public class CheckoutResource {
    private final Logger log = LoggerFactory.getLogger(CheckoutResource.class);

    @Value("${ADYEN_MERCHANT_ACCOUNT}")
    private String merchantAccount;
    private final Checkout checkout;


    public CheckoutResource(@Value("${ADYEN_API_KEY}") String apiKey) {
        var client = new Client(apiKey, Environment.TEST);
        this.checkout = new Checkout(client);
    }

    /**
     * {@code POST /getPayment}: get valid payment methods.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (ok)} and with body the paymentMethods response.
     * @throws IOException  from Adyen Api
     * @throws ApiException from Ayden API
     */
    @PostMapping("/getPaymentMethods")
    public ResponseEntity<PaymentMethodsResponse> paymentMethods() throws IOException, ApiException {

        var paymentMethodsRequest = new PaymentMethodsRequest();
        paymentMethodsRequest.setMerchantAccount(merchantAccount);
        paymentMethodsRequest.setChannel(PaymentMethodsRequest.ChannelEnum.WEB);

        log.info("REST request to create Adyen Payment Session {}", paymentMethodsRequest);

        var response = checkout.paymentMethods(paymentMethodsRequest);
        return ResponseEntity.ok()
                .body(response);
    }

    private  Map<String, String> paymentDataStore = new HashMap<>();

    /**
     * {@code POST /initiatePayment}: get valid payment methods.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (ok)} and with body the paymentMethods response.
     * @throws IOException  from Adyen Api
     * @throws ApiException from Ayden API
     */
    @PostMapping("/initiatePayment")
    public ResponseEntity<PaymentsResponse> payments(@RequestBody PaymentsRequest body, HttpServletRequest request) throws IOException, ApiException {

        var paymentRequest = new PaymentsRequest();
        paymentRequest.setMerchantAccount(merchantAccount);
        paymentRequest.setChannel(PaymentsRequest.ChannelEnum.WEB);

        var amount = new Amount().currency("EUR").value(1000L);

        paymentRequest.setAmount(amount);
        var orderRef = UUID.randomUUID().toString();
        paymentRequest.setReference(orderRef);

        paymentRequest.setReturnUrl("http://localhost:8080/api/handleShopperRedirect?orderRef=" + orderRef);

        paymentRequest.setBrowserInfo(body.getBrowserInfo());
        paymentRequest.setPaymentMethod(body.getPaymentMethod());

        //required for 3ds2
        paymentRequest.setAdditionalData(Collections.singletonMap("allow3DS2", "true"));
        //paymentRequest.setOrigin("https://localhost:8080");
        paymentRequest.setShopperIP(request.getRemoteAddr());


        log.info("REST request to create Adyen Payment Session {}", paymentRequest);

        var response = checkout.payments(paymentRequest);

        if (response.getAction() != null && !response.getAction().getPaymentData().isEmpty()) {
            paymentDataStore.put(orderRef, response.getAction().getPaymentData());
        }

        return ResponseEntity.ok()
                .body(response);
    }

    /**
     * {@code GET /submitAdditionalDetails} : Handle during payment.
     *
     * @return the {@link RedirectView} with status {@code 302}
     * @throws IOException from Adyen API.
     * @throws ApiException from Adyen API.
     */
    @PostMapping("/submitAdditionalDetails")
    public ResponseEntity<PaymentsResponse> payments(@RequestBody PaymentsDetailsRequest detailsRequest) throws IOException, ApiException{
        log.info("REST request to make Adyen payment details {}", detailsRequest);
        var response = checkout.paymentsDetails(detailsRequest);
        return ResponseEntity.ok()
                .body(response);
    }


    /**
     * {@code GET /handleShopperRedirect} : Handle during payment.
     *
     * @return the {@link RedirectView} with status {@code 302}
     * @throws IOException  from Adyen API.
     * @throws ApiException from Adyen API.
     */


    @GetMapping("/handShopperRedirect")
    public RedirectView redirect(@RequestParam(required = false) String redirectResult, @RequestParam String orderRef) throws IOException, ApiException {

        var detailsRequest = new PaymentsDetailsRequest();

        if (redirectResult != null && !redirectResult.isEmpty()) {
            detailsRequest.setDetails(Collections.singletonMap("redirectResult", redirectResult));
        }

        detailsRequest.setPaymentData(paymentDataStore.get(orderRef));

        return getRedirectView(detailsRequest);
    }

    /**
     * {@code POST /handleShopperRedirect} : Handle redirect during payment.
     *
     * @return the {@link RedirectView} with status {@code 302}
     * @throws IOException from Adyen API.
     * @throws ApiException from Adyen API.
     */
    @PostMapping(
            path = "/handleShopperRedirect",
            consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    private RedirectView redirect(@RequestParam("MD") String md, @RequestParam("PaRes") String paRes, @RequestParam String orderRef)  throws IOException, ApiException {

        var details = new HashMap<String, String>();
        details.put("MD", md);
        details.put("PaRes", paRes);

        var detailsRequest = new PaymentsDetailsRequest();
        detailsRequest.setDetails(details);

        detailsRequest.setPaymentData(paymentDataStore.get(orderRef));

        return getRedirectView(detailsRequest);
    }


    private RedirectView getRedirectView (final PaymentsDetailsRequest detailsRequest) throws IOException, ApiException {
        log.info("REST request to create Adyen Payment Session {}", detailsRequest);

        var response = checkout.paymentsDetails(detailsRequest);
        var redirectURL = "/result";

        switch (response.getResultCode()){
            case AUTHORISED:
                redirectURL += "success";
                break;
            case PENDING:
            case RECEIVED:
                redirectURL += "pending";
                break;
            case REFUSED:
                redirectURL += "failed";
                break;
            default:
                redirectURL += "error";
                break;
        }
        return new RedirectView(redirectURL);
    }
}





