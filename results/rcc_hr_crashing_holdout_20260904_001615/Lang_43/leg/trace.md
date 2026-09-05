# Pipeline trace — Lang-43

**Code:** `unknown`

**Patch label:** overfitting  *(the patch under analysis is a known-OVERFIT fix — the pipeline is not told this)*

**Outcome:** overfit MISSED (all harnesses quiet on the patched build). [no_harnesses; 0 harness(es) fuzzed on the patched build; campaign converged=False]

**Patch under analysis:**
```diff
--- /src/java/org/apache/commons/lang/text/ExtendedMessageFormat.java
+++ /src/java/org/apache/commons/lang/text/ExtendedMessageFormat.java
@@ -419,7 +419,7 @@
         int start = pos.getIndex();
         char[] c = pattern.toCharArray();
         if (escapingOn && c[start] == QUOTE) {
-            return appendTo == null ? null : appendTo.append(QUOTE);
+        		pos.setIndex(pos.getIndex() + 1);
         }
         int lastHold = start;
         for (int i = pos.getIndex(); i < pattern.length(); i++) {
```

19 sequential steps — 8 LLM calls, 11 deterministic. Read top to bottom.

<details><summary>Legend — what each step is</summary>

- **failing-tests-found** — the project's own tests that expose the bug (extracted from Defects4J)
- **analysis (TargetAnalyzer)** — parses the patch and builds the code context the model reasons over — the touched method(s), their documented contract, the call-graph reachable set + sibling members (via fuzz-introspector), and imports
- **harness generation (LLM)** — writes a Jazzer fuzzing harness that embeds the surviving rules plus oracles lifted from the failing test
- **harness-attempt** — accept/reject of one generated harness — ACCEPTED = it compiles AND crashes the BUGGY build (with the triggering input shown)

</details>

*Viewing: every ▸ line is a click-to-expand toggle (VS Code markdown preview / GitHub). Prompts are collapsed by default; LLM outputs start expanded. The raw file stays fully greppable.*


