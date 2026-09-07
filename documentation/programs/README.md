# Documentación funcional por programa COBOL

Documentación en español del funcionamiento de cada programa de la COBOL Legacy Benchmark Suite. Cada ficha sigue la misma plantilla (ficha técnica, propósito, funcionamiento con diagrama de flujo, interfaz, estructuras de datos, reglas de negocio, manejo de errores, grafo de dependencias, observaciones y referencias).

Las dependencias entre programas, copybooks, JCL, CICS, VSAM y DB2 están en el [grafo de dependencias global](../technical/dependency-graph.md) (generado por `tools/depgraph/build_dependency_graph.py`; modelo completo en [`dependency-graph.json`](../technical/dependency-graph.json)).

Programas: 38 (37 documentados, 1 con fuente vacío).

## Batch

| Programa | Líneas | Invocado por | Propósito |
| --- | ---: | --- | --- |
| [`BCHCTL00`](BCHCTL00.md) | 127 | — | Subrutina batch (esqueleto) que, según la función recibida (INIT/CHEK/UPDT/TERM), debe inicializar, comprobar prerrequisitos, actualizar y finalizar el registro de estado de un job en el fichero de control batch VSAM BCHCTL, delegando el registro de errores en ERRPROC. |
| [`CKPRST`](CKPRST.md) | 57 | — | Rutina común de checkpoint/restart a nivel de programa para los batch de larga duración (inicializar, tomar, confirmar y reanudar checkpoints sobre el fichero VSAM CKPTFILE), actualmente un esqueleto sin implementar. |
| [`HISTLD00`](HISTLD00.md) | 233 | — | Programa batch que lee secuencialmente el fichero VSAM de histórico de transacciones TRANHIST y lo carga fila a fila en la tabla DB2 POSHIST, con COMMIT y checkpoint en el fichero de control BCHCTL cada 1000 registros. |
| `POSUPDT` | 0 | — | **Fichero fuente vacío** (`src/programs/batch/POSUPDT.cbl`). `system-architecture.md` lo describe como el actualizador de posiciones del flujo batch, pero no hay código que documentar. |
| [`PRCSEQ00`](PRCSEQ00.md) | 345 | — | Subrutina de control batch que construye la secuencia diaria de procesos a partir del fichero VSAM PRCSEQ, crea sus registros de control en BCHCTL, entrega al invocador el siguiente proceso listo tras comprobar sus dependencias y calcula el código de retorno final de la secuencia. |
| [`RCVPRC00`](RCVPRC00.md) | 302 | — | Subrutina batch de recuperación de procesos que, según la definición del proceso en PRCSEQ, reinicia (estado R), salta (estado D, RC +4) o termina (estado E, RC +8) los registros del fichero de control BCHCTL, para un proceso concreto, todos los de una fecha o todo el fichero. |
| [`RPTAUD00`](RPTAUD00.md) | 147 | `JCL:RPTAUD` | Programa batch de reporting que debe generar el informe de auditoría del sistema (SYSTEM AUDIT REPORT, 132 columnas) a partir del log de auditoría AUDITLOG y del log de errores ERRLOG, aunque en el repositorio solo está implementada la apertura de ficheros y la escritura de cabeceras. |
| [`RPTPOS00`](RPTPOS00.md) | 160 | `JCL:RPTPOS` | Programa batch que recorre secuencialmente el maestro VSAM de posiciones (POSMSTRE) y genera el informe diario de posiciones de 132 columnas (RPTFILE) con una línea por posición, con la actividad de transacciones, excepciones y métricas anunciadas pero no implementadas. |
| [`RPTSTA00`](RPTSTA00.md) | 185 | `JCL:RPTSTA` | Programa batch que lee los ficheros VSAM de estadísticas DB2 (DB2STATS) y batch (BCHSTATS) para generar el informe de 132 columnas 'SYSTEM STATISTICS AND PERFORMANCE REPORT' en RPTFILE, aunque en el repositorio solo está codificado el esqueleto (apertura, cabeceras, bucles de lectura y cierre). |
| [`RTNANA00`](RTNANA00.md) | 210 | `JCL:RTNANA` | Utilidad batch que lee la tabla DB2 RTNCODES y genera un informe de 133 posiciones con el recuento de ejecuciones por programa desglosado por estado (S/W/E/F) más una línea de totales. |
| [`RTNCDE00`](RTNCDE00.md) | 141 | — | Subrutina común que centraliza la gestión de códigos de retorno del sistema: inicializa, fija y clasifica (S/W/E/F) el código y el máximo alcanzado en el área RTNCODE del llamador, lo registra en la tabla DB2 RTNCODES y devuelve estadísticas (total/máximo/mínimo) por programa y ventana temporal. |

