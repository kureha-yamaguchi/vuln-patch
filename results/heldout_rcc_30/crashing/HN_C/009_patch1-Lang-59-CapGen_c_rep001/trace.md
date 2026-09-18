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
<details><summary>▸ output — FailureTest(test_class='org.apache.commons.lang.text.StrBuilderAppendInsertTest', test_method='testL (~642 chars)</summary>

**output:** 
```json
[
  "FailureTest(test_class='org.apache.commons.lang.text.StrBuilderAppendInsertTest', test_method='testLang299', source_path='/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/Lang_59_buggy/src/test/org/apache/commons/lang/text/StrBuilderAppendInsertTest.java', method_source='    public void testLang299() {\\n        StrBuilder sb = new StrBuilder(1);\\n        sb.appendFixedWidthPadRight(\"foo\", 1, \\'-\\');\\n        assertEquals(\"f\", sb.toString());\\n    }', exception_type='java.lang.ArrayIndexOutOfBoundsException', failure_message=None, support_source=None)"
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
<details><summary>▸ Prompt (2 message(s), ~3,624 chars)</summary>

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

Codebase: `Lang_59_buggy`. Write a fuzz harness for the function(s) shown below.

Call them through the library's real public API, with inputs built from the FuzzedDataProvider, and exercise as much of each function's behaviour as you can: vary lengths, sizes, signs and contents, cover empty and boundary values, and try the argument combinations the signature allows. Construct whatever real library objects the call needs. A throwable that escapes fuzzerTestOneInput is reported as a finding, so let exceptions from the code under test propagate.

Function `appendFixedWidthPadRight` — declared in `org.apache.commons.lang.text.StrBuilder`:
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
<details open><summary>▸ Output (~4,626 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder builder;
        int ctorChoice = data.consumeInt(0, 2);
        if (ctorChoice == 0) {
            builder = new StrBuilder();
        } else if (ctorChoice == 1) {
            builder = new StrBuilder(data.consumeInt(-8, 256));
        } else {
            builder = new StrBuilder(data.consumeString(128));
        }

        if (data.consumeBoolean()) {
            builder.setNewLineText(data.consumeBoolean() ? null : data.consumeString(32));
        }

        if (data.consumeBoolean()) {
            builder.setNullText(data.consumeBoolean() ? null : data.consumeString(32));
        }

        int preOps = data.consumeInt(0, 6);
        for (int i = 0; i < preOps; i++) {
            switch (data.consumeInt(0, 5)) {
                case 0:
                    builder.append(data.consumeBoolean() ? null : data.consumeString(64));
                    break;
                case 1:
                    builder.append(data.consumeAsciiString(64));
                    break;
                case 2:
                    builder.append(data.consumeInt());
                    break;
                case 3:
                    builder.append(data.consumeBoolean());
                    break;
                case 4:
                    builder.ensureCapacity(data.consumeInt(-16, 512));
                    break;
                default:
                    builder.setLength(data.consumeInt(0, 128));
                    break;
            }
        }

        Object obj;
        switch (data.consumeInt(0, 7)) {
            case 0:
                obj = null;
                break;
            case 1:
                obj = data.consumeString(256);
                break;
            case 2:
                obj = data.consumeAsciiString(256);
                break;
            case 3:
                obj = Integer.valueOf(data.consumeInt());
                break;
            case 4:
                obj = Boolean.valueOf(data.consumeBoolean());
                break;
            case 5:
                obj = Byte.valueOf(data.consumeByte());
                break;
            case 6:
                obj = new String(data.consumeBytes(128));
                break;
            default:
                obj = new StrBuilder(data.consumeString(128));
                break;
        }

        char padChar;
        if (data.consumeBoolean()) {
            padChar = (char) (data.consumeByte() & 0xFF);
        } else {
            String s = data.consumeString(2);
            padChar = s.length() == 0 ? '\u0000' : s.charAt(0);
        }

        int widthChoice = data.consumeInt(0, 8);
        int width;
        switch (widthChoice) {
            case 0:
                width = data.consumeInt();
                break;
            case 1:
                width = data.consumeInt(-16, 256);
                break;
            case 2:
                width = -1;
                break;
            case 3:
                width = 0;
                break;
            case 4:
                width = 1;
                break;
            case 5:
                width = 2;
                break;
            case 6:
                width = 8;
                break;
            case 7:
                width = 64;
                break;
            default:
                width = 256;
                break;
        }

        builder.appendFixedWidthPadRight(obj, width, padChar);

        int postOps = data.consumeInt(0, 3);
        for (int i = 0; i < postOps; i++) {
            Object obj2;
            switch (data.consumeInt(0, 4)) {
                case 0:
                    obj2 = null;
                    break;
                case 1:
                    obj2 = data.consumeRemainingAsString();
                    break;
                case 2:
                    obj2 = Integer.valueOf(data.consumeInt());
                    break;
                case 3:
                    obj2 = new StrBuilder(data.consumeString(64));
                    break;
                default:
                    obj2 = Boolean.valueOf(data.consumeBoolean());
                    break;
            }

            int w2 = data.remainingBytes() > 0 ? data.consumeInt(-8, 128) : 0;
            char p2 = data.remainingBytes() > 0 ? (char) (data.consumeByte() & 0xFF) : ' ';
            builder.appendFixedWidthPadRight(obj2, w2, p2);
        }
    }
}
```

</details>

---
## [4] ⚙️ harness-attempt · `attempt_001`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [5] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,358 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder builder;
        switch (data.consumeInt(0, 3)) {
            case 0:
                builder = new StrBuilder();
                break;
            case 1:
                builder = new StrBuilder(data.consumeInt(0, 256));
                break;
            case 2:
                builder = new StrBuilder(data.consumeString(128));
                break;
            default:
                builder = new StrBuilder(data.consumeAsciiString(128));
                break;
        }

        if (data.consumeBoolean()) {
            builder.setNullText(null);
        } else {
            if (data.consumeBoolean()) {
                builder.setNullText(data.consumeString(32));
            } else {
                builder.setNullText(data.consumeAsciiString(32));
            }
        }

        if (data.consumeBoolean()) {
            builder.append(data.consumeString(64));
        }
        if (data.consumeBoolean()) {
            builder.append(data.consumeAsciiString(64));
        }
        if (data.consumeBoolean()) {
            builder.append(data.consumeInt());
        }
        if (data.consumeBoolean()) {
            builder.append(data.consumeBoolean());
        }

        int iterations = data.consumeInt(1, 6);
        for (int i = 0; i < iterations; i++) {
            Object obj;
            switch (data.consumeInt(0, 8)) {
                case 0:
                    obj = null;
                    break;
                case 1:
                    obj = data.consumeString(128);
                    break;
                case 2:
                    obj = data.consumeAsciiString(128);
                    break;
                case 3:
                    obj = Integer.valueOf(data.consumeInt());
                    break;
                case 4:
                    obj = Long.valueOf((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                    break;
                case 5:
                    obj = Boolean.valueOf(data.consumeBoolean());
                    break;
                case 6:
                    obj = Character.valueOf((char) (data.consumeByte() & 0xff));
                    break;
                case 7:
                    obj = new StringBuffer(data.consumeString(64));
                    break;
                default:
                    obj = new StrBuilder(data.consumeAsciiString(64));
                    break;
            }

            int width;
            switch (data.consumeInt(0, 5)) {
                case 0:
                    width = data.consumeInt(-4, 4);
                    break;
                case 1:
                    width = data.consumeInt(0, 32);
                    break;
                case 2:
                    width = data.consumeInt(33, 256);
                    break;
                case 3:
                    width = 0;
                    break;
                case 4:
                    width = 1;
                    break;
                default:
                    width = data.consumeInt(-256, 1024);
                    break;
            }

            char padChar = (char) (data.consumeByte() & 0xff);
            builder.appendFixedWidthPadRight(obj, width, padChar);

            if (data.consumeBoolean()) {
                if (data.consumeBoolean()) {
                    builder.setNullText(null);
                } else if (data.consumeBoolean()) {
                    builder.setNullText(data.consumeString(16));
                } else {
                    builder.setNullText(data.consumeAsciiString(16));
                }
            }
        }

        if (data.remainingBytes() > 0) {
            Object finalObj;
            if (data.consumeBoolean()) {
                finalObj = null;
            } else if (data.consumeBoolean()) {
                finalObj = data.consumeRemainingAsString();
            } else {
                finalObj = new StrBuilder(data.consumeRemainingAsString());
            }
            builder.appendFixedWidthPadRight(finalObj, data.consumeInt(-32, 512), (char) (data.consumeByte() & 0xff));
        }
    }
}
```

</details>

---
## [6] ⚙️ harness-attempt · `attempt_002`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [7] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~2,874 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int initialCapacity = data.consumeInt(-16, 64);
        StrBuilder builder = new StrBuilder(initialCapacity);

        if (data.consumeBoolean()) {
            builder.append(data.consumeString(64));
        } else if (data.consumeBoolean()) {
            builder.append(data.consumeAsciiString(64));
        }

        if (data.consumeBoolean()) {
            builder.minimizeCapacity();
        }

        if (data.consumeBoolean()) {
            if (data.consumeBoolean()) {
                builder.setNullText(null);
            } else if (data.consumeBoolean()) {
                builder.setNullText(data.consumeString(32));
            } else {
                builder.setNullText(data.consumeAsciiString(32));
            }
        }

        Object obj;
        switch (data.consumeInt(0, 7)) {
            case 0:
                obj = null;
                break;
            case 1:
                obj = data.consumeString(256);
                break;
            case 2:
                obj = data.consumeAsciiString(256);
                break;
            case 3:
                obj = Integer.valueOf(data.consumeInt());
                break;
            case 4:
                obj = Boolean.valueOf(data.consumeBoolean());
                break;
            case 5:
                obj = Character.valueOf((char) (data.consumeByte() & 0xff));
                break;
            case 6:
                obj = new StrBuilder(data.consumeString(256));
                break;
            default:
                obj = new String(data.consumeBytes(256));
                break;
        }

        int width = data.consumeInt();
        char padChar = (char) (data.consumeByte() & 0xff);
        builder.appendFixedWidthPadRight(obj, width, padChar);

        if (data.consumeBoolean()) {
            StrBuilder targeted = new StrBuilder();
            if (data.consumeBoolean()) {
                targeted.append(data.consumeString(16));
            }
            targeted.minimizeCapacity();

            String longString;
            if (data.consumeBoolean()) {
                longString = data.consumeRemainingAsString();
            } else {
                longString = data.consumeString(512);
            }

            int targetedWidth;
            if (data.consumeBoolean()) {
                targetedWidth = data.consumeInt(1, 32);
            } else {
                targetedWidth = Math.max(1, data.consumeInt() & 31);
            }

            char targetedPad = (char) (data.consumeByte() & 0xff);
            targeted.appendFixedWidthPadRight(longString, targetedWidth, targetedPad);
        }
    }
}
```

</details>

---
## [8] ⚙️ harness-attempt · `attempt_003`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [9] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,008 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder builder;
        if (data.consumeBoolean()) {
            builder = new StrBuilder(data.consumeInt(0, 256));
        } else {
            builder = new StrBuilder(data.consumeString(64));
        }

        if (data.consumeBoolean()) {
            builder.append(data.consumeString(64));
        }
        if (data.consumeBoolean()) {
            builder.append(data.consumeAsciiString(64));
        }
        if (data.consumeBoolean()) {
            builder.append((char) (data.consumeByte() & 0xff));
        }

        String nullText = null;
        if (data.consumeBoolean()) {
            nullText = data.consumeBoolean() ? null : data.consumeString(32);
            builder.setNullText(nullText);
        }

        Object obj;
        switch (data.consumeInt(0, 7)) {
            case 0:
                obj = null;
                break;
            case 1:
                obj = data.consumeString(64);
                break;
            case 2:
                obj = data.consumeAsciiString(64);
                break;
            case 3:
                obj = Integer.valueOf(data.consumeInt());
                break;
            case 4:
                obj = Long.valueOf((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                break;
            case 5:
                obj = new StringBuffer(data.consumeString(64));
                break;
            case 6:
                obj = new StrBuilder(data.consumeString(64));
                break;
            default:
                obj = data.consumeBytes(32);
                break;
        }

        char padChar = (char) (data.consumeByte() & 0xff);
        int width = data.consumeInt(-8, 256);

        builder.appendFixedWidthPadRight(obj, width, padChar);

        String rendered = (obj == null) ? nullText : obj.toString();
        if (rendered != null) {
            int len = rendered.length();
            builder.appendFixedWidthPadRight(obj, -1, padChar);
            builder.appendFixedWidthPadRight(obj, 0, padChar);
            builder.appendFixedWidthPadRight(obj, 1, padChar);
            builder.appendFixedWidthPadRight(obj, Math.max(0, len - 1), padChar);
            builder.appendFixedWidthPadRight(obj, len, padChar);
            builder.appendFixedWidthPadRight(obj, len + 1, padChar);
        } else {
            builder.appendFixedWidthPadRight(null, data.consumeInt(-2, 8), padChar);
        }

        if (data.remainingBytes() > 0) {
            Object obj2 = data.consumeBoolean() ? data.consumeRemainingAsString() : data.consumeRemainingAsBytes();
            int width2 = data.consumeBoolean() ? data.remainingBytes() : 0;
            builder.appendFixedWidthPadRight(obj2, width2, (char) (padChar ^ 0x5a));
        }
    }
}
```

</details>

---
## [10] ⚙️ harness-attempt · `attempt_004`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [11] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~2,568 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder builder;
        if (data.consumeBoolean()) {
            builder = new StrBuilder();
        } else {
            builder = new StrBuilder(data.consumeString(64));
        }

        if (data.consumeBoolean()) {
            if (data.consumeBoolean()) {
                builder.setNullText(null);
            } else {
                builder.setNullText(data.consumeString(32));
            }
        }

        if (data.consumeBoolean()) {
            builder.append(data.consumeString(32));
        }

        Object obj;
        int kind = data.consumeInt(0, 5);
        switch (kind) {
            case 0:
                obj = null;
                break;
            case 1:
                obj = data.consumeString(256);
                break;
            case 2:
                obj = data.consumeAsciiString(256);
                break;
            case 3:
                obj = Integer.valueOf(data.consumeInt());
                break;
            case 4:
                obj = new StrBuilder(data.consumeString(256));
                break;
            default:
                obj = data.consumeRemainingAsBytes();
                break;
        }

        char padChar = (char) (data.consumeByte() & 0xff);

        int width;
        if (obj instanceof String && data.consumeBoolean()) {
            String s = (String) obj;
            if (data.consumeBoolean()) {
                width = s.length();
            } else {
                width = s.length() == 0 ? 0 : data.consumeInt(-2, s.length() + 2);
            }
        } else if (obj instanceof StrBuilder && data.consumeBoolean()) {
            int len = ((StrBuilder) obj).length();
            width = len == 0 ? data.consumeInt(-2, 2) : data.consumeInt(-2, len + 2);
        } else {
            width = data.consumeInt();
        }

        builder.appendFixedWidthPadRight(obj, width, padChar);

        if (data.remainingBytes() > 0) {
            String s = data.consumeRemainingAsString();
            int secondWidth;
            if (data.consumeBoolean()) {
                secondWidth = s.length() == 0 ? 0 : data.consumeInt(-2, s.length() + 2);
            } else {
                secondWidth = data.consumeInt();
            }
            builder.appendFixedWidthPadRight(s, secondWidth, (char) (padChar ^ 0x5a));
        }
    }
}
```

</details>

---
## [12] ⚙️ harness-attempt · `attempt_005`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [13] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,744 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder sb;
        switch (data.consumeInt(0, 2)) {
            case 0:
                sb = new StrBuilder();
                break;
            case 1:
                sb = new StrBuilder(data.consumeString(32));
                break;
            default:
                sb = new StrBuilder(data.consumeInt(0, 128));
                break;
        }

        if (data.consumeBoolean()) {
            switch (data.consumeInt(0, 2)) {
                case 0:
                    sb.setNullText(null);
                    break;
                case 1:
                    sb.setNullText("");
                    break;
                default:
                    sb.setNullText(data.consumeString(32));
                    break;
            }
        }

        if (data.consumeBoolean()) {
            sb.append(data.consumeString(32));
        }
        if (data.consumeBoolean()) {
            sb.append(data.consumeAsciiString(32));
        }
        if (data.consumeBoolean()) {
            sb.appendPadding(data.consumeInt(0, 16), (char) (data.consumeByte() & 0xff));
        }

        for (int i = 0; i < 2 && data.remainingBytes() > 0; i++) {
            Object obj;
            switch (data.consumeInt(0, 6)) {
                case 0:
                    obj = null;
                    break;
                case 1:
                    obj = data.consumeString(32);
                    break;
                case 2:
                    obj = data.consumeAsciiString(32);
                    break;
                case 3:
                    obj = Integer.valueOf(data.consumeInt());
                    break;
                case 4:
                    obj = Boolean.valueOf(data.consumeBoolean());
                    break;
                case 5:
                    obj = new StrBuilder(data.consumeString(16));
                    break;
                default:
                    obj = data.consumeBytes(16);
                    break;
            }

            int width = data.consumeBoolean() ? data.consumeInt(1, 64) : data.consumeInt(-8, 64);
            char padChar = (char) (data.consumeByte() & 0xff);
            sb.appendFixedWidthPadRight(obj, width, padChar);
        }

        Object targetObj;
        switch (data.consumeInt(0, 7)) {
            case 0:
                targetObj = null;
                break;
            case 1:
                targetObj = data.consumeString(128);
                break;
            case 2:
                targetObj = data.consumeAsciiString(128);
                break;
            case 3:
                targetObj = Integer.valueOf(data.consumeInt());
                break;
            case 4:
                targetObj = Boolean.valueOf(data.consumeBoolean());
                break;
            case 5:
                targetObj = new StrBuilder(data.consumeString(64));
                break;
            case 6:
                targetObj = data.consumeRemainingAsString();
                break;
            default:
                targetObj = data.consumeRemainingAsBytes();
                break;
        }

        int targetWidth;
        if (data.remainingBytes() > 0) {
            targetWidth = data.consumeBoolean() ? data.consumeInt(1, 256) : data.consumeInt(-16, 256);
        } else {
            targetWidth = 1;
        }
        char targetPad = (char) (data.remainingBytes() > 0 ? (data.consumeByte() & 0xff) : ' ');

        sb.appendFixedWidthPadRight(targetObj, targetWidth, targetPad);
    }
}
```

</details>

---
## [14] ⚙️ harness-attempt · `attempt_006`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [15] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~2,846 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder builder;
        int ctorChoice = data.consumeInt(0, 2);
        if (ctorChoice == 0) {
            builder = new StrBuilder();
        } else if (ctorChoice == 1) {
            builder = new StrBuilder(data.consumeString(64));
        } else {
            builder = new StrBuilder(data.consumeInt(0, 128));
        }

        if (data.consumeBoolean()) {
            if (data.consumeBoolean()) {
                builder.setNullText(null);
            } else {
                builder.setNullText(data.consumeString(32));
            }
        }

        int preOps = data.consumeInt(0, 4);
        for (int i = 0; i < preOps; i++) {
            int op = data.consumeInt(0, 3);
            if (op == 0) {
                builder.append(data.consumeString(64));
            } else if (op == 1) {
                builder.append(data.consumeAsciiString(64));
            } else if (op == 2) {
                builder.append((Object) Integer.valueOf(data.consumeInt()));
            } else {
                builder.append(data.consumeBoolean() ? null : data.consumeString(16));
            }
        }

        int calls = data.consumeInt(1, 4);
        for (int i = 0; i < calls; i++) {
            Object obj;
            int objChoice = data.consumeInt(0, 7);
            if (objChoice == 0) {
                obj = null;
            } else if (objChoice == 1) {
                obj = data.consumeString(256);
            } else if (objChoice == 2) {
                obj = data.consumeAsciiString(256);
            } else if (objChoice == 3) {
                obj = new StringBuffer(data.consumeString(256));
            } else if (objChoice == 4) {
                obj = new StrBuilder(data.consumeString(256));
            } else if (objChoice == 5) {
                obj = Integer.valueOf(data.consumeInt());
            } else if (objChoice == 6) {
                obj = Boolean.valueOf(data.consumeBoolean());
            } else {
                obj = builder;
            }

            String asString = (obj == null) ? builder.getNullText() : obj.toString();
            int width;
            if (asString == null) {
                width = data.consumeBoolean() ? data.consumeInt(1, 64) : data.consumeInt(-16, 64);
            } else if (asString.length() > 1 && data.consumeBoolean()) {
                width = data.consumeInt(1, asString.length() - 1);
            } else {
                width = data.consumeInt(-16, 256);
            }

            char padChar = (char) data.consumeInt(0, 65535);
            builder.appendFixedWidthPadRight(obj, width, padChar);
        }
    }
}
```

