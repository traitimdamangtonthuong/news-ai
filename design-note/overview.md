# News AI — Project Technical Overview

**Project type:** Java Spring Boot News Aggregation + AI Assistant
**Purpose:** Build a news-reading platform that collects articles from reputable sources, stores and manages them through a database, and later provides AI-powered search and question answering.

---

# 1. Project Overview

The project is a web-based news aggregation application built primarily with **Java**.

The initial goal is to create a system capable of:

1. Collecting news articles from external sources.
2. Storing article information in a relational database.
3. Providing APIs for viewing, creating, updating, and deleting articles.
4. Searching articles.
5. Adding an AI chatbot that can answer questions based on collected news.
6. Providing modern AI-assisted functionality such as article summarization and semantic search.

The current implementation is focused on establishing a reliable backend and database foundation before implementing crawling and AI features.

The project is intentionally being developed as a **Spring Boot monolithic application** rather than a distributed microservice system. This keeps the system manageable for a student project with a limited development time.

---

# 2. Technologies Used So Far

## 2.1 Java

Java is the primary programming language of the project.

Java is used for:

* Backend application logic
* Object-oriented programming
* Entity classes
* Services
* REST controllers
* Database interaction through JPA
* Future crawler implementation
* Future AI integration through Spring AI

The project follows an object-oriented architecture rather than placing all logic inside a single class.

---

# 3. Development Environment

## 3.1 IDE / Code Editor

The current development environment is:

**Visual Studio Code**

VS Code is being used to:

* Create Java classes
* Edit Spring Boot files
* Run the application through the integrated terminal
* Inspect project structure
* Debug compilation/runtime problems

The application can be started from the VS Code terminal using Maven.

---

# 4. Spring Boot

The backend is implemented using **Spring Boot**.

Spring Boot provides:

* Application startup
* REST API support
* Dependency injection
* Configuration management
* Integration with Spring Data JPA
* Integration with PostgreSQL
* Future integration with Spring AI

The project follows a layered backend architecture:

```text
Client / Postman
       ↓
Controller
       ↓
Service
       ↓
Repository
       ↓
JPA / Hibernate
       ↓
PostgreSQL
```

This separation is important because each layer has a specific responsibility.

---

# 5. Maven

The project uses **Maven** as its build and dependency management system.

Maven is responsible for:

* Downloading libraries
* Managing dependencies
* Compiling Java source code
* Running the Spring Boot application
* Running tests

The project contains:

```text
pom.xml
mvnw
mvnw.cmd
```

The Maven Wrapper allows the project to be run without requiring a separately installed Maven version.

On Windows, the application can be started with:

```powershell
.\mvnw.cmd spring-boot:run
```

---

# 6. Current Project Structure

The backend currently follows a structure similar to:

```text
src/
└── main/
    └── java/
        └── com/
            └── newsai/
                └── news_ai/
                    ├── NewsAiApplication.java
                    │
                    └── article/
                        ├── Article.java
                        ├── ArticleRepository.java
                        ├── ArticleService.java
                        └── ArticleController.java
```

### Responsibilities

`NewsAiApplication.java`

Starts the Spring Boot application.

`Article.java`

Represents an article as a Java object and database entity.

`ArticleRepository.java`

Provides database access through Spring Data JPA.

`ArticleService.java`

Contains application/business logic.

`ArticleController.java`

Exposes REST API endpoints to clients.

---

# 7. REST API

The current API is based around:

```text
/api/articles
```

The controller is implemented as a REST controller:

```java
@RestController
@RequestMapping("/api/articles")
```

The API currently supports the basic CRUD operations.

## GET — Retrieve all articles

```http
GET /api/articles
```

Returns all articles stored in the database.

---

## GET — Retrieve one article

```http
GET /api/articles/{id}
```

Example:

```http
GET /api/articles/1
```

Returns the article with ID `1`.

---

## POST — Create an article

```http
POST /api/articles
```

Example request body:

```json
{
  "title": "Java and AI",
  "description": "Testing PostgreSQL",
  "content": "This article is stored in PostgreSQL.",
  "source": "Tech News",
  "url": "https://example.com/java-ai",
  "imageUrl": "https://example.com/java-ai.jpg",
  "publishedAt": "2026-09-07T10:00:00"
}
```

