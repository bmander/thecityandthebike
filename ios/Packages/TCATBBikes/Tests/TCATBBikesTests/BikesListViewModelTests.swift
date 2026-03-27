import Testing
import Foundation
import TCATBModels
@testable import TCATBBikes

@Suite("BikesListViewModel")
struct BikesListViewModelTests {

    @Test("initial state")
    func initialState() {
        let vm = BikesListViewModel(apiClient: MockBikesAPIClient())
        #expect(vm.bikes.isEmpty)
        #expect(!vm.isLoading)
        #expect(!vm.isLoadingMore)
        #expect(vm.error == nil)
        #expect(vm.searchText == "")
    }

    @Test("loadBikes success populates bikes")
    func loadBikesSuccess() async {
        let items = [
            TestFixtures.bikeListItem(bikeQrId: "b1"),
            TestFixtures.bikeListItem(bikeQrId: "b2"),
        ]
        let mock = MockBikesAPIClient()
        mock.getBikesResult = .success(TestFixtures.paginatedBikes(items: items))
        let vm = BikesListViewModel(apiClient: mock)

        await vm.loadBikes()

        #expect(vm.bikes.count == 2)
        #expect(vm.bikes[0].bikeQrId == "b1")
        #expect(vm.bikes[1].bikeQrId == "b2")
        #expect(!vm.isLoading)
        #expect(vm.error == nil)
        #expect(mock.getBikesCallCount == 1)
        #expect(mock.lastGetBikesOffset == 0)
    }

    @Test("loadBikes failure sets error")
    func loadBikesError() async {
        let mock = MockBikesAPIClient()
        mock.getBikesResult = .failure(TestError.test)
        let vm = BikesListViewModel(apiClient: mock)

        await vm.loadBikes()

        #expect(vm.bikes.isEmpty)
        #expect(vm.error == "test error")
        #expect(!vm.isLoading)
    }

    @Test("loadMoreBikes appends to existing bikes")
    func loadMoreBikesAppends() async {
        let page1 = [TestFixtures.bikeListItem(bikeQrId: "b1")]
        let page2 = [TestFixtures.bikeListItem(bikeQrId: "b2")]
        let mock = MockBikesAPIClient()
        mock.getBikesResult = .success(TestFixtures.paginatedBikes(items: page1, total: 2))
        let vm = BikesListViewModel(apiClient: mock)

        await vm.loadBikes()
        #expect(vm.bikes.count == 1)

        mock.getBikesResult = .success(TestFixtures.paginatedBikes(items: page2, total: 2, offset: 1))
        await vm.loadMoreBikes()

        #expect(vm.bikes.count == 2)
        #expect(vm.bikes[1].bikeQrId == "b2")
        #expect(!vm.isLoadingMore)
    }

    @Test("loadMoreBikes does nothing when no more pages")
    func loadMoreBikesNoMorePages() async {
        let items = [TestFixtures.bikeListItem(bikeQrId: "b1")]
        let mock = MockBikesAPIClient()
        // total == items.count so hasMorePages becomes false
        mock.getBikesResult = .success(TestFixtures.paginatedBikes(items: items, total: 1))
        let vm = BikesListViewModel(apiClient: mock)

        await vm.loadBikes()
        #expect(mock.getBikesCallCount == 1)

        await vm.loadMoreBikes()
        // Should not have made another call
        #expect(mock.getBikesCallCount == 1)
    }

    @Test("loadMoreBikes error preserves existing bikes")
    func loadMoreBikesErrorPreservesExisting() async {
        let items = [TestFixtures.bikeListItem(bikeQrId: "b1")]
        let mock = MockBikesAPIClient()
        mock.getBikesResult = .success(TestFixtures.paginatedBikes(items: items, total: 2))
        let vm = BikesListViewModel(apiClient: mock)

        await vm.loadBikes()
        #expect(vm.bikes.count == 1)

        mock.getBikesResult = .failure(TestError.test)
        await vm.loadMoreBikes()

        #expect(vm.bikes.count == 1)
        #expect(vm.bikes[0].bikeQrId == "b1")
        #expect(vm.error == "test error")
    }

    @Test("loadBikes passes search text")
    func loadBikesWithSearch() async {
        let mock = MockBikesAPIClient()
        mock.getBikesResult = .success(TestFixtures.paginatedBikes())
        let vm = BikesListViewModel(apiClient: mock)
        vm.searchText = "citibike"

        await vm.loadBikes()

        #expect(mock.lastGetBikesSearch == "citibike")
    }

    @Test("loadBikes passes nil search when empty")
    func loadBikesEmptySearch() async {
        let mock = MockBikesAPIClient()
        mock.getBikesResult = .success(TestFixtures.paginatedBikes())
        let vm = BikesListViewModel(apiClient: mock)

        await vm.loadBikes()

        #expect(mock.lastGetBikesSearch == nil)
    }

    @Test("clearError resets error")
    func clearError() async {
        let mock = MockBikesAPIClient()
        mock.getBikesResult = .failure(TestError.test)
        let vm = BikesListViewModel(apiClient: mock)

        await vm.loadBikes()
        #expect(vm.error != nil)

        vm.clearError()
        #expect(vm.error == nil)
    }

    @Test("loadBikes resets state on reload")
    func loadBikesResetsOnReload() async {
        let items = [TestFixtures.bikeListItem(bikeQrId: "b1")]
        let mock = MockBikesAPIClient()
        mock.getBikesResult = .success(TestFixtures.paginatedBikes(items: items, total: 2))
        let vm = BikesListViewModel(apiClient: mock)

        await vm.loadBikes()
        #expect(vm.bikes.count == 1)

        let newItems = [TestFixtures.bikeListItem(bikeQrId: "b3")]
        mock.getBikesResult = .success(TestFixtures.paginatedBikes(items: newItems, total: 1))
        await vm.loadBikes()

        #expect(vm.bikes.count == 1)
        #expect(vm.bikes[0].bikeQrId == "b3")
    }
}
