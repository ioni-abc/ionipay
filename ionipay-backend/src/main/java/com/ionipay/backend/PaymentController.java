package com.ionipay.backend;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @GetMapping
    public List<PaymentObject> getAllPayments() {
        return service.getAllPayments();
    }

    @GetMapping("/{id}")
    public Optional<PaymentObject> getPayment(@PathVariable UUID id) {
        return service.getPayment(id);
    }

    @PostMapping
    public ResponseEntity<?> createPayment(@Valid @RequestBody PaymentObject newPayment) {
        try {
            PaymentObject created = service.createPayment(newPayment);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approvePayment(@PathVariable UUID id, @RequestHeader("X-User-ID") String checkerId) {
        try {
            PaymentObject approvedPayment = service.approvePayment(id, checkerId);
            return ResponseEntity.ok(approvedPayment);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}