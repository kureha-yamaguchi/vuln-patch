# Pipeline trace — Lang-59

**Code:** `b4b78d5146829e25837ef7e7f105b4ec3b686d1e`

**Patch label:** correct  *(the patch under analysis is a known-CORRECT fix — the pipeline is not told this)*

**Outcome:** candidate_experiment (buggy build only)

**Patch under analysis:**
```diff
--- /src/java/org/apache/commons/lang/text/StrBuilder.java
+++ /src/java/org/apache/commons/lang/text/StrBuilder.java
@@ -881,7 +881,7 @@ public class StrBuilder implements Cloneable {
             String str = (obj == null ? getNullText() : obj.toString());
             int strLen = str.length();
             if (strLen >= width) {
-                str.getChars(0, strLen, buffer, size);
+                str.getChars(0, width, buffer, size);
             } else {
                 int padLen = width - strLen;
                 str.getChars(0, strLen, buffer, size);
```

64 sequential steps — 30 LLM calls, 34 deterministic. Read top to bottom.

<details><summary>Legend — what each step is</summary>

- **failing-tests-found** — the project's own tests that expose the bug (extracted from Defects4J)
- **analysis (TargetAnalyzer)** — parses the patch and builds the code context the model reasons over — the touched method(s), their documented contract, the call-graph reachable set + sibling members (via fuzz-introspector), and imports
- **harness generation (LLM)** — writes a Jazzer fuzzing harness that embeds the surviving rules plus oracles lifted from the failing test
- **harness-attempt** — accept/reject of one generated harness — ACCEPTED = it compiles AND crashes the BUGGY build (with the triggering input shown)

</details>

*Viewing: every ▸ line is a click-to-expand toggle (VS Code markdown preview / GitHub). Prompts are collapsed by default; LLM outputs start expanded. The raw file stays fully greppable.*


---
## [0] ⚙️ failing-tests-found
<details><summary>▸ output — FailureTest(test_class='org.apache.commons.lang.text.StrBuilderAppendInsertTest', test_method='testL (~640 chars)</summary>

**output:** 
```json
[
  "FailureTest(test_class='org.apache.commons.lang.text.StrBuilderAppendInsertTest', test_method='testLang299', source_path='/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/Lang_59_buggy/src/test/org/apache/commons/lang/text/StrBuilderAppendInsertTest.java', method_source='    public void testLang299() {\\n        StrBuilder sb = new StrBuilder(1);\\n        sb.appendFixedWidthPadRight(\"foo\", 1, \\'-\\');\\n        assertEquals(\"f\", sb.toString());\\n    }', exception_type='java.lang.ArrayIndexOutOfBoundsException', failure_message=None, support_source=None)"
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
<details><summary>▸ output — modified_files": [ (~11,693 chars)</summary>

**output:** 
```json
{
  "modified_files": [
    "src/java/org/apache/commons/lang/text/StrBuilder.java"
  ],
  "patch_text": "--- /src/java/org/apache/commons/lang/text/StrBuilder.java\n+++ /src/java/org/apache/commons/lang/text/StrBuilder.java\n@@ -881,7 +881,7 @@ public class StrBuilder implements Cloneable {\n             String str = (obj == null ? getNullText() : obj.toString());\n             int strLen = str.length();\n             if (strLen >= width) {\n-                str.getChars(0, strLen, buffer, size);\n+                str.getChars(0, width, buffer, size);\n             } else {\n                 int padLen = width - strLen;\n                 str.getChars(0, strLen, buffer, size);",
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
      "xref_names": [],
      "reachable_edges": [
        [
          "[org.apache.commons.lang.text.StrBuilder].appendFixedWidthPadRight(Object,int,char)",
          "[org.apache.commons.lang.text.StrBuilder].ensureCapacity(org.apache.commons.lang.text.StrBuilder)"
        ],
        [
          "[org.apache.commons.lang.text.StrBuilder].appendFixedWidthPadRight(Object,int,char)",
          "[org.apache.commons.lang.text.StrBuilder].getNullText()"
        ],
        [
          "[org.apache.commons.lang.text.StrBuilder].appendFixedWidthPadRight(Object,int,char)",
          "[Object].toString()"
        ],
        [
          "[org.apache.commons.lang.text.StrBuilder].appendFixedWidthPadRight(Object,int,char)",
          "str.length()"
        ],
        [
          "[org.apache.commons.lang.text.StrBuilder].appendFixedWidthPadRight(Object,int,char)",
          "str.getChars(int,org.apache.commons.lang.text.StrBuilder,char[],int)"
        ],
        [
          "[org.apache.commons.lang.text.StrBuilder].appendFixedWidthPadRight(Object,int,char)",
          "str.getChars(int,org.apache.commons.lang.text.StrBuilder,char[],int)"
        ]
      ],
      "reachable_depth": {
        "[org.apache.commons.lang.text.StrBuilder].appendFixedWidthPadRight(Object,int,char)": 0,
        "[org.apache.commons.lang.text.StrBuilder].ensureCapacity(org.apache.commons.lang.text.StrBuilder)": 1,
        "[org.apache.commons.lang.text.StrBuilder].getNullText()": 1,
        "[Object].toString()": 1,
        "str.length()": 1,
        "str.getChars(int,org.apache.commons.lang.text.StrBuilder,char[],int)": 1
      },
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
  ],
  "reachable_edges": [
    [
      "[org.apache.commons.lang.text.StrBuilder].appendFixedWidthPadRight(Object,int,char)",
      "[org.apache.commons.lang.text.StrBuilder].ensureCapacity(org.apache.commons.lang.text.StrBuilder)"
    ],
    [
      "[org.apache.commons.lang.text.StrBuilder].appendFixedWidthPadRight(Object,int,char)",
      "[org.apache.commons.lang.text.StrBuilder].getNullText()"
    ],
    [
      "[org.apache.commons.lang.text.StrBuilder].appendFixedWidthPadRight(Object,int,char)",
      "[Object].toString()"
    ],
    [
      "[org.apache.commons.lang.text.StrBuilder].appendFixedWidthPadRight(Object,int,char)",
      "str.length()"
    ],
    [
      "[org.apache.commons.lang.text.StrBuilder].appendFixedWidthPadRight(Object,int,char)",
      "str.getChars(int,org.apache.commons.lang.text.StrBuilder,char[],int)"
    ]
  ],
  "reachable_depth": {
    "[org.apache.commons.lang.text.StrBuilder].appendFixedWidthPadRight(Object,int,char)": 0,
    "[org.apache.commons.lang.text.StrBuilder].ensureCapacity(org.apache.commons.lang.text.StrBuilder)": 1,
    "[org.apache.commons.lang.text.StrBuilder].getNullText()": 1,
    "[Object].toString()": 1,
    "str.length()": 1,
    "str.getChars(int,org.apache.commons.lang.text.StrBuilder,char[],int)": 1
  }
}
```

</details>

---
## [3] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~18,903 chars)</summary>

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
@@ -881,7 +881,7 @@ public class StrBuilder implements Cloneable {
             String str = (obj == null ? getNullText() : obj.toString());
             int strLen = str.length();
             if (strLen >= width) {
-                str.getChars(0, strLen, buffer, size);
+                str.getChars(0, width, buffer, size);
             } else {
                 int padLen = width - strLen;
                 str.getChars(0, strLen, buffer, size);
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
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `appendFixedWidthPadRight`. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
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
<details open><summary>▸ Output (~7,698 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact failing test input from StrBuilderAppendInsertTest.testLang299.
        try {
            StrBuilder sb = new StrBuilder(1);
            sb.appendFixedWidthPadRight("foo", 1, '-');

            // Contract asserted: when width > 0 and str.length() >= width, the method must append
            // exactly the first width characters of obj.toString(). A throw-deleting or branch-skipping
            // patch would violate this observable result.
            String got = sb.toString();
            if (!"f".equals(got)) {
                throw new RuntimeException("[oracle:anchor-prefix] metamorphic violation: appendFixedWidthPadRight must keep only the leftmost width characters when input is longer than width input=foo width=1 lhs=" + got + " rhs=f");
            }

            // Shared-state agreement check over buffer/size:
            // toCharArray() "represents the contents of the builder", so it must agree with toString().
            char[] chars = sb.toCharArray();
            String fromChars = new String(chars);
            if (!got.equals(fromChars)) {
                throw new RuntimeException("[oracle:anchor-state] metamorphic violation: toCharArray must agree with toString after appendFixedWidthPadRight input=foo width=1 lhs=" + fromChars + " rhs=" + got);
            }
        } catch (RuntimeException t) {
            boolean fromTarget = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                        && "appendFixedWidthPadRight".equals(ste.getMethodName())) {
                    fromTarget = true;
                    break;
                }
            }
            if (fromTarget && t instanceof ArrayIndexOutOfBoundsException) {
                throw t;
            }
        }

        // EXPLORE: generate valid-by-construction inputs satisfying the root-cause property:
        // width > 0 and obj.toString().length() >= width.
        String core = data.consumeString(32);
        if (core == null) {
            core = "";
        }
        if (core.length() == 0) {
            core = "A";
        }

        int width = data.consumeInt(1, core.length());
        char padChar = (char) (data.consumeByte() & 0xff);
        String prefix = data.consumeString(16);
        String suffix = data.consumeString(16);
        if (prefix == null) {
            prefix = "";
        }
        if (suffix == null) {
            suffix = "";
        }

        boolean doSetLength = data.consumeBoolean();
        boolean doMinimize = data.consumeBoolean();

        try {
            StrBuilder actual = new StrBuilder(1);
            actual.append(prefix);
            int expectedBaseLen = actual.length();

            actual.appendFixedWidthPadRight(core, width, padChar);
            actual.append(suffix);

            if (doSetLength) {
                // setLength updates size/buffer and is documented to either drop trailing chars or add '\0'.
                // We choose a valid non-negative length derived from current state.
                int newLen = data.consumeInt(0, actual.length());
                actual.setLength(newLen);
            }

            if (doMinimize) {
                actual.minimizeCapacity();
            }

            // Oracle 1: equivalent-input sibling agreement.
            // For any correct implementation, when core.length() >= width, appendFixedWidthPadRight(core,width,p)
            // must equal append(core,0,width) because no padding branch should contribute observable characters.
            StrBuilder expected = new StrBuilder(1);
            expected.append(prefix);
            expected.append(core, 0, width);
            expected.append(suffix);

            if (doSetLength) {
                int targetLen = actual.length();
                if (targetLen <= expected.length()) {
                    expected.setLength(targetLen);
                } else {
                    // Should not happen, but if state diverged via exception we skip rather than false-positive.
                    return;
                }
            }

            if (doMinimize) {
                expected.minimizeCapacity();
            }

            String lhs = actual.toString();
            String rhs = expected.toString();
            if (!lhs.equals(rhs)) {
                throw new RuntimeException("[oracle:equiv-append] metamorphic violation: appendFixedWidthPadRight(str,width,pad) must agree with append(str,0,width) when str.length()>=width input=" + core + " width=" + width + " lhs=" + lhs + " rhs=" + rhs);
            }

            // Oracle 2: shared-state agreement on the actual builder.
            // toCharArray() copies the builder contents; therefore it must match toString() exactly.
            char[] arr = actual.toCharArray();
            String arrStr = new String(arr);
            if (!lhs.equals(arrStr)) {
                throw new RuntimeException("[oracle:state-agree] metamorphic violation: toCharArray must agree with toString after appendFixedWidthPadRight input=" + core + " width=" + width + " lhs=" + arrStr + " rhs=" + lhs);
            }

            // Additional direct prefix check around surrounding content.
            String expectedPrefixResult = prefix + core.substring(0, width) + suffix;
            if (!doSetLength && !lhs.equals(expectedPrefixResult)) {
                throw new RuntimeException("[oracle:direct-prefix] metamorphic violation: result must contain exactly prefix + first(width) chars + suffix input=" + core + " width=" + width + " lhs=" + lhs + " rhs=" + expectedPrefixResult);
            }

            // Use expectedBaseLen so the target method's write is observed at the insertion point too.
            if (!doSetLength && lhs.length() >= expectedBaseLen + width) {
                String inserted = lhs.substring(expectedBaseLen, expectedBaseLen + width);
                String expectedInserted = core.substring(0, width);
                if (!inserted.equals(expectedInserted)) {
                    throw new RuntimeException("[oracle:inserted-slice] metamorphic violation: inserted slice must equal first width chars of input input=" + core + " width=" + width + " lhs=" + inserted + " rhs=" + expectedInserted);
                }
            }
        } catch (RuntimeException t) {
            // Clean rejection: swallow argument-validation style exceptions.
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }

            boolean fromTarget = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                        && "appendFixedWidthPadRight".equals(ste.getMethodName())) {
                    fromTarget = true;
                    break;
                }
            }

            // Propagate only the verified root cause from the patched method on valid-by-construction inputs,
            // or our own oracle failures.
            if (t instanceof ArrayIndexOutOfBoundsException && fromTarget) {
                throw t;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }

            // Any other throwable is out of scope for this patch.
        }
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
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,413 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact reproducer from StrBuilderAppendInsertTest.testLang299.
        try {
            StrBuilder sb = new StrBuilder(1);
            sb.appendFixedWidthPadRight("foo", 1, '-');

            String out = sb.toString();
            if (!"f".equals(out)) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: exact regression input must yield test-observed contract output input=foo,width=1 lhs=" + out + " rhs=f");
            }

            // Contract asserted: appendFixedWidthPadRight appends exactly width chars; toCharArray()
            // "represents the contents of the builder". A throw-deleting / size-bookkeeping patch could
            // make toString()/size disagree with the written buffer, so require shared-state agreement.
            char[] chars = sb.toCharArray();
            String fromChars = new String(chars);
            if (!out.equals(fromChars) || chars.length != 1) {
                throw new RuntimeException("[oracle:state] metamorphic violation: builder textual view and char-array view must agree input=foo,width=1 lhs=" + out + "/" + chars.length + " rhs=" + fromChars + "/1");
            }
        } catch (Throwable t) {
            handleThrowable(t, true);
        }

        // EXPLORE: valid-by-construction inputs that satisfy the triggering property:
        // width > 0 and obj.toString().length() >= width. These inputs are documented to be accepted.
        String base = data.consumeString(32);
        if (base == null) {
            base = "";
        }
        if (base.length() == 0) {
            base = "X";
        }
        if (base.length() < 2 && data.consumeBoolean()) {
            base = base + "YZ";
        }

        int width = data.consumeInt(1, base.length());
        char padChar = (char) (data.consumeByte() & 0xff);
        String prefix = data.consumeString(8);
        String suffix = data.consumeString(8);

        try {
            StrBuilder actual = new StrBuilder(1);
            actual.append(prefix);
            actual.appendFixedWidthPadRight(base, width, padChar);
            actual.append(suffix);

            // Metamorphic relation from the documented fixed-width contract:
            // when str.length() >= width, appendFixedWidthPadRight(obj,width,pad) must append
            // exactly the leftmost width chars of obj, i.e. the same observable result as
            // append(obj.toString(), 0, width). A patch that merely suppresses the crash or
            // corrupts size/buffer bookkeeping violates this equality.
            StrBuilder expected = new StrBuilder(1);
            expected.append(prefix);
            expected.append(base, 0, width);
            expected.append(suffix);

            String lhs = actual.toString();
            String rhs = expected.toString();
            if (!lhs.equals(rhs)) {
                throw new RuntimeException("[oracle:prefix-width] metamorphic violation: fixed-width right append must equal append(str,0,width) when strLen>=width input=" + printable(base) + ",width=" + width + ",prefix=" + printable(prefix) + ",suffix=" + printable(suffix) + " lhs=" + printable(lhs) + " rhs=" + printable(rhs));
            }

            // Shared-state agreement check over buffer/size after additional real operations.
            char[] actualChars = actual.toCharArray();
            String charView = new String(actualChars);
            if (!lhs.equals(charView) || actualChars.length != lhs.length()) {
                throw new RuntimeException("[oracle:charview] metamorphic violation: toCharArray must represent builder contents input=" + printable(base) + ",width=" + width + " lhs=" + printable(lhs) + "/" + lhs.length() + " rhs=" + printable(charView) + "/" + actualChars.length);
            }

            // Exercise same shared state through another member that rewrites size/buffer.
            StrBuilder shrunk = new StrBuilder();
            shrunk.append(lhs);
            shrunk.setLength(lhs.length());
            shrunk.minimizeCapacity();
            String shrunkOut = shrunk.toString();
            String shrunkCharView = new String(shrunk.toCharArray());
            if (!lhs.equals(shrunkOut) || !lhs.equals(shrunkCharView)) {
                throw new RuntimeException("[oracle:shared-state] metamorphic violation: setLength/minimizeCapacity/toCharArray must preserve established contents input=" + printable(base) + ",width=" + width + " lhs=" + printable(lhs) + " rhs=" + printable(shrunkOut) + "/" + printable(shrunkCharView));
            }
        } catch (Throwable t) {
            handleThrowable(t, true);
        }
    }

    private static void handleThrowable(Throwable t, boolean validByConstruction) {
        if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        if (isCleanRejection(t)) {
            return;
        }
        if (validByConstruction && isRootCause(t)) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            throw new RuntimeException(t);
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
                || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.lang.text.StrBuilder".equals(e.getClassName())
                    && "appendFixedWidthPadRight".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static String printable(String s) {
        return String.valueOf(s).replace("\u0000", "\\0").replace("\n", "\\n").replace("\r", "\\r");
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
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,268 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            StrBuilder anchor = new StrBuilder(1);
            anchor.appendFixedWidthPadRight("foo", 1, '-');
            if (!"f".equals(anchor.toString())) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: exact regression input must truncate to width input=foo width=1 lhs=" + anchor.toString() + " rhs=f");
            }
            // Contract asserted:
            // appendFixedWidthPadRight appends exactly width characters; when the source string is
            // longer than width, only the first width characters are appended. A throw-deleting or
            // branch-skipping patch could avoid the crash but leave wrong contents/size. Since
            // toCharArray() copies the builder contents represented by the shared buffer/size state,
            // new String(toCharArray()) must agree with toString() for every correct implementation.
            char[] anchorChars = anchor.toCharArray();
            String anchorArrayView = new String(anchorChars);
            if (!anchor.toString().equals(anchorArrayView)) {
                throw new RuntimeException("[oracle:state] metamorphic violation: toCharArray and toString must agree input=foo width=1 lhs=" + anchor.toString() + " rhs=" + anchorArrayView);
            }
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            if (isOracleFailure(t)) {
                throw t;
            }
        }

        String prefix = data.consumeAsciiString(16);
        String body = data.consumeString(32);
        if (body == null) {
            body = "";
        }
        if (body.length() == 0) {
            body = "A";
        }
        if (body.length() == 1) {
            body = body + "B";
        }

        int width = data.consumeInt(1, body.length() - 1);
        char padChar = (char) (data.consumeByte() & 0xff);

        try {
            StrBuilder sb = new StrBuilder(Math.max(1, prefix.length()));
            sb.append(prefix);
            if (data.consumeBoolean()) {
                sb.minimizeCapacity();
            }

            int beforeLen = sb.length();
            String expectedAppended = body.substring(0, width);

            sb.appendFixedWidthPadRight(body, width, padChar);

            String result = sb.toString();
            String expected = prefix + expectedAppended;
            if (!expected.equals(result)) {
                throw new RuntimeException("[oracle:truncate] metamorphic violation: fixed-width append must append only first width chars when input is longer input=" + body + " width=" + width + " lhs=" + result + " rhs=" + expected);
            }

            try {
                sb.setLength(sb.length());
                if (data.consumeBoolean()) {
                    sb.minimizeCapacity();
                }
                char[] chars = sb.toCharArray();
                String arrayView = new String(chars);
                if (!result.equals(arrayView)) {
                    throw new RuntimeException("[oracle:state] metamorphic violation: toCharArray and toString must agree input=" + body + " width=" + width + " lhs=" + result + " rhs=" + arrayView);
                }
                if (sb.length() != beforeLen + width) {
                    throw new RuntimeException("[oracle:size] metamorphic violation: builder length must increase by exactly width input=" + body + " width=" + width + " lhs=" + sb.length() + " rhs=" + (beforeLen + width));
                }
            } catch (RuntimeException t) {
                if (isOracleFailure(t)) {
                    throw t;
                }
                return;
            }
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            if (isOracleFailure(t)) {
                throw t;
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            if ("org.apache.commons.lang.text.StrBuilder".equals(e.getClassName())
                    && "appendFixedWidthPadRight".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isOracleFailure(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
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
## [9] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,842 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        try {
            StrBuilder sb = new StrBuilder(1);
            sb.appendFixedWidthPadRight("foo", 1, '-');

            if (!"f".equals(sb.toString())) {
                throw new RuntimeException("[oracle:anchor-out] metamorphic violation: exact failing test contract input=foo/1/- lhs="
                        + sb.toString() + " rhs=f");
            }

            /* Contract asserted:
             * appendFixedWidthPadRight(Object,int,char) appends exactly width characters;
             * when the source string is longer than width, the content must be truncated to the first width chars.
             * Also, toCharArray() copies the builder contents represented by size/buffer, so a patch that merely
             * suppresses the crash or corrupts size/buffer bookkeeping will make toString() and toCharArray() disagree.
             */
            sb.setLength(sb.length());
            sb.minimizeCapacity();
            char[] chars = sb.toCharArray();
            String copied = new String(chars);
            if (!sb.toString().equals(copied)) {
                throw new RuntimeException("[oracle:anchor-state] metamorphic violation: toString/toCharArray agreement input=foo/1/- lhs="
                        + sb.toString() + " rhs=" + copied);
            }
        } catch (RuntimeException t) {
            if (isOracle(t) || isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        String prefix = data.consumeAsciiString(8);
        if (prefix.length() == 0) {
            prefix = "P";
        }

        String s = data.consumeString(16);
        if (s == null || s.length() == 0) {
            s = "X";
        }

        int width = data.consumeInt(1, 16);
        while (s.length() < width) {
            s = s + "Z";
        }

        int initialCapacity = data.consumeInt(0, 8);
        char pad = (char) (data.consumeByte() & 0x7f);

        StrBuilder lhs = new StrBuilder(initialCapacity);
        StrBuilder rhs = new StrBuilder(initialCapacity);

        try {
            lhs.append(prefix);
            rhs.append(prefix);

            if (data.consumeBoolean()) {
                lhs.setLength(lhs.length());
                rhs.setLength(rhs.length());
            }
            if (data.consumeBoolean()) {
                lhs.minimizeCapacity();
                rhs.minimizeCapacity();
            }

            lhs.appendFixedWidthPadRight(s, width, pad);
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        try {
            /* Contract asserted:
             * For any non-null string with str.length() >= width and width > 0, appendFixedWidthPadRight(str,width,pad)
             * must append exactly the first width characters of str and no padding.
             * Therefore it must agree with the sibling real-library call append(str, 0, width) on the same builder state.
             * A patch that simply deletes the failing copy, skips the write, or corrupts size/buffer will violate this.
             */
            rhs.append(s, 0, width);

            String lhsString = lhs.toString();
            String rhsString = rhs.toString();
            if (!lhsString.equals(rhsString)) {
                throw new RuntimeException("[oracle:truncate-eq] metamorphic violation: appendFixedWidthPadRight(str,width,pad)==append(str,0,width) input="
                        + s + " width=" + width + " prefix=" + prefix + " lhs=" + lhsString + " rhs=" + rhsString);
            }
        } catch (RuntimeException t) {
            if (isOracle(t)) {
                throw t;
            }
            return;
        }

        try {
            lhs.setLength(lhs.length());
            lhs.minimizeCapacity();
            char[] arr = lhs.toCharArray();
            String arrString = new String(arr);
            String text = lhs.toString();
            if (!text.equals(arrString)) {
                throw new RuntimeException("[oracle:state-agree] metamorphic violation: toString/toCharArray agreement input="
                        + s + " width=" + width + " prefix=" + prefix + " lhs=" + text + " rhs=" + arrString);
            }
        } catch (RuntimeException t) {
            if (isOracle(t)) {
                throw t;
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.lang.text.StrBuilder".equals(e.getClassName())
                    && "appendFixedWidthPadRight".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isOracle(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
    }
}
```

