from dataclasses import dataclass
from typing import Optional
from urllib.parse import urlparse


WHITELISTED_PROVIDERS = {"lime", "bird"}


@dataclass
class ParsedBikeUrl:
    provider: Optional[str]
    bike_id: str


def parse_bike_url(raw: str) -> ParsedBikeUrl:
    """Parse a bike QR code value into a provider and bike ID.

    Rules:
    - lime.bike hostname → provider="lime", id=last path segment with '=' padding stripped
    - ride.bird.co hostname → provider="bird", id=last path segment
    - Anything else → provider=None, id=raw value as-is
    """
    try:
        parsed = urlparse(raw)
        if not parsed.scheme or not parsed.hostname:
            return ParsedBikeUrl(provider=None, bike_id=raw)
    except Exception:
        return ParsedBikeUrl(provider=None, bike_id=raw)

    hostname = parsed.hostname
    path_segments = [s for s in parsed.path.split("/") if s]

    if not path_segments:
        return ParsedBikeUrl(provider=None, bike_id=raw)

    last_segment = path_segments[-1]

    if hostname == "lime.bike":
        return ParsedBikeUrl(provider="lime", bike_id=last_segment.rstrip("="))
    elif hostname == "ride.bird.co":
        return ParsedBikeUrl(provider="bird", bike_id=last_segment)
    else:
        return ParsedBikeUrl(provider=None, bike_id=raw)
