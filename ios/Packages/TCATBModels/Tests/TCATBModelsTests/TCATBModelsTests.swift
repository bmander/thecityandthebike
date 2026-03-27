import XCTest
@testable import TCATBModels

final class AuthModelsTests: XCTestCase {
    func testTokenResponseDecoding() throws {
        let json = """
        {
            "access_token": "abc123",
            "refresh_token": "def456",
            "token_type": "bearer"
        }
        """.data(using: .utf8)!

        let decoded = try JSONDecoder().decode(TokenResponse.self, from: json)
        XCTAssertEqual(decoded.accessToken, "abc123")
        XCTAssertEqual(decoded.refreshToken, "def456")
        XCTAssertEqual(decoded.tokenType, "bearer")
    }

    func testLoginRequestEncoding() throws {
        let request = LoginRequest(username: "user1", password: "pass1", deviceId: "dev-123")
        let data = try JSONEncoder().encode(request)
        let dict = try JSONSerialization.jsonObject(with: data) as! [String: Any]
        XCTAssertEqual(dict["username"] as? String, "user1")
        XCTAssertEqual(dict["password"] as? String, "pass1")
        XCTAssertEqual(dict["android_id"] as? String, "dev-123")
    }

    func testRefreshRequestEncoding() throws {
        let request = RefreshRequest(refreshToken: "tok-abc")
        let data = try JSONEncoder().encode(request)
        let dict = try JSONSerialization.jsonObject(with: data) as! [String: Any]
        XCTAssertEqual(dict["refresh_token"] as? String, "tok-abc")
    }

    func testMessageResponseDecoding() throws {
        let json = """
        {"msg": "ok"}
        """.data(using: .utf8)!
        let decoded = try JSONDecoder().decode(MessageResponse.self, from: json)
        XCTAssertEqual(decoded.msg, "ok")
    }

    func testErrorResponseDecoding() throws {
        let json = """
        {"detail": {"msg": "not found"}}
        """.data(using: .utf8)!
        let decoded = try JSONDecoder().decode(ErrorResponse.self, from: json)
        XCTAssertEqual(decoded.detail?.msg, "not found")
    }
}

final class SubmissionModelsTests: XCTestCase {
    func testSubmissionResponseDecoding() throws {
        let json = """
        {
            "submission_id": "sub-1",
            "user_id": "user-1",
            "bike_qr_id": "bike-1",
            "image_url": "https://example.com/img.jpg",
            "image_url_thumbnail": "https://example.com/thumb.jpg",
            "captured_date": "2024-01-01",
            "uploaded_at": "2024-01-01T12:00:00",
            "user_caption": "Nice bike",
            "username": "rider",
            "provider": "lime",
            "side": "left",
            "points_awarded": 12,
            "points_breakdown": [
                {"event_type": "add_image", "label": "Took a photo", "points": 2}
            ],
            "flag_count": 0
        }
        """.data(using: .utf8)!

        let decoded = try JSONDecoder().decode(SubmissionResponse.self, from: json)
        XCTAssertEqual(decoded.submissionId, "sub-1")
        XCTAssertEqual(decoded.userId, "user-1")
        XCTAssertEqual(decoded.bikeQrId, "bike-1")
        XCTAssertEqual(decoded.imageUrl, "https://example.com/img.jpg")
        XCTAssertEqual(decoded.pointsAwarded, 12)
        XCTAssertEqual(decoded.pointsBreakdown?.count, 1)
        XCTAssertEqual(decoded.pointsBreakdown?.first?.eventType, "add_image")
        XCTAssertEqual(decoded.pointsBreakdown?.first?.points, 2)
        XCTAssertEqual(decoded.flagCount, 0)
    }

    func testSubmissionResponseMinimalDecoding() throws {
        let json = """
        {
            "submission_id": "sub-2",
            "user_id": "user-2",
            "bike_qr_id": "bike-2"
        }
        """.data(using: .utf8)!

        let decoded = try JSONDecoder().decode(SubmissionResponse.self, from: json)
        XCTAssertEqual(decoded.submissionId, "sub-2")
        XCTAssertNil(decoded.imageUrl)
        XCTAssertNil(decoded.pointsAwarded)
        XCTAssertNil(decoded.pointsBreakdown)
    }

    func testCursorPaginatedSubmissionsDecoding() throws {
        let json = """
        {
            "items": [],
            "next_cursor": "cursor-abc",
            "has_more": true
        }
        """.data(using: .utf8)!

        let decoded = try JSONDecoder().decode(CursorPaginatedSubmissions.self, from: json)
        XCTAssertEqual(decoded.nextCursor, "cursor-abc")
        XCTAssertTrue(decoded.hasMore)
        XCTAssertTrue(decoded.items.isEmpty)
    }
}