</details>

---
## [16] ⚙️ harness-attempt · `attempt_007`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [17] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,551 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder sb;
        switch (data.consumeInt(0, 4)) {
            case 0:
                sb = new StrBuilder();
                break;
            case 1:
                sb = new StrBuilder(data.consumeString(32));
                break;
            case 2:
                sb = new StrBuilder(data.consumeAsciiString(32));
                break;
            case 3:
                sb = new StrBuilder(data.consumeInt(-16, 64));
                break;
            default:
                sb = new StrBuilder(data.consumeRemainingAsString());
                break;
        }

        if (data.consumeBoolean()) {
            sb.setNullText(null);
        } else {
            sb.setNullText(data.consumeString(16));
        }

        if (data.consumeBoolean()) {
            sb.setNewLineText(data.consumeString(8));
        }

        int setupOps = data.consumeInt(0, 6);
        for (int i = 0; i < setupOps; i++) {
            switch (data.consumeInt(0, 7)) {
                case 0:
                    sb.append(data.consumeString(24));
                    break;
                case 1:
                    sb.append(data.consumeAsciiString(24));
                    break;
                case 2:
                    sb.append(data.consumeInt());
                    break;
                case 3:
                    sb.append(data.consumeBoolean());
                    break;
                case 4:
                    sb.appendPadding(data.consumeInt(0, 16), (char) (data.consumeByte() & 0xff));
                    break;
                case 5:
                    sb.appendNewLine();
                    break;
                case 6:
                    if (sb.length() > 0) {
                        int start = data.consumeInt(0, sb.length());
                        int end = data.consumeInt(start, sb.length());
                        sb.delete(start, end);
                    }
                    break;
                default:
                    if (data.consumeBoolean()) {
                        sb.clear();
                    }
                    break;
            }
        }

        Object obj;
        switch (data.consumeInt(0, 6)) {
            case 0:
                obj = null;
                break;
            case 1:
                obj = data.consumeString(64);
                break;
            case 2:
                obj = data.consumeAsciiString(64);
                break;
            case 3:
                obj = Integer.valueOf(data.consumeInt());
                break;
            case 4:
                obj = Boolean.valueOf(data.consumeBoolean());
                break;
            case 5:
                obj = new StringBuilder(data.consumeString(64));
                break;
            default:
                obj = data.consumeRemainingAsBytes();
                break;
        }

        int width;
        switch (data.consumeInt(0, 3)) {
            case 0:
                width = data.consumeInt(-8, 8);
                break;
            case 1:
                width = data.consumeInt();
                break;
            case 2:
                width = sb.length();
                break;
            default:
                width = sb.length() + data.consumeInt(-4, 4);
                break;
        }

        char padChar = (char) (data.consumeByte() & 0xff);

        sb.appendFixedWidthPadRight(obj, width, padChar);

        if (data.remainingBytes() > 0) {
            Object obj2;
            switch (data.consumeInt(0, 4)) {
                case 0:
                    obj2 = null;
                    break;
                case 1:
                    obj2 = data.consumeRemainingAsString();
                    break;
                case 2:
                    obj2 = Long.valueOf(data.consumeInt());
                    break;
                case 3:
                    obj2 = sb.toString();
                    break;
                default:
                    obj2 = new StringBuffer(data.consumeString(32));
                    break;
            }
            int width2 = data.remainingBytes() > 0 ? data.consumeInt() : 0;
            char padChar2 = data.remainingBytes() > 0 ? (char) (data.consumeByte() & 0xff) : '\0';
            sb.appendFixedWidthPadRight(obj2, width2, padChar2);
        }
    }
}
```

</details>

---
## [18] ⚙️ harness-attempt · `attempt_008`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [19] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,401 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder sb;
        switch (data.consumeInt(0, 2)) {
            case 0:
                sb = new StrBuilder();
                break;
            case 1:
                sb = new StrBuilder(data.consumeInt(0, 256));
                break;
            default:
                sb = new StrBuilder(data.consumeBoolean() ? null : data.consumeString(64));
                break;
        }

        switch (data.consumeInt(0, 3)) {
            case 0:
                sb.setNullText(null);
                break;
            case 1:
                sb.setNullText("");
                break;
            case 2:
                sb.setNullText(data.consumeString(16));
                break;
            default:
                sb.setNullText(data.consumeAsciiString(16));
                break;
        }

        if (data.consumeBoolean()) {
            sb.append(data.consumeString(64));
        }
        if (data.consumeBoolean()) {
            sb.append(data.consumeAsciiString(64));
        }
        if (data.consumeBoolean()) {
            sb.appendPadding(data.consumeInt(0, 32), (char) (data.consumeByte() & 0xff));
        }
        if (data.consumeBoolean()) {
            sb.ensureCapacity(data.consumeInt(0, 512));
        }
        if (data.consumeBoolean()) {
            sb.setLength(data.consumeInt(0, 128));
        }

        Object obj;
        switch (data.consumeInt(0, 6)) {
            case 0:
                obj = null;
                break;
            case 1:
                obj = data.consumeString(64);
                break;
            case 2:
                obj = data.consumeAsciiString(64);
                break;
            case 3:
                obj = Integer.valueOf(data.consumeInt());
                break;
            case 4:
                obj = Boolean.valueOf(data.consumeBoolean());
                break;
            case 5:
                obj = new StringBuffer(data.consumeString(64));
                break;
            default:
                obj = data.consumeRemainingAsString();
                break;
        }

        String objString = (obj == null ? sb.getNullText() : obj.toString());
        int width;
        if (objString == null) {
            switch (data.consumeInt(0, 3)) {
                case 0:
                    width = data.consumeInt(1, 64);
                    break;
                case 1:
                    width = 0;
                    break;
                case 2:
                    width = -data.consumeInt(0, 64);
                    break;
                default:
                    width = data.consumeInt();
                    break;
            }
        } else {
            int len = objString.length();
            switch (data.consumeInt(0, 6)) {
                case 0:
                    width = len;
                    break;
                case 1:
                    width = len + 1;
                    break;
                case 2:
                    width = len == 0 ? 0 : len - 1;
                    break;
                case 3:
                    width = 0;
                    break;
                case 4:
                    width = -data.consumeInt(0, 64);
                    break;
                case 5:
                    width = data.consumeInt(1, 64);
                    break;
                default:
                    width = data.consumeInt();
                    break;
            }
        }

        char padChar = (char) (data.consumeByte() & 0xff);
        sb.appendFixedWidthPadRight(obj, width, padChar);

        if (data.remainingBytes() > 0) {
            Object obj2;
            switch (data.consumeInt(0, 4)) {
                case 0:
                    obj2 = null;
                    break;
                case 1:
                    obj2 = data.consumeString(32);
                    break;
                case 2:
                    obj2 = Long.valueOf(data.consumeInt());
                    break;
                case 3:
                    obj2 = new StringBuffer(data.consumeAsciiString(32));
                    break;
                default:
                    obj2 = data.consumeRemainingAsString();
                    break;
            }

            int width2;
            if (data.consumeBoolean()) {
                width2 = data.consumeInt();
            } else {
                String s2 = (obj2 == null ? sb.getNullText() : obj2.toString());
                if (s2 == null) {
                    width2 = data.consumeInt(1, 32);
                } else {
                    switch (data.consumeInt(0, 2)) {
                        case 0:
                            width2 = s2.length();
                            break;
                        case 1:
                            width2 = s2.length() + data.consumeInt(0, 8);
                            break;
                        default:
                            width2 = s2.length() == 0 ? 0 : s2.length() - 1;
                            break;
                    }
                }
            }
            sb.appendFixedWidthPadRight(obj2, width2, (char) (data.consumeByte() & 0xff));
        }
    }
}
```

