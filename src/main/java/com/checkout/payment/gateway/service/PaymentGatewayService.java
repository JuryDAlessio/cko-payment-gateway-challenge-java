package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.client.BankClient;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentGatewayService {
  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);
  private final PaymentsRepository paymentsRepository;
  private final BankClient bankClient;
  private final Validator validator;

  public PaymentGatewayService(PaymentsRepository paymentsRepository, BankClient bankClient, Validator validator) {
    this.paymentsRepository = paymentsRepository;
    this.bankClient = bankClient;
    this.validator = validator;
  }

  public PostPaymentResponse getPaymentById(UUID id) {
    LOG.debug("Requesting access to payment with ID {}", id);
    return paymentsRepository.get(id)
        .orElseThrow(() -> new EventProcessingException("Invalid ID"));
  }

  public PostPaymentResponse processPayment(PostPaymentRequest request) {
    Set<ConstraintViolation<PostPaymentRequest>> violations = validator.validate(request);
    if (!violations.isEmpty()) {
      savePayment(request, PaymentStatus.REJECTED);

      String errorMessage = violations.stream()
          .map(v -> v.getPropertyPath() + ": " + v.getMessage())
          .findFirst()
          .orElse("Invalid request payload");

      LOG.warn("Validation failed: {}", errorMessage);
      throw new IllegalArgumentException(errorMessage);
    }

    Optional<PostPaymentResponse> duplicate = paymentsRepository.findRecentDuplicate(request, 5);
    if (duplicate.isPresent()) {
      return duplicate.get();
    }

    PaymentStatus status = bankClient.processPayment(request);


    return savePayment(request, status);
  }

  private PostPaymentResponse savePayment(PostPaymentRequest request, PaymentStatus status) {
    PostPaymentResponse response = new PostPaymentResponse();
    response.setId(UUID.randomUUID());
    response.setStatus(status);
    response.setAmount(request.getAmount());
    response.setCurrency(request.getCurrency());
    response.setExpiryMonth(request.getExpiryMonth());
    response.setExpiryYear(request.getExpiryYear());
    if(request.getCardNumber().length() > 3) {
      response.setCardNumberLastFour(request.getCardNumber().substring(request.getCardNumber().length() - 4));
    } else {

      response.setCardNumberLastFour(request.getCardNumber());
    }


    paymentsRepository.add(request, response);
    return response;
  }
}