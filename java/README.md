# CLBS Java Migration

Java 17 / Spring Boot 3.3 migration of the COBOL Investment Portfolio Management System in
`../src`. The mapping from each COBOL program, copybook and dataset to its Java counterpart —
including the behaviour the COBOL source leaves undefined — is documented in
[`../documentation/technical/java-migration.md`](../documentation/technical/java-migration.md).

## Build and test

```bash
cd java
mvn -B test        # 54 unit + integration tests
mvn -B package     # target/clbs-java-1.0.0.jar
```

## Run on localhost

```bash
java -jar target/clbs-java-1.0.0.jar   # or: mvn -B spring-boot:run
```

The application listens on `http://localhost:8080`, keeps VSAM/QSAM datasets in memory and the DB2
tables in H2 (`jdbc:h2:mem:clbs`, console at `/h2-console`). On startup `SampleDataLoader` loads the
records from `documentation/operations/test-data-specs.md` unless `clbs.sample-data=false`.

## Endpoints

| Method | Path | COBOL program |
| --- | --- | --- |
| GET | `/api/portfolios` | PORTREAD |
| GET/POST/PUT/DELETE | `/api/portfolios[/{portId}]` | PORTMSTR |
| POST | `/api/portfolios/batch-{add,update,delete}` | PORTADD / PORTUPDT / PORTDEL |
| POST | `/api/transactions` | PORTTRAN |
| POST | `/api/inquiry` (`X-User-Id` header) | INQONLN → INQPORT / INQHIST / SECMGR |
| POST | `/api/batch/{control,sequence,recovery,history-load}` | BCHCTL00 / PRCSEQ00 / RCVPRC00 / HISTLD00 |
| GET | `/api/batch/reports/{positions,audit,statistics,return-codes}` | RPTPOS00 / RPTAUD00 / RPTSTA00 / RTNANA00 |
| POST | `/api/utility/{maintenance,monitor,validate}` | UTLMNT00 / UTLMON00 / UTLVAL00 |
| POST | `/api/utility/{test-data,test-validate}` | TSTGEN00 / TSTVAL00 |

Example:

```bash
curl -s localhost:8080/api/portfolios
curl -s -X POST localhost:8080/api/inquiry -H 'Content-Type: application/json' \
     -H 'X-User-Id: USER0001' -d '{"function":"INQP","accountNo":"PORT00001"}'
curl -s localhost:8080/api/batch/reports/positions
```
