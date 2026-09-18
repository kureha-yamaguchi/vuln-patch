# Pipeline trace — Lang-43

**Code:** `b4b78d5146829e25837ef7e7f105b4ec3b686d1e`

**Patch label:** correct  *(the patch under analysis is a known-CORRECT fix — the pipeline is not told this)*

**Outcome:** candidate_experiment (buggy build only)

**Patch under analysis:**
```diff
--- /src/java/org/apache/commons/lang/text/ExtendedMessageFormat.java
+++ /src/java/org/apache/commons/lang/text/ExtendedMessageFormat.java
@@ -418,7 +418,8 @@ public class ExtendedMessageFormat extends MessageFormat {
             StringBuffer appendTo, boolean escapingOn) {
         int start = pos.getIndex();
         char[] c = pattern.toCharArray();
+        next(pos);
         if (escapingOn && c[start] == QUOTE) {
             return appendTo == null ? null : appendTo.append(QUOTE);
         }
         int lastHold = start;
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
<details><summary>▸ output — FailureTest(test_class='org.apache.commons.lang.text.ExtendedMessageFormatTest', test_method='testEs (~739 chars)</summary>

**output:** 
```json
[
  "FailureTest(test_class='org.apache.commons.lang.text.ExtendedMessageFormatTest', test_method='testEscapedQuote_LANG_477', source_path='/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/Lang_43_buggy/src/test/org/apache/commons/lang/text/ExtendedMessageFormatTest.java', method_source='    public void testEscapedQuote_LANG_477() {\\n        String pattern = \"it\\'\\'s a {0,lower} \\'test\\'!\";\\n        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);\\n        assertEquals(\"it\\'s a dummy test!\", emf.format(new Object[] {\"DUMMY\"}));\\n    }', exception_type='java.lang.OutOfMemoryError', failure_message=None, support_source=None)"
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
<details><summary>▸ output — modified_files": [ (~24,612 chars)</summary>

**output:** 
```json
{
  "modified_files": [
    "src/java/org/apache/commons/lang/text/ExtendedMessageFormat.java"
  ],
  "patch_text": "--- /src/java/org/apache/commons/lang/text/ExtendedMessageFormat.java\n+++ /src/java/org/apache/commons/lang/text/ExtendedMessageFormat.java\n@@ -418,7 +418,8 @@ public class ExtendedMessageFormat extends MessageFormat {\n             StringBuffer appendTo, boolean escapingOn) {\n         int start = pos.getIndex();\n         char[] c = pattern.toCharArray();\n+        next(pos);\n         if (escapingOn && c[start] == QUOTE) {\n             return appendTo == null ? null : appendTo.append(QUOTE);\n         }\n         int lastHold = start;",
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
      "xref_names": [
        "[org.apache.commons.lang.text.ExtendedMessageFormat].applyPattern(String)",
        "[org.apache.commons.lang.text.ExtendedMessageFormat].insertFormats(String,java.util.ArrayList)"
      ],
      "reachable_edges": [
        [
          "[org.apache.commons.lang.text.ExtendedMessageFormat].appendQuotedString(String,java.text.ParsePosition,StringBuffer,boolean)",
          "[java.text.ParsePosition].getIndex()"
        ],
        [
          "[org.apache.commons.lang.text.ExtendedMessageFormat].appendQuotedString(String,java.text.ParsePosition,StringBuffer,boolean)",
          "[String].toCharArray()"
        ],
        [
          "[org.apache.commons.lang.text.ExtendedMessageFormat].appendQuotedString(String,java.text.ParsePosition,StringBuffer,boolean)",
          "[StringBuffer].append(char)"
        ],
        [
          "[org.apache.commons.lang.text.ExtendedMessageFormat].appendQuotedString(String,java.text.ParsePosition,StringBuffer,boolean)",
          "[java.text.ParsePosition].getIndex()"
        ],
        [
          "[org.apache.commons.lang.text.ExtendedMessageFormat].appendQuotedString(String,java.text.ParsePosition,StringBuffer,boolean)",
          "[String].length()"
        ],
        [
          "[org.apache.commons.lang.text.ExtendedMessageFormat].appendQuotedString(String,java.text.ParsePosition,StringBuffer,boolean)",
          "[String].substring(org.apache.commons.lang.text.ExtendedMessageFormat)"
        ],
        [
          "[org.apache.commons.lang.text.ExtendedMessageFormat].appendQuotedString(String,java.text.ParsePosition,StringBuffer,boolean)",
          "[org.apache.commons.lang.text.ExtendedMessageFormat].startsWith(String)"
        ],
        [
          "[org.apache.commons.lang.text.ExtendedMessageFormat].appendQuotedString(String,java.text.ParsePosition,StringBuffer,boolean)",
          "[java.text.ParsePosition].getIndex()"
        ],
        [
          "[org.apache.commons.lang.text.ExtendedMessageFormat].appendQuotedString(String,java.text.ParsePosition,StringBuffer,boolean)",
          "[StringBuffer].append(org.apache.commons.lang.text.ExtendedMessageFormat,org.apache.commons.lang.text.ExtendedMessageFormat,org.apache.commons.lang.text.ExtendedMessageFormat)"
        ],
        [
          "[org.apache.commons.lang.text.ExtendedMessageFormat].appendQuotedString(String,java.text.ParsePosition,StringBuffer,boolean)",
          "[org.apache.commons.lang.text.ExtendedMessageFormat].append(char)"
        ],
        [
          "[org.apache.commons.lang.text.ExtendedMessageFormat].appendQuotedString(String,java.text.ParsePosition,StringBuffer,boolean)",
          "[String].length()"
        ],
        [
          "[org.apache.commons.lang.text.ExtendedMessageFormat].appendQuotedString(String,java.text.ParsePosition,StringBuffer,boolean)",
          "[java.text.ParsePosition].setIndex(org.apache.commons.lang.text.ExtendedMessageFormat)"
        ],
        [
          "[org.apache.commons.lang.text.ExtendedMessageFormat].appendQuotedString(String,java.text.ParsePosition,StringBuffer,boolean)",
          "[java.text.ParsePosition].getIndex()"
        ],
        [
          "[org.apache.commons.lang.text.ExtendedMessageFormat].appendQuotedString(String,java.text.ParsePosition,StringBuffer,boolean)",
          "[java.text.ParsePosition].getIndex()"
        ],
        [
          "[org.apache.commons.lang.text.ExtendedMessageFormat].appendQuotedString(String,java.text.ParsePosition,StringBuffer,boolean)",
          "[org.apache.commons.lang.text.ExtendedMessageFormat].next(java.text.ParsePosition)"
        ],
        [
          "[org.apache.commons.lang.text.ExtendedMessageFormat].appendQuotedString(String,java.text.ParsePosition,StringBuffer,boolean)",
          "[java.text.ParsePosition].getIndex()"
        ],
        [
          "[org.apache.commons.lang.text.ExtendedMessageFormat].appendQuotedString(String,java.text.ParsePosition,StringBuffer,boolean)",
          "[StringBuffer].append(org.apache.commons.lang.text.ExtendedMessageFormat,org.apache.commons.lang.text.ExtendedMessageFormat,org.apache.commons.lang.text.ExtendedMessageFormat)"
        ],
        [
          "[org.apache.commons.lang.text.ExtendedMessageFormat].appendQuotedString(String,java.text.ParsePosition,StringBuffer,boolean)",
          "[org.apache.commons.lang.text.ExtendedMessageFormat].next(java.text.ParsePosition)"
        ],
        [
          "[org.apache.commons.lang.text.ExtendedMessageFormat].appendQuotedString(String,java.text.ParsePosition,StringBuffer,boolean)",
          "[IllegalArgumentException].<init>(String)"
        ],
        [
          "[org.apache.commons.lang.text.ExtendedMessageFormat].next(java.text.ParsePosition)",
          "[java.text.ParsePosition].getIndex()"
        ],
        [
          "[org.apache.commons.lang.text.ExtendedMessageFormat].next(java.text.ParsePosition)",
          "[java.text.ParsePosition].setIndex(int)"
        ]
      ],
      "reachable_depth": {
        "[org.apache.commons.lang.text.ExtendedMessageFormat].appendQuotedString(String,java.text.ParsePosition,StringBuffer,boolean)": 0,
        "[java.text.ParsePosition].getIndex()": 1,
        "[String].toCharArray()": 1,
        "[StringBuffer].append(char)": 1,
        "[String].length()": 1,
        "[String].substring(org.apache.commons.lang.text.ExtendedMessageFormat)": 1,
        "[org.apache.commons.lang.text.ExtendedMessageFormat].startsWith(String)": 1,
        "[StringBuffer].append(org.apache.commons.lang.text.ExtendedMessageFormat,org.apache.commons.lang.text.ExtendedMessageFormat,org.apache.commons.lang.text.ExtendedMessageFormat)": 1,
        "[org.apache.commons.lang.text.ExtendedMessageFormat].append(char)": 1,
        "[java.text.ParsePosition].setIndex(org.apache.commons.lang.text.ExtendedMessageFormat)": 1,
        "[org.apache.commons.lang.text.ExtendedMessageFormat].next(java.text.ParsePosition)": 1,
        "[IllegalArgumentException].<init>(String)": 1,
        "[java.text.ParsePosition].setIndex(int)": 2
      },
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
  ],
  "reachable_edges": [
    [
      "[org.apache.commons.lang.text.ExtendedMessageFormat].appendQuotedString(String,java.text.ParsePosition,StringBuffer,boolean)",
      "[java.text.ParsePosition].getIndex()"
    ],
    [
      "[org.apache.commons.lang.text.ExtendedMessageFormat].appendQuotedString(String,java.text.ParsePosition,StringBuffer,boolean)",
      "[String].toCharArray()"
    ],
    [
      "[org.apache.commons.lang.text.ExtendedMessageFormat].appendQuotedString(String,java.text.ParsePosition,StringBuffer,boolean)",
      "[StringBuffer].append(char)"
    ],
    [
      "[org.apache.commons.lang.text.ExtendedMessageFormat].appendQuotedString(String,java.text.ParsePosition,StringBuffer,boolean)",
      "[String].length()"
    ],
    [
      "[org.apache.commons.lang.text.ExtendedMessageFormat].appendQuotedString(String,java.text.ParsePosition,StringBuffer,boolean)",
      "[String].substring(org.apache.commons.lang.text.ExtendedMessageFormat)"
    ],
    [
      "[org.apache.commons.lang.text.ExtendedMessageFormat].appendQuotedString(String,java.text.ParsePosition,StringBuffer,boolean)",
      "[org.apache.commons.lang.text.ExtendedMessageFormat].startsWith(String)"
    ],
    [
      "[org.apache.commons.lang.text.ExtendedMessageFormat].appendQuotedString(String,java.text.ParsePosition,StringBuffer,boolean)",
      "[StringBuffer].append(org.apache.commons.lang.text.ExtendedMessageFormat,org.apache.commons.lang.text.ExtendedMessageFormat,org.apache.commons.lang.text.ExtendedMessageFormat)"
    ],
    [
      "[org.apache.commons.lang.text.ExtendedMessageFormat].appendQuotedString(String,java.text.ParsePosition,StringBuffer,boolean)",
      "[org.apache.commons.lang.text.ExtendedMessageFormat].append(char)"
    ],
    [
      "[org.apache.commons.lang.text.ExtendedMessageFormat].appendQuotedString(String,java.text.ParsePosition,StringBuffer,boolean)",
      "[java.text.ParsePosition].setIndex(org.apache.commons.lang.text.ExtendedMessageFormat)"
    ],
    [
      "[org.apache.commons.lang.text.ExtendedMessageFormat].appendQuotedString(String,java.text.ParsePosition,StringBuffer,boolean)",
      "[org.apache.commons.lang.text.ExtendedMessageFormat].next(java.text.ParsePosition)"
    ],
    [
      "[org.apache.commons.lang.text.ExtendedMessageFormat].appendQuotedString(String,java.text.ParsePosition,StringBuffer,boolean)",
      "[IllegalArgumentException].<init>(String)"
    ],
    [
      "[org.apache.commons.lang.text.ExtendedMessageFormat].next(java.text.ParsePosition)",
      "[java.text.ParsePosition].getIndex()"
    ],
    [
      "[org.apache.commons.lang.text.ExtendedMessageFormat].next(java.text.ParsePosition)",
      "[java.text.ParsePosition].setIndex(int)"
    ]
  ],
  "reachable_depth": {
    "[org.apache.commons.lang.text.ExtendedMessageFormat].appendQuotedString(String,java.text.ParsePosition,StringBuffer,boolean)": 0,
    "[java.text.ParsePosition].getIndex()": 1,
    "[String].toCharArray()": 1,
    "[StringBuffer].append(char)": 1,
    "[String].length()": 1,
    "[String].substring(org.apache.commons.lang.text.ExtendedMessageFormat)": 1,
    "[org.apache.commons.lang.text.ExtendedMessageFormat].startsWith(String)": 1,
    "[StringBuffer].append(org.apache.commons.lang.text.ExtendedMessageFormat,org.apache.commons.lang.text.ExtendedMessageFormat,org.apache.commons.lang.text.ExtendedMessageFormat)": 1,
    "[org.apache.commons.lang.text.ExtendedMessageFormat].append(char)": 1,
    "[java.text.ParsePosition].setIndex(org.apache.commons.lang.text.ExtendedMessageFormat)": 1,
    "[org.apache.commons.lang.text.ExtendedMessageFormat].next(java.text.ParsePosition)": 1,
    "[IllegalArgumentException].<init>(String)": 1,
    "[java.text.ParsePosition].setIndex(int)": 2
  }
}
```

</details>

---
## [3] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~32,153 chars)</summary>

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
@@ -418,7 +418,8 @@ public class ExtendedMessageFormat extends MessageFormat {
             StringBuffer appendTo, boolean escapingOn) {
         int start = pos.getIndex();
         char[] c = pattern.toCharArray();
+        next(pos);
         if (escapingOn && c[start] == QUOTE) {
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

GUARDRAIL (all options): prefer a property that holds for EVERY correct implementation, and state the documented guarantee behind each check in a comment (an automated soundness reviewer screens every fired assertion and reads it). Do NOT retreat to vacuous checks (finiteness, non-null, no-throw) to stay safe — a vacuous check guarantees a missed bug, which nothing downstream can recover; the reviewer backstops a justified-but-borderline check, though it is not infallible.
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
<details open><summary>▸ Output (~9,123 chars)</summary>

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

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = makeRegistry();

        runCase(ANCHOR_PATTERN, registry);

        String a = sanitize(data.consumeAsciiString(32));
        String b = sanitize(data.consumeAsciiString(32));
        String c = sanitize(data.consumeAsciiString(32));
        String d = sanitize(data.consumeAsciiString(32));
        String fmt = data.consumeBoolean() ? "lower" : "upper";

        String[] patterns = new String[] {
            "''" + nonEmpty(a) + " {0," + fmt + "} '" + nonEmpty(b) + "'",
            nonEmpty(a) + "''" + nonEmpty(b) + " {0," + fmt + "} '" + nonEmpty(c) + "'",
            nonEmpty(a) + " {0," + fmt + "} '' " + nonEmpty(b),
            nonEmpty(a) + " '' " + nonEmpty(b) + " {0," + fmt + "} '" + nonEmpty(c) + "' " + nonEmpty(d),
            "pre '' mid {0," + fmt + "} 'lit' post",
            nonEmpty(a) + "''{0," + fmt + "}",
            "{0," + fmt + "} '' " + nonEmpty(a) + " '" + nonEmpty(b) + "'"
        };

        for (int i = 0; i < patterns.length; i++) {
            runCase(patterns[i], registry);
        }
    }

    private static void runCase(String pattern, Map registry) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, Locale.getDefault(), registry);

            String before = emf.toPattern();
            String out1;
            try {
                out1 = emf.format(new Object[] { "DuMmY" });
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throw rethrow(t);
                }
                return;
            }
            String after = emf.toPattern();

            try {
                ExtendedMessageFormat emf2 = new ExtendedMessageFormat(pattern, Locale.getDefault(), registry);
                String before2 = emf2.toPattern();
                String out2 = emf2.format(new Object[] { "DuMmY" });
                String after2 = emf2.toPattern();

                // Contract used for this oracle:
                // formatting is a read-only operation with respect to the message pattern,
                // so toPattern() must not change before/after format();
                // and two independently constructed objects with the same pattern/registry
                // must expose the same toPattern() and produce the same formatted output.
                if (!safeEquals(before, after)) {
                    throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: toPattern changed across format input="
                            + pattern + " before=" + before + " after=" + after);
                }
                if (!safeEquals(before2, after2)) {
                    throw new RuntimeException("[oracle:topattern-stable2] metamorphic violation: toPattern changed across format on fresh object input="
                            + pattern + " before=" + before2 + " after=" + after2);
                }
                if (!safeEquals(before, before2) || !safeEquals(after, after2)) {
                    throw new RuntimeException("[oracle:constructor-determinism] metamorphic violation: identical constructions disagree input="
                            + pattern + " lhsBefore=" + before + " rhsBefore=" + before2
                            + " lhsAfter=" + after + " rhsAfter=" + after2);
                }
                if (!safeEquals(out1, out2)) {
                    throw new RuntimeException("[oracle:format-determinism] metamorphic violation: identical constructions disagree on output input="
                            + pattern + " lhs=" + out1 + " rhs=" + out2);
                }
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throw rethrow(t);
                }
                if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                }
                return;
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw rethrow(t);
            }
        }
    }

    private static Map makeRegistry() {
        HashMap registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String n = cur.getClass().getName();
            if (n.contains("Validate") || n.contains("Validation") || n.contains("Invalid")) {
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
            String cls = st[i].getClassName();
            String m = st[i].getMethodName();
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cls)
                    && ("appendQuotedString".equals(m)
                        || "next".equals(m)
                        || "append".equals(m)
                        || "startsWith".equals(m))) {
                return true;
            }
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                    && ("append".equals(m) || "startsWith".equals(m))) {
                return true;
            }
        }
        return false;
    }

    private static RuntimeException rethrow(Throwable t) {
        if (t instanceof RuntimeException) {
            return (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        return new RuntimeException(t);
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'' || c == '{' || c == '}' || c == ',') {
                sb.append('x');
            } else if (c < 32 || c > 126) {
                sb.append('x');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String nonEmpty(String s) {
        return (s == null || s.length() == 0) ? "x" : s;
    }

    private static boolean safeEquals(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
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
            String s = String.valueOf(obj);
            toAppendTo.append(s.toLowerCase(locale));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source;
        }
    }

    static final class UpperCaseFormat extends Format {
        private final Locale locale;

        UpperCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(s.toUpperCase(locale));
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
## [4] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [5] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~12,089 chars, 2 new)</summary>

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

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = makeRegistry();

        runCase(ANCHOR_PATTERN, registry);

        String a = sanitize(data.consumeAsciiString(32));
        String b = sanitize(data.consumeAsciiString(32));
        String c = sanitize(data.consumeAsciiString(32));
        String d = sanitize(data.consumeAsciiString(32));
        String fmt = data.consumeBoolean() ? "lower" : "upper";

        String[] patterns = new String[] {
            "''" + nonEmpty(a) + " {0," + fmt + "} '" + nonEmpty(b) + "'",
            nonEmpty(a) + "''" + nonEmpty(b) + " {0," + fmt + "} '" + nonEmpty(c) + "'",
            nonEmpty(a) + " {0," + fmt + "} '' " + nonEmpty(b),
            nonEmpty(a) + " '' " + nonEmpty(b) + " {0," + fmt + "} '" + nonEmpty(c) + "' " + nonEmpty(d),
            "pre '' mid {0," + fmt + "} 'lit' post",
            nonEmpty(a) + "''{0," + fmt + "}",
            "{0," + fmt + "} '' " + nonEmpty(a) + " '" + nonEmpty(b) + "'"
        };

        for (int i = 0; i < patterns.length; i++) {
            runCase(patterns[i], registry);
        }
    }

    private static void runCase(String pattern, Map registry) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, Locale.getDefault(), registry);

            String before = emf.toPattern();
            String out1;
            try {
                out1 = emf.format(new Object[] { "DuMmY" });
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throw rethrow(t);
                }
                return;
            }
            String after = emf.toPattern();

            try {
                ExtendedMessageFormat emf2 = new ExtendedMessageFormat(pattern, Locale.getDefault(), registry);
                String before2 = emf2.toPattern();
                String out2 = emf2.format(new Object[] { "DuMmY" });
                String after2 = emf2.toPattern();

                // Contract used for this oracle:
                // formatting is a read-only operation with respect to the message pattern,
                // so toPattern() must not change before/after format();
                // and two independently constructed objects with the same pattern/registry
                // must expose the same toPattern() and produce the same formatted output.
                if (!safeEquals(before, after)) {
                    throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: toPattern changed across format input="
                            + pattern + " before=" + before + " after=" + after);
                }
                if (!safeEquals(before2, after2)) {
                    throw new RuntimeException("[oracle:topattern-stable2] metamorphic violation: toPattern changed across format on fresh object input="
                            + pattern + " before=" + before2 + " after=" + after2);
                }
                if (!safeEquals(before, before2) || !safeEquals(after, after2)) {
                    throw new RuntimeException("[oracle:constructor-determinism] metamorphic violation: identical constructions disagree input="
                            + pattern + " lhsBefore=" + before + " rhsBefore=" + before2
                            + " lhsAfter=" + after + " rhsAfter=" + after2);
                }
                if (!safeEquals(out1, out2)) {
                    throw new RuntimeException("[oracle:format-determinism] metamorphic violation: identical constructions disagree on output input="
                            + pattern + " lhs=" + out1 + " rhs=" + out2);
                }
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throw rethrow(t);
                }
                if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                }
                return;
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw rethrow(t);
            }
        }
    }

    private static Map makeRegistry() {
        HashMap registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String n = cur.getClass().getName();
            if (n.contains("Validate") || n.contains("Validation") || n.contains("Invalid")) {
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
            String cls = st[i].getClassName();
            String m = st[i].getMethodName();
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cls)
                    && ("appendQuotedString".equals(m)
                        || "next".equals(m)
                        || "append".equals(m)
                        || "startsWith".equals(m))) {
                return true;
            }
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                    && ("append".equals(m) || "startsWith".equals(m))) {
                return true;
            }
        }
        return false;
    }

    private static RuntimeException rethrow(Throwable t) {
        if (t instanceof RuntimeException) {
            return (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        return new RuntimeException(t);
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'' || c == '{' || c == '}' || c == ',') {
                sb.append('x');
            } else if (c < 32 || c > 126) {
                sb.append('x');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String nonEmpty(String s) {
        return (s == null || s.length() == 0) ? "x" : s;
    }

    private static boolean safeEquals(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
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
            String s = String.valueOf(obj);
            toAppendTo.append(s.toLowerCase(locale));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source;
        }
    }

    static final class UpperCaseFormat extends Format {
        private final Locale locale;

        UpperCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(s.toUpperCase(locale));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source;
        }
    }
}
```
**[user]**
```
That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generated never drove execution into the changed code, or always took a path that does not fault.

