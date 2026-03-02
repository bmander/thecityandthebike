import Foundation
import Observation
import TCATBBikes
import TCATBNetworking
import TCATBFeed

@MainActor
@Observable
final class AppDependencies {
    let apiClient: APIClient
    let feedViewModel: MainViewModel
    let bikesListViewModel: BikesListViewModel
    let imageBaseURL: String

    init() {
        let client = APIClient(environment: .staging, tokenProvider: StubTokenProvider())
        self.apiClient = client
        self.feedViewModel = MainViewModel(apiClient: client)
        self.bikesListViewModel = BikesListViewModel(apiClient: client)
        self.imageBaseURL = client.baseURL.absoluteString
    }
}
