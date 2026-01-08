"""
Reports Routes - REST API endpoints for report generation.

This module provides API endpoints for generating reports,
integrating with the batch reporting modules.
"""

import logging
from flask import Blueprint, jsonify, request, Response
from datetime import datetime

from ...batch.reports import (
    PositionReportGenerator,
    AuditReportGenerator,
    StatisticsReportGenerator
)

logger = logging.getLogger(__name__)

reports_bp = Blueprint('reports', __name__)


@reports_bp.route('/position', methods=['GET'])
def generate_position_report():
    """
    Generate position report.
    
    Query Parameters:
        portfolio_ids: Comma-separated portfolio IDs (optional)
        format: Output format (json, text) - default json
        
    Returns:
        Position report in requested format
    """
    portfolio_ids_param = request.args.get('portfolio_ids')
    output_format = request.args.get('format', 'json')
    
    portfolio_ids = None
    if portfolio_ids_param:
        portfolio_ids = [p.strip() for p in portfolio_ids_param.split(',')]
    
    logger.info(f"Generating position report, portfolios: {portfolio_ids}")
    
    try:
        generator = PositionReportGenerator()
        result = generator.generate_report(portfolio_ids=portfolio_ids)
        generator.close()
        
        if output_format == 'text':
            return Response(
                result.report_content,
                mimetype='text/plain',
                headers={'Content-Disposition': 'attachment; filename=position_report.txt'}
            )
        
        return jsonify({
            'report_date': result.report_date,
            'portfolios_processed': result.portfolios_processed,
            'positions_processed': result.positions_processed,
            'total_market_value': str(result.total_market_value),
            'return_code': result.return_code,
            'report_content': result.report_content if output_format == 'full' else None
        }), 200
        
    except Exception as e:
        logger.error(f"Error generating position report: {e}")
        return jsonify({'error': 'Report generation failed', 'message': str(e)}), 500


@reports_bp.route('/audit', methods=['GET'])
def generate_audit_report():
    """
    Generate audit report.
    
    Query Parameters:
        start_date: Start date (YYYYMMDD)
        end_date: End date (YYYYMMDD)
        portfolio_ids: Comma-separated portfolio IDs (optional)
        format: Output format (json, text) - default json
        
    Returns:
        Audit report in requested format
    """
    start_date = request.args.get('start_date')
    end_date = request.args.get('end_date')
    portfolio_ids_param = request.args.get('portfolio_ids')
    output_format = request.args.get('format', 'json')
    
    portfolio_ids = None
    if portfolio_ids_param:
        portfolio_ids = [p.strip() for p in portfolio_ids_param.split(',')]
    
    logger.info(f"Generating audit report, {start_date} to {end_date}")
    
    try:
        generator = AuditReportGenerator()
        result = generator.generate_report(
            start_date=start_date,
            end_date=end_date,
            portfolio_ids=portfolio_ids
        )
        generator.close()
        
        if output_format == 'text':
            return Response(
                result.report_content,
                mimetype='text/plain',
                headers={'Content-Disposition': 'attachment; filename=audit_report.txt'}
            )
        
        return jsonify({
            'report_date': result.report_date,
            'start_date': result.start_date,
            'end_date': result.end_date,
            'entries_processed': result.entries_processed,
            'return_code': result.return_code,
            'report_content': result.report_content if output_format == 'full' else None
        }), 200
        
    except Exception as e:
        logger.error(f"Error generating audit report: {e}")
        return jsonify({'error': 'Report generation failed', 'message': str(e)}), 500


@reports_bp.route('/statistics', methods=['GET'])
def generate_statistics_report():
    """
    Generate statistics report.
    
    Query Parameters:
        period_start: Start date (YYYYMMDD)
        period_end: End date (YYYYMMDD)
        format: Output format (json, text) - default json
        
    Returns:
        Statistics report in requested format
    """
    period_start = request.args.get('period_start')
    period_end = request.args.get('period_end')
    output_format = request.args.get('format', 'json')
    
    logger.info(f"Generating statistics report, {period_start} to {period_end}")
    
    try:
        generator = StatisticsReportGenerator()
        result = generator.generate_report(
            period_start=period_start,
            period_end=period_end
        )
        generator.close()
        
        if output_format == 'text':
            return Response(
                result.report_content,
                mimetype='text/plain',
                headers={'Content-Disposition': 'attachment; filename=statistics_report.txt'}
            )
        
        # Build JSON response with statistics
        job_stats_list = []
        for js in result.job_stats:
            job_stats_list.append({
                'job_name': js.job_name,
                'run_count': js.run_count,
                'success_count': js.success_count,
                'error_count': js.error_count,
                'avg_records_read': js.avg_records_read,
                'avg_records_written': js.avg_records_written,
                'last_run_date': js.last_run_date,
                'last_return_code': js.last_return_code
            })
        
        return jsonify({
            'report_date': result.report_date,
            'period_start': result.period_start,
            'period_end': result.period_end,
            'job_statistics': job_stats_list,
            'system_statistics': {
                'total_portfolios': result.system_stats.total_portfolios,
                'total_positions': result.system_stats.total_positions,
                'total_transactions': result.system_stats.total_transactions,
                'total_history_records': result.system_stats.total_history_records,
                'total_market_value': str(result.system_stats.total_market_value),
                'active_positions': result.system_stats.active_positions,
                'closed_positions': result.system_stats.closed_positions
            },
            'return_code': result.return_code,
            'report_content': result.report_content if output_format == 'full' else None
        }), 200
        
    except Exception as e:
        logger.error(f"Error generating statistics report: {e}")
        return jsonify({'error': 'Report generation failed', 'message': str(e)}), 500
