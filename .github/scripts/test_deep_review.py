"""과금 경계와 결과가 GitHub 승인과 독립적으로 유지되는지 검사한다."""

import io
import json
import unittest
from pathlib import Path
from unittest.mock import Mock, patch

import deep_review as deep
from review_routing import BASE, reordered_calls


class DeepReviewTests(unittest.TestCase):
    def setUp(self):
        self.sha = "a" * 40
        self.repo = "org/backend"
        self.pr = {"head": {"sha": self.sha, "repo": {"full_name": self.repo}},
                   "base": {"sha": "b" * 40, "ref": "development"}, "state": "open", "draft": False,
                   "title": "SCRUM-42", "changed_files": 1, "body": "완료 조건"}
        self.file = {"filename": ".github/workflows/ci.yml", "patch": "@@ -1 +1 @@\n-old\n+new",
                     "status": "modified", "additions": 1, "deletions": 1}
        self.checks = []
        self.writes = []
        self.paid = self.enter_mock("model_review", return_value=("success", "추가 지적 없음", "판단 근거", []))
        self.enter_mock("api", side_effect=self.api)

    def enter_mock(self, name, **kwargs):
        handle = patch.object(deep, name, **kwargs)
        self.addCleanup(handle.stop)
        return handle.start()

    def api(self, path, payload=None, **kwargs):
        if payload is None:
            if "/check-runs?" in path:
                return {"check_runs": self.checks}
            if "/files?" in path:
                return [self.file]
            self.assertTrue(path.endswith("/pulls/62"))
            return self.pr
        self.writes.append(payload)
        if kwargs.get("method") == "PATCH":
            self.checks[-1].update(payload)
        else:
            self.checks.append(dict(payload, id=711))
        return self.checks[-1]

    def run_review(self, key="test-key"):
        return deep.run(self.repo, 62, self.sha, key)

    def test_paid_once_per_pr_head_and_no_automatic_approval(self):
        self.checks.append({"external_id": "pr-61"})
        msg = self.run_review()
        self.assertEqual(self.writes[0]["status"], "in_progress")
        self.assertEqual(self.writes[-1]["conclusion"], "success")
        self.assertIn("팀원", msg["blocks"][1]["text"]["text"])
        self.assertIn("gpt-6.2-sol / max", msg["text"])
        self.assertIn(self.sha[:7], msg["text"])
        self.assertIsNone(self.run_review())
        self.paid.assert_called_once()

    def test_attests_only_fresh_success_for_this_head_and_base(self):
        proof = Mock()
        deep.run(self.repo, 62, self.sha, "test", record_clear=proof)
        proof.assert_called_once_with(f"62 {self.sha} {self.pr['base']['sha']} 711")
        deep.run(self.repo, 62, self.sha, "test", record_clear=proof)
        proof.assert_called_once()
        self.checks.clear()
        self.paid.return_value = ("action_required", "확인 필요", "근거", [])
        deep.run(self.repo, 62, self.sha, "test", record_clear=proof)
        proof.assert_called_once()

    def test_changes_can_retry_publication_without_repeating_payment(self):
        self.paid.return_value = ("failure", "수정 필요", "근거", [{"path": self.file["filename"], "anchor": "R1", "body": "수정"}])
        queue = Mock()
        def execute(run_url):
            return deep.run(self.repo, 62, self.sha, "test", run_url=run_url, queue_review=queue)
        self.assertIn("수정 필요", execute("run/42")["text"])
        self.assertIn("R1", self.writes[-1]["output"]["text"])
        queue.assert_called_once_with(711)
        self.checks[0]["details_url"] = "https://github.com/org/backend/runs/711"
        execute("run/42")
        self.assertEqual(queue.call_count, 2)
        execute("run/43")
        self.assertEqual(queue.call_count, 2)
        self.paid.assert_called_once()
        self.assertEqual(len(self.checks), 1)

    def test_legacy_paid_check_prevents_recharging_when_original_run_is_unknown(self):
        self.checks.append({"id": 21, "external_id": "pr-62", "conclusion": "failure",
                            "details_url": "https://github.com/org/backend/runs/21", "output": {"title": "수정 필요"}})
        queue = Mock()
        deep.run(self.repo, 62, self.sha, "test", run_url="run/42", queue_review=queue)
        self.paid.assert_not_called()
        queue.assert_not_called()

    def test_no_paid_call_for_other_targets(self):
        for change in ({"state": "closed"}, {"draft": True}, {"head": {"sha": self.sha, "repo": {"full_name": "fork/backend"}}},
                       {"head": self.pr["head"] | {"sha": "c"*40}}, {"base": {"ref": "test", "sha": "d"*40}}):
            with self.subTest(change=change), patch.dict(self.pr, change):
                self.assertIsNone(self.run_review())
        self.file["filename"] = "docs/note.md"
        self.assertIsNone(self.run_review())
        self.paid.assert_not_called()

    def test_development_release_to_main_never_calls_model_or_creates_check(self):
        # 실제 승격 방향으로 보호 파일과 고위험 판정이 있어도 유료 호출은 0회다.
        self.pr["head"]["ref"] = "development"
        self.pr["base"]["ref"] = "main"
        self.assertTrue(deep.high_risk_reason(self.pr, [self.file]))
        self.assertIsNone(self.run_review())
        self.paid.assert_not_called()
        self.assertEqual(self.writes, [])

    def test_retarget_to_main_before_charge(self):
        changed = self.pr | {"base": self.pr["base"] | {"ref": "main"}}
        snapshots = iter([self.pr, changed])
        with patch.object(deep, "api", side_effect=lambda path, **kw:
                          next(snapshots) if path.endswith("/pulls/62") else self.api(path, **kw)):
            self.assertIsNone(self.run_review())
        self.paid.assert_not_called()
        self.assertEqual(self.writes, [])

    def test_known_flow_service_and_moved_protected_file(self):
        self.file["filename"] = BASE + "conversation/application/TurnAcceptanceService.java"
        self.assertIn("대화 턴", deep.high_risk_reason(self.pr, [self.file]))
        self.file.update(filename="docs/guide.md", previous_filename="AGENTS.md")
        self.assertIn("CI·리뷰", deep.high_risk_reason(self.pr, [self.file]))

    def test_workflow_serializes_each_paid_head_and_only_executes_trusted_workflow_code(self):
        workflow = (Path(__file__).resolve().parents[1] / "workflows/notify-slack.yml").read_text()
        paid_job, publisher = workflow.split("\n  deep-review:", 1)[1].split("\n  publish-review:", 1)
        self.assertIn("pull-requests: write", publisher)
        self.assertNotIn("OPENAI_API_KEY", publisher)
        self.assertNotIn("checks: write", publisher)
        self.assertIn("ref: ${{ github.workflow_sha }}", publisher)
        for guard in ("needs.notify.outputs.deep_sha != ''", "environment: sol-max-review",
                      "group: sol-max-${{ github.event.issue.number || inputs.number }}-${{ needs.notify.outputs.deep_sha }}",
                      "cancel-in-progress: false", "ref: ${{ github.workflow_sha }}", "checks: write"):
            self.assertIn(guard, paid_job)
        self.assertIn("timeout-minutes: 35", paid_job)
        self.assertNotIn("pull-requests: write", paid_job)
        self.assertNotIn("pull_request_target", workflow)
        self.assertIn("github.ref == 'refs/heads/development' && github.ref_protected", workflow)
        ci = (Path(__file__).resolve().parents[1] / "workflows/ci.yml").read_text()
        relay = ci.split("  request-development-review:", 1)[1].split("  quality:", 1)[0]
        self.assertIn("pull_request.head.repo.full_name == github.repository", relay)
        self.assertIn("--ref development", relay)
        self.assertNotIn("actions/checkout", relay)
        self.assertNotIn("secrets.", relay)

    def test_reordered_calls_outside_protected_paths(self):
        diff = "@@ -1,2 +1,2 @@\n-    ledger.reserve();\n     ledger.commit();\n+    ledger.reserve();"
        self.file.update(filename=BASE+"child/application/ChildService.java", patch=diff)
        self.assertIn("호출 순서", deep.high_risk_reason(self.pr, [self.file]))
        self.assertFalse(reordered_calls(diff.replace("+    ledger.reserve();", "+    ledger.notify();")))
        self.assertFalse(reordered_calls(diff.replace("+    ledger.reserve();", "@@ -20 +20 @@\n+    ledger.reserve();")))

    def test_missing_key_or_partial_patch_reaches_human_without_charge(self):
        self.assertIn("미실행", self.run_review(key="")["text"])
        for change in ({"additions": 2}, {"patch": ""}, {"patch": "+" + "x"*deep.INPUT_BYTES, "deletions": 0}):
            with self.subTest(change=change), patch.dict(self.file, change):
                self.assertIn("미실행", self.run_review()["text"])
                self.assertEqual(self.writes[-1]["conclusion"], "action_required")
        self.paid.assert_not_called()
        self.run_review()
        self.paid.assert_called_once()

    def test_failed_paid_request_is_not_retried(self):
        self.paid.side_effect = TimeoutError()
        self.assertIn("미완료", self.run_review()["text"])
        self.assertEqual(self.writes[-1]["conclusion"], "action_required")
        self.assertIsNone(self.run_review())
        self.paid.assert_called_once()

    def test_push_during_paid_review_discards_verdict(self):
        def result(*args):
            self.pr["head"]["sha"] = "c"*40
            return "failure", "수정 필요", "근거", [{"path": "a", "anchor": "R1", "body": "지적"}]
        self.paid.side_effect = result
        self.assertIn("이전 변경 기준", self.run_review()["text"])
        self.assertEqual(self.writes[-1]["conclusion"], "neutral")


