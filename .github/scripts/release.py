"""검증된 main 커밋에 변경 불가능한 버전 스냅샷을 생성한다."""

import json
import os
from pathlib import Path
import re
import subprocess
import sys


VERSION_PATTERN = r"(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)"


def validate_version(version):
    """정식 숫자 버전만 허용하여 ref·API 경로에 그대로 사용한다."""
    if not re.fullmatch(VERSION_PATTERN, version):
        raise ValueError("VERSION must contain a stable X.Y.Z version without leading zeros.")
    return version


def read_version(path=Path("VERSION")):
    """파일 누락과 잘못된 버전은 PR 또는 릴리스를 실패시킨다."""
    return validate_version(Path(path).read_text(encoding="utf-8").strip())


def api(endpoint, payload=None, *, paginate=False):
    """인증은 기존 GH_TOKEN에 맡기고 API 실패를 '없음'으로 바꾸지 않는다."""
    command = ["gh", "api", endpoint]
    if payload is not None:
        command += ["--method", "POST", "--input", "-"]
    if paginate:
        command += ["--paginate", "--slurp"]
    result = subprocess.run(command, input=json.dumps(payload) if payload is not None else None,
                            capture_output=True, text=True, timeout=60)
    if result.returncode:
        raise RuntimeError(f"GitHub API failed: {result.stderr.strip()}")
    data = json.loads(result.stdout)
    return [item for page in data for item in page] if paginate else data


def ref_commit(repo, short_ref):
    """접두어 일치 ref를 걸러내고 annotated tag도 실제 커밋으로 비교한다."""
    refs = api(f"repos/{repo}/git/matching-refs/{short_ref}")
    obj = next((ref["object"] for ref in refs if ref["ref"] == f"refs/{short_ref}"), None)
    if obj is None:
        return None
    seen = set()
    while obj["type"] == "tag":
        if obj["sha"] in seen:
            raise ValueError("Cyclic annotated tag")
        seen.add(obj["sha"])
        obj = api(f"repos/{repo}/git/tags/{obj['sha']}")["object"]
    if obj["type"] != "commit":
        raise ValueError(f"{short_ref} must point to a commit.")
    return obj["sha"]


def publish(repo, sha, version):
    """충돌을 먼저 검사한 뒤 누락된 ref와 Release만 순서대로 생성한다."""
    validate_version(version)
    if not re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", repo):
        raise ValueError("Invalid repository")
    if not re.fullmatch(r"[0-9a-f]{40}", sha):
        raise ValueError("Release must use a full commit SHA.")
    tag = f"v{version}"
    refs = {ref: ref_commit(repo, ref) for ref in (f"heads/release/{tag}", f"tags/{tag}")}
    for ref, existing in refs.items():
        if existing is not None and existing != sha:
            raise ValueError(f"{ref} already refers to another commit; choose a new version.")
    releases = api(f"repos/{repo}/releases?per_page=100", paginate=True)
    existing_release = next((release for release in releases if release["tag_name"] == tag), None)
    if existing_release:
        if existing_release["draft"] or existing_release["prerelease"]:
            raise ValueError("Existing release is draft or prerelease; inspect it before publishing.")
        if refs[f"tags/{tag}"] is None:
            raise ValueError("Existing release has no verifiable tag; inspect it before publishing.")

    # 생성 API만 사용한다. 경합·부분 실패는 중단하고 같은 SHA로 재실행한다.
    for ref, existing in refs.items():
        if existing is None:
            api(f"repos/{repo}/git/refs", {"ref": f"refs/{ref}", "sha": sha})
    if existing_release is None:
        existing_release = api(f"repos/{repo}/releases", {
            "tag_name": tag, "target_commitish": sha, "name": tag,
            "draft": False, "prerelease": False, "generate_release_notes": True,
        })
    print(f"Released {tag} at {sha}: {existing_release['html_url']}")
    return existing_release["html_url"]


def main():
    """workflow 조건에 더해 로컬·PR·수동 실행의 원격 생성을 차단한다."""
    try:
        if os.environ.get("GITHUB_EVENT_NAME") != "push" or os.environ.get("GITHUB_REF") != "refs/heads/main":
            raise ValueError("Release publishing is allowed only for a main push after quality succeeds.")
        publish(os.environ["GITHUB_REPOSITORY"], os.environ["GITHUB_SHA"], read_version())
    except (ValueError, OSError, KeyError, RuntimeError, subprocess.TimeoutExpired) as error:
        print(str(error), file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
