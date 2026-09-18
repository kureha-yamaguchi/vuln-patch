# Pipeline trace — Lang-45

**Code:** `b4b78d5146829e25837ef7e7f105b4ec3b686d1e`

**Patch label:** correct  *(the patch under analysis is a known-CORRECT fix — the pipeline is not told this)*

**Outcome:** candidate_experiment (buggy build only)

**Patch under analysis:**
```diff
--- /src/java/org/apache/commons/lang/WordUtils.java
+++ /src/java/org/apache/commons/lang/WordUtils.java
@@ -622,7 +622,9 @@
         if (upper < lower) {
             upper = lower;
         }
-
+        if (upper == -1 || upper > str.length()) {
+        	       	  upper=str.length();
+        }
         StringBuffer result = new StringBuffer();
         int index = StringUtils.indexOf(str, " ", lower);
         if (index == -1) {
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
<details><summary>▸ output — FailureTest(test_class='org.apache.commons.lang.WordUtilsTest', test_method='testAbbreviate', source (~2,674 chars)</summary>

**output:** 
```json
[
  "FailureTest(test_class='org.apache.commons.lang.WordUtilsTest', test_method='testAbbreviate', source_path='/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/Lang_45_buggy/src/test/org/apache/commons/lang/WordUtilsTest.java', method_source='    public void testAbbreviate() {\\n        // check null and empty are returned respectively\\n        assertNull(WordUtils.abbreviate(null, 1,-1,\"\"));\\n        assertEquals(StringUtils.EMPTY, WordUtils.abbreviate(\"\", 1,-1,\"\"));\\n\\n        // test upper limit\\n        assertEquals(\"01234\", WordUtils.abbreviate(\"0123456789\", 0,5,\"\"));\\n        assertEquals(\"01234\", WordUtils.abbreviate(\"0123456789\", 5, 2,\"\"));\\n        assertEquals(\"012\", WordUtils.abbreviate(\"012 3456789\", 2, 5,\"\"));\\n        assertEquals(\"012 3\", WordUtils.abbreviate(\"012 3456789\", 5, 2,\"\"));\\n        assertEquals(\"0123456789\", WordUtils.abbreviate(\"0123456789\", 0,-1,\"\"));\\n\\n        // test upper limit + append string\\n        assertEquals(\"01234-\", WordUtils.abbreviate(\"0123456789\", 0,5,\"-\"));\\n        assertEquals(\"01234-\", WordUtils.abbreviate(\"0123456789\", 5, 2,\"-\"));\\n        assertEquals(\"012\", WordUtils.abbreviate(\"012 3456789\", 2, 5, null));\\n        assertEquals(\"012 3\", WordUtils.abbreviate(\"012 3456789\", 5, 2,\"\"));\\n        assertEquals(\"0123456789\", WordUtils.abbreviate(\"0123456789\", 0,-1,\"\"));\\n\\n        // test lower value\\n        assertEquals(\"012\", WordUtils.abbreviate(\"012 3456789\", 0,5, null));\\n        assertEquals(\"01234\", WordUtils.abbreviate(\"01234 56789\", 5, 10, null));\\n        assertEquals(\"01 23 45 67\", WordUtils.abbreviate(\"01 23 45 67 89\", 9, -1, null));\\n        assertEquals(\"01 23 45 6\", WordUtils.abbreviate(\"01 23 45 67 89\", 9, 10, null));\\n        assertEquals(\"0123456789\", WordUtils.abbreviate(\"0123456789\", 15, 20, null));\\n\\n        // test lower value + append\\n        assertEquals(\"012\", WordUtils.abbreviate(\"012 3456789\", 0,5, null));\\n        assertEquals(\"01234-\", WordUtils.abbreviate(\"01234 56789\", 5, 10, \"-\"));\\n        assertEquals(\"01 23 45 67abc\", WordUtils.abbreviate(\"01 23 45 67 89\", 9, -1, \"abc\"));\\n        assertEquals(\"01 23 45 6\", WordUtils.abbreviate(\"01 23 45 67 89\", 9, 10, \"\"));\\n\\n        // others\\n        assertEquals(\"\", WordUtils.abbreviate(\"0123456790\", 0,0,\"\"));\\n        assertEquals(\"\", WordUtils.abbreviate(\" 0123456790\", 0,-1,\"\"));\\n    }', exception_type='java.lang.StringIndexOutOfBoundsException', failure_message=None, support_source=None)"
]
```

</details>

---
## [1] ⚙️ test-context (H1/H2)
**output:** 
```json
[
  {
    "test": "org.apache.commons.lang.WordUtilsTest::testAbbreviate",
    "failure_message": "--- org.apache.commons.lang.WordUtilsTest::testAbbreviate\njava.lang.StringIndexOutOfBoundsException: String index out of range: 15",
    "support_chars": 0
  }
]
```

---
## [2] ⚙️ analysis (TargetAnalyzer)
<details><summary>▸ output — modified_files": [ (~18,901 chars)</summary>

**output:** 
```json
{
  "modified_files": [
    "src/java/org/apache/commons/lang/WordUtils.java"
  ],
  "patch_text": "--- /src/java/org/apache/commons/lang/WordUtils.java\n+++ /src/java/org/apache/commons/lang/WordUtils.java\n@@ -622,7 +622,9 @@\n         if (upper < lower) {\n             upper = lower;\n         }\n-\n+        if (upper == -1 || upper > str.length()) {\n+        \t       \t  upper=str.length();\n+        }\n         StringBuffer result = new StringBuffer();\n         int index = StringUtils.indexOf(str, \" \", lower);\n         if (index == -1) {\n\n",
  "functions": [
    {
      "func_name": "abbreviate",
      "func_signature": "public static String abbreviate(String str, int lower, int upper, String appendToEnd)",
      "func_source": "    public static String abbreviate(String str, int lower, int upper, String appendToEnd) {\n        // initial parameter checks\n        if (str == null) {\n            return null;\n        }\n        if (str.length() == 0) {\n            return StringUtils.EMPTY;\n        }\n\n        // if the lower value is greater than the length of the string,\n        // set to the length of the string\n        // if the upper value is -1 (i.e. no limit) or is greater\n        // than the length of the string, set to the length of the string\n        if (upper == -1 || upper > str.length()) {\n            upper = str.length();\n        }\n        // if upper is less than lower, raise it to lower\n        if (upper < lower) {\n            upper = lower;\n        }\n\n        StringBuffer result = new StringBuffer();\n        int index = StringUtils.indexOf(str, \" \", lower);\n        if (index == -1) {\n            result.append(str.substring(0, upper));\n            // only if abbreviation has occured do we append the appendToEnd value\n            if (upper != str.length()) {\n                result.append(StringUtils.defaultString(appendToEnd));\n            }\n        } else if (index > upper) {\n            result.append(str.substring(0, upper));\n            result.append(StringUtils.defaultString(appendToEnd));\n        } else {\n            result.append(str.substring(0, index));\n            result.append(StringUtils.defaultString(appendToEnd));\n        }\n        return result.toString();\n    }",
      "func_class": "WordUtils",
      "func_class_fq": "org.apache.commons.lang.WordUtils",
      "func_param_types": [
        "String",
        "int",
        "int",
        "String"
      ],
      "fi_name": "[org.apache.commons.lang.WordUtils].abbreviate(String,int,int,String)",
      "overload_types": [
        [
          "String",
          "int",
          "int",
          "String"
        ]
      ],
      "xrefs": [
        "public void testAbbreviate() {\n        // check null and empty are returned respectively\n        assertNull(WordUtils.abbreviate(null, 1,-1,\"\"));\n        assertEquals(StringUtils.EMPTY, WordUtils.abbreviate(\"\", 1,-1,\"\"));\n\n        // test upper limit\n        assertEquals(\"01234\", WordUtils.abbreviate(\"0123456789\", 0,5,\"\"));\n        assertEquals(\"01234\", WordUtils.abbreviate(\"0123456789\", 5, 2,\"\"));\n        assertEquals(\"012\", WordUtils.abbreviate(\"012 3456789\", 2, 5,\"\"));\n        assertEquals(\"012 3\", WordUtils.abbreviate(\"012 3456789\", 5, 2,\"\"));\n        assertEquals(\"0123456789\", WordUtils.abbreviate(\"0123456789\", 0,-1,\"\"));\n\n        // test upper limit + append string\n        assertEquals(\"01234-\", WordUtils.abbreviate(\"0123456789\", 0,5,\"-\"));\n        assertEquals(\"01234-\", WordUtils.abbreviate(\"0123456789\", 5, 2,\"-\"));\n        assertEquals(\"012\", WordUtils.abbreviate(\"012 3456789\", 2, 5, null));\n        assertEquals(\"012 3\", WordUtils.abbreviate(\"012 3456789\", 5, 2,\"\"));\n        assertEquals(\"0123456789\", WordUtils.abbreviate(\"0123456789\", 0,-1,\"\"));\n\n        // test lower value\n        assertEquals(\"012\", WordUtils.abbreviate(\"012 3456789\", 0,5, null));\n        assertEquals(\"01234\", WordUtils.abbreviate(\"01234 56789\", 5, 10, null));\n        assertEquals(\"01 23 45 67\", WordUtils.abbreviate(\"01 23 45 67 89\", 9, -1, null));\n        assertEquals(\"01 23 45 6\", WordUtils.abbreviate(\"01 23 45 67 89\", 9, 10, null));\n        assertEquals(\"0123456789\", WordUtils.abbreviate(\"0123456789\", 15, 20, null));\n\n        // test lower value + append\n        assertEquals(\"012\", WordUtils.abbreviate(\"012 3456789\", 0,5, null));\n        assertEquals(\"01234-\", WordUtils.abbreviate(\"01234 56789\", 5, 10, \"-\"));\n        assertEquals(\"01 23 45 67abc\", WordUtils.abbreviate(\"01 23 45 67 89\", 9, -1, \"abc\"));\n        assertEquals(\"01 23 45 6\", WordUtils.abbreviate(\"01 23 45 67 89\", 9, 10, \"\"));\n\n        // others\n        assertEquals(\"\", WordUtils.abbreviate(\"0123456790\", 0,0,\"\"));\n        assertEquals(\"\", WordUtils.abbreviate(\" 0123456790\", 0,-1,\"\"));\n    }"
      ],
      "xref_names": [
        "[org.apache.commons.lang.WordUtilsTest].testAbbreviate()"
      ],
      "reachable_edges": [
        [
          "[org.apache.commons.lang.WordUtils].abbreviate(String,int,int,String)",
          "[String].length()"
        ],
        [
          "[org.apache.commons.lang.WordUtils].abbreviate(String,int,int,String)",
          "[String].length()"
        ],
        [
          "[org.apache.commons.lang.WordUtils].abbreviate(String,int,int,String)",
          "[String].length()"
        ],
        [
          "[org.apache.commons.lang.WordUtils].abbreviate(String,int,int,String)",
          "[StringBuffer].<init>()"
        ],
        [
          "[org.apache.commons.lang.WordUtils].abbreviate(String,int,int,String)",
          "[org.apache.commons.lang.StringUtils].indexOf(String,String,int)"
        ],
        [
          "[org.apache.commons.lang.WordUtils].abbreviate(String,int,int,String)",
          "[String].substring(int,org.apache.commons.lang.WordUtils)"
        ],
        [
          "[org.apache.commons.lang.WordUtils].abbreviate(String,int,int,String)",
          "[StringBuffer].append(org.apache.commons.lang.WordUtils)"
        ],
        [
          "[org.apache.commons.lang.WordUtils].abbreviate(String,int,int,String)",
          "[String].length()"
        ],
        [
          "[org.apache.commons.lang.WordUtils].abbreviate(String,int,int,String)",
          "[org.apache.commons.lang.StringUtils].defaultString(String)"
        ],
        [
          "[org.apache.commons.lang.WordUtils].abbreviate(String,int,int,String)",
          "[StringBuffer].append(String)"
        ],
        [
          "[org.apache.commons.lang.WordUtils].abbreviate(String,int,int,String)",
          "[String].substring(int,org.apache.commons.lang.WordUtils)"
        ],
        [
          "[org.apache.commons.lang.WordUtils].abbreviate(String,int,int,String)",
          "[StringBuffer].append(org.apache.commons.lang.WordUtils)"
        ],
        [
          "[org.apache.commons.lang.WordUtils].abbreviate(String,int,int,String)",
          "[org.apache.commons.lang.StringUtils].defaultString(String)"
        ],
        [
          "[org.apache.commons.lang.WordUtils].abbreviate(String,int,int,String)",
          "[StringBuffer].append(String)"
        ],
        [
          "[org.apache.commons.lang.WordUtils].abbreviate(String,int,int,String)",
          "[String].substring(int,int)"
        ],
        [
          "[org.apache.commons.lang.WordUtils].abbreviate(String,int,int,String)",
          "[StringBuffer].append(org.apache.commons.lang.WordUtils)"
        ],
        [
          "[org.apache.commons.lang.WordUtils].abbreviate(String,int,int,String)",
          "[org.apache.commons.lang.StringUtils].defaultString(String)"
        ],
        [
          "[org.apache.commons.lang.WordUtils].abbreviate(String,int,int,String)",
          "[StringBuffer].append(String)"
        ],
        [
          "[org.apache.commons.lang.WordUtils].abbreviate(String,int,int,String)",
          "[StringBuffer].toString()"
        ],
        [
          "[org.apache.commons.lang.StringUtils].indexOf(String,String,int)",
          "[String].length()"
        ],
        [
          "[org.apache.commons.lang.StringUtils].indexOf(String,String,int)",
          "[String].length()"
        ],
        [
          "[org.apache.commons.lang.StringUtils].indexOf(String,String,int)",
          "[String].length()"
        ],
        [
          "[org.apache.commons.lang.StringUtils].indexOf(String,String,int)",
          "[String].indexOf(String,int)"
        ]
      ],
      "reachable_depth": {
        "[org.apache.commons.lang.WordUtils].abbreviate(String,int,int,String)": 0,
        "[String].length()": 1,
        "[StringBuffer].<init>()": 1,
        "[org.apache.commons.lang.StringUtils].indexOf(String,String,int)": 1,
        "[String].substring(int,org.apache.commons.lang.WordUtils)": 1,
        "[StringBuffer].append(org.apache.commons.lang.WordUtils)": 1,
        "[org.apache.commons.lang.StringUtils].defaultString(String)": 1,
        "[StringBuffer].append(String)": 1,
        "[String].substring(int,int)": 1,
        "[StringBuffer].toString()": 1,
        "[String].indexOf(String,int)": 2
      },
      "reachable": [
        "[String].length()",
        "[StringBuffer].<init>()",
        "[org.apache.commons.lang.StringUtils].indexOf(String,String,int)",
        "[String].substring(int,org.apache.commons.lang.WordUtils)",
        "[StringBuffer].append(org.apache.commons.lang.WordUtils)",
        "[org.apache.commons.lang.StringUtils].defaultString(String)",
        "[StringBuffer].append(String)",
        "[String].substring(int,int)",
        "[StringBuffer].toString()",
        "[String].indexOf(String,int)"
      ],
      "related_callees": [
        {
          "name": "length",
          "source_file": "StringUtils.java",
          "signature": "public static int length(String str)",
          "source": "    public static int length(String str) {\n        return str == null ? 0 : str.length();\n    }",
          "is_abstract": false,
          "impls": []
        },
        {
          "name": "indexOf",
          "source_file": "ArrayUtils.java",
          "signature": "public static int indexOf(Object[] array, Object objectToFind)",
          "source": "    public static int indexOf(Object[] array, Object objectToFind) {\n        return indexOf(array, objectToFind, 0);\n    }",
          "is_abstract": false,
          "impls": [
            [
              "ArrayUtils.java",
              "    public static int indexOf(Object[] array, Object objectToFind) {\n        return indexOf(array, objectToFind, 0);\n    }"
            ],
            [
              "ArrayUtils.java",
              "    public static int indexOf(Object[] array, Object objectToFind, int startIndex) {\n        if (array == null) {\n            return INDEX_NOT_FOUND;\n        }\n        if (startIndex < 0) {\n            startIndex = 0;\n        }\n        if (objectToFind == null) {\n            for (int i = startIndex; i < array.length; i++) {\n                if (array[i] == null) {\n                    return i;\n                }\n            }\n        } else {\n            for (int i = startIndex; i < array.length; i++) {\n                if (objectToFind.equals(array[i])) {\n                    return i;\n                }\n            }\n        }\n        return INDEX_NOT_FOUND;\n    }"
            ],
            [
              "ArrayUtils.java",
              "    public static int indexOf(long[] array, long valueToFind) {\n        return indexOf(array, valueToFind, 0);\n    }"
            ],
            [
              "ArrayUtils.java",
              "    public static int indexOf(long[] array, long valueToFind, int startIndex) {\n        if (array == null) {\n            return INDEX_NOT_FOUND;\n        }\n        if (startIndex < 0) {\n            startIndex = 0;\n        }\n        for (int i = startIndex; i < array.length; i++) {\n            if (valueToFind == array[i]) {\n                return i;\n            }\n        }\n        return INDEX_NOT_FOUND;\n    }"
            ]
          ]
        },
        {
          "name": "substring",
          "source_file": "StringUtils.java",
          "signature": "public static String substring(String str, int start)",
          "source": "    public static String substring(String str, int start) {\n        if (str == null) {\n            return null;\n        }\n\n        // handle negatives, which means last n characters\n        if (start < 0) {\n            start = str.length() + start; // remember start is negative\n        }\n\n        if (start < 0) {\n            start = 0;\n        }\n        if (start > str.length()) {\n            return EMPTY;\n        }\n\n        return str.substring(start);\n    }",
          "is_abstract": false,
          "impls": [
            [
              "StringUtils.java",
              "    public static String substring(String str, int start) {\n        if (str == null) {\n            return null;\n        }\n\n        // handle negatives, which means last n characters\n        if (start < 0) {\n            start = str.length() + start; // remember start is negative\n        }\n\n        if (start < 0) {\n            start = 0;\n        }\n        if (start > str.length()) {\n            return EMPTY;\n        }\n\n        return str.substring(start);\n    }"
            ],
            [
              "StringUtils.java",
              "    public static String substring(String str, int start, int end) {\n        if (str == null) {\n            return null;\n        }\n\n        // handle negatives\n        if (end < 0) {\n            end = str.length() + end; // remember end is negative\n        }\n        if (start < 0) {\n            start = str.length() + start; // remember start is negative\n        }\n\n        // check length next\n        if (end > str.length()) {\n            end = str.length();\n        }\n\n        // if start is greater than end, return \"\"\n        if (start > end) {\n            return EMPTY;\n        }\n\n        if (start < 0) {\n            start = 0;\n        }\n        if (end < 0) {\n            end = 0;\n        }\n\n        return str.substring(start, end);\n    }"
            ]
          ]
        },
        {
          "name": "defaultString",
          "source_file": "StringUtils.java",
          "signature": "public static String defaultString(String str)",
          "source": "    public static String defaultString(String str) {\n        return str == null ? EMPTY : str;\n    }",
          "is_abstract": false,
          "impls": [
            [
              "StringUtils.java",
              "    public static String defaultString(String str) {\n        return str == null ? EMPTY : str;\n    }"
            ],
            [
              "StringUtils.java",
              "    public static String defaultString(String str, String defaultStr) {\n        return str == null ? defaultStr : str;\n    }"
            ]
          ]
        },
        {
          "name": "toString",
          "source_file": "ArrayUtils.java",
          "signature": "public static String toString(Object array)",
          "source": "    public static String toString(Object array) {\n        return toString(array, \"{}\");\n    }",
          "is_abstract": false,
          "impls": [
            [
              "ArrayUtils.java",
              "    public static String toString(Object array) {\n        return toString(array, \"{}\");\n    }"
            ],
            [
              "ArrayUtils.java",
              "    public static String toString(Object array, String stringIfNull) {\n        if (array == null) {\n            return stringIfNull;\n        }\n        return new ToStringBuilder(array, ToStringStyle.SIMPLE_STYLE).append(array).toString();\n    }"
            ],
            [
              "BooleanUtils.java",
              "    public static String toString(Boolean bool, String trueString, String falseString, String nullString) {\n        if (bool == null) {\n            return nullString;\n        }\n        return bool.booleanValue() ? trueString : falseString;\n    }"
            ],
            [
              "BooleanUtils.java",
              "    public static String toString(boolean bool, String trueString, String falseString) {\n        return bool ? trueString : falseString;\n    }"
            ]
          ]
        }
      ],
      "field_siblings": []
    }
  ],
  "package": "org.apache.commons.lang",
  "root_cause_reachable": [
    "StringUtils.indexOf",
    "StringUtils.defaultString"
  ],
  "neighbourhood_notes": [],
  "source_imports": [],
  "reachable_edges": [
    [
      "[org.apache.commons.lang.WordUtils].abbreviate(String,int,int,String)",
      "[String].length()"
    ],
    [
      "[org.apache.commons.lang.WordUtils].abbreviate(String,int,int,String)",
      "[StringBuffer].<init>()"
    ],
    [
      "[org.apache.commons.lang.WordUtils].abbreviate(String,int,int,String)",
      "[org.apache.commons.lang.StringUtils].indexOf(String,String,int)"
    ],
    [
      "[org.apache.commons.lang.WordUtils].abbreviate(String,int,int,String)",
      "[String].substring(int,org.apache.commons.lang.WordUtils)"
    ],
    [
      "[org.apache.commons.lang.WordUtils].abbreviate(String,int,int,String)",
      "[StringBuffer].append(org.apache.commons.lang.WordUtils)"
    ],
    [
      "[org.apache.commons.lang.WordUtils].abbreviate(String,int,int,String)",
      "[org.apache.commons.lang.StringUtils].defaultString(String)"
    ],
    [
      "[org.apache.commons.lang.WordUtils].abbreviate(String,int,int,String)",
      "[StringBuffer].append(String)"
    ],
    [
      "[org.apache.commons.lang.WordUtils].abbreviate(String,int,int,String)",
      "[String].substring(int,int)"
    ],
    [
      "[org.apache.commons.lang.WordUtils].abbreviate(String,int,int,String)",
      "[StringBuffer].toString()"
    ],
    [
      "[org.apache.commons.lang.StringUtils].indexOf(String,String,int)",
      "[String].length()"
    ],
    [
      "[org.apache.commons.lang.StringUtils].indexOf(String,String,int)",
      "[String].indexOf(String,int)"
    ]
  ],
  "reachable_depth": {
    "[org.apache.commons.lang.WordUtils].abbreviate(String,int,int,String)": 0,
    "[String].length()": 1,
    "[StringBuffer].<init>()": 1,
    "[org.apache.commons.lang.StringUtils].indexOf(String,String,int)": 1,
    "[String].substring(int,org.apache.commons.lang.WordUtils)": 1,
    "[StringBuffer].append(org.apache.commons.lang.WordUtils)": 1,
    "[org.apache.commons.lang.StringUtils].defaultString(String)": 1,
    "[StringBuffer].append(String)": 1,
    "[String].substring(int,int)": 1,
    "[StringBuffer].toString()": 1,
    "[String].indexOf(String,int)": 2
  }
}
```

</details>

---
## [3] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~18,260 chars)</summary>

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
- Package: `org.apache.commons.lang` (`package org.apache.commons.lang;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Lang_45_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/java/org/apache/commons/lang/WordUtils.java
+++ /src/java/org/apache/commons/lang/WordUtils.java
@@ -622,7 +622,9 @@
         if (upper < lower) {
             upper = lower;
         }
