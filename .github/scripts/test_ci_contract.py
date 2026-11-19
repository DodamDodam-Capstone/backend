"""Exercise CI build detection and real Compose configuration contracts."""

import os
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import textwrap
import unittest


ROOT = Path(__file__).resolve().parents[2]


class CiContractTests(unittest.TestCase):
    def test_release_requires_quality_and_main_push_before_notification(self):
        # 품질 실패·PR 실행에서 외부 쓰기가 시작되지 않도록 workflow 연결을 고정한다.
        workflow = (ROOT / ".github/workflows/ci.yml").read_text()
        self.assertIn("  push:\n    branches: [main]", workflow)
        self.assertIn("run: python3 .github/scripts/test_release.py", workflow)
        self.assertIn("cancel-in-progress: ${{ github.event_name == 'pull_request' }}", workflow)
        self.assertIn("github.event.pull_request.number || github.sha", workflow)
        release = workflow.split("\n  release:\n")[1].split("\n  notify-integration:\n")[0]
        self.assertIn("    needs: quality", release)
        self.assertIn("if: github.event_name == 'push' && github.ref == 'refs/heads/main'", release)
        self.assertIn("      contents: write", release)
        self.assertIn("      pull-requests: read", release)
        self.assertIn("          ref: ${{ github.sha }}", release)
        self.assertNotIn("always()", release)
        notification = workflow.split("\n  notify-integration:\n")[1]
        self.assertIn("    needs: release", notification)
        self.assertIn("    uses: ./.github/workflows/notify-integration.yml", notification)
        self.assertNotIn("always()", notification)
        reusable = (ROOT / ".github/workflows/notify-integration.yml").read_text()
        self.assertIn("  workflow_call:", reusable)
        self.assertNotIn("  push:", reusable)
        self.assertNotIn("  workflow_dispatch:", reusable)

    def test_local_compose_runtime_uses_host_nonroot_uid(self):
        for uid, gid, succeeds in [("501", "20", True), ("0", "0", False), ("501", "0", False)]:
            with self.subTest(uid=uid, gid=gid), tempfile.TemporaryDirectory() as directory:
                root = Path(directory)
                fake_id = root / "id"
                fake_id.write_text('#!/bin/sh\ncase "$1" in -u) echo "$TEST_UID";; -g) echo "$TEST_GID";; esac\n')
                fake_docker = root / "docker"
                fake_docker.write_text('#!/usr/bin/env python3\nimport os,json,sys\nprint(json.dumps({"uid":os.getenv("APP_RUNTIME_UID"),"gid":os.getenv("APP_RUNTIME_GID"),"args":sys.argv[1:]}))\n')
                fake_id.chmod(0o755)
                fake_docker.chmod(0o755)
                result = subprocess.run(
                    ["sh", str(ROOT / "scripts/compose-local.sh"), "config", "--quiet"],
                    cwd=ROOT, capture_output=True, text=True,
                    env={**os.environ, "PATH": str(root) + os.pathsep + os.environ["PATH"],
                         "TEST_UID": uid, "TEST_GID": gid, "APP_RUNTIME_UID": "0", "APP_RUNTIME_GID": "0"},
                )
                self.assertEqual(result.returncode == 0, succeeds, result.stderr)
                if succeeds:
                    actual = json.loads(result.stdout)
                    self.assertEqual((actual["uid"], actual["gid"]), (uid, gid))
                    self.assertEqual(actual["args"], ["compose", "--profile", "full", "config", "--quiet"])
                else:
                    self.assertEqual(result.stdout, "")

    def test_build_detection(self):
        workflow = (ROOT / ".github/workflows/ci.yml").read_text()
        step = workflow.split("      - name: Detect build system\n", 1)[1]
        step = step.split("      - name:", 1)[0]
        command = textwrap.dedent(step.split("        run: |\n", 1)[1])
        cases = [
            ("no build", {}, False, "No supported build configuration"),
            ("missing wrapper", {"build.gradle.kts": ""}, False, "wrapper"),
            ("missing Java version", {"build.gradle.kts": "", "gradlew": "#!/bin/sh\n"}, False, ".java-version"),
            ("Gradle", {"build.gradle.kts": "", "gradlew": "#!/bin/sh\n", ".java-version": "25\n"}, True, "system=gradle\njava-version=25\n"),
            ("Maven", {"pom.xml": "", "mvnw": "#!/bin/sh\n", ".java-version": "25\n"}, True, "system=maven\njava-version=25\n"),
        ]
        for name, files, succeeds, expected in cases:
            with self.subTest(name=name), tempfile.TemporaryDirectory() as directory:
                root = Path(directory)
                for filename, content in files.items():
                    path = root / filename
                    path.write_text(content)
                    if filename in {"gradlew", "mvnw"}:
                        path.chmod(0o755)
                output = root / "github-output"
                result = subprocess.run(
                    ["bash", "-e", "-o", "pipefail", "-c", command],
                    cwd=root, env={**os.environ, "GITHUB_OUTPUT": str(output)},
                    capture_output=True, text=True,
                )
                self.assertEqual(result.returncode == 0, succeeds, result.stderr)
                actual = output.read_text() if succeeds else result.stderr
                self.assertIn(expected, actual)

    def test_compose_contracts(self):
        compose = (ROOT / "compose.yaml").read_text()
        secure_mapping = "SESSION_COOKIE_SECURE: ${SESSION_COOKIE_SECURE:-false}"
        same_site_mapping = "      SESSION_COOKIE_SAME_SITE: ${SESSION_COOKIE_SAME_SITE:-lax}\n"
        cases = [
            ("valid", compose, True, "Compose configuration and cookie checks passed."),
            ("invalid full profile", compose.replace("condition: service_healthy", "condition: invalid"), False, "condition"),
            ("hardcoded Secure", compose.replace(secure_mapping, 'SESSION_COOKIE_SECURE: "false"'), False, "SESSION_COOKIE_SECURE"),
            ("missing SameSite", compose.replace(same_site_mapping, ""), False, "SESSION_COOKIE_SAME_SITE"),
            ("hardcoded root user", compose.replace('user: "${APP_RUNTIME_UID:-10001}:${APP_RUNTIME_GID:-10001}"', 'user: "0:0"'), False, "app.user"),
        ]
        for name, content, succeeds, expected in cases:
            with self.subTest(name=name), tempfile.TemporaryDirectory() as directory:
                root = Path(directory)
                (root / "compose.yaml").write_text(content)
                (root / ".env").write_text("THIS_IS_NOT_VALID_DOTENV='\n")
                result = subprocess.run(
                    [sys.executable, str(ROOT / ".github/scripts/check_compose.py")],
                    cwd=root, capture_output=True, text=True,
                    env={**os.environ, "SESSION_COOKIE_SECURE": "ambient", "SESSION_COOKIE_SAME_SITE": "ambient"},
                )
                self.assertEqual(result.returncode == 0, succeeds, result.stderr)
                self.assertIn(expected, result.stdout + result.stderr)
                self.assertNotIn("DB_PASSWORD", result.stdout + result.stderr)


if __name__ == "__main__":
    unittest.main()
