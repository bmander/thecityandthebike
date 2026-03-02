import SwiftUI

public struct LoginScreen: View {
    let state: AuthState
    let onLogin: (String, String) -> Void
    let onNavigateToRegister: () -> Void
    let onClearError: () -> Void

    @State private var username = ""
    @State private var password = ""
    @FocusState private var focusedField: Field?

    private enum Field {
        case username, password
    }

    public init(
        state: AuthState,
        onLogin: @escaping (String, String) -> Void,
        onNavigateToRegister: @escaping () -> Void,
        onClearError: @escaping () -> Void
    ) {
        self.state = state
        self.onLogin = onLogin
        self.onNavigateToRegister = onNavigateToRegister
        self.onClearError = onClearError
    }

    public var body: some View {
        VStack(spacing: 16) {
            Text("The City and the Bike")
                .font(.title2)
                .fontWeight(.bold)

            Text("Sign in to continue")
                .font(.subheadline)
                .foregroundStyle(.secondary)

            Spacer().frame(height: 8)

            TextField("Username", text: $username)
                .textFieldStyle(.roundedBorder)
                .textContentType(.username)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .focused($focusedField, equals: .username)
                .submitLabel(.next)
                .disabled(state.isLoading)
                .onChange(of: username) { onClearError() }
                .onSubmit { focusedField = .password }

            SecureField("Password", text: $password)
                .textFieldStyle(.roundedBorder)
                .textContentType(.password)
                .focused($focusedField, equals: .password)
                .submitLabel(.done)
                .disabled(state.isLoading)
                .onChange(of: password) { onClearError() }
                .onSubmit { submitIfValid() }

            if let error = state.error {
                Text(error)
                    .font(.caption)
                    .foregroundStyle(.red)
            }

            Button(action: { submitIfValid() }) {
                if state.isLoading {
                    ProgressView()
                        .frame(maxWidth: .infinity)
                } else {
                    Text("Sign In")
                        .frame(maxWidth: .infinity)
                }
            }
            .buttonStyle(.borderedProminent)
            .disabled(state.isLoading || username.isEmpty || password.isEmpty)

            Button("Don't have an account? Register") {
                onNavigateToRegister()
            }
            .disabled(state.isLoading)
        }
        .padding(24)
        .frame(maxWidth: 400)
    }

    private func submitIfValid() {
        guard !username.isEmpty, !password.isEmpty else { return }
        focusedField = nil
        onLogin(username, password)
    }
}
