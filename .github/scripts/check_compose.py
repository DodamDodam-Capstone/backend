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
    if use_overrides:
        environment.update(expected)
    # Explicit env-file prevents loading the developer's actual .env.
    result = subprocess.run(
        ["docker", "compose", "--env-file", "/dev/null", "--file", "compose.yaml",
         "--profile", "full", "config", "--format", "json"],
        env=environment, stdout=subprocess.PIPE, text=True, check=True,
    )
    app_environment = json.loads(result.stdout)["services"]["app"]["environment"]
    for key, value in expected.items():
        if app_environment.get(key) != value:
            raise SystemExit(f"compose.yaml: app.environment.{key} must resolve to {value}.")

print("Compose configuration and cookie checks passed.")