final class BikeModelsTests: XCTestCase {
    func testBikeListItemDecoding() throws {
        let json = """
        {
            "bike_qr_id": "BIKE-001",
            "provider": "lime",
            "submission_count": 5,
            "owner": {"name": "alice", "id": "u-1"}
        }
        """.data(using: .utf8)!

        let decoded = try JSONDecoder().decode(BikeListItem.self, from: json)
        XCTAssertEqual(decoded.bikeQrId, "BIKE-001")
        XCTAssertEqual(decoded.provider, "lime")
        XCTAssertEqual(decoded.submissionCount, 5)
        XCTAssertEqual(decoded.owner?.name, "alice")
    }

    func testBikeDetailResponseDecoding() throws {
        let json = """
        {
            "bike_qr_id": "BIKE-002",
            "provider": "bird",
            "bike_brand": "Trek",
            "first_seen_at": "2024-01-01",
            "last_seen_at": "2024-06-01",
            "notes": "scratched",
            "submission_count": 10,
            "owners": [
                {
                    "user": {"name": "bob", "id": "u-2"},
                    "submission_count": 3
                }
            ],
            "first_captured_by": {"name": "carol", "id": "u-3"},
            "last_captured_by": {"name": "dave", "id": "u-4"}
        }
        """.data(using: .utf8)!

        let decoded = try JSONDecoder().decode(BikeDetailResponse.self, from: json)
        XCTAssertEqual(decoded.bikeQrId, "BIKE-002")
        XCTAssertEqual(decoded.bikeBrand, "Trek")
        XCTAssertEqual(decoded.owners.count, 1)
        XCTAssertEqual(decoded.owners.first?.user.name, "bob")
        XCTAssertEqual(decoded.firstCapturedBy?.name, "carol")
    }

    func testPaginatedBikesDecoding() throws {
        let json = """
        {
            "items": [
                {"bike_qr_id": "B-1", "submission_count": 1}
            ],
            "total": 100,
            "limit": 20,
            "offset": 0
        }
        """.data(using: .utf8)!

        let decoded = try JSONDecoder().decode(PaginatedBikes.self, from: json)
        XCTAssertEqual(decoded.total, 100)
        XCTAssertEqual(decoded.items.count, 1)
    }
}

final class UserModelsTests: XCTestCase {
    func testUserResponseDecoding() throws {
        let json = """
        {
            "user_id": "u-1",
            "username": "alice",
            "is_admin": true,
            "is_banned": false,
            "created_at": "2024-01-01"
        }
        """.data(using: .utf8)!

        let decoded = try JSONDecoder().decode(UserResponse.self, from: json)
        XCTAssertEqual(decoded.userId, "u-1")
        XCTAssertEqual(decoded.username, "alice")
        XCTAssertTrue(decoded.isAdmin)
        XCTAssertFalse(decoded.isBanned)
    }

    func testUserDetailResponseDecoding() throws {
        let json = """
        {
            "user_id": "u-2",
            "username": "bob",
            "is_admin": false,
            "is_banned": false,
            "submission_count": 42,
            "first_seen_at": "2024-01-01",
            "last_seen_at": "2024-06-01",
            "owned_bike_count": 3,
            "leaderboard_ranks": [
                {"period": "weekly", "rank": 5},
                {"period": "all_time", "rank": null}
            ]
        }
        """.data(using: .utf8)!

        let decoded = try JSONDecoder().decode(UserDetailResponse.self, from: json)
        XCTAssertEqual(decoded.userId, "u-2")
        XCTAssertEqual(decoded.submissionCount, 42)
        XCTAssertEqual(decoded.ownedBikeCount, 3)
        XCTAssertEqual(decoded.leaderboardRanks.count, 2)
        XCTAssertEqual(decoded.leaderboardRanks[0].rank, 5)
        XCTAssertNil(decoded.leaderboardRanks[1].rank)
    }
}

final class TagModelsTests: XCTestCase {
    func testTagResponseDecoding() throws {
        let json = """
        {
            "tag_id": "t-1",
            "submission_id": "s-1",
            "user_id": "u-1",
            "image_url": "https://example.com/tag.jpg",
            "ring": [[0.1, 0.2], [0.3, 0.4]],
            "ring_width": 100,
            "ring_height": 200,
            "created_at": "2024-01-01T12:00:00",
            "points_awarded": 2,
            "points_breakdown": [
                {"event_type": "tag_1", "label": "1st tag", "points": 2}
            ]
        }
        """.data(using: .utf8)!

        let decoded = try JSONDecoder().decode(TagResponse.self, from: json)
        XCTAssertEqual(decoded.tagId, "t-1")
        XCTAssertEqual(decoded.ring?.count, 2)
        XCTAssertEqual(decoded.ringWidth, 100)
        XCTAssertEqual(decoded.pointsAwarded, 2)
    }

