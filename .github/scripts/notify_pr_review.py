"""기존 Slack 알림에서 PR 위험도, 확인 근거와 다음 행동을 보여 준다."""

import json
import os
import re
from pathlib import Path

from review_routing import change_risk, high_risk_reason


PENDING = ("미판정", "리뷰 대기", "최신 커밋의 공식 리뷰와 필수 CI를 기다립니다.",
           "수정 커밋이면 PR 댓글에 @codex review를 남기세요.")
CODEX_APP_ID = 1144995
CODEX_BOT_ID = 199175422


def review_state(body):
    # 공식 앱의 버전 있는 메타데이터만 읽고 표시용 문구나 이모지는 판정에 쓰지 않는다.
    match = re.match(r"^<!-- codex-pull-request-review-summary -->\r?\n<!-- codex-review-state:v1 (\{[^\r\n]*\}) -->", body or "")
    if not match:
        return None
    try:
        state = json.loads(match[1])["code_review"]
        if (isinstance(state, dict) and state.get("status") == "completed"
                and re.fullmatch(r"[0-9a-f]{40}", state.get("commit_sha") or "")
                and isinstance(state.get("has_unresolved_findings"), bool)):
            return state
    except (ValueError, KeyError, TypeError):
        pass
    return None


def risk_for_pr(run, pr, get, app_id, codex=None):
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
    protected = high_risk_reason(current, files)
    risk, decision, reason, action = PENDING
    if protected:
        risk, decision = "높음", "팀원 확인"
        reason = protected
        action = "작성자와 마지막 푸시자를 제외한 팀원 1명의 승인을 받으세요."
    if codex:
        if codex["has_unresolved_findings"]:
            risk, decision = ("높음" if risk == "높음" else "중간"), "리뷰 지적 확인"
            reason = f"{protected}. " if protected else ""
            reason += "공식 Codex 리뷰에 미해결 지적이 있습니다."
            action = "리뷰 링크에서 지적을 확인하고 변경 후 수동 재리뷰를 요청하세요."
        elif risk == "높음":
            reason += ". 최신 Codex 리뷰에 미해결 지적이 없습니다."
        elif codex.get("has_any_findings") is not False:
            decision, reason, action = ("리뷰 확인", "최신 공식 리뷰의 무지적 판정을 확인할 수 없습니다.",
                                       "최신 커밋에서 공식 재리뷰를 요청하세요.")
        elif change_risk(current, files) == "low":
            risk, decision, reason, action = ("낮음", "필수 검사 확인", "공식 리뷰 무지적이며 보호 경로·호출 순서·테스트 약화 징후가 없습니다.",
                                              "필수 CI와 대화 해결 후 자동 병합 대상입니다.")
        else:
            risk, decision, reason, action = ("중간", "팀원 확인", "변경된 호출 흐름 또는 테스트 강도를 자동으로 확인하기 어렵습니다.",
                                              "독립된 backend 팀원이 최신 커밋을 승인해야 합니다.")

    # 체크 이름만 같은 다른 앱이나 같은 SHA를 공유하는 다른 PR의 판정을 제외한다.
    # 같은 커밋도 다시 리뷰할 수 있다. CI 단독 알림이나 미해결 최신 리뷰는 이전 성공으로 덮지 않는다.
    if app_id and app_id.isascii() and app_id.isdigit() and not (codex and codex["has_unresolved_findings"]):
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
            elif codex and check.get("conclusion") == "success" and title == "통과" and risk != "높음":
                risk, decision = "낮음", "리뷰 통과"
                reason, action = "최신 커밋에 신뢰된 검증 체크의 통과 기록이 있습니다.", "필수 CI와 GitHub 병합 조건을 확인하세요."
            elif title == "팀원 판단" and check.get("conclusion") == "action_required":
                decision = "팀원 확인"
                reason, action = "검증 체크에서 팀원 판단을 요청했습니다.", "PR에서 호출 흐름과 검증 근거를 확인하고 승인하세요."
            elif codex and title == "팀원 승인" and check.get("conclusion") == "success":
                decision = "팀원 승인"
                reason, action = "검증 체크가 최신 커밋의 독립된 팀원 승인을 확인했습니다.", "나머지 필수 CI와 리뷰 대화를 확인하세요."

    if run.get("conclusion") in {"failure", "timed_out", "action_required"}:
        decision = "CI 확인"
        reason += f" {run['name']} 검사가 성공하지 않았습니다."
        action = "실패한 실행 로그를 확인한 뒤 수정하거나 재실행하세요."
    if get(url)["head"]["sha"] != sha:
        return "미판정", "이전 커밋", "조회 도중 새 커밋이 올라왔습니다.", "최신 PR 검사를 확인하세요."
    return risk, decision, reason, action