The patch under analysis is below. The buggy version is the code BEFORE this patch is applied (the '+' lines are the FIXED code; the buggy code has the '-' lines instead):
--- /src/java/org/apache/commons/lang/text/ExtendedMessageFormat.java
+++ /src/java/org/apache/commons/lang/text/ExtendedMessageFormat.java
@@ -418,7 +418,8 @@ public class ExtendedMessageFormat extends MessageFormat {
             StringBuffer appendTo, boolean escapingOn) {
         int start = pos.getIndex();
         char[] c = pattern.toCharArray();
+        next(pos);
         if (escapingOn && c[start] == QUOTE) {
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
<details open><summary>▸ Output (~6,326 chars)</summary>

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

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = makeRegistry();

        // Exact trigger from the failing test. On the buggy version, construction calls
        // ExtendedMessageFormat.applyPattern(), which reaches appendQuotedString() on the
        // doubled quote in "it''s" and loops until OutOfMemoryError.
        exercise(ANCHOR_PATTERN, registry);

        // Explore the root-cause property from the diff: any pattern containing a quote
        // reaches appendQuotedString(..., escapingOn=true) from applyPattern(). In the
        // buggy version, the method returns without advancing ParsePosition when the
        // current character is QUOTE, so repeated scanning can loop forever.
        String left = clean(data.consumeAsciiString(24));
        String mid = clean(data.consumeAsciiString(24));
        String right = clean(data.consumeAsciiString(24));
        String literal = clean(data.consumeAsciiString(24));
        String fmt = data.consumeBoolean() ? "lower" : "upper";

        String[] patterns = new String[] {
            left + "''" + right,
            left + "''" + mid + " {0," + fmt + "} " + right,
            left + " {0," + fmt + "} '" + literal + "' " + right,
            "'" + literal + "'",
            "''",
            left + " 'x' " + right,
            left + "''s a {0," + fmt + "} 'test'!",
            "{0," + fmt + "} '" + literal + "'",
            left + " {0," + fmt + "} '' " + right
        };

        for (int i = 0; i < patterns.length; i++) {
            exercise(patterns[i], registry);
        }
    }

    private static void exercise(String pattern, Map registry) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

        String before = emf.toPattern();
        String formatted = emf.format(new Object[] { "DuMmY" });
        String after = emf.toPattern();

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(pattern, registry);
        String before2 = emf2.toPattern();
        String formatted2 = emf2.format(new Object[] { "DuMmY" });
        String after2 = emf2.toPattern();

        // Contract/post-condition:
        // format() is read-only with respect to the compiled message pattern, so toPattern()
        // must stay stable across formatting; and two independently constructed formatters
        // from the same pattern/registry must behave identically.
        if (!eq(before, after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: pattern=" + pattern
                    + " before=" + before + " after=" + after);
        }
        if (!eq(before2, after2)) {
            throw new RuntimeException("[oracle:topattern-stable-fresh] metamorphic violation: pattern=" + pattern
                    + " before=" + before2 + " after=" + after2);
        }
        if (!eq(before, before2) || !eq(after, after2)) {
            throw new RuntimeException("[oracle:constructor-determinism] metamorphic violation: pattern=" + pattern
                    + " before1=" + before + " before2=" + before2
                    + " after1=" + after + " after2=" + after2);
        }
        if (!eq(formatted, formatted2)) {
            throw new RuntimeException("[oracle:format-determinism] metamorphic violation: pattern=" + pattern
                    + " out1=" + formatted + " out2=" + formatted2);
        }
    }

    private static Map makeRegistry() {
        HashMap registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static boolean eq(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    private static String clean(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 32 || c > 126 || c == '{' || c == '}' || c == ',') {
                sb.append('x');
            } else {
                sb.append(c);
            }
        }
        if (sb.length() == 0) {
            return "x";
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
            toAppendTo.append(String.valueOf(obj).toLowerCase(locale));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source;
        }
    }

    static final class UpperCaseFormat extends Format {
        private final Locale locale;

        UpperCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            toAppendTo.append(String.valueOf(obj).toUpperCase(locale));
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
## [6] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [7] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~6,642 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
- *[assistant] message: identical to step [5] — not reprinted*
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

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = makeRegistry();

        // Exact trigger from the failing test. On the buggy version, construction calls
        // ExtendedMessageFormat.applyPattern(), which reaches appendQuotedString() on the
        // doubled quote in "it''s" and loops until OutOfMemoryError.
        exercise(ANCHOR_PATTERN, registry);

        // Explore the root-cause property from the diff: any pattern containing a quote
        // reaches appendQuotedString(..., escapingOn=true) from applyPattern(). In the
        // buggy version, the method returns without advancing ParsePosition when the
        // current character is QUOTE, so repeated scanning can loop forever.
        String left = clean(data.consumeAsciiString(24));
        String mid = clean(data.consumeAsciiString(24));
        String right = clean(data.consumeAsciiString(24));
        String literal = clean(data.consumeAsciiString(24));
        String fmt = data.consumeBoolean() ? "lower" : "upper";

        String[] patterns = new String[] {
            left + "''" + right,
            left + "''" + mid + " {0," + fmt + "} " + right,
            left + " {0," + fmt + "} '" + literal + "' " + right,
            "'" + literal + "'",
            "''",
            left + " 'x' " + right,
            left + "''s a {0," + fmt + "} 'test'!",
            "{0," + fmt + "} '" + literal + "'",
            left + " {0," + fmt + "} '' " + right
        };

        for (int i = 0; i < patterns.length; i++) {
            exercise(patterns[i], registry);
        }
    }

    private static void exercise(String pattern, Map registry) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

        String before = emf.toPattern();
        String formatted = emf.format(new Object[] { "DuMmY" });
        String after = emf.toPattern();

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(pattern, registry);
        String before2 = emf2.toPattern();
        String formatted2 = emf2.format(new Object[] { "DuMmY" });
        String after2 = emf2.toPattern();

        // Contract/post-condition:
        // format() is read-only with respect to the compiled message pattern, so toPattern()
        // must stay stable across formatting; and two independently constructed formatters
        // from the same pattern/registry must behave identically.
        if (!eq(before, after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: pattern=" + pattern
                    + " before=" + before + " after=" + after);
        }
        if (!eq(before2, after2)) {
            throw new RuntimeException("[oracle:topattern-stable-fresh] metamorphic violation: pattern=" + pattern
                    + " before=" + before2 + " after=" + after2);
        }
        if (!eq(before, before2) || !eq(after, after2)) {
            throw new RuntimeException("[oracle:constructor-determinism] metamorphic violation: pattern=" + pattern
                    + " before1=" + before + " before2=" + before2
                    + " after1=" + after + " after2=" + after2);
        }
        if (!eq(formatted, formatted2)) {
            throw new RuntimeException("[oracle:format-determinism] metamorphic violation: pattern=" + pattern
                    + " out1=" + formatted + " out2=" + formatted2);
        }
    }

    private static Map makeRegistry() {
        HashMap registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static boolean eq(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    private static String clean(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 32 || c > 126 || c == '{' || c == '}' || c == ',') {
                sb.append('x');
            } else {
                sb.append(c);
            }
        }
        if (sb.length() == 0) {
            return "x";
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
            toAppendTo.append(String.valueOf(obj).toLowerCase(locale));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source;
        }
    }

    static final class UpperCaseFormat extends Format {
        private final Locale locale;

        UpperCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            toAppendTo.append(String.valueOf(obj).toUpperCase(locale));
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
<details open><summary>▸ Output (~3,622 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    private static final String ANCHOR_PATTERN = "it''s a {0,lower} 'test'!";

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        // Exact trigger from the upstream failing test. With registry != null,
        // ExtendedMessageFormat.applyPattern() uses appendQuotedString(..., true).
        // On the buggy version, a pattern position at QUOTE returns without next(pos),
        // so the parser stays on the same quote and grows buffers until OOME.
        new ExtendedMessageFormat(ANCHOR_PATTERN, registry);

        String a = atom(data.consumeAsciiString(16));
        String b = atom(data.consumeAsciiString(16));
        String c = atom(data.consumeAsciiString(16));

        String[] patterns = new String[] {
            a + "''" + b,
            a + "''" + b + " {0}",
            a + " {0} '' " + b,
            a + " 'x' " + b,
            a + " '" + c + "' " + b,
            "''",
            "'x'",
            a + "''s a {0} 'test'!"
        };

        String pattern = patterns[data.consumeInt(0, patterns.length - 1)];
        ExtendedMessageFormat emf1 = new ExtendedMessageFormat(pattern, registry);
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(pattern, registry);

        String before1 = emf1.toPattern();
        String out1 = emf1.format(new Object[] { "DuMmY" });
        String after1 = emf1.toPattern();

        String before2 = emf2.toPattern();
        String out2 = emf2.format(new Object[] { "DuMmY" });
        String after2 = emf2.toPattern();

        // Contract: formatting must not mutate the formatter's pattern representation,
        // and two independently constructed formatters from the same pattern must behave identically.
        if (!eq(before1, after1)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: pattern=" + pattern
                    + " before=" + before1 + " after=" + after1);
        }
        if (!eq(before2, after2)) {
            throw new RuntimeException("[oracle:topattern-stable2] metamorphic violation: pattern=" + pattern
                    + " before=" + before2 + " after=" + after2);
        }
        if (!eq(before1, before2) || !eq(after1, after2)) {
            throw new RuntimeException("[oracle:constructor-determinism] metamorphic violation: pattern=" + pattern
                    + " before1=" + before1 + " before2=" + before2
                    + " after1=" + after1 + " after2=" + after2);
        }
        if (!eq(out1, out2)) {
            throw new RuntimeException("[oracle:format-determinism] metamorphic violation: pattern=" + pattern
                    + " out1=" + out1 + " out2=" + out2);
        }
    }

    private static String atom(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch < 32 || ch > 126 || ch == '\'' || ch == '{' || ch == '}' || ch == ',') {
                sb.append('x');
            } else {
                sb.append(ch);
            }
        }
        if (sb.length() == 0) {
            return "x";
        }
        return sb.toString();
    }

    private static boolean eq(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
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
<details><summary>▸ Prompt (8 message(s), ~4,057 chars, 1 new)</summary>

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
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    private static final String ANCHOR_PATTERN = "it''s a {0,lower} 'test'!";

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        // Exact trigger from the upstream failing test. With registry != null,
        // ExtendedMessageFormat.applyPattern() uses appendQuotedString(..., true).
        // On the buggy version, a pattern position at QUOTE returns without next(pos),
        // so the parser stays on the same quote and grows buffers until OOME.
        new ExtendedMessageFormat(ANCHOR_PATTERN, registry);

        String a = atom(data.consumeAsciiString(16));
        String b = atom(data.consumeAsciiString(16));
        String c = atom(data.consumeAsciiString(16));

        String[] patterns = new String[] {
            a + "''" + b,
            a + "''" + b + " {0}",
            a + " {0} '' " + b,
            a + " 'x' " + b,
            a + " '" + c + "' " + b,
            "''",
            "'x'",
            a + "''s a {0} 'test'!"
        };

        String pattern = patterns[data.consumeInt(0, patterns.length - 1)];
        ExtendedMessageFormat emf1 = new ExtendedMessageFormat(pattern, registry);
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(pattern, registry);

        String before1 = emf1.toPattern();
        String out1 = emf1.format(new Object[] { "DuMmY" });
        String after1 = emf1.toPattern();

        String before2 = emf2.toPattern();
        String out2 = emf2.format(new Object[] { "DuMmY" });
        String after2 = emf2.toPattern();

        // Contract: formatting must not mutate the formatter's pattern representation,
        // and two independently constructed formatters from the same pattern must behave identically.
        if (!eq(before1, after1)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: pattern=" + pattern
                    + " before=" + before1 + " after=" + after1);
        }
        if (!eq(before2, after2)) {
            throw new RuntimeException("[oracle:topattern-stable2] metamorphic violation: pattern=" + pattern
                    + " before=" + before2 + " after=" + after2);
        }
        if (!eq(before1, before2) || !eq(after1, after2)) {
            throw new RuntimeException("[oracle:constructor-determinism] metamorphic violation: pattern=" + pattern
                    + " before1=" + before1 + " before2=" + before2
                    + " after1=" + after1 + " after2=" + after2);
        }
        if (!eq(out1, out2)) {
            throw new RuntimeException("[oracle:format-determinism] metamorphic violation: pattern=" + pattern
                    + " out1=" + out1 + " out2=" + out2);
        }
    }

    private static String atom(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch < 32 || ch > 126 || ch == '\'' || ch == '{' || ch == '}' || ch == ',') {
                sb.append('x');
            } else {
                sb.append(ch);
            }
        }
        if (sb.length() == 0) {
            return "x";
        }
        return sb.toString();
    }

    private static boolean eq(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~4,797 chars)</summary>

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

        // Exact upstream regression trigger. On the buggy version this constructor call
        // reaches applyPattern() -> appendQuotedString(..., true) at the first quote in
        // "it''s...". Because the buggy code does not advance ParsePosition before the
        // early-return path, it reprocesses the same quote forever and eventually throws
        // OutOfMemoryError. Let that unchecked error escape to Jazzer.
        new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);

        // Also exercise close variants with the same root-cause property: a quote seen by
        // applyPattern() while registry != null.
        String word1 = safeAtom(data.consumeAsciiString(12));
        String word2 = safeAtom(data.consumeAsciiString(12));
        String quoted = safeAtom(data.consumeAsciiString(12));
        String fmt = data.consumeBoolean() ? "lower" : "upper";

        String pattern;
        switch (data.consumeInt(0, 5)) {
            case 0:
                pattern = word1 + "''" + word2;
                break;
            case 1:
                pattern = word1 + "''" + word2 + " {0," + fmt + "}";
                break;
            case 2:
                pattern = word1 + " {0," + fmt + "} '" + quoted + "'";
                break;
            case 3:
                pattern = "'" + quoted + "'";
                break;
            case 4:
                pattern = "''";
                break;
            default:
                pattern = word1 + "''s a {0," + fmt + "} 'test'!";
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

        // Post-condition: formatting must not mutate the pattern representation.
        String before = emf.toPattern();
        emf.format(new Object[] { "DuMmY" });
        String after = emf.toPattern();
        if (before != null ? !before.equals(after) : after != null) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: pattern=" + pattern
                    + " before=" + before + " after=" + after);
        }
    }

    private static String safeAtom(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch < 32 || ch > 126 || ch == '\'' || ch == '{' || ch == '}' || ch == ',') {
                sb.append('x');
            } else {
                sb.append(ch);
            }
        }
        return sb.length() == 0 ? "x" : sb.toString();
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
            toAppendTo.append(String.valueOf(obj).toLowerCase(locale));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source;
        }
    }

    static final class UpperCaseFormat extends Format {
        private final Locale locale;

        UpperCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            toAppendTo.append(String.valueOf(obj).toUpperCase(locale));
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
<details><summary>▸ Prompt (2 message(s), ~32,027 chars, 1 new)</summary>

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
@@ -418,7 +418,8 @@ public class ExtendedMessageFormat extends MessageFormat {
             StringBuffer appendTo, boolean escapingOn) {
         int start = pos.getIndex();
         char[] c = pattern.toCharArray();
+        next(pos);
         if (escapingOn && c[start] == QUOTE) {
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

GUARDRAIL (all options): prefer a property that holds for EVERY correct implementation, and state the documented guarantee behind each check in a comment (an automated soundness reviewer screens every fired assertion and reads it). Do NOT retreat to vacuous checks (finiteness, non-null, no-throw) to stay safe — a vacuous check guarantees a missed bug, which nothing downstream can recover; the reviewer backstops a justified-but-borderline check, though it is not infallible.
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
<details open><summary>▸ Output (~8,425 chars)</summary>

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
    private static final Map REGISTRY = createRegistry();

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exercise("it''s a {0,lower} 'test'!", "DUMMY", true);

        String a = sanitize(data.consumeAsciiString(24));
        String b = sanitize(data.consumeAsciiString(24));
        String lit = sanitizeNonEmpty(data.consumeAsciiString(12));
        String arg = sanitizeNonEmpty(data.consumeAsciiString(24));
        if (arg.length() == 0) {
            arg = "DUMMY";
        }

        int variant = data.consumeInt(0, 4);
        String pattern;
        switch (variant) {
            case 0:
                pattern = a + "''" + b + " {0,lower}";
                break;
            case 1:
                pattern = a + " {0,lower} ''" + b;
                break;
            case 2:
                pattern = a + " '" + lit + "' {0,lower}";
                break;
            case 3:
                pattern = a + "''" + b + " {0,lower} '" + lit + "'!";
                break;
            default:
                pattern = "'" + lit + "' " + a + " {0,lower} " + b + "''";
                break;
        }

        exercise(pattern, arg, false);
    }

    private static void exercise(String pattern, String arg, boolean checkExactSeedOutput) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, Locale.getDefault(), REGISTRY);

            String beforePattern = emf.toPattern();
            String formatted = String.valueOf(emf.format(new Object[] { arg }));
            String afterPattern = emf.toPattern();

            // MessageFormat.format is read-only with respect to the compiled pattern; a correct
            // ExtendedMessageFormat must not mutate toPattern() merely by formatting.
            if (!beforePattern.equals(afterPattern)) {
                throw new RuntimeException(
                        "[oracle:toPattern-stable] metamorphic violation: format mutated pattern input="
                                + pattern + " before=" + beforePattern + " after=" + afterPattern);
            }

            if (checkExactSeedOutput) {
                String expected = "it's a dummy test!";
                if (!expected.equals(formatted)) {
                    throw new RuntimeException(
                            "[oracle:seed-output] metamorphic violation: exact seed formatted unexpectedly input="
                                    + pattern + " expected=" + expected + " actual=" + formatted);
                }
            }

            try {
                ExtendedMessageFormat emf2 =
                        new ExtendedMessageFormat(beforePattern, Locale.getDefault(), REGISTRY);
                String roundTripPattern = emf2.toPattern();
                String formatted2 = String.valueOf(emf2.format(new Object[] { arg }));

                // For a correct implementation, toPattern() is a round-trippable representation of
                // the object's pattern including custom formats; rebuilding from it must preserve
                // both the pattern representation and formatting behavior.
                if (!beforePattern.equals(roundTripPattern) || !formatted.equals(formatted2)) {
                    throw new RuntimeException(
                            "[oracle:roundtrip] metamorphic violation: toPattern round-trip mismatch input="
                                    + pattern + " p1=" + beforePattern + " p2=" + roundTripPattern
                                    + " f1=" + formatted + " f2=" + formatted2);
                }
            } catch (Throwable t) {
                if (isRootCause(t)) {
                    throw propagate(t);
                }
                if (!isCleanRejection(t)) {
                    return;
                }
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throw propagate(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Validation")
                || name.contains("Invalid")
                || name.contains("Malformed")
                || name.contains("Parse");
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
            String cls = e.getClassName();
            String m = e.getMethodName();
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
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == '\'') {
                continue;
            }
            if (c < 0x20) {
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static String sanitizeNonEmpty(String s) {
        String out = sanitize(s);
        return out.length() == 0 ? "x" : out;
    }

    private static Map createRegistry() {
        Map map = new HashMap();
        map.put("lower", new LowerCaseFormatFactory());
        map.put("upper", new UpperCaseFormatFactory());
        return map;
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat(locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new UpperCaseFormat(locale);
        }
    }

    private static final class LowerCaseFormat extends Format {
        private final Locale locale;

        private LowerCaseFormat(Locale locale) {
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = obj == null ? "null" : String.valueOf(obj);
            toAppendTo.append(s.toLowerCase(locale));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }

    private static final class UpperCaseFormat extends Format {
        private final Locale locale;

        private UpperCaseFormat(Locale locale) {
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = obj == null ? "null" : String.valueOf(obj);
            toAppendTo.append(s.toUpperCase(locale));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
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
<details><summary>▸ Prompt (4 message(s), ~8,623 chars, 1 new)</summary>

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
    private static final Map REGISTRY = createRegistry();

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exercise("it''s a {0,lower} 'test'!", "DUMMY", true);

        String a = sanitize(data.consumeAsciiString(24));
        String b = sanitize(data.consumeAsciiString(24));
        String lit = sanitizeNonEmpty(data.consumeAsciiString(12));
        String arg = sanitizeNonEmpty(data.consumeAsciiString(24));
        if (arg.length() == 0) {
            arg = "DUMMY";
        }

        int variant = data.consumeInt(0, 4);
        String pattern;
        switch (variant) {
            case 0:
                pattern = a + "''" + b + " {0,lower}";
                break;
            case 1:
                pattern = a + " {0,lower} ''" + b;
                break;
            case 2:
                pattern = a + " '" + lit + "' {0,lower}";
                break;
            case 3:
                pattern = a + "''" + b + " {0,lower} '" + lit + "'!";
                break;
            default:
                pattern = "'" + lit + "' " + a + " {0,lower} " + b + "''";
                break;
        }

        exercise(pattern, arg, false);
    }

    private static void exercise(String pattern, String arg, boolean checkExactSeedOutput) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, Locale.getDefault(), REGISTRY);

            String beforePattern = emf.toPattern();
            String formatted = String.valueOf(emf.format(new Object[] { arg }));
            String afterPattern = emf.toPattern();

            // MessageFormat.format is read-only with respect to the compiled pattern; a correct
            // ExtendedMessageFormat must not mutate toPattern() merely by formatting.
            if (!beforePattern.equals(afterPattern)) {
                throw new RuntimeException(
                        "[oracle:toPattern-stable] metamorphic violation: format mutated pattern input="
                                + pattern + " before=" + beforePattern + " after=" + afterPattern);
            }

            if (checkExactSeedOutput) {
                String expected = "it's a dummy test!";
                if (!expected.equals(formatted)) {
                    throw new RuntimeException(
                            "[oracle:seed-output] metamorphic violation: exact seed formatted unexpectedly input="
                                    + pattern + " expected=" + expected + " actual=" + formatted);
                }
            }

            try {
                ExtendedMessageFormat emf2 =
                        new ExtendedMessageFormat(beforePattern, Locale.getDefault(), REGISTRY);
                String roundTripPattern = emf2.toPattern();
                String formatted2 = String.valueOf(emf2.format(new Object[] { arg }));

                // For a correct implementation, toPattern() is a round-trippable representation of
                // the object's pattern including custom formats; rebuilding from it must preserve
                // both the pattern representation and formatting behavior.
                if (!beforePattern.equals(roundTripPattern) || !formatted.equals(formatted2)) {
                    throw new RuntimeException(
                            "[oracle:roundtrip] metamorphic violation: toPattern round-trip mismatch input="
                                    + pattern + " p1=" + beforePattern + " p2=" + roundTripPattern
                                    + " f1=" + formatted + " f2=" + formatted2);
                }
            } catch (Throwable t) {
                if (isRootCause(t)) {
                    throw propagate(t);
                }
                if (!isCleanRejection(t)) {
                    return;
                }
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throw propagate(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Validation")
                || name.contains("Invalid")
                || name.contains("Malformed")
                || name.contains("Parse");
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
            String cls = e.getClassName();
            String m = e.getMethodName();
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
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == '\'') {
                continue;
            }
            if (c < 0x20) {
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static String sanitizeNonEmpty(String s) {
        String out = sanitize(s);
        return out.length() == 0 ? "x" : out;
    }

    private static Map createRegistry() {
        Map map = new HashMap();
        map.put("lower", new LowerCaseFormatFactory());
        map.put("upper", new UpperCaseFormatFactory());
        return map;
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat(locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new UpperCaseFormat(locale);
        }
    }

    private static final class LowerCaseFormat extends Format {
        private final Locale locale;

        private LowerCaseFormat(Locale locale) {
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = obj == null ? "null" : String.valueOf(obj);
            toAppendTo.append(s.toLowerCase(locale));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }

    private static final class UpperCaseFormat extends Format {
        private final Locale locale;

        private UpperCaseFormat(Locale locale) {
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = obj == null ? "null" : String.valueOf(obj);
            toAppendTo.append(s.toUpperCase(locale));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~5,131 chars)</summary>

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
    private static final Map REGISTRY = createRegistry();

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Anchor: exact trigger from the failing test. On the buggy version this constructor call
        // reaches ExtendedMessageFormat.applyPattern -> appendQuotedString with pos at a quote and
        // escapingOn=true, and appendQuotedString fails to advance pos before returning, causing the
        // outer loop to spin until OutOfMemoryError.
        new ExtendedMessageFormat("it''s a {0,lower} 'test'!", REGISTRY);

        // Explore the patched condition boundary with related real patterns that begin a quoted
        // segment via doubled quotes and still use a registered custom format so the same parser
        // path is exercised through the public API.
        String prefix = clean(data.consumeAsciiString(16));
        String middle = clean(data.consumeAsciiString(16));
        String suffix = clean(data.consumeAsciiString(16));
        String literal = cleanNonEmpty(data.consumeAsciiString(12));

        int variant = data.consumeInt(0, 5);
        String pattern;
        switch (variant) {
            case 0:
                pattern = prefix + "''" + middle + " {0,lower}";
                break;
            case 1:
                pattern = prefix + "''" + middle + " {0,lower} '" + literal + "'";
                break;
            case 2:
                pattern = "''" + prefix + " {0,lower} " + suffix;
                break;
            case 3:
                pattern = prefix + " {0,lower} ''" + middle;
                break;
            case 4:
                pattern = prefix + "''" + middle + " {0,upper} '" + literal + "' " + suffix;
                break;
            default:
                pattern = prefix + "''" + middle + " {0,lower} 'test'!";
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);

        // Post-condition: formatting must not mutate the object's pattern representation.
        String before = emf.toPattern();
        emf.format(new Object[] { cleanNonEmpty(data.consumeAsciiString(16)) });
        String after = emf.toPattern();
        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: before=" + before + " after=" + after + " input=" + pattern);
        }
    }

    private static Map createRegistry() {
        Map map = new HashMap();
        map.put("lower", new LowerCaseFormatFactory());
        map.put("upper", new UpperCaseFormatFactory());
        return map;
    }

    private static String clean(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 0x20 && c != '{' && c != '}') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String cleanNonEmpty(String s) {
        String out = clean(s);
        return out.length() == 0 ? "x" : out;
    }

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
            String s = String.valueOf(obj);
            toAppendTo.append(s.toLowerCase(locale));
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
            String s = String.valueOf(obj);
            toAppendTo.append(s.toUpperCase(locale));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
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
<details><summary>▸ Prompt (6 message(s), ~5,449 chars, 1 new)</summary>

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
    private static final Map REGISTRY = createRegistry();

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Anchor: exact trigger from the failing test. On the buggy version this constructor call
        // reaches ExtendedMessageFormat.applyPattern -> appendQuotedString with pos at a quote and
        // escapingOn=true, and appendQuotedString fails to advance pos before returning, causing the
        // outer loop to spin until OutOfMemoryError.
        new ExtendedMessageFormat("it''s a {0,lower} 'test'!", REGISTRY);

        // Explore the patched condition boundary with related real patterns that begin a quoted
        // segment via doubled quotes and still use a registered custom format so the same parser
        // path is exercised through the public API.
        String prefix = clean(data.consumeAsciiString(16));
        String middle = clean(data.consumeAsciiString(16));
        String suffix = clean(data.consumeAsciiString(16));
        String literal = cleanNonEmpty(data.consumeAsciiString(12));

        int variant = data.consumeInt(0, 5);
        String pattern;
        switch (variant) {
            case 0:
                pattern = prefix + "''" + middle + " {0,lower}";
                break;
            case 1:
                pattern = prefix + "''" + middle + " {0,lower} '" + literal + "'";
                break;
            case 2:
                pattern = "''" + prefix + " {0,lower} " + suffix;
                break;
            case 3:
                pattern = prefix + " {0,lower} ''" + middle;
                break;
            case 4:
                pattern = prefix + "''" + middle + " {0,upper} '" + literal + "' " + suffix;
                break;
            default:
                pattern = prefix + "''" + middle + " {0,lower} 'test'!";
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);

        // Post-condition: formatting must not mutate the object's pattern representation.
        String before = emf.toPattern();
        emf.format(new Object[] { cleanNonEmpty(data.consumeAsciiString(16)) });
        String after = emf.toPattern();
        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: before=" + before + " after=" + after + " input=" + pattern);
        }
    }

    private static Map createRegistry() {
        Map map = new HashMap();
        map.put("lower", new LowerCaseFormatFactory());
        map.put("upper", new UpperCaseFormatFactory());
        return map;
    }

    private static String clean(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 0x20 && c != '{' && c != '}') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String cleanNonEmpty(String s) {
        String out = clean(s);
        return out.length() == 0 ? "x" : out;
    }

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
            String s = String.valueOf(obj);
            toAppendTo.append(s.toLowerCase(locale));
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
            String s = String.valueOf(obj);
            toAppendTo.append(s.toUpperCase(locale));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~5,910 chars)</summary>

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

        // ANCHOR: exact failing test input. On the buggy version, applyPattern() reaches
        // appendQuotedString() with pos at the first quote of the doubled-quote sequence in "it''s".
        // The buggy code returns without advancing pos, so the outer loop keeps reprocessing the
        // same quote and grows buffers until OutOfMemoryError. The fixed code advances first.
        ExtendedMessageFormat emf = new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);

        String formatted = emf.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(formatted)) {
            throw new RuntimeException("[oracle:seed-output] metamorphic violation: expected=it's a dummy test! actual=" + formatted);
        }

        // POST-CONDITION: toPattern() is the object's externally visible pattern representation.
        // Formatting should not mutate it; a throw-deleting or branch-skipping patch that damages
        // parser bookkeeping can violate this while avoiding the crash.
        String before = emf.toPattern();
        emf.format(new Object[] { "DUMMY" });
        String after = emf.toPattern();
        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: before=" + before + " after=" + after);
        }

        // EXPLORE: build more patterns that exercise the same changed condition: a doubled quote
        // outside a format element while a custom format is present, so parsing goes through the
        // same public entry point and appendQuotedString(..., escapingOn=true).
        String left = clean(data.consumeAsciiString(12));
        String right = clean(data.consumeAsciiString(12));
        String lit = cleanNonEmpty(data.consumeAsciiString(8));
        String arg = cleanNonEmpty(data.consumeAsciiString(12));

        String pattern;
        switch (data.consumeInt(0, 4)) {
            case 0:
                pattern = left + "''" + right + " {0,lower}";
                break;
            case 1:
                pattern = left + "''" + right + " {0,lower} '" + lit + "'";
                break;
            case 2:
                pattern = "''" + left + " {0,upper}";
                break;
            case 3:
                pattern = left + " {0,lower} ''" + right;
                break;
            default:
                pattern = "'" + lit + "' " + left + "''" + right + " {0,lower}";
                break;
        }

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(pattern, registry);
        String p1 = emf2.toPattern();
        String out1 = emf2.format(new Object[] { arg });

        // Round-trip metamorphic check: rebuilding from toPattern() must preserve formatting
        // behavior for the same argument on any correct implementation.
        ExtendedMessageFormat emf3 = new ExtendedMessageFormat(p1, registry);
        String p2 = emf3.toPattern();
        String out2 = emf3.format(new Object[] { arg });
        if (!p1.equals(p2) || !out1.equals(out2)) {
            throw new RuntimeException("[oracle:roundtrip] metamorphic violation: input=" + pattern + " p1=" + p1 + " p2=" + p2 + " out1=" + out1 + " out2=" + out2);
        }
    }

    private static String clean(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 0x20 && c != '{' && c != '}') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String cleanNonEmpty(String s) {
        String out = clean(s);
        return out.length() == 0 ? "x" : out;
    }

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
            toAppendTo.append(String.valueOf(obj).toLowerCase(locale));
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
            toAppendTo.append(String.valueOf(obj).toUpperCase(locale));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
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
<details><summary>▸ Prompt (8 message(s), ~6,348 chars, 1 new)</summary>

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

        // ANCHOR: exact failing test input. On the buggy version, applyPattern() reaches
        // appendQuotedString() with pos at the first quote of the doubled-quote sequence in "it''s".
        // The buggy code returns without advancing pos, so the outer loop keeps reprocessing the
        // same quote and grows buffers until OutOfMemoryError. The fixed code advances first.
        ExtendedMessageFormat emf = new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);

        String formatted = emf.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(formatted)) {
            throw new RuntimeException("[oracle:seed-output] metamorphic violation: expected=it's a dummy test! actual=" + formatted);
        }

        // POST-CONDITION: toPattern() is the object's externally visible pattern representation.
        // Formatting should not mutate it; a throw-deleting or branch-skipping patch that damages
        // parser bookkeeping can violate this while avoiding the crash.
        String before = emf.toPattern();
        emf.format(new Object[] { "DUMMY" });
        String after = emf.toPattern();
        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: before=" + before + " after=" + after);
        }

        // EXPLORE: build more patterns that exercise the same changed condition: a doubled quote
        // outside a format element while a custom format is present, so parsing goes through the
        // same public entry point and appendQuotedString(..., escapingOn=true).
        String left = clean(data.consumeAsciiString(12));
        String right = clean(data.consumeAsciiString(12));
        String lit = cleanNonEmpty(data.consumeAsciiString(8));
        String arg = cleanNonEmpty(data.consumeAsciiString(12));

        String pattern;
        switch (data.consumeInt(0, 4)) {
            case 0:
                pattern = left + "''" + right + " {0,lower}";
                break;
            case 1:
                pattern = left + "''" + right + " {0,lower} '" + lit + "'";
                break;
            case 2:
                pattern = "''" + left + " {0,upper}";
                break;
            case 3:
                pattern = left + " {0,lower} ''" + right;
                break;
            default:
                pattern = "'" + lit + "' " + left + "''" + right + " {0,lower}";
                break;
        }

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(pattern, registry);
        String p1 = emf2.toPattern();
        String out1 = emf2.format(new Object[] { arg });

        // Round-trip metamorphic check: rebuilding from toPattern() must preserve formatting
        // behavior for the same argument on any correct implementation.
        ExtendedMessageFormat emf3 = new ExtendedMessageFormat(p1, registry);
        String p2 = emf3.toPattern();
        String out2 = emf3.format(new Object[] { arg });
        if (!p1.equals(p2) || !out1.equals(out2)) {
            throw new RuntimeException("[oracle:roundtrip] metamorphic violation: input=" + pattern + " p1=" + p1 + " p2=" + p2 + " out1=" + out1 + " out2=" + out2);
        }
    }

    private static String clean(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 0x20 && c != '{' && c != '}') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String cleanNonEmpty(String s) {
        String out = clean(s);
        return out.length() == 0 ? "x" : out;
    }

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
            toAppendTo.append(String.valueOf(obj).toLowerCase(locale));
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
            toAppendTo.append(String.valueOf(obj).toUpperCase(locale));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~5,638 chars)</summary>

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
        HashMap registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        // Exact anchor from the failing test. This is the real public entry point and the same setup.
        String anchor = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat emf = new ExtendedMessageFormat(anchor, registry);

        // Observable contract from the test: the parsed pattern must format identically to the test.
        String out = emf.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(out)) {
            throw new RuntimeException("[oracle:seed-output] metamorphic violation: actual=" + out);
        }

        // Formatting should be read-only with respect to the externally visible pattern.
        String pBefore = emf.toPattern();
        emf.format(new Object[] { "DUMMY" });
        String pAfter = emf.toPattern();
        if (!pBefore.equals(pAfter)) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: before=" + pBefore + " after=" + pAfter);
        }

        // Explore the changed condition: inputs containing doubled quotes outside format elements
        // must still parse and preserve formatting behavior through the same constructor path.
        String prefix = clean(data.consumeAsciiString(16));
        String middle = clean(data.consumeAsciiString(16));
        String suffix = clean(data.consumeAsciiString(16));
        String lit = cleanNonEmpty(data.consumeAsciiString(12));
        String arg = cleanNonEmpty(data.consumeAsciiString(16));

        String pattern;
        switch (data.consumeInt(0, 5)) {
            case 0:
                pattern = prefix + "''" + middle + " {0,lower}";
                break;
            case 1:
                pattern = prefix + "''" + middle + " {0,lower} '" + lit + "'";
                break;
            case 2:
                pattern = "''" + prefix + " {0,lower} " + suffix;
                break;
            case 3:
                pattern = prefix + " {0,lower} ''" + middle;
                break;
            case 4:
                pattern = "'" + lit + "' " + prefix + "''" + middle + " {0,upper}";
                break;
            default:
                pattern = prefix + "''" + middle + " {0,lower} 'test'!";
                break;
        }

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(pattern, registry);
        String patternRoundTrip = emf2.toPattern();
        String out1 = emf2.format(new Object[] { arg });

        // Round-trip guarantee: rebuilding from toPattern() should preserve behavior.
        ExtendedMessageFormat emf3 = new ExtendedMessageFormat(patternRoundTrip, registry);
        String out2 = emf3.format(new Object[] { arg });
        if (!out1.equals(out2) || !patternRoundTrip.equals(emf3.toPattern())) {
            throw new RuntimeException("[oracle:roundtrip] metamorphic violation: input=" + pattern + " p1=" + patternRoundTrip + " p2=" + emf3.toPattern() + " out1=" + out1 + " out2=" + out2);
        }
    }

    private static String clean(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 0x20 && c != '{' && c != '}') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String cleanNonEmpty(String s) {
        String r = clean(s);
        return r.length() == 0 ? "x" : r;
    }

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
            toAppendTo.append(String.valueOf(obj).toLowerCase(locale));
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
            toAppendTo.append(String.valueOf(obj).toUpperCase(locale));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
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
<details><summary>▸ Prompt (2 message(s), ~32,027 chars, 1 new)</summary>

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
@@ -418,7 +418,8 @@ public class ExtendedMessageFormat extends MessageFormat {
             StringBuffer appendTo, boolean escapingOn) {
         int start = pos.getIndex();
         char[] c = pattern.toCharArray();
+        next(pos);
         if (escapingOn && c[start] == QUOTE) {
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

GUARDRAIL (all options): prefer a property that holds for EVERY correct implementation, and state the documented guarantee behind each check in a comment (an automated soundness reviewer screens every fired assertion and reads it). Do NOT retreat to vacuous checks (finiteness, non-null, no-throw) to stay safe — a vacuous check guarantees a missed bug, which nothing downstream can recover; the reviewer backstops a justified-but-borderline check, though it is not infallible.
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
<details open><summary>▸ Output (~10,467 chars)</summary>

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
    private static final Map REGISTRY = makeRegistry();

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        String pattern = "it''s a {0,lower} 'test'!";
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);

            String before = emf.toPattern();
            String formatted = emf.format(new Object[] { "DUMMY" });
            String after = emf.toPattern();

            if (!"it's a dummy test!".equals(formatted)) {
                throw new RuntimeException("[oracle:anchor-output] metamorphic violation: exact regression input formatted unexpectedly input="
                        + pattern + " output=" + formatted);
            }

            // Contract/invariant: formatting is read-only with respect to the parsed pattern;
            // toPattern is observable object state and should not change after format().
            if (before == null ? after != null : !before.equals(after)) {
                throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: format() changed toPattern input="
                        + pattern + " before=" + before + " after=" + after);
            }

            // Contract/invariant: toPattern() is the object's canonical pattern representation.
            // Reconstructing an equivalent object from that pattern and the same registry must
            // preserve both the canonical pattern and formatting semantics.
            ExtendedMessageFormat roundTrip = new ExtendedMessageFormat(before, REGISTRY);
            String roundTripPattern = roundTrip.toPattern();
            String roundTripFormatted = roundTrip.format(new Object[] { "DUMMY" });

            if (before == null ? roundTripPattern != null : !before.equals(roundTripPattern)) {
                throw new RuntimeException("[oracle:roundtrip-pattern] metamorphic violation: toPattern round-trip changed canonical pattern input="
                        + pattern + " p1=" + before + " p2=" + roundTripPattern);
            }
            if (formatted == null ? roundTripFormatted != null : !formatted.equals(roundTripFormatted)) {
                throw new RuntimeException("[oracle:roundtrip-format] metamorphic violation: equivalent patterns formatted differently input="
                        + pattern + " lhs=" + formatted + " rhs=" + roundTripFormatted);
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                sneakyThrow(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            throwAsOracle("anchor-exception", "valid anchor input threw unexpectedly", pattern, t);
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        String left = sanitize(data.consumeAsciiString(16));
        String middle = sanitize(data.consumeAsciiString(16));
        String quoted = sanitize(data.consumeAsciiString(16));
        String right = sanitize(data.consumeAsciiString(16));
        String arg = sanitizeArg(data.consumeString(24));
        int style = data.consumeInt(0, 2);

        String fmt;
        switch (style) {
            case 0:
                fmt = "lower";
                break;
            case 1:
                fmt = "upper";
                break;
            default:
                fmt = "number";
                break;
        }

        if (arg.length() == 0) {
            arg = "Dummy";
        }
        if (quoted.length() == 0) {
            quoted = "q";
        }

        // Root-cause property: a pattern processed through ExtendedMessageFormat's custom-registry
        // parsing path that contains an escaped quote '' and also a format element. This exercises
        // appendQuotedString via the real public entry point and varies quote placement/content.
        String pattern = left + "''" + middle + " {0," + fmt + "} '" + quoted + "'" + right;

        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);
            String before = emf.toPattern();
            String out1 = emf.format(new Object[] { arg });
            String after = emf.toPattern();

            if (before == null ? after != null : !before.equals(after)) {
                throw new RuntimeException("[oracle:explore-topattern-stable] metamorphic violation: format() changed toPattern input="
                        + pattern + " before=" + before + " after=" + after);
            }

            try {
                ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, REGISTRY);
                String out2 = emf2.format(new Object[] { arg });
                String before2 = emf2.toPattern();

                if (before == null ? before2 != null : !before.equals(before2)) {
                    throw new RuntimeException("[oracle:explore-roundtrip-pattern] metamorphic violation: toPattern round-trip changed canonical pattern input="
                            + pattern + " p1=" + before + " p2=" + before2);
                }
                if (out1 == null ? out2 != null : !out1.equals(out2)) {
                    throw new RuntimeException("[oracle:explore-roundtrip-format] metamorphic violation: equivalent patterns formatted differently input="
                            + pattern + " lhs=" + out1 + " rhs=" + out2);
                }
            } catch (Throwable t) {
                if (isRootCause(t)) {
                    sneakyThrow(t);
                }
                if (isCleanRejection(t)) {
                    return;
                }
                return;
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                sneakyThrow(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == '\'') {
                b.append('x');
            } else if (c < 32 || c > 126) {
                b.append('x');
            } else {
                b.append(c);
            }
        }
        if (b.length() == 0) {
            b.append('x');
        }
        return b.toString();
    }

    private static String sanitizeArg(String s) {
        if (s == null || s.length() == 0) {
            return "Dummy";
        }
        return s;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
                || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            String cls = e.getClassName();
            String method = e.getMethodName();
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cls)
                    && ("appendQuotedString".equals(method)
                    || "next".equals(method)
                    || "append".equals(method)
                    || "startsWith".equals(method))) {
                return true;
            }
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                    && ("append".equals(method)
                    || "startsWith".equals(method))) {
                return true;
            }
        }
        return false;
    }

    private static Map makeRegistry() {
        HashMap map = new HashMap();
        map.put("lower", new LowerCaseFormatFactory());
        map.put("upper", new UpperCaseFormatFactory());
        return map;
    }

    private static void throwAsOracle(String id, String msg, String input, Throwable t) {
        RuntimeException r = new RuntimeException("[oracle:" + id + "] metamorphic violation: "
                + msg + " input=" + input + " threw=" + t, t);
        throw r;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}

