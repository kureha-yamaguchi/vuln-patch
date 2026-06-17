"""Call an LLM to produce a fuzzing harness from a chat-completion prompt.

Supports four backends, selected automatically from the environment:
  - Azure OpenAI — set AZURE_OPENAI_API_KEY + AZURE_OPENAI_ENDPOINT +
    AZURE_OPENAI_DEPLOYMENT; uses the AzureOpenAI client with an api
    version. Takes precedence over the others.
  - OpenAI API (standard) — gpt-4o / gpt-4.1 etc.; accept temperature/top_p.
  - OpenAI API (reasoning) — GPT-5.x / o-series; reject custom sampling
    params and instead take a `reasoning_effort` knob.
  - Local server — Ollama or LM Studio; set LOCAL_LLM_BASE_URL +
    LOCAL_LLM_MODEL.

All four speak the same /v1/chat/completions protocol, so the only
real difference is which client to construct and which request
parameters are legal for the chosen model. `HarnessGenerator` resolves
that once at construction time.
"""
from typing import List, Dict, Optional

import openai
from openai import AzureOpenAI

import config


# OpenAI reasoning models reject `temperature`/`top_p` (only the default
# is allowed) and instead expose `reasoning_effort`. We detect them by
# name prefix so the same code path serves gpt-4o and gpt-5 alike. The
# check is intentionally permissive — an unknown model is treated as a
# standard sampling model, matching prior behaviour.
_REASONING_PREFIXES = ('gpt-5', 'o1', 'o3', 'o4', 'gpt-oss')


def _is_reasoning_model(model: str) -> bool:
    name = (model or '').lower()
    return any(name.startswith(p) for p in _REASONING_PREFIXES)


class HarnessGenerator:
    """Thin wrapper around the OpenAI / AzureOpenAI SDK.

    When use_azure is True the AzureOpenAI client is constructed from
    azure_endpoint + api_version, and `model` is the Azure deployment
    name. Otherwise: when base_url is None the SDK targets api.openai.com
    (real OpenAI); when base_url is set it targets that server — Ollama,
    LM Studio, etc. All modes use the same /v1/chat/completions protocol.

    For OpenAI reasoning models (GPT-5.x, o-series) `temperature` and
    `top_p` are omitted from the request because the API only accepts
    their default values; `reasoning_effort` is sent instead. For all
    other models the sampling params are passed through as before.
    """

    def __init__(self,
                 base_url: Optional[str] = config.LOCAL_LLM_BASE_URL,
                 api_key: str = config.LOCAL_LLM_API_KEY,
                 model: str = config.LOCAL_LLM_MODEL,
                 temperature: float = 0.6,
                 top_p: float = 1.0,
                 reasoning_effort: str = config.OPENAI_REASONING_EFFORT,
                 max_completion_tokens: int = config.OPENAI_MAX_COMPLETION_TOKENS,
                 use_azure: bool = config.USE_AZURE,
                 azure_endpoint: Optional[str] = config.AZURE_OPENAI_ENDPOINT,
                 azure_api_version: str = config.AZURE_OPENAI_API_VERSION):
        if use_azure:
            # Azure OpenAI: the SDK needs azure_endpoint + api_version, and
            # the request's `model` field is the *deployment* name.
            self._client = AzureOpenAI(
                api_key=api_key,
                azure_endpoint=azure_endpoint,
                api_version=azure_api_version,
            )
        else:
            kwargs: dict = {'api_key': api_key}
            if base_url is not None:
                kwargs['base_url'] = base_url
            self._client = openai.OpenAI(**kwargs)
        self.use_azure = use_azure
        self.model = model
        self.temperature = temperature
        self.top_p = top_p
        self.reasoning_effort = reasoning_effort
        self.max_completion_tokens = max_completion_tokens
        self._reasoning = _is_reasoning_model(model)

    def generate(self, messages: List[Dict[str, str]]) -> str:
        """Send the chat-completion request and return the assistant's
        textual response."""
        params: dict = {
            'messages': messages,
            'model': self.model,
        }
        if self._reasoning:
            # Reasoning models: no temperature/top_p; steer with effort.
            if self.reasoning_effort:
                params['reasoning_effort'] = self.reasoning_effort
            # Reasoning tokens count against the output budget, so give a
            # generous cap to avoid truncated/empty harnesses.
            if self.max_completion_tokens:
                params['max_completion_tokens'] = self.max_completion_tokens
        else:
            params['temperature'] = self.temperature
            params['top_p'] = self.top_p

        result = self._client.chat.completions.create(**params)
        return result.choices[0].message.content