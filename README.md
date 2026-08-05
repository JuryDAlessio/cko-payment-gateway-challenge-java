## Project Overview
This project is a RESTful API Payment Gateway built with Java and Spring Boot. It acts as an intermediary between merchants and acquiring banks, allowing merchants to process e-commerce card payments and retrieve historical payment details.

The application includes:
* A `POST /payments` endpoint to authorize payments via an external bank simulator.
* A `GET /payments/{id}` endpoint to retrieve previously processed payments.
* Comprehensive validation to ensure card numbers, expiry dates, amounts, and currencies meet strict criteria before any processing occurs.
* An in-memory data store for quick retrieval and a thread-safe implementation to handle concurrent requests.

## Example User Flow
1. **Submit Payment:** A merchant's checkout system sends a `POST` request to `/payments` containing a shopper's card details (number, expiry, CVV, amount, and currency).
2. **Validation:** The gateway validates the payload. If the card is expired or the amount is invalid, it immediately rejects the request and returns a `400 Bad Request`.
3. **Idempotency Check:** The gateway checks if an identical payment was submitted in the last few seconds to prevent accidental double-charges.
4. **Bank Processing:** The gateway forwards the valid request to the Acquiring Bank Simulator.
5. **Response:** The bank simulator responds with an authorization or decline status. The gateway masks the card number, saves the record, and returns the result to the merchant with a unique payment ID.
6. **Reconciliation:** Later, the merchant queries `GET /payments/{id}` using the unique ID to confirm the payment status for their own reporting.

## Key Files
* `PaymentGatewayController.java`: The REST entry point handling incoming HTTP requests and routing them to the service layer.
* `PaymentGatewayService.java`: The core business logic orchestrating validation, duplication checks, and external bank calls.
* `PaymentsRepository.java`: A thread-safe, in-memory repository managing the storage and retrieval of payment records.
* `BankClient.java`: The HTTP client responsible for communicating with the external bank simulator.
* `CommonExceptionHandler.java`: A global interceptor that formats internal exceptions into clean, standardized API error responses.

## Project Structure
```text
├── src
│   ├── main
│   │   ├── java/com/checkout/payment/gateway
│   │   │   ├── client/         # External HTTP clients (Bank Simulator)
│   │   │   ├── configuration/  # Spring Bean configurations (RestTemplate)
│   │   │   ├── controller/     # REST API endpoints
│   │   │   ├── enums/          # Status enums (Authorized, Declined, Rejected)
│   │   │   ├── exception/      # Custom exceptions and global handlers
│   │   │   ├── model/          # DTOs (Requests, Responses, Errors)
│   │   │   ├── repository/     # Data access layer
│   │   │   └── service/        # Business logic layer
│   │   └── resources
│   │       └── application.properties # Application config
│   └── test                    # Unit and Integration tests
├── pom.xml (or build.gradle)   # Dependency management
└── README.md                   # Project documentation
```
## How to Run the Application

### Running Locally
To run the application locally, you'll first need the bank simulator running. Assuming the simulator is provided via Docker:
1. Start the simulator: `docker-compose up`
2. Start the Spring Boot application using your build tool:
   * **Gradle:** `./gradlew bootRun`
3. The gateway will be available at `http://localhost:8090`.

### Running with Docker
You can also build and spin up the entire stack via Docker Compose: `docker-compose up --build`

## Assumptions
* **Data Persistence:** For this phase, an in-memory data store is sufficient. Data will be lost when the application restarts.
* **Idempotency:** A duplicate request is defined as having the exact same card details, CVV, and amount within a 5-second window.
* **Bank Simulator:** The acquiring bank simulator is available at `http://localhost:8080/payments` and responds reliably within a few seconds.
* **Currency Support:** The gateway specifically validates against common ISO codes (e.g., USD, GBP, EUR) for this iteration.

## Design Considerations
* **Separation of Concerns:** The architecture heavily utilizes a layered approach (Controller -> Service -> Repository/Client) so that the HTTP transport logic is completely decoupled from business rules and storage mechanisms.
* **Thread Safety:** The in-memory repository uses a `ConcurrentHashMap` combined with a `ReentrantLock` for the chronological history list to ensure race conditions don't corrupt the duplicate-checking logic during high concurrency.
* **Fail-Fast Validation:** Spring's `@Valid` annotations are used alongside custom exception handling to reject bad requests instantly, saving network bandwidth and downstream processing time.
* **Date/Time Validation & Risks:** The application relies on the system's local date and time to validate whether a credit card's expiry year is in the future. A potential risk here is timezone misalignment. If the server's timezone differs significantly from the merchant's or the shopper's timezone, edge cases around the New Year could result in false rejections or incorrect authorizations.
* **Edge Case Handling (Index Out of Bounds):** When masking the card number to store only the last four digits, an `if` statement was implemented to verify the string length. This patches a potential `StringIndexOutOfBoundsException` (a software error that crashes the program when it tries to access text data that doesn't exist) that could occur if a malformed or artificially shortened string somehow bypasses earlier validation steps and reaches the `length() - 4` extraction logic.
* **Security & Compliance:** Full card numbers are never returned in responses. Only the last four digits are stored and transmitted back to the client to comply with basic PCI-DSS concepts (the payment card industry's strict security standards for handling credit card data safely).

## Future Work
* **Persistent Storage:** Replace the in-memory repository with a relational database like PostgreSQL or MySQL using Spring Data JPA.
* **Authentication & Authorization:** Secure the API endpoints using API keys or OAuth2 so only verified merchants can process payments.
* **Resilience & Retries:** Implement Circuit Breaker patterns (a safety mechanism that temporarily stops the system from making calls to a service that is failing, preventing system-wide crashes) on the `BankClient` to gracefully handle downstream simulator timeouts or outages.
* **Observability:** Integrate Prometheus and Grafana for system metrics, and implement distributed tracing (a method to track and monitor requests as they move through different parts of the system) to track a payment's lifecycle across microservices (a way of building software as a collection of small, independent pieces that talk to each other).
* **Pagination:** Add pagination to a new endpoint (e.g., `GET /payments`) so merchants can retrieve all their payments in batches.