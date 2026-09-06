# java-migration — Portfolio Management System (COBOL → Java)

Migración de `src/` (COBOL / DB2 / VSAM / CICS-BMS / JCL) a Spring Boot 3, Java 17, Maven,
H2 + Flyway, Spring Data JPA, Spring Batch, Spring MVC + Thymeleaf, Spring Security y Actuator.
Paquete base: `com.cog.portfolio`.

```bash
mvn -f java-migration/pom.xml clean verify      # compila + tests
mvn -f java-migration/pom.xml spring-boot:run   # http://localhost:8080  (Flyway aplica V1 contra H2 en memoria)
```

Usuarios demo (in-memory): `admin/admin`, `operator/operator`, `user/user`.

---

## Decisiones clave

### Decisión 1 — Seguridad: solo roles (`SECMGR` / `AUTHFILE`)

La autorización se implementa exclusivamente con roles de Spring Security: `admin`, `operator`, `user`
(`security/SecurityConfiguration.java`).

| Recurso | Roles |
|---|---|
| `/menu`, `/inquiry/**`, `GET /api/portfolios/**` | user, operator, admin |
| `POST/PUT/PATCH/DELETE /api/portfolios/**`, `/batch/**`, `/maintenance/**`, `/validation/**`, `/actuator/**` | operator, admin |
| `/h2-console/**` | admin |
| `/actuator/health`, `/actuator/info` | público |

**NO se migró** la tabla `AUTHFILE` ni el `SELECT COUNT(*) FROM AUTHFILE` de `SECMGR.cbl`.

> **VALIDACIÓN HUMANA PENDIENTE (producción)** — marcado en código como `// TODO producción`:
> decidir si `AUTHFILE` se migra como fuente de un `UserDetailsService` / tabla de permisos por recurso,
> o si los roles `admin`/`operator`/`user` son suficientes. En la demo queda solo con roles.

### Decisión 2 — Steps provisorios `TRNVAL00` y `POSUPD00`

`TRNVAL00` y `POSUPD00` **no tienen fuente COBOL** en el repositorio: solo se los referencia en
`PRCSEQ.cpy` y en la documentación ("TRNMAIN (TRNVAL00)", "POSUPDT (POSUPD00)").
Los steps Java correspondientes son **RECONSTRUIDOS**, no traducciones fieles, y están marcados en el
código con `// PROVISIONAL - requiere validación humana`:

| Step (`batch/BatchConfiguration.java`) | Rol | Reconstruido desde |
|---|---|---|
| `transactionValidationStep` | TRNVAL00 | `PORTVALD.cbl` (formato de ID, cuenta, tipo, rango de montos) vía `PortfolioValidator` + `TransactionProcessingService.validate` |
| `positionUpdateStep` | POSUPD00 | `PORTTRAN.cbl` (matemática BUY/SELL/FEE) vía `PositionUpdateService`; el market value = cantidad × último precio es un supuesto |

**Hallazgo importante:** el archivo `src/programs/batch/POSUPDT.cbl` existe pero está **vacío** (0 líneas), y no
existe `src/programs/portfolio/POSUPDT.cbl`. Por eso `positionUpdateStep` no pudo reconstruirse desde
`POSUPDT.cbl` como preveía el plan; se reconstruyó desde `PORTTRAN.cbl` y la descripción de la documentación
("updates position records, maintains cost basis").

> **VALIDACIÓN HUMANA PENDIENTE** — un experto de negocio/mainframe debe:
> (a) confirmar la semántica esperada de `TRNVAL00` y `POSUPD00`;
> (b) confirmar/refutar que `POSUPDT` es el equivalente funcional de `POSUPD00` (no hay fuente para comparar);
> (c) validar el supuesto de market value del `positionUpdateStep`.
> Los tests de `DailyProcessingJobTest` fijan el comportamiento reconstruido y **no** afirman equivalencia con el COBOL.

Otras reconstrucciones menores marcadas `PROVISIONAL`: `TRNREC` no tiene estado "rechazado", las transacciones
inválidas se marcan `FAILED` (`F`).

