#!/usr/bin/env python3
"""
ETL Migration Pipeline for COBOL Legacy System
Phase 1: VSAM/DB2 to PostgreSQL Migration

This script orchestrates the migration of data from legacy VSAM files
and DB2 tables to the new PostgreSQL database.
"""

import os
import sys
import logging
import yaml
import click
from datetime import datetime
from pathlib import Path
from typing import Dict, Any, Optional

import pandas as pd
import psycopg2
from psycopg2.extras import execute_batch
from sqlalchemy import create_engine
from tqdm import tqdm
import structlog

from vsam_parser import VSAMParser
from db2_extractor import DB2Extractor
from data_transformer import DataTransformer
from postgres_loader import PostgresLoader
from checkpoint_manager import CheckpointManager


structlog.configure(
    processors=[
        structlog.stdlib.filter_by_level,
        structlog.stdlib.add_logger_name,
        structlog.stdlib.add_log_level,
        structlog.stdlib.PositionalArgumentsFormatter(),
        structlog.processors.TimeStamper(fmt="iso"),
        structlog.processors.StackInfoRenderer(),
        structlog.processors.format_exc_info,
        structlog.processors.UnicodeDecoder(),
        structlog.dev.ConsoleRenderer()
    ],
    wrapper_class=structlog.stdlib.BoundLogger,
    context_class=dict,
    logger_factory=structlog.stdlib.LoggerFactory(),
    cache_logger_on_first_use=True,
)

logger = structlog.get_logger(__name__)


