import Testing
import Foundation
@testable import TCATBProfile

@Suite("DeleteAccountViewModel")
struct DeleteAccountViewModelTests {

    @Test("initial state")
    func initialState() {
        let vm = DeleteAccountViewModel(apiClient: MockProfileAPIClient())
        #expect(vm.isDeleting == false)
        #expect(vm.isDeleted == false)
        #expect(vm.error == nil)
    }

    @Test("successful deletion sets isDeleted")
    func successfulDeletion() async {
        let vm = DeleteAccountViewModel(apiClient: MockProfileAPIClient())
        await vm.deleteAccount()
        #expect(vm.isDeleted == true)
        #expect(vm.isDeleting == false)
        #expect(vm.error == nil)
    }

    @Test("deleteAccount calls API once")
    func callsAPI() async {
        let mock = MockProfileAPIClient()
        let vm = DeleteAccountViewModel(apiClient: mock)
        await vm.deleteAccount()
        #expect(mock.deleteAccountCallCount == 1)
    }

    @Test("error sets error message")
    func errorHandling() async {
        let mock = MockProfileAPIClient()
        mock.deleteAccountResult = .failure(TestError.test)
        let vm = DeleteAccountViewModel(apiClient: mock)
        await vm.deleteAccount()
        #expect(vm.error == "test error")
        #expect(vm.isDeleted == false)
        #expect(vm.isDeleting == false)
    }

    @Test("clearError removes error")
    func clearError() async {
        let mock = MockProfileAPIClient()
        mock.deleteAccountResult = .failure(TestError.test)
        let vm = DeleteAccountViewModel(apiClient: mock)
        await vm.deleteAccount()
        #expect(vm.error != nil)

        vm.clearError()
        #expect(vm.error == nil)
    }
}
