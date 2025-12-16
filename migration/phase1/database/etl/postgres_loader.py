"""
PostgreSQL Data Loader for COBOL Legacy Migration
Loads transformed data into PostgreSQL tables.
"""

import os
from typing import Dict, Any, List
import psycopg2
from psycopg2.extras import execute_batch
import structlog

logger = structlog.get_logger(__name__)


class PostgresLoader:
    """Loads data into PostgreSQL database."""
    
    def __init__(self, config: Dict[str, Any]):
        self.config = config
        self.db_config = config.get('database', {}).get('postgresql', {})
        self.schema = self.db_config.get('schema', 'portfolio')
        self.batch_size = config.get('migration', {}).get('batch_size', 1000)
        self._connection = None
        
    def get_connection(self):
        """Get database connection."""
        
        if self._connection is None or self._connection.closed:
            self._connection = psycopg2.connect(
                host=os.environ.get('POSTGRES_HOST', self.db_config.get('host', 'localhost')),
                port=os.environ.get('POSTGRES_PORT', self.db_config.get('port', 5432)),
                database=os.environ.get('POSTGRES_DB', self.db_config.get('database', 'portfolio_db')),
                user=os.environ.get('POSTGRES_USER', self.db_config.get('user', 'portfolio_app')),
                password=os.environ.get('POSTGRES_PASSWORD', self.db_config.get('password', '')),
            )
            self._connection.autocommit = False
            
        return self._connection
    
    def load(self, records: List[Dict[str, Any]], table_name: str) -> int:
        """Load records into the specified table."""
        
        if not records:
            return 0
            
        conn = self.get_connection()
        cursor = conn.cursor()
        
        try:
            columns = list(records[0].keys())
            columns = [c for c in columns if not c.startswith('_')]
            
            placeholders = ', '.join(['%s'] * len(columns))
            column_names = ', '.join(columns)
            
            insert_sql = f"""
                INSERT INTO {self.schema}.{table_name} ({column_names})
                VALUES ({placeholders})
                ON CONFLICT DO NOTHING
            """
            
            values = []
            for record in records:
                row = tuple(record.get(col) for col in columns)
                values.append(row)
            
            execute_batch(cursor, insert_sql, values, page_size=self.batch_size)
            
            conn.commit()
            
            logger.info(f"Loaded {len(records)} records into {table_name}")
            return len(records)
            
        except Exception as e:
            conn.rollback()
            logger.error(f"Error loading data into {table_name}", error=str(e))
            raise
        finally:
            cursor.close()
    
    def upsert(self, records: List[Dict[str, Any]], table_name: str, key_columns: List[str]) -> int:
        """Upsert records into the specified table."""
        
        if not records:
            return 0
            
        conn = self.get_connection()
        cursor = conn.cursor()
        
        try:
            columns = list(records[0].keys())
            columns = [c for c in columns if not c.startswith('_')]
            
            placeholders = ', '.join(['%s'] * len(columns))
            column_names = ', '.join(columns)
            key_column_names = ', '.join(key_columns)
            
            update_columns = [c for c in columns if c not in key_columns]
            update_clause = ', '.join([f"{c} = EXCLUDED.{c}" for c in update_columns])
            
            upsert_sql = f"""
                INSERT INTO {self.schema}.{table_name} ({column_names})
                VALUES ({placeholders})
                ON CONFLICT ({key_column_names})
                DO UPDATE SET {update_clause}
            """
            
            values = []
            for record in records:
                row = tuple(record.get(col) for col in columns)
                values.append(row)
            
            execute_batch(cursor, upsert_sql, values, page_size=self.batch_size)
            
            conn.commit()
            
            logger.info(f"Upserted {len(records)} records into {table_name}")
            return len(records)
            
        except Exception as e:
            conn.rollback()
            logger.error(f"Error upserting data into {table_name}", error=str(e))
            raise
        finally:
            cursor.close()
    
    def truncate(self, table_name: str) -> None:
        """Truncate the specified table."""
        
        conn = self.get_connection()
        cursor = conn.cursor()
        
        try:
            cursor.execute(f"TRUNCATE TABLE {self.schema}.{table_name} CASCADE")
            conn.commit()
            logger.info(f"Truncated table {table_name}")
        except Exception as e:
            conn.rollback()
            logger.error(f"Error truncating table {table_name}", error=str(e))
            raise
        finally:
            cursor.close()
    
    def get_count(self, table_name: str) -> int:
        """Get record count for the specified table."""
        
        conn = self.get_connection()
        cursor = conn.cursor()
        
        try:
            cursor.execute(f"SELECT COUNT(*) FROM {self.schema}.{table_name}")
            count = cursor.fetchone()[0]
            return count
        finally:
            cursor.close()
    
    def close(self) -> None:
        """Close database connection."""
        
        if self._connection and not self._connection.closed:
            self._connection.close()
            self._connection = None
