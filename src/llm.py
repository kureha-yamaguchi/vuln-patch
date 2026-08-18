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

The response can be streamed (controlled by `config.LLM_STREAM`). For
slow reasoning models a synchronous call can sit silent for minutes
during the hidden thinking phase, and an intermediary often kills that
"idle" connection before any bytes return — surfacing as
RemoteProtocolError -> APIConnectionError even though the model was
still producing tokens. Streaming keeps the socket active and avoids
that drop; by default we stream only for reasoning models.
"""
from typing import List, Dict, Optional

import httpx
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


# --- token accounting --------------------------------------------------------
# Every generate() call (harness generation, relation verifier, relation
# synthesis) records its usage here, keyed by model, so a run can report the
# exact prompt/completion tokens it spent — the input to any cost estimate.
# Process-global so the several HarnessGenerator instances a run creates
# (primary + escalation + verifier + synth) all accumulate into one place.
_USAGE_BY_MODEL: Dict[str, Dict[str, int]] = {}


def _record_usage(model: str, usage) -> None:
    if usage is None:
        return
    slot = _USAGE_BY_MODEL.setdefault(
        model or 'unknown',
        {'prompt_tokens': 0, 'cached_prompt_tokens': 0,
         'completion_tokens': 0, 'total_tokens': 0, 'calls': 0})
    slot['prompt_tokens'] += int(getattr(usage, 'prompt_tokens', 0) or 0)
    # How much of that input the provider served from its prompt cache.
    # `prompt_tokens` counts cached tokens at full count, so without this the
    # only cost figure available is an upper bound. Absent on backends that
    # do not report it, which reads as zero.
    _details = getattr(usage, 'prompt_tokens_details', None)
    slot['cached_prompt_tokens'] += int(
        getattr(_details, 'cached_tokens', 0) or 0)
    slot['completion_tokens'] += int(
        getattr(usage, 'completion_tokens', 0) or 0)
    slot['total_tokens'] += int(getattr(usage, 'total_tokens', 0) or 0)
    slot['calls'] += 1


def token_usage() -> Dict[str, Dict[str, int]]:
    """Per-model cumulative token usage since the last reset (a deep copy)."""
    return {m: dict(v) for m, v in _USAGE_BY_MODEL.items()}


def reset_token_usage() -> None:
    _USAGE_BY_MODEL.clear()


# --- Full pipeline transcript (ordered LLM + deterministic events) ----------
# One process-global ordered log so a run can dump EVERY step of the whole
# pipeline: each LLM call (which model, its full prompt, its full output — all
# funnel through HarnessGenerator.generate below) AND each deterministic
# decision (which method, its output). Opt-in (off by default = no cost).
_EVENTS: List[Dict] = []
_RECORD = False


def enable_recording() -> None:
    global _RECORD
    _RECORD = True


def reset_events() -> None:
    _EVENTS.clear()


def get_events() -> List[Dict]:
    """Every recorded event in call order. LLM events:
    {seq, kind:'llm', model, messages, output}. Deterministic events:
    {seq, kind:'deterministic', method, ...detail..., output}."""
    return list(_EVENTS)


def record_event(kind: str, **fields) -> None:
    """Append one ordered pipeline event (LLM or deterministic)."""
    if not _RECORD:
        return
    try:
        _EVENTS.append({'seq': len(_EVENTS), 'kind': kind, **fields})
    except Exception:
        pass


def _record_llm_call(model, messages, output) -> None:
    record_event('llm', model=model, messages=messages, output=output)


def usage_totals() -> Dict[str, int]:
    """Summed prompt/completion/total tokens and call count across models."""
    out = {'prompt_tokens': 0, 'cached_prompt_tokens': 0,
           'completion_tokens': 0, 'total_tokens': 0, 'calls': 0}
    for v in _USAGE_BY_MODEL.values():
        for k in out:
            out[k] += v.get(k, 0)
    return out


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
                 azure_api_version: str = config.AZURE_OPENAI_API_VERSION,
                 timeout: float = config.LLM_TIMEOUT_SECONDS,
                 max_retries: int = config.LLM_MAX_RETRIES,
                 stream: str = config.LLM_STREAM):
        # Generous timeout + retries: reasoning models can be slow to
        # respond, and proxies may drop idle connections mid-call. Both
        # the OpenAI and AzureOpenAI clients accept these and the SDK
        # retries connection errors and 5xx/429 automatically.
        if use_azure:
            # Azure OpenAI: the SDK needs azure_endpoint + api_version, and
            # the request's `model` field is the *deployment* name.
            self._client = AzureOpenAI(
                api_key=api_key,
                azure_endpoint=azure_endpoint,
                api_version=azure_api_version,
                timeout=timeout,
                max_retries=max_retries,
                http_client=httpx.Client(
                    limits=httpx.Limits(keepalive_expiry=30),
                ),
            )
        else:
            kwargs: dict = {
                'api_key': api_key,
                'timeout': timeout,
                'max_retries': max_retries,
            }
            if base_url is not None:
                kwargs['base_url'] = base_url
            self._client = openai.OpenAI(**kwargs)
        self.use_azure = use_azure
        self.model = model
        self.temperature = temperature
        self.top_p = top_p
        self.reasoning_effort = reasoning_effort
        self.max_completion_tokens = max_completion_tokens
        self.timeout = timeout
        self.max_retries = max_retries
        self._reasoning = _is_reasoning_model(model)
        # Resolve the streaming policy once. 'auto' streams reasoning
        # models only (that is where the long silent thinking phase
        # provokes the idle-connection drop); 'always'/'never' force it.
        _s = (stream or 'auto').strip().lower()
        if _s in ('always', '1', 'true', 'yes', 'on'):
            self._stream = True
        elif _s in ('never', '0', 'false', 'no', 'off'):
            self._stream = False
        else:  # 'auto' or anything unrecognised
            self._stream = self._reasoning

    def generate(self, messages: List[Dict[str, str]]) -> str:
        """Send the chat-completion request and return the assistant's
        textual response. Streams the response when self._stream is set
        (see config.LLM_STREAM)."""
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

        if not self._stream:
            result = self._client.chat.completions.create(**params)
            _record_usage(self.model, getattr(result, 'usage', None))
            out = result.choices[0].message.content
            _record_llm_call(self.model, messages, out)
            return out

        # Streaming path: accumulate content deltas as they arrive. The
        # constant traffic keeps proxies/gateways from treating the
        # connection as idle and dropping it mid-generation.
        params['stream'] = True
        # Ask the API to emit a final usage-only chunk so streamed calls are
        # accounted for too (it arrives as an event with empty choices).
        params['stream_options'] = {'include_usage': True}
        chunks: List[str] = []
        stream = self._client.chat.completions.create(**params)
        try:
            for event in stream:
                # Usage-only or keep-alive events can carry no choices.
                if not event.choices:
                    _record_usage(self.model, getattr(event, 'usage', None))
                    continue
                delta = event.choices[0].delta
                if delta is not None and delta.content:
                    chunks.append(delta.content)
        finally:
            # Ensure the underlying HTTP response is released even if the
            # caller stops consuming or an error interrupts the loop.
            close = getattr(stream, 'close', None)
            if close is not None:
                close()
        out = ''.join(chunks)
        _record_llm_call(self.model, messages, out)
        return out