"""Call an LLM to produce a fuzzing harness from a chat-completion prompt.

Supports two backends, selected automatically from the environment:
  - OpenAI API    — set OPENAI_API_KEY; model defaults to gpt-4o
  - Local server  — Ollama or LM Studio; set LOCAL_LLM_BASE_URL + LOCAL_LLM_MODEL
"""
from typing import List, Dict, Optional

import openai

import config


class HarnessGenerator:
    """Thin wrapper around the OpenAI SDK.

    When base_url is None the SDK targets api.openai.com (real OpenAI).
    When base_url is set it targets that server — Ollama, LM Studio, etc.
    Both modes use the same /v1/chat/completions protocol.
    """

    def __init__(self,
                 base_url: Optional[str] = config.LOCAL_LLM_BASE_URL,
                 api_key: str = config.LOCAL_LLM_API_KEY,
                 model: str = config.LOCAL_LLM_MODEL,
                 temperature: float = 0.6,
                 top_p: float = 1.0):
        kwargs: dict = {'api_key': api_key}
        if base_url is not None:
            kwargs['base_url'] = base_url
        self._client = openai.OpenAI(**kwargs)
        self.model = model
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