    func testTagDetailResponseDecoding() throws {
        let json = """
        {
            "tag_id": "t-2",
            "image_url": "https://example.com/tag2.jpg",
            "created_at": "2024-01-01",
            "submission_count": 5,
            "first_captured_at": "2024-01-01",
            "last_captured_at": "2024-06-01",
            "first_captured_by": {"name": "alice", "id": "u-1"},
            "last_captured_by": {"name": "bob", "id": "u-2"}
        }
        """.data(using: .utf8)!

        let decoded = try JSONDecoder().decode(TagDetailResponse.self, from: json)
        XCTAssertEqual(decoded.tagId, "t-2")
        XCTAssertEqual(decoded.submissionCount, 5)
        XCTAssertEqual(decoded.firstCapturedBy?.name, "alice")
    }
}

final class FlagModelsTests: XCTestCase {
    func testFlagStatusResponseDecoding() throws {
        let json = """
        {"flagged": true, "flag_count": 3}
        """.data(using: .utf8)!

        let decoded = try JSONDecoder().decode(FlagStatusResponse.self, from: json)
        XCTAssertTrue(decoded.flagged)
        XCTAssertEqual(decoded.flagCount, 3)
    }
}

final class BanModelsTests: XCTestCase {
    func testBanResponseDecoding() throws {
        let json = """
        {"msg": "User banned", "is_banned": true, "device_banned": true}
        """.data(using: .utf8)!

        let decoded = try JSONDecoder().decode(BanResponse.self, from: json)
        XCTAssertEqual(decoded.msg, "User banned")
        XCTAssertTrue(decoded.isBanned)
        XCTAssertTrue(decoded.deviceBanned)
    }
}

final class LeaderboardModelsTests: XCTestCase {
    func testLeaderboardResponseDecoding() throws {
        let json = """
        {
            "period": "weekly",
            "start_date": "2024-01-01",
            "end_date": "2024-01-07",
            "entries": [
                {"rank": 1, "user_id": "u-1", "username": "alice", "score": 100},
                {"rank": 2, "user_id": "u-2", "username": "bob", "score": 80}
            ]
        }
        """.data(using: .utf8)!

        let decoded = try JSONDecoder().decode(LeaderboardResponse.self, from: json)
        XCTAssertEqual(decoded.period, "weekly")
        XCTAssertEqual(decoded.startDate, "2024-01-01")
        XCTAssertEqual(decoded.entries.count, 2)
        XCTAssertEqual(decoded.entries[0].score, 100)
    }

    func testLeaderboardPeriodCodable() throws {
        let weekly = LeaderboardPeriod.weekly
        let allTime = LeaderboardPeriod.allTime

        let encoder = JSONEncoder()
        let weeklyData = try encoder.encode(weekly)
        let allTimeData = try encoder.encode(allTime)

        XCTAssertEqual(String(data: weeklyData, encoding: .utf8), "\"weekly\"")
        XCTAssertEqual(String(data: allTimeData, encoding: .utf8), "\"all_time\"")

        let decoder = JSONDecoder()
        let decodedWeekly = try decoder.decode(LeaderboardPeriod.self, from: weeklyData)
        let decodedAllTime = try decoder.decode(LeaderboardPeriod.self, from: allTimeData)
        XCTAssertEqual(decodedWeekly, .weekly)
        XCTAssertEqual(decodedAllTime, .allTime)
    }
}

final class UploadModelsTests: XCTestCase {
    func testUploadResponseDecoding() throws {
        let json = """
        {
            "url": "https://example.com/upload.jpg",
            "filename": "upload.jpg",
            "thumbnail_url": "https://example.com/thumb.jpg"
        }
        """.data(using: .utf8)!

        let decoded = try JSONDecoder().decode(UploadResponse.self, from: json)
        XCTAssertEqual(decoded.url, "https://example.com/upload.jpg")
        XCTAssertEqual(decoded.filename, "upload.jpg")
        XCTAssertEqual(decoded.thumbnailUrl, "https://example.com/thumb.jpg")
    }
}

final class BikeUrlParserTests: XCTestCase {
    func testLimeBikeUrl() {
        let result = parseBikeUrl("https://lime.bike/scooters/ABC123===")
        XCTAssertEqual(result.provider, "lime")
        XCTAssertEqual(result.bikeId, "ABC123")
    }