The database generates the article ID automatically.

The client therefore does **not** need to send:

```json
"id": 1
```

---

## PUT — Update an article

```http
PUT /api/articles/{id}
```

Example:

```http
PUT /api/articles/1
```

Used to modify an existing article.

---

## DELETE — Delete an article

```http
DELETE /api/articles/{id}
```

Example:

```http
DELETE /api/articles/1
```

Deletes an article from the database.

---

# 8. Postman

**Postman** is being used to test the REST API.

Postman allows the team to test the backend without having to build the frontend first.

The current testing flow is:

```text
Spring Boot
    ↓
localhost:8080
    ↓
REST API
    ↓
Postman
```

Examples:

```text
GET    /api/articles
GET    /api/articles/1
POST   /api/articles
PUT    /api/articles/1
DELETE /api/articles/1
```

Postman is therefore being used as the initial API client during backend development.

---

# 9. PostgreSQL

The project uses **PostgreSQL** as the relational database.

The planned/current database is:

```text
Database name:
news_ai
```

The normal PostgreSQL server uses:

```text
Host: localhost
Port: 5432
```

The connection follows the pattern:

```text
jdbc:postgresql://localhost:5432/news_ai
```

PostgreSQL is responsible for persistent data storage.

Unlike an in-memory Java list, PostgreSQL allows data to remain after the Spring Boot application is stopped and restarted.

---

# 10. JPA

The project uses **Spring Data JPA** to communicate with PostgreSQL.

JPA allows Java objects to be mapped to database tables.

The basic relationship is:

```text
Java Object
     ↕
    JPA
     ↕
Database Table
```

For example:

```java
@Entity
@Table(name = "articles")
public class Article
```

means that the `Article` Java class is mapped to the PostgreSQL table:

```text
articles
```

---

# 11. Hibernate

Spring Data JPA uses **Hibernate** as the JPA implementation.

Hibernate is responsible for translating operations such as:

```java
articleRepository.findAll();
```

into SQL/database operations.

Conceptually:

```text
Java:
articleRepository.findAll()

        ↓

Hibernate / JPA

        ↓

SQL

        ↓

PostgreSQL
```

This means the application does not need to manually write SQL for every basic CRUD operation.

---

# 12. Current Article Entity

The current Article entity contains the following fields:

```text
Article
├── id
├── title
├── description
├── content
├── source
├── url
├── imageUrl
├── publishedAt
└── crawledAt
```

The Java types are currently:

```text
id            → Long
title         → String
description   → String
content       → String
source        → String
url           → String
imageUrl      → String
publishedAt   → LocalDateTime
crawledAt     → LocalDateTime
```

---

# 13. JPA Entity Mapping

The Article entity uses:

```java
@Entity
@Table(name = "articles")
```

The primary key is:

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

This means PostgreSQL is responsible for generating the article ID.

Previously, a temporary `nextId` approach was considered for an in-memory list.

That is no longer needed once PostgreSQL/JPA is being used.

The database should generate IDs instead.

---

# 14. Article Validation / Database Constraints

The Article entity can use JPA annotations such as:

```java
@Column(nullable = false)
```

For example:

```java
@Column(nullable = false, length = 500)
private String title;
```

This indicates that the title should not be null.

The URL can also eventually be protected against duplication:

```java
@Column(nullable = false, unique = true, length = 2048)
private String url;
```

This is particularly important for the future crawler because a crawler may encounter the same article multiple times.

---

# 15. Text Storage

Article content can be significantly larger than a normal short string.

For this reason, the entity uses PostgreSQL-compatible text storage such as:

```java
@Column(columnDefinition = "TEXT")
private String content;
```

The same approach can be used for:

```java
description
content
```

This is more appropriate than treating article content like a short VARCHAR field.

---

# 16. Date and Time

The project uses Java:

```java
LocalDateTime
```

for:

```text
publishedAt
crawledAt
```

The meaning is:

### publishedAt

When the original article was published.

### crawledAt

When our system collected the article.

This distinction becomes important when implementing the crawler.

For example:

```text
Article published:
2026-09-07 08:30

Crawler collected:
2026-09-07 09:05
```

---

# 17. PostgreSQL Database Creation

The project database is intended to be:

