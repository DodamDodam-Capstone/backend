"""보호된 워크플로에서 수동 병합을 위한 PR별 필수 검사를 발행한다."""

import argparse
import json
import os
import re
import subprocess
from urllib.parse import quote

from notify_pr_review import latest_codex_summary, review_state
from release import api
from review_routing import change_risk, independent_approval

CHECK = "backend-review-policy"
APP_ID = 5832717
REQUIRED = {"backend-quality", "gitmoji-conventional-title", "jira-issue-key"}


def eligible(pr, repo):
    return bool(pr["state"] == "open" and pr.get("draft") is False and pr["base"]["ref"] == "development"
                and (pr["head"].get("repo") or {}).get("full_name") == repo
                and re.fullmatch(r"(?:feature|fix|hotfix)/SCRUM-[0-9]+-.+", pr["head"]["ref"]))


def codex_clear(comments, sha):
    # 재리뷰 중이거나 새 요약을 해석할 수 없으면 과거 완료 결과를 쓰지 않는다.
    latest = latest_codex_summary(comments)
    if not latest:
        return False
    state = review_state(latest["body"]) or {}
    return (state.get("commit_sha") == sha and state.get("has_any_findings") is False
            and state.get("has_unresolved_findings") is False)


def ci_clear(checks):
    # 이 검사의 이전 판정을 필수 CI에 포함하면 스스로의 완료를 기다리게 된다.
    ci = [row for row in checks if row["name"] != CHECK]
    return REQUIRED <= {row["name"] for row in ci} and all(row["state"] == "SUCCESS" for row in ci)


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
    # development 수동 실행은 검토한 base와 동일한 보호 브랜치 코드여야 한다.
    trusted = ((run["event"] == "issue_comment" and run["head_branch"] == "main")
               or (run["event"] == "workflow_dispatch" and run["head_branch"] == "development"
                   and run.get("head_sha") == pr["base"]["sha"]))
    if not (run["path"] == ".github/workflows/notify-slack.yml" and trusted and run["head_repository"]["full_name"] == repo
            and run["conclusion"] == "success"):
        return False
    # 같은 Actions 앱이 발행한 다른 체크가 유료 리뷰 실행을 가리켜도 성공으로 오인하지 않는다.
    expected = f"sol-max-clear {number} {sha} {pr['base']['sha']} {check['id']}"
    return any(job["name"] == expected and job["conclusion"] == "success"
               for job in api(run_url + "/jobs?per_page=100")["jobs"])


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
    source = (pr["head"].get("repo") or {}).get("full_name")
    if not source:
        return False
    activity = api(f"repos/{source}/activity?ref={ref}&per_page=1")
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
    if pr["state"] != "open" or pr["base"]["ref"] != "development" or pr.get("draft") is not False:
        return None, "열린 development 일반 PR 필요"
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
    if level == "low" and not eligible(pr, repo):
        level = "medium"  # 내부 작업 브랜치에서만 사람 승인을 생략한다.
    result = subprocess.run(["gh", "pr", "checks", str(number), "-R", repo, "--required", "--json", "name,state"],
                            capture_output=True, text=True, timeout=60)
    if result.returncode not in {0, 1, 8} or not result.stdout.strip() or not ci_clear(json.loads(result.stdout)):
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
    if level != "low" and not human:
        return None, "독립된 backend 팀원 승인 필요"
    if level == "high" and not paid_clear(repo, pr):
        return None, "고위험 · 현재 PR과 커밋의 Sol Max 통과 필요"
    return (level, sha, pr["base"]["sha"]), "병합 조건 충족"


def existing_check(root, sha, name):
    path = f"{root}/commits/{sha}/check-runs?check_name={quote(name, safe='')}&app_id={APP_ID}&filter=latest"
    return next((c for c in api(path)["check_runs"] if c["name"] == name and c["app"]["id"] == APP_ID), None)


