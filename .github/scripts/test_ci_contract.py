"""Exercise CI build detection and real Compose configuration contracts."""

import os
from pathlib import Path
import subprocess
import sys
import tempfile
import textwrap
import unittest


ROOT = Path(__file__).resolve().parents[2]


class CiContractTests(unittest.TestCase):
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
