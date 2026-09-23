# News AI — Current Status Overview

**Project type:** Java Spring Boot News Aggregation + AI Assistant
**Purpose:** Build a news-reading platform that collects articles from external
sources, stores them in a relational database, exposes a REST API, and later
adds AI-powered search and question answering.

**Status date:** 2026-09-23

This document describes **what has actually been done so far and what has not**
by reading the real code under `src/main`. It is meant to update the planning
document `overview.md`, which describes the target design. Anything in this
file is verified against the current source code; anything listed as *not 
done* does not exist in `src/main` yet.

---

# 1. Project Overview

The project is a **Spring Boot monolithic backend** that manages news
`Article`, `Source`, and `Category` records through a layered architecture:

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
it can start, connect to PostgreSQL, perform full CRUD on articles, sources,
and categories through REST endpoints, and return consistent JSON error
responses (404/400/409) through a shared error handler. Article search
(paginated, DTO-based) and feed crawling (upsert-style) are implemented, a
React frontend scaffold exists under `frontend/`, and AI features are planned
but not yet implemented.

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
    │               │   ├── ArticleSearchItem.java
    │               │   ├── ArticleSearchResponse.java
    │               │   ├── ArticleCrawlOutcome.java
    │               │   ├── ArticleCrawlStatus.java
    │               │   ├── CategorySource.java
    │               │   └── old-ArticleService.java   ← 100% commented out
    │               │
    │               ├── source/
    │               │   ├── Source.java
    │               │   ├── SourceRepository.java
    │               │   ├── SourceService.java
    │               │   └── SourceController.java
    │               │
    │               ├── category/
    │               │   ├── Category.java
    │               │   ├── CategoryRepository.java
    │               │   ├── CategoryService.java
    │               │   └── CategoryController.java
    │               │
    │               ├── error/
    │               │   ├── ApiError.java
    │               │   ├── ResourceNotFoundException.java
    │               │   └── GlobalExceptionHandler.java
    │               │
    │               ├── crawler/
    │               │   ├── RssCrawlerService.java
    │               │   ├── RssFeedFetcher.java
    │               │   ├── FeedItemMapper.java
    │               │   ├── ArticleUrlNormalizer.java
    │               │   ├── CrawlController.java
    │               │   ├── CrawlScheduler.java
    │               │   ├── CrawlerConfig.java
    │               │   ├── CrawlerProperties.java
    │               │   ├── CrawlResult.java
    │               │   ├── FeedParseResult.java
    │               │   ├── RssCrawlException.java
    │               │   └── RssFeedItem.java
    │               │
    │               └── controller/
    │                   └── HelloController.java   ← 100% commented out
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

> Test tree: `src/test/java/.../NewsAiApplicationTests.java`,
> `article/ArticleServiceCrawlUpsertTest.java`, `article/ArticleServiceSearchTest.java`,
> `crawler/ArticleUrlNormalizerTest.java`, `crawler/FeedItemMapperTest.java`,
> `crawler/RssCrawlerServiceTest.java`, `crawler/CrawlControllerTest.java`, and an
> empty `crawler/CrawlerEndToEndTest.java` placeholder.
>
> Other top-level work-in-progress: `database/manual-migrations/` (one-time SQL
> fixes, e.g. BYTEA→TEXT content) and `frontend/` (React 19 + TypeScript + Vite
> scaffold).

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

| Dependency                          | Purpose                                                | Scope   |
| ----------------------------------- | ------------------------------------------------------ | ------- |
| `spring-boot-starter-webmvc`      | REST controllers / Web MVC                             | compile |
| `spring-boot-starter-data-jpa`    | JPA + Hibernate persistence                            | compile |
| `spring-boot-starter-validation`  | Bean validation (`@Valid`, `@NotBlank`, `@Size`) | compile |
| `com.rometools:rome:2.1.0`        | RSS/Atom feed parsing for the news crawler             | compile |
| `org.postgresql:postgresql`       | PostgreSQL JDBC driver                                 | runtime |
| `spring-boot-starter-webmvc-test` | Testing utilities                                      | test    |

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

A former sanity controller in a separate `controller` package used to verify
the server boots early on (`/hello`, `/about`, `/api/status`). It is currently
**100% commented out** (dead code) — replaced by the real feature controllers
and the `/api/crawl` endpoints; the `controller/` package no longer holds any
active beans.

---

# 6. Feature Package Layout

The project uses the **feature-package style**: each feature keeps its own
Controller, Service, Repository, and entity together:

```text
com.newsai.news_ai.article
com.newsai.news_ai.source
com.newsai.news_ai.category
```

Shared cross-cutting code lives in its own package:

```text
com.newsai.news_ai.error    ← shared error handling (ApiError, exceptions, advice)
```

Future features (e.g. an `ai/` package) should follow the same pattern; the
`crawler/` package already does.

---

# 7. Article Feature Package (`article/`)

```text
com.newsai.news_ai.article
├── Article.java
├── ArticleRepository.java
├── ArticleService.java
└── ArticleController.java
```

## 7.1 `Article.java` — The Entity

```java
@Entity
@Table(name = "articles")
public class Article { ... }
```

Mapped to the PostgreSQL table `articles`.

### 7.1.1 Fields

| Field              | Java type          | Mapping / constraints                                                                        |
| ------------------ | ------------------ | -------------------------------------------------------------------------------------------- |
| `id`             | `Long`           | `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)`                                  |
| `title`          | `String`         | `@NotBlank @Size(max = 500)`, `@Column(nullable = false, length = 500)`                  |
| `description`    | `String`         | `@Column(columnDefinition = "TEXT")`                                                       |
| `content`        | `String`         | `@Column(columnDefinition = "TEXT")`                                                       |
| `source`         | `Source`         | `@ManyToOne @JoinColumn(name = "source_id", nullable = false)`                             |
| `category`       | `Category`       | `@ManyToOne @JoinColumn(name = "category_id")`                                             |
| `url`            | `String`         | `@NotBlank @Size(max = 2048)`, `@Column(nullable = false, unique = true, length = 2048)` |
| `imageUrl`       | `String`         | `@Column(length = 2048)`                                                                   |
| `publishedAt`    | `LocalDateTime`  | plain column                                                                                 |
| `crawledAt`      | `LocalDateTime`  | plain column (first crawl time, preserved across refreshes)                                  |
| `canonicalUrl`   | `String`         | `@Column(length = 2048)`, **unique** (multiple NULLs allowed for legacy rows)        |
| `lastCrawledAt`  | `LocalDateTime`  | plain column (advanced on every successful observation)                                      |
| `updatedAt`      | `LocalDateTime`  | plain column (advanced only when crawler-managed content changes)                            |
| `categorySource` | `CategorySource` | `@Enumerated(EnumType.STRING)`, values `MANUAL`/`CRAWLER` (null = never assigned)      |

Notable facts:

* The database generates the ID (`GenerationType.IDENTITY`).
* `title`/`url` cannot be `null` and are validated with `@NotBlank`.
* `url` is **unique** — the original raw publisher link, kept stable across
  refreshes (never replaced by a tracking-parameter variant).
* `canonicalUrl` is **unique** for new rows — the crawler's stable identity
  (`ArticleUrlNormalizer.normalize(url)`); legacy rows have it `NULL` until
  the next successful crawl/refresh backfills it.
* `description`/`content` use PostgreSQL `TEXT`; `imageUrl` is limited to 2048 chars.
* `source` is a real `Source` entity (`@ManyToOne`, required, `source_id` column).
* `category` is an optional `@ManyToOne` to `Category`.
* The class has a full constructor plus getters/setters.

## 7.2 `ArticleRepository.java`

```java
public interface ArticleRepository
        extends JpaRepository<Article, Long>, JpaSpecificationExecutor<Article> {
    List<Article> findBySourceId(Long sourceId);
    List<Article> findByCategoryId(Long categoryId);
    Optional<Article> findByUrl(String url);
    Optional<Article> findByCanonicalUrl(String canonicalUrl);
}
```

Provides built-in operations (`findAll(Specification, Pageable)`, `findById`,
`save`, `deleteById`, `existsById`) plus the query helpers above. Search is
built from dynamic `Specification` predicates + `PageRequest` (the hand-written
JPQL `@Query search(...)` was removed), and `findByCanonicalUrl` backs the
crawler's canonical-URL identity lookup.

## 7.3 `ArticleService.java`

Annotated `@Service`, constructor injection. Behaviors:

| Method                          | Behavior                                                                                                                                                                                          |
| ------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `getAllArticles()`            | `findAll()` → all articles                                                                                                                                                                     |
| `getArticleById(id)`          | `findById(id)` or throws `ResourceNotFoundException` → **404**                                                                                                                         |
| `addArticle(article)`         | `@Transactional`; forces `id = null`, sets `crawledAt = now`, resolves `source`/`category` by id (throws **404** if missing); a supplied category becomes `MANUAL`-owned, saves |
| `updateArticle(id, article)`  | `@Transactional`; throws **404** if missing; copies all fields except `id`/`crawledAt`; resolves references; a supplied category becomes `MANUAL`-owned, saves                      |
| `upsertFromCrawl(candidate)`  | Dedicated crawler refresh; identity is `canonicalUrl` (legacy rows matched by raw `url` and backfilled); creates or merges, returns `ArticleCrawlOutcome(status, article)`                   |
| `deleteArticle(id)`           | throws **404** if missing, otherwise deletes                                                                                                                                               |
| `getArticlesBySourceId(id)`   | repository query                                                                                                                                                                                  |
| `getArticlesByCategoryId(id)` | repository query                                                                                                                                                                                  |
| `getArticleByUrl(url)`        | `findByUrl(url)` or throws **404**                                                                                                                                                        |
| `searchArticles(keyword, sourceId, categoryId, publishedFrom, publishedTo, page, size)` | Optional filters; validates `page >= 0`, `1 <= size <= 50`, `from <= to`; builds a `Specification` (LIKE-escaped, case-insensitive), sorts `publishedAt DESC, id DESC`, returns a paged `ArticleSearchResponse` of `ArticleSearchItem` DTOs (no full content) |

Notes:

* `crawledAt` is set automatically only on **create**.
* No `null`/boolean error returns anymore — every not-found case throws the
  custom exception and the client receives a proper HTTP status code.

## 7.4 `ArticleController.java`

```java
@RestController
@RequestMapping("/api/articles")
public class ArticleController { ... }
```

| Method | Endpoint                                | Success response            | Failure response     |
| ------ | --------------------------------------- | --------------------------- | -------------------- |
| GET    | `/api/articles`                       | JSON array                  | —                   |
| GET    | `/api/articles/search?keyword=...&page=0&size=20` | JSON object (`ArticleSearchResponse`) | 400 invalid page/size/date range |
| GET    | `/api/articles/{id}`                  | JSON object                 | 404 ApiError         |
| GET    | `/api/articles/source/{sourceId}`     | JSON array                  | —                   |
| GET    | `/api/articles/category/{categoryId}` | JSON array                  | —                   |
| GET    | `/api/articles/by-url?url=...`        | JSON object                 | 404 ApiError         |
| POST   | `/api/articles`                       | JSON object (saved article) | 400/404/409 ApiError |
| PUT    | `/api/articles/{id}`                  | JSON object                 | 400/404/409 ApiError |
| DELETE | `/api/articles/{id}`                  | `204 No Content`          | 404 ApiError         |

All write endpoints use `@Valid` so bean-validation failures are mapped to
**400** with field-error details.

Example JSON body for `POST /api/articles`:

```json
{
  "title": "Java and AI",
  "description": "Testing PostgreSQL",
  "content": "This article is stored in PostgreSQL.",
  "source": { "id": 1 },
  "category": { "id": 2 },
  "url": "https://example.com/java-ai",
  "imageUrl": "https://example.com/java-ai.jpg",
  "publishedAt": "2026-09-07T10:00:00"
}
```

The client must **not** send `id` (forced to `null` in the service) and
`crawledAt` is set automatically.

---

# 8. Source Feature Package (`source/`)

```text
com.newsai.news_ai.source
├── Source.java
├── SourceRepository.java
├── SourceService.java
└── SourceController.java
```

## 8.1 `Source.java` — The Entity

```java
@Entity
@Table(name = "sources")
public class Source { ... }
```

| Field       | Java type  | Mapping / constraints                                                                        |
| ----------- | ---------- | -------------------------------------------------------------------------------------------- |
| `id`      | `Long`   | `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)`                                  |
| `name`    | `String` | `@NotBlank @Size(max = 100)`, `@Column(nullable = false, unique = true, length = 100)`   |
| `baseUrl` | `String` | `@NotBlank @Size(max = 2048)`, `@Column(nullable = false, unique = true, length = 2048)` |
| `rssUrl`  | `String` | `@Column(length = 2048)` — optional RSS feed for the future crawler                       |

