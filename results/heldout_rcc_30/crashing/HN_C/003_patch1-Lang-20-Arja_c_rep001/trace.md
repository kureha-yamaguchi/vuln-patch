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
<details><summary>▸ output — FailureTest(test_class='org.apache.commons.lang3.StringUtilsTest', test_method='testJoin_ArrayChar', (~2,746 chars)</summary>

**output:** 
```json
[
  "FailureTest(test_class='org.apache.commons.lang3.StringUtilsTest', test_method='testJoin_ArrayChar', source_path='/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/Lang_20_buggy/src/test/java/org/apache/commons/lang3/StringUtilsTest.java', method_source='    public void testJoin_ArrayChar() {\\n        assertEquals(null, StringUtils.join((Object[]) null, \\',\\'));\\n        assertEquals(TEXT_LIST_CHAR, StringUtils.join(ARRAY_LIST, SEPARATOR_CHAR));\\n        assertEquals(\"\", StringUtils.join(EMPTY_ARRAY_LIST, SEPARATOR_CHAR));\\n        assertEquals(\";;foo\", StringUtils.join(MIXED_ARRAY_LIST, SEPARATOR_CHAR));\\n        assertEquals(\"foo;2\", StringUtils.join(MIXED_TYPE_LIST, SEPARATOR_CHAR));\\n\\n        assertEquals(\"/\", StringUtils.join(MIXED_ARRAY_LIST, \\'/\\', 0, MIXED_ARRAY_LIST.length-1));\\n        assertEquals(\"foo\", StringUtils.join(MIXED_TYPE_LIST, \\'/\\', 0, 1));\\n        assertEquals(\"null\", StringUtils.join(NULL_TO_STRING_LIST,\\'/\\', 0, 1));\\n        assertEquals(\"foo/2\", StringUtils.join(MIXED_TYPE_LIST, \\'/\\', 0, 2));\\n        assertEquals(\"2\", StringUtils.join(MIXED_TYPE_LIST, \\'/\\', 1, 2));\\n        assertEquals(\"\", StringUtils.join(MIXED_TYPE_LIST, \\'/\\', 2, 1));\\n    }', exception_type='java.lang.NullPointerException', failure_message=None, support_source=None)",
  "FailureTest(test_class='org.apache.commons.lang3.StringUtilsTest', test_method='testJoin_Objectarray', source_path='/datadrive/vuln-patch/results/heldout_rcc_30/checkouts/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/Lang_20_buggy/src/test/java/org/apache/commons/lang3/StringUtilsTest.java', method_source='    public void testJoin_Objectarray() {\\n//        assertEquals(null, StringUtils.join(null)); // generates warning\\n        assertEquals(null, StringUtils.join((Object[]) null)); // equivalent explicit cast\\n        // test additional varargs calls\\n        assertEquals(\"\", StringUtils.join()); // empty array\\n        assertEquals(\"\", StringUtils.join((Object) null)); // => new Object[]{null}\\n\\n        assertEquals(\"\", StringUtils.join(EMPTY_ARRAY_LIST));\\n        assertEquals(\"\", StringUtils.join(NULL_ARRAY_LIST));\\n        assertEquals(\"null\", StringUtils.join(NULL_TO_STRING_LIST));\\n        assertEquals(\"abc\", StringUtils.join(new String[] {\"a\", \"b\", \"c\"}));\\n        assertEquals(\"a\", StringUtils.join(new String[] {null, \"a\", \"\"}));\\n        assertEquals(\"foo\", StringUtils.join(MIXED_ARRAY_LIST));\\n        assertEquals(\"foo2\", StringUtils.join(MIXED_TYPE_LIST));\\n    }', exception_type='java.lang.NullPointerException', failure_message=None, support_source=None)"
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
<details><summary>▸ Prompt (2 message(s), ~4,740 chars)</summary>

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

Codebase: `Lang_20_buggy`. Write a fuzz harness for the function(s) shown below.

Call them through the library's real public API, with inputs built from the FuzzedDataProvider, and exercise as much of each function's behaviour as you can: vary lengths, sizes, signs and contents, cover empty and boundary values, and try the argument combinations the signature allows. Construct whatever real library objects the call needs. A throwable that escapes fuzzerTestOneInput is reported as a finding, so let exceptions from the code under test propagate.

Function `join` — declared in `org.apache.commons.lang3.StringUtils`:
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

Function `join` — declared in `org.apache.commons.lang3.StringUtils`:
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
<details open><summary>▸ Output (~4,254 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array = null;
        if (data.consumeBoolean()) {
            int len = data.consumeInt(0, 32);
            array = new Object[len];
            for (int i = 0; i < len; i++) {
                switch (data.consumeInt(0, 7)) {
                    case 0:
                        array[i] = null;
                        break;
                    case 1:
                        array[i] = data.consumeString(32);
                        break;
                    case 2:
                        array[i] = data.consumeAsciiString(32);
                        break;
                    case 3:
                        array[i] = Integer.valueOf(data.consumeInt());
                        break;
                    case 4:
                        array[i] = Character.valueOf((char) (data.consumeByte() & 0xFF));
                        break;
                    case 5:
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                    case 6:
                        array[i] = new StringBuilder(data.consumeString(32));
                        break;
                    default:
                        array[i] = data.consumeBytes(16);
                        break;
                }
            }
        }

        char charSeparator = (char) (data.consumeByte() & 0xFF);
        String stringSeparator;
        switch (data.consumeInt(0, 3)) {
            case 0:
                stringSeparator = null;
                break;
            case 1:
                stringSeparator = "";
                break;
            case 2:
                stringSeparator = data.consumeAsciiString(16);
                break;
            default:
                stringSeparator = data.consumeString(16);
                break;
        }

        int startIndexA;
        int endIndexA;
        int startIndexB;
        int endIndexB;

        if (array == null) {
            startIndexA = data.consumeInt();
            endIndexA = data.consumeInt();
            startIndexB = data.consumeInt();
            endIndexB = data.consumeInt();
        } else {
            int len = array.length;
            if (data.consumeBoolean()) {
                startIndexA = data.consumeInt(-len - 2, len + 2);
                endIndexA = data.consumeInt(-len - 2, len + 2);
            } else {
                startIndexA = data.consumeInt();
                endIndexA = data.consumeInt();
            }
            if (data.consumeBoolean()) {
                startIndexB = data.consumeInt(-len - 2, len + 2);
                endIndexB = data.consumeInt(-len - 2, len + 2);
            } else {
                startIndexB = data.consumeInt();
                endIndexB = data.consumeInt();
            }
        }

        StringUtils.join(array, charSeparator, startIndexA, endIndexA);
        StringUtils.join(array, stringSeparator, startIndexB, endIndexB);

        if (array != null) {
            StringUtils.join(array, charSeparator, 0, array.length);
            StringUtils.join(array, stringSeparator, 0, array.length);

            int len = array.length;
            if (len == 0) {
                StringUtils.join(array, charSeparator, 0, 0);
                StringUtils.join(array, stringSeparator, 0, 0);
            } else {
                StringUtils.join(array, charSeparator, 0, 1);
                StringUtils.join(array, stringSeparator, 0, 1);
                StringUtils.join(array, charSeparator, len - 1, len);
                StringUtils.join(array, stringSeparator, len - 1, len);
                StringUtils.join(array, charSeparator, len, len);
                StringUtils.join(array, stringSeparator, len, len);
                StringUtils.join(array, charSeparator, 1, 0);
                StringUtils.join(array, stringSeparator, 1, 0);
            }
        } else {
            StringUtils.join((Object[]) null, charSeparator, 0, 0);
            StringUtils.join((Object[]) null, stringSeparator, 0, 0);
        }
    }
}
```

</details>

---
## [4] ⚙️ harness-attempt · `attempt_001`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException: -2

---
## [5] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,162 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array;
        if (data.consumeBoolean()) {
            array = null;
        } else {
            int size = data.consumeInt(0, 16);
            array = new Object[size];
            for (int i = 0; i < size; i++) {
                switch (data.consumeInt(0, 8)) {
                    case 0:
                        array[i] = null;
                        break;
                    case 1:
                        array[i] = data.consumeString(32);
                        break;
                    case 2:
                        array[i] = data.consumeAsciiString(32);
                        break;
                    case 3:
                        array[i] = Integer.valueOf(data.consumeInt());
                        break;
                    case 4:
                        array[i] = Long.valueOf(data.consumeInt());
                        break;
                    case 5:
                        array[i] = Character.valueOf((char) (data.consumeByte() & 0xFF));
                        break;
                    case 6:
                        array[i] = new StringBuilder(data.consumeString(32));
                        break;
                    case 7:
                        array[i] = data.consumeBytes(16);
                        break;
                    default:
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                }
            }
        }

        char charSeparator = (char) (data.consumeByte() & 0xFF);
        String stringSeparator;
        if (data.consumeBoolean()) {
            stringSeparator = null;
        } else if (data.consumeBoolean()) {
            stringSeparator = data.consumeString(16);
        } else {
            stringSeparator = data.consumeAsciiString(16);
        }

        if (array != null) {
            int len = array.length;

            int validStart = data.consumeInt(0, len);
            int validEnd = data.consumeInt(validStart, len);
            StringUtils.join(array, charSeparator, validStart, validEnd);
            StringUtils.join(array, stringSeparator, validStart, validEnd);

            int nearStart = data.consumeInt(-2, len + 2);
            int nearEnd = data.consumeInt(-2, len + 2);
            StringUtils.join(array, charSeparator, nearStart, nearEnd);
            StringUtils.join(array, stringSeparator, nearStart, nearEnd);

            StringUtils.join(array, charSeparator, 0, 0);
            StringUtils.join(array, stringSeparator, len, len);
            if (len > 0) {
                StringUtils.join(array, charSeparator, 0, len);
                StringUtils.join(array, stringSeparator, 0, len);
            }
        }

        int anyStart = data.consumeInt();
        int anyEnd = data.consumeInt();
        StringUtils.join(array, charSeparator, anyStart, anyEnd);
        StringUtils.join(array, stringSeparator, anyStart, anyEnd);
    }
}
```

</details>

---
## [6] ⚙️ harness-attempt · `attempt_002`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException: -57300

---
## [7] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,878 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array = null;
        if (data.consumeBoolean()) {
            int len = data.consumeInt(0, 32);
            array = new Object[len];
            for (int i = 0; i < len; i++) {
                switch (data.consumeInt(0, 8)) {
                    case 0:
                        array[i] = null;
                        break;
                    case 1:
                        array[i] = data.consumeString(32);
                        break;
                    case 2:
                        array[i] = data.consumeAsciiString(32);
                        break;
                    case 3:
                        array[i] = Integer.valueOf(data.consumeInt());
                        break;
                    case 4:
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                    case 5:
                        array[i] = Character.valueOf((char) (data.consumeByte() & 0xff));
                        break;
                    case 6:
                        array[i] = new StringBuilder(data.consumeString(32));
                        break;
                    case 7:
                        array[i] = data.consumeBytes(16);
                        break;
                    default:
                        array[i] = data.consumeRemainingAsString();
                        break;
                }
            }
        }

        char charSeparator = (char) (data.consumeByte() & 0xff);

        String stringSeparator;
        switch (data.consumeInt(0, 3)) {
            case 0:
                stringSeparator = null;
                break;
            case 1:
                stringSeparator = "";
                break;
            case 2:
                stringSeparator = data.consumeAsciiString(16);
                break;
            default:
                stringSeparator = data.consumeString(16);
                break;
        }

        int start1 = data.consumeInt();
        int end1 = data.consumeInt();
        StringUtils.join(array, charSeparator, start1, end1);

        int start2 = data.consumeInt();
        int end2 = data.consumeInt();
        StringUtils.join(array, stringSeparator, start2, end2);

        if (array != null) {
            int len = array.length;

            int validStart = len == 0 ? 0 : data.consumeInt(0, len);
            int validEnd = len == 0 ? 0 : data.consumeInt(validStart, len);
            StringUtils.join(array, charSeparator, validStart, validEnd);

            String sep2;
            switch (data.consumeInt(0, 2)) {
                case 0:
                    sep2 = null;
                    break;
                case 1:
                    sep2 = "";
                    break;
                default:
                    sep2 = data.consumeString(8);
                    break;
            }
            StringUtils.join(array, sep2, validStart, validEnd);

            if (len > 0) {
                StringUtils.join(array, charSeparator, 0, len);
                StringUtils.join(array, stringSeparator, 0, len);

                int boundaryStart = data.consumeBoolean() ? -1 : len;
                int boundaryEnd = data.consumeBoolean() ? len + 1 : -1;
                StringUtils.join(array, charSeparator, boundaryStart, boundaryEnd);
                StringUtils.join(array, stringSeparator, boundaryStart, boundaryEnd);
            }
        } else {
            StringUtils.join((Object[]) null, charSeparator, data.consumeInt(), data.consumeInt());
            StringUtils.join((Object[]) null, stringSeparator, data.consumeInt(), data.consumeInt());
        }
    }
}
```