</details>

---
## [20] ⚙️ harness-attempt · `attempt_009`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [21] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,593 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder builder;
        switch (data.consumeInt(0, 3)) {
            case 0:
                builder = new StrBuilder();
                break;
            case 1:
                builder = new StrBuilder(data.consumeString(64));
                break;
            case 2:
                builder = new StrBuilder(data.consumeInt(-32, 128));
                break;
            default:
                builder = new StrBuilder(data.consumeAsciiString(64));
                break;
        }

        if (data.consumeBoolean()) {
            builder.setNewLineText(data.consumeString(16));
        }
        if (data.consumeBoolean()) {
            if (data.consumeBoolean()) {
                builder.setNullText(null);
            } else {
                builder.setNullText(data.consumeString(32));
            }
        }

        if (data.consumeBoolean()) {
            builder.ensureCapacity(data.consumeInt(-32, 256));
        }
        if (data.consumeBoolean()) {
            builder.minimizeCapacity();
        }
        if (data.consumeBoolean()) {
            builder.setLength(data.consumeInt(0, 64));
        }
        if (data.consumeBoolean()) {
            builder.append(data.consumeString(64));
        }
        if (data.consumeBoolean()) {
            builder.appendPadding(data.consumeInt(-8, 32), (char) (data.consumeByte() & 0xff));
        }
        if (data.consumeBoolean()) {
            builder.appendNewLine();
        }

        Object obj;
        switch (data.consumeInt(0, 8)) {
            case 0:
                obj = null;
                break;
            case 1:
                obj = data.consumeString(128);
                break;
            case 2:
                obj = data.consumeAsciiString(128);
                break;
            case 3:
                obj = Integer.valueOf(data.consumeInt());
                break;
            case 4:
                obj = Long.valueOf(((long) data.consumeInt() << 32) ^ (long) data.consumeInt());
                break;
            case 5:
                obj = Boolean.valueOf(data.consumeBoolean());
                break;
            case 6:
                obj = new StrBuilder(data.consumeString(128));
                break;
            case 7:
                obj = new StringBuffer(data.consumeString(128));
                break;
            default:
                obj = new char[] { (char) (data.consumeByte() & 0xff), (char) (data.consumeByte() & 0xff) };
                break;
        }

        int width;
        switch (data.consumeInt(0, 4)) {
            case 0:
                width = data.consumeInt();
                break;
            case 1:
                width = data.consumeInt(-8, 8);
                break;
            case 2:
                width = data.consumeInt(0, 256);
                break;
            case 3:
                width = 0;
                break;
            default:
                width = data.consumeBoolean() ? Integer.MIN_VALUE : Integer.MAX_VALUE;
                break;
        }

        char padChar = (char) (data.consumeByte() & 0xff);

        builder.appendFixedWidthPadRight(obj, width, padChar);

        if (data.consumeBoolean()) {
            builder.appendFixedWidthPadRight(data.consumeRemainingAsString(), data.consumeInt(-8, 64), (char) (data.consumeByte() & 0xff));
        }
    }
}
```

</details>

---
## [22] ⚙️ harness-attempt · `attempt_010`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: Out of memory (use '-Xmx27646m' to reproduce)

---
## [23] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,334 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder builder;
        int ctorChoice = data.consumeInt(0, 2);
        if (ctorChoice == 0) {
            builder = new StrBuilder();
        } else if (ctorChoice == 1) {
            builder = new StrBuilder(data.consumeInt(0, 256));
        } else {
            builder = new StrBuilder(data.consumeString(64));
        }

        if (data.consumeBoolean()) {
            builder.setNullText(null);
        } else {
            builder.setNullText(data.consumeString(32));
        }

        if (data.consumeBoolean()) {
            builder.append(data.consumeString(64));
        }
        if (data.consumeBoolean()) {
            builder.append(data.consumeAsciiString(64));
        }
        if (data.consumeBoolean()) {
            builder.appendPadding(data.consumeInt(0, 64), (char) (data.consumeByte() & 0xFF));
        }

        int ops = data.consumeInt(1, 6);
        for (int i = 0; i < ops; i++) {
            Object obj;
            switch (data.consumeInt(0, 7)) {
                case 0:
                    obj = null;
                    break;
                case 1:
                    obj = data.consumeString(128);
                    break;
                case 2:
                    obj = data.consumeAsciiString(128);
                    break;
                case 3:
                    obj = Integer.valueOf(data.consumeInt());
                    break;
                case 4:
                    obj = Boolean.valueOf(data.consumeBoolean());
                    break;
                case 5:
                    obj = Byte.valueOf(data.consumeByte());
                    break;
                case 6:
                    obj = new StringBuilder(data.consumeString(128));
                    break;
                default:
                    obj = new StrBuilder(data.consumeString(128));
                    break;
            }

            int width;
            if (data.consumeBoolean()) {
                width = data.consumeInt(-8, 256);
            } else {
                int r = data.remainingBytes();
                width = (r == 0) ? 0 : data.consumeInt(-32, Math.min(4096, r * 64 + 32));
            }

            char padChar = (char) (data.consumeByte() & 0xFF);
            builder.appendFixedWidthPadRight(obj, width, padChar);

            if (data.consumeBoolean()) {
                builder.toString();
            }
            if (data.consumeBoolean()) {
                builder.length();
            }
        }

        if (data.consumeBoolean()) {
            StrBuilder second = new StrBuilder(builder.toString());
            if (data.consumeBoolean()) {
                second.setNullText(null);
            } else {
                second.setNullText(data.consumeString(16));
            }
            second.appendFixedWidthPadRight(
                data.consumeBoolean() ? null : data.consumeRemainingAsString(),
                data.consumeInt(-4, 128),
                (char) (data.consumeByte() & 0xFF)
            );
            second.toString();
        } else {
            builder.toString();
        }
    }
}
```

</details>

