"""
Base Service — abstract foundation for all Python microservice business logic.

Provides structured logging, health-check template, and a standard
constructor that accepts one or more repositories.
"""
import logging
from abc import ABC, abstractmethod
from typing import Any


class BaseService(ABC):
    """Abstract base for all service classes."""

    def __init__(self):
        self.log = logging.getLogger(self.__class__.__name__)

    @abstractmethod
    def service_name(self) -> str:
        """Return the human-readable name of this service."""
        ...

    def health_check(self) -> dict[str, Any]:
        """Default health-check response; override to add component checks."""
        return {"status": "UP", "service": self.service_name()}

