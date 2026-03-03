"""Reporting modules for the Investment Portfolio Management System.

Replaces COBOL report programs:
  - RPTPOS00 -> position_report.py
  - RPTAUD00 -> audit_report.py
  - RPTSTA00 -> statistics_report.py

Uses pandas + jinja2 for report generation instead of
132-byte fixed-width COBOL report output.
"""
