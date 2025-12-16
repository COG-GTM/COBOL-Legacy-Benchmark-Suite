"""
VSAM File Parser for COBOL Legacy Migration
Parses VSAM KSDS and ESDS files based on COBOL copybook definitions.
"""

import struct
from datetime import datetime, date
from decimal import Decimal
from pathlib import Path
from typing import Dict, Any, Iterator, Optional
import structlog

logger = structlog.get_logger(__name__)


class VSAMParser:
    """Parser for VSAM files based on COBOL record layouts."""
    
    COBOL_FIELD_TYPES = {
        'PIC X': 'string',
        'PIC 9': 'numeric',
        'PIC S9': 'signed_numeric',
        'COMP': 'binary',
        'COMP-3': 'packed_decimal',
    }
    
    def __init__(self, config: Dict[str, Any]):
        self.config = config
        self.source_path = config.get('source_path', '')
        self.record_length = config.get('record_length', 0)
        self.key_length = config.get('key_length', 0)
        self.record_layout = self._get_record_layout(config.get('name', ''))
        
    def _get_record_layout(self, file_name: str) -> Dict[str, Any]:
        """Get record layout based on file name (from COBOL copybooks)."""
        
        layouts = {
            'POSFILE': {
                'fields': [
                    {'name': 'POS-PORTFOLIO-ID', 'type': 'string', 'offset': 0, 'length': 8},
                    {'name': 'POS-DATE', 'type': 'string', 'offset': 8, 'length': 8},
                    {'name': 'POS-INVESTMENT-ID', 'type': 'string', 'offset': 16, 'length': 10},
                    {'name': 'POS-QUANTITY', 'type': 'packed_decimal', 'offset': 26, 'length': 8, 'decimals': 4},
                    {'name': 'POS-COST-BASIS', 'type': 'packed_decimal', 'offset': 34, 'length': 8, 'decimals': 2},
                    {'name': 'POS-MARKET-VALUE', 'type': 'packed_decimal', 'offset': 42, 'length': 8, 'decimals': 2},
                    {'name': 'POS-CURRENCY', 'type': 'string', 'offset': 50, 'length': 3},
                    {'name': 'POS-STATUS', 'type': 'string', 'offset': 53, 'length': 1},
                    {'name': 'POS-LAST-MAINT-DATE', 'type': 'string', 'offset': 54, 'length': 26},
                    {'name': 'POS-LAST-MAINT-USER', 'type': 'string', 'offset': 80, 'length': 8},
                ]
            },
            'TRANHIST': {
                'fields': [
                    {'name': 'TRN-DATE', 'type': 'string', 'offset': 0, 'length': 8},
                    {'name': 'TRN-TIME', 'type': 'string', 'offset': 8, 'length': 6},
                    {'name': 'TRN-PORTFOLIO-ID', 'type': 'string', 'offset': 14, 'length': 8},
                    {'name': 'TRN-SEQUENCE-NO', 'type': 'string', 'offset': 22, 'length': 6},
                    {'name': 'TRN-INVESTMENT-ID', 'type': 'string', 'offset': 28, 'length': 10},
                    {'name': 'TRN-TYPE', 'type': 'string', 'offset': 38, 'length': 2},
                    {'name': 'TRN-QUANTITY', 'type': 'packed_decimal', 'offset': 40, 'length': 8, 'decimals': 4},
                    {'name': 'TRN-PRICE', 'type': 'packed_decimal', 'offset': 48, 'length': 8, 'decimals': 4},
                    {'name': 'TRN-AMOUNT', 'type': 'packed_decimal', 'offset': 56, 'length': 8, 'decimals': 2},
                    {'name': 'TRN-CURRENCY', 'type': 'string', 'offset': 64, 'length': 3},
                    {'name': 'TRN-STATUS', 'type': 'string', 'offset': 67, 'length': 1},
                    {'name': 'TRN-PROCESS-DATE', 'type': 'string', 'offset': 68, 'length': 26},
                    {'name': 'TRN-PROCESS-USER', 'type': 'string', 'offset': 94, 'length': 8},
                ]
            },
            'PORTMSTR': {
                'fields': [
                    {'name': 'PORTFOLIO-ID', 'type': 'string', 'offset': 0, 'length': 8},
                    {'name': 'ACCOUNT-TYPE', 'type': 'string', 'offset': 8, 'length': 2},
                    {'name': 'BRANCH-ID', 'type': 'string', 'offset': 10, 'length': 2},
                    {'name': 'CLIENT-ID', 'type': 'string', 'offset': 12, 'length': 10},
                    {'name': 'PORTFOLIO-NAME', 'type': 'string', 'offset': 22, 'length': 50},
                    {'name': 'CURRENCY-CODE', 'type': 'string', 'offset': 72, 'length': 3},
                    {'name': 'RISK-LEVEL', 'type': 'string', 'offset': 75, 'length': 1},
                    {'name': 'STATUS', 'type': 'string', 'offset': 76, 'length': 1},
                    {'name': 'OPEN-DATE', 'type': 'string', 'offset': 77, 'length': 8},
                    {'name': 'CLOSE-DATE', 'type': 'string', 'offset': 85, 'length': 8},
                ]
            },
            'BCHCTL': {
                'fields': [
                    {'name': 'BCH-PROCESS-DATE', 'type': 'string', 'offset': 0, 'length': 8},
                    {'name': 'BCH-PROCESS-ID', 'type': 'string', 'offset': 8, 'length': 8},
                    {'name': 'BCH-STATUS', 'type': 'string', 'offset': 16, 'length': 1},
                    {'name': 'BCH-START-TIME', 'type': 'string', 'offset': 17, 'length': 8},
                    {'name': 'BCH-END-TIME', 'type': 'string', 'offset': 25, 'length': 8},
                    {'name': 'BCH-RECORD-COUNT', 'type': 'numeric', 'offset': 33, 'length': 9},
                    {'name': 'BCH-ERROR-COUNT', 'type': 'numeric', 'offset': 42, 'length': 9},
                    {'name': 'BCH-LAST-POS', 'type': 'numeric', 'offset': 51, 'length': 9},
                    {'name': 'BCH-RETURN-CODE', 'type': 'numeric', 'offset': 60, 'length': 4},
                    {'name': 'BCH-MESSAGE', 'type': 'string', 'offset': 64, 'length': 50},
                ]
            }
        }
        
        return layouts.get(file_name, {'fields': []})
    
    def parse(self, start_from: int = 0) -> Iterator[Dict[str, Any]]:
        """Parse VSAM file and yield records as dictionaries."""
        
        if not self.source_path or self.source_path.startswith('${'):
            logger.warning("VSAM source path not configured, generating sample data")
            yield from self._generate_sample_data()
            return
            
        source_file = Path(self.source_path)
        if not source_file.exists():
            logger.warning(f"VSAM file not found: {self.source_path}")
            yield from self._generate_sample_data()
            return
            
        logger.info(f"Parsing VSAM file: {self.source_path}")
        
        with open(source_file, 'rb') as f:
            if start_from > 0:
                f.seek(start_from * self.record_length)
                
            record_num = start_from
            while True:
                record_data = f.read(self.record_length)
                if not record_data or len(record_data) < self.record_length:
                    break
                    
                try:
                    parsed_record = self._parse_record(record_data)
                    parsed_record['_record_number'] = record_num
                    yield parsed_record
                    record_num += 1
                except Exception as e:
                    logger.error(f"Error parsing record {record_num}", error=str(e))
                    continue
    
    def _parse_record(self, record_data: bytes) -> Dict[str, Any]:
        """Parse a single VSAM record based on layout."""
        
        result = {}
        
        for field in self.record_layout.get('fields', []):
            field_name = field['name']
            field_type = field['type']
            offset = field['offset']
            length = field['length']
            decimals = field.get('decimals', 0)
            
            field_data = record_data[offset:offset + length]
            
            if field_type == 'string':
                result[field_name] = field_data.decode('cp037', errors='replace').strip()
            elif field_type == 'numeric':
                result[field_name] = self._parse_numeric(field_data)
            elif field_type == 'signed_numeric':
                result[field_name] = self._parse_signed_numeric(field_data)
            elif field_type == 'packed_decimal':
                result[field_name] = self._parse_packed_decimal(field_data, decimals)
            elif field_type == 'binary':
                result[field_name] = self._parse_binary(field_data)
            else:
                result[field_name] = field_data.hex()
                
        return result
    
    def _parse_numeric(self, data: bytes) -> int:
        """Parse COBOL numeric (PIC 9) field."""
        try:
            return int(data.decode('cp037').strip() or '0')
        except (ValueError, UnicodeDecodeError):
            return 0
    
    def _parse_signed_numeric(self, data: bytes) -> int:
        """Parse COBOL signed numeric (PIC S9) field."""
        try:
            decoded = data.decode('cp037').strip()
            if not decoded:
                return 0
            sign_char = decoded[-1]
            if sign_char in 'JKLMNOPQR}':
                return -int(decoded[:-1] + str('JKLMNOPQR}'.index(sign_char)))
            elif sign_char in 'ABCDEFGHI{':
                return int(decoded[:-1] + str('ABCDEFGHI{'.index(sign_char)))
            return int(decoded)
        except (ValueError, UnicodeDecodeError):
            return 0
    
    def _parse_packed_decimal(self, data: bytes, decimals: int = 0) -> Decimal:
        """Parse COBOL COMP-3 (packed decimal) field."""
        try:
            digits = ''
            for byte in data[:-1]:
                digits += f'{byte:02x}'
            last_byte = data[-1]
            digits += f'{last_byte >> 4:x}'
            sign = last_byte & 0x0f
            
            if sign in (0x0d, 0x0b):
                digits = '-' + digits
                
            if decimals > 0:
                integer_part = digits[:-decimals] or '0'
                decimal_part = digits[-decimals:]
                return Decimal(f'{integer_part}.{decimal_part}')
            return Decimal(digits)
        except Exception:
            return Decimal('0')
    
    def _parse_binary(self, data: bytes) -> int:
        """Parse COBOL COMP (binary) field."""
        return int.from_bytes(data, byteorder='big', signed=True)
    
    def _generate_sample_data(self) -> Iterator[Dict[str, Any]]:
        """Generate sample data when VSAM file is not available."""
        
        file_name = self.config.get('name', '')
        
        if file_name == 'POSFILE':
            sample_records = [
                {
                    'POS-PORTFOLIO-ID': 'PORT0001',
                    'POS-DATE': '20240315',
                    'POS-INVESTMENT-ID': 'AAPL',
                    'POS-QUANTITY': Decimal('100.0000'),
                    'POS-COST-BASIS': Decimal('15000.00'),
                    'POS-MARKET-VALUE': Decimal('17500.00'),
                    'POS-CURRENCY': 'USD',
                    'POS-STATUS': 'A',
                    'POS-LAST-MAINT-DATE': datetime.now().isoformat(),
                    'POS-LAST-MAINT-USER': 'BATCH',
                },
            ]
        elif file_name == 'TRANHIST':
            sample_records = [
                {
                    'TRN-DATE': '20240315',
                    'TRN-TIME': '093000',
                    'TRN-PORTFOLIO-ID': 'PORT0001',
                    'TRN-SEQUENCE-NO': '000001',
                    'TRN-INVESTMENT-ID': 'AAPL',
                    'TRN-TYPE': 'BU',
                    'TRN-QUANTITY': Decimal('100.0000'),
                    'TRN-PRICE': Decimal('150.0000'),
                    'TRN-AMOUNT': Decimal('15000.00'),
                    'TRN-CURRENCY': 'USD',
                    'TRN-STATUS': 'D',
                    'TRN-PROCESS-DATE': datetime.now().isoformat(),
                    'TRN-PROCESS-USER': 'BATCH',
                },
            ]
        else:
            sample_records = []
            
        for i, record in enumerate(sample_records):
            record['_record_number'] = i
            yield record