```text
news_ai
```

The application connects through Spring Boot configuration.

The basic development configuration is:

```properties
spring.application.name=news-ai

spring.datasource.url=jdbc:postgresql://localhost:5432/news_ai
spring.datasource.username=postgres
spring.datasource.password=${DB_PASSWORD}

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

---

# 18. Database Schema Generation

The project currently uses:

```properties
spring.jpa.hibernate.ddl-auto=update
```

During development, Hibernate can detect the entity:

```java
@Entity
@Table(name = "articles")
```

and create/update the corresponding table.

Therefore the team does not necessarily need to manually create the `articles` table using SQL.

The expected database structure is approximately:

```text
news_ai
└── public
    └── articles
```

with columns corresponding to the Article entity.

---

# 19. Database Password Security

The PostgreSQL password should not be hard-coded into the Java source code.

The current approach is to use an environment variable:

```properties
spring.datasource.password=${DB_PASSWORD}
```

On Windows PowerShell:

```powershell
$env:DB_PASSWORD="YOUR_POSTGRES_PASSWORD"
```

Then the application can be started:

```powershell
.\mvnw.cmd spring-boot:run
```

This is preferable to committing a real database password to GitHub.

---

# 20. Repository Layer

The Article repository uses Spring Data JPA:

```java
public interface ArticleRepository
        extends JpaRepository<Article, Long> {
}
```

By extending `JpaRepository`, the application automatically receives common database operations such as:

```text
findAll()
findById()
save()
deleteById()
existsById()
```

This is one of the main reasons Spring Data JPA is useful for the project.

---

# 21. Service Layer

The Article service sits between the controller and repository.

Current responsibilities include:

```text
Get all articles
Get article by ID
Add article
Update article
Delete article
```

The service uses:

```java
private final ArticleRepository articleRepository;
```

and communicates with PostgreSQL indirectly through the repository.

---

# 22. Current Data Flow

For example, when creating an article:

```text
Postman
   ↓
POST /api/articles
   ↓
ArticleController
   ↓
ArticleService
   ↓
ArticleRepository
   ↓
JPA / Hibernate
   ↓
PostgreSQL
```

When retrieving an article:

```text
Postman
   ↓
GET /api/articles/1
   ↓
ArticleController
   ↓
ArticleService
   ↓
ArticleRepository
   ↓
JPA / Hibernate
   ↓
PostgreSQL
   ↓
Article object
   ↓
JSON response
   ↓
Postman
```

---

# 23. Error Encountered During Development

A PostgreSQL error was previously encountered:

```text
ERROR: relation "articles" does not exist
LINE 2: FROM articles;
SQL state: 42P01
```

The meaning is that PostgreSQL could not find a table named:

```text
articles
```

Possible causes include:

* The table had not yet been created.
* Hibernate had not successfully connected to PostgreSQL.
* The application was connected to a different database.
* The entity was not being detected.
* The schema/table had not been refreshed in the database interface.

The important debugging principle is to first verify that:

```text
Spring Boot starts successfully
        ↓
PostgreSQL connection succeeds
        ↓
Hibernate detects Article
        ↓
articles table exists
```

before troubleshooting the REST API itself.

---

# 24. pgAdmin

pgAdmin is being used to inspect/manage PostgreSQL.

The expected navigation is roughly:

```text
Servers
└── PostgreSQL
    └── Databases
        └── news_ai
            └── Schemas
                └── public
                    └── Tables
                        └── articles
```

The `articles` table should appear after the Spring Boot application successfully starts with the correct JPA configuration.

If the table does not appear, refreshing the Tables section and checking the Spring Boot startup log are important debugging steps.

---

# 25. Current Backend Architecture

The current architecture can be represented as:

```text
                    ┌──────────────┐
                    │   Postman    │
                    └──────┬───────┘
                           │ HTTP
                           ▼
                    ┌──────────────┐
                    │  Controller  │
                    └──────┬───────┘
                           │
                           ▼
                    ┌──────────────┐
                    │   Service    │
                    └──────┬───────┘
                           │
                           ▼
                    ┌──────────────┐
                    │  Repository  │
                    └──────┬───────┘
                           │
                           ▼
                    ┌──────────────┐
                    │ JPA/Hibernate│
                    └──────┬───────┘
                           │ JDBC
                           ▼
                    ┌──────────────┐
                    │ PostgreSQL   │
                    └──────────────┘
