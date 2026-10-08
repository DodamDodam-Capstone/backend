#!/usr/bin/env python3
"""Check backend agent documents and known AI attribution without dependencies."""

import argparse
import json
import os
from pathlib import Path
import re
import subprocess
from urllib.parse import unquote, urlsplit


def git(*args):
    return subprocess.check_output(["git", *args], text=True).strip("\n")


def check_documents(root, files):
    errors = []
    for required in ("AGENTS.md", "CLAUDE.md"):
        if required not in files:
            errors.append(f"{required}: required root document is missing")
    for name in sorted(files):
        path = Path(name)
        limit = {"AGENTS.md": 60, "CLAUDE.md": 10}.get(path.name)
        if limit is None and not (path.parent == Path("docs") and path.match("AGENT_*.md")):
            continue
        text = (root / path).read_text(encoding="utf-8")
        if limit and len(text.splitlines()) > limit:
            errors.append(f"{name}: exceeds {limit} lines; move details into docs/")
        prose = re.sub(r"```.*?```|~~~.*?~~~|`[^`]*`", "", text, flags=re.S)
        if path.name == "CLAUDE.md":
            imports = re.findall(r"(?<!\w)@[^\s`]+", prose)
            if imports != ["@AGENTS.md"] or str(path.with_name("AGENTS.md")) not in files:
                errors.append(f"{name}: import only the AGENTS.md in the same directory")
        # ponytail: inline links only; extend this check if docs adopt reference-style links.
        for target in re.findall(r"\[[^\]\n]+\]\(([^)\s]+)\)", prose):
            url = urlsplit(target)
            if url.scheme in {"https", "http", "mailto"} or not url.path:
                continue
            resolved = (root / path.parent / unquote(url.path)).resolve()
            if (url.scheme or url.netloc or not resolved.is_relative_to(root)
                    or str(resolved.relative_to(root)) not in files or not resolved.is_file()):
                errors.append(f"{name}: link must target a tracked backend file: {target}")
    return errors


def is_agent_identity(identity):
    name = identity.split("<", 1)[0].strip()
    return bool(re.fullmatch(
        r"(?:claude(?:[ -]code)?|codex|openai(?:[ -]codex)?|anthropic)(?:\[bot\])?", name, re.I
    ) or re.search(
        r"\b(?:claude|codex|noreply|no-reply)@(?:anthropic|openai)\.com\b|"
        r"\b(?:\d+\+)?(?:claude|codex)\[bot\]@users\.noreply\.github\.com\b",
        identity, re.I
    ))


def check_attribution(text, context):
    text = re.sub(r"```.*?```|~~~.*?~~~", "", text, flags=re.S)
    trailers = re.findall(
        r"^\s*(?:Co-authored-by|Contributed-by|Signed-off-by|Generated-by):\s*(.+)$", text, re.I | re.M
    )
    generated = re.search(
        r"^\s*(?:🤖\s*)?(?:generated|authored|co-authored|created|powered|assisted)\s+(?:by|with)"
        r"\s+[\[*_`]*(?:claude|codex|openai|anthropic)\b", text, re.I | re.M
    )
    return [f"{context}: remove AI attribution"] if generated or any(map(is_agent_identity, trailers)) else []


def check_commits(revision):
    errors = []
    for entry in git("log", "-z", "--format=%H%n%an <%ae>%n%cn <%ce>%n%B", revision).split("\0"):
        if not entry.strip():
            continue
        sha, author, committer, body = entry.split("\n", 3)
        if is_agent_identity(author) or is_agent_identity(committer):
            errors.append(f"commit {sha[:12]}: use the existing human author and committer identity")
        errors.extend(check_attribution(body, f"commit {sha[:12]}"))
    return errors


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base", help="Check commits after this base; defaults to the PR base or HEAD only")
    args = parser.parse_args()
    root = Path(git("rev-parse", "--show-toplevel")).resolve()
    os.chdir(root)
    files = set(git("ls-files", "-z").split("\0")) - {""}
    errors = check_documents(root, files)
    event_path = os.environ.get("GITHUB_EVENT_PATH")
    event = json.loads(Path(event_path).read_text()) if event_path else {}
    pr = event.get("pull_request")
    if pr:
        errors.extend(check_attribution(pr["title"] + "\n" + (pr.get("body") or ""), "PR"))
    base = args.base or (pr["base"]["sha"] if pr else None)
    head = pr["head"]["sha"] if pr else "HEAD"
    errors.extend(check_commits(f"{base}..{head}" if base else f"{head}^!"))
    for error in errors:
        print(error)
    if not errors:
        print("Backend agent policy checks passed.")
    return bool(errors)


if __name__ == "__main__":
    raise SystemExit(main())