## Común (subrutinas y soporte DB2)

| Programa | Líneas | Invocado por | Propósito |
| --- | ---: | --- | --- |
| [`AUDPROC`](AUDPROC.md) | 96 | `PORTMSTR`, `PORTTRAN` | Subrutina común que recibe una petición de auditoría (LS-AUDIT-REQUEST) del programa llamador y la graba como un registro de 392 bytes (copybook AUDITLOG) al final del fichero secuencial AUDFILE, devolviendo 0 u 8 en LS-RETURN-CODE. |
| [`DB2CMT`](DB2CMT.md) | 170 | — | Subrutina común de la capa de soporte DB2 que centraliza el control de la unidad de trabajo (COMMIT condicional por frecuencia de registros o forzado, ROLLBACK, creación y restauración de SAVEPOINT) y mantiene estadísticas de commits, rollbacks y savepoints. |
| [`DB2CONN`](DB2CONN.md) | 154 | — | Subrutina común que gestiona la conexión con DB2 (CONNECT con hasta 3 reintentos, desconexión con COMMIT previo y comprobación de estado vía SYSIBM.SYSDUMMY1) devolviendo al llamador código de retorno, SQLCODE y mensaje de error. |
| [`DB2ERR`](DB2ERR.md) | 200 | `DB2CMT` | Subrutina común de la capa de soporte DB2 que, según el código de función recibido (LOG/DIAG/RETR), registra errores SQL en la tabla ERRLOG clasificándolos por severidad y marcando si son reintentables, traduce un SQLCODE a un texto de diagnóstico y código de retorno, o recupera el último error registrado para un programa. |
| [`DB2STAT`](DB2STAT.md) | 228 | — | Subrutina batch de soporte DB2 que, mediante los códigos de función INIT/UPDT/TERM/DISP, registra en la tabla temporal declarada SESSION.DBSTATS las estadísticas de ejecución (filas leídas/insertadas/actualizadas/borradas, commits, rollbacks, tiempo transcurrido y CPU estimada) de un programa y las muestra por DISPLAY al terminar. |
| [`ERRPROC`](ERRPROC.md) | 107 | `BCHCTL00`, `DB2CMT`, `DB2CONN`, `DB2ERR`, `DB2STAT`, `HISTLD00`, `PORTMSTR`, `PORTTRAN`, `PRCSEQ00`, `RCVPRC00` | Subrutina común de proceso de errores batch que recibe una petición de error (programa, categoría, código, severidad, texto y detalles), le añade la marca de tiempo, la graba en el fichero secuencial ERRLOG en modo EXTEND, la muestra por DISPLAY en SYSOUT y devuelve la severidad como código de retorno al llamador. |

## Online (CICS)

