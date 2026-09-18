# Pipeline trace — Lang-43

**Code:** `b4b78d5146829e25837ef7e7f105b4ec3b686d1e`

**Patch label:** correct  *(the patch under analysis is a known-CORRECT fix — the pipeline is not told this)*

**Outcome:** candidate_experiment (buggy build only)

**Patch under analysis:**
```diff
--- /src/java/org/apache/commons/lang/text/ExtendedMessageFormat.java	
+++ /src/java/org/apache/commons/lang/text/ExtendedMessageFormat.java
@@ -419,7 +419,8 @@ public class ExtendedMessageFormat extends MessageFormat {
         int start = pos.getIndex();
         char[] c = pattern.toCharArray();
         if (escapingOn && c[start] == QUOTE) {
+            next(pos);
             return appendTo == null ? null : appendTo.append(QUOTE);
         }
         int lastHold = start;
         for (int i = pos.getIndex(); i < pattern.length(); i++) {
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
<details><summary>▸ output — FailureTest(test_class='org.apache.commons.lang.text.ExtendedMessageFormatTest', test_method='testEs (~737 chars)</summary>

**output:** 
```json
[
  "FailureTest(test_class='org.apache.commons.lang.text.ExtendedMessageFormatTest', test_method='testEscapedQuote_LANG_477', source_path='/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/Lang_43_buggy/src/test/org/apache/commons/lang/text/ExtendedMessageFormatTest.java', method_source='    public void testEscapedQuote_LANG_477() {\\n        String pattern = \"it\\'\\'s a {0,lower} \\'test\\'!\";\\n        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);\\n        assertEquals(\"it\\'s a dummy test!\", emf.format(new Object[] {\"DUMMY\"}));\\n    }', exception_type='java.lang.OutOfMemoryError', failure_message=None, support_source=None)"
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
<details><summary>▸ output — modified_files": [ (~24,629 chars)</summary>

**output:** 
```json
{
  "modified_files": [
    "src/java/org/apache/commons/lang/text/ExtendedMessageFormat.java"
  ],
  "patch_text": "--- /src/java/org/apache/commons/lang/text/ExtendedMessageFormat.java\t\n+++ /src/java/org/apache/commons/lang/text/ExtendedMessageFormat.java\n@@ -419,7 +419,8 @@ public class ExtendedMessageFormat extends MessageFormat {\n         int start = pos.getIndex();\n         char[] c = pattern.toCharArray();\n         if (escapingOn && c[start] == QUOTE) {\n+            next(pos);\n             return appendTo == null ? null : appendTo.append(QUOTE);\n         }\n         int lastHold = start;\n         for (int i = pos.getIndex(); i < pattern.length(); i++) {\n",
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
<details><summary>▸ Prompt (2 message(s), ~17,331 chars)</summary>

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
@@ -419,7 +419,8 @@ public class ExtendedMessageFormat extends MessageFormat {
         int start = pos.getIndex();
         char[] c = pattern.toCharArray();
         if (escapingOn && c[start] == QUOTE) {
+            next(pos);
             return appendTo == null ? null : appendTo.append(QUOTE);
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
<details open><summary>▸ Output (~7,366 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final String anchorPattern = "it''s a {0,lower} 'test'!";
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(anchorPattern, new HashMap());
            try {
                emf.format(new Object[] { "DUMMY" });
            } catch (Throwable t) {
                boolean cleanRejection =
                        t instanceof IllegalArgumentException ||
                        t instanceof NumberFormatException ||
                        t.getClass().getName().indexOf("Validate") >= 0 ||
                        t.getClass().getName().indexOf("Invalid") >= 0;
                if (!cleanRejection) {
                    boolean throughPatchedMethod = false;
                    StackTraceElement[] st = t.getStackTrace();
                    for (int i = 0; i < st.length; i++) {
                        if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(st[i].getClassName())
                                && "appendQuotedString".equals(st[i].getMethodName())) {
                            throughPatchedMethod = true;
                            break;
                        }
                    }
                    if (t instanceof OutOfMemoryError && throughPatchedMethod) {
                        throw (OutOfMemoryError) t;
                    }
                }
            }
        } catch (Throwable t) {
            boolean cleanRejection =
                    t instanceof IllegalArgumentException ||
                    t instanceof NumberFormatException ||
                    t.getClass().getName().indexOf("Validate") >= 0 ||
                    t.getClass().getName().indexOf("Invalid") >= 0;
            if (!cleanRejection) {
                boolean throughPatchedMethod = false;
                StackTraceElement[] st = t.getStackTrace();
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(st[i].getClassName())
                            && "appendQuotedString".equals(st[i].getMethodName())) {
                        throughPatchedMethod = true;
                        break;
                    }
                }
                if (t instanceof OutOfMemoryError && throughPatchedMethod) {
                    throw (OutOfMemoryError) t;
                }
            }
        }

        String rawA = data.consumeAsciiString(24);
        String rawB = data.consumeAsciiString(24);
        String rawC = data.consumeAsciiString(24);
        String rawD = data.consumeAsciiString(24);
        String rawArg = data.consumeAsciiString(24);

        StringBuilder a = new StringBuilder();
        for (int i = 0; i < rawA.length(); i++) {
            char ch = rawA.charAt(i);
            if (ch != '\'' && ch != '{' && ch != '}') {
                a.append(ch);
            }
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < rawB.length(); i++) {
            char ch = rawB.charAt(i);
            if (ch != '\'' && ch != '{' && ch != '}') {
                b.append(ch);
            }
        }
        StringBuilder c = new StringBuilder();
        for (int i = 0; i < rawC.length(); i++) {
            char ch = rawC.charAt(i);
            if (ch != '\'' && ch != '{' && ch != '}') {
                c.append(ch);
            }
        }
        StringBuilder d = new StringBuilder();
        for (int i = 0; i < rawD.length(); i++) {
            char ch = rawD.charAt(i);
            if (ch != '\'' && ch != '{' && ch != '}') {
                d.append(ch);
            }
        }

        if (a.length() == 0) {
            a.append('A');
        }
        if (b.length() == 0) {
            b.append('B');
        }
        if (c.length() == 0) {
            c.append('C');
        }
        if (d.length() == 0) {
            d.append('D');
        }

        String arg = rawArg.length() == 0 ? "X" : rawArg;

        String pattern;
        switch (data.consumeInt(0, 3)) {
            case 0:
                pattern = a.toString() + "''" + b.toString() + " {0} '" + c.toString() + "'" + d.toString();
                break;
            case 1:
                pattern = "'" + a.toString() + "' " + b.toString() + "''" + c.toString() + " {0} " + d.toString();
                break;
            case 2:
                pattern = a.toString() + " {0} " + b.toString() + "''" + c.toString() + " '" + d.toString() + "'";
                break;
            default:
                pattern = a.toString() + "''" + b.toString() + " '{0}' " + c.toString() + " {0} " + d.toString();
                break;
        }

        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern);
            MessageFormat mf = new MessageFormat(pattern);

            String before = emf.toPattern();
            String lhs = emf.format(new Object[] { arg });
            String after = emf.toPattern();
            String rhs = mf.format(new Object[] { arg });

            /* Contract/oracle:
               For patterns that use only standard MessageFormat syntax (no custom registry formats),
               ExtendedMessageFormat must behave like MessageFormat, and formatting is read-only:
               toPattern() should not change across format(). A patch that merely skips the escaped-quote
               bookkeeping can avoid the crash but silently produce wrong quoting/parsing results. */
            if (!before.equals(after)) {
                throw new RuntimeException("[oracle:topattern] metamorphic violation: format changed toPattern input=" + pattern + " lhs=" + before + " rhs=" + after);
            }
            if (!lhs.equals(rhs)) {
                throw new RuntimeException("[oracle:msgfmt-eq] metamorphic violation: ExtendedMessageFormat must match MessageFormat for standard patterns input=" + pattern + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }

            boolean cleanRejection =
                    t instanceof IllegalArgumentException ||
                    t instanceof NumberFormatException ||
                    t.getClass().getName().indexOf("Validate") >= 0 ||
                    t.getClass().getName().indexOf("Invalid") >= 0;
            if (cleanRejection) {
                return;
            }

            boolean throughPatchedMethod = false;
            StackTraceElement[] st = t.getStackTrace();
            for (int i = 0; i < st.length; i++) {
                if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(st[i].getClassName())
                        && "appendQuotedString".equals(st[i].getMethodName())) {
                    throughPatchedMethod = true;
                    break;
                }
            }

            if (t instanceof OutOfMemoryError && throughPatchedMethod) {
                throw (OutOfMemoryError) t;
            }
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
<details><summary>▸ Prompt (4 message(s), ~10,347 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final String anchorPattern = "it''s a {0,lower} 'test'!";
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(anchorPattern, new HashMap());
            try {
                emf.format(new Object[] { "DUMMY" });
            } catch (Throwable t) {
                boolean cleanRejection =
                        t instanceof IllegalArgumentException ||
                        t instanceof NumberFormatException ||
                        t.getClass().getName().indexOf("Validate") >= 0 ||
                        t.getClass().getName().indexOf("Invalid") >= 0;
                if (!cleanRejection) {
                    boolean throughPatchedMethod = false;
                    StackTraceElement[] st = t.getStackTrace();
                    for (int i = 0; i < st.length; i++) {
                        if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(st[i].getClassName())
                                && "appendQuotedString".equals(st[i].getMethodName())) {
                            throughPatchedMethod = true;
                            break;
                        }
                    }
                    if (t instanceof OutOfMemoryError && throughPatchedMethod) {
                        throw (OutOfMemoryError) t;
                    }
                }
            }
        } catch (Throwable t) {
            boolean cleanRejection =
                    t instanceof IllegalArgumentException ||
                    t instanceof NumberFormatException ||
                    t.getClass().getName().indexOf("Validate") >= 0 ||
                    t.getClass().getName().indexOf("Invalid") >= 0;
            if (!cleanRejection) {
                boolean throughPatchedMethod = false;
                StackTraceElement[] st = t.getStackTrace();
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(st[i].getClassName())
                            && "appendQuotedString".equals(st[i].getMethodName())) {
                        throughPatchedMethod = true;
                        break;
                    }
                }
                if (t instanceof OutOfMemoryError && throughPatchedMethod) {
                    throw (OutOfMemoryError) t;
                }
            }
        }

        String rawA = data.consumeAsciiString(24);
        String rawB = data.consumeAsciiString(24);
        String rawC = data.consumeAsciiString(24);
        String rawD = data.consumeAsciiString(24);
        String rawArg = data.consumeAsciiString(24);

        StringBuilder a = new StringBuilder();
        for (int i = 0; i < rawA.length(); i++) {
            char ch = rawA.charAt(i);
            if (ch != '\'' && ch != '{' && ch != '}') {
                a.append(ch);
            }
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < rawB.length(); i++) {
            char ch = rawB.charAt(i);
            if (ch != '\'' && ch != '{' && ch != '}') {
                b.append(ch);
            }
        }
        StringBuilder c = new StringBuilder();
        for (int i = 0; i < rawC.length(); i++) {
            char ch = rawC.charAt(i);
            if (ch != '\'' && ch != '{' && ch != '}') {
                c.append(ch);
            }
        }
        StringBuilder d = new StringBuilder();
        for (int i = 0; i < rawD.length(); i++) {
            char ch = rawD.charAt(i);
            if (ch != '\'' && ch != '{' && ch != '}') {
                d.append(ch);
            }
        }

        if (a.length() == 0) {
            a.append('A');
        }
        if (b.length() == 0) {
            b.append('B');
        }
        if (c.length() == 0) {
            c.append('C');
        }
        if (d.length() == 0) {
            d.append('D');
        }

        String arg = rawArg.length() == 0 ? "X" : rawArg;

        String pattern;
        switch (data.consumeInt(0, 3)) {
            case 0:
                pattern = a.toString() + "''" + b.toString() + " {0} '" + c.toString() + "'" + d.toString();
                break;
            case 1:
                pattern = "'" + a.toString() + "' " + b.toString() + "''" + c.toString() + " {0} " + d.toString();
                break;
            case 2:
                pattern = a.toString() + " {0} " + b.toString() + "''" + c.toString() + " '" + d.toString() + "'";
                break;
            default:
                pattern = a.toString() + "''" + b.toString() + " '{0}' " + c.toString() + " {0} " + d.toString();
                break;
        }

        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern);
            MessageFormat mf = new MessageFormat(pattern);

            String before = emf.toPattern();
            String lhs = emf.format(new Object[] { arg });
            String after = emf.toPattern();
            String rhs = mf.format(new Object[] { arg });

            /* Contract/oracle:
               For patterns that use only standard MessageFormat syntax (no custom registry formats),
               ExtendedMessageFormat must behave like MessageFormat, and formatting is read-only:
               toPattern() should not change across format(). A patch that merely skips the escaped-quote
               bookkeeping can avoid the crash but silently produce wrong quoting/parsing results. */
            if (!before.equals(after)) {
                throw new RuntimeException("[oracle:topattern] metamorphic violation: format changed toPattern input=" + pattern + " lhs=" + before + " rhs=" + after);
            }
            if (!lhs.equals(rhs)) {
                throw new RuntimeException("[oracle:msgfmt-eq] metamorphic violation: ExtendedMessageFormat must match MessageFormat for standard patterns input=" + pattern + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }

            boolean cleanRejection =
                    t instanceof IllegalArgumentException ||
                    t instanceof NumberFormatException ||
                    t.getClass().getName().indexOf("Validate") >= 0 ||
                    t.getClass().getName().indexOf("Invalid") >= 0;
            if (cleanRejection) {
                return;
            }

            boolean throughPatchedMethod = false;
            StackTraceElement[] st = t.getStackTrace();
            for (int i = 0; i < st.length; i++) {
                if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(st[i].getClassName())
                        && "appendQuotedString".equals(st[i].getMethodName())) {
                    throughPatchedMethod = true;
                    break;
                }
            }

            if (t instanceof OutOfMemoryError && throughPatchedMethod) {
                throw (OutOfMemoryError) t;
            }
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
@@ -419,7 +419,8 @@ public class ExtendedMessageFormat extends MessageFormat {
         int start = pos.getIndex();
         char[] c = pattern.toCharArray();
         if (escapingOn && c[start] == QUOTE) {
+            next(pos);
             return appendTo == null ? null : appendTo.append(QUOTE);
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
<details open><summary>▸ Output (~2,794 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        new ExtendedMessageFormat("it''s a {0} 'test'!");

        String prefix = sanitize(data.consumeAsciiString(16), 'A');
        String middle = sanitize(data.consumeAsciiString(16), 'B');
        String suffix = sanitize(data.consumeAsciiString(16), 'C');
        String quoted = sanitize(data.consumeAsciiString(16), 'D');
        String arg = sanitize(data.consumeAsciiString(16), 'X');

        String pattern;
        switch (data.consumeInt(0, 3)) {
            case 0:
                pattern = prefix + "''" + middle + " {0} '" + quoted + "'" + suffix;
                break;
            case 1:
                pattern = "'" + quoted + "' " + prefix + "''" + middle + " {0} " + suffix;
                break;
            case 2:
                pattern = prefix + " {0} " + middle + "''" + suffix;
                break;
            default:
                pattern = prefix + "''" + middle + " '{0}' " + suffix + " {0}";
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern);
        MessageFormat mf = new MessageFormat(pattern);

        String before = emf.toPattern();
        String emfOut = emf.format(new Object[] { arg });
        String after = emf.toPattern();
        String mfOut = mf.format(new Object[] { arg });

        /* Contract: for standard MessageFormat patterns with no custom registry formats,
           ExtendedMessageFormat should behave like MessageFormat, and format() is read-only.
           A bogus fix that merely avoids the crash by skipping quote handling can change
           parsing/formatting results or mutate the stored pattern. */
        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:topattern] metamorphic violation: format changed toPattern input=" + pattern + " lhs=" + before + " rhs=" + after);
        }
        if (!emfOut.equals(mfOut)) {
            throw new RuntimeException("[oracle:msgfmt-eq] metamorphic violation: ExtendedMessageFormat must match MessageFormat for standard patterns input=" + pattern + " lhs=" + emfOut + " rhs=" + mfOut);
        }
    }

    private static String sanitize(String s, char fallback) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch != '\'' && ch != '{' && ch != '}') {
                out.append(ch);
            }
        }
        if (out.length() == 0) {
            out.append(fallback);
        }
        return out.toString();
    }
}
```

</details>

---
## [6] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. Jazzer exited cleanly with no finding on the buggy code. Most often this means every input was handled normally, or the harnes

---
## [7] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~5,876 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
- *[assistant] message: identical to step [5] — not reprinted*
- *[user] message: identical to step [5] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        new ExtendedMessageFormat("it''s a {0} 'test'!");

        String prefix = sanitize(data.consumeAsciiString(16), 'A');
        String middle = sanitize(data.consumeAsciiString(16), 'B');
        String suffix = sanitize(data.consumeAsciiString(16), 'C');
        String quoted = sanitize(data.consumeAsciiString(16), 'D');
        String arg = sanitize(data.consumeAsciiString(16), 'X');

        String pattern;
        switch (data.consumeInt(0, 3)) {
            case 0:
                pattern = prefix + "''" + middle + " {0} '" + quoted + "'" + suffix;
                break;
            case 1:
                pattern = "'" + quoted + "' " + prefix + "''" + middle + " {0} " + suffix;
                break;
            case 2:
                pattern = prefix + " {0} " + middle + "''" + suffix;
                break;
            default:
                pattern = prefix + "''" + middle + " '{0}' " + suffix + " {0}";
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern);
        MessageFormat mf = new MessageFormat(pattern);

        String before = emf.toPattern();
        String emfOut = emf.format(new Object[] { arg });
        String after = emf.toPattern();
        String mfOut = mf.format(new Object[] { arg });

        /* Contract: for standard MessageFormat patterns with no custom registry formats,
           ExtendedMessageFormat should behave like MessageFormat, and format() is read-only.
           A bogus fix that merely avoids the crash by skipping quote handling can change
           parsing/formatting results or mutate the stored pattern. */
        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:topattern] metamorphic violation: format changed toPattern input=" + pattern + " lhs=" + before + " rhs=" + after);
        }
        if (!emfOut.equals(mfOut)) {
            throw new RuntimeException("[oracle:msgfmt-eq] metamorphic violation: ExtendedMessageFormat must match MessageFormat for standard patterns input=" + pattern + " lhs=" + emfOut + " rhs=" + mfOut);
        }
    }

    private static String sanitize(String s, char fallback) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch != '\'' && ch != '{' && ch != '}') {
                out.append(ch);
            }
        }
        if (out.length() == 0) {
            out.append(fallback);
        }
        return out.toString();
    }
}
```
**[user]**
```
That compiled, but it did NOT trigger the bug on the known-buggy version. Jazzer exited cleanly with no finding on the buggy code. Most often this means every input was handled normally, or the harness itself caught and swallowed the exception that should have propagated.

