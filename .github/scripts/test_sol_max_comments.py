"""Sol Max의 유료 판정과 GitHub 리뷰 쓰기 권한의 경계를 검증한다."""

import json
import unittest
from unittest.mock import patch

import deep_review as deep
import sol_max_comments as comments


class CommentTests(unittest.TestCase):
    def setUp(self):
        self.repo = "org/backend"
        self.sha = "a" * 40
        self.base = "b" * 40
        self.file = {"filename": "new/A.java", "previous_filename": "old/A.java", "status": "renamed",
                     "additions": 2, "deletions": 1, "patch":
                     "@@ -5,2 +8,2 @@ method\n-old\n+new\n same\n@@ -55,0 +60 @@\n+last\n\\ No newline at end of file"}
        self.pr = {"number": 60, "state": "open", "base": {"ref": "development", "sha": self.base},
                   "head": {"repo": {"full_name": self.repo}, "sha": self.sha}, "title": "test", "body": "task"}
        self.findings = [{"path": self.file["filename"], "anchor": a, "body": b}
                         for a, b in [("L5", "삭제 줄"), ("R9", "문맥 줄"), ("R60", "추가 줄"), ("R30", "모호한 줄")]]
        self.findings.append({"path": "outside.java", "anchor": "R60", "body": "@everyone\n# title"})
        self.check = {"id": 88, "status": "completed", "conclusion": "failure", "external_id": "pr-60",
                      "app": {"id": 15368}, "name": deep.CHECK, "head_sha": self.sha, "details_url": "run/123",
                      "output": {"title": "수정 필요", "text": comments.receipt(self.base, "요약", self.findings)}}
        self.reviews = []
        self.posts = []
        handle = patch.object(comments, "api", side_effect=self.api)
        self.addCleanup(handle.stop)
        handle.start()

    def api(self, path, payload=None, **kwargs):
        if path.endswith("/check-runs/88"):
            return self.check
        if path.endswith("/pulls/60"):
            return self.pr
        if "/files?" in path:
            return [self.file]
        if "/reviews?" in path:
            return self.reviews
        self.assertTrue(path.endswith("/reviews"))
        self.posts.append(payload)
        self.reviews.append({"user": {"id": 41898282}, "state": "COMMENTED", **payload})
        return {"id": 900}

    def publish(self):
        return comments.publish(self.repo, 60, self.sha, 88, "run/123")

    def test_model_and_comments_use_same_diff_coordinates(self):
        annotated = json.loads(deep.review_input(self.pr, [self.file], "높음"))["changes"][0]["patch"]
        self.assertIn("[L5] -old\n[R8] +new\n[R9]  same", annotated)
        self.assertIn("[R60] +last", annotated)
        self.publish()
        post, = self.posts
        self.assertEqual((post["event"], post["commit_id"]), ("COMMENT", self.sha))
        self.assertEqual([(c["side"], c["line"]) for c in post["comments"]], [("LEFT", 5), ("RIGHT", 9), ("RIGHT", 60)])
        self.assertTrue(all(c["path"] == self.file["filename"] for c in post["comments"]))
        self.assertIn("위치 확인 필요", post["body"])
        self.assertIn("    @everyone\n    # title", post["body"])
        self.assertIn("checks?check_run_id=88", post["body"])
        self.publish()
        self.assertEqual(len(self.posts), 1)

    def test_wrong_pr_sha_app_run_or_verdict_cannot_post(self):
        for edit in ({"external_id": "pr-61"}, {"head_sha": "c"*40}, {"details_url": "run/99"}, {"app": {"id": 9}},
                     {"conclusion": "success"}, {"conclusion": "action_required"}, {"status": "in_progress"}):
            with self.subTest(edit=edit), patch.dict(self.check, edit):
                self.publish()
        for edit in ({"state": "closed"}, {"head": self.pr["head"] | {"sha": "c"*40}},
                     {"base": {"ref": "main", "sha": self.base}}, {"base": {"ref": "development", "sha": "c"*40}}):
            with self.subTest(edit=edit), patch.dict(self.pr, edit):
                self.publish()
        self.assertEqual(self.posts, [])

    def test_untrusted_marker_does_not_suppress_review(self):
        self.reviews.append({"user": {"id": 7}, "commit_id": self.sha, "state": "COMMENTED",
                             "body": "<!-- sol-max-review:88 -->\nspoof"})
        self.publish()
        self.assertEqual(len(self.posts), 1)

    def test_invalid_diff_coordinates_keep_all_findings_in_summary(self):
        def reject_lines(path, payload=None, **kw):
            if payload and payload["comments"]:
                raise RuntimeError("Validation failed (HTTP 422)")
            return self.api(path, payload, **kw)
        with patch.object(comments, "api", side_effect=reject_lines):
            self.publish()
        post, = self.posts
        self.assertEqual(post["comments"], [])
        for finding in self.findings:
            self.assertIn(finding["body"].splitlines()[0], post["body"])

    def test_ambiguous_post_failure_rechecks_existing_review_on_retry(self):
        def lost_response(path, payload=None, **kw):
            result = self.api(path, payload, **kw)
            if payload:
                raise RuntimeError("응답 연결 끊김")
            return result
        with patch.object(comments, "api", side_effect=lost_response), self.assertRaises(RuntimeError):
            self.publish()
        self.publish()
        self.assertEqual(len(self.posts), 1)


if __name__ == "__main__":
    unittest.main()