</details>

---
## [8] ⚙️ harness-attempt · `attempt_003`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException: -533960193

---
## [9] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,409 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array;
        if (data.consumeBoolean()) {
            array = null;
        } else {
            int len = data.consumeInt(0, 32);
            array = new Object[len];
            for (int i = 0; i < len; i++) {
                switch (data.consumeInt(0, 8)) {
                    case 0:
                        array[i] = null;
                        break;
                    case 1:
                        array[i] = data.consumeString(32);
                        break;
                    case 2:
                        array[i] = data.consumeAsciiString(32);
                        break;
                    case 3:
                        array[i] = Integer.valueOf(data.consumeInt());
                        break;
                    case 4:
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                    case 5:
                        array[i] = Character.valueOf((char) (data.consumeByte() & 0xff));
                        break;
                    case 6:
                        array[i] = new StringBuilder(data.consumeString(32));
                        break;
                    case 7:
                        array[i] = new StringBuffer(data.consumeAsciiString(32));
                        break;
                    default:
                        array[i] = data.consumeBytes(16);
                        break;
                }
            }
        }

        char charSep = (char) (data.consumeByte() & 0xff);
        String stringSep;
        if (data.consumeBoolean()) {
            stringSep = null;
        } else if (data.consumeBoolean()) {
            stringSep = data.consumeAsciiString(8);
        } else {
            stringSep = data.consumeString(8);
        }

        int startIndex;
        int endIndex;
        if (data.consumeBoolean()) {
            startIndex = data.consumeInt();
            endIndex = data.consumeInt();
        } else {
            int len = array == null ? 0 : array.length;
            int min = -2;
            int max = len + 2;
            startIndex = data.consumeInt(min, max);
            endIndex = data.consumeInt(min, max);
        }

        StringUtils.join(array, charSep, startIndex, endIndex);
        StringUtils.join(array, stringSep, startIndex, endIndex);

        if (array != null) {
            int len = array.length;

            StringUtils.join(array, charSep, 0, len);
            StringUtils.join(array, stringSep, 0, len);

            StringUtils.join(array, charSep, 0, 0);
            StringUtils.join(array, stringSep, 0, 0);

            StringUtils.join(array, charSep, len, len);
            StringUtils.join(array, stringSep, len, len);

            if (len > 0) {
                StringUtils.join(array, charSep, 0, 1);
                StringUtils.join(array, stringSep, 0, 1);

                StringUtils.join(array, charSep, len - 1, len);
                StringUtils.join(array, stringSep, len - 1, len);
            }
        } else {
            StringUtils.join((Object[]) null, charSep, 0, 0);
            StringUtils.join((Object[]) null, stringSep, 0, 0);
        }
    }
}
```

</details>

---
## [10] ⚙️ harness-attempt · `attempt_004`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException: -2

---
## [11] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,717 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int length;
        if (data.remainingBytes() > 0) {
            length = data.consumeInt(0, Math.min(16, data.remainingBytes()));
        } else {
            length = 0;
        }

        Object[] array = new Object[length];
        for (int i = 0; i < length; i++) {
            int choice = data.remainingBytes() > 0 ? data.consumeInt(0, 7) : 0;
            switch (choice) {
                case 0:
                    array[i] = null;
                    break;
                case 1:
                    array[i] = data.consumeString(32);
                    break;
                case 2:
                    array[i] = data.consumeAsciiString(32);
                    break;
                case 3:
                    array[i] = Integer.valueOf(data.consumeInt());
                    break;
                case 4:
                    array[i] = Byte.valueOf(data.consumeByte());
                    break;
                case 5:
                    array[i] = Boolean.valueOf(data.consumeBoolean());
                    break;
                case 6:
                    array[i] = data.consumeBytes(Math.min(16, data.remainingBytes()));
                    break;
                default:
                    array[i] = data.consumeRemainingAsString();
                    break;
            }
        }

        char charSeparator = (char) (data.consumeByte() & 0xFF);
        String stringSeparator;
        if (data.consumeBoolean()) {
            stringSeparator = null;
        } else if (data.consumeBoolean()) {
            stringSeparator = "";
        } else if (data.consumeBoolean()) {
            stringSeparator = data.consumeAsciiString(16);
        } else {
            stringSeparator = data.consumeString(16);
        }

        StringUtils.join((Object[]) null, charSeparator, data.consumeInt(), data.consumeInt());
        StringUtils.join((Object[]) null, stringSeparator, data.consumeInt(), data.consumeInt());

        int safeStart = 0;
        int safeEnd = length;
        if (length > 0) {
            safeStart = data.consumeInt(0, length);
            safeEnd = data.consumeInt(safeStart, length);
        }

        StringUtils.join(array, charSeparator, safeStart, safeEnd);
        StringUtils.join(array, stringSeparator, safeStart, safeEnd);

        if (length > 0) {
            int singleIndex = data.consumeInt(0, length - 1);
            StringUtils.join(array, charSeparator, singleIndex, singleIndex + 1);
            StringUtils.join(array, stringSeparator, singleIndex, singleIndex + 1);
        }

        StringUtils.join(array, charSeparator, 0, 0);
        StringUtils.join(array, stringSeparator, 0, 0);
        StringUtils.join(array, charSeparator, length, length);
        StringUtils.join(array, stringSeparator, length, length);

        int rawStart = data.consumeInt();
        int rawEnd = data.consumeInt();

        if (data.consumeBoolean()) {
            rawStart = rawStart % (length + 3) - 1;
        }
        if (data.consumeBoolean()) {
            rawEnd = rawEnd % (length + 3) - 1;
        }

        if (data.consumeBoolean()) {
            rawStart = length == 0 ? 0 : length;
        }
        if (data.consumeBoolean()) {
            rawEnd = length == 0 ? 1 : length + 1;
        }

        if (data.consumeBoolean()) {
            StringUtils.join(array, charSeparator, rawStart, rawEnd);
        } else {
            StringUtils.join(array, stringSeparator, rawStart, rawEnd);
        }
    }
}
```

</details>

---
## [12] ⚙️ harness-attempt · `attempt_005`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException: 6

