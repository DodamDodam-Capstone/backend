"""기존 Slack 알림에서 PR 위험도, 확인 근거와 다음 행동을 보여 준다."""

import json
import os
import re
from pathlib import Path

from review_routing import protected_reason


PENDING = ("미판정", "리뷰 대기", "최신 커밋의 SCRUM 완료 조건과 호출 흐름 확인이 필요합니다.",
           "수정 커밋이면 PR 댓글에 @codex review를 남기세요.")


def risk_for_pr(run, pr, get, app_id):
    sha = run["head_sha"]
    api = f"https://api.github.com/repos/{run['repository']['full_name']}"
    url = f"{api}/pulls/{pr['number']}"
    current = get(url)
    if current["head"]["sha"] != sha:
        return "미판정", "이전 커밋", "이 실행 뒤 새 커밋이 올라왔습니다.", "최신 PR 검사를 확인하세요."

    # 이동·삭제된 보호 파일도 확인하고, 누락된 페이지로 일반 변경을 확정하지 않는다.
    count = current["changed_files"]
    if not 0 < count <= 3000:
        return "미판정", "팀원 확인", "전체 변경 파일을 확인할 수 없습니다.", "팀원이 변경 범위를 확인하세요."
    files = []
    for page in range(1, (count + 99) // 100 + 1):
        files.extend(get(f"{url}/files?per_page=100&page={page}"))
    if len(files) != count:
        return "미판정", "팀원 확인", "변경 파일 조회 결과가 불완전합니다.", "팀원이 최신 변경을 확인하세요."
    paths = [path for file in files for path in (file["filename"], file.get("previous_filename")) if path]
    protected = next((f"{rule} · {path}" for path in paths if (rule := protected_reason(path))), None)
    risk, decision, reason, action = PENDING
    if protected or current["base"]["ref"] == "main":
        risk, decision = "높음", "팀원 확인"
        reason = protected or "main 릴리스 승인 대상입니다."
        action = "작성자와 마지막 푸시자를 제외한 팀원 1명의 승인을 받으세요."

    # 체크 이름만 같은 다른 앱이나 같은 SHA를 공유하는 다른 PR의 판정을 제외한다.
    if app_id and app_id.isascii() and app_id.isdigit():
        result = get(f"{api}/commits/{sha}/check-runs?check_name=review-risk&app_id={app_id}&filter=latest")
        runs = [check for check in result["check_runs"] if str(check["app"]["id"]) == app_id
                and check["name"] == "review-risk" and check["head_sha"] == sha
                and check.get("external_id") == f"pr-{pr['number']}"]
        check = max(runs, key=lambda row: row["id"], default={})
        title = (check.get("output") or {}).get("title")
        if check.get("status") == "completed":
            if check.get("conclusion") == "failure" and title == "수정 필요":
                risk, decision = ("높음" if risk == "높음" else "중간"), "수정 필요"
                reason, action = "검증 체크에서 SCRUM 완료 조건 위반을 확인했습니다.", "PR의 지적을 수정한 뒤 재리뷰를 요청하세요."
            elif check.get("conclusion") == "success" and title == "통과" and risk != "높음":
                risk, decision = "낮음", "리뷰 통과"
                reason, action = "최신 커밋에서 SCRUM 완료 조건별 근거를 확인했습니다.", "필수 CI와 GitHub 병합 조건을 확인하세요."
            elif title == "팀원 판단" and check.get("conclusion") == "action_required":
                decision = "팀원 확인"
                reason, action = "검증 체크에서 팀원 판단을 요청했습니다.", "PR에서 호출 흐름과 검증 근거를 확인하고 승인하세요."
            elif title == "팀원 승인" and check.get("conclusion") == "success":
                decision = "팀원 승인"
                reason, action = "검증 체크가 최신 커밋의 독립된 팀원 승인을 확인했습니다.", "나머지 필수 CI와 리뷰 대화를 확인하세요."

    if run.get("conclusion") in {"failure", "timed_out", "action_required"}:
        decision = "CI 확인"
        reason += f" {run['name']} 검사가 성공하지 않았습니다."
        action = "실패한 실행 로그를 확인한 뒤 수정하거나 재실행하세요."
    if get(url)["head"]["sha"] != sha:
        return "미판정", "이전 커밋", "조회 도중 새 커밋이 올라왔습니다.", "최신 PR 검사를 확인하세요."
    return risk, decision, reason, action


def build_payload(event, token, helper, app_id=""):
    run = event["workflow_run"]
    if run.get("event") != "pull_request":
        return helper.build_payload(event, token)
    sha = run["head_sha"]
    repo = run["repository"]["full_name"]
    matches = [pr for pr in helper.associated_pull_requests(run, token)
               if pr.get("head", {}).get("sha") == sha and pr["head"].get("ref") == run["head_branch"]]
    if run["name"] in {"Backend CI", "Jira Key"}:
        matches = [pr for pr in matches if pr["base"]["ref"] in {"development", "main"}]
    pr = matches[0] if len(matches) == 1 else None
    if pr:
        try:
            risk, decision, reason, action = risk_for_pr(run, pr, lambda url: helper.github_json(url, token), app_id)
        except (RuntimeError, KeyError, TypeError, ValueError):
            risk, decision, reason, action = "미판정", "조회 대기", "GitHub 검증 근거를 확인할 수 없습니다.", "PR에서 최신 검사와 변경 범위를 확인하세요."
    else:
        risk, decision, reason, action = "미판정", "PR 확인", "실행과 연결된 PR을 하나로 특정할 수 없습니다.", "실행 링크에서 대상 PR을 확인하세요."

    label = f"PR #{pr['number']}" if pr else "PR"
    key = re.search(r"\bSCRUM-[0-9]{1,10}\b", run["head_branch"])
    header = f"{label} · 위험도 {risk} · {decision}"
    if key:
        header = f"{key.group()} · {header}"
    result = {"success": "통과", "failure": "실패", "cancelled": "취소", "timed_out": "시간 초과"}.get(run.get("conclusion"), run.get("conclusion") or "미확인")
    links = [f"<{run['html_url']}|실행 로그>", f"<https://github.com/{repo}/commit/{sha}|{sha[:7]}>"]
    if pr:
        links.insert(0, f"<https://github.com/{repo}/pull/{pr['number']}|PR 보기>")
    # 경로 등 PR 작성자가 바꿀 수 있는 값은 plain_text로만 표시한다.
    return {
        "text": f"{header}\n{reason.partition(' · ')[0]}\n{action}", "unfurl_links": False, "unfurl_media": False,
        "blocks": [
            {"type": "header", "text": {"type": "plain_text", "text": header}},
            {"type": "section", "text": {"type": "plain_text", "text": f"판단 근거\n{reason[:500]}\n\n다음 행동\n{action}"}},
            {"type": "context", "elements": [{"type": "plain_text", "text": f"{run['name']} {result} · {repo}"},
                                              {"type": "mrkdwn", "text": "  ·  ".join(links)}]},
        ],
    }


if __name__ == "__main__":
    # 고정 버전의 팀 공용 helper가 webhook 검증·재시도와 일반 이벤트를 담당한다.
    import notify_workflow_run_to_slack as shared

    event = json.loads(Path(os.environ["GITHUB_EVENT_PATH"]).read_text(encoding="utf-8"))
    payload = build_payload(event, os.environ["GITHUB_API_TOKEN"], shared, os.getenv("REVIEW_RISK_APP_ID", ""))
    shared.post_slack(os.environ["SLACK_WEBHOOK_URL"], payload)
