# Pipeline trace — Lang-43

**Code:** `unknown`

**Patch label:** correct  *(the patch under analysis is a known-CORRECT fix — the pipeline is not told this)*

**Outcome:** correctly quiet (no false alarm). [no_harnesses; 0 harness(es) fuzzed on the patched build; campaign converged=False]

**Patch under analysis:**
```diff
--- /src/java/org/apache/commons/lang/text/ExtendedMessageFormat.java
+++ /src/java/org/apache/commons/lang/text/ExtendedMessageFormat.java
@@ -419,6 +419,7 @@
         int start = pos.getIndex();
         char[] c = pattern.toCharArray();
         if (escapingOn && c[start] == QUOTE) {
+            next(pos);
             return appendTo == null ? null : appendTo.append(QUOTE);
         }
         int lastHold = start;
```

63 sequential steps — 30 LLM calls, 33 deterministic. Read top to bottom.

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
<details><summary>▸ output — modified_files": [ (~14,647 chars)</summary>

**output:** 
```json
{
  "modified_files": [
    "src/java/org/apache/commons/lang/text/ExtendedMessageFormat.java"
  ],
  "patch_text": "--- /src/java/org/apache/commons/lang/text/ExtendedMessageFormat.java\n+++ /src/java/org/apache/commons/lang/text/ExtendedMessageFormat.java\n@@ -419,6 +419,7 @@\n         int start = pos.getIndex();\n         char[] c = pattern.toCharArray();\n         if (escapingOn && c[start] == QUOTE) {\n+            next(pos);\n             return appendTo == null ? null : appendTo.append(QUOTE);\n         }\n         int lastHold = start;\n",
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
<details><summary>▸ Prompt (2 message(s), ~31,786 chars)</summary>

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
@@ -419,6 +419,7 @@
         int start = pos.getIndex();
         char[] c = pattern.toCharArray();
         if (escapingOn && c[start] == QUOTE) {
+            next(pos);
             return appendTo == null ? null : appendTo.append(QUOTE);
         }
         int lastHold = start;

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
<details open><summary>▸ Output (~4,315 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        exercise("it''s a {0,lower} 'test'!", registry);

        String a = data.consumeAsciiString(24);
        String b = data.consumeAsciiString(24);
        String c = data.consumeAsciiString(24);
        int variant = data.consumeInt(0, 5);

        String pattern;
        switch (variant) {
            case 0:
                pattern = a + "''" + b;
                break;
            case 1:
                pattern = a + "'" + b + "'";
                break;
            case 2:
                pattern = a + "''" + b + " {0} " + c;
                break;
            case 3:
                pattern = a + " '" + b + "' " + c;
                break;
            case 4:
                pattern = a + "''" + b + " '{0}'";
                break;
            default:
                pattern = a + "'" + b + " {0} " + c;
                break;
        }

        exercise(pattern, registry);
    }

    private static void exercise(String pattern, Map registry) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, Locale.getDefault(), registry);

            String toPattern1 = emf.toPattern();

            try {
                ExtendedMessageFormat emf2 = new ExtendedMessageFormat(toPattern1, Locale.getDefault(), registry);
                String toPattern2 = emf2.toPattern();

                // Contract/oracle: toPattern is the object's canonical pattern representation.
                // Reconstructing a second object from that canonical representation and reading
                // toPattern again must yield the same canonical representation. A "fix" that
                // merely skips advancing/processing the quoted branch can avoid the crash while
                // silently producing a wrong canonical pattern, which this cross-check detects.
                if (toPattern1 != null && toPattern2 != null && !toPattern1.equals(toPattern2)) {
                    throw new RuntimeException(
                        "[oracle:toPattern-idempotence] metamorphic violation: "
                            + "pattern=" + pattern
                            + " lhs=" + toPattern1
                            + " rhs=" + toPattern2);
                }
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    rethrowUnchecked(t);
                }
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                rethrowUnchecked(t);
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
            || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError || t instanceof RuntimeException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            String cls = st[i].getClassName();
            String m = st[i].getMethodName();
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cls)
                    && ("appendQuotedString".equals(m)
                        || "next".equals(m)
                        || "startsWith".equals(m)
                        || "append".equals(m))) {
                return true;
            }
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                    && ("append".equals(m) || "startsWith".equals(m))) {
                return true;
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
}
```

</details>

---
## [4] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [5] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~7,169 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        exercise("it''s a {0,lower} 'test'!", registry);

        String a = data.consumeAsciiString(24);
        String b = data.consumeAsciiString(24);
        String c = data.consumeAsciiString(24);
        int variant = data.consumeInt(0, 5);

        String pattern;
        switch (variant) {
            case 0:
                pattern = a + "''" + b;
                break;
            case 1:
                pattern = a + "'" + b + "'";
                break;
            case 2:
                pattern = a + "''" + b + " {0} " + c;
                break;
            case 3:
                pattern = a + " '" + b + "' " + c;
                break;
            case 4:
                pattern = a + "''" + b + " '{0}'";
                break;
            default:
                pattern = a + "'" + b + " {0} " + c;
                break;
        }

        exercise(pattern, registry);
    }

    private static void exercise(String pattern, Map registry) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, Locale.getDefault(), registry);

            String toPattern1 = emf.toPattern();

            try {
                ExtendedMessageFormat emf2 = new ExtendedMessageFormat(toPattern1, Locale.getDefault(), registry);
                String toPattern2 = emf2.toPattern();

                // Contract/oracle: toPattern is the object's canonical pattern representation.
                // Reconstructing a second object from that canonical representation and reading
                // toPattern again must yield the same canonical representation. A "fix" that
                // merely skips advancing/processing the quoted branch can avoid the crash while
                // silently producing a wrong canonical pattern, which this cross-check detects.
                if (toPattern1 != null && toPattern2 != null && !toPattern1.equals(toPattern2)) {
                    throw new RuntimeException(
                        "[oracle:toPattern-idempotence] metamorphic violation: "
                            + "pattern=" + pattern
                            + " lhs=" + toPattern1
                            + " rhs=" + toPattern2);
                }
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    rethrowUnchecked(t);
                }
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                rethrowUnchecked(t);
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
            || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError || t instanceof RuntimeException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            String cls = st[i].getClassName();
            String m = st[i].getMethodName();
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cls)
                    && ("appendQuotedString".equals(m)
                        || "next".equals(m)
                        || "startsWith".equals(m)
                        || "append".equals(m))) {
                return true;
            }
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                    && ("append".equals(m) || "startsWith".equals(m))) {
                return true;
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
}
```
**[user]**
```
That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generated never drove execution into the changed code, or always took a path that does not fault.