class LowerCaseFormatFactory implements FormatFactory {
    public Format getFormat(String name, String arguments, Locale locale) {
        return new LowerCaseFormat(locale);
    }
}

class UpperCaseFormatFactory implements FormatFactory {
    public Format getFormat(String name, String arguments, Locale locale) {
        return new UpperCaseFormat(locale);
    }
}

class LowerCaseFormat extends Format {
    private final Locale locale;

    LowerCaseFormat(Locale locale) {
        this.locale = locale == null ? Locale.getDefault() : locale;
    }

    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        String s = String.valueOf(obj);
        toAppendTo.append(s.toLowerCase(locale));
        return toAppendTo;
    }

    public Object parseObject(String source, ParsePosition pos) {
        if (source == null) {
            return null;
        }
        pos.setIndex(source.length());
        return source.toLowerCase(locale);
    }
}

class UpperCaseFormat extends Format {
    private final Locale locale;

    UpperCaseFormat(Locale locale) {
        this.locale = locale == null ? Locale.getDefault() : locale;
    }

    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        String s = String.valueOf(obj);
        toAppendTo.append(s.toUpperCase(locale));
        return toAppendTo;
    }

    public Object parseObject(String source, ParsePosition pos) {
        if (source == null) {
            return null;
        }
        pos.setIndex(source.length());
        return source.toUpperCase(locale);
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
<details><summary>▸ Prompt (4 message(s), ~10,665 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [19] — not reprinted*
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
    private static final Map REGISTRY = makeRegistry();

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        String pattern = "it''s a {0,lower} 'test'!";
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);

            String before = emf.toPattern();
            String formatted = emf.format(new Object[] { "DUMMY" });
            String after = emf.toPattern();

            if (!"it's a dummy test!".equals(formatted)) {
                throw new RuntimeException("[oracle:anchor-output] metamorphic violation: exact regression input formatted unexpectedly input="
                        + pattern + " output=" + formatted);
            }

            // Contract/invariant: formatting is read-only with respect to the parsed pattern;
            // toPattern is observable object state and should not change after format().
            if (before == null ? after != null : !before.equals(after)) {
                throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: format() changed toPattern input="
                        + pattern + " before=" + before + " after=" + after);
            }

            // Contract/invariant: toPattern() is the object's canonical pattern representation.
            // Reconstructing an equivalent object from that pattern and the same registry must
            // preserve both the canonical pattern and formatting semantics.
            ExtendedMessageFormat roundTrip = new ExtendedMessageFormat(before, REGISTRY);
            String roundTripPattern = roundTrip.toPattern();
            String roundTripFormatted = roundTrip.format(new Object[] { "DUMMY" });

            if (before == null ? roundTripPattern != null : !before.equals(roundTripPattern)) {
                throw new RuntimeException("[oracle:roundtrip-pattern] metamorphic violation: toPattern round-trip changed canonical pattern input="
                        + pattern + " p1=" + before + " p2=" + roundTripPattern);
            }
            if (formatted == null ? roundTripFormatted != null : !formatted.equals(roundTripFormatted)) {
                throw new RuntimeException("[oracle:roundtrip-format] metamorphic violation: equivalent patterns formatted differently input="
                        + pattern + " lhs=" + formatted + " rhs=" + roundTripFormatted);
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                sneakyThrow(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            throwAsOracle("anchor-exception", "valid anchor input threw unexpectedly", pattern, t);
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        String left = sanitize(data.consumeAsciiString(16));
        String middle = sanitize(data.consumeAsciiString(16));
        String quoted = sanitize(data.consumeAsciiString(16));
        String right = sanitize(data.consumeAsciiString(16));
        String arg = sanitizeArg(data.consumeString(24));
        int style = data.consumeInt(0, 2);

        String fmt;
        switch (style) {
            case 0:
                fmt = "lower";
                break;
            case 1:
                fmt = "upper";
                break;
            default:
                fmt = "number";
                break;
        }

        if (arg.length() == 0) {
            arg = "Dummy";
        }
        if (quoted.length() == 0) {
            quoted = "q";
        }

        // Root-cause property: a pattern processed through ExtendedMessageFormat's custom-registry
        // parsing path that contains an escaped quote '' and also a format element. This exercises
        // appendQuotedString via the real public entry point and varies quote placement/content.
        String pattern = left + "''" + middle + " {0," + fmt + "} '" + quoted + "'" + right;

        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);
            String before = emf.toPattern();
            String out1 = emf.format(new Object[] { arg });
            String after = emf.toPattern();

            if (before == null ? after != null : !before.equals(after)) {
                throw new RuntimeException("[oracle:explore-topattern-stable] metamorphic violation: format() changed toPattern input="
                        + pattern + " before=" + before + " after=" + after);
            }

            try {
                ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, REGISTRY);
                String out2 = emf2.format(new Object[] { arg });
                String before2 = emf2.toPattern();

                if (before == null ? before2 != null : !before.equals(before2)) {
                    throw new RuntimeException("[oracle:explore-roundtrip-pattern] metamorphic violation: toPattern round-trip changed canonical pattern input="
                            + pattern + " p1=" + before + " p2=" + before2);
                }
                if (out1 == null ? out2 != null : !out1.equals(out2)) {
                    throw new RuntimeException("[oracle:explore-roundtrip-format] metamorphic violation: equivalent patterns formatted differently input="
                            + pattern + " lhs=" + out1 + " rhs=" + out2);
                }
            } catch (Throwable t) {
                if (isRootCause(t)) {
                    sneakyThrow(t);
                }
                if (isCleanRejection(t)) {
                    return;
                }
                return;
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                sneakyThrow(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == '\'') {
                b.append('x');
            } else if (c < 32 || c > 126) {
                b.append('x');
            } else {
                b.append(c);
            }
        }
        if (b.length() == 0) {
            b.append('x');
        }
        return b.toString();
    }

    private static String sanitizeArg(String s) {
        if (s == null || s.length() == 0) {
            return "Dummy";
        }
        return s;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
                || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            String cls = e.getClassName();
            String method = e.getMethodName();
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cls)
                    && ("appendQuotedString".equals(method)
                    || "next".equals(method)
                    || "append".equals(method)
                    || "startsWith".equals(method))) {
                return true;
            }
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                    && ("append".equals(method)
                    || "startsWith".equals(method))) {
                return true;
            }
        }
        return false;
    }

    private static Map makeRegistry() {
        HashMap map = new HashMap();
        map.put("lower", new LowerCaseFormatFactory());
        map.put("upper", new UpperCaseFormatFactory());
        return map;
    }

    private static void throwAsOracle(String id, String msg, String input, Throwable t) {
        RuntimeException r = new RuntimeException("[oracle:" + id + "] metamorphic violation: "
                + msg + " input=" + input + " threw=" + t, t);
        throw r;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}

