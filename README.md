# preq — Backend

Spring Boot + Kotlin backend for PreQ, a collaborative price comparison platform for Argentina.

![CI](https://github.com/PreQ-G10/preq-backend/actions/workflows/ci-quality-checks.yml/badge.svg?branch=main)
[![codecov](https://codecov.io/github/PreQ-G10/preq-backend/graph/badge.svg?token=IE1QB6CDSV)](https://codecov.io/github/PreQ-G10/preq-backend)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.0-7F52FF?logo=kotlin&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.14-6DB33F?logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6.5.9-6DB33F?logo=springsecurity&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-PostGIS%20%2B%20pgvector-4169E1?logo=postgresql&logoColor=white)
![ktlint](https://img.shields.io/badge/ktlint-1.6.0-7F52FF)

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.2.0 |
| Framework | Spring Boot 3.5.14 |
| Runtime | Java 21 (Temurin) |
| Database | PostgreSQL + PostGIS + pgvector |
| Security | Spring Security 6.5.9 + JWT (jjwt 0.12.6) |
| ML — Background removal | U2Net (ONNX Runtime 1.17.0) |
| ML — Embeddings | ResNet-50 (DJL 0.31.1 + PyTorch) |
| Image storage | Cloudinary |
| Linting | ktlint 1.6.0 |
| Build | Gradle |

## Key Features

### Product Detection
- **Image-based** — U2Net removes background → ResNet-50 generates 2048-dim embedding → cosine similarity search via pgvector
- **Barcode-based** — DB lookup → OpenFoodFacts API fallback

## Getting Started

### Prerequisites
- Java 21
- PostgreSQL with PostGIS and pgvector extensions
- A `.env` file with the required environment variables

### Environment Variables
```properties
DB_HOST=
DB_PORT=
DB_NAME=
DB_USER=
DB_PASSWORD=
CLOUDINARY_URL=
JWT_SECRET=
U2NET_MODEL_PATH=     # optional, defaults to ~/.cache/u2net.onnx
RESNET_MODEL_PATH=    # optional, defaults to ~/.cache/traced_resnet50.pt
```

### Run locally
```bash
./gradlew bootRun
```

### Run tests
```bash
./gradlew test
```

### Lint
```bash
# Check
./gradlew ktlintCheck
# Auto-fix
./gradlew ktlintFormat
```

## Configuration

### `application.yml`

| Property | Default | Description |
|---|---|---|
| `spring.jpa.hibernate.ddl-auto` | `create-drop` | DB schema strategy — change to `validate` or `update` in production |
| `spring.servlet.multipart.max-file-size` | `10MB` | Max image upload size |
| `spring.servlet.multipart.max-request-size` | `10MB` | Max request size |
| `jwt.access-token-expiration-ms` | `900000` | Access token expiry (15 minutes) |
| `jwt.refresh-token-expiration-ms` | `604800000` | Refresh token expiry (7 days) |
| `u2net.model.path` | `~/.cache/u2net.onnx` | Path to U2Net ONNX model |
| `resnet.model.path` | `~/.cache/traced_resnet50.pt` | Path to ResNet-50 model |