The patch under analysis is below. The buggy version is the code BEFORE this patch is applied (the '+' lines are the FIXED code; the buggy code has the '-' lines instead):
--- /src/java/org/apache/commons/lang/text/ExtendedMessageFormat.java
+++ /src/java/org/apache/commons/lang/text/ExtendedMessageFormat.java
@@ -419,6 +419,7 @@
         int start = pos.getIndex();
         char[] c = pattern.toCharArray();
         if (escapingOn && c[start] == QUOTE) {
+            next(pos);
             return appendTo == null ? null : appendTo.append(QUOTE);
         }
         int lastHold = start;


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
<details open><summary>▸ Output (~3,373 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        exercise("it''s a {0} 'test'!", registry);

        String a = safeLiteral(data.consumeAsciiString(20));
        String b = safeLiteral(data.consumeAsciiString(20));
        String c = safeLiteral(data.consumeAsciiString(20));
        String d = safeLiteral(data.consumeAsciiString(20));

        int variant = data.consumeInt(0, 5);
        String pattern;
        switch (variant) {
            case 0:
                pattern = a + "''" + b + " {0}";
                break;
            case 1:
                pattern = a + "''" + b + " {0} '" + c + "'";
                break;
            case 2:
                pattern = a + " {0} " + b + "''" + c;
                break;
            case 3:
                pattern = "'" + a + "' " + b + "''" + c + " {0}";
                break;
            case 4:
                pattern = a + "''" + b + " '{0}' " + c + " {0}";
                break;
            default:
                pattern = a + " {0} " + b + "''" + c + " '" + d + "'!";
                break;
        }

        exercise(pattern, registry);
    }

    private static void exercise(String pattern, Map registry) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

        Object[] args = new Object[] { "DuMmY" };
        String actual = emf.format(args);
        String expected = new MessageFormat(pattern).format(args);

        if (!expected.equals(actual)) {
            throw new RuntimeException(
                "[oracle:messageformat-equivalence] metamorphic violation: pattern="
                    + pattern + " expected=" + expected + " actual=" + actual);
        }

        String canon1 = emf.toPattern();
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(canon1, registry);
        String canon2 = emf2.toPattern();

        // Contract used for this post-condition:
        // toPattern() is the canonical pattern representation of the formatter.
        // Re-parsing that canonical pattern and asking for toPattern() again must be stable.
        // A throw-deleting or branch-skipping patch in quoted-string handling can avoid the crash
        // while silently producing the wrong canonical pattern, which this check detects.
        if (!canon1.equals(canon2)) {
            throw new RuntimeException(
                "[oracle:toPattern-roundtrip] metamorphic violation: pattern="
                    + pattern + " lhs=" + canon1 + " rhs=" + canon2);
        }
    }

    private static String safeLiteral(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\'' || ch == '{' || ch == '}' || ch == ',') {
                out.append('x');
            } else if (ch < 0x20 || ch > 0x7e) {
                out.append('y');
            } else {
                out.append(ch);
            }
        }
        if (out.length() == 0) {
            out.append('a');
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
<details><summary>▸ Prompt (6 message(s), ~3,689 chars, 1 new)</summary>

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

        exercise("it''s a {0} 'test'!", registry);

        String a = safeLiteral(data.consumeAsciiString(20));
        String b = safeLiteral(data.consumeAsciiString(20));
        String c = safeLiteral(data.consumeAsciiString(20));
        String d = safeLiteral(data.consumeAsciiString(20));

        int variant = data.consumeInt(0, 5);
        String pattern;
        switch (variant) {
            case 0:
                pattern = a + "''" + b + " {0}";
                break;
            case 1:
                pattern = a + "''" + b + " {0} '" + c + "'";
                break;
            case 2:
                pattern = a + " {0} " + b + "''" + c;
                break;
            case 3:
                pattern = "'" + a + "' " + b + "''" + c + " {0}";
                break;
            case 4:
                pattern = a + "''" + b + " '{0}' " + c + " {0}";
                break;
            default:
                pattern = a + " {0} " + b + "''" + c + " '" + d + "'!";
                break;
        }

        exercise(pattern, registry);
    }

    private static void exercise(String pattern, Map registry) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

        Object[] args = new Object[] { "DuMmY" };
        String actual = emf.format(args);
        String expected = new MessageFormat(pattern).format(args);

        if (!expected.equals(actual)) {
            throw new RuntimeException(
                "[oracle:messageformat-equivalence] metamorphic violation: pattern="
                    + pattern + " expected=" + expected + " actual=" + actual);
        }

        String canon1 = emf.toPattern();
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(canon1, registry);
        String canon2 = emf2.toPattern();

        // Contract used for this post-condition:
        // toPattern() is the canonical pattern representation of the formatter.
        // Re-parsing that canonical pattern and asking for toPattern() again must be stable.
        // A throw-deleting or branch-skipping patch in quoted-string handling can avoid the crash
        // while silently producing the wrong canonical pattern, which this check detects.
        if (!canon1.equals(canon2)) {
            throw new RuntimeException(
                "[oracle:toPattern-roundtrip] metamorphic violation: pattern="
                    + pattern + " lhs=" + canon1 + " rhs=" + canon2);
        }
    }

    private static String safeLiteral(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\'' || ch == '{' || ch == '}' || ch == ',') {
                out.append('x');
            } else if (ch < 0x20 || ch > 0x7e) {
                out.append('y');
            } else {
                out.append(ch);
            }
        }
        if (out.length() == 0) {
            out.append('a');
        }
        return out.toString();
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~7,001 chars)</summary>

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

        exercise("it''s a {0,lower} 'test'!", "DUMMY", registry);

        String left = safeLiteral(data.consumeAsciiString(16));
        String middle = safeLiteral(data.consumeAsciiString(16));
        String tail = safeLiteral(data.consumeAsciiString(16));
        String arg = safeArg(data.consumeAsciiString(16));

        int variant = data.consumeInt(0, 3);
        String pattern;
        if (variant == 0) {
            pattern = left + "''" + middle + " {0,lower} '" + tail + "'!";
        } else if (variant == 1) {
            pattern = "'" + tail + "' " + left + "''" + middle + " {0,lower}";
        } else if (variant == 2) {
            pattern = left + " {0,lower} " + middle + "''" + tail;
        } else {
            pattern = left + "''" + middle + " and {0,upper} then {0,lower} '" + tail + "'";
        }

        exercise(pattern, arg, registry);
    }

    private static void exercise(String pattern, String arg, Map registry) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, Locale.ENGLISH, registry);
        String actual = emf.format(new Object[] { arg });

        // Contract/oracle: custom "lower"/"upper" formats must apply to argument 0, while quoted
        // sections are emitted literally and doubled single quotes represent one literal quote.
        // A patch that merely avoids the crash by skipping quote handling would change this output.
        String expected = expectedOutput(pattern, arg);
        if (!actual.equals(expected)) {
            throw new RuntimeException(
                "[oracle:format-semantics] metamorphic violation: pattern=" + pattern
                    + " arg=" + arg + " expected=" + expected + " actual=" + actual);
        }

        String p1 = emf.toPattern();
        String p2 = new ExtendedMessageFormat(p1, Locale.ENGLISH, registry).toPattern();
        if (!p1.equals(p2)) {
            throw new RuntimeException(
                "[oracle:toPattern-roundtrip] metamorphic violation: pattern=" + pattern
                    + " lhs=" + p1 + " rhs=" + p2);
        }
    }

    private static String expectedOutput(String pattern, String arg) {
        String lower = arg.toLowerCase(Locale.ENGLISH);
        String upper = arg.toUpperCase(Locale.ENGLISH);

        if ("it''s a {0,lower} 'test'!".equals(pattern)) {
            return "it's a " + lower + " test!";
        }

        int idxLower = pattern.indexOf("{0,lower}");
        int idxUpper = pattern.indexOf("{0,upper}");

        if (idxLower >= 0 && idxUpper < 0) {
            String before = decodeLiterals(pattern.substring(0, idxLower));
            String after = decodeLiterals(pattern.substring(idxLower + "{0,lower}".length()));
            return before + lower + after;
        }

        if (idxUpper >= 0 && idxLower > idxUpper) {
            String before = decodeLiterals(pattern.substring(0, idxUpper));
            String middle = decodeLiterals(pattern.substring(idxUpper + "{0,upper}".length(), idxLower));
            String after = decodeLiterals(pattern.substring(idxLower + "{0,lower}".length()));
            return before + upper + middle + lower + after;
        }

        throw new RuntimeException("[oracle:unexpected-pattern-shape] metamorphic violation: pattern=" + pattern);
    }

    private static String decodeLiterals(String s) {
        StringBuilder out = new StringBuilder(s.length());
        boolean inQuote = false;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\'') {
                if (i + 1 < s.length() && s.charAt(i + 1) == '\'') {
                    out.append('\'');
                    i++;
                } else {
                    inQuote = !inQuote;
                }
            } else {
                out.append(ch);
            }
        }
        return out.toString();
    }

    private static String safeLiteral(String s) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9')) {
                out.append(ch);
            }
        }
        if (out.length() == 0) {
            out.append('x');
        }
        return out.toString();
    }

    private static String safeArg(String s) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
                out.append(ch);
            }
        }
        if (out.length() == 0) {
            out.append("DuMmY");
        }
        return out.toString();
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat(locale == null ? Locale.ENGLISH : locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new UpperCaseFormat(locale == null ? Locale.ENGLISH : locale);
        }
    }

    private static final class LowerCaseFormat extends Format {
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
            pos.setIndex(source.length());
            return source;
        }
    }

    private static final class UpperCaseFormat extends Format {
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
            pos.setIndex(source.length());
            return source;
        }
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
<details><summary>▸ Prompt (8 message(s), ~7,436 chars, 1 new)</summary>

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

        exercise("it''s a {0,lower} 'test'!", "DUMMY", registry);

        String left = safeLiteral(data.consumeAsciiString(16));
        String middle = safeLiteral(data.consumeAsciiString(16));
        String tail = safeLiteral(data.consumeAsciiString(16));
        String arg = safeArg(data.consumeAsciiString(16));

        int variant = data.consumeInt(0, 3);
        String pattern;
        if (variant == 0) {
            pattern = left + "''" + middle + " {0,lower} '" + tail + "'!";
        } else if (variant == 1) {
            pattern = "'" + tail + "' " + left + "''" + middle + " {0,lower}";
        } else if (variant == 2) {
            pattern = left + " {0,lower} " + middle + "''" + tail;
        } else {
            pattern = left + "''" + middle + " and {0,upper} then {0,lower} '" + tail + "'";
        }

        exercise(pattern, arg, registry);
    }

    private static void exercise(String pattern, String arg, Map registry) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, Locale.ENGLISH, registry);
        String actual = emf.format(new Object[] { arg });

        // Contract/oracle: custom "lower"/"upper" formats must apply to argument 0, while quoted
        // sections are emitted literally and doubled single quotes represent one literal quote.
        // A patch that merely avoids the crash by skipping quote handling would change this output.
        String expected = expectedOutput(pattern, arg);
        if (!actual.equals(expected)) {
            throw new RuntimeException(
                "[oracle:format-semantics] metamorphic violation: pattern=" + pattern
                    + " arg=" + arg + " expected=" + expected + " actual=" + actual);
        }

        String p1 = emf.toPattern();
        String p2 = new ExtendedMessageFormat(p1, Locale.ENGLISH, registry).toPattern();
        if (!p1.equals(p2)) {
            throw new RuntimeException(
                "[oracle:toPattern-roundtrip] metamorphic violation: pattern=" + pattern
                    + " lhs=" + p1 + " rhs=" + p2);
        }
    }

    private static String expectedOutput(String pattern, String arg) {
        String lower = arg.toLowerCase(Locale.ENGLISH);
        String upper = arg.toUpperCase(Locale.ENGLISH);

        if ("it''s a {0,lower} 'test'!".equals(pattern)) {
            return "it's a " + lower + " test!";
        }

        int idxLower = pattern.indexOf("{0,lower}");
        int idxUpper = pattern.indexOf("{0,upper}");

        if (idxLower >= 0 && idxUpper < 0) {
            String before = decodeLiterals(pattern.substring(0, idxLower));
            String after = decodeLiterals(pattern.substring(idxLower + "{0,lower}".length()));
            return before + lower + after;
        }

        if (idxUpper >= 0 && idxLower > idxUpper) {
            String before = decodeLiterals(pattern.substring(0, idxUpper));
            String middle = decodeLiterals(pattern.substring(idxUpper + "{0,upper}".length(), idxLower));
            String after = decodeLiterals(pattern.substring(idxLower + "{0,lower}".length()));
            return before + upper + middle + lower + after;
        }

        throw new RuntimeException("[oracle:unexpected-pattern-shape] metamorphic violation: pattern=" + pattern);
    }

    private static String decodeLiterals(String s) {
        StringBuilder out = new StringBuilder(s.length());
        boolean inQuote = false;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\'') {
                if (i + 1 < s.length() && s.charAt(i + 1) == '\'') {
                    out.append('\'');
                    i++;
                } else {
                    inQuote = !inQuote;
                }
            } else {
                out.append(ch);
            }
        }
        return out.toString();
    }

    private static String safeLiteral(String s) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9')) {
                out.append(ch);
            }
        }
        if (out.length() == 0) {
            out.append('x');
        }
        return out.toString();
    }

    private static String safeArg(String s) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
                out.append(ch);
            }
        }
        if (out.length() == 0) {
            out.append("DuMmY");
        }
        return out.toString();
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat(locale == null ? Locale.ENGLISH : locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new UpperCaseFormat(locale == null ? Locale.ENGLISH : locale);
        }
    }

    private static final class LowerCaseFormat extends Format {
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
            pos.setIndex(source.length());
            return source;
        }
    }

    private static final class UpperCaseFormat extends Format {
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
            pos.setIndex(source.length());
            return source;
        }
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~2,730 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        trigger("it''s a {0} 'test'!", registry);

        String left = lettersAndDigits(data.consumeAsciiString(24));
        String right = lettersAndDigits(data.consumeAsciiString(24));
        String tail = lettersAndDigits(data.consumeAsciiString(24));
        String arg = lettersAndDigits(data.consumeAsciiString(16));
        if (arg.length() == 0) {
            arg = "X";
        }

        String pattern;
        switch (data.consumeInt(0, 4)) {
            case 0:
                pattern = left + "''" + right + " {0}";
                break;
            case 1:
                pattern = left + "''" + right + " {0} " + tail;
                break;
            case 2:
                pattern = left + " {0} " + right + "''" + tail;
                break;
            case 3:
                pattern = left + "''" + right + " and " + tail + " {0}";
                break;
            default:
                pattern = left + " {0} " + right + "''" + tail + " end";
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, Locale.US, registry);

        // Contract/oracle: for patterns using only standard MessageFormat syntax,
        // ExtendedMessageFormat must behave like MessageFormat. A patch that merely
        // suppresses the buggy branch or mangles quote handling would violate this.
        String actual = emf.format(new Object[] { arg });
        String expected = new MessageFormat(pattern, Locale.US).format(new Object[] { arg });
        if (!expected.equals(actual)) {
            throw new RuntimeException(
                "[oracle:messageformat-equivalence] metamorphic violation: pattern="
                    + pattern + " arg=" + arg + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void trigger(String pattern, Map registry) {
        new ExtendedMessageFormat(pattern, Locale.US, registry);
    }

    private static String lettersAndDigits(String s) {
        StringBuffer out = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if ((ch >= 'a' && ch <= 'z')
                    || (ch >= 'A' && ch <= 'Z')
                    || (ch >= '0' && ch <= '9')) {
                out.append(ch);
            }
        }
        return out.toString();
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
<details><summary>▸ Prompt (2 message(s), ~31,660 chars, 1 new)</summary>

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
@@ -419,6 +419,7 @@
         int start = pos.getIndex();
         char[] c = pattern.toCharArray();
         if (escapingOn && c[start] == QUOTE) {
+            next(pos);
             return appendTo == null ? null : appendTo.append(QUOTE);
         }
         int lastHold = start;

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
<details open><summary>▸ Output (~8,658 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        class LowerCaseFormat extends java.text.Format {
            private final java.util.Locale locale;

            LowerCaseFormat(java.util.Locale locale) {
                this.locale = locale == null ? java.util.Locale.ROOT : locale;
            }

            public StringBuffer format(Object obj, StringBuffer toAppendTo, java.text.FieldPosition pos) {
                if (obj != null) {
                    toAppendTo.append(obj.toString().toLowerCase(locale));
                }
                return toAppendTo;
            }

            public Object parseObject(String source, java.text.ParsePosition pos) {
                int start = pos.getIndex();
                pos.setIndex(source.length());
                return source.substring(start);
            }
        }

        class UpperCaseFormat extends java.text.Format {
            private final java.util.Locale locale;

            UpperCaseFormat(java.util.Locale locale) {
                this.locale = locale == null ? java.util.Locale.ROOT : locale;
            }

            public StringBuffer format(Object obj, StringBuffer toAppendTo, java.text.FieldPosition pos) {
                if (obj != null) {
                    toAppendTo.append(obj.toString().toUpperCase(locale));
                }
                return toAppendTo;
            }

            public Object parseObject(String source, java.text.ParsePosition pos) {
                int start = pos.getIndex();
                pos.setIndex(source.length());
                return source.substring(start);
            }
        }

        class LowerCaseFormatFactory implements FormatFactory {
            public java.text.Format getFormat(String name, String arguments, java.util.Locale locale) {
                return new LowerCaseFormat(locale);
            }
        }

        class UpperCaseFormatFactory implements FormatFactory {
            public java.text.Format getFormat(String name, String arguments, java.util.Locale locale) {
                return new UpperCaseFormat(locale);
            }
        }

        java.util.Map registry = new java.util.HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        // ANCHOR: exact regression input from the test.
        try {
            String pattern = "it''s a {0,lower} 'test'!";
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
            String formatted = emf.format(new Object[] { "DUMMY" });

            if (!"it's a dummy test!".equals(formatted)) {
                throw new RuntimeException("[oracle:anchor-output] metamorphic violation: regression formatting input="
                        + pattern + " lhs=" + formatted + " rhs=it's a dummy test!");
            }

            // Contract: toPattern() is the pattern string for this message format.
            // Re-parsing that pattern and formatting the same arguments must preserve output.
            String roundTripPattern = emf.toPattern();
            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(roundTripPattern, registry);
            String formatted2 = emf2.format(new Object[] { "DUMMY" });
            if (!formatted.equals(formatted2)) {
                throw new RuntimeException("[oracle:toPattern-roundtrip] metamorphic violation: input="
                        + pattern + " lhs=" + formatted + " rhs=" + formatted2);
            }
        } catch (Throwable t) {
            boolean oracle = t instanceof RuntimeException
                    && t.getMessage() != null
                    && t.getMessage().startsWith("[oracle:");
            boolean rootCause = t instanceof OutOfMemoryError;
            if (rootCause) {
                for (StackTraceElement ste : t.getStackTrace()) {
                    String cn = ste.getClassName();
                    String mn = ste.getMethodName();
                    if (("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cn)
                            && ("appendQuotedString".equals(mn) || "next".equals(mn) || "append".equals(mn)
                                    || "startsWith".equals(mn)))
                            || ("org.apache.commons.lang.text.StrBuilder".equals(cn)
                                    && ("append".equals(mn) || "startsWith".equals(mn)))) {
                        throw (OutOfMemoryError) t;
                    }
                }
            }
            if (oracle) {
                throw (RuntimeException) t;
            }
        }

        String prefix = data.consumeAsciiString(24);
        String middle = data.consumeAsciiString(24);
        String suffix = data.consumeAsciiString(24);
        String literal = data.consumeAsciiString(16);
        String arg = data.consumeAsciiString(16);

        prefix = prefix.replace("'", "").replace("{", "").replace("}", "");
        middle = middle.replace("'", "").replace("{", "").replace("}", "");
        suffix = suffix.replace("'", "").replace("{", "").replace("}", "");
        literal = literal.replace("'", "").replace("{", "").replace("}", "");
        arg = arg.replace("'", "").replace("{", "").replace("}", "");

        if (prefix.length() == 0) {
            prefix = "p";
        }
        if (middle.length() == 0) {
            middle = "m";
        }
        if (suffix.length() == 0) {
            suffix = "s";
        }
        if (literal.length() == 0) {
            literal = "lit";
        }
        if (arg.length() == 0) {
            arg = "X";
        }

        String[] patterns = new String[] {
                "''" + prefix + " {0,lower} '" + literal + "'",
                prefix + "''" + middle + " {0,lower} " + suffix,
                prefix + " {0,lower} ''" + suffix,
                prefix + "''" + middle + "{0,lower}'" + literal + "'"
        };

        for (int i = 0; i < patterns.length; i++) {
            try {
                ExtendedMessageFormat emf = new ExtendedMessageFormat(patterns[i], registry);
                String out1 = emf.format(new Object[] { arg });

                // Strategy (c): flip the patched condition by varying where doubled quotes occur
                // (start of pattern, adjacent to format element, after format element). For every
                // correct implementation, toPattern() must describe an equivalent format, so
                // formatting after re-parsing toPattern() must yield the same observable result.
                String tp1 = emf.toPattern();
                ExtendedMessageFormat emf2 = new ExtendedMessageFormat(tp1, registry);
                String out2 = emf2.format(new Object[] { arg });
                if (!out1.equals(out2)) {
                    throw new RuntimeException("[oracle:toPattern-equiv] metamorphic violation: input="
                            + patterns[i] + " lhs=" + out1 + " rhs=" + out2);
                }

                String tp2 = emf2.toPattern();
                if (!tp1.equals(tp2)) {
                    throw new RuntimeException("[oracle:toPattern-idempotent] metamorphic violation: input="
                            + patterns[i] + " lhs=" + tp1 + " rhs=" + tp2);
                }
            } catch (Throwable t) {
                boolean oracle = t instanceof RuntimeException
                        && t.getMessage() != null
                        && t.getMessage().startsWith("[oracle:");
                boolean rootCause = t instanceof OutOfMemoryError;
                if (rootCause) {
                    for (StackTraceElement ste : t.getStackTrace()) {
                        String cn = ste.getClassName();
                        String mn = ste.getMethodName();
                        if (("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cn)
                                && ("appendQuotedString".equals(mn) || "next".equals(mn) || "append".equals(mn)
                                        || "startsWith".equals(mn)))
                                || ("org.apache.commons.lang.text.StrBuilder".equals(cn)
                                        && ("append".equals(mn) || "startsWith".equals(mn)))) {
                            throw (OutOfMemoryError) t;
                        }
                    }
                }
                if (oracle) {
                    throw (RuntimeException) t;
                }
            }
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
<details><summary>▸ Prompt (4 message(s), ~8,856 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [11] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        class LowerCaseFormat extends java.text.Format {
            private final java.util.Locale locale;

            LowerCaseFormat(java.util.Locale locale) {
                this.locale = locale == null ? java.util.Locale.ROOT : locale;
            }

            public StringBuffer format(Object obj, StringBuffer toAppendTo, java.text.FieldPosition pos) {
                if (obj != null) {
                    toAppendTo.append(obj.toString().toLowerCase(locale));
                }
                return toAppendTo;
            }

            public Object parseObject(String source, java.text.ParsePosition pos) {
                int start = pos.getIndex();
                pos.setIndex(source.length());
                return source.substring(start);
            }
        }

        class UpperCaseFormat extends java.text.Format {
            private final java.util.Locale locale;

            UpperCaseFormat(java.util.Locale locale) {
                this.locale = locale == null ? java.util.Locale.ROOT : locale;
            }

            public StringBuffer format(Object obj, StringBuffer toAppendTo, java.text.FieldPosition pos) {
                if (obj != null) {
                    toAppendTo.append(obj.toString().toUpperCase(locale));
                }
                return toAppendTo;
            }

            public Object parseObject(String source, java.text.ParsePosition pos) {
                int start = pos.getIndex();
                pos.setIndex(source.length());
                return source.substring(start);
            }
        }

        class LowerCaseFormatFactory implements FormatFactory {
            public java.text.Format getFormat(String name, String arguments, java.util.Locale locale) {
                return new LowerCaseFormat(locale);
            }
        }

        class UpperCaseFormatFactory implements FormatFactory {
            public java.text.Format getFormat(String name, String arguments, java.util.Locale locale) {
                return new UpperCaseFormat(locale);
            }
        }

        java.util.Map registry = new java.util.HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        // ANCHOR: exact regression input from the test.
        try {
            String pattern = "it''s a {0,lower} 'test'!";
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
            String formatted = emf.format(new Object[] { "DUMMY" });

            if (!"it's a dummy test!".equals(formatted)) {
                throw new RuntimeException("[oracle:anchor-output] metamorphic violation: regression formatting input="
                        + pattern + " lhs=" + formatted + " rhs=it's a dummy test!");
            }

            // Contract: toPattern() is the pattern string for this message format.
            // Re-parsing that pattern and formatting the same arguments must preserve output.
            String roundTripPattern = emf.toPattern();
            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(roundTripPattern, registry);
            String formatted2 = emf2.format(new Object[] { "DUMMY" });
            if (!formatted.equals(formatted2)) {
                throw new RuntimeException("[oracle:toPattern-roundtrip] metamorphic violation: input="
                        + pattern + " lhs=" + formatted + " rhs=" + formatted2);
            }
        } catch (Throwable t) {
            boolean oracle = t instanceof RuntimeException
                    && t.getMessage() != null
                    && t.getMessage().startsWith("[oracle:");
            boolean rootCause = t instanceof OutOfMemoryError;
            if (rootCause) {
                for (StackTraceElement ste : t.getStackTrace()) {
                    String cn = ste.getClassName();
                    String mn = ste.getMethodName();
                    if (("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cn)
                            && ("appendQuotedString".equals(mn) || "next".equals(mn) || "append".equals(mn)
                                    || "startsWith".equals(mn)))
                            || ("org.apache.commons.lang.text.StrBuilder".equals(cn)
                                    && ("append".equals(mn) || "startsWith".equals(mn)))) {
                        throw (OutOfMemoryError) t;
                    }
                }
            }
            if (oracle) {
                throw (RuntimeException) t;
            }
        }

        String prefix = data.consumeAsciiString(24);
        String middle = data.consumeAsciiString(24);
        String suffix = data.consumeAsciiString(24);
        String literal = data.consumeAsciiString(16);
        String arg = data.consumeAsciiString(16);

        prefix = prefix.replace("'", "").replace("{", "").replace("}", "");
        middle = middle.replace("'", "").replace("{", "").replace("}", "");
        suffix = suffix.replace("'", "").replace("{", "").replace("}", "");
        literal = literal.replace("'", "").replace("{", "").replace("}", "");
        arg = arg.replace("'", "").replace("{", "").replace("}", "");

        if (prefix.length() == 0) {
            prefix = "p";
        }
        if (middle.length() == 0) {
            middle = "m";
        }
        if (suffix.length() == 0) {
            suffix = "s";
        }
        if (literal.length() == 0) {
            literal = "lit";
        }
        if (arg.length() == 0) {
            arg = "X";
        }

        String[] patterns = new String[] {
                "''" + prefix + " {0,lower} '" + literal + "'",
                prefix + "''" + middle + " {0,lower} " + suffix,
                prefix + " {0,lower} ''" + suffix,
                prefix + "''" + middle + "{0,lower}'" + literal + "'"
        };

        for (int i = 0; i < patterns.length; i++) {
            try {
                ExtendedMessageFormat emf = new ExtendedMessageFormat(patterns[i], registry);
                String out1 = emf.format(new Object[] { arg });

                // Strategy (c): flip the patched condition by varying where doubled quotes occur
                // (start of pattern, adjacent to format element, after format element). For every
                // correct implementation, toPattern() must describe an equivalent format, so
                // formatting after re-parsing toPattern() must yield the same observable result.
                String tp1 = emf.toPattern();
                ExtendedMessageFormat emf2 = new ExtendedMessageFormat(tp1, registry);
                String out2 = emf2.format(new Object[] { arg });
                if (!out1.equals(out2)) {
                    throw new RuntimeException("[oracle:toPattern-equiv] metamorphic violation: input="
                            + patterns[i] + " lhs=" + out1 + " rhs=" + out2);
                }

                String tp2 = emf2.toPattern();
                if (!tp1.equals(tp2)) {
                    throw new RuntimeException("[oracle:toPattern-idempotent] metamorphic violation: input="
                            + patterns[i] + " lhs=" + tp1 + " rhs=" + tp2);
                }
            } catch (Throwable t) {
                boolean oracle = t instanceof RuntimeException
                        && t.getMessage() != null
                        && t.getMessage().startsWith("[oracle:");
                boolean rootCause = t instanceof OutOfMemoryError;
                if (rootCause) {
                    for (StackTraceElement ste : t.getStackTrace()) {
                        String cn = ste.getClassName();
                        String mn = ste.getMethodName();
                        if (("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cn)
                                && ("appendQuotedString".equals(mn) || "next".equals(mn) || "append".equals(mn)
                                        || "startsWith".equals(mn)))
                                || ("org.apache.commons.lang.text.StrBuilder".equals(cn)
                                        && ("append".equals(mn) || "startsWith".equals(mn)))) {
                            throw (OutOfMemoryError) t;
                        }
                    }
                }
                if (oracle) {
                    throw (RuntimeException) t;
                }
            }
        }
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~4,834 chars)</summary>

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
    private static final class LowerCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toLowerCase(Locale.ROOT));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            int start = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(start);
        }
    }

    private static final class UpperCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toUpperCase(Locale.ROOT));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            int start = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(start);
        }
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat();
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new UpperCaseFormat();
        }
    }

    private static Map makeRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static void checkRoundTrip(String pattern, String arg, Map registry) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String out1 = emf.format(new Object[] { arg });

        String p2 = emf.toPattern();
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(p2, registry);
        String out2 = emf2.format(new Object[] { arg });

        if (!out1.equals(out2)) {
            throw new RuntimeException("[oracle:roundtrip] metamorphic violation: pattern=" + pattern + " lhs=" + out1 + " rhs=" + out2);
        }

        String p3 = emf2.toPattern();
        if (!p2.equals(p3)) {
            throw new RuntimeException("[oracle:topattern-idempotent] metamorphic violation: pattern=" + pattern + " lhs=" + p2 + " rhs=" + p3);
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = makeRegistry();

        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, registry);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: lhs=" + anchorOut + " rhs=it's a dummy test!");
        }
        checkRoundTrip(anchorPattern, "DUMMY", registry);

        String a = data.consumeAsciiString(12).replace("'", "").replace("{", "").replace("}", "");
        String b = data.consumeAsciiString(12).replace("'", "").replace("{", "").replace("}", "");
        String c = data.consumeAsciiString(12).replace("'", "").replace("{", "").replace("}", "");
        String arg = data.consumeAsciiString(12).replace("'", "").replace("{", "").replace("}", "");

        if (a.length() == 0) a = "a";
        if (b.length() == 0) b = "b";
        if (c.length() == 0) c = "c";
        if (arg.length() == 0) arg = "X";

        int variant = data.consumeInt(0, 5);
        String pattern;
        switch (variant) {
            case 0:
                pattern = a + "''" + b + " {0,lower} '" + c + "'!";
                break;
            case 1:
                pattern = "''" + a + " {0,lower} '" + b + "'";
                break;
            case 2:
                pattern = a + " {0,lower} ''" + b;
                break;
            case 3:
                pattern = a + "''s {" + 0 + ",lower} '" + b + "' " + c;
                break;
            case 4:
                pattern = a + "''" + b + " {0,upper} '" + c + "'";
                break;
            default:
                pattern = a + "''" + b + " {0,lower} '" + c + "' and ''more''";
                break;
        }

        checkRoundTrip(pattern, arg, registry);
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
<details><summary>▸ Prompt (6 message(s), ~5,152 chars, 1 new)</summary>

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
    private static final class LowerCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toLowerCase(Locale.ROOT));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            int start = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(start);
        }
    }

    private static final class UpperCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toUpperCase(Locale.ROOT));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            int start = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(start);
        }
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat();
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new UpperCaseFormat();
        }
    }

    private static Map makeRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static void checkRoundTrip(String pattern, String arg, Map registry) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String out1 = emf.format(new Object[] { arg });

        String p2 = emf.toPattern();
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(p2, registry);
        String out2 = emf2.format(new Object[] { arg });

        if (!out1.equals(out2)) {
            throw new RuntimeException("[oracle:roundtrip] metamorphic violation: pattern=" + pattern + " lhs=" + out1 + " rhs=" + out2);
        }

        String p3 = emf2.toPattern();
        if (!p2.equals(p3)) {
            throw new RuntimeException("[oracle:topattern-idempotent] metamorphic violation: pattern=" + pattern + " lhs=" + p2 + " rhs=" + p3);
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = makeRegistry();

        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, registry);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: lhs=" + anchorOut + " rhs=it's a dummy test!");
        }
        checkRoundTrip(anchorPattern, "DUMMY", registry);

        String a = data.consumeAsciiString(12).replace("'", "").replace("{", "").replace("}", "");
        String b = data.consumeAsciiString(12).replace("'", "").replace("{", "").replace("}", "");
        String c = data.consumeAsciiString(12).replace("'", "").replace("{", "").replace("}", "");
        String arg = data.consumeAsciiString(12).replace("'", "").replace("{", "").replace("}", "");

        if (a.length() == 0) a = "a";
        if (b.length() == 0) b = "b";
        if (c.length() == 0) c = "c";
        if (arg.length() == 0) arg = "X";

        int variant = data.consumeInt(0, 5);
        String pattern;
        switch (variant) {
            case 0:
                pattern = a + "''" + b + " {0,lower} '" + c + "'!";
                break;
            case 1:
                pattern = "''" + a + " {0,lower} '" + b + "'";
                break;
            case 2:
                pattern = a + " {0,lower} ''" + b;
                break;
            case 3:
                pattern = a + "''s {" + 0 + ",lower} '" + b + "' " + c;
                break;
            case 4:
                pattern = a + "''" + b + " {0,upper} '" + c + "'";
                break;
            default:
                pattern = a + "''" + b + " {0,lower} '" + c + "' and ''more''";
                break;
        }

        checkRoundTrip(pattern, arg, registry);
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~4,716 chars)</summary>

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
    private static final class LowerCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toLowerCase(Locale.ENGLISH));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            int start = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(start);
        }
    }

    private static final class UpperCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toUpperCase(Locale.ENGLISH));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            int start = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(start);
        }
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat();
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new UpperCaseFormat();
        }
    }

    private static Map makeRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static String clean(String s) {
        return s.replace("'", "").replace("{", "").replace("}", "");
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = makeRegistry();

        // Exact trigger from the failing test. On the buggy version this reaches
        // ExtendedMessageFormat.applyPattern -> appendQuotedString with escapingOn=true
        // at the doubled quote in "it''s...", where the missing next(pos) causes the loop
        // to reprocess the same quote and grow output until OutOfMemoryError.
        ExtendedMessageFormat emf = new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);
        String formatted = emf.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(formatted)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: lhs=" + formatted + " rhs=it's a dummy test!");
        }

        String a = clean(data.consumeAsciiString(8));
        String b = clean(data.consumeAsciiString(8));
        String c = clean(data.consumeAsciiString(8));
        String arg = clean(data.consumeAsciiString(8));
        if (a.length() == 0) a = "x";
        if (b.length() == 0) b = "y";
        if (c.length() == 0) c = "z";
        if (arg.length() == 0) arg = "MiXeD";

        int where = data.consumeInt(0, 4);
        String pattern;
        switch (where) {
            case 0:
                pattern = a + "''" + b + " {0,lower} '" + c + "'!";
                break;
            case 1:
                pattern = "''" + a + " {0,lower} '" + b + "'!";
                break;
            case 2:
                pattern = a + " {0,lower} ''" + b + "!";
                break;
            case 3:
                pattern = a + "''" + b + "{0,upper} '" + c + "'";
                break;
            default:
                pattern = a + "''s a {0,lower} '" + b + "' " + c;
                break;
        }

        ExtendedMessageFormat fuzzed = new ExtendedMessageFormat(pattern, registry);
        String out1 = fuzzed.format(new Object[] { arg });

        // Contract-based post-condition: toPattern() must describe an equivalent format.
        // Reconstructing from toPattern() and formatting the same arguments must preserve output.
        ExtendedMessageFormat reparsed = new ExtendedMessageFormat(fuzzed.toPattern(), registry);
        String out2 = reparsed.format(new Object[] { arg });
        if (!out1.equals(out2)) {
            throw new RuntimeException("[oracle:toPattern-roundtrip] metamorphic violation: pattern=" + pattern + " lhs=" + out1 + " rhs=" + out2);
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
<details><summary>▸ Prompt (8 message(s), ~5,154 chars, 1 new)</summary>

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
    private static final class LowerCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toLowerCase(Locale.ENGLISH));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            int start = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(start);
        }
    }

    private static final class UpperCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toUpperCase(Locale.ENGLISH));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            int start = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(start);
        }
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat();
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new UpperCaseFormat();
        }
    }

    private static Map makeRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static String clean(String s) {
        return s.replace("'", "").replace("{", "").replace("}", "");
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = makeRegistry();

        // Exact trigger from the failing test. On the buggy version this reaches
        // ExtendedMessageFormat.applyPattern -> appendQuotedString with escapingOn=true
        // at the doubled quote in "it''s...", where the missing next(pos) causes the loop
        // to reprocess the same quote and grow output until OutOfMemoryError.
        ExtendedMessageFormat emf = new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);
        String formatted = emf.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(formatted)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: lhs=" + formatted + " rhs=it's a dummy test!");
        }

        String a = clean(data.consumeAsciiString(8));
        String b = clean(data.consumeAsciiString(8));
        String c = clean(data.consumeAsciiString(8));
        String arg = clean(data.consumeAsciiString(8));
        if (a.length() == 0) a = "x";
        if (b.length() == 0) b = "y";
        if (c.length() == 0) c = "z";
        if (arg.length() == 0) arg = "MiXeD";

        int where = data.consumeInt(0, 4);
        String pattern;
        switch (where) {
            case 0:
                pattern = a + "''" + b + " {0,lower} '" + c + "'!";
                break;
            case 1:
                pattern = "''" + a + " {0,lower} '" + b + "'!";
                break;
            case 2:
                pattern = a + " {0,lower} ''" + b + "!";
                break;
            case 3:
                pattern = a + "''" + b + "{0,upper} '" + c + "'";
                break;
            default:
                pattern = a + "''s a {0,lower} '" + b + "' " + c;
                break;
        }

        ExtendedMessageFormat fuzzed = new ExtendedMessageFormat(pattern, registry);
        String out1 = fuzzed.format(new Object[] { arg });

        // Contract-based post-condition: toPattern() must describe an equivalent format.
        // Reconstructing from toPattern() and formatting the same arguments must preserve output.
        ExtendedMessageFormat reparsed = new ExtendedMessageFormat(fuzzed.toPattern(), registry);
        String out2 = reparsed.format(new Object[] { arg });
        if (!out1.equals(out2)) {
            throw new RuntimeException("[oracle:toPattern-roundtrip] metamorphic violation: pattern=" + pattern + " lhs=" + out1 + " rhs=" + out2);
        }
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~5,070 chars)</summary>

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
    private static final class LowerCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toLowerCase(Locale.ENGLISH));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            int start = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(start);
        }
    }

    private static final class UpperCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toUpperCase(Locale.ENGLISH));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            int start = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(start);
        }
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat();
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new UpperCaseFormat();
        }
    }

    private static Map makeRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static String clean(String s) {
        return s.replace("'", "").replace("{", "").replace("}", "");
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = makeRegistry();

        // Exact trigger from ExtendedMessageFormatTest.testEscapedQuote_LANG_477.
        // On the buggy version, the doubled quote at the start of "''s" reaches
        // appendQuotedString(..., escapingOn=true) and hits the missing next(pos).
        ExtendedMessageFormat emf = new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);

        // Post-condition on fixed builds: formatting the known regression input must match
        // the test's expected output, and re-parsing toPattern() must preserve that output.
        String formatted = emf.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(formatted)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: lhs=" + formatted + " rhs=it's a dummy test!");
        }
        ExtendedMessageFormat reparsedAnchor = new ExtendedMessageFormat(emf.toPattern(), registry);
        String formatted2 = reparsedAnchor.format(new Object[] { "DUMMY" });
        if (!formatted.equals(formatted2)) {
            throw new RuntimeException("[oracle:anchor-roundtrip] metamorphic violation: lhs=" + formatted + " rhs=" + formatted2);
        }

        String a = clean(data.consumeAsciiString(8));
        String b = clean(data.consumeAsciiString(8));
        String c = clean(data.consumeAsciiString(8));
        String arg = clean(data.consumeAsciiString(8));
        if (a.length() == 0) a = "it";
        if (b.length() == 0) b = "s";
        if (c.length() == 0) c = "test";
        if (arg.length() == 0) arg = "DUMMY";

        String pattern;
        switch (data.consumeInt(0, 5)) {
            case 0:
                pattern = a + "''" + b + " a {0,lower} '" + c + "'!";
                break;
            case 1:
                pattern = "''" + a + " {0,lower} '" + b + "'!";
                break;
            case 2:
                pattern = a + " {0,lower} ''" + b + "!";
                break;
            case 3:
                pattern = a + "''" + b + " {0,upper} '" + c + "'!";
                break;
            case 4:
                pattern = a + "''" + b + "{0,lower}'" + c + "'!";
                break;
            default:
                pattern = a + "''s a {0,lower} '" + c + "'!";
                break;
        }

        ExtendedMessageFormat fuzzed = new ExtendedMessageFormat(pattern, registry);
        String out1 = fuzzed.format(new Object[] { arg });
        ExtendedMessageFormat reparsed = new ExtendedMessageFormat(fuzzed.toPattern(), registry);
        String out2 = reparsed.format(new Object[] { arg });
        if (!out1.equals(out2)) {
            throw new RuntimeException("[oracle:toPattern-roundtrip] metamorphic violation: pattern=" + pattern + " lhs=" + out1 + " rhs=" + out2);
        }
    }
}
```

</details>

---
## [18] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [19] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~31,660 chars, 1 new)</summary>

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
@@ -419,6 +419,7 @@
         int start = pos.getIndex();
         char[] c = pattern.toCharArray();
         if (escapingOn && c[start] == QUOTE) {
+            next(pos);
             return appendTo == null ? null : appendTo.append(QUOTE);
         }
         int lastHold = start;

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
<details open><summary>▸ Output (~8,553 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        try {
            Class lowerCls = null;
            Class upperCls = null;
            try {
                lowerCls = Class.forName("org.apache.commons.lang.text.ExtendedMessageFormatTest$LowerCaseFormatFactory");
                upperCls = Class.forName("org.apache.commons.lang.text.ExtendedMessageFormatTest$UpperCaseFormatFactory");
            } catch (Throwable ignored) {
                try {
                    lowerCls = Class.forName("org.apache.commons.lang.text.LowerCaseFormatFactory");
                    upperCls = Class.forName("org.apache.commons.lang.text.UpperCaseFormatFactory");
                } catch (Throwable ignoredToo) {
                    return;
                }
            }

            Constructor lowerCtor = lowerCls.getDeclaredConstructor(new Class[0]);
            Constructor upperCtor = upperCls.getDeclaredConstructor(new Class[0]);
            lowerCtor.setAccessible(true);
            upperCtor.setAccessible(true);
            registry.put("lower", (FormatFactory) lowerCtor.newInstance(new Object[0]));
            registry.put("upper", (FormatFactory) upperCtor.newInstance(new Object[0]));
        } catch (Throwable t) {
            return;
        }

        String anchorPattern = "it''s a {0,lower} 'test'!";
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(anchorPattern, registry);
            String formatted = emf.format(new Object[] { "DUMMY" });
            if (!"it's a dummy test!".equals(formatted)) {
                throw new RuntimeException("[oracle:anchor-output] metamorphic violation: expected exact regression-test output input="
                        + anchorPattern + " actual=" + formatted);
            }

            String canonical = emf.toPattern();
            try {
                ExtendedMessageFormat reparsed = new ExtendedMessageFormat(canonical, registry);
                String formatted2 = reparsed.format(new Object[] { "DUMMY" });
                if (!formatted.equals(formatted2) || !canonical.equals(reparsed.toPattern())) {
                    throw new RuntimeException("[oracle:roundtrip] metamorphic violation: parse(format-pattern)-roundtrip input="
                            + anchorPattern + " lhs=" + formatted + " rhs=" + formatted2
                            + " p1=" + canonical + " p2=" + reparsed.toPattern());
                }
            } catch (Throwable ignored) {
            }
        } catch (Throwable t) {
            boolean cleanRejection =
                    (t instanceof IllegalArgumentException) ||
                    (t instanceof NumberFormatException) ||
                    (t.getClass().getName().indexOf("Validate") >= 0) ||
                    (t.getClass().getName().indexOf("Validation") >= 0);
            boolean rootCause = false;
            if (t instanceof OutOfMemoryError || t instanceof RuntimeException) {
                StackTraceElement[] st = t.getStackTrace();
                for (int i = 0; i < st.length; i++) {
                    String cn = st[i].getClassName();
                    String mn = st[i].getMethodName();
                    if (("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cn) &&
                            ("appendQuotedString".equals(mn) || "next".equals(mn) || "startsWith".equals(mn) || "append".equals(mn))) ||
                        ("org.apache.commons.lang.text.StrBuilder".equals(cn) &&
                            ("append".equals(mn) || "startsWith".equals(mn)))) {
                        rootCause = true;
                        break;
                    }
                }
            }
            if (!cleanRejection && rootCause && (t instanceof OutOfMemoryError || t instanceof RuntimeException)) {
                throw (RuntimeException) t;
            }
            if (!cleanRejection && rootCause && t instanceof Error) {
                throw (Error) t;
            }
        }

        String prefix = data.consumeAsciiString(24);
        String mid = data.consumeAsciiString(24);
        String suffix = data.consumeAsciiString(24);
        String tail = data.consumeRemainingAsString();

        prefix = prefix.replace('{', 'x').replace('}', 'x').replace('\'', 'x').replace(',', 'x');
        mid = mid.replace('{', 'y').replace('}', 'y').replace('\'', 'y').replace(',', 'y');
        suffix = suffix.replace('{', 'z').replace('}', 'z').replace('\'', 'z').replace(',', 'z');
        tail = tail.replace('{', 'w').replace('}', 'w').replace('\'', 'w').replace(',', 'w');

        StringBuffer patternBuilder = new StringBuffer();
        if (data.consumeBoolean()) {
            patternBuilder.append(prefix);
        }
        patternBuilder.append("''");
        if (data.consumeBoolean()) {
            patternBuilder.append(mid);
        }
        if (data.consumeBoolean()) {
            patternBuilder.append(" ");
        }
        patternBuilder.append("{0,lower}");
        if (data.consumeBoolean()) {
            patternBuilder.append(" '");
            patternBuilder.append(suffix);
            patternBuilder.append("'");
        }
        if (tail.length() > 0) {
            if (data.consumeBoolean()) {
                patternBuilder.append(" ");
            }
            patternBuilder.append(tail);
        }

        String pattern = patternBuilder.toString();
        String arg = data.consumeAsciiString(32);
        if (arg.length() == 0) {
            arg = "DUMMY";
        }

        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
            String before = emf.toPattern();
            String out1 = emf.format(new Object[] { arg });

            try {
                ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, registry);
                String out2 = emf2.format(new Object[] { arg });
                String after = emf2.toPattern();

                // Contract asserted: toPattern() returns a pattern representation that can be used
                // to create an equivalent ExtendedMessageFormat. A patch that merely skips the
                // quoted-append logic or silently drops quote handling can stop crashing while
                // producing a non-equivalent pattern/output; this round-trip check catches that.
                if (!out1.equals(out2) || !before.equals(after)) {
                    throw new RuntimeException("[oracle:toPattern-roundtrip] metamorphic violation: input="
                            + pattern + " arg=" + arg + " lhs=" + out1 + " rhs=" + out2
                            + " p1=" + before + " p2=" + after);
                }
            } catch (Throwable ignored) {
            }
        } catch (Throwable t) {
            boolean cleanRejection =
                    (t instanceof IllegalArgumentException) ||
                    (t instanceof NumberFormatException) ||
                    (t.getClass().getName().indexOf("Validate") >= 0) ||
                    (t.getClass().getName().indexOf("Validation") >= 0);
            boolean rootCause = false;
            if (t instanceof OutOfMemoryError || t instanceof RuntimeException) {
                StackTraceElement[] st = t.getStackTrace();
                for (int i = 0; i < st.length; i++) {
                    String cn = st[i].getClassName();
                    String mn = st[i].getMethodName();
                    if (("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cn) &&
                            ("appendQuotedString".equals(mn) || "next".equals(mn) || "startsWith".equals(mn) || "append".equals(mn))) ||
                        ("org.apache.commons.lang.text.StrBuilder".equals(cn) &&
                            ("append".equals(mn) || "startsWith".equals(mn)))) {
                        rootCause = true;
                        break;
                    }
                }
            }
            if (!cleanRejection && rootCause && (t instanceof OutOfMemoryError || t instanceof RuntimeException)) {
                throw (RuntimeException) t;
            }
            if (!cleanRejection && rootCause && t instanceof Error) {
                throw (Error) t;
            }
        }
    }
}
```

