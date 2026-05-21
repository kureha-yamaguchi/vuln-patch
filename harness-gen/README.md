## Dependencies

`brew install llvm`

install `uv`

Run with local gpt-oss-20b model

# 1. Install
`brew install ollama`

# 2. Start the server (leave running in a terminal)
`ollama serve`

# 3. In another terminal, pull the model
`ollama pull gpt-oss:20b`

# If needed
`pip install --upgrade openai`

# 4. Run your script 
`uv run -m run <language> <target_dir> <target_function>`


![Minimal LLM-based fuzz harness generator](https://adalogics.com/blog/minimal-llm-based-fuzz-harness-generator)

![Fuzz Introspector: enabling rapid fuzz introspection tool development](https://adalogics.com/blog/fuzz-introspection-as-python-library)

## Dependencies

`git clone git@github.com:ASSERT-KTH/drr.git`
`git clone git@github.com:rjust/defects4j.git`
`cd defects4j`
`git reset --hard 486e2b49d806cdd3288a64ee3c10b3a25632e991`

## Key changes to run.py

extract_data_about_target(language, patch_path, buggy_dir) now does three things in order: parses the patch to collect modified-file paths and candidate function names (from the @@ ... @@ header tail when available, plus any identifier on +/- lines); runs fi_commands.analyse_end_to_end once on buggy_dir; then, for each candidate the project actually knows about, pulls source + xrefs the same way the original single-function code did. Unknown names are filtered out automatically because find_function_by_name returns None for them, which keeps Java keywords and non-project identifiers out of the result. The returned dict has modified_files, patch_text, and a functions list with one entry per touched function (each with func_name, func_signature, func_source, xrefs).

create_prompt_from_data(language, patch_path, buggy_dir, context) takes the same three semantic inputs plus the context dict (it would be wasteful to re-run the analyser inside it). It uses os.path.basename(buggy_dir) as the codebase name (replacing the old target_dir), embeds the patch verbatim in a <patch> block, and emits one <signature>/<code>/<xref> block per touched function — the same layout as before, just iterated.

main() now calls create_prompt_from_data(args.language, patch_path, buggy_dir, context). One caveat worth knowing: the +/- line scan is deliberately loose, so a change like return foo(x) will also surface foo as a candidate. In practice that's helpful (called helpers are good context for a harness), and the project-lookup filter keeps the noise low. If you find it too broad later, the cheapest tightening is to drop the +/- scan and rely on the @@ tail only.