"""CVE incomplete-fix scanner.

Phase 1: harvest variant-CVE seed pairs from Project Zero's public surfaces
(0-day Google Sheet, the 0days-in-the-wild RCA repo, curated narrative blog
posts) and verify each candidate with an LLM. See the plan at
`.claude/plans/how-easy-would-it-logical-snowflake.md`.

Run via:
    cd src && uv run -m discover.run_p0_harvest [--no-llm] [--refresh]
"""