| Programa | Líneas | Invocado por | Propósito |
| --- | ---: | --- | --- |
| [`CURSMGR`](CURSMGR.md) | 91 | `INQHIST` | Subrutina CICS invocada por LINK desde INQHIST que pretende centralizar el ciclo de vida (declarar, abrir, leer, cerrar) de cursores DB2 dinámicos para las consultas online de histórico sobre POSHIST. |
| [`DB2ONLN`](DB2ONLN.md) | 120 | `DB2RECV`, `INQHIST` | Subprograma CICS invocado por LINK desde INQHIST y DB2RECV que centraliza las peticiones de conexión ('C'), desconexión ('D') y estado ('S') con la base de datos DB2 POSMVP, devolviendo en la COMMAREA un código de respuesta, el SQLCODE, un mensaje y un token de conexión. |
| [`DB2RECV`](DB2RECV.md) | 145 | `INQHIST` | Subrutina CICS de recuperación DB2 para la capa online que, según el tipo de petición recibido en la COMMAREA, reintenta la conexión a DB2 vía DB2ONLN (hasta 3 intentos con espera de 2 s), ejecuta un ROLLBACK de la transacción o registra un fallo de cursor a través de ERRHNDL, devolviendo al llamador (INQHIST) un estado S/F/R, un código de respuesta y el SQLCODE. |
| [`ERRHNDL`](ERRHNDL.md) | 118 | `DB2RECV`, `INQONLN` | ERRHNDL es el manejador centralizado de errores de la capa online CICS: recibe por COMMAREA (copybook ERRHND) el contexto del error, lo registra en la tabla DB2 ERRLOG, formatea el mensaje para el usuario y devuelve al llamador la acción a tomar (R/C/A) según la severidad. |
| [`INQHIST`](INQHIST.md) | 193 | `INQONLN` | Subprograma CICS invocado por INQONLN que recupera de la tabla DB2 POSHIST las últimas 10 transacciones de una cuenta (vía DB2ONLN/DB2RECV/CURSMGR) y las envía a la pantalla HISMAP del mapset INQSET. |
| [`INQONLN`](INQONLN.md) | 171 | `CICS:PINQ` | Programa inicial de la transacción CICS PINQ que recibe la pantalla del mapset INQSET, despacha la función solicitada (menú, consulta de posición vía INQPORT, consulta de histórico vía INQHIST o salida), y coordina el control de seguridad con SECMGR y el registro de errores con ERRHNDL, sin acceder directamente a ficheros ni DB2. |
| [`INQPORT`](INQPORT.md) | 110 | `INQONLN` | Subprograma CICS invocado por LINK desde INQONLN que lee por clave el registro de posición de la cuenta recibida en la COMMAREA (INQCOM) del fichero VSAM POSFILE y, si existe, lo envía a pantalla con el mapa POSMAP del mapset INQSET; en caso contrario devuelve un mensaje de error en la COMMAREA. |
| [`SECMGR`](SECMGR.md) | 135 | `INQONLN` | SECMGR es la subrutina CICS de seguridad de la capa online que, según el tipo de petición recibido en la COMMAREA, valida que el usuario indicado coincide con el firmado en CICS, comprueba su autorización sobre un recurso en la tabla DB2 AUTHFILE y registra el acceso en la tabla DB2 AUDITLOG. |

## Gestión de carteras

| Programa | Líneas | Invocado por | Propósito |
| --- | ---: | --- | --- |
| [`PORTADD`](PORTADD.md) | 148 | `JCL:PORTADD` | Programa batch que lee un fichero secuencial de carteras con formato PORTFLIO, valida mínimamente cada registro (PORT-ID y PORT-CLIENT-NAME no vacíos, PORT-STATUS = 'A'), sella las fechas de creación y última modificación con la fecha del sistema y da de alta los registros en el fichero maestro VSAM KSDS PORTFILE, contabilizando altas, duplicados (status 22) y errores. |
| [`PORTDEL`](PORTDEL.md) | 194 | `JCL:PORTDEL` | Programa batch que lee un fichero secuencial de solicitudes de baja (DELEFILE), borra las carteras correspondientes del maestro VSAM KSDS PORTFILE por clave PORT-ID+PORT-ACCOUNT-NO y escribe una traza de auditoría propia en AUDFILE. |
| [`PORTMSTR`](PORTMSTR.md) | 288 | — | Subrutina invocable por CALL que ofrece operaciones CRUD (crear, leer, actualizar, borrar) sobre el fichero maestro de carteras VSAM KSDS PORTFILE identificado por PORT-ID, devolviendo un código de retorno 0/8 al llamador. |
| [`PORTREAD`](PORTREAD.md) | 111 | `JCL:PORTREAD` | Programa batch de utilidad que recorre secuencialmente el fichero maestro de carteras VSAM (PORTFILE) y muestra por SYSOUT un resumen de cada registro y el total de registros leídos. |
| [`PORTTEST`](PORTTEST.md) | 119 | `JCL:PORTTEST` | Utilidad batch autónoma que genera un fichero secuencial (TESTFILE) con 100 registros sintéticos de cartera con el layout del copybook PORTFLIO, usando FUNCTION RANDOM para tipo de cliente, estado y valor total, como datos de prueba para los programas de mantenimiento del maestro de carteras. |
| [`PORTTRAN`](PORTTRAN.md) | 317 | — | Programa batch que lee el fichero secuencial de transacciones TRANFILE, valida cada transacción (cartera existente, tipo BU/SL/TR/FE, cantidades) contra el maestro VSAM PORTFILE y, en teoría, actualiza las posiciones de la cartera registrando auditoría vía AUDPROC y errores vía ERRPROC. |
| [`PORTUPDT`](PORTUPDT.md) | 160 | `JCL:PORTUPDT` | Programa batch que lee un fichero secuencial de movimientos (UPDTFILE) y actualiza por clave el estado, el nombre de cliente o el valor total de registros existentes del maestro VSAM de carteras (PORTFILE) mediante READ/REWRITE. |
| [`PORTVALD`](PORTVALD.md) | 120 | — | Subrutina de validación de formato de datos elementales de cartera (identificador de cartera, número de cuenta, tipo de inversión e importe) que devuelve un código de retorno y un mensaje de error al llamador a través de la LINKAGE SECTION. |

