import io

from PIL import Image

from app.models import DeviceBan, User


def create_test_image():
    img = Image.new("RGB", (100, 100), color="red")
    buf = io.BytesIO()
    img.save(buf, format="JPEG")
    buf.seek(0)
    return buf


class TestBanEndpoint:
    """Tests for POST /users/{user_id}/ban."""

    def test_admin_can_ban_user(self, client, admin_auth_headers, test_user):
        response = client.post(
            f"/users/{test_user.user_id}/ban",
            headers=admin_auth_headers,
            json={"reason": "Spamming"},
        )
        assert response.status_code == 200
        data = response.json()
        assert data["is_banned"] is True
        assert data["msg"] == "User banned"

    def test_non_admin_cannot_ban(self, client, auth_headers, test_admin_user):
        response = client.post(
            f"/users/{test_admin_user.user_id}/ban",
            headers=auth_headers,
            json={},
        )
        assert response.status_code == 403

    def test_ban_nonexistent_user(self, client, admin_auth_headers):
        response = client.post(
            "/users/00000000-0000-0000-0000-000000000000/ban",
            headers=admin_auth_headers,
            json={},
        )
        assert response.status_code == 404

    def test_ban_creates_device_ban(self, client, admin_auth_headers, db_session, test_user):
        # Give user an android_id
        user = db_session.query(User).filter(User.user_id == test_user.user_id).first()
        user.android_id = "device-abc-123"
        db_session.commit()

        response = client.post(
            f"/users/{test_user.user_id}/ban",
            headers=admin_auth_headers,
            json={"reason": "Bad behavior"},
        )
        assert response.status_code == 200
        assert response.json()["device_banned"] is True

        # Verify DeviceBan record exists
        device_ban = db_session.query(DeviceBan).filter(DeviceBan.android_id == "device-abc-123").first()
        assert device_ban is not None
        assert device_ban.reason == "Bad behavior"

    def test_ban_without_android_id_no_device_ban(self, client, admin_auth_headers, test_user):
        response = client.post(
            f"/users/{test_user.user_id}/ban",
            headers=admin_auth_headers,
            json={},
        )
        assert response.status_code == 200
        assert response.json()["device_banned"] is False


class TestUnbanEndpoint:
    """Tests for DELETE /users/{user_id}/ban."""

    def test_admin_can_unban_user(self, client, admin_auth_headers, test_banned_user):
        response = client.delete(
            f"/users/{test_banned_user.user_id}/ban",
            headers=admin_auth_headers,
        )
        assert response.status_code == 200
        data = response.json()
        assert data["is_banned"] is False
        assert data["msg"] == "User unbanned"

    def test_unban_removes_device_ban(self, client, admin_auth_headers, test_banned_user, db_session):
        # Create a device ban for the banned user's android_id
        device_ban = DeviceBan(
            android_id=test_banned_user.android_id,
            banned_by=test_banned_user.user_id,  # doesn't matter for test
        )
        db_session.add(device_ban)
        db_session.commit()

        response = client.delete(
            f"/users/{test_banned_user.user_id}/ban",
            headers=admin_auth_headers,
        )
        assert response.status_code == 200
        assert response.json()["device_banned"] is True

        # Verify DeviceBan record removed
        remaining = db_session.query(DeviceBan).filter(
            DeviceBan.android_id == test_banned_user.android_id
        ).first()
        assert remaining is None

    def test_non_admin_cannot_unban(self, client, auth_headers, test_banned_user):
        response = client.delete(
            f"/users/{test_banned_user.user_id}/ban",
            headers=auth_headers,
        )
        assert response.status_code == 403


class TestBannedUserCannotCreateContent:
    """Tests that banned users cannot create submissions or tags."""

    def test_banned_user_cannot_create_submission(
        self, client, banned_auth_headers, test_bike
    ):
        image = create_test_image()
        response = client.post(
            "/submissions",
            headers=banned_auth_headers,
            files={"image": ("test.jpg", image, "image/jpeg")},
            data={
                "bike_qr_id": f"https://bikeshare.com/{test_bike.bike_qr_id}",
                "captured_date": "2026-01-01",
            },
        )
        assert response.status_code == 403
        assert "banned" in response.json()["detail"]["msg"].lower()

    def test_banned_user_cannot_create_tag(
        self, client, banned_auth_headers, test_submission
    ):
        image = create_test_image()
        response = client.post(
            f"/submissions/{test_submission.submission_id}/tags",
            headers=banned_auth_headers,
            files={"image": ("test.png", image, "image/png")},
        )
        assert response.status_code == 403
        assert "banned" in response.json()["detail"]["msg"].lower()


