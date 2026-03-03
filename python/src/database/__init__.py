"""Database engine and session factory.

Usage::

    from database import SessionLocal, engine

    with SessionLocal() as session:
        ...
"""

from sqlalchemy.orm import sessionmaker

from .connection import create_db_engine

engine = create_db_engine()
SessionLocal = sessionmaker(bind=engine, expire_on_commit=False)
