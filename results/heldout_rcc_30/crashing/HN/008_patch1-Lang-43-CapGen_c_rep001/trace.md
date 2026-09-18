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
  "FailureTest(test_class='org.apache.commons.lang.text.ExtendedMessageFormatTest', test_method='testEscapedQuote_LANG_477', source_path='/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/Lang_43_buggy/src/test/org/apache/commons/lang/text/ExtendedMessageFormatTest.java', method_source='    public void testEscapedQuote_LANG_477() {\\n        String pattern = \"it\\'\\'s a {0,lower} \\'test\\'!\";\\n        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);\\n        assertEquals(\"it\\'s a dummy test!\", emf.format(new Object[] {\"DUMMY\"}));\\n    }', exception_type='java.lang.OutOfMemoryError', failure_message=None, support_source=None)"
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
<details><summary>▸ Prompt (2 message(s), ~17,316 chars)</summary>

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

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `ExtendedMessageFormat`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.OutOfMemoryError (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `appendQuotedString`. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
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
<details open><summary>▸ Output (~6,609 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        // ANCHOR: exact failing-test pattern first, through the real public API.
        // We cannot register the test-only factories here, so we still drive the same constructor path
        // and accept clean rejection if this build reaches registry validation before the bug.
        exerciseForCrashOnly("it''s a {0,lower} 'test'!", registry);

        // EXPLORE: generate many valid-by-construction MessageFormat-compatible patterns that contain:
        // - an escaped quote ("''"), which reaches the escapingOn branch, and
        // - a quoted literal section ("'x'"), which exercises quoted-string parsing.
        //
        // Contract asserted below:
        // For patterns without custom registry formats, ExtendedMessageFormat is a MessageFormat extension,
        // so formatting the same valid pattern/arguments must match MessageFormat. A patch that merely skips
        // quote-handling bookkeeping can avoid the crash but silently produce a different parse/format result.
        String a = visibleAscii(data.consumeAsciiString(12));
        String b = visibleAscii(data.consumeAsciiString(12));
        String c = visibleAscii(data.consumeAsciiString(12));
        String d = visibleAscii(data.consumeAsciiString(12));
        String arg = visibleAscii(data.consumeAsciiString(12));

        int variant = data.consumeInt(0, 5);
        String pattern;
        switch (variant) {
            case 0:
                pattern = a + "''" + b + " {0} '" + c + "'" + d;
                break;
            case 1:
                pattern = "'" + a + "' " + b + "''" + c + " {0}";
                break;
            case 2:
                pattern = a + " {0} '' " + b + " '" + c + "'";
                break;
            case 3:
                pattern = a + "''" + b + " '{0}' " + c + " {0}";
                break;
            case 4:
                pattern = "'" + a + "''" + b + "' {0} " + c;
                break;
            default:
                pattern = a + " '' " + b + " {0} '" + c + "' " + d;
                break;
        }

        exerciseWithOracle(pattern, registry, arg);

        // A second exploration family with some surrounding fuzz noise while preserving validity.
        String tail = visibleAscii(data.consumeAsciiString(10));
        String pattern2 = visibleAscii(data.consumeAsciiString(8)) + "''"
                + visibleAscii(data.consumeAsciiString(8)) + " {0} '"
                + visibleAscii(data.consumeAsciiString(8)) + "' "
                + tail;
        exerciseWithOracle(pattern2, registry, arg);
    }

    private static void exerciseForCrashOnly(String pattern, Map registry) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
            emf.format(new Object[] {"DUMMY"});
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static void exerciseWithOracle(String pattern, Map registry, String arg) {
        try {
            MessageFormat mf = new MessageFormat(pattern);
            String expected = mf.format(new Object[] {arg});

            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
            String before = emf.toPattern();
            String actual = emf.format(new Object[] {arg});
            String after = emf.toPattern();

            if (!before.equals(after)) {
                throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: toPattern changed across format input="
                        + pattern + " before=" + before + " after=" + after);
            }
            if (!expected.equals(actual)) {
                throw new RuntimeException("[oracle:mf-equiv] metamorphic violation: ExtendedMessageFormat output differs from MessageFormat input="
                        + pattern + " lhs=" + actual + " rhs=" + expected);
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && t.getMessage() != null
                    && t.getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(e.getClassName())
                    && "appendQuotedString".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Validation")
                || name.contains("Invalid")
                || name.contains("Malformed")
                || name.contains("FormatException");
    }

    private static String visibleAscii(String s) {
        if (s == null || s.length() == 0) {
            return "X";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\'' || ch == '{' || ch == '}') {
                sb.append('X');
            } else if (ch <= 0x20 || ch >= 0x7f) {
                sb.append('Y');
            } else {
                sb.append(ch);
            }
        }
        if (sb.length() == 0) {
            sb.append('Z');
        }
        return sb.toString();
    }

    private static void throwUnchecked(Throwable t) {
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
## [4] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [5] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~9,575 chars, 2 new)</summary>

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
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        // ANCHOR: exact failing-test pattern first, through the real public API.
        // We cannot register the test-only factories here, so we still drive the same constructor path
        // and accept clean rejection if this build reaches registry validation before the bug.
        exerciseForCrashOnly("it''s a {0,lower} 'test'!", registry);

        // EXPLORE: generate many valid-by-construction MessageFormat-compatible patterns that contain:
        // - an escaped quote ("''"), which reaches the escapingOn branch, and
        // - a quoted literal section ("'x'"), which exercises quoted-string parsing.
        //
        // Contract asserted below:
        // For patterns without custom registry formats, ExtendedMessageFormat is a MessageFormat extension,
        // so formatting the same valid pattern/arguments must match MessageFormat. A patch that merely skips
        // quote-handling bookkeeping can avoid the crash but silently produce a different parse/format result.
        String a = visibleAscii(data.consumeAsciiString(12));
        String b = visibleAscii(data.consumeAsciiString(12));
        String c = visibleAscii(data.consumeAsciiString(12));
        String d = visibleAscii(data.consumeAsciiString(12));
        String arg = visibleAscii(data.consumeAsciiString(12));

        int variant = data.consumeInt(0, 5);
        String pattern;
        switch (variant) {
            case 0:
                pattern = a + "''" + b + " {0} '" + c + "'" + d;
                break;
            case 1:
                pattern = "'" + a + "' " + b + "''" + c + " {0}";
                break;
            case 2:
                pattern = a + " {0} '' " + b + " '" + c + "'";
                break;
            case 3:
                pattern = a + "''" + b + " '{0}' " + c + " {0}";
                break;
            case 4:
                pattern = "'" + a + "''" + b + "' {0} " + c;
                break;
            default:
                pattern = a + " '' " + b + " {0} '" + c + "' " + d;
                break;
        }

        exerciseWithOracle(pattern, registry, arg);

        // A second exploration family with some surrounding fuzz noise while preserving validity.
        String tail = visibleAscii(data.consumeAsciiString(10));
        String pattern2 = visibleAscii(data.consumeAsciiString(8)) + "''"
                + visibleAscii(data.consumeAsciiString(8)) + " {0} '"
                + visibleAscii(data.consumeAsciiString(8)) + "' "
                + tail;
        exerciseWithOracle(pattern2, registry, arg);
    }

    private static void exerciseForCrashOnly(String pattern, Map registry) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
            emf.format(new Object[] {"DUMMY"});
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static void exerciseWithOracle(String pattern, Map registry, String arg) {
        try {
            MessageFormat mf = new MessageFormat(pattern);
            String expected = mf.format(new Object[] {arg});

            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
            String before = emf.toPattern();
            String actual = emf.format(new Object[] {arg});
            String after = emf.toPattern();

            if (!before.equals(after)) {
                throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: toPattern changed across format input="
                        + pattern + " before=" + before + " after=" + after);
            }
            if (!expected.equals(actual)) {
                throw new RuntimeException("[oracle:mf-equiv] metamorphic violation: ExtendedMessageFormat output differs from MessageFormat input="
                        + pattern + " lhs=" + actual + " rhs=" + expected);
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && t.getMessage() != null
                    && t.getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(e.getClassName())
                    && "appendQuotedString".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Validation")
                || name.contains("Invalid")
                || name.contains("Malformed")
                || name.contains("FormatException");
    }

    private static String visibleAscii(String s) {
        if (s == null || s.length() == 0) {
            return "X";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\'' || ch == '{' || ch == '}') {
                sb.append('X');
            } else if (ch <= 0x20 || ch >= 0x7f) {
                sb.append('Y');
            } else {
                sb.append(ch);
            }
        }
        if (sb.length() == 0) {
            sb.append('Z');
        }
        return sb.toString();
    }

    private static void throwUnchecked(Throwable t) {
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
<details open><summary>▸ Output (~3,956 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("nonempty", "nonempty");

        // ANCHOR: same quote structure as the regression test, but with a standard {0} format so the
        // input is valid without any custom test-only FormatFactory. A non-empty registry forces
        // ExtendedMessageFormat down its extended parsing path, which reaches appendQuotedString.
        runOne("it''s a {0} 'test'!", registry, "DUMMY");

        // EXPLORE: the fixed line advances ParsePosition before checking the opening quote.
        // So generate valid MessageFormat patterns that start a quoted-string parse on a quote
        // immediately after an escaped quote sequence, with surrounding text and a real {0}.
        String p1 = atom(data.consumeAsciiString(10));
        String p2 = atom(data.consumeAsciiString(10));
        String p3 = atom(data.consumeAsciiString(10));
        String p4 = atom(data.consumeAsciiString(10));
        String arg = atom(data.consumeString(16));

        int variant = data.consumeInt(0, 5);
        String pattern;
        switch (variant) {
            case 0:
                pattern = p1 + "''" + p2 + " {0} '" + p3 + "' " + p4;
                break;
            case 1:
                pattern = p1 + "'' " + p2 + " '{"+ "0" + "}' {0} " + p3;
                break;
            case 2:
                pattern = p1 + " {0} " + p2 + "''" + p3 + " '" + p4 + "'";
                break;
            case 3:
                pattern = "'" + p1 + "' " + p2 + "''" + p3 + " {0}";
                break;
            case 4:
                pattern = p1 + "''" + p2 + " " + p3 + " {0} '" + p4 + "'!";
                break;
            default:
                pattern = p1 + "''" + p2 + " {0} '" + p3 + "' '" + p4 + "'";
                break;
        }

        runOne(pattern, registry, arg);
    }

    private static void runOne(String pattern, Map registry, String arg) {
        // Contract/oracle: with no custom formats used in the pattern, ExtendedMessageFormat must
        // behave like MessageFormat on the same valid pattern and arguments. A bogus "fix" that
        // simply skips quoted-string handling could avoid the crash but produce a different result.
        MessageFormat mf = new MessageFormat(pattern);
        String expected = mf.format(new Object[] { arg });

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String before = emf.toPattern();
        String actual = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: toPattern changed input="
                    + pattern + " before=" + before + " after=" + after);
        }
        if (!expected.equals(actual)) {
            throw new RuntimeException("[oracle:mf-equiv] metamorphic violation: MessageFormat equivalence input="
                    + pattern + " lhs=" + actual + " rhs=" + expected);
        }
    }

    private static String atom(String s) {
        if (s == null || s.length() == 0) {
            return "X";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\'' || ch == '{' || ch == '}') {
                sb.append('X');
            } else if (Character.isISOControl(ch)) {
                sb.append('Y');
            } else {
                sb.append(ch);
            }
        }
        if (sb.length() == 0) {
            sb.append('Z');
        }
        return sb.toString();
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
<details><summary>▸ Prompt (6 message(s), ~4,272 chars, 1 new)</summary>

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
        registry.put("nonempty", "nonempty");

        // ANCHOR: same quote structure as the regression test, but with a standard {0} format so the
        // input is valid without any custom test-only FormatFactory. A non-empty registry forces
        // ExtendedMessageFormat down its extended parsing path, which reaches appendQuotedString.
        runOne("it''s a {0} 'test'!", registry, "DUMMY");

        // EXPLORE: the fixed line advances ParsePosition before checking the opening quote.
        // So generate valid MessageFormat patterns that start a quoted-string parse on a quote
        // immediately after an escaped quote sequence, with surrounding text and a real {0}.
        String p1 = atom(data.consumeAsciiString(10));
        String p2 = atom(data.consumeAsciiString(10));
        String p3 = atom(data.consumeAsciiString(10));
        String p4 = atom(data.consumeAsciiString(10));
        String arg = atom(data.consumeString(16));

        int variant = data.consumeInt(0, 5);
        String pattern;
        switch (variant) {
            case 0:
                pattern = p1 + "''" + p2 + " {0} '" + p3 + "' " + p4;
                break;
            case 1:
                pattern = p1 + "'' " + p2 + " '{"+ "0" + "}' {0} " + p3;
                break;
            case 2:
                pattern = p1 + " {0} " + p2 + "''" + p3 + " '" + p4 + "'";
                break;
            case 3:
                pattern = "'" + p1 + "' " + p2 + "''" + p3 + " {0}";
                break;
            case 4:
                pattern = p1 + "''" + p2 + " " + p3 + " {0} '" + p4 + "'!";
                break;
            default:
                pattern = p1 + "''" + p2 + " {0} '" + p3 + "' '" + p4 + "'";
                break;
        }

        runOne(pattern, registry, arg);
    }

    private static void runOne(String pattern, Map registry, String arg) {
        // Contract/oracle: with no custom formats used in the pattern, ExtendedMessageFormat must
        // behave like MessageFormat on the same valid pattern and arguments. A bogus "fix" that
        // simply skips quoted-string handling could avoid the crash but produce a different result.
        MessageFormat mf = new MessageFormat(pattern);
        String expected = mf.format(new Object[] { arg });

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String before = emf.toPattern();
        String actual = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: toPattern changed input="
                    + pattern + " before=" + before + " after=" + after);
        }
        if (!expected.equals(actual)) {
            throw new RuntimeException("[oracle:mf-equiv] metamorphic violation: MessageFormat equivalence input="
                    + pattern + " lhs=" + actual + " rhs=" + expected);
        }
    }

    private static String atom(String s) {
        if (s == null || s.length() == 0) {
            return "X";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\'' || ch == '{' || ch == '}') {
                sb.append('X');
            } else if (Character.isISOControl(ch)) {
                sb.append('Y');
            } else {
                sb.append(ch);
            }
        }
        if (sb.length() == 0) {
            sb.append('Z');
        }
        return sb.toString();
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~5,186 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        // ANCHOR: exact regression-test input and setup first.
        ExtendedMessageFormat anchor = new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: exact regression output input=it''s a {0,lower} 'test'! lhs="
                    + anchorOut + " rhs=it's a dummy test!");
        }
        String anchorBefore = anchor.toPattern();
        String anchorAgain = anchor.format(new Object[] { "DUMMY" });
        String anchorAfter = anchor.toPattern();
        if (!anchorBefore.equals(anchorAfter) || !anchorOut.equals(anchorAgain)) {
            throw new RuntimeException("[oracle:anchor-stable] metamorphic violation: formatting changed observable state input=it''s a {0,lower} 'test'! before="
                    + anchorBefore + " after=" + anchorAfter + " once=" + anchorOut + " twice=" + anchorAgain);
        }

        // EXPLORE: target the fixed/buggy behavioral difference.
        // The bug fires when appendQuotedString is entered with escapingOn=true at a quote from a doubled quote ("''").
        // So always build valid patterns containing:
        //   1) a doubled quote before a real custom format element {0,lower}/{0,upper}
        //   2) a normal quoted literal later in the pattern
        // This keeps inputs valid-by-construction and drives the real ExtendedMessageFormat parser.
        String a = atom(data.consumeAsciiString(8));
        String b = atom(data.consumeAsciiString(8));
        String lit = atom(data.consumeAsciiString(8));
        String tail = atom(data.consumeAsciiString(8));
        String arg = nonEmpty(data.consumeString(16));

        boolean lower = data.consumeBoolean();
        String fmt = lower ? "lower" : "upper";

        String pattern;
        switch (data.consumeInt(0, 5)) {
            case 0:
                pattern = a + "''" + b + " {0," + fmt + "} '" + lit + "' " + tail;
                break;
            case 1:
                pattern = a + "''" + b + " x {0," + fmt + "} '" + lit + "'!";
                break;
            case 2:
                pattern = "Q " + a + "''" + b + " {0," + fmt + "} '" + lit + "' " + tail;
                break;
            case 3:
                pattern = a + "''s " + b + " {0," + fmt + "} '" + lit + "'";
                break;
            case 4:
                pattern = a + "''" + b + " {0," + fmt + "} '" + lit + "' " + tail + " end";
                break;
            default:
                pattern = a + "''" + b + " {0," + fmt + "} '" + lit + "'?";
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String before = emf.toPattern();
        String actual = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: formatting mutated toPattern input="
                    + pattern + " before=" + before + " after=" + after);
        }

        // Oracle: for these known test fixtures, {0,lower}/{0,upper} is equivalent to formatting the
        // transformed argument with plain MessageFormat's {0} on the same surrounding pattern.
        String plainPattern = pattern.replace("," + fmt, "");
        String transformedArg = lower ? arg.toLowerCase() : arg.toUpperCase();
        String expected = new MessageFormat(plainPattern).format(new Object[] { transformedArg });

        if (!expected.equals(actual)) {
            throw new RuntimeException("[oracle:factory-equiv] metamorphic violation: custom factory result differs from equivalent plain MessageFormat input="
                    + pattern + " arg=" + arg + " lhs=" + actual + " rhs=" + expected);
        }
    }

    private static String atom(String s) {
        if (s == null || s.length() == 0) {
            return "X";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\'' || ch == '{' || ch == '}') {
                sb.append('X');
            } else if (Character.isISOControl(ch)) {
                sb.append('Y');
            } else {
                sb.append(ch);
            }
        }
        if (sb.length() == 0) {
            sb.append('Z');
        }
        return sb.toString();
    }

    private static String nonEmpty(String s) {
        if (s == null || s.length() == 0) {
            return "DUMMY";
        }
        return s;
    }
}
```

</details>

---
## [8] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [9] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (8 message(s), ~7,102 chars, 2 new)</summary>

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
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        // ANCHOR: exact regression-test input and setup first.
        ExtendedMessageFormat anchor = new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: exact regression output input=it''s a {0,lower} 'test'! lhs="
                    + anchorOut + " rhs=it's a dummy test!");
        }
        String anchorBefore = anchor.toPattern();
        String anchorAgain = anchor.format(new Object[] { "DUMMY" });
        String anchorAfter = anchor.toPattern();
        if (!anchorBefore.equals(anchorAfter) || !anchorOut.equals(anchorAgain)) {
            throw new RuntimeException("[oracle:anchor-stable] metamorphic violation: formatting changed observable state input=it''s a {0,lower} 'test'! before="
                    + anchorBefore + " after=" + anchorAfter + " once=" + anchorOut + " twice=" + anchorAgain);
        }

        // EXPLORE: target the fixed/buggy behavioral difference.
        // The bug fires when appendQuotedString is entered with escapingOn=true at a quote from a doubled quote ("''").
        // So always build valid patterns containing:
        //   1) a doubled quote before a real custom format element {0,lower}/{0,upper}
        //   2) a normal quoted literal later in the pattern
        // This keeps inputs valid-by-construction and drives the real ExtendedMessageFormat parser.
        String a = atom(data.consumeAsciiString(8));
        String b = atom(data.consumeAsciiString(8));
        String lit = atom(data.consumeAsciiString(8));
        String tail = atom(data.consumeAsciiString(8));
        String arg = nonEmpty(data.consumeString(16));

        boolean lower = data.consumeBoolean();
        String fmt = lower ? "lower" : "upper";

        String pattern;
        switch (data.consumeInt(0, 5)) {
            case 0:
                pattern = a + "''" + b + " {0," + fmt + "} '" + lit + "' " + tail;
                break;
            case 1:
                pattern = a + "''" + b + " x {0," + fmt + "} '" + lit + "'!";
                break;
            case 2:
                pattern = "Q " + a + "''" + b + " {0," + fmt + "} '" + lit + "' " + tail;
                break;
            case 3:
                pattern = a + "''s " + b + " {0," + fmt + "} '" + lit + "'";
                break;
            case 4:
                pattern = a + "''" + b + " {0," + fmt + "} '" + lit + "' " + tail + " end";
                break;
            default:
                pattern = a + "''" + b + " {0," + fmt + "} '" + lit + "'?";
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String before = emf.toPattern();
        String actual = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: formatting mutated toPattern input="
                    + pattern + " before=" + before + " after=" + after);
        }

        // Oracle: for these known test fixtures, {0,lower}/{0,upper} is equivalent to formatting the
        // transformed argument with plain MessageFormat's {0} on the same surrounding pattern.
        String plainPattern = pattern.replace("," + fmt, "");
        String transformedArg = lower ? arg.toLowerCase() : arg.toUpperCase();
        String expected = new MessageFormat(plainPattern).format(new Object[] { transformedArg });

        if (!expected.equals(actual)) {
            throw new RuntimeException("[oracle:factory-equiv] metamorphic violation: custom factory result differs from equivalent plain MessageFormat input="
                    + pattern + " arg=" + arg + " lhs=" + actual + " rhs=" + expected);
        }
    }

    private static String atom(String s) {
        if (s == null || s.length() == 0) {
            return "X";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\'' || ch == '{' || ch == '}') {
                sb.append('X');
            } else if (Character.isISOControl(ch)) {
                sb.append('Y');
            } else {
                sb.append(ch);
            }
        }
        if (sb.length() == 0) {
            sb.append('Z');
        }
        return sb.toString();
    }

    private static String nonEmpty(String s) {
        if (s == null || s.length() == 0) {
            return "DUMMY";
        }
        return s;
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/Lang_43_buggy/fuzz/attempt_003/FuzzHarness.java:11: error: cannot find symbol
        registry.put("lower", new LowerCaseFormatFactory());
                                  ^
  symbol:   class LowerCaseFormatFactory
  location: class FuzzHarness
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/Lang_43_buggy/fuzz/attempt_003/FuzzHarness.java:12: error: cannot find symbol
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
<details open><summary>▸ Output (~3,410 chars)</summary>

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
        checkPattern(anchorPattern, registry, "DUMMY");

        String a = atom(data.consumeAsciiString(8));
        String b = atom(data.consumeAsciiString(8));
        String c = atom(data.consumeAsciiString(8));
        String d = atom(data.consumeAsciiString(8));
        String arg = nonEmpty(data.consumeString(16));

        String pattern;
        switch (data.consumeInt(0, 5)) {
            case 0:
                pattern = a + "''" + b + " {0} '" + c + "' " + d;
                break;
            case 1:
                pattern = a + "''s " + b + " {0} '" + c + "'!";
                break;
            case 2:
                pattern = "'" + a + "' " + b + "''" + c + " {0}";
                break;
            case 3:
                pattern = a + " {0} " + b + "''" + c + " '" + d + "'";
                break;
            case 4:
                pattern = a + "''" + b + " x {0} 'y" + c + "'";
                break;
            default:
                pattern = a + "''" + b + " {0} '" + c + "' '" + d + "'";
                break;
        }

        checkPattern(pattern, registry, arg);
    }

    private static void checkPattern(String pattern, Map registry, String arg) {
        MessageFormat mf = new MessageFormat(pattern);
        String expected = mf.format(new Object[] { arg });

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String before = emf.toPattern();
        String actual = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        // Contract: formatting should not mutate the parsed pattern's textual form.
        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: formatting changed toPattern input="
                    + pattern + " before=" + before + " after=" + after);
        }

        // Contract: with no custom format elements used, ExtendedMessageFormat must behave like MessageFormat.
        if (!expected.equals(actual)) {
            throw new RuntimeException("[oracle:mf-equiv] metamorphic violation: ExtendedMessageFormat differs from MessageFormat input="
                    + pattern + " lhs=" + actual + " rhs=" + expected);
        }
    }

    private static String atom(String s) {
        if (s == null || s.length() == 0) {
            return "X";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\'' || ch == '{' || ch == '}') {
                sb.append('X');
            } else if (Character.isISOControl(ch)) {
                sb.append('Y');
            } else {
                sb.append(ch);
            }
        }
        if (sb.length() == 0) {
            sb.append('Z');
        }
        return sb.toString();
    }

    private static String nonEmpty(String s) {
        if (s == null || s.length() == 0) {
            return "DUMMY";
        }
        return s;
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
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,850 chars)</summary>

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
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        Map registry = makeRegistry();

        String anchorPattern = "it''s a {0,lower} 'test'!";
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(anchorPattern, registry);
            String before = emf.toPattern();
            String out = emf.format(new Object[] { "DUMMY" });
            String after = emf.toPattern();

            if (!anchorPattern.equals(before) || !anchorPattern.equals(after) || !"it's a dummy test!".equals(out)) {
                throw new RuntimeException(
                    "[oracle:anchor-format] metamorphic violation: known valid seed must preserve pattern and format output input="
                        + anchorPattern + " before=" + before + " after=" + after + " out=" + out);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }

        String exploredPattern = buildPattern(data);
        try {
            ExtendedMessageFormat emf1 = new ExtendedMessageFormat(exploredPattern, registry);
            String p1 = emf1.toPattern();
            String out1 = emf1.format(new Object[] { pickArg(data) });

            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(p1, registry);
            String p2 = emf2.toPattern();
            String out2 = emf2.format(new Object[] { pickArg(data) });

            /*
             * Contract/oracle: toPattern() is the public reader for the format's pattern.
             * Reconstructing an ExtendedMessageFormat from its own toPattern() with the same
             * registry must preserve the represented format. A throw-deleting/branch-skipping
             * patch in quoted-string handling can silently corrupt parsing/bookkeeping while
             * avoiding a crash; this observable round-trip catches that.
             */
            String oracleArg = sanitizeLiteral(data.consumeString(12));
            if (oracleArg.length() == 0) {
                oracleArg = "x";
            }
            String lhs = emf1.format(new Object[] { oracleArg });
            String rhs = emf2.format(new Object[] { oracleArg });
            if (!safeEquals(lhs, rhs) || !safeEquals(p1, p2)) {
                throw new RuntimeException(
                    "[oracle:pattern-roundtrip] metamorphic violation: reconstructing from toPattern must preserve behavior input="
                        + exploredPattern + " p1=" + p1 + " p2=" + p2 + " lhs=" + lhs + " rhs=" + rhs);
            }

            if (out1 == null || out2 == null) {
                throw new RuntimeException(
                    "[oracle:null-output] metamorphic violation: valid formatting produced null input="
                        + exploredPattern + " out1=" + out1 + " out2=" + out2);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }
    }

    private static Map makeRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static String buildPattern(FuzzedDataProvider data) {
        String prefix = sanitizeLiteral(data.consumeAsciiString(16));
        String infix = sanitizeLiteral(data.consumeAsciiString(16));
        String quoted = sanitizeLiteral(data.consumeAsciiString(16));
        String suffix = sanitizeLiteral(data.consumeAsciiString(16));

        if (prefix.length() == 0) {
            prefix = "a";
        }
        if (infix.length() == 0) {
            infix = "b";
        }
        if (quoted.length() == 0) {
            quoted = "q";
        }
        if (suffix.length() == 0) {
            suffix = "z";
        }

        String formatName = data.consumeBoolean() ? "lower" : "upper";
        int variant = data.consumeInt(0, 5);

        if (variant == 0) {
            return prefix + "''" + infix + " {0," + formatName + "} '" + quoted + "' " + suffix;
        } else if (variant == 1) {
            return "'" + quoted + "' " + prefix + "''" + infix + " {0," + formatName + "} " + suffix;
        } else if (variant == 2) {
            return prefix + " {0," + formatName + "} " + infix + " '' " + suffix + " '" + quoted + "'";
        } else if (variant == 3) {
            return prefix + "''" + infix + "{0," + formatName + "}'" + quoted + "'" + suffix;
        } else if (variant == 4) {
            return prefix + " '" + quoted + "' " + infix + " {0," + formatName + "} " + suffix + "''";
        } else {
            return prefix + "''s " + infix + " {0," + formatName + "} '" + quoted + "'!" + suffix;
        }
    }

    private static String sanitizeLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == '\'') {
                b.append('x');
            } else if (Character.isISOControl(c)) {
                b.append(' ');
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }

    private static String pickArg(FuzzedDataProvider data) {
        String s = sanitizeLiteral(data.consumeString(20));
        if (s.length() == 0) {
            s = "dummy";
        }
        return s;
    }

    private static void handleThrowable(Throwable t) {
        if (isRootCause(t)) {
            throw (OutOfMemoryError) t;
        }
        if (isCleanRejection(t)) {
            return;
        }
        if (t instanceof RuntimeException) {
            return;
        }
        if (t instanceof Error) {
            return;
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(e.getClassName())
                && "appendQuotedString".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException) {
            return true;
        }
        if (t instanceof NumberFormatException) {
            return true;
        }
        String n = t.getClass().getName();
        return n.contains("Validation") || n.contains("Invalid");
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new LowerCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
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
            int start = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(start);
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
            int start = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(start);
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
<details><summary>▸ Prompt (4 message(s), ~9,047 chars, 1 new)</summary>

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
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        Map registry = makeRegistry();

        String anchorPattern = "it''s a {0,lower} 'test'!";
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(anchorPattern, registry);
            String before = emf.toPattern();
            String out = emf.format(new Object[] { "DUMMY" });
            String after = emf.toPattern();

            if (!anchorPattern.equals(before) || !anchorPattern.equals(after) || !"it's a dummy test!".equals(out)) {
                throw new RuntimeException(
                    "[oracle:anchor-format] metamorphic violation: known valid seed must preserve pattern and format output input="
                        + anchorPattern + " before=" + before + " after=" + after + " out=" + out);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }

        String exploredPattern = buildPattern(data);
        try {
            ExtendedMessageFormat emf1 = new ExtendedMessageFormat(exploredPattern, registry);
            String p1 = emf1.toPattern();
            String out1 = emf1.format(new Object[] { pickArg(data) });

            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(p1, registry);
            String p2 = emf2.toPattern();
            String out2 = emf2.format(new Object[] { pickArg(data) });

            /*
             * Contract/oracle: toPattern() is the public reader for the format's pattern.
             * Reconstructing an ExtendedMessageFormat from its own toPattern() with the same
             * registry must preserve the represented format. A throw-deleting/branch-skipping
             * patch in quoted-string handling can silently corrupt parsing/bookkeeping while
             * avoiding a crash; this observable round-trip catches that.
             */
            String oracleArg = sanitizeLiteral(data.consumeString(12));
            if (oracleArg.length() == 0) {
                oracleArg = "x";
            }
            String lhs = emf1.format(new Object[] { oracleArg });
            String rhs = emf2.format(new Object[] { oracleArg });
            if (!safeEquals(lhs, rhs) || !safeEquals(p1, p2)) {
                throw new RuntimeException(
                    "[oracle:pattern-roundtrip] metamorphic violation: reconstructing from toPattern must preserve behavior input="
                        + exploredPattern + " p1=" + p1 + " p2=" + p2 + " lhs=" + lhs + " rhs=" + rhs);
            }

            if (out1 == null || out2 == null) {
                throw new RuntimeException(
                    "[oracle:null-output] metamorphic violation: valid formatting produced null input="
                        + exploredPattern + " out1=" + out1 + " out2=" + out2);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }
    }

    private static Map makeRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static String buildPattern(FuzzedDataProvider data) {
        String prefix = sanitizeLiteral(data.consumeAsciiString(16));
        String infix = sanitizeLiteral(data.consumeAsciiString(16));
        String quoted = sanitizeLiteral(data.consumeAsciiString(16));
        String suffix = sanitizeLiteral(data.consumeAsciiString(16));

        if (prefix.length() == 0) {
            prefix = "a";
        }
        if (infix.length() == 0) {
            infix = "b";
        }
        if (quoted.length() == 0) {
            quoted = "q";
        }
        if (suffix.length() == 0) {
            suffix = "z";
        }

        String formatName = data.consumeBoolean() ? "lower" : "upper";
        int variant = data.consumeInt(0, 5);

        if (variant == 0) {
            return prefix + "''" + infix + " {0," + formatName + "} '" + quoted + "' " + suffix;
        } else if (variant == 1) {
            return "'" + quoted + "' " + prefix + "''" + infix + " {0," + formatName + "} " + suffix;
        } else if (variant == 2) {
            return prefix + " {0," + formatName + "} " + infix + " '' " + suffix + " '" + quoted + "'";
        } else if (variant == 3) {
            return prefix + "''" + infix + "{0," + formatName + "}'" + quoted + "'" + suffix;
        } else if (variant == 4) {
            return prefix + " '" + quoted + "' " + infix + " {0," + formatName + "} " + suffix + "''";
        } else {
            return prefix + "''s " + infix + " {0," + formatName + "} '" + quoted + "'!" + suffix;
        }
    }

    private static String sanitizeLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == '\'') {
                b.append('x');
            } else if (Character.isISOControl(c)) {
                b.append(' ');
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }

    private static String pickArg(FuzzedDataProvider data) {
        String s = sanitizeLiteral(data.consumeString(20));
        if (s.length() == 0) {
            s = "dummy";
        }
        return s;
    }

    private static void handleThrowable(Throwable t) {
        if (isRootCause(t)) {
            throw (OutOfMemoryError) t;
        }
        if (isCleanRejection(t)) {
            return;
        }
        if (t instanceof RuntimeException) {
            return;
        }
        if (t instanceof Error) {
            return;
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(e.getClassName())
                && "appendQuotedString".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException) {
            return true;
        }
        if (t instanceof NumberFormatException) {
            return true;
        }
        String n = t.getClass().getName();
        return n.contains("Validation") || n.contains("Invalid");
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new LowerCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
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
            int start = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(start);
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
            int start = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(start);
        }
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~6,472 chars)</summary>

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
        Map registry = makeRegistry();

        // ANCHOR: exact trigger from the failing test. On the buggy version this must
        // escape as the observed OutOfMemoryError from appendQuotedString.
        String anchor = "it''s a {0,lower} 'test'!";
        runAndCheck(anchor, registry, "DUMMY", "it's a dummy test!");

        // EXPLORE: same root-cause property as the patch: a quote-handling path where
        // appendQuotedString is entered with pos at a quote and escaping enabled, so the
        // buggy code fails to advance past the opening quote before scanning.
        String fuzzArg = nonEmptyLiteral(data.consumeString(24));
        String pattern = buildQuoteHeavyPattern(data);
        runAndCheck(pattern, registry, fuzzArg, null);
    }

    private static void runAndCheck(String pattern, Map registry, String arg, String expectedOrNull) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

        /*
         * Observable contract used as oracle:
         * - toPattern() exposes the parsed pattern.
         * - Reconstructing an ExtendedMessageFormat from its own toPattern() with the same
         *   registry must preserve formatting behavior for the same argument.
         * A patch that merely skips/deletes the quoted-string bookkeeping could avoid the
         * crash but silently mis-parse quotes; this round-trip would then disagree.
         */
        String p1 = emf.toPattern();
        String out1 = emf.format(new Object[] { arg });

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(p1, registry);
        String p2 = emf2.toPattern();
        String out2 = emf2.format(new Object[] { arg });

        if (!eq(p1, p2) || !eq(out1, out2)) {
            throw new RuntimeException(
                "[oracle:pattern-roundtrip] metamorphic violation: reconstructing from toPattern must preserve behavior input="
                    + pattern + " p1=" + p1 + " p2=" + p2 + " out1=" + out1 + " out2=" + out2);
        }

        if (expectedOrNull != null && !expectedOrNull.equals(out1)) {
            throw new RuntimeException(
                "[oracle:anchor-output] metamorphic violation: known regression input formatted unexpectedly input="
                    + pattern + " expected=" + expectedOrNull + " actual=" + out1);
        }
    }

    private static String buildQuoteHeavyPattern(FuzzedDataProvider data) {
        String a = nonEmptyLiteral(data.consumeAsciiString(8));
        String b = nonEmptyLiteral(data.consumeAsciiString(8));
        String c = nonEmptyLiteral(data.consumeAsciiString(8));
        String d = nonEmptyLiteral(data.consumeAsciiString(8));
        String fmt = data.consumeBoolean() ? "lower" : "upper";

        switch (data.consumeInt(0, 5)) {
            case 0:
                return a + "''" + b + " {0," + fmt + "} '" + c + "'";
            case 1:
                return "'" + a + "' " + b + "''" + c + " {0," + fmt + "}!";
            case 2:
                return a + " {0," + fmt + "} '" + b + "' " + c + "''" + d;
            case 3:
                return a + "''s " + b + " {0," + fmt + "} '" + c + "'!";
            case 4:
                return "'" + a + "' " + b + " {0," + fmt + "} '" + c + "' " + d;
            default:
                return a + "''" + b + " {0," + fmt + "} '" + c + "' " + d + "''";
        }
    }

    private static String nonEmptyLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'') {
                sb.append('x');
            } else if (Character.isISOControl(ch)) {
                sb.append(' ');
            } else {
                sb.append(ch);
            }
        }
        if (sb.length() == 0) {
            sb.append('x');
        }
        return sb.toString();
    }

    private static boolean eq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static Map makeRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new LowerCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
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
            int start = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(start);
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
<details><summary>▸ Prompt (6 message(s), ~6,789 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
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
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = makeRegistry();

        // ANCHOR: exact trigger from the failing test. On the buggy version this must
        // escape as the observed OutOfMemoryError from appendQuotedString.
        String anchor = "it''s a {0,lower} 'test'!";
        runAndCheck(anchor, registry, "DUMMY", "it's a dummy test!");

        // EXPLORE: same root-cause property as the patch: a quote-handling path where
        // appendQuotedString is entered with pos at a quote and escaping enabled, so the
        // buggy code fails to advance past the opening quote before scanning.
        String fuzzArg = nonEmptyLiteral(data.consumeString(24));
        String pattern = buildQuoteHeavyPattern(data);
        runAndCheck(pattern, registry, fuzzArg, null);
    }

    private static void runAndCheck(String pattern, Map registry, String arg, String expectedOrNull) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

        /*
         * Observable contract used as oracle:
         * - toPattern() exposes the parsed pattern.
         * - Reconstructing an ExtendedMessageFormat from its own toPattern() with the same
         *   registry must preserve formatting behavior for the same argument.
         * A patch that merely skips/deletes the quoted-string bookkeeping could avoid the
         * crash but silently mis-parse quotes; this round-trip would then disagree.
         */
        String p1 = emf.toPattern();
        String out1 = emf.format(new Object[] { arg });

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(p1, registry);
        String p2 = emf2.toPattern();
        String out2 = emf2.format(new Object[] { arg });

        if (!eq(p1, p2) || !eq(out1, out2)) {
            throw new RuntimeException(
                "[oracle:pattern-roundtrip] metamorphic violation: reconstructing from toPattern must preserve behavior input="
                    + pattern + " p1=" + p1 + " p2=" + p2 + " out1=" + out1 + " out2=" + out2);
        }

        if (expectedOrNull != null && !expectedOrNull.equals(out1)) {
            throw new RuntimeException(
                "[oracle:anchor-output] metamorphic violation: known regression input formatted unexpectedly input="
                    + pattern + " expected=" + expectedOrNull + " actual=" + out1);
        }
    }

    private static String buildQuoteHeavyPattern(FuzzedDataProvider data) {
        String a = nonEmptyLiteral(data.consumeAsciiString(8));
        String b = nonEmptyLiteral(data.consumeAsciiString(8));
        String c = nonEmptyLiteral(data.consumeAsciiString(8));
        String d = nonEmptyLiteral(data.consumeAsciiString(8));
        String fmt = data.consumeBoolean() ? "lower" : "upper";

        switch (data.consumeInt(0, 5)) {
            case 0:
                return a + "''" + b + " {0," + fmt + "} '" + c + "'";
            case 1:
                return "'" + a + "' " + b + "''" + c + " {0," + fmt + "}!";
            case 2:
                return a + " {0," + fmt + "} '" + b + "' " + c + "''" + d;
            case 3:
                return a + "''s " + b + " {0," + fmt + "} '" + c + "'!";
            case 4:
                return "'" + a + "' " + b + " {0," + fmt + "} '" + c + "' " + d;
            default:
                return a + "''" + b + " {0," + fmt + "} '" + c + "' " + d + "''";
        }
    }

    private static String nonEmptyLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'') {
                sb.append('x');
            } else if (Character.isISOControl(ch)) {
                sb.append(' ');
            } else {
                sb.append(ch);
            }
        }
        if (sb.length() == 0) {
            sb.append('x');
        }
        return sb.toString();
    }

    private static boolean eq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static Map makeRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new LowerCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
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
            int start = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(start);
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
            int start = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(start);
        }
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~3,921 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: run the exact regression test first, with its real setup.
        // On the buggy version this is the known OutOfMemoryError path.
        ExtendedMessageFormatTest test = new ExtendedMessageFormatTest();
        try {
            test.setUp();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        test.testEscapedQuote_LANG_477();

        // EXPLORE: same root-cause property, but with many valid patterns that contain
        // an escaped quote before/around a format element, forcing the parser down the
        // real quote-handling path reached by applyPattern -> appendQuotedString.
        Map registry = new HashMap();
        String pattern = buildPattern(data);
        String arg = nonEmpty(data.consumeString(24));

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String p1 = emf.toPattern();
        String out1 = emf.format(new Object[] { arg });

        /*
         * Contract/oracle: toPattern() is the public representation of the parsed
         * message format. Reconstructing from that pattern with the same registry
         * must preserve formatting behavior for the same argument. A patch that only
         * suppresses the crash by skipping quote parsing/bookkeeping would violate this.
         */
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(p1, registry);
        String p2 = emf2.toPattern();
        String out2 = emf2.format(new Object[] { arg });

        if (!eq(p1, p2) || !eq(out1, out2)) {
            throw new RuntimeException(
                "[oracle:pattern-roundtrip] metamorphic violation: reconstructing from toPattern must preserve behavior input="
                    + pattern + " p1=" + p1 + " p2=" + p2 + " out1=" + out1 + " out2=" + out2);
        }
    }

    private static String buildPattern(FuzzedDataProvider data) {
        String a = literal(data.consumeAsciiString(10));
        String b = literal(data.consumeAsciiString(10));
        String c = literal(data.consumeAsciiString(10));
        String d = literal(data.consumeAsciiString(10));

        if (a.length() == 0) a = "a";
        if (b.length() == 0) b = "b";
        if (c.length() == 0) c = "c";
        if (d.length() == 0) d = "d";

        switch (data.consumeInt(0, 5)) {
            case 0:
                return a + "''" + b + " {0} '" + c + "'!";
            case 1:
                return "'" + a + "' " + b + "''" + c + " {0}";
            case 2:
                return a + " {0} '" + b + "' " + c + "''" + d;
            case 3:
                return a + "''s a {0} '" + b + "'!";
            case 4:
                return a + " {0} " + b + "''" + c + " '" + d + "'";
            default:
                return "'" + a + "' " + b + " {0} '" + c + "' " + d;
        }
    }

    private static String literal(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'') {
                sb.append('x');
            } else if (Character.isISOControl(ch)) {
                sb.append(' ');
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static String nonEmpty(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        return s;
    }

    private static boolean eq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
```