---
## [13] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,498 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array;
        if (data.consumeBoolean()) {
            array = null;
        } else {
            int len = data.consumeInt(0, 16);
            array = new Object[len];
            for (int i = 0; i < len; i++) {
                switch (data.consumeInt(0, 8)) {
                    case 0:
                        array[i] = null;
                        break;
                    case 1:
                        array[i] = data.consumeString(32);
                        break;
                    case 2:
                        array[i] = data.consumeAsciiString(32);
                        break;
                    case 3:
                        array[i] = Integer.valueOf(data.consumeInt());
                        break;
                    case 4:
                        array[i] = Long.valueOf((long) data.consumeInt());
                        break;
                    case 5:
                        array[i] = Character.valueOf((char) (data.consumeByte() & 0xFF));
                        break;
                    case 6:
                        array[i] = new StringBuilder(data.consumeString(32));
                        break;
                    case 7:
                        array[i] = new StringBuffer(data.consumeAsciiString(32));
                        break;
                    default:
                        array[i] = data.consumeRemainingAsString();
                        break;
                }
            }
        }

        char charSeparator = (char) (data.consumeByte() & 0xFF);
        String stringSeparator = data.consumeBoolean() ? null : data.consumeString(16);

        int len = array == null ? 0 : array.length;

        int start1 = data.consumeInt();
        int end1 = data.consumeInt();

        int start2;
        switch (data.consumeInt(0, 8)) {
            case 0:
                start2 = -1;
                break;
            case 1:
                start2 = 0;
                break;
            case 2:
                start2 = 1;
                break;
            case 3:
                start2 = len - 1;
                break;
            case 4:
                start2 = len;
                break;
            case 5:
                start2 = len + 1;
                break;
            case 6:
                start2 = Integer.MIN_VALUE;
                break;
            case 7:
                start2 = Integer.MAX_VALUE;
                break;
            default:
                start2 = data.consumeInt();
                break;
        }

        int end2;
        switch (data.consumeInt(0, 8)) {
            case 0:
                end2 = -1;
                break;
            case 1:
                end2 = 0;
                break;
            case 2:
                end2 = 1;
                break;
            case 3:
                end2 = len - 1;
                break;
            case 4:
                end2 = len;
                break;
            case 5:
                end2 = len + 1;
                break;
            case 6:
                end2 = Integer.MIN_VALUE;
                break;
            case 7:
                end2 = Integer.MAX_VALUE;
                break;
            default:
                end2 = data.consumeInt();
                break;
        }

        StringUtils.join(array, charSeparator, start1, end1);
        StringUtils.join(array, stringSeparator, start1, end1);
        StringUtils.join(array, charSeparator, start2, end2);
        StringUtils.join(array, stringSeparator, start2, end2);

        if (array != null) {
            StringUtils.join(array, charSeparator, 0, len);
            StringUtils.join(array, stringSeparator, 0, len);
            StringUtils.join(array, charSeparator, len, len);
            StringUtils.join(array, stringSeparator, len, len);
            StringUtils.join(array, charSeparator, 0, 0);
            StringUtils.join(array, stringSeparator, 0, 0);
            if (len > 0) {
                StringUtils.join(array, charSeparator, 0, 1);
                StringUtils.join(array, stringSeparator, 0, 1);
                StringUtils.join(array, charSeparator, len - 1, len);
                StringUtils.join(array, stringSeparator, len - 1, len);
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
- trigger: java.lang.ArrayIndexOutOfBoundsException: -224

---
## [15] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,447 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array;
        int len = data.consumeInt(0, 16);
        if (data.consumeBoolean()) {
            array = null;
        } else {
            array = new Object[len];
            for (int i = 0; i < len; i++) {
                switch (data.consumeInt(0, 7)) {
                    case 0:
                        array[i] = null;
                        break;
                    case 1:
                        array[i] = data.consumeString(32);
                        break;
                    case 2:
                        array[i] = data.consumeAsciiString(32);
                        break;
                    case 3:
                        array[i] = Integer.valueOf(data.consumeInt());
                        break;
                    case 4:
                        array[i] = Long.valueOf((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                        break;
                    case 5:
                        array[i] = Character.valueOf((char) (data.consumeByte() & 0xff));
                        break;
                    case 6:
                        array[i] = new StringBuilder(data.consumeString(32));
                        break;
                    default:
                        array[i] = data.consumeBytes(16);
                        break;
                }
            }
        }

        char separatorChar = (char) (data.consumeByte() & 0xff);

        String separatorString;
        switch (data.consumeInt(0, 4)) {
            case 0:
                separatorString = null;
                break;
            case 1:
                separatorString = "";
                break;
            case 2:
                separatorString = String.valueOf(separatorChar);
                break;
            case 3:
                separatorString = data.consumeAsciiString(8);
                break;
            default:
                separatorString = data.consumeString(16);
                break;
        }

        if (array == null) {
            StringUtils.join((Object[]) null, separatorChar, data.consumeInt(), data.consumeInt());
            StringUtils.join((Object[]) null, separatorString, data.consumeInt(), data.consumeInt());
            return;
        }

        int n = array.length;

        StringUtils.join(array, separatorChar, 0, n);
        StringUtils.join(array, separatorString, 0, n);

        int safeStart = data.consumeInt(0, n);
        int safeEnd = data.consumeInt(safeStart, n);
        StringUtils.join(array, separatorChar, safeStart, safeEnd);
        StringUtils.join(array, separatorString, safeStart, safeEnd);

        StringUtils.join(array, separatorChar, safeStart, safeStart);
        StringUtils.join(array, separatorString, safeStart, safeStart);

        if (n > 0) {
            int oneStart = data.consumeInt(0, n - 1);
            int oneEnd = Math.min(n, oneStart + 1);
            StringUtils.join(array, separatorChar, oneStart, oneEnd);
            StringUtils.join(array, separatorString, oneStart, oneEnd);
        }

        int startIndex;
        switch (data.consumeInt(0, 9)) {
            case 0:
                startIndex = -1;
                break;
            case 1:
                startIndex = 0;
                break;
            case 2:
                startIndex = 1;
                break;
            case 3:
                startIndex = n - 1;
                break;
            case 4:
                startIndex = n;
                break;
            case 5:
                startIndex = n + 1;
                break;
            case 6:
                startIndex = -n - 1;
                break;
            case 7:
                startIndex = data.consumeInt(-n - 2, n + 2);
                break;
            case 8:
                startIndex = Integer.MIN_VALUE;
                break;
            default:
                startIndex = data.consumeInt();
                break;
        }

        int endIndex;
        switch (data.consumeInt(0, 9)) {
            case 0:
                endIndex = -1;
                break;
            case 1:
                endIndex = 0;
                break;
            case 2:
                endIndex = 1;
                break;
            case 3:
                endIndex = n - 1;
                break;
            case 4:
                endIndex = n;
                break;
            case 5:
                endIndex = n + 1;
                break;
            case 6:
                endIndex = -n - 1;
                break;
            case 7:
                endIndex = data.consumeInt(-n - 2, n + 2);
                break;
            case 8:
                endIndex = Integer.MAX_VALUE;
                break;
            default:
                endIndex = data.consumeInt();
                break;
        }

        if (data.consumeBoolean()) {
            StringUtils.join(array, separatorChar, startIndex, endIndex);
            StringUtils.join(array, separatorString, startIndex, endIndex);
        } else {
            StringUtils.join(array, separatorString, startIndex, endIndex);
            StringUtils.join(array, separatorChar, startIndex, endIndex);
        }
    }
}
```

</details>

---
## [16] ⚙️ harness-attempt · `attempt_007`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException: -1

---
## [17] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,578 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array;
        if (data.consumeBoolean()) {
            array = null;
        } else {
            int length = data.consumeInt(0, 16);
            array = new Object[length];
            for (int i = 0; i < length; i++) {
                switch (data.consumeInt(0, 5)) {
                    case 0:
                        array[i] = null;
                        break;
                    case 1:
                        array[i] = data.consumeString(32);
                        break;
                    case 2:
                        array[i] = Integer.valueOf(data.consumeInt());
                        break;
                    case 3:
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                    case 4:
                        array[i] = Byte.valueOf(data.consumeByte());
                        break;
                    default:
                        array[i] = new StringBuilder(data.consumeAsciiString(32));
                        break;
                }
            }
        }

        char charSeparator = (char) data.consumeInt(0, Character.MAX_VALUE);
        String stringSeparator;
        switch (data.consumeInt(0, 2)) {
            case 0:
                stringSeparator = null;
                break;
            case 1:
                stringSeparator = "";
                break;
            default:
                stringSeparator = data.consumeString(16);
                break;
        }

        StringUtils.join((Object[]) null, charSeparator, data.consumeInt(), data.consumeInt());
        StringUtils.join((Object[]) null, stringSeparator, data.consumeInt(), data.consumeInt());

        if (array == null) {
            return;
        }

        int len = array.length;

        int safeStart = len == 0 ? 0 : data.consumeInt(0, len);
        int safeEnd = len == 0 ? 0 : data.consumeInt(safeStart, len);
        StringUtils.join(array, charSeparator, safeStart, safeEnd);
        StringUtils.join(array, stringSeparator, safeStart, safeEnd);

        int reverseA = len == 0 ? 0 : data.consumeInt(0, len);
        int reverseB = len == 0 ? 0 : data.consumeInt(0, len);
        int reverseStart = Math.max(reverseA, reverseB);
        int reverseEnd = Math.min(reverseA, reverseB);
        StringUtils.join(array, charSeparator, reverseStart, reverseEnd);
        StringUtils.join(array, stringSeparator, reverseStart, reverseEnd);

        int boundaryStart;
        switch (data.consumeInt(0, 7)) {
            case 0:
                boundaryStart = -2;
                break;
            case 1:
                boundaryStart = -1;
                break;
            case 2:
                boundaryStart = 0;
                break;
            case 3:
                boundaryStart = len == 0 ? 0 : len - 1;
                break;
            case 4:
                boundaryStart = len;
                break;
            case 5:
                boundaryStart = len + 1;
                break;
            case 6:
                boundaryStart = len + 2;
                break;
            default:
                boundaryStart = data.consumeInt();
                break;
        }

        int boundaryEnd;
        switch (data.consumeInt(0, 7)) {
            case 0:
                boundaryEnd = -2;
                break;
            case 1:
                boundaryEnd = -1;
                break;
            case 2:
                boundaryEnd = 0;
                break;
            case 3:
                boundaryEnd = len == 0 ? 0 : len - 1;
                break;
            case 4:
                boundaryEnd = len;
                break;
            case 5:
                boundaryEnd = len + 1;
                break;
            case 6:
                boundaryEnd = len + 2;
                break;
            default:
                boundaryEnd = data.consumeInt();
                break;
        }

        if (data.consumeBoolean()) {
            StringUtils.join(array, charSeparator, boundaryStart, boundaryEnd);
            StringUtils.join(array, stringSeparator, boundaryStart, boundaryEnd);
        } else {
            StringUtils.join(array, stringSeparator, boundaryStart, boundaryEnd);
            StringUtils.join(array, charSeparator, boundaryStart, boundaryEnd);
        }
    }
}
```

</details>

---
## [18] ⚙️ harness-attempt · `attempt_008`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException: -2

---
## [19] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~2,698 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array;
        if (data.consumeBoolean()) {
            array = null;
        } else {
            int length = data.consumeInt(0, 16);
            array = new Object[length];
            for (int i = 0; i < length; i++) {
                switch (data.consumeInt(0, 7)) {
                    case 0:
                        array[i] = null;
                        break;
                    case 1:
                        array[i] = data.consumeString(32);
                        break;
                    case 2:
                        array[i] = data.consumeAsciiString(32);
                        break;
                    case 3:
                        array[i] = Integer.valueOf(data.consumeInt());
                        break;
                    case 4:
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                    case 5:
                        array[i] = Character.valueOf((char) (data.consumeByte() & 0xFF));
                        break;
                    case 6:
                        array[i] = new StringBuilder(data.consumeString(32));
                        break;
                    default:
                        array[i] = data.consumeBytes(16);
                        break;
                }
            }
        }

        char charSeparator = (char) (data.consumeByte() & 0xFF);
        String stringSeparator1 = data.consumeBoolean() ? null : data.consumeString(16);
        String stringSeparator2 = data.consumeBoolean() ? "" : data.consumeAsciiString(16);

        int rawStart = data.consumeInt();
        int rawEnd = data.consumeInt();

        int len = array == null ? 0 : array.length;
        int[] starts = new int[] {
            rawStart,
            rawEnd,
            0,
            len,
            len == 0 ? 0 : len - 1,
            -1,
            len + 1
        };
        int[] ends = new int[] {
            rawEnd,
            rawStart,
            0,
            len,
            len == 0 ? 0 : len - 1,
            -1,
            len + 1
        };

        for (int i = 0; i < starts.length; i++) {
            for (int j = 0; j < ends.length; j++) {
                StringUtils.join(array, charSeparator, starts[i], ends[j]);
                StringUtils.join(array, stringSeparator1, starts[i], ends[j]);
                StringUtils.join(array, stringSeparator2, starts[i], ends[j]);
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
- trigger: java.lang.ArrayIndexOutOfBoundsException: 0

---
## [21] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,251 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array = null;
        if (data.consumeBoolean()) {
            int size = data.consumeInt(0, 16);
            array = new Object[size];
            for (int i = 0; i < size; i++) {
                switch (data.consumeInt(0, 7)) {
                    case 0:
                        array[i] = null;
                        break;
                    case 1:
                        array[i] = data.consumeString(32);
                        break;
                    case 2:
                        array[i] = data.consumeAsciiString(32);
                        break;
                    case 3:
                        array[i] = Integer.valueOf(data.consumeInt());
                        break;
                    case 4:
                        array[i] = Character.valueOf((char) (data.consumeByte() & 0xff));
                        break;
                    case 5:
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                    case 6:
                        array[i] = new String(data.consumeBytes(32));
                        break;
                    default:
                        array[i] = data.consumeRemainingAsString();
                        break;
                }
            }
        }

        char charSeparator = (char) (data.consumeByte() & 0xff);
        String stringSeparator;
        if (data.consumeBoolean()) {
            stringSeparator = null;
        } else if (data.consumeBoolean()) {
            stringSeparator = data.consumeAsciiString(16);
        } else {
            stringSeparator = data.consumeString(16);
        }

        int length = array == null ? 0 : array.length;

        int[][] boundaryPairs = new int[][] {
            {0, 0},
            {0, length},
            {length, length},
            {-1, length},
            {0, length + 1},
            {-1, -1},
            {1, 0},
            {length - 1, length},
            {length, length + 1}
        };

        for (int i = 0; i < boundaryPairs.length; i++) {
            StringUtils.join(array, charSeparator, boundaryPairs[i][0], boundaryPairs[i][1]);
            StringUtils.join(array, stringSeparator, boundaryPairs[i][0], boundaryPairs[i][1]);
        }

        int extraCalls = data.remainingBytes() > 0 ? data.consumeInt(1, 8) : 1;
        for (int i = 0; i < extraCalls; i++) {
            int startIndex;
            int endIndex;

            if (data.consumeBoolean()) {
                startIndex = data.consumeInt();
                endIndex = data.consumeInt();
            } else {
                int span = length + 4;
                startIndex = data.consumeInt(-span, span);
                endIndex = data.consumeInt(-span, span);
            }

            if (data.consumeBoolean()) {
                StringUtils.join(array, charSeparator, startIndex, endIndex);
            } else {
                StringUtils.join(array, stringSeparator, startIndex, endIndex);
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
- trigger: java.lang.ArrayIndexOutOfBoundsException: -1

---
## [23] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,817 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array;
        int length = 0;

        if (data.consumeBoolean()) {
            array = null;
        } else {
            length = data.consumeInt(0, 16);
            array = new Object[length];
            for (int i = 0; i < length; i++) {
                switch (data.consumeInt(0, 9)) {
                    case 0:
                        array[i] = null;
                        break;
                    case 1:
                        array[i] = data.consumeString(32);
                        break;
                    case 2:
                        array[i] = data.consumeAsciiString(32);
                        break;
                    case 3:
                        array[i] = Integer.valueOf(data.consumeInt());
                        break;
                    case 4:
                        array[i] = Character.valueOf((char) (data.consumeByte() & 0xff));
                        break;
                    case 5:
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                    case 6:
                        array[i] = new StringBuilder(data.consumeString(32));
                        break;
                    case 7:
                        array[i] = new StringBuffer(data.consumeAsciiString(32));
                        break;
                    case 8:
                        array[i] = data.consumeBytes(16);
                        break;
                    default:
                        array[i] = Long.valueOf((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                        break;
                }
            }
        }

        char charSeparator = (char) (data.consumeByte() & 0xff);
        String stringSeparator;
        switch (data.consumeInt(0, 3)) {
            case 0:
                stringSeparator = null;
                break;
            case 1:
                stringSeparator = data.consumeString(16);
                break;
            case 2:
                stringSeparator = data.consumeAsciiString(16);
                break;
            default:
                stringSeparator = data.consumeRemainingAsString();
                break;
        }

        int startIndex;
        int endIndex;

        if (array == null) {
            startIndex = data.consumeInt();
            endIndex = data.consumeInt();
        } else {
            switch (data.consumeInt(0, 5)) {
                case 0:
                    startIndex = data.consumeInt();
                    endIndex = data.consumeInt();
                    break;
                case 1:
                    startIndex = data.consumeInt(-2, length + 2);
                    endIndex = data.consumeInt(-2, length + 2);
                    break;
                case 2:
                    startIndex = 0;
                    endIndex = length;
                    break;
                case 3:
                    startIndex = length;
                    endIndex = length + 1;
                    break;
                case 4:
                    startIndex = data.consumeInt(0, length);
                    endIndex = data.consumeInt(startIndex, length);
                    break;
                default:
                    startIndex = data.consumeInt(-1, length);
                    endIndex = data.consumeInt(startIndex, length + 1);
                    break;
            }
        }

        StringUtils.join(array, charSeparator, startIndex, endIndex);
        StringUtils.join(array, stringSeparator, startIndex, endIndex);
    }
}
```

</details>

---
## [24] ⚙️ harness-attempt · `attempt_011`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException: 2

---
## [25] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,270 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array = null;
        if (data.consumeBoolean()) {
            int length = data.consumeInt(0, 16);
            array = new Object[length];
            for (int i = 0; i < length; i++) {
                switch (data.consumeInt(0, 9)) {
                    case 0:
                        array[i] = null;
                        break;
                    case 1:
                        array[i] = data.consumeString(32);
                        break;
                    case 2:
                        array[i] = data.consumeAsciiString(32);
                        break;
                    case 3:
                        array[i] = Integer.valueOf(data.consumeInt());
                        break;
                    case 4:
                        array[i] = Long.valueOf((long) data.consumeInt());
                        break;
                    case 5:
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                    case 6:
                        array[i] = Character.valueOf((char) (data.consumeByte() & 0xff));
                        break;
                    case 7:
                        array[i] = new StringBuilder(data.consumeString(32));
                        break;
                    case 8:
                        array[i] = data.consumeBytes(16);
                        break;
                    default:
                        array[i] = data.consumeRemainingAsString();
                        break;
                }
            }
        }

        int len = array == null ? 0 : array.length;

        int safeStart = data.consumeInt(0, len);
        int safeEnd = data.consumeInt(0, len);
        if (data.consumeBoolean() && safeStart > safeEnd) {
            int tmp = safeStart;
            safeStart = safeEnd;
            safeEnd = tmp;
        }

        int boundedStart = data.consumeInt(-2, len + 2);
        int boundedEnd = data.consumeInt(-2, len + 2);

        int rawStart = data.consumeBoolean() ? data.consumeInt() : boundedStart;
        int rawEnd = data.consumeBoolean() ? data.consumeInt() : boundedEnd;

        char charSeparator = (char) (data.consumeByte() & 0xff);

        String stringSeparator;
        if (data.consumeBoolean()) {
            stringSeparator = null;
        } else if (data.consumeBoolean()) {
            stringSeparator = data.consumeString(16);
        } else if (data.consumeBoolean()) {
            stringSeparator = data.consumeAsciiString(16);
        } else {
            stringSeparator = data.consumeRemainingAsString();
        }

        StringUtils.join(array, charSeparator, safeStart, safeEnd);
        StringUtils.join(array, stringSeparator, safeStart, safeEnd);

        StringUtils.join(array, charSeparator, boundedStart, boundedEnd);
        StringUtils.join(array, stringSeparator, boundedStart, boundedEnd);

        StringUtils.join(array, charSeparator, rawStart, rawEnd);
        StringUtils.join(array, stringSeparator, rawStart, rawEnd);
    }
}
```

</details>

---
## [26] ⚙️ harness-attempt · `attempt_012`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException: -370546199

---
## [27] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,354 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array;
        if (data.consumeBoolean()) {
            array = null;
        } else {
            int length = data.consumeInt(0, 16);
            array = new Object[length];
            for (int i = 0; i < length; i++) {
                int kind = data.consumeInt(0, 7);
                switch (kind) {
                    case 0:
                        array[i] = null;
                        break;
                    case 1:
                        array[i] = data.consumeString(32);
                        break;
                    case 2:
                        array[i] = data.consumeAsciiString(32);
                        break;
                    case 3:
                        array[i] = Integer.valueOf(data.consumeInt());
                        break;
                    case 4:
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                    case 5:
                        array[i] = Byte.valueOf(data.consumeByte());
                        break;
                    case 6:
                        array[i] = data.consumeBytes(16);
                        break;
                    default:
                        array[i] = Character.valueOf((char) (data.consumeByte() & 0xff));
                        break;
                }
            }
        }

        char charSeparator = (char) (data.consumeByte() & 0xff);
        String stringSeparator = data.consumeBoolean() ? null : data.consumeString(16);

        int len = array == null ? 0 : array.length;

        int start1 = data.consumeBoolean() ? data.consumeInt() : data.consumeInt(-2, len + 2);
        int end1 = data.consumeBoolean() ? data.consumeInt() : data.consumeInt(-2, len + 2);
        StringUtils.join(array, charSeparator, start1, end1);

        int start2 = data.consumeBoolean() ? data.consumeInt() : data.consumeInt(-2, len + 2);
        int end2 = data.consumeBoolean() ? data.consumeInt() : data.consumeInt(-2, len + 2);
        StringUtils.join(array, stringSeparator, start2, end2);

        if (array != null) {
            StringUtils.join(array, charSeparator, 0, len);
            StringUtils.join(array, stringSeparator, 0, len);

            if (len > 0) {
                StringUtils.join(array, charSeparator, len - 1, len);
                StringUtils.join(array, stringSeparator, len - 1, len);
            }

            StringUtils.join(array, charSeparator, len, len);
            StringUtils.join(array, stringSeparator, len, len);

            StringUtils.join(array, charSeparator, 0, 0);
            StringUtils.join(array, stringSeparator, 0, 0);

            StringUtils.join(array, charSeparator, -1, len);
            StringUtils.join(array, stringSeparator, -1, len);

            StringUtils.join(array, charSeparator, 0, len + 1);
            StringUtils.join(array, stringSeparator, 0, len + 1);
        } else {
            StringUtils.join(array, charSeparator, data.consumeInt(), data.consumeInt());
            StringUtils.join(array, stringSeparator, data.consumeInt(), data.consumeInt());
        }
    }
}
```

</details>

---
## [28] ⚙️ harness-attempt · `attempt_013`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException: -1

---
## [29] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,725 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array = null;
        boolean useNullArray = data.consumeBoolean();

        if (!useNullArray) {
            int length = data.consumeInt(0, 32);
            array = new Object[length];
            for (int i = 0; i < length; i++) {
                int kind = data.consumeInt(0, 6);
                switch (kind) {
                    case 0:
                        array[i] = null;
                        break;
                    case 1:
                        array[i] = data.consumeString(32);
                        break;
                    case 2:
                        array[i] = data.consumeAsciiString(32);
                        break;
                    case 3:
                        array[i] = Integer.valueOf(data.consumeInt());
                        break;
                    case 4:
                        array[i] = Character.valueOf((char) (data.consumeByte() & 0xFF));
                        break;
                    case 5:
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                    default:
                        array[i] = new String(data.consumeBytes(16));
                        break;
                }
            }
        }

        char charSeparator = (char) (data.consumeByte() & 0xFF);
        String stringSeparator;
        if (data.consumeBoolean()) {
            stringSeparator = null;
        } else if (data.consumeBoolean()) {
            stringSeparator = data.consumeAsciiString(16);
        } else {
            stringSeparator = data.consumeString(16);
        }

        if (array == null) {
            StringUtils.join((Object[]) null, charSeparator, data.consumeInt(), data.consumeInt());
            StringUtils.join((Object[]) null, stringSeparator, data.consumeInt(), data.consumeInt());
            return;
        }

        int len = array.length;

        StringUtils.join(array, charSeparator, 0, len);
        StringUtils.join(array, stringSeparator, 0, len);

        int safeStart1 = data.consumeInt(0, len);
        int safeEnd1 = data.consumeInt(0, len);
        if (safeStart1 > safeEnd1) {
            int t = safeStart1;
            safeStart1 = safeEnd1;
            safeEnd1 = t;
        }
        StringUtils.join(array, charSeparator, safeStart1, safeEnd1);
        StringUtils.join(array, stringSeparator, safeStart1, safeEnd1);

        StringUtils.join(array, charSeparator, len, len);
        StringUtils.join(array, stringSeparator, len, len);

        if (len > 0) {
            int single = data.consumeInt(0, len - 1);
            StringUtils.join(array, charSeparator, single, single + 1);
            StringUtils.join(array, stringSeparator, single, single + 1);
        }

        int startIndex;
        if (data.consumeBoolean()) {
            startIndex = data.consumeInt();
        } else {
            startIndex = data.consumeInt(-4, len + 4);
        }

        int endIndex;
        if (data.consumeBoolean()) {
            endIndex = data.consumeInt();
        } else {
            endIndex = data.consumeInt(-4, len + 4);
        }

        if (data.consumeBoolean()) {
            StringUtils.join(array, charSeparator, startIndex, endIndex);
            StringUtils.join(array, stringSeparator, startIndex, endIndex);
        } else {
            StringUtils.join(array, stringSeparator, startIndex, endIndex);
            StringUtils.join(array, charSeparator, startIndex, endIndex);
        }
    }
}
```

</details>

---
## [30] ⚙️ harness-attempt · `attempt_014`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException: 1852404340

---
## [31] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,275 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array = null;

        if (data.consumeBoolean()) {
            int len = data.consumeInt(0, 16);
            array = new Object[len];
            for (int i = 0; i < len; i++) {
                switch (data.consumeInt(0, 7)) {
                    case 0:
                        array[i] = null;
                        break;
                    case 1:
                        array[i] = data.consumeString(32);
                        break;
                    case 2:
                        array[i] = data.consumeAsciiString(32);
                        break;
                    case 3:
                        array[i] = Integer.valueOf(data.consumeInt());
                        break;
                    case 4:
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                    case 5:
                        array[i] = new String(data.consumeBytes(32));
                        break;
                    case 6:
                        array[i] = new StringBuilder(data.consumeString(32));
                        break;
                    default:
                        array[i] = data.consumeBytes(16);
                        break;
                }
            }
        }

        char charSeparator = (char) (data.consumeByte() & 0xFF);
        String stringSeparator;
        switch (data.consumeInt(0, 3)) {
            case 0:
                stringSeparator = null;
                break;
            case 1:
                stringSeparator = "";
                break;
            case 2:
                stringSeparator = data.consumeString(16);
                break;
            default:
                stringSeparator = data.consumeAsciiString(16);
                break;
        }

        int anyStart = data.consumeInt();
        int anyEnd = data.consumeInt();

        StringUtils.join(array, charSeparator, anyStart, anyEnd);
        StringUtils.join(array, stringSeparator, anyStart, anyEnd);

        if (array != null) {
            int len = array.length;

            int boundedStart = data.consumeInt(-2, len + 2);
            int boundedEnd = data.consumeInt(-2, len + 2);
            StringUtils.join(array, charSeparator, boundedStart, boundedEnd);
            StringUtils.join(array, stringSeparator, boundedStart, boundedEnd);

            int orderedStart = data.consumeInt(-2, len + 2);
            int orderedEnd = data.consumeInt(-2, len + 2);
            if (orderedStart > orderedEnd) {
                int t = orderedStart;
                orderedStart = orderedEnd;
                orderedEnd = t;
            }
            StringUtils.join(array, charSeparator, orderedStart, orderedEnd);
            StringUtils.join(array, stringSeparator, orderedStart, orderedEnd);

            StringUtils.join(array, charSeparator, 0, len);
            StringUtils.join(array, stringSeparator, 0, len);

            StringUtils.join(array, charSeparator, 0, 0);
            StringUtils.join(array, stringSeparator, 0, 0);

            StringUtils.join(array, charSeparator, len, len);
            StringUtils.join(array, stringSeparator, len, len);

            StringUtils.join(array, charSeparator, -1, len);
            StringUtils.join(array, stringSeparator, -1, len);

            StringUtils.join(array, charSeparator, 0, len + 1);
            StringUtils.join(array, stringSeparator, 0, len + 1);

            if (len > 0) {
                StringUtils.join(array, charSeparator, len - 1, len);
                StringUtils.join(array, stringSeparator, len - 1, len);

                StringUtils.join(array, charSeparator, 1, len);
                StringUtils.join(array, stringSeparator, 1, len);

                StringUtils.join(array, charSeparator, 0, 1);
                StringUtils.join(array, stringSeparator, 0, 1);
            }
        } else {
            StringUtils.join((Object[]) null, charSeparator, 0, 1);
            StringUtils.join((Object[]) null, stringSeparator, 0, 1);
        }
    }
}
```

</details>

---
## [32] ⚙️ harness-attempt · `attempt_015`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException: -1

---
## [33] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,215 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array;
        if (data.consumeBoolean()) {
            array = null;
        } else {
            int len = data.consumeInt(0, 8);
            array = new Object[len];
            for (int i = 0; i < len; i++) {
                switch (data.consumeInt(0, 7)) {
                    case 0:
                        array[i] = null;
                        break;
                    case 1:
                        array[i] = data.consumeString(32);
                        break;
                    case 2:
                        array[i] = data.consumeAsciiString(32);
                        break;
                    case 3:
                        array[i] = Integer.valueOf(data.consumeInt());
                        break;
                    case 4:
                        array[i] = Character.valueOf((char) (data.consumeByte() & 0xFF));
                        break;
                    case 5:
                        array[i] = new StringBuilder(data.consumeString(16));
                        break;
                    case 6:
                        array[i] = data.consumeBytes(16);
                        break;
                    default:
                        array[i] = data.consumeRemainingAsString();
                        break;
                }
            }
        }

        char charSeparator = (char) (data.consumeByte() & 0xFF);
        String stringSeparator = data.consumeBoolean() ? null : data.consumeString(16);

        StringUtils.join((Object[]) null, charSeparator, 0, 0);
        StringUtils.join((Object[]) null, stringSeparator, 0, 0);

        if (array != null) {
            int len = array.length;
            int safeStart = data.consumeInt(0, len);
            int safeEnd = data.consumeInt(safeStart, len);
            StringUtils.join(array, charSeparator, safeStart, safeEnd);
            StringUtils.join(array, stringSeparator, safeStart, safeEnd);

            int[] candidates = new int[] {
                -1,
                0,
                1,
                len - 1,
                len,
                len + 1,
                Integer.MIN_VALUE,
                Integer.MAX_VALUE
            };

            int startIndex;
            int endIndex;
            if (data.consumeBoolean()) {
                startIndex = data.consumeInt();
                endIndex = data.consumeInt();
            } else {
                startIndex = candidates[data.consumeInt(0, candidates.length - 1)];
                endIndex = candidates[data.consumeInt(0, candidates.length - 1)];
            }

            if (data.consumeBoolean()) {
                StringUtils.join(array, charSeparator, startIndex, endIndex);
                StringUtils.join(array, stringSeparator, startIndex, endIndex);
            } else {
                StringUtils.join(array, stringSeparator, startIndex, endIndex);
                StringUtils.join(array, charSeparator, startIndex, endIndex);
            }
        }
    }
}
```

</details>

---
## [34] ⚙️ harness-attempt · `attempt_016`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException: -2147483648

---
## [35] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,418 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int size = data.consumeInt(0, 16);
        Object[] array = new Object[size];

        for (int i = 0; i < size; i++) {
            switch (data.consumeInt(0, 7)) {
                case 0:
                    array[i] = null;
                    break;
                case 1:
                    array[i] = data.consumeString(32);
                    break;
                case 2:
                    array[i] = data.consumeAsciiString(32);
                    break;
                case 3:
                    array[i] = Integer.valueOf(data.consumeInt());
                    break;
                case 4:
                    array[i] = Long.valueOf(data.consumeInt());
                    break;
                case 5:
                    array[i] = Character.valueOf((char) (data.consumeByte() & 0xFF));
                    break;
                case 6:
                    array[i] = Boolean.valueOf(data.consumeBoolean());
                    break;
                default:
                    array[i] = new String(data.consumeBytes(32));
                    break;
            }
        }

        Object[] maybeNullArray = data.consumeBoolean() ? null : array;
        char charSeparator = (char) (data.consumeByte() & 0xFF);
        String stringSeparator = data.consumeBoolean() ? null : data.consumeString(16);

        int rawStart1 = data.consumeInt();
        int rawEnd1 = data.consumeInt();
        StringUtils.join(maybeNullArray, charSeparator, rawStart1, rawEnd1);

        int rawStart2 = data.consumeInt();
        int rawEnd2 = data.consumeInt();
        StringUtils.join(maybeNullArray, stringSeparator, rawStart2, rawEnd2);

        int len = array.length;
        int boundedStart = data.consumeInt(-2, len + 2);
        int boundedEnd = data.consumeInt(-2, len + 2);
        StringUtils.join(array, charSeparator, boundedStart, boundedEnd);
        StringUtils.join(array, stringSeparator, boundedStart, boundedEnd);

        int startAtBoundary;
        int endAtBoundary;
        switch (data.consumeInt(0, 7)) {
            case 0:
                startAtBoundary = 0;
                endAtBoundary = len;
                break;
            case 1:
                startAtBoundary = len;
                endAtBoundary = len;
                break;
            case 2:
                startAtBoundary = 0;
                endAtBoundary = 0;
                break;
            case 3:
                startAtBoundary = -1;
                endAtBoundary = len;
                break;
            case 4:
                startAtBoundary = 0;
                endAtBoundary = len + 1;
                break;
            case 5:
                startAtBoundary = len - 1;
                endAtBoundary = len;
                break;
            case 6:
                startAtBoundary = 1;
                endAtBoundary = 0;
                break;
            default:
                startAtBoundary = len + 1;
                endAtBoundary = len + 1;
                break;
        }

        StringUtils.join(array, charSeparator, startAtBoundary, endAtBoundary);
        StringUtils.join(array, stringSeparator, startAtBoundary, endAtBoundary);
    }
}
```

</details>

---
## [36] ⚙️ harness-attempt · `attempt_017`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException: 0

---
## [37] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,609 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array;

        if (data.consumeBoolean()) {
            array = null;
        } else {
            int len = data.consumeInt(0, 16);
            array = new Object[len];
            for (int i = 0; i < len; i++) {
                if (data.consumeBoolean()) {
                    array[i] = null;
                    continue;
                }
                switch (data.consumeInt(0, 9)) {
                    case 0:
                        array[i] = data.consumeString(32);
                        break;
                    case 1:
                        array[i] = data.consumeAsciiString(32);
                        break;
                    case 2:
                        array[i] = Integer.valueOf(data.consumeInt());
                        break;
                    case 3:
                        array[i] = Long.valueOf(data.consumeInt());
                        break;
                    case 4:
                        array[i] = Character.valueOf((char) (data.consumeByte() & 0xFF));
                        break;
                    case 5:
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                    case 6:
                        array[i] = new StringBuilder(data.consumeString(32));
                        break;
                    case 7:
                        array[i] = new StringBuffer(data.consumeAsciiString(32));
                        break;
                    case 8:
                        array[i] = data.consumeBytes(16);
                        break;
                    default:
                        array[i] = "";
                        break;
                }
            }
        }

        char charSeparator = (char) (data.consumeByte() & 0xFF);
        String stringSeparator;
        if (data.consumeBoolean()) {
            stringSeparator = null;
        } else if (data.consumeBoolean()) {
            stringSeparator = "";
        } else if (data.consumeBoolean()) {
            stringSeparator = data.consumeAsciiString(16);
        } else {
            stringSeparator = data.consumeString(16);
        }

        int startIndex1 = data.consumeInt();
        int endIndex1 = data.consumeInt();
        StringUtils.join(array, charSeparator, startIndex1, endIndex1);

        int startIndex2;
        int endIndex2;
        if (array != null && data.consumeBoolean()) {
            int len = array.length;
            startIndex2 = data.consumeInt(-2, len + 2);
            endIndex2 = data.consumeInt(-2, len + 2);
        } else {
            startIndex2 = data.consumeInt();
            endIndex2 = data.consumeInt();
        }
        StringUtils.join(array, stringSeparator, startIndex2, endIndex2);

        if (array != null) {
            int len = array.length;

            int startIndex3 = data.consumeBoolean() ? 0 : len;
            int endIndex3 = data.consumeBoolean() ? len : 0;
            StringUtils.join(array, charSeparator, startIndex3, endIndex3);

            int startIndex4 = data.consumeInt(-1, len + 1);
            int endIndex4 = startIndex4 + data.consumeInt(-1, 2);
            StringUtils.join(array, stringSeparator, startIndex4, endIndex4);
        } else {
            StringUtils.join(array, charSeparator, 0, 0);
            StringUtils.join(array, stringSeparator, 0, 0);
        }
    }
}
```

