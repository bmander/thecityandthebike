import Testing
import Foundation
import TCATBModels
@testable import TCATBProfile

@Suite("UserViewModel")
struct UserViewModelTests {

    private func makeMock(
        detail: UserDetailResponse = TestFixtures.userDetail(),
        submissions: CursorPaginatedSubmissions = TestFixtures.paginatedSubmissions()
    ) -> MockProfileAPIClient {
        let mock = MockProfileAPIClient()
        mock.getUserDetailResult = .success(detail)
        mock.getUserSubmissionsResult = .success(submissions)
        return mock
    }

    // MARK: - Initial state

    @Test("initial state")
    func initialState() {
        let vm = UserViewModel(userId: "user-1", apiClient: MockProfileAPIClient())
        #expect(vm.userDetail == nil)
        #expect(vm.submissions.isEmpty)
        #expect(vm.isLoading == false)
        #expect(vm.isLoadingMore == false)
        #expect(vm.error == nil)
        #expect(vm.hasMorePages == true)
        #expect(vm.isBanning == false)
        #expect(vm.currentUserIsAdmin == false)
    }

    // MARK: - loadUser

    @Test("loadUser success populates detail and submissions")
    func loadUserSuccess() async {
        let subs = [TestFixtures.submission(submissionId: "s1")]
        let mock = makeMock(
            detail: TestFixtures.userDetail(username: "alice"),
            submissions: TestFixtures.paginatedSubmissions(items: subs, hasMore: false)
        )
        let vm = UserViewModel(userId: "user-1", apiClient: mock)

        await vm.loadUser()

        #expect(vm.userDetail?.username == "alice")
        #expect(vm.submissions.count == 1)
        #expect(vm.hasMorePages == false)
        #expect(vm.isLoading == false)
        #expect(vm.error == nil)
    }

    @Test("detail failure short-circuits without calling submissions API")
    func detailFailureShortCircuits() async {
        let mock = MockProfileAPIClient()
        mock.getUserDetailResult = .failure(TestError.test)
        let vm = UserViewModel(userId: "user-1", apiClient: mock)

        await vm.loadUser()

        #expect(vm.error == "test error")
        #expect(vm.userDetail == nil)
        #expect(mock.getUserSubmissionsCallCount == 0)
        #expect(vm.isLoading == false)
    }

    @Test("partial failure: detail OK, submissions fail")
    func partialFailure() async {
        let mock = MockProfileAPIClient()
        mock.getUserDetailResult = .success(TestFixtures.userDetail(username: "alice"))
        mock.getUserSubmissionsResult = .failure(TestError.test)
        let vm = UserViewModel(userId: "user-1", apiClient: mock)

        await vm.loadUser()

        #expect(vm.userDetail?.username == "alice")
        #expect(vm.submissions.isEmpty)
        #expect(vm.error == "test error")
        #expect(vm.isLoading == false)
    }

    // MARK: - loadMore

    @Test("loadMore appends submissions")
    func loadMoreAppends() async {
        let page1 = [TestFixtures.submission(submissionId: "s1")]
        let mock = makeMock(
            submissions: TestFixtures.paginatedSubmissions(items: page1, nextCursor: "c1", hasMore: true)
        )
        let vm = UserViewModel(userId: "user-1", apiClient: mock)

        await vm.loadUser()
        #expect(vm.submissions.count == 1)

        let page2 = [TestFixtures.submission(submissionId: "s2")]
        mock.getUserSubmissionsResult = .success(
            TestFixtures.paginatedSubmissions(items: page2, hasMore: false)
        )

        await vm.loadMoreSubmissions()

        #expect(vm.submissions.count == 2)
        #expect(vm.submissions[0].submissionId == "s1")
        #expect(vm.submissions[1].submissionId == "s2")
        #expect(vm.hasMorePages == false)
    }

    @Test("loadMore guards when no more pages")
    func loadMoreGuards() async {
        let mock = makeMock(
            submissions: TestFixtures.paginatedSubmissions(
                items: [TestFixtures.submission()], hasMore: false
            )
        )
        let vm = UserViewModel(userId: "user-1", apiClient: mock)

        await vm.loadUser()
        let callsBefore = mock.getUserSubmissionsCallCount

        await vm.loadMoreSubmissions()

        #expect(mock.getUserSubmissionsCallCount == callsBefore)
    }

    @Test("loadMore passes correct cursor")
    func loadMoreCursor() async {
        let mock = makeMock(
            submissions: TestFixtures.paginatedSubmissions(
                items: [TestFixtures.submission()], nextCursor: "abc123", hasMore: true
            )
        )
        let vm = UserViewModel(userId: "user-1", apiClient: mock)

        await vm.loadUser()

        mock.getUserSubmissionsResult = .success(
            TestFixtures.paginatedSubmissions(items: [], hasMore: false)
        )
        await vm.loadMoreSubmissions()

        #expect(mock.lastGetUserSubmissionsCursor == "abc123")
    }