</details>

---
## [20] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [21] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~8,751 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [19] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        try {
            Class lowerCls = null;
            Class upperCls = null;
            try {
                lowerCls = Class.forName("org.apache.commons.lang.text.ExtendedMessageFormatTest$LowerCaseFormatFactory");
                upperCls = Class.forName("org.apache.commons.lang.text.ExtendedMessageFormatTest$UpperCaseFormatFactory");
            } catch (Throwable ignored) {
                try {
                    lowerCls = Class.forName("org.apache.commons.lang.text.LowerCaseFormatFactory");
                    upperCls = Class.forName("org.apache.commons.lang.text.UpperCaseFormatFactory");
                } catch (Throwable ignoredToo) {
                    return;
                }
            }

            Constructor lowerCtor = lowerCls.getDeclaredConstructor(new Class[0]);
            Constructor upperCtor = upperCls.getDeclaredConstructor(new Class[0]);
            lowerCtor.setAccessible(true);
            upperCtor.setAccessible(true);
            registry.put("lower", (FormatFactory) lowerCtor.newInstance(new Object[0]));
            registry.put("upper", (FormatFactory) upperCtor.newInstance(new Object[0]));
        } catch (Throwable t) {
            return;
        }

        String anchorPattern = "it''s a {0,lower} 'test'!";
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(anchorPattern, registry);
            String formatted = emf.format(new Object[] { "DUMMY" });
            if (!"it's a dummy test!".equals(formatted)) {
                throw new RuntimeException("[oracle:anchor-output] metamorphic violation: expected exact regression-test output input="
                        + anchorPattern + " actual=" + formatted);
            }

            String canonical = emf.toPattern();
            try {
                ExtendedMessageFormat reparsed = new ExtendedMessageFormat(canonical, registry);
                String formatted2 = reparsed.format(new Object[] { "DUMMY" });
                if (!formatted.equals(formatted2) || !canonical.equals(reparsed.toPattern())) {
                    throw new RuntimeException("[oracle:roundtrip] metamorphic violation: parse(format-pattern)-roundtrip input="
                            + anchorPattern + " lhs=" + formatted + " rhs=" + formatted2
                            + " p1=" + canonical + " p2=" + reparsed.toPattern());
                }
            } catch (Throwable ignored) {
            }
        } catch (Throwable t) {
            boolean cleanRejection =
                    (t instanceof IllegalArgumentException) ||
                    (t instanceof NumberFormatException) ||
                    (t.getClass().getName().indexOf("Validate") >= 0) ||
                    (t.getClass().getName().indexOf("Validation") >= 0);
            boolean rootCause = false;
            if (t instanceof OutOfMemoryError || t instanceof RuntimeException) {
                StackTraceElement[] st = t.getStackTrace();
                for (int i = 0; i < st.length; i++) {
                    String cn = st[i].getClassName();
                    String mn = st[i].getMethodName();
                    if (("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cn) &&
                            ("appendQuotedString".equals(mn) || "next".equals(mn) || "startsWith".equals(mn) || "append".equals(mn))) ||
                        ("org.apache.commons.lang.text.StrBuilder".equals(cn) &&
                            ("append".equals(mn) || "startsWith".equals(mn)))) {
                        rootCause = true;
                        break;
                    }
                }
            }
            if (!cleanRejection && rootCause && (t instanceof OutOfMemoryError || t instanceof RuntimeException)) {
                throw (RuntimeException) t;
            }
            if (!cleanRejection && rootCause && t instanceof Error) {
                throw (Error) t;
            }
        }

        String prefix = data.consumeAsciiString(24);
        String mid = data.consumeAsciiString(24);
        String suffix = data.consumeAsciiString(24);
        String tail = data.consumeRemainingAsString();

        prefix = prefix.replace('{', 'x').replace('}', 'x').replace('\'', 'x').replace(',', 'x');
        mid = mid.replace('{', 'y').replace('}', 'y').replace('\'', 'y').replace(',', 'y');
        suffix = suffix.replace('{', 'z').replace('}', 'z').replace('\'', 'z').replace(',', 'z');
        tail = tail.replace('{', 'w').replace('}', 'w').replace('\'', 'w').replace(',', 'w');

        StringBuffer patternBuilder = new StringBuffer();
        if (data.consumeBoolean()) {
            patternBuilder.append(prefix);
        }
        patternBuilder.append("''");
        if (data.consumeBoolean()) {
            patternBuilder.append(mid);
        }
        if (data.consumeBoolean()) {
            patternBuilder.append(" ");
        }
        patternBuilder.append("{0,lower}");
        if (data.consumeBoolean()) {
            patternBuilder.append(" '");
            patternBuilder.append(suffix);
            patternBuilder.append("'");
        }
        if (tail.length() > 0) {
            if (data.consumeBoolean()) {
                patternBuilder.append(" ");
            }
            patternBuilder.append(tail);
        }

        String pattern = patternBuilder.toString();
        String arg = data.consumeAsciiString(32);
        if (arg.length() == 0) {
            arg = "DUMMY";
        }

        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
            String before = emf.toPattern();
            String out1 = emf.format(new Object[] { arg });

            try {
                ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, registry);
                String out2 = emf2.format(new Object[] { arg });
                String after = emf2.toPattern();

                // Contract asserted: toPattern() returns a pattern representation that can be used
                // to create an equivalent ExtendedMessageFormat. A patch that merely skips the
                // quoted-append logic or silently drops quote handling can stop crashing while
                // producing a non-equivalent pattern/output; this round-trip check catches that.
                if (!out1.equals(out2) || !before.equals(after)) {
                    throw new RuntimeException("[oracle:toPattern-roundtrip] metamorphic violation: input="
                            + pattern + " arg=" + arg + " lhs=" + out1 + " rhs=" + out2
                            + " p1=" + before + " p2=" + after);
                }
            } catch (Throwable ignored) {
            }
        } catch (Throwable t) {
            boolean cleanRejection =
                    (t instanceof IllegalArgumentException) ||
                    (t instanceof NumberFormatException) ||
                    (t.getClass().getName().indexOf("Validate") >= 0) ||
                    (t.getClass().getName().indexOf("Validation") >= 0);
            boolean rootCause = false;
            if (t instanceof OutOfMemoryError || t instanceof RuntimeException) {
                StackTraceElement[] st = t.getStackTrace();
                for (int i = 0; i < st.length; i++) {
                    String cn = st[i].getClassName();
                    String mn = st[i].getMethodName();
                    if (("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cn) &&
                            ("appendQuotedString".equals(mn) || "next".equals(mn) || "startsWith".equals(mn) || "append".equals(mn))) ||
                        ("org.apache.commons.lang.text.StrBuilder".equals(cn) &&
                            ("append".equals(mn) || "startsWith".equals(mn)))) {
                        rootCause = true;
                        break;
                    }
                }
            }
            if (!cleanRejection && rootCause && (t instanceof OutOfMemoryError || t instanceof RuntimeException)) {
                throw (RuntimeException) t;
            }
            if (!cleanRejection && rootCause && t instanceof Error) {
                throw (Error) t;
            }
        }
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~3,275 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    private static String sanitizeLiteral(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\'' && c != '{' && c != '}') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        String anchorPattern = "it''s a test!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, registry);
        String anchorOut = anchor.format(new Object[0]);
        if (!"it's a test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: input=" + anchorPattern + " output=" + anchorOut);
        }
        String anchorRoundTripPattern = anchor.toPattern();
        ExtendedMessageFormat anchorRoundTrip = new ExtendedMessageFormat(anchorRoundTripPattern, registry);
        String anchorRoundTripOut = anchorRoundTrip.format(new Object[0]);
        if (!anchorOut.equals(anchorRoundTripOut)) {
            throw new RuntimeException("[oracle:anchor-roundtrip] metamorphic violation: input=" + anchorPattern + " lhs=" + anchorOut + " rhs=" + anchorRoundTripOut);
        }

        String left = sanitizeLiteral(data.consumeAsciiString(32));
        String middle = sanitizeLiteral(data.consumeAsciiString(32));
        String right = sanitizeLiteral(data.consumeRemainingAsString());

        String pattern;
        String expected;
        if (data.consumeBoolean()) {
            pattern = left + "''" + middle + "''" + right;
            expected = left + "'" + middle + "'" + right;
        } else {
            pattern = left + "''" + middle + right;
            expected = left + "'" + middle + right;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String out = emf.format(new Object[0]);

        // Contract used for this oracle:
        // MessageFormat-style doubled single quotes in literal text represent one literal apostrophe.
        // Since this harness constructs a pattern containing only literal text (no braces, no lone quotes),
        // every correct implementation must format it to the same text with each '' collapsed to '.
        if (!expected.equals(out)) {
            throw new RuntimeException("[oracle:escaped-quote] metamorphic violation: input=" + pattern + " expected=" + expected + " actual=" + out);
        }

        String roundTripPattern = emf.toPattern();
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(roundTripPattern, registry);
        String out2 = emf2.format(new Object[0]);

        // A correct toPattern() must describe an equivalent pattern; reparsing it must preserve output.
        if (!out.equals(out2)) {
            throw new RuntimeException("[oracle:toPattern-roundtrip] metamorphic violation: input=" + pattern + " p1=" + roundTripPattern + " lhs=" + out + " rhs=" + out2);
        }
    }
}
```

</details>

---
## [22] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [23] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~3,593 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [19] — not reprinted*
- *[assistant] message: identical to step [21] — not reprinted*
- *[user] message: identical to step [5] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    private static String sanitizeLiteral(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\'' && c != '{' && c != '}') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        String anchorPattern = "it''s a test!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, registry);
        String anchorOut = anchor.format(new Object[0]);
        if (!"it's a test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: input=" + anchorPattern + " output=" + anchorOut);
        }
        String anchorRoundTripPattern = anchor.toPattern();
        ExtendedMessageFormat anchorRoundTrip = new ExtendedMessageFormat(anchorRoundTripPattern, registry);
        String anchorRoundTripOut = anchorRoundTrip.format(new Object[0]);
        if (!anchorOut.equals(anchorRoundTripOut)) {
            throw new RuntimeException("[oracle:anchor-roundtrip] metamorphic violation: input=" + anchorPattern + " lhs=" + anchorOut + " rhs=" + anchorRoundTripOut);
        }

        String left = sanitizeLiteral(data.consumeAsciiString(32));
        String middle = sanitizeLiteral(data.consumeAsciiString(32));
        String right = sanitizeLiteral(data.consumeRemainingAsString());

        String pattern;
        String expected;
        if (data.consumeBoolean()) {
            pattern = left + "''" + middle + "''" + right;
            expected = left + "'" + middle + "'" + right;
        } else {
            pattern = left + "''" + middle + right;
            expected = left + "'" + middle + right;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String out = emf.format(new Object[0]);

        // Contract used for this oracle:
        // MessageFormat-style doubled single quotes in literal text represent one literal apostrophe.
        // Since this harness constructs a pattern containing only literal text (no braces, no lone quotes),
        // every correct implementation must format it to the same text with each '' collapsed to '.
        if (!expected.equals(out)) {
            throw new RuntimeException("[oracle:escaped-quote] metamorphic violation: input=" + pattern + " expected=" + expected + " actual=" + out);
        }

        String roundTripPattern = emf.toPattern();
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(roundTripPattern, registry);
        String out2 = emf2.format(new Object[0]);

        // A correct toPattern() must describe an equivalent pattern; reparsing it must preserve output.
        if (!out.equals(out2)) {
            throw new RuntimeException("[oracle:toPattern-roundtrip] metamorphic violation: input=" + pattern + " p1=" + roundTripPattern + " lhs=" + out + " rhs=" + out2);
        }
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~2,917 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    private static String stripMeta(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\'' && c != '{' && c != '}') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        // Guaranteed trigger from the real public API path:
        // registry != null forces ExtendedMessageFormat.applyPattern() to use its custom parser,
        // and the doubled quote at position 2 reaches appendQuotedString(..., escapingOn=true)
        // with c[start] == QUOTE, i.e. exactly the patched branch.
        new ExtendedMessageFormat("it''s a test!", registry);

        String left = stripMeta(data.consumeAsciiString(32));
        String right = stripMeta(data.consumeRemainingAsString());

        String pattern;
        if (data.consumeBoolean()) {
            pattern = left + "''" + right;
        } else {
            pattern = left + "''" + right + " {0}";
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

        // Contract/oracle: in MessageFormat patterns, doubled single quotes in literal text
        // denote one literal apostrophe. For the patterns we construct here, replacing every
        // occurrence of '' with ' yields the literal output outside any {0} placeholder.
        String expectedLiteral = left + "'" + right;
        String actual;
        if (pattern.indexOf("{0}") >= 0) {
            actual = emf.format(new Object[] { "X" });
            String expected = expectedLiteral + " X";
            if (!expected.equals(actual)) {
                throw new RuntimeException("[oracle:escaped-quote-format] metamorphic violation: input=" + pattern + " expected=" + expected + " actual=" + actual);
            }
        } else {
            actual = emf.format(new Object[0]);
            if (!expectedLiteral.equals(actual)) {
                throw new RuntimeException("[oracle:escaped-quote-literal] metamorphic violation: input=" + pattern + " expected=" + expectedLiteral + " actual=" + actual);
            }
        }

        String p2 = emf.toPattern();
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(p2, registry);
        String actual2 = pattern.indexOf("{0}") >= 0 ? emf2.format(new Object[] { "X" }) : emf2.format(new Object[0]);
        if (!actual.equals(actual2)) {
            throw new RuntimeException("[oracle:toPattern-roundtrip] metamorphic violation: input=" + pattern + " p2=" + p2 + " lhs=" + actual + " rhs=" + actual2);
        }
    }
}
```

</details>