</details>

---
## [38] ⚙️ harness-attempt · `attempt_018`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException: -224

---
## [39] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,446 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array;

        if (data.consumeBoolean()) {
            array = null;
        } else {
            int len = data.consumeInt(0, 16);
            array = new Object[len];
            for (int i = 0; i < len; i++) {
                switch (data.consumeInt(0, 8)) {
                    case 0:
                        array[i] = null;
                        break;
                    case 1:
                        array[i] = data.consumeString(32);
                        break;
                    case 2:
                        array[i] = data.consumeAsciiString(32);
                        break;
                    case 3:
                        array[i] = Integer.valueOf(data.consumeInt());
                        break;
                    case 4:
                        array[i] = Long.valueOf((long) data.consumeInt());
                        break;
                    case 5:
                        array[i] = Character.valueOf((char) (data.consumeByte() & 0xff));
                        break;
                    case 6:
                        array[i] = new StringBuilder(data.consumeString(32));
                        break;
                    case 7:
                        array[i] = data.consumeBytes(16);
                        break;
                    default:
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                }
            }
        }

        char charSeparator = (char) (data.consumeByte() & 0xff);
        String stringSeparator = data.consumeBoolean() ? null : data.consumeString(16);

        if (array == null) {
            StringUtils.join(array, charSeparator, 0, 0);
            StringUtils.join(array, stringSeparator, 0, 0);
        } else {
            int safeStart = data.consumeInt(0, array.length);
            int safeEnd = data.consumeInt(safeStart, array.length);

            StringUtils.join(array, charSeparator, 0, 0);
            StringUtils.join(array, stringSeparator, 0, 0);
            StringUtils.join(array, charSeparator, safeStart, safeEnd);
            StringUtils.join(array, stringSeparator, safeStart, safeEnd);
            StringUtils.join(array, charSeparator, 0, array.length);
            StringUtils.join(array, stringSeparator, 0, array.length);
        }

        int len = array == null ? 0 : array.length;
        int edgeStart = data.consumeBoolean() ? data.consumeInt() : data.consumeInt(-2, len + 2);
        int edgeEnd = data.consumeBoolean() ? data.consumeInt() : data.consumeInt(-2, len + 2);

        StringUtils.join(array, charSeparator, edgeStart, edgeEnd);

        String secondSeparator;
        if (data.consumeBoolean()) {
            secondSeparator = null;
        } else if (data.consumeBoolean()) {
            secondSeparator = data.consumeAsciiString(8);
        } else {
            secondSeparator = data.consumeRemainingAsString();
        }

        int edgeStart2 = data.consumeBoolean() ? data.consumeInt() : data.consumeInt(-2, len + 2);
        int edgeEnd2 = data.consumeBoolean() ? data.consumeInt() : data.consumeInt(-2, len + 2);

        StringUtils.join(array, secondSeparator, edgeStart2, edgeEnd2);
    }
}
```

</details>

---
## [40] ⚙️ harness-attempt · `attempt_019`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException: -57300

---
## [41] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,670 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array;
        if (data.consumeBoolean()) {
            array = null;
        } else {
            int length = data.consumeInt(0, 16);
            array = new Object[length];
            for (int i = 0; i < length; i++) {
                switch (data.consumeInt(0, 8)) {
                    case 0:
                        array[i] = null;
                        break;
                    case 1:
                        array[i] = data.consumeString(32);
                        break;
                    case 2:
                        array[i] = data.consumeAsciiString(32);
                        break;
                    case 3:
                        array[i] = Integer.valueOf(data.consumeInt());
                        break;
                    case 4:
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                    case 5:
                        array[i] = Character.valueOf((char) (data.consumeByte() & 0xFF));
                        break;
                    case 6:
                        array[i] = data.consumeBytes(16);
                        break;
                    case 7:
                        array[i] = new StringBuilder(data.consumeString(16));
                        break;
                    default:
                        array[i] = new Object[] {
                            data.consumeString(8),
                            Integer.valueOf(data.consumeInt()),
                            null
                        };
                        break;
                }
            }
        }

        char charSeparator = (char) (data.consumeByte() & 0xFF);
        String stringSeparator;
        if (data.consumeBoolean()) {
            stringSeparator = null;
        } else if (data.consumeBoolean()) {
            stringSeparator = "";
        } else if (data.consumeBoolean()) {
            stringSeparator = data.consumeAsciiString(16);
        } else {
            stringSeparator = data.consumeString(16);
        }

        int startIndex;
        int endIndex;
        if (array != null && data.consumeBoolean()) {
            int len = array.length;
            switch (data.consumeInt(0, 9)) {
                case 0:
                    startIndex = 0;
                    break;
                case 1:
                    startIndex = len;
                    break;
                case 2:
                    startIndex = -1;
                    break;
                case 3:
                    startIndex = len + 1;
                    break;
                default:
                    startIndex = data.consumeInt(-2, len + 2);
                    break;
            }
            switch (data.consumeInt(0, 9)) {
                case 0:
                    endIndex = 0;
                    break;
                case 1:
                    endIndex = len;
                    break;
                case 2:
                    endIndex = -1;
                    break;
                case 3:
                    endIndex = len + 1;
                    break;
                default:
                    endIndex = data.consumeInt(-2, len + 2);
                    break;
            }
        } else {
            startIndex = data.consumeInt();
            endIndex = data.consumeInt();
        }

        if (data.consumeBoolean()) {
            StringUtils.join(array, charSeparator, startIndex, endIndex);
            StringUtils.join(array, stringSeparator, startIndex, endIndex);
        } else {
            StringUtils.join(array, stringSeparator, startIndex, endIndex);
            StringUtils.join(array, charSeparator, startIndex, endIndex);
        }

        if (array != null && data.remainingBytes() > 0) {
            int len = array.length;
            int[][] extraRanges = new int[][] {
                {0, len},
                {0, 0},
                {len, len},
                {-1, len},
                {0, len + 1},
                {1, len},
                {0, len - 1}
            };
            int which = data.consumeInt(0, extraRanges.length - 1);
            int s = extraRanges[which][0];
            int e = extraRanges[which][1];
            if (data.consumeBoolean()) {
                StringUtils.join(array, charSeparator, s, e);
            } else {
                StringUtils.join(array, stringSeparator, s, e);
            }
        }
    }
}
```

