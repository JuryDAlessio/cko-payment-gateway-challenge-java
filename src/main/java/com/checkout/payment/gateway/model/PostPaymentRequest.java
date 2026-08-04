package com.checkout.payment.gateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Value;
import java.io.Serializable;

public class PostPaymentRequest implements Serializable {

  @NotBlank(message = "Card number is required")
  @Pattern(regexp = "\\d{14,19}", message = "Card number must be between 14 and 19 digits")
  @JsonProperty("card_number")
  private String cardNumber;

  @Min(value = 1, message = "Expiry month must be between 1 and 12")
  @Max(value = 12, message = "Expiry month must be between 1 and 12")
  @JsonProperty("expiry_month")
  private int expiryMonth;

  @Min(value = 2026, message = "Expiry year must be in the future")
  @JsonProperty("expiry_year")
  private int expiryYear;

  @NotBlank(message = "Currency is required")
  @Schema(example = "USD", allowableValues = {"USD", "GBP", "EUR"})
  private String currency;

  @Min(value = 1, message = "Amount must be greater than zero")
  private int amount;

  @Min(value = 100, message = "CVV must be 3 or 4 digits")
  @Max(value = 9999, message = "CVV must be 3 or 4 digits")
  private int cvv;

  @Schema(hidden = true)
  @JsonProperty(value = "expiry_date", access = JsonProperty.Access.READ_ONLY)
  public String getExpiryDate() {
    return String.format("%02d/%d", expiryMonth, expiryYear);
  }

  public String getCardNumber() {
    return cardNumber;
  }

  public void setCardNumber(String cardNumber) {
    this.cardNumber = cardNumber;
  }

  public int getExpiryMonth() {
    return expiryMonth;
  }

  public void setExpiryMonth(int expiryMonth) {
    this.expiryMonth = expiryMonth;
  }

  public int getExpiryYear() {
    return expiryYear;
  }

  public void setExpiryYear(int expiryYear) {
    this.expiryYear = expiryYear;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public int getAmount() {
    return amount;
  }

  public void setAmount(int amount) {
    this.amount = amount;
  }

  public int getCvv() {
    return cvv;
  }

  public void setCvv(int cvv) {
    this.cvv = cvv;
  }

}