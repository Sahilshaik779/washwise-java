# WashWise: Enterprise Laundry Management System

WashWise is a full-stack, role-based laundry management platform designed to streamline operations for both customers and service providers. This project utilizes a robust Java Spring Boot backend to ensure security, data integrity, and scalability, paired with a modern React.js frontend.

## Technical Stack

### Backend
* Framework: Java Spring Boot 3
* Security: Spring Security with stateless JWT (JSON Web Token) authentication
* Persistence: Hibernate/JPA
* Database: PostgreSQL

### Frontend
* Library: React.js (Vite)
* State Management: React Hooks
* API Communication: Axios

### Infrastructure
* Containerization: Docker and Docker Compose
* Version Control: Git and GitHub

## Core Features

* Role-Based Access Control (RBAC): Dedicated, secure portals for Customers and Admin/Staff members.
* Stateless Authentication: Secure session management using JWTs to mitigate unauthorized access risks across portals.
* Order Lifecycle Management: Backend state machine tracking orders through real-time status transitions.
* Automated QR Generation: Instant generation of QR codes for efficient order tracking and identification.
* Membership and Subscriptions: Dynamic models for managing customer plans and service usage.

## Project Structure

The project is organized as a monorepo containing both the backend and frontend applications.

```text
Washwise-java/
├── backend/                              # Spring Boot REST API
│   ├── .mvn/                             # Maven wrapper configuration
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/washwise/backend/
│   │   │   │   ├── config/               # Security and application configurations
│   │   │   │   ├── domain/               # Business logic, controllers, and entities
│   │   │   │   ├── exception/            # Global exception handling
│   │   │   │   ├── security/             # JWT filters and authentication logic
│   │   │   │   ├── utils/                # Helper utilities (e.g., QR Code generation)
│   │   │   │   └── WashwiseApplication.java
│   │   │   └── resources/                # Application properties and static assets
│   │   └── test/                         # Backend unit and integration tests
│   ├── pom.xml                           # Maven dependencies
│   └── mvnw                              # Maven wrapper executable
├── frontend/                             # React.js client application
│   ├── public/                           # Static public assets
│   ├── src/
│   │   ├── assets/                       # Application styles and images
│   │   ├── components/                   # React UI components (Dashboards, Auth)
│   │   ├── api.js                        # Axios interceptors and API endpoint definitions
│   │   ├── App.jsx                       # Main application routing
│   │   └── main.jsx                      # React DOM rendering entry point
│   ├── package.json                      # Node.js dependencies
│   ├── vite.config.js                    # Vite bundler configuration
│   └── Dockerfile                        # Frontend container specifications
├── docker-compose.yml                    # Orchestration for multi-container deployment
└── .gitignore                            # Global repository ignore rules
```

## Getting Started

### Prerequisites
* Java 21 or higher
* Node.js (v18 or higher)
* PostgreSQL (Running locally or via Docker)
* Maven

### Backend Setup
1. Navigate to the backend directory:
   ```bash
   cd backend
   ```
2. Configure your PostgreSQL database credentials in `src/main/resources/application.yml` or `application.properties`.
3. Build and start the Spring Boot application:
   ```bash
   ./mvnw spring-boot:run
   ```
   The backend API will be available at `http://localhost:8080`.

### Frontend Setup
1. Navigate to the frontend directory:
   ```bash
   cd frontend
   ```
2. Install the necessary Node dependencies:
   ```bash
   npm install
   ```
3. Start the Vite development server:
   ```bash
   npm run dev
   ```
   The frontend application will be accessible at `http://localhost:5173`.

### Docker Setup
To run the entire application stack (Database, Backend, and Frontend) using Docker containers:
1. Ensure Docker Desktop is running.
2. From the root directory (`Washwise-java`), execute:
   ```bash
   docker-compose up --build
   ```

## Security Implementation

The platform ensures data security through robust Spring Security configurations. All protected endpoints require a valid Bearer Token (JWT) provided in the HTTP Authorization header. The stateless nature of the JWTs allows for highly scalable and secure role verification for both Customer and Serviceman endpoints.

## Author

**Sahil**
* B.Tech Computer Science, VIT-AP University (Ranked Top 1% of cohort)