</details>

---
## [42] ⚙️ harness-attempt · `attempt_020`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException: -2

---
## [43] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,155 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array;
        if (data.consumeBoolean()) {
            array = null;
        } else {
            int maxLen = Math.min(32, data.remainingBytes() + 1);
            int len = data.consumeInt(0, maxLen);
            array = new Object[len];
            for (int i = 0; i < len; i++) {
                switch (data.consumeInt(0, 8)) {
                    case 0:
                        array[i] = null;
                        break;
                    case 1:
                        array[i] = data.consumeString(32);
                        break;
                    case 2:
                        array[i] = data.consumeAsciiString(32);
                        break;
                    case 3:
                        array[i] = Integer.valueOf(data.consumeInt());
                        break;
                    case 4:
                        array[i] = Long.valueOf((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                        break;
                    case 5:
                        array[i] = Character.valueOf((char) data.consumeInt(0, 65535));
                        break;
                    case 6:
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                    case 7:
                        array[i] = new String(data.consumeBytes(data.consumeInt(0, 16)));
                        break;
                    default:
                        array[i] = new StringBuilder(data.consumeAsciiString(16));
                        break;
                }
            }
        }

        char charSeparator = (char) data.consumeInt(0, 65535);
        String stringSeparator;
        switch (data.consumeInt(0, 3)) {
            case 0:
                stringSeparator = null;
                break;
            case 1:
                stringSeparator = data.consumeString(16);
                break;
            case 2:
                stringSeparator = data.consumeAsciiString(16);
                break;
            default:
                stringSeparator = data.consumeRemainingAsString();
                break;
        }

        if (array != null) {
            int len = array.length;

            int safeStart = data.consumeInt(0, len);
            int safeEnd = data.consumeInt(0, len);
            StringUtils.join(array, charSeparator, safeStart, safeEnd);
            StringUtils.join(array, stringSeparator, safeStart, safeEnd);

            int orderedStart = data.consumeInt(0, len);
            int orderedEnd = data.consumeInt(orderedStart, len);
            StringUtils.join(array, charSeparator, orderedStart, orderedEnd);
            StringUtils.join(array, stringSeparator, orderedStart, orderedEnd);

            if (len > 0) {
                StringUtils.join(array, charSeparator, 0, len);
                StringUtils.join(array, stringSeparator, 0, len);
                StringUtils.join(array, charSeparator, len - 1, len);
                StringUtils.join(array, stringSeparator, len - 1, len);
            } else {
                StringUtils.join(array, charSeparator, 0, 0);
                StringUtils.join(array, stringSeparator, 0, 0);
            }
        } else {
            StringUtils.join((Object[]) null, charSeparator, 0, 0);
            StringUtils.join((Object[]) null, stringSeparator, 0, 0);
        }

        int bound = array == null ? 4 : array.length + 4;
        int startIndex = data.consumeInt(-bound, bound);
        int endIndex = data.consumeInt(-bound, bound);

        if (data.consumeBoolean()) {
            StringUtils.join(array, charSeparator, startIndex, endIndex);
            StringUtils.join(array, stringSeparator, startIndex, endIndex);
        } else {
            StringUtils.join(array, stringSeparator, startIndex, endIndex);
            StringUtils.join(array, charSeparator, startIndex, endIndex);
        }
    }
}
```

</details>

---
## [44] ⚙️ harness-attempt · `attempt_021`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException: -15

---
## [45] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,195 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array;
        if (data.consumeBoolean()) {
            array = null;
        } else {
            int size = data.consumeInt(0, 16);
            array = new Object[size];
            for (int i = 0; i < size; i++) {
                switch (data.consumeInt(0, 7)) {
                    case 0:
                        array[i] = null;
                        break;
                    case 1:
                        array[i] = data.consumeString(32);
                        break;
                    case 2:
                        array[i] = data.consumeAsciiString(32);
                        break;
                    case 3:
                        array[i] = Integer.valueOf(data.consumeInt());
                        break;
                    case 4:
                        array[i] = Character.valueOf((char) (data.consumeByte() & 0xFF));
                        break;
                    case 5:
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                    case 6:
                        array[i] = new String(data.consumeBytes(32));
                        break;
                    default:
                        array[i] = new StringBuilder(data.consumeString(32));
                        break;
                }
            }
        }

        char charSeparator = (char) (data.consumeByte() & 0xFF);
        String stringSeparator;
        if (data.consumeBoolean()) {
            stringSeparator = null;
        } else if (data.consumeBoolean()) {
            stringSeparator = "";
        } else if (data.consumeBoolean()) {
            stringSeparator = data.consumeAsciiString(16);
        } else {
            stringSeparator = data.consumeString(16);
        }

        int size = array == null ? 0 : array.length;

        int boundedStart = data.consumeInt(-2, size + 2);
        int boundedEnd = data.consumeInt(-2, size + 2);

        StringUtils.join(array, charSeparator, boundedStart, boundedEnd);
        StringUtils.join(array, stringSeparator, boundedStart, boundedEnd);

        if (array != null) {
            StringUtils.join(array, charSeparator, 0, size);
            StringUtils.join(array, stringSeparator, 0, size);

            StringUtils.join(array, charSeparator, size, size);
            StringUtils.join(array, stringSeparator, size, size);

            if (size > 0) {
                StringUtils.join(array, charSeparator, 0, 1);
                StringUtils.join(array, stringSeparator, 0, 1);

                StringUtils.join(array, charSeparator, size - 1, size);
                StringUtils.join(array, stringSeparator, size - 1, size);
            }
        }

        int arbitraryStart = data.consumeInt();
        int arbitraryEnd = data.consumeInt();
        StringUtils.join(array, charSeparator, arbitraryStart, arbitraryEnd);
        StringUtils.join(array, stringSeparator, arbitraryStart, arbitraryEnd);
    }
}
```

