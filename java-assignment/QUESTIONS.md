# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
Yes. I would standardize on one persistence style to reduce inconsistency and maintenance overhead.
Right now we mix:
Panache Active Record (Store extends PanacheEntity + static find/list)
Repository style (ProductRepository + ProductResource)
Ports/usecases/adapters (Warehouses)
Mixing patterns makes transactions/error handling/testing inconsistent. I’d move Store/Product towards the same approach (preferably repository + service/usecase for business rules), and centralize exception mapping instead of per-resource mappers.
```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
OpenAPI-first (generated):
Clear contract, easy client generation, consistent docs
More build/tooling complexity, DTO↔domain mapping overhead
Code-first:
Faster iteration, simpler tooling
Contract/doc drift risk, less consistency across endpoints
My choice: keep Warehouse contract-driven (it has more rules and is higher risk), and either (a) also make Product/Store contract-driven for consistency, or (b) at least enforce a published OpenAPI contract for Product/Store (generated from code) in CI.
```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
Priority:
Unit tests for warehouse use cases (fast, highest value): validations + replace/archive rules.
Small set of @QuarkusTest API tests to verify wiring + status codes (happy path + key errors).
DB/query tests only for non-trivial repository queries.
Keep it effective:
Add tests whenever a business rule is added/changed.
Avoid brittle REST tests (don’t hardcode IDs; assert on fields).
Use consistent error responses so tests can assert status + message reliably.
```