-
+        if (upper == -1 || upper > str.length()) {
+        	       	  upper=str.length();
+        }
         StringBuffer result = new StringBuffer();
         int index = StringUtils.indexOf(str, " ", lower);
         if (index == -1) {


</patch>

Function `abbreviate`:
<signature>
public static String abbreviate(String str, int lower, int upper, String appendToEnd)
</signature>
<code>
    public static String abbreviate(String str, int lower, int upper, String appendToEnd) {
        // initial parameter checks
        if (str == null) {
            return null;
        }
        if (str.length() == 0) {
            return StringUtils.EMPTY;
        }

        // if the lower value is greater than the length of the string,
        // set to the length of the string
        // if the upper value is -1 (i.e. no limit) or is greater
        // than the length of the string, set to the length of the string
        if (upper == -1 || upper > str.length()) {
            upper = str.length();
        }
        // if upper is less than lower, raise it to lower
        if (upper < lower) {
            upper = lower;
        }

        StringBuffer result = new StringBuffer();
        int index = StringUtils.indexOf(str, " ", lower);
        if (index == -1) {
            result.append(str.substring(0, upper));
            // only if abbreviation has occured do we append the appendToEnd value
            if (upper != str.length()) {
                result.append(StringUtils.defaultString(appendToEnd));
            }
        } else if (index > upper) {
            result.append(str.substring(0, upper));
            result.append(StringUtils.defaultString(appendToEnd));
        } else {
            result.append(str.substring(0, index));
            result.append(StringUtils.defaultString(appendToEnd));
        }
        return result.toString();
    }
</code>

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `WordUtils`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.StringIndexOutOfBoundsException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `abbreviate`. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
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
message: String index out of range: 15
thrown_at: org.apache.commons.lang.WordUtils.abbreviate(WordUtils.java:629)
</ground_truth_crash>
Trigger lines: numeric argument exceeds the string length in the same call. Mirror these calls and use FuzzedDataProvider to vary the numeric arguments at or beyond the string length:
<key_calls class="org.apache.commons.lang.WordUtilsTest" method="testAbbreviate">
assertEquals("0123456789", WordUtils.abbreviate("0123456789", 15, 20, null));
</key_calls>
<failing_test class="org.apache.commons.lang.WordUtilsTest" method="testAbbreviate">
    public void testAbbreviate() {
        // check null and empty are returned respectively
        assertNull(WordUtils.abbreviate(null, 1,-1,""));
        assertEquals(StringUtils.EMPTY, WordUtils.abbreviate("", 1,-1,""));

        // test upper limit
        assertEquals("01234", WordUtils.abbreviate("0123456789", 0,5,""));
        assertEquals("01234", WordUtils.abbreviate("0123456789", 5, 2,""));
        assertEquals("012", WordUtils.abbreviate("012 3456789", 2, 5,""));
        assertEquals("012 3", WordUtils.abbreviate("012 3456789", 5, 2,""));
        assertEquals("0123456789", WordUtils.abbreviate("0123456789", 0,-1,""));

        // test upper limit + append string
        assertEquals("01234-", WordUtils.abbreviate("0123456789", 0,5,"-"));
        assertEquals("01234-", WordUtils.abbreviate("0123456789", 5, 2,"-"));
        assertEquals("012", WordUtils.abbreviate("012 3456789", 2, 5, null));
        assertEquals("012 3", WordUtils.abbreviate("012 3456789", 5, 2,""));
        assertEquals("0123456789", WordUtils.abbreviate("0123456789", 0,-1,""));

        // test lower value
        assertEquals("012", WordUtils.abbreviate("012 3456789", 0,5, null));
        assertEquals("01234", WordUtils.abbreviate("01234 56789", 5, 10, null));
        assertEquals("01 23 45 67", WordUtils.abbreviate("01 23 45 67 89", 9, -1, null));
        assertEquals("01 23 45 6", WordUtils.abbreviate("01 23 45 67 89", 9, 10, null));
        assertEquals("0123456789", WordUtils.abbreviate("01
        // ... (truncated)
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.lang.WordUtilsTest::testAbbreviate
java.lang.StringIndexOutOfBoundsException: String index out of range: 15
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  capitalize(String str) / (String str, char[] delimiters)
  capitalizeFully(String str) / (String str, char[] delimiters)
  initials(String str) / (String str, char[] delimiters)
  uncapitalize(String str) / (String str, char[] delimiters)
  wrap(String str, int wrapLength) / (String str, int wrapLength, String newLineStr, boolean wrapLongWords)
  wrapText(String str, int lineLength) / (String str, String newLineChars, int lineLength)

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
package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~5,000 chars)</summary>

```
package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        // Anchor: exact trigger from WordUtilsTest.testAbbreviate.
        // Documented guarantee in abbreviate's own comments/code:
        // - if lower > str.length(), lower is effectively beyond the searchable range
        // - if upper == -1 or upper > str.length(), upper must be set to str.length()
        // Therefore abbreviate("0123456789", 15, 20, null) must return the full original string,
        // not throw. A "fix" that merely suppresses the crash or skips the normalization would violate this.
        exerciseAndCheck("0123456789", 15, 20, null, "0123456789", true);

        // Explore the root cause with many real inputs:
        // non-null, non-empty strings; lower at or beyond the string length; upper == -1 or > length.
        // For every correct implementation, the method must clamp upper to str.length() and return the
        // original string unchanged.
        String str = data.consumeAsciiString(64);
        if (str.length() == 0) {
            str = "A";
        }

        int len = str.length();
        int lower = len + data.consumeInt(0, 32);

        int upper;
        if (data.consumeBoolean()) {
            upper = -1;
        } else {
            upper = len + data.consumeInt(1, 32);
            if (data.consumeBoolean()) {
                // Also explore the case upper < lower before normalization.
                upper = Math.min(upper, lower - data.consumeInt(0, 8));
            }
        }

        String appendToEnd = data.consumeBoolean() ? null : data.consumeAsciiString(8);

        exerciseAndCheck(str, lower, upper, appendToEnd, str, true);

        // A second varied call uses the same real API and same root property on a related input with
        // surrounding content changes, including possible spaces.
        String extra = data.consumeAsciiString(16);
        String str2 = str + extra;
        if (str2.length() == 0) {
            str2 = "B";
        }
        int len2 = str2.length();
        int lower2 = len2 + data.consumeInt(0, 32);
        int upper2 = data.consumeBoolean() ? -1 : len2 + data.consumeInt(1, 32);
        exerciseAndCheck(str2, lower2, upper2, appendToEnd, str2, true);
    }

    private static void exerciseAndCheck(String str, int lower, int upper, String appendToEnd,
                                         String expected, boolean validByConstruction) {
        try {
            String actual = WordUtils.abbreviate(str, lower, upper, appendToEnd);
            if (!safeEquals(expected, actual)) {
                throw new RuntimeException(
                    "[oracle:abbrev-clamp] metamorphic violation: expected full string when lower>=len and upper is -1 or >len"
                        + " input=" + describe(str, lower, upper, appendToEnd)
                        + " lhs=" + expected
                        + " rhs=" + actual
                );
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (validByConstruction && isRootCause(t)) {
                sneakyThrow(t);
            }
            // Any other throwable is outside this patch's scope or from invalid/unspecified behavior.
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            if ("org.apache.commons.lang.WordUtils".equals(e.getClassName())
                    && "abbreviate".equals(e.getMethodName())) {
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
        String name = t.getClass().getName();
        return name.contains("Validation")
                || name.contains("Invalid")
                || name.contains("Illegal");
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String describe(String str, int lower, int upper, String appendToEnd) {
        return "{str=" + String.valueOf(str)
                + ", lower=" + lower
                + ", upper=" + upper
                + ", appendToEnd=" + String.valueOf(appendToEnd)
                + "}";
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}
```

</details>

---
## [4] ⚙️ harness-attempt · `attempt_001`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.StringIndexOutOfBoundsException: String index out of range: 15

---
## [5] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,697 chars)</summary>

```
package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();

        String str = data.consumeAsciiString(40);
        if (str == null || str.length() == 0) {
            str = "A";
        }

        int len = str.length();
        int lower = len + data.consumeInt(0, 50);
        int upper = lower + data.consumeInt(0, 50);
        String appendToEnd = data.consumeBoolean() ? null : data.consumeAsciiString(10);

        exerciseValidCase(str, lower, upper, appendToEnd);

        if (data.consumeBoolean()) {
            exerciseValidCase(str, lower, -1, appendToEnd);
        }
    }

    private static void anchor() {
        final String s = "0123456789";
        try {
            String out = WordUtils.abbreviate(s, 15, 20, null);
            if (!s.equals(out)) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: lower/upper beyond length must return original string input="
                        + s + " lhs=" + out + " rhs=" + s);
            }
        } catch (RuntimeException t) {
            handleThrowable(t, true);
        }
    }

    private static void exerciseValidCase(String str, int lower, int upper, String appendToEnd) {
        try {
            String out = WordUtils.abbreviate(str, lower, upper, appendToEnd);

            /*
             * Contract asserted: the method's own tests require
             * WordUtils.abbreviate("0123456789", 15, 20, null) == "0123456789".
             * More generally, when lower >= str.length() and upper is also at/above it,
             * a correct implementation clamps to the string length and returns the original
             * string without appending appendToEnd. A patch that merely suppresses the crash
             * but returns a truncated/modified value would violate this observable result.
             */
            if (!str.equals(out)) {
                throw new RuntimeException("[oracle:result] metamorphic violation: lower/upper at or beyond string length must yield original string input="
                        + printable(str) + " lower=" + lower + " upper=" + upper + " append="
                        + printable(appendToEnd) + " lhs=" + printable(out) + " rhs=" + printable(str));
            }

            if (upper != -1) {
                try {
                    String outUnlimited = WordUtils.abbreviate(str, lower, -1, appendToEnd);
                    if (!out.equals(outUnlimited)) {
                        throw new RuntimeException("[oracle:unlimited] metamorphic violation: for lower beyond end, explicit upper beyond end and upper=-1 must agree input="
                                + printable(str) + " lower=" + lower + " upper=" + upper + " append="
                                + printable(appendToEnd) + " lhs=" + printable(out) + " rhs=" + printable(outUnlimited));
                    }
                } catch (RuntimeException t) {
                    handleThrowable(t, false);
                }
            }
        } catch (RuntimeException t) {
            handleThrowable(t, true);
        }
    }

    private static void handleThrowable(RuntimeException t, boolean validByConstruction) {
        if (isOracleFailure(t)) {
            throw t;
        }
        if (isCleanRejection(t)) {
            return;
        }
        if (validByConstruction && isRootCause(t)) {
            throw t;
        }
    }

    private static boolean isOracleFailure(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Invalid") || name.contains("Validation");
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.lang.WordUtils".equals(e.getClassName())
                    && "abbreviate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static String printable(String s) {
        return s == null ? "null" : s;
    }
}
```

</details>

---
## [6] ⚙️ harness-attempt · `attempt_002`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.StringIndexOutOfBoundsException: String index out of range: 15

---
## [7] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,406 chars)</summary>

```
package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact failing test input first. On the buggy build this is the verified crash:
        // WordUtils.abbreviate("0123456789", 15, 20, null) -> StringIndexOutOfBoundsException from abbreviate.
        // Contract/post-condition asserted here: the failing test documents that this call must return
        // "0123456789". A throw-deleting or branch-skipping patch that returns the wrong substring would violate it.
        try {
            String anchor = WordUtils.abbreviate("0123456789", 15, 20, null);
            if (!"0123456789".equals(anchor)) {
                throw new RuntimeException("[oracle:anchor-value] metamorphic violation: documented test expectation input=(\"0123456789\",15,20,null) lhs="
                        + String.valueOf(anchor) + " rhs=0123456789");
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }

        String base = data.consumeString(64);
        if (base == null) {
            base = "";
        }
        if (base.length() == 0) {
            base = "x";
        }

        String append = data.consumeBoolean() ? null : data.consumeString(8);

        int len = base.length();

        // EXPLORE strategy:
        // Build valid-by-construction inputs that satisfy the patched-line property:
        // upper is -1 or greater than str.length(). The method's own contract/comments say such upper
        // values are to be treated as str.length(), so a correct implementation must accept them.
        int lower;
        if (data.consumeBoolean()) {
            lower = data.consumeInt(0, len);
        } else {
            lower = data.consumeInt(len + 1, len + 32);
        }

        int upperChoice = data.consumeInt(0, 2);
        int upper;
        if (upperChoice == 0) {
            upper = -1;
        } else if (upperChoice == 1) {
            upper = len + 1 + data.consumeInt(0, 32);
        } else {
            upper = len + data.consumeInt(1, 32);
        }

        // Direct exploration call through the real public API.
        try {
            WordUtils.abbreviate(base, lower, upper, append);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        // Mandatory metamorphic/post-condition check:
        // Documented guarantee in the shown method body:
        // "if the upper value is -1 (i.e. no limit) or is greater than the length of the string,
        // set to the length of the string"
        // Therefore, for the same non-null string/lower/append, abbreviate(str, lower, upper>len, append)
        // must equal abbreviate(str, lower, -1, append). If a patch merely suppresses the crash but
        // computes the wrong result, this sibling-equivalent-input relation catches it.
        try {
            String lhs = WordUtils.abbreviate(base, lower, upper, append);
            String rhs = WordUtils.abbreviate(base, lower, -1, append);
            if (lhs != null ? !lhs.equals(rhs) : rhs != null) {
                throw new RuntimeException("[oracle:upper-cap] metamorphic violation: equivalent upper values should agree input="
                        + describe(base, lower, upper, append) + " lhs=" + String.valueOf(lhs) + " rhs=" + String.valueOf(rhs));
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        // Additional equivalent-input check with an explicit oversized upper bound.
        int oversizedUpper = len + 1 + data.consumeInt(0, 64);
        try {
            String lhs = WordUtils.abbreviate(base, lower, oversizedUpper, append);
            String rhs = WordUtils.abbreviate(base, lower, len, append);
            if (lhs != null ? !lhs.equals(rhs) : rhs != null) {
                throw new RuntimeException("[oracle:upper-equals-len] metamorphic violation: upper>length should behave like upper==length input="
                        + describe(base, lower, oversizedUpper, append) + " lhs=" + String.valueOf(lhs) + " rhs=" + String.valueOf(rhs));
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

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name != null) {
                String lower = name.toLowerCase();
                if (lower.contains("validation") || lower.contains("invalid") || lower.contains("malformed")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.lang.WordUtils".equals(e.getClassName())
                    && "abbreviate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static String describe(String str, int lower, int upper, String append) {
        return "(strLen=" + (str == null ? -1 : str.length())
                + ",str=" + String.valueOf(str)
                + ",lower=" + lower
                + ",upper=" + upper
                + ",append=" + String.valueOf(append)
                + ")";
    }
}
```

</details>

---
## [8] ⚙️ harness-attempt · `attempt_003`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.StringIndexOutOfBoundsException: String index out of range: 15

---
## [9] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,337 chars)</summary>

```
package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact regression input from WordUtilsTest.testAbbreviate.
        // This is valid by construction according to the test's documented expected result:
        // abbreviate("0123456789", 15, 20, null) must return "0123456789".
        try {
            WordUtils.abbreviate("0123456789", 15, 20, null);
        } catch (Throwable t) {
            if (isOracleViolation(t)) {
                throw (RuntimeException) t;
            }
            if (isValidationThrowable(t)) {
                return;
            }
            if (isRootCauseFromAbbreviate(t) && isValidByConstruction("0123456789", 15, 20)) {
                throw propagate(t);
            }
            return;
        }

        String s = buildNonEmptyString(data);
        int len = s.length();

        // EXPLORE: generate many real inputs satisfying the root-cause property:
        // upper is -1 or exceeds str.length(), especially with lower at or beyond str.length().
        int lower;
        if (data.consumeBoolean()) {
            lower = len + data.consumeInt(0, 32);
        } else {
            lower = data.consumeInt(0, len + 32);
        }

        int upper;
        if (data.consumeBoolean()) {
            upper = -1;
        } else {
            upper = len + data.consumeInt(1, 32);
        }

        String appendToEnd = data.consumeBoolean() ? null : data.consumeString(8);

        try {
            WordUtils.abbreviate(s, lower, upper, appendToEnd);
        } catch (Throwable t) {
            if (isOracleViolation(t)) {
                throw (RuntimeException) t;
            }
            if (isValidationThrowable(t)) {
                return;
            }
            if (isRootCauseFromAbbreviate(t) && isValidByConstruction(s, lower, upper)) {
                throw propagate(t);
            }
            return;
        }

        // Metamorphic oracle:
        // Contract visible in the method body/comments: "if the upper value is -1
        // (i.e. no limit) or is greater than the length of the string, set to the
        // length of the string". Therefore, for the same (str, lower, appendToEnd),
        // replacing upper=-1 with any upper>str.length() must produce the same result.
        // A patch that merely deletes/guards the crashing path but returns a wrong
        // substring would violate this observable equivalence without throwing.
        int lowerForOracle = len + data.consumeInt(0, 32);
        int beyondLenUpper = len + data.consumeInt(1, 32);
        String oracleAppend = data.consumeBoolean() ? null : data.consumeString(8);

        String lhs;
        String rhs;
        try {
            lhs = WordUtils.abbreviate(s, lowerForOracle, -1, oracleAppend);
        } catch (Throwable t) {
            if (isValidationThrowable(t)) {
                return;
            }
            if (isRootCauseFromAbbreviate(t) && isValidByConstruction(s, lowerForOracle, -1)) {
                throw propagate(t);
            }
            return;
        }
        try {
            rhs = WordUtils.abbreviate(s, lowerForOracle, beyondLenUpper, oracleAppend);
        } catch (Throwable t) {
            if (isValidationThrowable(t)) {
                return;
            }
            if (isRootCauseFromAbbreviate(t) && isValidByConstruction(s, lowerForOracle, beyondLenUpper)) {
                throw propagate(t);
            }
            return;
        }

        if (!safeEquals(lhs, rhs)) {
            throw new RuntimeException(
                "[oracle:upper-clamp] metamorphic violation: abbreviate(str, lower, -1, append) must equal abbreviate(str, lower, upper>len, append)"
                    + " input=" + printable(s)
                    + " lower=" + lowerForOracle
                    + " upper=" + beyondLenUpper
                    + " append=" + printable(oracleAppend)
                    + " lhs=" + printable(lhs)
                    + " rhs=" + printable(rhs));
        }
    }

    private static String buildNonEmptyString(FuzzedDataProvider data) {
        String a = data.consumeString(24);
        String b = data.consumeAsciiString(24);
        String s;
        if (data.consumeBoolean()) {
            s = a + " " + b;
        } else if (data.consumeBoolean()) {
            s = a + b;
        } else {
            s = b + " " + a;
        }
        if (s == null || s.length() == 0) {
            s = "A B";
        }
        return s;
    }

    private static boolean isRootCauseFromAbbreviate(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement ste = trace[i];
            if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName())
                    && "abbreviate".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValidationThrowable(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isValidByConstruction(String s, int lower, int upper) {
        if (s == null || s.length() == 0) {
            return false;
        }
        if (lower < 0) {
            return false;
        }
        return upper == -1 || upper > s.length();
    }

    private static RuntimeException propagate(Throwable t) {
        if (t instanceof RuntimeException) {
            return (RuntimeException) t;
        }
        return new RuntimeException(t);
    }

    private static boolean isOracleViolation(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String printable(String s) {
        return String.valueOf(s);
    }
}
```

</details>

---
## [10] ⚙️ harness-attempt · `attempt_004`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.StringIndexOutOfBoundsException: String index out of range: 15

---
## [11] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,210 chars)</summary>

```
package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact trigger from the failing test. On the buggy version this reaches
        // WordUtils.abbreviate and throws StringIndexOutOfBoundsException.
        try {
            String anchor = WordUtils.abbreviate("0123456789", 15, 20, null);
            if (!"0123456789".equals(anchor)) {
                throw new RuntimeException(
                    "[oracle:anchor-return] metamorphic violation: documented clamping of out-of-range bounds should make this call return the original string input=0123456789 lhs="
                        + String.valueOf(anchor) + " rhs=0123456789");
            }
        } catch (RuntimeException t) {
            boolean cleanRejection =
                t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            if (cleanRejection) {
                return;
            }
            boolean throughAbbreviate = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName())
                        && "abbreviate".equals(ste.getMethodName())) {
                    throughAbbreviate = true;
                    break;
                }
            }
            if (t instanceof StringIndexOutOfBoundsException && throughAbbreviate) {
                throw t;
            }
            return;
        }

        // EXPLORE: root cause property is a numeric bound at or beyond str.length().
        // Build VALID-BY-CONSTRUCTION inputs:
        //   - non-null, non-empty strings
        //   - no spaces, so a correct implementation must take the "index == -1" path
        //   - lower >= str.length()
        //   - upper == -1 or upper > str.length()
        //
        // Post-condition asserted below comes directly from the method contract/comments:
        // "if the lower value is greater than the length of the string, set to the length
        // of the string" and "if the upper value is -1 ... or is greater than the length
        // of the string, set to the length of the string".
        // For a no-space string, after those clamps there is no abbreviation point, so the
        // returned value must be the original string. A throw-deleting or branch-skipping
        // patch could avoid crashing yet still return the wrong value; this oracle catches that.
        String raw = data.consumeAsciiString(64);
        if (raw.length() == 0) {
            raw = "A";
        }
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == ' ') {
                sb.append('X');
            } else {
                sb.append(c);
            }
        }
        String str = sb.toString();
        if (str.length() == 0) {
            str = "A";
        }

        int len = str.length();
        int lower = len + data.consumeInt(0, 32);
        int upper = data.consumeBoolean() ? -1 : len + data.consumeInt(1, 32);
        String appendToEnd = data.consumeBoolean() ? null : data.consumeAsciiString(8);

        try {
            String result = WordUtils.abbreviate(str, lower, upper, appendToEnd);
            if (!str.equals(result)) {
                throw new RuntimeException(
                    "[oracle:no-space-clamp] metamorphic violation: no-space string with lower>=len and upper out of range must round to original string input="
                        + str + " lhs=" + String.valueOf(result) + " rhs=" + str);
            }
        } catch (RuntimeException t) {
            boolean cleanRejection =
                t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            if (cleanRejection) {
                return;
            }
            boolean throughAbbreviate = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName())
                        && "abbreviate".equals(ste.getMethodName())) {
                    throughAbbreviate = true;
                    break;
                }
            }
            if (t instanceof StringIndexOutOfBoundsException && throughAbbreviate) {
                throw t;
            }
            return;
        }

        // Additional metamorphic check using only real library calls:
        // for these valid-by-construction inputs, upper == -1 and upper > len are documented
        // to normalize to the same effective upper bound (str.length()), so both calls must agree.
        int otherUpper = len + data.consumeInt(1, 32);
        String append2 = data.consumeBoolean() ? null : data.consumeAsciiString(8);
        try {
            String lhs = WordUtils.abbreviate(str, lower, -1, append2);
            String rhs = WordUtils.abbreviate(str, lower, otherUpper, append2);
            if (!String.valueOf(lhs).equals(String.valueOf(rhs))) {
                throw new RuntimeException(
                    "[oracle:upper-equivalence] metamorphic violation: upper=-1 and upper>len should normalize to the same result input="
                        + str + " lhs=" + String.valueOf(lhs) + " rhs=" + String.valueOf(rhs));
            }
        } catch (RuntimeException t) {
            boolean cleanRejection =
                t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            if (cleanRejection) {
                return;
            }
            boolean throughAbbreviate = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName())
                        && "abbreviate".equals(ste.getMethodName())) {
                    throughAbbreviate = true;
                    break;
                }
            }
            if (t instanceof StringIndexOutOfBoundsException && throughAbbreviate) {
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
- trigger: java.lang.StringIndexOutOfBoundsException: String index out of range: 15

---
## [13] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,421 chars)</summary>

```
package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            String anchor = WordUtils.abbreviate("0123456789", 15, 20, null);
            if (!"0123456789".equals(anchor)) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: exact regression input returned wrong value input=0123456789,15,20,null lhs=" + String.valueOf(anchor) + " rhs=0123456789");
            }
        } catch (RuntimeException t) {
            boolean cleanRejection =
                    t instanceof IllegalArgumentException ||
                    t instanceof NumberFormatException;
            if (cleanRejection) {
                return;
            }
            boolean throughTarget = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName())
                        && "abbreviate".equals(ste.getMethodName())) {
                    throughTarget = true;
                    break;
                }
            }
            if (throughTarget && t instanceof StringIndexOutOfBoundsException) {
                throw t;
            }
        }

        String str = data.consumeAsciiString(40);
        if (str.length() == 0) {
            str = "A";
        }
        String appendToEnd = data.consumeBoolean() ? null : data.consumeAsciiString(8);
        int len = str.length();
        int lower = data.consumeInt(0, len + 20);

        int exploredUpper;
        if (data.consumeBoolean()) {
            exploredUpper = -1;
        } else {
            exploredUpper = len + data.consumeInt(1, 20);
        }

        String exploredResult;
        try {
            exploredResult = WordUtils.abbreviate(str, lower, exploredUpper, appendToEnd);
        } catch (RuntimeException t) {
            boolean cleanRejection =
                    t instanceof IllegalArgumentException ||
                    t instanceof NumberFormatException;
            if (cleanRejection) {
                return;
            }
            boolean throughTarget = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName())
                        && "abbreviate".equals(ste.getMethodName())) {
                    throughTarget = true;
                    break;
                }
            }
            if (throughTarget && t instanceof StringIndexOutOfBoundsException) {
                throw t;
            }
            return;
        }

        String canonicalAtLength;
        try {
            canonicalAtLength = WordUtils.abbreviate(str, lower, len, appendToEnd);
        } catch (Throwable ignored) {
            return;
        }

        if (!String.valueOf(exploredResult).equals(String.valueOf(canonicalAtLength))) {
            throw new RuntimeException(
                    "[oracle:upper-cap] metamorphic violation: documented contract says upper==-1 or upper>str.length() is treated as upper=str.length() input="
                            + str + "|" + lower + "|" + exploredUpper + "|" + String.valueOf(appendToEnd)
                            + " lhs=" + String.valueOf(exploredResult)
                            + " rhs=" + String.valueOf(canonicalAtLength));
        }

        if (data.consumeBoolean()) {
            String noLimitResult;
            String exactLengthResult;
            try {
                noLimitResult = WordUtils.abbreviate(str, lower, -1, appendToEnd);
                exactLengthResult = WordUtils.abbreviate(str, lower, len, appendToEnd);
            } catch (Throwable ignored) {
                return;
            }

            if (!String.valueOf(noLimitResult).equals(String.valueOf(exactLengthResult))) {
                throw new RuntimeException(
                        "[oracle:no-limit] metamorphic violation: documented contract says upper==-1 means no limit and is treated as str.length() input="
                                + str + "|" + lower + "|" + String.valueOf(appendToEnd)
                                + " lhs=" + String.valueOf(noLimitResult)
                                + " rhs=" + String.valueOf(exactLengthResult));
            }
        }
    }
}
```

</details>

---
## [14] ⚙️ harness-attempt · `attempt_006`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.StringIndexOutOfBoundsException: String index out of range: 15

---
## [15] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,966 chars)</summary>

```
package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            String anchored = WordUtils.abbreviate("0123456789", 15, 20, null);
            if (!"0123456789".equals(anchored)) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: documented/tested out-of-range bounds should abbreviate to the full original string input=0123456789 lhs=" + anchored + " rhs=0123456789");
            }
        } catch (Throwable t) {
            boolean throughTarget = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName())
                        && "abbreviate".equals(ste.getMethodName())) {
                    throughTarget = true;
                    break;
                }
            }
            if ((t instanceof IllegalArgumentException) || (t instanceof NumberFormatException)) {
                return;
            }
            if (throughTarget && (t instanceof StringIndexOutOfBoundsException)) {
                throw (RuntimeException) t;
            }
            return;
        }

        String base = data.consumeAsciiString(32);
        if (base.length() == 0) {
            base = "X";
        }
        if (data.consumeBoolean()) {
            String left = data.consumeAsciiString(8);
            String right = data.consumeAsciiString(8);
            if (left.length() == 0) {
                left = "L";
            }
            if (right.length() == 0) {
                right = "R";
            }
            base = left + " " + base + " " + right;
        }

        int len = base.length();
        int lower = len + data.consumeInt(1, 20);
        int upperBeyondLen = len + data.consumeInt(1, 20);
        String appendToEnd = data.consumeBoolean() ? null : data.consumeAsciiString(8);

        String lhs;
        try {
            lhs = WordUtils.abbreviate(base, lower, -1, appendToEnd);
        } catch (Throwable t) {
            boolean throughTarget = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName())
                        && "abbreviate".equals(ste.getMethodName())) {
                    throughTarget = true;
                    break;
                }
            }
            if ((t instanceof IllegalArgumentException) || (t instanceof NumberFormatException)) {
                return;
            }
            if (throughTarget && (t instanceof StringIndexOutOfBoundsException)) {
                throw (RuntimeException) t;
            }
            return;
        }

        String rhs;
        try {
            rhs = WordUtils.abbreviate(base, lower, upperBeyondLen, appendToEnd);
        } catch (Throwable t) {
            boolean throughTarget = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName())
                        && "abbreviate".equals(ste.getMethodName())) {
                    throughTarget = true;
                    break;
                }
            }
            if ((t instanceof IllegalArgumentException) || (t instanceof NumberFormatException)) {
                return;
            }
            if (throughTarget && (t instanceof StringIndexOutOfBoundsException)) {
                throw (RuntimeException) t;
            }
            return;
        }

        /*
         * Contract asserted from the method's own comments and the regression test:
         * - if lower is greater than the string length, it is treated as the string length
         * - if upper is -1 or greater than the string length, it is treated as the string length
         * Therefore, for any non-null/non-empty string with lower > length and upper == -1
         * or upper > length, the result must be the original string, with no append string added.
         * A patch that only suppresses the crash or special-cases one bound would violate this.
         */
        if (!base.equals(lhs)) {
            throw new RuntimeException("[oracle:full-string-minus1] metamorphic violation: lower>length and upper==-1 must return the original string input=" + base + " lhs=" + lhs + " rhs=" + base);
        }
        if (!base.equals(rhs)) {
            throw new RuntimeException("[oracle:full-string-overflow] metamorphic violation: lower>length and upper>length must return the original string input=" + base + " lhs=" + rhs + " rhs=" + base);
        }
        if (!lhs.equals(rhs)) {
            throw new RuntimeException("[oracle:equiv-upper] metamorphic violation: upper==-1 and upper>length are documented-equivalent here input=" + base + " lhs=" + lhs + " rhs=" + rhs);
        }
    }
}
```

</details>

---
## [16] ⚙️ harness-attempt · `attempt_007`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.StringIndexOutOfBoundsException: String index out of range: 15

---
## [17] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,214 chars)</summary>

```
package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact regression test input from WordUtilsTest.
        checkAbbreviateIdentity("0123456789", 15, 20, null, "anchor", true);

        // EXPLORE:
        // Documented contract from the method comments shown in the patch:
        // - "if the lower value is greater than the length of the string, set to the length of the string"
        // - "if the upper value is -1 ... or is greater than the length of the string, set to the length of the string"
        //
        // Therefore, for any non-null, non-empty string with lower >= str.length() and
        // upper == -1 or upper > str.length(), a correct implementation must behave as if
        // lower == upper == str.length(). Since StringUtils.indexOf(str, " ", lowerClamped)
        // then finds no space, result must be str.substring(0, str.length()) == str, and
        // appendToEnd must not be appended because no abbreviation occurred.
        //
        // This directly checks an observable post-condition; a "fix" that merely avoids the
        // crash but returns the wrong substring would violate it.
        String str = data.consumeString(64);
        if (str == null || str.length() == 0) {
            return;
        }

        int len = str.length();

        int lowerChoice = data.consumeInt(0, 2);
        int lower;
        if (lowerChoice == 0) {
            lower = len;
        } else if (lowerChoice == 1) {
            lower = len + data.consumeInt(1, 64);
        } else {
            lower = len + Math.abs(data.consumeInt() % 64);
            if (lower < len) {
                lower = len;
            }
        }

        String appendToEnd = data.consumeBoolean() ? null : "";

        int upperMode = data.consumeInt(0, 2);
        int upper;
        if (upperMode == 0) {
            upper = -1;
        } else if (upperMode == 1) {
            upper = len + data.consumeInt(1, 64);
        } else {
            upper = len + Math.abs(data.consumeInt() % 64) + 1;
        }

        checkAbbreviateIdentity(str, lower, upper, appendToEnd, "identity", true);

        // Additional boundary-focused variants around the exact property.
        checkAbbreviateIdentity(str, len, len + 1, null, "boundary-upper-plus-one", true);
        checkAbbreviateIdentity(str, len + 1, len + 1, "", "boundary-lower-upper-equal-past-end", true);
        checkAbbreviateIdentity(str, len + data.consumeInt(1, 8), -1, null, "boundary-upper-unlimited", true);
    }

    private static void checkAbbreviateIdentity(String str, int lower, int upper, String appendToEnd,
                                                String oracleId, boolean validByConstruction) {
        try {
            String result = WordUtils.abbreviate(str, lower, upper, appendToEnd);
            if (!str.equals(result)) {
                throw new RuntimeException(
                    "[oracle:" + oracleId + "] metamorphic violation: expected identity when lower>=len and upper==-1 or >len"
                        + " input=" + quote(str)
                        + " lower=" + lower
                        + " upper=" + upper
                        + " append=" + quote(appendToEnd)
                        + " lhs=" + quote(result)
                        + " rhs=" + quote(str));
            }
        } catch (RuntimeException t) {
            if (isOracleFailure(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            if (validByConstruction && isRootCause(t)) {
                throw t;
            }
        }
    }

    private static boolean isOracleFailure(RuntimeException t) {
        String msg = t.getMessage();
        return msg != null && msg.startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        for (Class<?> c = t.getClass(); c != null; c = c.getSuperclass()) {
            String n = c.getName();
            String s = c.getSimpleName();
            if (n.contains("Validation") || n.contains("Invalid") || s.contains("Validation") || s.contains("Invalid")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement ste = stack[i];
            if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName())
                    && "abbreviate".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static String quote(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }
}
```

</details>

---
## [18] ⚙️ harness-attempt · `attempt_008`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.StringIndexOutOfBoundsException: String index out of range: 15

---
## [19] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,475 chars)</summary>

```
package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            String anchorResult = WordUtils.abbreviate("0123456789", 15, 20, null);
            if (!"0123456789".equals(anchorResult)) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: documented clamp-to-length behaviour input=0123456789 lower=15 upper=20 append=null lhs=" + String.valueOf(anchorResult) + " rhs=0123456789");
            }
        } catch (RuntimeException t) {
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            boolean throughAbbreviate = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName())
                        && "abbreviate".equals(ste.getMethodName())) {
                    throughAbbreviate = true;
                    break;
                }
            }
            if (t instanceof StringIndexOutOfBoundsException && throughAbbreviate) {
                throw t;
            }
            if (t.getMessage() != null && t.getClass().getName().startsWith("org.apache.commons.lang") && throughAbbreviate) {
                return;
            }
            return;
        }

        String s = data.consumeAsciiString(64);
        if (s.length() == 0) {
            s = "A";
        }
        if (s.indexOf(' ') >= 0) {
            s = s.replace(' ', 'X');
        }

        int len = s.length();
        int lower = len + data.consumeInt(1, 32);
        int upper;
        if (data.consumeBoolean()) {
            upper = -1;
        } else {
            upper = len + data.consumeInt(1, 32);
        }

        String append = data.consumeBoolean() ? null : data.consumeAsciiString(8);

        try {
            String result = WordUtils.abbreviate(s, lower, upper, append);

            /*
             * Contract asserted from the method's documented behavior shown in the patch:
             * - if lower is greater than the length of the string, set to the length of the string
             * - if upper is -1 or greater than the length of the string, set to the length of the string
             * For a non-empty string constructed without spaces, indexOf(" ", lower==len) is -1,
             * substring(0, len) is the original string, and because upper==len no appendToEnd is added.
             * Therefore a correct implementation must return the original string exactly.
             * A "fix" that merely avoids the crash but returns a truncated/appended/empty value breaks this.
             */
            if (!s.equals(result)) {
                throw new RuntimeException("[oracle:no-space-clamp] metamorphic violation: oversize lower/upper on no-space string must return original input=" + s + " lower=" + lower + " upper=" + upper + " append=" + String.valueOf(append) + " lhs=" + String.valueOf(result) + " rhs=" + s);
            }
        } catch (RuntimeException t) {
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            boolean throughAbbreviate = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName())
                        && "abbreviate".equals(ste.getMethodName())) {
                    throughAbbreviate = true;
                    break;
                }
            }
            if (t instanceof StringIndexOutOfBoundsException && throughAbbreviate) {
                throw t;
            }
            if (t.getMessage() != null && t.getClass().getName().startsWith("org.apache.commons.lang") && throughAbbreviate) {
                return;
            }
            return;
        }

        String spacedCore = data.consumeAsciiString(48);
        if (spacedCore.length() == 0) {
            spacedCore = "B";
        }
        String spaced = "P " + spacedCore.replace('\n', ' ').replace('\r', ' ');
        if (spaced.length() < 3) {
            spaced = "P Q";
        }
        int spacedLen = spaced.length();
        int lower2 = spacedLen + data.consumeInt(1, 32);
        int upper2 = spacedLen + data.consumeInt(1, 32);
        String append2 = data.consumeBoolean() ? null : data.consumeAsciiString(8);

        try {
            WordUtils.abbreviate(spaced, lower2, upper2, append2);
        } catch (RuntimeException t) {
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            boolean throughAbbreviate = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName())
                        && "abbreviate".equals(ste.getMethodName())) {
                    throughAbbreviate = true;
                    break;
                }
            }
            if (t instanceof StringIndexOutOfBoundsException && throughAbbreviate) {
                throw t;
            }
            if (t.getMessage() != null && t.getClass().getName().startsWith("org.apache.commons.lang") && throughAbbreviate) {
                return;
            }
        }
    }
}
```

</details>

---
## [20] ⚙️ harness-attempt · `attempt_009`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.StringIndexOutOfBoundsException: String index out of range: 15

---
## [21] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,470 chars)</summary>

```
package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Contract asserted below comes directly from the method's own comments:
        // - "if the lower value is greater than the length of the string, set to the length of the string"
        // - "if the upper value is -1 ... or is greater than the length of the string, set to the length of the string"
        // For any non-null, non-empty string built that way, a correct implementation must return the original
        // string unchanged (substring(0, str.length()) with no append). A patch that merely suppresses the crash
        // or skips the bookkeeping would violate this observable post-condition.

        // ANCHOR: exact regression-test trigger first.
        try {
            String out = WordUtils.abbreviate("0123456789", 15, 20, null);
            if (!"0123456789".equals(out)) {
                throw new RuntimeException(
                    "[oracle:anchor] metamorphic violation: documented clamp-to-length contract input=0123456789,15,20,null lhs="
                        + out + " rhs=0123456789");
            }
        } catch (Throwable t) {
            handleThrowable(t, true);
        }

        // EXPLORE: varied real inputs satisfying the same root-cause property:
        // non-null, non-empty string; lower beyond length; upper == -1 or beyond length.
        String str = data.consumeAsciiString(64);
        if (str.length() == 0) {
            str = "A";
        }

        // Occasionally add spaces and surrounding content; lower remains beyond length, so correct behavior is unchanged.
        if (data.consumeBoolean()) {
            String extra = data.consumeAsciiString(16);
            if (extra.length() == 0) {
                extra = "B";
            }
            str = data.consumeBoolean() ? (str + " " + extra) : (extra + " " + str);
        }

        int len = str.length();
        int lower = len + data.consumeInt(1, 32);
        int upper;
        if (data.consumeBoolean()) {
            upper = -1;
        } else {
            upper = len + data.consumeInt(1, 32);
        }

        String appendToEnd = data.consumeBoolean() ? null : data.consumeAsciiString(8);

        try {
            String out = WordUtils.abbreviate(str, lower, upper, appendToEnd);
            if (!str.equals(out)) {
                throw new RuntimeException(
                    "[oracle:clamp] metamorphic violation: documented clamp-to-length contract input="
                        + printable(str) + "," + lower + "," + upper + "," + printable(appendToEnd)
                        + " lhs=" + printable(out) + " rhs=" + printable(str));
            }
        } catch (Throwable t) {
            handleThrowable(t, true);
        }
    }

    private static void handleThrowable(Throwable t, boolean validByConstruction) {
        if (t == null) {
            return;
        }

        if (isOracleViolation(t)) {
            throw (RuntimeException) t;
        }

        if (isCleanRejection(t)) {
            return;
        }

        if (validByConstruction && t instanceof StringIndexOutOfBoundsException && passesThroughAbbreviate(t)) {
            throw (StringIndexOutOfBoundsException) t;
        }

        // Outside this patch's scope or malformed-input rejection: swallow to avoid false positives.
    }

    private static boolean isOracleViolation(Throwable t) {
        return t instanceof RuntimeException
            && t.getMessage() != null
            && t.getMessage().startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
            || t instanceof NumberFormatException;
    }

    private static boolean passesThroughAbbreviate(Throwable t) {
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.lang.WordUtils".equals(e.getClassName())
                && "abbreviate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static String printable(String s) {
        return s == null ? "null" : s;
    }
}
```

</details>

---
## [22] ⚙️ harness-attempt · `attempt_010`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.StringIndexOutOfBoundsException: String index out of range: 15

---
## [23] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,409 chars)</summary>

```
package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact failing test input first. On the buggy build this reaches
        // WordUtils.abbreviate and throws StringIndexOutOfBoundsException from abbreviate.
        try {
            String anchor = WordUtils.abbreviate("0123456789", 15, 20, null);
            if (!"0123456789".equals(anchor)) {
                throw new RuntimeException("[oracle:anchor-eq] metamorphic violation: exact failing-test call must return the full original string input=0123456789 lower=15 upper=20 lhs=" + String.valueOf(anchor) + " rhs=0123456789");
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromAbbreviate(t)) {
                throwUnchecked(t);
            }
            return;
        }

        // EXPLORE:
        // Documented guarantee from the method body/comments:
        // - "if the upper value is -1 ... or is greater than the length of the string, set to the length of the string"
        // Therefore, for any non-null string and any fixed lower/appendToEnd,
        // abbreviate(str, lower, -1, append) and abbreviate(str, lower, upper>str.length(), append)
        // must produce the same observable result.
        // A patch that merely suppresses the crash or skips the intended clamp would violate this.
        String str = data.consumeString(64);
        if (str == null) {
            str = "";
        }

        String appendToEnd = data.consumeBoolean() ? null : data.consumeString(16);

        int len = str.length();
        int lower = data.consumeInt(0, len + 20);
        int upperBeyondLen = len + data.consumeInt(1, 20);

        String lhs;
        try {
            lhs = WordUtils.abbreviate(str, lower, -1, appendToEnd);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromAbbreviate(t)) {
                throwUnchecked(t);
            }
            return;
        }

        String rhs;
        try {
            rhs = WordUtils.abbreviate(str, lower, upperBeyondLen, appendToEnd);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromAbbreviate(t)) {
                throwUnchecked(t);
            }
            return;
        }

        if (!safeEquals(lhs, rhs)) {
            throw new RuntimeException("[oracle:abbrev-upper-clamp] metamorphic violation: abbreviate(str, lower, -1, append) must equal abbreviate(str, lower, upper>len, append) input="
                    + debug(str) + " lower=" + lower + " upper=" + upperBeyondLen + " append=" + debug(appendToEnd)
                    + " lhs=" + debug(lhs) + " rhs=" + debug(rhs));
        }

        // Additional direct post-condition on a non-degenerate no-space string built by construction:
        // if upper > str.length(), the code must clamp upper to str.length();
        // for a string with no spaces, that means the result is the full original string
        // (and no appendToEnd is added because no abbreviation occurred).
        String noSpace = data.consumeAsciiString(64).replace(' ', 'X');
        if (noSpace.length() == 0) {
            noSpace = "X";
        }
        int lower2 = data.consumeInt(noSpace.length(), noSpace.length() + 20);
        int upper2 = noSpace.length() + data.consumeInt(1, 20);

        try {
            String out = WordUtils.abbreviate(noSpace, lower2, upper2, null);
            if (!noSpace.equals(out)) {
                throw new RuntimeException("[oracle:abbrev-full] metamorphic violation: for a no-space string with upper>len, abbreviate must return the full original string input="
                        + debug(noSpace) + " lower=" + lower2 + " upper=" + upper2 + " lhs=" + debug(out) + " rhs=" + debug(noSpace));
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromAbbreviate(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException;
    }

    private static boolean isRootCauseFromAbbreviate(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement ste = trace[i];
            if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName())
                    && "abbreviate".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String debug(String s) {
        return String.valueOf(s);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(Throwable t) throws T {
        throw (T) t;
    }
}
```

</details>

---
## [24] ⚙️ harness-attempt · `attempt_011`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.StringIndexOutOfBoundsException: String index out of range: 15

---
## [25] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,332 chars)</summary>

```
package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact trigger from the failing test. On the buggy build this must reach
        // WordUtils.abbreviate and fail with StringIndexOutOfBoundsException there.
        try {
            String anchored = WordUtils.abbreviate("0123456789", 15, 20, null);
            if (!"0123456789".equals(anchored)) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: documented clamping for lower/upper beyond string length should return full string input=0123456789 lower=15 upper=20 lhs=" + anchored + " rhs=0123456789");
            }
        } catch (RuntimeException t) {
            if (isOracleFailure(t) || isRootCauseInAbbreviate(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        String left = data.consumeAsciiString(20);
        String right = data.consumeAsciiString(20);
        boolean addSpace = data.consumeBoolean();
        boolean addTrailingSpace = data.consumeBoolean();

        String str = left;
        if (addSpace) {
            str = str + " " + right;
        } else {
            str = str + right;
        }
        if (addTrailingSpace) {
            str = str + " ";
        }
        if (str.length() == 0) {
            str = "X";
        }

        int len = str.length();
        int lower = len + data.consumeInt(1, 32);
        int upper;
        if (data.consumeBoolean()) {
            upper = -1;
        } else {
            upper = len + data.consumeInt(1, 32);
        }

        String appendToEnd = null;
        if (data.consumeBoolean()) {
            appendToEnd = data.consumeAsciiString(8);
        }

        try {
            String result = WordUtils.abbreviate(str, lower, upper, appendToEnd);

            // Post-condition from the method's own documented behavior:
            // - "if the lower value is greater than the length of the string, set to the length of the string"
            // - "if the upper value is -1 ... or is greater than the length of the string, set to the length of the string"
            // With lower > len and upper in {-1, > len}, both clamp to len, indexOf(..., lower) is effectively beyond
            // the end, no abbreviation occurs, and the method must return the original string unchanged.
            if (!str.equals(result)) {
                throw new RuntimeException("[oracle:full-string] metamorphic violation: lower/upper clamped beyond end should yield original string input=" + printable(str) + " lower=" + lower + " upper=" + upper + " append=" + printable(appendToEnd) + " lhs=" + printable(result) + " rhs=" + printable(str));
            }

            // Metamorphic relation: for these valid-by-construction inputs, upper == -1 and any upper > len are
            // documented to clamp to the same effective upper bound (str.length()), so results must agree.
            try {
                String unlimited = WordUtils.abbreviate(str, lower, -1, appendToEnd);
                if (!result.equals(unlimited)) {
                    throw new RuntimeException("[oracle:upper-clamp] metamorphic violation: upper=-1 and upper>length should agree after clamping input=" + printable(str) + " lower=" + lower + " upper=" + upper + " append=" + printable(appendToEnd) + " lhs=" + printable(result) + " rhs=" + printable(unlimited));
                }
            } catch (RuntimeException inner) {
                if (isOracleFailure(inner)) {
                    throw inner;
                }
                if (isRootCauseInAbbreviate(inner)) {
                    throw inner;
                }
                if (isCleanRejection(inner)) {
                    return;
                }
                return;
            }
        } catch (RuntimeException t) {
            if (isOracleFailure(t) || isRootCauseInAbbreviate(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static boolean isOracleFailure(RuntimeException t) {
        String msg = t.getMessage();
        return msg != null && msg.startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCauseInAbbreviate(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.lang.WordUtils".equals(e.getClassName())
                    && "abbreviate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static String printable(String s) {
        return s == null ? "null" : s.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r");
    }
}
```

</details>

---
## [26] ⚙️ harness-attempt · `attempt_012`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.StringIndexOutOfBoundsException: String index out of range: 15

---
## [27] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,648 chars)</summary>

```
package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Contract asserted below:
        // In WordUtils.abbreviate, "if the upper value is -1 (i.e. no limit) or is greater
        // than the length of the string, set to the length of the string", and
        // "only if abbreviation has occured do we append the appendToEnd value".
        // Therefore, for a non-empty string with no spaces, when upper is -1 or >= str.length(),
        // the result must be exactly the original string, independent of appendToEnd.

        // ANCHOR: exact failing test input first.
        try {
            String anchor = WordUtils.abbreviate("0123456789", 15, 20, null);
            if (!"0123456789".equals(anchor)) {
                throw new RuntimeException(
                    "[oracle:anchor] metamorphic violation: exact regression input must return original string input=0123456789 lower=15 upper=20 append=null lhs="
                        + String.valueOf(anchor) + " rhs=0123456789");
            }
        } catch (RuntimeException t) {
            if (isOracleFailure(t) || isRootCauseFromAbbreviate(t)) {
                throw t;
            }
            return;
        }

        // EXPLORE: generate many real inputs with the triggering property:
        // non-empty strings without spaces, lower beyond the string length, and upper at or beyond
        // the string length (or -1). These are valid-by-construction for the documented clamping behavior.
        String raw = data.consumeAsciiString(64);
        String str = removeSpaces(raw);
        if (str.length() == 0) {
            str = "A";
        }

        int len = str.length();
        int lower = len + data.consumeInt(1, 32);
        int upper;
        if (data.consumeBoolean()) {
            upper = -1;
        } else {
            upper = len + data.consumeInt(0, 32);
        }

        String appendA = data.consumeBoolean() ? null : data.consumeAsciiString(16);
        String appendB = data.consumeBoolean() ? null : data.consumeAsciiString(16);

        String r1;
        try {
            r1 = WordUtils.abbreviate(str, lower, upper, appendA);
        } catch (RuntimeException t) {
            if (isRootCauseFromAbbreviate(t)) {
                throw t;
            }
            return;
        }

        if (!str.equals(r1)) {
            throw new RuntimeException(
                "[oracle:full-string] metamorphic violation: upper at or beyond length must clamp to full string for no-space input input="
                    + String.valueOf(str) + " lower=" + lower + " upper=" + upper + " append="
                    + String.valueOf(appendA) + " lhs=" + String.valueOf(r1) + " rhs=" + str);
        }

        String r2;
        try {
            r2 = WordUtils.abbreviate(str, lower, upper, appendB);
        } catch (RuntimeException t) {
            if (isRootCauseFromAbbreviate(t)) {
                throw t;
            }
            return;
        }

        if (!r1.equals(r2)) {
            throw new RuntimeException(
                "[oracle:append-ignored] metamorphic violation: appendToEnd must be ignored when no abbreviation occurs input="
                    + String.valueOf(str) + " lower=" + lower + " upper=" + upper + " lhs="
                    + String.valueOf(r1) + " rhs=" + String.valueOf(r2));
        }
    }

    private static boolean isOracleFailure(RuntimeException t) {
        String msg = t.getMessage();
        return msg != null && msg.startsWith("[oracle:");
    }

    private static boolean isRootCauseFromAbbreviate(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.lang.WordUtils".equals(e.getClassName())
                    && "abbreviate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static String removeSpaces(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuffer sb = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            sb.append(c == ' ' ? 'A' : c);
        }
        return sb.toString();
    }
}
```

</details>

---
## [28] ⚙️ harness-attempt · `attempt_013`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.StringIndexOutOfBoundsException: String index out of range: 15

---
## [29] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,275 chars)</summary>

```
package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            WordUtils.abbreviate("0123456789", 15, 20, null);
        } catch (RuntimeException t) {
            boolean inTarget = false;
            for (StackTraceElement e : t.getStackTrace()) {
                if ("org.apache.commons.lang.WordUtils".equals(e.getClassName())
                        && "abbreviate".equals(e.getMethodName())) {
                    inTarget = true;
                    break;
                }
            }
            if (t instanceof StringIndexOutOfBoundsException && inTarget) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
        }

        int rounds = 1;
        if (data.remainingBytes() > 0) {
            rounds += data.consumeInt(1, 4);
        }

        for (int i = 0; i < rounds; i++) {
            String base = data.consumeAsciiString(64);
            if (base == null || base.length() == 0) {
                base = "A";
            }

            String str;
            if (data.consumeBoolean()) {
                str = base.replace(' ', 'X');
                if (str.length() == 0) {
                    str = "B";
                }
            } else {
                String extra = data.consumeAsciiString(32);
                if (extra == null) {
                    extra = "";
                }
                if (base.length() == 0) {
                    base = "A";
                }
                str = base + " " + extra;
            }

            int len = str.length();
            int lower = data.consumeInt(0, len + 8);
            int upper;
            if (data.consumeBoolean()) {
                upper = -1;
            } else {
                upper = len + data.consumeInt(1, 8);
            }
            String appendToEnd = data.consumeBoolean() ? null : data.consumeAsciiString(8);

            try {
                WordUtils.abbreviate(str, lower, upper, appendToEnd);
            } catch (RuntimeException t) {
                boolean inTarget = false;
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.lang.WordUtils".equals(e.getClassName())
                            && "abbreviate".equals(e.getMethodName())) {
                        inTarget = true;
                        break;
                    }
                }
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    continue;
                }
                if (t instanceof StringIndexOutOfBoundsException && inTarget) {
                    throw t;
                }
                continue;
            }

            try {
                String lhs = WordUtils.abbreviate(str, lower, upper, appendToEnd);
                String rhs = WordUtils.abbreviate(str, lower, len, appendToEnd);

                /*
                 * Contract used for this oracle:
                 * the method states that if upper == -1 or upper > str.length(),
                 * upper is set to str.length(). Therefore, for any non-null, non-empty
                 * input we construct here, abbreviate(str, lower, upper, appendToEnd)
                 * must equal abbreviate(str, lower, str.length(), appendToEnd) whenever
                 * upper is -1 or beyond the string length. A throw-deleting or branch-
                 * skipping patch would break this observable equivalence.
                 */
                if (!StringUtils.equals(lhs, rhs)) {
                    throw new RuntimeException(
                            "[oracle:upper-clamp] metamorphic violation: abbreviate must clamp upper to string length"
                                    + " input=" + str
                                    + " lower=" + lower
                                    + " upper=" + upper
                                    + " append=" + appendToEnd
                                    + " lhs=" + lhs
                                    + " rhs=" + rhs);
                }

                if (str.indexOf(' ') == -1 && lower >= len && (upper == -1 || upper > len)) {
                    /*
                     * Additional direct post-condition from the code/comments:
                     * with no spaces after lower, indexOf returns -1; after clamping upper
                     * to str.length(), result is substring(0, len), i.e. the original string,
                     * and appendToEnd must not be appended because no abbreviation occurred.
                     */
                    if (!StringUtils.equals(lhs, str)) {
                        throw new RuntimeException(
                                "[oracle:no-space-full] metamorphic violation: no-space string with oversized upper must be returned unchanged"
                                        + " input=" + str
                                        + " lower=" + lower
                                        + " upper=" + upper
                                        + " append=" + appendToEnd
                                        + " result=" + lhs);
                    }
                }
            } catch (RuntimeException t) {
                if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                    throw t;
                }

                boolean inTarget = false;
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.lang.WordUtils".equals(e.getClassName())
                            && "abbreviate".equals(e.getMethodName())) {
                        inTarget = true;
                        break;
                    }
                }
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    continue;
                }
                if (t instanceof StringIndexOutOfBoundsException && inTarget) {
                    throw t;
                }
            }
        }
    }
}
```

</details>

---
## [30] ⚙️ harness-attempt · `attempt_014`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.StringIndexOutOfBoundsException: String index out of range: 15

---
## [31] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,694 chars)</summary>

```
package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact failing test input from WordUtilsTest.
        String anchor = invokeAbbreviate("0123456789", 15, 20, null, true);
        if (anchor != null && !"0123456789".equals(anchor)) {
            throw new RuntimeException("[oracle:anchor] metamorphic violation: exact trigger should return original string input=0123456789 lower=15 upper=20 lhs=" + anchor + " rhs=0123456789");
        }

        // EXPLORE: root cause is lower > str.length(), then upper is at or beyond length
        // (-1 meaning unlimited, or > length), which a correct implementation clamps.
        String s = data.consumeAsciiString(32);
        if (s.length() == 0) {
            s = "A";
        }
        s = sanitizeNoWhitespace(s);
        if (s.length() == 0) {
            s = "A";
        }

        String append = data.consumeBoolean() ? null : data.consumeString(8);
        int len = s.length();
        int lower = len + 1 + data.consumeInt(0, 20);
        int upperBeyondLen = len + 1 + data.consumeInt(0, 20);

        // Contract guarantee from the method body and the failing test:
        // if upper == -1 or upper > str.length(), upper is treated as str.length().
        // For a non-empty string without spaces and lower > length, indexOf(..., lower) == -1,
        // so a correct implementation returns str.substring(0, str.length()) == original string,
        // and appendToEnd is not appended because no abbreviation occurred.
        String rUnlimited = invokeAbbreviate(s, lower, -1, append, true);
        if (rUnlimited != null && !s.equals(rUnlimited)) {
            throw new RuntimeException("[oracle:clamp-unlimited] metamorphic violation: lower>length and upper=-1 must return original string input=" + s + " lower=" + lower + " upper=-1 lhs=" + rUnlimited + " rhs=" + s);
        }

        String rTooLarge = invokeAbbreviate(s, lower, upperBeyondLen, append, true);
        if (rTooLarge != null && !s.equals(rTooLarge)) {
            throw new RuntimeException("[oracle:clamp-toolarge] metamorphic violation: lower>length and upper>length must return original string input=" + s + " lower=" + lower + " upper=" + upperBeyondLen + " lhs=" + rTooLarge + " rhs=" + s);
        }

        // Equivalent-input relation using only real library calls:
        // upper == -1 and upper > str.length() are both documented/clamped to str.length(),
        // so they must agree on the same input.
        if (rUnlimited != null && rTooLarge != null && !rUnlimited.equals(rTooLarge)) {
            throw new RuntimeException("[oracle:eq-upper-clamp] metamorphic violation: clamped upper variants must agree input=" + s + " lower=" + lower + " upperA=-1 upperB=" + upperBeyondLen + " lhs=" + rUnlimited + " rhs=" + rTooLarge);
        }
    }

    private static String sanitizeNoWhitespace(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) {
                b.append('X');
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }

    private static String invokeAbbreviate(String str, int lower, int upper, String appendToEnd, boolean validByConstruction) {
        try {
            return WordUtils.abbreviate(str, lower, upper, appendToEnd);
        } catch (RuntimeException t) {
            if (isValidationThrowable(t)) {
                return null;
            }
            if (validByConstruction && isRootCause(t)) {
                throw t;
            }
            return null;
        }
    }

    private static boolean isValidationThrowable(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Invalid") || name.contains("Validation");
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            if ("org.apache.commons.lang.WordUtils".equals(e.getClassName())
                    && "abbreviate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}
```

</details>

---
## [32] ⚙️ harness-attempt · `attempt_015`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.StringIndexOutOfBoundsException: String index out of range: 15

---
## [33] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,008 chars)</summary>

```
package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();
        explore(data);
    }

    private static void anchor() {
        String s = "0123456789";
        try {
            String r = WordUtils.abbreviate(s, 15, 20, null);
            if (!s.equals(r)) {
                throw new RuntimeException("[oracle:anchor-eq] metamorphic violation: abbreviate should return the original string when lower and upper exceed length and appendToEnd is null input="
                        + describe(s, 15, 20, null) + " lhs=" + r + " rhs=" + s);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromAbbreviate(t)) {
                throw t;
            }
        }
    }

    private static void explore(FuzzedDataProvider data) {
        String base = data.consumeString(64);
        if (base == null) {
            base = "";
        }

        String append = data.consumeBoolean() ? null : data.consumeString(8);

        String s;
        if (base.length() == 0) {
            s = "A";
        } else {
            s = base;
        }

        int len = s.length();

        int lower = len + 1 + data.consumeInt(0, 32);
        int upper;
        if (data.consumeBoolean()) {
            upper = -1;
        } else {
            upper = lower + data.consumeInt(0, 32);
        }

        try {
            String r = WordUtils.abbreviate(s, lower, upper, append);

            /*
             * Contract asserted:
             * - The method comments and failing test state that if lower is greater than the string length,
             *   lower is treated as the string length; and if upper is -1 or greater than the string length,
             *   upper is treated as the string length.
             * Therefore for any non-null, non-empty string with lower > length and upper == -1 or upper >= lower > length,
             * a correct implementation must return the original string unchanged, with no appendToEnd suffix added.
             * A patch that merely suppresses the exception or changes control flow incorrectly would violate this observable result.
             */
            if (!s.equals(r)) {
                throw new RuntimeException("[oracle:overshoot-identity] metamorphic violation: abbreviate should return the original string for overshoot bounds input="
                        + describe(s, lower, upper, append) + " lhs=" + r + " rhs=" + s);
            }

            try {
                String r2 = WordUtils.abbreviate(r, lower, upper, append);
                if (!r.equals(r2)) {
                    throw new RuntimeException("[oracle:idempotence] metamorphic violation: applying abbreviate twice with the same overshoot bounds should be idempotent input="
                            + describe(s, lower, upper, append) + " lhs=" + r + " rhs=" + r2);
                }
            } catch (RuntimeException ignored) {
                if (isRootCauseFromAbbreviate(ignored)) {
                    throw ignored;
                }
                return;
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromAbbreviate(t)) {
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
            if (name != null) {
                String lower = name.toLowerCase();
                if (lower.contains("invalid") || lower.contains("validation")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isRootCauseFromAbbreviate(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.lang.WordUtils".equals(e.getClassName())
                    && "abbreviate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static String describe(String s, int lower, int upper, String append) {
        return "{str=" + quote(s) + ",lower=" + lower + ",upper=" + upper + ",append=" + quote(append) + "}";
    }

    private static String quote(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }
}
```

</details>

---
## [34] ⚙️ harness-attempt · `attempt_016`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.StringIndexOutOfBoundsException: String index out of range: 15

---
## [35] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,489 chars)</summary>

```
package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact regression input from WordUtilsTest.testAbbreviate.
        try {
            String anchored = WordUtils.abbreviate("0123456789", 15, 20, null);
            if (!"0123456789".equals(anchored)) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: exact regression input must return original string input=0123456789 lower=15 upper=20 append=null result=" + String.valueOf(anchored));
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseInAbbreviate(t)) {
                throw t;
            }
        }

        String str = data.consumeString(64);
        if (str == null) {
            str = "";
        }

        int extra = data.consumeInt(1, 32);
        int lower = data.consumeInt(0, str.length() + 32);
        int upper = data.consumeBoolean() ? -1 : str.length() + extra;
        String appendToEnd = data.consumeBoolean() ? null : data.consumeString(16);

        // EXPLORE crash surface through the real public API.
        // Valid-by-construction: non-null string, non-negative lower, and upper is either -1 or > str.length(),
        // which the method's own comments say must be normalized to str.length().
        try {
            WordUtils.abbreviate(str, lower, upper, appendToEnd);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseInAbbreviate(t) && isValidByConstruction(str, lower, upper)) {
                throw t;
            }
            return;
        }

        // Mandatory oracle:
        // Contract visible in the method comments/code: "if the upper value is -1 ... or is greater
        // than the length of the string, set to the length of the string".
        // Therefore, for any fixed (str, lower, appendToEnd), calls with upper == -1 and upper > str.length()
        // must be equivalent. A throw-deleting or overfitted patch that changes behavior instead of correctly
        // normalizing upper would violate this observable sibling-input relation.
        try {
            String lhs = WordUtils.abbreviate(str, lower, -1, appendToEnd);
            String rhs = WordUtils.abbreviate(str, lower, str.length() + extra, appendToEnd);
            if (!safeEquals(lhs, rhs)) {
                throw new RuntimeException("[oracle:upper-normalization] metamorphic violation: abbreviate(str, lower, -1, append) must equal abbreviate(str, lower, upper>len, append) input="
                        + printable(str) + " lower=" + lower + " upper1=-1 upper2=" + (str.length() + extra)
                        + " append=" + printable(appendToEnd) + " lhs=" + printable(lhs) + " rhs=" + printable(rhs));
            }
        } catch (RuntimeException t) {
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            if (isRootCauseInAbbreviate(t) && isValidByConstruction(str, lower, -1)) {
                throw t;
            }
        }
    }

    private static boolean isValidByConstruction(String str, int lower, int upper) {
        return str != null && lower >= 0 && (upper == -1 || upper > str.length());
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCauseInAbbreviate(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.lang.WordUtils".equals(e.getClassName())
                    && "abbreviate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String printable(String s) {
        return s == null ? "null" : s;
    }
}
```

</details>

---
## [36] ⚙️ harness-attempt · `attempt_017`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.StringIndexOutOfBoundsException: String index out of range: 15

---
## [37] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,606 chars)</summary>

```
package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact trigger from the failing test. On the buggy version this must
        // reproduce java.lang.StringIndexOutOfBoundsException from WordUtils.abbreviate.
        try {
            String anchor = WordUtils.abbreviate("0123456789", 15, 20, null);
            if (!"0123456789".equals(anchor)) {
                throw new RuntimeException("[oracle:anchor-ret] metamorphic violation: exact regression input must abbreviate to original string input=0123456789 lower=15 upper=20 lhs=" + String.valueOf(anchor) + " rhs=0123456789");
            }
        } catch (RuntimeException t) {
            boolean throughAbbreviate = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName())
                        && "abbreviate".equals(ste.getMethodName())) {
                    throughAbbreviate = true;
                    break;
                }
            }
            if (throughAbbreviate && t instanceof StringIndexOutOfBoundsException) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            return;
        }

        // EXPLORE: vary strings and numeric arguments with the same root-cause property:
        // lower beyond the string length, upper at/beyond the string length (or -1).
        String fuzzStr = data.consumeString(64);
        if (fuzzStr == null) {
            fuzzStr = "";
        }
        if (fuzzStr.length() == 0) {
            fuzzStr = "A";
        }
        int len = fuzzStr.length();
        int lower = len + data.consumeInt(1, 32);
        int upper;
        if (data.consumeBoolean()) {
            upper = -1;
        } else if (data.consumeBoolean()) {
            upper = len + data.consumeInt(0, 32);
        } else {
            upper = data.consumeInt(0, lower);
        }
        String append = data.consumeBoolean() ? null : data.consumeAsciiString(8);

        try {
            WordUtils.abbreviate(fuzzStr, lower, upper, append);
        } catch (RuntimeException t) {
            boolean throughAbbreviate = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName())
                        && "abbreviate".equals(ste.getMethodName())) {
                    throughAbbreviate = true;
                    break;
                }
            }
            if (throughAbbreviate && t instanceof StringIndexOutOfBoundsException) {
                // Valid by construction: non-null, non-empty string; lower is beyond length;
                // upper is at/beyond length or -1 / <= lower. Per the method's own contract
                // comments, these bounds are normalized rather than rejected.
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
        }

        // MANDATORY ORACLE:
        // Contract visible in the method comments/body: if upper == -1 or > str.length(),
        // set upper to str.length(); if upper < lower, raise it to lower. For a non-empty
        // string with NO spaces and lower > length, StringUtils.indexOf(str, " ", lower)
        // is -1, so a correct implementation must return str.substring(0, str.length()),
        // i.e. the original string, with no appendToEnd added because upper == str.length().
        // A "fix" that merely suppresses the crash or skips the intended normalization would
        // violate this observable result.
        String oracleStr = data.consumeAsciiString(48).replace(" ", "");
        if (oracleStr.length() == 0) {
            oracleStr = "Z";
        }
        int oracleLen = oracleStr.length();
        int oracleLower = oracleLen + data.consumeInt(1, 32);
        int oracleUpper = data.consumeBoolean() ? -1 : oracleLen + data.consumeInt(0, 32);
        String oracleAppend = data.consumeBoolean() ? null : data.consumeAsciiString(8);

        try {
            String got = WordUtils.abbreviate(oracleStr, oracleLower, oracleUpper, oracleAppend);
            if (!oracleStr.equals(got)) {
                throw new RuntimeException("[oracle:no-space-clamp] metamorphic violation: no-space input with bounds beyond length must return original string input=" + oracleStr + " lower=" + oracleLower + " upper=" + oracleUpper + " lhs=" + String.valueOf(got) + " rhs=" + oracleStr);
            }
        } catch (RuntimeException t) {
            boolean throughAbbreviate = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName())
                        && "abbreviate".equals(ste.getMethodName())) {
                    throughAbbreviate = true;
                    break;
                }
            }
            if (throughAbbreviate && t instanceof StringIndexOutOfBoundsException) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
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
## [38] ⚙️ harness-attempt · `attempt_018`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.StringIndexOutOfBoundsException: String index out of range: 15

