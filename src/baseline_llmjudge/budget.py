"""Turn the recorded token usage into a disclosable budget.

Two rules this module exists to enforce:

  1. No invented prices. A price comes from the environment or the report says
     the price was not set. A cost figure with a guessed rate behind it is
     worse than no cost figure.
  2. Cached input is stated, never hidden. All samples of one patch send a
     byte-identical prompt, so the provider serves most of that input from its
     prompt cache. `prompt_tokens` still counts those tokens in full, so the
     full-rate figure is an upper bound on the invoice. When a cached-input
     rate is set, the report carries the discounted figure beside it.

Set the rates for the model you actually ran:

    export LLMJUDGE_PRICE_IN_USD_PER_MTOK=...
    export LLMJUDGE_PRICE_OUT_USD_PER_MTOK=...
    export LLMJUDGE_PRICE_CACHED_IN_USD_PER_MTOK=...   # optional
"""
import os
from typing import Dict, Optional


def _price(name: str) -> Optional[float]:
    raw = os.getenv(name)
    if raw is None or not raw.strip():
        return None
    return float(raw)


def prices() -> Dict[str, Optional[float]]:
    return {
        'in_usd_per_mtok': _price('LLMJUDGE_PRICE_IN_USD_PER_MTOK'),
        'out_usd_per_mtok': _price('LLMJUDGE_PRICE_OUT_USD_PER_MTOK'),
        'cached_in_usd_per_mtok': _price(
            'LLMJUDGE_PRICE_CACHED_IN_USD_PER_MTOK'),
    }


def report(tokens: Dict[str, int]) -> Dict:
    """The budget block for one patch, or for a whole side.

    `tokens` is a `llm.usage_totals()`-shaped dict: prompt_tokens,
    completion_tokens, cached_prompt_tokens, total_tokens, calls."""
    p = prices()
    prompt = int(tokens.get('prompt_tokens', 0) or 0)
    completion = int(tokens.get('completion_tokens', 0) or 0)
    cached = int(tokens.get('cached_prompt_tokens', 0) or 0)
    out: Dict = {
        'calls': int(tokens.get('calls', 0) or 0),
        'prompt_tokens': prompt,
        'cached_prompt_tokens': cached,
        'completion_tokens': completion,
        'total_tokens': int(tokens.get('total_tokens', 0) or 0),
        'cache_hit_rate': (cached / prompt) if prompt else None,
        'prices_usd_per_mtok': p,
        'cost_usd_full_rate': None,
        'cost_usd_with_cache_rate': None,
        'note': None,
    }
    if p['in_usd_per_mtok'] is None or p['out_usd_per_mtok'] is None:
        out['note'] = ('price not set — tokens only. Set '
                       'LLMJUDGE_PRICE_IN_USD_PER_MTOK and '
                       'LLMJUDGE_PRICE_OUT_USD_PER_MTOK for a cost figure.')
        return out

    out_cost = completion * p['out_usd_per_mtok'] / 1e6
    out['cost_usd_full_rate'] = (
        prompt * p['in_usd_per_mtok'] / 1e6 + out_cost)
    if p['cached_in_usd_per_mtok'] is not None:
        out['cost_usd_with_cache_rate'] = (
            (prompt - cached) * p['in_usd_per_mtok'] / 1e6
            + cached * p['cached_in_usd_per_mtok'] / 1e6
            + out_cost)
        out['note'] = ('cost_usd_full_rate bills every prompt token at the '
                       'uncached rate and is an upper bound; '
                       'cost_usd_with_cache_rate is the closer estimate.')
    else:
        out['note'] = ('cost_usd_full_rate bills every prompt token at the '
                       'uncached rate, so it is an upper bound. Set '
                       'LLMJUDGE_PRICE_CACHED_IN_USD_PER_MTOK for the '
                       'discounted figure.')
    return out


def add(a: Dict[str, int], b: Dict[str, int]) -> Dict[str, int]:
    """Sum two `usage_totals()`-shaped dicts."""
    keys = ('prompt_tokens', 'cached_prompt_tokens', 'completion_tokens',
            'total_tokens', 'calls')
    return {k: int(a.get(k, 0) or 0) + int(b.get(k, 0) or 0) for k in keys}