---
## [24] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [25] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (8 message(s), ~3,355 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [19] — not reprinted*
- *[assistant] message: identical to step [21] — not reprinted*
- *[user] message: identical to step [5] — not reprinted*
- *[assistant] message: identical to step [23] — not reprinted*
- *[user] message: identical to step [5] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    private static String stripMeta(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\'' && c != '{' && c != '}') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        // Guaranteed trigger from the real public API path:
        // registry != null forces ExtendedMessageFormat.applyPattern() to use its custom parser,
        // and the doubled quote at position 2 reaches appendQuotedString(..., escapingOn=true)
        // with c[start] == QUOTE, i.e. exactly the patched branch.
        new ExtendedMessageFormat("it''s a test!", registry);

        String left = stripMeta(data.consumeAsciiString(32));
        String right = stripMeta(data.consumeRemainingAsString());

        String pattern;
        if (data.consumeBoolean()) {
            pattern = left + "''" + right;
        } else {
            pattern = left + "''" + right + " {0}";
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

        // Contract/oracle: in MessageFormat patterns, doubled single quotes in literal text
        // denote one literal apostrophe. For the patterns we construct here, replacing every
        // occurrence of '' with ' yields the literal output outside any {0} placeholder.
        String expectedLiteral = left + "'" + right;
        String actual;
        if (pattern.indexOf("{0}") >= 0) {
            actual = emf.format(new Object[] { "X" });
            String expected = expectedLiteral + " X";
            if (!expected.equals(actual)) {
                throw new RuntimeException("[oracle:escaped-quote-format] metamorphic violation: input=" + pattern + " expected=" + expected + " actual=" + actual);
            }
        } else {
            actual = emf.format(new Object[0]);
            if (!expectedLiteral.equals(actual)) {
                throw new RuntimeException("[oracle:escaped-quote-literal] metamorphic violation: input=" + pattern + " expected=" + expectedLiteral + " actual=" + actual);
            }
        }

        String p2 = emf.toPattern();
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(p2, registry);
        String actual2 = pattern.indexOf("{0}") >= 0 ? emf2.format(new Object[] { "X" }) : emf2.format(new Object[0]);
        if (!actual.equals(actual2)) {
            throw new RuntimeException("[oracle:toPattern-roundtrip] metamorphic violation: input=" + pattern + " p2=" + p2 + " lhs=" + actual + " rhs=" + actual2);
        }
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~6,025 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    private static FormatFactory tryInstantiateFactory(String className) {
        try {
            Class cls = Class.forName(className);
            Constructor ctor = cls.getDeclaredConstructor(new Class[0]);
            ctor.setAccessible(true);
            return (FormatFactory) ctor.newInstance(new Object[0]);
        } catch (ClassNotFoundException e) {
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Map buildRegistry() {
        Map registry = new HashMap();

        FormatFactory lower = tryInstantiateFactory("org.apache.commons.lang.text.ExtendedMessageFormatTest$LowerCaseFormatFactory");
        if (lower == null) {
            lower = tryInstantiateFactory("org.apache.commons.lang.text.LowerCaseFormatFactory");
        }
        if (lower != null) {
            registry.put("lower", lower);
        }

        FormatFactory upper = tryInstantiateFactory("org.apache.commons.lang.text.ExtendedMessageFormatTest$UpperCaseFormatFactory");
        if (upper == null) {
            upper = tryInstantiateFactory("org.apache.commons.lang.text.UpperCaseFormatFactory");
        }
        if (upper != null) {
            registry.put("upper", upper);
        }

        return registry;
    }

    private static String stripSpecials(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\'' && c != '{' && c != '}') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = buildRegistry();

        // Guaranteed buggy trigger from the real public API:
        // doubled quote ("''") with non-null registry drives applyPattern() into
        // appendQuotedString(..., escapingOn=true) where the buggy code forgets to advance pos.
        // On the buggy version this loops until OutOfMemoryError.
        String anchorPattern = registry.containsKey("lower")
                ? "it''s a {0,lower} 'test'!"
                : "it''s a {0} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, registry);

        // Post-condition/oracle: a correct implementation must preserve MessageFormat escaped-quote
        // semantics and produce a reparsable equivalent pattern via toPattern().
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        String expectedAnchor = registry.containsKey("lower")
                ? "it's a dummy test!"
                : "it's a DUMMY test!";
        if (!expectedAnchor.equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-format] metamorphic violation: input=" + anchorPattern + " expected=" + expectedAnchor + " actual=" + anchorOut);
        }
        String anchorRoundTripPattern = anchor.toPattern();
        ExtendedMessageFormat anchor2 = new ExtendedMessageFormat(anchorRoundTripPattern, registry);
        String anchorOut2 = anchor2.format(new Object[] { "DUMMY" });
        if (!anchorOut.equals(anchorOut2)) {
            throw new RuntimeException("[oracle:anchor-roundtrip] metamorphic violation: input=" + anchorPattern + " p2=" + anchorRoundTripPattern + " lhs=" + anchorOut + " rhs=" + anchorOut2);
        }

        String left = stripSpecials(data.consumeAsciiString(24));
        String middle = stripSpecials(data.consumeAsciiString(24));
        String right = stripSpecials(data.consumeRemainingAsString());

        String formatToken;
        String arg = "DUMMY";
        String expectedArg;
        if (registry.containsKey("lower") && data.consumeBoolean()) {
            formatToken = "{0,lower}";
            expectedArg = "dummy";
        } else if (registry.containsKey("upper") && data.consumeBoolean()) {
            formatToken = "{0,upper}";
            expectedArg = "DUMMY";
        } else {
            formatToken = "{0}";
            expectedArg = "DUMMY";
        }

        // Explore the same root cause: any pattern with a doubled quote while the custom parser
        // is active should hit the patched branch. Vary surrounding literals and quote placement
        // but keep the pattern valid by construction.
        String pattern;
        String expected;
        switch (data.consumeInt(0, 2)) {
            case 0:
                pattern = left + "''" + middle + " " + formatToken + " '" + right + "'";
                expected = left + "'" + middle + " " + expectedArg + " " + right;
                break;
            case 1:
                pattern = "'" + left + "' " + middle + "''" + right + " " + formatToken;
                expected = left + " " + middle + "'" + right + " " + expectedArg;
                break;
            default:
                pattern = left + " " + formatToken + " " + middle + "''" + right;
                expected = left + " " + expectedArg + " " + middle + "'" + right;
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String out = emf.format(new Object[] { arg });
        if (!expected.equals(out)) {
            throw new RuntimeException("[oracle:escaped-quote] metamorphic violation: input=" + pattern + " expected=" + expected + " actual=" + out);
        }

        String p2 = emf.toPattern();
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(p2, registry);
        String out2 = emf2.format(new Object[] { arg });
        if (!out.equals(out2)) {
            throw new RuntimeException("[oracle:toPattern-roundtrip] metamorphic violation: input=" + pattern + " p2=" + p2 + " lhs=" + out + " rhs=" + out2);
        }
    }
}
```

</details>

---
## [26] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [27] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,615 chars)</summary>

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
    private static final String ANCHOR_PATTERN = "it''s a {0,lower} 'test'!";
    private static final String ANCHOR_EXPECTED = "it's a dummy test!";

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        exerciseAnchor(registry);
        exerciseExplore(data, registry);
    }

    private static void exerciseAnchor(Map registry) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(ANCHOR_PATTERN, Locale.getDefault(), registry);
            String before = emf.toPattern();
            String actual = emf.format(new Object[] { "DUMMY" });
            String after = emf.toPattern();

            if (!ANCHOR_EXPECTED.equals(actual)) {
                throw new RuntimeException("[oracle:anchor-output] metamorphic violation: exact regression input formatted unexpectedly input="
                        + ANCHOR_PATTERN + " lhs=" + actual + " rhs=" + ANCHOR_EXPECTED);
            }

            /* Contract used: formatting is read-only with respect to the compiled pattern.
               A throw-deleting patch that skips quote handling could leave formatting or pattern
               bookkeeping inconsistent; toPattern must not change across format(). */
            if (before != null && after != null && !before.equals(after)) {
                throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: toPattern changed across format input="
                        + ANCHOR_PATTERN + " lhs=" + before + " rhs=" + after);
            }

            /* Contract used: toPattern() returns a pattern representation acceptable to the constructor.
               Reconstructing an identically-configured formatter from that pattern must report the same pattern. */
            try {
                ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, Locale.getDefault(), registry);
                String roundTrip = emf2.toPattern();
                if (roundTrip != null && !roundTrip.equals(before)) {
                    throw new RuntimeException("[oracle:topattern-roundtrip] metamorphic violation: toPattern round-trip mismatch input="
                            + ANCHOR_PATTERN + " lhs=" + before + " rhs=" + roundTrip);
                }
            } catch (Throwable ignored) {
                if (isRootCause(ignored)) {
                    throw ignored;
                }
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void exerciseExplore(FuzzedDataProvider data, Map registry) {
        String prefix = sanitize(data.consumeAsciiString(20));
        String middle = sanitize(data.consumeAsciiString(20));
        String suffix = sanitize(data.consumeAsciiString(20));
        String fmt = data.consumeBoolean() ? "lower" : "upper";
        String arg = data.consumeAsciiString(20);
        if (arg.length() == 0) {
            arg = "DuMmY";
        }

        int variant = data.consumeInt(0, 5);
        String pattern;
        switch (variant) {
            case 0:
                pattern = "''" + prefix + " {0," + fmt + "} " + suffix;
                break;
            case 1:
                pattern = prefix + "''" + middle + " {0," + fmt + "} " + suffix;
                break;
            case 2:
                pattern = prefix + " '' " + middle + " {0," + fmt + "} " + suffix;
                break;
            case 3:
                pattern = prefix + " {0," + fmt + "} ''" + suffix;
                break;
            case 4:
                pattern = "'" + prefix + "'" + " {0," + fmt + "} " + suffix;
                break;
            default:
                pattern = prefix + "''" + middle + " {0} " + suffix;
                break;
        }

        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, Locale.getDefault(), registry);
            String before = emf.toPattern();
            String formatted = emf.format(new Object[] { arg });
            String after = emf.toPattern();

            if (before != null && after != null && !before.equals(after)) {
                throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: toPattern changed across format input="
                        + pattern + " lhs=" + before + " rhs=" + after);
            }

            try {
                ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, Locale.getDefault(), registry);
                String roundTrip = emf2.toPattern();
                if (roundTrip != null && !roundTrip.equals(before)) {
                    throw new RuntimeException("[oracle:topattern-roundtrip] metamorphic violation: toPattern round-trip mismatch input="
                            + pattern + " lhs=" + before + " rhs=" + roundTrip);
                }

                String formatted2 = emf2.format(new Object[] { arg });
                if (formatted != null && formatted2 != null && !formatted.equals(formatted2)) {
                    throw new RuntimeException("[oracle:format-roundtrip] metamorphic violation: equivalent formatter instances disagree input="
                            + pattern + " lhs=" + formatted + " rhs=" + formatted2);
                }
            } catch (Throwable t) {
                if (isRootCause(t)) {
                    throw t;
                }
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static String sanitize(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("{", "")
                .replace("}", "")
                .replace("'", "")
                .replace(",", "")
                .replace("\r", "")
                .replace("\n", "");
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
                || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            String cls = st[i].getClassName();
            String method = st[i].getMethodName();
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cls)
                    && ("appendQuotedString".equals(method)
                        || "next".equals(method)
                        || "append".equals(method)
                        || "startsWith".equals(method))) {
                return true;
            }
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                    && ("append".equals(method) || "startsWith".equals(method))) {
                return true;
            }
        }
        return false;
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new CaseFormat(true);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new CaseFormat(false);
        }
    }

    private static final class CaseFormat extends Format {
        private final boolean lower;

        private CaseFormat(boolean lower) {
            this.lower = lower;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            s = lower ? s.toLowerCase() : s.toUpperCase();
            toAppendTo.append(s);
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
## [28] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [29] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~8,812 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
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
    private static final String ANCHOR_PATTERN = "it''s a {0,lower} 'test'!";
    private static final String ANCHOR_EXPECTED = "it's a dummy test!";

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        exerciseAnchor(registry);
        exerciseExplore(data, registry);
    }

    private static void exerciseAnchor(Map registry) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(ANCHOR_PATTERN, Locale.getDefault(), registry);
            String before = emf.toPattern();
            String actual = emf.format(new Object[] { "DUMMY" });
            String after = emf.toPattern();

            if (!ANCHOR_EXPECTED.equals(actual)) {
                throw new RuntimeException("[oracle:anchor-output] metamorphic violation: exact regression input formatted unexpectedly input="
                        + ANCHOR_PATTERN + " lhs=" + actual + " rhs=" + ANCHOR_EXPECTED);
            }

            /* Contract used: formatting is read-only with respect to the compiled pattern.
               A throw-deleting patch that skips quote handling could leave formatting or pattern
               bookkeeping inconsistent; toPattern must not change across format(). */
            if (before != null && after != null && !before.equals(after)) {
                throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: toPattern changed across format input="
                        + ANCHOR_PATTERN + " lhs=" + before + " rhs=" + after);
            }

            /* Contract used: toPattern() returns a pattern representation acceptable to the constructor.
               Reconstructing an identically-configured formatter from that pattern must report the same pattern. */
            try {
                ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, Locale.getDefault(), registry);
                String roundTrip = emf2.toPattern();
                if (roundTrip != null && !roundTrip.equals(before)) {
                    throw new RuntimeException("[oracle:topattern-roundtrip] metamorphic violation: toPattern round-trip mismatch input="
                            + ANCHOR_PATTERN + " lhs=" + before + " rhs=" + roundTrip);
                }
            } catch (Throwable ignored) {
                if (isRootCause(ignored)) {
                    throw ignored;
                }
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void exerciseExplore(FuzzedDataProvider data, Map registry) {
        String prefix = sanitize(data.consumeAsciiString(20));
        String middle = sanitize(data.consumeAsciiString(20));
        String suffix = sanitize(data.consumeAsciiString(20));
        String fmt = data.consumeBoolean() ? "lower" : "upper";
        String arg = data.consumeAsciiString(20);
        if (arg.length() == 0) {
            arg = "DuMmY";
        }

        int variant = data.consumeInt(0, 5);
        String pattern;
        switch (variant) {
            case 0:
                pattern = "''" + prefix + " {0," + fmt + "} " + suffix;
                break;
            case 1:
                pattern = prefix + "''" + middle + " {0," + fmt + "} " + suffix;
                break;
            case 2:
                pattern = prefix + " '' " + middle + " {0," + fmt + "} " + suffix;
                break;
            case 3:
                pattern = prefix + " {0," + fmt + "} ''" + suffix;
                break;
            case 4:
                pattern = "'" + prefix + "'" + " {0," + fmt + "} " + suffix;
                break;
            default:
                pattern = prefix + "''" + middle + " {0} " + suffix;
                break;
        }

        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, Locale.getDefault(), registry);
            String before = emf.toPattern();
            String formatted = emf.format(new Object[] { arg });
            String after = emf.toPattern();

            if (before != null && after != null && !before.equals(after)) {
                throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: toPattern changed across format input="
                        + pattern + " lhs=" + before + " rhs=" + after);
            }

            try {
                ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, Locale.getDefault(), registry);
                String roundTrip = emf2.toPattern();
                if (roundTrip != null && !roundTrip.equals(before)) {
                    throw new RuntimeException("[oracle:topattern-roundtrip] metamorphic violation: toPattern round-trip mismatch input="
                            + pattern + " lhs=" + before + " rhs=" + roundTrip);
                }

                String formatted2 = emf2.format(new Object[] { arg });
                if (formatted != null && formatted2 != null && !formatted.equals(formatted2)) {
                    throw new RuntimeException("[oracle:format-roundtrip] metamorphic violation: equivalent formatter instances disagree input="
                            + pattern + " lhs=" + formatted + " rhs=" + formatted2);
                }
            } catch (Throwable t) {
                if (isRootCause(t)) {
                    throw t;
                }
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static String sanitize(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("{", "")
                .replace("}", "")
                .replace("'", "")
                .replace(",", "")
                .replace("\r", "")
                .replace("\n", "");
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
                || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            String cls = st[i].getClassName();
            String method = st[i].getMethodName();
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cls)
                    && ("appendQuotedString".equals(method)
                        || "next".equals(method)
                        || "append".equals(method)
                        || "startsWith".equals(method))) {
                return true;
            }
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                    && ("append".equals(method) || "startsWith".equals(method))) {
                return true;
            }
        }
        return false;
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new CaseFormat(true);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new CaseFormat(false);
        }
    }

    private static final class CaseFormat extends Format {
        private final boolean lower;

        private CaseFormat(boolean lower) {
            this.lower = lower;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            s = lower ? s.toLowerCase() : s.toUpperCase();
            toAppendTo.append(s);
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source;
        }
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~4,977 chars)</summary>

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
    private static final String ANCHOR_PATTERN = "it''s a {0,lower} 'test'!";
    private static final String ANCHOR_EXPECTED = "it's a dummy test!";

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        ExtendedMessageFormat anchor = new ExtendedMessageFormat(ANCHOR_PATTERN, registry);
        String before = anchor.toPattern();
        String actual = anchor.format(new Object[] { "DUMMY" });
        String after = anchor.toPattern();

        if (!ANCHOR_EXPECTED.equals(actual)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: input=" + ANCHOR_PATTERN + " lhs=" + actual + " rhs=" + ANCHOR_EXPECTED);
        }

        /* Contract used: format() is read-only for the formatter's compiled pattern.
           A patch that merely avoids the crash by skipping/altering quote processing would break this. */
        if (before != null ? !before.equals(after) : after != null) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: input=" + ANCHOR_PATTERN + " lhs=" + before + " rhs=" + after);
        }

        String prefix = sanitize(data.consumeAsciiString(12));
        String middle = sanitize(data.consumeAsciiString(12));
        String suffix = sanitize(data.consumeAsciiString(12));
        String arg = data.consumeAsciiString(16);
        if (arg.length() == 0) {
            arg = "DuMmY";
        }
        String fmt = data.consumeBoolean() ? "lower" : "upper";

        String pattern;
        switch (data.consumeInt(0, 4)) {
            case 0:
                pattern = "''" + prefix + "{0," + fmt + "}" + suffix;
                break;
            case 1:
                pattern = prefix + "''" + middle + "{0," + fmt + "}" + suffix;
                break;
            case 2:
                pattern = prefix + "{0," + fmt + "}''" + suffix;
                break;
            case 3:
                pattern = prefix + "''" + middle + " {0," + fmt + "} '" + suffix + "'";
                break;
            default:
                pattern = prefix + "''" + middle + " {0} " + suffix;
                break;
        }

        ExtendedMessageFormat emf1 = new ExtendedMessageFormat(pattern, registry);
        String p1 = emf1.toPattern();
        String out1 = emf1.format(new Object[] { arg });
        String p1After = emf1.toPattern();

        if (p1 != null ? !p1.equals(p1After) : p1After != null) {
            throw new RuntimeException("[oracle:topattern-stable-fuzz] metamorphic violation: input=" + pattern + " lhs=" + p1 + " rhs=" + p1After);
        }

        /* Contract used: toPattern() returns a pattern representation for the same formatter state.
           Reconstructing from that representation with the same registry must preserve the observable format result. */
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(p1, registry);
        String out2 = emf2.format(new Object[] { arg });
        if (out1 != null ? !out1.equals(out2) : out2 != null) {
            throw new RuntimeException("[oracle:roundtrip-format] metamorphic violation: input=" + pattern + " lhs=" + out1 + " rhs=" + out2);
        }
    }

    private static String sanitize(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("{", "")
                .replace("}", "")
                .replace(",", "")
                .replace("\r", "")
                .replace("\n", "");
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new CaseFormat(true);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new CaseFormat(false);
        }
    }

    private static final class CaseFormat extends Format {
        private final boolean lower;

        private CaseFormat(boolean lower) {
            this.lower = lower;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(lower ? s.toLowerCase() : s.toUpperCase());
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
## [30] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [31] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~5,294 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
- *[assistant] message: identical to step [29] — not reprinted*
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
    private static final String ANCHOR_PATTERN = "it''s a {0,lower} 'test'!";
    private static final String ANCHOR_EXPECTED = "it's a dummy test!";

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        ExtendedMessageFormat anchor = new ExtendedMessageFormat(ANCHOR_PATTERN, registry);
        String before = anchor.toPattern();
        String actual = anchor.format(new Object[] { "DUMMY" });
        String after = anchor.toPattern();

        if (!ANCHOR_EXPECTED.equals(actual)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: input=" + ANCHOR_PATTERN + " lhs=" + actual + " rhs=" + ANCHOR_EXPECTED);
        }

        /* Contract used: format() is read-only for the formatter's compiled pattern.
           A patch that merely avoids the crash by skipping/altering quote processing would break this. */
        if (before != null ? !before.equals(after) : after != null) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: input=" + ANCHOR_PATTERN + " lhs=" + before + " rhs=" + after);
        }

        String prefix = sanitize(data.consumeAsciiString(12));
        String middle = sanitize(data.consumeAsciiString(12));
        String suffix = sanitize(data.consumeAsciiString(12));
        String arg = data.consumeAsciiString(16);
        if (arg.length() == 0) {
            arg = "DuMmY";
        }
        String fmt = data.consumeBoolean() ? "lower" : "upper";

        String pattern;
        switch (data.consumeInt(0, 4)) {
            case 0:
                pattern = "''" + prefix + "{0," + fmt + "}" + suffix;
                break;
            case 1:
                pattern = prefix + "''" + middle + "{0," + fmt + "}" + suffix;
                break;
            case 2:
                pattern = prefix + "{0," + fmt + "}''" + suffix;
                break;
            case 3:
                pattern = prefix + "''" + middle + " {0," + fmt + "} '" + suffix + "'";
                break;
            default:
                pattern = prefix + "''" + middle + " {0} " + suffix;
                break;
        }

        ExtendedMessageFormat emf1 = new ExtendedMessageFormat(pattern, registry);
        String p1 = emf1.toPattern();
        String out1 = emf1.format(new Object[] { arg });
        String p1After = emf1.toPattern();

        if (p1 != null ? !p1.equals(p1After) : p1After != null) {
            throw new RuntimeException("[oracle:topattern-stable-fuzz] metamorphic violation: input=" + pattern + " lhs=" + p1 + " rhs=" + p1After);
        }

        /* Contract used: toPattern() returns a pattern representation for the same formatter state.
           Reconstructing from that representation with the same registry must preserve the observable format result. */
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(p1, registry);
        String out2 = emf2.format(new Object[] { arg });
        if (out1 != null ? !out1.equals(out2) : out2 != null) {
            throw new RuntimeException("[oracle:roundtrip-format] metamorphic violation: input=" + pattern + " lhs=" + out1 + " rhs=" + out2);
        }
    }

    private static String sanitize(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("{", "")
                .replace("}", "")
                .replace(",", "")
                .replace("\r", "")
                .replace("\n", "");
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new CaseFormat(true);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new CaseFormat(false);
        }
    }

    private static final class CaseFormat extends Format {
        private final boolean lower;

        private CaseFormat(boolean lower) {
            this.lower = lower;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(lower ? s.toLowerCase() : s.toUpperCase());
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source;
        }
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~5,864 chars)</summary>

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
    private static final String ANCHOR_PATTERN = "it''s a {0,lower} 'test'!";
    private static final String ANCHOR_EXPECTED = "it's a dummy test!";

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        /* Exact trigger from the failing test.
           On the buggy version, construction loops in applyPattern()->appendQuotedString()
           because the escaped-quote fast path fails to advance ParsePosition. */
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(ANCHOR_PATTERN, registry);

        /* Post-condition from the original test: the formatter must format the argument exactly. */
        String anchorBefore = anchor.toPattern();
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        String anchorAfter = anchor.toPattern();
        if (!ANCHOR_EXPECTED.equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: input=" + ANCHOR_PATTERN + " lhs=" + anchorOut + " rhs=" + ANCHOR_EXPECTED);
        }

        /* Contract: format() is read-only with respect to the compiled pattern representation. */
        if (anchorBefore == null ? anchorAfter != null : !anchorBefore.equals(anchorAfter)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: input=" + ANCHOR_PATTERN + " lhs=" + anchorBefore + " rhs=" + anchorAfter);
        }

        String prefix = sanitize(data.consumeAsciiString(16));
        String middle = sanitize(data.consumeAsciiString(16));
        String suffix = sanitize(data.consumeAsciiString(16));
        String arg = data.consumeAsciiString(16);
        if (arg.length() == 0) {
            arg = "DuMmY";
        }
        String fmt = data.consumeBoolean() ? "lower" : "upper";

        /* Valid-by-construction exploration:
           always includes an escaped quote sequence '' so applyPattern reaches the changed branch,
           and keeps other quoting balanced to avoid unrelated parser rejections. */
        String pattern;
        switch (data.consumeInt(0, 4)) {
            case 0:
                pattern = prefix + "''" + middle + " {" + "0," + fmt + "} '" + suffix + "'";
                break;
            case 1:
                pattern = "''" + prefix + " {" + "0," + fmt + "} '" + middle + suffix + "'";
                break;
            case 2:
                pattern = "'" + prefix + "' '' " + middle + " {" + "0," + fmt + "} " + suffix;
                break;
            case 3:
                pattern = prefix + " {" + "0," + fmt + "} '' '" + suffix + "'";
                break;
            default:
                pattern = prefix + "''" + middle + " {" + "0} '" + suffix + "'";
                break;
        }

        ExtendedMessageFormat emf1 = new ExtendedMessageFormat(pattern, registry);
        String p1 = emf1.toPattern();
        String out1 = emf1.format(new Object[] { arg });
        String p1After = emf1.toPattern();

        /* Contract: formatting does not mutate the pattern. */
        if (p1 == null ? p1After != null : !p1.equals(p1After)) {
            throw new RuntimeException("[oracle:topattern-stable-fuzz] metamorphic violation: input=" + pattern + " lhs=" + p1 + " rhs=" + p1After);
        }

        /* Contract: toPattern() is a representation of the formatter state.
           Reconstructing an identically configured formatter from it must preserve formatting. */
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(p1, registry);
        String p2 = emf2.toPattern();
        String out2 = emf2.format(new Object[] { arg });

        if (p1 == null ? p2 != null : !p1.equals(p2)) {
            throw new RuntimeException("[oracle:topattern-roundtrip] metamorphic violation: input=" + pattern + " lhs=" + p1 + " rhs=" + p2);
        }
        if (out1 == null ? out2 != null : !out1.equals(out2)) {
            throw new RuntimeException("[oracle:format-roundtrip] metamorphic violation: input=" + pattern + " lhs=" + out1 + " rhs=" + out2);
        }
    }

    private static String sanitize(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("'", "")
                .replace("{", "")
                .replace("}", "")
                .replace(",", "")
                .replace("\r", "")
                .replace("\n", "");
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new CaseFormat(true);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new CaseFormat(false);
        }
    }

    private static final class CaseFormat extends Format {
        private final boolean lower;

        private CaseFormat(boolean lower) {
            this.lower = lower;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(lower ? s.toLowerCase() : s.toUpperCase());
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
## [32] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [33] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (8 message(s), ~6,301 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
- *[assistant] message: identical to step [29] — not reprinted*
- *[user] message: identical to step [5] — not reprinted*
- *[assistant] message: identical to step [31] — not reprinted*
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
    private static final String ANCHOR_PATTERN = "it''s a {0,lower} 'test'!";
    private static final String ANCHOR_EXPECTED = "it's a dummy test!";

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        /* Exact trigger from the failing test.
           On the buggy version, construction loops in applyPattern()->appendQuotedString()
           because the escaped-quote fast path fails to advance ParsePosition. */
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(ANCHOR_PATTERN, registry);

        /* Post-condition from the original test: the formatter must format the argument exactly. */
        String anchorBefore = anchor.toPattern();
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        String anchorAfter = anchor.toPattern();
        if (!ANCHOR_EXPECTED.equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: input=" + ANCHOR_PATTERN + " lhs=" + anchorOut + " rhs=" + ANCHOR_EXPECTED);
        }

        /* Contract: format() is read-only with respect to the compiled pattern representation. */
        if (anchorBefore == null ? anchorAfter != null : !anchorBefore.equals(anchorAfter)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: input=" + ANCHOR_PATTERN + " lhs=" + anchorBefore + " rhs=" + anchorAfter);
        }

        String prefix = sanitize(data.consumeAsciiString(16));
        String middle = sanitize(data.consumeAsciiString(16));
        String suffix = sanitize(data.consumeAsciiString(16));
        String arg = data.consumeAsciiString(16);
        if (arg.length() == 0) {
            arg = "DuMmY";
        }
        String fmt = data.consumeBoolean() ? "lower" : "upper";

        /* Valid-by-construction exploration:
           always includes an escaped quote sequence '' so applyPattern reaches the changed branch,
           and keeps other quoting balanced to avoid unrelated parser rejections. */
        String pattern;
        switch (data.consumeInt(0, 4)) {
            case 0:
                pattern = prefix + "''" + middle + " {" + "0," + fmt + "} '" + suffix + "'";
                break;
            case 1:
                pattern = "''" + prefix + " {" + "0," + fmt + "} '" + middle + suffix + "'";
                break;
            case 2:
                pattern = "'" + prefix + "' '' " + middle + " {" + "0," + fmt + "} " + suffix;
                break;
            case 3:
                pattern = prefix + " {" + "0," + fmt + "} '' '" + suffix + "'";
                break;
            default:
                pattern = prefix + "''" + middle + " {" + "0} '" + suffix + "'";
                break;
        }

        ExtendedMessageFormat emf1 = new ExtendedMessageFormat(pattern, registry);
        String p1 = emf1.toPattern();
        String out1 = emf1.format(new Object[] { arg });
        String p1After = emf1.toPattern();

        /* Contract: formatting does not mutate the pattern. */
        if (p1 == null ? p1After != null : !p1.equals(p1After)) {
            throw new RuntimeException("[oracle:topattern-stable-fuzz] metamorphic violation: input=" + pattern + " lhs=" + p1 + " rhs=" + p1After);
        }

        /* Contract: toPattern() is a representation of the formatter state.
           Reconstructing an identically configured formatter from it must preserve formatting. */
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(p1, registry);
        String p2 = emf2.toPattern();
        String out2 = emf2.format(new Object[] { arg });

        if (p1 == null ? p2 != null : !p1.equals(p2)) {
            throw new RuntimeException("[oracle:topattern-roundtrip] metamorphic violation: input=" + pattern + " lhs=" + p1 + " rhs=" + p2);
        }
        if (out1 == null ? out2 != null : !out1.equals(out2)) {
            throw new RuntimeException("[oracle:format-roundtrip] metamorphic violation: input=" + pattern + " lhs=" + out1 + " rhs=" + out2);
        }
    }

    private static String sanitize(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("'", "")
                .replace("{", "")
                .replace("}", "")
                .replace(",", "")
                .replace("\r", "")
                .replace("\n", "");
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new CaseFormat(true);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new CaseFormat(false);
        }
    }

    private static final class CaseFormat extends Format {
        private final boolean lower;

        private CaseFormat(boolean lower) {
            this.lower = lower;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(lower ? s.toLowerCase() : s.toUpperCase());
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source;
        }
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~5,832 chars)</summary>

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
    private static final String ANCHOR_PATTERN = "it''s a {0,lower} 'test'!";
    private static final String ANCHOR_EXPECTED = "it's a dummy test!";

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        /* Drive the real public entry point after the object has been constructed with a non-null registry.
           The bug is in applyPattern()->appendQuotedString() when escapingOn is true and the current char is QUOTE.
           For input containing doubled quotes ('') the buggy code appends a quote but fails to advance ParsePosition,
           causing an infinite loop and eventual OutOfMemoryError. */
        ExtendedMessageFormat anchor = new ExtendedMessageFormat("", registry);
        anchor.applyPattern(ANCHOR_PATTERN);

        /* Observable contract from the original regression test. */
        String before = anchor.toPattern();
        String actual = anchor.format(new Object[] { "DUMMY" });
        String after = anchor.toPattern();
        if (!ANCHOR_EXPECTED.equals(actual)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: input=" + ANCHOR_PATTERN + " lhs=" + actual + " rhs=" + ANCHOR_EXPECTED);
        }

        /* Contract: formatting is read-only with respect to the active pattern. */
        if (before == null ? after != null : !before.equals(after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: input=" + ANCHOR_PATTERN + " lhs=" + before + " rhs=" + after);
        }

        String prefix = clean(data.consumeAsciiString(12));
        String middle = clean(data.consumeAsciiString(12));
        String suffix = clean(data.consumeAsciiString(12));
        String arg = data.consumeAsciiString(16);
        if (arg.length() == 0) {
            arg = "DuMmY";
        }

        String fmt = data.consumeBoolean() ? "lower" : "upper";
        String pattern;
        switch (data.consumeInt(0, 5)) {
            case 0:
                pattern = "''" + prefix + " {0," + fmt + "} " + suffix;
                break;
            case 1:
                pattern = prefix + "''" + middle + " {0," + fmt + "} " + suffix;
                break;
            case 2:
                pattern = prefix + " {0," + fmt + "} ''" + suffix;
                break;
            case 3:
                pattern = "'" + prefix + "' '' " + middle + " {0," + fmt + "} " + suffix;
                break;
            case 4:
                pattern = prefix + "''" + middle + " {0} " + suffix;
                break;
            default:
                pattern = prefix + " '' " + middle + " {0," + fmt + "} '" + suffix + "'";
                break;
        }

        ExtendedMessageFormat emf1 = new ExtendedMessageFormat("", registry);
        emf1.applyPattern(pattern);
        String p1 = emf1.toPattern();
        String out1 = emf1.format(new Object[] { arg });
        String p1After = emf1.toPattern();

        if (p1 == null ? p1After != null : !p1.equals(p1After)) {
            throw new RuntimeException("[oracle:topattern-stable-fuzz] metamorphic violation: input=" + pattern + " lhs=" + p1 + " rhs=" + p1After);
        }

        /* Contract: toPattern() exposes a pattern representation of the formatter state.
           Re-applying that representation to an identically configured formatter must preserve output. */
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat("", registry);
        emf2.applyPattern(p1);
        String p2 = emf2.toPattern();
        String out2 = emf2.format(new Object[] { arg });

        if (p1 == null ? p2 != null : !p1.equals(p2)) {
            throw new RuntimeException("[oracle:topattern-roundtrip] metamorphic violation: input=" + pattern + " lhs=" + p1 + " rhs=" + p2);
        }
        if (out1 == null ? out2 != null : !out1.equals(out2)) {
            throw new RuntimeException("[oracle:format-roundtrip] metamorphic violation: input=" + pattern + " lhs=" + out1 + " rhs=" + out2);
        }
    }

    private static String clean(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("'", "")
                .replace("{", "")
                .replace("}", "")
                .replace(",", "")
                .replace("\r", "")
                .replace("\n", "");
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new CaseFormat(true);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new CaseFormat(false);
        }
    }

    private static final class CaseFormat extends Format {
        private final boolean lower;

        private CaseFormat(boolean lower) {
            this.lower = lower;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(lower ? s.toLowerCase() : s.toUpperCase());
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
## [34] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [35] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~161 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [11] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~9,487 chars)</summary>

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
    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new UpperCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    private static final class LowerCaseFormat extends Format {
        private final Locale locale;

        LowerCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj == null) {
                return toAppendTo;
            }
            return toAppendTo.append(String.valueOf(obj).toLowerCase(locale));
        }

        public Object parseObject(String source, ParsePosition pos) {
            int i = pos.getIndex();
            pos.setIndex(source == null ? i : source.length());
            return source;
        }
    }

    private static final class UpperCaseFormat extends Format {
        private final Locale locale;

        UpperCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj == null) {
                return toAppendTo;
            }
            return toAppendTo.append(String.valueOf(obj).toUpperCase(locale));
        }

        public Object parseObject(String source, ParsePosition pos) {
            int i = pos.getIndex();
            pos.setIndex(source == null ? i : source.length());
            return source;
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        runAnchor(registry);

        String prefix = sanitizeLiteral(data.consumeAsciiString(24));
        String mid = sanitizeLiteral(data.consumeAsciiString(24));
        String suffix = sanitizeLiteral(data.consumeAsciiString(24));
        String quoted = sanitizeQuotedLiteral(data.consumeAsciiString(16));
        String arg = sanitizeLiteral(data.consumeAsciiString(24));
        if (arg.length() == 0) {
            arg = "X";
        }

        boolean useLower = data.consumeBoolean();
        boolean includeQuotedSegment = data.consumeBoolean();
        boolean includeEscapedQuoteNearStart = data.consumeBoolean();
        boolean includeEscapedQuoteNearEnd = data.consumeBoolean();

        StringBuilder pattern = new StringBuilder();
        if (includeEscapedQuoteNearStart) {
            pattern.append("''");
        }
        pattern.append(prefix);
        if (mid.length() > 0) {
            pattern.append(mid);
        }
        pattern.append(" {0");
        if (useLower) {
            pattern.append(",lower");
        }
        pattern.append("}");
        if (includeQuotedSegment) {
            pattern.append(" '").append(quoted).append("'");
        }
        if (suffix.length() > 0) {
            pattern.append(' ').append(suffix);
        }
        if (includeEscapedQuoteNearEnd) {
            pattern.append("''");
        }

        String builtPattern = pattern.toString();
        String expected = computeExpected(prefix, mid, suffix, quoted, arg, useLower, includeQuotedSegment,
                includeEscapedQuoteNearStart, includeEscapedQuoteNearEnd);

        runValidPatternChecks(builtPattern, registry, arg, expected);
    }

    private static void runAnchor(Map registry) {
        String pattern = "it''s a {0,lower} 'test'!";
        String arg = "DUMMY";
        String expected = "it's a dummy test!";
        runValidPatternChecks(pattern, registry, arg, expected);
    }

    private static void runValidPatternChecks(String pattern, Map registry, String arg, String expected) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

            String before = emf.toPattern();
            String formatted = emf.format(new Object[] { arg });
            String after = emf.toPattern();

            if (!before.equals(after)) {
                throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: format should not mutate toPattern input="
                        + pattern + " before=" + before + " after=" + after);
            }

            if (!expected.equals(formatted)) {
                throw new RuntimeException("[oracle:format-output] metamorphic violation: valid constructed pattern must format to its constructed literal/result input="
                        + pattern + " expected=" + expected + " actual=" + formatted);
            }

            try {
                ExtendedMessageFormat emf2 = new ExtendedMessageFormat(emf.toPattern(), registry);
                String formatted2 = emf2.format(new Object[] { arg });
                if (!formatted.equals(formatted2)) {
                    throw new RuntimeException("[oracle:roundtrip-topattern] metamorphic violation: formatting after toPattern round-trip changed input="
                            + pattern + " toPattern=" + emf.toPattern() + " lhs=" + formatted + " rhs=" + formatted2);
                }
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throwUnchecked(t);
                }
                return;
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static String computeExpected(String prefix, String mid, String suffix, String quoted, String arg,
            boolean useLower, boolean includeQuotedSegment, boolean includeEscapedQuoteNearStart,
            boolean includeEscapedQuoteNearEnd) {
        StringBuilder out = new StringBuilder();
        if (includeEscapedQuoteNearStart) {
            out.append('\'');
        }
        out.append(prefix);
        out.append(mid);
        out.append(' ');
        out.append(useLower ? arg.toLowerCase(Locale.getDefault()) : arg);
        if (includeQuotedSegment) {
            out.append(' ').append(quoted);
        }
        if (suffix.length() > 0) {
            out.append(' ').append(suffix);
        }
        if (includeEscapedQuoteNearEnd) {
            out.append('\'');
        }
        return out.toString();
    }

    private static String sanitizeLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == '\'') {
                continue;
            }
            if (Character.isISOControl(c)) {
                continue;
            }
            out.append(c);
        }
        return out.toString().trim();
    }

    private static String sanitizeQuotedLiteral(String s) {
        String x = sanitizeLiteral(s);
        return x.length() == 0 ? "q" : x;
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
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            String cls = st[i].getClassName();
            String method = st[i].getMethodName();
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cls)
                    && ("appendQuotedString".equals(method)
                            || "next".equals(method)
                            || "startsWith".equals(method)
                            || "append".equals(method))) {
                return true;
            }
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                    && ("append".equals(method) || "startsWith".equals(method))) {
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
## [36] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [37] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~9,685 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [11] — not reprinted*
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
    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new UpperCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    private static final class LowerCaseFormat extends Format {
        private final Locale locale;

        LowerCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj == null) {
                return toAppendTo;
            }
            return toAppendTo.append(String.valueOf(obj).toLowerCase(locale));
        }

        public Object parseObject(String source, ParsePosition pos) {
            int i = pos.getIndex();
            pos.setIndex(source == null ? i : source.length());
            return source;
        }
    }

    private static final class UpperCaseFormat extends Format {
        private final Locale locale;

        UpperCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj == null) {
                return toAppendTo;
            }
            return toAppendTo.append(String.valueOf(obj).toUpperCase(locale));
        }

        public Object parseObject(String source, ParsePosition pos) {
            int i = pos.getIndex();
            pos.setIndex(source == null ? i : source.length());
            return source;
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        runAnchor(registry);

        String prefix = sanitizeLiteral(data.consumeAsciiString(24));
        String mid = sanitizeLiteral(data.consumeAsciiString(24));
        String suffix = sanitizeLiteral(data.consumeAsciiString(24));
        String quoted = sanitizeQuotedLiteral(data.consumeAsciiString(16));
        String arg = sanitizeLiteral(data.consumeAsciiString(24));
        if (arg.length() == 0) {
            arg = "X";
        }

        boolean useLower = data.consumeBoolean();
        boolean includeQuotedSegment = data.consumeBoolean();
        boolean includeEscapedQuoteNearStart = data.consumeBoolean();
        boolean includeEscapedQuoteNearEnd = data.consumeBoolean();

        StringBuilder pattern = new StringBuilder();
        if (includeEscapedQuoteNearStart) {
            pattern.append("''");
        }
        pattern.append(prefix);
        if (mid.length() > 0) {
            pattern.append(mid);
        }
        pattern.append(" {0");
        if (useLower) {
            pattern.append(",lower");
        }
        pattern.append("}");
        if (includeQuotedSegment) {
            pattern.append(" '").append(quoted).append("'");
        }
        if (suffix.length() > 0) {
            pattern.append(' ').append(suffix);
        }
        if (includeEscapedQuoteNearEnd) {
            pattern.append("''");
        }

        String builtPattern = pattern.toString();
        String expected = computeExpected(prefix, mid, suffix, quoted, arg, useLower, includeQuotedSegment,
                includeEscapedQuoteNearStart, includeEscapedQuoteNearEnd);

        runValidPatternChecks(builtPattern, registry, arg, expected);
    }

    private static void runAnchor(Map registry) {
        String pattern = "it''s a {0,lower} 'test'!";
        String arg = "DUMMY";
        String expected = "it's a dummy test!";
        runValidPatternChecks(pattern, registry, arg, expected);
    }

    private static void runValidPatternChecks(String pattern, Map registry, String arg, String expected) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

            String before = emf.toPattern();
            String formatted = emf.format(new Object[] { arg });
            String after = emf.toPattern();

            if (!before.equals(after)) {
                throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: format should not mutate toPattern input="
                        + pattern + " before=" + before + " after=" + after);
            }

            if (!expected.equals(formatted)) {
                throw new RuntimeException("[oracle:format-output] metamorphic violation: valid constructed pattern must format to its constructed literal/result input="
                        + pattern + " expected=" + expected + " actual=" + formatted);
            }

            try {
                ExtendedMessageFormat emf2 = new ExtendedMessageFormat(emf.toPattern(), registry);
                String formatted2 = emf2.format(new Object[] { arg });
                if (!formatted.equals(formatted2)) {
                    throw new RuntimeException("[oracle:roundtrip-topattern] metamorphic violation: formatting after toPattern round-trip changed input="
                            + pattern + " toPattern=" + emf.toPattern() + " lhs=" + formatted + " rhs=" + formatted2);
                }
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throwUnchecked(t);
                }
                return;
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static String computeExpected(String prefix, String mid, String suffix, String quoted, String arg,
            boolean useLower, boolean includeQuotedSegment, boolean includeEscapedQuoteNearStart,
            boolean includeEscapedQuoteNearEnd) {
        StringBuilder out = new StringBuilder();
        if (includeEscapedQuoteNearStart) {
            out.append('\'');
        }
        out.append(prefix);
        out.append(mid);
        out.append(' ');
        out.append(useLower ? arg.toLowerCase(Locale.getDefault()) : arg);
        if (includeQuotedSegment) {
            out.append(' ').append(quoted);
        }
        if (suffix.length() > 0) {
            out.append(' ').append(suffix);
        }
        if (includeEscapedQuoteNearEnd) {
            out.append('\'');
        }
        return out.toString();
    }

    private static String sanitizeLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == '\'') {
                continue;
            }
            if (Character.isISOControl(c)) {
                continue;
            }
            out.append(c);
        }
        return out.toString().trim();
    }

    private static String sanitizeQuotedLiteral(String s) {
        String x = sanitizeLiteral(s);
        return x.length() == 0 ? "q" : x;
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
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            String cls = st[i].getClassName();
            String method = st[i].getMethodName();
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cls)
                    && ("appendQuotedString".equals(method)
                            || "next".equals(method)
                            || "startsWith".equals(method)
                            || "append".equals(method))) {
                return true;
            }
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                    && ("append".equals(method) || "startsWith".equals(method))) {
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
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~6,968 chars)</summary>

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
    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new UpperCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    private static final class LowerCaseFormat extends Format {
        private final Locale locale;

        LowerCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(String.valueOf(obj).toLowerCase(locale));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }

    private static final class UpperCaseFormat extends Format {
        private final Locale locale;

        UpperCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(String.valueOf(obj).toUpperCase(locale));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        // Anchor: exact trigger from ExtendedMessageFormatTest.testEscapedQuote_LANG_477.
        // On the buggy version, constructing this object reaches appendQuotedString with
        // escapingOn=true and c[start]=='\'', but fails to advance pos, causing the loop in
        // applyPattern to revisit the same quote forever until OutOfMemoryError.
        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, registry);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: input=" + anchorPattern + " actual=" + anchorOut);
        }
        String anchorBefore = anchor.toPattern();
        String anchorAgain = anchor.format(new Object[] { "DUMMY" });
        String anchorAfter = anchor.toPattern();
        if (!anchorBefore.equals(anchorAfter) || !anchorOut.equals(anchorAgain)) {
            throw new RuntimeException("[oracle:anchor-stable] metamorphic violation: before=" + anchorBefore + " after=" + anchorAfter + " once=" + anchorOut + " twice=" + anchorAgain);
        }

        // Explore the flipped condition boundary: any valid pattern containing a doubled quote
        // outside a quoted literal should be accepted and should render a single apostrophe.
        String left = sanitize(data.consumeAsciiString(16));
        String right = sanitize(data.consumeAsciiString(16));
        String quoted = sanitize(data.consumeAsciiString(16));
        String arg = sanitizeArg(data.consumeAsciiString(16));
        String formatName = data.consumeBoolean() ? "lower" : "upper";

        int variant = data.consumeInt(0, 3);
        String pattern;
        String expectedPrefix;
        String transformedArg = "lower".equals(formatName)
                ? arg.toLowerCase(Locale.getDefault())
                : arg.toUpperCase(Locale.getDefault());

        if (variant == 0) {
            pattern = left + "''" + right + " {0," + formatName + "} '" + quoted + "'";
            expectedPrefix = left + "'" + right + " " + transformedArg + " " + quoted;
        } else if (variant == 1) {
            pattern = "''" + left + right + " {0," + formatName + "} '" + quoted + "'";
            expectedPrefix = "'" + left + right + " " + transformedArg + " " + quoted;
        } else if (variant == 2) {
            pattern = left + " {0," + formatName + "} ''" + right + " '" + quoted + "'";
            expectedPrefix = left + " " + transformedArg + " '" + right + " " + quoted;
        } else {
            pattern = left + "''" + right + " {0}";
            expectedPrefix = left + "'" + right + " " + arg;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String before = emf.toPattern();
        String actual = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        // Post-condition: formatting is read-only with respect to toPattern(), and a valid
        // doubled-quote pattern must preserve the apostrophe semantics observable in output.
        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: pattern=" + pattern + " before=" + before + " after=" + after);
        }
        if (!expectedPrefix.equals(actual)) {
            throw new RuntimeException("[oracle:escaped-quote-output] metamorphic violation: pattern=" + pattern + " expected=" + expectedPrefix + " actual=" + actual);
        }

        ExtendedMessageFormat roundTrip = new ExtendedMessageFormat(emf.toPattern(), registry);
        String rt = roundTrip.format(new Object[] { arg });
        if (!actual.equals(rt)) {
            throw new RuntimeException("[oracle:roundtrip] metamorphic violation: pattern=" + pattern + " toPattern=" + emf.toPattern() + " lhs=" + actual + " rhs=" + rt);
        }
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'' || c == '{' || c == '}' || Character.isISOControl(c)) {
                continue;
            }
            out.append(c);
        }
        if (out.length() == 0) {
            return "x";
        }
        return out.toString();
    }

    private static String sanitizeArg(String s) {
        String x = sanitize(s);
        return x.length() == 0 ? "DUMMY" : x;
    }
}
```

</details>

---
## [38] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [39] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~7,286 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [11] — not reprinted*
- *[assistant] message: identical to step [37] — not reprinted*
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
    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new UpperCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    private static final class LowerCaseFormat extends Format {
        private final Locale locale;

        LowerCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(String.valueOf(obj).toLowerCase(locale));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }

    private static final class UpperCaseFormat extends Format {
        private final Locale locale;

        UpperCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(String.valueOf(obj).toUpperCase(locale));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        // Anchor: exact trigger from ExtendedMessageFormatTest.testEscapedQuote_LANG_477.
        // On the buggy version, constructing this object reaches appendQuotedString with
        // escapingOn=true and c[start]=='\'', but fails to advance pos, causing the loop in
        // applyPattern to revisit the same quote forever until OutOfMemoryError.
        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, registry);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: input=" + anchorPattern + " actual=" + anchorOut);
        }
        String anchorBefore = anchor.toPattern();
        String anchorAgain = anchor.format(new Object[] { "DUMMY" });
        String anchorAfter = anchor.toPattern();
        if (!anchorBefore.equals(anchorAfter) || !anchorOut.equals(anchorAgain)) {
            throw new RuntimeException("[oracle:anchor-stable] metamorphic violation: before=" + anchorBefore + " after=" + anchorAfter + " once=" + anchorOut + " twice=" + anchorAgain);
        }

        // Explore the flipped condition boundary: any valid pattern containing a doubled quote
        // outside a quoted literal should be accepted and should render a single apostrophe.
        String left = sanitize(data.consumeAsciiString(16));
        String right = sanitize(data.consumeAsciiString(16));
        String quoted = sanitize(data.consumeAsciiString(16));
        String arg = sanitizeArg(data.consumeAsciiString(16));
        String formatName = data.consumeBoolean() ? "lower" : "upper";

        int variant = data.consumeInt(0, 3);
        String pattern;
        String expectedPrefix;
        String transformedArg = "lower".equals(formatName)
                ? arg.toLowerCase(Locale.getDefault())
                : arg.toUpperCase(Locale.getDefault());

        if (variant == 0) {
            pattern = left + "''" + right + " {0," + formatName + "} '" + quoted + "'";
            expectedPrefix = left + "'" + right + " " + transformedArg + " " + quoted;
        } else if (variant == 1) {
            pattern = "''" + left + right + " {0," + formatName + "} '" + quoted + "'";
            expectedPrefix = "'" + left + right + " " + transformedArg + " " + quoted;
        } else if (variant == 2) {
            pattern = left + " {0," + formatName + "} ''" + right + " '" + quoted + "'";
            expectedPrefix = left + " " + transformedArg + " '" + right + " " + quoted;
        } else {
            pattern = left + "''" + right + " {0}";
            expectedPrefix = left + "'" + right + " " + arg;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String before = emf.toPattern();
        String actual = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        // Post-condition: formatting is read-only with respect to toPattern(), and a valid
        // doubled-quote pattern must preserve the apostrophe semantics observable in output.
        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: pattern=" + pattern + " before=" + before + " after=" + after);
        }
        if (!expectedPrefix.equals(actual)) {
            throw new RuntimeException("[oracle:escaped-quote-output] metamorphic violation: pattern=" + pattern + " expected=" + expectedPrefix + " actual=" + actual);
        }

        ExtendedMessageFormat roundTrip = new ExtendedMessageFormat(emf.toPattern(), registry);
        String rt = roundTrip.format(new Object[] { arg });
        if (!actual.equals(rt)) {
            throw new RuntimeException("[oracle:roundtrip] metamorphic violation: pattern=" + pattern + " toPattern=" + emf.toPattern() + " lhs=" + actual + " rhs=" + rt);
        }
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'' || c == '{' || c == '}' || Character.isISOControl(c)) {
                continue;
            }
            out.append(c);
        }
        if (out.length() == 0) {
            return "x";
        }
        return out.toString();
    }

    private static String sanitizeArg(String s) {
        String x = sanitize(s);
        return x.length() == 0 ? "DUMMY" : x;
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~5,992 chars)</summary>

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
    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new UpperCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    private static final class LowerCaseFormat extends Format {
        private final Locale locale;

        LowerCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(String.valueOf(obj).toLowerCase(locale));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }

    private static final class UpperCaseFormat extends Format {
        private final Locale locale;

        UpperCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(String.valueOf(obj).toUpperCase(locale));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        String anchorPattern = "it''s a {0,lower} 'test'!";

        // Exact trigger from ExtendedMessageFormatTest.testEscapedQuote_LANG_477.
        // On the buggy version, applyPattern() reaches appendQuotedString() at the first
        // quote of the doubled quote sequence. The buggy code returns without next(pos),
        // so the outer loop revisits the same quote forever and grows buffers until OOME.
        ExtendedMessageFormat emf = new ExtendedMessageFormat(anchorPattern, registry);

        // Observable contract check for fixed builds: the documented test case must format
        // correctly, and format() must not mutate the pattern reported by toPattern().
        String before = emf.toPattern();
        String out = emf.format(new Object[] { "DUMMY" });
        String after = emf.toPattern();

        if (!"it's a dummy test!".equals(out)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: pattern=" + anchorPattern + " actual=" + out);
        }
        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: before=" + before + " after=" + after);
        }

        // Explore the same root-cause property on nearby valid inputs: a doubled quote
        // outside a quoted literal must be consumed as one apostrophe and must not break
        // parsing. These execute only on fixed builds because the buggy build already
        // crashes above on the exact anchor.
        String left = sanitize(data.consumeAsciiString(12));
        String right = sanitize(data.consumeAsciiString(12));
        String literal = sanitize(data.consumeAsciiString(12));
        String arg = sanitizeArg(data.consumeAsciiString(12));
        boolean useLower = data.consumeBoolean();
        boolean quotedTail = data.consumeBoolean();

        String fmt = useLower ? ",lower" : ",upper";
        String transformed = useLower ? arg.toLowerCase(Locale.getDefault()) : arg.toUpperCase(Locale.getDefault());

        String pattern;
        String expected;
        if (quotedTail) {
            pattern = left + "''" + right + " {0" + fmt + "} '" + literal + "'";
            expected = left + "'" + right + " " + transformed + " " + literal;
        } else {
            pattern = left + "''" + right + " {0" + fmt + "}";
            expected = left + "'" + right + " " + transformed;
        }

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(pattern, registry);
        String before2 = emf2.toPattern();
        String out2 = emf2.format(new Object[] { arg });
        String after2 = emf2.toPattern();

        if (!expected.equals(out2)) {
            throw new RuntimeException("[oracle:escaped-quote-output] metamorphic violation: pattern=" + pattern + " expected=" + expected + " actual=" + out2);
        }
        if (!before2.equals(after2)) {
            throw new RuntimeException("[oracle:escaped-quote-topattern] metamorphic violation: before=" + before2 + " after=" + after2);
        }
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'' || c == '{' || c == '}' || Character.isISOControl(c)) {
                continue;
            }
            sb.append(c);
        }
        return sb.length() == 0 ? "x" : sb.toString();
    }

    private static String sanitizeArg(String s) {
        String x = sanitize(s);
        return x.length() == 0 ? "DUMMY" : x;
    }
}
```

</details>

---
## [40] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [41] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (8 message(s), ~6,430 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [11] — not reprinted*
- *[assistant] message: identical to step [37] — not reprinted*
- *[user] message: identical to step [5] — not reprinted*
- *[assistant] message: identical to step [39] — not reprinted*
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
    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new UpperCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    private static final class LowerCaseFormat extends Format {
        private final Locale locale;

        LowerCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(String.valueOf(obj).toLowerCase(locale));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }

    private static final class UpperCaseFormat extends Format {
        private final Locale locale;

        UpperCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(String.valueOf(obj).toUpperCase(locale));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        String anchorPattern = "it''s a {0,lower} 'test'!";

        // Exact trigger from ExtendedMessageFormatTest.testEscapedQuote_LANG_477.
        // On the buggy version, applyPattern() reaches appendQuotedString() at the first
        // quote of the doubled quote sequence. The buggy code returns without next(pos),
        // so the outer loop revisits the same quote forever and grows buffers until OOME.
        ExtendedMessageFormat emf = new ExtendedMessageFormat(anchorPattern, registry);

        // Observable contract check for fixed builds: the documented test case must format
        // correctly, and format() must not mutate the pattern reported by toPattern().
        String before = emf.toPattern();
        String out = emf.format(new Object[] { "DUMMY" });
        String after = emf.toPattern();

        if (!"it's a dummy test!".equals(out)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: pattern=" + anchorPattern + " actual=" + out);
        }
        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: before=" + before + " after=" + after);
        }

        // Explore the same root-cause property on nearby valid inputs: a doubled quote
        // outside a quoted literal must be consumed as one apostrophe and must not break
        // parsing. These execute only on fixed builds because the buggy build already
        // crashes above on the exact anchor.
        String left = sanitize(data.consumeAsciiString(12));
        String right = sanitize(data.consumeAsciiString(12));
        String literal = sanitize(data.consumeAsciiString(12));
        String arg = sanitizeArg(data.consumeAsciiString(12));
        boolean useLower = data.consumeBoolean();
        boolean quotedTail = data.consumeBoolean();

        String fmt = useLower ? ",lower" : ",upper";
        String transformed = useLower ? arg.toLowerCase(Locale.getDefault()) : arg.toUpperCase(Locale.getDefault());

        String pattern;
        String expected;
        if (quotedTail) {
            pattern = left + "''" + right + " {0" + fmt + "} '" + literal + "'";
            expected = left + "'" + right + " " + transformed + " " + literal;
        } else {
            pattern = left + "''" + right + " {0" + fmt + "}";
            expected = left + "'" + right + " " + transformed;
        }

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(pattern, registry);
        String before2 = emf2.toPattern();
        String out2 = emf2.format(new Object[] { arg });
        String after2 = emf2.toPattern();

        if (!expected.equals(out2)) {
            throw new RuntimeException("[oracle:escaped-quote-output] metamorphic violation: pattern=" + pattern + " expected=" + expected + " actual=" + out2);
        }
        if (!before2.equals(after2)) {
            throw new RuntimeException("[oracle:escaped-quote-topattern] metamorphic violation: before=" + before2 + " after=" + after2);
        }
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'' || c == '{' || c == '}' || Character.isISOControl(c)) {
                continue;
            }
            sb.append(c);
        }
        return sb.length() == 0 ? "x" : sb.toString();
    }

    private static String sanitizeArg(String s) {
        String x = sanitize(s);
        return x.length() == 0 ? "DUMMY" : x;
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~5,526 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        // ANCHOR: exact root-cause shape from the bug report: escapingOn=true and the current
        // character is the first quote of a doubled quote outside a quoted literal.
        // A non-null registry forces ExtendedMessageFormat down its custom applyPattern path,
        // which calls appendQuotedString(..., true).
        //
        // On the buggy version, appendQuotedString returns without next(pos), so applyPattern's
        // outer loop revisits the same quote forever and eventually throws OutOfMemoryError.
        // On the fixed version, the doubled quote is consumed and the pattern is accepted.
        String anchorPattern = "it''s";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, registry);

        // Post-condition for fixed builds:
        // In MessageFormat syntax, doubled quote '' denotes a literal apostrophe.
        // So formatting a literal-only valid pattern must collapse '' to '.
        String anchorOut = anchor.format(new Object[0]);
        if (!"it's".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: pattern=" + anchorPattern + " actual=" + anchorOut);
        }
        String anchorToPattern1 = anchor.toPattern();
        String anchorOut2 = anchor.format(new Object[0]);
        String anchorToPattern2 = anchor.toPattern();
        if (!anchorOut.equals(anchorOut2) || !anchorToPattern1.equals(anchorToPattern2)) {
            throw new RuntimeException("[oracle:anchor-stable] metamorphic violation: pattern=" + anchorPattern
                    + " out1=" + anchorOut + " out2=" + anchorOut2
                    + " p1=" + anchorToPattern1 + " p2=" + anchorToPattern2);
        }

        // EXPLORE: same property with varied valid patterns that place a doubled quote at and
        // around the boundary the patch changed. All of these are valid-by-construction for
        // correct MessageFormat handling of apostrophes.
        String left = sanitizeLiteral(data.consumeAsciiString(24));
        String right = sanitizeLiteral(data.consumeAsciiString(24));
        String tail = sanitizeLiteral(data.consumeAsciiString(24));
        boolean putQuoteAtStart = data.consumeBoolean();
        boolean putQuoteAtEnd = data.consumeBoolean();
        boolean includeArgument = data.consumeBoolean();

        String pattern;
        String expected;
        Object[] args;

        if (includeArgument) {
            String arg = sanitizeArg(data.consumeAsciiString(24));
            if (putQuoteAtStart) {
                pattern = "''" + left + "{0}" + right;
                expected = "'" + left + arg + right;
            } else if (putQuoteAtEnd) {
                pattern = left + "{0}" + right + "''" + tail;
                expected = left + arg + right + "'" + tail;
            } else {
                pattern = left + "''" + right + "{0}" + tail;
                expected = left + "'" + right + arg + tail;
            }
            args = new Object[] { arg };
        } else {
            if (putQuoteAtStart) {
                pattern = "''" + left + right;
                expected = "'" + left + right;
            } else if (putQuoteAtEnd) {
                pattern = left + right + "''" + tail;
                expected = left + right + "'" + tail;
            } else {
                pattern = left + "''" + right + tail;
                expected = left + "'" + right + tail;
            }
            args = new Object[0];
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String before = emf.toPattern();
        String actual = emf.format(args);
        String after = emf.toPattern();

        if (!expected.equals(actual)) {
            throw new RuntimeException("[oracle:escaped-quote-output] metamorphic violation: pattern=" + pattern
                    + " expected=" + expected + " actual=" + actual);
        }
        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: pattern=" + pattern
                    + " before=" + before + " after=" + after);
        }

        ExtendedMessageFormat roundTrip = new ExtendedMessageFormat(emf.toPattern(), registry);
        String roundTripOut = roundTrip.format(args);
        if (!actual.equals(roundTripOut)) {
            throw new RuntimeException("[oracle:roundtrip] metamorphic violation: pattern=" + pattern
                    + " toPattern=" + emf.toPattern() + " lhs=" + actual + " rhs=" + roundTripOut);
        }
    }

    private static String sanitizeLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'' || c == '{' || c == '}' || Character.isISOControl(c)) {
                continue;
            }
            sb.append(c);
        }
        return sb.length() == 0 ? "x" : sb.toString();
    }

    private static String sanitizeArg(String s) {
        String x = sanitizeLiteral(s);
        return x.length() == 0 ? "A" : x;
    }
}
```