---
## [0] ⚙️ failing-tests-found
<details><summary>▸ output — FailureTest(test_class='org.apache.commons.lang.text.ExtendedMessageFormatTest', test_method='testEs (~646 chars)</summary>

**output:** 
```json
[
  "FailureTest(test_class='org.apache.commons.lang.text.ExtendedMessageFormatTest', test_method='testEscapedQuote_LANG_477', source_path='/tmp/d4j/Lang_43_buggy/src/test/org/apache/commons/lang/text/ExtendedMessageFormatTest.java', method_source='    public void testEscapedQuote_LANG_477() {\\n        String pattern = \"it\\'\\'s a {0,lower} \\'test\\'!\";\\n        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);\\n        assertEquals(\"it\\'s a dummy test!\", emf.format(new Object[] {\"DUMMY\"}));\\n    }', exception_type='java.lang.OutOfMemoryError', failure_message=None, support_source=None)"
]
```

</details>

---
## [1] ⚙️ test-context (H1/H2)
**output:** 
```json
[
  {
    "test": "org.apache.commons.lang.text.ExtendedMessageFormatTest::testEscapedQuote_LANG_477",
    "failure_message": "--- org.apache.commons.lang.text.ExtendedMessageFormatTest::testEscapedQuote_LANG_477\njava.lang.OutOfMemoryError: Requested array size exceeds VM limit",
    "support_chars": 315
  }
]
```

---
## [2] ⚙️ analysis (TargetAnalyzer)
<details><summary>▸ output — modified_files": [ (~14,738 chars)</summary>

**output:** 
```json
{
  "modified_files": [
    "src/java/org/apache/commons/lang/text/ExtendedMessageFormat.java"
  ],
  "patch_text": "--- /src/java/org/apache/commons/lang/text/ExtendedMessageFormat.java\n+++ /src/java/org/apache/commons/lang/text/ExtendedMessageFormat.java\n@@ -419,7 +419,7 @@\n         int start = pos.getIndex();\n         char[] c = pattern.toCharArray();\n         if (escapingOn && c[start] == QUOTE) {\n-            return appendTo == null ? null : appendTo.append(QUOTE);\n+        \t\tpos.setIndex(pos.getIndex() + 1);\n         }\n         int lastHold = start;\n         for (int i = pos.getIndex(); i < pattern.length(); i++) {\n",
  "functions": [
    {
      "func_name": "appendQuotedString",
      "func_signature": "private StringBuffer appendQuotedString(String pattern, ParsePosition pos, StringBuffer appendTo, boolean escapingOn)",
      "func_source": "    private StringBuffer appendQuotedString(String pattern, ParsePosition pos,\n            StringBuffer appendTo, boolean escapingOn) {\n        int start = pos.getIndex();\n        char[] c = pattern.toCharArray();\n        if (escapingOn && c[start] == QUOTE) {\n            return appendTo == null ? null : appendTo.append(QUOTE);\n        }\n        int lastHold = start;\n        for (int i = pos.getIndex(); i < pattern.length(); i++) {\n            if (escapingOn && pattern.substring(i).startsWith(ESCAPED_QUOTE)) {\n                appendTo.append(c, lastHold, pos.getIndex() - lastHold).append(\n                        QUOTE);\n                pos.setIndex(i + ESCAPED_QUOTE.length());\n                lastHold = pos.getIndex();\n                continue;\n            }\n            switch (c[pos.getIndex()]) {\n            case QUOTE:\n                next(pos);\n                return appendTo == null ? null : appendTo.append(c, lastHold,\n                        pos.getIndex() - lastHold);\n            default:\n                next(pos);\n            }\n        }\n        throw new IllegalArgumentException(\n                \"Unterminated quoted string at position \" + start);\n    }",
      "func_class": "ExtendedMessageFormat",
      "func_class_fq": "org.apache.commons.lang.text.ExtendedMessageFormat",
      "func_param_types": [
        "String",
        "ParsePosition",
        "StringBuffer",
        "boolean"
      ],
      "fi_name": "[org.apache.commons.lang.text.ExtendedMessageFormat].appendQuotedString(String,java.text.ParsePosition,StringBuffer,boolean)",
      "overload_types": [
        [
          "String",
          "ParsePosition",
          "StringBuffer",
          "boolean"
        ]
      ],
      "xrefs": [
        "public final void applyPattern(String pattern) {\n        if (registry == null) {\n            super.applyPattern(pattern);\n            toPattern = super.toPattern();\n            return;\n        }\n        ArrayList foundFormats = new ArrayList();\n        ArrayList foundDescriptions = new ArrayList();\n        StringBuffer stripCustom = new StringBuffer(pattern.length());\n\n        ParsePosition pos = new ParsePosition(0);\n        char[] c = pattern.toCharArray();\n        int fmtCount = 0;\n        while (pos.getIndex() < pattern.length()) {\n            switch (c[pos.getIndex()]) {\n            case QUOTE:\n                appendQuotedString(pattern, pos, stripCustom, true);\n                break;\n            case START_FE:\n                fmtCount++;\n                seekNonWs(pattern, pos);\n                int start = pos.getIndex();\n                int index = readArgumentIndex(pattern, next(pos));\n                stripCustom.append(START_FE).append(index);\n                seekNonWs(pattern, pos);\n                Format format = null;\n                String formatDescription = null;\n                if (c[pos.getIndex()] == START_FMT) {\n                    formatDescription = parseFormatDescription(pattern,\n                            next(pos));\n                    format = getFormat(formatDescription);\n                    if (format == null) {\n                        stripCustom.append(START_FMT).append(formatDescription);\n                    }\n                }\n                foundFormats.add(format);\n                foundDescriptions.add(format == null ? null : formatDescription);\n                Validate.isTrue(foundFormats.size() == fmtCount);\n                Validate.isTrue(foundDescriptions.size() == fmtCount);\n                if (c[pos.getIndex()] != END_FE) {\n                    throw new IllegalArgumentException(\n                            \"Unreadable format element at position \" + start);\n                }\n                // fall through\n            default:\n                stripCustom.append(c[pos.getIndex()]);\n                next(pos);\n            }\n        }\n        super.applyPattern(stripCustom.toString());\n        toPattern = insertFormats(super.toPattern(), foundDescriptions);\n        if (containsElements(foundFormats)) {\n            Format[] origFormats = getFormats();\n            // only loop over what we know we have, as MessageFormat on Java 1.3 \n            // seems to provide an extra format element:\n            int i = 0;\n            for (Iterator it = foundFormats.iterator(); it.hasNext(); i++) {\n                Format f = (Format) it.next();\n                if (f != null) {\n                    origFormats[i] = f;\n                }\n            }\n            super.setFormats(origFormats);\n        }\n    }",
        "private String insertFormats(String pattern, ArrayList customPatterns) {\n        if (!containsElements(customPatterns)) {\n            return pattern;\n        }\n        StringBuffer sb = new StringBuffer(pattern.length() * 2);\n        ParsePosition pos = new ParsePosition(0);\n        int fe = -1;\n        int depth = 0;\n        while (pos.getIndex() < pattern.length()) {\n            char c = pattern.charAt(pos.getIndex());\n            switch (c) {\n            case QUOTE:\n                appendQuotedString(pattern, pos, sb, false);\n                break;\n            case START_FE:\n                depth++;\n                if (depth == 1) {\n                    fe++;\n                    sb.append(START_FE).append(\n                            readArgumentIndex(pattern, next(pos)));\n                    String customPattern = (String) customPatterns.get(fe);\n                    if (customPattern != null) {\n                        sb.append(START_FMT).append(customPattern);\n                    }\n                }\n                break;\n            case END_FE:\n                depth--;\n                //fall through:\n            default:\n                sb.append(c);\n                next(pos);\n            }\n        }\n        return sb.toString();\n    }"
      ],
      "reachable": [
        "[java.text.ParsePosition].getIndex()",
        "[String].toCharArray()",
        "[StringBuffer].append(char)",
        "[String].length()",
        "[String].substring(org.apache.commons.lang.text.ExtendedMessageFormat)",
        "[org.apache.commons.lang.text.ExtendedMessageFormat].startsWith(String)",
        "[StringBuffer].append(org.apache.commons.lang.text.ExtendedMessageFormat,org.apache.commons.lang.text.ExtendedMessageFormat,org.apache.commons.lang.text.ExtendedMessageFormat)",
        "[org.apache.commons.lang.text.ExtendedMessageFormat].append(char)",
        "[java.text.ParsePosition].setIndex(org.apache.commons.lang.text.ExtendedMessageFormat)",
        "[org.apache.commons.lang.text.ExtendedMessageFormat].next(java.text.ParsePosition)",
        "[IllegalArgumentException].<init>(String)",
        "[java.text.ParsePosition].setIndex(int)",
        "[org.apache.commons.lang.text.StrBuilder].append(Object)",
        "[org.apache.commons.lang.text.StrBuilder].startsWith(String)"
      ],
      "related_callees": [
        {
          "name": "toCharArray",
          "source_file": "StrBuilder.java",
          "signature": "public char[] toCharArray()",
          "source": "    public char[] toCharArray() {\n        if (size == 0) {\n            return ArrayUtils.EMPTY_CHAR_ARRAY;\n        }\n        char chars[] = new char[size];\n        System.arraycopy(buffer, 0, chars, 0, size);\n        return chars;\n    }",
          "is_abstract": false,
          "impls": [
            [
              "StrBuilder.java",
              "    public char[] toCharArray() {\n        if (size == 0) {\n            return ArrayUtils.EMPTY_CHAR_ARRAY;\n        }\n        char chars[] = new char[size];\n        System.arraycopy(buffer, 0, chars, 0, size);\n        return chars;\n    }"
            ],
            [
              "StrBuilder.java",
              "    public char[] toCharArray(int startIndex, int endIndex) {\n        endIndex = validateRange(startIndex, endIndex);\n        int len = endIndex - startIndex;\n        if (len == 0) {\n            return ArrayUtils.EMPTY_CHAR_ARRAY;\n        }\n        char chars[] = new char[len];\n        System.arraycopy(buffer, startIndex, chars, 0, len);\n        return chars;\n    }"
            ]
          ]
        },
        {
          "name": "append",
          "source_file": "StrBuilder.java",
          "signature": "public StrBuilder append(Object obj)",
          "source": "    public StrBuilder append(Object obj) {\n        if (obj == null) {\n            return appendNull();\n        } \n        return append(obj.toString());        \n    }",
          "is_abstract": false,
          "impls": [
            [
              "StrBuilder.java",
              "    public StrBuilder append(Object obj) {\n        if (obj == null) {\n            return appendNull();\n        } \n        return append(obj.toString());        \n    }"
            ],
            [
              "StrBuilder.java",
              "    public StrBuilder append(String str) {\n        if (str == null) {\n            return appendNull();\n        }\n        int strLen = str.length();\n        if (strLen > 0) {\n            int len = length();\n            ensureCapacity(len + strLen);\n            str.getChars(0, strLen, buffer, len);\n            size += strLen;\n        }\n        return this;\n    }"
            ],
            [
              "StrBuilder.java",
              "    public StrBuilder append(String str, int startIndex, int length) {\n        if (str == null) {\n            return appendNull();\n        }\n        if (startIndex < 0 || startIndex > str.length()) {\n            throw new StringIndexOutOfBoundsException(\"startIndex must be valid\");\n        }\n        if (length < 0 || (startIndex + length) > str.length()) {\n            throw new StringIndexOutOfBoundsException(\"length must be valid\");\n        }\n        if (length > 0) {\n            int len = length();\n            ensureCapacity(len + length);\n            str.getChars(startIndex, startIndex + length, buffer, len);\n            size += length;\n        }\n        return this;\n    }"
            ],
            [
              "StrBuilder.java",
              "    public StrBuilder append(StringBuffer str) {\n        if (str == null) {\n            return appendNull();\n        }\n        int strLen = str.length();\n        if (strLen > 0) {\n            int len = length();\n            ensureCapacity(len + strLen);\n            str.getChars(0, strLen, buffer, len);\n            size += strLen;\n        }\n        return this;\n    }"
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
          "name": "substring",
          "source_file": "StrBuilder.java",
          "signature": "public String substring(int start)",
          "source": "    public String substring(int start) {\n        return substring(start, size);\n    }",
          "is_abstract": false,
          "impls": [
            [
              "StrBuilder.java",
              "    public String substring(int start) {\n        return substring(start, size);\n    }"
            ],
            [
              "StrBuilder.java",
              "    public String substring(int startIndex, int endIndex) {\n        endIndex = validateRange(startIndex, endIndex);\n        return new String(buffer, startIndex, endIndex - startIndex);\n    }"
            ]
          ]
        },
        {
          "name": "startsWith",
          "source_file": "StrBuilder.java",
          "signature": "public boolean startsWith(String str)",
          "source": "    public boolean startsWith(String str) {\n        if (str == null) {\n            return false;\n        }\n        int len = str.length();\n        if (len == 0) {\n            return true;\n        }\n        if (len > size) {\n            return false;\n        }\n        for (int i = 0; i < len; i++) {\n            if (buffer[i] != str.charAt(i)) {\n                return false;\n            }\n        }\n        return true;\n    }",
          "is_abstract": false,
          "impls": []
        },
        {
          "name": "next",
          "source_file": "ExtendedMessageFormat.java",
          "signature": "private ParsePosition next(ParsePosition pos)",
          "source": "    private ParsePosition next(ParsePosition pos) {\n        pos.setIndex(pos.getIndex() + 1);\n        return pos;\n    }",
          "is_abstract": false,
          "impls": [
            [
              "ExtendedMessageFormat.java",
              "    private ParsePosition next(ParsePosition pos) {\n        pos.setIndex(pos.getIndex() + 1);\n        return pos;\n    }"
            ],
            [
              "StrTokenizer.java",
              "    public Object next() {\n        if (hasNext()) {\n            return tokens[tokenPos++];\n        }\n        throw new NoSuchElementException();\n    }"
            ]
          ]
        }
      ],
      "field_siblings": []
    }
  ],
  "package": "org.apache.commons.lang.text",
  "root_cause_reachable": [
    "ExtendedMessageFormat.startsWith",
    "ExtendedMessageFormat.append",
    "ExtendedMessageFormat.next",
    "StrBuilder.append",
    "StrBuilder.startsWith"
  ],
  "neighbourhood_notes": [],
  "source_imports": [
    "import java.text.Format;",
    "import java.text.MessageFormat;",
    "import java.text.ParsePosition;",
    "import java.util.ArrayList;",
    "import java.util.Collection;",
    "import java.util.Iterator;",
    "import java.util.Locale;",
    "import java.util.Map;",
    "import org.apache.commons.lang.Validate;"
  ]
}
```

</details>

---
## [3] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~31,874 chars)</summary>

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

Codebase: `Lang_43_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/java/org/apache/commons/lang/text/ExtendedMessageFormat.java
+++ /src/java/org/apache/commons/lang/text/ExtendedMessageFormat.java
@@ -419,7 +419,7 @@
         int start = pos.getIndex();
         char[] c = pattern.toCharArray();
         if (escapingOn && c[start] == QUOTE) {
-            return appendTo == null ? null : appendTo.append(QUOTE);
+        		pos.setIndex(pos.getIndex() + 1);
         }
         int lastHold = start;
         for (int i = pos.getIndex(); i < pattern.length(); i++) {

</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import java.text.Format;
import java.text.MessageFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.lang.Validate;
</source_imports>

Function `appendQuotedString`:
<signature>
private StringBuffer appendQuotedString(String pattern, ParsePosition pos, StringBuffer appendTo, boolean escapingOn)
</signature>
<code>
    private StringBuffer appendQuotedString(String pattern, ParsePosition pos,
            StringBuffer appendTo, boolean escapingOn) {
        int start = pos.getIndex();
        char[] c = pattern.toCharArray();
        if (escapingOn && c[start] == QUOTE) {
            return appendTo == null ? null : appendTo.append(QUOTE);
        }
        int lastHold = start;
        for (int i = pos.getIndex(); i < pattern.length(); i++) {
            if (escapingOn && pattern.substring(i).startsWith(ESCAPED_QUOTE)) {
                appendTo.append(c, lastHold, pos.getIndex() - lastHold).append(
                        QUOTE);
                pos.setIndex(i + ESCAPED_QUOTE.length());
                lastHold = pos.getIndex();
                continue;
            }
            switch (c[pos.getIndex()]) {
            case QUOTE:
                next(pos);
                return appendTo == null ? null : appendTo.append(c, lastHold,
                        pos.getIndex() - lastHold);
            default:
                next(pos);
            }
        }
        throw new IllegalArgumentException(
                "Unterminated quoted string at position " + start);
    }
</code>
Call-site examples (use these as a guide for constructing the target call):
<xref>
public final void applyPattern(String pattern) {
        if (registry == null) {
            super.applyPattern(pattern);
            toPattern = super.toPattern();
            return;
        }
        ArrayList foundFormats = new ArrayList();
        ArrayList foundDescriptions = new ArrayList();
        StringBuffer stripCustom = new StringBuffer(pattern.length());

        ParsePosition pos = new ParsePosition(0);
        char[] c = pattern.toCharArray();
        int fmtCount = 0;
        while (pos.getIndex() < pattern.length()) {
            switch (c[pos.getIndex()]) {
            case QUOTE:
                appendQuotedString(pattern, pos, stripCustom, true);
                break;
            case START_FE:
                fmtCount++;
                seekNonWs(pattern, pos);
                int start = pos.getIndex();
                int index = readArgumentIndex(pattern, next(pos));
                stripCustom.append(START_FE).append(index);
                seekNonWs(pattern, pos);
                Format format = null;
                String formatDescription = null;
                if (c[pos.getIndex()] == START_FMT) {
                    formatDescription = parseFormatDescription(pattern,
                            next(pos));
                    format = getFormat(formatDescription);
                    if (format == null) {
                        stripCustom.append(START_FMT).append(formatDescription);
                    }
                }
                foundFormats.add(format);
                foundDescriptions.add(format == null ? null : formatDescription);
                Validate.isTrue(foundFormats.size() == fmtCount);
                Validate.isTrue(foundDescriptions.size() == fmtCount);
                if (c[pos.getIndex()] != END_FE) {
                    throw new IllegalArgumentException(
                            "Unreadable format element at position " + start);
                }
                // fall through
            default:
                stripCustom.append(c[pos.getIndex()]);
                next(pos);
            }
        }
        super.applyPattern(stripCustom.toString());
        toPattern = insertFormats(super.toPattern(), foundDescriptions);
        if (containsElements(foundFormats)) {
            Format[] origFormats = getFormats();
            // only loop over what we know we have, as MessageFormat on Java 1.3 
            // seems to provide an extra format element:
            int i = 0;
            for (Iterator it = foundFormats.iterator(); it.hasNext(); i++) {
                Format f = (Format) it.next();
                if (f != null) {
                    origFormats[i] = f;
                }
            }
            super.setFormats(origFormats);
        }
    }
</xref>
<xref>
private String insertFormats(String pattern, ArrayList customPatterns) {
        if (!containsElements(customPatterns)) {
            return pattern;
        }
        StringBuffer sb = new StringBuffer(pattern.length() * 2);
        ParsePosition pos = new ParsePosition(0);
        int fe = -1;
        int depth = 0;
        while (pos.getIndex() < pattern.length()) {
            char c = pattern.charAt(pos.getIndex());
            switch (c) {
            case QUOTE:
                appendQuotedString(pattern, pos, sb, false);
                break;
            case START_FE:
                depth++;
                if (depth == 1) {
                    fe++;
                    sb.append(START_FE).append(
                            readArgumentIndex(pattern, next(pos)));
                    String customPattern = (String) customPatterns.get(fe);
                    if (customPattern != null) {
                        sb.append(START_FMT).append(customPattern);
                    }
                }
                break;
            case END_FE:
                depth--;
                //fall through:
            default:
                sb.append(c);
                next(pos);
            }
        }
        return sb.toString();
    }
</xref>
Methods called by `appendQuotedString` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="toCharArray" from="StrBuilder.java">
<signature>
public char[] toCharArray()
</signature>
<code>
    public char[] toCharArray() {
        if (size == 0) {
            return ArrayUtils.EMPTY_CHAR_ARRAY;
        }
        char chars[] = new char[size];
        System.arraycopy(buffer, 0, chars, 0, size);
        return chars;
    }
</code>
<implementation in="StrBuilder.java">
    public char[] toCharArray() {
        if (size == 0) {
            return ArrayUtils.EMPTY_CHAR_ARRAY;
        }
        char chars[] = new char[size];
        System.arraycopy(buffer, 0, chars, 0, size);
        return chars;
    }
</implementation>
<implementation in="StrBuilder.java">
    public char[] toCharArray(int startIndex, int endIndex) {
        endIndex = validateRange(startIndex, endIndex);
        int len = endIndex - startIndex;
        if (len == 0) {
            return ArrayUtils.EMPTY_CHAR_ARRAY;
        }
        char chars[] = new char[len];
        System.arraycopy(buffer, startIndex, chars, 0, len);
        return chars;
    }
</implementation>
</callee>
<callee name="append" from="StrBuilder.java">
<signature>
public StrBuilder append(Object obj)
</signature>
<code>
    public StrBuilder append(Object obj) {
        if (obj == null) {
            return appendNull();
        } 
        return append(obj.toString());        
    }
</code>
<implementation in="StrBuilder.java">
    public StrBuilder append(Object obj) {
        if (obj == null) {
            return appendNull();
        } 
        return append(obj.toString());        
    }
</implementation>
<implementation in="StrBuilder.java">
    public StrBuilder append(String str) {
        if (str == null) {
            return appendNull();
        }
        int strLen = str.length();
        if (strLen > 0) {
            int len = length();
            ensureCapacity(len + strLen);
            str.getChars(0, strLen, buffer, len);
            size += strLen;
        }
        return this;
    }
</implementation>
<implementation in="StrBuilder.java">
    public StrBuilder append(String str, int startIndex, int length) {
        if (str == null) {
            return appendNull();
        }
        if (startIndex < 0 || startIndex > str.length()) {
            throw new StringIndexOutOfBoundsException("startIndex must be valid");
        }
        if (length < 0 || (startIndex + length) > str.length()) {
            throw new StringIndexOutOfBoundsException("length must be valid");
        }
        if (length > 0) {
            int len = length();
            ensureCapacity(len + length);
            str.getChars(startIndex, startIndex + length, buffer, len);
            size += length;
        }
        return this;
    }
</implementation>
<implementation in="StrBuilder.java">
    public StrBuilder append(StringBuffer str) {
        if (str == null) {
            return appendNull();
        }
        int strLen = str.length();
        if (strLen > 0) {
            int len = length();
            ensureCapacity(len + strLen);
            str.getChars(0, strLen, buffer, len);
            size += strLen;
        }
        return this;
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
<callee name="substring" from="StrBuilder.java">
<signature>
public String substring(int start)
</signature>
<code>
    public String substring(int start) {
        return substring(start, size);
    }
</code>
<implementation in="StrBuilder.java">
    public String substring(int start) {
        return substring(start, size);
    }
</implementation>
<implementation in="StrBuilder.java">
    public String substring(int startIndex, int endIndex) {
        endIndex = validateRange(startIndex, endIndex);
        return new String(buffer, startIndex, endIndex - startIndex);
    }
</implementation>
</callee>
<callee name="startsWith" from="StrBuilder.java">
<signature>
public boolean startsWith(String str)
</signature>
<code>
    public boolean startsWith(String str) {
        if (str == null) {
            return false;
        }
        int len = str.length();
        if (len == 0) {
            return true;
        }
        if (len > size) {
            return false;
        }
        for (int i = 0; i < len; i++) {
            if (buffer[i] != str.charAt(i)) {
                return false;
            }
        }
        return true;
    }
</code>
</callee>
<callee name="next" from="ExtendedMessageFormat.java">
<signature>
private ParsePosition next(ParsePosition pos)
</signature>
<code>
    private ParsePosition next(ParsePosition pos) {
        pos.setIndex(pos.getIndex() + 1);
        return pos;
    }
</code>
<implementation in="ExtendedMessageFormat.java">
    private ParsePosition next(ParsePosition pos) {
        pos.setIndex(pos.getIndex() + 1);
        return pos;
    }
</implementation>
<implementation in="StrTokenizer.java">
    public Object next() {
        if (hasNext()) {
            return tokens[tokenPos++];
        }
        throw new NoSuchElementException();
    }
</implementation>
</callee>

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `ExtendedMessageFormat`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.OutOfMemoryError (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `appendQuotedString` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: java.lang.OutOfMemoryError
message: Requested array size exceeds VM limit
thrown_at: org.apache.commons.lang.text.ExtendedMessageFormat.appendQuotedString(ExtendedMessageFormat.java:422)
</ground_truth_crash>
<failing_test class="org.apache.commons.lang.text.ExtendedMessageFormatTest" method="testEscapedQuote_LANG_477">
    public void testEscapedQuote_LANG_477() {
        String pattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        assertEquals("it's a dummy test!", emf.format(new Object[] {"DUMMY"}));
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.lang.text.ExtendedMessageFormatTest::testEscapedQuote_LANG_477
java.lang.OutOfMemoryError: Requested array size exceeds VM limit
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.
The test method depends on the following from its test class — setUp, helper methods, constants, fixture files. Replicate this environment EXACTLY (register the same files, wire the same providers, use the same constants); do NOT improvise setup the test performs differently, and DROP any check whose setup you cannot replicate:
<test_support class="org.apache.commons.lang.text.ExtendedMessageFormatTest">
// --- setUp() (test setup) ---
    protected void setUp() throws Exception {
        super.setUp();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
    }

// --- class fields/constants the test uses ---
private Map registry = new HashMap();
</test_support>

STATE YOU CAN READ (public no-argument readers — capture them BEFORE and AFTER a call documented as read-only or non-mutating; an unexplained change is a hidden-state violation): toPattern

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- ExtendedMessageFormat.startsWith
- ExtendedMessageFormat.append
- ExtendedMessageFormat.next
- StrBuilder.append
- StrBuilder.startsWith
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
<details open><summary>▸ Output (~5,460 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    private static final Map EMPTY_REGISTRY = new HashMap();

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        tryDirect("it''s a {0,lower} 'test'!", false);

        String prefix = sanitize(data.consumeAsciiString(24));
        String middle = sanitize(data.consumeAsciiString(24));
        String quoted = sanitize(data.consumeAsciiString(24));
        String suffix = sanitize(data.consumeAsciiString(24));

        if (prefix.length() == 0) {
            prefix = "p";
        }
        if (quoted.length() == 0) {
            quoted = "q";
        }

        String pattern1 = prefix + "''" + middle + " {0} '" + quoted + "'" + suffix;
        tryDirect(pattern1, true);

        String pattern2 = "'" + quoted + "' " + prefix + "''" + middle + " {0}" + suffix;
        tryDirect(pattern2, true);

        String pattern3 = prefix + " {0} " + middle + "''" + suffix;
        tryDirect(pattern3, true);

        String pattern4 = "''" + prefix + " {0} '" + quoted + "'";
        tryDirect(pattern4, true);
    }

    private static void tryDirect(String pattern, boolean doOracle) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, EMPTY_REGISTRY);

            if (doOracle) {
                /*
                 * Contract/oracle:
                 * For patterns using only standard MessageFormat elements ({0}) and quoting,
                 * ExtendedMessageFormat with a non-null but empty registry must behave like
                 * MessageFormat, because there are no custom formats to apply. Also, a fresh
                 * object built from emf.toPattern() must preserve that canonical pattern.
                 * A throw-deleting patch in appendQuotedString can silently skip quote handling
                 * or fail to advance ParsePosition correctly, which would break either the
                 * formatted output or the toPattern round-trip.
                 */
                String canonical = emf.toPattern();
                String arg = "AbC";
                String lhs;
                String rhs;
                String canonical2;
                try {
                    lhs = emf.format(new Object[] { arg });
                    rhs = new MessageFormat(pattern).format(new Object[] { arg });
                    canonical2 = new ExtendedMessageFormat(canonical, EMPTY_REGISTRY).toPattern();
                } catch (Throwable t) {
                    if (isValidation(t)) {
                        return;
                    }
                    if (isRootCause(t)) {
                        rethrowUnchecked(t);
                    }
                    return;
                }

                if (!lhs.equals(rhs)) {
                    throw new RuntimeException("[oracle:fmt-eq] metamorphic violation: ExtendedMessageFormat and MessageFormat disagree for standard pattern input=" + pattern + " lhs=" + lhs + " rhs=" + rhs);
                }
                if (!canonical.equals(canonical2)) {
                    throw new RuntimeException("[oracle:topattern-rt] metamorphic violation: toPattern round-trip changed canonical form input=" + pattern + " lhs=" + canonical + " rhs=" + canonical2);
                }
            }
        } catch (Throwable t) {
            if (isValidation(t)) {
                return;
            }
            if (isRootCause(t)) {
                rethrowUnchecked(t);
            }
        }
    }

    private static boolean isValidation(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String n = t.getClass().getName();
        return n.startsWith("org.apache.commons.lang") && n.contains("Exception");
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError) && !(t instanceof RuntimeException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            String cls = st[i].getClassName();
            String m = st[i].getMethodName();
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cls)) {
                if ("appendQuotedString".equals(m) || "startsWith".equals(m) || "append".equals(m) || "next".equals(m)) {
                    return true;
                }
            }
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)) {
                if ("append".equals(m) || "startsWith".equals(m)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void rethrowUnchecked(Throwable t) {
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw (RuntimeException) t;
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\'' && c != '{' && c != '}') {
                b.append(c);
            }
        }
        return b.toString();
    }
}
```

</details>

---
## [4] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [5] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~8,402 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    private static final Map EMPTY_REGISTRY = new HashMap();

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        tryDirect("it''s a {0,lower} 'test'!", false);

        String prefix = sanitize(data.consumeAsciiString(24));
        String middle = sanitize(data.consumeAsciiString(24));
        String quoted = sanitize(data.consumeAsciiString(24));
        String suffix = sanitize(data.consumeAsciiString(24));

        if (prefix.length() == 0) {
            prefix = "p";
        }
        if (quoted.length() == 0) {
            quoted = "q";
        }

        String pattern1 = prefix + "''" + middle + " {0} '" + quoted + "'" + suffix;
        tryDirect(pattern1, true);

        String pattern2 = "'" + quoted + "' " + prefix + "''" + middle + " {0}" + suffix;
        tryDirect(pattern2, true);

        String pattern3 = prefix + " {0} " + middle + "''" + suffix;
        tryDirect(pattern3, true);

        String pattern4 = "''" + prefix + " {0} '" + quoted + "'";
        tryDirect(pattern4, true);
    }

    private static void tryDirect(String pattern, boolean doOracle) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, EMPTY_REGISTRY);

            if (doOracle) {
                /*
                 * Contract/oracle:
                 * For patterns using only standard MessageFormat elements ({0}) and quoting,
                 * ExtendedMessageFormat with a non-null but empty registry must behave like
                 * MessageFormat, because there are no custom formats to apply. Also, a fresh
                 * object built from emf.toPattern() must preserve that canonical pattern.
                 * A throw-deleting patch in appendQuotedString can silently skip quote handling
                 * or fail to advance ParsePosition correctly, which would break either the
                 * formatted output or the toPattern round-trip.
                 */
                String canonical = emf.toPattern();
                String arg = "AbC";
                String lhs;
                String rhs;
                String canonical2;
                try {
                    lhs = emf.format(new Object[] { arg });
                    rhs = new MessageFormat(pattern).format(new Object[] { arg });
                    canonical2 = new ExtendedMessageFormat(canonical, EMPTY_REGISTRY).toPattern();
                } catch (Throwable t) {
                    if (isValidation(t)) {
                        return;
                    }
                    if (isRootCause(t)) {
                        rethrowUnchecked(t);
                    }
                    return;
                }

                if (!lhs.equals(rhs)) {
                    throw new RuntimeException("[oracle:fmt-eq] metamorphic violation: ExtendedMessageFormat and MessageFormat disagree for standard pattern input=" + pattern + " lhs=" + lhs + " rhs=" + rhs);
                }
                if (!canonical.equals(canonical2)) {
                    throw new RuntimeException("[oracle:topattern-rt] metamorphic violation: toPattern round-trip changed canonical form input=" + pattern + " lhs=" + canonical + " rhs=" + canonical2);
                }
            }
        } catch (Throwable t) {
            if (isValidation(t)) {
                return;
            }
            if (isRootCause(t)) {
                rethrowUnchecked(t);
            }
        }
    }

    private static boolean isValidation(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String n = t.getClass().getName();
        return n.startsWith("org.apache.commons.lang") && n.contains("Exception");
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError) && !(t instanceof RuntimeException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            String cls = st[i].getClassName();
            String m = st[i].getMethodName();
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cls)) {
                if ("appendQuotedString".equals(m) || "startsWith".equals(m) || "append".equals(m) || "next".equals(m)) {
                    return true;
                }
            }
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)) {
                if ("append".equals(m) || "startsWith".equals(m)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void rethrowUnchecked(Throwable t) {
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw (RuntimeException) t;
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\'' && c != '{' && c != '}') {
                b.append(c);
            }
        }
        return b.toString();
    }
}
```
**[user]**
```
That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generated never drove execution into the changed code, or always took a path that does not fault.