---
## [24] ⚙️ harness-attempt · `attempt_011`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [25] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,019 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder sb;
        int ctorMode = data.consumeInt(0, 2);
        if (ctorMode == 0) {
            sb = new StrBuilder();
        } else if (ctorMode == 1) {
            sb = new StrBuilder(data.consumeString(64));
        } else {
            int capacity = data.consumeInt(-32, 256);
            sb = new StrBuilder(capacity);
        }

        if (data.consumeBoolean()) {
            sb.setNullText(data.consumeBoolean() ? null : data.consumeString(32));
        }

        if (data.consumeBoolean()) {
            sb.setNewLineText(data.consumeBoolean() ? null : data.consumeString(16));
        }

        int preOps = data.consumeInt(0, 6);
        for (int i = 0; i < preOps; i++) {
            switch (data.consumeInt(0, 5)) {
                case 0:
                    sb.append(data.consumeString(32));
                    break;
                case 1:
                    sb.append(data.consumeBoolean() ? null : data.consumeAsciiString(32));
                    break;
                case 2:
                    sb.appendPadding(data.consumeInt(0, 32), (char) (data.consumeByte() & 0xff));
                    break;
                case 3:
                    sb.appendSeparator(data.consumeString(8));
                    break;
                case 4:
                    sb.ensureCapacity(data.consumeInt(0, 512));
                    break;
                default:
                    if (data.consumeBoolean()) {
                        sb.clear();
                    } else {
                        sb.minimizeCapacity();
                    }
                    break;
            }
        }

        int calls = data.consumeInt(1, 6);
        for (int i = 0; i < calls; i++) {
            Object obj;
            switch (data.consumeInt(0, 7)) {
                case 0:
                    obj = null;
                    break;
                case 1:
                    obj = data.consumeString(128);
                    break;
                case 2:
                    obj = data.consumeAsciiString(128);
                    break;
                case 3:
                    obj = Integer.valueOf(data.consumeInt());
                    break;
                case 4:
                    obj = Boolean.valueOf(data.consumeBoolean());
                    break;
                case 5:
                    obj = new StringBuilder(data.consumeString(64));
                    break;
                case 6:
                    obj = new StringBuffer(data.consumeAsciiString(64));
                    break;
                default:
                    obj = new StrBuilder(data.consumeString(64));
                    break;
            }

            int rawWidth = data.consumeInt();
            int width;
            if (rawWidth > 8192) {
                width = 8192;
            } else if (rawWidth < -8192) {
                width = -8192;
            } else {
                width = rawWidth;
            }

            char padChar = (char) (data.consumeByte() & 0xff);
            sb.appendFixedWidthPadRight(obj, width, padChar);

            if (data.consumeBoolean()) {
                sb.toString();
            }
            if (data.consumeBoolean() && sb.length() > 0) {
                int start = data.consumeInt(0, sb.length());
                int end = data.consumeInt(start, sb.length());
                sb.substring(start, end);
            }
        }

        if (data.consumeBoolean()) {
            Object finalObj = data.consumeBoolean() ? null : data.consumeRemainingAsString();
            int width = data.remainingBytes() == 0 ? 0 : data.consumeInt(-32, 256);
            char padChar = (char) (data.consumeByte() & 0xff);
            sb.appendFixedWidthPadRight(finalObj, width, padChar);
        }
    }
}
```

</details>

---
## [26] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [27] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~5,355 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder sb;
        int ctorMode = data.consumeInt(0, 2);
        if (ctorMode == 0) {
            sb = new StrBuilder();
        } else if (ctorMode == 1) {
            sb = new StrBuilder(data.consumeString(64));
        } else {
            int capacity = data.consumeInt(-32, 256);
            sb = new StrBuilder(capacity);
        }

        if (data.consumeBoolean()) {
            sb.setNullText(data.consumeBoolean() ? null : data.consumeString(32));
        }

        if (data.consumeBoolean()) {
            sb.setNewLineText(data.consumeBoolean() ? null : data.consumeString(16));
        }

        int preOps = data.consumeInt(0, 6);
        for (int i = 0; i < preOps; i++) {
            switch (data.consumeInt(0, 5)) {
                case 0:
                    sb.append(data.consumeString(32));
                    break;
                case 1:
                    sb.append(data.consumeBoolean() ? null : data.consumeAsciiString(32));
                    break;
                case 2:
                    sb.appendPadding(data.consumeInt(0, 32), (char) (data.consumeByte() & 0xff));
                    break;
                case 3:
                    sb.appendSeparator(data.consumeString(8));
                    break;
                case 4:
                    sb.ensureCapacity(data.consumeInt(0, 512));
                    break;
                default:
                    if (data.consumeBoolean()) {
                        sb.clear();
                    } else {
                        sb.minimizeCapacity();
                    }
                    break;
            }
        }

        int calls = data.consumeInt(1, 6);
        for (int i = 0; i < calls; i++) {
            Object obj;
            switch (data.consumeInt(0, 7)) {
                case 0:
                    obj = null;
                    break;
                case 1:
                    obj = data.consumeString(128);
                    break;
                case 2:
                    obj = data.consumeAsciiString(128);
                    break;
                case 3:
                    obj = Integer.valueOf(data.consumeInt());
                    break;
                case 4:
                    obj = Boolean.valueOf(data.consumeBoolean());
                    break;
                case 5:
                    obj = new StringBuilder(data.consumeString(64));
                    break;
                case 6:
                    obj = new StringBuffer(data.consumeAsciiString(64));
                    break;
                default:
                    obj = new StrBuilder(data.consumeString(64));
                    break;
            }

            int rawWidth = data.consumeInt();
            int width;
            if (rawWidth > 8192) {
                width = 8192;
            } else if (rawWidth < -8192) {
                width = -8192;
            } else {
                width = rawWidth;
            }

            char padChar = (char) (data.consumeByte() & 0xff);
            sb.appendFixedWidthPadRight(obj, width, padChar);

            if (data.consumeBoolean()) {
                sb.toString();
            }
            if (data.consumeBoolean() && sb.length() > 0) {
                int start = data.consumeInt(0, sb.length());
                int end = data.consumeInt(start, sb.length());
                sb.substring(start, end);
            }
        }

        if (data.consumeBoolean()) {
            Object finalObj = data.consumeBoolean() ? null : data.consumeRemainingAsString();
            int width = data.remainingBytes() == 0 ? 0 : data.consumeInt(-32, 256);
            char padChar = (char) (data.consumeByte() & 0xff);
            sb.appendFixedWidthPadRight(finalObj, width, padChar);
        }
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/Lang_59_buggy/fuzz/attempt_012/FuzzHarness.java:39: error: cannot find symbol
                    sb.appendSeparator(data.consumeString(8));
                      ^
  symbol:   method appendSeparator(String)
  location: variable sb of type StrBuilder
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
<details open><summary>▸ Output (~4,264 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder sb;
        int ctorMode = data.consumeInt(0, 2);
        if (ctorMode == 0) {
            sb = new StrBuilder();
        } else if (ctorMode == 1) {
            sb = new StrBuilder(data.consumeString(64));
        } else {
            sb = new StrBuilder(data.consumeInt(-32, 256));
        }

        if (data.consumeBoolean()) {
            sb.setNullText(data.consumeBoolean() ? null : data.consumeString(32));
        }

        if (data.consumeBoolean()) {
            sb.setNewLineText(data.consumeBoolean() ? null : data.consumeString(16));
        }

        int preOps = data.consumeInt(0, 8);
        for (int i = 0; i < preOps; i++) {
            switch (data.consumeInt(0, 6)) {
                case 0:
                    sb.append(data.consumeString(32));
                    break;
                case 1:
                    sb.append(data.consumeBoolean() ? null : data.consumeAsciiString(32));
                    break;
                case 2:
                    sb.appendPadding(data.consumeInt(0, 32), (char) (data.consumeByte() & 0xff));
                    break;
                case 3:
                    sb.appendNewLine();
                    break;
                case 4:
                    sb.ensureCapacity(data.consumeInt(0, 512));
                    break;
                case 5:
                    if (data.consumeBoolean()) {
                        sb.clear();
                    } else {
                        sb.minimizeCapacity();
                    }
                    break;
                default:
                    if (sb.length() > 0) {
                        int start = data.consumeInt(0, sb.length());
                        int end = data.consumeInt(start, sb.length());
                        sb.substring(start, end);
                    }
                    break;
            }
        }

        int calls = data.consumeInt(1, 6);
        for (int i = 0; i < calls; i++) {
            Object obj;
            switch (data.consumeInt(0, 7)) {
                case 0:
                    obj = null;
                    break;
                case 1:
                    obj = data.consumeString(128);
                    break;
                case 2:
                    obj = data.consumeAsciiString(128);
                    break;
                case 3:
                    obj = Integer.valueOf(data.consumeInt());
                    break;
                case 4:
                    obj = Boolean.valueOf(data.consumeBoolean());
                    break;
                case 5:
                    obj = new StringBuilder(data.consumeString(64));
                    break;
                case 6:
                    obj = new StringBuffer(data.consumeAsciiString(64));
                    break;
                default:
                    obj = new StrBuilder(data.consumeString(64));
                    break;
            }

            int rawWidth = data.consumeInt();
            int width;
            if (rawWidth > 8192) {
                width = 8192;
            } else if (rawWidth < -8192) {
                width = -8192;
            } else {
                width = rawWidth;
            }

            char padChar = (char) (data.consumeByte() & 0xff);
            sb.appendFixedWidthPadRight(obj, width, padChar);

            if (data.consumeBoolean()) {
                sb.toString();
            }
            if (data.consumeBoolean() && sb.length() > 0) {
                int start = data.consumeInt(0, sb.length());
                int end = data.consumeInt(start, sb.length());
                sb.substring(start, end);
            }
        }

        if (data.consumeBoolean()) {
            Object finalObj = data.consumeBoolean() ? null : data.consumeRemainingAsString();
            int width = data.remainingBytes() > 0 ? data.consumeInt(-32, 256) : 0;
            char padChar = (char) (data.consumeByte() & 0xff);
            sb.appendFixedWidthPadRight(finalObj, width, padChar);
        }
    }
}
```

</details>

---
## [28] ⚙️ harness-attempt · `attempt_013`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [29] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,370 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder builder;
        if (data.consumeBoolean()) {
            builder = new StrBuilder();
        } else {
            builder = new StrBuilder(data.consumeString(64));
        }

        if (data.consumeBoolean()) {
            builder.setNullText(null);
        } else {
            builder.setNullText(data.consumeString(32));
        }

        if (data.consumeBoolean()) {
            builder.setNewLineText(data.consumeString(16));
        }

        int preOps = data.consumeInt(0, 6);
        for (int i = 0; i < preOps; i++) {
            switch (data.consumeInt(0, 7)) {
                case 0:
                    builder.append(data.consumeString(32));
                    break;
                case 1:
                    builder.append(data.consumeAsciiString(32));
                    break;
                case 2:
                    builder.append(data.consumeInt());
                    break;
                case 3:
                    builder.append(data.consumeBoolean());
                    break;
                case 4:
                    builder.appendPadding(data.consumeInt(0, 16), (char) (data.consumeByte() & 0xFF));
                    break;
                case 5:
                    builder.appendFixedWidthPadLeft(data.consumeString(32), data.consumeInt(-8, 32),
                            (char) (data.consumeByte() & 0xFF));
                    break;
                case 6:
                    if (builder.length() > 0) {
                        int start = data.consumeInt(0, builder.length() - 1);
                        int end = data.consumeInt(start, builder.length());
                        builder.delete(start, end);
                    }
                    break;
                default:
                    if (builder.length() > 0) {
                        int idx = data.consumeInt(0, builder.length() - 1);
                        builder.setCharAt(idx, (char) (data.consumeByte() & 0xFF));
                    }
                    break;
            }
        }

        int mode = data.consumeInt(0, 8);
        Object obj;
        switch (mode) {
            case 0:
                obj = null;
                break;
            case 1:
                obj = data.consumeString(128);
                break;
            case 2:
                obj = data.consumeAsciiString(128);
                break;
            case 3:
                obj = Integer.valueOf(data.consumeInt());
                break;
            case 4:
                obj = Boolean.valueOf(data.consumeBoolean());
                break;
            case 5:
                obj = new StrBuilder(data.consumeString(64));
                break;
            case 6:
                obj = data.consumeBytes(64);
                break;
            case 7:
                obj = builder.toStringBuffer();
                break;
            default:
                obj = builder;
                break;
        }

        int width1 = data.consumeInt();
        char pad1 = (char) (data.consumeByte() & 0xFF);
        builder.appendFixedWidthPadRight(obj, width1, pad1);

        if (data.remainingBytes() > 0) {
            Object obj2;
            switch (data.consumeInt(0, 5)) {
                case 0:
                    obj2 = null;
                    break;
                case 1:
                    obj2 = data.consumeRemainingAsString();
                    break;
                case 2:
                    obj2 = Integer.valueOf(data.consumeInt());
                    break;
                case 3:
                    obj2 = new StrBuilder(data.consumeString(32));
                    break;
                case 4:
                    obj2 = builder.toString();
                    break;
                default:
                    obj2 = data.consumeRemainingAsBytes();
                    break;
            }
            int width2 = data.remainingBytes() > 0 ? data.consumeInt(-16, 64) : 1;
            char pad2 = data.remainingBytes() > 0 ? (char) (data.consumeByte() & 0xFF) : ' ';
            builder.appendFixedWidthPadRight(obj2, width2, pad2);
        }
    }
}
```

</details>

---
## [30] ⚙️ harness-attempt · `attempt_014`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [31] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,776 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder builder;
        switch (data.consumeInt(0, 4)) {
            case 0:
                builder = new StrBuilder();
                break;
            case 1:
                builder = new StrBuilder(data.consumeString(64));
                break;
            case 2:
                builder = new StrBuilder(data.consumeAsciiString(64));
                break;
            case 3:
                builder = new StrBuilder(data.consumeInt(-32, 256));
                break;
            default:
                builder = new StrBuilder(data.consumeRemainingAsString());
                break;
        }

        if (data.consumeBoolean()) {
            builder.setNewLineText(data.consumeBoolean() ? null : data.consumeString(32));
        }

        if (data.consumeBoolean()) {
            builder.setNullText(data.consumeBoolean() ? null : data.consumeString(32));
        }

        if (data.consumeBoolean()) {
            builder.append(data.consumeString(32));
        }
        if (data.consumeBoolean()) {
            builder.append(data.consumeAsciiString(32));
        }
        if (data.consumeBoolean()) {
            builder.append(data.consumeInt());
        }
        if (data.consumeBoolean()) {
            builder.append(data.consumeBoolean());
        }
        if (data.consumeBoolean()) {
            builder.append(new String(data.consumeBytes(32)));
        }

        Object obj;
        switch (data.consumeInt(0, 10)) {
            case 0:
                obj = null;
                break;
            case 1:
                obj = data.consumeString(128);
                break;
            case 2:
                obj = data.consumeAsciiString(128);
                break;
            case 3:
                obj = Integer.valueOf(data.consumeInt());
                break;
            case 4:
                obj = Long.valueOf(data.consumeInt());
                break;
            case 5:
                obj = Boolean.valueOf(data.consumeBoolean());
                break;
            case 6:
                obj = Character.valueOf((char) (data.consumeByte() & 0xff));
                break;
            case 7:
                obj = new String(data.consumeBytes(128));
                break;
            case 8:
                obj = new StringBuffer(data.consumeString(128));
                break;
            case 9:
                obj = new StrBuilder(data.consumeString(128));
                break;
            default:
                obj = data.consumeRemainingAsString();
                break;
        }

        int width;
        switch (data.consumeInt(0, 5)) {
            case 0:
                width = data.consumeInt(-8, 8);
                break;
            case 1:
                width = data.consumeInt(0, 256);
                break;
            case 2:
                width = data.consumeInt(-256, 256);
                break;
            case 3:
                width = 0;
                break;
            case 4:
                width = 1;
                break;
            default:
                width = data.consumeInt();
                break;
        }

        char padChar = (char) (data.consumeByte() & 0xff);

        builder.appendFixedWidthPadRight(obj, width, padChar);

        if (data.consumeBoolean()) {
            Object obj2;
            switch (data.consumeInt(0, 6)) {
                case 0:
                    obj2 = null;
                    break;
                case 1:
                    obj2 = builder.toString();
                    break;
                case 2:
                    obj2 = new StrBuilder(data.consumeAsciiString(64));
                    break;
                case 3:
                    obj2 = Integer.valueOf(data.consumeInt());
                    break;
                case 4:
                    obj2 = new String(data.consumeRemainingAsBytes());
                    break;
                case 5:
                    obj2 = Boolean.valueOf(data.consumeBoolean());
                    break;
                default:
                    obj2 = data.consumeString(64);
                    break;
            }
            builder.appendFixedWidthPadRight(obj2, data.consumeInt(-16, 128), (char) (data.consumeByte() & 0xff));
        }

        if (data.consumeBoolean()) {
            builder.toString();
            builder.length();
            if (builder.length() > 0 && data.consumeBoolean()) {
                builder.charAt(data.consumeInt(0, builder.length() - 1));
            }
        }
    }
}
```

</details>

