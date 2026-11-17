"""Validate the full Compose profile and session cookie environment mappings."""

import json
import os
import subprocess


for use_overrides, expected in [
    (False, {"SESSION_COOKIE_SECURE": "false", "SESSION_COOKIE_SAME_SITE": "lax"}),
    (True, {"SESSION_COOKIE_SECURE": "true", "SESSION_COOKIE_SAME_SITE": "none"}),
]:
    environment = dict(os.environ)
    for key in expected:
        environment.pop(key, None)
    environment.pop("APP_RUNTIME_UID", None)
    environment.pop("APP_RUNTIME_GID", None)
    if use_overrides:
        environment.update(expected)
        environment.update({"APP_RUNTIME_UID": "12345", "APP_RUNTIME_GID": "12346"})
    # Explicit env-file prevents loading the developer's actual .env.
    result = subprocess.run(
        ["docker", "compose", "--env-file", "/dev/null", "--file", "compose.yaml",
         "--profile", "full", "config", "--format", "json"],
        env=environment, stdout=subprocess.PIPE, text=True, check=True,
    )
    app = json.loads(result.stdout)["services"]["app"]
    app_environment = app["environment"]
    expected_user = "12345:12346" if use_overrides else "10001:10001"
    if app.get("user") != expected_user:
        raise SystemExit(f"compose.yaml: app.user must resolve to {expected_user}.")
    for key, value in expected.items():
        if app_environment.get(key) != value:
            raise SystemExit(f"compose.yaml: app.environment.{key} must resolve to {value}.")

print("Compose configuration and cookie checks passed.")