The patch under analysis is below. The buggy version is the code BEFORE this patch is applied (the '+' lines are the FIXED code; the buggy code has the '-' lines instead):
--- /src/java/org/apache/commons/lang/text/ExtendedMessageFormat.java	
+++ /src/java/org/apache/commons/lang/text/ExtendedMessageFormat.java
@@ -419,7 +419,8 @@ public class ExtendedMessageFormat extends MessageFormat {
         int start = pos.getIndex();
         char[] c = pattern.toCharArray();
         if (escapingOn && c[start] == QUOTE) {
+            next(pos);
             return appendTo == null ? null : appendTo.append(QUOTE);
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
<details open><summary>▸ Output (~5,870 chars)</summary>

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
    private static final Map REGISTRY = new HashMap();

    static {
        REGISTRY.put("lower", new LowerCaseFormatFactory());
        REGISTRY.put("upper", new UpperCaseFormatFactory());
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, REGISTRY);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: exact regression input formatted incorrectly input=" + anchorPattern + " lhs=" + anchorOut + " rhs=it's a dummy test!");
        }
        String anchorBefore = anchor.toPattern();
        String anchorAfter = anchor.toPattern();
        if (!anchorBefore.equals(anchorAfter)) {
            throw new RuntimeException("[oracle:anchor-topattern] metamorphic violation: toPattern changed across read-only observation input=" + anchorPattern + " lhs=" + anchorBefore + " rhs=" + anchorAfter);
        }

        String prefix = sanitize(data.consumeAsciiString(12), 'A');
        String middle = sanitize(data.consumeAsciiString(12), 'B');
        String quoted = sanitize(data.consumeAsciiString(12), 'C');
        String suffix = sanitize(data.consumeAsciiString(12), 'D');
        String arg = sanitize(data.consumeAsciiString(12), 'X');

        String pattern;
        switch (data.consumeInt(0, 3)) {
            case 0:
                pattern = prefix + "''" + middle + " {0,lower} '" + quoted + "'" + suffix;
                break;
            case 1:
                pattern = "'" + quoted + "' " + prefix + "''" + middle + " {0,lower} " + suffix;
                break;
            case 2:
                pattern = prefix + " {0,lower} " + middle + "''" + suffix + " '" + quoted + "'";
                break;
            default:
                pattern = prefix + "''" + middle + " '{0}' " + suffix + " {0,lower}";
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);
        String before = emf.toPattern();
        String actual = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        String referencePattern = pattern.replace("{0,lower}", "{0}");
        MessageFormat mf = new MessageFormat(referencePattern);
        String expected = mf.format(new Object[] { arg.toLowerCase(Locale.ROOT) });

        /* Contract/oracle:
           For a registered "lower" format, formatting "{0,lower}" is equivalent to formatting "{0}"
           with the argument lowercased beforehand. This uses real library formatting on both sides.
           Also, format() is read-only with respect to the stored pattern, so toPattern() must stay stable.
           A patch that merely skips/changes the quote-handling bookkeeping can avoid the crash but
           silently mis-parse doubled quotes or mutate the reconstructed pattern. */
        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:topattern] metamorphic violation: format changed toPattern input=" + pattern + " lhs=" + before + " rhs=" + after);
        }
        if (!actual.equals(expected)) {
            throw new RuntimeException("[oracle:lower-eq] metamorphic violation: custom lower format disagrees with equivalent real formatting input=" + pattern + " lhs=" + actual + " rhs=" + expected);
        }
    }

    private static String sanitize(String s, char fallback) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch != '\'' && ch != '{' && ch != '}') {
                out.append(ch);
            }
        }
        if (out.length() == 0) {
            out.append(fallback);
        }
        return out.toString();
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return LowerCaseFormat.INSTANCE;
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return UpperCaseFormat.INSTANCE;
        }
    }

    private static final class LowerCaseFormat extends Format {
        private static final LowerCaseFormat INSTANCE = new LowerCaseFormat();

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj == null) {
                return toAppendTo;
            }
            return toAppendTo.append(obj.toString().toLowerCase(Locale.ROOT));
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source;
        }
    }

    private static final class UpperCaseFormat extends Format {
        private static final UpperCaseFormat INSTANCE = new UpperCaseFormat();

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj == null) {
                return toAppendTo;
            }
            return toAppendTo.append(obj.toString().toUpperCase(Locale.ROOT));
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
<details><summary>▸ Prompt (8 message(s), ~6,305 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
- *[assistant] message: identical to step [5] — not reprinted*
- *[user] message: identical to step [5] — not reprinted*
- *[assistant] message: identical to step [7] — not reprinted*
- *[user] message: identical to step [7] — not reprinted*
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
    private static final Map REGISTRY = new HashMap();

    static {
        REGISTRY.put("lower", new LowerCaseFormatFactory());
        REGISTRY.put("upper", new UpperCaseFormatFactory());
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, REGISTRY);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: exact regression input formatted incorrectly input=" + anchorPattern + " lhs=" + anchorOut + " rhs=it's a dummy test!");
        }
        String anchorBefore = anchor.toPattern();
        String anchorAfter = anchor.toPattern();
        if (!anchorBefore.equals(anchorAfter)) {
            throw new RuntimeException("[oracle:anchor-topattern] metamorphic violation: toPattern changed across read-only observation input=" + anchorPattern + " lhs=" + anchorBefore + " rhs=" + anchorAfter);
        }

        String prefix = sanitize(data.consumeAsciiString(12), 'A');
        String middle = sanitize(data.consumeAsciiString(12), 'B');
        String quoted = sanitize(data.consumeAsciiString(12), 'C');
        String suffix = sanitize(data.consumeAsciiString(12), 'D');
        String arg = sanitize(data.consumeAsciiString(12), 'X');

        String pattern;
        switch (data.consumeInt(0, 3)) {
            case 0:
                pattern = prefix + "''" + middle + " {0,lower} '" + quoted + "'" + suffix;
                break;
            case 1:
                pattern = "'" + quoted + "' " + prefix + "''" + middle + " {0,lower} " + suffix;
                break;
            case 2:
                pattern = prefix + " {0,lower} " + middle + "''" + suffix + " '" + quoted + "'";
                break;
            default:
                pattern = prefix + "''" + middle + " '{0}' " + suffix + " {0,lower}";
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);
        String before = emf.toPattern();
        String actual = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        String referencePattern = pattern.replace("{0,lower}", "{0}");
        MessageFormat mf = new MessageFormat(referencePattern);
        String expected = mf.format(new Object[] { arg.toLowerCase(Locale.ROOT) });

        /* Contract/oracle:
           For a registered "lower" format, formatting "{0,lower}" is equivalent to formatting "{0}"
           with the argument lowercased beforehand. This uses real library formatting on both sides.
           Also, format() is read-only with respect to the stored pattern, so toPattern() must stay stable.
           A patch that merely skips/changes the quote-handling bookkeeping can avoid the crash but
           silently mis-parse doubled quotes or mutate the reconstructed pattern. */
        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:topattern] metamorphic violation: format changed toPattern input=" + pattern + " lhs=" + before + " rhs=" + after);
        }
        if (!actual.equals(expected)) {
            throw new RuntimeException("[oracle:lower-eq] metamorphic violation: custom lower format disagrees with equivalent real formatting input=" + pattern + " lhs=" + actual + " rhs=" + expected);
        }
    }

    private static String sanitize(String s, char fallback) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch != '\'' && ch != '{' && ch != '}') {
                out.append(ch);
            }
        }
        if (out.length() == 0) {
            out.append(fallback);
        }
        return out.toString();
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return LowerCaseFormat.INSTANCE;
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return UpperCaseFormat.INSTANCE;
        }
    }

    private static final class LowerCaseFormat extends Format {
        private static final LowerCaseFormat INSTANCE = new LowerCaseFormat();

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj == null) {
                return toAppendTo;
            }
            return toAppendTo.append(obj.toString().toLowerCase(Locale.ROOT));
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source;
        }
    }

    private static final class UpperCaseFormat extends Format {
        private static final UpperCaseFormat INSTANCE = new UpperCaseFormat();

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj == null) {
                return toAppendTo;
            }
            return toAppendTo.append(obj.toString().toUpperCase(Locale.ROOT));
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
<details open><summary>▸ Output (~5,696 chars)</summary>

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
        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, REGISTRY);

        String anchorBefore = anchor.toPattern();
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        String anchorAfter = anchor.toPattern();

        /* Contract asserted:
           the exact regression input from the real test must format to "it's a dummy test!",
           and format() must not mutate the stored pattern. A patch that merely avoids the
           crashing branch by skipping quote bookkeeping can return a wrong string or corrupt
           toPattern() while no exception fires. */
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: exact regression input formatted incorrectly input=" + anchorPattern + " lhs=" + anchorOut + " rhs=it's a dummy test!");
        }
        if (!anchorBefore.equals(anchorAfter)) {
            throw new RuntimeException("[oracle:anchor-topattern] metamorphic violation: format changed toPattern input=" + anchorPattern + " lhs=" + anchorBefore + " rhs=" + anchorAfter);
        }

        String p1 = clean(data.consumeAsciiString(12), 'A');
        String p2 = clean(data.consumeAsciiString(12), 'B');
        String p3 = clean(data.consumeAsciiString(12), 'C');
        String arg = clean(data.consumeAsciiString(12), 'X');

        String pattern;
        switch (data.consumeInt(0, 3)) {
            case 0:
                pattern = p1 + "''" + p2 + " {0,lower} '" + p3 + "'!";
                break;
            case 1:
                pattern = "'" + p1 + "' " + p2 + "''" + p3 + " {0,lower}!";
                break;
            case 2:
                pattern = p1 + " {0,lower} " + p2 + "''" + p3 + "!";
                break;
            default:
                pattern = p1 + "''" + p2 + " {0,upper} '" + p3 + "'!";
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);
        String before = emf.toPattern();
        String out = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:topattern] metamorphic violation: format changed toPattern input=" + pattern + " lhs=" + before + " rhs=" + after);
        }

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, REGISTRY);
        String out2 = emf2.format(new Object[] { arg });

        /* Contract asserted:
           toPattern() returns a pattern representation equivalent to the current formatter.
           Reconstructing a new ExtendedMessageFormat from toPattern() must preserve format output
           for the same arguments. A throw-deleting patch in quote parsing can silently emit a
           non-equivalent pattern. */
        if (!out.equals(out2)) {
            throw new RuntimeException("[oracle:roundtrip] metamorphic violation: rebuilding from toPattern changed formatting input=" + pattern + " lhs=" + out + " rhs=" + out2);
        }
    }

    private static String clean(String s, char fallback) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch != '\'' && ch != '{' && ch != '}') {
                b.append(ch);
            }
        }
        if (b.length() == 0) {
            b.append(fallback);
        }
        return b.toString();
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return LowerCaseFormat.INSTANCE;
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return UpperCaseFormat.INSTANCE;
        }
    }

    private static final class LowerCaseFormat extends Format {
        private static final LowerCaseFormat INSTANCE = new LowerCaseFormat();

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toLowerCase(Locale.ROOT));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source;
        }
    }

    private static final class UpperCaseFormat extends Format {
        private static final UpperCaseFormat INSTANCE = new UpperCaseFormat();

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toUpperCase(Locale.ROOT));
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
<details open><summary>▸ Output (~6,815 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        // ANCHOR: exact failing-test pattern first.
        drivePattern("it''s a {0,lower} 'test'!", registry, "DUMMY");

        // EXPLORE: generate many real MessageFormat-compatible patterns that still
        // exercise escaped-quote handling on the public ExtendedMessageFormat API.
        String a = sanitizeLiteral(data.consumeAsciiString(24));
        String b = sanitizeLiteral(data.consumeAsciiString(24));
        String c = sanitizeLiteral(data.consumeAsciiString(24));
        String d = sanitizeLiteral(data.consumeAsciiString(24));

        if (a.length() == 0) {
            a = "a";
        }
        if (b.length() == 0) {
            b = "b";
        }
        if (c.length() == 0) {
            c = "c";
        }

        StringBuilder pattern = new StringBuilder();
        pattern.append(a);

        // Root-cause property: include at least one escaped quote sequence outside
        // a format element so appendQuotedString is reached with escaping enabled.
        int escapedQuoteCount = data.consumeInt(1, 3);
        for (int i = 0; i < escapedQuoteCount; i++) {
            pattern.append("''");
            pattern.append((i % 2 == 0) ? b : c);
        }

        if (data.consumeBoolean()) {
            pattern.append(' ');
        }
        pattern.append("{0}");

        // Add a well-formed quoted literal section to vary positions/surroundings.
        if (data.consumeBoolean()) {
            pattern.append(' ');
            pattern.append('\'').append(c).append('\'');
        }

        if (data.consumeBoolean()) {
            pattern.append(d);
        }

        drivePattern(pattern.toString(), registry, data.consumeString(32));
    }

    private static void drivePattern(String pattern, Map registry, Object arg) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

            // Contract/metamorphic check:
            // For patterns using only standard MessageFormat syntax (i.e. no custom
            // registry formats), ExtendedMessageFormat should behave like the real
            // JDK MessageFormat on the same pattern and arguments.
            // Also, format(...) is observationally read-only with respect to the
            // applied pattern, so toPattern() must not change across formatting.
            if (pattern.indexOf(",lower") < 0 && pattern.indexOf(",upper") < 0) {
                try {
                    String before = emf.toPattern();
                    String lhs = emf.format(new Object[] { arg });

                    MessageFormat mf = new MessageFormat(pattern);
                    String rhs = mf.format(new Object[] { arg });

                    String after = emf.toPattern();
                    if (!safeEquals(before, after)) {
                        throw new RuntimeException(
                                "[oracle:topattern-stable] metamorphic violation: format changed toPattern input="
                                        + pattern + " lhs=" + before + " rhs=" + after);
                    }
                    if (!safeEquals(lhs, rhs)) {
                        throw new RuntimeException(
                                "[oracle:mf-equivalence] metamorphic violation: ExtendedMessageFormat and MessageFormat disagree input="
                                        + pattern + " lhs=" + lhs + " rhs=" + rhs);
                    }
                } catch (Throwable t) {
                    if (isRootCause(t)) {
                        throwUnchecked(t);
                    }
                    if (t instanceof RuntimeException && isOracleFailure((RuntimeException) t)) {
                        throw (RuntimeException) t;
                    }
                    if (isCleanRejection(t)) {
                        return;
                    }
                    return;
                }
            } else {
                try {
                    emf.format(new Object[] { arg });
                } catch (Throwable t) {
                    if (isRootCause(t)) {
                        throwUnchecked(t);
                    }
                    if (isCleanRejection(t)) {
                        return;
                    }
                    return;
                }
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
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
            StackTraceElement ste = stack[i];
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                    && "appendQuotedString".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String cn = t.getClass().getName();
        return cn != null && (cn.contains("Validation") || cn.contains("Invalid"));
    }

    private static boolean isOracleFailure(RuntimeException t) {
        String msg = t.getMessage();
        return msg != null && msg.startsWith("[oracle:");
    }

    private static String sanitizeLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'') {
                continue;
            }
            out.append(ch);
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
## [12] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [13] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~7,012 chars, 1 new)</summary>

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

        // ANCHOR: exact failing-test pattern first.
        drivePattern("it''s a {0,lower} 'test'!", registry, "DUMMY");

        // EXPLORE: generate many real MessageFormat-compatible patterns that still
        // exercise escaped-quote handling on the public ExtendedMessageFormat API.
        String a = sanitizeLiteral(data.consumeAsciiString(24));
        String b = sanitizeLiteral(data.consumeAsciiString(24));
        String c = sanitizeLiteral(data.consumeAsciiString(24));
        String d = sanitizeLiteral(data.consumeAsciiString(24));

        if (a.length() == 0) {
            a = "a";
        }
        if (b.length() == 0) {
            b = "b";
        }
        if (c.length() == 0) {
            c = "c";
        }

        StringBuilder pattern = new StringBuilder();
        pattern.append(a);

        // Root-cause property: include at least one escaped quote sequence outside
        // a format element so appendQuotedString is reached with escaping enabled.
        int escapedQuoteCount = data.consumeInt(1, 3);
        for (int i = 0; i < escapedQuoteCount; i++) {
            pattern.append("''");
            pattern.append((i % 2 == 0) ? b : c);
        }

        if (data.consumeBoolean()) {
            pattern.append(' ');
        }
        pattern.append("{0}");

        // Add a well-formed quoted literal section to vary positions/surroundings.
        if (data.consumeBoolean()) {
            pattern.append(' ');
            pattern.append('\'').append(c).append('\'');
        }

        if (data.consumeBoolean()) {
            pattern.append(d);
        }

        drivePattern(pattern.toString(), registry, data.consumeString(32));
    }

    private static void drivePattern(String pattern, Map registry, Object arg) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

            // Contract/metamorphic check:
            // For patterns using only standard MessageFormat syntax (i.e. no custom
            // registry formats), ExtendedMessageFormat should behave like the real
            // JDK MessageFormat on the same pattern and arguments.
            // Also, format(...) is observationally read-only with respect to the
            // applied pattern, so toPattern() must not change across formatting.
            if (pattern.indexOf(",lower") < 0 && pattern.indexOf(",upper") < 0) {
                try {
                    String before = emf.toPattern();
                    String lhs = emf.format(new Object[] { arg });

                    MessageFormat mf = new MessageFormat(pattern);
                    String rhs = mf.format(new Object[] { arg });

                    String after = emf.toPattern();
                    if (!safeEquals(before, after)) {
                        throw new RuntimeException(
                                "[oracle:topattern-stable] metamorphic violation: format changed toPattern input="
                                        + pattern + " lhs=" + before + " rhs=" + after);
                    }
                    if (!safeEquals(lhs, rhs)) {
                        throw new RuntimeException(
                                "[oracle:mf-equivalence] metamorphic violation: ExtendedMessageFormat and MessageFormat disagree input="
                                        + pattern + " lhs=" + lhs + " rhs=" + rhs);
                    }
                } catch (Throwable t) {
                    if (isRootCause(t)) {
                        throwUnchecked(t);
                    }
                    if (t instanceof RuntimeException && isOracleFailure((RuntimeException) t)) {
                        throw (RuntimeException) t;
                    }
                    if (isCleanRejection(t)) {
                        return;
                    }
                    return;
                }
            } else {
                try {
                    emf.format(new Object[] { arg });
                } catch (Throwable t) {
                    if (isRootCause(t)) {
                        throwUnchecked(t);
                    }
                    if (isCleanRejection(t)) {
                        return;
                    }
                    return;
                }
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
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
            StackTraceElement ste = stack[i];
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                    && "appendQuotedString".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String cn = t.getClass().getName();
        return cn != null && (cn.contains("Validation") || cn.contains("Invalid"));
    }

    private static boolean isOracleFailure(RuntimeException t) {
        String msg = t.getMessage();
        return msg != null && msg.startsWith("[oracle:");
    }

    private static String sanitizeLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'') {
                continue;
            }
            out.append(ch);
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
<details open><summary>▸ Output (~4,805 chars)</summary>

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
        // ANCHOR: exact regression test input. On the buggy version this reaches
        // ExtendedMessageFormat.appendQuotedString and triggers the verified OOME.
        exercise("it''s a {0,lower} 'test'!");

        // EXPLORE: same root-cause property as the patch:
        // when escapingOn is true and the current character is a quote from an
        // escaped quote sequence ("''"), appendQuotedString must advance ParsePosition.
        // Build only valid patterns so fixed code accepts them.
        String prefix = atom(data.consumeAsciiString(16));
        String mid = atom(data.consumeAsciiString(16));
        String suffix = atom(data.consumeAsciiString(16));

        int repeats = data.consumeInt(1, 4);
        StringBuilder pattern = new StringBuilder();
        pattern.append(prefix);

        for (int i = 0; i < repeats; i++) {
            pattern.append("''");
            pattern.append((i & 1) == 0 ? mid : suffix);
            if (data.consumeBoolean()) {
                pattern.append(' ');
            }
        }

        if (data.consumeBoolean()) {
            pattern.append(' ');
            pattern.append('\'').append(atom(data.consumeAsciiString(12))).append('\'');
        }

        if (data.consumeBoolean()) {
            pattern.append(' ');
        }
        pattern.append("{0,lower}");

        if (data.consumeBoolean()) {
            pattern.append(' ');
            pattern.append(atom(data.consumeAsciiString(12)));
        }

        exercise(pattern.toString());
    }

    private static void exercise(String pattern) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);

        // Oracle: constructing from a valid pattern and then reading it back via
        // toPattern() must preserve the pattern, and re-parsing that returned
        // pattern must also preserve it. A "fix" that merely skips quote handling
        // or drops bookkeeping for custom formats would violate this observable API.
        String roundTrip = emf.toPattern();
        if (!pattern.equals(roundTrip)) {
            throw new RuntimeException(
                    "[oracle:pattern-roundtrip] metamorphic violation: toPattern must preserve valid input pattern input="
                            + pattern + " lhs=" + pattern + " rhs=" + roundTrip);
        }

        String roundTrip2 = new ExtendedMessageFormat(roundTrip, REGISTRY).toPattern();
        if (!roundTrip.equals(roundTrip2)) {
            throw new RuntimeException(
                    "[oracle:pattern-idempotent] metamorphic violation: reparsing toPattern must be stable input="
                            + pattern + " lhs=" + roundTrip + " rhs=" + roundTrip2);
        }
    }

    private static String atom(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch != '{' && ch != '}' && ch != '\'' && ch != ',') {
                out.append(ch);
            }
        }
        if (out.length() == 0) {
            return "x";
        }
        return out.toString();
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return PassthroughFormat.INSTANCE;
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return PassthroughFormat.INSTANCE;
        }
    }

    private static final class PassthroughFormat extends Format {
        private static final PassthroughFormat INSTANCE = new PassthroughFormat();

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            return toAppendTo.append(obj == null ? "null" : String.valueOf(obj));
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
<details><summary>▸ Prompt (6 message(s), ~5,122 chars, 1 new)</summary>

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
    private static final Map REGISTRY = new HashMap();

    static {
        REGISTRY.put("lower", new LowerCaseFormatFactory());
        REGISTRY.put("upper", new UpperCaseFormatFactory());
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact regression test input. On the buggy version this reaches
        // ExtendedMessageFormat.appendQuotedString and triggers the verified OOME.
        exercise("it''s a {0,lower} 'test'!");

        // EXPLORE: same root-cause property as the patch:
        // when escapingOn is true and the current character is a quote from an
        // escaped quote sequence ("''"), appendQuotedString must advance ParsePosition.
        // Build only valid patterns so fixed code accepts them.
        String prefix = atom(data.consumeAsciiString(16));
        String mid = atom(data.consumeAsciiString(16));
        String suffix = atom(data.consumeAsciiString(16));

        int repeats = data.consumeInt(1, 4);
        StringBuilder pattern = new StringBuilder();
        pattern.append(prefix);

        for (int i = 0; i < repeats; i++) {
            pattern.append("''");
            pattern.append((i & 1) == 0 ? mid : suffix);
            if (data.consumeBoolean()) {
                pattern.append(' ');
            }
        }

        if (data.consumeBoolean()) {
            pattern.append(' ');
            pattern.append('\'').append(atom(data.consumeAsciiString(12))).append('\'');
        }

        if (data.consumeBoolean()) {
            pattern.append(' ');
        }
        pattern.append("{0,lower}");

        if (data.consumeBoolean()) {
            pattern.append(' ');
            pattern.append(atom(data.consumeAsciiString(12)));
        }

        exercise(pattern.toString());
    }

    private static void exercise(String pattern) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);

        // Oracle: constructing from a valid pattern and then reading it back via
        // toPattern() must preserve the pattern, and re-parsing that returned
        // pattern must also preserve it. A "fix" that merely skips quote handling
        // or drops bookkeeping for custom formats would violate this observable API.
        String roundTrip = emf.toPattern();
        if (!pattern.equals(roundTrip)) {
            throw new RuntimeException(
                    "[oracle:pattern-roundtrip] metamorphic violation: toPattern must preserve valid input pattern input="
                            + pattern + " lhs=" + pattern + " rhs=" + roundTrip);
        }

        String roundTrip2 = new ExtendedMessageFormat(roundTrip, REGISTRY).toPattern();
        if (!roundTrip.equals(roundTrip2)) {
            throw new RuntimeException(
                    "[oracle:pattern-idempotent] metamorphic violation: reparsing toPattern must be stable input="
                            + pattern + " lhs=" + roundTrip + " rhs=" + roundTrip2);
        }
    }

    private static String atom(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch != '{' && ch != '}' && ch != '\'' && ch != ',') {
                out.append(ch);
            }
        }
        if (out.length() == 0) {
            return "x";
        }
        return out.toString();
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return PassthroughFormat.INSTANCE;
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return PassthroughFormat.INSTANCE;
        }
    }

    private static final class PassthroughFormat extends Format {
        private static final PassthroughFormat INSTANCE = new PassthroughFormat();

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            return toAppendTo.append(obj == null ? "null" : String.valueOf(obj));
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
    private static final Map REGISTRY = new HashMap();

    static {
        REGISTRY.put("lower", new LowerCaseFormatFactory());
        REGISTRY.put("upper", new UpperCaseFormatFactory());
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, REGISTRY);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException(
                    "[oracle:anchor-output] metamorphic violation: exact regression pattern must format correctly input="
                            + anchorPattern + " lhs=" + anchorOut + " rhs=it's a dummy test!");
        }

        String pre = atom(data.consumeAsciiString(12));
        String mid = atom(data.consumeAsciiString(12));
        String quoted = atom(data.consumeAsciiString(12));
        String post = atom(data.consumeAsciiString(12));
        String arg = atom(data.consumeAsciiString(12)).toUpperCase();

        int escapedQuotes = data.consumeInt(1, 4);

        StringBuilder pattern = new StringBuilder();
        pattern.append(pre);

        for (int i = 0; i < escapedQuotes; i++) {
            pattern.append("''");
            pattern.append((i & 1) == 0 ? mid : post);
            if (data.consumeBoolean()) {
                pattern.append(' ');
            }
        }

        if (data.consumeBoolean()) {
            pattern.append('\'').append(quoted).append('\'');
            if (data.consumeBoolean()) {
                pattern.append(' ');
            }
        }

        pattern.append("{0,lower}");

        if (data.consumeBoolean()) {
            pattern.append(' ');
            pattern.append('\'').append(post).append('\'');
        } else {
            pattern.append(post);
        }

        String p = pattern.toString();
        ExtendedMessageFormat emf = new ExtendedMessageFormat(p, REGISTRY);

        String before = emf.toPattern();
        String formatted = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        if (!before.equals(after)) {
            throw new RuntimeException(
                    "[oracle:topattern-stable] metamorphic violation: formatting must not change toPattern input="
                            + p + " lhs=" + before + " rhs=" + after);
        }

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, REGISTRY);
        String reparsed = emf2.toPattern();
        if (!before.equals(reparsed)) {
            throw new RuntimeException(
                    "[oracle:topattern-idempotent] metamorphic violation: reparsing toPattern must be stable input="
                            + p + " lhs=" + before + " rhs=" + reparsed);
        }

        String expectedLower = arg.toLowerCase();
        if (formatted.indexOf(expectedLower) < 0) {
            throw new RuntimeException(
                    "[oracle:lower-format] metamorphic violation: lower format must include lower-cased argument input="
                            + p + " lhs=" + formatted + " rhs=" + expectedLower);
        }
    }

    private static String atom(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch != '{' && ch != '}' && ch != '\'' && ch != ',') {
                out.append(ch);
            }
        }
        if (out.length() == 0) {
            return "x";
        }
        return out.toString();
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
<details><summary>▸ Prompt (8 message(s), ~5,835 chars, 2 new)</summary>

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
    private static final Map REGISTRY = new HashMap();

    static {
        REGISTRY.put("lower", new LowerCaseFormatFactory());
        REGISTRY.put("upper", new UpperCaseFormatFactory());
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, REGISTRY);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException(
                    "[oracle:anchor-output] metamorphic violation: exact regression pattern must format correctly input="
                            + anchorPattern + " lhs=" + anchorOut + " rhs=it's a dummy test!");
        }

        String pre = atom(data.consumeAsciiString(12));
        String mid = atom(data.consumeAsciiString(12));
        String quoted = atom(data.consumeAsciiString(12));
        String post = atom(data.consumeAsciiString(12));
        String arg = atom(data.consumeAsciiString(12)).toUpperCase();

        int escapedQuotes = data.consumeInt(1, 4);

        StringBuilder pattern = new StringBuilder();
        pattern.append(pre);

        for (int i = 0; i < escapedQuotes; i++) {
            pattern.append("''");
            pattern.append((i & 1) == 0 ? mid : post);
            if (data.consumeBoolean()) {
                pattern.append(' ');
            }
        }

        if (data.consumeBoolean()) {
            pattern.append('\'').append(quoted).append('\'');
            if (data.consumeBoolean()) {
                pattern.append(' ');
            }
        }

        pattern.append("{0,lower}");

        if (data.consumeBoolean()) {
            pattern.append(' ');
            pattern.append('\'').append(post).append('\'');
        } else {
            pattern.append(post);
        }

        String p = pattern.toString();
        ExtendedMessageFormat emf = new ExtendedMessageFormat(p, REGISTRY);

        String before = emf.toPattern();
        String formatted = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        if (!before.equals(after)) {
            throw new RuntimeException(
                    "[oracle:topattern-stable] metamorphic violation: formatting must not change toPattern input="
                            + p + " lhs=" + before + " rhs=" + after);
        }

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, REGISTRY);
        String reparsed = emf2.toPattern();
        if (!before.equals(reparsed)) {
            throw new RuntimeException(
                    "[oracle:topattern-idempotent] metamorphic violation: reparsing toPattern must be stable input="
                            + p + " lhs=" + before + " rhs=" + reparsed);
        }

        String expectedLower = arg.toLowerCase();
        if (formatted.indexOf(expectedLower) < 0) {
            throw new RuntimeException(
                    "[oracle:lower-format] metamorphic violation: lower format must include lower-cased argument input="
                            + p + " lhs=" + formatted + " rhs=" + expectedLower);
        }
    }

    private static String atom(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch != '{' && ch != '}' && ch != '\'' && ch != ',') {
                out.append(ch);
            }
        }
        if (out.length() == 0) {
            return "x";
        }
        return out.toString();
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/Lang_43_buggy/fuzz/attempt_007/FuzzHarness.java:11: error: cannot find symbol
        REGISTRY.put("lower", new LowerCaseFormatFactory());
                                  ^
  symbol:   class LowerCaseFormatFactory
  location: class FuzzHarness
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/Lang_43_buggy/fuzz/attempt_007/FuzzHarness.java:12: error: cannot find symbol
        REGISTRY.put("upper", new UpperCaseFormatFactory());
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
<details open><summary>▸ Output (~3,831 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Collections;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String anchorPattern = "it''s a {0} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a DUMMY test!".equals(anchorOut)) {
            throw new RuntimeException(
                    "[oracle:anchor-output] metamorphic violation: exact escaped-quote pattern must format correctly input="
                            + anchorPattern + " lhs=" + anchorOut + " rhs=it's a DUMMY test!");
        }

        String pre = atom(data.consumeAsciiString(16));
        String mid = atom(data.consumeAsciiString(16));
        String quoted = atom(data.consumeAsciiString(16));
        String post = atom(data.consumeAsciiString(16));
        String arg = atom(data.consumeString(16));

        int escapedQuoteRuns = data.consumeInt(1, 4);
        int formatIndex = data.consumeInt(0, 3);

        StringBuilder pattern = new StringBuilder();
        pattern.append(pre);

        for (int i = 0; i < escapedQuoteRuns; i++) {
            pattern.append("''");
            pattern.append((i & 1) == 0 ? mid : post);
            if (data.consumeBoolean()) {
                pattern.append(' ');
            }
        }

        if (data.consumeBoolean()) {
            pattern.append('\'').append(quoted).append('\'');
            if (data.consumeBoolean()) {
                pattern.append(' ');
            }
        }

        pattern.append('{').append(formatIndex).append('}');

        if (data.consumeBoolean()) {
            pattern.append(' ');
            pattern.append('\'').append(post).append('\'');
        } else {
            pattern.append(post);
        }

        String p = pattern.toString();
        ExtendedMessageFormat emf = new ExtendedMessageFormat(p, data.consumeBoolean() ? Collections.EMPTY_MAP : null);
        String before = emf.toPattern();
        String formatted = emf.format(new Object[] { arg, "B", "C", "D" });
        String after = emf.toPattern();

        if (!before.equals(after)) {
            throw new RuntimeException(
                    "[oracle:topattern-stable] metamorphic violation: formatting must not change toPattern input="
                            + p + " lhs=" + before + " rhs=" + after);
        }

        ExtendedMessageFormat reparsed = new ExtendedMessageFormat(before, data.consumeBoolean() ? Collections.EMPTY_MAP : null);
        String reparsedPattern = reparsed.toPattern();
        if (!before.equals(reparsedPattern)) {
            throw new RuntimeException(
                    "[oracle:topattern-idempotent] metamorphic violation: reparsing toPattern must be stable input="
                            + p + " lhs=" + before + " rhs=" + reparsedPattern);
        }

        if (formatted.length() == 0) {
            throw new RuntimeException(
                    "[oracle:nonempty-format] metamorphic violation: valid nonempty pattern with one argument should not format to empty string input="
                            + p + " lhs=" + formatted + " rhs=non-empty");
        }
    }

    private static String atom(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch != '{' && ch != '}' && ch != '\'') {
                out.append(ch);
            }
        }
        if (out.length() == 0) {
            return "x";
        }
        return out.toString();
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
<details open><summary>▸ Output (~6,172 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        // ANCHOR: exact failing-test pattern first. The buggy build reaches
        // ExtendedMessageFormat.appendQuotedString() and blows up with OOME.
        tryExtendedMessageFormat("it''s a {0,lower} 'test'!", registry, new Object[] { "DUMMY" }, false);

        // EXPLORE: vary surrounding text while preserving the root-cause property:
        // a MessageFormat pattern containing an escaped quote token ("''").
        String a = sanitizeLiteral(data.consumeAsciiString(24));
        String b = sanitizeLiteral(data.consumeAsciiString(24));
        String c = sanitizeLiteral(data.consumeAsciiString(24));
        String d = sanitizeLiteral(data.consumeAsciiString(24));

        int variant = data.consumeInt(0, 5);
        String pattern;
        switch (variant) {
            case 0:
                pattern = a + "''" + b + " {0} " + c;
                break;
            case 1:
                pattern = a + " {0} " + b + "''" + c;
                break;
            case 2:
                pattern = "'" + a + "'" + b + "''" + c + " {0}";
                break;
            case 3:
                pattern = a + "''" + b + " '{1}' " + c + " {0}";
                break;
            case 4:
                pattern = a + " {0} " + b + " '' " + c + " {1}";
                break;
            default:
                pattern = a + "''" + b + " '{quoted}' " + c + " {0} " + d;
                break;
        }

        Object[] args = new Object[] {
            data.consumeRemainingAsString(),
            sanitizeLiteral(data.consumeAsciiString(16))
        };

        tryExtendedMessageFormat(pattern, null, args, true);
    }

    private static void tryExtendedMessageFormat(String pattern, Map registry, Object[] args, boolean runOracle) {
        try {
            ExtendedMessageFormat emf = (registry == null)
                    ? new ExtendedMessageFormat(pattern)
                    : new ExtendedMessageFormat(pattern, registry);

            String before = emf.toPattern();
            emf.format(args);
            String after = emf.toPattern();

            if (runOracle) {
                // Oracle: with no custom registry, ExtendedMessageFormat must preserve ordinary
                // MessageFormat behavior for valid MessageFormat patterns. A throw-deleting or
                // bookkeeping-skipping patch in quote handling can silently change parsing/output
                // while no exception is thrown, so compare real-library results.
                try {
                    MessageFormat mf = new MessageFormat(pattern);
                    String lhs = emf.format(args);
                    String rhs = mf.format(args);
                    if (!safeEquals(lhs, rhs)) {
                        throw new RuntimeException(
                                "[oracle:msgfmt-equiv] metamorphic violation: ExtendedMessageFormat without registry must match MessageFormat on the same valid pattern input="
                                        + pattern + " lhs=" + lhs + " rhs=" + rhs);
                    }
                    String mfPattern = mf.toPattern();
                    if (!safeEquals(after, mfPattern)) {
                        throw new RuntimeException(
                                "[oracle:topattern-equiv] metamorphic violation: toPattern must match MessageFormat without custom formats input="
                                        + pattern + " lhs=" + after + " rhs=" + mfPattern);
                    }
                    if (!safeEquals(before, after)) {
                        throw new RuntimeException(
                                "[oracle:topattern-stable] metamorphic violation: formatting must not mutate toPattern input="
                                        + pattern + " lhs=" + before + " rhs=" + after);
                    }
                } catch (Throwable t) {
                    if (t instanceof RuntimeException && ((RuntimeException) t).getMessage() != null
                            && ((RuntimeException) t).getMessage().startsWith("[oracle:")) {
                        throw (RuntimeException) t;
                    }
                    return;
                }
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw (Error) t;
            }
        }
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

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Validation") || name.contains("Invalid");
    }

    private static String sanitizeLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'') {
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

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
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
<details><summary>▸ Prompt (4 message(s), ~6,369 chars, 1 new)</summary>

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

        // ANCHOR: exact failing-test pattern first. The buggy build reaches
        // ExtendedMessageFormat.appendQuotedString() and blows up with OOME.
        tryExtendedMessageFormat("it''s a {0,lower} 'test'!", registry, new Object[] { "DUMMY" }, false);

        // EXPLORE: vary surrounding text while preserving the root-cause property:
        // a MessageFormat pattern containing an escaped quote token ("''").
        String a = sanitizeLiteral(data.consumeAsciiString(24));
        String b = sanitizeLiteral(data.consumeAsciiString(24));
        String c = sanitizeLiteral(data.consumeAsciiString(24));
        String d = sanitizeLiteral(data.consumeAsciiString(24));

        int variant = data.consumeInt(0, 5);
        String pattern;
        switch (variant) {
            case 0:
                pattern = a + "''" + b + " {0} " + c;
                break;
            case 1:
                pattern = a + " {0} " + b + "''" + c;
                break;
            case 2:
                pattern = "'" + a + "'" + b + "''" + c + " {0}";
                break;
            case 3:
                pattern = a + "''" + b + " '{1}' " + c + " {0}";
                break;
            case 4:
                pattern = a + " {0} " + b + " '' " + c + " {1}";
                break;
            default:
                pattern = a + "''" + b + " '{quoted}' " + c + " {0} " + d;
                break;
        }

        Object[] args = new Object[] {
            data.consumeRemainingAsString(),
            sanitizeLiteral(data.consumeAsciiString(16))
        };

        tryExtendedMessageFormat(pattern, null, args, true);
    }

    private static void tryExtendedMessageFormat(String pattern, Map registry, Object[] args, boolean runOracle) {
        try {
            ExtendedMessageFormat emf = (registry == null)
                    ? new ExtendedMessageFormat(pattern)
                    : new ExtendedMessageFormat(pattern, registry);

            String before = emf.toPattern();
            emf.format(args);
            String after = emf.toPattern();

            if (runOracle) {
                // Oracle: with no custom registry, ExtendedMessageFormat must preserve ordinary
                // MessageFormat behavior for valid MessageFormat patterns. A throw-deleting or
                // bookkeeping-skipping patch in quote handling can silently change parsing/output
                // while no exception is thrown, so compare real-library results.
                try {
                    MessageFormat mf = new MessageFormat(pattern);
                    String lhs = emf.format(args);
                    String rhs = mf.format(args);
                    if (!safeEquals(lhs, rhs)) {
                        throw new RuntimeException(
                                "[oracle:msgfmt-equiv] metamorphic violation: ExtendedMessageFormat without registry must match MessageFormat on the same valid pattern input="
                                        + pattern + " lhs=" + lhs + " rhs=" + rhs);
                    }
                    String mfPattern = mf.toPattern();
                    if (!safeEquals(after, mfPattern)) {
                        throw new RuntimeException(
                                "[oracle:topattern-equiv] metamorphic violation: toPattern must match MessageFormat without custom formats input="
                                        + pattern + " lhs=" + after + " rhs=" + mfPattern);
                    }
                    if (!safeEquals(before, after)) {
                        throw new RuntimeException(
                                "[oracle:topattern-stable] metamorphic violation: formatting must not mutate toPattern input="
                                        + pattern + " lhs=" + before + " rhs=" + after);
                    }
                } catch (Throwable t) {
                    if (t instanceof RuntimeException && ((RuntimeException) t).getMessage() != null
                            && ((RuntimeException) t).getMessage().startsWith("[oracle:")) {
                        throw (RuntimeException) t;
                    }
                    return;
                }
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw (Error) t;
            }
        }
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

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Validation") || name.contains("Invalid");
    }

    private static String sanitizeLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'') {
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

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~3,182 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String anchorPattern = "it''s a {0} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        String anchorExpected = new MessageFormat(anchorPattern).format(new Object[] { "DUMMY" });
        if (!anchorExpected.equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-equiv] metamorphic violation: ExtendedMessageFormat must match MessageFormat for standard patterns input=" + anchorPattern + " lhs=" + anchorOut + " rhs=" + anchorExpected);
        }

        String left = literal(data.consumeAsciiString(20));
        String mid = literal(data.consumeAsciiString(20));
        String right = literal(data.consumeAsciiString(20));
        String arg0 = data.consumeRemainingAsString();

        int variant = data.consumeInt(0, 5);
        String pattern;
        switch (variant) {
            case 0:
                pattern = left + "''" + mid + " {0}";
                break;
            case 1:
                pattern = left + " {0} " + mid + "''" + right;
                break;
            case 2:
                pattern = left + "''s " + mid + " {0} '" + right + "'";
                break;
            case 3:
                pattern = "'" + left + "' " + mid + "''" + right + " {0}";
                break;
            case 4:
                pattern = left + "''" + mid + " '{0}' " + right + " {0}";
                break;
            default:
                pattern = left + " {0} " + mid + " '' " + right;
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern);
        String before = emf.toPattern();
        String got = emf.format(new Object[] { arg0 });
        String after = emf.toPattern();

        String expected = new MessageFormat(pattern).format(new Object[] { arg0 });

        if (!expected.equals(got)) {
            throw new RuntimeException("[oracle:msgfmt-equiv] metamorphic violation: ExtendedMessageFormat must match MessageFormat for standard patterns input=" + pattern + " lhs=" + got + " rhs=" + expected);
        }
        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: formatting must not mutate toPattern input=" + pattern + " lhs=" + before + " rhs=" + after);
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
```

</details>

---
## [22] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. Jazzer exited cleanly with no finding on the buggy code. Most often this means every input was handled normally, or the harnes

---
## [23] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~3,499 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
- *[assistant] message: identical to step [21] — not reprinted*
- *[user] message: identical to step [5] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String anchorPattern = "it''s a {0} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        String anchorExpected = new MessageFormat(anchorPattern).format(new Object[] { "DUMMY" });
        if (!anchorExpected.equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-equiv] metamorphic violation: ExtendedMessageFormat must match MessageFormat for standard patterns input=" + anchorPattern + " lhs=" + anchorOut + " rhs=" + anchorExpected);
        }

        String left = literal(data.consumeAsciiString(20));
        String mid = literal(data.consumeAsciiString(20));
        String right = literal(data.consumeAsciiString(20));
        String arg0 = data.consumeRemainingAsString();

        int variant = data.consumeInt(0, 5);
        String pattern;
        switch (variant) {
            case 0:
                pattern = left + "''" + mid + " {0}";
                break;
            case 1:
                pattern = left + " {0} " + mid + "''" + right;
                break;
            case 2:
                pattern = left + "''s " + mid + " {0} '" + right + "'";
                break;
            case 3:
                pattern = "'" + left + "' " + mid + "''" + right + " {0}";
                break;
            case 4:
                pattern = left + "''" + mid + " '{0}' " + right + " {0}";
                break;
            default:
                pattern = left + " {0} " + mid + " '' " + right;
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern);
        String before = emf.toPattern();
        String got = emf.format(new Object[] { arg0 });
        String after = emf.toPattern();

        String expected = new MessageFormat(pattern).format(new Object[] { arg0 });

        if (!expected.equals(got)) {
            throw new RuntimeException("[oracle:msgfmt-equiv] metamorphic violation: ExtendedMessageFormat must match MessageFormat for standard patterns input=" + pattern + " lhs=" + got + " rhs=" + expected);
        }
        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: formatting must not mutate toPattern input=" + pattern + " lhs=" + before + " rhs=" + after);
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
```
- *[user] message: identical to step [7] — not reprinted*

