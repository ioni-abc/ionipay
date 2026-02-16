package com.ionipay.backend;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository repository;

    public PaymentService(PaymentRepository repository) {
        this.repository = repository;
    }

    public List<PaymentObject> getAllPayments() {
        return repository.findAll();
    }

    public Optional<PaymentObject> getPayment(UUID id) {
        return repository.findById(id);
    }

    public PaymentObject createPayment(PaymentObject payment) {
        if (payment.getAmount() < 0) {
            throw new IllegalArgumentException("Amount can't be negative.");
        }
        return repository.save(payment);
    }

    public PaymentObject approvePayment(UUID paymentId, String ckeckerUserId) {
        PaymentObject payment = repository.findById(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("Payment not found."));
        
        if (ckeckerUserId.equals(payment.getCreatedByUserId())) {
            throw new SecurityException("You cannot approve a payment you created.");
        }

        payment.setStatus("APPROVED");
        payment.setApprovedByUserId(ckeckerUserId);

        return repository.save(payment);
    }

}