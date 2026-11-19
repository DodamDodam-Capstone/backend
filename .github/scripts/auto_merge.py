"""신뢰된 main 실행에서만 최신 development 작업 PR을 squash 병합한다."""

import argparse
import json
import os
from pathlib import Path
import re
import subprocess
from urllib.parse import quote

from notify_pr_review import CODEX_APP_ID, CODEX_BOT_ID, review_state
from release import api
from review_routing import change_risk, independent_approval

REQUIRED = {"backend-quality", "gitmoji-conventional-title", "jira-issue-key"}


def eligible(pr, repo):
    return bool(pr["state"] == "open" and pr.get("draft") is False and pr["base"]["ref"] == "development"
                and (pr["head"].get("repo") or {}).get("full_name") == repo
                and re.fullmatch(r"(?:feature|fix|hotfix)/SCRUM-[0-9]+-.+", pr["head"]["ref"]))


def codex_clear(comments, sha):
    # 재리뷰 중이거나 새 요약을 해석할 수 없으면 과거 완료 결과를 쓰지 않는다.
    summaries = [row for row in comments if row["user"]["id"] == CODEX_BOT_ID
                 and (row.get("performed_via_github_app") or {}).get("id") == CODEX_APP_ID
                 and row["body"].startswith("<!-- codex-pull-request-review-summary -->")]
    if not summaries:
        return False
    latest = max(summaries, key=lambda row: (row["updated_at"], row["id"]))
    state = review_state(latest["body"]) or {}
    return (state.get("commit_sha") == sha and state.get("has_any_findings") is False
            and state.get("has_unresolved_findings") is False)


def ci_clear(checks):
    return REQUIRED <= {row["name"] for row in checks} and all(row["state"] == "SUCCESS" for row in checks)


def paid_clear(repo, pr):
    sha, number = pr["head"]["sha"], pr["number"]
    prefix = f"pr-{number}@https://github.com/{repo}/actions/runs/"
    url = f"repos/{repo}/commits/{sha}/check-runs?check_name=sol-max-review&app_id=15368&filter=all&per_page=100"
    relevant = []
    for page in range(1, 12):
        rows = api(f"{url}&page={page}")["check_runs"]
        relevant.extend(r for r in rows if (r.get("external_id") or "").split("@")[0] == f"pr-{number}")
        if len(rows) < 100:
            break
    else:
        return False
    check = max(relevant, key=lambda row: row["id"], default={})
    marker = check.get("external_id") or ""
    if not (marker.startswith(prefix) and re.fullmatch(r"[0-9]+", marker[len(prefix):])
            and check.get("head_sha") == sha and check.get("name") == "sol-max-review"
            and (check.get("app") or {}).get("id") == 15368 and check.get("status") == "completed"
            and check.get("conclusion") == "success" and check["output"]["title"] == "추가 지적 없음"):
        return False
    run_url = f"repos/{repo}/actions/runs/{marker[len(prefix):]}"
    run = api(run_url)
    if not (run["path"] == ".github/workflows/notify-slack.yml" and run["event"] == "issue_comment"
            and run["head_branch"] == "main" and run["head_repository"]["full_name"] == repo
            and run["conclusion"] == "success"):
        return False
    # 같은 Actions 앱이 발행한 다른 체크가 유료 리뷰 실행을 가리켜도 성공으로 오인하지 않는다.
    expected = f"sol-max-clear {number} {sha} {pr['base']['sha']} {check['id']}"
    return any(job["name"] == expected and job["conclusion"] == "success"
               for job in api(run_url + "/jobs?per_page=100")["jobs"])


def allowed(level, human, paid):
    return level == "low" or (human and (level == "medium" or level == "high" and paid))


def latest_opinions(repo, number):
    reviews = api(f"repos/{repo}/pulls/{number}/reviews?per_page=100", paginate=True)
    latest = {}
    for review in sorted(reviews, key=lambda row: row["id"]):
        if review["state"] in {"APPROVED", "CHANGES_REQUESTED", "DISMISSED"}:
            latest[review["user"]["login"]] = review
    return latest


def team_approved(repo, pr, app_token, latest):
    owner = repo.split("/")[0]
    ref = quote("refs/heads/" + pr["head"]["ref"], safe="")
    activity = api(f"repos/{repo}/activity?ref={ref}&per_page=1")
    if not activity or activity[0].get("after") != pr["head"]["sha"]:
        return False
    pusher = (activity[0].get("actor") or {}).get("login")
    members = {user["login"] for user in api(f"orgs/{owner}/teams/backend/members?per_page=100",
                                             paginate=True, token=app_token) if user["type"] == "User"}
    return any(independent_approval(head=pr["head"]["sha"], author=pr["user"]["login"], last_pusher=pusher,
                                    reviewer=login, reviewed_head=row["commit_id"], state=row["state"],
                                    human_team_member=login in members and row["user"]["type"] == "User")
               for login, row in latest.items())


