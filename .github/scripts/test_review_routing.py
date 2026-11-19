"""실제 작업 경로로 수정·사람 리뷰·통과의 경계를 검증한다."""

from dataclasses import replace
import unittest

from review_routing import BASE, Review, independent_approval, route


class ReviewRoutingTests(unittest.TestCase):
    def setUp(self):
        self.review = Review("SCRUM-80", "new", "pass", {"nickname": "애칭 1~20자 경계 테스트"},
                             changed_flow=False, tests_weakened=False)
        self.request = dict(task="SCRUM-80", head="new", paths=[BASE + "child/dto/CreateChildRequest.java"],
                            ci="passed", review=self.review, criteria={"nickname"}, only_known_changes=True)

    def check(self, expected, **changes):
        self.assertEqual(route(**(self.request | changes)), expected)

    def test_verified_scrum_feature_passes(self):
        self.check("통과")

    def test_feature_with_relevant_new_tests_can_pass(self):
        self.check("통과", paths=[*self.request["paths"], "src/test/java/child/CreateChildRequestTests.java"])

    def test_missing_one_acceptance_criterion(self):
        self.check("팀원 판단", criteria={"nickname", "limits"})
        self.check("팀원 판단", criteria=set())

    def test_human_must_be_independent_current_and_still_approved(self):
        approval = dict(head="new", author=10, last_pusher=20, reviewer=30, reviewed_head="new",
                        state="APPROVED", human_team_member=True)
        self.assertTrue(independent_approval(**approval))
        for override in ({"reviewer": 10}, {"reviewer": 20}, {"state": "DISMISSED"},
                         {"reviewed_head": "old"}, {"human_team_member": False}, {"last_pusher": None}):
            with self.subTest(override=override):
                self.assertFalse(independent_approval(**(approval | override)))

    def test_failing_contract_requires_fix(self):
        self.check("수정 필요", ci="failed")

    def test_finding_can_block_even_if_ci_passes(self):
        self.check("수정 필요", review=replace(self.review, outcome="changes", evidence={"nickname": "20자 입력 거부"}))

    def test_protected_change_with_green_tests(self):
        self.check("팀원 판단", paths=[BASE + "auth/application/EmailVerificationService.java"])

    def test_mixed_or_moved_protected_paths(self):
        self.check("팀원 판단", paths=["README.md", ".github/workflows/ci.yml"])
        self.check("팀원 판단", paths=[BASE + "guardian/api/Pin.java", BASE + "child/dto/Pin.java"])

    def test_new_call_in_ordinary_file(self):
        self.check("팀원 판단", review=replace(self.review, changed_flow=True))

    def test_uncertain_call_analysis(self):
        self.check("팀원 판단", review=replace(self.review, changed_flow=None))

    def test_weakened_or_unassessed_assertions(self):
        for result in (True, None):
            with self.subTest(result=result):
                self.check("팀원 판단", review=replace(self.review, tests_weakened=result))

    def test_stale_or_cross_issue_result(self):
        self.check("팀원 판단", review=replace(self.review, head="old"))
        self.check("팀원 판단", review=replace(self.review, task="SCRUM-90"))

    def test_missing_evidence_or_completion(self):
        self.check("팀원 판단", review=None)
        self.check("팀원 판단", review=replace(self.review, evidence={"nickname": " "}))
        self.check("팀원 판단", review=replace(self.review, outcome="incomplete"))

    def test_unknown_files_or_incomplete_diff(self):
        self.check("팀원 판단", only_known_changes=False)
        self.check("팀원 판단", paths=[])

    def test_waits_for_ci(self):
        self.check("CI 대기", ci="pending")

    def test_changes_then_human_after_fix(self):
        protected = [BASE + "auth/application/EmailVerificationService.java"]
        self.check("수정 필요", paths=protected, review=replace(self.review, outcome="changes"))
        self.check("팀원 판단", paths=protected)


if __name__ == "__main__":
    unittest.main()