</details>

---
## [46] ⚙️ harness-attempt · `attempt_022`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException: -168430091

---
## [47] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,842 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array = null;
        if (data.consumeBoolean()) {
            int maxLen = Math.min(16, Math.max(0, data.remainingBytes()));
            int len = maxLen == 0 ? 0 : data.consumeInt(0, maxLen);
            array = new Object[len];
            for (int i = 0; i < len; i++) {
                array[i] = makeElement(data);
            }
        }

        int len = array == null ? 0 : array.length;
        int startIndex = chooseIndex(data, len);
        int endIndex = chooseIndex(data, len);

        char charSeparator = (char) (data.consumeByte() & 0xFF);
        String stringSeparator;
        if (data.consumeBoolean()) {
            stringSeparator = null;
        } else if (data.consumeBoolean()) {
            stringSeparator = data.consumeAsciiString(32);
        } else {
            stringSeparator = data.consumeString(32);
        }

        switch (data.consumeInt(0, 5)) {
            case 0:
                StringUtils.join(array, charSeparator, startIndex, endIndex);
                break;
            case 1:
                StringUtils.join(array, stringSeparator, startIndex, endIndex);
                break;
            case 2:
                StringUtils.join(array, charSeparator, startIndex, endIndex);
                StringUtils.join(array, stringSeparator, startIndex, endIndex);
                break;
            case 3:
                StringUtils.join(array, stringSeparator, startIndex, endIndex);
                StringUtils.join(array, charSeparator, startIndex, endIndex);
                break;
            case 4:
                StringUtils.join(array, charSeparator, endIndex, startIndex);
                break;
            default:
                StringUtils.join(array, stringSeparator, endIndex, startIndex);
                break;
        }
    }

    private static int chooseIndex(FuzzedDataProvider data, int len) {
        switch (data.consumeInt(0, 11)) {
            case 0:
                return Integer.MIN_VALUE;
            case 1:
                return -2;
            case 2:
                return -1;
            case 3:
                return 0;
            case 4:
                return 1;
            case 5:
                return len - 1;
            case 6:
                return len;
            case 7:
                return len + 1;
            case 8:
                return len + 2;
            case 9:
                return Integer.MAX_VALUE;
            case 10:
                return data.consumeInt();
            default:
                int low = len - 2;
                int high = len + 2;
                if (low > high) {
                    int tmp = low;
                    low = high;
                    high = tmp;
                }
                return data.consumeInt(low, high);
        }
    }

    private static Object makeElement(FuzzedDataProvider data) {
        switch (data.consumeInt(0, 8)) {
            case 0:
                return null;
            case 1:
                return data.consumeString(32);
            case 2:
                return data.consumeAsciiString(32);
            case 3:
                return Integer.valueOf(data.consumeInt());
            case 4:
                return Long.valueOf(data.consumeInt());
            case 5:
                return Boolean.valueOf(data.consumeBoolean());
            case 6:
                return Character.valueOf((char) (data.consumeByte() & 0xFF));
            case 7:
                return data.consumeBytes(16);
            default:
                return new StringBuilder(data.consumeString(32));
        }
    }
}
```

</details>

---
## [48] ⚙️ harness-attempt · `attempt_023`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException: -2

---
## [49] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,700 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array;

        if (data.consumeBoolean()) {
            array = null;
        } else {
            int length = data.consumeInt(0, 32);
            array = new Object[length];
            for (int i = 0; i < length; i++) {
                switch (data.consumeInt(0, 6)) {
                    case 0:
                        array[i] = null;
                        break;
                    case 1:
                        array[i] = data.consumeString(32);
                        break;
                    case 2:
                        array[i] = data.consumeAsciiString(32);
                        break;
                    case 3:
                        array[i] = Integer.valueOf(data.consumeInt());
                        break;
                    case 4:
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                    case 5:
                        array[i] = Byte.valueOf(data.consumeByte());
                        break;
                    default:
                        array[i] = new String(data.consumeBytes(32));
                        break;
                }
            }
        }

        char charSeparator = (char) (data.consumeByte() & 0xFF);

        String stringSeparator;
        switch (data.consumeInt(0, 3)) {
            case 0:
                stringSeparator = null;
                break;
            case 1:
                stringSeparator = data.consumeString(32);
                break;
            case 2:
                stringSeparator = data.consumeAsciiString(32);
                break;
            default:
                stringSeparator = data.consumeRemainingAsString();
                break;
        }

        int startIndexAny = data.consumeInt();
        int endIndexAny = data.consumeInt();

        StringUtils.join(array, charSeparator, startIndexAny, endIndexAny);
        StringUtils.join(array, stringSeparator, startIndexAny, endIndexAny);

        int len = array == null ? 0 : array.length;

        int startIndexNear;
        switch (data.consumeInt(0, 7)) {
            case 0:
                startIndexNear = -1;
                break;
            case 1:
                startIndexNear = 0;
                break;
            case 2:
                startIndexNear = 1;
                break;
            case 3:
                startIndexNear = len - 1;
                break;
            case 4:
                startIndexNear = len;
                break;
            case 5:
                startIndexNear = len + 1;
                break;
            case 6:
                startIndexNear = Integer.MIN_VALUE;
                break;
            default:
                startIndexNear = Integer.MAX_VALUE;
                break;
        }

        int endIndexNear;
        switch (data.consumeInt(0, 7)) {
            case 0:
                endIndexNear = -1;
                break;
            case 1:
                endIndexNear = 0;
                break;
            case 2:
                endIndexNear = 1;
                break;
            case 3:
                endIndexNear = len - 1;
                break;
            case 4:
                endIndexNear = len;
                break;
            case 5:
                endIndexNear = len + 1;
                break;
            case 6:
                endIndexNear = Integer.MIN_VALUE;
                break;
            default:
                endIndexNear = Integer.MAX_VALUE;
                break;
        }

        StringUtils.join(array, charSeparator, startIndexNear, endIndexNear);
        StringUtils.join(array, stringSeparator, startIndexNear, endIndexNear);

        if (array != null) {
            int validStart = len == 0 ? 0 : data.consumeInt(0, len);
            int validEnd = len == 0 ? 0 : data.consumeInt(0, len);

            StringUtils.join(array, charSeparator, validStart, validEnd);
            StringUtils.join(array, stringSeparator, validStart, validEnd);

            StringUtils.join(array, charSeparator, 0, len);
            StringUtils.join(array, stringSeparator, 0, len);

            if (len > 0) {
                StringUtils.join(array, charSeparator, 0, 1);
                StringUtils.join(array, stringSeparator, 0, 1);
                StringUtils.join(array, charSeparator, len - 1, len);
                StringUtils.join(array, stringSeparator, len - 1, len);
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
- trigger: java.lang.ArrayIndexOutOfBoundsException: 17

---
## [51] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,084 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array;
        if (data.consumeBoolean()) {
            array = null;
        } else {
            int length = data.consumeInt(0, 32);
            array = new Object[length];
            for (int i = 0; i < length; i++) {
                switch (data.consumeInt(0, 8)) {
                    case 0:
                        array[i] = null;
                        break;
                    case 1:
                        array[i] = data.consumeString(32);
                        break;
                    case 2:
                        array[i] = data.consumeAsciiString(32);
                        break;
                    case 3:
                        array[i] = Integer.valueOf(data.consumeInt());
                        break;
                    case 4:
                        array[i] = Long.valueOf((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                        break;
                    case 5:
                        array[i] = Character.valueOf((char) (data.consumeByte() & 0xff));
                        break;
                    case 6:
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                    case 7:
                        array[i] = new String(data.consumeBytes(16));
                        break;
                    default:
                        array[i] = new StringBuilder(data.consumeAsciiString(16));
                        break;
                }
            }
        }

        int len = array == null ? 0 : array.length;

        int startIndex1;
        int endIndex1;
        switch (data.consumeInt(0, 7)) {
            case 0:
                startIndex1 = data.consumeInt();
                endIndex1 = data.consumeInt();
                break;
            case 1:
                startIndex1 = data.consumeInt(-len - 2, len + 2);
                endIndex1 = data.consumeInt(-len - 2, len + 2);
                break;
            case 2:
                startIndex1 = 0;
                endIndex1 = len;
                break;
            case 3:
                startIndex1 = len == 0 ? 0 : len - 1;
                endIndex1 = len;
                break;
            case 4:
                startIndex1 = len;
                endIndex1 = len + 1;
                break;
            case 5:
                startIndex1 = -1;
                endIndex1 = len;
                break;
            case 6:
                startIndex1 = 0;
                endIndex1 = len + data.consumeInt(0, 2);
                break;
            default:
                startIndex1 = data.consumeBoolean() ? Integer.MIN_VALUE : Integer.MAX_VALUE;
                endIndex1 = data.consumeBoolean() ? Integer.MIN_VALUE : Integer.MAX_VALUE;
                break;
        }

        int startIndex2;
        int endIndex2;
        switch (data.consumeInt(0, 7)) {
            case 0:
                startIndex2 = data.consumeInt();
                endIndex2 = data.consumeInt();
                break;
            case 1:
                startIndex2 = data.consumeInt(-len - 2, len + 2);
                endIndex2 = data.consumeInt(-len - 2, len + 2);
                break;
            case 2:
                startIndex2 = 0;
                endIndex2 = len;
                break;
            case 3:
                startIndex2 = len;
                endIndex2 = len;
                break;
            case 4:
                startIndex2 = 1;
                endIndex2 = 0;
                break;
            case 5:
                startIndex2 = 0;
                endIndex2 = len + 1;
                break;
            case 6:
                startIndex2 = -1;
                endIndex2 = -1;
                break;
            default:
                startIndex2 = data.consumeBoolean() ? Integer.MIN_VALUE : Integer.MAX_VALUE;
                endIndex2 = data.consumeBoolean() ? Integer.MIN_VALUE : Integer.MAX_VALUE;
                break;
        }

        char separatorChar = (char) (data.consumeByte() & 0xff);

        String separatorString;
        if (data.consumeBoolean()) {
            separatorString = null;
        } else if (data.consumeBoolean()) {
            separatorString = data.consumeString(32);
        } else if (data.consumeBoolean()) {
            separatorString = data.consumeAsciiString(32);
        } else {
            separatorString = data.consumeRemainingAsString();
        }

        if (data.consumeBoolean()) {
            StringUtils.join(array, separatorChar, startIndex1, endIndex1);
            StringUtils.join(array, separatorString, startIndex2, endIndex2);
        } else {
            StringUtils.join(array, separatorString, startIndex2, endIndex2);
            StringUtils.join(array, separatorChar, startIndex1, endIndex1);
        }
    }
}
```

