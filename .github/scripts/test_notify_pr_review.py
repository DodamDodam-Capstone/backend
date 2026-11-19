"""PR 알림이 실제로 확인한 근거와 최신 커밋만 표시하는지 검증한다."""

import json
import unittest
from unittest.mock import Mock

from notify_pr_review import CODEX_APP_ID, CODEX_BOT_ID, build_payload


class SlackReviewTests(unittest.TestCase):
    def setUp(self):
        self.sha = "a" * 40
        self.pr = {"number": 62, "base": {"ref": "development"}, "head": {"ref": "feature/SCRUM-181-review", "sha": self.sha}}
        self.files = [{"filename": "src/Child.java"}]
        self.checks = []
        self.current_sha = self.sha
        self.run = {"name": "Backend CI", "event": "pull_request", "head_sha": self.sha,
                    "head_branch": self.pr["head"]["ref"], "repository": {"full_name": "org/backend"},
                    "conclusion": "success", "html_url": "https://github.com/org/backend/actions/runs/42"}
        self.helper = Mock()
        self.helper.associated_pull_requests.return_value = [self.pr]
        self.helper.github_json.side_effect = self.get

    def get(self, url, token):
        if "/check-runs?" in url:
            return {"check_runs": self.checks}
        if "/files?" in url:
            page = int(url.split("page=")[-1])
            return self.files[(page - 1) * 100:page * 100]
        self.assertTrue(url.endswith("/pulls/62"))
        return {"head": {"sha": self.current_sha}, "base": self.pr["base"], "changed_files": len(self.files)}

    def message(self, app_id=""):
        return build_payload({"workflow_run": self.run}, "test-token", self.helper, app_id)

    def check_record(self, **changes):
        return {"name": "review-risk", "id": 1, "app": {"id": 7}, "head_sha": self.sha,
                "external_id": "pr-62", "status": "completed", "conclusion": "success",
                "output": {"title": "통과"}, **changes}

    def test_green_ci_without_review_stays_pending(self):
        result = self.message()
        self.assertIn("위험도 미판정", result["text"])
        self.assertIn("@codex review", result["blocks"][1]["text"]["text"])
        self.assertIn(self.sha[:7], result["blocks"][2]["elements"][1]["text"])

    def test_old_protected_path_on_second_page(self):
        self.files = [{"filename": f"docs/{i}"} for i in range(100)] + [
            {"filename": "docs/config", "previous_filename": ".github/<@here>"}]
        result = self.message()
        self.assertIn("위험도 높음", result["text"])
        self.assertEqual(result["blocks"][1]["text"]["type"], "plain_text")
        self.assertIn(".github/<@here>", result["blocks"][1]["text"]["text"])

    def test_stale_event_and_failed_api(self):
        self.current_sha = "new"
        self.assertIn("이전 커밋", self.message()["text"])
        self.helper.github_json.side_effect = RuntimeError("denied")
        self.assertIn("조회 대기", self.message()["text"])

    def test_failed_ci_does_not_invent_code_defect(self):
        self.run["conclusion"] = "failure"
        self.assertIn("위험도 미판정 · CI 확인", self.message()["text"])

    def test_only_current_trusted_per_pr_verdict_counts(self):
        for changes in ({"external_id": "pr-63"}, {"app": {"id": 8}}, {"head_sha": "old"},
                        {"status": "in_progress"}, {"name": "some-check"}):
            with self.subTest(changes=changes):
                self.checks = [self.check_record(**changes)]
                self.assertIn("미판정", self.message("7")["text"])
        self.checks = [self.check_record()]
        self.assertIn("위험도 미판정", self.message("7")["text"])
        self.assertIn("위험도 낮음 · 리뷰 통과", build_payload(self.codex_event(), "token", self.helper, "7")["text"])
        self.files = [{"filename": ".github/workflows/ci.yml"}]
        self.assertIn("위험도 높음 · 팀원 확인", self.message("7")["text"])

    def test_trusted_findings_and_required_human(self):
        self.checks = [self.check_record(conclusion="failure", output={"title": "수정 필요"})]
        self.assertIn("위험도 중간 · 수정 필요", self.message("7")["text"])
        self.files = [{"filename": "AGENTS.md"}]
        self.assertIn("위험도 높음 · 수정 필요", self.message("7")["text"])
        self.checks = [self.check_record(conclusion="action_required", output={"title": "팀원 판단"})]
        self.assertIn("팀원 확인", self.message("7")["text"])
        self.checks = [self.check_record(output={"title": "팀원 승인"})]
        self.assertIn("팀원 승인", build_payload(self.codex_event(), "token", self.helper, "7")["text"])

    def test_shared_head_uses_workflow_base_filter(self):
        probe = self.pr | {"number": 63, "base": {"ref": "test/probe"}}
        self.helper.associated_pull_requests.return_value = [probe, self.pr]
        self.assertIn("PR #62", self.message()["text"])
        self.run["name"] = "PR Title"
        self.assertIn("PR 확인", self.message()["text"])
        self.assertNotIn("#63", self.message()["text"])

    def test_non_pr_uses_existing_notification(self):
        self.run["event"] = "push"
        self.assertIs(self.message(), self.helper.build_payload.return_value)

    def codex_event(self, status="completed", findings=False):
        state = {"code_review": {"status": status, "commit_sha": self.sha, "has_any_findings": findings, "has_unresolved_findings": findings}}
        body = "<!-- codex-pull-request-review-summary -->\n<!-- codex-review-state:v1 " + json.dumps(state) + " -->"
        self.comment = {"id": 88, "user": {"id": CODEX_BOT_ID}, "body": body,
                        "performed_via_github_app": {"id": CODEX_APP_ID},
                        "html_url": "https://github.com/org/backend/pull/62#issuecomment-88",
                        "issue_url": "https://api.github.com/repos/org/backend/issues/62"}

        def lookup(url, token):
            if url.endswith("/issues/comments/88"):
                return self.comment
            if url.endswith("/pulls/62"):
                return self.pr | {"changed_files": len(self.files)}
            return self.get(url, token)

        self.helper.github_json.side_effect = lookup
        return {"repository": self.run["repository"], "issue": {"number": 62, "pull_request": {"url": "pull/62"}},
                "comment": dict(self.comment), "sender": {"id": CODEX_BOT_ID}}

    def test_official_clear_still_requires_verifiable_patch(self):
        result = build_payload(self.codex_event(), "token", self.helper)
        self.assertIn("중간 · 팀원 확인", result["text"])
        self.assertIn("공식 리뷰", result["blocks"][2]["elements"][1]["text"])
        self.files = [{"filename": ".github/workflows/ci.yml"}]
        result = build_payload(self.codex_event(), "token", self.helper)
        self.assertIn("높음 · 팀원 확인", result["text"])

    def test_only_official_current_internal_high_risk_review_queues_payment(self):
        queue = Mock()
        self.pr.update(state="open", head=self.pr["head"] | {"repo": {"full_name": "org/backend"}})
        self.files = [{"filename": ".github/workflows/ci.yml"}]
        event = self.codex_event()
        build_payload({"workflow_run": self.run}, "token", self.helper, on_high_risk=queue)
        build_payload(event | {"sender": {"id": 1}}, "token", self.helper, on_high_risk=queue)
        with unittest.mock.patch.dict(self.pr["head"]["repo"], {"full_name": "fork/backend"}):
            build_payload(event, "token", self.helper, on_high_risk=queue)
        queue.assert_not_called()
        build_payload(event, "token", self.helper, on_high_risk=queue)
        queue.assert_called_once_with(self.sha)
        self.files = [{"filename": "README.md"}]
        build_payload(event, "token", self.helper, on_high_risk=queue)
        queue.assert_called_once()

    def test_release_keeps_human_risk_notification_without_queueing_sol_max(self):
        self.pr.update(state="open", base={"ref": "main"}, head={
            "sha": self.sha, "ref": "development", "repo": {"full_name": "org/backend"}})
        self.files = [{"filename": ".github/workflows/ci.yml"}]
        for findings in (False, True):
            with self.subTest(findings=findings):
                queue = Mock()
                result = build_payload(self.codex_event(findings=findings), "token", self.helper, on_high_risk=queue)
                self.assertIn("위험도 높음", result["text"])
                queue.assert_not_called()

    def test_official_unresolved_findings_are_actionable(self):
        result = build_payload(self.codex_event(findings=True), "token", self.helper)
        self.assertIn("중간 · 리뷰 지적 확인", result["text"])

    def test_new_findings_override_older_success_on_the_same_commit(self):
        for title in ("통과", "팀원 승인"):
            with self.subTest(title=title):
                self.checks = [self.check_record(output={"title": title})]
                result = build_payload(self.codex_event(findings=True), "token", self.helper, "7")
                self.assertIn("중간 · 리뷰 지적 확인", result["text"])

    def test_skips_unsigned_stale_repeated_and_running_comments(self):
        event = self.codex_event(status="running")
        self.assertIsNone(build_payload(event, "token", self.helper))
        event = self.codex_event()
        event["sender"] = {"id": 42}
        self.assertIsNone(build_payload(event, "token", self.helper))
        event = self.codex_event()
        self.comment["performed_via_github_app"] = {"id": 99}
        self.assertIsNone(build_payload(event, "token", self.helper))
        event = self.codex_event()
        event["changes"] = {"body": {"from": event["comment"]["body"]}}
        self.assertIsNone(build_payload(event, "token", self.helper))
        event = self.codex_event()
        self.pr["head"] = self.pr["head"] | {"sha": "f" * 40}
        self.assertIsNone(build_payload(event, "token", self.helper))


if __name__ == "__main__":
    unittest.main()
