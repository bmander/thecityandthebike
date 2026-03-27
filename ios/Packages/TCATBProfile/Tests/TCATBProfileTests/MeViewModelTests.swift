import Testing
import Foundation
import TCATBModels
@testable import TCATBProfile

@Suite("MeViewModel")
struct MeViewModelTests {

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
        let vm = MeViewModel(userId: "user-1", apiClient: MockProfileAPIClient())
        #expect(vm.userDetail == nil)
        #expect(vm.submissions.isEmpty)
        #expect(vm.isLoading == false)
        #expect(vm.isLoadingMore == false)
        #expect(vm.error == nil)
        #expect(vm.hasMorePages == true)
    }

    // MARK: - loadUser

    @Test("loadUser success populates detail and submissions")
    func loadUserSuccess() async {
        let subs = [TestFixtures.submission(submissionId: "s1")]
        let mock = makeMock(
            detail: TestFixtures.userDetail(username: "alice"),
            submissions: TestFixtures.paginatedSubmissions(items: subs, hasMore: false)
        )
        let vm = MeViewModel(userId: "user-1", apiClient: mock)

        await vm.loadUser()

        #expect(vm.userDetail?.username == "alice")
        #expect(vm.submissions.count == 1)
        #expect(vm.hasMorePages == false)
        #expect(vm.isLoading == false)
        #expect(vm.error == nil)
    }

    @Test("loadUser no-ops when userId is nil")
    func loadUserNilUserId() async {
        let mock = MockProfileAPIClient()
        let vm = MeViewModel(userId: nil, apiClient: mock)

        await vm.loadUser()

        #expect(mock.getUserDetailCallCount == 0)
        #expect(mock.getUserSubmissionsCallCount == 0)
        #expect(vm.isLoading == false)
    }

    @Test("detail failure short-circuits without calling submissions API")
    func detailFailureShortCircuits() async {
        let mock = MockProfileAPIClient()
        mock.getUserDetailResult = .failure(TestError.test)
        let vm = MeViewModel(userId: "user-1", apiClient: mock)

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
        let vm = MeViewModel(userId: "user-1", apiClient: mock)

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
        let vm = MeViewModel(userId: "user-1", apiClient: mock)

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
    }

    @Test("loadMore guards when no more pages")
    func loadMoreGuards() async {
        let mock = makeMock(
            submissions: TestFixtures.paginatedSubmissions(
                items: [TestFixtures.submission()], hasMore: false
            )
        )
        let vm = MeViewModel(userId: "user-1", apiClient: mock)

        await vm.loadUser()
        let callsBefore = mock.getUserSubmissionsCallCount

        await vm.loadMoreSubmissions()

        #expect(mock.getUserSubmissionsCallCount == callsBefore)
    }

    @Test("loadMore no-ops when userId is nil")
    func loadMoreNilUserId() async {
        let mock = MockProfileAPIClient()
        let vm = MeViewModel(userId: nil, apiClient: mock)

        await vm.loadMoreSubmissions()

        #expect(mock.getUserSubmissionsCallCount == 0)
    }

    // MARK: - removeSubmission & clearError

    @Test("removeSubmission removes existing submission")
    func removeSubmission() async {
        let subs = [
            TestFixtures.submission(submissionId: "s1"),
            TestFixtures.submission(submissionId: "s2"),
        ]
        let mock = makeMock(submissions: TestFixtures.paginatedSubmissions(items: subs))
        let vm = MeViewModel(userId: "user-1", apiClient: mock)
        await vm.loadUser()

        vm.removeSubmission(submissionId: "s1")

        #expect(vm.submissions.count == 1)
        #expect(vm.submissions[0].submissionId == "s2")
    }

    @Test("clearError removes error")
    func clearError() async {
        let mock = MockProfileAPIClient()
        mock.getUserDetailResult = .failure(TestError.test)
        let vm = MeViewModel(userId: "user-1", apiClient: mock)
        await vm.loadUser()
        #expect(vm.error != nil)

        vm.clearError()
        #expect(vm.error == nil)
    }
}