---
## [39] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,296 chars)</summary>

```
package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();

        String raw = data.consumeString(64);
        String append = data.consumeBoolean() ? null : data.consumeString(8);

        String noSpace = normalizeNoSpace(raw);
        if (noSpace.length() == 0) {
            noSpace = "A";
        }

        int len = noSpace.length();
        int lower = len + data.consumeInt(0, 32);
        int upperChoice = data.consumeInt(0, 4);
        int upper;
        if (upperChoice == 0) {
            upper = -1;
        } else {
            upper = len + data.consumeInt(0, 32);
        }

        try {
            String result = WordUtils.abbreviate(noSpace, lower, upper, append);

            /*
             * Contract asserted:
             * For abbreviate, values beyond the string length are normalized to the length
             * ("if the lower value is greater than the length of the string, set to the length
             * of the string" and "if the upper value is -1 or is greater than the length of the
             * string, set to the length of the string"). For a string with no spaces, indexOf(..., lower)
             * must therefore act as "no space found", causing substring(0, str.length()) and, because
             * no abbreviation occurred, appendToEnd must not be appended.
             *
             * A throw-deleting or branch-skipping patch that avoids the crash but returns a truncated
             * string or appends appendToEnd would violate this observable result.
             */
            if (lower >= len && (upper == -1 || upper >= len) && !noSpace.equals(result)) {
                throw new RuntimeException(
                    "[oracle:abbrev-full] metamorphic violation: expected full string when bounds exceed length"
                        + " input=" + printable(noSpace)
                        + " lower=" + lower
                        + " upper=" + upper
                        + " append=" + printable(append)
                        + " lhs=" + printable(result)
                        + " rhs=" + printable(noSpace));
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromAbbreviate(t)) {
                throw t;
            }
            return;
        }

        String arbitrary = data.consumeRemainingAsString();
        if (arbitrary == null) {
            return;
        }
        int arbLen = arbitrary.length();
        int arbLower = arbLen + 1 + Math.abs(data.consumeInt() % 16);
        int arbUpper = data.consumeBoolean() ? -1 : arbLen + 1 + Math.abs(data.consumeInt() % 16);
        try {
            WordUtils.abbreviate(arbitrary, arbLower, arbUpper, append);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromAbbreviate(t) && arbLen > 0) {
                throw t;
            }
        }
    }

    private static void anchor() {
        try {
            String result = WordUtils.abbreviate("0123456789", 15, 20, null);
            if (!"0123456789".equals(result)) {
                throw new RuntimeException(
                    "[oracle:anchor-exact] metamorphic violation: exact regression input returned wrong value"
                        + " input=0123456789 lower=15 upper=20 append=null"
                        + " lhs=" + printable(result)
                        + " rhs=0123456789");
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromAbbreviate(t)) {
                throw t;
            }
        }
    }

    private static boolean isRootCauseFromAbbreviate(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.lang.WordUtils".equals(e.getClassName())
                    && "abbreviate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName().toLowerCase();
        return name.contains("invalid") || name.contains("validation");
    }

    private static String normalizeNoSpace(String s) {
        if (s == null) {
            return "A";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ' ') {
                sb.append('X');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String printable(String s) {
        return s == null ? "null" : s;
    }
}
```