</details>
<details open><summary>▸ Output (~3,783 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        // ANCHOR: exact trigger shape from the regression test, but using a non-null registry so
        // ExtendedMessageFormat takes its custom-pattern parsing path and reaches appendQuotedString.
        // On the buggy version, the escaped quote ("''") causes appendQuotedString to return
        // without advancing ParsePosition, leading to unbounded growth and OutOfMemoryError.
        new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);

        String left = literal(data.consumeAsciiString(16));
        String mid = literal(data.consumeAsciiString(16));
        String right = literal(data.consumeAsciiString(16));
        String arg = data.consumeRemainingAsString();

        // EXPLORE: preserve the root-cause property:
        // - non-null registry, which forces ExtendedMessageFormat through its real parser
        // - an escaped quote token ("''"), which hits the patched branch
        String pattern;
        switch (data.consumeInt(0, 5)) {
            case 0:
                pattern = left + "''" + mid + " {0}";
                break;
            case 1:
                pattern = left + " {0} " + mid + "''" + right;
                break;
            case 2:
                pattern = left + "''s " + mid + " {0} " + right;
                break;
            case 3:
                pattern = "'" + left + "' " + mid + "''" + right + " {0}";
                break;
            case 4:
                pattern = left + " {0} '' " + mid + " " + right;
                break;
            default:
                pattern = left + "''" + mid + " '{quoted}' " + right + " {0}";
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String before = emf.toPattern();
        String actual = emf.format(new Object[] { arg });
        String after = emf.toPattern();
        String expected = new MessageFormat(pattern).format(new Object[] { arg });

        // Oracle 1: with an empty registry and a valid standard MessageFormat pattern, this class
        // should behave like MessageFormat. A bogus "fix" that merely skips quote handling or
        // returns the wrong parsed text would violate this without throwing.
        if (!expected.equals(actual)) {
            throw new RuntimeException(
                "[oracle:msgfmt-equiv] metamorphic violation: ExtendedMessageFormat must match MessageFormat for standard patterns input="
                    + pattern + " lhs=" + actual + " rhs=" + expected);
        }

        // Oracle 2: formatting is read-only; toPattern() should not change across format().
        if (!before.equals(after)) {
            throw new RuntimeException(
                "[oracle:topattern-stable] metamorphic violation: formatting must not mutate toPattern input="
                    + pattern + " lhs=" + before + " rhs=" + after);
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
```

</details>

---
## [24] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [25] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (8 message(s), ~4,220 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
- *[assistant] message: identical to step [21] — not reprinted*
- *[user] message: identical to step [5] — not reprinted*
- *[assistant] message: identical to step [23] — not reprinted*
- *[user] message: identical to step [7] — not reprinted*
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

        // ANCHOR: exact trigger shape from the regression test, but using a non-null registry so
        // ExtendedMessageFormat takes its custom-pattern parsing path and reaches appendQuotedString.
        // On the buggy version, the escaped quote ("''") causes appendQuotedString to return
        // without advancing ParsePosition, leading to unbounded growth and OutOfMemoryError.
        new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);

        String left = literal(data.consumeAsciiString(16));
        String mid = literal(data.consumeAsciiString(16));
        String right = literal(data.consumeAsciiString(16));
        String arg = data.consumeRemainingAsString();

        // EXPLORE: preserve the root-cause property:
        // - non-null registry, which forces ExtendedMessageFormat through its real parser
        // - an escaped quote token ("''"), which hits the patched branch
        String pattern;
        switch (data.consumeInt(0, 5)) {
            case 0:
                pattern = left + "''" + mid + " {0}";
                break;
            case 1:
                pattern = left + " {0} " + mid + "''" + right;
                break;
            case 2:
                pattern = left + "''s " + mid + " {0} " + right;
                break;
            case 3:
                pattern = "'" + left + "' " + mid + "''" + right + " {0}";
                break;
            case 4:
                pattern = left + " {0} '' " + mid + " " + right;
                break;
            default:
                pattern = left + "''" + mid + " '{quoted}' " + right + " {0}";
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String before = emf.toPattern();
        String actual = emf.format(new Object[] { arg });
        String after = emf.toPattern();
        String expected = new MessageFormat(pattern).format(new Object[] { arg });

        // Oracle 1: with an empty registry and a valid standard MessageFormat pattern, this class
        // should behave like MessageFormat. A bogus "fix" that merely skips quote handling or
        // returns the wrong parsed text would violate this without throwing.
        if (!expected.equals(actual)) {
            throw new RuntimeException(
                "[oracle:msgfmt-equiv] metamorphic violation: ExtendedMessageFormat must match MessageFormat for standard patterns input="
                    + pattern + " lhs=" + actual + " rhs=" + expected);
        }

        // Oracle 2: formatting is read-only; toPattern() should not change across format().
        if (!before.equals(after)) {
            throw new RuntimeException(
                "[oracle:topattern-stable] metamorphic violation: formatting must not mutate toPattern input="
                    + pattern + " lhs=" + before + " rhs=" + after);
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
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~5,742 chars)</summary>

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
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: exact regression input must format as the test expects input=" + anchorPattern + " lhs=" + anchorOut + " rhs=it's a dummy test!");
        }

        String left = literal(data.consumeAsciiString(12));
        String mid = literal(data.consumeAsciiString(12));
        String right = literal(data.consumeAsciiString(12));
        String arg = data.consumeRemainingAsString();

        String pattern;
        String expectedPattern;
        switch (data.consumeInt(0, 5)) {
            case 0:
                pattern = left + "''" + mid + " {0,lower} " + right;
                expectedPattern = left + "''" + mid + " {0} " + right;
                break;
            case 1:
                pattern = left + " {0,lower} " + mid + "''" + right;
                expectedPattern = left + " {0} " + mid + "''" + right;
                break;
            case 2:
                pattern = left + "''s " + mid + " {0,lower} '" + right + "'";
                expectedPattern = left + "''s " + mid + " {0} '" + right + "'";
                break;
            case 3:
                pattern = "'" + left + "' " + mid + "''" + right + " {0,lower}";
                expectedPattern = "'" + left + "' " + mid + "''" + right + " {0}";
                break;
            case 4:
                pattern = left + " {0,upper} " + mid + "''" + right;
                expectedPattern = left + " {0} " + mid + "''" + right;
                break;
            default:
                pattern = left + "''" + mid + " '{quoted}' " + right + " {0,lower}";
                expectedPattern = left + "''" + mid + " '{quoted}' " + right + " {0}";
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String before = emf.toPattern();
        String actual = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        Object expectedArg;
        if (pattern.indexOf("{0,upper}") >= 0) {
            expectedArg = arg == null ? null : arg.toUpperCase();
        } else {
            expectedArg = arg == null ? null : arg.toLowerCase();
        }
        String expected = new MessageFormat(expectedPattern).format(new Object[] { expectedArg });

        if (!expected.equals(actual)) {
            throw new RuntimeException("[oracle:format-equiv] metamorphic violation: custom lower/upper formatting with escaped quotes must match equivalent MessageFormat on transformed argument input=" + pattern + " lhs=" + actual + " rhs=" + expected);
        }

        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: formatting must not mutate toPattern input=" + pattern + " lhs=" + before + " rhs=" + after);
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
            } else {
                sb.append(ch);
            }
        }
        if (sb.length() == 0) {
            sb.append('x');
        }
        return sb.toString();
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new LowerCaseFormat();
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new UpperCaseFormat();
        }
    }

    private static final class LowerCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toLowerCase());
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            int index = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(index).toLowerCase();
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
            int index = pos.getIndex();
            pos.setIndex(source.length());
            return source.substring(index).toUpperCase();
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
<details open><summary>▸ Output (~6,924 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        class LowerCaseFormat extends java.text.Format {
            public StringBuffer format(Object obj, StringBuffer toAppendTo, java.text.FieldPosition pos) {
                String s = String.valueOf(obj);
                toAppendTo.append(s.toLowerCase());
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
                toAppendTo.append(s.toUpperCase());
                return toAppendTo;
            }
            public Object parseObject(String source, java.text.ParsePosition pos) {
                pos.setIndex(source == null ? 0 : source.length());
                return source;
            }
        }
        class LowerCaseFormatFactory implements org.apache.commons.lang.text.FormatFactory {
            public java.text.Format getFormat(String name, String args, java.util.Locale locale) {
                return new LowerCaseFormat();
            }
        }
        class UpperCaseFormatFactory implements org.apache.commons.lang.text.FormatFactory {
            public java.text.Format getFormat(String name, String args, java.util.Locale locale) {
                return new UpperCaseFormat();
            }
        }
        class Runner {
            private String sanitize(String s) {
                if (s == null) {
                    return "";
                }
                return s.replace("'", "").replace("{", "").replace("}", "");
            }

            private boolean stackHasAppendQuotedString(Throwable t) {
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(e.getClassName())
                            && "appendQuotedString".equals(e.getMethodName())) {
                        return true;
                    }
                }
                return false;
            }

            void run(String pattern, String arg, String expected, boolean assertOracle) {
                java.util.Map registry = new java.util.HashMap();
                registry.put("lower", new LowerCaseFormatFactory());
                registry.put("upper", new UpperCaseFormatFactory());
                try {
                    ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

                    String before = emf.toPattern();
                    String r1 = emf.format(new Object[] { arg });
                    String after = emf.toPattern();
                    if (!before.equals(after)) {
                        throw new RuntimeException("[oracle:state] metamorphic violation: toPattern changed across format input="
                                + pattern + " before=" + before + " after=" + after);
                    }

                    String r2 = emf.format(new Object[] { arg });
                    if (!r1.equals(r2)) {
                        throw new RuntimeException("[oracle:det] metamorphic violation: repeated format on same input must be deterministic input="
                                + pattern + " lhs=" + r1 + " rhs=" + r2);
                    }

                    if (assertOracle && expected != null && !expected.equals(r1)) {
                        throw new RuntimeException("[oracle:fmt] metamorphic violation: valid pattern must render escaped quote as a literal apostrophe and quoted text without quote delimiters input="
                                + pattern + " lhs=" + r1 + " rhs=" + expected);
                    }
                } catch (Throwable t) {
                    if (t instanceof RuntimeException) {
                        String m = t.getMessage();
                        if (m != null && m.startsWith("[oracle:")) {
                            throw (RuntimeException) t;
                        }
                    }
                    if (t instanceof OutOfMemoryError && stackHasAppendQuotedString(t)) {
                        throw (OutOfMemoryError) t;
                    }
                    if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                        return;
                    }
                    return;
                }
            }

            void runExplore(FuzzedDataProvider d) {
                String pre = sanitize(d.consumeAsciiString(12));
                String mid = sanitize(d.consumeAsciiString(12));
                String lit = sanitize(d.consumeAsciiString(12));
                String post = sanitize(d.consumeAsciiString(12));
                String arg = sanitize(d.consumeAsciiString(12));

                if (mid.length() == 0) {
                    mid = "x";
                }
                if (lit.length() == 0) {
                    lit = "test";
                }
                if (arg.length() == 0) {
                    arg = "DUMMY";
                }

                int variant = d.consumeInt(0, 3);
                String pattern;
                String expected;
                switch (variant) {
                    case 0:
                        pattern = pre + "''" + mid + " {0,lower} '" + lit + "'" + post;
                        expected = pre + "'" + mid + " " + arg.toLowerCase() + " " + lit + post;
                        break;
                    case 1:
                        pattern = pre + "''" + mid + "{0,lower} '" + lit + "'" + post;
                        expected = pre + "'" + mid + arg.toLowerCase() + " " + lit + post;
                        break;
                    case 2:
                        pattern = pre + "''" + mid + " {0,upper} '" + lit + "'" + post;
                        expected = pre + "'" + mid + " " + arg.toUpperCase() + " " + lit + post;
                        break;
                    default:
                        pattern = pre + "''" + mid + "{0,upper} '" + lit + "'" + post;
                        expected = pre + "'" + mid + arg.toUpperCase() + " " + lit + post;
                        break;
                }

                run(pattern, arg, expected, true);
            }
        }

        Runner runner = new Runner();

        runner.run("it''s a {0,lower} 'test'!", "DUMMY", "it's a dummy test!", true);

        int cases = 1 + data.consumeInt(0, 4);
        for (int i = 0; i < cases; i++) {
            runner.runExplore(data);
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
<details><summary>▸ Prompt (4 message(s), ~7,121 chars, 1 new)</summary>

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
                toAppendTo.append(s.toLowerCase());
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
                toAppendTo.append(s.toUpperCase());
                return toAppendTo;
            }
            public Object parseObject(String source, java.text.ParsePosition pos) {
                pos.setIndex(source == null ? 0 : source.length());
                return source;
            }
        }
        class LowerCaseFormatFactory implements org.apache.commons.lang.text.FormatFactory {
            public java.text.Format getFormat(String name, String args, java.util.Locale locale) {
                return new LowerCaseFormat();
            }
        }
        class UpperCaseFormatFactory implements org.apache.commons.lang.text.FormatFactory {
            public java.text.Format getFormat(String name, String args, java.util.Locale locale) {
                return new UpperCaseFormat();
            }
        }
        class Runner {
            private String sanitize(String s) {
                if (s == null) {
                    return "";
                }
                return s.replace("'", "").replace("{", "").replace("}", "");
            }

            private boolean stackHasAppendQuotedString(Throwable t) {
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(e.getClassName())
                            && "appendQuotedString".equals(e.getMethodName())) {
                        return true;
                    }
                }
                return false;
            }

            void run(String pattern, String arg, String expected, boolean assertOracle) {
                java.util.Map registry = new java.util.HashMap();
                registry.put("lower", new LowerCaseFormatFactory());
                registry.put("upper", new UpperCaseFormatFactory());
                try {
                    ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

                    String before = emf.toPattern();
                    String r1 = emf.format(new Object[] { arg });
                    String after = emf.toPattern();
                    if (!before.equals(after)) {
                        throw new RuntimeException("[oracle:state] metamorphic violation: toPattern changed across format input="
                                + pattern + " before=" + before + " after=" + after);
                    }

                    String r2 = emf.format(new Object[] { arg });
                    if (!r1.equals(r2)) {
                        throw new RuntimeException("[oracle:det] metamorphic violation: repeated format on same input must be deterministic input="
                                + pattern + " lhs=" + r1 + " rhs=" + r2);
                    }

                    if (assertOracle && expected != null && !expected.equals(r1)) {
                        throw new RuntimeException("[oracle:fmt] metamorphic violation: valid pattern must render escaped quote as a literal apostrophe and quoted text without quote delimiters input="
                                + pattern + " lhs=" + r1 + " rhs=" + expected);
                    }
                } catch (Throwable t) {
                    if (t instanceof RuntimeException) {
                        String m = t.getMessage();
                        if (m != null && m.startsWith("[oracle:")) {
                            throw (RuntimeException) t;
                        }
                    }
                    if (t instanceof OutOfMemoryError && stackHasAppendQuotedString(t)) {
                        throw (OutOfMemoryError) t;
                    }
                    if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                        return;
                    }
                    return;
                }
            }

            void runExplore(FuzzedDataProvider d) {
                String pre = sanitize(d.consumeAsciiString(12));
                String mid = sanitize(d.consumeAsciiString(12));
                String lit = sanitize(d.consumeAsciiString(12));
                String post = sanitize(d.consumeAsciiString(12));
                String arg = sanitize(d.consumeAsciiString(12));

                if (mid.length() == 0) {
                    mid = "x";
                }
                if (lit.length() == 0) {
                    lit = "test";
                }
                if (arg.length() == 0) {
                    arg = "DUMMY";
                }

                int variant = d.consumeInt(0, 3);
                String pattern;
                String expected;
                switch (variant) {
                    case 0:
                        pattern = pre + "''" + mid + " {0,lower} '" + lit + "'" + post;
                        expected = pre + "'" + mid + " " + arg.toLowerCase() + " " + lit + post;
                        break;
                    case 1:
                        pattern = pre + "''" + mid + "{0,lower} '" + lit + "'" + post;
                        expected = pre + "'" + mid + arg.toLowerCase() + " " + lit + post;
                        break;
                    case 2:
                        pattern = pre + "''" + mid + " {0,upper} '" + lit + "'" + post;
                        expected = pre + "'" + mid + " " + arg.toUpperCase() + " " + lit + post;
                        break;
                    default:
                        pattern = pre + "''" + mid + "{0,upper} '" + lit + "'" + post;
                        expected = pre + "'" + mid + arg.toUpperCase() + " " + lit + post;
                        break;
                }

                run(pattern, arg, expected, true);
            }
        }

        Runner runner = new Runner();

        runner.run("it''s a {0,lower} 'test'!", "DUMMY", "it's a dummy test!", true);

        int cases = 1 + data.consumeInt(0, 4);
        for (int i = 0; i < cases; i++) {
            runner.runExplore(data);
        }
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~4,589 chars)</summary>

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
            return toAppendTo.append(String.valueOf(obj).toLowerCase());
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }

    private static final class UpperCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            return toAppendTo.append(String.valueOf(obj).toUpperCase());
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new LowerCaseFormat();
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
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
        if (s == null) {
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

    private static void drive(String pattern, String arg) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, makeRegistry());

        String before = emf.toPattern();
        String out1 = emf.format(new Object[] { arg });
        String after = emf.toPattern();
        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:state] metamorphic violation: toPattern changed across format input=" + pattern + " before=" + before + " after=" + after);
        }

        String out2 = emf.format(new Object[] { arg });
        if (!out1.equals(out2)) {
            throw new RuntimeException("[oracle:det] metamorphic violation: repeated format must be deterministic input=" + pattern + " lhs=" + out1 + " rhs=" + out2);
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        drive("it''s a {0,lower} 'test'!", "DUMMY");

        int variants = 1 + data.consumeInt(0, 4);
        for (int i = 0; i < variants; i++) {
            String pre = clean(data.consumeAsciiString(8));
            String mid = clean(data.consumeAsciiString(8));
            String lit = clean(data.consumeAsciiString(8));
            String post = clean(data.consumeAsciiString(8));
            String arg = clean(data.consumeAsciiString(8));

            if (mid.length() == 0) {
                mid = "x";
            }
            if (lit.length() == 0) {
                lit = "test";
            }
            if (arg.length() == 0) {
                arg = "DUMMY";
            }

            String fmt = data.consumeBoolean() ? "lower" : "upper";
            boolean withSpaceBeforeFormat = data.consumeBoolean();
            boolean withSpaceBeforeQuoted = data.consumeBoolean();

            StringBuilder pattern = new StringBuilder();
            pattern.append(pre);
            pattern.append("''");
            pattern.append(mid);
            if (withSpaceBeforeFormat) {
                pattern.append(' ');
            }
            pattern.append("{0,");
            pattern.append(fmt);
            pattern.append("}");
            if (withSpaceBeforeQuoted) {
                pattern.append(' ');
            }
            pattern.append('\'');
            pattern.append(lit);
            pattern.append('\'');
            pattern.append(post);

            drive(pattern.toString(), arg);
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
<details><summary>▸ Prompt (6 message(s), ~4,906 chars, 1 new)</summary>

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
    private static final class LowerCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            return toAppendTo.append(String.valueOf(obj).toLowerCase());
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }

    private static final class UpperCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            return toAppendTo.append(String.valueOf(obj).toUpperCase());
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new LowerCaseFormat();
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
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
        if (s == null) {
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

    private static void drive(String pattern, String arg) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, makeRegistry());

        String before = emf.toPattern();
        String out1 = emf.format(new Object[] { arg });
        String after = emf.toPattern();
        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:state] metamorphic violation: toPattern changed across format input=" + pattern + " before=" + before + " after=" + after);
        }

        String out2 = emf.format(new Object[] { arg });
        if (!out1.equals(out2)) {
            throw new RuntimeException("[oracle:det] metamorphic violation: repeated format must be deterministic input=" + pattern + " lhs=" + out1 + " rhs=" + out2);
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        drive("it''s a {0,lower} 'test'!", "DUMMY");

        int variants = 1 + data.consumeInt(0, 4);
        for (int i = 0; i < variants; i++) {
            String pre = clean(data.consumeAsciiString(8));
            String mid = clean(data.consumeAsciiString(8));
            String lit = clean(data.consumeAsciiString(8));
            String post = clean(data.consumeAsciiString(8));
            String arg = clean(data.consumeAsciiString(8));

            if (mid.length() == 0) {
                mid = "x";
            }
            if (lit.length() == 0) {
                lit = "test";
            }
            if (arg.length() == 0) {
                arg = "DUMMY";
            }

            String fmt = data.consumeBoolean() ? "lower" : "upper";
            boolean withSpaceBeforeFormat = data.consumeBoolean();
            boolean withSpaceBeforeQuoted = data.consumeBoolean();

            StringBuilder pattern = new StringBuilder();
            pattern.append(pre);
            pattern.append("''");
            pattern.append(mid);
            if (withSpaceBeforeFormat) {
                pattern.append(' ');
            }
            pattern.append("{0,");
            pattern.append(fmt);
            pattern.append("}");
            if (withSpaceBeforeQuoted) {
                pattern.append(' ');
            }
            pattern.append('\'');
            pattern.append(lit);
            pattern.append('\'');
            pattern.append(post);

            drive(pattern.toString(), arg);
        }
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~5,236 chars)</summary>

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
            return toAppendTo.append(String.valueOf(obj).toLowerCase());
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }

    private static final class UpperCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            return toAppendTo.append(String.valueOf(obj).toUpperCase());
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new LowerCaseFormat();
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
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
        if (s == null) {
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

    private static void exercise(String pattern, String arg, String expected) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, makeRegistry());

        String before = emf.toPattern();
        String actual = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:state] metamorphic violation: toPattern changed across format input=" + pattern + " before=" + before + " after=" + after);
        }

        if (!actual.equals(expected)) {
            throw new RuntimeException("[oracle:fmt] metamorphic violation: formatting with escaped quote and quoted literal must preserve apostrophe semantics input=" + pattern + " lhs=" + actual + " rhs=" + expected);
        }

        String actual2 = emf.format(new Object[] { arg });
        if (!actual.equals(actual2)) {
            throw new RuntimeException("[oracle:det] metamorphic violation: repeated format on same input must be deterministic input=" + pattern + " lhs=" + actual + " rhs=" + actual2);
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exercise("it''s a {0,lower} 'test'!", "DUMMY", "it's a dummy test!");

        String pre = clean(data.consumeAsciiString(6));
        String word1 = clean(data.consumeAsciiString(6));
        String literal = clean(data.consumeAsciiString(6));
        String post = clean(data.consumeAsciiString(6));
        String arg = clean(data.consumeAsciiString(6));

        if (word1.length() == 0) {
            word1 = "a";
        }
        if (literal.length() == 0) {
            literal = "test";
        }
        if (arg.length() == 0) {
            arg = "DUMMY";
        }

        boolean lower = data.consumeBoolean();
        boolean space1 = data.consumeBoolean();
        boolean space2 = data.consumeBoolean();

        StringBuilder pattern = new StringBuilder();
        pattern.append(pre);
        pattern.append("it''s");
        if (space1) {
            pattern.append(' ');
        }
        pattern.append(word1);
        if (space1) {
            pattern.append(' ');
        }
        pattern.append("{0,");
        pattern.append(lower ? "lower" : "upper");
        pattern.append("}");
        if (space2) {
            pattern.append(' ');
        }
        pattern.append('\'');
        pattern.append(literal);
        pattern.append('\'');
        pattern.append(post);

        StringBuilder expected = new StringBuilder();
        expected.append(pre);
        expected.append("it's");
        if (space1) {
            expected.append(' ');
        }
        expected.append(word1);
        if (space1) {
            expected.append(' ');
        }
        expected.append(lower ? arg.toLowerCase() : arg.toUpperCase());
        if (space2) {
            expected.append(' ');
        }
        expected.append(literal);
        expected.append(post);

        exercise(pattern.toString(), arg, expected.toString());
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
<details><summary>▸ Prompt (8 message(s), ~5,673 chars, 1 new)</summary>

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
            return toAppendTo.append(String.valueOf(obj).toLowerCase());
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }

    private static final class UpperCaseFormat extends Format {
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            return toAppendTo.append(String.valueOf(obj).toUpperCase());
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source == null ? 0 : source.length());
            return source;
        }
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new LowerCaseFormat();
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
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
        if (s == null) {
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

    private static void exercise(String pattern, String arg, String expected) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, makeRegistry());

        String before = emf.toPattern();
        String actual = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:state] metamorphic violation: toPattern changed across format input=" + pattern + " before=" + before + " after=" + after);
        }

        if (!actual.equals(expected)) {
            throw new RuntimeException("[oracle:fmt] metamorphic violation: formatting with escaped quote and quoted literal must preserve apostrophe semantics input=" + pattern + " lhs=" + actual + " rhs=" + expected);
        }

        String actual2 = emf.format(new Object[] { arg });
        if (!actual.equals(actual2)) {
            throw new RuntimeException("[oracle:det] metamorphic violation: repeated format on same input must be deterministic input=" + pattern + " lhs=" + actual + " rhs=" + actual2);
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exercise("it''s a {0,lower} 'test'!", "DUMMY", "it's a dummy test!");

        String pre = clean(data.consumeAsciiString(6));
        String word1 = clean(data.consumeAsciiString(6));
        String literal = clean(data.consumeAsciiString(6));
        String post = clean(data.consumeAsciiString(6));
        String arg = clean(data.consumeAsciiString(6));

        if (word1.length() == 0) {
            word1 = "a";
        }
        if (literal.length() == 0) {
            literal = "test";
        }
        if (arg.length() == 0) {
            arg = "DUMMY";
        }

        boolean lower = data.consumeBoolean();
        boolean space1 = data.consumeBoolean();
        boolean space2 = data.consumeBoolean();

        StringBuilder pattern = new StringBuilder();
        pattern.append(pre);
        pattern.append("it''s");
        if (space1) {
            pattern.append(' ');
        }
        pattern.append(word1);
        if (space1) {
            pattern.append(' ');
        }
        pattern.append("{0,");
        pattern.append(lower ? "lower" : "upper");
        pattern.append("}");
        if (space2) {
            pattern.append(' ');
        }
        pattern.append('\'');
        pattern.append(literal);
        pattern.append('\'');
        pattern.append(post);

        StringBuilder expected = new StringBuilder();
        expected.append(pre);
        expected.append("it's");
        if (space1) {
            expected.append(' ');
        }
        expected.append(word1);
        if (space1) {
            expected.append(' ');
        }
        expected.append(lower ? arg.toLowerCase() : arg.toUpperCase());
        if (space2) {
            expected.append(' ');
        }
        expected.append(literal);
        expected.append(post);

        exercise(pattern.toString(), arg, expected.toString());
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~0 chars)</summary>

```

```

</details>

---
## [34] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Your previous response contained no usable Java source. Return the complete FuzzHarness.java now and nothing else: raw Java only, no markdown fences, no commentary, public class named FuzzHarness, ent

---
## [35] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,531 chars)</summary>

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

        String anchor = "it''s a {0,lower} 'test'!";
        try {
            new ExtendedMessageFormat(anchor, Locale.US, registry);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw (OutOfMemoryError) t;
            }
        }

        String pre = sanitize(data.consumeAsciiString(24));
        String mid = sanitize(data.consumeAsciiString(24));
        String post = sanitize(data.consumeAsciiString(24));
        String lit = sanitize(data.consumeAsciiString(16));
        String arg = sanitize(data.consumeString(24));
        int quoteCount = data.consumeInt(1, 3);
        boolean leadingFormat = data.consumeBoolean();
        boolean trailingQuotedLiteral = data.consumeBoolean();

        StringBuilder sb = new StringBuilder();
        if (leadingFormat) {
            sb.append("{0}");
        }
        sb.append(pre);
        for (int i = 0; i < quoteCount; i++) {
            sb.append("''");
            if (i + 1 < quoteCount) {
                sb.append(sanitize(data.consumeAsciiString(8)));
            }
        }
        sb.append(mid);
        sb.append(" ");
        sb.append("{0}");
        if (trailingQuotedLiteral) {
            sb.append(" '").append(lit).append("'");
        }
        sb.append(post);
        String pattern = sb.toString();

        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, Locale.US, registry);
            String before = emf.toPattern();
            String emfOut = emf.format(new Object[] { arg });
            String after = emf.toPattern();

            if (!safeEquals(before, after)) {
                throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: non-mutating format changed toPattern input=" + pattern + " before=" + before + " after=" + after);
            }

            try {
                MessageFormat mf = new MessageFormat(pattern, Locale.US);
                String mfOut = mf.format(new Object[] { arg });

                /*
                 * Contract/oracle: ExtendedMessageFormat is a MessageFormat extension.
                 * For patterns using only standard MessageFormat syntax and no custom registry formats,
                 * formatting should match MessageFormat. A patch that merely suppresses the crashing branch
                 * or skips quote bookkeeping can silently produce the wrong formatted result.
                 */
                if (!safeEquals(emfOut, mfOut)) {
                    throw new RuntimeException("[oracle:msgfmt-equivalence] metamorphic violation: ExtendedMessageFormat and MessageFormat disagree input=" + pattern + " lhs=" + emfOut + " rhs=" + mfOut);
                }
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (t instanceof RuntimeException) {
                    return;
                }
                return;
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw (OutOfMemoryError) t;
            }
            if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
        }
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

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("Validation") || name.contains("InvalidFormat") || name.contains("Malformed")) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == '\'') {
                out.append('x');
            } else if (Character.isISOControl(c)) {
                out.append('y');
            } else {
                out.append(c);
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
## [36] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [37] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~5,728 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
**[assistant]**
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

        String anchor = "it''s a {0,lower} 'test'!";
        try {
            new ExtendedMessageFormat(anchor, Locale.US, registry);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw (OutOfMemoryError) t;
            }
        }

        String pre = sanitize(data.consumeAsciiString(24));
        String mid = sanitize(data.consumeAsciiString(24));
        String post = sanitize(data.consumeAsciiString(24));
        String lit = sanitize(data.consumeAsciiString(16));
        String arg = sanitize(data.consumeString(24));
        int quoteCount = data.consumeInt(1, 3);
        boolean leadingFormat = data.consumeBoolean();
        boolean trailingQuotedLiteral = data.consumeBoolean();

        StringBuilder sb = new StringBuilder();
        if (leadingFormat) {
            sb.append("{0}");
        }
        sb.append(pre);
        for (int i = 0; i < quoteCount; i++) {
            sb.append("''");
            if (i + 1 < quoteCount) {
                sb.append(sanitize(data.consumeAsciiString(8)));
            }
        }
        sb.append(mid);
        sb.append(" ");
        sb.append("{0}");
        if (trailingQuotedLiteral) {
            sb.append(" '").append(lit).append("'");
        }
        sb.append(post);
        String pattern = sb.toString();

        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, Locale.US, registry);
            String before = emf.toPattern();
            String emfOut = emf.format(new Object[] { arg });
            String after = emf.toPattern();

            if (!safeEquals(before, after)) {
                throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: non-mutating format changed toPattern input=" + pattern + " before=" + before + " after=" + after);
            }

            try {
                MessageFormat mf = new MessageFormat(pattern, Locale.US);
                String mfOut = mf.format(new Object[] { arg });

                /*
                 * Contract/oracle: ExtendedMessageFormat is a MessageFormat extension.
                 * For patterns using only standard MessageFormat syntax and no custom registry formats,
                 * formatting should match MessageFormat. A patch that merely suppresses the crashing branch
                 * or skips quote bookkeeping can silently produce the wrong formatted result.
                 */
                if (!safeEquals(emfOut, mfOut)) {
                    throw new RuntimeException("[oracle:msgfmt-equivalence] metamorphic violation: ExtendedMessageFormat and MessageFormat disagree input=" + pattern + " lhs=" + emfOut + " rhs=" + mfOut);
                }
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (t instanceof RuntimeException) {
                    return;
                }
                return;
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw (OutOfMemoryError) t;
            }
            if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
        }
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

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("Validation") || name.contains("InvalidFormat") || name.contains("Malformed")) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '}' || c == '\'') {
                out.append('x');
            } else if (Character.isISOControl(c)) {
                out.append('y');
            } else {
                out.append(c);
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
<details open><summary>▸ Output (~5,395 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        runAndCheck("it''s a {0,lower} 'test'!", "DUMMY", registry, "it's a dummy test!");

        String prefix = clean(data.consumeAsciiString(20));
        String infix = clean(data.consumeAsciiString(20));
        String suffix = clean(data.consumeAsciiString(20));
        String quoted = clean(data.consumeAsciiString(12));
        String arg = cleanArg(data.consumeString(20));
        int escapedPairs = data.consumeInt(1, 4);
        boolean addLeadingText = data.consumeBoolean();
        boolean addTrailingQuoted = data.consumeBoolean();
        boolean useUpper = data.consumeBoolean();

        StringBuilder pattern = new StringBuilder();
        if (addLeadingText) {
            pattern.append(prefix);
            pattern.append(' ');
        }
        for (int i = 0; i < escapedPairs; i++) {
            pattern.append("''");
            if (i + 1 < escapedPairs) {
                pattern.append(clean(data.consumeAsciiString(6)));
            }
        }
        pattern.append(infix);
        pattern.append(" {0,");
        pattern.append(useUpper ? "upper" : "lower");
        pattern.append('}');
        if (addTrailingQuoted) {
            pattern.append(" '");
            pattern.append(quoted);
            pattern.append('\'');
        }
        pattern.append(suffix);

        String p = pattern.toString();
        String expected = expectedOutput(p, arg, useUpper);
        runAndCheck(p, arg, registry, expected);
    }

    private static void runAndCheck(String pattern, String arg, Map registry, String expected) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, Locale.US, registry);
        String before = emf.toPattern();
        String actual = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        /*
         * Oracle: formatting is a read-only operation for the pattern; toPattern() must not change.
         * A bogus fix that merely skips quote-handling/bookkeeping could avoid the crash but mutate or
         * mis-parse the stored pattern.
         */
        if (!eq(before, after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: format changed toPattern input=" + pattern + " before=" + before + " after=" + after);
        }

        /*
         * Oracle from construction: we build only valid patterns of the same form as the regression test:
         * ordinary text + doubled apostrophes + one registered format {0,lower}/{0,upper} + optional quoted
         * literal. For these inputs the formatted output is determined by the input itself.
         */
        if (!eq(expected, actual)) {
            throw new RuntimeException("[oracle:constructed-output] metamorphic violation: wrong formatted output input=" + pattern + " arg=" + arg + " expected=" + expected + " actual=" + actual);
        }
    }

    private static String expectedOutput(String pattern, String arg, boolean useUpper) {
        String transformed = useUpper ? arg.toUpperCase() : arg.toLowerCase();
        String p = pattern.replace("{0,upper}", "\u0000").replace("{0,lower}", "\u0000");
        p = p.replace("''", "'");
        p = stripSimpleQuotes(p);
        return p.replace("\u0000", transformed);
    }

    private static String stripSimpleQuotes(String s) {
        StringBuilder out = new StringBuilder(s.length());
        boolean inQuote = false;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\'') {
                inQuote = !inQuote;
            } else {
                out.append(ch);
            }
        }
        return out.toString();
    }

    private static String clean(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'') {
                out.append('x');
            } else if (Character.isISOControl(ch)) {
                out.append('y');
            } else {
                out.append(ch);
            }
        }
        if (out.length() == 0) {
            out.append('x');
        }
        return out.toString();
    }

    private static String cleanArg(String s) {
        if (s == null || s.length() == 0) {
            return "Dummy";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (Character.isISOControl(ch)) {
                out.append('a');
            } else {
                out.append(ch);
            }
        }
        if (out.length() == 0) {
            out.append("Dummy");
        }
        return out.toString();
    }

    private static boolean eq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
```

</details>

---
## [38] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [39] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~7,189 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
- *[assistant] message: identical to step [37] — not reprinted*
- *[user] message: identical to step [5] — not reprinted*
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
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        runAndCheck("it''s a {0,lower} 'test'!", "DUMMY", registry, "it's a dummy test!");

        String prefix = clean(data.consumeAsciiString(20));
        String infix = clean(data.consumeAsciiString(20));
        String suffix = clean(data.consumeAsciiString(20));
        String quoted = clean(data.consumeAsciiString(12));
        String arg = cleanArg(data.consumeString(20));
        int escapedPairs = data.consumeInt(1, 4);
        boolean addLeadingText = data.consumeBoolean();
        boolean addTrailingQuoted = data.consumeBoolean();
        boolean useUpper = data.consumeBoolean();

        StringBuilder pattern = new StringBuilder();
        if (addLeadingText) {
            pattern.append(prefix);
            pattern.append(' ');
        }
        for (int i = 0; i < escapedPairs; i++) {
            pattern.append("''");
            if (i + 1 < escapedPairs) {
                pattern.append(clean(data.consumeAsciiString(6)));
            }
        }
        pattern.append(infix);
        pattern.append(" {0,");
        pattern.append(useUpper ? "upper" : "lower");
        pattern.append('}');
        if (addTrailingQuoted) {
            pattern.append(" '");
            pattern.append(quoted);
            pattern.append('\'');
        }
        pattern.append(suffix);

        String p = pattern.toString();
        String expected = expectedOutput(p, arg, useUpper);
        runAndCheck(p, arg, registry, expected);
    }

    private static void runAndCheck(String pattern, String arg, Map registry, String expected) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, Locale.US, registry);
        String before = emf.toPattern();
        String actual = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        /*
         * Oracle: formatting is a read-only operation for the pattern; toPattern() must not change.
         * A bogus fix that merely skips quote-handling/bookkeeping could avoid the crash but mutate or
         * mis-parse the stored pattern.
         */
        if (!eq(before, after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: format changed toPattern input=" + pattern + " before=" + before + " after=" + after);
        }

        /*
         * Oracle from construction: we build only valid patterns of the same form as the regression test:
         * ordinary text + doubled apostrophes + one registered format {0,lower}/{0,upper} + optional quoted
         * literal. For these inputs the formatted output is determined by the input itself.
         */
        if (!eq(expected, actual)) {
            throw new RuntimeException("[oracle:constructed-output] metamorphic violation: wrong formatted output input=" + pattern + " arg=" + arg + " expected=" + expected + " actual=" + actual);
        }
    }

    private static String expectedOutput(String pattern, String arg, boolean useUpper) {
        String transformed = useUpper ? arg.toUpperCase() : arg.toLowerCase();
        String p = pattern.replace("{0,upper}", "\u0000").replace("{0,lower}", "\u0000");
        p = p.replace("''", "'");
        p = stripSimpleQuotes(p);
        return p.replace("\u0000", transformed);
    }

    private static String stripSimpleQuotes(String s) {
        StringBuilder out = new StringBuilder(s.length());
        boolean inQuote = false;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\'') {
                inQuote = !inQuote;
            } else {
                out.append(ch);
            }
        }
        return out.toString();
    }

    private static String clean(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'') {
                out.append('x');
            } else if (Character.isISOControl(ch)) {
                out.append('y');
            } else {
                out.append(ch);
            }
        }
        if (out.length() == 0) {
            out.append('x');
        }
        return out.toString();
    }

    private static String cleanArg(String s) {
        if (s == null || s.length() == 0) {
            return "Dummy";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (Character.isISOControl(ch)) {
                out.append('a');
            } else {
                out.append(ch);
            }
        }
        if (out.length() == 0) {
            out.append("Dummy");
        }
        return out.toString();
    }

    private static boolean eq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/Lang_43_buggy/fuzz/attempt_018/FuzzHarness.java:11: error: cannot find symbol
        registry.put("lower", new LowerCaseFormatFactory());
                                  ^
  symbol:   class LowerCaseFormatFactory
  location: class FuzzHarness
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/Lang_43_buggy/fuzz/attempt_018/FuzzHarness.java:12: error: cannot find symbol
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
<details open><summary>▸ Output (~4,035 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.Locale;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runPattern("it''s a {0} 'test'!", "DUMMY");

        String a = clean(data.consumeAsciiString(20));
        String b = clean(data.consumeAsciiString(20));
        String c = clean(data.consumeAsciiString(20));
        String q = clean(data.consumeAsciiString(12));
        String arg = cleanArg(data.consumeString(20));
        int escapedQuoteRuns = data.consumeInt(1, 4);
        boolean leadingArg = data.consumeBoolean();
        boolean trailingQuoted = data.consumeBoolean();

        StringBuilder pattern = new StringBuilder();
        if (leadingArg) {
            pattern.append("{0} ");
        }
        pattern.append(a);
        for (int i = 0; i < escapedQuoteRuns; i++) {
            pattern.append("''");
            if (i + 1 < escapedQuoteRuns) {
                pattern.append(clean(data.consumeAsciiString(8)));
            }
        }
        pattern.append(b);
        pattern.append(" {0}");
        if (trailingQuoted) {
            pattern.append(" '");
            pattern.append(q);
            pattern.append('\'');
        }
        pattern.append(c);

        runPattern(pattern.toString(), arg);
    }

    private static void runPattern(String pattern, String arg) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, Locale.US);
        String before = emf.toPattern();
        String emfOut = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        /*
         * Oracle: format is read-only with respect to the stored pattern, so toPattern() must be stable
         * before and after formatting. A fix that only bypasses quote processing could corrupt bookkeeping.
         */
        if (!eq(before, after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: format changed toPattern input=" + pattern + " before=" + before + " after=" + after);
        }

        /*
         * Oracle: for patterns using only standard MessageFormat syntax, ExtendedMessageFormat must behave
         * like MessageFormat. We generate only valid-by-construction patterns with one argument and quotes.
         */
        MessageFormat mf = new MessageFormat(pattern, Locale.US);
        String mfOut = mf.format(new Object[] { arg });
        if (!eq(emfOut, mfOut)) {
            throw new RuntimeException("[oracle:messageformat-equivalence] metamorphic violation: input=" + pattern + " arg=" + arg + " lhs=" + emfOut + " rhs=" + mfOut);
        }
    }

    private static String clean(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'') {
                out.append('x');
            } else if (Character.isISOControl(ch)) {
                out.append('y');
            } else {
                out.append(ch);
            }
        }
        if (out.length() == 0) {
            out.append('x');
        }
        return out.toString();
    }

    private static String cleanArg(String s) {
        if (s == null || s.length() == 0) {
            return "Dummy";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (Character.isISOControl(ch)) {
                out.append('a');
            } else {
                out.append(ch);
            }
        }
        if (out.length() == 0) {
            out.append("Dummy");
        }
        return out.toString();
    }

    private static boolean eq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