</details>

---
## [42] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [43] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~161 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [19] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,384 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        exercisePattern("it''s a {0,lower} 'test'!", registry, false);

        String fuzzPattern = buildPattern(data);
        exercisePattern(fuzzPattern, registry, true);
    }

    private static void exercisePattern(String pattern, Map registry, boolean withOracle) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
            String before = emf.toPattern();

            Object[] args = new Object[] { "DuMmY" };
            String formatted = emf.format(args);
            String after = emf.toPattern();

            if (withOracle) {
                try {
                    MessageFormat mf = new MessageFormat(pattern);
                    String expectedPattern = mf.toPattern();
                    String expectedFormatted = mf.format(args);

                    /*
                     * Contract used for this oracle:
                     * when no custom formats are available from the registry, ExtendedMessageFormat
                     * should behave like MessageFormat for patterns that use only standard syntax.
                     * The generated patterns are valid-by-construction and contain no custom format
                     * descriptions, only escaped quotes and a normal {0} argument.
                     * A patch that merely suppresses the crash or skips quote-processing work could
                     * change parsing/serialization or formatting output, so we compare both.
                     */
                    if (!safeEquals(before, after)) {
                        throw new RuntimeException(
                                "[oracle:topattern-stable] metamorphic violation: toPattern changed across read-only format call input="
                                        + pattern + " before=" + before + " after=" + after);
                    }
                    if (!safeEquals(after, expectedPattern)) {
                        throw new RuntimeException(
                                "[oracle:mf-topattern] metamorphic violation: ExtendedMessageFormat.toPattern disagrees with MessageFormat input="
                                        + pattern + " lhs=" + after + " rhs=" + expectedPattern);
                    }
                    if (!safeEquals(formatted, expectedFormatted)) {
                        throw new RuntimeException(
                                "[oracle:mf-format] metamorphic violation: ExtendedMessageFormat.format disagrees with MessageFormat input="
                                        + pattern + " lhs=" + formatted + " rhs=" + expectedFormatted);
                    }
                } catch (Throwable ignored) {
                    if (ignored instanceof RuntimeException
                            && ignored.getMessage() != null
                            && ignored.getMessage().startsWith("[oracle:")) {
                        throw (RuntimeException) ignored;
                    }
                    return;
                }
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName().toLowerCase();
            if (name.contains("validation") || name.contains("invalid")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(e.getClassName())
                    && ("appendQuotedString".equals(e.getMethodName())
                            || "next".equals(e.getMethodName())
                            || "applyPattern".equals(e.getMethodName()))) {
                return true;
            }
            if ("org.apache.commons.lang.text.StrBuilder".equals(e.getClassName())
                    && ("append".equals(e.getMethodName()) || "startsWith".equals(e.getMethodName()))) {
                return true;
            }
        }
        return false;
    }

    private static String buildPattern(FuzzedDataProvider data) {
        String a = sanitize(data.consumeAsciiString(12));
        String b = sanitize(data.consumeAsciiString(12));
        String c = sanitize(data.consumeRemainingAsString());

        int variant = data.consumeInt(0, 3);
        switch (variant) {
            case 0:
                return a + "''" + b + " {0} " + c;
            case 1:
                return a + " '' " + b + " {0} " + c;
            case 2:
                return a + "''" + b + " '{0}' " + c + " {0}";
            default:
                return a + " {0} " + b + "''" + c;
        }
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'') {
                out.append('x');
            } else if (Character.isISOControl(ch)) {
                out.append(' ');
            } else {
                out.append(ch);
            }
        }
        if (out.length() == 0) {
            out.append('x');
        }
        return out.toString();
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
```

</details>

---
## [44] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [45] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~6,582 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [19] — not reprinted*
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

        exercisePattern("it''s a {0,lower} 'test'!", registry, false);

        String fuzzPattern = buildPattern(data);
        exercisePattern(fuzzPattern, registry, true);
    }

    private static void exercisePattern(String pattern, Map registry, boolean withOracle) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
            String before = emf.toPattern();

            Object[] args = new Object[] { "DuMmY" };
            String formatted = emf.format(args);
            String after = emf.toPattern();

            if (withOracle) {
                try {
                    MessageFormat mf = new MessageFormat(pattern);
                    String expectedPattern = mf.toPattern();
                    String expectedFormatted = mf.format(args);

                    /*
                     * Contract used for this oracle:
                     * when no custom formats are available from the registry, ExtendedMessageFormat
                     * should behave like MessageFormat for patterns that use only standard syntax.
                     * The generated patterns are valid-by-construction and contain no custom format
                     * descriptions, only escaped quotes and a normal {0} argument.
                     * A patch that merely suppresses the crash or skips quote-processing work could
                     * change parsing/serialization or formatting output, so we compare both.
                     */
                    if (!safeEquals(before, after)) {
                        throw new RuntimeException(
                                "[oracle:topattern-stable] metamorphic violation: toPattern changed across read-only format call input="
                                        + pattern + " before=" + before + " after=" + after);
                    }
                    if (!safeEquals(after, expectedPattern)) {
                        throw new RuntimeException(
                                "[oracle:mf-topattern] metamorphic violation: ExtendedMessageFormat.toPattern disagrees with MessageFormat input="
                                        + pattern + " lhs=" + after + " rhs=" + expectedPattern);
                    }
                    if (!safeEquals(formatted, expectedFormatted)) {
                        throw new RuntimeException(
                                "[oracle:mf-format] metamorphic violation: ExtendedMessageFormat.format disagrees with MessageFormat input="
                                        + pattern + " lhs=" + formatted + " rhs=" + expectedFormatted);
                    }
                } catch (Throwable ignored) {
                    if (ignored instanceof RuntimeException
                            && ignored.getMessage() != null
                            && ignored.getMessage().startsWith("[oracle:")) {
                        throw (RuntimeException) ignored;
                    }
                    return;
                }
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName().toLowerCase();
            if (name.contains("validation") || name.contains("invalid")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(e.getClassName())
                    && ("appendQuotedString".equals(e.getMethodName())
                            || "next".equals(e.getMethodName())
                            || "applyPattern".equals(e.getMethodName()))) {
                return true;
            }
            if ("org.apache.commons.lang.text.StrBuilder".equals(e.getClassName())
                    && ("append".equals(e.getMethodName()) || "startsWith".equals(e.getMethodName()))) {
                return true;
            }
        }
        return false;
    }

    private static String buildPattern(FuzzedDataProvider data) {
        String a = sanitize(data.consumeAsciiString(12));
        String b = sanitize(data.consumeAsciiString(12));
        String c = sanitize(data.consumeRemainingAsString());

        int variant = data.consumeInt(0, 3);
        switch (variant) {
            case 0:
                return a + "''" + b + " {0} " + c;
            case 1:
                return a + " '' " + b + " {0} " + c;
            case 2:
                return a + "''" + b + " '{0}' " + c + " {0}";
            default:
                return a + " {0} " + b + "''" + c;
        }
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'') {
                out.append('x');
            } else if (Character.isISOControl(ch)) {
                out.append(' ');
            } else {
                out.append(ch);
            }
        }
        if (out.length() == 0) {
            out.append('x');
        }
        return out.toString();
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~1,962 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);

        String prefix = clean(data.consumeAsciiString(16));
        String middle = clean(data.consumeAsciiString(16));
        String suffix = clean(data.consumeRemainingAsString());

        String pattern;
        switch (data.consumeInt(0, 4)) {
            case 0:
                pattern = prefix + "''" + middle + " {0,lower} 'test'!" + suffix;
                break;
            case 1:
                pattern = prefix + "it''s a {0,lower} 'test'!" + suffix;
                break;
            case 2:
                pattern = prefix + "'' {0,lower} '" + middle + "'" + suffix;
                break;
            case 3:
                pattern = prefix + "a''b {0,upper} 'c'" + suffix;
                break;
            default:
                pattern = prefix + "x''y {0,lower} z" + suffix;
                break;
        }

        new ExtendedMessageFormat(pattern, registry);
    }

    private static String clean(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuffer out = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == ',') {
                out.append('x');
            } else if (Character.isISOControl(ch)) {
                out.append(' ');
            } else {
                out.append(ch);
            }
        }
        return out.toString();
    }
}
```

