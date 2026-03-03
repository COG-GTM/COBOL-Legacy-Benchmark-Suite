"""Batch processing programs for the Investment Portfolio Management System.

This package replaces the COBOL batch pipeline:
  TRNVAL00 -> POSUPD00 -> HISTLD00 -> Reporting

Each program is a Python module with a main() entry point that
can be called by the Prefect orchestration flow.
"""