</details>

---
## [10] ⚙️ harness-attempt · `attempt_004`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException

---
## [11] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,682 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runValidCase("foo", 1, '-');

        String prefix = data.consumeString(16);
        String core = data.consumeString(64);
        int extra = data.consumeInt(0, 32);
        char pad = (char) (data.consumeByte() & 0xff);

        if (core == null) {
            core = "";
        }
        if (core.length() == 0) {
            core = "A";
        }

        String longInput = prefix + core;
        if (longInput.length() == 0) {
            longInput = "B";
        }

        int width = data.consumeInt(1, longInput.length() + extra);
        if (width > longInput.length()) {
            longInput = longInput + repeat('Z', width - longInput.length());
        }
        if (longInput.length() == 0) {
            return;
        }
        if (width <= 0 || width > longInput.length()) {
            return;
        }

        runValidCase(longInput, width, pad);
    }

    private static void runValidCase(String input, int width, char padChar) {
        if (input == null || width <= 0 || input.length() < width) {
            return;
        }

        try {
            StrBuilder actual = new StrBuilder(1);
            actual.appendFixedWidthPadRight(input, width, padChar);

            StrBuilder expected = new StrBuilder(1);
            expected.append(input, 0, width);

            try {
                actual.minimizeCapacity();
                actual.setLength(actual.length());
                String actualString = actual.toString();
                String expectedString = expected.toString();

                if (!actualString.equals(expectedString)) {
                    throw new RuntimeException(
                        "[oracle:fixed-width-truncate] metamorphic violation: appendFixedWidthPadRight(str,width,pad) must agree with append(str,0,width) when str.length()>=width input="
                            + quote(input) + " width=" + width + " lhs=" + quote(actualString) + " rhs=" + quote(expectedString));
                }

                char[] chars = actual.toCharArray();
                String fromChars = new String(chars);
                if (!fromChars.equals(actualString)) {
                    throw new RuntimeException(
                        "[oracle:state-agreement] metamorphic violation: toCharArray() must represent the same contents established by append/minimizeCapacity/setLength input="
                            + quote(input) + " width=" + width + " lhs=" + quote(fromChars) + " rhs=" + quote(actualString));
                }
            } catch (RuntimeException oracle) {
                throw oracle;
            } catch (Throwable ignored) {
                return;
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (t instanceof RuntimeException && isRootCause(t)) {
                throw t;
            }
            throwIfOracle(t);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void throwIfOracle(Throwable t) {
        if (t instanceof RuntimeException) {
            String msg = t.getMessage();
            if (msg != null && msg.startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.lang.text.StrBuilder".equals(e.getClassName())
                && "appendFixedWidthPadRight".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static String repeat(char ch, int count) {
        if (count <= 0) {
            return "";
        }
        char[] out = new char[count];
        for (int i = 0; i < count; i++) {
            out[i] = ch;
        }
        return new String(out);
    }

    private static String quote(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }
}
```

</details>

---
## [12] ⚙️ harness-attempt · `attempt_005`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException

---
## [13] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,901 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact regression test input from StrBuilderAppendInsertTest.testLang299.
        try {
            StrBuilder sb = new StrBuilder(1);
            sb.appendFixedWidthPadRight("foo", 1, '-');

            // Contract/oracle: when obj.toString().length() >= width, appendFixedWidthPadRight
            // must append exactly the first width characters. A throw-deleting or bookkeeping-
            // deleting patch would violate this observable result.
            String got = sb.toString();
            if (!"f".equals(got)) {
                throw new RuntimeException("[oracle:anchor-prefix] metamorphic violation: exact regression result input=foo,width=1 lhs=" + got + " rhs=f");
            }

            // Shared-state agreement check: writer-established content must agree with what a reader reports.
            try {
                java.io.Reader r = sb.asReader();
                StringBuffer readBack = new StringBuffer();
                char[] tmp = new char[8];
                int n;
                while ((n = r.read(tmp)) != -1) {
                    readBack.append(tmp, 0, n);
                }
                String readerView = readBack.toString();
                String charArrayView = new String(sb.toCharArray());
                if (!got.equals(readerView) || !got.equals(charArrayView)) {
                    throw new RuntimeException("[oracle:anchor-reader] metamorphic violation: reader/toCharArray agreement input=foo,width=1 lhs=" + got + " rhsReader=" + readerView + " rhsChars=" + charArrayView);
                }
            } catch (Throwable t) {
                if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                }
                return;
            }
        } catch (RuntimeException t) {
            boolean validation = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            if (!validation) {
                boolean inPatchedMethod = false;
                for (int i = 0; i < t.getStackTrace().length; i++) {
                    StackTraceElement ste = t.getStackTrace()[i];
                    if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                            && "appendFixedWidthPadRight".equals(ste.getMethodName())) {
                        inPatchedMethod = true;
                        break;
                    }
                }
                if (t instanceof ArrayIndexOutOfBoundsException && inPatchedMethod) {
                    throw t;
                }
            }
        }

        int prefixLen = data.consumeInt(0, 8);
        int width = data.consumeInt(1, 32);
        int extra = data.consumeInt(1, 16);

        String prefix = data.consumeAsciiString(prefixLen);
        int neededSourceLen = prefix.length() + (2 * width) + extra;
        String fuzz = data.consumeAsciiString(neededSourceLen + 8);
        if (fuzz.length() < neededSourceLen) {
            StringBuffer sbuf = new StringBuffer(fuzz);
            while (sbuf.length() < neededSourceLen) {
                sbuf.append('X');
            }
            fuzz = sbuf.toString();
        } else if (fuzz.length() > neededSourceLen) {
            fuzz = fuzz.substring(0, neededSourceLen);
        }
        char pad = (char) ('!' + (data.consumeInt(0, 93)));

        // EXPLORE valid-by-construction inputs:
        // width > 0, source is non-null, and source.length() > width.
        // Additionally, with size == prefix.length() and capacity minimized to size,
        // Commons Lang's ensureCapacity(size + width) on the buggy version can still leave
        // room only for size + 2*width chars, so choosing source.length() > size + 2*width
        // exercises the buggy branch on many varied real inputs.
        StrBuilder lhs = new StrBuilder(prefix.length());
        try {
            lhs.append(prefix);
            lhs.minimizeCapacity();
            lhs.appendFixedWidthPadRight(fuzz, width, pad);
        } catch (RuntimeException t) {
            boolean validation = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            if (validation) {
                return;
            }
            boolean inPatchedMethod = false;
            for (int i = 0; i < t.getStackTrace().length; i++) {
                StackTraceElement ste = t.getStackTrace()[i];
                if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                        && "appendFixedWidthPadRight".equals(ste.getMethodName())) {
                    inPatchedMethod = true;
                    break;
                }
            }
            if (t instanceof ArrayIndexOutOfBoundsException && inPatchedMethod) {
                throw t;
            }
            return;
        }

        StrBuilder rhs = new StrBuilder(prefix.length() + width);
        try {
            rhs.append(prefix);
            // Documented/code-visible equivalence for strLen >= width:
            // appendFixedWidthPadRight(obj, width, pad) appends exactly the first width chars,
            // so it must agree with append(str, 0, width). This catches silent wrong-output fixes.
            rhs.append(fuzz, 0, width);
        } catch (RuntimeException t) {
            return;
        }

        String lhsString;
        String rhsString;
        try {
            lhsString = lhs.toString();
            rhsString = rhs.toString();
        } catch (RuntimeException t) {
            return;
        }

        if (!lhsString.equals(rhsString)) {
            throw new RuntimeException("[oracle:fixedwidth-prefix] metamorphic violation: appendFixedWidthPadRight(obj,width,pad) must equal append(str,0,width) when str.length>=width inputPrefix=" + prefix + " inputStr=" + fuzz + " width=" + width + " lhs=" + lhsString + " rhs=" + rhsString);
        }

        // Shared-state agreement check mandated by the prompt:
        // the builder writes buffer/size; reader and toCharArray report from the same state and must agree.
        try {
            java.io.Reader r = lhs.asReader();
            StringBuffer readBack = new StringBuffer();
            char[] tmp = new char[16];
            int n;
            while ((n = r.read(tmp)) != -1) {
                readBack.append(tmp, 0, n);
            }
            String readerView = readBack.toString();
            String charArrayView = new String(lhs.toCharArray());
            if (!lhsString.equals(readerView) || !lhsString.equals(charArrayView)) {
                throw new RuntimeException("[oracle:state-agreement] metamorphic violation: toString/asReader/toCharArray must agree inputPrefix=" + prefix + " inputStr=" + fuzz + " width=" + width + " lhs=" + lhsString + " rhsReader=" + readerView + " rhsChars=" + charArrayView);
            }
        } catch (RuntimeException t) {
            throw t;
        } catch (Throwable t) {
            return;
        }

        // Additional state mutation check using another shared-state member.
        try {
            StrBuilder cut = new StrBuilder();
            cut.append(lhsString);
            int keep = data.consumeInt(0, lhsString.length());
            cut.setLength(keep);
            String expected = lhsString.substring(0, keep);
            String actual = cut.toString();
            if (!expected.equals(actual)) {
                throw new RuntimeException("[oracle:setlength] metamorphic violation: setLength must truncate to the requested prefix input=" + lhsString + " keep=" + keep + " lhs=" + actual + " rhs=" + expected);
            }
        } catch (RuntimeException t) {
            throw t;
        }
    }
}
```

</details>

---
## [14] ⚙️ harness-attempt · `attempt_006`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException

---
## [15] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,025 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            StrBuilder sb = new StrBuilder(1);
            sb.appendFixedWidthPadRight("foo", 1, '-');
            String out = sb.toString();
            if (!"f".equals(out)) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: exact failing test contract input=foo,width=1,pad=- lhs=" + out + " rhs=f");
            }
            /* Contract asserted:
             * appendFixedWidthPadRight appends exactly width characters; when the input string is longer than width,
             * it must append the leftmost width characters. The failing test documents this as "foo",1 -> "f".
             * Also, toCharArray() copies the builder's contents, so it must agree with toString() even if a throw-deleting
             * patch silently corrupts size/buffer bookkeeping.
             */
            String arrView = new String(sb.toCharArray());
            if (!out.equals(arrView)) {
                throw new RuntimeException("[oracle:state-anchor] metamorphic violation: toString/toCharArray agreement input=foo,width=1 lhs=" + out + " rhs=" + arrView);
            }
            sb.minimizeCapacity();
            String minimized = sb.toString();
            if (!out.equals(minimized)) {
                throw new RuntimeException("[oracle:mincap-anchor] metamorphic violation: minimizeCapacity preserves contents input=foo,width=1 lhs=" + out + " rhs=" + minimized);
            }
        } catch (RuntimeException t) {
            String msg = t.getMessage();
            if (msg != null && msg.startsWith("[oracle:")) {
                throw t;
            }
            boolean throughTarget = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                        && "appendFixedWidthPadRight".equals(ste.getMethodName())) {
                    throughTarget = true;
                    break;
                }
            }
            if (t instanceof ArrayIndexOutOfBoundsException && throughTarget) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            return;
        }

        int width = data.consumeInt(1, 32);
        char padChar = (char) (data.consumeByte() & 0xFF);
        int capacity = data.consumeInt(1, 16);
        String prefix = data.consumeString(12);
        String raw = data.consumeString(48);

        String str = raw;
        if (str == null) {
            str = "";
        }
        if (str.length() < width) {
            StringBuilder fill = new StringBuilder(str);
            while (fill.length() < width) {
                fill.append('X');
            }
            str = fill.toString();
        }

        try {
            StrBuilder lhs = new StrBuilder(capacity);
            lhs.append(prefix);
            lhs.appendFixedWidthPadRight(str, width, padChar);

            StrBuilder rhs = new StrBuilder(capacity);
            rhs.append(prefix);
            rhs.append(str, 0, width);

            /* Contract asserted:
             * For str.length() >= width, appendFixedWidthPadRight(obj,width,pad) must append exactly the first width
             * characters of obj.toString(); no padding is used in that branch. Therefore it must agree with the real
             * sibling API append(String,startIndex,length) on equivalent inputs. This catches silent wrong-output fixes.
             */
            String lhsString = lhs.toString();
            String rhsString = rhs.toString();
            if (!lhsString.equals(rhsString)) {
                throw new RuntimeException("[oracle:fwpr-equiv] metamorphic violation: appendFixedWidthPadRight(str,width,pad)==append(str,0,width) input=str=" + str + ",width=" + width + ",prefix=" + prefix + " lhs=" + lhsString + " rhs=" + rhsString);
            }

            /* Shared-state agreement:
             * toCharArray() "copies the builder's character array into a new character array", so it must represent the
             * same logical contents reported by toString(). minimizeCapacity() must not change contents.
             */
            String lhsChars = new String(lhs.toCharArray());
            if (!lhsString.equals(lhsChars)) {
                throw new RuntimeException("[oracle:state-array] metamorphic violation: toString/toCharArray agreement input=str=" + str + ",width=" + width + ",prefix=" + prefix + " lhs=" + lhsString + " rhs=" + lhsChars);
            }

            lhs.minimizeCapacity();
            String afterMin = lhs.toString();
            if (!lhsString.equals(afterMin)) {
                throw new RuntimeException("[oracle:mincap] metamorphic violation: minimizeCapacity preserves contents input=str=" + str + ",width=" + width + ",prefix=" + prefix + " lhs=" + lhsString + " rhs=" + afterMin);
            }
        } catch (RuntimeException t) {
            String msg = t.getMessage();
            if (msg != null && msg.startsWith("[oracle:")) {
                throw t;
            }
            boolean throughTarget = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                        && "appendFixedWidthPadRight".equals(ste.getMethodName())) {
                    throughTarget = true;
                    break;
                }
            }
            if (t instanceof ArrayIndexOutOfBoundsException && throughTarget) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            return;
        }
    }
}
```

</details>

---
## [16] ⚙️ harness-attempt · `attempt_007`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException

---
## [17] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,084 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            StrBuilder anchor = new StrBuilder(1);
            anchor.appendFixedWidthPadRight("foo", 1, '-');

            String anchorString = anchor.toString();
            try {
                anchor.minimizeCapacity();
                String anchorChars = new String(anchor.toCharArray());
                if (!"f".equals(anchorString) || !"f".equals(anchorChars) || anchor.length() != 1) {
                    throw new RuntimeException("[oracle:fwpr-anchor] metamorphic violation: exact failing test contract input=foo,width=1 lhs="
                            + anchorString + " rhs=" + anchorChars);
                }
            } catch (RuntimeException oracleSide) {
                throw oracleSide;
            }
        } catch (RuntimeException t) {
            boolean fromTarget = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                        && "appendFixedWidthPadRight".equals(ste.getMethodName())) {
                    fromTarget = true;
                    break;
                }
            }
            if (fromTarget && t instanceof ArrayIndexOutOfBoundsException) {
                throw t;
            }
        }

        int width = data.consumeInt(1, 32);
        int initialCapacity = data.consumeInt(1, 8);
        char padChar = (char) (data.consumeByte() & 0xff);
        String prefix = data.consumeString(8);
        String suffix = data.consumeString(8);
        String base = data.consumeString(40);

        if (base == null) {
            base = "";
        }
        if (base.length() < width) {
            StringBuilder grown = new StringBuilder(base);
            while (grown.length() < width) {
                grown.append('X');
            }
            base = grown.toString();
        }

        try {
            StrBuilder lhs = new StrBuilder(initialCapacity);
            lhs.append(prefix);
            lhs.appendFixedWidthPadRight(base, width, padChar);
            lhs.append(suffix);

            StrBuilder rhs = new StrBuilder(initialCapacity);
            rhs.append(prefix);
            rhs.append(base, 0, width);
            rhs.append(suffix);

            String lhsString;
            String rhsString;
            try {
                lhsString = lhs.toString();
                rhsString = rhs.toString();
            } catch (RuntimeException ignored) {
                return;
            }

            if (!lhsString.equals(rhsString)) {
                throw new RuntimeException("[oracle:fwpr-eq] metamorphic violation: appendFixedWidthPadRight(str,width,pad) must equal append(str,0,width) when str.length()>=width input="
                        + base + " lhs=" + lhsString + " rhs=" + rhsString);
            }

            try {
                lhs.minimizeCapacity();
                String charView = new String(lhs.toCharArray());
                if (!lhsString.equals(charView) || lhs.length() != prefix.length() + width + suffix.length()) {
                    throw new RuntimeException("[oracle:fwpr-state] metamorphic violation: toCharArray/length must agree with appended contents input="
                            + base + " lhs=" + lhsString + " rhs=" + charView);
                }
            } catch (RuntimeException ignored) {
                return;
            }
        } catch (RuntimeException t) {
            boolean fromTarget = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                        && "appendFixedWidthPadRight".equals(ste.getMethodName())) {
                    fromTarget = true;
                    break;
                }
            }

            if (fromTarget && t instanceof ArrayIndexOutOfBoundsException) {
                throw t;
            }

            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }

            String name = t.getClass().getName();
            if (name.contains("Invalid") || name.contains("Validation")) {
                return;
            }

            if (name.startsWith("java.lang.IndexOutOfBoundsException")
                    || name.startsWith("java.lang.StringIndexOutOfBoundsException")
                    || name.startsWith("java.lang.NullPointerException")
                    || name.startsWith("java.lang.ClassCastException")
                    || name.startsWith("java.lang.ArithmeticException")) {
                return;
            }

            if (name.startsWith("java.lang.RuntimeException") && t.getMessage() != null
                    && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
        }
    }
}
```