</details>

---
## [40] ⚙️ harness-attempt · `attempt_019`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.StringIndexOutOfBoundsException: String index out of range: 15

---
## [41] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,637 chars)</summary>

```
package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            String anchor = WordUtils.abbreviate("0123456789", 15, 20, null);
            if (!"0123456789".equals(anchor)) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: exact regression input returned wrong value input=0123456789 lower=15 upper=20 append=null result=" + String.valueOf(anchor));
            }
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        String s = data.consumeAsciiString(20);
        if (s.length() == 0) {
            s = "A";
        }

        if (data.consumeBoolean() && s.length() > 1) {
            int pos = data.consumeInt(1, s.length() - 1);
            s = s.substring(0, pos) + " " + s.substring(pos);
        }

        String appendToEnd = data.consumeBoolean() ? null : data.consumeAsciiString(5);
        int lower = data.consumeInt(0, s.length());
        int upperTooLarge = s.length() + 1 + data.consumeInt(0, 20);

        String lhs;
        try {
            lhs = WordUtils.abbreviate(s, lower, upperTooLarge, appendToEnd);
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        String rhs;
        try {
            rhs = WordUtils.abbreviate(s, lower, -1, appendToEnd);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        /*
         * Contract asserted from the method body/comments:
         * "if the upper value is -1 (i.e. no limit) or is greater than the length
         * of the string, set to the length of the string".
         * Therefore, for any non-null, non-empty string and any lower within [0, len],
         * abbreviate(str, lower, len+k, append) must equal abbreviate(str, lower, -1, append).
         * A patch that merely suppresses the exception or skips the normalization would
         * break this observable equivalence.
         */
        if (!String.valueOf(lhs).equals(String.valueOf(rhs))) {
            throw new RuntimeException(
                "[oracle:upper-clamp] metamorphic violation: upper>length must behave like upper=-1 input="
                    + quote(s) + " lower=" + lower + " upper=" + upperTooLarge + " append=" + quote(appendToEnd)
                    + " lhs=" + quote(lhs) + " rhs=" + quote(rhs));
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.lang.WordUtils".equals(e.getClassName())
                    && "abbreviate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static String quote(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }
}
```

