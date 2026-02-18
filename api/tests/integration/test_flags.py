import uuid

from app.models import Flag


class TestCreateFlag:
    def test_create_flag_success(self, client, auth_headers, test_submission):
        response = client.post(
            f"/submissions/{test_submission.submission_id}/flags",
            headers=auth_headers,
        )
        assert response.status_code == 201
        data = response.json()
        assert data["flagged"] is True
        assert data["flag_count"] == 1

    def test_create_flag_duplicate_returns_409(self, client, auth_headers, test_submission):
        client.post(
            f"/submissions/{test_submission.submission_id}/flags",
            headers=auth_headers,
        )
        response = client.post(
            f"/submissions/{test_submission.submission_id}/flags",
            headers=auth_headers,
        )
        assert response.status_code == 409
        assert "Already flagged" in response.json()["detail"]

    def test_create_flag_nonexistent_submission(self, client, auth_headers):
        fake_id = str(uuid.uuid4())
        response = client.post(
            f"/submissions/{fake_id}/flags",
            headers=auth_headers,
        )
        assert response.status_code == 404

    def test_create_flag_no_auth(self, client, test_submission):
        response = client.post(
            f"/submissions/{test_submission.submission_id}/flags",
        )
        assert response.status_code == 401


class TestGetFlagStatus:
    def test_not_flagged(self, client, auth_headers, test_submission):
        response = client.get(
            f"/submissions/{test_submission.submission_id}/flags/me",
            headers=auth_headers,
        )
        assert response.status_code == 200
        data = response.json()
        assert data["flagged"] is False
        assert data["flag_count"] == 0

    def test_flagged_by_me(self, client, auth_headers, test_submission):
        client.post(
            f"/submissions/{test_submission.submission_id}/flags",
            headers=auth_headers,
        )
        response = client.get(
            f"/submissions/{test_submission.submission_id}/flags/me",
            headers=auth_headers,
        )
        assert response.status_code == 200
        data = response.json()
        assert data["flagged"] is True
        assert data["flag_count"] == 1

    def test_flagged_by_another(self, client, auth_headers, test_submission, db_session):
        """When another user flags a submission, /me should show flagged=False but flag_count=1."""
        from app.dependencies import get_password_hash, create_access_token
        from app.models import User

        other_user = User(
            username="otheruser",
            email="other@example.com",
            password_hash=get_password_hash("password123"),
        )
        db_session.add(other_user)
        db_session.commit()
        db_session.refresh(other_user)

        other_token = create_access_token(subject=str(other_user.user_id))
        other_headers = {"Authorization": f"Bearer {other_token}"}

        # Other user flags the submission
        resp = client.post(
            f"/submissions/{test_submission.submission_id}/flags",
            headers=other_headers,
        )
        assert resp.status_code == 201

        # Original user checks their flag status
        response = client.get(
            f"/submissions/{test_submission.submission_id}/flags/me",
            headers=auth_headers,
        )
        assert response.status_code == 200
        data = response.json()
        assert data["flagged"] is False
        assert data["flag_count"] == 1

    def test_get_flag_status_no_auth(self, client, test_submission):
        response = client.get(
            f"/submissions/{test_submission.submission_id}/flags/me",
        )
        assert response.status_code == 401

    def test_get_flag_status_nonexistent_submission(self, client, auth_headers):
        fake_id = str(uuid.uuid4())
        response = client.get(
            f"/submissions/{fake_id}/flags/me",
            headers=auth_headers,
        )
        assert response.status_code == 404


class TestFlagCascadeDelete:
    def test_deleting_submission_deletes_its_flags(
        self, client, auth_headers, db_session, test_submission,
    ):
        """Flags should be cascade-deleted when their parent submission is deleted."""
        # Create a flag on the submission
        resp = client.post(
            f"/submissions/{test_submission.submission_id}/flags",
            headers=auth_headers,
        )
        assert resp.status_code == 201

        # Verify flag exists
        status_resp = client.get(
            f"/submissions/{test_submission.submission_id}/flags/me",
            headers=auth_headers,
        )
        assert status_resp.json()["flag_count"] == 1

        # Delete the submission
        del_resp = client.delete(
            f"/submissions/{test_submission.submission_id}",
            headers=auth_headers,
        )
        assert del_resp.status_code == 200

        # Verify flags were cascade-deleted from the DB
        remaining = db_session.query(Flag).filter(
            Flag.submission_id == test_submission.submission_id
        ).count()
        assert remaining == 0
