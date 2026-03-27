import Testing
import Foundation
import TCATBModels
@testable import TCATBSharedUI

@Suite("groupSubmissionsByDate")
struct GroupSubmissionsByDateTests {

    private func makeSubmission(
        id: String = "sub-1",
        capturedDate: String? = nil,
        uploadedAt: String? = nil
    ) -> SubmissionResponse {
        SubmissionResponse(
            submissionId: id,
            userId: "user-1",
            bikeQrId: "bike-1",
            capturedDate: capturedDate,
            uploadedAt: uploadedAt
        )
    }

    @Test("empty array returns empty groups")
    func emptyArray() {
        let groups = groupSubmissionsByDate([])
        #expect(groups.isEmpty)
    }

    @Test("single submission returns one group")
    func singleSubmission() {
        let sub = makeSubmission(capturedDate: "2026-03-01T12:00:00Z")
        let groups = groupSubmissionsByDate([sub])
        #expect(groups.count == 1)
        #expect(groups[0].submissions.count == 1)
        #expect(groups[0].submissions[0].submissionId == "sub-1")
    }

    @Test("same-date submissions grouped together")
    func sameDateGrouping() {
        let subs = [
            makeSubmission(id: "sub-1", capturedDate: "2026-03-01T10:00:00Z"),
            makeSubmission(id: "sub-2", capturedDate: "2026-03-01T14:00:00Z"),
        ]
        let groups = groupSubmissionsByDate(subs)
        #expect(groups.count == 1)
        #expect(groups[0].submissions.count == 2)
    }

    @Test("different dates create separate groups")
    func differentDateGroups() {
        let subs = [
            makeSubmission(id: "sub-1", capturedDate: "2026-03-01T12:00:00Z"),
            makeSubmission(id: "sub-2", capturedDate: "2026-03-02T12:00:00Z"),
        ]
        let groups = groupSubmissionsByDate(subs)
        #expect(groups.count == 2)
        #expect(groups[0].submissions.count == 1)
        #expect(groups[1].submissions.count == 1)
    }

    @Test("current year has nil yearLabel")
    func currentYearNilLabel() {
        // Use a date in the current year
        let year = Calendar.current.component(.year, from: Date())
        let sub = makeSubmission(capturedDate: "\(year)-06-15T12:00:00Z")
        let groups = groupSubmissionsByDate([sub])
        #expect(groups.count == 1)
        #expect(groups[0].yearLabel == nil)
    }

    @Test("past year shows yearLabel")
    func pastYearLabel() {
        let sub = makeSubmission(capturedDate: "2024-06-15T12:00:00Z")
        let groups = groupSubmissionsByDate([sub])
        #expect(groups.count == 1)
        #expect(groups[0].yearLabel == "2024")
    }

    @Test("submissions with nil dates are skipped")
    func nilDatesSkipped() {
        let subs = [
            makeSubmission(id: "sub-1", capturedDate: nil, uploadedAt: nil),
            makeSubmission(id: "sub-2", capturedDate: "2026-03-01T12:00:00Z"),
        ]
        let groups = groupSubmissionsByDate(subs)
        #expect(groups.count == 1)
        #expect(groups[0].submissions.count == 1)
        #expect(groups[0].submissions[0].submissionId == "sub-2")
    }

    @Test("submissions with invalid dates are skipped")
    func invalidDatesSkipped() {
        let subs = [
            makeSubmission(id: "sub-1", capturedDate: "not-a-date"),
            makeSubmission(id: "sub-2", capturedDate: "2026-03-01T12:00:00Z"),
        ]
        let groups = groupSubmissionsByDate(subs)
        #expect(groups.count == 1)
        #expect(groups[0].submissions[0].submissionId == "sub-2")
    }

    @Test("prefers capturedDate over uploadedAt")
    func prefersCapturedDate() {
        let sub = makeSubmission(
            capturedDate: "2026-03-01T12:00:00Z",
            uploadedAt: "2026-03-05T12:00:00Z"
        )
        let groups = groupSubmissionsByDate([sub])
        #expect(groups.count == 1)
        let expected = DateFormatting.formatMonthDay(
            DateFormatting.parseDate("2026-03-01T12:00:00Z")!
        )
        #expect(groups[0].dateLabel == expected)
    }

    @Test("falls back to uploadedAt when capturedDate is nil")
    func fallsBackToUploadedAt() {
        let sub = makeSubmission(
            capturedDate: nil,
            uploadedAt: "2026-03-05T12:00:00Z"
        )
        let groups = groupSubmissionsByDate([sub])
        #expect(groups.count == 1)
        let expected = DateFormatting.formatMonthDay(
            DateFormatting.parseDate("2026-03-05T12:00:00Z")!
        )
        #expect(groups[0].dateLabel == expected)
    }

    @Test("preserves input order within groups")
    func preservesOrder() {
        let subs = [
            makeSubmission(id: "first", capturedDate: "2026-03-01T08:00:00Z"),
            makeSubmission(id: "second", capturedDate: "2026-03-01T16:00:00Z"),
            makeSubmission(id: "third", capturedDate: "2026-03-01T23:00:00Z"),
        ]
        let groups = groupSubmissionsByDate(subs)
        #expect(groups.count == 1)
        #expect(groups[0].submissions.map(\.submissionId) == ["first", "second", "third"])
    }
}

@Suite("formattedDate")
struct FormattedDateTests {

    @Test("valid date returns formatted string")
    func validDate() {
        let result = formattedDate("2026-03-01T12:00:00Z")
        let expected = " on " + DateFormatting.formatDisplayDate(
            DateFormatting.parseDate("2026-03-01T12:00:00Z")!
        )
        #expect(result == expected)
    }

    @Test("nil input returns empty string")
    func nilInput() {
        let result = formattedDate(nil)
        #expect(result == "")
    }

    @Test("invalid date returns empty string")
    func invalidDate() {
        let result = formattedDate("not-a-date")
        #expect(result == "")
    }
}