</details>

---
## [42] ⚙️ harness-attempt · `attempt_020`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.StringIndexOutOfBoundsException: String index out of range: 15

---
## [43] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,957 chars)</summary>

```
package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        probeCrashAndMaybePropagate("0123456789", 15, 20, null, true);

        String base = data.consumeAsciiString(64);
        if (base == null) {
            return;
        }

        String noSpace = removeSpaces(base);
        if (noSpace.length() == 0) {
            noSpace = "A";
        }

        int len = noSpace.length();

        int lower;
        int upperChoice = data.consumeInt(0, 5);
        int appendChoice = data.consumeInt(0, 3);

        switch (data.consumeInt(0, 4)) {
            case 0:
                lower = len;
                break;
            case 1:
                lower = len + 1 + data.consumeInt(0, 32);
                break;
            case 2:
                lower = len + data.consumeInt(0, 32);
                break;
            case 3:
                lower = 15;
                break;
            default:
                lower = len + Math.abs(data.consumeByte());
                break;
        }

        int upper;
        switch (upperChoice) {
            case 0:
                upper = -1;
                break;
            case 1:
                upper = len;
                break;
            case 2:
                upper = len + 1 + data.consumeInt(0, 32);
                break;
            case 3:
                upper = lower;
                break;
            case 4:
                upper = lower + data.consumeInt(0, 16);
                break;
            default:
                upper = 20;
                break;
        }

        String appendToEnd;
        switch (appendChoice) {
            case 0:
                appendToEnd = null;
                break;
            case 1:
                appendToEnd = "";
                break;
            case 2:
                appendToEnd = data.consumeAsciiString(8);
                break;
            default:
                appendToEnd = "-";
                break;
        }

        probeCrashAndMaybePropagate(noSpace, lower, upper, appendToEnd, true);

        String spaced = buildSpacedVariant(noSpace, data);
        int lower2 = Math.max(spaced.length(), lower);
        int upper2 = data.consumeBoolean() ? -1 : lower2 + data.consumeInt(0, 16);
        probeCrashAndMaybePropagate(spaced, lower2, upper2, appendToEnd, true);

        /*
         * Contract asserted:
         * The method comment/code says:
         * - if the lower value is greater than the length of the string, set to the length of the string
         * - if the upper value is -1 or greater than the length of the string, set to the length of the string
         * For a string with no spaces, this means abbreviate(str, lower>=len, upper=-1 or upper>len, append)
         * must return the original string unchanged. A patch that merely suppresses the exception but returns
         * the wrong substring would violate this observable result.
         */
        checkNoSpaceFullReturnOracle(noSpace, lower, upper, appendToEnd);

        /*
         * Metamorphic relation from the same documented normalization:
         * once upper is treated as "to the end" (-1 or >len), all such values are equivalent.
         * Therefore, on no-space strings and lower>=len, calls with upper=-1 and upper=len+k must agree.
         * If either call throws, this check does not apply and is skipped.
         */
        checkEquivalentUpperOracle(noSpace, lower, appendToEnd, len + 1 + data.consumeInt(0, 32));
    }

    private static void probeCrashAndMaybePropagate(String str, int lower, int upper, String appendToEnd, boolean validByConstruction) {
        try {
            WordUtils.abbreviate(str, lower, upper, appendToEnd);
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

    private static void checkNoSpaceFullReturnOracle(String str, int lower, int upper, String appendToEnd) {
        if (str == null || str.length() == 0) {
            return;
        }
        if (str.indexOf(' ') >= 0) {
            return;
        }
        if (lower < str.length()) {
            return;
        }
        if (!(upper == -1 || upper >= str.length())) {
            return;
        }

        final String result;
        try {
            result = WordUtils.abbreviate(str, lower, upper, appendToEnd);
        } catch (Throwable t) {
            return;
        }

        if (!str.equals(result)) {
            throw new RuntimeException(
                "[oracle:no-space-full] metamorphic violation: expected original string when lower>=len and upper reaches end input="
                    + printable(str) + " lower=" + lower + " upper=" + upper + " append=" + printable(appendToEnd)
                    + " lhs=" + printable(result) + " rhs=" + printable(str));
        }
    }

    private static void checkEquivalentUpperOracle(String str, int lower, String appendToEnd, int largeUpper) {
        if (str == null || str.length() == 0) {
            return;
        }
        if (str.indexOf(' ') >= 0) {
            return;
        }
        if (lower < str.length()) {
            return;
        }
        if (largeUpper <= str.length()) {
            largeUpper = str.length() + 1;
        }

        final String lhs;
        final String rhs;
        try {
            lhs = WordUtils.abbreviate(str, lower, -1, appendToEnd);
            rhs = WordUtils.abbreviate(str, lower, largeUpper, appendToEnd);
        } catch (Throwable t) {
            return;
        }

        if (lhs == null ? rhs != null : !lhs.equals(rhs)) {
            throw new RuntimeException(
                "[oracle:upper-equiv] metamorphic violation: upper=-1 and upper>len must agree after normalization input="
                    + printable(str) + " lower=" + lower + " upper1=-1 upper2=" + largeUpper
                    + " append=" + printable(appendToEnd) + " lhs=" + printable(lhs) + " rhs=" + printable(rhs));
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            if ("org.apache.commons.lang.WordUtils".equals(e.getClassName())
                && "abbreviate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static String removeSpaces(String s) {
        if (s == null || s.length() == 0) {
            return s;
        }
        StringBuffer sb = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != ' ') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String buildSpacedVariant(String s, FuzzedDataProvider data) {
        if (s == null || s.length() == 0) {
            return "X";
        }
        if (s.length() == 1) {
            return s + " " + s;
        }
        int pos = data.consumeInt(1, s.length() - 1);
        return s.substring(0, pos) + " " + s.substring(pos);
    }

    private static String printable(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }
}
```

