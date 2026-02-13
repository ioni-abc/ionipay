package com.ionipay.backend;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController                    // turns the class into a webserver. tells spring to listen for http requests
@RequestMapping("/payments")       // sets base url ie everything under localhost:8080/payments
public class PaymentController {

    private final PaymentRepository repository;

    public PaymentController(PaymentRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<PaymentObject> getAllPayments() {
        return repository.findAll();
    }

    @PostMapping
    public PaymentObject createPayment(@RequestBody PaymentObject newPayment) {
        return repository.save(newPayment);
    }
}