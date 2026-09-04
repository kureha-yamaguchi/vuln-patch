# Pipeline trace — Lang-16

**Code:** `unknown`

**Patch label:** overfitting  *(the patch under analysis is a known-OVERFIT fix — the pipeline is not told this)*

**Outcome:** OVERFIT CAUGHT (a harness fired on the patched build). [evaluated; 3 harness(es) fuzzed on the patched build; campaign converged=True]

**Patch under analysis:**
```diff
--- /src/main/java/org/apache/commons/lang3/math/NumberUtils.java
+++ /src/main/java/org/apache/commons/lang3/math/NumberUtils.java
@@ -542,8 +542,7 @@
                     }
                     //$FALL-THROUGH$
                 default :
-                    throw new NumberFormatException(str + " is not a valid number.");
-
+                	return createInteger(str);
             }
         } else {
             //User doesn't have a preference on the return type, so let's start
```

17 sequential steps — 4 LLM calls, 13 deterministic. Read top to bottom.

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
<details><summary>▸ output — FailureTest(test_class='org.apache.commons.lang3.math.NumberUtilsTest', test_method='testCreateNumbe (~4,384 chars)</summary>

**output:** 
```json
[
  "FailureTest(test_class='org.apache.commons.lang3.math.NumberUtilsTest', test_method='testCreateNumber', source_path='/tmp/d4j/Lang_16_buggy/src/test/java/org/apache/commons/lang3/math/NumberUtilsTest.java', method_source='    @Test\\n    public void testCreateNumber() {\\n        // a lot of things can go wrong\\n        assertEquals(\"createNumber(String) 1 failed\", Float.valueOf(\"1234.5\"), NumberUtils.createNumber(\"1234.5\"));\\n        assertEquals(\"createNumber(String) 2 failed\", Integer.valueOf(\"12345\"), NumberUtils.createNumber(\"12345\"));\\n        assertEquals(\"createNumber(String) 3 failed\", Double.valueOf(\"1234.5\"), NumberUtils.createNumber(\"1234.5D\"));\\n        assertEquals(\"createNumber(String) 3 failed\", Double.valueOf(\"1234.5\"), NumberUtils.createNumber(\"1234.5d\"));\\n        assertEquals(\"createNumber(String) 4 failed\", Float.valueOf(\"1234.5\"), NumberUtils.createNumber(\"1234.5F\"));\\n        assertEquals(\"createNumber(String) 4 failed\", Float.valueOf(\"1234.5\"), NumberUtils.createNumber(\"1234.5f\"));\\n        assertEquals(\"createNumber(String) 5 failed\", Long.valueOf(Integer.MAX_VALUE + 1L), NumberUtils.createNumber(\"\"\\n            + (Integer.MAX_VALUE + 1L)));\\n        assertEquals(\"createNumber(String) 6 failed\", Long.valueOf(12345), NumberUtils.createNumber(\"12345L\"));\\n        assertEquals(\"createNumber(String) 6 failed\", Long.valueOf(12345), NumberUtils.createNumber(\"12345l\"));\\n        assertEquals(\"createNumber(String) 7 failed\", Float.valueOf(\"-1234.5\"), NumberUtils.createNumber(\"-1234.5\"));\\n        assertEquals(\"createNumber(String) 8 failed\", Integer.valueOf(\"-12345\"), NumberUtils.createNumber(\"-12345\"));\\n        assertTrue(\"createNumber(String) 9a failed\", 0xFADE == NumberUtils.createNumber(\"0xFADE\").intValue());\\n        assertTrue(\"createNumber(String) 9b failed\", 0xFADE == NumberUtils.createNumber(\"0Xfade\").intValue());\\n        assertTrue(\"createNumber(String) 10a failed\", -0xFADE == NumberUtils.createNumber(\"-0xFADE\").intValue());\\n        assertTrue(\"createNumber(String) 10b failed\", -0xFADE == NumberUtils.createNumber(\"-0Xfade\").intValue());\\n        assertEquals(\"createNumber(String) 11 failed\", Double.valueOf(\"1.1E200\"), NumberUtils.createNumber(\"1.1E200\"));\\n        assertEquals(\"createNumber(String) 12 failed\", Float.valueOf(\"1.1E20\"), NumberUtils.createNumber(\"1.1E20\"));\\n        assertEquals(\"createNumber(String) 13 failed\", Double.valueOf(\"-1.1E200\"), NumberUtils.createNumber(\"-1.1E200\"));\\n        assertEquals(\"createNumber(String) 14 failed\", Double.valueOf(\"1.1E-200\"), NumberUtils.createNumber(\"1.1E-200\"));\\n        assertEquals(\"createNumber(null) failed\", null, NumberUtils.createNumber(null));\\n        assertEquals(\"createNumber(String) failed\", new BigInteger(\"12345678901234567890\"), NumberUtils\\n                .createNumber(\"12345678901234567890L\"));\\n\\n        // jdk 1.2 doesn\\'t support this. unsure about jdk 1.2.2\\n        if (SystemUtils.isJavaVersionAtLeast(JAVA_1_3)) {\\n            assertEquals(\"createNumber(String) 15 failed\", new BigDecimal(\"1.1E-700\"), NumberUtils\\n                    .createNumber(\"1.1E-700F\"));\\n        }\\n        assertEquals(\"createNumber(String) 16 failed\", Long.valueOf(\"10\" + Integer.MAX_VALUE), NumberUtils\\n                .createNumber(\"10\" + Integer.MAX_VALUE + \"L\"));\\n        assertEquals(\"createNumber(String) 17 failed\", Long.valueOf(\"10\" + Integer.MAX_VALUE), NumberUtils\\n                .createNumber(\"10\" + Integer.MAX_VALUE));\\n        assertEquals(\"createNumber(String) 18 failed\", new BigInteger(\"10\" + Long.MAX_VALUE), NumberUtils\\n                .createNumber(\"10\" + Long.MAX_VALUE));\\n\\n        // LANG-521\\n        assertEquals(\"createNumber(String) LANG-521 failed\", Float.valueOf(\"2.\"), NumberUtils.createNumber(\"2.\"));\\n\\n        // LANG-638\\n        assertFalse(\"createNumber(String) succeeded\", checkCreateNumber(\"1eE\"));\\n\\n        // LANG-693\\n        assertEquals(\"createNumber(String) LANG-693 failed\", Double.valueOf(Double.MAX_VALUE), NumberUtils\\n                    .createNumber(\"\" + Double.MAX_VALUE));\\n    }', exception_type='java.lang.NumberFormatException', failure_message=None, support_source=None)"
]
```

</details>

---
## [1] ⚙️ test-context (H1/H2)
**output:** 
```json
[
  {
    "test": "org.apache.commons.lang3.math.NumberUtilsTest::testCreateNumber",
    "failure_message": "--- org.apache.commons.lang3.math.NumberUtilsTest::testCreateNumber\njava.lang.NumberFormatException: 0Xfade is not a valid number.",
    "support_chars": 344
  }
]
```

---
## [2] ⚙️ analysis (TargetAnalyzer)
<details><summary>▸ output — modified_files": [ (~17,791 chars)</summary>

**output:** 
```json
{
  "modified_files": [
    "src/main/java/org/apache/commons/lang3/math/NumberUtils.java"
  ],
  "patch_text": "--- /src/main/java/org/apache/commons/lang3/math/NumberUtils.java\n+++ /src/main/java/org/apache/commons/lang3/math/NumberUtils.java\n@@ -542,8 +542,7 @@\n                     }\n                     //$FALL-THROUGH$\n                 default :\n-                    throw new NumberFormatException(str + \" is not a valid number.\");\n-\n+                \treturn createInteger(str);\n             }\n         } else {\n             //User doesn't have a preference on the return type, so let's start\n",
  "functions": [
    {
      "func_name": "createNumber",
      "func_signature": "public static Number createNumber(String str) throws NumberFormatException",
      "func_source": "    public static Number createNumber(String str) throws NumberFormatException {\n        if (str == null) {\n            return null;\n        }\n        if (StringUtils.isBlank(str)) {\n            throw new NumberFormatException(\"A blank string is not a valid number\");\n        }  \n        if (str.startsWith(\"--\")) {\n            // this is protection for poorness in java.lang.BigDecimal.\n            // it accepts this as a legal value, but it does not appear \n            // to be in specification of class. OS X Java parses it to \n            // a wrong value.\n            return null;\n        }\n        if (str.startsWith(\"0x\") || str.startsWith(\"-0x\")) {\n            return createInteger(str);\n        }   \n        char lastChar = str.charAt(str.length() - 1);\n        String mant;\n        String dec;\n        String exp;\n        int decPos = str.indexOf('.');\n        int expPos = str.indexOf('e') + str.indexOf('E') + 1;\n\n        if (decPos > -1) {\n\n            if (expPos > -1) {\n                if (expPos < decPos || expPos > str.length()) {\n                    throw new NumberFormatException(str + \" is not a valid number.\");\n                }\n                dec = str.substring(decPos + 1, expPos);\n            } else {\n                dec = str.substring(decPos + 1);\n            }\n            mant = str.substring(0, decPos);\n        } else {\n            if (expPos > -1) {\n                if (expPos > str.length()) {\n                    throw new NumberFormatException(str + \" is not a valid number.\");\n                }\n                mant = str.substring(0, expPos);\n            } else {\n                mant = str;\n            }\n            dec = null;\n        }\n        if (!Character.isDigit(lastChar) && lastChar != '.') {\n            if (expPos > -1 && expPos < str.length() - 1) {\n                exp = str.substring(expPos + 1, str.length() - 1);\n            } else {\n                exp = null;\n            }\n            //Requesting a specific type..\n            String numeric = str.substring(0, str.length() - 1);\n            boolean allZeros = isAllZeros(mant) && isAllZeros(exp);\n            switch (lastChar) {\n                case 'l' :\n                case 'L' :\n                    if (dec == null\n                        && exp == null\n                        && (numeric.charAt(0) == '-' && isDigits(numeric.substring(1)) || isDigits(numeric))) {\n                        try {\n                            return createLong(numeric);\n                        } catch (NumberFormatException nfe) { // NOPMD\n                            // Too big for a long\n                        }\n                        return createBigInteger(numeric);\n\n                    }\n                    throw new NumberFormatException(str + \" is not a valid number.\");\n                case 'f' :\n                case 'F' :\n                    try {\n                        Float f = NumberUtils.createFloat(numeric);\n                        if (!(f.isInfinite() || (f.floatValue() == 0.0F && !allZeros))) {\n                            //If it's too big for a float or the float value = 0 and the string\n                            //has non-zeros in it, then float does not have the precision we want\n                            return f;\n                        }\n\n                    } catch (NumberFormatException nfe) { // NOPMD\n                        // ignore the bad number\n                    }\n                    //$FALL-THROUGH$\n                case 'd' :\n                case 'D' :\n                    try {\n                        Double d = NumberUtils.createDouble(numeric);\n                        if (!(d.isInfinite() || (d.floatValue() == 0.0D && !allZeros))) {\n                            return d;\n                        }\n                    } catch (NumberFormatException nfe) { // NOPMD\n                        // ignore the bad number\n                    }\n                    try {\n                        return createBigDecimal(numeric);\n                    } catch (NumberFormatException e) { // NOPMD\n                        // ignore the bad number\n                    }\n                    //$FALL-THROUGH$\n                default :\n                    throw new NumberFormatException(str + \" is not a valid number.\");\n\n            }\n        } else {\n            //User doesn't have a preference on the return type, so let's start\n            //small and go from there...\n            if (expPos > -1 && expPos < str.length() - 1) {\n                exp = str.substring(expPos + 1, str.length());\n            } else {\n                exp = null;\n            }\n            if (dec == null && exp == null) {\n                //Must be an int,long,bigint\n                try {\n                    return createInteger(str);\n                } catch (NumberFormatException nfe) { // NOPMD\n                    // ignore the bad number\n                }\n                try {\n                    return createLong(str);\n                } catch (NumberFormatException nfe) { // NOPMD\n                    // ignore the bad number\n                }\n                return createBigInteger(str);\n\n            } else {\n                //Must be a float,double,BigDec\n                boolean allZeros = isAllZeros(mant) && isAllZeros(exp);\n                try {\n                    Float f = createFloat(str);\n                    if (!(f.isInfinite() || (f.floatValue() == 0.0F && !allZeros))) {\n                        return f;\n                    }\n                } catch (NumberFormatException nfe) { // NOPMD\n                    // ignore the bad number\n                }\n                try {\n                    Double d = createDouble(str);\n                    if (!(d.isInfinite() || (d.doubleValue() == 0.0D && !allZeros))) {\n                        return d;\n                    }\n                } catch (NumberFormatException nfe) { // NOPMD\n                    // ignore the bad number\n                }\n\n                return createBigDecimal(str);\n\n            }\n        }\n    }",
      "func_class": "NumberUtils",
      "func_class_fq": "org.apache.commons.lang3.math.NumberUtils",
      "func_param_types": [
        "String"
      ],
      "fi_name": "[org.apache.commons.lang3.math.NumberUtils].createNumber(String)",
      "overload_types": [
        [
          "String"
        ]
      ],
      "xrefs": [
        "@Test\n    public void testCreateNumber() {\n        // a lot of things can go wrong\n        assertEquals(\"createNumber(String) 1 failed\", Float.valueOf(\"1234.5\"), NumberUtils.createNumber(\"1234.5\"));\n        assertEquals(\"createNumber(String) 2 failed\", Integer.valueOf(\"12345\"), NumberUtils.createNumber(\"12345\"));\n        assertEquals(\"createNumber(String) 3 failed\", Double.valueOf(\"1234.5\"), NumberUtils.createNumber(\"1234.5D\"));\n        assertEquals(\"createNumber(String) 3 failed\", Double.valueOf(\"1234.5\"), NumberUtils.createNumber(\"1234.5d\"));\n        assertEquals(\"createNumber(String) 4 failed\", Float.valueOf(\"1234.5\"), NumberUtils.createNumber(\"1234.5F\"));\n        assertEquals(\"createNumber(String) 4 failed\", Float.valueOf(\"1234.5\"), NumberUtils.createNumber(\"1234.5f\"));\n        assertEquals(\"createNumber(String) 5 failed\", Long.valueOf(Integer.MAX_VALUE + 1L), NumberUtils.createNumber(\"\"\n            + (Integer.MAX_VALUE + 1L)));\n        assertEquals(\"createNumber(String) 6 failed\", Long.valueOf(12345), NumberUtils.createNumber(\"12345L\"));\n        assertEquals(\"createNumber(String) 6 failed\", Long.valueOf(12345), NumberUtils.createNumber(\"12345l\"));\n        assertEquals(\"createNumber(String) 7 failed\", Float.valueOf(\"-1234.5\"), NumberUtils.createNumber(\"-1234.5\"));\n        assertEquals(\"createNumber(String) 8 failed\", Integer.valueOf(\"-12345\"), NumberUtils.createNumber(\"-12345\"));\n        assertTrue(\"createNumber(String) 9a failed\", 0xFADE == NumberUtils.createNumber(\"0xFADE\").intValue());\n        assertTrue(\"createNumber(String) 9b failed\", 0xFADE == NumberUtils.createNumber(\"0Xfade\").intValue());\n        assertTrue(\"createNumber(String) 10a failed\", -0xFADE == NumberUtils.createNumber(\"-0xFADE\").intValue());\n        assertTrue(\"createNumber(String) 10b failed\", -0xFADE == NumberUtils.createNumber(\"-0Xfade\").intValue());\n        assertEquals(\"createNumber(String) 11 failed\", Double.valueOf(\"1.1E200\"), NumberUtils.createNumber(\"1.1E200\"));\n        assertEquals(\"createNumber(String) 12 failed\", Float.valueOf(\"1.1E20\"), NumberUtils.createNumber(\"1.1E20\"));\n        assertEquals(\"createNumber(String) 13 failed\", Double.valueOf(\"-1.1E200\"), NumberUtils.createNumber(\"-1.1E200\"));\n        assertEquals(\"createNumber(String) 14 failed\", Double.valueOf(\"1.1E-200\"), NumberUtils.createNumber(\"1.1E-200\"));\n        assertEquals(\"createNumber(null) failed\", null, NumberUtils.createNumber(null));\n        assertEquals(\"createNumber(String) failed\", new BigInteger(\"12345678901234567890\"), NumberUtils\n                .createNumber(\"12345678901234567890L\"));\n\n        // jdk 1.2 doesn't support this. unsure about jdk 1.2.2\n        if (SystemUtils.isJavaVersionAtLeast(JAVA_1_3)) {\n            assertEquals(\"createNumber(String) 15 failed\", new BigDecimal(\"1.1E-700\"), NumberUtils\n                    .createNumber(\"1.1E-700F\"));\n        }\n        assertEquals(\"createNumber(String) 16 failed\", Long.valueOf(\"10\" + Integer.MAX_VALUE), NumberUtils\n                .createNumber(\"10\" + Integer.MAX_VALUE + \"L\"));\n        assertEquals(\"createNumber(String) 17 failed\", Long.valueOf(\"10\" + Integer.MAX_VALUE), NumberUtils\n                .createNumber(\"10\" + Integer.MAX_VALUE));\n        assertEquals(\"createNumber(String) 18 failed\", new BigInteger(\"10\" + Long.MAX_VALUE), NumberUtils\n                .createNumber(\"10\" + Long.MAX_VALUE));\n\n        // LANG-521\n        assertEquals(\"createNumber(String) LANG-521 failed\", Float.valueOf(\"2.\"), NumberUtils.createNumber(\"2.\"));\n\n        // LANG-638\n        assertFalse(\"createNumber(String) succeeded\", checkCreateNumber(\"1eE\"));\n\n        // LANG-693\n        assertEquals(\"createNumber(String) LANG-693 failed\", Double.valueOf(Double.MAX_VALUE), NumberUtils\n                    .createNumber(\"\" + Double.MAX_VALUE));\n    }",
        "private boolean checkCreateNumber(String val) {\n        try {\n            Object obj = NumberUtils.createNumber(val);\n            if (obj == null) {\n                return false;\n            }\n            return true;\n        } catch (NumberFormatException e) {\n            return false;\n       }\n    }",
        "@Test\n    public void testLang300() {\n        NumberUtils.createNumber(\"-1l\");\n        NumberUtils.createNumber(\"01l\");\n        NumberUtils.createNumber(\"1l\");\n    }"
      ],
      "reachable": [
        "[org.apache.commons.lang3.StringUtils].isBlank(String)",
        "[NumberFormatException].<init>(String)",
        "[String].startsWith(String)",
        "[org.apache.commons.lang3.math.NumberUtils].createInteger(String)",
        "[String].length()",
        "[String].charAt(int)",
        "[String].indexOf(char)",
        "[String].substring(int,org.apache.commons.lang3.math.NumberUtils)",
        "[String].substring(int)",
        "Character.isDigit(org.apache.commons.lang3.math.NumberUtils)",
        "[String].substring(int,int)",
        "[org.apache.commons.lang3.math.NumberUtils].isAllZeros(org.apache.commons.lang3.math.NumberUtils)",
        "[org.apache.commons.lang3.math.NumberUtils].charAt(int)",
        "[org.apache.commons.lang3.math.NumberUtils].substring(int)",
        "[org.apache.commons.lang3.math.NumberUtils].isDigits(org.apache.commons.lang3.math.NumberUtils)",
        "[org.apache.commons.lang3.math.NumberUtils].createLong(org.apache.commons.lang3.math.NumberUtils)",
        "[org.apache.commons.lang3.math.NumberUtils].createBigInteger(org.apache.commons.lang3.math.NumberUtils)",
        "[org.apache.commons.lang3.math.NumberUtils].createFloat(org.apache.commons.lang3.math.NumberUtils)",
        "[Float].isInfinite()",
        "[Float].floatValue()",
        "[org.apache.commons.lang3.math.NumberUtils].createDouble(org.apache.commons.lang3.math.NumberUtils)",
        "[Double].isInfinite()",
        "[Double].floatValue()",
        "[org.apache.commons.lang3.math.NumberUtils].createBigDecimal(org.apache.commons.lang3.math.NumberUtils)",
        "[org.apache.commons.lang3.math.NumberUtils].createLong(String)",
        "[org.apache.commons.lang3.math.NumberUtils].createBigInteger(String)",
        "[org.apache.commons.lang3.math.NumberUtils].createFloat(String)",
        "[org.apache.commons.lang3.math.NumberUtils].createDouble(String)",
        "[Double].doubleValue()",
        "[org.apache.commons.lang3.math.NumberUtils].createBigDecimal(String)",
        "Integer.decode(String)",
        "Long.valueOf(String)",
        "[java.math.BigInteger].<init>(String)",
        "Float.valueOf(String)",
        "Double.valueOf(String)",
        "[java.math.BigDecimal].<init>(String)",
        "[org.apache.commons.lang3.math.NumberUtils].isAllZeros(String)",
        "[org.apache.commons.lang3.math.NumberUtils].isDigits(String)"
      ],
      "related_callees": [
        {
          "name": "createInteger",
          "source_file": "NumberUtils.java",
          "signature": "public static Integer createInteger(String str)",
          "source": "    public static Integer createInteger(String str) {\n        if (str == null) {\n            return null;\n        }\n        // decode() handles 0xAABD and 0777 (hex and octal) as well.\n        return Integer.decode(str);\n    }",
          "is_abstract": false,
          "impls": []
        },
        {
          "name": "isAllZeros",
          "source_file": "NumberUtils.java",
          "signature": "private static boolean isAllZeros(String str)",
          "source": "    private static boolean isAllZeros(String str) {\n        if (str == null) {\n            return true;\n        }\n        for (int i = str.length() - 1; i >= 0; i--) {\n            if (str.charAt(i) != '0') {\n                return false;\n            }\n        }\n        return str.length() > 0;\n    }",
          "is_abstract": false,
          "impls": []
        },
        {
          "name": "isDigits",
          "source_file": "NumberUtils.java",
          "signature": "public static boolean isDigits(String str)",
          "source": "    public static boolean isDigits(String str) {\n        if (StringUtils.isEmpty(str)) {\n            return false;\n        }\n        for (int i = 0; i < str.length(); i++) {\n            if (!Character.isDigit(str.charAt(i))) {\n                return false;\n            }\n        }\n        return true;\n    }",
          "is_abstract": false,
          "impls": []
        },
        {
          "name": "createLong",
          "source_file": "NumberUtils.java",
          "signature": "public static Long createLong(String str)",
          "source": "    public static Long createLong(String str) {\n        if (str == null) {\n            return null;\n        }\n        return Long.valueOf(str);\n    }",
          "is_abstract": false,
          "impls": []
        },
        {
          "name": "createBigInteger",
          "source_file": "NumberUtils.java",
          "signature": "public static BigInteger createBigInteger(String str)",
          "source": "    public static BigInteger createBigInteger(String str) {\n        if (str == null) {\n            return null;\n        }\n        return new BigInteger(str);\n    }",
          "is_abstract": false,
          "impls": []
        },
        {
          "name": "createFloat",
          "source_file": "NumberUtils.java",
          "signature": "public static Float createFloat(String str)",
          "source": "    public static Float createFloat(String str) {\n        if (str == null) {\n            return null;\n        }\n        return Float.valueOf(str);\n    }",
          "is_abstract": false,
          "impls": []
        }
      ],
      "field_siblings": []
    }
  ],
  "package": "org.apache.commons.lang3.math",
  "root_cause_reachable": [
    "StringUtils.isBlank",
    "NumberUtils.createInteger",
    "NumberUtils.isAllZeros",
    "NumberUtils.charAt",
    "NumberUtils.substring",
    "NumberUtils.isDigits",
    "NumberUtils.createLong",
    "NumberUtils.createBigInteger",
    "NumberUtils.createFloat",
    "NumberUtils.createDouble",
    "NumberUtils.createBigDecimal"
  ],
  "neighbourhood_notes": [],
  "source_imports": [
    "import java.math.BigDecimal;",
    "import java.math.BigInteger;",
    "import org.apache.commons.lang3.StringUtils;"
  ]
}
```

</details>

---
## [3] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~36,626 chars)</summary>

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
- Package: `org.apache.commons.lang3.math` (`package org.apache.commons.lang3.math;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Lang_16_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/lang3/math/NumberUtils.java
+++ /src/main/java/org/apache/commons/lang3/math/NumberUtils.java
@@ -542,8 +542,7 @@
                     }
                     //$FALL-THROUGH$
                 default :
-                    throw new NumberFormatException(str + " is not a valid number.");
-
+                	return createInteger(str);
             }
         } else {
             //User doesn't have a preference on the return type, so let's start

</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import java.math.BigDecimal;
import java.math.BigInteger;
import org.apache.commons.lang3.StringUtils;
</source_imports>

Function `createNumber`:
<signature>
public static Number createNumber(String str) throws NumberFormatException
</signature>
<code>
    public static Number createNumber(String str) throws NumberFormatException {
        if (str == null) {
            return null;
        }
        if (StringUtils.isBlank(str)) {
            throw new NumberFormatException("A blank string is not a valid number");
        }  
        if (str.startsWith("--")) {
            // this is protection for poorness in java.lang.BigDecimal.
            // it accepts this as a legal value, but it does not appear 
            // to be in specification of class. OS X Java parses it to 
            // a wrong value.
            return null;
        }
        if (str.startsWith("0x") || str.startsWith("-0x")) {
            return createInteger(str);
        }   
        char lastChar = str.charAt(str.length() - 1);
        String mant;
        String dec;
        String exp;
        int decPos = str.indexOf('.');
        int expPos = str.indexOf('e') + str.indexOf('E') + 1;

        if (decPos > -1) {

            if (expPos > -1) {
                if (expPos < decPos || expPos > str.length()) {
                    throw new NumberFormatException(str + " is not a valid number.");
                }
                dec = str.substring(decPos + 1, expPos);
            } else {
                dec = str.substring(decPos + 1);
            }
            mant = str.substring(0, decPos);
        } else {
            if (expPos > -1) {
                if (expPos > str.length()) {
                    throw new NumberFormatException(str + " is not a valid number.");
                }
                mant = str.substring(0, expPos);
            } else {
                mant = str;
            }
            dec = null;
        }
        if (!Character.isDigit(lastChar) && lastChar != '.') {
            if (expPos > -1 && expPos < str.length() - 1) {
                exp = str.substring(expPos + 1, str.length() - 1);
            } else {
                exp = null;
            }
            //Requesting a specific type..
            String numeric = str.substring(0, str.length() - 1);
            boolean allZeros = isAllZeros(mant) && isAllZeros(exp);
            switch (lastChar) {
                case 'l' :
                case 'L' :
                    if (dec == null
                        && exp == null
                        && (numeric.charAt(0) == '-' && isDigits(numeric.substring(1)) || isDigits(numeric))) {
                        try {
                            return createLong(numeric);
                        } catch (NumberFormatException nfe) { // NOPMD
                            // Too big for a long
                        }
                        return createBigInteger(numeric);

                    }
                    throw new NumberFormatException(str + " is not a valid number.");
                case 'f' :
                case 'F' :
                    try {
                        Float f = NumberUtils.createFloat(numeric);
                        if (!(f.isInfinite() || (f.floatValue() == 0.0F && !allZeros))) {
                            //If it's too big for a float or the float value = 0 and the string
                            //has non-zeros in it, then float does not have the precision we want
                            return f;
                        }

                    } catch (NumberFormatException nfe) { // NOPMD
                        // ignore the bad number
                    }
                    //$FALL-THROUGH$
                case 'd' :
                case 'D' :
                    try {
                        Double d = NumberUtils.createDouble(numeric);
                        if (!(d.isInfinite() || (d.floatValue() == 0.0D && !allZeros))) {
                            return d;
                        }
                    } catch (NumberFormatException nfe) { // NOPMD
                        // ignore the bad number
                    }
                    try {
                        return createBigDecimal(numeric);
                    } catch (NumberFormatException e) { // NOPMD
                        // ignore the bad number
                    }
                    //$FALL-THROUGH$
                default :
                    throw new NumberFormatException(str + " is not a valid number.");

            }
        } else {
            //User doesn't have a preference on the return type, so let's start
            //small and go from there...
            if (expPos > -1 && expPos < str.length() - 1) {
                exp = str.substring(expPos + 1, str.length());
            } else {
                exp = null;
            }
            if (dec == null && exp == null) {
                //Must be an int,long,bigint
                try {
                    return createInteger(str);
                } catch (NumberFormatException nfe) { // NOPMD
                    // ignore the bad number
                }
                try {
                    return createLong(str);
                } catch (NumberFormatException nfe) { // NOPMD
                    // ignore the bad number
                }
                return createBigInteger(str);

            } else {
                //Must be a float,double,BigDec
                boolean allZeros = isAllZeros(mant) && isAllZeros(exp);
                try {
                    Float f = createFloat(str);
                    if (!(f.isInfinite() || (f.floatValue() == 0.0F && !allZeros))) {
                        return f;
                    }
                } catch (NumberFormatException nfe) { // NOPMD
                    // ignore the bad number
                }
                try {
                    Double d = createDouble(str);
                    if (!(d.isInfinite() || (d.doubleValue() == 0.0D && !allZeros))) {
                        return d;
                    }
                } catch (NumberFormatException nfe) { // NOPMD
                    // ignore the bad number
                }

                return createBigDecimal(str);

            }
        }
    }
</code>
Call-site examples (use these as a guide for constructing the target call):
<xref>
@Test
    public void testCreateNumber() {
        // a lot of things can go wrong
        assertEquals("createNumber(String) 1 failed", Float.valueOf("1234.5"), NumberUtils.createNumber("1234.5"));
        assertEquals("createNumber(String) 2 failed", Integer.valueOf("12345"), NumberUtils.createNumber("12345"));
        assertEquals("createNumber(String) 3 failed", Double.valueOf("1234.5"), NumberUtils.createNumber("1234.5D"));
        assertEquals("createNumber(String) 3 failed", Double.valueOf("1234.5"), NumberUtils.createNumber("1234.5d"));
        assertEquals("createNumber(String) 4 failed", Float.valueOf("1234.5"), NumberUtils.createNumber("1234.5F"));
        assertEquals("createNumber(String) 4 failed", Float.valueOf("1234.5"), NumberUtils.createNumber("1234.5f"));
        assertEquals("createNumber(String) 5 failed", Long.valueOf(Integer.MAX_VALUE + 1L), NumberUtils.createNumber(""
            + (Integer.MAX_VALUE + 1L)));
        assertEquals("createNumber(String) 6 failed", Long.valueOf(12345), NumberUtils.createNumber("12345L"));
        assertEquals("createNumber(String) 6 failed", Long.valueOf(12345), NumberUtils.createNumber("12345l"));
        assertEquals("createNumber(String) 7 failed", Float.valueOf("-1234.5"), NumberUtils.createNumber("-1234.5"));
        assertEquals("createNumber(String) 8 failed", Integer.valueOf("-12345"), NumberUtils.createNumber("-12345"));
        assertTrue("createNumber(String) 9a failed", 0xFADE == NumberUtils.createNumber("0xFADE").intValue());
        assertTrue("createNumber(String) 9b failed", 0xFADE == NumberUtils.createNumber("0Xfade").intValue());
        assertTrue("createNumber(String) 10a failed", -0xFADE == NumberUtils.createNumber("-0xFADE").intValue());
        assertTrue("createNumber(String) 10b failed", -0xFADE == NumberUtils.createNumber("-0Xfade").intValue());
        assertEquals("createNumber(String) 11 failed", Double.valueOf("1.1E200"), NumberUtils.createNumber("1.1E200"));
        assertEquals("createNumber(String) 12 failed", Float.valueOf("1.1E20"), NumberUtils.createNumber("1.1E20"));
        assertEquals("createNumber(String) 13 failed", Double.valueOf("-1.1E200"), NumberUtils.createNumber("-1.1E200"));
        assertEquals("createNumber(String) 14 failed", Double.valueOf("1.1E-200"), NumberUtils.createNumber("1.1E-200"));
        assertEquals("createNumber(null) failed", null, NumberUtils.createNumber(null));
        assertEquals("createNumber(String) failed", new BigInteger("12345678901234567890"), NumberUtils
                .createNumber("12345678901234567890L"));

        // jdk 1.2 doesn't support this. unsure about jdk 1.2.2
        if (SystemUtils.isJavaVersionAtLeast(JAVA_1_3)) {
            assertEquals("createNumber(String) 15 failed", new BigDecimal("1.1E-700"), NumberUtils
                    .createNumber("1.1E-700F"));
        }
        assertEquals("createNumber(String) 16 failed", Long.valueOf("10" + Integer.MAX_VALUE), NumberUtils
                .createNumber("10" + Integer.MAX_VALUE + "L"));
        assertEquals("createNumber(String) 17 failed", Long.valueOf("10" + Integer.MAX_VALUE), NumberUtils
                .createNumber("10" + Integer.MAX_VALUE));
        assertEquals("createNumber(String) 18 failed", new BigInteger("10" + Long.MAX_VALUE), NumberUtils
                .createNumber("10" + Long.MAX_VALUE));

        // LANG-521
        assertEquals("createNumber(String) LANG-521 failed", Float.valueOf("2."), NumberUtils.createNumber("2."));

        // LANG-638
        assertFalse("createNumber(String) succeeded", checkCreateNumber("1eE"));

        // LANG-693
        assertEquals("createNumber(String) LANG-693 failed", Double.valueOf(Double.MAX_VALUE), NumberUtils
                    .createNumber("" + Double.MAX_VALUE));
    }
</xref>
<xref>
private boolean checkCreateNumber(String val) {
        try {
            Object obj = NumberUtils.createNumber(val);
            if (obj == null) {
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
       }
    }
</xref>
<xref>
@Test
    public void testLang300() {
        NumberUtils.createNumber("-1l");
        NumberUtils.createNumber("01l");
        NumberUtils.createNumber("1l");
    }
</xref>
Methods called by `createNumber` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="createInteger" from="NumberUtils.java">
<signature>
public static Integer createInteger(String str)
</signature>
<code>
    public static Integer createInteger(String str) {
        if (str == null) {
            return null;
        }
        // decode() handles 0xAABD and 0777 (hex and octal) as well.
        return Integer.decode(str);
    }
</code>
</callee>
<callee name="isAllZeros" from="NumberUtils.java">
<signature>
private static boolean isAllZeros(String str)
</signature>
<code>
    private static boolean isAllZeros(String str) {
        if (str == null) {
            return true;
        }
        for (int i = str.length() - 1; i >= 0; i--) {
            if (str.charAt(i) != '0') {
                return false;
            }
        }
        return str.length() > 0;
    }
</code>
</callee>
<callee name="isDigits" from="NumberUtils.java">
<signature>
public static boolean isDigits(String str)
</signature>
<code>
    public static boolean isDigits(String str) {
        if (StringUtils.isEmpty(str)) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }
</code>
</callee>
<callee name="createLong" from="NumberUtils.java">
<signature>
public static Long createLong(String str)
</signature>
<code>
    public static Long createLong(String str) {
        if (str == null) {
            return null;
        }
        return Long.valueOf(str);
    }
</code>
</callee>
<callee name="createBigInteger" from="NumberUtils.java">
<signature>
public static BigInteger createBigInteger(String str)
</signature>
<code>
    public static BigInteger createBigInteger(String str) {
        if (str == null) {
            return null;
        }
        return new BigInteger(str);
    }
</code>
</callee>
<callee name="createFloat" from="NumberUtils.java">
<signature>
public static Float createFloat(String str)
</signature>
<code>
    public static Float createFloat(String str) {
        if (str == null) {
            return null;
        }
        return Float.valueOf(str);
    }
</code>
</callee>

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `NumberUtils`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.NumberFormatException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `createNumber` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: java.lang.NumberFormatException
message: 0Xfade is not a valid number.
thrown_at: org.apache.commons.lang3.math.NumberUtils.createNumber(NumberUtils.java:545)
anchor_input: "1234.5"  // hard-code this verbatim as your first call, then fuzz inputs of the same shape
other_observed_literals: "12345", "1234.5D", "1234.5d", "1234.5F", "1234.5f", "12345L", "12345l", "-1234.5", "-12345", "0xFADE", "0Xfade", "-0xFADE", "-0Xfade", "1.1E200", "1.1E20", "-1.1E200", "1.1E-200", "12345678901234567890L", "1.1E-700F", "2."
</ground_truth_crash>
<failing_test class="org.apache.commons.lang3.math.NumberUtilsTest" method="testCreateNumber">
    @Test
    public void testCreateNumber() {
        // a lot of things can go wrong
        assertEquals("createNumber(String) 1 failed", Float.valueOf("1234.5"), NumberUtils.createNumber("1234.5"));
        assertEquals("createNumber(String) 2 failed", Integer.valueOf("12345"), NumberUtils.createNumber("12345"));
        assertEquals("createNumber(String) 3 failed", Double.valueOf("1234.5"), NumberUtils.createNumber("1234.5D"));
        assertEquals("createNumber(String) 3 failed", Double.valueOf("1234.5"), NumberUtils.createNumber("1234.5d"));
        assertEquals("createNumber(String) 4 failed", Float.valueOf("1234.5"), NumberUtils.createNumber("1234.5F"));
        assertEquals("createNumber(String) 4 failed", Float.valueOf("1234.5"), NumberUtils.createNumber("1234.5f"));
        assertEquals("createNumber(String) 5 failed", Long.valueOf(Integer.MAX_VALUE + 1L), NumberUtils.createNumber(""
            + (Integer.MAX_VALUE + 1L)));
        assertEquals("createNumber(String) 6 failed", Long.valueOf(12345), NumberUtils.createNumber("12345L"));
        assertEquals("createNumber(String) 6 failed", Long.valueOf(12345), NumberUtils.createNumber("12345l"));
        assertEquals("createNumber(String) 7 failed", Float.valueOf("-1234.5"), NumberUtils.createNumber("-1234.5"));
        assertEquals("createNumber(String) 8 failed", Integer.valueOf("-12345"), NumberUtils.createNumber("-12345"));
        assertTrue("createNumber(String) 9a failed", 0xFADE == NumberUtils.createNumber("
        // ... (truncated)
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.lang3.math.NumberUtilsTest::testCreateNumber
java.lang.NumberFormatException: 0Xfade is not a valid number.
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.
The test method depends on the following from its test class — setUp, helper methods, constants, fixture files. Replicate this environment EXACTLY (register the same files, wire the same providers, use the same constants); do NOT improvise setup the test performs differently, and DROP any check whose setup you cannot replicate:
<test_support class="org.apache.commons.lang3.math.NumberUtilsTest">
// --- helper checkCreateNumber() ---
    private boolean checkCreateNumber(String val) {
        try {
            Object obj = NumberUtils.createNumber(val);
            if (obj == null) {
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
       }
    }
</test_support>

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  max(long[] array) / (int[] array) / (short[] array) / (byte[] array) / (double[] array) / (float[] array) / (long a, long b, long c) / (int a, int b, int c) / (short a, short b, short c) / (byte a, byte b, byte c) / (double a, double b, double c) / (float a, float b, float c)
  min(long[] array) / (int[] array) / (short[] array) / (byte[] array) / (double[] array) / (float[] array) / (long a, long b, long c) / (int a, int b, int c) / (short a, short b, short c) / (byte a, byte b, byte c) / (double a, double b, double c) / (float a, float b, float c)
  toByte(String str) / (String str, byte defaultValue)
  toDouble(String str) / (String str, double defaultValue)
  toFloat(String str) / (String str, float defaultValue)
  toInt(String str) / (String str, int defaultValue)
  toLong(String str) / (String str, long defaultValue)
  toShort(String str) / (String str, short defaultValue)

METHOD FAMILIES over the same input space (factory/parser siblings — where the docs state a selection or agreement rule between family members, equivalent inputs must respect it):
  create* family: createBigDecimal, createBigInteger, createDouble, createFloat, createInteger, createLong, createNumber
  to* family: toByte, toDouble, toFloat, toInt, toLong, toShort

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- StringUtils.isBlank
- NumberUtils.createInteger
- NumberUtils.isAllZeros
- NumberUtils.charAt
- NumberUtils.substring
- NumberUtils.isDigits
- NumberUtils.createLong
- NumberUtils.createBigInteger
- NumberUtils.createFloat
- NumberUtils.createDouble
- NumberUtils.createBigDecimal
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
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~7,516 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Anchor 1: exact literal from the provided ground-truth anchor_input.
        // Contract used for this metamorphic check: for a plain decimal literal like "1234.5"
        // the shown implementation takes the float/double/bigdecimal selection path and, as
        // demonstrated by the test, createNumber("1234.5") must agree with createFloat("1234.5").
        try {
            Number n = NumberUtils.createNumber("1234.5");
            Float f = NumberUtils.createFloat("1234.5");
            if (n != null && f != null) {
                if (!(n instanceof Float) || Float.compare(((Float) n).floatValue(), f.floatValue()) != 0) {
                    throw new RuntimeException(
                            "[oracle:decimal-float] metamorphic violation: createNumber/createFloat disagree input=1234.5 lhs="
                                    + n + " rhs=" + f);
                }
            }
        } catch (RuntimeException ignored) {
            // If either side rejects, the relation does not apply for this iteration.
        }

        // Anchor 2: exact failing literal from the test/ground truth.
        // Contract used for this post-condition/metamorphic check:
        // - createInteger delegates to Integer.decode, whose comment explicitly says it handles hex.
        // - the test asserts createNumber("0xFADE") and createNumber("0Xfade") both equal 0xFADE.
        // Therefore, for any valid decode-able uppercase-0X integer form without a numeric type suffix,
        // createNumber must agree with createInteger and with the equivalent lowercase-x form.
        checkValidHexAgreement("0Xfade");

        // Explore the same root cause with many valid-by-construction variants:
        // uppercase 0X / -0X prefixes plus a final hex letter, which on the buggy build are
        // misrouted into the suffix-handling branch and can throw from createNumber.
        String explored1 = buildFromMagnitude(data);
        checkValidHexAgreement(explored1);

        String explored2 = buildFreeFormHex(data);
        checkValidHexAgreement(explored2);
    }

    private static void checkValidHexAgreement(String s) {
        Integer decoded;
        try {
            decoded = NumberUtils.createInteger(s);
        } catch (RuntimeException ignored) {
            return;
        }
        if (decoded == null) {
            return;
        }

        Number viaCreateNumber;
        try {
            viaCreateNumber = NumberUtils.createNumber(s);
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        if (viaCreateNumber == null || viaCreateNumber.intValue() != decoded.intValue()) {
            throw new RuntimeException(
                    "[oracle:hex-agree] metamorphic violation: createNumber/createInteger disagree input=" + s
                            + " lhs=" + viaCreateNumber + " rhs=" + decoded);
        }

        String lowerEquivalent = toLowercaseXPrefix(s);
        try {
            Number lower = NumberUtils.createNumber(lowerEquivalent);
            Integer lowerDecoded = NumberUtils.createInteger(lowerEquivalent);
            if (lower == null || lowerDecoded == null) {
                return;
            }
            if (lower.intValue() != lowerDecoded.intValue()
                    || lower.intValue() != decoded.intValue()
                    || lower.intValue() != viaCreateNumber.intValue()) {
                throw new RuntimeException(
                        "[oracle:hex-case] metamorphic violation: equivalent hex inputs disagree input=" + s
                                + " lower=" + lowerEquivalent + " lhs=" + viaCreateNumber + " rhs=" + lower
                                + " decode=" + decoded + " lowerDecode=" + lowerDecoded);
            }
        } catch (RuntimeException ignored) {
            // If either side rejects here, skip this relation for this input.
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NumberFormatException)) {
            return false;
        }
        for (StackTraceElement frame : t.getStackTrace()) {
            String cls = frame.getClassName();
            String method = frame.getMethodName();
            if ("org.apache.commons.lang3.math.NumberUtils".equals(cls)) {
                if ("createNumber".equals(method)
                        || "createInteger".equals(method)
                        || "isAllZeros".equals(method)
                        || "isDigits".equals(method)
                        || "createLong".equals(method)
                        || "createBigInteger".equals(method)
                        || "createFloat".equals(method)
                        || "createDouble".equals(method)
                        || "createBigDecimal".equals(method)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String buildFromMagnitude(FuzzedDataProvider data) {
        int magnitude = data.consumeInt(10, 0x0FFFFFFF);
        magnitude = (magnitude & ~0xF) | (10 + data.consumeInt(0, 5)); // ensure last nibble is a-f
        String body = Integer.toHexString(magnitude);
        body = maybePadZeros(body, data.consumeInt(0, 4));
        body = mixHexCase(body, data);
        return (data.consumeBoolean() ? "-0X" : "0X") + body;
    }

    private static String buildFreeFormHex(FuzzedDataProvider data) {
        int len = data.consumeInt(1, 7); // always valid int-range magnitude when interpreted as hex
        StringBuilder body = new StringBuilder(len + 4);
        for (int i = 0; i < len - 1; i++) {
            int v = data.consumeInt(0, 15);
            body.append(toHexChar(v, data.consumeBoolean()));
        }
        body.append(toHexChar(10 + data.consumeInt(0, 5), data.consumeBoolean())); // final char is a-f/A-F
        String core = maybePadZeros(body.toString(), data.consumeInt(0, 3));
        return (data.consumeBoolean() ? "-0X" : "0X") + core;
    }

    private static String maybePadZeros(String body, int count) {
        if (count <= 0) {
            return body;
        }
        StringBuilder sb = new StringBuilder(count + body.length());
        for (int i = 0; i < count; i++) {
            sb.append('0');
        }
        sb.append(body);
        return sb.toString();
    }

    private static String mixHexCase(String s, FuzzedDataProvider data) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'a' && c <= 'f' && data.consumeBoolean()) {
                sb.append((char) (c - ('a' - 'A')));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static char toHexChar(int v, boolean upper) {
        if (v < 10) {
            return (char) ('0' + v);
        }
        return (char) ((upper ? 'A' : 'a') + (v - 10));
    }

    private static String toLowercaseXPrefix(String s) {
        if (s.startsWith("-0X")) {
            return "-0x" + s.substring(3);
        }
        if (s.startsWith("0X")) {
            return "0x" + s.substring(2);
        }
        return s;
    }
}
```

</details>

---
## [4] ⚙️ harness-attempt · `attempt_001`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NumberFormatException: 0Xfade is not a valid number.

---
## [5] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~38,168 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.apache.commons.lang3.math` (`package org.apache.commons.lang3.math;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Lang_16_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/lang3/math/NumberUtils.java
+++ /src/main/java/org/apache/commons/lang3/math/NumberUtils.java
@@ -542,8 +542,7 @@
                     }
                     //$FALL-THROUGH$
                 default :
-                    throw new NumberFormatException(str + " is not a valid number.");
-
+                	return createInteger(str);
             }
         } else {
             //User doesn't have a preference on the return type, so let's start

</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import java.math.BigDecimal;
import java.math.BigInteger;
import org.apache.commons.lang3.StringUtils;
</source_imports>

Function `createNumber`:
<signature>
public static Number createNumber(String str) throws NumberFormatException
</signature>
<code>
    public static Number createNumber(String str) throws NumberFormatException {
        if (str == null) {
            return null;
        }
        if (StringUtils.isBlank(str)) {
            throw new NumberFormatException("A blank string is not a valid number");
        }  
        if (str.startsWith("--")) {
            // this is protection for poorness in java.lang.BigDecimal.
            // it accepts this as a legal value, but it does not appear 
            // to be in specification of class. OS X Java parses it to 
            // a wrong value.
            return null;
        }
        if (str.startsWith("0x") || str.startsWith("-0x")) {
            return createInteger(str);
        }   
        char lastChar = str.charAt(str.length() - 1);
        String mant;
        String dec;
        String exp;
        int decPos = str.indexOf('.');
        int expPos = str.indexOf('e') + str.indexOf('E') + 1;

        if (decPos > -1) {

            if (expPos > -1) {
                if (expPos < decPos || expPos > str.length()) {
                    throw new NumberFormatException(str + " is not a valid number.");
                }
                dec = str.substring(decPos + 1, expPos);
            } else {
                dec = str.substring(decPos + 1);
            }
            mant = str.substring(0, decPos);
        } else {
            if (expPos > -1) {
                if (expPos > str.length()) {
                    throw new NumberFormatException(str + " is not a valid number.");
                }
                mant = str.substring(0, expPos);
            } else {
                mant = str;
            }
            dec = null;
        }
        if (!Character.isDigit(lastChar) && lastChar != '.') {
            if (expPos > -1 && expPos < str.length() - 1) {
                exp = str.substring(expPos + 1, str.length() - 1);
            } else {
                exp = null;
            }
            //Requesting a specific type..
            String numeric = str.substring(0, str.length() - 1);
            boolean allZeros = isAllZeros(mant) && isAllZeros(exp);
            switch (lastChar) {
                case 'l' :
                case 'L' :
                    if (dec == null
                        && exp == null
                        && (numeric.charAt(0) == '-' && isDigits(numeric.substring(1)) || isDigits(numeric))) {
                        try {
                            return createLong(numeric);
                        } catch (NumberFormatException nfe) { // NOPMD
                            // Too big for a long
                        }
                        return createBigInteger(numeric);

                    }
                    throw new NumberFormatException(str + " is not a valid number.");
                case 'f' :
                case 'F' :
                    try {
                        Float f = NumberUtils.createFloat(numeric);
                        if (!(f.isInfinite() || (f.floatValue() == 0.0F && !allZeros))) {
                            //If it's too big for a float or the float value = 0 and the string
                            //has non-zeros in it, then float does not have the precision we want
                            return f;
                        }

                    } catch (NumberFormatException nfe) { // NOPMD
                        // ignore the bad number
                    }
                    //$FALL-THROUGH$
                case 'd' :
                case 'D' :
                    try {
                        Double d = NumberUtils.createDouble(numeric);
                        if (!(d.isInfinite() || (d.floatValue() == 0.0D && !allZeros))) {
                            return d;
                        }
                    } catch (NumberFormatException nfe) { // NOPMD
                        // ignore the bad number
                    }
                    try {
                        return createBigDecimal(numeric);
                    } catch (NumberFormatException e) { // NOPMD
                        // ignore the bad number
                    }
                    //$FALL-THROUGH$
                default :
                    throw new NumberFormatException(str + " is not a valid number.");

            }
        } else {
            //User doesn't have a preference on the return type, so let's start
            //small and go from there...
            if (expPos > -1 && expPos < str.length() - 1) {
                exp = str.substring(expPos + 1, str.length());
            } else {
                exp = null;
            }
            if (dec == null && exp == null) {
                //Must be an int,long,bigint
                try {
                    return createInteger(str);
                } catch (NumberFormatException nfe) { // NOPMD
                    // ignore the bad number
                }
                try {
                    return createLong(str);
                } catch (NumberFormatException nfe) { // NOPMD
                    // ignore the bad number
                }
                return createBigInteger(str);

            } else {
                //Must be a float,double,BigDec
                boolean allZeros = isAllZeros(mant) && isAllZeros(exp);
                try {
                    Float f = createFloat(str);
                    if (!(f.isInfinite() || (f.floatValue() == 0.0F && !allZeros))) {
                        return f;
                    }
                } catch (NumberFormatException nfe) { // NOPMD
                    // ignore the bad number
                }
                try {
                    Double d = createDouble(str);
                    if (!(d.isInfinite() || (d.doubleValue() == 0.0D && !allZeros))) {
                        return d;
                    }
                } catch (NumberFormatException nfe) { // NOPMD
                    // ignore the bad number
                }

                return createBigDecimal(str);

            }
        }
    }
</code>
Call-site examples (use these as a guide for constructing the target call):
<xref>
@Test
    public void testCreateNumber() {
        // a lot of things can go wrong
        assertEquals("createNumber(String) 1 failed", Float.valueOf("1234.5"), NumberUtils.createNumber("1234.5"));
        assertEquals("createNumber(String) 2 failed", Integer.valueOf("12345"), NumberUtils.createNumber("12345"));
        assertEquals("createNumber(String) 3 failed", Double.valueOf("1234.5"), NumberUtils.createNumber("1234.5D"));
        assertEquals("createNumber(String) 3 failed", Double.valueOf("1234.5"), NumberUtils.createNumber("1234.5d"));
        assertEquals("createNumber(String) 4 failed", Float.valueOf("1234.5"), NumberUtils.createNumber("1234.5F"));
        assertEquals("createNumber(String) 4 failed", Float.valueOf("1234.5"), NumberUtils.createNumber("1234.5f"));
        assertEquals("createNumber(String) 5 failed", Long.valueOf(Integer.MAX_VALUE + 1L), NumberUtils.createNumber(""
            + (Integer.MAX_VALUE + 1L)));
        assertEquals("createNumber(String) 6 failed", Long.valueOf(12345), NumberUtils.createNumber("12345L"));
        assertEquals("createNumber(String) 6 failed", Long.valueOf(12345), NumberUtils.createNumber("12345l"));
        assertEquals("createNumber(String) 7 failed", Float.valueOf("-1234.5"), NumberUtils.createNumber("-1234.5"));
        assertEquals("createNumber(String) 8 failed", Integer.valueOf("-12345"), NumberUtils.createNumber("-12345"));
        assertTrue("createNumber(String) 9a failed", 0xFADE == NumberUtils.createNumber("0xFADE").intValue());
        assertTrue("createNumber(String) 9b failed", 0xFADE == NumberUtils.createNumber("0Xfade").intValue());
        assertTrue("createNumber(String) 10a failed", -0xFADE == NumberUtils.createNumber("-0xFADE").intValue());
        assertTrue("createNumber(String) 10b failed", -0xFADE == NumberUtils.createNumber("-0Xfade").intValue());
        assertEquals("createNumber(String) 11 failed", Double.valueOf("1.1E200"), NumberUtils.createNumber("1.1E200"));
        assertEquals("createNumber(String) 12 failed", Float.valueOf("1.1E20"), NumberUtils.createNumber("1.1E20"));
        assertEquals("createNumber(String) 13 failed", Double.valueOf("-1.1E200"), NumberUtils.createNumber("-1.1E200"));
        assertEquals("createNumber(String) 14 failed", Double.valueOf("1.1E-200"), NumberUtils.createNumber("1.1E-200"));
        assertEquals("createNumber(null) failed", null, NumberUtils.createNumber(null));
        assertEquals("createNumber(String) failed", new BigInteger("12345678901234567890"), NumberUtils
                .createNumber("12345678901234567890L"));

        // jdk 1.2 doesn't support this. unsure about jdk 1.2.2
        if (SystemUtils.isJavaVersionAtLeast(JAVA_1_3)) {
            assertEquals("createNumber(String) 15 failed", new BigDecimal("1.1E-700"), NumberUtils
                    .createNumber("1.1E-700F"));
        }
        assertEquals("createNumber(String) 16 failed", Long.valueOf("10" + Integer.MAX_VALUE), NumberUtils
                .createNumber("10" + Integer.MAX_VALUE + "L"));
        assertEquals("createNumber(String) 17 failed", Long.valueOf("10" + Integer.MAX_VALUE), NumberUtils
                .createNumber("10" + Integer.MAX_VALUE));
        assertEquals("createNumber(String) 18 failed", new BigInteger("10" + Long.MAX_VALUE), NumberUtils
                .createNumber("10" + Long.MAX_VALUE));

        // LANG-521
        assertEquals("createNumber(String) LANG-521 failed", Float.valueOf("2."), NumberUtils.createNumber("2."));

        // LANG-638
        assertFalse("createNumber(String) succeeded", checkCreateNumber("1eE"));

        // LANG-693
        assertEquals("createNumber(String) LANG-693 failed", Double.valueOf(Double.MAX_VALUE), NumberUtils
                    .createNumber("" + Double.MAX_VALUE));
    }
</xref>
<xref>
private boolean checkCreateNumber(String val) {
        try {
            Object obj = NumberUtils.createNumber(val);
            if (obj == null) {
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
       }
    }
</xref>
<xref>
@Test
    public void testLang300() {
        NumberUtils.createNumber("-1l");
        NumberUtils.createNumber("01l");
        NumberUtils.createNumber("1l");
    }
</xref>
Methods called by `createNumber` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="createInteger" from="NumberUtils.java">
<signature>
public static Integer createInteger(String str)
</signature>
<code>
    public static Integer createInteger(String str) {
        if (str == null) {
            return null;
        }
        // decode() handles 0xAABD and 0777 (hex and octal) as well.
        return Integer.decode(str);
    }
</code>
</callee>
<callee name="isAllZeros" from="NumberUtils.java">
<signature>
private static boolean isAllZeros(String str)
</signature>
<code>
    private static boolean isAllZeros(String str) {
        if (str == null) {
            return true;
        }
        for (int i = str.length() - 1; i >= 0; i--) {
            if (str.charAt(i) != '0') {
                return false;
            }
        }
        return str.length() > 0;
    }
</code>
</callee>
<callee name="isDigits" from="NumberUtils.java">
<signature>
public static boolean isDigits(String str)
</signature>
<code>
    public static boolean isDigits(String str) {
        if (StringUtils.isEmpty(str)) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }
</code>
</callee>
<callee name="createLong" from="NumberUtils.java">
<signature>
public static Long createLong(String str)
</signature>
<code>
    public static Long createLong(String str) {
        if (str == null) {
            return null;
        }
        return Long.valueOf(str);
    }
</code>
</callee>
<callee name="createBigInteger" from="NumberUtils.java">
<signature>
public static BigInteger createBigInteger(String str)
</signature>
<code>
    public static BigInteger createBigInteger(String str) {
        if (str == null) {
            return null;
        }
        return new BigInteger(str);
    }
</code>
</callee>
<callee name="createFloat" from="NumberUtils.java">
<signature>
public static Float createFloat(String str)
</signature>
<code>
    public static Float createFloat(String str) {
        if (str == null) {
            return null;
        }
        return Float.valueOf(str);
    }
</code>
</callee>

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `NumberUtils`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.NumberFormatException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `createNumber` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: java.lang.NumberFormatException
message: 0Xfade is not a valid number.
thrown_at: org.apache.commons.lang3.math.NumberUtils.createNumber(NumberUtils.java:545)
anchor_input: "1234.5"  // hard-code this verbatim as your first call, then fuzz inputs of the same shape
other_observed_literals: "12345", "1234.5D", "1234.5d", "1234.5F", "1234.5f", "12345L", "12345l", "-1234.5", "-12345", "0xFADE", "0Xfade", "-0xFADE", "-0Xfade", "1.1E200", "1.1E20", "-1.1E200", "1.1E-200", "12345678901234567890L", "1.1E-700F", "2."
</ground_truth_crash>
<failing_test class="org.apache.commons.lang3.math.NumberUtilsTest" method="testCreateNumber">
    @Test
    public void testCreateNumber() {
        // a lot of things can go wrong
        assertEquals("createNumber(String) 1 failed", Float.valueOf("1234.5"), NumberUtils.createNumber("1234.5"));
        assertEquals("createNumber(String) 2 failed", Integer.valueOf("12345"), NumberUtils.createNumber("12345"));
        assertEquals("createNumber(String) 3 failed", Double.valueOf("1234.5"), NumberUtils.createNumber("1234.5D"));
        assertEquals("createNumber(String) 3 failed", Double.valueOf("1234.5"), NumberUtils.createNumber("1234.5d"));
        assertEquals("createNumber(String) 4 failed", Float.valueOf("1234.5"), NumberUtils.createNumber("1234.5F"));
        assertEquals("createNumber(String) 4 failed", Float.valueOf("1234.5"), NumberUtils.createNumber("1234.5f"));
        assertEquals("createNumber(String) 5 failed", Long.valueOf(Integer.MAX_VALUE + 1L), NumberUtils.createNumber(""
            + (Integer.MAX_VALUE + 1L)));
        assertEquals("createNumber(String) 6 failed", Long.valueOf(12345), NumberUtils.createNumber("12345L"));
        assertEquals("createNumber(String) 6 failed", Long.valueOf(12345), NumberUtils.createNumber("12345l"));
        assertEquals("createNumber(String) 7 failed", Float.valueOf("-1234.5"), NumberUtils.createNumber("-1234.5"));
        assertEquals("createNumber(String) 8 failed", Integer.valueOf("-12345"), NumberUtils.createNumber("-12345"));
        assertTrue("createNumber(String) 9a failed", 0xFADE == NumberUtils.createNumber("
        // ... (truncated)
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.lang3.math.NumberUtilsTest::testCreateNumber
java.lang.NumberFormatException: 0Xfade is not a valid number.
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.
The test method depends on the following from its test class — setUp, helper methods, constants, fixture files. Replicate this environment EXACTLY (register the same files, wire the same providers, use the same constants); do NOT improvise setup the test performs differently, and DROP any check whose setup you cannot replicate:
<test_support class="org.apache.commons.lang3.math.NumberUtilsTest">
// --- helper checkCreateNumber() ---
    private boolean checkCreateNumber(String val) {
        try {
            Object obj = NumberUtils.createNumber(val);
            if (obj == null) {
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
       }
    }
</test_support>

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  max(long[] array) / (int[] array) / (short[] array) / (byte[] array) / (double[] array) / (float[] array) / (long a, long b, long c) / (int a, int b, int c) / (short a, short b, short c) / (byte a, byte b, byte c) / (double a, double b, double c) / (float a, float b, float c)
  min(long[] array) / (int[] array) / (short[] array) / (byte[] array) / (double[] array) / (float[] array) / (long a, long b, long c) / (int a, int b, int c) / (short a, short b, short c) / (byte a, byte b, byte c) / (double a, double b, double c) / (float a, float b, float c)
  toByte(String str) / (String str, byte defaultValue)
  toDouble(String str) / (String str, double defaultValue)
  toFloat(String str) / (String str, float defaultValue)
  toInt(String str) / (String str, int defaultValue)
  toLong(String str) / (String str, long defaultValue)
  toShort(String str) / (String str, short defaultValue)

METHOD FAMILIES over the same input space (factory/parser siblings — where the docs state a selection or agreement rule between family members, equivalent inputs must respect it):
  create* family: createBigDecimal, createBigInteger, createDouble, createFloat, createInteger, createLong, createNumber
  to* family: toByte, toDouble, toFloat, toInt, toLong, toShort

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- StringUtils.isBlank
- NumberUtils.createInteger
- NumberUtils.isAllZeros
- NumberUtils.charAt
- NumberUtils.substring
- NumberUtils.isDigits
- NumberUtils.createLong
- NumberUtils.createBigInteger
- NumberUtils.createFloat
- NumberUtils.createDouble
- NumberUtils.createBigDecimal
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.lang3.math.FuzzHarness.checkValidHexAgreement
- org.apache.commons.lang3.math.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.lang3.math.NumberUtils.createNumber
Crashes already found:
- java.lang.NumberFormatException@org.apache.commons.lang3.math.NumberUtils.createNumber
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- StringUtils.isBlank
- NumberUtils.createInteger
- NumberUtils.isAllZeros
- NumberUtils.charAt
- NumberUtils.substring
- NumberUtils.isDigits
- NumberUtils.createLong
- NumberUtils.createBigInteger
- NumberUtils.createFloat
- NumberUtils.createDouble
- NumberUtils.createBigDecimal
Check FAMILIES already covered by accepted harnesses: {decimal-float, hex-agree, hex-case}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.NumberFormatException@org.apache.commons.lang3.math.NumberUtils.createNumber. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
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
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~7,279 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            NumberUtils.createNumber("1234.5");
        } catch (RuntimeException ignored) {
        }

        checkUppercaseHexAgainstCreateInteger("0Xfade");

        int rounds = 1 + data.consumeInt(0, 3);
        for (int i = 0; i < rounds; i++) {
            if (data.remainingBytes() <= 0) {
                break;
            }
            switch (data.consumeInt(0, 2)) {
                case 0:
                    checkUppercaseHexAgainstCreateInteger(buildUppercaseHexAlphaTerminated(data));
                    break;
                case 1:
                    checkLongSuffixOracle(buildDecimalBase(data));
                    break;
                default:
                    checkPlainDecimalOracle(buildPlainIntString(data));
                    break;
            }
        }
    }

    private static void checkUppercaseHexAgainstCreateInteger(String s) {
        Integer expected;
        try {
            expected = NumberUtils.createInteger(s);
        } catch (RuntimeException e) {
            return;
        }

        try {
            Number actual = NumberUtils.createNumber(s);
            if (actual == null || actual.intValue() != expected.intValue()) {
                throw new RuntimeException("[oracle:upper-x-int] metamorphic violation: createNumber(valid uppercase-hex) must agree with createInteger input=" + s + " lhs=" + actual + " rhs=" + expected);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                if (isRootCauseNumberFormat(t)) {
                    throw t;
                }
                return;
            }
            throw t;
        }
    }

    private static void checkLongSuffixOracle(String base) {
        try {
            Long direct = NumberUtils.createLong(base);
            Number lower = NumberUtils.createNumber(base + "l");
            Number upper = NumberUtils.createNumber(base + "L");

            if (lower == null || upper == null) {
                throw new RuntimeException("[oracle:long-suffix] metamorphic violation: suffixed long parse returned null input=" + base + " lower=" + lower + " upper=" + upper + " direct=" + direct);
            }

            long lv = lower.longValue();
            long uv = upper.longValue();
            long dv = direct.longValue();

            /*
             * Contract used: the tests for createNumber explicitly require both "12345L"
             * and "12345l" to parse as the same Long value, and NumberUtils.createLong(base)
             * is the direct real-library parser for that numeric body. A patch that merely
             * deletes/guards a throwing branch but returns the wrong number would violate this.
             */
            if (lv != uv || lv != dv) {
                throw new RuntimeException("[oracle:long-suffix] metamorphic violation: createNumber(base+'l'), createNumber(base+'L'), and createLong(base) must agree input=" + base + " lhs=" + lv + " rhs=" + uv + " direct=" + dv);
            }
        } catch (RuntimeException e) {
            if (!isCleanRejection(e)) {
                throw e;
            }
        }
    }

    private static void checkPlainDecimalOracle(String s) {
        try {
            Integer expected = NumberUtils.createInteger(s);
            Number actual = NumberUtils.createNumber(s);

            /*
             * Contract used: for a valid decimal integer string with no decimal point,
             * exponent, or type suffix, createNumber starts with the smallest fitting
             * integral type; for int-range inputs that must match createInteger(s).
             */
            if (actual == null || actual.intValue() != expected.intValue()) {
                throw new RuntimeException("[oracle:plain-int] metamorphic violation: createNumber(valid int string) must agree with createInteger input=" + s + " lhs=" + actual + " rhs=" + expected);
            }
        } catch (RuntimeException e) {
            if (!isCleanRejection(e)) {
                throw e;
            }
        }
    }

    private static String buildUppercaseHexAlphaTerminated(FuzzedDataProvider data) {
        boolean negative = data.consumeBoolean();
        int len = 1 + data.consumeInt(0, 6);
        StringBuilder sb = new StringBuilder();
        if (negative) {
            sb.append('-');
        }
        sb.append('0').append('X');
        if (len == 1) {
            sb.append(alphaHexChar(data));
            return sb.toString();
        }
        for (int i = 0; i < len - 1; i++) {
            sb.append(anyHexChar(data));
        }
        sb.append(alphaHexChar(data));
        return sb.toString();
    }

    private static String buildDecimalBase(FuzzedDataProvider data) {
        int n = data.consumeInt(-1_000_000, 1_000_000);
        String digits = Integer.toString(Math.abs(n));
        int zeros = data.consumeInt(0, 3);
        StringBuilder sb = new StringBuilder();
        if (n < 0) {
            sb.append('-');
        }
        for (int i = 0; i < zeros; i++) {
            sb.append('0');
        }
        sb.append(digits);
        return sb.toString();
    }

    private static String buildPlainIntString(FuzzedDataProvider data) {
        int n = data.consumeInt(-1_000_000, 1_000_000);
        return Integer.toString(n);
    }

    private static char anyHexChar(FuzzedDataProvider data) {
        final String hex = "0123456789abcdefABCDEF";
        return hex.charAt(data.consumeInt(0, hex.length() - 1));
    }

    private static char alphaHexChar(FuzzedDataProvider data) {
        final String hex = "abcdefABCDEF";
        return hex.charAt(data.consumeInt(0, hex.length() - 1));
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException;
    }

    private static boolean isRootCauseNumberFormat(Throwable t) {
        if (!(t instanceof NumberFormatException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String method = ste.getMethodName();
            if ("org.apache.commons.lang3.math.NumberUtils".equals(cls)) {
                if ("createNumber".equals(method)
                        || "createInteger".equals(method)
                        || "isAllZeros".equals(method)
                        || "isDigits".equals(method)
                        || "createLong".equals(method)
                        || "createBigInteger".equals(method)
                        || "createFloat".equals(method)
                        || "createDouble".equals(method)
                        || "createBigDecimal".equals(method)
                        || "charAt".equals(method)
                        || "substring".equals(method)) {
                    return true;
                }
            }
            if ("org.apache.commons.lang3.StringUtils".equals(cls) && "isBlank".equals(method)) {
                return true;
            }
        }
        return false;
    }
}
```

</details>

---
## [6] ⚙️ harness-attempt · `attempt_002`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NumberFormatException: 0Xfade is not a valid number.

---
## [7] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~38,287 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.apache.commons.lang3.math` (`package org.apache.commons.lang3.math;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Lang_16_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/lang3/math/NumberUtils.java
+++ /src/main/java/org/apache/commons/lang3/math/NumberUtils.java
@@ -542,8 +542,7 @@
                     }
                     //$FALL-THROUGH$
                 default :
-                    throw new NumberFormatException(str + " is not a valid number.");
-
+                	return createInteger(str);
             }
         } else {
             //User doesn't have a preference on the return type, so let's start

</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import java.math.BigDecimal;
import java.math.BigInteger;
import org.apache.commons.lang3.StringUtils;
</source_imports>

Function `createNumber`:
<signature>
public static Number createNumber(String str) throws NumberFormatException
</signature>
<code>
    public static Number createNumber(String str) throws NumberFormatException {
        if (str == null) {
            return null;
        }
        if (StringUtils.isBlank(str)) {
            throw new NumberFormatException("A blank string is not a valid number");
        }  
        if (str.startsWith("--")) {
            // this is protection for poorness in java.lang.BigDecimal.
            // it accepts this as a legal value, but it does not appear 
            // to be in specification of class. OS X Java parses it to 
            // a wrong value.
            return null;
        }
        if (str.startsWith("0x") || str.startsWith("-0x")) {
            return createInteger(str);
        }   
        char lastChar = str.charAt(str.length() - 1);
        String mant;
        String dec;
        String exp;
        int decPos = str.indexOf('.');
        int expPos = str.indexOf('e') + str.indexOf('E') + 1;

        if (decPos > -1) {

            if (expPos > -1) {
                if (expPos < decPos || expPos > str.length()) {
                    throw new NumberFormatException(str + " is not a valid number.");
                }
                dec = str.substring(decPos + 1, expPos);
            } else {
                dec = str.substring(decPos + 1);
            }
            mant = str.substring(0, decPos);
        } else {
            if (expPos > -1) {
                if (expPos > str.length()) {
                    throw new NumberFormatException(str + " is not a valid number.");
                }
                mant = str.substring(0, expPos);
            } else {
                mant = str;
            }
            dec = null;
        }
        if (!Character.isDigit(lastChar) && lastChar != '.') {
            if (expPos > -1 && expPos < str.length() - 1) {
                exp = str.substring(expPos + 1, str.length() - 1);
            } else {
                exp = null;
            }
            //Requesting a specific type..
            String numeric = str.substring(0, str.length() - 1);
            boolean allZeros = isAllZeros(mant) && isAllZeros(exp);
            switch (lastChar) {
                case 'l' :
                case 'L' :
                    if (dec == null
                        && exp == null
                        && (numeric.charAt(0) == '-' && isDigits(numeric.substring(1)) || isDigits(numeric))) {
                        try {
                            return createLong(numeric);
                        } catch (NumberFormatException nfe) { // NOPMD
                            // Too big for a long
                        }
                        return createBigInteger(numeric);

                    }
                    throw new NumberFormatException(str + " is not a valid number.");
                case 'f' :
                case 'F' :
                    try {
                        Float f = NumberUtils.createFloat(numeric);
                        if (!(f.isInfinite() || (f.floatValue() == 0.0F && !allZeros))) {
                            //If it's too big for a float or the float value = 0 and the string
                            //has non-zeros in it, then float does not have the precision we want
                            return f;
                        }

                    } catch (NumberFormatException nfe) { // NOPMD
                        // ignore the bad number
                    }
                    //$FALL-THROUGH$
                case 'd' :
                case 'D' :
                    try {
                        Double d = NumberUtils.createDouble(numeric);
                        if (!(d.isInfinite() || (d.floatValue() == 0.0D && !allZeros))) {
                            return d;
                        }
                    } catch (NumberFormatException nfe) { // NOPMD
                        // ignore the bad number
                    }
                    try {
                        return createBigDecimal(numeric);
                    } catch (NumberFormatException e) { // NOPMD
                        // ignore the bad number
                    }
                    //$FALL-THROUGH$
                default :
                    throw new NumberFormatException(str + " is not a valid number.");

            }
        } else {
            //User doesn't have a preference on the return type, so let's start
            //small and go from there...
            if (expPos > -1 && expPos < str.length() - 1) {
                exp = str.substring(expPos + 1, str.length());
            } else {
                exp = null;
            }
            if (dec == null && exp == null) {
                //Must be an int,long,bigint
                try {
                    return createInteger(str);
                } catch (NumberFormatException nfe) { // NOPMD
                    // ignore the bad number
                }
                try {
                    return createLong(str);
                } catch (NumberFormatException nfe) { // NOPMD
                    // ignore the bad number
                }
                return createBigInteger(str);

            } else {
                //Must be a float,double,BigDec
                boolean allZeros = isAllZeros(mant) && isAllZeros(exp);
                try {
                    Float f = createFloat(str);
                    if (!(f.isInfinite() || (f.floatValue() == 0.0F && !allZeros))) {
                        return f;
                    }
                } catch (NumberFormatException nfe) { // NOPMD
                    // ignore the bad number
                }
                try {
                    Double d = createDouble(str);
                    if (!(d.isInfinite() || (d.doubleValue() == 0.0D && !allZeros))) {
                        return d;
                    }
                } catch (NumberFormatException nfe) { // NOPMD
                    // ignore the bad number
                }

                return createBigDecimal(str);

            }
        }
    }
</code>
Call-site examples (use these as a guide for constructing the target call):
<xref>
@Test
    public void testCreateNumber() {
        // a lot of things can go wrong
        assertEquals("createNumber(String) 1 failed", Float.valueOf("1234.5"), NumberUtils.createNumber("1234.5"));
        assertEquals("createNumber(String) 2 failed", Integer.valueOf("12345"), NumberUtils.createNumber("12345"));
        assertEquals("createNumber(String) 3 failed", Double.valueOf("1234.5"), NumberUtils.createNumber("1234.5D"));
        assertEquals("createNumber(String) 3 failed", Double.valueOf("1234.5"), NumberUtils.createNumber("1234.5d"));
        assertEquals("createNumber(String) 4 failed", Float.valueOf("1234.5"), NumberUtils.createNumber("1234.5F"));
        assertEquals("createNumber(String) 4 failed", Float.valueOf("1234.5"), NumberUtils.createNumber("1234.5f"));
        assertEquals("createNumber(String) 5 failed", Long.valueOf(Integer.MAX_VALUE + 1L), NumberUtils.createNumber(""
            + (Integer.MAX_VALUE + 1L)));
        assertEquals("createNumber(String) 6 failed", Long.valueOf(12345), NumberUtils.createNumber("12345L"));
        assertEquals("createNumber(String) 6 failed", Long.valueOf(12345), NumberUtils.createNumber("12345l"));
        assertEquals("createNumber(String) 7 failed", Float.valueOf("-1234.5"), NumberUtils.createNumber("-1234.5"));
        assertEquals("createNumber(String) 8 failed", Integer.valueOf("-12345"), NumberUtils.createNumber("-12345"));
        assertTrue("createNumber(String) 9a failed", 0xFADE == NumberUtils.createNumber("0xFADE").intValue());
        assertTrue("createNumber(String) 9b failed", 0xFADE == NumberUtils.createNumber("0Xfade").intValue());
        assertTrue("createNumber(String) 10a failed", -0xFADE == NumberUtils.createNumber("-0xFADE").intValue());
        assertTrue("createNumber(String) 10b failed", -0xFADE == NumberUtils.createNumber("-0Xfade").intValue());
        assertEquals("createNumber(String) 11 failed", Double.valueOf("1.1E200"), NumberUtils.createNumber("1.1E200"));
        assertEquals("createNumber(String) 12 failed", Float.valueOf("1.1E20"), NumberUtils.createNumber("1.1E20"));
        assertEquals("createNumber(String) 13 failed", Double.valueOf("-1.1E200"), NumberUtils.createNumber("-1.1E200"));
        assertEquals("createNumber(String) 14 failed", Double.valueOf("1.1E-200"), NumberUtils.createNumber("1.1E-200"));
        assertEquals("createNumber(null) failed", null, NumberUtils.createNumber(null));
        assertEquals("createNumber(String) failed", new BigInteger("12345678901234567890"), NumberUtils
                .createNumber("12345678901234567890L"));

        // jdk 1.2 doesn't support this. unsure about jdk 1.2.2
        if (SystemUtils.isJavaVersionAtLeast(JAVA_1_3)) {
            assertEquals("createNumber(String) 15 failed", new BigDecimal("1.1E-700"), NumberUtils
                    .createNumber("1.1E-700F"));
        }
        assertEquals("createNumber(String) 16 failed", Long.valueOf("10" + Integer.MAX_VALUE), NumberUtils
                .createNumber("10" + Integer.MAX_VALUE + "L"));
        assertEquals("createNumber(String) 17 failed", Long.valueOf("10" + Integer.MAX_VALUE), NumberUtils
                .createNumber("10" + Integer.MAX_VALUE));
        assertEquals("createNumber(String) 18 failed", new BigInteger("10" + Long.MAX_VALUE), NumberUtils
                .createNumber("10" + Long.MAX_VALUE));

        // LANG-521
        assertEquals("createNumber(String) LANG-521 failed", Float.valueOf("2."), NumberUtils.createNumber("2."));

        // LANG-638
        assertFalse("createNumber(String) succeeded", checkCreateNumber("1eE"));

        // LANG-693
        assertEquals("createNumber(String) LANG-693 failed", Double.valueOf(Double.MAX_VALUE), NumberUtils
                    .createNumber("" + Double.MAX_VALUE));
    }
</xref>
<xref>
private boolean checkCreateNumber(String val) {
        try {
            Object obj = NumberUtils.createNumber(val);
            if (obj == null) {
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
       }
    }
</xref>
<xref>
@Test
    public void testLang300() {
        NumberUtils.createNumber("-1l");
        NumberUtils.createNumber("01l");
        NumberUtils.createNumber("1l");
    }
</xref>
Methods called by `createNumber` whose behaviour the patched code depends on. The body above shows how their results are USED; the declarations below show what they return and how real implementations behave. To reach the fault you usually need to drive the target through one of these implementations with an input that makes its return value exercise the patched path.
<callee name="createInteger" from="NumberUtils.java">
<signature>
public static Integer createInteger(String str)
</signature>
<code>
    public static Integer createInteger(String str) {
        if (str == null) {
            return null;
        }
        // decode() handles 0xAABD and 0777 (hex and octal) as well.
        return Integer.decode(str);
    }
</code>
</callee>
<callee name="isAllZeros" from="NumberUtils.java">
<signature>
private static boolean isAllZeros(String str)
</signature>
<code>
    private static boolean isAllZeros(String str) {
        if (str == null) {
            return true;
        }
        for (int i = str.length() - 1; i >= 0; i--) {
            if (str.charAt(i) != '0') {
                return false;
            }
        }
        return str.length() > 0;
    }
</code>
</callee>
<callee name="isDigits" from="NumberUtils.java">
<signature>
public static boolean isDigits(String str)
</signature>
<code>
    public static boolean isDigits(String str) {
        if (StringUtils.isEmpty(str)) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }
</code>
</callee>
<callee name="createLong" from="NumberUtils.java">
<signature>
public static Long createLong(String str)
</signature>
<code>
    public static Long createLong(String str) {
        if (str == null) {
            return null;
        }
        return Long.valueOf(str);
    }
</code>
</callee>
<callee name="createBigInteger" from="NumberUtils.java">
<signature>
public static BigInteger createBigInteger(String str)
</signature>
<code>
    public static BigInteger createBigInteger(String str) {
        if (str == null) {
            return null;
        }
        return new BigInteger(str);
    }
</code>
</callee>
<callee name="createFloat" from="NumberUtils.java">
<signature>
public static Float createFloat(String str)
</signature>
<code>
    public static Float createFloat(String str) {
        if (str == null) {
            return null;
        }
        return Float.valueOf(str);
    }
</code>
</callee>

The failing test below shows how to reach the bug. Use it with TWO strategies:

1. ANCHOR: call the target with the exact input(s) from the test first. This is your guaranteed crash on the buggy version.

2. EXPLORE: identify the input PROPERTY that triggers the patched line (the root cause), then use FuzzedDataProvider to generate many varied REAL inputs that satisfy that property — different lengths, positions, and surrounding content — and drive them all through the real entry point. You are testing whether the patch fixes the root cause for ALL such inputs, not just the seed; overfitting patches special-case the seed.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `NumberUtils`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
On the buggy version the root cause surfaces as: java.lang.NumberFormatException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
  - The fixed code is SUPPOSED to reject invalid input cleanly. Any throwable that is a deliberate rejection of bad input is CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput and return normally. Recognize clean rejection by exception FAMILY and context, NEVER by exact class identity or message text: a correct patch may reject the same invalid input with a DIFFERENT exception class or message than the buggy version (e.g. a specific IllegalArgumentException subclass instead of a generic one, or a null/reworded message). Any throwable in the IllegalArgumentException / NumberFormatException family — or any library-specific validation exception — raised while the code is checking its arguments counts as clean rejection.
  - PROPAGATE a throwable only when BOTH hold: (1) it signals the root cause — its class matches the ground-truth throwable below, or it is your own assertion/metamorphic RuntimeException — AND (2) its stack trace passes through `createNumber` or a function listed in <root_cause_reachable>. Any other throwable — INCLUDING the same exception class thrown from a different location — must be swallowed: it is a pre-existing defect outside this patch's scope, it will crash every version including correctly patched ones, and it produces a false positive. Enforce this in code, e.g.:
    try { /* library call */ }
    catch (RuntimeException t) {
        if (isRootCause(t)) throw t;  // else swallow
    }
  where isRootCause checks instanceof against the ground-truth throwable class AND loops over t.getStackTrace() requiring a frame whose class/method matches the patched or reachable region.
VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable is either (i) a validation/rejection exception (IllegalArgumentException or a subclass, NumberFormatException, a library 'invalid input' exception, ...) OR (ii) a GENERIC JDK runtime exception that a method commonly leaks on malformed / out-of-domain input (StringIndexOutOfBoundsException, IndexOutOfBoundsException, ArrayIndexOutOfBoundsException, NullPointerException, ClassCastException, ArithmeticException), then its signature CANNOT distinguish the bug from correct rejection — a correctly fixed version legitimately throws the same exception, possibly at the same line, when the input really is invalid. This is the #1 false-positive source: the SAME exception class leaks from the SAME method on OTHER malformed inputs that even the correct fix does not handle (a parser given a truncated or degenerate input shape often throws the identical index/format exception on buggy AND fixed alike) — so matching the ground-truth class and location is NOT enough. In that case, let it propagate ONLY for inputs that are VALID BY CONSTRUCTION: inputs a correct implementation is obligated to accept because you built them to satisfy the documented preconditions yourself (e.g. if the API requires start <= end, generate two values and order them BEFORE the call; if a parameter must be positive, force it positive; if elements must be non-null, supply non-null elements). For any input whose validity you cannot guarantee, catch the rejection and return normally — a rejection of a possibly-invalid input proves nothing.
Rule of thumb: if a careful, correct version of this method would still throw that exception for that input, it is NOT a bug — swallow it. Only when the method throws on an input it was obliged to handle has it lost control of its own invariants — let that propagate.
GROUND-TRUTH CRASH (captured by running the trigger test on the buggy version — this is the verified failure, trust it over anything inferred from the test body):
<ground_truth_crash>
throwable: java.lang.NumberFormatException
message: 0Xfade is not a valid number.
thrown_at: org.apache.commons.lang3.math.NumberUtils.createNumber(NumberUtils.java:545)
anchor_input: "1234.5"  // hard-code this verbatim as your first call, then fuzz inputs of the same shape
other_observed_literals: "12345", "1234.5D", "1234.5d", "1234.5F", "1234.5f", "12345L", "12345l", "-1234.5", "-12345", "0xFADE", "0Xfade", "-0xFADE", "-0Xfade", "1.1E200", "1.1E20", "-1.1E200", "1.1E-200", "12345678901234567890L", "1.1E-700F", "2."
</ground_truth_crash>
<failing_test class="org.apache.commons.lang3.math.NumberUtilsTest" method="testCreateNumber">
    @Test
    public void testCreateNumber() {
        // a lot of things can go wrong
        assertEquals("createNumber(String) 1 failed", Float.valueOf("1234.5"), NumberUtils.createNumber("1234.5"));
        assertEquals("createNumber(String) 2 failed", Integer.valueOf("12345"), NumberUtils.createNumber("12345"));
        assertEquals("createNumber(String) 3 failed", Double.valueOf("1234.5"), NumberUtils.createNumber("1234.5D"));
        assertEquals("createNumber(String) 3 failed", Double.valueOf("1234.5"), NumberUtils.createNumber("1234.5d"));
        assertEquals("createNumber(String) 4 failed", Float.valueOf("1234.5"), NumberUtils.createNumber("1234.5F"));
        assertEquals("createNumber(String) 4 failed", Float.valueOf("1234.5"), NumberUtils.createNumber("1234.5f"));
        assertEquals("createNumber(String) 5 failed", Long.valueOf(Integer.MAX_VALUE + 1L), NumberUtils.createNumber(""
            + (Integer.MAX_VALUE + 1L)));
        assertEquals("createNumber(String) 6 failed", Long.valueOf(12345), NumberUtils.createNumber("12345L"));
        assertEquals("createNumber(String) 6 failed", Long.valueOf(12345), NumberUtils.createNumber("12345l"));
        assertEquals("createNumber(String) 7 failed", Float.valueOf("-1234.5"), NumberUtils.createNumber("-1234.5"));
        assertEquals("createNumber(String) 8 failed", Integer.valueOf("-12345"), NumberUtils.createNumber("-12345"));
        assertTrue("createNumber(String) 9a failed", 0xFADE == NumberUtils.createNumber("
        // ... (truncated)
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.lang3.math.NumberUtilsTest::testCreateNumber
java.lang.NumberFormatException: 0Xfade is not a valid number.
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.
The test method depends on the following from its test class — setUp, helper methods, constants, fixture files. Replicate this environment EXACTLY (register the same files, wire the same providers, use the same constants); do NOT improvise setup the test performs differently, and DROP any check whose setup you cannot replicate:
<test_support class="org.apache.commons.lang3.math.NumberUtilsTest">
// --- helper checkCreateNumber() ---
    private boolean checkCreateNumber(String val) {
        try {
            Object obj = NumberUtils.createNumber(val);
            if (obj == null) {
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
       }
    }
</test_support>

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  max(long[] array) / (int[] array) / (short[] array) / (byte[] array) / (double[] array) / (float[] array) / (long a, long b, long c) / (int a, int b, int c) / (short a, short b, short c) / (byte a, byte b, byte c) / (double a, double b, double c) / (float a, float b, float c)
  min(long[] array) / (int[] array) / (short[] array) / (byte[] array) / (double[] array) / (float[] array) / (long a, long b, long c) / (int a, int b, int c) / (short a, short b, short c) / (byte a, byte b, byte c) / (double a, double b, double c) / (float a, float b, float c)
  toByte(String str) / (String str, byte defaultValue)
  toDouble(String str) / (String str, double defaultValue)
  toFloat(String str) / (String str, float defaultValue)
  toInt(String str) / (String str, int defaultValue)
  toLong(String str) / (String str, long defaultValue)
  toShort(String str) / (String str, short defaultValue)

METHOD FAMILIES over the same input space (factory/parser siblings — where the docs state a selection or agreement rule between family members, equivalent inputs must respect it):
  create* family: createBigDecimal, createBigInteger, createDouble, createFloat, createInteger, createLong, createNumber
  to* family: toByte, toDouble, toFloat, toInt, toLong, toShort

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- StringUtils.isBlank
- NumberUtils.createInteger
- NumberUtils.isAllZeros
- NumberUtils.charAt
- NumberUtils.substring
- NumberUtils.isDigits
- NumberUtils.createLong
- NumberUtils.createBigInteger
- NumberUtils.createFloat
- NumberUtils.createDouble
- NumberUtils.createBigDecimal
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.lang3.math.FuzzHarness.checkUppercaseHexAgainstCreateInteger
- org.apache.commons.lang3.math.FuzzHarness.checkValidHexAgreement
- org.apache.commons.lang3.math.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.lang3.math.NumberUtils.createNumber
Crashes already found:
- java.lang.NumberFormatException@org.apache.commons.lang3.math.NumberUtils.createNumber
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- StringUtils.isBlank
- NumberUtils.createInteger
- NumberUtils.isAllZeros
- NumberUtils.charAt
- NumberUtils.substring
- NumberUtils.isDigits
- NumberUtils.createLong
- NumberUtils.createBigInteger
- NumberUtils.createFloat
- NumberUtils.createDouble
- NumberUtils.createBigDecimal
Check FAMILIES already covered by accepted harnesses: {decimal-float, hex-agree, hex-case, long-suffix, plain-int, upper-x-int}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.NumberFormatException@org.apache.commons.lang3.math.NumberUtils.createNumber. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
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
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~3,323 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            NumberUtils.createNumber("1234.5");
        } catch (RuntimeException ignored) {
        }

        int extraLen = data.consumeInt(0, 7);
        boolean negative = data.consumeBoolean();
        boolean useEightDigits = data.consumeBoolean();

        String hexDigits = "0123456789abcdefABCDEF";
        String specialLast = "dDeEfF";

        StringBuilder sb = new StringBuilder();
        if (negative) {
            sb.append('-');
        }
        sb.append('0').append('X');

        if (useEightDigits) {
            String first8 = "01234567";
            sb.append(first8.charAt(data.consumeInt(0, first8.length() - 1)));
            for (int i = 0; i < extraLen; i++) {
                sb.append(hexDigits.charAt(data.consumeInt(0, hexDigits.length() - 1)));
            }
        } else {
            int prefixLen = extraLen;
            for (int i = 0; i < prefixLen; i++) {
                sb.append(hexDigits.charAt(data.consumeInt(0, hexDigits.length() - 1)));
            }
        }

        sb.append(specialLast.charAt(data.consumeInt(0, specialLast.length() - 1)));
        String s = sb.toString();

        if (s.equals("0X") || s.equals("-0X")) {
            return;
        }

        Integer expected;
        try {
            expected = NumberUtils.createInteger(s);
        } catch (RuntimeException invalidForDecode) {
            return;
        }

        boolean accepted;
        try {
            Object obj = NumberUtils.createNumber(s);
            accepted = obj != null;
        } catch (NumberFormatException e) {
            accepted = false;
        }

        /*
         * Contract asserted:
         * - The library tests show createNumber("0Xfade") must succeed.
         * - createInteger delegates to Integer.decode, whose comment explicitly says it handles 0xAABD.
         * For any valid-by-construction uppercase-0X hex string that Integer.decode accepts, createNumber
         * must also accept it and produce the same integer value. A patch that merely suppresses one throw
         * or special-cases one seed will violate at least one of these relations for other hex strings,
         * especially those ending in d/e/f that first flow through createFloat/createDouble/createBigDecimal.
         */
        if (!accepted) {
            throw new RuntimeException("[oracle:upperx-suffix-accept] metamorphic violation: createNumber rejected valid uppercase-0X hex input=" + s + " expected=" + expected);
        }

        try {
            Number actual = NumberUtils.createNumber(s);
            if (actual == null || actual.intValue() != expected.intValue()) {
                throw new RuntimeException("[oracle:upperx-suffix-value] metamorphic violation: createNumber/createInteger disagree input=" + s + " lhs=" + actual + " rhs=" + expected);
            }
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("[oracle:upperx-suffix-value] metamorphic violation: createNumber threw on valid uppercase-0X hex input=" + s + " expected=" + expected, e);
        }
    }
}
```

</details>

---
## [8] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `accepted` (literal assignment, no rethrow, the caught excep

---
## [9] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~4,471 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [7] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            NumberUtils.createNumber("1234.5");
        } catch (RuntimeException ignored) {
        }

        int extraLen = data.consumeInt(0, 7);
        boolean negative = data.consumeBoolean();
        boolean useEightDigits = data.consumeBoolean();

        String hexDigits = "0123456789abcdefABCDEF";
        String specialLast = "dDeEfF";

        StringBuilder sb = new StringBuilder();
        if (negative) {
            sb.append('-');
        }
        sb.append('0').append('X');

        if (useEightDigits) {
            String first8 = "01234567";
            sb.append(first8.charAt(data.consumeInt(0, first8.length() - 1)));
            for (int i = 0; i < extraLen; i++) {
                sb.append(hexDigits.charAt(data.consumeInt(0, hexDigits.length() - 1)));
            }
        } else {
            int prefixLen = extraLen;
            for (int i = 0; i < prefixLen; i++) {
                sb.append(hexDigits.charAt(data.consumeInt(0, hexDigits.length() - 1)));
            }
        }

        sb.append(specialLast.charAt(data.consumeInt(0, specialLast.length() - 1)));
        String s = sb.toString();

        if (s.equals("0X") || s.equals("-0X")) {
            return;
        }

        Integer expected;
        try {
            expected = NumberUtils.createInteger(s);
        } catch (RuntimeException invalidForDecode) {
            return;
        }

        boolean accepted;
        try {
            Object obj = NumberUtils.createNumber(s);
            accepted = obj != null;
        } catch (NumberFormatException e) {
            accepted = false;
        }

        /*
         * Contract asserted:
         * - The library tests show createNumber("0Xfade") must succeed.
         * - createInteger delegates to Integer.decode, whose comment explicitly says it handles 0xAABD.
         * For any valid-by-construction uppercase-0X hex string that Integer.decode accepts, createNumber
         * must also accept it and produce the same integer value. A patch that merely suppresses one throw
         * or special-cases one seed will violate at least one of these relations for other hex strings,
         * especially those ending in d/e/f that first flow through createFloat/createDouble/createBigDecimal.
         */
        if (!accepted) {
            throw new RuntimeException("[oracle:upperx-suffix-accept] metamorphic violation: createNumber rejected valid uppercase-0X hex input=" + s + " expected=" + expected);
        }

        try {
            Number actual = NumberUtils.createNumber(s);
            if (actual == null || actual.intValue() != expected.intValue()) {
                throw new RuntimeException("[oracle:upperx-suffix-value] metamorphic violation: createNumber/createInteger disagree input=" + s + " lhs=" + actual + " rhs=" + expected);
            }
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("[oracle:upperx-suffix-value] metamorphic violation: createNumber threw on valid uppercase-0X hex input=" + s + " expected=" + expected, e);
        }
    }
}
```
**[user]**
```
Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `accepted` (literal assignment, no rethrow, the caught exception is never used) and the alarm near line 62 is conditioned on `accepted` — the caught exception's identity is destroyed, so attribution cannot tell WHICH exception fired or whether the unpatched build crashes the same way. Rethrow the exception as the alarm's cause (`..., e`), or assert on the real observable instead of a swallowed boolean.
A caught exception is a REJECTION, never a violation, and flattening it into a flag destroys the identity attribution needs. Either assert on the real observable the test pins (compute the actual value and compare it), or if you must re-report a caught crash, re-throw it with the original as the cause: `new FuzzerSecurityIssueLow("[oracle:<id>] ...", e)`. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~4,991 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            Number anchor = NumberUtils.createNumber("1234.5");
            if (anchor == null || anchor.floatValue() != 1234.5f) {
                throw new RuntimeException("[oracle:anchor-basic] metamorphic violation: createNumber(\"1234.5\") returned " + anchor);
            }
        } catch (RuntimeException ignored) {
        }

        String s = buildUppercaseHexCandidate(data);
        if (s.equals("0X") || s.equals("-0X")) {
            return;
        }

        Integer expected;
        try {
            expected = NumberUtils.createInteger(s);
        } catch (RuntimeException invalidForDecode) {
            return;
        }

        /*
         * Contract asserted:
         * createInteger delegates to Integer.decode and explicitly handles 0x/0X-prefixed integers.
         * For any valid uppercase-0X hex string that createInteger accepts, createNumber must accept the
         * same real input and denote the same integer value. This is an equivalent-input/family-agreement
         * relation between real library entry points, so a throw-deleting or seed-only patch still fails.
         */
        final Number actual;
        try {
            actual = NumberUtils.createNumber(s);
        } catch (RuntimeException e) {
            throw new RuntimeException(
                "[oracle:upperx-family] metamorphic violation: createNumber rejected valid uppercase-0X hex input=" + s + " expected=" + expected,
                e
            );
        }

        if (actual == null) {
            throw new RuntimeException(
                "[oracle:upperx-family] metamorphic violation: createNumber returned null for valid uppercase-0X hex input=" + s + " expected=" + expected
            );
        }

        if (actual.intValue() != expected.intValue()) {
            throw new RuntimeException(
                "[oracle:upperx-family] metamorphic violation: createNumber/createInteger disagree input=" + s + " lhs=" + actual + " rhs=" + expected
            );
        }

        /*
         * Independent oracle:
         * Hex digits are case-insensitive for Integer.decode-style parsing. Therefore replacing only the
         * post-prefix payload with lowercase must preserve the numeric value for valid hex inputs.
         * Both sides are computed via real library calls and the check is skipped if either side rejects.
         */
        String lowered = lowerHexPayload(s);
        try {
            Integer loweredExpected = NumberUtils.createInteger(lowered);
            Number loweredActual = NumberUtils.createNumber(lowered);
            if (loweredActual == null) {
                throw new RuntimeException(
                    "[oracle:case-payload] metamorphic violation: createNumber returned null input=" + lowered + " expected=" + loweredExpected
                );
            }
            if (loweredExpected.intValue() != expected.intValue() || loweredActual.intValue() != actual.intValue()) {
                throw new RuntimeException(
                    "[oracle:case-payload] metamorphic violation: case-equivalent hex inputs disagree original=" + s
                        + " lowered=" + lowered + " origExpected=" + expected + " loweredExpected=" + loweredExpected
                        + " origActual=" + actual + " loweredActual=" + loweredActual
                );
            }
        } catch (RuntimeException ignored) {
        }
    }

    private static String buildUppercaseHexCandidate(FuzzedDataProvider data) {
        boolean negative = data.consumeBoolean();
        boolean forceSuffixBranch = data.consumeBoolean();
        int bodyLen = data.consumeInt(1, 8);

        String middleDigits = "0123456789abcdefABCDEF";
        String suffixyDigits = "dDeEfF";
        StringBuilder sb = new StringBuilder();

        if (negative) {
            sb.append('-');
        }
        sb.append('0').append('X');

        if (bodyLen == 1) {
            sb.append(suffixyDigits.charAt(data.consumeInt(0, suffixyDigits.length() - 1)));
            return sb.toString();
        }

        for (int i = 0; i < bodyLen - 1; i++) {
            sb.append(middleDigits.charAt(data.consumeInt(0, middleDigits.length() - 1)));
        }

        if (forceSuffixBranch) {
            sb.append(suffixyDigits.charAt(data.consumeInt(0, suffixyDigits.length() - 1)));
        } else {
            sb.append(middleDigits.charAt(data.consumeInt(0, middleDigits.length() - 1)));
        }
        return sb.toString();
    }

    private static String lowerHexPayload(String s) {
        int start = s.startsWith("-0X") ? 3 : 2;
        if (s.length() <= start) {
            return s;
        }
        return s.substring(0, start) + s.substring(start).toLowerCase();
    }
}
```

</details>

---
## [10] ⚙️ harness-attempt · `attempt_004`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.RuntimeException: [oracle:upperx-family] metamorphic violation: createNumber rejected valid uppercase-0X hex input=0Xd expected=13

---
## [11] ⚙️ corpus-seed · `attempt_001`
**output:** **96 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['createNumber(String) 1 failed', '1234.5', 'createNumber(String) 2 failed', '12345', 'createNumber(String) 3 failed', '1234.5D', '1234.5d', 'createNumber(String) 4 failed']}

---
## [12] ⚙️ corpus-seed · `attempt_002`
**output:** **96 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['createNumber(String) 1 failed', '1234.5', 'createNumber(String) 2 failed', '12345', 'createNumber(String) 3 failed', '1234.5D', '1234.5d', 'createNumber(String) 4 failed']}

---
## [13] ⚙️ corpus-seed · `attempt_004`
**output:** **96 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['createNumber(String) 1 failed', '1234.5', 'createNumber(String) 2 failed', '12345', 'createNumber(String) 3 failed', '1234.5D', '1234.5d', 'createNumber(String) 4 failed']}

---
## [14] ⚙️ patched-fuzz · `attempt_001`
**output:** **FIRED — crash on patched build**
- reproducing_input_file: /tmp/d4j/Lang_16_buggy/fuzz/attempt_001/crashes/crash-f210f177238ed8b734600aa12afcd7e2a520d1bf

---
## [15] ⚙️ patched-fuzz · `attempt_002`
**output:** **FIRED — crash on patched build**
- reproducing_input_file: /tmp/d4j/Lang_16_buggy/fuzz/attempt_002/crashes/crash-7011d83ec98b26987bdf0c873b7823177799b375

---
## [16] ⚙️ patched-fuzz · `attempt_004`
**output:** **FIRED — [oracle:upperx-family] metamorphic violation: createNumber rejected valid uppercase-0X hex input=-0Xe00 expected=-3584**
- reproducing_input_file: /tmp/d4j/Lang_16_buggy/fuzz/attempt_004/crashes/crash-fd8c84304a55ac401bac36d1263e6add10193d86