The patch under analysis is below. The buggy version is the code BEFORE this patch is applied (the '+' lines are the FIXED code; the buggy code has the '-' lines instead):
--- /src/java/org/apache/commons/lang/text/ExtendedMessageFormat.java
+++ /src/java/org/apache/commons/lang/text/ExtendedMessageFormat.java
@@ -419,7 +419,7 @@
         int start = pos.getIndex();
         char[] c = pattern.toCharArray();
         if (escapingOn && c[start] == QUOTE) {
-            return appendTo == null ? null : appendTo.append(QUOTE);
+        		pos.setIndex(pos.getIndex() + 1);
         }
         int lastHold = start;
         for (int i = pos.getIndex(); i < pattern.length(); i++) {


Your last harness did NOT reach any project code on the buggy version that we could observe — it likely threw or returned before getting into the changed method. First priority: construct an input that actually calls the patched method with arguments that get past its initial validation, so execution reaches the changed lines at all.

To make the next harness crash the BUGGY version, work through this:

  1. From the diff, identify the exact behavioural difference between buggy and fixed code — which inputs are handled differently. The crash you want exists only on the buggy side, so target inputs that hit that difference. Do not assume the bug is any particular shape (missing bounds check, wrong branch, off-by-one, null handling, etc.) — read the diff and let it tell you.
  2. Choose input that reaches the changed code AND drives it into the faulting state. Use the FuzzedDataProvider to produce values in the range that exercises the difference, not arbitrary values that are likely rejected early.
  3. Do NOT rely on an exception that BOTH versions throw (e.g. validation that exists in buggy and fixed alike). That fires on the patched code too and so cannot distinguish them. You want a fault that the buggy code reaches and the fixed code prevents.
  4. CRITICAL — let the crash escape. Do NOT wrap the call in a try/catch that swallows the throwable you want Jazzer to report. Catch ONLY checked exceptions the signature forces you to handle, and rethrow them as RuntimeException; let every unchecked exception propagate out of fuzzerTestOneInput so Jazzer can see it.

Return the full corrected FuzzHarness.java — raw Java only, no fences, public class FuzzHarness, entrypoint exactly
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
```

</details>
<details open><summary>▸ Output (~2,697 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("lower", null);
        registry.put("upper", null);

        String anchor = "it''s a {0,lower} 'test'!";

        ExtendedMessageFormat emf = new ExtendedMessageFormat(anchor, registry);

        /*
         * Contract asserted:
         * With no actual custom Format instance resolved from the registry, the pattern
         * should still parse and behave like standard MessageFormat for the same quoted
         * text and argument structure. A patch that merely suppresses the crash but skips
         * quote-processing/state updates would violate this observable formatting result.
         */
        String lhs = emf.format(new Object[] { "DUMMY" });
        String rhs = new MessageFormat("it''s a {0} 'test'!").format(new Object[] { "DUMMY" });
        if (!lhs.equals(rhs)) {
            throw new RuntimeException("[oracle:anchor-format] metamorphic violation: input=" + anchor + " lhs=" + lhs + " rhs=" + rhs);
        }

        String pre = safe(data.consumeAsciiString(16));
        String mid = safe(data.consumeAsciiString(16));
        String quoted = safe(data.consumeAsciiString(16));
        String post = safe(data.consumeAsciiString(16));
        String arg = safe(data.consumeAsciiString(12));
        if (pre.length() == 0) pre = "a";
        if (quoted.length() == 0) quoted = "q";
        if (arg.length() == 0) arg = "x";

        String fuzzPattern = pre + "''" + mid + " {0,lower} '" + quoted + "'" + post;
        ExtendedMessageFormat fuzzEmf = new ExtendedMessageFormat(fuzzPattern, registry);
        String fuzzLhs = fuzzEmf.format(new Object[] { arg });
        String fuzzRhs = new MessageFormat(pre + "''" + mid + " {0} '" + quoted + "'" + post).format(new Object[] { arg });
        if (!fuzzLhs.equals(fuzzRhs)) {
            throw new RuntimeException("[oracle:fuzz-format] metamorphic violation: input=" + fuzzPattern + " lhs=" + fuzzLhs + " rhs=" + fuzzRhs);
        }
    }

    private static String safe(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '{' && c != '}' && c != '\'') {
                out.append(c);
            }
        }
        return out.toString();
    }
}
```

</details>

---
## [6] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [7] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~3,013 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
- *[assistant] message: identical to step [5] — not reprinted*
- *[user] message: identical to step [5] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("lower", null);
        registry.put("upper", null);

        String anchor = "it''s a {0,lower} 'test'!";

        ExtendedMessageFormat emf = new ExtendedMessageFormat(anchor, registry);

        /*
         * Contract asserted:
         * With no actual custom Format instance resolved from the registry, the pattern
         * should still parse and behave like standard MessageFormat for the same quoted
         * text and argument structure. A patch that merely suppresses the crash but skips
         * quote-processing/state updates would violate this observable formatting result.
         */
        String lhs = emf.format(new Object[] { "DUMMY" });
        String rhs = new MessageFormat("it''s a {0} 'test'!").format(new Object[] { "DUMMY" });
        if (!lhs.equals(rhs)) {
            throw new RuntimeException("[oracle:anchor-format] metamorphic violation: input=" + anchor + " lhs=" + lhs + " rhs=" + rhs);
        }

        String pre = safe(data.consumeAsciiString(16));
        String mid = safe(data.consumeAsciiString(16));
        String quoted = safe(data.consumeAsciiString(16));
        String post = safe(data.consumeAsciiString(16));
        String arg = safe(data.consumeAsciiString(12));
        if (pre.length() == 0) pre = "a";
        if (quoted.length() == 0) quoted = "q";
        if (arg.length() == 0) arg = "x";

        String fuzzPattern = pre + "''" + mid + " {0,lower} '" + quoted + "'" + post;
        ExtendedMessageFormat fuzzEmf = new ExtendedMessageFormat(fuzzPattern, registry);
        String fuzzLhs = fuzzEmf.format(new Object[] { arg });
        String fuzzRhs = new MessageFormat(pre + "''" + mid + " {0} '" + quoted + "'" + post).format(new Object[] { arg });
        if (!fuzzLhs.equals(fuzzRhs)) {
            throw new RuntimeException("[oracle:fuzz-format] metamorphic violation: input=" + fuzzPattern + " lhs=" + fuzzLhs + " rhs=" + fuzzRhs);
        }
    }

    private static String safe(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '{' && c != '}' && c != '\'') {
                out.append(c);
            }
        }
        return out.toString();
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~4,076 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        String anchorPattern = "it''s a {0} 'test'!";
        ExtendedMessageFormat anchorEmf = new ExtendedMessageFormat(anchorPattern, registry);
        String anchorActual = anchorEmf.format(new Object[] { "DUMMY" });
        String anchorExpected = new MessageFormat(anchorPattern).format(new Object[] { "DUMMY" });
        if (!anchorExpected.equals(anchorActual)) {
            throw new RuntimeException("[oracle:anchor-format] metamorphic violation: input=" + anchorPattern + " lhs=" + anchorActual + " rhs=" + anchorExpected);
        }
        String anchorCanonical = anchorEmf.toPattern();
        String anchorCanonical2 = new ExtendedMessageFormat(anchorCanonical, registry).toPattern();
        if (!anchorCanonical.equals(anchorCanonical2)) {
            throw new RuntimeException("[oracle:anchor-roundtrip] metamorphic violation: input=" + anchorPattern + " lhs=" + anchorCanonical + " rhs=" + anchorCanonical2);
        }

        String pre = clean(data.consumeAsciiString(16));
        String mid = clean(data.consumeAsciiString(16));
        String quoted = clean(data.consumeAsciiString(16));
        String post = clean(data.consumeAsciiString(16));
        String arg = clean(data.consumeAsciiString(16));

        if (pre.length() == 0) {
            pre = "a";
        }
        if (mid.length() == 0) {
            mid = "b";
        }
        if (quoted.length() == 0) {
            quoted = "q";
        }
        if (arg.length() == 0) {
            arg = "x";
        }

        String pattern;
        switch (data.consumeInt(0, 3)) {
            case 0:
                pattern = pre + "''" + mid + " {0} '" + quoted + "'" + post;
                break;
            case 1:
                pattern = pre + " {0} " + mid + "''" + quoted + post;
                break;
            case 2:
                pattern = "''" + pre + " {0} '" + quoted + "'" + post;
                break;
            default:
                pattern = pre + "''" + mid + " {0}";
                break;
        }

        /*
         * Contract asserted:
         * For patterns using only standard MessageFormat syntax ({0}) and valid quoting,
         * ExtendedMessageFormat with a non-null empty registry must preserve MessageFormat
         * behavior. Also, toPattern() is the object's canonical pattern; constructing a fresh
         * ExtendedMessageFormat from that canonical form must yield the same canonical form.
         * A patch that merely avoids the buggy loop by skipping quote-state advancement or by
         * dropping quote content would violate one of these observable properties.
         */
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String actual = emf.format(new Object[] { arg });
        String expected = new MessageFormat(pattern).format(new Object[] { arg });
        if (!expected.equals(actual)) {
            throw new RuntimeException("[oracle:fmt-eq] metamorphic violation: input=" + pattern + " lhs=" + actual + " rhs=" + expected);
        }

        String canonical = emf.toPattern();
        String canonicalRoundTrip = new ExtendedMessageFormat(canonical, registry).toPattern();
        if (!canonical.equals(canonicalRoundTrip)) {
            throw new RuntimeException("[oracle:topattern-rt] metamorphic violation: input=" + pattern + " lhs=" + canonical + " rhs=" + canonicalRoundTrip);
        }
    }

    private static String clean(String s) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\'' && c != '{' && c != '}') {
                out.append(c);
            }
        }
        return out.toString();
    }
}
```

</details>

---
## [8] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [9] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (8 message(s), ~4,511 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
- *[assistant] message: identical to step [5] — not reprinted*
- *[user] message: identical to step [5] — not reprinted*
- *[assistant] message: identical to step [7] — not reprinted*
- *[user] message: identical to step [5] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        String anchorPattern = "it''s a {0} 'test'!";
        ExtendedMessageFormat anchorEmf = new ExtendedMessageFormat(anchorPattern, registry);
        String anchorActual = anchorEmf.format(new Object[] { "DUMMY" });
        String anchorExpected = new MessageFormat(anchorPattern).format(new Object[] { "DUMMY" });
        if (!anchorExpected.equals(anchorActual)) {
            throw new RuntimeException("[oracle:anchor-format] metamorphic violation: input=" + anchorPattern + " lhs=" + anchorActual + " rhs=" + anchorExpected);
        }
        String anchorCanonical = anchorEmf.toPattern();
        String anchorCanonical2 = new ExtendedMessageFormat(anchorCanonical, registry).toPattern();
        if (!anchorCanonical.equals(anchorCanonical2)) {
            throw new RuntimeException("[oracle:anchor-roundtrip] metamorphic violation: input=" + anchorPattern + " lhs=" + anchorCanonical + " rhs=" + anchorCanonical2);
        }

        String pre = clean(data.consumeAsciiString(16));
        String mid = clean(data.consumeAsciiString(16));
        String quoted = clean(data.consumeAsciiString(16));
        String post = clean(data.consumeAsciiString(16));
        String arg = clean(data.consumeAsciiString(16));

        if (pre.length() == 0) {
            pre = "a";
        }
        if (mid.length() == 0) {
            mid = "b";
        }
        if (quoted.length() == 0) {
            quoted = "q";
        }
        if (arg.length() == 0) {
            arg = "x";
        }

        String pattern;
        switch (data.consumeInt(0, 3)) {
            case 0:
                pattern = pre + "''" + mid + " {0} '" + quoted + "'" + post;
                break;
            case 1:
                pattern = pre + " {0} " + mid + "''" + quoted + post;
                break;
            case 2:
                pattern = "''" + pre + " {0} '" + quoted + "'" + post;
                break;
            default:
                pattern = pre + "''" + mid + " {0}";
                break;
        }

        /*
         * Contract asserted:
         * For patterns using only standard MessageFormat syntax ({0}) and valid quoting,
         * ExtendedMessageFormat with a non-null empty registry must preserve MessageFormat
         * behavior. Also, toPattern() is the object's canonical pattern; constructing a fresh
         * ExtendedMessageFormat from that canonical form must yield the same canonical form.
         * A patch that merely avoids the buggy loop by skipping quote-state advancement or by
         * dropping quote content would violate one of these observable properties.
         */
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String actual = emf.format(new Object[] { arg });
        String expected = new MessageFormat(pattern).format(new Object[] { arg });
        if (!expected.equals(actual)) {
            throw new RuntimeException("[oracle:fmt-eq] metamorphic violation: input=" + pattern + " lhs=" + actual + " rhs=" + expected);
        }

        String canonical = emf.toPattern();
        String canonicalRoundTrip = new ExtendedMessageFormat(canonical, registry).toPattern();
        if (!canonical.equals(canonicalRoundTrip)) {
            throw new RuntimeException("[oracle:topattern-rt] metamorphic violation: input=" + pattern + " lhs=" + canonical + " rhs=" + canonicalRoundTrip);
        }
    }

    private static String clean(String s) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\'' && c != '{' && c != '}') {
                out.append(c);
            }
        }
        return out.toString();
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~5,448 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.FieldPosition;
import java.text.Format;
import java.text.MessageFormat;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, registry);

        /*
         * Contract asserted:
         * The regression test documents that this valid pattern must format to
         * "it's a dummy test!" with the lower-case custom format. A patch that merely
         * suppresses the buggy loop but mishandles quote advancement would change the
         * observable formatted result or the canonical pattern round-trip.
         */
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: input=" + anchorPattern + " out=" + anchorOut);
        }
        String anchorCanon = anchor.toPattern();
        String anchorCanon2 = new ExtendedMessageFormat(anchorCanon, registry).toPattern();
        if (!anchorCanon.equals(anchorCanon2)) {
            throw new RuntimeException("[oracle:anchor-topattern] metamorphic violation: input=" + anchorPattern + " lhs=" + anchorCanon + " rhs=" + anchorCanon2);
        }

        String pre = clean(data.consumeAsciiString(12));
        String mid = clean(data.consumeAsciiString(12));
        String quoted = clean(data.consumeAsciiString(12));
        String post = clean(data.consumeAsciiString(12));
        String arg = clean(data.consumeAsciiString(12));
        boolean lower = data.consumeBoolean();

        if (pre.length() == 0) {
            pre = "a";
        }
        if (mid.length() == 0) {
            mid = "b";
        }
        if (quoted.length() == 0) {
            quoted = "q";
        }
        if (arg.length() == 0) {
            arg = "XyZ";
        }

        String formatName = lower ? "lower" : "upper";
        String pattern;
        switch (data.consumeInt(0, 3)) {
            case 0:
                pattern = pre + "''" + mid + " {0," + formatName + "} '" + quoted + "'" + post;
                break;
            case 1:
                pattern = "''" + pre + " {0," + formatName + "} '" + quoted + "'" + post;
                break;
            case 2:
                pattern = pre + " {0," + formatName + "} " + mid + "''" + post;
                break;
            default:
                pattern = pre + "''" + mid + " {0," + formatName + "}";
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String actual = emf.format(new Object[] { arg });

        String transformed = lower ? arg.toLowerCase() : arg.toUpperCase();
        String baselinePattern = pattern.replace("," + formatName, "");
        String expected = new MessageFormat(baselinePattern).format(new Object[] { transformed });
        if (!expected.equals(actual)) {
            throw new RuntimeException("[oracle:format-equivalence] metamorphic violation: input=" + pattern + " lhs=" + actual + " rhs=" + expected);
        }

        String canon = emf.toPattern();
        String canonRoundTrip = new ExtendedMessageFormat(canon, registry).toPattern();
        if (!canon.equals(canonRoundTrip)) {
            throw new RuntimeException("[oracle:topattern-roundtrip] metamorphic violation: input=" + pattern + " lhs=" + canon + " rhs=" + canonRoundTrip);
        }
    }

    private static String clean(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\'' && c != '{' && c != '}') {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new CaseFormat(locale, true);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new CaseFormat(locale, false);
        }
    }

    private static final class CaseFormat extends Format {
        private final Locale locale;
        private final boolean lower;

        CaseFormat(Locale locale, boolean lower) {
            this.locale = locale == null ? Locale.getDefault() : locale;
            this.lower = lower;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(lower ? s.toLowerCase(locale) : s.toUpperCase(locale));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source;
        }
    }
}
```

