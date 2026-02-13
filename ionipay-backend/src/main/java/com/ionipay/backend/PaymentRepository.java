package com.ionipay.backend;

import org.springframework.data.jpa.repository.JpaRepository;       // we extend this to inherit many methods
import org.springframework.stereotype.Repository;                   // responsible for db communication
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentObject, UUID> {}