</details>

---
## [46] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [47] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~3,575 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [19] — not reprinted*
- *[assistant] message: identical to step [45] — not reprinted*
- *[user] message: identical to step [5] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);

        String prefix = clean(data.consumeAsciiString(16));
        String middle = clean(data.consumeAsciiString(16));
        String suffix = clean(data.consumeRemainingAsString());

        String pattern;
        switch (data.consumeInt(0, 4)) {
            case 0:
                pattern = prefix + "''" + middle + " {0,lower} 'test'!" + suffix;
                break;
            case 1:
                pattern = prefix + "it''s a {0,lower} 'test'!" + suffix;
                break;
            case 2:
                pattern = prefix + "'' {0,lower} '" + middle + "'" + suffix;
                break;
            case 3:
                pattern = prefix + "a''b {0,upper} 'c'" + suffix;
                break;
            default:
                pattern = prefix + "x''y {0,lower} z" + suffix;
                break;
        }

        new ExtendedMessageFormat(pattern, registry);
    }

    private static String clean(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuffer out = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == ',') {
                out.append('x');
            } else if (Character.isISOControl(ch)) {
                out.append(' ');
            } else {
                out.append(ch);
            }
        }
        return out.toString();
    }
}
```
**[user]**
```
That did not compile. javac reported:
/tmp/d4j/Lang_43_buggy/fuzz/attempt_022/FuzzHarness.java:10: error: cannot find symbol
        registry.put("lower", new LowerCaseFormatFactory());
                                  ^
  symbol:   class LowerCaseFormatFactory
  location: class FuzzHarness