</details>

---
## [18] ⚙️ harness-attempt · `attempt_008`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException

---
## [19] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,925 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact failing test from StrBuilderAppendInsertTest.testLang299.
        try {
            StrBuilder sb = new StrBuilder(1);
            sb.appendFixedWidthPadRight("foo", 1, '-');
            String out = sb.toString();
            if (!"f".equals(out)) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: exact regression input must produce the documented truncation input=foo,width=1 lhs=" + out + " rhs=f");
            }
            char[] chars = sb.toCharArray();
            String fromChars = new String(chars);
            if (!out.equals(fromChars)) {
                throw new RuntimeException("[oracle:state] metamorphic violation: toString and toCharArray must agree after appendFixedWidthPadRight input=foo,width=1 lhs=" + out + " rhs=" + fromChars);
            }
        } catch (RuntimeException t) {
            boolean inPatchedMethod = false;
            for (StackTraceElement e : t.getStackTrace()) {
                if ("org.apache.commons.lang.text.StrBuilder".equals(e.getClassName())
                        && "appendFixedWidthPadRight".equals(e.getMethodName())) {
                    inPatchedMethod = true;
                    break;
                }
            }
            if (t instanceof ArrayIndexOutOfBoundsException && inPatchedMethod) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
        }

        // EXPLORE: valid-by-construction inputs with width > 0 and str.length() >= width.
        // Documented guarantee from appendFixedWidthPadRight: when the string is at least as wide
        // as width, it appends exactly the first `width` characters and then increases size by width.
        // Therefore, for valid inputs, it must agree with append(str, 0, width), and readers over the
        // shared {buffer,size} state (toString/toCharArray, including after minimizeCapacity()) must agree.
        String prefix = data.consumeAsciiString(16);
        String middle = data.consumeString(32);
        if (middle == null || middle.length() == 0) {
            middle = "X";
        }
        String suffix = data.consumeAsciiString(16);
        String str = middle + suffix;
        if (str.length() == 0) {
            str = "Y";
        }

        int width = data.consumeInt(1, Math.min(16, str.length()));
        char padChar = (char) (data.consumeByte() & 0xff);
        int initialCapacity = data.consumeInt(0, 32);

        StrBuilder lhs = new StrBuilder(initialCapacity);
        StrBuilder rhs = new StrBuilder(initialCapacity);

        try {
            lhs.append(prefix);
            lhs.appendFixedWidthPadRight(str, width, padChar);
        } catch (RuntimeException t) {
            boolean inPatchedMethod = false;
            for (StackTraceElement e : t.getStackTrace()) {
                if ("org.apache.commons.lang.text.StrBuilder".equals(e.getClassName())
                        && "appendFixedWidthPadRight".equals(e.getMethodName())) {
                    inPatchedMethod = true;
                    break;
                }
            }
            if (t instanceof ArrayIndexOutOfBoundsException && inPatchedMethod) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            return;
        }

        try {
            rhs.append(prefix);
            rhs.append(str, 0, width);
        } catch (RuntimeException t) {
            return;
        }

        String lhsString;
        String rhsString;
        try {
            lhsString = lhs.toString();
            rhsString = rhs.toString();
        } catch (RuntimeException t) {
            return;
        }

        if (!lhsString.equals(rhsString)) {
            throw new RuntimeException("[oracle:eqv] metamorphic violation: appendFixedWidthPadRight(str,width,pad) must equal append(str,0,width) when str.length()>=width input=" + str + " width=" + width + " prefix=" + prefix + " lhs=" + lhsString + " rhs=" + rhsString);
        }

        try {
            char[] beforeMin = lhs.toCharArray();
            String beforeMinString = new String(beforeMin);
            if (!lhsString.equals(beforeMinString)) {
                throw new RuntimeException("[oracle:state] metamorphic violation: toString and toCharArray must agree on shared buffer/size state input=" + str + " width=" + width + " lhs=" + lhsString + " rhs=" + beforeMinString);
            }

            lhs.minimizeCapacity();
            String afterMin = lhs.toString();
            String afterMinChars = new String(lhs.toCharArray());
            if (!lhsString.equals(afterMin) || !afterMin.equals(afterMinChars)) {
                throw new RuntimeException("[oracle:mincap] metamorphic violation: minimizeCapacity must preserve contents and readers must still agree input=" + str + " width=" + width + " lhs=" + lhsString + " rhs=" + afterMin + " chars=" + afterMinChars);
            }

            lhs.setLength(afterMin.length());
            String afterSet = lhs.toString();
            String afterSetChars = new String(lhs.toCharArray());
            if (!afterMin.equals(afterSet) || !afterSet.equals(afterSetChars)) {
                throw new RuntimeException("[oracle:setlen] metamorphic violation: setLength(currentLength) must preserve contents and readers must agree input=" + str + " width=" + width + " lhs=" + afterMin + " rhs=" + afterSet + " chars=" + afterSetChars);
            }
        } catch (RuntimeException t) {
            return;
        }
    }
}
```

</details>

---
## [20] ⚙️ harness-attempt · `attempt_009`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException

---
## [21] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,396 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        StrBuilder sb = new StrBuilder(1);
        try {
            sb.appendFixedWidthPadRight("foo", 1, '-');
        } catch (RuntimeException t) {
            if (isValidationException(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        String actual = sb.toString();
        if (!"f".equals(actual)) {
            throw new RuntimeException("[oracle:anchor] metamorphic violation: appendFixedWidthPadRight(\"foo\",1,'-') must append exactly the first width characters input=foo width=1 lhs=" + actual + " rhs=f");
        }

        /* Contract used:
         * - appendFixedWidthPadRight appends the object as a fixed-width field, truncating on the right when the string is longer than width.
         * - toCharArray "Copies the builder's character array into a new character array" and therefore must agree with the builder's visible contents.
         * - minimizeCapacity only changes capacity, not contents.
         * A throw-deleting or bookkeeping-breaking patch could leave toString seemingly right while shared state (buffer/size) disagrees; these checks catch that.
         */
        String charsView = new String(sb.toCharArray());
        if (!actual.equals(charsView)) {
            throw new RuntimeException("[oracle:state] metamorphic violation: toCharArray must agree with toString after fixed-width append input=foo width=1 lhs=" + charsView + " rhs=" + actual);
        }

        try {
            sb.minimizeCapacity();
        } catch (RuntimeException t) {
            return;
        }
        String afterMin = sb.toString();
        String charsAfterMin = new String(sb.toCharArray());
        if (!actual.equals(afterMin) || !afterMin.equals(charsAfterMin)) {
            throw new RuntimeException("[oracle:mincap] metamorphic violation: minimizeCapacity must preserve contents input=foo width=1 lhs=" + afterMin + "/" + charsAfterMin + " rhs=" + actual);
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        String prefix = data.consumeString(16);
        String core = data.consumeString(32);
        String suffix = data.consumeRemainingAsString();

        if (core == null || core.length() == 0) {
            core = "A";
        }

        int maxWidth = core.length();
        if (maxWidth <= 0) {
            return;
        }
        int width = data.consumeInt(1, maxWidth);
        char padChar = (char) (data.consumeByte() & 0xff);

        StrBuilder sb = new StrBuilder(1);
        try {
            sb.append(prefix);
            sb.appendFixedWidthPadRight(core, width, padChar);
            sb.append(suffix);
        } catch (RuntimeException t) {
            if (isValidationException(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        String expected = safe(prefix) + fixedWidth(core, width, padChar) + safe(suffix);
        String actual = sb.toString();
        if (!expected.equals(actual)) {
            throw new RuntimeException("[oracle:content] metamorphic violation: composed append result must equal prefix + fixed-width(core) + suffix input=prefix(" + printable(prefix) + "),core(" + printable(core) + "),width=" + width + ",pad=" + (int) padChar + ",suffix(" + printable(suffix) + ") lhs=" + printable(actual) + " rhs=" + printable(expected));
        }

        String charsView;
        try {
            charsView = new String(sb.toCharArray());
        } catch (RuntimeException t) {
            return;
        }
        if (!actual.equals(charsView)) {
            throw new RuntimeException("[oracle:chararray] metamorphic violation: toCharArray must agree with visible contents input=core(" + printable(core) + "),width=" + width + " lhs=" + printable(charsView) + " rhs=" + printable(actual));
        }

        try {
            sb.minimizeCapacity();
        } catch (RuntimeException t) {
            return;
        }
        String afterMin = sb.toString();
        String charsAfterMin;
        try {
            charsAfterMin = new String(sb.toCharArray());
        } catch (RuntimeException t) {
            return;
        }
        if (!actual.equals(afterMin) || !afterMin.equals(charsAfterMin)) {
            throw new RuntimeException("[oracle:shared-state] metamorphic violation: minimizeCapacity must preserve buffer/size-observable contents after fixed-width append input=core(" + printable(core) + "),width=" + width + " lhs=" + printable(afterMin) + "/" + printable(charsAfterMin) + " rhs=" + printable(actual));
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement ste = trace[i];
            if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                    && "appendFixedWidthPadRight".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValidationException(Throwable t) {
        return t instanceof IllegalArgumentException;
    }

    private static String fixedWidth(String s, int width, char padChar) {
        if (width <= 0) {
            return "";
        }
        if (s == null) {
            s = "null";
        }
        if (s.length() >= width) {
            return s.substring(0, width);
        }
        StringBuffer out = new StringBuffer(width);
        out.append(s);
        for (int i = s.length(); i < width; i++) {
            out.append(padChar);
        }
        return out.toString();
    }

    private static String safe(String s) {
        return s == null ? "null" : s;
    }

    private static String printable(String s) {
        return String.valueOf(s);
    }
}
```

</details>

---
## [22] ⚙️ harness-attempt · `attempt_010`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException

---
## [23] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,211 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        {
            StrBuilder sb = new StrBuilder(1);
            try {
                sb.appendFixedWidthPadRight("foo", 1, '-');
            } catch (RuntimeException t) {
                boolean inPatchedMethod = false;
                StackTraceElement[] st = t.getStackTrace();
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.lang.text.StrBuilder".equals(st[i].getClassName())
                            && "appendFixedWidthPadRight".equals(st[i].getMethodName())) {
                        inPatchedMethod = true;
                        break;
                    }
                }
                if (inPatchedMethod && t instanceof ArrayIndexOutOfBoundsException) {
                    throw t;
                }
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    return;
                }
                return;
            }

            String s = sb.toString();
            if (!"f".equals(s)) {
                throw new RuntimeException("[oracle:anchor-prefix] metamorphic violation: appendFixedWidthPadRight on width 1 must keep only the leftmost character for overlong input input=foo width=1 lhs=" + s + " rhs=f");
            }

            /* Contract asserted:
             * - appendFixedWidthPadRight appends exactly width characters; when strLen >= width, the content is the first width chars.
             * - toCharArray "represents the contents of the builder".
             * A throw-deleting or silently-wrong patch could leave internal state inconsistent even if no exception fires.
             */
            char[] chars = sb.toCharArray();
            String charsAsString = new String(chars);
            if (!s.equals(charsAsString) || chars.length != 1) {
                throw new RuntimeException("[oracle:anchor-state] metamorphic violation: toCharArray must agree with builder contents after appendFixedWidthPadRight input=foo width=1 lhs=" + charsAsString + " rhs=" + s);
            }

            sb.minimizeCapacity();
            char[] chars2 = sb.toCharArray();
            String s2 = sb.toString();
            if (!s2.equals(new String(chars2))) {
                throw new RuntimeException("[oracle:anchor-mincap] metamorphic violation: minimizeCapacity must not change visible contents input=foo width=1 lhs=" + new String(chars2) + " rhs=" + s2);
            }
        }

        String base = data.consumeAsciiString(32);
        if (base.length() == 0) {
            base = "X";
        }

        int width = data.consumeInt(1, base.length());
        char pad = (char) (data.consumeByte() & 0x7f);

        StrBuilder left = new StrBuilder(width);
        try {
            left.appendFixedWidthPadRight(base, width, pad);
        } catch (RuntimeException t) {
            boolean inPatchedMethod = false;
            StackTraceElement[] st = t.getStackTrace();
            for (int i = 0; i < st.length; i++) {
                if ("org.apache.commons.lang.text.StrBuilder".equals(st[i].getClassName())
                        && "appendFixedWidthPadRight".equals(st[i].getMethodName())) {
                    inPatchedMethod = true;
                    break;
                }
            }
            if (inPatchedMethod && t instanceof ArrayIndexOutOfBoundsException) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            return;
        }

        StrBuilder right = new StrBuilder(width);
        try {
            right.append(base, 0, width);
        } catch (RuntimeException t) {
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            return;
        }

        /* Documented/visible guarantee:
         * when strLen >= width, appendFixedWidthPadRight copies exactly the first width characters and adds no padding.
         * Therefore, for valid-by-construction inputs with 1 <= width <= base.length(),
         * appendFixedWidthPadRight(base, width, pad) must equal append(base, 0, width).
         * This catches silent wrong-output patches that avoid the crash but append the wrong content or corrupt size/buffer state.
         */
        String lhs = left.toString();
        String rhs = right.toString();
        if (!lhs.equals(rhs)) {
            throw new RuntimeException("[oracle:prefix-eq] metamorphic violation: appendFixedWidthPadRight(base,width,pad) must equal append(base,0,width) when base is at least width chars input=" + base + " width=" + width + " lhs=" + lhs + " rhs=" + rhs);
        }

        char[] leftChars = left.toCharArray();
        if (!lhs.equals(new String(leftChars)) || leftChars.length != width) {
            throw new RuntimeException("[oracle:chararray-agree] metamorphic violation: toCharArray must agree with visible contents and width after appendFixedWidthPadRight input=" + base + " width=" + width + " lhs=" + new String(leftChars) + " rhs=" + lhs);
        }

        left.minimizeCapacity();
        String afterMin = left.toString();
        char[] afterMinChars = left.toCharArray();
        if (!afterMin.equals(lhs) || !afterMin.equals(new String(afterMinChars))) {
            throw new RuntimeException("[oracle:mincap-preserve] metamorphic violation: minimizeCapacity must preserve contents established by appendFixedWidthPadRight input=" + base + " width=" + width + " lhs=" + afterMin + " rhs=" + lhs);
        }

        left.setLength(width);
        String afterSetLength = left.toString();
        if (!afterSetLength.equals(lhs)) {
            throw new RuntimeException("[oracle:setlength-noop] metamorphic violation: setLength(currentLength) must preserve contents after appendFixedWidthPadRight input=" + base + " width=" + width + " lhs=" + afterSetLength + " rhs=" + lhs);
        }
    }
}
```

</details>

---
## [24] ⚙️ harness-attempt · `attempt_011`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException

---
## [25] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,648 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        {
            StrBuilder sb = new StrBuilder(1);
            try {
                sb.appendFixedWidthPadRight("foo", 1, '-');
                if (!"f".equals(sb.toString())) {
                    throw new RuntimeException("[oracle:anchor] metamorphic violation: exact regression input should produce truncated prefix input=foo width=1 lhs=" + sb.toString() + " rhs=f");
                }
                char[] chars = sb.toCharArray();
                String rebuilt = new String(chars);
                if (!rebuilt.equals(sb.toString())) {
                    throw new RuntimeException("[oracle:state-anchor] metamorphic violation: toCharArray and toString must report the same builder contents input=foo width=1 lhs=" + rebuilt + " rhs=" + sb.toString());
                }
            } catch (RuntimeException t) {
                boolean fromPatchedMethod = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                            && "appendFixedWidthPadRight".equals(ste.getMethodName())) {
                        fromPatchedMethod = true;
                        break;
                    }
                }
                if (t instanceof ArrayIndexOutOfBoundsException && fromPatchedMethod) {
                    throw t;
                }
            }
        }

        String prefix = data.consumeString(16);
        String body = data.consumeString(32);
        String suffix = data.consumeString(16);
        char padChar = (char) (data.consumeByte() & 0xff);

        if (body == null || body.length() == 0) {
            body = "X";
        }

        int width = data.consumeInt(1, body.length());
        int initialCapacity = data.consumeInt(1, Math.max(1, prefix.length() + width + 4));

        StrBuilder lhs = new StrBuilder(initialCapacity);
        StrBuilder rhs = new StrBuilder(initialCapacity);

        try {
            lhs.append(prefix);
            rhs.append(prefix);
        } catch (RuntimeException t) {
            return;
        }

        try {
            lhs.appendFixedWidthPadRight(body, width, padChar);
        } catch (RuntimeException t) {
            boolean fromPatchedMethod = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                        && "appendFixedWidthPadRight".equals(ste.getMethodName())) {
                    fromPatchedMethod = true;
                    break;
                }
            }
            if (t instanceof ArrayIndexOutOfBoundsException && fromPatchedMethod) {
                throw t;
            }
            return;
        }

        try {
            rhs.append(body, 0, width);
        } catch (RuntimeException t) {
            return;
        }

        String lhsAfter;
        String rhsAfter;
        try {
            lhsAfter = lhs.toString();
            rhsAfter = rhs.toString();
        } catch (RuntimeException t) {
            return;
        }

        /* Contract/oracle:
           When obj.toString().length() >= width and width > 0, appendFixedWidthPadRight appends exactly the first width
           characters of that string; append(String, 0, width) appends that same slice. A throw-deleting or wrong-bookkeeping
           patch can avoid the crash but still append the wrong characters or wrong length, so these two real-library calls must agree.
        */
        if (!lhsAfter.equals(rhsAfter)) {
            throw new RuntimeException("[oracle:equiv-append] metamorphic violation: appendFixedWidthPadRight(obj,width,pad) must equal append(str,0,width) when str.length()>=width input=" + body + " width=" + width + " lhs=" + lhsAfter + " rhs=" + rhsAfter);
        }

        try {
            char[] chars = lhs.toCharArray();
            String rebuilt = new String(chars);
            if (!rebuilt.equals(lhsAfter)) {
                throw new RuntimeException("[oracle:state-chararray] metamorphic violation: toCharArray and toString must report the same contents input=" + body + " width=" + width + " lhs=" + rebuilt + " rhs=" + lhsAfter);
            }
        } catch (RuntimeException t) {
            return;
        }

        try {
            int beforeLen = lhs.length();
            lhs.minimizeCapacity();
            lhs.setLength(beforeLen);
            String afterStateOps = lhs.toString();
            if (!afterStateOps.equals(lhsAfter)) {
                throw new RuntimeException("[oracle:state-length] metamorphic violation: minimizeCapacity and setLength(currentLength) must preserve contents input=" + body + " width=" + width + " lhs=" + afterStateOps + " rhs=" + lhsAfter);
            }
        } catch (RuntimeException t) {
            return;
        }

        try {
            lhs.append(suffix);
            rhs.append(suffix);
            String lhsFinal = lhs.toString();
            String rhsFinal = rhs.toString();
            if (!lhsFinal.equals(rhsFinal)) {
                throw new RuntimeException("[oracle:suffix-compose] metamorphic violation: equal builders must stay equal after appending same suffix input=" + body + " width=" + width + " suffix=" + suffix + " lhs=" + lhsFinal + " rhs=" + rhsFinal);
            }
        } catch (RuntimeException t) {
            return;
        }
    }
}
```