```

</details>

---
## [40] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. Jazzer exited cleanly with no finding on the buggy code. Most often this means every input was handled normally, or the harnes

---
## [41] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (8 message(s), ~4,473 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
- *[assistant] message: identical to step [37] — not reprinted*
- *[user] message: identical to step [5] — not reprinted*
- *[assistant] message: identical to step [39] — not reprinted*
- *[user] message: identical to step [39] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.Locale;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runPattern("it''s a {0} 'test'!", "DUMMY");

        String a = clean(data.consumeAsciiString(20));
        String b = clean(data.consumeAsciiString(20));
        String c = clean(data.consumeAsciiString(20));
        String q = clean(data.consumeAsciiString(12));
        String arg = cleanArg(data.consumeString(20));
        int escapedQuoteRuns = data.consumeInt(1, 4);
        boolean leadingArg = data.consumeBoolean();
        boolean trailingQuoted = data.consumeBoolean();

        StringBuilder pattern = new StringBuilder();
        if (leadingArg) {
            pattern.append("{0} ");
        }
        pattern.append(a);
        for (int i = 0; i < escapedQuoteRuns; i++) {
            pattern.append("''");
            if (i + 1 < escapedQuoteRuns) {
                pattern.append(clean(data.consumeAsciiString(8)));
            }
        }
        pattern.append(b);
        pattern.append(" {0}");
        if (trailingQuoted) {
            pattern.append(" '");
            pattern.append(q);
            pattern.append('\'');
        }
        pattern.append(c);

        runPattern(pattern.toString(), arg);
    }

    private static void runPattern(String pattern, String arg) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, Locale.US);
        String before = emf.toPattern();
        String emfOut = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        /*
         * Oracle: format is read-only with respect to the stored pattern, so toPattern() must be stable
         * before and after formatting. A fix that only bypasses quote processing could corrupt bookkeeping.
         */
        if (!eq(before, after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: format changed toPattern input=" + pattern + " before=" + before + " after=" + after);
        }

        /*
         * Oracle: for patterns using only standard MessageFormat syntax, ExtendedMessageFormat must behave
         * like MessageFormat. We generate only valid-by-construction patterns with one argument and quotes.
         */
        MessageFormat mf = new MessageFormat(pattern, Locale.US);
        String mfOut = mf.format(new Object[] { arg });
        if (!eq(emfOut, mfOut)) {
            throw new RuntimeException("[oracle:messageformat-equivalence] metamorphic violation: input=" + pattern + " arg=" + arg + " lhs=" + emfOut + " rhs=" + mfOut);
        }
    }

    private static String clean(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'') {
                out.append('x');
            } else if (Character.isISOControl(ch)) {
                out.append('y');
            } else {
                out.append(ch);
            }
        }
        if (out.length() == 0) {
            out.append('x');
        }
        return out.toString();
    }

    private static String cleanArg(String s) {
        if (s == null || s.length() == 0) {
            return "Dummy";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (Character.isISOControl(ch)) {
                out.append('a');
            } else {
                out.append(ch);
            }
        }
        if (out.length() == 0) {
            out.append("Dummy");
        }
        return out.toString();
    }

    private static boolean eq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
```
- *[user] message: identical to step [7] — not reprinted*

