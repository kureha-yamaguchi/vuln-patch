"""LLM-backed classifier shared by Phase 1 (variant-pair verification)
and Phase 3 (corpus-wide edge classification).

Wraps the OpenAI SDK with three concerns the rest of the project shouldn't
care about:

  - **Disk cache** of every response keyed by sha256(model + prompt). Re-runs
    of the same prompt return instantly with zero spend.
  - **Budget tracking**. Spend is summed from each response's usage object
    against config.LLM_MAX_BUDGET_USD. Once the cap is exceeded the
    classifier refuses to call the API; callers can detect this via the
    raised `BudgetExceededError` and label the record accordingly.
  - **Structured output**. The caller hands in a JSON schema; the SDK
    enforces it and we parse the response into a dict before returning.
"""
import hashlib
import json
import os
from dataclasses import dataclass
from typing import Any, Dict, List, Optional

import openai

from . import config


class BudgetExceededError(RuntimeError):
    """Raised by Classifier.classify when the cumulative spend would
    exceed LLM_MAX_BUDGET_USD. The caller is expected to catch this and
    label the record as unclassified."""


@dataclass
class ClassifierDecision:
    """Returned from Classifier.classify. `parsed` is the validated JSON
    body; `usage` lets callers report total tokens used."""
    parsed: Dict[str, Any]
    prompt_tokens: int
    completion_tokens: int
    cost_usd: float
    cached: bool

    def as_dict(self) -> dict:
        return {
            'parsed': self.parsed,
            'prompt_tokens': self.prompt_tokens,
            'completion_tokens': self.completion_tokens,
            'cost_usd': self.cost_usd,
            'cached': self.cached,
        }


class Classifier:
    """One client, one budget. Instantiate once per run and reuse."""

    def __init__(self,
                 model: str = config.LLM_CLASSIFIER_MODEL,
                 cache_dir: str = config.LLM_CACHE_DIR,
                 max_budget_usd: float = config.LLM_MAX_BUDGET_USD,
                 price_in_per_mtok: float = config.LLM_PRICE_IN_USD_PER_MTOK,
                 price_out_per_mtok: float = config.LLM_PRICE_OUT_USD_PER_MTOK):
        # OpenAI SDK reads OPENAI_API_KEY from the environment when no
        # api_key is passed — keeps the secret out of code/config files.
        self._client = openai.OpenAI()
        self.model = model
        self.cache_dir = cache_dir
        self.max_budget_usd = max_budget_usd
        self.price_in_per_mtok = price_in_per_mtok
        self.price_out_per_mtok = price_out_per_mtok
        os.makedirs(self.cache_dir, exist_ok=True)
        self._spend_usd = 0.0
        self._calls_live = 0
        self._calls_cached = 0

    # ------------------------------------------------------------------
    # public API

    def classify(self,
                 system_prompt: str,
                 user_prompt: str,
                 schema: Dict[str, Any],
                 schema_name: str = 'classification') -> ClassifierDecision:
        """Send a structured-output chat completion. Returns the parsed
        JSON body. Raises BudgetExceededError if the next call would
        exceed the budget."""
        key = self._cache_key(system_prompt, user_prompt, schema_name)
        cached = self._read_cache(key)
        if cached is not None:
            self._calls_cached += 1
            return ClassifierDecision(
                parsed=cached['parsed'],
                prompt_tokens=cached.get('prompt_tokens', 0),
                completion_tokens=cached.get('completion_tokens', 0),
                cost_usd=cached.get('cost_usd', 0.0),
                cached=True,
            )

        # Budget guard before the spend, not after — once we're over, stop.
        if self._spend_usd >= self.max_budget_usd:
            raise BudgetExceededError(
                f"LLM budget ${self.max_budget_usd:.2f} reached "
                f"(spent ${self._spend_usd:.4f})"
            )

        resp = self._client.chat.completions.create(
            model=self.model,
            messages=[
                {'role': 'system', 'content': system_prompt},
                {'role': 'user',   'content': user_prompt},
            ],
            response_format={
                'type': 'json_schema',
                'json_schema': {
                    'name': schema_name,
                    'strict': True,
                    'schema': schema,
                },
            },
        )

        raw = resp.choices[0].message.content
        parsed = json.loads(raw)
        usage = resp.usage
        prompt_tokens = getattr(usage, 'prompt_tokens', 0) or 0
        completion_tokens = getattr(usage, 'completion_tokens', 0) or 0
        cost = self._cost_for(prompt_tokens, completion_tokens)

        self._spend_usd += cost
        self._calls_live += 1

        record = {
            'parsed': parsed,
            'prompt_tokens': prompt_tokens,
            'completion_tokens': completion_tokens,
            'cost_usd': cost,
        }
        self._write_cache(key, record)

        return ClassifierDecision(
            parsed=parsed,
            prompt_tokens=prompt_tokens,
            completion_tokens=completion_tokens,
            cost_usd=cost,
            cached=False,
        )

    @property
    def spend_usd(self) -> float:
        return self._spend_usd

    @property
    def calls(self) -> Dict[str, int]:
        return {'live': self._calls_live, 'cached': self._calls_cached}

    # ------------------------------------------------------------------
    # internals

    def _cost_for(self, prompt_tokens: int, completion_tokens: int) -> float:
        return (
            prompt_tokens    * self.price_in_per_mtok  / 1_000_000.0
            + completion_tokens * self.price_out_per_mtok / 1_000_000.0
        )

    def _cache_key(self, system_prompt: str, user_prompt: str,
                   schema_name: str) -> str:
        h = hashlib.sha256()
        h.update(self.model.encode('utf-8'))
        h.update(b'\x00')
        h.update(schema_name.encode('utf-8'))
        h.update(b'\x00')
        h.update(system_prompt.encode('utf-8'))
        h.update(b'\x00')
        h.update(user_prompt.encode('utf-8'))
        return h.hexdigest()

    def _cache_path(self, key: str) -> str:
        return os.path.join(self.cache_dir, key + '.json')

    def _read_cache(self, key: str) -> Optional[Dict[str, Any]]:
        path = self._cache_path(key)
        if not os.path.isfile(path):
            return None
        try:
            with open(path, 'r', encoding='utf-8') as f:
                return json.load(f)
        except (OSError, json.JSONDecodeError):
            # Corrupt cache entry — treat as miss; the live call will
            # overwrite it.
            return None

    def _write_cache(self, key: str, record: Dict[str, Any]) -> None:
        path = self._cache_path(key)
        tmp = path + '.tmp'
        with open(tmp, 'w', encoding='utf-8') as f:
            json.dump(record, f)
        os.replace(tmp, path)
