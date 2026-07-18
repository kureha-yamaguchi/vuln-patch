"""Evaluate R4 relation-ranking: deterministic keyword score vs gpt-5.4-nano.

Question (from review): is the deterministic keyword ranking good enough,
or should the content-aware step use a cheap model? For 25 diverse tasks
we detect the input kind (deterministic), then rank the kind's candidate
menu relations two ways — keyword overlap vs a nano LLM pick — and
compare the top-3 sets.

Run from src/: python ../study/rank_eval.py
"""
import sys, os, json
sys.path.insert(0, 'java'); sys.path.insert(0, '.')
import input_kind as ik
import variation_menu as vm

# 25 tasks: 14 grounded in the actual benchmark's touched methods (dev set,
# already spent as evidence) + 11 standard-library methods spanning the
# domains, incl. deliberately tricky/ambiguous ones. Each: signatures,
# the touched signature(s), and context text (name + 1-line javadoc + class).
TASKS = [
 ("Math-2 getNumericalMean", ["public double getNumericalMean()","public int sample()","public double cumulativeProbability(int x)"], ["public double getNumericalMean()"],
  "getNumericalMean HypergeometricDistribution the mean of the distribution n*m/N cumulative probability sample"),
 ("Math-53 Complex.add", ["public Complex add(Complex addend)"], ["public Complex add(Complex addend)"],
  "add Complex returns a Complex whose value is this plus the addend real imaginary NaN"),
 ("Math-57 KMeans assignPoints", ["private int assignPointsToClusters(List clusters, Collection points)"], ["private int assignPointsToClusters(List clusters, Collection points)"],
  "assignPointsToClusters KMeansPlusPlusClusterer nearest cluster distance points list"),
 ("Lang-7 createNumber", ["public static Number createNumber(String str)"], ["public static Number createNumber(String str)"],
  "createNumber NumberUtils turns a string into a java.lang.Number hex float"),
 ("Lang-41 getPackageName", ["public static String getPackageName(Class cls)","public static String getPackageName(String className)"], ["public static String getPackageName(Class cls)","public static String getPackageName(String className)"],
  "getPackageName getShortClassName ClassUtils the package name of a class"),
 ("Lang-60 StrBuilder.contains", ["public boolean contains(char ch)","public StrBuilder deleteFirst(String str)","public int indexOf(char ch)"], ["public boolean contains(char ch)"],
  "contains char whether the builder contains a character StrBuilder capacity length indexOf deleteFirst"),
 ("Time-4 Partial.with", ["public Partial with(DateTimeFieldType fieldType, int value)"], ["public Partial with(DateTimeFieldType fieldType, int value)"],
  "Partial with DateTimeFieldType value the partial datetime chronology field instant"),
 ("Time-11 DateTimeZone.forID", ["public static DateTimeZone forID(String id)"], ["public static DateTimeZone forID(String id)"],
  "forID DateTimeZone gets a time zone instance for the specified id offset"),
 ("Closure-62 formatError", ["public String formatError(JSError error)","LightweightMessageFormatter formatter(String sourceCode)"], ["public String formatError(JSError error)"],
  "formatError JSError LightweightMessageFormatter formats an error against the source code line"),
 ("Closure-33 matchConstraint", ["public void matchConstraint(ObjectType constraint)"], ["public void matchConstraint(ObjectType constraint)"],
  "matchConstraint ObjectType PrototypeObjectType infer property record type"),
 ("Chart-26 CategoryPlot.draw", ["public void draw(Graphics2D g2, Rectangle2D area, Point2D anchor, ChartRenderingInfo info)"], ["public void draw(Graphics2D g2, Rectangle2D area, Point2D anchor, ChartRenderingInfo info)"],
  "draw CategoryPlot renders the plot within the given area axis entity bounds rectangle"),
 ("Chart-1 Range.combine", ["public static Range combine(Range range1, Range range2)","public double getLength()"], ["public static Range combine(Range range1, Range range2)"],
  "combine Range creates a new range by combining two ranges lower upper null length"),
 ("Chart-7 TimeSeries.getMaxY", ["public double getMaxY()","public void add(TimePeriod period, double value)"], ["public double getMaxY()"],
  "getMaxY TimeSeries the maximum y-value in the series bounds range add item"),
 ("Math-70 BisectionSolver.solve", ["public double solve(UnivariateFunction f, double min, double max)"], ["public double solve(UnivariateFunction f, double min, double max)"],
  "solve BisectionSolver find a root of the function between min and max iterations tolerance converge"),
 # --- standard library spread ---
 ("String.split", ["public String[] split(String regex)"], ["public String[] split(String regex)"],
  "split String splits this string around matches of the given regular expression trailing empty"),
 ("Collections.sort", ["public static void sort(List list)"], ["public static void sort(List list)"],
  "sort Collections sorts the specified list into ascending order stable comparator"),
 ("String.replace", ["public String replace(CharSequence target, CharSequence replacement)"], ["public String replace(CharSequence target, CharSequence replacement)"],
  "replace String replaces each substring that matches the target sequence count length"),
 ("LocalDate.plusDays", ["public LocalDate plusDays(long days)"], ["public LocalDate plusDays(long days)"],
  "plusDays LocalDate returns a copy of this date with the specified days added duration period"),
 ("Rectangle2D.createUnion", ["public Rectangle2D createUnion(Rectangle2D r)"], ["public Rectangle2D createUnion(Rectangle2D r)"],
  "createUnion Rectangle2D the smallest rectangle containing both this and the specified rectangle bounds"),
 ("Base64.encode", ["public byte[] encode(byte[] src)","public byte[] decode(byte[] src)"], ["public byte[] encode(byte[] src)"],
  "encode Base64 encodes the specified byte array decode roundtrip"),
 ("StringUtils.reverse", ["public static String reverse(String str)"], ["public static String reverse(String str)"],
  "reverse StringUtils reverses a string surrogate code point"),
 ("BigDecimal.add", ["public BigDecimal add(BigDecimal augend)"], ["public BigDecimal add(BigDecimal augend)"],
  "add BigDecimal returns a BigDecimal whose value is this plus augend scale precision"),
 ("TreeMap.put", ["public Object put(Object key, Object value)"], ["public Object put(Object key, Object value)"],
  "put TreeMap associates the specified value with the specified key size contains order"),
 ("ArrayUtils.indexOf", ["public static int indexOf(Object[] array, Object obj)"], ["public static int indexOf(Object[] array, Object obj)"],
  "indexOf ArrayUtils finds the index of the given object in the array search contains"),
 ("MessageDigest.digest", ["public byte[] digest(byte[] input)"], ["public byte[] digest(byte[] input)"],
  "digest MessageDigest performs a final update and computes the hash value bytes"),
]