## Pruebas

| Programa | Líneas | Invocado por | Propósito |
| --- | ---: | --- | --- |
| [`TSTGEN00`](TSTGEN00.md) | 216 | `JCL:TSTGEN` | Programa batch de la capa de test que lee un fichero de configuración de escenarios (TSTCFG) y una semilla aleatoria (RANDSEED) para generar ficheros secuenciales de carteras (PORTOUT) y transacciones (TRANOUT) de prueba con los formatos PORTFLIO y TRNREC, aunque la lógica de generación está sin implementar. |
| [`TSTVAL00`](TSTVAL00.md) | 225 | `JCL:TSTVAL` | Programa batch de la capa de test que debe leer casos de prueba (TESTCASE), ejecutarlos según su tipo, comparar resultados esperados y reales y emitir un informe de validación con métricas, aunque en el repositorio solo existe el esqueleto de control sin la lógica de ejecución/validación. |

## Utilidades

| Programa | Líneas | Invocado por | Propósito |
| --- | ---: | --- | --- |
| [`UTLMNT00`](UTLMNT00.md) | 182 | `JCL:UTLMNT` | Utilidad batch de mantenimiento de ficheros que, dirigida por un fichero de control (CTLFILE), ejecuta operaciones de archivado, limpieza, reorganización y análisis sobre los ficheros VSAM del sistema de carteras. |
| [`UTLMON00`](UTLMON00.md) | 221 | `JCL:UTLMON` | Utilidad batch de monitorización del sistema que debe leer umbrales de configuración (MONCFG), recoger métricas de CPU/memoria/DASD/DB2 en un bucle hasta las 23 h, registrar el estado en MONLOG y generar alertas en ALERTS, aunque en el repositorio solo existe como esqueleto incompleto. |
| [`UTLVAL00`](UTLVAL00.md) | 190 | `JCL:UTLVAL` | Utilidad batch que, dirigida por un fichero de control (VALCTL) con tipos INTEGRITY/XREF/FORMAT/BALANCE, debe validar la integridad, referencias cruzadas, formato y cuadre de saldos del maestro de posiciones (POSMSTRE) y del histórico de transacciones (TRANHIST), escribiendo las anomalías en un informe secuencial (ERRRPT). |

## Hallazgos transversales

Problemas detectados de forma repetida durante el análisis (el detalle está en la sección *Observaciones y problemas detectados* de cada ficha):

- Se registraron 514 observaciones en total en los 37 programas documentados.
- `POSUPDT.cbl` está vacío; el flujo batch descrito en `system-architecture.md` (`TRNVAL00` → `POSUPD00` → `HISTLD00`) referencia programas que no existen en `src/`.
- Copybooks referenciados pero inexistentes: `DB2STAT`, `PORTREC`.
- Tablas DB2 usadas sin DDL en `src/database/db2/`: `AUDITLOG`, `AUTHFILE`.
- Programas sin invocador conocido (ni `CALL`, ni `LINK`, ni JCL, ni transacción CICS): `BCHCTL00`, `CKPRST`, `DB2CMT`, `DB2CONN`, `DB2STAT`, `HISTLD00`, `PORTMSTR`, `PORTTRAN`, `PORTVALD`, `POSUPDT`, `PRCSEQ00`, `RCVPRC00`, `RTNCDE00`.
