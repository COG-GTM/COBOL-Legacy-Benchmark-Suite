# POSUPDT — Position Update (fuente vacío)

Fuente: `src/programs/batch/POSUPDT.cbl`

## Propósito

Según `documentation/technical/system-architecture.md`, `POSUPDT` (alias `POSUPD00`) es el módulo de actualización de posiciones de cartera: lee el fichero de transacciones validadas y actualiza el maestro de posiciones, usando `DB2CONN` y `ERRPROC`. Aparece como paso intermedio de la secuencia principal (`TRNVAL00 → POSUPD00 → HISTLD00`) en `PRCSEQ.cpy`.

**El fichero fuente `POSUPDT.cbl` está vacío (0 bytes)**, por lo que no hay lógica que documentar.

## Tipo

Programa batch (según la arquitectura); no verificable en el código.

## Entradas y salidas

No determinables desde el fuente. La arquitectura documental indica: entrada fichero de transacciones, salida maestro de posiciones (lectura/escritura).

## Flujo principal

Sin implementación.

## Reglas de negocio y validaciones

Sin implementación.

## Manejo de errores y códigos de retorno

Sin implementación.

## Dependencias

- `deps.json` registra el programa (`programs.POSUPDT`, grupo `batch`) pero **ninguna arista** con `from = POSUPDT`, coherente con el fuente vacío.
- Es llamado por: nadie en el código. Documentalmente, orquestado por `BCHCTL00` y precedido por `TRNVAL00`.

## Diagrama

```mermaid
flowchart LR
    POSUPDT[POSUPDT - fuente vacio]
    NOTA[Sin dependencias estaticas en deps.json]
    POSUPDT -.-> NOTA
```