class LowerCaseFormatFactory implements FormatFactory {
    public Format getFormat(String name, String arguments, Locale locale) {
        return new LowerCaseFormat(locale);
    }
}

class UpperCaseFormatFactory implements FormatFactory {
    public Format getFormat(String name, String arguments, Locale locale) {
        return new UpperCaseFormat(locale);
    }
}

class LowerCaseFormat extends Format {
    private final Locale locale;

    LowerCaseFormat(Locale locale) {
        this.locale = locale == null ? Locale.getDefault() : locale;
    }

    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        String s = String.valueOf(obj);
        toAppendTo.append(s.toLowerCase(locale));
        return toAppendTo;
    }

    public Object parseObject(String source, ParsePosition pos) {
        if (source == null) {
            return null;
        }
        pos.setIndex(source.length());
        return source.toLowerCase(locale);
    }
}

class UpperCaseFormat extends Format {
    private final Locale locale;

    UpperCaseFormat(Locale locale) {
        this.locale = locale == null ? Locale.getDefault() : locale;
    }

    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        String s = String.valueOf(obj);
        toAppendTo.append(s.toUpperCase(locale));
        return toAppendTo;
    }

    public Object parseObject(String source, ParsePosition pos) {
        if (source == null) {
            return null;
        }
        pos.setIndex(source.length());
        return source.toUpperCase(locale);
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~5,135 chars)</summary>

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
    private static final Map REGISTRY = makeRegistry();
    private static final Object[] ANCHOR_ARGS = new Object[] { "DUMMY" };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, REGISTRY);
        String anchorOut = anchor.format(ANCHOR_ARGS);
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: input=" + anchorPattern + " output=" + anchorOut);
        }

        String prefix = clean(data.consumeAsciiString(12));
        String infix = clean(data.consumeAsciiString(12));
        String quoted = clean(data.consumeAsciiString(12));
        String suffix = clean(data.consumeAsciiString(12));
        String arg = data.consumeString(16);
        if (arg.length() == 0) {
            arg = "Dummy";
        }
        if (quoted.length() == 0) {
            quoted = "test";
        }

        String formatName;
        switch (data.consumeInt(0, 1)) {
            case 0:
                formatName = "lower";
                break;
            default:
                formatName = "upper";
                break;
        }

        String pattern = prefix + "''" + infix + " {0," + formatName + "} '" + quoted + "'" + suffix;
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);

        String before = emf.toPattern();
        String out1 = emf.format(new Object[] { arg });
        String after = emf.toPattern();
        if (before == null ? after != null : !before.equals(after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: input=" + pattern + " before=" + before + " after=" + after);
        }

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, REGISTRY);
        String out2 = emf2.format(new Object[] { arg });
        String before2 = emf2.toPattern();
        if (before == null ? before2 != null : !before.equals(before2)) {
            throw new RuntimeException("[oracle:roundtrip-pattern] metamorphic violation: input=" + pattern + " p1=" + before + " p2=" + before2);
        }
        if (out1 == null ? out2 != null : !out1.equals(out2)) {
            throw new RuntimeException("[oracle:roundtrip-format] metamorphic violation: input=" + pattern + " lhs=" + out1 + " rhs=" + out2);
        }
    }

    private static String clean(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == '\'') {
                b.append('x');
            } else if (c < 32 || c > 126) {
                b.append('x');
            } else {
                b.append(c);
            }
        }
        if (b.length() == 0) {
            b.append('x');
        }
        return b.toString();
    }

    private static Map makeRegistry() {
        HashMap map = new HashMap();
        map.put("lower", new LowerCaseFormatFactory());
        map.put("upper", new UpperCaseFormatFactory());
        return map;
    }
}

class LowerCaseFormatFactory implements FormatFactory {
    public Format getFormat(String name, String arguments, Locale locale) {
        return new LowerCaseFormat(locale);
    }
}

class UpperCaseFormatFactory implements FormatFactory {
    public Format getFormat(String name, String arguments, Locale locale) {
        return new UpperCaseFormat(locale);
    }
}

class LowerCaseFormat extends Format {
    private final Locale locale;

    LowerCaseFormat(Locale locale) {
        this.locale = locale == null ? Locale.getDefault() : locale;
    }

    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        toAppendTo.append(String.valueOf(obj).toLowerCase(locale));
        return toAppendTo;
    }

    public Object parseObject(String source, ParsePosition pos) {
        pos.setIndex(source == null ? 0 : source.length());
        return source == null ? null : source.toLowerCase(locale);
    }
}

class UpperCaseFormat extends Format {
    private final Locale locale;

    UpperCaseFormat(Locale locale) {
        this.locale = locale == null ? Locale.getDefault() : locale;
    }

    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        toAppendTo.append(String.valueOf(obj).toUpperCase(locale));
        return toAppendTo;
    }

    public Object parseObject(String source, ParsePosition pos) {
        pos.setIndex(source == null ? 0 : source.length());
        return source == null ? null : source.toUpperCase(locale);
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
<details><summary>▸ Prompt (6 message(s), ~5,453 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [19] — not reprinted*
- *[assistant] message: identical to step [21] — not reprinted*
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
    private static final Map REGISTRY = makeRegistry();
    private static final Object[] ANCHOR_ARGS = new Object[] { "DUMMY" };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, REGISTRY);
        String anchorOut = anchor.format(ANCHOR_ARGS);
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: input=" + anchorPattern + " output=" + anchorOut);
        }

        String prefix = clean(data.consumeAsciiString(12));
        String infix = clean(data.consumeAsciiString(12));
        String quoted = clean(data.consumeAsciiString(12));
        String suffix = clean(data.consumeAsciiString(12));
        String arg = data.consumeString(16);
        if (arg.length() == 0) {
            arg = "Dummy";
        }
        if (quoted.length() == 0) {
            quoted = "test";
        }

        String formatName;
        switch (data.consumeInt(0, 1)) {
            case 0:
                formatName = "lower";
                break;
            default:
                formatName = "upper";
                break;
        }

        String pattern = prefix + "''" + infix + " {0," + formatName + "} '" + quoted + "'" + suffix;
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);

        String before = emf.toPattern();
        String out1 = emf.format(new Object[] { arg });
        String after = emf.toPattern();
        if (before == null ? after != null : !before.equals(after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: input=" + pattern + " before=" + before + " after=" + after);
        }

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, REGISTRY);
        String out2 = emf2.format(new Object[] { arg });
        String before2 = emf2.toPattern();
        if (before == null ? before2 != null : !before.equals(before2)) {
            throw new RuntimeException("[oracle:roundtrip-pattern] metamorphic violation: input=" + pattern + " p1=" + before + " p2=" + before2);
        }
        if (out1 == null ? out2 != null : !out1.equals(out2)) {
            throw new RuntimeException("[oracle:roundtrip-format] metamorphic violation: input=" + pattern + " lhs=" + out1 + " rhs=" + out2);
        }
    }

    private static String clean(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == '\'') {
                b.append('x');
            } else if (c < 32 || c > 126) {
                b.append('x');
            } else {
                b.append(c);
            }
        }
        if (b.length() == 0) {
            b.append('x');
        }
        return b.toString();
    }

    private static Map makeRegistry() {
        HashMap map = new HashMap();
        map.put("lower", new LowerCaseFormatFactory());
        map.put("upper", new UpperCaseFormatFactory());
        return map;
    }
}

class LowerCaseFormatFactory implements FormatFactory {
    public Format getFormat(String name, String arguments, Locale locale) {
        return new LowerCaseFormat(locale);
    }
}

class UpperCaseFormatFactory implements FormatFactory {
    public Format getFormat(String name, String arguments, Locale locale) {
        return new UpperCaseFormat(locale);
    }
}

class LowerCaseFormat extends Format {
    private final Locale locale;

    LowerCaseFormat(Locale locale) {
        this.locale = locale == null ? Locale.getDefault() : locale;
    }

    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        toAppendTo.append(String.valueOf(obj).toLowerCase(locale));
        return toAppendTo;
    }

    public Object parseObject(String source, ParsePosition pos) {
        pos.setIndex(source == null ? 0 : source.length());
        return source == null ? null : source.toLowerCase(locale);
    }
}

class UpperCaseFormat extends Format {
    private final Locale locale;

    UpperCaseFormat(Locale locale) {
        this.locale = locale == null ? Locale.getDefault() : locale;
    }

    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        toAppendTo.append(String.valueOf(obj).toUpperCase(locale));
        return toAppendTo;
    }

    public Object parseObject(String source, ParsePosition pos) {
        pos.setIndex(source == null ? 0 : source.length());
        return source == null ? null : source.toUpperCase(locale);
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~5,398 chars)</summary>

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

        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, Locale.getDefault(), registry);
        String anchorFormatted = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorFormatted)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: input=" + anchorPattern + " output=" + anchorFormatted);
        }
        String anchorBefore = anchor.toPattern();
        String anchorAfter = anchor.toPattern();
        if (anchorBefore == null ? anchorAfter != null : !anchorBefore.equals(anchorAfter)) {
            throw new RuntimeException("[oracle:anchor-topattern] metamorphic violation: before=" + anchorBefore + " after=" + anchorAfter);
        }

        String a = safeLiteral(data.consumeAsciiString(8));
        String b = safeLiteral(data.consumeAsciiString(8));
        String c = safeLiteral(data.consumeAsciiString(8));
        String d = safeLiteral(data.consumeAsciiString(8));
        String arg = data.consumeString(12);
        if (arg.length() == 0) {
            arg = "Dummy";
        }
        String quoted = safeLiteral(data.consumeAsciiString(8));
        if (quoted.length() == 0) {
            quoted = "q";
        }

        String formatName = data.consumeBoolean() ? "lower" : "upper";

        String pattern = a + "''" + b + " {0," + formatName + "} '" + quoted + "'" + c + "''" + d;
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, Locale.getDefault(), registry);

        String before = emf.toPattern();
        String out1 = emf.format(new Object[] { arg });
        String after = emf.toPattern();
        if (before == null ? after != null : !before.equals(after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: input=" + pattern + " before=" + before + " after=" + after);
        }

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, Locale.getDefault(), registry);
        String out2 = emf2.format(new Object[] { arg });
        String before2 = emf2.toPattern();

        if (before == null ? before2 != null : !before.equals(before2)) {
            throw new RuntimeException("[oracle:roundtrip-pattern] metamorphic violation: input=" + pattern + " lhs=" + before + " rhs=" + before2);
        }
        if (out1 == null ? out2 != null : !out1.equals(out2)) {
            throw new RuntimeException("[oracle:roundtrip-format] metamorphic violation: input=" + pattern + " lhs=" + out1 + " rhs=" + out2);
        }
    }

    private static String safeLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'') {
                sb.append('x');
            } else if (ch < 32 || ch > 126) {
                sb.append('x');
            } else {
                sb.append(ch);
            }
        }
        if (sb.length() == 0) {
            sb.append('x');
        }
        return sb.toString();
    }
}

class LowerCaseFormatFactory implements FormatFactory {
    public Format getFormat(String name, String arguments, Locale locale) {
        return new LowerCaseFormat(locale == null ? Locale.getDefault() : locale);
    }
}

class UpperCaseFormatFactory implements FormatFactory {
    public Format getFormat(String name, String arguments, Locale locale) {
        return new UpperCaseFormat(locale == null ? Locale.getDefault() : locale);
    }
}

class LowerCaseFormat extends Format {
    private final Locale locale;

    LowerCaseFormat(Locale locale) {
        this.locale = locale;
    }

    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        toAppendTo.append(String.valueOf(obj).toLowerCase(locale));
        return toAppendTo;
    }

    public Object parseObject(String source, ParsePosition pos) {
        if (source == null) {
            pos.setIndex(0);
            return null;
        }
        pos.setIndex(source.length());
        return source.toLowerCase(locale);
    }
}

class UpperCaseFormat extends Format {
    private final Locale locale;

    UpperCaseFormat(Locale locale) {
        this.locale = locale;
    }

    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        toAppendTo.append(String.valueOf(obj).toUpperCase(locale));
        return toAppendTo;
    }

    public Object parseObject(String source, ParsePosition pos) {
        if (source == null) {
            pos.setIndex(0);
            return null;
        }
        pos.setIndex(source.length());
        return source.toUpperCase(locale);
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
<details><summary>▸ Prompt (8 message(s), ~5,836 chars, 1 new)</summary>

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

        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, Locale.getDefault(), registry);
        String anchorFormatted = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorFormatted)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: input=" + anchorPattern + " output=" + anchorFormatted);
        }
        String anchorBefore = anchor.toPattern();
        String anchorAfter = anchor.toPattern();
        if (anchorBefore == null ? anchorAfter != null : !anchorBefore.equals(anchorAfter)) {
            throw new RuntimeException("[oracle:anchor-topattern] metamorphic violation: before=" + anchorBefore + " after=" + anchorAfter);
        }

        String a = safeLiteral(data.consumeAsciiString(8));
        String b = safeLiteral(data.consumeAsciiString(8));
        String c = safeLiteral(data.consumeAsciiString(8));
        String d = safeLiteral(data.consumeAsciiString(8));
        String arg = data.consumeString(12);
        if (arg.length() == 0) {
            arg = "Dummy";
        }
        String quoted = safeLiteral(data.consumeAsciiString(8));
        if (quoted.length() == 0) {
            quoted = "q";
        }

        String formatName = data.consumeBoolean() ? "lower" : "upper";

        String pattern = a + "''" + b + " {0," + formatName + "} '" + quoted + "'" + c + "''" + d;
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, Locale.getDefault(), registry);

        String before = emf.toPattern();
        String out1 = emf.format(new Object[] { arg });
        String after = emf.toPattern();
        if (before == null ? after != null : !before.equals(after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: input=" + pattern + " before=" + before + " after=" + after);
        }

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, Locale.getDefault(), registry);
        String out2 = emf2.format(new Object[] { arg });
        String before2 = emf2.toPattern();

        if (before == null ? before2 != null : !before.equals(before2)) {
            throw new RuntimeException("[oracle:roundtrip-pattern] metamorphic violation: input=" + pattern + " lhs=" + before + " rhs=" + before2);
        }
        if (out1 == null ? out2 != null : !out1.equals(out2)) {
            throw new RuntimeException("[oracle:roundtrip-format] metamorphic violation: input=" + pattern + " lhs=" + out1 + " rhs=" + out2);
        }
    }

    private static String safeLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'') {
                sb.append('x');
            } else if (ch < 32 || ch > 126) {
                sb.append('x');
            } else {
                sb.append(ch);
            }
        }
        if (sb.length() == 0) {
            sb.append('x');
        }
        return sb.toString();
    }
}

class LowerCaseFormatFactory implements FormatFactory {
    public Format getFormat(String name, String arguments, Locale locale) {
        return new LowerCaseFormat(locale == null ? Locale.getDefault() : locale);
    }
}

class UpperCaseFormatFactory implements FormatFactory {
    public Format getFormat(String name, String arguments, Locale locale) {
        return new UpperCaseFormat(locale == null ? Locale.getDefault() : locale);
    }
}

class LowerCaseFormat extends Format {
    private final Locale locale;

    LowerCaseFormat(Locale locale) {
        this.locale = locale;
    }

    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        toAppendTo.append(String.valueOf(obj).toLowerCase(locale));
        return toAppendTo;
    }

    public Object parseObject(String source, ParsePosition pos) {
        if (source == null) {
            pos.setIndex(0);
            return null;
        }
        pos.setIndex(source.length());
        return source.toLowerCase(locale);
    }
}

class UpperCaseFormat extends Format {
    private final Locale locale;

    UpperCaseFormat(Locale locale) {
        this.locale = locale;
    }

    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        toAppendTo.append(String.valueOf(obj).toUpperCase(locale));
        return toAppendTo;
    }

    public Object parseObject(String source, ParsePosition pos) {
        if (source == null) {
            pos.setIndex(0);
            return null;
        }
        pos.setIndex(source.length());
        return source.toUpperCase(locale);
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~5,944 chars)</summary>

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
    private static final Map REGISTRY = makeRegistry();

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, REGISTRY);

        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: pattern=" + anchorPattern + " output=" + anchorOut);
        }

        String anchorBefore = anchor.toPattern();
        String anchorAfter = anchor.toPattern();
        if (anchorBefore == null ? anchorAfter != null : !anchorBefore.equals(anchorAfter)) {
            throw new RuntimeException("[oracle:anchor-topattern] metamorphic violation: before=" + anchorBefore + " after=" + anchorAfter);
        }

        String left = literal(data.consumeAsciiString(8));
        String mid = literal(data.consumeAsciiString(8));
        String quoted = literal(data.consumeAsciiString(8));
        String right = literal(data.consumeAsciiString(8));
        String arg = data.consumeString(16);
        if (arg.length() == 0) {
            arg = "Dummy";
        }
        if (quoted.length() == 0) {
            quoted = "test";
        }

        String formatName = data.consumeBoolean() ? "lower" : "upper";

        // Bug-triggering property from the diff:
        // applyPattern() enters appendQuotedString() with pos on a QUOTE and escapingOn=true.
        // In the buggy code pos is not advanced before checking c[start] == QUOTE, so the
        // method returns without consuming input and the caller loops forever, appending until OOM.
        String pattern = left + "''" + mid + " {0," + formatName + "} '" + quoted + "'" + right;
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);

        String before = emf.toPattern();
        String out1 = emf.format(new Object[] { arg });
        String after = emf.toPattern();
        if (before == null ? after != null : !before.equals(after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: pattern=" + pattern + " before=" + before + " after=" + after);
        }

        // Contract-based round-trip check: toPattern() is the canonical pattern. Rebuilding
        // an ExtendedMessageFormat from that pattern with the same registry must preserve
        // formatting behavior for the same arguments.
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, REGISTRY);
        String out2 = emf2.format(new Object[] { arg });
        String before2 = emf2.toPattern();
        if (before == null ? before2 != null : !before.equals(before2)) {
            throw new RuntimeException("[oracle:roundtrip-pattern] metamorphic violation: pattern=" + pattern + " lhs=" + before + " rhs=" + before2);
        }
        if (out1 == null ? out2 != null : !out1.equals(out2)) {
            throw new RuntimeException("[oracle:roundtrip-format] metamorphic violation: pattern=" + pattern + " lhs=" + out1 + " rhs=" + out2);
        }
    }

    private static String literal(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'') {
                sb.append('x');
            } else if (ch < 32 || ch > 126) {
                sb.append('x');
            } else {
                sb.append(ch);
            }
        }
        if (sb.length() == 0) {
            sb.append('x');
        }
        return sb.toString();
    }

    private static Map makeRegistry() {
        HashMap map = new HashMap();
        map.put("lower", new LowerCaseFormatFactory());
        map.put("upper", new UpperCaseFormatFactory());
        return map;
    }
}

class LowerCaseFormatFactory implements FormatFactory {
    public Format getFormat(String name, String arguments, Locale locale) {
        return new LowerCaseFormat(locale == null ? Locale.getDefault() : locale);
    }
}

class UpperCaseFormatFactory implements FormatFactory {
    public Format getFormat(String name, String arguments, Locale locale) {
        return new UpperCaseFormat(locale == null ? Locale.getDefault() : locale);
    }
}

class LowerCaseFormat extends Format {
    private final Locale locale;

    LowerCaseFormat(Locale locale) {
        this.locale = locale;
    }

    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        toAppendTo.append(String.valueOf(obj).toLowerCase(locale));
        return toAppendTo;
    }

    public Object parseObject(String source, ParsePosition pos) {
        if (source == null) {
            pos.setIndex(0);
            return null;
        }
        pos.setIndex(source.length());
        return source.toLowerCase(locale);
    }
}

class UpperCaseFormat extends Format {
    private final Locale locale;