</details>

---
## [44] ⚙️ harness-attempt · `attempt_021`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.StringIndexOutOfBoundsException: String index out of range: 15

---
## [45] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,434 chars)</summary>

```
package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            WordUtils.abbreviate("0123456789", 15, 20, null);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }

        String str = data.consumeString(64);
        if (str == null) {
            str = "";
        }
        if (str.length() == 0) {
            str = "A";
        }

        String appendToEnd;
        if (data.consumeBoolean()) {
            appendToEnd = null;
        } else {
            appendToEnd = data.consumeString(8);
        }

        int len = str.length();
        int lower = len + data.consumeInt(1, 32);
        int upperVariant = len + data.consumeInt(1, 32);

        try {
            WordUtils.abbreviate(str, lower, upperVariant, appendToEnd);
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
            WordUtils.abbreviate(str, lower, -1, appendToEnd);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        /*
         * Oracle: abbreviate's documented normalization says:
         * "if the upper value is -1 (i.e. no limit) or is greater than the length
         * of the string, set to the length of the string".
         * Therefore, for the same (valid-by-construction) string/lower/appendToEnd,
         * calls that differ only by upper == -1 versus upper > str.length() must
         * produce the same observable result.
         * A patch that merely suppresses the crash or changes control flow without
         * performing the documented normalization can violate this equality.
         */
        try {
            String lhs = WordUtils.abbreviate(str, lower, -1, appendToEnd);
            String rhs = WordUtils.abbreviate(str, lower, upperVariant, appendToEnd);
            if (lhs != null ? !lhs.equals(rhs) : rhs != null) {
                throw new RuntimeException(
                    "[oracle:upper-normalization] metamorphic violation: upper=-1 and upper>length must agree input="
                        + describe(str, lower, upperVariant, appendToEnd)
                        + " lhs=" + String.valueOf(lhs)
                        + " rhs=" + String.valueOf(rhs));
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isOracleViolation(t)) {
                throw t;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static boolean isOracleViolation(RuntimeException t) {
        return t.getMessage() != null && t.getMessage().startsWith("[oracle:");
    }

    private static boolean isRootCause(RuntimeException t) {
        return t instanceof StringIndexOutOfBoundsException
            && hasAbbreviateFrame(t);
    }

    private static boolean hasAbbreviateFrame(Throwable t) {
        for (StackTraceElement e : t.getStackTrace()) {
            if ("org.apache.commons.lang.WordUtils".equals(e.getClassName())
                && "abbreviate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(RuntimeException t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Invalid")
            || name.contains("Validation")
            || name.contains("Malformed");
    }

    private static String describe(String str, int lower, int upper, String appendToEnd) {
        return "{str=" + String.valueOf(str)
            + ",len=" + str.length()
            + ",lower=" + lower
            + ",upper=" + upper
            + ",appendToEnd=" + String.valueOf(appendToEnd)
            + "}";
    }
}
```

</details>

---
## [46] ⚙️ harness-attempt · `attempt_022`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.StringIndexOutOfBoundsException: String index out of range: 15

---
## [47] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,609 chars)</summary>

```
package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Contract asserted below:
        // For WordUtils.abbreviate, the code comments and the regression test show that when
        // upper is -1 or greater than str.length(), it must be normalized to str.length().
        // Therefore, for a non-empty string with no spaces, if lower >= str.length() and
        // upper >= lower, a correct implementation must return the original string unchanged:
        // indexOf(" ", lower) == -1, substring(0, normalizedUpper) == whole string, and no
        // appendToEnd is added because no abbreviation occurred. A patch that merely suppresses
        // the crashing branch or silently truncates would violate this observable result.

        // ANCHOR: exact regression trigger from the failing test.
        try {
            String anchor = WordUtils.abbreviate("0123456789", 15, 20, null);
            if (!"0123456789".equals(anchor)) {
                throw new RuntimeException("[oracle:anchor-ret] metamorphic violation: exact regression input must return original string input=0123456789 lower=15 upper=20 append=null lhs="
                        + String.valueOf(anchor) + " rhs=0123456789");
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromAbbreviate(t)) {
                throw t;
            }
            return;
        }

        // EXPLORE: vary valid inputs that satisfy the same root-cause property:
        // lower at or beyond string length and upper at or beyond lower, driven through the
        // real public API.
        String raw = data.consumeAsciiString(64);
        String str = removeSpaces(raw);
        if (str.length() == 0) {
            str = "A";
        }

        int len = str.length();
        int lower = len + data.consumeInt(0, 32);
        int upper;
        if (data.consumeBoolean()) {
            upper = lower + data.consumeInt(0, 32);
        } else {
            upper = len + data.consumeInt(0, 32);
            if (upper < lower) {
                upper = lower;
            }
        }

        String appendToEnd = data.consumeBoolean() ? null : data.consumeAsciiString(16);

        try {
            String result = WordUtils.abbreviate(str, lower, upper, appendToEnd);

            // Oracle applies only to our non-empty, no-space strings constructed above.
            if (!str.equals(result)) {
                throw new RuntimeException("[oracle:no-space-full] metamorphic violation: no-space string with upper beyond length must be returned unchanged input="
                        + printable(str) + " lower=" + lower + " upper=" + upper + " append="
                        + printable(String.valueOf(appendToEnd)) + " lhs=" + printable(result) + " rhs=" + printable(str));
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromAbbreviate(t)) {
                throw t;
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCauseFromAbbreviate(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.lang.WordUtils".equals(e.getClassName())
                    && "abbreviate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static String removeSpaces(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuffer b = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != ' ') {
                b.append(c);
            }
        }
        return b.toString();
    }

    private static String printable(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r");
    }
}
```