class ResponsesTests(unittest.TestCase):
    @patch.object(deep, "urlopen")
    def test_reasoning_model_budget_and_structured_response(self, post):
        findings = [{"path": "A.java", "anchor": "R3", "body": "호출 순서를 확인하세요."}]
        report = {"evidence": "파일 A 호출 순서", "assessment": {"decision": "changes", "findings": findings}}
        part = {"type": "output_text", "text": json.dumps(report)}
        post.return_value = io.BytesIO(json.dumps({"status": "completed", "output": [
            {"type": "reasoning"}, {"type": "message", "content": [part]}]}).encode())
        self.assertEqual(deep.model_review("입력", "test"), ("failure", "수정 필요", "파일 A 호출 순서", findings))
        self.assertEqual(post.call_args.kwargs["timeout"], 30 * 60)
        request = post.call_args.args[0]
        body = json.loads(request.data)
        self.assertEqual(request.full_url, "https://api.openai.com/v1/responses")
        self.assertEqual((body["model"], body["reasoning"], body["max_output_tokens"]), ("gpt-6.2-sol", {"effort": "max"}, 48_000))
        self.assertFalse(body["store"])
        self.assertNotIn("tools", body)

    @patch.object(deep, "urlopen")
    def test_refusal_or_incomplete_never_passes(self, post):
        for result in ({"status": "incomplete"}, {"status": "completed", "output": []},
                       {"status": "completed", "output": [{"type": "message", "content": [{"type": "refusal"}]}]}):
            with self.subTest(result=result):
                post.return_value = io.BytesIO(json.dumps(result).encode())
                with self.assertRaises(ValueError):
                    deep.model_review("입력", "test")


if __name__ == "__main__":
    unittest.main()
