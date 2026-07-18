"""Does the R4 menu cover what an LLM freely invents for a patch?

The direction-deciding experiment. For each method: (1) ask the flagship
model to FREELY propose metamorphic relations / invariants from the code
context alone, no menu shown; (2) ask it to map each freely-proposed
relation to a menu family or mark it NOVEL. Aggregate coverage.

If coverage is high, the menu subsumes free invention (suggesting from it
is sound). If low, free invention finds things the menu misses (the menu
is supplementary; the existing free synthesis carries the weight).

Run from src/: python ../study/coverage_eval.py
"""
import sys, os, json, re
sys.path.insert(0, 'java'); sys.path.insert(0, '.')
import variation_menu as vm
from study_tasks import TASKS  # reuse the 25-method set

FREE_SYS = (
    "You are an expert in metamorphic and property-based testing of Java "
    "libraries. Given a method, propose the metamorphic relations and "
    "invariants a correct implementation MUST satisfy — properties usable to "
    "catch a subtly-wrong patch on inputs no example test covers. Think from "
    "the method's contract and the mathematics/semantics of what it computes. "
    "Give 8 distinct relations, each ONE line: '<short-name>: <the property>'. "
    "Be concrete and sound; prefer strong discriminating properties."
)

MAP_SYS = (
    "You compare freely-invented test relations against a fixed catalog. For "
    "each invented relation, output one line 'N => <menu-id>' if the catalog "
    "already contains an entry expressing the SAME underlying property (same "
    "family, even if worded differently), or 'N => NOVEL: <5-word why>' if no "
    "catalog entry covers it. Use exact catalog ids. Be strict: only map when "
    "the property is genuinely the same, not merely the same input kind."
)


def menu_listing():
    return "\n".join(f'{e["id"]}: {e["statement"]}' for e in vm.load_all())


def free_relations(ctx, gen):
    reply = gen.generate([{"role": "system", "content": FREE_SYS},
                          {"role": "user", "content": f"Method:\n{ctx}"}])
    lines = [l.strip(' -*') for l in (reply or '').splitlines()
             if ':' in l and len(l.strip()) > 8]
    return lines[:8]


def map_coverage(free, listing, gen):
    numbered = "\n".join(f"{i+1}. {r}" for i, r in enumerate(free))
    reply = gen.generate([
        {"role": "system", "content": MAP_SYS},
        {"role": "user", "content":
         f"CATALOG (id: property):\n{listing}\n\nINVENTED RELATIONS:\n"
         f"{numbered}\n\nMap each."}])
    covered, novel = [], []
    for m in re.finditer(r'(\d+)\s*=>\s*(NOVEL[:\s].*|[\w-]+)', reply or ''):
        idx = int(m.group(1)) - 1
        val = m.group(2).strip()
        if idx < 0 or idx >= len(free):
            continue
        if val.upper().startswith('NOVEL'):
            novel.append((free[idx], val))
        elif any(e['id'] == val for e in vm.load_all()):
            covered.append((free[idx], val))
        else:
            novel.append((free[idx], 'unmapped:' + val))
    return covered, novel


def main():
    from llm import HarnessGenerator
    gen = HarnessGenerator(model=os.environ.get("MODEL", "gpt-5.4"),
                           temperature=0.3, top_p=1.0)
    listing = menu_listing()
    subset = [t for t in TASKS][:12]
    tot_free = tot_cov = 0
    all_novel = []
    for name, sigs, own, ctx in subset:
        free = free_relations(ctx, gen)
        cov, nov = map_coverage(free, listing, gen)
        tot_free += len(free); tot_cov += len(cov)
        rate = f"{len(cov)}/{len(free)}"
        print(f"{name:28s} covered {rate}")
        for r, why in nov:
            print(f"     NOVEL: {r[:90]}")
            all_novel.append((name, r))
    print("\n==== COVERAGE SUMMARY ====")
    if tot_free:
        print(f"total freely-invented relations: {tot_free}")
        print(f"already covered by the menu     : {tot_cov} "
              f"({100*tot_cov/tot_free:.0f}%)")
        print(f"NOVEL (menu misses)             : {tot_free-tot_cov} "
              f"({100*(tot_free-tot_cov)/tot_free:.0f}%)")


if __name__ == "__main__":
    main()
