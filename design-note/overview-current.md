# News AI — Current Status Overview

**Project type:** Java Spring Boot News Aggregation + AI Assistant
**Purpose:** Build a news-reading platform that collects articles from external
sources, stores them in a relational database, exposes a REST API, and later
adds AI-powered search and question answering.

**Status date:** 2026-09-07

This document describes **what has actually been done so far and what has not**
by reading the real code under `src/main`. It is meant to update the planning
document `overview.md`, which describes the target design. Anything in this
file is verified against the current source code; anything listed as *not
done* does not exist in `src/main` yet.

---

# 1. Project Overview

The project is a **Spring Boot monolithic backend** that manages news
`Article` records through a layered architecture:

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

The application is currently at the **backend + database foundation stage**:
it can start, connect to PostgreSQL, and perform full CRUD on articles through
REST endpoints. Crawling, search, and AI features are planned but not yet
implemented.

---

# 2. Current Project Structure (`src/main`)

The structure strictly follows what exists in the source folder today:

```text
src/
└── main/
    ├── java/
    │   └── com/
    │       └── newsai/
    │           └── news_ai/
    │               ├── NewsAiApplication.java
    │               │
    │               ├── article/
    │               │   ├── Article.java
    │               │   ├── ArticleRepository.java
    │               │   ├── ArticleService.java
    │               │   ├── ArticleController.java
    │               │   └── old-ArticleService.java   ← 100% commented out
    │               │
    │               └── controller/
    │                   └── HelloController.java
    │
    └── resources/
        └── application.properties
```

Build files at the project root:

```text
pom.xml
mvnw
mvnw.cmd
```

> Note: `src/test/java/.../NewsAiApplicationTests.java` also exists, but it is
> intentionally out of scope for this document.

---

# 3. Technologies In Use

## 3.1 Java 25

The `pom.xml` declares:

```xml
<java.version>25</java.version>
```

Java is the only language used in the project so far.

## 3.2 Spring Boot 4.1.1

