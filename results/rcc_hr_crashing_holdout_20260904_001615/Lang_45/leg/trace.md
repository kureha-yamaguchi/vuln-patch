# Pipeline trace — Lang-45

**Code:** `unknown`

**Patch label:** overfitting  *(the patch under analysis is a known-OVERFIT fix — the pipeline is not told this)*

**Outcome:** overfit MISSED (all harnesses quiet on the patched build). [evaluated; 3 harness(es) fuzzed on the patched build; campaign converged=True]

**Patch under analysis:**
```diff
--- /src/java/org/apache/commons/lang/WordUtils.java
+++ /src/java/org/apache/commons/lang/WordUtils.java
@@ -607,9 +607,14 @@
         if (str == null) {
             return null;
         }
+        if((lower > str.length()) == true){
+        	lower=str.length();
+
+        	}else{
         if (str.length() == 0) {
             return StringUtils.EMPTY;
         }
+        	}
 
         // if the lower value is greater than the length of the string,
         // set to the length of the string
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
<details><summary>▸ output — FailureTest(test_class='org.apache.commons.lang.WordUtilsTest', test_method='testAbbreviate', source (~2,583 chars)</summary>

**output:** 
```json
[
  "FailureTest(test_class='org.apache.commons.lang.WordUtilsTest', test_method='testAbbreviate', source_path='/tmp/d4j/Lang_45_buggy/src/test/org/apache/commons/lang/WordUtilsTest.java', method_source='    public void testAbbreviate() {\\n        // check null and empty are returned respectively\\n        assertNull(WordUtils.abbreviate(null, 1,-1,\"\"));\\n        assertEquals(StringUtils.EMPTY, WordUtils.abbreviate(\"\", 1,-1,\"\"));\\n\\n        // test upper limit\\n        assertEquals(\"01234\", WordUtils.abbreviate(\"0123456789\", 0,5,\"\"));\\n        assertEquals(\"01234\", WordUtils.abbreviate(\"0123456789\", 5, 2,\"\"));\\n        assertEquals(\"012\", WordUtils.abbreviate(\"012 3456789\", 2, 5,\"\"));\\n        assertEquals(\"012 3\", WordUtils.abbreviate(\"012 3456789\", 5, 2,\"\"));\\n        assertEquals(\"0123456789\", WordUtils.abbreviate(\"0123456789\", 0,-1,\"\"));\\n\\n        // test upper limit + append string\\n        assertEquals(\"01234-\", WordUtils.abbreviate(\"0123456789\", 0,5,\"-\"));\\n        assertEquals(\"01234-\", WordUtils.abbreviate(\"0123456789\", 5, 2,\"-\"));\\n        assertEquals(\"012\", WordUtils.abbreviate(\"012 3456789\", 2, 5, null));\\n        assertEquals(\"012 3\", WordUtils.abbreviate(\"012 3456789\", 5, 2,\"\"));\\n        assertEquals(\"0123456789\", WordUtils.abbreviate(\"0123456789\", 0,-1,\"\"));\\n\\n        // test lower value\\n        assertEquals(\"012\", WordUtils.abbreviate(\"012 3456789\", 0,5, null));\\n        assertEquals(\"01234\", WordUtils.abbreviate(\"01234 56789\", 5, 10, null));\\n        assertEquals(\"01 23 45 67\", WordUtils.abbreviate(\"01 23 45 67 89\", 9, -1, null));\\n        assertEquals(\"01 23 45 6\", WordUtils.abbreviate(\"01 23 45 67 89\", 9, 10, null));\\n        assertEquals(\"0123456789\", WordUtils.abbreviate(\"0123456789\", 15, 20, null));\\n\\n        // test lower value + append\\n        assertEquals(\"012\", WordUtils.abbreviate(\"012 3456789\", 0,5, null));\\n        assertEquals(\"01234-\", WordUtils.abbreviate(\"01234 56789\", 5, 10, \"-\"));\\n        assertEquals(\"01 23 45 67abc\", WordUtils.abbreviate(\"01 23 45 67 89\", 9, -1, \"abc\"));\\n        assertEquals(\"01 23 45 6\", WordUtils.abbreviate(\"01 23 45 67 89\", 9, 10, \"\"));\\n\\n        // others\\n        assertEquals(\"\", WordUtils.abbreviate(\"0123456790\", 0,0,\"\"));\\n        assertEquals(\"\", WordUtils.abbreviate(\" 0123456790\", 0,-1,\"\"));\\n    }', exception_type='java.lang.StringIndexOutOfBoundsException', failure_message=None, support_source=None)"
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
<details><summary>▸ output — modified_files": [ (~12,543 chars)</summary>

**output:** 
```json
{
  "modified_files": [
    "src/java/org/apache/commons/lang/WordUtils.java"
  ],
  "patch_text": "--- /src/java/org/apache/commons/lang/WordUtils.java\n+++ /src/java/org/apache/commons/lang/WordUtils.java\n@@ -607,9 +607,14 @@\n         if (str == null) {\n             return null;\n         }\n+        if((lower > str.length()) == true){\n+        \tlower=str.length();\n+\n+        \t}else{\n         if (str.length() == 0) {\n             return StringUtils.EMPTY;\n         }\n+        \t}\n \n         // if the lower value is greater than the length of the string,\n         // set to the length of the string\n",
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
  "source_imports": []
}
```

</details>

---
## [3] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~31,407 chars)</summary>

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
@@ -607,9 +607,14 @@
         if (str == null) {
             return null;
         }
+        if((lower > str.length()) == true){
+        	lower=str.length();
+
+        	}else{
         if (str.length() == 0) {
             return StringUtils.EMPTY;
         }
+        	}
 
         // if the lower value is greater than the length of the string,
         // set to the length of the string

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
Call-site examples (use these as a guide for constructing the target call):
<xref>
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
        assertEquals("0123456789", WordUtils.abbreviate("0123456789", 15, 20, null));

        // test lower value + append
        assertEquals("012", WordUtils.abbreviate("012 3456789", 0,5, null));
        assertEquals("01234-", WordUtils.abbreviate("01234 56789", 5, 10, "-"));
        assertEquals("01 23 45 67abc", WordUtils.abbreviate("01 23 45 67 89", 9, -1, "abc"));
        assertEquals("01 23 45 6", WordUtils.abbreviate("01 23 45 67 89", 9, 10, ""));

        // others
        assertEquals("", WordUtils.abbreviate("0123456790", 0,0,""));
        assertEquals("", WordUtils.abbreviate(" 0123456790", 0,-1,""));
    }
