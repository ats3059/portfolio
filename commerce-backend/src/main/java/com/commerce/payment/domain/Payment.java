package com.commerce.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "payment",
        indexes = {
                @Index(name = "idx_payment_order_id", columnList = "orderId"),
                @Index(name = "idx_payment_no", columnList = "paymentNo", unique = true)
        }
)
public class Payment {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "payment_no", nullable = false, unique = true)
    private String paymentNo;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus;

    @Column(nullable = false)
    private long totalAmount;

    private LocalDateTime paidAt;

    public static Payment create(String paymentNo, UUID orderId, PaymentMethod paymentMethod, long totalAmount) {
        Payment payment = new Payment();
        payment.paymentNo = paymentNo;
        payment.orderId = orderId;
        payment.paymentMethod = paymentMethod;
        payment.paymentStatus = PaymentStatus.READY;
        payment.totalAmount = totalAmount;
        return payment;
    }

    public void confirm() {
        if (paymentStatus == PaymentStatus.DONE) {
            return;
        }
        if (paymentStatus != PaymentStatus.READY
                && paymentStatus != PaymentStatus.IN_PROGRESS
                && paymentStatus != PaymentStatus.WAITING_FOR_DEPOSIT) {
            throw new IllegalStateException("결제 확정은 READY/IN_PROGRESS/WAITING_FOR_DEPOSIT 상태에서만 가능합니다. 현재 상태: " + paymentStatus);
        }
        this.paymentStatus = PaymentStatus.DONE;
        this.paidAt = LocalDateTime.now();
    }

    public void waitForDeposit() {
        if (paymentStatus == PaymentStatus.WAITING_FOR_DEPOSIT) {
            return;
        }
        if (paymentStatus != PaymentStatus.READY && paymentStatus != PaymentStatus.IN_PROGRESS) {
            throw new IllegalStateException("입금 대기 전이는 READY/IN_PROGRESS 상태에서만 가능합니다. 현재 상태: " + paymentStatus);
        }
        this.paymentStatus = PaymentStatus.WAITING_FOR_DEPOSIT;
    }

    public void cancel() {
        if (paymentStatus == PaymentStatus.CANCELED) {
            return;
        }
        if (paymentStatus == PaymentStatus.DONE) {
            throw new IllegalStateException("결제 취소는 DONE 상태에서는 불가합니다.");
        }
        this.paymentStatus = PaymentStatus.CANCELED;
    }

    public void abort() {
        if (paymentStatus == PaymentStatus.ABORTED) {
            return;
        }
        this.paymentStatus = PaymentStatus.ABORTED;
    }

    public void expire() {
        if (paymentStatus == PaymentStatus.EXPIRED) {
            return;
        }
        this.paymentStatus = PaymentStatus.EXPIRED;
    }
}
