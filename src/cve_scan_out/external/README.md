# `external/` — third-party datasets

Data files imported from sources outside this pipeline, useful for
cross-validation or as alternative ground-truth.

## Files

### `liu_seeds_linux.json`

A Linux-kernel subset of the incomplete-fix dataset from:

> Liu, X., Yang, K., Zhang, Z., Kang, K., Ji, S., Pan, S., et al.
> *Characteristics, Root Causes, and Detection of Incomplete Security
> Bug Fixes in the Linux Kernel.*  arXiv:2511.17799 (November 2025).
> Dataset: https://doi.org/10.5281/zenodo.6423844

26 hand-curated Linux kernel CVE pairs where an incomplete or
incorrect initial fix (Fix-0) caused a follow-up vulnerability (Fix-1),
covering 2005–2021. The associated patches, source context, and
metadata for these pairs are checked out under
`src/cve_sibling_db_linux/`.

**Useful as:** an independently curated set to validate the harvester's
heuristics against. The harvester's Project Zero focus and the Liu
dataset's Linux-kernel focus are largely disjoint, so overlap is
expected to be small.