</details>

---
## [52] ⚙️ harness-attempt · `attempt_025`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException: 11

---
## [53] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,544 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array;

        if (data.consumeBoolean()) {
            array = null;
        } else {
            int maxLen = Math.min(32, Math.max(0, data.remainingBytes()));
            int len = maxLen == 0 ? 0 : data.consumeInt(0, maxLen);
            array = new Object[len];
            for (int i = 0; i < len; i++) {
                int choice = data.consumeInt(0, 7);
                switch (choice) {
                    case 0:
                        array[i] = null;
                        break;
                    case 1:
                        array[i] = data.consumeString(32);
                        break;
                    case 2:
                        array[i] = data.consumeAsciiString(32);
                        break;
                    case 3:
                        array[i] = Integer.valueOf(data.consumeInt());
                        break;
                    case 4:
                        array[i] = Character.valueOf((char) (data.consumeByte() & 0xFF));
                        break;
                    case 5:
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                    case 6:
                        array[i] = new String(data.consumeBytes(16));
                        break;
                    default:
                        array[i] = data.consumeRemainingAsString();
                        break;
                }
            }
        }

        char charSeparator = (char) (data.consumeByte() & 0xFF);
        String stringSeparator;
        if (data.consumeBoolean()) {
            stringSeparator = null;
        } else if (data.consumeBoolean()) {
            stringSeparator = data.consumeString(32);
        } else {
            stringSeparator = data.consumeAsciiString(32);
        }

        int startIndex;
        int endIndex;

        if (array == null || data.consumeBoolean()) {
            startIndex = data.consumeInt();
            endIndex = data.consumeInt();
        } else {
            int len = array.length;
            int mode = data.consumeInt(0, 8);
            switch (mode) {
                case 0:
                    startIndex = 0;
                    endIndex = len;
                    break;
                case 1:
                    startIndex = 0;
                    endIndex = 0;
                    break;
                case 2:
                    startIndex = len;
                    endIndex = len;
                    break;
                case 3:
                    startIndex = -1;
                    endIndex = len;
                    break;
                case 4:
                    startIndex = 0;
                    endIndex = len + 1;
                    break;
                case 5:
                    startIndex = len == 0 ? 0 : len - 1;
                    endIndex = len;
                    break;
                case 6:
                    startIndex = len;
                    endIndex = 0;
                    break;
                case 7:
                    startIndex = data.consumeInt(-2, len + 2);
                    endIndex = data.consumeInt(-2, len + 2);
                    break;
                default:
                    startIndex = data.consumeInt();
                    endIndex = data.consumeInt();
                    break;
            }
        }

        StringUtils.join(array, charSeparator, startIndex, endIndex);
        StringUtils.join(array, stringSeparator, startIndex, endIndex);

        if (array != null) {
            int len = array.length;
            StringUtils.join(array, charSeparator, 0, len);
            StringUtils.join(array, stringSeparator, 0, len);

            if (len > 0) {
                StringUtils.join(array, charSeparator, 0, 1);
                StringUtils.join(array, stringSeparator, 0, 1);
                StringUtils.join(array, charSeparator, len - 1, len);
                StringUtils.join(array, stringSeparator, len - 1, len);
            }

            StringUtils.join(array, charSeparator, len, len);
            StringUtils.join(array, stringSeparator, len, len);
            StringUtils.join(array, charSeparator, 0, Math.max(0, len - 1));
            StringUtils.join(array, stringSeparator, 0, Math.max(0, len - 1));
        }
    }
}
```

</details>

---
## [54] ⚙️ harness-attempt · `attempt_026`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException: -168488704

---
## [55] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,002 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array;
        if (data.consumeBoolean()) {
            array = null;
        } else {
            int len = data.consumeInt(0, 32);
            array = new Object[len];
            for (int i = 0; i < len; i++) {
                switch (data.consumeInt(0, 8)) {
                    case 0:
                        array[i] = null;
                        break;
                    case 1:
                        array[i] = data.consumeString(32);
                        break;
                    case 2:
                        array[i] = data.consumeAsciiString(32);
                        break;
                    case 3:
                        array[i] = Integer.valueOf(data.consumeInt());
                        break;
                    case 4:
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                    case 5:
                        array[i] = Byte.valueOf(data.consumeByte());
                        break;
                    case 6:
                        array[i] = Character.valueOf((char) (data.consumeByte() & 0xFF));
                        break;
                    case 7:
                        array[i] = new StringBuilder(data.consumeString(32));
                        break;
                    default:
                        array[i] = data.consumeBytes(16);
                        break;
                }
            }
        }

        int len = array == null ? 0 : array.length;

        int rawA = data.consumeInt();
        int rawB = data.consumeInt();

        int startIndex;
        switch (data.consumeInt(0, 9)) {
            case 0:
                startIndex = rawA;
                break;
            case 1:
                startIndex = rawA % 64;
                break;
            case 2:
                startIndex = -1;
                break;
            case 3:
                startIndex = 0;
                break;
            case 4:
                startIndex = 1;
                break;
            case 5:
                startIndex = len == 0 ? 0 : len - 1;
                break;
            case 6:
                startIndex = len;
                break;
            case 7:
                startIndex = len + 1;
                break;
            case 8:
                startIndex = -len - 1;
                break;
            default:
                startIndex = data.consumeInt(-16, 16);
                break;
        }

        int endIndex;
        switch (data.consumeInt(0, 9)) {
            case 0:
                endIndex = rawB;
                break;
            case 1:
                endIndex = rawB % 64;
                break;
            case 2:
                endIndex = -1;
                break;
            case 3:
                endIndex = 0;
                break;
            case 4:
                endIndex = 1;
                break;
            case 5:
                endIndex = len == 0 ? 0 : len - 1;
                break;
            case 6:
                endIndex = len;
                break;
            case 7:
                endIndex = len + 1;
                break;
            case 8:
                endIndex = startIndex;
                break;
            default:
                endIndex = startIndex + data.consumeInt(-16, 16);
                break;
        }

        char charSeparator = (char) (data.consumeByte() & 0xFF);

        String stringSeparator;
        switch (data.consumeInt(0, 4)) {
            case 0:
                stringSeparator = null;
                break;
            case 1:
                stringSeparator = "";
                break;
            case 2:
                stringSeparator = String.valueOf((char) (data.consumeByte() & 0xFF));
                break;
            case 3:
                stringSeparator = data.consumeAsciiString(16);
                break;
            default:
                stringSeparator = data.consumeString(16);
                break;
        }

        StringUtils.join(array, charSeparator, startIndex, endIndex);
        StringUtils.join(array, stringSeparator, startIndex, endIndex);

        if (array != null) {
            StringUtils.join(array, charSeparator, 0, len);
            StringUtils.join(array, stringSeparator, 0, len);
            StringUtils.join(array, charSeparator, len, len);
            StringUtils.join(array, stringSeparator, len, len);

            if (len > 0) {
                StringUtils.join(array, charSeparator, 0, 1);
                StringUtils.join(array, stringSeparator, 0, 1);
                StringUtils.join(array, charSeparator, len - 1, len);
                StringUtils.join(array, stringSeparator, len - 1, len);
            }
        }
    }
}
```

</details>

---
## [56] ⚙️ harness-attempt · `attempt_027`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException: -1482184793