def publish(root, sha, name, identity, result, title, summary, prior, token):
    if prior and prior["conclusion"] == result and prior["output"]["title"] == title and prior.get("external_id") == identity:
        return
    payload = {"name": name, "status": "completed", "conclusion": result, "external_id": identity,
               "output": {"title": title, "summary": summary}}
    if prior:
        api(f"{root}/check-runs/{prior['id']}", payload, method="PATCH", token=token)
    else:
        api(f"{root}/check-runs", payload | {"head_sha": sha}, token=token)


def report(repo, number, *, token=None):
    root = f"repos/{repo}"
    url = f"{root}/pulls/{number}"
    pr = api(url)
    if pr["state"] != "open" or pr["base"]["ref"] != "development":
        return
    head, base = pr["head"]["sha"], pr["base"]["sha"]
    target = pr.get("merge_commit_sha")
    info = f"PR #{number} · 수동 병합 안내"
    identity = f"manual-review:{number}:{base}:{head}"
    summary = f"PR #{number} · head `{head}` · base `{base}`"
    if not target or pr.get("mergeable") is not True:
        title = "Draft · 검토 준비 대기" if pr.get("draft") else "GitHub 병합 상태 확인 · 충돌 여부 확인"
        print(f"PR #{number} · {title}")
        if token:
            publish(root, head, info, identity, "action_required", title, summary,
                    existing_check(root, head, info), token)
        return
    # 동일한 head를 쓰는 다른 PR이나 이전 base에 성공 판정을 발행하지 않는다.
    ref = f"{root}/git/ref/pull/{number}/merge"
    if (api(ref)["object"]["sha"] != target or
            [p["sha"] for p in api(f"{root}/git/commits/{target}")["parents"]] != [base, head]):
        return
    prior = None
    shared = False
    if token:
        prior = existing_check(root, target, CHECK)
        shared = prior is not None and prior.get("external_id") != identity
        if prior and prior["conclusion"] == "success":
            # 승인 철회나 조회 장애 중에 이전 성공이 남지 않게 먼저 닫는다.
            api(f"{root}/check-runs/{prior['id']}", {"status": "completed", "conclusion": "action_required",
                "output": {"title": "최신 리뷰 재확인 중", "summary": f"PR #{number}"}}, method="PATCH", token=token)
            prior = prior | {"conclusion": "action_required", "output": {"title": "최신 리뷰 재확인 중"}}
    result = "action_required"
    try:
        decision, title = evaluate(repo, number, token)
        if decision and decision[1:] == (head, base):
            result = "success"
            title = ("팀원 승인 생략 · 수동 병합 가능" if decision[0] == "low"
                     else "독립된 팀원 승인 확인 · 수동 병합 가능")
    except (RuntimeError, ValueError, KeyError, subprocess.TimeoutExpired):
        title = "리뷰 근거 조회 실패 · 재확인 필요"
    latest = api(url)
    if (latest["state"] != "open" or latest["base"]["ref"] != "development"
            or latest["head"]["sha"] != head or latest["base"]["sha"] != base
            or api(ref)["object"]["sha"] != target):
        return
    if latest.get("draft") is not False or api(f"{root}/git/ref/heads/development")["object"]["sha"] != base:
        result, title = "action_required", "Draft 또는 대상 브랜치 변경 · 재확인 필요"
    if shared:
        identity = "shared-merge-commit"
        result, title = "action_required", "동일한 검사 커밋을 공유하는 PR · 별도 커밋 필요"
    print(f"PR #{number} · {title}")
    if not token:
        return
    publish(root, target, CHECK, identity, result, title, summary, prior, token)
    # PR의 Checks 탭은 head 검사를 보여 준다. 안내 이름은 필수 검사와 분리한다.
    publish(root, head, info, identity, result, title, summary, existing_check(root, head, info), token)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("number", type=int)
    parser.add_argument("--publish", action="store_true")
    args = parser.parse_args()
    report(os.environ["GITHUB_REPOSITORY"], args.number,
           token=os.environ["REVIEW_APP_TOKEN"] if args.publish else None)