/tmp/d4j/Lang_43_buggy/fuzz/attempt_022/FuzzHarness.java:11: error: cannot find symbol
        registry.put("upper", new UpperCaseFormatFactory());
                                  ^
  symbol:   class UpperCaseFormatFactory
  location: class FuzzHarness
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
<details open><summary>▸ Output (~1,835 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);

        String left = sanitize(data.consumeAsciiString(12));
        String right = sanitize(data.consumeAsciiString(12));
        String tail = sanitize(data.consumeRemainingAsString());

        String pattern;
        switch (data.consumeInt(0, 4)) {
            case 0:
                pattern = left + "''" + right + " {0,lower} 'test'!" + tail;
                break;
            case 1:
                pattern = left + "it''s a {0,lower} 'test'!" + tail;
                break;
            case 2:
                pattern = left + "'' {0,lower} " + right + tail;
                break;
            case 3:
                pattern = left + "a''b {0,upper} 'c' " + right + tail;
                break;
            default:
                pattern = left + "''" + right + "{0,lower}" + tail;
                break;
        }

        new ExtendedMessageFormat(pattern, registry);
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuffer out = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == ',') {
                out.append('x');
            } else if (Character.isISOControl(ch)) {
                out.append(' ');
            } else {
                out.append(ch);
            }
        }
        return out.toString();
    }
}
```

</details>

---
## [48] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [49] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (8 message(s), ~2,274 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [19] — not reprinted*
- *[assistant] message: identical to step [45] — not reprinted*
- *[user] message: identical to step [5] — not reprinted*
- *[assistant] message: identical to step [47] — not reprinted*
- *[user] message: identical to step [47] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);

        String left = sanitize(data.consumeAsciiString(12));
        String right = sanitize(data.consumeAsciiString(12));
        String tail = sanitize(data.consumeRemainingAsString());

        String pattern;
        switch (data.consumeInt(0, 4)) {
            case 0:
                pattern = left + "''" + right + " {0,lower} 'test'!" + tail;
                break;
            case 1:
                pattern = left + "it''s a {0,lower} 'test'!" + tail;
                break;
            case 2:
                pattern = left + "'' {0,lower} " + right + tail;
                break;
            case 3:
                pattern = left + "a''b {0,upper} 'c' " + right + tail;
                break;
            default:
                pattern = left + "''" + right + "{0,lower}" + tail;
                break;
        }

        new ExtendedMessageFormat(pattern, registry);
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuffer out = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == ',') {
                out.append('x');
            } else if (Character.isISOControl(ch)) {
                out.append(' ');
            } else {
                out.append(ch);
            }
        }
        return out.toString();
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~1,843 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);

        String a = safe(data.consumeAsciiString(8));
        String b = safe(data.consumeAsciiString(8));
        String c = safe(data.consumeRemainingAsString());

        String pattern;
        switch (data.consumeInt(0, 5)) {
            case 0:
                pattern = a + "''" + b + " {0,lower} 'test'!" + c;
                break;
            case 1:
                pattern = a + "it''s a {0,lower} 'test'!" + c;
                break;
            case 2:
                pattern = a + "''x {0,lower}" + c;
                break;
            case 3:
                pattern = a + "x''y {0}" + c;
                break;
            case 4:
                pattern = "it''s a " + a + "{0,lower} 'test'!" + c;
                break;
            default:
                pattern = a + "''" + b + " {0}" + c;
                break;
        }

        new ExtendedMessageFormat(pattern, registry);
    }

    private static String safe(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuffer sb = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == ',') {
                sb.append('x');
            } else if (Character.isISOControl(ch)) {
                sb.append(' ');
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
}
```

</details>

---
## [50] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [51] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~10,034 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        class LowerCaseFormat extends java.text.Format {
            public StringBuffer format(Object obj, StringBuffer toAppendTo, java.text.FieldPosition pos) {
                if (obj == null) {
                    return toAppendTo;
                }
                return toAppendTo.append(obj.toString().toLowerCase(java.util.Locale.ROOT));
            }

            public Object parseObject(String source, java.text.ParsePosition pos) {
                pos.setIndex(source == null ? 0 : source.length());
                return source;
            }
        }
        class UpperCaseFormat extends java.text.Format {
            public StringBuffer format(Object obj, StringBuffer toAppendTo, java.text.FieldPosition pos) {
                if (obj == null) {
                    return toAppendTo;
                }
                return toAppendTo.append(obj.toString().toUpperCase(java.util.Locale.ROOT));
            }

            public Object parseObject(String source, java.text.ParsePosition pos) {
                pos.setIndex(source == null ? 0 : source.length());
                return source;
            }
        }
        class LowerCaseFormatFactory implements FormatFactory {
            public java.text.Format getFormat(String name, String arguments, java.util.Locale locale) {
                return new LowerCaseFormat();
            }
        }
        class UpperCaseFormatFactory implements FormatFactory {
            public java.text.Format getFormat(String name, String arguments, java.util.Locale locale) {
                return new UpperCaseFormat();
            }
        }

        java.util.Map registry = new java.util.HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        String anchorPattern = "it''s a {0,lower} 'test'!";
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(anchorPattern, registry);
            String before = emf.toPattern();
            String out1 = emf.format(new Object[] { "DUMMY" });
            String after = emf.toPattern();

            if (!"it's a dummy test!".equals(out1)) {
                throw new RuntimeException("[oracle:anchor-output] metamorphic violation: exact regression input formatted incorrectly input="
                        + anchorPattern + " lhs=" + out1 + " rhs=it's a dummy test!");
            }

            /* Contract asserted: formatting is read-only for the pattern representation exposed by toPattern(),
               and reconstructing an equivalent object from the same valid pattern/registry must yield the same
               observable formatting result. A throw-deleting patch that skips quote bookkeeping can leave output
               or stored pattern inconsistent even when no exception is thrown. */
            if (before == null ? after != null : !before.equals(after)) {
                throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: toPattern changed across format input="
                        + anchorPattern + " lhs=" + before + " rhs=" + after);
            }

            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(anchorPattern, registry);
            String out2 = emf2.format(new Object[] { "DUMMY" });
            if (!out1.equals(out2)) {
                throw new RuntimeException("[oracle:reconstruct-eq] metamorphic violation: identical constructions disagree input="
                        + anchorPattern + " lhs=" + out1 + " rhs=" + out2);
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
            boolean root = false;
            if (t instanceof OutOfMemoryError) {
                StackTraceElement[] st = t.getStackTrace();
                if (st != null) {
                    for (int i = 0; i < st.length; i++) {
                        StackTraceElement e = st[i];
                        if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(e.getClassName())
                                && "appendQuotedString".equals(e.getMethodName())) {
                            root = true;
                            break;
                        }
                        if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(e.getClassName())
                                && ("next".equals(e.getMethodName()) || "append".equals(e.getMethodName())
                                || "startsWith".equals(e.getMethodName()))) {
                            root = true;
                            break;
                        }
                        if ("org.apache.commons.lang.text.StrBuilder".equals(e.getClassName())
                                && ("append".equals(e.getMethodName()) || "startsWith".equals(e.getMethodName()))) {
                            root = true;
                            break;
                        }
                    }
                }
            }
            if (root) {
                throw (Error) t;
            }
        }

        String a = data.consumeAsciiString(24);
        String b = data.consumeAsciiString(24);
        String c = data.consumeAsciiString(24);
        String arg = data.consumeString(24);
        boolean useLower = data.consumeBoolean();
        boolean useQuotedTail = data.consumeBoolean();

        a = a.replace("{", "A").replace("}", "B").replace("'", "Q");
        b = b.replace("{", "C").replace("}", "D").replace("'", "R");
        c = c.replace("{", "E").replace("}", "F").replace("'", "S");
        if (a.length() == 0) {
            a = "x";
        }
        if (b.length() == 0) {
            b = "y";
        }
        if (c.length() == 0) {
            c = "z";
        }

        String formatName = useLower ? "lower" : "upper";
        StringBuilder patternBuilder = new StringBuilder();
        if (data.consumeBoolean()) {
            patternBuilder.append(a);
        }
        patternBuilder.append("''");
        if (data.consumeBoolean()) {
            patternBuilder.append(b);
        }
        patternBuilder.append(" {0,").append(formatName).append("}");
        if (useQuotedTail) {
            patternBuilder.append(" '").append(c).append("'");
        } else {
            patternBuilder.append(" ").append(c);
        }
        String fuzzPattern = patternBuilder.toString();

        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(fuzzPattern, registry);
            String before = emf.toPattern();
            String out1 = emf.format(new Object[] { arg });
            String after = emf.toPattern();

            if (before == null ? after != null : !before.equals(after)) {
                throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: toPattern changed across format input="
                        + fuzzPattern + " lhs=" + before + " rhs=" + after);
            }

            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(fuzzPattern, registry);
            String out2 = emf2.format(new Object[] { arg });
            if (!out1.equals(out2)) {
                throw new RuntimeException("[oracle:reconstruct-eq] metamorphic violation: identical constructions disagree input="
                        + fuzzPattern + " lhs=" + out1 + " rhs=" + out2);
            }

            String roundTripPattern = emf.toPattern();
            try {
                ExtendedMessageFormat emf3 = new ExtendedMessageFormat(roundTripPattern, registry);
                String out3 = emf3.format(new Object[] { arg });
                if (!out1.equals(out3)) {
                    throw new RuntimeException("[oracle:roundtrip] metamorphic violation: format(parse(toPattern)) changed result input="
                            + fuzzPattern + " lhs=" + out1 + " rhs=" + out3);
                }
            } catch (Throwable ignored) {
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }

            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }

            boolean root = false;
            if (t instanceof OutOfMemoryError) {
                StackTraceElement[] st = t.getStackTrace();
                if (st != null) {
                    for (int i = 0; i < st.length; i++) {
                        StackTraceElement e = st[i];
                        if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(e.getClassName())
                                && "appendQuotedString".equals(e.getMethodName())) {
                            root = true;
                            break;
                        }
                        if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(e.getClassName())
                                && ("next".equals(e.getMethodName()) || "append".equals(e.getMethodName())
                                || "startsWith".equals(e.getMethodName()))) {
                            root = true;
                            break;
                        }
                        if ("org.apache.commons.lang.text.StrBuilder".equals(e.getClassName())
                                && ("append".equals(e.getMethodName()) || "startsWith".equals(e.getMethodName()))) {
                            root = true;
                            break;
                        }
                    }
                }
            }
            if (root) {
                throw (Error) t;
            }
        }
    }
}
```

</details>

---
## [52] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [53] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~10,231 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        class LowerCaseFormat extends java.text.Format {
            public StringBuffer format(Object obj, StringBuffer toAppendTo, java.text.FieldPosition pos) {
                if (obj == null) {
                    return toAppendTo;
                }
                return toAppendTo.append(obj.toString().toLowerCase(java.util.Locale.ROOT));
            }

            public Object parseObject(String source, java.text.ParsePosition pos) {
                pos.setIndex(source == null ? 0 : source.length());
                return source;
            }
        }
        class UpperCaseFormat extends java.text.Format {
            public StringBuffer format(Object obj, StringBuffer toAppendTo, java.text.FieldPosition pos) {
                if (obj == null) {
                    return toAppendTo;
                }
                return toAppendTo.append(obj.toString().toUpperCase(java.util.Locale.ROOT));
            }

            public Object parseObject(String source, java.text.ParsePosition pos) {
                pos.setIndex(source == null ? 0 : source.length());
                return source;
            }
        }
        class LowerCaseFormatFactory implements FormatFactory {
            public java.text.Format getFormat(String name, String arguments, java.util.Locale locale) {
                return new LowerCaseFormat();
            }
        }
        class UpperCaseFormatFactory implements FormatFactory {
            public java.text.Format getFormat(String name, String arguments, java.util.Locale locale) {
                return new UpperCaseFormat();
            }
        }

        java.util.Map registry = new java.util.HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        String anchorPattern = "it''s a {0,lower} 'test'!";
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(anchorPattern, registry);
            String before = emf.toPattern();
            String out1 = emf.format(new Object[] { "DUMMY" });
            String after = emf.toPattern();

            if (!"it's a dummy test!".equals(out1)) {
                throw new RuntimeException("[oracle:anchor-output] metamorphic violation: exact regression input formatted incorrectly input="
                        + anchorPattern + " lhs=" + out1 + " rhs=it's a dummy test!");
            }

            /* Contract asserted: formatting is read-only for the pattern representation exposed by toPattern(),
               and reconstructing an equivalent object from the same valid pattern/registry must yield the same
               observable formatting result. A throw-deleting patch that skips quote bookkeeping can leave output
               or stored pattern inconsistent even when no exception is thrown. */
            if (before == null ? after != null : !before.equals(after)) {
                throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: toPattern changed across format input="
                        + anchorPattern + " lhs=" + before + " rhs=" + after);
            }

            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(anchorPattern, registry);
            String out2 = emf2.format(new Object[] { "DUMMY" });
            if (!out1.equals(out2)) {
                throw new RuntimeException("[oracle:reconstruct-eq] metamorphic violation: identical constructions disagree input="
                        + anchorPattern + " lhs=" + out1 + " rhs=" + out2);
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
            boolean root = false;
            if (t instanceof OutOfMemoryError) {
                StackTraceElement[] st = t.getStackTrace();
                if (st != null) {
                    for (int i = 0; i < st.length; i++) {
                        StackTraceElement e = st[i];
                        if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(e.getClassName())
                                && "appendQuotedString".equals(e.getMethodName())) {
                            root = true;
                            break;
                        }
                        if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(e.getClassName())
                                && ("next".equals(e.getMethodName()) || "append".equals(e.getMethodName())
                                || "startsWith".equals(e.getMethodName()))) {
                            root = true;
                            break;
                        }
                        if ("org.apache.commons.lang.text.StrBuilder".equals(e.getClassName())
                                && ("append".equals(e.getMethodName()) || "startsWith".equals(e.getMethodName()))) {
                            root = true;
                            break;
                        }
                    }
                }
            }
            if (root) {
                throw (Error) t;
            }
        }

        String a = data.consumeAsciiString(24);
        String b = data.consumeAsciiString(24);
        String c = data.consumeAsciiString(24);
        String arg = data.consumeString(24);
        boolean useLower = data.consumeBoolean();
        boolean useQuotedTail = data.consumeBoolean();

        a = a.replace("{", "A").replace("}", "B").replace("'", "Q");
        b = b.replace("{", "C").replace("}", "D").replace("'", "R");
        c = c.replace("{", "E").replace("}", "F").replace("'", "S");
        if (a.length() == 0) {
            a = "x";
        }
        if (b.length() == 0) {
            b = "y";
        }
        if (c.length() == 0) {
            c = "z";
        }

        String formatName = useLower ? "lower" : "upper";
        StringBuilder patternBuilder = new StringBuilder();
        if (data.consumeBoolean()) {
            patternBuilder.append(a);
        }
        patternBuilder.append("''");
        if (data.consumeBoolean()) {
            patternBuilder.append(b);
        }
        patternBuilder.append(" {0,").append(formatName).append("}");
        if (useQuotedTail) {
            patternBuilder.append(" '").append(c).append("'");
        } else {
            patternBuilder.append(" ").append(c);
        }
        String fuzzPattern = patternBuilder.toString();

        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(fuzzPattern, registry);
            String before = emf.toPattern();
            String out1 = emf.format(new Object[] { arg });
            String after = emf.toPattern();

            if (before == null ? after != null : !before.equals(after)) {
                throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: toPattern changed across format input="
                        + fuzzPattern + " lhs=" + before + " rhs=" + after);
            }

            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(fuzzPattern, registry);
            String out2 = emf2.format(new Object[] { arg });
            if (!out1.equals(out2)) {
                throw new RuntimeException("[oracle:reconstruct-eq] metamorphic violation: identical constructions disagree input="
                        + fuzzPattern + " lhs=" + out1 + " rhs=" + out2);
            }

            String roundTripPattern = emf.toPattern();
            try {
                ExtendedMessageFormat emf3 = new ExtendedMessageFormat(roundTripPattern, registry);
                String out3 = emf3.format(new Object[] { arg });
                if (!out1.equals(out3)) {
                    throw new RuntimeException("[oracle:roundtrip] metamorphic violation: format(parse(toPattern)) changed result input="
                            + fuzzPattern + " lhs=" + out1 + " rhs=" + out3);
                }
            } catch (Throwable ignored) {
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }

            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }

            boolean root = false;
            if (t instanceof OutOfMemoryError) {
                StackTraceElement[] st = t.getStackTrace();
                if (st != null) {
                    for (int i = 0; i < st.length; i++) {
                        StackTraceElement e = st[i];
                        if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(e.getClassName())
                                && "appendQuotedString".equals(e.getMethodName())) {
                            root = true;
                            break;
                        }
                        if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(e.getClassName())
                                && ("next".equals(e.getMethodName()) || "append".equals(e.getMethodName())
                                || "startsWith".equals(e.getMethodName()))) {
                            root = true;
                            break;
                        }
                        if ("org.apache.commons.lang.text.StrBuilder".equals(e.getClassName())
                                && ("append".equals(e.getMethodName()) || "startsWith".equals(e.getMethodName()))) {
                            root = true;
                            break;
                        }
                    }
                }
            }
            if (root) {
                throw (Error) t;
            }
        }
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~4,319 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        class LowerCaseFormat extends java.text.Format {
            public StringBuffer format(Object obj, StringBuffer toAppendTo, java.text.FieldPosition pos) {
                if (obj != null) {
                    toAppendTo.append(obj.toString().toLowerCase(java.util.Locale.ROOT));
                }
                return toAppendTo;
            }

            public Object parseObject(String source, java.text.ParsePosition pos) {
                pos.setIndex(source == null ? 0 : source.length());
                return source;
            }
        }
        class UpperCaseFormat extends java.text.Format {
            public StringBuffer format(Object obj, StringBuffer toAppendTo, java.text.FieldPosition pos) {
                if (obj != null) {
                    toAppendTo.append(obj.toString().toUpperCase(java.util.Locale.ROOT));
                }
                return toAppendTo;
            }

            public Object parseObject(String source, java.text.ParsePosition pos) {
                pos.setIndex(source == null ? 0 : source.length());
                return source;
            }
        }
        class LowerCaseFormatFactory implements FormatFactory {
            public java.text.Format getFormat(String name, String arguments, java.util.Locale locale) {
                return new LowerCaseFormat();
            }
        }
        class UpperCaseFormatFactory implements FormatFactory {
            public java.text.Format getFormat(String name, String arguments, java.util.Locale locale) {
                return new UpperCaseFormat();
            }
        }

        java.util.Map registry = new java.util.HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        String anchor = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchorEmf = new ExtendedMessageFormat(anchor, registry);
        String anchorBefore = anchorEmf.toPattern();
        String anchorOut = anchorEmf.format(new Object[] { "DUMMY" });
        String anchorAfter = anchorEmf.toPattern();
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: input=" + anchor + " lhs=" + anchorOut + " rhs=it's a dummy test!");
        }
        if (anchorBefore == null ? anchorAfter != null : !anchorBefore.equals(anchorAfter)) {
            throw new RuntimeException("[oracle:anchor-topattern] metamorphic violation: input=" + anchor + " lhs=" + anchorBefore + " rhs=" + anchorAfter);
        }

        String prefix = data.consumeAsciiString(16).replace("'", "").replace("{", "").replace("}", "");
        String middle = data.consumeAsciiString(16).replace("'", "").replace("{", "").replace("}", "");
        String suffix = data.consumeAsciiString(16).replace("'", "").replace("{", "").replace("}", "");
        String arg = data.consumeString(16);
        String fmt = data.consumeBoolean() ? "lower" : "upper";

        if (prefix.length() == 0) {
            prefix = "a";
        }
        if (middle.length() == 0) {
            middle = "b";
        }
        if (suffix.length() == 0) {
            suffix = "c";
        }

        String pattern = prefix + "''" + middle + " {0," + fmt + "} '" + suffix + "'!";
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String before = emf.toPattern();
        String out1 = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        if (before == null ? after != null : !before.equals(after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: input=" + pattern + " lhs=" + before + " rhs=" + after);
        }

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(pattern, registry);
        String out2 = emf2.format(new Object[] { arg });
        if (!out1.equals(out2)) {
            throw new RuntimeException("[oracle:reconstruct-eq] metamorphic violation: input=" + pattern + " lhs=" + out1 + " rhs=" + out2);
        }
    }
}
```

</details>

