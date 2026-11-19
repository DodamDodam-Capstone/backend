"""완료된 Sol Max 판정을 해당 PR의 대화와 변경 줄에 함께 전달한다."""

import json
import os
import re

from release import api

HUNK = re.compile(r"^@@ -(\d+)(?:,\d+)? \+(\d+)(?:,\d+)? @@")
FORMAT_START = "<details><summary>PR 리뷰 게시 자료</summary>\n\n```json\n"
FORMAT_END = "\n```\n</details>"


def diff_lines(patch):
    """GitHub의 삭제 줄은 LEFT, 추가·문맥 줄은 RIGHT를 사용한다."""
    left = right = None
    for line in patch.split("\n"):
        anchor = ""
        if match := HUNK.match(line):
            left, right = int(match[1]), int(match[2])
        elif right is not None:
            if line.startswith("-"):
                anchor = f"L{left}"
                left += 1
            elif line.startswith(("+", " ")):
                anchor = f"R{right}"
                right += 1
                left += int(line.startswith(" "))
        yield anchor, line


def literal(text):
    # PR과 모델의 텍스트를 멘션, 링크, HTML로 실행하지 않는다.
    return "\n".join("    " + line for line in text.splitlines())


def receipt(base_sha, evidence, findings):
    data = {"base": base_sha, "summary": evidence, "findings": findings}
    return FORMAT_START + json.dumps(data, ensure_ascii=False) + FORMAT_END


def review_payload(sha, check_id, check_url, report, files):
    locations = {f["filename"]: {a for a, _ in diff_lines(f.get("patch") or "") if a} for f in files}
    marker = f"<!-- sol-max-review:{check_id} -->"
    body = [marker, f"**Sol Max 2차 리뷰 · 위험도 높음 · 수정 필요** · `{sha[:7]}`",
            literal(report["summary"])]
    comments = []
    for index, finding in enumerate(report["findings"], 1):
        path, anchor, message = finding["path"], finding["anchor"], finding["body"]
        anchored = anchor in locations.get(path, ())
        if anchored:
            side = "RIGHT" if anchor[0] == "R" else "LEFT"
            line = int(anchor[1:])
            comments.append({"path": path, "line": line, "side": side, "body": literal(message)})
            location = f"{path} · {line}행 ({side})"
        else:
            location = f"{path or '파일 미지정'} · 위치 확인 필요"
        body.extend([f"**지적 {index}**", literal(location + "\n" + message)])
    body.append(f"[정밀 리뷰 체크]({check_url})")
    return {"event": "COMMENT", "commit_id": sha, "body": "\n\n".join(body), "comments": comments}


def publish(repo, number, sha, check_id, run_url):
    number, check_id = int(number), int(check_id)
    if not re.fullmatch(r"[0-9a-f]{40}", sha):
        raise ValueError("전체 커밋 SHA가 필요합니다.")
    root = f"repos/{repo}"
    pull = f"{root}/pulls/{number}"
    check = api(f"{root}/check-runs/{check_id}")
    if not (check["app"]["id"] == 15368 and check["name"] == "sol-max-review"
            and check["external_id"] == f"pr-{number}" and check["head_sha"] == sha
            and check["details_url"] == run_url and check["status"] == "completed"
            and check["conclusion"] == "failure" and check["output"]["title"] == "수정 필요"):
        return
    data = check["output"]["text"]
    if not (data.startswith(FORMAT_START) and data.endswith(FORMAT_END)):
        raise ValueError("줄별 지적 자료를 확인할 수 없습니다.")
    report = json.loads(data[len(FORMAT_START):-len(FORMAT_END)])

    def current():
        pr = api(pull)
        return (pr["state"] == "open" and pr["head"]["sha"] == sha
                and (pr["head"].get("repo") or {}).get("full_name") == repo
                and pr["base"]["ref"] == "development" and pr["base"]["sha"] == report["base"])

    if not current():
        return
    marker = f"<!-- sol-max-review:{check_id} -->"
    reviews = api(f"{pull}/reviews?per_page=100", paginate=True)
    if any((r.get("user") or {}).get("id") == 41898282 and r["commit_id"] == sha and r["state"] == "COMMENTED"
           and (r["body"] or "").startswith(marker + "\n") for r in reviews):
        return
    files = api(f"{pull}/files?per_page=100", paginate=True)
    check_url = f"https://github.com/{repo}/pull/{number}/checks?check_run_id={check_id}"
    payload = review_payload(sha, check_id, check_url, report, files)
    if not current():
        return
    try:
        api(f"{pull}/reviews", payload)
    except RuntimeError as error:
        # GitHub가 diff 위치를 거부한 경우만 요약으로 대체한다. 권한·네트워크 오류는 재실행에 맡긴다.
        if not payload["comments"] or "HTTP 422" not in str(error):
            raise
        if current():
            payload = payload | {"comments": [], "body": payload["body"] +
                                 "\n\nGitHub가 줄 위치를 수락하지 않아 각 지적을 이 요약에 표시합니다."}
            api(f"{pull}/reviews", payload)


if __name__ == "__main__":
    publish(os.environ["GITHUB_REPOSITORY"], os.environ["PR_NUMBER"], os.environ["REVIEW_SHA"],
            os.environ["REVIEW_CHECK"], os.environ["REVIEW_RUN_URL"])