    @Test("loadMore preserves existing submissions on error")
    func loadMoreError() async {
        let page1 = [TestFixtures.submission(submissionId: "s1")]
        let mock = makeMock(
            submissions: TestFixtures.paginatedSubmissions(items: page1, nextCursor: "c1", hasMore: true)
        )
        let vm = UserViewModel(userId: "user-1", apiClient: mock)

        await vm.loadUser()
        #expect(vm.submissions.count == 1)

        mock.getUserSubmissionsResult = .failure(TestError.test)
        await vm.loadMoreSubmissions()

        #expect(vm.submissions.count == 1)
        #expect(vm.error == "test error")
        #expect(vm.isLoadingMore == false)
    }

    // MARK: - Ban / Unban

    @Test("banUser sets isBanned to true")
    func banUser() async {
        let mock = makeMock(detail: TestFixtures.userDetail(isBanned: false))
        let vm = UserViewModel(userId: "user-1", apiClient: mock)

        await vm.loadUser()
        #expect(vm.userDetail?.isBanned == false)

        await vm.banUser()

        #expect(vm.userDetail?.isBanned == true)
        #expect(vm.isBanning == false)
    }

    @Test("unbanUser sets isBanned to false")
    func unbanUser() async {
        let mock = makeMock(detail: TestFixtures.userDetail(isBanned: true))
        let vm = UserViewModel(userId: "user-1", apiClient: mock)

        await vm.loadUser()
        #expect(vm.userDetail?.isBanned == true)

        await vm.unbanUser()

        #expect(vm.userDetail?.isBanned == false)
        #expect(vm.isBanning == false)
    }

    @Test("banUser error sets error message")
    func banUserError() async {
        let mock = makeMock()
        let vm = UserViewModel(userId: "user-1", apiClient: mock)
        await vm.loadUser()

        mock.banUserResult = .failure(TestError.test)
        await vm.banUser()

        #expect(vm.error == "test error")
        #expect(vm.isBanning == false)
    }

    @Test("unbanUser error keeps isBanned unchanged")
    func unbanUserError() async {
        let mock = makeMock(detail: TestFixtures.userDetail(isBanned: true))
        let vm = UserViewModel(userId: "user-1", apiClient: mock)
        await vm.loadUser()
        #expect(vm.userDetail?.isBanned == true)

        mock.unbanUserResult = .failure(TestError.test)
        await vm.unbanUser()

        #expect(vm.userDetail?.isBanned == true)
        #expect(vm.error == "test error")
    }

    // MARK: - removeSubmission

    @Test("removeSubmission removes existing submission")
    func removeSubmissionExisting() async {
        let subs = [
            TestFixtures.submission(submissionId: "s1"),
            TestFixtures.submission(submissionId: "s2"),
        ]
        let mock = makeMock(submissions: TestFixtures.paginatedSubmissions(items: subs))
        let vm = UserViewModel(userId: "user-1", apiClient: mock)
        await vm.loadUser()

        vm.removeSubmission(submissionId: "s1")

        #expect(vm.submissions.count == 1)
        #expect(vm.submissions[0].submissionId == "s2")
    }

    @Test("removeSubmission with nonexistent ID is a no-op")
    func removeSubmissionNonexistent() async {
        let mock = makeMock(
            submissions: TestFixtures.paginatedSubmissions(items: [TestFixtures.submission(submissionId: "s1")])
        )
        let vm = UserViewModel(userId: "user-1", apiClient: mock)
        await vm.loadUser()

        vm.removeSubmission(submissionId: "nonexistent")

        #expect(vm.submissions.count == 1)
    }

    // MARK: - clearError & currentUserIsAdmin

    @Test("clearError removes error")
    func clearError() async {
        let mock = MockProfileAPIClient()
        mock.getUserDetailResult = .failure(TestError.test)
        let vm = UserViewModel(userId: "user-1", apiClient: mock)
        await vm.loadUser()
        #expect(vm.error != nil)

        vm.clearError()
        #expect(vm.error == nil)
    }

    @Test("currentUserIsAdmin reflects init parameter")
    func currentUserIsAdminTrue() {
        let vm = UserViewModel(userId: "user-1", apiClient: MockProfileAPIClient(), currentUserIsAdmin: true)
        #expect(vm.currentUserIsAdmin == true)
    }

    @Test("currentUserIsAdmin defaults to false")
    func currentUserIsAdminDefault() {
        let vm = UserViewModel(userId: "user-1", apiClient: MockProfileAPIClient())
        #expect(vm.currentUserIsAdmin == false)
    }
}
