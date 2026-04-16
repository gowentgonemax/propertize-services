"""
Base Repository — encapsulates database access with connection pooling.

All Python service repositories should extend this class.
Provides query() for reads and execute() for writes, both
using the shared connection pool from db.py.
"""
import logging
from typing import Optional

import pandas as pd

from shared.db import get_connection


class BaseRepository:
    """Abstract base for all data-access classes."""

    def __init__(self):
        self.log = logging.getLogger(self.__class__.__name__)

    def query(self, sql: str, params: Optional[list] = None) -> pd.DataFrame:
        """Execute a SELECT and return a DataFrame."""
        with get_connection() as conn:
            return pd.read_sql(sql, conn, params=params)

    def execute(self, sql: str, params: Optional[tuple] = None) -> None:
        """Execute a single INSERT / UPDATE / DELETE statement."""
        with get_connection() as conn:
            with conn.cursor() as cur:
                cur.execute(sql, params)

    def execute_many(self, sql: str, params_list: list[tuple]) -> int:
        """Execute a parameterised statement for each row; return count."""
        with get_connection() as conn:
            with conn.cursor() as cur:
                for params in params_list:
                    cur.execute(sql, params)
                return len(params_list)

    def execute_batch(self, statements: list[tuple[str, tuple]]) -> None:
        """Execute multiple heterogeneous statements in one transaction."""
        with get_connection() as conn:
            with conn.cursor() as cur:
                for sql, params in statements:
                    cur.execute(sql, params)

