from app.bike_url_parser import WHITELISTED_PROVIDERS, parse_bike_url


class TestWhitelistedProviders:
    """Tests for WHITELISTED_PROVIDERS constant."""

    def test_whitelisted_providers_contains_expected_values(self):
        assert WHITELISTED_PROVIDERS == {"lime", "bird"}


class TestParseBikeUrl:
    """Tests for parse_bike_url function."""

    def test_lime_url(self):
        result = parse_bike_url("https://lime.bike/bc/v1/G5EZAYI=")
        assert result.provider == "lime"
        assert result.bike_id == "G5EZAYI"

    def test_lime_url_multiple_padding(self):
        result = parse_bike_url("https://lime.bike/bc/v1/ABC==")
        assert result.provider == "lime"
        assert result.bike_id == "ABC"

    def test_lime_url_no_padding(self):
        result = parse_bike_url("https://lime.bike/bc/v1/G5EZAYI")
        assert result.provider == "lime"
        assert result.bike_id == "G5EZAYI"

    def test_bird_url(self):
        result = parse_bike_url("https://ride.bird.co/bc/v1/abc123")
        assert result.provider == "bird"
        assert result.bike_id == "abc123"

    def test_unknown_url(self):
        result = parse_bike_url("https://unknown-provider.com/bikes/XYZ")
        assert result.provider is None
        assert result.bike_id == "https://unknown-provider.com/bikes/XYZ"

    def test_plain_string(self):
        result = parse_bike_url("BIKE-001")
        assert result.provider is None
        assert result.bike_id == "BIKE-001"

    def test_empty_string(self):
        result = parse_bike_url("")
        assert result.provider is None
        assert result.bike_id == ""

    def test_url_with_no_path(self):
        result = parse_bike_url("https://lime.bike/")
        assert result.provider is None
        assert result.bike_id == "https://lime.bike/"

    def test_http_lime_url(self):
        result = parse_bike_url("http://lime.bike/bc/v1/G5EZAYI=")
        assert result.provider == "lime"
        assert result.bike_id == "G5EZAYI"