def build_payload(event, token, helper, app_id="", on_high_risk=None):
    codex = None
    if "comment" in event:
        comment, issue = event["comment"], event["issue"]
        if (not issue.get("pull_request") or event.get("sender", {}).get("id") != CODEX_BOT_ID
                or comment.get("user", {}).get("id") != CODEX_BOT_ID):
            return None
        codex = review_state(comment.get("body"))
        previous = review_state(event.get("changes", {}).get("body", {}).get("from"))
        if not codex or codex == previous:
            return None
        repo = event["repository"]["full_name"]
        api = f"https://api.github.com/repos/{repo}"
        verified = helper.github_json(f"{api}/issues/comments/{int(comment['id'])}", token)
        if ((verified.get("performed_via_github_app") or {}).get("id") != CODEX_APP_ID
                or verified.get("user", {}).get("id") != CODEX_BOT_ID or review_state(verified.get("body")) != codex
                or verified.get("issue_url") != f"{api}/issues/{int(issue['number'])}"):
            return None
        pr = helper.github_json(f"{api}/pulls/{issue['number']}", token)
        sha = codex["commit_sha"]
        if pr["head"]["sha"] != sha:
            return None
        run = {"name": "Codex 코드 리뷰", "head_branch": pr["head"]["ref"], "head_sha": sha,
               "repository": event["repository"], "html_url": verified["html_url"], "conclusion": "리뷰 결과"}
    else:
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
            risk, decision, reason, action = risk_for_pr(run, pr, lambda url: helper.github_json(url, token), app_id, codex)
        except (RuntimeError, KeyError, TypeError, ValueError):
            risk, decision, reason, action = "미판정", "조회 대기", "GitHub 검증 근거를 확인할 수 없습니다.", "PR에서 최신 검사와 변경 범위를 확인하세요."
    else:
        risk, decision, reason, action = "미판정", "PR 확인", "실행과 연결된 PR을 하나로 특정할 수 없습니다.", "실행 링크에서 대상 PR을 확인하세요."

    # 유료 작업에는 공식 완료 이벤트에서 재확인한 내부 PR의 SHA만 전달한다.
    if (codex and risk == "높음" and on_high_risk and pr.get("state") == "open"
            and pr["base"]["ref"] == "development"
            and (pr["head"].get("repo") or {}).get("full_name") == repo):
        on_high_risk(sha)

    label = f"PR #{pr['number']}" if pr else "PR"
    key = re.search(r"\bSCRUM-[0-9]{1,10}\b", run["head_branch"])
    header = f"{label} · 위험도 {risk} · {decision}"
    if key:
        header = f"{key.group()} · {header}"
    result = {"success": "통과", "failure": "실패", "cancelled": "취소", "timed_out": "시간 초과"}.get(run.get("conclusion"), run.get("conclusion") or "미확인")
    source = "공식 리뷰" if codex else "실행 로그"
    links = [f"<{run['html_url']}|{source}>", f"<https://github.com/{repo}/commit/{sha}|{sha[:7]}>"]
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
    def queue_review(sha):
        with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as output:
            print(f"deep_sha={sha}", file=output)

    payload = build_payload(event, os.environ["GITHUB_API_TOKEN"], shared, os.getenv("REVIEW_RISK_APP_ID", ""), queue_review)
    if payload is not None:
        shared.post_slack(os.environ["SLACK_WEBHOOK_URL"], payload)