### Decisión 3 — Precisión: escala 4 (dominio) → escala 3 (`POSHIST`) con `HALF_UP`

- `Transaction.quantity/price`: `BigDecimal` escala 4 (`TRN-QUANTITY/TRN-PRICE PIC S9(11)V9(4)`).
- `PositionHistory.quantity/price` (`POSHIST.PH_QUANTITY/PH_PRICE`, `DECIMAL(15,3)`): escala 3.
- Conversión explícita y centralizada en `common/ScaleConverter.toPoshistScale(...)` /
  `convertToPoshistScale(...)` con `setScale(3, RoundingMode.HALF_UP)`. Devuelve `ScaleConversion`
  (original, convertido, **residuo**) y `HistoryLoadProcessor` registra en WARN cada conversión con
  residuo ≠ 0 y la contabiliza (`getPrecisionLossCount()`). Nunca se trunca en silencio.
- Prohibido `double`/`float` (test por reflexión en `ScaleConverterTest`).

> **VALIDACIÓN HUMANA PENDIENTE** — `RoundingMode.HALF_UP` es un **supuesto técnico**: el COBOL original
> hacía un `MOVE` COMP-3 que truncaba implícitamente. El negocio debe aprobar la política de redondeo
> antes de producción, dado su impacto en cálculos monetarios de historia de posiciones.
> Tests: `ScaleConverterTest` (casos `.5` hacia arriba, residuo positivo/negativo) e `HistoryLoadStepTest`
> (persistencia real en POSHIST vía Spring Batch).

---

## Mapa COBOL → Java

### Datos (`domain/`, `repository/`, `db/migration/V1__baseline_schema.sql`)

| Copybook / DDL | Entidad JPA | Tabla H2 |
|---|---|---|
| `PORTFLIO.cpy` | `Portfolio` + `PortfolioId` | `PORTFOLIO` |
| `POSREC.cpy` | `Position` + `PositionId` | `POSITION_RECORD` |
| `TRNREC.cpy` | `Transaction` + `TransactionId` | `TRANSACTION_RECORD` |
| `HISTREC.cpy` | `History` + `HistoryId` | `HISTORY_RECORD` |
| `AUDITLOG.cpy` | `AuditLog` | `AUDIT_LOG` |
| `DBTBLS.cpy` / `POSHIST.sql` | `PositionHistory` + `PositionHistoryId` | `POSHIST` |
| `DBTBLS.cpy` / `ERRLOG.sql` | `ErrorLog` | `ERRLOG` |
| `RTNCODES.sql` | `ReturnCodeLog` | `RTNCODES` |

Niveles 88 → enums (`TransactionType`, `TransactionStatus`, `PortfolioStatus`, `ClientType`, `AuditAction`, …)
persistidos con su código COBOL (`CodedValueConverter`). `FILLER` descartados; fechas/horas `PIC X` →
`LocalDate`/`LocalTime`/`LocalDateTime`. Tablespaces, storage groups, particiones, grants y `PORTPLAN.sql`
(plan DB2) se omiten. `SQLCA.cpy`/`DBPROC.cpy` no se migran.

### Infraestructura reemplazada por framework (sin portar código)

| COBOL | Reemplazo |
|---|---|
| `DB2CONN`, `DB2ONLN` (pool, `WS-MAX-CONNECTIONS=100`) | HikariCP (`maximum-pool-size: 100`) |
| `DB2CMT` (commit por umbral 1000) | `@Transactional` / `commit-interval: 1000` |
| `DB2RECV`, `DB2ERR` | `@Retryable`, rollback, `DataAccessException` |
| `CURSMGR` | paginación Spring Data |
| `DB2STAT`, `UTLMON00` | Micrometer / Actuator (`/actuator/metrics`, `/actuator/prometheus`) |
| `ERRPROC`, `ERRHNDL` | SLF4J + `web/GlobalExceptionHandler` (`@ControllerAdvice`) |
| `RTNCDE00`, `RTNANA00` | `ReturnCodeStepListener` (RC → `ExitStatus`, fila en `RTNCODES`) + `ReportService.returnCodeAnalysis` |
| `BCHCTL00`, `PRCSEQ00`, `CKPRST`, `RCVPRC00` | `JobRepository`, `Job`/`Step`/`Flow`, `ReturnCodeDecider`, restart nativo |
| `AUDPROC` (lógica conservada) | `service/AuditService` |

### Lógica de negocio (`service/`, `validation/`)

| COBOL | Java |
|---|---|
| `PORTTRAN.cbl` | `TransactionProcessingService` (BUY suma; SELL valida "Insufficient units for sale" antes de restar; FEE resta costo; TRANSFER → "Transfer processing not implemented") |
| `PORTVALD.cbl` | `PortfolioValidator` (`PORT`+4 dígitos, cuenta 10 dígitos ≠ 0, `STK/BND/MMF/ETF`, rango de montos) |
| `PORTUPDT.cbl` | `PortfolioUpdateService` (S/N/V) |
| `PORTMSTR/PORTADD/PORTDEL/PORTREAD` | `PortfolioService` |
| `INQPORT.cbl`, `INQHIST.cbl` | `PositionInquiryService`, `HistoryInquiryService` (ORDER BY fecha/hora DESC, 10 filas/página) |
| `UTLVAL00.cbl` | `DataValidationService` |
| `UTLMNT00.cbl` | `MaintenanceService` (`ARCHIVE/CLEANUP/REORG/ANALYZE`; REORG es no-op en H2) + `@Scheduled` |

### Batch (`batch/`)

Flujo diario `dailyProcessingJob`: `TRNVAL00 → POSUPD00 → HISTLD00 → RPTPOS00`, cada etapa condicionada
por `ReturnCodeDecider` (continúa solo si `RC <= 4`). `historyLoadStep` (`HISTLD00`): chunk 1000, claves
duplicadas (`SQLCODE -803`) ignoradas en `HistoryLoadWriter`, otros errores hacen rollback del chunk.
`RPTPOS00`: `% cambio = (actual - previo) / previo * 100` en `ReportService.changePercent`.

### Web (`web/`, `templates/`)

- `INQSET.bms` (`MENMAP/POSMAP/HISMAP/ERRMAP`) → `menu.html`, `position.html`, `history.html`, `error.html`.
- `InquiryController`: `GET /menu`, `GET /inquiry/position`, `GET /inquiry/history` (PF7/PF8 = paginación).

### REST que reemplaza JCL

| JCL | Endpoint |
|---|---|
| `RPTPOS.jcl` | `POST /batch/report/positions` |
| `RPTAUD.jcl` | `POST /batch/report/audit` |
| `RPTSTA.jcl` | `POST /batch/report/statistics` |
| `RTNANA.jcl` | `POST /batch/returncodes/analyze` |
| (PRCSEQ diario) | `POST /batch/daily`, `POST /batch/history/load` |
| `PORTADD/PORTREAD/PORTUPDT/PORTDEL.jcl` | `POST/GET/PUT/PATCH/DELETE /api/portfolios` |
| `UTLMNT.jcl`, `UTLVAL.jcl` | `POST /maintenance/{ARCHIVE\|CLEANUP\|REORG\|ANALYZE}`, `POST /validation/run` |
| `UTLMON.jcl` | Actuator |
| `PORTDEF.jcl` (IDCAMS) | Flyway `V1__baseline_schema.sql` |
| `TSTGEN/TSTVAL.jcl`, `PORTTEST.cbl` | `TestData` + tests JUnit |

---

## Validaciones humanas pendientes (resumen)

1. **AUTHFILE** (Decisión 1): migrar como `UserDetailsService`/tabla de permisos o mantener roles.
2. **TRNVAL00 / POSUPD00** (Decisión 2): confirmar semántica; confirmar si `POSUPDT` == `POSUPD00`
   (fuente vacía en el repo); validar supuesto de market value.
3. **HALF_UP** (Decisión 3): aprobar la política de redondeo 4 → 3 hacia `POSHIST`.
