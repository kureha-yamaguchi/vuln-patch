# Pipeline trace — Lang-59

**Code:** `unknown`

**Patch label:** overfitting  *(the patch under analysis is a known-OVERFIT fix — the pipeline is not told this)*

**Outcome:** OVERFIT CAUGHT (a harness fired on the patched build). [evaluated; 3 harness(es) fuzzed on the patched build; campaign converged=True]

**Patch under analysis:**
```diff
--- /src/java/org/apache/commons/lang/text/StrBuilder.java	
+++ /src/java/org/apache/commons/lang/text/StrBuilder.java
@@ -877,6 +877,7 @@
      */
     public StrBuilder appendFixedWidthPadRight(Object obj, int width, char padChar) {
         if (width > 0) {
+        	    buffer=new char[CAPACITY];
             ensureCapacity(size + width);
             String str = (obj == null ? getNullText() : obj.toString());
             int strLen = str.length();
```

15 sequential steps — 3 LLM calls, 12 deterministic. Read top to bottom.

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
    "failure_message": "--- org.apache.commons.lang.text.StrBuilderAppendInsertTest::testLang299\njava.lang.ArrayIndexOutOfBoundsException",
    "support_chars": 0
  }
]
```

---
## [2] ⚙️ analysis (TargetAnalyzer)
<details><summary>▸ output — modified_files": [ (~8,682 chars)</summary>

**output:** 
```json
{
  "modified_files": [
    "src/java/org/apache/commons/lang/text/StrBuilder.java"
  ],
  "patch_text": "--- /src/java/org/apache/commons/lang/text/StrBuilder.java\t\n+++ /src/java/org/apache/commons/lang/text/StrBuilder.java\n@@ -877,6 +877,7 @@\n      */\n     public StrBuilder appendFixedWidthPadRight(Object obj, int width, char padChar) {\n         if (width > 0) {\n+        \t    buffer=new char[CAPACITY];\n             ensureCapacity(size + width);\n             String str = (obj == null ? getNullText() : obj.toString());\n             int strLen = str.length();\n",
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
<details><summary>▸ Prompt (2 message(s), ~26,695 chars)</summary>

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
@@ -877,6 +877,7 @@
      */
     public StrBuilder appendFixedWidthPadRight(Object obj, int width, char padChar) {
         if (width > 0) {
+        	    buffer=new char[CAPACITY];
             ensureCapacity(size + width);
             String str = (obj == null ? getNullText() : obj.toString());
             int strLen = str.length();

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
On the buggy version the root cause surfaces as: java.lang.ArrayIndexOutOfBoundsException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
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
throwable: java.lang.ArrayIndexOutOfBoundsException
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
java.lang.ArrayIndexOutOfBoundsException
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
<details open><summary>▸ Output (~7,715 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            StrBuilder anchor = new StrBuilder(1);
            anchor.appendFixedWidthPadRight("foo", 1, '-');

            String anchorOut = anchor.toString();
            if (!"f".equals(anchorOut)) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: failing test contract input=foo,width=1,pad=- actual=" + anchorOut);
            }
            if (anchor.length() != anchorOut.length()
                    || anchor.toCharArray().length != anchorOut.length()
                    || anchor.getChars((char[]) null).length != anchorOut.length()) {
                throw new RuntimeException("[oracle:anchor-state] consistency violation: toStringLen=" + anchorOut.length()
                        + " length()=" + anchor.length()
                        + " toCharArrayLen=" + anchor.toCharArray().length
                        + " getCharsLen=" + anchor.getChars((char[]) null).length);
            }
        } catch (RuntimeException t) {
            boolean rootCause = t instanceof ArrayIndexOutOfBoundsException;
            if (rootCause) {
                StackTraceElement[] st = t.getStackTrace();
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.lang.text.StrBuilder".equals(st[i].getClassName())
                            && "appendFixedWidthPadRight".equals(st[i].getMethodName())) {
                        throw t;
                    }
                }
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
        }

        String prefix = data.consumeString(32);
        String s = data.consumeString(32);
        if (s.length() == 0) {
            s = "X";
        }
        char pad = (char) (data.consumeByte() & 0xff);
        int initialCapacity = data.consumeInt(1, 8);

        int mode = data.consumeInt(0, 5);
        int width;
        if (mode == 0) {
            width = 1;
        } else if (mode == 1) {
            width = s.length();
        } else if (mode == 2) {
            width = Math.max(1, s.length() - 1);
        } else if (mode == 3) {
            width = s.length() + 1;
        } else if (mode == 4) {
            width = s.length() + 2;
        } else {
            width = data.consumeInt(1, 40);
        }

        boolean shrinkBeforeAppend = data.consumeBoolean();
        boolean exerciseNullTextPath = data.consumeBoolean();

        try {
            StrBuilder lhs = new StrBuilder(initialCapacity);
            lhs.append(prefix);
            if (shrinkBeforeAppend) {
                lhs.minimizeCapacity();
            }

            StrBuilder rhs = new StrBuilder(initialCapacity);
            rhs.append(prefix);
            if (shrinkBeforeAppend) {
                rhs.minimizeCapacity();
            }

            String effective;
            if (exerciseNullTextPath) {
                String nullText = data.consumeAsciiString(16);
                if (nullText.length() == 0) {
                    nullText = "N";
                }
                lhs.setNullText(nullText);
                rhs.setNullText(nullText);
                effective = nullText;
                lhs.appendFixedWidthPadRight(null, width, pad);
            } else {
                effective = s;
                lhs.appendFixedWidthPadRight(s, width, pad);
            }

            /* Contract checked: appendFixedWidthPadRight appends a fixed-width field.
               For non-null effective text, if text is longer than width it must truncate to width;
               otherwise it must append the text and then pad to width. A throw-deleting or
               content-dropping patch breaks this sibling-equivalent construction. */
            if (effective.length() >= width) {
                rhs.append(effective, 0, width);
            } else {
                rhs.append(effective);
                rhs.appendPadding(width - effective.length(), pad);
            }

            String lhsString = lhs.toString();
            String rhsString = rhs.toString();
            if (!lhsString.equals(rhsString)) {
                throw new RuntimeException("[oracle:fixed-width] metamorphic violation: appendFixedWidthPadRight vs equivalent append sequence"
                        + " prefix=" + prefix
                        + " effective=" + effective
                        + " width=" + width
                        + " pad=" + (int) pad
                        + " lhs=" + lhsString
                        + " rhs=" + rhsString);
            }

            int reportedLength = lhs.length();
            int stringLength = lhsString.length();
            int charArrayLength = lhs.toCharArray().length;
            int getCharsLength = lhs.getChars((char[]) null).length;
            if (reportedLength != stringLength || reportedLength != charArrayLength || reportedLength != getCharsLength) {
                throw new RuntimeException("[oracle:state] consistency violation: reportedLength=" + reportedLength
                        + " stringLength=" + stringLength
                        + " charArrayLength=" + charArrayLength
                        + " getCharsLength=" + getCharsLength
                        + " value=" + lhsString);
            }

            StrBuilder preserved = new StrBuilder(initialCapacity);
            preserved.append(lhsString);
            preserved.minimizeCapacity();
            if (!lhsString.equals(preserved.toString())) {
                throw new RuntimeException("[oracle:mincap] metamorphic violation: minimizeCapacity changed visible contents value="
                        + lhsString + " after=" + preserved.toString());
            }

            StrBuilder sameLen = new StrBuilder(initialCapacity);
            sameLen.append(lhsString);
            sameLen.setLength(sameLen.length());
            if (!lhsString.equals(sameLen.toString())) {
                throw new RuntimeException("[oracle:setlen] metamorphic violation: setLength(length()) changed visible contents value="
                        + lhsString + " after=" + sameLen.toString());
            }
        } catch (RuntimeException t) {
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }

            boolean rootCause = t instanceof ArrayIndexOutOfBoundsException;
            if (rootCause) {
                StackTraceElement[] st = t.getStackTrace();
                for (int i = 0; i < st.length; i++) {
                    String cls = st[i].getClassName();
                    String method = st[i].getMethodName();
                    if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                            && ("appendFixedWidthPadRight".equals(method)
                                || "ensureCapacity".equals(method)
                                || "getNullText".equals(method))) {
                        if (width > 0 && effectiveInputIsValid(exerciseNullTextPath, s)) {
                            throw t;
                        }
                        return;
                    }
                }
            }
        }
    }

    private static boolean effectiveInputIsValid(boolean exerciseNullTextPath, String s) {
        return exerciseNullTextPath || s != null;
    }
}
```

</details>

---
## [4] ⚙️ harness-attempt · `attempt_001`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException

---
## [5] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~28,009 chars, 1 new)</summary>

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
@@ -877,6 +877,7 @@
      */
     public StrBuilder appendFixedWidthPadRight(Object obj, int width, char padChar) {
         if (width > 0) {
+        	    buffer=new char[CAPACITY];
             ensureCapacity(size + width);
             String str = (obj == null ? getNullText() : obj.toString());
             int strLen = str.length();

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
On the buggy version the root cause surfaces as: java.lang.ArrayIndexOutOfBoundsException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
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
throwable: java.lang.ArrayIndexOutOfBoundsException
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
java.lang.ArrayIndexOutOfBoundsException
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
- org.apache.commons.lang.text.StrBuilder.appendFixedWidthPadRight
Crashes already found:
- java.lang.ArrayIndexOutOfBoundsException@org.apache.commons.lang.text.StrBuilder.appendFixedWidthPadRight
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- StrBuilder.ensureCapacity
- StrBuilder.getNullText
Check FAMILIES already covered by accepted harnesses: {anchor, anchor-state, fixed-width, mincap, setlen, state}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.ArrayIndexOutOfBoundsException@org.apache.commons.lang.text.StrBuilder.appendFixedWidthPadRight. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
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
<details open><summary>▸ Output (~8,604 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.io.Reader;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int scenarios = 1 + data.consumeInt(0, 3);
        for (int i = 0; i < scenarios; i++) {
            int initialCapacity = data.consumeInt(1, 8);
            String prefix = data.consumeAsciiString(8);
            String src = data.consumeString(16);
            if (src == null) {
                src = "";
            }
            if (src.length() == 0 && data.consumeBoolean()) {
                src = "A";
            }

            boolean useNullObject = data.consumeBoolean();
            String nullText = data.consumeAsciiString(8);
            if (nullText == null || nullText.length() == 0) {
                nullText = "N";
            }

            String effective = useNullObject ? nullText : src;
            int around = effective.length() + data.consumeInt(-1, 2);
            int width = around <= 0 ? 1 : around;
            char pad = (char) (data.consumeByte() & 0x7f);

            runScenario(initialCapacity, prefix, src, useNullObject, nullText, width, pad);
        }

        String boundaryString = data.consumeAsciiString(24);
        if (boundaryString == null || boundaryString.length() == 0) {
            boundaryString = "foo";
        }
        char boundaryPad = (char) (data.consumeByte() & 0x7f);
        int len = boundaryString.length();
        int[] widths = new int[] {
            1,
            len <= 0 ? 1 : len - 1,
            len <= 0 ? 1 : len,
            len + 1
        };
        for (int w : widths) {
            if (w > 0) {
                runScenario(1, "", boundaryString, false, "N", w, boundaryPad);
                runScenario(1, "P", boundaryString, false, "N", w, boundaryPad);
                runScenario(1, "", boundaryString, true, boundaryString, w, boundaryPad);
            }
        }
    }

    private static void runAnchor() {
        StrBuilder actual = new StrBuilder(1);
        try {
            actual.appendFixedWidthPadRight("foo", 1, '-');
        } catch (RuntimeException t) {
            if (isRootCause(t) && isValidConstruction(1, "foo")) {
                throw t;
            }
            return;
        }

        String got;
        try {
            got = actual.toString();
        } catch (RuntimeException t) {
            return;
        }
        if (!"f".equals(got)) {
            throw new RuntimeException("[oracle:anchor] metamorphic violation: appendFixedWidthPadRight(\"foo\",1,'-') must truncate to width 1 input=foo lhs=" + got + " rhs=f");
        }

        StrBuilder expected = new StrBuilder(1);
        try {
            expected.append("foo", 0, 1);
        } catch (RuntimeException t) {
            return;
        }
        String exp;
        try {
            exp = expected.toString();
        } catch (RuntimeException t) {
            return;
        }
        if (!got.equals(exp)) {
            throw new RuntimeException("[oracle:sibling-anchor] metamorphic violation: fixed-width truncation must agree with append(str,0,width) when str.length()>=width input=foo lhs=" + got + " rhs=" + exp);
        }

        checkStateConsistency(actual, "anchor");
    }

    private static void runScenario(int initialCapacity, String prefix, String src, boolean useNullObject,
                                    String nullText, int width, char pad) {
        if (width <= 0) {
            return;
        }

        String effective = useNullObject ? nullText : src;
        if (!isValidConstruction(width, effective)) {
            return;
        }

        StrBuilder actual = new StrBuilder(initialCapacity);
        try {
            actual.append(prefix);
            if (useNullObject) {
                actual.setNullText(nullText);
                actual.appendFixedWidthPadRight(null, width, pad);
            } else {
                actual.appendFixedWidthPadRight(src, width, pad);
            }
        } catch (RuntimeException t) {
            if (isRootCause(t) && isValidConstruction(width, effective)) {
                throw t;
            }
            return;
        }

        StrBuilder expected = new StrBuilder(initialCapacity);
        try {
            expected.append(prefix);
            if (effective.length() >= width) {
                expected.append(effective, 0, width);
            } else {
                expected.append(effective);
                for (int i = effective.length(); i < width; i++) {
                    expected.append(pad);
                }
            }
        } catch (RuntimeException t) {
            return;
        }

        String got;
        String exp;
        try {
            got = actual.toString();
            exp = expected.toString();
        } catch (RuntimeException t) {
            return;
        }

        /*
         * Contract asserted:
         * appendFixedWidthPadRight(obj, width, padChar) must append exactly width characters:
         * either the object's string truncated to width, or the full string padded on the right.
         * A patch that merely suppresses the throw or skips the copy/update will violate this.
         * Independent oracle: build the same documented result using sibling public append overloads.
         */
        if (!got.equals(exp)) {
            throw new RuntimeException(
                "[oracle:fixedwidth-sibling] metamorphic violation: fixed-width right padding/truncation must agree with sibling append operations input=" +
                printable(prefix) + "|" + printable(effective) + "|w=" + width + "|pad=" + (int) pad +
                " lhs=" + printable(got) + " rhs=" + printable(exp));
        }

        checkStateConsistency(actual, "state");
    }

    private static void checkStateConsistency(StrBuilder sb, String tag) {
        String s;
        int reportedLen;
        char[] chars;
        try {
            s = sb.toString();
            reportedLen = sb.length();
            chars = sb.toCharArray();
        } catch (RuntimeException t) {
            return;
        }

        if (reportedLen != s.length()) {
            throw new RuntimeException("[oracle:" + tag + "-len] metamorphic violation: length() must equal toString().length() lhs=" + reportedLen + " rhs=" + s.length());
        }
        if (chars.length != reportedLen) {
            throw new RuntimeException("[oracle:" + tag + "-chars] metamorphic violation: toCharArray().length must equal length() lhs=" + chars.length + " rhs=" + reportedLen);
        }
        if (!s.equals(new String(chars))) {
            throw new RuntimeException("[oracle:" + tag + "-chars-content] metamorphic violation: toCharArray contents must equal toString() lhs=" + printable(new String(chars)) + " rhs=" + printable(s));
        }

        try {
            Reader r = sb.asReader();
            char[] buf = new char[reportedLen + 2];
            int off = 0;
            while (off < buf.length) {
                int n = r.read(buf, off, buf.length - off);
                if (n < 0) {
                    break;
                }
                off += n;
            }
            String fromReader = new String(buf, 0, off);
            if (!s.equals(fromReader)) {
                throw new RuntimeException("[oracle:" + tag + "-reader] metamorphic violation: asReader() stream must match toString() lhs=" + printable(fromReader) + " rhs=" + printable(s));
            }
        } catch (Throwable t) {
            return;
        }
    }

    private static boolean isValidConstruction(int width, String effective) {
        return width > 0 && effective != null;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            String cls = trace[i].getClassName();
            String method = trace[i].getMethodName();
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                    && ("appendFixedWidthPadRight".equals(method)
                    || "ensureCapacity".equals(method)
                    || "getNullText".equals(method))) {
                return true;
            }
        }
        return false;
    }

    private static String printable(String s) {
        return s == null ? "null" : s.replace("\u0000", "\\0");
    }
}
```

