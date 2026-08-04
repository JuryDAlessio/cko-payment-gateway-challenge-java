package com.checkout.payment.gateway.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.checkout.payment.gateway.client.BankClient;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentGatewayServiceTest {

  @Mock
  private PaymentsRepository paymentsRepository;

  @Mock
  private BankClient bankClient;

  @Mock
  private Validator validator;

  @InjectMocks
  private PaymentGatewayService paymentGatewayService;

  private PostPaymentRequest request;

  @BeforeEach
  void setUp() {
    request = new PostPaymentRequest();
    request.setCardNumber("123456789012345");
    request.setAmount(100);
    request.setCurrency("USD");
    request.setExpiryMonth(12);
    request.setExpiryYear(2030);
  }

  @Test
  void getPaymentById_WhenFound_ReturnsResponse() {
    UUID id = UUID.randomUUID();
    PostPaymentResponse response = new PostPaymentResponse();
    when(paymentsRepository.get(id)).thenReturn(Optional.of(response));

    PostPaymentResponse result = paymentGatewayService.getPaymentById(id);

    assertEquals(response, result);
  }

  @Test
  void getPaymentById_WhenNotFound_ThrowsException() {
    UUID id = UUID.randomUUID();
    when(paymentsRepository.get(id)).thenReturn(Optional.empty());

    assertThrows(EventProcessingException.class, () -> paymentGatewayService.getPaymentById(id));
  }

  @Test
  void processPayment_WhenValidationFails_SavesAsRejectedAndThrows() {
    ConstraintViolation<PostPaymentRequest> violation = mock(ConstraintViolation.class);
    when(violation.getMessage()).thenReturn("Invalid card");
    when(validator.validate(request)).thenReturn(Set.of(violation));

    assertThrows(IllegalArgumentException.class, () -> paymentGatewayService.processPayment(request));

    // Verifies it saves the REJECTED status before throwing[cite: 1]
    verify(paymentsRepository).add(eq(request), argThat(response -> response.getStatus() == PaymentStatus.REJECTED));
    verifyNoInteractions(bankClient);
  }

  @Test
  void processPayment_WhenDuplicateFound_ReturnsDuplicateWithoutCallingBank() {
    when(validator.validate(request)).thenReturn(Collections.emptySet());
    PostPaymentResponse duplicateResponse = new PostPaymentResponse();
    when(paymentsRepository.findRecentDuplicate(eq(request), anyInt())).thenReturn(Optional.of(duplicateResponse));

    PostPaymentResponse result = paymentGatewayService.processPayment(request);

    assertEquals(duplicateResponse, result);
    verifyNoInteractions(bankClient);
  }

  @Test
  void processPayment_WhenValidAndUnique_ProcessesThroughBankAndSaves() {
    when(validator.validate(request)).thenReturn(Collections.emptySet());
    when(paymentsRepository.findRecentDuplicate(any(), anyInt())).thenReturn(Optional.empty());
    when(bankClient.processPayment(request)).thenReturn(PaymentStatus.AUTHORIZED);

    PostPaymentResponse result = paymentGatewayService.processPayment(request);

    assertEquals(PaymentStatus.AUTHORIZED, result.getStatus());
    assertEquals("2345", result.getCardNumberLastFour());
    assertEquals(100, result.getAmount());
    verify(paymentsRepository).add(eq(request), eq(result));
  }
}