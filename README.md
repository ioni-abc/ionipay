# Spring Boot backend for a simple four eyes approval payment system.

## Prerequisites:
- Docker Desktop

## Set up instructions:

### 1. Clone
- Clone the repo.
- `cd ionipay-backend`

### 2. Configure Secrets
- Create `db_name.txt` and type your desired database name (e.g., `ionipay_db`).
- Create `db_password.txt` and type your database password.

### 3. Run 
You do not need Java or Maven installed. Docker will handle the compilation and environment setup.
- `docker compose up -d --build`

### 4. Verify
- The API will be available at: http://localhost:8080.
- The Postgres Database will be available at: localhost:5433.
- Swagger UI: http://localhost:8080/swagger-ui/index.html.

### 5. Create a Vendor
```
curl -i -X POST http://localhost:8080/vendors \
  -H "Content-Type: application/json" \
  -d '{
    "name": "ACME Corp",
    "iban": "GR12345678901234567890"
  }'
```

### 6. Create a Payment
```
curl -i -X POST http://localhost:8080/payments \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 2500,
    "currency": "EUR",
    "vendorId": "PASTE_VENDOR_ID_HERE",
    "createdByUserId": "manager_01"
  }'
```

### 7. Approve a Payment
```
curl -i -X PUT http://localhost:8080/payments/PASTE_PAYMENT_ID_HERE/approve \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "director_99"
  }'
```