</details>

---
## [6] ⚙️ harness-attempt · `attempt_002`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException

---
## [7] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~28,098 chars, 1 new)</summary>

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
@@ -877,6 +877,7 @@
      */
     public StrBuilder appendFixedWidthPadRight(Object obj, int width, char padChar) {
         if (width > 0) {
+        	    buffer=new char[CAPACITY];
             ensureCapacity(size + width);
             String str = (obj == null ? getNullText() : obj.toString());
             int strLen = str.length();

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
On the buggy version the root cause surfaces as: java.lang.ArrayIndexOutOfBoundsException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
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
throwable: java.lang.ArrayIndexOutOfBoundsException
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
java.lang.ArrayIndexOutOfBoundsException
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
- org.apache.commons.lang.text.FuzzHarness.runAnchor
- org.apache.commons.lang.text.StrBuilder.appendFixedWidthPadRight
Crashes already found:
- java.lang.ArrayIndexOutOfBoundsException@org.apache.commons.lang.text.StrBuilder.appendFixedWidthPadRight
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- StrBuilder.ensureCapacity
- StrBuilder.getNullText
Check FAMILIES already covered by accepted harnesses: {anchor, anchor-state, fixed-width, fixedwidth-sibling, mincap, setlen, sibling-anchor, state}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.ArrayIndexOutOfBoundsException@org.apache.commons.lang.text.StrBuilder.appendFixedWidthPadRight. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
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
<details open><summary>▸ Output (~7,665 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            StrBuilder anchor = new StrBuilder(1);
            anchor.appendFixedWidthPadRight("foo", 1, '-');
            String anchorOut = anchor.toString();
            if (!"f".equals(anchorOut)) {
                throw new RuntimeException("[oracle:anchor-out] metamorphic violation: exact regression test output input=foo,width=1,pad=- lhs=" + anchorOut + " rhs=f");
            }
        } catch (RuntimeException t) {
            boolean root = t instanceof ArrayIndexOutOfBoundsException;
            if (root) {
                StackTraceElement[] st = t.getStackTrace();
                for (int i = 0; i < st.length; i++) {
                    String cls = st[i].getClassName();
                    String m = st[i].getMethodName();
                    if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                            && ("appendFixedWidthPadRight".equals(m) || "ensureCapacity".equals(m) || "getNullText".equals(m))) {
                        throw t;
                    }
                }
            }
        }

        String prefix = data.consumeString(8);
        if (prefix.length() == 0) {
            prefix = "P";
        }
        String obj = data.consumeString(8);
        if (obj.length() == 0) {
            obj = "X";
        }
        String nullText = data.consumeString(8);
        if (nullText.length() == 0) {
            nullText = "N";
        }
        int width = data.consumeInt(1, 8);
        char pad = (char) (data.consumeByte() & 0x7f);
        int initialCapacity = data.consumeInt(1, Math.max(1, prefix.length()));

        try {
            StrBuilder sb = new StrBuilder(initialCapacity);
            sb.append(prefix);
            sb.minimizeCapacity();
            int beforeLen = sb.length();

            sb.appendFixedWidthPadRight(obj, width, pad);

            String after = sb.toString();

            // Contract: append(String) appends existing content, and appendFixedWidthPadRight appends width chars to this builder.
            // A throw-deleting/resetting patch can silently lose the already-appended prefix; every correct implementation must preserve it.
            if (!after.startsWith(prefix)) {
                throw new RuntimeException("[oracle:prefix-preserved] metamorphic violation: append must preserve prior content inputPrefix=" + prefix + " obj=" + obj + " width=" + width + " pad=" + (int) pad + " out=" + after);
            }

            // Contract: fixed-width append increases builder size by exactly width when width > 0.
            int expectedLen = beforeLen + width;
            if (sb.length() != expectedLen || after.length() != expectedLen) {
                throw new RuntimeException("[oracle:length-grow] metamorphic violation: fixed-width append length mismatch inputPrefix=" + prefix + " obj=" + obj + " width=" + width + " reportedLen=" + sb.length() + " stringLen=" + after.length() + " expected=" + expectedLen);
            }

            // Consistency check: reader view and string view of the same builder state must agree.
            try {
                java.io.Reader r = sb.asReader();
                StringBuffer readBack = new StringBuffer();
                char[] buf = new char[16];
                for (;;) {
                    int n = r.read(buf);
                    if (n < 0) {
                        break;
                    }
                    readBack.append(buf, 0, n);
                }
                String readerOut = readBack.toString();
                if (!after.equals(readerOut)) {
                    throw new RuntimeException("[oracle:reader-agree] metamorphic violation: asReader output must equal toString inputPrefix=" + prefix + " obj=" + obj + " width=" + width + " lhs=" + after + " rhs=" + readerOut);
                }
            } catch (Throwable ignored) {
                return;
            }
        } catch (IllegalArgumentException t) {
            return;
        } catch (RuntimeException t) {
            boolean root = t instanceof ArrayIndexOutOfBoundsException;
            if (root) {
                StackTraceElement[] st = t.getStackTrace();
                for (int i = 0; i < st.length; i++) {
                    String cls = st[i].getClassName();
                    String m = st[i].getMethodName();
                    if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                            && ("appendFixedWidthPadRight".equals(m) || "ensureCapacity".equals(m) || "getNullText".equals(m))) {
                        throw t;
                    }
                }
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        }

        try {
            StrBuilder viaNull = new StrBuilder(initialCapacity);
            viaNull.setNullText(nullText);
            viaNull.append(prefix);
            viaNull.minimizeCapacity();

            StrBuilder viaDirect = new StrBuilder(initialCapacity);
            viaDirect.setNullText(nullText);
            viaDirect.append(prefix);
            viaDirect.minimizeCapacity();

            viaNull.appendFixedWidthPadRight(null, width, pad);
            viaDirect.appendFixedWidthPadRight(nullText, width, pad);

            String a = viaNull.toString();
            String b = viaDirect.toString();

            // Contract from implementation: null obj is replaced by getNullText(); therefore null with configured nullText
            // must behave the same as passing that same string directly.
            if (!a.equals(b)) {
                throw new RuntimeException("[oracle:nulltext-equiv] metamorphic violation: null object path must match direct nullText path prefix=" + prefix + " nullText=" + nullText + " width=" + width + " lhs=" + a + " rhs=" + b);
            }

            try {
                java.io.Reader ra = viaNull.asReader();
                StringBuffer sba = new StringBuffer();
                char[] tmp = new char[16];
                for (;;) {
                    int n = ra.read(tmp);
                    if (n < 0) {
                        break;
                    }
                    sba.append(tmp, 0, n);
                }
                if (!a.equals(sba.toString())) {
                    throw new RuntimeException("[oracle:null-reader] metamorphic violation: reader/string disagreement after nullText path out=" + a + " reader=" + sba.toString());
                }
            } catch (Throwable ignored) {
                return;
            }
        } catch (IllegalArgumentException t) {
            return;
        } catch (RuntimeException t) {
            boolean root = t instanceof ArrayIndexOutOfBoundsException;
            if (root) {
                StackTraceElement[] st = t.getStackTrace();
                for (int i = 0; i < st.length; i++) {
                    String cls = st[i].getClassName();
                    String m = st[i].getMethodName();
                    if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                            && ("appendFixedWidthPadRight".equals(m) || "ensureCapacity".equals(m) || "getNullText".equals(m))) {
                        throw t;
                    }
                }
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
        }
    }
}
```

</details>

---
## [8] ⚙️ harness-attempt · `attempt_003`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException

---
## [9] ⚙️ corpus-seed · `attempt_001`
**output:** **2 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['foo', 'f']}

---
## [10] ⚙️ corpus-seed · `attempt_002`
**output:** **2 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['foo', 'f']}

---
## [11] ⚙️ corpus-seed · `attempt_003`
**output:** **2 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['foo', 'f']}

---
## [12] ⚙️ patched-fuzz · `attempt_001`
**output:** **FIRED — [oracle:fixed-width] metamorphic violation: appendFixedWidthPadRight vs equivalent append sequence prefix=foo effective=X width=1 pad=0 lhs=   X rhs=fooX**
- reproducing_input_file: /tmp/d4j/Lang_59_buggy/fuzz/attempt_001/crashes/crash-0beec7b5ea3f0fdbc95d0dd47f3c5bc275da8a33

---
## [13] ⚙️ patched-fuzz · `attempt_002`
**output:** **FIRED — [oracle:fixedwidth-sibling] metamorphic violation: fixed-width right padding/truncation must agree with sibling append operations input=P|foo|w=1|pad=0 lhs=\0f rhs=Pf**
- reproducing_input_file: /tmp/d4j/Lang_59_buggy/fuzz/attempt_002/crashes/crash-da39a3ee5e6b4b0d3255bfef95601890afd80709

---
## [14] ⚙️ patched-fuzz · `attempt_003`
**output:** **FIRED — [oracle:prefix-preserved] metamorphic violation: append must preserve prior content inputPrefix=P obj=X width=1 pad=0 out= X**
- reproducing_input_file: /tmp/d4j/Lang_59_buggy/fuzz/attempt_003/crashes/crash-da39a3ee5e6b4b0d3255bfef95601890afd80709
