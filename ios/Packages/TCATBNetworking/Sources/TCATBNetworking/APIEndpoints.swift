import Foundation
import TCATBModels

// MARK: - Auth Endpoints
extension APIClient {
    public func register(username: String, password: String, deviceId: String? = nil) async throws -> MessageResponse {
        let body = RegisterRequest(username: username, password: password, deviceId: deviceId)
        let request = try await buildRequest(method: "POST", path: "auth/register", body: body)
        return try await performRequest(request)
    }

    public func login(username: String, password: String, deviceId: String? = nil) async throws -> TokenResponse {
        let body = LoginRequest(username: username, password: password, deviceId: deviceId)
        let request = try await buildRequest(method: "POST", path: "auth/login", body: body)
        return try await performRequest(request)
    }

    public func refreshToken(refreshToken: String) async throws -> TokenResponse {
        let body = RefreshRequest(refreshToken: refreshToken)
        let request = try await buildRequest(method: "POST", path: "auth/refresh", body: body)
        return try await performRequest(request)
    }

    public func logout(refreshToken: String) async throws -> MessageResponse {
        let body = RefreshRequest(refreshToken: refreshToken)
        let request = try await buildRequest(method: "POST", path: "auth/logout", body: body)
        return try await performRequest(request)
    }
}

// MARK: - User Endpoints
extension APIClient {
    public func getCurrentUser() async throws -> UserResponse {
        let request = try await buildRequest(method: "GET", path: "users/me")
        return try await performRequest(request)
    }

    public func deleteAccount() async throws -> MessageResponse {
        let request = try await buildRequest(method: "DELETE", path: "users/me")
        return try await performRequest(request)
    }

    public func getMySubmissions(limit: Int = 20, cursor: String? = nil) async throws -> CursorPaginatedSubmissions {
        var queryItems: [URLQueryItem] = [URLQueryItem(name: "limit", value: "\(limit)")]
        if let cursor { queryItems.append(URLQueryItem(name: "cursor", value: cursor)) }
        let request = try await buildRequest(method: "GET", path: "users/me/submissions", queryItems: queryItems)
        return try await performRequest(request)
    }

    public func getUserDetail(userId: String) async throws -> UserDetailResponse {
        let request = try await buildRequest(method: "GET", path: "users/\(userId)")
        return try await performRequest(request)
    }

    public func getUserSubmissions(userId: String, limit: Int = 20, cursor: String? = nil) async throws -> CursorPaginatedSubmissions {
        var queryItems: [URLQueryItem] = [URLQueryItem(name: "limit", value: "\(limit)")]
        if let cursor { queryItems.append(URLQueryItem(name: "cursor", value: cursor)) }
        let request = try await buildRequest(method: "GET", path: "users/\(userId)/submissions", queryItems: queryItems)
        return try await performRequest(request)
    }
}

// MARK: - Submission Endpoints
extension APIClient {
    public func getSubmissions(limit: Int = 20, cursor: String? = nil) async throws -> CursorPaginatedSubmissions {
        var queryItems: [URLQueryItem] = [URLQueryItem(name: "limit", value: "\(limit)")]
        if let cursor { queryItems.append(URLQueryItem(name: "cursor", value: cursor)) }
        let request = try await buildRequest(method: "GET", path: "submissions", queryItems: queryItems)
        return try await performRequest(request)
    }

    public func createSubmission(
        imageData: Data,
        imageFileName: String,
        imageMimeType: String,
        bikeQrId: String,
        capturedDate: String,
        userCaption: String? = nil,
        side: String? = nil
    ) async throws -> SubmissionResponse {
        var multipart = MultipartFormData()
        multipart.addFile(name: "image", data: imageData, fileName: imageFileName, mimeType: imageMimeType)
        multipart.addField(name: "bike_qr_id", value: bikeQrId)
        multipart.addField(name: "captured_date", value: capturedDate)
        if let userCaption { multipart.addField(name: "user_caption", value: userCaption) }
        if let side { multipart.addField(name: "side", value: side) }

        let url = buildURL(path: "submissions")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue(multipart.contentType, forHTTPHeaderField: "Content-Type")
        request.httpBody = multipart.buildBody()
        request = await authInterceptor.intercept(request)

        return try await performRequest(request)
    }

    public func getSubmission(submissionId: String) async throws -> SubmissionResponse {
        let request = try await buildRequest(method: "GET", path: "submissions/\(submissionId)")
        return try await performRequest(request)
    }

    public func deleteSubmission(submissionId: String) async throws -> MessageResponse {
        let request = try await buildRequest(method: "DELETE", path: "submissions/\(submissionId)")
        return try await performRequest(request)
    }
}

// MARK: - Bike Endpoints
extension APIClient {
    public func getBikes(limit: Int = 20, offset: Int = 0) async throws -> PaginatedBikes {
        let queryItems: [URLQueryItem] = [
            URLQueryItem(name: "limit", value: "\(limit)"),
            URLQueryItem(name: "offset", value: "\(offset)"),
        ]
        let request = try await buildRequest(method: "GET", path: "bikes", queryItems: queryItems)
        return try await performRequest(request)
    }

