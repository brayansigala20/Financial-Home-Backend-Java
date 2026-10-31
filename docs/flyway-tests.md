# Pruebas aisladas de Flyway

Esta configuración es independiente de `../docker-compose.yml` y de su
override. No ejecutarla combinando esos archivos con `-f`.

El Compose habitual define `financial_db` y `financial_backend_java` y el
archivo base monta `/mnt/data/postgres_data`. El override cambia el servicio
habitual `db` para usar tmpfs; no crea un entorno de pruebas independiente.
Por eso estas pruebas usan su propio archivo y proyecto Compose.

El entorno de pruebas usa PostgreSQL 15 (como el Compose principal) y Maven
con JDK 21 (como la compilación del backend). No tiene nombres de contenedor
fijos, puertos publicados, montajes del repositorio ni volúmenes persistentes.
PostgreSQL guarda sus datos en tmpfs. La autenticación `trust` corresponde
exclusivamente a esta base temporal accesible desde la red de pruebas.
No modificar los secretos ni la configuración de la aplicación habitual.

Maven recibe únicamente las variables `TEST_DB_*` de este archivo. El perfil
`db-test` activa Flyway y mantiene Hibernate en `validate` y Flyway clean
deshabilitado. Se ejecutan `PostgresqlMigrationTests` y la prueba de carga
del contexto con ese perfil. Las pruebas comprueban tablas, claves foráneas,
restricciones, validación de migraciones e idempotencia.

## Comandos para Bash en Arch Linux

Ejecutar desde una terminal donde Docker funcione. La primera ejecución
puede descargar imágenes y dependencias Maven dentro del entorno Docker.

```bash
cd /home/brayan/Workspace/Financial-Home-Infra/Financial-Home-Backend-Java

# Un proyecto nuevo por ejecución evita reutilizar una base de pruebas anterior.
export FLYWAY_TEST_PROJECT="financial-flyway-test-$(date +%Y%m%d%H%M%S)-$$"

docker compose -f compose.flyway-test.yaml -p "$FLYWAY_TEST_PROJECT" config --quiet

if docker compose -f compose.flyway-test.yaml -p "$FLYWAY_TEST_PROJECT" up \
  --build --abort-on-container-exit --exit-code-from migration-tests; then
  FLYWAY_TEST_EXIT=0
else
  FLYWAY_TEST_EXIT=$?
fi
printf 'Código de salida de las pruebas: %s\n' "$FLYWAY_TEST_EXIT"

# Conservar resultados antes de retirar los contenedores de pruebas.
export FLYWAY_TEST_RESULTS="/tmp/$FLYWAY_TEST_PROJECT-results"
mkdir -p "$FLYWAY_TEST_RESULTS"
docker compose -f compose.flyway-test.yaml -p "$FLYWAY_TEST_PROJECT" logs \
  --no-color > "$FLYWAY_TEST_RESULTS/compose.log"
docker compose -f compose.flyway-test.yaml -p "$FLYWAY_TEST_PROJECT" cp \
  migration-tests:/workspace/target/surefire-reports "$FLYWAY_TEST_RESULTS/"
printf 'Resultados: %s\n' "$FLYWAY_TEST_RESULTS"

# Retirar únicamente los contenedores y la red de este proyecto de pruebas.
docker compose -f compose.flyway-test.yaml -p "$FLYWAY_TEST_PROJECT" down
```

Ejecutar los comandos en la misma terminal para conservar las variables.
Si `config --quiet` falla, detenerse antes de ejecutar `up`.
Si falla la construcción o Maven antes de iniciar Surefire, puede no existir
`surefire-reports`; conservar `compose.log` y la salida de la construcción.
No usar `down -v`, el Compose habitual ni nombres de proyectos de la aplicación.
Al detener PostgreSQL se pierde la base temporal; los informes copiados en
`/tmp` permanecen hasta que se limpie ese directorio.

Para revisar el resultado, compartir el código de salida, el resumen
`Tests run` / `BUILD SUCCESS` o `BUILD FAILURE` y los errores de los informes.
Un código cero confirma éxito de las clases seleccionadas. Un fallo requiere
revisar los registros antes de cambiar la migración o las pruebas.
