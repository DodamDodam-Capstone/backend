"""수동 병합 검사의 승인 조건과 발행 대상을 검증한다."""

import json
from pathlib import Path
import unittest
from unittest.mock import Mock, patch

import manual_review as merge
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


class ManualReviewTests(unittest.TestCase):
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

    def test_rereview_requested_after_summary_blocks_the_same_sha(self):
        request = {"id": 31, "created_at": "2026-11-19", "author_association": "MEMBER",
                   "user": {"id": 42, "type": "User"}, "body": "@codex review " + SHA}
        self.assertFalse(merge.codex_clear([summary(), request], SHA))
        edited = request | {"created_at": "2026-11-17", "updated_at": "2026-11-19"}
        self.assertFalse(merge.codex_clear([summary(), edited], SHA))
        self.assertTrue(merge.codex_clear([summary() | {"updated_at": "2026-11-20"}, request], SHA))

    def test_review_change_signal_has_no_write_access_or_pr_checkout(self):
        workflows = Path(merge.__file__).resolve().parents[1] / "workflows"
        signal = (workflows / "review-state-changed.yml").read_text()
        self.assertIn("pull_request_review:", signal)
        self.assertIn("dismissed", signal)
        self.assertIn("permissions: {}", signal)
        self.assertNotIn("uses:", signal)
        self.assertNotIn("secrets.", signal)
        self.assertNotIn(": write", signal)
        trusted = (workflows / "development-auto-merge.yml").read_text()
        self.assertIn("workflows: [Slack Notification, Review State Changed]", trusted)
        self.assertIn("github.ref == 'refs/heads/main'", trusted)
        self.assertNotIn("concurrency:", trusted.split("jobs:")[0])
        self.assertIn("group: backend-manual-review-publisher\n      queue: max", trusted.split("  review:\n", 1)[1])

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
        review = {"reviewDecision": None, "headRefOid": SHA, "baseRefOid": BASE_SHA, "autoMergeAllowed": False}
        with patch.object(merge, "api", side_effect=get), patch.object(merge.subprocess, "run", return_value=checks), \
                patch.object(merge, "pull_reviews", return_value=review) as reviews:
            self.assertEqual(merge.evaluate(REPO, 12, None)[0], ("low", SHA, BASE_SHA))
            for value in (None, review | {"reviewDecision": "CHANGES_REQUESTED"}, review | {"headRefOid": BASE_SHA},
                          review | {"autoMergeAllowed": True}, review | {"autoMergeAllowed": None},
                          review | {"autoMergeRequest": {"enabledAt": "2026-11-19"}}):
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


    def test_own_pending_check_never_blocks_the_other_required_ci(self):
        checks = [{"name": n, "state": "SUCCESS"} for n in merge.REQUIRED]
        gate = {"name": merge.CHECK, "state": "ACTION_REQUIRED"}
        self.assertTrue(merge.ci_clear(checks + [gate]))
        self.assertFalse(merge.ci_clear(checks[1:] + [gate]))

    def test_only_verified_merge_parents_and_current_base_may_receive_success(self):
        target = "c" * 40
        pr = PR | {"merge_commit_sha": target}
        for parents, base, counted, shared, written, passed in (([BASE_SHA, SHA], BASE_SHA, True, False, True, True),
                                                                ([BASE_SHA, SHA], BASE_SHA, False, False, True, False),
                                                                ([BASE_SHA, SHA], BASE_SHA, True, True, True, False),
                                                                ([SHA, BASE_SHA], BASE_SHA, True, False, False, False),
                                                                ([BASE_SHA, SHA], SHA, True, False, True, False)):
            def api(endpoint, data=None, **kwargs):
                if data:
                    return {}
                if "/git/ref/pull/" in endpoint:
                    return {"object": {"sha": target}}
                if "/git/commits/" in endpoint:
                    return {"parents": [{"sha": p} for p in parents]}
                if "/git/ref/heads/" in endpoint:
                    return {"object": {"sha": base}}
                if "/pulls?state=open" in endpoint:
                    return [pr, pr | {"number": 24}] if shared else [pr]
                if "/reviews?" in endpoint:
                    return []
                if "/check-runs?" in endpoint:
                    return {"check_runs": []}
                return pr
            review = {"headRefOid": SHA, "baseRefOid": BASE_SHA, "autoMergeAllowed": False,
                      "reviewDecision": "APPROVED" if counted else "REVIEW_REQUIRED"}
            with self.subTest(parents=parents, base=base, counted=counted, shared=shared), patch.object(merge, "api", side_effect=api) as calls, \
                    patch.object(merge, "pull_reviews", return_value=review), \
                    patch.object(merge, "app_vote_counts", return_value=counted), patch.object(merge, "policy_active", return_value=True), \
                    patch.object(merge, "evaluate", return_value=(("low", SHA, BASE_SHA), "허용")):
                merge.report(REPO, 12, token="checks-only")
                writes = [c for c in calls.call_args_list if len(c.args) > 1]
                vote = base == BASE_SHA and not shared
                self.assertEqual(len(writes), (2 + int(vote)) if written else 1)
                if not written:
                    self.assertNotEqual(writes[0].args[1]["name"], merge.CHECK)
                    self.assertEqual(writes[0].args[1]["conclusion"], "action_required")
                if written:
                    if vote:
                        self.assertEqual(writes[0].args[1]["event"], "APPROVE")
                        self.assertEqual(writes[0].args[1]["commit_id"], SHA)
                    call, info = writes[-2:]
                    self.assertNotEqual(info.args[1]["name"], merge.CHECK)
                    self.assertEqual(info.args[1]["head_sha"], SHA)
                    self.assertEqual(call.args[0], "repos/org/backend/check-runs")
                    self.assertEqual(call.args[1]["head_sha"], target)
                    self.assertEqual(call.args[1]["conclusion"] == "success", passed)

    def test_dynamic_risk_needs_independent_approval_and_high_needs_paid_review(self):
        checks = Mock(returncode=0, stdout=json.dumps([{"name": n, "state": "SUCCESS"} for n in merge.REQUIRED]))
        view = {"headRefOid": SHA, "baseRefOid": BASE_SHA, "reviewDecision": None, "autoMergeAllowed": False}
        for level, human, paid, allowed in (("medium", False, False, False), ("medium", True, False, True),
                                            ("high", False, True, False), ("high", True, False, False),
                                            ("high", True, True, True)):
            data = [PR, [summary()], [FILE]]
            with self.subTest(level=level, human=human, paid=paid), patch.object(merge, "api", side_effect=data), \
                    patch.object(merge, "change_risk", return_value=level), patch.object(merge, "pull_reviews", return_value=view), \
                    patch.object(merge, "latest_opinions", return_value={}), patch.object(merge.subprocess, "run", return_value=checks), \
                    patch.object(merge, "team_approved", return_value=human), patch.object(merge, "paid_clear", return_value=paid):
                decision, reason = merge.evaluate(REPO, 12, None)
                self.assertEqual(decision is not None, allowed)
                if not human:
                    self.assertIn("팀원 승인 필요", reason)

    def test_medium_and_revoked_low_never_keep_the_apps_approval(self):
        url = "repos/org/backend/pulls/12"
        bot = {"id": merge.APP_BOT_ID, "type": "Bot"}
        vote = {"id": 44, "user": bot, "state": "APPROVED", "commit_id": SHA, "body": "<!-- proof -->"}
        human = vote | {"id": 45, "user": {"id": 27, "type": "User"}}
        with patch.object(merge, "api", side_effect=[[vote, human], {}]) as api:
            merge.reconcile_app_approval(url, SHA, "proof", False, "app")
            self.assertEqual(api.call_count, 2)
            self.assertEqual(api.call_args.args[0], url + "/reviews/44/dismissals")
        with patch.object(merge, "api", return_value=[vote, human]) as api:
            merge.reconcile_app_approval(url, SHA, "proof", True, "app")
            self.assertEqual(api.call_count, 1)

    def test_high_risk_fork_points_to_internal_review(self):
        fork = PR | {"head": PR["head"] | {"repo": {"full_name": "fork/backend"}}}
        with patch.object(merge, "api", side_effect=[fork, [summary()], [FILE]]), \
                patch.object(merge, "change_risk", return_value="high"), patch.object(merge, "paid_clear") as paid:
            decision, reason = merge.evaluate(REPO, 12, None)
            self.assertIsNone(decision)
            self.assertIn("내부 브랜치", reason)
            paid.assert_not_called()

    def test_dismissed_app_review_stays_dismissed_for_same_commit(self):
        own = {"state": "DISMISSED", "commit_id": SHA, "user": {"id": merge.APP_BOT_ID, "type": "Bot"}}
        with patch.object(merge, "api", return_value=[own]) as api:
            self.assertIsNone(merge.reconcile_app_approval("pull", SHA, "proof", True, "app"))
            api.assert_called_once()
        stale = own | {"id": 22, "state": "APPROVED", "body": "<!-- previous base -->"}
        with patch.object(merge, "api", side_effect=[[stale], {}]) as api:
            self.assertIsNone(merge.reconcile_app_approval("pull", SHA, "new base", True, "app"))
            self.assertEqual(api.call_count, 2)
            self.assertEqual(api.call_args.args[0], "pull/reviews/22/dismissals")

    def test_success_is_withheld_when_evidence_changes_before_publication(self):
        target = "c" * 40
        pr = PR | {"merge_commit_sha": target}
        start = (("medium", SHA, BASE_SHA), "조건 충족")
        low = (("low", SHA, BASE_SHA), "조건 충족")
        for first, end, late_peers, reason in [(start, (None, "팀원 승인 철회"), [pr], "팀원 승인 철회"),
                                               (low, (None, "Codex 재리뷰 중"), [pr], "Codex 재리뷰 중"),
                                               (start, start, [pr, pr | {"number": 24}], "공유"),
                                               (low, low, [pr], "철회")]:
            peers = iter([[pr], late_peers])
            def read(endpoint, **kwargs):
                if "/pulls?state=open" in endpoint:
                    return next(peers)
                if endpoint.endswith("/git/ref/pull/12/merge"):
                    return {"object": {"sha": target}}
                if "/git/commits/" in endpoint:
                    return {"parents": [{"sha": BASE_SHA}, {"sha": SHA}]}
                if endpoint.endswith("/git/ref/heads/development"):
                    return {"object": {"sha": BASE_SHA}}
                return pr
            validated = []
            def evaluate(*args):
                validated.append(True)
                return first if len(validated) == 1 else end
            def counts(*args):
                return reason != "철회" or len(validated) < 2
            with self.subTest(reason=reason), patch.object(merge, "api", side_effect=read), \
                    patch.object(merge, "existing_check", return_value=None), patch.object(merge, "policy_active", return_value=True), \
                    patch.object(merge, "pull_reviews", return_value={"headRefOid": SHA, "baseRefOid": BASE_SHA,
                                 "reviewDecision": "APPROVED", "autoMergeAllowed": False}), patch.object(merge, "app_vote_counts", side_effect=counts), \
                    patch.object(merge, "latest_opinions", return_value={}), patch.object(merge, "team_approved", return_value=False), \
                    patch.object(merge, "reconcile_app_approval") as approval, patch.object(merge, "publish") as publish, \
                    patch.object(merge, "evaluate", side_effect=evaluate):
                merge.report(REPO, 12, token="app")
                required = publish.call_args_list[0].args
                self.assertEqual(required[2], merge.CHECK)
                self.assertEqual(required[4], "action_required")
                self.assertIn(reason, required[5])
                if first == low and end != low:
                    self.assertEqual([c.args[3] for c in approval.call_args_list], [True, False])

    def test_specific_app_vote_requires_push_access_and_current_head(self):
        row = {"state": "APPROVED", "commit": {"oid": SHA}, "authorCanPushToRepository": True}
        for changes, expected in (({}, True), ({"authorCanPushToRepository": False}, False),
                                  ({"state": "DISMISSED"}, False), ({"commit": {"oid": BASE_SHA}}, False)):
            with patch.object(merge, "api", return_value={"data": {"node": row | changes}}):
                self.assertEqual(merge.app_vote_counts({"node_id": "review"}, SHA), expected)

    def test_rollout_requires_source_bound_check_and_native_approval(self):
        def rules(app, native, extra):
            return [{"type": "pull_request", "parameters": {"required_approving_review_count": 1, "require_last_push_approval": native}},
                    {"type": "required_status_checks", "parameters": {
                "required_status_checks": [{"context": merge.CHECK, "integration_id": app}]
                    + [{"context": name} for name in merge.REQUIRED] + extra}}]
        ok = {"status": "completed", "conclusion": "success"}
        head = {(name, 15368): ok for name in merge.REQUIRED}
        for source, native, extra, target, head_ci, expected in (
                (0, True, [], {}, head, False), (merge.APP_ID, False, [], {}, head, False),
                (merge.APP_ID, True, [], {}, head, True), (merge.APP_ID, True, [], {}, {}, False),
                (merge.APP_ID, True, [{"context": "new", "integration_id": 7}], {}, head, False),
                (merge.APP_ID, True, [], {("backend-quality", 9): ok}, head, False),
                (merge.APP_ID, True, [], {("backend-quality", 15368): ok | {"conclusion": "failure"}}, head, False)):
            with patch.object(merge, "api", return_value=rules(source, native, extra)), \
                    patch.object(merge, "ci_on_commit", side_effect=[target, head_ci]):
                self.assertEqual(merge.policy_active("repo", SHA, "merge-sha"), expected)

    def test_unknown_merge_state_invalidates_previous_required_check(self):
        target = "c" * 40
        prior = {"id": 4, "conclusion": "success", "output": {"title": "통과"},
                 "external_id": f"manual-review:12:{BASE_SHA}:{SHA}"}
        info = prior | {"id": 5, "output": {"title": "통과", "summary": f"test merge `{target}`"}}
        with patch.object(merge, "api", return_value=PR | {"mergeable": None, "merge_commit_sha": None}), \
                patch.object(merge.time, "sleep"), patch.object(merge, "existing_check", side_effect=[info, prior]), \
                patch.object(merge, "reconcile_app_approval") as reconcile, patch.object(merge, "publish") as publish:
            merge.report(REPO, 12, token="app")
            self.assertEqual(publish.call_args_list[0].args[1:5], (target, merge.CHECK, prior["external_id"], "action_required"))
            self.assertFalse(reconcile.call_args.args[3])

    def test_merge_ref_read_error_attempts_check_and_approval_cleanup(self):
        pr = PR | {"merge_commit_sha": "c" * 40}
        own = {"id": 7, "external_id": f"manual-review:12:{BASE_SHA}:{SHA}", "conclusion": "success", "output": {}}
        with patch.object(merge, "api", side_effect=[pr, RuntimeError("ref unavailable")]), \
                patch.object(merge, "existing_check", side_effect=[None, own]), patch.object(merge, "publish") as publish, \
                patch.object(merge, "reconcile_app_approval") as approval:
            merge.report(REPO, 12, token="app")
            self.assertEqual(publish.call_args_list[0].args[2:5], (merge.CHECK, own["external_id"], "action_required"))
            self.assertFalse(approval.call_args.args[3])

    def test_ci_evidence_is_latest_for_the_exact_sha_name_and_app(self):
        row = {"id": 3, "name": "backend-quality", "head_sha": SHA, "app": {"id": 15368}, "conclusion": "failure"}
        with patch.object(merge, "api", return_value={"check_runs": [
                row, row | {"id": 2, "conclusion": "success"}, row | {"id": 4, "head_sha": BASE_SHA}]}):
            self.assertEqual(merge.ci_on_commit("repo", SHA), {("backend-quality", 15368): row})