```

This forms the foundation for the rest of the application.

---

# 26. Why This Architecture Was Chosen

The architecture provides clear separation of concerns.

### Controller

Handles HTTP requests and responses.

### Service

Contains application logic.

### Repository

Handles data access.

### JPA/Hibernate

Maps Java objects to relational data.

### PostgreSQL

Stores persistent data.

This makes the application easier to:

* Maintain
* Debug
* Test
* Extend
* Work on collaboratively as a team

---

# 27. Technology Decisions Already Made

The project has currently chosen:

```text
Language:
Java

Backend:
Spring Boot

Build:
Maven

Database:
PostgreSQL

ORM / Persistence:
JPA + Hibernate

API:
REST

API Testing:
Postman

Development Environment:
VS Code
```

---

# 28. Technologies Planned for Later

The following technologies/features have been considered but are not yet part of the completed MVP:

```text
RSS / news crawling
Jsoup
ROME
Possibly Playwright for difficult dynamic sites

Spring AI
LLM API
RAG
Embeddings
PGVector
AI chatbot

Search
Semantic search
Keyword search

Security
Spring Security
JWT / authentication

Frontend
News reading interface
Chat interface
```

These should be added only after the database/backend foundation is stable.

---

# 29. Planned News Crawler

The crawler will eventually collect articles from reputable news sources.

The conceptual flow is:

```text
News Website / RSS
        ↓
Crawler
        ↓
Extract article information
        ↓
Clean / normalize content
        ↓
Check duplicate
        ↓
Save Article
        ↓
PostgreSQL
```

The system should collect information such as:

```text
title
description
content
source
URL
image URL
published time
crawl time
```

---

# 30. Planned Crawler Technologies

## RSS / Atom

RSS is preferred where a news source provides a feed because it provides structured information and avoids unnecessary HTML scraping.

A Java RSS/Atom parser such as ROME can be considered.

## Jsoup

Jsoup is suitable for extracting content from HTML pages.

It can be used to:

* Download HTML
* Select elements with CSS selectors
* Extract article titles
* Extract paragraphs
* Extract images
* Clean unwanted HTML

## Playwright

Playwright can be introduced only when a website requires browser-like execution because of JavaScript-rendered content.

It should not be the default crawler for every site.

---

# 31. Planned Duplicate Detection

Duplicate detection will be an important part of the crawler.

A basic strategy is:

```text
Incoming article
       ↓
Check URL
       ↓
URL already exists?
     /       \
   YES        NO
   ↓           ↓
Ignore       Save
```

The article URL can therefore become a unique identifier for duplicate prevention.

More advanced duplicate detection may later use:

```text
URL
content hash
canonical URL
similarity
```

---

# 32. Planned Database Improvement: Source

The current Article model stores the source as a String.

For a more complete database design, the system should eventually introduce:

```text
Source
├── id
├── name
├── baseUrl
└── ...
```

Then Article can reference Source:

```text
Source 1 ────────< Article
```

This is a one-to-many relationship.

Example:

```text
VnExpress
   ├── Article A
   ├── Article B
   └── Article C
```

---

# 33. Planned Database Improvement: Category

A Category entity can also be introduced:

```text
Category
├── id
└── name
```

Examples:

```text
AI
Technology
Business
World
Sports
Education
```

Article can then reference a category.

This allows queries such as:

```text
Find all AI articles
Find all Technology articles
Find all Business articles
```

---

# 34. Planned JPA Relationships

After introducing Source and Category, the database would approximately become:

```text
                 ┌──────────────┐
                 │    Source    │
                 ├──────────────┤
                 │ id           │
                 │ name         │
                 │ baseUrl      │
                 └──────┬───────┘
                        │
                       1:N
                        │
                        ▼
                 ┌──────────────┐
                 │   Article    │
                 ├──────────────┤
                 │ id           │
                 │ title        │
                 │ content      │
                 │ url          │
                 │ source_id    │
                 │ category_id  │
                 └──────┬───────┘
                        │
                       N:1
                        │
                        ▼
                 ┌──────────────┐
                 │   Category   │
                 ├──────────────┤
                 │ id           │
                 │ name         │
                 └──────────────┘
