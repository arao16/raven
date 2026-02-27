# Case Study Scenarios to discuss

## Scenario 1: Cost Allocation and Tracking
**Situation**: The company needs to track and allocate costs accurately across different Warehouses and Stores. The costs include labor, inventory, transportation, and overhead expenses.

**Task**: Discuss the challenges in accurately tracking and allocating costs in a fulfillment environment. Think about what are important considerations for this, what are previous experiences that you have you could related to this problem and elaborate some questions and considerations

**Questions you may have and considerations:**
- **Main challenges**
    - Costs happen in different systems and timeframes (WMS/TMS/ERP/payroll) → hard to reconcile.
    - Shared resources (labor teams, equipment, facilities) require fair allocation rules.
    - Inventory cost vs operational cost can be mixed incorrectly (e.g., shrink, damages, rework).
    - Transportation is multi-leg and often shared across stores/shipments.
    - Data quality: missing scans, inconsistent location/store identifiers, late adjustments/credits.
- **Key design considerations**
    - Define a **cost model**: cost centers (warehouse/store), activities (pick/pack/receive), and objects (orders, SKUs, pallets).
    - Decide allocation basis: per order line, per unit, per weight/volume, per handling step, per labor minute.
    - Track both **planned vs actual** costs and store variance reasons.
    - Ensure consistent **master data** (warehouse BU code, store id, product/SKU).
    - Auditable trail: raw events + derived allocations + versioned rules.
- **Questions to ask**
    - What decisions will this enable (pricing, SLA, network design, budget control)?
    - Required granularity: daily/weekly? per order? per SKU? per store?
    - What’s the source of truth for labor time, freight, inventory valuation, overhead?
    - How do we treat exceptions (returns, cancellations, partial shipments, stock corrections)?
    - What accuracy threshold is “good enough” vs cost of measurement?


## Scenario 2: Cost Optimization Strategies
**Situation**: The company wants to identify and implement cost optimization strategies for its fulfillment operations. The goal is to reduce overall costs without compromising service quality.

**Task**: Discuss potential cost optimization strategies for fulfillment operations and expected outcomes from that. How would you identify, prioritize and implement these strategies?

**Questions you may have and considerations:**
- **Potential strategies**
    - Labor: slotting optimization, pick path optimization, batching/waves, training, flexible staffing.
    - Inventory placement: reduce split shipments by better stock distribution across warehouses.
    - Transportation: mode/carrier mix, zone skipping, consolidation, delivery promise tuning.
    - Process: reduce rework/damages, improve receiving accuracy, automate high-volume steps.
    - Network: right-size warehouse capacity vs demand; avoid expensive overflow.
- **How to identify opportunities**
    - Pareto: top cost drivers by warehouse/store/activity/SKU.
    - Variance analysis: planned vs actual; highlight outliers.
    - Unit economics: cost per order, per line, per unit, per kg/m³, per delivery.
    - SLA correlation: where cost is high with no service gain.
- **Prioritization**
    - Impact vs effort + time-to-value.
    - Risk (customer impact, operational disruption).
    - Reversibility and measurability (can we run A/B or phased rollout?).
- **Implementation approach**
    - Define baseline metrics and target KPIs.
    - Pilot in one warehouse/store cluster.
    - Measure before/after with control group where possible.
    - Operational playbooks + monitoring to prevent regression.


## Scenario 3: Integration with Financial Systems
**Situation**: The Cost Control Tool needs to integrate with existing financial systems to ensure accurate and timely cost data. The integration should support real-time data synchronization and reporting.

**Task**: Discuss the importance of integrating the Cost Control Tool with financial systems. What benefits the company would have from that and how would you ensure seamless integration and data synchronization?

**Questions you may have and considerations:**
- **Why integrate**
    - Finance-grade numbers (GL alignment) reduce disputes and “two versions of truth”.
    - Faster close and better variance explanations.
    - Enables chargebacks/showbacks across warehouses/stores/business units.
- **Integration approach**
    - Start with a **canonical cost event** model + mapping to GL accounts/cost centers.
    - Use near-real-time for operational insights, but keep an auditable batch path for financial close.
    - Idempotent ingestion (dedupe keys), retries, and reconciliation reports.
