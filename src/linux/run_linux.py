"""Generate and verify Linux kernel harnesses for a CVE sibling pair.

Pipeline:
    analysis_linux  build LinuxPatchContext from the DB
    prompts_linux   build chat-completion prompt
    llm             call the local LLM (shared with Java pipeline)
    build_linux     extract + gcc/clang compile the generated C harness
    verify_linux    run against 3 kernel states (pre_fix / fix0 / fix1)

Repeats until target_successes harnesses are both compiled AND
ground-truth confirmed (triggers on pre_fix + fix0, clean on fix1),
or max_attempts is reached.

Usage:
    cd src && uv run linux/run_linux.py --pair CVE-2011-2482__CVE-2011-4348
    cd src && uv run linux/run_linux.py --pair CVE-2016-9576__CVE-2016-10088 \\
        --target_successes 3 --max_attempts 30 --verify_timeout 60
"""
import argparse
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))

import config
from llm import HarnessGenerator
from linux.analysis_linux import build_context
from linux.build_linux import build, LinuxBuildResult
from linux.prompts_linux import build as build_prompt, build_repair
from linux.verify_linux import HarnessVerifier, VerifyResult


def _checkout_path(pair_name: str, state: str) -> str | None:
    """Return the worktree path for a given state, or None if it doesn't exist."""
    p = Path(config.LINUX_CHECKOUT_ROOT) / pair_name / state
    return str(p) if p.exists() else None


def _print_summary(
    pair_name: str,
    attempts: int,
    build_successes: list[LinuxBuildResult],
    verified: list[VerifyResult],
) -> None:
    print()
    print("=" * 60)
    print(f"Pair:             {pair_name}")
    print(f"Attempts:         {attempts}")
    print(f"Compiled:         {len(build_successes)}")
    print(f"Ground-truth:     {len(verified)}")
    if verified:
        print("\nConfirmed harnesses:")
        for v in verified:
            print(v.summary())
    print("=" * 60)


