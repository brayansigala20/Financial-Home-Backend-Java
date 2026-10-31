-- Financial Home: primera migración PostgreSQL / Flyway
-- UUID generados por Spring Boot o la aplicación, fechas UTC mediante timestamptz.

CREATE TABLE usuarios (
    id uuid PRIMARY KEY,
    email varchar(255) NOT NULL UNIQUE,
    password_hash varchar(255),
    estado varchar(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT usuarios_estado_chk CHECK (estado IN ('ACTIVO', 'INACTIVO', 'BLOQUEADO'))
);

CREATE TABLE personas (
    id uuid PRIMARY KEY,
    usuario_id uuid UNIQUE REFERENCES usuarios(id),
    nombre varchar(100) NOT NULL,
    apellido varchar(100),
    sexo varchar(20),
    CONSTRAINT personas_sexo_chk CHECK (sexo IS NULL OR sexo IN ('FEMENINO', 'MASCULINO', 'NO_ESPECIFICADO'))
);

CREATE TABLE hogares (
    id uuid PRIMARY KEY,
    nombre varchar(150) NOT NULL,
    estado varchar(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT hogares_estado_chk CHECK (estado IN ('ACTIVO', 'ARCHIVADO'))
);

CREATE TABLE integrantes_hogar (
    id uuid PRIMARY KEY,
    hogar_id uuid NOT NULL REFERENCES hogares(id),
    persona_id uuid NOT NULL REFERENCES personas(id),
    rol_familiar varchar(30),
    rol_acceso varchar(20) NOT NULL DEFAULT 'SIN_ACCESO',
    fecha_ingreso timestamptz NOT NULL DEFAULT now(),
    fecha_salida timestamptz,
    CONSTRAINT integrantes_hogar_uk UNIQUE (hogar_id, persona_id),
    CONSTRAINT integrantes_hogar_rol_acceso_chk CHECK (rol_acceso IN ('ADMINISTRADOR', 'MIEMBRO', 'SIN_ACCESO')),
    CONSTRAINT integrantes_hogar_rol_familiar_chk CHECK (rol_familiar IS NULL OR rol_familiar IN
      ('PADRE','MADRE','HIJO','HIJA','ABUELO','ABUELA','HERMANO','HERMANA','PAREJA','TUTOR','OTRO')),
    CONSTRAINT integrantes_hogar_fechas_chk CHECK (fecha_salida IS NULL OR fecha_salida >= fecha_ingreso)
);
CREATE INDEX integrantes_hogar_persona_idx ON integrantes_hogar(persona_id);

-- Primera versión: invitaciones solo para usuarios que ya están registrados.
CREATE TABLE invitaciones_hogar (
    id uuid PRIMARY KEY,
    hogar_id uuid NOT NULL REFERENCES hogares(id),
    invitado_usuario_id uuid NOT NULL REFERENCES usuarios(id),
    invitado_por_usuario_id uuid NOT NULL REFERENCES usuarios(id),
    rol_propuesto varchar(20) NOT NULL,
    estado varchar(20) NOT NULL DEFAULT 'PENDIENTE',
    fecha_creacion timestamptz NOT NULL DEFAULT now(),
    fecha_expiracion timestamptz NOT NULL,
    fecha_respuesta timestamptz,
    CONSTRAINT invitaciones_rol_chk CHECK (rol_propuesto IN ('ADMINISTRADOR', 'MIEMBRO')),
    CONSTRAINT invitaciones_estado_chk CHECK (estado IN ('PENDIENTE', 'ACEPTADA', 'RECHAZADA', 'EXPIRADA')),
    CONSTRAINT invitaciones_fechas_chk CHECK (fecha_expiracion > fecha_creacion),
    CONSTRAINT invitaciones_distintos_chk CHECK (invitado_usuario_id <> invitado_por_usuario_id)
);
CREATE UNIQUE INDEX invitaciones_pendientes_uk ON invitaciones_hogar(hogar_id, invitado_usuario_id)
    WHERE estado = 'PENDIENTE';

CREATE TABLE cuentas_financieras (
    id uuid PRIMARY KEY,
    nombre varchar(100) NOT NULL,
    tipo varchar(30) NOT NULL,
    tipo_titular varchar(20) NOT NULL,
    propietario_persona_id uuid REFERENCES personas(id),
    propietario_hogar_id uuid REFERENCES hogares(id),
    moneda varchar(3) NOT NULL DEFAULT 'MXN',
    fecha_creacion timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT cuentas_tipo_chk CHECK (tipo IN ('BANCARIA', 'EFECTIVO', 'AHORRO', 'BILLETERA_DIGITAL')),
    CONSTRAINT cuentas_titular_chk CHECK (
        (tipo_titular = 'PERSONA' AND propietario_persona_id IS NOT NULL AND propietario_hogar_id IS NULL)
        OR (tipo_titular = 'HOGAR' AND propietario_hogar_id IS NOT NULL AND propietario_persona_id IS NULL)
    ),
    CONSTRAINT cuentas_moneda_chk CHECK (moneda ~ '^[A-Z]{3}$')
);
CREATE INDEX cuentas_propietario_persona_idx ON cuentas_financieras(propietario_persona_id);
CREATE INDEX cuentas_propietario_hogar_idx ON cuentas_financieras(propietario_hogar_id);

CREATE TABLE cuentas_hogar (
    id uuid PRIMARY KEY,
    cuenta_id uuid NOT NULL REFERENCES cuentas_financieras(id),
    hogar_id uuid NOT NULL REFERENCES hogares(id),
    fecha_vinculacion timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT cuentas_hogar_uk UNIQUE (cuenta_id, hogar_id)
);
CREATE INDEX cuentas_hogar_hogar_idx ON cuentas_hogar(hogar_id);

CREATE TABLE tarjetas (
    id uuid PRIMARY KEY,
    cuenta_id uuid NOT NULL REFERENCES cuentas_financieras(id),
    nombre varchar(100) NOT NULL,
    tipo varchar(20) NOT NULL,
    ultimos_4_digitos varchar(4),
    estado varchar(20) NOT NULL DEFAULT 'ACTIVA',
    fecha_creacion timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT tarjetas_tipo_chk CHECK (tipo IN ('FISICA', 'DIGITAL')),
    CONSTRAINT tarjetas_estado_chk CHECK (estado IN ('ACTIVA', 'INACTIVA', 'BLOQUEADA')),
    CONSTRAINT tarjetas_ultimos4_chk CHECK (ultimos_4_digitos IS NULL OR ultimos_4_digitos ~ '^[0-9]{4}$'),
    CONSTRAINT tarjetas_id_cuenta_uk UNIQUE (id, cuenta_id)
);

CREATE TABLE movimientos_financieros (
    id uuid PRIMARY KEY,
    hogar_id uuid NOT NULL REFERENCES hogares(id),
    tipo varchar(20) NOT NULL,
    estado varchar(20) NOT NULL DEFAULT 'BORRADOR',
    concepto varchar(200) NOT NULL,
    registrado_por_persona_id uuid NOT NULL REFERENCES personas(id),
    realizado_por_persona_id uuid NOT NULL REFERENCES personas(id),
    movimiento_revertido_id uuid UNIQUE REFERENCES movimientos_financieros(id),
    fecha_movimiento timestamptz NOT NULL,
    fecha_registro timestamptz NOT NULL DEFAULT now(),
    fecha_confirmacion timestamptz,
    CONSTRAINT movimientos_tipo_chk CHECK (tipo IN ('INGRESO', 'GASTO', 'TRANSFERENCIA', 'SALDO_INICIAL', 'REVERSO')),
    CONSTRAINT movimientos_estado_chk CHECK (estado IN ('BORRADOR', 'CONFIRMADO')),
    CONSTRAINT movimientos_confirmacion_chk CHECK (
        (estado = 'BORRADOR' AND fecha_confirmacion IS NULL)
        OR (estado = 'CONFIRMADO' AND fecha_confirmacion IS NOT NULL)
    ),
    CONSTRAINT movimientos_reverso_chk CHECK ((tipo = 'REVERSO') = (movimiento_revertido_id IS NOT NULL)),
    CONSTRAINT movimientos_autorreverso_chk CHECK (movimiento_revertido_id IS NULL OR movimiento_revertido_id <> id)
);
CREATE INDEX movimientos_hogar_fecha_idx ON movimientos_financieros(hogar_id, fecha_movimiento DESC);
CREATE INDEX movimientos_confirmados_hogar_idx ON movimientos_financieros(hogar_id, fecha_movimiento DESC)
    WHERE estado = 'CONFIRMADO';

CREATE TABLE detalles_movimiento (
    id uuid PRIMARY KEY,
    movimiento_id uuid NOT NULL REFERENCES movimientos_financieros(id),
    cuenta_id uuid NOT NULL REFERENCES cuentas_financieras(id),
    forma_pago varchar(30),
    tarjeta_id uuid,
    monto numeric(18,2) NOT NULL,
    CONSTRAINT detalles_monto_no_cero_chk CHECK (monto <> 0),
    CONSTRAINT detalles_forma_pago_chk CHECK (forma_pago IS NULL OR forma_pago IN
        ('EFECTIVO', 'TRANSFERENCIA', 'TARJETA_DEBITO', 'OTRO')),
    CONSTRAINT detalles_tarjeta_forma_chk CHECK (
        tarjeta_id IS NULL OR (forma_pago IS NOT NULL AND forma_pago = 'TARJETA_DEBITO')
    ),
    CONSTRAINT detalles_tarjeta_cuenta_fk FOREIGN KEY (tarjeta_id, cuenta_id) REFERENCES tarjetas(id, cuenta_id)
);
CREATE INDEX detalles_movimiento_idx ON detalles_movimiento(movimiento_id);
CREATE INDEX detalles_cuenta_idx ON detalles_movimiento(cuenta_id);

-- IMPORTANTE PARA LA PRIMERA ITERACION:
-- 1) Antes de CONFIRMAR, el backend valida el tipo y signos de detalles;
--    en transferencias internas la suma debe ser 0 y las monedas deben coincidir.
-- 2) Confirmados son inmutables: bloquear UPDATE/DELETE en backend y después
--    reforzar con permisos SQL o triggers antes de uso real multiusuario.
-- 3) Verificar pertenencia/autorización al hogar y a las cuentas.
-- 4) No se incluye todavía Row Level Security, invitación por correo, ni
--    auditoría administrativa. Son requisitos pendientes antes de producción.
-- 5) No contar el saldo de una cuenta personal vinculada como dinero común.