def pull_reviews(repo, number):
    owner, name = repo.split("/")
    query = """query($owner:String!, $name:String!, $n:Int!, $after:String) {
      repository(owner:$owner,name:$name) { pullRequest(number:$n) { reviewDecision headRefOid baseRefOid
        reviewThreads(first:100,after:$after) { nodes { isResolved } pageInfo {hasNextPage endCursor} }
      } } }"""
    after = None
    while True:
        data = api("graphql", {"query": query, "variables": {"owner": owner, "name": name, "n": number, "after": after}})
        page = data["data"]["repository"]["pullRequest"]
        threads = page["reviewThreads"]
        if any(node["isResolved"] is not True for node in threads["nodes"]):
            return None
        if not threads["pageInfo"]["hasNextPage"]:
            return page
        after = threads["pageInfo"]["endCursor"]


def evaluate(repo, number, app_token):
    root = f"repos/{repo}"
    pr = api(f"{root}/pulls/{number}")
    if not eligible(pr, repo):
        return None, "대상 외 PR"
    sha = pr["head"]["sha"]
    comments = api(f"{root}/issues/{number}/comments?per_page=100", paginate=True)
    if not codex_clear(comments, sha):
        return None, "최신 공식 Codex 무지적 판정 대기"
    if not 0 < pr["changed_files"] <= 3000:
        return None, "전체 파일 수 확인 필요"
    files = api(f"{root}/pulls/{number}/files?per_page=100", paginate=True)
    if len(files) != pr["changed_files"]:
        return None, "전체 변경 목록 확인 필요"
    level = change_risk(pr, files)
    paid = level == "high" and paid_clear(repo, pr)
    if level == "high" and not paid:
        return None, "고위험 · 현재 변경에 대한 Sol Max 통과 근거 없음"
    result = subprocess.run(["gh", "pr", "checks", str(number), "-R", repo, "--required", "--json", "name,state"],
                            capture_output=True, text=True, timeout=60)
    if result.returncode or not ci_clear(json.loads(result.stdout)):
        return None, "필수 CI 통과 대기"
    reviews = pull_reviews(repo, number)
    if (not reviews or reviews["headRefOid"] != sha or reviews["baseRefOid"] != pr["base"]["sha"]
            or reviews["reviewDecision"] == "CHANGES_REQUESTED"):
        return None, "미해결 대화·변경 요청 또는 커밋 변경"
    # Ruleset만 사용하는 저장소에서는 실제 리뷰가 있어도 reviewDecision이 null일 수 있다.
    opinions = latest_opinions(repo, number)
    if any(row["state"] == "CHANGES_REQUESTED" for row in opinions.values()):
        return None, "유효한 변경 요청"
    human = level != "low" and team_approved(repo, pr, app_token, opinions)
    if not allowed(level, human, paid):
        return None, f"{level} · 최신 커밋에 독립된 backend 팀원 승인 필요"
    return (level, sha, pr["base"]["sha"]), "병합 조건 충족"


def close_issues(repo, pr):
    # GITHUB_TOKEN 병합은 closed 워크플로를 트리거하지 않아 같은 이슈 종료 helper를 실행한다.
    helper = Path(".automation/.github/scripts/close_linked_issues.py")
    subprocess.run(["python3", str(helper)], check=True, env=os.environ | {
        "GITHUB_TOKEN": os.environ["GH_TOKEN"], "PR_BODY": pr.get("body") or "", "GITHUB_REPOSITORY": repo})


def reconcile(repo, number, apply):
    app_token = os.getenv("LOW_RISK_TOKEN")
    decision, reason = evaluate(repo, number, app_token)
    print(f"PR #{number} · {reason}")
    if not decision or not apply:
        if apply and reason == "대상 외 PR":
            pr = api(f"repos/{repo}/pulls/{number}")
            if (pr.get("merged") and pr["base"]["ref"] == "development"
                    and (pr.get("merged_by") or {}).get("login") == "github-actions[bot]"):
                close_issues(repo, pr)
        return
    level, sha, base = decision
    pr = api(f"repos/{repo}/pulls/{number}")
    if (not eligible(pr, repo) or pr["head"]["sha"] != sha or pr["base"]["sha"] != base
            or pr.get("mergeable") is not True or pr.get("mergeable_state") not in {"blocked", "clean"}
            or api(f"repos/{repo}/git/ref/heads/development")["object"]["sha"] != base):
        return
    if level == "low" and not app_token:
        raise ValueError("저위험 PR 전용 앱 토큰이 필요합니다.")
    # 일반 토큰에는 승인 예외가 없다. 보호 규칙과 최종 head 비교는 GitHub도 다시 수행한다.
    result = api(f"repos/{repo}/pulls/{number}/merge", {"sha": sha, "merge_method": "squash",
                 "commit_title": f"{pr['title']} (#{number})"}, method="PUT",
                 token=app_token if level == "low" else None)
    if not result["merged"]:
        raise RuntimeError(f"PR #{number} 병합 보류")
    if level != "low":
        close_issues(repo, pr)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("number", type=int)
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    reconcile(os.environ["GITHUB_REPOSITORY"], args.number, args.apply)
