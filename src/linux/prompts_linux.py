"""Build chat-completion prompts for Linux kernel harness generation.

Supports two harness styles (determined by LinuxPatchContext.harness_style):
  'syscall'   — standalone main()-based C program that makes syscalls to
                reach the vulnerable kernel code path. Correct for most
                kernel subsystems (SCTP, block device, inotify, unix sockets).
  'libfuzzer' — LLVMFuzzerTestOneInput harness. Only appropriate for
                subsystems with a well-defined byte-stream interface (e.g.
                filesystem image parsers).

Analogous to src/java/prompts.py but for C / Linux kernel patches.
"""
from __future__ import annotations

import sys
from pathlib import Path
from typing import TYPE_CHECKING

sys.path.insert(0, str(Path(__file__).parent.parent))

if TYPE_CHECKING:
    from analysis_linux import LinuxPatchContext

_SYSCALL_SYSTEM = """\
You are a Linux kernel security engineer. Your task is to write a C \
trigger program that reproduces a specific kernel vulnerability described \
by a patch. The program must be a standalone C file with a main() function \
that uses POSIX syscalls (open, read, write, ioctl, sendmsg, socket, etc.) \
to reach the vulnerable code path. It should be compilable with:
    gcc -o trigger trigger.c -lpthread
and run as root on a Linux machine with an unpatched kernel.\
"""

_LIBFUZZER_SYSTEM = """\
You are a Linux kernel security engineer. Your task is to write a \
libFuzzer harness in C that exercises the vulnerable code path described \
by a patch. Use the LLVMFuzzerTestOneInput signature. It must be \
compilable with:
    clang -fsanitize=address,fuzzer harness.c -o harness
The harness should call the kernel interface (via syscalls or /proc/sys) \
using data derived from the fuzzer-provided buffer.\
"""

_GUIDELINES_SYSCALL = """\
## Harness guidelines (syscall-sequence style)

- Start with a main() function, include <stdio.h>, <stdlib.h>, <unistd.h>, \
  <sys/socket.h>, <pthread.h>, and other standard POSIX headers as needed.
- CRITICAL: Do NOT include <netinet/sctp.h>, <linux/sctp.h>, or any \
  <linux/*.h> header — they are not available on the compilation host. \
  Instead define any needed constants inline using their numeric values, e.g. \
  #define IPPROTO_SCTP 132 or #define SOL_SCTP 132.
- Use syscalls that naturally reach the vulnerable function shown in the \
  patch. Work backwards from the patch to identify the syscall entry point.
- If the bug is a race condition, implement the race explicitly with pthreads \
  — spawn a thread that performs the conflicting operation concurrently.
- Keep the program deterministic — do NOT use random() or random inputs. \
  The goal is a minimal, reliable reproducer.
- Handle errors with perror()/exit() — do not silently swallow failures.
- Do NOT include placeholder comments like "// fill in the struct" — \
  write complete, compilable code only.
- Output a single C code block surrounded by ```c ... ```.\
"""

_GUIDELINES_LIBFUZZER = """\
## Harness guidelines (libFuzzer style)

- Implement: int LLVMFuzzerTestOneInput(const uint8_t *data, size_t size)
- Include <stdint.h> and <stddef.h>.
- Derive all kernel inputs (file content, ioctl args, sysctl values) from \
  the fuzzer-provided data/size buffer.
- Return 0 always (crash == bug found by sanitiser).
- Do NOT call exit() inside the harness.
- Output a single C code block surrounded by ```c ... ```.\
"""

_CHAIN_OF_THOUGHT = """\
Before writing any code:
1. Identify which kernel function was patched (look at the diff header).
2. Determine what syscall sequence or file operation reaches that function.
3. Identify what input causes the bug (e.g., specific struct field value, \
   race window, resource exhaustion).
4. Write the minimal reproducer that achieves step 2 with step 3's input.
Reason briefly through each step, then output the complete C code.\
"""


def build(ctx: "LinuxPatchContext") -> list[dict]:
    """Return a chat-completion messages list for the given patch context."""
    if ctx.harness_style == "libfuzzer":
        system = _LIBFUZZER_SYSTEM
        guidelines = _GUIDELINES_LIBFUZZER
    else:
        system = _SYSCALL_SYSTEM
        guidelines = _GUIDELINES_SYSCALL

    messages: list[dict] = [{"role": "system", "content": system}]
    messages.append({"role": "user", "content": _build_user(ctx, guidelines)})
    return messages


def build_repair(
    ctx: "LinuxPatchContext",
    prior_messages: list[dict],
    compiler_error: str,
) -> list[dict]:
    """Extend a conversation with a compile-error repair turn."""
    msgs = list(prior_messages)
    msgs.append({
        "role": "user",
        "content": (
            "The harness failed to compile. Fix ALL errors and output the "
            "complete corrected C code in a single ```c ... ``` block.\n\n"
            f"Compiler output:\n```\n{compiler_error}\n```"
        ),
    })
    return msgs


def _build_user(ctx: "LinuxPatchContext", guidelines: str) -> str:
    parts: list[str] = []

    parts.append(_patch_block(ctx))
    for fn in ctx.touched_functions:
        parts.append(_function_block(fn))
    parts.append(guidelines)
    parts.append(_chain_of_thought_block())
    parts.append(
        f"Write a {'syscall-sequence trigger' if ctx.harness_style == 'syscall' else 'libFuzzer harness'} "
        f"for the vulnerability in **{ctx.prior_cve}** "
        f"(subsystem: `{ctx.subsystem}`, fix type: `{ctx.metadata.get('incomplete_fix_type', 'unknown')}`)."
    )
    return "\n\n".join(parts)


def _patch_block(ctx: "LinuxPatchContext") -> str:
    lines = [
        "## Patch diff (Fix-0 — the incomplete fix)",
        "",
        f"CVE: **{ctx.prior_cve}** → **{ctx.later_cve}**",
        f"Subsystem: `{ctx.subsystem}`",
        f"CWE: {ctx.metadata.get('cwe_fix0', 'N/A')} — {ctx.metadata.get('cwe_fix0_detail', '')}",
        "",
        "```diff",
        ctx.patch_text.strip(),
        "```",
    ]
    return "\n".join(lines)


def _function_block(fn) -> str:
    if not fn.source:
        return f"## Changed function: `{fn.name}` (source not available)\n"
    # Show only the first 200 lines to stay within context budget.
    source_lines = fn.source.splitlines()
    shown = "\n".join(source_lines[:200])
    truncation = (
        f"\n... ({len(source_lines) - 200} more lines truncated)"
        if len(source_lines) > 200 else ""
    )
    return (
        f"## Source context: `{fn.source_file}` (at Fix-0 commit)\n\n"
        f"```c\n{shown}{truncation}\n```"
    )


def _chain_of_thought_block() -> str:
    return _CHAIN_OF_THOUGHT
