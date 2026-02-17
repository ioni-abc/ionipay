package com.ionipay.backend;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Data;
import java.util.UUID;

@Entity
@Data
@Table(name = "payment_objects")
public class PaymentObject {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotNull(message = "Amount is required")
    @Min(value = 0, message = "Amount cannot be negative")
    @Column(nullable = false)
    private Long amount;

    @NotBlank(message = "Currency is required")
    @Column(nullable = false)
    private String currency;

    @NotBlank(message = "Status is required")
    @Column(nullable = false)
    private String status;

    @NotBlank(message = "Creator ID is required")
    @Column(nullable = false)
    private String createdByUserId;

    private String approvedByUserId;
    
    private UUID vendorId;

    @PrePersist
    protected void onCreate() {
        this.status = "PENDING";
    }
}