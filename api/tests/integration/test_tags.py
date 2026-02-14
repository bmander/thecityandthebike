import io
import json
import os
import uuid

from PIL import Image, ImageDraw

from app.models import Tag
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

    def test_list_tags_response_shape(self, client, test_tag, test_submission):
        """All expected fields are present in the response."""
        response = client.get(f"/submissions/{test_submission.submission_id}/tags")
        assert response.status_code == 200
        tag = response.json()[0]
        assert tag["tag_id"] == str(test_tag.tag_id)
        assert tag["submission_id"] == str(test_tag.submission_id)
        assert tag["user_id"] == str(test_tag.user_id)
        assert tag["image_url"] == test_tag.image_url
        assert "created_at" in tag

    def test_list_tags_ordered_by_created_at_desc(
        self, client, db_session, test_submission, test_user
    ):
        """Multiple tags are returned newest-first."""
        from datetime import datetime, timezone, timedelta

        older = Tag(
            submission_id=test_submission.submission_id,
            user_id=test_user.user_id,
            image_url="/uploads/images/old.png",
            created_at=datetime(2020, 1, 1, tzinfo=timezone.utc),
        )
        newer = Tag(
            submission_id=test_submission.submission_id,
            user_id=test_user.user_id,
            image_url="/uploads/images/new.png",
            created_at=datetime(2025, 1, 1, tzinfo=timezone.utc),
        )
        db_session.add_all([older, newer])
        db_session.commit()

        response = client.get(f"/submissions/{test_submission.submission_id}/tags")
        data = response.json()
        assert len(data) == 2
        assert data[0]["image_url"] == "/uploads/images/new.png"
        assert data[1]["image_url"] == "/uploads/images/old.png"

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

    def test_create_tag_corrupt_image(self, client, auth_headers, test_submission):
        """A .png file with garbage bytes should be rejected."""
        corrupt_buf = io.BytesIO(b"not-a-real-image")
        response = client.post(
            f"/submissions/{test_submission.submission_id}/tags",
            headers=auth_headers,
            files={"image": ("tag.png", corrupt_buf, "image/png")},
        )
        assert response.status_code == 400

    def test_create_tag_non_image_bytes_with_png_extension(
        self, client, auth_headers, test_submission
    ):
        """Random bytes that happen to have a .png extension should be rejected."""
        random_bytes = io.BytesIO(os.urandom(256))
        response = client.post(
            f"/submissions/{test_submission.submission_id}/tags",
            headers=auth_headers,
            files={"image": ("payload.png", random_bytes, "image/png")},
        )
        assert response.status_code == 400

    def test_create_tag_with_ring(self, client, auth_headers, test_submission):
        """Ring data is stored when provided alongside the image."""
        png_buf = create_test_png()
        ring = [[0.0, 0.0], [100.0, 0.0], [100.0, 100.0], [0.0, 100.0], [0.0, 0.0]]
        response = client.post(
            f"/submissions/{test_submission.submission_id}/tags",
            headers=auth_headers,
            files={"image": ("tag.png", png_buf, "image/png")},
            data={
                "ring": json.dumps(ring),
                "ring_width": "200",
                "ring_height": "200",
            },
        )
        assert response.status_code == 201
        data = response.json()
        assert data["ring"] == ring
        assert data["ring_width"] == 200
        assert data["ring_height"] == 200

    def test_create_tag_without_ring(self, client, auth_headers, test_submission):
        """Ring fields are null when not provided."""
        png_buf = create_test_png()
        response = client.post(
            f"/submissions/{test_submission.submission_id}/tags",
            headers=auth_headers,
            files={"image": ("tag.png", png_buf, "image/png")},
        )
        assert response.status_code == 201
        data = response.json()
        assert data["ring"] is None
        assert data["ring_width"] is None
        assert data["ring_height"] is None

    def test_create_tag_invalid_ring_json(self, client, auth_headers, test_submission):
        """Invalid ring JSON should return 400."""
        png_buf = create_test_png()
        response = client.post(
            f"/submissions/{test_submission.submission_id}/tags",
            headers=auth_headers,
            files={"image": ("tag.png", png_buf, "image/png")},
            data={"ring": "not-valid-json"},
        )
        assert response.status_code == 400

    def test_create_tag_ring_without_dimensions(self, client, auth_headers, test_submission):
        """Sending ring without ring_width/ring_height stores ring but null dimensions."""
        png_buf = create_test_png()
        ring = [[0.0, 0.0], [50.0, 0.0], [50.0, 50.0], [0.0, 0.0]]
        response = client.post(
            f"/submissions/{test_submission.submission_id}/tags",
            headers=auth_headers,
            files={"image": ("tag.png", png_buf, "image/png")},
            data={"ring": json.dumps(ring)},
        )
        assert response.status_code == 201
        data = response.json()
        assert data["ring"] == ring
        assert data["ring_width"] is None
        assert data["ring_height"] is None

    def test_create_tag_dimensions_without_ring(self, client, auth_headers, test_submission):
        """Sending ring_width/ring_height without ring stores dimensions but null ring."""
        png_buf = create_test_png()
        response = client.post(
            f"/submissions/{test_submission.submission_id}/tags",
            headers=auth_headers,
            files={"image": ("tag.png", png_buf, "image/png")},
            data={"ring_width": "200", "ring_height": "200"},
        )
        assert response.status_code == 201
        data = response.json()
        assert data["ring"] is None
        assert data["ring_width"] == 200
        assert data["ring_height"] == 200

    def test_list_tags_includes_ring(
        self, client, auth_headers, test_submission
    ):
        """Ring data is included in the list tags response."""
        png_buf = create_test_png()
        ring = [[10.0, 20.0], [30.0, 40.0], [50.0, 60.0], [10.0, 20.0]]
        client.post(
            f"/submissions/{test_submission.submission_id}/tags",
            headers=auth_headers,
            files={"image": ("tag.png", png_buf, "image/png")},
            data={
                "ring": json.dumps(ring),
                "ring_width": "100",
                "ring_height": "100",
            },
        )
        response = client.get(f"/submissions/{test_submission.submission_id}/tags")
        assert response.status_code == 200
        data = response.json()
        assert len(data) == 1
        assert data[0]["ring"] == ring
        assert data[0]["ring_width"] == 100
        assert data[0]["ring_height"] == 100


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

    def test_delete_tag_removes_stored_image(self, client, auth_headers, test_submission):
        """Deleting a tag should also delete the stored image file."""
        # First create a real tag so a file gets written to disk
        png_buf = create_test_png()
        create_resp = client.post(
            f"/submissions/{test_submission.submission_id}/tags",
            headers=auth_headers,
            files={"image": ("tag.png", png_buf, "image/png")},
        )
        assert create_resp.status_code == 201
        tag_data = create_resp.json()

        # Verify the file exists on disk
        from app.routers.uploads import UPLOAD_DIR

        filename = tag_data["image_url"].rsplit("/", 1)[-1]
        file_path = os.path.join(UPLOAD_DIR, "images", filename)
        assert os.path.isfile(file_path)

        # Delete the tag
        delete_resp = client.delete(
            f"/tags/{tag_data['tag_id']}",
            headers=auth_headers,
        )
        assert delete_resp.status_code == 200

        # Verify the file was removed
        assert not os.path.isfile(file_path)


