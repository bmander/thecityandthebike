import io
import uuid

from PIL import Image

from tests.conftest import create_test_image


def create_test_png(width=100, height=100):
    """Create a real PNG image with transparency for testing."""
    img = Image.new("RGBA", (width, height), (255, 0, 0, 128))
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    buf.seek(0)
    return buf


class TestListTags:
    def test_list_tags_empty(self, client, test_submission):
        response = client.get(f"/submissions/{test_submission.submission_id}/tags")
        assert response.status_code == 200
        assert response.json() == []

    def test_list_tags_with_tags(self, client, test_tag, test_submission):
        response = client.get(f"/submissions/{test_submission.submission_id}/tags")
        assert response.status_code == 200
        data = response.json()
        assert len(data) == 1
        assert data[0]["tag_id"] == str(test_tag.tag_id)

    def test_list_tags_nonexistent_submission(self, client):
        fake_id = str(uuid.uuid4())
        response = client.get(f"/submissions/{fake_id}/tags")
        assert response.status_code == 404


class TestCreateTag:
    def test_create_tag_success(self, client, auth_headers, test_submission):
        png_buf = create_test_png()
        response = client.post(
            f"/submissions/{test_submission.submission_id}/tags",
            headers=auth_headers,
            files={"image": ("tag.png", png_buf, "image/png")},
        )
        assert response.status_code == 201
        data = response.json()
        assert data["submission_id"] == str(test_submission.submission_id)
        assert "tag_id" in data
        assert data["image_url"].endswith(".png")

    def test_create_tag_no_auth(self, client, test_submission):
        png_buf = create_test_png()
        response = client.post(
            f"/submissions/{test_submission.submission_id}/tags",
            files={"image": ("tag.png", png_buf, "image/png")},
        )
        assert response.status_code == 401

    def test_create_tag_nonexistent_submission(self, client, auth_headers):
        fake_id = str(uuid.uuid4())
        png_buf = create_test_png()
        response = client.post(
            f"/submissions/{fake_id}/tags",
            headers=auth_headers,
            files={"image": ("tag.png", png_buf, "image/png")},
        )
        assert response.status_code == 404

    def test_create_tag_invalid_file_type(self, client, auth_headers, test_submission):
        jpg_buf = create_test_image()
        response = client.post(
            f"/submissions/{test_submission.submission_id}/tags",
            headers=auth_headers,
            files={"image": ("tag.jpg", jpg_buf, "image/jpeg")},
        )
        assert response.status_code == 400


class TestDeleteTag:
    def test_delete_own_tag(self, client, auth_headers, test_tag):
        response = client.delete(
            f"/tags/{test_tag.tag_id}",
            headers=auth_headers,
        )
        assert response.status_code == 200
        assert response.json()["msg"] == "Tag deleted"

    def test_delete_other_users_tag(self, client, test_tag, db_session):
        """A different user should not be able to delete another user's tag."""
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

        response = client.delete(
            f"/tags/{test_tag.tag_id}",
            headers=other_headers,
        )
        assert response.status_code == 403

    def test_delete_nonexistent_tag(self, client, auth_headers):
        fake_id = str(uuid.uuid4())
        response = client.delete(
            f"/tags/{fake_id}",
            headers=auth_headers,
        )
        assert response.status_code == 404

    def test_delete_tag_no_auth(self, client, test_tag):
        response = client.delete(f"/tags/{test_tag.tag_id}")
        assert response.status_code == 401