    func testBirdUrl() {
        let result = parseBikeUrl("https://ride.bird.co/scooters/XYZ789")
        XCTAssertEqual(result.provider, "bird")
        XCTAssertEqual(result.bikeId, "XYZ789")
    }

    func testUnknownUrl() {
        let result = parseBikeUrl("https://unknown.com/bikes/FOO")
        XCTAssertNil(result.provider)
        XCTAssertEqual(result.bikeId, "https://unknown.com/bikes/FOO")
    }

    func testPlainText() {
        let result = parseBikeUrl("BIKE-001")
        XCTAssertNil(result.provider)
        XCTAssertEqual(result.bikeId, "BIKE-001")
    }

    func testEmptyPath() {
        let result = parseBikeUrl("https://lime.bike/")
        XCTAssertNil(result.provider)
        XCTAssertEqual(result.bikeId, "https://lime.bike/")
    }

    func testIsRecognizedProvider() {
        XCTAssertTrue(isRecognizedProvider("https://lime.bike/scooters/ABC"))
        XCTAssertTrue(isRecognizedProvider("https://ride.bird.co/scooters/DEF"))
        XCTAssertFalse(isRecognizedProvider("https://unknown.com/bikes/GHI"))
        XCTAssertFalse(isRecognizedProvider("BIKE-001"))
    }
}

final class ImageUrlUtilTests: XCTestCase {
    func testAbsoluteUrl() {
        let url = imageUrl(from: "https://cdn.example.com/img.jpg", baseURL: "https://api.example.com")
        XCTAssertEqual(url?.absoluteString, "https://cdn.example.com/img.jpg")
    }

    func testRelativePath() {
        let url = imageUrl(from: "/uploads/img.jpg", baseURL: "https://api.example.com")
        XCTAssertEqual(url?.absoluteString, "https://api.example.com/uploads/img.jpg")
    }

    func testHttpUrl() {
        let url = imageUrl(from: "http://cdn.example.com/img.jpg", baseURL: "https://api.example.com")
        XCTAssertEqual(url?.absoluteString, "http://cdn.example.com/img.jpg")
    }
}

final class AppErrorTests: XCTestCase {
    func testDisplayMessages() {
        XCTAssertEqual(AppError.network("timeout").displayMessage, "Network error. Check your connection and try again.")
        XCTAssertEqual(AppError.auth(code: 401, message: "Unauthorized").displayMessage, "Unauthorized")
        XCTAssertEqual(AppError.validation(field: "email", message: "Invalid email").displayMessage, "Invalid email")
        XCTAssertEqual(AppError.server(code: 500, message: "Internal error").displayMessage, "Internal error")
        XCTAssertEqual(AppError.rateLimit(retryAfterSeconds: 30).displayMessage, "Too many attempts. Please try again later.")
        XCTAssertEqual(AppError.unknown("err").displayMessage, "An unexpected error occurred.")
    }
}

final class ApiResultTests: XCTestCase {
    func testSuccessValue() {
        let result: ApiResult<String> = .success("hello")
        XCTAssertEqual(result.value, "hello")
        XCTAssertNil(result.error)
    }

    func testFailureError() {
        let result: ApiResult<String> = .failure(.network("timeout"))
        XCTAssertNil(result.value)
        XCTAssertEqual(result.error, .network("timeout"))
    }

    func testMap() {
        let result: ApiResult<Int> = .success(42)
        let mapped = result.map { String($0) }
        XCTAssertEqual(mapped.value, "42")

        let failResult: ApiResult<Int> = .failure(.unknown("err"))
        let failMapped = failResult.map { String($0) }
        XCTAssertNil(failMapped.value)
        XCTAssertEqual(failMapped.error, .unknown("err"))
    }
}

final class ScoreRulesTests: XCTestCase {
    func testPhotoRules() {
        XCTAssertEqual(ScoreRules.photoRules.count, 4)
        XCTAssertEqual(ScoreRules.photoRules[0].id, "add_image")
        XCTAssertEqual(ScoreRules.photoRules[0].points, 2)
        XCTAssertEqual(ScoreRules.photoRules[3].id, "first_bike_ever")
        XCTAssertEqual(ScoreRules.photoRules[3].points, 10)
    }

    func testTagRules() {
        XCTAssertEqual(ScoreRules.tagRules.count, 4)
        XCTAssertEqual(ScoreRules.tagRules[0].id, "tag_1")
        XCTAssertEqual(ScoreRules.tagRules[0].points, 2)
        XCTAssertEqual(ScoreRules.tagRules[3].points, 0)
    }
}
