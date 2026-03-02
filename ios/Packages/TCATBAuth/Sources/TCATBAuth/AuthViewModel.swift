import Foundation
import SwiftUI

public struct AuthState: Equatable {
    public var isLoading: Bool
    public var isLoggedIn: Bool
    public var error: String?
    public var registrationSuccess: Bool

    public init(
        isLoading: Bool = false,
        isLoggedIn: Bool = false,
        error: String? = nil,
        registrationSuccess: Bool = false
    ) {
        self.isLoading = isLoading
        self.isLoggedIn = isLoggedIn
        self.error = error
        self.registrationSuccess = registrationSuccess
    }
}

@Observable
public final class AuthViewModel {
    public internal(set) var state: AuthState

    private let authRepository: AuthRepository
    private let deviceId: String?

    public init(authRepository: AuthRepository, deviceId: String? = nil) {
        self.authRepository = authRepository
        self.deviceId = deviceId
        self.state = AuthState(isLoggedIn: authRepository.isLoggedIn)
    }

    @MainActor
    public func login(username: String, password: String) {
        state.isLoading = true
        state.error = nil
        Task {
            do {
                try await authRepository.login(
                    username: username,
                    password: password,
                    deviceId: deviceId
                )
                state.isLoading = false
                state.isLoggedIn = true
            } catch {
                state.isLoading = false
                state.error = displayMessage(for: error)
            }
        }
    }

    @MainActor
    public func register(username: String, password: String) {
        state.isLoading = true
        state.error = nil
        state.registrationSuccess = false
        Task {
            do {
                try await authRepository.register(
                    username: username,
                    password: password,
                    deviceId: deviceId
                )
                state.isLoading = false
                state.registrationSuccess = true
            } catch {
                state.isLoading = false
                state.error = displayMessage(for: error)
            }
        }
    }

    @MainActor
    public func logout() {
        Task {
            await authRepository.logout()
            state = AuthState(isLoggedIn: false)
        }
    }

    public func clearError() {
        state.error = nil
    }

    public func clearRegistrationSuccess() {
        state.registrationSuccess = false
    }

    private func displayMessage(for error: Error) -> String {
        if let authError = error as? AuthError {
            return authError.displayMessage
        }
        return "An unexpected error occurred."
    }
}
