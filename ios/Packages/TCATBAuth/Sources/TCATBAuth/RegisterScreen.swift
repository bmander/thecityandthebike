import SwiftUI

public struct RegisterScreen: View {
    let state: AuthState
    let onRegister: (String, String) -> Void
    let onNavigateBack: () -> Void
    let onClearError: () -> Void
    let onClearRegistrationSuccess: () -> Void

    @State private var username = ""
    @State private var password = ""
    @State private var confirmPassword = ""
    @State private var readUnderstood = false
    @State private var agreedToLicense = false
    @FocusState private var focusedField: Field?

    private enum Field {
        case username, password, confirmPassword
    }

    public init(
        state: AuthState,
        onRegister: @escaping (String, String) -> Void,
        onNavigateBack: @escaping () -> Void,
        onClearError: @escaping () -> Void,
        onClearRegistrationSuccess: @escaping () -> Void
    ) {
        self.state = state
        self.onRegister = onRegister
        self.onNavigateBack = onNavigateBack
        self.onClearError = onClearError
        self.onClearRegistrationSuccess = onClearRegistrationSuccess
    }

    private var passwordsMatch: Bool {
        confirmPassword.isEmpty || password == confirmPassword
    }

    private var isFormValid: Bool {
        !username.isEmpty &&
        !password.isEmpty &&
        password.count >= 8 &&
        password == confirmPassword &&
        readUnderstood &&
        agreedToLicense
    }

    public var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                // Form section
                VStack(spacing: 16) {
                    Text("Join the community")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)

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
                        .textContentType(.newPassword)
                        .focused($focusedField, equals: .password)
                        .submitLabel(.next)
                        .disabled(state.isLoading)
                        .onChange(of: password) { onClearError() }
                        .onSubmit { focusedField = .confirmPassword }

                    VStack(alignment: .leading, spacing: 4) {
                        SecureField("Confirm Password", text: $confirmPassword)
                            .textFieldStyle(.roundedBorder)
                            .textContentType(.newPassword)
                            .focused($focusedField, equals: .confirmPassword)
                            .submitLabel(.done)
                            .disabled(state.isLoading)
                            .onChange(of: confirmPassword) { onClearError() }
                            .onSubmit { submitIfValid() }

                        if !passwordsMatch {
                            Text("Passwords don't match")
                                .font(.caption)
                                .foregroundStyle(.red)
                        }
                    }
                }
                .padding(24)

                Spacer().frame(height: 8)

                // Privacy & Copyright
                VStack(alignment: .leading, spacing: 16) {
                    Text("Privacy & Copyright")
                        .font(.title2)
                        .fontWeight(.bold)
                        .frame(maxWidth: .infinity, alignment: .center)

                    Text("Privacy")
                        .font(.subheadline)
                        .fontWeight(.semibold)
                    // swiftlint:disable:next line_length
                    Text("This app collects as little information about you as possible. We don't ask for your name or email. We don't track your location. When you upload a photo, we remove hidden details like the time it was taken. We only store the date you uploaded it. We store an app-scoped device identifier to enforce bans and protect the service from abuse. It is not linked to any hardware serial number and cannot track you across other apps. That said, someone could still figure out who you are based on where the bikes are and what's visible in your photos. Keep that in mind.")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)

                    Text("Copyright")
                        .font(.subheadline)
                        .fontWeight(.semibold)
                    // swiftlint:disable:next line_length
                    Text("The copyright of all photos you upload belong to you. By uploading the file you agree to license the image under Creative Commons BY-NC 4.0. This license requires that reusers give credit to the creator. It allows reusers to distribute, remix, adapt, and build upon the material in any medium or format, for noncommercial purposes only.")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)

                    Link(
                        "Learn more about CC BY-NC 4.0",
                        destination: URL(string: "https://creativecommons.org/licenses/by-nc/4.0/")!
                    )
                    .font(.subheadline)
                    .accessibilityLabel("Learn more about CC BY-NC 4.0, opens in browser")

                    Toggle(isOn: $readUnderstood) {
                        Text("I've read this and I understand")
                            .font(.subheadline)
                    }
                    .tint(.accentColor)

                    Toggle(isOn: $agreedToLicense) {
                        Text("I agree to license my photos under CC BY-NC 4.0")
                            .font(.subheadline)
                    }
                    .tint(.accentColor)
                }
                .padding(.horizontal, 24)

                Spacer().frame(height: 8)

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
                        Text("Create Account")
                            .frame(maxWidth: .infinity)
                    }
                }
                .buttonStyle(.borderedProminent)
                .disabled(state.isLoading || !isFormValid)
                .padding(.horizontal, 24)

                Spacer().frame(height: 16)
            }
            .frame(maxWidth: 400)
        }
        .navigationTitle("Create Account")
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button(action: onNavigateBack) {
                    Image(systemName: "chevron.left")
                }
            }
        }
        .onChange(of: state.registrationSuccess) {
            if state.registrationSuccess {
                onClearRegistrationSuccess()
                onNavigateBack()
            }
        }
    }

    private func submitIfValid() {
        guard isFormValid else { return }
        focusedField = nil
        onRegister(username, password)
    }
}
