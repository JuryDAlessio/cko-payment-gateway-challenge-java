package com.checkout.payment.gateway.client;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class BankClient {
  private static final Logger LOG = LoggerFactory.getLogger(BankClient.class);
  private final RestTemplate restTemplate;
  private final String simulatorUrl;

  public BankClient(
      RestTemplate restTemplate,
      @Value("${bank.simulator.url:http://localhost:8080/payments}") String simulatorUrl
  ) {
    this.restTemplate = restTemplate;
    this.simulatorUrl = simulatorUrl;
  }

  public PaymentStatus processPayment(PostPaymentRequest request) {
    try {
      LOG.info("Sending payment request to: {}", simulatorUrl);
      ResponseEntity<String> response = restTemplate.postForEntity(simulatorUrl, request, String.class);

      if (response.getStatusCode().is2xxSuccessful()) {
        return PaymentStatus.AUTHORIZED;
      }
      return PaymentStatus.DECLINED;
    } catch (Exception e) {
      LOG.error("Failed downstream call to {}: {}", simulatorUrl, e.getMessage(), e);
      throw new EventProcessingException("Downstream bank simulator unavailable");
    }
  }
}