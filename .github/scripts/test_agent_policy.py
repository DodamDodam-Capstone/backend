#!/usr/bin/env python3
"""Run with python3 .github/scripts/test_agent_policy.py (stdlib only)."""

from pathlib import Path
from tempfile import TemporaryDirectory
import os
import unittest

from check_agent_policy import check_attribution, check_commits, check_documents, git, is_agent_identity


class AgentPolicyTest(unittest.TestCase):
    def test_documents_and_attribution_regressions(self):
        with TemporaryDirectory() as directory:
            root = Path(directory).resolve()
            documents = {
                "AGENTS.md": "[Details](docs/AGENT_GUIDE.md#rules)\n",
                "CLAUDE.md": "@AGENTS.md\n",
                "docs/AGENT_GUIDE.md": "[Root](../AGENTS.md)\n[Web](https://example.com)\n"
                                       "`[Example](missing.md)`\n```md\n[Example](missing.md)\n```\n",
                "src/AGENTS.md": "Local rules\n",
                "src/CLAUDE.md": "@AGENTS.md\n",
            }
            for name, text in documents.items():
                (root / name).parent.mkdir(parents=True, exist_ok=True)
                (root / name).write_text(text)
            files = set(documents)
            self.assertEqual(check_documents(root, files), [])
            for name, invalid in [
                ("AGENTS.md", "line\n" * 61),
                ("src/AGENTS.md", "line\n" * 61),
                ("CLAUDE.md", "@AGENTS.md\n" + "line\n" * 10),
                ("src/CLAUDE.md", "@../../AGENTS.md\n"),
                ("CLAUDE.md", "@AGENTS.md\n@docs/AGENT_GUIDE.md\n"),
                ("docs/AGENT_GUIDE.md", "[missing](missing.md)\n"),
                ("AGENTS.md", "[outside](../AGENTS.md)\n"),
                ("AGENTS.md", "[untracked](local.md)\n"),
            ]:
                with self.subTest(name=name, invalid=invalid):
                    (root / "local.md").write_text("not committed\n")
                    (root / name).write_text(invalid)
                    self.assertTrue(check_documents(root, files))
                    (root / name).write_text(documents[name])
            self.assertTrue(check_documents(root, files - {"AGENTS.md"}))
            self.assertTrue(check_documents(root, files - {"src/AGENTS.md"}))
        for identity in ("Codex <codex@openai.com>", "Claude <noreply@anthropic.com>",
                         "Agent <123+claude[bot]@users.noreply.github.com>"):
            self.assertTrue(is_agent_identity(identity))
            self.assertTrue(check_attribution(f"Co-authored-by: {identity}", "commit"))
        for message in ("Generated with Codex", "🤖 Generated with [Claude Code](https://claude.com)",
                        "Signed-off-by: OpenAI <codex@openai.com>"):
            self.assertTrue(check_attribution(message, "PR"))
        self.assertFalse(is_agent_identity("Claude Martin <claude@example.com>"))
        self.assertFalse(is_agent_identity("dependabot[bot] <49699333+dependabot[bot]@users.noreply.github.com>"))
        self.assertEqual(check_attribution("Document Claude and Codex rules\nCo-authored-by: Teammate <dev@example.com>", "PR"), [])
        self.assertEqual(check_attribution("Regression example:\n```text\nCo-authored-by: Codex <codex@openai.com>\n```", "PR"), [])

    def test_entire_commit_range(self):
        previous = Path.cwd()
        with TemporaryDirectory() as directory:
            try:
                os.chdir(directory)
                git("init", "--quiet")
                git("config", "user.name", "Test Developer")
                git("config", "user.email", "developer@example.com")
                for message in ("baseline", "change\n\nCo-authored-by: Codex <codex@openai.com>", "clean tip"):
                    git("-c", "commit.gpgsign=false", "commit", "--quiet", "--allow-empty", "-m", message)
                self.assertEqual(check_commits("HEAD^..HEAD"), [])
                self.assertEqual(len(check_commits("HEAD~2..HEAD")), 1)
                git("-c", "commit.gpgsign=false", "commit", "--quiet", "--allow-empty", "--author",
                    "Claude <noreply@anthropic.com>", "-m", "agent author")
                self.assertEqual(len(check_commits("HEAD^..HEAD")), 1)
            finally:
                os.chdir(previous)


if __name__ == "__main__":
    unittest.main()
