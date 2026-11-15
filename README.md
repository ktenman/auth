# Authentication Service

[![Build & Test](https://github.com/ktenman/auth/actions/workflows/ci.yml/badge.svg)](https://github.com/ktenman/auth/actions/workflows/ci.yml)

## Overview

This authentication service is a Spring Boot application written in Kotlin that provides OAuth2 authentication using
Google as the identity provider. It's designed to be a secure and scalable solution for handling user authentication in
web applications.

![System Architecture](./screenshots/architecture.svg)

## Features

- OAuth2 authentication with Google
- Session management using Redis
- Configurable allowed email addresses
- Custom authentication success handler
- Secure logout mechanism
- Session validation endpoint

## Technologies Used

- Kotlin 2.4
- Spring Boot 4.1
- Spring Security
- Redis for session storage
- GraalVM native image (about 25 MiB of memory in use)
- Docker for containerization

## Prerequisites

- JDK 25
- Docker and Docker Compose
- Redis
- Google OAuth2 credentials (Client ID and Client Secret)

## Configuration

The application uses environment variables for configuration. Make sure to set the following variables:

- `GOOGLE_CLIENT_ID`: Your Google OAuth2 client ID
- `GOOGLE_CLIENT_SECRET`: Your Google OAuth2 client secret
- `REDIRECT_URI`: The URI to redirect after successful authentication
- `ALLOWED_EMAILS`: Comma-separated list of email addresses allowed to authenticate

## Running the Application

1. Set up the environment variables mentioned above.
2. Run the application using Gradle. It starts Redis from `compose.yaml` with Docker Compose by itself:
   ```bash
   ./gradlew bootRun
   ```

The application will start on port 8083 by default.

## API Endpoints

* `/login`: Login page
* `/oauth2/authorization/google`: Initiates Google OAuth2 authentication
* `/user`: Returns authenticated user information
* `/validate`: Validates the current session
* `POST /logout`: Logs out the current user
* `/actuator/health`: Health check, which also pings Redis

## Security Considerations

- Only configured email addresses are allowed to authenticate
- Sessions are stored securely in Redis
- CSRF protection is enabled
- Secure cookie settings for session management

## Development

To set up the development environment:

1. Clone the repository
2. Import the project into your IDE as a Gradle project
3. Ensure you have the required environment variables set
4. Run the AuthApplication.kt file to start the application

## Testing

Run the tests using:
```bash
./gradlew test
```

`bench/run.sh` signs in against a stand-in for Google, checks the session, page and logout paths, then prints the
container's memory use. CI runs it against each native image before pushing it.

## Docker

The application includes a Dockerfile for easy containerization. To build and run the Docker image:

1. Build the Docker image (compiling the native image takes a few minutes and about 6 GB of memory):
   ```bash
   docker build -t auth-service .
   ```
   If the native image fails with a missing serialization or reflection registration for a type, add that type to
   `src/main/resources/META-INF/native-image/auth/reachability-metadata.json`.

2. Run the Docker container:
   ```bash
    docker run -p 8083:8083 -e GOOGLE_CLIENT_ID=your_client_id -e GOOGLE_CLIENT_SECRET=your_client_secret -e REDIRECT_URI=your_redirect_uri -e ALLOWED_EMAILS=allowed@email.com auth-service
    ```
   The container expects Redis at the host name `redis`; set `SPRING_DATA_REDIS_HOST` to use another one.