---
## [32] ⚙️ harness-attempt · `attempt_015`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [33] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~2,872 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder builder;
        int padByte = data.consumeByte();
        char padChar = (char) (padByte & 0xFF);

        int width1 = data.consumeInt();
        int width2 = data.consumeInt(-32, 256);
        int width3 = data.consumeInt(-1, 64);

        String s1 = data.consumeString(64);
        String s2 = data.consumeAsciiString(64);
        String s3 = data.consumeRemainingAsString();

        Object obj1;
        switch (data.consumeInt(0, 5)) {
            case 0:
                obj1 = null;
                break;
            case 1:
                obj1 = s1;
                break;
            case 2:
                obj1 = Integer.valueOf(width1);
                break;
            case 3:
                obj1 = Long.valueOf(((long) width1 << 32) ^ width2);
                break;
            case 4:
                obj1 = new StringBuilder(s2);
                break;
            default:
                obj1 = new StrBuilder(s3);
                break;
        }

        builder = new StrBuilder();
        if (data.consumeBoolean()) {
            builder.setNullText(data.consumeBoolean() ? null : data.consumeString(32));
        }
        if (data.consumeBoolean()) {
            builder.append(data.consumeString(64));
        }
        builder.appendFixedWidthPadRight(obj1, width1, padChar);

        builder = new StrBuilder(data.consumeString(64));
        if (data.consumeBoolean()) {
            builder.setNullText(null);
        } else {
            builder.setNullText(data.consumeAsciiString(32));
        }
        Object obj2;
        switch (data.consumeInt(0, 4)) {
            case 0:
                obj2 = null;
                break;
            case 1:
                obj2 = "";
                break;
            case 2:
                obj2 = s2;
                break;
            case 3:
                obj2 = builder;
                break;
            default:
                obj2 = data.consumeBytes(Math.min(32, Math.max(0, data.remainingBytes())));
                break;
        }
        builder.appendFixedWidthPadRight(obj2, width2, (char) (data.consumeByte() & 0xFF));

        builder = new StrBuilder();
        builder.setNullText(null);
        if (data.consumeBoolean()) {
            builder.append(data.consumeAsciiString(16));
        }
        builder.appendFixedWidthPadRight(null, width3, (char) (data.consumeByte() & 0xFF));

        builder = new StrBuilder();
        builder.setNullText(data.consumeBoolean() ? "" : data.consumeString(16));
        builder.appendFixedWidthPadRight(s3, data.consumeInt(-8, 128), (char) (data.consumeByte() & 0xFF));
    }
}
```

</details>

---
## [34] ⚙️ harness-attempt · `attempt_016`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [35] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,299 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder builder;
        switch (data.consumeInt(0, 4)) {
            case 0:
                builder = new StrBuilder();
                break;
            case 1:
                builder = new StrBuilder(data.consumeString(32));
                break;
            case 2:
                builder = new StrBuilder(data.consumeInt(-16, 128));
                break;
            case 3:
                builder = new StrBuilder(data.consumeAsciiString(32));
                break;
            default:
                builder = new StrBuilder(data.consumeRemainingAsString());
                break;
        }

        if (data.consumeBoolean()) {
            builder.setNewLineText(data.consumeBoolean() ? null : data.consumeString(16));
        }

        if (data.consumeBoolean()) {
            switch (data.consumeInt(0, 3)) {
                case 0:
                    builder.setNullText(null);
                    break;
                case 1:
                    builder.setNullText("");
                    break;
                case 2:
                    builder.setNullText(data.consumeString(16));
                    break;
                default:
                    builder.setNullText(data.consumeAsciiString(16));
                    break;
            }
        }

        if (data.consumeBoolean()) {
            builder.append(data.consumeString(32));
        }
        if (data.consumeBoolean()) {
            builder.append(data.consumeAsciiString(32));
        }
        if (data.consumeBoolean()) {
            builder.append(data.consumeInt());
        }
        if (data.consumeBoolean()) {
            builder.append(data.consumeBoolean());
        }
        if (data.consumeBoolean()) {
            builder.appendPadding(data.consumeInt(-8, 32), (char) (data.consumeByte() & 0xff));
        }

        int iterations = data.consumeInt(1, 8);
        for (int i = 0; i < iterations; i++) {
            Object obj;
            switch (data.consumeInt(0, 8)) {
                case 0:
                    obj = null;
                    break;
                case 1:
                    obj = data.consumeString(64);
                    break;
                case 2:
                    obj = data.consumeAsciiString(64);
                    break;
                case 3:
                    obj = Integer.valueOf(data.consumeInt());
                    break;
                case 4:
                    obj = Long.valueOf(data.consumeInt());
                    break;
                case 5:
                    obj = Character.valueOf((char) (data.consumeByte() & 0xff));
                    break;
                case 6:
                    obj = Boolean.valueOf(data.consumeBoolean());
                    break;
                case 7:
                    obj = new String(data.consumeBytes(64));
                    break;
                default:
                    obj = data.consumeRemainingAsString();
                    break;
            }

            int width;
            switch (data.consumeInt(0, 5)) {
                case 0:
                    width = data.consumeInt(-4, 4);
                    break;
                case 1:
                    width = data.consumeInt(0, 16);
                    break;
                case 2:
                    width = data.consumeInt(16, 128);
                    break;
                case 3:
                    width = data.consumeInt();
                    break;
                case 4:
                    width = builder.length();
                    break;
                default:
                    width = builder.length() + data.consumeInt(-8, 8);
                    break;
            }

            char padChar;
            switch (data.consumeInt(0, 3)) {
                case 0:
                    padChar = (char) (data.consumeByte() & 0xff);
                    break;
                case 1:
                    padChar = ' ';
                    break;
                case 2:
                    padChar = '\0';
                    break;
                default:
                    padChar = (char) data.consumeInt(0, Character.MAX_VALUE);
                    break;
            }

            builder.appendFixedWidthPadRight(obj, width, padChar);

            if (data.consumeBoolean()) {
                builder.length();
                builder.capacity();
                builder.toString();
            }
            if (data.consumeBoolean()) {
                int start = data.consumeInt(0, builder.length());
                int end = data.consumeInt(start, builder.length());
                builder.substring(start, end);
            }
        }

        if (data.consumeBoolean()) {
            Object finalObj = data.consumeBoolean() ? null : data.consumeRemainingAsString();
            int finalWidth = data.consumeBoolean() ? 0 : data.consumeInt(-32, 256);
            char finalPad = (char) (data.consumeByte() & 0xff);
            builder.appendFixedWidthPadRight(finalObj, finalWidth, finalPad);
        }
    }
}
```

</details>

---
## [36] ⚙️ harness-attempt · `attempt_017`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [37] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,099 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder builder;
        if (data.consumeBoolean()) {
            builder = new StrBuilder();
        } else {
            builder = new StrBuilder(data.consumeString(64));
        }

        if (data.consumeBoolean()) {
            builder.setNullText(null);
        } else {
            builder.setNullText(data.consumeString(32));
        }

        if (data.consumeBoolean()) {
            builder.setNewLineText(data.consumeString(16));
        }

        if (data.consumeBoolean()) {
            builder.append(data.consumeString(64));
        }
        if (data.consumeBoolean()) {
            builder.append(data.consumeAsciiString(64));
        }
        if (data.consumeBoolean()) {
            builder.appendPadding(data.consumeInt(0, 32), (char) (data.consumeByte() & 0xff));
        }
        if (data.consumeBoolean()) {
            builder.ensureCapacity(data.consumeInt(0, 256));
        }

        int iterations = data.consumeInt(1, 8);
        for (int i = 0; i < iterations; i++) {
            Object obj;
            switch (data.consumeInt(0, 7)) {
                case 0:
                    obj = null;
                    break;
                case 1:
                    obj = data.consumeString(128);
                    break;
                case 2:
                    obj = data.consumeAsciiString(128);
                    break;
                case 3:
                    obj = new StringBuilder(data.consumeString(128));
                    break;
                case 4:
                    obj = new StrBuilder(data.consumeString(128));
                    break;
                case 5:
                    obj = Integer.valueOf(data.consumeInt());
                    break;
                case 6:
                    obj = Boolean.valueOf(data.consumeBoolean());
                    break;
                default:
                    obj = data.consumeBytes(64);
                    break;
            }

            int width;
            switch (data.consumeInt(0, 5)) {
                case 0:
                    width = data.consumeInt();
                    break;
                case 1:
                    width = data.consumeInt(-4, 4);
                    break;
                case 2:
                    width = data.consumeInt(0, 32);
                    break;
                case 3:
                    width = data.remainingBytes();
                    break;
                case 4:
                    width = builder.length();
                    break;
                default:
                    width = builder.length() + data.consumeInt(-8, 8);
                    break;
            }

            char padChar;
            switch (data.consumeInt(0, 3)) {
                case 0:
                    padChar = (char) (data.consumeByte() & 0xff);
                    break;
                case 1:
                    padChar = '\u0000';
                    break;
                case 2:
                    padChar = ' ';
                    break;
                default:
                    padChar = (char) data.consumeInt(0, 65535);
                    break;
            }

            builder.appendFixedWidthPadRight(obj, width, padChar);

            if (data.consumeBoolean()) {
                builder.toString();
            }
            if (data.consumeBoolean() && builder.length() > 0) {
                builder.charAt(data.consumeInt(0, builder.length() - 1));
            }
        }

        if (data.consumeBoolean()) {
            builder.appendFixedWidthPadRight(null, data.consumeInt(-2, 8), (char) (data.consumeByte() & 0xff));
        }

        if (data.remainingBytes() > 0 && data.consumeBoolean()) {
            builder.appendFixedWidthPadRight(data.consumeRemainingAsString(), data.consumeInt(-2, 16), (char) (data.consumeByte() & 0xff));
        }
    }
}
```

</details>

---
## [38] ⚙️ harness-attempt · `attempt_018`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [39] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,110 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        char padChar = (char) (data.consumeByte() & 0xFF);

        int prefixLen = data.consumeInt(0, 31);
        StrBuilder builder = new StrBuilder(32);
        if (prefixLen > 0) {
            builder.appendPadding(prefixLen, 'P');
        }

        if (data.consumeBoolean()) {
            builder.setNullText(null);
        } else {
            builder.setNullText(data.consumeString(32));
        }

        String base = data.consumeString(128);
        int minLongLen = data.consumeInt(33, 160);
        if (base.length() < minLongLen) {
            StringBuilder sb = new StringBuilder(base);
            while (sb.length() < minLongLen) {
                sb.append((char) ('A' + (sb.length() % 26)));
            }
            base = sb.toString();
        }

        String ascii = data.consumeAsciiString(64);
        String remaining = data.consumeRemainingAsString();

        Object obj;
        switch (data.consumeInt(0, 7)) {
            case 0:
                obj = null;
                break;
            case 1:
                obj = base;
                break;
            case 2:
                obj = ascii;
                break;
            case 3:
                obj = remaining;
                break;
            case 4:
                obj = Integer.valueOf(data.consumeInt());
                break;
            case 5:
                obj = Boolean.valueOf(data.consumeBoolean());
                break;
            case 6:
                obj = new StringBuffer(base);
                break;
            default:
                obj = new StrBuilder(base);
                break;
        }

        int rawWidth = data.consumeInt();
        int width = (rawWidth == Integer.MIN_VALUE) ? Integer.MIN_VALUE : (rawWidth % 4096);

        builder.appendFixedWidthPadRight(obj, width, padChar);

        StrBuilder boundaryBuilder = new StrBuilder(32);
        boundaryBuilder.appendPadding(31, 'X');
        boundaryBuilder.appendFixedWidthPadRight(base, 1, padChar);

        StrBuilder widthVariants = new StrBuilder(32);
        widthVariants.appendPadding(data.consumeInt(0, 31), 'Y');
        widthVariants.appendFixedWidthPadRight(base, 0, padChar);
        widthVariants.appendFixedWidthPadRight(base, 1, padChar);
        widthVariants.appendFixedWidthPadRight(base, Math.max(1, base.length() - 1), padChar);
        widthVariants.appendFixedWidthPadRight(base, base.length(), padChar);
        widthVariants.appendFixedWidthPadRight(base, base.length() + 1, padChar);

        StrBuilder nullCase = new StrBuilder(32);
        if (data.consumeBoolean()) {
            nullCase.setNullText(null);
        } else {
            nullCase.setNullText(data.consumeAsciiString(16));
        }
        nullCase.appendPadding(data.consumeInt(0, 31), 'N');
        nullCase.appendFixedWidthPadRight(null, data.consumeInt(-4, 8), padChar);
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
<details open><summary>▸ Output (~3,748 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder builder;
        if (data.consumeBoolean()) {
            builder = new StrBuilder();
        } else if (data.consumeBoolean()) {
            builder = new StrBuilder(data.consumeString(64));
        } else {
            builder = new StrBuilder(data.consumeInt(0, 128));
        }

        if (data.consumeBoolean()) {
            builder.setNullText(null);
        } else {
            builder.setNullText(data.consumeString(32));
        }

        if (data.consumeBoolean()) {
            builder.append(data.consumeString(64));
        }
        if (data.consumeBoolean()) {
            builder.append(data.consumeAsciiString(64));
        }
        if (data.consumeBoolean()) {
            builder.append(data.consumeInt());
        }

        int iterations = data.remainingBytes() > 0 ? data.consumeInt(1, Math.min(8, data.remainingBytes())) : 1;
        for (int i = 0; i < iterations; i++) {
            Object obj;
            switch (data.consumeInt(0, 8)) {
                case 0:
                    obj = null;
                    break;
                case 1:
                    obj = data.consumeString(128);
                    break;
                case 2:
                    obj = data.consumeAsciiString(128);
                    break;
                case 3:
                    obj = Integer.valueOf(data.consumeInt());
                    break;
                case 4:
                    obj = Long.valueOf((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                    break;
                case 5:
                    obj = Boolean.valueOf(data.consumeBoolean());
                    break;
                case 6:
                    obj = new StringBuilder(data.consumeString(64));
                    break;
                case 7:
                    obj = new StringBuffer(data.consumeAsciiString(64));
                    break;
                default:
                    obj = new StrBuilder(data.consumeString(64));
                    break;
            }

            int width;
            switch (data.consumeInt(0, 5)) {
                case 0:
                    width = data.consumeInt(-16, 16);
                    break;
                case 1:
                    width = data.consumeInt(0, 256);
                    break;
                case 2:
                    width = 0;
                    break;
                case 3:
                    width = 1;
                    break;
                case 4:
                    width = data.consumeInt(-256, -1);
                    break;
                default:
                    width = data.consumeInt();
                    if (width > 4096) {
                        width = 4096;
                    } else if (width < -4096) {
                        width = -4096;
                    }
                    break;
            }

            char padChar;
            if (data.consumeBoolean()) {
                padChar = (char) (data.consumeByte() & 0xff);
            } else {
                String s = data.consumeString(1);
                padChar = s.length() == 0 ? '\0' : s.charAt(0);
            }

            builder.appendFixedWidthPadRight(obj, width, padChar);

            if (data.consumeBoolean()) {
                if (data.consumeBoolean()) {
                    builder.setNullText(null);
                } else {
                    builder.setNullText(data.consumeString(16));
                }
            }
        }
    }
}
```

