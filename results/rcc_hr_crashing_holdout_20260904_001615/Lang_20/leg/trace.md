# Pipeline trace — Lang-20

**Code:** `unknown`

**Patch label:** overfitting  *(the patch under analysis is a known-OVERFIT fix — the pipeline is not told this)*

**Outcome:** OVERFIT CAUGHT (a harness fired on the patched build). [evaluated; 3 harness(es) fuzzed on the patched build; campaign converged=True]

**Patch under analysis:**
```diff
--- /src/main/java/org/apache/commons/lang3/StringUtils.java	
+++ /src/main/java/org/apache/commons/lang3/StringUtils.java	
@@ -3295,8 +3295,7 @@
             return EMPTY;
         }
         
-        StringBuilder buf = new StringBuilder((array[startIndex] == null ? 16 : array[startIndex].toString().length()) + 1);
-
+        StringBuilder buf=new StringBuilder(256);
         for (int i = startIndex; i < endIndex; i++) {
             if (i > startIndex) {
                 buf.append(separator);
@@ -3380,7 +3379,7 @@
             return EMPTY;
         }
 
-        StringBuilder buf = new StringBuilder((array[startIndex] == null ? 16 : array[startIndex].toString().length()) + separator.length());
+        StringBuilder buf=new StringBuilder(4);
 
         for (int i = startIndex; i < endIndex; i++) {
             if (i > startIndex) {
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
<details><summary>▸ output — FailureTest(test_class='org.apache.commons.lang3.StringUtilsTest', test_method='testJoin_ArrayChar', (~2,560 chars)</summary>

**output:** 
```json
[
  "FailureTest(test_class='org.apache.commons.lang3.StringUtilsTest', test_method='testJoin_ArrayChar', source_path='/tmp/d4j/Lang_20_buggy/src/test/java/org/apache/commons/lang3/StringUtilsTest.java', method_source='    public void testJoin_ArrayChar() {\\n        assertEquals(null, StringUtils.join((Object[]) null, \\',\\'));\\n        assertEquals(TEXT_LIST_CHAR, StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR));\\n        assertEquals(\"\", StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR));\\n        assertEquals(\";;foo\", StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR));\\n        assertEquals(\"foo;2\", StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR));\\n\\n        assertEquals(\"/\", StringUtils.join(MIXED_ARRAY_LIST, \\'/\\', 0, MIXED_ARRAY_LIST.length-1));\\n        assertEquals(\"foo\", StringUtils.join(MIXED_TYPE_LIST, \\'/\\', 0, 1));\\n        assertEquals(\"null\", StringUtils.join(NULL_TO_STRING_LIST,\\'/\\', 0, 1));\\n        assertEquals(\"foo/2\", StringUtils.join(MIXED_TYPE_LIST, \\'/\\', 0, 2));\\n        assertEquals(\"2\", StringUtils.join(MIXED_TYPE_LIST, \\'/\\', 1, 2));\\n        assertEquals(\"\", StringUtils.join(MIXED_TYPE_LIST, \\'/\\', 2, 1));\\n    }', exception_type='java.lang.NullPointerException', failure_message=None, support_source=None)",
  "FailureTest(test_class='org.apache.commons.lang3.StringUtilsTest', test_method='testJoin_Objectarray', source_path='/tmp/d4j/Lang_20_buggy/src/test/java/org/apache/commons/lang3/StringUtilsTest.java', method_source='    public void testJoin_Objectarray() {\\n//        assertEquals(null, StringUtils.join(null)); // generates warning\\n        assertEquals(null, StringUtils.join((Object[]) null)); // equivalent explicit cast\\n        // test additional varargs calls\\n        assertEquals(\"\", StringUtils.join()); // empty array\\n        assertEquals(\"\", StringUtils.join((Object) null)); // => new Object[]{null}\\n\\n        assertEquals(\"\", StringUtils.join(EMPTY_ARRAY_LIST));\\n        assertEquals(\"\", StringUtils.join(NULL_ARRAY_LIST));\\n        assertEquals(\"null\", StringUtils.join(NULL_TO_STRING_LIST));\\n        assertEquals(\"abc\", StringUtils.join(new String[] {\"a\", \"b\", \"c\"}));\\n        assertEquals(\"a\", StringUtils.join(new String[] {null, \"a\", \"\"}));\\n        assertEquals(\"foo\", StringUtils.join(MIXED_ARRAY_LIST));\\n        assertEquals(\"foo2\", StringUtils.join(MIXED_TYPE_LIST));\\n    }', exception_type='java.lang.NullPointerException', failure_message=None, support_source=None)"
]
```

</details>

---
## [1] ⚙️ test-context (H1/H2)
<details><summary>▸ output — test": "org.apache.commons.lang3.StringUtilsTest::testJoin_ArrayChar (~485 chars)</summary>

**output:** 
```json
[
  {
    "test": "org.apache.commons.lang3.StringUtilsTest::testJoin_ArrayChar",
    "failure_message": "--- org.apache.commons.lang3.StringUtilsTest::testJoin_ArrayChar\njava.lang.NullPointerException",
    "support_chars": 539
  },
  {
    "test": "org.apache.commons.lang3.StringUtilsTest::testJoin_Objectarray",
    "failure_message": "--- org.apache.commons.lang3.StringUtilsTest::testJoin_Objectarray\njava.lang.NullPointerException",
    "support_chars": 417
  }
]
```

</details>

---
## [2] ⚙️ analysis (TargetAnalyzer)
<details><summary>▸ output — modified_files": [ (~20,081 chars)</summary>

**output:** 
```json
{
  "modified_files": [
    "src/main/java/org/apache/commons/lang3/StringUtils.java"
  ],
  "patch_text": "--- /src/main/java/org/apache/commons/lang3/StringUtils.java\t\n+++ /src/main/java/org/apache/commons/lang3/StringUtils.java\t\n@@ -3295,8 +3295,7 @@\n             return EMPTY;\n         }\n         \n-        StringBuilder buf = new StringBuilder((array[startIndex] == null ? 16 : array[startIndex].toString().length()) + 1);\n-\n+        StringBuilder buf=new StringBuilder(256);\n         for (int i = startIndex; i < endIndex; i++) {\n             if (i > startIndex) {\n                 buf.append(separator);\n@@ -3380,7 +3379,7 @@\n             return EMPTY;\n         }\n \n-        StringBuilder buf = new StringBuilder((array[startIndex] == null ? 16 : array[startIndex].toString().length()) + separator.length());\n+        StringBuilder buf=new StringBuilder(4);\n \n         for (int i = startIndex; i < endIndex; i++) {\n             if (i > startIndex) {\n",
  "functions": [
    {
      "func_name": "join",
      "func_signature": "public static String join(Object[] array, char separator, int startIndex, int endIndex)",
      "func_source": "    public static String join(Object[] array, char separator, int startIndex, int endIndex) {\n        if (array == null) {\n            return null;\n        }\n        int noOfItems = (endIndex - startIndex);\n        if (noOfItems <= 0) {\n            return EMPTY;\n        }\n        \n        StringBuilder buf = new StringBuilder((array[startIndex] == null ? 16 : array[startIndex].toString().length()) + 1);\n\n        for (int i = startIndex; i < endIndex; i++) {\n            if (i > startIndex) {\n                buf.append(separator);\n            }\n            if (array[i] != null) {\n                buf.append(array[i]);\n            }\n        }\n        return buf.toString();\n    }",
      "func_class": "StringUtils",
      "func_class_fq": "org.apache.commons.lang3.StringUtils",
      "func_param_types": [
        "Object[]",
        "char",
        "int",
        "int"
      ],
      "fi_name": "[org.apache.commons.lang3.StringUtils].join(Object[],char,int,int)",
      "overload_types": [
        [
          "T"
        ],
        [
          "Object[]",
          "char"
        ],
        [
          "Object[]",
          "char",
          "int",
          "int"
        ],
        [
          "Object[]",
          "String"
        ],
        [
          "Object[]",
          "String",
          "int",
          "int"
        ],
        [
          "Iterator<?>",
          "char"
        ],
        [
          "Iterator<?>",
          "String"
        ],
        [
          "Iterable<?>",
          "char"
        ],
        [
          "Iterable<?>",
          "String"
        ]
      ],
      "xrefs": [
        "public void testJoin_ArrayChar() {\n        assertEquals(null, StringUtils.join((Object[]) null, ','));\n        assertEquals(TEXT_LIST_CHAR, StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR));\n        assertEquals(\"\", StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR));\n        assertEquals(\";;foo\", StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR));\n        assertEquals(\"foo;2\", StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR));\n\n        assertEquals(\"/\", StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length-1));\n        assertEquals(\"foo\", StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1));\n        assertEquals(\"null\", StringUtils.join(NULL_TO_STRING_LIST,'/', 0, 1));\n        assertEquals(\"foo/2\", StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2));\n        assertEquals(\"2\", StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2));\n        assertEquals(\"\", StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1));\n    }"
      ],
      "reachable": [
        "[StringBuilder].<init>(int)",
        "[StringBuilder].append(char)",
        "[StringBuilder].append()",
        "[StringBuilder].toString()",
        "[org.apache.commons.lang3.StringUtils].length(CharSequence)",
        "[org.apache.commons.lang3.reflect.ConstructorUtilsTest.TestBean].toString()"
      ],
      "related_callees": [
        {
          "name": "toString",
          "source_file": "AnnotationUtils.java",
          "signature": "public static String toString(Annotation a)",
          "source": "    public static String toString(final Annotation a) {\n        ToStringBuilder builder = new ToStringBuilder(a, TO_STRING_STYLE);\n        for (Method m : a.annotationType().getDeclaredMethods()) {\n            if (m.getParameterTypes().length > 0) {\n                continue; //wtf?\n            }\n            try {\n                builder.append(m.getName(), m.invoke(a));\n            } catch (RuntimeException ex) {\n                throw ex;\n            } catch (Exception ex) {\n                throw new RuntimeException(ex);\n            }\n        }\n        return builder.build();\n    }",
          "is_abstract": false,
          "impls": [
            [
              "AnnotationUtils.java",
              "    public static String toString(final Annotation a) {\n        ToStringBuilder builder = new ToStringBuilder(a, TO_STRING_STYLE);\n        for (Method m : a.annotationType().getDeclaredMethods()) {\n            if (m.getParameterTypes().length > 0) {\n                continue; //wtf?\n            }\n            try {\n                builder.append(m.getName(), m.invoke(a));\n            } catch (RuntimeException ex) {\n                throw ex;\n            } catch (Exception ex) {\n                throw new RuntimeException(ex);\n            }\n        }\n        return builder.build();\n    }"
            ],
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
            ]
          ]
        },
        {
          "name": "length",
          "source_file": "StringUtils.java",
          "signature": "public static int length(CharSequence cs)",
          "source": "    public static int length(CharSequence cs) {\n        return cs == null ? 0 : cs.length();\n    }",
          "is_abstract": false,
          "impls": []
        }
      ],
      "field_siblings": [
        {
          "name": "trimToEmpty",
          "signature": "public static String trimToEmpty(String str)",
          "shared_fields": [
            "EMPTY"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "<p>Removes control characters (char &lt;= 32) from both ends of this String returning an empty String (\"\") if the String is empty (\"\") after the trim or if it is {@code null}. <p>The String is trimmed using {@link String#trim()}. Trim removes start and end characters &lt;= 32. To strip whitespace use {@link #stripToEmp…"
        },
        {
          "name": "stripToEmpty",
          "signature": "public static String stripToEmpty(String str)",
          "shared_fields": [
            "EMPTY"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "<p>Strips whitespace from the start and end of a String  returning an empty String if {@code null} input.</p> <p>This is similar to {@link #trimToEmpty(String)} but removes whitespace. Whitespace is defined by {@link Character#isWhitespace(char)}.</p> <pre> StringUtils.stripToEmpty(null)     = \"\" StringUtils.stripToEmp…"
        },
        {
          "name": "substring",
          "signature": "public static String substring(String str, int start)",
          "shared_fields": [
            "EMPTY"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "<p>Gets a substring from the specified String avoiding exceptions.</p> <p>A negative start position can be used to start {@code n} characters from the end of the String.</p> <p>A {@code null} String will return {@code null}. An empty (\"\") String will return \"\".</p> <pre> StringUtils.substring(null, *)   = null StringUt…"
        },
        {
          "name": "substring",
          "signature": "public static String substring(String str, int start, int end)",
          "shared_fields": [
            "EMPTY"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "<p>Gets a substring from the specified String avoiding exceptions.</p> <p>A negative start position can be used to start/end {@code n} characters from the end of the String.</p> <p>The returned substring starts with the character in the {@code start} position and ends before the {@code end} position. All position count…"
        },
        {
          "name": "left",
          "signature": "public static String left(String str, int len)",
          "shared_fields": [
            "EMPTY"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "<p>Gets the leftmost {@code len} characters of a String.</p> <p>If {@code len} characters are not available, or the String is {@code null}, the String will be returned without an exception. An empty String is returned if len is negative.</p> <pre> StringUtils.left(null, *)    = null StringUtils.left(*, -ve)     = \"\" St…"
        }
      ]
    },
    {
      "func_name": "join",
      "func_signature": "public static String join(Object[] array, String separator, int startIndex, int endIndex)",
      "func_source": "    public static String join(Object[] array, String separator, int startIndex, int endIndex) {\n        if (array == null) {\n            return null;\n        }\n        if (separator == null) {\n            separator = EMPTY;\n        }\n\n        // endIndex - startIndex > 0:   Len = NofStrings *(len(firstString) + len(separator))\n        //           (Assuming that all Strings are roughly equally long)\n        int noOfItems = (endIndex - startIndex);\n        if (noOfItems <= 0) {\n            return EMPTY;\n        }\n\n        StringBuilder buf = new StringBuilder((array[startIndex] == null ? 16 : array[startIndex].toString().length()) + separator.length());\n\n        for (int i = startIndex; i < endIndex; i++) {\n            if (i > startIndex) {\n                buf.append(separator);\n            }\n            if (array[i] != null) {\n                buf.append(array[i]);\n            }\n        }\n        return buf.toString();\n    }",
      "func_class": "StringUtils",
      "func_class_fq": "org.apache.commons.lang3.StringUtils",
      "func_param_types": [
        "Object[]",
        "String",
        "int",
        "int"
      ],
      "fi_name": "[org.apache.commons.lang3.StringUtils].join(Object[],String,int,int)",
      "overload_types": [
        [
          "T"
        ],
        [
          "Object[]",
          "char"
        ],
        [
          "Object[]",
          "char",
          "int",
          "int"
        ],
        [
          "Object[]",
          "String"
        ],
        [
          "Object[]",
          "String",
          "int",
          "int"
        ],
        [
          "Iterator<?>",
          "char"
        ],
        [
          "Iterator<?>",
          "String"
        ],
        [
          "Iterable<?>",
          "char"
        ],
        [
          "Iterable<?>",
          "String"
        ]
      ],
      "xrefs": [
        "public void testJoin_ArrayString() {\n        assertEquals(null, StringUtils.join((Object[]) null, null));\n        assertEquals(TEXT_LIST_NOSEP, StringUtils.join(ARRAY_LIST, null));\n        assertEquals(TEXT_LIST_NOSEP, StringUtils.join(ARRAY_LIST, \"\"));\n        \n        assertEquals(\"\", StringUtils.join(NULL_ARRAY_LIST, null));\n        \n        assertEquals(\"\", StringUtils.join(EMPTY_ARRAY_LIST, null));\n        assertEquals(\"\", StringUtils.join(EMPTY_ARRAY_LIST, \"\"));\n        assertEquals(\"\", StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR));\n\n        assertEquals(TEXT_LIST, StringUtils.join(ARRAY_LIST, SEPARATOR));\n        assertEquals(\",,foo\", StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR));\n        assertEquals(\"foo,2\", StringUtils.join(MIXED_TYPE_LIST, SEPARATOR));\n\n        assertEquals(\"/\", StringUtils.join(MIXED_ARRAY_LIST, \"/\", 0, MIXED_ARRAY_LIST.length-1));\n        assertEquals(\"\", StringUtils.join(MIXED_ARRAY_LIST, \"\", 0, MIXED_ARRAY_LIST.length-1));\n        assertEquals(\"foo\", StringUtils.join(MIXED_TYPE_LIST, \"/\", 0, 1));\n        assertEquals(\"foo/2\", StringUtils.join(MIXED_TYPE_LIST, \"/\", 0, 2));\n        assertEquals(\"2\", StringUtils.join(MIXED_TYPE_LIST, \"/\", 1, 2));\n        assertEquals(\"\", StringUtils.join(MIXED_TYPE_LIST, \"/\", 2, 1));\n    }"
      ],
      "reachable": [
        "separator.length()",
        "[StringBuilder].<init>(org.apache.commons.lang3.StringUtils)",
        "[StringBuilder].append(org.apache.commons.lang3.StringUtils)",
        "[StringBuilder].append()",
        "[StringBuilder].toString()",
        "[org.apache.commons.lang3.StringUtils].length(CharSequence)",
        "[org.apache.commons.lang3.reflect.ConstructorUtilsTest.TestBean].toString()"
      ],
      "related_callees": [
        {
          "name": "toString",
          "source_file": "AnnotationUtils.java",
          "signature": "public static String toString(Annotation a)",
          "source": "    public static String toString(final Annotation a) {\n        ToStringBuilder builder = new ToStringBuilder(a, TO_STRING_STYLE);\n        for (Method m : a.annotationType().getDeclaredMethods()) {\n            if (m.getParameterTypes().length > 0) {\n                continue; //wtf?\n            }\n            try {\n                builder.append(m.getName(), m.invoke(a));\n            } catch (RuntimeException ex) {\n                throw ex;\n            } catch (Exception ex) {\n                throw new RuntimeException(ex);\n            }\n        }\n        return builder.build();\n    }",
          "is_abstract": false,
          "impls": [
            [
              "AnnotationUtils.java",
              "    public static String toString(final Annotation a) {\n        ToStringBuilder builder = new ToStringBuilder(a, TO_STRING_STYLE);\n        for (Method m : a.annotationType().getDeclaredMethods()) {\n            if (m.getParameterTypes().length > 0) {\n                continue; //wtf?\n            }\n            try {\n                builder.append(m.getName(), m.invoke(a));\n            } catch (RuntimeException ex) {\n                throw ex;\n            } catch (Exception ex) {\n                throw new RuntimeException(ex);\n            }\n        }\n        return builder.build();\n    }"
            ],
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
            ]
          ]
        },
        {
          "name": "length",
          "source_file": "StringUtils.java",
          "signature": "public static int length(CharSequence cs)",
          "source": "    public static int length(CharSequence cs) {\n        return cs == null ? 0 : cs.length();\n    }",
          "is_abstract": false,
          "impls": []
        }
      ],
      "field_siblings": [
        {
          "name": "trimToEmpty",
          "signature": "public static String trimToEmpty(String str)",
          "shared_fields": [
            "EMPTY"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "<p>Removes control characters (char &lt;= 32) from both ends of this String returning an empty String (\"\") if the String is empty (\"\") after the trim or if it is {@code null}. <p>The String is trimmed using {@link String#trim()}. Trim removes start and end characters &lt;= 32. To strip whitespace use {@link #stripToEmp…"
        },
        {
          "name": "stripToEmpty",
          "signature": "public static String stripToEmpty(String str)",
          "shared_fields": [
            "EMPTY"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "<p>Strips whitespace from the start and end of a String  returning an empty String if {@code null} input.</p> <p>This is similar to {@link #trimToEmpty(String)} but removes whitespace. Whitespace is defined by {@link Character#isWhitespace(char)}.</p> <pre> StringUtils.stripToEmpty(null)     = \"\" StringUtils.stripToEmp…"
        },
        {
          "name": "substring",
          "signature": "public static String substring(String str, int start)",
          "shared_fields": [
            "EMPTY"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "<p>Gets a substring from the specified String avoiding exceptions.</p> <p>A negative start position can be used to start {@code n} characters from the end of the String.</p> <p>A {@code null} String will return {@code null}. An empty (\"\") String will return \"\".</p> <pre> StringUtils.substring(null, *)   = null StringUt…"
        },
        {
          "name": "substring",
          "signature": "public static String substring(String str, int start, int end)",
          "shared_fields": [
            "EMPTY"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "<p>Gets a substring from the specified String avoiding exceptions.</p> <p>A negative start position can be used to start/end {@code n} characters from the end of the String.</p> <p>The returned substring starts with the character in the {@code start} position and ends before the {@code end} position. All position count…"
        },
        {
          "name": "left",
          "signature": "public static String left(String str, int len)",
          "shared_fields": [
            "EMPTY"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "<p>Gets the leftmost {@code len} characters of a String.</p> <p>If {@code len} characters are not available, or the String is {@code null}, the String will be returned without an exception. An empty String is returned if len is negative.</p> <pre> StringUtils.left(null, *)    = null StringUtils.left(*, -ve)     = \"\" St…"
        }
      ]
    }
  ],
  "package": "org.apache.commons.lang3",
  "root_cause_reachable": [
    "StringUtils.length",
    "TestBean.toString"
  ],
  "neighbourhood_notes": [],
  "source_imports": [
    "import java.lang.reflect.InvocationTargetException;",
    "import java.lang.reflect.Method;",
    "import java.util.ArrayList;",
    "import java.util.Arrays;",
    "import java.util.Iterator;",
    "import java.util.List;",
    "import java.util.Locale;",
    "import java.util.regex.Pattern;"
  ]
}
```

</details>

---
## [3] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~40,819 chars)</summary>

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
- Package: `org.apache.commons.lang3` (`package org.apache.commons.lang3;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Lang_20_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/lang3/StringUtils.java	
+++ /src/main/java/org/apache/commons/lang3/StringUtils.java	
@@ -3295,8 +3295,7 @@
             return EMPTY;
         }
         
-        StringBuilder buf = new StringBuilder((array[startIndex] == null ? 16 : array[startIndex].toString().length()) + 1);
-
+        StringBuilder buf=new StringBuilder(256);
         for (int i = startIndex; i < endIndex; i++) {
             if (i > startIndex) {
                 buf.append(separator);
@@ -3380,7 +3379,7 @@
             return EMPTY;
         }
 
-        StringBuilder buf = new StringBuilder((array[startIndex] == null ? 16 : array[startIndex].toString().length()) + separator.length());
+        StringBuilder buf=new StringBuilder(4);
 
         for (int i = startIndex; i < endIndex; i++) {
             if (i > startIndex) {

</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
</source_imports>

Function `join`:
<signature>
public static String join(Object[] array, char separator, int startIndex, int endIndex)
</signature>
<code>
    public static String join(Object[] array, char separator, int startIndex, int endIndex) {
        if (array == null) {
            return null;
        }
        int noOfItems = (endIndex - startIndex);
        if (noOfItems <= 0) {
            return EMPTY;
        }
        
        StringBuilder buf = new StringBuilder((array[startIndex] == null ? 16 : array[startIndex].toString().length()) + 1);

        for (int i = startIndex; i < endIndex; i++) {
            if (i > startIndex) {
                buf.append(separator);
            }
            if (array[i] != null) {
                buf.append(array[i]);
            }
        }
        return buf.toString();
    }
</code>
Call-site examples (use these as a guide for constructing the target call):
<xref>
public void testJoin_ArrayChar() {
        assertEquals(null, StringUtils.join((Object[]) null, ','));
        assertEquals(TEXT_LIST_CHAR, StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR));
        assertEquals("", StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR));
        assertEquals(";;foo", StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR));
        assertEquals("foo;2", StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR));

        assertEquals("/", StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length-1));
        assertEquals("foo", StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1));
        assertEquals("null", StringUtils.join(NULL_TO_STRING_LIST,'/', 0, 1));
        assertEquals("foo/2", StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2));
        assertEquals("2", StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2));
        assertEquals("", StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1));
    }
</xref>
Methods called by `join` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="toString" from="AnnotationUtils.java">
<signature>
public static String toString(Annotation a)
</signature>
<code>
    public static String toString(final Annotation a) {
        ToStringBuilder builder = new ToStringBuilder(a, TO_STRING_STYLE);
        for (Method m : a.annotationType().getDeclaredMethods()) {
            if (m.getParameterTypes().length > 0) {
                continue; //wtf?
            }
            try {
                builder.append(m.getName(), m.invoke(a));
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return builder.build();
    }
</code>
<implementation in="AnnotationUtils.java">
    public static String toString(final Annotation a) {
        ToStringBuilder builder = new ToStringBuilder(a, TO_STRING_STYLE);
        for (Method m : a.annotationType().getDeclaredMethods()) {
            if (m.getParameterTypes().length > 0) {
                continue; //wtf?
            }
            try {
                builder.append(m.getName(), m.invoke(a));
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return builder.build();
    }
</implementation>
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
</callee>
<callee name="length" from="StringUtils.java">
<signature>
public static int length(CharSequence cs)
</signature>
<code>
    public static int length(CharSequence cs) {
        return cs == null ? 0 : cs.length();
    }
</code>
</callee>
STATE COUPLING: these members of the same class share the listed field(s) with `join` but neither calls the other — state one of them writes is what the other reports. A defect the patch leaves (or introduces) in that shared state stays observable through these members even when `join`'s own output looks right, so a strong harness CHECKS THEY AGREE: compare what a reader reports against what the writer/constructor established.
  - `public static String trimToEmpty(String str)` (shared field(s): EMPTY)
      doc: <p>Removes control characters (char &lt;= 32) from both ends of this String returning an empty String ("") if the String is empty ("") after the trim or if it is {@code null}. <p>The String is trimmed using {@link String#trim()}. Trim removes start and end characters &lt;= 32. To strip whitespace use {@link #stripToEmp…
  - `public static String stripToEmpty(String str)` (shared field(s): EMPTY)
      doc: <p>Strips whitespace from the start and end of a String  returning an empty String if {@code null} input.</p> <p>This is similar to {@link #trimToEmpty(String)} but removes whitespace. Whitespace is defined by {@link Character#isWhitespace(char)}.</p> <pre> StringUtils.stripToEmpty(null)     = "" StringUtils.stripToEmp…
  - `public static String substring(String str, int start)` (shared field(s): EMPTY)
      doc: <p>Gets a substring from the specified String avoiding exceptions.</p> <p>A negative start position can be used to start {@code n} characters from the end of the String.</p> <p>A {@code null} String will return {@code null}. An empty ("") String will return "".</p> <pre> StringUtils.substring(null, *)   = null StringUt…
  - `public static String substring(String str, int start, int end)` (shared field(s): EMPTY)
      doc: <p>Gets a substring from the specified String avoiding exceptions.</p> <p>A negative start position can be used to start/end {@code n} characters from the end of the String.</p> <p>The returned substring starts with the character in the {@code start} position and ends before the {@code end} position. All position count…
  - `public static String left(String str, int len)` (shared field(s): EMPTY)
      doc: <p>Gets the leftmost {@code len} characters of a String.</p> <p>If {@code len} characters are not available, or the String is {@code null}, the String will be returned without an exception. An empty String is returned if len is negative.</p> <pre> StringUtils.left(null, *)    = null StringUtils.left(*, -ve)     = "" St…

Function `join`:
<signature>
public static String join(Object[] array, String separator, int startIndex, int endIndex)
</signature>
<code>
    public static String join(Object[] array, String separator, int startIndex, int endIndex) {
        if (array == null) {
            return null;
        }
        if (separator == null) {
            separator = EMPTY;
        }

        // endIndex - startIndex > 0:   Len = NofStrings *(len(firstString) + len(separator))
        //           (Assuming that all Strings are roughly equally long)
        int noOfItems = (endIndex - startIndex);
        if (noOfItems <= 0) {
            return EMPTY;
        }

        StringBuilder buf = new StringBuilder((array[startIndex] == null ? 16 : array[startIndex].toString().length()) + separator.length());

        for (int i = startIndex; i < endIndex; i++) {
            if (i > startIndex) {
                buf.append(separator);
            }
            if (array[i] != null) {
                buf.append(array[i]);
            }
        }
        return buf.toString();
    }
</code>
Call-site examples (use these as a guide for constructing the target call):
<xref>
public void testJoin_ArrayString() {
        assertEquals(null, StringUtils.join((Object[]) null, null));
        assertEquals(TEXT_LIST_NOSEP, StringUtils.join(ARRAY_LIST, null));
        assertEquals(TEXT_LIST_NOSEP, StringUtils.join(ARRAY_LIST, ""));
        
        assertEquals("", StringUtils.join(NULL_ARRAY_LIST, null));
        
        assertEquals("", StringUtils.join(EMPTY_ARRAY_LIST, null));
        assertEquals("", StringUtils.join(EMPTY_ARRAY_LIST, ""));
        assertEquals("", StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR));

        assertEquals(TEXT_LIST, StringUtils.join(ARRAY_LIST, SEPARATOR));
        assertEquals(",,foo", StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR));
        assertEquals("foo,2", StringUtils.join(MIXED_TYPE_LIST, SEPARATOR));

        assertEquals("/", StringUtils.join(MIXED_ARRAY_LIST, "/", 0, MIXED_ARRAY_LIST.length-1));
        assertEquals("", StringUtils.join(MIXED_ARRAY_LIST, "", 0, MIXED_ARRAY_LIST.length-1));
        assertEquals("foo", StringUtils.join(MIXED_TYPE_LIST, "/", 0, 1));
        assertEquals("foo/2", StringUtils.join(MIXED_TYPE_LIST, "/", 0, 2));
        assertEquals("2", StringUtils.join(MIXED_TYPE_LIST, "/", 1, 2));
        assertEquals("", StringUtils.join(MIXED_TYPE_LIST, "/", 2, 1));
    }
</xref>
Methods called by `join` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="toString" from="AnnotationUtils.java">
<signature>
public static String toString(Annotation a)
</signature>
<code>
    public static String toString(final Annotation a) {
        ToStringBuilder builder = new ToStringBuilder(a, TO_STRING_STYLE);
        for (Method m : a.annotationType().getDeclaredMethods()) {
            if (m.getParameterTypes().length > 0) {
                continue; //wtf?
            }
            try {
                builder.append(m.getName(), m.invoke(a));
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return builder.build();
    }
</code>
<implementation in="AnnotationUtils.java">
    public static String toString(final Annotation a) {
        ToStringBuilder builder = new ToStringBuilder(a, TO_STRING_STYLE);
        for (Method m : a.annotationType().getDeclaredMethods()) {
            if (m.getParameterTypes().length > 0) {
                continue; //wtf?
            }
            try {
                builder.append(m.getName(), m.invoke(a));
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return builder.build();
    }
</implementation>
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
</callee>
<callee name="length" from="StringUtils.java">
<signature>
public static int length(CharSequence cs)
</signature>
<code>
    public static int length(CharSequence cs) {
        return cs == null ? 0 : cs.length();
    }
</code>
</callee>
STATE COUPLING: these members of the same class share the listed field(s) with `join` but neither calls the other — state one of them writes is what the other reports. A defect the patch leaves (or introduces) in that shared state stays observable through these members even when `join`'s own output looks right, so a strong harness CHECKS THEY AGREE: compare what a reader reports against what the writer/constructor established.
  - `public static String trimToEmpty(String str)` (shared field(s): EMPTY)
      doc: <p>Removes control characters (char &lt;= 32) from both ends of this String returning an empty String ("") if the String is empty ("") after the trim or if it is {@code null}. <p>The String is trimmed using {@link String#trim()}. Trim removes start and end characters &lt;= 32. To strip whitespace use {@link #stripToEmp…
  - `public static String stripToEmpty(String str)` (shared field(s): EMPTY)
      doc: <p>Strips whitespace from the start and end of a String  returning an empty String if {@code null} input.</p> <p>This is similar to {@link #trimToEmpty(String)} but removes whitespace. Whitespace is defined by {@link Character#isWhitespace(char)}.</p> <pre> StringUtils.stripToEmpty(null)     = "" StringUtils.stripToEmp…
  - `public static String substring(String str, int start)` (shared field(s): EMPTY)
      doc: <p>Gets a substring from the specified String avoiding exceptions.</p> <p>A negative start position can be used to start {@code n} characters from the end of the String.</p> <p>A {@code null} String will return {@code null}. An empty ("") String will return "".</p> <pre> StringUtils.substring(null, *)   = null StringUt…
  - `public static String substring(String str, int start, int end)` (shared field(s): EMPTY)
      doc: <p>Gets a substring from the specified String avoiding exceptions.</p> <p>A negative start position can be used to start/end {@code n} characters from the end of the String.</p> <p>The returned substring starts with the character in the {@code start} position and ends before the {@code end} position. All position count…
  - `public static String left(String str, int len)` (shared field(s): EMPTY)
      doc: <p>Gets the leftmost {@code len} characters of a String.</p> <p>If {@code len} characters are not available, or the String is {@code null}, the String will be returned without an exception. An empty String is returned if len is negative.</p> <pre> StringUtils.left(null, *)    = null StringUtils.left(*, -ve)     = "" St…

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `StringUtils`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.NullPointerException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `join`, `join` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: java.lang.NullPointerException
thrown_at: org.apache.commons.lang3.StringUtils.join(StringUtils.java:3383)
</ground_truth_crash>
Trigger lines: null passed as an array element. Mirror these calls:
<key_calls class="org.apache.commons.lang3.StringUtilsTest" method="testJoin_ArrayChar">
assertEquals(null, StringUtils.join((Object[]) null, ','));
assertEquals("null", StringUtils.join(NULL_TO_STRING_LIST,'/', 0, 1));
</key_calls>
<failing_test class="org.apache.commons.lang3.StringUtilsTest" method="testJoin_ArrayChar">
    public void testJoin_ArrayChar() {
        assertEquals(null, StringUtils.join((Object[]) null, ','));
        assertEquals(TEXT_LIST_CHAR, StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR));
        assertEquals("", StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR));
        assertEquals(";;foo", StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR));
        assertEquals("foo;2", StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR));

        assertEquals("/", StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length-1));
        assertEquals("foo", StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1));
        assertEquals("null", StringUtils.join(NULL_TO_STRING_LIST,'/', 0, 1));
        assertEquals("foo/2", StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2));
        assertEquals("2", StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2));
        assertEquals("", StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1));
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.lang3.StringUtilsTest::testJoin_ArrayChar
java.lang.NullPointerException
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.
The test method depends on the following from its test class — setUp, helper methods, constants, fixture files. Replicate this environment EXACTLY (register the same files, wire the same providers, use the same constants); do NOT improvise setup the test performs differently, and DROP any check whose setup you cannot replicate:
<test_support class="org.apache.commons.lang3.StringUtilsTest">
// --- class fields/constants the test uses ---
private static final String[] ARRAY_LIST = { "foo", "bar", "baz" };
private static final String[] EMPTY_ARRAY_LIST = {};
private static final Object[] NULL_TO_STRING_LIST = { new Object(){ @Override public String toString() { return null;
private static final String[] MIXED_ARRAY_LIST = {null, "", "foo"};
private static final Object[] MIXED_TYPE_LIST = {"foo", Long.valueOf(2L)};
private static final char   SEPARATOR_CHAR = ';';
private static final String TEXT_LIST_CHAR = "foo;bar;baz";
</test_support>
Trigger lines: null passed as an array element. Mirror these calls:
<key_calls class="org.apache.commons.lang3.StringUtilsTest" method="testJoin_Objectarray">
//        assertEquals(null, StringUtils.join(null)); // generates warning
assertEquals(null, StringUtils.join((Object[]) null)); // equivalent explicit cast
assertEquals("", StringUtils.join((Object) null)); // => new Object[]{null}
assertEquals("null", StringUtils.join(NULL_TO_STRING_LIST));
assertEquals("a", StringUtils.join(new String[] {null, "a", ""}));
</key_calls>
<failing_test class="org.apache.commons.lang3.StringUtilsTest" method="testJoin_Objectarray">
    public void testJoin_Objectarray() {
//        assertEquals(null, StringUtils.join(null)); // generates warning
        assertEquals(null, StringUtils.join((Object[]) null)); // equivalent explicit cast
        // test additional varargs calls
        assertEquals("", StringUtils.join()); // empty array
        assertEquals("", StringUtils.join((Object) null)); // => new Object[]{null}

        assertEquals("", StringUtils.join(EMPTY_ARRAY_LIST));
        assertEquals("", StringUtils.join(NULL_ARRAY_LIST));
        assertEquals("null", StringUtils.join(NULL_TO_STRING_LIST));
        assertEquals("abc", StringUtils.join(new String[] {"a", "b", "c"}));
        assertEquals("a", StringUtils.join(new String[] {null, "a", ""}));
        assertEquals("foo", StringUtils.join(MIXED_ARRAY_LIST));
        assertEquals("foo2", StringUtils.join(MIXED_TYPE_LIST));
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.lang3.StringUtilsTest::testJoin_Objectarray
java.lang.NullPointerException
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.
The test method depends on the following from its test class — setUp, helper methods, constants, fixture files. Replicate this environment EXACTLY (register the same files, wire the same providers, use the same constants); do NOT improvise setup the test performs differently, and DROP any check whose setup you cannot replicate:
<test_support class="org.apache.commons.lang3.StringUtilsTest">
// --- class fields/constants the test uses ---
private static final String[] EMPTY_ARRAY_LIST = {};
private static final String[] NULL_ARRAY_LIST = {null};
private static final Object[] NULL_TO_STRING_LIST = { new Object(){ @Override public String toString() { return null;
private static final String[] MIXED_ARRAY_LIST = {null, "", "foo"};
private static final Object[] MIXED_TYPE_LIST = {"foo", Long.valueOf(2L)};
</test_support>

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  abbreviate(String str, int maxWidth) / (String str, int offset, int maxWidth)
  center(String str, int size) / (String str, int size, char padChar) / (String str, int size, String padStr)
  chomp(String str) / (String str, String separator)
  contains(CharSequence seq, int searchChar) / (CharSequence seq, CharSequence searchSeq)
  containsAny(CharSequence cs, char... searchChars) / (CharSequence cs, CharSequence searchChars)
  containsNone(CharSequence cs, char... searchChars) / (CharSequence cs, String invalidChars)
  containsOnly(CharSequence cs, char... valid) / (CharSequence cs, String validChars)
  defaultString(String str) / (String str, String defaultStr)

METHOD FAMILIES over the same input space (factory/parser siblings — where the docs state a selection or agreement rule between family members, equivalent inputs must respect it):
  contains* family: containsAny, containsIgnoreCase, containsNone, containsOnly, containsWhitespace
  default* family: defaultIfBlank, defaultIfEmpty, defaultString
  ends* family: endsWith, endsWithAny, endsWithIgnoreCase
  index* family: indexOf, indexOfAny, indexOfAnyBut, indexOfDifference, indexOfIgnoreCase
  last* family: lastIndexOf, lastIndexOfAny, lastIndexOfIgnoreCase, lastOrdinalIndexOf
  remove* family:

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- StringUtils.length
- TestBean.toString
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
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~13,143 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final String[] ARRAY_LIST = { "foo", "bar", "baz" };
    private static final String[] EMPTY_ARRAY_LIST = {};
    private static final String[] NULL_ARRAY_LIST = { null };
    private static final String[] MIXED_ARRAY_LIST = { null, "", "foo" };
    private static final Object[] MIXED_TYPE_LIST = { "foo", Long.valueOf(2L) };
    private static final char SEPARATOR_CHAR = ';';
    private static final String TEXT_LIST_CHAR = "foo;bar;baz";
    private static final Object NULL_TO_STRING = new Object() {
        @Override
        public String toString() {
            return null;
        }
    };
    private static final Object[] NULL_TO_STRING_LIST = { NULL_TO_STRING };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();

        int mode = data.consumeInt(0, 5);
        switch (mode) {
            case 0:
                exploreNullToStringAtStart(data);
                break;
            case 1:
                exploreEquivalentOverloads(data);
                break;
            case 2:
                exploreFixtureVariants(data);
                break;
            case 3:
                exploreRangeAndEmptyState(data);
                break;
            case 4:
                exploreStringSeparatorNullAndEmpty(data);
                break;
            default:
                exploreSingleElementProperty(data);
                break;
        }
    }

    private static void anchor() {
        String nullArray = StringUtils.join((Object[]) null, SEPARATOR_CHAR);
        if (nullArray != null) {
            throw new RuntimeException("[oracle:anchor-null-array] metamorphic violation: join((Object[])null,char) must return null lhs=" + nullArray);
        }

        String exactFixture = StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR);
        if (!TEXT_LIST_CHAR.equals(exactFixture)) {
            throw new RuntimeException("[oracle:anchor-fixture] metamorphic violation: fixture mismatch lhs=" + exactFixture + " rhs=" + TEXT_LIST_CHAR);
        }

        try {
            String r1 = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            if (!"null".equals(r1)) {
                throw new RuntimeException("[oracle:anchor-char] metamorphic violation: expected \"null\" for documented fixture lhs=" + r1);
            }
        } catch (RuntimeException t) {
            handleThrowable(t);
        }

        try {
            String r2 = StringUtils.join(NULL_TO_STRING_LIST);
            if (!"null".equals(r2)) {
                throw new RuntimeException("[oracle:anchor-object] metamorphic violation: expected \"null\" for documented fixture lhs=" + r2);
            }
        } catch (RuntimeException t) {
            handleThrowable(t);
        }

        try {
            String r3 = StringUtils.join(NULL_TO_STRING_LIST, "", 0, 1);
            if (!"null".equals(r3)) {
                throw new RuntimeException("[oracle:anchor-string] metamorphic violation: expected \"null\" for documented fixture lhs=" + r3);
            }
        } catch (RuntimeException t) {
            handleThrowable(t);
        }
    }

    private static void exploreNullToStringAtStart(FuzzedDataProvider data) {
        int len = data.consumeInt(1, 6);
        Object[] array = new Object[len];
        for (int i = 0; i < len; i++) {
            array[i] = generateOrdinaryElement(data);
        }

        int start = data.consumeInt(0, len - 1);
        int end = data.consumeInt(start + 1, len);
        array[start] = NULL_TO_STRING;

        char sep = (char) data.consumeInt(0, 127);
        String sepStr = data.consumeBoolean() ? String.valueOf(sep) : data.consumeString(4);

        try {
            String charJoin = StringUtils.join(array, sep, start, end);
            String stringJoin = StringUtils.join(array, sepStr, start, end);

            if (sepStr != null && sepStr.length() == 1 && sepStr.charAt(0) == sep && !safeEquals(charJoin, stringJoin)) {
                throw new RuntimeException("[oracle:char-vs-string] metamorphic violation: equivalent separator overloads disagree lhs=" + charJoin + " rhs=" + stringJoin);
            }

            /* Contract used: join(Object[]) is the no-separator sibling; tests show join(NULL_TO_STRING_LIST) == "null".
               A throw-deleting patch that silently skips the element would break this equality against the explicit empty-separator form. */
            String full1 = StringUtils.join(array);
            String full2 = StringUtils.join(array, "", 0, array.length);
            if (!safeEquals(full1, full2)) {
                throw new RuntimeException("[oracle:no-sep-sibling] metamorphic violation: join(array) != join(array,\"\",0,len) lhs=" + full1 + " rhs=" + full2);
            }
        } catch (RuntimeException t) {
            handleThrowable(t);
        }
    }

    private static void exploreEquivalentOverloads(FuzzedDataProvider data) {
        Object[] array = buildArray(data, data.consumeInt(0, 6));
        char sep = (char) data.consumeInt(0, 127);
        int len = array.length;
        int start = len == 0 ? 0 : data.consumeInt(0, len);
        int end = len == 0 ? 0 : data.consumeInt(start, len);

        try {
            String a = StringUtils.join(array, sep, start, end);
            String b = StringUtils.join(array, String.valueOf(sep), start, end);
            if (!safeEquals(a, b)) {
                throw new RuntimeException("[oracle:overload-agree] metamorphic violation: char and single-char String overloads disagree lhs=" + a + " rhs=" + b);
            }

            String full1 = StringUtils.join(array);
            String full2 = StringUtils.join(array, "", 0, len);
            if (!safeEquals(full1, full2)) {
                throw new RuntimeException("[oracle:object-vs-range] metamorphic violation: no-separator siblings disagree lhs=" + full1 + " rhs=" + full2);
            }
        } catch (RuntimeException t) {
            handleThrowable(t);
        }
    }

    private static void exploreFixtureVariants(FuzzedDataProvider data) {
        Object[][] fixtures = new Object[][] {
            ARRAY_LIST,
            EMPTY_ARRAY_LIST,
            NULL_ARRAY_LIST,
            NULL_TO_STRING_LIST,
            MIXED_ARRAY_LIST,
            MIXED_TYPE_LIST,
            new Object[] { null, "a", "" }
        };
        Object[] array = fixtures[data.consumeInt(0, fixtures.length - 1)];
        char sep = data.consumeBoolean() ? '/' : SEPARATOR_CHAR;
        String sepStr = data.consumeBoolean() ? String.valueOf(sep) : (data.consumeBoolean() ? "" : null);
        int len = array.length;
        int start = len == 0 ? 0 : data.consumeInt(0, len);
        int end = len == 0 ? 0 : data.consumeInt(start, len);

        try {
            String a = StringUtils.join(array, sep, start, end);
            String b = StringUtils.join(array, String.valueOf(sep), start, end);
            if (!safeEquals(a, b)) {
                throw new RuntimeException("[oracle:fixture-overload] metamorphic violation: equivalent overloads disagree lhs=" + a + " rhs=" + b);
            }

            String c = StringUtils.join(array, sepStr, start, end);
            if (sepStr == null) {
                String d = StringUtils.join(array, "", start, end);
                if (!safeEquals(c, d)) {
                    throw new RuntimeException("[oracle:null-sep] metamorphic violation: null separator must behave as EMPTY lhs=" + c + " rhs=" + d);
                }
            }
        } catch (RuntimeException t) {
            handleThrowable(t);
        }
    }

    private static void exploreRangeAndEmptyState(FuzzedDataProvider data) {
        Object[] array = buildArray(data, data.consumeInt(0, 6));
        int len = array.length;
        int point = len == 0 ? 0 : data.consumeInt(0, len);
        char sep = (char) data.consumeInt(0, 127);

        try {
            String emptyRange = StringUtils.join(array, sep, point, point);
            /* Contract used: if noOfItems <= 0 join returns EMPTY; trimToEmpty(null), stripToEmpty(null),
               substring("",0), substring("",0,0), and left("x",-1) are documented to return EMPTY too. */
            if (!safeEquals(emptyRange, StringUtils.trimToEmpty(null))
                    || !safeEquals(emptyRange, StringUtils.stripToEmpty(null))
                    || !safeEquals(emptyRange, StringUtils.substring("", 0))
                    || !safeEquals(emptyRange, StringUtils.substring("", 0, 0))
                    || !safeEquals(emptyRange, StringUtils.left("x", -1))) {
                throw new RuntimeException("[oracle:empty-shared] metamorphic violation: EMPTY readers disagree lhs=" + emptyRange);
            }
        } catch (RuntimeException t) {
            handleThrowable(t);
        }
    }

    private static void exploreStringSeparatorNullAndEmpty(FuzzedDataProvider data) {
        Object[] array = buildArray(data, data.consumeInt(0, 6));
        int len = array.length;
        int start = len == 0 ? 0 : data.consumeInt(0, len);
        int end = len == 0 ? 0 : data.consumeInt(start, len);

        try {
            String a = StringUtils.join(array, (String) null, start, end);
            String b = StringUtils.join(array, "", start, end);
            if (!safeEquals(a, b)) {
                throw new RuntimeException("[oracle:null-empty-sep] metamorphic violation: null separator should equal EMPTY lhs=" + a + " rhs=" + b);
            }
        } catch (RuntimeException t) {
            handleThrowable(t);
        }
    }

    private static void exploreSingleElementProperty(FuzzedDataProvider data) {
        Object element = data.consumeBoolean() ? NULL_TO_STRING : generateOrdinaryElement(data);
        Object[] array = new Object[] { element, generateOrdinaryElement(data), generateOrdinaryElement(data) };
        int start = data.consumeInt(0, 2);
        int end = start + 1;
        char sep = (char) data.consumeInt(0, 127);

        try {
            String range = StringUtils.join(array, sep, start, end);
            String noSepRange = StringUtils.join(array, "", start, end);
            if (!safeEquals(range, noSepRange)) {
                throw new RuntimeException("[oracle:single-item] metamorphic violation: one-item range must ignore separator lhs=" + range + " rhs=" + noSepRange);
            }
        } catch (RuntimeException t) {
            handleThrowable(t);
        }
    }

    private static Object[] buildArray(FuzzedDataProvider data, int len) {
        Object[] array = new Object[len];
        for (int i = 0; i < len; i++) {
            if (data.consumeInt(0, 9) == 0) {
                array[i] = NULL_TO_STRING;
            } else {
                array[i] = generateOrdinaryElement(data);
            }
        }
        return array;
    }

    private static Object generateOrdinaryElement(FuzzedDataProvider data) {
        switch (data.consumeInt(0, 4)) {
            case 0:
                return null;
            case 1:
                return data.consumeAsciiString(8);
            case 2:
                return Integer.valueOf(data.consumeInt(-1000, 1000));
            case 3:
                return Long.valueOf(data.consumeInt(-1000, 1000));
            default:
                return data.consumeString(8);
        }
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static void handleThrowable(RuntimeException t) {
        if (isCleanRejection(t)) {
            return;
        }
        if (isRootCause(t)) {
            throw t;
        }
        throw t;
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof IllegalArgumentException || c instanceof NumberFormatException) {
                return true;
            }
            String name = c.getClass().getName();
            String lower = name.toLowerCase();
            if (lower.contains("invalid") || lower.contains("malformed") || lower.contains("format")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName())
                    && "join".equals(ste.getMethodName())) {
                return true;
            }
            if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName())
                    && "length".equals(ste.getMethodName())) {
                return true;
            }
            if ("org.apache.commons.lang3.TestBean".equals(ste.getClassName())
                    && "toString".equals(ste.getMethodName())) {
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
- trigger: java.lang.NullPointerException

---
## [5] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~42,230 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.apache.commons.lang3` (`package org.apache.commons.lang3;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Lang_20_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/lang3/StringUtils.java	
+++ /src/main/java/org/apache/commons/lang3/StringUtils.java	
@@ -3295,8 +3295,7 @@
             return EMPTY;
         }
         
-        StringBuilder buf = new StringBuilder((array[startIndex] == null ? 16 : array[startIndex].toString().length()) + 1);
-
+        StringBuilder buf=new StringBuilder(256);
         for (int i = startIndex; i < endIndex; i++) {
             if (i > startIndex) {
                 buf.append(separator);
@@ -3380,7 +3379,7 @@
             return EMPTY;
         }
 
-        StringBuilder buf = new StringBuilder((array[startIndex] == null ? 16 : array[startIndex].toString().length()) + separator.length());
+        StringBuilder buf=new StringBuilder(4);
 
         for (int i = startIndex; i < endIndex; i++) {
             if (i > startIndex) {

</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
</source_imports>

Function `join`:
<signature>
public static String join(Object[] array, char separator, int startIndex, int endIndex)
</signature>
<code>
    public static String join(Object[] array, char separator, int startIndex, int endIndex) {
        if (array == null) {
            return null;
        }
        int noOfItems = (endIndex - startIndex);
        if (noOfItems <= 0) {
            return EMPTY;
        }
        
        StringBuilder buf = new StringBuilder((array[startIndex] == null ? 16 : array[startIndex].toString().length()) + 1);

        for (int i = startIndex; i < endIndex; i++) {
            if (i > startIndex) {
                buf.append(separator);
            }
            if (array[i] != null) {
                buf.append(array[i]);
            }
        }
        return buf.toString();
    }
</code>
Call-site examples (use these as a guide for constructing the target call):
<xref>
public void testJoin_ArrayChar() {
        assertEquals(null, StringUtils.join((Object[]) null, ','));
        assertEquals(TEXT_LIST_CHAR, StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR));
        assertEquals("", StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR));
        assertEquals(";;foo", StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR));
        assertEquals("foo;2", StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR));

        assertEquals("/", StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length-1));
        assertEquals("foo", StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1));
        assertEquals("null", StringUtils.join(NULL_TO_STRING_LIST,'/', 0, 1));
        assertEquals("foo/2", StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2));
        assertEquals("2", StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2));
        assertEquals("", StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1));
    }
