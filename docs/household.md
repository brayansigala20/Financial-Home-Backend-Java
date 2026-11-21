# Consulta de hogares

| Operación | Respuesta |
| --- | --- |
| `GET /api/households` | `200 OK` con un arreglo de hogares visibles, ordenado por nombre y luego por identificador. Devuelve `[]` si la persona no tiene hogares visibles. |
| `GET /api/households/{id}` | `200 OK` con el hogar visible. Devuelve `404 Not Found` si el hogar no existe o está fuera del alcance de la persona. Un ID con formato UUID inválido devuelve `400 Bad Request`. |

Cada hogar incluye `id`, `name` y `status`. Ambas rutas reutilizan el formato de respuesta de `POST /api/households`.

El controlador obtiene el UUID de usuario del `Principal`. Devuelve `401 Unauthorized` si el principal falta o su nombre no es un UUID válido. Devuelve `403 Forbidden` si el usuario carece de una cuenta activa vinculada a una persona.

Ambas consultas aplican el mismo filtro en el repositorio de membresías. El hogar debe estar en estado `ACTIVE` y la persona debe tener una membresía con `leftAt` nulo y rol `ADMIN` o `MEMBER`. Aplicar el filtro en la consulta evita devolver un hogar por su ID antes de comprobar la membresía.

Se actualizaron `HouseholdController`, `HouseholdService` y `HouseholdMemberRepository`. Las pruebas de integración se ejecutaron con MockMvc y PostgreSQL 18.6 temporal, sin Docker. Pasaron las dos pruebas de `HouseholdReadTests` y las cinco de `HouseholdCreationTests`. Las pruebas de lectura cubren el listado vacío, el orden, los roles autorizados y la exclusión de hogares archivados o ajenos y de membresías terminadas o con `NO_ACCESS`. MockMvc suministra el `Principal` de prueba.