</details>

---
## [42] ⚙️ harness-attempt · `attempt_020`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [43] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,460 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder builder;
        switch (data.consumeInt(0, 2)) {
            case 0:
                builder = new StrBuilder();
                break;
            case 1:
                builder = new StrBuilder(data.consumeString(64));
                break;
            default:
                builder = new StrBuilder(data.consumeInt(0, 256));
                break;
        }

        if (data.consumeBoolean()) {
            builder.setNullText(data.consumeBoolean() ? null : data.consumeString(32));
        }

        if (data.consumeBoolean()) {
            builder.append(data.consumeString(64));
        }
        if (data.consumeBoolean()) {
            builder.append(data.consumeAsciiString(64));
        }
        if (data.consumeBoolean()) {
            builder.append(data.consumeInt());
        }
        if (data.consumeBoolean()) {
            builder.append(data.consumeBoolean());
        }

        int iterations = data.consumeInt(1, 8);
        for (int i = 0; i < iterations; i++) {
            Object obj;
            switch (data.consumeInt(0, 6)) {
                case 0:
                    obj = null;
                    break;
                case 1:
                    obj = data.consumeString(128);
                    break;
                case 2:
                    obj = data.consumeAsciiString(128);
                    break;
                case 3:
                    obj = Integer.valueOf(data.consumeInt());
                    break;
                case 4:
                    obj = Boolean.valueOf(data.consumeBoolean());
                    break;
                case 5:
                    obj = Byte.valueOf(data.consumeByte());
                    break;
                default:
                    obj = data.consumeRemainingAsString();
                    break;
            }

            int width;
            switch (data.consumeInt(0, 4)) {
                case 0:
                    width = data.consumeInt(-8, 8);
                    break;
                case 1:
                    width = data.consumeInt(0, 64);
                    break;
                case 2:
                    width = data.consumeInt(1, 256);
                    break;
                case 3:
                    width = 0;
                    break;
                default:
                    width = data.remainingBytes();
                    break;
            }

            char padChar;
            switch (data.consumeInt(0, 3)) {
                case 0:
                    padChar = (char) data.consumeInt(0, 65535);
                    break;
                case 1:
                    padChar = (char) (data.consumeByte() & 0xff);
                    break;
                case 2:
                    padChar = ' ';
                    break;
                default:
                    padChar = '\0';
                    break;
            }

            builder.appendFixedWidthPadRight(obj, width, padChar);

            if (data.consumeBoolean()) {
                builder.toString();
            }
            if (data.consumeBoolean() && width >= 0 && width <= builder.length()) {
                builder.substring(0, width);
            }
        }
    }
}
```

</details>

---
## [44] ⚙️ harness-attempt · `attempt_021`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [45] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,494 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder builder;
        if (data.consumeBoolean()) {
            builder = new StrBuilder();
        } else {
            builder = new StrBuilder(data.consumeString(64));
        }

        if (data.consumeBoolean()) {
            builder.setNullText(null);
        } else {
            builder.setNullText(data.consumeString(32));
        }

        if (data.consumeBoolean()) {
            builder.setNewLineText(data.consumeString(16));
        }

        if (data.consumeBoolean()) {
            builder.append(data.consumeString(64));
        }
        if (data.consumeBoolean()) {
            builder.append(data.consumeAsciiString(64));
        }
        if (data.consumeBoolean()) {
            builder.append(data.consumeInt());
        }
        if (data.consumeBoolean()) {
            builder.append(data.consumeBoolean());
        }
        if (data.consumeBoolean()) {
            builder.appendPadding(data.consumeInt(-8, 64), (char) (data.consumeByte() & 0xff));
        }
        if (data.consumeBoolean()) {
            builder.ensureCapacity(data.consumeInt(-16, 256));
        }
        if (data.consumeBoolean() && builder.length() > 0) {
            builder.deleteCharAt(data.consumeInt(0, builder.length() - 1));
        }

        Object obj;
        switch (data.consumeInt(0, 8)) {
            case 0:
                obj = null;
                break;
            case 1:
                obj = data.consumeString(128);
                break;
            case 2:
                obj = data.consumeAsciiString(128);
                break;
            case 3:
                obj = Integer.valueOf(data.consumeInt());
                break;
            case 4:
                obj = Long.valueOf((long) data.consumeInt());
                break;
            case 5:
                obj = Character.valueOf((char) (data.consumeByte() & 0xff));
                break;
            case 6:
                obj = Boolean.valueOf(data.consumeBoolean());
                break;
            case 7:
                obj = data.consumeBytes(64);
                break;
            default:
                obj = new StrBuilder(data.consumeString(64));
                break;
        }

        int width = data.consumeInt();
        char padChar = (char) (data.consumeByte() & 0xff);

        builder.appendFixedWidthPadRight(obj, width, padChar);

        if (data.consumeBoolean()) {
            Object obj2;
            switch (data.consumeInt(0, 5)) {
                case 0:
                    obj2 = null;
                    break;
                case 1:
                    obj2 = data.consumeRemainingAsString();
                    break;
                case 2:
                    obj2 = data.consumeRemainingAsBytes();
                    break;
                case 3:
                    obj2 = Integer.valueOf(data.consumeInt());
                    break;
                case 4:
                    obj2 = new StrBuilder(data.consumeString(32));
                    break;
                default:
                    obj2 = builder.toString();
                    break;
            }
            builder.appendFixedWidthPadRight(obj2, data.consumeInt(-32, 256), (char) (data.consumeByte() & 0xff));
        }
    }
}
```

</details>

---
## [46] ⚙️ harness-attempt · `attempt_022`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [47] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,865 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder builder;
        switch (data.consumeInt(0, 4)) {
            case 0:
                builder = new StrBuilder();
                break;
            case 1:
                builder = new StrBuilder(data.consumeString(64));
                break;
            case 2:
                builder = new StrBuilder(data.consumeInt(-16, 256));
                break;
            case 3:
                builder = new StrBuilder(data.consumeAsciiString(64));
                break;
            default:
                builder = new StrBuilder();
                break;
        }

        if (data.consumeBoolean()) {
            String nullText = data.consumeBoolean() ? null : data.consumeString(32);
            builder.setNullText(nullText);
        }
        if (data.consumeBoolean()) {
            String newLineText = data.consumeBoolean() ? null : data.consumeAsciiString(16);
            builder.setNewLineText(newLineText);
        }

        int prefixOps = data.consumeInt(0, 8);
        for (int i = 0; i < prefixOps; i++) {
            switch (data.consumeInt(0, 11)) {
                case 0:
                    builder.append(data.consumeString(32));
                    break;
                case 1:
                    builder.append(data.consumeAsciiString(32));
                    break;
                case 2:
                    builder.append(data.consumeBoolean());
                    break;
                case 3:
                    builder.append(data.consumeInt());
                    break;
                case 4:
                    builder.appendPadding(data.consumeInt(0, 32), (char) (data.consumeByte() & 0xff));
                    break;
                case 5:
                    builder.appendNewLine();
                    break;
                case 6:
                    builder.clear();
                    break;
                case 7:
                    builder.ensureCapacity(data.consumeInt(-16, 512));
                    break;
                case 8:
                    builder.minimizeCapacity();
                    break;
                case 9:
                    builder.setLength(data.consumeInt(0, 64));
                    break;
                case 10:
                    builder.reverse();
                    break;
                default:
                    builder.trim();
                    break;
            }
        }

        int calls = data.consumeInt(1, 6);
        for (int i = 0; i < calls; i++) {
            Object obj;
            switch (data.consumeInt(0, 6)) {
                case 0:
                    obj = null;
                    break;
                case 1:
                    obj = data.consumeString(128);
                    break;
                case 2:
                    obj = data.consumeAsciiString(128);
                    break;
                case 3:
                    obj = Integer.valueOf(data.consumeInt());
                    break;
                case 4:
                    obj = Boolean.valueOf(data.consumeBoolean());
                    break;
                case 5:
                    obj = new StrBuilder(data.consumeString(64));
                    break;
                default:
                    byte[] bytes = data.consumeBytes(64);
                    obj = new String(bytes);
                    break;
            }

            int width;
            switch (data.consumeInt(0, 5)) {
                case 0:
                    width = data.consumeInt();
                    break;
                case 1:
                    width = data.consumeInt(-4, 4);
                    break;
                case 2:
                    width = data.consumeInt(0, 32);
                    break;
                case 3:
                    width = data.consumeInt(33, 256);
                    break;
                case 4:
                    width = 0;
                    break;
                default:
                    width = -1;
                    break;
            }

            char padChar = (char) (data.consumeByte() & 0xff);

            builder.appendFixedWidthPadRight(obj, width, padChar);

            if (data.consumeBoolean()) {
                builder.length();
                builder.capacity();
                builder.size();
                builder.toString();
                if (builder.length() > 0) {
                    int start = data.consumeInt(0, builder.length() - 1);
                    int end = data.consumeInt(start, builder.length());
                    builder.substring(start, end);
                }
            }
        }
    }
}
```

</details>

---
## [48] ⚙️ harness-attempt · `attempt_023`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [49] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,773 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder sb;
        switch (data.consumeInt(0, 3)) {
            case 0:
                sb = new StrBuilder();
                break;
            case 1:
                sb = new StrBuilder(data.consumeString(32));
                break;
            case 2:
                sb = new StrBuilder(data.consumeInt(-4, 64));
                break;
            default:
                sb = new StrBuilder(data.consumeAsciiString(32));
                break;
        }

        if (data.consumeBoolean()) {
            sb.setNullText(null);
        } else {
            sb.setNullText(data.consumeString(16));
        }

        if (data.consumeBoolean()) {
            sb.append(data.consumeString(32));
        }
        if (data.consumeBoolean()) {
            sb.append(data.consumeAsciiString(32));
        }
        if (data.consumeBoolean()) {
            sb.append(data.consumeInt());
        }

        Object obj1;
        switch (data.consumeInt(0, 7)) {
            case 0:
                obj1 = null;
                break;
            case 1:
                obj1 = data.consumeString(64);
                break;
            case 2:
                obj1 = data.consumeAsciiString(64);
                break;
            case 3:
                obj1 = Integer.valueOf(data.consumeInt());
                break;
            case 4:
                obj1 = Boolean.valueOf(data.consumeBoolean());
                break;
            case 5:
                obj1 = new StrBuilder(data.consumeString(32));
                break;
            case 6:
                obj1 = new StringBuffer(data.consumeAsciiString(32));
                break;
            default:
                obj1 = data.consumeBytes(16);
                break;
        }

        int width1;
        switch (data.consumeInt(0, 5)) {
            case 0:
                width1 = -1;
                break;
            case 1:
                width1 = 0;
                break;
            case 2:
                width1 = 1;
                break;
            case 3:
                width1 = data.consumeInt(-8, 8);
                break;
            case 4:
                width1 = data.consumeInt(0, 64);
                break;
            default:
                width1 = data.consumeInt(-32, 128);
                break;
        }
        char pad1 = (char) (data.consumeByte() & 0xff);
        sb.appendFixedWidthPadRight(obj1, width1, pad1);

        Object obj2;
        switch (data.consumeInt(0, 6)) {
            case 0:
                obj2 = null;
                break;
            case 1:
                obj2 = data.consumeRemainingAsString();
                break;
            case 2:
                obj2 = Long.valueOf(data.consumeInt());
                break;
            case 3:
                obj2 = new StrBuilder(data.consumeAsciiString(24));
                break;
            case 4:
                obj2 = new StringBuffer(data.consumeString(24));
                break;
            case 5:
                obj2 = data.consumeRemainingAsBytes();
                break;
            default:
                obj2 = "";
                break;
        }

        int width2 = data.remainingBytes() == 0 ? data.consumeInt(-2, 4) : data.consumeInt(-16, 96);
        char pad2 = (char) (data.consumeByte() & 0xff);
        sb.appendFixedWidthPadRight(obj2, width2, pad2);

        if (data.consumeBoolean()) {
            sb.appendFixedWidthPadRight(null, data.consumeInt(-4, 16), (char) (data.consumeByte() & 0xff));
        }
    }
}
```

