package com.ionipay.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * End-to-end tests through the whole application: HTTP request -> controller -> service ->
 * JPA -> database. Runs on the in-memory H2 database configured in src/test/resources.
 */
@SpringBootTest
class PaymentApiIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private PaymentRepository repository;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        repository.deleteAll();
    }

    /** Creates a payment through the API and returns its generated id. */
    private UUID createPayment(String createdBy, String status) throws Exception {
        String body = """
            {"amount": 2500, "currency": "EUR", "status": "%s", "createdByUserId": "%s"}
            """.formatted(status, createdBy);
        String response = mvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(response, "$.id"));
    }

    @Test
    @DisplayName("a created payment is stored and can be read back by id and in the list")
    void createThenRead() throws Exception {
        UUID id = createPayment("maker_01", "PENDING");

        mvc.perform(get("/payments/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.amount").value(2500))
            .andExpect(jsonPath("$.createdByUserId").value("maker_01"));

        mvc.perform(get("/payments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(id.toString()));
    }

    @Test
    @DisplayName("a client cannot create an already approved payment: the server always stores PENDING")
    void clientCannotSetStatus() throws Exception {
        UUID id = createPayment("maker_01", "APPROVED");

        assertThat(repository.findById(id).orElseThrow().getStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("four-eyes flow: the creator is refused, then a second user approves the payment")
    void fourEyesApprovalFlow() throws Exception {
        UUID id = createPayment("maker_01", "PENDING");

        mvc.perform(put("/payments/{id}/approve", id).header("X-User-ID", "maker_01"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("You cannot approve a payment you created."));
        assertThat(repository.findById(id).orElseThrow().getStatus()).isEqualTo("PENDING");

        mvc.perform(put("/payments/{id}/approve", id).header("X-User-ID", "checker_99"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"));
        PaymentObject stored = repository.findById(id).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo("APPROVED");
        assertThat(stored.getApprovedByUserId()).isEqualTo("checker_99");
    }

    @Test
    @DisplayName("approving a payment that does not exist returns 400 and changes nothing")
    void approveUnknownPayment() throws Exception {
        mvc.perform(put("/payments/{id}/approve", UUID.randomUUID()).header("X-User-ID", "checker_99"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Payment not found."));
        assertThat(repository.count()).isZero();
    }
}
