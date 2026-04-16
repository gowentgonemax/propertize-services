"""
Shared request/response logging middleware for all Python microservices.

Usage in any FastAPI service::

    from shared.logging_middleware import add_logging_middleware
    add_logging_middleware(app, service_name="report-service")

Every inbound request is logged at INFO with:
  - HTTP method + path + query string
  - Client IP (respects X-Forwarded-For)
  - Correlation ID (X-Correlation-Id header, or generated UUID)
  - User ID (X-User-Id header from gateway)

Every outbound response is logged at INFO/WARNING/ERROR based on status code.
Slow responses (>3 s) are additionally flagged at WARNING.
Health-check paths (/health, /actuator/health) are suppressed to avoid noise.

Sensitive headers (Authorization, Cookie, X-Api-Key) are never logged.
"""

import logging
import time
import uuid
from typing import Callable, Awaitable

from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import Response

log = logging.getLogger("propertize.access")

# Requests on these paths are not logged at INFO to reduce noise
_EXCLUDED_PATHS = {"/health", "/actuator/health", "/favicon.ico"}
# Threshold for "slow request" warning
_SLOW_REQUEST_S  = 3.0
# Headers whose values must never appear in logs
_MASKED_HEADERS  = {"authorization", "cookie", "set-cookie", "x-api-key"}


class RequestResponseLoggingMiddleware(BaseHTTPMiddleware):
    """ASGI middleware that logs every HTTP exchange."""

    def __init__(self, app, service_name: str = "python-service") -> None:
        super().__init__(app)
        self.service_name = service_name

    async def dispatch(
        self,
        request: Request,
        call_next: Callable[[Request], Awaitable[Response]],
    ) -> Response:
        path = request.url.path

        if path in _EXCLUDED_PATHS:
            return await call_next(request)

        correlation_id = (
            request.headers.get("x-correlation-id")
            or request.headers.get("x-correlation-ID")
            or str(uuid.uuid4())
        )
        user_id = request.headers.get("x-user-id", "anonymous")
        org_id  = request.headers.get("x-organization-id")
        client_ip = _extract_client_ip(request)

        query = str(request.url.query)
        full_path = f"{path}?{query}" if query else path

        log.info(
            "▶ REQUEST | service=%s | %s %s | ip=%s | correlationId=%s | userId=%s%s",
            self.service_name,
            request.method,
            full_path,
            client_ip,
            correlation_id,
            user_id,
            f" | orgId={org_id}" if org_id else "",
        )

        start = time.perf_counter()
        response: Response = await call_next(request)
        duration_ms = int((time.perf_counter() - start) * 1000)

        status = response.status_code

        log_fn = log.info
        if status >= 500:
            log_fn = log.error
        elif status >= 400:
            log_fn = log.warning

        log_fn(
            "◀ RESPONSE | service=%s | %s %s | status=%s | duration=%dms | correlationId=%s",
            self.service_name,
            request.method,
            path,
            status,
            duration_ms,
            correlation_id,
        )

        if duration_ms / 1000 > _SLOW_REQUEST_S:
            log.warning(
                "⚠ SLOW REQUEST | service=%s | %s %s took %dms [correlationId=%s]",
                self.service_name,
                request.method,
                path,
                duration_ms,
                correlation_id,
            )

        # Propagate correlation ID in the response for client tracing
        response.headers["x-correlation-id"] = correlation_id
        return response


def add_logging_middleware(app, service_name: str) -> None:
    """
    Convenience function — call once at application startup.

    Example::

        app = FastAPI(...)
        add_logging_middleware(app, service_name="vendor-matching")
    """
    app.add_middleware(RequestResponseLoggingMiddleware, service_name=service_name)


# ── Helpers ───────────────────────────────────────────────────────────────────

def _extract_client_ip(request: Request) -> str:
    forwarded_for = request.headers.get("x-forwarded-for")
    if forwarded_for:
        return forwarded_for.split(",")[0].strip()
    real_ip = request.headers.get("x-real-ip")
    if real_ip:
        return real_ip
    if request.client:
        return request.client.host
    return "unknown"

