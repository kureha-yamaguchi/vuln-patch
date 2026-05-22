"""Call a local OpenAI-compatible LLM (Ollama or LM Studio) to produce a
Jazzer harness from a chat-completion prompt."""
from typing import List, Dict

import openai

import config


class HarnessGenerator:
    """Thin wrapper around the OpenAI SDK pointed at a local server.

    Both Ollama (port 11434) and LM Studio (port 1234) speak the
    /v1/chat/completions protocol, so swapping between them is just a
    matter of changing the base URL — no code changes required.
    """

    def __init__(self,
                 base_url: str = config.LOCAL_LLM_BASE_URL,
                 api_key: str = config.LOCAL_LLM_API_KEY,
                 model: str = config.LOCAL_LLM_MODEL,
                 temperature: float = 1.0,
                 top_p: float = 1.0):
        self._client = openai.OpenAI(base_url=base_url, api_key=api_key)
        self.model = model
        # gpt-oss-20b recommended sampling: temperature=1.0, top_p=1.0.
        # Lower temperature if you want more deterministic harnesses.
        self.temperature = temperature
        self.top_p = top_p

    def generate(self, messages: List[Dict[str, str]]) -> str:
        """Send the chat-completion request and return the assistant's
        textual response."""
        result = self._client.chat.completions.create(
            messages=messages,
            model=self.model,
            temperature=self.temperature,
            top_p=self.top_p,
        )
        return result.choices[0].message.content