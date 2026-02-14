import io

import pytest
from PIL import Image, ImageDraw

from app.services.mask_processing import process_mask_to_ring


def _make_mask_png(width=200, height=200, draw_fn=None):
    """Create a PNG mask image. By default draws a white circle on black."""
    img = Image.new("L", (width, height), 0)
    if draw_fn:
        draw_fn(img)
    else:
        draw = ImageDraw.Draw(img)
        draw.ellipse([50, 50, 150, 150], fill=255)
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    return buf.getvalue()


class TestProcessMaskToRing:
    def test_single_blob_returns_valid_ring(self):
        image_bytes = _make_mask_png()
        result = process_mask_to_ring(image_bytes)
        assert "ring" in result
        assert "width" in result
        assert "height" in result
        assert len(result["ring"]) >= 4  # at least a triangle + closing point

    def test_correct_dimensions_returned(self):
        image_bytes = _make_mask_png(width=320, height=240)
        result = process_mask_to_ring(image_bytes)
        assert result["width"] == 320
        assert result["height"] == 240

    def test_ring_is_closed(self):
        image_bytes = _make_mask_png()
        result = process_mask_to_ring(image_bytes)
        ring = result["ring"]
        assert ring[0] == ring[-1], "Ring should be closed (first == last coordinate)"

    def test_multiple_blobs_keeps_largest(self):
        def draw_two_blobs(img):
            draw = ImageDraw.Draw(img)
            # Small blob
            draw.ellipse([10, 10, 30, 30], fill=255)
            # Large blob
            draw.ellipse([60, 60, 180, 180], fill=255)

        image_bytes = _make_mask_png(draw_fn=draw_two_blobs)
        result = process_mask_to_ring(image_bytes)
        ring = result["ring"]
        # All ring coordinates should be in the region of the large blob
        xs = [p[0] for p in ring]
        ys = [p[1] for p in ring]
        assert min(xs) >= 50, "Ring should correspond to the larger blob"
        assert min(ys) >= 50, "Ring should correspond to the larger blob"

    def test_donut_shape_hole_is_filled(self):
        """A shape with a hole (donut) should produce a single exterior ring
        without interior rings because RETR_EXTERNAL ignores holes."""
        def draw_donut(img):
            draw = ImageDraw.Draw(img)
            draw.ellipse([20, 20, 180, 180], fill=255)
            draw.ellipse([60, 60, 140, 140], fill=0)  # hole

        image_bytes = _make_mask_png(draw_fn=draw_donut)
        result = process_mask_to_ring(image_bytes)
        ring = result["ring"]
        # The ring should exist and be the outer boundary
        assert len(ring) >= 4

    def test_empty_mask_raises_value_error(self):
        """An all-black image should raise ValueError."""
        image_bytes = _make_mask_png(draw_fn=lambda img: None)
        with pytest.raises(ValueError, match="No contours found"):
            process_mask_to_ring(image_bytes)

    def test_ring_coordinates_are_float_pairs(self):
        image_bytes = _make_mask_png()
        result = process_mask_to_ring(image_bytes)
        for coord in result["ring"]:
            assert len(coord) == 2
            assert isinstance(coord[0], float)
            assert isinstance(coord[1], float)

    def test_undecodable_bytes_raises_value_error(self):
        """Random bytes that cv2 cannot decode should raise ValueError."""
        with pytest.raises(ValueError, match="Could not decode image"):
            process_mask_to_ring(b"not-a-real-image-at-all")

    def test_simplification_reduces_points(self):
        """A jagged contour should have fewer points after simplification."""
        def draw_jagged(img):
            draw = ImageDraw.Draw(img)
            # Draw a large jagged star shape with many vertices
            import math
            cx, cy, n = 200, 200, 60
            points = []
            for i in range(n):
                angle = 2 * math.pi * i / n
                r = 150 if i % 2 == 0 else 80
                points.append((cx + r * math.cos(angle), cy + r * math.sin(angle)))
            draw.polygon(points, fill=255)

        image_bytes = _make_mask_png(width=400, height=400, draw_fn=draw_jagged)

        # Count raw contour points (without simplification) by running
        # opencv directly
        import cv2
        import numpy as np
        arr = np.frombuffer(image_bytes, dtype=np.uint8)
        img = cv2.imdecode(arr, cv2.IMREAD_GRAYSCALE)
        _, binary = cv2.threshold(img, 127, 255, cv2.THRESH_BINARY)
        contours, _ = cv2.findContours(binary, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        raw_point_count = len(max(contours, key=cv2.contourArea).reshape(-1, 2))

        result = process_mask_to_ring(image_bytes)
        simplified_point_count = len(result["ring"])

        assert simplified_point_count < raw_point_count, (
            f"Simplification should reduce points: {simplified_point_count} >= {raw_point_count}"
        )
