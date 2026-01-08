"""
History Routes - Migrated from COBOL INQHIST program.

This module implements REST API endpoints for transaction history inquiries,
replacing the CICS INQHIST program functionality.

Original CICS Program: src/programs/online/INQHIST.cbl
"""

import logging
from flask import Blueprint, jsonify, request
from datetime import datetime

from ...database.connection import get_session
from ...models.transaction import Transaction, TransactionStatus
from ...models.history import History, HistoryRecordType

logger = logging.getLogger(__name__)

history_bp = Blueprint('history', __name__)


@history_bp.route('/<portfolio_id>/transactions', methods=['GET'])
def get_transaction_history(portfolio_id: str):
    """
    Get transaction history for a portfolio.
    Replaces CICS INQHIST 2100-INQUIRY-HISTORY paragraph.
    
    Args:
        portfolio_id: Portfolio identifier
        
    Query Parameters:
        start_date: Start date (YYYYMMDD)
        end_date: End date (YYYYMMDD)
        investment_id: Filter by investment
        transaction_type: Filter by type (BU, SL, TR, FE)
        limit: Maximum records (default 100)
        
    Returns:
        JSON list of transactions
    """
    logger.info(f"Transaction history inquiry: {portfolio_id}")
    
    # Get query parameters
    start_date = request.args.get('start_date')
    end_date = request.args.get('end_date')
    investment_id = request.args.get('investment_id')
    transaction_type = request.args.get('transaction_type')
    limit = int(request.args.get('limit', 100))
    
    session = get_session()
    try:
        query = session.query(Transaction).filter(
            Transaction.portfolio_id == portfolio_id
        )
        
        if start_date:
            query = query.filter(Transaction.date >= start_date)
        
        if end_date:
            query = query.filter(Transaction.date <= end_date)
        
        if investment_id:
            query = query.filter(Transaction.investment_id == investment_id)
        
        if transaction_type:
            query = query.filter(Transaction.transaction_type == transaction_type)
        
        query = query.order_by(Transaction.date.desc(), Transaction.time.desc())
        query = query.limit(limit)
        
        transactions = query.all()
        
        transaction_list = []
        for txn in transactions:
            transaction_list.append({
                'date': txn.date,
                'time': txn.time,
                'sequence_no': txn.sequence_no,
                'investment_id': txn.investment_id,
                'transaction_type': txn.transaction_type,
                'quantity': str(txn.quantity),
                'price': str(txn.price),
                'amount': str(txn.amount),
                'currency': txn.currency,
                'status': txn.status,
                'process_date': txn.process_date.isoformat() if txn.process_date else None,
                'process_user': txn.process_user
            })
        
        return jsonify({
            'portfolio_id': portfolio_id,
            'transactions': transaction_list,
            'count': len(transaction_list),
            'filters': {
                'start_date': start_date,
                'end_date': end_date,
                'investment_id': investment_id,
                'transaction_type': transaction_type
            }
        }), 200
        
    except Exception as e:
        logger.error(f"Error querying transaction history {portfolio_id}: {e}")
        return jsonify({'error': 'Database error', 'message': str(e)}), 500
    finally:
        session.close()


@history_bp.route('/<portfolio_id>/audit', methods=['GET'])
def get_audit_history(portfolio_id: str):
    """
    Get audit history for a portfolio.
    
    Args:
        portfolio_id: Portfolio identifier
        
    Query Parameters:
        start_date: Start date (YYYYMMDD)
        end_date: End date (YYYYMMDD)
        record_type: Filter by type (PT, PS, TR)
        limit: Maximum records (default 100)
        
    Returns:
        JSON list of audit records
    """
    logger.info(f"Audit history inquiry: {portfolio_id}")
    
    start_date = request.args.get('start_date')
    end_date = request.args.get('end_date')
    record_type = request.args.get('record_type')
    limit = int(request.args.get('limit', 100))
    
    session = get_session()
    try:
        query = session.query(History).filter(
            History.portfolio_id == portfolio_id
        )
        
        if start_date:
            query = query.filter(History.date >= start_date)
        
        if end_date:
            query = query.filter(History.date <= end_date)
        
        if record_type:
            query = query.filter(History.record_type == record_type)
        
        query = query.order_by(History.date.desc(), History.time.desc())
        query = query.limit(limit)
        
        records = query.all()
        
        audit_list = []
        for rec in records:
            audit_list.append({
                'date': rec.date,
                'time': rec.time,
                'seq_no': rec.seq_no,
                'record_type': rec.record_type,
                'action_code': rec.action_code,
                'before_image': rec.before_image,
                'after_image': rec.after_image,
                'reason_code': rec.reason_code,
                'process_date': rec.process_date.isoformat() if rec.process_date else None,
                'process_user': rec.process_user
            })
        
        return jsonify({
            'portfolio_id': portfolio_id,
            'audit_records': audit_list,
            'count': len(audit_list)
        }), 200
        
    except Exception as e:
        logger.error(f"Error querying audit history {portfolio_id}: {e}")
        return jsonify({'error': 'Database error', 'message': str(e)}), 500
    finally:
        session.close()


@history_bp.route('/transaction/<date>/<sequence_no>', methods=['GET'])
def get_transaction_detail(date: str, sequence_no: str):
    """
    Get detail for a specific transaction.
    
    Args:
        date: Transaction date (YYYYMMDD)
        sequence_no: Transaction sequence number
        
    Returns:
        JSON transaction detail
    """
    logger.info(f"Transaction detail inquiry: {date}/{sequence_no}")
    
    session = get_session()
    try:
        transaction = session.query(Transaction).filter(
            Transaction.date == date,
            Transaction.sequence_no == sequence_no
        ).first()
        
        if not transaction:
            return jsonify({
                'error': 'Not Found',
                'message': f'Transaction not found: {date}/{sequence_no}'
            }), 404
        
        return jsonify({
            'portfolio_id': transaction.portfolio_id,
            'date': transaction.date,
            'time': transaction.time,
            'sequence_no': transaction.sequence_no,
            'investment_id': transaction.investment_id,
            'transaction_type': transaction.transaction_type,
            'quantity': str(transaction.quantity),
            'price': str(transaction.price),
            'amount': str(transaction.amount),
            'currency': transaction.currency,
            'status': transaction.status,
            'process_date': transaction.process_date.isoformat() if transaction.process_date else None,
            'process_user': transaction.process_user
        }), 200
        
    except Exception as e:
        logger.error(f"Error querying transaction {date}/{sequence_no}: {e}")
        return jsonify({'error': 'Database error', 'message': str(e)}), 500
    finally:
        session.close()