    UpperCaseFormat(Locale locale) {
        this.locale = locale;
    }

    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        toAppendTo.append(String.valueOf(obj).toUpperCase(locale));
        return toAppendTo;
    }

    public Object parseObject(String source, ParsePosition pos) {
        if (source == null) {
            pos.setIndex(0);
            return null;
        }
        pos.setIndex(source.length());
        return source.toUpperCase(locale);
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
<details open><summary>▸ Output (~9,629 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        class LowerCaseFormat extends java.text.Format {
            public StringBuffer format(Object obj, StringBuffer toAppendTo, java.text.FieldPosition pos) {
                String s = String.valueOf(obj);
                toAppendTo.append(s.toLowerCase(java.util.Locale.ROOT));
                return toAppendTo;
            }

            public Object parseObject(String source, java.text.ParsePosition pos) {
                pos.setIndex(source == null ? 0 : source.length());
                return source;
            }
        }
        class UpperCaseFormat extends java.text.Format {
            public StringBuffer format(Object obj, StringBuffer toAppendTo, java.text.FieldPosition pos) {
                String s = String.valueOf(obj);
                toAppendTo.append(s.toUpperCase(java.util.Locale.ROOT));
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

        String anchorPattern = "it''s a {0,lower} 'test'!";
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(anchorPattern, registry);
            String before = emf.toPattern();
            String out1 = emf.format(new Object[] { "DUMMY" });
            String after = emf.toPattern();

            if (!"it's a dummy test!".equals(out1)) {
                throw new RuntimeException("[oracle:anchor-output] metamorphic violation: exact regression input produced unexpected output input="
                        + anchorPattern + " out=" + out1);
            }

            // Contract: formatting is read-only with respect to the pattern; a throw-deleting or
            // branch-skipping patch in applyPattern/appendQuotedString must not silently corrupt
            // the stored pattern observable via toPattern().
            if (before != null && after != null && !before.equals(after)) {
                throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: format mutated toPattern input="
                        + anchorPattern + " before=" + before + " after=" + after);
            }

            // Contract: reparsing the object's own toPattern with the same registry should produce
            // an equivalent formatter. This cross-check computes the same semantics via a second
            // independent library construction.
            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, registry);
            String out2 = emf2.format(new Object[] { "DUMMY" });
            String tp2 = emf2.toPattern();
            if (!out1.equals(out2) || (before != null && tp2 != null && !before.equals(tp2))) {
                throw new RuntimeException("[oracle:roundtrip-pattern] metamorphic violation: reparsing toPattern changed semantics input="
                        + anchorPattern + " lhsOut=" + out1 + " rhsOut=" + out2 + " lhsPattern=" + before
                        + " rhsPattern=" + tp2);
            }
        } catch (Throwable t) {
            boolean oracle = t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:");
            if (oracle) {
                throw (RuntimeException) t;
            }
            boolean rootStack = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                String cn = ste.getClassName();
                String mn = ste.getMethodName();
                if (("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cn)
                        && ("appendQuotedString".equals(mn) || "next".equals(mn) || "startsWith".equals(mn)
                                || "append".equals(mn)))
                        || ("org.apache.commons.lang.text.StrBuilder".equals(cn)
                                && ("append".equals(mn) || "startsWith".equals(mn)))) {
                    rootStack = true;
                    break;
                }
            }
            if (t instanceof OutOfMemoryError && rootStack) {
                throw (OutOfMemoryError) t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            return;
        }

        String raw1 = data.consumeAsciiString(24);
        String raw2 = data.consumeAsciiString(24);
        String raw3 = data.consumeAsciiString(24);
        String raw4 = data.consumeAsciiString(24);
        String arg = data.consumeString(24);
        boolean useLower = data.consumeBoolean();
        boolean includeQuotedLiteral = data.consumeBoolean();
        boolean includeLeadingSpace = data.consumeBoolean();
        boolean includeTrailingPunct = data.consumeBoolean();

        String s1 = raw1.replace("{", "").replace("}", "").replace("'", "").replace(",", "");
        String s2 = raw2.replace("{", "").replace("}", "").replace("'", "").replace(",", "");
        String s3 = raw3.replace("{", "").replace("}", "").replace("'", "").replace(",", "");
        String s4 = raw4.replace("{", "").replace("}", "").replace("'", "").replace(",", "");
        String fmt = useLower ? "lower" : "upper";

        // Valid by construction: includes a doubled quote in literal text, one real format element,
        // and balanced quoted literal when present, so a correct implementation is obligated to
        // accept it.
        StringBuilder patternBuilder = new StringBuilder();
        if (includeLeadingSpace) {
            patternBuilder.append(' ');
        }
        patternBuilder.append(s1);
        patternBuilder.append("''");
        patternBuilder.append(s2);
        patternBuilder.append(" {0,");
        patternBuilder.append(fmt);
        patternBuilder.append("}");
        if (includeQuotedLiteral) {
            patternBuilder.append(" '");
            patternBuilder.append(s3.length() == 0 ? "x" : s3);
            patternBuilder.append("'");
        } else {
            patternBuilder.append(' ');
            patternBuilder.append(s3);
        }
        if (includeTrailingPunct) {
            patternBuilder.append('!');
        } else {
            patternBuilder.append(' ');
            patternBuilder.append(s4);
        }
        String pattern = patternBuilder.toString();

        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
            String before = emf.toPattern();
            String out1 = emf.format(new Object[] { arg });
            String after = emf.toPattern();

            if (before != null && after != null && !before.equals(after)) {
                throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: format mutated toPattern input="
                        + pattern + " before=" + before + " after=" + after);
            }

            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, registry);
            String out2 = emf2.format(new Object[] { arg });
            String tp2 = emf2.toPattern();

            if (!out1.equals(out2) || (before != null && tp2 != null && !before.equals(tp2))) {
                throw new RuntimeException("[oracle:roundtrip-pattern] metamorphic violation: reparsing toPattern changed semantics input="
                        + pattern + " arg=" + arg + " lhsOut=" + out1 + " rhsOut=" + out2 + " lhsPattern="
                        + before + " rhsPattern=" + tp2);
            }
        } catch (Throwable t) {
            boolean oracle = t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:");
            if (oracle) {
                throw (RuntimeException) t;
            }
            boolean rootStack = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                String cn = ste.getClassName();
                String mn = ste.getMethodName();
                if (("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cn)
                        && ("appendQuotedString".equals(mn) || "next".equals(mn) || "startsWith".equals(mn)
                                || "append".equals(mn)))
                        || ("org.apache.commons.lang.text.StrBuilder".equals(cn)
                                && ("append".equals(mn) || "startsWith".equals(mn)))) {
                    rootStack = true;
                    break;
                }
            }
            if (t instanceof OutOfMemoryError && rootStack) {
                throw (OutOfMemoryError) t;
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
## [28] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [29] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~9,826 chars, 1 new)</summary>

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
                String s = String.valueOf(obj);
                toAppendTo.append(s.toLowerCase(java.util.Locale.ROOT));
                return toAppendTo;
            }

            public Object parseObject(String source, java.text.ParsePosition pos) {
                pos.setIndex(source == null ? 0 : source.length());
                return source;
            }
        }
        class UpperCaseFormat extends java.text.Format {
            public StringBuffer format(Object obj, StringBuffer toAppendTo, java.text.FieldPosition pos) {
                String s = String.valueOf(obj);
                toAppendTo.append(s.toUpperCase(java.util.Locale.ROOT));
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

        String anchorPattern = "it''s a {0,lower} 'test'!";
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(anchorPattern, registry);
            String before = emf.toPattern();
            String out1 = emf.format(new Object[] { "DUMMY" });
            String after = emf.toPattern();

            if (!"it's a dummy test!".equals(out1)) {
                throw new RuntimeException("[oracle:anchor-output] metamorphic violation: exact regression input produced unexpected output input="
                        + anchorPattern + " out=" + out1);
            }

            // Contract: formatting is read-only with respect to the pattern; a throw-deleting or
            // branch-skipping patch in applyPattern/appendQuotedString must not silently corrupt
            // the stored pattern observable via toPattern().
            if (before != null && after != null && !before.equals(after)) {
                throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: format mutated toPattern input="
                        + anchorPattern + " before=" + before + " after=" + after);
            }

            // Contract: reparsing the object's own toPattern with the same registry should produce
            // an equivalent formatter. This cross-check computes the same semantics via a second
            // independent library construction.
            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, registry);
            String out2 = emf2.format(new Object[] { "DUMMY" });
            String tp2 = emf2.toPattern();
            if (!out1.equals(out2) || (before != null && tp2 != null && !before.equals(tp2))) {
                throw new RuntimeException("[oracle:roundtrip-pattern] metamorphic violation: reparsing toPattern changed semantics input="
                        + anchorPattern + " lhsOut=" + out1 + " rhsOut=" + out2 + " lhsPattern=" + before
                        + " rhsPattern=" + tp2);
            }
        } catch (Throwable t) {
            boolean oracle = t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:");
            if (oracle) {
                throw (RuntimeException) t;
            }
            boolean rootStack = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                String cn = ste.getClassName();
                String mn = ste.getMethodName();
                if (("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cn)
                        && ("appendQuotedString".equals(mn) || "next".equals(mn) || "startsWith".equals(mn)
                                || "append".equals(mn)))
                        || ("org.apache.commons.lang.text.StrBuilder".equals(cn)
                                && ("append".equals(mn) || "startsWith".equals(mn)))) {
                    rootStack = true;
                    break;
                }
            }
            if (t instanceof OutOfMemoryError && rootStack) {
                throw (OutOfMemoryError) t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            return;
        }

        String raw1 = data.consumeAsciiString(24);
        String raw2 = data.consumeAsciiString(24);
        String raw3 = data.consumeAsciiString(24);
        String raw4 = data.consumeAsciiString(24);
        String arg = data.consumeString(24);
        boolean useLower = data.consumeBoolean();
        boolean includeQuotedLiteral = data.consumeBoolean();
        boolean includeLeadingSpace = data.consumeBoolean();
        boolean includeTrailingPunct = data.consumeBoolean();

        String s1 = raw1.replace("{", "").replace("}", "").replace("'", "").replace(",", "");
        String s2 = raw2.replace("{", "").replace("}", "").replace("'", "").replace(",", "");
        String s3 = raw3.replace("{", "").replace("}", "").replace("'", "").replace(",", "");
        String s4 = raw4.replace("{", "").replace("}", "").replace("'", "").replace(",", "");
        String fmt = useLower ? "lower" : "upper";

        // Valid by construction: includes a doubled quote in literal text, one real format element,
        // and balanced quoted literal when present, so a correct implementation is obligated to
        // accept it.
        StringBuilder patternBuilder = new StringBuilder();
        if (includeLeadingSpace) {
            patternBuilder.append(' ');
        }
        patternBuilder.append(s1);
        patternBuilder.append("''");
        patternBuilder.append(s2);
        patternBuilder.append(" {0,");
        patternBuilder.append(fmt);
        patternBuilder.append("}");
        if (includeQuotedLiteral) {
            patternBuilder.append(" '");
            patternBuilder.append(s3.length() == 0 ? "x" : s3);
            patternBuilder.append("'");
        } else {
            patternBuilder.append(' ');
            patternBuilder.append(s3);
        }
        if (includeTrailingPunct) {
            patternBuilder.append('!');
        } else {
            patternBuilder.append(' ');
            patternBuilder.append(s4);
        }
        String pattern = patternBuilder.toString();

        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
            String before = emf.toPattern();
            String out1 = emf.format(new Object[] { arg });
            String after = emf.toPattern();

            if (before != null && after != null && !before.equals(after)) {
                throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: format mutated toPattern input="
                        + pattern + " before=" + before + " after=" + after);
            }

            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, registry);
            String out2 = emf2.format(new Object[] { arg });
            String tp2 = emf2.toPattern();

            if (!out1.equals(out2) || (before != null && tp2 != null && !before.equals(tp2))) {
                throw new RuntimeException("[oracle:roundtrip-pattern] metamorphic violation: reparsing toPattern changed semantics input="
                        + pattern + " arg=" + arg + " lhsOut=" + out1 + " rhsOut=" + out2 + " lhsPattern="
                        + before + " rhsPattern=" + tp2);
            }
        } catch (Throwable t) {
            boolean oracle = t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:");
            if (oracle) {
                throw (RuntimeException) t;
            }
            boolean rootStack = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                String cn = ste.getClassName();
                String mn = ste.getMethodName();
                if (("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cn)
                        && ("appendQuotedString".equals(mn) || "next".equals(mn) || "startsWith".equals(mn)
                                || "append".equals(mn)))
                        || ("org.apache.commons.lang.text.StrBuilder".equals(cn)
                                && ("append".equals(mn) || "startsWith".equals(mn)))) {
                    rootStack = true;
                    break;
                }
            }
            if (t instanceof OutOfMemoryError && rootStack) {
                throw (OutOfMemoryError) t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            return;
        }
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~2,174 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        // Anchor: exact trigger from the failing test shape.
        // The bug is that appendQuotedString starts at a quote but, on the buggy version,
        // does not advance ParsePosition before checking for the closing quote.
        // For a doubled quote in literal text ("''"), it immediately "closes" on the same quote,
        // leaves position handling inconsistent, and can grow the StringBuffer until OOME.
        new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);

        // Explore the same root cause with varied real patterns:
        // a non-null registry forces ExtendedMessageFormat down its custom applyPattern path,
        // and any doubled quote in literal text before/around a format element exercises
        // appendQuotedString with escapingOn=true.
        String a = sanitize(data.consumeAsciiString(16));
        String b = sanitize(data.consumeAsciiString(16));
        String c = sanitize(data.consumeAsciiString(16));
        String d = sanitize(data.consumeAsciiString(16));

        if (a.length() == 0) a = "x";
        if (b.length() == 0) b = "y";
        if (c.length() == 0) c = "z";

        StringBuilder pattern = new StringBuilder();
        pattern.append(a);
        pattern.append("''");
        pattern.append(b);
        pattern.append(' ');
        pattern.append("{0,lower}");
        pattern.append(' ');
        if (data.consumeBoolean()) {
            pattern.append('\'').append(c).append('\'');
        } else {
            pattern.append(c);
        }
        if (data.consumeBoolean()) {
            pattern.append(' ');
            pattern.append(d);
        }

        new ExtendedMessageFormat(pattern.toString(), registry);
    }

    private static String sanitize(String s) {
        return s.replace("{", "").replace("}", "").replace(",", "").replace("'", "");
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
<details><summary>▸ Prompt (6 message(s), ~2,491 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
- *[assistant] message: identical to step [29] — not reprinted*
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

        // Anchor: exact trigger from the failing test shape.
        // The bug is that appendQuotedString starts at a quote but, on the buggy version,
        // does not advance ParsePosition before checking for the closing quote.
        // For a doubled quote in literal text ("''"), it immediately "closes" on the same quote,
        // leaves position handling inconsistent, and can grow the StringBuffer until OOME.
        new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);

        // Explore the same root cause with varied real patterns:
        // a non-null registry forces ExtendedMessageFormat down its custom applyPattern path,
        // and any doubled quote in literal text before/around a format element exercises
        // appendQuotedString with escapingOn=true.
        String a = sanitize(data.consumeAsciiString(16));
        String b = sanitize(data.consumeAsciiString(16));
        String c = sanitize(data.consumeAsciiString(16));
        String d = sanitize(data.consumeAsciiString(16));

        if (a.length() == 0) a = "x";
        if (b.length() == 0) b = "y";
        if (c.length() == 0) c = "z";

        StringBuilder pattern = new StringBuilder();
        pattern.append(a);
        pattern.append("''");
        pattern.append(b);
        pattern.append(' ');
        pattern.append("{0,lower}");
        pattern.append(' ');
        if (data.consumeBoolean()) {
            pattern.append('\'').append(c).append('\'');
        } else {
            pattern.append(c);
        }
        if (data.consumeBoolean()) {
            pattern.append(' ');
            pattern.append(d);
        }

        new ExtendedMessageFormat(pattern.toString(), registry);
    }

    private static String sanitize(String s) {
        return s.replace("{", "").replace("}", "").replace(",", "").replace("'", "");
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~4,898 chars)</summary>

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
            String s = String.valueOf(obj);
            toAppendTo.append(s.toLowerCase(Locale.ENGLISH));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }

    private static final class UpperCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(s.toUpperCase(Locale.ENGLISH));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
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

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = makeRegistry();

        // Anchor: exact regression test input. On the buggy version this reaches
        // ExtendedMessageFormat.applyPattern -> appendQuotedString and triggers OOME.
        ExtendedMessageFormat anchor = new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: expected=it's a dummy test! actual=" + anchorOut);
        }

        // Explore the same root cause with varied valid patterns that contain a doubled quote.
        // The changed line advances past the opening quote before scanning; so we deliberately
        // build inputs beginning a quoted-string parse at a quote that is immediately followed
        // by another quote somewhere in the pattern.
        String p1 = sanitize(data.consumeAsciiString(12));
        String p2 = sanitize(data.consumeAsciiString(12));
        String p3 = sanitize(data.consumeAsciiString(12));
        String arg = data.consumeString(12);
        boolean useLower = data.consumeBoolean();
        boolean addQuotedTail = data.consumeBoolean();
        boolean addSecondEscapedQuote = data.consumeBoolean();

        if (p1.length() == 0) p1 = "a";
        if (p2.length() == 0) p2 = "b";
        if (p3.length() == 0) p3 = "c";

        StringBuilder pattern = new StringBuilder();
        pattern.append(p1);
        pattern.append("''");
        pattern.append(p2);
        pattern.append(" {0,");
        pattern.append(useLower ? "lower" : "upper");
        pattern.append("}");
        pattern.append(' ');
        if (addQuotedTail) {
            pattern.append('\'').append(p3).append('\'');
        } else {
            pattern.append(p3);
        }
        if (addSecondEscapedQuote) {
            pattern.append(' ');
            pattern.append("q''q");
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern.toString(), registry);
        String out1 = emf.format(new Object[] { arg });

        // Post-condition / metamorphic check:
        // reparsing the formatter's own toPattern with the same registry must preserve behavior.
        String reparsedPattern = emf.toPattern();
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(reparsedPattern, registry);
        String out2 = emf2.format(new Object[] { arg });
        if (!out1.equals(out2)) {
            throw new RuntimeException("[oracle:roundtrip-pattern] metamorphic violation: pattern=" + pattern
                    + " toPattern=" + reparsedPattern + " lhs=" + out1 + " rhs=" + out2);
        }
    }

    private static String sanitize(String s) {
        return s.replace("{", "").replace("}", "").replace(",", "").replace("'", "");
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
<details><summary>▸ Prompt (8 message(s), ~5,335 chars, 1 new)</summary>

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
    private static final class LowerCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(s.toLowerCase(Locale.ENGLISH));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }

    private static final class UpperCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(s.toUpperCase(Locale.ENGLISH));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
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

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = makeRegistry();

        // Anchor: exact regression test input. On the buggy version this reaches
        // ExtendedMessageFormat.applyPattern -> appendQuotedString and triggers OOME.
        ExtendedMessageFormat anchor = new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: expected=it's a dummy test! actual=" + anchorOut);
        }

        // Explore the same root cause with varied valid patterns that contain a doubled quote.
        // The changed line advances past the opening quote before scanning; so we deliberately
        // build inputs beginning a quoted-string parse at a quote that is immediately followed
        // by another quote somewhere in the pattern.
        String p1 = sanitize(data.consumeAsciiString(12));
        String p2 = sanitize(data.consumeAsciiString(12));
        String p3 = sanitize(data.consumeAsciiString(12));
        String arg = data.consumeString(12);
        boolean useLower = data.consumeBoolean();
        boolean addQuotedTail = data.consumeBoolean();
        boolean addSecondEscapedQuote = data.consumeBoolean();

        if (p1.length() == 0) p1 = "a";
        if (p2.length() == 0) p2 = "b";
        if (p3.length() == 0) p3 = "c";

        StringBuilder pattern = new StringBuilder();
        pattern.append(p1);
        pattern.append("''");
        pattern.append(p2);
        pattern.append(" {0,");
        pattern.append(useLower ? "lower" : "upper");
        pattern.append("}");
        pattern.append(' ');
        if (addQuotedTail) {
            pattern.append('\'').append(p3).append('\'');
        } else {
            pattern.append(p3);
        }
        if (addSecondEscapedQuote) {
            pattern.append(' ');
            pattern.append("q''q");
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern.toString(), registry);
        String out1 = emf.format(new Object[] { arg });

        // Post-condition / metamorphic check:
        // reparsing the formatter's own toPattern with the same registry must preserve behavior.
        String reparsedPattern = emf.toPattern();
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(reparsedPattern, registry);
        String out2 = emf2.format(new Object[] { arg });
        if (!out1.equals(out2)) {
            throw new RuntimeException("[oracle:roundtrip-pattern] metamorphic violation: pattern=" + pattern
                    + " toPattern=" + reparsedPattern + " lhs=" + out1 + " rhs=" + out2);
        }
    }

    private static String sanitize(String s) {
        return s.replace("{", "").replace("}", "").replace(",", "").replace("'", "");
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~1,814 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        // Exact anchor from the failing test. A non-null registry is critical because it forces
        // ExtendedMessageFormat.applyPattern() down the custom parsing path that calls
        // appendQuotedString(). The buggy code fails to advance ParsePosition before scanning a
        // quoted string, so a doubled quote in literal text triggers the bad behavior.
        new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);

        // Explore the same condition with valid patterns that contain doubled quotes.
        String left = clean(data.consumeAsciiString(20));
        String mid = clean(data.consumeAsciiString(20));
        String tail = clean(data.consumeAsciiString(20));

        if (left.length() == 0) {
            left = "it";
        }
        if (mid.length() == 0) {
            mid = "s";
        }
        if (tail.length() == 0) {
            tail = "test";
        }

        String formatName = data.consumeBoolean() ? "lower" : "upper";

        StringBuilder pattern = new StringBuilder();
        pattern.append(left);
        pattern.append("''");
        pattern.append(mid);
        pattern.append(" a {0,");
        pattern.append(formatName);
        pattern.append("} '");
        pattern.append(tail);
        pattern.append("'!");

        new ExtendedMessageFormat(pattern.toString(), registry);
    }

    private static String clean(String s) {
        return s.replace("{", "").replace("}", "").replace(",", "").replace("'", "");
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
<details open><summary>▸ Output (~8,861 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        // ANCHOR: exact trigger from the failing test.
        // On the buggy version this reaches appendQuotedString() and loops until OOME
        // before any later validation of the unknown "lower" format can reject the input.
        tryPattern("it''s a {0,lower} 'test'!", registry, false, null);

        // EXPLORE / flip the patched condition:
        // The bug is triggered by doubled quotes (escaped apostrophes) when parsing with a non-null registry.
        // We generate valid-by-construction MessageFormat patterns with many quote placements and lengths.
        String prefix = data.consumeAsciiString(24);
        String middle = data.consumeAsciiString(24);
        String suffix = data.consumeAsciiString(24);
        String quoted = data.consumeAsciiString(24);

        prefix = sanitize(prefix);
        middle = sanitize(middle);
        suffix = sanitize(suffix);
        quoted = sanitize(quoted);

        int mode = data.consumeInt(0, 5);
        String pattern;
        switch (mode) {
            case 0:
                // Boundary: escaped quote near the beginning.
                pattern = prefix + "it''s " + middle + " {0} " + suffix;
                break;
            case 1:
                // Boundary: escaped quote adjacent to a format element.
                pattern = prefix + "''{0}" + suffix;
                break;
            case 2:
                // Boundary flip around the changed condition: quoted literal section plus escaped quote outside it.
                pattern = prefix + "'" + quoted + "'" + " " + middle + "''" + " {0}" + suffix;
                break;
            case 3:
                // Multiple escaped quotes.
                pattern = prefix + "''" + middle + "''" + " {0} '" + quoted + "'" + suffix;
                break;
            case 4:
                // Escaped quote inside surrounding text before and after the argument.
                pattern = prefix + "a''b " + "{0}" + " c''d " + suffix;
                break;
            default:
                // Exact failing shape but without custom format, so it is valid by construction.
                pattern = "it''s a {0} 'test'!" + suffix;
                break;
        }

        // Sound oracle:
        // For patterns with no custom formats, ExtendedMessageFormat with a non-null but empty registry
        // must behave like java.text.MessageFormat on the same pattern.
        // A throw-deleting patch that silently skips quote handling would violate this observable equivalence.
        tryPattern(pattern, registry, true, "dummy");

        // Additional metamorphic/idempotence check on the library's own exposed reader.
        // Contract basis: toPattern() is the pattern representation of the formatter; reconstructing a fresh
        // formatter from that pattern must produce the same toPattern() again for a correct implementation.
        tryIdempotence(pattern, registry);
    }

    private static void tryPattern(String pattern, Map registry, boolean checkOracle, Object arg) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

            if (!checkOracle) {
                try {
                    emf.format(new Object[] { "dummy" });
                } catch (Throwable t) {
                    if (isCleanRejection(t)) {
                        return;
                    }
                    if (isRootCause(t)) {
                        throwRoot(t);
                    }
                }
                return;
            }

            String emfToPatternBefore = null;
            try {
                emfToPatternBefore = emf.toPattern();
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throwRoot(t);
                }
                return;
            }

            String lhs;
            try {
                lhs = emf.format(new Object[] { arg });
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throwRoot(t);
                }
                return;
            }

            String rhs;
            try {
                rhs = new MessageFormat(pattern).format(new Object[] { arg });
            } catch (Throwable t) {
                // If the comparison side rejects, the relation does not apply.
                return;
            }

            if (!lhs.equals(rhs)) {
                throw new RuntimeException("[oracle:mf-eq] metamorphic violation: ExtendedMessageFormat must match MessageFormat for patterns without custom formats input=" + pattern + " lhs=" + lhs + " rhs=" + rhs);
            }

            String emfToPatternAfter;
            try {
                emfToPatternAfter = emf.toPattern();
            } catch (Throwable t) {
                return;
            }

            if (emfToPatternBefore != null && emfToPatternAfter != null && !emfToPatternBefore.equals(emfToPatternAfter)) {
                throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: formatting should not mutate toPattern input=" + pattern + " before=" + emfToPatternBefore + " after=" + emfToPatternAfter);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwRoot(t);
            }
        }
    }

    private static void tryIdempotence(String pattern, Map registry) {
        try {
            ExtendedMessageFormat first = new ExtendedMessageFormat(pattern, registry);
            String p1 = first.toPattern();
            ExtendedMessageFormat second = new ExtendedMessageFormat(p1, registry);
            String p2 = second.toPattern();
            if (p1 != null && p2 != null && !p1.equals(p2)) {
                throw new RuntimeException("[oracle:toPattern-idem] metamorphic violation: reconstructing from toPattern must preserve toPattern input=" + pattern + " first=" + p1 + " second=" + p2);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwRoot(t);
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Validation")
                || name.contains("Invalid")
                || name.contains("Malformed")
                || name.contains("Parse");
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

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == ',') {
                b.append('x');
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }

    private static void throwRoot(Throwable t) {
        if (t instanceof Error) {
            throw (Error) t;
        }
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
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
<details><summary>▸ Prompt (4 message(s), ~9,059 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [11] — not reprinted*
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

        // ANCHOR: exact trigger from the failing test.
        // On the buggy version this reaches appendQuotedString() and loops until OOME
        // before any later validation of the unknown "lower" format can reject the input.
        tryPattern("it''s a {0,lower} 'test'!", registry, false, null);

        // EXPLORE / flip the patched condition:
        // The bug is triggered by doubled quotes (escaped apostrophes) when parsing with a non-null registry.
        // We generate valid-by-construction MessageFormat patterns with many quote placements and lengths.
        String prefix = data.consumeAsciiString(24);
        String middle = data.consumeAsciiString(24);
        String suffix = data.consumeAsciiString(24);
        String quoted = data.consumeAsciiString(24);

        prefix = sanitize(prefix);
        middle = sanitize(middle);
        suffix = sanitize(suffix);
        quoted = sanitize(quoted);

        int mode = data.consumeInt(0, 5);
        String pattern;
        switch (mode) {
            case 0:
                // Boundary: escaped quote near the beginning.
                pattern = prefix + "it''s " + middle + " {0} " + suffix;
                break;
            case 1:
                // Boundary: escaped quote adjacent to a format element.
                pattern = prefix + "''{0}" + suffix;
                break;
            case 2:
                // Boundary flip around the changed condition: quoted literal section plus escaped quote outside it.
                pattern = prefix + "'" + quoted + "'" + " " + middle + "''" + " {0}" + suffix;
                break;
            case 3:
                // Multiple escaped quotes.
                pattern = prefix + "''" + middle + "''" + " {0} '" + quoted + "'" + suffix;
                break;
            case 4:
                // Escaped quote inside surrounding text before and after the argument.
                pattern = prefix + "a''b " + "{0}" + " c''d " + suffix;
                break;
            default:
                // Exact failing shape but without custom format, so it is valid by construction.
                pattern = "it''s a {0} 'test'!" + suffix;
                break;
        }

        // Sound oracle:
        // For patterns with no custom formats, ExtendedMessageFormat with a non-null but empty registry
        // must behave like java.text.MessageFormat on the same pattern.
        // A throw-deleting patch that silently skips quote handling would violate this observable equivalence.
        tryPattern(pattern, registry, true, "dummy");

        // Additional metamorphic/idempotence check on the library's own exposed reader.
        // Contract basis: toPattern() is the pattern representation of the formatter; reconstructing a fresh
        // formatter from that pattern must produce the same toPattern() again for a correct implementation.
        tryIdempotence(pattern, registry);
    }

    private static void tryPattern(String pattern, Map registry, boolean checkOracle, Object arg) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

            if (!checkOracle) {
                try {
                    emf.format(new Object[] { "dummy" });
                } catch (Throwable t) {
                    if (isCleanRejection(t)) {
                        return;
                    }
                    if (isRootCause(t)) {
                        throwRoot(t);
                    }
                }
                return;
            }

            String emfToPatternBefore = null;
            try {
                emfToPatternBefore = emf.toPattern();
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throwRoot(t);
                }
                return;
            }

            String lhs;
            try {
                lhs = emf.format(new Object[] { arg });
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throwRoot(t);
                }
                return;
            }

            String rhs;
            try {
                rhs = new MessageFormat(pattern).format(new Object[] { arg });
            } catch (Throwable t) {
                // If the comparison side rejects, the relation does not apply.
                return;
            }

            if (!lhs.equals(rhs)) {
                throw new RuntimeException("[oracle:mf-eq] metamorphic violation: ExtendedMessageFormat must match MessageFormat for patterns without custom formats input=" + pattern + " lhs=" + lhs + " rhs=" + rhs);
            }

            String emfToPatternAfter;
            try {
                emfToPatternAfter = emf.toPattern();
            } catch (Throwable t) {
                return;
            }

            if (emfToPatternBefore != null && emfToPatternAfter != null && !emfToPatternBefore.equals(emfToPatternAfter)) {
                throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: formatting should not mutate toPattern input=" + pattern + " before=" + emfToPatternBefore + " after=" + emfToPatternAfter);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwRoot(t);
            }
        }
    }

    private static void tryIdempotence(String pattern, Map registry) {
        try {
            ExtendedMessageFormat first = new ExtendedMessageFormat(pattern, registry);
            String p1 = first.toPattern();
            ExtendedMessageFormat second = new ExtendedMessageFormat(p1, registry);
            String p2 = second.toPattern();
            if (p1 != null && p2 != null && !p1.equals(p2)) {
                throw new RuntimeException("[oracle:toPattern-idem] metamorphic violation: reconstructing from toPattern must preserve toPattern input=" + pattern + " first=" + p1 + " second=" + p2);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwRoot(t);
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Validation")
                || name.contains("Invalid")
                || name.contains("Malformed")
                || name.contains("Parse");
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

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == ',') {
                b.append('x');
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }

    private static void throwRoot(Throwable t) {
        if (t instanceof Error) {
            throw (Error) t;
        }
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        throw new RuntimeException(t);
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~3,126 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        // ANCHOR: exact behavioural trigger shape from the bug, but using only standard MessageFormat syntax
        // so the fixed version is obligated to accept it.
        exercise("it''s a {0} 'test'!", registry, "dummy");

        String a = sanitize(data.consumeAsciiString(20));
        String b = sanitize(data.consumeAsciiString(20));
        String c = sanitize(data.consumeAsciiString(20));

        int mode = data.consumeInt(0, 5);
        String pattern;
        switch (mode) {
            case 0:
                pattern = a + "''" + b + " {0} " + c;
                break;
            case 1:
                pattern = a + " {0} ''" + b + c;
                break;
            case 2:
                pattern = a + "''" + b + " '{0}' " + c;
                break;
            case 3:
                pattern = a + " '" + b + "' {0} ''" + c;
                break;
            case 4:
                pattern = "it''s " + a + " {0} '" + b + "' " + c;
                break;
            default:
                pattern = a + "''" + b + " {0} '" + c + "'";
                break;
        }

        exercise(pattern, registry, "dummy");
    }

    private static void exercise(String pattern, Map registry, Object arg) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

        // Contract/oracle: with no custom formats in the pattern, ExtendedMessageFormat must behave
        // the same as MessageFormat on the same valid pattern.
        String actual = emf.format(new Object[] { arg });
        String expected = new MessageFormat(pattern).format(new Object[] { arg });
        if (!expected.equals(actual)) {
            throw new RuntimeException("[oracle:mf-eq] metamorphic violation: pattern=" + pattern + " expected=" + expected + " actual=" + actual);
        }

        // Observable state check: formatting should not mutate the formatter's pattern representation.
        String p1 = emf.toPattern();
        String p2 = emf.toPattern();
        if (p1 == null ? p2 != null : !p1.equals(p2)) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: pattern=" + pattern + " first=" + p1 + " second=" + p2);
        }
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'' || ch < 0x20) {
                out.append('x');
            } else {
                out.append(ch);
            }
        }
        if (out.length() == 0) {
            out.append('x');
        }
        return out.toString();
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
<details><summary>▸ Prompt (6 message(s), ~3,444 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [11] — not reprinted*
- *[assistant] message: identical to step [37] — not reprinted*
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

        // ANCHOR: exact behavioural trigger shape from the bug, but using only standard MessageFormat syntax
        // so the fixed version is obligated to accept it.
        exercise("it''s a {0} 'test'!", registry, "dummy");

        String a = sanitize(data.consumeAsciiString(20));
        String b = sanitize(data.consumeAsciiString(20));
        String c = sanitize(data.consumeAsciiString(20));

        int mode = data.consumeInt(0, 5);
        String pattern;
        switch (mode) {
            case 0:
                pattern = a + "''" + b + " {0} " + c;
                break;
            case 1:
                pattern = a + " {0} ''" + b + c;
                break;
            case 2:
                pattern = a + "''" + b + " '{0}' " + c;
                break;
            case 3:
                pattern = a + " '" + b + "' {0} ''" + c;
                break;
            case 4:
                pattern = "it''s " + a + " {0} '" + b + "' " + c;
                break;
            default:
                pattern = a + "''" + b + " {0} '" + c + "'";
                break;
        }

        exercise(pattern, registry, "dummy");
    }

    private static void exercise(String pattern, Map registry, Object arg) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

        // Contract/oracle: with no custom formats in the pattern, ExtendedMessageFormat must behave
        // the same as MessageFormat on the same valid pattern.
        String actual = emf.format(new Object[] { arg });
        String expected = new MessageFormat(pattern).format(new Object[] { arg });
        if (!expected.equals(actual)) {
            throw new RuntimeException("[oracle:mf-eq] metamorphic violation: pattern=" + pattern + " expected=" + expected + " actual=" + actual);
        }

        // Observable state check: formatting should not mutate the formatter's pattern representation.
        String p1 = emf.toPattern();
        String p2 = emf.toPattern();
        if (p1 == null ? p2 != null : !p1.equals(p2)) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: pattern=" + pattern + " first=" + p1 + " second=" + p2);
        }
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'' || ch < 0x20) {
                out.append('x');
            } else {
                out.append(ch);
            }
        }
        if (out.length() == 0) {
            out.append('x');
        }
        return out.toString();
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~5,602 chars)</summary>

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

        // ANCHOR: exact failing-test setup. On the buggy version, any leading quote handling
        // in applyPattern() with a non-null registry reaches appendQuotedString() and loops
        // because pos is not advanced before the escapingOn fast-path.
        ExtendedMessageFormat anchor = new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-exact] metamorphic violation: expected=it's a dummy test! actual=" + anchorOut);
        }
        String anchorPattern1 = anchor.toPattern();
        String anchorPattern2 = anchor.toPattern();
        if (anchorPattern1 == null ? anchorPattern2 != null : !anchorPattern1.equals(anchorPattern2)) {
            throw new RuntimeException("[oracle:anchor-topattern] metamorphic violation: first=" + anchorPattern1 + " second=" + anchorPattern2);
        }

        String a = sanitize(data.consumeAsciiString(16));
        String b = sanitize(data.consumeAsciiString(16));
        String c = sanitize(data.consumeAsciiString(16));
        String d = sanitize(data.consumeAsciiString(16));

        String pattern;
        switch (data.consumeInt(0, 5)) {
            case 0:
                pattern = a + "''" + b + " {0,lower} '" + c + "'";
                break;
            case 1:
                pattern = "'" + a + "' " + b + "'' " + "{0,lower} " + c;
                break;
            case 2:
                pattern = a + " {0,lower} ''" + b + " '" + c + "'";
                break;
            case 3:
                pattern = "it''s " + a + " {0,lower} '" + b + "' " + c;
                break;
            case 4:
                pattern = "'" + a + "'" + " {0,lower} " + b + "''" + c + "'" + d + "'";
                break;
            default:
                pattern = a + "''" + b + " {0,lower} '" + c + "' " + d;
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

        // Sound metamorphic relation: the "lower" format lower-cases its argument, so formatting
        // the same valid pattern with "DUMMY" and "dummy" must produce the same output.
        String outUpper = emf.format(new Object[] { "DUMMY" });
        String outLower = emf.format(new Object[] { "dummy" });
        if (!outUpper.equals(outLower)) {
            throw new RuntimeException("[oracle:lower-eq] metamorphic violation: pattern=" + pattern + " upper=" + outUpper + " lower=" + outLower);
        }

        // Observable post-condition: formatting should not mutate the stored pattern representation.
        String p1 = emf.toPattern();
        String p2 = emf.toPattern();
        if (p1 == null ? p2 != null : !p1.equals(p2)) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: pattern=" + pattern + " first=" + p1 + " second=" + p2);
        }
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuffer sb = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == ',' || ch == '\'') {
                sb.append('x');
            } else if (ch < 0x20) {
                sb.append('x');
            } else {
                sb.append(ch);
            }
        }
        if (sb.length() == 0) {
            sb.append('x');
        }
        return sb.toString();
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

    public static class LowerCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj == null) {
                return toAppendTo;
            }
            toAppendTo.append(obj.toString().toLowerCase());
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source.toLowerCase();
        }
    }

    public static class UpperCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj == null) {
                return toAppendTo;
            }
            toAppendTo.append(obj.toString().toUpperCase());
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source.toUpperCase();
        }
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
<details><summary>▸ Prompt (8 message(s), ~6,040 chars, 1 new)</summary>

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
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        // ANCHOR: exact failing-test setup. On the buggy version, any leading quote handling
        // in applyPattern() with a non-null registry reaches appendQuotedString() and loops
        // because pos is not advanced before the escapingOn fast-path.
        ExtendedMessageFormat anchor = new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-exact] metamorphic violation: expected=it's a dummy test! actual=" + anchorOut);
        }
        String anchorPattern1 = anchor.toPattern();
        String anchorPattern2 = anchor.toPattern();
        if (anchorPattern1 == null ? anchorPattern2 != null : !anchorPattern1.equals(anchorPattern2)) {
            throw new RuntimeException("[oracle:anchor-topattern] metamorphic violation: first=" + anchorPattern1 + " second=" + anchorPattern2);
        }

        String a = sanitize(data.consumeAsciiString(16));
        String b = sanitize(data.consumeAsciiString(16));
        String c = sanitize(data.consumeAsciiString(16));
        String d = sanitize(data.consumeAsciiString(16));

        String pattern;
        switch (data.consumeInt(0, 5)) {
            case 0:
                pattern = a + "''" + b + " {0,lower} '" + c + "'";
                break;
            case 1:
                pattern = "'" + a + "' " + b + "'' " + "{0,lower} " + c;
                break;
            case 2:
                pattern = a + " {0,lower} ''" + b + " '" + c + "'";
                break;
            case 3:
                pattern = "it''s " + a + " {0,lower} '" + b + "' " + c;
                break;
            case 4:
                pattern = "'" + a + "'" + " {0,lower} " + b + "''" + c + "'" + d + "'";
                break;
            default:
                pattern = a + "''" + b + " {0,lower} '" + c + "' " + d;
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

        // Sound metamorphic relation: the "lower" format lower-cases its argument, so formatting
        // the same valid pattern with "DUMMY" and "dummy" must produce the same output.
        String outUpper = emf.format(new Object[] { "DUMMY" });
        String outLower = emf.format(new Object[] { "dummy" });
        if (!outUpper.equals(outLower)) {
            throw new RuntimeException("[oracle:lower-eq] metamorphic violation: pattern=" + pattern + " upper=" + outUpper + " lower=" + outLower);
        }

        // Observable post-condition: formatting should not mutate the stored pattern representation.
        String p1 = emf.toPattern();
        String p2 = emf.toPattern();
        if (p1 == null ? p2 != null : !p1.equals(p2)) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: pattern=" + pattern + " first=" + p1 + " second=" + p2);
        }
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuffer sb = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == ',' || ch == '\'') {
                sb.append('x');
            } else if (ch < 0x20) {
                sb.append('x');
            } else {
                sb.append(ch);
            }
        }
        if (sb.length() == 0) {
            sb.append('x');
        }
        return sb.toString();
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

    public static class LowerCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj == null) {
                return toAppendTo;
            }
            toAppendTo.append(obj.toString().toLowerCase());
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source.toLowerCase();
        }
    }

    public static class UpperCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj == null) {
                return toAppendTo;
            }
            toAppendTo.append(obj.toString().toUpperCase());
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source.toUpperCase();
        }
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~3,649 chars)</summary>

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

        String pattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

        String actual = emf.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(actual)) {
            throw new RuntimeException("[oracle:exact-output] metamorphic violation: expected=it's a dummy test! actual=" + actual);
        }

        String before = emf.toPattern();
        String after = emf.toPattern();
        if (before == null ? after != null : !before.equals(after)) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: before=" + before + " after=" + after);
        }

        String fuzz = sanitize(data.consumeAsciiString(16));
        String varied = fuzz + "''s a {0,lower} 'test'!";
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(varied, registry);
        String out1 = emf2.format(new Object[] { "DUMMY" });
        String out2 = emf2.format(new Object[] { "dummy" });
        if (!out1.equals(out2)) {
            throw new RuntimeException("[oracle:lowercase-format] metamorphic violation: pattern=" + varied + " upper=" + out1 + " lower=" + out2);
        }
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "it";
        }
        StringBuffer sb = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == ',' || ch == '\'' || ch < 0x20) {
                sb.append('x');
            } else {
                sb.append(ch);
            }
        }
        return sb.length() == 0 ? "it" : sb.toString();
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

    public static class LowerCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toLowerCase());
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source.toLowerCase();
        }
    }

    public static class UpperCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toUpperCase());
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source.toUpperCase();
        }
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
<details open><summary>▸ Output (~12,274 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;
import java.text.Format;
import java.text.FieldPosition;
import java.text.ParsePosition;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        class LowerCaseFormat extends Format {
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                if (obj != null) {
                    toAppendTo.append(obj.toString().toLowerCase(Locale.ROOT));
                }
                return toAppendTo;
            }

            public Object parseObject(String source, ParsePosition pos) {
                if (source == null) {
                    return null;
                }
                pos.setIndex(source.length());
                return source.toLowerCase(Locale.ROOT);
            }
        }

        class UpperCaseFormat extends Format {
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                if (obj != null) {
                    toAppendTo.append(obj.toString().toUpperCase(Locale.ROOT));
                }
                return toAppendTo;
            }

            public Object parseObject(String source, ParsePosition pos) {
                if (source == null) {
                    return null;
                }
                pos.setIndex(source.length());
                return source.toUpperCase(Locale.ROOT);
            }
        }

        class LowerCaseFormatFactory implements FormatFactory {
            public Format getFormat(String name, String arguments, Locale locale) {
                return new LowerCaseFormat();
            }
        }

        class UpperCaseFormatFactory implements FormatFactory {
            public Format getFormat(String name, String arguments, Locale locale) {
                return new UpperCaseFormat();
            }
        }

        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        {
            String pattern = "it''s a {0,lower} 'test'!";
            ExtendedMessageFormat emf = null;
            String before = null;
            String formatted = null;
            String after = null;
            try {
                emf = new ExtendedMessageFormat(pattern, registry);
                before = emf.toPattern();
                formatted = emf.format(new Object[] { "DUMMY" });
                after = emf.toPattern();
            } catch (Throwable t) {
                boolean rooted = false;
                if (t instanceof OutOfMemoryError) {
                    for (StackTraceElement ste : t.getStackTrace()) {
                        if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                                && ("appendQuotedString".equals(ste.getMethodName())
                                        || "next".equals(ste.getMethodName())
                                        || "append".equals(ste.getMethodName())
                                        || "startsWith".equals(ste.getMethodName()))) {
                            rooted = true;
                            break;
                        }
                        if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                                && ("append".equals(ste.getMethodName())
                                        || "startsWith".equals(ste.getMethodName()))) {
                            rooted = true;
                            break;
                        }
                    }
                }
                if (rooted) {
                    throw (OutOfMemoryError) t;
                }
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    return;
                }
                String cn = t.getClass().getName();
                if (cn.indexOf("Invalid") >= 0 || cn.indexOf("Validation") >= 0) {
                    return;
                }
            }
            if (emf != null) {
                if (!pattern.equals(before)) {
                    throw new RuntimeException("[oracle:anchor-topattern] metamorphic violation: toPattern should preserve the supplied extended pattern input=" + pattern + " got=" + before);
                }
                if (!before.equals(after)) {
                    throw new RuntimeException("[oracle:anchor-state] metamorphic violation: format() is read-only and must not change toPattern input=" + pattern + " before=" + before + " after=" + after);
                }
                if (!"it's a dummy test!".equals(formatted)) {
                    throw new RuntimeException("[oracle:anchor-format] metamorphic violation: valid anchor pattern formatted unexpectedly input=" + pattern + " out=" + formatted);
                }
                try {
                    ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, registry);
                    String formatted2 = emf2.format(new Object[] { "DUMMY" });
                    if (!formatted.equals(formatted2)) {
                        throw new RuntimeException("[oracle:anchor-roundtrip] metamorphic violation: formatting after reparsing toPattern must agree input=" + pattern + " lhs=" + formatted + " rhs=" + formatted2);
                    }
                } catch (Throwable t) {
                    boolean rooted = false;
                    if (t instanceof OutOfMemoryError) {
                        for (StackTraceElement ste : t.getStackTrace()) {
                            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                                    && ("appendQuotedString".equals(ste.getMethodName())
                                            || "next".equals(ste.getMethodName())
                                            || "append".equals(ste.getMethodName())
                                            || "startsWith".equals(ste.getMethodName()))) {
                                rooted = true;
                                break;
                            }
                            if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                                    && ("append".equals(ste.getMethodName())
                                            || "startsWith".equals(ste.getMethodName()))) {
                                rooted = true;
                                break;
                            }
                        }
                    }
                    if (rooted) {
                        throw (OutOfMemoryError) t;
                    }
                }
            }
        }

        String a = data.consumeAsciiString(24);
        String b = data.consumeAsciiString(24);
        String c = data.consumeAsciiString(24);
        String arg = data.consumeAsciiString(24);

        a = a.replace('{', 'a').replace('}', 'b').replace('\'', 'c').replace(',', 'd');
        b = b.replace('{', 'a').replace('}', 'b').replace('\'', 'c').replace(',', 'd');
        c = c.replace('{', 'a').replace('}', 'b').replace('\'', 'c').replace(',', 'd');
        arg = arg.replace('{', 'a').replace('}', 'b').replace('\'', 'c').replace(',', 'd');

        if (a.length() == 0) {
            a = "A";
        }
        if (b.length() == 0) {
            b = "B";
        }
        if (c.length() == 0) {
            c = "C";
        }
        if (arg.length() == 0) {
            arg = "DuMmY";
        }

        String pattern;
        String expected;
        if (data.consumeBoolean()) {
            pattern = "''" + a + " {0,lower} '" + c + "'";
            expected = "'" + a + " " + arg.toLowerCase(Locale.ROOT) + " " + c;
        } else {
            pattern = a + "''" + b + " {0,lower} '" + c + "'";
            expected = a + "'" + b + " " + arg.toLowerCase(Locale.ROOT) + " " + c;
        }

        ExtendedMessageFormat emf = null;
        String before = null;
        String formatted = null;
        String after = null;
        try {
            emf = new ExtendedMessageFormat(pattern, registry);
            before = emf.toPattern();
            formatted = emf.format(new Object[] { arg });
            after = emf.toPattern();
        } catch (Throwable t) {
            boolean rooted = false;
            if (t instanceof OutOfMemoryError) {
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                            && ("appendQuotedString".equals(ste.getMethodName())
                                    || "next".equals(ste.getMethodName())
                                    || "append".equals(ste.getMethodName())
                                    || "startsWith".equals(ste.getMethodName()))) {
                        rooted = true;
                        break;
                    }
                    if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                            && ("append".equals(ste.getMethodName())
                                    || "startsWith".equals(ste.getMethodName()))) {
                        rooted = true;
                        break;
                    }
                }
            }
            if (rooted) {
                throw (OutOfMemoryError) t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            String cn = t.getClass().getName();
            if (cn.indexOf("Invalid") >= 0 || cn.indexOf("Validation") >= 0) {
                return;
            }
            return;
        }

        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:state] metamorphic violation: format() must not mutate toPattern input=" + pattern + " before=" + before + " after=" + after);
        }

        if (!pattern.equals(before)) {
            throw new RuntimeException("[oracle:topattern] metamorphic violation: toPattern should reproduce the valid extended pattern input=" + pattern + " got=" + before);
        }

        if (!expected.equals(formatted)) {
            throw new RuntimeException("[oracle:format] metamorphic violation: valid-by-construction lower/quote pattern formatted incorrectly input=" + pattern + " expected=" + expected + " got=" + formatted);
        }

        try {
            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, registry);
            String formatted2 = emf2.format(new Object[] { arg });
            if (!formatted.equals(formatted2)) {
                throw new RuntimeException("[oracle:roundtrip] metamorphic violation: reparsing toPattern must preserve formatting input=" + pattern + " lhs=" + formatted + " rhs=" + formatted2);
            }
        } catch (Throwable t) {
            boolean rooted = false;
            if (t instanceof OutOfMemoryError) {
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                            && ("appendQuotedString".equals(ste.getMethodName())
                                    || "next".equals(ste.getMethodName())
                                    || "append".equals(ste.getMethodName())
                                    || "startsWith".equals(ste.getMethodName()))) {
                        rooted = true;
                        break;
                    }
                    if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                            && ("append".equals(ste.getMethodName())
                                    || "startsWith".equals(ste.getMethodName()))) {
                        rooted = true;
                        break;
                    }
                }
            }
            if (rooted) {
                throw (OutOfMemoryError) t;
            }
        }
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
<details><summary>▸ Prompt (4 message(s), ~12,472 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [19] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;
import java.text.Format;
import java.text.FieldPosition;
import java.text.ParsePosition;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        class LowerCaseFormat extends Format {
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                if (obj != null) {
                    toAppendTo.append(obj.toString().toLowerCase(Locale.ROOT));
                }
                return toAppendTo;
            }

            public Object parseObject(String source, ParsePosition pos) {
                if (source == null) {
                    return null;
                }
                pos.setIndex(source.length());
                return source.toLowerCase(Locale.ROOT);
            }
        }

        class UpperCaseFormat extends Format {
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                if (obj != null) {
                    toAppendTo.append(obj.toString().toUpperCase(Locale.ROOT));
                }
                return toAppendTo;
            }

            public Object parseObject(String source, ParsePosition pos) {
                if (source == null) {
                    return null;
                }
                pos.setIndex(source.length());
                return source.toUpperCase(Locale.ROOT);
            }
        }

        class LowerCaseFormatFactory implements FormatFactory {
            public Format getFormat(String name, String arguments, Locale locale) {
                return new LowerCaseFormat();
            }
        }

        class UpperCaseFormatFactory implements FormatFactory {
            public Format getFormat(String name, String arguments, Locale locale) {
                return new UpperCaseFormat();
            }
        }

        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        {
            String pattern = "it''s a {0,lower} 'test'!";
            ExtendedMessageFormat emf = null;
            String before = null;
            String formatted = null;
            String after = null;
            try {
                emf = new ExtendedMessageFormat(pattern, registry);
                before = emf.toPattern();
                formatted = emf.format(new Object[] { "DUMMY" });
                after = emf.toPattern();
            } catch (Throwable t) {
                boolean rooted = false;
                if (t instanceof OutOfMemoryError) {
                    for (StackTraceElement ste : t.getStackTrace()) {
                        if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                                && ("appendQuotedString".equals(ste.getMethodName())
                                        || "next".equals(ste.getMethodName())
                                        || "append".equals(ste.getMethodName())
                                        || "startsWith".equals(ste.getMethodName()))) {
                            rooted = true;
                            break;
                        }
                        if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                                && ("append".equals(ste.getMethodName())
                                        || "startsWith".equals(ste.getMethodName()))) {
                            rooted = true;
                            break;
                        }
                    }
                }
                if (rooted) {
                    throw (OutOfMemoryError) t;
                }
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    return;
                }
                String cn = t.getClass().getName();
                if (cn.indexOf("Invalid") >= 0 || cn.indexOf("Validation") >= 0) {
                    return;
                }
            }
            if (emf != null) {
                if (!pattern.equals(before)) {
                    throw new RuntimeException("[oracle:anchor-topattern] metamorphic violation: toPattern should preserve the supplied extended pattern input=" + pattern + " got=" + before);
                }
                if (!before.equals(after)) {
                    throw new RuntimeException("[oracle:anchor-state] metamorphic violation: format() is read-only and must not change toPattern input=" + pattern + " before=" + before + " after=" + after);
                }
                if (!"it's a dummy test!".equals(formatted)) {
                    throw new RuntimeException("[oracle:anchor-format] metamorphic violation: valid anchor pattern formatted unexpectedly input=" + pattern + " out=" + formatted);
                }
                try {
                    ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, registry);
                    String formatted2 = emf2.format(new Object[] { "DUMMY" });
                    if (!formatted.equals(formatted2)) {
                        throw new RuntimeException("[oracle:anchor-roundtrip] metamorphic violation: formatting after reparsing toPattern must agree input=" + pattern + " lhs=" + formatted + " rhs=" + formatted2);
                    }
                } catch (Throwable t) {
                    boolean rooted = false;
                    if (t instanceof OutOfMemoryError) {
                        for (StackTraceElement ste : t.getStackTrace()) {
                            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                                    && ("appendQuotedString".equals(ste.getMethodName())
                                            || "next".equals(ste.getMethodName())
                                            || "append".equals(ste.getMethodName())
                                            || "startsWith".equals(ste.getMethodName()))) {
                                rooted = true;
                                break;
                            }
                            if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                                    && ("append".equals(ste.getMethodName())
                                            || "startsWith".equals(ste.getMethodName()))) {
                                rooted = true;
                                break;
                            }
                        }
                    }
                    if (rooted) {
                        throw (OutOfMemoryError) t;
                    }
                }
            }
        }

        String a = data.consumeAsciiString(24);
        String b = data.consumeAsciiString(24);
        String c = data.consumeAsciiString(24);
        String arg = data.consumeAsciiString(24);

        a = a.replace('{', 'a').replace('}', 'b').replace('\'', 'c').replace(',', 'd');
        b = b.replace('{', 'a').replace('}', 'b').replace('\'', 'c').replace(',', 'd');
        c = c.replace('{', 'a').replace('}', 'b').replace('\'', 'c').replace(',', 'd');
        arg = arg.replace('{', 'a').replace('}', 'b').replace('\'', 'c').replace(',', 'd');

        if (a.length() == 0) {
            a = "A";
        }
        if (b.length() == 0) {
            b = "B";
        }
        if (c.length() == 0) {
            c = "C";
        }
        if (arg.length() == 0) {
            arg = "DuMmY";
        }

        String pattern;
        String expected;
        if (data.consumeBoolean()) {
            pattern = "''" + a + " {0,lower} '" + c + "'";
            expected = "'" + a + " " + arg.toLowerCase(Locale.ROOT) + " " + c;
        } else {
            pattern = a + "''" + b + " {0,lower} '" + c + "'";
            expected = a + "'" + b + " " + arg.toLowerCase(Locale.ROOT) + " " + c;
        }

        ExtendedMessageFormat emf = null;
        String before = null;
        String formatted = null;
        String after = null;
        try {
            emf = new ExtendedMessageFormat(pattern, registry);
            before = emf.toPattern();
            formatted = emf.format(new Object[] { arg });
            after = emf.toPattern();
        } catch (Throwable t) {
            boolean rooted = false;
            if (t instanceof OutOfMemoryError) {
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                            && ("appendQuotedString".equals(ste.getMethodName())
                                    || "next".equals(ste.getMethodName())
                                    || "append".equals(ste.getMethodName())
                                    || "startsWith".equals(ste.getMethodName()))) {
                        rooted = true;
                        break;
                    }
                    if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                            && ("append".equals(ste.getMethodName())
                                    || "startsWith".equals(ste.getMethodName()))) {
                        rooted = true;
                        break;
                    }
                }
            }
            if (rooted) {
                throw (OutOfMemoryError) t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            String cn = t.getClass().getName();
            if (cn.indexOf("Invalid") >= 0 || cn.indexOf("Validation") >= 0) {
                return;
            }
            return;
        }

        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:state] metamorphic violation: format() must not mutate toPattern input=" + pattern + " before=" + before + " after=" + after);
        }

        if (!pattern.equals(before)) {
            throw new RuntimeException("[oracle:topattern] metamorphic violation: toPattern should reproduce the valid extended pattern input=" + pattern + " got=" + before);
        }

        if (!expected.equals(formatted)) {
            throw new RuntimeException("[oracle:format] metamorphic violation: valid-by-construction lower/quote pattern formatted incorrectly input=" + pattern + " expected=" + expected + " got=" + formatted);
        }

        try {
            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, registry);
            String formatted2 = emf2.format(new Object[] { arg });
            if (!formatted.equals(formatted2)) {
                throw new RuntimeException("[oracle:roundtrip] metamorphic violation: reparsing toPattern must preserve formatting input=" + pattern + " lhs=" + formatted + " rhs=" + formatted2);
            }
        } catch (Throwable t) {
            boolean rooted = false;
            if (t instanceof OutOfMemoryError) {
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                            && ("appendQuotedString".equals(ste.getMethodName())
                                    || "next".equals(ste.getMethodName())
                                    || "append".equals(ste.getMethodName())
                                    || "startsWith".equals(ste.getMethodName()))) {
                        rooted = true;
                        break;
                    }
                    if ("org.apache.commons.lang.text.StrBuilder".equals(ste.getClassName())
                            && ("append".equals(ste.getMethodName())
                                    || "startsWith".equals(ste.getMethodName()))) {
                        rooted = true;
                        break;
                    }
                }
            }
            if (rooted) {
                throw (OutOfMemoryError) t;
            }
        }
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~5,255 chars)</summary>

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
            pos.setIndex(source == null ? 0 : source.length());
            return source == null ? null : source.toLowerCase(Locale.ROOT);
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
            pos.setIndex(source == null ? 0 : source.length());
            return source == null ? null : source.toUpperCase(Locale.ROOT);
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

    private static String cleanLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == ',') {
                sb.append('x');
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = makeRegistry();

        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, registry);
        String anchorFormatted = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorFormatted)) {
            throw new RuntimeException("[oracle:anchor-format] metamorphic violation: input=" + anchorPattern + " got=" + anchorFormatted);
        }
        String anchorBefore = anchor.toPattern();
        String anchorAgain = anchor.format(new Object[] { "DUMMY" });
        String anchorAfter = anchor.toPattern();
        if (!anchorBefore.equals(anchorAfter) || !anchorFormatted.equals(anchorAgain)) {
            throw new RuntimeException("[oracle:anchor-state] metamorphic violation: before=" + anchorBefore + " after=" + anchorAfter + " once=" + anchorFormatted + " twice=" + anchorAgain);
        }

        String left = cleanLiteral(data.consumeAsciiString(20));
        String right = cleanLiteral(data.consumeAsciiString(20));
        String tail = cleanLiteral(data.consumeAsciiString(20));
        String arg = cleanLiteral(data.consumeAsciiString(20));
        if (arg.length() == 0) {
            arg = "DuMmY";
        }

        int variant = data.consumeInt(0, 3);
        String pattern;
        if (variant == 0) {
            pattern = left + "''" + right + " {0,lower} '" + tail + "'";
        } else if (variant == 1) {
            pattern = "''" + left + " {0,lower} '" + tail + "'";
        } else if (variant == 2) {
            pattern = left + " '' " + right + " {0,upper} '" + tail + "'";
        } else {
            pattern = left + "''s {0,lower} '" + tail + "'!";
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String before = emf.toPattern();
        String out1 = emf.format(new Object[] { arg });
        String out2 = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        if (!before.equals(after) || !out1.equals(out2)) {
            throw new RuntimeException("[oracle:determinism] metamorphic violation: pattern=" + pattern + " before=" + before + " after=" + after + " out1=" + out1 + " out2=" + out2);
        }

        ExtendedMessageFormat reparsed = new ExtendedMessageFormat(before, registry);
        String out3 = reparsed.format(new Object[] { arg });
        if (!out1.equals(out3)) {
            throw new RuntimeException("[oracle:roundtrip] metamorphic violation: pattern=" + pattern + " toPattern=" + before + " lhs=" + out1 + " rhs=" + out3);
        }
    }
}
```

</details>

---
## [46] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [47] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~5,573 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [19] — not reprinted*
- *[assistant] message: identical to step [45] — not reprinted*
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
            pos.setIndex(source == null ? 0 : source.length());
            return source == null ? null : source.toLowerCase(Locale.ROOT);
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
            pos.setIndex(source == null ? 0 : source.length());
            return source == null ? null : source.toUpperCase(Locale.ROOT);
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

    private static String cleanLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == ',') {
                sb.append('x');
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = makeRegistry();

        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, registry);
        String anchorFormatted = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorFormatted)) {
            throw new RuntimeException("[oracle:anchor-format] metamorphic violation: input=" + anchorPattern + " got=" + anchorFormatted);
        }
        String anchorBefore = anchor.toPattern();
        String anchorAgain = anchor.format(new Object[] { "DUMMY" });
        String anchorAfter = anchor.toPattern();
        if (!anchorBefore.equals(anchorAfter) || !anchorFormatted.equals(anchorAgain)) {
            throw new RuntimeException("[oracle:anchor-state] metamorphic violation: before=" + anchorBefore + " after=" + anchorAfter + " once=" + anchorFormatted + " twice=" + anchorAgain);
        }

        String left = cleanLiteral(data.consumeAsciiString(20));
        String right = cleanLiteral(data.consumeAsciiString(20));
        String tail = cleanLiteral(data.consumeAsciiString(20));
        String arg = cleanLiteral(data.consumeAsciiString(20));
        if (arg.length() == 0) {
            arg = "DuMmY";
        }

        int variant = data.consumeInt(0, 3);
        String pattern;
        if (variant == 0) {
            pattern = left + "''" + right + " {0,lower} '" + tail + "'";
        } else if (variant == 1) {
            pattern = "''" + left + " {0,lower} '" + tail + "'";
        } else if (variant == 2) {
            pattern = left + " '' " + right + " {0,upper} '" + tail + "'";
        } else {
            pattern = left + "''s {0,lower} '" + tail + "'!";
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String before = emf.toPattern();
        String out1 = emf.format(new Object[] { arg });
        String out2 = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        if (!before.equals(after) || !out1.equals(out2)) {
            throw new RuntimeException("[oracle:determinism] metamorphic violation: pattern=" + pattern + " before=" + before + " after=" + after + " out1=" + out1 + " out2=" + out2);
        }

        ExtendedMessageFormat reparsed = new ExtendedMessageFormat(before, registry);
        String out3 = reparsed.format(new Object[] { arg });
        if (!out1.equals(out3)) {
            throw new RuntimeException("[oracle:roundtrip] metamorphic violation: pattern=" + pattern + " toPattern=" + before + " lhs=" + out1 + " rhs=" + out3);
        }
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~5,923 chars)</summary>

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
            if (source == null) {
                pos.setIndex(0);
                return null;
            }
            pos.setIndex(source.length());
            return source.toLowerCase(Locale.ROOT);
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
            if (source == null) {
                pos.setIndex(0);
                return null;
            }
            pos.setIndex(source.length());
            return source.toUpperCase(Locale.ROOT);
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

    private static String literal(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == ',') {
                sb.append('x');
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = makeRegistry();

        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, registry);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor] metamorphic violation: pattern=" + anchorPattern + " out=" + anchorOut);
        }
        String anchorBefore = anchor.toPattern();
        String anchorAfter = anchor.toPattern();
        if (!anchorBefore.equals(anchorAfter)) {
            throw new RuntimeException("[oracle:anchor-state] metamorphic violation: before=" + anchorBefore + " after=" + anchorAfter);
        }

        String prefix = literal(data.consumeAsciiString(16));
        String middle = literal(data.consumeAsciiString(16));
        String suffix = literal(data.consumeAsciiString(16));
        String arg = literal(data.consumeAsciiString(16));
        if (arg.length() == 0) {
            arg = "DuMmY";
        }

        int variant = data.consumeInt(0, 3);
        String pattern;
        String expected;
        if (variant == 0) {
            pattern = prefix + "''" + middle + " {0,lower} '" + suffix + "'";
            expected = prefix + "'" + middle + " " + arg.toLowerCase(Locale.ROOT) + " " + suffix;
        } else if (variant == 1) {
            pattern = "''" + prefix + " {0,lower} '" + suffix + "'";
            expected = "'" + prefix + " " + arg.toLowerCase(Locale.ROOT) + " " + suffix;
        } else if (variant == 2) {
            pattern = prefix + "''s a {0,lower} '" + suffix + "'!";
            expected = prefix + "'s a " + arg.toLowerCase(Locale.ROOT) + " " + suffix + "!";
        } else {
            pattern = prefix + "''" + middle + " {0,upper} '" + suffix + "'";
            expected = prefix + "'" + middle + " " + arg.toUpperCase(Locale.ROOT) + " " + suffix;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String before = emf.toPattern();
        String out1 = emf.format(new Object[] { arg });
        String out2 = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: pattern=" + pattern + " before=" + before + " after=" + after);
        }
        if (!out1.equals(out2)) {
            throw new RuntimeException("[oracle:determinism] metamorphic violation: pattern=" + pattern + " out1=" + out1 + " out2=" + out2);
        }
        if (!expected.equals(out1)) {
            throw new RuntimeException("[oracle:format] metamorphic violation: pattern=" + pattern + " expected=" + expected + " got=" + out1);
        }

        ExtendedMessageFormat reparsed = new ExtendedMessageFormat(before, registry);
        String out3 = reparsed.format(new Object[] { arg });
        if (!out1.equals(out3)) {
            throw new RuntimeException("[oracle:roundtrip] metamorphic violation: pattern=" + pattern + " toPattern=" + before + " lhs=" + out1 + " rhs=" + out3);
        }
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
<details><summary>▸ Prompt (8 message(s), ~6,361 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [19] — not reprinted*
- *[assistant] message: identical to step [45] — not reprinted*
- *[user] message: identical to step [5] — not reprinted*
- *[assistant] message: identical to step [47] — not reprinted*
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
            if (source == null) {
                pos.setIndex(0);
                return null;
            }
            pos.setIndex(source.length());
            return source.toLowerCase(Locale.ROOT);
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
            if (source == null) {
                pos.setIndex(0);
                return null;
            }
            pos.setIndex(source.length());
            return source.toUpperCase(Locale.ROOT);
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

    private static String literal(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == ',') {
                sb.append('x');
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = makeRegistry();

        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, registry);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor] metamorphic violation: pattern=" + anchorPattern + " out=" + anchorOut);
        }
        String anchorBefore = anchor.toPattern();
        String anchorAfter = anchor.toPattern();
        if (!anchorBefore.equals(anchorAfter)) {
            throw new RuntimeException("[oracle:anchor-state] metamorphic violation: before=" + anchorBefore + " after=" + anchorAfter);
        }

        String prefix = literal(data.consumeAsciiString(16));
        String middle = literal(data.consumeAsciiString(16));
        String suffix = literal(data.consumeAsciiString(16));
        String arg = literal(data.consumeAsciiString(16));
        if (arg.length() == 0) {
            arg = "DuMmY";
        }

        int variant = data.consumeInt(0, 3);
        String pattern;
        String expected;
        if (variant == 0) {
            pattern = prefix + "''" + middle + " {0,lower} '" + suffix + "'";
            expected = prefix + "'" + middle + " " + arg.toLowerCase(Locale.ROOT) + " " + suffix;
        } else if (variant == 1) {
            pattern = "''" + prefix + " {0,lower} '" + suffix + "'";
            expected = "'" + prefix + " " + arg.toLowerCase(Locale.ROOT) + " " + suffix;
        } else if (variant == 2) {
            pattern = prefix + "''s a {0,lower} '" + suffix + "'!";
            expected = prefix + "'s a " + arg.toLowerCase(Locale.ROOT) + " " + suffix + "!";
        } else {
            pattern = prefix + "''" + middle + " {0,upper} '" + suffix + "'";
            expected = prefix + "'" + middle + " " + arg.toUpperCase(Locale.ROOT) + " " + suffix;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String before = emf.toPattern();
        String out1 = emf.format(new Object[] { arg });
        String out2 = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: pattern=" + pattern + " before=" + before + " after=" + after);
        }
        if (!out1.equals(out2)) {
            throw new RuntimeException("[oracle:determinism] metamorphic violation: pattern=" + pattern + " out1=" + out1 + " out2=" + out2);
        }
        if (!expected.equals(out1)) {
            throw new RuntimeException("[oracle:format] metamorphic violation: pattern=" + pattern + " expected=" + expected + " got=" + out1);
        }

        ExtendedMessageFormat reparsed = new ExtendedMessageFormat(before, registry);
        String out3 = reparsed.format(new Object[] { arg });
        if (!out1.equals(out3)) {
            throw new RuntimeException("[oracle:roundtrip] metamorphic violation: pattern=" + pattern + " toPattern=" + before + " lhs=" + out1 + " rhs=" + out3);
        }
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~4,103 chars)</summary>

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

    public static final class LowerCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toLowerCase(Locale.ROOT));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            if (source != null) {
                pos.setIndex(source.length());
                return source.toLowerCase(Locale.ROOT);
            }
            pos.setIndex(0);
            return null;
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
            if (source != null) {
                pos.setIndex(source.length());
                return source.toUpperCase(Locale.ROOT);
            }
            pos.setIndex(0);
            return null;
        }
    }

    private static Map makeRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static String clean(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == ',') {
                sb.append('x');
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = makeRegistry();

        new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);

        String a = clean(data.consumeAsciiString(12));
        String b = clean(data.consumeAsciiString(12));
        String c = clean(data.consumeAsciiString(12));
        if (a.length() == 0) {
            a = "it";
        }
        if (b.length() == 0) {
            b = "s";
        }
        if (c.length() == 0) {
            c = "test";
        }

        int which = data.consumeInt(0, 3);
        String pattern;
        if (which == 0) {
            pattern = a + "''" + b + " a {0,lower} '" + c + "'!";
        } else if (which == 1) {
            pattern = "''" + a + " {0,lower} '" + c + "'!";
        } else if (which == 2) {
            pattern = a + " '' " + b + " {0,lower} '" + c + "'!";
        } else {
            pattern = a + "''s a {0,lower} '" + c + "'!";
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String out = emf.format(new Object[] { "DUMMY" });
        String before = emf.toPattern();
        String after = emf.toPattern();
        if (!before.equals(after) || out == null) {
            throw new RuntimeException("[oracle:stable] metamorphic violation: pattern=" + pattern + " before=" + before + " after=" + after + " out=" + out);
        }
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
<details open><summary>▸ Output (~6,734 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        exercisePattern("it''s a {0,lower} 'test'!", registry, false, null);

        String left = sanitizeLiteral(data.consumeAsciiString(32));
        String right = sanitizeLiteral(data.consumeAsciiString(32));
        String tail = sanitizeLiteral(data.consumeAsciiString(24));

        if (left.length() == 0) {
            left = "a";
        }
        if (right.length() == 0) {
            right = "b";
        }

        StringBuilder pattern = new StringBuilder();
        pattern.append(left);
        if (data.consumeBoolean()) {
            pattern.append(' ');
        }
        pattern.append("''");
        pattern.append(right);
        if (data.consumeBoolean()) {
            pattern.append(" '");
            String quoted = sanitizeQuotedBody(data.consumeAsciiString(16));
            if (quoted.length() == 0) {
                quoted = "q";
            }
            pattern.append(quoted).append('\'');
        }
        if (tail.length() > 0) {
            pattern.append(' ').append(tail);
        }
        pattern.append(" {0}");

        String arg = data.consumeString(24);
        if (arg.length() == 0) {
            arg = "dummy";
        }

        exercisePattern(pattern.toString(), registry, true, arg);
    }

    private static void exercisePattern(String pattern, Map registry, boolean doOracle, String arg) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

            if (!doOracle) {
                return;
            }

            String reportedPattern;
            try {
                reportedPattern = emf.toPattern();
            } catch (Throwable t) {
                if (isCleanRejection(t) || !isRootCause(t)) {
                    return;
                }
                throwUnchecked(t);
                return;
            }

            String formatted1;
            try {
                formatted1 = emf.format(new Object[] { arg });
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throwUnchecked(t);
                }
                return;
            }

            try {
                ExtendedMessageFormat emf2 = new ExtendedMessageFormat(reportedPattern, registry);
                String reportedPattern2 = emf2.toPattern();
                String formatted2 = emf2.format(new Object[] { arg });

                /* Contract: toPattern() exposes the pattern representation of this formatter.
                 * Reconstructing an ExtendedMessageFormat from that reported pattern must preserve
                 * the same observable pattern and formatting behavior. A patch that merely skips
                 * the quote-handling step can stop crashing yet silently change the preserved
                 * pattern or its formatted output, so we assert this round-trip relation. */
                if (!safeEquals(reportedPattern, reportedPattern2)) {
                    throw new RuntimeException("[oracle:toPattern-roundtrip] metamorphic violation: input=" + pattern
                            + " lhs=" + reportedPattern + " rhs=" + reportedPattern2);
                }
                if (!safeEquals(formatted1, formatted2)) {
                    throw new RuntimeException("[oracle:format-roundtrip] metamorphic violation: input=" + pattern
                            + " lhs=" + formatted1 + " rhs=" + formatted2);
                }
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throwUnchecked(t);
                }
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

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName().toLowerCase();
            if (name.contains("validation") || name.contains("invalid") || name.contains("format")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
            String cls = e.getClassName();
            String m = e.getMethodName();
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cls)) {
                if ("appendQuotedString".equals(m) || "next".equals(m) || "append".equals(m) || "startsWith".equals(m)) {
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

    private static String sanitizeLiteral(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == '\'') {
                out.append('x');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static String sanitizeQuotedBody(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'') {
                out.append('q');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
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
## [52] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [53] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~6,931 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        exercisePattern("it''s a {0,lower} 'test'!", registry, false, null);

        String left = sanitizeLiteral(data.consumeAsciiString(32));
        String right = sanitizeLiteral(data.consumeAsciiString(32));
        String tail = sanitizeLiteral(data.consumeAsciiString(24));

        if (left.length() == 0) {
            left = "a";
        }
        if (right.length() == 0) {
            right = "b";
        }

        StringBuilder pattern = new StringBuilder();
        pattern.append(left);
        if (data.consumeBoolean()) {
            pattern.append(' ');
        }
        pattern.append("''");
        pattern.append(right);
        if (data.consumeBoolean()) {
            pattern.append(" '");
            String quoted = sanitizeQuotedBody(data.consumeAsciiString(16));
            if (quoted.length() == 0) {
                quoted = "q";
            }
            pattern.append(quoted).append('\'');
        }
        if (tail.length() > 0) {
            pattern.append(' ').append(tail);
        }
        pattern.append(" {0}");

        String arg = data.consumeString(24);
        if (arg.length() == 0) {
            arg = "dummy";
        }

        exercisePattern(pattern.toString(), registry, true, arg);
    }

    private static void exercisePattern(String pattern, Map registry, boolean doOracle, String arg) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

            if (!doOracle) {
                return;
            }

            String reportedPattern;
            try {
                reportedPattern = emf.toPattern();
            } catch (Throwable t) {
                if (isCleanRejection(t) || !isRootCause(t)) {
                    return;
                }
                throwUnchecked(t);
                return;
            }

            String formatted1;
            try {
                formatted1 = emf.format(new Object[] { arg });
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throwUnchecked(t);
                }
                return;
            }

            try {
                ExtendedMessageFormat emf2 = new ExtendedMessageFormat(reportedPattern, registry);
                String reportedPattern2 = emf2.toPattern();
                String formatted2 = emf2.format(new Object[] { arg });

                /* Contract: toPattern() exposes the pattern representation of this formatter.
                 * Reconstructing an ExtendedMessageFormat from that reported pattern must preserve
                 * the same observable pattern and formatting behavior. A patch that merely skips
                 * the quote-handling step can stop crashing yet silently change the preserved
                 * pattern or its formatted output, so we assert this round-trip relation. */
                if (!safeEquals(reportedPattern, reportedPattern2)) {
                    throw new RuntimeException("[oracle:toPattern-roundtrip] metamorphic violation: input=" + pattern
                            + " lhs=" + reportedPattern + " rhs=" + reportedPattern2);
                }
                if (!safeEquals(formatted1, formatted2)) {
                    throw new RuntimeException("[oracle:format-roundtrip] metamorphic violation: input=" + pattern
                            + " lhs=" + formatted1 + " rhs=" + formatted2);
                }
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throwUnchecked(t);
                }
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

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName().toLowerCase();
            if (name.contains("validation") || name.contains("invalid") || name.contains("format")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
            String cls = e.getClassName();
            String m = e.getMethodName();
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cls)) {
                if ("appendQuotedString".equals(m) || "next".equals(m) || "append".equals(m) || "startsWith".equals(m)) {
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

    private static String sanitizeLiteral(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == '\'') {
                out.append('x');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static String sanitizeQuotedBody(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'') {
                out.append('q');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
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
<details open><summary>▸ Output (~2,028 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);

        String left = sanitize(data.consumeAsciiString(32));
        String right = sanitize(data.consumeAsciiString(32));
        String quoted = sanitize(data.consumeAsciiString(32));

        if (left.length() == 0) {
            left = "it";
        }
        if (right.length() == 0) {
            right = "works";
        }
        if (quoted.length() == 0) {
            quoted = "test";
        }

        String pattern = left + "''" + right + " {0} '" + quoted + "'!";
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

        String p1 = emf.toPattern();
        String formatted1 = emf.format(new Object[] { "dummy" });

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(p1, registry);
        String p2 = emf2.toPattern();
        String formatted2 = emf2.format(new Object[] { "dummy" });

        if (!p1.equals(p2)) {
            throw new RuntimeException("[oracle:toPattern-roundtrip] metamorphic violation: input=" + pattern + " lhs=" + p1 + " rhs=" + p2);
        }
        if (!formatted1.equals(formatted2)) {
            throw new RuntimeException("[oracle:format-roundtrip] metamorphic violation: input=" + pattern + " lhs=" + formatted1 + " rhs=" + formatted2);
        }
    }

    private static String sanitize(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == '\'') {
                out.append('x');
            } else {
                out.append(c);
            }
        }
        return out.toString();
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
<details><summary>▸ Prompt (6 message(s), ~2,345 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
- *[assistant] message: identical to step [53] — not reprinted*
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

        new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);

        String left = sanitize(data.consumeAsciiString(32));
        String right = sanitize(data.consumeAsciiString(32));
        String quoted = sanitize(data.consumeAsciiString(32));

        if (left.length() == 0) {
            left = "it";
        }
        if (right.length() == 0) {
            right = "works";
        }
        if (quoted.length() == 0) {
            quoted = "test";
        }

        String pattern = left + "''" + right + " {0} '" + quoted + "'!";
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

        String p1 = emf.toPattern();
        String formatted1 = emf.format(new Object[] { "dummy" });

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(p1, registry);
        String p2 = emf2.toPattern();
        String formatted2 = emf2.format(new Object[] { "dummy" });

        if (!p1.equals(p2)) {
            throw new RuntimeException("[oracle:toPattern-roundtrip] metamorphic violation: input=" + pattern + " lhs=" + p1 + " rhs=" + p2);
        }
        if (!formatted1.equals(formatted2)) {
            throw new RuntimeException("[oracle:format-roundtrip] metamorphic violation: input=" + pattern + " lhs=" + formatted1 + " rhs=" + formatted2);
        }
    }

    private static String sanitize(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == '\'') {
                out.append('x');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

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

        checkPattern("it''s a {0} 'test'!", "dummy", registry);

        String pre = sanitizeLiteral(data.consumeAsciiString(24));
        String mid = sanitizeLiteral(data.consumeAsciiString(24));
        String quoted = sanitizeLiteral(data.consumeAsciiString(24));
        String post = sanitizeLiteral(data.consumeAsciiString(24));
        String arg = data.consumeString(24);

        if (pre.length() == 0) {
            pre = "it";
        }
        if (mid.length() == 0) {
            mid = "works";
        }
        if (quoted.length() == 0) {
            quoted = "test";
        }
        if (arg.length() == 0) {
            arg = "dummy";
        }

        int escapedQuotes = data.consumeInt(1, 4);
        StringBuilder sb = new StringBuilder();
        sb.append(pre);
        for (int i = 0; i < escapedQuotes; i++) {
            sb.append("''");
            sb.append(mid);
        }
        sb.append(" {0} ");
        sb.append('\'').append(quoted).append('\'');
        sb.append(post);

        checkPattern(sb.toString(), arg, registry);
    }

    private static void checkPattern(String pattern, String arg, Map registry) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        MessageFormat mf = new MessageFormat(pattern);

        String emfFormatted = emf.format(new Object[] { arg });
        String mfFormatted = mf.format(new Object[] { arg });

        if (!emfFormatted.equals(mfFormatted)) {
            throw new RuntimeException("[oracle:messageformat-equivalence] metamorphic violation: input=" + pattern + " lhs=" + emfFormatted + " rhs=" + mfFormatted);
        }

        String emfPattern = emf.toPattern();
        String mfPattern = mf.toPattern();

        if (!emfPattern.equals(mfPattern)) {
            throw new RuntimeException("[oracle:topattern-equivalence] metamorphic violation: input=" + pattern + " lhs=" + emfPattern + " rhs=" + mfPattern);
        }
    }

    private static String sanitizeLiteral(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == '\'') {
                out.append('x');
            } else {
                out.append(c);
            }
        }
        return out.toString();
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
<details><summary>▸ Prompt (8 message(s), ~3,134 chars, 1 new)</summary>

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
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        checkPattern("it''s a {0} 'test'!", "dummy", registry);

        String pre = sanitizeLiteral(data.consumeAsciiString(24));
        String mid = sanitizeLiteral(data.consumeAsciiString(24));
        String quoted = sanitizeLiteral(data.consumeAsciiString(24));
        String post = sanitizeLiteral(data.consumeAsciiString(24));
        String arg = data.consumeString(24);

        if (pre.length() == 0) {
            pre = "it";
        }
        if (mid.length() == 0) {
            mid = "works";
        }
        if (quoted.length() == 0) {
            quoted = "test";
        }
        if (arg.length() == 0) {
            arg = "dummy";
        }

        int escapedQuotes = data.consumeInt(1, 4);
        StringBuilder sb = new StringBuilder();
        sb.append(pre);
        for (int i = 0; i < escapedQuotes; i++) {
            sb.append("''");
            sb.append(mid);
        }
        sb.append(" {0} ");
        sb.append('\'').append(quoted).append('\'');
        sb.append(post);

        checkPattern(sb.toString(), arg, registry);
    }

    private static void checkPattern(String pattern, String arg, Map registry) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        MessageFormat mf = new MessageFormat(pattern);

        String emfFormatted = emf.format(new Object[] { arg });
        String mfFormatted = mf.format(new Object[] { arg });

        if (!emfFormatted.equals(mfFormatted)) {
            throw new RuntimeException("[oracle:messageformat-equivalence] metamorphic violation: input=" + pattern + " lhs=" + emfFormatted + " rhs=" + mfFormatted);
        }

        String emfPattern = emf.toPattern();
        String mfPattern = mf.toPattern();

        if (!emfPattern.equals(mfPattern)) {
            throw new RuntimeException("[oracle:topattern-equivalence] metamorphic violation: input=" + pattern + " lhs=" + emfPattern + " rhs=" + mfPattern);
        }
    }

    private static String sanitizeLiteral(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == '\'') {
                out.append('x');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~2,560 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        String anchor = "it''s a {0,lower} 'test'!";
        new ExtendedMessageFormat(anchor, registry);

        String a = sanitize(data.consumeAsciiString(20));
        String b = sanitize(data.consumeAsciiString(20));
        String c = sanitize(data.consumeAsciiString(20));
        String arg = data.consumeString(20);

        if (a.length() == 0) {
            a = "it";
        }
        if (b.length() == 0) {
            b = "s";
        }
        if (c.length() == 0) {
            c = "test";
        }
        if (arg.length() == 0) {
            arg = "dummy";
        }

        int repeats = data.consumeInt(1, 4);

        StringBuilder patternBuilder = new StringBuilder();
        patternBuilder.append(a);
        for (int i = 0; i < repeats; i++) {
            patternBuilder.append("''");
            patternBuilder.append(b);
        }
        patternBuilder.append(" {0} '");
        patternBuilder.append(c);
        patternBuilder.append("'!");

        String pattern = patternBuilder.toString();

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        MessageFormat mf = new MessageFormat(pattern);

        String emfPattern = emf.toPattern();
        String mfPattern = mf.toPattern();
        if (!emfPattern.equals(mfPattern)) {
            throw new RuntimeException("[oracle:topattern-equivalence] metamorphic violation: input=" + pattern + " lhs=" + emfPattern + " rhs=" + mfPattern);
        }

        String emfFormatted = emf.format(new Object[] { arg });
        String mfFormatted = mf.format(new Object[] { arg });
        if (!emfFormatted.equals(mfFormatted)) {
            throw new RuntimeException("[oracle:format-equivalence] metamorphic violation: input=" + pattern + " lhs=" + emfFormatted + " rhs=" + mfFormatted);
        }
    }

    private static String sanitize(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'') {
                out.append('x');
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
<details open><summary>▸ Output (~5,620 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Collections;
import java.util.Map;

public class FuzzHarness {
    private static final Map EMPTY_REGISTRY = Collections.EMPTY_MAP;
    private static final Object[] FORMAT_ARGS = new Object[] { "DuMmY", Integer.valueOf(7) };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exercise("it''s a {0,lower} 'test'!");

        String a = sanitize(data.consumeAsciiString(24));
        String b = sanitize(data.consumeAsciiString(24));
        String c = sanitize(data.consumeAsciiString(24));
        String d = sanitize(data.consumeAsciiString(24));

        int choice = data.consumeInt(0, 7);
        String pattern;
        switch (choice) {
            case 0:
                pattern = a + "''" + b + " {0}";
                break;
            case 1:
                pattern = "'" + a + "''" + b + "' {0}";
                break;
            case 2:
                pattern = a + " {0} '' " + b;
                break;
            case 3:
                pattern = "''" + a + "{0}" + b;
                break;
            case 4:
                pattern = a + "''" + b + " '{1}' {0}";
                break;
            case 5:
                pattern = a + " '" + b + "''" + c + "' {0}";
                break;
            case 6:
                pattern = a + "''" + b + " {0,number} " + c;
                break;
            default:
                pattern = a + " " + b + "''" + c + " " + d + " {0}";
                break;
        }

        exercise(pattern);
    }

    private static void exercise(String pattern) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, EMPTY_REGISTRY);

            String before = emf.toPattern();
            String out1 = emf.format(FORMAT_ARGS);
            String after = emf.toPattern();

            // MessageFormat.format is observationally read-only for the compiled pattern;
            // deleting/skipping bookkeeping in the patched path must not silently mutate toPattern().
            if (!safeEquals(before, after)) {
                throw new RuntimeException("[oracle:pattern-stable] metamorphic violation: format changed toPattern input="
                        + pattern + " before=" + before + " after=" + after);
            }

            // toPattern() is the object's serialized pattern. Reconstructing a new formatter from
            // that pattern and formatting the same arguments must yield the same result for any
            // correct implementation.
            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, EMPTY_REGISTRY);
            String before2 = emf2.toPattern();
            String out2 = emf2.format(FORMAT_ARGS);

            if (!safeEquals(before, before2) || !safeEquals(out1, out2)) {
                throw new RuntimeException("[oracle:roundtrip] metamorphic violation: parse(formatter-pattern) not stable input="
                        + pattern + " p1=" + before + " p2=" + before2 + " out1=" + out1 + " out2=" + out2);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                rethrow(t);
            }
        }
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
            String cls = st[i].getClassName();
            String method = st[i].getMethodName();
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cls)
                    && ("appendQuotedString".equals(method) || "next".equals(method)
                    || "append".equals(method) || "startsWith".equals(method))) {
                return true;
            }
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                    && ("append".equals(method) || "startsWith".equals(method))) {
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
            if (name.contains("Validation") || name.contains("InvalidFormat")) {
                return true;
            }
        }
        return false;
    }

    private static void rethrow(Throwable t) {
        if (t instanceof Error) {
            throw (Error) t;
        }
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        throw new RuntimeException(t);
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == ',') {
                sb.append('x');
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
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
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [61] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~5,818 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [11] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Collections;
import java.util.Map;

public class FuzzHarness {
    private static final Map EMPTY_REGISTRY = Collections.EMPTY_MAP;
    private static final Object[] FORMAT_ARGS = new Object[] { "DuMmY", Integer.valueOf(7) };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exercise("it''s a {0,lower} 'test'!");

        String a = sanitize(data.consumeAsciiString(24));
        String b = sanitize(data.consumeAsciiString(24));
        String c = sanitize(data.consumeAsciiString(24));
        String d = sanitize(data.consumeAsciiString(24));

        int choice = data.consumeInt(0, 7);
        String pattern;
        switch (choice) {
            case 0:
                pattern = a + "''" + b + " {0}";
                break;
            case 1:
                pattern = "'" + a + "''" + b + "' {0}";
                break;
            case 2:
                pattern = a + " {0} '' " + b;
                break;
            case 3:
                pattern = "''" + a + "{0}" + b;
                break;
            case 4:
                pattern = a + "''" + b + " '{1}' {0}";
                break;
            case 5:
                pattern = a + " '" + b + "''" + c + "' {0}";
                break;
            case 6:
                pattern = a + "''" + b + " {0,number} " + c;
                break;
            default:
                pattern = a + " " + b + "''" + c + " " + d + " {0}";
                break;
        }

        exercise(pattern);
    }

    private static void exercise(String pattern) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, EMPTY_REGISTRY);

            String before = emf.toPattern();
            String out1 = emf.format(FORMAT_ARGS);
            String after = emf.toPattern();

            // MessageFormat.format is observationally read-only for the compiled pattern;
            // deleting/skipping bookkeeping in the patched path must not silently mutate toPattern().
            if (!safeEquals(before, after)) {
                throw new RuntimeException("[oracle:pattern-stable] metamorphic violation: format changed toPattern input="
                        + pattern + " before=" + before + " after=" + after);
            }

            // toPattern() is the object's serialized pattern. Reconstructing a new formatter from
            // that pattern and formatting the same arguments must yield the same result for any
            // correct implementation.
            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, EMPTY_REGISTRY);
            String before2 = emf2.toPattern();
            String out2 = emf2.format(FORMAT_ARGS);

            if (!safeEquals(before, before2) || !safeEquals(out1, out2)) {
                throw new RuntimeException("[oracle:roundtrip] metamorphic violation: parse(formatter-pattern) not stable input="
                        + pattern + " p1=" + before + " p2=" + before2 + " out1=" + out1 + " out2=" + out2);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                rethrow(t);
            }
        }
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
            String cls = st[i].getClassName();
            String method = st[i].getMethodName();
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cls)
                    && ("appendQuotedString".equals(method) || "next".equals(method)
                    || "append".equals(method) || "startsWith".equals(method))) {
                return true;
            }
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                    && ("append".equals(method) || "startsWith".equals(method))) {
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
            if (name.contains("Validation") || name.contains("InvalidFormat")) {
                return true;
            }
        }
        return false;
    }

    private static void rethrow(Throwable t) {
        if (t instanceof Error) {
            throw (Error) t;
        }
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        throw new RuntimeException(t);
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == ',') {
                sb.append('x');
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~3,505 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.Format;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    private static final class LowerCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toLowerCase());
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }

    private static final class UpperCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toUpperCase());
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
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

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);

        String prefix = sanitize(data.consumeAsciiString(16));
        String mid = sanitize(data.consumeAsciiString(16));
        String suffix = sanitize(data.consumeAsciiString(16));
        boolean quotedTail = data.consumeBoolean();
        boolean useLower = data.consumeBoolean();

        StringBuilder pattern = new StringBuilder();
        if (prefix.length() > 0) {
            pattern.append(prefix);
        } else {
            pattern.append('a');
        }
        pattern.append("''");
        pattern.append(mid.length() > 0 ? mid : "b");
        pattern.append(" {0,");
        pattern.append(useLower ? "lower" : "upper");
        pattern.append("} ");
        if (quotedTail) {
            pattern.append('\'');
            pattern.append(suffix.length() > 0 ? suffix : "test");
            pattern.append('\'');
        } else {
            pattern.append(suffix.length() > 0 ? suffix : "test");
        }

        new ExtendedMessageFormat(pattern.toString(), registry);
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == ',') {
                out.append('x');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
```

