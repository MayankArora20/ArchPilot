In the rapidly evolving landscape of 2026, you've noticed that the older Gemini 1.5 models have been retired (as of mid-2025) and replaced by the Gemini 2.0 and 2.5 families. However, because these newer models are more powerful, their free-tier "Rate Limits" are stricter than the old ones.

On the free tier, the limits for the models you see in your list are roughly:
Gemini 2.5 Flash: ~10 Requests Per Minute (RPM) and 250,000 Tokens Per Minute (TPM).
Gemini 2.5 Flash-Lite: ~15 Requests Per Minute (RPM). This is the "speed king" for free agents.

Thats why we need a service that acts as a **"Guardrail"** for our AI Agent. Its job is to intercept requests, check the current token "debt" or request count, and either grant passage or calculate a "wait time" for the user.

### 1. The Strategy: The "Bucket & Refill" Plan

To make this scalable and handle "bursts" (like a user suddenly uploading 5 files), we use the **Token Bucket** strategy at a database level.

* **The Bucket:** Each user has a "bucket" that holds a maximum number of tokens (e.g., 250,000 for Gemini 2.5 Flash).
* **The Consumption:** Every request removes tokens from the bucket based on the size of the GitHub code or Meeting summary sent.
* **The Refill:** The system automatically "refills" the bucket at a steady rate (e.g., 5,000 tokens per minute) until it’s full again.

---

### 2. Database Requirement Plan (PostgreSQL)

You need a structure that doesn't just store logs, but stores the **state** of the rate limit so that multiple Spring Boot instances can share the same information.

* **User Identity Tracking:** Link limits to a unique ID (API Key or UserID) rather than just an IP address, as IPs can change.
* **Current Balance State:** Store how many tokens are currently available and the **exact timestamp** of the last request.
* **Metadata Storage:** Track which model is being hit (2.5 Flash vs 2.5 Pro) since they have different "cost" weights.

---

### 3. Service Requirement Plan (Spring Boot)

The service should be a thin "Interceptor" that runs **before** the AI Agent logic ever starts.

#### **A. Pre-Request Validation**

* **Calculated Recovery:** When a request arrives, the service calculates the "New Balance" by seeing how much time has passed since the last request and adding the "refill" amount.
* **Wait-Time Calculation:** If the request requires more tokens than available, the service calculates:



#### **B. The Countdown Logic**

* **Non-Blocking Rejection:** If the limit is reached, the service throws a custom exception that returns a **429 Too Many Requests** status.
* **Header Injection:** The response must include a specific field (e.g., `X-Retry-After`) or a JSON body containing the exact seconds the user must wait.

#### **C. Post-Request Update**

* **Token Counting:** After the AI responds, the service must read the "Usage" object returned by the Gemini API (which tells you the exact number of prompt and completion tokens used) and subtract those from the user's database balance.

---

### 4. Scalability & Performance Requirements

To ensure this side service doesn't slow down your agent, it needs the following:

* **Atomic Operations:** The database updates must be "Atomic." This prevents a user from sending 10 requests at the exact same millisecond to bypass the limit.
* **Caching Layer (Optional but Recommended):** While you want Postgres for long-term persistence, using a fast memory layer (like Redis) for the "active countdowns" will make the service significantly faster.
* **Background Cleanup:** A scheduled task to archive old usage logs so the database doesn't grow infinitely and become slow.

**Next Step:** Would you like me to outline the logic for how the "Refill" calculation works mathematically so you can implement it in your service?