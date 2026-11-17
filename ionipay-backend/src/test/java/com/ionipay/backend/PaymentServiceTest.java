package com.ionipay.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for the business rules in PaymentService.
 * The repository is a Mockito mock, so no database or Spring context is needed.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository repository;

    @InjectMocks
    private PaymentService service;

    private static PaymentObject pendingPayment(UUID id, String createdBy) {
        PaymentObject payment = new PaymentObject();
        payment.setId(id);
        payment.setAmount(2500L);
        payment.setCurrency("EUR");
        payment.setStatus("PENDING");
        payment.setCreatedByUserId(createdBy);
        return payment;
    }

    @Test
    @DisplayName("getAllPayments returns every payment from the repository")
    void getAllPaymentsReturnsRepositoryContents() {
        List<PaymentObject> payments = List.of(
            pendingPayment(UUID.randomUUID(), "maker_01"),
            pendingPayment(UUID.randomUUID(), "maker_02"));
        when(repository.findAll()).thenReturn(payments);

        assertThat(service.getAllPayments()).isEqualTo(payments);
    }

    @Test
    @DisplayName("getPayment returns the payment when it exists")
    void getPaymentFound() {
        UUID id = UUID.randomUUID();
        PaymentObject payment = pendingPayment(id, "maker_01");
        when(repository.findById(id)).thenReturn(Optional.of(payment));

        assertThat(service.getPayment(id)).contains(payment);
    }

    @Test
    @DisplayName("getPayment returns an empty Optional when the payment does not exist")
    void getPaymentNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThat(service.getPayment(id)).isEmpty();
    }

    @Test
    @DisplayName("createPayment saves the payment and returns the saved entity")
    void createPaymentSaves() {
        PaymentObject payment = pendingPayment(null, "maker_01");
        PaymentObject saved = pendingPayment(UUID.randomUUID(), "maker_01");
        when(repository.save(payment)).thenReturn(saved);

        assertThat(service.createPayment(payment)).isEqualTo(saved);
        verify(repository).save(payment);
    }

    @Test
    @DisplayName("a second user can approve a payment: status becomes APPROVED and the approver is recorded")
    void approveByAnotherUser() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(pendingPayment(id, "maker_01")));
        when(repository.save(any(PaymentObject.class))).thenAnswer(call -> call.getArgument(0));

        PaymentObject approved = service.approvePayment(id, "checker_99");

        assertThat(approved.getStatus()).isEqualTo("APPROVED");
        assertThat(approved.getApprovedByUserId()).isEqualTo("checker_99");
        assertThat(approved.getCreatedByUserId()).isEqualTo("maker_01");
    }

    @Test
    @DisplayName("four-eyes rule: the creator cannot approve their own payment, and nothing is saved")
    void creatorCannotApprove() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(pendingPayment(id, "maker_01")));

        assertThatThrownBy(() -> service.approvePayment(id, "maker_01"))
            .isInstanceOf(SecurityException.class)
            .hasMessage("You cannot approve a payment you created.");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("approving a payment that does not exist fails, and nothing is saved")
    void approveMissingPayment() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approvePayment(id, "checker_99"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Payment not found.");
        verify(repository, never()).save(any());
    }

    // ---------- intended behaviour, not implemented yet ----------

    @Test
    @Disabled("Not implemented yet: an already APPROVED payment can currently be approved again (and the approver overwritten)")
    @DisplayName("a payment that is already approved cannot be approved again")
    void cannotApproveTwice() {
        UUID id = UUID.randomUUID();
        PaymentObject alreadyApproved = pendingPayment(id, "maker_01");
        alreadyApproved.setStatus("APPROVED");
        alreadyApproved.setApprovedByUserId("checker_99");
        when(repository.findById(id)).thenReturn(Optional.of(alreadyApproved));

        assertThatThrownBy(() -> service.approvePayment(id, "checker_42"))
            .isInstanceOf(IllegalStateException.class);
        verify(repository, never()).save(any());
    }
}
