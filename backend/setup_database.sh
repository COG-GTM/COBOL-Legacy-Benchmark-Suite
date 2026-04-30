#!/bin/bash
set -e

echo "Setting up database..."

echo "Running migrations..."
alembic upgrade head

echo "Seeding database with sample data..."
python seed_database.py

echo "Database setup complete!"
