import Testing
import Foundation
import TCATBModels
@testable import TCATBProfile

@Suite("LeaderboardViewModel")
struct LeaderboardViewModelTests {

    @Test("initial state")
    func initialState() {
        let vm = LeaderboardViewModel(apiClient: MockProfileAPIClient())
        #expect(vm.selectedPeriod == .weekly)
        #expect(vm.entries.isEmpty)
        #expect(vm.isLoading == false)
        #expect(vm.error == nil)
    }

    @Test("loadLeaderboard populates entries")
    func loadsEntries() async {
        let mock = MockProfileAPIClient()
        let entries = [
            TestFixtures.leaderboardEntry(rank: 1, userId: "u1", username: "alice", score: 200),
            TestFixtures.leaderboardEntry(rank: 2, userId: "u2", username: "bob", score: 150),
        ]
        mock.getLeaderboardResult = .success(LeaderboardResponse(period: "weekly", entries: entries))
        let vm = LeaderboardViewModel(apiClient: mock)

        await vm.loadLeaderboard()

        #expect(vm.entries.count == 2)
        #expect(vm.entries[0].username == "alice")
        #expect(vm.isLoading == false)
    }

    @Test("loadLeaderboard sends selected period to API")
    func sendsSelectedPeriod() async {
        let mock = MockProfileAPIClient()
        let vm = LeaderboardViewModel(apiClient: mock)
        vm.selectedPeriod = .monthly

        await vm.loadLeaderboard()

        #expect(mock.lastGetLeaderboardPeriod == "monthly")
    }

    @Test("loadLeaderboard error sets error message")
    func errorHandling() async {
        let mock = MockProfileAPIClient()
        mock.getLeaderboardResult = .failure(TestError.test)
        let vm = LeaderboardViewModel(apiClient: mock)

        await vm.loadLeaderboard()

        #expect(vm.error == "test error")
        #expect(vm.entries.isEmpty)
        #expect(vm.isLoading == false)
    }

    @Test("successful load clears previous error")
    func clearsPreviousError() async {
        let mock = MockProfileAPIClient()
        mock.getLeaderboardResult = .failure(TestError.test)
        let vm = LeaderboardViewModel(apiClient: mock)

        await vm.loadLeaderboard()
        #expect(vm.error != nil)

        mock.getLeaderboardResult = .success(LeaderboardResponse(period: "weekly", entries: []))
        await vm.loadLeaderboard()
        #expect(vm.error == nil)
    }

    @Test("clearError removes error")
    func clearError() async {
        let mock = MockProfileAPIClient()
        mock.getLeaderboardResult = .failure(TestError.test)
        let vm = LeaderboardViewModel(apiClient: mock)
        await vm.loadLeaderboard()
        #expect(vm.error != nil)

        vm.clearError()
        #expect(vm.error == nil)
    }

    @MainActor
    @Test("periodDidChange triggers load")
    func periodDidChange() async throws {
        let mock = MockProfileAPIClient()
        let entry = TestFixtures.leaderboardEntry()
        mock.getLeaderboardResult = .success(LeaderboardResponse(period: "monthly", entries: [entry]))
        let vm = LeaderboardViewModel(apiClient: mock)

        vm.selectedPeriod = .monthly
        vm.periodDidChange()
        try await Task.sleep(nanoseconds: 100_000_000)

        #expect(mock.getLeaderboardCallCount == 1)
        #expect(mock.lastGetLeaderboardPeriod == "monthly")
        #expect(vm.entries.count == 1)
    }
}
