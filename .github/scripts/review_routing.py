"""SCRUM 리뷰 증거로 다음 담당자를 정한다. 병합 권한이나 GitHub 승인을 발급하지 않는다."""

from dataclasses import dataclass


@dataclass(frozen=True)
class Review:
    """신뢰된 수집기가 확인한 최신 리뷰. PR 본문이나 라벨로 이 값을 채우지 않는다."""

    task: str
    head: str
    outcome: str
    evidence: dict[str, str]
    changed_flow: bool | None
    tests_weakened: bool | None


BASE = "src/main/java/com/dodamdodam/backend/"
BUILD_FILES = {"build.gradle.kts", "settings.gradle.kts", "gradle.properties", "gradlew", "gradlew.bat", "compose.yaml"}


def protected_reason(path):
    parts = path.split("/")
    if path.startswith(".github/") or parts[-1] in {"AGENTS.md", "CLAUDE.md", "CODEOWNERS"} or path in {
        "CONTRIBUTING.md", "docs/AGENT_WORKFLOW.md", "docs/AGENT_DOCUMENTATION.md", "docs/TEAM_WORKFLOW_GUIDE.md",
    }:
        return "CI·리뷰 정책 변경"
    if path.startswith((BASE + "auth/", BASE + "guardian/")):
        return "인증·보호자 권한 처리 변경"
    if "infrastructure" in parts or path.startswith("src/main/resources/db/"):
        return "영속화·외부 연동 변경"
    if path.startswith(BASE + "global/"):
        return "여러 API에 적용되는 공통 처리 변경"
    if path in BUILD_FILES or path.startswith(("gradle/", "scripts/", "src/main/resources/")):
        return "빌드·런타임 설정 변경"
    return None


def route(*, task, head, paths, ci, review, criteria, only_known_changes):
    """신호가 누락되면 담당자에게 보낸다. 확정된 결함을 먼저 수정한 뒤 위험도를 다시 판정한다."""
    current = bool(task and head and review and (review.task, review.head) == (task, head))
    if ci == "failed" or (current and review.outcome == "changes" and any(v.strip() for v in review.evidence.values())):
        return "수정 필요"
    if ci != "passed":
        return "CI 대기"
    if not current or not paths or only_known_changes is not True:
        return "팀원 판단"
    if review.outcome != "pass" or not criteria or any(not review.evidence.get(key, "").strip() for key in criteria):
        return "팀원 판단"
    if review.changed_flow is not False or review.tests_weakened is not False:
        return "팀원 판단"
    if any(protected_reason(path) for path in paths):
        return "팀원 판단"
    return "통과"


def independent_approval(*, head, author, last_pusher, reviewer, reviewed_head, state, human_team_member):
    """수집기는 GitHub의 유효 리뷰·사람 팀원 여부·실제 마지막 푸시자를 확인해 전달한다."""
    return bool(head and author and last_pusher and reviewer and human_team_member is True
                and head == reviewed_head and state == "APPROVED"
                and reviewer not in {author, last_pusher})