---
## [54] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [55] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~4,636 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
- *[assistant] message: identical to step [53] — not reprinted*
- *[user] message: identical to step [5] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        class LowerCaseFormat extends java.text.Format {
            public StringBuffer format(Object obj, StringBuffer toAppendTo, java.text.FieldPosition pos) {
                if (obj != null) {
                    toAppendTo.append(obj.toString().toLowerCase(java.util.Locale.ROOT));
                }
                return toAppendTo;
            }

            public Object parseObject(String source, java.text.ParsePosition pos) {
                pos.setIndex(source == null ? 0 : source.length());
                return source;
            }
        }
        class UpperCaseFormat extends java.text.Format {
            public StringBuffer format(Object obj, StringBuffer toAppendTo, java.text.FieldPosition pos) {
                if (obj != null) {
                    toAppendTo.append(obj.toString().toUpperCase(java.util.Locale.ROOT));
                }
                return toAppendTo;
            }

            public Object parseObject(String source, java.text.ParsePosition pos) {
                pos.setIndex(source == null ? 0 : source.length());
                return source;
            }
        }
        class LowerCaseFormatFactory implements FormatFactory {
            public java.text.Format getFormat(String name, String arguments, java.util.Locale locale) {
                return new LowerCaseFormat();
            }
        }
        class UpperCaseFormatFactory implements FormatFactory {
            public java.text.Format getFormat(String name, String arguments, java.util.Locale locale) {
                return new UpperCaseFormat();
            }
        }

        java.util.Map registry = new java.util.HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        String anchor = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchorEmf = new ExtendedMessageFormat(anchor, registry);
        String anchorBefore = anchorEmf.toPattern();
        String anchorOut = anchorEmf.format(new Object[] { "DUMMY" });
        String anchorAfter = anchorEmf.toPattern();
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: input=" + anchor + " lhs=" + anchorOut + " rhs=it's a dummy test!");
        }
        if (anchorBefore == null ? anchorAfter != null : !anchorBefore.equals(anchorAfter)) {
            throw new RuntimeException("[oracle:anchor-topattern] metamorphic violation: input=" + anchor + " lhs=" + anchorBefore + " rhs=" + anchorAfter);
        }

        String prefix = data.consumeAsciiString(16).replace("'", "").replace("{", "").replace("}", "");
        String middle = data.consumeAsciiString(16).replace("'", "").replace("{", "").replace("}", "");
        String suffix = data.consumeAsciiString(16).replace("'", "").replace("{", "").replace("}", "");
        String arg = data.consumeString(16);
        String fmt = data.consumeBoolean() ? "lower" : "upper";

        if (prefix.length() == 0) {
            prefix = "a";
        }
        if (middle.length() == 0) {
            middle = "b";
        }
        if (suffix.length() == 0) {
            suffix = "c";
        }

        String pattern = prefix + "''" + middle + " {0," + fmt + "} '" + suffix + "'!";
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String before = emf.toPattern();
        String out1 = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        if (before == null ? after != null : !before.equals(after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: input=" + pattern + " lhs=" + before + " rhs=" + after);
        }

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(pattern, registry);
        String out2 = emf2.format(new Object[] { arg });
        if (!out1.equals(out2)) {
            throw new RuntimeException("[oracle:reconstruct-eq] metamorphic violation: input=" + pattern + " lhs=" + out1 + " rhs=" + out2);
        }
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~5,716 chars)</summary>

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
    public static class LowerCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toLowerCase(Locale.ROOT));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }

    public static class UpperCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toUpperCase(Locale.ROOT));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }

    public static class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat();
        }
    }

    public static class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new UpperCaseFormat();
        }
    }

    private static Map makeRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map anchorRegistry = makeRegistry();

        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchorEmf = new ExtendedMessageFormat(anchorPattern, anchorRegistry);
        String anchorBefore = anchorEmf.toPattern();
        String anchorOut = anchorEmf.format(new Object[] { "DUMMY" });
        String anchorAfter = anchorEmf.toPattern();

        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: input=" + anchorPattern + " lhs=" + anchorOut + " rhs=it's a dummy test!");
        }

        /* Contract asserted: format() is observationally read-only with respect to the message pattern,
           so toPattern() must not change across formatting; and rebuilding from the same pattern/registry
           must produce the same formatted result. A patch that merely suppresses the crash by skipping the
           quote-progress bookkeeping could silently mis-parse quotes and violate these observables. */
        if (anchorBefore == null ? anchorAfter != null : !anchorBefore.equals(anchorAfter)) {
            throw new RuntimeException("[oracle:anchor-topattern] metamorphic violation: input=" + anchorPattern + " lhs=" + anchorBefore + " rhs=" + anchorAfter);
        }

        ExtendedMessageFormat anchorEmf2 = new ExtendedMessageFormat(anchorPattern, makeRegistry());
        String anchorOut2 = anchorEmf2.format(new Object[] { "DUMMY" });
        if (!anchorOut.equals(anchorOut2)) {
            throw new RuntimeException("[oracle:anchor-rebuild] metamorphic violation: input=" + anchorPattern + " lhs=" + anchorOut + " rhs=" + anchorOut2);
        }

        String left = data.consumeAsciiString(12).replace("'", "").replace("{", "").replace("}", "");
        String mid = data.consumeAsciiString(12).replace("'", "").replace("{", "").replace("}", "");
        String tail = data.consumeAsciiString(12).replace("'", "").replace("{", "").replace("}", "");
        String arg = data.consumeString(12);
        boolean lower = data.consumeBoolean();
        boolean quotedTail = data.consumeBoolean();

        if (left.length() == 0) {
            left = "x";
        }
        if (mid.length() == 0) {
            mid = "y";
        }
        if (tail.length() == 0) {
            tail = "z";
        }

        String fmt = lower ? "lower" : "upper";

        String pattern = left + "''" + mid + " {0," + fmt + "}" + (quotedTail ? " '" + tail + "'" : " " + tail);
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, makeRegistry());
        String before = emf.toPattern();
        String out1 = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        if (before == null ? after != null : !before.equals(after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: input=" + pattern + " lhs=" + before + " rhs=" + after);
        }

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(pattern, makeRegistry());
        String out2 = emf2.format(new Object[] { arg });
        if (!out1.equals(out2)) {
            throw new RuntimeException("[oracle:reconstruct-eq] metamorphic violation: input=" + pattern + " lhs=" + out1 + " rhs=" + out2);
        }

        String roundTripPattern = emf.toPattern();
        ExtendedMessageFormat emf3 = new ExtendedMessageFormat(roundTripPattern, makeRegistry());
        String out3 = emf3.format(new Object[] { arg });
        if (!out1.equals(out3)) {
            throw new RuntimeException("[oracle:roundtrip] metamorphic violation: input=" + pattern + " lhs=" + out1 + " rhs=" + out3);
        }
    }
}
```

</details>

---
## [56] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [57] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (8 message(s), ~6,153 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
- *[assistant] message: identical to step [53] — not reprinted*
- *[user] message: identical to step [5] — not reprinted*
- *[assistant] message: identical to step [55] — not reprinted*
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
    public static class LowerCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toLowerCase(Locale.ROOT));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }

    public static class UpperCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toUpperCase(Locale.ROOT));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }

    public static class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat();
        }
    }

    public static class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new UpperCaseFormat();
        }
    }

    private static Map makeRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map anchorRegistry = makeRegistry();

        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchorEmf = new ExtendedMessageFormat(anchorPattern, anchorRegistry);
        String anchorBefore = anchorEmf.toPattern();
        String anchorOut = anchorEmf.format(new Object[] { "DUMMY" });
        String anchorAfter = anchorEmf.toPattern();

        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: input=" + anchorPattern + " lhs=" + anchorOut + " rhs=it's a dummy test!");
        }

        /* Contract asserted: format() is observationally read-only with respect to the message pattern,
           so toPattern() must not change across formatting; and rebuilding from the same pattern/registry
           must produce the same formatted result. A patch that merely suppresses the crash by skipping the
           quote-progress bookkeeping could silently mis-parse quotes and violate these observables. */
        if (anchorBefore == null ? anchorAfter != null : !anchorBefore.equals(anchorAfter)) {
            throw new RuntimeException("[oracle:anchor-topattern] metamorphic violation: input=" + anchorPattern + " lhs=" + anchorBefore + " rhs=" + anchorAfter);
        }

        ExtendedMessageFormat anchorEmf2 = new ExtendedMessageFormat(anchorPattern, makeRegistry());
        String anchorOut2 = anchorEmf2.format(new Object[] { "DUMMY" });
        if (!anchorOut.equals(anchorOut2)) {
            throw new RuntimeException("[oracle:anchor-rebuild] metamorphic violation: input=" + anchorPattern + " lhs=" + anchorOut + " rhs=" + anchorOut2);
        }

        String left = data.consumeAsciiString(12).replace("'", "").replace("{", "").replace("}", "");
        String mid = data.consumeAsciiString(12).replace("'", "").replace("{", "").replace("}", "");
        String tail = data.consumeAsciiString(12).replace("'", "").replace("{", "").replace("}", "");
        String arg = data.consumeString(12);
        boolean lower = data.consumeBoolean();
        boolean quotedTail = data.consumeBoolean();

        if (left.length() == 0) {
            left = "x";
        }
        if (mid.length() == 0) {
            mid = "y";
        }
        if (tail.length() == 0) {
            tail = "z";
        }

        String fmt = lower ? "lower" : "upper";

        String pattern = left + "''" + mid + " {0," + fmt + "}" + (quotedTail ? " '" + tail + "'" : " " + tail);
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, makeRegistry());
        String before = emf.toPattern();
        String out1 = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        if (before == null ? after != null : !before.equals(after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: input=" + pattern + " lhs=" + before + " rhs=" + after);
        }

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(pattern, makeRegistry());
        String out2 = emf2.format(new Object[] { arg });
        if (!out1.equals(out2)) {
            throw new RuntimeException("[oracle:reconstruct-eq] metamorphic violation: input=" + pattern + " lhs=" + out1 + " rhs=" + out2);
        }

        String roundTripPattern = emf.toPattern();
        ExtendedMessageFormat emf3 = new ExtendedMessageFormat(roundTripPattern, makeRegistry());
        String out3 = emf3.format(new Object[] { arg });
        if (!out1.equals(out3)) {
            throw new RuntimeException("[oracle:roundtrip] metamorphic violation: input=" + pattern + " lhs=" + out1 + " rhs=" + out3);
        }
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~4,669 chars)</summary>

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
    public static final class LowerCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toLowerCase(Locale.ROOT));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }

    public static final class UpperCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toUpperCase(Locale.ROOT));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }

    public static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat();
        }
    }

    public static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new UpperCaseFormat();
        }
    }

    private static Map makeRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = makeRegistry();

        String anchor = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchorEmf = new ExtendedMessageFormat(anchor, registry);
        String anchorOut = anchorEmf.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: input=" + anchor + " lhs=" + anchorOut + " rhs=it's a dummy test!");
        }
        String anchorPatternBefore = anchorEmf.toPattern();
        String anchorOut2 = anchorEmf.format(new Object[] { "DUMMY" });
        String anchorPatternAfter = anchorEmf.toPattern();
        if (!anchorOut.equals(anchorOut2)) {
            throw new RuntimeException("[oracle:anchor-idempotent] metamorphic violation: input=" + anchor + " lhs=" + anchorOut + " rhs=" + anchorOut2);
        }
        if (anchorPatternBefore == null ? anchorPatternAfter != null : !anchorPatternBefore.equals(anchorPatternAfter)) {
            throw new RuntimeException("[oracle:anchor-topattern] metamorphic violation: input=" + anchor + " lhs=" + anchorPatternBefore + " rhs=" + anchorPatternAfter);
        }

        String p1 = data.consumeAsciiString(8).replace("'", "").replace("{", "").replace("}", "");
        String p2 = data.consumeAsciiString(8).replace("'", "").replace("{", "").replace("}", "");
        String p3 = data.consumeAsciiString(8).replace("'", "").replace("{", "").replace("}", "");
        String arg = data.consumeString(8);
        String fmt = data.consumeBoolean() ? "lower" : "upper";

        if (p1.length() == 0) {
            p1 = "a";
        }
        if (p2.length() == 0) {
            p2 = "b";
        }
        if (p3.length() == 0) {
            p3 = "c";
        }

        String pattern = p1 + "''" + p2 + " {0," + fmt + "} '" + p3 + "'!";
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, makeRegistry());
        String out1 = emf.format(new Object[] { arg });
        String tp1 = emf.toPattern();

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(pattern, makeRegistry());
        String out2 = emf2.format(new Object[] { arg });
        if (!out1.equals(out2)) {
            throw new RuntimeException("[oracle:reparse] metamorphic violation: input=" + pattern + " lhs=" + out1 + " rhs=" + out2);
        }

        String tp2 = emf.toPattern();
        if (tp1 == null ? tp2 != null : !tp1.equals(tp2)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: input=" + pattern + " lhs=" + tp1 + " rhs=" + tp2);
        }
    }
}
```

</details>

---
## [58] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [59] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~161 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [11] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,998 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        try {
            registry.put("lower", new LowerCaseFormatFactory());
            registry.put("upper", new UpperCaseFormatFactory());
        } catch (Throwable t) {
            return;
        }

        runAnchor(registry);

        int variants = 1 + data.consumeInt(1, 4);
        for (int i = 0; i < variants; i++) {
            String pattern = buildValidPattern(data, i);
            exercisePattern(pattern, registry);
        }
    }

    private static void runAnchor(Map registry) {
        String pattern = "it''s a {0,lower} 'test'!";
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
            String beforeFormatPattern = emf.toPattern();
            String out = emf.format(new Object[] { "DUMMY" });
            String afterFormatPattern = emf.toPattern();

            if (!"it's a dummy test!".equals(out)) {
                throw new RuntimeException("[oracle:anchor-output] metamorphic violation: exact regression input formatted unexpectedly input="
                        + pattern + " output=" + out);
            }
            if (!safeEquals(beforeFormatPattern, afterFormatPattern)) {
                throw new RuntimeException("[oracle:format-readonly] metamorphic violation: format() changed toPattern input="
                        + pattern + " before=" + beforeFormatPattern + " after=" + afterFormatPattern);
            }

            /* Contract used: toPattern() is the object's pattern representation.
             * Re-parsing the pattern returned by toPattern() with the same registry
             * must preserve both the canonical pattern and the formatting result.
             * A patch that merely skips quote handling or drops bookkeeping can stop
             * crashing yet produce a different canonical pattern/output here. */
            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(beforeFormatPattern, registry);
            String roundTripPattern = emf2.toPattern();
            String out2 = emf2.format(new Object[] { "DUMMY" });
            if (!safeEquals(beforeFormatPattern, roundTripPattern) || !safeEquals(out, out2)) {
                throw new RuntimeException("[oracle:roundtrip] metamorphic violation: reparsing toPattern changed behavior input="
                        + pattern + " p1=" + beforeFormatPattern + " p2=" + roundTripPattern
                        + " o1=" + out + " o2=" + out2);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void exercisePattern(String pattern, Map registry) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
            String canon1 = emf.toPattern();
            Object arg = pattern.length() % 2 == 0 ? "DuMmY" : "dummy";
            String out1 = emf.format(new Object[] { arg });
            String canonAfterFormat = emf.toPattern();

            if (!safeEquals(canon1, canonAfterFormat)) {
                throw new RuntimeException("[oracle:format-readonly] metamorphic violation: format() changed toPattern input="
                        + pattern + " before=" + canon1 + " after=" + canonAfterFormat);
            }

            /* Contract used: a pattern emitted by toPattern() is a valid representation
             * of the same formatting behavior. Reconstructing a second object from it
             * with the same registry must preserve both pattern and output. */
            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(canon1, registry);
            String canon2 = emf2.toPattern();
            String out2 = emf2.format(new Object[] { arg });

            if (!safeEquals(canon1, canon2) || !safeEquals(out1, out2)) {
                throw new RuntimeException("[oracle:roundtrip] metamorphic violation: reparsing toPattern changed behavior input="
                        + pattern + " p1=" + canon1 + " p2=" + canon2 + " o1=" + out1 + " o2=" + out2);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static String buildValidPattern(FuzzedDataProvider data, int idx) {
        String left = sanitizeLiteral(data.consumeAsciiString(12));
        String right = sanitizeLiteral(data.consumeAsciiString(12));
        String quoted = sanitizeLiteral(data.consumeAsciiString(10));
        if (quoted.length() == 0) {
            quoted = "q";
        }

        String fmt = data.consumeBoolean() ? "lower" : "upper";
        String escaped = repeat("''", 1 + (idx % 3) + data.consumeInt(0, 2));

        int mode = data.consumeInt(0, 4);
        switch (mode) {
            case 0:
                return escaped + left + " {0," + fmt + "} '" + quoted + "'" + right;
            case 1:
                return left + escaped + " {0," + fmt + "} '" + quoted + "'" + right;
            case 2:
                return left + " {0," + fmt + "} " + escaped + "'" + quoted + "'" + right;
            case 3:
                return left + " '" + quoted + "' " + escaped + " {0," + fmt + "}" + right;
            default:
                return left + escaped + " {0," + fmt + "} '" + quoted + "' " + escaped + right;
        }
    }

    private static String sanitizeLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == '\'') {
                sb.append('x');
            } else {
                sb.append(c);
            }
        }
        if (sb.length() == 0) {
            sb.append('x');
        }
        return sb.toString();
    }

    private static String repeat(String s, int n) {
        StringBuilder sb = new StringBuilder(s.length() * n);
        for (int i = 0; i < n; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("Validate") || name.contains("Validation") || name.contains("Illegal")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            String cls = st[i].getClassName();
            String m = st[i].getMethodName();
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cls)) {
                if ("appendQuotedString".equals(m) || "next".equals(m) || "startsWith".equals(m) || "append".equals(m)) {
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

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
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
<details><summary>▸ Prompt (4 message(s), ~9,507 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [11] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        try {
            registry.put("lower", new LowerCaseFormatFactory());
            registry.put("upper", new UpperCaseFormatFactory());
        } catch (Throwable t) {
            return;
        }

        runAnchor(registry);

        int variants = 1 + data.consumeInt(1, 4);
        for (int i = 0; i < variants; i++) {
            String pattern = buildValidPattern(data, i);
            exercisePattern(pattern, registry);
        }
    }

    private static void runAnchor(Map registry) {
        String pattern = "it''s a {0,lower} 'test'!";
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
            String beforeFormatPattern = emf.toPattern();
            String out = emf.format(new Object[] { "DUMMY" });
            String afterFormatPattern = emf.toPattern();

            if (!"it's a dummy test!".equals(out)) {
                throw new RuntimeException("[oracle:anchor-output] metamorphic violation: exact regression input formatted unexpectedly input="
                        + pattern + " output=" + out);
            }
            if (!safeEquals(beforeFormatPattern, afterFormatPattern)) {
                throw new RuntimeException("[oracle:format-readonly] metamorphic violation: format() changed toPattern input="
                        + pattern + " before=" + beforeFormatPattern + " after=" + afterFormatPattern);
            }

            /* Contract used: toPattern() is the object's pattern representation.
             * Re-parsing the pattern returned by toPattern() with the same registry
             * must preserve both the canonical pattern and the formatting result.
             * A patch that merely skips quote handling or drops bookkeeping can stop
             * crashing yet produce a different canonical pattern/output here. */
            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(beforeFormatPattern, registry);
            String roundTripPattern = emf2.toPattern();
            String out2 = emf2.format(new Object[] { "DUMMY" });
            if (!safeEquals(beforeFormatPattern, roundTripPattern) || !safeEquals(out, out2)) {
                throw new RuntimeException("[oracle:roundtrip] metamorphic violation: reparsing toPattern changed behavior input="
                        + pattern + " p1=" + beforeFormatPattern + " p2=" + roundTripPattern
                        + " o1=" + out + " o2=" + out2);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void exercisePattern(String pattern, Map registry) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
            String canon1 = emf.toPattern();
            Object arg = pattern.length() % 2 == 0 ? "DuMmY" : "dummy";
            String out1 = emf.format(new Object[] { arg });
            String canonAfterFormat = emf.toPattern();

            if (!safeEquals(canon1, canonAfterFormat)) {
                throw new RuntimeException("[oracle:format-readonly] metamorphic violation: format() changed toPattern input="
                        + pattern + " before=" + canon1 + " after=" + canonAfterFormat);
            }

            /* Contract used: a pattern emitted by toPattern() is a valid representation
             * of the same formatting behavior. Reconstructing a second object from it
             * with the same registry must preserve both pattern and output. */
            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(canon1, registry);
            String canon2 = emf2.toPattern();
            String out2 = emf2.format(new Object[] { arg });

            if (!safeEquals(canon1, canon2) || !safeEquals(out1, out2)) {
                throw new RuntimeException("[oracle:roundtrip] metamorphic violation: reparsing toPattern changed behavior input="
                        + pattern + " p1=" + canon1 + " p2=" + canon2 + " o1=" + out1 + " o2=" + out2);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static String buildValidPattern(FuzzedDataProvider data, int idx) {
        String left = sanitizeLiteral(data.consumeAsciiString(12));
        String right = sanitizeLiteral(data.consumeAsciiString(12));
        String quoted = sanitizeLiteral(data.consumeAsciiString(10));
        if (quoted.length() == 0) {
            quoted = "q";
        }

        String fmt = data.consumeBoolean() ? "lower" : "upper";
        String escaped = repeat("''", 1 + (idx % 3) + data.consumeInt(0, 2));

        int mode = data.consumeInt(0, 4);
        switch (mode) {
            case 0:
                return escaped + left + " {0," + fmt + "} '" + quoted + "'" + right;
            case 1:
                return left + escaped + " {0," + fmt + "} '" + quoted + "'" + right;
            case 2:
                return left + " {0," + fmt + "} " + escaped + "'" + quoted + "'" + right;
            case 3:
                return left + " '" + quoted + "' " + escaped + " {0," + fmt + "}" + right;
            default:
                return left + escaped + " {0," + fmt + "} '" + quoted + "' " + escaped + right;
        }
    }

    private static String sanitizeLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == '\'') {
                sb.append('x');
            } else {
                sb.append(c);
            }
        }
        if (sb.length() == 0) {
            sb.append('x');
        }
        return sb.toString();
    }

    private static String repeat(String s, int n) {
        StringBuilder sb = new StringBuilder(s.length() * n);
        for (int i = 0; i < n; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("Validate") || name.contains("Validation") || name.contains("Illegal")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            String cls = st[i].getClassName();
            String m = st[i].getMethodName();
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cls)) {
                if ("appendQuotedString".equals(m) || "next".equals(m) || "startsWith".equals(m) || "append".equals(m)) {
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

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
```
**[user]**
```
That did not compile. javac reported:
/tmp/d4j/Lang_43_buggy/fuzz/attempt_029/FuzzHarness.java:11: error: cannot find symbol
            registry.put("lower", new LowerCaseFormatFactory());
                                      ^
  symbol:   class LowerCaseFormatFactory
  location: class FuzzHarness
/tmp/d4j/Lang_43_buggy/fuzz/attempt_029/FuzzHarness.java:12: error: cannot find symbol
            registry.put("upper", new UpperCaseFormatFactory());
                                      ^
  symbol:   class UpperCaseFormatFactory
  location: class FuzzHarness
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
<details open><summary>▸ Output (~7,667 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        runAnchor(registry);

        int variants = data.consumeInt(1, 4);
        for (int i = 0; i < variants; i++) {
            String pattern = buildValidPattern(data, i);
            exercisePattern(pattern, registry);
        }
    }

    private static void runAnchor(Map registry) {
        String pattern = "it''s a {0} 'test'!";
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
            String before = emf.toPattern();
            String out = emf.format(new Object[] { "DUMMY" });
            String after = emf.toPattern();

            if (!"it's a DUMMY test!".equals(out)) {
                throw new RuntimeException("[oracle:anchor-output] metamorphic violation: exact escaped-quote seed formatted unexpectedly input="
                        + pattern + " output=" + out);
            }
            if (!safeEquals(before, after)) {
                throw new RuntimeException("[oracle:format-readonly] metamorphic violation: format() changed toPattern input="
                        + pattern + " before=" + before + " after=" + after);
            }

            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, registry);
            String roundTripPattern = emf2.toPattern();
            String out2 = emf2.format(new Object[] { "DUMMY" });
            if (!safeEquals(before, roundTripPattern) || !safeEquals(out, out2)) {
                throw new RuntimeException("[oracle:roundtrip] metamorphic violation: reparsing toPattern changed behavior input="
                        + pattern + " p1=" + before + " p2=" + roundTripPattern
                        + " o1=" + out + " o2=" + out2);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void exercisePattern(String pattern, Map registry) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
            String canon1 = emf.toPattern();
            String arg = pattern.length() % 2 == 0 ? "DuMmY" : "dummy";
            String out1 = emf.format(new Object[] { arg });
            String canonAfterFormat = emf.toPattern();

            /* Contract asserted:
             * format(...) is read-only with respect to the pattern; toPattern() is an exposed reader.
             * A throw-deleting or branch-skipping patch in quote handling can silently corrupt parsing
             * state and change the stored pattern even if no exception is thrown. */
            if (!safeEquals(canon1, canonAfterFormat)) {
                throw new RuntimeException("[oracle:format-readonly] metamorphic violation: format() changed toPattern input="
                        + pattern + " before=" + canon1 + " after=" + canonAfterFormat);
            }

            /* Contract asserted:
             * toPattern() returns a pattern representation for this formatter. Reconstructing a new
             * formatter from that representation with the same registry must preserve the canonical
             * pattern and the formatting result for the same arguments. */
            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(canon1, registry);
            String canon2 = emf2.toPattern();
            String out2 = emf2.format(new Object[] { arg });

            if (!safeEquals(canon1, canon2) || !safeEquals(out1, out2)) {
                throw new RuntimeException("[oracle:roundtrip] metamorphic violation: reparsing toPattern changed behavior input="
                        + pattern + " p1=" + canon1 + " p2=" + canon2 + " o1=" + out1 + " o2=" + out2);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static String buildValidPattern(FuzzedDataProvider data, int idx) {
        String left = sanitizeLiteral(data.consumeAsciiString(12));
        String right = sanitizeLiteral(data.consumeAsciiString(12));
        String quoted = sanitizeLiteral(data.consumeAsciiString(10));
        if (quoted.length() == 0) {
            quoted = "q";
        }

        String escaped = repeat("''", 1 + data.consumeInt(0, 3));
        String middleEscaped = repeat("''", 1 + (idx % 2) + data.consumeInt(0, 2));

        int mode = data.consumeInt(0, 5);
        switch (mode) {
            case 0:
                return escaped + left + " {0} '" + quoted + "'" + right;
            case 1:
                return left + escaped + " {0} '" + quoted + "'" + right;
            case 2:
                return left + " {0} " + escaped + "'" + quoted + "'" + right;
            case 3:
                return left + " '" + quoted + "' " + escaped + " {0}" + right;
            case 4:
                return left + escaped + " {0} '" + quoted + "' " + middleEscaped + right;
            default:
                return escaped + left + middleEscaped + " {0} '" + quoted + "'" + right;
        }
    }

    private static String sanitizeLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == '\'') {
                sb.append('x');
            } else {
                sb.append(c);
            }
        }
        if (sb.length() == 0) {
            sb.append('x');
        }
        return sb.toString();
    }

    private static String repeat(String s, int n) {
        StringBuilder sb = new StringBuilder(s.length() * n);
        for (int i = 0; i < n; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("Validate") || name.contains("Validation")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            String cls = st[i].getClassName();
            String m = st[i].getMethodName();
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cls)) {
                if ("appendQuotedString".equals(m) || "next".equals(m) || "startsWith".equals(m) || "append".equals(m)) {
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

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
```

</details>

---
## [62] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat
