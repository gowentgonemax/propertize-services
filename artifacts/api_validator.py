#!/usr/bin/env python3
"""
api_validator.py — Comprehensive REST API End-to-End Validator
==============================================================
Loads endpoint definitions from a JSON or YAML file, auto-authenticates,
resolves {{VARIABLE}} placeholders, runs every request with retry logic,
and prints a colour-coded summary.

Usage:
    python3 api_validator.py --endpoints endpoints.json [options]

Options:
    --endpoints FILE         Endpoint config file  (default: endpoints.json)
    --base-url URL           Override base URL from config
    --token TOKEN            Bearer token — skips auto-login
    --env-file FILE          .env file to load  (default: .env)
    --var KEY=VAL            Set/override a variable (repeatable)
    --tags TAG[,TAG]         Run only endpoints matching these tags
    --skip-tags TAG[,TAG]    Skip endpoints matching these tags
    --fail-fast              Stop on first failure
    --retries N              Max retries per endpoint  (default: 2)
    --retry-delay SECS       Base delay between retries  (default: 1.0)
    --timeout SECS           Request timeout in seconds  (default: 30)
    --output FILE            JSON report path  (default: api_report.json)
    --log-file FILE          Detailed log path  (default: api_validator.log)
    --no-color               Disable ANSI colour output
    --verbose                Print response bodies for passing requests too
    --dry-run                Print what would be sent without executing
    --version                Print version and exit
"""

import argparse
import json
import logging
import os
import re
import sys
import time
import uuid
from copy import deepcopy
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

# Optional YAML support
try:
    import yaml
    _YAML_OK = True
except ImportError:
    _YAML_OK = False

# ─── Constants ────────────────────────────────────────────────────────────────

VERSION = "1.0.0"
SUCCESS_STATUSES = {200, 201, 202, 204}
DEFAULT_TIMEOUT = 45
DEFAULT_RETRIES = 2
DEFAULT_RETRY_DELAY = 1.0
LOGIN_TIMEOUT = 120      # longer timeout for cold-start auth login
LOGIN_RETRIES = 5        # retry login this many times before giving up
LOGIN_RETRY_DELAY = 10.0  # seconds between login retries


# ─── ANSI Colours ─────────────────────────────────────────────────────────────

class C:
    """ANSI terminal colours. Call C.disable() to strip all codes."""

    _on = True

    @classmethod
    def disable(cls):
        cls._on = False

    @classmethod
    def _w(cls, code: str, text: str) -> str:
        return f"\033[{code}m{text}\033[0m" if cls._on else text

    @classmethod
    def green(cls, t):  return cls._w("92", t)
    @classmethod
    def red(cls, t):    return cls._w("91", t)
    @classmethod
    def yellow(cls, t): return cls._w("93", t)
    @classmethod
    def cyan(cls, t):   return cls._w("96", t)
    @classmethod
    def blue(cls, t):   return cls._w("94", t)
    @classmethod
    def bold(cls, t):   return cls._w("1",  t)
    @classmethod
    def dim(cls, t):    return cls._w("2",  t)


# ─── Variable Engine ──────────────────────────────────────────────────────────

class VarEngine:
    """
    Resolves {{VAR}} and {{VAR:default}} placeholders anywhere in
    strings, dicts, and lists.

    Precedence (highest → lowest):
        1. set() calls  (CLI --var, extracted response values)
        2. OS environment variables
        3. .env file
        4. 'variables' block in config file
    """

    _PATTERN = re.compile(r"\{\{([A-Za-z0-9_]+)(?::([^}]*))?\}\}")

    def __init__(self, config_vars: dict | None = None, env_file: str | None = None):
        self._store: dict[str, str] = {}

        # Lowest precedence: config file variables
        if config_vars:
            for k, v in config_vars.items():
                self._store[k] = str(v)

        # .env file
        if env_file:
            self._load_dotenv(env_file)

        # OS environment (overrides .env and config vars)
        for k, v in os.environ.items():
            self._store[k] = v

        # Auto-generate a unique run ID so test data never clashes
        self._store.setdefault("RUN_ID", uuid.uuid4().hex[:8])

    # ── public interface ──────────────────────────────────────────────────────

    def set(self, key: str, value: str):
        """Store a runtime variable at highest precedence."""
        self._store[key] = str(value)

    def get(self, key: str, default: str = "") -> str:
        return self._store.get(key, default)

    def resolve(self, value: Any) -> Any:
        """Recursively substitute {{VAR}} in strings, dicts, and lists."""
        if isinstance(value, str):
            return self._PATTERN.sub(self._sub, value)
        if isinstance(value, dict):
            return {k: self.resolve(v) for k, v in value.items()}
        if isinstance(value, list):
            return [self.resolve(item) for item in value]
        return value

    # ── private ───────────────────────────────────────────────────────────────

    def _sub(self, m: re.Match) -> str:
        name, default = m.group(1), m.group(2)
        val = self._store.get(name)
        if val is not None:
            return val
        if default is not None:
            return default
        return m.group(0)  # leave unresolved as-is

    def _load_dotenv(self, path: str):
        p = Path(path)
        if not p.exists():
            return
        with p.open() as f:
            for line in f:
                line = line.strip()
                if not line or line.startswith("#") or "=" not in line:
                    continue
                k, _, v = line.partition("=")
                # Strip surrounding quotes
                v = v.strip().strip('"').strip("'")
                self._store.setdefault(k.strip(), v)


# ─── Config Loader ────────────────────────────────────────────────────────────

def load_config(path: str) -> dict:
    """Load endpoint config from a JSON or YAML file."""
    p = Path(path)
    if not p.exists():
        raise FileNotFoundError(f"Config file not found: {path}")
    with p.open(encoding="utf-8") as f:
        if p.suffix in (".yaml", ".yml"):
            if not _YAML_OK:
                raise ImportError("PyYAML required for YAML files: pip install pyyaml")
            return yaml.safe_load(f) or {}
        return json.load(f)


# ─── JSON-path extractor ──────────────────────────────────────────────────────

def _jpath(data: Any, path: str) -> Any:
    """
    Extract a nested value using dot-separated keys.
    Supports list indices, e.g. "data.items.0.id".
    Returns None if any key is missing.
    """
    for part in path.split("."):
        if data is None:
            return None
        if isinstance(data, dict):
            data = data.get(part)
        elif isinstance(data, list):
            try:
                data = data[int(part)]
            except (IndexError, ValueError):
                return None
        else:
            return None
    return data


def extract_from_response(resp_json: Any, extract_map: dict, vars: VarEngine):
    """Store extracted response values into the variable engine."""
    if not extract_map or not isinstance(resp_json, dict):
        return
    for var_name, path in extract_map.items():
        value = _jpath(resp_json, path)
        if value is not None:
            vars.set(var_name, str(value))
            logging.debug("Extracted %s = %s", var_name, value)


# ─── Authentication ───────────────────────────────────────────────────────────

class AuthHandler:
    """
    Handles Bearer-token and API-key injection.
    Can auto-login by POSTing credentials and extracting a token.
    """

    def __init__(self, auth_cfg: dict, vars: VarEngine,
                 session: requests.Session, base_url: str, timeout: int):
        self._cfg = auth_cfg or {}
        self._vars = vars
        self._session = session
        self._base_url = base_url.rstrip("/")
        self._timeout = timeout
        self._token: str | None = None

    def login(self) -> str | None:
        """Perform auto-login and cache the token. Raises RuntimeError on failure."""
        login_cfg = self._cfg.get("login")
        if not login_cfg:
            return None

        method = login_cfg.get("method", "POST").upper()
        path   = self._vars.resolve(login_cfg["path"])
        body   = self._vars.resolve(login_cfg.get("body", {}))
        url    = f"{self._base_url}{path}"

        token_path = login_cfg.get("token_path", "accessToken")
        last_error: str = "unknown"

        for attempt in range(1, LOGIN_RETRIES + 1):
            try:
                print(C.dim(f"  Login attempt {attempt}/{LOGIN_RETRIES} → {url}"))
                resp = self._session.request(
                    method, url, json=body, timeout=LOGIN_TIMEOUT
                )
            except requests.RequestException as e:
                last_error = f"Login request failed: {e}"
                print(C.yellow(f"  Login attempt {attempt} failed: {last_error}"))
                if attempt < LOGIN_RETRIES:
                    import time; time.sleep(LOGIN_RETRY_DELAY)
                continue

            if resp.status_code not in range(200, 300):
                last_error = f"Login returned HTTP {resp.status_code}: {resp.text[:300]}"
                print(C.yellow(f"  Login attempt {attempt} failed: {last_error}"))
                if attempt < LOGIN_RETRIES:
                    import time; time.sleep(LOGIN_RETRY_DELAY)
                continue

            data = resp.json()
            token = _jpath(data, token_path)
            if not token:
                last_error = f"Token not found at path '{token_path}' in login response"
                print(C.yellow(f"  Login attempt {attempt} failed: {last_error}"))
                if attempt < LOGIN_RETRIES:
                    import time; time.sleep(LOGIN_RETRY_DELAY)
                continue

            self._token = str(token)
            return self._token

        raise RuntimeError(f"Login failed after {LOGIN_RETRIES} attempts. Last error: {last_error}")

    def inject(self, headers: dict) -> dict:
        """Mutate headers to add the appropriate auth header."""
        auth_type = self._cfg.get("type", "bearer").lower()
        if auth_type == "bearer" and self._token:
            headers["Authorization"] = f"Bearer {self._token}"
        elif auth_type == "apikey":
            header_name  = self._cfg.get("header", "X-API-Key")
            header_value = self._vars.resolve(self._cfg.get("key", ""))
            if header_value:
                headers[header_name] = header_value
        return headers

    def set_token(self, token: str):
        self._token = token


# ─── HTTP Session ─────────────────────────────────────────────────────────────

def build_session() -> requests.Session:
    """Build a requests.Session with a no-retry transport adapter."""
    session = requests.Session()
    adapter = HTTPAdapter(
        max_retries=Retry(total=0)
    )
    session.mount("http://", adapter)
    session.mount("https://", adapter)
    return session


# ─── Request Execution ────────────────────────────────────────────────────────

def execute_request(
    session:     requests.Session,
    method:      str,
    url:         str,
    *,
    headers:     dict,
    params:      dict | None,
    body:        Any,
    content_type: str,
    timeout:     int,
    retries:     int,
    retry_delay: float,
    dry_run:     bool = False,
) -> tuple[requests.Response | None, float, int, str]:
    """
    Run an HTTP request with application-level retries and exponential backoff.

    Returns:
        (response, elapsed_seconds, attempt_count, error_message)
    """
    if dry_run:
        print(C.dim(f"    [DRY-RUN] {method} {url}"))
        if params:
            print(C.dim(f"    params: {params}"))
        if body:
            preview = json.dumps(body, indent=2)[:400] if isinstance(body, dict) else str(body)[:400]
            print(C.dim(f"    body:   {preview}"))
        return None, 0.0, 0, ""

    ct = (content_type or "application/json").lower()
    last_error = ""

    for attempt in range(retries + 1):
        if attempt > 0:
            delay = retry_delay * (2 ** (attempt - 1))  # exponential backoff
            time.sleep(delay)

        try:
            start = time.perf_counter()

            if "form" in ct or "multipart" in ct:
                # Send as form-data
                resp = session.request(
                    method, url,
                    headers={k: v for k, v in headers.items()
                             if k.lower() != "content-type"},
                    params=params,
                    data=body,
                    timeout=timeout,
                )
            else:
                # Default: send as JSON
                resp = session.request(
                    method, url,
                    headers=headers,
                    params=params or None,
                    json=body,
                    timeout=timeout,
                )

            elapsed = time.perf_counter() - start
            return resp, elapsed, attempt + 1, ""

        except requests.Timeout:
            last_error = f"Timeout after {timeout}s"
        except requests.ConnectionError as exc:
            last_error = f"Connection error: {exc}"
        except requests.RequestException as exc:
            last_error = f"Request error: {exc}"

    return None, 0.0, retries + 1, last_error


