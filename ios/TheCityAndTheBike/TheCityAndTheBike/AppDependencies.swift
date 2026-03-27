import Foundation
import Observation
import TCATBAuth
import TCATBBikes
import TCATBNetworking
import TCATBFeed
import TCATBProfile

@MainActor
@Observable
final class AppDependencies {
    let apiClient: APIClient
    let feedViewModel: MainViewModel
    let bikesListViewModel: BikesListViewModel
    let leaderboardViewModel: LeaderboardViewModel
    let imageBaseURL: String
    let tokenManager: TokenManager
    let authViewModel: AuthViewModel

    init() {
        let tokenManager = TokenManager()
        self.tokenManager = tokenManager
        let client = APIClient(environment: .staging, tokenProvider: TokenManagerTokenProvider(tokenManager: tokenManager))
        self.apiClient = client
        let authRepo = AuthRepository(apiClient: client, tokenManager: tokenManager)
        self.authViewModel = AuthViewModel(authRepository: authRepo)
        self.feedViewModel = MainViewModel(apiClient: client)
        self.bikesListViewModel = BikesListViewModel(apiClient: client)
        self.leaderboardViewModel = LeaderboardViewModel(apiClient: client)
        self.imageBaseURL = client.baseURL.absoluteString
    }
}