</details>

---
## [48] ⚙️ harness-attempt · `attempt_023`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.StringIndexOutOfBoundsException: String index out of range: 15

---
## [49] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,832 chars)</summary>

```
package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();

        String fuzz = data.consumeAsciiString(64);
        if (fuzz == null) {
            return;
        }

        String noSpaces = fuzz.replace(" ", "");
        if (noSpaces.length() == 0) {
            noSpaces = "A";
        }

        int len = noSpaces.length();
        int lower = len + data.consumeInt(0, 32);
        int upper;
        if (data.consumeBoolean()) {
            upper = -1;
        } else {
            upper = len + data.consumeInt(0, 32);
        }
        String appendToEnd = data.consumeBoolean() ? null : data.consumeAsciiString(8);

        callAndCheckValid(noSpaces, lower, upper, appendToEnd);

        String mixed = data.consumeString(64);
        if (mixed == null) {
            return;
        }
        if (mixed.length() == 0) {
            mixed = "B";
        }
        int mlen = mixed.length();
        int lower2 = mlen + data.consumeInt(0, 32);
        int upper2 = data.consumeBoolean() ? -1 : mlen + data.consumeInt(0, 32);
        try {
            WordUtils.abbreviate(mixed, lower2, upper2, appendToEnd);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                return;
            }
        }
    }

    private static void anchor() {
        try {
            String out = WordUtils.abbreviate("0123456789", 15, 20, null);
            if (!"0123456789".equals(out)) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: exact regression input should abbreviate to original string input=0123456789 lower=15 upper=20 lhs=" + String.valueOf(out) + " rhs=0123456789");
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw (RuntimeException) t;
            }
        }
    }

    private static void callAndCheckValid(String str, int lower, int upper, String appendToEnd) {
        try {
            String out = WordUtils.abbreviate(str, lower, upper, appendToEnd);

            /*
             * Contract used for this oracle:
             * - shown code/comments say: if upper == -1 or upper > str.length(), upper is set to str.length()
             * - then if no space is found from lower, result appends str.substring(0, upper)
             * - appendToEnd is appended only if upper != str.length()
             * Therefore, for a non-empty string with no spaces and lower >= str.length(),
             * any correct implementation must return the original string exactly when upper is -1
             * or greater than the string length. A patch that merely suppresses the crash but
             * loses this normalization would violate this observable result.
             */
            if (!str.equals(out)) {
                throw new RuntimeException("[oracle:abbrev-no-space] metamorphic violation: no-space string with lower>=len and upper out of range must return original string input=" + str + " lower=" + lower + " upper=" + upper + " append=" + String.valueOf(appendToEnd) + " lhs=" + String.valueOf(out) + " rhs=" + str);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw (RuntimeException) t;
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name != null) {
                String lower = name.toLowerCase();
                if (lower.contains("invalid") || lower.contains("validation")) {
                    return true;
                }
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            if ("org.apache.commons.lang.WordUtils".equals(e.getClassName())
                    && "abbreviate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}
```

</details>

---
## [50] ⚙️ harness-attempt · `attempt_024`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.StringIndexOutOfBoundsException: String index out of range: 15

---
## [51] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,909 chars)</summary>

```
package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact trigger from the failing test.
        try {
            String anchored = WordUtils.abbreviate("0123456789", 15, 20, null);
            if (!"0123456789".equals(anchored)) {
                throw new RuntimeException(
                    "[oracle:anchor-eq] metamorphic violation: exact trigger should return original string input=0123456789 lower=15 upper=20 lhs="
                        + String.valueOf(anchored) + " rhs=0123456789");
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseInAbbreviate(t)) {
                throw t;
            }
        }

        // EXPLORE: varied REAL inputs with the same root-cause property:
        // lower is at or beyond str.length(), and upper is -1 or beyond str.length().
        // We build no-space strings by construction so StringUtils.indexOf(str, " ", lower) is -1,
        // making the documented/clamped substring path observable.
        String base = data.consumeAsciiString(64);
        if (base == null) {
            return;
        }
        String noSpace = stripSpaces(base);
        if (noSpace.length() == 0) {
            noSpace = "A";
        }

        int len = noSpace.length();
        int lower = len + data.consumeInt(0, 32);
        int upper;
        if (data.consumeBoolean()) {
            upper = -1;
        } else {
            upper = len + data.consumeInt(1, 32);
        }

        String append = data.consumeBoolean() ? null : data.consumeAsciiString(16);

        try {
            String result = WordUtils.abbreviate(noSpace, lower, upper, append);

            // Post-condition from the method contract visible in the provided code/comments:
            // if upper == -1 or upper > str.length(), upper is set to str.length();
            // if no space is found, the method appends str.substring(0, upper), and only appends
            // appendToEnd when upper != str.length().
            // For a no-space string with lower >= str.length() and upper == -1 or > str.length(),
            // a correct implementation must therefore return the original string unchanged.
            // A throw-deleting or branch-skipping patch that avoids the crash by altering behavior
            // would violate this observable result.
            if (!noSpace.equals(result)) {
                throw new RuntimeException(
                    "[oracle:nospace-identity] metamorphic violation: no-space string with lower>=len and upper out of range must return original string input="
                        + quote(noSpace) + " lower=" + lower + " upper=" + upper + " append="
                        + quote(append) + " lhs=" + quote(result) + " rhs=" + quote(noSpace));
            }

            // Equivalent-input relation on the same documented guarantee:
            // for these constructed inputs, upper == -1 and upper > len are both specified to clamp
            // to str.length(), so they must agree.
            try {
                String resultUnlimited = WordUtils.abbreviate(noSpace, lower, -1, append);
                if (!result.equals(resultUnlimited)) {
                    throw new RuntimeException(
                        "[oracle:upper-clamp-agree] metamorphic violation: upper=-1 and upper>len should agree after clamping input="
                            + quote(noSpace) + " lower=" + lower + " upper=" + upper + " append="
                            + quote(append) + " lhs=" + quote(result) + " rhs=" + quote(resultUnlimited));
                }
            } catch (RuntimeException t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCauseInAbbreviate(t) && isValidByConstruction(noSpace, lower, -1)) {
                    throw t;
                }
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseInAbbreviate(t) && isValidByConstruction(noSpace, lower, upper)) {
                throw t;
            }
        }
    }

    private static boolean isValidByConstruction(String s, int lower, int upper) {
        if (s == null || s.length() == 0) {
            return false;
        }
        if (s.indexOf(' ') != -1) {
            return false;
        }
        if (lower < s.length()) {
            return false;
        }
        return upper == -1 || upper > s.length();
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCauseInAbbreviate(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.lang.WordUtils".equals(e.getClassName())
                    && "abbreviate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static String stripSpaces(String s) {
        StringBuffer sb = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != ' ') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String quote(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }
}
```

</details>

---
## [52] ⚙️ harness-attempt · `attempt_025`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.StringIndexOutOfBoundsException: String index out of range: 15

---
## [53] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,833 chars)</summary>

