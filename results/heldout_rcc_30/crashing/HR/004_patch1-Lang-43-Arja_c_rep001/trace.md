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
  "FailureTest(test_class='org.apache.commons.lang.text.ExtendedMessageFormatTest', test_method='testEscapedQuote_LANG_477', source_path='/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/Lang_43_buggy/src/test/org/apache/commons/lang/text/ExtendedMessageFormatTest.java', method_source='    public void testEscapedQuote_LANG_477() {\\n        String pattern = \"it\\'\\'s a {0,lower} \\'test\\'!\";\\n        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);\\n        assertEquals(\"it\\'s a dummy test!\", emf.format(new Object[] {\"DUMMY\"}));\\n    }', exception_type='java.lang.OutOfMemoryError', failure_message=None, support_source=None)"
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
<details><summary>▸ Prompt (2 message(s), ~32,168 chars)</summary>

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
<details open><summary>▸ Output (~6,544 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String anchorPattern = "it''s a {0,lower} 'test'!";
        try {
            exercisePattern(anchorPattern, new Object[] { "DUMMY" }, false);
        } catch (Throwable t) {
            handleThrowable(t);
        }

        String safeArg = data.consumeAsciiString(16);
        String prefix = sanitizeLiteral(data.consumeAsciiString(24));
        String middle = sanitizeLiteral(data.consumeAsciiString(24));
        String suffix = sanitizeLiteral(data.consumeAsciiString(24));

        int variant = data.consumeInt(0, 5);
        String pattern;
        switch (variant) {
            case 0:
                pattern = prefix + "''" + middle + "{0}" + suffix;
                break;
            case 1:
                pattern = "'" + prefix + "'" + middle + "{0}" + suffix;
                break;
            case 2:
                pattern = prefix + "{0}" + middle + "''" + suffix;
                break;
            case 3:
                pattern = prefix + "''" + middle + " '{0}' " + suffix;
                break;
            case 4:
                pattern = prefix + "'x'" + middle + "{0}" + suffix;
                break;
            default:
                pattern = prefix + "''" + middle + "{0,number}" + suffix;
                break;
        }

        try {
            exercisePattern(pattern, new Object[] { safeArg }, true);
        } catch (Throwable t) {
            handleThrowable(t);
        }
    }

    private static void exercisePattern(String pattern, Object[] args, boolean runOracles) {
        Map registry = new HashMap();
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

        if (!runOracles) {
            try {
                emf.format(args);
            } catch (Throwable ignored) {
            }
            return;
        }

        String beforeToPattern;
        String firstFormatted;
        try {
            beforeToPattern = emf.toPattern();
            firstFormatted = emf.format(args);
        } catch (Throwable t) {
            throw t;
        }

        try {
            String afterToPattern = emf.toPattern();
            if (beforeToPattern != null ? !beforeToPattern.equals(afterToPattern) : afterToPattern != null) {
                throw new RuntimeException("[oracle:stable-topattern] metamorphic violation: format must not mutate the pattern input="
                        + pattern + " before=" + beforeToPattern + " after=" + afterToPattern);
            }
        } catch (Throwable ignored) {
            return;
        }

        try {
            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(beforeToPattern, registry);
            String reparsedToPattern = emf2.toPattern();
            String secondFormatted = emf2.format(args);

            if (beforeToPattern != null ? !beforeToPattern.equals(reparsedToPattern) : reparsedToPattern != null) {
                throw new RuntimeException("[oracle:roundtrip-topattern] metamorphic violation: reparsing toPattern must preserve toPattern input="
                        + pattern + " first=" + beforeToPattern + " second=" + reparsedToPattern);
            }

            if (firstFormatted != null ? !firstFormatted.equals(secondFormatted) : secondFormatted != null) {
                throw new RuntimeException("[oracle:roundtrip-format] metamorphic violation: formatting after reparsing toPattern must be consistent input="
                        + pattern + " lhs=" + firstFormatted + " rhs=" + secondFormatted);
            }
        } catch (Throwable ignored) {
        }
    }

    private static String sanitizeLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "a";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\'' && c != '{' && c != '}') {
                sb.append(c);
            }
        }
        if (sb.length() == 0) {
            sb.append('a');
        }
        return sb.toString();
    }

    private static void handleThrowable(Throwable t) {
        if (t instanceof RuntimeException && isOracleViolation((RuntimeException) t)) {
            throw (RuntimeException) t;
        }
        if (isRootCause(t)) {
            if (t instanceof Error) {
                throw (Error) t;
            }
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            throw new RuntimeException(t);
        }
        if (isCleanRejection(t)) {
            return;
        }
    }

    private static boolean isOracleViolation(RuntimeException t) {
        String msg = t.getMessage();
        return msg != null && msg.startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name != null) {
                String lower = name.toLowerCase();
                if (lower.contains("validate") || lower.contains("invalid")) {
                    return true;
                }
            }
        }
        return false;
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
            String cls = ste.getClassName();
            String m = ste.getMethodName();
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
}
```

</details>

---
## [4] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [5] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~9,525 chars, 2 new)</summary>

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
        String anchorPattern = "it''s a {0,lower} 'test'!";
        try {
            exercisePattern(anchorPattern, new Object[] { "DUMMY" }, false);
        } catch (Throwable t) {
            handleThrowable(t);
        }

        String safeArg = data.consumeAsciiString(16);
        String prefix = sanitizeLiteral(data.consumeAsciiString(24));
        String middle = sanitizeLiteral(data.consumeAsciiString(24));
        String suffix = sanitizeLiteral(data.consumeAsciiString(24));

        int variant = data.consumeInt(0, 5);
        String pattern;
        switch (variant) {
            case 0:
                pattern = prefix + "''" + middle + "{0}" + suffix;
                break;
            case 1:
                pattern = "'" + prefix + "'" + middle + "{0}" + suffix;
                break;
            case 2:
                pattern = prefix + "{0}" + middle + "''" + suffix;
                break;
            case 3:
                pattern = prefix + "''" + middle + " '{0}' " + suffix;
                break;
            case 4:
                pattern = prefix + "'x'" + middle + "{0}" + suffix;
                break;
            default:
                pattern = prefix + "''" + middle + "{0,number}" + suffix;
                break;
        }

        try {
            exercisePattern(pattern, new Object[] { safeArg }, true);
        } catch (Throwable t) {
            handleThrowable(t);
        }
    }

    private static void exercisePattern(String pattern, Object[] args, boolean runOracles) {
        Map registry = new HashMap();
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

        if (!runOracles) {
            try {
                emf.format(args);
            } catch (Throwable ignored) {
            }
            return;
        }

        String beforeToPattern;
        String firstFormatted;
        try {
            beforeToPattern = emf.toPattern();
            firstFormatted = emf.format(args);
        } catch (Throwable t) {
            throw t;
        }

        try {
            String afterToPattern = emf.toPattern();
            if (beforeToPattern != null ? !beforeToPattern.equals(afterToPattern) : afterToPattern != null) {
                throw new RuntimeException("[oracle:stable-topattern] metamorphic violation: format must not mutate the pattern input="
                        + pattern + " before=" + beforeToPattern + " after=" + afterToPattern);
            }
        } catch (Throwable ignored) {
            return;
        }

        try {
            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(beforeToPattern, registry);
            String reparsedToPattern = emf2.toPattern();
            String secondFormatted = emf2.format(args);

            if (beforeToPattern != null ? !beforeToPattern.equals(reparsedToPattern) : reparsedToPattern != null) {
                throw new RuntimeException("[oracle:roundtrip-topattern] metamorphic violation: reparsing toPattern must preserve toPattern input="
                        + pattern + " first=" + beforeToPattern + " second=" + reparsedToPattern);
            }

            if (firstFormatted != null ? !firstFormatted.equals(secondFormatted) : secondFormatted != null) {
                throw new RuntimeException("[oracle:roundtrip-format] metamorphic violation: formatting after reparsing toPattern must be consistent input="
                        + pattern + " lhs=" + firstFormatted + " rhs=" + secondFormatted);
            }
        } catch (Throwable ignored) {
        }
    }

    private static String sanitizeLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "a";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\'' && c != '{' && c != '}') {
                sb.append(c);
            }
        }
        if (sb.length() == 0) {
            sb.append('a');
        }
        return sb.toString();
    }

    private static void handleThrowable(Throwable t) {
        if (t instanceof RuntimeException && isOracleViolation((RuntimeException) t)) {
            throw (RuntimeException) t;
        }
        if (isRootCause(t)) {
            if (t instanceof Error) {
                throw (Error) t;
            }
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            throw new RuntimeException(t);
        }
        if (isCleanRejection(t)) {
            return;
        }
    }

    private static boolean isOracleViolation(RuntimeException t) {
        String msg = t.getMessage();
        return msg != null && msg.startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name != null) {
                String lower = name.toLowerCase();
                if (lower.contains("validate") || lower.contains("invalid")) {
                    return true;
                }
            }
        }
        return false;
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
            String cls = ste.getClassName();
            String m = ste.getMethodName();
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
<details open><summary>▸ Output (~2,613 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        try {
            new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);
        } catch (IllegalArgumentException e) {
        }

        new ExtendedMessageFormat("it''s a {0} 'test'!", registry);

        String prefix = sanitizeLiteral(data.consumeAsciiString(24));
        String middle = sanitizeLiteral(data.consumeAsciiString(24));
        String suffix = sanitizeLiteral(data.consumeAsciiString(24));
        String arg = data.consumeAsciiString(16);

        int variant = data.consumeInt(0, 5);
        String pattern;
        switch (variant) {
            case 0:
                pattern = prefix + "''" + middle + "{0}" + suffix;
                break;
            case 1:
                pattern = prefix + "{0}" + middle + "''" + suffix;
                break;
            case 2:
                pattern = "'" + prefix + "''" + middle + "'" + suffix + "{0}";
                break;
            case 3:
                pattern = prefix + "''" + middle + " 'x' " + suffix + "{0}";
                break;
            case 4:
                pattern = prefix + "''" + middle + "{0,number}" + suffix;
                break;
            default:
                pattern = prefix + "''" + middle + "{0,date}" + suffix;
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String once = emf.format(new Object[] { arg });

        String toPattern = emf.toPattern();
        ExtendedMessageFormat reparsed = new ExtendedMessageFormat(toPattern, registry);
        String twice = reparsed.format(new Object[] { arg });
        if (!once.equals(twice)) {
            throw new RuntimeException("[oracle:roundtrip-format] metamorphic violation: input=" + pattern + " lhs=" + once + " rhs=" + twice);
        }
    }

    private static String sanitizeLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "a";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\'' && c != '{' && c != '}') {
                sb.append(c);
            }
        }
        if (sb.length() == 0) {
            sb.append('a');
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
<details><summary>▸ Prompt (6 message(s), ~2,929 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
- *[assistant] message: identical to step [5] — not reprinted*
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

        try {
            new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);
        } catch (IllegalArgumentException e) {
        }

        new ExtendedMessageFormat("it''s a {0} 'test'!", registry);

        String prefix = sanitizeLiteral(data.consumeAsciiString(24));
        String middle = sanitizeLiteral(data.consumeAsciiString(24));
        String suffix = sanitizeLiteral(data.consumeAsciiString(24));
        String arg = data.consumeAsciiString(16);

        int variant = data.consumeInt(0, 5);
        String pattern;
        switch (variant) {
            case 0:
                pattern = prefix + "''" + middle + "{0}" + suffix;
                break;
            case 1:
                pattern = prefix + "{0}" + middle + "''" + suffix;
                break;
            case 2:
                pattern = "'" + prefix + "''" + middle + "'" + suffix + "{0}";
                break;
            case 3:
                pattern = prefix + "''" + middle + " 'x' " + suffix + "{0}";
                break;
            case 4:
                pattern = prefix + "''" + middle + "{0,number}" + suffix;
                break;
            default:
                pattern = prefix + "''" + middle + "{0,date}" + suffix;
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String once = emf.format(new Object[] { arg });

        String toPattern = emf.toPattern();
        ExtendedMessageFormat reparsed = new ExtendedMessageFormat(toPattern, registry);
        String twice = reparsed.format(new Object[] { arg });
        if (!once.equals(twice)) {
            throw new RuntimeException("[oracle:roundtrip-format] metamorphic violation: input=" + pattern + " lhs=" + once + " rhs=" + twice);
        }
    }

    private static String sanitizeLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "a";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\'' && c != '{' && c != '}') {
                sb.append(c);
            }
        }
        if (sb.length() == 0) {
            sb.append('a');
        }
        return sb.toString();
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~5,500 chars)</summary>

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
        Map registry = buildRegistry();

        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, registry);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: input=" + anchorPattern + " out=" + anchorOut);
        }
        String anchorToPattern = anchor.toPattern();
        ExtendedMessageFormat anchorRoundTrip = new ExtendedMessageFormat(anchorToPattern, registry);
        String anchorOut2 = anchorRoundTrip.format(new Object[] { "DUMMY" });
        if (!anchorOut.equals(anchorOut2)) {
            throw new RuntimeException("[oracle:anchor-roundtrip] metamorphic violation: input=" + anchorPattern + " lhs=" + anchorOut + " rhs=" + anchorOut2);
        }

        String a = sanitizeLiteral(data.consumeAsciiString(20));
        String b = sanitizeLiteral(data.consumeAsciiString(20));
        String c = sanitizeLiteral(data.consumeAsciiString(20));
        String d = sanitizeLiteral(data.consumeAsciiString(20));
        String arg = sanitizeLiteral(data.consumeAsciiString(20));
        String fmt = data.consumeBoolean() ? "lower" : "upper";

        String pattern;
        switch (data.consumeInt(0, 4)) {
            case 0:
                pattern = a + "''" + b + " {" + "0," + fmt + "} '" + c + "'" + d;
                break;
            case 1:
                pattern = a + "''" + b + "{0," + fmt + "}" + c;
                break;
            case 2:
                pattern = a + " '' " + b + " {0," + fmt + "} '" + c + "' " + d;
                break;
            case 3:
                pattern = a + "{0," + fmt + "}" + b + "''" + c + "'x'" + d;
                break;
            default:
                pattern = a + "''" + b + " {0," + fmt + "} " + c;
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String once = emf.format(new Object[] { arg });

        String tp = emf.toPattern();
        ExtendedMessageFormat reparsed = new ExtendedMessageFormat(tp, registry);
        String twice = reparsed.format(new Object[] { arg });

        if (!once.equals(twice)) {
            throw new RuntimeException("[oracle:format-roundtrip] metamorphic violation: input=" + pattern + " lhs=" + once + " rhs=" + twice);
        }

        String tp2 = reparsed.toPattern();
        if (!tp.equals(tp2)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: input=" + pattern + " lhs=" + tp + " rhs=" + tp2);
        }
    }

    private static Map buildRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static String sanitizeLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "a";
        }
        StringBuffer sb = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch != '{' && ch != '}' && ch != '\'') {
                sb.append(ch);
            }
        }
        if (sb.length() == 0) {
            sb.append('a');
        }
        return sb.toString();
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

        LowerCaseFormat(Locale locale) {
            this.locale = locale == null ? Locale.getDefault() : locale;
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

    private static final class UpperCaseFormat extends Format {
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
<details><summary>▸ Prompt (8 message(s), ~5,935 chars, 1 new)</summary>

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
        Map registry = buildRegistry();

        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, registry);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: input=" + anchorPattern + " out=" + anchorOut);
        }
        String anchorToPattern = anchor.toPattern();
        ExtendedMessageFormat anchorRoundTrip = new ExtendedMessageFormat(anchorToPattern, registry);
        String anchorOut2 = anchorRoundTrip.format(new Object[] { "DUMMY" });
        if (!anchorOut.equals(anchorOut2)) {
            throw new RuntimeException("[oracle:anchor-roundtrip] metamorphic violation: input=" + anchorPattern + " lhs=" + anchorOut + " rhs=" + anchorOut2);
        }

        String a = sanitizeLiteral(data.consumeAsciiString(20));
        String b = sanitizeLiteral(data.consumeAsciiString(20));
        String c = sanitizeLiteral(data.consumeAsciiString(20));
        String d = sanitizeLiteral(data.consumeAsciiString(20));
        String arg = sanitizeLiteral(data.consumeAsciiString(20));
        String fmt = data.consumeBoolean() ? "lower" : "upper";

        String pattern;
        switch (data.consumeInt(0, 4)) {
            case 0:
                pattern = a + "''" + b + " {" + "0," + fmt + "} '" + c + "'" + d;
                break;
            case 1:
                pattern = a + "''" + b + "{0," + fmt + "}" + c;
                break;
            case 2:
                pattern = a + " '' " + b + " {0," + fmt + "} '" + c + "' " + d;
                break;
            case 3:
                pattern = a + "{0," + fmt + "}" + b + "''" + c + "'x'" + d;
                break;
            default:
                pattern = a + "''" + b + " {0," + fmt + "} " + c;
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String once = emf.format(new Object[] { arg });

        String tp = emf.toPattern();
        ExtendedMessageFormat reparsed = new ExtendedMessageFormat(tp, registry);
        String twice = reparsed.format(new Object[] { arg });

        if (!once.equals(twice)) {
            throw new RuntimeException("[oracle:format-roundtrip] metamorphic violation: input=" + pattern + " lhs=" + once + " rhs=" + twice);
        }

        String tp2 = reparsed.toPattern();
        if (!tp.equals(tp2)) {
            throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: input=" + pattern + " lhs=" + tp + " rhs=" + tp2);
        }
    }

    private static Map buildRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static String sanitizeLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "a";
        }
        StringBuffer sb = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch != '{' && ch != '}' && ch != '\'') {
                sb.append(ch);
            }
        }
        if (sb.length() == 0) {
            sb.append('a');
        }
        return sb.toString();
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

        LowerCaseFormat(Locale locale) {
            this.locale = locale == null ? Locale.getDefault() : locale;
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

    private static final class UpperCaseFormat extends Format {
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
            pos.setIndex(source.length());
            return source;
        }
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~5,321 chars)</summary>

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
        Map registry = buildRegistry();

        ExtendedMessageFormat anchor = new ExtendedMessageFormat("", Locale.getDefault(), registry);
        try {
            anchor.applyPattern("it''s a {0,lower} 'test'!");
        } catch (IllegalArgumentException ignored) {
        }

        String left = sanitize(data.consumeAsciiString(24));
        String middle = sanitize(data.consumeAsciiString(24));
        String right = sanitize(data.consumeAsciiString(24));
        String arg = sanitize(data.consumeAsciiString(24));
        if (arg.length() == 0) {
            arg = "X";
        }

        String pattern;
        switch (data.consumeInt(0, 4)) {
            case 0:
                pattern = left + "''" + middle + "{0}" + right;
                break;
            case 1:
                pattern = left + "{0}" + middle + "''" + right;
                break;
            case 2:
                pattern = left + "''" + middle + " '{0}' " + right + "{0}";
                break;
            case 3:
                pattern = left + "'q'" + middle + "''" + right + "{0}";
                break;
            default:
                pattern = left + "''" + middle + " {0} " + right;
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat("", Locale.getDefault(), registry);
        emf.applyPattern(pattern);

        String before = emf.toPattern();
        String formatted1 = emf.format(new Object[] { arg });
        String after = emf.toPattern();

        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:stable-topattern] metamorphic violation: format must not change toPattern input=" + pattern + " before=" + before + " after=" + after);
        }

        ExtendedMessageFormat reparsed = new ExtendedMessageFormat("", Locale.getDefault(), registry);
        reparsed.applyPattern(before);
        String formatted2 = reparsed.format(new Object[] { arg });
        String reparsedPattern = reparsed.toPattern();

        if (!formatted1.equals(formatted2)) {
            throw new RuntimeException("[oracle:roundtrip-format] metamorphic violation: parsing toPattern and formatting again must preserve output input=" + pattern + " lhs=" + formatted1 + " rhs=" + formatted2);
        }

        if (!before.equals(reparsedPattern)) {
            throw new RuntimeException("[oracle:roundtrip-topattern] metamorphic violation: applying toPattern and reading it back must be idempotent input=" + pattern + " lhs=" + before + " rhs=" + reparsedPattern);
        }
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "a";
        }
        StringBuffer out = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch != '{' && ch != '}' && ch != '\'') {
                out.append(ch);
            }
        }
        if (out.length() == 0) {
            out.append('a');
        }
        return out.toString();
    }

    private static Map buildRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
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
            String s = String.valueOf(obj);
            return toAppendTo.append(s.toLowerCase(locale));
        }

        public Object parseObject(String source, ParsePosition pos) {
            pos.setIndex(source.length());
            return source;
        }
    }

    private static final class UpperCaseFormat extends Format {
        private final Locale locale;

        private UpperCaseFormat(Locale locale) {
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            return toAppendTo.append(s.toUpperCase(locale));
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
<details><summary>▸ Prompt (2 message(s), ~32,042 chars, 1 new)</summary>

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
<details open><summary>▸ Output (~7,409 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = makeRegistry();

        // ANCHOR: exact reproducer from the failing test.
        exercise(
                "it''s a {0,lower} 'test'!",
                "it's a dummy test!",
                registry);

        // EXPLORE / strategy (c): vary inputs around the patched condition.
        // Root cause: when applyPattern() encounters a QUOTE and calls
        // appendQuotedString(..., escapingOn=true), the buggy code fails to advance
        // ParsePosition for an escaped quote at the current position. Build many valid
        // patterns that contain "''" at different positions so old/new behavior differs.
        String left = plain(data.consumeAsciiString(24));
        String mid = plain(data.consumeAsciiString(24));
        String right = plain(data.consumeAsciiString(24));

        if (left.length() == 0) {
            left = "a";
        }
        if (mid.length() == 0) {
            mid = "b";
        }
        if (right.length() == 0) {
            right = "c";
        }

        int variant = data.consumeInt(0, 5);
        String pattern;
        String expected;
        switch (variant) {
            case 0:
                pattern = "''" + left + "{0,lower}" + right;
                expected = "'" + left + "dummy" + right;
                break;
            case 1:
                pattern = left + "''" + mid + "{0,lower}" + right;
                expected = left + "'" + mid + "dummy" + right;
                break;
            case 2:
                pattern = left + "{0,lower}" + "''" + right;
                expected = left + "dummy'" + right;
                break;
            case 3:
                pattern = left + "''" + "{0,lower}" + "''" + right;
                expected = left + "'dummy'" + right;
                break;
            case 4:
                pattern = left + " {0,lower} ''" + right;
                expected = left + " dummy '" + right;
                break;
            default:
                pattern = left + "''" + mid + " {0,lower} 'x' " + right;
                expected = left + "'" + mid + " dummy x " + right;
                break;
        }

        exercise(pattern, expected, registry);
    }

    private static void exercise(String pattern, String expected, Map registry) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

            // Contract/post-condition: ExtendedMessageFormat preserves custom format
            // descriptions in toPattern(); for a successfully parsed pattern, rebuilding
            // from toPattern() should describe the same pattern again.
            String toPattern = emf.toPattern();
            if (!pattern.equals(toPattern)) {
                throw new RuntimeException(
                        "[oracle:toPattern] metamorphic violation: input=" + pattern
                                + " toPattern=" + toPattern);
            }

            String formatted = emf.format(new Object[] { "DUMMY" });
            if (!expected.equals(formatted)) {
                throw new RuntimeException(
                        "[oracle:format] metamorphic violation: input=" + pattern
                                + " expected=" + expected + " actual=" + formatted);
            }

            // Metamorphic check: formatting a second object reconstructed from toPattern()
            // must agree with the first one, since both are created from the same real API.
            try {
                ExtendedMessageFormat emf2 = new ExtendedMessageFormat(toPattern, registry);
                String formatted2 = emf2.format(new Object[] { "DUMMY" });
                if (!formatted.equals(formatted2)) {
                    throw new RuntimeException(
                            "[oracle:roundtrip] metamorphic violation: input=" + pattern
                                    + " lhs=" + formatted + " rhs=" + formatted2);
                }
            } catch (Throwable t) {
                if (isOracleFailure(t)) {
                    throw (RuntimeException) t;
                }
                if (isRootCause(t)) {
                    rethrow(t);
                }
                if (!isCleanRejection(t)) {
                    // Skip check on unrelated failures to avoid false positives.
                }
            }
        } catch (Throwable t) {
            if (isOracleFailure(t)) {
                throw (RuntimeException) t;
            }
            if (isRootCause(t)) {
                rethrow(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            // Swallow unrelated throwables to avoid flagging bugs outside this patch.
        }
    }

    private static Map makeRegistry() {
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
            String cls = ste.getClassName();
            String method = ste.getMethodName();
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cls)
                    && ("appendQuotedString".equals(method) || "next".equals(method))) {
                return true;
            }
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                    && ("append".equals(method) || "startsWith".equals(method))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOracleFailure(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
    }

    private static String plain(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length() && sb.length() < 24; i++) {
            char ch = s.charAt(i);
            if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || ch == ' ') {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static void rethrow(Throwable t) {
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
- reason: That did not compile. javac reported:

---
## [13] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~9,102 chars, 2 new)</summary>

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
        Map registry = makeRegistry();

        // ANCHOR: exact reproducer from the failing test.
        exercise(
                "it''s a {0,lower} 'test'!",
                "it's a dummy test!",
                registry);

        // EXPLORE / strategy (c): vary inputs around the patched condition.
        // Root cause: when applyPattern() encounters a QUOTE and calls
        // appendQuotedString(..., escapingOn=true), the buggy code fails to advance
        // ParsePosition for an escaped quote at the current position. Build many valid
        // patterns that contain "''" at different positions so old/new behavior differs.
        String left = plain(data.consumeAsciiString(24));
        String mid = plain(data.consumeAsciiString(24));
        String right = plain(data.consumeAsciiString(24));

        if (left.length() == 0) {
            left = "a";
        }
        if (mid.length() == 0) {
            mid = "b";
        }
        if (right.length() == 0) {
            right = "c";
        }

        int variant = data.consumeInt(0, 5);
        String pattern;
        String expected;
        switch (variant) {
            case 0:
                pattern = "''" + left + "{0,lower}" + right;
                expected = "'" + left + "dummy" + right;
                break;
            case 1:
                pattern = left + "''" + mid + "{0,lower}" + right;
                expected = left + "'" + mid + "dummy" + right;
                break;
            case 2:
                pattern = left + "{0,lower}" + "''" + right;
                expected = left + "dummy'" + right;
                break;
            case 3:
                pattern = left + "''" + "{0,lower}" + "''" + right;
                expected = left + "'dummy'" + right;
                break;
            case 4:
                pattern = left + " {0,lower} ''" + right;
                expected = left + " dummy '" + right;
                break;
            default:
                pattern = left + "''" + mid + " {0,lower} 'x' " + right;
                expected = left + "'" + mid + " dummy x " + right;
                break;
        }

        exercise(pattern, expected, registry);
    }

    private static void exercise(String pattern, String expected, Map registry) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

            // Contract/post-condition: ExtendedMessageFormat preserves custom format
            // descriptions in toPattern(); for a successfully parsed pattern, rebuilding
            // from toPattern() should describe the same pattern again.
            String toPattern = emf.toPattern();
            if (!pattern.equals(toPattern)) {
                throw new RuntimeException(
                        "[oracle:toPattern] metamorphic violation: input=" + pattern
                                + " toPattern=" + toPattern);
            }

            String formatted = emf.format(new Object[] { "DUMMY" });
            if (!expected.equals(formatted)) {
                throw new RuntimeException(
                        "[oracle:format] metamorphic violation: input=" + pattern
                                + " expected=" + expected + " actual=" + formatted);
            }

            // Metamorphic check: formatting a second object reconstructed from toPattern()
            // must agree with the first one, since both are created from the same real API.
            try {
                ExtendedMessageFormat emf2 = new ExtendedMessageFormat(toPattern, registry);
                String formatted2 = emf2.format(new Object[] { "DUMMY" });
                if (!formatted.equals(formatted2)) {
                    throw new RuntimeException(
                            "[oracle:roundtrip] metamorphic violation: input=" + pattern
                                    + " lhs=" + formatted + " rhs=" + formatted2);
                }
            } catch (Throwable t) {
                if (isOracleFailure(t)) {
                    throw (RuntimeException) t;
                }
                if (isRootCause(t)) {
                    rethrow(t);
                }
                if (!isCleanRejection(t)) {
                    // Skip check on unrelated failures to avoid false positives.
                }
            }
        } catch (Throwable t) {
            if (isOracleFailure(t)) {
                throw (RuntimeException) t;
            }
            if (isRootCause(t)) {
                rethrow(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            // Swallow unrelated throwables to avoid flagging bugs outside this patch.
        }
    }

    private static Map makeRegistry() {
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
            String cls = ste.getClassName();
            String method = ste.getMethodName();
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cls)
                    && ("appendQuotedString".equals(method) || "next".equals(method))) {
                return true;
            }
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                    && ("append".equals(method) || "startsWith".equals(method))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOracleFailure(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
    }

    private static String plain(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length() && sb.length() < 24; i++) {
            char ch = s.charAt(i);
            if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || ch == ' ') {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static void rethrow(Throwable t) {
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
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/Lang_43_buggy/fuzz/attempt_005/FuzzHarness.java:128: error: cannot find symbol
            registry.put("lower", new LowerCaseFormatFactory());
                                      ^
  symbol:   class LowerCaseFormatFactory
  location: class FuzzHarness
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/Lang_43_buggy/fuzz/attempt_005/FuzzHarness.java:132: error: cannot find symbol
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
<details open><summary>▸ Output (~6,588 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        exercise("it''s a {0} 'test'!", "it's a dummy test!", registry);

        String left = sanitize(data.consumeAsciiString(24));
        String middle = sanitize(data.consumeAsciiString(24));
        String right = sanitize(data.consumeAsciiString(24));

        if (left.length() == 0) {
            left = "a";
        }
        if (middle.length() == 0) {
            middle = "b";
        }
        if (right.length() == 0) {
            right = "c";
        }

        int variant = data.consumeInt(0, 7);
        String pattern;
        String expected;
        switch (variant) {
            case 0:
                pattern = "''" + left + "{0}" + right;
                expected = "'" + left + "dummy" + right;
                break;
            case 1:
                pattern = left + "''" + middle + "{0}" + right;
                expected = left + "'" + middle + "dummy" + right;
                break;
            case 2:
                pattern = left + "{0}" + "''" + right;
                expected = left + "dummy'" + right;
                break;
            case 3:
                pattern = left + "''" + "{0}" + "''" + right;
                expected = left + "'dummy'" + right;
                break;
            case 4:
                pattern = left + " {0} ''" + right;
                expected = left + " dummy '" + right;
                break;
            case 5:
                pattern = left + "''" + middle + " {0} 'x' " + right;
                expected = left + "'" + middle + " dummy x " + right;
                break;
            case 6:
                pattern = "'" + left + "' " + "''" + " {0} " + right;
                expected = left + " ' dummy " + right;
                break;
            default:
                pattern = left + "''" + middle + "''" + right + " {0}";
                expected = left + "'" + middle + "'" + right + " dummy";
                break;
        }

        exercise(pattern, expected, registry);
    }

    private static void exercise(String pattern, String expected, Map registry) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

            String toPattern = emf.toPattern();
            if (!pattern.equals(toPattern)) {
                throw new RuntimeException(
                        "[oracle:toPattern] metamorphic violation: input=" + pattern
                                + " toPattern=" + toPattern);
            }

            String formatted = emf.format(new Object[] { "dummy" });
            if (!expected.equals(formatted)) {
                throw new RuntimeException(
                        "[oracle:format] metamorphic violation: input=" + pattern
                                + " expected=" + expected + " actual=" + formatted);
            }

            try {
                ExtendedMessageFormat emf2 = new ExtendedMessageFormat(toPattern, registry);
                String formatted2 = emf2.format(new Object[] { "dummy" });
                if (!formatted.equals(formatted2)) {
                    throw new RuntimeException(
                            "[oracle:roundtrip] metamorphic violation: input=" + pattern
                                    + " lhs=" + formatted + " rhs=" + formatted2);
                }
            } catch (Throwable t) {
                if (isOracleFailure(t)) {
                    throw (RuntimeException) t;
                }
                if (isRootCause(t)) {
                    rethrow(t);
                }
                if (isCleanRejection(t)) {
                    return;
                }
            }
        } catch (Throwable t) {
            if (isOracleFailure(t)) {
                throw (RuntimeException) t;
            }
            if (isRootCause(t)) {
                rethrow(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static String sanitize(String s) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length() && sb.length() < 24; i++) {
            char ch = s.charAt(i);
            if ((ch >= 'a' && ch <= 'z')
                    || (ch >= 'A' && ch <= 'Z')
                    || (ch >= '0' && ch <= '9')
                    || ch == ' ') {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.indexOf("Validation") >= 0 || name.indexOf("Invalid") >= 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        for (int i = 0; i < stack.length; i++) {
            String cls = stack[i].getClassName();
            String method = stack[i].getMethodName();
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

    private static boolean isOracleFailure(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
    }

    private static void rethrow(Throwable t) {
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
## [14] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [15] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~6,907 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [11] — not reprinted*
- *[assistant] message: identical to step [13] — not reprinted*
- *[user] message: identical to step [13] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        exercise("it''s a {0} 'test'!", "it's a dummy test!", registry);

        String left = sanitize(data.consumeAsciiString(24));
        String middle = sanitize(data.consumeAsciiString(24));
        String right = sanitize(data.consumeAsciiString(24));

        if (left.length() == 0) {
            left = "a";
        }
        if (middle.length() == 0) {
            middle = "b";
        }
        if (right.length() == 0) {
            right = "c";
        }

        int variant = data.consumeInt(0, 7);
        String pattern;
        String expected;
        switch (variant) {
            case 0:
                pattern = "''" + left + "{0}" + right;
                expected = "'" + left + "dummy" + right;
                break;
            case 1:
                pattern = left + "''" + middle + "{0}" + right;
                expected = left + "'" + middle + "dummy" + right;
                break;
            case 2:
                pattern = left + "{0}" + "''" + right;
                expected = left + "dummy'" + right;
                break;
            case 3:
                pattern = left + "''" + "{0}" + "''" + right;
                expected = left + "'dummy'" + right;
                break;
            case 4:
                pattern = left + " {0} ''" + right;
                expected = left + " dummy '" + right;
                break;
            case 5:
                pattern = left + "''" + middle + " {0} 'x' " + right;
                expected = left + "'" + middle + " dummy x " + right;
                break;
            case 6:
                pattern = "'" + left + "' " + "''" + " {0} " + right;
                expected = left + " ' dummy " + right;
                break;
            default:
                pattern = left + "''" + middle + "''" + right + " {0}";
                expected = left + "'" + middle + "'" + right + " dummy";
                break;
        }

        exercise(pattern, expected, registry);
    }

    private static void exercise(String pattern, String expected, Map registry) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

            String toPattern = emf.toPattern();
            if (!pattern.equals(toPattern)) {
                throw new RuntimeException(
                        "[oracle:toPattern] metamorphic violation: input=" + pattern
                                + " toPattern=" + toPattern);
            }

            String formatted = emf.format(new Object[] { "dummy" });
            if (!expected.equals(formatted)) {
                throw new RuntimeException(
                        "[oracle:format] metamorphic violation: input=" + pattern
                                + " expected=" + expected + " actual=" + formatted);
            }

            try {
                ExtendedMessageFormat emf2 = new ExtendedMessageFormat(toPattern, registry);
                String formatted2 = emf2.format(new Object[] { "dummy" });
                if (!formatted.equals(formatted2)) {
                    throw new RuntimeException(
                            "[oracle:roundtrip] metamorphic violation: input=" + pattern
                                    + " lhs=" + formatted + " rhs=" + formatted2);
                }
            } catch (Throwable t) {
                if (isOracleFailure(t)) {
                    throw (RuntimeException) t;
                }
                if (isRootCause(t)) {
                    rethrow(t);
                }
                if (isCleanRejection(t)) {
                    return;
                }
            }
        } catch (Throwable t) {
            if (isOracleFailure(t)) {
                throw (RuntimeException) t;
            }
            if (isRootCause(t)) {
                rethrow(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static String sanitize(String s) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length() && sb.length() < 24; i++) {
            char ch = s.charAt(i);
            if ((ch >= 'a' && ch <= 'z')
                    || (ch >= 'A' && ch <= 'Z')
                    || (ch >= '0' && ch <= '9')
                    || ch == ' ') {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.indexOf("Validation") >= 0 || name.indexOf("Invalid") >= 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        for (int i = 0; i < stack.length; i++) {
            String cls = stack[i].getClassName();
            String method = stack[i].getMethodName();
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

    private static boolean isOracleFailure(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
    }

    private static void rethrow(Throwable t) {
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
<details open><summary>▸ Output (~3,791 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("force-custom-path", "non-empty");

        exercise("it''s a {0} 'test'!", "dummy", "it's a dummy test!", registry);

        String a = sanitize(data.consumeAsciiString(16));
        String b = sanitize(data.consumeAsciiString(16));
        String c = sanitize(data.consumeAsciiString(16));

        if (a.length() == 0) {
            a = "a";
        }
        if (b.length() == 0) {
            b = "b";
        }
        if (c.length() == 0) {
            c = "c";
        }

        String arg = sanitize(data.consumeAsciiString(12));
        if (arg.length() == 0) {
            arg = "x";
        }

        int variant = data.consumeInt(0, 7);
        String pattern;
        String expected;

        switch (variant) {
            case 0:
                pattern = "''" + a + "{0}" + b;
                expected = "'" + a + arg + b;
                break;
            case 1:
                pattern = a + "''" + b + "{0}" + c;
                expected = a + "'" + b + arg + c;
                break;
            case 2:
                pattern = a + "{0}" + "''" + b;
                expected = a + arg + "'" + b;
                break;
            case 3:
                pattern = a + "''" + "{0}" + "''" + b;
                expected = a + "'" + arg + "'" + b;
                break;
            case 4:
                pattern = a + " {0} '' " + b;
                expected = a + " " + arg + " ' " + b;
                break;
            case 5:
                pattern = a + "''" + b + " {0} 'x' " + c;
                expected = a + "'" + b + " " + arg + " x " + c;
                break;
            case 6:
                pattern = "'" + a + "' " + "''" + " {0} " + b;
                expected = a + " ' " + arg + " " + b;
                break;
            default:
                pattern = a + "''" + b + "''" + c + " {0}";
                expected = a + "'" + b + "'" + c + " " + arg;
                break;
        }

        exercise(pattern, arg, expected, registry);
    }

    private static void exercise(String pattern, String arg, String expected, Map registry) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String actual = emf.format(new Object[] { arg });
        assertEquals(expected, actual, "[oracle:format]");

        // Contract/metamorphic check: rebuilding from the object's own pattern description
        // must preserve formatting behavior for the same argument.
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(emf.toPattern(), registry);
        String actual2 = emf2.format(new Object[] { arg });
        assertEquals(actual, actual2, "[oracle:roundtrip]");
    }

    private static void assertEquals(String expected, String actual, String oracle) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new RuntimeException(
                    oracle + " metamorphic violation: expected=" + expected + " actual=" + actual);
        }
    }

    private static String sanitize(String s) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length() && sb.length() < 16; i++) {
            char ch = s.charAt(i);
            if ((ch >= 'a' && ch <= 'z')
                    || (ch >= 'A' && ch <= 'Z')
                    || (ch >= '0' && ch <= '9')
                    || ch == ' ') {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
}
```

