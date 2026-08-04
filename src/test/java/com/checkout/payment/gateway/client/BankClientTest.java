package com.checkout.payment.gateway.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class BankClientTest {

  @Mock
  private RestTemplate restTemplate;

  private BankClient bankClient;
  private PostPaymentRequest request;
  private final String simulatorUrl = "http://localhost:8080/payments";

  @BeforeEach
  void setUp() {
    bankClient = new BankClient(restTemplate, simulatorUrl);
    request = new PostPaymentRequest();
  }

  @Test
  void processPayment_When2xxSuccessful_ReturnsAuthorized() {
    ResponseEntity<String> successResponse = new ResponseEntity<>("{}", HttpStatus.OK);
    when(restTemplate.postForEntity(eq(simulatorUrl), eq(request), eq(String.class)))
        .thenReturn(successResponse);

    PaymentStatus status = bankClient.processPayment(request);

    assertEquals(PaymentStatus.AUTHORIZED, status);
  }

  @Test
  void processPayment_WhenNot2xxSuccessful_ReturnsDeclined() {
    ResponseEntity<String> declinedResponse = new ResponseEntity<>("{}", HttpStatus.BAD_REQUEST);
    when(restTemplate.postForEntity(eq(simulatorUrl), eq(request), eq(String.class)))
        .thenReturn(declinedResponse);

    PaymentStatus status = bankClient.processPayment(request);

    assertEquals(PaymentStatus.DECLINED, status);
  }

  @Test
  void processPayment_WhenNetworkError_ThrowsEventProcessingException() {
    when(restTemplate.postForEntity(eq(simulatorUrl), eq(request), eq(String.class)))
        .thenThrow(new RestClientException("Connection refused"));

    assertThrows(EventProcessingException.class, () -> bankClient.processPayment(request));
  }
}