The build inherits from the Spring Boot starter parent:

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.1.1</version>
</parent>
```

Spring Boot provides startup, dependency injection, the embedded web server,
and auto-configuration.

## 3.3 Dependencies Declared in `pom.xml`

| Dependency                          | Purpose                     | Scope   |
| ----------------------------------- | --------------------------- | ------- |
| `spring-boot-starter-webmvc`      | REST controllers / Web MVC  | compile |
| `spring-boot-starter-data-jpa`    | JPA + Hibernate persistence | compile |
| `org.postgresql:postgresql`       | PostgreSQL JDBC driver      | runtime |
| `spring-boot-starter-webmvc-test` | Testing utilities           | test    |

The project uses the modern **WebMVC** starter naming (not the older
`spring-boot-starter-web`).

## 3.4 Maven Wrapper

The project contains the Maven Wrapper (`mvnw`, `mvnw.cmd`) so it can be run
without a separately installed Maven. On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

---

# 4. Application Entry Point — `NewsAiApplication.java`

```java
@SpringBootApplication
public class NewsAiApplication {
    public static void main(String[] args) {
        SpringApplication.run(NewsAiApplication.class, args);
    }
}
```

Located at `com.newsai.news_ai`. It:

* starts the Spring Boot application,
* scans the classpath and creates Spring beans,
* configures the embedded web server.

The source file contains Vietnamese comments explaining this startup flow.

---

# 5. `HelloController.java`

A simple sanity controller in a separate `controller` package, separate from
the article feature package:

| Method | Mapping         | Returns                                                                  |
| ------ | --------------- | ------------------------------------------------------------------------ |
| GET    | `/hello`      | `"Xin Chao Quy Phuoc!"` (plain text)                                   |
| GET    | `/about`      | `"Toi la HQP!"` (plain text)                                           |
| GET    | `/api/status` | JSON:`{ "application": "News AI", "status": "running", "version": 1 }` |

It is used to verify that the server is running during early development.

---

# 6. Article Feature Package (`article/`)

Every class related to an article lives in its own package:

```text
com.newsai.news_ai.article
├── Article.java
├── ArticleRepository.java
├── ArticleService.java
└── ArticleController.java
```

This is the feature-package style (not class-grouped-by-layer), which is
important to follow for future features (e.g. a `crawler/` package, an
`ai/` package).

---

# 7. `Article.java` — The Entity

```java
@Entity
@Table(name = "articles")
public class Article { ... }
```

Mapped to the PostgreSQL table `articles`.

## 7.1 Fields

| Field           | Java type         | JPA mapping                                                 |
| --------------- | ----------------- | ----------------------------------------------------------- |
| `id`          | `Long`          | `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)` |
| `title`       | `String`        | `@Column(nullable = false)`                               |
| `description` | `String`        | `@Column(columnDefinition = "TEXT")`                      |
| `content`     | `String`        | `@Column(columnDefinition = "TEXT")`                      |
| `source`      | `String`        | plain`String` (no constraint)                             |
| `url`         | `String`        | `@Column(length = 2048)`                                  |
| `imageUrl`    | `String`        | `@Column(length = 2048)`                                  |
| `publishedAt` | `LocalDateTime` | plain column                                                |
| `crawledAt`   | `LocalDateTime` | plain column                                                |

Notable facts:

* The database generates the ID (`GenerationType.IDENTITY`).
* `title` cannot be `null` (`nullable = false`).
* `description` and `content` use PostgreSQL `TEXT` for large text.
* `url`/`imageUrl` are limited to 2048 characters but are **not yet unique** —
  duplicate URL protection for the crawler is not implemented yet.
* The class has a full constructor plus getters/setters.
* There are **no JPA relationships** (`@ManyToOne`, etc.) yet — `source` is
  still a plain `String`, not a `Source` entity.

---

# 8. `ArticleRepository.java`

```java
public interface ArticleRepository extends JpaRepository<Article, Long> {
}
```

Provides built-in operations:

```text
findAll()
findById()
save()
deleteById()
existsById()
```

No custom query methods exist yet (no search, no `findByTitleContaining`, etc.).

---

# 9. `ArticleService.java`

Annotated `@Service`. Uses constructor injection:

```java
private final ArticleRepository articleRepository;
```

Behaviors implemented today:

| Method                                | Behavior                                                                                                           |
| ------------------------------------- | ------------------------------------------------------------------------------------------------------------------ |
| `getAllArticles()`                  | `articleRepository.findAll()` → returns all articles                                                            |
| `getArticleById(Long id)`           | `findById(id).orElse(null)` — returns `null` when the article does not exist                                  |
| `addArticle(Article article)`       | forces`id = null`, sets `crawledAt = LocalDateTime.now()`, then saves                                          |
| `updateArticle(id, updatedArticle)` | loads by id; returns`null` if not found; otherwise copies all fields (except `id` and `crawledAt`) and saves |
| `deleteArticle(Long id)`            | returns`false` if not exists; otherwise deletes and returns `true`                                             |

Notes:

* `crawledAt` is set automatically only on **create**.
* There is no `@Transactional`, no custom exceptions, and no `ResponseEntity` —
  errors are represented as `null`/boolean returns.

---

# 10. `ArticleController.java`

```java
@RestController
@RequestMapping("/api/articles")
public class ArticleController { ... }
```

| Method | Endpoint               | Action            | Success response                                 |
| ------ | ---------------------- | ----------------- | ------------------------------------------------ |
| GET    | `/api/articles`      | list all articles | JSON array                                       |
| GET    | `/api/articles/{id}` | get one article   | JSON object (or empty`null` body if not found) |
| POST   | `/api/articles`      | create an article | JSON object of saved article                     |
| PUT    | `/api/articles/{id}` | update an article | JSON object (or empty`null` body if not found) |
| DELETE | `/api/articles/{id}` | delete an article | String message                                   |

Example JSON body for `POST /api/articles`:

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

The client must **not** send `id` (it is forced to `null` in the service) and
`crawledAt` is set automatically.

DELETE returns plain text:

```text
"Article deleted successfully!"   ← when deleted
"Article not found!"              ← when id does not exist
```

Note: a not-found article currently returns HTTP 200 with `null` (or a text
message) instead of a proper `404` — production-grade error handling is still
on the to-do list.

---

# 11. Configuration & Database (`application.properties`)

The actual configuration file contains:

```properties
spring.application.name=news-ai

spring.datasource.url=jdbc:postgresql://localhost:5728/news_ai
spring.datasource.username=postgres
spring.datasource.password=${DB_PASSWORD}

spring.jpa.hibernate.ddl-auto=update

spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

Facts to note (they differ from the older planning doc):

| Setting        | Real value in the project                         |
| -------------- | ------------------------------------------------- |
| Database name  | `news_ai`                                       |
| Host           | `localhost`                                     |
| **Port** | **5728** (not 5432)                         |
| Username       | `postgres`                                      |
| Password       | read from env var`DB_PASSWORD`                  |
| DDL strategy   | `update` (Hibernate creates/updates the schema) |
| SQL logging    | enabled (`show-sql` + `format_sql`)           |

Because `ddl-auto=update`, the `articles` table is expected to be created
automatically from the `Article` entity when the application first connects.

To start the app on Windows, the password must first be available in the
environment:

