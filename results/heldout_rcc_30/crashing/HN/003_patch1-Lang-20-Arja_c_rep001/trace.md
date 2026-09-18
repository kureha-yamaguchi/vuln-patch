# Pipeline trace — Lang-20

**Code:** `b4b78d5146829e25837ef7e7f105b4ec3b686d1e`

**Patch label:** correct  *(the patch under analysis is a known-CORRECT fix — the pipeline is not told this)*

**Outcome:** candidate_experiment (buggy build only)

**Patch under analysis:**
```diff
--- /src/main/java/org/apache/commons/lang3/StringUtils.java
+++ /src/main/java/org/apache/commons/lang3/StringUtils.java
@@ -3295,7 +3295,7 @@
             return EMPTY;
         }
         
-        StringBuilder buf = new StringBuilder((array[startIndex] == null ? 16 : array[startIndex].toString().length()) + 1);
+        StringBuilder buf=new StringBuilder(256);
 
         for (int i = startIndex; i < endIndex; i++) {
             if (i > startIndex) {
@@ -3380,7 +3380,7 @@
             return EMPTY;
         }
 
-        StringBuilder buf = new StringBuilder((array[startIndex] == null ? 16 : array[startIndex].toString().length()) + separator.length());
+        StringBuilder buf=new StringBuilder(256);
 
         for (int i = startIndex; i < endIndex; i++) {
             if (i > startIndex) {
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
<details><summary>▸ output — FailureTest(test_class='org.apache.commons.lang3.StringUtilsTest', test_method='testJoin_ArrayChar', (~2,742 chars)</summary>

**output:** 
```json
[
  "FailureTest(test_class='org.apache.commons.lang3.StringUtilsTest', test_method='testJoin_ArrayChar', source_path='/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/Lang_20_buggy/src/test/java/org/apache/commons/lang3/StringUtilsTest.java', method_source='    public void testJoin_ArrayChar() {\\n        assertEquals(null, StringUtils.join((Object[]) null, \\',\\'));\\n        assertEquals(TEXT_LIST_CHAR, StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR));\\n        assertEquals(\"\", StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR));\\n        assertEquals(\";;foo\", StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR));\\n        assertEquals(\"foo;2\", StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR));\\n\\n        assertEquals(\"/\", StringUtils.join(MIXED_ARRAY_LIST, \\'/\\', 0, MIXED_ARRAY_LIST.length-1));\\n        assertEquals(\"foo\", StringUtils.join(MIXED_TYPE_LIST, \\'/\\', 0, 1));\\n        assertEquals(\"null\", StringUtils.join(NULL_TO_STRING_LIST,\\'/\\', 0, 1));\\n        assertEquals(\"foo/2\", StringUtils.join(MIXED_TYPE_LIST, \\'/\\', 0, 2));\\n        assertEquals(\"2\", StringUtils.join(MIXED_TYPE_LIST, \\'/\\', 1, 2));\\n        assertEquals(\"\", StringUtils.join(MIXED_TYPE_LIST, \\'/\\', 2, 1));\\n    }', exception_type='java.lang.NullPointerException', failure_message=None, support_source=None)",
  "FailureTest(test_class='org.apache.commons.lang3.StringUtilsTest', test_method='testJoin_Objectarray', source_path='/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/Lang_20_buggy/src/test/java/org/apache/commons/lang3/StringUtilsTest.java', method_source='    public void testJoin_Objectarray() {\\n//        assertEquals(null, StringUtils.join(null)); // generates warning\\n        assertEquals(null, StringUtils.join((Object[]) null)); // equivalent explicit cast\\n        // test additional varargs calls\\n        assertEquals(\"\", StringUtils.join()); // empty array\\n        assertEquals(\"\", StringUtils.join((Object) null)); // => new Object[]{null}\\n\\n        assertEquals(\"\", StringUtils.join(EMPTY_ARRAY_LIST));\\n        assertEquals(\"\", StringUtils.join(NULL_ARRAY_LIST));\\n        assertEquals(\"null\", StringUtils.join(NULL_TO_STRING_LIST));\\n        assertEquals(\"abc\", StringUtils.join(new String[] {\"a\", \"b\", \"c\"}));\\n        assertEquals(\"a\", StringUtils.join(new String[] {null, \"a\", \"\"}));\\n        assertEquals(\"foo\", StringUtils.join(MIXED_ARRAY_LIST));\\n        assertEquals(\"foo2\", StringUtils.join(MIXED_TYPE_LIST));\\n    }', exception_type='java.lang.NullPointerException', failure_message=None, support_source=None)"
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
<details><summary>▸ output — modified_files": [ (~23,971 chars)</summary>

**output:** 
```json
{
  "modified_files": [
    "src/main/java/org/apache/commons/lang3/StringUtils.java"
  ],
  "patch_text": "--- /src/main/java/org/apache/commons/lang3/StringUtils.java\n+++ /src/main/java/org/apache/commons/lang3/StringUtils.java\n@@ -3295,7 +3295,7 @@\n             return EMPTY;\n         }\n         \n-        StringBuilder buf = new StringBuilder((array[startIndex] == null ? 16 : array[startIndex].toString().length()) + 1);\n+        StringBuilder buf=new StringBuilder(256);\n \n         for (int i = startIndex; i < endIndex; i++) {\n             if (i > startIndex) {\n@@ -3380,7 +3380,7 @@\n             return EMPTY;\n         }\n \n-        StringBuilder buf = new StringBuilder((array[startIndex] == null ? 16 : array[startIndex].toString().length()) + separator.length());\n+        StringBuilder buf=new StringBuilder(256);\n \n         for (int i = startIndex; i < endIndex; i++) {\n             if (i > startIndex) {\n",
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
      "xref_names": [
        "[org.apache.commons.lang3.StringUtilsTest].testJoin_ArrayChar()"
      ],
      "reachable_edges": [
        [
          "[org.apache.commons.lang3.StringUtils].join(Object[],char,int,int)",
          "[StringBuilder].<init>(int)"
        ],
        [
          "[org.apache.commons.lang3.StringUtils].join(Object[],char,int,int)",
          "[StringBuilder].append(char)"
        ],
        [
          "[org.apache.commons.lang3.StringUtils].join(Object[],char,int,int)",
          "[StringBuilder].append()"
        ],
        [
          "[org.apache.commons.lang3.StringUtils].join(Object[],char,int,int)",
          "[StringBuilder].toString()"
        ]
      ],
      "reachable_depth": {
        "[org.apache.commons.lang3.StringUtils].join(Object[],char,int,int)": 0,
        "[StringBuilder].<init>(int)": 1,
        "[StringBuilder].append(char)": 1,
        "[StringBuilder].append()": 1,
        "[StringBuilder].toString()": 1
      },
      "reachable": [
        "[StringBuilder].<init>(int)",
        "[StringBuilder].append(char)",
        "[StringBuilder].append()",
        "[StringBuilder].toString()",
        "[org.apache.commons.lang3.StringUtils].length(CharSequence)",
        "[org.apache.commons.lang3.CharRange].toString()"
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
      "xref_names": [
        "[org.apache.commons.lang3.StringUtilsTest].testJoin_ArrayString()"
      ],
      "reachable_edges": [
        [
          "[org.apache.commons.lang3.StringUtils].join(Object[],String,int,int)",
          "separator.length()"
        ],
        [
          "[org.apache.commons.lang3.StringUtils].join(Object[],String,int,int)",
          "[StringBuilder].<init>(org.apache.commons.lang3.StringUtils)"
        ],
        [
          "[org.apache.commons.lang3.StringUtils].join(Object[],String,int,int)",
          "[StringBuilder].append(org.apache.commons.lang3.StringUtils)"
        ],
        [
          "[org.apache.commons.lang3.StringUtils].join(Object[],String,int,int)",
          "[StringBuilder].append()"
        ],
        [
          "[org.apache.commons.lang3.StringUtils].join(Object[],String,int,int)",
          "[StringBuilder].toString()"
        ]
      ],
      "reachable_depth": {
        "[org.apache.commons.lang3.StringUtils].join(Object[],String,int,int)": 0,
        "separator.length()": 1,
        "[StringBuilder].<init>(org.apache.commons.lang3.StringUtils)": 1,
        "[StringBuilder].append(org.apache.commons.lang3.StringUtils)": 1,
        "[StringBuilder].append()": 1,
        "[StringBuilder].toString()": 1
      },
      "reachable": [
        "separator.length()",
        "[StringBuilder].<init>(org.apache.commons.lang3.StringUtils)",
        "[StringBuilder].append(org.apache.commons.lang3.StringUtils)",
        "[StringBuilder].append()",
        "[StringBuilder].toString()",
        "[org.apache.commons.lang3.StringUtils].length(CharSequence)",
        "[org.apache.commons.lang3.CharRange].toString()"
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
    "CharRange.toString"
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
  ],
  "reachable_edges": [
    [
      "[org.apache.commons.lang3.StringUtils].join(Object[],char,int,int)",
      "[StringBuilder].<init>(int)"
    ],
    [
      "[org.apache.commons.lang3.StringUtils].join(Object[],char,int,int)",
      "[StringBuilder].append(char)"
    ],
    [
      "[org.apache.commons.lang3.StringUtils].join(Object[],char,int,int)",
      "[StringBuilder].append()"
    ],
    [
      "[org.apache.commons.lang3.StringUtils].join(Object[],char,int,int)",
      "[StringBuilder].toString()"
    ],
    [
      "[org.apache.commons.lang3.StringUtils].join(Object[],String,int,int)",
      "separator.length()"
    ],
    [
      "[org.apache.commons.lang3.StringUtils].join(Object[],String,int,int)",
      "[StringBuilder].<init>(org.apache.commons.lang3.StringUtils)"
    ],
    [
      "[org.apache.commons.lang3.StringUtils].join(Object[],String,int,int)",
      "[StringBuilder].append(org.apache.commons.lang3.StringUtils)"
    ],
    [
      "[org.apache.commons.lang3.StringUtils].join(Object[],String,int,int)",
      "[StringBuilder].append()"
    ],
    [
      "[org.apache.commons.lang3.StringUtils].join(Object[],String,int,int)",
      "[StringBuilder].toString()"
    ]
  ],
  "reachable_depth": {
    "[org.apache.commons.lang3.StringUtils].join(Object[],char,int,int)": 0,
    "[StringBuilder].<init>(int)": 1,
    "[StringBuilder].append(char)": 1,
    "[StringBuilder].append()": 1,
    "[StringBuilder].toString()": 1,
    "[org.apache.commons.lang3.StringUtils].join(Object[],String,int,int)": 0,
    "separator.length()": 1,
    "[StringBuilder].<init>(org.apache.commons.lang3.StringUtils)": 1,
    "[StringBuilder].append(org.apache.commons.lang3.StringUtils)": 1
  }
}
```

</details>

---
## [3] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~28,163 chars)</summary>

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
@@ -3295,7 +3295,7 @@
             return EMPTY;
         }
         
-        StringBuilder buf = new StringBuilder((array[startIndex] == null ? 16 : array[startIndex].toString().length()) + 1);
+        StringBuilder buf=new StringBuilder(256);
 
         for (int i = startIndex; i < endIndex; i++) {
             if (i > startIndex) {
@@ -3380,7 +3380,7 @@
             return EMPTY;
         }
 
-        StringBuilder buf = new StringBuilder((array[startIndex] == null ? 16 : array[startIndex].toString().length()) + separator.length());
+        StringBuilder buf=new StringBuilder(256);
 
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
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `join`, `join`. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
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
<details open><summary>▸ Output (~15,587 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final String[] ARRAY_LIST = { "foo", "bar", "baz" };
    private static final String[] EMPTY_ARRAY_LIST = {};
    private static final String[] NULL_ARRAY_LIST = { null };
    private static final Object[] NULL_TO_STRING_LIST = {
        new Object() {
            @Override
            public String toString() {
                return null;
            }
        }
    };
    private static final String[] MIXED_ARRAY_LIST = { null, "", "foo" };
    private static final Object[] MIXED_TYPE_LIST = { "foo", Long.valueOf(2L) };
    private static final char SEPARATOR_CHAR = ';';
    private static final String TEXT_LIST_CHAR = "foo;bar;baz";

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchors();

        int strategy = data.consumeInt(0, 2);
        if (strategy == 0) {
            exploreCharJoin(data);
        } else if (strategy == 1) {
            exploreStringJoin(data);
        } else {
            exploreObjectArrayJoin(data);
        }

        checkEmptyAgreement();
    }

    private static void runAnchors() {
        try {
            assertEquals(null, StringUtils.join((Object[]) null, ','), "anchor-null-array-char");
            assertEquals(TEXT_LIST_CHAR, StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR), "anchor-array-list-char");
            assertEquals("", StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR), "anchor-empty-array-char");
            assertEquals(";;foo", StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR), "anchor-mixed-array-char");
            assertEquals("foo;2", StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR), "anchor-mixed-type-char");

            assertEquals("/", StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1), "anchor-range-mixed-array-char");
            assertEquals("foo", StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1), "anchor-range-mixed-type-char-1");
            assertEquals("null", StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1), "anchor-range-null-to-string-char");
            assertEquals("foo/2", StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2), "anchor-range-mixed-type-char-2");
            assertEquals("2", StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2), "anchor-range-mixed-type-char-3");
            assertEquals("", StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1), "anchor-range-mixed-type-char-empty");

            assertEquals(null, StringUtils.join((Object[]) null), "anchor-null-array-object");
            assertEquals("", StringUtils.join(), "anchor-empty-varargs");
            assertEquals("", StringUtils.join((Object) null), "anchor-single-null-object");
            assertEquals("", StringUtils.join(EMPTY_ARRAY_LIST), "anchor-empty-array-object");
            assertEquals("", StringUtils.join(NULL_ARRAY_LIST), "anchor-null-array-element");
            assertEquals("null", StringUtils.join(NULL_TO_STRING_LIST), "anchor-null-to-string-object");
            assertEquals("abc", StringUtils.join(new String[] { "a", "b", "c" }), "anchor-abc-object");
            assertEquals("a", StringUtils.join(new String[] { null, "a", "" }), "anchor-null-a-empty-object");
            assertEquals("foo", StringUtils.join(MIXED_ARRAY_LIST), "anchor-mixed-array-object");
            assertEquals("foo2", StringUtils.join(MIXED_TYPE_LIST), "anchor-mixed-type-object");
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }
    }

    private static void exploreCharJoin(FuzzedDataProvider data) {
        Object[] array = buildFuzzArray(data, true);
        if (array == null || array.length == 0) {
            return;
        }
        int start = data.consumeInt(0, array.length);
        int end = data.consumeInt(start, array.length);
        char sep = (char) (data.consumeByte() & 0xff);

        try {
            String charJoin = StringUtils.join(array, sep, start, end);
            String stringJoin = StringUtils.join(array, String.valueOf(sep), start, end);

            /* Contract/oracle: the one-char String-separator overload and the char-separator overload
             * are documented as equivalent over the same slice. A throw-deleting or branch-skipping patch
             * could avoid the crash yet silently produce a different joined string in one overload. */
            if (!safeEquals(charJoin, stringJoin)) {
                throw new RuntimeException("[oracle:char-vs-string] metamorphic violation: equivalent one-char separator overloads disagree input="
                        + describe(array, start, end, String.valueOf(sep)) + " lhs=" + String.valueOf(charJoin)
                        + " rhs=" + String.valueOf(stringJoin));
            }

            String whole = StringUtils.join(array, sep);
            if (start == 0 && end == array.length && !safeEquals(whole, charJoin)) {
                throw new RuntimeException("[oracle:whole-vs-range-char] metamorphic violation: full-range char join must equal whole-array join input="
                        + describe(array, start, end, String.valueOf(sep)) + " lhs=" + String.valueOf(whole)
                        + " rhs=" + String.valueOf(charJoin));
            }

            if (containsNullToStringOnlyAtSlice(array, start, end) && end - start == 1) {
                if (!"null".equals(charJoin)) {
                    throw new RuntimeException("[oracle:null-to-string-char] metamorphic violation: single element whose toString returns null must join as \"null\" input="
                            + describe(array, start, end, String.valueOf(sep)) + " lhs=" + String.valueOf(charJoin)
                            + " rhs=null");
                }
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }
    }

    private static void exploreStringJoin(FuzzedDataProvider data) {
        Object[] array = buildFuzzArray(data, true);
        if (array == null || array.length == 0) {
            return;
        }
        int start = data.consumeInt(0, array.length);
        int end = data.consumeInt(start, array.length);
        String sep = data.consumeAsciiString(3);

        try {
            String joined = StringUtils.join(array, sep, start, end);
            String effectiveSep = (sep == null) ? "" : sep;

            if (effectiveSep.length() == 1) {
                String sibling = StringUtils.join(array, effectiveSep.charAt(0), start, end);
                /* Contract/oracle: with a one-character separator, the String and char range overloads
                 * should produce the same result for the same valid array slice. */
                if (!safeEquals(joined, sibling)) {
                    throw new RuntimeException("[oracle:string-vs-char] metamorphic violation: one-char String separator overload disagrees with char overload input="
                            + describe(array, start, end, effectiveSep) + " lhs=" + String.valueOf(joined)
                            + " rhs=" + String.valueOf(sibling));
                }
            }

            if (start == 0 && end == array.length && "".equals(effectiveSep)) {
                String objectJoin = StringUtils.join(array);
                if (!safeEquals(joined, objectJoin)) {
                    throw new RuntimeException("[oracle:string-emptysep-vs-object] metamorphic violation: empty String separator range over whole array must equal join(Object[]) input="
                            + describe(array, start, end, effectiveSep) + " lhs=" + String.valueOf(joined)
                            + " rhs=" + String.valueOf(objectJoin));
                }
            }

            if (containsNullToStringOnlyAtSlice(array, start, end) && end - start == 1) {
                if (!"null".equals(joined)) {
                    throw new RuntimeException("[oracle:null-to-string-string] metamorphic violation: single element whose toString returns null must join as \"null\" input="
                            + describe(array, start, end, effectiveSep) + " lhs=" + String.valueOf(joined)
                            + " rhs=null");
                }
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }
    }

    private static void exploreObjectArrayJoin(FuzzedDataProvider data) {
        Object[] array = buildFuzzArray(data, false);
        try {
            String joined = StringUtils.join(array);
            if (array != null) {
                String viaRange = StringUtils.join(array, "", 0, array.length);
                /* Contract/oracle: join(Object[]) is the whole-array empty-separator form, so it must
                 * agree with the explicit range/String-separator overload on the same array. */
                if (!safeEquals(joined, viaRange)) {
                    throw new RuntimeException("[oracle:object-vs-range] metamorphic violation: join(Object[]) must equal whole-array empty-separator range join input="
                            + describe(array, 0, array.length, "") + " lhs=" + String.valueOf(joined)
                            + " rhs=" + String.valueOf(viaRange));
                }
            }

            if (array != null && array.length == 1 && array[0] == NULL_TO_STRING_LIST[0] && !"null".equals(joined)) {
                throw new RuntimeException("[oracle:null-to-string-object] metamorphic violation: single element whose toString returns null must join as \"null\" input="
                        + describe(array, 0, array.length, "") + " lhs=" + String.valueOf(joined) + " rhs=null");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, array != null);
        }
    }

    private static void checkEmptyAgreement() {
        try {
            String emptyJoin1 = StringUtils.join(new Object[] { "x" }, '/', 1, 1);
            String emptyJoin2 = StringUtils.join(new Object[] { "x" }, "", 1, 1);

            /* Contract/oracle: both join range overloads return EMPTY when endIndex - startIndex <= 0.
             * The sibling methods below also expose the shared EMPTY constant for covered edge cases
             * (null/negative/empty) per their javadocs, so they must all agree on the same observable value. */
            String e1 = StringUtils.trimToEmpty(null);
            String e2 = StringUtils.stripToEmpty(null);
            String e3 = StringUtils.substring("", 1);
            String e4 = StringUtils.substring("", 0, 0);
            String e5 = StringUtils.left("x", -1);

            if (!(safeEquals(emptyJoin1, e1) && safeEquals(emptyJoin2, e1) && safeEquals(e1, e2)
                    && safeEquals(e2, e3) && safeEquals(e3, e4) && safeEquals(e4, e5))) {
                throw new RuntimeException("[oracle:empty-agreement] metamorphic violation: EMPTY-observing APIs disagree lhs="
                        + String.valueOf(emptyJoin1) + "," + String.valueOf(emptyJoin2) + "," + String.valueOf(e1)
                        + "," + String.valueOf(e2) + "," + String.valueOf(e3) + "," + String.valueOf(e4) + ","
                        + String.valueOf(e5));
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }
    }

    private static Object[] buildFuzzArray(FuzzedDataProvider data, boolean requireNonEmpty) {
        if (!requireNonEmpty && data.consumeBoolean()) {
            return null;
        }
        int min = requireNonEmpty ? 1 : 0;
        int len = data.consumeInt(min, 6);
        Object[] array = new Object[len];
        boolean injectedNullToString = false;
        for (int i = 0; i < len; i++) {
            int kind = data.consumeInt(0, 5);
            switch (kind) {
                case 0:
                    array[i] = null;
                    break;
                case 1:
                    array[i] = data.consumeString(12);
                    break;
                case 2:
                    array[i] = data.consumeAsciiString(12);
                    break;
                case 3:
                    array[i] = Long.valueOf(data.consumeInt(-1000, 1000));
                    break;
                case 4:
                    array[i] = Integer.valueOf(data.consumeInt(-1000, 1000));
                    break;
                default:
                    array[i] = NULL_TO_STRING_LIST[0];
                    injectedNullToString = true;
                    break;
            }
        }
        if (requireNonEmpty && !injectedNullToString && data.consumeBoolean()) {
            array[data.consumeInt(0, len - 1)] = NULL_TO_STRING_LIST[0];
        }
        return array;
    }

    private static boolean containsNullToStringOnlyAtSlice(Object[] array, int start, int end) {
        return array != null && end - start == 1 && start >= 0 && end <= array.length && array[start] == NULL_TO_STRING_LIST[0];
    }

    private static void assertEquals(String expected, String actual, String id) {
        if (!safeEquals(expected, actual)) {
            throw new RuntimeException("[oracle:" + id + "] metamorphic violation: expected=" + String.valueOf(expected)
                    + " actual=" + String.valueOf(actual));
        }
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static void handleRuntime(RuntimeException t, boolean validByConstruction) {
        if (isCleanRejection(t)) {
            return;
        }
        if (t instanceof RuntimeException && !(t instanceof NullPointerException) && isOracleViolation(t)) {
            throw t;
        }
        if (validByConstruction && isRootCause(t)) {
            throw t;
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isOracleViolation(Throwable t) {
        String msg = t.getMessage();
        return msg != null && msg.startsWith("[oracle:");
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] frames = t.getStackTrace();
        boolean seenStringJoin = false;
        for (int i = 0; i < frames.length; i++) {
            StackTraceElement f = frames[i];
            if ("org.apache.commons.lang3.StringUtils".equals(f.getClassName()) && "join".equals(f.getMethodName())) {
                seenStringJoin = true;
                break;
            }
        }
        return seenStringJoin;
    }

    private static String describe(Object[] array, int start, int end, String sep) {
        StringBuilder sb = new StringBuilder();
        sb.append("array=");
        if (array == null) {
            sb.append("null");
        } else {
            sb.append('[');
            for (int i = 0; i < array.length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                Object o = array[i];
                if (o == null) {
                    sb.append("null");
                } else if (o == NULL_TO_STRING_LIST[0]) {
                    sb.append("<toString:null>");
                } else {
                    sb.append(String.valueOf(o));
                }
            }
            sb.append(']');
        }
        sb.append(" start=").append(start).append(" end=").append(end).append(" sep=").append(String.valueOf(sep));
        return sb.toString();
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
<details open><summary>▸ Output (~13,497 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final Object nullToStringObject = new Object() {
            @Override
            public String toString() {
                return null;
            }
        };

        final String[] ARRAY_LIST = { "foo", "bar", "baz" };
        final String[] EMPTY_ARRAY_LIST = {};
        final String[] NULL_ARRAY_LIST = { null };
        final Object[] NULL_TO_STRING_LIST = { nullToStringObject };
        final String[] MIXED_ARRAY_LIST = { null, "", "foo" };
        final Object[] MIXED_TYPE_LIST = { "foo", Long.valueOf(2L) };
        final char SEPARATOR_CHAR = ';';
        final String TEXT_LIST_CHAR = "foo;bar;baz";

        // ANCHOR: exact trigger/setup from the project tests.
        try {
            String r1 = StringUtils.join((Object[]) null, ',');
            if (r1 != null) {
                throw new RuntimeException("[oracle:anchor-null-array-char] metamorphic violation: expected null for join((Object[])null, ',') input=null lhs=" + r1 + " rhs=null");
            }

            String r2 = StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR);
            if (!TEXT_LIST_CHAR.equals(r2)) {
                throw new RuntimeException("[oracle:anchor-array-char] metamorphic violation: expected TEXT_LIST_CHAR input=" + java.util.Arrays.toString(ARRAY_LIST) + " lhs=" + r2 + " rhs=" + TEXT_LIST_CHAR);
            }

            String r3 = StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR);
            if (!"".equals(r3)) {
                throw new RuntimeException("[oracle:anchor-empty-char] metamorphic violation: expected empty string input=[] lhs=" + r3 + " rhs=");
            }

            String r4 = StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR);
            if (!";;foo".equals(r4)) {
                throw new RuntimeException("[oracle:anchor-mixed-array-char] metamorphic violation: expected ';;foo' input=" + java.util.Arrays.toString(MIXED_ARRAY_LIST) + " lhs=" + r4 + " rhs=;;foo");
            }

            String r5 = StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR);
            if (!"foo;2".equals(r5)) {
                throw new RuntimeException("[oracle:anchor-mixed-type-char] metamorphic violation: expected 'foo;2' input=" + java.util.Arrays.toString(MIXED_TYPE_LIST) + " lhs=" + r5 + " rhs=foo;2");
            }

            String r6 = StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1);
            if (!"/".equals(r6)) {
                throw new RuntimeException("[oracle:anchor-range-char-1] metamorphic violation: expected '/' input=" + java.util.Arrays.toString(MIXED_ARRAY_LIST) + " lhs=" + r6 + " rhs=/");
            }

            String r7 = StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1);
            if (!"foo".equals(r7)) {
                throw new RuntimeException("[oracle:anchor-range-char-2] metamorphic violation: expected 'foo' input=" + java.util.Arrays.toString(MIXED_TYPE_LIST) + " lhs=" + r7 + " rhs=foo");
            }

            String r8 = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            if (!"null".equals(r8)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-char] metamorphic violation: expected 'null' for single element whose toString() returns null input=" + java.util.Arrays.toString(NULL_TO_STRING_LIST) + " lhs=" + r8 + " rhs=null");
            }

            String r9 = StringUtils.join((Object[]) null);
            if (r9 != null) {
                throw new RuntimeException("[oracle:anchor-null-array-object] metamorphic violation: expected null for join((Object[])null) input=null lhs=" + r9 + " rhs=null");
            }

            String r10 = StringUtils.join((Object) null);
            if (!"".equals(r10)) {
                throw new RuntimeException("[oracle:anchor-single-null-object] metamorphic violation: expected empty string for join((Object)null) lhs=" + r10 + " rhs=");
            }

            String r11 = StringUtils.join(EMPTY_ARRAY_LIST);
            if (!"".equals(r11)) {
                throw new RuntimeException("[oracle:anchor-empty-array-object] metamorphic violation: expected empty string lhs=" + r11 + " rhs=");
            }

            String r12 = StringUtils.join(NULL_ARRAY_LIST);
            if (!"".equals(r12)) {
                throw new RuntimeException("[oracle:anchor-null-array-element] metamorphic violation: expected empty string input=[null] lhs=" + r12 + " rhs=");
            }

            String r13 = StringUtils.join(NULL_TO_STRING_LIST);
            if (!"null".equals(r13)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-object] metamorphic violation: expected 'null' for join(NULL_TO_STRING_LIST) lhs=" + r13 + " rhs=null");
            }

            String r14 = StringUtils.join(new String[] { null, "a", "" });
            if (!"a".equals(r14)) {
                throw new RuntimeException("[oracle:anchor-null-leading-object] metamorphic violation: expected 'a' lhs=" + r14 + " rhs=a");
            }
        } catch (RuntimeException t) {
            String msg = t.getMessage();
            if (msg != null && msg.startsWith("[oracle:")) {
                throw t;
            }
            boolean throughJoin = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName()) && "join".equals(ste.getMethodName())) {
                    throughJoin = true;
                    break;
                }
            }
            if ((t instanceof IllegalArgumentException) || (t instanceof NumberFormatException)) {
                return;
            }
            if ((t instanceof NullPointerException) && throughJoin) {
                throw t;
            }
            return;
        }

        int len = data.consumeInt(1, 6);
        Object[] arr = new Object[len];
        for (int i = 0; i < len; i++) {
            int kind = data.consumeInt(0, 4);
            switch (kind) {
                case 0:
                    arr[i] = null;
                    break;
                case 1:
                    arr[i] = data.consumeAsciiString(12);
                    break;
                case 2:
                    arr[i] = Integer.valueOf(data.consumeInt(-1000, 1000));
                    break;
                case 3:
                    arr[i] = Long.valueOf(data.consumeInt(-1000, 1000));
                    break;
                default:
                    arr[i] = data.consumeBoolean() ? "" : data.consumeString(8);
                    break;
            }
        }

        int startIndex = data.consumeInt(0, len - 1);
        int endIndex = data.consumeInt(startIndex + 1, len);
        arr[startIndex] = nullToStringObject;

        char sepChar = (char) data.consumeInt(1, 126);
        String sepString = String.valueOf(sepChar);

        // EXPLORE root cause property: a valid, in-range startIndex whose first joined element is non-null
        // but its toString() returns null. Correct behavior is to produce the text "null" as shown by the tests,
        // not to dereference that null while sizing the StringBuilder.
        try {
            String lhs = StringUtils.join(arr, sepChar, startIndex, endIndex);
            String rhs = StringUtils.join(arr, sepString, startIndex, endIndex);

            // Contract/oracle: these overloads differ only in separator type; for a 1-char separator string,
            // equivalent inputs must yield identical output. A throw-deleting patch that silently drops the first
            // element or mishandles null-toString would break this sibling agreement.
            if (!lhs.equals(rhs)) {
                throw new RuntimeException("[oracle:char-vs-string-range] metamorphic violation: equivalent join overloads disagreed input=" + java.util.Arrays.toString(arr) + " start=" + startIndex + " end=" + endIndex + " lhs=" + lhs + " rhs=" + rhs);
            }

            // Contract/oracle from the regression tests: when the joined slice is exactly the single special
            // element, the result must be the literal text \"null\".
            if (endIndex == startIndex + 1) {
                if (!"null".equals(lhs)) {
                    throw new RuntimeException("[oracle:single-null-tostring-range] metamorphic violation: expected 'null' for single element whose toString() returns null input=" + java.util.Arrays.toString(arr) + " start=" + startIndex + " end=" + endIndex + " lhs=" + lhs + " rhs=null");
                }
            }
        } catch (RuntimeException t) {
            String msg = t.getMessage();
            if (msg != null && msg.startsWith("[oracle:")) {
                throw t;
            }
            boolean throughJoin = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName()) && "join".equals(ste.getMethodName())) {
                    throughJoin = true;
                    break;
                }
            }
            if ((t instanceof IllegalArgumentException) || (t instanceof NumberFormatException)) {
                return;
            }
            if ((t instanceof NullPointerException) && throughJoin) {
                throw t;
            }
            return;
        }

        try {
            Object[] full = new Object[len];
            System.arraycopy(arr, 0, full, 0, len);
            full[0] = nullToStringObject;

            String lhs = StringUtils.join(full);
            String rhs = StringUtils.join(full, "", 0, full.length);

            // Contract/oracle: joining an object array with no separator is equivalent to joining the same
            // full range with the empty-string separator. A patch that only avoids the crash by skipping work
            // would violate this observable equality.
            if (!lhs.equals(rhs)) {
                throw new RuntimeException("[oracle:object-vs-empty-sep] metamorphic violation: join(array) must equal join(array,\"\",0,len) input=" + java.util.Arrays.toString(full) + " lhs=" + lhs + " rhs=" + rhs);
            }

            if (full.length == 1 && !"null".equals(lhs)) {
                throw new RuntimeException("[oracle:single-null-tostring-object] metamorphic violation: expected 'null' for join(singleObjectWhoseToStringReturnsNull) lhs=" + lhs + " rhs=null");
            }
        } catch (RuntimeException t) {
            String msg = t.getMessage();
            if (msg != null && msg.startsWith("[oracle:")) {
                throw t;
            }
            boolean throughJoin = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName()) && "join".equals(ste.getMethodName())) {
                    throughJoin = true;
                    break;
                }
            }
            if ((t instanceof IllegalArgumentException) || (t instanceof NumberFormatException)) {
                return;
            }
            if ((t instanceof NullPointerException) && throughJoin) {
                throw t;
            }
            return;
        }

        try {
            String emptyFromJoinChar = StringUtils.join(arr, sepChar, 1, 1);
            String emptyFromJoinString = StringUtils.join(arr, sepString, 1, 1);
            String emptyFromTrim = StringUtils.trimToEmpty(null);
            String emptyFromStrip = StringUtils.stripToEmpty(null);
            String emptyFromSubstring = StringUtils.substring("", 0);
            String emptyFromLeft = StringUtils.left("abc", -1);

            // Shared-state agreement on StringUtils.EMPTY:
            // docs say all of these return an empty String for these inputs. If join's empty-range result stops
            // agreeing with the other EMPTY readers/writers, a patch could hide the original throw yet still
            // return the wrong sentinel.
            if (!emptyFromJoinChar.equals(emptyFromTrim)
                    || !emptyFromJoinString.equals(emptyFromStrip)
                    || !emptyFromJoinChar.equals(emptyFromSubstring)
                    || !emptyFromJoinString.equals(emptyFromLeft)) {
                throw new RuntimeException("[oracle:empty-sentinel-agreement] metamorphic violation: EMPTY-returning APIs disagreed lhs1=" + emptyFromJoinChar + " lhs2=" + emptyFromJoinString + " rhs1=" + emptyFromTrim + " rhs2=" + emptyFromStrip + " rhs3=" + emptyFromSubstring + " rhs4=" + emptyFromLeft);
            }
        } catch (RuntimeException t) {
            String msg = t.getMessage();
            if (msg != null && msg.startsWith("[oracle:")) {
                throw t;
            }
            boolean throughJoin = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName()) && "join".equals(ste.getMethodName())) {
                    throughJoin = true;
                    break;
                }
            }
            if ((t instanceof IllegalArgumentException) || (t instanceof NumberFormatException)) {
                return;
            }
            if ((t instanceof NullPointerException) && throughJoin) {
                throw t;
            }
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
<details open><summary>▸ Output (~15,451 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final Object nullToStringObject = new Object() {
            @Override
            public String toString() {
                return null;
            }
        };

        final Object[] NULL_TO_STRING_LIST = new Object[] { nullToStringObject };
        final String[] ARRAY_LIST = new String[] { "foo", "bar", "baz" };
        final String[] EMPTY_ARRAY_LIST = new String[] {};
        final String[] NULL_ARRAY_LIST = new String[] { null };
        final String[] MIXED_ARRAY_LIST = new String[] { null, "", "foo" };
        final Object[] MIXED_TYPE_LIST = new Object[] { "foo", Long.valueOf(2L) };
        final char SEPARATOR_CHAR = ';';
        final String TEXT_LIST_CHAR = "foo;bar;baz";

        try {
            String r0 = StringUtils.join((Object[]) null, ',');
            if (r0 != null) {
                throw new RuntimeException("[oracle:anchor-null-array-char] metamorphic violation: join((Object[])null, ',') must return null input=null lhs=" + String.valueOf(r0) + " rhs=null");
            }

            String r1 = StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR);
            if (!TEXT_LIST_CHAR.equals(r1)) {
                throw new RuntimeException("[oracle:anchor-array-char] metamorphic violation: fixed test contract input=ARRAY_LIST lhs=" + String.valueOf(r1) + " rhs=" + TEXT_LIST_CHAR);
            }

            String r2 = StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR);
            if (!"".equals(r2)) {
                throw new RuntimeException("[oracle:anchor-empty-array-char] metamorphic violation: empty array must join to empty string input=EMPTY_ARRAY_LIST lhs=" + String.valueOf(r2) + " rhs=");
            }

            String r3 = StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR);
            if (!";;foo".equals(r3)) {
                throw new RuntimeException("[oracle:anchor-mixed-array-char] metamorphic violation: fixed test contract input=MIXED_ARRAY_LIST lhs=" + String.valueOf(r3) + " rhs=;;foo");
            }

            String r4 = StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR);
            if (!"foo;2".equals(r4)) {
                throw new RuntimeException("[oracle:anchor-mixed-type-char] metamorphic violation: fixed test contract input=MIXED_TYPE_LIST lhs=" + String.valueOf(r4) + " rhs=foo;2");
            }

            String r5 = StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1);
            if (!"/".equals(r5)) {
                throw new RuntimeException("[oracle:anchor-range-mixed-array-char] metamorphic violation: fixed test contract input=MIXED_ARRAY_LIST lhs=" + String.valueOf(r5) + " rhs=/");
            }

            String r6 = StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1);
            if (!"foo".equals(r6)) {
                throw new RuntimeException("[oracle:anchor-range-mixed-type-char] metamorphic violation: fixed test contract input=MIXED_TYPE_LIST lhs=" + String.valueOf(r6) + " rhs=foo");
            }

            String r7 = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            if (!"null".equals(r7)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-char] metamorphic violation: fixed test contract input=NULL_TO_STRING_LIST lhs=" + String.valueOf(r7) + " rhs=null");
            }

            String r8 = StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2);
            if (!"foo/2".equals(r8)) {
                throw new RuntimeException("[oracle:anchor-range-mixed-type2-char] metamorphic violation: fixed test contract input=MIXED_TYPE_LIST lhs=" + String.valueOf(r8) + " rhs=foo/2");
            }

            String r9 = StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2);
            if (!"2".equals(r9)) {
                throw new RuntimeException("[oracle:anchor-range-mixed-type3-char] metamorphic violation: fixed test contract input=MIXED_TYPE_LIST lhs=" + String.valueOf(r9) + " rhs=2");
            }

            String r10 = StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1);
            if (!"".equals(r10)) {
                throw new RuntimeException("[oracle:anchor-range-empty-char] metamorphic violation: endIndex<=startIndex must return empty string input=MIXED_TYPE_LIST lhs=" + String.valueOf(r10) + " rhs=");
            }

            String r11 = StringUtils.join((Object[]) null);
            if (r11 != null) {
                throw new RuntimeException("[oracle:anchor-null-array-varargs] metamorphic violation: join((Object[])null) must return null input=null lhs=" + String.valueOf(r11) + " rhs=null");
            }

            String r12 = StringUtils.join();
            if (!"".equals(r12)) {
                throw new RuntimeException("[oracle:anchor-empty-varargs] metamorphic violation: join() must return empty string input=[] lhs=" + String.valueOf(r12) + " rhs=");
            }

            String r13 = StringUtils.join((Object) null);
            if (!"".equals(r13)) {
                throw new RuntimeException("[oracle:anchor-null-object-varargs] metamorphic violation: join((Object)null) must return empty string input=[null] lhs=" + String.valueOf(r13) + " rhs=");
            }

            String r14 = StringUtils.join(EMPTY_ARRAY_LIST);
            if (!"".equals(r14)) {
                throw new RuntimeException("[oracle:anchor-empty-array-varargs] metamorphic violation: empty array must join to empty string input=EMPTY_ARRAY_LIST lhs=" + String.valueOf(r14) + " rhs=");
            }

            String r15 = StringUtils.join(NULL_ARRAY_LIST);
            if (!"".equals(r15)) {
                throw new RuntimeException("[oracle:anchor-null-array-element-varargs] metamorphic violation: single null element joins to empty string input=NULL_ARRAY_LIST lhs=" + String.valueOf(r15) + " rhs=");
            }

            String r16 = StringUtils.join(NULL_TO_STRING_LIST);
            if (!"null".equals(r16)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-varargs] metamorphic violation: fixed test contract input=NULL_TO_STRING_LIST lhs=" + String.valueOf(r16) + " rhs=null");
            }

            String r17 = StringUtils.join(new String[] { "a", "b", "c" });
            if (!"abc".equals(r17)) {
                throw new RuntimeException("[oracle:anchor-abc-varargs] metamorphic violation: fixed test contract input=[a,b,c] lhs=" + String.valueOf(r17) + " rhs=abc");
            }

            String r18 = StringUtils.join(new String[] { null, "a", "" });
            if (!"a".equals(r18)) {
                throw new RuntimeException("[oracle:anchor-null-a-empty-varargs] metamorphic violation: fixed test contract input=[null,a,empty] lhs=" + String.valueOf(r18) + " rhs=a");
            }

            String r19 = StringUtils.join(MIXED_ARRAY_LIST);
            if (!"foo".equals(r19)) {
                throw new RuntimeException("[oracle:anchor-mixed-array-varargs] metamorphic violation: fixed test contract input=MIXED_ARRAY_LIST lhs=" + String.valueOf(r19) + " rhs=foo");
            }

            String r20 = StringUtils.join(MIXED_TYPE_LIST);
            if (!"foo2".equals(r20)) {
                throw new RuntimeException("[oracle:anchor-mixed-type-varargs] metamorphic violation: fixed test contract input=MIXED_TYPE_LIST lhs=" + String.valueOf(r20) + " rhs=foo2");
            }

            String r21 = StringUtils.join(NULL_TO_STRING_LIST, (String) null, 0, 1);
            if (!"null".equals(r21)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-stringsep] metamorphic violation: null separator is documented to behave as EMPTY input=NULL_TO_STRING_LIST lhs=" + String.valueOf(r21) + " rhs=null");
            }
        } catch (RuntimeException t) {
            boolean stackHasJoin = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName()) && "join".equals(ste.getMethodName())) {
                    stackHasJoin = true;
                    break;
                }
            }
            if (t instanceof NullPointerException && stackHasJoin) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            if (t.getClass().getName().contains("Invalid") || t.getClass().getName().contains("Validation")) {
                return;
            }
            throw t;
        }

        int len = data.consumeInt(1, 6);
        Object[] fuzzArray = new Object[len];
        int specialIndex = data.consumeInt(0, len - 1);
        for (int i = 0; i < len; i++) {
            if (i == specialIndex) {
                fuzzArray[i] = nullToStringObject;
            } else {
                int kind = data.consumeInt(0, 3);
                if (kind == 0) {
                    fuzzArray[i] = null;
                } else if (kind == 1) {
                    fuzzArray[i] = data.consumeString(16);
                } else if (kind == 2) {
                    fuzzArray[i] = Long.valueOf(data.consumeInt(-1000000, 1000000));
                } else {
                    fuzzArray[i] = data.consumeAsciiString(16);
                }
            }
        }

        int startIndex = data.consumeInt(0, len - 1);
        if (data.consumeBoolean()) {
            startIndex = specialIndex;
        }
        int endIndex = data.consumeInt(startIndex + 1, len);
        char sepChar = (char) (data.consumeByte() & 0xFF);
        String sepString = String.valueOf(sepChar);

        try {
            String charJoin = StringUtils.join(fuzzArray, sepChar, startIndex, endIndex);
            String stringJoin = StringUtils.join(fuzzArray, sepString, startIndex, endIndex);

            /* Contract/oracle:
             * The two overloads join(Object[], char, int, int) and join(Object[], String, int, int)
             * document the same joining behavior for a one-character separator on the same slice.
             * A throw-deleting or branch-skipping patch could avoid the crash but silently produce
             * different output in one overload; this sibling-agreement check catches that.
             */
            if (!StringUtils.equals(charJoin, stringJoin)) {
                throw new RuntimeException("[oracle:char-vs-string-overload] metamorphic violation: single-char String separator must agree with char separator input=start=" + startIndex + ",end=" + endIndex + ",sep=" + sepChar + " lhs=" + String.valueOf(charJoin) + " rhs=" + String.valueOf(stringJoin));
            }
        } catch (RuntimeException t) {
            boolean stackHasJoin = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName()) && "join".equals(ste.getMethodName())) {
                    stackHasJoin = true;
                    break;
                }
            }
            if (t instanceof NullPointerException && stackHasJoin) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            if (t.getClass().getName().contains("Invalid") || t.getClass().getName().contains("Validation")) {
                return;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        }

        try {
            String nullSepJoin = StringUtils.join(fuzzArray, (String) null, startIndex, endIndex);
            String emptySepJoin = StringUtils.join(fuzzArray, "", startIndex, endIndex);

            /* Contract/oracle:
             * The method body explicitly normalizes a null separator to EMPTY, so join(array, null, s, e)
             * must equal join(array, "", s, e) for every valid slice. A patch that merely suppresses the
             * exception but mishandles separator normalization would violate this observable post-condition.
             */
            if (!StringUtils.equals(nullSepJoin, emptySepJoin)) {
                throw new RuntimeException("[oracle:null-separator-equals-empty] metamorphic violation: null separator must behave as EMPTY input=start=" + startIndex + ",end=" + endIndex + " lhs=" + String.valueOf(nullSepJoin) + " rhs=" + String.valueOf(emptySepJoin));
            }
        } catch (RuntimeException t) {
            boolean stackHasJoin = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName()) && "join".equals(ste.getMethodName())) {
                    stackHasJoin = true;
                    break;
                }
            }
            if (t instanceof NullPointerException && stackHasJoin) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            if (t.getClass().getName().contains("Invalid") || t.getClass().getName().contains("Validation")) {
                return;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        }

        try {
            String emptyFromJoin = StringUtils.join(fuzzArray, sepChar, 1, 1);
            String emptyFromTrim = StringUtils.trimToEmpty(null);
            String emptyFromStrip = StringUtils.stripToEmpty(null);
            String emptyFromSubstring = StringUtils.substring("", 0);
            String emptyFromLeft = StringUtils.left("abc", -1);

            if (!StringUtils.equals(emptyFromJoin, emptyFromTrim)
                    || !StringUtils.equals(emptyFromJoin, emptyFromStrip)
                    || !StringUtils.equals(emptyFromJoin, emptyFromSubstring)
                    || !StringUtils.equals(emptyFromJoin, emptyFromLeft)) {
                throw new RuntimeException("[oracle:empty-shared-constant] metamorphic violation: APIs documented to return empty string must agree on EMPTY input=shared-empty lhs=" + String.valueOf(emptyFromJoin) + " rhs=" + String.valueOf(emptyFromTrim) + "/" + String.valueOf(emptyFromStrip) + "/" + String.valueOf(emptyFromSubstring) + "/" + String.valueOf(emptyFromLeft));
            }
        } catch (RuntimeException t) {
            boolean stackHasJoin = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName()) && "join".equals(ste.getMethodName())) {
                    stackHasJoin = true;
                    break;
                }
            }
            if (t instanceof NullPointerException && stackHasJoin) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            if (t.getClass().getName().contains("Invalid") || t.getClass().getName().contains("Validation")) {
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
<details open><summary>▸ Output (~10,999 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final String[] ARRAY_LIST = { "foo", "bar", "baz" };
    private static final String[] EMPTY_ARRAY_LIST = {};
    private static final String[] NULL_ARRAY_LIST = { null };
    private static final Object NULL_TO_STRING_OBJECT = new Object() {
        @Override
        public String toString() {
            return null;
        }
    };
    private static final Object[] NULL_TO_STRING_LIST = { NULL_TO_STRING_OBJECT };
    private static final String[] MIXED_ARRAY_LIST = { null, "", "foo" };
    private static final Object[] MIXED_TYPE_LIST = { "foo", Long.valueOf(2L) };
    private static final char SEPARATOR_CHAR = ';';
    private static final String TEXT_LIST_CHAR = "foo;bar;baz";

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchorChecks();
        emptyStateAgreementChecks();

        int len = data.consumeInt(1, 8);
        Object[] array = new Object[len];
        int startIndex = data.consumeInt(0, len - 1);
        int endIndex = data.consumeInt(startIndex + 1, len);
        array[startIndex] = NULL_TO_STRING_OBJECT;

        for (int i = 0; i < len; i++) {
            if (i == startIndex) {
                continue;
            }
            int kind = data.consumeInt(0, 4);
            switch (kind) {
                case 0:
                    array[i] = null;
                    break;
                case 1:
                    array[i] = data.consumeAsciiString(8);
                    break;
                case 2:
                    array[i] = Long.valueOf(data.consumeInt(-1000000, 1000000));
                    break;
                case 3:
                    array[i] = data.consumeString(8);
                    break;
                default:
                    array[i] = Boolean.valueOf(data.consumeBoolean());
                    break;
            }
        }

        char charSep = (char) data.consumeInt(1, 126);
        String stringSep;
        if (data.consumeBoolean()) {
            stringSep = null;
        } else {
            stringSep = data.consumeAsciiString(3);
        }

        try {
            String result = StringUtils.join(array, charSep, startIndex, endIndex);
            /* Contract/post-condition asserted from the documented and tested behavior:
             * a non-null first included element whose toString() returns null must still contribute
             * "null" rather than crashing or being silently skipped. A throw-deleting patch that
             * simply omits that element would violate this observable result.
             */
            if (result == null || !result.startsWith("null")) {
                throw new RuntimeException("[oracle:join-prefix-char] metamorphic violation: first included element with null toString must render as leading null inputStart=" + startIndex + " inputEnd=" + endIndex + " result=" + result);
            }
        } catch (RuntimeException t) {
            handleThrowable(t, true);
        }

        try {
            String result = StringUtils.join(array, stringSep, startIndex, endIndex);
            if (result == null || !result.startsWith("null")) {
                throw new RuntimeException("[oracle:join-prefix-string] metamorphic violation: first included element with null toString must render as leading null inputStart=" + startIndex + " inputEnd=" + endIndex + " sep=" + stringSep + " result=" + result);
            }
        } catch (RuntimeException t) {
            handleThrowable(t, true);
        }

        if (data.consumeBoolean()) {
            char eqSep = (char) data.consumeInt(1, 126);
            try {
                /* Sibling-agreement guarantee: these overloads differ only in separator type.
                 * For a one-character separator over the same valid slice, both must produce
                 * the same joined text.
                 */
                String lhs = StringUtils.join(array, eqSep, startIndex, endIndex);
                String rhs = StringUtils.join(array, String.valueOf(eqSep), startIndex, endIndex);
                if (lhs != null && rhs != null && !lhs.equals(rhs)) {
                    throw new RuntimeException("[oracle:join-overload-agree] metamorphic violation: char/string separator overloads disagree inputStart=" + startIndex + " inputEnd=" + endIndex + " sep=" + eqSep + " lhs=" + lhs + " rhs=" + rhs);
                }
            } catch (RuntimeException t) {
                handleThrowable(t, true);
            }
        }

        try {
            String text = StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR);
            if (!TEXT_LIST_CHAR.equals(text)) {
                throw new RuntimeException("[oracle:join-basic-char] metamorphic violation: known fixture changed lhs=" + text + " rhs=" + TEXT_LIST_CHAR);
            }
        } catch (RuntimeException t) {
            handleThrowable(t, false);
        }

        try {
            String text = StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2);
            if (!"foo/2".equals(text)) {
                throw new RuntimeException("[oracle:join-mixed-char] metamorphic violation: known fixture changed lhs=" + text + " rhs=foo/2");
            }
        } catch (RuntimeException t) {
            handleThrowable(t, false);
        }

        try {
            String text = StringUtils.join(MIXED_ARRAY_LIST);
            if (!"foo".equals(text)) {
                throw new RuntimeException("[oracle:join-varargs] metamorphic violation: known fixture changed lhs=" + text + " rhs=foo");
            }
        } catch (RuntimeException t) {
            handleThrowable(t, false);
        }
    }

    private static void anchorChecks() {
        try {
            String r = StringUtils.join((Object[]) null, ',');
            if (r != null) {
                throw new RuntimeException("[oracle:anchor-null-array-char] metamorphic violation: join((Object[])null, ',') must be null lhs=" + r + " rhs=null");
            }
        } catch (RuntimeException t) {
            handleThrowable(t, false);
        }

        try {
            String r = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            if (!"null".equals(r)) {
                throw new RuntimeException("[oracle:anchor-null-toString-char] metamorphic violation: exact regression fixture must produce null lhs=" + r + " rhs=null");
            }
        } catch (RuntimeException t) {
            handleThrowable(t, true);
        }

        try {
            String r = StringUtils.join((Object[]) null);
            if (r != null) {
                throw new RuntimeException("[oracle:anchor-null-array-varargs] metamorphic violation: join((Object[])null) must be null lhs=" + r + " rhs=null");
            }
        } catch (RuntimeException t) {
            handleThrowable(t, false);
        }

        try {
            String r = StringUtils.join((Object) null);
            if (!"".equals(r)) {
                throw new RuntimeException("[oracle:anchor-single-null-object] metamorphic violation: join((Object)null) must be empty lhs=" + r + " rhs=");
            }
        } catch (RuntimeException t) {
            handleThrowable(t, false);
        }

        try {
            String r = StringUtils.join(NULL_TO_STRING_LIST);
            if (!"null".equals(r)) {
                throw new RuntimeException("[oracle:anchor-null-toString-varargs] metamorphic violation: exact regression fixture must produce null lhs=" + r + " rhs=null");
            }
        } catch (RuntimeException t) {
            handleThrowable(t, true);
        }

        try {
            String r = StringUtils.join(NULL_TO_STRING_LIST, (String) null, 0, 1);
            if (!"null".equals(r)) {
                throw new RuntimeException("[oracle:anchor-null-toString-string] metamorphic violation: string-separator overload must produce null lhs=" + r + " rhs=null");
            }
        } catch (RuntimeException t) {
            handleThrowable(t, true);
        }

        try {
            String r = StringUtils.join(EMPTY_ARRAY_LIST);
            if (!"".equals(r)) {
                throw new RuntimeException("[oracle:anchor-empty-array] metamorphic violation: empty array must join to empty string lhs=" + r + " rhs=");
            }
        } catch (RuntimeException t) {
            handleThrowable(t, false);
        }

        try {
            String r = StringUtils.join(NULL_ARRAY_LIST);
            if (!"".equals(r)) {
                throw new RuntimeException("[oracle:anchor-null-element-array] metamorphic violation: single null element array must join to empty string lhs=" + r + " rhs=");
            }
        } catch (RuntimeException t) {
            handleThrowable(t, false);
        }
    }

    private static void emptyStateAgreementChecks() {
        try {
            String empty1 = StringUtils.trimToEmpty(null);
            String empty2 = StringUtils.stripToEmpty(null);
            String empty3 = StringUtils.substring("", 0);
            String empty4 = StringUtils.substring("abc", 2, 1);
            String empty5 = StringUtils.left("abc", -1);
            String empty6 = StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1);

            if (!"".equals(empty1) || !empty1.equals(empty2) || !empty1.equals(empty3) || !empty1.equals(empty4)
                    || !empty1.equals(empty5) || !empty1.equals(empty6)) {
                throw new RuntimeException("[oracle:empty-agree] metamorphic violation: EMPTY-backed APIs must agree e1=" + empty1 + " e2=" + empty2 + " e3=" + empty3 + " e4=" + empty4 + " e5=" + empty5 + " e6=" + empty6);
            }
        } catch (RuntimeException t) {
            handleThrowable(t, false);
        }
    }

    private static void handleThrowable(RuntimeException t, boolean validByConstructionForRootCause) {
        if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
            throw t;
        }
        if (isValidationException(t)) {
            return;
        }
        if (validByConstructionForRootCause && isRootCause(t)) {
            throw t;
        }
    }

    private static boolean isValidationException(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement ste = stack[i];
            if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName())
                    && "join".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
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
<details open><summary>▸ Output (~14,526 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final String[] ARRAY_LIST = { "foo", "bar", "baz" };
    private static final String[] EMPTY_ARRAY_LIST = {};
    private static final String[] NULL_ARRAY_LIST = { null };
    private static final Object NULL_TO_STRING = new Object() {
        @Override
        public String toString() {
            return null;
        }
    };
    private static final Object[] NULL_TO_STRING_LIST = { NULL_TO_STRING };
    private static final String[] MIXED_ARRAY_LIST = { null, "", "foo" };
    private static final Object[] MIXED_TYPE_LIST = { "foo", Long.valueOf(2L) };
    private static final char SEPARATOR_CHAR = ';';
    private static final String TEXT_LIST_CHAR = "foo;bar;baz";

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchorCalls();
        sharedEmptyChecks();
        explore(data);
    }

    private static void anchorCalls() {
        try {
            String r0 = StringUtils.join((Object[]) null, ',');
            if (r0 != null) {
                throw new RuntimeException("[oracle:anchor-char-null-array] metamorphic violation: expected null for null array input lhs=" + r0);
            }

            String r1 = StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR);
            if (!TEXT_LIST_CHAR.equals(r1)) {
                throw new RuntimeException("[oracle:anchor-char-basic] metamorphic violation: expected exact fixture result lhs=" + r1 + " rhs=" + TEXT_LIST_CHAR);
            }

            String r2 = StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR);
            if (!"".equals(r2)) {
                throw new RuntimeException("[oracle:anchor-char-empty] metamorphic violation: expected empty string for empty array lhs=" + r2);
            }

            String r3 = StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR);
            if (!";;foo".equals(r3)) {
                throw new RuntimeException("[oracle:anchor-char-mixed] metamorphic violation: expected exact fixture result lhs=" + r3 + " rhs=;;foo");
            }

            String r4 = StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR);
            if (!"foo;2".equals(r4)) {
                throw new RuntimeException("[oracle:anchor-char-mixed-type] metamorphic violation: expected exact fixture result lhs=" + r4 + " rhs=foo;2");
            }

            String r5 = StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1);
            if (!"/".equals(r5)) {
                throw new RuntimeException("[oracle:anchor-char-range1] metamorphic violation: expected exact fixture result lhs=" + r5 + " rhs=/");
            }

            String r6 = StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1);
            if (!"foo".equals(r6)) {
                throw new RuntimeException("[oracle:anchor-char-range2] metamorphic violation: expected exact fixture result lhs=" + r6 + " rhs=foo");
            }

            String r7 = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            if (!"null".equals(r7)) {
                throw new RuntimeException("[oracle:anchor-char-trigger] metamorphic violation: expected exact fixture result lhs=" + r7 + " rhs=null");
            }

            String r8 = StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2);
            if (!"foo/2".equals(r8)) {
                throw new RuntimeException("[oracle:anchor-char-range3] metamorphic violation: expected exact fixture result lhs=" + r8 + " rhs=foo/2");
            }

            String r9 = StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2);
            if (!"2".equals(r9)) {
                throw new RuntimeException("[oracle:anchor-char-range4] metamorphic violation: expected exact fixture result lhs=" + r9 + " rhs=2");
            }

            String r10 = StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1);
            if (!"".equals(r10)) {
                throw new RuntimeException("[oracle:anchor-char-range5] metamorphic violation: expected exact fixture result lhs=" + r10 + " rhs=");
            }

            String r11 = StringUtils.join((Object[]) null);
            if (r11 != null) {
                throw new RuntimeException("[oracle:anchor-obj-null-array] metamorphic violation: expected null for null array input lhs=" + r11);
            }

            String r12 = StringUtils.join();
            if (!"".equals(r12)) {
                throw new RuntimeException("[oracle:anchor-obj-empty-varargs] metamorphic violation: expected empty string lhs=" + r12);
            }

            String r13 = StringUtils.join((Object) null);
            if (!"".equals(r13)) {
                throw new RuntimeException("[oracle:anchor-obj-single-null] metamorphic violation: expected empty string lhs=" + r13);
            }

            String r14 = StringUtils.join(EMPTY_ARRAY_LIST);
            if (!"".equals(r14)) {
                throw new RuntimeException("[oracle:anchor-obj-empty-array] metamorphic violation: expected empty string lhs=" + r14);
            }

            String r15 = StringUtils.join(NULL_ARRAY_LIST);
            if (!"".equals(r15)) {
                throw new RuntimeException("[oracle:anchor-obj-null-element] metamorphic violation: expected empty string lhs=" + r15);
            }

            String r16 = StringUtils.join(NULL_TO_STRING_LIST);
            if (!"null".equals(r16)) {
                throw new RuntimeException("[oracle:anchor-obj-trigger] metamorphic violation: expected exact fixture result lhs=" + r16 + " rhs=null");
            }

            String r17 = StringUtils.join(new String[] { "a", "b", "c" });
            if (!"abc".equals(r17)) {
                throw new RuntimeException("[oracle:anchor-obj-abc] metamorphic violation: expected exact fixture result lhs=" + r17 + " rhs=abc");
            }

            String r18 = StringUtils.join(new String[] { null, "a", "" });
            if (!"a".equals(r18)) {
                throw new RuntimeException("[oracle:anchor-obj-a] metamorphic violation: expected exact fixture result lhs=" + r18 + " rhs=a");
            }

            String r19 = StringUtils.join(MIXED_ARRAY_LIST);
            if (!"foo".equals(r19)) {
                throw new RuntimeException("[oracle:anchor-obj-mixed] metamorphic violation: expected exact fixture result lhs=" + r19 + " rhs=foo");
            }

            String r20 = StringUtils.join(MIXED_TYPE_LIST);
            if (!"foo2".equals(r20)) {
                throw new RuntimeException("[oracle:anchor-obj-mixed-type] metamorphic violation: expected exact fixture result lhs=" + r20 + " rhs=foo2");
            }
        } catch (RuntimeException t) {
            handleThrowable(t, true);
        }
    }

    private static void sharedEmptyChecks() {
        try {
            String emptyFromCharJoin = StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1);
            String emptyFromStringJoin = StringUtils.join(MIXED_TYPE_LIST, "", 2, 1);
            String emptyFromTrim = StringUtils.trimToEmpty(null);
            String emptyFromStrip = StringUtils.stripToEmpty(null);
            String emptyFromSubstring = StringUtils.substring("", 0);
            String emptyFromSubstring2 = StringUtils.substring("", 0, 0);
            String emptyFromLeft = StringUtils.left("abc", -1);

            if (!emptyFromCharJoin.equals(emptyFromTrim)
                    || !emptyFromStringJoin.equals(emptyFromTrim)
                    || !emptyFromStrip.equals(emptyFromTrim)
                    || !emptyFromSubstring.equals(emptyFromTrim)
                    || !emptyFromSubstring2.equals(emptyFromTrim)
                    || !emptyFromLeft.equals(emptyFromTrim)) {
                throw new RuntimeException("[oracle:shared-empty] metamorphic violation: methods documented to return empty string disagree"
                        + " charJoin=" + quote(emptyFromCharJoin)
                        + " stringJoin=" + quote(emptyFromStringJoin)
                        + " trim=" + quote(emptyFromTrim)
                        + " strip=" + quote(emptyFromStrip)
                        + " substring=" + quote(emptyFromSubstring)
                        + " substring2=" + quote(emptyFromSubstring2)
                        + " left=" + quote(emptyFromLeft));
            }
        } catch (RuntimeException t) {
            handleThrowable(t, true);
        }
    }

    private static void explore(FuzzedDataProvider data) {
        int prefixLen = data.consumeInt(0, 4);
        int suffixLen = data.consumeInt(0, 4);
        int totalLen = prefixLen + 1 + suffixLen;
        Object[] array = new Object[totalLen];

        for (int i = 0; i < prefixLen; i++) {
            array[i] = makeRegularElement(data);
        }
        array[prefixLen] = NULL_TO_STRING;
        for (int i = prefixLen + 1; i < totalLen; i++) {
            array[i] = makeRegularElement(data);
        }

        int extraSelected = data.consumeInt(0, suffixLen);
        int startIndex = prefixLen;
        int endIndex = prefixLen + 1 + extraSelected;
        char sepChar = (char) (data.consumeByte() & 0xFF);
        String sepString = data.consumeBoolean() ? null : data.consumeString(4);

        try {
            String charJoin = StringUtils.join(array, sepChar, startIndex, endIndex);
            String oneCharStringJoin = StringUtils.join(array, String.valueOf(sepChar), startIndex, endIndex);

            /* Contract used: both overloads join the same selected slice with the supplied separator;
               when the String separator is exactly one character, it must agree with the char overload.
               A patch that only suppresses the crash by skipping the first element or returning a wrong
               default value would break this observable equivalence without throwing. */
            if (!charJoin.equals(oneCharStringJoin)) {
                throw new RuntimeException("[oracle:char-vs-string] metamorphic violation: join(array,char,s,e) must equal join(array,String.valueOf(char),s,e)"
                        + " start=" + startIndex + " end=" + endIndex
                        + " sepChar=" + (int) sepChar
                        + " lhs=" + quote(charJoin)
                        + " rhs=" + quote(oneCharStringJoin));
            }
        } catch (RuntimeException t) {
            handleThrowable(t, true);
        }

        try {
            String nullSepJoin = StringUtils.join(array, (String) null, startIndex, endIndex);
            String emptySepJoin = StringUtils.join(array, "", startIndex, endIndex);

            /* Contract used: StringUtils.join(Object[], String, int, int) explicitly states that
               a null separator is treated as EMPTY. Therefore join(..., null, ...) and
               join(..., "", ...) must always return the same result on accepted inputs. */
            if (!nullSepJoin.equals(emptySepJoin)) {
                throw new RuntimeException("[oracle:null-sep-equals-empty] metamorphic violation: null separator must behave like empty separator"
                        + " start=" + startIndex + " end=" + endIndex
                        + " lhs=" + quote(nullSepJoin)
                        + " rhs=" + quote(emptySepJoin));
            }

            String directJoin = StringUtils.join(array);
            if (startIndex == 0 && endIndex == array.length && !directJoin.equals(nullSepJoin)) {
                throw new RuntimeException("[oracle:varargs-vs-null-sep] metamorphic violation: join(array) must agree with join(array,null,0,len)"
                        + " lhs=" + quote(directJoin)
                        + " rhs=" + quote(nullSepJoin));
            }
        } catch (RuntimeException t) {
            handleThrowable(t, true);
        }

        try {
            Object[] noItemArray = new Object[] { "x" };
            String j1 = StringUtils.join(noItemArray, sepChar, 1, 1);
            String j2 = StringUtils.join(noItemArray, sepString, 1, 1);
            String e = StringUtils.trimToEmpty(null);
            if (!j1.equals(e) || !j2.equals(e)) {
                throw new RuntimeException("[oracle:empty-slice] metamorphic violation: non-positive item count must yield the documented empty string"
                        + " j1=" + quote(j1) + " j2=" + quote(j2) + " empty=" + quote(e));
            }
        } catch (RuntimeException t) {
            handleThrowable(t, true);
        }
    }

    private static Object makeRegularElement(FuzzedDataProvider data) {
        int choice = data.consumeInt(0, 3);
        switch (choice) {
            case 0:
                return null;
            case 1:
                return data.consumeAsciiString(8);
            case 2:
                return Integer.valueOf(data.consumeInt(-1000, 1000));
            default:
                return Long.valueOf(data.consumeInt(-1000, 1000));
        }
    }

    private static void handleThrowable(RuntimeException t, boolean validByConstruction) {
        if (isOracle(t)) {
            throw t;
        }
        if (validByConstruction && isRootCause(t)) {
            throw t;
        }
        if (isCleanRejection(t)) {
            return;
        }
    }

    private static boolean isOracle(RuntimeException t) {
        String msg = t.getMessage();
        return msg != null && msg.startsWith("[oracle:");
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

    private static String quote(String s) {
        return s == null ? "null" : "\"" + s + "\"";
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
<details open><summary>▸ Output (~10,620 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final Object nullToStringObject = new Object() {
            @Override
            public String toString() {
                return null;
            }
        };
        final Object[] NULL_TO_STRING_LIST = { nullToStringObject };
        final String[] EMPTY_ARRAY_LIST = {};
        final String[] NULL_ARRAY_LIST = { null };
        final String[] MIXED_ARRAY_LIST = { null, "", "foo" };
        final Object[] MIXED_TYPE_LIST = { "foo", Long.valueOf(2L) };

        try {
            if (StringUtils.join((Object[]) null, ';') != null) {
                throw new RuntimeException("[oracle:anchor-null-array-char] metamorphic violation: join((Object[])null,char) must return null");
            }
            if (StringUtils.join((Object[]) null) != null) {
                throw new RuntimeException("[oracle:anchor-null-array-varargs] metamorphic violation: join((Object[])null) must return null");
            }
            if (!"".equals(StringUtils.join((Object) null))) {
                throw new RuntimeException("[oracle:anchor-single-null] metamorphic violation: join((Object)null) must return empty string");
            }
            if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-empty-array] metamorphic violation: join(emptyArray) must return empty string");
            }
            if (!"".equals(StringUtils.join(NULL_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-null-element-array] metamorphic violation: join([null]) must return empty string");
            }
            if (!"foo".equals(StringUtils.join(MIXED_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-mixed-array] metamorphic violation: join([null,\"\",\"foo\"]) must return \"foo\"");
            }
            if (!"foo2".equals(StringUtils.join(MIXED_TYPE_LIST))) {
                throw new RuntimeException("[oracle:anchor-mixed-type] metamorphic violation: join([\"foo\",2]) must return \"foo2\"");
            }
            if (!"null".equals(StringUtils.join(NULL_TO_STRING_LIST))) {
                throw new RuntimeException("[oracle:anchor-varargs-nulltostring] metamorphic violation: join(NULL_TO_STRING_LIST) must return \"null\"");
            }
            if (!"null".equals(StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1))) {
                throw new RuntimeException("[oracle:anchor-char-nulltostring] metamorphic violation: join(NULL_TO_STRING_LIST,'/',0,1) must return \"null\"");
            }
            if (!"null".equals(StringUtils.join(NULL_TO_STRING_LIST, "/", 0, 1))) {
                throw new RuntimeException("[oracle:anchor-string-nulltostring] metamorphic violation: join(NULL_TO_STRING_LIST,\"/\",0,1) must return \"null\"");
            }
        } catch (Throwable t) {
            handleThrowable(t, true);
            return;
        }

        int len = data.consumeInt(1, 8);
        Object[] array = new Object[len];
        int startIndex = data.consumeInt(0, len - 1);
        int endIndex = data.consumeInt(startIndex + 1, len);
        array[startIndex] = nullToStringObject;

        for (int i = 0; i < len; i++) {
            if (i == startIndex) {
                continue;
            }
            int kind = data.consumeInt(0, 5);
            switch (kind) {
                case 0:
                    array[i] = null;
                    break;
                case 1:
                    array[i] = data.consumeAsciiString(16);
                    break;
                case 2:
                    array[i] = Integer.valueOf(data.consumeInt(-1000, 1000));
                    break;
                case 3:
                    array[i] = Long.valueOf(data.consumeInt(-1000, 1000));
                    break;
                case 4:
                    array[i] = Boolean.valueOf(data.consumeBoolean());
                    break;
                default:
                    array[i] = data.consumeString(16);
                    break;
            }
        }

        char separatorChar = (char) (data.consumeByte() & 0x7f);
        String separatorString = String.valueOf(separatorChar);
        String maybeNullSeparator = data.consumeBoolean() ? null : data.consumeAsciiString(4);

        try {
            String joinedChar = StringUtils.join(array, separatorChar, startIndex, endIndex);
            String joinedString = StringUtils.join(array, separatorString, startIndex, endIndex);

            /* Contract used: both overloads join(Object[], char, int, int) and
             * join(Object[], String, int, int) perform the same join over the same
             * slice, differing only in how the separator is provided. For a one-char
             * separator string, a correct implementation must produce the same result.
             * A throw-deleting or branch-skipping patch can silently return a wrong
             * string without throwing; this equality check catches that.
             */
            if (!safeEquals(joinedChar, joinedString)) {
                throw new RuntimeException("[oracle:char-vs-string] metamorphic violation: one-char String separator must agree with char separator input="
                        + describe(array, startIndex, endIndex, separatorString)
                        + " lhs=" + String.valueOf(joinedChar)
                        + " rhs=" + String.valueOf(joinedString));
            }
        } catch (Throwable t) {
            handleThrowable(t, true);
            return;
        }

        try {
            /* Contract used: String overload explicitly says if separator == null,
             * separator = EMPTY. Therefore join(array, null, s, e) and
             * join(array, "", s, e) must agree for every valid slice.
             */
            String lhs = StringUtils.join(array, maybeNullSeparator, startIndex, endIndex);
            String rhs = StringUtils.join(array, maybeNullSeparator == null ? "" : maybeNullSeparator, startIndex, endIndex);
            if (!safeEquals(lhs, rhs)) {
                throw new RuntimeException("[oracle:null-separator-empty] metamorphic violation: null separator must behave like empty separator input="
                        + describe(array, startIndex, endIndex, maybeNullSeparator)
                        + " lhs=" + String.valueOf(lhs)
                        + " rhs=" + String.valueOf(rhs));
            }
        } catch (Throwable t) {
            handleThrowable(t, true);
            return;
        }

        try {
            /* Shared-state agreement check on EMPTY: the join overloads return EMPTY
             * when endIndex - startIndex <= 0, and the sibling readers below also
             * report EMPTY for their documented empty-result cases. They must all
             * agree on the observable string value.
             */
            String emptyFromJoinChar = StringUtils.join(array, separatorChar, startIndex, startIndex);
            String emptyFromJoinString = StringUtils.join(array, separatorString, startIndex, startIndex);
            String emptyFromTrim = StringUtils.trimToEmpty(null);
            String emptyFromStrip = StringUtils.stripToEmpty(null);
            String emptyFromSubstring = StringUtils.substring("", 0);
            String emptyFromLeft = StringUtils.left("abc", -1);

            if (!(safeEquals(emptyFromJoinChar, emptyFromTrim)
                    && safeEquals(emptyFromJoinChar, emptyFromStrip)
                    && safeEquals(emptyFromJoinChar, emptyFromSubstring)
                    && safeEquals(emptyFromJoinChar, emptyFromLeft)
                    && safeEquals(emptyFromJoinChar, emptyFromJoinString))) {
                throw new RuntimeException("[oracle:empty-consistency] metamorphic violation: EMPTY readers/writers disagree"
                        + " joinChar=" + String.valueOf(emptyFromJoinChar)
                        + " joinString=" + String.valueOf(emptyFromJoinString)
                        + " trim=" + String.valueOf(emptyFromTrim)
                        + " strip=" + String.valueOf(emptyFromStrip)
                        + " substring=" + String.valueOf(emptyFromSubstring)
                        + " left=" + String.valueOf(emptyFromLeft));
            }
        } catch (Throwable t) {
            handleThrowable(t, true);
        }
    }

    private static void handleThrowable(Throwable t, boolean validByConstruction) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return;
        }
        if (validByConstruction && isRootCause(t)) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            if (t instanceof Error) {
                throw (Error) t;
            }
            throw new RuntimeException(t);
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement ste = stack[i];
            if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName())
                    && "join".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String describe(Object[] array, int startIndex, int endIndex, String sep) {
        StringBuilder sb = new StringBuilder();
        sb.append("len=").append(array == null ? -1 : array.length);
        sb.append(",start=").append(startIndex);
        sb.append(",end=").append(endIndex);
        sb.append(",sep=").append(String.valueOf(sep));
        sb.append(",array=[");
        if (array != null) {
            for (int i = 0; i < array.length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                Object o = array[i];
                if (o == null) {
                    sb.append("null");
                } else {
                    sb.append(o.getClass().getName());
                }
            }
        }
        sb.append(']');
        return sb.toString();
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
<details open><summary>▸ Output (~11,042 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final String[] ARRAY_LIST = { "foo", "bar", "baz" };
    private static final String[] EMPTY_ARRAY_LIST = {};
    private static final String[] NULL_ARRAY_LIST = { null };
    private static final Object[] NULL_TO_STRING_LIST = { new Object() {
        @Override
        public String toString() {
            return null;
        }
    } };
    private static final String[] MIXED_ARRAY_LIST = { null, "", "foo" };
    private static final Object[] MIXED_TYPE_LIST = { "foo", Long.valueOf(2L) };
    private static final char SEPARATOR_CHAR = ';';
    private static final String TEXT_LIST_CHAR = "foo;bar;baz";

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            if (StringUtils.join((Object[]) null, ',') != null) {
                throw new RuntimeException("[oracle:anchor-null-array-char] metamorphic violation: join((Object[])null, ',') must return null");
            }
            if (StringUtils.join((Object[]) null) != null) {
                throw new RuntimeException("[oracle:anchor-null-array-varargs] metamorphic violation: join((Object[])null) must return null");
            }
            if (!TEXT_LIST_CHAR.equals(StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-text-list] metamorphic violation: known fixture result disagrees");
            }
            if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-empty-array-char] metamorphic violation: empty array must join to empty string");
            }
            if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-empty-array-varargs] metamorphic violation: empty array must join to empty string");
            }
            if (!"".equals(StringUtils.join(NULL_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-null-element-varargs] metamorphic violation: single null element must join to empty string");
            }
            if (!"foo".equals(StringUtils.join(MIXED_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-mixed-array-varargs] metamorphic violation: known fixture result disagrees");
            }
            if (!"foo2".equals(StringUtils.join(MIXED_TYPE_LIST))) {
                throw new RuntimeException("[oracle:anchor-mixed-type-varargs] metamorphic violation: known fixture result disagrees");
            }
            if (!"/".equals(StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1))) {
                throw new RuntimeException("[oracle:anchor-slice-char] metamorphic violation: known fixture result disagrees");
            }
            if (!"foo".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1))) {
                throw new RuntimeException("[oracle:anchor-slice-char-foo] metamorphic violation: known fixture result disagrees");
            }
            if (!"foo/2".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2))) {
                throw new RuntimeException("[oracle:anchor-slice-char-foo2] metamorphic violation: known fixture result disagrees");
            }
            if (!"2".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2))) {
                throw new RuntimeException("[oracle:anchor-slice-char-2] metamorphic violation: known fixture result disagrees");
            }
            if (!"".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1))) {
                throw new RuntimeException("[oracle:anchor-slice-char-empty] metamorphic violation: endIndex <= startIndex must return empty string");
            }
        } catch (RuntimeException t) {
            if (isRootCauseNpeInJoin(t)) {
                throw t;
            }
            return;
        }

        try {
            String anchoredChar = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            if (!"null".equals(anchoredChar)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-char] metamorphic violation: single-element slice whose first element has toString()==null must produce \"null\" lhs=" + anchoredChar);
            }
        } catch (RuntimeException t) {
            if (isRootCauseNpeInJoin(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }

        try {
            String anchoredString = StringUtils.join(NULL_TO_STRING_LIST, "/", 0, 1);
            if (!"null".equals(anchoredString)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-string] metamorphic violation: single-element slice whose first element has toString()==null must produce \"null\" lhs=" + anchoredString);
            }
        } catch (RuntimeException t) {
            if (isRootCauseNpeInJoin(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }

        try {
            String anchoredVarargs = StringUtils.join(NULL_TO_STRING_LIST);
            if (!"null".equals(anchoredVarargs)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-varargs] metamorphic violation: join(NULL_TO_STRING_LIST) must produce \"null\" lhs=" + anchoredVarargs);
            }
        } catch (RuntimeException t) {
            if (isRootCauseNpeInJoin(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }

        try {
            String emptyByChar = StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1);
            String emptyByString = StringUtils.join(MIXED_TYPE_LIST, "/", 2, 1);
            String trimEmpty = StringUtils.trimToEmpty(null);
            String stripEmpty = StringUtils.stripToEmpty(null);
            String substringEmpty = StringUtils.substring("", 0);
            String leftEmpty = StringUtils.left("abc", -1);

            /* Contract asserted:
             * join(..., startIndex, endIndex) returns EMPTY when noOfItems <= 0.
             * trimToEmpty(null), stripToEmpty(null), substring("",0), and left("abc",-1)
             * also document empty-string returns. They all share StringUtils.EMPTY.
             * A throw-deleting or wrong-return patch could avoid the crash but return a
             * non-empty or inconsistent sentinel; this checks the observable shared state. */
            if (!"".equals(emptyByChar)
                    || !"".equals(emptyByString)
                    || !emptyByChar.equals(trimEmpty)
                    || !emptyByChar.equals(stripEmpty)
                    || !emptyByChar.equals(substringEmpty)
                    || !emptyByChar.equals(leftEmpty)) {
                throw new RuntimeException("[oracle:empty-shared-state] metamorphic violation: EMPTY readers/writers disagree char=" + emptyByChar + " string=" + emptyByString + " trim=" + trimEmpty + " strip=" + stripEmpty + " substring=" + substringEmpty + " left=" + leftEmpty);
            }
        } catch (RuntimeException t) {
            if (isRootCauseNpeInJoin(t)) {
                throw t;
            }
            return;
        }

        int totalLen = data.consumeInt(1, 6);
        Object[] arr = new Object[totalLen];
        int rootIndex = data.consumeInt(0, totalLen - 1);
        arr[rootIndex] = NULL_TO_STRING_LIST[0];
        for (int i = 0; i < totalLen; i++) {
            if (i == rootIndex) {
                continue;
            }
            int choice = data.consumeInt(0, 3);
            if (choice == 0) {
                arr[i] = null;
            } else if (choice == 1) {
                arr[i] = data.consumeAsciiString(8);
            } else if (choice == 2) {
                arr[i] = data.consumeString(8);
            } else {
                arr[i] = Long.valueOf(data.consumeInt(-1000000, 1000000));
            }
        }

        int start = rootIndex;
        int end = rootIndex + 1;
        char sepChar = (char) (data.consumeByte() & 0x7f);
        String sepString = data.consumeBoolean() ? null : data.consumeString(8);

        try {
            String r1 = StringUtils.join(arr, sepChar, start, end);
            if (!"null".equals(r1)) {
                throw new RuntimeException("[oracle:explore-char-single] metamorphic violation: valid single-element slice with toString()==null must produce \"null\" inputIndex=" + rootIndex + " lhs=" + r1);
            }
        } catch (RuntimeException t) {
            if (isRootCauseNpeInJoin(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }

        try {
            String r2 = StringUtils.join(arr, sepString, start, end);
            if (!"null".equals(r2)) {
                throw new RuntimeException("[oracle:explore-string-single] metamorphic violation: valid single-element slice with toString()==null must produce \"null\" inputIndex=" + rootIndex + " sep=" + sepString + " lhs=" + r2);
            }
        } catch (RuntimeException t) {
            if (isRootCauseNpeInJoin(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }

        try {
            /* Contract asserted:
             * In StringUtils.join(Object[], String, int, int), a null separator is replaced by EMPTY.
             * Therefore join(arr, null, s, e) and join(arr, "", s, e) must agree for every accepted input.
             * A patch that merely guards away the crash but mishandles separator/state would violate this. */
            String lhs = StringUtils.join(arr, null, start, end);
            String rhs = StringUtils.join(arr, "", start, end);
            if (!safeEquals(lhs, rhs)) {
                throw new RuntimeException("[oracle:null-separator-equals-empty] metamorphic violation: join(arr, null, s, e) must equal join(arr, \"\", s, e) inputIndex=" + rootIndex + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (RuntimeException t) {
            if (isRootCauseNpeInJoin(t)) {
                throw t;
            }
        }
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCauseNpeInJoin(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName())
                    && "join".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
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
<details open><summary>▸ Output (~11,982 chars)</summary>

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

    /*
     * Exact fixture from StringUtilsTest: an element whose toString() returns null.
     * This is the documented, real trigger used by the failing tests.
     */
    private static final Object[] NULL_TO_STRING_LIST = {
        new Object() {
            @Override
            public String toString() {
                return null;
            }
        }
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            runAnchors();
            runExplore(data);
            runSharedEmptyChecks();
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromJoin(t)) {
                throw t;
            }
        } catch (Error t) {
            if (isRootCauseFromJoin(t)) {
                throw t;
            }
        } catch (Throwable t) {
            return;
        }
    }

    private static void runAnchors() {
        if (StringUtils.join((Object[]) null, ',') != null) {
            throw new RuntimeException("[oracle:anchor-null-array-char] metamorphic violation: expected null for join((Object[])null, ',')");
        }
        if (StringUtils.join((Object[]) null) != null) {
            throw new RuntimeException("[oracle:anchor-null-array-varargs] metamorphic violation: expected null for join((Object[])null)");
        }
        if (!TEXT_LIST_CHAR.equals(StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR))) {
            throw new RuntimeException("[oracle:anchor-array-list] metamorphic violation: expected " + TEXT_LIST_CHAR + " got " + StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR));
        }
        if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR))) {
            throw new RuntimeException("[oracle:anchor-empty-array] metamorphic violation: expected empty result");
        }
        if (!";;foo".equals(StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR))) {
            throw new RuntimeException("[oracle:anchor-mixed-array] metamorphic violation: expected ;;foo got " + StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR));
        }
        if (!"foo;2".equals(StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR))) {
            throw new RuntimeException("[oracle:anchor-mixed-type] metamorphic violation: expected foo;2 got " + StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR));
        }
        if (!"/".equals(StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1))) {
            throw new RuntimeException("[oracle:anchor-range-char] metamorphic violation: expected /");
        }
        if (!"foo".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1))) {
            throw new RuntimeException("[oracle:anchor-range-char-foo] metamorphic violation: expected foo");
        }

        /*
         * Guaranteed buggy-version crash from the real failing test.
         * On a fixed build this must return "null".
         */
        String exact = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
        if (!"null".equals(exact)) {
            throw new RuntimeException("[oracle:anchor-null-tostring-char] metamorphic violation: expected null-string result input=NULL_TO_STRING_LIST lhs=" + exact + " rhs=null");
        }

        /*
         * Mirror the sibling failing test that reaches the real public API.
         * A throw-deleting patch could avoid the NPE but still return a wrong value; assert the documented output.
         */
        String viaVarargs = StringUtils.join(NULL_TO_STRING_LIST);
        if (!"null".equals(viaVarargs)) {
            throw new RuntimeException("[oracle:anchor-null-tostring-varargs] metamorphic violation: expected null-string result input=NULL_TO_STRING_LIST lhs=" + viaVarargs + " rhs=null");
        }

        /*
         * Call the second patched overload directly as well.
         * Equivalent separator forms must agree for any correct implementation.
         */
        String directStringSep = StringUtils.join(NULL_TO_STRING_LIST, "/", 0, 1);
        if (!"null".equals(directStringSep)) {
            throw new RuntimeException("[oracle:anchor-null-tostring-string] metamorphic violation: expected null-string result input=NULL_TO_STRING_LIST lhs=" + directStringSep + " rhs=null");
        }

        if (!"".equals(StringUtils.join((Object) null))) {
            throw new RuntimeException("[oracle:anchor-object-null] metamorphic violation: expected empty result for join((Object)null)");
        }
        if (!"".equals(StringUtils.join(NULL_ARRAY_LIST))) {
            throw new RuntimeException("[oracle:anchor-null-element-array] metamorphic violation: expected empty result for single null element");
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        int totalLen = data.consumeInt(1, 8);
        int startIndex = data.consumeInt(0, totalLen - 1);
        int endIndex = data.consumeInt(startIndex + 1, totalLen);
        int triggerIndex = data.consumeInt(startIndex, endIndex - 1);
        char sepChar = (char) (data.consumeByte() & 0xff);
        String sepString = data.consumeBoolean() ? String.valueOf(sepChar) : data.consumeAsciiString(3);
        if (sepString == null) {
            sepString = "";
        }

        Object[] array = new Object[totalLen];
        for (int i = 0; i < totalLen; i++) {
            switch (data.consumeInt(0, 4)) {
                case 0:
                    array[i] = data.consumeAsciiString(16);
                    break;
                case 1:
                    array[i] = Integer.valueOf(data.consumeInt(-1000000, 1000000));
                    break;
                case 2:
                    array[i] = Long.valueOf(data.consumeInt(-1000000, 1000000));
                    break;
                case 3:
                    array[i] = "";
                    break;
                default:
                    array[i] = null;
                    break;
            }
        }

        /*
         * Valid-by-construction trigger: the sliced range is non-empty and contains the exact
         * NULL_TO_STRING fixture inside the selected [startIndex, endIndex) range.
         * A correct implementation is obligated to accept it because the project tests expect "null".
         */
        array[triggerIndex] = NULL_TO_STRING_LIST[0];

        String resultChar = StringUtils.join(array, sepChar, startIndex, endIndex);
        String resultString = StringUtils.join(array, String.valueOf(sepChar), startIndex, endIndex);

        /*
         * Documented-equivalence oracle:
         * join(Object[], char, start, end) and join(Object[], String.valueOf(char), start, end)
         * should produce the same sequence because both insert the same separator between the same elements.
         * A patch that merely suppresses the throw or skips an append can make one side silently wrong.
         * If either call throws, this check does not apply; the caller catches and skips.
         */
        if (!safeEquals(resultChar, resultString)) {
            throw new RuntimeException("[oracle:char-vs-string-sep] metamorphic violation: equivalent separator overloads inputStart=" + startIndex + " inputEnd=" + endIndex + " lhs=" + resultChar + " rhs=" + resultString);
        }

        /*
         * Additional direct post-condition from the failing tests:
         * if the selected slice contains exactly the single NULL_TO_STRING fixture, result must be "null"
         * regardless of separator content, because no separator is inserted for a single-element range.
         */
        if (endIndex - startIndex == 1 && triggerIndex == startIndex) {
            if (!"null".equals(resultChar)) {
                throw new RuntimeException("[oracle:single-null-tostring-char] metamorphic violation: single element slice must stringify to null lhs=" + resultChar + " rhs=null");
            }
            if (!"null".equals(resultString)) {
                throw new RuntimeException("[oracle:single-null-tostring-string] metamorphic violation: single element slice must stringify to null lhs=" + resultString + " rhs=null");
            }
        }

        /*
         * Exercise additional real public APIs from the same family with exact supported fixtures.
         */
        StringUtils.join(array);
        StringUtils.join(array, sepChar);
        StringUtils.join(array, sepString);
        StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2);
        StringUtils.join(MIXED_TYPE_LIST, sepString, 1, 2);
        StringUtils.join(EMPTY_ARRAY_LIST, sepString, 0, 0);
    }

    private static void runSharedEmptyChecks() {
        /*
         * Shared-state agreement oracle over StringUtils.EMPTY:
         * the docs for trimToEmpty(null), stripToEmpty(null), substring("", 0), substring("", 0, 0),
         * left("x", -1), and join(..., start==end) all promise the empty string result.
         * If a patch corrupted the shared EMPTY constant or returned a different value, these readers would disagree.
         */
        String e1 = StringUtils.trimToEmpty(null);
        String e2 = StringUtils.stripToEmpty(null);
        String e3 = StringUtils.substring("", 0);
        String e4 = StringUtils.substring("", 0, 0);
        String e5 = StringUtils.left("x", -1);
        String e6 = StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1);
        String e7 = StringUtils.join(MIXED_TYPE_LIST, "", 2, 1);

        if (!allEqual(e1, e2, e3, e4, e5, e6, e7, "")) {
            throw new RuntimeException("[oracle:shared-empty] metamorphic violation: EMPTY readers disagree lhs="
                    + stringify(e1) + "," + stringify(e2) + "," + stringify(e3) + "," + stringify(e4) + ","
                    + stringify(e5) + "," + stringify(e6) + "," + stringify(e7) + " rhs=\"\"");
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCauseFromJoin(Throwable t) {
        if (!(t instanceof NullPointerException || t instanceof RuntimeException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        boolean sawJoin = false;
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                sawJoin = true;
                break;
            }
        }
        return sawJoin && (t instanceof NullPointerException || isOracleRuntime(t));
    }

    private static boolean isOracleRuntime(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static boolean allEqual(String... values) {
        if (values.length == 0) {
            return true;
        }
        String first = values[0];
        for (int i = 1; i < values.length; i++) {
            if (!safeEquals(first, values[i])) {
                return false;
            }
        }
        return true;
    }

    private static String stringify(String s) {
        return s == null ? "null" : "\"" + s + "\"";
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
<details open><summary>▸ Output (~8,798 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final String[] EMPTY_ARRAY_LIST = {};
    private static final String[] NULL_ARRAY_LIST = { null };
    private static final String[] MIXED_ARRAY_LIST = { null, "", "foo" };
    private static final Object[] MIXED_TYPE_LIST = { "foo", Long.valueOf(2L) };
    private static final Object[] NULL_TO_STRING_LIST = {
        new Object() {
            @Override
            public String toString() {
                return null;
            }
        }
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchorChecks();

        int len = data.consumeInt(1, 6);
        Object[] array = new Object[len];
        for (int i = 0; i < len; i++) {
            int kind = data.consumeInt(0, 4);
            switch (kind) {
                case 0:
                    array[i] = null;
                    break;
                case 1:
                    array[i] = data.consumeAsciiString(16);
                    break;
                case 2:
                    array[i] = Long.valueOf(data.consumeInt(-1000000, 1000000));
                    break;
                case 3:
                    array[i] = data.consumeString(16);
                    break;
                default:
                    array[i] = NULL_TO_STRING_LIST[0];
                    break;
            }
        }

        int startIndex = data.consumeInt(0, len - 1);
        int endIndex = data.consumeInt(startIndex + 1, len);
        array[startIndex] = NULL_TO_STRING_LIST[0];

        char sepChar = (char) (data.consumeByte() & 0x7f);
        String sepString = String.valueOf(sepChar);

        checkJoinPair(array, sepChar, sepString, startIndex, endIndex);

        if (startIndex == 0) {
            checkJoinNoSeparator(array);
        }

        int benignLen = data.consumeInt(1, 6);
        Object[] benign = new Object[benignLen];
        for (int i = 0; i < benignLen; i++) {
            int kind = data.consumeInt(0, 2);
            benign[i] = kind == 0 ? null : (kind == 1 ? data.consumeAsciiString(12) : Long.valueOf(data.consumeInt(-999, 999)));
        }
        int benignStart = data.consumeInt(0, benignLen - 1);
        int benignEnd = data.consumeInt(benignStart + 1, benignLen);
        checkJoinPair(benign, sepChar, sepString, benignStart, benignEnd);

        checkEmptyCoupling();
        touchAdditionalRealInputs();
    }

    private static void anchorChecks() {
        if (StringUtils.join((Object[]) null, ',') != null) {
            throw new RuntimeException("[oracle:anchor-null-char] metamorphic violation: join((Object[]) null, ',') must return null");
        }
        if (StringUtils.join((Object[]) null) != null) {
            throw new RuntimeException("[oracle:anchor-null-varargs] metamorphic violation: join((Object[]) null) must return null");
        }

        try {
            String charJoin = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            if (!"null".equals(charJoin)) {
                throw new RuntimeException("[oracle:anchor-char] metamorphic violation: single-element join with toString()==null must yield \"null\" input=/ lhs=" + charJoin + " rhs=null");
            }
        } catch (RuntimeException t) {
            handleThrowable(t, true);
        }

        try {
            String plainJoin = StringUtils.join(NULL_TO_STRING_LIST);
            if (!"null".equals(plainJoin)) {
                throw new RuntimeException("[oracle:anchor-varargs] metamorphic violation: join(NULL_TO_STRING_LIST) must yield \"null\" lhs=" + plainJoin + " rhs=null");
            }
        } catch (RuntimeException t) {
            handleThrowable(t, true);
        }

        try {
            String stringJoin = StringUtils.join(NULL_TO_STRING_LIST, "/", 0, 1);
            if (!"null".equals(stringJoin)) {
                throw new RuntimeException("[oracle:anchor-string] metamorphic violation: single-element join with string separator and toString()==null must yield \"null\" lhs=" + stringJoin + " rhs=null");
            }
        } catch (RuntimeException t) {
            handleThrowable(t, true);
        }
    }

    private static void checkJoinPair(Object[] array, char sepChar, String sepString, int startIndex, int endIndex) {
        String lhs;
        try {
            lhs = StringUtils.join(array, sepChar, startIndex, endIndex);
        } catch (RuntimeException t) {
            handleThrowable(t, true);
            return;
        }

        String rhs;
        try {
            rhs = StringUtils.join(array, sepString, startIndex, endIndex);
        } catch (RuntimeException t) {
            handleThrowable(t, true);
            return;
        }

        /* Contract asserted:
           join(Object[], char, ...) and join(Object[], String, ...) are same-name overloads with the same join semantics.
           For an equivalent one-character separator, both public APIs must produce the same text on the same valid slice.
           A "fix" that merely suppresses the exception or skips appending data would violate this observable agreement. */
        if (!StringUtils.equals(lhs, rhs)) {
            throw new RuntimeException(
                "[oracle:join-overload-agreement] metamorphic violation: char and string separator overloads disagree inputStart="
                    + startIndex + " inputEnd=" + endIndex + " lhs=" + lhs + " rhs=" + rhs);
        }
    }

    private static void checkJoinNoSeparator(Object[] array) {
        String lhs;
        try {
            lhs = StringUtils.join(array);
        } catch (RuntimeException t) {
            handleThrowable(t, true);
            return;
        }

        String rhs;
        try {
            rhs = StringUtils.join(array, "", 0, array.length);
        } catch (RuntimeException t) {
            handleThrowable(t, true);
            return;
        }

        /* Contract asserted:
           join(Object...) is the no-separator variant of the same public API family and must agree with the explicit empty-string
           separator form over the full valid range. A patch that only avoids the crash by dropping the first element would break this. */
        if (!StringUtils.equals(lhs, rhs)) {
            throw new RuntimeException(
                "[oracle:join-empty-separator] metamorphic violation: varargs join disagrees with explicit empty separator lhs="
                    + lhs + " rhs=" + rhs);
        }
    }

    private static void checkEmptyCoupling() {
        String emptyFromJoinChar = StringUtils.join(EMPTY_ARRAY_LIST, ',', 0, 0);
        String emptyFromJoinString = StringUtils.join(EMPTY_ARRAY_LIST, "", 0, 0);
        String emptyFromTrim = StringUtils.trimToEmpty(null);
        String emptyFromStrip = StringUtils.stripToEmpty(null);
        String emptyFromSubstring = StringUtils.substring("", 0);
        String emptyFromLeft = StringUtils.left("abc", -1);

        if (!StringUtils.equals(emptyFromJoinChar, emptyFromTrim)
                || !StringUtils.equals(emptyFromJoinString, emptyFromStrip)
                || !StringUtils.equals(emptyFromJoinChar, emptyFromSubstring)
                || !StringUtils.equals(emptyFromJoinString, emptyFromLeft)) {
            throw new RuntimeException(
                "[oracle:empty-coupling] metamorphic violation: EMPTY-backed APIs disagree lhs1="
                    + emptyFromJoinChar + " lhs2=" + emptyFromJoinString + " rhs1=" + emptyFromTrim
                    + " rhs2=" + emptyFromStrip + " rhs3=" + emptyFromSubstring + " rhs4=" + emptyFromLeft);
        }
    }

    private static void touchAdditionalRealInputs() {
        StringUtils.join(NULL_ARRAY_LIST);
        StringUtils.join(MIXED_ARRAY_LIST, ';', 0, MIXED_ARRAY_LIST.length);
        StringUtils.join(MIXED_TYPE_LIST, ";", 0, MIXED_TYPE_LIST.length);
    }

    private static void handleThrowable(RuntimeException t, boolean validByConstruction) {
        if (isCleanRejection(t)) {
            return;
        }
        if (validByConstruction && isRootCause(t)) {
            throw t;
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
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
        }
        return false;
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
<details open><summary>▸ Output (~15,260 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final Object nullToString = new Object() {
            @Override
            public String toString() {
                return null;
            }
        };

        final String[] ARRAY_LIST = { "foo", "bar", "baz" };
        final String[] EMPTY_ARRAY_LIST = {};
        final String[] NULL_ARRAY_LIST = { null };
        final Object[] NULL_TO_STRING_LIST = { nullToString };
        final String[] MIXED_ARRAY_LIST = { null, "", "foo" };
        final Object[] MIXED_TYPE_LIST = { "foo", Long.valueOf(2L) };
        final char SEPARATOR_CHAR = ';';
        final String TEXT_LIST_CHAR = "foo;bar;baz";

        if (StringUtils.join((Object[]) null, ',') != null) {
            throw new RuntimeException("[oracle:null-array] metamorphic violation: join((Object[]) null, ',') must return null");
        }
        if (!TEXT_LIST_CHAR.equals(StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR))) {
            throw new RuntimeException("[oracle:fixture-char] metamorphic violation: known fixture mismatch input=ARRAY_LIST lhs="
                    + StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR) + " rhs=" + TEXT_LIST_CHAR);
        }
        if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR))) {
            throw new RuntimeException("[oracle:fixture-empty-char] metamorphic violation: empty array must join to empty string");
        }
        if (!";;foo".equals(StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR))) {
            throw new RuntimeException("[oracle:fixture-mixed-char] metamorphic violation: known fixture mismatch lhs="
                    + StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR) + " rhs=;;foo");
        }
        if (!"foo;2".equals(StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR))) {
            throw new RuntimeException("[oracle:fixture-mixed-type-char] metamorphic violation: known fixture mismatch lhs="
                    + StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR) + " rhs=foo;2");
        }
        if (!"/".equals(StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1))) {
            throw new RuntimeException("[oracle:fixture-range-char-1] metamorphic violation: known fixture mismatch");
        }
        if (!"foo".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1))) {
            throw new RuntimeException("[oracle:fixture-range-char-2] metamorphic violation: known fixture mismatch");
        }
        if (!"foo/2".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2))) {
            throw new RuntimeException("[oracle:fixture-range-char-3] metamorphic violation: known fixture mismatch");
        }
        if (!"2".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2))) {
            throw new RuntimeException("[oracle:fixture-range-char-4] metamorphic violation: known fixture mismatch");
        }
        if (!"".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1))) {
            throw new RuntimeException("[oracle:fixture-range-char-5] metamorphic violation: known fixture mismatch");
        }
        if (StringUtils.join((Object[]) null) != null) {
            throw new RuntimeException("[oracle:null-array-varargs] metamorphic violation: join((Object[]) null) must return null");
        }
        if (!"".equals(StringUtils.join())) {
            throw new RuntimeException("[oracle:empty-varargs] metamorphic violation: join() must return empty string");
        }
        if (!"".equals(StringUtils.join((Object) null))) {
            throw new RuntimeException("[oracle:null-singleton-varargs] metamorphic violation: join((Object) null) must return empty string");
        }
        if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST))) {
            throw new RuntimeException("[oracle:empty-array-varargs] metamorphic violation: empty array must join to empty string");
        }
        if (!"".equals(StringUtils.join(NULL_ARRAY_LIST))) {
            throw new RuntimeException("[oracle:null-array-element-varargs] metamorphic violation: single null element must join to empty string");
        }
        if (!"abc".equals(StringUtils.join(new String[] { "a", "b", "c" }))) {
            throw new RuntimeException("[oracle:abc-varargs] metamorphic violation: known fixture mismatch");
        }
        if (!"a".equals(StringUtils.join(new String[] { null, "a", "" }))) {
            throw new RuntimeException("[oracle:a-varargs] metamorphic violation: known fixture mismatch");
        }
        if (!"foo".equals(StringUtils.join(MIXED_ARRAY_LIST))) {
            throw new RuntimeException("[oracle:foo-varargs] metamorphic violation: known fixture mismatch");
        }
        if (!"foo2".equals(StringUtils.join(MIXED_TYPE_LIST))) {
            throw new RuntimeException("[oracle:foo2-varargs] metamorphic violation: known fixture mismatch");
        }

        {
            try {
                String anchored = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
                if (!"null".equals(anchored)) {
                    throw new RuntimeException("[oracle:anchor-char] metamorphic violation: documented fixture from StringUtilsTest must produce \"null\" input=NULL_TO_STRING_LIST lhs="
                            + anchored + " rhs=null");
                }
            } catch (RuntimeException t) {
                boolean root = t instanceof NullPointerException;
                boolean inJoin = false;
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                        inJoin = true;
                        break;
                    }
                }
                if (root && inJoin) {
                    throw t;
                }
            }
        }

        {
            try {
                String anchored = StringUtils.join(NULL_TO_STRING_LIST, (String) null, 0, 1);
                if (!"null".equals(anchored)) {
                    throw new RuntimeException("[oracle:anchor-string] metamorphic violation: valid input with first element whose toString() returns null must still join as StringBuilder.append(Object) would, i.e. \"null\"; a throw-deleting patch that skips/changes append would break this input=NULL_TO_STRING_LIST lhs="
                            + anchored + " rhs=null");
                }
            } catch (RuntimeException t) {
                boolean cleanRejection = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
                if (cleanRejection) {
                    return;
                }
                boolean root = t instanceof NullPointerException;
                boolean inJoin = false;
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                        inJoin = true;
                        break;
                    }
                }
                if (root && inJoin) {
                    throw t;
                }
            }
        }

        {
            try {
                String anchored = StringUtils.join(NULL_TO_STRING_LIST);
                if (!"null".equals(anchored)) {
                    throw new RuntimeException("[oracle:anchor-varargs] metamorphic violation: fixture from StringUtilsTest must produce \"null\" input=NULL_TO_STRING_LIST lhs="
                            + anchored + " rhs=null");
                }
            } catch (RuntimeException t) {
                boolean root = t instanceof NullPointerException;
                boolean inJoin = false;
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                        inJoin = true;
                        break;
                    }
                }
                if (root && inJoin) {
                    throw t;
                }
            }
        }

        {
            String emptyFromRange = StringUtils.join(new Object[] { "x" }, '/', 1, 1);
            String emptyFromTrim = StringUtils.trimToEmpty(null);
            String emptyFromStrip = StringUtils.stripToEmpty(null);
            String emptyFromSubstring = StringUtils.substring("", 0);
            String emptyFromLeft = StringUtils.left("x", -1);
            if (!(emptyFromRange.equals(emptyFromTrim)
                    && emptyFromTrim.equals(emptyFromStrip)
                    && emptyFromStrip.equals(emptyFromSubstring)
                    && emptyFromSubstring.equals(emptyFromLeft))) {
                throw new RuntimeException("[oracle:empty-shared] metamorphic violation: members sharing EMPTY must agree lhs="
                        + emptyFromRange + "," + emptyFromTrim + "," + emptyFromStrip + "," + emptyFromSubstring + "," + emptyFromLeft);
            }
        }

        int len = data.consumeInt(1, 8);
        Object[] arr = new Object[len];
        int specialPos = data.consumeInt(0, len - 1);
        for (int i = 0; i < len; i++) {
            if (i == specialPos) {
                arr[i] = nullToString;
            } else if (data.consumeBoolean()) {
                arr[i] = null;
            } else if (data.consumeBoolean()) {
                arr[i] = Long.valueOf(data.consumeInt(-1000000, 1000000));
            } else {
                arr[i] = data.consumeString(16);
            }
        }

        int startIndex = data.consumeInt(0, specialPos);
        int endIndex = data.consumeInt(specialPos + 1, len);
        char sepChar = (char) (data.consumeByte() & 0xff);
        String sepString;
        if (data.consumeBoolean()) {
            sepString = String.valueOf(sepChar);
        } else {
            sepString = data.consumeAsciiString(4);
        }

        {
            String lhs;
            String rhs;
            try {
                lhs = StringUtils.join(arr, sepChar, startIndex, endIndex);
            } catch (RuntimeException t) {
                boolean cleanRejection = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
                if (cleanRejection) {
                    return;
                }
                boolean root = t instanceof NullPointerException;
                boolean inJoin = false;
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                        inJoin = true;
                        break;
                    }
                }
                if (root && inJoin) {
                    throw t;
                }
                return;
            }
            try {
                rhs = StringUtils.join(arr, String.valueOf(sepChar), startIndex, endIndex);
            } catch (RuntimeException t) {
                boolean cleanRejection = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
                if (cleanRejection) {
                    return;
                }
                boolean root = t instanceof NullPointerException;
                boolean inJoin = false;
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                        inJoin = true;
                        break;
                    }
                }
                if (root && inJoin) {
                    throw t;
                }
                return;
            }
            if (!lhs.equals(rhs)) {
                throw new RuntimeException("[oracle:char-string-overload] metamorphic violation: equivalent one-character separators must agree inputStart="
                        + startIndex + " inputEnd=" + endIndex + " lhs=" + lhs + " rhs=" + rhs);
            }
        }

        {
            String lhs;
            String rhs;
            try {
                lhs = StringUtils.join(arr);
            } catch (RuntimeException t) {
                boolean root = t instanceof NullPointerException;
                boolean inJoin = false;
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                        inJoin = true;
                        break;
                    }
                }
                if (root && inJoin) {
                    throw t;
                }
                return;
            }
            try {
                rhs = StringUtils.join(arr, "", 0, arr.length);
            } catch (RuntimeException t) {
                boolean cleanRejection = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
                if (cleanRejection) {
                    return;
                }
                boolean root = t instanceof NullPointerException;
                boolean inJoin = false;
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                        inJoin = true;
                        break;
                    }
                }
                if (root && inJoin) {
                    throw t;
                }
                return;
            }
            if (!lhs.equals(rhs)) {
                throw new RuntimeException("[oracle:varargs-range-overload] metamorphic violation: join(array) and join(array, \"\", 0, array.length) must agree lhs="
                        + lhs + " rhs=" + rhs);
            }
        }

        {
            try {
                String lhs = StringUtils.join(arr, sepString, startIndex, endIndex);
                String rhs = StringUtils.join(arr, sepString, startIndex, endIndex);
                if (!lhs.equals(rhs)) {
                    throw new RuntimeException("[oracle:idempotent-call] metamorphic violation: repeated pure calls must agree lhs="
                            + lhs + " rhs=" + rhs);
                }
            } catch (RuntimeException t) {
                boolean cleanRejection = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
                if (cleanRejection) {
                    return;
                }
                boolean root = t instanceof NullPointerException;
                boolean inJoin = false;
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                        inJoin = true;
                        break;
                    }
                }
                if (root && inJoin) {
                    throw t;
                }
            }
        }
    }
}
```

</details>

---
## [22] ⚙️ harness-attempt · `attempt_010`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [23] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~10,099 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final String[] ARRAY_LIST = { "foo", "bar", "baz" };
    private static final String[] EMPTY_ARRAY_LIST = {};
    private static final String[] NULL_ARRAY_LIST = { null };
    private static final Object NULL_TO_STRING = new Object() {
        @Override
        public String toString() {
            return null;
        }
    };
    private static final Object[] NULL_TO_STRING_LIST = { NULL_TO_STRING };
    private static final String[] MIXED_ARRAY_LIST = { null, "", "foo" };
    private static final Object[] MIXED_TYPE_LIST = { "foo", Long.valueOf(2L) };
    private static final char SEPARATOR_CHAR = ';';
    private static final String TEXT_LIST_CHAR = "foo;bar;baz";

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchorChecks();

        int len = data.consumeInt(1, 6);
        Object[] array = new Object[len];
        for (int i = 0; i < len; i++) {
            int kind = data.consumeInt(0, 3);
            if (kind == 0) {
                array[i] = null;
            } else if (kind == 1) {
                array[i] = data.consumeAsciiString(8);
            } else if (kind == 2) {
                array[i] = Long.valueOf(data.consumeInt(-1000, 1000));
            } else {
                array[i] = data.consumeString(6);
            }
        }

        int startIndex = data.consumeInt(0, len - 1);
        int endIndex = data.consumeInt(startIndex + 1, len);
        array[startIndex] = NULL_TO_STRING;

        char sepChar = (char) (data.consumeByte() & 0xff);
        String sepString = data.consumeBoolean() ? null : data.consumeAsciiString(4);

        checkJoinChar(array, sepChar, startIndex, endIndex, null, true);
        checkJoinString(array, sepString, startIndex, endIndex, null, true);

        // Contract/oracle:
        // StringUtils.join(array, null, s, e) treats null separator as EMPTY,
        // so it must agree with join(array, "", s, e) on every accepted input.
        // A patch that only suppresses the crash but changes EMPTY handling or
        // silently drops separator behavior would violate this observable relation.
        try {
            String lhs = StringUtils.join(array, (String) null, startIndex, endIndex);
            String rhs = StringUtils.join(array, "", startIndex, endIndex);
            if (lhs != null ? !lhs.equals(rhs) : rhs != null) {
                throw new RuntimeException("[oracle:null-sep-eq-empty] metamorphic violation: join(array,null,s,e)==join(array,\"\",s,e) inputStart=" + startIndex + " inputEnd=" + endIndex + " lhs=" + String.valueOf(lhs) + " rhs=" + String.valueOf(rhs));
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t) && isValidByConstruction(array, startIndex, endIndex)) {
                throw t;
            }
        }

        // Shared EMPTY-state consistency: methods documented to return empty string
        // on these inputs should agree with StringUtils.EMPTY.
        checkEmptyConsistency();
    }

    private static void anchorChecks() {
        checkJoinChar((Object[]) null, ',', 0, 0, null, true);
        checkJoinChar(ARRAY_LIST, SEPARATOR_CHAR, 0, ARRAY_LIST.length, TEXT_LIST_CHAR, true);
        checkJoinChar(EMPTY_ARRAY_LIST, SEPARATOR_CHAR, 0, 0, "", true);
        checkJoinChar(MIXED_ARRAY_LIST, SEPARATOR_CHAR, 0, MIXED_ARRAY_LIST.length, ";;foo", true);
        checkJoinChar(MIXED_TYPE_LIST, SEPARATOR_CHAR, 0, MIXED_TYPE_LIST.length, "foo;2", true);
        checkJoinChar(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1, "/", true);
        checkJoinChar(MIXED_TYPE_LIST, '/', 0, 1, "foo", true);
        checkJoinChar(NULL_TO_STRING_LIST, '/', 0, 1, "null", true);
        checkJoinChar(MIXED_TYPE_LIST, '/', 0, 2, "foo/2", true);
        checkJoinChar(MIXED_TYPE_LIST, '/', 1, 2, "2", true);
        checkJoinChar(MIXED_TYPE_LIST, '/', 2, 1, "", true);

        checkJoinObjectArray((Object[]) null, null, true);
        checkJoinObjectArray(new Object[0], "", true);
        checkJoinVarargsNull("", true);
        checkJoinObjectArray(EMPTY_ARRAY_LIST, "", true);
        checkJoinObjectArray(NULL_ARRAY_LIST, "", true);
        checkJoinObjectArray(NULL_TO_STRING_LIST, "null", true);
        checkJoinObjectArray(new String[] { "a", "b", "c" }, "abc", true);
        checkJoinObjectArray(new String[] { null, "a", "" }, "a", true);
        checkJoinObjectArray(MIXED_ARRAY_LIST, "foo", true);
        checkJoinObjectArray(MIXED_TYPE_LIST, "foo2", true);

        checkJoinString(NULL_TO_STRING_LIST, "/", 0, 1, "null", true);
    }

    private static void checkJoinVarargsNull(String expected, boolean valid) {
        try {
            String actual = StringUtils.join((Object) null);
            if (!safeEquals(actual, expected)) {
                throw new RuntimeException("[oracle:join-vararg-null] metamorphic violation: expected=" + String.valueOf(expected) + " actual=" + String.valueOf(actual));
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (valid && isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void checkJoinObjectArray(Object[] array, String expected, boolean valid) {
        try {
            String actual = StringUtils.join(array);
            if (!safeEquals(actual, expected)) {
                throw new RuntimeException("[oracle:join-object-array] metamorphic violation: expected=" + String.valueOf(expected) + " actual=" + String.valueOf(actual));
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (valid && isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void checkJoinChar(Object[] array, char separator, int startIndex, int endIndex, String expected, boolean valid) {
        try {
            String actual = StringUtils.join(array, separator, startIndex, endIndex);
            if (expected != null || array == null || (endIndex - startIndex) <= 0 || valid) {
                if (!safeEquals(actual, expected)) {
                    throw new RuntimeException("[oracle:join-char] metamorphic violation: expected=" + String.valueOf(expected) + " actual=" + String.valueOf(actual) + " start=" + startIndex + " end=" + endIndex);
                }
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (valid && isRootCause(t) && isValidByConstruction(array, startIndex, endIndex)) {
                throw t;
            }
        }
    }

    private static void checkJoinString(Object[] array, String separator, int startIndex, int endIndex, String expected, boolean valid) {
        try {
            String actual = StringUtils.join(array, separator, startIndex, endIndex);
            if (expected != null || array == null || (endIndex - startIndex) <= 0 || valid) {
                if (!safeEquals(actual, expected)) {
                    throw new RuntimeException("[oracle:join-string] metamorphic violation: expected=" + String.valueOf(expected) + " actual=" + String.valueOf(actual) + " start=" + startIndex + " end=" + endIndex + " sep=" + String.valueOf(separator));
                }
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (valid && isRootCause(t) && isValidByConstruction(array, startIndex, endIndex)) {
                throw t;
            }
        }
    }

    private static void checkEmptyConsistency() {
        try {
            String e = StringUtils.EMPTY;
            String a = StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1);
            String b = StringUtils.trimToEmpty(null);
            String c = StringUtils.stripToEmpty(null);
            String d = StringUtils.substring("", 1);
            String f = StringUtils.left("abc", -1);
            if (!safeEquals(e, a) || !safeEquals(e, b) || !safeEquals(e, c) || !safeEquals(e, d) || !safeEquals(e, f)) {
                throw new RuntimeException("[oracle:empty-consistency] metamorphic violation: EMPTY disagreement a=" + String.valueOf(a) + " b=" + String.valueOf(b) + " c=" + String.valueOf(c) + " d=" + String.valueOf(d) + " f=" + String.valueOf(f));
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

    private static boolean isValidByConstruction(Object[] array, int startIndex, int endIndex) {
        return array != null
                && startIndex >= 0
                && endIndex >= startIndex
                && endIndex <= array.length
                && startIndex < array.length
                && (endIndex - startIndex) > 0;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        boolean sawJoin = false;
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement ste = stack[i];
            if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName())
                    && "join".equals(ste.getMethodName())) {
                sawJoin = true;
                break;
            }
        }
        return sawJoin;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
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
<details open><summary>▸ Output (~12,800 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final String[] ARRAY_LIST = { "foo", "bar", "baz" };
        final String[] EMPTY_ARRAY_LIST = {};
        final String[] NULL_ARRAY_LIST = { null };
        final String[] MIXED_ARRAY_LIST = { null, "", "foo" };
        final Object[] MIXED_TYPE_LIST = { "foo", Long.valueOf(2L) };
        final char SEPARATOR_CHAR = ';';
        final String TEXT_LIST_CHAR = "foo;bar;baz";
        final Object nullToStringObject = new Object() {
            @Override
            public String toString() {
                return null;
            }
        };
        final Object[] NULL_TO_STRING_LIST = { nullToStringObject };

        try {
            if (StringUtils.join((Object[]) null, ',') != null) {
                throw new RuntimeException("[oracle:anchor-null-array-char] metamorphic violation: join((Object[])null, ',') must return null");
            }
            if (!TEXT_LIST_CHAR.equals(StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-array-char] metamorphic violation: expected=" + TEXT_LIST_CHAR + " actual=" + StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR));
            }
            if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-empty-array-char] metamorphic violation: empty array must join to empty string");
            }
            if (!";;foo".equals(StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-mixed-array-char] metamorphic violation: expected=';;foo' actual=" + StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR));
            }
            if (!"foo;2".equals(StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-mixed-type-char] metamorphic violation: expected='foo;2' actual=" + StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR));
            }
            if (!"/".equals(StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1))) {
                throw new RuntimeException("[oracle:anchor-range-char-1] metamorphic violation: expected='/' actual=" + StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1));
            }
            if (!"foo".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1))) {
                throw new RuntimeException("[oracle:anchor-range-char-2] metamorphic violation: expected='foo' actual=" + StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1));
            }
            if (!"null".equals(StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1))) {
                throw new RuntimeException("[oracle:anchor-null-tostring-char] metamorphic violation: expected='null' actual=" + StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1));
            }
            if (!"foo/2".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2))) {
                throw new RuntimeException("[oracle:anchor-range-char-3] metamorphic violation: expected='foo/2' actual=" + StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2));
            }
            if (!"2".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2))) {
                throw new RuntimeException("[oracle:anchor-range-char-4] metamorphic violation: expected='2' actual=" + StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2));
            }
            if (!"".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1))) {
                throw new RuntimeException("[oracle:anchor-range-char-5] metamorphic violation: expected='' actual=" + StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1));
            }

            if (StringUtils.join((Object[]) null) != null) {
                throw new RuntimeException("[oracle:anchor-null-array-object] metamorphic violation: join((Object[])null) must return null");
            }
            if (!"".equals(StringUtils.join())) {
                throw new RuntimeException("[oracle:anchor-empty-varargs] metamorphic violation: join() must return empty string");
            }
            if (!"".equals(StringUtils.join((Object) null))) {
                throw new RuntimeException("[oracle:anchor-single-null] metamorphic violation: join((Object)null) must return empty string");
            }
            if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-empty-array-object-2] metamorphic violation: expected empty string");
            }
            if (!"".equals(StringUtils.join(NULL_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-null-array-element] metamorphic violation: expected empty string");
            }
            if (!"null".equals(StringUtils.join(NULL_TO_STRING_LIST))) {
                throw new RuntimeException("[oracle:anchor-null-tostring-object] metamorphic violation: expected='null' actual=" + StringUtils.join(NULL_TO_STRING_LIST));
            }
            if (!"a".equals(StringUtils.join(new String[] { null, "a", "" }))) {
                throw new RuntimeException("[oracle:anchor-leading-null] metamorphic violation: expected='a' actual=" + StringUtils.join(new String[] { null, "a", "" }));
            }
            if (!"foo".equals(StringUtils.join(MIXED_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-mixed-array-object] metamorphic violation: expected='foo' actual=" + StringUtils.join(MIXED_ARRAY_LIST));
            }
            if (!"foo2".equals(StringUtils.join(MIXED_TYPE_LIST))) {
                throw new RuntimeException("[oracle:anchor-mixed-type-object] metamorphic violation: expected='foo2' actual=" + StringUtils.join(MIXED_TYPE_LIST));
            }

            if (!"null".equals(StringUtils.join(NULL_TO_STRING_LIST, "", 0, 1))) {
                throw new RuntimeException("[oracle:anchor-null-tostring-stringsep] metamorphic violation: expected='null' actual=" + StringUtils.join(NULL_TO_STRING_LIST, "", 0, 1));
            }
        } catch (RuntimeException t) {
            boolean isCleanRejection = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            boolean passesJoin = false;
            StackTraceElement[] st = t.getStackTrace();
            for (int i = 0; i < st.length; i++) {
                if ("org.apache.commons.lang3.StringUtils".equals(st[i].getClassName()) && "join".equals(st[i].getMethodName())) {
                    passesJoin = true;
                    break;
                }
            }
            if (!isCleanRejection && t instanceof NullPointerException && passesJoin) {
                throw t;
            }
        }

        int len = data.consumeInt(1, 8);
        Object[] arr = new Object[len];
        int startIndex = data.consumeInt(0, len - 1);
        int endIndex = data.consumeInt(startIndex + 1, len);

        for (int i = 0; i < len; i++) {
            if (i == startIndex) {
                arr[i] = nullToStringObject;
            } else {
                int kind = data.consumeInt(0, 4);
                if (kind == 0) {
                    arr[i] = null;
                } else if (kind == 1) {
                    arr[i] = data.consumeAsciiString(16);
                } else if (kind == 2) {
                    arr[i] = data.consumeString(16);
                } else if (kind == 3) {
                    arr[i] = Long.valueOf(data.consumeInt(-1000000, 1000000));
                } else {
                    arr[i] = "";
                }
            }
        }

        char charSep = (char) (data.consumeInt(0, 127));
        String stringSep;
        if (data.consumeBoolean()) {
            stringSep = String.valueOf(charSep);
        } else {
            stringSep = data.consumeString(4);
            if (stringSep == null) {
                stringSep = "";
            }
        }

        try {
            String joinedChar = StringUtils.join(arr, charSep, startIndex, endIndex);
            String joinedString = StringUtils.join(arr, stringSep, startIndex, endIndex);
            String joinedVarargs = null;
            if (startIndex == 0 && endIndex == arr.length) {
                joinedVarargs = StringUtils.join(arr);
            }

            if (String.valueOf(charSep).equals(stringSep) && !joinedChar.equals(joinedString)) {
                throw new RuntimeException("[oracle:char-vs-string] metamorphic violation: one-character String separator must agree with char separator inputStart=" + startIndex + " inputEnd=" + endIndex + " lhs=" + joinedChar + " rhs=" + joinedString);
            }

            /* Contract used: join(array, sep, start, end) joins the elements in [start,end).
               Therefore joining a copied slice with the same separator from 0..slice.length must produce the same result.
               A throw-deleting or branch-skipping patch could avoid the crash by skipping the first selected element, breaking this equality. */
            if (endIndex > startIndex) {
                Object[] slice = new Object[endIndex - startIndex];
                for (int i = 0; i < slice.length; i++) {
                    slice[i] = arr[startIndex + i];
                }
                try {
                    String sliceChar = StringUtils.join(slice, charSep, 0, slice.length);
                    if (!joinedChar.equals(sliceChar)) {
                        throw new RuntimeException("[oracle:slice-char] metamorphic violation: join(range) must equal join(slice) inputStart=" + startIndex + " inputEnd=" + endIndex + " lhs=" + joinedChar + " rhs=" + sliceChar);
                    }
                } catch (RuntimeException ignored) {
                }
                try {
                    String sliceString = StringUtils.join(slice, stringSep, 0, slice.length);
                    if (!joinedString.equals(sliceString)) {
                        throw new RuntimeException("[oracle:slice-string] metamorphic violation: join(range) must equal join(slice) inputStart=" + startIndex + " inputEnd=" + endIndex + " lhs=" + joinedString + " rhs=" + sliceString);
                    }
                } catch (RuntimeException ignored) {
                }
            }

            if (joinedVarargs != null) {
                try {
                    String wholeRange = StringUtils.join(arr, "", 0, arr.length);
                    if (!joinedVarargs.equals(wholeRange)) {
                        throw new RuntimeException("[oracle:varargs-vs-range] metamorphic violation: join(array) must equal join(array, \"\", 0, len) lhs=" + joinedVarargs + " rhs=" + wholeRange);
                    }
                } catch (RuntimeException ignored) {
                }
            }

            /* Shared EMPTY contract: join returns EMPTY when noOfItems <= 0, and trimToEmpty/stripToEmpty/left/substring
               also report the same EMPTY constant for their documented empty-result cases. If a patch corrupts or bypasses EMPTY
               handling, these observable readers disagree even when no exception is thrown. */
            String emptyJoin = StringUtils.join(arr, charSep, 1, 1);
            String emptyTrim = StringUtils.trimToEmpty(null);
            String emptyStrip = StringUtils.stripToEmpty(null);
            String emptyLeft = StringUtils.left("abc", -1);
            String emptySubstring = StringUtils.substring("", 0);
            if (!(emptyJoin.equals(emptyTrim) && emptyJoin.equals(emptyStrip) && emptyJoin.equals(emptyLeft) && emptyJoin.equals(emptySubstring))) {
                throw new RuntimeException("[oracle:empty-shared-state] metamorphic violation: EMPTY-bearing methods disagree join=" + emptyJoin + " trim=" + emptyTrim + " strip=" + emptyStrip + " left=" + emptyLeft + " substring=" + emptySubstring);
            }
        } catch (RuntimeException t) {
            boolean isCleanRejection = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            if (isCleanRejection) {
                return;
            }
            boolean passesJoin = false;
            StackTraceElement[] st = t.getStackTrace();
            for (int i = 0; i < st.length; i++) {
                if ("org.apache.commons.lang3.StringUtils".equals(st[i].getClassName()) && "join".equals(st[i].getMethodName())) {
                    passesJoin = true;
                    break;
                }
            }
            if (t instanceof NullPointerException && passesJoin) {
                throw t;
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
## [26] ⚙️ harness-attempt · `attempt_012`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NullPointerException

---
## [27] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,708 chars)</summary>

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
    private static final Object[] NULL_TO_STRING_LIST = { makeNullToStringObject() };

    private static Object makeNullToStringObject() {
        return new Object() {
            @Override
            public String toString() {
                return null;
            }
        };
    }

    private static boolean isJoinRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
            if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName())
                    && "join".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static void handleThrowable(Throwable t, boolean validByConstruction) {
        if (t instanceof RuntimeException) {
            RuntimeException rt = (RuntimeException) t;
            String msg = rt.getMessage();
            if (msg != null && msg.startsWith("[oracle:")) {
                throw rt;
            }
            if (validByConstruction && isJoinRootCause(rt)) {
                throw rt;
            }
        }
    }

    private static void checkEquals(String oracleId, String what, String lhs, String rhs) {
        if (lhs == null ? rhs != null : !lhs.equals(rhs)) {
            throw new RuntimeException("[oracle:" + oracleId + "] metamorphic violation: " + what
                    + " lhs=" + String.valueOf(lhs) + " rhs=" + String.valueOf(rhs));
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            if (StringUtils.join((Object[]) null, ',') != null) {
                throw new RuntimeException("[oracle:anchor-null-array] metamorphic violation: join((Object[])null, ',') must return null");
            }
            checkEquals("anchor-char", "failing test contract for single null-toString element",
                    "null", StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1));
            if (StringUtils.join((Object[]) null) != null) {
                throw new RuntimeException("[oracle:anchor-null-varargs] metamorphic violation: join((Object[])null) must return null");
            }
            checkEquals("anchor-varargs", "failing test contract for object-array overload",
                    "null", StringUtils.join(NULL_TO_STRING_LIST));
            checkEquals("anchor-text-list", "regression sanity from test fixture",
                    TEXT_LIST_CHAR, StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR));
            checkEquals("anchor-empty-array", "empty array contract",
                    "", StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR));
            checkEquals("anchor-null-array-list", "single null element contract",
                    "", StringUtils.join(NULL_ARRAY_LIST));
            checkEquals("anchor-mixed-array", "fixture contract",
                    "foo", StringUtils.join(MIXED_ARRAY_LIST));
            checkEquals("anchor-mixed-type", "fixture contract",
                    "foo2", StringUtils.join(MIXED_TYPE_LIST));
        } catch (Throwable t) {
            handleThrowable(t, true);
            return;
        }

        int totalLen = data.consumeInt(1, 8);
        int startIndex = data.consumeInt(0, totalLen - 1);
        int endIndex = data.consumeInt(startIndex + 1, totalLen);

        Object[] array = new Object[totalLen];
        for (int i = 0; i < totalLen; i++) {
            int kind = data.consumeInt(0, 3);
            if (kind == 0) {
                array[i] = data.consumeBoolean() ? null : data.consumeString(16);
            } else if (kind == 1) {
                array[i] = data.consumeAsciiString(16);
            } else if (kind == 2) {
                array[i] = Long.valueOf(data.consumeInt(-1000000, 1000000));
            } else {
                array[i] = "";
            }
        }

        array[startIndex] = makeNullToStringObject();

        char charSep = (char) (data.consumeByte() & 0x7f);
        String stringSep = data.consumeBoolean() ? null : data.consumeString(8);

        try {
            String charJoined = StringUtils.join(array, charSep, startIndex, endIndex);
            String stringJoined = StringUtils.join(array, stringSep, startIndex, endIndex);

            /* Contract asserted:
             * - The tests show a slice containing exactly one element whose toString() returns null
             *   must produce the text "null", not throw.
             * - For any correct implementation, when startIndex+1==endIndex, the separator is never appended,
             *   so char and String separator overloads must agree on the same single-element slice.
             * - The javadocs for trimToEmpty/stripToEmpty/substring/left state they return an empty String ("")
             *   for covered empty/null cases; join with noOfItems<=0 also returns EMPTY. A throw-deleting or
             *   wrong-return patch that changes join's EMPTY behavior would break this observable agreement.
             */
            if (endIndex - startIndex == 1) {
                checkEquals("single-slice-char", "single null-toString element must stringify as \"null\"",
                        "null", charJoined);
                checkEquals("single-slice-string", "single null-toString element must stringify as \"null\"",
                        "null", stringJoined);
                checkEquals("single-overload-agree", "single-element slice ignores separator and overloads must agree",
                        charJoined, stringJoined);
            }

            String emptyFromJoinChar = StringUtils.join(array, charSep, startIndex, startIndex);
            String emptyFromJoinString = StringUtils.join(array, stringSep, startIndex, startIndex);
            String emptyFromTrim = StringUtils.trimToEmpty(null);
            String emptyFromStrip = StringUtils.stripToEmpty(null);
            String emptyFromSubstring = StringUtils.substring("", 1);
            String emptyFromSubstring2 = StringUtils.substring("", 1, 0);
            String emptyFromLeft = StringUtils.left("abc", -1);

            checkEquals("empty-shared-1", "join(empty slice) and trimToEmpty(null) share EMPTY contract",
                    emptyFromJoinChar, emptyFromTrim);
            checkEquals("empty-shared-2", "join(empty slice) and stripToEmpty(null) share EMPTY contract",
                    emptyFromJoinString, emptyFromStrip);
            checkEquals("empty-shared-3", "join(empty slice) and substring(\"\",1) share EMPTY contract",
                    emptyFromJoinChar, emptyFromSubstring);
            checkEquals("empty-shared-4", "join(empty slice) and substring(\"\",1,0) share EMPTY contract",
                    emptyFromJoinString, emptyFromSubstring2);
            checkEquals("empty-shared-5", "join(empty slice) and left(\"abc\",-1) share EMPTY contract",
                    emptyFromJoinChar, emptyFromLeft);
        } catch (IllegalArgumentException e) {
            return;
        } catch (Throwable t) {
            handleThrowable(t, true);
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
<details open><summary>▸ Output (~8,737 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final Object nullToString = new Object() {
            @Override
            public String toString() {
                return null;
            }
        };

        final String[] ARRAY_LIST = { "foo", "bar", "baz" };
        final String[] EMPTY_ARRAY_LIST = {};
        final String[] NULL_ARRAY_LIST = { null };
        final Object[] NULL_TO_STRING_LIST = { nullToString };
        final String[] MIXED_ARRAY_LIST = { null, "", "foo" };
        final Object[] MIXED_TYPE_LIST = { "foo", Long.valueOf(2L) };
        final char SEPARATOR_CHAR = ';';
        final String TEXT_LIST_CHAR = "foo;bar;baz";

        // Contract/oracle used below:
        // - The test suite documents that join(NULL_TO_STRING_LIST, '/', 0, 1) == "null"
        //   and join(NULL_TO_STRING_LIST) == "null". A throw-deleting / branch-skipping patch
        //   could avoid the NPE yet still return the wrong text, so we assert these outputs.
        // - Equivalent-input relation: join(array, c, s, e) must agree with join(array, String.valueOf(c), s, e).
        // - EMPTY-sharing sanity: when endIndex - startIndex <= 0, join returns EMPTY; trimToEmpty(null) also returns EMPTY.

        try {
            if (StringUtils.join((Object[]) null, ',') != null) {
                throw new RuntimeException("[oracle:anchor-null-char] metamorphic violation: documented null-array join must return null");
            }
            String anchorChar = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            if (!"null".equals(anchorChar)) {
                throw new RuntimeException("[oracle:anchor-char] metamorphic violation: documented one-element char join input=" + NULL_TO_STRING_LIST.length + " lhs=" + anchorChar + " rhs=null");
            }
            if (StringUtils.join((Object[]) null) != null) {
                throw new RuntimeException("[oracle:anchor-null-varargs] metamorphic violation: documented null-array varargs join must return null");
            }
            String anchorObj = StringUtils.join(NULL_TO_STRING_LIST);
            if (!"null".equals(anchorObj)) {
                throw new RuntimeException("[oracle:anchor-obj] metamorphic violation: documented object-array join input=" + NULL_TO_STRING_LIST.length + " lhs=" + anchorObj + " rhs=null");
            }
            if (!TEXT_LIST_CHAR.equals(StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-text] metamorphic violation: documented text join mismatch");
            }
            if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-empty] metamorphic violation: documented empty join mismatch");
            }
            if (!";;foo".equals(StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-mixed-array] metamorphic violation: documented mixed-array join mismatch");
            }
            if (!"foo;2".equals(StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-mixed-type] metamorphic violation: documented mixed-type join mismatch");
            }
            if (!"".equals(StringUtils.join(NULL_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-null-element] metamorphic violation: documented single-null join mismatch");
            }
            if (!"foo".equals(StringUtils.join(MIXED_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-varargs-mixed-array] metamorphic violation: documented varargs mixed-array join mismatch");
            }
            if (!"foo2".equals(StringUtils.join(MIXED_TYPE_LIST))) {
                throw new RuntimeException("[oracle:anchor-varargs-mixed-type] metamorphic violation: documented varargs mixed-type join mismatch");
            }
        } catch (RuntimeException t) {
            boolean cleanRejection = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            boolean stackHasJoin = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName()) && "join".equals(ste.getMethodName())) {
                    stackHasJoin = true;
                    break;
                }
            }
            if (cleanRejection) {
                return;
            }
            if (t instanceof NullPointerException && stackHasJoin) {
                throw t;
            }
            if (t.getClass() == RuntimeException.class && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        }

        int len = data.consumeInt(1, 8);
        Object[] arr = new Object[len];
        int startIndex = data.consumeInt(0, len - 1);
        int endIndex = data.consumeInt(startIndex + 1, len);
        arr[startIndex] = nullToString;
        for (int i = 0; i < len; i++) {
            if (i == startIndex) {
                continue;
            }
            int choice = data.consumeInt(0, 4);
            switch (choice) {
                case 0:
                    arr[i] = null;
                    break;
                case 1:
                    arr[i] = data.consumeString(16);
                    break;
                case 2:
                    arr[i] = data.consumeAsciiString(16);
                    break;
                case 3:
                    arr[i] = Long.valueOf(data.consumeInt(-1000, 1000));
                    break;
                default:
                    arr[i] = "";
                    break;
            }
        }

        char sepChar = (char) data.consumeInt(0, 127);
        String sepString = String.valueOf(sepChar);

        try {
            String charJoin = StringUtils.join(arr, sepChar, startIndex, endIndex);
            String stringJoin = StringUtils.join(arr, sepString, startIndex, endIndex);

            if (!charJoin.equals(stringJoin)) {
                throw new RuntimeException("[oracle:char-string-agree] metamorphic violation: equivalent separators must agree input=start=" + startIndex + ",end=" + endIndex + " lhs=" + charJoin + " rhs=" + stringJoin);
            }

            Object[] single = new Object[] { nullToString };
            String singleChar = StringUtils.join(single, sepChar, 0, 1);
            String singleString = StringUtils.join(single, sepString, 0, 1);
            if (!"null".equals(singleChar)) {
                throw new RuntimeException("[oracle:single-char-null] metamorphic violation: documented one-element join must render null-toString as \"null\" input=" + sepChar + " lhs=" + singleChar + " rhs=null");
            }
            if (!"null".equals(singleString)) {
                throw new RuntimeException("[oracle:single-string-null] metamorphic violation: documented one-element join must render null-toString as \"null\" input=" + sepString + " lhs=" + singleString + " rhs=null");
            }

            String emptyByJoinChar = StringUtils.join(arr, sepChar, endIndex, startIndex);
            String emptyByJoinString = StringUtils.join(arr, sepString, endIndex, startIndex);
            String emptyByTrim = StringUtils.trimToEmpty(null);
            if (!emptyByTrim.equals(emptyByJoinChar) || !emptyByTrim.equals(emptyByJoinString)) {
                throw new RuntimeException("[oracle:empty-shared] metamorphic violation: EMPTY-backed APIs must agree input=start=" + startIndex + ",end=" + endIndex + " lhs=" + emptyByJoinChar + "/" + emptyByJoinString + " rhs=" + emptyByTrim);
            }
        } catch (RuntimeException t) {
            boolean cleanRejection = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            boolean stackHasJoin = false;
            for (StackTraceElement ste : t.getStackTrace()) {
                if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName()) && "join".equals(ste.getMethodName())) {
                    stackHasJoin = true;
                    break;
                }
            }
            if (cleanRejection) {
                return;
            }
            if (t instanceof NullPointerException && stackHasJoin) {
                throw t;
            }
            if (t.getClass() == RuntimeException.class && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
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
- trigger: java.lang.NullPointerException

---
## [31] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~9,938 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final Object nullToString = new Object() {
            @Override
            public String toString() {
                return null;
            }
        };

        final Object[] NULL_TO_STRING_LIST = new Object[] { nullToString };
        final String[] EMPTY_ARRAY_LIST = new String[] {};
        final String[] NULL_ARRAY_LIST = new String[] { null };
        final String[] MIXED_ARRAY_LIST = new String[] { null, "", "foo" };
        final Object[] MIXED_TYPE_LIST = new Object[] { "foo", Long.valueOf(2L) };

        try {
            if (StringUtils.join((Object[]) null, ',') != null) {
                throw new RuntimeException("[oracle:anchor-null-array-char] metamorphic violation: exact test contract input=null-array lhs=" + StringUtils.join((Object[]) null, ',') + " rhs=null");
            }
            if (StringUtils.join((Object[]) null) != null) {
                throw new RuntimeException("[oracle:anchor-null-array-obj] metamorphic violation: exact test contract input=null-array lhs=" + StringUtils.join((Object[]) null) + " rhs=null");
            }
            String emptyFromVarargsNull = StringUtils.join((Object) null);
            if (!"".equals(emptyFromVarargsNull)) {
                throw new RuntimeException("[oracle:anchor-varargs-null] metamorphic violation: exact test contract input=(Object)null lhs=" + emptyFromVarargsNull + " rhs=");
            }
            if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-empty-array] metamorphic violation: exact test contract input=empty-array lhs=" + StringUtils.join(EMPTY_ARRAY_LIST) + " rhs=");
            }
            if (!"".equals(StringUtils.join(NULL_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-null-element-array] metamorphic violation: exact test contract input=[null] lhs=" + StringUtils.join(NULL_ARRAY_LIST) + " rhs=");
            }
            if (!"foo".equals(StringUtils.join(MIXED_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-mixed-array] metamorphic violation: exact test contract input=[null,\"\",\"foo\"] lhs=" + StringUtils.join(MIXED_ARRAY_LIST) + " rhs=foo");
            }
            if (!"foo2".equals(StringUtils.join(MIXED_TYPE_LIST))) {
                throw new RuntimeException("[oracle:anchor-mixed-type] metamorphic violation: exact test contract input=[\"foo\",2] lhs=" + StringUtils.join(MIXED_TYPE_LIST) + " rhs=foo2");
            }

            String anchorObj = StringUtils.join(NULL_TO_STRING_LIST);
            if (!"null".equals(anchorObj)) {
                throw new RuntimeException("[oracle:anchor-objectarray] metamorphic violation: exact test contract input=[toString()->null] lhs=" + anchorObj + " rhs=null");
            }

            String anchorChar = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            if (!"null".equals(anchorChar)) {
                throw new RuntimeException("[oracle:anchor-char] metamorphic violation: exact test contract input=[toString()->null],sep=/,start=0,end=1 lhs=" + anchorChar + " rhs=null");
            }
        } catch (RuntimeException t) {
            boolean inJoin = false;
            StackTraceElement[] st = t.getStackTrace();
            for (int i = 0; i < st.length; i++) {
                if ("org.apache.commons.lang3.StringUtils".equals(st[i].getClassName()) && "join".equals(st[i].getMethodName())) {
                    inJoin = true;
                    break;
                }
            }
            if (t instanceof NullPointerException && inJoin) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            if (t.getClass().getName().startsWith("org.apache.commons.lang3") && inJoin) {
                return;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        }

        int mode = data.consumeInt(0, 3);
        int len = data.consumeInt(1, 8);
        Object[] arr = new Object[len];
        int specialIndex = data.consumeInt(0, len - 1);

        for (int i = 0; i < len; i++) {
            int kind = data.consumeInt(0, 4);
            if (i == specialIndex || (mode == 1 && kind == 4)) {
                arr[i] = nullToString;
            } else if (kind == 0) {
                arr[i] = null;
            } else if (kind == 1) {
                arr[i] = data.consumeAsciiString(16);
            } else if (kind == 2) {
                arr[i] = data.consumeString(16);
            } else if (kind == 3) {
                arr[i] = Long.valueOf(data.consumeInt(-1000000, 1000000));
            } else {
                arr[i] = "";
            }
        }

        int start = data.consumeInt(0, len - 1);
        int end = data.consumeInt(start + 1, len);

        char sepChar = (char) data.consumeInt(1, 126);
        String sepString;
        if (data.consumeBoolean()) {
            sepString = String.valueOf(sepChar);
        } else {
            sepString = data.consumeAsciiString(4);
        }

        try {
            String charJoin = StringUtils.join(arr, sepChar, start, end);

            String stringJoin = StringUtils.join(arr, String.valueOf(sepChar), start, end);

            /* Contract/oracle:
             * The char-separator and one-character String-separator overloads document the same join semantics
             * over the same slice, so for any valid array/start/end they must produce the same result.
             * A throw-deleting or branch-skipping patch that merely avoids the NPE but mishandles null-toString
             * elements would break this sibling-agreement check without throwing.
             */
            if (!charJoin.equals(stringJoin)) {
                throw new RuntimeException("[oracle:char-vs-string] metamorphic violation: equivalent overloads inputStart=" + start + " inputEnd=" + end + " sepChar=" + sepChar + " lhs=" + charJoin + " rhs=" + stringJoin);
            }
        } catch (RuntimeException t) {
            boolean inJoin = false;
            StackTraceElement[] st = t.getStackTrace();
            for (int i = 0; i < st.length; i++) {
                if ("org.apache.commons.lang3.StringUtils".equals(st[i].getClassName()) && "join".equals(st[i].getMethodName())) {
                    inJoin = true;
                    break;
                }
            }
            if (t instanceof NullPointerException && inJoin) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            if (t.getClass().getName().startsWith("org.apache.commons.lang3") && inJoin) {
                return;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        }

        try {
            String nullSep = StringUtils.join(arr, (String) null, start, end);
            String emptySep = StringUtils.join(arr, "", start, end);

            /* Contract/oracle:
             * The implementation explicitly specifies "if (separator == null) separator = EMPTY",
             * so joining with a null separator must equal joining with the empty String.
             * This also checks agreement with readers of the shared EMPTY constant.
             */
            if (!nullSep.equals(emptySep)) {
                throw new RuntimeException("[oracle:null-sep-empty-sep] metamorphic violation: null separator must act like EMPTY inputStart=" + start + " inputEnd=" + end + " lhs=" + nullSep + " rhs=" + emptySep);
            }

            String empty1 = StringUtils.trimToEmpty(null);
            String empty2 = StringUtils.stripToEmpty(null);
            String empty3 = StringUtils.substring("", 0);
            String empty4 = StringUtils.substring("abc", 1, 1);
            String empty5 = StringUtils.left("abc", -1);

            if (!empty1.equals(empty2) || !empty1.equals(empty3) || !empty1.equals(empty4) || !empty1.equals(empty5) || !empty1.equals("")) {
                throw new RuntimeException("[oracle:empty-agreement] metamorphic violation: readers of EMPTY disagree lhs=" + empty1 + " rhs=" + empty2 + "/" + empty3 + "/" + empty4 + "/" + empty5);
            }

            String anyJoin = StringUtils.join(arr, sepString, start, end);
            if (anyJoin == null) {
                throw new RuntimeException("[oracle:nonnull-result] metamorphic violation: valid non-null array slice should produce non-null String inputStart=" + start + " inputEnd=" + end + " sep=" + sepString);
            }
        } catch (RuntimeException t) {
            boolean inJoin = false;
            StackTraceElement[] st = t.getStackTrace();
            for (int i = 0; i < st.length; i++) {
                if ("org.apache.commons.lang3.StringUtils".equals(st[i].getClassName()) && "join".equals(st[i].getMethodName())) {
                    inJoin = true;
                    break;
                }
            }
            if (t instanceof NullPointerException && inJoin) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            if (t.getClass().getName().startsWith("org.apache.commons.lang3") && inJoin) {
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
<details open><summary>▸ Output (~15,084 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final String[] ARRAY_LIST = { "foo", "bar", "baz" };
        final String[] EMPTY_ARRAY_LIST = {};
        final String[] NULL_ARRAY_LIST = { null };
        final String[] MIXED_ARRAY_LIST = { null, "", "foo" };
        final Object[] MIXED_TYPE_LIST = { "foo", Long.valueOf(2L) };
        final char SEPARATOR_CHAR = ';';
        final String TEXT_LIST_CHAR = "foo;bar;baz";
        final Object[] NULL_TO_STRING_LIST = {
            new Object() {
                @Override
                public String toString() {
                    return null;
                }
            }
        };

        /* Anchor checks copied from the real tests: these are valid-by-construction inputs.
           The documented behavior requires these calls to succeed and produce the asserted values.
           A "fix" that only suppresses the exception or skips appending would violate these results. */
        try {
            String r0 = StringUtils.join((Object[]) null, ',');
            if (r0 != null) {
                throw new RuntimeException("[oracle:anchor-null-array-char] metamorphic violation: expected null for join((Object[])null, ',') input=null lhs=" + String.valueOf(r0) + " rhs=null");
            }

            String r1 = StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR);
            if (!TEXT_LIST_CHAR.equals(r1)) {
                throw new RuntimeException("[oracle:anchor-array-char] metamorphic violation: expected documented join result input=ARRAY_LIST lhs=" + String.valueOf(r1) + " rhs=" + TEXT_LIST_CHAR);
            }

            String r2 = StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR);
            if (!"".equals(r2)) {
                throw new RuntimeException("[oracle:anchor-empty-array-char] metamorphic violation: expected empty join result input=EMPTY_ARRAY_LIST lhs=" + String.valueOf(r2) + " rhs=");
            }

            String r3 = StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1);
            if (!"/".equals(r3)) {
                throw new RuntimeException("[oracle:anchor-mixed-char-range] metamorphic violation: expected documented join result input=MIXED_ARRAY_LIST lhs=" + String.valueOf(r3) + " rhs=/");
            }

            String r4 = StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1);
            if (!"foo".equals(r4)) {
                throw new RuntimeException("[oracle:anchor-mixed-type-char-range] metamorphic violation: expected documented join result input=MIXED_TYPE_LIST lhs=" + String.valueOf(r4) + " rhs=foo");
            }

            String r5 = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            if (!"null".equals(r5)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-char] metamorphic violation: expected documented join result for toString()==null input=NULL_TO_STRING_LIST lhs=" + String.valueOf(r5) + " rhs=null");
            }

            String r6 = StringUtils.join((Object[]) null);
            if (r6 != null) {
                throw new RuntimeException("[oracle:anchor-null-array-object] metamorphic violation: expected null for join((Object[])null) input=null lhs=" + String.valueOf(r6) + " rhs=null");
            }

            String r7 = StringUtils.join((Object) null);
            if (!"".equals(r7)) {
                throw new RuntimeException("[oracle:anchor-single-null-object] metamorphic violation: expected empty string for join((Object)null) input=(Object)null lhs=" + String.valueOf(r7) + " rhs=");
            }

            String r8 = StringUtils.join(EMPTY_ARRAY_LIST);
            if (!"".equals(r8)) {
                throw new RuntimeException("[oracle:anchor-empty-array-object] metamorphic violation: expected empty string for join(EMPTY_ARRAY_LIST) input=EMPTY_ARRAY_LIST lhs=" + String.valueOf(r8) + " rhs=");
            }

            String r9 = StringUtils.join(NULL_ARRAY_LIST);
            if (!"".equals(r9)) {
                throw new RuntimeException("[oracle:anchor-null-array-element-object] metamorphic violation: expected empty string for join(NULL_ARRAY_LIST) input=NULL_ARRAY_LIST lhs=" + String.valueOf(r9) + " rhs=");
            }

            String r10 = StringUtils.join(NULL_TO_STRING_LIST);
            if (!"null".equals(r10)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-object] metamorphic violation: expected documented join result for varargs/object overload input=NULL_TO_STRING_LIST lhs=" + String.valueOf(r10) + " rhs=null");
            }

            String r11 = StringUtils.join(new String[] { null, "a", "" });
            if (!"a".equals(r11)) {
                throw new RuntimeException("[oracle:anchor-null-leading-object] metamorphic violation: expected documented join result input=[null,a,\"\"] lhs=" + String.valueOf(r11) + " rhs=a");
            }

            String r12 = StringUtils.join(MIXED_ARRAY_LIST);
            if (!"foo".equals(r12)) {
                throw new RuntimeException("[oracle:anchor-mixed-array-object] metamorphic violation: expected documented join result input=MIXED_ARRAY_LIST lhs=" + String.valueOf(r12) + " rhs=foo");
            }

            String r13 = StringUtils.join(MIXED_TYPE_LIST);
            if (!"foo2".equals(r13)) {
                throw new RuntimeException("[oracle:anchor-mixed-type-object] metamorphic violation: expected documented join result input=MIXED_TYPE_LIST lhs=" + String.valueOf(r13) + " rhs=foo2");
            }
        } catch (IllegalArgumentException e) {
            return;
        } catch (NumberFormatException e) {
            return;
        } catch (RuntimeException t) {
            boolean throughJoin = false;
            StackTraceElement[] st = t.getStackTrace();
            if (st != null) {
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.lang3.StringUtils".equals(st[i].getClassName())
                            && "join".equals(st[i].getMethodName())) {
                        throughJoin = true;
                        break;
                    }
                }
            }
            if (t instanceof NullPointerException && throughJoin) {
                throw t;
            }
            if (t.getClass().getName().contains("Invalid") || t.getClass().getName().contains("Validation")) {
                return;
            }
            return;
        }

        /* Shared EMPTY reader agreement checks.
           The docs for trimToEmpty/stripToEmpty/substring/left explicitly say they return the empty
           string on these inputs; they all share StringUtils.EMPTY, so they must agree on the value. */
        try {
            if (!StringUtils.EMPTY.equals(StringUtils.trimToEmpty(null))) {
                throw new RuntimeException("[oracle:empty-trim] metamorphic violation: trimToEmpty(null) must equal EMPTY input=null lhs=" + String.valueOf(StringUtils.trimToEmpty(null)) + " rhs=" + StringUtils.EMPTY);
            }
            if (!StringUtils.EMPTY.equals(StringUtils.stripToEmpty(null))) {
                throw new RuntimeException("[oracle:empty-strip] metamorphic violation: stripToEmpty(null) must equal EMPTY input=null lhs=" + String.valueOf(StringUtils.stripToEmpty(null)) + " rhs=" + StringUtils.EMPTY);
            }
            if (!StringUtils.EMPTY.equals(StringUtils.substring("", 0))) {
                throw new RuntimeException("[oracle:empty-sub1] metamorphic violation: substring(\"\",0) must equal EMPTY input=\"\" lhs=" + String.valueOf(StringUtils.substring("", 0)) + " rhs=" + StringUtils.EMPTY);
            }
            if (!StringUtils.EMPTY.equals(StringUtils.substring("", 0, 0))) {
                throw new RuntimeException("[oracle:empty-sub2] metamorphic violation: substring(\"\",0,0) must equal EMPTY input=\"\" lhs=" + String.valueOf(StringUtils.substring("", 0, 0)) + " rhs=" + StringUtils.EMPTY);
            }
            if (!StringUtils.EMPTY.equals(StringUtils.left("abc", -1))) {
                throw new RuntimeException("[oracle:empty-left] metamorphic violation: left(\"abc\",-1) must equal EMPTY input=\"abc\" lhs=" + String.valueOf(StringUtils.left("abc", -1)) + " rhs=" + StringUtils.EMPTY);
            }
        } catch (RuntimeException ignored) {
        }

        int len = data.consumeInt(1, 6);
        Object[] arr = new Object[len];
        for (int i = 0; i < len; i++) {
            int kind = data.consumeInt(0, 4);
            if (kind == 0) {
                arr[i] = null;
            } else if (kind == 1) {
                arr[i] = data.consumeString(16);
            } else if (kind == 2) {
                arr[i] = Long.valueOf(data.consumeInt(-1000000, 1000000));
            } else if (kind == 3) {
                arr[i] = data.consumeAsciiString(16);
            } else {
                arr[i] = Boolean.valueOf(data.consumeBoolean());
            }
        }

        final int specialIndex = data.consumeInt(0, len - 1);
        arr[specialIndex] = new Object() {
            @Override
            public String toString() {
                return null;
            }
        };

        int a = data.consumeInt(0, len);
        int b = data.consumeInt(0, len);
        int startIndex = Math.min(a, b);
        int endIndex = Math.max(a, b);
        if (startIndex == endIndex) {
            endIndex = Math.min(len, startIndex + 1);
            if (startIndex >= endIndex) {
                startIndex = 0;
                endIndex = Math.min(1, len);
            }
        }

        if (specialIndex < startIndex || specialIndex >= endIndex) {
            startIndex = specialIndex;
            endIndex = Math.min(len, specialIndex + 1);
        }

        char sepChar = (char) data.consumeInt(1, 127);
        String sepString = data.consumeBoolean() ? null : data.consumeString(8);

        try {
            String charJoin = StringUtils.join(arr, sepChar, startIndex, endIndex);
            String stringJoin = StringUtils.join(arr, String.valueOf(sepChar), startIndex, endIndex);

            /* Equivalent-input oracle:
               the char-separator and one-character String-separator overloads document the same joining
               behavior on the same slice, so for any valid constructed input their outputs must match. */
            if (charJoin != null && stringJoin != null && !charJoin.equals(stringJoin)) {
                throw new RuntimeException("[oracle:char-vs-string] metamorphic violation: join(array,char,start,end) must equal join(array,String.valueOf(char),start,end) input=start=" + startIndex + ",end=" + endIndex + ",sep=" + sepChar + " lhs=" + charJoin + " rhs=" + stringJoin);
            }
        } catch (IllegalArgumentException e) {
            return;
        } catch (NumberFormatException e) {
            return;
        } catch (RuntimeException t) {
            boolean throughJoin = false;
            StackTraceElement[] st = t.getStackTrace();
            if (st != null) {
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.lang3.StringUtils".equals(st[i].getClassName())
                            && "join".equals(st[i].getMethodName())) {
                        throughJoin = true;
                        break;
                    }
                }
            }
            if (t instanceof NullPointerException && throughJoin) {
                throw t;
            }
            if (t.getClass().getName().contains("Invalid") || t.getClass().getName().contains("Validation")) {
                return;
            }
            return;
        }

        try {
            String nullSep = StringUtils.join(arr, (String) null, startIndex, endIndex);
            String emptySep = StringUtils.join(arr, "", startIndex, endIndex);

            /* Documented guarantee from join(Object[],String,...):
               "if (separator == null) separator = EMPTY", therefore joining with null separator must
               produce exactly the same result as joining with "" on the same valid slice. */
            if (nullSep != null && emptySep != null && !nullSep.equals(emptySep)) {
                throw new RuntimeException("[oracle:null-sep-eq] metamorphic violation: join(array,null,start,end) must equal join(array,\"\",start,end) input=start=" + startIndex + ",end=" + endIndex + " lhs=" + nullSep + " rhs=" + emptySep);
            }
        } catch (IllegalArgumentException e) {
            return;
        } catch (NumberFormatException e) {
            return;
        } catch (RuntimeException t) {
            boolean throughJoin = false;
            StackTraceElement[] st = t.getStackTrace();
            if (st != null) {
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.lang3.StringUtils".equals(st[i].getClassName())
                            && "join".equals(st[i].getMethodName())) {
                        throughJoin = true;
                        break;
                    }
                }
            }
            if (t instanceof NullPointerException && throughJoin) {
                throw t;
            }
            if (t.getClass().getName().contains("Invalid") || t.getClass().getName().contains("Validation")) {
                return;
            }
        }

        try {
            String direct = StringUtils.join(arr, sepString, startIndex, endIndex);
            String objectApi = StringUtils.join(new Object[] { arr[specialIndex] });
            if (specialIndex >= startIndex && specialIndex < endIndex && endIndex - startIndex == 1) {
                if (direct != null && objectApi != null && !"null".equals(direct)) {
                    throw new RuntimeException("[oracle:single-null-tostring] metamorphic violation: single-element join with toString()==null must produce \"null\" input=index=" + specialIndex + ",sep=" + String.valueOf(sepString) + " lhs=" + direct + " rhs=null");
                }
            }
        } catch (IllegalArgumentException e) {
            return;
        } catch (NumberFormatException e) {
            return;
        } catch (RuntimeException t) {
            boolean throughJoin = false;
            StackTraceElement[] st = t.getStackTrace();
            if (st != null) {
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.lang3.StringUtils".equals(st[i].getClassName())
                            && "join".equals(st[i].getMethodName())) {
                        throughJoin = true;
                        break;
                    }
                }
            }
            if (t instanceof NullPointerException && throughJoin) {
                throw t;
            }
            if (t.getClass().getName().contains("Invalid") || t.getClass().getName().contains("Validation")) {
                return;
            }
        }
    }
}
```

</details>

---
## [34] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [35] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~17,153 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final String[] ARRAY_LIST = { "foo", "bar", "baz" };
        final String[] EMPTY_ARRAY_LIST = {};
        final String[] NULL_ARRAY_LIST = { null };
        final String[] MIXED_ARRAY_LIST = { null, "", "foo" };
        final Object[] MIXED_TYPE_LIST = { "foo", Long.valueOf(2L) };
        final char SEPARATOR_CHAR = ';';
        final String TEXT_LIST_CHAR = "foo;bar;baz";
        final Object[] NULL_TO_STRING_LIST = {
            new Object() {
                @Override
                public String toString() {
                    return null;
                }
            }
        };

        /* Anchor checks copied from the real tests: these are valid-by-construction inputs.
           The documented behavior requires these calls to succeed and produce the asserted values.
           A "fix" that only suppresses the exception or skips appending would violate these results. */
        try {
            String r0 = StringUtils.join((Object[]) null, ',');
            if (r0 != null) {
                throw new RuntimeException("[oracle:anchor-null-array-char] metamorphic violation: expected null for join((Object[])null, ',') input=null lhs=" + String.valueOf(r0) + " rhs=null");
            }

            String r1 = StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR);
            if (!TEXT_LIST_CHAR.equals(r1)) {
                throw new RuntimeException("[oracle:anchor-array-char] metamorphic violation: expected documented join result input=ARRAY_LIST lhs=" + String.valueOf(r1) + " rhs=" + TEXT_LIST_CHAR);
            }

            String r2 = StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR);
            if (!"".equals(r2)) {
                throw new RuntimeException("[oracle:anchor-empty-array-char] metamorphic violation: expected empty join result input=EMPTY_ARRAY_LIST lhs=" + String.valueOf(r2) + " rhs=");
            }

            String r3 = StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1);
            if (!"/".equals(r3)) {
                throw new RuntimeException("[oracle:anchor-mixed-char-range] metamorphic violation: expected documented join result input=MIXED_ARRAY_LIST lhs=" + String.valueOf(r3) + " rhs=/");
            }

            String r4 = StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1);
            if (!"foo".equals(r4)) {
                throw new RuntimeException("[oracle:anchor-mixed-type-char-range] metamorphic violation: expected documented join result input=MIXED_TYPE_LIST lhs=" + String.valueOf(r4) + " rhs=foo");
            }

            String r5 = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            if (!"null".equals(r5)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-char] metamorphic violation: expected documented join result for toString()==null input=NULL_TO_STRING_LIST lhs=" + String.valueOf(r5) + " rhs=null");
            }

            String r6 = StringUtils.join((Object[]) null);
            if (r6 != null) {
                throw new RuntimeException("[oracle:anchor-null-array-object] metamorphic violation: expected null for join((Object[])null) input=null lhs=" + String.valueOf(r6) + " rhs=null");
            }

            String r7 = StringUtils.join((Object) null);
            if (!"".equals(r7)) {
                throw new RuntimeException("[oracle:anchor-single-null-object] metamorphic violation: expected empty string for join((Object)null) input=(Object)null lhs=" + String.valueOf(r7) + " rhs=");
            }

            String r8 = StringUtils.join(EMPTY_ARRAY_LIST);
            if (!"".equals(r8)) {
                throw new RuntimeException("[oracle:anchor-empty-array-object] metamorphic violation: expected empty string for join(EMPTY_ARRAY_LIST) input=EMPTY_ARRAY_LIST lhs=" + String.valueOf(r8) + " rhs=");
            }

            String r9 = StringUtils.join(NULL_ARRAY_LIST);
            if (!"".equals(r9)) {
                throw new RuntimeException("[oracle:anchor-null-array-element-object] metamorphic violation: expected empty string for join(NULL_ARRAY_LIST) input=NULL_ARRAY_LIST lhs=" + String.valueOf(r9) + " rhs=");
            }

            String r10 = StringUtils.join(NULL_TO_STRING_LIST);
            if (!"null".equals(r10)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-object] metamorphic violation: expected documented join result for varargs/object overload input=NULL_TO_STRING_LIST lhs=" + String.valueOf(r10) + " rhs=null");
            }

            String r11 = StringUtils.join(new String[] { null, "a", "" });
            if (!"a".equals(r11)) {
                throw new RuntimeException("[oracle:anchor-null-leading-object] metamorphic violation: expected documented join result input=[null,a,\"\"] lhs=" + String.valueOf(r11) + " rhs=a");
            }

            String r12 = StringUtils.join(MIXED_ARRAY_LIST);
            if (!"foo".equals(r12)) {
                throw new RuntimeException("[oracle:anchor-mixed-array-object] metamorphic violation: expected documented join result input=MIXED_ARRAY_LIST lhs=" + String.valueOf(r12) + " rhs=foo");
            }

            String r13 = StringUtils.join(MIXED_TYPE_LIST);
            if (!"foo2".equals(r13)) {
                throw new RuntimeException("[oracle:anchor-mixed-type-object] metamorphic violation: expected documented join result input=MIXED_TYPE_LIST lhs=" + String.valueOf(r13) + " rhs=foo2");
            }
        } catch (IllegalArgumentException e) {
            return;
        } catch (NumberFormatException e) {
            return;
        } catch (RuntimeException t) {
            boolean throughJoin = false;
            StackTraceElement[] st = t.getStackTrace();
            if (st != null) {
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.lang3.StringUtils".equals(st[i].getClassName())
                            && "join".equals(st[i].getMethodName())) {
                        throughJoin = true;
                        break;
                    }
                }
            }
            if (t instanceof NullPointerException && throughJoin) {
                throw t;
            }
            if (t.getClass().getName().contains("Invalid") || t.getClass().getName().contains("Validation")) {
                return;
            }
            return;
        }

        /* Shared EMPTY reader agreement checks.
           The docs for trimToEmpty/stripToEmpty/substring/left explicitly say they return the empty
           string on these inputs; they all share StringUtils.EMPTY, so they must agree on the value. */
        try {
            if (!StringUtils.EMPTY.equals(StringUtils.trimToEmpty(null))) {
                throw new RuntimeException("[oracle:empty-trim] metamorphic violation: trimToEmpty(null) must equal EMPTY input=null lhs=" + String.valueOf(StringUtils.trimToEmpty(null)) + " rhs=" + StringUtils.EMPTY);
            }
            if (!StringUtils.EMPTY.equals(StringUtils.stripToEmpty(null))) {
                throw new RuntimeException("[oracle:empty-strip] metamorphic violation: stripToEmpty(null) must equal EMPTY input=null lhs=" + String.valueOf(StringUtils.stripToEmpty(null)) + " rhs=" + StringUtils.EMPTY);
            }
            if (!StringUtils.EMPTY.equals(StringUtils.substring("", 0))) {
                throw new RuntimeException("[oracle:empty-sub1] metamorphic violation: substring(\"\",0) must equal EMPTY input=\"\" lhs=" + String.valueOf(StringUtils.substring("", 0)) + " rhs=" + StringUtils.EMPTY);
            }
            if (!StringUtils.EMPTY.equals(StringUtils.substring("", 0, 0))) {
                throw new RuntimeException("[oracle:empty-sub2] metamorphic violation: substring(\"\",0,0) must equal EMPTY input=\"\" lhs=" + String.valueOf(StringUtils.substring("", 0, 0)) + " rhs=" + StringUtils.EMPTY);
            }
            if (!StringUtils.EMPTY.equals(StringUtils.left("abc", -1))) {
                throw new RuntimeException("[oracle:empty-left] metamorphic violation: left(\"abc\",-1) must equal EMPTY input=\"abc\" lhs=" + String.valueOf(StringUtils.left("abc", -1)) + " rhs=" + StringUtils.EMPTY);
            }
        } catch (RuntimeException ignored) {
        }

        int len = data.consumeInt(1, 6);
        Object[] arr = new Object[len];
        for (int i = 0; i < len; i++) {
            int kind = data.consumeInt(0, 4);
            if (kind == 0) {
                arr[i] = null;
            } else if (kind == 1) {
                arr[i] = data.consumeString(16);
            } else if (kind == 2) {
                arr[i] = Long.valueOf(data.consumeInt(-1000000, 1000000));
            } else if (kind == 3) {
                arr[i] = data.consumeAsciiString(16);
            } else {
                arr[i] = Boolean.valueOf(data.consumeBoolean());
            }
        }

        final int specialIndex = data.consumeInt(0, len - 1);
        arr[specialIndex] = new Object() {
            @Override
            public String toString() {
                return null;
            }
        };

        int a = data.consumeInt(0, len);
        int b = data.consumeInt(0, len);
        int startIndex = Math.min(a, b);
        int endIndex = Math.max(a, b);
        if (startIndex == endIndex) {
            endIndex = Math.min(len, startIndex + 1);
            if (startIndex >= endIndex) {
                startIndex = 0;
                endIndex = Math.min(1, len);
            }
        }

        if (specialIndex < startIndex || specialIndex >= endIndex) {
            startIndex = specialIndex;
            endIndex = Math.min(len, specialIndex + 1);
        }

        char sepChar = (char) data.consumeInt(1, 127);
        String sepString = data.consumeBoolean() ? null : data.consumeString(8);

        try {
            String charJoin = StringUtils.join(arr, sepChar, startIndex, endIndex);
            String stringJoin = StringUtils.join(arr, String.valueOf(sepChar), startIndex, endIndex);

            /* Equivalent-input oracle:
               the char-separator and one-character String-separator overloads document the same joining
               behavior on the same slice, so for any valid constructed input their outputs must match. */
            if (charJoin != null && stringJoin != null && !charJoin.equals(stringJoin)) {
                throw new RuntimeException("[oracle:char-vs-string] metamorphic violation: join(array,char,start,end) must equal join(array,String.valueOf(char),start,end) input=start=" + startIndex + ",end=" + endIndex + ",sep=" + sepChar + " lhs=" + charJoin + " rhs=" + stringJoin);
            }
        } catch (IllegalArgumentException e) {
            return;
        } catch (NumberFormatException e) {
            return;
        } catch (RuntimeException t) {
            boolean throughJoin = false;
            StackTraceElement[] st = t.getStackTrace();
            if (st != null) {
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.lang3.StringUtils".equals(st[i].getClassName())
                            && "join".equals(st[i].getMethodName())) {
                        throughJoin = true;
                        break;
                    }
                }
            }
            if (t instanceof NullPointerException && throughJoin) {
                throw t;
            }
            if (t.getClass().getName().contains("Invalid") || t.getClass().getName().contains("Validation")) {
                return;
            }
            return;
        }

        try {
            String nullSep = StringUtils.join(arr, (String) null, startIndex, endIndex);
            String emptySep = StringUtils.join(arr, "", startIndex, endIndex);

            /* Documented guarantee from join(Object[],String,...):
               "if (separator == null) separator = EMPTY", therefore joining with null separator must
               produce exactly the same result as joining with "" on the same valid slice. */
            if (nullSep != null && emptySep != null && !nullSep.equals(emptySep)) {
                throw new RuntimeException("[oracle:null-sep-eq] metamorphic violation: join(array,null,start,end) must equal join(array,\"\",start,end) input=start=" + startIndex + ",end=" + endIndex + " lhs=" + nullSep + " rhs=" + emptySep);
            }
        } catch (IllegalArgumentException e) {
            return;
        } catch (NumberFormatException e) {
            return;
        } catch (RuntimeException t) {
            boolean throughJoin = false;
            StackTraceElement[] st = t.getStackTrace();
            if (st != null) {
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.lang3.StringUtils".equals(st[i].getClassName())
                            && "join".equals(st[i].getMethodName())) {
                        throughJoin = true;
                        break;
                    }
                }
            }
            if (t instanceof NullPointerException && throughJoin) {
                throw t;
            }
            if (t.getClass().getName().contains("Invalid") || t.getClass().getName().contains("Validation")) {
                return;
            }
        }

        try {
            String direct = StringUtils.join(arr, sepString, startIndex, endIndex);
            String objectApi = StringUtils.join(new Object[] { arr[specialIndex] });
            if (specialIndex >= startIndex && specialIndex < endIndex && endIndex - startIndex == 1) {
                if (direct != null && objectApi != null && !"null".equals(direct)) {
                    throw new RuntimeException("[oracle:single-null-tostring] metamorphic violation: single-element join with toString()==null must produce \"null\" input=index=" + specialIndex + ",sep=" + String.valueOf(sepString) + " lhs=" + direct + " rhs=null");
                }
            }
        } catch (IllegalArgumentException e) {
            return;
        } catch (NumberFormatException e) {
            return;
        } catch (RuntimeException t) {
            boolean throughJoin = false;
            StackTraceElement[] st = t.getStackTrace();
            if (st != null) {
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.lang3.StringUtils".equals(st[i].getClassName())
                            && "join".equals(st[i].getMethodName())) {
                        throughJoin = true;
                        break;
                    }
                }
            }
            if (t instanceof NullPointerException && throughJoin) {
                throw t;
            }
            if (t.getClass().getName().contains("Invalid") || t.getClass().getName().contains("Validation")) {
                return;
            }
        }
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/Lang_20_buggy/fuzz/attempt_016/FuzzHarness.java:98: error: exception NumberFormatException has already been caught
        } catch (NumberFormatException e) {
          ^
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/Lang_20_buggy/fuzz/attempt_016/FuzzHarness.java:200: error: exception NumberFormatException has already been caught
        } catch (NumberFormatException e) {
          ^
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/Lang_20_buggy/fuzz/attempt_016/FuzzHarness.java:235: error: exception NumberFormatException has already been caught
        } catch (NumberFormatException e) {
          ^
/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/Lang_20_buggy/fuzz/attempt_016/FuzzHarness.java:267: error: exception NumberFormatException has already been caught
        } catch (NumberFormatException e) {
          ^
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
<details open><summary>▸ Output (~12,535 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static boolean isThroughJoin(Throwable t) {
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            if ("org.apache.commons.lang3.StringUtils".equals(st[i].getClassName())
                    && "join".equals(st[i].getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValidation(Throwable t) {
        if (t instanceof IllegalArgumentException) {
            return true;
        }
        String n = t.getClass().getName();
        return n.contains("Invalid") || n.contains("Validation");
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final String[] ARRAY_LIST = { "foo", "bar", "baz" };
        final String[] EMPTY_ARRAY_LIST = {};
        final String[] NULL_ARRAY_LIST = { null };
        final String[] MIXED_ARRAY_LIST = { null, "", "foo" };
        final Object[] MIXED_TYPE_LIST = { "foo", Long.valueOf(2L) };
        final char SEPARATOR_CHAR = ';';
        final String TEXT_LIST_CHAR = "foo;bar;baz";
        final Object[] NULL_TO_STRING_LIST = {
            new Object() {
                @Override
                public String toString() {
                    return null;
                }
            }
        };

        try {
            String r0 = StringUtils.join((Object[]) null, ',');
            if (r0 != null) {
                throw new RuntimeException("[oracle:anchor-null-array-char] metamorphic violation: expected null for join((Object[])null, ',') input=null lhs=" + String.valueOf(r0) + " rhs=null");
            }

            String r1 = StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR);
            if (!TEXT_LIST_CHAR.equals(r1)) {
                throw new RuntimeException("[oracle:anchor-array-char] metamorphic violation: expected documented join result input=ARRAY_LIST lhs=" + String.valueOf(r1) + " rhs=" + TEXT_LIST_CHAR);
            }

            String r2 = StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR);
            if (!"".equals(r2)) {
                throw new RuntimeException("[oracle:anchor-empty-array-char] metamorphic violation: expected empty join result input=EMPTY_ARRAY_LIST lhs=" + String.valueOf(r2) + " rhs=");
            }

            String r3 = StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1);
            if (!"/".equals(r3)) {
                throw new RuntimeException("[oracle:anchor-mixed-char-range] metamorphic violation: expected documented join result input=MIXED_ARRAY_LIST lhs=" + String.valueOf(r3) + " rhs=/");
            }

            String r4 = StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1);
            if (!"foo".equals(r4)) {
                throw new RuntimeException("[oracle:anchor-mixed-type-char-range] metamorphic violation: expected documented join result input=MIXED_TYPE_LIST lhs=" + String.valueOf(r4) + " rhs=foo");
            }

            String r5 = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            if (!"null".equals(r5)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-char] metamorphic violation: expected documented join result for toString()==null input=NULL_TO_STRING_LIST lhs=" + String.valueOf(r5) + " rhs=null");
            }

            String r6 = StringUtils.join((Object[]) null);
            if (r6 != null) {
                throw new RuntimeException("[oracle:anchor-null-array-object] metamorphic violation: expected null for join((Object[])null) input=null lhs=" + String.valueOf(r6) + " rhs=null");
            }

            String r7 = StringUtils.join((Object) null);
            if (!"".equals(r7)) {
                throw new RuntimeException("[oracle:anchor-single-null-object] metamorphic violation: expected empty string for join((Object)null) input=(Object)null lhs=" + String.valueOf(r7) + " rhs=");
            }

            String r8 = StringUtils.join(EMPTY_ARRAY_LIST);
            if (!"".equals(r8)) {
                throw new RuntimeException("[oracle:anchor-empty-array-object] metamorphic violation: expected empty string for join(EMPTY_ARRAY_LIST) input=EMPTY_ARRAY_LIST lhs=" + String.valueOf(r8) + " rhs=");
            }

            String r9 = StringUtils.join(NULL_ARRAY_LIST);
            if (!"".equals(r9)) {
                throw new RuntimeException("[oracle:anchor-null-array-element-object] metamorphic violation: expected empty string for join(NULL_ARRAY_LIST) input=NULL_ARRAY_LIST lhs=" + String.valueOf(r9) + " rhs=");
            }

            String r10 = StringUtils.join(NULL_TO_STRING_LIST);
            if (!"null".equals(r10)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-object] metamorphic violation: expected documented join result for varargs/object overload input=NULL_TO_STRING_LIST lhs=" + String.valueOf(r10) + " rhs=null");
            }

            String r11 = StringUtils.join(new String[] { null, "a", "" });
            if (!"a".equals(r11)) {
                throw new RuntimeException("[oracle:anchor-null-leading-object] metamorphic violation: expected documented join result input=[null,a,\"\"] lhs=" + String.valueOf(r11) + " rhs=a");
            }

            String r12 = StringUtils.join(MIXED_ARRAY_LIST);
            if (!"foo".equals(r12)) {
                throw new RuntimeException("[oracle:anchor-mixed-array-object] metamorphic violation: expected documented join result input=MIXED_ARRAY_LIST lhs=" + String.valueOf(r12) + " rhs=foo");
            }

            String r13 = StringUtils.join(MIXED_TYPE_LIST);
            if (!"foo2".equals(r13)) {
                throw new RuntimeException("[oracle:anchor-mixed-type-object] metamorphic violation: expected documented join result input=MIXED_TYPE_LIST lhs=" + String.valueOf(r13) + " rhs=foo2");
            }
        } catch (RuntimeException t) {
            if (t instanceof NullPointerException && isThroughJoin(t)) {
                throw t;
            }
            if (isValidation(t)) {
                return;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        }

        try {
            if (!StringUtils.EMPTY.equals(StringUtils.trimToEmpty(null))) {
                throw new RuntimeException("[oracle:empty-trim] metamorphic violation: trimToEmpty(null) must equal EMPTY input=null lhs=" + String.valueOf(StringUtils.trimToEmpty(null)) + " rhs=" + StringUtils.EMPTY);
            }
            if (!StringUtils.EMPTY.equals(StringUtils.stripToEmpty(null))) {
                throw new RuntimeException("[oracle:empty-strip] metamorphic violation: stripToEmpty(null) must equal EMPTY input=null lhs=" + String.valueOf(StringUtils.stripToEmpty(null)) + " rhs=" + StringUtils.EMPTY);
            }
            if (!StringUtils.EMPTY.equals(StringUtils.substring("", 0))) {
                throw new RuntimeException("[oracle:empty-sub1] metamorphic violation: substring(\"\",0) must equal EMPTY input=\"\" lhs=" + String.valueOf(StringUtils.substring("", 0)) + " rhs=" + StringUtils.EMPTY);
            }
            if (!StringUtils.EMPTY.equals(StringUtils.substring("", 0, 0))) {
                throw new RuntimeException("[oracle:empty-sub2] metamorphic violation: substring(\"\",0,0) must equal EMPTY input=\"\" lhs=" + String.valueOf(StringUtils.substring("", 0, 0)) + " rhs=" + StringUtils.EMPTY);
            }
            if (!StringUtils.EMPTY.equals(StringUtils.left("abc", -1))) {
                throw new RuntimeException("[oracle:empty-left] metamorphic violation: left(\"abc\",-1) must equal EMPTY input=\"abc\" lhs=" + String.valueOf(StringUtils.left("abc", -1)) + " rhs=" + StringUtils.EMPTY);
            }
        } catch (RuntimeException t) {
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
        }

        int len = data.consumeInt(1, 6);
        Object[] arr = new Object[len];
        for (int i = 0; i < len; i++) {
            int kind = data.consumeInt(0, 4);
            if (kind == 0) {
                arr[i] = null;
            } else if (kind == 1) {
                arr[i] = data.consumeString(16);
            } else if (kind == 2) {
                arr[i] = Long.valueOf(data.consumeInt(-1000000, 1000000));
            } else if (kind == 3) {
                arr[i] = data.consumeAsciiString(16);
            } else {
                arr[i] = Boolean.valueOf(data.consumeBoolean());
            }
        }

        final int specialIndex = data.consumeInt(0, len - 1);
        arr[specialIndex] = new Object() {
            @Override
            public String toString() {
                return null;
            }
        };

        int a = data.consumeInt(0, len);
        int b = data.consumeInt(0, len);
        int startIndex = Math.min(a, b);
        int endIndex = Math.max(a, b);
        if (startIndex == endIndex) {
            endIndex = Math.min(len, startIndex + 1);
            if (startIndex >= endIndex) {
                startIndex = 0;
                endIndex = Math.min(1, len);
            }
        }

        if (specialIndex < startIndex || specialIndex >= endIndex) {
            startIndex = specialIndex;
            endIndex = Math.min(len, specialIndex + 1);
        }

        char sepChar = (char) data.consumeInt(1, 127);
        String sepString = data.consumeBoolean() ? null : data.consumeString(8);

        try {
            String charJoin = StringUtils.join(arr, sepChar, startIndex, endIndex);
            String stringJoin = StringUtils.join(arr, String.valueOf(sepChar), startIndex, endIndex);

            if (charJoin != null && stringJoin != null && !charJoin.equals(stringJoin)) {
                throw new RuntimeException("[oracle:char-vs-string] metamorphic violation: join(array,char,start,end) must equal join(array,String.valueOf(char),start,end) input=start=" + startIndex + ",end=" + endIndex + ",sep=" + sepChar + " lhs=" + charJoin + " rhs=" + stringJoin);
            }
        } catch (RuntimeException t) {
            if (t instanceof NullPointerException && isThroughJoin(t)) {
                throw t;
            }
            if (isValidation(t)) {
                return;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        }

        try {
            String nullSep = StringUtils.join(arr, (String) null, startIndex, endIndex);
            String emptySep = StringUtils.join(arr, "", startIndex, endIndex);

            if (nullSep != null && emptySep != null && !nullSep.equals(emptySep)) {
                throw new RuntimeException("[oracle:null-sep-eq] metamorphic violation: join(array,null,start,end) must equal join(array,\"\",start,end) input=start=" + startIndex + ",end=" + endIndex + " lhs=" + nullSep + " rhs=" + emptySep);
            }
        } catch (RuntimeException t) {
            if (t instanceof NullPointerException && isThroughJoin(t)) {
                throw t;
            }
            if (isValidation(t)) {
                return;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        }

        try {
            String direct = StringUtils.join(arr, sepString, startIndex, endIndex);
            if (specialIndex >= startIndex && specialIndex < endIndex && endIndex - startIndex == 1) {
                if (!"null".equals(direct)) {
                    throw new RuntimeException("[oracle:single-null-tostring] metamorphic violation: single-element join with toString()==null must produce \"null\" input=index=" + specialIndex + ",sep=" + String.valueOf(sepString) + " lhs=" + String.valueOf(direct) + " rhs=null");
                }
            }
        } catch (RuntimeException t) {
            if (t instanceof NullPointerException && isThroughJoin(t)) {
                throw t;
            }
            if (isValidation(t)) {
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
<details open><summary>▸ Output (~14,630 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final String[] ARRAY_LIST = { "foo", "bar", "baz" };
        final String[] EMPTY_ARRAY_LIST = {};
        final String[] NULL_ARRAY_LIST = { null };
        final String[] MIXED_ARRAY_LIST = { null, "", "foo" };
        final Object[] MIXED_TYPE_LIST = { "foo", Long.valueOf(2L) };
        final char SEPARATOR_CHAR = ';';
        final String TEXT_LIST_CHAR = "foo;bar;baz";
        final Object[] NULL_TO_STRING_LIST = { new Object() { @Override public String toString() { return null; } } };

        // ANCHOR: faithfully mirror the regression tests first.

        try {
            String r = StringUtils.join((Object[]) null, ',');
            if (r != null) {
                throw new RuntimeException("[oracle:anchor-null-array-char] metamorphic violation: documented null-array contract input=null lhs=" + r + " rhs=null");
            }
        } catch (Throwable t) {
            boolean root = t instanceof NullPointerException;
            if (root) {
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                        throw (RuntimeException) t;
                    }
                }
            }
            return;
        }

        try {
            String r = StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR);
            if (!TEXT_LIST_CHAR.equals(r)) {
                throw new RuntimeException("[oracle:anchor-array-char] metamorphic violation: regression-test output input=ARRAY_LIST lhs=" + r + " rhs=" + TEXT_LIST_CHAR);
            }
        } catch (Throwable t) {
            return;
        }

        try {
            String r = StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR);
            if (!"".equals(r)) {
                throw new RuntimeException("[oracle:anchor-empty-array-char] metamorphic violation: documented empty-array contract input=EMPTY_ARRAY_LIST lhs=" + r + " rhs=");
            }
        } catch (Throwable t) {
            return;
        }

        try {
            String r = StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR);
            if (!";;foo".equals(r)) {
                throw new RuntimeException("[oracle:anchor-mixed-array-char] metamorphic violation: regression-test output input=MIXED_ARRAY_LIST lhs=" + r + " rhs=;;foo");
            }
        } catch (Throwable t) {
            return;
        }

        try {
            String r = StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR);
            if (!"foo;2".equals(r)) {
                throw new RuntimeException("[oracle:anchor-mixed-type-char] metamorphic violation: regression-test output input=MIXED_TYPE_LIST lhs=" + r + " rhs=foo;2");
            }
        } catch (Throwable t) {
            return;
        }

        try {
            String r = StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1);
            if (!"/".equals(r)) {
                throw new RuntimeException("[oracle:anchor-range-char-1] metamorphic violation: regression-test output input=MIXED_ARRAY_LIST lhs=" + r + " rhs=/");
            }
        } catch (Throwable t) {
            return;
        }

        try {
            String r = StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1);
            if (!"foo".equals(r)) {
                throw new RuntimeException("[oracle:anchor-range-char-2] metamorphic violation: regression-test output input=MIXED_TYPE_LIST lhs=" + r + " rhs=foo");
            }
        } catch (Throwable t) {
            return;
        }

        try {
            String r = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            if (!"null".equals(r)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-char] metamorphic violation: regression-test output input=NULL_TO_STRING_LIST lhs=" + r + " rhs=null");
            }
        } catch (Throwable t) {
            boolean root = t instanceof NullPointerException;
            if (root) {
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                        throw (RuntimeException) t;
                    }
                }
            }
            return;
        }

        try {
            String r = StringUtils.join((Object[]) null);
            if (r != null) {
                throw new RuntimeException("[oracle:anchor-null-array-varargs] metamorphic violation: documented null-array contract input=null lhs=" + r + " rhs=null");
            }
        } catch (Throwable t) {
            return;
        }

        try {
            String r = StringUtils.join();
            if (!"".equals(r)) {
                throw new RuntimeException("[oracle:anchor-empty-varargs] metamorphic violation: documented empty-varargs contract input=empty lhs=" + r + " rhs=");
            }
        } catch (Throwable t) {
            return;
        }

        try {
            String r = StringUtils.join((Object) null);
            if (!"".equals(r)) {
                throw new RuntimeException("[oracle:anchor-single-null-object] metamorphic violation: documented single-null contract input=(Object)null lhs=" + r + " rhs=");
            }
        } catch (Throwable t) {
            return;
        }

        try {
            String r = StringUtils.join(EMPTY_ARRAY_LIST);
            if (!"".equals(r)) {
                throw new RuntimeException("[oracle:anchor-empty-array] metamorphic violation: documented empty-array contract input=EMPTY_ARRAY_LIST lhs=" + r + " rhs=");
            }
        } catch (Throwable t) {
            return;
        }

        try {
            String r = StringUtils.join(NULL_ARRAY_LIST);
            if (!"".equals(r)) {
                throw new RuntimeException("[oracle:anchor-null-array-elem] metamorphic violation: regression-test output input=NULL_ARRAY_LIST lhs=" + r + " rhs=");
            }
        } catch (Throwable t) {
            return;
        }

        try {
            String r = StringUtils.join(NULL_TO_STRING_LIST);
            if (!"null".equals(r)) {
                throw new RuntimeException("[oracle:anchor-null-tostring-varargs] metamorphic violation: regression-test output input=NULL_TO_STRING_LIST lhs=" + r + " rhs=null");
            }
        } catch (Throwable t) {
            boolean root = t instanceof NullPointerException;
            if (root) {
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                        throw (RuntimeException) t;
                    }
                }
            }
            return;
        }

        // Shared-state agreement on EMPTY:
        // join returns EMPTY for empty ranges/inputs; trimToEmpty/stripToEmpty/substring/left also report EMPTY.
        // A "fix" that bypasses the intended EMPTY value or corrupts that shared state could avoid crashes yet
        // still break observable behaviour. These readers must agree on the same EMPTY instance/value.
        try {
            String e1 = StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1);
            String e2 = StringUtils.trimToEmpty(null);
            String e3 = StringUtils.stripToEmpty(null);
            String e4 = StringUtils.substring("", 0);
            String e5 = StringUtils.substring("", 0, 0);
            String e6 = StringUtils.left("x", -1);
            if (!(StringUtils.EMPTY.equals(e1) && StringUtils.EMPTY.equals(e2) && StringUtils.EMPTY.equals(e3)
                    && StringUtils.EMPTY.equals(e4) && StringUtils.EMPTY.equals(e5) && StringUtils.EMPTY.equals(e6))) {
                throw new RuntimeException("[oracle:empty-shared-state] metamorphic violation: EMPTY readers disagree input=shared-field lhs="
                        + e1 + "|" + e2 + "|" + e3 + "|" + e4 + "|" + e5 + "|" + e6 + " rhs=" + StringUtils.EMPTY);
            }
        } catch (Throwable t) {
            return;
        }

        // EXPLORE:
        // Root cause property: array[startIndex].toString() may return null on a valid, non-null element.
        // For any correct implementation, joining an array containing such an element must behave exactly as if
        // that element were the literal string "null", because StringBuilder.append(Object) uses String.valueOf(obj).
        // We compare only real library calls: join on the special-object array vs join on an equivalent array with
        // the corresponding element replaced by "null". Also the char and String separator overloads must agree
        // for equivalent one-character separators.

        int len = data.consumeInt(1, 8);
        Object[] fuzz = new Object[len];
        Object[] oracle = new Object[len];
        int specialIndex = data.consumeInt(0, len - 1);

        for (int i = 0; i < len; i++) {
            if (i == specialIndex) {
                Object special = new Object() { @Override public String toString() { return null; } };
                fuzz[i] = special;
                oracle[i] = "null";
            } else {
                int kind = data.consumeInt(0, 4);
                switch (kind) {
                    case 0:
                        fuzz[i] = null;
                        oracle[i] = null;
                        break;
                    case 1:
                        String s1 = data.consumeString(12);
                        fuzz[i] = s1;
                        oracle[i] = s1;
                        break;
                    case 2:
                        String s2 = data.consumeAsciiString(12);
                        fuzz[i] = s2;
                        oracle[i] = s2;
                        break;
                    case 3:
                        Long v = Long.valueOf(data.consumeInt(-1000000, 1000000));
                        fuzz[i] = v;
                        oracle[i] = v;
                        break;
                    default:
                        Integer iv = Integer.valueOf(data.consumeInt(-1000000, 1000000));
                        fuzz[i] = iv;
                        oracle[i] = iv;
                        break;
                }
            }
        }

        int start = data.consumeInt(0, len - 1);
        int end = data.consumeInt(start + 1, len);
        if (!(start <= specialIndex && specialIndex < end)) {
            start = specialIndex;
            end = specialIndex + 1;
        }

        char sepChar = (char) data.consumeInt(1, 126);
        String sepString = String.valueOf(sepChar);

        String lhsChar;
        try {
            lhsChar = StringUtils.join(fuzz, sepChar, start, end);
        } catch (Throwable t) {
            boolean cleanRejection = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            if (cleanRejection) {
                return;
            }
            boolean root = t instanceof NullPointerException;
            if (root) {
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                        throw (RuntimeException) t;
                    }
                }
            }
            return;
        }

        String rhsChar;
        try {
            rhsChar = StringUtils.join(oracle, sepChar, start, end);
        } catch (Throwable t) {
            return;
        }

        if (!lhsChar.equals(rhsChar)) {
            throw new RuntimeException("[oracle:char-null-tostring] metamorphic violation: join(array,char,start,end) must equal join(arrayWithLiteralNull,char,start,end) input=start="
                    + start + ",end=" + end + ",sep=" + sepChar + " lhs=" + lhsChar + " rhs=" + rhsChar);
        }

        String lhsString;
        try {
            lhsString = StringUtils.join(fuzz, sepString, start, end);
        } catch (Throwable t) {
            boolean cleanRejection = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            if (cleanRejection) {
                return;
            }
            boolean root = t instanceof NullPointerException;
            if (root) {
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                        throw (RuntimeException) t;
                    }
                }
            }
            return;
        }

        String rhsString;
        try {
            rhsString = StringUtils.join(oracle, sepString, start, end);
        } catch (Throwable t) {
            return;
        }

        if (!lhsString.equals(rhsString)) {
            throw new RuntimeException("[oracle:string-null-tostring] metamorphic violation: join(array,String,start,end) must equal join(arrayWithLiteralNull,String,start,end) input=start="
                    + start + ",end=" + end + ",sep=" + sepString + " lhs=" + lhsString + " rhs=" + rhsString);
        }

        if (!lhsChar.equals(lhsString)) {
            throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: char and String separator overloads must agree for equivalent one-character separators input=start="
                    + start + ",end=" + end + ",sep=" + sepChar + " lhs=" + lhsChar + " rhs=" + lhsString);
        }

        try {
            String lhsVarargs = StringUtils.join(fuzz);
            String rhsVarargs = StringUtils.join(oracle);
            if (!lhsVarargs.equals(rhsVarargs)) {
                throw new RuntimeException("[oracle:varargs-null-tostring] metamorphic violation: join(array) must equal join(arrayWithLiteralNull) input=len="
                        + len + " lhs=" + lhsVarargs + " rhs=" + rhsVarargs);
            }
        } catch (Throwable t) {
            boolean root = t instanceof NullPointerException;
            if (root) {
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                        throw (RuntimeException) t;
                    }
                }
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
- trigger: java.lang.NullPointerException

---
## [39] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~12,473 chars)</summary>

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

    /*
     * Exact fixture from StringUtilsTest: this object is the real trigger because
     * StringUtils.join pre-sizes the StringBuilder using array[startIndex].toString().length().
     * A correct implementation must still later append the object, and StringBuilder.append(Object)
     * renders an Object whose toString() returns null as the literal "null".
     */
    private static final Object NULL_TO_STRING_OBJECT = new Object() {
        @Override
        public String toString() {
            return null;
        }
    };
    private static final Object[] NULL_TO_STRING_LIST = { NULL_TO_STRING_OBJECT };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchorChecks();
        runExploreChecks(data);
    }

    private static void runAnchorChecks() {
        try {
            if (StringUtils.join((Object[]) null, ',') != null) {
                throw new RuntimeException("[oracle:null-array-char] metamorphic violation: join((Object[]) null, ',') must return null");
            }
            if (StringUtils.join((Object[]) null) != null) {
                throw new RuntimeException("[oracle:null-array-varargs] metamorphic violation: join((Object[]) null) must return null");
            }
            if (!"".equals(StringUtils.join())) {
                throw new RuntimeException("[oracle:empty-varargs] metamorphic violation: join() must return empty string");
            }
            if (!"".equals(StringUtils.join((Object) null))) {
                throw new RuntimeException("[oracle:null-single-vararg] metamorphic violation: join((Object) null) must return empty string");
            }
            if (!TEXT_LIST_CHAR.equals(StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:array-char-basic] metamorphic violation: expected " + TEXT_LIST_CHAR);
            }
            if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:empty-array] metamorphic violation: empty array must join to empty string");
            }
            if (!"".equals(StringUtils.join(NULL_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:null-element-array] metamorphic violation: single null element must join to empty string");
            }
            if (!"foo".equals(StringUtils.join(MIXED_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:mixed-array] metamorphic violation: expected foo");
            }
            if (!"foo2".equals(StringUtils.join(MIXED_TYPE_LIST))) {
                throw new RuntimeException("[oracle:mixed-type] metamorphic violation: expected foo2");
            }

            String charJoined = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            String stringJoined = StringUtils.join(NULL_TO_STRING_LIST, "/", 0, 1);
            String plainJoined = StringUtils.join(NULL_TO_STRING_LIST);

            /*
             * Contract/oracle: for a one-element range there is no separator output, and the loop
             * appends the element via StringBuilder.append(Object). For an object whose toString()
             * returns null, append(Object) yields the literal "null". A throw-deleting or
             * branch-skipping patch could avoid the crash but return "", null, or some other value;
             * this post-condition catches that wrong-output fix.
             */
            if (!"null".equals(charJoined)) {
                throw new RuntimeException("[oracle:anchor-char] metamorphic violation: singleton char-separator join must be \"null\" lhs=" + safe(charJoined));
            }
            if (!"null".equals(stringJoined)) {
                throw new RuntimeException("[oracle:anchor-string] metamorphic violation: singleton string-separator join must be \"null\" lhs=" + safe(stringJoined));
            }
            if (!"null".equals(plainJoined)) {
                throw new RuntimeException("[oracle:anchor-plain] metamorphic violation: singleton plain join must be \"null\" lhs=" + safe(plainJoined));
            }
            if (!charJoined.equals(stringJoined)) {
                throw new RuntimeException("[oracle:anchor-overload] metamorphic violation: char and String separator overloads must agree input=" + safe(charJoined) + " rhs=" + safe(stringJoined));
            }

            if (!"/".equals(StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1))) {
                throw new RuntimeException("[oracle:range-mixed-array] metamorphic violation: expected /");
            }
            if (!"foo".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1))) {
                throw new RuntimeException("[oracle:range-first] metamorphic violation: expected foo");
            }
            if (!"foo/2".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2))) {
                throw new RuntimeException("[oracle:range-both] metamorphic violation: expected foo/2");
            }
            if (!"2".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2))) {
                throw new RuntimeException("[oracle:range-second] metamorphic violation: expected 2");
            }
            if (!"".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1))) {
                throw new RuntimeException("[oracle:range-empty] metamorphic violation: start>end must return empty string");
            }

            checkEmptyFieldAgreement();
        } catch (RuntimeException t) {
            if (shouldPropagate(t)) {
                throw t;
            }
        }
    }

    private static void runExploreChecks(FuzzedDataProvider data) {
        int len = data.consumeInt(1, 8);
        Object[] arr = new Object[len];
        int specialIndex = data.consumeInt(0, len - 1);

        for (int i = 0; i < len; i++) {
            if (i == specialIndex) {
                arr[i] = NULL_TO_STRING_OBJECT;
            } else {
                int choice = data.consumeInt(0, 3);
                if (choice == 0) {
                    arr[i] = null;
                } else if (choice == 1) {
                    arr[i] = data.consumeAsciiString(12);
                } else if (choice == 2) {
                    arr[i] = Long.valueOf(data.consumeInt(-1000000, 1000000));
                } else {
                    arr[i] = data.consumeString(12);
                }
            }
        }

        int startIndex = specialIndex;
        int endIndex = data.consumeInt(startIndex + 1, len);
        char sep = (char) data.consumeInt(1, 126);
        String sepStr = String.valueOf(sep);

        try {
            String charJoined = StringUtils.join(arr, sep, startIndex, endIndex);
            String stringJoined = StringUtils.join(arr, sepStr, startIndex, endIndex);

            /*
             * Documented equivalence/oracle: these two overloads implement the same join semantics;
             * when the String separator is exactly the single character used by the char overload,
             * they must produce the same result on the same valid range.
             */
            if (!safeEquals(charJoined, stringJoined)) {
                throw new RuntimeException("[oracle:overload-eq] metamorphic violation: equivalent separators disagree inputRange=" + startIndex + ".." + endIndex + " lhs=" + safe(charJoined) + " rhs=" + safe(stringJoined));
            }

            if (endIndex == startIndex + 1) {
                /*
                 * Contract/oracle for a singleton valid range containing only NULL_TO_STRING_OBJECT:
                 * the result must be the literal "null" regardless of separator, because no separator
                 * is emitted and the sole element is appended with StringBuilder.append(Object).
                 */
                if (!"null".equals(charJoined)) {
                    throw new RuntimeException("[oracle:singleton-null-char] metamorphic violation: singleton join must be \"null\" inputRange=" + startIndex + ".." + endIndex + " lhs=" + safe(charJoined));
                }
                if (!"null".equals(stringJoined)) {
                    throw new RuntimeException("[oracle:singleton-null-string] metamorphic violation: singleton join must be \"null\" inputRange=" + startIndex + ".." + endIndex + " lhs=" + safe(stringJoined));
                }
            }

            String plain = null;
            if (startIndex == 0 && endIndex == arr.length) {
                try {
                    plain = StringUtils.join(arr);
                } catch (RuntimeException ignored) {
                    plain = null;
                }
                if (plain != null && !safeEquals(plain, StringUtils.join(arr, "", 0, arr.length))) {
                    throw new RuntimeException("[oracle:plain-vs-empty-sep] metamorphic violation: join(arr) must agree with join(arr, \"\", 0, len) lhs=" + safe(plain) + " rhs=" + safe(StringUtils.join(arr, "", 0, arr.length)));
                }
            }

            checkEmptyFieldAgreement();
        } catch (RuntimeException t) {
            if (shouldPropagate(t)) {
                throw t;
            }
        }
    }

    private static void checkEmptyFieldAgreement() {
        /*
         * Shared-state agreement oracle: these methods all use StringUtils.EMPTY.
         * Their docs explicitly say null input returns an empty String, and negative/empty
         * substring/left cases also return empty String. If a patch corrupts or bypasses EMPTY,
         * these readers will disagree even when join itself appears fine.
         */
        String e1 = StringUtils.trimToEmpty(null);
        String e2 = StringUtils.stripToEmpty(null);
        String e3 = StringUtils.substring("", 0);
        String e4 = StringUtils.substring("", 0, 0);
        String e5 = StringUtils.left("abc", -1);
        String e6 = StringUtils.join(new Object[0], "/", 0, 0);

        if (!"".equals(e1) || !"".equals(e2) || !"".equals(e3) || !"".equals(e4) || !"".equals(e5) || !"".equals(e6)) {
            throw new RuntimeException("[oracle:empty-agreement] metamorphic violation: EMPTY-based methods must all report empty string values="
                    + safe(e1) + "," + safe(e2) + "," + safe(e3) + "," + safe(e4) + "," + safe(e5) + "," + safe(e6));
        }
        if (!(e1.equals(e2) && e2.equals(e3) && e3.equals(e4) && e4.equals(e5) && e5.equals(e6))) {
            throw new RuntimeException("[oracle:empty-eq] metamorphic violation: EMPTY-based methods disagree values="
                    + safe(e1) + "," + safe(e2) + "," + safe(e3) + "," + safe(e4) + "," + safe(e5) + "," + safe(e6));
        }
    }

    private static boolean shouldPropagate(Throwable t) {
        if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
            return true;
        }
        if (isCleanRejection(t)) {
            return false;
        }
        return isRootCause(t);
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String safe(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }
}
```

</details>

---
## [40] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `plain` (literal assignment, no rethrow, the caught exceptio

---
## [41] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~13,616 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
**[assistant]**
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

    /*
     * Exact fixture from StringUtilsTest: this object is the real trigger because
     * StringUtils.join pre-sizes the StringBuilder using array[startIndex].toString().length().
     * A correct implementation must still later append the object, and StringBuilder.append(Object)
     * renders an Object whose toString() returns null as the literal "null".
     */
    private static final Object NULL_TO_STRING_OBJECT = new Object() {
        @Override
        public String toString() {
            return null;
        }
    };
    private static final Object[] NULL_TO_STRING_LIST = { NULL_TO_STRING_OBJECT };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchorChecks();
        runExploreChecks(data);
    }

    private static void runAnchorChecks() {
        try {
            if (StringUtils.join((Object[]) null, ',') != null) {
                throw new RuntimeException("[oracle:null-array-char] metamorphic violation: join((Object[]) null, ',') must return null");
            }
            if (StringUtils.join((Object[]) null) != null) {
                throw new RuntimeException("[oracle:null-array-varargs] metamorphic violation: join((Object[]) null) must return null");
            }
            if (!"".equals(StringUtils.join())) {
                throw new RuntimeException("[oracle:empty-varargs] metamorphic violation: join() must return empty string");
            }
            if (!"".equals(StringUtils.join((Object) null))) {
                throw new RuntimeException("[oracle:null-single-vararg] metamorphic violation: join((Object) null) must return empty string");
            }
            if (!TEXT_LIST_CHAR.equals(StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:array-char-basic] metamorphic violation: expected " + TEXT_LIST_CHAR);
            }
            if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:empty-array] metamorphic violation: empty array must join to empty string");
            }
            if (!"".equals(StringUtils.join(NULL_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:null-element-array] metamorphic violation: single null element must join to empty string");
            }
            if (!"foo".equals(StringUtils.join(MIXED_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:mixed-array] metamorphic violation: expected foo");
            }
            if (!"foo2".equals(StringUtils.join(MIXED_TYPE_LIST))) {
                throw new RuntimeException("[oracle:mixed-type] metamorphic violation: expected foo2");
            }

            String charJoined = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            String stringJoined = StringUtils.join(NULL_TO_STRING_LIST, "/", 0, 1);
            String plainJoined = StringUtils.join(NULL_TO_STRING_LIST);

            /*
             * Contract/oracle: for a one-element range there is no separator output, and the loop
             * appends the element via StringBuilder.append(Object). For an object whose toString()
             * returns null, append(Object) yields the literal "null". A throw-deleting or
             * branch-skipping patch could avoid the crash but return "", null, or some other value;
             * this post-condition catches that wrong-output fix.
             */
            if (!"null".equals(charJoined)) {
                throw new RuntimeException("[oracle:anchor-char] metamorphic violation: singleton char-separator join must be \"null\" lhs=" + safe(charJoined));
            }
            if (!"null".equals(stringJoined)) {
                throw new RuntimeException("[oracle:anchor-string] metamorphic violation: singleton string-separator join must be \"null\" lhs=" + safe(stringJoined));
            }
            if (!"null".equals(plainJoined)) {
                throw new RuntimeException("[oracle:anchor-plain] metamorphic violation: singleton plain join must be \"null\" lhs=" + safe(plainJoined));
            }
            if (!charJoined.equals(stringJoined)) {
                throw new RuntimeException("[oracle:anchor-overload] metamorphic violation: char and String separator overloads must agree input=" + safe(charJoined) + " rhs=" + safe(stringJoined));
            }

            if (!"/".equals(StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1))) {
                throw new RuntimeException("[oracle:range-mixed-array] metamorphic violation: expected /");
            }
            if (!"foo".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1))) {
                throw new RuntimeException("[oracle:range-first] metamorphic violation: expected foo");
            }
            if (!"foo/2".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2))) {
                throw new RuntimeException("[oracle:range-both] metamorphic violation: expected foo/2");
            }
            if (!"2".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2))) {
                throw new RuntimeException("[oracle:range-second] metamorphic violation: expected 2");
            }
            if (!"".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1))) {
                throw new RuntimeException("[oracle:range-empty] metamorphic violation: start>end must return empty string");
            }

            checkEmptyFieldAgreement();
        } catch (RuntimeException t) {
            if (shouldPropagate(t)) {
                throw t;
            }
        }
    }

    private static void runExploreChecks(FuzzedDataProvider data) {
        int len = data.consumeInt(1, 8);
        Object[] arr = new Object[len];
        int specialIndex = data.consumeInt(0, len - 1);

        for (int i = 0; i < len; i++) {
            if (i == specialIndex) {
                arr[i] = NULL_TO_STRING_OBJECT;
            } else {
                int choice = data.consumeInt(0, 3);
                if (choice == 0) {
                    arr[i] = null;
                } else if (choice == 1) {
                    arr[i] = data.consumeAsciiString(12);
                } else if (choice == 2) {
                    arr[i] = Long.valueOf(data.consumeInt(-1000000, 1000000));
                } else {
                    arr[i] = data.consumeString(12);
                }
            }
        }

        int startIndex = specialIndex;
        int endIndex = data.consumeInt(startIndex + 1, len);
        char sep = (char) data.consumeInt(1, 126);
        String sepStr = String.valueOf(sep);

        try {
            String charJoined = StringUtils.join(arr, sep, startIndex, endIndex);
            String stringJoined = StringUtils.join(arr, sepStr, startIndex, endIndex);

            /*
             * Documented equivalence/oracle: these two overloads implement the same join semantics;
             * when the String separator is exactly the single character used by the char overload,
             * they must produce the same result on the same valid range.
             */
            if (!safeEquals(charJoined, stringJoined)) {
                throw new RuntimeException("[oracle:overload-eq] metamorphic violation: equivalent separators disagree inputRange=" + startIndex + ".." + endIndex + " lhs=" + safe(charJoined) + " rhs=" + safe(stringJoined));
            }

            if (endIndex == startIndex + 1) {
                /*
                 * Contract/oracle for a singleton valid range containing only NULL_TO_STRING_OBJECT:
                 * the result must be the literal "null" regardless of separator, because no separator
                 * is emitted and the sole element is appended with StringBuilder.append(Object).
                 */
                if (!"null".equals(charJoined)) {
                    throw new RuntimeException("[oracle:singleton-null-char] metamorphic violation: singleton join must be \"null\" inputRange=" + startIndex + ".." + endIndex + " lhs=" + safe(charJoined));
                }
                if (!"null".equals(stringJoined)) {
                    throw new RuntimeException("[oracle:singleton-null-string] metamorphic violation: singleton join must be \"null\" inputRange=" + startIndex + ".." + endIndex + " lhs=" + safe(stringJoined));
                }
            }

            String plain = null;
            if (startIndex == 0 && endIndex == arr.length) {
                try {
                    plain = StringUtils.join(arr);
                } catch (RuntimeException ignored) {
                    plain = null;
                }
                if (plain != null && !safeEquals(plain, StringUtils.join(arr, "", 0, arr.length))) {
                    throw new RuntimeException("[oracle:plain-vs-empty-sep] metamorphic violation: join(arr) must agree with join(arr, \"\", 0, len) lhs=" + safe(plain) + " rhs=" + safe(StringUtils.join(arr, "", 0, arr.length)));
                }
            }

            checkEmptyFieldAgreement();
        } catch (RuntimeException t) {
            if (shouldPropagate(t)) {
                throw t;
            }
        }
    }

    private static void checkEmptyFieldAgreement() {
        /*
         * Shared-state agreement oracle: these methods all use StringUtils.EMPTY.
         * Their docs explicitly say null input returns an empty String, and negative/empty
         * substring/left cases also return empty String. If a patch corrupts or bypasses EMPTY,
         * these readers will disagree even when join itself appears fine.
         */
        String e1 = StringUtils.trimToEmpty(null);
        String e2 = StringUtils.stripToEmpty(null);
        String e3 = StringUtils.substring("", 0);
        String e4 = StringUtils.substring("", 0, 0);
        String e5 = StringUtils.left("abc", -1);
        String e6 = StringUtils.join(new Object[0], "/", 0, 0);

        if (!"".equals(e1) || !"".equals(e2) || !"".equals(e3) || !"".equals(e4) || !"".equals(e5) || !"".equals(e6)) {
            throw new RuntimeException("[oracle:empty-agreement] metamorphic violation: EMPTY-based methods must all report empty string values="
                    + safe(e1) + "," + safe(e2) + "," + safe(e3) + "," + safe(e4) + "," + safe(e5) + "," + safe(e6));
        }
        if (!(e1.equals(e2) && e2.equals(e3) && e3.equals(e4) && e4.equals(e5) && e5.equals(e6))) {
            throw new RuntimeException("[oracle:empty-eq] metamorphic violation: EMPTY-based methods disagree values="
                    + safe(e1) + "," + safe(e2) + "," + safe(e3) + "," + safe(e4) + "," + safe(e5) + "," + safe(e6));
        }
    }

    private static boolean shouldPropagate(Throwable t) {
        if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
            return true;
        }
        if (isCleanRejection(t)) {
            return false;
        }
        return isRootCause(t);
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String safe(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }
}
```
**[user]**
```
Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `plain` (literal assignment, no rethrow, the caught exception is never used) and the alarm near line 154 is conditioned on `plain` — the caught exception's identity is destroyed, so attribution cannot tell WHICH exception fired or whether the unpatched build crashes the same way. Rethrow the exception as the alarm's cause (`..., e`), or assert on the real observable instead of a swallowed boolean.
A caught exception is a REJECTION, never a violation, and flattening it into a flag destroys the identity attribution needs. Either assert on the real observable the test pins (compute the actual value and compare it), or if you must re-report a caught crash, re-throw it with the original as the cause: `new FuzzerSecurityIssueLow("[oracle:<id>] ...", e)`. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~12,216 chars)</summary>

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

    private static final Object NULL_TO_STRING_OBJECT = new Object() {
        @Override
        public String toString() {
            return null;
        }
    };
    private static final Object[] NULL_TO_STRING_LIST = { NULL_TO_STRING_OBJECT };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchorChecks();
        runExploreChecks(data);
    }

    private static void runAnchorChecks() {
        try {
            if (StringUtils.join((Object[]) null, ',') != null) {
                throw new RuntimeException("[oracle:null-array-char] metamorphic violation: join((Object[]) null, ',') must return null");
            }
            if (StringUtils.join((Object[]) null) != null) {
                throw new RuntimeException("[oracle:null-array-varargs] metamorphic violation: join((Object[]) null) must return null");
            }
            if (!"".equals(StringUtils.join())) {
                throw new RuntimeException("[oracle:empty-varargs] metamorphic violation: join() must return empty string");
            }
            if (!"".equals(StringUtils.join((Object) null))) {
                throw new RuntimeException("[oracle:null-single-vararg] metamorphic violation: join((Object) null) must return empty string");
            }
            if (!TEXT_LIST_CHAR.equals(StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:array-char-basic] metamorphic violation: expected " + TEXT_LIST_CHAR);
            }
            if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:empty-array] metamorphic violation: empty array must join to empty string");
            }
            if (!"".equals(StringUtils.join(NULL_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:null-element-array] metamorphic violation: single null element must join to empty string");
            }
            if (!"foo".equals(StringUtils.join(MIXED_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:mixed-array] metamorphic violation: expected foo");
            }
            if (!"foo2".equals(StringUtils.join(MIXED_TYPE_LIST))) {
                throw new RuntimeException("[oracle:mixed-type] metamorphic violation: expected foo2");
            }

            String charJoined = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            String stringJoined = StringUtils.join(NULL_TO_STRING_LIST, "/", 0, 1);
            String plainJoined = StringUtils.join(NULL_TO_STRING_LIST);

            /*
             * Contract guarantee asserted: for a one-element range there is no separator output, and
             * the implementation appends the element object itself. StringBuilder.append(Object)
             * renders an object whose toString() returns null as the literal "null". A patch that
             * merely suppresses the crash but skips/changes the append would violate this.
             */
            if (!"null".equals(charJoined)) {
                throw new RuntimeException("[oracle:anchor-char] metamorphic violation: singleton char-separator join must be \"null\" lhs=" + safe(charJoined));
            }
            if (!"null".equals(stringJoined)) {
                throw new RuntimeException("[oracle:anchor-string] metamorphic violation: singleton string-separator join must be \"null\" lhs=" + safe(stringJoined));
            }
            if (!"null".equals(plainJoined)) {
                throw new RuntimeException("[oracle:anchor-plain] metamorphic violation: singleton plain join must be \"null\" lhs=" + safe(plainJoined));
            }
            if (!charJoined.equals(stringJoined)) {
                throw new RuntimeException("[oracle:anchor-overload] metamorphic violation: char and String separator overloads must agree lhs=" + safe(charJoined) + " rhs=" + safe(stringJoined));
            }

            if (!"/".equals(StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1))) {
                throw new RuntimeException("[oracle:range-mixed-array] metamorphic violation: expected /");
            }
            if (!"foo".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1))) {
                throw new RuntimeException("[oracle:range-first] metamorphic violation: expected foo");
            }
            if (!"foo/2".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2))) {
                throw new RuntimeException("[oracle:range-both] metamorphic violation: expected foo/2");
            }
            if (!"2".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2))) {
                throw new RuntimeException("[oracle:range-second] metamorphic violation: expected 2");
            }
            if (!"".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1))) {
                throw new RuntimeException("[oracle:range-empty] metamorphic violation: start>end must return empty string");
            }

            checkEmptyFieldAgreement();
        } catch (RuntimeException t) {
            if (shouldPropagate(t)) {
                throw t;
            }
        }
    }

    private static void runExploreChecks(FuzzedDataProvider data) {
        int len = data.consumeInt(1, 8);
        Object[] arr = new Object[len];
        int specialIndex = data.consumeInt(0, len - 1);

        for (int i = 0; i < len; i++) {
            if (i == specialIndex) {
                arr[i] = NULL_TO_STRING_OBJECT;
            } else {
                int choice = data.consumeInt(0, 3);
                if (choice == 0) {
                    arr[i] = null;
                } else if (choice == 1) {
                    arr[i] = data.consumeAsciiString(12);
                } else if (choice == 2) {
                    arr[i] = Long.valueOf(data.consumeInt(-1000000, 1000000));
                } else {
                    arr[i] = data.consumeString(12);
                }
            }
        }

        int startIndex = specialIndex;
        int endIndex = data.consumeInt(startIndex + 1, len);
        char sep = (char) data.consumeInt(1, 126);
        String sepStr = String.valueOf(sep);

        try {
            String charJoined = StringUtils.join(arr, sep, startIndex, endIndex);
            String stringJoined = StringUtils.join(arr, sepStr, startIndex, endIndex);

            /*
             * Contract guarantee asserted: the char-separator and String-separator overloads are
             * sibling APIs for the same operation. When the String separator is exactly the same
             * single character, they must produce the same result on the same valid slice.
             */
            if (!safeEquals(charJoined, stringJoined)) {
                throw new RuntimeException("[oracle:overload-eq] metamorphic violation: equivalent separators disagree inputRange=" + startIndex + ".." + endIndex + " lhs=" + safe(charJoined) + " rhs=" + safe(stringJoined));
            }

            if (endIndex == startIndex + 1) {
                /*
                 * Contract guarantee asserted: a singleton valid range containing only the special
                 * object must yield "null", because no separator is emitted and the sole element is
                 * appended via append(Object).
                 */
                if (!"null".equals(charJoined)) {
                    throw new RuntimeException("[oracle:singleton-null-char] metamorphic violation: singleton join must be \"null\" inputRange=" + startIndex + ".." + endIndex + " lhs=" + safe(charJoined));
                }
                if (!"null".equals(stringJoined)) {
                    throw new RuntimeException("[oracle:singleton-null-string] metamorphic violation: singleton join must be \"null\" inputRange=" + startIndex + ".." + endIndex + " lhs=" + safe(stringJoined));
                }
            }

            if (startIndex == 0 && endIndex == arr.length) {
                try {
                    /*
                     * Contract guarantee asserted: join(arr) is equivalent to joining the full array
                     * with the empty separator. If either side throws, this oracle does not apply.
                     */
                    String plainJoined = StringUtils.join(arr);
                    String emptySepJoined = StringUtils.join(arr, "", 0, arr.length);
                    if (!safeEquals(plainJoined, emptySepJoined)) {
                        throw new RuntimeException("[oracle:plain-vs-empty-sep] metamorphic violation: join(arr) must agree with join(arr, \"\", 0, len) lhs=" + safe(plainJoined) + " rhs=" + safe(emptySepJoined));
                    }
                } catch (RuntimeException inner) {
                    if (shouldPropagate(inner)) {
                        throw inner;
                    }
                    return;
                }
            }

            checkEmptyFieldAgreement();
        } catch (RuntimeException t) {
            if (shouldPropagate(t)) {
                throw t;
            }
        }
    }

    private static void checkEmptyFieldAgreement() {
        /*
         * Shared-state agreement oracle: these methods all use StringUtils.EMPTY. Their docs state
         * they return an empty String for the covered null/empty/negative cases, so they must agree.
         */
        String e1 = StringUtils.trimToEmpty(null);
        String e2 = StringUtils.stripToEmpty(null);
        String e3 = StringUtils.substring("", 0);
        String e4 = StringUtils.substring("", 0, 0);
        String e5 = StringUtils.left("abc", -1);
        String e6 = StringUtils.join(new Object[0], "/", 0, 0);

        if (!"".equals(e1) || !"".equals(e2) || !"".equals(e3) || !"".equals(e4) || !"".equals(e5) || !"".equals(e6)) {
            throw new RuntimeException("[oracle:empty-agreement] metamorphic violation: EMPTY-based methods must all report empty string values="
                    + safe(e1) + "," + safe(e2) + "," + safe(e3) + "," + safe(e4) + "," + safe(e5) + "," + safe(e6));
        }
        if (!(e1.equals(e2) && e2.equals(e3) && e3.equals(e4) && e4.equals(e5) && e5.equals(e6))) {
            throw new RuntimeException("[oracle:empty-eq] metamorphic violation: EMPTY-based methods disagree values="
                    + safe(e1) + "," + safe(e2) + "," + safe(e3) + "," + safe(e4) + "," + safe(e5) + "," + safe(e6));
        }
    }

    private static boolean shouldPropagate(Throwable t) {
        if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
            return true;
        }
        if (isCleanRejection(t)) {
            return false;
        }
        return isRootCause(t);
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String safe(String s) {
        return s == null ? "null" : "\"" + s + "\"";
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
<details open><summary>▸ Output (~16,455 chars)</summary>

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
        checkSharedEmptyState();
        runAnchorCalls();
        runExploreCalls(data);
    }

    private static void runAnchorCalls() {
        try {
            String r = StringUtils.join((Object[]) null, ',');
            if (r != null) {
                throw new RuntimeException("[oracle:anchor-null-array-char] metamorphic violation: documented null-array join should return null input=null lhs=" + r + " rhs=null");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, false);
        }

        try {
            String r = StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR);
            if (!TEXT_LIST_CHAR.equals(r)) {
                throw new RuntimeException("[oracle:anchor-array-char] metamorphic violation: exact regression anchor input=ARRAY_LIST lhs=" + r + " rhs=" + TEXT_LIST_CHAR);
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }

        try {
            String r = StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR);
            if (!"".equals(r)) {
                throw new RuntimeException("[oracle:anchor-empty-array-char] metamorphic violation: exact regression anchor input=EMPTY_ARRAY_LIST lhs=" + r + " rhs=");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }

        try {
            String r = StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR);
            if (!";;foo".equals(r)) {
                throw new RuntimeException("[oracle:anchor-mixed-array-char] metamorphic violation: exact regression anchor input=MIXED_ARRAY_LIST lhs=" + r + " rhs=;;foo");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }

        try {
            String r = StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR);
            if (!"foo;2".equals(r)) {
                throw new RuntimeException("[oracle:anchor-mixed-type-char] metamorphic violation: exact regression anchor input=MIXED_TYPE_LIST lhs=" + r + " rhs=foo;2");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }

        try {
            String r = StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1);
            if (!"/".equals(r)) {
                throw new RuntimeException("[oracle:anchor-slice-char-1] metamorphic violation: exact regression anchor input=MIXED_ARRAY_LIST lhs=" + r + " rhs=/");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }

        try {
            String r = StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1);
            if (!"foo".equals(r)) {
                throw new RuntimeException("[oracle:anchor-slice-char-2] metamorphic violation: exact regression anchor input=MIXED_TYPE_LIST lhs=" + r + " rhs=foo");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }

        try {
            String r = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            if (!"null".equals(r)) {
                throw new RuntimeException("[oracle:anchor-null-toString-char] metamorphic violation: exact failing test says one-element slice whose only element has toString()==null must join to literal null input=NULL_TO_STRING_LIST lhs=" + r + " rhs=null");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }

        try {
            String r = StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2);
            if (!"foo/2".equals(r)) {
                throw new RuntimeException("[oracle:anchor-slice-char-3] metamorphic violation: exact regression anchor input=MIXED_TYPE_LIST lhs=" + r + " rhs=foo/2");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }

        try {
            String r = StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2);
            if (!"2".equals(r)) {
                throw new RuntimeException("[oracle:anchor-slice-char-4] metamorphic violation: exact regression anchor input=MIXED_TYPE_LIST lhs=" + r + " rhs=2");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }

        try {
            String r = StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1);
            if (!"".equals(r)) {
                throw new RuntimeException("[oracle:anchor-slice-char-5] metamorphic violation: exact regression anchor input=MIXED_TYPE_LIST lhs=" + r + " rhs=");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, false);
        }

        try {
            String r = StringUtils.join((Object[]) null);
            if (r != null) {
                throw new RuntimeException("[oracle:anchor-null-array-varargs] metamorphic violation: documented null-array join should return null input=null lhs=" + r + " rhs=null");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, false);
        }

        try {
            String r = StringUtils.join();
            if (!"".equals(r)) {
                throw new RuntimeException("[oracle:anchor-empty-varargs] metamorphic violation: exact regression anchor input=empty varargs lhs=" + r + " rhs=");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }

        try {
            String r = StringUtils.join((Object) null);
            if (!"".equals(r)) {
                throw new RuntimeException("[oracle:anchor-single-null-object] metamorphic violation: exact regression anchor input=(Object)null lhs=" + r + " rhs=");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }

        try {
            String r = StringUtils.join(EMPTY_ARRAY_LIST);
            if (!"".equals(r)) {
                throw new RuntimeException("[oracle:anchor-empty-array] metamorphic violation: exact regression anchor input=EMPTY_ARRAY_LIST lhs=" + r + " rhs=");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }

        try {
            String r = StringUtils.join(NULL_ARRAY_LIST);
            if (!"".equals(r)) {
                throw new RuntimeException("[oracle:anchor-null-array-element] metamorphic violation: exact regression anchor input=NULL_ARRAY_LIST lhs=" + r + " rhs=");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }

        try {
            String r = StringUtils.join(NULL_TO_STRING_LIST);
            if (!"null".equals(r)) {
                throw new RuntimeException("[oracle:anchor-null-toString-varargs] metamorphic violation: exact failing test says array whose only element has toString()==null must join to literal null input=NULL_TO_STRING_LIST lhs=" + r + " rhs=null");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }

        try {
            String r = StringUtils.join(new String[] { "a", "b", "c" });
            if (!"abc".equals(r)) {
                throw new RuntimeException("[oracle:anchor-abc] metamorphic violation: exact regression anchor input=[a,b,c] lhs=" + r + " rhs=abc");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }

        try {
            String r = StringUtils.join(new String[] { null, "a", "" });
            if (!"a".equals(r)) {
                throw new RuntimeException("[oracle:anchor-null-a-empty] metamorphic violation: exact regression anchor input=[null,a,empty] lhs=" + r + " rhs=a");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }

        try {
            String r = StringUtils.join(MIXED_ARRAY_LIST);
            if (!"foo".equals(r)) {
                throw new RuntimeException("[oracle:anchor-mixed-array] metamorphic violation: exact regression anchor input=MIXED_ARRAY_LIST lhs=" + r + " rhs=foo");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }

        try {
            String r = StringUtils.join(MIXED_TYPE_LIST);
            if (!"foo2".equals(r)) {
                throw new RuntimeException("[oracle:anchor-mixed-type] metamorphic violation: exact regression anchor input=MIXED_TYPE_LIST lhs=" + r + " rhs=foo2");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }

        try {
            String r = StringUtils.join(NULL_TO_STRING_LIST, "/", 0, 1);
            if (!"null".equals(r)) {
                throw new RuntimeException("[oracle:anchor-null-toString-string] metamorphic violation: one-element slice whose only element has toString()==null must join to literal null input=NULL_TO_STRING_LIST lhs=" + r + " rhs=null");
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }
    }

    private static void runExploreCalls(FuzzedDataProvider data) {
        int len = data.consumeInt(1, 8);
        Object[] array = new Object[len];
        for (int i = 0; i < len; i++) {
            int kind = data.consumeInt(0, 5);
            if (kind == 0) {
                array[i] = null;
            } else if (kind == 1) {
                array[i] = data.consumeAsciiString(8);
            } else if (kind == 2) {
                array[i] = Long.valueOf(data.consumeInt(-1000, 1000));
            } else if (kind == 3) {
                array[i] = Boolean.valueOf(data.consumeBoolean());
            } else if (kind == 4) {
                array[i] = Character.valueOf((char) (data.consumeByte() & 0xff));
            } else {
                array[i] = data.consumeString(8);
            }
        }

        int start = data.consumeInt(0, len - 1);
        int end = data.consumeInt(start + 1, len);
        array[start] = NULL_TO_STRING;

        Object[] transformed = array.clone();
        transformed[start] = "null";

        char sepChar = (char) (data.consumeByte() & 0xff);
        String sepString;
        if (data.consumeBoolean()) {
            sepString = null;
        } else if (data.consumeBoolean()) {
            sepString = String.valueOf(sepChar);
        } else {
            sepString = data.consumeString(4);
        }

        /*
         * Contract/oracle:
         * For every selected element, join appends the element itself via StringBuilder.append(Object).
         * The regression tests establish that an element whose toString() returns null must contribute
         * the literal text "null". Therefore, replacing that element with the literal String "null"
         * is an equivalent-input transformation: both joins must return the same result.
         * A throw-deleting or behavior-skipping patch would break this equality even when no exception fires.
         */
        try {
            String lhs = StringUtils.join(array, sepChar, start, end);
            String rhs = StringUtils.join(transformed, sepChar, start, end);
            if (!safeEquals(lhs, rhs)) {
                throw new RuntimeException("[oracle:eq-char] metamorphic violation: join(array,char,start,end) must equal join(transformed,char,start,end) when transformed replaces toString()==null with literal null inputStart=" + start + " inputEnd=" + end + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }

        try {
            String lhs = StringUtils.join(array, sepString, start, end);
            String rhs = StringUtils.join(transformed, sepString, start, end);
            if (!safeEquals(lhs, rhs)) {
                throw new RuntimeException("[oracle:eq-string] metamorphic violation: join(array,String,start,end) must equal join(transformed,String,start,end) when transformed replaces toString()==null with literal null inputStart=" + start + " inputEnd=" + end + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }

        if (sepString != null && sepString.length() == 1) {
            try {
                String lhs = StringUtils.join(array, sepChar, start, end);
                String rhs = StringUtils.join(array, sepString, start, end);
                if (!safeEquals(lhs, rhs)) {
                    throw new RuntimeException("[oracle:sibling-sep] metamorphic violation: single-character String separator and char separator are equivalent inputs inputStart=" + start + " inputEnd=" + end + " lhs=" + lhs + " rhs=" + rhs);
                }
            } catch (RuntimeException t) {
                handleRuntime(t, true);
            }
        }

        try {
            String lhs = StringUtils.join(transformed, (String) null, start, end);
            String rhs = StringUtils.join(transformed, "", start, end);
            if (!safeEquals(lhs, rhs)) {
                throw new RuntimeException("[oracle:null-sep] metamorphic violation: implementation explicitly treats null String separator as EMPTY inputStart=" + start + " inputEnd=" + end + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (RuntimeException t) {
            handleRuntime(t, true);
        }
    }

    private static void checkSharedEmptyState() {
        /*
         * These methods all use the shared EMPTY constant and their docs explicitly say they return
         * an empty String for the covered inputs. If a patch corrupted shared state, these readers
         * would disagree on what EMPTY means even when join's own output looked plausible.
         */
        String e = StringUtils.EMPTY;
        if (!safeEquals(e, StringUtils.trimToEmpty(null))) {
            throw new RuntimeException("[oracle:empty-trim] metamorphic violation: trimToEmpty(null) must equal EMPTY input=null lhs=" + StringUtils.trimToEmpty(null) + " rhs=" + e);
        }
        if (!safeEquals(e, StringUtils.stripToEmpty(null))) {
            throw new RuntimeException("[oracle:empty-strip] metamorphic violation: stripToEmpty(null) must equal EMPTY input=null lhs=" + StringUtils.stripToEmpty(null) + " rhs=" + e);
        }
        if (!safeEquals(e, StringUtils.substring("", 0))) {
            throw new RuntimeException("[oracle:empty-substring] metamorphic violation: substring(empty,0) must equal EMPTY input=empty lhs=" + StringUtils.substring("", 0) + " rhs=" + e);
        }
        if (!safeEquals(e, StringUtils.left("abc", -1))) {
            throw new RuntimeException("[oracle:empty-left] metamorphic violation: left(str,-1) must equal EMPTY input=abc,-1 lhs=" + StringUtils.left("abc", -1) + " rhs=" + e);
        }
    }

    private static void handleRuntime(RuntimeException t, boolean validByConstruction) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return;
        }
        if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
            throw t;
        }
        if (validByConstruction && isJoinRootCause(t)) {
            throw t;
        }
    }

    private static boolean isJoinRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName()) && "join".equals(ste.getMethodName())) {
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
<details open><summary>▸ Output (~14,377 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final String[] ARRAY_LIST = { "foo", "bar", "baz" };
    private static final String[] EMPTY_ARRAY_LIST = {};
    private static final String[] NULL_ARRAY_LIST = { null };
    private static final Object[] NULL_TO_STRING_LIST = { new NullToStringObject() };
    private static final String[] MIXED_ARRAY_LIST = { null, "", "foo" };
    private static final Object[] MIXED_TYPE_LIST = { "foo", Long.valueOf(2L) };
    private static final char SEPARATOR_CHAR = ';';
    private static final String TEXT_LIST_CHAR = "foo;bar;baz";

    private static final class NullToStringObject {
        @Override
        public String toString() {
            return null;
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchorChecks();

        int len = data.consumeInt(1, 8);
        Object[] array = new Object[len];
        for (int i = 0; i < len; i++) {
            if (data.consumeBoolean()) {
                array[i] = null;
            } else {
                array[i] = data.consumeString(32);
            }
        }

        int startIndex = data.consumeInt(0, len - 1);
        int endIndex = data.consumeInt(startIndex + 1, len);
        array[startIndex] = new NullToStringObject();

        char sepChar = (char) data.consumeInt(0, 127);
        String sepString = data.consumeString(8);
        if (data.consumeBoolean()) {
            sepString = String.valueOf(sepChar);
        }

        // Documented/observed contract from the library tests: joining a single-element range
        // whose only element is a non-null object with toString() == null must yield "null".
        // A bogus patch that merely avoids the throw by skipping the append or returning EMPTY
        // would violate this observable result.
        try {
            String singleChar = StringUtils.join(array, sepChar, startIndex, startIndex + 1);
            if (!"null".equals(singleChar)) {
                throw new RuntimeException("[oracle:join-single-char] metamorphic violation: singleton join must yield \"null\" inputStart=" + startIndex + " lhs=" + String.valueOf(singleChar) + " rhs=null");
            }
        } catch (Throwable t) {
            handleLibraryThrowable(t, true);
            return;
        }

        try {
            String singleString = StringUtils.join(array, sepString, startIndex, startIndex + 1);
            if (!"null".equals(singleString)) {
                throw new RuntimeException("[oracle:join-single-string] metamorphic violation: singleton join must yield \"null\" inputStart=" + startIndex + " lhs=" + String.valueOf(singleString) + " rhs=null");
            }
        } catch (Throwable t) {
            handleLibraryThrowable(t, true);
            return;
        }

        // Equivalent-input relation: using a one-character String separator must agree with the
        // char-separator overload on the same valid range.
        String oneCharStringSep = String.valueOf(sepChar);
        try {
            String lhs = StringUtils.join(array, sepChar, startIndex, endIndex);
            String rhs = StringUtils.join(array, oneCharStringSep, startIndex, endIndex);
            if (lhs != null ? !lhs.equals(rhs) : rhs != null) {
                throw new RuntimeException("[oracle:join-overload] metamorphic violation: char and one-char String separators must agree inputStart=" + startIndex + " inputEnd=" + endIndex + " lhs=" + String.valueOf(lhs) + " rhs=" + String.valueOf(rhs));
            }
        } catch (Throwable t) {
            handleLibraryThrowable(t, true);
            return;
        }

        // Shared EMPTY post-checks on explicitly documented degenerate inputs.
        try {
            String emptyFromTrim = StringUtils.trimToEmpty(null);
            String emptyFromStrip = StringUtils.stripToEmpty(null);
            String emptyFromSubstring = StringUtils.substring("", 0);
            String emptyFromLeft = StringUtils.left("x", -1);
            if (!(StringUtils.EMPTY.equals(emptyFromTrim)
                    && StringUtils.EMPTY.equals(emptyFromStrip)
                    && StringUtils.EMPTY.equals(emptyFromSubstring)
                    && StringUtils.EMPTY.equals(emptyFromLeft))) {
                throw new RuntimeException("[oracle:empty-shared] metamorphic violation: EMPTY-based readers disagreed lhs="
                        + emptyFromTrim + "|" + emptyFromStrip + "|" + emptyFromSubstring + "|" + emptyFromLeft
                        + " rhs=" + StringUtils.EMPTY);
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
        }
    }

    private static void anchorChecks() {
        try {
            if (StringUtils.join((Object[]) null, ',') != null) {
                throw new RuntimeException("[oracle:anchor-null-array-char] metamorphic violation: null array join(char) must return null input=null lhs="
                        + String.valueOf(StringUtils.join((Object[]) null, ',')) + " rhs=null");
            }
            if (!TEXT_LIST_CHAR.equals(StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-array-list] metamorphic violation: expected fixture text input=" + java.util.Arrays.toString(ARRAY_LIST)
                        + " lhs=" + StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR) + " rhs=" + TEXT_LIST_CHAR);
            }
            if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-empty-char] metamorphic violation: empty array join(char) must be empty input=[] lhs="
                        + String.valueOf(StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR)) + " rhs=");
            }
            if (!";;foo".equals(StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-mixed-array-char] metamorphic violation: fixture mismatch input="
                        + java.util.Arrays.toString(MIXED_ARRAY_LIST) + " lhs=" + StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR) + " rhs=;;foo");
            }
            if (!"foo;2".equals(StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-mixed-type-char] metamorphic violation: fixture mismatch input="
                        + java.util.Arrays.toString(MIXED_TYPE_LIST) + " lhs=" + StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR) + " rhs=foo;2");
            }
            if (!"/".equals(StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1))) {
                throw new RuntimeException("[oracle:anchor-range-char] metamorphic violation: fixture mismatch input="
                        + java.util.Arrays.toString(MIXED_ARRAY_LIST) + " lhs=" + StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1) + " rhs=/");
            }
            if (!"foo".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1))) {
                throw new RuntimeException("[oracle:anchor-range-char-foo] metamorphic violation: fixture mismatch input="
                        + java.util.Arrays.toString(MIXED_TYPE_LIST) + " lhs=" + StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1) + " rhs=foo");
            }

            try {
                String exact = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
                if (!"null".equals(exact)) {
                    throw new RuntimeException("[oracle:anchor-exact-char] metamorphic violation: exact failing-test input must yield \"null\" input=[NullToStringObject] lhs="
                            + String.valueOf(exact) + " rhs=null");
                }
            } catch (Throwable t) {
                handleLibraryThrowable(t, true);
            }

            if (!"foo/2".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2))) {
                throw new RuntimeException("[oracle:anchor-range-char-foo2] metamorphic violation: fixture mismatch input="
                        + java.util.Arrays.toString(MIXED_TYPE_LIST) + " lhs=" + StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2) + " rhs=foo/2");
            }
            if (!"2".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2))) {
                throw new RuntimeException("[oracle:anchor-range-char-2] metamorphic violation: fixture mismatch input="
                        + java.util.Arrays.toString(MIXED_TYPE_LIST) + " lhs=" + StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2) + " rhs=2");
            }
            if (!"".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1))) {
                throw new RuntimeException("[oracle:anchor-range-char-empty] metamorphic violation: fixture mismatch input="
                        + java.util.Arrays.toString(MIXED_TYPE_LIST) + " lhs=" + StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1) + " rhs=");
            }

            if (StringUtils.join((Object[]) null) != null) {
                throw new RuntimeException("[oracle:anchor-null-array-object] metamorphic violation: null array join() must return null input=null lhs="
                        + String.valueOf(StringUtils.join((Object[]) null)) + " rhs=null");
            }
            if (!"".equals(StringUtils.join())) {
                throw new RuntimeException("[oracle:anchor-empty-varargs] metamorphic violation: empty varargs join must be empty input=[] lhs="
                        + String.valueOf(StringUtils.join()) + " rhs=");
            }
            if (!"".equals(StringUtils.join((Object) null))) {
                throw new RuntimeException("[oracle:anchor-object-null] metamorphic violation: join((Object)null) must be empty input=null lhs="
                        + String.valueOf(StringUtils.join((Object) null)) + " rhs=");
            }
            if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-empty-array] metamorphic violation: fixture mismatch input=[] lhs="
                        + String.valueOf(StringUtils.join(EMPTY_ARRAY_LIST)) + " rhs=");
            }
            if (!"".equals(StringUtils.join(NULL_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-null-array-list] metamorphic violation: fixture mismatch input=[null] lhs="
                        + String.valueOf(StringUtils.join(NULL_ARRAY_LIST)) + " rhs=");
            }

            try {
                String exact = StringUtils.join(NULL_TO_STRING_LIST);
                if (!"null".equals(exact)) {
                    throw new RuntimeException("[oracle:anchor-exact-object] metamorphic violation: exact failing-test input must yield \"null\" input=[NullToStringObject] lhs="
                            + String.valueOf(exact) + " rhs=null");
                }
            } catch (Throwable t) {
                handleLibraryThrowable(t, true);
            }

            if (!"abc".equals(StringUtils.join(new String[] { "a", "b", "c" }))) {
                throw new RuntimeException("[oracle:anchor-abc] metamorphic violation: fixture mismatch input=[a,b,c] lhs="
                        + String.valueOf(StringUtils.join(new String[] { "a", "b", "c" })) + " rhs=abc");
            }
            if (!"a".equals(StringUtils.join(new String[] { null, "a", "" }))) {
                throw new RuntimeException("[oracle:anchor-a] metamorphic violation: fixture mismatch input=[null,a,\"\"] lhs="
                        + String.valueOf(StringUtils.join(new String[] { null, "a", "" })) + " rhs=a");
            }
            if (!"foo".equals(StringUtils.join(MIXED_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-mixed-array] metamorphic violation: fixture mismatch input="
                        + java.util.Arrays.toString(MIXED_ARRAY_LIST) + " lhs=" + StringUtils.join(MIXED_ARRAY_LIST) + " rhs=foo");
            }
            if (!"foo2".equals(StringUtils.join(MIXED_TYPE_LIST))) {
                throw new RuntimeException("[oracle:anchor-mixed-type] metamorphic violation: fixture mismatch input="
                        + java.util.Arrays.toString(MIXED_TYPE_LIST) + " lhs=" + StringUtils.join(MIXED_TYPE_LIST) + " rhs=foo2");
            }

            try {
                String exact = StringUtils.join(NULL_TO_STRING_LIST, "/", 0, 1);
                if (!"null".equals(exact)) {
                    throw new RuntimeException("[oracle:anchor-exact-string] metamorphic violation: exact failing path through join(String) must yield \"null\" input=[NullToStringObject] lhs="
                            + String.valueOf(exact) + " rhs=null");
                }
            } catch (Throwable t) {
                handleLibraryThrowable(t, true);
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
            handleLibraryThrowable(t, true);
        }
    }

    private static void handleLibraryThrowable(Throwable t, boolean validByConstruction) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return;
        }
        if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
            throw (RuntimeException) t;
        }
        if (validByConstruction && isRootCause(t)) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            throw new RuntimeException(t);
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
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
<details open><summary>▸ Output (~16,359 chars)</summary>

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
        try {
            runAnchorChecks();
            runExploreChecks(data);
        } catch (RuntimeException t) {
            if (isOracleFailure(t) || isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        } catch (Error t) {
            if (isRootCause(t)) {
                throw t;
            }
        }
    }

    private static void runAnchorChecks() {
        Object nullJoin = StringUtils.join((Object[]) null, ',');
        if (nullJoin != null) {
            throw new RuntimeException("[oracle:anchor-null-array-char] metamorphic violation: documented test expects join((Object[])null, ',') == null lhs=" + String.valueOf(nullJoin));
        }

        String textList = StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR);
        if (!TEXT_LIST_CHAR.equals(textList)) {
            throw new RuntimeException("[oracle:anchor-array-char] metamorphic violation: documented test expects foo;bar;baz lhs=" + printable(textList));
        }

        String emptyList = StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR);
        if (!"".equals(emptyList)) {
            throw new RuntimeException("[oracle:anchor-empty-array-char] metamorphic violation: documented test expects empty string lhs=" + printable(emptyList));
        }

        String mixedArray = StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR);
        if (!";;foo".equals(mixedArray)) {
            throw new RuntimeException("[oracle:anchor-mixed-array-char] metamorphic violation: documented test expects ';;foo' lhs=" + printable(mixedArray));
        }

        String mixedType = StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR);
        if (!"foo;2".equals(mixedType)) {
            throw new RuntimeException("[oracle:anchor-mixed-type-char] metamorphic violation: documented test expects 'foo;2' lhs=" + printable(mixedType));
        }

        String rangedMixedArray = StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1);
        if (!"/".equals(rangedMixedArray)) {
            throw new RuntimeException("[oracle:anchor-range-mixed-array-char] metamorphic violation: documented test expects '/' lhs=" + printable(rangedMixedArray));
        }

        String rangedMixedType0 = StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1);
        if (!"foo".equals(rangedMixedType0)) {
            throw new RuntimeException("[oracle:anchor-range-mixed-type-0-char] metamorphic violation: documented test expects 'foo' lhs=" + printable(rangedMixedType0));
        }

        String nullToStringRanged = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
        if (!"null".equals(nullToStringRanged)) {
            throw new RuntimeException("[oracle:anchor-null-to-string-char] metamorphic violation: documented test expects 'null' lhs=" + printable(nullToStringRanged));
        }

        String rangedMixedType1 = StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2);
        if (!"foo/2".equals(rangedMixedType1)) {
            throw new RuntimeException("[oracle:anchor-range-mixed-type-1-char] metamorphic violation: documented test expects 'foo/2' lhs=" + printable(rangedMixedType1));
        }

        String rangedMixedType2 = StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2);
        if (!"2".equals(rangedMixedType2)) {
            throw new RuntimeException("[oracle:anchor-range-mixed-type-2-char] metamorphic violation: documented test expects '2' lhs=" + printable(rangedMixedType2));
        }

        String rangedEmpty = StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1);
        if (!"".equals(rangedEmpty)) {
            throw new RuntimeException("[oracle:anchor-range-empty-char] metamorphic violation: documented test expects empty string lhs=" + printable(rangedEmpty));
        }

        Object nullJoinObjectArray = StringUtils.join((Object[]) null);
        if (nullJoinObjectArray != null) {
            throw new RuntimeException("[oracle:anchor-null-array-object] metamorphic violation: documented test expects join((Object[])null) == null lhs=" + String.valueOf(nullJoinObjectArray));
        }

        String joinNoArgs = StringUtils.join();
        if (!"".equals(joinNoArgs)) {
            throw new RuntimeException("[oracle:anchor-noargs-object] metamorphic violation: documented test expects empty string lhs=" + printable(joinNoArgs));
        }

        String joinSingleNullVararg = StringUtils.join((Object) null);
        if (!"".equals(joinSingleNullVararg)) {
            throw new RuntimeException("[oracle:anchor-single-null-object] metamorphic violation: documented test expects empty string lhs=" + printable(joinSingleNullVararg));
        }

        String joinEmptyArray = StringUtils.join(EMPTY_ARRAY_LIST);
        if (!"".equals(joinEmptyArray)) {
            throw new RuntimeException("[oracle:anchor-empty-array-object] metamorphic violation: documented test expects empty string lhs=" + printable(joinEmptyArray));
        }

        String joinNullArrayElement = StringUtils.join(NULL_ARRAY_LIST);
        if (!"".equals(joinNullArrayElement)) {
            throw new RuntimeException("[oracle:anchor-null-element-object] metamorphic violation: documented test expects empty string lhs=" + printable(joinNullArrayElement));
        }

        String joinNullToString = StringUtils.join(NULL_TO_STRING_LIST);
        if (!"null".equals(joinNullToString)) {
            throw new RuntimeException("[oracle:anchor-null-to-string-object] metamorphic violation: documented test expects 'null' lhs=" + printable(joinNullToString));
        }

        String joinAbc = StringUtils.join(new String[] { "a", "b", "c" });
        if (!"abc".equals(joinAbc)) {
            throw new RuntimeException("[oracle:anchor-abc-object] metamorphic violation: documented test expects 'abc' lhs=" + printable(joinAbc));
        }

        String joinNullAEmpty = StringUtils.join(new String[] { null, "a", "" });
        if (!"a".equals(joinNullAEmpty)) {
            throw new RuntimeException("[oracle:anchor-null-a-empty-object] metamorphic violation: documented test expects 'a' lhs=" + printable(joinNullAEmpty));
        }

        String joinMixedArrayNoSep = StringUtils.join(MIXED_ARRAY_LIST);
        if (!"foo".equals(joinMixedArrayNoSep)) {
            throw new RuntimeException("[oracle:anchor-mixed-array-object] metamorphic violation: documented test expects 'foo' lhs=" + printable(joinMixedArrayNoSep));
        }

        String joinMixedTypeNoSep = StringUtils.join(MIXED_TYPE_LIST);
        if (!"foo2".equals(joinMixedTypeNoSep)) {
            throw new RuntimeException("[oracle:anchor-mixed-type-object] metamorphic violation: documented test expects 'foo2' lhs=" + printable(joinMixedTypeNoSep));
        }

        /* Contract check on shared EMPTY state:
           join with no items returns EMPTY; trimToEmpty(null), stripToEmpty(null), substring("",0), and left("x",-1)
           also document returning the empty string. A "fix" that changes join's empty-result bookkeeping would break this
           observable agreement even if it merely avoids the throw. */
        String emptyFromJoinChar = StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1);
        String emptyFromJoinString = StringUtils.join(MIXED_TYPE_LIST, "/", 2, 1);
        String emptyFromTrim = StringUtils.trimToEmpty(null);
        String emptyFromStrip = StringUtils.stripToEmpty(null);
        String emptyFromSubstring = StringUtils.substring("", 0);
        String emptyFromLeft = StringUtils.left("x", -1);
        if (!(sameEmpty(emptyFromJoinChar, emptyFromTrim)
                && sameEmpty(emptyFromJoinChar, emptyFromStrip)
                && sameEmpty(emptyFromJoinChar, emptyFromSubstring)
                && sameEmpty(emptyFromJoinChar, emptyFromLeft)
                && sameEmpty(emptyFromJoinChar, emptyFromJoinString))) {
            throw new RuntimeException("[oracle:shared-empty] metamorphic violation: empty-returning StringUtils methods disagree"
                    + " joinChar=" + printable(emptyFromJoinChar)
                    + " joinString=" + printable(emptyFromJoinString)
                    + " trim=" + printable(emptyFromTrim)
                    + " strip=" + printable(emptyFromStrip)
                    + " substring=" + printable(emptyFromSubstring)
                    + " left=" + printable(emptyFromLeft));
        }
    }

    private static void runExploreChecks(FuzzedDataProvider data) {
        int len = data.consumeInt(1, 8);
        Object[] array = new Object[len];
        int specialIndex = data.consumeInt(0, len - 1);

        for (int i = 0; i < len; i++) {
            if (i == specialIndex) {
                array[i] = NULL_TO_STRING;
            } else {
                int kind = data.consumeInt(0, 4);
                switch (kind) {
                    case 0:
                        array[i] = null;
                        break;
                    case 1:
                        array[i] = data.consumeAsciiString(16);
                        break;
                    case 2:
                        array[i] = Long.valueOf(data.consumeInt(-1000000, 1000000));
                        break;
                    case 3:
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                    default:
                        array[i] = data.consumeString(16);
                        break;
                }
            }
        }

        int startIndex = specialIndex;
        int endIndex = data.consumeInt(startIndex + 1, len);
        char sepChar = (char) data.consumeInt(1, 126);
        String sepString = String.valueOf(sepChar);

        try {
            StringUtils.join(array, sepChar, startIndex, endIndex);
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }

        try {
            StringUtils.join(array, sepString, startIndex, endIndex);
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }

        try {
            StringUtils.join(array);
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }

        Object[] regular = new Object[data.consumeInt(1, 8)];
        for (int i = 0; i < regular.length; i++) {
            int kind = data.consumeInt(0, 3);
            switch (kind) {
                case 0:
                    regular[i] = null;
                    break;
                case 1:
                    regular[i] = data.consumeAsciiString(12);
                    break;
                case 2:
                    regular[i] = Integer.valueOf(data.consumeInt(-1000000, 1000000));
                    break;
                default:
                    regular[i] = data.consumeString(12);
                    break;
            }
        }
        int s = data.consumeInt(0, regular.length - 1);
        int e = data.consumeInt(s + 1, regular.length);
        char c = (char) data.consumeInt(1, 126);
        String sSep = String.valueOf(c);

        /* Equivalent-input relation from the documented semantics of the two patched overloads:
           both join(Object[], char, start, end) and join(Object[], String, start, end) insert the same separator
           between the same elements. Therefore, when the String separator is exactly String.valueOf(char),
           both overloads must produce identical output for every valid array/range. A patch that merely avoids
           the crash by skipping appends or mishandling the first element will violate this without throwing. */
        try {
            String lhs = StringUtils.join(regular, c, s, e);
            String rhs = StringUtils.join(regular, sSep, s, e);
            if (lhs != null ? !lhs.equals(rhs) : rhs != null) {
                throw new RuntimeException("[oracle:char-vs-string] metamorphic violation: join overloads disagree inputRange="
                        + s + ".." + e + " sepChar=" + (int) c + " lhs=" + printable(lhs) + " rhs=" + printable(rhs));
            }
        } catch (RuntimeException t) {
            if (isOracleFailure(t) || isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }

        int prefixLen = data.consumeInt(1, 4);
        int suffixLen = data.consumeInt(1, 4);
        Object[] withContext = new Object[prefixLen + 1 + suffixLen];
        for (int i = 0; i < prefixLen; i++) {
            withContext[i] = data.consumeAsciiString(8);
        }
        withContext[prefixLen] = NULL_TO_STRING;
        for (int i = prefixLen + 1; i < withContext.length; i++) {
            withContext[i] = data.consumeAsciiString(8);
        }
        int constrainedEnd = data.consumeInt(prefixLen + 1, withContext.length);
        try {
            String charJoin = StringUtils.join(withContext, '|', prefixLen, constrainedEnd);
            String stringJoin = StringUtils.join(withContext, "|", prefixLen, constrainedEnd);
            if (charJoin != null ? !charJoin.equals(stringJoin) : stringJoin != null) {
                throw new RuntimeException("[oracle:null-tostring-overloads] metamorphic violation: join overloads disagree on null-toString element start="
                        + prefixLen + " end=" + constrainedEnd + " lhs=" + printable(charJoin) + " rhs=" + printable(stringJoin));
            }
            if (charJoin != null && charJoin.length() >= 4 && !charJoin.startsWith("null")) {
                throw new RuntimeException("[oracle:null-tostring-prefix] metamorphic violation: first selected element has toString()==null, documented behavior from tests expects textual 'null' prefix lhs=" + printable(charJoin));
            }
        } catch (RuntimeException t) {
            if (isOracleFailure(t) || isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static boolean sameEmpty(String a, String b) {
        return a != null && b != null && a.length() == 0 && b.length() == 0 && a.equals(b);
    }

    private static boolean isOracleFailure(Throwable t) {
        return t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static String printable(String s) {
        return s == null ? "null" : "\"" + s + "\"";
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
<details open><summary>▸ Output (~12,263 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final Object nullToStringObject = new Object() {
            @Override
            public String toString() {
                return null;
            }
        };
        final Object[] NULL_TO_STRING_LIST = new Object[] { nullToStringObject };
        final String[] EMPTY_ARRAY_LIST = new String[] {};
        final String[] NULL_ARRAY_LIST = new String[] { null };
        final String[] MIXED_ARRAY_LIST = new String[] { null, "", "foo" };
        final Object[] MIXED_TYPE_LIST = new Object[] { "foo", Long.valueOf(2L) };
        final char SEPARATOR_CHAR = ';';

        // ANCHOR: faithful copies of the triggering test calls.
        try {
            if (StringUtils.join((Object[]) null, ',') != null) {
                throw new RuntimeException("[oracle:anchor-null-array-char] metamorphic violation: join((Object[])null, ',') must return null");
            }
            if (StringUtils.join((Object[]) null) != null) {
                throw new RuntimeException("[oracle:anchor-null-array-varargs] metamorphic violation: join((Object[])null) must return null");
            }
            if (!"".equals(StringUtils.join())) {
                throw new RuntimeException("[oracle:anchor-empty-varargs] metamorphic violation: join() must return empty string");
            }
            if (!"".equals(StringUtils.join((Object) null))) {
                throw new RuntimeException("[oracle:anchor-object-null] metamorphic violation: join((Object)null) must return empty string");
            }
            if (!"null".equals(StringUtils.join(NULL_TO_STRING_LIST))) {
                throw new RuntimeException("[oracle:anchor-null-tostring-varargs] metamorphic violation: expected \"null\" for singleton object with toString()==null");
            }
            if (!"null".equals(StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1))) {
                throw new RuntimeException("[oracle:anchor-null-tostring-char] metamorphic violation: expected \"null\" for singleton object with toString()==null and valid range");
            }
            if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-empty-array] metamorphic violation: empty array must join to empty string");
            }
            if (!"".equals(StringUtils.join(NULL_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-null-element-array] metamorphic violation: single null element array must join to empty string");
            }
            if (!"foo".equals(StringUtils.join(MIXED_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-mixed-array] metamorphic violation: mixed array expected foo");
            }
            if (!"foo2".equals(StringUtils.join(MIXED_TYPE_LIST))) {
                throw new RuntimeException("[oracle:anchor-mixed-types] metamorphic violation: mixed type array expected foo2");
            }
            if (!"/".equals(StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1))) {
                throw new RuntimeException("[oracle:anchor-range-char] metamorphic violation: expected /");
            }
            if (!"foo".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1))) {
                throw new RuntimeException("[oracle:anchor-range-foo] metamorphic violation: expected foo");
            }
            if (!"foo/2".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2))) {
                throw new RuntimeException("[oracle:anchor-range-foo2] metamorphic violation: expected foo/2");
            }
            if (!"2".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2))) {
                throw new RuntimeException("[oracle:anchor-range-2] metamorphic violation: expected 2");
            }
            if (!"".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1))) {
                throw new RuntimeException("[oracle:anchor-empty-range] metamorphic violation: reversed range must yield empty string");
            }
        } catch (RuntimeException t) {
            boolean hasJoinFrame = false;
            StackTraceElement[] st = t.getStackTrace();
            if (st != null) {
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.lang3.StringUtils".equals(st[i].getClassName())
                            && "join".equals(st[i].getMethodName())) {
                        hasJoinFrame = true;
                        break;
                    }
                }
            }
            if (t instanceof NullPointerException && hasJoinFrame) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        }

        // Shared EMPTY state agreement checks.
        try {
            String emptyFromJoinChar = StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1);
            String emptyFromJoinString = StringUtils.join(MIXED_TYPE_LIST, "/", 2, 1);
            String emptyFromTrim = StringUtils.trimToEmpty(null);
            String emptyFromStrip = StringUtils.stripToEmpty(null);
            String emptyFromSubstring = StringUtils.substring("", 0);
            String emptyFromSubstring2 = StringUtils.substring("", 0, 0);
            String emptyFromLeft = StringUtils.left("abc", -1);
            if (!(emptyFromJoinChar.equals(emptyFromJoinString)
                    && emptyFromJoinChar.equals(emptyFromTrim)
                    && emptyFromJoinChar.equals(emptyFromStrip)
                    && emptyFromJoinChar.equals(emptyFromSubstring)
                    && emptyFromJoinChar.equals(emptyFromSubstring2)
                    && emptyFromJoinChar.equals(emptyFromLeft))) {
                throw new RuntimeException("[oracle:empty-agreement] metamorphic violation: EMPTY-backed APIs disagree");
            }
        } catch (RuntimeException t) {
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        }

        int len = data.consumeInt(1, 6);
        Object[] arr = new Object[len];
        String[] mirrored = new String[len];

        boolean useNullToStringAtZero = data.consumeBoolean();
        for (int i = 0; i < len; i++) {
            int kind = data.consumeInt(0, 4);
            if (i == 0 && useNullToStringAtZero) {
                arr[i] = nullToStringObject;
                mirrored[i] = "null";
                continue;
            }
            switch (kind) {
                case 0:
                    arr[i] = null;
                    mirrored[i] = null;
                    break;
                case 1:
                    String s1 = data.consumeAsciiString(8);
                    arr[i] = s1;
                    mirrored[i] = s1;
                    break;
                case 2:
                    String s2 = data.consumeString(8);
                    arr[i] = s2;
                    mirrored[i] = s2;
                    break;
                case 3:
                    Long v = Long.valueOf(data.consumeInt(-1000, 1000));
                    arr[i] = v;
                    mirrored[i] = String.valueOf(v);
                    break;
                default:
                    Integer iv = Integer.valueOf(data.consumeInt(-1000, 1000));
                    arr[i] = iv;
                    mirrored[i] = String.valueOf(iv);
                    break;
            }
        }

        int start = data.consumeInt(0, len - 1);
        int end = data.consumeInt(start, len);
        char sepChar = (char) (data.consumeByte() & 0x7f);
        String sepString = data.consumeBoolean() ? String.valueOf(sepChar) : data.consumeString(4);
        if (sepString == null) {
            sepString = "";
        }

        // Contract asserted:
        // join(Object[], char, s, e) and join(Object[], String.valueOf(char), s, e) are sibling overloads over the same
        // valid range and must agree on observable output for the same separator character.
        // A throw-deleting patch or a patch that silently skips/changes element rendering can violate this without throwing.
        try {
            String lhs = StringUtils.join(arr, sepChar, start, end);
            String rhs = StringUtils.join(arr, String.valueOf(sepChar), start, end);
            if (!lhs.equals(rhs)) {
                throw new RuntimeException("[oracle:char-string-overload] metamorphic violation: equivalent separator overloads disagree inputStart="
                        + start + " inputEnd=" + end + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (RuntimeException t) {
            boolean hasJoinFrame = false;
            StackTraceElement[] st = t.getStackTrace();
            if (st != null) {
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.lang3.StringUtils".equals(st[i].getClassName())
                            && "join".equals(st[i].getMethodName())) {
                        hasJoinFrame = true;
                        break;
                    }
                }
            }
            if (t instanceof NullPointerException && hasJoinFrame && start == 0 && end > start && useNullToStringAtZero) {
                throw t;
            }
            if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                return;
            }
            if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
            return;
        }

        // Contract asserted:
        // The implementation appends each non-null element via StringBuilder.append(Object); for an object whose
        // toString() returns null, StringBuilder renders "null". Therefore replacing that element with the literal
        // "null" must preserve the joined result on the same valid range.
        try {
            String joinedOriginalChar = StringUtils.join(arr, sepChar, start, end);
            String joinedMirroredChar = StringUtils.join(mirrored, sepChar, start, end);
            if (!joinedOriginalChar.equals(joinedMirroredChar)) {
                throw new RuntimeException("[oracle:null-tostring-char] metamorphic violation: replacing toString()==null object with literal \"null\" changed output lhs="
                        + joinedOriginalChar + " rhs=" + joinedMirroredChar);
            }

            String joinedOriginalString = StringUtils.join(arr, sepString, start, end);
            String joinedMirroredString = StringUtils.join(mirrored, sepString, start, end);
            if (!joinedOriginalString.equals(joinedMirroredString)) {
                throw new RuntimeException("[oracle:null-tostring-string] metamorphic violation: replacing toString()==null object with literal \"null\" changed output lhs="
                        + joinedOriginalString + " rhs=" + joinedMirroredString);
            }
        } catch (RuntimeException t) {
            boolean hasJoinFrame = false;
            StackTraceElement[] st = t.getStackTrace();
            if (st != null) {
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.lang3.StringUtils".equals(st[i].getClassName())
                            && "join".equals(st[i].getMethodName())) {
                        hasJoinFrame = true;
                        break;
                    }
                }
            }
            if (t instanceof NullPointerException && hasJoinFrame && start == 0 && end > start && useNullToStringAtZero) {
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
<details open><summary>▸ Output (~14,705 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final String[] ARRAY_LIST = { "foo", "bar", "baz" };
        final String[] EMPTY_ARRAY_LIST = {};
        final String[] NULL_ARRAY_LIST = { null };
        final Object[] NULL_TO_STRING_LIST = { new Object() { @Override public String toString() { return null; } } };
        final String[] MIXED_ARRAY_LIST = { null, "", "foo" };
        final Object[] MIXED_TYPE_LIST = { "foo", Long.valueOf(2L) };
        final char SEPARATOR_CHAR = ';';
        final String TEXT_LIST_CHAR = "foo;bar;baz";

        Object anchorCharResult = null;
        Object anchorObjectResult = null;

        try {
            String nullArrayJoin = StringUtils.join((Object[]) null, ',');
            if (nullArrayJoin != null) {
                throw new RuntimeException("[oracle:anchor-null-array-char] metamorphic violation: join((Object[])null, ',') must return null input=" + null + " lhs=" + nullArrayJoin + " rhs=null");
            }

            String textList = StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR);
            if (!TEXT_LIST_CHAR.equals(textList)) {
                throw new RuntimeException("[oracle:anchor-array-char] metamorphic violation: test fixture contract input=ARRAY_LIST lhs=" + textList + " rhs=" + TEXT_LIST_CHAR);
            }

            String emptyJoin = StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR);
            if (!"".equals(emptyJoin)) {
                throw new RuntimeException("[oracle:anchor-empty-array-char] metamorphic violation: empty array must join to EMPTY input=EMPTY_ARRAY_LIST lhs=" + emptyJoin + " rhs=");
            }

            String mixedArrayJoin = StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR);
            if (!";;foo".equals(mixedArrayJoin)) {
                throw new RuntimeException("[oracle:anchor-mixed-array-char] metamorphic violation: test fixture contract input=MIXED_ARRAY_LIST lhs=" + mixedArrayJoin + " rhs=;;foo");
            }

            String mixedTypeJoin = StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR);
            if (!"foo;2".equals(mixedTypeJoin)) {
                throw new RuntimeException("[oracle:anchor-mixed-type-char] metamorphic violation: test fixture contract input=MIXED_TYPE_LIST lhs=" + mixedTypeJoin + " rhs=foo;2");
            }

            String slashRange = StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1);
            if (!"/".equals(slashRange)) {
                throw new RuntimeException("[oracle:anchor-range-char] metamorphic violation: test fixture contract input=MIXED_ARRAY_LIST lhs=" + slashRange + " rhs=/");
            }

            String oneFoo = StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1);
            if (!"foo".equals(oneFoo)) {
                throw new RuntimeException("[oracle:anchor-range-char-one] metamorphic violation: test fixture contract input=MIXED_TYPE_LIST lhs=" + oneFoo + " rhs=foo");
            }

            anchorCharResult = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);

            String foo2 = StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2);
            if (!"foo/2".equals(foo2)) {
                throw new RuntimeException("[oracle:anchor-range-char-two] metamorphic violation: test fixture contract input=MIXED_TYPE_LIST lhs=" + foo2 + " rhs=foo/2");
            }

            String just2 = StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2);
            if (!"2".equals(just2)) {
                throw new RuntimeException("[oracle:anchor-range-char-tail] metamorphic violation: test fixture contract input=MIXED_TYPE_LIST lhs=" + just2 + " rhs=2");
            }

            String emptyReversed = StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1);
            if (!"".equals(emptyReversed)) {
                throw new RuntimeException("[oracle:anchor-range-char-empty] metamorphic violation: test fixture contract input=MIXED_TYPE_LIST lhs=" + emptyReversed + " rhs=");
            }

            String nullArrayObj = StringUtils.join((Object[]) null);
            if (nullArrayObj != null) {
                throw new RuntimeException("[oracle:anchor-null-array-obj] metamorphic violation: join((Object[])null) must return null input=" + null + " lhs=" + nullArrayObj + " rhs=null");
            }

            String noArgs = StringUtils.join();
            if (!"".equals(noArgs)) {
                throw new RuntimeException("[oracle:anchor-noargs-obj] metamorphic violation: join() must return EMPTY input=[] lhs=" + noArgs + " rhs=");
            }

            String oneNullArg = StringUtils.join((Object) null);
            if (!"".equals(oneNullArg)) {
                throw new RuntimeException("[oracle:anchor-one-null-obj] metamorphic violation: join((Object)null) must return EMPTY input=[null] lhs=" + oneNullArg + " rhs=");
            }

            String emptyObj = StringUtils.join(EMPTY_ARRAY_LIST);
            if (!"".equals(emptyObj)) {
                throw new RuntimeException("[oracle:anchor-empty-array-obj] metamorphic violation: empty array must join to EMPTY input=EMPTY_ARRAY_LIST lhs=" + emptyObj + " rhs=");
            }

            String nullArrayListObj = StringUtils.join(NULL_ARRAY_LIST);
            if (!"".equals(nullArrayListObj)) {
                throw new RuntimeException("[oracle:anchor-null-array-list-obj] metamorphic violation: singleton null element joins to EMPTY input=NULL_ARRAY_LIST lhs=" + nullArrayListObj + " rhs=");
            }

            anchorObjectResult = StringUtils.join(NULL_TO_STRING_LIST);

            String abc = StringUtils.join(new String[] { "a", "b", "c" });
            if (!"abc".equals(abc)) {
                throw new RuntimeException("[oracle:anchor-abc-obj] metamorphic violation: test fixture contract input=[a,b,c] lhs=" + abc + " rhs=abc");
            }

            String a = StringUtils.join(new String[] { null, "a", "" });
            if (!"a".equals(a)) {
                throw new RuntimeException("[oracle:anchor-a-obj] metamorphic violation: test fixture contract input=[null,a,empty] lhs=" + a + " rhs=a");
            }

            String foo = StringUtils.join(MIXED_ARRAY_LIST);
            if (!"foo".equals(foo)) {
                throw new RuntimeException("[oracle:anchor-foo-obj] metamorphic violation: test fixture contract input=MIXED_ARRAY_LIST lhs=" + foo + " rhs=foo");
            }

            String foo2obj = StringUtils.join(MIXED_TYPE_LIST);
            if (!"foo2".equals(foo2obj)) {
                throw new RuntimeException("[oracle:anchor-foo2-obj] metamorphic violation: test fixture contract input=MIXED_TYPE_LIST lhs=" + foo2obj + " rhs=foo2");
            }
        } catch (RuntimeException t) {
            boolean cleanRejection = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            if (!cleanRejection && t instanceof NullPointerException) {
                boolean throughJoin = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName())
                            && "join".equals(ste.getMethodName())) {
                        throughJoin = true;
                        break;
                    }
                }
                if (throughJoin) {
                    throw t;
                }
            }
            return;
        }

        if (!"null".equals(anchorCharResult)) {
            throw new RuntimeException("[oracle:anchor-char-null-tostring] metamorphic violation: documented test fixture expects Object.toString()==null to be rendered as literal null for a one-element range input=NULL_TO_STRING_LIST lhs=" + anchorCharResult + " rhs=null");
        }
        if (!"null".equals(anchorObjectResult)) {
            throw new RuntimeException("[oracle:anchor-obj-null-tostring] metamorphic violation: documented test fixture expects Object.toString()==null to be rendered as literal null for object-array join input=NULL_TO_STRING_LIST lhs=" + anchorObjectResult + " rhs=null");
        }

        String e1 = StringUtils.trimToEmpty(null);
        String e2 = StringUtils.stripToEmpty(null);
        String e3 = StringUtils.substring("", 0);
        String e4 = StringUtils.substring("", 0, 0);
        String e5 = StringUtils.left("x", -1);
        if (!(StringUtils.EMPTY.equals(e1) && StringUtils.EMPTY.equals(e2) && StringUtils.EMPTY.equals(e3)
                && StringUtils.EMPTY.equals(e4) && StringUtils.EMPTY.equals(e5))) {
            throw new RuntimeException("[oracle:empty-shared-state] metamorphic violation: methods documented to return EMPTY must agree lhs="
                    + e1 + "|" + e2 + "|" + e3 + "|" + e4 + "|" + e5 + " rhs=" + StringUtils.EMPTY);
        }

        Object bad = new Object() { @Override public String toString() { return null; } };
        char fuzzChar = (char) (data.consumeByte() & 0xff);
        String fuzzSep = data.consumeString(16);
        boolean useNullSep = data.consumeBoolean();
        if (useNullSep) {
            fuzzSep = null;
        }

        int len = data.consumeInt(1, 4);
        Object[] arr = new Object[len];
        int badIndex = data.consumeInt(0, len - 1);
        for (int i = 0; i < len; i++) {
            if (i == badIndex) {
                arr[i] = bad;
            } else {
                int choice = data.consumeInt(0, 4);
                if (choice == 0) {
                    arr[i] = null;
                } else if (choice == 1) {
                    arr[i] = data.consumeAsciiString(8);
                } else if (choice == 2) {
                    arr[i] = Long.valueOf(data.consumeInt(-1000, 1000));
                } else if (choice == 3) {
                    arr[i] = data.consumeString(8);
                } else {
                    arr[i] = Boolean.valueOf(data.consumeBoolean());
                }
            }
        }

        int startIndex = badIndex;
        int endIndex = data.consumeInt(startIndex + 1, len);

        try {
            StringUtils.join(arr, fuzzChar, startIndex, endIndex);
        } catch (RuntimeException t) {
            boolean cleanRejection = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            if (!cleanRejection && t instanceof NullPointerException) {
                boolean throughJoin = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName())
                            && "join".equals(ste.getMethodName())) {
                        throughJoin = true;
                        break;
                    }
                }
                if (throughJoin) {
                    throw t;
                }
            }
        }

        try {
            StringUtils.join(arr, fuzzSep, startIndex, endIndex);
        } catch (RuntimeException t) {
            boolean cleanRejection = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            if (!cleanRejection && t instanceof NullPointerException) {
                boolean throughJoin = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName())
                            && "join".equals(ste.getMethodName())) {
                        throughJoin = true;
                        break;
                    }
                }
                if (throughJoin) {
                    throw t;
                }
            }
        }

        Object[] single = new Object[] { bad };
        String lhsChar;
        String rhsObj;
        try {
            lhsChar = StringUtils.join(single, fuzzChar, 0, 1);
            rhsObj = StringUtils.join(single);
        } catch (RuntimeException t) {
            boolean cleanRejection = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            if (!cleanRejection && t instanceof NullPointerException) {
                boolean throughJoin = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName())
                            && "join".equals(ste.getMethodName())) {
                        throughJoin = true;
                        break;
                    }
                }
                if (throughJoin) {
                    throw t;
                }
            }
            return;
        }
        /* Contract used for this oracle: with exactly one selected item, join(Object[], sep, 0, 1)
           never appends a separator because the loop's separator branch is guarded by i > startIndex.
           Therefore it must agree with join(Object[]) on the same singleton input. A "fix" that simply
           skips appending the element or changes null-toString handling would break this equality. */
        if (!String.valueOf(lhsChar).equals(String.valueOf(rhsObj))) {
            throw new RuntimeException("[oracle:singleton-char-vs-varargs] metamorphic violation: one-element join with any char separator must equal join(Object[]) input=" + String.valueOf(single[0]) + " lhs=" + lhsChar + " rhs=" + rhsObj);
        }

        String lhsString;
        String rhsString;
        try {
            lhsString = StringUtils.join(single, fuzzSep, 0, 1);
            rhsString = StringUtils.join(single);
        } catch (RuntimeException t) {
            boolean cleanRejection = t instanceof IllegalArgumentException || t instanceof NumberFormatException;
            if (!cleanRejection && t instanceof NullPointerException) {
                boolean throughJoin = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName())
                            && "join".equals(ste.getMethodName())) {
                        throughJoin = true;
                        break;
                    }
                }
                if (throughJoin) {
                    throw t;
                }
            }
            return;
        }
        if (!String.valueOf(lhsString).equals(String.valueOf(rhsString))) {
            throw new RuntimeException("[oracle:singleton-string-vs-varargs] metamorphic violation: one-element join with any string separator must equal join(Object[]) input=" + String.valueOf(single[0]) + " lhs=" + lhsString + " rhs=" + rhsString);
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
<details open><summary>▸ Output (~10,677 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] NULL_TO_STRING_LIST = new Object[] {
            new Object() {
                @Override
                public String toString() {
                    return null;
                }
            }
        };
        String[] EMPTY_ARRAY_LIST = new String[] {};
        String[] NULL_ARRAY_LIST = new String[] { null };
        String[] MIXED_ARRAY_LIST = new String[] { null, "", "foo" };
        Object[] MIXED_TYPE_LIST = new Object[] { "foo", Long.valueOf(2L) };
        String[] ARRAY_LIST = new String[] { "foo", "bar", "baz" };
        char SEPARATOR_CHAR = ';';
        String TEXT_LIST_CHAR = "foo;bar;baz";

        try {
            if (StringUtils.join((Object[]) null, ',') != null) {
                throw new RuntimeException("[oracle:anchor-null-char] metamorphic violation: expected null for join((Object[]) null, ',')");
            }
            if (StringUtils.join((Object[]) null) != null) {
                throw new RuntimeException("[oracle:anchor-null-varargs] metamorphic violation: expected null for join((Object[]) null)");
            }
            if (!"".equals(StringUtils.join())) {
                throw new RuntimeException("[oracle:anchor-empty-varargs] metamorphic violation: expected empty string for join()");
            }
            if (!"".equals(StringUtils.join((Object) null))) {
                throw new RuntimeException("[oracle:anchor-object-null] metamorphic violation: expected empty string for join((Object) null)");
            }
            if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-empty-array] metamorphic violation: expected empty string for empty array");
            }
            if (!"".equals(StringUtils.join(NULL_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-null-array-entry] metamorphic violation: expected empty string for single null entry");
            }
            if (!TEXT_LIST_CHAR.equals(StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-text-list] metamorphic violation: expected " + TEXT_LIST_CHAR);
            }
            if (!";;foo".equals(StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-mixed-array-char] metamorphic violation: expected ';;foo'");
            }
            if (!"foo;2".equals(StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-mixed-type-char] metamorphic violation: expected 'foo;2'");
            }
            if (!"/".equals(StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1))) {
                throw new RuntimeException("[oracle:anchor-slice-slash] metamorphic violation: expected '/'");
            }
            if (!"foo".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1))) {
                throw new RuntimeException("[oracle:anchor-slice-foo] metamorphic violation: expected 'foo'");
            }

            String anchoredChar = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            if (!"null".equals(anchoredChar)) {
                throw new RuntimeException("[oracle:anchor-null-to-string-char] metamorphic violation: input=NULL_TO_STRING_LIST lhs=" + anchoredChar + " rhs=null");
            }

            String anchoredVarargs = StringUtils.join(NULL_TO_STRING_LIST);
            if (!"null".equals(anchoredVarargs)) {
                throw new RuntimeException("[oracle:anchor-null-to-string-varargs] metamorphic violation: input=NULL_TO_STRING_LIST lhs=" + anchoredVarargs + " rhs=null");
            }

            if (!"foo/2".equals(StringUtils.join(MIXED_TYPE_LIST, "/", 0, 2))) {
                throw new RuntimeException("[oracle:anchor-string-sep] metamorphic violation: expected 'foo/2'");
            }
        } catch (RuntimeException t) {
            boolean stackHasJoin = false;
            for (StackTraceElement e : t.getStackTrace()) {
                if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                    stackHasJoin = true;
                    break;
                }
            }
            boolean cleanRejection =
                t instanceof IllegalArgumentException ||
                t instanceof NumberFormatException;
            if (!cleanRejection && t instanceof NullPointerException && stackHasJoin) {
                throw t;
            }
        }

        /* Contract asserted below:
           join returns EMPTY when endIndex - startIndex <= 0, and trimToEmpty/stripToEmpty/substring/left
           also expose the same EMPTY constant through their documented empty-result cases.
           A throw-deleting or wrong-return patch that changes join's empty-result behavior would violate this observable agreement. */
        try {
            String emptyFromJoinChar = StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1);
            String emptyFromJoinString = StringUtils.join(MIXED_TYPE_LIST, "/", 2, 1);
            String emptyFromTrim = StringUtils.trimToEmpty(null);
            String emptyFromStrip = StringUtils.stripToEmpty(null);
            String emptyFromSubstring = StringUtils.substring("", 0);
            String emptyFromLeft = StringUtils.left("abc", -1);
            if (!(emptyFromJoinChar.equals(emptyFromTrim)
                    && emptyFromJoinString.equals(emptyFromTrim)
                    && emptyFromStrip.equals(emptyFromTrim)
                    && emptyFromSubstring.equals(emptyFromTrim)
                    && emptyFromLeft.equals(emptyFromTrim))) {
                throw new RuntimeException("[oracle:empty-agreement] metamorphic violation: join/EMPTY readers disagree lhs="
                        + emptyFromJoinChar + "," + emptyFromJoinString + " rhs="
                        + emptyFromTrim + "," + emptyFromStrip + "," + emptyFromSubstring + "," + emptyFromLeft);
            }
        } catch (RuntimeException t) {
            boolean stackHasJoin = false;
            for (StackTraceElement e : t.getStackTrace()) {
                if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                    stackHasJoin = true;
                    break;
                }
            }
            if (t instanceof NullPointerException && stackHasJoin) {
                throw t;
            }
            if (!(t instanceof IllegalArgumentException) && !(t instanceof NumberFormatException)
                    && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
        }

        int len = data.consumeInt(1, 8);
        Object[] fuzzArray = new Object[len];
        for (int i = 0; i < len; i++) {
            int kind = data.consumeInt(0, 3);
            if (kind == 0) {
                fuzzArray[i] = null;
            } else if (kind == 1) {
                fuzzArray[i] = data.consumeAsciiString(16);
            } else if (kind == 2) {
                fuzzArray[i] = Long.valueOf(data.consumeInt(-1000000, 1000000));
            } else {
                fuzzArray[i] = data.consumeString(8);
            }
        }

        int start = data.consumeInt(0, len - 1);
        int end = data.consumeInt(start + 1, len);
        fuzzArray[start] = NULL_TO_STRING_LIST[0];

        char sepChar = (char) (data.consumeByte() & 0x7f);
        String sepString = String.valueOf(sepChar);

        try {
            String charJoined = StringUtils.join(fuzzArray, sepChar, start, end);
            String stringJoined = StringUtils.join(fuzzArray, sepString, start, end);

            if (!charJoined.equals(stringJoined)) {
                throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: equivalent char/string separator overloads disagree inputStart="
                        + start + " inputEnd=" + end + " lhs=" + charJoined + " rhs=" + stringJoined);
            }

            /* Contract asserted below:
               for any valid array slice containing exactly one element, separators are not emitted,
               so join(array, sep, i, i+1) must equal join(new Object[]{array[i]}, sep, 0, 1).
               This uses only real library calls on equivalent inputs and catches silent wrong-output fixes. */
            String sliceChar = StringUtils.join(fuzzArray, sepChar, start, start + 1);
            String singletonChar = StringUtils.join(new Object[] { fuzzArray[start] }, sepChar, 0, 1);
            if (!sliceChar.equals(singletonChar)) {
                throw new RuntimeException("[oracle:single-slice-char] metamorphic violation: one-element slice mismatch inputStart="
                        + start + " lhs=" + sliceChar + " rhs=" + singletonChar);
            }

            String sliceString = StringUtils.join(fuzzArray, sepString, start, start + 1);
            String singletonString = StringUtils.join(new Object[] { fuzzArray[start] }, sepString, 0, 1);
            if (!sliceString.equals(singletonString)) {
                throw new RuntimeException("[oracle:single-slice-string] metamorphic violation: one-element slice mismatch inputStart="
                        + start + " lhs=" + sliceString + " rhs=" + singletonString);
            }

            if (!"null".equals(sliceChar) || !"null".equals(sliceString)) {
                throw new RuntimeException("[oracle:null-to-string-value] metamorphic violation: documented test case requires null-returning toString to join as 'null' lhs="
                        + sliceChar + " rhs=" + sliceString);
            }
        } catch (RuntimeException t) {
            boolean stackHasJoin = false;
            for (StackTraceElement e : t.getStackTrace()) {
                if ("org.apache.commons.lang3.StringUtils".equals(e.getClassName()) && "join".equals(e.getMethodName())) {
                    stackHasJoin = true;
                    break;
                }
            }
            boolean cleanRejection =
                t instanceof IllegalArgumentException ||
                t instanceof NumberFormatException;
            if (!cleanRejection && t instanceof NullPointerException && stackHasJoin) {
                throw t;
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
<details open><summary>▸ Output (~10,875 chars)</summary>

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

    /*
     * This mirrors the test fixture exactly: join must accept an element whose
     * toString() returns null and render it as "null" rather than crash.
     */
    private static final Object[] NULL_TO_STRING_LIST = {
        new Object() {
            @Override
            public String toString() {
                return null;
            }
        }
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchorChecks();
        runSharedEmptyChecks();

        try {
            runExploreChecks(data);
        } catch (RuntimeException e) {
            if (isCleanRejection(e)) {
                return;
            }
            throw e;
        } catch (Error e) {
            throw e;
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void runAnchorChecks() {
        checkEquals("anchor-null-array-char", null, StringUtils.join((Object[]) null, ','));
        checkEquals("anchor-array-char", TEXT_LIST_CHAR, StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR));
        checkEquals("anchor-empty-array-char", "", StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR));
        checkEquals("anchor-mixed-array-char", ";;foo", StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR));
        checkEquals("anchor-mixed-type-char", "foo;2", StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR));
        checkEquals("anchor-mixed-array-char-range", "/", StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1));
        checkEquals("anchor-mixed-type-char-range-a", "foo", StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1));
        checkRootCauseOrEquals("anchor-null-to-string-char-range", "null", true, new JoinCall() {
            @Override
            public String call() {
                return StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            }
        });
        checkEquals("anchor-mixed-type-char-range-b", "foo/2", StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2));
        checkEquals("anchor-mixed-type-char-range-c", "2", StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2));
        checkEquals("anchor-mixed-type-char-range-d", "", StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1));

        checkEquals("anchor-null-array", null, StringUtils.join((Object[]) null));
        checkEquals("anchor-empty-varargs", "", StringUtils.join());
        checkEquals("anchor-single-null-vararg", "", StringUtils.join((Object) null));
        checkEquals("anchor-empty-array", "", StringUtils.join(EMPTY_ARRAY_LIST));
        checkEquals("anchor-null-array-list", "", StringUtils.join(NULL_ARRAY_LIST));
        checkRootCauseOrEquals("anchor-null-to-string", "null", true, new JoinCall() {
            @Override
            public String call() {
                return StringUtils.join(NULL_TO_STRING_LIST);
            }
        });
        checkEquals("anchor-abc", "abc", StringUtils.join(new String[] { "a", "b", "c" }));
        checkEquals("anchor-null-a-empty", "a", StringUtils.join(new String[] { null, "a", "" }));
        checkEquals("anchor-mixed-array", "foo", StringUtils.join(MIXED_ARRAY_LIST));
        checkEquals("anchor-mixed-type", "foo2", StringUtils.join(MIXED_TYPE_LIST));
    }

    private static void runSharedEmptyChecks() {
        String empty1 = StringUtils.trimToEmpty(null);
        String empty2 = StringUtils.stripToEmpty(null);
        String empty3 = StringUtils.substring("", 0);
        String empty4 = StringUtils.substring("", 0, 0);
        String empty5 = StringUtils.left("abc", -1);

        if (!"".equals(empty1) || !"".equals(empty2) || !"".equals(empty3) || !"".equals(empty4) || !"".equals(empty5)) {
            throw new RuntimeException("[oracle:empty-shared] metamorphic violation: EMPTY-backed readers disagree input=nulls lhs="
                + safe(empty1) + "," + safe(empty2) + "," + safe(empty3) + "," + safe(empty4) + "," + safe(empty5));
        }
    }

    private static void runExploreChecks(FuzzedDataProvider data) {
        int len = data.consumeInt(1, 6);
        Object[] arr = new Object[len];
        int badIndex = data.consumeInt(0, len - 1);

        for (int i = 0; i < len; i++) {
            if (i == badIndex) {
                arr[i] = NULL_TO_STRING_LIST[0];
            } else {
                int kind = data.consumeInt(0, 4);
                switch (kind) {
                    case 0:
                        arr[i] = null;
                        break;
                    case 1:
                        arr[i] = data.consumeAsciiString(12);
                        break;
                    case 2:
                        arr[i] = Long.valueOf(data.consumeInt(-1000000, 1000000));
                        break;
                    case 3:
                        arr[i] = data.consumeString(12);
                        break;
                    default:
                        arr[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                }
            }
        }

        char sepChar = (char) data.consumeInt(0, 127);
        String sepString = String.valueOf(sepChar);

        final int start = badIndex;
        final int end = badIndex + 1;

        checkRootCauseOrEquals("explore-single-char", "null", true, new JoinCall() {
            @Override
            public String call() {
                return StringUtils.join(arr, sepChar, start, end);
            }
        });

        checkRootCauseOrEquals("explore-single-string", "null", true, new JoinCall() {
            @Override
            public String call() {
                return StringUtils.join(arr, sepString, start, end);
            }
        });

        int rangeStart = data.consumeInt(0, badIndex);
        int rangeEnd = data.consumeInt(badIndex + 1, len);

        String lhs;
        String rhs;
        try {
            lhs = StringUtils.join(arr, sepChar, rangeStart, rangeEnd);
            rhs = StringUtils.join(arr, sepString, rangeStart, rangeEnd);
        } catch (RuntimeException e) {
            if (isCleanRejection(e)) {
                return;
            }
            if (isRootCauseInJoin(e) && isValidByConstruction(arr, rangeStart, rangeEnd, badIndex)) {
                throw e;
            }
            return;
        }

        /*
         * Contract/oracle: the char-separator and String-separator overloads are sibling APIs
         * over the same operation; using String.valueOf(sepChar) must produce the same joined text.
         * A patch that merely suppresses the crash or skips appending the first element would break
         * this agreement or the known one-element result below.
         */
        if (!safeEquals(lhs, rhs)) {
            throw new RuntimeException("[oracle:sep-overload] metamorphic violation: char and String separator overloads disagree input=start="
                + rangeStart + ",end=" + rangeEnd + ",sep=" + (int) sepChar + " lhs=" + safe(lhs) + " rhs=" + safe(rhs));
        }

        try {
            String noSep = StringUtils.join(arr, "", rangeStart, rangeEnd);
            String direct = StringUtils.join(slice(arr, rangeStart, rangeEnd));
            if (!safeEquals(noSep, direct)) {
                throw new RuntimeException("[oracle:no-sep-range] metamorphic violation: range join with empty separator must equal join of sliced range input=start="
                    + rangeStart + ",end=" + rangeEnd + " lhs=" + safe(noSep) + " rhs=" + safe(direct));
            }
        } catch (RuntimeException e) {
            if (isCleanRejection(e)) {
                return;
            }
            if (isRootCauseInJoin(e) && isValidByConstruction(arr, rangeStart, rangeEnd, badIndex)) {
                throw e;
            }
        }
    }

    private static Object[] slice(Object[] arr, int start, int end) {
        Object[] out = new Object[end - start];
        for (int i = 0; i < out.length; i++) {
            out[i] = arr[start + i];
        }
        return out;
    }

    private static void checkEquals(String id, String expected, String actual) {
        if (!safeEquals(expected, actual)) {
            throw new RuntimeException("[oracle:" + id + "] metamorphic violation: expected=" + safe(expected) + " actual=" + safe(actual));
        }
    }

    private static void checkRootCauseOrEquals(String id, String expected, boolean validByConstruction, JoinCall call) {
        try {
            String actual = call.call();
            checkEquals(id, expected, actual);
        } catch (RuntimeException e) {
            if (isCleanRejection(e)) {
                return;
            }
            if (validByConstruction && isRootCauseInJoin(e)) {
                throw e;
            }
            if (e.getMessage() != null && e.getMessage().startsWith("[oracle:")) {
                throw e;
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static boolean isValidByConstruction(Object[] arr, int start, int end, int badIndex) {
        return arr != null && start >= 0 && end >= start && end <= arr.length && start <= badIndex && badIndex < end && arr[badIndex] == NULL_TO_STRING_LIST[0];
    }

    private static boolean isRootCauseInJoin(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement ste = trace[i];
            if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName()) && "join".equals(ste.getMethodName())) {
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

    private static String safe(String s) {
        return s == null ? "null" : s;
    }

    private interface JoinCall {
        String call();
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
<details open><summary>▸ Output (~17,710 chars)</summary>

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
        runAnchorChecks();

        int mode = data.consumeInt(0, 4);
        switch (mode) {
            case 0:
                exploreCharJoin(data);
                break;
            case 1:
                exploreStringJoin(data);
                break;
            case 2:
                exploreBothAndCompare(data);
                break;
            case 3:
                exploreExactPropertyAtRandomPosition(data);
                break;
            default:
                exploreObjectArrayEntryPoints(data);
                break;
        }
    }

    private static void runAnchorChecks() {
        try {
            if (StringUtils.join((Object[]) null, ',') != null) {
                throw new RuntimeException("[oracle:anchor-null-array-char] metamorphic violation: join((Object[])null, ',') must return null");
            }
            if (!TEXT_LIST_CHAR.equals(StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-array-char] metamorphic violation: expected=" + TEXT_LIST_CHAR + " got=" + StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR));
            }
            if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-empty-array-char] metamorphic violation: expected empty");
            }
            if (!";;foo".equals(StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-mixed-array-char] metamorphic violation: expected=';;foo' got=" + StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR));
            }
            if (!"foo;2".equals(StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR))) {
                throw new RuntimeException("[oracle:anchor-mixed-type-char] metamorphic violation: expected='foo;2' got=" + StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR));
            }
            if (!"/".equals(StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1))) {
                throw new RuntimeException("[oracle:anchor-slice-char-1] metamorphic violation: expected='/'");
            }
            if (!"foo".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1))) {
                throw new RuntimeException("[oracle:anchor-slice-char-2] metamorphic violation: expected='foo'");
            }
            expectJoinResultChar(NULL_TO_STRING_LIST, '/', 0, 1, "null");
            if (!"foo/2".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2))) {
                throw new RuntimeException("[oracle:anchor-slice-char-3] metamorphic violation: expected='foo/2'");
            }
            if (!"2".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2))) {
                throw new RuntimeException("[oracle:anchor-slice-char-4] metamorphic violation: expected='2'");
            }
            if (!"".equals(StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1))) {
                throw new RuntimeException("[oracle:anchor-slice-char-5] metamorphic violation: expected empty");
            }

            if (StringUtils.join((Object[]) null) != null) {
                throw new RuntimeException("[oracle:anchor-null-array-varargs] metamorphic violation: join((Object[])null) must return null");
            }
            if (!"".equals(StringUtils.join())) {
                throw new RuntimeException("[oracle:anchor-empty-varargs] metamorphic violation: join() must return empty");
            }
            if (!"".equals(StringUtils.join((Object) null))) {
                throw new RuntimeException("[oracle:anchor-single-null-varargs] metamorphic violation: join((Object)null) must return empty");
            }
            if (!"".equals(StringUtils.join(EMPTY_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-empty-array] metamorphic violation: expected empty");
            }
            if (!"".equals(StringUtils.join(NULL_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-null-array-entry] metamorphic violation: expected empty");
            }
            expectJoinResultDefault(NULL_TO_STRING_LIST, "null");
            if (!"abc".equals(StringUtils.join(new String[] { "a", "b", "c" }))) {
                throw new RuntimeException("[oracle:anchor-abc] metamorphic violation: expected='abc'");
            }
            if (!"a".equals(StringUtils.join(new String[] { null, "a", "" }))) {
                throw new RuntimeException("[oracle:anchor-a] metamorphic violation: expected='a'");
            }
            if (!"foo".equals(StringUtils.join(MIXED_ARRAY_LIST))) {
                throw new RuntimeException("[oracle:anchor-foo] metamorphic violation: expected='foo'");
            }
            if (!"foo2".equals(StringUtils.join(MIXED_TYPE_LIST))) {
                throw new RuntimeException("[oracle:anchor-foo2] metamorphic violation: expected='foo2'");
            }

            expectJoinResultString(NULL_TO_STRING_LIST, "/", 0, 1, "null");

            checkEmptyStateAgreement();
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t)) {
                throw t;
            }
        }
    }

    private static void exploreCharJoin(FuzzedDataProvider data) {
        Object[] array = buildArrayWithPossibleNullToString(data, true);
        if (array.length == 0) {
            array = new Object[] { NULL_TO_STRING };
        }
        int start = data.consumeInt(0, array.length - 1);
        int end = data.consumeInt(start + 1, array.length);
        char sep = (char) data.consumeInt(0, 127);

        try {
            String result = StringUtils.join(array, sep, start, end);
            if (containsNullToStringAt(array, start)) {
                if (!"null".equals(result)) {
                    throw new RuntimeException("[oracle:null-to-string-char] metamorphic violation: documented test support shows a null toString element in the joined slice must contribute text 'null' inputStart=" + start + " inputEnd=" + end + " result=" + result);
                }
            }
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t)) {
                throw t;
            }
        }

        checkEmptyStateAgreement();
    }

    private static void exploreStringJoin(FuzzedDataProvider data) {
        Object[] array = buildArrayWithPossibleNullToString(data, true);
        if (array.length == 0) {
            array = new Object[] { NULL_TO_STRING };
        }
        int start = data.consumeInt(0, array.length - 1);
        int end = data.consumeInt(start + 1, array.length);
        String sep = data.consumeBoolean() ? null : data.consumeAsciiString(4);

        try {
            String result = StringUtils.join(array, sep, start, end);
            if (containsNullToStringAt(array, start)) {
                if (!"null".equals(result)) {
                    throw new RuntimeException("[oracle:null-to-string-string] metamorphic violation: documented test support shows a null toString element in the joined slice must contribute text 'null' inputStart=" + start + " inputEnd=" + end + " sep=" + sep + " result=" + result);
                }
            }
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t)) {
                throw t;
            }
        }

        checkEmptyStateAgreement();
    }

    private static void exploreBothAndCompare(FuzzedDataProvider data) {
        Object[] array = buildArrayWithPossibleNullToString(data, false);
        if (array.length == 0) {
            array = new Object[] { sampleRegularObject(data), sampleRegularObject(data) };
        }
        int start = data.consumeInt(0, array.length - 1);
        int end = data.consumeInt(start + 1, array.length);
        char sep = (char) data.consumeInt(1, 126);
        String lhs;
        String rhs;
        try {
            lhs = StringUtils.join(array, sep, start, end);
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t)) {
                throw t;
            }
            return;
        }
        try {
            rhs = StringUtils.join(array, String.valueOf(sep), start, end);
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t)) {
                throw t;
            }
            return;
        }

        /*
         * Contract asserted: the two public overloads join(Object[], char, int, int)
         * and join(Object[], String, int, int) are documented to join the same slice,
         * differing only in separator representation. For a one-character separator,
         * both sides must produce the same observable string. A throw-deleting or
         * branch-skipping patch could make one overload silently omit/alter content.
         */
        if (!safeEquals(lhs, rhs)) {
            throw new RuntimeException("[oracle:char-vs-string] metamorphic violation: one-character String separator must agree with char separator arrayLen=" + array.length + " start=" + start + " end=" + end + " sep=" + sep + " lhs=" + lhs + " rhs=" + rhs);
        }

        checkEmptyStateAgreement();
    }

    private static void exploreExactPropertyAtRandomPosition(FuzzedDataProvider data) {
        int len = data.consumeInt(1, 8);
        Object[] array = new Object[len];
        int start = data.consumeInt(0, len - 1);
        int end = data.consumeInt(start + 1, len);
        for (int i = 0; i < len; i++) {
            array[i] = sampleRegularObject(data);
        }
        array[start] = NULL_TO_STRING;
        char sep = (char) data.consumeInt(0, 127);
        String sepString = data.consumeBoolean() ? String.valueOf(sep) : data.consumeAsciiString(3);

        try {
            String charResult = StringUtils.join(array, sep, start, end);
            if (!"null".equals(charResult) && end == start + 1) {
                throw new RuntimeException("[oracle:single-null-to-string-char] metamorphic violation: single-element slice with null-returning toString must yield 'null' start=" + start + " end=" + end + " result=" + charResult);
            }
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t)) {
                throw t;
            }
        }

        try {
            String stringResult = StringUtils.join(array, sepString, start, end);
            if (!"null".equals(stringResult) && end == start + 1) {
                throw new RuntimeException("[oracle:single-null-to-string-string] metamorphic violation: single-element slice with null-returning toString must yield 'null' start=" + start + " end=" + end + " sep=" + sepString + " result=" + stringResult);
            }
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t)) {
                throw t;
            }
        }

        checkEmptyStateAgreement();
    }

    private static void exploreObjectArrayEntryPoints(FuzzedDataProvider data) {
        Object[] array = buildArrayWithPossibleNullToString(data, true);
        try {
            StringUtils.join(array);
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t)) {
                throw t;
            }
        }

        try {
            StringUtils.join((Object[]) null);
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t)) {
                throw t;
            }
        }

        try {
            StringUtils.join((Object) null);
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t)) {
                throw t;
            }
        }

        checkEmptyStateAgreement();
    }

    private static void expectJoinResultChar(Object[] array, char sep, int start, int end, String expected) {
        try {
            String actual = StringUtils.join(array, sep, start, end);
            if (!safeEquals(expected, actual)) {
                throw new RuntimeException("[oracle:anchor-char-expected] metamorphic violation: expected=" + expected + " actual=" + actual);
            }
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t)) {
                throw t;
            }
        }
    }

    private static void expectJoinResultString(Object[] array, String sep, int start, int end, String expected) {
        try {
            String actual = StringUtils.join(array, sep, start, end);
            if (!safeEquals(expected, actual)) {
                throw new RuntimeException("[oracle:anchor-string-expected] metamorphic violation: expected=" + expected + " actual=" + actual);
            }
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t)) {
                throw t;
            }
        }
    }

    private static void expectJoinResultDefault(Object[] array, String expected) {
        try {
            String actual = StringUtils.join(array);
            if (!safeEquals(expected, actual)) {
                throw new RuntimeException("[oracle:anchor-default-expected] metamorphic violation: expected=" + expected + " actual=" + actual);
            }
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t)) {
                throw t;
            }
        }
    }

    private static void checkEmptyStateAgreement() {
        try {
            String e1 = StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1);
            String e2 = StringUtils.join(MIXED_TYPE_LIST, "", 2, 1);
            String e3 = StringUtils.trimToEmpty(null);
            String e4 = StringUtils.stripToEmpty(null);
            String e5 = StringUtils.substring("", 0);
            String e6 = StringUtils.substring("", 0, 0);
            String e7 = StringUtils.left("abc", -1);

            /*
             * Contract asserted: all these methods document returning the empty String
             * for the covered inputs. They share StringUtils.EMPTY, so the observable
             * empty value established by one reader must agree with the others.
             * A patch that skips the empty-result path or returns a non-empty value
             * would violate this without throwing.
             */
            if (!"".equals(e1) || !"".equals(e2) || !"".equals(e3) || !"".equals(e4) || !"".equals(e5) || !"".equals(e6) || !"".equals(e7)) {
                throw new RuntimeException("[oracle:empty-state] metamorphic violation: EMPTY-backed readers disagree e1=" + e1 + " e2=" + e2 + " e3=" + e3 + " e4=" + e4 + " e5=" + e5 + " e6=" + e6 + " e7=" + e7);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (shouldPropagateRootCause(t)) {
                throw t;
            }
        }
    }

    private static Object[] buildArrayWithPossibleNullToString(FuzzedDataProvider data, boolean ensureSpecialPresent) {
        int len = data.consumeInt(0, 8);
        Object[] array = new Object[len];
        for (int i = 0; i < len; i++) {
            if (data.consumeBoolean()) {
                array[i] = null;
            } else if (data.consumeBoolean()) {
                array[i] = NULL_TO_STRING;
            } else {
                array[i] = sampleRegularObject(data);
            }
        }
        if (ensureSpecialPresent && len > 0) {
            array[data.consumeInt(0, len - 1)] = NULL_TO_STRING;
        }
        return array;
    }

    private static Object sampleRegularObject(FuzzedDataProvider data) {
        int choice = data.consumeInt(0, 2);
        if (choice == 0) {
            return data.consumeAsciiString(6);
        } else if (choice == 1) {
            return Long.valueOf(data.consumeInt(-1000, 1000));
        } else {
            return data.consumeString(6);
        }
    }

    private static boolean containsNullToStringAt(Object[] array, int idx) {
        return idx >= 0 && idx < array.length && array[idx] == NULL_TO_STRING;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean shouldPropagateRootCause(Throwable t) {
        if (t instanceof RuntimeException && ((RuntimeException) t).getMessage() != null
                && ((RuntimeException) t).getMessage().startsWith("[oracle:")) {
            return true;
        }
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName())
                    && "join".equals(ste.getMethodName())) {
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
<details open><summary>▸ Output (~12,918 chars)</summary>

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

    private static final class SkipCheck extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchors();
        runSharedEmptyChecks();

        try {
            runExplore(data);
        } catch (SkipCheck ignored) {
            return;
        }
    }

    private static void runAnchors() {
        if (StringUtils.join((Object[]) null, ',') != null) {
            throw new RuntimeException("[oracle:anchor-null-array-char] metamorphic violation: expected null for null array");
        }
        if (StringUtils.join((Object[]) null) != null) {
            throw new RuntimeException("[oracle:anchor-null-array-varargs] metamorphic violation: expected null for null object array");
        }
        String joinObjectNull = StringUtils.join((Object) null);
        if (!"".equals(joinObjectNull)) {
            throw new RuntimeException("[oracle:anchor-object-null] metamorphic violation: input=(Object)null lhs=" + joinObjectNull + " rhs=");
        }
        String textList = StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR);
        if (!TEXT_LIST_CHAR.equals(textList)) {
            throw new RuntimeException("[oracle:anchor-array-list] metamorphic violation: input=ARRAY_LIST lhs=" + textList + " rhs=" + TEXT_LIST_CHAR);
        }
        String empty = StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR);
        if (!"".equals(empty)) {
            throw new RuntimeException("[oracle:anchor-empty-array] metamorphic violation: input=EMPTY_ARRAY_LIST lhs=" + empty + " rhs=");
        }

        String anchoredChar = joinCharChecked(NULL_TO_STRING_LIST, '/', 0, 1, true);
        if (!"null".equals(anchoredChar)) {
            throw new RuntimeException("[oracle:anchor-null-toString-char] metamorphic violation: input=NULL_TO_STRING_LIST lhs=" + anchoredChar + " rhs=null");
        }

        String anchoredString = joinStringChecked(NULL_TO_STRING_LIST, "", 0, 1, true);
        if (!"null".equals(anchoredString)) {
            throw new RuntimeException("[oracle:anchor-null-toString-string] metamorphic violation: input=NULL_TO_STRING_LIST lhs=" + anchoredString + " rhs=null");
        }

        String anchoredVarargs = joinVarargsChecked(NULL_TO_STRING_LIST, true);
        if (!"null".equals(anchoredVarargs)) {
            throw new RuntimeException("[oracle:anchor-null-toString-varargs] metamorphic violation: input=NULL_TO_STRING_LIST lhs=" + anchoredVarargs + " rhs=null");
        }
    }

    private static void runSharedEmptyChecks() {
        String emptyFromJoinChar = joinCharChecked(MIXED_TYPE_LIST, '/', 2, 1, true);
        String emptyFromJoinString = joinStringChecked(MIXED_TYPE_LIST, "", 2, 1, true);
        String emptyFromTrim = StringUtils.trimToEmpty(null);
        String emptyFromStrip = StringUtils.stripToEmpty(null);
        String emptyFromSubstring1 = StringUtils.substring("", 1);
        String emptyFromSubstring2 = StringUtils.substring("", 0, 0);
        String emptyFromLeft = StringUtils.left("abc", -1);

        if (!emptyFromJoinChar.equals(emptyFromTrim)
                || !emptyFromJoinChar.equals(emptyFromStrip)
                || !emptyFromJoinChar.equals(emptyFromSubstring1)
                || !emptyFromJoinChar.equals(emptyFromSubstring2)
                || !emptyFromJoinChar.equals(emptyFromLeft)
                || !emptyFromJoinChar.equals(emptyFromJoinString)) {
            throw new RuntimeException("[oracle:shared-empty] metamorphic violation: EMPTY readers disagree"
                    + " joinChar=" + printable(emptyFromJoinChar)
                    + " joinString=" + printable(emptyFromJoinString)
                    + " trim=" + printable(emptyFromTrim)
                    + " strip=" + printable(emptyFromStrip)
                    + " substring1=" + printable(emptyFromSubstring1)
                    + " substring2=" + printable(emptyFromSubstring2)
                    + " left=" + printable(emptyFromLeft));
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        int len = data.consumeInt(1, 6);
        Object[] array = new Object[len];
        Object[] oracleArray = new Object[len];

        int specialIndex = data.consumeInt(0, len - 1);
        for (int i = 0; i < len; i++) {
            if (i == specialIndex) {
                array[i] = NULL_TO_STRING;
                oracleArray[i] = "null";
                continue;
            }
            int kind = data.consumeInt(0, 4);
            switch (kind) {
                case 0:
                    array[i] = null;
                    oracleArray[i] = null;
                    break;
                case 1:
                    String s1 = data.consumeAsciiString(16);
                    array[i] = s1;
                    oracleArray[i] = s1;
                    break;
                case 2:
                    String s2 = data.consumeString(16);
                    array[i] = s2;
                    oracleArray[i] = s2;
                    break;
                case 3:
                    Long v = Long.valueOf(data.consumeInt(-1000, 1000));
                    array[i] = v;
                    oracleArray[i] = v;
                    break;
                default:
                    String s3 = "";
                    array[i] = s3;
                    oracleArray[i] = s3;
                    break;
            }
        }

        int startIndex = data.consumeInt(0, len - 1);
        int endIndex = data.consumeInt(startIndex + 1, len);

        if (data.consumeBoolean()) {
            startIndex = specialIndex;
            endIndex = data.consumeInt(startIndex + 1, len);
        } else if (specialIndex < startIndex || specialIndex >= endIndex) {
            startIndex = specialIndex;
            endIndex = Math.min(len, startIndex + 1 + data.consumeInt(0, len - startIndex - 1));
        }

        char sepChar = (char) data.consumeByte();
        String sepString = data.consumeBoolean() ? String.valueOf(sepChar) : data.consumeAsciiString(4);
        if (sepString.length() == 1) {
            sepChar = sepString.charAt(0);
        } else {
            sepString = String.valueOf(sepChar);
        }

        String actualChar = joinCharChecked(array, sepChar, startIndex, endIndex, true);
        String expectedChar = joinCharChecked(oracleArray, sepChar, startIndex, endIndex, true);

        /* Contract asserted: regression tests require that an element whose toString() returns null
           contributes the literal "null" to join output ("assertEquals(\"null\", StringUtils.join(NULL_TO_STRING_LIST,...))").
           Therefore replacing that real element with the literal String "null" must not change the result.
           A throw-deleting or element-skipping patch would violate this observable equality without throwing. */
        if (!actualChar.equals(expectedChar)) {
            throw new RuntimeException("[oracle:null-toString-char] metamorphic violation: replacing null-toString element with literal \"null\" changed char-join result"
                    + " start=" + startIndex
                    + " end=" + endIndex
                    + " sep=" + (int) sepChar
                    + " lhs=" + printable(actualChar)
                    + " rhs=" + printable(expectedChar));
        }

        String actualString = joinStringChecked(array, sepString, startIndex, endIndex, true);
        String expectedString = joinStringChecked(oracleArray, sepString, startIndex, endIndex, true);
        if (!actualString.equals(expectedString)) {
            throw new RuntimeException("[oracle:null-toString-string] metamorphic violation: replacing null-toString element with literal \"null\" changed string-join result"
                    + " start=" + startIndex
                    + " end=" + endIndex
                    + " sep=" + printable(sepString)
                    + " lhs=" + printable(actualString)
                    + " rhs=" + printable(expectedString));
        }

        if (sepString.length() == 1) {
            /* Contract asserted: join(Object[], char, start, end) and join(Object[], String, start, end)
               document the same joining behavior for equivalent one-character separators, so they must agree. */
            if (!actualChar.equals(actualString)) {
                throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: char and String separator overloads disagree"
                        + " start=" + startIndex
                        + " end=" + endIndex
                        + " sepChar=" + (int) sepChar
                        + " lhs=" + printable(actualChar)
                        + " rhs=" + printable(actualString));
            }
        }

        if (startIndex == 0 && endIndex == array.length) {
            String varargs = joinVarargsChecked(array, true);
            String stringNoSep = joinStringChecked(array, "", 0, array.length, true);
            /* Contract asserted: join(Object[]) is the no-separator public API for the same whole-array operation,
               so it must match join(Object[], "", 0, array.length) on every accepted input. */
            if (!varargs.equals(stringNoSep)) {
                throw new RuntimeException("[oracle:varargs-agreement] metamorphic violation: join(Object[]) disagrees with join(array,\"\",0,len)"
                        + " lhs=" + printable(varargs)
                        + " rhs=" + printable(stringNoSep));
            }
        }
    }

    private static String joinCharChecked(Object[] array, char separator, int startIndex, int endIndex, boolean validByConstruction) {
        try {
            return StringUtils.join(array, separator, startIndex, endIndex);
        } catch (Throwable t) {
            handleThrowable(t, validByConstruction);
            throw new SkipCheck();
        }
    }

    private static String joinStringChecked(Object[] array, String separator, int startIndex, int endIndex, boolean validByConstruction) {
        try {
            return StringUtils.join(array, separator, startIndex, endIndex);
        } catch (Throwable t) {
            handleThrowable(t, validByConstruction);
            throw new SkipCheck();
        }
    }

    private static String joinVarargsChecked(Object[] array, boolean validByConstruction) {
        try {
            return StringUtils.join(array);
        } catch (Throwable t) {
            handleThrowable(t, validByConstruction);
            throw new SkipCheck();
        }
    }

    private static void handleThrowable(Throwable t, boolean validByConstruction) {
        if (t instanceof RuntimeException && isOracleFailure((RuntimeException) t)) {
            throw (RuntimeException) t;
        }
        if (isCleanRejection(t)) {
            throw new SkipCheck();
        }
        if (validByConstruction && isRootCause(t)) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            throw new RuntimeException(t);
        }
        throw new SkipCheck();
    }

    private static boolean isOracleFailure(RuntimeException t) {
        String msg = t.getMessage();
        return msg != null && msg.startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement ste = stack[i];
            if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName())
                    && "join".equals(ste.getMethodName())) {
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
<details open><summary>▸ Output (~11,204 chars)</summary>

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
        runAnchors();
        runExplore(data);
        runMetamorphicChecks(data);
        runEmptyStateAgreementChecks(data);
    }

    private static void runAnchors() {
        if (StringUtils.join((Object[]) null, ',') != null) {
            throw new RuntimeException("[oracle:anchor-null-array-char] metamorphic violation: expected null for null array");
        }
        if (StringUtils.join((Object[]) null) != null) {
            throw new RuntimeException("[oracle:anchor-null-array-varargs] metamorphic violation: expected null for null array");
        }

        String objectNullJoin = StringUtils.join((Object) null);
        if (!"".equals(objectNullJoin)) {
            throw new RuntimeException("[oracle:anchor-object-null] metamorphic violation: input=(Object)null lhs=" + objectNullJoin + " rhs=");
        }

        String emptyJoin = StringUtils.join();
        if (!"".equals(emptyJoin)) {
            throw new RuntimeException("[oracle:anchor-empty-varargs] metamorphic violation: input=empty lhs=" + emptyJoin + " rhs=");
        }

        String emptyArrayJoin = StringUtils.join(EMPTY_ARRAY_LIST);
        if (!"".equals(emptyArrayJoin)) {
            throw new RuntimeException("[oracle:anchor-empty-array] metamorphic violation: input=EMPTY_ARRAY_LIST lhs=" + emptyArrayJoin + " rhs=");
        }

        String nullArrayListJoin = StringUtils.join(NULL_ARRAY_LIST);
        if (!"".equals(nullArrayListJoin)) {
            throw new RuntimeException("[oracle:anchor-null-array-element] metamorphic violation: input=NULL_ARRAY_LIST lhs=" + nullArrayListJoin + " rhs=");
        }

        String mixedArrayJoin = StringUtils.join(MIXED_ARRAY_LIST);
        if (!"foo".equals(mixedArrayJoin)) {
            throw new RuntimeException("[oracle:anchor-mixed-array] metamorphic violation: input=MIXED_ARRAY_LIST lhs=" + mixedArrayJoin + " rhs=foo");
        }

        String mixedTypeJoin = StringUtils.join(MIXED_TYPE_LIST);
        if (!"foo2".equals(mixedTypeJoin)) {
            throw new RuntimeException("[oracle:anchor-mixed-type] metamorphic violation: input=MIXED_TYPE_LIST lhs=" + mixedTypeJoin + " rhs=foo2");
        }

        String arrayListJoin = StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR);
        if (!TEXT_LIST_CHAR.equals(arrayListJoin)) {
            throw new RuntimeException("[oracle:anchor-array-char] metamorphic violation: input=ARRAY_LIST lhs=" + arrayListJoin + " rhs=" + TEXT_LIST_CHAR);
        }

        String mixedArraySlice = StringUtils.join(MIXED_ARRAY_LIST, '/', 0, MIXED_ARRAY_LIST.length - 1);
        if (!"/".equals(mixedArraySlice)) {
            throw new RuntimeException("[oracle:anchor-mixed-slice] metamorphic violation: input=MIXED_ARRAY_LIST lhs=" + mixedArraySlice + " rhs=/");
        }

        String mixedTypeSlice0 = StringUtils.join(MIXED_TYPE_LIST, '/', 0, 1);
        if (!"foo".equals(mixedTypeSlice0)) {
            throw new RuntimeException("[oracle:anchor-mixed-type-slice0] metamorphic violation: input=MIXED_TYPE_LIST lhs=" + mixedTypeSlice0 + " rhs=foo");
        }

        String mixedTypeSlice1 = StringUtils.join(MIXED_TYPE_LIST, '/', 0, 2);
        if (!"foo/2".equals(mixedTypeSlice1)) {
            throw new RuntimeException("[oracle:anchor-mixed-type-slice1] metamorphic violation: input=MIXED_TYPE_LIST lhs=" + mixedTypeSlice1 + " rhs=foo/2");
        }

        String mixedTypeSlice2 = StringUtils.join(MIXED_TYPE_LIST, '/', 1, 2);
        if (!"2".equals(mixedTypeSlice2)) {
            throw new RuntimeException("[oracle:anchor-mixed-type-slice2] metamorphic violation: input=MIXED_TYPE_LIST lhs=" + mixedTypeSlice2 + " rhs=2");
        }

        String mixedTypeSliceEmpty = StringUtils.join(MIXED_TYPE_LIST, '/', 2, 1);
        if (!"".equals(mixedTypeSliceEmpty)) {
            throw new RuntimeException("[oracle:anchor-mixed-type-slice-empty] metamorphic violation: input=MIXED_TYPE_LIST lhs=" + mixedTypeSliceEmpty + " rhs=");
        }

        try {
            String s = StringUtils.join(NULL_TO_STRING_LIST, '/', 0, 1);
            if (!"null".equals(s)) {
                throw new RuntimeException("[oracle:anchor-null-to-string-char] metamorphic violation: input=NULL_TO_STRING_LIST lhs=" + s + " rhs=null");
            }
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t)) {
                throw t;
            }
        }

        try {
            String s = StringUtils.join(NULL_TO_STRING_LIST);
            if (!"null".equals(s)) {
                throw new RuntimeException("[oracle:anchor-null-to-string-varargs] metamorphic violation: input=NULL_TO_STRING_LIST lhs=" + s + " rhs=null");
            }
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t)) {
                throw t;
            }
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        int len = data.consumeInt(1, 8);
        Object[] array = new Object[len];
        int triggerIndex = data.consumeInt(0, len - 1);
        for (int i = 0; i < len; i++) {
            if (i == triggerIndex) {
                array[i] = NULL_TO_STRING;
            } else if (data.consumeBoolean()) {
                array[i] = null;
            } else if (data.consumeBoolean()) {
                array[i] = data.consumeAsciiString(12);
            } else {
                array[i] = Long.valueOf(data.consumeInt(-1000, 1000));
            }
        }

        int startIndex = triggerIndex;
        int endIndex = data.consumeInt(triggerIndex + 1, len);
        char charSep = (char) (data.consumeByte() & 0xff);
        String stringSep = data.consumeBoolean() ? null : data.consumeString(8);

        try {
            StringUtils.join(array, charSep, startIndex, endIndex);
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t)) {
                throw t;
            }
        }

        try {
            StringUtils.join(array, stringSep, startIndex, endIndex);
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t)) {
                throw t;
            }
        }

        try {
            StringUtils.join(new Object[] { NULL_TO_STRING });
        } catch (RuntimeException t) {
            if (shouldPropagateRootCause(t)) {
                throw t;
            }
        }
    }

    private static void runMetamorphicChecks(FuzzedDataProvider data) {
        int len = data.consumeInt(1, 6);
        Object[] safe = new Object[len];
        for (int i = 0; i < len; i++) {
            if (data.consumeBoolean()) {
                safe[i] = data.consumeAsciiString(10);
            } else {
                safe[i] = Long.valueOf(data.consumeInt(-1000, 1000));
            }
        }
        int start = data.consumeInt(0, len - 1);
        int end = data.consumeInt(start + 1, len);
        char c = (char) (data.consumeByte() & 0xff);

        /* Contract asserted:
         * join(Object[] array, String separator, ...) treats null separator as EMPTY.
         * A throw-deleting or skip-append patch could make the call stop crashing but silently
         * differ from the documented EMPTY-separator behavior.
         */
        try {
            String lhs = StringUtils.join(safe, null, start, end);
            String rhs = StringUtils.join(safe, "", start, end);
            if (!eq(lhs, rhs)) {
                throw new RuntimeException("[oracle:null-sep-equals-empty] metamorphic violation: join(array,null,start,end)==join(array,\"\",start,end) inputStart=" + start + " inputEnd=" + end + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Throwable ignored) {
        }

        /* Contract asserted:
         * The char-separator and String-separator overloads operate over the same slice and should
         * agree when given equivalent separators.
         * A patch that only suppresses the exception but changes concatenation logic would break this.
         */
        try {
            String lhs = StringUtils.join(safe, c, start, end);
            String rhs = StringUtils.join(safe, String.valueOf(c), start, end);
            if (!eq(lhs, rhs)) {
                throw new RuntimeException("[oracle:char-string-overload] metamorphic violation: join(array,char,start,end)==join(array,String.valueOf(char),start,end) inputStart=" + start + " inputEnd=" + end + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void runEmptyStateAgreementChecks(FuzzedDataProvider data) {
        String fuzz = data.consumeString(16);

        /* Shared-state agreement over StringUtils.EMPTY:
         * docs for trimToEmpty(null), stripToEmpty(null), substring("",0), and left(x,-ve)
         * all guarantee an empty String result. If a patch corrupted the shared EMPTY state,
         * these readers would disagree even if join's visible output looked plausible.
         */
        String e1 = StringUtils.trimToEmpty(null);
        String e2 = StringUtils.stripToEmpty(null);
        String e3 = StringUtils.substring("", 0);
        String e4 = StringUtils.left(fuzz, -1);
        if (!(eq(e1, e2) && eq(e2, e3) && eq(e3, e4) && "".equals(e1))) {
            throw new RuntimeException("[oracle:empty-state-agreement] metamorphic violation: EMPTY readers disagree lhs=" + e1 + " rhs=" + e2 + " e3=" + e3 + " e4=" + e4);
        }
    }

    private static boolean shouldPropagateRootCause(Throwable t) {
        if (t == null) {
            return false;
        }
        if (isCleanRejection(t)) {
            return false;
        }
        return t instanceof NullPointerException && hasJoinFrame(t);
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean hasJoinFrame(Throwable t) {
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.lang3.StringUtils".equals(ste.getClassName())
                    && "join".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean eq(String a, String b) {
        return a == null ? b == null : a.equals(b);
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
**output:** **28/28 dump(s) written under /datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/cov; 28 raw fuzz log(s)**
<details><summary>▸ detail (~14,004 chars)</summary>

- detail: {'dumps': [{'harness': 'attempt_001', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_001_compiled.exec', 'exists': True, 'bytes': 391}, {'harness': 'attempt_002', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_002_compiled.exec', 'exists': True, 'bytes': 384}, {'harness': 'attempt_003', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_003_compiled.exec', 'exists': True, 'bytes': 386}, {'harness': 'attempt_004', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_004_compiled.exec', 'exists': True, 'bytes': 382}, {'harness': 'attempt_005', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_005_compiled.exec', 'exists': True, 'bytes': 387}, {'harness': 'attempt_006', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_006_compiled.exec', 'exists': True, 'bytes': 379}, {'harness': 'attempt_007', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_007_compiled.exec', 'exists': True, 'bytes': 380}, {'harness': 'attempt_008', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_008_compiled.exec', 'exists': True, 'bytes': 380}, {'harness': 'attempt_009', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_009_compiled.exec', 'exists': True, 'bytes': 375}, {'harness': 'attempt_010', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_010_compiled.exec', 'exists': True, 'bytes': 389}, {'harness': 'attempt_011', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_011_compiled.exec', 'exists': True, 'bytes': 382}, {'harness': 'attempt_012', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_012_compiled.exec', 'exists': True, 'bytes': 380}, {'harness': 'attempt_013', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_013_compiled.exec', 'exists': True, 'bytes': 373}, {'harness': 'attempt_014', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_014_compiled.exec', 'exists': True, 'bytes': 374}, {'harness': 'attempt_015', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_015_compiled.exec', 'exists': True, 'bytes': 379}, {'harness': 'attempt_017', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_017_compiled.exec', 'exists': True, 'bytes': 383}, {'harness': 'attempt_018', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_018_compiled.exec', 'exists': True, 'bytes': 386}, {'harness': 'attempt_020', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_020_compiled.exec', 'exists': True, 'bytes': 380}, {'harness': 'attempt_021', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_021_compiled.exec', 'exists': True, 'bytes': 390}, {'harness': 'attempt_022', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_022_compiled.exec', 'exists': True, 'bytes': 401}, {'harness': 'attempt_023', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_023_compiled.exec', 'exists': True, 'bytes': 389}, {'harness': 'attempt_024', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_024_compiled.exec', 'exists': True, 'bytes': 382}, {'harness': 'attempt_025', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_025_compiled.exec', 'exists': True, 'bytes': 387}, {'harness': 'attempt_026', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_026_compiled.exec', 'exists': True, 'bytes': 379}, {'harness': 'attempt_027', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_027_compiled.exec', 'exists': True, 'bytes': 431}, {'harness': 'attempt_028', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_028_compiled.exec', 'exists': True, 'bytes': 392}, {'harness': 'attempt_029', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_029_compiled.exec', 'exists': True, 'bytes': 382}, {'harness': 'attempt_030', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_030_compiled.exec', 'exists': True, 'bytes': 381}], 'outputs': ['/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_001_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_002_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_003_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_004_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_005_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_006_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_007_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_008_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_009_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_010_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_011_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_012_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_013_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_014_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_015_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_017_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_018_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_020_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_021_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_022_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_023_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_024_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_025_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_026_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_027_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_028_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_029_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_030_compiled.txt'], 'classpath': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/cov/classpath.json', 'dir': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/cov', 'compiled_attempts': ['attempt_001', 'attempt_002', 'attempt_003', 'attempt_004', 'attempt_005', 'attempt_006', 'attempt_007', 'attempt_008', 'attempt_009', 'attempt_010', 'attempt_011', 'attempt_012', 'attempt_013', 'attempt_014', 'attempt_015', 'attempt_017', 'attempt_018', 'attempt_020', 'attempt_021', 'attempt_022', 'attempt_023', 'attempt_024', 'attempt_025', 'attempt_026', 'attempt_027', 'attempt_028', 'attempt_029', 'attempt_030'], 'accepted_attempts': ['attempt_001', 'attempt_002', 'attempt_003', 'attempt_004', 'attempt_005', 'attempt_006', 'attempt_007', 'attempt_008', 'attempt_009', 'attempt_010', 'attempt_011', 'attempt_012', 'attempt_013', 'attempt_014', 'attempt_015', 'attempt_017', 'attempt_018', 'attempt_020', 'attempt_021', 'attempt_022', 'attempt_023', 'attempt_024', 'attempt_025', 'attempt_026', 'attempt_027', 'attempt_028', 'attempt_029', 'attempt_030'], 'sources': ['/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_001.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_002.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_003.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_004.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_005.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_006.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_007.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_008.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_009.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_010.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_011.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_012.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_013.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_014.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_015.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_017.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_018.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_020.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_021.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_022.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_023.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_024.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_025.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_026.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_027.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_028.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_029.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_030.java']}

</details>
