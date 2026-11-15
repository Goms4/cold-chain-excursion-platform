# ColdLatch

## Cold-Chain Excursion Command Platform

ColdLatch is a cold-chain monitoring and decision platform for pharmaceutical shipments.

The project focuses on what happens when temperature data is not perfect. Sensor readings can arrive late, appear more than once, arrive out of order, or have gaps. The system processes these events, identifies possible temperature excursions, links them to affected shipments and lots, and supports an investigation and final disposition.

The main workflow is:

**Monitor → Detect → Investigate → Decide**

---

## What the application does

- Register pharmaceutical shipments and storage profiles
- Record sensor temperature events
- Identify temperature excursions and their severity
- Detect duplicate, late, missing and out-of-order data
- Track affected lots
- Create and manage investigations
- Add operational/manual inspection evidence
- Support analyst decisions such as Release, Hold and Reject
- Maintain an audit history of important actions
- Reconcile operational data for analytics
- Provide an executive Power BI dashboard

---

## Project approach

The project was built as a modular application rather than splitting the system into multiple microservices.

### Application layer

**Frontend**
- React
- Vite

**Backend**
- Java 17
- Spring Boot
- REST APIs

**Database**
- PostgreSQL

**Data processing**
- Python
- Pandas
- NumPy

**Analytics**
- Snowflake
- Power BI

**Cloud / deployment**
- AWS EC2
- AWS S3
- Docker
- GitHub Actions

---

## Data flow

```text
Sensor / Operations
        |
        v
   React Frontend
        |
        v
   Spring Boot API
        |
        v
     PostgreSQL
        |
        v
 Python Reconciliation
        |
        v
      AWS S3
        |
        v
     Snowflake
        |
        v
     Power BI
```

The operational application uses PostgreSQL for live shipment, event, excursion, investigation and decision data.

Python reads the analytical views from PostgreSQL and prepares the datasets used for analytics.

---

## Handling temperature events

The system keeps event time and ingestion time separately.

**Event time** is used for temperature evaluation, while **ingestion time** records when the platform received the event.

This allows the system to handle cases where data arrives later than the time at which the temperature actually occurred.

The platform also distinguishes between:

- Normal readings
- Excursions
- Missing intervals
- Late events
- Duplicate events
- Out-of-order events
- Unknown conditions

Missing data is not automatically treated as safe.

---

## Investigation and decision workflow

When an excursion is identified, the shipment can move through an investigation process.

```text
Temperature Event
       ↓
Excursion Detection
       ↓
Investigation
       ↓
Operational Evidence
       ↓
Analyst Review
       ↓
Release / Hold / Reject
       ↓
Audit History
```

Manual inspection evidence can also be recorded when it does not completely agree with the sensor data. These cases are preserved as part of the investigation rather than silently replacing the sensor record.

---

## Analytics

The analytical pipeline produces datasets for:

- Shipments
- Lots
- Sensor events
- Excursions
- Investigations
- Decisions
- Temperature/data-quality information
- KPI summaries

These datasets are staged in AWS S3 and loaded into Snowflake for reporting.

The Power BI report provides a single-page executive overview showing the journey from monitoring temperature data through excursion detection, investigation and final decisions.

---

## Testing

The project was tested at multiple levels:

- Backend unit/integration tests
- API checks
- Frontend production build
- Python pipeline tests
- PostgreSQL pipeline execution
- Docker image builds
- End-to-end application workflow
- AWS deployment verification
- Power BI data and visual verification

The final application was also tested through the complete operational workflow from shipment registration through investigation and analyst decision.

---

## Deployment

The application is containerized using Docker and deployed to an AWS EC2 instance.

The deployed environment contains:

```text
Nginx
  |
  +-- React frontend
  |
  +-- Spring Boot backend
  |
  +-- PostgreSQL
```

GitHub Actions is used for automated build and deployment checks.

---

## Repository structure

```text
cold-chain-excursion-platform/
│
├── backend/          # Spring Boot application
├── frontend/         # React application
├── database/         # Database migrations and setup
├── data-pipeline/    # Python analytics pipeline
├── docker/           # Docker Compose and deployment setup
├── scripts/          # Utility and verification scripts
├── docs/             # Project documentation
├── .github/
│   └── workflows/    # GitHub Actions
│
├── architecture_flow.svg
└── README.md
```

---

## Project outcome

ColdLatch provides a complete flow for turning imperfect cold-chain temperature data into traceable operational decisions.

The important part of the project is not only detecting a temperature excursion, but preserving the information needed to understand **what happened, what was affected, what evidence was considered, and why a final decision was made.**
