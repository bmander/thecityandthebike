import asyncio
from unittest.mock import MagicMock

from tests.conftest import TestingSessionLocal


class TestAdminAccess:
    def test_admin_login_page_accessible(self, client):
        response = client.get("/admin/login", follow_redirects=False)
        assert response.status_code == 200

    def test_unauthenticated_redirects_to_login(self, client):
        response = client.get("/admin/", follow_redirects=False)
        assert response.status_code in (302, 303)
        assert "/admin/login" in response.headers["location"]

    def test_admin_user_can_login(self, client, test_admin_user):
        response = client.post(
            "/admin/login",
            data={"username": "adminuser", "password": "adminpassword123"},
            follow_redirects=False,
        )
        assert response.status_code in (302, 303)
        assert "/admin/login" not in response.headers.get("location", "")

    def test_non_admin_user_rejected(self, client, test_user, test_user_data):
        response = client.post(
            "/admin/login",
            data={"username": test_user_data["username"], "password": test_user_data["password"]},
            follow_redirects=False,
        )
        assert response.status_code == 400

    def test_wrong_password_rejected(self, client, test_admin_user):
        response = client.post(
            "/admin/login",
            data={"username": "adminuser", "password": "wrongpassword"},
            follow_redirects=False,
        )
        assert response.status_code == 400

    def test_authenticate_invalid_uuid_in_session(self):
        from app.admin import AdminAuth

        auth = AdminAuth(secret_key="test", session_factory=TestingSessionLocal)
        request = MagicMock()
        request.session = {"user_id": "not-a-uuid"}
        result = asyncio.run(auth.authenticate(request))
        assert result is False

    def test_authenticate_deleted_user(self, db_session, test_admin_user):
        from app.admin import AdminAuth
        from app.models import User

        # Delete the admin user
        user = db_session.query(User).filter(
            User.user_id == test_admin_user.user_id
        ).first()
        db_session.delete(user)
        db_session.commit()

        auth = AdminAuth(secret_key="test", session_factory=TestingSessionLocal)
        request = MagicMock()
        request.session = {"user_id": str(test_admin_user.user_id)}
        result = asyncio.run(auth.authenticate(request))
        assert result is False


class TestAdminLogout:
    def test_logout_clears_session_and_returns_true(self):
        from app.admin import AdminAuth

        auth = AdminAuth(secret_key="test", session_factory=TestingSessionLocal)
        session = {"user_id": "x", "extra": "y"}
        request = MagicMock()
        request.session = session
        result = asyncio.run(auth.logout(request))
        assert result is True
        assert session == {}


class TestAdminModelViews:
    def _login(self, client):
        client.post(
            "/admin/login",
            data={"username": "adminuser", "password": "adminpassword123"},
            follow_redirects=False,
        )

    def test_user_list_accessible(self, client, test_admin_user):
        self._login(client)
        response = client.get("/admin/user/list")
        assert response.status_code == 200
        assert "User" in response.text

    def test_bike_list_accessible(self, client, test_admin_user):
        self._login(client)
        response = client.get("/admin/bike/list")
        assert response.status_code == 200
        assert "Bike" in response.text

    def test_fender_submission_list_accessible(self, client, test_admin_user):
        self._login(client)
        response = client.get("/admin/fender-submission/list")
        assert response.status_code == 200


class TestPromoteAdmin:
    def test_promote_existing_user(self, db_session, test_user):
        from app.promote_admin import promote_user
        from tests.conftest import TestingSessionLocal

        result = promote_user(test_user.username, session_factory=TestingSessionLocal)
        assert result is True

        from app.models import User
        user = db_session.query(User).filter(User.username == test_user.username).first()
        assert user.is_admin is True

    def test_promote_nonexistent_user(self):
        from app.promote_admin import promote_user
        from tests.conftest import TestingSessionLocal

        result = promote_user("nonexistent", session_factory=TestingSessionLocal)
        assert result is False

    def test_promote_already_admin(self, db_session, test_admin_user):
        from app.promote_admin import promote_user
        from tests.conftest import TestingSessionLocal

        result = promote_user(test_admin_user.username, session_factory=TestingSessionLocal)
        assert result is True
