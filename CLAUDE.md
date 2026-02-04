# Claude Code Guidelines

## Testing

Always run tests with the `-x` flag to stop on the first error:

```bash
source venv/bin/activate && pytest -x
```

This prevents overwhelming output when there are multiple failures from the same root cause.
