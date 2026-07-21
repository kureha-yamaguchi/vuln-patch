# labels/crashing/ — verified labels for CRASHING bugs (kept OUT of the semantic lists)

These are the same schema as `../verified_*.jsonl` but for bugs whose trigger
test THROWS (crashing), not asserts (semantic). They were certified alongside
the semantic sweep but moved here so the semantic lists (`../verified_*.jsonl`)
and `DATASET_AUDIT.md` §2/§3 stay strictly semantic (the detection pipeline and
eval are semantic).

Crashing bugs here: Chart-5, Chart-9, Lang-6, Lang-16, Lang-20, Lang-27, Lang-39,
Lang-43, Lang-44, Lang-45, Lang-51, Lang-58, Lang-59, Math-32, Math-58, Math-70,
Math-79, Math-85. The crashing pool is NOT fully swept — see DATASET_AUDIT §7.