## 8.2 `SourceService` & `SourceController`

CRUD mirrors the Article feature: missing resources throw
`ResourceNotFoundException` (**404**), `updateSource` is `@Transactional`, and
DELETE returns `204 No Content`.

| Method | Endpoint              | Success response   | Failure response     |
| ------ | --------------------- | ------------------ | -------------------- |
| GET    | `/api/sources`      | JSON array         | —                   |
| GET    | `/api/sources/{id}` | JSON object        | 404 ApiError         |
| POST   | `/api/sources`      | JSON object        | 400/409 ApiError     |
| PUT    | `/api/sources/{id}` | JSON object        | 400/404/409 ApiError |
| DELETE | `/api/sources/{id}` | `204 No Content` | 404 ApiError         |

---

# 9. Category Feature Package (`category/`)

```text
com.newsai.news_ai.category
├── Category.java
├── CategoryRepository.java
├── CategoryService.java
└── CategoryController.java
```

## 9.1 `Category.java` — The Entity

```java
@Entity
@Table(name = "categories")
public class Category { ... }
```

| Field    | Java type  | Mapping / constraints                                                                      |
| -------- | ---------- | ------------------------------------------------------------------------------------------ |
| `id`   | `Long`   | `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)`                                |
| `name` | `String` | `@NotBlank @Size(max = 100)`, `@Column(nullable = false, unique = true, length = 100)` |

## 9.2 `CategoryService` & `CategoryController`

Same CRUD pattern: 404 on missing resources, `@Transactional` update, DELETE →
`204 No Content`.

| Method | Endpoint                 | Success response   | Failure response     |
| ------ | ------------------------ | ------------------ | -------------------- |
| GET    | `/api/categories`      | JSON array         | —                   |
| GET    | `/api/categories/{id}` | JSON object        | 404 ApiError         |
| POST   | `/api/categories`      | JSON object        | 400/409 ApiError     |
| PUT    | `/api/categories/{id}` | JSON object        | 400/404/409 ApiError |
| DELETE | `/api/categories/{id}` | `204 No Content` | 404 ApiError         |

---

# 10. Configuration & Database (`application.properties`)

The actual configuration file contains:

```properties
spring.application.name=news-ai

spring.datasource.url=jdbc:postgresql://localhost:5728/news_ai
spring.datasource.username=postgres
spring.datasource.password=${DB_PASSWORD}

spring.jpa.hibernate.ddl-auto=update

spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# ------------------
# RSS crawler config
# ------------------
crawler.connect-timeout=10s
crawler.read-timeout=15s
crawler.user-agent=news-ai-crawler/0.1
crawler.max-items-per-feed=200
crawler.scheduling.enabled=false
crawler.scheduling.fixed-delay=PT1H
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
| Crawler        | `crawler.*` block: timeouts `10s`/`15s`, UA `news-ai-crawler/0.1`, max `200` items/feed, scheduling **off** |

Because `ddl-auto=update`, the `articles`, `sources`, and `categories` tables
are created automatically from the entities when the application first
connects.

To start the app on Windows, the password must first be available in the
environment:

```powershell
$env:DB_PASSWORD="YOUR_POSTGRES_PASSWORD"
.\mvnw.cmd spring-boot:run
```

---

# 11. Shared Error Handling (`error/`)

Every controller is protected by one central `@RestControllerAdvice`
(`GlobalExceptionHandler`). All error responses share the same JSON shape:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Article not found with id: 5",
  "timestamp": "2026-09-15T10:00:00",
  "path": "/api/articles/5"
}
```

| Situation                            | Handler                             | HTTP status                                           |
| ------------------------------------ | ----------------------------------- | ----------------------------------------------------- |
| Resource not found (GET/PUT/DELETE)  | `ResourceNotFoundException`       | 404                                                   |
| Bean-validation failure (`@Valid`) | `MethodArgumentNotValidException` | 400 (with field errors)                               |
| Duplicate unique value (DB)          | `DataIntegrityViolationException` | 409                                                   |
| Malformed JSON body                  | `HttpMessageNotReadableException` | 400                                                   |
| Unknown endpoint                     | `NoResourceFoundException`        | 404                                                   |
| Any other unhandled exception        | catch-all`Exception`              | 500 (generic message; stack trace logged server-side) |

