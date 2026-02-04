# Claude Code Guidelines

## Virtual Environment

If a virtual environment doesn't exist, create one:

```bash
cd api && python -m venv venv && source venv/bin/activate && pip install -r requirements.txt
```

## Testing

Always run tests with the `-x` flag to stop on the first error:

```bash
cd api && source venv/bin/activate && pytest -x
```

This prevents overwhelming output when there are multiple failures from the same root cause.