```powershell
$env:DB_PASSWORD="YOUR_POSTGRES_PASSWORD"
.\mvnw.cmd spring-boot:run
```

---

# 12. What Has Been Done So Far

Verified against the code, the following are **implemented**:

```text
✓ Spring Boot application skeleton (NewsAiApplication)
✓ Maven project + Maven Wrapper
✓ Java 25 / Spring Boot 4.1.1 build
✓ REST layer (spring-boot-starter-webmvc) + @RestController
✓ Feature package structure under com.newsai.news_ai
✓ HelloController sanity endpoints (/hello, /about, /api/status)
✓ Article entity mapped to table "articles" (9 fields, IDENTITY id, TEXT columns)
✓ JPA + Hibernate persistence (spring-boot-starter-data-jpa)
✓ ArticleRepository extends JpaRepository<Article, Long>
✓ ArticleService with basic validation-by-null and crawledAt auto-fill
✓ ArticleController with full CRUD under /api/articles
      GET    /api/articles
      GET    /api/articles/{id}
      POST   /api/articles
      PUT    /api/articles/{id}
      DELETE /api/articles/{id}
✓ PostgreSQL driver wired via JDBC + environment-variable password
✓ ddl-auto=update for automatic schema generation of the "articles" table
✓ In-memory prototype superseded: old-ArticleService.java is fully commented out
```

---

# 13. What Has NOT Been Done Yet

None of the following currently exist in `src/main`:

```text
✗ Production-ready error handling
      (no @ControllerAdvice, no exceptions, not-found returns null/200)
✗ Advanced bean validation (@Valid, @NotBlank, @Email, etc.)
✗ Unique constraint on article URL (duplicate-URL protection for crawler)
✗ Source entity (source is still a plain String on Article)
✗ Category entity
✗ Article ↔ Source / Article ↔ Category JPA relationships
✗ Article search endpoints (e.g. /api/articles/search?keyword=...)
✗ News crawler / RSS parser / Jsoup / ROME / Playwright
✗ Duplicate detection system (URL/content-hash/similarity)
✗ Scheduled crawling (Spring @Scheduled)
✗ Spring AI integration, LLM calls, embeddings
✗ Vector storage / PGVector
✗ RAG-based chatbot
✗ AI features: summarization, ask-this-article, semantic search, multi-source comparison
✗ Source citations in AI answers
✗ Security: Spring Security, JWT, user accounts, roles
✗ Frontend (news reader / chat interface)
✗ Admin dashboard / crawler monitoring
✗ Tests: `src/test` is excluded from this document's scope
```

---

# 14. Known Limitations in the Current Code

These are directly observable from the current sources:

1. `getArticleById(id)` and `updateArticle(id, ...)` return `null` when the
   article is missing — the API answers **HTTP 200 with an empty body** rather
   than `404 Not Found`.
2. `DELETE /api/articles/{id}` returns a String message with HTTP 200 instead
   of a proper status code (`204 No Content` / `404`).
3. `ArticleService` has no `@Transactional`, so multi-operation logic (later
   needed for crawling) is not protected by transactions yet.
4. `url` is not unique, so the same article URL can theoretically be inserted
   twice — a problem the crawler must solve.
5. `old-ArticleService.java` is dead code (fully commented out) kept as a
   reference for how the in-memory list implementation looked before JPA.

---

# 15. Recommended Next Steps

Order suggested by the current state of the code:

```text
STEP 1   Stabilize the Article CRUD foundation   ← we are here
STEP 2   Add HTTP error handling (404/400) + @Valid validation
STEP 3   Add unique constraint on url (seed data / migration)
STEP 4   Create Source entity (id, name, baseUrl)
STEP 5   Create Category entity (id, name)
STEP 6   Wire Article → Source / Article → Category relationships
STEP 7   Implement article search (keyword, source, category, date range)
STEP 8   Implement RSS / Jsoup crawler + duplicate detection
STEP 9   Automate scheduled crawling
STEP 10  Integrate Spring AI + embeddings + PGVector
STEP 11  Implement RAG chatbot with source citations
STEP 12  Frontend, security, admin dashboard
```

---

# 16. Summary

The project has so far delivered a working **Spring Boot + JPA + PostgreSQL**
backend with:

```text
- full CRUD REST API for articles
- automatic crawledAt timestamp on create
- automatic table creation via ddl-auto=update
- a feature-based package layout (article/, controller/)
- a commented-out in-memory prototype showing the migration to JPA
```

The database foundation is the only part that is complete. Everything else —
sources, categories, relationships, search, crawler, deduplication, AI/RAG,
security, and frontend — is still planned work, as listed in section 13.
