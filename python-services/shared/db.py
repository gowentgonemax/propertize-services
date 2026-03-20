"""
Shared PostgreSQL connection management using a connection pool.
All Python services import get_connection() from here.
"""
import psycopg2
from psycopg2 import pool
from contextlib import contextmanager
import sys
import os

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from shared.config import DATABASE_URL

_pool = None


def init_pool(minconn: int = 2, maxconn: int = 10):
    global _pool
    _pool = psycopg2.pool.ThreadedConnectionPool(minconn, maxconn, DATABASE_URL)
    return _pool


@contextmanager
def get_connection():
    """Context manager that checks out and returns a connection from the pool."""
    global _pool
    if _pool is None:
        init_pool()
    conn = _pool.getconn()
    try:
        yield conn
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        _pool.putconn(conn)