</xref>
Methods called by `abbreviate` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="length" from="StringUtils.java">
<signature>
public static int length(String str)
</signature>
<code>
    public static int length(String str) {
        return str == null ? 0 : str.length();
    }
</code>
</callee>
<callee name="indexOf" from="ArrayUtils.java">
<signature>
public static int indexOf(Object[] array, Object objectToFind)
</signature>
<code>
    public static int indexOf(Object[] array, Object objectToFind) {
        return indexOf(array, objectToFind, 0);
    }
</code>
<implementation in="ArrayUtils.java">
    public static int indexOf(Object[] array, Object objectToFind) {
        return indexOf(array, objectToFind, 0);
    }
</implementation>
<implementation in="ArrayUtils.java">
    public static int indexOf(Object[] array, Object objectToFind, int startIndex) {
        if (array == null) {
            return INDEX_NOT_FOUND;
        }
        if (startIndex < 0) {
            startIndex = 0;
        }
        if (objectToFind == null) {
            for (int i = startIndex; i < array.length; i++) {
                if (array[i] == null) {
                    return i;
                }
            }
        } else {
            for (int i = startIndex; i < array.length; i++) {
                if (objectToFind.equals(array[i])) {
                    return i;
                }
            }
        }
        return INDEX_NOT_FOUND;
    }
</implementation>
<implementation in="ArrayUtils.java">
    public static int indexOf(long[] array, long valueToFind) {
        return indexOf(array, valueToFind, 0);
    }