def _create_mask_png(width=200, height=200, draw_fn=None):
    """Create a PNG mask image for testing."""
    img = Image.new("L", (width, height), 0)
    if draw_fn:
        draw_fn(img)
    else:
        draw = ImageDraw.Draw(img)
        draw.ellipse([50, 50, 150, 150], fill=255)
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    buf.seek(0)
    return buf


class TestProcessMask:
    def test_process_mask_success(self, client, auth_headers, test_submission):
        mask_buf = _create_mask_png()
        response = client.post(
            f"/submissions/{test_submission.submission_id}/process-mask",
            headers=auth_headers,
            files={"image": ("mask.png", mask_buf, "image/png")},
        )
        assert response.status_code == 200
        data = response.json()
        assert "ring" in data
        assert "width" in data
        assert "height" in data
        assert data["width"] == 200
        assert data["height"] == 200
        assert len(data["ring"]) >= 4

    def test_process_mask_no_auth(self, client, test_submission):
        mask_buf = _create_mask_png()
        response = client.post(
            f"/submissions/{test_submission.submission_id}/process-mask",
            files={"image": ("mask.png", mask_buf, "image/png")},
        )
        assert response.status_code == 401

    def test_process_mask_nonexistent_submission(self, client, auth_headers):
        fake_id = str(uuid.uuid4())
        mask_buf = _create_mask_png()
        response = client.post(
            f"/submissions/{fake_id}/process-mask",
            headers=auth_headers,
            files={"image": ("mask.png", mask_buf, "image/png")},
        )
        assert response.status_code == 404

    def test_process_mask_invalid_file_type(self, client, auth_headers, test_submission):
        jpg_buf = create_test_image()
        response = client.post(
            f"/submissions/{test_submission.submission_id}/process-mask",
            headers=auth_headers,
            files={"image": ("mask.jpg", jpg_buf, "image/jpeg")},
        )
        assert response.status_code == 400

    def test_process_mask_empty_mask(self, client, auth_headers, test_submission):
        """An all-black mask should return 422."""
        mask_buf = _create_mask_png(draw_fn=lambda img: None)
        response = client.post(
            f"/submissions/{test_submission.submission_id}/process-mask",
            headers=auth_headers,
            files={"image": ("mask.png", mask_buf, "image/png")},
        )
        assert response.status_code == 422

    def test_process_mask_corrupt_image(self, client, auth_headers, test_submission):
        corrupt_buf = io.BytesIO(b"not-a-real-image")
        response = client.post(
            f"/submissions/{test_submission.submission_id}/process-mask",
            headers=auth_headers,
            files={"image": ("mask.png", corrupt_buf, "image/png")},
        )
        assert response.status_code == 400


class TestTagCascadeDelete:
    def test_deleting_submission_deletes_its_tags(
        self, client, auth_headers, db_session, test_submission
    ):
        """Tags should be cascade-deleted when their parent submission is deleted."""
        # Create two tags on the submission
        for _ in range(2):
            png_buf = create_test_png()
            resp = client.post(
                f"/submissions/{test_submission.submission_id}/tags",
                headers=auth_headers,
                files={"image": ("tag.png", png_buf, "image/png")},
            )
            assert resp.status_code == 201

        # Verify tags exist
        list_resp = client.get(f"/submissions/{test_submission.submission_id}/tags")
        assert len(list_resp.json()) == 2

        # Delete the submission
        del_resp = client.delete(
            f"/submissions/{test_submission.submission_id}",
            headers=auth_headers,
        )
        assert del_resp.status_code == 200

        # Verify tags were cascade-deleted from the DB
        remaining = db_session.query(Tag).filter(
            Tag.submission_id == test_submission.submission_id
        ).count()
        assert remaining == 0