class MigrationPipeline:
    """Main ETL pipeline orchestrator for COBOL to PostgreSQL migration."""
    
    def __init__(self, config_path: str, validate_only: bool = False):
        self.config = self._load_config(config_path)
        self.validate_only = validate_only
        self.checkpoint_manager = CheckpointManager(self.config)
        self.transformer = DataTransformer(self.config)
        self.loader = PostgresLoader(self.config)
        self.stats = {
            'start_time': None,
            'end_time': None,
            'tables_migrated': 0,
            'records_processed': 0,
            'records_failed': 0,
            'errors': []
        }
        
    def _load_config(self, config_path: str) -> Dict[str, Any]:
        """Load and validate configuration file."""
        with open(config_path, 'r') as f:
            config = yaml.safe_load(f)
        
        for key, value in config.items():
            if isinstance(value, str) and value.startswith('${'):
                env_var = value[2:-1].split(':')[0]
                default = value[2:-1].split(':')[1] if ':' in value[2:-1] else None
                config[key] = os.environ.get(env_var, default)
                
        return config
    
    def run(self) -> Dict[str, Any]:
        """Execute the full migration pipeline."""
        self.stats['start_time'] = datetime.now()
        logger.info("Starting migration pipeline", validate_only=self.validate_only)
        
        try:
            if self.validate_only:
                self._validate_sources()
                self._validate_targets()
                logger.info("Validation completed successfully")
            else:
                self._migrate_vsam_files()
                self._migrate_db2_tables()
                self._verify_migration()
                
        except Exception as e:
            logger.error("Migration failed", error=str(e))
            self.stats['errors'].append(str(e))
            raise
        finally:
            self.stats['end_time'] = datetime.now()
            self._generate_report()
            
        return self.stats
    
    def _validate_sources(self):
        """Validate source data availability and format."""
        logger.info("Validating source data")
        
        for source_name, source_config in self.config.get('sources', {}).get('vsam', {}).items():
            source_path = source_config.get('source_path', '')
            if source_path and not source_path.startswith('${'):
                if not Path(source_path).exists():
                    logger.warning(f"VSAM source not found: {source_path}")
                else:
                    logger.info(f"VSAM source validated: {source_name}")
                    
        logger.info("Source validation complete")
    
    def _validate_targets(self):
        """Validate target database connectivity and schema."""
        logger.info("Validating target database")
        
        try:
            conn = self.loader.get_connection()
            cursor = conn.cursor()
            
            tables = ['positions', 'transactions', 'portfolios', 'position_history', 
                     'error_log', 'audit_log', 'users', 'user_authorizations']
            
            for table in tables:
                cursor.execute(f"""
                    SELECT EXISTS (
                        SELECT FROM information_schema.tables 
                        WHERE table_schema = 'portfolio' 
                        AND table_name = %s
                    )
                """, (table,))
                exists = cursor.fetchone()[0]
                
                if exists:
                    logger.info(f"Target table validated: {table}")
                else:
                    logger.warning(f"Target table missing: {table}")
                    
            conn.close()
            logger.info("Target validation complete")
            
        except Exception as e:
            logger.error("Target validation failed", error=str(e))
            raise
    
    def _migrate_vsam_files(self):
        """Migrate data from VSAM files to PostgreSQL."""
        logger.info("Starting VSAM file migration")
        
        vsam_sources = self.config.get('sources', {}).get('vsam', {})
        
        for source_name, source_config in vsam_sources.items():
            logger.info(f"Migrating VSAM source: {source_name}")
            
            try:
                checkpoint = self.checkpoint_manager.get_checkpoint(source_name)
                
                parser = VSAMParser(source_config)
                records = parser.parse(start_from=checkpoint)
                
                transformed = self.transformer.transform(
                    records, 
                    source_config['target_table']
                )
                
                batch_size = self.config['migration']['batch_size']
                for i, batch in enumerate(self._batch_records(transformed, batch_size)):
                    self.loader.load(batch, source_config['target_table'])
                    self.stats['records_processed'] += len(batch)
                    
                    if (i + 1) % (self.config['migration']['checkpoint_interval'] // batch_size) == 0:
                        self.checkpoint_manager.save_checkpoint(
                            source_name, 
                            self.stats['records_processed']
                        )
                        
                self.stats['tables_migrated'] += 1
                logger.info(f"Completed VSAM migration: {source_name}", 
                           records=self.stats['records_processed'])
                
            except FileNotFoundError:
                logger.warning(f"VSAM file not found, skipping: {source_name}")
            except Exception as e:
                logger.error(f"VSAM migration failed: {source_name}", error=str(e))
                self.stats['errors'].append(f"{source_name}: {str(e)}")
    
    def _migrate_db2_tables(self):
        """Migrate data from DB2 tables to PostgreSQL."""
        logger.info("Starting DB2 table migration")
        
        db2_sources = self.config.get('sources', {}).get('db2', {})
        
        for source_name, source_config in db2_sources.items():
            logger.info(f"Migrating DB2 source: {source_name}")
            
            try:
                extractor = DB2Extractor(self.config)
                records = extractor.extract(source_config['source_query'])
                
                transformed = self.transformer.transform(
                    records,
                    source_config['target_table']
                )
                
                batch_size = self.config['migration']['batch_size']
                for batch in self._batch_records(transformed, batch_size):
                    self.loader.load(batch, source_config['target_table'])
                    self.stats['records_processed'] += len(batch)
                    
                self.stats['tables_migrated'] += 1
                logger.info(f"Completed DB2 migration: {source_name}")
                
            except Exception as e:
                logger.error(f"DB2 migration failed: {source_name}", error=str(e))
                self.stats['errors'].append(f"{source_name}: {str(e)}")
    
    def _verify_migration(self):
        """Verify data integrity after migration."""
        logger.info("Verifying migration integrity")
        
        conn = self.loader.get_connection()
        cursor = conn.cursor()
        
        verification_queries = [
            ("positions", "SELECT COUNT(*) FROM portfolio.positions"),
            ("transactions", "SELECT COUNT(*) FROM portfolio.transactions"),
            ("portfolios", "SELECT COUNT(*) FROM portfolio.portfolios"),
            ("position_history", "SELECT COUNT(*) FROM portfolio.position_history"),
            ("error_log", "SELECT COUNT(*) FROM portfolio.error_log"),
            ("audit_log", "SELECT COUNT(*) FROM portfolio.audit_log"),
        ]
        
        for table_name, query in verification_queries:
            cursor.execute(query)
            count = cursor.fetchone()[0]
            logger.info(f"Verification: {table_name}", record_count=count)
            
        conn.close()
        logger.info("Migration verification complete")
    
    def _batch_records(self, records, batch_size):
        """Yield batches of records."""
        batch = []
        for record in records:
            batch.append(record)
            if len(batch) >= batch_size:
                yield batch
                batch = []
        if batch:
            yield batch
    
    def _generate_report(self):
        """Generate migration summary report."""
        duration = (self.stats['end_time'] - self.stats['start_time']).total_seconds()
        
        report = f"""
================================================================================
                    MIGRATION SUMMARY REPORT
================================================================================
Start Time:         {self.stats['start_time']}
End Time:           {self.stats['end_time']}
Duration:           {duration:.2f} seconds
Tables Migrated:    {self.stats['tables_migrated']}
Records Processed:  {self.stats['records_processed']}
Records Failed:     {self.stats['records_failed']}
Errors:             {len(self.stats['errors'])}
================================================================================
"""
        if self.stats['errors']:
            report += "\nErrors:\n"
            for error in self.stats['errors']:
                report += f"  - {error}\n"
                
        logger.info(report)
        
        report_path = Path(self.config.get('logging', {}).get('file', 'migration_report.txt')).parent
        report_path.mkdir(parents=True, exist_ok=True)
        
        with open(report_path / 'migration_report.txt', 'w') as f:
            f.write(report)


@click.command()
@click.option('--config', '-c', default='config.yaml', help='Configuration file path')
@click.option('--validate-only', is_flag=True, help='Only validate sources and targets')
@click.option('--source', '-s', help='Migrate specific source only')
@click.option('--resume', is_flag=True, help='Resume from last checkpoint')
def main(config: str, validate_only: bool, source: Optional[str], resume: bool):
    """Run the ETL migration pipeline."""
    
    config_path = Path(__file__).parent / config
    
    if not config_path.exists():
        logger.error(f"Configuration file not found: {config_path}")
        sys.exit(1)
        
    pipeline = MigrationPipeline(str(config_path), validate_only=validate_only)
    
    try:
        stats = pipeline.run()
        
        if stats['errors']:
            logger.warning("Migration completed with errors")
            sys.exit(1)
        else:
            logger.info("Migration completed successfully")
            sys.exit(0)
            
    except Exception as e:
        logger.error("Migration failed", error=str(e))
        sys.exit(1)


if __name__ == '__main__':
    main()