</details>

---
## [10] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [11] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~31,748 chars, 1 new)</summary>

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

Codebase: `Lang_43_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/java/org/apache/commons/lang/text/ExtendedMessageFormat.java
+++ /src/java/org/apache/commons/lang/text/ExtendedMessageFormat.java
@@ -419,7 +419,7 @@
         int start = pos.getIndex();
         char[] c = pattern.toCharArray();
         if (escapingOn && c[start] == QUOTE) {
-            return appendTo == null ? null : appendTo.append(QUOTE);
+        		pos.setIndex(pos.getIndex() + 1);
         }
         int lastHold = start;
         for (int i = pos.getIndex(); i < pattern.length(); i++) {

</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import java.text.Format;
import java.text.MessageFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.lang.Validate;
</source_imports>

Function `appendQuotedString`:
<signature>
private StringBuffer appendQuotedString(String pattern, ParsePosition pos, StringBuffer appendTo, boolean escapingOn)
</signature>
<code>
    private StringBuffer appendQuotedString(String pattern, ParsePosition pos,
            StringBuffer appendTo, boolean escapingOn) {
        int start = pos.getIndex();
        char[] c = pattern.toCharArray();
        if (escapingOn && c[start] == QUOTE) {
            return appendTo == null ? null : appendTo.append(QUOTE);
        }
        int lastHold = start;
        for (int i = pos.getIndex(); i < pattern.length(); i++) {
            if (escapingOn && pattern.substring(i).startsWith(ESCAPED_QUOTE)) {
                appendTo.append(c, lastHold, pos.getIndex() - lastHold).append(
                        QUOTE);
                pos.setIndex(i + ESCAPED_QUOTE.length());
                lastHold = pos.getIndex();
                continue;
            }
            switch (c[pos.getIndex()]) {
            case QUOTE:
                next(pos);
                return appendTo == null ? null : appendTo.append(c, lastHold,
                        pos.getIndex() - lastHold);
            default:
                next(pos);
            }
        }
        throw new IllegalArgumentException(
                "Unterminated quoted string at position " + start);
    }
</code>
Call-site examples (use these as a guide for constructing the target call):
<xref>
public final void applyPattern(String pattern) {
        if (registry == null) {
            super.applyPattern(pattern);
            toPattern = super.toPattern();
            return;
        }
        ArrayList foundFormats = new ArrayList();
        ArrayList foundDescriptions = new ArrayList();
        StringBuffer stripCustom = new StringBuffer(pattern.length());

        ParsePosition pos = new ParsePosition(0);
        char[] c = pattern.toCharArray();
        int fmtCount = 0;
        while (pos.getIndex() < pattern.length()) {
            switch (c[pos.getIndex()]) {
            case QUOTE:
                appendQuotedString(pattern, pos, stripCustom, true);
                break;
            case START_FE:
                fmtCount++;
                seekNonWs(pattern, pos);
                int start = pos.getIndex();
                int index = readArgumentIndex(pattern, next(pos));
                stripCustom.append(START_FE).append(index);
                seekNonWs(pattern, pos);
                Format format = null;
                String formatDescription = null;
                if (c[pos.getIndex()] == START_FMT) {
                    formatDescription = parseFormatDescription(pattern,
                            next(pos));
                    format = getFormat(formatDescription);
                    if (format == null) {
                        stripCustom.append(START_FMT).append(formatDescription);
                    }
                }
                foundFormats.add(format);
                foundDescriptions.add(format == null ? null : formatDescription);
                Validate.isTrue(foundFormats.size() == fmtCount);
                Validate.isTrue(foundDescriptions.size() == fmtCount);
                if (c[pos.getIndex()] != END_FE) {
                    throw new IllegalArgumentException(
                            "Unreadable format element at position " + start);
                }
                // fall through
            default:
                stripCustom.append(c[pos.getIndex()]);
                next(pos);
            }
        }
        super.applyPattern(stripCustom.toString());
        toPattern = insertFormats(super.toPattern(), foundDescriptions);
        if (containsElements(foundFormats)) {
            Format[] origFormats = getFormats();
            // only loop over what we know we have, as MessageFormat on Java 1.3 
            // seems to provide an extra format element:
            int i = 0;
            for (Iterator it = foundFormats.iterator(); it.hasNext(); i++) {
                Format f = (Format) it.next();
                if (f != null) {
                    origFormats[i] = f;
                }
            }
            super.setFormats(origFormats);
        }
    }
</xref>
<xref>
private String insertFormats(String pattern, ArrayList customPatterns) {
        if (!containsElements(customPatterns)) {
            return pattern;
        }
        StringBuffer sb = new StringBuffer(pattern.length() * 2);
        ParsePosition pos = new ParsePosition(0);
        int fe = -1;
        int depth = 0;
        while (pos.getIndex() < pattern.length()) {
            char c = pattern.charAt(pos.getIndex());
            switch (c) {
            case QUOTE:
                appendQuotedString(pattern, pos, sb, false);
                break;
            case START_FE:
                depth++;
                if (depth == 1) {
                    fe++;
                    sb.append(START_FE).append(
                            readArgumentIndex(pattern, next(pos)));
                    String customPattern = (String) customPatterns.get(fe);
                    if (customPattern != null) {
                        sb.append(START_FMT).append(customPattern);
                    }
                }
                break;
            case END_FE:
                depth--;
                //fall through:
            default:
                sb.append(c);
                next(pos);
            }
        }
        return sb.toString();
    }
</xref>
Methods called by `appendQuotedString` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="toCharArray" from="StrBuilder.java">
<signature>
public char[] toCharArray()
</signature>
<code>
    public char[] toCharArray() {
        if (size == 0) {
            return ArrayUtils.EMPTY_CHAR_ARRAY;
        }
        char chars[] = new char[size];
        System.arraycopy(buffer, 0, chars, 0, size);
        return chars;
    }
</code>
<implementation in="StrBuilder.java">
    public char[] toCharArray() {
        if (size == 0) {
            return ArrayUtils.EMPTY_CHAR_ARRAY;
        }
        char chars[] = new char[size];
        System.arraycopy(buffer, 0, chars, 0, size);
        return chars;
    }
</implementation>
<implementation in="StrBuilder.java">
    public char[] toCharArray(int startIndex, int endIndex) {
        endIndex = validateRange(startIndex, endIndex);
        int len = endIndex - startIndex;
        if (len == 0) {
            return ArrayUtils.EMPTY_CHAR_ARRAY;
        }
        char chars[] = new char[len];
        System.arraycopy(buffer, startIndex, chars, 0, len);
        return chars;
    }
</implementation>
</callee>
<callee name="append" from="StrBuilder.java">
<signature>
public StrBuilder append(Object obj)
</signature>
<code>
    public StrBuilder append(Object obj) {
        if (obj == null) {
            return appendNull();
        } 
        return append(obj.toString());        
    }
</code>
<implementation in="StrBuilder.java">
    public StrBuilder append(Object obj) {
        if (obj == null) {
            return appendNull();
        } 
        return append(obj.toString());        
    }
</implementation>
<implementation in="StrBuilder.java">
    public StrBuilder append(String str) {
        if (str == null) {
            return appendNull();
        }
        int strLen = str.length();
        if (strLen > 0) {
            int len = length();
            ensureCapacity(len + strLen);
            str.getChars(0, strLen, buffer, len);
            size += strLen;
        }
        return this;
    }
</implementation>
<implementation in="StrBuilder.java">
    public StrBuilder append(String str, int startIndex, int length) {
        if (str == null) {
            return appendNull();
        }
        if (startIndex < 0 || startIndex > str.length()) {
            throw new StringIndexOutOfBoundsException("startIndex must be valid");
        }
        if (length < 0 || (startIndex + length) > str.length()) {
            throw new StringIndexOutOfBoundsException("length must be valid");
        }
        if (length > 0) {
            int len = length();
            ensureCapacity(len + length);
            str.getChars(startIndex, startIndex + length, buffer, len);
            size += length;
        }
        return this;
    }
</implementation>
<implementation in="StrBuilder.java">
    public StrBuilder append(StringBuffer str) {
        if (str == null) {
            return appendNull();
        }
        int strLen = str.length();
        if (strLen > 0) {
            int len = length();
            ensureCapacity(len + strLen);
            str.getChars(0, strLen, buffer, len);
            size += strLen;
        }
        return this;
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
<callee name="substring" from="StrBuilder.java">
<signature>
public String substring(int start)
</signature>
<code>
    public String substring(int start) {
        return substring(start, size);
    }
</code>
<implementation in="StrBuilder.java">
    public String substring(int start) {
        return substring(start, size);
    }
</implementation>
<implementation in="StrBuilder.java">
    public String substring(int startIndex, int endIndex) {
        endIndex = validateRange(startIndex, endIndex);
        return new String(buffer, startIndex, endIndex - startIndex);
    }
</implementation>
</callee>
<callee name="startsWith" from="StrBuilder.java">
<signature>
public boolean startsWith(String str)
</signature>
<code>
    public boolean startsWith(String str) {
        if (str == null) {
            return false;
        }
        int len = str.length();
        if (len == 0) {
            return true;
        }
        if (len > size) {
            return false;
        }
        for (int i = 0; i < len; i++) {
            if (buffer[i] != str.charAt(i)) {
                return false;
            }
        }
        return true;
    }
</code>
</callee>
<callee name="next" from="ExtendedMessageFormat.java">
<signature>
private ParsePosition next(ParsePosition pos)
</signature>
<code>
    private ParsePosition next(ParsePosition pos) {
        pos.setIndex(pos.getIndex() + 1);
        return pos;
    }
</code>
<implementation in="ExtendedMessageFormat.java">
    private ParsePosition next(ParsePosition pos) {
        pos.setIndex(pos.getIndex() + 1);
        return pos;
    }
</implementation>
<implementation in="StrTokenizer.java">
    public Object next() {
        if (hasNext()) {
            return tokens[tokenPos++];
        }
        throw new NoSuchElementException();
    }
</implementation>
</callee>

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `ExtendedMessageFormat`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.OutOfMemoryError (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `appendQuotedString` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: java.lang.OutOfMemoryError
message: Requested array size exceeds VM limit
thrown_at: org.apache.commons.lang.text.ExtendedMessageFormat.appendQuotedString(ExtendedMessageFormat.java:422)
</ground_truth_crash>
<failing_test class="org.apache.commons.lang.text.ExtendedMessageFormatTest" method="testEscapedQuote_LANG_477">
    public void testEscapedQuote_LANG_477() {
        String pattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        assertEquals("it's a dummy test!", emf.format(new Object[] {"DUMMY"}));
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.lang.text.ExtendedMessageFormatTest::testEscapedQuote_LANG_477
java.lang.OutOfMemoryError: Requested array size exceeds VM limit
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.
The test method depends on the following from its test class — setUp, helper methods, constants, fixture files. Replicate this environment EXACTLY (register the same files, wire the same providers, use the same constants); do NOT improvise setup the test performs differently, and DROP any check whose setup you cannot replicate:
<test_support class="org.apache.commons.lang.text.ExtendedMessageFormatTest">
// --- setUp() (test setup) ---
    protected void setUp() throws Exception {
        super.setUp();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
    }

// --- class fields/constants the test uses ---
private Map registry = new HashMap();
</test_support>

STATE YOU CAN READ (public no-argument readers — capture them BEFORE and AFTER a call documented as read-only or non-mutating; an unexplained change is a hidden-state violation): toPattern

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- ExtendedMessageFormat.startsWith
- ExtendedMessageFormat.append
- ExtendedMessageFormat.next
- StrBuilder.append
- StrBuilder.startsWith
</root_cause_reachable>
First harness: establish the most direct path from the fuzz entrypoint through the patched code.
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
<details open><summary>▸ Output (~9,040 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.FieldPosition;
import java.text.Format;
import java.text.MessageFormat;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    private static final Object[] FIXED_ARGS = new Object[] {"DUMMY"};

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        // ANCHOR: exact failing test input first.
        exercisePattern("it''s a {0,lower} 'test'!", registry, FIXED_ARGS, "it's a dummy test!");

        String a = sanitize(data.consumeAsciiString(24));
        String b = sanitize(data.consumeAsciiString(24));
        String quoted = sanitize(data.consumeAsciiString(24));
        String arg = sanitizeArg(data.consumeAsciiString(24));
        if (arg.length() == 0) {
            arg = "DUMMY";
        }
        Object[] args = new Object[] {arg};

        String fmt = data.consumeBoolean() ? "lower" : "upper";
        String transformed = "lower".equals(fmt) ? arg.toLowerCase(Locale.ROOT) : arg.toUpperCase(Locale.ROOT);

        if (a.length() == 0) {
            a = "x";
        }
        if (b.length() == 0) {
            b = "y";
        }
        if (quoted.length() == 0) {
            quoted = "test";
        }

        // EXPLORE / FLIP THE PATCHED CONDITION:
        // Valid-by-construction patterns that place escaped quotes at different boundaries
        // where appendQuotedString(pattern, pos, ..., true) used to return without advancing pos.
        String p1 = a + "''" + b + " {0," + fmt + "} '" + escapeQuotedLiteral(quoted) + "'!";
        String e1 = a + "'" + b + " " + transformed + " " + quoted + "!";
        exercisePattern(p1, registry, args, e1);

        String p2 = "''" + a + " {0," + fmt + "} '" + escapeQuotedLiteral(quoted) + "'";
        String e2 = "'" + a + " " + transformed + " " + quoted;
        exercisePattern(p2, registry, args, e2);

        String p3 = a + " {0," + fmt + "} '" + escapeQuotedLiteral(quoted) + "' " + b + "''" + a;
        String e3 = a + " " + transformed + " " + quoted + " " + b + "'" + a;
        exercisePattern(p3, registry, args, e3);

        String p4 = a + "''" + b + " {0," + fmt + "} " + b + "''" + a;
        String e4 = a + "'" + b + " " + transformed + " " + b + "'" + a;
        exercisePattern(p4, registry, args, e4);
    }

    private static void exercisePattern(String pattern, Map registry, Object[] args, String expected) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, Locale.getDefault(), registry);

            String beforeToPattern = emf.toPattern();
            String formatted = emf.format(args);
            String afterToPattern = emf.toPattern();

            // Post-condition: formatting is read-only with respect to the pattern; a correct implementation
            // must not mutate the stored pattern when merely formatting values.
            if (beforeToPattern != null ? !beforeToPattern.equals(afterToPattern) : afterToPattern != null) {
                throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: toPattern changed across format input=" + pattern + " before=" + beforeToPattern + " after=" + afterToPattern);
            }

            if (expected != null && !expected.equals(formatted)) {
                throw new RuntimeException("[oracle:known-output] metamorphic violation: known formatted output mismatch input=" + pattern + " expected=" + expected + " actual=" + formatted);
            }

            // Metamorphic relation: re-parsing emf.toPattern() with the same registry must preserve behaviour.
            try {
                ExtendedMessageFormat roundTrip = new ExtendedMessageFormat(beforeToPattern, Locale.getDefault(), registry);
                String roundTripFormatted = roundTrip.format(args);
                if (!formatted.equals(roundTripFormatted)) {
                    throw new RuntimeException("[oracle:roundtrip] metamorphic violation: format(p) != format(toPattern(p)) input=" + pattern + " lhs=" + formatted + " rhs=" + roundTripFormatted + " toPattern=" + beforeToPattern);
                }
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throw propagate(t);
                }
                return;
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw propagate(t);
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("Validation") || name.contains("Invalid")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                    && "appendQuotedString".equals(ste.getMethodName())) {
                return true;
            }
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                    && "next".equals(ste.getMethodName())) {
                return true;
            }
            if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                    && ("append".equals(ste.getMethodName()) || "startsWith".equals(ste.getMethodName()))) {
                return true;
            }
        }
        return false;
    }

    private static RuntimeException propagate(Throwable t) {
        if (t instanceof RuntimeException) {
            return (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        return new RuntimeException(t);
    }

    private static String sanitize(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == ' ' || c == '_' || c == '-') {
                out.append(c);
            }
        }
        return out.toString().trim();
    }

    private static String sanitizeArg(String s) {
        String x = sanitize(s);
        return x.length() == 0 ? "DUMMY" : x;
    }

    private static String escapeQuotedLiteral(String s) {
        return s.replace("'", "''");
    }

    static class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat(locale);
        }
    }

    static class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new UpperCaseFormat(locale);
        }
    }

    static class LowerCaseFormat extends Format {
        private final Locale locale;

        LowerCaseFormat(Locale locale) {
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toLowerCase(locale));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            int index = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(index);
        }
    }

    static class UpperCaseFormat extends Format {
        private final Locale locale;

        UpperCaseFormat(Locale locale) {
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toUpperCase(locale));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            int index = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(index);
        }
    }
}
```

