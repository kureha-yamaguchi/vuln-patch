# Pipeline trace — Lang-59

**Code:** `unknown`

**Patch label:** overfitting  *(the patch under analysis is a known-OVERFIT fix — the pipeline is not told this)*

**Outcome:** OVERFIT CAUGHT (a harness fired on the patched build). [evaluated; 5 harness(es) fuzzed on the patched build; campaign converged=True]

**Patch under analysis:**
```diff
--- /src/java/org/apache/commons/lang/text/StrBuilder.java
+++ /src/java/org/apache/commons/lang/text/StrBuilder.java
@@ -877,7 +877,7 @@ public class StrBuilder implements Cloneable {
      */
     public StrBuilder appendFixedWidthPadRight(Object obj, int width, char padChar) {
         if (width > 0) {
-            ensureCapacity(size + width);
+            ensureCapacity((size) + 5);
             String str = (obj == null ? getNullText() : obj.toString());
             int strLen = str.length();
             if (strLen >= width) {
```

23 sequential steps — 5 LLM calls, 18 deterministic. Read top to bottom.

<details><summary>Legend — what each step is</summary>

- **failing-tests-found** — the project's own tests that expose the bug (extracted from Defects4J)
- **analysis (TargetAnalyzer)** — parses the patch and builds the code context the model reasons over — the touched method(s), their documented contract, the call-graph reachable set + sibling members (via fuzz-introspector), and imports
- **harness generation (LLM)** — writes a Jazzer fuzzing harness that embeds the surviving rules plus oracles lifted from the failing test
- **harness-attempt** — accept/reject of one generated harness — ACCEPTED = it compiles AND crashes the BUGGY build (with the triggering input shown)
- **patched-fuzz** — fuzzes an accepted harness against the PATCHED build — FIRED (with the input + mismatch) means the overfit was caught; quiet means it escaped

</details>

*Viewing: every ▸ line is a click-to-expand toggle (VS Code markdown preview / GitHub). Prompts are collapsed by default; LLM outputs start expanded. The raw file stays fully greppable.*


