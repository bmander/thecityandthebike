import Foundation
import Observation
import TCATBNetworking
import TCATBFeed

@MainActor
@Observable
final class AppDependencies {
    let apiClient: APIClient
    let feedViewModel: MainViewModel

    init() {
        let client = APIClient(environment: .staging, tokenProvider: StubTokenProvider())
        self.apiClient = client
        self.feedViewModel = MainViewModel(apiClient: client)
    }
}