</details>

---
## [16] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Every oracle alarm in your harness needs a name: the alarm near line 89 has no oracle ID (`throw new RuntimeException( oracle + " metamorphic violation: expected=" + expected + " actual=" + actual)...

---
## [17] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (8 message(s), ~4,758 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [11] — not reprinted*
- *[assistant] message: identical to step [13] — not reprinted*
- *[user] message: identical to step [13] — not reprinted*
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
        Map registry = new HashMap();
        registry.put("force-custom-path", "non-empty");

        exercise("it''s a {0} 'test'!", "dummy", "it's a dummy test!", registry);

        String a = sanitize(data.consumeAsciiString(16));
        String b = sanitize(data.consumeAsciiString(16));
        String c = sanitize(data.consumeAsciiString(16));

        if (a.length() == 0) {
            a = "a";
        }
        if (b.length() == 0) {
            b = "b";
        }
        if (c.length() == 0) {
            c = "c";
        }

        String arg = sanitize(data.consumeAsciiString(12));
        if (arg.length() == 0) {
            arg = "x";
        }

        int variant = data.consumeInt(0, 7);
        String pattern;
        String expected;

        switch (variant) {
            case 0:
                pattern = "''" + a + "{0}" + b;
                expected = "'" + a + arg + b;
                break;
            case 1:
                pattern = a + "''" + b + "{0}" + c;
                expected = a + "'" + b + arg + c;
                break;
            case 2:
                pattern = a + "{0}" + "''" + b;
                expected = a + arg + "'" + b;
                break;
            case 3:
                pattern = a + "''" + "{0}" + "''" + b;
                expected = a + "'" + arg + "'" + b;
                break;
            case 4:
                pattern = a + " {0} '' " + b;
                expected = a + " " + arg + " ' " + b;
                break;
            case 5:
                pattern = a + "''" + b + " {0} 'x' " + c;
                expected = a + "'" + b + " " + arg + " x " + c;
                break;
            case 6:
                pattern = "'" + a + "' " + "''" + " {0} " + b;
                expected = a + " ' " + arg + " " + b;
                break;
            default:
                pattern = a + "''" + b + "''" + c + " {0}";
                expected = a + "'" + b + "'" + c + " " + arg;
                break;
        }

        exercise(pattern, arg, expected, registry);
    }

    private static void exercise(String pattern, String arg, String expected, Map registry) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String actual = emf.format(new Object[] { arg });
        assertEquals(expected, actual, "[oracle:format]");

        // Contract/metamorphic check: rebuilding from the object's own pattern description
        // must preserve formatting behavior for the same argument.
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(emf.toPattern(), registry);
        String actual2 = emf2.format(new Object[] { arg });
        assertEquals(actual, actual2, "[oracle:roundtrip]");
    }

    private static void assertEquals(String expected, String actual, String oracle) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new RuntimeException(
                    oracle + " metamorphic violation: expected=" + expected + " actual=" + actual);
        }
    }

    private static String sanitize(String s) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length() && sb.length() < 16; i++) {
            char ch = s.charAt(i);
            if ((ch >= 'a' && ch <= 'z')
                    || (ch >= 'A' && ch <= 'Z')
                    || (ch >= '0' && ch <= '9')
                    || ch == ' ') {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
}
```
**[user]**
```
Every oracle alarm in your harness needs a name: the alarm near line 89 has no oracle ID (`throw new RuntimeException( oracle + " metamorphic violation: expected=" + expected + " actual=" + actual)...`) — start its message with "[oracle:<short-id>]" so acceptance can tell which check earned its place
Start each distinct check's message with [oracle:<short-id>], e.g. `throw new FuzzerSecurityIssueLow("[oracle:round-trip] semantic mismatch: ...")`. Use a DIFFERENT id per check. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~3,631 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("force-custom-path", "non-empty");

        exercise("it''s a {0} 'test'!", "dummy", "it's a dummy test!", registry);

        String a = sanitize(data.consumeAsciiString(16));
        String b = sanitize(data.consumeAsciiString(16));
        String c = sanitize(data.consumeAsciiString(16));

        if (a.length() == 0) {
            a = "a";
        }
        if (b.length() == 0) {
            b = "b";
        }
        if (c.length() == 0) {
            c = "c";
        }

        String arg = sanitize(data.consumeAsciiString(12));
        if (arg.length() == 0) {
            arg = "x";
        }

        int variant = data.consumeInt(0, 7);
        String pattern;
        String expected;

        switch (variant) {
            case 0:
                pattern = "''" + a + "{0}" + b;
                expected = "'" + a + arg + b;
                break;
            case 1:
                pattern = a + "''" + b + "{0}" + c;
                expected = a + "'" + b + arg + c;
                break;
            case 2:
                pattern = a + "{0}" + "''" + b;
                expected = a + arg + "'" + b;
                break;
            case 3:
                pattern = a + "''" + "{0}" + "''" + b;
                expected = a + "'" + arg + "'" + b;
                break;
            case 4:
                pattern = a + " {0} '' " + b;
                expected = a + " " + arg + " ' " + b;
                break;
            case 5:
                pattern = a + "''" + b + " {0} 'x' " + c;
                expected = a + "'" + b + " " + arg + " x " + c;
                break;
            case 6:
                pattern = "'" + a + "' " + "''" + " {0} " + b;
                expected = a + " ' " + arg + " " + b;
                break;
            default:
                pattern = a + "''" + b + "''" + c + " {0}";
                expected = a + "'" + b + "'" + c + " " + arg;
                break;
        }

        exercise(pattern, arg, expected, registry);
    }

    private static void exercise(String pattern, String arg, String expected, Map registry) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String actual = emf.format(new Object[] { arg });
        assertEquals(expected, actual, "format");

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(emf.toPattern(), registry);
        String actual2 = emf2.format(new Object[] { arg });
        assertEquals(actual, actual2, "roundtrip");
    }

    private static void assertEquals(String expected, String actual, String oracleId) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new RuntimeException(
                    "[oracle:" + oracleId + "] metamorphic violation: expected=" + expected + " actual=" + actual);
        }
    }

    private static String sanitize(String s) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length() && sb.length() < 16; i++) {
            char ch = s.charAt(i);
            if ((ch >= 'a' && ch <= 'z')
                    || (ch >= 'A' && ch <= 'Z')
                    || (ch >= '0' && ch <= '9')
                    || ch == ' ') {
                sb.append(ch);
            }
        }
        return sb.toString();
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
<details><summary>▸ Prompt (2 message(s), ~32,042 chars, 1 new)</summary>

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
<details open><summary>▸ Output (~6,480 chars)</summary>

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
        Map registry = buildRegistry();

        runPattern("it''s a {0,lower} 'test'!", "DUMMY", registry, true);

        String a = sanitizeLiteral(data.consumeAsciiString(12));
        String b = sanitizeLiteral(data.consumeAsciiString(12));
        String c = sanitizeLiteral(data.consumeAsciiString(12));
        String quoted = sanitizeQuoted(data.consumeAsciiString(8));
        String arg = data.consumeString(24);

        StringBuilder pattern = new StringBuilder();
        pattern.append(a);
        pattern.append("''");
        if (b.length() > 0) {
            pattern.append(b).append(' ');
        }
        pattern.append("{0,lower}");
        pattern.append(" '").append(quoted).append("'");
        pattern.append(c);

        runPattern(pattern.toString(), arg, registry, false);
    }

    private static void runPattern(String pattern, String arg, Map registry, boolean anchor) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

            String toPattern = emf.toPattern();
            if (!pattern.equals(toPattern)) {
                throw new RuntimeException(
                        "[oracle:toPattern] metamorphic violation: original pattern must round-trip through toPattern "
                                + "input=" + pattern + " toPattern=" + toPattern);
            }

            String lhs = emf.format(new Object[] { arg });

            String equivalentPattern = pattern.replace("{0,lower}", "{0}");
            ExtendedMessageFormat plain = new ExtendedMessageFormat(equivalentPattern);
            String rhs = plain.format(new Object[] { String.valueOf(arg).toLowerCase() });

            if (!lhs.equals(rhs)) {
                throw new RuntimeException(
                        "[oracle:lower-equiv] metamorphic violation: formatting with {0,lower} must match formatting "
                                + "the equivalent plain pattern with a lowercased argument input=" + pattern
                                + " arg=" + arg + " lhs=" + lhs + " rhs=" + rhs);
            }

            if (anchor) {
                String expected = "it's a dummy test!";
                if (!expected.equals(lhs)) {
                    throw new RuntimeException(
                            "[oracle:anchor] metamorphic violation: anchor output mismatch input=" + pattern
                                    + " lhs=" + lhs + " expected=" + expected);
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
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String m = ste.getMethodName();
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

    private static Map buildRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static String sanitizeLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'' || ch == ',') {
                continue;
            }
            if (Character.isISOControl(ch)) {
                continue;
            }
            out.append(ch);
        }
        return out.toString();
    }

    private static String sanitizeQuoted(String s) {
        String v = sanitizeLiteral(s);
        return v.length() == 0 ? "x" : v;
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

        CaseFormat(boolean upper, Locale locale) {
            this.upper = upper;
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            s = upper ? s.toUpperCase(locale) : s.toLowerCase(locale);
            return toAppendTo.append(s);
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
## [20] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [21] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~6,678 chars, 1 new)</summary>

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
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = buildRegistry();

        runPattern("it''s a {0,lower} 'test'!", "DUMMY", registry, true);

        String a = sanitizeLiteral(data.consumeAsciiString(12));
        String b = sanitizeLiteral(data.consumeAsciiString(12));
        String c = sanitizeLiteral(data.consumeAsciiString(12));
        String quoted = sanitizeQuoted(data.consumeAsciiString(8));
        String arg = data.consumeString(24);

        StringBuilder pattern = new StringBuilder();
        pattern.append(a);
        pattern.append("''");
        if (b.length() > 0) {
            pattern.append(b).append(' ');
        }
        pattern.append("{0,lower}");
        pattern.append(" '").append(quoted).append("'");
        pattern.append(c);

        runPattern(pattern.toString(), arg, registry, false);
    }

    private static void runPattern(String pattern, String arg, Map registry, boolean anchor) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

            String toPattern = emf.toPattern();
            if (!pattern.equals(toPattern)) {
                throw new RuntimeException(
                        "[oracle:toPattern] metamorphic violation: original pattern must round-trip through toPattern "
                                + "input=" + pattern + " toPattern=" + toPattern);
            }

            String lhs = emf.format(new Object[] { arg });

            String equivalentPattern = pattern.replace("{0,lower}", "{0}");
            ExtendedMessageFormat plain = new ExtendedMessageFormat(equivalentPattern);
            String rhs = plain.format(new Object[] { String.valueOf(arg).toLowerCase() });

            if (!lhs.equals(rhs)) {
                throw new RuntimeException(
                        "[oracle:lower-equiv] metamorphic violation: formatting with {0,lower} must match formatting "
                                + "the equivalent plain pattern with a lowercased argument input=" + pattern
                                + " arg=" + arg + " lhs=" + lhs + " rhs=" + rhs);
            }

            if (anchor) {
                String expected = "it's a dummy test!";
                if (!expected.equals(lhs)) {
                    throw new RuntimeException(
                            "[oracle:anchor] metamorphic violation: anchor output mismatch input=" + pattern
                                    + " lhs=" + lhs + " expected=" + expected);
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
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String m = ste.getMethodName();
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

    private static Map buildRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static String sanitizeLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'' || ch == ',') {
                continue;
            }
            if (Character.isISOControl(ch)) {
                continue;
            }
            out.append(ch);
        }
        return out.toString();
    }

    private static String sanitizeQuoted(String s) {
        String v = sanitizeLiteral(s);
        return v.length() == 0 ? "x" : v;
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

        CaseFormat(boolean upper, Locale locale) {
            this.upper = upper;
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            s = upper ? s.toUpperCase(locale) : s.toLowerCase(locale);
            return toAppendTo.append(s);
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
<details open><summary>▸ Output (~3,925 chars)</summary>

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
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor] metamorphic violation: pattern=" + anchorPattern + " out=" + anchorOut);
        }
        if (!anchorPattern.equals(anchor.toPattern())) {
            throw new RuntimeException("[oracle:roundtrip] metamorphic violation: pattern=" + anchorPattern + " toPattern=" + anchor.toPattern());
        }

        String prefix = clean(data.consumeAsciiString(16));
        String middle = clean(data.consumeAsciiString(16));
        String suffix = clean(data.consumeAsciiString(16));
        String quoted = cleanQuoted(data.consumeAsciiString(16));
        String arg = data.consumeString(32);

        String pattern = prefix + "''" + middle + "{0,lower} '" + quoted + "'" + suffix;
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String out = emf.format(new Object[] { arg });

        ExtendedMessageFormat plain = new ExtendedMessageFormat(pattern.replace("{0,lower}", "{0}"));
        String expected = plain.format(new Object[] { String.valueOf(arg).toLowerCase() });
        if (!out.equals(expected)) {
            throw new RuntimeException("[oracle:lower-equiv] metamorphic violation: pattern=" + pattern + " out=" + out + " expected=" + expected);
        }
    }

    private static String clean(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'' || ch == ',') {
                continue;
            }
            if (Character.isISOControl(ch)) {
                continue;
            }
            b.append(ch);
        }
        return b.toString();
    }

    private static String cleanQuoted(String s) {
        String v = clean(s);
        return v.length() == 0 ? "x" : v;
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

        CaseFormat(boolean upper, Locale locale) {
            this.upper = upper;
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            return toAppendTo.append(upper ? s.toUpperCase(locale) : s.toLowerCase(locale));
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
## [22] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [23] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~4,243 chars, 1 new)</summary>

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
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, registry);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor] metamorphic violation: pattern=" + anchorPattern + " out=" + anchorOut);
        }
        if (!anchorPattern.equals(anchor.toPattern())) {
            throw new RuntimeException("[oracle:roundtrip] metamorphic violation: pattern=" + anchorPattern + " toPattern=" + anchor.toPattern());
        }

        String prefix = clean(data.consumeAsciiString(16));
        String middle = clean(data.consumeAsciiString(16));
        String suffix = clean(data.consumeAsciiString(16));
        String quoted = cleanQuoted(data.consumeAsciiString(16));
        String arg = data.consumeString(32);

        String pattern = prefix + "''" + middle + "{0,lower} '" + quoted + "'" + suffix;
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String out = emf.format(new Object[] { arg });

        ExtendedMessageFormat plain = new ExtendedMessageFormat(pattern.replace("{0,lower}", "{0}"));
        String expected = plain.format(new Object[] { String.valueOf(arg).toLowerCase() });
        if (!out.equals(expected)) {
            throw new RuntimeException("[oracle:lower-equiv] metamorphic violation: pattern=" + pattern + " out=" + out + " expected=" + expected);
        }
    }

    private static String clean(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'' || ch == ',') {
                continue;
            }
            if (Character.isISOControl(ch)) {
                continue;
            }
            b.append(ch);
        }
        return b.toString();
    }

    private static String cleanQuoted(String s) {
        String v = clean(s);
        return v.length() == 0 ? "x" : v;
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

        CaseFormat(boolean upper, Locale locale) {
            this.upper = upper;
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            return toAppendTo.append(upper ? s.toUpperCase(locale) : s.toLowerCase(locale));
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
<details open><summary>▸ Output (~3,989 chars)</summary>

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

        ExtendedMessageFormat anchor = new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);
        String anchorFormatted = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorFormatted)) {
            throw new RuntimeException("[oracle:anchor] metamorphic violation: got=" + anchorFormatted);
        }
        if (!"it''s a {0,lower} 'test'!".equals(anchor.toPattern())) {
            throw new RuntimeException("[oracle:roundtrip] metamorphic violation: toPattern=" + anchor.toPattern());
        }

        String before = filterLiteral(data.consumeAsciiString(12));
        String between = filterLiteral(data.consumeAsciiString(12));
        String quoted = filterQuoted(data.consumeAsciiString(12));
        String after = filterLiteral(data.consumeAsciiString(12));
        String arg = data.consumeString(24);

        String pattern = before + "''" + between + "{0,lower} '" + quoted + "'" + after;
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String actual = emf.format(new Object[] { arg });

        String equivalent = pattern.replace("{0,lower}", "{0}");
        ExtendedMessageFormat plain = new ExtendedMessageFormat(equivalent);
        String expected = plain.format(new Object[] { String.valueOf(arg).toLowerCase() });

        if (!actual.equals(expected)) {
            throw new RuntimeException(
                "[oracle:lower-equiv] metamorphic violation: pattern=" + pattern
                    + " actual=" + actual + " expected=" + expected);
        }
    }

    private static String filterLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'' || ch == ',') {
                continue;
            }
            if (Character.isISOControl(ch)) {
                continue;
            }
            out.append(ch);
        }
        return out.toString();
    }

    private static String filterQuoted(String s) {
        String v = filterLiteral(s);
        return v.length() == 0 ? "x" : v;
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

        CaseFormat(boolean upper, Locale locale) {
            this.upper = upper;
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            return toAppendTo.append(upper ? s.toUpperCase(locale) : s.toLowerCase(locale));
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
## [24] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [25] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (8 message(s), ~4,427 chars, 1 new)</summary>

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

        ExtendedMessageFormat anchor = new ExtendedMessageFormat("it''s a {0,lower} 'test'!", registry);
        String anchorFormatted = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorFormatted)) {
            throw new RuntimeException("[oracle:anchor] metamorphic violation: got=" + anchorFormatted);
        }
        if (!"it''s a {0,lower} 'test'!".equals(anchor.toPattern())) {
            throw new RuntimeException("[oracle:roundtrip] metamorphic violation: toPattern=" + anchor.toPattern());
        }

        String before = filterLiteral(data.consumeAsciiString(12));
        String between = filterLiteral(data.consumeAsciiString(12));
        String quoted = filterQuoted(data.consumeAsciiString(12));
        String after = filterLiteral(data.consumeAsciiString(12));
        String arg = data.consumeString(24);

        String pattern = before + "''" + between + "{0,lower} '" + quoted + "'" + after;
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String actual = emf.format(new Object[] { arg });

        String equivalent = pattern.replace("{0,lower}", "{0}");
        ExtendedMessageFormat plain = new ExtendedMessageFormat(equivalent);
        String expected = plain.format(new Object[] { String.valueOf(arg).toLowerCase() });

        if (!actual.equals(expected)) {
            throw new RuntimeException(
                "[oracle:lower-equiv] metamorphic violation: pattern=" + pattern
                    + " actual=" + actual + " expected=" + expected);
        }
    }

    private static String filterLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'' || ch == ',') {
                continue;
            }
            if (Character.isISOControl(ch)) {
                continue;
            }
            out.append(ch);
        }
        return out.toString();
    }

    private static String filterQuoted(String s) {
        String v = filterLiteral(s);
        return v.length() == 0 ? "x" : v;
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

        CaseFormat(boolean upper, Locale locale) {
            this.upper = upper;
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            return toAppendTo.append(upper ? s.toUpperCase(locale) : s.toLowerCase(locale));
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
<details open><summary>▸ Output (~4,829 chars)</summary>

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
        Map nonNullRegistry = new HashMap();

        ExtendedMessageFormat direct = new ExtendedMessageFormat("''", nonNullRegistry);
        String directOut = direct.format(new Object[0]);
        String jdkDirectOut = new MessageFormat("''").format(new Object[0]);
        if (!jdkDirectOut.equals(directOut)) {
            throw new RuntimeException(
                    "[oracle:direct-quote] metamorphic violation: pattern='' lhs=" + directOut + " rhs=" + jdkDirectOut);
        }
        if (!"''".equals(direct.toPattern())) {
            throw new RuntimeException(
                    "[oracle:direct-roundtrip] metamorphic violation: pattern='' toPattern=" + direct.toPattern());
        }

        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        String exactPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat exact = new ExtendedMessageFormat(exactPattern, registry);
        String exactOut = exact.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(exactOut)) {
            throw new RuntimeException(
                    "[oracle:anchor] metamorphic violation: pattern=" + exactPattern + " out=" + exactOut);
        }
        if (!exactPattern.equals(exact.toPattern())) {
            throw new RuntimeException(
                    "[oracle:anchor-roundtrip] metamorphic violation: pattern=" + exactPattern + " toPattern=" + exact.toPattern());
        }

        String literal1 = sanitizeLiteral(data.consumeAsciiString(12));
        String literal2 = sanitizeLiteral(data.consumeAsciiString(12));
        String literal3 = sanitizeLiteral(data.consumeAsciiString(12));
        String quoted = sanitizeQuoted(data.consumeAsciiString(12));
        String arg = data.consumeString(24);

        String pattern = "''" + literal1 + "{0,lower}" + literal2 + " '" + quoted + "'" + literal3;
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String actual = emf.format(new Object[] { arg });

        String plainPattern = pattern.replace("{0,lower}", "{0}");
        String expected = new MessageFormat(plainPattern).format(new Object[] { String.valueOf(arg).toLowerCase() });

        if (!expected.equals(actual)) {
            throw new RuntimeException(
                    "[oracle:lower-equiv] metamorphic violation: pattern=" + pattern
                            + " actual=" + actual + " expected=" + expected);
        }
    }

    private static String sanitizeLiteral(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'' || ch == ',') {
                continue;
            }
            if (Character.isISOControl(ch)) {
                continue;
            }
            out.append(ch);
        }
        return out.toString();
    }

    private static String sanitizeQuoted(String s) {
        String v = sanitizeLiteral(s);
        return v.length() == 0 ? "x" : v;
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

        CaseFormat(boolean upper, Locale locale) {
            this.upper = upper;
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            return toAppendTo.append(upper ? s.toUpperCase(locale) : s.toLowerCase(locale));
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
<details open><summary>▸ Output (~4,971 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    private static final Object[] FORMAT_ARGS = new Object[] { "DUMMY" };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        exercise("it''s a {0,lower} 'test'!", registry, false);

        String pattern = buildPattern(data);
        exercise(pattern, registry, true);
    }

    private static String buildPattern(FuzzedDataProvider data) {
        String prefix = sanitize(data.consumeAsciiString(24));
        String mid = sanitize(data.consumeAsciiString(24));
        String suffix = sanitize(data.consumeAsciiString(24));
        int shape = data.consumeInt(0, 4);

        switch (shape) {
            case 0:
                return prefix + "''" + mid + " {0} " + suffix;
            case 1:
                return prefix + "'" + mid + "' {0} " + suffix;
            case 2:
                return prefix + "''" + mid + " '' " + suffix + " {0}";
            case 3:
                return "'" + prefix + "'" + mid + " {0} '" + suffix + "'";
            default:
                return prefix + " {0} '' " + mid + " '" + suffix + "'";
        }
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        return s.replace('{', 'x').replace('}', 'y');
    }

    private static void exercise(String pattern, Map registry, boolean doChecks) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

            if (doChecks) {
                String before = emf.toPattern();

                try {
                    emf.format(FORMAT_ARGS);
                } catch (Throwable t) {
                    if (isCleanRejection(t)) {
                        return;
                    }
                    if (isRootCause(t)) {
                        throw propagate(t);
                    }
                    return;
                }

                String after = emf.toPattern();
                if (!safeEquals(before, after)) {
                    throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: format changed toPattern input=" + pattern + " before=" + before + " after=" + after);
                }

                try {
                    ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, registry);
                    String reparsed = emf2.toPattern();
                    String reparsed2 = emf2.toPattern();
                    if (!safeEquals(before, reparsed) || !safeEquals(reparsed, reparsed2)) {
                        throw new RuntimeException("[oracle:topattern-idempotent] metamorphic violation: canonical toPattern not stable input=" + pattern + " first=" + before + " second=" + reparsed + " third=" + reparsed2);
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

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
                || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String method = ste.getMethodName();
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

    private static RuntimeException propagate(Throwable t) {
        if (t instanceof RuntimeException) {
            return (RuntimeException) t;
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
## [28] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [29] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~5,168 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    private static final Object[] FORMAT_ARGS = new Object[] { "DUMMY" };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        exercise("it''s a {0,lower} 'test'!", registry, false);

        String pattern = buildPattern(data);
        exercise(pattern, registry, true);
    }

    private static String buildPattern(FuzzedDataProvider data) {
        String prefix = sanitize(data.consumeAsciiString(24));
        String mid = sanitize(data.consumeAsciiString(24));
        String suffix = sanitize(data.consumeAsciiString(24));
        int shape = data.consumeInt(0, 4);

        switch (shape) {
            case 0:
                return prefix + "''" + mid + " {0} " + suffix;
            case 1:
                return prefix + "'" + mid + "' {0} " + suffix;
            case 2:
                return prefix + "''" + mid + " '' " + suffix + " {0}";
            case 3:
                return "'" + prefix + "'" + mid + " {0} '" + suffix + "'";
            default:
                return prefix + " {0} '' " + mid + " '" + suffix + "'";
        }
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        return s.replace('{', 'x').replace('}', 'y');
    }

    private static void exercise(String pattern, Map registry, boolean doChecks) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

            if (doChecks) {
                String before = emf.toPattern();

                try {
                    emf.format(FORMAT_ARGS);
                } catch (Throwable t) {
                    if (isCleanRejection(t)) {
                        return;
                    }
                    if (isRootCause(t)) {
                        throw propagate(t);
                    }
                    return;
                }

                String after = emf.toPattern();
                if (!safeEquals(before, after)) {
                    throw new RuntimeException("[oracle:topattern-stable] metamorphic violation: format changed toPattern input=" + pattern + " before=" + before + " after=" + after);
                }

                try {
                    ExtendedMessageFormat emf2 = new ExtendedMessageFormat(before, registry);
                    String reparsed = emf2.toPattern();
                    String reparsed2 = emf2.toPattern();
                    if (!safeEquals(before, reparsed) || !safeEquals(reparsed, reparsed2)) {
                        throw new RuntimeException("[oracle:topattern-idempotent] metamorphic violation: canonical toPattern not stable input=" + pattern + " first=" + before + " second=" + reparsed + " third=" + reparsed2);
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

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
                || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String method = ste.getMethodName();
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

    private static RuntimeException propagate(Throwable t) {
        if (t instanceof RuntimeException) {
            return (RuntimeException) t;
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
<details open><summary>▸ Output (~3,646 chars)</summary>

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
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        ExtendedMessageFormat anchor = new ExtendedMessageFormat("it''s a {0,lower} 'test'!", REGISTRY);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: input=it''s a {0,lower} 'test'! got=" + anchorOut);
        }

        String a = sanitize(data.consumeAsciiString(16));
        String b = sanitize(data.consumeAsciiString(16));
        String c = sanitize(data.consumeAsciiString(16));
        String d = sanitize(data.consumeAsciiString(16));

        String pattern;
        switch (data.consumeInt(0, 3)) {
            case 0:
                pattern = a + "''" + b + " {0,lower} '" + c + "'";
                break;
            case 1:
                pattern = "'" + a + "' " + b + "''" + c + " {0,lower}";
                break;
            case 2:
                pattern = a + " {0,lower} '' " + b + " '" + c + "'";
                break;
            default:
                pattern = a + "''" + b + " {0,lower} '" + c + "' " + d;
                break;
        }

        ExtendedMessageFormat emf1 = new ExtendedMessageFormat(pattern, REGISTRY);
        String out1 = emf1.format(new Object[] { "DuMmY" });
        String canonical = emf1.toPattern();

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(canonical, REGISTRY);
        String out2 = emf2.format(new Object[] { "DuMmY" });

        if (!out1.equals(out2)) {
            throw new RuntimeException("[oracle:roundtrip-format] metamorphic violation: pattern=" + pattern + " canonical=" + canonical + " lhs=" + out1 + " rhs=" + out2);
        }

        String canonical2 = emf2.toPattern();
        if (!canonical.equals(canonical2)) {
            throw new RuntimeException("[oracle:topattern-idempotent] metamorphic violation: pattern=" + pattern + " first=" + canonical + " second=" + canonical2);
        }
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        return s.replace('\'', 'x').replace('{', 'x').replace('}', 'y');
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat(locale);
        }
    }

    private static final class LowerCaseFormat extends Format {
        private final Locale locale;

        private LowerCaseFormat(Locale locale) {
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
            return source.substring(index).toLowerCase(locale);
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
<details><summary>▸ Prompt (6 message(s), ~3,963 chars, 1 new)</summary>

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
    private static final Map REGISTRY = new HashMap();

    static {
        REGISTRY.put("lower", new LowerCaseFormatFactory());
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        ExtendedMessageFormat anchor = new ExtendedMessageFormat("it''s a {0,lower} 'test'!", REGISTRY);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: input=it''s a {0,lower} 'test'! got=" + anchorOut);
        }

        String a = sanitize(data.consumeAsciiString(16));
        String b = sanitize(data.consumeAsciiString(16));
        String c = sanitize(data.consumeAsciiString(16));
        String d = sanitize(data.consumeAsciiString(16));

        String pattern;
        switch (data.consumeInt(0, 3)) {
            case 0:
                pattern = a + "''" + b + " {0,lower} '" + c + "'";
                break;
            case 1:
                pattern = "'" + a + "' " + b + "''" + c + " {0,lower}";
                break;
            case 2:
                pattern = a + " {0,lower} '' " + b + " '" + c + "'";
                break;
            default:
                pattern = a + "''" + b + " {0,lower} '" + c + "' " + d;
                break;
        }

        ExtendedMessageFormat emf1 = new ExtendedMessageFormat(pattern, REGISTRY);
        String out1 = emf1.format(new Object[] { "DuMmY" });
        String canonical = emf1.toPattern();

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(canonical, REGISTRY);
        String out2 = emf2.format(new Object[] { "DuMmY" });

        if (!out1.equals(out2)) {
            throw new RuntimeException("[oracle:roundtrip-format] metamorphic violation: pattern=" + pattern + " canonical=" + canonical + " lhs=" + out1 + " rhs=" + out2);
        }

        String canonical2 = emf2.toPattern();
        if (!canonical.equals(canonical2)) {
            throw new RuntimeException("[oracle:topattern-idempotent] metamorphic violation: pattern=" + pattern + " first=" + canonical + " second=" + canonical2);
        }
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        return s.replace('\'', 'x').replace('{', 'x').replace('}', 'y');
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new LowerCaseFormat(locale);
        }
    }

    private static final class LowerCaseFormat extends Format {
        private final Locale locale;

        private LowerCaseFormat(Locale locale) {
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
            return source.substring(index).toLowerCase(locale);
        }
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~2,465 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        String anchor = "it''s a {0} 'test'!";
        ExtendedMessageFormat anchorFmt = new ExtendedMessageFormat(anchor, registry);
        String anchorOut = anchorFmt.format(new Object[] { "dummy" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: pattern=" + anchor + " output=" + anchorOut);
        }

        String left = sanitize(data.consumeAsciiString(16));
        String middle = sanitize(data.consumeAsciiString(16));
        String right = sanitize(data.consumeAsciiString(16));
        int shape = data.consumeInt(0, 3);

        String pattern;
        switch (shape) {
            case 0:
                pattern = left + "''" + middle + " {0} '" + right + "'";
                break;
            case 1:
                pattern = "'" + left + "' " + middle + "''" + right + " {0}";
                break;
            case 2:
                pattern = left + " {0} '' " + middle + " '" + right + "'";
                break;
            default:
                pattern = left + "''" + middle + right;
                break;
        }

        ExtendedMessageFormat emf1 = new ExtendedMessageFormat(pattern, registry);
        String out1 = emf1.format(new Object[] { "dummy" });
        String canon1 = emf1.toPattern();

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(canon1, registry);
        String out2 = emf2.format(new Object[] { "dummy" });
        String canon2 = emf2.toPattern();

        if (!out1.equals(out2)) {
            throw new RuntimeException("[oracle:format-roundtrip] metamorphic violation: pattern=" + pattern + " canon=" + canon1 + " lhs=" + out1 + " rhs=" + out2);
        }
        if (!canon1.equals(canon2)) {
            throw new RuntimeException("[oracle:topattern-idempotent] metamorphic violation: pattern=" + pattern + " first=" + canon1 + " second=" + canon2);
        }
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        return s.replace('\'', 'x').replace('{', 'x').replace('}', 'y');
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
<details><summary>▸ Prompt (8 message(s), ~2,902 chars, 1 new)</summary>

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
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        String anchor = "it''s a {0} 'test'!";
        ExtendedMessageFormat anchorFmt = new ExtendedMessageFormat(anchor, registry);
        String anchorOut = anchorFmt.format(new Object[] { "dummy" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: pattern=" + anchor + " output=" + anchorOut);
        }

        String left = sanitize(data.consumeAsciiString(16));
        String middle = sanitize(data.consumeAsciiString(16));
        String right = sanitize(data.consumeAsciiString(16));
        int shape = data.consumeInt(0, 3);

        String pattern;
        switch (shape) {
            case 0:
                pattern = left + "''" + middle + " {0} '" + right + "'";
                break;
            case 1:
                pattern = "'" + left + "' " + middle + "''" + right + " {0}";
                break;
            case 2:
                pattern = left + " {0} '' " + middle + " '" + right + "'";
                break;
            default:
                pattern = left + "''" + middle + right;
                break;
        }

        ExtendedMessageFormat emf1 = new ExtendedMessageFormat(pattern, registry);
        String out1 = emf1.format(new Object[] { "dummy" });
        String canon1 = emf1.toPattern();

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(canon1, registry);
        String out2 = emf2.format(new Object[] { "dummy" });
        String canon2 = emf2.toPattern();

        if (!out1.equals(out2)) {
            throw new RuntimeException("[oracle:format-roundtrip] metamorphic violation: pattern=" + pattern + " canon=" + canon1 + " lhs=" + out1 + " rhs=" + out2);
        }
        if (!canon1.equals(canon2)) {
            throw new RuntimeException("[oracle:topattern-idempotent] metamorphic violation: pattern=" + pattern + " first=" + canon1 + " second=" + canon2);
        }
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        return s.replace('\'', 'x').replace('{', 'x').replace('}', 'y');
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~4,958 chars)</summary>

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
        String anchor = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchorEmf = new ExtendedMessageFormat(anchor, REGISTRY);
        String anchorOut = anchorEmf.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: pattern=" + anchor + " output=" + anchorOut);
        }

        String a = clean(data.consumeAsciiString(24));
        String b = clean(data.consumeAsciiString(24));
        String c = clean(data.consumeAsciiString(24));
        String d = clean(data.consumeAsciiString(24));

        String quoteRun = data.consumeBoolean() ? "''" : "'''";
        String literal1 = data.consumeBoolean() ? "'" + a + "'" : a + quoteRun + b;
        String literal2 = data.consumeBoolean() ? "'" + c + "'" : c + quoteRun + d;

        String pattern;
        switch (data.consumeInt(0, 4)) {
            case 0:
                pattern = literal1 + " {0,lower} " + literal2;
                break;
            case 1:
                pattern = a + quoteRun + " {0,upper} '" + b + "'";
                break;
            case 2:
                pattern = "'" + a + "' {0,lower} " + b + quoteRun + c;
                break;
            case 3:
                pattern = a + " {0,lower} " + b + quoteRun + " " + c;
                break;
            default:
                pattern = a + quoteRun + b + " {0,lower} '" + c + "' " + d;
                break;
        }

        ExtendedMessageFormat emf1 = new ExtendedMessageFormat(pattern, REGISTRY);
        String out1 = emf1.format(new Object[] { "DuMmY" });
        String canon = emf1.toPattern();

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(canon, REGISTRY);
        String out2 = emf2.format(new Object[] { "DuMmY" });

        if (!out1.equals(out2)) {
            throw new RuntimeException("[oracle:roundtrip-format] metamorphic violation: pattern=" + pattern + " canon=" + canon + " lhs=" + out1 + " rhs=" + out2);
        }

        String canon2 = emf2.toPattern();
        if (!canon.equals(canon2)) {
            throw new RuntimeException("[oracle:topattern-idempotent] metamorphic violation: pattern=" + pattern + " first=" + canon + " second=" + canon2);
        }
    }

    private static String clean(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        return s.replace('{', 'x').replace('}', 'y');
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

        private LowerCaseFormat(Locale locale) {
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
            return source.substring(start).toLowerCase(locale);
        }
    }

    private static final class UpperCaseFormat extends Format {
        private final Locale locale;

        private UpperCaseFormat(Locale locale) {
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
            return source.substring(start).toUpperCase(locale);
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
<details open><summary>▸ Output (~7,222 chars)</summary>

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

        exercise("it''s a {0,lower} 'test'!", registry, "DUMMY", "it's a dummy test!");

        String arg = data.consumeAsciiString(32);
        if (arg.length() == 0) {
            arg = "DUMMY";
        }

        String a = safeLiteral(data.consumeAsciiString(24));
        String b = safeLiteral(data.consumeAsciiString(24));
        String c = safeLiteral(data.consumeAsciiString(24));
        String d = safeLiteral(data.consumeAsciiString(24));

        int mode = data.consumeInt(0, 5);
        String pattern;
        switch (mode) {
            case 0:
                pattern = "''" + a + "{0,lower}" + b;
                break;
            case 1:
                pattern = a + "''" + b + "{0,lower}" + c;
                break;
            case 2:
                pattern = a + "{0,lower}" + b + "''" + c;
                break;
            case 3:
                pattern = "'" + a + "'" + b + "{0,lower}" + c;
                break;
            case 4:
                pattern = a + "{0,lower}" + b + "'" + c + "'";
                break;
            default:
                pattern = "''" + a + "'" + b + "'" + c + "{0,lower}" + d + "''";
                break;
        }

        exercise(pattern, registry, arg, null);
    }

    private static void exercise(String pattern, Map registry, String arg, String expected) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, Locale.getDefault(), registry);
            String out = emf.format(new Object[] { arg });

            if (expected != null && !expected.equals(out)) {
                throw new RuntimeException("[oracle:anchor-output] metamorphic violation: expected formatted output mismatch input="
                        + pattern + " expected=" + expected + " actual=" + out);
            }

            String reportedPattern = emf.toPattern();

            try {
                ExtendedMessageFormat emf2 = new ExtendedMessageFormat(reportedPattern, Locale.getDefault(), registry);
                String out2 = emf2.format(new Object[] { arg });

                if (!out.equals(out2)) {
                    throw new RuntimeException("[oracle:roundtrip-format] metamorphic violation: ExtendedMessageFormat.toPattern() must recreate an equivalent formatter input="
                            + pattern + " toPattern=" + reportedPattern + " lhs=" + out + " rhs=" + out2);
                }

                String reportedPattern2 = emf2.toPattern();
                if (!reportedPattern.equals(reportedPattern2)) {
                    throw new RuntimeException("[oracle:roundtrip-pattern] consistency violation: two identically constructed formatters must report the same toPattern input="
                            + pattern + " first=" + reportedPattern + " second=" + reportedPattern2);
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
            String cls = ste.getClassName();
            String m = ste.getMethodName();
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cls)
                    && ("appendQuotedString".equals(m) || "next".equals(m) || "startsWith".equals(m) || "append".equals(m))) {
                return true;
            }
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                    && ("append".equals(m) || "startsWith".equals(m))) {
                return true;
            }
        }
        return false;
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
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
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

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new CaseFormat(true, locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new CaseFormat(false, locale);
        }
    }

    private static final class CaseFormat extends Format {
        private final boolean lower;
        private final Locale locale;

        private CaseFormat(boolean lower, Locale locale) {
            this.lower = lower;
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(lower ? s.toLowerCase(locale) : s.toUpperCase(locale));
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
<details><summary>▸ Prompt (4 message(s), ~7,420 chars, 1 new)</summary>

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
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = makeRegistry();

        exercise("it''s a {0,lower} 'test'!", registry, "DUMMY", "it's a dummy test!");

        String arg = data.consumeAsciiString(32);
        if (arg.length() == 0) {
            arg = "DUMMY";
        }

        String a = safeLiteral(data.consumeAsciiString(24));
        String b = safeLiteral(data.consumeAsciiString(24));
        String c = safeLiteral(data.consumeAsciiString(24));
        String d = safeLiteral(data.consumeAsciiString(24));

        int mode = data.consumeInt(0, 5);
        String pattern;
        switch (mode) {
            case 0:
                pattern = "''" + a + "{0,lower}" + b;
                break;
            case 1:
                pattern = a + "''" + b + "{0,lower}" + c;
                break;
            case 2:
                pattern = a + "{0,lower}" + b + "''" + c;
                break;
            case 3:
                pattern = "'" + a + "'" + b + "{0,lower}" + c;
                break;
            case 4:
                pattern = a + "{0,lower}" + b + "'" + c + "'";
                break;
            default:
                pattern = "''" + a + "'" + b + "'" + c + "{0,lower}" + d + "''";
                break;
        }

        exercise(pattern, registry, arg, null);
    }

    private static void exercise(String pattern, Map registry, String arg, String expected) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, Locale.getDefault(), registry);
            String out = emf.format(new Object[] { arg });

            if (expected != null && !expected.equals(out)) {
                throw new RuntimeException("[oracle:anchor-output] metamorphic violation: expected formatted output mismatch input="
                        + pattern + " expected=" + expected + " actual=" + out);
            }

            String reportedPattern = emf.toPattern();

            try {
                ExtendedMessageFormat emf2 = new ExtendedMessageFormat(reportedPattern, Locale.getDefault(), registry);
                String out2 = emf2.format(new Object[] { arg });

                if (!out.equals(out2)) {
                    throw new RuntimeException("[oracle:roundtrip-format] metamorphic violation: ExtendedMessageFormat.toPattern() must recreate an equivalent formatter input="
                            + pattern + " toPattern=" + reportedPattern + " lhs=" + out + " rhs=" + out2);
                }

                String reportedPattern2 = emf2.toPattern();
                if (!reportedPattern.equals(reportedPattern2)) {
                    throw new RuntimeException("[oracle:roundtrip-pattern] consistency violation: two identically constructed formatters must report the same toPattern input="
                            + pattern + " first=" + reportedPattern + " second=" + reportedPattern2);
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
            String cls = ste.getClassName();
            String m = ste.getMethodName();
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cls)
                    && ("appendQuotedString".equals(m) || "next".equals(m) || "startsWith".equals(m) || "append".equals(m))) {
                return true;
            }
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                    && ("append".equals(m) || "startsWith".equals(m))) {
                return true;
            }
        }
        return false;
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
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
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

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new CaseFormat(true, locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new CaseFormat(false, locale);
        }
    }

    private static final class CaseFormat extends Format {
        private final boolean lower;
        private final Locale locale;

        private CaseFormat(boolean lower, Locale locale) {
            this.lower = lower;
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(lower ? s.toLowerCase(locale) : s.toUpperCase(locale));
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
<details open><summary>▸ Output (~5,467 chars)</summary>

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

        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, registry);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: input=" + anchorPattern
                    + " expected=it's a dummy test! actual=" + anchorOut);
        }
        String anchorRoundTrip = new ExtendedMessageFormat(anchor.toPattern(), registry).format(new Object[] { "DUMMY" });
        if (!anchorOut.equals(anchorRoundTrip)) {
            throw new RuntimeException("[oracle:anchor-roundtrip] metamorphic violation: input=" + anchorPattern
                    + " lhs=" + anchorOut + " rhs=" + anchorRoundTrip);
        }

        String arg = data.consumeAsciiString(32);
        if (arg.length() == 0) {
            arg = "DUMMY";
        }

        String a = sanitize(data.consumeAsciiString(20));
        String b = sanitize(data.consumeAsciiString(20));
        String c = sanitize(data.consumeAsciiString(20));
        String d = sanitize(data.consumeAsciiString(20));

        String pattern;
        switch (data.consumeInt(0, 7)) {
            case 0:
                pattern = a + "''" + b + "{0,lower}" + c;
                break;
            case 1:
                pattern = a + "{0,lower}" + b + "''" + c;
                break;
            case 2:
                pattern = "''" + a + "{0,lower}" + b;
                break;
            case 3:
                pattern = a + "''" + b + " {0,upper} " + c;
                break;
            case 4:
                pattern = a + " 'Q' " + b + "''" + c + "{0,lower}" + d;
                break;
            case 5:
                pattern = a + "{0,lower}" + b + " 'X' " + c + "''" + d;
                break;
            case 6:
                pattern = a + "''" + b + "''" + c + "{0,lower}" + d;
                break;
            default:
                pattern = a + "{0,lower}" + b + "''" + c + "''" + d;
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String out = emf.format(new Object[] { arg });

        String reparsedPattern = emf.toPattern();
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(reparsedPattern, registry);
        String out2 = emf2.format(new Object[] { arg });

        if (!out.equals(out2)) {
            throw new RuntimeException("[oracle:roundtrip-format] metamorphic violation: input=" + pattern
                    + " toPattern=" + reparsedPattern + " lhs=" + out + " rhs=" + out2);
        }
        if (!reparsedPattern.equals(emf2.toPattern())) {
            throw new RuntimeException("[oracle:roundtrip-pattern] metamorphic violation: input=" + pattern
                    + " first=" + reparsedPattern + " second=" + emf2.toPattern());
        }
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
            } else if (ch == '\'') {
                sb.append('q');
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
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new CaseFormat(true, locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new CaseFormat(false, locale);
        }
    }

    private static final class CaseFormat extends Format {
        private final boolean lower;
        private final Locale locale;

        private CaseFormat(boolean lower, Locale locale) {
            this.lower = lower;
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(lower ? s.toLowerCase(locale) : s.toUpperCase(locale));
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
## [38] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [39] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~5,785 chars, 1 new)</summary>

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
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = makeRegistry();

        String anchorPattern = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchor = new ExtendedMessageFormat(anchorPattern, registry);
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: input=" + anchorPattern
                    + " expected=it's a dummy test! actual=" + anchorOut);
        }
        String anchorRoundTrip = new ExtendedMessageFormat(anchor.toPattern(), registry).format(new Object[] { "DUMMY" });
        if (!anchorOut.equals(anchorRoundTrip)) {
            throw new RuntimeException("[oracle:anchor-roundtrip] metamorphic violation: input=" + anchorPattern
                    + " lhs=" + anchorOut + " rhs=" + anchorRoundTrip);
        }

        String arg = data.consumeAsciiString(32);
        if (arg.length() == 0) {
            arg = "DUMMY";
        }

        String a = sanitize(data.consumeAsciiString(20));
        String b = sanitize(data.consumeAsciiString(20));
        String c = sanitize(data.consumeAsciiString(20));
        String d = sanitize(data.consumeAsciiString(20));

        String pattern;
        switch (data.consumeInt(0, 7)) {
            case 0:
                pattern = a + "''" + b + "{0,lower}" + c;
                break;
            case 1:
                pattern = a + "{0,lower}" + b + "''" + c;
                break;
            case 2:
                pattern = "''" + a + "{0,lower}" + b;
                break;
            case 3:
                pattern = a + "''" + b + " {0,upper} " + c;
                break;
            case 4:
                pattern = a + " 'Q' " + b + "''" + c + "{0,lower}" + d;
                break;
            case 5:
                pattern = a + "{0,lower}" + b + " 'X' " + c + "''" + d;
                break;
            case 6:
                pattern = a + "''" + b + "''" + c + "{0,lower}" + d;
                break;
            default:
                pattern = a + "{0,lower}" + b + "''" + c + "''" + d;
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String out = emf.format(new Object[] { arg });

        String reparsedPattern = emf.toPattern();
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(reparsedPattern, registry);
        String out2 = emf2.format(new Object[] { arg });

        if (!out.equals(out2)) {
            throw new RuntimeException("[oracle:roundtrip-format] metamorphic violation: input=" + pattern
                    + " toPattern=" + reparsedPattern + " lhs=" + out + " rhs=" + out2);
        }
        if (!reparsedPattern.equals(emf2.toPattern())) {
            throw new RuntimeException("[oracle:roundtrip-pattern] metamorphic violation: input=" + pattern
                    + " first=" + reparsedPattern + " second=" + emf2.toPattern());
        }
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
            } else if (ch == '\'') {
                sb.append('q');
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
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new CaseFormat(true, locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new CaseFormat(false, locale);
        }
    }

    private static final class CaseFormat extends Format {
        private final boolean lower;
        private final Locale locale;

        private CaseFormat(boolean lower, Locale locale) {
            this.lower = lower;
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(lower ? s.toLowerCase(locale) : s.toUpperCase(locale));
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
<details open><summary>▸ Output (~5,317 chars)</summary>

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

        String anchor = "it''s a {0,lower} 'test'!";

        ExtendedMessageFormat emfAnchor = new ExtendedMessageFormat("");
        emfAnchor.setRegistry(registry);
        emfAnchor.applyPattern(anchor);
        String anchorOut = emfAnchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: expected=it's a dummy test! actual=" + anchorOut);
        }

        String roundTripPattern = emfAnchor.toPattern();
        ExtendedMessageFormat emfAnchor2 = new ExtendedMessageFormat("");
        emfAnchor2.setRegistry(registry);
        emfAnchor2.applyPattern(roundTripPattern);
        String anchorOut2 = emfAnchor2.format(new Object[] { "DUMMY" });
        if (!anchorOut.equals(anchorOut2)) {
            throw new RuntimeException("[oracle:anchor-roundtrip] metamorphic violation: lhs=" + anchorOut + " rhs=" + anchorOut2 + " pattern=" + roundTripPattern);
        }

        String left = sanitize(data.consumeAsciiString(16));
        String mid = sanitize(data.consumeAsciiString(16));
        String right = sanitize(data.consumeAsciiString(16));
        String tail = sanitize(data.consumeAsciiString(16));
        String arg = data.consumeAsciiString(24);
        if (arg.length() == 0) {
            arg = "DUMMY";
        }

        String pattern;
        switch (data.consumeInt(0, 5)) {
            case 0:
                pattern = left + "''" + mid + "{0,lower}" + right;
                break;
            case 1:
                pattern = left + "{0,lower}" + mid + "''" + right;
                break;
            case 2:
                pattern = "it''s " + left + "{0,lower}" + right;
                break;
            case 3:
                pattern = left + "''" + mid + " {0,upper} '" + right + "'";
                break;
            case 4:
                pattern = "'" + left + "'" + mid + "{0,lower}" + right + "''" + tail;
                break;
            default:
                pattern = left + "{0,lower}" + mid + " '" + right + "' " + tail + "''";
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat("");
        emf.setRegistry(registry);
        emf.applyPattern(pattern);
        String out = emf.format(new Object[] { arg });
        String tp = emf.toPattern();

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat("");
        emf2.setRegistry(registry);
        emf2.applyPattern(tp);
        String out2 = emf2.format(new Object[] { arg });

        if (!out.equals(out2)) {
            throw new RuntimeException("[oracle:roundtrip-format] metamorphic violation: input=" + pattern + " toPattern=" + tp + " lhs=" + out + " rhs=" + out2);
        }
        if (!tp.equals(emf2.toPattern())) {
            throw new RuntimeException("[oracle:roundtrip-pattern] metamorphic violation: input=" + pattern + " first=" + tp + " second=" + emf2.toPattern());
        }
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
            } else if (ch == '\'') {
                sb.append('q');
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new CaseFormat(true, locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new CaseFormat(false, locale);
        }
    }

    private static final class CaseFormat extends Format {
        private final boolean lower;
        private final Locale locale;

        private CaseFormat(boolean lower, Locale locale) {
            this.lower = lower;
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(lower ? s.toLowerCase(locale) : s.toUpperCase(locale));
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
- reason: That did not compile. javac reported:

---
## [41] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (8 message(s), ~8,092 chars, 2 new)</summary>

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

        String anchor = "it''s a {0,lower} 'test'!";

        ExtendedMessageFormat emfAnchor = new ExtendedMessageFormat("");
        emfAnchor.setRegistry(registry);
        emfAnchor.applyPattern(anchor);
        String anchorOut = emfAnchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: expected=it's a dummy test! actual=" + anchorOut);
        }

        String roundTripPattern = emfAnchor.toPattern();
        ExtendedMessageFormat emfAnchor2 = new ExtendedMessageFormat("");
        emfAnchor2.setRegistry(registry);
        emfAnchor2.applyPattern(roundTripPattern);
        String anchorOut2 = emfAnchor2.format(new Object[] { "DUMMY" });
        if (!anchorOut.equals(anchorOut2)) {
            throw new RuntimeException("[oracle:anchor-roundtrip] metamorphic violation: lhs=" + anchorOut + " rhs=" + anchorOut2 + " pattern=" + roundTripPattern);
        }

        String left = sanitize(data.consumeAsciiString(16));
        String mid = sanitize(data.consumeAsciiString(16));
        String right = sanitize(data.consumeAsciiString(16));
        String tail = sanitize(data.consumeAsciiString(16));
        String arg = data.consumeAsciiString(24);
        if (arg.length() == 0) {
            arg = "DUMMY";
        }

        String pattern;
        switch (data.consumeInt(0, 5)) {
            case 0:
                pattern = left + "''" + mid + "{0,lower}" + right;
                break;
            case 1:
                pattern = left + "{0,lower}" + mid + "''" + right;
                break;
            case 2:
                pattern = "it''s " + left + "{0,lower}" + right;
                break;
            case 3:
                pattern = left + "''" + mid + " {0,upper} '" + right + "'";
                break;
            case 4:
                pattern = "'" + left + "'" + mid + "{0,lower}" + right + "''" + tail;
                break;
            default:
                pattern = left + "{0,lower}" + mid + " '" + right + "' " + tail + "''";
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat("");
        emf.setRegistry(registry);
        emf.applyPattern(pattern);
        String out = emf.format(new Object[] { arg });
        String tp = emf.toPattern();

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat("");
        emf2.setRegistry(registry);
        emf2.applyPattern(tp);
        String out2 = emf2.format(new Object[] { arg });

        if (!out.equals(out2)) {
            throw new RuntimeException("[oracle:roundtrip-format] metamorphic violation: input=" + pattern + " toPattern=" + tp + " lhs=" + out + " rhs=" + out2);
        }
        if (!tp.equals(emf2.toPattern())) {
            throw new RuntimeException("[oracle:roundtrip-pattern] metamorphic violation: input=" + pattern + " first=" + tp + " second=" + emf2.toPattern());
        }
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
            } else if (ch == '\'') {
                sb.append('q');
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new CaseFormat(true, locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new CaseFormat(false, locale);
        }
    }

    private static final class CaseFormat extends Format {
        private final boolean lower;
        private final Locale locale;

        private CaseFormat(boolean lower, Locale locale) {
            this.lower = lower;
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(lower ? s.toLowerCase(locale) : s.toUpperCase(locale));
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
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/Lang_43_buggy/fuzz/attempt_019/FuzzHarness.java:20: error: cannot find symbol
        emfAnchor.setRegistry(registry);
                 ^
  symbol:   method setRegistry(Map)
  location: variable emfAnchor of type ExtendedMessageFormat
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/Lang_43_buggy/fuzz/attempt_019/FuzzHarness.java:29: error: cannot find symbol
        emfAnchor2.setRegistry(registry);
                  ^
  symbol:   method setRegistry(Map)
  location: variable emfAnchor2 of type ExtendedMessageFormat
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/Lang_43_buggy/fuzz/attempt_019/FuzzHarness.java:68: error: cannot find symbol
        emf.setRegistry(registry);
           ^
  symbol:   method setRegistry(Map)
  location: variable emf of type ExtendedMessageFormat
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/Lang_43_buggy/fuzz/attempt_019/FuzzHarness.java:74: error: cannot find symbol
        emf2.setRegistry(registry);
            ^
  symbol:   method setRegistry(Map)
  location: variable emf2 of type ExtendedMessageFormat
Note: /datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/Lang_43_buggy/fuzz/attempt_019/FuzzHarness.java uses unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
4 errors


Return the full corrected FuzzHarness.java. Rules:
- Raw Java source only. No markdown fences. No prose. No explanations. The file starts with a comment or package statement and ends with a closing brace.
- Public class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Use only the FuzzedDataProvider methods listed in the original instructions. Do NOT invent methods like getInt(), consumeDouble(), getRemainingSize(), or consumeIntInRange() — they do not exist.
- Do NOT use classes or methods that are not on the project classpath. If javac says 'cannot find symbol', remove that import and use only classes visible in the source_imports block or java.* / java.awt.*.
```

</details>
<details open><summary>▸ Output (~5,376 chars)</summary>

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
        String anchorOut = anchor.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: pattern=" + anchorPattern + " actual=" + anchorOut);
        }

        String anchorPattern2 = anchor.toPattern();
        ExtendedMessageFormat anchor2 = new ExtendedMessageFormat(anchorPattern2, registry);
        String anchorOut2 = anchor2.format(new Object[] { "DUMMY" });
        if (!anchorOut.equals(anchorOut2)) {
            throw new RuntimeException("[oracle:anchor-roundtrip] metamorphic violation: pattern=" + anchorPattern2 + " lhs=" + anchorOut + " rhs=" + anchorOut2);
        }

        String p1 = sanitize(data.consumeAsciiString(12));
        String p2 = sanitize(data.consumeAsciiString(12));
        String p3 = sanitize(data.consumeAsciiString(12));
        String p4 = sanitize(data.consumeAsciiString(12));
        String arg = data.consumeAsciiString(24);
        if (arg.length() == 0) {
            arg = "DuMmY";
        }

        String pattern;
        switch (data.consumeInt(0, 7)) {
            case 0:
                pattern = p1 + "''" + p2 + "{0,lower}" + p3;
                break;
            case 1:
                pattern = p1 + "{0,lower}" + p2 + "''" + p3;
                break;
            case 2:
                pattern = "it''s " + p1 + "{0,lower} " + p2;
                break;
            case 3:
                pattern = p1 + "''" + p2 + " {0,upper} " + p3;
                break;
            case 4:
                pattern = p1 + "''" + p2 + " '" + p3 + "' {0,lower}";
                break;
            case 5:
                pattern = "'" + p1 + "' " + p2 + "''" + p3 + " {0,lower} " + p4;
                break;
            case 6:
                pattern = p1 + "{0,lower} '" + p2 + "' " + p3 + "''" + p4;
                break;
            default:
                pattern = p1 + "''" + p2 + "''" + p3 + "{0,lower}" + p4;
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String out = emf.format(new Object[] { arg });
        String toPattern = emf.toPattern();

        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(toPattern, registry);
        String out2 = emf2.format(new Object[] { arg });
        if (!out.equals(out2)) {
            throw new RuntimeException("[oracle:roundtrip-format] metamorphic violation: input=" + pattern + " toPattern=" + toPattern + " lhs=" + out + " rhs=" + out2);
        }

        String toPattern2 = emf2.toPattern();
        if (!toPattern.equals(toPattern2)) {
            throw new RuntimeException("[oracle:roundtrip-pattern] metamorphic violation: input=" + pattern + " first=" + toPattern + " second=" + toPattern2);
        }
    }

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "x";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == ',') {
                sb.append('x');
            } else if (ch == '\'') {
                sb.append('q');
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
            return new CaseFormat(true, locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new CaseFormat(false, locale);
        }
    }

    private static final class CaseFormat extends Format {
        private final boolean lower;
        private final Locale locale;

        private CaseFormat(boolean lower, Locale locale) {
            this.lower = lower;
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(lower ? s.toLowerCase(this.locale) : s.toUpperCase(this.locale));
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
<details open><summary>▸ Output (~6,926 chars)</summary>

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
        Map registry = buildRegistry();

        exercisePattern(ANCHOR_PATTERN, registry, true);

        String fmt = data.consumeBoolean() ? "lower" : "upper";
        String left = safeLiteral(data.consumeAsciiString(24));
        String right = safeLiteral(data.consumeAsciiString(24));
        String quoted = safeLiteral(data.consumeAsciiString(16));
        String mid = safeLiteral(data.consumeAsciiString(16));

        StringBuilder pattern = new StringBuilder();
        pattern.append(left);
        pattern.append("it''s");
        if (mid.length() > 0) {
            pattern.append(' ').append(mid);
        }
        pattern.append(" {0,").append(fmt).append("} ");
        pattern.append('\'').append(quoted).append('\'');
        if (right.length() > 0) {
            pattern.append(' ').append(right);
        }

        exercisePattern(pattern.toString(), registry, false);
    }

    private static void exercisePattern(String pattern, Map registry, boolean anchor) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
            String toPattern1 = emf.toPattern();

            /* Contract asserted: ExtendedMessageFormat stores a pattern representation via toPattern;
             * reparsing that representation with the same registry must preserve it. A "fix" that merely
             * skips the quoted-string bookkeeping would make this observable round-trip unstable. */
            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(toPattern1, registry);
            String toPattern2 = emf2.toPattern();
            if (!toPattern1.equals(toPattern2)) {
                throw new RuntimeException(
                        "[oracle:topattern-idem] metamorphic violation: input=" + pattern
                                + " lhs=" + toPattern1 + " rhs=" + toPattern2);
            }

            String sample = anchor ? "DUMMY" : "DuMmY";
            String out1 = emf.format(new Object[] { sample });
            String out2 = emf2.format(new Object[] { sample });

            /* Contract asserted: reparsing an object's own toPattern with the same registry describes
             * an equivalent formatter, so formatting the same argument must yield the same result. */
            if (!out1.equals(out2)) {
                throw new RuntimeException(
                        "[oracle:format-roundtrip] metamorphic violation: input=" + pattern
                                + " lhs=" + out1 + " rhs=" + out2);
            }

            if (anchor) {
                String expected = "it's a dummy test!";
                if (!expected.equals(out1)) {
                    throw new RuntimeException(
                            "[oracle:anchor-output] metamorphic violation: input=" + pattern
                                    + " lhs=" + out1 + " rhs=" + expected);
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
            String name = cur.getClass().getName();
            if (name.contains("Validation") || name.contains("Invalid") || name.contains("Format")) {
                if (cur instanceof RuntimeException || cur instanceof Exception) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String method = ste.getMethodName();
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cls)
                    && ("appendQuotedString".equals(method) || "next".equals(method))) {
                return true;
            }
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                    && ("append".equals(method) || "startsWith".equals(method))) {
                return true;
            }
        }
        return false;
    }

    private static String safeLiteral(String s) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length() && out.length() < 32; i++) {
            char c = s.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
                out.append(c);
            } else if (c == ' ' || c == '_' || c == '-') {
                out.append(' ');
            }
        }
        if (out.length() == 0) {
            out.append('x');
        }
        return out.toString();
    }

    private static Map buildRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new CaseFormat(true, locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new CaseFormat(false, locale);
        }
    }

    private static final class CaseFormat extends Format {
        private static final long serialVersionUID = 1L;
        private final boolean lower;
        private final Locale locale;

        private CaseFormat(boolean lower, Locale locale) {
            this.lower = lower;
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            return toAppendTo.append(lower ? s.toLowerCase(locale) : s.toUpperCase(locale));
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
## [44] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [45] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~7,124 chars, 1 new)</summary>

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
    private static final String ANCHOR_PATTERN = "it''s a {0,lower} 'test'!";

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = buildRegistry();

        exercisePattern(ANCHOR_PATTERN, registry, true);

        String fmt = data.consumeBoolean() ? "lower" : "upper";
        String left = safeLiteral(data.consumeAsciiString(24));
        String right = safeLiteral(data.consumeAsciiString(24));
        String quoted = safeLiteral(data.consumeAsciiString(16));
        String mid = safeLiteral(data.consumeAsciiString(16));

        StringBuilder pattern = new StringBuilder();
        pattern.append(left);
        pattern.append("it''s");
        if (mid.length() > 0) {
            pattern.append(' ').append(mid);
        }
        pattern.append(" {0,").append(fmt).append("} ");
        pattern.append('\'').append(quoted).append('\'');
        if (right.length() > 0) {
            pattern.append(' ').append(right);
        }

        exercisePattern(pattern.toString(), registry, false);
    }

    private static void exercisePattern(String pattern, Map registry, boolean anchor) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
            String toPattern1 = emf.toPattern();

            /* Contract asserted: ExtendedMessageFormat stores a pattern representation via toPattern;
             * reparsing that representation with the same registry must preserve it. A "fix" that merely
             * skips the quoted-string bookkeeping would make this observable round-trip unstable. */
            ExtendedMessageFormat emf2 = new ExtendedMessageFormat(toPattern1, registry);
            String toPattern2 = emf2.toPattern();
            if (!toPattern1.equals(toPattern2)) {
                throw new RuntimeException(
                        "[oracle:topattern-idem] metamorphic violation: input=" + pattern
                                + " lhs=" + toPattern1 + " rhs=" + toPattern2);
            }

            String sample = anchor ? "DUMMY" : "DuMmY";
            String out1 = emf.format(new Object[] { sample });
            String out2 = emf2.format(new Object[] { sample });

            /* Contract asserted: reparsing an object's own toPattern with the same registry describes
             * an equivalent formatter, so formatting the same argument must yield the same result. */
            if (!out1.equals(out2)) {
                throw new RuntimeException(
                        "[oracle:format-roundtrip] metamorphic violation: input=" + pattern
                                + " lhs=" + out1 + " rhs=" + out2);
            }

            if (anchor) {
                String expected = "it's a dummy test!";
                if (!expected.equals(out1)) {
                    throw new RuntimeException(
                            "[oracle:anchor-output] metamorphic violation: input=" + pattern
                                    + " lhs=" + out1 + " rhs=" + expected);
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
            String name = cur.getClass().getName();
            if (name.contains("Validation") || name.contains("Invalid") || name.contains("Format")) {
                if (cur instanceof RuntimeException || cur instanceof Exception) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String method = ste.getMethodName();
            if ("org.apache.commons.lang.text.ExtendedMessageFormat".equals(cls)
                    && ("appendQuotedString".equals(method) || "next".equals(method))) {
                return true;
            }
            if ("org.apache.commons.lang.text.StrBuilder".equals(cls)
                    && ("append".equals(method) || "startsWith".equals(method))) {
                return true;
            }
        }
        return false;
    }

    private static String safeLiteral(String s) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length() && out.length() < 32; i++) {
            char c = s.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
                out.append(c);
            } else if (c == ' ' || c == '_' || c == '-') {
                out.append(' ');
            }
        }
        if (out.length() == 0) {
            out.append('x');
        }
        return out.toString();
    }

    private static Map buildRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new CaseFormat(true, locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new CaseFormat(false, locale);
        }
    }

    private static final class CaseFormat extends Format {
        private static final long serialVersionUID = 1L;
        private final boolean lower;
        private final Locale locale;

        private CaseFormat(boolean lower, Locale locale) {
            this.lower = lower;
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            return toAppendTo.append(lower ? s.toLowerCase(locale) : s.toUpperCase(locale));
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
<details open><summary>▸ Output (~3,425 chars)</summary>

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
    private static final String ANCHOR = "it''s a {0,lower} 'test'!";

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        trigger(ANCHOR);

        String prefix = lettersOnly(data.consumeAsciiString(20));
        String suffix = lettersOnly(data.consumeAsciiString(20));
        String quoted = lettersOnly(data.consumeAsciiString(20));
        String fmt = data.consumeBoolean() ? "lower" : "upper";

        String pattern = prefix + "it''s " + suffix + " {0," + fmt + "} '" + quoted + "'!";
        trigger(pattern);
    }

    private static void trigger(String pattern) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);
        emf.format(new Object[] { "DUMMY" });

        String roundTrip = emf.toPattern();
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(roundTrip, REGISTRY);
        String a = emf.format(new Object[] { "DuMmY" });
        String b = emf2.format(new Object[] { "DuMmY" });
        if (!a.equals(b)) {
            throw new RuntimeException("[oracle:roundtrip] metamorphic violation: pattern=" + pattern + " lhs=" + a + " rhs=" + b);
        }
    }

    private static String lettersOnly(String in) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < in.length() && sb.length() < 20; i++) {
            char c = in.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                sb.append(c);
            }
        }
        if (sb.length() == 0) {
            sb.append('x');
        }
        return sb.toString();
    }

    private static Map buildRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new CaseFormat(true, locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new CaseFormat(false, locale);
        }
    }

    private static final class CaseFormat extends Format {
        private static final long serialVersionUID = 1L;
        private final boolean lower;
        private final Locale locale;

        private CaseFormat(boolean lower, Locale locale) {
            this.lower = lower;
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            return toAppendTo.append(lower ? s.toLowerCase(locale) : s.toUpperCase(locale));
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
## [46] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [47] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~3,743 chars, 1 new)</summary>

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
    private static final Map REGISTRY = buildRegistry();
    private static final String ANCHOR = "it''s a {0,lower} 'test'!";

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        trigger(ANCHOR);

        String prefix = lettersOnly(data.consumeAsciiString(20));
        String suffix = lettersOnly(data.consumeAsciiString(20));
        String quoted = lettersOnly(data.consumeAsciiString(20));
        String fmt = data.consumeBoolean() ? "lower" : "upper";

        String pattern = prefix + "it''s " + suffix + " {0," + fmt + "} '" + quoted + "'!";
        trigger(pattern);
    }

    private static void trigger(String pattern) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);
        emf.format(new Object[] { "DUMMY" });

        String roundTrip = emf.toPattern();
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(roundTrip, REGISTRY);
        String a = emf.format(new Object[] { "DuMmY" });
        String b = emf2.format(new Object[] { "DuMmY" });
        if (!a.equals(b)) {
            throw new RuntimeException("[oracle:roundtrip] metamorphic violation: pattern=" + pattern + " lhs=" + a + " rhs=" + b);
        }
    }

    private static String lettersOnly(String in) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < in.length() && sb.length() < 20; i++) {
            char c = in.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                sb.append(c);
            }
        }
        if (sb.length() == 0) {
            sb.append('x');
        }
        return sb.toString();
    }

    private static Map buildRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new CaseFormat(true, locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new CaseFormat(false, locale);
        }
    }

    private static final class CaseFormat extends Format {
        private static final long serialVersionUID = 1L;
        private final boolean lower;
        private final Locale locale;

        private CaseFormat(boolean lower, Locale locale) {
            this.lower = lower;
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            return toAppendTo.append(lower ? s.toLowerCase(locale) : s.toUpperCase(locale));
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
<details open><summary>▸ Output (~2,356 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    private static final Map NON_NULL_REGISTRY = new HashMap();
    private static final String ANCHOR_PATTERN = "it''s a {0} 'test'!";

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        trigger(ANCHOR_PATTERN);

        String left = sanitize(data.consumeAsciiString(24));
        String right = sanitize(data.consumeAsciiString(24));
        String quoted = sanitize(data.consumeAsciiString(24));
        boolean putQuoteNearStart = data.consumeBoolean();

        StringBuilder pattern = new StringBuilder();
        if (putQuoteNearStart) {
            pattern.append(left);
            pattern.append("''");
            pattern.append(right);
        } else {
            pattern.append(left);
            pattern.append(' ');
            pattern.append(right);
            pattern.append("''");
        }
        pattern.append(" {0} ");
        pattern.append('\'').append(quoted).append('\'');

        trigger(pattern.toString());
    }

    private static void trigger(String pattern) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, NON_NULL_REGISTRY);

        String formatted = emf.format(new Object[] { "DUMMY" });
        String reparsedPattern = emf.toPattern();
        ExtendedMessageFormat reparsed = new ExtendedMessageFormat(reparsedPattern, NON_NULL_REGISTRY);
        String formattedAgain = reparsed.format(new Object[] { "DUMMY" });

        if (!formatted.equals(formattedAgain)) {
            throw new RuntimeException(
                    "[oracle:roundtrip] metamorphic violation: pattern=" + pattern
                            + " lhs=" + formatted + " rhs=" + formattedAgain);
        }
    }

    private static String sanitize(String s) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length() && out.length() < 24; i++) {
            char c = s.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == ' ') {
                out.append(c);
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
## [48] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [49] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (8 message(s), ~2,794 chars, 1 new)</summary>

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
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    private static final Map NON_NULL_REGISTRY = new HashMap();
    private static final String ANCHOR_PATTERN = "it''s a {0} 'test'!";

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        trigger(ANCHOR_PATTERN);

        String left = sanitize(data.consumeAsciiString(24));
        String right = sanitize(data.consumeAsciiString(24));
        String quoted = sanitize(data.consumeAsciiString(24));
        boolean putQuoteNearStart = data.consumeBoolean();

        StringBuilder pattern = new StringBuilder();
        if (putQuoteNearStart) {
            pattern.append(left);
            pattern.append("''");
            pattern.append(right);
        } else {
            pattern.append(left);
            pattern.append(' ');
            pattern.append(right);
            pattern.append("''");
        }
        pattern.append(" {0} ");
        pattern.append('\'').append(quoted).append('\'');

        trigger(pattern.toString());
    }

    private static void trigger(String pattern) {
        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, NON_NULL_REGISTRY);

        String formatted = emf.format(new Object[] { "DUMMY" });
        String reparsedPattern = emf.toPattern();
        ExtendedMessageFormat reparsed = new ExtendedMessageFormat(reparsedPattern, NON_NULL_REGISTRY);
        String formattedAgain = reparsed.format(new Object[] { "DUMMY" });

        if (!formatted.equals(formattedAgain)) {
            throw new RuntimeException(
                    "[oracle:roundtrip] metamorphic violation: pattern=" + pattern
                            + " lhs=" + formatted + " rhs=" + formattedAgain);
        }
    }

    private static String sanitize(String s) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length() && out.length() < 24; i++) {
            char c = s.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == ' ') {
                out.append(c);
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
<details open><summary>▸ Output (~2,848 chars)</summary>

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
    private static final String ANCHOR = "it''s a {0,lower} 'test'!";

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        new ExtendedMessageFormat(ANCHOR, REGISTRY);

        String before = word(data.consumeAsciiString(16));
        String middle = word(data.consumeAsciiString(16));
        String quoted = word(data.consumeAsciiString(16));
        String fmt = data.consumeBoolean() ? "lower" : "upper";

        String pattern = before + "''" + middle + " {0," + fmt + "} '" + quoted + "'!";
        new ExtendedMessageFormat(pattern, REGISTRY);
    }

    private static String word(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length() && sb.length() < 16; i++) {
            char c = s.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                sb.append(c);
            }
        }
        if (sb.length() == 0) {
            sb.append('x');
        }
        return sb.toString();
    }

    private static Map buildRegistry() {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());
        return registry;
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new CaseFormat(true, locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String args, Locale locale) {
            return new CaseFormat(false, locale);
        }
    }

    private static final class CaseFormat extends Format {
        private static final long serialVersionUID = 1L;
        private final boolean lower;
        private final Locale locale;

        private CaseFormat(boolean lower, Locale locale) {
            this.lower = lower;
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            return toAppendTo.append(lower ? s.toLowerCase(locale) : s.toUpperCase(locale));
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
<details open><summary>▸ Output (~5,318 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        exercise("it''s a {0,lower} 'test'!", registry, false);

        int cases = 1 + data.consumeInt(0, 3);
        for (int i = 0; i < cases; i++) {
            String pattern = buildPattern(data, i);
            exercise(pattern, registry, true);
        }
    }

    private static void exercise(String pattern, Map registry, boolean runOracle) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

            if (!runOracle) {
                return;
            }

            String reported = emf.toPattern();

            try {
                ExtendedMessageFormat emf2 = new ExtendedMessageFormat(reported, registry);
                String independent = emf2.toPattern();

                /* Contract/oracle:
                 * toPattern() is the object's own pattern representation. Reconstructing an
                 * ExtendedMessageFormat from that representation and reading toPattern() again
                 * must be stable for any correct implementation. A throw-deleting or
                 * quote-skipping patch can avoid the crash while silently corrupting the
                 * recorded pattern, which this idempotent round-trip exposes.
                 */
                if (!reported.equals(independent)) {
                    throw new RuntimeException(
                        "[oracle:toPattern-idempotence] metamorphic violation: input=" + pattern
                            + " lhs=" + reported + " rhs=" + independent);
                }

                String f1 = emf.format(new Object[] { "DuMmY" });
                String f2 = emf2.format(new Object[] { "DuMmY" });
                if (!f1.equals(f2)) {
                    throw new RuntimeException(
                        "[oracle:format-consistency] metamorphic violation: input=" + pattern
                            + " lhs=" + f1 + " rhs=" + f2);
                }
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                }
                if (t instanceof Error) {
                    throw (Error) t;
                }
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                if (t instanceof Error) {
                    throw (Error) t;
                }
                if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                }
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
        String n = t.getClass().getName();
        return n.contains("Validation") || n.contains("Invalid");
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String m = ste.getMethodName();
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

    private static String buildPattern(FuzzedDataProvider data, int variant) {
        String a = safeLiteral(data.consumeAsciiString(12));
        String b = safeLiteral(data.consumeAsciiString(12));
        String c = safeLiteral(data.consumeAsciiString(12));

        if (a.length() == 0) {
            a = "a";
        }
        if (b.length() == 0) {
            b = "b";
        }
        if (c.length() == 0) {
            c = "c";
        }

        switch (variant % 4) {
            case 0:
                return a + "''" + b + " {0} " + c;
            case 1:
                return "'" + a + "''" + b + "' {0}";
            case 2:
                return a + " {0} '' " + b;
            default:
                return a + "''" + b + " '{1}' {0} " + c;
        }
    }

    private static String safeLiteral(String s) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length() && out.length() < 24; i++) {
            char ch = s.charAt(i);
            if (Character.isLetterOrDigit(ch) || ch == ' ') {
                out.append(ch);
            }
        }
        return out.toString();
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
<details><summary>▸ Prompt (4 message(s), ~5,515 chars, 1 new)</summary>

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

        exercise("it''s a {0,lower} 'test'!", registry, false);

        int cases = 1 + data.consumeInt(0, 3);
        for (int i = 0; i < cases; i++) {
            String pattern = buildPattern(data, i);
            exercise(pattern, registry, true);
        }
    }

    private static void exercise(String pattern, Map registry, boolean runOracle) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

            if (!runOracle) {
                return;
            }

            String reported = emf.toPattern();

            try {
                ExtendedMessageFormat emf2 = new ExtendedMessageFormat(reported, registry);
                String independent = emf2.toPattern();

                /* Contract/oracle:
                 * toPattern() is the object's own pattern representation. Reconstructing an
                 * ExtendedMessageFormat from that representation and reading toPattern() again
                 * must be stable for any correct implementation. A throw-deleting or
                 * quote-skipping patch can avoid the crash while silently corrupting the
                 * recorded pattern, which this idempotent round-trip exposes.
                 */
                if (!reported.equals(independent)) {
                    throw new RuntimeException(
                        "[oracle:toPattern-idempotence] metamorphic violation: input=" + pattern
                            + " lhs=" + reported + " rhs=" + independent);
                }

                String f1 = emf.format(new Object[] { "DuMmY" });
                String f2 = emf2.format(new Object[] { "DuMmY" });
                if (!f1.equals(f2)) {
                    throw new RuntimeException(
                        "[oracle:format-consistency] metamorphic violation: input=" + pattern
                            + " lhs=" + f1 + " rhs=" + f2);
                }
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                }
                if (t instanceof Error) {
                    throw (Error) t;
                }
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                if (t instanceof Error) {
                    throw (Error) t;
                }
                if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                }
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
        String n = t.getClass().getName();
        return n.contains("Validation") || n.contains("Invalid");
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String m = ste.getMethodName();
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

    private static String buildPattern(FuzzedDataProvider data, int variant) {
        String a = safeLiteral(data.consumeAsciiString(12));
        String b = safeLiteral(data.consumeAsciiString(12));
        String c = safeLiteral(data.consumeAsciiString(12));

        if (a.length() == 0) {
            a = "a";
        }
        if (b.length() == 0) {
            b = "b";
        }
        if (c.length() == 0) {
            c = "c";
        }

        switch (variant % 4) {
            case 0:
                return a + "''" + b + " {0} " + c;
            case 1:
                return "'" + a + "''" + b + "' {0}";
            case 2:
                return a + " {0} '' " + b;
            default:
                return a + "''" + b + " '{1}' {0} " + c;
        }
    }

    private static String safeLiteral(String s) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length() && out.length() < 24; i++) {
            char ch = s.charAt(i);
            if (Character.isLetterOrDigit(ch) || ch == ' ') {
                out.append(ch);
            }
        }
        return out.toString();
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~2,930 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        String anchor = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchorEmf = new ExtendedMessageFormat(anchor, registry);

        String anchorPattern = anchorEmf.toPattern();
        ExtendedMessageFormat anchorRoundTrip = new ExtendedMessageFormat(anchorPattern, registry);
        String anchorPattern2 = anchorRoundTrip.toPattern();
        if (!anchorPattern.equals(anchorPattern2)) {
            throw new RuntimeException("[oracle:anchor-toPattern-idempotence] metamorphic violation: input=" + anchor
                    + " lhs=" + anchorPattern + " rhs=" + anchorPattern2);
        }

        String left = safeLiteral(data.consumeAsciiString(16));
        String right = safeLiteral(data.consumeAsciiString(16));
        String tail = safeLiteral(data.consumeAsciiString(16));

        if (left.length() == 0) {
            left = "it";
        }
        if (right.length() == 0) {
            right = "s";
        }

        String pattern;
        switch (data.consumeInt(0, 3)) {
            case 0:
                pattern = left + "''" + right + " {0}";
                break;
            case 1:
                pattern = left + "''" + right + " {0,lower}";
                break;
            case 2:
                pattern = left + "''" + right + " {0} '" + tail + "'";
                break;
            default:
                pattern = left + "''" + right + " {0,lower} '" + tail + "'!";
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

        /* Contract: toPattern() returns the pattern representation of the format.
         * Reconstructing a new ExtendedMessageFormat from that representation and
         * asking for toPattern() again must be stable for any correct implementation.
         * A patch that merely skips quote handling to avoid the crash can violate this.
         */
        String p1 = emf.toPattern();
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(p1, registry);
        String p2 = emf2.toPattern();
        if (!p1.equals(p2)) {
            throw new RuntimeException("[oracle:toPattern-idempotence] metamorphic violation: input=" + pattern
                    + " lhs=" + p1 + " rhs=" + p2);
        }
    }

    private static String safeLiteral(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length() && sb.length() < 24; i++) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c) || c == ' ') {
                sb.append(c);
            }
        }
        return sb.toString();
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
<details><summary>▸ Prompt (6 message(s), ~3,247 chars, 1 new)</summary>

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

        String anchor = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchorEmf = new ExtendedMessageFormat(anchor, registry);

        String anchorPattern = anchorEmf.toPattern();
        ExtendedMessageFormat anchorRoundTrip = new ExtendedMessageFormat(anchorPattern, registry);
        String anchorPattern2 = anchorRoundTrip.toPattern();
        if (!anchorPattern.equals(anchorPattern2)) {
            throw new RuntimeException("[oracle:anchor-toPattern-idempotence] metamorphic violation: input=" + anchor
                    + " lhs=" + anchorPattern + " rhs=" + anchorPattern2);
        }

        String left = safeLiteral(data.consumeAsciiString(16));
        String right = safeLiteral(data.consumeAsciiString(16));
        String tail = safeLiteral(data.consumeAsciiString(16));

        if (left.length() == 0) {
            left = "it";
        }
        if (right.length() == 0) {
            right = "s";
        }

        String pattern;
        switch (data.consumeInt(0, 3)) {
            case 0:
                pattern = left + "''" + right + " {0}";
                break;
            case 1:
                pattern = left + "''" + right + " {0,lower}";
                break;
            case 2:
                pattern = left + "''" + right + " {0} '" + tail + "'";
                break;
            default:
                pattern = left + "''" + right + " {0,lower} '" + tail + "'!";
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);

        /* Contract: toPattern() returns the pattern representation of the format.
         * Reconstructing a new ExtendedMessageFormat from that representation and
         * asking for toPattern() again must be stable for any correct implementation.
         * A patch that merely skips quote handling to avoid the crash can violate this.
         */
        String p1 = emf.toPattern();
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(p1, registry);
        String p2 = emf2.toPattern();
        if (!p1.equals(p2)) {
            throw new RuntimeException("[oracle:toPattern-idempotence] metamorphic violation: input=" + pattern
                    + " lhs=" + p1 + " rhs=" + p2);
        }
    }

    private static String safeLiteral(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length() && sb.length() < 24; i++) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c) || c == ' ') {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~4,595 chars)</summary>

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

        String anchor = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchorEmf = new ExtendedMessageFormat(anchor, registry);
        String anchorOut = anchorEmf.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-format] metamorphic violation: input=" + anchor + " output=" + anchorOut);
        }

        String prefix = clean(data.consumeAsciiString(12));
        String middle = clean(data.consumeAsciiString(12));
        String quoted = clean(data.consumeAsciiString(12));
        String arg = clean(data.consumeAsciiString(12));
        boolean useLower = data.consumeBoolean();

        if (prefix.length() == 0) {
            prefix = "it";
        }
        if (middle.length() == 0) {
            middle = "s";
        }
        if (quoted.length() == 0) {
            quoted = "test";
        }
        if (arg.length() == 0) {
            arg = "DUMMY";
        }

        String fmt = useLower ? "lower" : "upper";
        String pattern = prefix + "''" + middle + " {0," + fmt + "} '" + quoted + "'!";

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String out = emf.format(new Object[] { arg });

        String expectedArg = useLower ? arg.toLowerCase(Locale.ROOT) : arg.toUpperCase(Locale.ROOT);
        String expected = prefix + "'" + middle + " " + expectedArg + " " + quoted + "!";
        if (!expected.equals(out)) {
            throw new RuntimeException("[oracle:format] metamorphic violation: input=" + pattern + " output=" + out + " expected=" + expected);
        }

        String p1 = emf.toPattern();
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(p1, registry);
        String p2 = emf2.toPattern();
        if (!p1.equals(p2)) {
            throw new RuntimeException("[oracle:toPattern-idempotence] metamorphic violation: input=" + pattern + " lhs=" + p1 + " rhs=" + p2);
        }
    }

    private static String clean(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length() && sb.length() < 24; i++) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c) || c == ' ') {
                sb.append(c);
            }
        }
        return sb.toString();
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

    public static class UpperCaseFormat extends Format {
        private final Locale locale;

        public UpperCaseFormat(Locale locale) {
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
## [56] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [57] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (8 message(s), ~5,032 chars, 1 new)</summary>

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
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();
        registry.put("lower", new LowerCaseFormatFactory());
        registry.put("upper", new UpperCaseFormatFactory());

        String anchor = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat anchorEmf = new ExtendedMessageFormat(anchor, registry);
        String anchorOut = anchorEmf.format(new Object[] { "DUMMY" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-format] metamorphic violation: input=" + anchor + " output=" + anchorOut);
        }

        String prefix = clean(data.consumeAsciiString(12));
        String middle = clean(data.consumeAsciiString(12));
        String quoted = clean(data.consumeAsciiString(12));
        String arg = clean(data.consumeAsciiString(12));
        boolean useLower = data.consumeBoolean();

        if (prefix.length() == 0) {
            prefix = "it";
        }
        if (middle.length() == 0) {
            middle = "s";
        }
        if (quoted.length() == 0) {
            quoted = "test";
        }
        if (arg.length() == 0) {
            arg = "DUMMY";
        }

        String fmt = useLower ? "lower" : "upper";
        String pattern = prefix + "''" + middle + " {0," + fmt + "} '" + quoted + "'!";

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String out = emf.format(new Object[] { arg });

        String expectedArg = useLower ? arg.toLowerCase(Locale.ROOT) : arg.toUpperCase(Locale.ROOT);
        String expected = prefix + "'" + middle + " " + expectedArg + " " + quoted + "!";
        if (!expected.equals(out)) {
            throw new RuntimeException("[oracle:format] metamorphic violation: input=" + pattern + " output=" + out + " expected=" + expected);
        }

        String p1 = emf.toPattern();
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(p1, registry);
        String p2 = emf2.toPattern();
        if (!p1.equals(p2)) {
            throw new RuntimeException("[oracle:toPattern-idempotence] metamorphic violation: input=" + pattern + " lhs=" + p1 + " rhs=" + p2);
        }
    }

    private static String clean(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length() && sb.length() < 24; i++) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c) || c == ' ') {
                sb.append(c);
            }
        }
        return sb.toString();
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

    public static class UpperCaseFormat extends Format {
        private final Locale locale;

        public UpperCaseFormat(Locale locale) {
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
<details open><summary>▸ Output (~4,233 chars)</summary>

```
package org.apache.commons.lang.text;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashMap;
import java.util.Map;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Map registry = new HashMap();

        String anchor = "it''s a {0} 'test'!";
        ExtendedMessageFormat anchorEmf = new ExtendedMessageFormat(anchor, registry);
        String anchorOut = anchorEmf.format(new Object[] { "dummy" });
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-format] metamorphic violation: input=" + anchor + " output=" + anchorOut);
        }

        String left = sanitize(data.consumeAsciiString(12));
        String right = sanitize(data.consumeAsciiString(12));
        String quoted = sanitize(data.consumeAsciiString(12));
        String arg = sanitize(data.consumeAsciiString(12));
        String suffix = sanitize(data.consumeAsciiString(12));

        if (left.length() == 0) {
            left = "it";
        }
        if (right.length() == 0) {
            right = "s";
        }
        if (quoted.length() == 0) {
            quoted = "test";
        }
        if (arg.length() == 0) {
            arg = "dummy";
        }

        String pattern;
        switch (data.consumeInt(0, 3)) {
            case 0:
                pattern = left + "''" + right + " {0}";
                break;
            case 1:
                pattern = left + "''" + right + " {0} '" + quoted + "'";
                break;
            case 2:
                pattern = "'" + quoted + "' " + left + "''" + right + " {0}";
                break;
            default:
                pattern = left + "''" + right + " {0} '" + quoted + "'" + suffix;
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, registry);
        String out = emf.format(new Object[] { arg });

        String expected;
        switch (patternCase(pattern, left, right, quoted, suffix)) {
            case 0:
                expected = left + "'" + right + " " + arg;
                break;
            case 1:
                expected = left + "'" + right + " " + arg + " " + quoted;
                break;
            case 2:
                expected = quoted + " " + left + "'" + right + " " + arg;
                break;
            default:
                expected = left + "'" + right + " " + arg + " " + quoted + suffix;
                break;
        }

        /* Contract: MessageFormat formatting must honor doubled quotes as a literal
         * apostrophe and single-quoted text as literal text. A patch that merely avoids
         * the crash by skipping quote handling would violate this observable output.
         */
        if (!expected.equals(out)) {
            throw new RuntimeException("[oracle:format] metamorphic violation: input=" + pattern + " output=" + out + " expected=" + expected);
        }

        String p1 = emf.toPattern();
        ExtendedMessageFormat emf2 = new ExtendedMessageFormat(p1, registry);
        String p2 = emf2.toPattern();
        if (!p1.equals(p2)) {
            throw new RuntimeException("[oracle:toPattern-idempotence] metamorphic violation: input=" + pattern + " lhs=" + p1 + " rhs=" + p2);
        }
    }

    private static int patternCase(String pattern, String left, String right, String quoted, String suffix) {
        String c0 = left + "''" + right + " {0}";
        if (pattern.equals(c0)) {
            return 0;
        }
        String c1 = left + "''" + right + " {0} '" + quoted + "'";
        if (pattern.equals(c1)) {
            return 1;
        }
        String c2 = "'" + quoted + "' " + left + "''" + right + " {0}";
        if (pattern.equals(c2)) {
            return 2;
        }
        return 3;
    }

    private static String sanitize(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length() && sb.length() < 24; i++) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c) || c == ' ') {
                sb.append(c);
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
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [59] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~161 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [11] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,499 chars)</summary>

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
        runCase("it''s a {0,lower} 'test'!", true);

        int variant = data.consumeInt(0, 5);
        String a = sanitize(data.consumeAsciiString(16));
        String b = sanitize(data.consumeAsciiString(16));
        String c = sanitizeNoQuote(data.consumeAsciiString(16));
        String d = sanitizeNoQuote(data.consumeAsciiString(16));
        String fmt = data.consumeBoolean() ? "lower" : "upper";

        String pattern;
        switch (variant) {
            case 0:
                pattern = a + "''" + b + " {0," + fmt + "} '" + c + "'";
                break;
            case 1:
                pattern = "''" + a + " {0," + fmt + "} '" + c + "'";
                break;
            case 2:
                pattern = a + " ''" + b + " {0," + fmt + "} '" + c + "'";
                break;
            case 3:
                pattern = a + "''" + b + "{0," + fmt + "}'" + c + "'";
                break;
            case 4:
                pattern = a + " {0," + fmt + "} " + b + "''" + c + " '" + d + "'";
                break;
            default:
                pattern = a + "''" + b + " {0," + fmt + "} '" + c + "'!";
                break;
        }

        runCase(pattern, false);
    }

    private static void runCase(String pattern, boolean anchor) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, Locale.getDefault(), REGISTRY);

            String toPatternBefore = emf.toPattern();
            String formatted = emf.format(new Object[] { "DUMMY" });
            String toPatternAfter = emf.toPattern();

            if (anchor) {
                if (!"it's a dummy test!".equals(formatted)) {
                    throw new RuntimeException("[oracle:anchor-output] metamorphic violation: seed formatting input=" + pattern + " got=" + formatted + " expected=it's a dummy test!");
                }
            }

            if (toPatternBefore != null && toPatternAfter != null && !toPatternBefore.equals(toPatternAfter)) {
                throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: toPattern changed across read-only operations input=" + pattern + " before=" + toPatternBefore + " after=" + toPatternAfter);
            }

            try {
                ExtendedMessageFormat reparsed = new ExtendedMessageFormat(toPatternBefore, Locale.getDefault(), REGISTRY);
                String reparsedPattern = reparsed.toPattern();
                String reparsedFormatted = reparsed.format(new Object[] { "DUMMY" });

                /*
                 * Contract/oracle:
                 * toPattern() is the object's pattern representation; constructing a fresh
                 * ExtendedMessageFormat from that representation should preserve the same
                 * pattern and formatting behavior for the same arguments. A "fix" that only
                 * skips the crashing branch or silently drops quote handling would break this.
                 */
                if (!toPatternBefore.equals(reparsedPattern)) {
                    throw new RuntimeException("[oracle:roundtrip-pattern] metamorphic violation: input=" + pattern + " first=" + toPatternBefore + " second=" + reparsedPattern);
                }
                if (!formatted.equals(reparsedFormatted)) {
                    throw new RuntimeException("[oracle:roundtrip-format] metamorphic violation: input=" + pattern + " first=" + formatted + " second=" + reparsedFormatted);
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
        while (t != null) {
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return true;
            }
            String name = t.getClass().getName();
            if (name != null) {
                String lower = name.toLowerCase(Locale.ROOT);
                if (lower.contains("validation") || lower.contains("invalid")) {
                    return true;
                }
            }
            t = t.getCause();
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String method = ste.getMethodName();
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

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "a";
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
            sb.append('a');
        }
        return sb.toString();
    }

    private static String sanitizeNoQuote(String s) {
        if (s == null || s.length() == 0) {
            return "t";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\'' || ch == '{' || ch == '}') {
                sb.append('t');
            } else {
                sb.append(ch);
            }
        }
        if (sb.length() == 0) {
            sb.append('t');
        }
        return sb.toString();
    }

    private static Map buildRegistry() {
        Map map = new HashMap();
        map.put("lower", new LowerCaseFormatFactory());
        map.put("upper", new UpperCaseFormatFactory());
        return map;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(Throwable t) throws T {
        throw (T) t;
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new CaseFormat(true, locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new CaseFormat(false, locale);
        }
    }

    private static final class CaseFormat extends Format {
        private final boolean lower;
        private final Locale locale;

        private CaseFormat(boolean lower, Locale locale) {
            this.lower = lower;
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(lower ? s.toLowerCase(locale) : s.toUpperCase(locale));
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
## [60] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [61] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~8,697 chars, 1 new)</summary>

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
    private static final Map REGISTRY = buildRegistry();

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runCase("it''s a {0,lower} 'test'!", true);

        int variant = data.consumeInt(0, 5);
        String a = sanitize(data.consumeAsciiString(16));
        String b = sanitize(data.consumeAsciiString(16));
        String c = sanitizeNoQuote(data.consumeAsciiString(16));
        String d = sanitizeNoQuote(data.consumeAsciiString(16));
        String fmt = data.consumeBoolean() ? "lower" : "upper";

        String pattern;
        switch (variant) {
            case 0:
                pattern = a + "''" + b + " {0," + fmt + "} '" + c + "'";
                break;
            case 1:
                pattern = "''" + a + " {0," + fmt + "} '" + c + "'";
                break;
            case 2:
                pattern = a + " ''" + b + " {0," + fmt + "} '" + c + "'";
                break;
            case 3:
                pattern = a + "''" + b + "{0," + fmt + "}'" + c + "'";
                break;
            case 4:
                pattern = a + " {0," + fmt + "} " + b + "''" + c + " '" + d + "'";
                break;
            default:
                pattern = a + "''" + b + " {0," + fmt + "} '" + c + "'!";
                break;
        }

        runCase(pattern, false);
    }

    private static void runCase(String pattern, boolean anchor) {
        try {
            ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, Locale.getDefault(), REGISTRY);

            String toPatternBefore = emf.toPattern();
            String formatted = emf.format(new Object[] { "DUMMY" });
            String toPatternAfter = emf.toPattern();

            if (anchor) {
                if (!"it's a dummy test!".equals(formatted)) {
                    throw new RuntimeException("[oracle:anchor-output] metamorphic violation: seed formatting input=" + pattern + " got=" + formatted + " expected=it's a dummy test!");
                }
            }

            if (toPatternBefore != null && toPatternAfter != null && !toPatternBefore.equals(toPatternAfter)) {
                throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: toPattern changed across read-only operations input=" + pattern + " before=" + toPatternBefore + " after=" + toPatternAfter);
            }

            try {
                ExtendedMessageFormat reparsed = new ExtendedMessageFormat(toPatternBefore, Locale.getDefault(), REGISTRY);
                String reparsedPattern = reparsed.toPattern();
                String reparsedFormatted = reparsed.format(new Object[] { "DUMMY" });

                /*
                 * Contract/oracle:
                 * toPattern() is the object's pattern representation; constructing a fresh
                 * ExtendedMessageFormat from that representation should preserve the same
                 * pattern and formatting behavior for the same arguments. A "fix" that only
                 * skips the crashing branch or silently drops quote handling would break this.
                 */
                if (!toPatternBefore.equals(reparsedPattern)) {
                    throw new RuntimeException("[oracle:roundtrip-pattern] metamorphic violation: input=" + pattern + " first=" + toPatternBefore + " second=" + reparsedPattern);
                }
                if (!formatted.equals(reparsedFormatted)) {
                    throw new RuntimeException("[oracle:roundtrip-format] metamorphic violation: input=" + pattern + " first=" + formatted + " second=" + reparsedFormatted);
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
        while (t != null) {
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return true;
            }
            String name = t.getClass().getName();
            if (name != null) {
                String lower = name.toLowerCase(Locale.ROOT);
                if (lower.contains("validation") || lower.contains("invalid")) {
                    return true;
                }
            }
            t = t.getCause();
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof OutOfMemoryError)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String method = ste.getMethodName();
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

    private static String sanitize(String s) {
        if (s == null || s.length() == 0) {
            return "a";
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
            sb.append('a');
        }
        return sb.toString();
    }

    private static String sanitizeNoQuote(String s) {
        if (s == null || s.length() == 0) {
            return "t";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\'' || ch == '{' || ch == '}') {
                sb.append('t');
            } else {
                sb.append(ch);
            }
        }
        if (sb.length() == 0) {
            sb.append('t');
        }
        return sb.toString();
    }

    private static Map buildRegistry() {
        Map map = new HashMap();
        map.put("lower", new LowerCaseFormatFactory());
        map.put("upper", new UpperCaseFormatFactory());
        return map;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(Throwable t) throws T {
        throw (T) t;
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new CaseFormat(true, locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new CaseFormat(false, locale);
        }
    }

    private static final class CaseFormat extends Format {
        private final boolean lower;
        private final Locale locale;

        private CaseFormat(boolean lower, Locale locale) {
            this.lower = lower;
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(lower ? s.toLowerCase(locale) : s.toUpperCase(locale));
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
<details open><summary>▸ Output (~4,820 chars)</summary>

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
        String anchor = "it''s a {0,lower} 'test'!";
        ExtendedMessageFormat emfAnchor = new ExtendedMessageFormat(anchor, REGISTRY);
        String anchorOut = emfAnchor.format(new Object[] {"DUMMY"});
        if (!"it's a dummy test!".equals(anchorOut)) {
            throw new RuntimeException("[oracle:anchor-output] metamorphic violation: expected it's a dummy test! got=" + anchorOut);
        }

        String prefix = clean(data.consumeAsciiString(12));
        String infix = clean(data.consumeAsciiString(12));
        String suffix = clean(data.consumeAsciiString(12));
        String quoted = clean(data.consumeAsciiString(12));
        String fmt = data.consumeBoolean() ? "lower" : "upper";

        int shape = data.consumeInt(0, 5);
        String pattern;
        switch (shape) {
            case 0:
                pattern = prefix + "''" + infix + " {0," + fmt + "} '" + quoted + "'" + suffix;
                break;
            case 1:
                pattern = "''" + prefix + " {0," + fmt + "} '" + quoted + "'" + suffix;
                break;
            case 2:
                pattern = prefix + "''s a {0," + fmt + "} '" + quoted + "'!";
                break;
            case 3:
                pattern = prefix + "''" + infix + "{0," + fmt + "} '" + quoted + "'";
                break;
            case 4:
                pattern = prefix + "''" + infix + " {0," + fmt + "} '" + quoted + "'!";
                break;
            default:
                pattern = prefix + "''" + infix + " and {0," + fmt + "} '" + quoted + "' " + suffix;
                break;
        }

        ExtendedMessageFormat emf = new ExtendedMessageFormat(pattern, REGISTRY);
        String before = emf.toPattern();
        String out1 = emf.format(new Object[] {"DUMMY"});
        String after = emf.toPattern();
        if (!before.equals(after)) {
            throw new RuntimeException("[oracle:toPattern-stable] metamorphic violation: before=" + before + " after=" + after + " input=" + pattern);
        }

        ExtendedMessageFormat roundTrip = new ExtendedMessageFormat(before, REGISTRY);
        String out2 = roundTrip.format(new Object[] {"DUMMY"});
        if (!out1.equals(out2)) {
            throw new RuntimeException("[oracle:roundtrip-format] metamorphic violation: input=" + pattern + " first=" + out1 + " second=" + out2);
        }
    }

    private static String clean(String s) {
        if (s == null || s.length() == 0) {
            return "a";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{' || ch == '}' || ch == '\'') {
                sb.append('a');
            } else {
                sb.append(ch);
            }
        }
        if (sb.length() == 0) {
            sb.append('a');
        }
        return sb.toString();
    }

    private static Map buildRegistry() {
        Map map = new HashMap();
        map.put("lower", new LowerCaseFormatFactory());
        map.put("upper", new UpperCaseFormatFactory());
        return map;
    }

    private static final class LowerCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new CaseFormat(true, locale);
        }
    }

    private static final class UpperCaseFormatFactory implements FormatFactory {
        public Format getFormat(String name, String arguments, Locale locale) {
            return new CaseFormat(false, locale);
        }
    }

    private static final class CaseFormat extends Format {
        private final boolean lower;
        private final Locale locale;

        private CaseFormat(boolean lower, Locale locale) {
            this.lower = lower;
            this.locale = locale == null ? Locale.getDefault() : locale;
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            String s = String.valueOf(obj);
            toAppendTo.append(lower ? s.toLowerCase(locale) : s.toUpperCase(locale));
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
## [62] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. It ran for the entire time budget on the buggy code and Jazzer reported no crash. Most often this means the inputs you generat

---
## [63] ⚙️ coverage · `jazzer coverage dumps`
**output:** **0/27 dump(s) written under /datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/cov; 27 raw fuzz log(s)**
<details><summary>▸ detail (~13,085 chars)</summary>

- detail: {'dumps': [{'harness': 'attempt_001', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_001_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_002', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_002_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_003', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_003_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_004', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_004_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_006', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_006_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_008', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_008_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_009', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_009_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_010', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_010_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_011', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_011_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_012', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_012_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_013', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_013_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_014', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_014_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_015', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_015_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_016', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_016_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_017', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_017_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_018', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_018_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_020', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_020_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_021', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_021_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_022', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_022_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_023', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_023_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_024', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_024_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_025', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_025_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_026', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_026_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_027', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_027_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_028', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_028_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_029', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_029_compiled.exec', 'exists': False, 'bytes': 0}, {'harness': 'attempt_030', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_030_compiled.exec', 'exists': False, 'bytes': 0}], 'outputs': ['/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_001_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_002_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_003_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_004_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_006_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_008_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_009_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_010_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_011_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_012_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_013_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_014_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_015_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_016_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_017_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_018_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_020_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_021_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_022_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_023_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_024_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_025_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_026_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_027_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_028_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_029_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/fuzz_out/attempt_030_compiled.txt'], 'classpath': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/cov/classpath.json', 'dir': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/cov', 'compiled_attempts': ['attempt_001', 'attempt_002', 'attempt_003', 'attempt_004', 'attempt_006', 'attempt_008', 'attempt_009', 'attempt_010', 'attempt_011', 'attempt_012', 'attempt_013', 'attempt_014', 'attempt_015', 'attempt_016', 'attempt_017', 'attempt_018', 'attempt_020', 'attempt_021', 'attempt_022', 'attempt_023', 'attempt_024', 'attempt_025', 'attempt_026', 'attempt_027', 'attempt_028', 'attempt_029', 'attempt_030'], 'accepted_attempts': [], 'sources': ['/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_001.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_002.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_003.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_004.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_006.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_008.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_009.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_010.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_011.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_012.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_013.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_014.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_015.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_016.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_017.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_018.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_020.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_021.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_022.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_023.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_024.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_025.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_026.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_027.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_028.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_029.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/harness_src/attempt_030.java']}

</details>