# ─── Result Object ────────────────────────────────────────────────────────────

class Result:
    """Holds everything about one endpoint test."""

    __slots__ = (
        "name", "method", "url", "status", "elapsed",
        "attempts", "passed", "error", "body_snippet",
        "skipped", "skip_reason",
    )

    def __init__(self):
        self.name        = ""
        self.method      = ""
        self.url         = ""
        self.status: int | None = None
        self.elapsed     = 0.0
        self.attempts    = 0
        self.passed      = False
        self.error       = ""
        self.body_snippet = ""
        self.skipped     = False
        self.skip_reason = ""


# ─── Single Endpoint Runner ───────────────────────────────────────────────────

def run_endpoint(
    ep:          dict,
    session:     requests.Session,
    auth:        AuthHandler,
    vars:        VarEngine,
    base_url:    str,
    *,
    g_timeout:   int,
    g_retries:   int,
    g_retry_delay: float,
    dry_run:     bool,
) -> Result:
    """Execute one endpoint definition and return a Result."""

    r = Result()
    r.name   = ep.get("name", ep.get("path", "unnamed"))
    r.method = ep.get("method", "GET").upper()

    # Resolve all {{VAR}} in path, params, body, headers
    path    = vars.resolve(ep.get("path", "/"))
    # Support absolute URLs in path (e.g. for Python sidecar services)
    if path.startswith("http://") or path.startswith("https://"):
        r.url = path
    else:
        r.url = f"{base_url.rstrip('/')}{path}"
    params  = vars.resolve(ep.get("params")) or {}
    body    = vars.resolve(deepcopy(ep.get("body")))
    ct      = ep.get("content_type", "application/json")
    timeout = int(ep.get("timeout", g_timeout))
    retries = int(ep.get("retry", g_retries))
    r_delay = float(ep.get("retry_delay", g_retry_delay))
    expected = set(ep.get("expected_status", sorted(SUCCESS_STATUSES)))

    # Build request headers
    headers = {
        "Content-Type": ct,
        "Accept": "application/json",
    }
    headers.update(vars.resolve(ep.get("headers") or {}))
    auth.inject(headers)

    # Run
    resp, elapsed, attempts, error = execute_request(
        session, r.method, r.url,
        headers=headers,
        params=params or None,
        body=body,
        content_type=ct,
        timeout=timeout,
        retries=retries,
        retry_delay=r_delay,
        dry_run=dry_run,
    )

    r.elapsed  = elapsed
    r.attempts = attempts

    if dry_run:
        r.passed = True
        return r

    if resp is None:
        r.passed = False
        r.error  = error or "No response (timeout or connection error)"
        return r

    r.status = resp.status_code
    r.passed = resp.status_code in expected

    # Capture response body for failure logging
    try:
        r.body_snippet = resp.text[:2000]
    except Exception:
        r.body_snippet = "<unreadable>"

    # Extract variables from successful responses
    if r.passed and ep.get("extract"):
        try:
            extract_from_response(resp.json(), ep["extract"], vars)
        except Exception:
            pass  # extraction failure should not fail the test

    return r


# ─── Output Helpers ───────────────────────────────────────────────────────────

def _print_result(r: Result, verbose: bool):
    """Print one colourised result line (+ details on failure)."""
    if r.skipped:
        reason = f"  ({r.skip_reason})" if r.skip_reason else ""
        print(f"  {C.yellow('⊘')} {C.yellow('SKIP')}  {C.dim(r.method + ' ' + r.url)}{C.dim(reason)}")
        return

    icon  = C.green("✔") if r.passed else C.red("✖")
    label = C.green("PASS") if r.passed else C.red("FAIL")
    stat  = C.dim(f"HTTP {r.status}") if r.status else C.red("NO RESPONSE")
    ms    = C.dim(f"{r.elapsed * 1000:.0f}ms")
    retry = C.yellow(f" (attempt {r.attempts})") if r.attempts > 1 else ""

    print(f"  {icon} {label}  {C.bold(r.name)}  {stat}  {ms}{retry}")

    if not r.passed:
        print(f"       {C.cyan('URL:')} {r.method} {r.url}")
        if r.error:
            print(f"       {C.cyan('ERR:')} {C.red(r.error)}")
        if r.body_snippet:
            _print_body_preview(r.body_snippet, indent=7)

    elif verbose and r.body_snippet:
        _print_body_preview(r.body_snippet, indent=7, max_lines=8)


def _print_body_preview(text: str, indent: int = 7, max_lines: int = 15):
    """Pretty-print the first `max_lines` lines of a JSON-or-text body."""
    pad = " " * indent
    try:
        pretty = json.dumps(json.loads(text), indent=2)
        lines  = pretty.splitlines()
        for ln in lines[:max_lines]:
            print(f"{pad}{C.dim(ln)}")
        if len(lines) > max_lines:
            print(f"{pad}{C.dim('... (truncated)')}")
    except Exception:
        for ln in text[:600].splitlines()[:max_lines]:
            print(f"{pad}{C.dim(ln)}")


def _print_section(title: str):
    w = 66
    print(f"\n{C.bold(C.blue('─' * w))}")
    print(f"  {C.bold(C.blue(title))}")
    print(f"{C.bold(C.blue('─' * w))}")


# ─── CLI + Main ───────────────────────────────────────────────────────────────

def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        prog="api_validator",
        description="Comprehensive REST API end-to-end validator",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__,
    )
    p.add_argument("--endpoints", "-e", default="endpoints.json",
                   help="Endpoint config file (default: endpoints.json)")
    p.add_argument("--base-url",   help="Override base URL from config")
    p.add_argument("--token",      help="Bearer token — skips auto-login")
    p.add_argument("--env-file",   default=".env", help=".env file (default: .env)")
    p.add_argument("--var",        action="append", default=[], metavar="KEY=VALUE",
                   help="Set a variable (repeatable, e.g. --var ORG_ID=abc123)")
    p.add_argument("--tags",       help="Comma-separated tags to include")
    p.add_argument("--skip-tags",  help="Comma-separated tags to exclude")
    p.add_argument("--fail-fast",  action="store_true", help="Halt on first failure")
    p.add_argument("--retries",    type=int,   default=None)
    p.add_argument("--retry-delay",type=float, default=None)
    p.add_argument("--timeout",    type=int,   default=None)
    p.add_argument("--output",     default="api_report.json",
                   help="JSON report path (default: api_report.json)")
    p.add_argument("--log-file",   default="api_validator.log",
                   help="Log file path (default: api_validator.log)")
    p.add_argument("--no-color",   action="store_true")
    p.add_argument("--verbose",    "-v", action="store_true",
                   help="Print response bodies even for passing tests")
    p.add_argument("--dry-run",    action="store_true",
                   help="Print requests without executing them")
    p.add_argument("--version",    action="version", version=f"%(prog)s {VERSION}")
    return p


def main():
    args = build_parser().parse_args()

    if args.no_color:
        C.disable()

    # ── File logger ───────────────────────────────────────────────────────────
    logging.basicConfig(
        level=logging.DEBUG,
        format="%(asctime)s %(levelname)-8s %(message)s",
        handlers=[logging.FileHandler(args.log_file, mode="w", encoding="utf-8")],
    )
    log = logging.getLogger("api_validator")

    # ── Load config ───────────────────────────────────────────────────────────
    try:
        config = load_config(args.endpoints)
    except (FileNotFoundError, ImportError, json.JSONDecodeError, Exception) as exc:
        print(C.red(f"ERROR loading config: {exc}"))
        sys.exit(1)

    cfg         = config.get("config", {})
    # Resolve base_url through VarEngine so {{BASE_URL:...}} works
    _raw_base   = args.base_url or cfg.get("base_url", "http://localhost:8080")
    # VarEngine not yet built here; do a quick env-aware resolve
    _base_env   = os.environ.get("BASE_URL", "")
    base_url    = _base_env if _base_env else re.sub(r"\{\{[^}:]+:([^}]*)\}\}", r"\1", _raw_base)
    timeout     = args.timeout     if args.timeout     is not None else cfg.get("timeout",     DEFAULT_TIMEOUT)
    retries     = args.retries     if args.retries     is not None else cfg.get("retries",     DEFAULT_RETRIES)
    retry_delay = args.retry_delay if args.retry_delay is not None else cfg.get("retry_delay", DEFAULT_RETRY_DELAY)
    fail_fast   = args.fail_fast or cfg.get("fail_fast", False)

    # ── Variables ─────────────────────────────────────────────────────────────
    vars = VarEngine(
        config_vars=config.get("variables", {}),
        env_file=args.env_file,
    )
    for kv in args.var:
        if "=" in kv:
            k, _, v = kv.partition("=")
            vars.set(k.strip(), v.strip())

    # ── Session + Auth ────────────────────────────────────────────────────────
    session  = build_session()
    auth_cfg = config.get("auth", {})
    auth     = AuthHandler(auth_cfg, vars, session, base_url, timeout)

    if args.token:
        auth.set_token(args.token)
        print(C.dim("  ✔ Using provided Bearer token"))
    elif auth_cfg.get("login"):
        print(C.bold("\n  Authenticating..."))
        try:
            auth.login()
            print(C.dim(f"  ✔ Login successful (token acquired)"))
        except RuntimeError as exc:
            print(C.red(f"  AUTH FAILED: {exc}"))
            if fail_fast:
                sys.exit(1)

    # ── Tag filters ───────────────────────────────────────────────────────────
    run_tags  = set(args.tags.split(","))      if args.tags      else set()
    skip_tags = set(args.skip_tags.split(",")) if args.skip_tags else set()

    # ── Endpoint list ─────────────────────────────────────────────────────────
    endpoints = config.get("endpoints", [])
    if not endpoints:
        print(C.yellow("WARNING: No endpoints defined in config file."))
        sys.exit(0)

    # ── Banner ────────────────────────────────────────────────────────────────
    started_at = datetime.now(timezone.utc)
    print(C.bold(f"\n  API Validator  v{VERSION}"))
    print(C.dim(f"  Base URL  : {base_url}"))
    print(C.dim(f"  Config    : {args.endpoints}  ({len(endpoints)} endpoints)"))
    print(C.dim(f"  Retries   : {retries}  |  Timeout: {timeout}s  |  Fail-fast: {fail_fast}"))
    print(C.dim(f"  Run ID    : {vars.get('RUN_ID')}"))
    print(C.dim(f"  Started   : {started_at.strftime('%Y-%m-%d %H:%M:%S UTC')}"))

    # ── Run ───────────────────────────────────────────────────────────────────
    results: list[Result] = []
    current_section = None

    for ep in endpoints:
        # Section headers
        section = ep.get("section")
        if section and section != current_section:
            current_section = section
            _print_section(section)

        r = Result()
        r.name   = ep.get("name", ep.get("path", "unnamed"))
        r.method = ep.get("method", "GET").upper()
        _p = vars.resolve(ep.get("path", "/"))
        r.url = _p if _p.startswith(("http://", "https://")) else f"{base_url.rstrip('/')}{_p}"

        # ── Skip checks ──────────────────────────────────────────────────────
        if ep.get("skip", False):
            r.skipped     = True
            r.skip_reason = ep.get("skip_reason", "disabled in config")
            results.append(r)
            _print_result(r, args.verbose)
            continue

        ep_tags = set(ep.get("tags", []))
        if run_tags and not ep_tags.intersection(run_tags):
            r.skipped     = True
            r.skip_reason = f"not in --tags"
            results.append(r)
            _print_result(r, args.verbose)
            continue

        if skip_tags and ep_tags.intersection(skip_tags):
            r.skipped     = True
            r.skip_reason = f"excluded by --skip-tags"
            results.append(r)
            _print_result(r, args.verbose)
            continue

        # ── Execute ───────────────────────────────────────────────────────────
        log.info("%-7s %s", r.method, r.url)
        r = run_endpoint(
            ep, session, auth, vars, base_url,
            g_timeout=timeout,
            g_retries=retries,
            g_retry_delay=retry_delay,
            dry_run=args.dry_run,
        )
        results.append(r)
        log.info("  → %s  HTTP %s  %.0fms",
                 "PASS" if r.passed else "FAIL", r.status, r.elapsed * 1000)
        _print_result(r, args.verbose)

        if not r.passed and not r.skipped and fail_fast:
            print(C.red("\n  ✖ FAIL-FAST: stopping on first failure\n"))
            break

    # ── Summary ───────────────────────────────────────────────────────────────
    finished_at  = datetime.now(timezone.utc)
    total_secs   = (finished_at - started_at).total_seconds()
    active       = [r for r in results if not r.skipped]
    passed_list  = [r for r in active if r.passed]
    failed_list  = [r for r in active if not r.passed]
    skipped_list = [r for r in results if r.skipped]

    w = 66
    print(f"\n{'═' * w}")
    print(C.bold("  TEST SUMMARY"))
    print(f"{'═' * w}")
    print(f"  Tested   : {C.bold(str(len(active)))}")
    print(f"  {C.green('Passed')}   : {C.bold(C.green(str(len(passed_list))))}")
    print(f"  {C.red('Failed')}   : {C.bold(C.red(str(len(failed_list))))}")
    print(f"  {C.yellow('Skipped')}  : {C.yellow(str(len(skipped_list)))}")
    print(f"  Duration : {total_secs:.1f}s")
    print(f"{'═' * w}")

    if failed_list:
        print(C.red(C.bold("\n  Failed Endpoints:")))
        for r in failed_list:
            status_str = f"HTTP {r.status}" if r.status else "NO RESPONSE"
            print(C.red(f"    ✖ [{status_str}]  {r.name}"))
            print(C.dim(f"         {r.method} {r.url}"))
    elif active:
        print(C.green(C.bold("\n  🎉  All tests passed!")))

    # ── JSON Report ───────────────────────────────────────────────────────────
    report = {
        "meta": {
            "generated_at":   finished_at.isoformat(),
            "duration_secs":  round(total_secs, 2),
            "base_url":       base_url,
            "config_file":    str(args.endpoints),
            "run_id":         vars.get("RUN_ID"),
        },
        "summary": {
            "total":     len(active),
            "passed":    len(passed_list),
            "failed":    len(failed_list),
            "skipped":   len(skipped_list),
            "pass_rate": f"{len(passed_list)/len(active)*100:.1f}%" if active else "N/A",
        },
        "results": [
            {
                "name":             r.name,
                "method":           r.method,
                "url":              r.url,
                "status":           r.status,
                "passed":           r.passed,
                "skipped":          r.skipped,
                "skip_reason":      r.skip_reason or None,
                "elapsed_ms":       round(r.elapsed * 1000, 1),
                "attempts":         r.attempts,
                "error":            r.error or None,
                "response_snippet": r.body_snippet[:500] if not r.passed and r.body_snippet else None,
            }
            for r in results
        ],
    }

    try:
        with open(args.output, "w", encoding="utf-8") as f:
            json.dump(report, f, indent=2)
        print(C.dim(f"\n  Report  → {args.output}"))
        print(C.dim(f"  Log     → {args.log_file}"))
    except OSError as exc:
        print(C.yellow(f"  Warning: could not write report: {exc}"))

    sys.exit(0 if not failed_list else 1)


if __name__ == "__main__":
    main()