class TestDeviceBanOnRegister:
    """Tests that registration is rejected when android_id is banned."""

    def test_register_with_banned_device_rejected(self, client, db_session, test_admin_user):
        device_ban = DeviceBan(
            android_id="banned-device-xyz",
            banned_by=test_admin_user.user_id,
        )
        db_session.add(device_ban)
        db_session.commit()

        response = client.post(
            "/auth/register",
            json={
                "username": "newuser123",
                "password": "password123",
                "android_id": "banned-device-xyz",
            },
        )
        assert response.status_code == 403
        assert "device" in response.json()["detail"]["msg"].lower()

    def test_register_with_unbanned_device_succeeds(self, client):
        response = client.post(
            "/auth/register",
            json={
                "username": "newuser456",
                "password": "password123",
                "android_id": "clean-device-abc",
            },
        )
        assert response.status_code == 201

    def test_register_stores_android_id(self, client, db_session):
        client.post(
            "/auth/register",
            json={
                "username": "deviceuser",
                "password": "password123",
                "android_id": "my-device-id",
            },
        )
        user = db_session.query(User).filter(User.username == "deviceuser").first()
        assert user is not None
        assert user.android_id == "my-device-id"


class TestBannedUserLogin:
    """Tests that banned users cannot login."""

    def test_banned_user_login_rejected(self, client, test_banned_user):
        response = client.post(
            "/auth/login",
            json={
                "username": "banneduser",
                "password": "bannedpassword123",
            },
        )
        assert response.status_code == 403
        assert "banned" in response.json()["detail"]["msg"].lower()

    def test_login_with_banned_device_rejected(self, client, db_session, test_user, test_user_data, test_admin_user):
        device_ban = DeviceBan(
            android_id="banned-login-device",
            banned_by=test_admin_user.user_id,
        )
        db_session.add(device_ban)
        db_session.commit()

        response = client.post(
            "/auth/login",
            json={
                "username": test_user_data["username"],
                "password": test_user_data["password"],
                "android_id": "banned-login-device",
            },
        )
        assert response.status_code == 403
        assert "device" in response.json()["detail"]["msg"].lower()

    def test_login_does_not_overwrite_android_id(self, client, db_session, test_user, test_user_data):
        """Login should not update android_id — only registration sets it."""
        # Give user an existing android_id (as if they registered with it)
        user = db_session.query(User).filter(User.user_id == test_user.user_id).first()
        user.android_id = "original-device"
        db_session.commit()

        response = client.post(
            "/auth/login",
            json={
                "username": test_user_data["username"],
                "password": test_user_data["password"],
                "android_id": "different-device",
            },
        )
        assert response.status_code == 200

        db_session.expire_all()
        user = db_session.query(User).filter(User.user_id == test_user.user_id).first()
        assert user.android_id == "original-device"


class TestUnbanRestoresAccess:
    """Tests that unbanning restores user access."""

    def test_unban_allows_login(self, client, admin_auth_headers, test_banned_user):
        # Unban the user
        client.delete(
            f"/users/{test_banned_user.user_id}/ban",
            headers=admin_auth_headers,
        )

        # Now the user should be able to login
        response = client.post(
            "/auth/login",
            json={
                "username": "banneduser",
                "password": "bannedpassword123",
            },
        )
        assert response.status_code == 200
        assert "access_token" in response.json()


class TestUserDetailShowsBanStatus:
    """Tests that user detail response includes ban status."""

    def test_user_detail_shows_banned(self, client, test_banned_user):
        response = client.get(f"/users/{test_banned_user.user_id}")
        assert response.status_code == 200
        assert response.json()["is_banned"] is True

    def test_user_detail_shows_not_banned(self, client, test_user):
        response = client.get(f"/users/{test_user.user_id}")
        assert response.status_code == 200
        assert response.json()["is_banned"] is False

    def test_user_me_shows_admin_status(self, client, admin_auth_headers):
        response = client.get("/users/me", headers=admin_auth_headers)
        assert response.status_code == 200
        assert response.json()["is_admin"] is True

    def test_user_me_shows_ban_status(self, client, auth_headers):
        response = client.get("/users/me", headers=auth_headers)
        assert response.status_code == 200
        assert response.json()["is_banned"] is False
