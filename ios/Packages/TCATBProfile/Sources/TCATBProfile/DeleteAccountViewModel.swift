import Foundation
import Observation

@Observable
public final class DeleteAccountViewModel {
    public private(set) var isDeleting = false
    public private(set) var isDeleted = false
    public private(set) var error: String?

    private let apiClient: ProfileAPIClient

    public init(apiClient: ProfileAPIClient) {
        self.apiClient = apiClient
    }

    public func deleteAccount() async {
        isDeleting = true
        error = nil

        do {
            try await apiClient.deleteAccount()
            isDeleted = true
        } catch {
            self.error = error.localizedDescription
        }

        isDeleting = false
    }

    public func clearError() {
        error = nil
    }
}