def run(args: argparse.Namespace) -> None:
    prior_cve, later_cve = args.pair.split("__")

    # --- Load context ---------------------------------------------------
    ctx = build_context(prior_cve, later_cve)

    if ctx.metadata.get("fuzzing_excluded"):
        print(f"Pair {args.pair} is marked fuzzing_excluded (no fix1 commit). Exiting.")
        sys.exit(1)

    model = args.model or config.LOCAL_LLM_MODEL
    backend = "OpenAI API" if config.LOCAL_LLM_BASE_URL is None else config.LOCAL_LLM_BASE_URL
    print(f"Pair:      {args.pair}")
    print(f"Subsystem: {ctx.subsystem}")
    print(f"Style:     {ctx.harness_style}")
    print(f"PoC:       {'available' if ctx.poc_available else 'not found publicly'}")
    print(f"LLM:       {model}  via {backend}")
    print()

    # --- Resolve checkout paths -----------------------------------------
    pre_fix_path = _checkout_path(args.pair, "fix0_parent")
    fix0_path    = _checkout_path(args.pair, "fix0")
    fix1_path    = _checkout_path(args.pair, "fix1")

    if not fix0_path:
        print(f"WARNING: fix0 checkout not found at "
              f"{config.LINUX_CHECKOUT_ROOT}/{args.pair}/fix0")
        print("Run: python3 cve_sibling_db_linux/checkout_pair.py "
              f"--pair {args.pair}")
    if not pre_fix_path:
        print(f"WARNING: fix0_parent checkout not found. "
              "Harness validity check (pre_fix state) will be skipped.")
        print("Run: python3 cve_sibling_db_linux/checkout_pair.py "
              f"--pair {args.pair} --pre-fix")

    verifier = HarnessVerifier(
        pre_fix_checkout=pre_fix_path,
        fix0_checkout=fix0_path,
        fix1_checkout=fix1_path,
        timeout=args.verify_timeout,
    )

    # --- Output directory -----------------------------------------------
    out_dir = Path(config.LINUX_CHECKOUT_ROOT) / args.pair / "harnesses"
    out_dir.mkdir(parents=True, exist_ok=True)

    # --- Campaign loop --------------------------------------------------
    llm = HarnessGenerator(model=args.model or config.LOCAL_LLM_MODEL)
    messages = build_prompt(ctx)

    attempts = 0
    repair_failures = 0
    build_successes: list[LinuxBuildResult] = []
    verified: list[VerifyResult] = []

    while (
        len(verified) < args.target_successes
        and attempts < args.max_attempts
    ):
        attempts += 1
        label = f"attempt_{attempts:03d}"
        print(f"[{label}] generating ...", end=" ", flush=True)

        response = llm.generate(messages)

        # Build
        result = build(
            llm_response=response,
            attempt_label=label,
            harness_style=ctx.harness_style,
            kernel_checkout=fix0_path,
            output_dir=str(out_dir),
        )

        if not result.success:
            print(f"compile FAIL")
            repair_failures += 1
            if repair_failures <= args.max_repair_failures:
                messages = build_repair(ctx, messages, result.compiler_stderr)
            else:
                # Reset to original prompt after too many repair failures
                messages = build_prompt(ctx)
                repair_failures = 0
            continue

        # Compiled successfully
        repair_failures = 0
        build_successes.append(result)
        messages = build_prompt(ctx)  # fresh prompt for next attempt
        print(f"compile OK → {result.binary_path}", end=" ", flush=True)

        if not args.no_verify:
            vr = verifier.verify(result.binary_path, label)
            print(f"  pre_fix={'CRASH' if vr.pre_fix and vr.pre_fix.triggered else 'CLEAN'}"
                  f"  fix0={'CRASH' if vr.fix0 and vr.fix0.triggered else 'CLEAN'}"
                  f"  fix1={'CRASH' if vr.fix1 and vr.fix1.triggered else 'CLEAN'}")
            if vr.ground_truth_confirmed:
                verified.append(vr)
                print(f"  ✓ ground-truth confirmed ({len(verified)}/{args.target_successes})")
        else:
            # Skip verification — count every compiled harness as success
            from linux.verify_linux import VerifyResult
            verified.append(VerifyResult(
                harness_path=result.binary_path,
                attempt_label=label,
                pre_fix=None, fix0=None, fix1=None,
            ))
            print()

    _print_summary(args.pair, attempts, build_successes, verified)


def main() -> None:
    parser = argparse.ArgumentParser(
        description=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument("--pair", required=True,
                        help="CVE pair name, e.g. CVE-2011-2482__CVE-2011-4348")
    parser.add_argument("--target_successes", type=int, default=3,
                        help="Stop after this many ground-truth confirmed harnesses (default: 3)")
    parser.add_argument("--max_attempts", type=int, default=50,
                        help="Hard cap on LLM calls (default: 50)")
    parser.add_argument("--max_repair_failures", type=int, default=3,
                        help="Consecutive compile failures before resetting prompt (default: 3)")
    parser.add_argument("--verify_timeout", type=int, default=config.VERIFY_TIMEOUT_SECONDS,
                        help=f"Seconds per harness per state (default: {config.VERIFY_TIMEOUT_SECONDS})")
    parser.add_argument("--no-verify", action="store_true",
                        help="Skip 3-state verification; count all compiled harnesses as successes")
    parser.add_argument("--harness_style", choices=["syscall", "libfuzzer"],
                        default=None,
                        help="Override harness style (default: auto-detected from subsystem)")
    parser.add_argument("--model", default=None,
                        help="LLM model to use, e.g. gpt-4o or gpt-oss:20b "
                             "(default: gpt-4o if OPENAI_API_KEY set, else gpt-oss:20b)")
    args = parser.parse_args()

    if args.harness_style:
        import os
        os.environ["LINUX_HARNESS_STYLE"] = args.harness_style

    run(args)


if __name__ == "__main__":
    main()