```

This is the next major database improvement.

---

# 35. Planned Search System

The system will eventually support normal article search.

For example:

```text
GET /api/articles/search?keyword=AI
```

Traditional keyword search can initially use database queries.

Possible searches:

```text
title contains keyword
description contains keyword
content contains keyword
source = selected source
category = selected category
publishedAt within date range
```

---

# 36. Planned AI Assistant

The AI chatbot is one of the major modern features of the project.

The preferred architecture is not to build/train an LLM from scratch.

Instead, the project can use an existing LLM through an API or compatible model service.

Spring AI can then be used to integrate the model with the Java backend.

The architecture is expected to look like:

```text
User
 ↓
Chat API
 ↓
Chat Service
 ↓
Search / Retrieval
 ↓
Relevant news articles
 ↓
Prompt + Context
 ↓
LLM
 ↓
Answer
```

---

# 37. Planned RAG Architecture

The recommended AI architecture is **Retrieval-Augmented Generation (RAG)**.

The basic ingestion pipeline is:

```text
News Source
    ↓
Crawler
    ↓
Article
    ↓
Clean Article
    ↓
Split into chunks
    ↓
Embedding Model
    ↓
Vector Database
```

The question-answering pipeline is:

```text
User Question
      ↓
Question Embedding
      ↓
Vector Search
      ↓
Relevant Article Chunks
      ↓
LLM
      ↓
Answer + Sources
```

---

# 38. PGVector

Because PostgreSQL is already being used, **pgvector / PGVector** is a natural future extension.

It allows vector embeddings to be stored alongside PostgreSQL data.

This potentially gives the project a structure such as:

```text
PostgreSQL
├── articles
├── sources
├── categories
└── article_embeddings / vector storage
```

This avoids immediately introducing another database system solely for vector search.

---

# 39. Planned AI Features

Potential AI features include:

### AI Article Summary

The system generates a short summary of a long article.

### Ask This Article

A user selects an article and asks:

```text
"What is this article mainly about?"
```

or:

```text
"What are the key arguments?"
```

### Semantic News Search

Instead of searching exact keywords, users can ask:

```text
"Find articles discussing AI replacing entry-level software developers."
```

The system retrieves semantically related articles.

### Multi-source Comparison

Example:

```text
"How are different news outlets reporting this event?"
```

The system retrieves articles from multiple sources and compares them.

### News Question Answering

Example:

```text
"What are the biggest AI stories today?"
```

The AI uses retrieved articles as context.

---

# 40. Important AI Reliability Principle

The chatbot should not simply generate an answer from its general model knowledge.

For news-related questions, the system should prioritize the retrieved database content.

The ideal behavior is:

```text
Retrieve evidence
      ↓
Give evidence to LLM
      ↓
Generate answer
      ↓
Return source links
```

If the retrieved information is insufficient, the assistant should clearly indicate that it cannot reliably answer rather than inventing information.

---

# 41. Planned Source Citations

AI responses should ideally contain article references.

For example:

```text
Answer:

Several recent reports describe increased investment
in generative AI infrastructure.

Sources:
- Article A
- Article B
- Article C
```

This makes the AI feature more useful and easier to verify.

---

# 42. Security — Future

Authentication and authorization have not yet been implemented.

A future version may use:

```text
Spring Security
JWT
User accounts
Roles
```

Possible roles:

```text
USER
ADMIN
```

An administrator may eventually be allowed to:

```text
Add/remove news sources
Trigger crawlers
Delete articles
Manage categories
View crawler errors
```

---

# 43. Future Admin Dashboard

A useful modern feature would be an administrator dashboard.

Possible information:

```text
Number of articles
Number of sources
Latest crawl time
Crawler status
Failed crawls
Articles per category
Articles per source
```

Example:

```text
Crawler Status
-------------------------
VnExpress       ✓
Tuoi Tre        ✓
BBC             ✓
Reuters         ✓

Last crawl:
2026-09-07 10:00
```

---

# 44. Scheduled Crawling

Instead of requiring the user to manually trigger the crawler, Spring scheduling can eventually be used.

Example concept:

```text
Every 15 minutes
        ↓
Run crawler
        ↓
Fetch new articles
        ↓