- **Data synchronization concerns**
    - Timing differences (accruals, freight invoices arriving late).
    - Currency, tax, and accounting periods.
    - Adjustments/credit notes must be linked to original cost events.
- **Questions to ask**
    - Which system owns the chart of accounts and cost center hierarchy?
    - Required latency (minutes vs daily) for decision-making?
    - What’s the reconciliation process and tolerance for differences?
    - Who approves allocation rule changes and how are they versioned?

## Scenario 4: Budgeting and Forecasting
**Situation**: The company needs to develop budgeting and forecasting capabilities for its fulfillment operations. The goal is to predict future costs and allocate resources effectively.

**Task**: Discuss the importance of budgeting and forecasting in fulfillment operations and what would you take into account designing a system to support accurate budgeting and forecasting?

**Questions you may have and considerations:**
- **Why it matters**
    - Prevents capacity surprises (labor, space, transportation).
    - Supports pricing, promotions planning, and SLA commitments.
    - Helps manage cash flow and investment planning (automation, new sites).
- **What to forecast**
    - Demand drivers: orders, lines, units, returns, bulky items.
    - Cost drivers: labor hours, overtime, carrier rates, fuel surcharges, damage rates.
    - Capacity constraints: max warehouses per location, max capacity per location (ties to operational limits).
- **Model considerations**
    - Seasonality (holidays, campaigns), lead/lag effects.
    - Scenario planning (base/best/worst; promo spikes; carrier rate changes).
    - Forecast at the right granularity: by warehouse, store region, product category.
    - Track forecast accuracy and continuously recalibrate.
- **Questions to ask**
    - What are the planning horizons (weekly staffing vs quarterly budgets)?
    - Which levers can ops actually change (staffing, inventory placement, routing rules)?
    - What is the approval workflow for budget changes?


## Scenario 5: Cost Control in Warehouse Replacement
**Situation**: The company is planning to replace an existing Warehouse with a new one. The new Warehouse will reuse the Business Unit Code of the old Warehouse. The old Warehouse will be archived, but its cost history must be preserved.

**Task**: Discuss the cost control aspects of replacing a Warehouse. Why is it important to preserve cost history and how this relates to keeping the new Warehouse operation within budget?

**Questions you may have and considerations:**
- **Why preserve cost history**
    - Trend analysis and benchmarking: compare “before vs after” replacement.
    - Auditability: costs tied to past operations must not be lost when an entity is archived.
    - Prevents breaking downstream reporting that references the BU code.
- **How to model it**
    - Treat “warehouse replacement” as a new version of the warehouse:
        - Keep stable business identifier (BU code) for business continuity.
        - Store a separate technical identifier/version (e.g., warehouse_id + createdAt/archivedAt).
    - Ensure cost events link to the correct warehouse **version** (time-bounded validity).
    - Budget for ramp-up period: temporary inefficiency, dual running, migration costs.
- **Budget control aspects**
    - Establish baseline unit costs of the old warehouse before cutover.
    - Track replacement project costs separately (capex/opex).
    - Monitor early warning signals post-cutover (cost per order, overtime, rework, missed SLA penalties).
- **Questions to ask**
    - What is the cutover plan (big-bang vs phased)?
    - Do we run both sites in parallel and how do we allocate shared costs?
    - How do we handle inventory transfer and transportation spikes during migration?


## Instructions for Candidates
Before starting the case study, read the [BRIEFING.md](BRIEFING.md) to quickly understand the domain, entities, business rules, and other relevant details.

**Analyze the Scenarios**: Carefully analyze each scenario and consider the tasks provided. To make informed decisions about the project's scope and ensure valuable outcomes, what key information would you seek to gather before defining the boundaries of the work? Your goal is to bridge technical aspects with business value, bringing a high level discussion; no need to deep dive.

Key information I would gather before scoping:
- Who are the users (finance, ops, product) and what decisions must the tool support?
- Must-have metrics and accuracy requirements (finance close vs operational steering).
- Required granularity (per warehouse/store/order/SKU) and latency (real-time vs daily).
- Source systems and data readiness (WMS/TMS/ERP/payroll), and master data quality.
- Allocation rules governance: ownership, approvals, versioning, audit needs.
- Non-functional requirements: reconciliation, observability, security, retention, reporting needs.
- Delivery plan: incremental milestones (tracking → allocation → reporting → optimization → forecasting).