</details>

---
## [50] ⚙️ harness-attempt · `attempt_024`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [51] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,550 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder builder;
        switch (data.consumeInt(0, 3)) {
            case 0:
                builder = new StrBuilder();
                break;
            case 1:
                builder = new StrBuilder(data.consumeString(64));
                break;
            case 2:
                builder = new StrBuilder(data.consumeInt(0, 256));
                break;
            default:
                builder = new StrBuilder(data.consumeAsciiString(64));
                break;
        }

        switch (data.consumeInt(0, 3)) {
            case 0:
                builder.setNullText(null);
                break;
            case 1:
                builder.setNullText("");
                break;
            case 2:
                builder.setNullText(data.consumeString(32));
                break;
            default:
                builder.setNullText(data.consumeAsciiString(32));
                break;
        }

        int rounds = data.consumeInt(1, 6);
        for (int i = 0; i < rounds; i++) {
            Object obj;
            switch (data.consumeInt(0, 6)) {
                case 0:
                    obj = null;
                    break;
                case 1:
                    obj = data.consumeString(128);
                    break;
                case 2:
                    obj = data.consumeAsciiString(128);
                    break;
                case 3:
                    obj = Integer.valueOf(data.consumeInt());
                    break;
                case 4:
                    obj = Boolean.valueOf(data.consumeBoolean());
                    break;
                case 5:
                    obj = new StringBuilder(data.consumeString(64));
                    break;
                default:
                    obj = new String(data.consumeBytes(64));
                    break;
            }

            int width;
            switch (data.consumeInt(0, 4)) {
                case 0:
                    width = data.consumeInt();
                    if (width > 4096) {
                        width = 4096;
                    } else if (width < -4096) {
                        width = -4096;
                    }
                    break;
                case 1:
                    width = data.consumeInt(-8, 8);
                    break;
                case 2:
                    width = data.consumeInt(0, 32);
                    break;
                case 3:
                    width = data.consumeInt(33, 256);
                    break;
                default:
                    width = data.remainingBytes();
                    break;
            }

            char padChar;
            switch (data.consumeInt(0, 3)) {
                case 0:
                    padChar = (char) data.consumeByte();
                    break;
                case 1:
                    padChar = (char) data.consumeInt(0, 65535);
                    break;
                case 2:
                    padChar = data.consumeBoolean() ? ' ' : '0';
                    break;
                default:
                    String s = data.consumeString(1);
                    padChar = s.length() == 0 ? '\0' : s.charAt(0);
                    break;
            }

            builder.appendFixedWidthPadRight(obj, width, padChar);

            if (data.consumeBoolean()) {
                builder.toString();
                builder.length();
                builder.capacity();
            }

            if (data.consumeBoolean()) {
                switch (data.consumeInt(0, 2)) {
                    case 0:
                        builder.setNullText(null);
                        break;
                    case 1:
                        builder.setNullText(data.consumeString(16));
                        break;
                    default:
                        builder.setNullText(data.consumeAsciiString(16));
                        break;
                }
            }
        }

        if (data.consumeBoolean()) {
            Object tailObj = data.consumeBoolean() ? null : data.consumeRemainingAsString();
            int tailWidth = data.remainingBytes();
            char tailPad = (char) (data.consumeBoolean() ? 0 : 'A');
            builder.appendFixedWidthPadRight(tailObj, tailWidth, tailPad);
        }
    }
}
```

</details>

---
## [52] ⚙️ harness-attempt · `attempt_025`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [53] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,233 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder builder;
        switch (data.consumeInt(0, 5)) {
            case 0:
                builder = new StrBuilder();
                break;
            case 1:
                builder = new StrBuilder(data.consumeString(64));
                break;
            case 2:
                builder = new StrBuilder(data.consumeAsciiString(64));
                break;
            case 3:
                builder = new StrBuilder(data.consumeInt(-8, 128));
                break;
            case 4:
                builder = new StrBuilder();
                builder.append(data.consumeString(32));
                builder.append(data.consumeAsciiString(32));
                break;
            default:
                builder = new StrBuilder();
                if (data.consumeBoolean()) {
                    builder.setNullText(data.consumeBoolean() ? null : data.consumeString(16));
                }
                break;
        }

        if (data.consumeBoolean()) {
            builder.setNullText(data.consumeBoolean() ? null : data.consumeString(32));
        }
        if (data.consumeBoolean()) {
            builder.setNewLineText(data.consumeBoolean() ? null : data.consumeAsciiString(16));
        }

        switch (data.consumeInt(0, 4)) {
            case 0:
                builder.append(data.consumeString(64));
                break;
            case 1:
                builder.append(data.consumeAsciiString(64));
                break;
            case 2:
                builder.append(data.consumeBoolean());
                builder.append(data.consumeInt(-1000, 1000));
                break;
            case 3:
                builder.appendPadding(data.consumeInt(0, 32), (char) (data.consumeByte() & 0xff));
                break;
            default:
                if (data.consumeBoolean()) {
                    builder.clear();
                }
                break;
        }

        Object obj;
        switch (data.consumeInt(0, 7)) {
            case 0:
                obj = null;
                break;
            case 1:
                obj = data.consumeString(128);
                break;
            case 2:
                obj = data.consumeAsciiString(128);
                break;
            case 3:
                obj = Integer.valueOf(data.consumeInt());
                break;
            case 4:
                obj = Long.valueOf((long) data.consumeInt());
                break;
            case 5:
                obj = Boolean.valueOf(data.consumeBoolean());
                break;
            case 6:
                obj = new StringBuffer(data.consumeString(64));
                break;
            default:
                obj = new StrBuilder(data.consumeString(64));
                break;
        }

        int width;
        switch (data.consumeInt(0, 6)) {
            case 0:
                width = data.consumeInt();
                break;
            case 1:
                width = data.consumeInt(-8, 8);
                break;
            case 2:
                width = data.consumeInt(0, 32);
                break;
            case 3:
                width = data.consumeInt(1, 256);
                break;
            case 4:
                width = 0;
                break;
            case 5:
                width = -1;
                break;
            default:
                width = builder.length() + data.consumeInt(-4, 16);
                break;
        }

        char padChar = (char) (data.consumeByte() & 0xff);

        if (data.consumeBoolean()) {
            builder.appendFixedWidthPadRight(obj, width, padChar);
        } else {
            int firstWidth = data.consumeInt(-4, 32);
            char firstPad = (char) (data.consumeByte() & 0xff);
            builder.appendFixedWidthPadRight(obj, firstWidth, firstPad);
            builder.appendFixedWidthPadRight(obj, width, padChar);
        }

        if (data.consumeBoolean()) {
            builder.toString();
        }
    }
}
```

</details>

---
## [54] ⚙️ harness-attempt · `attempt_026`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [55] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,647 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder sb;
        if (data.consumeBoolean()) {
            sb = new StrBuilder();
        } else {
            sb = new StrBuilder(data.consumeString(64));
        }

        if (data.consumeBoolean()) {
            sb.setNullText(data.consumeBoolean() ? null : data.consumeString(32));
        }

        if (data.consumeBoolean()) {
            sb.append(data.consumeString(64));
        }
        if (data.consumeBoolean()) {
            sb.append(data.consumeAsciiString(64));
        }
        if (data.consumeBoolean()) {
            sb.appendPadding(data.consumeInt(0, 32), (char) (data.consumeByte() & 0xff));
        }

        Object obj;
        switch (data.consumeInt(0, 7)) {
            case 0:
                obj = null;
                break;
            case 1:
                obj = data.consumeString(512);
                break;
            case 2:
                obj = data.consumeAsciiString(512);
                break;
            case 3:
                obj = Integer.valueOf(data.consumeInt());
                break;
            case 4:
                obj = Long.valueOf(((long) data.consumeInt() << 32) ^ (long) data.consumeInt());
                break;
            case 5:
                obj = Boolean.valueOf(data.consumeBoolean());
                break;
            case 6:
                obj = new StringBuffer(data.consumeString(256));
                break;
            default:
                obj = new StrBuilder(data.consumeString(256));
                break;
        }

        int width;
        switch (data.consumeInt(0, 4)) {
            case 0:
                width = data.consumeInt(-8, 8);
                break;
            case 1:
                width = data.consumeInt(0, 32);
                break;
            case 2:
                width = data.consumeInt(0, 512);
                break;
            case 3:
                String s;
                if (obj == null) {
                    s = sb.getNullText();
                    width = (s == null) ? data.consumeInt(0, 16) : Math.max(1, s.length() - data.consumeInt(0, 4));
                } else {
                    s = obj.toString();
                    width = Math.max(1, s.length() - data.consumeInt(0, 4));
                }
                break;
            default:
                width = data.consumeInt();
                break;
        }

        char padChar = (char) (data.consumeByte() & 0xff);

        sb.appendFixedWidthPadRight(obj, width, padChar);

        if (data.consumeBoolean()) {
            Object obj2;
            if (data.consumeBoolean()) {
                obj2 = data.consumeString(1024);
            } else if (data.consumeBoolean()) {
                obj2 = null;
                sb.setNullText(data.consumeBoolean() ? null : data.consumeString(64));
            } else {
                obj2 = new StrBuilder(data.consumeAsciiString(1024));
            }

            int width2;
            if (obj2 == null) {
                String nullText = sb.getNullText();
                width2 = (nullText == null) ? data.consumeInt(-4, 32)
                        : Math.max(1, nullText.length() - data.consumeInt(0, 8));
            } else {
                width2 = Math.max(1, obj2.toString().length() - data.consumeInt(0, 8));
            }

            sb.appendFixedWidthPadRight(obj2, width2, (char) (data.consumeByte() & 0xff));
        }
    }
}
```

</details>

---
## [56] ⚙️ harness-attempt · `attempt_027`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [57] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,117 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder builder;
        switch (data.consumeInt(0, 2)) {
            case 0:
                builder = new StrBuilder();
                break;
            case 1:
                builder = new StrBuilder(data.consumeInt(0, 128));
                break;
            default:
                builder = new StrBuilder(data.consumeString(64));
                break;
        }

        if (data.consumeBoolean()) {
            builder.append(data.consumeString(64));
        }
        if (data.consumeBoolean()) {
            builder.append(data.consumeAsciiString(64));
        }

        switch (data.consumeInt(0, 3)) {
            case 0:
                builder.setNullText(null);
                break;
            case 1:
                builder.setNullText("");
                break;
            case 2:
                builder.setNullText(data.consumeString(32));
                break;
            default:
                builder.setNullText(data.consumeAsciiString(32));
                break;
        }

        Object obj1;
        switch (data.consumeInt(0, 6)) {
            case 0:
                obj1 = null;
                break;
            case 1:
                obj1 = data.consumeString(64);
                break;
            case 2:
                obj1 = data.consumeAsciiString(64);
                break;
            case 3:
                obj1 = Integer.valueOf(data.consumeInt());
                break;
            case 4:
                obj1 = new StrBuilder(data.consumeString(32));
                break;
            case 5:
                obj1 = new StringBuffer(data.consumeString(32));
                break;
            default:
                obj1 = data.consumeBytes(32);
                break;
        }

        int width1;
        switch (data.consumeInt(0, 7)) {
            case 0:
                width1 = -1;
                break;
            case 1:
                width1 = 0;
                break;
            case 2:
                width1 = 1;
                break;
            case 3:
                width1 = data.consumeInt(-16, 16);
                break;
            case 4:
                width1 = data.consumeInt(0, 64);
                break;
            case 5:
                width1 = data.consumeInt(1, 128);
                break;
            case 6:
                width1 = data.consumeInt(-64, 256);
                break;
            default:
                width1 = data.consumeInt();
                if (width1 > 512) {
                    width1 = 512;
                } else if (width1 < -64) {
                    width1 = -64;
                }
                break;
        }

        char pad1 = (char) (data.consumeByte() & 0xFF);
        builder.appendFixedWidthPadRight(obj1, width1, pad1);

        Object obj2;
        switch (data.consumeInt(0, 4)) {
            case 0:
                obj2 = null;
                break;
            case 1:
                obj2 = builder.toString();
                break;
            case 2:
                obj2 = new StrBuilder(data.consumeAsciiString(24));
                break;
            case 3:
                obj2 = Boolean.valueOf(data.consumeBoolean());
                break;
            default:
                obj2 = data.consumeRemainingAsString();
                break;
        }

        int width2 = data.consumeInt(-32, 256);
        char pad2 = (char) (data.consumeByte() & 0xFF);
        builder.appendFixedWidthPadRight(obj2, width2, pad2);

        if (data.remainingBytes() > 0) {
            Object obj3 = data.consumeBoolean() ? null : data.consumeRemainingAsBytes();
            int width3 = data.remainingBytes() == 0 ? data.consumeInt(-8, 32) : data.consumeInt(-8, 64);
            char pad3 = (char) (data.consumeByte() & 0xFF);
            builder.appendFixedWidthPadRight(obj3, width3, pad3);
        }
    }
}
```