</details>

---
## [16] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [17] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (8 message(s), ~5,628 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
- *[assistant] message: identical to step [13] — not reprinted*
- *[user] message: identical to step [5] — not reprinted*
- *[assistant] message: identical to step [15] — not reprinted*
- *[user] message: identical to step [5] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: run the exact regression test first, with its real setup.
        // On the buggy version this is the known OutOfMemoryError path.
        ExtendedMessageFormatTest test = new ExtendedMessageFormatTest();
        try {
            test.setUp();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        test.testEscapedQuote_LANG_477();

        // EXPLORE: same root-cause property, but with many valid patterns that contain
        // an escaped quote before/around a format element, forcing the parser down the
        // real quote-handling path reached by applyPattern -> appendQuotedString.
        Map registry = new HashMap();
        String pattern = buildPattern(data);
        String arg = nonEmpty(data.consumeString(24));

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String p1 = emf.toPattern();
        String out1 = emf.format(new Object[] { arg });

        /*
         * Contract/oracle: toPattern() is the public representation of the parsed
         * message format. Reconstructing from that pattern with the same registry
         * must preserve formatting behavior for the same argument. A patch that only
         * suppresses the crash by skipping quote parsing/bookkeeping would violate this.
         */
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(p1, registry);
        String p2 = emf2.toPattern();
        String out2 = emf2.format(new Object[] { arg });

        if (!eq(p1, p2) || !eq(out1, out2)) {
            throw new RuntimeException(
                "[oracle:pattern-roundtrip] metamorphic violation: reconstructing from toPattern must preserve behavior input="
                    + pattern + " p1=" + p1 + " p2=" + p2 + " out1=" + out1 + " out2=" + out2);
        }
    }

    private static String buildPattern(FuzzedDataProvider data) {
        String a = literal(data.consumeAsciiString(10));
        String b = literal(data.consumeAsciiString(10));
        String c = literal(data.consumeAsciiString(10));
        String d = literal(data.consumeAsciiString(10));

        if (a.length() == 0) a = "a";
        if (b.length() == 0) b = "b";
        if (c.length() == 0) c = "c";
        if (d.length() == 0) d = "d";

        switch (data.consumeInt(0, 5)) {
            case 0:
                return a + "''" + b + " {0} '" + c + "'!";
            case 1:
                return "'" + a + "' " + b + "''" + c + " {0}";
            case 2:
                return a + " {0} '" + b + "' " + c + "''" + d;
            case 3:
                return a + "''s a {0} '" + b + "'!";
            case 4:
                return a + " {0} " + b + "''" + c + " '" + d + "'";
            default:
                return "'" + a + "' " + b + " {0} '" + c + "' " + d;
        }
    }

    private static String literal(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'') {
                sb.append('x');
            } else if (Character.isISOControl(ch)) {
                sb.append(' ');
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static String nonEmpty(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        return s;
    }

    private static boolean eq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/Lang_43_buggy/fuzz/attempt_007/FuzzHarness.java:11: error: constructor ExtendedMessageFormatTest in class ExtendedMessageFormatTest cannot be applied to given types;
        ExtendedMessageFormatTest test = new ExtendedMessageFormatTest();
                                         ^
  required: String
  found: no arguments
  reason: actual and formal argument lists differ in length
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
<details open><summary>▸ Output (~6,335 chars)</summary>

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
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, registry);
        String anchorBefore = anchor.toPattern();
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        String anchorAfter = anchor.toPattern();
        if (!anchorPattern.equals(anchorBefore) || !anchorPattern.equals(anchorAfter)
                || !"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException(
                "[oracle:anchor-output] metamorphic violation: known valid seed must preserve pattern and expected output input="
                    + anchorPattern + " before=" + anchorBefore + " after=" + anchorAfter + " out=" + anchorOut);
        }

        String pattern = buildPattern(data);
        String arg = sanitizeArg(data.consumeString(24));

        ExtendedMessageFormat emf1 = new ExtendedMessageFormat(pattern, registry);
        String p1 = emf1.toPattern();
        String out1 = emf1.format(new Object[] { arg });

        /*
         * Contract/oracle: toPattern() exposes the parsed pattern. Reconstructing an
         * ExtendedMessageFormat from its own toPattern() with the same registry must
         * preserve the represented formatting behavior for the same argument. A patch
         * that merely skips/deletes quoted-string bookkeeping could avoid the crash but
         * silently misparse quotes; this round-trip would then disagree.
         */
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(p1, registry);
        String p2 = emf2.toPattern();
        String out2 = emf2.format(new Object[] { arg });

        if (!eq(p1, p2) || !eq(out1, out2)) {
            throw new RuntimeException(
                "[oracle:pattern-roundtrip] metamorphic violation: reconstructing from toPattern must preserve behavior input="
                    + pattern + " p1=" + p1 + " p2=" + p2 + " out1=" + out1 + " out2=" + out2);
        }
    }

    private static String buildPattern(FuzzedDataProvider data) {
        String a = literal(data.consumeAsciiString(10));
        String b = literal(data.consumeAsciiString(10));
        String c = literal(data.consumeAsciiString(10));
        String d = literal(data.consumeAsciiString(10));

        if (a.length() == 0) {
            a = "it";
        }
        if (b.length() == 0) {
            b = "s";
        }
        if (c.length() == 0) {
            c = "test";
        }
        if (d.length() == 0) {
            d = "end";
        }

        String fmt = data.consumeBoolean() ? "lower" : "upper";
        switch (data.consumeInt(0, 5)) {
            case 0:
                return a + "''" + b + " a {0," + fmt + "} '" + c + "'!";
            case 1:
                return a + "''" + b + " {0," + fmt + "} '" + c + "'";
            case 2:
                return "'" + a + "' " + b + "'' " + c + " {0," + fmt + "}!";
            case 3:
                return a + " {0," + fmt + "} '" + b + "' " + c + "''" + d;
            case 4:
                return a + "''s a {0," + fmt + "} '" + b + "'!" + d;
            default:
                return "'" + a + "' " + b + " {0," + fmt + "} '" + c + "' " + d;
        }
    }

    private static String literal(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'') {
                sb.append('x');
            } else if (Character.isISOControl(ch)) {
                sb.append(' ');
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static String sanitizeArg(String s) {
        if (s == null || s.length() == 0) {
            return "Dummy";
        }
        return s;
    }

    private static boolean eq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new LowerCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
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
            int start = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(start);
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

---
## [19] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,362 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = buildRegistry();

        String anchorPattern = "it''s a {0,lower} 'test'!";
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(anchorPattern, registry);
            String before = emf.toPattern();
            String out = emf.format(new Object[] { "DUMMY" });
            String after = emf.toPattern();

            if (!"it's a dummy test!".equals(out)) {
                throw new RuntimeException("[oracle:anchor-format] metamorphic violation: exact regression pattern formatted incorrectly input="
                        + anchorPattern + " lhs=" + out + " rhs=it's a dummy test!");
            }
            if (before != null ? !before.equals(after) : after != null) {
                throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: format should not mutate toPattern input="
                        + anchorPattern + " lhs=" + before + " rhs=" + after);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }

        String prefix = sanitizeLiteral(data.consumeAsciiString(20));
        String infix = sanitizeLiteral(data.consumeAsciiString(20));
        String suffix = sanitizeLiteral(data.consumeAsciiString(20));
        String quotedLiteral = sanitizeLiteral(data.consumeAsciiString(12));
        String arg = sanitizeArg(data.consumeString(20));

        if (prefix.length() == 0) {
            prefix = "a";
        }
        if (quotedLiteral.length() == 0) {
            quotedLiteral = "test";
        }

        boolean includeCustomFormat = data.consumeBoolean();
        String formatElement = includeCustomFormat ? "{0,lower}" : "{0}";
        String patternWithQuotedLiteral =
                prefix + "''" + infix + " " + formatElement + " '" + quotedLiteral + "' " + suffix;
        String patternWithoutQuotedLiteral =
                prefix + "''" + infix + " " + formatElement + " " + quotedLiteral + " " + suffix;

        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(patternWithQuotedLiteral, registry);
            String before = emf.toPattern();
            emf.format(new Object[] { arg });
            String after = emf.toPattern();

            if (before != null ? !before.equals(after) : after != null) {
                throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: format should not mutate toPattern input="
                        + patternWithQuotedLiteral + " lhs=" + before + " rhs=" + after);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }

        try {
            ExtendedMessageFormat lhsFmt = new ExtendedMessageFormat(patternWithQuotedLiteral, registry);
            ExtendedMessageFormat rhsFmt = new ExtendedMessageFormat(patternWithoutQuotedLiteral, registry);
            String lhs = lhsFmt.format(new Object[] { arg });
            String rhs = rhsFmt.format(new Object[] { arg });

            /*
             * Contract used for this oracle:
             * In MessageFormat patterns, ordinary literal text does not need quoting; quotes are only
             * syntax for escaping. Therefore quoting a non-empty literal that contains no special
             * pattern characters must not change the formatted result. A patch that merely skips the
             * quoted-string bookkeeping can make the quote branch unreachable or silently mis-parse
             * the literal, violating this equivalence without throwing.
             */
            if (!lhs.equals(rhs)) {
                throw new RuntimeException("[oracle:quoted-vs-unquoted] metamorphic violation: equivalent quoted and unquoted literal patterns differ input="
                        + patternWithQuotedLiteral + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }

        int variants = data.consumeInt(1, 4);
        for (int i = 0; i < variants; i++) {
            String p1 = sanitizeLiteral(data.consumeAsciiString(10));
            String p2 = sanitizeLiteral(data.consumeAsciiString(10));
            String lit = sanitizeLiteral(data.consumeAsciiString(8));
            String a = sanitizeArg(data.consumeString(12));
            if (p1.length() == 0) {
                p1 = "x";
            }
            if (lit.length() == 0) {
                lit = "y";
            }

            String pattern = p1 + "''" + p2 + " {0,lower} '" + lit + "'";
            try {
                ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
                String before = emf.toPattern();
                emf.format(new Object[] { a });
                String after = emf.toPattern();
                if (before != null ? !before.equals(after) : after != null) {
                    throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: format should not mutate toPattern input="
                            + pattern + " lhs=" + before + " rhs=" + after);
                }
            } catch (Throwable t) {
                handleThrowable(t);
            }
        }
    }

    private static Map buildRegistry() {
        Map registry = new HashMap();
        try {
            registry.put("lower", new LowerCaseFormatFactory());
        } catch (Throwable ignored) {
        }
        try {
            registry.put("upper", new UpperCaseFormatFactory());
        } catch (Throwable ignored) {
        }
        return registry;
    }

    private static String sanitizeLiteral(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'' || c == '{' || c == '}' || c < 32) {
                continue;
            }
            if (Character.isLetterOrDigit(c) || c == ' ' || c == '_' || c == '-' || c == '.' || c == '!') {
                sb.append(c);
            }
        }
        String out = sb.toString().trim();
        return out.length() == 0 ? "" : out;
    }

    private static String sanitizeArg(String s) {
        if (s == null || s.length() == 0) {
            return "DUMMY";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length() && sb.length() < 20; i++) {
            char c = s.charAt(i);
            if (c >= 32) {
                sb.append(c);
            }
        }
        return sb.length() == 0 ? "DUMMY" : sb.toString();
    }

    private static void handleThrowable(Throwable t) {
        if (isRootCause(t)) {
            throwAsUnchecked(t);
        }
        if (isCleanRejection(t)) {
            return;
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
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(e.getClassName())
                    && "appendQuotedString".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static void throwAsUnchecked(Throwable t) {
        FuzzHarness.<RuntimeException>sneakyThrow(t);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}
```

</details>

---
## [20] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [21] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~10,058 chars, 2 new)</summary>

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
        Map registry = buildRegistry();

        String anchorPattern = "it''s a {0,lower} 'test'!";
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(anchorPattern, registry);
            String before = emf.toPattern();
            String out = emf.format(new Object[] { "DUMMY" });
            String after = emf.toPattern();

            if (!"it's a dummy test!".equals(out)) {
                throw new RuntimeException("[oracle:anchor-format] metamorphic violation: exact regression pattern formatted incorrectly input="
                        + anchorPattern + " lhs=" + out + " rhs=it's a dummy test!");
            }
            if (before != null ? !before.equals(after) : after != null) {
                throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: format should not mutate toPattern input="
                        + anchorPattern + " lhs=" + before + " rhs=" + after);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }

        String prefix = sanitizeLiteral(data.consumeAsciiString(20));
        String infix = sanitizeLiteral(data.consumeAsciiString(20));
        String suffix = sanitizeLiteral(data.consumeAsciiString(20));
        String quotedLiteral = sanitizeLiteral(data.consumeAsciiString(12));
        String arg = sanitizeArg(data.consumeString(20));

        if (prefix.length() == 0) {
            prefix = "a";
        }
        if (quotedLiteral.length() == 0) {
            quotedLiteral = "test";
        }

        boolean includeCustomFormat = data.consumeBoolean();
        String formatElement = includeCustomFormat ? "{0,lower}" : "{0}";
        String patternWithQuotedLiteral =
                prefix + "''" + infix + " " + formatElement + " '" + quotedLiteral + "' " + suffix;
        String patternWithoutQuotedLiteral =
                prefix + "''" + infix + " " + formatElement + " " + quotedLiteral + " " + suffix;

        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(patternWithQuotedLiteral, registry);
            String before = emf.toPattern();
            emf.format(new Object[] { arg });
            String after = emf.toPattern();

            if (before != null ? !before.equals(after) : after != null) {
                throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: format should not mutate toPattern input="
                        + patternWithQuotedLiteral + " lhs=" + before + " rhs=" + after);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }

        try {
            ExtendedMessageFormat lhsFmt = new ExtendedMessageFormat(patternWithQuotedLiteral, registry);
            ExtendedMessageFormat rhsFmt = new ExtendedMessageFormat(patternWithoutQuotedLiteral, registry);
            String lhs = lhsFmt.format(new Object[] { arg });
            String rhs = rhsFmt.format(new Object[] { arg });

            /*
             * Contract used for this oracle:
             * In MessageFormat patterns, ordinary literal text does not need quoting; quotes are only
             * syntax for escaping. Therefore quoting a non-empty literal that contains no special
             * pattern characters must not change the formatted result. A patch that merely skips the
             * quoted-string bookkeeping can make the quote branch unreachable or silently mis-parse
             * the literal, violating this equivalence without throwing.
             */
            if (!lhs.equals(rhs)) {
                throw new RuntimeException("[oracle:quoted-vs-unquoted] metamorphic violation: equivalent quoted and unquoted literal patterns differ input="
                        + patternWithQuotedLiteral + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }

        int variants = data.consumeInt(1, 4);
        for (int i = 0; i < variants; i++) {
            String p1 = sanitizeLiteral(data.consumeAsciiString(10));
            String p2 = sanitizeLiteral(data.consumeAsciiString(10));
            String lit = sanitizeLiteral(data.consumeAsciiString(8));
            String a = sanitizeArg(data.consumeString(12));
            if (p1.length() == 0) {
                p1 = "x";
            }
            if (lit.length() == 0) {
                lit = "y";
            }

            String pattern = p1 + "''" + p2 + " {0,lower} '" + lit + "'";
            try {
                ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
                String before = emf.toPattern();
                emf.format(new Object[] { a });
                String after = emf.toPattern();
                if (before != null ? !before.equals(after) : after != null) {
                    throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: format should not mutate toPattern input="
                            + pattern + " lhs=" + before + " rhs=" + after);
                }
            } catch (Throwable t) {
                handleThrowable(t);
            }
        }
    }

    private static Map buildRegistry() {
        Map registry = new HashMap();
        try {
            registry.put("lower", new LowerCaseFormatFactory());
        } catch (Throwable ignored) {
        }
        try {
            registry.put("upper", new UpperCaseFormatFactory());
        } catch (Throwable ignored) {
        }
        return registry;
    }

    private static String sanitizeLiteral(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'' || c == '{' || c == '}' || c < 32) {
                continue;
            }
            if (Character.isLetterOrDigit(c) || c == ' ' || c == '_' || c == '-' || c == '.' || c == '!') {
                sb.append(c);
            }
        }
        String out = sb.toString().trim();
        return out.length() == 0 ? "" : out;
    }

    private static String sanitizeArg(String s) {
        if (s == null || s.length() == 0) {
            return "DUMMY";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length() && sb.length() < 20; i++) {
            char c = s.charAt(i);
            if (c >= 32) {
                sb.append(c);
            }
        }
        return sb.length() == 0 ? "DUMMY" : sb.toString();
    }

    private static void handleThrowable(Throwable t) {
        if (isRootCause(t)) {
            throwAsUnchecked(t);
        }
        if (isCleanRejection(t)) {
            return;
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
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(e.getClassName())
                    && "appendQuotedString".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static void throwAsUnchecked(Throwable t) {
        FuzzHarness.<RuntimeException>sneakyThrow(t);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/Lang_43_buggy/fuzz/attempt_009/FuzzHarness.java:118: error: cannot find symbol
            registry.put("lower", new LowerCaseFormatFactory());
                                      ^
  symbol:   class LowerCaseFormatFactory
  location: class FuzzHarness
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/Lang_43_buggy/fuzz/attempt_009/FuzzHarness.java:122: error: cannot find symbol
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
<details open><summary>▸ Output (~7,917 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        String anchorPattern = "it''s a {0,lower} 'test'!";
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(anchorPattern, registry);
            String before = emf.toPattern();
            String out = emf.format(new Object[] { "DUMMY" });
            String after = emf.toPattern();
            if (!"it's a dummy test!".equals(out)) {
                throw new RuntimeException("[oracle:anchor-format] metamorphic violation: exact regression pattern formatted incorrectly input="
                        + anchorPattern + " lhs=" + out + " rhs=it's a dummy test!");
            }
            if (before != null ? !before.equals(after) : after != null) {
                throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: format should not mutate toPattern input="
                        + anchorPattern + " lhs=" + before + " rhs=" + after);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }

        String prefix = sanitizeLiteral(data.consumeAsciiString(20));
        String infix = sanitizeLiteral(data.consumeAsciiString(20));
        String suffix = sanitizeLiteral(data.consumeAsciiString(20));
        String quotedLiteral = sanitizeLiteral(data.consumeAsciiString(12));
        String arg = sanitizeArg(data.consumeString(20));
        boolean useCustomDescriptor = data.consumeBoolean();

        if (prefix.length() == 0) {
            prefix = "a";
        }
        if (quotedLiteral.length() == 0) {
            quotedLiteral = "test";
        }

        String formatElement = useCustomDescriptor ? "{0,lower}" : "{0}";
        String patternWithQuotedLiteral =
                prefix + "''" + infix + " " + formatElement + " '" + quotedLiteral + "' " + suffix;
        String patternWithoutQuotedLiteral =
                prefix + "''" + infix + " " + formatElement + " " + quotedLiteral + " " + suffix;

        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(patternWithQuotedLiteral, registry);
            String before = emf.toPattern();
            emf.format(new Object[] { arg });
            String after = emf.toPattern();
            if (before != null ? !before.equals(after) : after != null) {
                throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: format should not mutate toPattern input="
                        + patternWithQuotedLiteral + " lhs=" + before + " rhs=" + after);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }

        try {
            ExtendedMessageFormat lhsFmt = new ExtendedMessageFormat(patternWithQuotedLiteral, registry);
            ExtendedMessageFormat rhsFmt = new ExtendedMessageFormat(patternWithoutQuotedLiteral, registry);
            String lhs = lhsFmt.format(new Object[] { arg });
            String rhs = rhsFmt.format(new Object[] { arg });

            /*
             * Contract used for this oracle:
             * In MessageFormat patterns, ordinary literal text does not need quoting; quotes are only
             * syntax for escaping. Therefore quoting a non-empty literal that contains no special
             * pattern characters must not change the formatted result. A patch that simply skips
             * quoted-string parsing or bookkeeping can silently change the output without throwing.
             */
            if (!lhs.equals(rhs)) {
                throw new RuntimeException("[oracle:quoted-vs-unquoted] metamorphic violation: equivalent quoted and unquoted literal patterns differ input="
                        + patternWithQuotedLiteral + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }

        int variants = data.consumeInt(1, 4);
        for (int i = 0; i < variants; i++) {
            String p1 = sanitizeLiteral(data.consumeAsciiString(10));
            String p2 = sanitizeLiteral(data.consumeAsciiString(10));
            String lit = sanitizeLiteral(data.consumeAsciiString(8));
            String a = sanitizeArg(data.consumeString(12));

            if (p1.length() == 0) {
                p1 = "x";
            }
            if (lit.length() == 0) {
                lit = "y";
            }

            String pattern = p1 + "''" + p2 + " {0,lower} '" + lit + "'";
            try {
                ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
                String before = emf.toPattern();
                emf.format(new Object[] { a });
                String after = emf.toPattern();
                if (before != null ? !before.equals(after) : after != null) {
                    throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: format should not mutate toPattern input="
                            + pattern + " lhs=" + before + " rhs=" + after);
                }
            } catch (Throwable t) {
                handleThrowable(t);
            }
        }
    }

    private static String sanitizeLiteral(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'' || c == '{' || c == '}' || c < 32) {
                continue;
            }
            if (Character.isLetterOrDigit(c) || c == ' ' || c == '_' || c == '-' || c == '.' || c == '!') {
                sb.append(c);
            }
        }
        String out = sb.toString().trim();
        return out.length() == 0 ? "" : out;
    }

    private static String sanitizeArg(String s) {
        if (s == null || s.length() == 0) {
            return "DUMMY";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length() && sb.length() < 20; i++) {
            char c = s.charAt(i);
            if (c >= 32) {
                sb.append(c);
            }
        }
        return sb.length() == 0 ? "DUMMY" : sb.toString();
    }

    private static void handleThrowable(Throwable t) {
        if (isRootCause(t)) {
            throwAsUnchecked(t);
        }
        if (isCleanRejection(t)) {
            return;
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
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(e.getClassName())
                    && "appendQuotedString".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static void throwAsUnchecked(Throwable t) {
        FuzzHarness.<RuntimeException>sneakyThrow(t);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
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
<details><summary>▸ Prompt (6 message(s), ~8,235 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
- *[assistant] message: identical to step [21] — not reprinted*
- *[user] message: identical to step [21] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        String anchorPattern = "it''s a {0,lower} 'test'!";
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(anchorPattern, registry);
            String before = emf.toPattern();
            String out = emf.format(new Object[] { "DUMMY" });
            String after = emf.toPattern();
            if (!"it's a dummy test!".equals(out)) {
                throw new RuntimeException("[oracle:anchor-format] metamorphic violation: exact regression pattern formatted incorrectly input="
                        + anchorPattern + " lhs=" + out + " rhs=it's a dummy test!");
            }
            if (before != null ? !before.equals(after) : after != null) {
                throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: format should not mutate toPattern input="
                        + anchorPattern + " lhs=" + before + " rhs=" + after);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }

        String prefix = sanitizeLiteral(data.consumeAsciiString(20));
        String infix = sanitizeLiteral(data.consumeAsciiString(20));
        String suffix = sanitizeLiteral(data.consumeAsciiString(20));
        String quotedLiteral = sanitizeLiteral(data.consumeAsciiString(12));
        String arg = sanitizeArg(data.consumeString(20));
        boolean useCustomDescriptor = data.consumeBoolean();

        if (prefix.length() == 0) {
            prefix = "a";
        }
        if (quotedLiteral.length() == 0) {
            quotedLiteral = "test";
        }

        String formatElement = useCustomDescriptor ? "{0,lower}" : "{0}";
        String patternWithQuotedLiteral =
                prefix + "''" + infix + " " + formatElement + " '" + quotedLiteral + "' " + suffix;
        String patternWithoutQuotedLiteral =
                prefix + "''" + infix + " " + formatElement + " " + quotedLiteral + " " + suffix;

        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(patternWithQuotedLiteral, registry);
            String before = emf.toPattern();
            emf.format(new Object[] { arg });
            String after = emf.toPattern();
            if (before != null ? !before.equals(after) : after != null) {
                throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: format should not mutate toPattern input="
                        + patternWithQuotedLiteral + " lhs=" + before + " rhs=" + after);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }

        try {
            ExtendedMessageFormat lhsFmt = new ExtendedMessageFormat(patternWithQuotedLiteral, registry);
            ExtendedMessageFormat rhsFmt = new ExtendedMessageFormat(patternWithoutQuotedLiteral, registry);
            String lhs = lhsFmt.format(new Object[] { arg });
            String rhs = rhsFmt.format(new Object[] { arg });

            /*
             * Contract used for this oracle:
             * In MessageFormat patterns, ordinary literal text does not need quoting; quotes are only
             * syntax for escaping. Therefore quoting a non-empty literal that contains no special
             * pattern characters must not change the formatted result. A patch that simply skips
             * quoted-string parsing or bookkeeping can silently change the output without throwing.
             */
            if (!lhs.equals(rhs)) {
                throw new RuntimeException("[oracle:quoted-vs-unquoted] metamorphic violation: equivalent quoted and unquoted literal patterns differ input="
                        + patternWithQuotedLiteral + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }

        int variants = data.consumeInt(1, 4);
        for (int i = 0; i < variants; i++) {
            String p1 = sanitizeLiteral(data.consumeAsciiString(10));
            String p2 = sanitizeLiteral(data.consumeAsciiString(10));
            String lit = sanitizeLiteral(data.consumeAsciiString(8));
            String a = sanitizeArg(data.consumeString(12));

            if (p1.length() == 0) {
                p1 = "x";
            }
            if (lit.length() == 0) {
                lit = "y";
            }

            String pattern = p1 + "''" + p2 + " {0,lower} '" + lit + "'";
            try {
                ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
                String before = emf.toPattern();
                emf.format(new Object[] { a });
                String after = emf.toPattern();
                if (before != null ? !before.equals(after) : after != null) {
                    throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: format should not mutate toPattern input="
                            + pattern + " lhs=" + before + " rhs=" + after);
                }
            } catch (Throwable t) {
                handleThrowable(t);
            }
        }
    }

    private static String sanitizeLiteral(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'' || c == '{' || c == '}' || c < 32) {
                continue;
            }
            if (Character.isLetterOrDigit(c) || c == ' ' || c == '_' || c == '-' || c == '.' || c == '!') {
                sb.append(c);
            }
        }
        String out = sb.toString().trim();
        return out.length() == 0 ? "" : out;
    }

    private static String sanitizeArg(String s) {
        if (s == null || s.length() == 0) {
            return "DUMMY";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length() && sb.length() < 20; i++) {
            char c = s.charAt(i);
            if (c >= 32) {
                sb.append(c);
            }
        }
        return sb.length() == 0 ? "DUMMY" : sb.toString();
    }

    private static void handleThrowable(Throwable t) {
        if (isRootCause(t)) {
            throwAsUnchecked(t);
        }
        if (isCleanRejection(t)) {
            return;
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
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(e.getClassName())
                    && "appendQuotedString".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static void throwAsUnchecked(Throwable t) {
        FuzzHarness.<RuntimeException>sneakyThrow(t);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~6,283 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String anchorPattern = "it''s a {0} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern);
        String anchorBefore = anchor.toPattern();
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        String anchorAfter = anchor.toPattern();
        if (!"it's a DUMMY test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-format] metamorphic violation: exact regression pattern formatted incorrectly input="
                    + anchorPattern + " lhs=" + anchorOut + " rhs=it's a DUMMY test!");
        }
        if (anchorBefore != null ? !anchorBefore.equals(anchorAfter) : anchorAfter != null) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: format should not mutate toPattern input="
                    + anchorPattern + " lhs=" + anchorBefore + " rhs=" + anchorAfter);
        }

        String prefix = nonEmptyLiteral(data.consumeAsciiString(24), "a");
        String middle = nonEmptyLiteral(data.consumeAsciiString(24), "b");
        String quoted = nonEmptyLiteral(data.consumeAsciiString(16), "test");
        String suffix = cleanArg(data.consumeAsciiString(24), "z");
        String arg = cleanArg(data.consumeString(24), "X");
        int shape = data.consumeInt(0, 3);

        String pattern1;
        String pattern2;

        /*
         * Contract used for these oracles:
         * In MessageFormat patterns, ordinary literal text does not need quoting; quoting a literal
         * segment that contains no special pattern characters must preserve the formatted output.
         * Also, formatting is read-only with respect to the pattern representation, so toPattern()
         * should be stable before and after format().
         *
         * The bug fixed by the patch is specifically on doubled-quote handling ("''"). These patterns
         * are valid by construction and all contain an escaped quote so execution reaches the changed
         * code on construction.
         */
        if (shape == 0) {
            pattern1 = prefix + "''" + middle + " {0} '" + quoted + "' " + suffix;
            pattern2 = prefix + "''" + middle + " {0} " + quoted + " " + suffix;
        } else if (shape == 1) {
            pattern1 = "'" + quoted + "' " + prefix + "''" + middle + " {0}";
            pattern2 = quoted + " " + prefix + "''" + middle + " {0}";
        } else if (shape == 2) {
            pattern1 = prefix + " {0} " + middle + "''" + suffix + " '" + quoted + "'";
            pattern2 = prefix + " {0} " + middle + "''" + suffix + " " + quoted;
        } else {
            pattern1 = prefix + "''" + middle + " '" + quoted + "' {0} " + suffix;
            pattern2 = prefix + "''" + middle + " " + quoted + " {0} " + suffix;
        }

        ExtendedMessageFormat emf1 = new ExtendedMessageFormat(pattern1);
        String before1 = emf1.toPattern();
        String out1 = emf1.format(new Object[] { arg });
        String after1 = emf1.toPattern();
        if (before1 != null ? !before1.equals(after1) : after1 != null) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: format should not mutate toPattern input="
                    + pattern1 + " lhs=" + before1 + " rhs=" + after1);
        }

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(pattern2);
        String out2 = emf2.format(new Object[] { arg });
        if (!out1.equals(out2)) {
            throw new RuntimeException("[oracle:quoted-vs-unquoted] metamorphic violation: equivalent quoted and unquoted literal patterns differ input="
                    + pattern1 + " lhs=" + out1 + " rhs=" + out2);
        }

        String extraPrefix = nonEmptyLiteral(data.consumeAsciiString(12), "m");
        String extraQuoted = nonEmptyLiteral(data.consumeAsciiString(10), "n");
        String extraArg = cleanArg(data.consumeString(12), "Y");
        String pattern3 = extraPrefix + "''" + " {0} '" + extraQuoted + "'";
        ExtendedMessageFormat emf3 = new ExtendedMessageFormat(pattern3);
        String before3 = emf3.toPattern();
        String out3a = emf3.format(new Object[] { extraArg });
        String after3 = emf3.toPattern();
        if (before3 != null ? !before3.equals(after3) : after3 != null) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: format should not mutate toPattern input="
                    + pattern3 + " lhs=" + before3 + " rhs=" + after3);
        }
        String out3b = emf3.format(new Object[] { extraArg });
        if (!out3a.equals(out3b)) {
            throw new RuntimeException("[oracle:idempotent-format] metamorphic violation: repeated format on same instance should be stable input="
                    + pattern3 + " lhs=" + out3a + " rhs=" + out3b);
        }
    }

    private static String nonEmptyLiteral(String s, String fallback) {
        String cleaned = cleanLiteral(s);
        return cleaned.length() == 0 ? fallback : cleaned;
    }

    private static String cleanLiteral(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length() && sb.length() < 24; i++) {
            char c = s.charAt(i);
            if (c < 32 || c == '\'' || c == '{' || c == '}') {
                continue;
            }
            if (Character.isLetterOrDigit(c) || c == ' ' || c == '_' || c == '-' || c == '.' || c == '!') {
                sb.append(c);
            }
        }
        return sb.toString().trim();
    }

    private static String cleanArg(String s, String fallback) {
        if (s == null) {
            return fallback;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length() && sb.length() < 24; i++) {
            char c = s.charAt(i);
            if (c >= 32) {
                sb.append(c);
            }
        }
        String out = sb.toString();
        return out.length() == 0 ? fallback : out;
    }
}
```

</details>

---
## [24] ⚙️ harness-attempt · `attempt_011`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.IllegalArgumentException: Unmatched braces in the pattern.

---
## [25] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,779 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        // ANCHOR: exact failing-test pattern first, through the real public API.
        tryPattern("it''s a {0,lower} 'test'!", registry, new Object[] { "DUMMY" }, false);

        // EXPLORE: generate many patterns with the same root-cause property:
        // a doubled quote in normal text, which drives ExtendedMessageFormat parsing
        // through appendQuotedString while scanning a real pattern.
        String prefix = sanitizeText(data.consumeAsciiString(24));
        String infix = sanitizeText(data.consumeAsciiString(24));
        String suffix = sanitizeText(data.consumeAsciiString(24));
        String quoted = sanitizeQuotedLiteral(data.consumeAsciiString(24));

        int variant = data.consumeInt(0, 5);
        String pattern;
        switch (variant) {
            case 0:
                pattern = nonEmpty(prefix) + "''" + nonEmpty(infix) + " {0}";
                break;
            case 1:
                pattern = nonEmpty(prefix) + "''" + nonEmpty(infix) + " {0} '" + quoted + "'";
                break;
            case 2:
                pattern = "'" + quoted + "' " + nonEmpty(prefix) + "''" + nonEmpty(infix) + " {0}";
                break;
            case 3:
                pattern = nonEmpty(prefix) + " {0} " + nonEmpty(infix) + "''" + nonEmpty(suffix);
                break;
            case 4:
                pattern = nonEmpty(prefix) + "''" + nonEmpty(infix) + " {0,number,integer}";
                break;
            default:
                pattern = nonEmpty(prefix) + "''" + nonEmpty(infix) + " {0,date,short}";
                break;
        }

        Object arg;
        if (pattern.indexOf("{0,date,short}") >= 0) {
            arg = new java.util.Date(Math.abs((long) data.consumeInt()) % 1000000000L);
        } else if (pattern.indexOf("{0,number,integer}") >= 0) {
            arg = Integer.valueOf(data.consumeInt(-1000000, 1000000));
        } else {
            arg = sanitizeText(data.consumeAsciiString(20));
        }

        // Metamorphic/post-condition:
        // With no custom registry formats in use, ExtendedMessageFormat is obligated to behave
        // like MessageFormat for the same valid pattern and arguments. A "fix" that merely skips
        // the quoted-string bookkeeping could stop crashing but silently produce the wrong pattern
        // or formatted output. We therefore compare two REAL library calls on valid-by-construction
        // inputs and also require toPattern() round-tripping to agree.
        try {
            MessageFormat mf = new MessageFormat(pattern);
            String expected = mf.format(new Object[] { arg });

            ExtendedMessageFormat emf1 = new ExtendedMessageFormat(pattern, registry);
            String before = emf1.toPattern();
            String actual = emf1.format(new Object[] { arg });
            String after = emf1.toPattern();

            if (!safeEquals(before, after)) {
                throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: toPattern changed across format input=" + pattern + " lhs=" + before + " rhs=" + after);
            }
            if (!safeEquals(actual, expected)) {
                throw new RuntimeException("[oracle:mf-equivalence] metamorphic violation: ExtendedMessageFormat must match MessageFormat when no custom formats are used input=" + pattern + " lhs=" + actual + " rhs=" + expected);
            }

            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, registry);
            String reparsed = emf2.toPattern();
            if (!safeEquals(before, reparsed)) {
                throw new RuntimeException("[oracle:toPattern-roundtrip] metamorphic violation: reparsing toPattern should preserve it input=" + pattern + " lhs=" + before + " rhs=" + reparsed);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw rethrowUnchecked(t);
            }
        }
    }

    private static void tryPattern(String pattern, Map registry, Object[] args, boolean swallowAllNonRoot) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
            emf.format(args);
            emf.toPattern();
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw rethrowUnchecked(t);
            }
            if (!swallowAllNonRoot) {
                return;
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
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(e.getClassName())
                    && "appendQuotedString".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static RuntimeException rethrowUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            return (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        return new RuntimeException(t);
    }

    private static String sanitizeText(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == '\'') {
                b.append('x');
            } else if (Character.isISOControl(c)) {
                b.append('y');
            } else {
                b.append(c);
            }
        }
        if (b.length() == 0) {
            b.append('x');
        }
        return b.toString();
    }

    private static String sanitizeQuotedLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "q";
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'') {
                b.append("''");
            } else if (c == '{' || c == '}') {
                b.append('q');
            } else if (Character.isISOControl(c)) {
                b.append('r');
            } else {
                b.append(c);
            }
        }
        if (b.length() == 0) {
            b.append('q');
        }
        return b.toString();
    }

    private static String nonEmpty(String s) {
        return (s == null || s.length() == 0) ? "x" : s;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
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
<details><summary>▸ Prompt (4 message(s), ~7,976 chars, 1 new)</summary>

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
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        // ANCHOR: exact failing-test pattern first, through the real public API.
        tryPattern("it''s a {0,lower} 'test'!", registry, new Object[] { "DUMMY" }, false);

        // EXPLORE: generate many patterns with the same root-cause property:
        // a doubled quote in normal text, which drives ExtendedMessageFormat parsing
        // through appendQuotedString while scanning a real pattern.
        String prefix = sanitizeText(data.consumeAsciiString(24));
        String infix = sanitizeText(data.consumeAsciiString(24));
        String suffix = sanitizeText(data.consumeAsciiString(24));
        String quoted = sanitizeQuotedLiteral(data.consumeAsciiString(24));

        int variant = data.consumeInt(0, 5);
        String pattern;
        switch (variant) {
            case 0:
                pattern = nonEmpty(prefix) + "''" + nonEmpty(infix) + " {0}";
                break;
            case 1:
                pattern = nonEmpty(prefix) + "''" + nonEmpty(infix) + " {0} '" + quoted + "'";
                break;
            case 2:
                pattern = "'" + quoted + "' " + nonEmpty(prefix) + "''" + nonEmpty(infix) + " {0}";
                break;
            case 3:
                pattern = nonEmpty(prefix) + " {0} " + nonEmpty(infix) + "''" + nonEmpty(suffix);
                break;
            case 4:
                pattern = nonEmpty(prefix) + "''" + nonEmpty(infix) + " {0,number,integer}";
                break;
            default:
                pattern = nonEmpty(prefix) + "''" + nonEmpty(infix) + " {0,date,short}";
                break;
        }

        Object arg;
        if (pattern.indexOf("{0,date,short}") >= 0) {
            arg = new java.util.Date(Math.abs((long) data.consumeInt()) % 1000000000L);
        } else if (pattern.indexOf("{0,number,integer}") >= 0) {
            arg = Integer.valueOf(data.consumeInt(-1000000, 1000000));
        } else {
            arg = sanitizeText(data.consumeAsciiString(20));
        }

        // Metamorphic/post-condition:
        // With no custom registry formats in use, ExtendedMessageFormat is obligated to behave
        // like MessageFormat for the same valid pattern and arguments. A "fix" that merely skips
        // the quoted-string bookkeeping could stop crashing but silently produce the wrong pattern
        // or formatted output. We therefore compare two REAL library calls on valid-by-construction
        // inputs and also require toPattern() round-tripping to agree.
        try {
            MessageFormat mf = new MessageFormat(pattern);
            String expected = mf.format(new Object[] { arg });

            ExtendedMessageFormat emf1 = new ExtendedMessageFormat(pattern, registry);
            String before = emf1.toPattern();
            String actual = emf1.format(new Object[] { arg });
            String after = emf1.toPattern();

            if (!safeEquals(before, after)) {
                throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: toPattern changed across format input=" + pattern + " lhs=" + before + " rhs=" + after);
            }
            if (!safeEquals(actual, expected)) {
                throw new RuntimeException("[oracle:mf-equivalence] metamorphic violation: ExtendedMessageFormat must match MessageFormat when no custom formats are used input=" + pattern + " lhs=" + actual + " rhs=" + expected);
            }

            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, registry);
            String reparsed = emf2.toPattern();
            if (!safeEquals(before, reparsed)) {
                throw new RuntimeException("[oracle:toPattern-roundtrip] metamorphic violation: reparsing toPattern should preserve it input=" + pattern + " lhs=" + before + " rhs=" + reparsed);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw rethrowUnchecked(t);
            }
        }
    }

    private static void tryPattern(String pattern, Map registry, Object[] args, boolean swallowAllNonRoot) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
            emf.format(args);
            emf.toPattern();
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw rethrowUnchecked(t);
            }
            if (!swallowAllNonRoot) {
                return;
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
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(e.getClassName())
                    && "appendQuotedString".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static RuntimeException rethrowUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            return (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        return new RuntimeException(t);
    }

    private static String sanitizeText(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == '\'') {
                b.append('x');
            } else if (Character.isISOControl(c)) {
                b.append('y');
            } else {
                b.append(c);
            }
        }
        if (b.length() == 0) {
            b.append('x');
        }
        return b.toString();
    }

    private static String sanitizeQuotedLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "q";
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'') {
                b.append("''");
            } else if (c == '{' || c == '}') {
                b.append('q');
            } else if (Character.isISOControl(c)) {
                b.append('r');
            } else {
                b.append(c);
            }
        }
        if (b.length() == 0) {
            b.append('q');
        }
        return b.toString();
    }

    private static String nonEmpty(String s) {
        return (s == null || s.length() == 0) ? "x" : s;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~6,084 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        // ANCHOR: exact failing test. On the buggy version this must reach
        // ExtendedMessageFormat.appendQuotedString and trigger the verified OOME.
        ExtendedMessageFormat anchor = new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });

        // Contract asserted from the test itself: the pattern must format to the same
        // observable result as the regression test expects. A "fix" that merely avoids
        // the crash by skipping quote handling would violate this.
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: exact regression output changed input=it''s a {0,lower} 'test'! lhs=" + anchorOut + " rhs=it's a dummy test!");
        }

        String anchorPattern = anchor.toPattern();
        if (!"it''s a {0,lower} 'test'!".equals(anchorPattern)) {
            throw new RuntimeException("[oracle:anchor-topattern] metamorphic violation: toPattern must preserve the registered-format pattern input=it''s a {0,lower} 'test'! lhs=" + anchorPattern + " rhs=it''s a {0,lower} 'test'!");
        }

        // EXPLORE: same root-cause property as the patch: a doubled quote outside a
        // quoted section, before a custom format element, so appendQuotedString is
        // invoked with escapingOn=true while positioned on a quote.
        String left = cleanText(data.consumeAsciiString(16));
        String middle = cleanText(data.consumeAsciiString(16));
        String literal = cleanQuotedLiteral(data.consumeAsciiString(16));
        String arg = cleanText(data.consumeAsciiString(16));
        if (arg.length() == 0) {
            arg = "X";
        }

        int variant = data.consumeInt(0, 5);
        String fmt = data.consumeBoolean() ? "lower" : "upper";
        String pattern;
        switch (variant) {
            case 0:
                pattern = nonEmpty(left) + "''" + nonEmpty(middle) + " {0," + fmt + "} '" + literal + "'!";
                break;
            case 1:
                pattern = nonEmpty(left) + "''s a {0," + fmt + "} '" + literal + "'!";
                break;
            case 2:
                pattern = nonEmpty(left) + "''" + nonEmpty(middle) + " {0," + fmt + "}";
                break;
            case 3:
                pattern = nonEmpty(left) + "'' " + "{0," + fmt + "} '" + literal + "'";
                break;
            case 4:
                pattern = "'" + literal + "' " + nonEmpty(left) + "''" + nonEmpty(middle) + " {0," + fmt + "}!";
                break;
            default:
                pattern = nonEmpty(left) + "''" + nonEmpty(middle) + " {0," + fmt + "} '" + literal + "' " + nonEmpty(cleanText(data.consumeAsciiString(8)));
                break;
        }

        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
            String out = emf.format(new Object[] { arg });
            String p1 = emf.toPattern();

            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(p1, registry);
            String p2 = emf2.toPattern();
            if (!safeEquals(p1, p2)) {
                throw new RuntimeException("[oracle:pattern-roundtrip] metamorphic violation: reparsing toPattern must preserve it input=" + pattern + " lhs=" + p1 + " rhs=" + p2);
            }

            String expected;
            if ("lower".equals(fmt)) {
                expected = new MessageFormat(pattern.replace("," + fmt + "}", "}")).format(new Object[] { arg.toLowerCase() });
            } else {
                expected = new MessageFormat(pattern.replace("," + fmt + "}", "}")).format(new Object[] { arg.toUpperCase() });
            }
            if (!safeEquals(out, expected)) {
                throw new RuntimeException("[oracle:format-behavior] metamorphic violation: registered format result disagrees with equivalent transformed MessageFormat input=" + pattern + " lhs=" + out + " rhs=" + expected);
            }
        } catch (IllegalArgumentException e) {
            return;
        } catch (NumberFormatException e) {
            return;
        }
    }

    private static String cleanText(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == '\'') {
                b.append('x');
            } else if (Character.isISOControl(c)) {
                b.append('y');
            } else {
                b.append(c);
            }
        }
        return b.length() == 0 ? "x" : b.toString();
    }

    private static String cleanQuotedLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "test";
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'') {
                b.append("''");
            } else if (c == '{' || c == '}') {
                b.append('q');
            } else if (Character.isISOControl(c)) {
                b.append('r');
            } else {
                b.append(c);
            }
        }
        return b.length() == 0 ? "test" : b.toString();
    }

    private static String nonEmpty(String s) {
        return (s == null || s.length() == 0) ? "x" : s;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
```

</details>

---
## [28] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [29] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~7,882 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
- *[assistant] message: identical to step [27] — not reprinted*
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
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        // ANCHOR: exact failing test. On the buggy version this must reach
        // ExtendedMessageFormat.appendQuotedString and trigger the verified OOME.
        ExtendedMessageFormat anchor = new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });

        // Contract asserted from the test itself: the pattern must format to the same
        // observable result as the regression test expects. A "fix" that merely avoids
        // the crash by skipping quote handling would violate this.
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: exact regression output changed input=it''s a {0,lower} 'test'! lhs=" + anchorOut + " rhs=it's a dummy test!");
        }

        String anchorPattern = anchor.toPattern();
        if (!"it''s a {0,lower} 'test'!".equals(anchorPattern)) {
            throw new RuntimeException("[oracle:anchor-topattern] metamorphic violation: toPattern must preserve the registered-format pattern input=it''s a {0,lower} 'test'! lhs=" + anchorPattern + " rhs=it''s a {0,lower} 'test'!");
        }

        // EXPLORE: same root-cause property as the patch: a doubled quote outside a
        // quoted section, before a custom format element, so appendQuotedString is
        // invoked with escapingOn=true while positioned on a quote.
        String left = cleanText(data.consumeAsciiString(16));
        String middle = cleanText(data.consumeAsciiString(16));
        String literal = cleanQuotedLiteral(data.consumeAsciiString(16));
        String arg = cleanText(data.consumeAsciiString(16));
        if (arg.length() == 0) {
            arg = "X";
        }

        int variant = data.consumeInt(0, 5);
        String fmt = data.consumeBoolean() ? "lower" : "upper";
        String pattern;
        switch (variant) {
            case 0:
                pattern = nonEmpty(left) + "''" + nonEmpty(middle) + " {0," + fmt + "} '" + literal + "'!";
                break;
            case 1:
                pattern = nonEmpty(left) + "''s a {0," + fmt + "} '" + literal + "'!";
                break;
            case 2:
                pattern = nonEmpty(left) + "''" + nonEmpty(middle) + " {0," + fmt + "}";
                break;
            case 3:
                pattern = nonEmpty(left) + "'' " + "{0," + fmt + "} '" + literal + "'";
                break;
            case 4:
                pattern = "'" + literal + "' " + nonEmpty(left) + "''" + nonEmpty(middle) + " {0," + fmt + "}!";
                break;
            default:
                pattern = nonEmpty(left) + "''" + nonEmpty(middle) + " {0," + fmt + "} '" + literal + "' " + nonEmpty(cleanText(data.consumeAsciiString(8)));
                break;
        }

        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
            String out = emf.format(new Object[] { arg });
            String p1 = emf.toPattern();

            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(p1, registry);
            String p2 = emf2.toPattern();
            if (!safeEquals(p1, p2)) {
                throw new RuntimeException("[oracle:pattern-roundtrip] metamorphic violation: reparsing toPattern must preserve it input=" + pattern + " lhs=" + p1 + " rhs=" + p2);
            }

            String expected;
            if ("lower".equals(fmt)) {
                expected = new MessageFormat(pattern.replace("," + fmt + "}", "}")).format(new Object[] { arg.toLowerCase() });
            } else {
                expected = new MessageFormat(pattern.replace("," + fmt + "}", "}")).format(new Object[] { arg.toUpperCase() });
            }
            if (!safeEquals(out, expected)) {
                throw new RuntimeException("[oracle:format-behavior] metamorphic violation: registered format result disagrees with equivalent transformed MessageFormat input=" + pattern + " lhs=" + out + " rhs=" + expected);
            }
        } catch (IllegalArgumentException e) {
            return;
        } catch (NumberFormatException e) {
            return;
        }
    }

    private static String cleanText(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == '\'') {
                b.append('x');
            } else if (Character.isISOControl(c)) {
                b.append('y');
            } else {
                b.append(c);
            }
        }
        return b.length() == 0 ? "x" : b.toString();
    }

    private static String cleanQuotedLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "test";
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'') {
                b.append("''");
            } else if (c == '{' || c == '}') {
                b.append('q');
            } else if (Character.isISOControl(c)) {
                b.append('r');
            } else {
                b.append(c);
            }
        }
        return b.length() == 0 ? "test" : b.toString();
    }

    private static String nonEmpty(String s) {
        return (s == null || s.length() == 0) ? "x" : s;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/Lang_43_buggy/fuzz/attempt_013/FuzzHarness.java:11: error: cannot find symbol
        registry.put("lower", new LowerCaseFormatFactory());
                                  ^
  symbol:   class LowerCaseFormatFactory
  location: class FuzzHarness
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/Lang_43_buggy/fuzz/attempt_013/FuzzHarness.java:12: error: cannot find symbol
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
<details open><summary>▸ Output (~5,755 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        // ANCHOR: exercise the exact quote pattern shape from the regression through
        // the real public API. On the buggy version, parsing the doubled quote reaches
        // appendQuotedString and can trigger the verified OOME before any formatting.
        String anchorPattern = "it''s a {0} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, registry);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });

        // Contract/post-condition: with no custom formats registered, ExtendedMessageFormat
        // must behave like MessageFormat for the same valid pattern and arguments.
        String anchorExpected = new MessageFormat(anchorPattern).format(new Object[] { "DUMMY" });
        if (!safeEquals(anchorOut, anchorExpected)) {
            throw new RuntimeException("[oracle:anchor-equivalence] metamorphic violation: ExtendedMessageFormat must match MessageFormat on valid built-in pattern input=" + anchorPattern + " lhs=" + anchorOut + " rhs=" + anchorExpected);
        }
        String anchorToPattern = anchor.toPattern();
        String anchorRoundTrip = new ExtendedMessageFormat(anchorToPattern, registry).toPattern();
        if (!safeEquals(anchorToPattern, anchorRoundTrip)) {
            throw new RuntimeException("[oracle:anchor-roundtrip] metamorphic violation: reparsing toPattern must preserve it input=" + anchorPattern + " lhs=" + anchorToPattern + " rhs=" + anchorRoundTrip);
        }

        // EXPLORE: same root-cause property as the patch: parser positioned on a quote
        // that starts an escaped quote sequence outside quoted literals.
        String left = cleanText(data.consumeAsciiString(12));
        String middle = cleanText(data.consumeAsciiString(12));
        String right = cleanText(data.consumeAsciiString(12));
        String quoted = cleanQuotedLiteral(data.consumeAsciiString(12));
        String arg = cleanText(data.consumeAsciiString(12));
        if (arg.length() == 0) {
            arg = "X";
        }

        int variant = data.consumeInt(0, 5);
        String pattern;
        switch (variant) {
            case 0:
                pattern = nonEmpty(left) + "''" + nonEmpty(middle) + " {0}";
                break;
            case 1:
                pattern = nonEmpty(left) + "''s a {0} 'test'!";
                break;
            case 2:
                pattern = nonEmpty(left) + "''" + nonEmpty(middle) + " {0} '" + quoted + "'";
                break;
            case 3:
                pattern = "'" + quoted + "' " + nonEmpty(left) + "''" + nonEmpty(middle) + " {0}";
                break;
            case 4:
                pattern = nonEmpty(left) + " {0} " + nonEmpty(middle) + "''" + nonEmpty(right);
                break;
            default:
                pattern = nonEmpty(left) + "''" + nonEmpty(middle) + " {0} '" + quoted + "' " + nonEmpty(right);
                break;
        }

        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
            String actual = emf.format(new Object[] { arg });
            String expected = new MessageFormat(pattern).format(new Object[] { arg });

            if (!safeEquals(actual, expected)) {
                throw new RuntimeException("[oracle:mf-equivalence] metamorphic violation: ExtendedMessageFormat must match MessageFormat when no custom formats are used input=" + pattern + " lhs=" + actual + " rhs=" + expected);
            }

            String p1 = emf.toPattern();
            String p2 = new ExtendedMessageFormat(p1, registry).toPattern();
            if (!safeEquals(p1, p2)) {
                throw new RuntimeException("[oracle:toPattern-roundtrip] metamorphic violation: reparsing toPattern must preserve it input=" + pattern + " lhs=" + p1 + " rhs=" + p2);
            }
        } catch (IllegalArgumentException e) {
            return;
        } catch (NumberFormatException e) {
            return;
        }
    }

    private static String cleanText(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == '\'') {
                b.append('x');
            } else if (Character.isISOControl(c)) {
                b.append('y');
            } else {
                b.append(c);
            }
        }
        return b.length() == 0 ? "x" : b.toString();
    }

    private static String cleanQuotedLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "q";
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'') {
                b.append("''");
            } else if (c == '{' || c == '}') {
                b.append('q');
            } else if (Character.isISOControl(c)) {
                b.append('r');
            } else {
                b.append(c);
            }
        }
        return b.length() == 0 ? "q" : b.toString();
    }

    private static String nonEmpty(String s) {
        return (s == null || s.length() == 0) ? "x" : s;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
```

</details>

---
## [30] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [31] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (8 message(s), ~7,250 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
- *[assistant] message: identical to step [27] — not reprinted*
- *[user] message: identical to step [5] — not reprinted*
- *[assistant] message: identical to step [29] — not reprinted*
- *[user] message: identical to step [29] — not reprinted*
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

        // ANCHOR: exercise the exact quote pattern shape from the regression through
        // the real public API. On the buggy version, parsing the doubled quote reaches
        // appendQuotedString and can trigger the verified OOME before any formatting.
        String anchorPattern = "it''s a {0} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, registry);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });

        // Contract/post-condition: with no custom formats registered, ExtendedMessageFormat
        // must behave like MessageFormat for the same valid pattern and arguments.
        String anchorExpected = new MessageFormat(anchorPattern).format(new Object[] { "DUMMY" });
        if (!safeEquals(anchorOut, anchorExpected)) {
            throw new RuntimeException("[oracle:anchor-equivalence] metamorphic violation: ExtendedMessageFormat must match MessageFormat on valid built-in pattern input=" + anchorPattern + " lhs=" + anchorOut + " rhs=" + anchorExpected);
        }
        String anchorToPattern = anchor.toPattern();
        String anchorRoundTrip = new ExtendedMessageFormat(anchorToPattern, registry).toPattern();
        if (!safeEquals(anchorToPattern, anchorRoundTrip)) {
            throw new RuntimeException("[oracle:anchor-roundtrip] metamorphic violation: reparsing toPattern must preserve it input=" + anchorPattern + " lhs=" + anchorToPattern + " rhs=" + anchorRoundTrip);
        }

        // EXPLORE: same root-cause property as the patch: parser positioned on a quote
        // that starts an escaped quote sequence outside quoted literals.
        String left = cleanText(data.consumeAsciiString(12));
        String middle = cleanText(data.consumeAsciiString(12));
        String right = cleanText(data.consumeAsciiString(12));
        String quoted = cleanQuotedLiteral(data.consumeAsciiString(12));
        String arg = cleanText(data.consumeAsciiString(12));
        if (arg.length() == 0) {
            arg = "X";
        }

        int variant = data.consumeInt(0, 5);
        String pattern;
        switch (variant) {
            case 0:
                pattern = nonEmpty(left) + "''" + nonEmpty(middle) + " {0}";
                break;
            case 1:
                pattern = nonEmpty(left) + "''s a {0} 'test'!";
                break;
            case 2:
                pattern = nonEmpty(left) + "''" + nonEmpty(middle) + " {0} '" + quoted + "'";
                break;
            case 3:
                pattern = "'" + quoted + "' " + nonEmpty(left) + "''" + nonEmpty(middle) + " {0}";
                break;
            case 4:
                pattern = nonEmpty(left) + " {0} " + nonEmpty(middle) + "''" + nonEmpty(right);
                break;
            default:
                pattern = nonEmpty(left) + "''" + nonEmpty(middle) + " {0} '" + quoted + "' " + nonEmpty(right);
                break;
        }

        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
            String actual = emf.format(new Object[] { arg });
            String expected = new MessageFormat(pattern).format(new Object[] { arg });

            if (!safeEquals(actual, expected)) {
                throw new RuntimeException("[oracle:mf-equivalence] metamorphic violation: ExtendedMessageFormat must match MessageFormat when no custom formats are used input=" + pattern + " lhs=" + actual + " rhs=" + expected);
            }

            String p1 = emf.toPattern();
            String p2 = new ExtendedMessageFormat(p1, registry).toPattern();
            if (!safeEquals(p1, p2)) {
                throw new RuntimeException("[oracle:toPattern-roundtrip] metamorphic violation: reparsing toPattern must preserve it input=" + pattern + " lhs=" + p1 + " rhs=" + p2);
            }
        } catch (IllegalArgumentException e) {
            return;
        } catch (NumberFormatException e) {
            return;
        }
    }

    private static String cleanText(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == '\'') {
                b.append('x');
            } else if (Character.isISOControl(c)) {
                b.append('y');
            } else {
                b.append(c);
            }
        }
        return b.length() == 0 ? "x" : b.toString();
    }

    private static String cleanQuotedLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "q";
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'') {
                b.append("''");
            } else if (c == '{' || c == '}') {
                b.append('q');
            } else if (Character.isISOControl(c)) {
                b.append('r');
            } else {
                b.append(c);
            }
        }
        return b.length() == 0 ? "q" : b.toString();
    }

    private static String nonEmpty(String s) {
        return (s == null || s.length() == 0) ? "x" : s;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/Lang_43_buggy/fuzz/attempt_014/FuzzHarness.java:81: error: exception NumberFormatException has already been caught
        } catch (NumberFormatException e) {
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
<details open><summary>▸ Output (~5,091 chars)</summary>

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
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, registry);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });

        String anchorExpected = new MessageFormat(anchorPattern).format(new Object[] { "DUMMY" });
        if (!safeEquals(anchorOut, anchorExpected)) {
            throw new RuntimeException("[oracle:anchor-equivalence] metamorphic violation: ExtendedMessageFormat must match MessageFormat on valid built-in pattern input=" + anchorPattern + " lhs=" + anchorOut + " rhs=" + anchorExpected);
        }
        String anchorToPattern = anchor.toPattern();
        String anchorRoundTrip = new ExtendedMessageFormat(anchorToPattern, registry).toPattern();
        if (!safeEquals(anchorToPattern, anchorRoundTrip)) {
            throw new RuntimeException("[oracle:anchor-roundtrip] metamorphic violation: reparsing toPattern must preserve it input=" + anchorPattern + " lhs=" + anchorToPattern + " rhs=" + anchorRoundTrip);
        }

        String left = cleanText(data.consumeAsciiString(12));
        String middle = cleanText(data.consumeAsciiString(12));
        String right = cleanText(data.consumeAsciiString(12));
        String quoted = cleanQuotedLiteral(data.consumeAsciiString(12));
        String arg = cleanText(data.consumeAsciiString(12));
        if (arg.length() == 0) {
            arg = "X";
        }

        int variant = data.consumeInt(0, 5);
        String pattern;
        switch (variant) {
            case 0:
                pattern = nonEmpty(left) + "''" + nonEmpty(middle) + " {0}";
                break;
            case 1:
                pattern = nonEmpty(left) + "''s a {0} 'test'!";
                break;
            case 2:
                pattern = nonEmpty(left) + "''" + nonEmpty(middle) + " {0} '" + quoted + "'";
                break;
            case 3:
                pattern = "'" + quoted + "' " + nonEmpty(left) + "''" + nonEmpty(middle) + " {0}";
                break;
            case 4:
                pattern = nonEmpty(left) + " {0} " + nonEmpty(middle) + "''" + nonEmpty(right);
                break;
            default:
                pattern = nonEmpty(left) + "''" + nonEmpty(middle) + " {0} '" + quoted + "' " + nonEmpty(right);
                break;
        }

        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
            String actual = emf.format(new Object[] { arg });
            String expected = new MessageFormat(pattern).format(new Object[] { arg });

            if (!safeEquals(actual, expected)) {
                throw new RuntimeException("[oracle:mf-equivalence] metamorphic violation: ExtendedMessageFormat must match MessageFormat when no custom formats are used input=" + pattern + " lhs=" + actual + " rhs=" + expected);
            }

            String p1 = emf.toPattern();
            String p2 = new ExtendedMessageFormat(p1, registry).toPattern();
            if (!safeEquals(p1, p2)) {
                throw new RuntimeException("[oracle:toPattern-roundtrip] metamorphic violation: reparsing toPattern must preserve it input=" + pattern + " lhs=" + p1 + " rhs=" + p2);
            }
        } catch (IllegalArgumentException e) {
            return;
        }
    }

    private static String cleanText(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == '\'') {
                b.append('x');
            } else if (Character.isISOControl(c)) {
                b.append('y');
            } else {
                b.append(c);
            }
        }
        return b.length() == 0 ? "x" : b.toString();
    }

    private static String cleanQuotedLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "q";
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'') {
                b.append("''");
            } else if (c == '{' || c == '}') {
                b.append('q');
            } else if (Character.isISOControl(c)) {
                b.append('r');
            } else {
                b.append(c);
            }
        }
        return b.length() == 0 ? "q" : b.toString();
    }

    private static String nonEmpty(String s) {
        return (s == null || s.length() == 0) ? "x" : s;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
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
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,993 chars)</summary>

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
    private static final Map REGISTRY = buildRegistry();

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String anchorPattern = "it''s a {0,lower} 'test'!";
        runCase(anchorPattern, "DUMMY", "it's a dummy test!", anchorPattern);

        String left = sanitizeLiteral(data.consumeAsciiString(16));
        String mid = sanitizeLiteral(data.consumeAsciiString(16));
        String between = sanitizeLiteral(data.consumeAsciiString(16));
        String quoted = sanitizeQuotedLiteral(data.consumeAsciiString(16));
        String right = sanitizeLiteral(data.consumeAsciiString(16));
        String arg = data.consumeAsciiString(24);
        if (arg.length() == 0) {
            arg = "X";
        }
        if (quoted.length() == 0) {
            quoted = "q";
        }

        String fmt = data.consumeBoolean() ? "lower" : "upper";
        String pattern = left + "''" + mid + "{0," + fmt + "}" + between + "'" + quoted + "'" + right;
        String expected = left + "'" + mid + transform(fmt, arg) + between + quoted + right;

        runCase(pattern, arg, expected, pattern);
    }

    private static void runCase(String pattern, String arg, String expected, String expectedToPattern) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, Locale.getDefault(), REGISTRY);

            String before = emf.toPattern();
            Object[] args = new Object[] { arg };
            String formatted = emf.format(args);
            String after = emf.toPattern();

            if (!expectedToPattern.equals(before) || !expectedToPattern.equals(after)) {
                throw new RuntimeException(
                    "[oracle:topattern] metamorphic violation: valid ExtendedMessageFormat should preserve its pattern via toPattern() input="
                        + pattern + " lhsBefore=" + before + " lhsAfter=" + after + " rhs=" + expectedToPattern);
            }

            /*
             * Contract/oracle: for these inputs we construct a valid pattern by construction:
             * - "''" is the MessageFormat escape for a literal single quote
             * - "'" + quoted + "'" is a properly terminated quoted literal
             * - "{0,lower}" / "{0,upper}" uses a registered custom format
             * Therefore a correct implementation must format the single argument into the
             * corresponding lower/upper-cased output while preserving the literals.
             * A patch that merely suppresses the failing branch or skips quote bookkeeping
             * can avoid the crash yet produce the wrong formatted text, which this catches.
             */
            if (!expected.equals(formatted)) {
                throw new RuntimeException(
                    "[oracle:format] metamorphic violation: oracle from constructed valid pattern input="
                        + pattern + " arg=" + arg + " lhs=" + formatted + " rhs=" + expected);
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && isOracleFailure((RuntimeException) t)) {
                throw (RuntimeException) t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static boolean isOracleFailure(RuntimeException t) {
        String msg = t.getMessage();
        return msg != null && msg.startsWith("[oracle:");
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
        for (StackTraceElement ste : stack) {
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                && "appendQuotedString".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof Error) {
            throw (Error) t;
        }
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        throw new RuntimeException(t);
    }

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

    private static String sanitizeQuotedLiteral(String s) {
        return sanitizeLiteral(s);
    }

    private static String transform(String fmt, String arg) {
        Locale locale = Locale.getDefault();
        if ("upper".equals(fmt)) {
            return arg.toUpperCase(locale);
        }
        return arg.toLowerCase(locale);
    }

    private static Map buildRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new CaseFormat(false, locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new CaseFormat(true, locale);
        }
    }

    private static final class CaseFormat extends Format {
        private final boolean upper;
        private final Locale locale;

        private CaseFormat(boolean upper, Locale locale) {
            this.upper = upper;
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(upper ? s.toUpperCase(locale) : s.toLowerCase(locale));
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
## [34] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [35] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~7,190 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
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
    private static final Map REGISTRY = buildRegistry();

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String anchorPattern = "it''s a {0,lower} 'test'!";
        runCase(anchorPattern, "DUMMY", "it's a dummy test!", anchorPattern);

        String left = sanitizeLiteral(data.consumeAsciiString(16));
        String mid = sanitizeLiteral(data.consumeAsciiString(16));
        String between = sanitizeLiteral(data.consumeAsciiString(16));
        String quoted = sanitizeQuotedLiteral(data.consumeAsciiString(16));
        String right = sanitizeLiteral(data.consumeAsciiString(16));
        String arg = data.consumeAsciiString(24);
        if (arg.length() == 0) {
            arg = "X";
        }
        if (quoted.length() == 0) {
            quoted = "q";
        }

        String fmt = data.consumeBoolean() ? "lower" : "upper";
        String pattern = left + "''" + mid + "{0," + fmt + "}" + between + "'" + quoted + "'" + right;
        String expected = left + "'" + mid + transform(fmt, arg) + between + quoted + right;

        runCase(pattern, arg, expected, pattern);
    }

    private static void runCase(String pattern, String arg, String expected, String expectedToPattern) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, Locale.getDefault(), REGISTRY);

            String before = emf.toPattern();
            Object[] args = new Object[] { arg };
            String formatted = emf.format(args);
            String after = emf.toPattern();

            if (!expectedToPattern.equals(before) || !expectedToPattern.equals(after)) {
                throw new RuntimeException(
                    "[oracle:topattern] metamorphic violation: valid ExtendedMessageFormat should preserve its pattern via toPattern() input="
                        + pattern + " lhsBefore=" + before + " lhsAfter=" + after + " rhs=" + expectedToPattern);
            }

            /*
             * Contract/oracle: for these inputs we construct a valid pattern by construction:
             * - "''" is the MessageFormat escape for a literal single quote
             * - "'" + quoted + "'" is a properly terminated quoted literal
             * - "{0,lower}" / "{0,upper}" uses a registered custom format
             * Therefore a correct implementation must format the single argument into the
             * corresponding lower/upper-cased output while preserving the literals.
             * A patch that merely suppresses the failing branch or skips quote bookkeeping
             * can avoid the crash yet produce the wrong formatted text, which this catches.
             */
            if (!expected.equals(formatted)) {
                throw new RuntimeException(
                    "[oracle:format] metamorphic violation: oracle from constructed valid pattern input="
                        + pattern + " arg=" + arg + " lhs=" + formatted + " rhs=" + expected);
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && isOracleFailure((RuntimeException) t)) {
                throw (RuntimeException) t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static boolean isOracleFailure(RuntimeException t) {
        String msg = t.getMessage();
        return msg != null && msg.startsWith("[oracle:");
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
        for (StackTraceElement ste : stack) {
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                && "appendQuotedString".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof Error) {
            throw (Error) t;
        }
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        throw new RuntimeException(t);
    }

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

    private static String sanitizeQuotedLiteral(String s) {
        return sanitizeLiteral(s);
    }

    private static String transform(String fmt, String arg) {
        Locale locale = Locale.getDefault();
        if ("upper".equals(fmt)) {
            return arg.toUpperCase(locale);
        }
        return arg.toLowerCase(locale);
    }

    private static Map buildRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new CaseFormat(false, locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new CaseFormat(true, locale);
        }
    }

    private static final class CaseFormat extends Format {
        private final boolean upper;
        private final Locale locale;

        private CaseFormat(boolean upper, Locale locale) {
            this.upper = upper;
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(upper ? s.toUpperCase(locale) : s.toLowerCase(locale));
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
<details open><summary>▸ Output (~5,458 chars)</summary>

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
        anchor();

        String prefix = clean(data.consumeAsciiString(12));
        String infix = clean(data.consumeAsciiString(12));
        String quoted = cleanNonEmpty(data.consumeAsciiString(12));
        String suffix = clean(data.consumeAsciiString(12));
        String arg = data.consumeAsciiString(20);
        if (arg.length() == 0) {
            arg = "DUMMY";
        }

        String formatName = data.consumeBoolean() ? "lower" : "upper";

        String pattern = prefix + "''" + infix + "{0," + formatName + "}" + " '" + quoted + "'" + suffix;
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);

        String expected = prefix
                + "'"
                + infix
                + ("lower".equals(formatName) ? arg.toLowerCase(Locale.getDefault()) : arg.toUpperCase(Locale.getDefault()))
                + " "
                + quoted
                + suffix;

        String before = emf.toPattern();
        String actual = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        /*
         * Contract asserted:
         * For these patterns, the quotes are balanced and the custom format name is registered,
         * so construction and formatting are valid by construction. A correct implementation must
         * preserve the parsed pattern in toPattern() and must render doubled quote '' as a single
         * literal quote while preserving the quoted literal section.
         */
        if (!pattern.equals(before) || !pattern.equals(after)) {
            throw new RuntimeException(
                    "[oracle:topattern] metamorphic violation: valid pattern must round-trip through toPattern input="
                            + pattern + " lhsBefore=" + before + " lhsAfter=" + after + " rhs=" + pattern);
        }
        if (!expected.equals(actual)) {
            throw new RuntimeException(
                    "[oracle:format] metamorphic violation: valid constructed pattern must format to the known literal/argument composition input="
                            + pattern + " arg=" + arg + " lhs=" + actual + " rhs=" + expected);
        }
    }

    private static void anchor() {
        String pattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);
        String actual = emf.format(new Object[] { "DUMMY" });
        String expected = "it's a dummy test!";
        if (!pattern.equals(emf.toPattern())) {
            throw new RuntimeException(
                    "[oracle:anchor-topattern] metamorphic violation: anchor pattern must be preserved input="
                            + pattern + " lhs=" + emf.toPattern() + " rhs=" + pattern);
        }
        if (!expected.equals(actual)) {
            throw new RuntimeException(
                    "[oracle:anchor-format] metamorphic violation: anchor formatting must match the regression test input="
                            + pattern + " lhs=" + actual + " rhs=" + expected);
        }
    }

    private static String clean(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\'' && c != '{' && c != '}') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String cleanNonEmpty(String s) {
        String out = clean(s);
        return out.length() == 0 ? "test" : out;
    }

    private static Map createRegistry() {
        Map map = new HashMap();
        map.put("lower", new LowerCaseFormatFactory());
        map.put("upper", new UpperCaseFormatFactory());
        return map;
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new CaseFormat(false, locale == null ? Locale.getDefault() : locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new CaseFormat(true, locale == null ? Locale.getDefault() : locale);
        }
    }

    private static final class CaseFormat extends Format {
        private final boolean upper;
        private final Locale locale;

        private CaseFormat(boolean upper, Locale locale) {
            this.upper = upper;
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(upper ? s.toUpperCase(locale) : s.toLowerCase(locale));
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
## [36] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [37] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~5,775 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
- *[assistant] message: identical to step [35] — not reprinted*
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
        anchor();

        String prefix = clean(data.consumeAsciiString(12));
        String infix = clean(data.consumeAsciiString(12));
        String quoted = cleanNonEmpty(data.consumeAsciiString(12));
        String suffix = clean(data.consumeAsciiString(12));
        String arg = data.consumeAsciiString(20);
        if (arg.length() == 0) {
            arg = "DUMMY";
        }

        String formatName = data.consumeBoolean() ? "lower" : "upper";

        String pattern = prefix + "''" + infix + "{0," + formatName + "}" + " '" + quoted + "'" + suffix;
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);

        String expected = prefix
                + "'"
                + infix
                + ("lower".equals(formatName) ? arg.toLowerCase(Locale.getDefault()) : arg.toUpperCase(Locale.getDefault()))
                + " "
                + quoted
                + suffix;

        String before = emf.toPattern();
        String actual = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        /*
         * Contract asserted:
         * For these patterns, the quotes are balanced and the custom format name is registered,
         * so construction and formatting are valid by construction. A correct implementation must
         * preserve the parsed pattern in toPattern() and must render doubled quote '' as a single
         * literal quote while preserving the quoted literal section.
         */
        if (!pattern.equals(before) || !pattern.equals(after)) {
            throw new RuntimeException(
                    "[oracle:topattern] metamorphic violation: valid pattern must round-trip through toPattern input="
                            + pattern + " lhsBefore=" + before + " lhsAfter=" + after + " rhs=" + pattern);
        }
        if (!expected.equals(actual)) {
            throw new RuntimeException(
                    "[oracle:format] metamorphic violation: valid constructed pattern must format to the known literal/argument composition input="
                            + pattern + " arg=" + arg + " lhs=" + actual + " rhs=" + expected);
        }
    }

    private static void anchor() {
        String pattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);
        String actual = emf.format(new Object[] { "DUMMY" });
        String expected = "it's a dummy test!";
        if (!pattern.equals(emf.toPattern())) {
            throw new RuntimeException(
                    "[oracle:anchor-topattern] metamorphic violation: anchor pattern must be preserved input="
                            + pattern + " lhs=" + emf.toPattern() + " rhs=" + pattern);
        }
        if (!expected.equals(actual)) {
            throw new RuntimeException(
                    "[oracle:anchor-format] metamorphic violation: anchor formatting must match the regression test input="
                            + pattern + " lhs=" + actual + " rhs=" + expected);
        }
    }

    private static String clean(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\'' && c != '{' && c != '}') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String cleanNonEmpty(String s) {
        String out = clean(s);
        return out.length() == 0 ? "test" : out;
    }

    private static Map createRegistry() {
        Map map = new HashMap();
        map.put("lower", new LowerCaseFormatFactory());
        map.put("upper", new UpperCaseFormatFactory());
        return map;
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new CaseFormat(false, locale == null ? Locale.getDefault() : locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new CaseFormat(true, locale == null ? Locale.getDefault() : locale);
        }
    }

    private static final class CaseFormat extends Format {
        private final boolean upper;
        private final Locale locale;

        private CaseFormat(boolean upper, Locale locale) {
            this.upper = upper;
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(upper ? s.toUpperCase(locale) : s.toLowerCase(locale));
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
<details open><summary>▸ Output (~4,703 chars)</summary>

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
        registry.put("lower", new SimpleFormatFactory(false));
        registry.put("upper", new SimpleFormatFactory(true));

        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, registry);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });

        /*
         * Contract asserted: for a valid pattern, toPattern() should preserve the pattern and
         * formatting should honor doubled quotes as a literal single quote plus the registered
         * custom format. A throw-deleting patch in quote handling could avoid the crash but
         * silently change parsing/formatting; these checks catch that.
         */
        if (!anchorPattern.equals(anchor.toPattern())) {
            throw new RuntimeException(
                "[oracle:anchor-topattern] metamorphic violation: valid pattern must round-trip through toPattern input="
                    + anchorPattern + " lhs=" + anchor.toPattern() + " rhs=" + anchorPattern);
        }
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException(
                "[oracle:anchor-format] metamorphic violation: anchor regression pattern must format as expected input="
                    + anchorPattern + " lhs=" + anchorOut + " rhs=it's a dummy test!");
        }

        String pre = stripSpecial(data.consumeAsciiString(8));
        String mid = stripSpecial(data.consumeAsciiString(8));
        String quoted = stripSpecial(data.consumeAsciiString(8));
        String post = stripSpecial(data.consumeAsciiString(8));
        String arg = data.consumeAsciiString(16);
        if (arg.length() == 0) {
            arg = "X";
        }
        if (quoted.length() == 0) {
            quoted = "q";
        }

        String fmt = data.consumeBoolean() ? "lower" : "upper";

        String pattern = pre + "''" + mid + " {0," + fmt + "} '" + quoted + "'" + post;
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String actual = emf.format(new Object[] { arg });

        String transformed = "lower".equals(fmt)
            ? arg.toLowerCase(Locale.getDefault())
            : arg.toUpperCase(Locale.getDefault());
        String expected = pre + "'" + mid + " " + transformed + " " + quoted + post;

        if (!pattern.equals(emf.toPattern())) {
            throw new RuntimeException(
                "[oracle:topattern] metamorphic violation: valid pattern must round-trip through toPattern input="
                    + pattern + " lhs=" + emf.toPattern() + " rhs=" + pattern);
        }
        if (!expected.equals(actual)) {
            throw new RuntimeException(
                "[oracle:format] metamorphic violation: constructed valid pattern must preserve quote semantics and registered format input="
                    + pattern + " arg=" + arg + " lhs=" + actual + " rhs=" + expected);
        }
    }

    private static String stripSpecial(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\'' && c != '{' && c != '}') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static final class SimpleFormatFactory implements FormatFactory {
        private final boolean upper;

        SimpleFormatFactory(boolean upper) {
            this.upper = upper;
        }

        public Format getFormat(String name, String args, Locale locale) {
            return new Format() {
                public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                    String s = String.valueOf(obj);
                    Locale useLocale = locale == null ? Locale.getDefault() : locale;
                    toAppendTo.append(upper ? s.toUpperCase(useLocale) : s.toLowerCase(useLocale));
                    return toAppendTo;
                }

                public Object parseObject(String source, ParsePosition pos) {
                    int start = pos.getIndex();
                    pos.setIndex(source.length());
                    return source.substring(start);
                }
            };
        }
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
<details><summary>▸ Prompt (8 message(s), ~5,140 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
- *[assistant] message: identical to step [35] — not reprinted*
- *[user] message: identical to step [5] — not reprinted*
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
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("lower", new SimpleFormatFactory(false));
        registry.put("upper", new SimpleFormatFactory(true));

        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, registry);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });

        /*
         * Contract asserted: for a valid pattern, toPattern() should preserve the pattern and
         * formatting should honor doubled quotes as a literal single quote plus the registered
         * custom format. A throw-deleting patch in quote handling could avoid the crash but
         * silently change parsing/formatting; these checks catch that.
         */
        if (!anchorPattern.equals(anchor.toPattern())) {
            throw new RuntimeException(
                "[oracle:anchor-topattern] metamorphic violation: valid pattern must round-trip through toPattern input="
                    + anchorPattern + " lhs=" + anchor.toPattern() + " rhs=" + anchorPattern);
        }
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException(
                "[oracle:anchor-format] metamorphic violation: anchor regression pattern must format as expected input="
                    + anchorPattern + " lhs=" + anchorOut + " rhs=it's a dummy test!");
        }

        String pre = stripSpecial(data.consumeAsciiString(8));
        String mid = stripSpecial(data.consumeAsciiString(8));
        String quoted = stripSpecial(data.consumeAsciiString(8));
        String post = stripSpecial(data.consumeAsciiString(8));
        String arg = data.consumeAsciiString(16);
        if (arg.length() == 0) {
            arg = "X";
        }
        if (quoted.length() == 0) {
            quoted = "q";
        }

        String fmt = data.consumeBoolean() ? "lower" : "upper";

        String pattern = pre + "''" + mid + " {0," + fmt + "} '" + quoted + "'" + post;
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String actual = emf.format(new Object[] { arg });

        String transformed = "lower".equals(fmt)
            ? arg.toLowerCase(Locale.getDefault())
            : arg.toUpperCase(Locale.getDefault());
        String expected = pre + "'" + mid + " " + transformed + " " + quoted + post;

        if (!pattern.equals(emf.toPattern())) {
            throw new RuntimeException(
                "[oracle:topattern] metamorphic violation: valid pattern must round-trip through toPattern input="
                    + pattern + " lhs=" + emf.toPattern() + " rhs=" + pattern);
        }
        if (!expected.equals(actual)) {
            throw new RuntimeException(
                "[oracle:format] metamorphic violation: constructed valid pattern must preserve quote semantics and registered format input="
                    + pattern + " arg=" + arg + " lhs=" + actual + " rhs=" + expected);
        }
    }

    private static String stripSpecial(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\'' && c != '{' && c != '}') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static final class SimpleFormatFactory implements FormatFactory {
        private final boolean upper;

        SimpleFormatFactory(boolean upper) {
            this.upper = upper;
        }

        public Format getFormat(String name, String args, Locale locale) {
            return new Format() {
                public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                    String s = String.valueOf(obj);
                    Locale useLocale = locale == null ? Locale.getDefault() : locale;
                    toAppendTo.append(upper ? s.toUpperCase(useLocale) : s.toLowerCase(useLocale));
                    return toAppendTo;
                }

                public Object parseObject(String source, ParsePosition pos) {
                    int start = pos.getIndex();
                    pos.setIndex(source.length());
                    return source.substring(start);
                }
            };
        }
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~5,427 chars)</summary>

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

        /*
         * Contract asserted: this exact valid regression pattern must parse and format as in the
         * original test. If a patch merely suppresses the buggy quote-handling path, it can avoid
         * the crash yet still produce the wrong parsed pattern or wrong formatted output.
         */
        String anchorFormatted = anchor.format(new Object[] { "DUMMY" });
        if (!anchorPattern.equals(anchor.toPattern())) {
            throw new RuntimeException(
                "[oracle:anchor-topattern] metamorphic violation: valid pattern must round-trip through toPattern input="
                    + anchorPattern + " lhs=" + anchor.toPattern() + " rhs=" + anchorPattern);
        }
        if (!"it's a dummy test!".equals(anchorFormatted)) {
            throw new RuntimeException(
                "[oracle:anchor-format] metamorphic violation: regression pattern must format as expected input="
                    + anchorPattern + " lhs=" + anchorFormatted + " rhs=it's a dummy test!");
        }

        String pre = clean(data.consumeAsciiString(10));
        String mid = clean(data.consumeAsciiString(10));
        String post = clean(data.consumeAsciiString(10));
        String quoted = clean(data.consumeAsciiString(10));
        if (quoted.length() == 0) {
            quoted = "test";
        }
        String arg = data.consumeAsciiString(20);
        if (arg.length() == 0) {
            arg = "Dummy";
        }
        String fmt = data.consumeBoolean() ? "lower" : "upper";

        /*
         * Targeted exploration:
         * The diff shows the buggy/fixed difference is whether ParsePosition advances before
         * handling an escaped quote. So we always generate patterns containing a doubled quote
         * outside a format element: ...''...{0,lower|upper}...'literal'...
         * These are valid-by-construction and drive the real constructor/applyPattern path.
         */
        String pattern = pre + "''" + mid + " {0," + fmt + "} '" + quoted + "'" + post;
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);

        String actual = emf.format(new Object[] { arg });
        String expectedArg = "lower".equals(fmt)
            ? arg.toLowerCase(Locale.getDefault())
            : arg.toUpperCase(Locale.getDefault());
        String expected = pre + "'" + mid + " " + expectedArg + " " + quoted + post;

        if (!pattern.equals(emf.toPattern())) {
            throw new RuntimeException(
                "[oracle:topattern] metamorphic violation: valid pattern must round-trip through toPattern input="
                    + pattern + " lhs=" + emf.toPattern() + " rhs=" + pattern);
        }
        if (!expected.equals(actual)) {
            throw new RuntimeException(
                "[oracle:format] metamorphic violation: doubled quote and quoted literal semantics must be preserved input="
                    + pattern + " arg=" + arg + " lhs=" + actual + " rhs=" + expected);
        }
    }

    private static String clean(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\'' && c != '{' && c != '}') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static Map makeRegistry() {
        Map map = new HashMap();
        map.put("lower", new LowerCaseFormatFactory());
        map.put("upper", new UpperCaseFormatFactory());
        return map;
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new CaseFormat(false, locale == null ? Locale.getDefault() : locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new CaseFormat(true, locale == null ? Locale.getDefault() : locale);
        }
    }

    private static final class CaseFormat extends Format {
        private final boolean upper;
        private final Locale locale;

        private CaseFormat(boolean upper, Locale locale) {
            this.upper = upper;
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(upper ? s.toUpperCase(locale) : s.toLowerCase(locale));
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
## [40] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [41] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,557 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final String anchorPattern = "it''s a {0,lower} 'test'!";

        try {
            Map anchorRegistry = new HashMap();
            new ExtendedMessageFormat(anchorPattern, anchorRegistry);
        } catch (Throwable t) {
            boolean inPatchedMethod = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                        && "appendQuotedString".equals(ste.getMethodName())) {
                    inPatchedMethod = true;
                    break;
                }
            }
            if (t instanceof OutOfMemoryError && inPatchedMethod) {
                throw (OutOfMemoryError) t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
        }

        String pre = data.consumeAsciiString(20).replace("'", "''").replace("{", "(").replace("}", ")");
        String mid = data.consumeAsciiString(20).replace("'", "''").replace("{", "(").replace("}", ")");
        String quoted = data.consumeAsciiString(20).replace("'", "''");
        String post = data.consumeAsciiString(20).replace("'", "''").replace("{", "(").replace("}", ")");
        String arg = data.consumeString(20);

        int variant = data.consumeInt(0, 3);
        String pattern;
        switch (variant) {
            case 0:
                pattern = pre + "''" + mid + " {0} '" + quoted + "'" + post;
                break;
            case 1:
                pattern = "'" + quoted + "' " + pre + "''" + mid + " {0}" + post;
                break;
            case 2:
                pattern = pre + " {0} " + mid + "''" + post + " '" + quoted + "'";
                break;
            default:
                pattern = pre + "''" + mid + " '" + quoted + "' {0} " + post;
                break;
        }

        ExtendedMessageFormat emf;
        String emfFormatted;
        String emfPattern;
        try {
            emf = new ExtendedMessageFormat(pattern);
            emfPattern = emf.toPattern();
            emfFormatted = emf.format(new Object[] { arg });
        } catch (Throwable t) {
            boolean inPatchedMethod = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                        && "appendQuotedString".equals(ste.getMethodName())) {
                    inPatchedMethod = true;
                    break;
                }
            }
            if (t instanceof OutOfMemoryError && inPatchedMethod) {
                throw (OutOfMemoryError) t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            return;
        }

        String mfFormatted;
        String mfPattern;
        try {
            MessageFormat mf = new MessageFormat(pattern);
            mfPattern = mf.toPattern();
            mfFormatted = mf.format(new Object[] { arg });
        } catch (Throwable t) {
            return;
        }

        /*
         * Contract/oracle:
         * ExtendedMessageFormat is a MessageFormat extension. With no custom registry/formats,
         * valid patterns must behave like MessageFormat. A patch that merely skips quote parsing
         * bookkeeping could stop the crash but still produce a different parsed pattern/output.
         */
        if (!emfFormatted.equals(mfFormatted)) {
            throw new RuntimeException(
                    "[oracle:msgfmt-eq] metamorphic violation: ExtendedMessageFormat.format must match MessageFormat.format for patterns without custom formats input="
                            + pattern + " lhs=" + emfFormatted + " rhs=" + mfFormatted);
        }
        if (!emfPattern.equals(mfPattern)) {
            throw new RuntimeException(
                    "[oracle:topattern-eq] metamorphic violation: ExtendedMessageFormat.toPattern must match MessageFormat.toPattern for patterns without custom formats input="
                            + pattern + " lhs=" + emfPattern + " rhs=" + mfPattern);
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
<details><summary>▸ Prompt (4 message(s), ~4,754 chars, 1 new)</summary>

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
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final String anchorPattern = "it''s a {0,lower} 'test'!";

        try {
            Map anchorRegistry = new HashMap();
            new ExtendedMessageFormat(anchorPattern, anchorRegistry);
        } catch (Throwable t) {
            boolean inPatchedMethod = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                        && "appendQuotedString".equals(ste.getMethodName())) {
                    inPatchedMethod = true;
                    break;
                }
            }
            if (t instanceof OutOfMemoryError && inPatchedMethod) {
                throw (OutOfMemoryError) t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
        }

        String pre = data.consumeAsciiString(20).replace("'", "''").replace("{", "(").replace("}", ")");
        String mid = data.consumeAsciiString(20).replace("'", "''").replace("{", "(").replace("}", ")");
        String quoted = data.consumeAsciiString(20).replace("'", "''");
        String post = data.consumeAsciiString(20).replace("'", "''").replace("{", "(").replace("}", ")");
        String arg = data.consumeString(20);

        int variant = data.consumeInt(0, 3);
        String pattern;
        switch (variant) {
            case 0:
                pattern = pre + "''" + mid + " {0} '" + quoted + "'" + post;
                break;
            case 1:
                pattern = "'" + quoted + "' " + pre + "''" + mid + " {0}" + post;
                break;
            case 2:
                pattern = pre + " {0} " + mid + "''" + post + " '" + quoted + "'";
                break;
            default:
                pattern = pre + "''" + mid + " '" + quoted + "' {0} " + post;
                break;
        }

        ExtendedMessageFormat emf;
        String emfFormatted;
        String emfPattern;
        try {
            emf = new ExtendedMessageFormat(pattern);
            emfPattern = emf.toPattern();
            emfFormatted = emf.format(new Object[] { arg });
        } catch (Throwable t) {
            boolean inPatchedMethod = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                        && "appendQuotedString".equals(ste.getMethodName())) {
                    inPatchedMethod = true;
                    break;
                }
            }
            if (t instanceof OutOfMemoryError && inPatchedMethod) {
                throw (OutOfMemoryError) t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            return;
        }

        String mfFormatted;
        String mfPattern;
        try {
            MessageFormat mf = new MessageFormat(pattern);
            mfPattern = mf.toPattern();
            mfFormatted = mf.format(new Object[] { arg });
        } catch (Throwable t) {
            return;
        }

        /*
         * Contract/oracle:
         * ExtendedMessageFormat is a MessageFormat extension. With no custom registry/formats,
         * valid patterns must behave like MessageFormat. A patch that merely skips quote parsing
         * bookkeeping could stop the crash but still produce a different parsed pattern/output.
         */
        if (!emfFormatted.equals(mfFormatted)) {
            throw new RuntimeException(
                    "[oracle:msgfmt-eq] metamorphic violation: ExtendedMessageFormat.format must match MessageFormat.format for patterns without custom formats input="
                            + pattern + " lhs=" + emfFormatted + " rhs=" + mfFormatted);
        }
        if (!emfPattern.equals(mfPattern)) {
            throw new RuntimeException(
                    "[oracle:topattern-eq] metamorphic violation: ExtendedMessageFormat.toPattern must match MessageFormat.toPattern for patterns without custom formats input="
                            + pattern + " lhs=" + emfPattern + " rhs=" + mfPattern);
        }
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~5,220 chars)</summary>

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

        LowerCaseFormat(Locale locale) {
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(s.toLowerCase(this.locale));
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
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(s.toUpperCase(this.locale));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }

    private static Map makeRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static String sanitizeLiteral(String s) {
        return s.replace("{", "(").replace("}", ")").replace("'", "");
    }

    private static String sanitizeQuotedBody(String s) {
        return s.replace("'", "''").replace("{", "(").replace("}", ")");
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = makeRegistry();

        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, registry);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: known regression test output changed input="
                    + anchorPattern + " lhs=" + anchorOut + " rhs=it's a dummy test!");
        }

        String prefix = sanitizeLiteral(data.consumeAsciiString(16));
        String infix = sanitizeLiteral(data.consumeAsciiString(16));
        String quoted = sanitizeQuotedBody(data.consumeAsciiString(16));
        String suffix = sanitizeLiteral(data.consumeAsciiString(16));
        String arg = data.consumeString(24);

        int variant = data.consumeInt(0, 3);
        String pattern;
        if (variant == 0) {
            pattern = prefix + "''" + infix + " {0,lower} '" + quoted + "'" + suffix;
        } else if (variant == 1) {
            pattern = "'" + quoted + "' " + prefix + "''" + infix + " {0,lower}" + suffix;
        } else if (variant == 2) {
            pattern = prefix + " {0,lower} " + infix + "''" + suffix + " '" + quoted + "'";
        } else {
            pattern = prefix + "''" + infix + " '{"
                    + quoted.replace("{", "").replace("}", "") + "}' {0,lower} " + suffix;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String actual = emf.format(new Object[] { arg });

        /*
         * Contract/oracle: with exactly one custom {0,lower} element, formatting must be equivalent
         * to formatting the same pattern with plain MessageFormat after replacing {0,lower} by {0}
         * and lowercasing the argument up front. A patch that merely avoids the buggy quote-parsing
         * loop but loses/skips quote handling would change the observable formatted result.
         */
        String referencePattern = pattern.replace("{0,lower}", "{0}");
        String expected = new MessageFormat(referencePattern).format(new Object[] {
                String.valueOf(arg).toLowerCase()
        });
        if (!actual.equals(expected)) {
            throw new RuntimeException("[oracle:lower-eq] metamorphic violation: ExtendedMessageFormat with lower must match MessageFormat on pre-lowercased argument input="
                    + pattern + " lhs=" + actual + " rhs=" + expected);
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
<details><summary>▸ Prompt (6 message(s), ~5,537 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
- *[assistant] message: identical to step [43] — not reprinted*
- *[user] message: identical to step [5] — not reprinted*
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

        LowerCaseFormat(Locale locale) {
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(s.toLowerCase(this.locale));
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
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(s.toUpperCase(this.locale));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }

    private static Map makeRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static String sanitizeLiteral(String s) {
        return s.replace("{", "(").replace("}", ")").replace("'", "");
    }

    private static String sanitizeQuotedBody(String s) {
        return s.replace("'", "''").replace("{", "(").replace("}", ")");
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = makeRegistry();

        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, registry);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: known regression test output changed input="
                    + anchorPattern + " lhs=" + anchorOut + " rhs=it's a dummy test!");
        }

        String prefix = sanitizeLiteral(data.consumeAsciiString(16));
        String infix = sanitizeLiteral(data.consumeAsciiString(16));
        String quoted = sanitizeQuotedBody(data.consumeAsciiString(16));
        String suffix = sanitizeLiteral(data.consumeAsciiString(16));
        String arg = data.consumeString(24);

        int variant = data.consumeInt(0, 3);
        String pattern;
        if (variant == 0) {
            pattern = prefix + "''" + infix + " {0,lower} '" + quoted + "'" + suffix;
        } else if (variant == 1) {
            pattern = "'" + quoted + "' " + prefix + "''" + infix + " {0,lower}" + suffix;
        } else if (variant == 2) {
            pattern = prefix + " {0,lower} " + infix + "''" + suffix + " '" + quoted + "'";
        } else {
            pattern = prefix + "''" + infix + " '{"
                    + quoted.replace("{", "").replace("}", "") + "}' {0,lower} " + suffix;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String actual = emf.format(new Object[] { arg });

        /*
         * Contract/oracle: with exactly one custom {0,lower} element, formatting must be equivalent
         * to formatting the same pattern with plain MessageFormat after replacing {0,lower} by {0}
         * and lowercasing the argument up front. A patch that merely avoids the buggy quote-parsing
         * loop but loses/skips quote handling would change the observable formatted result.
         */
        String referencePattern = pattern.replace("{0,lower}", "{0}");
        String expected = new MessageFormat(referencePattern).format(new Object[] {
                String.valueOf(arg).toLowerCase()
        });
        if (!actual.equals(expected)) {
            throw new RuntimeException("[oracle:lower-eq] metamorphic violation: ExtendedMessageFormat with lower must match MessageFormat on pre-lowercased argument input="
                    + pattern + " lhs=" + actual + " rhs=" + expected);
        }
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~4,640 chars)</summary>

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
            toAppendTo.append(String.valueOf(obj).toLowerCase(locale));
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
            toAppendTo.append(String.valueOf(obj).toUpperCase(locale));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source;
        }
    }

    private static Map makeRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static String noBracesNoQuotes(String s) {
        return s.replace("'", "").replace("{", "").replace("}", "");
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = makeRegistry();

        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, registry);
        String anchorResult = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorResult)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: known regression output changed input="
                    + anchorPattern + " lhs=" + anchorResult + " rhs=it's a dummy test!");
        }

        String prefix = noBracesNoQuotes(data.consumeAsciiString(12));
        String middle = noBracesNoQuotes(data.consumeAsciiString(12));
        String quoted = noBracesNoQuotes(data.consumeAsciiString(12));
        String suffix = noBracesNoQuotes(data.consumeAsciiString(12));
        String arg = data.consumeString(16);

        int which = data.consumeInt(0, 3);
        String pattern;
        if (which == 0) {
            pattern = prefix + "''" + middle + " {0,lower} '" + quoted + "'!" + suffix;
        } else if (which == 1) {
            pattern = prefix + "''s " + middle + "{0,lower} '" + quoted + "'" + suffix;
        } else if (which == 2) {
            pattern = prefix + "''" + middle + " and {0,upper} '" + quoted + "' " + suffix;
        } else {
            pattern = prefix + "''" + middle + " {0,lower} '" + quoted + "' " + suffix;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String before = emf.toPattern();
        String formatted1 = emf.format(new Object[] { arg });
        String after = emf.toPattern();
        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: formatting must not mutate toPattern input="
                    + pattern + " lhs=" + before + " rhs=" + after);
        }

        String formatted2 = emf.format(new Object[] { arg });
        if (!formatted1.equals(formatted2)) {
            throw new RuntimeException("[oracle:format-idempotent] metamorphic violation: repeated formatting with same argument must be stable input="
                    + pattern + " lhs=" + formatted1 + " rhs=" + formatted2);
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
<details><summary>▸ Prompt (8 message(s), ~5,077 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
- *[assistant] message: identical to step [43] — not reprinted*
- *[user] message: identical to step [5] — not reprinted*
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
            toAppendTo.append(String.valueOf(obj).toUpperCase(locale));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source;
        }
    }

    private static Map makeRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static String noBracesNoQuotes(String s) {
        return s.replace("'", "").replace("{", "").replace("}", "");
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = makeRegistry();

        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, registry);
        String anchorResult = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorResult)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: known regression output changed input="
                    + anchorPattern + " lhs=" + anchorResult + " rhs=it's a dummy test!");
        }

        String prefix = noBracesNoQuotes(data.consumeAsciiString(12));
        String middle = noBracesNoQuotes(data.consumeAsciiString(12));
        String quoted = noBracesNoQuotes(data.consumeAsciiString(12));
        String suffix = noBracesNoQuotes(data.consumeAsciiString(12));
        String arg = data.consumeString(16);

        int which = data.consumeInt(0, 3);
        String pattern;
        if (which == 0) {
            pattern = prefix + "''" + middle + " {0,lower} '" + quoted + "'!" + suffix;
        } else if (which == 1) {
            pattern = prefix + "''s " + middle + "{0,lower} '" + quoted + "'" + suffix;
        } else if (which == 2) {
            pattern = prefix + "''" + middle + " and {0,upper} '" + quoted + "' " + suffix;
        } else {
            pattern = prefix + "''" + middle + " {0,lower} '" + quoted + "' " + suffix;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String before = emf.toPattern();
        String formatted1 = emf.format(new Object[] { arg });
        String after = emf.toPattern();
        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: formatting must not mutate toPattern input="
                    + pattern + " lhs=" + before + " rhs=" + after);
        }

        String formatted2 = emf.format(new Object[] { arg });
        if (!formatted1.equals(formatted2)) {
            throw new RuntimeException("[oracle:format-idempotent] metamorphic violation: repeated formatting with same argument must be stable input="
                    + pattern + " lhs=" + formatted1 + " rhs=" + formatted2);
        }
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~5,619 chars)</summary>

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
        String anchorFormatted = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorFormatted)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: exact regression-test pattern must format as expected input="
                    + anchorPattern + " lhs=" + anchorFormatted + " rhs=it's a dummy test!");
        }

        String a = sanitizeLiteral(data.consumeAsciiString(12));
        String b = sanitizeLiteral(data.consumeAsciiString(12));
        String c = sanitizeLiteral(data.consumeAsciiString(12));
        String quoted = sanitizeQuoted(data.consumeAsciiString(12));
        String arg = data.consumeString(20);

        String pattern;
        switch (data.consumeInt(0, 4)) {
            case 0:
                pattern = a + "''" + b + " {0,lower} '" + quoted + "'!" + c;
                break;
            case 1:
                pattern = a + "''" + b + " {0,lower} '" + quoted + "'";
                break;
            case 2:
                pattern = a + "''" + b + " x {0,lower} '" + quoted + "' y " + c;
                break;
            case 3:
                pattern = a + "''s a {0,lower} '" + quoted + "'!" + c;
                break;
            default:
                pattern = a + "''" + b + " {0,upper} '" + quoted + "'!" + c;
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String before = emf.toPattern();
        String actual = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        /*
         * Contract/oracle: formatting is read-only with respect to the parsed pattern state;
         * toPattern is a public reader and must not change across repeated format calls.
         * A patch that only suppresses the buggy quote-handling path by skipping/losing parser
         * bookkeeping can leave observable pattern state inconsistent.
         */
        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: formatting must not mutate toPattern input="
                    + pattern + " lhs=" + before + " rhs=" + after);
        }

        String referencePattern = pattern.replace("{0,lower}", "{0}").replace("{0,upper}", "{0}");
        MessageFormat mf = new MessageFormat(referencePattern);
        String transformedArg;
        if (pattern.indexOf("{0,upper}") >= 0) {
            transformedArg = String.valueOf(arg).toUpperCase();
        } else {
            transformedArg = String.valueOf(arg).toLowerCase();
        }
        String expected = mf.format(new Object[] { transformedArg });
        if (!actual.equals(expected)) {
            throw new RuntimeException("[oracle:format-equivalence] metamorphic violation: custom lower/upper format should match MessageFormat on pre-transformed argument input="
                    + pattern + " lhs=" + actual + " rhs=" + expected);
        }
    }

    private static String sanitizeLiteral(String s) {
        return s.replace("'", "").replace("{", "").replace("}", "");
    }

    private static String sanitizeQuoted(String s) {
        return s.replace("'", "''").replace("{", "").replace("}", "");
    }

    public static class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    public static class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new UpperCaseFormat(locale == null ? Locale.getDefault() : locale);
        }
    }

    public static class LowerCaseFormat extends Format {
        private final Locale locale;

        public LowerCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            toAppendTo.append(String.valueOf(obj).toLowerCase(this.locale));
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source;
        }
    }

    public static class UpperCaseFormat extends Format {
        private final Locale locale;

        public UpperCaseFormat(Locale locale) {
            this.locale = locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            toAppendTo.append(String.valueOf(obj).toUpperCase(this.locale));
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
## [48] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [49] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~9,099 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = buildRegistry();

        // ANCHOR: exact trigger from the failing test first.
        String anchorPattern = "it''s a {0,lower} 'test'!";
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(anchorPattern, registry);
            String before = emf.toPattern();
            String out = emf.format(new Object[] { "DUMMY" });
            String after = emf.toPattern();

            if (!"it's a dummy test!".equals(out)) {
                throw new RuntimeException("[oracle:anchor-output] metamorphic violation: exact regression output input="
                        + anchorPattern + " lhs=" + out + " rhs=it's a dummy test!");
            }

            // Contract/oracle: formatting is a read-only operation with respect to the pattern;
            // a patch that only avoids the crash by skipping quote handling can change observable state.
            if (before != null ? !before.equals(after) : after != null) {
                throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: toPattern changed across format input="
                        + anchorPattern + " lhs=" + before + " rhs=" + after);
            }

            // Contract/oracle: formatting the same arguments twice should be deterministic.
            String out2 = emf.format(new Object[] { "DUMMY" });
            if (!out.equals(out2)) {
                throw new RuntimeException("[oracle:format-deterministic] metamorphic violation: repeated format differs input="
                        + anchorPattern + " lhs=" + out + " rhs=" + out2);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }

        // EXPLORE: root-cause property is an escaped quote ("''") in a pattern, especially near
        // a quoted literal and/or a format element, which drives parsing through appendQuotedString.
        String fuzzToken = sanitizeToken(data.consumeAsciiString(16));
        String fuzzQuoted = sanitizeLiteral(data.consumeAsciiString(16));
        String prefix = sanitizeLiteral(data.consumeAsciiString(12));
        String suffix = sanitizeLiteral(data.consumeAsciiString(12));
        boolean useCustom = data.consumeBoolean();
        boolean includeQuotedSection = data.consumeBoolean();
        boolean includeEscapedQuoteNearStart = data.consumeBoolean();

        StringBuilder pattern = new StringBuilder();
        pattern.append(prefix);
        if (includeEscapedQuoteNearStart) {
            pattern.append("''");
        } else {
            pattern.append("x''");
        }
        pattern.append(" ");
        pattern.append("a ");
        pattern.append(useCustom ? "{0,lower}" : "{0}");
        if (includeQuotedSection) {
            pattern.append(" '").append(fuzzQuoted).append("'");
        }
        pattern.append(" ");
        pattern.append(suffix);

        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern.toString(), Locale.getDefault(), registry);
            String before = emf.toPattern();
            Object[] args = new Object[] { fuzzToken.length() == 0 ? "X" : fuzzToken };
            String out1 = emf.format(args);
            String after = emf.toPattern();

            // Contract/oracle: toPattern is an observable reader; formatting should not mutate it.
            if (before != null ? !before.equals(after) : after != null) {
                throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: toPattern changed across format input="
                        + pattern + " lhs=" + before + " rhs=" + after);
            }

            // Contract/oracle: formatting with the same pattern and arguments is deterministic.
            String out2 = emf.format(args);
            if (out1 != null ? !out1.equals(out2) : out2 != null) {
                throw new RuntimeException("[oracle:format-deterministic] metamorphic violation: repeated format differs input="
                        + pattern + " lhs=" + out1 + " rhs=" + out2);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }

        // Additional real-entry exploration without custom formats, so the harness remains useful
        // even if test-only factories are unavailable on the classpath.
        String plainPattern = buildPlainPattern(data);
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(plainPattern);
            String before = emf.toPattern();
            Object[] args = new Object[] { fuzzToken.length() == 0 ? "Y" : fuzzToken };
            String out1 = emf.format(args);
            String after = emf.toPattern();

            if (before != null ? !before.equals(after) : after != null) {
                throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: toPattern changed across format input="
                        + plainPattern + " lhs=" + before + " rhs=" + after);
            }

            String out2 = emf.format(args);
            if (out1 != null ? !out1.equals(out2) : out2 != null) {
                throw new RuntimeException("[oracle:format-deterministic] metamorphic violation: repeated format differs input="
                        + plainPattern + " lhs=" + out1 + " rhs=" + out2);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }
    }

    private static void handleThrowable(Throwable t) {
        if (isCleanRejection(t)) {
            return;
        }
        if (isRootCause(t)) {
            throwAsUnchecked(t);
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
        }
        return false;
    }

    private static void throwAsUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }

    private static String sanitizeToken(String s) {
        if (s == null || s.length() == 0) {
            return "X";
        }
        return s.replace('{', 'A').replace('}', 'B').replace('\'', 'q');
    }

    private static String sanitizeLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "lit";
        }
        return s.replace('{', 'L').replace('}', 'R').replace('\'', 's');
    }

    private static String buildPlainPattern(FuzzedDataProvider data) {
        String a = sanitizeLiteral(data.consumeAsciiString(12));
        String b = sanitizeLiteral(data.consumeAsciiString(12));
        String c = sanitizeLiteral(data.consumeAsciiString(12));
        int variant = data.consumeInt(0, 2);
        if (variant == 0) {
            return a + "'' " + "{0}" + " '" + b + "' " + c;
        }
        if (variant == 1) {
            return "''" + a + " {0} '" + b + "'";
        }
        return a + " '" + b + "' " + "''" + " {0} " + c;
    }

    private static Map buildRegistry() {
        Map registry = new HashMap();
        Object lower = instantiateFactory(
                "org.apache.commons.lang.text.LowerCaseFormatFactory",
                "org.apache.commons.lang.text.ExtendedMessageFormatTest$LowerCaseFormatFactory");
        Object upper = instantiateFactory(
                "org.apache.commons.lang.text.UpperCaseFormatFactory",
                "org.apache.commons.lang.text.ExtendedMessageFormatTest$UpperCaseFormatFactory");
        if (lower != null) {
            registry.put("lower", lower);
        }
        if (upper != null) {
            registry.put("upper", upper);
        }
        return registry;
    }

    private static Object instantiateFactory(String... names) {
        for (int i = 0; i < names.length; i++) {
            try {
                Class cls = Class.forName(names[i]);
                return cls.getDeclaredConstructor().newInstance();
            } catch (Throwable ignored) {
            }
        }
        return null;
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
<details><summary>▸ Prompt (4 message(s), ~9,296 chars, 1 new)</summary>

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
        Map registry = buildRegistry();

        // ANCHOR: exact trigger from the failing test first.
        String anchorPattern = "it''s a {0,lower} 'test'!";
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(anchorPattern, registry);
            String before = emf.toPattern();
            String out = emf.format(new Object[] { "DUMMY" });
            String after = emf.toPattern();

            if (!"it's a dummy test!".equals(out)) {
                throw new RuntimeException("[oracle:anchor-output] metamorphic violation: exact regression output input="
                        + anchorPattern + " lhs=" + out + " rhs=it's a dummy test!");
            }

            // Contract/oracle: formatting is a read-only operation with respect to the pattern;
            // a patch that only avoids the crash by skipping quote handling can change observable state.
            if (before != null ? !before.equals(after) : after != null) {
                throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: toPattern changed across format input="
                        + anchorPattern + " lhs=" + before + " rhs=" + after);
            }

            // Contract/oracle: formatting the same arguments twice should be deterministic.
            String out2 = emf.format(new Object[] { "DUMMY" });
            if (!out.equals(out2)) {
                throw new RuntimeException("[oracle:format-deterministic] metamorphic violation: repeated format differs input="
                        + anchorPattern + " lhs=" + out + " rhs=" + out2);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }

        // EXPLORE: root-cause property is an escaped quote ("''") in a pattern, especially near
        // a quoted literal and/or a format element, which drives parsing through appendQuotedString.
        String fuzzToken = sanitizeToken(data.consumeAsciiString(16));
        String fuzzQuoted = sanitizeLiteral(data.consumeAsciiString(16));
        String prefix = sanitizeLiteral(data.consumeAsciiString(12));
        String suffix = sanitizeLiteral(data.consumeAsciiString(12));
        boolean useCustom = data.consumeBoolean();
        boolean includeQuotedSection = data.consumeBoolean();
        boolean includeEscapedQuoteNearStart = data.consumeBoolean();

        StringBuilder pattern = new StringBuilder();
        pattern.append(prefix);
        if (includeEscapedQuoteNearStart) {
            pattern.append("''");
        } else {
            pattern.append("x''");
        }
        pattern.append(" ");
        pattern.append("a ");
        pattern.append(useCustom ? "{0,lower}" : "{0}");
        if (includeQuotedSection) {
            pattern.append(" '").append(fuzzQuoted).append("'");
        }
        pattern.append(" ");
        pattern.append(suffix);

        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern.toString(), Locale.getDefault(), registry);
            String before = emf.toPattern();
            Object[] args = new Object[] { fuzzToken.length() == 0 ? "X" : fuzzToken };
            String out1 = emf.format(args);
            String after = emf.toPattern();

            // Contract/oracle: toPattern is an observable reader; formatting should not mutate it.
            if (before != null ? !before.equals(after) : after != null) {
                throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: toPattern changed across format input="
                        + pattern + " lhs=" + before + " rhs=" + after);
            }

            // Contract/oracle: formatting with the same pattern and arguments is deterministic.
            String out2 = emf.format(args);
            if (out1 != null ? !out1.equals(out2) : out2 != null) {
                throw new RuntimeException("[oracle:format-deterministic] metamorphic violation: repeated format differs input="
                        + pattern + " lhs=" + out1 + " rhs=" + out2);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }

        // Additional real-entry exploration without custom formats, so the harness remains useful
        // even if test-only factories are unavailable on the classpath.
        String plainPattern = buildPlainPattern(data);
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(plainPattern);
            String before = emf.toPattern();
            Object[] args = new Object[] { fuzzToken.length() == 0 ? "Y" : fuzzToken };
            String out1 = emf.format(args);
            String after = emf.toPattern();

            if (before != null ? !before.equals(after) : after != null) {
                throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: toPattern changed across format input="
                        + plainPattern + " lhs=" + before + " rhs=" + after);
            }

            String out2 = emf.format(args);
            if (out1 != null ? !out1.equals(out2) : out2 != null) {
                throw new RuntimeException("[oracle:format-deterministic] metamorphic violation: repeated format differs input="
                        + plainPattern + " lhs=" + out1 + " rhs=" + out2);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }
    }

    private static void handleThrowable(Throwable t) {
        if (isCleanRejection(t)) {
            return;
        }
        if (isRootCause(t)) {
            throwAsUnchecked(t);
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
        }
        return false;
    }

    private static void throwAsUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }

    private static String sanitizeToken(String s) {
        if (s == null || s.length() == 0) {
            return "X";
        }
        return s.replace('{', 'A').replace('}', 'B').replace('\'', 'q');
    }

    private static String sanitizeLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "lit";
        }
        return s.replace('{', 'L').replace('}', 'R').replace('\'', 's');
    }

    private static String buildPlainPattern(FuzzedDataProvider data) {
        String a = sanitizeLiteral(data.consumeAsciiString(12));
        String b = sanitizeLiteral(data.consumeAsciiString(12));
        String c = sanitizeLiteral(data.consumeAsciiString(12));
        int variant = data.consumeInt(0, 2);
        if (variant == 0) {
            return a + "'' " + "{0}" + " '" + b + "' " + c;
        }
        if (variant == 1) {
            return "''" + a + " {0} '" + b + "'";
        }
        return a + " '" + b + "' " + "''" + " {0} " + c;
    }

    private static Map buildRegistry() {
        Map registry = new HashMap();
        Object lower = instantiateFactory(
                "org.apache.commons.lang.text.LowerCaseFormatFactory",
                "org.apache.commons.lang.text.ExtendedMessageFormatTest$LowerCaseFormatFactory");
        Object upper = instantiateFactory(
                "org.apache.commons.lang.text.UpperCaseFormatFactory",
                "org.apache.commons.lang.text.ExtendedMessageFormatTest$UpperCaseFormatFactory");
        if (lower != null) {
            registry.put("lower", lower);
        }
        if (upper != null) {
            registry.put("upper", upper);
        }
        return registry;
    }

    private static Object instantiateFactory(String... names) {
        for (int i = 0; i < names.length; i++) {
            try {
                Class cls = Class.forName(names[i]);
                return cls.getDeclaredConstructor().newInstance();
            } catch (Throwable ignored) {
            }
        }
        return null;
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~4,509 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = buildRegistry();

        String anchor = "it''s a {0,lower} 'test'!";
        exercise(anchor, registry, "DUMMY");

        String prefix = cleanLiteral(data.consumeAsciiString(12));
        String quoted = cleanLiteral(data.consumeAsciiString(12));
        String suffix = cleanLiteral(data.consumeAsciiString(12));
        String fmt = data.consumeBoolean() ? "lower" : cleanFormatName(data.consumeAsciiString(8));
        if (fmt.length() == 0) {
            fmt = "lower";
        }

        StringBuilder pattern = new StringBuilder();
        if (data.consumeBoolean()) {
            pattern.append(prefix);
        } else {
            pattern.append("x");
        }
        pattern.append("''");
        pattern.append(data.consumeBoolean() ? " " : "");
        pattern.append(cleanLiteral(data.consumeAsciiString(8)));
        pattern.append(" {0,").append(fmt).append("}");
        pattern.append(data.consumeBoolean() ? " '" + quoted + "'" : "");
        pattern.append(data.consumeBoolean() ? " " + suffix : "!");
        exercise(pattern.toString(), registry, cleanArg(data.consumeAsciiString(12)));
    }

    private static void exercise(String pattern, Map registry, String arg) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

        String before = emf.toPattern();
        String out1 = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        // Contract: format(Object[]) is read-only with respect to the compiled pattern;
        // deleting the patched bookkeeping can avoid the crash by mis-parsing/skipping content,
        // which would show up as changed observable pattern state.
        if (before == null ? after != null : !before.equals(after)) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: format changed toPattern input="
                    + pattern + " lhs=" + before + " rhs=" + after);
        }

        // Contract: formatting the same arguments with the same formatter is deterministic.
        String out2 = emf.format(new Object[] { arg });
        if (out1 == null ? out2 != null : !out1.equals(out2)) {
            throw new RuntimeException("[oracle:format-deterministic] metamorphic violation: repeated format differs input="
                    + pattern + " lhs=" + out1 + " rhs=" + out2);
        }
    }

    private static String cleanLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "a";
        }
        return s.replace('\'', 'q').replace('{', 'x').replace('}', 'y');
    }

    private static String cleanFormatName(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                b.append(c);
            }
        }
        return b.toString();
    }

    private static String cleanArg(String s) {
        if (s == null || s.length() == 0) {
            return "X";
        }
        return s.replace('{', 'a').replace('}', 'b').replace('\'', 'c');
    }

    private static Map buildRegistry() {
        Map registry = new HashMap();
        Object lower = instantiate(
                "org.apache.commons.lang.text.LowerCaseFormatFactory",
                "org.apache.commons.lang.text.ExtendedMessageFormatTest$LowerCaseFormatFactory");
        Object upper = instantiate(
                "org.apache.commons.lang.text.UpperCaseFormatFactory",
                "org.apache.commons.lang.text.ExtendedMessageFormatTest$UpperCaseFormatFactory");
        if (lower != null) {
            registry.put("lower", lower);
        }
        if (upper != null) {
            registry.put("upper", upper);
        }
        return registry;
    }

    private static Object instantiate(String... classNames) {
        for (int i = 0; i < classNames.length; i++) {
            try {
                Class c = Class.forName(classNames[i]);
                return c.getDeclaredConstructor().newInstance();
            } catch (Throwable ignored) {
            }
        }
        return null;
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
<details><summary>▸ Prompt (6 message(s), ~4,826 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
- *[assistant] message: identical to step [51] — not reprinted*
- *[user] message: identical to step [5] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = buildRegistry();

        String anchor = "it''s a {0,lower} 'test'!";
        exercise(anchor, registry, "DUMMY");

        String prefix = cleanLiteral(data.consumeAsciiString(12));
        String quoted = cleanLiteral(data.consumeAsciiString(12));
        String suffix = cleanLiteral(data.consumeAsciiString(12));
        String fmt = data.consumeBoolean() ? "lower" : cleanFormatName(data.consumeAsciiString(8));
        if (fmt.length() == 0) {
            fmt = "lower";
        }

        StringBuilder pattern = new StringBuilder();
        if (data.consumeBoolean()) {
            pattern.append(prefix);
        } else {
            pattern.append("x");
        }
        pattern.append("''");
        pattern.append(data.consumeBoolean() ? " " : "");
        pattern.append(cleanLiteral(data.consumeAsciiString(8)));
        pattern.append(" {0,").append(fmt).append("}");
        pattern.append(data.consumeBoolean() ? " '" + quoted + "'" : "");
        pattern.append(data.consumeBoolean() ? " " + suffix : "!");
        exercise(pattern.toString(), registry, cleanArg(data.consumeAsciiString(12)));
    }

    private static void exercise(String pattern, Map registry, String arg) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

        String before = emf.toPattern();
        String out1 = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        // Contract: format(Object[]) is read-only with respect to the compiled pattern;
        // deleting the patched bookkeeping can avoid the crash by mis-parsing/skipping content,
        // which would show up as changed observable pattern state.
        if (before == null ? after != null : !before.equals(after)) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: format changed toPattern input="
                    + pattern + " lhs=" + before + " rhs=" + after);
        }

        // Contract: formatting the same arguments with the same formatter is deterministic.
        String out2 = emf.format(new Object[] { arg });
        if (out1 == null ? out2 != null : !out1.equals(out2)) {
            throw new RuntimeException("[oracle:format-deterministic] metamorphic violation: repeated format differs input="
                    + pattern + " lhs=" + out1 + " rhs=" + out2);
        }
    }

    private static String cleanLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "a";
        }
        return s.replace('\'', 'q').replace('{', 'x').replace('}', 'y');
    }

    private static String cleanFormatName(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                b.append(c);
            }
        }
        return b.toString();
    }

    private static String cleanArg(String s) {
        if (s == null || s.length() == 0) {
            return "X";
        }
        return s.replace('{', 'a').replace('}', 'b').replace('\'', 'c');
    }

    private static Map buildRegistry() {
        Map registry = new HashMap();
        Object lower = instantiate(
                "org.apache.commons.lang.text.LowerCaseFormatFactory",
                "org.apache.commons.lang.text.ExtendedMessageFormatTest$LowerCaseFormatFactory");
        Object upper = instantiate(
                "org.apache.commons.lang.text.UpperCaseFormatFactory",
                "org.apache.commons.lang.text.ExtendedMessageFormatTest$UpperCaseFormatFactory");
        if (lower != null) {
            registry.put("lower", lower);
        }
        if (upper != null) {
            registry.put("upper", upper);
        }
        return registry;
    }

    private static Object instantiate(String... classNames) {
        for (int i = 0; i < classNames.length; i++) {
            try {
                Class c = Class.forName(classNames[i]);
                return c.getDeclaredConstructor().newInstance();
            } catch (Throwable ignored) {
            }
        }
        return null;
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~3,162 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        // ANCHOR: exact trigger pattern from the failing test.
        // On the buggy version, construction itself reaches applyPattern -> appendQuotedString
        // and can loop until OutOfMemoryError because the parse position is not advanced.
        new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);

        // Valid-by-construction exploration: a non-null registry forces ExtendedMessageFormat's
        // custom pattern parser, and any doubled quote in the pattern exercises the fixed line.
        String prefix = clean(data.consumeAsciiString(12));
        String middle = clean(data.consumeAsciiString(12));
        String quoted = clean(data.consumeAsciiString(12));
        String suffix = clean(data.consumeAsciiString(12));
        String arg = cleanArg(data.consumeAsciiString(12));

        String pattern = prefix + "''" + middle + " {0} '" + quoted + "'" + suffix;
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

        String before = emf.toPattern();
        String out1 = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        // Contract: format is read-only with respect to the compiled pattern; toPattern should stay stable.
        if (before == null ? after != null : !before.equals(after)) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: format changed toPattern input="
                    + pattern + " lhs=" + before + " rhs=" + after);
        }

        // Contract: formatting the same arguments twice with the same formatter is deterministic.
        String out2 = emf.format(new Object[] { arg });
        if (out1 == null ? out2 != null : !out1.equals(out2)) {
            throw new RuntimeException("[oracle:format-deterministic] metamorphic violation: repeated format differs input="
                    + pattern + " lhs=" + out1 + " rhs=" + out2);
        }

        // Oracle from known value: for a valid pattern built as text + escaped quote + {0} + quoted text,
        // the formatted output must contain the argument and preserve the escaped quote as a literal apostrophe.
        if (out1.indexOf(arg) < 0 || out1.indexOf('\'') < 0) {
            throw new RuntimeException("[oracle:formatted-shape] metamorphic violation: expected literal quote and argument in output input="
                    + pattern + " output=" + out1 + " arg=" + arg);
        }
    }

    private static String clean(String s) {
        if (s == null || s.length() == 0) {
            return "a";
        }
        return s.replace('\'', 'q').replace('{', 'x').replace('}', 'y');
    }

    private static String cleanArg(String s) {
        if (s == null || s.length() == 0) {
            return "X";
        }
        return s.replace('\'', 'c').replace('{', 'a').replace('}', 'b');
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
<details><summary>▸ Prompt (8 message(s), ~3,599 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
- *[assistant] message: identical to step [51] — not reprinted*
- *[user] message: identical to step [5] — not reprinted*
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

        // ANCHOR: exact trigger pattern from the failing test.
        // On the buggy version, construction itself reaches applyPattern -> appendQuotedString
        // and can loop until OutOfMemoryError because the parse position is not advanced.
        new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);

        // Valid-by-construction exploration: a non-null registry forces ExtendedMessageFormat's
        // custom pattern parser, and any doubled quote in the pattern exercises the fixed line.
        String prefix = clean(data.consumeAsciiString(12));
        String middle = clean(data.consumeAsciiString(12));
        String quoted = clean(data.consumeAsciiString(12));
        String suffix = clean(data.consumeAsciiString(12));
        String arg = cleanArg(data.consumeAsciiString(12));

        String pattern = prefix + "''" + middle + " {0} '" + quoted + "'" + suffix;
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

        String before = emf.toPattern();
        String out1 = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        // Contract: format is read-only with respect to the compiled pattern; toPattern should stay stable.
        if (before == null ? after != null : !before.equals(after)) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: format changed toPattern input="
                    + pattern + " lhs=" + before + " rhs=" + after);
        }

        // Contract: formatting the same arguments twice with the same formatter is deterministic.
        String out2 = emf.format(new Object[] { arg });
        if (out1 == null ? out2 != null : !out1.equals(out2)) {
            throw new RuntimeException("[oracle:format-deterministic] metamorphic violation: repeated format differs input="
                    + pattern + " lhs=" + out1 + " rhs=" + out2);
        }

        // Oracle from known value: for a valid pattern built as text + escaped quote + {0} + quoted text,
        // the formatted output must contain the argument and preserve the escaped quote as a literal apostrophe.
        if (out1.indexOf(arg) < 0 || out1.indexOf('\'') < 0) {
            throw new RuntimeException("[oracle:formatted-shape] metamorphic violation: expected literal quote and argument in output input="
                    + pattern + " output=" + out1 + " arg=" + arg);
        }
    }

    private static String clean(String s) {
        if (s == null || s.length() == 0) {
            return "a";
        }
        return s.replace('\'', 'q').replace('{', 'x').replace('}', 'y');
    }

    private static String cleanArg(String s) {
        if (s == null || s.length() == 0) {
            return "X";
        }
        return s.replace('\'', 'c').replace('{', 'a').replace('}', 'b');
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~4,893 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = buildRegistry();

        // ANCHOR: exact failing-test trigger. The buggy code misses next(pos) before checking
        // c[start] == QUOTE when appendQuotedString is entered on an escaped quote (''), so the
        // parse position never advances and construction can spiral into OutOfMemoryError.
        ExtendedMessageFormat anchor = new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: exact regression output input=it''s a {0,lower} 'test'! lhs="
                    + anchorOut + " rhs=it's a dummy test!");
        }
        String anchorBefore = anchor.toPattern();
        String anchorAfter = anchor.toPattern();
        if (anchorBefore == null ? anchorAfter != null : !anchorBefore.equals(anchorAfter)) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: toPattern changed input=it''s a {0,lower} 'test'! lhs="
                    + anchorBefore + " rhs=" + anchorAfter);
        }

        // EXPLORE: valid-by-construction patterns containing an escaped quote ("''") before a
        // custom format element, which is the input property that exercises the patched line.
        String prefix = clean(data.consumeAsciiString(10));
        String infix = clean(data.consumeAsciiString(10));
        String quoted = clean(data.consumeAsciiString(10));
        String suffix = clean(data.consumeAsciiString(10));
        String arg = cleanArg(data.consumeAsciiString(10));

        String pattern = prefix + "''" + infix + " {0,lower} '" + quoted + "'" + suffix;
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

        String before = emf.toPattern();
        String out1 = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        // Contract: formatting does not mutate the compiled pattern; a throw-deleting or
        // branch-skipping patch can silently produce wrong internal state observable via toPattern().
        if (before == null ? after != null : !before.equals(after)) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: format changed toPattern input="
                    + pattern + " lhs=" + before + " rhs=" + after);
        }

        // Contract: formatting the same arguments twice with the same formatter is deterministic.
        String out2 = emf.format(new Object[] { arg });
        if (out1 == null ? out2 != null : !out1.equals(out2)) {
            throw new RuntimeException("[oracle:format-deterministic] metamorphic violation: repeated format differs input="
                    + pattern + " lhs=" + out1 + " rhs=" + out2);
        }

        // Oracle from the constructed input: the lower formatter must lowercase the argument, and
        // the escaped quote must survive as a literal apostrophe in the output.
        String expectedLower = arg.toLowerCase();
        if (out1.indexOf(expectedLower) < 0 || out1.indexOf('\'') < 0) {
            throw new RuntimeException("[oracle:formatted-shape] metamorphic violation: expected lowercase argument and apostrophe input="
                    + pattern + " output=" + out1 + " expectedLower=" + expectedLower);
        }
    }

    private static Map buildRegistry() {
        try {
            Map registry = new HashMap();
            registry.put("lower", instantiateFactory("org.apache.commons.lang.text.ExtendedMessageFormatTest$LowerCaseFormatFactory"));
            registry.put("upper", instantiateFactory("org.apache.commons.lang.text.ExtendedMessageFormatTest$UpperCaseFormatFactory"));
            return registry;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Object instantiateFactory(String className) throws Exception {
        Class cls = Class.forName(className);
        Constructor ctor = cls.getDeclaredConstructor();
        ctor.setAccessible(true);
        return ctor.newInstance();
    }

    private static String clean(String s) {
        if (s == null || s.length() == 0) {
            return "a";
        }
        return s.replace('\'', 'q').replace('{', 'x').replace('}', 'y');
    }

    private static String cleanArg(String s) {
        if (s == null || s.length() == 0) {
            return "DuMmY";
        }
        return s.replace('\'', 'c').replace('{', 'a').replace('}', 'b');
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
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,144 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = makeRegistry();

        // ANCHOR: faithful trigger from the regression test, with the same registry setup.
        exercise("it''s a {0,lower} 'test'!", registry, true);

        // EXPLORE: vary surrounding text while preserving the root-cause property:
        // an escaped quote ("''") in a pattern parsed by ExtendedMessageFormat with a custom format.
        String left = visibleToken(data.consumeAsciiString(12));
        String right = visibleToken(data.consumeAsciiString(12));
        String quoted = visibleToken(data.consumeAsciiString(12));
        String tail = visibleToken(data.consumeAsciiString(12));
        String fmt = data.consumeBoolean() ? "lower" : "upper";

        StringBuilder pattern = new StringBuilder();
        pattern.append(left);
        if (data.consumeBoolean()) {
            pattern.append(' ');
        }
        pattern.append("''");
        if (data.consumeBoolean()) {
            pattern.append(right);
        } else {
            pattern.append('x').append(right);
        }
        pattern.append(" {0,").append(fmt).append("}");
        if (data.consumeBoolean()) {
            pattern.append(" '").append(quoted).append('\'');
        }
        if (data.consumeBoolean()) {
            pattern.append(' ').append(tail);
        }
        exercise(pattern.toString(), registry, false);
    }

    private static Map makeRegistry() {
        Map registry = new HashMap();
        try {
            registry.put("lower", new LowerCaseFormatFactory());
            registry.put("upper", new UpperCaseFormatFactory());
        } catch (Throwable t) {
            // If test support classes are unavailable at runtime, still pass a non-null registry
            // so ExtendedMessageFormat follows its real custom-format parsing path.
        }
        return registry;
    }

    private static void exercise(String pattern, Map registry, boolean anchor) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

            String before = emf.toPattern();

            try {
                emf.format(new Object[] { "DUMMY" });
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throwUnchecked(t);
                }
                return;
            }

            String after = emf.toPattern();

            // Oracle: toPattern is a read-only state reader; formatting must not mutate it.
            // A throw-deleting or bookkeeping-skipping patch in quote handling can silently build
            // the wrong internal pattern, which this stability check exposes.
            if (before != null && after != null && !before.equals(after)) {
                throw new RuntimeException(
                    "[oracle:toPattern-stable] metamorphic violation: format changed toPattern input="
                        + pattern + " lhs=" + before + " rhs=" + after);
            }

            // Oracle: for an accepted pattern, re-parsing the canonical toPattern() must be idempotent.
            try {
                ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, registry);
                String reparsed = emf2.toPattern();
                if (reparsed != null && !before.equals(reparsed)) {
                    throw new RuntimeException(
                        "[oracle:toPattern-idem] metamorphic violation: reparsing canonical pattern changed it input="
                            + pattern + " lhs=" + before + " rhs=" + reparsed);
                }
            } catch (Throwable t) {
                if (isRootCause(t)) {
                    throwUnchecked(t);
                }
                // Per hygiene rules, if either side throws, skip the check.
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (anchor && t instanceof RuntimeException && hasAppendQuotedStringFrame(t)) {
                throw (RuntimeException) t;
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        return t instanceof OutOfMemoryError && hasAppendQuotedStringFrame(t);
    }

    private static boolean hasAppendQuotedStringFrame(Throwable t) {
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                    && "appendQuotedString".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Validation") || name.contains("Invalid");
    }

    private static String visibleToken(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == '\'') {
                out.append('x');
            } else if (c >= 32 && c <= 126) {
                out.append(c);
            }
        }
        if (out.length() == 0) {
            out.append('x');
        }
        return out.toString();
    }

    private static void throwUnchecked(Throwable t) {
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
## [58] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [59] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~7,838 chars, 2 new)</summary>

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
        Map registry = makeRegistry();

        // ANCHOR: faithful trigger from the regression test, with the same registry setup.
        exercise("it''s a {0,lower} 'test'!", registry, true);

        // EXPLORE: vary surrounding text while preserving the root-cause property:
        // an escaped quote ("''") in a pattern parsed by ExtendedMessageFormat with a custom format.
        String left = visibleToken(data.consumeAsciiString(12));
        String right = visibleToken(data.consumeAsciiString(12));
        String quoted = visibleToken(data.consumeAsciiString(12));
        String tail = visibleToken(data.consumeAsciiString(12));
        String fmt = data.consumeBoolean() ? "lower" : "upper";

        StringBuilder pattern = new StringBuilder();
        pattern.append(left);
        if (data.consumeBoolean()) {
            pattern.append(' ');
        }
        pattern.append("''");
        if (data.consumeBoolean()) {
            pattern.append(right);
        } else {
            pattern.append('x').append(right);
        }
        pattern.append(" {0,").append(fmt).append("}");
        if (data.consumeBoolean()) {
            pattern.append(" '").append(quoted).append('\'');
        }
        if (data.consumeBoolean()) {
            pattern.append(' ').append(tail);
        }
        exercise(pattern.toString(), registry, false);
    }

    private static Map makeRegistry() {
        Map registry = new HashMap();
        try {
            registry.put("lower", new LowerCaseFormatFactory());
            registry.put("upper", new UpperCaseFormatFactory());
        } catch (Throwable t) {
            // If test support classes are unavailable at runtime, still pass a non-null registry
            // so ExtendedMessageFormat follows its real custom-format parsing path.
        }
        return registry;
    }

    private static void exercise(String pattern, Map registry, boolean anchor) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

            String before = emf.toPattern();

            try {
                emf.format(new Object[] { "DUMMY" });
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throwUnchecked(t);
                }
                return;
            }

            String after = emf.toPattern();

            // Oracle: toPattern is a read-only state reader; formatting must not mutate it.
            // A throw-deleting or bookkeeping-skipping patch in quote handling can silently build
            // the wrong internal pattern, which this stability check exposes.
            if (before != null && after != null && !before.equals(after)) {
                throw new RuntimeException(
                    "[oracle:toPattern-stable] metamorphic violation: format changed toPattern input="
                        + pattern + " lhs=" + before + " rhs=" + after);
            }

            // Oracle: for an accepted pattern, re-parsing the canonical toPattern() must be idempotent.
            try {
                ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, registry);
                String reparsed = emf2.toPattern();
                if (reparsed != null && !before.equals(reparsed)) {
                    throw new RuntimeException(
                        "[oracle:toPattern-idem] metamorphic violation: reparsing canonical pattern changed it input="
                            + pattern + " lhs=" + before + " rhs=" + reparsed);
                }
            } catch (Throwable t) {
                if (isRootCause(t)) {
                    throwUnchecked(t);
                }
                // Per hygiene rules, if either side throws, skip the check.
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (anchor && t instanceof RuntimeException && hasAppendQuotedStringFrame(t)) {
                throw (RuntimeException) t;
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        return t instanceof OutOfMemoryError && hasAppendQuotedStringFrame(t);
    }

    private static boolean hasAppendQuotedStringFrame(Throwable t) {
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                    && "appendQuotedString".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Validation") || name.contains("Invalid");
    }

    private static String visibleToken(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == '\'') {
                out.append('x');
            } else if (c >= 32 && c <= 126) {
                out.append(c);
            }
        }
        if (out.length() == 0) {
            out.append('x');
        }
        return out.toString();
    }

    private static void throwUnchecked(Throwable t) {
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
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/Lang_43_buggy/fuzz/attempt_028/FuzzHarness.java:46: error: cannot find symbol
            registry.put("lower", new LowerCaseFormatFactory());
                                      ^
  symbol:   class LowerCaseFormatFactory
  location: class FuzzHarness
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/Lang_43_buggy/fuzz/attempt_028/FuzzHarness.java:47: error: cannot find symbol
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
<details open><summary>▸ Output (~6,182 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        // ANCHOR: exact pattern from the regression test, driven through the real public API.
        exercise("it''s a {0,lower} 'test'!", registry);

        // EXPLORE: preserve the triggering property: a pattern accepted by ExtendedMessageFormat
        // that contains an escaped quote ("''"), varied in length/position/surrounding content.
        String left = safeToken(data.consumeAsciiString(12));
        String middle = safeToken(data.consumeAsciiString(12));
        String quoted = safeToken(data.consumeAsciiString(12));
        String tail = safeToken(data.consumeAsciiString(12));

        StringBuilder p = new StringBuilder();
        if (data.consumeBoolean()) {
            p.append(left);
        } else {
            p.append("pre").append(left);
        }
        if (data.consumeBoolean()) {
            p.append("''");
        } else {
            p.append(left).append("''");
        }
        if (data.consumeBoolean()) {
            p.append(' ').append(middle);
        }
        p.append(" {0,number}");
        if (data.consumeBoolean()) {
            p.append(" '").append(quoted).append('\'');
        }
        if (data.consumeBoolean()) {
            p.append(' ').append(tail);
        }
        exercise(p.toString(), registry);

        // A second exploration strategy using only literal text plus a simple argument.
        StringBuilder p2 = new StringBuilder();
        p2.append(safeToken(data.consumeAsciiString(8)));
        p2.append("''");
        p2.append(safeToken(data.consumeAsciiString(8)));
        if (data.consumeBoolean()) {
            p2.append(" {0}");
        } else {
            p2.append(" {0,time,short}");
        }
        if (data.consumeBoolean()) {
            p2.append(" '").append(safeToken(data.consumeAsciiString(8))).append('\'');
        }
        exercise(p2.toString(), registry);
    }

    private static void exercise(String pattern, Map registry) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

            String before = emf.toPattern();

            try {
                emf.format(new Object[] { Integer.valueOf(123), "DUMMY" });
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throwUnchecked(t);
                }
                return;
            }

            String after = emf.toPattern();

            // Oracle: toPattern() is a public reader of the formatter state; formatting must not
            // mutate that state. A patch that merely skips quote-processing bookkeeping can avoid
            // the crash yet silently produce inconsistent internal pattern state.
            if (before != null && after != null && !before.equals(after)) {
                throw new RuntimeException(
                    "[oracle:toPattern-stable] metamorphic violation: format changed toPattern input="
                        + pattern + " lhs=" + before + " rhs=" + after);
            }

            // Oracle: reparsing the canonical pattern from toPattern() must be idempotent for any
            // accepted pattern. If either side throws, skip per fuzzing hygiene rules.
            try {
                ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, registry);
                String reparsed = emf2.toPattern();
                if (before != null && reparsed != null && !before.equals(reparsed)) {
                    throw new RuntimeException(
                        "[oracle:toPattern-idem] metamorphic violation: reparsing canonical pattern changed it input="
                            + pattern + " lhs=" + before + " rhs=" + reparsed);
                }
            } catch (Throwable t) {
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

    private static boolean isRootCause(Throwable t) {
        return t instanceof OutOfMemoryError && hasAppendQuotedStringFrame(t);
    }

    private static boolean hasAppendQuotedStringFrame(Throwable t) {
        StackTraceElement[] frames = t.getStackTrace();
        for (int i = 0; i < frames.length; i++) {
            StackTraceElement ste = frames[i];
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                    && "appendQuotedString".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String n = t.getClass().getName();
        return n.indexOf("Validation") >= 0 || n.indexOf("Invalid") >= 0;
    }

    private static String safeToken(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 32 && c <= 126 && c != '{' && c != '}' && c != '\'') {
                out.append(c);
            } else {
                out.append('x');
            }
        }
        if (out.length() == 0) {
            out.append('x');
        }
        return out.toString();
    }

    private static void throwUnchecked(Throwable t) {
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
## [60] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [61] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~6,500 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
- *[assistant] message: identical to step [59] — not reprinted*
- *[user] message: identical to step [59] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        // ANCHOR: exact pattern from the regression test, driven through the real public API.
        exercise("it''s a {0,lower} 'test'!", registry);

        // EXPLORE: preserve the triggering property: a pattern accepted by ExtendedMessageFormat
        // that contains an escaped quote ("''"), varied in length/position/surrounding content.
        String left = safeToken(data.consumeAsciiString(12));
        String middle = safeToken(data.consumeAsciiString(12));
        String quoted = safeToken(data.consumeAsciiString(12));
        String tail = safeToken(data.consumeAsciiString(12));

        StringBuilder p = new StringBuilder();
        if (data.consumeBoolean()) {
            p.append(left);
        } else {
            p.append("pre").append(left);
        }
        if (data.consumeBoolean()) {
            p.append("''");
        } else {
            p.append(left).append("''");
        }
        if (data.consumeBoolean()) {
            p.append(' ').append(middle);
        }
        p.append(" {0,number}");
        if (data.consumeBoolean()) {
            p.append(" '").append(quoted).append('\'');
        }
        if (data.consumeBoolean()) {
            p.append(' ').append(tail);
        }
        exercise(p.toString(), registry);

        // A second exploration strategy using only literal text plus a simple argument.
        StringBuilder p2 = new StringBuilder();
        p2.append(safeToken(data.consumeAsciiString(8)));
        p2.append("''");
        p2.append(safeToken(data.consumeAsciiString(8)));
        if (data.consumeBoolean()) {
            p2.append(" {0}");
        } else {
            p2.append(" {0,time,short}");
        }
        if (data.consumeBoolean()) {
            p2.append(" '").append(safeToken(data.consumeAsciiString(8))).append('\'');
        }
        exercise(p2.toString(), registry);
    }

    private static void exercise(String pattern, Map registry) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

            String before = emf.toPattern();

            try {
                emf.format(new Object[] { Integer.valueOf(123), "DUMMY" });
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throwUnchecked(t);
                }
                return;
            }

            String after = emf.toPattern();

            // Oracle: toPattern() is a public reader of the formatter state; formatting must not
            // mutate that state. A patch that merely skips quote-processing bookkeeping can avoid
            // the crash yet silently produce inconsistent internal pattern state.
            if (before != null && after != null && !before.equals(after)) {
                throw new RuntimeException(
                    "[oracle:toPattern-stable] metamorphic violation: format changed toPattern input="
                        + pattern + " lhs=" + before + " rhs=" + after);
            }

            // Oracle: reparsing the canonical pattern from toPattern() must be idempotent for any
            // accepted pattern. If either side throws, skip per fuzzing hygiene rules.
            try {
                ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, registry);
                String reparsed = emf2.toPattern();
                if (before != null && reparsed != null && !before.equals(reparsed)) {
                    throw new RuntimeException(
                        "[oracle:toPattern-idem] metamorphic violation: reparsing canonical pattern changed it input="
                            + pattern + " lhs=" + before + " rhs=" + reparsed);
                }
            } catch (Throwable t) {
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

    private static boolean isRootCause(Throwable t) {
        return t instanceof OutOfMemoryError && hasAppendQuotedStringFrame(t);
    }

    private static boolean hasAppendQuotedStringFrame(Throwable t) {
        StackTraceElement[] frames = t.getStackTrace();
        for (int i = 0; i < frames.length; i++) {
            StackTraceElement ste = frames[i];
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                    && "appendQuotedString".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String n = t.getClass().getName();
        return n.indexOf("Validation") >= 0 || n.indexOf("Invalid") >= 0;
    }

    private static String safeToken(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 32 && c <= 126 && c != '{' && c != '}' && c != '\'') {
                out.append(c);
            } else {
                out.append('x');
            }
        }
        if (out.length() == 0) {
            out.append('x');
        }
        return out.toString();
    }

    private static void throwUnchecked(Throwable t) {
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
<details open><summary>▸ Output (~2,586 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("lower", null);
        registry.put("upper", null);

        // Guaranteed anchor from the regression test: same pattern, same public entry point,
        // and a non-null registry so ExtendedMessageFormat takes its custom-pattern parsing path.
        String anchor = "it''s a {0,lower} 'test'!";
        new ExtendedMessageFormat(anchor, registry).format(new Object[] { "DUMMY" });

        // Explore the same root-cause property: an escaped quote at the start of a quoted run
        // in a pattern parsed by ExtendedMessageFormat with a custom format element.
        String left = clean(data.consumeAsciiString(16));
        String right = clean(data.consumeAsciiString(16));
        String quoted = clean(data.consumeAsciiString(16));
        String suffix = clean(data.consumeAsciiString(16));
        String formatName = data.consumeBoolean() ? "lower" : "upper";

        StringBuilder pattern = new StringBuilder();
        if (data.consumeBoolean()) {
            pattern.append(left);
        } else {
            pattern.append("it");
        }
        pattern.append("''");
        if (data.consumeBoolean()) {
            pattern.append('s');
        }
        pattern.append(' ');
        pattern.append(cleanNonEmpty(right));
        pattern.append(" {0,");
        pattern.append(formatName);
        pattern.append("} '");
        pattern.append(cleanNonEmpty(quoted));
        pattern.append('\'');
        if (data.consumeBoolean()) {
            pattern.append(' ').append(suffix);
        }

        Object arg = data.consumeBoolean() ? "DUMMY" : cleanNonEmpty(data.consumeAsciiString(12));
        new ExtendedMessageFormat(pattern.toString(), registry).format(new Object[] { arg });
    }

    private static String clean(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 32 && c <= 126 && c != '{' && c != '}' && c != '\'') {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static String cleanNonEmpty(String s) {
        String t = clean(s);
        return t.length() == 0 ? "x" : t;
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
**output:** **1/24 dump(s) written under /datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/cov; 24 raw fuzz log(s)**
<details><summary>▸ detail (~11,828 chars)</summary>

- detail: {'dumps': [{'harness': 'attempt_001', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_001_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_002', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_002_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_004', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_004_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_005', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_005_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_006', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_006_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_008', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_008_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_010', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_010_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_011', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_011_compiled.exec', 'exists': True, 'bytes': 210}, {'harness': 'attempt_012', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_012_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_015', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_015_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_016', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_016_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_017', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_017_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_018', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_018_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_019', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_019_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_020', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_020_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_021', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_021_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_022', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_022_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_023', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_023_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_024', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_024_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_025', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_025_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_026', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_026_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_027', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_027_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_029', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_029_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_030', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_030_compiled.exec', 'exists': False, 'bytes': 0}], 'outputs': ['/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_001_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_002_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_004_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_005_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_006_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_008_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_010_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_011_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_012_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_015_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_016_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_017_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_018_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_019_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_020_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_021_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_022_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_023_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_024_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_025_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_026_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_027_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_029_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/fuzz_out/attempt_030_compiled.txt'], 'classpath': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/cov/classpath.json', 'dir': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/cov', 'compiled_attempts': ['attempt_001', 'attempt_002', 'attempt_004', 'attempt_005', 'attempt_006', 'attempt_008', 'attempt_010', 'attempt_011', 'attempt_012', 'attempt_015', 'attempt_016', 'attempt_017', 'attempt_018', 'attempt_019', 'attempt_020', 'attempt_021', 'attempt_022', 'attempt_023', 'attempt_024', 'attempt_025', 'attempt_026', 'attempt_027', 'attempt_029', 'attempt_030'], 'accepted_attempts': ['attempt_011'], 'sources': ['/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_001.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_002.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_004.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_005.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_006.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_008.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_010.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_011.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_012.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_015.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_016.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_017.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_018.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_019.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_020.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_021.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_022.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_023.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_024.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_025.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_026.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_027.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_029.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/harness_src/attempt_030.java']}

</details>
