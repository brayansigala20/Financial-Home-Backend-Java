# Isolated Flyway tests

This setup is independent of `../docker-compose.yml` and its override. Do not combine those files with this test configuration using `-f`.

The regular Compose configuration uses application services and persistent database storage. These tests use a separate Compose file and project, PostgreSQL 15, and Maven with JDK 21. There are no fixed container names, published ports, repository mounts, or persistent volumes. PostgreSQL stores its data in tmpfs. Trust authentication is exclusively for this temporary database on the test network. Do not modify regular application secrets or settings.

Maven receives only the `TEST_DB_*` connection variables from the test Compose file. The `db-test` profile enables Flyway, keeps Hibernate in `validate` mode, and disables Flyway clean. It runs `PostgresqlMigrationTests`, `EntityPersistenceTests`, and the application context test. Checks cover tables, foreign keys, constraints, migration validation, idempotence, and persistence of all ten entities. Entity tests use transactional rollback and also exercise composite foreign keys and partial unique indexes through JPA writes.

V1 now uses English identifiers and values. Always start a fresh disposable test project; an old test database contains the previous schema and migration checksum.

## Bash commands

Run these commands only after approval to execute test migrations and download any required images or Maven dependencies, from a terminal where Docker works.

```bash
cd /home/brayan/Workspace/Financial-Home-Infra/Financial-Home-Backend-Java

# A new project for each run avoids reusing an earlier test database.
export FLYWAY_TEST_PROJECT="financial-flyway-test-$(date +%Y%m%d%H%M%S)-$$"

docker compose -f compose.flyway-test.yaml -p "$FLYWAY_TEST_PROJECT" config --quiet

if docker compose -f compose.flyway-test.yaml -p "$FLYWAY_TEST_PROJECT" up \
  --build --abort-on-container-exit --exit-code-from migration-tests; then
  FLYWAY_TEST_EXIT=0
else
  FLYWAY_TEST_EXIT=$?
fi
printf 'Test exit code: %s\n' "$FLYWAY_TEST_EXIT"

# Save reports before removing test containers.
export FLYWAY_TEST_RESULTS="/tmp/$FLYWAY_TEST_PROJECT-results"
mkdir -p "$FLYWAY_TEST_RESULTS"
docker compose -f compose.flyway-test.yaml -p "$FLYWAY_TEST_PROJECT" logs \
  --no-color > "$FLYWAY_TEST_RESULTS/compose.log"
docker compose -f compose.flyway-test.yaml -p "$FLYWAY_TEST_PROJECT" cp \
  migration-tests:/workspace/target/surefire-reports "$FLYWAY_TEST_RESULTS/"
printf 'Results: %s\n' "$FLYWAY_TEST_RESULTS"

# Remove only this test project's containers and network.
docker compose -f compose.flyway-test.yaml -p "$FLYWAY_TEST_PROJECT" down
```

Use the same terminal to preserve the variables. If `config --quiet` fails, stop before running `up`. If the build or Maven fails before Surefire starts, `surefire-reports` may not exist; preserve `compose.log` and the build output.

Do not use `down -v`, the regular Compose configuration, or application project names. Stopping PostgreSQL discards the temporary database; copied reports remain in `/tmp` until that directory is cleaned.

Review the exit code, `Tests run`, `BUILD SUCCESS` or `BUILD FAILURE`, and report errors. Exit code zero confirms success for the selected classes. Inspect failures before changing the migration or tests.
