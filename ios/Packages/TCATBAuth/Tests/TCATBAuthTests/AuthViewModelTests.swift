import Testing
import Foundation
import TCATBModels
@testable import TCATBAuth

final class MockAuthAPIClient: AuthAPIClient, @unchecked Sendable {
    var loginResult: Result<TokenResponse, Error> = .success(
        TokenResponse(accessToken: "access", refreshToken: "refresh")
    )
    var registerResult: Result<MessageResponse, Error> = .success(
        MessageResponse(msg: "User created")
    )
    var refreshResult: Result<TokenResponse, Error> = .success(
        TokenResponse(accessToken: "new-access", refreshToken: "new-refresh")
    )
    var logoutResult: Result<MessageResponse, Error> = .success(
        MessageResponse(msg: "Logged out")
    )

    var loginCallCount = 0
    var registerCallCount = 0
    var logoutCallCount = 0

    func login(_ request: LoginRequest) async throws -> TokenResponse {
        loginCallCount += 1
        return try loginResult.get()
    }

    func register(_ request: RegisterRequest) async throws -> MessageResponse {
        registerCallCount += 1
        return try registerResult.get()
    }

    func refreshToken(_ request: RefreshRequest) async throws -> TokenResponse {
        return try refreshResult.get()
    }

    func logout(_ request: RefreshRequest) async throws -> MessageResponse {
        logoutCallCount += 1
        return try logoutResult.get()
    }
}

@Suite("AuthViewModel")
struct AuthViewModelTests {

    private func makeViewModel(
        apiClient: MockAuthAPIClient = MockAuthAPIClient()
    ) -> (AuthViewModel, MockAuthAPIClient, MockKeychainStore) {
        let keychain = MockKeychainStore()
        let tokenManager = TokenManager(keychain: keychain)
        let repo = AuthRepository(apiClient: apiClient, tokenManager: tokenManager)
        let vm = AuthViewModel(authRepository: repo)
        return (vm, apiClient, keychain)
    }

    @Test("initial state is not logged in")
    func initialState() {
        let (vm, _, _) = makeViewModel()
        #expect(vm.state.isLoggedIn == false)
        #expect(vm.state.isLoading == false)
        #expect(vm.state.error == nil)
        #expect(vm.state.registrationSuccess == false)
    }

    @Test("initial state reflects existing token")
    func initialStateWithToken() {
        let keychain = MockKeychainStore()
        let tokenManager = TokenManager(keychain: keychain)
        tokenManager.saveTokens(accessToken: "existing", refreshToken: "token")
        let apiClient = MockAuthAPIClient()
        let repo = AuthRepository(apiClient: apiClient, tokenManager: tokenManager)
        let vm = AuthViewModel(authRepository: repo)

        #expect(vm.state.isLoggedIn == true)
    }

    @MainActor
    @Test("successful login updates state")
    func successfulLogin() async throws {
        let (vm, _, _) = makeViewModel()

        vm.login(username: "user", password: "pass")
        try await Task.sleep(nanoseconds: 100_000_000)

        #expect(vm.state.isLoggedIn == true)
        #expect(vm.state.isLoading == false)
        #expect(vm.state.error == nil)
    }

    @MainActor
    @Test("failed login sets error")
    func failedLogin() async throws {
        let apiClient = MockAuthAPIClient()
        apiClient.loginResult = .failure(AuthError.invalidCredentials)
        let (vm, _, _) = makeViewModel(apiClient: apiClient)

        vm.login(username: "user", password: "wrong")
        try await Task.sleep(nanoseconds: 100_000_000)

        #expect(vm.state.isLoggedIn == false)
        #expect(vm.state.isLoading == false)
        #expect(vm.state.error == "Invalid credentials")
    }

    @MainActor
    @Test("successful registration sets registrationSuccess")
    func successfulRegistration() async throws {
        let (vm, _, _) = makeViewModel()

        vm.register(username: "newuser", password: "password123")
        try await Task.sleep(nanoseconds: 100_000_000)

        #expect(vm.state.registrationSuccess == true)
        #expect(vm.state.isLoading == false)
        #expect(vm.state.error == nil)
    }

    @MainActor
    @Test("failed registration sets error")
    func failedRegistration() async throws {
        let apiClient = MockAuthAPIClient()
        apiClient.registerResult = .failure(AuthError.registrationFailed("Username taken"))
        let (vm, _, _) = makeViewModel(apiClient: apiClient)

        vm.register(username: "taken", password: "password123")
        try await Task.sleep(nanoseconds: 100_000_000)

        #expect(vm.state.registrationSuccess == false)
        #expect(vm.state.error == "Username taken")
    }

    @MainActor
    @Test("logout clears state")
    func logout() async throws {
        let (vm, _, _) = makeViewModel()

        vm.login(username: "user", password: "pass")
        try await Task.sleep(nanoseconds: 100_000_000)
        #expect(vm.state.isLoggedIn == true)

        vm.logout()
        try await Task.sleep(nanoseconds: 100_000_000)

        #expect(vm.state.isLoggedIn == false)
    }

    @Test("clearError removes error message")
    func clearError() {
        let (vm, _, _) = makeViewModel()
        vm.state = AuthState(error: "some error")

        vm.clearError()
        #expect(vm.state.error == nil)
    }

    @Test("clearRegistrationSuccess resets flag")
    func clearRegistrationSuccess() {
        let (vm, _, _) = makeViewModel()
        vm.state = AuthState(registrationSuccess: true)

        vm.clearRegistrationSuccess()
        #expect(vm.state.registrationSuccess == false)
    }
}
