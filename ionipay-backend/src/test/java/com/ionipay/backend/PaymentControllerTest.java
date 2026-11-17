package com.ionipay.backend;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * HTTP-level tests for each endpoint of PaymentController.
 * The service is mocked, so these tests check only the web layer: routing, status codes,
 * request validation, headers and JSON. No Spring context or database is started.
 */
class PaymentControllerTest {

    private static final String VALID_PAYMENT = """
        {"amount": 2500, "currency": "EUR", "status": "PENDING", "createdByUserId": "maker_01"}
        """;

    private PaymentService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(PaymentService.class);
        mvc = MockMvcBuilders.standaloneSetup(new PaymentController(service)).build();
    }

    private static PaymentObject payment(UUID id, String status, String createdBy) {
        PaymentObject payment = new PaymentObject();
        payment.setId(id);
        payment.setAmount(2500L);
        payment.setCurrency("EUR");
        payment.setStatus(status);
        payment.setCreatedByUserId(createdBy);
        return payment;
    }

    // ---------- GET /payments ----------

    @Test
    @DisplayName("GET /payments returns all payments as a JSON array")
    void listPayments() throws Exception {
        when(service.getAllPayments()).thenReturn(List.of(
            payment(UUID.randomUUID(), "PENDING", "maker_01"),
            payment(UUID.randomUUID(), "APPROVED", "maker_02")));

        mvc.perform(get("/payments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].createdByUserId").value("maker_01"))
            .andExpect(jsonPath("$[1].status").value("APPROVED"));
    }

    @Test
    @DisplayName("GET /payments returns an empty array when there are no payments")
    void listPaymentsEmpty() throws Exception {
        when(service.getAllPayments()).thenReturn(List.of());

        mvc.perform(get("/payments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    // ---------- GET /payments/{id} ----------

    @Test
    @DisplayName("GET /payments/{id} returns the payment")
    void getPayment() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.getPayment(id)).thenReturn(Optional.of(payment(id, "PENDING", "maker_01")));

        mvc.perform(get("/payments/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.amount").value(2500))
            .andExpect(jsonPath("$.currency").value("EUR"));
    }

    @Test
    @DisplayName("GET /payments/{id} with an id that is not a UUID returns 400")
    void getPaymentInvalidId() throws Exception {
        mvc.perform(get("/payments/{id}", "not-a-uuid"))
            .andExpect(status().isBadRequest());
        verify(service, never()).getPayment(any());
    }

    // ---------- POST /payments ----------

    @Test
    @DisplayName("POST /payments with a valid body creates the payment and returns it")
    void createPayment() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.createPayment(any(PaymentObject.class))).thenReturn(payment(id, "PENDING", "maker_01"));

        mvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON).content(VALID_PAYMENT))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /payments with a negative amount is rejected with 400 before reaching the service")
    void createPaymentNegativeAmount() throws Exception {
        String body = """
            {"amount": -1, "currency": "EUR", "status": "PENDING", "createdByUserId": "maker_01"}
            """;

        mvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
        verify(service, never()).createPayment(any());
    }

    @Test
    @DisplayName("POST /payments without required fields is rejected with 400 before reaching the service")
    void createPaymentMissingFields() throws Exception {
        String body = """
            {"amount": 2500}
            """;

        mvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
        verify(service, never()).createPayment(any());
    }

    @Test
    @DisplayName("POST /payments returns 400 with the message when the service rejects the payment")
    void createPaymentRejectedByService() throws Exception {
        when(service.createPayment(any(PaymentObject.class)))
            .thenThrow(new IllegalArgumentException("Vendor not found."));

        mvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON).content(VALID_PAYMENT))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Vendor not found."));
    }

    // ---------- PUT /payments/{id}/approve ----------

    @Test
    @DisplayName("PUT /payments/{id}/approve passes the X-User-ID header to the service and returns the approved payment")
    void approvePayment() throws Exception {
        UUID id = UUID.randomUUID();
        PaymentObject approved = payment(id, "APPROVED", "maker_01");
        approved.setApprovedByUserId("checker_99");
        when(service.approvePayment(id, "checker_99")).thenReturn(approved);

        mvc.perform(put("/payments/{id}/approve", id).header("X-User-ID", "checker_99"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"))
            .andExpect(jsonPath("$.approvedByUserId").value("checker_99"));
        verify(service).approvePayment(eq(id), eq("checker_99"));
    }

    @Test
    @DisplayName("PUT /payments/{id}/approve without the X-User-ID header returns 400")
    void approveWithoutUserHeader() throws Exception {
        mvc.perform(put("/payments/{id}/approve", UUID.randomUUID()))
            .andExpect(status().isBadRequest());
        verify(service, never()).approvePayment(any(), any());
    }

    @Test
    @DisplayName("PUT /payments/{id}/approve by the creator returns 400 with the four-eyes message")
    void approveByCreator() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.approvePayment(id, "maker_01"))
            .thenThrow(new SecurityException("You cannot approve a payment you created."));

        mvc.perform(put("/payments/{id}/approve", id).header("X-User-ID", "maker_01"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("You cannot approve a payment you created."));
    }

    @Test
    @DisplayName("PUT /payments/{id}/approve for an unknown payment returns 400")
    void approveUnknownPayment() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.approvePayment(id, "checker_99"))
            .thenThrow(new IllegalArgumentException("Payment not found."));

        mvc.perform(put("/payments/{id}/approve", id).header("X-User-ID", "checker_99"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Payment not found."));
    }

    // ---------- intended behaviour, not implemented yet ----------

    @Test
    @Disabled("Not implemented yet: a missing payment currently does not return 404")
    @DisplayName("GET /payments/{id} for an unknown payment returns 404")
    void getPaymentNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.getPayment(id)).thenReturn(Optional.empty());

        mvc.perform(get("/payments/{id}", id))
            .andExpect(status().isNotFound());
    }

    @Test
    @Disabled("Not implemented yet: the creator approving their own payment currently returns 400")
    @DisplayName("PUT /payments/{id}/approve by the creator returns 403 Forbidden")
    void approveByCreatorIsForbidden() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.approvePayment(id, "maker_01"))
            .thenThrow(new SecurityException("You cannot approve a payment you created."));

        mvc.perform(put("/payments/{id}/approve", id).header("X-User-ID", "maker_01"))
            .andExpect(status().isForbidden());
    }

    @Test
    @Disabled("Not implemented yet: clients must currently send a status, even though the server always sets it to PENDING")
    @DisplayName("POST /payments without a status is accepted, because the server sets the status itself")
    void createPaymentWithoutStatus() throws Exception {
        String body = """
            {"amount": 2500, "currency": "EUR", "createdByUserId": "maker_01"}
            """;
        when(service.createPayment(any(PaymentObject.class))).thenReturn(payment(UUID.randomUUID(), "PENDING", "maker_01"));

        mvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk());
    }
}
