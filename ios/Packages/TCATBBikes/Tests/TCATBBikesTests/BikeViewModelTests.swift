import Testing
import Foundation
import TCATBModels
@testable import TCATBBikes

@Suite("BikeViewModel")
struct BikeViewModelTests {

    private func makeMock(
        detail: BikeDetailResponse = TestFixtures.bikeDetail(),
        submissions: CursorPaginatedSubmissions = TestFixtures.paginatedSubmissions()
    ) -> MockBikesAPIClient {
        let mock = MockBikesAPIClient()
        mock.getBikeDetailResult = .success(detail)
        mock.getBikeSubmissionsResult = .success(submissions)
        return mock
    }

    @Test("initial state")
    func initialState() {
        let vm = BikeViewModel(bikeQrId: "bike-1", apiClient: MockBikesAPIClient())
        #expect(vm.bikeDetail == nil)
        #expect(vm.submissions.isEmpty)
        #expect(!vm.isLoading)
        #expect(!vm.isLoadingMore)
        #expect(vm.error == nil)
        #expect(vm.bikeQrId == "bike-1")
    }

    @Test("loadBike success populates detail and submissions")
    func loadBikeSuccess() async {
        let subs = [TestFixtures.submission(submissionId: "s1")]
        let mock = makeMock(
            detail: TestFixtures.bikeDetail(bikeQrId: "bike-1", submissionCount: 3),
            submissions: TestFixtures.paginatedSubmissions(items: subs)
        )
        let vm = BikeViewModel(bikeQrId: "bike-1", apiClient: mock)

        await vm.loadBike()

        #expect(vm.bikeDetail?.bikeQrId == "bike-1")
        #expect(vm.bikeDetail?.submissionCount == 3)
        #expect(vm.submissions.count == 1)
        #expect(vm.submissions[0].submissionId == "s1")
        #expect(!vm.isLoading)
        #expect(vm.error == nil)
        #expect(mock.getBikeDetailCallCount == 1)
        #expect(mock.getBikeSubmissionsCallCount == 1)
    }

    @Test("loadBike detail failure short-circuits — no submissions call")
    func loadBikeDetailFailure() async {
        let mock = MockBikesAPIClient()
        mock.getBikeDetailResult = .failure(TestError.test)
        let vm = BikeViewModel(bikeQrId: "bike-1", apiClient: mock)

        await vm.loadBike()

        #expect(vm.bikeDetail == nil)
        #expect(vm.submissions.isEmpty)
        #expect(vm.error == "test error")
        #expect(!vm.isLoading)
        #expect(mock.getBikeDetailCallCount == 1)
        #expect(mock.getBikeSubmissionsCallCount == 0)
    }

    @Test("loadBike partial failure — detail OK, submissions fail")
    func loadBikePartialFailure() async {
        let mock = MockBikesAPIClient()
        mock.getBikeDetailResult = .success(TestFixtures.bikeDetail(bikeQrId: "bike-1"))
        mock.getBikeSubmissionsResult = .failure(TestError.test)
        let vm = BikeViewModel(bikeQrId: "bike-1", apiClient: mock)

        await vm.loadBike()

        #expect(vm.bikeDetail?.bikeQrId == "bike-1")
        #expect(vm.submissions.isEmpty)
        #expect(vm.error == "test error")
        #expect(!vm.isLoading)
    }

    @Test("loadMoreSubmissions appends and passes cursor")
    func loadMoreSubmissionsAppends() async {
        let initial = [TestFixtures.submission(submissionId: "s1")]
        let more = [TestFixtures.submission(submissionId: "s2")]
        let mock = makeMock(
            submissions: TestFixtures.paginatedSubmissions(items: initial, nextCursor: "c1", hasMore: true)
        )
        let vm = BikeViewModel(bikeQrId: "bike-1", apiClient: mock)

        await vm.loadBike()
        #expect(vm.submissions.count == 1)

        mock.getBikeSubmissionsResult = .success(
            TestFixtures.paginatedSubmissions(items: more, hasMore: false)
        )
        await vm.loadMoreSubmissions()

        #expect(vm.submissions.count == 2)
        #expect(vm.submissions[1].submissionId == "s2")
        #expect(mock.lastGetBikeSubmissionsCursor == "c1")
        #expect(!vm.isLoadingMore)
    }

    @Test("loadMoreSubmissions does nothing when no more pages")
    func loadMoreSubmissionsNoMorePages() async {
        let mock = makeMock(
            submissions: TestFixtures.paginatedSubmissions(items: [TestFixtures.submission()], hasMore: false)
        )
        let vm = BikeViewModel(bikeQrId: "bike-1", apiClient: mock)

        await vm.loadBike()
        #expect(mock.getBikeSubmissionsCallCount == 1)

        await vm.loadMoreSubmissions()
        #expect(mock.getBikeSubmissionsCallCount == 1)
    }

    @Test("removeSubmission removes existing submission")
    func removeSubmissionExisting() async {
        let subs = [
            TestFixtures.submission(submissionId: "s1"),
            TestFixtures.submission(submissionId: "s2"),
        ]
        let mock = makeMock(submissions: TestFixtures.paginatedSubmissions(items: subs))
        let vm = BikeViewModel(bikeQrId: "bike-1", apiClient: mock)

        await vm.loadBike()
        #expect(vm.submissions.count == 2)

        vm.removeSubmission("s1")

        #expect(vm.submissions.count == 1)
        #expect(vm.submissions[0].submissionId == "s2")
    }

    @Test("removeSubmission no-op for nonexistent id")
    func removeSubmissionNonexistent() async {
        let subs = [TestFixtures.submission(submissionId: "s1")]
        let mock = makeMock(submissions: TestFixtures.paginatedSubmissions(items: subs))
        let vm = BikeViewModel(bikeQrId: "bike-1", apiClient: mock)

        await vm.loadBike()
        vm.removeSubmission("nonexistent")

        #expect(vm.submissions.count == 1)
    }

    @Test("clearError resets error")
    func clearError() async {
        let mock = MockBikesAPIClient()
        mock.getBikeDetailResult = .failure(TestError.test)
        let vm = BikeViewModel(bikeQrId: "bike-1", apiClient: mock)

        await vm.loadBike()
        #expect(vm.error != nil)

        vm.clearError()
        #expect(vm.error == nil)
    }
}