    public func getBikeDetail(bikeQrId: String) async throws -> BikeDetailResponse {
        let request = try await buildRequest(method: "GET", path: "bikes/\(bikeQrId)")
        return try await performRequest(request)
    }

    public func getBikeSubmissions(bikeQrId: String, limit: Int = 20, cursor: String? = nil) async throws -> CursorPaginatedSubmissions {
        var queryItems: [URLQueryItem] = [URLQueryItem(name: "limit", value: "\(limit)")]
        if let cursor { queryItems.append(URLQueryItem(name: "cursor", value: cursor)) }
        let request = try await buildRequest(method: "GET", path: "bikes/\(bikeQrId)/submissions", queryItems: queryItems)
        return try await performRequest(request)
    }
}

// MARK: - Leaderboard Endpoints
extension APIClient {
    public func getLeaderboard(period: String = "weekly") async throws -> LeaderboardResponse {
        let queryItems = [URLQueryItem(name: "period", value: period)]
        let request = try await buildRequest(method: "GET", path: "leaderboard", queryItems: queryItems)
        return try await performRequest(request)
    }
}

// MARK: - Upload Endpoints
extension APIClient {
    public func uploadImage(imageData: Data, fileName: String, mimeType: String) async throws -> UploadResponse {
        var multipart = MultipartFormData()
        multipart.addFile(name: "image", data: imageData, fileName: fileName, mimeType: mimeType)

        let url = buildURL(path: "uploads/images")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue(multipart.contentType, forHTTPHeaderField: "Content-Type")
        request.httpBody = multipart.buildBody()
        request = await authInterceptor.intercept(request)

        return try await performRequest(request)
    }
}

// MARK: - Tag Endpoints
extension APIClient {
    public func getTags(submissionId: String) async throws -> [TagResponse] {
        let request = try await buildRequest(method: "GET", path: "submissions/\(submissionId)/tags")
        return try await performRequest(request)
    }

    public func createTag(
        submissionId: String,
        imageData: Data,
        imageFileName: String,
        imageMimeType: String,
        ring: String? = nil,
        ringWidth: String? = nil,
        ringHeight: String? = nil
    ) async throws -> TagResponse {
        var multipart = MultipartFormData()
        multipart.addFile(name: "image", data: imageData, fileName: imageFileName, mimeType: imageMimeType)
        if let ring { multipart.addField(name: "ring", value: ring) }
        if let ringWidth { multipart.addField(name: "ring_width", value: ringWidth) }
        if let ringHeight { multipart.addField(name: "ring_height", value: ringHeight) }

        let url = buildURL(path: "submissions/\(submissionId)/tags")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue(multipart.contentType, forHTTPHeaderField: "Content-Type")
        request.httpBody = multipart.buildBody()
        request = await authInterceptor.intercept(request)

        return try await performRequest(request)
    }

    public func deleteTag(tagId: String) async throws -> MessageResponse {
        let request = try await buildRequest(method: "DELETE", path: "tags/\(tagId)")
        return try await performRequest(request)
    }

    public func getTagDetail(tagId: String) async throws -> TagDetailResponse {
        let request = try await buildRequest(method: "GET", path: "tags/\(tagId)")
        return try await performRequest(request)
    }

    public func getTagSubmissions(tagId: String, limit: Int = 20, cursor: String? = nil) async throws -> CursorPaginatedSubmissions {
        var queryItems: [URLQueryItem] = [URLQueryItem(name: "limit", value: "\(limit)")]
        if let cursor { queryItems.append(URLQueryItem(name: "cursor", value: cursor)) }
        let request = try await buildRequest(method: "GET", path: "tags/\(tagId)/submissions", queryItems: queryItems)
        return try await performRequest(request)
    }
}

// MARK: - Ban Endpoints
extension APIClient {
    public func banUser(userId: String, reason: String? = nil) async throws -> BanResponse {
        var body: [String: String?] = [:]
        if let reason { body["reason"] = reason }
        let request = try await buildRequest(method: "POST", path: "users/\(userId)/ban", body: body)
        return try await performRequest(request)
    }

    public func unbanUser(userId: String) async throws -> BanResponse {
        let request = try await buildRequest(method: "DELETE", path: "users/\(userId)/ban")
        return try await performRequest(request)
    }
}

// MARK: - Flag Endpoints
extension APIClient {
    public func createFlag(submissionId: String) async throws -> FlagStatusResponse {
        let request = try await buildRequest(method: "POST", path: "submissions/\(submissionId)/flags")
        return try await performRequest(request)
    }

    public func getFlagStatus(submissionId: String) async throws -> FlagStatusResponse {
        let request = try await buildRequest(method: "GET", path: "submissions/\(submissionId)/flags/me")
        return try await performRequest(request)
    }

    public func clearFlags(submissionId: String) async throws -> FlagStatusResponse {
        let request = try await buildRequest(method: "DELETE", path: "submissions/\(submissionId)/flags")
        return try await performRequest(request)
    }
}