Check duplicates
        ↓
Store new articles
```

This would make the project behave more like a real news aggregation platform.

---

# 45. Current Development Stage

The project is currently in the:

## Backend + Database Foundation Stage

Completed/established concepts:

```text
✓ Java
✓ Spring Boot
✓ Maven
✓ REST API
✓ Article entity
✓ PostgreSQL
✓ JPA
✓ Hibernate
✓ Repository layer
✓ Service layer
✓ Controller layer
✓ CRUD API design
✓ Postman API testing
✓ Database configuration
```

---

# 46. Not Yet Completed

The following should NOT currently be considered finished:

```text
✗ Production-ready error handling
✗ Advanced validation
✗ Source entity
✗ Category entity
✗ News crawler
✗ Duplicate detection system
✗ Scheduled crawling
✗ Advanced search
✗ Vector database
✗ RAG
✗ AI chatbot
✗ User authentication
✗ Frontend
✗ Admin dashboard
```

These are future development stages.

---

# 47. Recommended Development Order

The project should continue in this order:

```text
STEP 1
PostgreSQL + JPA foundation
        ↓
STEP 2
Improve Article entity and validation
        ↓
STEP 3
Create Source entity
        ↓
STEP 4
Create Category entity
        ↓
STEP 5
Implement JPA relationships
        ↓
STEP 6
Improve CRUD + HTTP error handling
        ↓
STEP 7
Implement article search
        ↓
STEP 8
Implement RSS / Jsoup crawler
        ↓
STEP 9
Implement duplicate detection
        ↓
STEP 10
Automate scheduled crawling
        ↓
STEP 11
Integrate Spring AI
        ↓
STEP 12
Add embeddings + PGVector
        ↓
STEP 13
Implement RAG chatbot
        ↓
STEP 14
Add source citations
        ↓
STEP 15
Frontend
        ↓
STEP 16
Security / Admin / final polish
```

---

# 48. Current Technical Goal

The immediate goal is **not yet AI**.

The immediate goal is to make the database model reliable enough that the crawler can safely insert hundreds or thousands of articles later.

The next database milestone is:

```text
Article
   +
Source
   +
Category
   +
JPA relationships
   +
validation
   +
duplicate URL protection
   +
search queries
```

Once this is stable, the crawler can be added without redesigning the entire application.

---

# 49. Overall System Vision

The final system is intended to look like:

```text
                  NEWS WEBSITES
                       │
                       ▼
              ┌─────────────────┐
              │ RSS / Crawler   │
              │ Jsoup / etc.    │
              └────────┬────────┘
                       │
                       ▼
              ┌─────────────────┐
              │ Data Cleaning   │
              │ Deduplication   │
              └────────┬────────┘
                       │
                       ▼
              ┌─────────────────┐
              │  PostgreSQL     │
              │                 │
              │ Articles        │
              │ Sources         │
              │ Categories      │
              │ Vector Data     │
              └────────┬────────┘
                       │
          ┌────────────┴────────────┐
          │                         │
          ▼                         ▼
   Normal Search              AI / RAG
          │                         │
          │                  ┌──────┴──────┐
          │                  │ Spring AI  │
          │                  │     +      │
          │                  │    LLM     │
          │                  └──────┬─────┘
          │                         │
          └────────────┬────────────┘
                       ▼
                Frontend / User
```

---

# 50. Summary

The project has already established the most important backend foundation:

```text
Java
  +
Spring Boot
  +
REST API
  +
Spring Data JPA
  +
Hibernate
  +
PostgreSQL
  +
Postman
```

The `Article` entity is the first persistent data model, and the backend follows a standard layered architecture:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
JPA / Hibernate
    ↓
PostgreSQL
```

This foundation is intentionally being completed before introducing crawling and AI.

The next major implementation task is to improve the database model by introducing:

```text
Source
Category
Article relationships
Validation
Duplicate URL protection
Search
```

After that, the crawler can safely populate the database, and the AI/RAG system can be built on top of the collected news data.

The final project will therefore combine:

```text
Java OOP
+
Spring Boot
+
Relational Database
+
Web Crawling
+
Search
+
AI
+
RAG
```

giving the project both the traditional backend/database components expected from a university project and modern AI-oriented functionality.