---
## [0] ⚙️ failing-tests-found
<details><summary>▸ output — FailureTest(test_class='org.apache.commons.lang.text.StrBuilderAppendInsertTest', test_method='testL (~547 chars)</summary>

**output:** 
```json
[
  "FailureTest(test_class='org.apache.commons.lang.text.StrBuilderAppendInsertTest', test_method='testLang299', source_path='/tmp/d4j/Lang_59_buggy/src/test/org/apache/commons/lang/text/StrBuilderAppendInsertTest.java', method_source='    public void testLang299() {\\n        StrBuilder sb = new StrBuilder(1);\\n        sb.appendFixedWidthPadRight(\"foo\", 1, \\'-\\');\\n        assertEquals(\"f\", sb.toString());\\n    }', exception_type='java.lang.ArrayIndexOutOfBoundsException', failure_message=None, support_source=None)"
]
```

</details>

---
## [1] ⚙️ test-context (H1/H2)
**output:** 
```json
[
  {
    "test": "org.apache.commons.lang.text.StrBuilderAppendInsertTest::testLang299",
    "failure_message": "--- org.apache.commons.lang.text.StrBuilderAppendInsertTest::testLang299\njava.lang.StringIndexOutOfBoundsException: offset 0, count 3, length 1",
    "support_chars": 0
  }
]
```

---
## [2] ⚙️ analysis (TargetAnalyzer)
<details><summary>▸ output — modified_files": [ (~8,763 chars)</summary>

**output:** 
```json
{
  "modified_files": [
    "src/java/org/apache/commons/lang/text/StrBuilder.java"
  ],
  "patch_text": "--- /src/java/org/apache/commons/lang/text/StrBuilder.java\n+++ /src/java/org/apache/commons/lang/text/StrBuilder.java\n@@ -877,7 +877,7 @@ public class StrBuilder implements Cloneable {\n      */\n     public StrBuilder appendFixedWidthPadRight(Object obj, int width, char padChar) {\n         if (width > 0) {\n-            ensureCapacity(size + width);\n+            ensureCapacity((size) + 5);\n             String str = (obj == null ? getNullText() : obj.toString());\n             int strLen = str.length();\n             if (strLen >= width) {\n",
  "functions": [
    {
      "func_name": "appendFixedWidthPadRight",
      "func_signature": "public StrBuilder appendFixedWidthPadRight(Object obj, int width, char padChar)",
      "func_source": "    public StrBuilder appendFixedWidthPadRight(Object obj, int width, char padChar) {\n        if (width > 0) {\n            ensureCapacity(size + width);\n            String str = (obj == null ? getNullText() : obj.toString());\n            int strLen = str.length();\n            if (strLen >= width) {\n                str.getChars(0, strLen, buffer, size);\n            } else {\n                int padLen = width - strLen;\n                str.getChars(0, strLen, buffer, size);\n                for (int i = 0; i < padLen; i++) {\n                    buffer[size + strLen + i] = padChar;\n                }\n            }\n            size += width;\n        }\n        return this;\n    }",
      "func_class": "StrBuilder",
      "func_class_fq": "org.apache.commons.lang.text.StrBuilder",
      "func_param_types": [
        "Object",
        "int",
        "char"
      ],
      "fi_name": "[org.apache.commons.lang.text.StrBuilder].appendFixedWidthPadRight(Object,int,char)",
      "overload_types": [
        [
          "Object",
          "int",
          "char"
        ],
        [
          "int",
          "int",
          "char"
        ]
      ],
      "xrefs": [],
      "reachable": [
        "[org.apache.commons.lang.text.StrBuilder].ensureCapacity(org.apache.commons.lang.text.StrBuilder)",
        "[org.apache.commons.lang.text.StrBuilder].getNullText()",
        "[Object].toString()",
        "str.length()",
        "str.getChars(int,org.apache.commons.lang.text.StrBuilder,char[],int)",
        "[org.apache.commons.lang.text.StrBuilder].ensureCapacity(int)"
      ],
      "related_callees": [
        {
          "name": "ensureCapacity",
          "source_file": "StrBuilder.java",
          "signature": "public StrBuilder ensureCapacity(int capacity)",
          "source": "    public StrBuilder ensureCapacity(int capacity) {\n        if (capacity > buffer.length) {\n            char[] old = buffer;\n            buffer = new char[capacity];\n            System.arraycopy(old, 0, buffer, 0, size);\n        }\n        return this;\n    }",
          "is_abstract": false,
          "impls": []
        },
        {
          "name": "getNullText",
          "source_file": "StrBuilder.java",
          "signature": "public String getNullText()",
          "source": "    public String getNullText() {\n        return nullText;\n    }",
          "is_abstract": false,
          "impls": []
        },
        {
          "name": "toString",
          "source_file": "StrBuilder.java",
          "signature": "public String toString()",
          "source": "    public String toString() {\n        return new String(buffer, 0, size);\n    }",
          "is_abstract": false,
          "impls": [
            [
              "StrBuilder.java",
              "    public String toString() {\n        return new String(buffer, 0, size);\n    }"
            ],
            [
              "StrTokenizer.java",
              "    public String toString() {\n        if (tokens == null) {\n            return \"StrTokenizer[not tokenized yet]\";\n        } else {\n            return \"StrTokenizer\" + getTokenList();\n        }\n    }"
            ]
          ]
        },
        {
          "name": "length",
          "source_file": "StrBuilder.java",
          "signature": "public int length()",
          "source": "    public int length() {\n        return size;\n    }",
          "is_abstract": false,
          "impls": []
        },
        {
          "name": "getChars",
          "source_file": "StrBuilder.java",
          "signature": "public char[] getChars(char[] destination)",
          "source": "    public char[] getChars(char[] destination) {\n        int len = length();\n        if (destination == null || destination.length < len) {\n            destination = new char[len];\n        }\n        System.arraycopy(buffer, 0, destination, 0, len);\n        return destination;\n    }",
          "is_abstract": false,
          "impls": [
            [
              "StrBuilder.java",
              "    public char[] getChars(char[] destination) {\n        int len = length();\n        if (destination == null || destination.length < len) {\n            destination = new char[len];\n        }\n        System.arraycopy(buffer, 0, destination, 0, len);\n        return destination;\n    }"
            ],
            [
              "StrBuilder.java",
              "    public void getChars(int startIndex, int endIndex, char destination[], int destinationIndex) {\n        if (startIndex < 0) {\n            throw new StringIndexOutOfBoundsException(startIndex);\n        }\n        if (endIndex < 0 || endIndex > length()) {\n            throw new StringIndexOutOfBoundsException(endIndex);\n        }\n        if (startIndex > endIndex) {\n            throw new StringIndexOutOfBoundsException(\"end < start\");\n        }\n        System.arraycopy(buffer, startIndex, destination, destinationIndex, endIndex - startIndex);\n    }"
            ]
          ]
        }
      ],
      "field_siblings": [
        {
          "name": "setLength",
          "signature": "public StrBuilder setLength(int length)",
          "shared_fields": [
            "buffer",
            "size"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "Updates the length of the builder by either dropping the last characters or adding filler of unicode zero. @param length  the length to set to, must be zero or positive @return this, to enable chaining @throws IndexOutOfBoundsException if the length is negative"
        },
        {
          "name": "minimizeCapacity",
          "signature": "public StrBuilder minimizeCapacity()",
          "shared_fields": [
            "buffer",
            "size"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "Minimizes the capacity to the actual length of the string. @return this, to enable chaining"
        },
        {
          "name": "toCharArray",
          "signature": "public char[] toCharArray()",
          "shared_fields": [
            "buffer",
            "size"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "Copies the builder's character array into a new character array. @return a new array that represents the contents of the builder"
        },
        {
          "name": "append",
          "signature": "public StrBuilder append(String str)",
          "shared_fields": [
            "buffer",
            "size"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "Appends a string to this string builder. Appending null will call {@link #appendNull()}. @param str  the string to append @return this, to enable chaining"
        },
        {
          "name": "append",
          "signature": "public StrBuilder append(String str, int startIndex, int length)",
          "shared_fields": [
            "buffer",
            "size"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "Appends part of a string to this string builder. Appending null will call {@link #appendNull()}. @param str  the string to append @param startIndex  the start index, inclusive, must be valid @param length  the length to append, must be valid @return this, to enable chaining"
        }
      ]
    }
  ],
  "package": "org.apache.commons.lang.text",
  "root_cause_reachable": [
    "StrBuilder.ensureCapacity",
    "StrBuilder.getNullText"
  ],
  "neighbourhood_notes": [],
  "source_imports": [
    "import java.io.Reader;",
    "import java.io.Writer;",
    "import java.util.Collection;",
    "import java.util.Iterator;",
    "import java.util.List;",
    "import org.apache.commons.lang.ArrayUtils;",
    "import org.apache.commons.lang.SystemUtils;"
  ]
}
```

</details>

---
## [3] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~26,888 chars)</summary>

**[system]**
```
You are an expert Java security engineer who writes Jazzer fuzzing harnesses. Return a single compilable .java file — no markdown fences, no prose outside the file.
```
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.apache.commons.lang.text` (`package org.apache.commons.lang.text;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Lang_59_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/java/org/apache/commons/lang/text/StrBuilder.java
+++ /src/java/org/apache/commons/lang/text/StrBuilder.java
@@ -877,7 +877,7 @@ public class StrBuilder implements Cloneable {
      */
     public StrBuilder appendFixedWidthPadRight(Object obj, int width, char padChar) {
         if (width > 0) {
-            ensureCapacity(size + width);
+            ensureCapacity((size) + 5);
             String str = (obj == null ? getNullText() : obj.toString());
             int strLen = str.length();
             if (strLen >= width) {

</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.SystemUtils;
</source_imports>

Function `appendFixedWidthPadRight`:
<signature>
public StrBuilder appendFixedWidthPadRight(Object obj, int width, char padChar)
</signature>
<code>
    public StrBuilder appendFixedWidthPadRight(Object obj, int width, char padChar) {
        if (width > 0) {
            ensureCapacity(size + width);
            String str = (obj == null ? getNullText() : obj.toString());
            int strLen = str.length();
            if (strLen >= width) {
                str.getChars(0, strLen, buffer, size);
            } else {
                int padLen = width - strLen;
                str.getChars(0, strLen, buffer, size);
                for (int i = 0; i < padLen; i++) {
                    buffer[size + strLen + i] = padChar;
                }
            }
            size += width;
        }
        return this;
    }
</code>
Methods called by `appendFixedWidthPadRight` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="ensureCapacity" from="StrBuilder.java">
<signature>
public StrBuilder ensureCapacity(int capacity)
</signature>
<code>
    public StrBuilder ensureCapacity(int capacity) {
        if (capacity > buffer.length) {
            char[] old = buffer;
            buffer = new char[capacity];
            System.arraycopy(old, 0, buffer, 0, size);
        }
        return this;
    }
</code>
</callee>
<callee name="getNullText" from="StrBuilder.java">
<signature>
public String getNullText()
</signature>
<code>
    public String getNullText() {
        return nullText;
    }
</code>
</callee>
<callee name="toString" from="StrBuilder.java">
<signature>
public String toString()
</signature>
<code>
    public String toString() {
        return new String(buffer, 0, size);
    }
</code>
<implementation in="StrBuilder.java">
    public String toString() {
        return new String(buffer, 0, size);
    }
</implementation>
<implementation in="StrTokenizer.java">
    public String toString() {
        if (tokens == null) {
            return "StrTokenizer[not tokenized yet]";
        } else {
            return "StrTokenizer" + getTokenList();
        }
    }
</implementation>
</callee>
<callee name="length" from="StrBuilder.java">
<signature>
public int length()
</signature>
<code>
    public int length() {
        return size;
    }
</code>
</callee>
<callee name="getChars" from="StrBuilder.java">
<signature>
public char[] getChars(char[] destination)
</signature>
<code>
    public char[] getChars(char[] destination) {
        int len = length();
        if (destination == null || destination.length < len) {
            destination = new char[len];
        }
        System.arraycopy(buffer, 0, destination, 0, len);
        return destination;
    }
</code>
<implementation in="StrBuilder.java">
    public char[] getChars(char[] destination) {
        int len = length();
        if (destination == null || destination.length < len) {
            destination = new char[len];
        }
        System.arraycopy(buffer, 0, destination, 0, len);
        return destination;
    }
</implementation>
<implementation in="StrBuilder.java">
    public void getChars(int startIndex, int endIndex, char destination[], int destinationIndex) {
        if (startIndex < 0) {
            throw new StringIndexOutOfBoundsException(startIndex);
        }
        if (endIndex < 0 || endIndex > length()) {
            throw new StringIndexOutOfBoundsException(endIndex);
        }
        if (startIndex > endIndex) {
            throw new StringIndexOutOfBoundsException("end < start");
        }
        System.arraycopy(buffer, startIndex, destination, destinationIndex, endIndex - startIndex);
    }
</implementation>
</callee>
STATE COUPLING: these members of the same class share the listed field(s) with `appendFixedWidthPadRight` but neither calls the other — state one of them writes is what the other reports. A defect the patch leaves (or introduces) in that shared state stays observable through these members even when `appendFixedWidthPadRight`'s own output looks right, so a strong harness CHECKS THEY AGREE: compare what a reader reports against what the writer/constructor established.
  - `public StrBuilder setLength(int length)` (shared field(s): buffer, size)
      doc: Updates the length of the builder by either dropping the last characters or adding filler of unicode zero. @param length  the length to set to, must be zero or positive @return this, to enable chaining @throws IndexOutOfBoundsException if the length is negative
  - `public StrBuilder minimizeCapacity()` (shared field(s): buffer, size)
      doc: Minimizes the capacity to the actual length of the string. @return this, to enable chaining
  - `public char[] toCharArray()` (shared field(s): buffer, size)
      doc: Copies the builder's character array into a new character array. @return a new array that represents the contents of the builder
  - `public StrBuilder append(String str)` (shared field(s): buffer, size)
      doc: Appends a string to this string builder. Appending null will call {@link #appendNull()}. @param str  the string to append @return this, to enable chaining
  - `public StrBuilder append(String str, int startIndex, int length)` (shared field(s): buffer, size)
      doc: Appends part of a string to this string builder. Appending null will call {@link #appendNull()}. @param str  the string to append @param startIndex  the start index, inclusive, must be valid @param length  the length to append, must be valid @return this, to enable chaining

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `StrBuilderAppendInsert`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.StringIndexOutOfBoundsException, java.lang.ArrayIndexOutOfBoundsException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `appendFixedWidthPadRight` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: java.lang.StringIndexOutOfBoundsException
message: offset 0, count 3, length 1
thrown_at: org.apache.commons.lang.text.StrBuilder.appendFixedWidthPadRight(StrBuilder.java:884)
</ground_truth_crash>
<failing_test class="org.apache.commons.lang.text.StrBuilderAppendInsertTest" method="testLang299">
    public void testLang299() {
        StrBuilder sb = new StrBuilder(1);
        sb.appendFixedWidthPadRight("foo", 1, '-');
        assertEquals("f", sb.toString());
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.lang.text.StrBuilderAppendInsertTest::testLang299
java.lang.StringIndexOutOfBoundsException: offset 0, count 3, length 1
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  append(Object obj) / (String str) / (String str, int startIndex, int length) / (StringBuffer str) / (StringBuffer str, int startIndex, int length) / (StrBuilder str) / (StrBuilder str, int startIndex, int length) / (char[] chars) / (char[] chars, int startIndex, int length) / (boolean value) / (char ch) / (int value) / (long value) / (float value) / (double value)
  appendFixedWidthPadLeft(Object obj, int width, char padChar) / (int value, int width, char padChar)
  appendFixedWidthPadRight(Object obj, int width, char padChar) / (int value, int width, char padChar)
  appendWithSeparators(Object[] array, String separator) / (Collection coll, String separator) / (Iterator it, String separator)
  contains(char ch) / (String str) / (StrMatcher matcher)
  deleteAll(char ch) / (String str) / (StrMatcher matcher)
  deleteFirst(char ch) / (String str) / (StrMatcher matcher)
  equals(StrBuilder other) / (Object obj)

METHOD FAMILIES over the same input space (factory/parser siblings — where the docs state a selection or agreement rule between family members, equivalent inputs must respect it):
  append* family: appendFixedWidthPadLeft, appendFixedWidthPadRight, appendNewLine, appendNull, appendPadding, appendWithSeparators
  as* family: asReader, asTokenizer

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- StrBuilder.ensureCapacity
- StrBuilder.getNullText
</root_cause_reachable>
First harness: establish the most direct path from the fuzz entrypoint through the patched code.
VARIANT STRATEGY — this harness is one of a set that divides the strategies below so each is tried. USE STRATEGY (b) for THIS harness (fall back to another ONLY if it is clearly inapplicable to this patch). The patch may be a band-aid that fixed the reported instance while the SAME root cause still bites elsewhere in this region:

  (a) DIFFERENT REACHABLE FUNCTION (coverage spread). Shape the input so a different function in the region above runs / a different failure signature appears. Good when the same root-cause pattern recurs across several functions and the patch only fixed one.

  (b) CONSISTENCY CROSS-CHECK on a masked helper. A defect can leave the patched function's top-level output CORRECT because a downstream step (an iterative solve, a convergence loop, a clamp, a re-normalisation) absorbs the error — while a reachable HELPER that produces a RELATED quantity stays wrong (the unfixed root cause). Rather than guess that helper's exact value, compute the SAME quantity TWO independent ways and assert they AGREE — a check that is sound without knowing the answer. Generic patterns: a stated summary/aggregate must match the EMPIRICAL value recomputed over many items the same object produces; a reported size/count must match a manual count of the elements; a cached/precomputed value must equal a fresh recomputation; equal objects must have equal hashCodes. Use a tolerance for floating-point/statistical estimates and require enough items, so a CORRECT implementation never fires.

  (c) FLIP THE PATCHED CONDITION (overfit on the seed). The patch changed a specific line/condition. An overfitting fix often special-cases the seed input and breaks just past it. Construct inputs that make the OLD and NEW behaviour DIFFER — values at and around the boundary the changed condition tests — and check the result is still correct there.

GUARDRAIL (all options): only assert a property that holds for EVERY correct implementation. If you cannot state one, exercise the path WITHOUT asserting — an assertion you can't justify false-positives on a correct patch.
CONSISTENCY CHECKS (these directly support strategy (b) — a summary a defect can leave wrong while the top-level output is masked). Identify from the API IN FRONT OF YOU which exposed values can be cross-checked against an independent computation — a reported aggregate vs a recomputation from the object's own output, a value vs the object's own stated bounds — and for at least one, throw on a mismatch beyond tolerance. These are SOUND (a summary must match the data it summarises), so they will not fire on a correct implementation.
WHAT DOES NOT COUNT: asserting the reported value is finite, non-NaN, non-null, or that no exception was thrown is NOT a consistency check — every wrong-but-finite value passes those, so they can never catch this class of bug. A valid check COMPARES the reported value against a second, independently obtained quantity: the object's own stated bounds, or a recomputation from the object's own output.
SHAPE OF A VALID CHECK (a schema — instantiate every <placeholder> with real calls from the API above):

    var reported = <the value the object claims>;
    var independent = <the SAME quantity obtained a second, independent way>;
    if (<reported disagrees with independent beyond a generous tolerance>)
        throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:<short-id>] consistency violation: " + <both values>);

Ways to obtain the independent value — each sound for ANY correct implementation of ANY library:
  1. LIMITS THE OBJECT ITSELF STATES: a summary of data must lie within that data's own reported extremes (lower/upper, min/max, first/last, 0..capacity).
  2. RECOMPUTATION FROM THE OBJECT'S OWN OUTPUT: aggregate many values/elements the same object produces and compare the aggregate to the reported summary.
  3. A SECOND, IDENTICALLY-CONSTRUCTED OBJECT (or a fresh recomputation of a cached value): both must report the same thing.

Use enough items and a generous tolerance (a few percent, or several standard errors) so noise on a CORRECT implementation never fires; a genuinely wrong value is typically off by far more than any sane tolerance.

POST-CONDITION / METAMORPHIC CHECK (MANDATORY — catches wrong-output bugs that never throw):
ASSUME THE ADVERSARY. The patch under analysis may 'fix' the bug by simply (a) deleting the throw / bookkeeping statement, (b) adding a guard that makes the crashing branch unreachable (and with it the branch's intended behaviour), or (c) replacing the failing operation with one that silently does the wrong thing (e.g. appending instead of sorted-inserting, skipping a modification-counter update). In all three worlds NO exception ever fires and a crash-only harness passes the patch. Therefore, in addition to reproducing the ground-truth failure, your harness MUST assert at least ONE observable post-condition from the documented contract of the patched method that such a patch would violate — e.g. after inserting into an auto-sorted collection, the collection is still sorted; after a removal, an iterator over the container either throws or reflects the removal (never silently yields stale state); a branch that is supposed to set a size/flag/result observably set it. State in a comment WHICH contract guarantee you assert and WHY a throw-deleting patch would break it. A harness that only reproduces the crash is incomplete.

Prefer a post-condition you can read directly off the API after the call. Where none is observable, use ONE metamorphic relation — a relation between two related calls of the target that must hold for ANY correct implementation: compute both sides from REAL library calls on the fuzzed input, and throw if they disagree.
- Round-trip / inverse: f(g(x)) == x  (e.g. decode(encode(x)), parse(format(v)), unescape(escape(s))).
- Idempotence: f(f(x)) == f(x)  (e.g. normalise, trim, strip, canonicalise).
- Equivalent inputs: two inputs that must map to the same result do  (e.g. case-insensitive parse: f(s) == f(s.toUpperCase()); leading-zero / whitespace variants of the same number).
- Composition / split: f(a + b) relates to f(a) and f(b) consistently  (e.g. a translator/encoder applied to a concatenation equals the concatenation of the parts).
- Oracle from the input itself: when the fuzzed input is CONSTRUCTED from a known value, the result must recover it (e.g. build the canonical string for a random int n, parse it, and assert it equals n).

HYGIENE RULES — violating these fires on CORRECT patches (a false positive, the worst outcome):
- If EITHER side of a relation, or the call preceding a post-condition read, throws anything at all, the check does not apply to that input: catch it, skip the check, return normally. NEVER convert a caught exception into a violation — an exception is a rejection, not a wrong answer.
- Before asserting a relation or post-condition, cite in a comment the documented guarantee (javadoc sentence, class contract, or invariant visible in the code shown above) that makes it hold for EVERY correct implementation, including edge cases: null elements, empty inputs, duplicates, no-solution inputs. If you cannot cite one, do not assert it.
- FENCE DEGENERATE INPUTS: construct the inputs you assert on to be non-degenerate BY CONSTRUCTION — non-empty strings and collections, visible (non-blank) labels/names, at least one element where elements are iterated. Contracts are routinely silent about the empty/blank case (a correct implementation may legitimately do nothing for an empty label, return nothing for an empty collection), so a relation whose ONLY violations occur on degenerate inputs is testing unspecified behaviour and will be rejected in review. Assert on the degenerate case ONLY when the documented contract explicitly covers it — and cite that sentence.
- FENCE EXTREME MAGNITUDES the same way: cap fuzzed numeric parameters to moderate ranges by construction (as a rule of thumb, |value| <= 1_000_000 for integers that get multiplied together or fed to combinatorial/statistical formulas) unless the documented contract explicitly covers larger values. At billion-scale parameters a CORRECT implementation's double arithmetic legitimately degrades — internal overflow to NaN, rounding beyond any tolerance, log-gamma saturation — so an assertion whose only violations occur at such magnitudes accuses correct code (this exact pattern produced repeated false accusations: a probability check that sees NaN only at N near 2^31, a validation exception from a parameter range that overflowed). The bug you hunt fires at REASONABLE magnitudes too whenever the contract really is broken.
- CONDITIONAL SIDE EFFECTS ARE NOT UNCONDITIONAL. If the method performs an observable side effect (adds to a collection, increments a counter, populates a cache, sets a field) only INSIDE guards — nested `if`s on collaborator or field state, not just on the input — you may assert that side effect happened ONLY after your harness has established EVERY one of those guard conditions. A correct implementation legitimately does nothing when any guard is unmet (a null owner/collector/context, a disabled flag), so 'the effect always happens after the call' is unsound and fires on correct patches. Read the shown method body: satisfy each guard on the path to the effect, or do not assert the effect.
- Use only real library calls for BOTH sides (no hand-rolled reference implementation).
- On violation, `throw new RuntimeException("[oracle:<short-id>] metamorphic violation: <which relation> input=<...> lhs=<...> rhs=<...>")` with the concrete values, so a reviewer can replay the disagreement. The [oracle:<short-id>] prefix is MANDATORY (checked mechanically) — an un-named alarm is invisible to the per-check acceptance machinery. Jazzer reports the throw as a finding, the same as a crash.

Use ONLY these FuzzedDataProvider methods (no invented overloads):

    int     consumeInt()                    // any int
    int     consumeInt(int min, int max)    // inclusive
    byte    consumeByte()
    boolean consumeBoolean()
    String  consumeString(int maxLength)    // ONE arg
    String  consumeAsciiString(int maxLength)
    String  consumeRemainingAsString()
    byte[]  consumeBytes(int maxLength)
    byte[]  consumeRemainingAsBytes()
    int     remainingBytes()


Complete the skeleton below. Fill in ONLY the `// >>> YOUR CODE HERE <<<` region. Do NOT change the package, import, class name, or entrypoint. Do NOT add a `main` method. Add extra imports only if javac requires them, directly below the existing import.
<skeleton>
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~5,853 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runScenario(1, "foo", 1, '-', true);

        int width = data.consumeInt(1, 32);
        char padChar = (char) (data.consumeByte() & 0xff);

        String prefix = data.consumeAsciiString(16);
        String core = data.consumeAsciiString(32);
        if (core.length() == 0) {
            core = "A";
        }
        String suffix = data.consumeRemainingAsString();

        String str = prefix + core + suffix;
        if (str.length() <= width) {
            StringBuilder sb = new StringBuilder(str);
            while (sb.length() <= width) {
                sb.append('X');
            }
            str = sb.toString();
        }

        int initialCapacity = data.consumeInt(width, Math.max(width, str.length() - 1));
        runScenario(initialCapacity, str, width, padChar, true);
    }

    private static void runScenario(int initialCapacity, String str, int width, char padChar, boolean validByConstruction) {
        StrBuilder actual = new StrBuilder(initialCapacity);
        try {
            actual.appendFixedWidthPadRight(str, width, padChar);
        } catch (RuntimeException t) {
            if (validByConstruction && isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        try {
            StrBuilder expected = new StrBuilder(initialCapacity);
            if (str.length() >= width) {
                expected.append(str, 0, width);
            } else {
                expected.append(str);
                expected.appendPadding(width - str.length(), padChar);
            }

            String actualString = actual.toString();
            String expectedString = expected.toString();

            if (!actualString.equals(expectedString)) {
                throw new RuntimeException(
                    "[oracle:fixed-width-pad-right] metamorphic violation: appendFixedWidthPadRight must agree with append-truncate-or-pad construction"
                        + " input=" + str
                        + " width=" + width
                        + " pad=" + (int) padChar
                        + " lhs=" + actualString
                        + " rhs=" + expectedString);
            }

            /* Contract/invariant asserted:
             * StrBuilder stores content in shared state (buffer,size), so independent readers of that state
             * must agree. A throw-deleting or bookkeeping-skipping patch can leave top-level output masked,
             * but length(), toString(), toCharArray(), and getChars(null) must all describe the same content.
             */
            int reportedLength = actual.length();
            char[] viaToCharArray = actual.toCharArray();
            char[] viaGetChars = actual.getChars((char[]) null);
            String fromToCharArray = new String(viaToCharArray);
            String fromGetChars = new String(viaGetChars);

            if (reportedLength != actualString.length()
                    || reportedLength != viaToCharArray.length
                    || reportedLength != viaGetChars.length
                    || !actualString.equals(fromToCharArray)
                    || !actualString.equals(fromGetChars)) {
                throw new RuntimeException(
                    "[oracle:state-consistency] consistency violation: length/toString/char views disagree"
                        + " length=" + reportedLength
                        + " toStringLen=" + actualString.length()
                        + " toCharArrayLen=" + viaToCharArray.length
                        + " getCharsLen=" + viaGetChars.length
                        + " toString=" + actualString
                        + " toCharArray=" + fromToCharArray
                        + " getChars=" + fromGetChars);
            }

            String beforeMinimize = actual.toString();
            int beforeLen = actual.length();
            actual.minimizeCapacity();
            actual.setLength(beforeLen);
            String afterMinimize = actual.toString();

            if (!beforeMinimize.equals(afterMinimize) || beforeLen != actual.length()) {
                throw new RuntimeException(
                    "[oracle:minimize-noop] metamorphic violation: minimizeCapacity/setLength(length()) must preserve content"
                        + " before=" + beforeMinimize
                        + " after=" + afterMinimize
                        + " beforeLen=" + beforeLen
                        + " afterLen=" + actual.length());
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            throw t;
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException
                || t instanceof ArrayIndexOutOfBoundsException
                || t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())) {
                String method = ste.getMethodName();
                if ("appendFixedWidthPadRight".equals(method)
                        || "ensureCapacity".equals(method)
                        || "getNullText".equals(method)) {
                    return true;
                }
            }
        }
        return false;
    }
}
```

</details>

---
## [4] ⚙️ harness-attempt · `attempt_001`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.StringIndexOutOfBoundsException: offset 0, count 3, length 1

---
## [5] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~28,258 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.apache.commons.lang.text` (`package org.apache.commons.lang.text;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Lang_59_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/java/org/apache/commons/lang/text/StrBuilder.java
+++ /src/java/org/apache/commons/lang/text/StrBuilder.java
@@ -877,7 +877,7 @@ public class StrBuilder implements Cloneable {
      */
     public StrBuilder appendFixedWidthPadRight(Object obj, int width, char padChar) {
         if (width > 0) {
-            ensureCapacity(size + width);
+            ensureCapacity((size) + 5);
             String str = (obj == null ? getNullText() : obj.toString());
             int strLen = str.length();
             if (strLen >= width) {

</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.SystemUtils;
</source_imports>

Function `appendFixedWidthPadRight`:
<signature>
public StrBuilder appendFixedWidthPadRight(Object obj, int width, char padChar)
</signature>
<code>
    public StrBuilder appendFixedWidthPadRight(Object obj, int width, char padChar) {
        if (width > 0) {
            ensureCapacity(size + width);
            String str = (obj == null ? getNullText() : obj.toString());
            int strLen = str.length();
            if (strLen >= width) {
                str.getChars(0, strLen, buffer, size);
            } else {
                int padLen = width - strLen;
                str.getChars(0, strLen, buffer, size);
                for (int i = 0; i < padLen; i++) {
                    buffer[size + strLen + i] = padChar;
                }
            }
            size += width;
        }
        return this;
    }
</code>
Methods called by `appendFixedWidthPadRight` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="ensureCapacity" from="StrBuilder.java">
<signature>
public StrBuilder ensureCapacity(int capacity)
</signature>
<code>
    public StrBuilder ensureCapacity(int capacity) {
        if (capacity > buffer.length) {
            char[] old = buffer;
            buffer = new char[capacity];
            System.arraycopy(old, 0, buffer, 0, size);
        }
        return this;
    }
</code>
</callee>
<callee name="getNullText" from="StrBuilder.java">
<signature>
public String getNullText()
</signature>
<code>
    public String getNullText() {
        return nullText;
    }
</code>
</callee>
<callee name="toString" from="StrBuilder.java">
<signature>
public String toString()
</signature>
<code>
    public String toString() {
        return new String(buffer, 0, size);
    }
</code>
<implementation in="StrBuilder.java">
    public String toString() {
        return new String(buffer, 0, size);
    }
</implementation>
<implementation in="StrTokenizer.java">
    public String toString() {
        if (tokens == null) {
            return "StrTokenizer[not tokenized yet]";
        } else {
            return "StrTokenizer" + getTokenList();
        }
    }
</implementation>
</callee>
<callee name="length" from="StrBuilder.java">
<signature>
public int length()
</signature>
<code>
    public int length() {
        return size;
    }
</code>
</callee>
<callee name="getChars" from="StrBuilder.java">
<signature>
public char[] getChars(char[] destination)
</signature>
<code>
    public char[] getChars(char[] destination) {
        int len = length();
        if (destination == null || destination.length < len) {
            destination = new char[len];
        }
        System.arraycopy(buffer, 0, destination, 0, len);
        return destination;
    }
</code>
<implementation in="StrBuilder.java">
    public char[] getChars(char[] destination) {
        int len = length();
        if (destination == null || destination.length < len) {
            destination = new char[len];
        }
        System.arraycopy(buffer, 0, destination, 0, len);
        return destination;
    }
</implementation>
<implementation in="StrBuilder.java">
    public void getChars(int startIndex, int endIndex, char destination[], int destinationIndex) {
        if (startIndex < 0) {
            throw new StringIndexOutOfBoundsException(startIndex);
        }
        if (endIndex < 0 || endIndex > length()) {
            throw new StringIndexOutOfBoundsException(endIndex);
        }
        if (startIndex > endIndex) {
            throw new StringIndexOutOfBoundsException("end < start");
        }
        System.arraycopy(buffer, startIndex, destination, destinationIndex, endIndex - startIndex);
    }
</implementation>
</callee>
STATE COUPLING: these members of the same class share the listed field(s) with `appendFixedWidthPadRight` but neither calls the other — state one of them writes is what the other reports. A defect the patch leaves (or introduces) in that shared state stays observable through these members even when `appendFixedWidthPadRight`'s own output looks right, so a strong harness CHECKS THEY AGREE: compare what a reader reports against what the writer/constructor established.
  - `public StrBuilder setLength(int length)` (shared field(s): buffer, size)
      doc: Updates the length of the builder by either dropping the last characters or adding filler of unicode zero. @param length  the length to set to, must be zero or positive @return this, to enable chaining @throws IndexOutOfBoundsException if the length is negative
  - `public StrBuilder minimizeCapacity()` (shared field(s): buffer, size)
      doc: Minimizes the capacity to the actual length of the string. @return this, to enable chaining
  - `public char[] toCharArray()` (shared field(s): buffer, size)
      doc: Copies the builder's character array into a new character array. @return a new array that represents the contents of the builder
  - `public StrBuilder append(String str)` (shared field(s): buffer, size)
      doc: Appends a string to this string builder. Appending null will call {@link #appendNull()}. @param str  the string to append @return this, to enable chaining
  - `public StrBuilder append(String str, int startIndex, int length)` (shared field(s): buffer, size)
      doc: Appends part of a string to this string builder. Appending null will call {@link #appendNull()}. @param str  the string to append @param startIndex  the start index, inclusive, must be valid @param length  the length to append, must be valid @return this, to enable chaining

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `StrBuilderAppendInsert`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.StringIndexOutOfBoundsException, java.lang.ArrayIndexOutOfBoundsException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `appendFixedWidthPadRight` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: java.lang.StringIndexOutOfBoundsException
message: offset 0, count 3, length 1
thrown_at: org.apache.commons.lang.text.StrBuilder.appendFixedWidthPadRight(StrBuilder.java:884)
</ground_truth_crash>
<failing_test class="org.apache.commons.lang.text.StrBuilderAppendInsertTest" method="testLang299">
    public void testLang299() {
        StrBuilder sb = new StrBuilder(1);
        sb.appendFixedWidthPadRight("foo", 1, '-');
        assertEquals("f", sb.toString());
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.lang.text.StrBuilderAppendInsertTest::testLang299
java.lang.StringIndexOutOfBoundsException: offset 0, count 3, length 1
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  append(Object obj) / (String str) / (String str, int startIndex, int length) / (StringBuffer str) / (StringBuffer str, int startIndex, int length) / (StrBuilder str) / (StrBuilder str, int startIndex, int length) / (char[] chars) / (char[] chars, int startIndex, int length) / (boolean value) / (char ch) / (int value) / (long value) / (float value) / (double value)
  appendFixedWidthPadLeft(Object obj, int width, char padChar) / (int value, int width, char padChar)
  appendFixedWidthPadRight(Object obj, int width, char padChar) / (int value, int width, char padChar)
  appendWithSeparators(Object[] array, String separator) / (Collection coll, String separator) / (Iterator it, String separator)
  contains(char ch) / (String str) / (StrMatcher matcher)
  deleteAll(char ch) / (String str) / (StrMatcher matcher)
  deleteFirst(char ch) / (String str) / (StrMatcher matcher)
  equals(StrBuilder other) / (Object obj)

METHOD FAMILIES over the same input space (factory/parser siblings — where the docs state a selection or agreement rule between family members, equivalent inputs must respect it):
  append* family: appendFixedWidthPadLeft, appendFixedWidthPadRight, appendNewLine, appendNull, appendPadding, appendWithSeparators
  as* family: asReader, asTokenizer

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- StrBuilder.ensureCapacity
- StrBuilder.getNullText
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.lang.text.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.lang.text.FuzzHarness.runScenario
- org.apache.commons.lang.text.StrBuilder.appendFixedWidthPadRight
Crashes already found:
- java.lang.StringIndexOutOfBoundsException@org.apache.commons.lang.text.StrBuilder.appendFixedWidthPadRight
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- StrBuilder.ensureCapacity
- StrBuilder.getNullText
Check FAMILIES already covered by accepted harnesses: {fixed-width-pad-right, minimize-noop, state-consistency}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.StringIndexOutOfBoundsException@org.apache.commons.lang.text.StrBuilder.appendFixedWidthPadRight. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
VARIANT STRATEGY — this harness is one of a set that divides the strategies below so each is tried. USE STRATEGY (c) for THIS harness (fall back to another ONLY if it is clearly inapplicable to this patch). The patch may be a band-aid that fixed the reported instance while the SAME root cause still bites elsewhere in this region:

  (a) DIFFERENT REACHABLE FUNCTION (coverage spread). Shape the input so a different function in the region above runs / a different failure signature appears. Good when the same root-cause pattern recurs across several functions and the patch only fixed one.

  (b) CONSISTENCY CROSS-CHECK on a masked helper. A defect can leave the patched function's top-level output CORRECT because a downstream step (an iterative solve, a convergence loop, a clamp, a re-normalisation) absorbs the error — while a reachable HELPER that produces a RELATED quantity stays wrong (the unfixed root cause). Rather than guess that helper's exact value, compute the SAME quantity TWO independent ways and assert they AGREE — a check that is sound without knowing the answer. Generic patterns: a stated summary/aggregate must match the EMPIRICAL value recomputed over many items the same object produces; a reported size/count must match a manual count of the elements; a cached/precomputed value must equal a fresh recomputation; equal objects must have equal hashCodes. Use a tolerance for floating-point/statistical estimates and require enough items, so a CORRECT implementation never fires.

  (c) FLIP THE PATCHED CONDITION (overfit on the seed). The patch changed a specific line/condition. An overfitting fix often special-cases the seed input and breaks just past it. Construct inputs that make the OLD and NEW behaviour DIFFER — values at and around the boundary the changed condition tests — and check the result is still correct there.

GUARDRAIL (all options): only assert a property that holds for EVERY correct implementation. If you cannot state one, exercise the path WITHOUT asserting — an assertion you can't justify false-positives on a correct patch.
CONSISTENCY CHECKS (these directly support strategy (b) — a summary a defect can leave wrong while the top-level output is masked). Identify from the API IN FRONT OF YOU which exposed values can be cross-checked against an independent computation — a reported aggregate vs a recomputation from the object's own output, a value vs the object's own stated bounds — and for at least one, throw on a mismatch beyond tolerance. These are SOUND (a summary must match the data it summarises), so they will not fire on a correct implementation.
WHAT DOES NOT COUNT: asserting the reported value is finite, non-NaN, non-null, or that no exception was thrown is NOT a consistency check — every wrong-but-finite value passes those, so they can never catch this class of bug. A valid check COMPARES the reported value against a second, independently obtained quantity: the object's own stated bounds, or a recomputation from the object's own output.
SHAPE OF A VALID CHECK (a schema — instantiate every <placeholder> with real calls from the API above):

    var reported = <the value the object claims>;
    var independent = <the SAME quantity obtained a second, independent way>;
    if (<reported disagrees with independent beyond a generous tolerance>)
        throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:<short-id>] consistency violation: " + <both values>);

Ways to obtain the independent value — each sound for ANY correct implementation of ANY library:
  1. LIMITS THE OBJECT ITSELF STATES: a summary of data must lie within that data's own reported extremes (lower/upper, min/max, first/last, 0..capacity).
  2. RECOMPUTATION FROM THE OBJECT'S OWN OUTPUT: aggregate many values/elements the same object produces and compare the aggregate to the reported summary.
  3. A SECOND, IDENTICALLY-CONSTRUCTED OBJECT (or a fresh recomputation of a cached value): both must report the same thing.

Use enough items and a generous tolerance (a few percent, or several standard errors) so noise on a CORRECT implementation never fires; a genuinely wrong value is typically off by far more than any sane tolerance.

POST-CONDITION / METAMORPHIC CHECK (MANDATORY — catches wrong-output bugs that never throw):
ASSUME THE ADVERSARY. The patch under analysis may 'fix' the bug by simply (a) deleting the throw / bookkeeping statement, (b) adding a guard that makes the crashing branch unreachable (and with it the branch's intended behaviour), or (c) replacing the failing operation with one that silently does the wrong thing (e.g. appending instead of sorted-inserting, skipping a modification-counter update). In all three worlds NO exception ever fires and a crash-only harness passes the patch. Therefore, in addition to reproducing the ground-truth failure, your harness MUST assert at least ONE observable post-condition from the documented contract of the patched method that such a patch would violate — e.g. after inserting into an auto-sorted collection, the collection is still sorted; after a removal, an iterator over the container either throws or reflects the removal (never silently yields stale state); a branch that is supposed to set a size/flag/result observably set it. State in a comment WHICH contract guarantee you assert and WHY a throw-deleting patch would break it. A harness that only reproduces the crash is incomplete.

Prefer a post-condition you can read directly off the API after the call. Where none is observable, use ONE metamorphic relation — a relation between two related calls of the target that must hold for ANY correct implementation: compute both sides from REAL library calls on the fuzzed input, and throw if they disagree.
- Round-trip / inverse: f(g(x)) == x  (e.g. decode(encode(x)), parse(format(v)), unescape(escape(s))).
- Idempotence: f(f(x)) == f(x)  (e.g. normalise, trim, strip, canonicalise).
- Equivalent inputs: two inputs that must map to the same result do  (e.g. case-insensitive parse: f(s) == f(s.toUpperCase()); leading-zero / whitespace variants of the same number).
- Composition / split: f(a + b) relates to f(a) and f(b) consistently  (e.g. a translator/encoder applied to a concatenation equals the concatenation of the parts).
- Oracle from the input itself: when the fuzzed input is CONSTRUCTED from a known value, the result must recover it (e.g. build the canonical string for a random int n, parse it, and assert it equals n).

HYGIENE RULES — violating these fires on CORRECT patches (a false positive, the worst outcome):
- If EITHER side of a relation, or the call preceding a post-condition read, throws anything at all, the check does not apply to that input: catch it, skip the check, return normally. NEVER convert a caught exception into a violation — an exception is a rejection, not a wrong answer.
- Before asserting a relation or post-condition, cite in a comment the documented guarantee (javadoc sentence, class contract, or invariant visible in the code shown above) that makes it hold for EVERY correct implementation, including edge cases: null elements, empty inputs, duplicates, no-solution inputs. If you cannot cite one, do not assert it.
- FENCE DEGENERATE INPUTS: construct the inputs you assert on to be non-degenerate BY CONSTRUCTION — non-empty strings and collections, visible (non-blank) labels/names, at least one element where elements are iterated. Contracts are routinely silent about the empty/blank case (a correct implementation may legitimately do nothing for an empty label, return nothing for an empty collection), so a relation whose ONLY violations occur on degenerate inputs is testing unspecified behaviour and will be rejected in review. Assert on the degenerate case ONLY when the documented contract explicitly covers it — and cite that sentence.
- FENCE EXTREME MAGNITUDES the same way: cap fuzzed numeric parameters to moderate ranges by construction (as a rule of thumb, |value| <= 1_000_000 for integers that get multiplied together or fed to combinatorial/statistical formulas) unless the documented contract explicitly covers larger values. At billion-scale parameters a CORRECT implementation's double arithmetic legitimately degrades — internal overflow to NaN, rounding beyond any tolerance, log-gamma saturation — so an assertion whose only violations occur at such magnitudes accuses correct code (this exact pattern produced repeated false accusations: a probability check that sees NaN only at N near 2^31, a validation exception from a parameter range that overflowed). The bug you hunt fires at REASONABLE magnitudes too whenever the contract really is broken.
- CONDITIONAL SIDE EFFECTS ARE NOT UNCONDITIONAL. If the method performs an observable side effect (adds to a collection, increments a counter, populates a cache, sets a field) only INSIDE guards — nested `if`s on collaborator or field state, not just on the input — you may assert that side effect happened ONLY after your harness has established EVERY one of those guard conditions. A correct implementation legitimately does nothing when any guard is unmet (a null owner/collector/context, a disabled flag), so 'the effect always happens after the call' is unsound and fires on correct patches. Read the shown method body: satisfy each guard on the path to the effect, or do not assert the effect.
- Use only real library calls for BOTH sides (no hand-rolled reference implementation).
- On violation, `throw new RuntimeException("[oracle:<short-id>] metamorphic violation: <which relation> input=<...> lhs=<...> rhs=<...>")` with the concrete values, so a reviewer can replay the disagreement. The [oracle:<short-id>] prefix is MANDATORY (checked mechanically) — an un-named alarm is invisible to the per-check acceptance machinery. Jazzer reports the throw as a finding, the same as a crash.

Use ONLY these FuzzedDataProvider methods (no invented overloads):

    int     consumeInt()                    // any int
    int     consumeInt(int min, int max)    // inclusive
    byte    consumeByte()
    boolean consumeBoolean()
    String  consumeString(int maxLength)    // ONE arg
    String  consumeAsciiString(int maxLength)
    String  consumeRemainingAsString()
    byte[]  consumeBytes(int maxLength)
    byte[]  consumeRemainingAsBytes()
    int     remainingBytes()


Complete the skeleton below. Fill in ONLY the `// >>> YOUR CODE HERE <<<` region. Do NOT change the package, import, class name, or entrypoint. Do NOT add a `main` method. Add extra imports only if javac requires them, directly below the existing import.
<skeleton>
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~7,634 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.io.IOException;
import java.io.Reader;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            // Anchor: exact regression test from StrBuilderAppendInsertTest.testLang299.
            anchorScenario();
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            throw t;
        }

        String prefix = data.consumeAsciiString(16);
        String value = data.consumeAsciiString(24);
        if (value.length() == 0) {
            value = "A";
        }

        char padChar = (char) (data.consumeByte() & 0xff);
        int width = data.consumeInt(1, 8);
        int initialCapacity = data.consumeInt(0, 4);

        // Strategy (c): flip the changed boundary. The bug is about ensuring too little capacity
        // before copying the source string when str.length() > width. Stress widths around 1..5
        // and source lengths just above width and above 5 to catch seed-only or +5 overfits.
        String longValue = value;
        while (longValue.length() <= width || longValue.length() <= 5) {
            longValue = longValue + value + "XYZ";
        }
        if (longValue.length() > 24) {
            longValue = longValue.substring(0, 24);
            if (longValue.length() <= width || longValue.length() <= 5) {
                longValue = "ABCDEFG";
            }
        }

        try {
            runNonNullScenario(prefix, longValue, width, padChar, initialCapacity);
        } catch (RuntimeException t) {
            if (isValidation(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }

        // Also drive the getNullText() path with a valid, non-null replacement.
        String nullText = data.consumeAsciiString(12);
        if (nullText.length() == 0) {
            nullText = "NULLTXT";
        }
        int nullWidth = data.consumeInt(1, 8);
        try {
            runNullScenario(prefix, nullText, nullWidth, padChar, initialCapacity);
        } catch (RuntimeException t) {
            if (isValidation(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void anchorScenario() {
        StrBuilder sb = new StrBuilder(1);
        try {
            sb.appendFixedWidthPadRight("foo", 1, '-');
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            throw t;
        }

        // Documented contract of appendFixedWidthPadRight:
        // if strLen >= width, append the first width characters.
        String actual = sb.toString();
        if (!"f".equals(actual)) {
            throw new RuntimeException("[oracle:anchor-post] metamorphic violation: exact regression output input=foo width=1 lhs=" + actual + " rhs=f");
        }

        // Independent observable outside the fixed-width-pad-right family:
        // asReader() must expose the same characters as toString() for the same builder state.
        String readBack = readAll(sb);
        if (!actual.equals(readBack)) {
            throw new RuntimeException("[oracle:anchor-reader] metamorphic violation: reader/toString disagreement lhs=" + actual + " rhs=" + readBack);
        }
    }

    private static void runNonNullScenario(String prefix, String value, int width, char padChar, int initialCapacity) {
        StrBuilder lhs = new StrBuilder(initialCapacity);
        lhs.append(prefix);

        StrBuilder rhs = new StrBuilder(initialCapacity);
        rhs.append(prefix);

        lhs.appendFixedWidthPadRight(value, width, padChar);

        // Contract used for every correct implementation:
        // if strLen >= width, append first width chars; else append string then right padding.
        if (value.length() >= width) {
            rhs.append(value, 0, width);
        } else {
            rhs.append(value);
            rhs.appendPadding(width - value.length(), padChar);
        }

        checkAgreement("nonnull", lhs, rhs);
    }

    private static void runNullScenario(String prefix, String nullText, int width, char padChar, int initialCapacity) {
        StrBuilder lhs = new StrBuilder(initialCapacity);
        lhs.setNullText(nullText);
        lhs.append(prefix);

        StrBuilder rhs = new StrBuilder(initialCapacity);
        rhs.setNullText(nullText);
        rhs.append(prefix);

        lhs.appendFixedWidthPadRight(null, width, padChar);

        if (nullText.length() >= width) {
            rhs.append(nullText, 0, width);
        } else {
            rhs.append(nullText);
            rhs.appendPadding(width - nullText.length(), padChar);
        }

        checkAgreement("nulltext", lhs, rhs);
    }

    private static void checkAgreement(String label, StrBuilder lhs, StrBuilder rhs) {
        String left = lhs.toString();
        String right = rhs.toString();
        if (!left.equals(right)) {
            throw new RuntimeException("[oracle:" + label + "-eq] metamorphic violation: equivalent construction disagrees lhs=" + left + " rhs=" + right);
        }

        // Independent oracle on a different API family:
        // asReader() must reproduce exactly the builder contents already reported by toString().
        String readerView = readAll(lhs);
        if (!left.equals(readerView)) {
            throw new RuntimeException("[oracle:" + label + "-reader] metamorphic violation: reader/toString disagreement lhs=" + left + " rhs=" + readerView);
        }

        // Another direct post-condition from the exposed state:
        // length() and toCharArray().length must agree with the number of characters in toString().
        char[] chars = lhs.toCharArray();
        if (chars.length != lhs.length() || chars.length != left.length()) {
            throw new RuntimeException("[oracle:" + label + "-length] metamorphic violation: inconsistent sizes strLen=" + left.length() + " arrayLen=" + chars.length + " reportedLen=" + lhs.length());
        }
    }

    private static String readAll(StrBuilder sb) {
        Reader r = sb.asReader();
        char[] buf = new char[8];
        StringBuffer out = new StringBuffer();
        try {
            int n;
            while ((n = r.read(buf)) != -1) {
                out.append(buf, 0, n);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return out.toString();
    }

    private static boolean isValidation(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException
                || t instanceof ArrayIndexOutOfBoundsException
                || t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            String cls = st[i].getClassName();
            String method = st[i].getMethodName();
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                    && ("appendFixedWidthPadRight".equals(method)
                    || "ensureCapacity".equals(method)
                    || "getNullText".equals(method))) {
                return true;
            }
        }
        return false;
    }
}
```

</details>

---
## [6] ⚙️ harness-attempt · `attempt_002`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.StringIndexOutOfBoundsException: offset 0, count 3, length 1

---
## [7] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~28,363 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.apache.commons.lang.text` (`package org.apache.commons.lang.text;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Lang_59_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/java/org/apache/commons/lang/text/StrBuilder.java
+++ /src/java/org/apache/commons/lang/text/StrBuilder.java
@@ -877,7 +877,7 @@ public class StrBuilder implements Cloneable {
      */
     public StrBuilder appendFixedWidthPadRight(Object obj, int width, char padChar) {
         if (width > 0) {
-            ensureCapacity(size + width);
+            ensureCapacity((size) + 5);
             String str = (obj == null ? getNullText() : obj.toString());
             int strLen = str.length();
             if (strLen >= width) {

</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.SystemUtils;
</source_imports>

Function `appendFixedWidthPadRight`:
<signature>
public StrBuilder appendFixedWidthPadRight(Object obj, int width, char padChar)
</signature>
<code>
    public StrBuilder appendFixedWidthPadRight(Object obj, int width, char padChar) {
        if (width > 0) {
            ensureCapacity(size + width);
            String str = (obj == null ? getNullText() : obj.toString());
            int strLen = str.length();
            if (strLen >= width) {
                str.getChars(0, strLen, buffer, size);
            } else {
                int padLen = width - strLen;
                str.getChars(0, strLen, buffer, size);
                for (int i = 0; i < padLen; i++) {
                    buffer[size + strLen + i] = padChar;
                }
            }
            size += width;
        }
        return this;
    }
</code>
Methods called by `appendFixedWidthPadRight` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="ensureCapacity" from="StrBuilder.java">
<signature>
public StrBuilder ensureCapacity(int capacity)
</signature>
<code>
    public StrBuilder ensureCapacity(int capacity) {
        if (capacity > buffer.length) {
            char[] old = buffer;
            buffer = new char[capacity];
            System.arraycopy(old, 0, buffer, 0, size);
        }
        return this;
    }
</code>
</callee>
<callee name="getNullText" from="StrBuilder.java">
<signature>
public String getNullText()
</signature>
<code>
    public String getNullText() {
        return nullText;
    }
</code>
</callee>
<callee name="toString" from="StrBuilder.java">
<signature>
public String toString()
</signature>
<code>
    public String toString() {
        return new String(buffer, 0, size);
    }
</code>
<implementation in="StrBuilder.java">
    public String toString() {
        return new String(buffer, 0, size);
    }
</implementation>
<implementation in="StrTokenizer.java">
    public String toString() {
        if (tokens == null) {
            return "StrTokenizer[not tokenized yet]";
        } else {
            return "StrTokenizer" + getTokenList();
        }
    }
</implementation>
</callee>
<callee name="length" from="StrBuilder.java">
<signature>
public int length()
</signature>
<code>
    public int length() {
        return size;
    }
</code>
</callee>
<callee name="getChars" from="StrBuilder.java">
<signature>
public char[] getChars(char[] destination)
</signature>
<code>
    public char[] getChars(char[] destination) {
        int len = length();
        if (destination == null || destination.length < len) {
            destination = new char[len];
        }
        System.arraycopy(buffer, 0, destination, 0, len);
        return destination;
    }
</code>
<implementation in="StrBuilder.java">
    public char[] getChars(char[] destination) {
        int len = length();
        if (destination == null || destination.length < len) {
            destination = new char[len];
        }
        System.arraycopy(buffer, 0, destination, 0, len);
        return destination;
    }
</implementation>
<implementation in="StrBuilder.java">
    public void getChars(int startIndex, int endIndex, char destination[], int destinationIndex) {
        if (startIndex < 0) {
            throw new StringIndexOutOfBoundsException(startIndex);
        }
        if (endIndex < 0 || endIndex > length()) {
            throw new StringIndexOutOfBoundsException(endIndex);
        }
        if (startIndex > endIndex) {
            throw new StringIndexOutOfBoundsException("end < start");
        }
        System.arraycopy(buffer, startIndex, destination, destinationIndex, endIndex - startIndex);
    }
</implementation>
</callee>
STATE COUPLING: these members of the same class share the listed field(s) with `appendFixedWidthPadRight` but neither calls the other — state one of them writes is what the other reports. A defect the patch leaves (or introduces) in that shared state stays observable through these members even when `appendFixedWidthPadRight`'s own output looks right, so a strong harness CHECKS THEY AGREE: compare what a reader reports against what the writer/constructor established.
  - `public StrBuilder setLength(int length)` (shared field(s): buffer, size)
      doc: Updates the length of the builder by either dropping the last characters or adding filler of unicode zero. @param length  the length to set to, must be zero or positive @return this, to enable chaining @throws IndexOutOfBoundsException if the length is negative
  - `public StrBuilder minimizeCapacity()` (shared field(s): buffer, size)
      doc: Minimizes the capacity to the actual length of the string. @return this, to enable chaining
  - `public char[] toCharArray()` (shared field(s): buffer, size)
      doc: Copies the builder's character array into a new character array. @return a new array that represents the contents of the builder
  - `public StrBuilder append(String str)` (shared field(s): buffer, size)
      doc: Appends a string to this string builder. Appending null will call {@link #appendNull()}. @param str  the string to append @return this, to enable chaining
  - `public StrBuilder append(String str, int startIndex, int length)` (shared field(s): buffer, size)
      doc: Appends part of a string to this string builder. Appending null will call {@link #appendNull()}. @param str  the string to append @param startIndex  the start index, inclusive, must be valid @param length  the length to append, must be valid @return this, to enable chaining

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `StrBuilderAppendInsert`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.StringIndexOutOfBoundsException, java.lang.ArrayIndexOutOfBoundsException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `appendFixedWidthPadRight` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: java.lang.StringIndexOutOfBoundsException
message: offset 0, count 3, length 1
thrown_at: org.apache.commons.lang.text.StrBuilder.appendFixedWidthPadRight(StrBuilder.java:884)
</ground_truth_crash>
<failing_test class="org.apache.commons.lang.text.StrBuilderAppendInsertTest" method="testLang299">
    public void testLang299() {
        StrBuilder sb = new StrBuilder(1);
        sb.appendFixedWidthPadRight("foo", 1, '-');
        assertEquals("f", sb.toString());
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.lang.text.StrBuilderAppendInsertTest::testLang299
java.lang.StringIndexOutOfBoundsException: offset 0, count 3, length 1
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  append(Object obj) / (String str) / (String str, int startIndex, int length) / (StringBuffer str) / (StringBuffer str, int startIndex, int length) / (StrBuilder str) / (StrBuilder str, int startIndex, int length) / (char[] chars) / (char[] chars, int startIndex, int length) / (boolean value) / (char ch) / (int value) / (long value) / (float value) / (double value)
  appendFixedWidthPadLeft(Object obj, int width, char padChar) / (int value, int width, char padChar)
  appendFixedWidthPadRight(Object obj, int width, char padChar) / (int value, int width, char padChar)
  appendWithSeparators(Object[] array, String separator) / (Collection coll, String separator) / (Iterator it, String separator)
  contains(char ch) / (String str) / (StrMatcher matcher)
  deleteAll(char ch) / (String str) / (StrMatcher matcher)
  deleteFirst(char ch) / (String str) / (StrMatcher matcher)
  equals(StrBuilder other) / (Object obj)

METHOD FAMILIES over the same input space (factory/parser siblings — where the docs state a selection or agreement rule between family members, equivalent inputs must respect it):
  append* family: appendFixedWidthPadLeft, appendFixedWidthPadRight, appendNewLine, appendNull, appendPadding, appendWithSeparators
  as* family: asReader, asTokenizer

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- StrBuilder.ensureCapacity
- StrBuilder.getNullText
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.lang.text.FuzzHarness.anchorScenario
- org.apache.commons.lang.text.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.lang.text.FuzzHarness.runScenario
- org.apache.commons.lang.text.StrBuilder.appendFixedWidthPadRight
Crashes already found:
- java.lang.StringIndexOutOfBoundsException@org.apache.commons.lang.text.StrBuilder.appendFixedWidthPadRight
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- StrBuilder.ensureCapacity
- StrBuilder.getNullText
Check FAMILIES already covered by accepted harnesses: {anchor-post, anchor-reader, fixed-width-pad-right, minimize-noop, nonnull, nulltext, state-consistency}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.StringIndexOutOfBoundsException@org.apache.commons.lang.text.StrBuilder.appendFixedWidthPadRight. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
VARIANT STRATEGY — this harness is one of a set that divides the strategies below so each is tried. USE STRATEGY (a) for THIS harness (fall back to another ONLY if it is clearly inapplicable to this patch). The patch may be a band-aid that fixed the reported instance while the SAME root cause still bites elsewhere in this region:

  (a) DIFFERENT REACHABLE FUNCTION (coverage spread). Shape the input so a different function in the region above runs / a different failure signature appears. Good when the same root-cause pattern recurs across several functions and the patch only fixed one.

  (b) CONSISTENCY CROSS-CHECK on a masked helper. A defect can leave the patched function's top-level output CORRECT because a downstream step (an iterative solve, a convergence loop, a clamp, a re-normalisation) absorbs the error — while a reachable HELPER that produces a RELATED quantity stays wrong (the unfixed root cause). Rather than guess that helper's exact value, compute the SAME quantity TWO independent ways and assert they AGREE — a check that is sound without knowing the answer. Generic patterns: a stated summary/aggregate must match the EMPIRICAL value recomputed over many items the same object produces; a reported size/count must match a manual count of the elements; a cached/precomputed value must equal a fresh recomputation; equal objects must have equal hashCodes. Use a tolerance for floating-point/statistical estimates and require enough items, so a CORRECT implementation never fires.

  (c) FLIP THE PATCHED CONDITION (overfit on the seed). The patch changed a specific line/condition. An overfitting fix often special-cases the seed input and breaks just past it. Construct inputs that make the OLD and NEW behaviour DIFFER — values at and around the boundary the changed condition tests — and check the result is still correct there.

GUARDRAIL (all options): only assert a property that holds for EVERY correct implementation. If you cannot state one, exercise the path WITHOUT asserting — an assertion you can't justify false-positives on a correct patch.
CONSISTENCY CHECKS (these directly support strategy (b) — a summary a defect can leave wrong while the top-level output is masked). Identify from the API IN FRONT OF YOU which exposed values can be cross-checked against an independent computation — a reported aggregate vs a recomputation from the object's own output, a value vs the object's own stated bounds — and for at least one, throw on a mismatch beyond tolerance. These are SOUND (a summary must match the data it summarises), so they will not fire on a correct implementation.
WHAT DOES NOT COUNT: asserting the reported value is finite, non-NaN, non-null, or that no exception was thrown is NOT a consistency check — every wrong-but-finite value passes those, so they can never catch this class of bug. A valid check COMPARES the reported value against a second, independently obtained quantity: the object's own stated bounds, or a recomputation from the object's own output.
SHAPE OF A VALID CHECK (a schema — instantiate every <placeholder> with real calls from the API above):

    var reported = <the value the object claims>;
    var independent = <the SAME quantity obtained a second, independent way>;
    if (<reported disagrees with independent beyond a generous tolerance>)
        throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:<short-id>] consistency violation: " + <both values>);

Ways to obtain the independent value — each sound for ANY correct implementation of ANY library:
  1. LIMITS THE OBJECT ITSELF STATES: a summary of data must lie within that data's own reported extremes (lower/upper, min/max, first/last, 0..capacity).
  2. RECOMPUTATION FROM THE OBJECT'S OWN OUTPUT: aggregate many values/elements the same object produces and compare the aggregate to the reported summary.
  3. A SECOND, IDENTICALLY-CONSTRUCTED OBJECT (or a fresh recomputation of a cached value): both must report the same thing.

Use enough items and a generous tolerance (a few percent, or several standard errors) so noise on a CORRECT implementation never fires; a genuinely wrong value is typically off by far more than any sane tolerance.

POST-CONDITION / METAMORPHIC CHECK (MANDATORY — catches wrong-output bugs that never throw):
ASSUME THE ADVERSARY. The patch under analysis may 'fix' the bug by simply (a) deleting the throw / bookkeeping statement, (b) adding a guard that makes the crashing branch unreachable (and with it the branch's intended behaviour), or (c) replacing the failing operation with one that silently does the wrong thing (e.g. appending instead of sorted-inserting, skipping a modification-counter update). In all three worlds NO exception ever fires and a crash-only harness passes the patch. Therefore, in addition to reproducing the ground-truth failure, your harness MUST assert at least ONE observable post-condition from the documented contract of the patched method that such a patch would violate — e.g. after inserting into an auto-sorted collection, the collection is still sorted; after a removal, an iterator over the container either throws or reflects the removal (never silently yields stale state); a branch that is supposed to set a size/flag/result observably set it. State in a comment WHICH contract guarantee you assert and WHY a throw-deleting patch would break it. A harness that only reproduces the crash is incomplete.

Prefer a post-condition you can read directly off the API after the call. Where none is observable, use ONE metamorphic relation — a relation between two related calls of the target that must hold for ANY correct implementation: compute both sides from REAL library calls on the fuzzed input, and throw if they disagree.
- Round-trip / inverse: f(g(x)) == x  (e.g. decode(encode(x)), parse(format(v)), unescape(escape(s))).
- Idempotence: f(f(x)) == f(x)  (e.g. normalise, trim, strip, canonicalise).
- Equivalent inputs: two inputs that must map to the same result do  (e.g. case-insensitive parse: f(s) == f(s.toUpperCase()); leading-zero / whitespace variants of the same number).
- Composition / split: f(a + b) relates to f(a) and f(b) consistently  (e.g. a translator/encoder applied to a concatenation equals the concatenation of the parts).
- Oracle from the input itself: when the fuzzed input is CONSTRUCTED from a known value, the result must recover it (e.g. build the canonical string for a random int n, parse it, and assert it equals n).

HYGIENE RULES — violating these fires on CORRECT patches (a false positive, the worst outcome):
- If EITHER side of a relation, or the call preceding a post-condition read, throws anything at all, the check does not apply to that input: catch it, skip the check, return normally. NEVER convert a caught exception into a violation — an exception is a rejection, not a wrong answer.
- Before asserting a relation or post-condition, cite in a comment the documented guarantee (javadoc sentence, class contract, or invariant visible in the code shown above) that makes it hold for EVERY correct implementation, including edge cases: null elements, empty inputs, duplicates, no-solution inputs. If you cannot cite one, do not assert it.
- FENCE DEGENERATE INPUTS: construct the inputs you assert on to be non-degenerate BY CONSTRUCTION — non-empty strings and collections, visible (non-blank) labels/names, at least one element where elements are iterated. Contracts are routinely silent about the empty/blank case (a correct implementation may legitimately do nothing for an empty label, return nothing for an empty collection), so a relation whose ONLY violations occur on degenerate inputs is testing unspecified behaviour and will be rejected in review. Assert on the degenerate case ONLY when the documented contract explicitly covers it — and cite that sentence.
- FENCE EXTREME MAGNITUDES the same way: cap fuzzed numeric parameters to moderate ranges by construction (as a rule of thumb, |value| <= 1_000_000 for integers that get multiplied together or fed to combinatorial/statistical formulas) unless the documented contract explicitly covers larger values. At billion-scale parameters a CORRECT implementation's double arithmetic legitimately degrades — internal overflow to NaN, rounding beyond any tolerance, log-gamma saturation — so an assertion whose only violations occur at such magnitudes accuses correct code (this exact pattern produced repeated false accusations: a probability check that sees NaN only at N near 2^31, a validation exception from a parameter range that overflowed). The bug you hunt fires at REASONABLE magnitudes too whenever the contract really is broken.
- CONDITIONAL SIDE EFFECTS ARE NOT UNCONDITIONAL. If the method performs an observable side effect (adds to a collection, increments a counter, populates a cache, sets a field) only INSIDE guards — nested `if`s on collaborator or field state, not just on the input — you may assert that side effect happened ONLY after your harness has established EVERY one of those guard conditions. A correct implementation legitimately does nothing when any guard is unmet (a null owner/collector/context, a disabled flag), so 'the effect always happens after the call' is unsound and fires on correct patches. Read the shown method body: satisfy each guard on the path to the effect, or do not assert the effect.
- Use only real library calls for BOTH sides (no hand-rolled reference implementation).
- On violation, `throw new RuntimeException("[oracle:<short-id>] metamorphic violation: <which relation> input=<...> lhs=<...> rhs=<...>")` with the concrete values, so a reviewer can replay the disagreement. The [oracle:<short-id>] prefix is MANDATORY (checked mechanically) — an un-named alarm is invisible to the per-check acceptance machinery. Jazzer reports the throw as a finding, the same as a crash.

Use ONLY these FuzzedDataProvider methods (no invented overloads):

    int     consumeInt()                    // any int
    int     consumeInt(int min, int max)    // inclusive
    byte    consumeByte()
    boolean consumeBoolean()
    String  consumeString(int maxLength)    // ONE arg
    String  consumeAsciiString(int maxLength)
    String  consumeRemainingAsString()
    byte[]  consumeBytes(int maxLength)
    byte[]  consumeRemainingAsBytes()
    int     remainingBytes()


Complete the skeleton below. Fill in ONLY the `// >>> YOUR CODE HERE <<<` region. Do NOT change the package, import, class name, or entrypoint. Do NOT add a `main` method. Add extra imports only if javac requires them, directly below the existing import.
<skeleton>
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~4,866 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchorScenario();

        String seed = data.consumeString(32);
        String nullText = data.consumeString(32);
        char pad = (char) (data.consumeByte() & 0xff);
        int width = data.consumeInt(1, 24);
        int initialCapacity = data.consumeInt(0, 4);
        boolean useNullObject = data.consumeBoolean();

        String src = seed;
        if (src == null) {
            src = "";
        }
        while (src.length() <= width) {
            src = src + "X";
        }

        if (nullText == null) {
            nullText = "";
        }
        while (nullText.length() <= width) {
            nullText = nullText + "N";
        }

        if (useNullObject) {
            runScenario(null, nullText, width, pad, initialCapacity);
        } else {
            runScenario(src, null, width, pad, initialCapacity);
        }
    }

    private static void anchorScenario() {
        StrBuilder sb = new StrBuilder(1);
        try {
            sb.appendFixedWidthPadRight("foo", 1, '-');
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        String actual = sb.toString();
        if (!"f".equals(actual)) {
            throw new RuntimeException("[oracle:anchor-truncate] metamorphic violation: appendFixedWidthPadRight should keep the leftmost width characters for overlong input input=foo width=1 lhs=" + actual + " rhs=f");
        }
    }

    private static void runScenario(Object obj, String configuredNullText, int width, char padChar, int initialCapacity) {
        String effective = (obj == null) ? configuredNullText : obj.toString();
        if (effective == null || width <= 0 || effective.length() < width) {
            return;
        }

        StrBuilder target = new StrBuilder(initialCapacity);
        if (obj == null) {
            target.setNullText(configuredNullText);
        }

        try {
            target.appendFixedWidthPadRight(obj, width, padChar);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        StrBuilder expected = new StrBuilder(Math.max(initialCapacity, width));
        try {
            expected.append(effective, 0, width);
        } catch (RuntimeException t) {
            return;
        }

        String actual;
        String oracle;
        try {
            actual = target.toString();
            oracle = expected.toString();
        } catch (RuntimeException t) {
            return;
        }

        /* Contract used for this oracle:
         * appendFixedWidthPadRight(obj, width, padChar) appends exactly width characters;
         * when the source string is at least width long, the appended content is the leftmost width
         * characters of that source. append(String, startIndex, length) with (0, width) constructs
         * that same quantity through a different public API path. A throw-deleting or skip-writing
         * patch would make these disagree even if no exception is thrown.
         */
        if (!actual.equals(oracle)) {
            throw new RuntimeException("[oracle:sibling-prefix] metamorphic violation: fixed-width right padding/truncation disagrees with append(str,0,width) input=" + effective + " width=" + width + " lhs=" + actual + " rhs=" + oracle);
        }

        if (target.length() != oracle.length()) {
            throw new RuntimeException("[oracle:length-agreement] metamorphic violation: reported length disagrees with sibling result input=" + effective + " width=" + width + " lhs=" + target.length() + " rhs=" + oracle.length());
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())) {
                String method = ste.getMethodName();
                if ("appendFixedWidthPadRight".equals(method)
                        || "ensureCapacity".equals(method)
                        || "getNullText".equals(method)) {
                    return true;
                }
            }
        }
        return false;
    }
}
```

</details>

---
## [8] ⚙️ harness-attempt · `attempt_003`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.StringIndexOutOfBoundsException: offset 0, count 3, length 1

---
## [9] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~28,414 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.apache.commons.lang.text` (`package org.apache.commons.lang.text;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Lang_59_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/java/org/apache/commons/lang/text/StrBuilder.java
+++ /src/java/org/apache/commons/lang/text/StrBuilder.java
@@ -877,7 +877,7 @@ public class StrBuilder implements Cloneable {
      */
     public StrBuilder appendFixedWidthPadRight(Object obj, int width, char padChar) {
         if (width > 0) {
-            ensureCapacity(size + width);
+            ensureCapacity((size) + 5);
             String str = (obj == null ? getNullText() : obj.toString());
             int strLen = str.length();
             if (strLen >= width) {

</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.SystemUtils;
</source_imports>

Function `appendFixedWidthPadRight`:
<signature>
public StrBuilder appendFixedWidthPadRight(Object obj, int width, char padChar)
</signature>
<code>
    public StrBuilder appendFixedWidthPadRight(Object obj, int width, char padChar) {
        if (width > 0) {
            ensureCapacity(size + width);
            String str = (obj == null ? getNullText() : obj.toString());
            int strLen = str.length();
            if (strLen >= width) {
                str.getChars(0, strLen, buffer, size);
            } else {
                int padLen = width - strLen;
                str.getChars(0, strLen, buffer, size);
                for (int i = 0; i < padLen; i++) {
                    buffer[size + strLen + i] = padChar;
                }
            }
            size += width;
        }
        return this;
    }
</code>
Methods called by `appendFixedWidthPadRight` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="ensureCapacity" from="StrBuilder.java">
<signature>
public StrBuilder ensureCapacity(int capacity)
</signature>
<code>
    public StrBuilder ensureCapacity(int capacity) {
        if (capacity > buffer.length) {
            char[] old = buffer;
            buffer = new char[capacity];
            System.arraycopy(old, 0, buffer, 0, size);
        }
        return this;
    }
</code>
</callee>
<callee name="getNullText" from="StrBuilder.java">
<signature>
public String getNullText()
</signature>
<code>
    public String getNullText() {
        return nullText;
    }
</code>
</callee>
<callee name="toString" from="StrBuilder.java">
<signature>
public String toString()
</signature>
<code>
    public String toString() {
        return new String(buffer, 0, size);
    }
</code>
<implementation in="StrBuilder.java">
    public String toString() {
        return new String(buffer, 0, size);
    }
</implementation>
<implementation in="StrTokenizer.java">
    public String toString() {
        if (tokens == null) {
            return "StrTokenizer[not tokenized yet]";
        } else {
            return "StrTokenizer" + getTokenList();
        }
    }
</implementation>
</callee>
<callee name="length" from="StrBuilder.java">
<signature>
public int length()
</signature>
<code>
    public int length() {
        return size;
    }
</code>
</callee>
<callee name="getChars" from="StrBuilder.java">
<signature>
public char[] getChars(char[] destination)
</signature>
<code>
    public char[] getChars(char[] destination) {
        int len = length();
        if (destination == null || destination.length < len) {
            destination = new char[len];
        }
        System.arraycopy(buffer, 0, destination, 0, len);
        return destination;
    }
</code>
<implementation in="StrBuilder.java">
    public char[] getChars(char[] destination) {
        int len = length();
        if (destination == null || destination.length < len) {
            destination = new char[len];
        }
        System.arraycopy(buffer, 0, destination, 0, len);
        return destination;
    }
</implementation>
<implementation in="StrBuilder.java">
    public void getChars(int startIndex, int endIndex, char destination[], int destinationIndex) {
        if (startIndex < 0) {
            throw new StringIndexOutOfBoundsException(startIndex);
        }
        if (endIndex < 0 || endIndex > length()) {
            throw new StringIndexOutOfBoundsException(endIndex);
        }
        if (startIndex > endIndex) {
            throw new StringIndexOutOfBoundsException("end < start");
        }
        System.arraycopy(buffer, startIndex, destination, destinationIndex, endIndex - startIndex);
    }
</implementation>
</callee>
STATE COUPLING: these members of the same class share the listed field(s) with `appendFixedWidthPadRight` but neither calls the other — state one of them writes is what the other reports. A defect the patch leaves (or introduces) in that shared state stays observable through these members even when `appendFixedWidthPadRight`'s own output looks right, so a strong harness CHECKS THEY AGREE: compare what a reader reports against what the writer/constructor established.
  - `public StrBuilder setLength(int length)` (shared field(s): buffer, size)
      doc: Updates the length of the builder by either dropping the last characters or adding filler of unicode zero. @param length  the length to set to, must be zero or positive @return this, to enable chaining @throws IndexOutOfBoundsException if the length is negative
  - `public StrBuilder minimizeCapacity()` (shared field(s): buffer, size)
      doc: Minimizes the capacity to the actual length of the string. @return this, to enable chaining
  - `public char[] toCharArray()` (shared field(s): buffer, size)
      doc: Copies the builder's character array into a new character array. @return a new array that represents the contents of the builder
  - `public StrBuilder append(String str)` (shared field(s): buffer, size)
      doc: Appends a string to this string builder. Appending null will call {@link #appendNull()}. @param str  the string to append @return this, to enable chaining
  - `public StrBuilder append(String str, int startIndex, int length)` (shared field(s): buffer, size)
      doc: Appends part of a string to this string builder. Appending null will call {@link #appendNull()}. @param str  the string to append @param startIndex  the start index, inclusive, must be valid @param length  the length to append, must be valid @return this, to enable chaining

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `StrBuilderAppendInsert`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.StringIndexOutOfBoundsException, java.lang.ArrayIndexOutOfBoundsException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `appendFixedWidthPadRight` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: java.lang.StringIndexOutOfBoundsException
message: offset 0, count 3, length 1
thrown_at: org.apache.commons.lang.text.StrBuilder.appendFixedWidthPadRight(StrBuilder.java:884)
</ground_truth_crash>
<failing_test class="org.apache.commons.lang.text.StrBuilderAppendInsertTest" method="testLang299">
    public void testLang299() {
        StrBuilder sb = new StrBuilder(1);
        sb.appendFixedWidthPadRight("foo", 1, '-');
        assertEquals("f", sb.toString());
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.lang.text.StrBuilderAppendInsertTest::testLang299
java.lang.StringIndexOutOfBoundsException: offset 0, count 3, length 1
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  append(Object obj) / (String str) / (String str, int startIndex, int length) / (StringBuffer str) / (StringBuffer str, int startIndex, int length) / (StrBuilder str) / (StrBuilder str, int startIndex, int length) / (char[] chars) / (char[] chars, int startIndex, int length) / (boolean value) / (char ch) / (int value) / (long value) / (float value) / (double value)
  appendFixedWidthPadLeft(Object obj, int width, char padChar) / (int value, int width, char padChar)
  appendFixedWidthPadRight(Object obj, int width, char padChar) / (int value, int width, char padChar)
  appendWithSeparators(Object[] array, String separator) / (Collection coll, String separator) / (Iterator it, String separator)
  contains(char ch) / (String str) / (StrMatcher matcher)
  deleteAll(char ch) / (String str) / (StrMatcher matcher)
  deleteFirst(char ch) / (String str) / (StrMatcher matcher)
  equals(StrBuilder other) / (Object obj)

METHOD FAMILIES over the same input space (factory/parser siblings — where the docs state a selection or agreement rule between family members, equivalent inputs must respect it):
  append* family: appendFixedWidthPadLeft, appendFixedWidthPadRight, appendNewLine, appendNull, appendPadding, appendWithSeparators
  as* family: asReader, asTokenizer

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- StrBuilder.ensureCapacity
- StrBuilder.getNullText
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.lang.text.FuzzHarness.anchorScenario
- org.apache.commons.lang.text.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.lang.text.FuzzHarness.runScenario
- org.apache.commons.lang.text.StrBuilder.appendFixedWidthPadRight
Crashes already found:
- java.lang.StringIndexOutOfBoundsException@org.apache.commons.lang.text.StrBuilder.appendFixedWidthPadRight
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- StrBuilder.ensureCapacity
- StrBuilder.getNullText
Check FAMILIES already covered by accepted harnesses: {anchor-post, anchor-reader, anchor-truncate, fixed-width-pad-right, length-agreement, minimize-noop, nonnull, nulltext, sibling-prefix, state-consistency}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.StringIndexOutOfBoundsException@org.apache.commons.lang.text.StrBuilder.appendFixedWidthPadRight. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
VARIANT STRATEGY — this harness is one of a set that divides the strategies below so each is tried. USE STRATEGY (b) for THIS harness (fall back to another ONLY if it is clearly inapplicable to this patch). The patch may be a band-aid that fixed the reported instance while the SAME root cause still bites elsewhere in this region:

  (a) DIFFERENT REACHABLE FUNCTION (coverage spread). Shape the input so a different function in the region above runs / a different failure signature appears. Good when the same root-cause pattern recurs across several functions and the patch only fixed one.

  (b) CONSISTENCY CROSS-CHECK on a masked helper. A defect can leave the patched function's top-level output CORRECT because a downstream step (an iterative solve, a convergence loop, a clamp, a re-normalisation) absorbs the error — while a reachable HELPER that produces a RELATED quantity stays wrong (the unfixed root cause). Rather than guess that helper's exact value, compute the SAME quantity TWO independent ways and assert they AGREE — a check that is sound without knowing the answer. Generic patterns: a stated summary/aggregate must match the EMPIRICAL value recomputed over many items the same object produces; a reported size/count must match a manual count of the elements; a cached/precomputed value must equal a fresh recomputation; equal objects must have equal hashCodes. Use a tolerance for floating-point/statistical estimates and require enough items, so a CORRECT implementation never fires.

  (c) FLIP THE PATCHED CONDITION (overfit on the seed). The patch changed a specific line/condition. An overfitting fix often special-cases the seed input and breaks just past it. Construct inputs that make the OLD and NEW behaviour DIFFER — values at and around the boundary the changed condition tests — and check the result is still correct there.

GUARDRAIL (all options): only assert a property that holds for EVERY correct implementation. If you cannot state one, exercise the path WITHOUT asserting — an assertion you can't justify false-positives on a correct patch.
CONSISTENCY CHECKS (these directly support strategy (b) — a summary a defect can leave wrong while the top-level output is masked). Identify from the API IN FRONT OF YOU which exposed values can be cross-checked against an independent computation — a reported aggregate vs a recomputation from the object's own output, a value vs the object's own stated bounds — and for at least one, throw on a mismatch beyond tolerance. These are SOUND (a summary must match the data it summarises), so they will not fire on a correct implementation.
WHAT DOES NOT COUNT: asserting the reported value is finite, non-NaN, non-null, or that no exception was thrown is NOT a consistency check — every wrong-but-finite value passes those, so they can never catch this class of bug. A valid check COMPARES the reported value against a second, independently obtained quantity: the object's own stated bounds, or a recomputation from the object's own output.
SHAPE OF A VALID CHECK (a schema — instantiate every <placeholder> with real calls from the API above):

    var reported = <the value the object claims>;
    var independent = <the SAME quantity obtained a second, independent way>;
    if (<reported disagrees with independent beyond a generous tolerance>)
        throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:<short-id>] consistency violation: " + <both values>);

Ways to obtain the independent value — each sound for ANY correct implementation of ANY library:
  1. LIMITS THE OBJECT ITSELF STATES: a summary of data must lie within that data's own reported extremes (lower/upper, min/max, first/last, 0..capacity).
  2. RECOMPUTATION FROM THE OBJECT'S OWN OUTPUT: aggregate many values/elements the same object produces and compare the aggregate to the reported summary.
  3. A SECOND, IDENTICALLY-CONSTRUCTED OBJECT (or a fresh recomputation of a cached value): both must report the same thing.

Use enough items and a generous tolerance (a few percent, or several standard errors) so noise on a CORRECT implementation never fires; a genuinely wrong value is typically off by far more than any sane tolerance.

POST-CONDITION / METAMORPHIC CHECK (MANDATORY — catches wrong-output bugs that never throw):
ASSUME THE ADVERSARY. The patch under analysis may 'fix' the bug by simply (a) deleting the throw / bookkeeping statement, (b) adding a guard that makes the crashing branch unreachable (and with it the branch's intended behaviour), or (c) replacing the failing operation with one that silently does the wrong thing (e.g. appending instead of sorted-inserting, skipping a modification-counter update). In all three worlds NO exception ever fires and a crash-only harness passes the patch. Therefore, in addition to reproducing the ground-truth failure, your harness MUST assert at least ONE observable post-condition from the documented contract of the patched method that such a patch would violate — e.g. after inserting into an auto-sorted collection, the collection is still sorted; after a removal, an iterator over the container either throws or reflects the removal (never silently yields stale state); a branch that is supposed to set a size/flag/result observably set it. State in a comment WHICH contract guarantee you assert and WHY a throw-deleting patch would break it. A harness that only reproduces the crash is incomplete.

Prefer a post-condition you can read directly off the API after the call. Where none is observable, use ONE metamorphic relation — a relation between two related calls of the target that must hold for ANY correct implementation: compute both sides from REAL library calls on the fuzzed input, and throw if they disagree.
- Round-trip / inverse: f(g(x)) == x  (e.g. decode(encode(x)), parse(format(v)), unescape(escape(s))).
- Idempotence: f(f(x)) == f(x)  (e.g. normalise, trim, strip, canonicalise).
- Equivalent inputs: two inputs that must map to the same result do  (e.g. case-insensitive parse: f(s) == f(s.toUpperCase()); leading-zero / whitespace variants of the same number).
- Composition / split: f(a + b) relates to f(a) and f(b) consistently  (e.g. a translator/encoder applied to a concatenation equals the concatenation of the parts).
- Oracle from the input itself: when the fuzzed input is CONSTRUCTED from a known value, the result must recover it (e.g. build the canonical string for a random int n, parse it, and assert it equals n).

HYGIENE RULES — violating these fires on CORRECT patches (a false positive, the worst outcome):
- If EITHER side of a relation, or the call preceding a post-condition read, throws anything at all, the check does not apply to that input: catch it, skip the check, return normally. NEVER convert a caught exception into a violation — an exception is a rejection, not a wrong answer.
- Before asserting a relation or post-condition, cite in a comment the documented guarantee (javadoc sentence, class contract, or invariant visible in the code shown above) that makes it hold for EVERY correct implementation, including edge cases: null elements, empty inputs, duplicates, no-solution inputs. If you cannot cite one, do not assert it.
- FENCE DEGENERATE INPUTS: construct the inputs you assert on to be non-degenerate BY CONSTRUCTION — non-empty strings and collections, visible (non-blank) labels/names, at least one element where elements are iterated. Contracts are routinely silent about the empty/blank case (a correct implementation may legitimately do nothing for an empty label, return nothing for an empty collection), so a relation whose ONLY violations occur on degenerate inputs is testing unspecified behaviour and will be rejected in review. Assert on the degenerate case ONLY when the documented contract explicitly covers it — and cite that sentence.
- FENCE EXTREME MAGNITUDES the same way: cap fuzzed numeric parameters to moderate ranges by construction (as a rule of thumb, |value| <= 1_000_000 for integers that get multiplied together or fed to combinatorial/statistical formulas) unless the documented contract explicitly covers larger values. At billion-scale parameters a CORRECT implementation's double arithmetic legitimately degrades — internal overflow to NaN, rounding beyond any tolerance, log-gamma saturation — so an assertion whose only violations occur at such magnitudes accuses correct code (this exact pattern produced repeated false accusations: a probability check that sees NaN only at N near 2^31, a validation exception from a parameter range that overflowed). The bug you hunt fires at REASONABLE magnitudes too whenever the contract really is broken.
- CONDITIONAL SIDE EFFECTS ARE NOT UNCONDITIONAL. If the method performs an observable side effect (adds to a collection, increments a counter, populates a cache, sets a field) only INSIDE guards — nested `if`s on collaborator or field state, not just on the input — you may assert that side effect happened ONLY after your harness has established EVERY one of those guard conditions. A correct implementation legitimately does nothing when any guard is unmet (a null owner/collector/context, a disabled flag), so 'the effect always happens after the call' is unsound and fires on correct patches. Read the shown method body: satisfy each guard on the path to the effect, or do not assert the effect.
- Use only real library calls for BOTH sides (no hand-rolled reference implementation).
- On violation, `throw new RuntimeException("[oracle:<short-id>] metamorphic violation: <which relation> input=<...> lhs=<...> rhs=<...>")` with the concrete values, so a reviewer can replay the disagreement. The [oracle:<short-id>] prefix is MANDATORY (checked mechanically) — an un-named alarm is invisible to the per-check acceptance machinery. Jazzer reports the throw as a finding, the same as a crash.

Use ONLY these FuzzedDataProvider methods (no invented overloads):

    int     consumeInt()                    // any int
    int     consumeInt(int min, int max)    // inclusive
    byte    consumeByte()
    boolean consumeBoolean()
    String  consumeString(int maxLength)    // ONE arg
    String  consumeAsciiString(int maxLength)
    String  consumeRemainingAsString()
    byte[]  consumeBytes(int maxLength)
    byte[]  consumeRemainingAsBytes()
    int     remainingBytes()


Complete the skeleton below. Fill in ONLY the `// >>> YOUR CODE HERE <<<` region. Do NOT change the package, import, class name, or entrypoint. Do NOT add a `main` method. Add extra imports only if javac requires them, directly below the existing import.
<skeleton>
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~6,920 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Contract used below:
        // - appendFixedWidthPadRight(obj, width, padChar) appends exactly width characters:
        //   if obj.toString().length() >= width, it appends the leftmost width chars;
        //   otherwise it appends the full string then pads on the right.
        // Therefore, for valid inputs, it must agree with a composition of other real StrBuilder APIs:
        //   append(str, 0, width)                     when str.length() >= width
        //   append(str).appendPadding(width-len, ch)  when str.length() < width
        //
        // This catches both the historical crash and silent wrong-output / throw-deleting patches.

        // 1) ANCHOR: exact failing test input first.
        try {
            StrBuilder anchor = new StrBuilder(1);
            anchor.appendFixedWidthPadRight("foo", 1, '-');
            String got = anchor.toString();
            if (!"f".equals(got)) {
                throw new RuntimeException("[oracle:anchor-eq] metamorphic violation: exact regression input must yield prefix input=foo width=1 lhs=" + got + " rhs=f");
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }

        // 2) EXPLORE non-null path: width chosen valid-by-construction, exercising ensureCapacity.
        String prefix = data.consumeAsciiString(8);
        String body = data.consumeString(16);
        if (body.length() == 0) {
            body = "A";
        }
        int width = data.consumeInt(1, body.length());
        char pad = (char) (data.consumeByte() & 0xff);
        int initialCapacity = data.consumeInt(0, Math.max(1, prefix.length()));

        StrBuilder lhs1 = new StrBuilder(initialCapacity);
        StrBuilder rhs1 = new StrBuilder(initialCapacity);

        try {
            lhs1.append(prefix);
            lhs1.appendFixedWidthPadRight(body, width, pad);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        try {
            rhs1.append(prefix);
            rhs1.append(body, 0, width);
        } catch (RuntimeException t) {
            return;
        }

        String lhs1s;
        String rhs1s;
        try {
            lhs1s = lhs1.toString();
            rhs1s = rhs1.toString();
        } catch (RuntimeException t) {
            return;
        }

        if (!lhs1s.equals(rhs1s)) {
            throw new RuntimeException("[oracle:prefix-compose] metamorphic violation: appendFixedWidthPadRight must equal append(str,0,width) when input is at least width input=" + body + " width=" + width + " prefix=" + prefix + " lhs=" + lhs1s + " rhs=" + rhs1s);
        }

        // Independent consistency cross-check on the same object, using two real summaries.
        // For any correct implementation, the builder's content string length must equal the copied char-array length.
        char[] copied1;
        try {
            copied1 = lhs1.getChars((char[]) null);
        } catch (RuntimeException t) {
            return;
        }
        if (lhs1s.length() != copied1.length) {
            throw new RuntimeException("[oracle:string-chars] consistency violation: toString().length must equal getChars(null).length strLen=" + lhs1s.length() + " charsLen=" + copied1.length);
        }

        // 3) EXPLORE null-object path to drive getNullText through the real code.
        String nullText = data.consumeString(12);
        if (nullText.length() == 0) {
            nullText = "N";
        }
        int width2 = data.consumeInt(1, Math.max(1, nullText.length() + 4));
        char pad2 = (char) (data.consumeByte() & 0xff);
        String prefix2 = data.consumeAsciiString(8);
        int initialCapacity2 = data.consumeInt(0, Math.max(1, prefix2.length()));

        StrBuilder lhs2 = new StrBuilder(initialCapacity2);
        StrBuilder rhs2 = new StrBuilder(initialCapacity2);

        try {
            lhs2.setNullText(nullText);
            lhs2.append(prefix2);
            lhs2.appendFixedWidthPadRight(null, width2, pad2);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        try {
            rhs2.setNullText(nullText);
            rhs2.append(prefix2);
            if (nullText.length() >= width2) {
                rhs2.append(nullText, 0, width2);
            } else {
                rhs2.append(nullText);
                rhs2.appendPadding(width2 - nullText.length(), pad2);
            }
        } catch (RuntimeException t) {
            return;
        }

        String lhs2s;
        String rhs2s;
        try {
            lhs2s = lhs2.toString();
            rhs2s = rhs2.toString();
        } catch (RuntimeException t) {
            return;
        }

        if (!lhs2s.equals(rhs2s)) {
            throw new RuntimeException("[oracle:nulltext-compose] metamorphic violation: nullText expansion must agree with equivalent append/appendPadding composition nullText=" + nullText + " width=" + width2 + " prefix=" + prefix2 + " lhs=" + lhs2s + " rhs=" + rhs2s);
        }

        char[] copied2;
        try {
            copied2 = lhs2.getChars((char[]) null);
        } catch (RuntimeException t) {
            return;
        }
        if (lhs2s.length() != copied2.length) {
            throw new RuntimeException("[oracle:null-string-chars] consistency violation: toString().length must equal getChars(null).length strLen=" + lhs2s.length() + " charsLen=" + copied2.length);
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException || t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            String cls = st[i].getClassName();
            String m = st[i].getMethodName();
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                    && ("appendFixedWidthPadRight".equals(m)
                    || "ensureCapacity".equals(m)
                    || "getNullText".equals(m))) {
                return true;
            }
        }
        return false;
    }
}
```

</details>

---
## [10] ⚙️ harness-attempt · `attempt_004`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.StringIndexOutOfBoundsException: offset 0, count 3, length 1

---
## [11] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~28,492 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.apache.commons.lang.text` (`package org.apache.commons.lang.text;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Lang_59_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/java/org/apache/commons/lang/text/StrBuilder.java
+++ /src/java/org/apache/commons/lang/text/StrBuilder.java
@@ -877,7 +877,7 @@ public class StrBuilder implements Cloneable {
      */
     public StrBuilder appendFixedWidthPadRight(Object obj, int width, char padChar) {
         if (width > 0) {
-            ensureCapacity(size + width);
+            ensureCapacity((size) + 5);
             String str = (obj == null ? getNullText() : obj.toString());
             int strLen = str.length();
             if (strLen >= width) {

</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.SystemUtils;
</source_imports>

Function `appendFixedWidthPadRight`:
<signature>
public StrBuilder appendFixedWidthPadRight(Object obj, int width, char padChar)
</signature>
<code>
    public StrBuilder appendFixedWidthPadRight(Object obj, int width, char padChar) {
        if (width > 0) {
            ensureCapacity(size + width);
            String str = (obj == null ? getNullText() : obj.toString());
            int strLen = str.length();
            if (strLen >= width) {
                str.getChars(0, strLen, buffer, size);
            } else {
                int padLen = width - strLen;
                str.getChars(0, strLen, buffer, size);
                for (int i = 0; i < padLen; i++) {
                    buffer[size + strLen + i] = padChar;
                }
            }
            size += width;
        }
        return this;
    }
</code>
Methods called by `appendFixedWidthPadRight` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="ensureCapacity" from="StrBuilder.java">
<signature>
public StrBuilder ensureCapacity(int capacity)
</signature>
<code>
    public StrBuilder ensureCapacity(int capacity) {
        if (capacity > buffer.length) {
            char[] old = buffer;
            buffer = new char[capacity];
            System.arraycopy(old, 0, buffer, 0, size);
        }
        return this;
    }
</code>
</callee>
<callee name="getNullText" from="StrBuilder.java">
<signature>
public String getNullText()
</signature>
<code>
    public String getNullText() {
        return nullText;
    }
</code>
</callee>
<callee name="toString" from="StrBuilder.java">
<signature>
public String toString()
</signature>
<code>
    public String toString() {
        return new String(buffer, 0, size);
    }
</code>
<implementation in="StrBuilder.java">
    public String toString() {
        return new String(buffer, 0, size);
    }
</implementation>
<implementation in="StrTokenizer.java">
    public String toString() {
        if (tokens == null) {
            return "StrTokenizer[not tokenized yet]";
        } else {
            return "StrTokenizer" + getTokenList();
        }
    }
</implementation>
</callee>
<callee name="length" from="StrBuilder.java">
<signature>
public int length()
</signature>
<code>
    public int length() {
        return size;
    }
</code>
</callee>
<callee name="getChars" from="StrBuilder.java">
<signature>
public char[] getChars(char[] destination)
</signature>
<code>
    public char[] getChars(char[] destination) {
        int len = length();
        if (destination == null || destination.length < len) {
            destination = new char[len];
        }
        System.arraycopy(buffer, 0, destination, 0, len);
        return destination;
    }
</code>
<implementation in="StrBuilder.java">
    public char[] getChars(char[] destination) {
        int len = length();
        if (destination == null || destination.length < len) {
            destination = new char[len];
        }
        System.arraycopy(buffer, 0, destination, 0, len);
        return destination;
    }
</implementation>
<implementation in="StrBuilder.java">
    public void getChars(int startIndex, int endIndex, char destination[], int destinationIndex) {
        if (startIndex < 0) {
            throw new StringIndexOutOfBoundsException(startIndex);
        }
        if (endIndex < 0 || endIndex > length()) {
            throw new StringIndexOutOfBoundsException(endIndex);
        }
        if (startIndex > endIndex) {
            throw new StringIndexOutOfBoundsException("end < start");
        }
        System.arraycopy(buffer, startIndex, destination, destinationIndex, endIndex - startIndex);
    }
</implementation>
</callee>
STATE COUPLING: these members of the same class share the listed field(s) with `appendFixedWidthPadRight` but neither calls the other — state one of them writes is what the other reports. A defect the patch leaves (or introduces) in that shared state stays observable through these members even when `appendFixedWidthPadRight`'s own output looks right, so a strong harness CHECKS THEY AGREE: compare what a reader reports against what the writer/constructor established.
  - `public StrBuilder setLength(int length)` (shared field(s): buffer, size)
      doc: Updates the length of the builder by either dropping the last characters or adding filler of unicode zero. @param length  the length to set to, must be zero or positive @return this, to enable chaining @throws IndexOutOfBoundsException if the length is negative
  - `public StrBuilder minimizeCapacity()` (shared field(s): buffer, size)
      doc: Minimizes the capacity to the actual length of the string. @return this, to enable chaining
  - `public char[] toCharArray()` (shared field(s): buffer, size)
      doc: Copies the builder's character array into a new character array. @return a new array that represents the contents of the builder
  - `public StrBuilder append(String str)` (shared field(s): buffer, size)
      doc: Appends a string to this string builder. Appending null will call {@link #appendNull()}. @param str  the string to append @return this, to enable chaining
  - `public StrBuilder append(String str, int startIndex, int length)` (shared field(s): buffer, size)
      doc: Appends part of a string to this string builder. Appending null will call {@link #appendNull()}. @param str  the string to append @param startIndex  the start index, inclusive, must be valid @param length  the length to append, must be valid @return this, to enable chaining

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `StrBuilderAppendInsert`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.StringIndexOutOfBoundsException, java.lang.ArrayIndexOutOfBoundsException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `appendFixedWidthPadRight` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: java.lang.StringIndexOutOfBoundsException
message: offset 0, count 3, length 1
thrown_at: org.apache.commons.lang.text.StrBuilder.appendFixedWidthPadRight(StrBuilder.java:884)
</ground_truth_crash>
<failing_test class="org.apache.commons.lang.text.StrBuilderAppendInsertTest" method="testLang299">
    public void testLang299() {
        StrBuilder sb = new StrBuilder(1);
        sb.appendFixedWidthPadRight("foo", 1, '-');
        assertEquals("f", sb.toString());
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.lang.text.StrBuilderAppendInsertTest::testLang299
java.lang.StringIndexOutOfBoundsException: offset 0, count 3, length 1
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  append(Object obj) / (String str) / (String str, int startIndex, int length) / (StringBuffer str) / (StringBuffer str, int startIndex, int length) / (StrBuilder str) / (StrBuilder str, int startIndex, int length) / (char[] chars) / (char[] chars, int startIndex, int length) / (boolean value) / (char ch) / (int value) / (long value) / (float value) / (double value)
  appendFixedWidthPadLeft(Object obj, int width, char padChar) / (int value, int width, char padChar)
  appendFixedWidthPadRight(Object obj, int width, char padChar) / (int value, int width, char padChar)
  appendWithSeparators(Object[] array, String separator) / (Collection coll, String separator) / (Iterator it, String separator)
  contains(char ch) / (String str) / (StrMatcher matcher)
  deleteAll(char ch) / (String str) / (StrMatcher matcher)
  deleteFirst(char ch) / (String str) / (StrMatcher matcher)
  equals(StrBuilder other) / (Object obj)

METHOD FAMILIES over the same input space (factory/parser siblings — where the docs state a selection or agreement rule between family members, equivalent inputs must respect it):
  append* family: appendFixedWidthPadLeft, appendFixedWidthPadRight, appendNewLine, appendNull, appendPadding, appendWithSeparators
  as* family: asReader, asTokenizer

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- StrBuilder.ensureCapacity
- StrBuilder.getNullText
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.lang.text.FuzzHarness.anchorScenario
- org.apache.commons.lang.text.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.lang.text.FuzzHarness.runScenario
- org.apache.commons.lang.text.StrBuilder.appendFixedWidthPadRight
Crashes already found:
- java.lang.StringIndexOutOfBoundsException@org.apache.commons.lang.text.StrBuilder.appendFixedWidthPadRight
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- StrBuilder.ensureCapacity
- StrBuilder.getNullText
Check FAMILIES already covered by accepted harnesses: {anchor-eq, anchor-post, anchor-reader, anchor-truncate, fixed-width-pad-right, length-agreement, minimize-noop, nonnull, null-string-chars, nulltext, nulltext-compose, prefix-compose, sibling-prefix, state-consistency, string-chars}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.StringIndexOutOfBoundsException@org.apache.commons.lang.text.StrBuilder.appendFixedWidthPadRight. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
VARIANT STRATEGY — this harness is one of a set that divides the strategies below so each is tried. USE STRATEGY (c) for THIS harness (fall back to another ONLY if it is clearly inapplicable to this patch). The patch may be a band-aid that fixed the reported instance while the SAME root cause still bites elsewhere in this region:

  (a) DIFFERENT REACHABLE FUNCTION (coverage spread). Shape the input so a different function in the region above runs / a different failure signature appears. Good when the same root-cause pattern recurs across several functions and the patch only fixed one.

  (b) CONSISTENCY CROSS-CHECK on a masked helper. A defect can leave the patched function's top-level output CORRECT because a downstream step (an iterative solve, a convergence loop, a clamp, a re-normalisation) absorbs the error — while a reachable HELPER that produces a RELATED quantity stays wrong (the unfixed root cause). Rather than guess that helper's exact value, compute the SAME quantity TWO independent ways and assert they AGREE — a check that is sound without knowing the answer. Generic patterns: a stated summary/aggregate must match the EMPIRICAL value recomputed over many items the same object produces; a reported size/count must match a manual count of the elements; a cached/precomputed value must equal a fresh recomputation; equal objects must have equal hashCodes. Use a tolerance for floating-point/statistical estimates and require enough items, so a CORRECT implementation never fires.

  (c) FLIP THE PATCHED CONDITION (overfit on the seed). The patch changed a specific line/condition. An overfitting fix often special-cases the seed input and breaks just past it. Construct inputs that make the OLD and NEW behaviour DIFFER — values at and around the boundary the changed condition tests — and check the result is still correct there.

GUARDRAIL (all options): only assert a property that holds for EVERY correct implementation. If you cannot state one, exercise the path WITHOUT asserting — an assertion you can't justify false-positives on a correct patch.
CONSISTENCY CHECKS (these directly support strategy (b) — a summary a defect can leave wrong while the top-level output is masked). Identify from the API IN FRONT OF YOU which exposed values can be cross-checked against an independent computation — a reported aggregate vs a recomputation from the object's own output, a value vs the object's own stated bounds — and for at least one, throw on a mismatch beyond tolerance. These are SOUND (a summary must match the data it summarises), so they will not fire on a correct implementation.
WHAT DOES NOT COUNT: asserting the reported value is finite, non-NaN, non-null, or that no exception was thrown is NOT a consistency check — every wrong-but-finite value passes those, so they can never catch this class of bug. A valid check COMPARES the reported value against a second, independently obtained quantity: the object's own stated bounds, or a recomputation from the object's own output.
SHAPE OF A VALID CHECK (a schema — instantiate every <placeholder> with real calls from the API above):

    var reported = <the value the object claims>;
    var independent = <the SAME quantity obtained a second, independent way>;
    if (<reported disagrees with independent beyond a generous tolerance>)
        throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:<short-id>] consistency violation: " + <both values>);

Ways to obtain the independent value — each sound for ANY correct implementation of ANY library:
  1. LIMITS THE OBJECT ITSELF STATES: a summary of data must lie within that data's own reported extremes (lower/upper, min/max, first/last, 0..capacity).
  2. RECOMPUTATION FROM THE OBJECT'S OWN OUTPUT: aggregate many values/elements the same object produces and compare the aggregate to the reported summary.
  3. A SECOND, IDENTICALLY-CONSTRUCTED OBJECT (or a fresh recomputation of a cached value): both must report the same thing.

Use enough items and a generous tolerance (a few percent, or several standard errors) so noise on a CORRECT implementation never fires; a genuinely wrong value is typically off by far more than any sane tolerance.

POST-CONDITION / METAMORPHIC CHECK (MANDATORY — catches wrong-output bugs that never throw):
ASSUME THE ADVERSARY. The patch under analysis may 'fix' the bug by simply (a) deleting the throw / bookkeeping statement, (b) adding a guard that makes the crashing branch unreachable (and with it the branch's intended behaviour), or (c) replacing the failing operation with one that silently does the wrong thing (e.g. appending instead of sorted-inserting, skipping a modification-counter update). In all three worlds NO exception ever fires and a crash-only harness passes the patch. Therefore, in addition to reproducing the ground-truth failure, your harness MUST assert at least ONE observable post-condition from the documented contract of the patched method that such a patch would violate — e.g. after inserting into an auto-sorted collection, the collection is still sorted; after a removal, an iterator over the container either throws or reflects the removal (never silently yields stale state); a branch that is supposed to set a size/flag/result observably set it. State in a comment WHICH contract guarantee you assert and WHY a throw-deleting patch would break it. A harness that only reproduces the crash is incomplete.

Prefer a post-condition you can read directly off the API after the call. Where none is observable, use ONE metamorphic relation — a relation between two related calls of the target that must hold for ANY correct implementation: compute both sides from REAL library calls on the fuzzed input, and throw if they disagree.
- Round-trip / inverse: f(g(x)) == x  (e.g. decode(encode(x)), parse(format(v)), unescape(escape(s))).
- Idempotence: f(f(x)) == f(x)  (e.g. normalise, trim, strip, canonicalise).
- Equivalent inputs: two inputs that must map to the same result do  (e.g. case-insensitive parse: f(s) == f(s.toUpperCase()); leading-zero / whitespace variants of the same number).
- Composition / split: f(a + b) relates to f(a) and f(b) consistently  (e.g. a translator/encoder applied to a concatenation equals the concatenation of the parts).
- Oracle from the input itself: when the fuzzed input is CONSTRUCTED from a known value, the result must recover it (e.g. build the canonical string for a random int n, parse it, and assert it equals n).

HYGIENE RULES — violating these fires on CORRECT patches (a false positive, the worst outcome):
- If EITHER side of a relation, or the call preceding a post-condition read, throws anything at all, the check does not apply to that input: catch it, skip the check, return normally. NEVER convert a caught exception into a violation — an exception is a rejection, not a wrong answer.
- Before asserting a relation or post-condition, cite in a comment the documented guarantee (javadoc sentence, class contract, or invariant visible in the code shown above) that makes it hold for EVERY correct implementation, including edge cases: null elements, empty inputs, duplicates, no-solution inputs. If you cannot cite one, do not assert it.
- FENCE DEGENERATE INPUTS: construct the inputs you assert on to be non-degenerate BY CONSTRUCTION — non-empty strings and collections, visible (non-blank) labels/names, at least one element where elements are iterated. Contracts are routinely silent about the empty/blank case (a correct implementation may legitimately do nothing for an empty label, return nothing for an empty collection), so a relation whose ONLY violations occur on degenerate inputs is testing unspecified behaviour and will be rejected in review. Assert on the degenerate case ONLY when the documented contract explicitly covers it — and cite that sentence.
- FENCE EXTREME MAGNITUDES the same way: cap fuzzed numeric parameters to moderate ranges by construction (as a rule of thumb, |value| <= 1_000_000 for integers that get multiplied together or fed to combinatorial/statistical formulas) unless the documented contract explicitly covers larger values. At billion-scale parameters a CORRECT implementation's double arithmetic legitimately degrades — internal overflow to NaN, rounding beyond any tolerance, log-gamma saturation — so an assertion whose only violations occur at such magnitudes accuses correct code (this exact pattern produced repeated false accusations: a probability check that sees NaN only at N near 2^31, a validation exception from a parameter range that overflowed). The bug you hunt fires at REASONABLE magnitudes too whenever the contract really is broken.
- CONDITIONAL SIDE EFFECTS ARE NOT UNCONDITIONAL. If the method performs an observable side effect (adds to a collection, increments a counter, populates a cache, sets a field) only INSIDE guards — nested `if`s on collaborator or field state, not just on the input — you may assert that side effect happened ONLY after your harness has established EVERY one of those guard conditions. A correct implementation legitimately does nothing when any guard is unmet (a null owner/collector/context, a disabled flag), so 'the effect always happens after the call' is unsound and fires on correct patches. Read the shown method body: satisfy each guard on the path to the effect, or do not assert the effect.
- Use only real library calls for BOTH sides (no hand-rolled reference implementation).
- On violation, `throw new RuntimeException("[oracle:<short-id>] metamorphic violation: <which relation> input=<...> lhs=<...> rhs=<...>")` with the concrete values, so a reviewer can replay the disagreement. The [oracle:<short-id>] prefix is MANDATORY (checked mechanically) — an un-named alarm is invisible to the per-check acceptance machinery. Jazzer reports the throw as a finding, the same as a crash.

Use ONLY these FuzzedDataProvider methods (no invented overloads):

    int     consumeInt()                    // any int
    int     consumeInt(int min, int max)    // inclusive
    byte    consumeByte()
    boolean consumeBoolean()
    String  consumeString(int maxLength)    // ONE arg
    String  consumeAsciiString(int maxLength)
    String  consumeRemainingAsString()
    byte[]  consumeBytes(int maxLength)
    byte[]  consumeRemainingAsBytes()
    int     remainingBytes()


Complete the skeleton below. Fill in ONLY the `// >>> YOUR CODE HERE <<<` region. Do NOT change the package, import, class name, or entrypoint. Do NOT add a `main` method. Add extra imports only if javac requires them, directly below the existing import.
<skeleton>
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~6,315 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            StrBuilder anchor = new StrBuilder(1);
            anchor.appendFixedWidthPadRight("foo", 1, '-');

            if (!"f".equals(anchor.toString()) || anchor.length() != 1) {
                throw new RuntimeException("[oracle:anchor-substring] metamorphic violation: appendFixedWidthPadRight(obj,width,pad) with width<=obj.length must append exactly the first width characters; input=foo width=1 actual=" + anchor.toString() + " length=" + anchor.length());
            }

            StrBuilder anchorExpected = new StrBuilder(1);
            anchorExpected.append("foo", 0, 1);
            if (!anchor.toString().equals(anchorExpected.toString()) || anchor.length() != anchorExpected.length()) {
                throw new RuntimeException("[oracle:anchor-sibling] metamorphic violation: fixed-width right append without padding must agree with append(str,0,width); lhs=" + anchor.toString() + " rhs=" + anchorExpected.toString());
            }

            String anchorBeforeEnsure = anchor.toString();
            int anchorBeforeLen = anchor.length();
            anchor.ensureCapacity(anchorBeforeLen + 4);
            if (!anchorBeforeEnsure.equals(anchor.toString()) || anchor.length() != anchorBeforeLen) {
                throw new RuntimeException("[oracle:ensure-preserve-anchor] metamorphic violation: ensureCapacity must preserve content and length; before=" + anchorBeforeEnsure + " after=" + anchor.toString() + " beforeLen=" + anchorBeforeLen + " afterLen=" + anchor.length());
            }
        } catch (RuntimeException t) {
            boolean validation = false;
            for (Class<?> c = t.getClass(); c != null; c = c.getSuperclass()) {
                String n = c.getName();
                if (n.equals("java.lang.IllegalArgumentException") || n.equals("java.lang.NumberFormatException")) {
                    validation = true;
                    break;
                }
            }
            if (validation) {
                return;
            }

            boolean locationMatch = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                        && ("appendFixedWidthPadRight".equals(ste.getMethodName())
                        || "ensureCapacity".equals(ste.getMethodName())
                        || "getNullText".equals(ste.getMethodName()))) {
                    locationMatch = true;
                    break;
                }
            }

            if (locationMatch
                    && (t instanceof StringIndexOutOfBoundsException
                    || t instanceof ArrayIndexOutOfBoundsException)) {
                throw t;
            }
        }

        String prefix = data.consumeAsciiString(3);
        String src = data.consumeAsciiString(5);
        if (src.length() < 2) {
            src = src + "ABCDE";
        }
        if (src.length() > 5) {
            src = src.substring(0, 5);
        }

        int maxWidth = Math.min(4, src.length() - 1);
        if (maxWidth < 1) {
            return;
        }
        int width = data.consumeInt(1, maxWidth);
        char pad = (char) (data.consumeByte() & 0x7f);

        try {
            StrBuilder actual = new StrBuilder(prefix.length() + width);
            actual.append(prefix);
            actual.appendFixedWidthPadRight(src, width, pad);

            StrBuilder expected = new StrBuilder(prefix.length() + width);
            expected.append(prefix);
            expected.append(src, 0, width);

            String actualString = actual.toString();
            String expectedString = expected.toString();

            if (!actualString.equals(expectedString) || actual.length() != expected.length()) {
                throw new RuntimeException("[oracle:boundary-sibling] metamorphic violation: when width<=source.length, appendFixedWidthPadRight must match append(source,0,width); prefix=" + prefix + " src=" + src + " width=" + width + " lhs=" + actualString + " rhs=" + expectedString + " lhsLen=" + actual.length() + " rhsLen=" + expected.length());
            }

            String beforeEnsure = actual.toString();
            int beforeLen = actual.length();
            actual.ensureCapacity(beforeLen + data.consumeInt(0, 5));

            if (!beforeEnsure.equals(actual.toString()) || actual.length() != beforeLen) {
                throw new RuntimeException("[oracle:ensure-preserve-fuzz] metamorphic violation: ensureCapacity must not change observable content or length; prefix=" + prefix + " src=" + src + " width=" + width + " before=" + beforeEnsure + " after=" + actual.toString() + " beforeLen=" + beforeLen + " afterLen=" + actual.length());
            }
        } catch (RuntimeException t) {
            boolean validation = false;
            for (Class<?> c = t.getClass(); c != null; c = c.getSuperclass()) {
                String n = c.getName();
                if (n.equals("java.lang.IllegalArgumentException") || n.equals("java.lang.NumberFormatException")) {
                    validation = true;
                    break;
                }
            }
            if (validation) {
                return;
            }

            boolean locationMatch = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                        && ("appendFixedWidthPadRight".equals(ste.getMethodName())
                        || "ensureCapacity".equals(ste.getMethodName())
                        || "getNullText".equals(ste.getMethodName()))) {
                    locationMatch = true;
                    break;
                }
            }

            if (locationMatch
                    && (t instanceof StringIndexOutOfBoundsException
                    || t instanceof ArrayIndexOutOfBoundsException
                    || (t.getMessage() != null && t.getMessage().startsWith("[oracle:")))) {
                throw t;
            }
        }
    }
}
```

</details>

---
## [12] ⚙️ harness-attempt · `attempt_005`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.StringIndexOutOfBoundsException: offset 0, count 3, length 1

---
## [13] ⚙️ corpus-seed · `attempt_001`
**output:** **2 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['foo', 'f']}

---
## [14] ⚙️ corpus-seed · `attempt_002`
**output:** **2 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['foo', 'f']}

---
## [15] ⚙️ corpus-seed · `attempt_003`
**output:** **2 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['foo', 'f']}

---
## [16] ⚙️ corpus-seed · `attempt_004`
**output:** **2 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['foo', 'f']}

---
## [17] ⚙️ corpus-seed · `attempt_005`
**output:** **2 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['foo', 'f']}

---
## [18] ⚙️ patched-fuzz · `attempt_001`
**output:** **FIRED — crash on patched build**
- reproducing_input_file: /tmp/d4j/Lang_59_buggy/fuzz/attempt_001/crashes/crash-4a0a19218e082a343a1b17e5333409af9d98f0f5

---
## [19] ⚙️ patched-fuzz · `attempt_002`
**output:** **FIRED — crash on patched build**
- reproducing_input_file: /tmp/d4j/Lang_59_buggy/fuzz/attempt_002/crashes/crash-7aa37ae7197bcdf892586b8f8ae904bbb4e9ce2f

---
## [20] ⚙️ patched-fuzz · `attempt_003`
**output:** **FIRED — crash on patched build**
- reproducing_input_file: /tmp/d4j/Lang_59_buggy/fuzz/attempt_003/crashes/crash-2ebb66d53f6e59505daf1bde70d4e181c2470431

---
## [21] ⚙️ patched-fuzz · `attempt_004`
**output:** **FIRED — crash on patched build**
- reproducing_input_file: /tmp/d4j/Lang_59_buggy/fuzz/attempt_004/crashes/crash-0680cd20afae26dde4086d41dada0a7be068211e

---
## [22] ⚙️ patched-fuzz · `attempt_005`
**output:** **quiet on patched build (no overfit signal)**