</implementation>
<implementation in="ArrayUtils.java">
    public static int indexOf(long[] array, long valueToFind, int startIndex) {
        if (array == null) {
            return INDEX_NOT_FOUND;
        }
        if (startIndex < 0) {
            startIndex = 0;
        }
        for (int i = startIndex; i < array.length; i++) {
            if (valueToFind == array[i]) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }
</implementation>
</callee>
<callee name="substring" from="StringUtils.java">
<signature>
public static String substring(String str, int start)
</signature>
<code>
    public static String substring(String str, int start) {
        if (str == null) {
            return null;
        }

        // handle negatives, which means last n characters
        if (start < 0) {
            start = str.length() + start; // remember start is negative
        }

        if (start < 0) {
            start = 0;
        }
        if (start > str.length()) {
            return EMPTY;
        }

        return str.substring(start);
    }
</code>
<implementation in="StringUtils.java">
    public static String substring(String str, int start) {
        if (str == null) {
            return null;
        }

        // handle negatives, which means last n characters
        if (start < 0) {
            start = str.length() + start; // remember start is negative
        }

        if (start < 0) {
            start = 0;
        }
        if (start > str.length()) {
            return EMPTY;
        }

        return str.substring(start);
    }
</implementation>
<implementation in="StringUtils.java">
    public static String substring(String str, int start, int end) {
        if (str == null) {
            return null;
        }

        // handle negatives
        if (end < 0) {
            end = str.length() + end; // remember end is negative
        }
        if (start < 0) {
            start = str.length() + start; // remember start is negative
        }

        // check length next
        if (end > str.length()) {
            end = str.length();
        }

        // if start is greater than end, return ""
        if (start > end) {
            return EMPTY;
        }

        if (start < 0) {
            start = 0;
        }
        if (end < 0) {
            end = 0;
        }

        return str.substring(start, end);
    }
</implementation>
</callee>
<callee name="defaultString" from="StringUtils.java">
<signature>
public static String defaultString(String str)
</signature>
<code>
    public static String defaultString(String str) {
        return str == null ? EMPTY : str;
    }
</code>
<implementation in="StringUtils.java">
    public static String defaultString(String str) {
        return str == null ? EMPTY : str;
    }
</implementation>
<implementation in="StringUtils.java">
    public static String defaultString(String str, String defaultStr) {
        return str == null ? defaultStr : str;
    }
</implementation>
</callee>
<callee name="toString" from="ArrayUtils.java">
<signature>
public static String toString(Object array)
</signature>
<code>
    public static String toString(Object array) {
        return toString(array, "{}");
    }
</code>
<implementation in="ArrayUtils.java">
    public static String toString(Object array) {
        return toString(array, "{}");
    }
</implementation>
<implementation in="ArrayUtils.java">
    public static String toString(Object array, String stringIfNull) {
        if (array == null) {
            return stringIfNull;
        }
        return new ToStringBuilder(array, ToStringStyle.SIMPLE_STYLE).append(array).toString();
    }
</implementation>
<implementation in="BooleanUtils.java">
    public static String toString(Boolean bool, String trueString, String falseString, String nullString) {
        if (bool == null) {
            return nullString;
        }
        return bool.booleanValue() ? trueString : falseString;
    }
</implementation>
<implementation in="BooleanUtils.java">
    public static String toString(boolean bool, String trueString, String falseString) {
        return bool ? trueString : falseString;
    }
</implementation>
</callee>

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `WordUtils`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.StringIndexOutOfBoundsException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `abbreviate` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
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

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- StringUtils.indexOf
- StringUtils.defaultString
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
<details open><summary>▸ Output (~5,623 chars)</summary>

```
package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        callAnchor();

        String str = data.consumeAsciiString(64);
        if (str == null || str.length() == 0) {
            str = "A";
        }

        String appendToEnd;
        if (data.consumeBoolean()) {
            appendToEnd = null;
        } else {
            appendToEnd = data.consumeAsciiString(16);
        }

        int len = str.length();

        int lower;
        if (data.consumeBoolean()) {
            lower = len + data.consumeInt(1, 32);
        } else {
            lower = len;
        }

        int upperChoice = data.consumeInt(0, 4);
        int upper;
        if (upperChoice == 0) {
            upper = -1;
        } else if (upperChoice == 1) {
            upper = len + data.consumeInt(0, 32);
        } else if (upperChoice == 2) {
            upper = data.consumeInt(0, len);
        } else if (upperChoice == 3) {
            upper = lower + data.consumeInt(0, 16);
        } else {
            upper = data.consumeInt(-8, len + 32);
        }

        try {
            String actual = WordUtils.abbreviate(str, lower, upper, appendToEnd);

            /*
             * Contract visible in WordUtils.abbreviate:
             * "if the lower value is greater than the length of the string, set to the length of the string".
             * Therefore, for every correct implementation and every non-null non-empty string:
             * abbreviate(str, lower>len, upper, append) must equal abbreviate(str, len, upper, append).
             * A patch that merely suppresses the exception or skips the clamp would violate this relation.
             */
            try {
                String clamped = WordUtils.abbreviate(str, len, upper, appendToEnd);
                if (lower > len) {
                    if (actual == null ? clamped != null : !actual.equals(clamped)) {
                        throw new RuntimeException(
                            "[oracle:lower-clamp] metamorphic violation: abbreviate(lower>len) must equal abbreviate(lower=len)"
                                + " input=" + str
                                + " len=" + len
                                + " lower=" + lower
                                + " upper=" + upper
                                + " append=" + appendToEnd
                                + " lhs=" + actual
                                + " rhs=" + clamped);
                    }
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

            try {
                String once = actual;
                String twice = WordUtils.abbreviate(once, lower, upper, appendToEnd);
                if (once != null && twice != null && lower >= once.length()) {
                    if (!once.equals(twice)) {
                        throw new RuntimeException(
                            "[oracle:idempotence] metamorphic violation: repeated abbreviation should stabilize after clamp"
                                + " input=" + str
                                + " first=" + once
                                + " second=" + twice
                                + " lower=" + lower
                                + " upper=" + upper
                                + " append=" + appendToEnd);
                    }
                }
            } catch (RuntimeException t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throw t;
                }
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

    private static void callAnchor() {
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
    }

    private static boolean isCleanRejection(Throwable t) {
        while (t != null) {
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException) && !(t instanceof IndexOutOfBoundsException)) {
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
            if ("org.apache.commons.lang.WordUtils".equals(cls) && "abbreviate".equals(method)) {
                return true;
            }
            if ("org.apache.commons.lang.StringUtils".equals(cls)
                    && ("indexOf".equals(method) || "defaultString".equals(method))) {
                return true;
            }
        }
        return false;
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
<details><summary>▸ Prompt (2 message(s), ~32,672 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
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
@@ -607,9 +607,14 @@
         if (str == null) {
             return null;
         }
+        if((lower > str.length()) == true){
+        	lower=str.length();
+
+        	}else{
         if (str.length() == 0) {
             return StringUtils.EMPTY;
         }
+        	}
 
         // if the lower value is greater than the length of the string,
         // set to the length of the string

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
Call-site examples (use these as a guide for constructing the target call):
<xref>
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
        assertEquals("0123456789", WordUtils.abbreviate("0123456789", 15, 20, null));

        // test lower value + append
        assertEquals("012", WordUtils.abbreviate("012 3456789", 0,5, null));
        assertEquals("01234-", WordUtils.abbreviate("01234 56789", 5, 10, "-"));
        assertEquals("01 23 45 67abc", WordUtils.abbreviate("01 23 45 67 89", 9, -1, "abc"));
        assertEquals("01 23 45 6", WordUtils.abbreviate("01 23 45 67 89", 9, 10, ""));

        // others
        assertEquals("", WordUtils.abbreviate("0123456790", 0,0,""));
        assertEquals("", WordUtils.abbreviate(" 0123456790", 0,-1,""));
    }
</xref>
Methods called by `abbreviate` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="length" from="StringUtils.java">
<signature>
public static int length(String str)
</signature>
<code>
    public static int length(String str) {
        return str == null ? 0 : str.length();
    }
</code>
</callee>
<callee name="indexOf" from="ArrayUtils.java">
<signature>
public static int indexOf(Object[] array, Object objectToFind)
</signature>
<code>
    public static int indexOf(Object[] array, Object objectToFind) {
        return indexOf(array, objectToFind, 0);
    }
</code>
<implementation in="ArrayUtils.java">
    public static int indexOf(Object[] array, Object objectToFind) {
        return indexOf(array, objectToFind, 0);
    }
</implementation>
<implementation in="ArrayUtils.java">
    public static int indexOf(Object[] array, Object objectToFind, int startIndex) {
        if (array == null) {
            return INDEX_NOT_FOUND;
        }
        if (startIndex < 0) {
            startIndex = 0;
        }
        if (objectToFind == null) {
            for (int i = startIndex; i < array.length; i++) {
                if (array[i] == null) {
                    return i;
                }
            }
        } else {
            for (int i = startIndex; i < array.length; i++) {
                if (objectToFind.equals(array[i])) {
                    return i;
                }
            }
        }
        return INDEX_NOT_FOUND;
    }
</implementation>
<implementation in="ArrayUtils.java">
    public static int indexOf(long[] array, long valueToFind) {
        return indexOf(array, valueToFind, 0);
    }
</implementation>
<implementation in="ArrayUtils.java">
    public static int indexOf(long[] array, long valueToFind, int startIndex) {
        if (array == null) {
            return INDEX_NOT_FOUND;
        }
        if (startIndex < 0) {
            startIndex = 0;
        }
        for (int i = startIndex; i < array.length; i++) {
            if (valueToFind == array[i]) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }
</implementation>
</callee>
<callee name="substring" from="StringUtils.java">
<signature>
public static String substring(String str, int start)
</signature>
<code>
    public static String substring(String str, int start) {
        if (str == null) {
            return null;
        }

        // handle negatives, which means last n characters
        if (start < 0) {
            start = str.length() + start; // remember start is negative
        }

        if (start < 0) {
            start = 0;
        }
        if (start > str.length()) {
            return EMPTY;
        }

        return str.substring(start);
    }
</code>
<implementation in="StringUtils.java">
    public static String substring(String str, int start) {
        if (str == null) {
            return null;
        }

        // handle negatives, which means last n characters
        if (start < 0) {
            start = str.length() + start; // remember start is negative
        }

        if (start < 0) {
            start = 0;
        }
        if (start > str.length()) {
            return EMPTY;
        }

        return str.substring(start);
    }
</implementation>
<implementation in="StringUtils.java">
    public static String substring(String str, int start, int end) {
        if (str == null) {
            return null;
        }

        // handle negatives
        if (end < 0) {
            end = str.length() + end; // remember end is negative
        }
        if (start < 0) {
            start = str.length() + start; // remember start is negative
        }

        // check length next
        if (end > str.length()) {
            end = str.length();
        }

        // if start is greater than end, return ""
        if (start > end) {
            return EMPTY;
        }

        if (start < 0) {
            start = 0;
        }
        if (end < 0) {
            end = 0;
        }

        return str.substring(start, end);
    }
</implementation>
</callee>
<callee name="defaultString" from="StringUtils.java">
<signature>
public static String defaultString(String str)
</signature>
<code>
    public static String defaultString(String str) {
        return str == null ? EMPTY : str;
    }
</code>
<implementation in="StringUtils.java">
    public static String defaultString(String str) {
        return str == null ? EMPTY : str;
    }
</implementation>
<implementation in="StringUtils.java">
    public static String defaultString(String str, String defaultStr) {
        return str == null ? defaultStr : str;
    }
</implementation>
</callee>
<callee name="toString" from="ArrayUtils.java">
<signature>
public static String toString(Object array)
</signature>
<code>
    public static String toString(Object array) {
        return toString(array, "{}");
    }
</code>
<implementation in="ArrayUtils.java">
    public static String toString(Object array) {
        return toString(array, "{}");
    }
</implementation>
<implementation in="ArrayUtils.java">
    public static String toString(Object array, String stringIfNull) {
        if (array == null) {
            return stringIfNull;
        }
        return new ToStringBuilder(array, ToStringStyle.SIMPLE_STYLE).append(array).toString();
    }
</implementation>
<implementation in="BooleanUtils.java">
    public static String toString(Boolean bool, String trueString, String falseString, String nullString) {
        if (bool == null) {
            return nullString;
        }
        return bool.booleanValue() ? trueString : falseString;
    }
</implementation>
<implementation in="BooleanUtils.java">
    public static String toString(boolean bool, String trueString, String falseString) {
        return bool ? trueString : falseString;
    }
</implementation>
</callee>

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `WordUtils`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.StringIndexOutOfBoundsException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `abbreviate` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
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

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- StringUtils.indexOf
- StringUtils.defaultString
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.lang.FuzzHarness.callAnchor
- org.apache.commons.lang.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.lang.WordUtils.abbreviate
Crashes already found:
- java.lang.StringIndexOutOfBoundsException@org.apache.commons.lang.WordUtils.abbreviate
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- StringUtils.indexOf
- StringUtils.defaultString
Check FAMILIES already covered by accepted harnesses: {idempotence, lower-clamp}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.StringIndexOutOfBoundsException@org.apache.commons.lang.WordUtils.abbreviate. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
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
<details open><summary>▸ Output (~4,873 chars)</summary>

```
package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        callAnchor();

        String base = data.consumeAsciiString(24);
        if (base.length() == 0) {
            base = "A";
        }
        if (data.consumeBoolean()) {
            int pos = data.consumeInt(0, base.length());
            base = base.substring(0, pos) + " " + base.substring(pos);
        }

        int len = base.length();
        int lower = len + data.consumeInt(0, 8);
        int upperChoice = data.consumeInt(0, 2);
        int upper;
        if (upperChoice == 0) {
            upper = -1;
        } else if (upperChoice == 1) {
            upper = len + data.consumeInt(0, 8);
        } else {
            upper = lower + data.consumeInt(0, 8);
        }
        String append = data.consumeBoolean() ? null : data.consumeAsciiString(8);

        try {
            String result = WordUtils.abbreviate(base, lower, upper, append);
            if (!base.equals(result)) {
                throw new RuntimeException(
                    "[oracle:boundary-original] metamorphic violation: for non-empty input and lower>=length with upper>=length/-1, abbreviate must return the original string input="
                        + base + " lower=" + lower + " upper=" + upper + " append=" + append + " result=" + result);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw t;
            }
        }

        // Contract/oracle: appendToEnd is passed through StringUtils.defaultString, so null and ""
        // are equivalent whenever the call succeeds. This catches throw-deleting or wrong-output fixes.
        String left = token(data.consumeAsciiString(8));
        String right = token(data.consumeAsciiString(8));
        String spaced = left + " " + right;
        int spaceIndex = left.length();
        int lower2 = data.consumeInt(0, spaceIndex);
        int upper2 = data.consumeInt(0, Math.max(0, spaced.length() - 1));

        try {
            String withNull = WordUtils.abbreviate(spaced, lower2, upper2, null);
            String withEmpty = WordUtils.abbreviate(spaced, lower2, upper2, "");
            if (!withNull.equals(withEmpty)) {
                throw new RuntimeException(
                    "[oracle:null-empty-append] metamorphic violation: abbreviate(..., null) must equal abbreviate(..., \"\") input="
                        + spaced + " lower=" + lower2 + " upper=" + upper2 + " lhs=" + withNull + " rhs=" + withEmpty);
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

    private static void callAnchor() {
        String s = "0123456789";
        int lower = 15;
        int upper = 20;
        try {
            String result = WordUtils.abbreviate(s, lower, upper, null);
            if (!s.equals(result)) {
                throw new RuntimeException(
                    "[oracle:anchor-result] metamorphic violation: documented boundary case must return original string input="
                        + s + " lower=" + lower + " upper=" + upper + " result=" + result);
            }
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            if (!isCleanRejection(t)) {
                throw t;
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
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
            String cls = e.getClassName();
            String method = e.getMethodName();
            if (("org.apache.commons.lang.WordUtils".equals(cls) && "abbreviate".equals(method))
                || ("org.apache.commons.lang.StringUtils".equals(cls)
                    && ("indexOf".equals(method) || "defaultString".equals(method)))) {
                return true;
            }
        }
        return false;
    }

    private static String token(String s) {
        if (s == null || s.length() == 0) {
            return "X";
        }
        String cleaned = s.replace(' ', 'X');
        return cleaned.length() == 0 ? "X" : cleaned;
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
<details><summary>▸ Prompt (2 message(s), ~32,725 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
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
@@ -607,9 +607,14 @@
         if (str == null) {
             return null;
         }
+        if((lower > str.length()) == true){
+        	lower=str.length();
+
+        	}else{
         if (str.length() == 0) {
             return StringUtils.EMPTY;
         }
+        	}
 
         // if the lower value is greater than the length of the string,
         // set to the length of the string

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
Call-site examples (use these as a guide for constructing the target call):
<xref>
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
        assertEquals("0123456789", WordUtils.abbreviate("0123456789", 15, 20, null));

        // test lower value + append
        assertEquals("012", WordUtils.abbreviate("012 3456789", 0,5, null));
        assertEquals("01234-", WordUtils.abbreviate("01234 56789", 5, 10, "-"));
        assertEquals("01 23 45 67abc", WordUtils.abbreviate("01 23 45 67 89", 9, -1, "abc"));
        assertEquals("01 23 45 6", WordUtils.abbreviate("01 23 45 67 89", 9, 10, ""));

        // others
        assertEquals("", WordUtils.abbreviate("0123456790", 0,0,""));
        assertEquals("", WordUtils.abbreviate(" 0123456790", 0,-1,""));
    }
</xref>
Methods called by `abbreviate` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="length" from="StringUtils.java">
<signature>
public static int length(String str)
</signature>
<code>
    public static int length(String str) {
        return str == null ? 0 : str.length();
    }
</code>
</callee>
<callee name="indexOf" from="ArrayUtils.java">
<signature>
public static int indexOf(Object[] array, Object objectToFind)
</signature>
<code>
    public static int indexOf(Object[] array, Object objectToFind) {
        return indexOf(array, objectToFind, 0);
    }
</code>
<implementation in="ArrayUtils.java">
    public static int indexOf(Object[] array, Object objectToFind) {
        return indexOf(array, objectToFind, 0);
    }
</implementation>
<implementation in="ArrayUtils.java">
    public static int indexOf(Object[] array, Object objectToFind, int startIndex) {
        if (array == null) {
            return INDEX_NOT_FOUND;
        }
        if (startIndex < 0) {
            startIndex = 0;
        }
        if (objectToFind == null) {
            for (int i = startIndex; i < array.length; i++) {
                if (array[i] == null) {
                    return i;
                }
            }
        } else {
            for (int i = startIndex; i < array.length; i++) {
                if (objectToFind.equals(array[i])) {
                    return i;
                }
            }
        }
        return INDEX_NOT_FOUND;
    }
</implementation>
<implementation in="ArrayUtils.java">
    public static int indexOf(long[] array, long valueToFind) {
        return indexOf(array, valueToFind, 0);
    }
</implementation>
<implementation in="ArrayUtils.java">
    public static int indexOf(long[] array, long valueToFind, int startIndex) {
        if (array == null) {
            return INDEX_NOT_FOUND;
        }
        if (startIndex < 0) {
            startIndex = 0;
        }
        for (int i = startIndex; i < array.length; i++) {
            if (valueToFind == array[i]) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }
</implementation>
</callee>
<callee name="substring" from="StringUtils.java">
<signature>
public static String substring(String str, int start)
</signature>
<code>
    public static String substring(String str, int start) {
        if (str == null) {
            return null;
        }

        // handle negatives, which means last n characters
        if (start < 0) {
            start = str.length() + start; // remember start is negative
        }

        if (start < 0) {
            start = 0;
        }
        if (start > str.length()) {
            return EMPTY;
        }

        return str.substring(start);
    }
</code>
<implementation in="StringUtils.java">
    public static String substring(String str, int start) {
        if (str == null) {
            return null;
        }

        // handle negatives, which means last n characters
        if (start < 0) {
            start = str.length() + start; // remember start is negative
        }

        if (start < 0) {
            start = 0;
        }
        if (start > str.length()) {
            return EMPTY;
        }

        return str.substring(start);
    }
</implementation>
<implementation in="StringUtils.java">
    public static String substring(String str, int start, int end) {
        if (str == null) {
            return null;
        }

        // handle negatives
        if (end < 0) {
            end = str.length() + end; // remember end is negative
        }
        if (start < 0) {
            start = str.length() + start; // remember start is negative
        }

        // check length next
        if (end > str.length()) {
            end = str.length();
        }

        // if start is greater than end, return ""
        if (start > end) {
            return EMPTY;
        }

        if (start < 0) {
            start = 0;
        }
        if (end < 0) {
            end = 0;
        }

        return str.substring(start, end);
    }
</implementation>
</callee>
<callee name="defaultString" from="StringUtils.java">
<signature>
public static String defaultString(String str)
</signature>
<code>
    public static String defaultString(String str) {
        return str == null ? EMPTY : str;
    }
</code>
<implementation in="StringUtils.java">
    public static String defaultString(String str) {
        return str == null ? EMPTY : str;
    }
</implementation>
<implementation in="StringUtils.java">
    public static String defaultString(String str, String defaultStr) {
        return str == null ? defaultStr : str;
    }
</implementation>
</callee>
<callee name="toString" from="ArrayUtils.java">
<signature>
public static String toString(Object array)
</signature>
<code>
    public static String toString(Object array) {
        return toString(array, "{}");
    }
</code>
<implementation in="ArrayUtils.java">
    public static String toString(Object array) {
        return toString(array, "{}");
    }
</implementation>
<implementation in="ArrayUtils.java">
    public static String toString(Object array, String stringIfNull) {
        if (array == null) {
            return stringIfNull;
        }
        return new ToStringBuilder(array, ToStringStyle.SIMPLE_STYLE).append(array).toString();
    }
</implementation>
<implementation in="BooleanUtils.java">
    public static String toString(Boolean bool, String trueString, String falseString, String nullString) {
        if (bool == null) {
            return nullString;
        }
        return bool.booleanValue() ? trueString : falseString;
    }
</implementation>
<implementation in="BooleanUtils.java">
    public static String toString(boolean bool, String trueString, String falseString) {
        return bool ? trueString : falseString;
    }
</implementation>
</callee>

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `WordUtils`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.StringIndexOutOfBoundsException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `abbreviate` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
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

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- StringUtils.indexOf
- StringUtils.defaultString
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.lang.FuzzHarness.callAnchor
- org.apache.commons.lang.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.lang.WordUtils.abbreviate
Crashes already found:
- java.lang.StringIndexOutOfBoundsException@org.apache.commons.lang.WordUtils.abbreviate
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- StringUtils.indexOf
- StringUtils.defaultString
Check FAMILIES already covered by accepted harnesses: {anchor-result, boundary-original, idempotence, lower-clamp, null-empty-append}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.StringIndexOutOfBoundsException@org.apache.commons.lang.WordUtils.abbreviate. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
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
<details open><summary>▸ Output (~6,689 chars)</summary>

```
package org.apache.commons.lang;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        callAnchor();

        String str = data.consumeAsciiString(40);
        if (str == null) {
            return;
        }
        if (str.length() == 0) {
            str = "A";
        }

        int len = str.length();
        int lower = len + data.consumeInt(1, 20);

        int upperChoice = data.consumeInt(0, 5);
        int upper;
        switch (upperChoice) {
            case 0:
                upper = -1;
                break;
            case 1:
                upper = len;
                break;
            case 2:
                upper = len + data.consumeInt(1, 20);
                break;
            case 3:
                upper = data.consumeInt(0, len);
                break;
            case 4:
                upper = lower - data.consumeInt(0, 5);
                break;
            default:
                upper = lower + data.consumeInt(0, 5);
                break;
        }

        String append = data.consumeBoolean() ? null : data.consumeAsciiString(8);

        exerciseAndCheck(str, lower, upper, append);

        String spaced = makeSpacedVariant(str, data.consumeInt(0, str.length()));
        if (spaced.length() > 0) {
            int lower2 = spaced.length() + data.consumeInt(1, 20);
            int upper2 = data.consumeBoolean() ? -1 : spaced.length() + data.consumeInt(0, 20);
            String append2 = data.consumeBoolean() ? null : data.consumeAsciiString(8);
            exerciseAndCheck(spaced, lower2, upper2, append2);
        }
    }

    private static void callAnchor() {
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
    }

    private static void exerciseAndCheck(String str, int lower, int upper, String appendToEnd) {
        String actual;
        try {
            actual = WordUtils.abbreviate(str, lower, upper, appendToEnd);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t) && lower > str.length() && str != null && str.length() > 0) {
                throw t;
            }
            return;
        }

        String expected;
        try {
            expected = recomputeViaHelpers(str, lower, upper, appendToEnd);
        } catch (RuntimeException t) {
            return;
        }

        /*
         * Contract visible in the method body and tests:
         * abbreviate computes its result from a clamp of lower/upper, then from
         * StringUtils.indexOf(..., lower), StringUtils.substring(...), and
         * StringUtils.defaultString(appendToEnd). A throw-deleting or overfit patch
         * could silence the known crash yet still produce the wrong string. This
         * oracle recomputes the same documented quantity through the reachable helper
         * functions and requires agreement.
         */
        if (actual == null ? expected != null : !actual.equals(expected)) {
            throw new RuntimeException(
                "[oracle:helper-recompute] metamorphic violation: abbreviate disagrees with helper recomputation"
                    + " input=" + str
                    + " lower=" + lower
                    + " upper=" + upper
                    + " append=" + appendToEnd
                    + " lhs=" + actual
                    + " rhs=" + expected);
        }
    }

    private static String recomputeViaHelpers(String str, int lower, int upper, String appendToEnd) {
        if (str == null) {
            return null;
        }
        if (str.length() == 0) {
            return StringUtils.EMPTY;
        }

        int effectiveLower = lower;
        if (effectiveLower > str.length()) {
            effectiveLower = str.length();
        }

        int effectiveUpper = upper;
        if (effectiveUpper == -1 || effectiveUpper > str.length()) {
            effectiveUpper = str.length();
        }
        if (effectiveUpper < effectiveLower) {
            effectiveUpper = effectiveLower;
        }

        StringBuffer result = new StringBuffer();
        int index = StringUtils.indexOf(str, " ", effectiveLower);
        if (index == -1) {
            result.append(StringUtils.substring(str, 0, effectiveUpper));
            if (effectiveUpper != str.length()) {
                result.append(StringUtils.defaultString(appendToEnd));
            }
        } else if (index > effectiveUpper) {
            result.append(StringUtils.substring(str, 0, effectiveUpper));
            result.append(StringUtils.defaultString(appendToEnd));
        } else {
            result.append(StringUtils.substring(str, 0, index));
            result.append(StringUtils.defaultString(appendToEnd));
        }
        return result.toString();
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
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String method = ste.getMethodName();
            if (("org.apache.commons.lang.WordUtils".equals(cls) && "abbreviate".equals(method))
                || ("org.apache.commons.lang.StringUtils".equals(cls) && "indexOf".equals(method))
                || ("org.apache.commons.lang.StringUtils".equals(cls) && "defaultString".equals(method))) {
                return true;
            }
        }
        return false;
    }

    private static String makeSpacedVariant(String s, int pos) {
        if (s == null) {
            return null;
        }
        if (s.length() == 0) {
            return " ";
        }
        if (pos < 0) {
            pos = 0;
        }
        if (pos > s.length()) {
            pos = s.length();
        }
        return s.substring(0, pos) + " " + s.substring(pos);
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
## [9] ⚙️ corpus-seed · `attempt_001`
**output:** **96 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['));\n        assertEquals(StringUtils.EMPTY, WordUtils.abbreviate(', ', 1,-1,', '));\n\n        // test upper limit\n        assertEquals(', ', WordUtils.abbreviate(', ', 0,5,', '));\n        assertEquals(', ', 5, 2,', ', 2, 5,']}

---
## [10] ⚙️ corpus-seed · `attempt_002`
**output:** **96 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['));\n        assertEquals(StringUtils.EMPTY, WordUtils.abbreviate(', ', 1,-1,', '));\n\n        // test upper limit\n        assertEquals(', ', WordUtils.abbreviate(', ', 0,5,', '));\n        assertEquals(', ', 5, 2,', ', 2, 5,']}

---
## [11] ⚙️ corpus-seed · `attempt_003`
**output:** **96 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['));\n        assertEquals(StringUtils.EMPTY, WordUtils.abbreviate(', ', 1,-1,', '));\n\n        // test upper limit\n        assertEquals(', ', WordUtils.abbreviate(', ', 0,5,', '));\n        assertEquals(', ', 5, 2,', ', 2, 5,']}

---
## [12] ⚙️ patched-fuzz · `attempt_001`
**output:** **quiet on patched build (no overfit signal)**

---
## [13] ⚙️ patched-fuzz · `attempt_002`
**output:** **quiet on patched build (no overfit signal)**

---
## [14] ⚙️ patched-fuzz · `attempt_003`
**output:** **quiet on patched build (no overfit signal)**
