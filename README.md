Here is your final README.md content in plain text format, ready to copy-paste into your project:

***

# OpenText Partner Directory Application

This project provides a full-stack web application as a solution to a three-part interview assignment:
- Right-angled triangle printing algorithm (Question 1)
- REST API for joining OpenText partner and solution data (Questions 2 & 3)
- React frontend displaying and filtering the partner directory (Question 3 + Bonus)

The system consists of:
- A Spring Boot backend
- A React/Vite frontend

***

## 📝 Interview Questions Addressed

**Question 1: Triangle Printing**  
Write a function that prints a right-angled triangle of `M` rows, the base row having `N` characters. For each row `i` (from 1 to M), stars are calculated by linear interpolation:
- stars = round(i * N / M)  
Example for M=3, N=4:
```
*
**
****
```
_Assumption:_ The triangle is right-angled; using linear interpolation for stars per row.

**Question 2: Partner-Solution Data Join**  
Fetch partners from [OpenText Partner Directory](https://www.opentext.com/partners/partner-directory) and solutions from [Partner Solutions Catalog](https://www.opentext.com/products-and-solutions/partners-and-alliances/partner-solutions-catalog). Join data on normalized partner names, output JSON, and note assumptions.

**Question 3: UI and Filtering**  
Create a Partner Directory UI using OpenText styling (cyan/blue palette), showing partners and associated solutions.  
Bonus: Checkbox filter "Listed Solutions" shows only partners with solutions.

***

## 🚀 Quick Start

Run backend and frontend independently.

### Prerequisites

- Java 17 or higher and Maven (backend)
- Node.js LTS and npm (frontend)

### Backend Setup (Terminal 1)

```
cd partner-api-backend/
mvn spring-boot:run
```
Backend REST API is served at: http://localhost:8080

### Frontend Setup (Terminal 2)

```
cd frontend/
npm install      # run once
npm run dev
```
Frontend runs at: http://localhost:3000

***

## 🛠️ Architecture & Implementation Details

### Backend: Spring Boot

| Component             | Role                                             | Notes                                                                                  |
|-----------------------|-------------------------------------------------|----------------------------------------------------------------------------------------|
| PartnerService        | Fetch, join, cache, filter partner/solution data | Uses WebClient for async fetch; @Cacheable for initial data load; joins on normalized partner names.|
| PartnerController     | REST API endpoint                                | Exposes `/api/partners` with pagination and hasSolutions filter.                        |
| PartnerApiApplication | Main app config                                  | Enables caching; disables DB configs (handles all data in-memory).                      |

### Frontend: React + Vite

| Component          | Role                                  | Notes                                                                |
|--------------------|---------------------------------------|----------------------------------------------------------------------|
| App.jsx            | Core UI & state management            | Fetch API, handle pagination, search, "Listed Solutions" filter.     |
| vite.config.js     | Dev proxy configuration               | Routes `/api` requests to backend at port 8080 for CORS handling.    |
| tailwind.config.js | Styling                               | Custom cyan/blue palette for OpenText branding.                      |

***

## 🔍 Assumptions & Design Decisions

- Triangle printing uses linear interpolation and rounding for stars per row.
- Partner/solution data joined solely by case-insensitive, trimmed partner name.
- Partner Directory is treated as source-of-truth; partners without solutions have empty arrays.
- Data fetching is concurrent and joined via in-memory hash maps for speed.
- "OpenText Styles" interpreted as clean, responsive, corporate-themed UI.
- "Listed Solutions" filter implemented as a checkbox toggling query param.
- Pagination handled backend and frontend for efficient data transfer and rendering.
- No database; all data handled in memory and cached server-side.

***

## 📁 Structure

```
.
├── partner-api-backend/   # Spring Boot backend
├── frontend/              # React + Vite frontend
├── README.md              # This file
```

***

## ⚡ Usage Example

Query for partner data (page 1, size 10, solutions only):

```
GET http://localhost:8080/api/partners?page=1&size=10&hasSolutions=true
```
JSON response includes partners, each with a solutions array (possibly empty).

***

## 👩‍💻 Contributors

- Priyanka Patel (PatelPriyanka)

***

Clone and run the app locally using the steps above. For any issues or suggestions, open an issue on GitHub.

***