_NANO_SYS = ("You pick the metamorphic-testing relations most relevant to a Java "
   "method, to check a patch. Given the method and a numbered list of candidate "
   "relations, reply with ONLY a JSON array of the 3 most relevant relation ids "
   "(exact strings from the list), most relevant first. No other text.")

def nano_rank(ctx, candidates, gen):
    listing = "\n".join(f'- {c["id"]}: {c["statement"]}' for c in candidates)
    prompt = f"Method context:\n{ctx}\n\nCandidate relations:\n{listing}\n\nReturn the 3 most relevant ids."
    reply = gen.generate([{"role":"system","content":_NANO_SYS},
                          {"role":"user","content":prompt}])
    import re
    m = re.search(r'\[([^\]]*)\]', reply or '')
    ids = []
    if m:
        for raw in re.findall(r'"([^"]+)"|\'([^\']+)\'|([\w-]+)', m.group(1)):
            v = next(x for x in raw if x)
            if any(v == c["id"] for c in candidates) and v not in ids:
                ids.append(v)
    return ids[:3], (reply or '')[:120]

def main():
    from llm import HarnessGenerator
    gen = HarnessGenerator(model=os.environ.get("NANO_MODEL","gpt-5.4-nano"),
                           temperature=0.0, top_p=1.0)
    agree_counts = []
    ambiguous = 0
    nokind = 0
    for name, sigs, own, ctx in TASKS:
        d = ik.detect(sigs, param_priority=own)
        if d.string_ambiguous: ambiguous += 1
        if not d.kinds:
            nokind += 1
            print(f"{name:28s} kinds=[] (string-ambiguous={d.string_ambiguous}) -> needs Tier-2 classify; skipping rank")
            continue
        cands = vm.entries_for_kinds(d.kinds, cap=99, context_text='')  # all candidates
        det = [e["id"] for e in vm.entries_for_kinds(d.kinds, cap=3, context_text=ctx)]
        nano, _ = nano_rank(ctx, cands, gen)
        overlap = len(set(det) & set(nano))
        agree_counts.append(overlap)
        flag = "OK " if overlap >= 2 else ("~  " if overlap == 1 else "XX ")
        print(f"{flag}{name:28s} kinds={d.kinds}")
        print(f"     deterministic: {det}")
        print(f"     nano         : {nano}   overlap={overlap}/3")
    n = len(agree_counts)
    print("\n==== SUMMARY ====")
    print(f"tasks ranked: {n}   string-ambiguous(needs Tier-2): {ambiguous}   no-kind: {nokind}")
    if n:
        import statistics
        print(f"mean top-3 overlap deterministic vs nano: {statistics.mean(agree_counts):.2f}/3")
        print(f"  >=2 overlap: {sum(1 for x in agree_counts if x>=2)}/{n}   "
              f"exact-3: {sum(1 for x in agree_counts if x==3)}/{n}   "
              f"0 overlap: {sum(1 for x in agree_counts if x==0)}/{n}")

if __name__ == "__main__":
    main()
