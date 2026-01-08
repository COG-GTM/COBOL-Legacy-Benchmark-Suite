"""
Portfolio Routes - Migrated from COBOL INQPORT program.

This module implements REST API endpoints for portfolio position inquiries,
replacing the CICS INQPORT program functionality.

Original CICS Program: src/programs/online/INQPORT.cbl
"""

import logging
from flask import Blueprint, jsonify, request
from decimal import Decimal

from ...database.connection import session_scope, get_session
from ...models.position import Position, PositionStatus
from ..security import require_auth, require_permission

logger = logging.getLogger(__name__)

portfolio_bp = Blueprint('portfolio', __name__)


@portfolio_bp.route('/<portfolio_id>/positions', methods=['GET'])
@require_auth
def get_portfolio_positions(portfolio_id: str):
    """
    Get all positions for a portfolio.
    Replaces CICS INQPORT 2100-INQUIRY-PORTFOLIO paragraph.
    
    Args:
        portfolio_id: Portfolio identifier
        
    Returns:
        JSON list of positions
    """
    logger.info(f"Portfolio inquiry: {portfolio_id}")
    
    session = get_session()
    try:
        # Query positions
        positions = session.query(Position).filter(
            Position.portfolio_id == portfolio_id,
            Position.status == PositionStatus.ACTIVE.value
        ).all()
        
        if not positions:
            return jsonify({
                'portfolio_id': portfolio_id,
                'positions': [],
                'message': 'No positions found'
            }), 200
        
        # Calculate totals
        total_cost_basis = Decimal('0')
        total_market_value = Decimal('0')
        
        position_list = []
        for pos in positions:
            cost_basis = Decimal(str(pos.cost_basis)) if pos.cost_basis else Decimal('0')
            market_value = Decimal(str(pos.market_value)) if pos.market_value else Decimal('0')
            unrealized_gl = market_value - cost_basis
            
            position_list.append({
                'investment_id': pos.investment_id,
                'quantity': str(pos.quantity),
                'cost_basis': str(cost_basis),
                'market_value': str(market_value),
                'unrealized_gain_loss': str(unrealized_gl),
                'currency': pos.currency,
                'status': pos.status,
                'date': pos.date
            })
            
            total_cost_basis += cost_basis
            total_market_value += market_value
        
        return jsonify({
            'portfolio_id': portfolio_id,
            'positions': position_list,
            'summary': {
                'position_count': len(position_list),
                'total_cost_basis': str(total_cost_basis),
                'total_market_value': str(total_market_value),
                'total_unrealized_gain_loss': str(total_market_value - total_cost_basis)
            }
        }), 200
        
    except Exception as e:
        logger.error(f"Error querying portfolio {portfolio_id}: {e}")
        return jsonify({'error': 'Database error', 'message': str(e)}), 500
    finally:
        session.close()


@portfolio_bp.route('/<portfolio_id>/positions/<investment_id>', methods=['GET'])
@require_auth
def get_position_detail(portfolio_id: str, investment_id: str):
    """
    Get detail for a specific position.
    Replaces CICS INQPORT 2200-INQUIRY-POSITION paragraph.
    
    Args:
        portfolio_id: Portfolio identifier
        investment_id: Investment identifier
        
    Returns:
        JSON position detail
    """
    logger.info(f"Position inquiry: {portfolio_id}/{investment_id}")
    
    session = get_session()
    try:
        position = session.query(Position).filter(
            Position.portfolio_id == portfolio_id,
            Position.investment_id == investment_id
        ).first()
        
        if not position:
            return jsonify({
                'error': 'Not Found',
                'message': f'Position not found: {portfolio_id}/{investment_id}'
            }), 404
        
        cost_basis = Decimal(str(position.cost_basis)) if position.cost_basis else Decimal('0')
        market_value = Decimal(str(position.market_value)) if position.market_value else Decimal('0')
        quantity = Decimal(str(position.quantity)) if position.quantity else Decimal('0')
        
        unrealized_gl = market_value - cost_basis
        avg_cost = cost_basis / quantity if quantity != 0 else Decimal('0')
        current_price = market_value / quantity if quantity != 0 else Decimal('0')
        
        return jsonify({
            'portfolio_id': position.portfolio_id,
            'investment_id': position.investment_id,
            'date': position.date,
            'quantity': str(quantity),
            'cost_basis': str(cost_basis),
            'market_value': str(market_value),
            'unrealized_gain_loss': str(unrealized_gl),
            'average_cost': str(avg_cost),
            'current_price': str(current_price),
            'currency': position.currency,
            'status': position.status,
            'last_maint_date': position.last_maint_date.isoformat() if position.last_maint_date else None,
            'last_maint_user': position.last_maint_user
        }), 200
        
    except Exception as e:
        logger.error(f"Error querying position {portfolio_id}/{investment_id}: {e}")
        return jsonify({'error': 'Database error', 'message': str(e)}), 500
    finally:
        session.close()


@portfolio_bp.route('/<portfolio_id>/summary', methods=['GET'])
@require_auth
def get_portfolio_summary(portfolio_id: str):
    """
    Get portfolio summary.
    
    Args:
        portfolio_id: Portfolio identifier
        
    Returns:
        JSON portfolio summary
    """
    logger.info(f"Portfolio summary: {portfolio_id}")
    
    session = get_session()
    try:
        positions = session.query(Position).filter(
            Position.portfolio_id == portfolio_id,
            Position.status == PositionStatus.ACTIVE.value
        ).all()
        
        total_cost_basis = Decimal('0')
        total_market_value = Decimal('0')
        investments = set()
        
        for pos in positions:
            cost_basis = Decimal(str(pos.cost_basis)) if pos.cost_basis else Decimal('0')
            market_value = Decimal(str(pos.market_value)) if pos.market_value else Decimal('0')
            total_cost_basis += cost_basis
            total_market_value += market_value
            investments.add(pos.investment_id)
        
        unrealized_gl = total_market_value - total_cost_basis
        gl_percent = (unrealized_gl / total_cost_basis * 100) if total_cost_basis != 0 else Decimal('0')
        
        return jsonify({
            'portfolio_id': portfolio_id,
            'position_count': len(positions),
            'unique_investments': len(investments),
            'total_cost_basis': str(total_cost_basis),
            'total_market_value': str(total_market_value),
            'total_unrealized_gain_loss': str(unrealized_gl),
            'gain_loss_percent': str(gl_percent)
        }), 200
        
    except Exception as e:
        logger.error(f"Error getting portfolio summary {portfolio_id}: {e}")
        return jsonify({'error': 'Database error', 'message': str(e)}), 500
    finally:
        session.close()


@portfolio_bp.route('/list', methods=['GET'])
@require_auth
def list_portfolios():
    """
    List all portfolios.
    
    Returns:
        JSON list of portfolio IDs
    """
    session = get_session()
    try:
        # Get distinct portfolio IDs
        portfolios = session.query(Position.portfolio_id).distinct().all()
        
        portfolio_list = [p[0] for p in portfolios]
        
        return jsonify({
            'portfolios': portfolio_list,
            'count': len(portfolio_list)
        }), 200
        
    except Exception as e:
        logger.error(f"Error listing portfolios: {e}")
        return jsonify({'error': 'Database error', 'message': str(e)}), 500
    finally:
        session.close()