</xref>
Methods called by `join` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="toString" from="AnnotationUtils.java">
<signature>
public static String toString(Annotation a)
</signature>
<code>
    public static String toString(final Annotation a) {
        ToStringBuilder builder = new ToStringBuilder(a, TO_STRING_STYLE);
        for (Method m : a.annotationType().getDeclaredMethods()) {
            if (m.getParameterTypes().length > 0) {
                continue; //wtf?
            }
            try {
                builder.append(m.getName(), m.invoke(a));
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return builder.build();
    }
</code>
<implementation in="AnnotationUtils.java">
    public static String toString(final Annotation a) {
        ToStringBuilder builder = new ToStringBuilder(a, TO_STRING_STYLE);
        for (Method m : a.annotationType().getDeclaredMethods()) {
            if (m.getParameterTypes().length > 0) {
                continue; //wtf?
            }
            try {
                builder.append(m.getName(), m.invoke(a));
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return builder.build();
    }
</implementation>
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
</callee>
<callee name="length" from="StringUtils.java">
<signature>
public static int length(CharSequence cs)
</signature>
<code>
    public static int length(CharSequence cs) {
        return cs == null ? 0 : cs.length();
    }
</code>
</callee>
STATE COUPLING: these members of the same class share the listed field(s) with `join` but neither calls the other — state one of them writes is what the other reports. A defect the patch leaves (or introduces) in that shared state stays observable through these members even when `join`'s own output looks right, so a strong harness CHECKS THEY AGREE: compare what a reader reports against what the writer/constructor established.
  - `public static String trimToEmpty(String str)` (shared field(s): EMPTY)
      doc: <p>Removes control characters (char &lt;= 32) from both ends of this String returning an empty String ("") if the String is empty ("") after the trim or if it is {@code null}. <p>The String is trimmed using {@link String#trim()}. Trim removes start and end characters &lt;= 32. To strip whitespace use {@link #stripToEmp…
  - `public static String stripToEmpty(String str)` (shared field(s): EMPTY)
      doc: <p>Strips whitespace from the start and end of a String  returning an empty String if {@code null} input.</p> <p>This is similar to {@link #trimToEmpty(String)} but removes whitespace. Whitespace is defined by {@link Character#isWhitespace(char)}.</p> <pre> StringUtils.stripToEmpty(null)     = "" StringUtils.stripToEmp…
  - `public static String substring(String str, int start)` (shared field(s): EMPTY)
      doc: <p>Gets a substring from the specified String avoiding exceptions.</p> <p>A negative start position can be used to start {@code n} characters from the end of the String.</p> <p>A {@code null} String will return {@code null}. An empty ("") String will return "".</p> <pre> StringUtils.substring(null, *)   = null StringUt…
  - `public static String substring(String str, int start, int end)` (shared field(s): EMPTY)
      doc: <p>Gets a substring from the specified String avoiding exceptions.</p> <p>A negative start position can be used to start/end {@code n} characters from the end of the String.</p> <p>The returned substring starts with the character in the {@code start} position and ends before the {@code end} position. All position count…
  - `public static String left(String str, int len)` (shared field(s): EMPTY)
      doc: <p>Gets the leftmost {@code len} characters of a String.</p> <p>If {@code len} characters are not available, or the String is {@code null}, the String will be returned without an exception. An empty String is returned if len is negative.</p> <pre> StringUtils.left(null, *)    = null StringUtils.left(*, -ve)     = "" St…

Function `join`:
<signature>
public static String join(Object[] array, String separator, int startIndex, int endIndex)
</signature>
<code>
    public static String join(Object[] array, String separator, int startIndex, int endIndex) {
        if (array == null) {
            return null;
        }
        if (separator == null) {
            separator = EMPTY;
        }

        // endIndex - startIndex > 0:   Len = NofStrings *(len(firstString) + len(separator))
        //           (Assuming that all Strings are roughly equally long)
        int noOfItems = (endIndex - startIndex);
        if (noOfItems <= 0) {
            return EMPTY;
        }

        StringBuilder buf = new StringBuilder((array[startIndex] == null ? 16 : array[startIndex].toString().length()) + separator.length());

        for (int i = startIndex; i < endIndex; i++) {
            if (i > startIndex) {
                buf.append(separator);
            }
            if (array[i] != null) {
                buf.append(array[i]);
            }
        }
        return buf.toString();
    }
</code>
Call-site examples (use these as a guide for constructing the target call):
<xref>
public void testJoin_ArrayString() {
        assertEquals(null, StringUtils.join((Object[]) null, null));
        assertEquals(TEXT_LIST_NOSEP, StringUtils.join(ARRAY_LIST, null));
        assertEquals(TEXT_LIST_NOSEP, StringUtils.join(ARRAY_LIST, ""));
        
        assertEquals("", StringUtils.join(NULL_ARRAY_LIST, null));
        
        assertEquals("", StringUtils.join(EMPTY_ARRAY_LIST, null));
        assertEquals("", StringUtils.join(EMPTY_ARRAY_LIST, ""));
        assertEquals("", StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR));

        assertEquals(TEXT_LIST, StringUtils.join(ARRAY_LIST, SEPARATOR));
        assertEquals(",,foo", StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR));
        assertEquals("foo,2", StringUtils.join(MIXED_TYPE_LIST, SEPARATOR));

        assertEquals("/", StringUtils.join(MIXED_ARRAY_LIST, "/", 0, MIXED_ARRAY_LIST.length-1));
        assertEquals("", StringUtils.join(MIXED_ARRAY_LIST, "", 0, MIXED_ARRAY_LIST.length-1));
        assertEquals("foo", StringUtils.join(MIXED_TYPE_LIST, "/", 0, 1));
        assertEquals("foo/2", StringUtils.join(MIXED_TYPE_LIST, "/", 0, 2));
        assertEquals("2", StringUtils.join(MIXED_TYPE_LIST, "/", 1, 2));
        assertEquals("", StringUtils.join(MIXED_TYPE_LIST, "/", 2, 1));
    }
</xref>
Methods called by `join` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="toString" from="AnnotationUtils.java">
<signature>
public static String toString(Annotation a)
</signature>
<code>
    public static String toString(final Annotation a) {
        ToStringBuilder builder = new ToStringBuilder(a, TO_STRING_STYLE);
        for (Method m : a.annotationType().getDeclaredMethods()) {
            if (m.getParameterTypes().length > 0) {
                continue; //wtf?
            }
            try {
                builder.append(m.getName(), m.invoke(a));
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return builder.build();
    }
</code>
<implementation in="AnnotationUtils.java">
    public static String toString(final Annotation a) {
        ToStringBuilder builder = new ToStringBuilder(a, TO_STRING_STYLE);
        for (Method m : a.annotationType().getDeclaredMethods()) {
            if (m.getParameterTypes().length > 0) {
                continue; //wtf?
            }
            try {
                builder.append(m.getName(), m.invoke(a));
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return builder.build();
    }
</implementation>
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
</callee>
<callee name="length" from="StringUtils.java">
<signature>
public static int length(CharSequence cs)
</signature>
<code>
    public static int length(CharSequence cs) {
        return cs == null ? 0 : cs.length();
    }
</code>
</callee>
STATE COUPLING: these members of the same class share the listed field(s) with `join` but neither calls the other — state one of them writes is what the other reports. A defect the patch leaves (or introduces) in that shared state stays observable through these members even when `join`'s own output looks right, so a strong harness CHECKS THEY AGREE: compare what a reader reports against what the writer/constructor established.
  - `public static String trimToEmpty(String str)` (shared field(s): EMPTY)
      doc: <p>Removes control characters (char &lt;= 32) from both ends of this String returning an empty String ("") if the String is empty ("") after the trim or if it is {@code null}. <p>The String is trimmed using {@link String#trim()}. Trim removes start and end characters &lt;= 32. To strip whitespace use {@link #stripToEmp…
  - `public static String stripToEmpty(String str)` (shared field(s): EMPTY)
      doc: <p>Strips whitespace from the start and end of a String  returning an empty String if {@code null} input.</p> <p>This is similar to {@link #trimToEmpty(String)} but removes whitespace. Whitespace is defined by {@link Character#isWhitespace(char)}.</p> <pre> StringUtils.stripToEmpty(null)     = "" StringUtils.stripToEmp…
  - `public static String substring(String str, int start)` (shared field(s): EMPTY)
      doc: <p>Gets a substring from the specified String avoiding exceptions.</p> <p>A negative start position can be used to start {@code n} characters from the end of the String.</p> <p>A {@code null} String will return {@code null}. An empty ("") String will return "".</p> <pre> StringUtils.substring(null, *)   = null StringUt…
  - `public static String substring(String str, int start, int end)` (shared field(s): EMPTY)
      doc: <p>Gets a substring from the specified String avoiding exceptions.</p> <p>A negative start position can be used to start/end {@code n} characters from the end of the String.</p> <p>The returned substring starts with the character in the {@code start} position and ends before the {@code end} position. All position count…
  - `public static String left(String str, int len)` (shared field(s): EMPTY)
      doc: <p>Gets the leftmost {@code len} characters of a String.</p> <p>If {@code len} characters are not available, or the String is {@code null}, the String will be returned without an exception. An empty String is returned if len is negative.</p> <pre> StringUtils.left(null, *)    = null StringUtils.left(*, -ve)     = "" St…

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `StringUtils`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.NullPointerException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `join`, `join` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: java.lang.NullPointerException
thrown_at: org.apache.commons.lang3.StringUtils.join(StringUtils.java:3383)
</ground_truth_crash>
Trigger lines: null passed as an array element. Mirror these calls:
<key_calls class="org.apache.commons.lang3.StringUtilsTest" method="testJoin_ArrayChar">
assertEquals(null, StringUtils.join((Object[]) null, ','));
assertEquals("null", StringUtils.join(NULL_TO_STRING_LIST,'/', 0, 1));
</key_calls>
<failing_test class="org.apache.commons.lang3.StringUtilsTest" method="testJoin_ArrayChar">
    public void testJoin_ArrayChar() {
        assertEquals(null, StringUtils.join((Object[]) null, ','));
        assertEquals(TEXT_LIST_CHAR, StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR));
        assertEquals("", StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR));
        assertEquals(";;foo", StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR));
        assertEquals("foo;2", StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR));

        assertEquals("/", StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length-1));
        assertEquals("foo", StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1));
        assertEquals("null", StringUtils.join(NULL_TO_STRING_LIST,'/', 0, 1));
        assertEquals("foo/2", StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2));
        assertEquals("2", StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2));
        assertEquals("", StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1));
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.lang3.StringUtilsTest::testJoin_ArrayChar
java.lang.NullPointerException
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.
The test method depends on the following from its test class — setUp, helper methods, constants, fixture files. Replicate this environment EXACTLY (register the same files, wire the same providers, use the same constants); do NOT improvise setup the test performs differently, and DROP any check whose setup you cannot replicate:
<test_support class="org.apache.commons.lang3.StringUtilsTest">
// --- class fields/constants the test uses ---
private static final String[] ARRAY_LIST = { "foo", "bar", "baz" };
private static final String[] EMPTY_ARRAY_LIST = {};
private static final Object[] NULL_TO_STRING_LIST = { new Object(){ @Override public String toString() { return null;
private static final String[] MIXED_ARRAY_LIST = {null, "", "foo"};
private static final Object[] MIXED_TYPE_LIST = {"foo", Long.valueOf(2L)};
private static final char   SEPARATOR_CHAR = ';';
private static final String TEXT_LIST_CHAR = "foo;bar;baz";
</test_support>
Trigger lines: null passed as an array element. Mirror these calls:
<key_calls class="org.apache.commons.lang3.StringUtilsTest" method="testJoin_Objectarray">
//        assertEquals(null, StringUtils.join(null)); // generates warning
assertEquals(null, StringUtils.join((Object[]) null)); // equivalent explicit cast
assertEquals("", StringUtils.join((Object) null)); // => new Object[]{null}
assertEquals("null", StringUtils.join(NULL_TO_STRING_LIST));
assertEquals("a", StringUtils.join(new String[] {null, "a", ""}));
</key_calls>
<failing_test class="org.apache.commons.lang3.StringUtilsTest" method="testJoin_Objectarray">
    public void testJoin_Objectarray() {
//        assertEquals(null, StringUtils.join(null)); // generates warning
        assertEquals(null, StringUtils.join((Object[]) null)); // equivalent explicit cast
        // test additional varargs calls
        assertEquals("", StringUtils.join()); // empty array
        assertEquals("", StringUtils.join((Object) null)); // => new Object[]{null}

        assertEquals("", StringUtils.join(EMPTY_ARRAY_LIST));
        assertEquals("", StringUtils.join(NULL_ARRAY_LIST));
        assertEquals("null", StringUtils.join(NULL_TO_STRING_LIST));
        assertEquals("abc", StringUtils.join(new String[] {"a", "b", "c"}));
        assertEquals("a", StringUtils.join(new String[] {null, "a", ""}));
        assertEquals("foo", StringUtils.join(MIXED_ARRAY_LIST));
        assertEquals("foo2", StringUtils.join(MIXED_TYPE_LIST));
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.lang3.StringUtilsTest::testJoin_Objectarray
java.lang.NullPointerException
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.
The test method depends on the following from its test class — setUp, helper methods, constants, fixture files. Replicate this environment EXACTLY (register the same files, wire the same providers, use the same constants); do NOT improvise setup the test performs differently, and DROP any check whose setup you cannot replicate:
<test_support class="org.apache.commons.lang3.StringUtilsTest">
// --- class fields/constants the test uses ---
private static final String[] EMPTY_ARRAY_LIST = {};
private static final String[] NULL_ARRAY_LIST = {null};
private static final Object[] NULL_TO_STRING_LIST = { new Object(){ @Override public String toString() { return null;
private static final String[] MIXED_ARRAY_LIST = {null, "", "foo"};
private static final Object[] MIXED_TYPE_LIST = {"foo", Long.valueOf(2L)};
</test_support>

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  abbreviate(String str, int maxWidth) / (String str, int offset, int maxWidth)
  center(String str, int size) / (String str, int size, char padChar) / (String str, int size, String padStr)
  chomp(String str) / (String str, String separator)
  contains(CharSequence seq, int searchChar) / (CharSequence seq, CharSequence searchSeq)
  containsAny(CharSequence cs, char... searchChars) / (CharSequence cs, CharSequence searchChars)
  containsNone(CharSequence cs, char... searchChars) / (CharSequence cs, String invalidChars)
  containsOnly(CharSequence cs, char... valid) / (CharSequence cs, String validChars)
  defaultString(String str) / (String str, String defaultStr)

METHOD FAMILIES over the same input space (factory/parser siblings — where the docs state a selection or agreement rule between family members, equivalent inputs must respect it):
  contains* family: containsAny, containsIgnoreCase, containsNone, containsOnly, containsWhitespace
  default* family: defaultIfBlank, defaultIfEmpty, defaultString
  ends* family: endsWith, endsWithAny, endsWithIgnoreCase
  index* family: indexOf, indexOfAny, indexOfAnyBut, indexOfDifference, indexOfIgnoreCase
  last* family: lastIndexOf, lastIndexOfAny, lastIndexOfIgnoreCase, lastOrdinalIndexOf
  remove* family:

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- StringUtils.length
- TestBean.toString
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.lang3.FuzzHarness.anchor
- org.apache.commons.lang3.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.lang3.StringUtils.join
Crashes already found:
- java.lang.NullPointerException@org.apache.commons.lang3.StringUtils.join
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- StringUtils.length
- TestBean.toString
Check FAMILIES already covered by accepted harnesses: {anchor-char, anchor-fixture, anchor-null-array, anchor-object, anchor-string, char-vs-string, empty-shared, fixture-overload, no-sep-sibling, null-empty-sep, null-sep, object-vs-range, overload-agree, single-item}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.NullPointerException@org.apache.commons.lang3.StringUtils.join. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
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
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~8,638 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    /*
     * Contract asserted:
     * - From StringUtils.join examples, joining a one-element range whose sole element's toString() is null
     *   must produce "null", not throw.
     * - For any correct implementation, joining a slice [start,end) is equivalent to first copying that slice
     *   into a new array and then joining [0,len) with the same separator.
     * - For any non-null String s, StringUtils.length(s) == s.length().
     * A patch that merely suppresses the known throw but mishandles the boundary element or skips content
     * will violate the slice-equivalence or length consistency checks.
     */
    private static final class NullToString {
        @Override
        public String toString() {
            return null;
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            runAnchors();
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
        }

        int extraPrefix = data.consumeInt(0, 4);
        int extraSuffix = data.consumeInt(0, 4);
        int middleCount = data.consumeInt(0, 4);
        boolean useChar = data.consumeBoolean();

        Object[] array = new Object[extraPrefix + 1 + middleCount + extraSuffix];
        for (int i = 0; i < extraPrefix; i++) {
            array[i] = makeRegularElement(data, i);
        }

        int startIndex = extraPrefix;
        array[startIndex] = new NullToString();

        for (int i = startIndex + 1; i < startIndex + 1 + middleCount; i++) {
            array[i] = makeRegularElement(data, i);
        }
        for (int i = startIndex + 1 + middleCount; i < array.length; i++) {
            array[i] = makeRegularElement(data, i);
        }

        int endIndex = startIndex + 1 + middleCount;
        if (endIndex < startIndex) {
            return;
        }

        if (useChar) {
            char separator = (char) data.consumeInt(0, Character.MAX_VALUE);
            exerciseChar(array, separator, startIndex, endIndex);
        } else {
            String separator = data.consumeBoolean() ? null : data.consumeString(8);
            exerciseString(array, separator, startIndex, endIndex);
        }
    }

    private static void runAnchors() {
        if (StringUtils.join((Object[]) null, ',') != null) {
            throw new RuntimeException("[oracle:anchor-null-array] metamorphic violation: explicit null array must yield null");
        }

        Object[] nullToStringList = new Object[] { new NullToString() };

        String charJoined = StringUtils.join(nullToStringList, '/', 0, 1);
        if (!"null".equals(charJoined)) {
            throw new RuntimeException("[oracle:anchor-char-nullstr] metamorphic violation: expected one-element char join to be \"null\" lhs=" + charJoined);
        }
        if (StringUtils.length(charJoined) != charJoined.length()) {
            throw new RuntimeException("[oracle:length-char-anchor] metamorphic violation: StringUtils.length mismatch lhs="
                    + StringUtils.length(charJoined) + " rhs=" + charJoined.length());
        }

        String stringJoined = StringUtils.join(nullToStringList, "/", 0, 1);
        if (!"null".equals(stringJoined)) {
            throw new RuntimeException("[oracle:anchor-string-nullstr] metamorphic violation: expected one-element string join to be \"null\" lhs=" + stringJoined);
        }
        if (StringUtils.length(stringJoined) != stringJoined.length()) {
            throw new RuntimeException("[oracle:length-string-anchor] metamorphic violation: StringUtils.length mismatch lhs="
                    + StringUtils.length(stringJoined) + " rhs=" + stringJoined.length());
        }
    }

    private static void exerciseChar(Object[] array, char separator, int startIndex, int endIndex) {
        try {
            String direct = StringUtils.join(array, separator, startIndex, endIndex);
            String copied = StringUtils.join(java.util.Arrays.copyOfRange(array, startIndex, endIndex), separator, 0, endIndex - startIndex);

            if (!safeEquals(direct, copied)) {
                throw new RuntimeException("[oracle:slice-copy-char] metamorphic violation: equivalent char-join inputs disagree start="
                        + startIndex + " end=" + endIndex + " sep=" + (int) separator + " lhs=" + direct + " rhs=" + copied);
            }

            if (direct != null && StringUtils.length(direct) != direct.length()) {
                throw new RuntimeException("[oracle:length-char] metamorphic violation: StringUtils.length mismatch lhs="
                        + StringUtils.length(direct) + " rhs=" + direct.length() + " value=" + direct);
            }

            if (endIndex - startIndex == 1 && !"null".equals(direct)) {
                throw new RuntimeException("[oracle:single-char-nullstr] metamorphic violation: one-element char join must be \"null\" lhs=" + direct);
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

    private static void exerciseString(Object[] array, String separator, int startIndex, int endIndex) {
        try {
            String direct = StringUtils.join(array, separator, startIndex, endIndex);
            String copied = StringUtils.join(java.util.Arrays.copyOfRange(array, startIndex, endIndex), separator, 0, endIndex - startIndex);

            if (!safeEquals(direct, copied)) {
                throw new RuntimeException("[oracle:slice-copy-string] metamorphic violation: equivalent string-join inputs disagree start="
                        + startIndex + " end=" + endIndex + " sep=" + separator + " lhs=" + direct + " rhs=" + copied);
            }

            if (direct != null && StringUtils.length(direct) != direct.length()) {
                throw new RuntimeException("[oracle:length-string] metamorphic violation: StringUtils.length mismatch lhs="
                        + StringUtils.length(direct) + " rhs=" + direct.length() + " value=" + direct);
            }

            if (endIndex - startIndex == 1 && !"null".equals(direct)) {
                throw new RuntimeException("[oracle:single-string-nullstr] metamorphic violation: one-element string join must be \"null\" lhs=" + direct);
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

    private static Object makeRegularElement(FuzzedDataProvider data, int salt) {
        switch (data.consumeInt(0, 5)) {
            case 0:
                return null;
            case 1:
                return data.consumeString(12);
            case 2:
                return Integer.valueOf(data.consumeInt(-1000, 1000));
            case 3:
                return Long.valueOf(data.consumeInt(-1000, 1000));
            case 4:
                return Boolean.valueOf(data.consumeBoolean());
            default:
                return "x" + salt + data.consumeAsciiString(8);
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException) && !(t instanceof RuntimeException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String method = ste.getMethodName();
            if ("org.apache.commons.lang3.StringUtils".equals(cls)
                    && ("join".equals(method) || "length".equals(method))) {
                return true;
            }
            if ("org.apache.commons.lang3.TestBean".equals(cls) && "toString".equals(method)) {
                return true;
            }
            if ("org.apache.commons.lang3.FuzzHarness".equals(cls) && method.startsWith("exercise")) {
                return true;
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
## [6] ⚙️ harness-attempt · `attempt_002`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [7] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~42,474 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.apache.commons.lang3` (`package org.apache.commons.lang3;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Lang_20_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/lang3/StringUtils.java	
+++ /src/main/java/org/apache/commons/lang3/StringUtils.java	
@@ -3295,8 +3295,7 @@
             return EMPTY;
         }
         
-        StringBuilder buf = new StringBuilder((array[startIndex] == null ? 16 : array[startIndex].toString().length()) + 1);
-
+        StringBuilder buf=new StringBuilder(256);
         for (int i = startIndex; i < endIndex; i++) {
             if (i > startIndex) {
                 buf.append(separator);
@@ -3380,7 +3379,7 @@
             return EMPTY;
         }
 
-        StringBuilder buf = new StringBuilder((array[startIndex] == null ? 16 : array[startIndex].toString().length()) + separator.length());
+        StringBuilder buf=new StringBuilder(4);
 
         for (int i = startIndex; i < endIndex; i++) {
             if (i > startIndex) {

</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
</source_imports>

Function `join`:
<signature>
public static String join(Object[] array, char separator, int startIndex, int endIndex)
</signature>
<code>
    public static String join(Object[] array, char separator, int startIndex, int endIndex) {
        if (array == null) {
            return null;
        }
        int noOfItems = (endIndex - startIndex);
        if (noOfItems <= 0) {
            return EMPTY;
        }
        
        StringBuilder buf = new StringBuilder((array[startIndex] == null ? 16 : array[startIndex].toString().length()) + 1);

        for (int i = startIndex; i < endIndex; i++) {
            if (i > startIndex) {
                buf.append(separator);
            }
            if (array[i] != null) {
                buf.append(array[i]);
            }
        }
        return buf.toString();
    }
</code>
Call-site examples (use these as a guide for constructing the target call):
<xref>
public void testJoin_ArrayChar() {
        assertEquals(null, StringUtils.join((Object[]) null, ','));
        assertEquals(TEXT_LIST_CHAR, StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR));
        assertEquals("", StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR));
        assertEquals(";;foo", StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR));
        assertEquals("foo;2", StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR));

        assertEquals("/", StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length-1));
        assertEquals("foo", StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1));
        assertEquals("null", StringUtils.join(NULL_TO_STRING_LIST,'/', 0, 1));
        assertEquals("foo/2", StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2));
        assertEquals("2", StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2));
        assertEquals("", StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1));
    }
</xref>
Methods called by `join` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="toString" from="AnnotationUtils.java">
<signature>
public static String toString(Annotation a)
</signature>
<code>
    public static String toString(final Annotation a) {
        ToStringBuilder builder = new ToStringBuilder(a, TO_STRING_STYLE);
        for (Method m : a.annotationType().getDeclaredMethods()) {
            if (m.getParameterTypes().length > 0) {
                continue; //wtf?
            }
            try {
                builder.append(m.getName(), m.invoke(a));
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return builder.build();
    }
</code>
<implementation in="AnnotationUtils.java">
    public static String toString(final Annotation a) {
        ToStringBuilder builder = new ToStringBuilder(a, TO_STRING_STYLE);
        for (Method m : a.annotationType().getDeclaredMethods()) {
            if (m.getParameterTypes().length > 0) {
                continue; //wtf?
            }
            try {
                builder.append(m.getName(), m.invoke(a));
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return builder.build();
    }
</implementation>
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
</callee>
<callee name="length" from="StringUtils.java">
<signature>
public static int length(CharSequence cs)
</signature>
<code>
    public static int length(CharSequence cs) {
        return cs == null ? 0 : cs.length();
    }
</code>
</callee>
STATE COUPLING: these members of the same class share the listed field(s) with `join` but neither calls the other — state one of them writes is what the other reports. A defect the patch leaves (or introduces) in that shared state stays observable through these members even when `join`'s own output looks right, so a strong harness CHECKS THEY AGREE: compare what a reader reports against what the writer/constructor established.
  - `public static String trimToEmpty(String str)` (shared field(s): EMPTY)
      doc: <p>Removes control characters (char &lt;= 32) from both ends of this String returning an empty String ("") if the String is empty ("") after the trim or if it is {@code null}. <p>The String is trimmed using {@link String#trim()}. Trim removes start and end characters &lt;= 32. To strip whitespace use {@link #stripToEmp…
  - `public static String stripToEmpty(String str)` (shared field(s): EMPTY)
      doc: <p>Strips whitespace from the start and end of a String  returning an empty String if {@code null} input.</p> <p>This is similar to {@link #trimToEmpty(String)} but removes whitespace. Whitespace is defined by {@link Character#isWhitespace(char)}.</p> <pre> StringUtils.stripToEmpty(null)     = "" StringUtils.stripToEmp…
  - `public static String substring(String str, int start)` (shared field(s): EMPTY)
      doc: <p>Gets a substring from the specified String avoiding exceptions.</p> <p>A negative start position can be used to start {@code n} characters from the end of the String.</p> <p>A {@code null} String will return {@code null}. An empty ("") String will return "".</p> <pre> StringUtils.substring(null, *)   = null StringUt…
  - `public static String substring(String str, int start, int end)` (shared field(s): EMPTY)
      doc: <p>Gets a substring from the specified String avoiding exceptions.</p> <p>A negative start position can be used to start/end {@code n} characters from the end of the String.</p> <p>The returned substring starts with the character in the {@code start} position and ends before the {@code end} position. All position count…
  - `public static String left(String str, int len)` (shared field(s): EMPTY)
      doc: <p>Gets the leftmost {@code len} characters of a String.</p> <p>If {@code len} characters are not available, or the String is {@code null}, the String will be returned without an exception. An empty String is returned if len is negative.</p> <pre> StringUtils.left(null, *)    = null StringUtils.left(*, -ve)     = "" St…

Function `join`:
<signature>
public static String join(Object[] array, String separator, int startIndex, int endIndex)
</signature>
<code>
    public static String join(Object[] array, String separator, int startIndex, int endIndex) {
        if (array == null) {
            return null;
        }
        if (separator == null) {
            separator = EMPTY;
        }

        // endIndex - startIndex > 0:   Len = NofStrings *(len(firstString) + len(separator))
        //           (Assuming that all Strings are roughly equally long)
        int noOfItems = (endIndex - startIndex);
        if (noOfItems <= 0) {
            return EMPTY;
        }

        StringBuilder buf = new StringBuilder((array[startIndex] == null ? 16 : array[startIndex].toString().length()) + separator.length());

        for (int i = startIndex; i < endIndex; i++) {
            if (i > startIndex) {
                buf.append(separator);
            }
            if (array[i] != null) {
                buf.append(array[i]);
            }
        }
        return buf.toString();
    }
</code>
Call-site examples (use these as a guide for constructing the target call):
<xref>
public void testJoin_ArrayString() {
        assertEquals(null, StringUtils.join((Object[]) null, null));
        assertEquals(TEXT_LIST_NOSEP, StringUtils.join(ARRAY_LIST, null));
        assertEquals(TEXT_LIST_NOSEP, StringUtils.join(ARRAY_LIST, ""));
        
        assertEquals("", StringUtils.join(NULL_ARRAY_LIST, null));
        
        assertEquals("", StringUtils.join(EMPTY_ARRAY_LIST, null));
        assertEquals("", StringUtils.join(EMPTY_ARRAY_LIST, ""));
        assertEquals("", StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR));

        assertEquals(TEXT_LIST, StringUtils.join(ARRAY_LIST, SEPARATOR));
        assertEquals(",,foo", StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR));
        assertEquals("foo,2", StringUtils.join(MIXED_TYPE_LIST, SEPARATOR));

        assertEquals("/", StringUtils.join(MIXED_ARRAY_LIST, "/", 0, MIXED_ARRAY_LIST.length-1));
        assertEquals("", StringUtils.join(MIXED_ARRAY_LIST, "", 0, MIXED_ARRAY_LIST.length-1));
        assertEquals("foo", StringUtils.join(MIXED_TYPE_LIST, "/", 0, 1));
        assertEquals("foo/2", StringUtils.join(MIXED_TYPE_LIST, "/", 0, 2));
        assertEquals("2", StringUtils.join(MIXED_TYPE_LIST, "/", 1, 2));
        assertEquals("", StringUtils.join(MIXED_TYPE_LIST, "/", 2, 1));
    }
</xref>
Methods called by `join` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="toString" from="AnnotationUtils.java">
<signature>
public static String toString(Annotation a)
</signature>
<code>
    public static String toString(final Annotation a) {
        ToStringBuilder builder = new ToStringBuilder(a, TO_STRING_STYLE);
        for (Method m : a.annotationType().getDeclaredMethods()) {
            if (m.getParameterTypes().length > 0) {
                continue; //wtf?
            }
            try {
                builder.append(m.getName(), m.invoke(a));
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return builder.build();
    }
</code>
<implementation in="AnnotationUtils.java">
    public static String toString(final Annotation a) {
        ToStringBuilder builder = new ToStringBuilder(a, TO_STRING_STYLE);
        for (Method m : a.annotationType().getDeclaredMethods()) {
            if (m.getParameterTypes().length > 0) {
                continue; //wtf?
            }
            try {
                builder.append(m.getName(), m.invoke(a));
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return builder.build();
    }
</implementation>
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
</callee>
<callee name="length" from="StringUtils.java">
<signature>
public static int length(CharSequence cs)
</signature>
<code>
    public static int length(CharSequence cs) {
        return cs == null ? 0 : cs.length();
    }
</code>
</callee>
STATE COUPLING: these members of the same class share the listed field(s) with `join` but neither calls the other — state one of them writes is what the other reports. A defect the patch leaves (or introduces) in that shared state stays observable through these members even when `join`'s own output looks right, so a strong harness CHECKS THEY AGREE: compare what a reader reports against what the writer/constructor established.
  - `public static String trimToEmpty(String str)` (shared field(s): EMPTY)
      doc: <p>Removes control characters (char &lt;= 32) from both ends of this String returning an empty String ("") if the String is empty ("") after the trim or if it is {@code null}. <p>The String is trimmed using {@link String#trim()}. Trim removes start and end characters &lt;= 32. To strip whitespace use {@link #stripToEmp…
  - `public static String stripToEmpty(String str)` (shared field(s): EMPTY)
      doc: <p>Strips whitespace from the start and end of a String  returning an empty String if {@code null} input.</p> <p>This is similar to {@link #trimToEmpty(String)} but removes whitespace. Whitespace is defined by {@link Character#isWhitespace(char)}.</p> <pre> StringUtils.stripToEmpty(null)     = "" StringUtils.stripToEmp…
  - `public static String substring(String str, int start)` (shared field(s): EMPTY)
      doc: <p>Gets a substring from the specified String avoiding exceptions.</p> <p>A negative start position can be used to start {@code n} characters from the end of the String.</p> <p>A {@code null} String will return {@code null}. An empty ("") String will return "".</p> <pre> StringUtils.substring(null, *)   = null StringUt…
  - `public static String substring(String str, int start, int end)` (shared field(s): EMPTY)
      doc: <p>Gets a substring from the specified String avoiding exceptions.</p> <p>A negative start position can be used to start/end {@code n} characters from the end of the String.</p> <p>The returned substring starts with the character in the {@code start} position and ends before the {@code end} position. All position count…
  - `public static String left(String str, int len)` (shared field(s): EMPTY)
      doc: <p>Gets the leftmost {@code len} characters of a String.</p> <p>If {@code len} characters are not available, or the String is {@code null}, the String will be returned without an exception. An empty String is returned if len is negative.</p> <pre> StringUtils.left(null, *)    = null StringUtils.left(*, -ve)     = "" St…

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `StringUtils`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.NullPointerException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `join`, `join` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: java.lang.NullPointerException
thrown_at: org.apache.commons.lang3.StringUtils.join(StringUtils.java:3383)
</ground_truth_crash>
Trigger lines: null passed as an array element. Mirror these calls:
<key_calls class="org.apache.commons.lang3.StringUtilsTest" method="testJoin_ArrayChar">
assertEquals(null, StringUtils.join((Object[]) null, ','));
assertEquals("null", StringUtils.join(NULL_TO_STRING_LIST,'/', 0, 1));
</key_calls>
<failing_test class="org.apache.commons.lang3.StringUtilsTest" method="testJoin_ArrayChar">
    public void testJoin_ArrayChar() {
        assertEquals(null, StringUtils.join((Object[]) null, ','));
        assertEquals(TEXT_LIST_CHAR, StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR));
        assertEquals("", StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR));
        assertEquals(";;foo", StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR));
        assertEquals("foo;2", StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR));

        assertEquals("/", StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length-1));
        assertEquals("foo", StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1));
        assertEquals("null", StringUtils.join(NULL_TO_STRING_LIST,'/', 0, 1));
        assertEquals("foo/2", StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2));
        assertEquals("2", StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2));
        assertEquals("", StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1));
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.lang3.StringUtilsTest::testJoin_ArrayChar
java.lang.NullPointerException
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.
The test method depends on the following from its test class — setUp, helper methods, constants, fixture files. Replicate this environment EXACTLY (register the same files, wire the same providers, use the same constants); do NOT improvise setup the test performs differently, and DROP any check whose setup you cannot replicate:
<test_support class="org.apache.commons.lang3.StringUtilsTest">
// --- class fields/constants the test uses ---
private static final String[] ARRAY_LIST = { "foo", "bar", "baz" };
private static final String[] EMPTY_ARRAY_LIST = {};
private static final Object[] NULL_TO_STRING_LIST = { new Object(){ @Override public String toString() { return null;
private static final String[] MIXED_ARRAY_LIST = {null, "", "foo"};
private static final Object[] MIXED_TYPE_LIST = {"foo", Long.valueOf(2L)};
private static final char   SEPARATOR_CHAR = ';';
private static final String TEXT_LIST_CHAR = "foo;bar;baz";
</test_support>
Trigger lines: null passed as an array element. Mirror these calls:
<key_calls class="org.apache.commons.lang3.StringUtilsTest" method="testJoin_Objectarray">
//        assertEquals(null, StringUtils.join(null)); // generates warning
assertEquals(null, StringUtils.join((Object[]) null)); // equivalent explicit cast
assertEquals("", StringUtils.join((Object) null)); // => new Object[]{null}
assertEquals("null", StringUtils.join(NULL_TO_STRING_LIST));
assertEquals("a", StringUtils.join(new String[] {null, "a", ""}));
</key_calls>
<failing_test class="org.apache.commons.lang3.StringUtilsTest" method="testJoin_Objectarray">
    public void testJoin_Objectarray() {
//        assertEquals(null, StringUtils.join(null)); // generates warning
        assertEquals(null, StringUtils.join((Object[]) null)); // equivalent explicit cast
        // test additional varargs calls
        assertEquals("", StringUtils.join()); // empty array
        assertEquals("", StringUtils.join((Object) null)); // => new Object[]{null}

        assertEquals("", StringUtils.join(EMPTY_ARRAY_LIST));
        assertEquals("", StringUtils.join(NULL_ARRAY_LIST));
        assertEquals("null", StringUtils.join(NULL_TO_STRING_LIST));
        assertEquals("abc", StringUtils.join(new String[] {"a", "b", "c"}));
        assertEquals("a", StringUtils.join(new String[] {null, "a", ""}));
        assertEquals("foo", StringUtils.join(MIXED_ARRAY_LIST));
        assertEquals("foo2", StringUtils.join(MIXED_TYPE_LIST));
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.lang3.StringUtilsTest::testJoin_Objectarray
java.lang.NullPointerException
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.
The test method depends on the following from its test class — setUp, helper methods, constants, fixture files. Replicate this environment EXACTLY (register the same files, wire the same providers, use the same constants); do NOT improvise setup the test performs differently, and DROP any check whose setup you cannot replicate:
<test_support class="org.apache.commons.lang3.StringUtilsTest">
// --- class fields/constants the test uses ---
private static final String[] EMPTY_ARRAY_LIST = {};
private static final String[] NULL_ARRAY_LIST = {null};
private static final Object[] NULL_TO_STRING_LIST = { new Object(){ @Override public String toString() { return null;
private static final String[] MIXED_ARRAY_LIST = {null, "", "foo"};
private static final Object[] MIXED_TYPE_LIST = {"foo", Long.valueOf(2L)};
</test_support>

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  abbreviate(String str, int maxWidth) / (String str, int offset, int maxWidth)
  center(String str, int size) / (String str, int size, char padChar) / (String str, int size, String padStr)
  chomp(String str) / (String str, String separator)
  contains(CharSequence seq, int searchChar) / (CharSequence seq, CharSequence searchSeq)
  containsAny(CharSequence cs, char... searchChars) / (CharSequence cs, CharSequence searchChars)
  containsNone(CharSequence cs, char... searchChars) / (CharSequence cs, String invalidChars)
  containsOnly(CharSequence cs, char... valid) / (CharSequence cs, String validChars)
  defaultString(String str) / (String str, String defaultStr)

METHOD FAMILIES over the same input space (factory/parser siblings — where the docs state a selection or agreement rule between family members, equivalent inputs must respect it):
  contains* family: containsAny, containsIgnoreCase, containsNone, containsOnly, containsWhitespace
  default* family: defaultIfBlank, defaultIfEmpty, defaultString
  ends* family: endsWith, endsWithAny, endsWithIgnoreCase
  index* family: indexOf, indexOfAny, indexOfAnyBut, indexOfDifference, indexOfIgnoreCase
  last* family: lastIndexOf, lastIndexOfAny, lastIndexOfIgnoreCase, lastOrdinalIndexOf
  remove* family:

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- StringUtils.length
- TestBean.toString
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.lang3.FuzzHarness.anchor
- org.apache.commons.lang3.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.lang3.FuzzHarness.runAnchors
- org.apache.commons.lang3.StringUtils.join
Crashes already found:
- java.lang.NullPointerException@org.apache.commons.lang3.StringUtils.join
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- StringUtils.length
- TestBean.toString
Check FAMILIES already covered by accepted harnesses: {anchor-char, anchor-char-nullstr, anchor-fixture, anchor-null-array, anchor-object, anchor-string, anchor-string-nullstr, char-vs-string, empty-shared, fixture-overload, length-char, length-char-anchor, length-string, length-string-anchor, no-sep-sibling, null-empty-sep, null-sep, object-vs-range, overload-agree, single-char-nullstr, single-item, single-string-nullstr, slice-copy-char, slice-copy-string}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.NullPointerException@org.apache.commons.lang3.StringUtils.join. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
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
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~8,503 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final Object NULL_TO_STRING = new Object() {
        @Override
        public String toString() {
            return null;
        }
    };

    private static final Object[] NULL_TO_STRING_LIST = { NULL_TO_STRING };
    private static final String[] EMPTY_ARRAY_LIST = {};
    private static final String[] NULL_ARRAY_LIST = { null };
    private static final String[] MIXED_ARRAY_LIST = { null, "", "foo" };
    private static final Object[] MIXED_TYPE_LIST = { "foo", Long.valueOf(2L) };
    private static final String[] ARRAY_LIST = { "foo", "bar", "baz" };
    private static final char SEPARATOR_CHAR = ';';
    private static final String TEXT_LIST_CHAR = "foo;bar;baz";

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchors();

        int len = data.consumeInt(1, 6);
        Object[] array = new Object[len];
        for (int i = 0; i < len; i++) {
            int kind = data.consumeInt(0, 4);
            if (kind == 0) {
                array[i] = null;
            } else if (kind == 1) {
                array[i] = NULL_TO_STRING;
            } else if (kind == 2) {
                array[i] = data.consumeAsciiString(8);
            } else if (kind == 3) {
                array[i] = Long.valueOf(data.consumeInt(-1000000, 1000000));
            } else {
                array[i] = Boolean.valueOf(data.consumeBoolean());
            }
        }

        int start = data.consumeInt(0, len - 1);
        array[start] = NULL_TO_STRING;
        int end = data.consumeInt(start + 1, len);

        char sepChar = (char) (data.consumeByte() & 0xff);
        String sepString = data.consumeBoolean() ? null : data.consumeString(4);

        exerciseCharJoin(array, start, end, sepChar);
        exerciseStringJoin(array, start, end, sepString);
    }

    private static void runAnchors() {
        if (StringUtils.join((Object[]) null, ',') != null) {
            throw new RuntimeException("[oracle:anchor-null-array] metamorphic violation: join((Object[])null, ',') must return null");
        }
        if (StringUtils.join((Object[]) null, "/") != null) {
            throw new RuntimeException("[oracle:anchor-null-array-string] metamorphic violation: join((Object[])null, \"/\") must return null");
        }

        try {
            String v = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            checkExactNullStringResult(v, "anchor-char");
            checkLengthOracle(NULL_TO_STRING_LIST, 0, 1, 1, v, "anchor-char-len");
        } catch (RuntimeException t) {
            handleJoinThrowable(t, "anchor-char");
        }

        try {
            String v = StringUtils.join(NULL_TO_STRING_LIST, "/", 0, 1);
            checkExactNullStringResult(v, "anchor-string");
            checkLengthOracle(NULL_TO_STRING_LIST, 0, 1, StringUtils.length("/"), v, "anchor-string-len");
        } catch (RuntimeException t) {
            handleJoinThrowable(t, "anchor-string");
        }

        try {
            String text = StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR, 0, ARRAY_LIST.length);
            if (!TEXT_LIST_CHAR.equals(text)) {
                throw new RuntimeException("[oracle:anchor-fixture-text] metamorphic violation: expected=" + TEXT_LIST_CHAR + " actual=" + text);
            }
            checkLengthOracle(ARRAY_LIST, 0, ARRAY_LIST.length, 1, text, "anchor-fixture-len");
        } catch (RuntimeException t) {
            handleNonRootThrowable(t);
        }

        // StringUtils.length is in the reachable region; a correct implementation must
        // report the exact number of characters in the returned join output.
        if (StringUtils.length("null") != 4) {
            throw new RuntimeException("[oracle:length-basic] metamorphic violation: StringUtils.length(\"null\") must be 4");
        }

        if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST, '/', 0, 0))) {
            throw new RuntimeException("[oracle:empty-range] metamorphic violation: empty range must join to empty string");
        }
        if (!"".equals(StringUtils.join(NULL_ARRAY_LIST, '/', 0, 1))) {
            throw new RuntimeException("[oracle:null-element] metamorphic violation: null element object contributes empty text");
        }
        if (!"foo".equals(StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length))) {
            throw new RuntimeException("[oracle:mixed-array] metamorphic violation: expected foo for documented fixture");
        }
        if (!"foo2".equals(StringUtils.join(MIXED_TYPE_LIST, "", 0, MIXED_TYPE_LIST.length))) {
            throw new RuntimeException("[oracle:mixed-type] metamorphic violation: expected foo2 for documented fixture");
        }
    }

    private static void exerciseCharJoin(Object[] array, int start, int end, char sepChar) {
        try {
            String joined = StringUtils.join(array, sepChar, start, end);
            checkLengthOracle(array, start, end, 1, joined, "char-len");
        } catch (RuntimeException t) {
            handleJoinThrowable(t, "char");
        }
    }

    private static void exerciseStringJoin(Object[] array, int start, int end, String sepString) {
        try {
            String joined = StringUtils.join(array, sepString, start, end);
            int sepLen = StringUtils.length(sepString);
            checkLengthOracle(array, start, end, sepLen, joined, "string-len");
        } catch (RuntimeException t) {
            handleJoinThrowable(t, "string");
        }
    }

    private static void checkExactNullStringResult(String v, String id) {
        if (!"null".equals(v)) {
            throw new RuntimeException("[oracle:" + id + "] metamorphic violation: single element with toString()==null must join as \"null\" actual=" + v);
        }
    }

    private static void checkLengthOracle(Object[] array, int start, int end, int separatorLength, String joined, String id) {
        int reported = StringUtils.length(joined);
        int independent = expectedJoinedLength(array, start, end, separatorLength);
        if (reported != independent) {
            throw new RuntimeException("[oracle:" + id + "] consistency violation: reported=" + reported + " independent=" + independent + " start=" + start + " end=" + end + " joined=" + joined);
        }
    }

    private static int expectedJoinedLength(Object[] array, int start, int end, int separatorLength) {
        int items = end - start;
        if (items <= 0) {
            return 0;
        }
        int total = 0;
        for (int i = start; i < end; i++) {
            if (i > start) {
                total += separatorLength;
            }
            if (array[i] != null) {
                total += StringUtils.length(String.valueOf(array[i]));
            }
        }
        return total;
    }

    private static void handleJoinThrowable(RuntimeException t, String id) {
        if (isCleanRejection(t)) {
            return;
        }
        if (isRootCauseNpeFromJoin(t)) {
            throw new RuntimeException("[oracle:" + id + "-npe] metamorphic violation: valid single/ranged join input threw root-cause NPE", t);
        }
        handleNonRootThrowable(t);
    }

    private static void handleNonRootThrowable(RuntimeException t) {
        if (isCleanRejection(t)) {
            return;
        }
        if (hasRelevantFrame(t, "org.apache.commons.lang3.StringUtils", "length")) {
            return;
        }
        return;
    }

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean isRootCauseNpeFromJoin(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        return hasRelevantFrame(t, "org.apache.commons.lang3.StringUtils", "join");
    }

    private static boolean hasRelevantFrame(Throwable t, String className, String methodName) {
        for (StackTraceElement e : t.getStackTrace()) {
            if (className.equals(e.getClassName()) && methodName.equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}
```

</details>

---
## [8] ⚙️ harness-attempt · `attempt_003`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.RuntimeException: [oracle:anchor-char-npe] metamorphic violation: valid single/ranged join input threw root-cause NPE

---
## [9] ⚙️ corpus-seed · `attempt_001`
**output:** **18 literal-variation seeds into the patched-side fuzz corpus**
<details><summary>▸ detail (~659 chars)</summary>

- detail: {'sample': [', StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR));\n        assertEquals(', ', StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR));\n        assertEquals(', ', StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR));\n\n        assertEquals(', ", StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length-1));\n        assertEquals(", ", StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1));\n        assertEquals(", ", StringUtils.join(NULL_TO_STRING_LIST,'/', 0, 1));\n        assertEquals(", ", StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2));\n        assertEquals(", ", StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2));\n        assertEquals("]}

</details>

---
## [10] ⚙️ corpus-seed · `attempt_002`
**output:** **18 literal-variation seeds into the patched-side fuzz corpus**
<details><summary>▸ detail (~659 chars)</summary>

- detail: {'sample': [', StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR));\n        assertEquals(', ', StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR));\n        assertEquals(', ', StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR));\n\n        assertEquals(', ", StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length-1));\n        assertEquals(", ", StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1));\n        assertEquals(", ", StringUtils.join(NULL_TO_STRING_LIST,'/', 0, 1));\n        assertEquals(", ", StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2));\n        assertEquals(", ", StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2));\n        assertEquals("]}

</details>

---
## [11] ⚙️ corpus-seed · `attempt_003`
**output:** **18 literal-variation seeds into the patched-side fuzz corpus**
<details><summary>▸ detail (~659 chars)</summary>

- detail: {'sample': [', StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR));\n        assertEquals(', ', StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR));\n        assertEquals(', ', StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR));\n\n        assertEquals(', ", StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length-1));\n        assertEquals(", ", StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1));\n        assertEquals(", ", StringUtils.join(NULL_TO_STRING_LIST,'/', 0, 1));\n        assertEquals(", ", StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2));\n        assertEquals(", ", StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2));\n        assertEquals("]}

</details>

---
## [12] ⚙️ patched-fuzz · `attempt_001`
**output:** **quiet on patched build (no overfit signal)**

---
## [13] ⚙️ patched-fuzz · `attempt_002`
**output:** **quiet on patched build (no overfit signal)**

---
## [14] ⚙️ patched-fuzz · `attempt_003`
**output:** **FIRED — [oracle:mixed-array] metamorphic violation: expected foo for documented fixture**
- reproducing_input_file: /tmp/d4j/Lang_20_buggy/fuzz/attempt_003/crashes/crash-da39a3ee5e6b4b0d3255bfef95601890afd80709