```
package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact trigger from the failing test. On the buggy version this throws
        // StringIndexOutOfBoundsException from WordUtils.abbreviate.
        exerciseAndCheck("0123456789", 15, 20, null, true);

        // EXPLORE:
        // Root-cause property: upper == -1 or upper > str.length() must be handled by clamping
        // to str.length(). For strings with no spaces, the documented/code-visible behavior is
        // that no abbreviation occurs, so the result must be the original string.
        //
        // We construct non-null, no-space strings by construction so a correct implementation
        // is obligated to accept them. We also keep magnitudes moderate.
        String base = makeNoSpaceString(data.consumeAsciiString(64));
        if (base.length() == 0) {
            base = "A";
        }

        int len = base.length();
        int variant = data.consumeInt(0, 5);

        int lower;
        switch (variant) {
            case 0:
                lower = len;
                break;
            case 1:
                lower = len + data.consumeInt(1, 32);
                break;
            case 2:
                lower = data.consumeInt(0, len);
                break;
            case 3:
                lower = 0;
                break;
            case 4:
                lower = data.consumeInt(0, len);
                break;
            default:
                lower = len + data.consumeInt(0, 16);
                break;
        }

        int upper;
        if (data.consumeBoolean()) {
            upper = -1;
        } else {
            upper = len + data.consumeInt(1, 32);
        }

        String appendToEnd = data.consumeBoolean() ? null : makeNoSpaceString(data.consumeAsciiString(8));

        exerciseAndCheck(base, lower, upper, appendToEnd, true);

        // Additional exploration around surrounding content and lengths, still no spaces.
        String prefix = makeNoSpaceString(data.consumeAsciiString(16));
        String suffix = makeNoSpaceString(data.consumeAsciiString(16));
        String combined = prefix + base + suffix;
        if (combined.length() > 0) {
            int lower2 = data.consumeBoolean() ? combined.length() + data.consumeInt(0, 16)
                                               : data.consumeInt(0, combined.length());
            int upper2 = data.consumeBoolean() ? -1 : combined.length() + data.consumeInt(1, 16);
            String append2 = data.consumeBoolean() ? null : makeNoSpaceString(data.consumeAsciiString(8));
            exerciseAndCheck(combined, lower2, upper2, append2, true);
        }

        // Mixed-input call for general coverage. This may be invalid for reasons unrelated to the
        // patch, so we never assert on it and only propagate the verified root-cause crash.
        String any = data.consumeString(64);
        int anyLower = data.consumeInt();
        int anyUpper = data.consumeBoolean() ? -1 : data.consumeInt();
        String anyAppend = data.consumeBoolean() ? null : data.consumeString(16);
        exerciseAndCheck(any, anyLower, anyUpper, anyAppend, false);
    }

    private static void exerciseAndCheck(String str, int lower, int upper, String appendToEnd, boolean assertContract) {
        try {
            String out = WordUtils.abbreviate(str, lower, upper, appendToEnd);

            if (assertContract) {
                // Contract/post-condition asserted:
                // From the method comments and failing test:
                // - if upper is -1 or > str.length(), it is set to str.length()
                // - in the no-space branch, substring(0, upper) is returned
                // - appendToEnd is appended only if abbreviation has occurred
                //
                // Therefore, for any non-null string containing no spaces, when upper == -1 or
                // upper > str.length(), a correct implementation must return the original string,
                // not append appendToEnd. A patch that merely deletes the throw or skips the clamp
                // can violate this observable result.
                if (str != null && str.indexOf(' ') == -1 && (upper == -1 || upper > str.length())) {
                    if (!str.equals(out)) {
                        throw new RuntimeException(
                            "[oracle:abbrev-no-space-clamp] metamorphic violation: expected original string when upper is unlimited/out-of-range on space-free input"
                                + " input=" + safe(str)
                                + " lower=" + lower
                                + " upper=" + upper
                                + " append=" + safe(appendToEnd)
                                + " lhs=" + safe(out)
                                + " rhs=" + safe(str));
                    }

                    // Idempotence-style check on an equivalent canonicalized call using the real API:
                    // once upper has been clamped to str.length(), calling abbreviate again with the
                    // exact length must produce the same result on these no-space inputs.
                    try {
                        String canon = WordUtils.abbreviate(str, lower, str.length(), appendToEnd);
                        if (!out.equals(canon)) {
                            throw new RuntimeException(
                                "[oracle:abbrev-canonical-upper] metamorphic violation: upper=-1 or upper>len must agree with upper=len"
                                    + " input=" + safe(str)
                                    + " lower=" + lower
                                    + " upper=" + upper
                                    + " append=" + safe(appendToEnd)
                                    + " lhs=" + safe(out)
                                    + " rhs=" + safe(canon));
                        }
                    } catch (Throwable ignored) {
                        // Hygiene rule: if either side throws, skip the relation.
                    }
                }
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t) && assertContract && isValidByConstruction(str, upper)) {
                throwUnchecked(t);
            }
            // Swallow unrelated throwables to avoid false positives on out-of-scope defects.
        }
    }

    private static boolean isValidByConstruction(String str, int upper) {
        return str != null && str.indexOf(' ') == -1 && str.length() > 0 && (upper == -1 || upper > str.length());
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            String simple = cur.getClass().getSimpleName();
            if (name.contains("Validation") || name.contains("Invalid") || simple.contains("Validation") || simple.contains("Invalid")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException || t instanceof IndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.lang.WordUtils".equals(e.getClassName()) && "abbreviate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static String makeNoSpaceString(String s) {
        if (s == null) {
            return "A";
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != ' ') {
                sb.append(c);
            }
        }
        if (sb.length() == 0) {
            sb.append('A');
        }
        return sb.toString();
    }

    private static String safe(String s) {
        return s == null ? "null" : "\"" + s + "\"";
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
## [54] ⚙️ harness-attempt · `attempt_026`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.StringIndexOutOfBoundsException: String index out of range: 15

---
## [55] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,045 chars)</summary>

```
package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        try {
            String result = WordUtils.abbreviate("0123456789", 15, 20, null);
            if (!"0123456789".equals(result)) {
                throw new RuntimeException("[oracle:anchor-eq] metamorphic violation: documented test case must return original string when lower and upper exceed length input=0123456789 lower=15 upper=20 append=null lhs=" + String.valueOf(result) + " rhs=0123456789");
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromAbbreviate(t)) {
                throw t;
            }
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        String base = buildNonSpaceString(data);
        if (base.length() == 0) {
            return;
        }

        int len = base.length();
        int lower = len + data.consumeInt(0, 32);
        int upperChoice = data.consumeInt(0, 3);
        int upper;
        if (upperChoice == 0) {
            upper = -1;
        } else if (upperChoice == 1) {
            upper = len + data.consumeInt(0, 32);
        } else if (upperChoice == 2) {
            upper = lower + data.consumeInt(0, 32);
        } else {
            upper = data.consumeInt(Integer.MIN_VALUE / 2, Integer.MAX_VALUE / 2);
        }
        String appendToEnd = data.consumeBoolean() ? null : data.consumeAsciiString(8);

        try {
            String result = WordUtils.abbreviate(base, lower, upper, appendToEnd);

            /*
             * Contract asserted:
             * In abbreviate, if lower exceeds the string length, lower is treated as the end of the string
             * by the subsequent search; if upper is -1 or greater than the string length, upper is set to the
             * string length; and appendToEnd is added only if abbreviation has occurred.
             * For a non-empty string with no spaces and lower >= str.length(), a correct implementation must
             * return the original string unchanged and must not append appendToEnd, because no abbreviation occurs.
             * A patch that merely suppresses the exception or skips the bound adjustment can return the wrong
             * substring or append marker, violating this observable post-condition.
             */
            if (!base.equals(result)) {
                throw new RuntimeException("[oracle:nospace-full] metamorphic violation: no-space input with lower>=length and upper at/above length must round to full original string input=" + printable(base) + " lower=" + lower + " upper=" + upper + " append=" + printable(appendToEnd) + " lhs=" + printable(result) + " rhs=" + printable(base));
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isOracle(t)) {
                throw t;
            }
            if (isRootCauseFromAbbreviate(t)) {
                throw t;
            }
        }

        trySiblingAgreement(data, base);
    }

    private static void trySiblingAgreement(FuzzedDataProvider data, String base) {
        String wrapInput = base + " " + buildNonSpaceString(data) + " " + buildNonSpaceString(data);
        if (wrapInput.trim().length() == 0) {
            return;
        }
        int wrapLength = data.consumeInt(1, 20);
        try {
            String lhs = WordUtils.wrap(wrapInput, wrapLength);
            String rhs = WordUtils.wrap(wrapInput, wrapLength, null, false);
            if (lhs != null && rhs != null && !lhs.equals(rhs)) {
                throw new RuntimeException("[oracle:wrap-overload] metamorphic violation: documented sibling overloads with default newline/null and wrapLongWords=false must agree input=" + printable(wrapInput) + " wrapLength=" + wrapLength + " lhs=" + printable(lhs) + " rhs=" + printable(rhs));
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isOracle(t)) {
                throw t;
            }
        }
    }

    private static String buildNonSpaceString(FuzzedDataProvider data) {
        String s = data.consumeAsciiString(32);
        if (s == null) {
            return "A";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ' ') {
                c = 'X';
            }
            if (c < 33 || c > 126) {
                c = 'Y';
            }
            sb.append(c);
        }
        if (sb.length() == 0) {
            sb.append('A');
        }
        return sb.toString();
    }

    private static boolean isRootCauseFromAbbreviate(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            if ("org.apache.commons.lang.WordUtils".equals(e.getClassName())
                    && "abbreviate".equals(e.getMethodName())) {
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

    private static String printable(String s) {
        return String.valueOf(s);
    }
}
```

</details>

---
## [56] ⚙️ harness-attempt · `attempt_027`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.StringIndexOutOfBoundsException: String index out of range: 15

---
## [57] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,950 chars)</summary>

```
package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact trigger from the failing test. On the buggy build this reaches
        // WordUtils.abbreviate and throws StringIndexOutOfBoundsException.
        try {
            WordUtils.abbreviate("0123456789", 15, 20, null);
        } catch (RuntimeException t) {
            boolean passesAbbreviate = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName())
                        && "abbreviate".equals(ste.getMethodName())) {
                    passesAbbreviate = true;
                    break;
                }
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            if (passesAbbreviate && t instanceof StringIndexOutOfBoundsException) {
                throw t;
            }
        }

        String base = data.consumeString(64);
        if (base == null) {
            return;
        }

        // Fence degenerate inputs for the oracle: use non-empty strings by construction.
        if (base.length() == 0) {
            base = "A";
        }

        // Explore the root-cause property: upper == -1 or upper > str.length().
        // Also vary lower at/around/above the string length, since the failing test does so.
        int len = base.length();
        int mode = data.consumeInt(0, 5);
        int lower;
        if (mode == 0) {
            lower = len;
        } else if (mode == 1) {
            lower = len + data.consumeInt(1, 8);
        } else if (mode == 2) {
            lower = Math.max(0, len - data.consumeInt(0, Math.min(len, 8)));
        } else {
            lower = data.consumeInt(0, len + 8);
        }

        String appendToEnd;
        int appendMode = data.consumeInt(0, 3);
        if (appendMode == 0) {
            appendToEnd = null;
        } else if (appendMode == 1) {
            appendToEnd = "";
        } else {
            appendToEnd = data.consumeAsciiString(8);
        }

        int upper;
        int upperMode = data.consumeInt(0, 3);
        if (upperMode == 0) {
            upper = -1;
        } else if (upperMode == 1) {
            upper = len + data.consumeInt(1, 12);
        } else if (upperMode == 2) {
            upper = len;
        } else {
            upper = data.consumeInt(0, len + 12);
        }

        // First real call through the public API with varied inputs satisfying the patched-line property.
        try {
            WordUtils.abbreviate(base, lower, upper, appendToEnd);
        } catch (RuntimeException t) {
            boolean passesAbbreviate = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang.WordUtils".equals(ste.getClassName())
                        && "abbreviate".equals(ste.getMethodName())) {
                    passesAbbreviate = true;
                    break;
                }
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            // Ground-truth throwable is generic, so only propagate when the input is valid-by-construction:
            // the documented contract in the method comments says upper == -1 or upper > str.length()
            // must be normalized to str.length(), so these inputs must be accepted.
            if (passesAbbreviate
                    && t instanceof StringIndexOutOfBoundsException
                    && (upper == -1 || upper > len)
                    && base.length() > 0) {
                throw t;
            }
            return;
        }

        // Mandatory metamorphic check.
        // Contract guarantee from the method's own comments/code:
        // "if the upper value is -1 ... or is greater than the length of the string, set to the length of the string".
        // Therefore, for every correct implementation:
        // abbreviate(str, lower, upper, append) == abbreviate(str, lower, str.length(), append)
        // whenever upper == -1 or upper > str.length().
        // A patch that merely deletes the crashing operation or makes the branch unreachable can violate this
        // observable equality without throwing.
        if (base.length() > 0 && (upper == -1 || upper > len)) {
            try {
                String lhs = WordUtils.abbreviate(base, lower, upper, appendToEnd);
                String rhs = WordUtils.abbreviate(base, lower, len, appendToEnd);
                if (lhs == null ? rhs != null : !lhs.equals(rhs)) {
                    throw new RuntimeException(
                            "[oracle:upper-clamp] metamorphic violation: abbreviate must clamp upper=-1/>len to str.length()"
                                    + " input="
                                    + base
                                    + " lower="
                                    + lower
                                    + " upper="
                                    + upper
                                    + " len="
                                    + len
                                    + " append="
                                    + String.valueOf(appendToEnd)
                                    + " lhs="
                                    + String.valueOf(lhs)
                                    + " rhs="
                                    + String.valueOf(rhs));
                }
            } catch (RuntimeException t) {
                if (t.getMessage() != null && t.getMessage().startsWith("[oracle:upper-clamp]")) {
                    throw t;
                }
                return;
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
- trigger: java.lang.StringIndexOutOfBoundsException: String index out of range: 15

---
## [59] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,971 chars)</summary>

```
package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact trigger from the failing test / ground truth.
        try {
            WordUtils.abbreviate("0123456789", 15, 20, null);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromAbbreviate(t)) {
                throw t;
            }
        }

        // EXPLORE:
        // Build valid-by-construction inputs for the documented/visible contract in abbreviate:
        // when upper == -1 or upper > str.length(), upper is treated as str.length().
        // For strings with no spaces, abbreviate(str, lower, hugeUpper, append) must therefore
        // agree with abbreviate(str, lower, -1, append): both normalize upper to length and
        // return the full string unchanged.
        String s = data.consumeAsciiString(64);
        if (s.length() == 0) {
            s = "A";
        }
        s = s.replace(' ', 'X');

        int len = s.length();
        int lower = len + data.consumeInt(0, 32);
        int upper = len + 1 + data.consumeInt(0, 32);

        String append = data.consumeBoolean() ? null : data.consumeAsciiString(16);

        String lhs;
        try {
            lhs = WordUtils.abbreviate(s, lower, upper, append);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromAbbreviate(t)) {
                throw t;
            }
            return;
        }

        String rhs;
        try {
            rhs = WordUtils.abbreviate(s, lower, -1, append);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromAbbreviate(t)) {
                throw t;
            }
            return;
        }

        if (!safeEquals(lhs, rhs)) {
            throw new RuntimeException(
                "[oracle:abbr-upper-normalization] metamorphic violation: abbreviate(str, lower, upper>len, append) " +
                "must agree with abbreviate(str, lower, -1, append) on no-space strings " +
                "input={str=" + quote(s) + ", lower=" + lower + ", upper=" + upper + ", append=" + quote(append) + "} " +
                "lhs=" + quote(lhs) + " rhs=" + quote(rhs)
            );
        }

        if (!safeEquals(lhs, s)) {
            throw new RuntimeException(
                "[oracle:abbr-full-string] metamorphic violation: with no spaces and upper normalized to len, result must be the original string " +
                "input={str=" + quote(s) + ", lower=" + lower + ", upper=" + upper + ", append=" + quote(append) + "} " +
                "lhs=" + quote(lhs) + " expected=" + quote(s)
            );
        }
    }

    private static boolean isRootCauseFromAbbreviate(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.lang.WordUtils".equals(e.getClassName())
                    && "abbreviate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String quote(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }
}
```

</details>

---
## [60] ⚙️ harness-attempt · `attempt_029`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.StringIndexOutOfBoundsException: String index out of range: 15

---
## [61] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,941 chars)</summary>

```
package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();

        String str = buildNonNullInput(data);
        int len = str.length();

        int lowerMode = data.consumeInt(0, 3);
        int lower;
        if (lowerMode == 0) {
            lower = 0;
        } else if (lowerMode == 1) {
            lower = len;
        } else if (lowerMode == 2) {
            lower = len + data.consumeInt(1, 32);
        } else {
            lower = data.consumeInt(0, len + 32);
        }

        int upper;
        if (data.consumeBoolean()) {
            upper = -1;
        } else {
            upper = len + data.consumeInt(1, 32);
        }

        String appendToEnd;
        int appendMode = data.consumeInt(0, 3);
        if (appendMode == 0) {
            appendToEnd = null;
        } else if (appendMode == 1) {
            appendToEnd = "";
        } else if (appendMode == 2) {
            appendToEnd = "-";
        } else {
            appendToEnd = data.consumeString(8);
        }

        boolean validByConstruction = str != null && lower >= 0 && (upper == -1 || upper > str.length());

        try {
            String lhs = WordUtils.abbreviate(str, lower, upper, appendToEnd);

            /*
             * Contract asserted from the method's own documented checks/comments shown in the patched code:
             * "if the upper value is -1 (i.e. no limit) or is greater than the length of the string, set to the length of the string".
             * Therefore for any correct implementation,
             * abbreviate(str, lower, upper, append) must equal abbreviate(str, lower, str.length(), append)
             * whenever upper == -1 or upper > str.length().
             * A patch that only suppresses the exception or skips the normalization would violate this observable equality.
             */
            try {
                String rhs = WordUtils.abbreviate(str, lower, str.length(), appendToEnd);
                if (!safeEquals(lhs, rhs)) {
                    throw new RuntimeException(
                        "[oracle:upper-normalization] metamorphic violation: upper==-1/or > len must normalize to len input="
                            + describe(str, lower, upper, appendToEnd)
                            + " lhs=" + String.valueOf(lhs)
                            + " rhs=" + String.valueOf(rhs));
                }
            } catch (Throwable t) {
                if (shouldTreatAsCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t) && validByConstruction) {
                    throwUnchecked(t);
                }
                return;
            }

            /*
             * Additional post-condition from the code path:
             * when there is no space at or after 'lower', the result is str.substring(0, normalizedUpper),
             * with appendToEnd appended only if abbreviation occurred.
             * For lower >= str.length() and normalizedUpper == str.length(), no abbreviation occurs,
             * so the full original string must be returned.
             */
            if (lower >= str.length() && (upper == -1 || upper > str.length())) {
                if (!safeEquals(lhs, str)) {
                    throw new RuntimeException(
                        "[oracle:full-string] metamorphic violation: lower beyond end with normalized upper must return original input="
                            + describe(str, lower, upper, appendToEnd)
                            + " lhs=" + String.valueOf(lhs)
                            + " rhs=" + String.valueOf(str));
                }
            }
        } catch (Throwable t) {
            if (shouldTreatAsCleanRejection(t)) {
                return;
            }
            if (isRootCause(t) && validByConstruction) {
                throwUnchecked(t);
            }
        }
    }

    private static void anchor() {
        String str = "0123456789";
        int lower = 15;
        int upper = 20;
        String appendToEnd = null;
        try {
            String result = WordUtils.abbreviate(str, lower, upper, appendToEnd);
            if (!safeEquals(result, str)) {
                throw new RuntimeException(
                    "[oracle:anchor] metamorphic violation: exact regression input must return original input="
                        + describe(str, lower, upper, appendToEnd)
                        + " lhs=" + String.valueOf(result)
                        + " rhs=" + String.valueOf(str));
            }
        } catch (Throwable t) {
            if (shouldTreatAsCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static String buildNonNullInput(FuzzedDataProvider data) {
        String base;
        if (data.consumeBoolean()) {
            base = data.consumeAsciiString(32);
        } else {
            base = data.consumeString(32);
        }
        if (base == null) {
            base = "";
        }
        if (base.length() == 0) {
            int mode = data.consumeInt(0, 3);
            if (mode == 0) {
                return "0123456789";
            } else if (mode == 1) {
                return "012 3456789";
            } else if (mode == 2) {
                return "01 23 45 67 89";
            } else {
                return "A";
            }
        }
        return base;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static boolean shouldTreatAsCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.lang.WordUtils".equals(e.getClassName())
                    && "abbreviate".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static String describe(String str, int lower, int upper, String appendToEnd) {
        return "{str=" + String.valueOf(str) + ", lower=" + lower + ", upper=" + upper
            + ", appendToEnd=" + String.valueOf(appendToEnd) + "}";
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        throw new RuntimeException(t);
    }
}
```

</details>

---
## [62] ⚙️ harness-attempt · `attempt_030`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.StringIndexOutOfBoundsException: String index out of range: 15

---
## [63] ⚙️ coverage · `jazzer coverage dumps`
**output:** **30/30 dump(s) written under /datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/cov; 30 raw fuzz log(s)**
<details><summary>▸ detail (~14,982 chars)</summary>

- detail: {'dumps': [{'harness': 'attempt_001', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/cov/attempt_001_compiled.exec', 'exists': True, 'bytes': 357}, {'harness': 'attempt_002', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/cov/attempt_002_compiled.exec', 'exists': True, 'bytes': 358}, {'harness': 'attempt_003', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/cov/attempt_003_compiled.exec', 'exists': True, 'bytes': 359}, {'harness': 'attempt_004', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/cov/attempt_004_compiled.exec', 'exists': True, 'bytes': 362}, {'harness': 'attempt_005', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/cov/attempt_005_compiled.exec', 'exists': True, 'bytes': 360}, {'harness': 'attempt_006', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/cov/attempt_006_compiled.exec', 'exists': True, 'bytes': 357}, {'harness': 'attempt_007', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/cov/attempt_007_compiled.exec', 'exists': True, 'bytes': 358}, {'harness': 'attempt_008', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/cov/attempt_008_compiled.exec', 'exists': True, 'bytes': 357}, {'harness': 'attempt_009', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/cov/attempt_009_compiled.exec', 'exists': True, 'bytes': 360}, {'harness': 'attempt_010', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/cov/attempt_010_compiled.exec', 'exists': True, 'bytes': 357}, {'harness': 'attempt_011', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/cov/attempt_011_compiled.exec', 'exists': True, 'bytes': 357}, {'harness': 'attempt_012', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/cov/attempt_012_compiled.exec', 'exists': True, 'bytes': 358}, {'harness': 'attempt_013', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/cov/attempt_013_compiled.exec', 'exists': True, 'bytes': 357}, {'harness': 'attempt_014', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/cov/attempt_014_compiled.exec', 'exists': True, 'bytes': 360}, {'harness': 'attempt_015', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/cov/attempt_015_compiled.exec', 'exists': True, 'bytes': 357}, {'harness': 'attempt_016', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/cov/attempt_016_compiled.exec', 'exists': True, 'bytes': 357}, {'harness': 'attempt_017', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/cov/attempt_017_compiled.exec', 'exists': True, 'bytes': 357}, {'harness': 'attempt_018', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/cov/attempt_018_compiled.exec', 'exists': True, 'bytes': 359}, {'harness': 'attempt_019', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/cov/attempt_019_compiled.exec', 'exists': True, 'bytes': 359}, {'harness': 'attempt_020', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/cov/attempt_020_compiled.exec', 'exists': True, 'bytes': 355}, {'harness': 'attempt_021', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/cov/attempt_021_compiled.exec', 'exists': True, 'bytes': 363}, {'harness': 'attempt_022', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/cov/attempt_022_compiled.exec', 'exists': True, 'bytes': 358}, {'harness': 'attempt_023', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/cov/attempt_023_compiled.exec', 'exists': True, 'bytes': 356}, {'harness': 'attempt_024', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/cov/attempt_024_compiled.exec', 'exists': True, 'bytes': 357}, {'harness': 'attempt_025', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/cov/attempt_025_compiled.exec', 'exists': True, 'bytes': 359}, {'harness': 'attempt_026', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/cov/attempt_026_compiled.exec', 'exists': True, 'bytes': 364}, {'harness': 'attempt_027', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/cov/attempt_027_compiled.exec', 'exists': True, 'bytes': 360}, {'harness': 'attempt_028', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/cov/attempt_028_compiled.exec', 'exists': True, 'bytes': 358}, {'harness': 'attempt_029', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/cov/attempt_029_compiled.exec', 'exists': True, 'bytes': 355}, {'harness': 'attempt_030', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/cov/attempt_030_compiled.exec', 'exists': True, 'bytes': 362}], 'outputs': ['/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/fuzz_out/attempt_001_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/fuzz_out/attempt_002_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/fuzz_out/attempt_003_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/fuzz_out/attempt_004_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/fuzz_out/attempt_005_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/fuzz_out/attempt_006_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/fuzz_out/attempt_007_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/fuzz_out/attempt_008_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/fuzz_out/attempt_009_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/fuzz_out/attempt_010_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/fuzz_out/attempt_011_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/fuzz_out/attempt_012_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/fuzz_out/attempt_013_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/fuzz_out/attempt_014_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/fuzz_out/attempt_015_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/fuzz_out/attempt_016_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/fuzz_out/attempt_017_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/fuzz_out/attempt_018_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/fuzz_out/attempt_019_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/fuzz_out/attempt_020_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/fuzz_out/attempt_021_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/fuzz_out/attempt_022_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/fuzz_out/attempt_023_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/fuzz_out/attempt_024_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/fuzz_out/attempt_025_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/fuzz_out/attempt_026_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/fuzz_out/attempt_027_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/fuzz_out/attempt_028_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/fuzz_out/attempt_029_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/fuzz_out/attempt_030_compiled.txt'], 'classpath': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/cov/classpath.json', 'dir': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/cov', 'compiled_attempts': ['attempt_001', 'attempt_002', 'attempt_003', 'attempt_004', 'attempt_005', 'attempt_006', 'attempt_007', 'attempt_008', 'attempt_009', 'attempt_010', 'attempt_011', 'attempt_012', 'attempt_013', 'attempt_014', 'attempt_015', 'attempt_016', 'attempt_017', 'attempt_018', 'attempt_019', 'attempt_020', 'attempt_021', 'attempt_022', 'attempt_023', 'attempt_024', 'attempt_025', 'attempt_026', 'attempt_027', 'attempt_028', 'attempt_029', 'attempt_030'], 'accepted_attempts': ['attempt_001', 'attempt_002', 'attempt_003', 'attempt_004', 'attempt_005', 'attempt_006', 'attempt_007', 'attempt_008', 'attempt_009', 'attempt_010', 'attempt_011', 'attempt_012', 'attempt_013', 'attempt_014', 'attempt_015', 'attempt_016', 'attempt_017', 'attempt_018', 'attempt_019', 'attempt_020', 'attempt_021', 'attempt_022', 'attempt_023', 'attempt_024', 'attempt_025', 'attempt_026', 'attempt_027', 'attempt_028', 'attempt_029', 'attempt_030'], 'sources': ['/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/harness_src/attempt_001.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/harness_src/attempt_002.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/harness_src/attempt_003.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/harness_src/attempt_004.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/harness_src/attempt_005.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/harness_src/attempt_006.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/harness_src/attempt_007.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/harness_src/attempt_008.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/harness_src/attempt_009.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/harness_src/attempt_010.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/harness_src/attempt_011.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/harness_src/attempt_012.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/harness_src/attempt_013.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/harness_src/attempt_014.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/harness_src/attempt_015.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/harness_src/attempt_016.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/harness_src/attempt_017.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/harness_src/attempt_018.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/harness_src/attempt_019.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/harness_src/attempt_020.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/harness_src/attempt_021.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/harness_src/attempt_022.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/harness_src/attempt_023.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/harness_src/attempt_024.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/harness_src/attempt_025.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/harness_src/attempt_026.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/harness_src/attempt_027.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/harness_src/attempt_028.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/harness_src/attempt_029.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/005_patch1-Lang-45-Arja_c_rep001/harness_src/attempt_030.java']}

</details>
