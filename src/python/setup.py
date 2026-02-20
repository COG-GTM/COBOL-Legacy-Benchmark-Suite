"""Setup configuration for portfolio_management package."""

from setuptools import setup, find_packages

setup(
    name="portfolio-management",
    version="1.0.0",
    description="Investment Portfolio Management System - migrated from COBOL",
    packages=find_packages(),
    python_requires=">=3.10",
    classifiers=[
        "Programming Language :: Python :: 3",
        "Programming Language :: Python :: 3.10",
        "Programming Language :: Python :: 3.11",
        "Programming Language :: Python :: 3.12",
    ],
)
