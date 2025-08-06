# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

必须用中文回复我！

## Project Overview

EasyPan is a Spring Boot file management application with an integrated TA (Transfer Agent) module for automated file transfers. The application combines a file sharing/management system with automated FTP/SFTP file download capabilities.

### Technology Stack
- Spring Boot 2.6.1
- Java 8
- MyBatis for database operations
- MySQL database
- Redis for caching and configuration storage
- Maven for dependency management

## Common Development Commands

### Build and Run
```bash
# Build the project
mvn clean compile

# Package the application
mvn clean package

# Run the application
mvn spring-boot:run
# Or run the JAR directly:
java -jar target/easypan-1.0.jar
```

### Configuration
- Main application runs on port **7090** with context path `/api`
- Database configuration in `src/main/resources/application.properties`
- Default database: `easychat` on localhost:3306
- Redis runs on localhost:6379

## Project Architecture

### Core EasyPan Application
The main application follows a standard Spring Boot layered architecture:

- **Controllers** (`com.easypan.controller`): REST API endpoints for file operations, user management, sharing, etc.
- **Services** (`com.easypan.service`): Business logic layer with implementations in `impl` package
- **Mappers** (`com.easypan.mappers`): MyBatis data access layer with XML configurations
- **Entities**: 
  - `po`: Persistent objects (database entities)
  - `dto`: Data transfer objects  
  - `vo`: View objects for API responses
  - `query`: Query parameter objects
- **Configuration** (`com.easypan.entity.config`): Application configuration classes
- **Utilities** (`com.easypan.utils`): Common utility classes for JSON, HTTP, validation, etc.

### TA (Transfer Agent) Module
Located in `com.easypan.TA`, this module handles automated file transfers:

- **Purpose**: Downloads financial data files (CPDM/JYCS formats) from remote FTP/SFTP servers
- **Integration**: REST API at `/api/file-transfer/*` with scheduled tasks every 5 minutes
- **Protocols**: Supports FTP, SFTP with password, and SFTP with private key authentication
- **Key Components**:
  - `Config/TransferConfig`: Connection configuration stored in Redis
  - `Service/FileTransferService`: Core transfer logic with duplicate prevention
  - `InputStream/*TransferProtocol`: Protocol-specific implementations (FTP/SFTP)
  - `Controller/FileTransferController`: REST API for configuration management

## File Structure

### Main Application Entry Point
- `EasyPanApplication.java`: Main Spring Boot application class

### Key Configuration Files
- `application.properties`: Database, Redis, email, and application settings
- `pom.xml`: Maven dependencies and build configuration
- `logback-spring.xml`: Logging configuration

### Database
- `easypan.sql`: Database schema (located in project root)
- MyBatis XML mappers in `src/main/resources/com/easypan/mappers/`

## Development Guidelines

### Database Access
- Use MyBatis mappers for database operations
- Follow the existing query object pattern for complex queries
- Mapper interfaces in `com.easypan.mappers` with XML implementations in resources

### API Development
- Extend `ABaseController` for common functionality
- Use `ResponseVO` for consistent API responses
- Follow existing controller patterns for authentication and validation

### File Operations
- File uploads limited to 15MB
- Temporary files stored in configured project folder + temp directory
- Use existing file utility classes in `com.easypan.utils`

### TA Module Development
- Transfer configurations stored in Redis via `TransferConfigService`
- File naming follows pattern: `[TYPE]-YYYYMMDDHHMMSSFFF.txt`
- Implement `FileTransferProtocol` interface for new transfer protocols
- Use `FileNameParser` for extracting metadata from standardized filenames

### Error Handling
- Global exception handling in `AGlobalExceptionHandlerController`
- Business exceptions use `BusinessException` class
- Follow existing error response patterns

## Testing and Validation

### Connection Testing
- TA module provides connection testing via REST API
- Test database connections through application startup
- Redis connectivity verified through configuration service

### File Transfer Testing
- Manual transfer triggering available through REST API
- Scheduled transfers run every 5 minutes
- Transfer results include timing metrics and success/failure details