---
## [57] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,904 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array = null;
        if (data.consumeBoolean()) {
            int len = data.consumeInt(0, 32);
            array = new Object[len];
            for (int i = 0; i < len; i++) {
                if (data.consumeBoolean()) {
                    array[i] = null;
                    continue;
                }
                switch (data.consumeInt(0, 8)) {
                    case 0:
                        array[i] = data.consumeString(32);
                        break;
                    case 1:
                        array[i] = data.consumeAsciiString(32);
                        break;
                    case 2:
                        array[i] = Integer.valueOf(data.consumeInt());
                        break;
                    case 3:
                        array[i] = Long.valueOf((long) data.consumeInt());
                        break;
                    case 4:
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                    case 5:
                        array[i] = Character.valueOf((char) (data.consumeByte() & 0xFF));
                        break;
                    case 6:
                        array[i] = data.consumeBytes(16);
                        break;
                    case 7:
                        array[i] = new StringBuilder(data.consumeString(32));
                        break;
                    default:
                        array[i] = data.consumeRemainingAsString();
                        break;
                }
            }
        }

        char charSeparator = (char) (data.consumeByte() & 0xFF);
        String stringSeparator;
        if (data.consumeBoolean()) {
            stringSeparator = null;
        } else if (data.consumeBoolean()) {
            stringSeparator = data.consumeString(16);
        } else {
            stringSeparator = data.consumeAsciiString(16);
        }

        StringUtils.join((Object[]) null, charSeparator, data.consumeInt(), data.consumeInt());
        StringUtils.join((Object[]) null, stringSeparator, data.consumeInt(), data.consumeInt());

        if (array == null) {
            return;
        }

        int len = array.length;

        int safeStart = data.consumeInt(0, len);
        int safeEnd = data.consumeInt(0, len);
        if (data.consumeBoolean() && safeStart > safeEnd) {
            int tmp = safeStart;
            safeStart = safeEnd;
            safeEnd = tmp;
        }

        StringUtils.join(array, charSeparator, 0, len);
        StringUtils.join(array, stringSeparator, 0, len);
        StringUtils.join(array, charSeparator, len, len);
        StringUtils.join(array, stringSeparator, len, len);
        StringUtils.join(array, charSeparator, 0, 0);
        StringUtils.join(array, stringSeparator, 0, 0);
        StringUtils.join(array, charSeparator, safeStart, safeEnd);
        StringUtils.join(array, stringSeparator, safeStart, safeEnd);

        int startIndex;
        int endIndex;
        if (data.consumeBoolean()) {
            startIndex = data.consumeInt();
            endIndex = data.consumeInt();
        } else {
            int span = len + 4;
            startIndex = data.consumeInt(-span, span);
            endIndex = data.consumeInt(-span, span);
        }

        if (data.consumeBoolean()) {
            StringUtils.join(array, charSeparator, startIndex, endIndex);
            StringUtils.join(array, stringSeparator, startIndex, endIndex);
        } else {
            StringUtils.join(array, stringSeparator, startIndex, endIndex);
            StringUtils.join(array, charSeparator, startIndex, endIndex);
        }
    }
}
```

</details>

---
## [58] ⚙️ harness-attempt · `attempt_028`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException: 1

---
## [59] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,663 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array;
        if (data.consumeBoolean()) {
            array = null;
        } else {
            int length = data.consumeInt(0, 16);
            array = new Object[length];
            for (int i = 0; i < length; i++) {
                switch (data.consumeInt(0, 8)) {
                    case 0:
                        array[i] = null;
                        break;
                    case 1:
                        array[i] = data.consumeString(32);
                        break;
                    case 2:
                        array[i] = data.consumeAsciiString(32);
                        break;
                    case 3:
                        array[i] = Integer.valueOf(data.consumeInt());
                        break;
                    case 4:
                        array[i] = Long.valueOf((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                        break;
                    case 5:
                        array[i] = Boolean.valueOf(data.consumeBoolean());
                        break;
                    case 6:
                        array[i] = Character.valueOf((char) (data.consumeByte() & 0xff));
                        break;
                    case 7:
                        array[i] = new StringBuilder(data.consumeString(32));
                        break;
                    default:
                        array[i] = data.consumeBytes(16);
                        break;
                }
            }
        }

        char charSeparator = (char) (data.consumeByte() & 0xff);
        String stringSeparator = data.consumeBoolean() ? null : data.consumeString(16);

        int basis = array == null ? data.consumeInt(0, 16) : array.length;
        int startIndex = data.consumeBoolean() ? data.consumeInt() : data.consumeInt(-basis - 4, basis + 4);
        int endIndex = data.consumeBoolean() ? data.consumeInt() : data.consumeInt(-basis - 4, basis + 4);

        if (data.consumeBoolean()) {
            StringUtils.join(array, charSeparator, startIndex, endIndex);
            StringUtils.join(array, stringSeparator, startIndex, endIndex);
        } else {
            StringUtils.join(array, stringSeparator, startIndex, endIndex);
            StringUtils.join(array, charSeparator, startIndex, endIndex);
        }

        if (array != null) {
            int len = array.length;

            int safeStart = data.consumeInt(0, len);
            int safeEnd = data.consumeInt(0, len);
            if (data.consumeBoolean() && safeStart > safeEnd) {
                int tmp = safeStart;
                safeStart = safeEnd;
                safeEnd = tmp;
            }

            StringUtils.join(array, charSeparator, safeStart, safeEnd);
            StringUtils.join(array, stringSeparator, safeStart, safeEnd);

            StringUtils.join(array, charSeparator, 0, len);
            StringUtils.join(array, stringSeparator, 0, len);

            StringUtils.join(array, charSeparator, len, len);
            StringUtils.join(array, stringSeparator, len, len);

            if (len > 0) {
                StringUtils.join(array, charSeparator, len - 1, len);
                StringUtils.join(array, stringSeparator, len - 1, len);
            }
        } else {
            StringUtils.join((Object[]) null, charSeparator, 0, 0);
            StringUtils.join((Object[]) null, stringSeparator, 0, 0);
        }
    }
}
```

</details>

---
## [60] ⚙️ harness-attempt · `attempt_029`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException: -57300

---
## [61] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,400 chars)</summary>

```
package org.apache.commons.lang3;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Object[] array = null;
        if (data.consumeBoolean()) {
            int length = data.consumeInt(0, 32);
            array = new Object[length];
            for (int i = 0; i < length; i++) {
                switch (data.consumeInt(0, 5)) {
                    case 0:
                        array[i] = null;
                        break;
                    case 1:
                        array[i] = data.consumeString(32);
                        break;
                    case 2:
                        array[i] = data.consumeAsciiString(32);
                        break;
                    case 3:
                        array[i] = Integer.valueOf(data.consumeInt());
                        break;
                    case 4:
                        array[i] = Byte.valueOf(data.consumeByte());
                        break;
                    default:
                        array[i] = data.consumeBytes(16);
                        break;
                }
            }
        }

        char charSeparator = (char) (data.consumeByte() & 0xFF);
        String stringSeparator;
        if (data.consumeBoolean()) {
            stringSeparator = null;
        } else if (data.consumeBoolean()) {
            stringSeparator = data.consumeAsciiString(16);
        } else {
            stringSeparator = data.consumeString(16);
        }

        int rawStart1 = data.consumeInt();
        int rawEnd1 = data.consumeInt();
        int rawStart2 = data.consumeInt();
        int rawEnd2 = data.consumeInt();

        int boundaryStart = 0;
        int boundaryEnd = 0;
        if (array != null) {
            switch (data.consumeInt(0, 8)) {
                case 0:
                    boundaryStart = -1;
                    break;
                case 1:
                    boundaryStart = 0;
                    break;
                case 2:
                    boundaryStart = array.length == 0 ? 0 : array.length - 1;
                    break;
                case 3:
                    boundaryStart = array.length;
                    break;
                case 4:
                    boundaryStart = array.length + 1;
                    break;
                default:
                    boundaryStart = rawStart1;
                    break;
            }
            switch (data.consumeInt(0, 8)) {
                case 0:
                    boundaryEnd = -1;
                    break;
                case 1:
                    boundaryEnd = 0;
                    break;
                case 2:
                    boundaryEnd = array.length == 0 ? 0 : array.length - 1;
                    break;
                case 3:
                    boundaryEnd = array.length;
                    break;
                case 4:
                    boundaryEnd = array.length + 1;
                    break;
                default:
                    boundaryEnd = rawEnd1;
                    break;
            }
        } else {
            boundaryStart = rawStart1;
            boundaryEnd = rawEnd1;
        }

        StringUtils.join(array, charSeparator, rawStart1, rawEnd1);
        StringUtils.join(array, stringSeparator, rawStart2, rawEnd2);
        StringUtils.join(array, charSeparator, boundaryStart, boundaryEnd);
        StringUtils.join(array, stringSeparator, boundaryStart, boundaryEnd);

        if (array != null) {
            StringUtils.join(array, charSeparator, 0, array.length);
            StringUtils.join(array, stringSeparator, 0, array.length);
            StringUtils.join(array, charSeparator, 0, 0);
            StringUtils.join(array, stringSeparator, 0, 0);
            StringUtils.join(array, charSeparator, array.length, array.length);
            StringUtils.join(array, stringSeparator, array.length, array.length);
            if (array.length > 0) {
                StringUtils.join(array, charSeparator, 0, 1);
                StringUtils.join(array, stringSeparator, 0, 1);
                StringUtils.join(array, charSeparator, array.length - 1, array.length);
                StringUtils.join(array, stringSeparator, array.length - 1, array.length);
            }
        }
    }
}
```

</details>

---
## [62] ⚙️ harness-attempt · `attempt_030`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.ArrayIndexOutOfBoundsException: -1

---
## [63] ⚙️ coverage · `jazzer coverage dumps`
**output:** **30/30 dump(s) written under /datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/cov; 30 raw fuzz log(s)**
<details><summary>▸ detail (~15,166 chars)</summary>

- detail: {'dumps': [{'harness': 'attempt_001', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_001_compiled.exec', 'exists': True, 'bytes': 317}, {'harness': 'attempt_002', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_002_compiled.exec', 'exists': True, 'bytes': 315}, {'harness': 'attempt_003', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_003_compiled.exec', 'exists': True, 'bytes': 317}, {'harness': 'attempt_004', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_004_compiled.exec', 'exists': True, 'bytes': 316}, {'harness': 'attempt_005', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_005_compiled.exec', 'exists': True, 'bytes': 317}, {'harness': 'attempt_006', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_006_compiled.exec', 'exists': True, 'bytes': 318}, {'harness': 'attempt_007', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_007_compiled.exec', 'exists': True, 'bytes': 318}, {'harness': 'attempt_008', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_008_compiled.exec', 'exists': True, 'bytes': 318}, {'harness': 'attempt_009', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_009_compiled.exec', 'exists': True, 'bytes': 315}, {'harness': 'attempt_010', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_010_compiled.exec', 'exists': True, 'bytes': 316}, {'harness': 'attempt_011', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_011_compiled.exec', 'exists': True, 'bytes': 315}, {'harness': 'attempt_012', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_012_compiled.exec', 'exists': True, 'bytes': 316}, {'harness': 'attempt_013', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_013_compiled.exec', 'exists': True, 'bytes': 317}, {'harness': 'attempt_014', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_014_compiled.exec', 'exists': True, 'bytes': 317}, {'harness': 'attempt_015', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_015_compiled.exec', 'exists': True, 'bytes': 317}, {'harness': 'attempt_016', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_016_compiled.exec', 'exists': True, 'bytes': 315}, {'harness': 'attempt_017', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_017_compiled.exec', 'exists': True, 'bytes': 315}, {'harness': 'attempt_018', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_018_compiled.exec', 'exists': True, 'bytes': 316}, {'harness': 'attempt_019', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_019_compiled.exec', 'exists': True, 'bytes': 317}, {'harness': 'attempt_020', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_020_compiled.exec', 'exists': True, 'bytes': 317}, {'harness': 'attempt_021', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_021_compiled.exec', 'exists': True, 'bytes': 317}, {'harness': 'attempt_022', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_022_compiled.exec', 'exists': True, 'bytes': 316}, {'harness': 'attempt_023', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_023_compiled.exec', 'exists': True, 'bytes': 317}, {'harness': 'attempt_024', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_024_compiled.exec', 'exists': True, 'bytes': 318}, {'harness': 'attempt_025', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_025_compiled.exec', 'exists': True, 'bytes': 318}, {'harness': 'attempt_026', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_026_compiled.exec', 'exists': True, 'bytes': 317}, {'harness': 'attempt_027', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_027_compiled.exec', 'exists': True, 'bytes': 318}, {'harness': 'attempt_028', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_028_compiled.exec', 'exists': True, 'bytes': 317}, {'harness': 'attempt_029', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_029_compiled.exec', 'exists': True, 'bytes': 317}, {'harness': 'attempt_030', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/cov/attempt_030_compiled.exec', 'exists': True, 'bytes': 318}], 'outputs': ['/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_001_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_002_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_003_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_004_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_005_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_006_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_007_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_008_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_009_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_010_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_011_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_012_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_013_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_014_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_015_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_016_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_017_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_018_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_019_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_020_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_021_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_022_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_023_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_024_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_025_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_026_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_027_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_028_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_029_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/fuzz_out/attempt_030_compiled.txt'], 'classpath': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/cov/classpath.json', 'dir': '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/cov', 'compiled_attempts': ['attempt_001', 'attempt_002', 'attempt_003', 'attempt_004', 'attempt_005', 'attempt_006', 'attempt_007', 'attempt_008', 'attempt_009', 'attempt_010', 'attempt_011', 'attempt_012', 'attempt_013', 'attempt_014', 'attempt_015', 'attempt_016', 'attempt_017', 'attempt_018', 'attempt_019', 'attempt_020', 'attempt_021', 'attempt_022', 'attempt_023', 'attempt_024', 'attempt_025', 'attempt_026', 'attempt_027', 'attempt_028', 'attempt_029', 'attempt_030'], 'accepted_attempts': ['attempt_001', 'attempt_002', 'attempt_003', 'attempt_004', 'attempt_005', 'attempt_006', 'attempt_007', 'attempt_008', 'attempt_009', 'attempt_010', 'attempt_011', 'attempt_012', 'attempt_013', 'attempt_014', 'attempt_015', 'attempt_016', 'attempt_017', 'attempt_018', 'attempt_019', 'attempt_020', 'attempt_021', 'attempt_022', 'attempt_023', 'attempt_024', 'attempt_025', 'attempt_026', 'attempt_027', 'attempt_028', 'attempt_029', 'attempt_030'], 'sources': ['/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_001.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_002.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_003.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_004.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_005.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_006.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_007.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_008.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_009.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_010.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_011.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_012.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_013.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_014.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_015.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_016.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_017.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_018.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_019.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_020.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_021.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_022.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_023.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_024.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_025.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_026.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_027.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_028.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_029.java', '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/003_patch1-Lang-20-Arja_c_rep001/harness_src/attempt_030.java']}

</details>