</details>

---
## [26] ⚙️ harness-attempt · `attempt_012`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException

---
## [27] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,703 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runCase("foo", 1, '-', true);

        int width = data.consumeInt(1, 64);
        char padChar = (char) (data.consumeByte() & 0xff);

        String prefix = data.consumeString(16);
        String core;
        if (data.consumeBoolean()) {
            int extra = data.consumeInt(0, 32);
            core = data.consumeString(width + extra + 1);
            if (core.length() < width) {
                String filler = data.consumeString(width - core.length());
                StringBuilder tmp = new StringBuilder(core);
                tmp.append(filler);
                while (tmp.length() < width) {
                    tmp.append('X');
                }
                core = tmp.toString();
            }
        } else {
            core = data.consumeString(width);
            if (core.length() < width) {
                StringBuilder tmp = new StringBuilder(core);
                while (tmp.length() < width) {
                    tmp.append('Y');
                }
                core = tmp.toString();
            }
        }

        runCase(prefix + core, width, padChar, false);
    }

    private static void runCase(String input, int width, char padChar, boolean isAnchor) {
        if (input == null || width <= 0 || input.length() < width) {
            return;
        }

        StrBuilder sb = new StrBuilder(1);
        String expectedSuffix = input.substring(0, width);

        try {
            sb.appendFixedWidthPadRight(input, width, padChar);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromPatchedMethod(t)) {
                throw t;
            }
            return;
        }

        String actual;
        try {
            actual = sb.toString();
        } catch (RuntimeException t) {
            return;
        }

        /* Contract asserted:
         * appendFixedWidthPadRight(Object,int,char) appends exactly width characters:
         * if obj.toString().length() >= width, it must append the leftmost width chars.
         * A throw-deleting or branch-skipping patch could avoid the crash yet append the wrong
         * number/content of chars. We therefore check the observable result directly.
         */
        if (!expectedSuffix.equals(actual)) {
            throw new RuntimeException("[oracle:fixed-width] metamorphic violation: appendFixedWidthPadRight must append exactly the first width chars when input is long enough input="
                    + printable(input) + " width=" + width + " pad=" + (int) padChar + " lhs=" + printable(actual) + " rhs=" + printable(expectedSuffix));
        }

        /* Shared-state agreement asserted:
         * toCharArray() "Copies the builder's character array into a new character array" and
         * minimizeCapacity() "Minimizes the capacity to the actual length of the string".
         * Both observe the same buffer/size state as appendFixedWidthPadRight. A patch that
         * corrupts size/buffer bookkeeping can leave toString() looking plausible while these
         * readers disagree. On any correct implementation, contents must remain identical.
         */
        try {
            char[] arr = sb.toCharArray();
            String viaArray = new String(arr);
            if (!actual.equals(viaArray)) {
                throw new RuntimeException("[oracle:chararray] metamorphic violation: toString and toCharArray must agree input="
                        + printable(input) + " width=" + width + " pad=" + (int) padChar + " lhs=" + printable(actual) + " rhs=" + printable(viaArray));
            }

            sb.minimizeCapacity();
            String afterMin = sb.toString();
            if (!actual.equals(afterMin)) {
                throw new RuntimeException("[oracle:mincap] metamorphic violation: minimizeCapacity must not change contents input="
                        + printable(input) + " width=" + width + " pad=" + (int) padChar + " lhs=" + printable(actual) + " rhs=" + printable(afterMin));
            }

            sb.setLength(width);
            String afterSetLength = sb.toString();
            if (!actual.equals(afterSetLength)) {
                throw new RuntimeException("[oracle:setlength] metamorphic violation: setLength(currentLength) must preserve contents input="
                        + printable(input) + " width=" + width + " pad=" + (int) padChar + " lhs=" + printable(actual) + " rhs=" + printable(afterSetLength));
            }
        } catch (RuntimeException t) {
            if (isRootCauseFromPatchedMethod(t) && isAnchor) {
                throw t;
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCauseFromPatchedMethod(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.lang.text.StrBuilder".equals(e.getClassName())
                    && "appendFixedWidthPadRight".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static String printable(String s) {
        return String.valueOf(s);
    }
}
```

</details>

---
## [28] ⚙️ harness-attempt · `attempt_013`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException

---
## [29] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,290 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        StrBuilder sb = new StrBuilder(1);
        try {
            sb.appendFixedWidthPadRight("foo", 1, '-');
            // Contract asserted:
            // - appendFixedWidthPadRight(Object, int, char) appends exactly "width" characters.
            //   When the string form is longer than width, the correct observable result is the leftmost
            //   width characters only; the failing test documents "foo", width 1 => "f".
            // - toCharArray() "represents the contents of the builder", so it must agree with toString().
            String actual = sb.toString();
            if (!"f".equals(actual)) {
                throw new RuntimeException("[oracle:anchor-prefix] metamorphic violation: exact failing-test postcondition input=foo width=1 lhs=" + actual + " rhs=f");
            }
            String charsView = new String(sb.toCharArray());
            if (!actual.equals(charsView)) {
                throw new RuntimeException("[oracle:anchor-state] metamorphic violation: toString/toCharArray disagreement input=foo width=1 lhs=" + actual + " rhs=" + charsView);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromPatchedMethod(t)) {
                throw t;
            }
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        String prefix = data.consumeString(16);
        String obj = data.consumeString(32);
        if (obj == null) {
            obj = "";
        }

        // Valid-by-construction: width > 0 and width <= obj.length(), so a correct implementation
        // is obligated to accept the call and truncate to exactly width characters.
        if (obj.length() == 0) {
            obj = "A";
        }
        int width = data.consumeInt(1, obj.length());
        char padChar = (char) (data.consumeByte() & 0xff);

        try {
            StrBuilder sb = new StrBuilder(Math.max(1, prefix.length() + 1));
            sb.append(prefix);
            int originalLen = sb.length();

            sb.appendFixedWidthPadRight(obj, width, padChar);

            String expected = prefix + obj.substring(0, width);
            String actual = sb.toString();

            // Contract asserted:
            // - appendFixedWidthPadRight appends exactly width chars.
            //   In the strLen >= width branch, the appended segment must be obj.toString().substring(0, width).
            //   A throw-deleting or branch-skipping patch would violate this observable result.
            if (!expected.equals(actual)) {
                throw new RuntimeException("[oracle:prefix-trunc] metamorphic violation: appendFixedWidthPadRight must append leftmost width chars inputPrefix=" + quote(prefix) + " inputObj=" + quote(obj) + " width=" + width + " lhs=" + actual + " rhs=" + expected);
            }

            // Shared-state agreement check required by the prompt:
            // toCharArray() "represents the contents of the builder", so it must agree with what append(...)
            // and appendFixedWidthPadRight established in buffer/size.
            String charsView = new String(sb.toCharArray());
            if (!actual.equals(charsView)) {
                throw new RuntimeException("[oracle:state-agree] metamorphic violation: toString/toCharArray disagreement inputPrefix=" + quote(prefix) + " inputObj=" + quote(obj) + " width=" + width + " lhs=" + actual + " rhs=" + charsView);
            }

            // Another shared-state observer over the same buffer/size:
            // minimizeCapacity() "minimizes the capacity to the actual length of the string" and must not
            // change logical contents.
            sb.minimizeCapacity();
            String minimized = sb.toString();
            if (!actual.equals(minimized)) {
                throw new RuntimeException("[oracle:mincap-content] metamorphic violation: minimizeCapacity changed logical contents inputPrefix=" + quote(prefix) + " inputObj=" + quote(obj) + " width=" + width + " lhs=" + actual + " rhs=" + minimized);
            }

            // setLength(int) updates size/buffer. Setting it to the existing length must preserve content.
            sb.setLength(originalLen + width);
            String sameLen = sb.toString();
            if (!actual.equals(sameLen)) {
                throw new RuntimeException("[oracle:setlen-id] metamorphic violation: setLength(currentLength) changed logical contents inputPrefix=" + quote(prefix) + " inputObj=" + quote(obj) + " width=" + width + " lhs=" + actual + " rhs=" + sameLen);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromPatchedMethod(t)) {
                throw t;
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            String lower = name.toLowerCase();
            if (lower.contains("invalid") || lower.contains("validation")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCauseFromPatchedMethod(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                    && "appendFixedWidthPadRight".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static String quote(String s) {
        return s == null ? "null" : s.replace("\0", "\\0");
    }
}
```

</details>

---
## [30] ⚙️ harness-attempt · `attempt_014`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException

---
## [31] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,680 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        StrBuilder sb = new StrBuilder(1);
        try {
            sb.appendFixedWidthPadRight("foo", 1, '-');
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            return;
        }

        String actual = sb.toString();
        if (!"f".equals(actual)) {
            throw new RuntimeException("[oracle:lang299-anchor] metamorphic violation: documented fixed-width right pad/truncate should keep only the first width chars when input is longer input=foo width=1 lhs=" + actual + " rhs=f");
        }

        sb.minimizeCapacity();
        String viaChars = new String(sb.toCharArray());
        if (!actual.equals(viaChars)) {
            throw new RuntimeException("[oracle:state-agree-anchor] metamorphic violation: toCharArray must represent the contents of the builder after appendFixedWidthPadRight/minimizeCapacity input=foo width=1 lhs=" + viaChars + " rhs=" + actual);
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        String prefix = data.consumeAsciiString(16);
        int width = data.consumeInt(1, 16);
        char padChar = (char) data.consumeInt(32, 126);
        String core = data.consumeAsciiString(32);

        if (core.length() < width) {
            StringBuilder tmp = new StringBuilder(core);
            while (tmp.length() < width) {
                tmp.append('X');
            }
            core = tmp.toString();
        }

        exerciseValidCase(prefix, core, width, padChar);

        String prefix2 = data.consumeAsciiString(16);
        int width2 = data.consumeInt(1, 16);
        char padChar2 = (char) data.consumeInt(32, 126);
        String core2 = data.consumeRemainingAsString();
        if (core2 == null) {
            core2 = "";
        }
        if (core2.length() < width2) {
            StringBuilder tmp2 = new StringBuilder(core2);
            while (tmp2.length() < width2) {
                tmp2.append('Y');
            }
            core2 = tmp2.toString();
        }

        exerciseValidCase(prefix2, core2, width2, padChar2);
    }

    private static void exerciseValidCase(String prefix, String str, int width, char padChar) {
        StrBuilder sb = new StrBuilder(Math.max(1, prefix.length()));
        sb.append(prefix);
        String expected = prefix + expectedFixedWidthPadRight(str, width, padChar);

        try {
            sb.appendFixedWidthPadRight(str, width, padChar);
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        String actual = sb.toString();

        /* Contract asserted:
         * appendFixedWidthPadRight appends exactly 'width' characters: if the string is longer,
         * it appends the leftmost width characters; otherwise it appends the string then pad chars.
         * Also, toCharArray() "represents the contents of the builder" and minimizeCapacity()
         * must not change those contents. A throw-deleting or silent-skip patch would break this.
         */
        if (!expected.equals(actual)) {
            throw new RuntimeException("[oracle:appendFixedWidthPadRight-post] metamorphic violation: result must equal prefix plus fixed-width truncated/padded text input=" + str + " width=" + width + " pad=" + (int) padChar + " lhs=" + actual + " rhs=" + expected);
        }

        sb.minimizeCapacity();
        String viaChars = new String(sb.toCharArray());
        if (!actual.equals(viaChars)) {
            throw new RuntimeException("[oracle:state-agree] metamorphic violation: toCharArray must agree with builder contents after appendFixedWidthPadRight/minimizeCapacity input=" + str + " width=" + width + " lhs=" + viaChars + " rhs=" + actual);
        }
    }

    private static String expectedFixedWidthPadRight(String str, int width, char padChar) {
        if (width <= 0) {
            return "";
        }
        if (str.length() >= width) {
            return str.substring(0, width);
        }
        StringBuilder out = new StringBuilder(width);
        out.append(str);
        while (out.length() < width) {
            out.append(padChar);
        }
        return out.toString();
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement ste = trace[i];
            if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                    && "appendFixedWidthPadRight".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }
}
```

</details>

---
## [32] ⚙️ harness-attempt · `attempt_015`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException

---
## [33] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,085 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runCase("foo", 1, '-', true);

        String prefix = data.consumeAsciiString(16);
        String core = data.consumeString(32);
        if (core == null) {
            core = "";
        }
        if (core.length() == 0) {
            core = "A";
        }
        String suffix = data.consumeAsciiString(16);
        String full = prefix + core + suffix;

        int choose = data.consumeInt(0, 3);
        int start = 0;
        int len = full.length();
        if (choose == 1) {
            start = prefix.length();
            len = core.length();
        } else if (choose == 2) {
            start = 0;
            len = prefix.length() + core.length();
        } else if (choose == 3) {
            start = prefix.length();
            len = core.length() + suffix.length();
        }

        if (start < 0) {
            start = 0;
        }
        if (start > full.length()) {
            start = full.length();
        }
        if (len < 0) {
            len = 0;
        }
        if (start + len > full.length()) {
            len = full.length() - start;
        }

        String selected = full.substring(start, start + len);
        int width = data.consumeInt(1, selected.length() == 0 ? 1 : selected.length());
        char pad = (char) (data.consumeByte() & 0xff);

        runCase(selected, width, pad, true);

        String extra = data.consumeAsciiString(16);
        String selected2 = selected + extra;
        if (selected2.length() > 0) {
            int width2 = data.consumeInt(1, selected2.length());
            runCase(selected2, width2, pad, true);
        }
    }

    private static void runCase(String s, int width, char pad, boolean validByConstruction) {
        try {
            StrBuilder anchor = new StrBuilder(1);
            anchor.appendFixedWidthPadRight(s, width, pad);

            String expected = s.substring(0, width);
            String actual = anchor.toString();
            if (!expected.equals(actual)) {
                throw new RuntimeException("[oracle:fwpr-trunc] metamorphic violation: appendFixedWidthPadRight must append exactly the leftmost width characters when input length >= width input=" + safe(s) + " width=" + width + " lhs=" + safe(actual) + " rhs=" + safe(expected));
            }

            /* Documented guarantee used for this oracle:
             * appendFixedWidthPadRight writes exactly width characters and updates shared state (buffer,size).
             * toCharArray() copies the builder contents represented by that same state.
             * A throw-deleting or bookkeeping-breaking patch could make toString()/reported contents diverge from the copied character array.
             */
            char[] charsBefore = anchor.toCharArray();
            String fromCharsBefore = new String(charsBefore);
            if (!actual.equals(fromCharsBefore)) {
                throw new RuntimeException("[oracle:state-agree] metamorphic violation: toCharArray must agree with toString after appendFixedWidthPadRight input=" + safe(s) + " width=" + width + " lhs=" + safe(actual) + " rhs=" + safe(fromCharsBefore));
            }

            anchor.minimizeCapacity();
            char[] charsAfter = anchor.toCharArray();
            String fromCharsAfter = new String(charsAfter);
            if (!actual.equals(fromCharsAfter)) {
                throw new RuntimeException("[oracle:mincap-agree] metamorphic violation: minimizeCapacity must not change visible contents input=" + safe(s) + " width=" + width + " lhs=" + safe(actual) + " rhs=" + safe(fromCharsAfter));
            }

            StrBuilder composed = new StrBuilder(1);
            composed.append("");
            composed.append(s, 0, s.length());
            composed.setLength(0);
            composed.appendFixedWidthPadRight(s, width, pad);
            String composedActual = composed.toString();
            if (!actual.equals(composedActual)) {
                throw new RuntimeException("[oracle:append-family] metamorphic violation: prior shared-state operations must not change appendFixedWidthPadRight result input=" + safe(s) + " width=" + width + " lhs=" + safe(actual) + " rhs=" + safe(composedActual));
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (validByConstruction && isRootCause(t)) {
                throw t;
            }
        } catch (Error t) {
            if (validByConstruction && isRootCause(t)) {
                throw t;
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String n = cur.getClass().getName();
            if (n != null) {
                String lower = n.toLowerCase();
                if (lower.contains("validation") || lower.contains("invalid")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        boolean classMatch = t instanceof ArrayIndexOutOfBoundsException;
        if (!classMatch) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.lang.text.StrBuilder".equals(e.getClassName())
                    && "appendFixedWidthPadRight".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static String safe(String s) {
        return String.valueOf(s);
    }
}
```

</details>

---
## [34] ⚙️ harness-attempt · `attempt_016`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException

---
## [35] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,069 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            StrBuilder anchor = new StrBuilder(1);
            anchor.appendFixedWidthPadRight("foo", 1, '-');
            if (!"f".equals(anchor.toString())) {
                throw new RuntimeException("[oracle:anchor-contract] metamorphic violation: exact regression input must produce truncated width output input=foo width=1 lhs=" + anchor.toString() + " rhs=f");
            }
            /* Contract/state check:
             * appendFixedWidthPadRight updates the builder contents/size; toCharArray() copies the contents
             * represented by the same shared state (buffer,size). For every correct implementation,
             * new String(toCharArray()) must equal toString(). A throw-deleting or bookkeeping-breaking patch
             * can leave these observers disagreeing.
             */
            String anchorChars = new String(anchor.toCharArray());
            if (!anchor.toString().equals(anchorChars)) {
                throw new RuntimeException("[oracle:anchor-state] metamorphic violation: toString and toCharArray must agree input=foo width=1 lhs=" + anchor.toString() + " rhs=" + anchorChars);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }

        String prefix = data.consumeAsciiString(16);
        String s = data.consumeString(32);
        if (s == null) {
            s = "";
        }

        int width;
        if (s.length() == 0) {
            width = 0;
        } else {
            width = data.consumeInt(1, s.length());
        }
        char padChar = (char) (data.consumeByte() & 0xff);

        StrBuilder sb = new StrBuilder(Math.max(1, prefix.length() + width));
        sb.append(prefix);

        try {
            sb.appendFixedWidthPadRight(s, width, padChar);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t) && width > 0 && s.length() >= width) {
                throw t;
            }
            return;
        }

        /* Metamorphic relation from the documented append contracts:
         * appendFixedWidthPadRight(obj,width,padChar) with a non-null string whose length >= width
         * must append exactly width characters from the string and no padding.
         * append(str,0,width) appends exactly that same substring.
         * Therefore, starting from the same prefix, both real-library calls must yield equal builders.
         */
        try {
            StrBuilder expected = new StrBuilder(Math.max(1, prefix.length() + width));
            expected.append(prefix);
            expected.append(s, 0, width);

            String lhs = sb.toString();
            String rhs = expected.toString();
            if (!lhs.equals(rhs)) {
                throw new RuntimeException("[oracle:truncate-equiv] metamorphic violation: appendFixedWidthPadRight must equal append(str,0,width) when str.length>=width input=" + s + " width=" + width + " prefix=" + prefix + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            throw t;
        }

        /* Shared-state observer agreement:
         * toCharArray() and toString() both report the builder contents represented by (buffer,size),
         * so they must agree after a successful append.
         */
        try {
            String charView = new String(sb.toCharArray());
            String stringView = sb.toString();
            if (!stringView.equals(charView)) {
                throw new RuntimeException("[oracle:state-agree] metamorphic violation: toString and toCharArray must agree input=" + s + " width=" + width + " prefix=" + prefix + " lhs=" + stringView + " rhs=" + charView);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            throw t;
        }

        if (data.remainingBytes() > 0) {
            int extra = data.consumeInt(0, 8);
            try {
                StrBuilder sb2 = new StrBuilder();
                sb2.append(prefix);
                sb2.appendFixedWidthPadRight(s, width, padChar);
                sb2.setLength(sb2.length() + extra);
                sb2.minimizeCapacity();
                String a = sb2.toString();
                String b = new String(sb2.toCharArray());
                if (!a.equals(b)) {
                    throw new RuntimeException("[oracle:post-size-state] metamorphic violation: observers must still agree after setLength/minimizeCapacity input=" + s + " width=" + width + " prefix=" + prefix + " extra=" + extra + " lhs=" + a + " rhs=" + b);
                }
            } catch (RuntimeException t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t) && width > 0 && s.length() >= width) {
                    throw t;
                }
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.lang.text.StrBuilder".equals(e.getClassName())
                    && "appendFixedWidthPadRight".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}
```

</details>

---
## [36] ⚙️ harness-attempt · `attempt_017`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException

---
## [37] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,675 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exerciseCase("", "foo", 1, '-', 1, true);

        int cases = 1;
        if (data.remainingBytes() > 0) {
            cases = data.consumeInt(1, 4);
        }

        for (int i = 0; i < cases; i++) {
            int width = 1;
            if (data.remainingBytes() > 0) {
                width = data.consumeInt(1, 32);
            }

            String prefix = data.consumeAsciiString(16);
            String obj = buildStringAtLeast(data, width);
            char padChar = (char) ('!' + (data.consumeByte() & 0x3f));
            int capacity = data.remainingBytes() > 0 ? data.consumeInt(1, 32) : 1;
            boolean doMinimize = data.consumeBoolean();

            exerciseCase(prefix, obj, width, padChar, capacity, doMinimize);
        }
    }

    private static void exerciseCase(String prefix, String obj, int width, char padChar, int capacity, boolean doMinimize) {
        if (obj == null || width <= 0 || obj.length() < width) {
            return;
        }

        StrBuilder sb = new StrBuilder(Math.max(1, capacity));
        try {
            sb.append(prefix);
            sb.appendFixedWidthPadRight(obj, width, padChar);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        String expected = prefix + obj.substring(0, width);

        /* Contract asserted:
         * appendFixedWidthPadRight(Object,int,char) appends a fixed-width field; when obj.toString().length() >= width,
         * the builder must contain exactly the first width characters of that string and size increases by width.
         * A throw-deleting or branch-skipping patch would violate this observable content/length guarantee.
         */
        String actual;
        try {
            actual = sb.toString();
        } catch (RuntimeException t) {
            return;
        }
        if (!expected.equals(actual)) {
            throw new RuntimeException("[oracle:fixedwidth] metamorphic violation: appended content mismatch input="
                    + debug(prefix, obj, width, padChar) + " lhs=" + actual + " rhs=" + expected);
        }
        if (sb.length() != expected.length()) {
            throw new RuntimeException("[oracle:fixedwidth] metamorphic violation: length mismatch input="
                    + debug(prefix, obj, width, padChar) + " lhs=" + sb.length() + " rhs=" + expected.length());
        }

        /* Shared-state agreement asserted:
         * toCharArray() copies the builder contents represented by the shared buffer/size fields.
         * Therefore toCharArray(), length(), and toString() must agree after appendFixedWidthPadRight.
         */
        try {
            char[] chars = sb.toCharArray();
            String fromChars = new String(chars);
            if (chars.length != sb.length() || !fromChars.equals(actual)) {
                throw new RuntimeException("[oracle:state] metamorphic violation: toCharArray/length/toString disagree input="
                        + debug(prefix, obj, width, padChar) + " lhs=" + fromChars + "/" + chars.length + " rhs="
                        + actual + "/" + sb.length());
            }
        } catch (RuntimeException t) {
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        }

        /* Contract asserted:
         * minimizeCapacity() "Minimizes the capacity to the actual length of the string" and must not change contents.
         */
        if (doMinimize) {
            try {
                String before = sb.toString();
                int beforeLen = sb.length();
                sb.minimizeCapacity();
                String after = sb.toString();
                int afterLen = sb.length();
                char[] afterChars = sb.toCharArray();
                String afterFromChars = new String(afterChars);
                if (!before.equals(after) || beforeLen != afterLen || !after.equals(afterFromChars) || afterChars.length != afterLen) {
                    throw new RuntimeException("[oracle:mincap] metamorphic violation: minimizeCapacity changed observable contents input="
                            + debug(prefix, obj, width, padChar) + " lhs=" + after + "/" + afterLen + " rhs="
                            + before + "/" + beforeLen);
                }
            } catch (RuntimeException t) {
                if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                    throw t;
                }
            }
        }
    }

    private static String buildStringAtLeast(FuzzedDataProvider data, int minLen) {
        String s = data.consumeString(Math.max(minLen + 8, 8));
        if (s == null) {
            s = "";
        }
        if (s.length() >= minLen) {
            return s;
        }
        StringBuilder b = new StringBuilder(s);
        char fill = visibleFill(data);
        while (b.length() < minLen) {
            b.append(fill);
        }
        return b.toString();
    }

    private static char visibleFill(FuzzedDataProvider data) {
        return (char) ('A' + (data.consumeByte() & 15));
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement ste = trace[i];
            if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                    && "appendFixedWidthPadRight".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static String debug(String prefix, String obj, int width, char padChar) {
        return "{prefix=" + quote(prefix) + ",obj=" + quote(obj) + ",width=" + width + ",padChar=" + (int) padChar + "}";
    }

    private static String quote(String s) {
        if (s == null) {
            return "null";
        }
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
```

</details>

---
## [38] ⚙️ harness-attempt · `attempt_018`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException

---
## [39] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,871 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        StrBuilder sb = new StrBuilder(1);
        try {
            sb.appendFixedWidthPadRight("foo", 1, '-');
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromTarget(t)) {
                throw t;
            }
            return;
        }

        String actual = sb.toString();
        if (!"f".equals(actual)) {
            throw new RuntimeException("[oracle:anchor] metamorphic violation: exact regression test result input=foo,width=1,pad=- lhs=" + actual + " rhs=f");
        }

        // Contract asserted:
        // appendFixedWidthPadRight(Object,int,char) appends exactly `width` characters:
        // if obj.toString().length() >= width, the appended segment is the leftmost `width` chars.
        // Also, toCharArray() copies the builder contents represented by shared state (buffer,size).
        // A throw-deleting or bookkeeping-breaking patch could make toString()/size look plausible while
        // leaving shared state inconsistent; the copied char[] must still equal the textual contents.
        char[] arr = sb.toCharArray();
        String fromArray = new String(arr);
        if (!actual.equals(fromArray)) {
            throw new RuntimeException("[oracle:state-anchor] metamorphic violation: toCharArray must agree with builder contents input=foo,width=1,pad=- lhs=" + fromArray + " rhs=" + actual);
        }

        StrBuilder sibling = new StrBuilder(1);
        try {
            sibling.append("foo", 0, 1);
        } catch (RuntimeException t) {
            return;
        }
        String siblingResult = sibling.toString();
        if (!actual.equals(siblingResult)) {
            throw new RuntimeException("[oracle:sibling-anchor] metamorphic violation: fixed-width right append without padding must equal append(str,0,width) input=foo,width=1 lhs=" + actual + " rhs=" + siblingResult);
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        String prefix = data.consumeString(16);
        String core = data.consumeString(32);
        if (core == null) {
            return;
        }

        int extra = data.consumeInt(0, 16);
        while (core.length() == 0) {
            core = data.consumeString(32);
            if (core == null) {
                return;
            }
            if (core.length() > 0) {
                break;
            }
            if (data.remainingBytes() == 0) {
                core = "A";
                break;
            }
        }

        String suffix = data.consumeString(16);
        String obj = core + suffix;
        if (obj.length() == 0) {
            obj = "A";
        }

        int width = data.consumeInt(1, obj.length());
        char padChar = (char) (data.consumeByte() & 0xff);

        StrBuilder sb = new StrBuilder(Math.max(1, prefix.length() + width));
        String before;
        try {
            sb.append(prefix);
            before = sb.toString();
        } catch (RuntimeException t) {
            return;
        }

        boolean validByConstruction = width > 0 && obj != null && obj.length() >= width;

        try {
            sb.appendFixedWidthPadRight(obj, width, padChar);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (validByConstruction && isRootCauseFromTarget(t)) {
                throw t;
            }
            return;
        }

        String actual;
        try {
            actual = sb.toString();
        } catch (RuntimeException t) {
            return;
        }

        String expectedSegment = obj.substring(0, width);
        String expected = before + expectedSegment;
        if (!expected.equals(actual)) {
            throw new RuntimeException("[oracle:content] metamorphic violation: appendFixedWidthPadRight must append leftmost width chars when source length>=width inputPrefix=" + before + " obj=" + obj + " width=" + width + " lhs=" + actual + " rhs=" + expected);
        }

        // Contract asserted:
        // toCharArray() "Copies the builder's character array into a new character array" and thus must
        // agree with the contents established by append()/appendFixedWidthPadRight() through shared state.
        try {
            String fromArray = new String(sb.toCharArray());
            if (!actual.equals(fromArray)) {
                throw new RuntimeException("[oracle:state] metamorphic violation: toCharArray must agree with toString after appendFixedWidthPadRight inputPrefix=" + before + " obj=" + obj + " width=" + width + " lhs=" + fromArray + " rhs=" + actual);
            }
        } catch (RuntimeException t) {
            return;
        }

        // Contract asserted:
        // when obj.toString().length() >= width, appendFixedWidthPadRight(obj,width,pad)
        // should behave like appending exactly the substring [0,width); padChar is irrelevant.
        // This is checked via a sibling real-library call, not a hand-rolled oracle.
        try {
            StrBuilder sibling = new StrBuilder(Math.max(1, prefix.length() + width));
            sibling.append(prefix);
            sibling.append(obj, 0, width);
            String siblingResult = sibling.toString();
            if (!actual.equals(siblingResult)) {
                throw new RuntimeException("[oracle:sibling] metamorphic violation: fixed-width right append without padding must equal append(str,0,width) inputPrefix=" + before + " obj=" + obj + " width=" + width + " lhs=" + actual + " rhs=" + siblingResult);
            }
        } catch (RuntimeException t) {
            return;
        }

        try {
            sb.minimizeCapacity();
            String afterMin = sb.toString();
            if (!actual.equals(afterMin)) {
                throw new RuntimeException("[oracle:mincap] metamorphic violation: minimizeCapacity must not change contents inputPrefix=" + before + " obj=" + obj + " width=" + width + " lhs=" + afterMin + " rhs=" + actual);
            }
            sb.setLength(actual.length());
            String afterSet = sb.toString();
            if (!actual.equals(afterSet)) {
                throw new RuntimeException("[oracle:setlen] metamorphic violation: setLength(currentLength) must preserve contents inputPrefix=" + before + " obj=" + obj + " width=" + width + " lhs=" + afterSet + " rhs=" + actual);
            }
        } catch (RuntimeException t) {
            return;
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
        }
        String name = t.getClass().getName().toLowerCase();
        return name.contains("invalid") || name.contains("validation");
    }

    private static boolean isRootCauseFromTarget(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.lang.text.StrBuilder".equals(e.getClassName())
                    && "appendFixedWidthPadRight".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}
```

</details>

---
## [40] ⚙️ harness-attempt · `attempt_019`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException

---
## [41] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,129 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        try {
            StrBuilder sb = new StrBuilder(1);
            sb.appendFixedWidthPadRight("foo", 1, '-');

            String out = sb.toString();
            if (!"f".equals(out)) {
                throw new RuntimeException("[oracle:anchor-contract] metamorphic violation: appendFixedWidthPadRight must truncate to fixed width for longer input input=foo width=1 lhs=" + out + " rhs=f");
            }

            // Contract/invariant checked via shared state:
            // appendFixedWidthPadRight writes buffer/size; toCharArray() reports the contents from the same state.
            // A throw-deleting or bookkeeping-buggy patch could leave size/buffer inconsistent even if toString() looks plausible.
            char[] arr = sb.toCharArray();
            String arrString = new String(arr);
            if (!out.equals(arrString) || arr.length != 1) {
                throw new RuntimeException("[oracle:anchor-state] metamorphic violation: toString/toCharArray must agree after fixed-width append input=foo width=1 lhs=" + out + " rhs=" + arrString + " arrLen=" + arr.length);
            }

            sb.minimizeCapacity();
            char[] arr2 = sb.toCharArray();
            String arr2String = new String(arr2);
            if (!out.equals(arr2String) || arr2.length != 1) {
                throw new RuntimeException("[oracle:anchor-mincap] metamorphic violation: minimizeCapacity must preserve contents/size input=foo width=1 lhs=" + out + " rhs=" + arr2String + " arrLen=" + arr2.length);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        int iterations = 1 + data.consumeInt(0, 3);
        for (int i = 0; i < iterations; i++) {
            String prefix = data.consumeAsciiString(8);
            String suffix = data.consumeAsciiString(8);
            int width = data.consumeInt(1, 32);
            int extra = data.consumeInt(0, 16);
            char pad = (char) (' ' + (data.consumeInt(0, 94)));
            String raw = data.consumeString(width + extra);
            String obj = ensureLengthAtLeast(raw, width);

            runOneValidCase(prefix, obj, width, pad, suffix);

            if (data.remainingBytes() <= 0) {
                break;
            }
        }
    }

    private static void runOneValidCase(String prefix, String obj, int width, char pad, String suffix) {
        try {
            // Valid by construction: width > 0 and obj.length() >= width.
            // For such inputs, a correct implementation is obligated to accept and append exactly the first width chars.
            StrBuilder sb = new StrBuilder(1);
            sb.append(prefix);
            int before = sb.length();
            sb.appendFixedWidthPadRight(obj, width, pad);
            sb.append(suffix);

            String actual = sb.toString();
            String expectedMiddle = obj.substring(0, width);
            String expected = prefix + expectedMiddle + suffix;

            // Documented guarantee of fixed-width right padding: longer input is truncated on the right to the fixed width.
            // A patch that merely suppresses the crash or skips copying would violate this observable result.
            if (!expected.equals(actual)) {
                throw new RuntimeException("[oracle:content] metamorphic violation: fixed-width append must truncate longer input to width input=" + obj + " width=" + width + " lhs=" + actual + " rhs=" + expected);
            }

            // Shared-state agreement check: appendFixedWidthPadRight updates buffer/size; toCharArray reports from the same state.
            char[] chars = sb.toCharArray();
            String fromChars = new String(chars);
            if (!actual.equals(fromChars) || chars.length != expected.length()) {
                throw new RuntimeException("[oracle:state-agree] metamorphic violation: toString and toCharArray must agree after appendFixedWidthPadRight input=" + obj + " width=" + width + " lhs=" + actual + " rhs=" + fromChars + " arrLen=" + chars.length);
            }

            // Another shared-state check using setLength on a valid, non-negative length established from current contents.
            // setLength truncates/drops trailing characters; shrinking to 'before + width' should remove the suffix and preserve the fixed-width segment.
            StrBuilder shrunk = new StrBuilder();
            shrunk.append(actual);
            shrunk.setLength(before + width);
            String shrunkOut = shrunk.toString();
            String expectedShrunk = prefix + expectedMiddle;
            if (!expectedShrunk.equals(shrunkOut)) {
                throw new RuntimeException("[oracle:setlength] metamorphic violation: setLength must expose the same fixed-width content established by appendFixedWidthPadRight input=" + obj + " width=" + width + " lhs=" + shrunkOut + " rhs=" + expectedShrunk);
            }

            shrunk.minimizeCapacity();
            String minOut = shrunk.toString();
            if (!expectedShrunk.equals(minOut)) {
                throw new RuntimeException("[oracle:mincap] metamorphic violation: minimizeCapacity must preserve contents after fixed-width append input=" + obj + " width=" + width + " lhs=" + minOut + " rhs=" + expectedShrunk);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.lang.text.StrBuilder".equals(e.getClassName())
                    && "appendFixedWidthPadRight".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
                || t instanceof NumberFormatException;
    }

    private static String ensureLengthAtLeast(String s, int minLen) {
        if (s == null) {
            s = "";
        }
        if (s.length() >= minLen) {
            return s;
        }
        StringBuffer sb = new StringBuffer(s);
        while (sb.length() < minLen) {
            sb.append('X');
        }
        return sb.toString();
    }
}
```

</details>

---
## [42] ⚙️ harness-attempt · `attempt_020`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException

---
## [43] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,101 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        {
            StrBuilder sb = new StrBuilder(1);
            try {
                sb.appendFixedWidthPadRight("foo", 1, '-');
                String actual = sb.toString();
                if (!"f".equals(actual)) {
                    throw new RuntimeException("[oracle:anchor-post] metamorphic violation: exact regression input must append only the first width characters input=foo width=1 lhs=" + actual + " rhs=f");
                }
                /* Contract asserted:
                 * - appendFixedWidthPadRight(obj,width,padChar) appends exactly width characters; when obj.toString().length() >= width,
                 *   the result is the leftmost width characters of the string.
                 * - toCharArray() "copies the builder's character array into a new character array".
                 * - minimizeCapacity() "Minimizes the capacity to the actual length of the string", so content must remain unchanged.
                 * A throw-deleting or bookkeeping-breaking patch could avoid the crash but leave stale/shared-state disagreement here.
                 */
                char[] chars = sb.toCharArray();
                String fromChars = new String(chars);
                if (!actual.equals(fromChars)) {
                    throw new RuntimeException("[oracle:anchor-state] metamorphic violation: toCharArray must agree with toString input=foo width=1 lhs=" + actual + " rhs=" + fromChars);
                }
                sb.minimizeCapacity();
                String afterMin = sb.toString();
                if (!actual.equals(afterMin)) {
                    throw new RuntimeException("[oracle:anchor-mincap] metamorphic violation: minimizeCapacity must not change contents input=foo width=1 lhs=" + actual + " rhs=" + afterMin);
                }
            } catch (RuntimeException t) {
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    return;
                }
                boolean throughTarget = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                            && "appendFixedWidthPadRight".equals(ste.getMethodName())) {
                        throughTarget = true;
                        break;
                    }
                }
                if (throughTarget && t instanceof ArrayIndexOutOfBoundsException) {
                    throw t;
                }
            }
        }

        String prefix = data.consumeString(16);
        String s = data.consumeString(32);
        int width = data.consumeInt(1, 32);
        char padChar = (char) (data.consumeByte() & 0xff);
        int initialCapacity = data.consumeInt(1, 8);

        if (s.length() < width) {
            StringBuilder fill = new StringBuilder(s);
            while (fill.length() < width) {
                String extra = data.consumeString(8);
                if (extra.length() == 0) {
                    fill.append('X');
                } else {
                    fill.append(extra);
                }
            }
            s = fill.toString();
        }

        StrBuilder lhs = new StrBuilder(initialCapacity);
        StrBuilder rhs = new StrBuilder(initialCapacity);
        String lhsResult;
        String rhsResult;

        try {
            lhs.append(prefix);
            lhs.appendFixedWidthPadRight(s, width, padChar);
            lhsResult = lhs.toString();
        } catch (RuntimeException t) {
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            boolean throughTarget = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                        && "appendFixedWidthPadRight".equals(ste.getMethodName())) {
                    throughTarget = true;
                    break;
                }
            }
            if (throughTarget && t instanceof ArrayIndexOutOfBoundsException) {
                throw t;
            }
            return;
        }

        try {
            rhs.append(prefix);
            rhs.append(s, 0, width);
            rhsResult = rhs.toString();
        } catch (RuntimeException t) {
            return;
        }

        /* Contract asserted:
         * When obj.toString().length() >= width, appendFixedWidthPadRight(obj,width,padChar)
         * should append exactly the first width characters of obj; this must agree with append(str,0,width)
         * on the same builder prefix. This uses only real library calls on both sides.
         */
        if (!lhsResult.equals(rhsResult)) {
            throw new RuntimeException("[oracle:prefix-eq] metamorphic violation: appendFixedWidthPadRight must agree with append(str,0,width) when str length >= width input=" + s + " width=" + width + " lhs=" + lhsResult + " rhs=" + rhsResult);
        }

        try {
            char[] chars = lhs.toCharArray();
            String fromChars = new String(chars);
            if (!lhsResult.equals(fromChars)) {
                throw new RuntimeException("[oracle:state-chararray] metamorphic violation: toCharArray must agree with builder contents input=" + s + " width=" + width + " lhs=" + lhsResult + " rhs=" + fromChars);
            }
            lhs.minimizeCapacity();
            String afterMin = lhs.toString();
            if (!lhsResult.equals(afterMin)) {
                throw new RuntimeException("[oracle:state-mincap] metamorphic violation: minimizeCapacity must not change contents input=" + s + " width=" + width + " lhs=" + lhsResult + " rhs=" + afterMin);
            }
        } catch (RuntimeException t) {
            return;
        }
    }
}
```

</details>

---
## [44] ⚙️ harness-attempt · `attempt_021`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException

---
## [45] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,105 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        StrBuilder sb = new StrBuilder(1);
        try {
            sb.appendFixedWidthPadRight("foo", 1, '-');
        } catch (RuntimeException t) {
            if (isValidationLike(t)) {
                return;
            }
            if (isRootCause(t, true)) {
                throw t;
            }
            return;
        }

        try {
            String actual = sb.toString();
            if (!"f".equals(actual)) {
                throw new RuntimeException("[oracle:fixedwidth-anchor] metamorphic violation: documented truncation to width failed input=foo width=1 lhs=" + actual + " rhs=f");
            }

            /* Contract asserted:
             * appendFixedWidthPadRight appends exactly width characters; when the string is longer than width,
             * the result must be truncated to the leftmost width chars. Also, toCharArray() "represents the contents
             * of the builder", so it must agree with toString() over the same shared state (buffer,size).
             * A patch that merely deletes the failing copy or corrupts size/buffer bookkeeping can avoid the throw
             * yet still violate these observable guarantees.
             */
            char[] chars = sb.toCharArray();
            String fromChars = new String(chars);
            if (!actual.equals(fromChars)) {
                throw new RuntimeException("[oracle:state-agree-anchor] metamorphic violation: toString/toCharArray disagreement input=foo width=1 lhs=" + actual + " rhs=" + fromChars);
            }
        } catch (RuntimeException t) {
            if (isValidationLike(t)) {
                return;
            }
            throw t;
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        String prefix = data.consumeString(16);
        String core = data.consumeString(32);
        String suffix = data.consumeString(16);
        char pad = (char) (data.consumeByte() & 0xff);
        int width = data.consumeInt(1, 32);
        boolean shrinkThenMinimize = data.consumeBoolean();
        boolean useSliceAppend = data.consumeBoolean();

        if (core == null) {
            core = "";
        }
        if (core.length() < width) {
            core = core + repeat('X', width - core.length());
        }

        String appendedObject;
        if (useSliceAppend && core.length() > 0) {
            int start = data.consumeInt(0, core.length() - 1);
            int len = data.consumeInt(1, core.length() - start);
            StrBuilder source = new StrBuilder();
            try {
                source.append(core, start, len);
            } catch (RuntimeException t) {
                if (isValidationLike(t)) {
                    return;
                }
                return;
            }
            appendedObject = source.toString();
            if (appendedObject.length() < width) {
                appendedObject = appendedObject + repeat('Y', width - appendedObject.length());
            }
        } else {
            appendedObject = core;
        }

        StrBuilder sb = new StrBuilder(Math.max(1, data.consumeInt(1, 8)));
        try {
            sb.append(prefix);
            if (shrinkThenMinimize) {
                sb.setLength(sb.length());
                sb.minimizeCapacity();
            }
        } catch (RuntimeException t) {
            if (isValidationLike(t)) {
                return;
            }
            return;
        }

        String expected = prefix + appendedObject.substring(0, width);
        try {
            sb.appendFixedWidthPadRight(appendedObject, width, pad);
        } catch (RuntimeException t) {
            if (isValidationLike(t)) {
                return;
            }
            if (isRootCause(t, true)) {
                throw t;
            }
            return;
        }

        try {
            String actual = sb.toString();
            if (!expected.equals(actual)) {
                throw new RuntimeException("[oracle:fixedwidth-trunc] metamorphic violation: fixed-width append should append exactly first width chars input=" + appendedObject + " width=" + width + " lhs=" + actual + " rhs=" + expected);
            }

            /* Contract asserted:
             * - appendFixedWidthPadRight appends exactly width chars, truncating when input is longer.
             * - toCharArray() returns a new array that represents the contents of the builder.
             * Because these methods share buffer/size state, they must agree after the append.
             */
            char[] chars = sb.toCharArray();
            String fromChars = new String(chars);
            if (!actual.equals(fromChars)) {
                throw new RuntimeException("[oracle:state-agree-explore] metamorphic violation: toString/toCharArray disagreement input=" + appendedObject + " width=" + width + " lhs=" + actual + " rhs=" + fromChars);
            }

            StrBuilder sibling = new StrBuilder(Math.max(1, prefix.length() + width + suffix.length()));
            sibling.append(prefix);
            sibling.append(appendedObject.substring(0, width));
            String siblingExpected = sibling.toString();
            if (!actual.equals(siblingExpected)) {
                throw new RuntimeException("[oracle:sibling-append] metamorphic violation: appendFixedWidthPadRight(longer,width,pad) should match append(prefix substring) input=" + appendedObject + " width=" + width + " lhs=" + actual + " rhs=" + siblingExpected);
            }
        } catch (RuntimeException t) {
            if (isValidationLike(t)) {
                return;
            }
            throw t;
        }
    }

    private static boolean isRootCause(Throwable t, boolean validByConstruction) {
        if (!validByConstruction) {
            return false;
        }
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.lang.text.StrBuilder".equals(e.getClassName())
                    && "appendFixedWidthPadRight".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValidationLike(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static String repeat(char c, int count) {
        if (count <= 0) {
            return "";
        }
        char[] out = new char[count];
        for (int i = 0; i < count; i++) {
            out[i] = c;
        }
        return new String(out);
    }
}
```

