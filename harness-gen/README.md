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

Example usage:

```
# Get target codebase
git clone https://gitlab.com/codesun/jakens
uv run -m run c++ ./jakens Json_parseFromFile
```




![Minimal LLM-based fuzz harness generator](https://adalogics.com/blog/minimal-llm-based-fuzz-harness-generator)

![Fuzz Introspector: enabling rapid fuzz introspection tool development](https://adalogics.com/blog/fuzz-introspection-as-python-library)