</details>
<details open><summary>▸ Output (~4,305 chars)</summary>

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

        String anchorPattern = "it''s a {0} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, Locale.US, registry);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a DUMMY test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: input=" + anchorPattern + " expected=it's a DUMMY test! actual=" + anchorOut);
        }

        String prefix = clean(data.consumeAsciiString(16));
        String middle = clean(data.consumeAsciiString(16));
        String suffix = clean(data.consumeAsciiString(16));
        String quoted = clean(data.consumeAsciiString(12));
        String arg = cleanArg(data.consumeString(16));
        int escapedQuoteRuns = data.consumeInt(1, 4);
        boolean leadingArg = data.consumeBoolean();
        boolean trailingQuoted = data.consumeBoolean();

        StringBuilder pattern = new StringBuilder();
        if (leadingArg) {
            pattern.append("{0} ");
        }
        pattern.append(prefix);
        for (int i = 0; i < escapedQuoteRuns; i++) {
            pattern.append("''");
            if (i + 1 < escapedQuoteRuns) {
                pattern.append(clean(data.consumeAsciiString(6)));
            }
        }
        pattern.append(middle);
        pattern.append(" {0}");
        if (trailingQuoted) {
            pattern.append(" '");
            pattern.append(quoted);
            pattern.append('\'');
        }
        pattern.append(suffix);

        String p = pattern.toString();
        ExtendedMessageFormat emf = new ExtendedMessageFormat(p, Locale.US, registry);
        String before = emf.toPattern();
        String emfOut = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        /*
         * Oracle: formatting is read-only with respect to the parsed pattern, so toPattern() must stay stable.
         */
        if (!eq(before, after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: input=" + p + " before=" + before + " after=" + after);
        }

        /*
         * Oracle: with an empty registry and only standard {0} placeholders, ExtendedMessageFormat must
         * behave like MessageFormat on valid-by-construction patterns.
         */
        MessageFormat mf = new MessageFormat(p, Locale.US);
        String mfOut = mf.format(new Object[] { arg });
        if (!eq(emfOut, mfOut)) {
            throw new RuntimeException("[oracle:messageformat-equivalence] metamorphic violation: input=" + p + " arg=" + arg + " lhs=" + emfOut + " rhs=" + mfOut);
        }
    }

    private static String clean(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'') {
                out.append('x');
            } else if (Character.isISOControl(ch)) {
                out.append('y');
            } else {
                out.append(ch);
            }
        }
        if (out.length() == 0) {
            out.append('x');
        }
        return out.toString();
    }

    private static String cleanArg(String s) {
        if (s == null || s.length() == 0) {
            return "Dummy";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (Character.isISOControl(ch)) {
                out.append('a');
            } else {
                out.append(ch);
            }
        }
        if (out.length() == 0) {
            out.append("Dummy");
        }
        return out.toString();
    }

    private static boolean eq(String a, String b) {
        return a == null ? b == null : a.equals(b);
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
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,730 chars)</summary>

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
        Map registry = makeRegistry();

        runCase(ANCHOR_PATTERN, "DUMMY", ANCHOR_EXPECTED, registry);

        int cases = data.consumeInt(1, 3);
        for (int i = 0; i < cases; i++) {
            String prefix = safeText(data.consumeAsciiString(20), "a");
            String infix = safeText(data.consumeAsciiString(20), "b");
            String literal = safeText(data.consumeAsciiString(20), "lit");
            String arg = safeText(data.consumeAsciiString(20), "DuMmY");
            String formatName = data.consumeBoolean() ? "lower" : "upper";

            int variant = data.consumeInt(0, 2);
            String pattern;
            String expectedArg = "lower".equals(formatName) ? arg.toLowerCase(Locale.ROOT) : arg.toUpperCase(Locale.ROOT);
            String expected;

            if (variant == 0) {
                pattern = prefix + "''" + infix + " {0," + formatName + "} '" + literal + "'";
                expected = prefix + "'" + infix + " " + expectedArg + " " + literal;
            } else if (variant == 1) {
                String suffix = safeText(data.consumeAsciiString(20), "z");
                pattern = prefix + " '' " + infix + " {0," + formatName + "} '" + literal + "' " + suffix;
                expected = prefix + " ' " + infix + " " + expectedArg + " " + literal + " " + suffix;
            } else {
                String tail = safeText(data.consumeAsciiString(20), "tail");
                pattern = prefix + "{0," + formatName + "}''" + infix + " '" + literal + "' " + tail;
                expected = prefix + expectedArg + "'" + infix + " " + literal + " " + tail;
            }

            runCase(pattern, arg, expected, registry);
        }
    }

    private static void runCase(String pattern, String arg, String expected, Map registry) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

            String beforePattern = emf.toPattern();

            String formatted = emf.format(new Object[] { arg });

            String afterPattern = emf.toPattern();

            if (!beforePattern.equals(afterPattern)) {
                throw new RuntimeException("[oracle:pattern-stable] metamorphic violation: formatting changed toPattern input="
                        + pattern + " before=" + beforePattern + " after=" + afterPattern);
            }

            if (!pattern.equals(beforePattern)) {
                throw new RuntimeException("[oracle:roundtrip-pattern] metamorphic violation: constructor/toPattern did not preserve valid pattern input="
                        + pattern + " toPattern=" + beforePattern);
            }

            if (!expected.equals(formatted)) {
                throw new RuntimeException("[oracle:format-output] metamorphic violation: valid constructed pattern formatted to unexpected output input="
                        + pattern + " lhs=" + formatted + " rhs=" + expected);
            }

            try {
                ExtendedMessageFormat emf2 = new ExtendedMessageFormat(beforePattern, registry);
                String roundTrip = emf2.toPattern();
                if (!beforePattern.equals(roundTrip)) {
                    throw new RuntimeException("[oracle:idempotent-topattern] metamorphic violation: reparsing toPattern changed it input="
                            + pattern + " lhs=" + beforePattern + " rhs=" + roundTrip);
                }
            } catch (Throwable t) {
                handleThrowable(t);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }
    }

    private static void handleThrowable(Throwable t) {
        if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
            throw (RuntimeException) t;
        }
        if (isCleanRejection(t)) {
            return;
        }
        if (isRootCause(t)) {
            rethrowUnchecked(t);
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
                || t instanceof NumberFormatException
                || t instanceof UnsupportedOperationException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (StackTraceElement e : stack) {
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(e.getClassName())
                    && "appendQuotedString".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static void rethrowUnchecked(Throwable t) {
        if (t instanceof Error) {
            throw (Error) t;
        }
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        throw new RuntimeException(t);
    }

    private static String safeText(String s, String fallback) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c) || c == ' ' || c == '_' || c == '-' || c == '.') {
                sb.append(c);
            }
        }
        String out = sb.toString().trim();
        return out.length() == 0 ? fallback : out;
    }

    private static Map makeRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
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

    private static final class LowerCaseFormat extends Format {
        private static final long serialVersionUID = 1L;

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toLowerCase(Locale.ROOT));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source;
        }
    }

    private static final class UpperCaseFormat extends Format {
        private static final long serialVersionUID = 1L;

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toUpperCase(Locale.ROOT));
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
## [44] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [45] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~7,927 chars, 1 new)</summary>

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
        Map registry = makeRegistry();

        runCase(ANCHOR_PATTERN, "DUMMY", ANCHOR_EXPECTED, registry);

        int cases = data.consumeInt(1, 3);
        for (int i = 0; i < cases; i++) {
            String prefix = safeText(data.consumeAsciiString(20), "a");
            String infix = safeText(data.consumeAsciiString(20), "b");
            String literal = safeText(data.consumeAsciiString(20), "lit");
            String arg = safeText(data.consumeAsciiString(20), "DuMmY");
            String formatName = data.consumeBoolean() ? "lower" : "upper";

            int variant = data.consumeInt(0, 2);
            String pattern;
            String expectedArg = "lower".equals(formatName) ? arg.toLowerCase(Locale.ROOT) : arg.toUpperCase(Locale.ROOT);
            String expected;

            if (variant == 0) {
                pattern = prefix + "''" + infix + " {0," + formatName + "} '" + literal + "'";
                expected = prefix + "'" + infix + " " + expectedArg + " " + literal;
            } else if (variant == 1) {
                String suffix = safeText(data.consumeAsciiString(20), "z");
                pattern = prefix + " '' " + infix + " {0," + formatName + "} '" + literal + "' " + suffix;
                expected = prefix + " ' " + infix + " " + expectedArg + " " + literal + " " + suffix;
            } else {
                String tail = safeText(data.consumeAsciiString(20), "tail");
                pattern = prefix + "{0," + formatName + "}''" + infix + " '" + literal + "' " + tail;
                expected = prefix + expectedArg + "'" + infix + " " + literal + " " + tail;
            }

            runCase(pattern, arg, expected, registry);
        }
    }

    private static void runCase(String pattern, String arg, String expected, Map registry) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

            String beforePattern = emf.toPattern();

            String formatted = emf.format(new Object[] { arg });

            String afterPattern = emf.toPattern();

            if (!beforePattern.equals(afterPattern)) {
                throw new RuntimeException("[oracle:pattern-stable] metamorphic violation: formatting changed toPattern input="
                        + pattern + " before=" + beforePattern + " after=" + afterPattern);
            }

            if (!pattern.equals(beforePattern)) {
                throw new RuntimeException("[oracle:roundtrip-pattern] metamorphic violation: constructor/toPattern did not preserve valid pattern input="
                        + pattern + " toPattern=" + beforePattern);
            }

            if (!expected.equals(formatted)) {
                throw new RuntimeException("[oracle:format-output] metamorphic violation: valid constructed pattern formatted to unexpected output input="
                        + pattern + " lhs=" + formatted + " rhs=" + expected);
            }

            try {
                ExtendedMessageFormat emf2 = new ExtendedMessageFormat(beforePattern, registry);
                String roundTrip = emf2.toPattern();
                if (!beforePattern.equals(roundTrip)) {
                    throw new RuntimeException("[oracle:idempotent-topattern] metamorphic violation: reparsing toPattern changed it input="
                            + pattern + " lhs=" + beforePattern + " rhs=" + roundTrip);
                }
            } catch (Throwable t) {
                handleThrowable(t);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }
    }

    private static void handleThrowable(Throwable t) {
        if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
            throw (RuntimeException) t;
        }
        if (isCleanRejection(t)) {
            return;
        }
        if (isRootCause(t)) {
            rethrowUnchecked(t);
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
                || t instanceof NumberFormatException
                || t instanceof UnsupportedOperationException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (StackTraceElement e : stack) {
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(e.getClassName())
                    && "appendQuotedString".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static void rethrowUnchecked(Throwable t) {
        if (t instanceof Error) {
            throw (Error) t;
        }
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        throw new RuntimeException(t);
    }

    private static String safeText(String s, String fallback) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c) || c == ' ' || c == '_' || c == '-' || c == '.') {
                sb.append(c);
            }
        }
        String out = sb.toString().trim();
        return out.length() == 0 ? fallback : out;
    }

    private static Map makeRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
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

    private static final class LowerCaseFormat extends Format {
        private static final long serialVersionUID = 1L;

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toLowerCase(Locale.ROOT));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source;
        }
    }

    private static final class UpperCaseFormat extends Format {
        private static final long serialVersionUID = 1L;

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toUpperCase(Locale.ROOT));
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
<details open><summary>▸ Output (~5,515 chars)</summary>

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
        // ANCHOR: exact failing test input. On the buggy version this must reach
        // ExtendedMessageFormat.appendQuotedString and trigger the verified OOM.
        checkPattern("it''s a {0,lower} 'test'!", "DUMMY", "it's a dummy test!");

        // EXPLORE: same root-cause property as the patch:
        // when escapingOn is true and the current position starts with a QUOTE,
        // the buggy code returns without advancing ParsePosition. Patterns with
        // doubled quotes ("''") in literal text drive that branch.
        String a = clean(data.consumeAsciiString(12));
        String b = clean(data.consumeAsciiString(12));
        String c = clean(data.consumeAsciiString(12));
        String d = clean(data.consumeAsciiString(12));
        String arg = cleanNonEmpty(data.consumeAsciiString(12));
        boolean lower = data.consumeBoolean();

        String formatName = lower ? "lower" : "upper";
        String transformed = lower ? arg.toLowerCase(Locale.ROOT) : arg.toUpperCase(Locale.ROOT);

        switch (data.consumeInt(0, 3)) {
            case 0:
                checkPattern(a + "''" + b + " {0," + formatName + "} '" + c + "'",
                        arg,
                        a + "'" + b + " " + transformed + " " + c);
                break;
            case 1:
                checkPattern("'" + a + "' " + b + "''" + c + " {0," + formatName + "}",
                        arg,
                        a + " " + b + "'" + c + " " + transformed);
                break;
            case 2:
                checkPattern(a + " {0," + formatName + "} " + b + "''" + c + " '" + d + "'",
                        arg,
                        a + " " + transformed + " " + b + "'" + c + " " + d);
                break;
            default:
                checkPattern(a + "''" + b + " {0," + formatName + "} '" + c + "' " + d,
                        arg,
                        a + "'" + b + " " + transformed + " " + c + " " + d);
                break;
        }
    }

    private static void checkPattern(String pattern, String arg, String expected) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);

        // Oracle 1: for valid-by-construction patterns, toPattern must preserve the
        // pattern text; a "fix" that skips the escaped-quote branch or drops state
        // can silently alter the parsed pattern.
        String reparsed = emf.toPattern();
        if (!pattern.equals(reparsed)) {
            throw new RuntimeException("[oracle:pattern-roundtrip] metamorphic violation: constructor/toPattern changed valid pattern input="
                    + pattern + " lhs=" + pattern + " rhs=" + reparsed);
        }

        // Oracle 2: the valid constructed pattern's formatted output is known from
        // the input itself: literals are preserved, '' becomes a single quote, the
        // custom factory transforms argument case, and quoted text loses delimiters.
        String out = emf.format(new Object[] { arg });
        if (!expected.equals(out)) {
            throw new RuntimeException("[oracle:format-output] metamorphic violation: valid pattern formatted unexpectedly input="
                    + pattern + " lhs=" + out + " rhs=" + expected);
        }
    }

    private static String clean(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (Character.isLetterOrDigit(ch) || ch == ' ' || ch == '_' || ch == '-') {
                sb.append(ch);
            }
        }
        return sb.toString().trim();
    }

    private static String cleanNonEmpty(String s) {
        String out = clean(s);
        return out.length() == 0 ? "Dummy" : out;
    }

    private static Map makeRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new SimpleCaseFormatFactory(true));
        registry.put("upper", new SimpleCaseFormatFactory(false));
        return registry;
    }

    private static final class SimpleCaseFormatFactory implements FormatFactory {
        private final boolean lower;

        private SimpleCaseFormatFactory(boolean lower) {
            this.lower = lower;
        }

        public Format getFormat(String name, String arguments, Locale locale) {
            return new SimpleCaseFormat(lower);
        }
    }

    private static final class SimpleCaseFormat extends Format {
        private static final long serialVersionUID = 1L;
        private final boolean lower;

        private SimpleCaseFormat(boolean lower) {
            this.lower = lower;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(lower ? s.toLowerCase(Locale.ROOT) : s.toUpperCase(Locale.ROOT));
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
## [46] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [47] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~5,832 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
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
    private static final Map REGISTRY = makeRegistry();

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact failing test input. On the buggy version this must reach
        // ExtendedMessageFormat.appendQuotedString and trigger the verified OOM.
        checkPattern("it''s a {0,lower} 'test'!", "DUMMY", "it's a dummy test!");

        // EXPLORE: same root-cause property as the patch:
        // when escapingOn is true and the current position starts with a QUOTE,
        // the buggy code returns without advancing ParsePosition. Patterns with
        // doubled quotes ("''") in literal text drive that branch.
        String a = clean(data.consumeAsciiString(12));
        String b = clean(data.consumeAsciiString(12));
        String c = clean(data.consumeAsciiString(12));
        String d = clean(data.consumeAsciiString(12));
        String arg = cleanNonEmpty(data.consumeAsciiString(12));
        boolean lower = data.consumeBoolean();

        String formatName = lower ? "lower" : "upper";
        String transformed = lower ? arg.toLowerCase(Locale.ROOT) : arg.toUpperCase(Locale.ROOT);

        switch (data.consumeInt(0, 3)) {
            case 0:
                checkPattern(a + "''" + b + " {0," + formatName + "} '" + c + "'",
                        arg,
                        a + "'" + b + " " + transformed + " " + c);
                break;
            case 1:
                checkPattern("'" + a + "' " + b + "''" + c + " {0," + formatName + "}",
                        arg,
                        a + " " + b + "'" + c + " " + transformed);
                break;
            case 2:
                checkPattern(a + " {0," + formatName + "} " + b + "''" + c + " '" + d + "'",
                        arg,
                        a + " " + transformed + " " + b + "'" + c + " " + d);
                break;
            default:
                checkPattern(a + "''" + b + " {0," + formatName + "} '" + c + "' " + d,
                        arg,
                        a + "'" + b + " " + transformed + " " + c + " " + d);
                break;
        }
    }

    private static void checkPattern(String pattern, String arg, String expected) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);

        // Oracle 1: for valid-by-construction patterns, toPattern must preserve the
        // pattern text; a "fix" that skips the escaped-quote branch or drops state
        // can silently alter the parsed pattern.
        String reparsed = emf.toPattern();
        if (!pattern.equals(reparsed)) {
            throw new RuntimeException("[oracle:pattern-roundtrip] metamorphic violation: constructor/toPattern changed valid pattern input="
                    + pattern + " lhs=" + pattern + " rhs=" + reparsed);
        }

        // Oracle 2: the valid constructed pattern's formatted output is known from
        // the input itself: literals are preserved, '' becomes a single quote, the
        // custom factory transforms argument case, and quoted text loses delimiters.
        String out = emf.format(new Object[] { arg });
        if (!expected.equals(out)) {
            throw new RuntimeException("[oracle:format-output] metamorphic violation: valid pattern formatted unexpectedly input="
                    + pattern + " lhs=" + out + " rhs=" + expected);
        }
    }

    private static String clean(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (Character.isLetterOrDigit(ch) || ch == ' ' || ch == '_' || ch == '-') {
                sb.append(ch);
            }
        }
        return sb.toString().trim();
    }

    private static String cleanNonEmpty(String s) {
        String out = clean(s);
        return out.length() == 0 ? "Dummy" : out;
    }

    private static Map makeRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new SimpleCaseFormatFactory(true));
        registry.put("upper", new SimpleCaseFormatFactory(false));
        return registry;
    }

    private static final class SimpleCaseFormatFactory implements FormatFactory {
        private final boolean lower;

        private SimpleCaseFormatFactory(boolean lower) {
            this.lower = lower;
        }

        public Format getFormat(String name, String arguments, Locale locale) {
            return new SimpleCaseFormat(lower);
        }
    }

    private static final class SimpleCaseFormat extends Format {
        private static final long serialVersionUID = 1L;
        private final boolean lower;

        private SimpleCaseFormat(boolean lower) {
            this.lower = lower;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(lower ? s.toLowerCase(Locale.ROOT) : s.toUpperCase(Locale.ROOT));
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
<details open><summary>▸ Output (~6,304 chars)</summary>

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
        // ANCHOR: exact failing test setup and input.
        // On the buggy version, constructing ExtendedMessageFormat with this pattern
        // reaches appendQuotedString with escapingOn=true at a doubled quote and
        // loops without advancing ParsePosition, eventually throwing OOME.
        ExtendedMessageFormat anchor = new ExtendedMessageFormat("it''s a {0,lower} 'test'!", REGISTRY);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: valid anchor pattern formatted unexpectedly lhs="
                    + anchorOut + " rhs=it's a dummy test!");
        }
        String anchorPattern = anchor.toPattern();
        if (!"it''s a {0,lower} 'test'!".equals(anchorPattern)) {
            throw new RuntimeException("[oracle:anchor-topattern] metamorphic violation: toPattern changed valid anchor pattern lhs="
                    + anchorPattern + " rhs=it''s a {0,lower} 'test'!");
        }

        // EXPLORE: generate more valid patterns with the same triggering property:
        // literal text containing doubled quotes before/around a format element.
        String a = atom(data.consumeAsciiString(8));
        String b = atom(data.consumeAsciiString(8));
        String c = atom(data.consumeAsciiString(8));
        String arg = atomNonEmpty(data.consumeAsciiString(8));
        boolean useLower = data.consumeBoolean();
        String formatName = useLower ? "lower" : "upper";
        String expectedArg = useLower ? arg.toLowerCase(Locale.ROOT) : arg.toUpperCase(Locale.ROOT);

        String pattern;
        String expected;
        switch (data.consumeInt(0, 3)) {
            case 0:
                pattern = a + "''" + b + " {0," + formatName + "} '" + c + "'!";
                expected = a + "'" + b + " " + expectedArg + " " + c + "!";
                break;
            case 1:
                pattern = a + " '' " + b + " {0," + formatName + "} '" + c + "'";
                expected = a + " ' " + b + " " + expectedArg + " " + c;
                break;
            case 2:
                pattern = a + "{0," + formatName + "}''" + b + " '" + c + "'";
                expected = a + expectedArg + "'" + b + " " + c;
                break;
            default:
                pattern = a + "''" + b + "{0," + formatName + "}'" + c + "'";
                expected = a + "'" + b + expectedArg + c;
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);
        String out = emf.format(new Object[] { arg });

        // Oracle: for these valid-by-construction patterns, formatting must preserve
        // literals, decode '' to a single quote, apply the registered format, and
        // include quoted text without the quote delimiters. A "fix" that merely
        // skips the branch or loses parser state would break this observable result.
        if (!expected.equals(out)) {
            throw new RuntimeException("[oracle:format-output] metamorphic violation: valid pattern formatted unexpectedly input="
                    + pattern + " lhs=" + out + " rhs=" + expected);
        }

        String roundTrip = emf.toPattern();
        if (!pattern.equals(roundTrip)) {
            throw new RuntimeException("[oracle:pattern-roundtrip] metamorphic violation: constructor/toPattern changed valid pattern input="
                    + pattern + " lhs=" + roundTrip + " rhs=" + pattern);
        }
    }

    private static String atom(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
                sb.append(ch);
            }
        }
        if (sb.length() == 0) {
            return "x";
        }
        return sb.toString();
    }

    private static String atomNonEmpty(String s) {
        String out = atom(s);
        return out.length() == 0 ? "Dummy" : out;
    }

    private static Map makeRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
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

    private static final class LowerCaseFormat extends Format {
        private static final long serialVersionUID = 1L;

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toLowerCase(Locale.ROOT));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source;
        }
    }

    private static final class UpperCaseFormat extends Format {
        private static final long serialVersionUID = 1L;

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toUpperCase(Locale.ROOT));
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
## [48] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [49] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (8 message(s), ~6,741 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
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
    private static final Map REGISTRY = makeRegistry();

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact failing test setup and input.
        // On the buggy version, constructing ExtendedMessageFormat with this pattern
        // reaches appendQuotedString with escapingOn=true at a doubled quote and
        // loops without advancing ParsePosition, eventually throwing OOME.
        ExtendedMessageFormat anchor = new ExtendedMessageFormat("it''s a {0,lower} 'test'!", REGISTRY);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: valid anchor pattern formatted unexpectedly lhs="
                    + anchorOut + " rhs=it's a dummy test!");
        }
        String anchorPattern = anchor.toPattern();
        if (!"it''s a {0,lower} 'test'!".equals(anchorPattern)) {
            throw new RuntimeException("[oracle:anchor-topattern] metamorphic violation: toPattern changed valid anchor pattern lhs="
                    + anchorPattern + " rhs=it''s a {0,lower} 'test'!");
        }

        // EXPLORE: generate more valid patterns with the same triggering property:
        // literal text containing doubled quotes before/around a format element.
        String a = atom(data.consumeAsciiString(8));
        String b = atom(data.consumeAsciiString(8));
        String c = atom(data.consumeAsciiString(8));
        String arg = atomNonEmpty(data.consumeAsciiString(8));
        boolean useLower = data.consumeBoolean();
        String formatName = useLower ? "lower" : "upper";
        String expectedArg = useLower ? arg.toLowerCase(Locale.ROOT) : arg.toUpperCase(Locale.ROOT);

        String pattern;
        String expected;
        switch (data.consumeInt(0, 3)) {
            case 0:
                pattern = a + "''" + b + " {0," + formatName + "} '" + c + "'!";
                expected = a + "'" + b + " " + expectedArg + " " + c + "!";
                break;
            case 1:
                pattern = a + " '' " + b + " {0," + formatName + "} '" + c + "'";
                expected = a + " ' " + b + " " + expectedArg + " " + c;
                break;
            case 2:
                pattern = a + "{0," + formatName + "}''" + b + " '" + c + "'";
                expected = a + expectedArg + "'" + b + " " + c;
                break;
            default:
                pattern = a + "''" + b + "{0," + formatName + "}'" + c + "'";
                expected = a + "'" + b + expectedArg + c;
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);
        String out = emf.format(new Object[] { arg });

        // Oracle: for these valid-by-construction patterns, formatting must preserve
        // literals, decode '' to a single quote, apply the registered format, and
        // include quoted text without the quote delimiters. A "fix" that merely
        // skips the branch or loses parser state would break this observable result.
        if (!expected.equals(out)) {
            throw new RuntimeException("[oracle:format-output] metamorphic violation: valid pattern formatted unexpectedly input="
                    + pattern + " lhs=" + out + " rhs=" + expected);
        }

        String roundTrip = emf.toPattern();
        if (!pattern.equals(roundTrip)) {
            throw new RuntimeException("[oracle:pattern-roundtrip] metamorphic violation: constructor/toPattern changed valid pattern input="
                    + pattern + " lhs=" + roundTrip + " rhs=" + pattern);
        }
    }

    private static String atom(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
                sb.append(ch);
            }
        }
        if (sb.length() == 0) {
            return "x";
        }
        return sb.toString();
    }

    private static String atomNonEmpty(String s) {
        String out = atom(s);
        return out.length() == 0 ? "Dummy" : out;
    }

    private static Map makeRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
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

    private static final class LowerCaseFormat extends Format {
        private static final long serialVersionUID = 1L;

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toLowerCase(Locale.ROOT));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source;
        }
    }

    private static final class UpperCaseFormat extends Format {
        private static final long serialVersionUID = 1L;

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toUpperCase(Locale.ROOT));
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
<details open><summary>▸ Output (~5,256 chars)</summary>

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
    private static final Map REGISTRY = buildRegistry();

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exercise("it''s a {0,lower} 'test'!", "DUMMY", "it's a dummy test!");

        String left = letters(data.consumeAsciiString(10));
        String mid = letters(data.consumeAsciiString(10));
        String quoted = letters(data.consumeAsciiString(10));
        String right = letters(data.consumeAsciiString(10));
        String arg = nonEmptyLetters(data.consumeAsciiString(10));
        boolean lower = data.consumeBoolean();

        String fmt = lower ? "lower" : "upper";
        String xform = lower ? arg.toLowerCase(Locale.ROOT) : arg.toUpperCase(Locale.ROOT);

        String pattern;
        String expected;

        switch (data.consumeInt(0, 4)) {
            case 0:
                pattern = left + "''" + mid + " {0," + fmt + "} '" + quoted + "'!";
                expected = left + "'" + mid + " " + xform + " " + quoted + "!";
                break;
            case 1:
                pattern = left + "''" + mid + "{0," + fmt + "} '" + quoted + "' " + right;
                expected = left + "'" + mid + xform + " " + quoted + " " + right;
                break;
            case 2:
                pattern = left + " {0," + fmt + "} " + mid + "''" + right + " '" + quoted + "'";
                expected = left + " " + xform + " " + mid + "'" + right + " " + quoted;
                break;
            case 3:
                pattern = left + "''" + mid + " {0," + fmt + "}";
                expected = left + "'" + mid + " " + xform;
                break;
            default:
                pattern = "{0," + fmt + "} " + left + "''" + mid + " '" + quoted + "'";
                expected = xform + " " + left + "'" + mid + " " + quoted;
                break;
        }

        exercise(pattern, arg, expected);
    }

    private static void exercise(String pattern, String arg, String expected) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);

        String out = emf.format(new Object[] { arg });
        if (!expected.equals(out)) {
            throw new RuntimeException("[oracle:format-output] metamorphic violation: valid pattern formatted unexpectedly input="
                    + pattern + " lhs=" + out + " rhs=" + expected);
        }

        String roundTrip = emf.toPattern();
        if (!pattern.equals(roundTrip)) {
            throw new RuntimeException("[oracle:pattern-roundtrip] metamorphic violation: toPattern changed valid pattern input="
                    + pattern + " lhs=" + roundTrip + " rhs=" + pattern);
        }
    }

    private static String letters(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                sb.append(c);
            }
        }
        if (sb.length() == 0) {
            return "a";
        }
        return sb.toString();
    }

    private static String nonEmptyLetters(String s) {
        String r = letters(s);
        return r.length() == 0 ? "Dummy" : r;
    }

    private static Map buildRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
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

    private static final class LowerCaseFormat extends Format {
        private static final long serialVersionUID = 1L;

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toLowerCase(Locale.ROOT));
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source;
        }
    }

    private static final class UpperCaseFormat extends Format {
        private static final long serialVersionUID = 1L;

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (obj != null) {
                toAppendTo.append(obj.toString().toUpperCase(Locale.ROOT));
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
<details open><summary>▸ Output (~5,659 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact pattern from the failing test, driven through the real public API.
        // The bug is reached during ExtendedMessageFormat construction/applyPattern.
        try {
            Map registry = new HashMap();
            new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throw (OutOfMemoryError) t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }

        // EXPLORE: root-cause property is a pattern containing an escaped quote ('')
        // that is parsed by ExtendedMessageFormat. Build many valid-by-construction
        // MessageFormat-compatible patterns so a correct implementation is obliged to handle them.
        String pre = sanitize(data.consumeAsciiString(16));
        String mid = sanitize(data.consumeAsciiString(16));
        String post = sanitize(data.consumeAsciiString(16));
        String literal = sanitize(data.consumeAsciiString(16));
        String arg = sanitize(data.consumeString(32));
        if (arg.length() == 0) {
            arg = "X";
        }
        if (literal.length() == 0) {
            literal = "lit";
        }

        int variant = data.consumeInt(0, 4);
        String pattern;
        switch (variant) {
            case 0:
                pattern = pre + "''" + mid + " {0} '" + literal + "'" + post;
                break;
            case 1:
                pattern = "'" + literal + "' " + pre + "''" + mid + " {0}" + post;
                break;
            case 2:
                pattern = pre + " {0} " + mid + "''" + post + " '" + literal + "'";
                break;
            case 3:
                pattern = pre + "''" + mid + " '{0}' " + post + " {0}";
                break;
            default:
                pattern = pre + " {0} " + mid + "''" + post;
                break;
        }

        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern);
            String before = emf.toPattern();
            String actual = emf.format(new Object[] { arg });
            String after = emf.toPattern();

            // Oracle: with no custom registry, ExtendedMessageFormat must remain compatible with
            // java.text.MessageFormat for ordinary MessageFormat patterns. A throw-deleting patch in
            // appendQuotedString can silently change quote parsing, yielding different output/pattern.
            String expectedPattern;
            String expected;
            try {
                MessageFormat mf = new MessageFormat(pattern);
                expectedPattern = mf.toPattern();
                expected = mf.format(new Object[] { arg });
            } catch (Throwable oracleThrow) {
                return;
            }

            if (!safeEquals(actual, expected)) {
                throw new RuntimeException("[oracle:msgfmt-eq] metamorphic violation: ExtendedMessageFormat output must match MessageFormat on standard patterns input="
                        + pattern + " lhs=" + actual + " rhs=" + expected);
            }
            if (!safeEquals(before, after)) {
                throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: toPattern changed across format input="
                        + pattern + " lhs=" + before + " rhs=" + after);
            }
            if (!safeEquals(after, expectedPattern)) {
                throw new RuntimeException("[oracle:topattern-eq] metamorphic violation: ExtendedMessageFormat toPattern must match MessageFormat on standard patterns input="
                        + pattern + " lhs=" + after + " rhs=" + expectedPattern);
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throw (OutOfMemoryError) t;
            }
            if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
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
        return t instanceof IllegalArgumentException
                || t instanceof NumberFormatException;
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuffer sb = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\'' && c != '{' && c != '}' && c >= 32) {
                sb.append(c);
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
## [52] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [53] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~5,856 chars, 1 new)</summary>

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
        // ANCHOR: exact pattern from the failing test, driven through the real public API.
        // The bug is reached during ExtendedMessageFormat construction/applyPattern.
        try {
            Map registry = new HashMap();
            new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throw (OutOfMemoryError) t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }

        // EXPLORE: root-cause property is a pattern containing an escaped quote ('')
        // that is parsed by ExtendedMessageFormat. Build many valid-by-construction
        // MessageFormat-compatible patterns so a correct implementation is obliged to handle them.
        String pre = sanitize(data.consumeAsciiString(16));
        String mid = sanitize(data.consumeAsciiString(16));
        String post = sanitize(data.consumeAsciiString(16));
        String literal = sanitize(data.consumeAsciiString(16));
        String arg = sanitize(data.consumeString(32));
        if (arg.length() == 0) {
            arg = "X";
        }
        if (literal.length() == 0) {
            literal = "lit";
        }

        int variant = data.consumeInt(0, 4);
        String pattern;
        switch (variant) {
            case 0:
                pattern = pre + "''" + mid + " {0} '" + literal + "'" + post;
                break;
            case 1:
                pattern = "'" + literal + "' " + pre + "''" + mid + " {0}" + post;
                break;
            case 2:
                pattern = pre + " {0} " + mid + "''" + post + " '" + literal + "'";
                break;
            case 3:
                pattern = pre + "''" + mid + " '{0}' " + post + " {0}";
                break;
            default:
                pattern = pre + " {0} " + mid + "''" + post;
                break;
        }

        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern);
            String before = emf.toPattern();
            String actual = emf.format(new Object[] { arg });
            String after = emf.toPattern();

            // Oracle: with no custom registry, ExtendedMessageFormat must remain compatible with
            // java.text.MessageFormat for ordinary MessageFormat patterns. A throw-deleting patch in
            // appendQuotedString can silently change quote parsing, yielding different output/pattern.
            String expectedPattern;
            String expected;
            try {
                MessageFormat mf = new MessageFormat(pattern);
                expectedPattern = mf.toPattern();
                expected = mf.format(new Object[] { arg });
            } catch (Throwable oracleThrow) {
                return;
            }

            if (!safeEquals(actual, expected)) {
                throw new RuntimeException("[oracle:msgfmt-eq] metamorphic violation: ExtendedMessageFormat output must match MessageFormat on standard patterns input="
                        + pattern + " lhs=" + actual + " rhs=" + expected);
            }
            if (!safeEquals(before, after)) {
                throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: toPattern changed across format input="
                        + pattern + " lhs=" + before + " rhs=" + after);
            }
            if (!safeEquals(after, expectedPattern)) {
                throw new RuntimeException("[oracle:topattern-eq] metamorphic violation: ExtendedMessageFormat toPattern must match MessageFormat on standard patterns input="
                        + pattern + " lhs=" + after + " rhs=" + expectedPattern);
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throw (OutOfMemoryError) t;
            }
            if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
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
        return t instanceof IllegalArgumentException
                || t instanceof NumberFormatException;
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuffer sb = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\'' && c != '{' && c != '}' && c >= 32) {
                sb.append(c);
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
<details open><summary>▸ Output (~4,266 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String anchorPattern = "it''s a {0} 'test'!";
        String anchorArg = "DUMMY";
        ExtendedMessageFormat anchorEmf = new ExtendedMessageFormat(anchorPattern);
        String anchorActual = anchorEmf.format(new Object[] { anchorArg });
        String anchorExpected = new MessageFormat(anchorPattern).format(new Object[] { anchorArg });
        if (!anchorExpected.equals(anchorActual)) {
            throw new RuntimeException("[oracle:anchor-msgfmt] metamorphic violation: ExtendedMessageFormat must match MessageFormat on valid standard pattern input="
                    + anchorPattern + " lhs=" + anchorActual + " rhs=" + anchorExpected);
        }
        String anchorBefore = anchorEmf.toPattern();
        String anchorAfter = anchorEmf.toPattern();
        if (!anchorBefore.equals(anchorAfter)) {
            throw new RuntimeException("[oracle:anchor-topattern] metamorphic violation: toPattern must be stable across reads input="
                    + anchorPattern + " lhs=" + anchorBefore + " rhs=" + anchorAfter);
        }

        String a = literal(data.consumeAsciiString(12));
        String b = literal(data.consumeAsciiString(12));
        String c = literal(data.consumeAsciiString(12));
        String d = literal(data.consumeAsciiString(12));
        String arg = literal(data.consumeString(20));
        if (arg.length() == 0) {
            arg = "X";
        }
        if (a.length() == 0) {
            a = "a";
        }
        if (b.length() == 0) {
            b = "b";
        }
        if (c.length() == 0) {
            c = "c";
        }
        if (d.length() == 0) {
            d = "d";
        }

        String pattern;
        switch (data.consumeInt(0, 5)) {
            case 0:
                pattern = a + "''" + b + " {0} '" + c + "'" + d;
                break;
            case 1:
                pattern = a + " {0} " + b + "''" + c;
                break;
            case 2:
                pattern = "'" + a + "' " + b + "''" + c + " {0}";
                break;
            case 3:
                pattern = a + "''" + b + " '" + c + "' {0} " + d;
                break;
            case 4:
                pattern = a + " {0} '" + b + "' " + c + "''" + d;
                break;
            default:
                pattern = a + "''" + b + c + " {0} ";
                break;
        }

        MessageFormat mf = new MessageFormat(pattern);
        String expected = mf.format(new Object[] { arg });
        String expectedPattern = mf.toPattern();

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern);
        String before = emf.toPattern();
        String actual = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        if (!expected.equals(actual)) {
            throw new RuntimeException("[oracle:msgfmt-eq] metamorphic violation: ExtendedMessageFormat must match MessageFormat on valid standard pattern input="
                    + pattern + " lhs=" + actual + " rhs=" + expected);
        }
        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: toPattern changed across format input="
                    + pattern + " lhs=" + before + " rhs=" + after);
        }
        if (!after.equals(expectedPattern)) {
            throw new RuntimeException("[oracle:topattern-eq] metamorphic violation: toPattern must match MessageFormat canonical pattern on valid standard pattern input="
                    + pattern + " lhs=" + after + " rhs=" + expectedPattern);
        }
    }

    private static String literal(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch >= 32 && ch != '\'' && ch != '{' && ch != '}') {
                out.append(ch);
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
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. Jazzer exited cleanly with no finding on the buggy code. Most often this means every input was handled normally, or the harnes

---
## [55] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~4,583 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
- *[assistant] message: identical to step [53] — not reprinted*
- *[user] message: identical to step [5] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String anchorPattern = "it''s a {0} 'test'!";
        String anchorArg = "DUMMY";
        ExtendedMessageFormat anchorEmf = new ExtendedMessageFormat(anchorPattern);
        String anchorActual = anchorEmf.format(new Object[] { anchorArg });
        String anchorExpected = new MessageFormat(anchorPattern).format(new Object[] { anchorArg });
        if (!anchorExpected.equals(anchorActual)) {
            throw new RuntimeException("[oracle:anchor-msgfmt] metamorphic violation: ExtendedMessageFormat must match MessageFormat on valid standard pattern input="
                    + anchorPattern + " lhs=" + anchorActual + " rhs=" + anchorExpected);
        }
        String anchorBefore = anchorEmf.toPattern();
        String anchorAfter = anchorEmf.toPattern();
        if (!anchorBefore.equals(anchorAfter)) {
            throw new RuntimeException("[oracle:anchor-topattern] metamorphic violation: toPattern must be stable across reads input="
                    + anchorPattern + " lhs=" + anchorBefore + " rhs=" + anchorAfter);
        }

        String a = literal(data.consumeAsciiString(12));
        String b = literal(data.consumeAsciiString(12));
        String c = literal(data.consumeAsciiString(12));
        String d = literal(data.consumeAsciiString(12));
        String arg = literal(data.consumeString(20));
        if (arg.length() == 0) {
            arg = "X";
        }
        if (a.length() == 0) {
            a = "a";
        }
        if (b.length() == 0) {
            b = "b";
        }
        if (c.length() == 0) {
            c = "c";
        }
        if (d.length() == 0) {
            d = "d";
        }

        String pattern;
        switch (data.consumeInt(0, 5)) {
            case 0:
                pattern = a + "''" + b + " {0} '" + c + "'" + d;
                break;
            case 1:
                pattern = a + " {0} " + b + "''" + c;
                break;
            case 2:
                pattern = "'" + a + "' " + b + "''" + c + " {0}";
                break;
            case 3:
                pattern = a + "''" + b + " '" + c + "' {0} " + d;
                break;
            case 4:
                pattern = a + " {0} '" + b + "' " + c + "''" + d;
                break;
            default:
                pattern = a + "''" + b + c + " {0} ";
                break;
        }

        MessageFormat mf = new MessageFormat(pattern);
        String expected = mf.format(new Object[] { arg });
        String expectedPattern = mf.toPattern();

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern);
        String before = emf.toPattern();
        String actual = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        if (!expected.equals(actual)) {
            throw new RuntimeException("[oracle:msgfmt-eq] metamorphic violation: ExtendedMessageFormat must match MessageFormat on valid standard pattern input="
                    + pattern + " lhs=" + actual + " rhs=" + expected);
        }
        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: toPattern changed across format input="
                    + pattern + " lhs=" + before + " rhs=" + after);
        }
        if (!after.equals(expectedPattern)) {
            throw new RuntimeException("[oracle:topattern-eq] metamorphic violation: toPattern must match MessageFormat canonical pattern on valid standard pattern input="
                    + pattern + " lhs=" + after + " rhs=" + expectedPattern);
        }
    }

    private static String literal(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch >= 32 && ch != '\'' && ch != '{' && ch != '}') {
                out.append(ch);
            }
        }
        return out.toString();
    }
}
```
- *[user] message: identical to step [7] — not reprinted*

</details>
<details open><summary>▸ Output (~4,156 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("x", "y");

        String anchorPattern = "it''s a {0} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, java.util.Locale.getDefault(), registry);
        String anchorArg = "DUMMY";
        String anchorExpected = new MessageFormat(anchorPattern).format(new Object[] { anchorArg });
        String anchorActual = anchor.format(new Object[] { anchorArg });
        if (!anchorExpected.equals(anchorActual)) {
            throw new RuntimeException("[oracle:anchor-msgfmt] metamorphic violation: ExtendedMessageFormat must match MessageFormat for standard patterns input="
                    + anchorPattern + " lhs=" + anchorActual + " rhs=" + anchorExpected);
        }

        String left = clean(data.consumeAsciiString(12));
        String mid = clean(data.consumeAsciiString(12));
        String right = clean(data.consumeAsciiString(12));
        String quoted = clean(data.consumeAsciiString(12));
        String arg = clean(data.consumeString(24));

        if (left.length() == 0) {
            left = "a";
        }
        if (mid.length() == 0) {
            mid = "b";
        }
        if (right.length() == 0) {
            right = "c";
        }
        if (quoted.length() == 0) {
            quoted = "q";
        }
        if (arg.length() == 0) {
            arg = "X";
        }

        String pattern;
        switch (data.consumeInt(0, 5)) {
            case 0:
                pattern = left + "''" + mid + " {0} '" + quoted + "' " + right;
                break;
            case 1:
                pattern = left + "''" + mid + " {0} " + right;
                break;
            case 2:
                pattern = "'" + quoted + "' " + left + "''" + mid + " {0}";
                break;
            case 3:
                pattern = left + " {0} " + mid + "''" + right;
                break;
            case 4:
                pattern = left + "''" + mid + " '" + quoted + "' {0}";
                break;
            default:
                pattern = left + " {0} '" + quoted + "' " + mid + "''" + right;
                break;
        }

        MessageFormat mf = new MessageFormat(pattern);
        String expected = mf.format(new Object[] { arg });
        String expectedPattern = mf.toPattern();

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, java.util.Locale.getDefault(), registry);
        String before = emf.toPattern();
        String actual = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        if (!expected.equals(actual)) {
            throw new RuntimeException("[oracle:msgfmt-eq] metamorphic violation: ExtendedMessageFormat must match MessageFormat for standard patterns input="
                    + pattern + " lhs=" + actual + " rhs=" + expected);
        }
        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: toPattern changed across format input="
                    + pattern + " lhs=" + before + " rhs=" + after);
        }
        if (!after.equals(expectedPattern)) {
            throw new RuntimeException("[oracle:topattern-eq] metamorphic violation: ExtendedMessageFormat toPattern must match MessageFormat canonical pattern input="
                    + pattern + " lhs=" + after + " rhs=" + expectedPattern);
        }
    }

    private static String clean(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch >= 32 && ch != '\'' && ch != '{' && ch != '}') {
                out.append(ch);
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
<details><summary>▸ Prompt (8 message(s), ~4,593 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
- *[assistant] message: identical to step [53] — not reprinted*
- *[user] message: identical to step [5] — not reprinted*
- *[assistant] message: identical to step [55] — not reprinted*
- *[user] message: identical to step [7] — not reprinted*
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
        registry.put("x", "y");

        String anchorPattern = "it''s a {0} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, java.util.Locale.getDefault(), registry);
        String anchorArg = "DUMMY";
        String anchorExpected = new MessageFormat(anchorPattern).format(new Object[] { anchorArg });
        String anchorActual = anchor.format(new Object[] { anchorArg });
        if (!anchorExpected.equals(anchorActual)) {
            throw new RuntimeException("[oracle:anchor-msgfmt] metamorphic violation: ExtendedMessageFormat must match MessageFormat for standard patterns input="
                    + anchorPattern + " lhs=" + anchorActual + " rhs=" + anchorExpected);
        }

        String left = clean(data.consumeAsciiString(12));
        String mid = clean(data.consumeAsciiString(12));
        String right = clean(data.consumeAsciiString(12));
        String quoted = clean(data.consumeAsciiString(12));
        String arg = clean(data.consumeString(24));

        if (left.length() == 0) {
            left = "a";
        }
        if (mid.length() == 0) {
            mid = "b";
        }
        if (right.length() == 0) {
            right = "c";
        }
        if (quoted.length() == 0) {
            quoted = "q";
        }
        if (arg.length() == 0) {
            arg = "X";
        }

        String pattern;
        switch (data.consumeInt(0, 5)) {
            case 0:
                pattern = left + "''" + mid + " {0} '" + quoted + "' " + right;
                break;
            case 1:
                pattern = left + "''" + mid + " {0} " + right;
                break;
            case 2:
                pattern = "'" + quoted + "' " + left + "''" + mid + " {0}";
                break;
            case 3:
                pattern = left + " {0} " + mid + "''" + right;
                break;
            case 4:
                pattern = left + "''" + mid + " '" + quoted + "' {0}";
                break;
            default:
                pattern = left + " {0} '" + quoted + "' " + mid + "''" + right;
                break;
        }

        MessageFormat mf = new MessageFormat(pattern);
        String expected = mf.format(new Object[] { arg });
        String expectedPattern = mf.toPattern();

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, java.util.Locale.getDefault(), registry);
        String before = emf.toPattern();
        String actual = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        if (!expected.equals(actual)) {
            throw new RuntimeException("[oracle:msgfmt-eq] metamorphic violation: ExtendedMessageFormat must match MessageFormat for standard patterns input="
                    + pattern + " lhs=" + actual + " rhs=" + expected);
        }
        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: toPattern changed across format input="
                    + pattern + " lhs=" + before + " rhs=" + after);
        }
        if (!after.equals(expectedPattern)) {
            throw new RuntimeException("[oracle:topattern-eq] metamorphic violation: ExtendedMessageFormat toPattern must match MessageFormat canonical pattern input="
                    + pattern + " lhs=" + after + " rhs=" + expectedPattern);
        }
    }

    private static String clean(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch >= 32 && ch != '\'' && ch != '{' && ch != '}') {
                out.append(ch);
            }
        }
        return out.toString();
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~4,424 chars)</summary>

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

        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchorEmf = new ExtendedMessageFormat(anchorPattern, registry);
        String anchorActual = anchorEmf.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorActual)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: exact regression test output changed input="
                    + anchorPattern + " lhs=" + anchorActual + " rhs=it's a dummy test!");
        }
        String anchorPatternBefore = anchorEmf.toPattern();
        String anchorPatternAfter = anchorEmf.toPattern();
        if (!anchorPatternBefore.equals(anchorPatternAfter)) {
            throw new RuntimeException("[oracle:anchor-topattern] metamorphic violation: toPattern must be stable across reads input="
                    + anchorPattern + " lhs=" + anchorPatternBefore + " rhs=" + anchorPatternAfter);
        }

        String prefix = clean(data.consumeAsciiString(10));
        String infix = clean(data.consumeAsciiString(10));
        String suffix = clean(data.consumeAsciiString(10));
        String quoted = clean(data.consumeAsciiString(10));
        String arg = clean(data.consumeString(20));

        if (prefix.length() == 0) {
            prefix = "pre";
        }
        if (infix.length() == 0) {
            infix = "mid";
        }
        if (suffix.length() == 0) {
            suffix = "suf";
        }
        if (quoted.length() == 0) {
            quoted = "test";
        }
        if (arg.length() == 0) {
            arg = "DuMmY";
        }

        String pattern;
        switch (data.consumeInt(0, 5)) {
            case 0:
                pattern = prefix + "''" + infix + " {0,lower} '" + quoted + "' " + suffix;
                break;
            case 1:
                pattern = prefix + "''" + infix + " {0,upper} '" + quoted + "'!";
                break;
            case 2:
                pattern = "'" + quoted + "' " + prefix + "''" + infix + " {0,lower}";
                break;
            case 3:
                pattern = prefix + " {0,lower} " + infix + "''" + suffix;
                break;
            case 4:
                pattern = prefix + "''" + infix + " {0,upper} " + suffix;
                break;
            default:
                pattern = prefix + "''s a {0,lower} '" + quoted + "'!";
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String actual = emf.format(new Object[] { arg });

        MessageFormat mfLowered = new MessageFormat(pattern.replace(",lower", "").replace(",upper", ""));
        String expected;
        if (pattern.indexOf("{0,upper}") >= 0) {
            expected = mfLowered.format(new Object[] { arg.toUpperCase() });
        } else {
            expected = mfLowered.format(new Object[] { arg.toLowerCase() });
        }

        if (!expected.equals(actual)) {
            throw new RuntimeException("[oracle:format-eq] metamorphic violation: custom lower/upper formatting with quoted text parsed incorrectly input="
                    + pattern + " lhs=" + actual + " rhs=" + expected);
        }

        String before = emf.toPattern();
        String after = emf.toPattern();
        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: toPattern changed across reads input="
                    + pattern + " lhs=" + before + " rhs=" + after);
        }
    }

    private static String clean(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch >= 32 && ch != '\'' && ch != '{' && ch != '}') {
                sb.append(ch);
            }
        }
        return sb.toString();
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
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,176 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        java.util.Map registry = new java.util.HashMap();
        registry.put("lower", null);
        registry.put("upper", null);

        String anchorPattern = "it''s a {0,lower} 'test'!";
        try {
            new ExtendedMessageFormat(anchorPattern, registry);
        } catch (Throwable t) {
            boolean cleanRejection =
                    t instanceof IllegalArgumentException ||
                    t instanceof NumberFormatException ||
                    t.getClass().getName().contains("Validation") ||
                    t.getClass().getName().contains("Invalid");
            if (!cleanRejection) {
                boolean throughPatchedMethod = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                            && "appendQuotedString".equals(ste.getMethodName())) {
                        throughPatchedMethod = true;
                        break;
                    }
                }
                if (t instanceof OutOfMemoryError && throughPatchedMethod) {
                    throw (OutOfMemoryError) t;
                }
            }
        }

        String prefix = data.consumeAsciiString(16);
        String middle = data.consumeAsciiString(16);
        String suffix = data.consumeAsciiString(16);
        String quoted = data.consumeAsciiString(12);
        String arg = data.consumeString(24);

        prefix = prefix.replace("{", "").replace("}", "").replace("'", "");
        middle = middle.replace("{", "").replace("}", "").replace("'", "");
        suffix = suffix.replace("{", "").replace("}", "").replace("'", "");
        quoted = quoted.replace("'", "");

        String[] patterns = new String[3];
        patterns[0] = "''" + prefix + " {0} '" + quoted + "' " + suffix;
        patterns[1] = prefix + "''" + middle + " {0} '" + quoted + "' " + suffix;
        patterns[2] = prefix + " {0} '" + quoted + "' " + middle + "''" + suffix;

        for (int i = 0; i < patterns.length; i++) {
            String pattern = patterns[i];
            try {
                ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
                String before = emf.toPattern();

                String lhs;
                try {
                    lhs = emf.format(new Object[] { arg });
                } catch (Throwable t) {
                    boolean cleanRejection =
                            t instanceof IllegalArgumentException ||
                            t instanceof NumberFormatException ||
                            t.getClass().getName().contains("Validation") ||
                            t.getClass().getName().contains("Invalid");
                    if (!cleanRejection) {
                        boolean throughPatchedMethod = false;
                        for (StackTraceElement ste : t.getStackTrace()) {
                            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                                    && "appendQuotedString".equals(ste.getMethodName())) {
                                throughPatchedMethod = true;
                                break;
                            }
                        }
                        if (t instanceof OutOfMemoryError && throughPatchedMethod) {
                            throw (OutOfMemoryError) t;
                        }
                    }
                    continue;
                }

                String rhs;
                try {
                    rhs = new java.text.MessageFormat(pattern).format(new Object[] { arg });
                } catch (Throwable t) {
                    continue;
                }

                if (!lhs.equals(rhs)) {
                    throw new RuntimeException("[oracle:msgfmt-equiv] metamorphic violation: empty-registry ExtendedMessageFormat must match MessageFormat for standard patterns input=" + pattern + " lhs=" + lhs + " rhs=" + rhs);
                }

                try {
                    String reparsed = new ExtendedMessageFormat(before, registry).toPattern();
                    if (!before.equals(reparsed)) {
                        throw new RuntimeException("[oracle:toPattern-idem] metamorphic violation: toPattern should be a stable reparseable representation input=" + pattern + " lhs=" + before + " rhs=" + reparsed);
                    }
                } catch (Throwable t) {
                    if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                        throw (RuntimeException) t;
                    }
                }
            } catch (Throwable t) {
                boolean cleanRejection =
                        t instanceof IllegalArgumentException ||
                        t instanceof NumberFormatException ||
                        t.getClass().getName().contains("Validation") ||
                        t.getClass().getName().contains("Invalid");
                if (cleanRejection) {
                    continue;
                }
                boolean throughPatchedMethod = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                            && "appendQuotedString".equals(ste.getMethodName())) {
                        throughPatchedMethod = true;
                        break;
                    }
                }
                if (t instanceof OutOfMemoryError && throughPatchedMethod) {
                    throw (OutOfMemoryError) t;
                }
                if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                    throw (RuntimeException) t;
                }
            }
        }
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
<details><summary>▸ Prompt (4 message(s), ~6,373 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        java.util.Map registry = new java.util.HashMap();
        registry.put("lower", null);
        registry.put("upper", null);

        String anchorPattern = "it''s a {0,lower} 'test'!";
        try {
            new ExtendedMessageFormat(anchorPattern, registry);
        } catch (Throwable t) {
            boolean cleanRejection =
                    t instanceof IllegalArgumentException ||
                    t instanceof NumberFormatException ||
                    t.getClass().getName().contains("Validation") ||
                    t.getClass().getName().contains("Invalid");
            if (!cleanRejection) {
                boolean throughPatchedMethod = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                            && "appendQuotedString".equals(ste.getMethodName())) {
                        throughPatchedMethod = true;
                        break;
                    }
                }
                if (t instanceof OutOfMemoryError && throughPatchedMethod) {
                    throw (OutOfMemoryError) t;
                }
            }
        }

        String prefix = data.consumeAsciiString(16);
        String middle = data.consumeAsciiString(16);
        String suffix = data.consumeAsciiString(16);
        String quoted = data.consumeAsciiString(12);
        String arg = data.consumeString(24);

        prefix = prefix.replace("{", "").replace("}", "").replace("'", "");
        middle = middle.replace("{", "").replace("}", "").replace("'", "");
        suffix = suffix.replace("{", "").replace("}", "").replace("'", "");
        quoted = quoted.replace("'", "");

        String[] patterns = new String[3];
        patterns[0] = "''" + prefix + " {0} '" + quoted + "' " + suffix;
        patterns[1] = prefix + "''" + middle + " {0} '" + quoted + "' " + suffix;
        patterns[2] = prefix + " {0} '" + quoted + "' " + middle + "''" + suffix;

        for (int i = 0; i < patterns.length; i++) {
            String pattern = patterns[i];
            try {
                ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
                String before = emf.toPattern();

                String lhs;
                try {
                    lhs = emf.format(new Object[] { arg });
                } catch (Throwable t) {
                    boolean cleanRejection =
                            t instanceof IllegalArgumentException ||
                            t instanceof NumberFormatException ||
                            t.getClass().getName().contains("Validation") ||
                            t.getClass().getName().contains("Invalid");
                    if (!cleanRejection) {
                        boolean throughPatchedMethod = false;
                        for (StackTraceElement ste : t.getStackTrace()) {
                            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                                    && "appendQuotedString".equals(ste.getMethodName())) {
                                throughPatchedMethod = true;
                                break;
                            }
                        }
                        if (t instanceof OutOfMemoryError && throughPatchedMethod) {
                            throw (OutOfMemoryError) t;
                        }
                    }
                    continue;
                }

                String rhs;
                try {
                    rhs = new java.text.MessageFormat(pattern).format(new Object[] { arg });
                } catch (Throwable t) {
                    continue;
                }

                if (!lhs.equals(rhs)) {
                    throw new RuntimeException("[oracle:msgfmt-equiv] metamorphic violation: empty-registry ExtendedMessageFormat must match MessageFormat for standard patterns input=" + pattern + " lhs=" + lhs + " rhs=" + rhs);
                }

                try {
                    String reparsed = new ExtendedMessageFormat(before, registry).toPattern();
                    if (!before.equals(reparsed)) {
                        throw new RuntimeException("[oracle:toPattern-idem] metamorphic violation: toPattern should be a stable reparseable representation input=" + pattern + " lhs=" + before + " rhs=" + reparsed);
                    }
                } catch (Throwable t) {
                    if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                        throw (RuntimeException) t;
                    }
                }
            } catch (Throwable t) {
                boolean cleanRejection =
                        t instanceof IllegalArgumentException ||
                        t instanceof NumberFormatException ||
                        t.getClass().getName().contains("Validation") ||
                        t.getClass().getName().contains("Invalid");
                if (cleanRejection) {
                    continue;
                }
                boolean throughPatchedMethod = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(ste.getClassName())
                            && "appendQuotedString".equals(ste.getMethodName())) {
                        throughPatchedMethod = true;
                        break;
                    }
                }
                if (t instanceof OutOfMemoryError && throughPatchedMethod) {
                    throw (OutOfMemoryError) t;
                }
                if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                    throw (RuntimeException) t;
                }
            }
        }
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~3,387 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: minimal real MessageFormat-compatible trigger for the buggy path.
        // The patched line is in appendQuotedString() when escapingOn && c[start] == QUOTE.
        // A doubled single quote in the pattern reaches that branch through the public constructor.
        runOne("it''s a {0} 'test'!", "dummy");

        // EXPLORE: vary surrounding literal text while preserving the root-cause property:
        // a doubled single quote outside a format element, followed by a real {0} placeholder,
        // so ExtendedMessageFormat must parse the quote escape via appendQuotedString().
        String a = sanitizeLiteral(data.consumeAsciiString(12));
        String b = sanitizeLiteral(data.consumeAsciiString(12));
        String c = sanitizeLiteral(data.consumeAsciiString(12));
        String d = sanitizeLiteral(data.consumeAsciiString(12));
        String arg = data.consumeString(24);

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
                pattern = "x''y {0} '" + a + "' " + b;
                break;
            case 4:
                pattern = a + "''" + b + " {0} '" + c + "' " + d;
                break;
            default:
                pattern = "it''s " + a + " {0} '" + b + "'!";
                break;
        }

        runOne(pattern, arg);
    }

    private static void runOne(String pattern, String arg) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern);

        // Oracle 1:
        // With no custom registry, ExtendedMessageFormat should behave like ordinary MessageFormat
        // on standard MessageFormat patterns. A patch that merely skips quote handling would change
        // observable formatting instead of crashing.
        String actual = emf.format(new Object[] { arg });
        String expected = new java.text.MessageFormat(pattern).format(new Object[] { arg });
        if (!actual.equals(expected)) {
            throw new RuntimeException("[oracle:msgfmt-equiv] metamorphic violation: ExtendedMessageFormat without registry must match MessageFormat for standard pattern input=" + pattern + " lhs=" + actual + " rhs=" + expected);
        }

        // Oracle 2:
        // toPattern() is the canonical pattern representation for this formatter; reparsing it and
        // reading toPattern() again should be stable for a correct implementation.
        String p1 = emf.toPattern();
        String p2 = new ExtendedMessageFormat(p1).toPattern();
        if (!p1.equals(p2)) {
            throw new RuntimeException("[oracle:toPattern-idem] metamorphic violation: reparsing toPattern must be stable input=" + pattern + " lhs=" + p1 + " rhs=" + p2);
        }
    }

    private static String sanitizeLiteral(String s) {
        return s.replace("{", "").replace("}", "").replace("'", "");
    }
}
```