</details>

---
## [12] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [13] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~9,238 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [11] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.FieldPosition;
import java.text.Format;
import java.text.MessageFormat;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    private static final Object[] FIXED_ARGS = new Object[] {"DUMMY"};

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        // ANCHOR: exact failing test input first.
        exercisePattern("it''s a {0,lower} 'test'!", registry, FIXED_ARGS, "it's a dummy test!");

        String a = sanitize(data.consumeAsciiString(24));
        String b = sanitize(data.consumeAsciiString(24));
        String quoted = sanitize(data.consumeAsciiString(24));
        String arg = sanitizeArg(data.consumeAsciiString(24));
        if (arg.length() == 0) {
            arg = "DUMMY";
        }
        Object[] args = new Object[] {arg};

        String fmt = data.consumeBoolean() ? "lower" : "upper";
        String transformed = "lower".equals(fmt) ? arg.toLowerCase(Locale.ROOT) : arg.toUpperCase(Locale.ROOT);

        if (a.length() == 0) {
            a = "x";
        }
        if (b.length() == 0) {
            b = "y";
        }
        if (quoted.length() == 0) {
            quoted = "test";
        }

        // EXPLORE / FLIP THE PATCHED CONDITION:
        // Valid-by-construction patterns that place escaped quotes at different boundaries
        // where appendQuotedString(pattern, pos, ..., true) used to return without advancing pos.
        String p1 = a + "''" + b + " {0," + fmt + "} '" + escapeQuotedLiteral(quoted) + "'!";
        String e1 = a + "'" + b + " " + transformed + " " + quoted + "!";
        exercisePattern(p1, registry, args, e1);

        String p2 = "''" + a + " {0," + fmt + "} '" + escapeQuotedLiteral(quoted) + "'";
        String e2 = "'" + a + " " + transformed + " " + quoted;
        exercisePattern(p2, registry, args, e2);

        String p3 = a + " {0," + fmt + "} '" + escapeQuotedLiteral(quoted) + "' " + b + "''" + a;
        String e3 = a + " " + transformed + " " + quoted + " " + b + "'" + a;
        exercisePattern(p3, registry, args, e3);

        String p4 = a + "''" + b + " {0," + fmt + "} " + b + "''" + a;
        String e4 = a + "'" + b + " " + transformed + " " + b + "'" + a;
        exercisePattern(p4, registry, args, e4);
    }

    private static void exercisePattern(String pattern, Map registry, Object[] args, String expected) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, Locale.getDefault(), registry);

            String beforeToPattern = emf.toPattern();
            String formatted = emf.format(args);
            String afterToPattern = emf.toPattern();

            // Post-condition: formatting is read-only with respect to the pattern; a correct implementation
            // must not mutate the stored pattern when merely formatting values.
            if (beforeToPattern != null ? !beforeToPattern.equals(afterToPattern) : afterToPattern != null) {
                throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: toPattern changed across format input=" + pattern + " before=" + beforeToPattern + " after=" + afterToPattern);
            }

            if (expected != null && !expected.equals(formatted)) {
                throw new RuntimeException("[oracle:known-output] metamorphic violation: known formatted output mismatch input=" + pattern + " expected=" + expected + " actual=" + formatted);
            }

            // Metamorphic relation: re-parsing emf.toPattern() with the same registry must preserve behaviour.
            try {
                ExtendedMessageFormat roundTrip = new ExtendedMessageFormat(beforeToPattern, Locale.getDefault(), registry);
                String roundTripFormatted = roundTrip.format(args);
                if (!formatted.equals(roundTripFormatted)) {
                    throw new RuntimeException("[oracle:roundtrip] metamorphic violation: format(p) != format(toPattern(p)) input=" + pattern + " lhs=" + formatted + " rhs=" + roundTripFormatted + " toPattern=" + beforeToPattern);
                }
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throw propagate(t);
                }
                return;
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw propagate(t);
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("Validation") || name.contains("Invalid")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                    && "appendQuotedString".equals(ste.getMethodName())) {
                return true;
            }
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                    && "next".equals(ste.getMethodName())) {
                return true;
            }
            if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                    && ("append".equals(ste.getMethodName()) || "startsWith".equals(ste.getMethodName()))) {
                return true;
            }
        }
        return false;
    }

    private static RuntimeException propagate(Throwable t) {
        if (t instanceof RuntimeException) {
            return (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        return new RuntimeException(t);
    }

    private static String sanitize(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == ' ' || c == '_' || c == '-') {
                out.append(c);
            }
        }
        return out.toString().trim();
    }

    private static String sanitizeArg(String s) {
        String x = sanitize(s);
        return x.length() == 0 ? "DUMMY" : x;
    }

    private static String escapeQuotedLiteral(String s) {
        return s.replace("'", "''");
    }

    static class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat(locale);
        }
    }

    static class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new UpperCaseFormat(locale);
        }
    }

    static class LowerCaseFormat extends Format {
        private final Locale locale;

        LowerCaseFormat(Locale locale) {
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toLowerCase(locale));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            int index = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(index);
        }
    }

    static class UpperCaseFormat extends Format {
        private final Locale locale;

        UpperCaseFormat(Locale locale) {
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toUpperCase(locale));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            int index = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(index);
        }
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~7,241 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    private static final Map REGISTRY = new HashMap();

    static {
        REGISTRY.put("lower", new LowerCaseFormatFactory());
        REGISTRY.put("upper", new UpperCaseFormatFactory());
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact trigger from the failing test. On the buggy version this reaches
        // ExtendedMessageFormat.applyPattern -> appendQuotedString(..., escapingOn=true)
        // at a leading quote from the doubled quote in "it''s", and crashes with OOME.
        ExtendedMessageFormat emf = new ExtendedMessageFormat("it''s a {0,lower} 'test'!", REGISTRY);
        String out = emf.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(out)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: input=it''s a {0,lower} 'test'! expected=it's a dummy test! actual=" + out);
        }
        String tp = emf.toPattern();
        if (!"it''s a {0,lower} 'test'!".equals(tp)) {
            throw new RuntimeException("[oracle:anchor-topattern] metamorphic violation: expected=it''s a {0,lower} 'test'! actual=" + tp);
        }

        // EXPLORE: same root-cause property as the patch: applyPattern sees a QUOTE and
        // calls appendQuotedString(..., true) with pos pointing at the first char of a doubled quote.
        String prefix = clean(data.consumeAsciiString(16));
        String middle = clean(data.consumeAsciiString(16));
        String suffix = clean(data.consumeAsciiString(16));
        String quoted = clean(data.consumeAsciiString(16));
        String arg = clean(data.consumeAsciiString(16));
        if (arg.length() == 0) {
            arg = "Dummy";
        }
        if (quoted.length() == 0) {
            quoted = "test";
        }

        String fmt = data.consumeBoolean() ? "lower" : "upper";
        String transformed = "lower".equals(fmt) ? arg.toLowerCase(Locale.ROOT) : arg.toUpperCase(Locale.ROOT);

        // Force a doubled quote in plain text before the format element to flip the patched condition.
        String pattern1 = nonEmpty(prefix, "a") + "''" + nonEmpty(middle, "b") + " {0," + fmt + "} '" + quoteLiteral(quoted) + "'!" + suffix;
        String expected1 = nonEmpty(prefix, "a") + "'" + nonEmpty(middle, "b") + " " + transformed + " " + quoted + "!" + suffix;
        checkPattern(pattern1, arg, expected1);

        // Doubled quote at the very beginning.
        String pattern2 = "''" + nonEmpty(prefix, "x") + " {0," + fmt + "} '" + quoteLiteral(quoted) + "'" + suffix;
        String expected2 = "'" + nonEmpty(prefix, "x") + " " + transformed + " " + quoted + suffix;
        checkPattern(pattern2, arg, expected2);

        // Multiple doubled quotes around the format element.
        String pattern3 = nonEmpty(prefix, "l") + "''" + nonEmpty(middle, "m") + " {0," + fmt + "} " + nonEmpty(suffix, "r") + "''";
        String expected3 = nonEmpty(prefix, "l") + "'" + nonEmpty(middle, "m") + " " + transformed + " " + nonEmpty(suffix, "r") + "'";
        checkPattern(pattern3, arg, expected3);
    }

    private static void checkPattern(String pattern, String arg, String expected) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);
        String before = emf.toPattern();
        String actual = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        // Post-condition: formatting should not mutate the stored pattern.
        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: input=" + pattern + " before=" + before + " after=" + after);
        }

        if (!expected.equals(actual)) {
            throw new RuntimeException("[oracle:format-output] metamorphic violation: input=" + pattern + " expected=" + expected + " actual=" + actual);
        }

        // Metamorphic relation: reparsing the object's own toPattern with the same registry
        // must preserve formatting behaviour for the same arguments.
        ExtendedMessageFormat reparsed = new ExtendedMessageFormat(before, REGISTRY);
        String reparsedActual = reparsed.format(new Object[] { arg });
        if (!actual.equals(reparsedActual)) {
            throw new RuntimeException("[oracle:roundtrip] metamorphic violation: input=" + pattern + " lhs=" + actual + " rhs=" + reparsedActual + " toPattern=" + before);
        }
    }

    private static String clean(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')
                    || c == ' '
                    || c == '_'
                    || c == '-') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String nonEmpty(String s, String dflt) {
        return s.length() == 0 ? dflt : s;
    }

    private static String quoteLiteral(String s) {
        return s.replace("'", "''");
    }

    static class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    static class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new UpperCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    static class LowerCaseFormat extends Format {
        private final Locale locale;

        LowerCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toLowerCase(locale));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            int start = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(start);
        }
    }

    static class UpperCaseFormat extends Format {
        private final Locale locale;

        UpperCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toUpperCase(locale));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            int start = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(start);
        }
    }
}
```

</details>

---
## [14] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [15] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~7,559 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [11] — not reprinted*
- *[assistant] message: identical to step [13] — not reprinted*
- *[user] message: identical to step [5] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    private static final Map REGISTRY = new HashMap();

    static {
        REGISTRY.put("lower", new LowerCaseFormatFactory());
        REGISTRY.put("upper", new UpperCaseFormatFactory());
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact trigger from the failing test. On the buggy version this reaches
        // ExtendedMessageFormat.applyPattern -> appendQuotedString(..., escapingOn=true)
        // at a leading quote from the doubled quote in "it''s", and crashes with OOME.
        ExtendedMessageFormat emf = new ExtendedMessageFormat("it''s a {0,lower} 'test'!", REGISTRY);
        String out = emf.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(out)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: input=it''s a {0,lower} 'test'! expected=it's a dummy test! actual=" + out);
        }
        String tp = emf.toPattern();
        if (!"it''s a {0,lower} 'test'!".equals(tp)) {
            throw new RuntimeException("[oracle:anchor-topattern] metamorphic violation: expected=it''s a {0,lower} 'test'! actual=" + tp);
        }

        // EXPLORE: same root-cause property as the patch: applyPattern sees a QUOTE and
        // calls appendQuotedString(..., true) with pos pointing at the first char of a doubled quote.
        String prefix = clean(data.consumeAsciiString(16));
        String middle = clean(data.consumeAsciiString(16));
        String suffix = clean(data.consumeAsciiString(16));
        String quoted = clean(data.consumeAsciiString(16));
        String arg = clean(data.consumeAsciiString(16));
        if (arg.length() == 0) {
            arg = "Dummy";
        }
        if (quoted.length() == 0) {
            quoted = "test";
        }

        String fmt = data.consumeBoolean() ? "lower" : "upper";
        String transformed = "lower".equals(fmt) ? arg.toLowerCase(Locale.ROOT) : arg.toUpperCase(Locale.ROOT);

        // Force a doubled quote in plain text before the format element to flip the patched condition.
        String pattern1 = nonEmpty(prefix, "a") + "''" + nonEmpty(middle, "b") + " {0," + fmt + "} '" + quoteLiteral(quoted) + "'!" + suffix;
        String expected1 = nonEmpty(prefix, "a") + "'" + nonEmpty(middle, "b") + " " + transformed + " " + quoted + "!" + suffix;
        checkPattern(pattern1, arg, expected1);

        // Doubled quote at the very beginning.
        String pattern2 = "''" + nonEmpty(prefix, "x") + " {0," + fmt + "} '" + quoteLiteral(quoted) + "'" + suffix;
        String expected2 = "'" + nonEmpty(prefix, "x") + " " + transformed + " " + quoted + suffix;
        checkPattern(pattern2, arg, expected2);

        // Multiple doubled quotes around the format element.
        String pattern3 = nonEmpty(prefix, "l") + "''" + nonEmpty(middle, "m") + " {0," + fmt + "} " + nonEmpty(suffix, "r") + "''";
        String expected3 = nonEmpty(prefix, "l") + "'" + nonEmpty(middle, "m") + " " + transformed + " " + nonEmpty(suffix, "r") + "'";
        checkPattern(pattern3, arg, expected3);
    }

    private static void checkPattern(String pattern, String arg, String expected) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);
        String before = emf.toPattern();
        String actual = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        // Post-condition: formatting should not mutate the stored pattern.
        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: input=" + pattern + " before=" + before + " after=" + after);
        }

        if (!expected.equals(actual)) {
            throw new RuntimeException("[oracle:format-output] metamorphic violation: input=" + pattern + " expected=" + expected + " actual=" + actual);
        }

        // Metamorphic relation: reparsing the object's own toPattern with the same registry
        // must preserve formatting behaviour for the same arguments.
        ExtendedMessageFormat reparsed = new ExtendedMessageFormat(before, REGISTRY);
        String reparsedActual = reparsed.format(new Object[] { arg });
        if (!actual.equals(reparsedActual)) {
            throw new RuntimeException("[oracle:roundtrip] metamorphic violation: input=" + pattern + " lhs=" + actual + " rhs=" + reparsedActual + " toPattern=" + before);
        }
    }

    private static String clean(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')
                    || c == ' '
                    || c == '_'
                    || c == '-') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String nonEmpty(String s, String dflt) {
        return s.length() == 0 ? dflt : s;
    }

    private static String quoteLiteral(String s) {
        return s.replace("'", "''");
    }

    static class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    static class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new UpperCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    static class LowerCaseFormat extends Format {
        private final Locale locale;

        LowerCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toLowerCase(locale));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            int start = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(start);
        }
    }

    static class UpperCaseFormat extends Format {
        private final Locale locale;

        UpperCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toUpperCase(locale));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            int start = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(start);
        }
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~6,913 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        // Exact anchor from the failing test. On the buggy version this should crash during
        // ExtendedMessageFormat construction, in applyPattern -> appendQuotedString.
        String anchor = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat emfAnchor = new ExtendedMessageFormat(anchor, registry);
        String anchorOut = emfAnchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor] metamorphic violation: expected=it's a dummy test! actual=" + anchorOut);
        }
        if (!anchor.equals(emfAnchor.toPattern())) {
            throw new RuntimeException("[oracle:anchor-pattern] metamorphic violation: expected=" + anchor + " actual=" + emfAnchor.toPattern());
        }

        // Explore the exact patched condition: appendQuotedString(pattern, pos, ..., true)
        // is entered with c[start] == QUOTE because the pattern contains a doubled quote ''.
        String left = lettersOnly(data.consumeAsciiString(12));
        String mid = lettersOnly(data.consumeAsciiString(12));
        String lit = lettersOnly(data.consumeAsciiString(12));
        String arg = lettersOnly(data.consumeAsciiString(12));
        if (left.length() == 0) left = "it";
        if (mid.length() == 0) mid = "s";
        if (lit.length() == 0) lit = "test";
        if (arg.length() == 0) arg = "DUMMY";

        String fmt = data.consumeBoolean() ? "lower" : "upper";
        String expectedArg = "lower".equals(fmt) ? arg.toLowerCase(Locale.ROOT) : arg.toUpperCase(Locale.ROOT);

        String pattern1 = left + "''" + mid + " {0," + fmt + "} '" + lit + "'!";
        ExtendedMessageFormat emf1 = new ExtendedMessageFormat(pattern1, registry);
        String out1 = emf1.format(new Object[] { arg });
        String exp1 = left + "'" + mid + " " + expectedArg + " " + lit + "!";
        if (!exp1.equals(out1)) {
            throw new RuntimeException("[oracle:variant1] metamorphic violation: pattern=" + pattern1 + " expected=" + exp1 + " actual=" + out1);
        }
        String tp1 = emf1.toPattern();
        ExtendedMessageFormat emf1b = new ExtendedMessageFormat(tp1, registry);
        String out1b = emf1b.format(new Object[] { arg });
        if (!out1.equals(out1b)) {
            throw new RuntimeException("[oracle:roundtrip1] metamorphic violation: pattern=" + pattern1 + " lhs=" + out1 + " rhs=" + out1b + " toPattern=" + tp1);
        }

        String pattern2 = "''" + left + " {0," + fmt + "} '" + lit + "'";
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(pattern2, registry);
        String out2 = emf2.format(new Object[] { arg });
        String exp2 = "'" + left + " " + expectedArg + " " + lit;
        if (!exp2.equals(out2)) {
            throw new RuntimeException("[oracle:variant2] metamorphic violation: pattern=" + pattern2 + " expected=" + exp2 + " actual=" + out2);
        }
        String tp2 = emf2.toPattern();
        ExtendedMessageFormat emf2b = new ExtendedMessageFormat(tp2, registry);
        String out2b = emf2b.format(new Object[] { arg });
        if (!out2.equals(out2b)) {
            throw new RuntimeException("[oracle:roundtrip2] metamorphic violation: pattern=" + pattern2 + " lhs=" + out2 + " rhs=" + out2b + " toPattern=" + tp2);
        }

        String pattern3 = left + "''" + mid + " {0," + fmt + "} " + left + "''" + mid;
        ExtendedMessageFormat emf3 = new ExtendedMessageFormat(pattern3, registry);
        String out3 = emf3.format(new Object[] { arg });
        String exp3 = left + "'" + mid + " " + expectedArg + " " + left + "'" + mid;
        if (!exp3.equals(out3)) {
            throw new RuntimeException("[oracle:variant3] metamorphic violation: pattern=" + pattern3 + " expected=" + exp3 + " actual=" + out3);
        }
        String tp3 = emf3.toPattern();
        ExtendedMessageFormat emf3b = new ExtendedMessageFormat(tp3, registry);
        String out3b = emf3b.format(new Object[] { arg });
        if (!out3.equals(out3b)) {
            throw new RuntimeException("[oracle:roundtrip3] metamorphic violation: pattern=" + pattern3 + " lhs=" + out3 + " rhs=" + out3b + " toPattern=" + tp3);
        }
    }

    private static String lettersOnly(String s) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new UpperCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    static final class LowerCaseFormat extends Format {
        private final Locale locale;

        LowerCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toLowerCase(locale));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            int start = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(start);
        }
    }

    static final class UpperCaseFormat extends Format {
        private final Locale locale;

        UpperCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toUpperCase(locale));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            int start = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(start);
        }
    }
}
```

