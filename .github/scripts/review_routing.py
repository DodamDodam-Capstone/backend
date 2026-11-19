"""SCRUM 리뷰 증거로 다음 담당자를 정한다. 병합 권한이나 GitHub 승인을 발급하지 않는다."""

from collections import Counter
from dataclasses import dataclass
import re


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

FLOW_SERVICES = {BASE + "conversation/application/" + name + ".java" for name in (
    "TurnAcceptanceService", "TurnResultService", "ConversationClosureCoordinator",
)}


def reordered_calls(patch):
    """같은 Java 변경 구간에서 양쪽에 한 번씩 등장하는 호출들의 상대 순서를 비교한다."""
    for hunk in re.split(r"(?m)^@@[^\n]*@@[^\n]*$", patch):
        before, after = [], []
        for line in hunk.splitlines():
            call = re.match(r"^([ +\-])\s*([A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)+)\s*\(", line)
            if call:
                if call[1] != "+":
                    before.append(call[2])
                if call[1] != "-":
                    after.append(call[2])
        old, new = Counter(before), Counter(after)
        shared = {name for name in old if old[name] == new[name] == 1}
        if [n for n in before if n in shared] != [n for n in after if n in shared]:
            return True
    return False


def protected_reason(path):
    parts = path.split("/")
    if path.startswith(".github/") or parts[-1] in {"AGENTS.md", "CLAUDE.md", "CODEOWNERS"} or path in {
        "CONTRIBUTING.md", "docs/AGENT_WORKFLOW.md", "docs/AGENT_DOCUMENTATION.md", "docs/TEAM_WORKFLOW_GUIDE.md",
    }:
        return "CI·리뷰 정책 변경"
    if path.startswith((BASE + "auth/", BASE + "guardian/")):
        return "인증·보호자 권한 처리 변경"
    if path in FLOW_SERVICES:
        return "대화 턴 접수·결과 확정·종료 순서 변경"
    if "infrastructure" in parts or path.startswith("src/main/resources/db/"):
        return "영속화·외부 연동 변경"
    if path.startswith(BASE + "global/"):
        return "여러 API에 적용되는 공통 처리 변경"
    if path in BUILD_FILES or path.startswith(("gradle/", "scripts/", "src/main/resources/")):
        return "빌드·런타임 설정 변경"
    return None


def high_risk_reason(current, files):
    paths = [path for file in files for path in (file["filename"], file.get("previous_filename")) if path]
    protected = next((f"{rule} · {path}" for path in paths if (rule := protected_reason(path))), None)
    if protected or current["base"]["ref"] == "main":
        return protected or "main 릴리스 승인 대상"
    for file in files:
        path = file["filename"]
        if path.startswith("src/main/java/") and path.endswith(".java") and reordered_calls(file.get("patch") or ""):
            return f"기존 메서드 호출 순서 변경 · {path}"
    return None


def change_risk(pr, files):
    if high_risk_reason(pr, files):
        return "high"
    for row in files:
        path, patch = row["filename"], row.get("patch") or ""
        added = [line[1:] for line in patch.splitlines() if line.startswith("+")]
        removed = [line[1:] for line in patch.splitlines() if line.startswith("-")]
        if (not patch or row.get("status") not in {"added", "modified"} or row.get("previous_filename")
                or (len(added), len(removed)) != (row.get("additions"), row.get("deletions"))):
            return "medium"
        if path == "README.md" or path.startswith("docs/") and path.endswith(".md"):
            continue
        if path.startswith("src/test/java/") and path.endswith(".java"):
            # 기존 테스트의 설정·데이터·실행 자체를 약화하는 추가 한 줄도 사람이 검토한다.
            source = "\n".join(added)
            annotations = set(re.findall(r"@([\w.]+)", source))
            declaration = re.findall(r"\b(?:class|interface|enum|record)\s+(\w+)", source)
            if (row["status"] == "added" and path.endswith(("Test.java", "Tests.java"))
                    and declaration == [path.rsplit("/", 1)[-1][:-5]]
                    and "Test" in annotations and annotations <= {"Test", "DisplayName"}
                    and not re.search(r"\b(?:extends|implements|static\s*\{|System|Runtime|assume\w*)\b", source)):
                continue
            return "medium"
        if not (path.startswith(BASE) and path.endswith(".java")):
            return "medium"
        # Java 호출·분기·범위의 의미를 단순 문자열만으로 확정할 수 없는 변경은 팀원에게 보낸다.
        simple_field = r"(?:private |protected |public |static |final )*(?:String|boolean|int|long|double|Integer|Long|Boolean)\s+\w+;"
        field_edits = all(re.fullmatch(simple_field, line.strip()) for line in added + removed if line.strip())
        # 호출이 없는 기존 단일 반환식의 리터럴·연산자 변경은 호출 순서를 바꾸지 않는다.
        simple_return = r"return (?:[\w .+*/<>=!&|%-]+);"
        return_edit = (len(added) == len(removed) == 1 and patch.count("@@") == 2 and
                       all(re.fullmatch(simple_return, line.strip()) for line in added + removed))
        if not (field_edits or return_edit):
            return "medium"
    return "low"


def route(*, task, head, paths, ci, review, only_known_changes):
    """신호가 누락되면 담당자에게 보낸다. 확정된 결함을 먼저 수정한 뒤 위험도를 다시 판정한다."""
    current = bool(task and head and review and (review.task, review.head) == (task, head))
    if ci == "failed" or (current and review.outcome == "changes" and any(v.strip() for v in review.evidence.values())):
        return "수정 필요"
    if ci != "passed":
        return "CI 대기"
    if not current or not paths or only_known_changes is not True:
        return "팀원 판단"
    if review.outcome != "pass":
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