</details>

---
## [62] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. Jazzer exited cleanly with no finding on the buggy code. Most often this means every input was handled normally, or the harnes

---
## [63] ⚙️ coverage · `jazzer coverage dumps`
**output:** **5/26 dump(s) written under /datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/cov; 26 raw fuzz log(s)**
<details><summary>▸ detail (~12,617 chars)</summary>

- detail: {'dumps': [{'harness': 'attempt_001', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_001_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_002', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_002_compiled.exec', 'exists': True, 'bytes': 202}, {'harness': 'attempt_003', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_003_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_004', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_004_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_005', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_005_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_006', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_006_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_008', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_008_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_009', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_009_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_010', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_010_compiled.exec', 'exists': True, 'bytes': 203}, {'harness': 'attempt_011', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_011_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_012', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_012_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_013', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_013_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_014', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_014_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_015', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_015_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_017', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_017_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_019', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_019_compiled.exec', 'exists': True, 'bytes': 207}, {'harness': 'attempt_020', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_020_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_021', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_021_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_022', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_022_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_023', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_023_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_024', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_024_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_025', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_025_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_026', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_026_compiled.exec', 'exists': True, 'bytes': 205}, {'harness': 'attempt_027', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_027_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_029', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_029_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_030', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_030_compiled.exec', 'exists': True, 'bytes': 201}], 'outputs': ['/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_001_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_002_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_003_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_004_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_005_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_006_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_008_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_009_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_010_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_011_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_012_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_013_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_014_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_015_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_017_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_019_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_020_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_021_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_022_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_023_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_024_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_025_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_026_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_027_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_029_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_030_compiled.txt'], 'classpath': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/cov/classpath.json', 'dir': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/cov', 'compiled_attempts': ['attempt_001', 'attempt_002', 'attempt_003', 'attempt_004', 'attempt_005', 'attempt_006', 'attempt_008', 'attempt_009', 'attempt_010', 'attempt_011', 'attempt_012', 'attempt_013', 'attempt_014', 'attempt_015', 'attempt_017', 'attempt_019', 'attempt_020', 'attempt_021', 'attempt_022', 'attempt_023', 'attempt_024', 'attempt_025', 'attempt_026', 'attempt_027', 'attempt_029', 'attempt_030'], 'accepted_attempts': [], 'sources': ['/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_001.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_002.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_003.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_004.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_005.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_006.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_008.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_009.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_010.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_011.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_012.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_013.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_014.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_015.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_017.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_019.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_020.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_021.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_022.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_023.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_024.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_025.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_026.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_027.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_029.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_030.java']}

</details>