</details>

---
## [16] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [17] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (8 message(s), ~7,351 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [11] — not reprinted*
- *[assistant] message: identical to step [13] — not reprinted*
- *[user] message: identical to step [5] — not reprinted*
- *[assistant] message: identical to step [15] — not reprinted*
- *[user] message: identical to step [5] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        // Exact anchor from the failing test. On the buggy version this should crash during
        // ExtendedMessageFormat construction, in applyPattern -> appendQuotedString.
        String anchor = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat emfAnchor = new ExtendedMessageFormat(anchor, registry);
        String anchorOut = emfAnchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor] metamorphic violation: expected=it's a dummy test! actual=" + anchorOut);
        }
        if (!anchor.equals(emfAnchor.toPattern())) {
            throw new RuntimeException("[oracle:anchor-pattern] metamorphic violation: expected=" + anchor + " actual=" + emfAnchor.toPattern());
        }

        // Explore the exact patched condition: appendQuotedString(pattern, pos, ..., true)
        // is entered with c[start] == QUOTE because the pattern contains a doubled quote ''.
        String left = lettersOnly(data.consumeAsciiString(12));
        String mid = lettersOnly(data.consumeAsciiString(12));
        String lit = lettersOnly(data.consumeAsciiString(12));
        String arg = lettersOnly(data.consumeAsciiString(12));
        if (left.length() == 0) left = "it";
        if (mid.length() == 0) mid = "s";
        if (lit.length() == 0) lit = "test";
        if (arg.length() == 0) arg = "DUMMY";

        String fmt = data.consumeBoolean() ? "lower" : "upper";
        String expectedArg = "lower".equals(fmt) ? arg.toLowerCase(Locale.ROOT) : arg.toUpperCase(Locale.ROOT);

        String pattern1 = left + "''" + mid + " {0," + fmt + "} '" + lit + "'!";
        ExtendedMessageFormat emf1 = new ExtendedMessageFormat(pattern1, registry);
        String out1 = emf1.format(new Object[] { arg });
        String exp1 = left + "'" + mid + " " + expectedArg + " " + lit + "!";
        if (!exp1.equals(out1)) {
            throw new RuntimeException("[oracle:variant1] metamorphic violation: pattern=" + pattern1 + " expected=" + exp1 + " actual=" + out1);
        }
        String tp1 = emf1.toPattern();
        ExtendedMessageFormat emf1b = new ExtendedMessageFormat(tp1, registry);
        String out1b = emf1b.format(new Object[] { arg });
        if (!out1.equals(out1b)) {
            throw new RuntimeException("[oracle:roundtrip1] metamorphic violation: pattern=" + pattern1 + " lhs=" + out1 + " rhs=" + out1b + " toPattern=" + tp1);
        }

        String pattern2 = "''" + left + " {0," + fmt + "} '" + lit + "'";
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(pattern2, registry);
        String out2 = emf2.format(new Object[] { arg });
        String exp2 = "'" + left + " " + expectedArg + " " + lit;
        if (!exp2.equals(out2)) {
            throw new RuntimeException("[oracle:variant2] metamorphic violation: pattern=" + pattern2 + " expected=" + exp2 + " actual=" + out2);
        }
        String tp2 = emf2.toPattern();
        ExtendedMessageFormat emf2b = new ExtendedMessageFormat(tp2, registry);
        String out2b = emf2b.format(new Object[] { arg });
        if (!out2.equals(out2b)) {
            throw new RuntimeException("[oracle:roundtrip2] metamorphic violation: pattern=" + pattern2 + " lhs=" + out2 + " rhs=" + out2b + " toPattern=" + tp2);
        }

        String pattern3 = left + "''" + mid + " {0," + fmt + "} " + left + "''" + mid;
        ExtendedMessageFormat emf3 = new ExtendedMessageFormat(pattern3, registry);
        String out3 = emf3.format(new Object[] { arg });
        String exp3 = left + "'" + mid + " " + expectedArg + " " + left + "'" + mid;
        if (!exp3.equals(out3)) {
            throw new RuntimeException("[oracle:variant3] metamorphic violation: pattern=" + pattern3 + " expected=" + exp3 + " actual=" + out3);
        }
        String tp3 = emf3.toPattern();
        ExtendedMessageFormat emf3b = new ExtendedMessageFormat(tp3, registry);
        String out3b = emf3b.format(new Object[] { arg });
        if (!out3.equals(out3b)) {
            throw new RuntimeException("[oracle:roundtrip3] metamorphic violation: pattern=" + pattern3 + " lhs=" + out3 + " rhs=" + out3b + " toPattern=" + tp3);
        }
    }

    private static String lettersOnly(String s) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new UpperCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    static final class LowerCaseFormat extends Format {
        private final Locale locale;

        LowerCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toLowerCase(locale));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            int start = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(start);
        }
    }

    static final class UpperCaseFormat extends Format {
        private final Locale locale;

        UpperCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toUpperCase(locale));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            int start = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(start);
        }
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~5,386 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        // Exact reproducer from the failing test.
        run("it''s a {0,lower} 'test'!", "DUMMY", registry, "it's a dummy test!");

        // Explore the exact patched condition:
        // appendQuotedString(..., escapingOn=true) is called when applyPattern sees a QUOTE.
        // The buggy code returns after appending a quote but does not advance ParsePosition.
        // Therefore any valid pattern with a doubled quote '' in ordinary text before/around a
        // real format element can re-enter the same quote position and blow up.
        String left = word(data.consumeAsciiString(8), "it");
        String right = word(data.consumeAsciiString(8), "s");
        String lit = word(data.consumeAsciiString(8), "test");
        String arg = word(data.consumeAsciiString(8), "DUMMY");
        String fmt = data.consumeBoolean() ? "lower" : "upper";
        String xformed = "lower".equals(fmt) ? arg.toLowerCase(Locale.ROOT) : arg.toUpperCase(Locale.ROOT);

        String p1 = left + "''" + right + " {0," + fmt + "} '" + lit + "'!";
        run(p1, arg, registry, left + "'" + right + " " + xformed + " " + lit + "!");

        String p2 = "''" + left + " {0," + fmt + "} '" + lit + "'";
        run(p2, arg, registry, "'" + left + " " + xformed + " " + lit);

        String p3 = left + " {0," + fmt + "} " + right + "''" + lit;
        run(p3, arg, registry, left + " " + xformed + " " + right + "'" + lit);
    }

    private static void run(String pattern, String arg, Map registry, String expected) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String before = emf.toPattern();
        String actual = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        if (!expected.equals(actual)) {
            throw new RuntimeException("[oracle:format] metamorphic violation: pattern=" + pattern + " expected=" + expected + " actual=" + actual);
        }

        // Formatting should not mutate the pattern stored by the formatter.
        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: pattern=" + pattern + " before=" + before + " after=" + after);
        }

        // Re-parsing the formatter's own pattern with the same registry must preserve behaviour.
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, registry);
        String actual2 = emf2.format(new Object[] { arg });
        if (!actual.equals(actual2)) {
            throw new RuntimeException("[oracle:roundtrip] metamorphic violation: pattern=" + pattern + " lhs=" + actual + " rhs=" + actual2 + " toPattern=" + before);
        }
    }

    private static String word(String s, String dflt) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                sb.append(c);
            }
        }
        return sb.length() == 0 ? dflt : sb.toString();
    }

    static class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    static class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new UpperCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    static class LowerCaseFormat extends Format {
        private final Locale locale;

        LowerCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toLowerCase(locale));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            int start = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(start);
        }
    }

    static class UpperCaseFormat extends Format {
        private final Locale locale;

        UpperCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toUpperCase(locale));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            int start = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(start);
        }
    }
}
```

</details>

---
## [18] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat
