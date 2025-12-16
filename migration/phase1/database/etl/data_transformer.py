"""
Data Transformer for COBOL Legacy Migration
Transforms COBOL data formats to PostgreSQL-compatible formats.
"""

from datetime import datetime, date, time
from decimal import Decimal
from typing import Dict, Any, Iterator, List, Optional
import uuid
import structlog

logger = structlog.get_logger(__name__)


class DataTransformer:
    """Transforms COBOL data to PostgreSQL format."""
    
    def __init__(self, config: Dict[str, Any]):
        self.config = config
        self.field_mappings = config.get('field_mappings', {})
        self.value_transformations = config.get('value_transformations', {})
        
    def transform(self, records: Iterator[Dict[str, Any]], target_table: str) -> Iterator[Dict[str, Any]]:
        """Transform records for the target table."""
        
        mapping = self.field_mappings.get(target_table, {})
        
        for record in records:
            try:
                transformed = self._transform_record(record, mapping, target_table)
                if transformed:
                    yield transformed
            except Exception as e:
                logger.error(f"Error transforming record", error=str(e), record=record)
                continue
    
    def _transform_record(self, record: Dict[str, Any], mapping: Dict[str, str], target_table: str) -> Optional[Dict[str, Any]]:
        """Transform a single record."""
        
        result = {
            'id': str(uuid.uuid4()),
            'created_at': datetime.now().isoformat(),
        }
        
        for source_field, target_field in mapping.items():
            if source_field in record:
                value = record[source_field]
                transformed_value = self._transform_value(value, source_field, target_field, target_table)
                result[target_field] = transformed_value
                
        for key, value in record.items():
            if key.startswith('_'):
                continue
            target_key = mapping.get(key)
            if not target_key and key.lower() not in result:
                snake_key = self._to_snake_case(key)
                if snake_key not in result:
                    result[snake_key] = self._transform_value(value, key, snake_key, target_table)
        
        result = self._add_computed_fields(result, target_table, record)
        
        return result
    
    def _transform_value(self, value: Any, source_field: str, target_field: str, target_table: str) -> Any:
        """Transform a single value based on field type and mappings."""
        
        if value is None:
            return None
            
        if target_field == 'status' and 'status' in self.value_transformations:
            status_map = self.value_transformations['status']
            if isinstance(value, str) and value in status_map:
                return status_map[value]
                
        if target_field == 'transaction_type' and 'transaction_type' in self.value_transformations:
            type_map = self.value_transformations['transaction_type']
            if isinstance(value, str) and value in type_map:
                return type_map[value]
                
        if target_field == 'transaction_status' and 'transaction_status' in self.value_transformations:
            status_map = self.value_transformations['transaction_status']
            if isinstance(value, str) and value in status_map:
                return status_map[value]
        
        if 'date' in target_field.lower() and isinstance(value, str):
            return self._parse_date(value)
            
        if 'time' in target_field.lower() and isinstance(value, str):
            return self._parse_time(value)
            
        if 'timestamp' in target_field.lower() and isinstance(value, str):
            return self._parse_timestamp(value)
        
        if isinstance(value, Decimal):
            return float(value)
            
        if isinstance(value, str):
            return value.strip()
            
        return value
    
    def _parse_date(self, value: str) -> Optional[str]:
        """Parse COBOL date format (YYYYMMDD) to ISO format."""
        
        if not value or value.strip() == '' or value == '00000000':
            return None
            
        value = value.strip()
        
        if len(value) == 8 and value.isdigit():
            try:
                year = int(value[0:4])
                month = int(value[4:6])
                day = int(value[6:8])
                return date(year, month, day).isoformat()
            except ValueError:
                return None
        
        if '-' in value:
            return value
            
        return None
    
    def _parse_time(self, value: str) -> Optional[str]:
        """Parse COBOL time format (HHMMSS) to ISO format."""
        
        if not value or value.strip() == '':
            return None
            
        value = value.strip()
        
        if len(value) == 6 and value.isdigit():
            try:
                hour = int(value[0:2])
                minute = int(value[2:4])
                second = int(value[4:6])
                return time(hour, minute, second).isoformat()
            except ValueError:
                return None
        
        if ':' in value:
            return value
            
        return None
    
    def _parse_timestamp(self, value: str) -> Optional[str]:
        """Parse timestamp to ISO format."""
        
        if not value or value.strip() == '':
            return None
            
        value = value.strip()
        
        if 'T' in value or '-' in value:
            return value
        
        if len(value) >= 14 and value[:14].isdigit():
            try:
                year = int(value[0:4])
                month = int(value[4:6])
                day = int(value[6:8])
                hour = int(value[8:10])
                minute = int(value[10:12])
                second = int(value[12:14])
                return datetime(year, month, day, hour, minute, second).isoformat()
            except ValueError:
                return None
                
        return value
    
    def _to_snake_case(self, name: str) -> str:
        """Convert field name to snake_case."""
        
        result = name.replace('-', '_').replace(' ', '_')
        
        snake = ''
        for i, char in enumerate(result):
            if char.isupper() and i > 0 and result[i-1].islower():
                snake += '_'
            snake += char.lower()
            
        return snake
    
    def _add_computed_fields(self, record: Dict[str, Any], target_table: str, source_record: Dict[str, Any]) -> Dict[str, Any]:
        """Add computed fields based on target table requirements."""
        
        if target_table == 'transactions':
            if 'transaction_id' not in record or not record['transaction_id']:
                date_part = record.get('transaction_date', '').replace('-', '')
                time_part = record.get('transaction_time', '').replace(':', '')
                seq = record.get('sequence_no', '000001')
                record['transaction_id'] = f"{date_part}{time_part}{seq}"
                
            if 'total_amount' not in record:
                amount = record.get('amount', 0) or 0
                fees = record.get('fees', 0) or 0
                record['total_amount'] = amount + fees
                
        if target_table == 'positions':
            if 'average_cost' not in record or not record.get('average_cost'):
                quantity = record.get('quantity', 0) or 0
                cost_basis = record.get('cost_basis', 0) or 0
                if quantity and quantity != 0:
                    record['average_cost'] = cost_basis / quantity
                else:
                    record['average_cost'] = 0
                    
        if target_table == 'error_log':
            error_type_map = {'S': 'SYSTEM', 'A': 'APPLICATION', 'D': 'DATA'}
            if 'error_type' in record and record['error_type'] in error_type_map:
                record['error_type'] = error_type_map[record['error_type']]
                
            severity_map = {1: 'INFO', 2: 'WARNING', 3: 'ERROR', 4: 'SEVERE'}
            if 'error_severity' in record and record['error_severity'] in severity_map:
                record['error_severity'] = severity_map[record['error_severity']]
                
        return record
