package com.ionipay.backend;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Entity     // Tells JPA that this class maps to a specific table within the schema              
@Data
@Table(name = "payment_objects")
public class PaymentObject {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    private Long amount;
    private String currency;
    private String status;
    private String payerId;
    private String payeeId;
}