</details>

---
## [46] ⚙️ harness-attempt · `attempt_022`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException

---
## [47] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,202 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        StrBuilder sb = new StrBuilder(1);
        try {
            sb.appendFixedWidthPadRight("foo", 1, '-');
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        // Contract asserted: when strLen >= width, appendFixedWidthPadRight appends exactly the
        // leftmost width characters and increases the logical content by width. A throw-deleting
        // or branch-skipping patch would violate the observable result and/or shared state.
        if (!"f".equals(sb.toString())) {
            throw new RuntimeException("[oracle:anchor-result] metamorphic violation: exact trigger should append first width chars input=foo width=1 lhs=" + sb.toString() + " rhs=f");
        }

        sb.minimizeCapacity();
        char[] arr = sb.toCharArray();
        String viaArray = new String(arr);
        if (!"f".equals(viaArray) || arr.length != 1) {
            throw new RuntimeException("[oracle:anchor-state] metamorphic violation: toCharArray/minimizeCapacity must agree with written content input=foo width=1 lhs=" + viaArray + "/" + arr.length + " rhs=f/1");
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        String prefix = data.consumeAsciiString(16);
        String core = data.consumeAsciiString(32);
        if (core.length() == 0) {
            core = "A";
        }
        int width = data.consumeInt(1, core.length());
        char padChar = (char) (data.consumeByte() & 0xff);
        boolean useNullTextPath = data.consumeBoolean();
        boolean doCapacityChanges = data.consumeBoolean();

        StrBuilder target = new StrBuilder(data.consumeInt(1, 32));
        try {
            if (prefix.length() > 0) {
                target.append(prefix);
            }
            if (doCapacityChanges) {
                target.setLength(target.length());
                target.minimizeCapacity();
            }

            if (useNullTextPath) {
                target.setNullText(core);
                target.appendFixedWidthPadRight(null, width, padChar);
            } else {
                target.appendFixedWidthPadRight(core, width, padChar);
            }
        } catch (IllegalArgumentException t) {
            return;
        } catch (NumberFormatException t) {
            return;
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        String effective = useNullTextPath ? core : core;
        String expectedSlice = effective.substring(0, width);
        String expectedWhole = prefix + expectedSlice;

        // Contract asserted from method body/docs: if strLen >= width, only the first width chars
        // are appended; when the call succeeds, shared-state readers must report the same logical
        // content that writers established.
        String actual = target.toString();
        if (!expectedWhole.equals(actual)) {
            throw new RuntimeException("[oracle:content] metamorphic violation: appendFixedWidthPadRight must append first width chars when input length >= width input=" + printable(effective) + " prefix=" + printable(prefix) + " width=" + width + " lhs=" + printable(actual) + " rhs=" + printable(expectedWhole));
        }

        char[] arr = target.toCharArray();
        String viaArray = new String(arr);
        if (!actual.equals(viaArray) || arr.length != actual.length()) {
            throw new RuntimeException("[oracle:shared-state] metamorphic violation: toCharArray must agree with toString after appendFixedWidthPadRight input=" + printable(effective) + " width=" + width + " lhs=" + printable(viaArray) + "/" + arr.length + " rhs=" + printable(actual) + "/" + actual.length());
        }

        // Equivalent-input relation using only real library calls:
        // appendFixedWidthPadRight(s, width, pad) with s.length() >= width must agree with
        // append(s, 0, width), since both append exactly that prefix.
        try {
            StrBuilder lhs = new StrBuilder();
            if (prefix.length() > 0) {
                lhs.append(prefix);
            }
            lhs.appendFixedWidthPadRight(effective, width, padChar);

            StrBuilder rhs = new StrBuilder();
            if (prefix.length() > 0) {
                rhs.append(prefix);
            }
            rhs.append(effective, 0, width);

            if (!lhs.toString().equals(rhs.toString())) {
                throw new RuntimeException("[oracle:equiv-overload] metamorphic violation: fixed-width truncation must equal append(str,0,width) input=" + printable(effective) + " prefix=" + printable(prefix) + " width=" + width + " lhs=" + printable(lhs.toString()) + " rhs=" + printable(rhs.toString()));
            }
        } catch (IllegalArgumentException t) {
            return;
        } catch (NumberFormatException t) {
            return;
        } catch (RuntimeException t) {
            if (isOracleViolation(t)) {
                throw t;
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.lang.text.StrBuilder".equals(e.getClassName())
                    && "appendFixedWidthPadRight".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOracleViolation(RuntimeException t) {
        String msg = t.getMessage();
        return msg != null && msg.startsWith("[oracle:");
    }

    private static String printable(String s) {
        return String.valueOf(s);
    }
}
```

</details>

---
## [48] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [49] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~7,731 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        StrBuilder sb = new StrBuilder(1);
        try {
            sb.appendFixedWidthPadRight("foo", 1, '-');
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        // Contract asserted: when strLen >= width, appendFixedWidthPadRight appends exactly the
        // leftmost width characters and increases the logical content by width. A throw-deleting
        // or branch-skipping patch would violate the observable result and/or shared state.
        if (!"f".equals(sb.toString())) {
            throw new RuntimeException("[oracle:anchor-result] metamorphic violation: exact trigger should append first width chars input=foo width=1 lhs=" + sb.toString() + " rhs=f");
        }

        sb.minimizeCapacity();
        char[] arr = sb.toCharArray();
        String viaArray = new String(arr);
        if (!"f".equals(viaArray) || arr.length != 1) {
            throw new RuntimeException("[oracle:anchor-state] metamorphic violation: toCharArray/minimizeCapacity must agree with written content input=foo width=1 lhs=" + viaArray + "/" + arr.length + " rhs=f/1");
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        String prefix = data.consumeAsciiString(16);
        String core = data.consumeAsciiString(32);
        if (core.length() == 0) {
            core = "A";
        }
        int width = data.consumeInt(1, core.length());
        char padChar = (char) (data.consumeByte() & 0xff);
        boolean useNullTextPath = data.consumeBoolean();
        boolean doCapacityChanges = data.consumeBoolean();

        StrBuilder target = new StrBuilder(data.consumeInt(1, 32));
        try {
            if (prefix.length() > 0) {
                target.append(prefix);
            }
            if (doCapacityChanges) {
                target.setLength(target.length());
                target.minimizeCapacity();
            }

            if (useNullTextPath) {
                target.setNullText(core);
                target.appendFixedWidthPadRight(null, width, padChar);
            } else {
                target.appendFixedWidthPadRight(core, width, padChar);
            }
        } catch (IllegalArgumentException t) {
            return;
        } catch (NumberFormatException t) {
            return;
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        String effective = useNullTextPath ? core : core;
        String expectedSlice = effective.substring(0, width);
        String expectedWhole = prefix + expectedSlice;

        // Contract asserted from method body/docs: if strLen >= width, only the first width chars
        // are appended; when the call succeeds, shared-state readers must report the same logical
        // content that writers established.
        String actual = target.toString();
        if (!expectedWhole.equals(actual)) {
            throw new RuntimeException("[oracle:content] metamorphic violation: appendFixedWidthPadRight must append first width chars when input length >= width input=" + printable(effective) + " prefix=" + printable(prefix) + " width=" + width + " lhs=" + printable(actual) + " rhs=" + printable(expectedWhole));
        }

        char[] arr = target.toCharArray();
        String viaArray = new String(arr);
        if (!actual.equals(viaArray) || arr.length != actual.length()) {
            throw new RuntimeException("[oracle:shared-state] metamorphic violation: toCharArray must agree with toString after appendFixedWidthPadRight input=" + printable(effective) + " width=" + width + " lhs=" + printable(viaArray) + "/" + arr.length + " rhs=" + printable(actual) + "/" + actual.length());
        }

        // Equivalent-input relation using only real library calls:
        // appendFixedWidthPadRight(s, width, pad) with s.length() >= width must agree with
        // append(s, 0, width), since both append exactly that prefix.
        try {
            StrBuilder lhs = new StrBuilder();
            if (prefix.length() > 0) {
                lhs.append(prefix);
            }
            lhs.appendFixedWidthPadRight(effective, width, padChar);

            StrBuilder rhs = new StrBuilder();
            if (prefix.length() > 0) {
                rhs.append(prefix);
            }
            rhs.append(effective, 0, width);

            if (!lhs.toString().equals(rhs.toString())) {
                throw new RuntimeException("[oracle:equiv-overload] metamorphic violation: fixed-width truncation must equal append(str,0,width) input=" + printable(effective) + " prefix=" + printable(prefix) + " width=" + width + " lhs=" + printable(lhs.toString()) + " rhs=" + printable(rhs.toString()));
            }
        } catch (IllegalArgumentException t) {
            return;
        } catch (NumberFormatException t) {
            return;
        } catch (RuntimeException t) {
            if (isOracleViolation(t)) {
                throw t;
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.lang.text.StrBuilder".equals(e.getClassName())
                    && "appendFixedWidthPadRight".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOracleViolation(RuntimeException t) {
        String msg = t.getMessage();
        return msg != null && msg.startsWith("[oracle:");
    }

    private static String printable(String s) {
        return String.valueOf(s);
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/Lang_59_buggy/fuzz/attempt_023/FuzzHarness.java:66: error: exception NumberFormatException has already been caught
        } catch (NumberFormatException t) {
          ^
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/Lang_59_buggy/fuzz/attempt_023/FuzzHarness.java:114: error: exception NumberFormatException has already been caught
        } catch (NumberFormatException t) {
          ^
2 errors


Return the full corrected FuzzHarness.java. Rules:
- Raw Java source only. No markdown fences. No prose. No explanations. The file starts with a comment or package statement and ends with a closing brace.
- Public class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Use only the FuzzedDataProvider methods listed in the original instructions. Do NOT invent methods like getInt(), consumeDouble(), getRemainingSize(), or consumeIntInRange() — they do not exist.
- Do NOT use classes or methods that are not on the project classpath. If javac says 'cannot find symbol', remove that import and use only classes visible in the source_imports block or java.* / java.awt.*.
```

</details>
<details open><summary>▸ Output (~4,653 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        StrBuilder sb = new StrBuilder(1);
        try {
            sb.appendFixedWidthPadRight("foo", 1, '-');
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        // Contract asserted: when strLen >= width, appendFixedWidthPadRight appends exactly the
        // leftmost width characters and increases the logical content by width. A patch that
        // suppresses the failing operation or corrupts bookkeeping breaks these observables.
        if (!"f".equals(sb.toString())) {
            throw new RuntimeException("[oracle:anchor-result] metamorphic violation: exact trigger should append first width chars input=foo width=1 lhs=" + sb.toString() + " rhs=f");
        }

        sb.minimizeCapacity();
        char[] arr = sb.toCharArray();
        String viaArray = new String(arr);
        if (!"f".equals(viaArray) || arr.length != 1) {
            throw new RuntimeException("[oracle:anchor-state] metamorphic violation: toCharArray/minimizeCapacity must agree with written content input=foo width=1 lhs=" + viaArray + "/" + arr.length + " rhs=f/1");
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        String prefix = data.consumeAsciiString(16);
        String core = data.consumeAsciiString(32);
        if (core.length() == 0) {
            core = "A";
        }
        int width = data.consumeInt(1, core.length());
        char padChar = (char) (data.consumeByte() & 0xff);
        boolean useNullTextPath = data.consumeBoolean();
        boolean doCapacityChanges = data.consumeBoolean();

        StrBuilder target = new StrBuilder(data.consumeInt(1, 32));
        try {
            if (prefix.length() > 0) {
                target.append(prefix);
            }
            if (doCapacityChanges) {
                target.setLength(target.length());
                target.minimizeCapacity();
            }

            if (useNullTextPath) {
                target.setNullText(core);
                target.appendFixedWidthPadRight(null, width, padChar);
            } else {
                target.appendFixedWidthPadRight(core, width, padChar);
            }
        } catch (RuntimeException t) {
            if (t instanceof IllegalArgumentException) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        String effective = core;
        String expectedSlice = effective.substring(0, width);
        String expectedWhole = prefix + expectedSlice;

        // Contract asserted from method body/docs: if strLen >= width, only the first width chars
        // are appended; when the call succeeds, shared-state readers must report the same logical
        // content that writers established.
        String actual = target.toString();
        if (!expectedWhole.equals(actual)) {
            throw new RuntimeException("[oracle:content] metamorphic violation: appendFixedWidthPadRight must append first width chars when input length >= width input=" + printable(effective) + " prefix=" + printable(prefix) + " width=" + width + " lhs=" + printable(actual) + " rhs=" + printable(expectedWhole));
        }

        char[] arr = target.toCharArray();
        String viaArray = new String(arr);
        if (!actual.equals(viaArray) || arr.length != actual.length()) {
            throw new RuntimeException("[oracle:shared-state] metamorphic violation: toCharArray must agree with toString after appendFixedWidthPadRight input=" + printable(effective) + " width=" + width + " lhs=" + printable(viaArray) + "/" + arr.length + " rhs=" + printable(actual) + "/" + actual.length());
        }

        // Equivalent-input relation using only real library calls:
        // appendFixedWidthPadRight(s, width, pad) with s.length() >= width must agree with
        // append(s, 0, width), since both append exactly that prefix.
        try {
            StrBuilder lhs = new StrBuilder();
            if (prefix.length() > 0) {
                lhs.append(prefix);
            }
            lhs.appendFixedWidthPadRight(effective, width, padChar);

            StrBuilder rhs = new StrBuilder();
            if (prefix.length() > 0) {
                rhs.append(prefix);
            }
```

</details>

---
## [50] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [51] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~6,236 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
- *[assistant] message: identical to step [49] — not reprinted*
- *[user] message: identical to step [49] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        StrBuilder sb = new StrBuilder(1);
        try {
            sb.appendFixedWidthPadRight("foo", 1, '-');
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        // Contract asserted: when strLen >= width, appendFixedWidthPadRight appends exactly the
        // leftmost width characters and increases the logical content by width. A patch that
        // suppresses the failing operation or corrupts bookkeeping breaks these observables.
        if (!"f".equals(sb.toString())) {
            throw new RuntimeException("[oracle:anchor-result] metamorphic violation: exact trigger should append first width chars input=foo width=1 lhs=" + sb.toString() + " rhs=f");
        }

        sb.minimizeCapacity();
        char[] arr = sb.toCharArray();
        String viaArray = new String(arr);
        if (!"f".equals(viaArray) || arr.length != 1) {
            throw new RuntimeException("[oracle:anchor-state] metamorphic violation: toCharArray/minimizeCapacity must agree with written content input=foo width=1 lhs=" + viaArray + "/" + arr.length + " rhs=f/1");
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        String prefix = data.consumeAsciiString(16);
        String core = data.consumeAsciiString(32);
        if (core.length() == 0) {
            core = "A";
        }
        int width = data.consumeInt(1, core.length());
        char padChar = (char) (data.consumeByte() & 0xff);
        boolean useNullTextPath = data.consumeBoolean();
        boolean doCapacityChanges = data.consumeBoolean();

        StrBuilder target = new StrBuilder(data.consumeInt(1, 32));
        try {
            if (prefix.length() > 0) {
                target.append(prefix);
            }
            if (doCapacityChanges) {
                target.setLength(target.length());
                target.minimizeCapacity();
            }

            if (useNullTextPath) {
                target.setNullText(core);
                target.appendFixedWidthPadRight(null, width, padChar);
            } else {
                target.appendFixedWidthPadRight(core, width, padChar);
            }
        } catch (RuntimeException t) {
            if (t instanceof IllegalArgumentException) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        String effective = core;
        String expectedSlice = effective.substring(0, width);
        String expectedWhole = prefix + expectedSlice;

        // Contract asserted from method body/docs: if strLen >= width, only the first width chars
        // are appended; when the call succeeds, shared-state readers must report the same logical
        // content that writers established.
        String actual = target.toString();
        if (!expectedWhole.equals(actual)) {
            throw new RuntimeException("[oracle:content] metamorphic violation: appendFixedWidthPadRight must append first width chars when input length >= width input=" + printable(effective) + " prefix=" + printable(prefix) + " width=" + width + " lhs=" + printable(actual) + " rhs=" + printable(expectedWhole));
        }

        char[] arr = target.toCharArray();
        String viaArray = new String(arr);
        if (!actual.equals(viaArray) || arr.length != actual.length()) {
            throw new RuntimeException("[oracle:shared-state] metamorphic violation: toCharArray must agree with toString after appendFixedWidthPadRight input=" + printable(effective) + " width=" + width + " lhs=" + printable(viaArray) + "/" + arr.length + " rhs=" + printable(actual) + "/" + actual.length());
        }

        // Equivalent-input relation using only real library calls:
        // appendFixedWidthPadRight(s, width, pad) with s.length() >= width must agree with
        // append(s, 0, width), since both append exactly that prefix.
        try {
            StrBuilder lhs = new StrBuilder();
            if (prefix.length() > 0) {
                lhs.append(prefix);
            }
            lhs.appendFixedWidthPadRight(effective, width, padChar);

            StrBuilder rhs = new StrBuilder();
            if (prefix.length() > 0) {
                rhs.append(prefix);
            }
           
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/Lang_59_buggy/fuzz/attempt_024/FuzzHarness.java:105: error: reached end of file while parsing
            }
             ^
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/Lang_59_buggy/fuzz/attempt_024/FuzzHarness.java:95: error: 'try' without 'catch', 'finally' or resource declarations
        try {
        ^
2 errors


Return the full corrected FuzzHarness.java. Rules:
- Raw Java source only. No markdown fences. No prose. No explanations. The file starts with a comment or package statement and ends with a closing brace.
- Public class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Use only the FuzzedDataProvider methods listed in the original instructions. Do NOT invent methods like getInt(), consumeDouble(), getRemainingSize(), or consumeIntInRange() — they do not exist.
- Do NOT use classes or methods that are not on the project classpath. If javac says 'cannot find symbol', remove that import and use only classes visible in the source_imports block or java.* / java.awt.*.
```

</details>
<details open><summary>▸ Output (~5,688 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        StrBuilder sb = new StrBuilder(1);
        try {
            sb.appendFixedWidthPadRight("foo", 1, '-');
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        if (!"f".equals(sb.toString())) {
            throw new RuntimeException("[oracle:anchor-result] metamorphic violation: exact trigger should append first width chars input=foo width=1 lhs=" + sb.toString() + " rhs=f");
        }

        sb.minimizeCapacity();
        char[] arr = sb.toCharArray();
        String viaArray = new String(arr);
        if (!"f".equals(viaArray) || arr.length != 1) {
            throw new RuntimeException("[oracle:anchor-state] metamorphic violation: toCharArray/minimizeCapacity must agree with written content input=foo width=1 lhs=" + viaArray + "/" + arr.length + " rhs=f/1");
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        String prefix = data.consumeAsciiString(16);
        String core = data.consumeAsciiString(32);
        if (core.length() == 0) {
            core = "A";
        }

        int width = data.consumeInt(1, core.length());
        char padChar = (char) (data.consumeByte() & 0xff);
        boolean useNullTextPath = data.consumeBoolean();
        boolean doCapacityChanges = data.consumeBoolean();
        int initialCapacity = data.consumeInt(1, 32);

        StrBuilder target = new StrBuilder(initialCapacity);
        try {
            if (prefix.length() > 0) {
                target.append(prefix);
            }
            if (doCapacityChanges) {
                target.setLength(target.length());
                target.minimizeCapacity();
            }

            if (useNullTextPath) {
                target.setNullText(core);
                target.appendFixedWidthPadRight(null, width, padChar);
            } else {
                target.appendFixedWidthPadRight(core, width, padChar);
            }
        } catch (RuntimeException t) {
            if (t instanceof IllegalArgumentException) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        String effective = core;
        String expectedSlice = effective.substring(0, width);
        String expectedWhole = prefix + expectedSlice;
        String actual = target.toString();

        // Contract asserted: when strLen >= width, appendFixedWidthPadRight appends exactly the
        // leftmost width characters. Shared-state readers (toString/toCharArray) must agree with
        // what the append established; a throw-deleting or bookkeeping-breaking patch violates this.
        if (!expectedWhole.equals(actual)) {
            throw new RuntimeException("[oracle:content] metamorphic violation: appendFixedWidthPadRight must append first width chars when input length >= width input=" + printable(effective) + " prefix=" + printable(prefix) + " width=" + width + " lhs=" + printable(actual) + " rhs=" + printable(expectedWhole));
        }

        char[] arr = target.toCharArray();
        String viaArray = new String(arr);
        if (!actual.equals(viaArray) || arr.length != actual.length()) {
            throw new RuntimeException("[oracle:shared-state] metamorphic violation: toCharArray must agree with toString after appendFixedWidthPadRight input=" + printable(effective) + " width=" + width + " lhs=" + printable(viaArray) + "/" + arr.length + " rhs=" + printable(actual) + "/" + actual.length());
        }

        try {
            StrBuilder lhs = new StrBuilder();
            if (prefix.length() > 0) {
                lhs.append(prefix);
            }
            lhs.appendFixedWidthPadRight(effective, width, padChar);

            StrBuilder rhs = new StrBuilder();
            if (prefix.length() > 0) {
                rhs.append(prefix);
            }
            rhs.append(effective, 0, width);

            if (!lhs.toString().equals(rhs.toString())) {
                throw new RuntimeException("[oracle:equiv-overload] metamorphic violation: fixed-width truncation must equal append(str,0,width) input=" + printable(effective) + " prefix=" + printable(prefix) + " width=" + width + " lhs=" + printable(lhs.toString()) + " rhs=" + printable(rhs.toString()));
            }
        } catch (RuntimeException t) {
            if (t instanceof IllegalArgumentException) {
                return;
            }
            if (isOracleViolation(t)) {
                throw t;
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.lang.text.StrBuilder".equals(e.getClassName())
                    && "appendFixedWidthPadRight".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOracleViolation(RuntimeException t) {
        String msg = t.getMessage();
        return msg != null && msg.startsWith("[oracle:");
    }

    private static String printable(String s) {
        return String.valueOf(s);
    }
}
```

</details>

---
## [52] ⚙️ harness-attempt · `attempt_025`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException

---
## [53] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,709 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        /* Contract/oracle:
         * appendFixedWidthPadRight(obj, width, padChar) appends exactly width characters;
         * when obj.toString().length() >= width, the appended segment must be the leftmost
         * width characters of that string. Also, toCharArray() copies the builder contents,
         * and minimizeCapacity() only changes capacity, not contents. Therefore a correct
         * implementation must satisfy:
         *   sb.toString().equals(prefix + core.substring(0, width) + suffix)
         *   new String(sb.toCharArray()).equals(sb.toString())
         * before and after minimizeCapacity().
         * A throw-deleting or branch-skipping patch would violate these observables even if
         * it never crashed.
         */

        // 1) ANCHOR: exact failing test from StrBuilderAppendInsertTest.testLang299
        try {
            StrBuilder anchor = new StrBuilder(1);
            anchor.appendFixedWidthPadRight("foo", 1, '-');
            String anchorResult = anchor.toString();
            if (!"f".equals(anchorResult)) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: exact regression test result input=foo,width=1 lhs=" + anchorResult + " rhs=f");
            }
            String anchorChars = new String(anchor.toCharArray());
            if (!anchorResult.equals(anchorChars)) {
                throw new RuntimeException("[oracle:state] metamorphic violation: toCharArray must agree with toString input=foo,width=1 lhs=" + anchorChars + " rhs=" + anchorResult);
            }
            anchor.minimizeCapacity();
            String anchorMin = anchor.toString();
            if (!anchorResult.equals(anchorMin)) {
                throw new RuntimeException("[oracle:mincap] metamorphic violation: minimizeCapacity must preserve contents input=foo,width=1 lhs=" + anchorMin + " rhs=" + anchorResult);
            }
        } catch (RuntimeException t) {
            boolean validationFamily = (t instanceof IllegalArgumentException) || (t instanceof NumberFormatException);
            if (validationFamily) {
                return;
            }
            boolean rootCause = t instanceof ArrayIndexOutOfBoundsException;
            boolean throughTarget = false;
            StackTraceElement[] st = t.getStackTrace();
            for (int i = 0; i < st.length; i++) {
                if ("org.apache.commons.lang.text.StrBuilder".equals(st[i].getClassName())
                        && "appendFixedWidthPadRight".equals(st[i].getMethodName())) {
                    throughTarget = true;
                    break;
                }
            }
            if (rootCause && throughTarget) {
                throw t;
            }
            return;
        }

        // 2) EXPLORE: valid-by-construction inputs with the root-cause property:
        // positive width and a non-null string whose length is >= width.
        String prefix = data.consumeString(8);
        String core = data.consumeString(32);
        if (core.length() == 0) {
            core = "A";
        }
        int width = data.consumeInt(1, core.length());
        char padChar = (char) (data.consumeByte() & 0xff);
        String suffix = data.consumeString(8);
        int initialCapacity = data.consumeInt(1, 8);

        try {
            StrBuilder sb = new StrBuilder(initialCapacity);
            sb.append(prefix);
            sb.appendFixedWidthPadRight(core, width, padChar);
            sb.append(suffix);

            String expected = prefix + core.substring(0, width) + suffix;
            String actual = sb.toString();
            if (!expected.equals(actual)) {
                throw new RuntimeException("[oracle:content] metamorphic violation: fixed-width append must contribute the leftmost width chars input=prefix(" + prefix + "),core(" + core + "),width=" + width + ",suffix(" + suffix + ") lhs=" + actual + " rhs=" + expected);
            }

            String charsView = new String(sb.toCharArray());
            if (!actual.equals(charsView)) {
                throw new RuntimeException("[oracle:state] metamorphic violation: toCharArray must agree with writer-established contents input=prefix(" + prefix + "),core(" + core + "),width=" + width + ",suffix(" + suffix + ") lhs=" + charsView + " rhs=" + actual);
            }

            sb.minimizeCapacity();
            String afterMinimize = sb.toString();
            if (!actual.equals(afterMinimize)) {
                throw new RuntimeException("[oracle:mincap] metamorphic violation: minimizeCapacity must preserve contents input=prefix(" + prefix + "),core(" + core + "),width=" + width + ",suffix(" + suffix + ") lhs=" + afterMinimize + " rhs=" + actual);
            }

            String afterMinimizeChars = new String(sb.toCharArray());
            if (!afterMinimize.equals(afterMinimizeChars)) {
                throw new RuntimeException("[oracle:state2] metamorphic violation: toCharArray must still agree after minimizeCapacity input=prefix(" + prefix + "),core(" + core + "),width=" + width + ",suffix(" + suffix + ") lhs=" + afterMinimizeChars + " rhs=" + afterMinimize);
            }

            sb.setLength(sb.length());
            String afterIdentitySetLength = sb.toString();
            if (!afterMinimize.equals(afterIdentitySetLength)) {
                throw new RuntimeException("[oracle:setlen] metamorphic violation: setLength(currentLength) must preserve contents input=prefix(" + prefix + "),core(" + core + "),width=" + width + ",suffix(" + suffix + ") lhs=" + afterIdentitySetLength + " rhs=" + afterMinimize);
            }
        } catch (RuntimeException t) {
            boolean validationFamily = (t instanceof IllegalArgumentException) || (t instanceof NumberFormatException);
            if (validationFamily) {
                return;
            }
            boolean rootCause = t instanceof ArrayIndexOutOfBoundsException;
            boolean throughTarget = false;
            StackTraceElement[] st = t.getStackTrace();
            for (int i = 0; i < st.length; i++) {
                if ("org.apache.commons.lang.text.StrBuilder".equals(st[i].getClassName())
                        && "appendFixedWidthPadRight".equals(st[i].getMethodName())) {
                    throughTarget = true;
                    break;
                }
            }
            if (rootCause && throughTarget) {
                throw t;
            }
            return;
        }
    }
}
```

</details>

---
## [54] ⚙️ harness-attempt · `attempt_026`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException

---
## [55] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,996 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Arrays;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchorTrigger();

        String prefix = data.consumeString(16);
        String s = data.consumeString(64);
        if (s == null) {
            s = "";
        }
        int strLen = s.length();
        int width = (strLen == 0) ? 0 : data.consumeInt(1, strLen);
        char padChar = (char) (data.consumeByte() & 0xFF);
        boolean shrink = data.consumeBoolean();
        boolean useSlicePrefix = data.consumeBoolean();

        if (width <= 0 || strLen < width) {
            return;
        }

        StrBuilder sb = new StrBuilder(Math.max(1, data.consumeInt(1, 32)));
        try {
            if (useSlicePrefix && prefix.length() > 0) {
                int start = data.consumeInt(0, prefix.length() - 1);
                int len = data.consumeInt(0, prefix.length() - start);
                sb.append(prefix, start, len);
            } else {
                sb.append(prefix);
            }

            int initialSize = sb.length();
            sb.appendFixedWidthPadRight(s, width, padChar);

            if (shrink) {
                sb.minimizeCapacity();
            }

            String built = sb.toString();
            char[] chars = sb.toCharArray();

            if (chars.length != built.length()) {
                throw new RuntimeException("[oracle:char-array-agreement] metamorphic violation: toCharArray length must equal textual length input="
                        + describe(prefix, s, width) + " lhs=" + chars.length + " rhs=" + built.length());
            }
            if (!Arrays.equals(chars, built.toCharArray())) {
                throw new RuntimeException("[oracle:char-array-agreement] metamorphic violation: toCharArray contents must equal toString contents input="
                        + describe(prefix, s, width) + " lhs=" + new String(chars) + " rhs=" + built);
            }

            String expectedSuffix = s.substring(0, width);
            String actualSuffix = built.substring(initialSize, initialSize + width);

            /*
             * Contract asserted: appendFixedWidthPadRight appends exactly `width` characters; when the
             * source string is at least that wide, those are the leftmost `width` characters of obj.toString().
             * A throw-deleting or silently-wrong patch can avoid crashing yet append the whole string or wrong
             * slice; this post-condition catches that from the public API.
             */
            if (!expectedSuffix.equals(actualSuffix)) {
                throw new RuntimeException("[oracle:fixed-width-prefix] metamorphic violation: appended region must equal first width chars input="
                        + describe(prefix, s, width) + " lhs=" + actualSuffix + " rhs=" + expectedSuffix);
            }
            if (sb.length() != initialSize + width) {
                throw new RuntimeException("[oracle:fixed-width-length] metamorphic violation: builder length must increase by width input="
                        + describe(prefix, s, width) + " lhs=" + sb.length() + " rhs=" + (initialSize + width));
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromPatchedMethod(t) && isValidByConstruction(s, width)) {
                throw t;
            }
            return;
        }
    }

    private static void anchorTrigger() {
        try {
            StrBuilder sb = new StrBuilder(1);
            sb.appendFixedWidthPadRight("foo", 1, '-');

            String out = sb.toString();
            if (!"f".equals(out)) {
                throw new RuntimeException("[oracle:anchor-output] metamorphic violation: exact regression seed must produce truncated width output input=foo/1 lhs="
                        + out + " rhs=f");
            }
            char[] chars = sb.toCharArray();
            if (chars.length != 1 || chars[0] != 'f') {
                throw new RuntimeException("[oracle:anchor-state] metamorphic violation: state readers must agree with anchor write input=foo/1 lhs="
                        + new String(chars) + " rhs=f");
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromPatchedMethod(t)) {
                throw t;
            }
        }
    }

    private static boolean isValidByConstruction(String s, int width) {
        return s != null && width > 0 && s.length() >= width;
    }

    private static boolean isRootCauseFromPatchedMethod(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.lang.text.StrBuilder".equals(e.getClassName())
                    && "appendFixedWidthPadRight".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            String lower = name.toLowerCase();
            if (lower.contains("validation") || lower.contains("invalid")) {
                return true;
            }
        }
        return false;
    }

    private static String describe(String prefix, String s, int width) {
        return "{prefix=" + prefix + ", s=" + s + ", width=" + width + "}";
    }
}
```

</details>

---
## [56] ⚙️ harness-attempt · `attempt_027`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException

---
## [57] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,567 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        /* Contract asserted:
         * For width > 0, appendFixedWidthPadRight(obj, width, padChar) appends exactly width chars.
         * When obj.toString().length() >= width, the result is the first width chars of that string, with no padding.
         * A throw-deleting or silently-wrong patch would violate this observable output.
         * We also check shared-state agreement: toCharArray() and toString()/length() must report the same contents/size.
         */

        try {
            StrBuilder sb = new StrBuilder(1);
            sb.appendFixedWidthPadRight("foo", 1, '-');
            String got = sb.toString();
            if (!"f".equals(got)) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: exact trigger must truncate to width input=foo,width=1 lhs=" + got + " rhs=f");
            }
            char[] arr = sb.toCharArray();
            if (arr.length != 1 || got.length() != arr.length || arr[0] != 'f') {
                throw new RuntimeException("[oracle:anchor-state] metamorphic violation: toCharArray/toString disagreement input=foo,width=1 lhsLen=" + arr.length + " rhsLen=" + got.length());
            }
        } catch (RuntimeException t) {
            String msg = t.getMessage();
            if (msg != null && msg.startsWith("[oracle:")) {
                throw t;
            }
            boolean throughPatchedMethod = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                        && "appendFixedWidthPadRight".equals(ste.getMethodName())) {
                    throughPatchedMethod = true;
                    break;
                }
            }
            if (throughPatchedMethod && t instanceof ArrayIndexOutOfBoundsException) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
        }

        String prefix = data.consumeString(16);
        String payload = data.consumeString(64);
        String suffix = data.consumeString(16);
        char pad = (char) (data.consumeByte() & 0xff);

        if (payload.length() == 0) {
            payload = "X";
        }
        if (prefix.length() == 0) {
            prefix = "P";
        }
        if (suffix.length() == 0) {
            suffix = "S";
        }

        int width = data.consumeInt(1, payload.length());

        StrBuilder lhs = new StrBuilder(data.consumeInt(1, 32));
        StrBuilder rhs = new StrBuilder(data.consumeInt(1, 32));

        try {
            lhs.append(prefix);
            lhs.appendFixedWidthPadRight(payload, width, pad);
            lhs.append(suffix);
        } catch (RuntimeException t) {
            String msg = t.getMessage();
            if (msg != null && msg.startsWith("[oracle:")) {
                throw t;
            }
            boolean throughPatchedMethod = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                        && "appendFixedWidthPadRight".equals(ste.getMethodName())) {
                    throughPatchedMethod = true;
                    break;
                }
            }
            if (throughPatchedMethod && t instanceof ArrayIndexOutOfBoundsException) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            return;
        }

        try {
            rhs.append(prefix);
            rhs.append(payload, 0, width);
            rhs.append(suffix);
        } catch (RuntimeException t) {
            return;
        }

        String lhsString;
        String rhsString;
        char[] lhsChars;
        char[] rhsChars;
        try {
            lhsString = lhs.toString();
            rhsString = rhs.toString();
            lhsChars = lhs.toCharArray();
            rhsChars = rhs.toCharArray();
        } catch (RuntimeException t) {
            return;
        }

        if (!lhsString.equals(rhsString)) {
            throw new RuntimeException("[oracle:eq-truncate] metamorphic violation: appendFixedWidthPadRight(str,width,pad) must equal append(str,0,width) when strLen>=width input="
                    + payload + ",width=" + width + ",prefix=" + prefix + ",suffix=" + suffix + " lhs=" + lhsString + " rhs=" + rhsString);
        }

        if (lhsChars.length != lhsString.length()) {
            throw new RuntimeException("[oracle:state-lhs] metamorphic violation: toCharArray length must equal logical length input="
                    + payload + ",width=" + width + " lhs=" + lhsChars.length + " rhs=" + lhsString.length());
        }
        if (rhsChars.length != rhsString.length()) {
            throw new RuntimeException("[oracle:state-rhs] metamorphic violation: toCharArray length must equal logical length input="
                    + payload + ",width=" + width + " lhs=" + rhsChars.length + " rhs=" + rhsString.length());
        }

        for (int i = 0; i < lhsChars.length; i++) {
            if (lhsChars[i] != lhsString.charAt(i)) {
                throw new RuntimeException("[oracle:state-content] metamorphic violation: toCharArray content must match toString input="
                        + payload + ",width=" + width + ",index=" + i + " lhs=" + lhsChars[i] + " rhs=" + lhsString.charAt(i));
            }
        }

        try {
            String before = lhs.toString();
            int beforeLen = lhs.length();
            lhs.minimizeCapacity();
            String after = lhs.toString();
            int afterLen = lhs.length();
            if (!before.equals(after) || beforeLen != afterLen) {
                throw new RuntimeException("[oracle:mincap] metamorphic violation: minimizeCapacity must not change content or length input="
                        + payload + ",width=" + width + " lhs=" + after + "/" + afterLen + " rhs=" + before + "/" + beforeLen);
            }
        } catch (RuntimeException t) {
            String msg = t.getMessage();
            if (msg != null && msg.startsWith("[oracle:")) {
                throw t;
            }
        }
    }
}
```

</details>

---
## [58] ⚙️ harness-attempt · `attempt_028`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException

---
## [59] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,763 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        String prefix = data.consumeString(16);
        String value = data.consumeString(32);
        if (value == null) {
            return;
        }

        int valueLen = value.length();
        int width;
        if (valueLen == 0) {
            width = data.consumeInt(0, 8);
        } else {
            width = data.consumeInt(1, valueLen + 8);
        }
        char padChar = (char) (data.consumeByte() & 0xff);

        runExplore(prefix, value, width, padChar);
    }

    private static void runAnchor() {
        StrBuilder sb = new StrBuilder(1);
        try {
            sb.appendFixedWidthPadRight("foo", 1, '-');
        } catch (RuntimeException t) {
            if (isOracleViolation(t) || isRootCauseAioobeInPatchedMethod(t)) {
                throw t;
            }
            return;
        }

        /* Contract asserted:
         * appendFixedWidthPadRight appends a fixed-width field; when the string is longer than width,
         * the content must be truncated to width characters, so "foo" with width 1 yields "f".
         * Also, toCharArray() "copies the builder's character array into a new character array" representing
         * the contents, so it must agree with toString(). A throw-deleting or bookkeeping-skipping patch can
         * make the call stop crashing yet leave wrong contents/state; this post-condition catches that.
         */
        String actual = sb.toString();
        if (!"f".equals(actual)) {
            throw new RuntimeException("[oracle:anchor] metamorphic violation: fixed-width truncation input=foo width=1 lhs=" + actual + " rhs=f");
        }
        String viaChars = new String(sb.toCharArray());
        if (!actual.equals(viaChars)) {
            throw new RuntimeException("[oracle:state] metamorphic violation: toCharArray must agree with toString input=foo width=1 lhs=" + actual + " rhs=" + viaChars);
        }
    }

    private static void runExplore(String prefix, String value, int width, char padChar) {
        if (width < 0) {
            return;
        }

        StrBuilder sb = new StrBuilder(Math.max(1, prefix.length() + 1));
        try {
            sb.append(prefix);
            sb.minimizeCapacity();
            sb.appendFixedWidthPadRight(value, width, padChar);
        } catch (IllegalArgumentException t) {
            return;
        } catch (NumberFormatException t) {
            return;
        } catch (RuntimeException t) {
            if (isOracleViolation(t) || (isRootCauseAioobeInPatchedMethod(t) && isValidByConstruction(value, width))) {
                throw t;
            }
            return;
        }

        /* Contract asserted:
         * appendFixedWidthPadRight appends exactly width characters: truncate when source is longer,
         * otherwise append the source then right-pad. append(String) established the prefix first, so the
         * final textual content must equal prefix + expectedFixedWidth(value,width,padChar).
         * toCharArray() copies the current contents, so it must agree with toString() on the same state.
         * This detects silent wrong-output/state-corruption patches that avoid throwing.
         */
        String expected = prefix + expectedFixedWidth(value, width, padChar);
        String actual = sb.toString();
        if (!expected.equals(actual)) {
            throw new RuntimeException("[oracle:content] metamorphic violation: appendFixedWidthPadRight content input=" + printable(value) + " width=" + width + " pad=" + (int) padChar + " lhs=" + actual + " rhs=" + expected);
        }

        String viaChars;
        try {
            viaChars = new String(sb.toCharArray());
        } catch (RuntimeException t) {
            return;
        }
        if (!actual.equals(viaChars)) {
            throw new RuntimeException("[oracle:state] metamorphic violation: toCharArray must agree with toString input=" + printable(value) + " width=" + width + " lhs=" + actual + " rhs=" + viaChars);
        }
    }

    private static boolean isValidByConstruction(String value, int width) {
        return value != null && width > 0;
    }

    private static boolean isRootCauseAioobeInPatchedMethod(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] frames = t.getStackTrace();
        if (frames == null) {
            return false;
        }
        for (int i = 0; i < frames.length; i++) {
            StackTraceElement f = frames[i];
            if ("org.apache.commons.lang.text.StrBuilder".equals(f.getClassName())
                    && "appendFixedWidthPadRight".equals(f.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOracleViolation(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
    }

    private static String expectedFixedWidth(String value, int width, char padChar) {
        if (width <= 0) {
            return "";
        }
        if (value.length() >= width) {
            return value.substring(0, width);
        }
        StringBuilder out = new StringBuilder(width);
        out.append(value);
        for (int i = value.length(); i < width; i++) {
            out.append(padChar);
        }
        return out.toString();
    }

    private static String printable(String s) {
        return s == null ? "null" : s;
    }
}
```

</details>

---
## [60] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [61] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~7,017 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        String prefix = data.consumeString(16);
        String value = data.consumeString(32);
        if (value == null) {
            return;
        }

        int valueLen = value.length();
        int width;
        if (valueLen == 0) {
            width = data.consumeInt(0, 8);
        } else {
            width = data.consumeInt(1, valueLen + 8);
        }
        char padChar = (char) (data.consumeByte() & 0xff);

        runExplore(prefix, value, width, padChar);
    }

    private static void runAnchor() {
        StrBuilder sb = new StrBuilder(1);
        try {
            sb.appendFixedWidthPadRight("foo", 1, '-');
        } catch (RuntimeException t) {
            if (isOracleViolation(t) || isRootCauseAioobeInPatchedMethod(t)) {
                throw t;
            }
            return;
        }

        /* Contract asserted:
         * appendFixedWidthPadRight appends a fixed-width field; when the string is longer than width,
         * the content must be truncated to width characters, so "foo" with width 1 yields "f".
         * Also, toCharArray() "copies the builder's character array into a new character array" representing
         * the contents, so it must agree with toString(). A throw-deleting or bookkeeping-skipping patch can
         * make the call stop crashing yet leave wrong contents/state; this post-condition catches that.
         */
        String actual = sb.toString();
        if (!"f".equals(actual)) {
            throw new RuntimeException("[oracle:anchor] metamorphic violation: fixed-width truncation input=foo width=1 lhs=" + actual + " rhs=f");
        }
        String viaChars = new String(sb.toCharArray());
        if (!actual.equals(viaChars)) {
            throw new RuntimeException("[oracle:state] metamorphic violation: toCharArray must agree with toString input=foo width=1 lhs=" + actual + " rhs=" + viaChars);
        }
    }

    private static void runExplore(String prefix, String value, int width, char padChar) {
        if (width < 0) {
            return;
        }

        StrBuilder sb = new StrBuilder(Math.max(1, prefix.length() + 1));
        try {
            sb.append(prefix);
            sb.minimizeCapacity();
            sb.appendFixedWidthPadRight(value, width, padChar);
        } catch (IllegalArgumentException t) {
            return;
        } catch (NumberFormatException t) {
            return;
        } catch (RuntimeException t) {
            if (isOracleViolation(t) || (isRootCauseAioobeInPatchedMethod(t) && isValidByConstruction(value, width))) {
                throw t;
            }
            return;
        }

        /* Contract asserted:
         * appendFixedWidthPadRight appends exactly width characters: truncate when source is longer,
         * otherwise append the source then right-pad. append(String) established the prefix first, so the
         * final textual content must equal prefix + expectedFixedWidth(value,width,padChar).
         * toCharArray() copies the current contents, so it must agree with toString() on the same state.
         * This detects silent wrong-output/state-corruption patches that avoid throwing.
         */
        String expected = prefix + expectedFixedWidth(value, width, padChar);
        String actual = sb.toString();
        if (!expected.equals(actual)) {
            throw new RuntimeException("[oracle:content] metamorphic violation: appendFixedWidthPadRight content input=" + printable(value) + " width=" + width + " pad=" + (int) padChar + " lhs=" + actual + " rhs=" + expected);
        }

        String viaChars;
        try {
            viaChars = new String(sb.toCharArray());
        } catch (RuntimeException t) {
            return;
        }
        if (!actual.equals(viaChars)) {
            throw new RuntimeException("[oracle:state] metamorphic violation: toCharArray must agree with toString input=" + printable(value) + " width=" + width + " lhs=" + actual + " rhs=" + viaChars);
        }
    }

    private static boolean isValidByConstruction(String value, int width) {
        return value != null && width > 0;
    }

    private static boolean isRootCauseAioobeInPatchedMethod(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] frames = t.getStackTrace();
        if (frames == null) {
            return false;
        }
        for (int i = 0; i < frames.length; i++) {
            StackTraceElement f = frames[i];
            if ("org.apache.commons.lang.text.StrBuilder".equals(f.getClassName())
                    && "appendFixedWidthPadRight".equals(f.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOracleViolation(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
    }

    private static String expectedFixedWidth(String value, int width, char padChar) {
        if (width <= 0) {
            return "";
        }
        if (value.length() >= width) {
            return value.substring(0, width);
        }
        StringBuilder out = new StringBuilder(width);
        out.append(value);
        for (int i = value.length(); i < width; i++) {
            out.append(padChar);
        }
        return out.toString();
    }

    private static String printable(String s) {
        return s == null ? "null" : s;
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/Lang_59_buggy/fuzz/attempt_029/FuzzHarness.java:67: error: exception NumberFormatException has already been caught
        } catch (NumberFormatException t) {
          ^
1 error


Return the full corrected FuzzHarness.java. Rules:
- Raw Java source only. No markdown fences. No prose. No explanations. The file starts with a comment or package statement and ends with a closing brace.
- Public class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Use only the FuzzedDataProvider methods listed in the original instructions. Do NOT invent methods like getInt(), consumeDouble(), getRemainingSize(), or consumeIntInRange() — they do not exist.
- Do NOT use classes or methods that are not on the project classpath. If javac says 'cannot find symbol', remove that import and use only classes visible in the source_imports block or java.* / java.awt.*.
```

</details>
<details open><summary>▸ Output (~5,559 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        String prefix = data.consumeString(16);
        String value = data.consumeString(32);
        if (value == null) {
            return;
        }

        int valueLen = value.length();
        int width;
        if (valueLen == 0) {
            width = data.consumeInt(0, 8);
        } else {
            width = data.consumeInt(1, valueLen + 8);
        }
        char padChar = (char) (data.consumeByte() & 0xff);

        runExplore(prefix, value, width, padChar);
    }

    private static void runAnchor() {
        StrBuilder sb = new StrBuilder(1);
        try {
            sb.appendFixedWidthPadRight("foo", 1, '-');
        } catch (RuntimeException t) {
            if (isOracleViolation(t) || isRootCauseAioobeInPatchedMethod(t)) {
                throw t;
            }
            return;
        }

        /* Contract asserted:
         * appendFixedWidthPadRight appends a fixed-width field; if the source string is longer than width,
         * it must truncate to width characters, so "foo" with width 1 yields "f".
         * toCharArray() copies the builder contents, so it must agree with toString() on the same state.
         */
        String actual = sb.toString();
        if (!"f".equals(actual)) {
            throw new RuntimeException("[oracle:anchor] metamorphic violation: fixed-width truncation input=foo width=1 lhs=" + actual + " rhs=f");
        }
        String viaChars = new String(sb.toCharArray());
        if (!actual.equals(viaChars)) {
            throw new RuntimeException("[oracle:state] metamorphic violation: toCharArray must agree with toString input=foo width=1 lhs=" + actual + " rhs=" + viaChars);
        }
    }

    private static void runExplore(String prefix, String value, int width, char padChar) {
        if (width < 0) {
            return;
        }

        StrBuilder sb = new StrBuilder(Math.max(1, prefix.length() + 1));
        try {
            sb.append(prefix);
            sb.minimizeCapacity();
            sb.appendFixedWidthPadRight(value, width, padChar);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isOracleViolation(t) || (isRootCauseAioobeInPatchedMethod(t) && isValidByConstruction(value, width))) {
                throw t;
            }
            return;
        }

        /* Contract asserted:
         * appendFixedWidthPadRight appends exactly width characters: truncate when source is longer,
         * otherwise append the source then right-pad. append(String) established the prefix first, so the
         * final textual content must equal prefix + expectedFixedWidth(value,width,padChar).
         * toCharArray() copies the current contents, so it must agree with toString() on the same state.
         */
        String expected = prefix + expectedFixedWidth(value, width, padChar);
        String actual = sb.toString();
        if (!expected.equals(actual)) {
            throw new RuntimeException("[oracle:content] metamorphic violation: appendFixedWidthPadRight content input=" + printable(value) + " width=" + width + " pad=" + (int) padChar + " lhs=" + actual + " rhs=" + expected);
        }

        String viaChars;
        try {
            viaChars = new String(sb.toCharArray());
        } catch (RuntimeException t) {
            return;
        }
        if (!actual.equals(viaChars)) {
            throw new RuntimeException("[oracle:state] metamorphic violation: toCharArray must agree with toString input=" + printable(value) + " width=" + width + " lhs=" + actual + " rhs=" + viaChars);
        }
    }

    private static boolean isValidByConstruction(String value, int width) {
        return value != null && width > 0;
    }

    private static boolean isCleanRejection(RuntimeException t) {
        return (t instanceof IllegalArgumentException) || (t instanceof NumberFormatException);
    }

    private static boolean isRootCauseAioobeInPatchedMethod(Throwable t) {
        if (!(t instanceof ArrayIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] frames = t.getStackTrace();
        if (frames == null) {
            return false;
        }
        for (int i = 0; i < frames.length; i++) {
            StackTraceElement f = frames[i];
            if ("org.apache.commons.lang.text.StrBuilder".equals(f.getClassName())
                    && "appendFixedWidthPadRight".equals(f.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOracleViolation(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
    }

    private static String expectedFixedWidth(String value, int width, char padChar) {
        if (width <= 0) {
            return "";
        }
        if (value.length() >= width) {
            return value.substring(0, width);
        }
        StringBuffer out = new StringBuffer(width);
        out.append(value);
        for (int i = value.length(); i < width; i++) {
            out.append(padChar);
        }
        return out.toString();
    }

    private static String printable(String s) {
        return s == null ? "null" : s;
    }
}
```

</details>

---
## [62] ⚙️ harness-attempt · `attempt_030`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException

---
## [63] ⚙️ coverage · `jazzer coverage dumps`
**output:** **27/27 dump(s) written under /datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/cov; 27 raw fuzz log(s)**
<details><summary>▸ detail (~13,681 chars)</summary>

- detail: {'dumps': [{'harness': 'attempt_001', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_001_compiled.exec', 'exists': True, 'bytes': 250}, {'harness': 'attempt_002', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_002_compiled.exec', 'exists': True, 'bytes': 248}, {'harness': 'attempt_003', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_003_compiled.exec', 'exists': True, 'bytes': 248}, {'harness': 'attempt_004', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_004_compiled.exec', 'exists': True, 'bytes': 250}, {'harness': 'attempt_005', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_005_compiled.exec', 'exists': True, 'bytes': 248}, {'harness': 'attempt_006', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_006_compiled.exec', 'exists': True, 'bytes': 251}, {'harness': 'attempt_007', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_007_compiled.exec', 'exists': True, 'bytes': 248}, {'harness': 'attempt_008', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_008_compiled.exec', 'exists': True, 'bytes': 249}, {'harness': 'attempt_009', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_009_compiled.exec', 'exists': True, 'bytes': 248}, {'harness': 'attempt_010', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_010_compiled.exec', 'exists': True, 'bytes': 248}, {'harness': 'attempt_011', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_011_compiled.exec', 'exists': True, 'bytes': 247}, {'harness': 'attempt_012', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_012_compiled.exec', 'exists': True, 'bytes': 247}, {'harness': 'attempt_013', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_013_compiled.exec', 'exists': True, 'bytes': 247}, {'harness': 'attempt_014', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_014_compiled.exec', 'exists': True, 'bytes': 246}, {'harness': 'attempt_015', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_015_compiled.exec', 'exists': True, 'bytes': 248}, {'harness': 'attempt_016', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_016_compiled.exec', 'exists': True, 'bytes': 250}, {'harness': 'attempt_017', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_017_compiled.exec', 'exists': True, 'bytes': 248}, {'harness': 'attempt_018', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_018_compiled.exec', 'exists': True, 'bytes': 250}, {'harness': 'attempt_019', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_019_compiled.exec', 'exists': True, 'bytes': 250}, {'harness': 'attempt_020', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_020_compiled.exec', 'exists': True, 'bytes': 249}, {'harness': 'attempt_021', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_021_compiled.exec', 'exists': True, 'bytes': 248}, {'harness': 'attempt_022', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_022_compiled.exec', 'exists': True, 'bytes': 250}, {'harness': 'attempt_025', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_025_compiled.exec', 'exists': True, 'bytes': 248}, {'harness': 'attempt_026', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_026_compiled.exec', 'exists': True, 'bytes': 247}, {'harness': 'attempt_027', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_027_compiled.exec', 'exists': True, 'bytes': 249}, {'harness': 'attempt_028', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_028_compiled.exec', 'exists': True, 'bytes': 251}, {'harness': 'attempt_030', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_030_compiled.exec', 'exists': True, 'bytes': 248}], 'outputs': ['/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_001_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_002_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_003_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_004_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_005_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_006_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_007_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_008_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_009_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_010_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_011_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_012_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_013_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_014_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_015_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_016_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_017_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_018_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_019_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_020_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_021_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_022_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_025_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_026_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_027_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_028_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_030_compiled.txt'], 'classpath': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/cov/classpath.json', 'dir': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/cov', 'compiled_attempts': ['attempt_001', 'attempt_002', 'attempt_003', 'attempt_004', 'attempt_005', 'attempt_006', 'attempt_007', 'attempt_008', 'attempt_009', 'attempt_010', 'attempt_011', 'attempt_012', 'attempt_013', 'attempt_014', 'attempt_015', 'attempt_016', 'attempt_017', 'attempt_018', 'attempt_019', 'attempt_020', 'attempt_021', 'attempt_022', 'attempt_025', 'attempt_026', 'attempt_027', 'attempt_028', 'attempt_030'], 'accepted_attempts': ['attempt_001', 'attempt_002', 'attempt_003', 'attempt_004', 'attempt_005', 'attempt_006', 'attempt_007', 'attempt_008', 'attempt_009', 'attempt_010', 'attempt_011', 'attempt_012', 'attempt_013', 'attempt_014', 'attempt_015', 'attempt_016', 'attempt_017', 'attempt_018', 'attempt_019', 'attempt_020', 'attempt_021', 'attempt_022', 'attempt_025', 'attempt_026', 'attempt_027', 'attempt_028', 'attempt_030'], 'sources': ['/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_001.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_002.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_003.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_004.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_005.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_006.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_007.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_008.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_009.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_010.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_011.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_012.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_013.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_014.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_015.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_016.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_017.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_018.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_019.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_020.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_021.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_022.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_025.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_026.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_027.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_028.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_030.java']}

</details>
