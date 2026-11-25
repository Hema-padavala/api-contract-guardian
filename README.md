# API Contract Guardian

## Project Overview

API Contract Guardian is a Java Spring Boot-based developer tool that helps developers identify API and Java source-code changes between different versions of a project.

It compares old and new API specifications and Java source files, detects changes, explains their impact, suggests possible fixes, and validates generated code through compilation and tests.

## Features

- **API Specification Comparison:** Compares old and new OpenAPI specifications.
- **Breaking Change Detection:** Identifies removed endpoints and other API changes.
- **Java Source Code Analysis:** Extracts and compares Java methods and API endpoints.
- **AI-Powered Explanations:** Uses a locally running AI model to explain code changes.
- **AI Fix Suggestions:** Generates possible Java code fixes.
- **Compilation Validation:** Checks whether generated Java code compiles.
- **Automated Testing:** Generates and runs test cases.
- **GitHub Integration:** Retrieves repository information, commits, and file contents.
- **Fix Safety Validation:** Checks suggested changes for potentially unsafe modifications.
- **Workflow Decision Report:** Reports whether suggested fixes are approved or rejected.

## Technologies Used

- Java 21
- Spring Boot
- Maven
- OpenAPI
- Ollama
- Qwen 2.5 (3B)
- GitHub REST API
- JUnit 5
- Postman
- Git and GitHub

## Project Structure

```text
api-contract-guardian/
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/apiguardian/
│   │           ├── controller/
│   │           └── service/
│   └── test/
├── .github/
│   └── workflows/
│       └── ci.yml
├── pom.xml
├── mvnw
├── mvnw.cmd
└── README.md
```

## Requirements

- Java 21
- Git
- Ollama
- Qwen 2.5 (3B) model
- Postman (for API testing)

## Running the Project

### 1. Clone the repository

```bash
git clone https://github.com/Hema-padavala/api-contract-guardian.git
cd api-contract-guardian
```

### 2. Configure Java 21 on Windows PowerShell

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
```

### 3. Start Ollama

Make sure Ollama is installed and the Qwen model is available.

```bash
ollama pull qwen2.5:3b
```

### 4. Run the Spring Boot application

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

### 5. Access the application

```text
http://localhost:8080
```

Health check:

```text
http://localhost:8080/api/health
```

## API Testing

The application can be tested using Postman.

Some available endpoints include:

- `GET /api/health` — Checks application status.
- `POST /api/specifications/upload` — Uploads old and new API specifications.
- `POST /api/java/compare` — Compares Java source code.
- `GET /api/github/automatic-test-report` — Generates an automated GitHub comparison and validation report.

## Testing

Run the project tests using Maven:

```powershell
.\mvnw.cmd clean test
```

## GitHub Actions

The project includes a GitHub Actions workflow that builds the application and runs tests using Java 21 whenever changes are pushed or a pull request is created for the main branch.

## Future Improvements

- Improve the accuracy of AI-generated code fixes.
- Add more comprehensive automated tests.
- Improve API change impact analysis.
- Enhance GitHub-based automation and reporting.

## Author

**Hema Prasanna Padavala**

GitHub: https://github.com/Hema-padavala
