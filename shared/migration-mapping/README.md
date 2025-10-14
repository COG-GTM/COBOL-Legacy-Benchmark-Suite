# Migration Mapping

This directory tracks the mapping between COBOL components and their Java equivalents throughout the migration process.

## Purpose

- Document which COBOL programs have been migrated to Java
- Track the Java components that implement COBOL functionality
- Maintain traceability between legacy and modern implementations
- Support incremental migration strategy

## Contents

Mapping files will include:

- Component mapping tables (COBOL program → Java class)
- Data structure mappings (COBOL copybook → Java model)
- Database mapping (VSAM/DB2 → Java persistence)
- Batch job mappings (JCL → Java batch framework)
- API mappings (CICS transactions → REST endpoints)

## Format

Mapping documents will be maintained in markdown format with tables showing:
- COBOL Component Name
- Java Component Name(s)
- Migration Status (Not Started, In Progress, Complete, Verified)
- Migration Sprint
- Notes and special considerations
