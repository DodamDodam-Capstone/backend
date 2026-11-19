"""PR 알림이 실제로 확인한 근거와 최신 커밋만 표시하는지 검증한다."""

import unittest
from unittest.mock import Mock

from notify_pr_review import build_payload


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
        self.assertIn("위험도 낮음 · 리뷰 통과", self.message("7")["text"])
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
        self.assertIn("팀원 승인", self.message("7")["text"])

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


if __name__ == "__main__":
    unittest.main()
