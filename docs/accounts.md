# Consulta de cuentas por hogar

`AccountRepository.findAllByOwnerHousehold_Id(UUID householdId)` devuelve las cuentas cuyo propietario es el hogar indicado. Spring Data JPA deriva la consulta de la relación `FinancialAccount.ownerHousehold`. Una cuenta personal vinculada mediante `HouseholdAccount` conserva a su propietario personal y queda fuera del resultado.

La consulta está disponible en la capa de persistencia. Para conectarla a la API, queda definir los roles que podrán consultar las cuentas y los campos que se devolverán.

`AccountRepositoryTests` confirmó que se devuelven las dos cuentas del hogar solicitado y se excluyen una cuenta de otro hogar y una cuenta personal vinculada. La prueba de integración pasó con Flyway V1 en PostgreSQL 18.6 temporal, sin Docker. El servidor de pruebas se detuvo al terminar.
