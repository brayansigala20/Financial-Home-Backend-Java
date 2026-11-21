# Financial Home Backend

Java / Spring Boot backend in early development. Technical names, database identifiers, domain values, comments, and documentation use English.

## Stack

- Java 21 and Maven.
- Spring Boot 4.0.8, Spring MVC, Spring Data JPA, and Bean Validation.
- PostgreSQL and Flyway with PostgreSQL support.
- Lombok for compile-time getter generation; excluded from the application package.

## Project structure

Application code lives in `src/main/java/com/financialhome/financial_home_backend/`:

- `FinancialHomeBackendApplication.java`: Spring Boot entry point.
- `Accounts/controller/AccountController.java`: exposes `GET /api/accounts`, currently returning `Accounts`.
- `Accounts/service/AccountService.java`: service placeholder.
- `Accounts/repository/AccountRepository.java`: repository placeholder.
- `Household/entity/Household.java`: JPA mapping for the `households` table.

Entities are grouped by module, inside its `entity` layer:

| Module | Entities |
| --- | --- |
| `Users/entity` | `User` |
| `People/entity` | `Person` |
| `Household/entity` | `Household`, `HouseholdMember`, `HouseholdInvitation` |
| `Accounts/entity` | `FinancialAccount`, `HouseholdAccount` |
| `Cards/entity` | `Card` |
| `Transactions/entity` | `FinancialTransaction`, `TransactionEntry` |

The account layers are not connected yet. The household entity maps UUID identifiers and timestamps to PostgreSQL and exposes getters through Lombok. Create a household with `new Household(name)`: the constructor requires a nonblank name of at most 150 Unicode code points and initializes status to `ACTIVE` and the creation timestamp to the current instant. JPA generates the UUID on persistence. The database defaults remain available for direct SQL inserts that omit status and creation time.

All ten entities use generated UUIDs, Lombok getters, protected JPA constructors, and public creation constructors. Relationships are unidirectional and request lazy loading, with no cascade operations. Persist referenced entities first. Money uses `BigDecimal` with precision 18 and scale 2; timestamps use `Instant`.

Creation constructors initialize the SQL defaults: users, households, and cards start `ACTIVE`; invitations start `PENDING`; transactions start `DRAFT`. A null membership access role selects `NO_ACCESS`, and a null account currency selects `MXN`. Financial accounts require exactly one owner and derive `PERSON` or `HOUSEHOLD` from that owner.

SQL remains the source of truth for CHECK constraints, foreign keys, and indexes, including the partial index for pending invitations. The entry's card association uses the card's primary key; PostgreSQL additionally enforces the composite `(card_id, account_id)` foreign key so the card must belong to the entry's account. Hibernate must remain in `validate` mode; entity mappings are not a replacement for the migration.

Creation constructors check required references, while PostgreSQL enforces allowed string values, lengths, dates, and other constraints on flush. Entity getters are for application use; these classes are not API response contracts. Public lifecycle methods for accepting invitations, ending memberships, archiving, or confirming transactions are intentionally not implemented yet. Their nullable lifecycle columns are mapped for reading existing rows.

## Database schema

`src/main/resources/db/migration/V1__initial_schema.sql` defines:

| Table | Purpose |
| --- | --- |
| `users` | Registered users |
| `people` | Personal profiles |
| `households` | Households |
| `household_members` | Membership and access roles |
| `household_invitations` | Invitations for registered users |
| `financial_accounts` | Personal and household accounts |
| `household_accounts` | Accounts linked to households |
| `cards` | Physical and digital cards |
| `financial_transactions` | Financial transaction headers |
| `transaction_entries` | Account entries for transactions |

Database identifiers use snake_case; Java fields use camelCase. Domain values use uppercase English, such as `ACTIVE`, `ARCHIVED`, and `CONFIRMED`. Currency codes such as `MXN` remain unchanged.

V1 was translated before use outside tests. Previously created disposable test databases must be replaced with a fresh isolated test database because the migration checksum and schema have changed. Do not edit an applied migration on a database that must be retained; use a new versioned migration instead.

## Configuration

`src/main/resources/application.properties` reads PostgreSQL connection settings from `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`, with existing fallback values.

- `spring.jpa.hibernate.ddl-auto=validate` checks entity mappings without changing the schema.
- `spring.flyway.enabled=false` disables automatic Flyway migrations in the regular configuration.

The translated schema must be applied with explicit approval before starting the application against a new database. Do not enable automatic migrations against an existing database without reviewing its schema and migration history.

## Verification

`FinancialHomeBackendApplicationTests` checks application context startup. `PostgresqlMigrationTests` checks the ten model tables, foreign keys, constraints, Flyway validation, migration idempotence, household persistence, and household name validation using the isolated `db-test` profile.

`EntityPersistenceTests` persists and reloads every entity and its relationships, including nullable associations, personal accounts, reversals, and populated lifecycle timestamps. It also checks exclusive account ownership, duplicate memberships and user profiles, duplicate pending invitations, card/account consistency, card payment methods, and nonzero amounts. Tests use transactional rollback; the isolated Docker workflow includes this class.

See [isolated Flyway tests](docs/flyway-tests.md) for the Docker test workflow. Running that workflow applies migrations only to its disposable test database and requires approval to execute migrations and download dependencies.

## Household creation

`POST /api/households` accepts `{"name":"Family home"}`. The authentication integration must supply the user's UUID as the servlet principal name. The user must have an active account linked to a person. The endpoint returns HTTP 201 with `id`, `name`, and `status`; the service creates the household and its initial administrator in one transaction. `HouseholdCreationTests` supplies a test principal and exercises the endpoint against the migrated PostgreSQL schema. A real authentication provider still needs to be connected.

## Remaining work

Implement the account service and repository, connect authentication for household creation, and define the rules for household administration.
