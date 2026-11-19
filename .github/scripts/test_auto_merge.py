"""실제 자동 병합 입력의 경계와 커밋 고정을 검증한다."""

import json
import unittest
from unittest.mock import Mock, patch

import auto_merge as merge
from notify_pr_review import CODEX_APP_ID, CODEX_BOT_ID
from review_routing import BASE, change_risk


SHA = "a" * 40
BASE_SHA = "b" * 40
REPO = "org/backend"
PR = {"number": 12, "state": "open", "draft": False, "changed_files": 1, "title": "✨ feat: SCRUM-12 [BE] 제목",
      "head": {"ref": "feature/SCRUM-12-change", "sha": SHA, "repo": {"full_name": REPO}},
      "base": {"ref": "development", "sha": BASE_SHA}, "user": {"login": "author"},
      "mergeable": True, "mergeable_state": "blocked"}
FILE = {"filename": BASE + "child/dto/Form.java", "patch": "@@ -1 +1 @@\n-  String name;\n+  String nickname;",
        "status": "modified", "additions": 1, "deletions": 1}


def summary(**updates):
    state = dict(commit_sha=SHA, status="completed", has_any_findings=False, has_unresolved_findings=False) | updates
    return {"id": 30, "updated_at": "2026-11-18", "user": {"id": CODEX_BOT_ID},
            "performed_via_github_app": {"id": CODEX_APP_ID},
            "body": "<!-- codex-pull-request-review-summary -->\n<!-- codex-review-state:v1 "
                    + json.dumps({"code_review": state}) + " -->"}


