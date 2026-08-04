package com.checkout.payment.gateway.repository;

import com.checkout.payment.gateway.client.BankClient;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;




@Repository
public class PaymentsRepository {
  private static final Logger LOG = LoggerFactory.getLogger(PaymentsRepository.class);
  private final ConcurrentHashMap<UUID, PostPaymentResponse> payments = new ConcurrentHashMap<>();

  // Tracks requests chronologically for the duplicate window
  private record HistoryEntry(PostPaymentRequest request, PostPaymentResponse response, Instant timestamp) {}
  private final List<HistoryEntry> history = new ArrayList<>();

  private final ReentrantLock lock = new ReentrantLock();

  public void add(PostPaymentRequest request, PostPaymentResponse response) {
    lock.lock();
    try {
      payments.put(response.getId(), response);
      history.add(new HistoryEntry(request, response, Instant.now()));
      LOG.info("Saved payment, with response {} ", response);
    } finally {
      lock.unlock();
    }
  }

  public Optional<PostPaymentResponse> get(UUID id) {
    return Optional.ofNullable(payments.get(id));
  }

  public Optional<PostPaymentResponse> findRecentDuplicate(PostPaymentRequest request, int windowSeconds) {
    lock.lock();
    try {
      Instant cutoff = Instant.now().minusSeconds(windowSeconds);

      for (int i = history.size() - 1; i >= 0; i--) {
        HistoryEntry entry = history.get(i);

        if (entry.timestamp().isBefore(cutoff)) break;

        boolean isDuplicate = entry.request().getAmount() == request.getAmount() &&
            Objects.equals(entry.request().getCvv(), request.getCvv()) &&
            entry.request().getExpiryMonth() == request.getExpiryMonth() &&
            entry.request().getExpiryYear() == request.getExpiryYear();

        if (isDuplicate) {
          LOG.info("Found duplicate payment: {} ", request.getCardNumber().substring(request.getCardNumber().length() - 4));
          return Optional.of(entry.response());
        }
      }
      return Optional.empty();
    } finally {
      lock.unlock();
    }
  }
}