</details>

---
## [58] ⚙️ harness-attempt · `attempt_028`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [59] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,369 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder sb;
        if (data.consumeBoolean()) {
            sb = new StrBuilder();
        } else {
            sb = new StrBuilder(data.consumeString(64));
        }

        if (data.consumeBoolean()) {
            sb.setNewLineText(data.consumeBoolean() ? null : data.consumeString(32));
        }
        if (data.consumeBoolean()) {
            sb.setNullText(data.consumeBoolean() ? null : data.consumeString(32));
        }
        if (data.consumeBoolean()) {
            sb.setLength(data.consumeInt(0, 32));
        }
        if (data.consumeBoolean()) {
            sb.ensureCapacity(data.consumeInt(-16, 256));
        }
        if (data.consumeBoolean()) {
            sb.minimizeCapacity();
        }

        Object obj;
        switch (data.consumeInt(0, 8)) {
            case 0:
                obj = null;
                break;
            case 1:
                obj = data.consumeString(128);
                break;
            case 2:
                obj = data.consumeAsciiString(128);
                break;
            case 3:
                obj = Integer.valueOf(data.consumeInt());
                break;
            case 4:
                obj = Long.valueOf(data.consumeInt());
                break;
            case 5:
                obj = Character.valueOf((char) (data.consumeByte() & 0xff));
                break;
            case 6:
                obj = new StringBuffer(data.consumeString(128));
                break;
            case 7:
                obj = new StrBuilder(data.consumeString(128));
                break;
            default:
                obj = data.consumeRemainingAsString();
                break;
        }

        int width;
        switch (data.consumeInt(0, 5)) {
            case 0:
                width = data.consumeInt(-8, 8);
                break;
            case 1:
                width = data.consumeInt(0, 1);
                break;
            case 2:
                width = data.consumeInt(2, 32);
                break;
            case 3:
                width = data.consumeInt(33, 256);
                break;
            case 4:
                width = data.consumeInt();
                break;
            default:
                width = data.remainingBytes();
                break;
        }

        char padChar;
        if (data.consumeBoolean()) {
            padChar = (char) (data.consumeByte() & 0xff);
        } else {
            String s = data.consumeString(2);
            padChar = s.length() == 0 ? '\0' : s.charAt(0);
        }

        sb.appendFixedWidthPadRight(obj, width, padChar);

        if (data.consumeBoolean()) {
            sb.appendFixedWidthPadRight(null, data.consumeInt(-4, 16), (char) (data.consumeByte() & 0xff));
        }
        if (data.consumeBoolean()) {
            sb.appendFixedWidthPadRight(data.consumeAsciiString(64), data.consumeInt(-4, 64), (char) (data.consumeByte() & 0xff));
        }
        if (data.consumeBoolean()) {
            sb.appendFixedWidthPadRight(new StrBuilder(data.consumeString(64)), data.consumeInt(-4, 64), (char) (data.consumeByte() & 0xff));
        }
    }
}
```

</details>

---
## [60] ⚙️ harness-attempt · `attempt_029`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [61] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,327 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        StrBuilder builder;
        if (data.consumeBoolean()) {
            builder = new StrBuilder();
        } else if (data.consumeBoolean()) {
            builder = new StrBuilder(data.consumeString(128));
        } else {
            builder = new StrBuilder(data.consumeInt(-16, 512));
        }

        if (data.consumeBoolean()) {
            builder.setNewLineText(data.consumeBoolean() ? null : data.consumeString(32));
        }
        if (data.consumeBoolean()) {
            builder.setNullText(data.consumeBoolean() ? null : data.consumeString(32));
        }

        int prefixOps = data.consumeInt(0, 6);
        for (int i = 0; i < prefixOps; i++) {
            switch (data.consumeInt(0, 7)) {
                case 0:
                    builder.append(data.consumeString(64));
                    break;
                case 1:
                    builder.append(data.consumeAsciiString(64));
                    break;
                case 2:
                    builder.append(data.consumeBoolean());
                    break;
                case 3:
                    builder.append(data.consumeInt());
                    break;
                case 4:
                    builder.append(data.consumeByte());
                    break;
                case 5:
                    builder.appendPadding(data.consumeInt(0, 32), (char) (data.consumeByte() & 0xff));
                    break;
                case 6:
                    builder.appendNewLine();
                    break;
                default:
                    builder.ensureCapacity(data.consumeInt(-16, 1024));
                    break;
            }
        }

        Object obj;
        switch (data.consumeInt(0, 7)) {
            case 0:
                obj = null;
                break;
            case 1:
                obj = data.consumeString(256);
                break;
            case 2:
                obj = data.consumeAsciiString(256);
                break;
            case 3:
                obj = Integer.valueOf(data.consumeInt());
                break;
            case 4:
                obj = Long.valueOf((long) data.consumeInt());
                break;
            case 5:
                obj = Boolean.valueOf(data.consumeBoolean());
                break;
            case 6:
                obj = new String(data.consumeBytes(128));
                break;
            default:
                obj = new StrBuilder(data.consumeString(128));
                break;
        }

        int width;
        switch (data.consumeInt(0, 4)) {
            case 0:
                width = data.consumeInt();
                break;
            case 1:
                width = data.consumeInt(-8, 8);
                break;
            case 2:
                width = data.consumeInt(0, 64);
                break;
            case 3:
                width = data.consumeInt(0, 1024);
                break;
            default:
                width = data.remainingBytes();
                break;
        }

        char padChar;
        switch (data.consumeInt(0, 3)) {
            case 0:
                padChar = (char) (data.consumeByte() & 0xff);
                break;
            case 1:
                padChar = (char) data.consumeInt(0, Character.MAX_VALUE);
                break;
            case 2:
                String s = data.consumeString(2);
                padChar = s.length() == 0 ? '\0' : s.charAt(0);
                break;
            default:
                padChar = ' ';
                break;
        }

        if (data.consumeBoolean()) {
            builder.appendFixedWidthPadRight(obj, width, padChar);
        } else {
            builder.appendFixedWidthPadLeft(obj, width, padChar);
            builder.appendFixedWidthPadRight(obj, width, padChar);
        }

        if (data.consumeBoolean()) {
            builder.toString();
            builder.length();
            if (builder.length() > 0 && data.consumeBoolean()) {
                builder.charAt(data.consumeInt(0, builder.length() - 1));
            }
        }
    }
}
```

</details>

---
## [62] ⚙️ harness-attempt · `attempt_030`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [63] ⚙️ coverage · `jazzer coverage dumps`
**output:** **29/29 dump(s) written under /datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/cov; 29 raw fuzz log(s)**
<details><summary>▸ detail (~14,849 chars)</summary>

- detail: {'dumps': [{'harness': 'attempt_001', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_001_compiled.exec', 'exists': True, 'bytes': 246}, {'harness': 'attempt_002', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_002_compiled.exec', 'exists': True, 'bytes': 245}, {'harness': 'attempt_003', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_003_compiled.exec', 'exists': True, 'bytes': 243}, {'harness': 'attempt_004', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_004_compiled.exec', 'exists': True, 'bytes': 244}, {'harness': 'attempt_005', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_005_compiled.exec', 'exists': True, 'bytes': 243}, {'harness': 'attempt_006', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_006_compiled.exec', 'exists': True, 'bytes': 244}, {'harness': 'attempt_007', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_007_compiled.exec', 'exists': True, 'bytes': 244}, {'harness': 'attempt_008', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_008_compiled.exec', 'exists': True, 'bytes': 245}, {'harness': 'attempt_009', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_009_compiled.exec', 'exists': True, 'bytes': 246}, {'harness': 'attempt_010', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_010_compiled.exec', 'exists': True, 'bytes': 244}, {'harness': 'attempt_011', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_011_compiled.exec', 'exists': True, 'bytes': 245}, {'harness': 'attempt_013', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_013_compiled.exec', 'exists': True, 'bytes': 246}, {'harness': 'attempt_014', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_014_compiled.exec', 'exists': True, 'bytes': 245}, {'harness': 'attempt_015', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_015_compiled.exec', 'exists': True, 'bytes': 246}, {'harness': 'attempt_016', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_016_compiled.exec', 'exists': True, 'bytes': 243}, {'harness': 'attempt_017', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_017_compiled.exec', 'exists': True, 'bytes': 247}, {'harness': 'attempt_018', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_018_compiled.exec', 'exists': True, 'bytes': 245}, {'harness': 'attempt_019', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_019_compiled.exec', 'exists': True, 'bytes': 243}, {'harness': 'attempt_020', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_020_compiled.exec', 'exists': True, 'bytes': 244}, {'harness': 'attempt_021', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_021_compiled.exec', 'exists': True, 'bytes': 244}, {'harness': 'attempt_022', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_022_compiled.exec', 'exists': True, 'bytes': 244}, {'harness': 'attempt_023', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_023_compiled.exec', 'exists': True, 'bytes': 245}, {'harness': 'attempt_024', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_024_compiled.exec', 'exists': True, 'bytes': 244}, {'harness': 'attempt_025', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_025_compiled.exec', 'exists': True, 'bytes': 245}, {'harness': 'attempt_026', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_026_compiled.exec', 'exists': True, 'bytes': 245}, {'harness': 'attempt_027', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_027_compiled.exec', 'exists': True, 'bytes': 245}, {'harness': 'attempt_028', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_028_compiled.exec', 'exists': True, 'bytes': 244}, {'harness': 'attempt_029', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_029_compiled.exec', 'exists': True, 'bytes': 244}, {'harness': 'attempt_030', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/cov/attempt_030_compiled.exec', 'exists': True, 'bytes': 245}], 'outputs': ['/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_001_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_002_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_003_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_004_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_005_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_006_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_007_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_008_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_009_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_010_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_011_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_013_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_014_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_015_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_016_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_017_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_018_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_019_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_020_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_021_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_022_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_023_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_024_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_025_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_026_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_027_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_028_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_029_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/fuzz_out/attempt_030_compiled.txt'], 'classpath': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/cov/classpath.json', 'dir': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/cov', 'compiled_attempts': ['attempt_001', 'attempt_002', 'attempt_003', 'attempt_004', 'attempt_005', 'attempt_006', 'attempt_007', 'attempt_008', 'attempt_009', 'attempt_010', 'attempt_011', 'attempt_013', 'attempt_014', 'attempt_015', 'attempt_016', 'attempt_017', 'attempt_018', 'attempt_019', 'attempt_020', 'attempt_021', 'attempt_022', 'attempt_023', 'attempt_024', 'attempt_025', 'attempt_026', 'attempt_027', 'attempt_028', 'attempt_029', 'attempt_030'], 'accepted_attempts': ['attempt_001', 'attempt_002', 'attempt_003', 'attempt_004', 'attempt_005', 'attempt_006', 'attempt_007', 'attempt_008', 'attempt_009', 'attempt_010', 'attempt_011', 'attempt_013', 'attempt_014', 'attempt_015', 'attempt_016', 'attempt_017', 'attempt_018', 'attempt_019', 'attempt_020', 'attempt_021', 'attempt_022', 'attempt_023', 'attempt_024', 'attempt_025', 'attempt_026', 'attempt_027', 'attempt_028', 'attempt_029', 'attempt_030'], 'sources': ['/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_001.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_002.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_003.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_004.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_005.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_006.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_007.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_008.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_009.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_010.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_011.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_013.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_014.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_015.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_016.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_017.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_018.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_019.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_020.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_021.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_022.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_023.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_024.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_025.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_026.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_027.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_028.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_029.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/009_patch1-Lang-59-CapGen_c_rep001/harness_src/attempt_030.java']}

</details>