class AutoMergeTests(unittest.TestCase):
    def test_latest_authenticated_codex_must_have_no_findings(self):
        self.assertTrue(merge.codex_clear([summary()], SHA))
        for updates in (dict(commit_sha=BASE_SHA), dict(status="running"), dict(has_any_findings=True),
                        dict(has_unresolved_findings=True), dict(has_any_findings=None)):
            later = summary(**updates) | {"id": 31, "updated_at": "2026-11-19"}
            self.assertFalse(merge.codex_clear([later, summary()], SHA))
        self.assertFalse(merge.codex_clear([summary() | {"performed_via_github_app": {"id": 1}}], SHA))

    def test_change_risk_includes_old_paths_and_unverifiable_source(self):
        self.assertEqual(change_risk(PR, [FILE]), "low")
        for edit in ({"patch": ""}, {"deletions": 2}, {"filename": "Dockerfile"},
                     {"patch": "@@ -1 +1 @@\n-  check();\n+  send();"},
                     {"filename": "src/test/java/FooTests.java"}):
            self.assertEqual(change_risk(PR, [FILE | edit]), "medium", edit)
        self.assertEqual(change_risk(PR, [FILE | {"previous_filename": BASE + "auth/Foo.java"}]), "high")
        self.assertEqual(change_risk(PR, [FILE | {"patch": "@@ -1,2 +1,2 @@\n- a.first();\n- a.last();\n+ a.last();\n+ a.first();",
                                                  "additions": 2, "deletions": 2}]), "high")

    def test_isolated_test_addition_and_changed_runtime_configuration(self):
        lines = ["class FormTests {", "@Test", "void acceptsName() { assertTrue(true); }", "}"]
        def new_test(content):
            return dict(filename="src/test/java/FormTests.java", status="added", additions=len(content), deletions=0,
                        patch="@@ -0,0 +1,4 @@\n" + "\n".join("+" + line for line in content))
        self.assertEqual(change_risk(PR, [new_test(lines)]), "low")
        self.assertEqual(change_risk(PR, [new_test(["@TestConfiguration"] + lines)]), "medium")

    def test_out_of_scope_prs(self):
        self.assertTrue(merge.eligible(PR, REPO))
        for edit in ({"draft": True}, {"state": "closed"}, {"base": {"ref": "main"}},
                     {"head": PR["head"] | {"ref": "development"}},
                     {"head": PR["head"] | {"repo": {"full_name": "fork/backend"}}}):
            self.assertFalse(merge.eligible(PR | edit, REPO))

    def test_only_successful_required_checks(self):
        checks = [{"name": n, "state": "SUCCESS"} for n in merge.REQUIRED]
        self.assertTrue(merge.ci_clear(checks))
        self.assertFalse(merge.ci_clear(checks[1:]))
        for state in ("PENDING", "SKIPPED", "NEUTRAL", "FAILURE"):
            self.assertFalse(merge.ci_clear(checks + [{"name": "extra", "state": state}]))

    def test_high_requires_current_human_and_proven_paid_success(self):
        for risk, human, paid, expected in (("low", False, False, True), ("medium", False, True, False),
                                            ("medium", True, False, True), ("high", False, True, False),
                                            ("high", True, False, False), ("high", True, True, True)):
            self.assertEqual(merge.allowed(risk, human, paid), expected)

    def test_paid_proof_must_come_from_trusted_workflow_for_this_check_and_base(self):
        check = {"id": 818, "app": {"id": 15368}, "head_sha": SHA, "name": "sol-max-review",
                 "status": "completed", "conclusion": "success", "output": {"title": "추가 지적 없음"},
                 "external_id": "pr-12@https://github.com/org/backend/actions/runs/42"}
        run = {"event": "issue_comment", "path": ".github/workflows/notify-slack.yml", "conclusion": "success",
               "head_branch": "main", "head_repository": {"full_name": REPO}}
        job = {"name": f"sol-max-clear 12 {SHA} {BASE_SHA} 818", "conclusion": "success"}
        def check_result(row, source=run, jobs=None):
            data = Mock(side_effect=[{"check_runs": [row]}, source, {"jobs": [job] if jobs is None else jobs}])
            with patch.object(merge, "api", data):
                return merge.paid_clear(REPO, PR)
        self.assertTrue(check_result(check))
        development = run | {"event": "workflow_dispatch", "head_branch": "development", "head_sha": BASE_SHA}
        self.assertTrue(check_result(check, development))
        for bad in ({"head_branch": "feature/test"}, {"head_sha": SHA}, {"event": "pull_request"},
                    {"path": ".github/workflows/ci.yml"}):
            self.assertFalse(check_result(check, development | bad))
        for edit in ({"conclusion": "failure"}, {"id": 819}, {"head_sha": BASE_SHA}, {"external_id": "pr-13"}):
            self.assertFalse(check_result(check | edit))
        self.assertFalse(check_result(check, run | {"event": "pull_request"}))
        self.assertFalse(check_result(check, jobs=[]))

    def test_dispatch_rechecks_head_and_does_not_queue_future_heads(self):
        for apply, head in ((False, SHA), (True, BASE_SHA), (True, SHA)):
            with self.subTest(apply=apply, head=head), patch.object(merge, "evaluate", return_value=(
                    ("low", SHA, BASE_SHA), "확인")), patch.dict("os.environ", LOW_RISK_TOKEN="low-token"), patch.object(
                        merge, "api", side_effect=[PR | {"head": PR["head"] | {"sha": head}}, {"object": {"sha": BASE_SHA}}, {"merged": True}]) as api:
                merge.reconcile(REPO, 12, apply)
                writes = [call for call in api.call_args_list if call.kwargs.get("method") == "PUT"]
                self.assertEqual(len(writes), int(apply and head == SHA))
                if writes:
                    self.assertEqual(writes[0].args[1]["sha"], SHA)
                    self.assertEqual(writes[0].kwargs["token"], "low-token")

    def test_ordinary_work_pr_can_merge_with_no_human_or_jira_evidence(self):
        def get(path, *args, **kwargs):
            if "/comments?" in path:
                return [summary()]
            if "/reviews?" in path:
                return []
            if "/files?" in path:
                return [FILE]
            return PR
        checks = Mock(returncode=0, stdout=json.dumps([{"name": n, "state": "SUCCESS"} for n in merge.REQUIRED]))
        review = {"reviewDecision": None, "headRefOid": SHA, "baseRefOid": BASE_SHA}
        with patch.object(merge, "api", side_effect=get), patch.object(merge.subprocess, "run", return_value=checks), \
                patch.object(merge, "pull_reviews", return_value=review) as reviews:
            self.assertEqual(merge.evaluate(REPO, 12, None)[0], ("low", SHA, BASE_SHA))
            for value in (None, review | {"reviewDecision": "CHANGES_REQUESTED"}, review | {"headRefOid": BASE_SHA}):
                reviews.return_value = value
                self.assertIsNone(merge.evaluate(REPO, 12, None)[0])

    def test_paginated_unresolved_thread(self):
        def page(resolved, more):
            return {"data": {"repository": {"pullRequest": {"reviewThreads": {
                "nodes": [{"isResolved": resolved}], "pageInfo": {"hasNextPage": more, "endCursor": "c"}}}}}}
        with patch.object(merge, "api", side_effect=[page(True, True), page(False, False)]) as calls:
            self.assertIsNone(merge.pull_reviews(REPO, 12))
            self.assertEqual(calls.call_args.args[1]["variables"]["after"], "c")

    def test_latest_team_review_after_push(self):
        for voter, state, reviewed_sha, valid in (("reviewer", "APPROVED", SHA, True),
                                                ("author", "APPROVED", SHA, False), ("pusher", "APPROVED", SHA, False),
                                                ("reviewer", "DISMISSED", SHA, False), ("reviewer", "APPROVED", BASE_SHA, False)):
            user = {"login": voter, "type": "User"}
            reviews = [{"id": 1, "state": "APPROVED", "commit_id": SHA, "user": user},
                       {"id": 2, "state": state, "commit_id": reviewed_sha, "user": user}]
            vals = [reviews, [{"after": SHA, "actor": {"login": "pusher"}}], [user]]
            with self.subTest(voter=voter, state=state), patch.object(merge, "api", side_effect=vals):
                opinions = merge.latest_opinions(REPO, 12)
                self.assertEqual(merge.team_approved(REPO, PR, "app-token", opinions), valid)

    def test_medium_and_high_use_non_exempt_token_and_preserve_linked_issue_closure(self):
        for level in ("medium", "high"):
            with self.subTest(level=level), patch.object(merge, "evaluate", return_value=((level, SHA, BASE_SHA), "확인")), \
                    patch.object(merge, "api", side_effect=[PR, {"object": {"sha": BASE_SHA}}, {"merged": True}]) as api, \
                    patch.dict("os.environ", GH_TOKEN="ordinary", LOW_RISK_TOKEN="low-token"), \
                    patch.object(merge.subprocess, "run") as close:
                merge.reconcile(REPO, 12, True)
                self.assertIsNone(api.call_args.kwargs["token"])
                self.assertEqual(close.call_args.kwargs["env"]["GITHUB_TOKEN"], "ordinary")
