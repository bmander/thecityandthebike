# The City And The Bike (TCATB)

A mobile app for discovering, photographing, and cataloging graffiti tags on the rear fenders of rental bikes.

## Quick Start

```bash
docker-compose up -d --build    # start PostgreSQL + API
curl http://localhost:5000/health  # verify API is running
```

Optionally configure secrets first (`cp .env.example .env`), otherwise dev defaults are used.

To stop: `docker-compose down`

## Component Documentation

- [API](api/README.md) -- local development, environment variables, and endpoints
- [Android App](android/README.md) -- building and running
- [Code Coverage Report](http://www.thecityandthebike.com/coverage/) -- backend test coverage