</details>

---
## [62] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [63] ⚙️ coverage · `jazzer coverage dumps`
**output:** **0/30 dump(s) written under /datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/cov; 30 raw fuzz log(s)**
<details><summary>▸ detail (~14,688 chars)</summary>

- detail: {'dumps': [{'harness': 'attempt_001', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_001_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_002', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_002_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_003', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_003_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_004', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_004_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_005', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_005_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_006', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_006_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_007', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_007_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_008', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_008_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_009', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_009_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_010', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_010_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_011', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_011_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_012', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_012_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_013', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_013_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_014', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_014_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_015', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_015_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_016', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_016_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_017', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_017_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_018', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_018_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_019', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_019_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_020', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_020_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_021', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_021_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_022', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_022_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_023', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_023_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_024', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_024_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_025', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_025_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_026', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_026_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_027', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_027_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_028', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_028_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_029', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_029_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_030', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_030_compiled.exec', 'exists': False, 'bytes': 0}], 'outputs': ['/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_001_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_002_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_003_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_004_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_005_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_006_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_007_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_008_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_009_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_010_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_011_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_012_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_013_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_014_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_015_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_016_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_017_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_018_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_019_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_020_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_021_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_022_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_023_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_024_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_025_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_026_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_027_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_028_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_029_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_030_compiled.txt'], 'classpath': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/cov/classpath.json', 'dir': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/cov', 'compiled_attempts': ['attempt_001', 'attempt_002', 'attempt_003', 'attempt_004', 'attempt_005', 'attempt_006', 'attempt_007', 'attempt_008', 'attempt_009', 'attempt_010', 'attempt_011', 'attempt_012', 'attempt_013', 'attempt_014', 'attempt_015', 'attempt_016', 'attempt_017', 'attempt_018', 'attempt_019', 'attempt_020', 'attempt_021', 'attempt_022', 'attempt_023', 'attempt_024', 'attempt_025', 'attempt_026', 'attempt_027', 'attempt_028', 'attempt_029', 'attempt_030'], 'accepted_attempts': [], 'sources': ['/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_001.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_002.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_003.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_004.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_005.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_006.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_007.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_008.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_009.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_010.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_011.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_012.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_013.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_014.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_015.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_016.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_017.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_018.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_019.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_020.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_021.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_022.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_023.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_024.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_025.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_026.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_027.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_028.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_029.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_030.java']}

</details>