Components:

* `ApiError.java` — immutable Java *record* used as the response body.
* `ResourceNotFoundException.java` — custom exception thrown by the services.
* `GlobalExceptionHandler.java` — one `@ExceptionHandler` method per case above.

This replaces the old `null`/boolean error style: nothing in the API returns
`200 OK` with an empty body for a missing resource anymore.

---

# 12. News Crawler & Refresh Behavior

The `crawler/` package ingests feeds and refreshes stored articles in place.
Pipeline:

```text
CrawlController (POST /api/crawl, POST /api/crawl/sources/{id})
   -> RssCrawlerService.crawlSource(Source)
       -> RssFeedFetcher.fetch(rssUrl)       (RestClient, redirects, UA)
       -> FeedItemMapper.parse(...)          (ROME: RSS 2.0 + Atom)
       -> per item: ArticleService.upsertFromCrawl(candidate)
```

The same flow as an architecture diagram — the left side is the calling chain,
`──►` marks data hand-offs between layers:

```text
┌─────────────┐   POST /api/crawl    ┌──────────────────┐
│ Client /    │ ───────────────────► │ CrawlController  │
│ Scheduler   │                      └────────┬─────────┘
└─────────────┘                               │
                                              ▼
┌──────────────────── RssCrawlerService ─────────────────────────────┐
│ SourceRepository.findAll()/findById(id)  ──►  Source              │
│         │                                                        │
│         ▼  (try-with-resources closes the stream)                │
│ RssFeedFetcher.fetch(rssUrl)  ──►  InputStream                   │
│         │                                                        │
│         ▼                                                        │
│ FeedItemMapper.parse(input, source) ──► FeedParseResult          │
│         │                              (items, skippedMalformed) │
│         ▼   per RssFeedItem:                                     │
│ canonicalUrl = ArticleUrlNormalizer.normalize(url) ──(in-run set)│
│         │                                                        │
│         ▼                                                        │
│ ArticleService.upsertFromCrawl(candidate)  ──► ArticleCrawlOutcome│
│   create ── crawlAt/lastCrawledAt/updatedAt = now                │
│   merge  ── diff title/desc/content/image/publishedAt/category   │
│             └─ UPDATED (updatedAt=now) | UNCHANGED (lastCrawledAt)│
│         │                                                        │
│         ▼  per-item counters + errors                            │
│ CrawlResult(fetched, created, updated, unchanged, failed, errors)│
└──────────────────────────────────────────────────────────────────┘
         │
         ▼
 JSON response (200 / 404)     @Scheduled log (if enabled)
```

## Stable identity

* `ArticleUrlNormalizer.normalize(item.url)` produces the stable `canonicalUrl`
  (host lowercased, tracking params dropped, query sorted, fragment removed).
  The raw publisher `url` field is **never** replaced by the canonical variant.
* `Article.canonicalUrl` is unique; multiple `NULL`s are allowed, so legacy rows keep working until a crawl backfills them.
* Existing rows are matched by `canonicalUrl`; rows that predate canonical URLs are matched by their raw `url` and backfilled during the refresh.
* An in-memory `Set` of canonical URLs only prevents double writes **within one run**; cross-run identity is always the persistent lookup, never the set.

## Timestamps

| Field             | Semantics                                                   |
| ----------------- | ----------------------------------------------------------- |
| `crawledAt`     | first discovery; preserved on every refresh                 |
| `lastCrawledAt` | advanced on every successful observation of an item         |
| `updatedAt`     | advanced only when crawler-managed content actually changes |

## Update detection

`upsertFromCrawl` compares title, description, content, imageUrl, publishedAt, and the crawler-eligible category before writing. If nothing differs the row's content is not written again (only `lastCrawledAt` bumps) and the item counts as `unchanged`. Each item is processed in its own transaction: a failing item rolls back alone and is reported as `failed` without aborting the rest of the source. A `DataIntegrityViolationException` from a concurrent create is
retried once by re-reading the row as an existing article.

## Category policy

* `CategorySource.MANUAL` — set by admin CRUD (`addArticle`/`updateArticle` when a category is supplied); the crawler never overwrites it.
* `CategorySource.CRAWLER` — set when the crawler assigns a canonical category.
* The crawler changes a category only when it currently owns the assignment
  (`CRAWLER`) or the article has none. A feed with no category/mapping **never clears** an existing category, and publisher labels never auto-create `categories` rows — only exact matches against existing canonical categories are used.
* Extraction (`FeedItemMapper`): entry-level `<category>` (Atom `term`,
  falling back to `label`) plus Dublin Core `<dc:subject>` values; channel/ feed-level metadata is consulted only when the entry declares nothing.

## Result contract

`CrawlResult(sourceId, sourceName, fetched, created, updated, unchanged, failed, errors)` — every fetched item is classified as `created`, `updated`,
`unchanged`, or `failed`; per-item error messages are accumulated in `errors`
instead of aborting the crawl.

---

# 13. What Has Been Done So Far

Verified against the code, the following are **implemented**:

```text
✓ Spring Boot application skeleton (NewsAiApplication)
✓ Maven project + Maven Wrapper
✓ Java 25 / Spring Boot 4.1.1 build
✓ REST layer (spring-boot-starter-webmvc) + @RestController
✓ Feature package structure (article/, source/, category/, error/)
✓ HelloController sanity endpoints (/hello, /about, /api/status)
✓ Article entity (title, description, content, source, category, url,
    imageUrl, publishedAt, crawledAt, canonicalUrl, lastCrawledAt,
    updatedAt, categorySource) mapped to table "articles"
✓ Source entity (id, name, baseUrl, rssUrl) mapped to table "sources"
✓ Category entity (id, name) mapped to table "categories"
✓ JPA relationships: Article → Source (required), Article → Category (optional)
✓ Unique URL constraint on Article.url (duplicate-URL protection)
✓ Unique canonicalUrl constraint (nullable for legacy rows)
✓ News crawler (ROME): RSS 2.0 + Atom fetch/parse, per-item
    create/update/unchanged upserts against a canonical URL
✓ Crawler refresh timestamps: crawledAt (first seen), lastCrawledAt (every
    observation), updatedAt (only when content actually changed)
✓ Category ownership (CategorySource MANUAL/CRAWLER): manual categories are
    never overwritten and a missing feed category never clears an assignment
✓ Category extraction: RSS <category>, Atom term/label, Dublin Core
    <dc:subject>, entry-over-channel fallback
✓ Crawl result contract: fetched/created/updated/unchanged/failed + per-item
    errors (POST /api/crawl, POST /api/crawl/sources/{id})
✓ Concurrent-create race retried once before an item is marked failed
✓ Spring Data JPA repositories
      (findBySourceId / findByCategoryId / findByUrl / findByCanonicalUrl,
       plus JpaSpecificationExecutor for paged search)
✓ Bean validation (spring-boot-starter-validation + @NotBlank/@Size + @Valid)
✓ Centralized error handling (ApiError + ResourceNotFoundException + GlobalExceptionHandler)
      404 → missing resource | 400 → invalid input | 409 → duplicate value
✓ DELETE endpoints return 204 No Content (404 when missing)
✓ @Transactional on multi-step create/update logic
✓ Full CRUD for Articles, Sources, Categories
      /api/articles, /api/sources, /api/categories
      + /api/articles/source/{id}, /api/articles/category/{id}, /api/articles/by-url
✓ PostgreSQL driver wired via JDBC + environment-variable password
✓ ddl-auto=update for automatic schema generation
✓ old-ArticleService.java superseded (commented-out in-memory prototype retained)
✓ Paginated article search: GET /api/articles/search?keyword&sourceId&categoryId&publishedFrom&publishedTo&page&size
      page>=0, 1<=size<=50, from<=to; dynamic Specification predicates (LIKE escaped)
✓ Search response DTOs: ArticleSearchResponse {items, page, size, totalElements,
      totalPages} over ArticleSearchItem {id, title, description, imageUrl,
      publishedAt, source{id,name}, category{id,name}}
✓ Automated unit tests: crawler orchestration, feed mapping (RSS/Atom/DC),
      upsert/refresh semantics, URL normalization, crawl controller JSON,
      search pagination + DTO mapping (src/test/java/...)
✓ mockwebserver (com.squareup.okhttp3) test dependency for HTTP-level crawl tests
✓ One-time DB migration script database/manual-migrations/001-articles-content-bytea-to-text.sql
✓ Frontend scaffold frontend/ (React 19 + TypeScript + Vite; src/api/articles.ts,
      src/pages/NewsPage.tsx) — early WIP
```

---

# 14. What Has NOT Been Done Yet

None of the following currently exist in `src/main`:

```text
✗ Pagination on plain list endpoints (GET /api/articles, /api/sources,
      /api/categories return all rows; only search is paged)
✗ HTML scraping (Jsoup) / Playwright — feeds only, no HTML extraction
✗ Content-similarity / content-hash duplicate detection (canonical URL is
      implemented; ML similarity is not)
✗ Scheduled crawling is off by default (crawler.scheduling.enabled=false;
      manual POST /api/crawl works)
✗ Spring AI integration, LLM calls, embeddings
✗ Vector storage / PGVector
✗ RAG-based chatbot
✗ AI features: summarization, ask-this-article, semantic search, multi-source comparison
✗ Source citations in AI answers
✗ Security: Spring Security, JWT, user accounts, roles
✗ Frontend features — only a Vite/React scaffold exists (frontend/); no
      completed reader UI yet
✗ Admin dashboard / crawler monitoring
✗ Tests for the error handler, and a filled-in HTTP end-to-end crawler test
      (CrawlerEndToEndTest.java is currently an empty placeholder)
```

---

# 15. Known Limitations in the Current Code

These are directly observable from the current sources:

1. Crawler duplicate detection is canonical-URL based (tracking params are stripped); redirects, AMP pages, and the same story on different URLs still need content-hash or similarity-based detection. Rows without a canonical URL are backfilled on the next successful crawl.
2. Plain list endpoints (`GET /api/articles`, etc.) return all rows at once;
   only `/api/articles/search` is paginated (`page`/`size`).
3. The 500 responses intentionally hide internal details from clients (details are logged server-side); there is no error-code/i18n layer yet.
4. `old-ArticleService.java` is dead code kept as an in-memory prototype reference.
5. `HelloController.java` is fully commented-out dead code kept in the
   layer-style `controller/` package.
6. No automated tests exercise the error handler (404/400/409) yet, and
   `CrawlerEndToEndTest.java` is still an empty placeholder.

---

# 16. Recommended Next Steps

Progress so far:

```text
STEP 1  Stabilize the Article CRUD foundation        ✓ done
STEP 2  HTTP error handling (404/400/409) + @Valid   ✓ done
STEP 3  Unique constraint on Article.url             ✓ done
STEP 4  Source entity (id, name, baseUrl, rssUrl)    ✓ done
STEP 5  Category entity (id, name)                   ✓ done
STEP 6  Article ↔ Source / Article ↔ Category        ✓ done
STEP 7  Article search (paginated, DTO response, dynamic Specification)   ✓ done
STEP 8  RSS crawler + canonical-URL refresh
        (create/update/unchanged, category ownership)                    ✓ done
STEP 9  Scheduled crawling (disabled by default)                         ✓ scaffold
STEP 10 Spring AI + embeddings + PGVector
STEP 11 RAG chatbot with source citations
STEP 12 Frontend (Vite/React scaffold started, UI not complete);
       security / admin dashboard pending
```

---

# 17. Summary

The project now delivers a solid **Spring Boot + JPA + PostgreSQL** backend
with:

```text
- full CRUD REST API for articles, sources, and categories
- proper HTTP error handling (404/400/409) with a consistent JSON error body
- bean validation on all write endpoints
- Article ↔ Source / Article ↔ Category JPA relationships
- unique URL protection for the future crawler
- canonical-URL identity + upsert-style crawler refresh (create/update/unchanged)
- paginated search API with DTO responses (ArticleSearchResponse/ArticleSearchItem)
- automated unit tests for crawler, feed mapping, refresh upsert, search, controller
- React + TypeScript + Vite frontend scaffold (frontend/)
- one-time DB migration scripts (database/manual-migrations/)
- automatic crawledAt timestamp on create
- automatic table creation via ddl-auto=update
- feature-based package layout (article/, source/, category/, error/)
```

The backend/database foundation is complete: article search (paginated,
DTO-based) and the feed-crawler milestones are done, a React frontend scaffold
exists, and a suite of unit tests covers the crawler/search/controller layers.
The next planned milestone is the **AI/RAG** layer, to be built on top of the
collected news data.
