import cv2
import numpy as np
from shapely.geometry import Polygon


def process_mask_to_ring(image_bytes: bytes) -> dict:
    """Process a binary mask image into a single exterior ring geometry.

    Args:
        image_bytes: Raw bytes of a PNG mask image.

    Returns:
        Dict with keys ``ring`` (list of [x, y] coordinate pairs),
        ``width`` and ``height`` of the source image.

    Raises:
        ValueError: If no contours are found in the mask.
    """
    arr = np.frombuffer(image_bytes, dtype=np.uint8)
    img = cv2.imdecode(arr, cv2.IMREAD_GRAYSCALE)
    if img is None:
        raise ValueError("Could not decode image")

    height, width = img.shape[:2]

    _, binary = cv2.threshold(img, 127, 255, cv2.THRESH_BINARY)

    contours, _ = cv2.findContours(binary, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

    if not contours:
        raise ValueError("No contours found in mask")

    largest = max(contours, key=cv2.contourArea)

    coords = largest.reshape(-1, 2).tolist()
    polygon = Polygon(coords)
    simplified = polygon.simplify(tolerance=2.0, preserve_topology=True)

    ring = [list(coord) for coord in simplified.exterior.coords]

    return {
        "ring": ring,
        "width": width,
        "height": height,
    }
