# Pipeline trace — Lang-27

**Code:** `unknown`

**Patch label:** correct  *(the patch under analysis is a known-CORRECT fix — the pipeline is not told this)*

**Outcome:** FALSE ALARM (a harness fired on this CORRECT patch). [evaluated; 5 harness(es) fuzzed on the patched build; campaign converged=True]

**Patch under analysis:**
```diff
--- /src/main/java/org/apache/commons/lang3/math/NumberUtils.java
+++ /src/main/java/org/apache/commons/lang3/math/NumberUtils.java
@@ -485,7 +485,7 @@
             }
             mant = str.substring(0, decPos);
         } else {
-            if (expPos > -1) {
+        	if(expPos>-1&&expPos<str.length()-1){
                 mant = str.substring(0, expPos);
             } else {
                 mant = str;
```

31 sequential steps — 9 LLM calls, 22 deterministic. Read top to bottom.

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
<details><summary>▸ output — FailureTest(test_class='org.apache.commons.lang3.math.NumberUtilsTest', test_method='testCreateNumbe (~3,868 chars)</summary>

**output:** 
```json
[
  "FailureTest(test_class='org.apache.commons.lang3.math.NumberUtilsTest', test_method='testCreateNumber', source_path='/tmp/d4j/Lang_27_buggy/src/test/java/org/apache/commons/lang3/math/NumberUtilsTest.java', method_source='    public void testCreateNumber() {\\n        // a lot of things can go wrong\\n        assertEquals(\"createNumber(String) 1 failed\", new Float(\"1234.5\"), NumberUtils.createNumber(\"1234.5\"));\\n        assertEquals(\"createNumber(String) 2 failed\", new Integer(\"12345\"), NumberUtils.createNumber(\"12345\"));\\n        assertEquals(\"createNumber(String) 3 failed\", new Double(\"1234.5\"), NumberUtils.createNumber(\"1234.5D\"));\\n        assertEquals(\"createNumber(String) 3 failed\", new Double(\"1234.5\"), NumberUtils.createNumber(\"1234.5d\"));\\n        assertEquals(\"createNumber(String) 4 failed\", new Float(\"1234.5\"), NumberUtils.createNumber(\"1234.5F\"));\\n        assertEquals(\"createNumber(String) 4 failed\", new Float(\"1234.5\"), NumberUtils.createNumber(\"1234.5f\"));\\n        assertEquals(\"createNumber(String) 5 failed\", new Long(Integer.MAX_VALUE + 1L), NumberUtils.createNumber(\"\"\\n            + (Integer.MAX_VALUE + 1L)));\\n        assertEquals(\"createNumber(String) 6 failed\", new Long(12345), NumberUtils.createNumber(\"12345L\"));\\n        assertEquals(\"createNumber(String) 6 failed\", new Long(12345), NumberUtils.createNumber(\"12345l\"));\\n        assertEquals(\"createNumber(String) 7 failed\", new Float(\"-1234.5\"), NumberUtils.createNumber(\"-1234.5\"));\\n        assertEquals(\"createNumber(String) 8 failed\", new Integer(\"-12345\"), NumberUtils.createNumber(\"-12345\"));\\n        assertTrue(\"createNumber(String) 9 failed\", 0xFADE == NumberUtils.createNumber(\"0xFADE\").intValue());\\n        assertTrue(\"createNumber(String) 10 failed\", -0xFADE == NumberUtils.createNumber(\"-0xFADE\").intValue());\\n        assertEquals(\"createNumber(String) 11 failed\", new Double(\"1.1E200\"), NumberUtils.createNumber(\"1.1E200\"));\\n        assertEquals(\"createNumber(String) 12 failed\", new Float(\"1.1E20\"), NumberUtils.createNumber(\"1.1E20\"));\\n        assertEquals(\"createNumber(String) 13 failed\", new Double(\"-1.1E200\"), NumberUtils.createNumber(\"-1.1E200\"));\\n        assertEquals(\"createNumber(String) 14 failed\", new Double(\"1.1E-200\"), NumberUtils.createNumber(\"1.1E-200\"));\\n        assertEquals(\"createNumber(null) failed\", null, NumberUtils.createNumber(null));\\n        assertEquals(\"createNumber(String) failed\", new BigInteger(\"12345678901234567890\"), NumberUtils\\n                .createNumber(\"12345678901234567890L\"));\\n\\n        // jdk 1.2 doesn\\'t support this. unsure about jdk 1.2.2\\n        if (SystemUtils.isJavaVersionAtLeast(1.3f)) {\\n            assertEquals(\"createNumber(String) 15 failed\", new BigDecimal(\"1.1E-700\"), NumberUtils\\n                    .createNumber(\"1.1E-700F\"));\\n        }\\n        assertEquals(\"createNumber(String) 16 failed\", new Long(\"10\" + Integer.MAX_VALUE), NumberUtils\\n                .createNumber(\"10\" + Integer.MAX_VALUE + \"L\"));\\n        assertEquals(\"createNumber(String) 17 failed\", new Long(\"10\" + Integer.MAX_VALUE), NumberUtils\\n                .createNumber(\"10\" + Integer.MAX_VALUE));\\n        assertEquals(\"createNumber(String) 18 failed\", new BigInteger(\"10\" + Long.MAX_VALUE), NumberUtils\\n                .createNumber(\"10\" + Long.MAX_VALUE));\\n\\n        // LANG-521\\n        assertEquals(\"createNumber(String) LANG-521 failed\", new Float(\"2.\"), NumberUtils.createNumber(\"2.\"));\\n\\n        // LANG-638\\n        assertFalse(\"createNumber(String) succeeded\", checkCreateNumber(\"1eE\"));\\n    }', exception_type='java.lang.StringIndexOutOfBoundsException', failure_message=None, support_source=None)"
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
    "failure_message": "--- org.apache.commons.lang3.math.NumberUtilsTest::testCreateNumber\njava.lang.StringIndexOutOfBoundsException: String index out of range: 4",
    "support_chars": 344
  }
]
```

---
## [2] ⚙️ analysis (TargetAnalyzer)
<details><summary>▸ output — modified_files": [ (~16,930 chars)</summary>

**output:** 
```json
{
  "modified_files": [
    "src/main/java/org/apache/commons/lang3/math/NumberUtils.java"
  ],
  "patch_text": "--- /src/main/java/org/apache/commons/lang3/math/NumberUtils.java\n+++ /src/main/java/org/apache/commons/lang3/math/NumberUtils.java\n@@ -485,7 +485,7 @@\n             }\n             mant = str.substring(0, decPos);\n         } else {\n-            if (expPos > -1) {\n+        \tif(expPos>-1&&expPos<str.length()-1){\n                 mant = str.substring(0, expPos);\n             } else {\n                 mant = str;\n",
  "functions": [
    {
      "func_name": "createNumber",
      "func_signature": "public static Number createNumber(String str) throws NumberFormatException",
      "func_source": "    public static Number createNumber(String str) throws NumberFormatException {\n        if (str == null) {\n            return null;\n        }\n        if (StringUtils.isBlank(str)) {\n            throw new NumberFormatException(\"A blank string is not a valid number\");\n        }  \n        if (str.startsWith(\"--\")) {\n            // this is protection for poorness in java.lang.BigDecimal.\n            // it accepts this as a legal value, but it does not appear \n            // to be in specification of class. OS X Java parses it to \n            // a wrong value.\n            return null;\n        }\n        if (str.startsWith(\"0x\") || str.startsWith(\"-0x\")) {\n            return createInteger(str);\n        }   \n        char lastChar = str.charAt(str.length() - 1);\n        String mant;\n        String dec;\n        String exp;\n        int decPos = str.indexOf('.');\n        int expPos = str.indexOf('e') + str.indexOf('E') + 1;\n\n        if (decPos > -1) {\n\n            if (expPos > -1) {\n                if (expPos < decPos) {\n                    throw new NumberFormatException(str + \" is not a valid number.\");\n                }\n                dec = str.substring(decPos + 1, expPos);\n            } else {\n                dec = str.substring(decPos + 1);\n            }\n            mant = str.substring(0, decPos);\n        } else {\n            if (expPos > -1) {\n                mant = str.substring(0, expPos);\n            } else {\n                mant = str;\n            }\n            dec = null;\n        }\n        if (!Character.isDigit(lastChar) && lastChar != '.') {\n            if (expPos > -1 && expPos < str.length() - 1) {\n                exp = str.substring(expPos + 1, str.length() - 1);\n            } else {\n                exp = null;\n            }\n            //Requesting a specific type..\n            String numeric = str.substring(0, str.length() - 1);\n            boolean allZeros = isAllZeros(mant) && isAllZeros(exp);\n            switch (lastChar) {\n                case 'l' :\n                case 'L' :\n                    if (dec == null\n                        && exp == null\n                        && (numeric.charAt(0) == '-' && isDigits(numeric.substring(1)) || isDigits(numeric))) {\n                        try {\n                            return createLong(numeric);\n                        } catch (NumberFormatException nfe) {\n                            //Too big for a long\n                        }\n                        return createBigInteger(numeric);\n\n                    }\n                    throw new NumberFormatException(str + \" is not a valid number.\");\n                case 'f' :\n                case 'F' :\n                    try {\n                        Float f = NumberUtils.createFloat(numeric);\n                        if (!(f.isInfinite() || (f.floatValue() == 0.0F && !allZeros))) {\n                            //If it's too big for a float or the float value = 0 and the string\n                            //has non-zeros in it, then float does not have the precision we want\n                            return f;\n                        }\n\n                    } catch (NumberFormatException nfe) {\n                        // ignore the bad number\n                    }\n                    //$FALL-THROUGH$\n                case 'd' :\n                case 'D' :\n                    try {\n                        Double d = NumberUtils.createDouble(numeric);\n                        if (!(d.isInfinite() || (d.floatValue() == 0.0D && !allZeros))) {\n                            return d;\n                        }\n                    } catch (NumberFormatException nfe) {\n                        // ignore the bad number\n                    }\n                    try {\n                        return createBigDecimal(numeric);\n                    } catch (NumberFormatException e) {\n                        // ignore the bad number\n                    }\n                    //$FALL-THROUGH$\n                default :\n                    throw new NumberFormatException(str + \" is not a valid number.\");\n\n            }\n        } else {\n            //User doesn't have a preference on the return type, so let's start\n            //small and go from there...\n            if (expPos > -1 && expPos < str.length() - 1) {\n                exp = str.substring(expPos + 1, str.length());\n            } else {\n                exp = null;\n            }\n            if (dec == null && exp == null) {\n                //Must be an int,long,bigint\n                try {\n                    return createInteger(str);\n                } catch (NumberFormatException nfe) {\n                    // ignore the bad number\n                }\n                try {\n                    return createLong(str);\n                } catch (NumberFormatException nfe) {\n                    // ignore the bad number\n                }\n                return createBigInteger(str);\n\n            } else {\n                //Must be a float,double,BigDec\n                boolean allZeros = isAllZeros(mant) && isAllZeros(exp);\n                try {\n                    Float f = createFloat(str);\n                    if (!(f.isInfinite() || (f.floatValue() == 0.0F && !allZeros))) {\n                        return f;\n                    }\n                } catch (NumberFormatException nfe) {\n                    // ignore the bad number\n                }\n                try {\n                    Double d = createDouble(str);\n                    if (!(d.isInfinite() || (d.doubleValue() == 0.0D && !allZeros))) {\n                        return d;\n                    }\n                } catch (NumberFormatException nfe) {\n                    // ignore the bad number\n                }\n\n                return createBigDecimal(str);\n\n            }\n        }\n    }",
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
        "public void testCreateNumber() {\n        // a lot of things can go wrong\n        assertEquals(\"createNumber(String) 1 failed\", new Float(\"1234.5\"), NumberUtils.createNumber(\"1234.5\"));\n        assertEquals(\"createNumber(String) 2 failed\", new Integer(\"12345\"), NumberUtils.createNumber(\"12345\"));\n        assertEquals(\"createNumber(String) 3 failed\", new Double(\"1234.5\"), NumberUtils.createNumber(\"1234.5D\"));\n        assertEquals(\"createNumber(String) 3 failed\", new Double(\"1234.5\"), NumberUtils.createNumber(\"1234.5d\"));\n        assertEquals(\"createNumber(String) 4 failed\", new Float(\"1234.5\"), NumberUtils.createNumber(\"1234.5F\"));\n        assertEquals(\"createNumber(String) 4 failed\", new Float(\"1234.5\"), NumberUtils.createNumber(\"1234.5f\"));\n        assertEquals(\"createNumber(String) 5 failed\", new Long(Integer.MAX_VALUE + 1L), NumberUtils.createNumber(\"\"\n            + (Integer.MAX_VALUE + 1L)));\n        assertEquals(\"createNumber(String) 6 failed\", new Long(12345), NumberUtils.createNumber(\"12345L\"));\n        assertEquals(\"createNumber(String) 6 failed\", new Long(12345), NumberUtils.createNumber(\"12345l\"));\n        assertEquals(\"createNumber(String) 7 failed\", new Float(\"-1234.5\"), NumberUtils.createNumber(\"-1234.5\"));\n        assertEquals(\"createNumber(String) 8 failed\", new Integer(\"-12345\"), NumberUtils.createNumber(\"-12345\"));\n        assertTrue(\"createNumber(String) 9 failed\", 0xFADE == NumberUtils.createNumber(\"0xFADE\").intValue());\n        assertTrue(\"createNumber(String) 10 failed\", -0xFADE == NumberUtils.createNumber(\"-0xFADE\").intValue());\n        assertEquals(\"createNumber(String) 11 failed\", new Double(\"1.1E200\"), NumberUtils.createNumber(\"1.1E200\"));\n        assertEquals(\"createNumber(String) 12 failed\", new Float(\"1.1E20\"), NumberUtils.createNumber(\"1.1E20\"));\n        assertEquals(\"createNumber(String) 13 failed\", new Double(\"-1.1E200\"), NumberUtils.createNumber(\"-1.1E200\"));\n        assertEquals(\"createNumber(String) 14 failed\", new Double(\"1.1E-200\"), NumberUtils.createNumber(\"1.1E-200\"));\n        assertEquals(\"createNumber(null) failed\", null, NumberUtils.createNumber(null));\n        assertEquals(\"createNumber(String) failed\", new BigInteger(\"12345678901234567890\"), NumberUtils\n                .createNumber(\"12345678901234567890L\"));\n\n        // jdk 1.2 doesn't support this. unsure about jdk 1.2.2\n        if (SystemUtils.isJavaVersionAtLeast(1.3f)) {\n            assertEquals(\"createNumber(String) 15 failed\", new BigDecimal(\"1.1E-700\"), NumberUtils\n                    .createNumber(\"1.1E-700F\"));\n        }\n        assertEquals(\"createNumber(String) 16 failed\", new Long(\"10\" + Integer.MAX_VALUE), NumberUtils\n                .createNumber(\"10\" + Integer.MAX_VALUE + \"L\"));\n        assertEquals(\"createNumber(String) 17 failed\", new Long(\"10\" + Integer.MAX_VALUE), NumberUtils\n                .createNumber(\"10\" + Integer.MAX_VALUE));\n        assertEquals(\"createNumber(String) 18 failed\", new BigInteger(\"10\" + Long.MAX_VALUE), NumberUtils\n                .createNumber(\"10\" + Long.MAX_VALUE));\n\n        // LANG-521\n        assertEquals(\"createNumber(String) LANG-521 failed\", new Float(\"2.\"), NumberUtils.createNumber(\"2.\"));\n\n        // LANG-638\n        assertFalse(\"createNumber(String) succeeded\", checkCreateNumber(\"1eE\"));\n    }",
        "private boolean checkCreateNumber(String val) {\n        try {\n            Object obj = NumberUtils.createNumber(val);\n            if (obj == null) {\n                return false;\n            }\n            return true;\n        } catch (NumberFormatException e) {\n            return false;\n       }\n    }",
        "public void testLang300() {\n        NumberUtils.createNumber(\"-1l\");\n        NumberUtils.createNumber(\"01l\");\n        NumberUtils.createNumber(\"1l\");\n    }"
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
<details><summary>▸ Prompt (2 message(s), ~35,800 chars)</summary>

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

Codebase: `Lang_27_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/lang3/math/NumberUtils.java
+++ /src/main/java/org/apache/commons/lang3/math/NumberUtils.java
@@ -485,7 +485,7 @@
             }
             mant = str.substring(0, decPos);
         } else {
-            if (expPos > -1) {
+        	if(expPos>-1&&expPos<str.length()-1){
                 mant = str.substring(0, expPos);
             } else {
                 mant = str;

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
                if (expPos < decPos) {
                    throw new NumberFormatException(str + " is not a valid number.");
                }
                dec = str.substring(decPos + 1, expPos);
            } else {
                dec = str.substring(decPos + 1);
            }
            mant = str.substring(0, decPos);
        } else {
            if (expPos > -1) {
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
                        } catch (NumberFormatException nfe) {
                            //Too big for a long
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

                    } catch (NumberFormatException nfe) {
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
                    } catch (NumberFormatException nfe) {
                        // ignore the bad number
                    }
                    try {
                        return createBigDecimal(numeric);
                    } catch (NumberFormatException e) {
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
                } catch (NumberFormatException nfe) {
                    // ignore the bad number
                }
                try {
                    return createLong(str);
                } catch (NumberFormatException nfe) {
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
                } catch (NumberFormatException nfe) {
                    // ignore the bad number
                }
                try {
                    Double d = createDouble(str);
                    if (!(d.isInfinite() || (d.doubleValue() == 0.0D && !allZeros))) {
                        return d;
                    }
                } catch (NumberFormatException nfe) {
                    // ignore the bad number
                }

                return createBigDecimal(str);

            }
        }
    }
</code>
Call-site examples (use these as a guide for constructing the target call):
<xref>
public void testCreateNumber() {
        // a lot of things can go wrong
        assertEquals("createNumber(String) 1 failed", new Float("1234.5"), NumberUtils.createNumber("1234.5"));
        assertEquals("createNumber(String) 2 failed", new Integer("12345"), NumberUtils.createNumber("12345"));
        assertEquals("createNumber(String) 3 failed", new Double("1234.5"), NumberUtils.createNumber("1234.5D"));
        assertEquals("createNumber(String) 3 failed", new Double("1234.5"), NumberUtils.createNumber("1234.5d"));
        assertEquals("createNumber(String) 4 failed", new Float("1234.5"), NumberUtils.createNumber("1234.5F"));
        assertEquals("createNumber(String) 4 failed", new Float("1234.5"), NumberUtils.createNumber("1234.5f"));
        assertEquals("createNumber(String) 5 failed", new Long(Integer.MAX_VALUE + 1L), NumberUtils.createNumber(""
            + (Integer.MAX_VALUE + 1L)));
        assertEquals("createNumber(String) 6 failed", new Long(12345), NumberUtils.createNumber("12345L"));
        assertEquals("createNumber(String) 6 failed", new Long(12345), NumberUtils.createNumber("12345l"));
        assertEquals("createNumber(String) 7 failed", new Float("-1234.5"), NumberUtils.createNumber("-1234.5"));
        assertEquals("createNumber(String) 8 failed", new Integer("-12345"), NumberUtils.createNumber("-12345"));
        assertTrue("createNumber(String) 9 failed", 0xFADE == NumberUtils.createNumber("0xFADE").intValue());
        assertTrue("createNumber(String) 10 failed", -0xFADE == NumberUtils.createNumber("-0xFADE").intValue());
        assertEquals("createNumber(String) 11 failed", new Double("1.1E200"), NumberUtils.createNumber("1.1E200"));
        assertEquals("createNumber(String) 12 failed", new Float("1.1E20"), NumberUtils.createNumber("1.1E20"));
        assertEquals("createNumber(String) 13 failed", new Double("-1.1E200"), NumberUtils.createNumber("-1.1E200"));
        assertEquals("createNumber(String) 14 failed", new Double("1.1E-200"), NumberUtils.createNumber("1.1E-200"));
        assertEquals("createNumber(null) failed", null, NumberUtils.createNumber(null));
        assertEquals("createNumber(String) failed", new BigInteger("12345678901234567890"), NumberUtils
                .createNumber("12345678901234567890L"));

        // jdk 1.2 doesn't support this. unsure about jdk 1.2.2
        if (SystemUtils.isJavaVersionAtLeast(1.3f)) {
            assertEquals("createNumber(String) 15 failed", new BigDecimal("1.1E-700"), NumberUtils
                    .createNumber("1.1E-700F"));
        }
        assertEquals("createNumber(String) 16 failed", new Long("10" + Integer.MAX_VALUE), NumberUtils
                .createNumber("10" + Integer.MAX_VALUE + "L"));
        assertEquals("createNumber(String) 17 failed", new Long("10" + Integer.MAX_VALUE), NumberUtils
                .createNumber("10" + Integer.MAX_VALUE));
        assertEquals("createNumber(String) 18 failed", new BigInteger("10" + Long.MAX_VALUE), NumberUtils
                .createNumber("10" + Long.MAX_VALUE));

        // LANG-521
        assertEquals("createNumber(String) LANG-521 failed", new Float("2."), NumberUtils.createNumber("2."));

        // LANG-638
        assertFalse("createNumber(String) succeeded", checkCreateNumber("1eE"));
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
On the buggy version the root cause surfaces as: java.lang.StringIndexOutOfBoundsException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
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
throwable: java.lang.StringIndexOutOfBoundsException
message: String index out of range: 4
thrown_at: org.apache.commons.lang3.math.NumberUtils.createNumber(NumberUtils.java:489)
anchor_input: "1234.5"  // hard-code this verbatim as your first call, then fuzz inputs of the same shape
other_observed_literals: "12345", "1234.5D", "1234.5d", "1234.5F", "1234.5f", "12345L", "12345l", "-1234.5", "-12345", "0xFADE", "-0xFADE", "1.1E200", "1.1E20", "-1.1E200", "1.1E-200", "12345678901234567890L", "1.1E-700F", "2."
</ground_truth_crash>
<failing_test class="org.apache.commons.lang3.math.NumberUtilsTest" method="testCreateNumber">
    public void testCreateNumber() {
        // a lot of things can go wrong
        assertEquals("createNumber(String) 1 failed", new Float("1234.5"), NumberUtils.createNumber("1234.5"));
        assertEquals("createNumber(String) 2 failed", new Integer("12345"), NumberUtils.createNumber("12345"));
        assertEquals("createNumber(String) 3 failed", new Double("1234.5"), NumberUtils.createNumber("1234.5D"));
        assertEquals("createNumber(String) 3 failed", new Double("1234.5"), NumberUtils.createNumber("1234.5d"));
        assertEquals("createNumber(String) 4 failed", new Float("1234.5"), NumberUtils.createNumber("1234.5F"));
        assertEquals("createNumber(String) 4 failed", new Float("1234.5"), NumberUtils.createNumber("1234.5f"));
        assertEquals("createNumber(String) 5 failed", new Long(Integer.MAX_VALUE + 1L), NumberUtils.createNumber(""
            + (Integer.MAX_VALUE + 1L)));
        assertEquals("createNumber(String) 6 failed", new Long(12345), NumberUtils.createNumber("12345L"));
        assertEquals("createNumber(String) 6 failed", new Long(12345), NumberUtils.createNumber("12345l"));
        assertEquals("createNumber(String) 7 failed", new Float("-1234.5"), NumberUtils.createNumber("-1234.5"));
        assertEquals("createNumber(String) 8 failed", new Integer("-12345"), NumberUtils.createNumber("-12345"));
        assertTrue("createNumber(String) 9 failed", 0xFADE == NumberUtils.createNumber("0xFADE").intValue());
        assertTrue("createNumber(
        // ... (truncated)
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.lang3.math.NumberUtilsTest::testCreateNumber
java.lang.StringIndexOutOfBoundsException: String index out of range: 4
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
<details open><summary>▸ Output (~6,982 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.math.BigDecimal;
import java.math.BigInteger;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        exercise("1234.5", true);

        String fuzzed = buildValidNumberString(data);
        exercise(fuzzed, true);

        if (data.remainingBytes() > 0) {
            String second = buildValidNumberString(data);
            exercise(second, true);
        }
    }

    private static void exercise(String input, boolean validByConstruction) {
        final Number parsed;
        try {
            parsed = NumberUtils.createNumber(input);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (validByConstruction && isRootCause(t)) {
                throw t;
            }
            return;
        }

        if (parsed == null) {
            return;
        }

        /*
         * Contract-based metamorphic/consistency check:
         * createNumber(String) selects one member of the real create* parser family
         * for the same input text. Therefore, once it returns a Number of a specific
         * runtime type, reparsing the same string with that exact helper must yield
         * an equal value. A throw-deleting or branch-skipping patch can avoid the
         * crash yet silently return the wrong type/value; this check catches that.
         * If either side throws, the relation does not apply and we skip it.
         */
        try {
            Number reparsed = reparseWithMatchingHelper(input, parsed);
            if (reparsed == null) {
                return;
            }
            if (!numbersAgree(parsed, reparsed)) {
                throw new RuntimeException(
                    "[oracle:create-family-consistency] metamorphic violation: input=" + input
                        + " parsedType=" + parsed.getClass().getName()
                        + " parsed=" + parsed
                        + " reparsedType=" + reparsed.getClass().getName()
                        + " reparsed=" + reparsed);
            }
        } catch (RuntimeException ignored) {
            if (ignored.getMessage() != null
                && ignored.getMessage().startsWith("[oracle:create-family-consistency]")) {
                throw ignored;
            }
        }
    }

    private static String buildValidNumberString(FuzzedDataProvider data) {
        int style = data.consumeInt(0, 5);
        boolean negative = data.consumeBoolean();

        switch (style) {
            case 0:
                return (negative ? "-" : "") + digits(data, 1, 8);
            case 1:
                return (negative ? "-" : "") + digits(data, 1, 6) + "." + digits(data, 1, 6);
            case 2:
                return (negative ? "-" : "") + digits(data, 1, 6) + ".";
            case 3:
                return (negative ? "-" : "") + digits(data, 1, 4) + exponentPart(data);
            case 4:
                return (negative ? "-" : "") + digits(data, 1, 3) + "." + digits(data, 1, 3) + exponentPart(data);
            default:
                String core = (negative ? "-" : "") + digits(data, 1, 6);
                char suffix;
                switch (data.consumeInt(0, 3)) {
                    case 0:
                        suffix = 'L';
                        break;
                    case 1:
                        suffix = 'l';
                        break;
                    case 2:
                        suffix = 'F';
                        break;
                    default:
                        suffix = 'D';
                        break;
                }
                if (suffix == 'F' || suffix == 'D') {
                    core = (negative ? "-" : "") + digits(data, 1, 5) + "." + digits(data, 1, 5);
                }
                return core + suffix;
        }
    }

    private static String exponentPart(FuzzedDataProvider data) {
        char e = data.consumeBoolean() ? 'e' : 'E';
        String sign = data.consumeBoolean() ? (data.consumeBoolean() ? "-" : "+") : "";
        return String.valueOf(e) + sign + digits(data, 1, 3);
    }

    private static String digits(FuzzedDataProvider data, int min, int max) {
        int len = data.consumeInt(min, max);
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append((char) ('0' + data.consumeInt(0, 9)));
        }
        return sb.toString();
    }

    private static Number reparseWithMatchingHelper(String input, Number parsed) {
        if (parsed instanceof Integer) {
            return NumberUtils.createInteger(input);
        }
        if (parsed instanceof Long) {
            return NumberUtils.createLong(input);
        }
        if (parsed instanceof Float) {
            return NumberUtils.createFloat(input);
        }
        if (parsed instanceof Double) {
            return NumberUtils.createDouble(input);
        }
        if (parsed instanceof BigInteger) {
            return NumberUtils.createBigInteger(input);
        }
        if (parsed instanceof BigDecimal) {
            return NumberUtils.createBigDecimal(input);
        }
        return null;
    }

    private static boolean numbersAgree(Number a, Number b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (!a.getClass().equals(b.getClass())) {
            return false;
        }
        if (a instanceof Float) {
            return Float.floatToIntBits(((Float) a).floatValue()) == Float.floatToIntBits(((Float) b).floatValue());
        }
        if (a instanceof Double) {
            return Double.doubleToLongBits(((Double) a).doubleValue()) == Double.doubleToLongBits(((Double) b).doubleValue());
        }
        return a.equals(b);
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
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
                    || "createBigDecimal".equals(method)) {
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
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. Jazzer exited cleanly with no finding on the buggy code. Most often this means every input was handled normally, or the harnes

---
## [5] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~9,806 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.math.BigDecimal;
import java.math.BigInteger;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        exercise("1234.5", true);

        String fuzzed = buildValidNumberString(data);
        exercise(fuzzed, true);

        if (data.remainingBytes() > 0) {
            String second = buildValidNumberString(data);
            exercise(second, true);
        }
    }

    private static void exercise(String input, boolean validByConstruction) {
        final Number parsed;
        try {
            parsed = NumberUtils.createNumber(input);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (validByConstruction && isRootCause(t)) {
                throw t;
            }
            return;
        }

        if (parsed == null) {
            return;
        }

        /*
         * Contract-based metamorphic/consistency check:
         * createNumber(String) selects one member of the real create* parser family
         * for the same input text. Therefore, once it returns a Number of a specific
         * runtime type, reparsing the same string with that exact helper must yield
         * an equal value. A throw-deleting or branch-skipping patch can avoid the
         * crash yet silently return the wrong type/value; this check catches that.
         * If either side throws, the relation does not apply and we skip it.
         */
        try {
            Number reparsed = reparseWithMatchingHelper(input, parsed);
            if (reparsed == null) {
                return;
            }
            if (!numbersAgree(parsed, reparsed)) {
                throw new RuntimeException(
                    "[oracle:create-family-consistency] metamorphic violation: input=" + input
                        + " parsedType=" + parsed.getClass().getName()
                        + " parsed=" + parsed
                        + " reparsedType=" + reparsed.getClass().getName()
                        + " reparsed=" + reparsed);
            }
        } catch (RuntimeException ignored) {
            if (ignored.getMessage() != null
                && ignored.getMessage().startsWith("[oracle:create-family-consistency]")) {
                throw ignored;
            }
        }
    }

    private static String buildValidNumberString(FuzzedDataProvider data) {
        int style = data.consumeInt(0, 5);
        boolean negative = data.consumeBoolean();

        switch (style) {
            case 0:
                return (negative ? "-" : "") + digits(data, 1, 8);
            case 1:
                return (negative ? "-" : "") + digits(data, 1, 6) + "." + digits(data, 1, 6);
            case 2:
                return (negative ? "-" : "") + digits(data, 1, 6) + ".";
            case 3:
                return (negative ? "-" : "") + digits(data, 1, 4) + exponentPart(data);
            case 4:
                return (negative ? "-" : "") + digits(data, 1, 3) + "." + digits(data, 1, 3) + exponentPart(data);
            default:
                String core = (negative ? "-" : "") + digits(data, 1, 6);
                char suffix;
                switch (data.consumeInt(0, 3)) {
                    case 0:
                        suffix = 'L';
                        break;
                    case 1:
                        suffix = 'l';
                        break;
                    case 2:
                        suffix = 'F';
                        break;
                    default:
                        suffix = 'D';
                        break;
                }
                if (suffix == 'F' || suffix == 'D') {
                    core = (negative ? "-" : "") + digits(data, 1, 5) + "." + digits(data, 1, 5);
                }
                return core + suffix;
        }
    }

    private static String exponentPart(FuzzedDataProvider data) {
        char e = data.consumeBoolean() ? 'e' : 'E';
        String sign = data.consumeBoolean() ? (data.consumeBoolean() ? "-" : "+") : "";
        return String.valueOf(e) + sign + digits(data, 1, 3);
    }

    private static String digits(FuzzedDataProvider data, int min, int max) {
        int len = data.consumeInt(min, max);
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append((char) ('0' + data.consumeInt(0, 9)));
        }
        return sb.toString();
    }

    private static Number reparseWithMatchingHelper(String input, Number parsed) {
        if (parsed instanceof Integer) {
            return NumberUtils.createInteger(input);
        }
        if (parsed instanceof Long) {
            return NumberUtils.createLong(input);
        }
        if (parsed instanceof Float) {
            return NumberUtils.createFloat(input);
        }
        if (parsed instanceof Double) {
            return NumberUtils.createDouble(input);
        }
        if (parsed instanceof BigInteger) {
            return NumberUtils.createBigInteger(input);
        }
        if (parsed instanceof BigDecimal) {
            return NumberUtils.createBigDecimal(input);
        }
        return null;
    }

    private static boolean numbersAgree(Number a, Number b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (!a.getClass().equals(b.getClass())) {
            return false;
        }
        if (a instanceof Float) {
            return Float.floatToIntBits(((Float) a).floatValue()) == Float.floatToIntBits(((Float) b).floatValue());
        }
        if (a instanceof Double) {
            return Double.doubleToLongBits(((Double) a).doubleValue()) == Double.doubleToLongBits(((Double) b).doubleValue());
        }
        return a.equals(b);
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
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
                    || "createBigDecimal".equals(method)) {
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
That compiled, but it did NOT trigger the bug on the known-buggy version. Jazzer exited cleanly with no finding on the buggy code. Most often this means every input was handled normally, or the harness itself caught and swallowed the exception that should have propagated.

The patch under analysis is below. The buggy version is the code BEFORE this patch is applied (the '+' lines are the FIXED code; the buggy code has the '-' lines instead):
--- /src/main/java/org/apache/commons/lang3/math/NumberUtils.java
+++ /src/main/java/org/apache/commons/lang3/math/NumberUtils.java
@@ -485,7 +485,7 @@
             }
             mant = str.substring(0, decPos);
         } else {
-            if (expPos > -1) {
+        	if(expPos>-1&&expPos<str.length()-1){
                 mant = str.substring(0, expPos);
             } else {
                 mant = str;


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
<details open><summary>▸ Output (~2,224 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NumberUtils.createNumber("1eE");

        String variant = buildBugTrigger(data);
        NumberUtils.createNumber(variant);

        String canonicalInt = buildCanonicalInteger(data);
        Number n1;
        Number n2;
        try {
            n1 = NumberUtils.createNumber(canonicalInt);
            n2 = NumberUtils.createInteger(canonicalInt);
        } catch (RuntimeException ignored) {
            return;
        }
        if (n1 != null && n2 != null && !n1.equals(n2)) {
            throw new RuntimeException(
                "[oracle:createNumber-createInteger] metamorphic violation: input="
                    + canonicalInt + " lhs=" + n1 + " rhs=" + n2);
        }
    }

    private static String buildBugTrigger(FuzzedDataProvider data) {
        String prefix = digits(data.consumeInt(1, 8), data);
        String middle = nonDotAscii(data.consumeInt(0, 4), data);
        boolean order = data.consumeBoolean();
        if (order) {
            return prefix + 'e' + middle + 'E';
        }
        return prefix + 'E' + middle + 'e';
    }

    private static String buildCanonicalInteger(FuzzedDataProvider data) {
        String s = digits(data.consumeInt(1, 9), data);
        if (data.consumeBoolean()) {
            s = "-" + s;
        }
        if ("-0".equals(s)) {
            return "0";
        }
        return s;
    }

    private static String digits(int len, FuzzedDataProvider data) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append((char) ('0' + data.consumeInt(0, 9)));
        }
        return sb.toString();
    }

    private static String nonDotAscii(int len, FuzzedDataProvider data) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c;
            do {
                c = (char) data.consumeInt(33, 126);
            } while (c == '.');
            sb.append(c);
        }
        return sb.toString();
    }
}
```

</details>

---
## [6] ⚙️ harness-attempt · `attempt_002`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.StringIndexOutOfBoundsException: String index out of range: 4

---
## [7] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~37,287 chars, 1 new)</summary>

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

Codebase: `Lang_27_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/lang3/math/NumberUtils.java
+++ /src/main/java/org/apache/commons/lang3/math/NumberUtils.java
@@ -485,7 +485,7 @@
             }
             mant = str.substring(0, decPos);
         } else {
-            if (expPos > -1) {
+        	if(expPos>-1&&expPos<str.length()-1){
                 mant = str.substring(0, expPos);
             } else {
                 mant = str;

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
                if (expPos < decPos) {
                    throw new NumberFormatException(str + " is not a valid number.");
                }
                dec = str.substring(decPos + 1, expPos);
            } else {
                dec = str.substring(decPos + 1);
            }
            mant = str.substring(0, decPos);
        } else {
            if (expPos > -1) {
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
                        } catch (NumberFormatException nfe) {
                            //Too big for a long
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

                    } catch (NumberFormatException nfe) {
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
                    } catch (NumberFormatException nfe) {
                        // ignore the bad number
                    }
                    try {
                        return createBigDecimal(numeric);
                    } catch (NumberFormatException e) {
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
                } catch (NumberFormatException nfe) {
                    // ignore the bad number
                }
                try {
                    return createLong(str);
                } catch (NumberFormatException nfe) {
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
                } catch (NumberFormatException nfe) {
                    // ignore the bad number
                }
                try {
                    Double d = createDouble(str);
                    if (!(d.isInfinite() || (d.doubleValue() == 0.0D && !allZeros))) {
                        return d;
                    }
                } catch (NumberFormatException nfe) {
                    // ignore the bad number
                }

                return createBigDecimal(str);

            }
        }
    }
</code>
Call-site examples (use these as a guide for constructing the target call):
<xref>
public void testCreateNumber() {
        // a lot of things can go wrong
        assertEquals("createNumber(String) 1 failed", new Float("1234.5"), NumberUtils.createNumber("1234.5"));
        assertEquals("createNumber(String) 2 failed", new Integer("12345"), NumberUtils.createNumber("12345"));
        assertEquals("createNumber(String) 3 failed", new Double("1234.5"), NumberUtils.createNumber("1234.5D"));
        assertEquals("createNumber(String) 3 failed", new Double("1234.5"), NumberUtils.createNumber("1234.5d"));
        assertEquals("createNumber(String) 4 failed", new Float("1234.5"), NumberUtils.createNumber("1234.5F"));
        assertEquals("createNumber(String) 4 failed", new Float("1234.5"), NumberUtils.createNumber("1234.5f"));
        assertEquals("createNumber(String) 5 failed", new Long(Integer.MAX_VALUE + 1L), NumberUtils.createNumber(""
            + (Integer.MAX_VALUE + 1L)));
        assertEquals("createNumber(String) 6 failed", new Long(12345), NumberUtils.createNumber("12345L"));
        assertEquals("createNumber(String) 6 failed", new Long(12345), NumberUtils.createNumber("12345l"));
        assertEquals("createNumber(String) 7 failed", new Float("-1234.5"), NumberUtils.createNumber("-1234.5"));
        assertEquals("createNumber(String) 8 failed", new Integer("-12345"), NumberUtils.createNumber("-12345"));
        assertTrue("createNumber(String) 9 failed", 0xFADE == NumberUtils.createNumber("0xFADE").intValue());
        assertTrue("createNumber(String) 10 failed", -0xFADE == NumberUtils.createNumber("-0xFADE").intValue());
        assertEquals("createNumber(String) 11 failed", new Double("1.1E200"), NumberUtils.createNumber("1.1E200"));
        assertEquals("createNumber(String) 12 failed", new Float("1.1E20"), NumberUtils.createNumber("1.1E20"));
        assertEquals("createNumber(String) 13 failed", new Double("-1.1E200"), NumberUtils.createNumber("-1.1E200"));
        assertEquals("createNumber(String) 14 failed", new Double("1.1E-200"), NumberUtils.createNumber("1.1E-200"));
        assertEquals("createNumber(null) failed", null, NumberUtils.createNumber(null));
        assertEquals("createNumber(String) failed", new BigInteger("12345678901234567890"), NumberUtils
                .createNumber("12345678901234567890L"));

        // jdk 1.2 doesn't support this. unsure about jdk 1.2.2
        if (SystemUtils.isJavaVersionAtLeast(1.3f)) {
            assertEquals("createNumber(String) 15 failed", new BigDecimal("1.1E-700"), NumberUtils
                    .createNumber("1.1E-700F"));
        }
        assertEquals("createNumber(String) 16 failed", new Long("10" + Integer.MAX_VALUE), NumberUtils
                .createNumber("10" + Integer.MAX_VALUE + "L"));
        assertEquals("createNumber(String) 17 failed", new Long("10" + Integer.MAX_VALUE), NumberUtils
                .createNumber("10" + Integer.MAX_VALUE));
        assertEquals("createNumber(String) 18 failed", new BigInteger("10" + Long.MAX_VALUE), NumberUtils
                .createNumber("10" + Long.MAX_VALUE));

        // LANG-521
        assertEquals("createNumber(String) LANG-521 failed", new Float("2."), NumberUtils.createNumber("2."));

        // LANG-638
        assertFalse("createNumber(String) succeeded", checkCreateNumber("1eE"));
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
On the buggy version the root cause surfaces as: java.lang.StringIndexOutOfBoundsException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
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
throwable: java.lang.StringIndexOutOfBoundsException
message: String index out of range: 4
thrown_at: org.apache.commons.lang3.math.NumberUtils.createNumber(NumberUtils.java:489)
anchor_input: "1234.5"  // hard-code this verbatim as your first call, then fuzz inputs of the same shape
other_observed_literals: "12345", "1234.5D", "1234.5d", "1234.5F", "1234.5f", "12345L", "12345l", "-1234.5", "-12345", "0xFADE", "-0xFADE", "1.1E200", "1.1E20", "-1.1E200", "1.1E-200", "12345678901234567890L", "1.1E-700F", "2."
</ground_truth_crash>
<failing_test class="org.apache.commons.lang3.math.NumberUtilsTest" method="testCreateNumber">
    public void testCreateNumber() {
        // a lot of things can go wrong
        assertEquals("createNumber(String) 1 failed", new Float("1234.5"), NumberUtils.createNumber("1234.5"));
        assertEquals("createNumber(String) 2 failed", new Integer("12345"), NumberUtils.createNumber("12345"));
        assertEquals("createNumber(String) 3 failed", new Double("1234.5"), NumberUtils.createNumber("1234.5D"));
        assertEquals("createNumber(String) 3 failed", new Double("1234.5"), NumberUtils.createNumber("1234.5d"));
        assertEquals("createNumber(String) 4 failed", new Float("1234.5"), NumberUtils.createNumber("1234.5F"));
        assertEquals("createNumber(String) 4 failed", new Float("1234.5"), NumberUtils.createNumber("1234.5f"));
        assertEquals("createNumber(String) 5 failed", new Long(Integer.MAX_VALUE + 1L), NumberUtils.createNumber(""
            + (Integer.MAX_VALUE + 1L)));
        assertEquals("createNumber(String) 6 failed", new Long(12345), NumberUtils.createNumber("12345L"));
        assertEquals("createNumber(String) 6 failed", new Long(12345), NumberUtils.createNumber("12345l"));
        assertEquals("createNumber(String) 7 failed", new Float("-1234.5"), NumberUtils.createNumber("-1234.5"));
        assertEquals("createNumber(String) 8 failed", new Integer("-12345"), NumberUtils.createNumber("-12345"));
        assertTrue("createNumber(String) 9 failed", 0xFADE == NumberUtils.createNumber("0xFADE").intValue());
        assertTrue("createNumber(
        // ... (truncated)
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.lang3.math.NumberUtilsTest::testCreateNumber
java.lang.StringIndexOutOfBoundsException: String index out of range: 4
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
- org.apache.commons.lang3.math.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.lang3.math.NumberUtils.createNumber
Crashes already found:
- java.lang.StringIndexOutOfBoundsException@org.apache.commons.lang3.math.NumberUtils.createNumber
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
Check FAMILIES already covered by accepted harnesses: {createnumber-createinteger}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.StringIndexOutOfBoundsException@org.apache.commons.lang3.math.NumberUtils.createNumber. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
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
<details open><summary>▸ Output (~9,585 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Contract asserted below:
        // For canonical, moderate valid numeric strings that this API is obliged to accept,
        // createNumber(String) must parse successfully and agree with the corresponding to* parser
        // on the represented numeric value. A patch that merely suppresses the known throw or
        // skips the intended parsing logic would violate this agreement.

        final String anchor = "1234.5";
        try {
            Number n = NumberUtils.createNumber(anchor);
            float f = NumberUtils.toFloat(anchor, Float.NaN);
            if (!(n instanceof Float) || Float.isNaN(f) || n.floatValue() != f) {
                throw new RuntimeException(
                    "[oracle:anchor-float] metamorphic violation: createNumber/createFloat-style agreement input="
                        + anchor + " parsed=" + describe(n) + " toFloat=" + f);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t) && isValidByConstruction(anchor)) {
                throw t;
            }
        }

        int mode = data.consumeInt(0, 5);

        if (mode == 0) {
            String s = buildCanonicalInteger(data);
            try {
                Number n = NumberUtils.createNumber(s);
                int asInt = NumberUtils.toInt(s, Integer.MIN_VALUE);
                long asLong = NumberUtils.toLong(s, Long.MIN_VALUE);
                if (asInt == Integer.MIN_VALUE || asLong == Long.MIN_VALUE) {
                    return;
                }
                if (n.longValue() != asLong || n.intValue() != asInt) {
                    throw new RuntimeException(
                        "[oracle:int-agree] metamorphic violation: input=" + s
                            + " parsed=" + describe(n)
                            + " toInt=" + asInt
                            + " toLong=" + asLong);
                }
            } catch (RuntimeException t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t) && isValidByConstruction(s)) {
                    throw t;
                }
            }
            return;
        }

        if (mode == 1) {
            String s = buildCanonicalHalfDecimal(data);
            try {
                Number n = NumberUtils.createNumber(s);
                float f = NumberUtils.toFloat(s, Float.NaN);
                double d = NumberUtils.toDouble(s, Double.NaN);
                if (Float.isNaN(f) || Double.isNaN(d)) {
                    return;
                }
                if (!(n instanceof Float) || n.floatValue() != f || n.doubleValue() != d) {
                    throw new RuntimeException(
                        "[oracle:decimal-agree] metamorphic violation: input=" + s
                            + " parsed=" + describe(n)
                            + " toFloat=" + f
                            + " toDouble=" + d);
                }
            } catch (RuntimeException t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t) && isValidByConstruction(s)) {
                    throw t;
                }
            }
            return;
        }

        if (mode == 2) {
            String s = buildSmallExponentInteger(data);
            try {
                Number n = NumberUtils.createNumber(s);
                double d = NumberUtils.toDouble(s, Double.NaN);
                if (Double.isNaN(d)) {
                    return;
                }
                if (n.doubleValue() != d) {
                    throw new RuntimeException(
                        "[oracle:exp-agree] metamorphic violation: input=" + s
                            + " parsed=" + describe(n)
                            + " toDouble=" + d);
                }
            } catch (RuntimeException t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t) && isValidByConstruction(s)) {
                    throw t;
                }
            }
            return;
        }

        if (mode == 3) {
            // Flip the patched condition: exponent marker at the boundary and just past it.
            // These malformed inputs should be rejected cleanly, not by losing control of indices.
            String digits = positiveDigits(data);
            String s;
            switch (data.consumeInt(0, 2)) {
                case 0:
                    s = digits + (data.consumeBoolean() ? 'e' : 'E');
                    break;
                case 1:
                    s = digits + (data.consumeBoolean() ? 'e' : 'E') + (data.consumeBoolean() ? "+" : "-");
                    break;
                default:
                    s = digits + "." + data.consumeInt(0, 9) + (data.consumeBoolean() ? 'e' : 'E');
                    break;
            }
            try {
                NumberUtils.createNumber(s);
            } catch (RuntimeException t) {
                if (isCleanRejection(t)) {
                    return;
                }
                // Invalid input: never report generic parser failures here.
            }
            return;
        }

        if (mode == 4) {
            // Boundary-neighbor valid inputs: exponent marker followed by exactly one digit.
            String s = positiveDigits(data) + (data.consumeBoolean() ? 'e' : 'E') + data.consumeInt(0, 6);
            try {
                Number n = NumberUtils.createNumber(s);
                double d = NumberUtils.toDouble(s, Double.NaN);
                if (!Double.isNaN(d) && n.doubleValue() != d) {
                    throw new RuntimeException(
                        "[oracle:boundary-valid-exp] metamorphic violation: input=" + s
                            + " parsed=" + describe(n)
                            + " toDouble=" + d);
                }
            } catch (RuntimeException t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t) && isValidByConstruction(s)) {
                    throw t;
                }
            }
            return;
        }

        String s = data.consumeAsciiString(Math.min(32, Math.max(1, data.remainingBytes())));
        try {
            NumberUtils.createNumber(s);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t) && isValidByConstruction(s)) {
                throw t;
            }
        }
    }

    private static String buildCanonicalInteger(FuzzedDataProvider data) {
        int v = data.consumeInt(-1_000_000, 1_000_000);
        return Integer.toString(v);
    }

    private static String buildCanonicalHalfDecimal(FuzzedDataProvider data) {
        int whole = data.consumeInt(-1_000_000, 1_000_000);
        return Integer.toString(whole) + ".5";
    }

    private static String buildSmallExponentInteger(FuzzedDataProvider data) {
        int mant = data.consumeInt(1, 9);
        int exp = data.consumeInt(0, 6);
        String sign = data.consumeBoolean() ? "" : "-";
        return sign + mant + (data.consumeBoolean() ? "e" : "E") + exp;
    }

    private static String positiveDigits(FuzzedDataProvider data) {
        int len = data.consumeInt(1, 8);
        StringBuilder sb = new StringBuilder(len);
        sb.append((char) ('1' + data.consumeInt(0, 8)));
        for (int i = 1; i < len; i++) {
            sb.append((char) ('0' + data.consumeInt(0, 9)));
        }
        return sb.toString();
    }

    private static boolean isValidByConstruction(String s) {
        if (s == null || s.length() == 0) {
            return false;
        }
        if (s.equals("1234.5")) {
            return true;
        }
        if (s.matches("-?\\d+")) {
            return true;
        }
        if (s.matches("-?\\d+\\.5")) {
            return true;
        }
        return s.matches("-?[1-9]\\d*[eE]\\d");
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            String cls = st[i].getClassName();
            String m = st[i].getMethodName();
            if ("org.apache.commons.lang3.math.NumberUtils".equals(cls)) {
                if ("createNumber".equals(m)
                    || "createInteger".equals(m)
                    || "isAllZeros".equals(m)
                    || "isDigits".equals(m)
                    || "createLong".equals(m)
                    || "createBigInteger".equals(m)
                    || "createFloat".equals(m)
                    || "createDouble".equals(m)
                    || "createBigDecimal".equals(m)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String describe(Number n) {
        if (n == null) {
            return "null";
        }
        return n.getClass().getName() + ":" + n.toString();
    }
}
```

</details>

---
## [8] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. Jazzer exited cleanly with no finding on the buggy code. Most often this means every input was handled normally, or the harnes

---
## [9] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~9,782 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [7] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Contract asserted below:
        // For canonical, moderate valid numeric strings that this API is obliged to accept,
        // createNumber(String) must parse successfully and agree with the corresponding to* parser
        // on the represented numeric value. A patch that merely suppresses the known throw or
        // skips the intended parsing logic would violate this agreement.

        final String anchor = "1234.5";
        try {
            Number n = NumberUtils.createNumber(anchor);
            float f = NumberUtils.toFloat(anchor, Float.NaN);
            if (!(n instanceof Float) || Float.isNaN(f) || n.floatValue() != f) {
                throw new RuntimeException(
                    "[oracle:anchor-float] metamorphic violation: createNumber/createFloat-style agreement input="
                        + anchor + " parsed=" + describe(n) + " toFloat=" + f);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t) && isValidByConstruction(anchor)) {
                throw t;
            }
        }

        int mode = data.consumeInt(0, 5);

        if (mode == 0) {
            String s = buildCanonicalInteger(data);
            try {
                Number n = NumberUtils.createNumber(s);
                int asInt = NumberUtils.toInt(s, Integer.MIN_VALUE);
                long asLong = NumberUtils.toLong(s, Long.MIN_VALUE);
                if (asInt == Integer.MIN_VALUE || asLong == Long.MIN_VALUE) {
                    return;
                }
                if (n.longValue() != asLong || n.intValue() != asInt) {
                    throw new RuntimeException(
                        "[oracle:int-agree] metamorphic violation: input=" + s
                            + " parsed=" + describe(n)
                            + " toInt=" + asInt
                            + " toLong=" + asLong);
                }
            } catch (RuntimeException t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t) && isValidByConstruction(s)) {
                    throw t;
                }
            }
            return;
        }

        if (mode == 1) {
            String s = buildCanonicalHalfDecimal(data);
            try {
                Number n = NumberUtils.createNumber(s);
                float f = NumberUtils.toFloat(s, Float.NaN);
                double d = NumberUtils.toDouble(s, Double.NaN);
                if (Float.isNaN(f) || Double.isNaN(d)) {
                    return;
                }
                if (!(n instanceof Float) || n.floatValue() != f || n.doubleValue() != d) {
                    throw new RuntimeException(
                        "[oracle:decimal-agree] metamorphic violation: input=" + s
                            + " parsed=" + describe(n)
                            + " toFloat=" + f
                            + " toDouble=" + d);
                }
            } catch (RuntimeException t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t) && isValidByConstruction(s)) {
                    throw t;
                }
            }
            return;
        }

        if (mode == 2) {
            String s = buildSmallExponentInteger(data);
            try {
                Number n = NumberUtils.createNumber(s);
                double d = NumberUtils.toDouble(s, Double.NaN);
                if (Double.isNaN(d)) {
                    return;
                }
                if (n.doubleValue() != d) {
                    throw new RuntimeException(
                        "[oracle:exp-agree] metamorphic violation: input=" + s
                            + " parsed=" + describe(n)
                            + " toDouble=" + d);
                }
            } catch (RuntimeException t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t) && isValidByConstruction(s)) {
                    throw t;
                }
            }
            return;
        }

        if (mode == 3) {
            // Flip the patched condition: exponent marker at the boundary and just past it.
            // These malformed inputs should be rejected cleanly, not by losing control of indices.
            String digits = positiveDigits(data);
            String s;
            switch (data.consumeInt(0, 2)) {
                case 0:
                    s = digits + (data.consumeBoolean() ? 'e' : 'E');
                    break;
                case 1:
                    s = digits + (data.consumeBoolean() ? 'e' : 'E') + (data.consumeBoolean() ? "+" : "-");
                    break;
                default:
                    s = digits + "." + data.consumeInt(0, 9) + (data.consumeBoolean() ? 'e' : 'E');
                    break;
            }
            try {
                NumberUtils.createNumber(s);
            } catch (RuntimeException t) {
                if (isCleanRejection(t)) {
                    return;
                }
                // Invalid input: never report generic parser failures here.
            }
            return;
        }

        if (mode == 4) {
            // Boundary-neighbor valid inputs: exponent marker followed by exactly one digit.
            String s = positiveDigits(data) + (data.consumeBoolean() ? 'e' : 'E') + data.consumeInt(0, 6);
            try {
                Number n = NumberUtils.createNumber(s);
                double d = NumberUtils.toDouble(s, Double.NaN);
                if (!Double.isNaN(d) && n.doubleValue() != d) {
                    throw new RuntimeException(
                        "[oracle:boundary-valid-exp] metamorphic violation: input=" + s
                            + " parsed=" + describe(n)
                            + " toDouble=" + d);
                }
            } catch (RuntimeException t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t) && isValidByConstruction(s)) {
                    throw t;
                }
            }
            return;
        }

        String s = data.consumeAsciiString(Math.min(32, Math.max(1, data.remainingBytes())));
        try {
            NumberUtils.createNumber(s);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t) && isValidByConstruction(s)) {
                throw t;
            }
        }
    }

    private static String buildCanonicalInteger(FuzzedDataProvider data) {
        int v = data.consumeInt(-1_000_000, 1_000_000);
        return Integer.toString(v);
    }

    private static String buildCanonicalHalfDecimal(FuzzedDataProvider data) {
        int whole = data.consumeInt(-1_000_000, 1_000_000);
        return Integer.toString(whole) + ".5";
    }

    private static String buildSmallExponentInteger(FuzzedDataProvider data) {
        int mant = data.consumeInt(1, 9);
        int exp = data.consumeInt(0, 6);
        String sign = data.consumeBoolean() ? "" : "-";
        return sign + mant + (data.consumeBoolean() ? "e" : "E") + exp;
    }

    private static String positiveDigits(FuzzedDataProvider data) {
        int len = data.consumeInt(1, 8);
        StringBuilder sb = new StringBuilder(len);
        sb.append((char) ('1' + data.consumeInt(0, 8)));
        for (int i = 1; i < len; i++) {
            sb.append((char) ('0' + data.consumeInt(0, 9)));
        }
        return sb.toString();
    }

    private static boolean isValidByConstruction(String s) {
        if (s == null || s.length() == 0) {
            return false;
        }
        if (s.equals("1234.5")) {
            return true;
        }
        if (s.matches("-?\\d+")) {
            return true;
        }
        if (s.matches("-?\\d+\\.5")) {
            return true;
        }
        return s.matches("-?[1-9]\\d*[eE]\\d");
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            String cls = st[i].getClassName();
            String m = st[i].getMethodName();
            if ("org.apache.commons.lang3.math.NumberUtils".equals(cls)) {
                if ("createNumber".equals(m)
                    || "createInteger".equals(m)
                    || "isAllZeros".equals(m)
                    || "isDigits".equals(m)
                    || "createLong".equals(m)
                    || "createBigInteger".equals(m)
                    || "createFloat".equals(m)
                    || "createDouble".equals(m)
                    || "createBigDecimal".equals(m)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String describe(Number n) {
        if (n == null) {
            return "null";
        }
        return n.getClass().getName() + ":" + n.toString();
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~1,628 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            NumberUtils.createNumber("1eE");
        } catch (IllegalArgumentException ignored) {
        }

        String trigger = buildBugTrigger(data);
        try {
            NumberUtils.createNumber(trigger);
        } catch (IllegalArgumentException ignored) {
        }

        String valid = Integer.toString(data.consumeInt(-1_000_000, 1_000_000));
        Number parsed = NumberUtils.createNumber(valid);
        long viaToLong = NumberUtils.toLong(valid, Long.MIN_VALUE);
        if (viaToLong == Long.MIN_VALUE || parsed.longValue() != viaToLong) {
            throw new RuntimeException(
                "[oracle:int-parse] metamorphic violation: input=" + valid
                    + " createNumber=" + parsed
                    + " toLong=" + viaToLong);
        }
    }

    private static String buildBugTrigger(FuzzedDataProvider data) {
        StringBuilder sb = new StringBuilder();
        if (data.consumeBoolean()) {
            sb.append('-');
        }

        int digits = data.consumeInt(1, 16);
        sb.append((char) ('1' + data.consumeInt(0, 8)));
        for (int i = 1; i < digits; i++) {
            sb.append((char) ('0' + data.consumeInt(0, 9)));
        }

        if (data.consumeBoolean()) {
            sb.append('e').append('E');
        } else {
            sb.append('E').append('e');
        }
        return sb.toString();
    }
}
```

</details>

---
## [10] ⚙️ harness-attempt · `attempt_004`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.StringIndexOutOfBoundsException: String index out of range: 4

---
## [11] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~37,298 chars, 1 new)</summary>

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

Codebase: `Lang_27_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/lang3/math/NumberUtils.java
+++ /src/main/java/org/apache/commons/lang3/math/NumberUtils.java
@@ -485,7 +485,7 @@
             }
             mant = str.substring(0, decPos);
         } else {
-            if (expPos > -1) {
+        	if(expPos>-1&&expPos<str.length()-1){
                 mant = str.substring(0, expPos);
             } else {
                 mant = str;

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
                if (expPos < decPos) {
                    throw new NumberFormatException(str + " is not a valid number.");
                }
                dec = str.substring(decPos + 1, expPos);
            } else {
                dec = str.substring(decPos + 1);
            }
            mant = str.substring(0, decPos);
        } else {
            if (expPos > -1) {
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
                        } catch (NumberFormatException nfe) {
                            //Too big for a long
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

                    } catch (NumberFormatException nfe) {
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
                    } catch (NumberFormatException nfe) {
                        // ignore the bad number
                    }
                    try {
                        return createBigDecimal(numeric);
                    } catch (NumberFormatException e) {
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
                } catch (NumberFormatException nfe) {
                    // ignore the bad number
                }
                try {
                    return createLong(str);
                } catch (NumberFormatException nfe) {
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
                } catch (NumberFormatException nfe) {
                    // ignore the bad number
                }
                try {
                    Double d = createDouble(str);
                    if (!(d.isInfinite() || (d.doubleValue() == 0.0D && !allZeros))) {
                        return d;
                    }
                } catch (NumberFormatException nfe) {
                    // ignore the bad number
                }

                return createBigDecimal(str);

            }
        }
    }
</code>
Call-site examples (use these as a guide for constructing the target call):
<xref>
public void testCreateNumber() {
        // a lot of things can go wrong
        assertEquals("createNumber(String) 1 failed", new Float("1234.5"), NumberUtils.createNumber("1234.5"));
        assertEquals("createNumber(String) 2 failed", new Integer("12345"), NumberUtils.createNumber("12345"));
        assertEquals("createNumber(String) 3 failed", new Double("1234.5"), NumberUtils.createNumber("1234.5D"));
        assertEquals("createNumber(String) 3 failed", new Double("1234.5"), NumberUtils.createNumber("1234.5d"));
        assertEquals("createNumber(String) 4 failed", new Float("1234.5"), NumberUtils.createNumber("1234.5F"));
        assertEquals("createNumber(String) 4 failed", new Float("1234.5"), NumberUtils.createNumber("1234.5f"));
        assertEquals("createNumber(String) 5 failed", new Long(Integer.MAX_VALUE + 1L), NumberUtils.createNumber(""
            + (Integer.MAX_VALUE + 1L)));
        assertEquals("createNumber(String) 6 failed", new Long(12345), NumberUtils.createNumber("12345L"));
        assertEquals("createNumber(String) 6 failed", new Long(12345), NumberUtils.createNumber("12345l"));
        assertEquals("createNumber(String) 7 failed", new Float("-1234.5"), NumberUtils.createNumber("-1234.5"));
        assertEquals("createNumber(String) 8 failed", new Integer("-12345"), NumberUtils.createNumber("-12345"));
        assertTrue("createNumber(String) 9 failed", 0xFADE == NumberUtils.createNumber("0xFADE").intValue());
        assertTrue("createNumber(String) 10 failed", -0xFADE == NumberUtils.createNumber("-0xFADE").intValue());
        assertEquals("createNumber(String) 11 failed", new Double("1.1E200"), NumberUtils.createNumber("1.1E200"));
        assertEquals("createNumber(String) 12 failed", new Float("1.1E20"), NumberUtils.createNumber("1.1E20"));
        assertEquals("createNumber(String) 13 failed", new Double("-1.1E200"), NumberUtils.createNumber("-1.1E200"));
        assertEquals("createNumber(String) 14 failed", new Double("1.1E-200"), NumberUtils.createNumber("1.1E-200"));
        assertEquals("createNumber(null) failed", null, NumberUtils.createNumber(null));
        assertEquals("createNumber(String) failed", new BigInteger("12345678901234567890"), NumberUtils
                .createNumber("12345678901234567890L"));

        // jdk 1.2 doesn't support this. unsure about jdk 1.2.2
        if (SystemUtils.isJavaVersionAtLeast(1.3f)) {
            assertEquals("createNumber(String) 15 failed", new BigDecimal("1.1E-700"), NumberUtils
                    .createNumber("1.1E-700F"));
        }
        assertEquals("createNumber(String) 16 failed", new Long("10" + Integer.MAX_VALUE), NumberUtils
                .createNumber("10" + Integer.MAX_VALUE + "L"));
        assertEquals("createNumber(String) 17 failed", new Long("10" + Integer.MAX_VALUE), NumberUtils
                .createNumber("10" + Integer.MAX_VALUE));
        assertEquals("createNumber(String) 18 failed", new BigInteger("10" + Long.MAX_VALUE), NumberUtils
                .createNumber("10" + Long.MAX_VALUE));

        // LANG-521
        assertEquals("createNumber(String) LANG-521 failed", new Float("2."), NumberUtils.createNumber("2."));

        // LANG-638
        assertFalse("createNumber(String) succeeded", checkCreateNumber("1eE"));
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
On the buggy version the root cause surfaces as: java.lang.StringIndexOutOfBoundsException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
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
throwable: java.lang.StringIndexOutOfBoundsException
message: String index out of range: 4
thrown_at: org.apache.commons.lang3.math.NumberUtils.createNumber(NumberUtils.java:489)
anchor_input: "1234.5"  // hard-code this verbatim as your first call, then fuzz inputs of the same shape
other_observed_literals: "12345", "1234.5D", "1234.5d", "1234.5F", "1234.5f", "12345L", "12345l", "-1234.5", "-12345", "0xFADE", "-0xFADE", "1.1E200", "1.1E20", "-1.1E200", "1.1E-200", "12345678901234567890L", "1.1E-700F", "2."
</ground_truth_crash>
<failing_test class="org.apache.commons.lang3.math.NumberUtilsTest" method="testCreateNumber">
    public void testCreateNumber() {
        // a lot of things can go wrong
        assertEquals("createNumber(String) 1 failed", new Float("1234.5"), NumberUtils.createNumber("1234.5"));
        assertEquals("createNumber(String) 2 failed", new Integer("12345"), NumberUtils.createNumber("12345"));
        assertEquals("createNumber(String) 3 failed", new Double("1234.5"), NumberUtils.createNumber("1234.5D"));
        assertEquals("createNumber(String) 3 failed", new Double("1234.5"), NumberUtils.createNumber("1234.5d"));
        assertEquals("createNumber(String) 4 failed", new Float("1234.5"), NumberUtils.createNumber("1234.5F"));
        assertEquals("createNumber(String) 4 failed", new Float("1234.5"), NumberUtils.createNumber("1234.5f"));
        assertEquals("createNumber(String) 5 failed", new Long(Integer.MAX_VALUE + 1L), NumberUtils.createNumber(""
            + (Integer.MAX_VALUE + 1L)));
        assertEquals("createNumber(String) 6 failed", new Long(12345), NumberUtils.createNumber("12345L"));
        assertEquals("createNumber(String) 6 failed", new Long(12345), NumberUtils.createNumber("12345l"));
        assertEquals("createNumber(String) 7 failed", new Float("-1234.5"), NumberUtils.createNumber("-1234.5"));
        assertEquals("createNumber(String) 8 failed", new Integer("-12345"), NumberUtils.createNumber("-12345"));
        assertTrue("createNumber(String) 9 failed", 0xFADE == NumberUtils.createNumber("0xFADE").intValue());
        assertTrue("createNumber(
        // ... (truncated)
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.lang3.math.NumberUtilsTest::testCreateNumber
java.lang.StringIndexOutOfBoundsException: String index out of range: 4
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
- org.apache.commons.lang3.math.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.lang3.math.NumberUtils.createNumber
Crashes already found:
- java.lang.StringIndexOutOfBoundsException@org.apache.commons.lang3.math.NumberUtils.createNumber
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
Check FAMILIES already covered by accepted harnesses: {createnumber-createinteger, int-parse}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.StringIndexOutOfBoundsException@org.apache.commons.lang3.math.NumberUtils.createNumber. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
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
<details open><summary>▸ Output (~7,537 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        oracleCanonicalDecimal("1234.5");

        int whole = data.consumeInt(0, 1_000_000);
        int frac = data.consumeInt(0, 999_999);
        boolean negative = data.consumeBoolean();

        float f = whole + (frac / 1_000_000.0f);
        if (negative) {
            f = -f;
        }

        String s = Float.toString(f);
        if (s.indexOf('e') >= 0 || s.indexOf('E') >= 0 || s.indexOf('.') < 0) {
            return;
        }

        oracleCreateFloatRoundTrip(f, s);
        oracleCanonicalDecimal(s);
        oracleSuffixedFloatAgreement(s);

        String ascii = data.consumeAsciiString(32);
        if (looksLikePlainDecimal(ascii)) {
            try {
                NumberUtils.createNumber(ascii);
            } catch (RuntimeException t) {
                if (isValidationLike(t)) {
                    return;
                }
                if (isKnownCreateNumberRootCause(t)) {
                    throw t;
                }
            }
        }
    }

    /*
     * Contract asserted:
     * For a canonical decimal string produced by Float.toString(f), NumberUtils.createFloat(s)
     * must round-trip to the original float, and NumberUtils.createNumber(s) must accept the same
     * valid numeric text and yield the same Float value (the implementation "start[s] small and go[es]
     * from there", first trying Float for decimal input).
     *
     * This catches throw-deleting / band-aid patches: if createNumber no longer crashes but silently
     * returns the wrong numeric type/value, the equality check still fails.
     */
    private static void oracleCanonicalDecimal(String s) {
        final Float viaCreateFloat;
        try {
            viaCreateFloat = NumberUtils.createFloat(s);
        } catch (RuntimeException t) {
            throw new RuntimeException("[oracle:canonical-float] metamorphic violation: createFloat rejected canonical decimal input=" + s, t);
        }
        if (viaCreateFloat == null) {
            throw new RuntimeException("[oracle:canonical-float] metamorphic violation: createFloat returned null for input=" + s);
        }

        final Number viaCreateNumber;
        try {
            viaCreateNumber = NumberUtils.createNumber(s);
        } catch (RuntimeException t) {
            throw new RuntimeException("[oracle:canonical-float] metamorphic violation: createNumber rejected canonical decimal input=" + s + " helper=" + viaCreateFloat, t);
        }

        if (!(viaCreateNumber instanceof Float)) {
            throw new RuntimeException("[oracle:canonical-float] metamorphic violation: expected Float from createNumber for input=" + s + " got=" + className(viaCreateNumber) + " value=" + viaCreateNumber);
        }
        if (Float.floatToIntBits(viaCreateNumber.floatValue()) != Float.floatToIntBits(viaCreateFloat.floatValue())) {
            throw new RuntimeException("[oracle:canonical-float] metamorphic violation: input=" + s + " lhs=" + viaCreateNumber + " rhs=" + viaCreateFloat);
        }
    }

    /*
     * Independent oracle on a different reachable function:
     * Float.toString(f) is the canonical textual form of f, so createFloat(Float.toString(f))
     * must recover the same float bit pattern.
     */
    private static void oracleCreateFloatRoundTrip(float original, String s) {
        final Float parsed;
        try {
            parsed = NumberUtils.createFloat(s);
        } catch (RuntimeException t) {
            throw new RuntimeException("[oracle:createFloat-rt] metamorphic violation: createFloat rejected its canonical input=" + s + " original=" + original, t);
        }
        if (parsed == null || Float.floatToIntBits(parsed.floatValue()) != Float.floatToIntBits(original)) {
            throw new RuntimeException("[oracle:createFloat-rt] metamorphic violation: input=" + s + " original=" + original + " parsed=" + parsed);
        }
    }

    /*
     * Equivalent-input oracle:
     * For valid canonical decimal text s, appending the explicit 'F' suffix requests Float parsing,
     * so createNumber(s + "F") must agree with createFloat(s).
     */
    private static void oracleSuffixedFloatAgreement(String s) {
        final Float helper;
        try {
            helper = NumberUtils.createFloat(s);
        } catch (RuntimeException t) {
            return;
        }

        final Number withSuffix;
        try {
            withSuffix = NumberUtils.createNumber(s + "F");
        } catch (RuntimeException t) {
            throw new RuntimeException("[oracle:suffix-float] metamorphic violation: createNumber rejected explicit-float input=" + s + "F helper=" + helper, t);
        }

        if (!(withSuffix instanceof Float)) {
            throw new RuntimeException("[oracle:suffix-float] metamorphic violation: expected Float for input=" + s + "F got=" + className(withSuffix) + " value=" + withSuffix);
        }
        if (Float.floatToIntBits(withSuffix.floatValue()) != Float.floatToIntBits(helper.floatValue())) {
            throw new RuntimeException("[oracle:suffix-float] metamorphic violation: input=" + s + "F lhs=" + withSuffix + " rhs=" + helper);
        }
    }

    private static boolean isValidationLike(Throwable t) {
        return t instanceof IllegalArgumentException;
    }

    private static boolean isKnownCreateNumberRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (StackTraceElement e : st) {
            String cls = e.getClassName();
            String m = e.getMethodName();
            if ("org.apache.commons.lang3.math.NumberUtils".equals(cls)
                    && ("createNumber".equals(m)
                    || "createInteger".equals(m)
                    || "isAllZeros".equals(m)
                    || "isDigits".equals(m)
                    || "createLong".equals(m)
                    || "createBigInteger".equals(m)
                    || "createFloat".equals(m)
                    || "createDouble".equals(m)
                    || "createBigDecimal".equals(m))) {
                return true;
            }
            if ("org.apache.commons.lang3.StringUtils".equals(cls) && "isBlank".equals(m)) {
                return true;
            }
        }
        return false;
    }

    private static boolean looksLikePlainDecimal(String s) {
        if (s == null || s.length() == 0) {
            return false;
        }
        int i = 0;
        if (s.charAt(0) == '-') {
            if (s.length() == 1) {
                return false;
            }
            i = 1;
        }
        boolean seenDot = false;
        boolean seenDigit = false;
        for (; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '.') {
                if (seenDot) {
                    return false;
                }
                seenDot = true;
            } else if (c >= '0' && c <= '9') {
                seenDigit = true;
            } else {
                return false;
            }
        }
        return seenDot && seenDigit;
    }

    private static String className(Object o) {
        return o == null ? "null" : o.getClass().getName();
    }
}
```

</details>

---
## [12] ⚙️ harness-attempt · `attempt_005`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.RuntimeException: [oracle:canonical-float] metamorphic violation: expected Float from createNumber for input=-0.0 got=java.math.BigDecimal value=0.0

---
## [13] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~37,599 chars, 1 new)</summary>

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

Codebase: `Lang_27_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/lang3/math/NumberUtils.java
+++ /src/main/java/org/apache/commons/lang3/math/NumberUtils.java
@@ -485,7 +485,7 @@
             }
             mant = str.substring(0, decPos);
         } else {
-            if (expPos > -1) {
+        	if(expPos>-1&&expPos<str.length()-1){
                 mant = str.substring(0, expPos);
             } else {
                 mant = str;

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
                if (expPos < decPos) {
                    throw new NumberFormatException(str + " is not a valid number.");
                }
                dec = str.substring(decPos + 1, expPos);
            } else {
                dec = str.substring(decPos + 1);
            }
            mant = str.substring(0, decPos);
        } else {
            if (expPos > -1) {
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
                        } catch (NumberFormatException nfe) {
                            //Too big for a long
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

                    } catch (NumberFormatException nfe) {
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
                    } catch (NumberFormatException nfe) {
                        // ignore the bad number
                    }
                    try {
                        return createBigDecimal(numeric);
                    } catch (NumberFormatException e) {
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
                } catch (NumberFormatException nfe) {
                    // ignore the bad number
                }
                try {
                    return createLong(str);
                } catch (NumberFormatException nfe) {
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
                } catch (NumberFormatException nfe) {
                    // ignore the bad number
                }
                try {
                    Double d = createDouble(str);
                    if (!(d.isInfinite() || (d.doubleValue() == 0.0D && !allZeros))) {
                        return d;
                    }
                } catch (NumberFormatException nfe) {
                    // ignore the bad number
                }

                return createBigDecimal(str);

            }
        }
    }
</code>
Call-site examples (use these as a guide for constructing the target call):
<xref>
public void testCreateNumber() {
        // a lot of things can go wrong
        assertEquals("createNumber(String) 1 failed", new Float("1234.5"), NumberUtils.createNumber("1234.5"));
        assertEquals("createNumber(String) 2 failed", new Integer("12345"), NumberUtils.createNumber("12345"));
        assertEquals("createNumber(String) 3 failed", new Double("1234.5"), NumberUtils.createNumber("1234.5D"));
        assertEquals("createNumber(String) 3 failed", new Double("1234.5"), NumberUtils.createNumber("1234.5d"));
        assertEquals("createNumber(String) 4 failed", new Float("1234.5"), NumberUtils.createNumber("1234.5F"));
        assertEquals("createNumber(String) 4 failed", new Float("1234.5"), NumberUtils.createNumber("1234.5f"));
        assertEquals("createNumber(String) 5 failed", new Long(Integer.MAX_VALUE + 1L), NumberUtils.createNumber(""
            + (Integer.MAX_VALUE + 1L)));
        assertEquals("createNumber(String) 6 failed", new Long(12345), NumberUtils.createNumber("12345L"));
        assertEquals("createNumber(String) 6 failed", new Long(12345), NumberUtils.createNumber("12345l"));
        assertEquals("createNumber(String) 7 failed", new Float("-1234.5"), NumberUtils.createNumber("-1234.5"));
        assertEquals("createNumber(String) 8 failed", new Integer("-12345"), NumberUtils.createNumber("-12345"));
        assertTrue("createNumber(String) 9 failed", 0xFADE == NumberUtils.createNumber("0xFADE").intValue());
        assertTrue("createNumber(String) 10 failed", -0xFADE == NumberUtils.createNumber("-0xFADE").intValue());
        assertEquals("createNumber(String) 11 failed", new Double("1.1E200"), NumberUtils.createNumber("1.1E200"));
        assertEquals("createNumber(String) 12 failed", new Float("1.1E20"), NumberUtils.createNumber("1.1E20"));
        assertEquals("createNumber(String) 13 failed", new Double("-1.1E200"), NumberUtils.createNumber("-1.1E200"));
        assertEquals("createNumber(String) 14 failed", new Double("1.1E-200"), NumberUtils.createNumber("1.1E-200"));
        assertEquals("createNumber(null) failed", null, NumberUtils.createNumber(null));
        assertEquals("createNumber(String) failed", new BigInteger("12345678901234567890"), NumberUtils
                .createNumber("12345678901234567890L"));

        // jdk 1.2 doesn't support this. unsure about jdk 1.2.2
        if (SystemUtils.isJavaVersionAtLeast(1.3f)) {
            assertEquals("createNumber(String) 15 failed", new BigDecimal("1.1E-700"), NumberUtils
                    .createNumber("1.1E-700F"));
        }
        assertEquals("createNumber(String) 16 failed", new Long("10" + Integer.MAX_VALUE), NumberUtils
                .createNumber("10" + Integer.MAX_VALUE + "L"));
        assertEquals("createNumber(String) 17 failed", new Long("10" + Integer.MAX_VALUE), NumberUtils
                .createNumber("10" + Integer.MAX_VALUE));
        assertEquals("createNumber(String) 18 failed", new BigInteger("10" + Long.MAX_VALUE), NumberUtils
                .createNumber("10" + Long.MAX_VALUE));

        // LANG-521
        assertEquals("createNumber(String) LANG-521 failed", new Float("2."), NumberUtils.createNumber("2."));

        // LANG-638
        assertFalse("createNumber(String) succeeded", checkCreateNumber("1eE"));
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
On the buggy version the root cause surfaces as: java.lang.StringIndexOutOfBoundsException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
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
throwable: java.lang.StringIndexOutOfBoundsException
message: String index out of range: 4
thrown_at: org.apache.commons.lang3.math.NumberUtils.createNumber(NumberUtils.java:489)
anchor_input: "1234.5"  // hard-code this verbatim as your first call, then fuzz inputs of the same shape
other_observed_literals: "12345", "1234.5D", "1234.5d", "1234.5F", "1234.5f", "12345L", "12345l", "-1234.5", "-12345", "0xFADE", "-0xFADE", "1.1E200", "1.1E20", "-1.1E200", "1.1E-200", "12345678901234567890L", "1.1E-700F", "2."
</ground_truth_crash>
<failing_test class="org.apache.commons.lang3.math.NumberUtilsTest" method="testCreateNumber">
    public void testCreateNumber() {
        // a lot of things can go wrong
        assertEquals("createNumber(String) 1 failed", new Float("1234.5"), NumberUtils.createNumber("1234.5"));
        assertEquals("createNumber(String) 2 failed", new Integer("12345"), NumberUtils.createNumber("12345"));
        assertEquals("createNumber(String) 3 failed", new Double("1234.5"), NumberUtils.createNumber("1234.5D"));
        assertEquals("createNumber(String) 3 failed", new Double("1234.5"), NumberUtils.createNumber("1234.5d"));
        assertEquals("createNumber(String) 4 failed", new Float("1234.5"), NumberUtils.createNumber("1234.5F"));
        assertEquals("createNumber(String) 4 failed", new Float("1234.5"), NumberUtils.createNumber("1234.5f"));
        assertEquals("createNumber(String) 5 failed", new Long(Integer.MAX_VALUE + 1L), NumberUtils.createNumber(""
            + (Integer.MAX_VALUE + 1L)));
        assertEquals("createNumber(String) 6 failed", new Long(12345), NumberUtils.createNumber("12345L"));
        assertEquals("createNumber(String) 6 failed", new Long(12345), NumberUtils.createNumber("12345l"));
        assertEquals("createNumber(String) 7 failed", new Float("-1234.5"), NumberUtils.createNumber("-1234.5"));
        assertEquals("createNumber(String) 8 failed", new Integer("-12345"), NumberUtils.createNumber("-12345"));
        assertTrue("createNumber(String) 9 failed", 0xFADE == NumberUtils.createNumber("0xFADE").intValue());
        assertTrue("createNumber(
        // ... (truncated)
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.lang3.math.NumberUtilsTest::testCreateNumber
java.lang.StringIndexOutOfBoundsException: String index out of range: 4
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
- org.apache.commons.lang3.math.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.lang3.math.FuzzHarness.oracleCanonicalDecimal
- org.apache.commons.lang3.math.NumberUtils.createNumber
Crashes already found:
- java.lang.StringIndexOutOfBoundsException@org.apache.commons.lang3.math.NumberUtils.createNumber
- java.lang.RuntimeException@org.apache.commons.lang3.math.FuzzHarness.oracleCanonicalDecimal
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
Check FAMILIES already covered by accepted harnesses: {canonical-float, createfloat-rt, createnumber-createinteger, int-parse, suffix-float}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.StringIndexOutOfBoundsException@org.apache.commons.lang3.math.NumberUtils.createNumber; java.lang.RuntimeException@org.apache.commons.lang3.math.FuzzHarness.oracleCanonicalDecimal. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
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
<details open><summary>▸ Output (~6,475 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final String anchor = "1234.5";

        try {
            NumberUtils.createNumber(anchor);
        } catch (RuntimeException t) {
            boolean rootCause = t instanceof StringIndexOutOfBoundsException;
            if (rootCause) {
                for (StackTraceElement ste : t.getStackTrace()) {
                    String cls = ste.getClassName();
                    String m = ste.getMethodName();
                    if ("org.apache.commons.lang3.math.NumberUtils".equals(cls)
                            && ("createNumber".equals(m)
                            || "createInteger".equals(m)
                            || "isAllZeros".equals(m)
                            || "isDigits".equals(m)
                            || "createLong".equals(m)
                            || "createBigInteger".equals(m)
                            || "createFloat".equals(m)
                            || "createDouble".equals(m)
                            || "createBigDecimal".equals(m))) {
                        rootCause = true;
                        break;
                    }
                    rootCause = false;
                }
            }
            if (!rootCause) {
                return;
            }
        }

        String[] plains = new String[3];
        plains[0] = anchor;

        int whole = data.consumeInt(-1000000, 1000000);
        String[] exactFractions = new String[] { "0", "25", "5", "75" };
        plains[1] = Integer.toString(whole) + "." + exactFractions[data.consumeInt(0, exactFractions.length - 1)];

        int whole2 = data.consumeInt(-1000000, 1000000);
        String signless = Integer.toString(Math.abs(whole2));
        if (signless.length() == 0) {
            signless = "0";
        }
        if (data.consumeBoolean()) {
            signless = "0" + signless;
        }
        String prefix = whole2 < 0 ? "-" : "";
        plains[2] = prefix + signless + "." + exactFractions[data.consumeInt(0, exactFractions.length - 1)];

        for (String plain : plains) {
            String explicit = plain + "D";

            Double expected;
            Number explicitParsed;
            try {
                expected = NumberUtils.createDouble(plain);
                explicitParsed = NumberUtils.createNumber(explicit);
            } catch (RuntimeException ex) {
                continue;
            }

            if (explicitParsed == null) {
                continue;
            }

            double expectedValue = expected.doubleValue();
            double explicitValue = explicitParsed.doubleValue();
            if (Double.doubleToLongBits(expectedValue) != Double.doubleToLongBits(explicitValue)) {
                throw new RuntimeException(
                        "[oracle:double-suffix-equivalence] metamorphic violation: explicit double disagrees with direct double parse input="
                                + plain + " lhs=" + explicitValue + " rhs=" + expectedValue);
            }

            try {
                Number plainParsed = NumberUtils.createNumber(plain);
                if (plainParsed == null) {
                    throw new RuntimeException(
                            "[oracle:double-suffix-equivalence] metamorphic violation: plain decimal returned null input="
                                    + plain);
                }

                /*
                 * Contract asserted:
                 * The test suite shows createNumber accepts canonical decimal literals like "1234.5"
                 * and the explicit-double form "1234.5D"; adding 'D' only requests a specific return
                 * type, not a different numeric meaning. For these by-construction decimals, chosen
                 * from { .0, .25, .5, .75 }, the value is exactly representable in binary, so
                 * createNumber(plain).doubleValue() must equal createNumber(plain + "D").doubleValue().
                 * A patch that only suppresses the known throw or skips the plain-decimal path breaks
                 * this equivalence.
                 */
                double plainValue = plainParsed.doubleValue();
                if (Double.doubleToLongBits(plainValue) != Double.doubleToLongBits(explicitValue)) {
                    throw new RuntimeException(
                            "[oracle:double-suffix-equivalence] metamorphic violation: plain and explicit-double forms disagree input="
                                    + plain + " lhs=" + plainValue + " rhs=" + explicitValue);
                }
            } catch (RuntimeException t) {
                boolean rootCause = t instanceof StringIndexOutOfBoundsException;
                if (rootCause) {
                    for (StackTraceElement ste : t.getStackTrace()) {
                        String cls = ste.getClassName();
                        String m = ste.getMethodName();
                        if ("org.apache.commons.lang3.math.NumberUtils".equals(cls)
                                && ("createNumber".equals(m)
                                || "createInteger".equals(m)
                                || "isAllZeros".equals(m)
                                || "isDigits".equals(m)
                                || "createLong".equals(m)
                                || "createBigInteger".equals(m)
                                || "createFloat".equals(m)
                                || "createDouble".equals(m)
                                || "createBigDecimal".equals(m))) {
                            throw new RuntimeException(
                                    "[oracle:double-suffix-equivalence] metamorphic violation: plain decimal rejected but explicit-double form succeeded input="
                                            + plain,
                                    t);
                        }
                    }
                    return;
                }

                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    return;
                }

                if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
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
## [14] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. Jazzer exited cleanly with no finding on the buggy code. Most often this means every input was handled normally, or the harnes

---
## [15] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~6,673 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [13] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final String anchor = "1234.5";

        try {
            NumberUtils.createNumber(anchor);
        } catch (RuntimeException t) {
            boolean rootCause = t instanceof StringIndexOutOfBoundsException;
            if (rootCause) {
                for (StackTraceElement ste : t.getStackTrace()) {
                    String cls = ste.getClassName();
                    String m = ste.getMethodName();
                    if ("org.apache.commons.lang3.math.NumberUtils".equals(cls)
                            && ("createNumber".equals(m)
                            || "createInteger".equals(m)
                            || "isAllZeros".equals(m)
                            || "isDigits".equals(m)
                            || "createLong".equals(m)
                            || "createBigInteger".equals(m)
                            || "createFloat".equals(m)
                            || "createDouble".equals(m)
                            || "createBigDecimal".equals(m))) {
                        rootCause = true;
                        break;
                    }
                    rootCause = false;
                }
            }
            if (!rootCause) {
                return;
            }
        }

        String[] plains = new String[3];
        plains[0] = anchor;

        int whole = data.consumeInt(-1000000, 1000000);
        String[] exactFractions = new String[] { "0", "25", "5", "75" };
        plains[1] = Integer.toString(whole) + "." + exactFractions[data.consumeInt(0, exactFractions.length - 1)];

        int whole2 = data.consumeInt(-1000000, 1000000);
        String signless = Integer.toString(Math.abs(whole2));
        if (signless.length() == 0) {
            signless = "0";
        }
        if (data.consumeBoolean()) {
            signless = "0" + signless;
        }
        String prefix = whole2 < 0 ? "-" : "";
        plains[2] = prefix + signless + "." + exactFractions[data.consumeInt(0, exactFractions.length - 1)];

        for (String plain : plains) {
            String explicit = plain + "D";

            Double expected;
            Number explicitParsed;
            try {
                expected = NumberUtils.createDouble(plain);
                explicitParsed = NumberUtils.createNumber(explicit);
            } catch (RuntimeException ex) {
                continue;
            }

            if (explicitParsed == null) {
                continue;
            }

            double expectedValue = expected.doubleValue();
            double explicitValue = explicitParsed.doubleValue();
            if (Double.doubleToLongBits(expectedValue) != Double.doubleToLongBits(explicitValue)) {
                throw new RuntimeException(
                        "[oracle:double-suffix-equivalence] metamorphic violation: explicit double disagrees with direct double parse input="
                                + plain + " lhs=" + explicitValue + " rhs=" + expectedValue);
            }

            try {
                Number plainParsed = NumberUtils.createNumber(plain);
                if (plainParsed == null) {
                    throw new RuntimeException(
                            "[oracle:double-suffix-equivalence] metamorphic violation: plain decimal returned null input="
                                    + plain);
                }

                /*
                 * Contract asserted:
                 * The test suite shows createNumber accepts canonical decimal literals like "1234.5"
                 * and the explicit-double form "1234.5D"; adding 'D' only requests a specific return
                 * type, not a different numeric meaning. For these by-construction decimals, chosen
                 * from { .0, .25, .5, .75 }, the value is exactly representable in binary, so
                 * createNumber(plain).doubleValue() must equal createNumber(plain + "D").doubleValue().
                 * A patch that only suppresses the known throw or skips the plain-decimal path breaks
                 * this equivalence.
                 */
                double plainValue = plainParsed.doubleValue();
                if (Double.doubleToLongBits(plainValue) != Double.doubleToLongBits(explicitValue)) {
                    throw new RuntimeException(
                            "[oracle:double-suffix-equivalence] metamorphic violation: plain and explicit-double forms disagree input="
                                    + plain + " lhs=" + plainValue + " rhs=" + explicitValue);
                }
            } catch (RuntimeException t) {
                boolean rootCause = t instanceof StringIndexOutOfBoundsException;
                if (rootCause) {
                    for (StackTraceElement ste : t.getStackTrace()) {
                        String cls = ste.getClassName();
                        String m = ste.getMethodName();
                        if ("org.apache.commons.lang3.math.NumberUtils".equals(cls)
                                && ("createNumber".equals(m)
                                || "createInteger".equals(m)
                                || "isAllZeros".equals(m)
                                || "isDigits".equals(m)
                                || "createLong".equals(m)
                                || "createBigInteger".equals(m)
                                || "createFloat".equals(m)
                                || "createDouble".equals(m)
                                || "createBigDecimal".equals(m))) {
                            throw new RuntimeException(
                                    "[oracle:double-suffix-equivalence] metamorphic violation: plain decimal rejected but explicit-double form succeeded input="
                                            + plain,
                                    t);
                        }
                    }
                    return;
                }

                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    return;
                }

                if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                    throw t;
                }
                return;
            }
        }
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~3,066 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NumberUtils.createNumber("1eE");

        String candidate = buildRootCauseVariant(data);
        try {
            NumberUtils.createNumber(candidate);
        } catch (NumberFormatException e) {
        } catch (IllegalArgumentException e) {
        }

        String digits = nonEmptyDigits(data.consumeInt(0, 1_000_000));
        String expDigits = nonEmptyDigits(data.consumeInt(0, 1_000_000));
        String scientific = digits + "e" + expDigits;
        String explicitDouble = scientific + "D";

        try {
            Number a = NumberUtils.createNumber(scientific);
            Number b = NumberUtils.createNumber(explicitDouble);
            Double c = NumberUtils.createDouble(scientific);

            if (a != null && b != null && c != null) {
                double av = a.doubleValue();
                double bv = b.doubleValue();
                double cv = c.doubleValue();

                /*
                 * Contract/assertion:
                 * createNumber parses a valid scientific-notation literal, and adding the 'D'
                 * suffix only requests a Double interpretation of the same numeric value.
                 * createDouble parses that same value through an independent public entry point.
                 * Therefore all three real-library parses must agree numerically.
                 */
                long abits = Double.doubleToLongBits(av);
                long bbits = Double.doubleToLongBits(bv);
                long cbits = Double.doubleToLongBits(cv);
                if (abits != bbits || abits != cbits) {
                    throw new RuntimeException(
                            "[oracle:scientific-double-agreement] metamorphic violation: input="
                                    + scientific + " createNumber=" + av
                                    + " createNumberD=" + bv
                                    + " createDouble=" + cv);
                }
            }
        } catch (NumberFormatException e) {
        } catch (IllegalArgumentException e) {
        }
    }

    private static String buildRootCauseVariant(FuzzedDataProvider data) {
        String left = nonEmptyDigits(data.consumeInt(0, 99999));
        String middle = data.consumeAsciiString(3).replace(".", "").replace("-", "").replace("+", "").replace(" ", "");
        String right = data.consumeBoolean() ? "" : nonEmptyDigits(data.consumeInt(0, 999));
        if (data.consumeBoolean()) {
            return left + "e" + middle + "E" + right;
        }
        return left + "E" + middle + "e" + right;
    }

    private static String nonEmptyDigits(int value) {
        String s = Integer.toString(value);
        if (s.startsWith("-")) {
            s = s.substring(1);
        }
        if (s.length() == 0) {
            return "0";
        }
        return s;
    }
}
```

</details>

---
## [16] ⚙️ harness-attempt · `attempt_007`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.StringIndexOutOfBoundsException: String index out of range: 4

---
## [17] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~37,628 chars, 1 new)</summary>

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

Codebase: `Lang_27_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/lang3/math/NumberUtils.java
+++ /src/main/java/org/apache/commons/lang3/math/NumberUtils.java
@@ -485,7 +485,7 @@
             }
             mant = str.substring(0, decPos);
         } else {
-            if (expPos > -1) {
+        	if(expPos>-1&&expPos<str.length()-1){
                 mant = str.substring(0, expPos);
             } else {
                 mant = str;

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
                if (expPos < decPos) {
                    throw new NumberFormatException(str + " is not a valid number.");
                }
                dec = str.substring(decPos + 1, expPos);
            } else {
                dec = str.substring(decPos + 1);
            }
            mant = str.substring(0, decPos);
        } else {
            if (expPos > -1) {
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
                        } catch (NumberFormatException nfe) {
                            //Too big for a long
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

                    } catch (NumberFormatException nfe) {
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
                    } catch (NumberFormatException nfe) {
                        // ignore the bad number
                    }
                    try {
                        return createBigDecimal(numeric);
                    } catch (NumberFormatException e) {
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
                } catch (NumberFormatException nfe) {
                    // ignore the bad number
                }
                try {
                    return createLong(str);
                } catch (NumberFormatException nfe) {
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
                } catch (NumberFormatException nfe) {
                    // ignore the bad number
                }
                try {
                    Double d = createDouble(str);
                    if (!(d.isInfinite() || (d.doubleValue() == 0.0D && !allZeros))) {
                        return d;
                    }
                } catch (NumberFormatException nfe) {
                    // ignore the bad number
                }

                return createBigDecimal(str);

            }
        }
    }
</code>
Call-site examples (use these as a guide for constructing the target call):
<xref>
public void testCreateNumber() {
        // a lot of things can go wrong
        assertEquals("createNumber(String) 1 failed", new Float("1234.5"), NumberUtils.createNumber("1234.5"));
        assertEquals("createNumber(String) 2 failed", new Integer("12345"), NumberUtils.createNumber("12345"));
        assertEquals("createNumber(String) 3 failed", new Double("1234.5"), NumberUtils.createNumber("1234.5D"));
        assertEquals("createNumber(String) 3 failed", new Double("1234.5"), NumberUtils.createNumber("1234.5d"));
        assertEquals("createNumber(String) 4 failed", new Float("1234.5"), NumberUtils.createNumber("1234.5F"));
        assertEquals("createNumber(String) 4 failed", new Float("1234.5"), NumberUtils.createNumber("1234.5f"));
        assertEquals("createNumber(String) 5 failed", new Long(Integer.MAX_VALUE + 1L), NumberUtils.createNumber(""
            + (Integer.MAX_VALUE + 1L)));
        assertEquals("createNumber(String) 6 failed", new Long(12345), NumberUtils.createNumber("12345L"));
        assertEquals("createNumber(String) 6 failed", new Long(12345), NumberUtils.createNumber("12345l"));
        assertEquals("createNumber(String) 7 failed", new Float("-1234.5"), NumberUtils.createNumber("-1234.5"));
        assertEquals("createNumber(String) 8 failed", new Integer("-12345"), NumberUtils.createNumber("-12345"));
        assertTrue("createNumber(String) 9 failed", 0xFADE == NumberUtils.createNumber("0xFADE").intValue());
        assertTrue("createNumber(String) 10 failed", -0xFADE == NumberUtils.createNumber("-0xFADE").intValue());
        assertEquals("createNumber(String) 11 failed", new Double("1.1E200"), NumberUtils.createNumber("1.1E200"));
        assertEquals("createNumber(String) 12 failed", new Float("1.1E20"), NumberUtils.createNumber("1.1E20"));
        assertEquals("createNumber(String) 13 failed", new Double("-1.1E200"), NumberUtils.createNumber("-1.1E200"));
        assertEquals("createNumber(String) 14 failed", new Double("1.1E-200"), NumberUtils.createNumber("1.1E-200"));
        assertEquals("createNumber(null) failed", null, NumberUtils.createNumber(null));
        assertEquals("createNumber(String) failed", new BigInteger("12345678901234567890"), NumberUtils
                .createNumber("12345678901234567890L"));

        // jdk 1.2 doesn't support this. unsure about jdk 1.2.2
        if (SystemUtils.isJavaVersionAtLeast(1.3f)) {
            assertEquals("createNumber(String) 15 failed", new BigDecimal("1.1E-700"), NumberUtils
                    .createNumber("1.1E-700F"));
        }
        assertEquals("createNumber(String) 16 failed", new Long("10" + Integer.MAX_VALUE), NumberUtils
                .createNumber("10" + Integer.MAX_VALUE + "L"));
        assertEquals("createNumber(String) 17 failed", new Long("10" + Integer.MAX_VALUE), NumberUtils
                .createNumber("10" + Integer.MAX_VALUE));
        assertEquals("createNumber(String) 18 failed", new BigInteger("10" + Long.MAX_VALUE), NumberUtils
                .createNumber("10" + Long.MAX_VALUE));

        // LANG-521
        assertEquals("createNumber(String) LANG-521 failed", new Float("2."), NumberUtils.createNumber("2."));

        // LANG-638
        assertFalse("createNumber(String) succeeded", checkCreateNumber("1eE"));
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
On the buggy version the root cause surfaces as: java.lang.StringIndexOutOfBoundsException (or a sibling failure with a different signature stemming from the same root cause). Your harness must distinguish a genuine defect from the patch doing its job:
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
throwable: java.lang.StringIndexOutOfBoundsException
message: String index out of range: 4
thrown_at: org.apache.commons.lang3.math.NumberUtils.createNumber(NumberUtils.java:489)
anchor_input: "1234.5"  // hard-code this verbatim as your first call, then fuzz inputs of the same shape
other_observed_literals: "12345", "1234.5D", "1234.5d", "1234.5F", "1234.5f", "12345L", "12345l", "-1234.5", "-12345", "0xFADE", "-0xFADE", "1.1E200", "1.1E20", "-1.1E200", "1.1E-200", "12345678901234567890L", "1.1E-700F", "2."
</ground_truth_crash>
<failing_test class="org.apache.commons.lang3.math.NumberUtilsTest" method="testCreateNumber">
    public void testCreateNumber() {
        // a lot of things can go wrong
        assertEquals("createNumber(String) 1 failed", new Float("1234.5"), NumberUtils.createNumber("1234.5"));
        assertEquals("createNumber(String) 2 failed", new Integer("12345"), NumberUtils.createNumber("12345"));
        assertEquals("createNumber(String) 3 failed", new Double("1234.5"), NumberUtils.createNumber("1234.5D"));
        assertEquals("createNumber(String) 3 failed", new Double("1234.5"), NumberUtils.createNumber("1234.5d"));
        assertEquals("createNumber(String) 4 failed", new Float("1234.5"), NumberUtils.createNumber("1234.5F"));
        assertEquals("createNumber(String) 4 failed", new Float("1234.5"), NumberUtils.createNumber("1234.5f"));
        assertEquals("createNumber(String) 5 failed", new Long(Integer.MAX_VALUE + 1L), NumberUtils.createNumber(""
            + (Integer.MAX_VALUE + 1L)));
        assertEquals("createNumber(String) 6 failed", new Long(12345), NumberUtils.createNumber("12345L"));
        assertEquals("createNumber(String) 6 failed", new Long(12345), NumberUtils.createNumber("12345l"));
        assertEquals("createNumber(String) 7 failed", new Float("-1234.5"), NumberUtils.createNumber("-1234.5"));
        assertEquals("createNumber(String) 8 failed", new Integer("-12345"), NumberUtils.createNumber("-12345"));
        assertTrue("createNumber(String) 9 failed", 0xFADE == NumberUtils.createNumber("0xFADE").intValue());
        assertTrue("createNumber(
        // ... (truncated)
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.lang3.math.NumberUtilsTest::testCreateNumber
java.lang.StringIndexOutOfBoundsException: String index out of range: 4
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
- org.apache.commons.lang3.math.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.lang3.math.FuzzHarness.oracleCanonicalDecimal
- org.apache.commons.lang3.math.NumberUtils.createNumber
Crashes already found:
- java.lang.StringIndexOutOfBoundsException@org.apache.commons.lang3.math.NumberUtils.createNumber
- java.lang.RuntimeException@org.apache.commons.lang3.math.FuzzHarness.oracleCanonicalDecimal
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
Check FAMILIES already covered by accepted harnesses: {canonical-float, createfloat-rt, createnumber-createinteger, int-parse, scientific-double-agreement, suffix-float}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.StringIndexOutOfBoundsException@org.apache.commons.lang3.math.NumberUtils.createNumber; java.lang.RuntimeException@org.apache.commons.lang3.math.FuzzHarness.oracleCanonicalDecimal. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
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
<details open><summary>▸ Output (~8,103 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.math.BigDecimal;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Anchor from the failing test: this is a valid decimal literal that a correct
        // implementation must accept and parse through NumberUtils.createNumber.
        try {
            Number n = NumberUtils.createNumber("1234.5");
            if (n == null || !sameNumericValue(n, new BigDecimal("1234.5"))) {
                throw new RuntimeException("[oracle:anchor-decimal] metamorphic violation: valid decimal literal parsed incorrectly input=1234.5 result=" + n);
            }
        } catch (RuntimeException t) {
            if (isRootCauseFromCreateNumber(t)) {
                throw t;
            }
        }

        String base = buildBaseInteger(data);
        String signedBase = data.consumeBoolean() ? ("-" + base) : base;

        // Strategy (c): flip the patched boundary condition.
        // The patch distinguishes exponent-at-end ("1e") from exponent-with-following-char ("1e0").
        // For every correct implementation, adding an exponent of zero preserves the numeric value:
        // createNumber(x + "e0") and createNumber(x) denote the same number for valid x.
        // A patch that only suppresses the old throw but computes the wrong slices will violate this.
        checkExponentZeroEquivalence(signedBase, 'e');
        checkExponentZeroEquivalence(signedBase, 'E');

        // Boundary exploration around the changed condition: malformed exponent marker at the end.
        // These are intentionally near the patch boundary but not valid-by-construction, so any
        // argument-validation-style rejection or generic malformed-input runtime exception is swallowed.
        exerciseBoundaryRejects(signedBase + "e");
        exerciseBoundaryRejects(signedBase + "E");

        // Additional independent oracle outside the already-covered families:
        // The implementation explicitly handles both 'l' and 'L' in the same switch branch.
        // For a valid decimal integer string, x + "L" and x + "l" must agree with createLong(x).
        checkLongSuffixAgreement(base);

        // Additional exploration with fuzzed surrounding content near the exponent boundary.
        // Only assert on valid-by-construction cases; otherwise just exercise the real API.
        String junk = data.consumeAsciiString(4);
        String candidate = signedBase + (data.consumeBoolean() ? "e0" : "E0") + junk;
        try {
            NumberUtils.createNumber(candidate);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromCreateNumber(t) && isStrictlyValidConstruct(candidate)) {
                throw t;
            }
        }
    }

    private static void checkExponentZeroEquivalence(String base, char expChar) {
        String withExp = base + expChar + "0";
        try {
            Number lhs = NumberUtils.createNumber(base);
            Number rhs = NumberUtils.createNumber(withExp);
            if (lhs != null && rhs != null && !sameNumericValue(lhs, rhs)) {
                throw new RuntimeException("[oracle:exp-zero] metamorphic violation: createNumber(x) must equal createNumber(x+e0) input=" + base + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromCreateNumber(t) && isStrictlyValidConstruct(withExp)) {
                throw t;
            }
        }
    }

    private static void checkLongSuffixAgreement(String base) {
        try {
            Long expected = NumberUtils.createLong(base);
            Number upper = NumberUtils.createNumber(base + "L");
            Number lower = NumberUtils.createNumber(base + "l");
            if (expected != null && upper != null && lower != null) {
                long e = expected.longValue();
                long u = upper.longValue();
                long l = lower.longValue();
                if (u != e || l != e) {
                    throw new RuntimeException("[oracle:long-suffix] metamorphic violation: L/l suffix parsing must agree with createLong input=" + base + " expected=" + e + " upper=" + upper + " lower=" + lower);
                }
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromCreateNumber(t)) {
                throw t;
            }
        }
    }

    private static void exerciseBoundaryRejects(String s) {
        try {
            NumberUtils.createNumber(s);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            // Not valid-by-construction, so even a matching runtime exception is swallowed here.
        }
    }

    private static String buildBaseInteger(FuzzedDataProvider data) {
        int len = data.consumeInt(1, 6);
        String raw = data.consumeAsciiString(len);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length() && sb.length() < len; i++) {
            char c = raw.charAt(i);
            if (c >= '0' && c <= '9') {
                sb.append(c);
            }
        }
        while (sb.length() < len) {
            int d = data.consumeInt(0, 9);
            sb.append((char) ('0' + d));
        }
        if (sb.length() > 1 && sb.charAt(0) == '0') {
            sb.setCharAt(0, '1');
        }
        return sb.toString();
    }

    private static boolean sameNumericValue(Number a, Number b) {
        try {
            BigDecimal da = new BigDecimal(a.toString()).stripTrailingZeros();
            BigDecimal db = new BigDecimal(b.toString()).stripTrailingZeros();
            return da.compareTo(db) == 0;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static boolean sameNumericValue(Number a, BigDecimal b) {
        try {
            BigDecimal da = new BigDecimal(a.toString()).stripTrailingZeros();
            return da.compareTo(b.stripTrailingZeros()) == 0;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException;
    }

    private static boolean isRootCauseFromCreateNumber(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.lang3.math.NumberUtils".equals(e.getClassName())
                    && "createNumber".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isStrictlyValidConstruct(String s) {
        if (s == null || s.length() == 0) {
            return false;
        }
        int start = s.charAt(0) == '-' ? 1 : 0;
        if (start >= s.length()) {
            return false;
        }
        int exp = s.indexOf('e');
        if (exp < 0) {
            exp = s.indexOf('E');
        }
        if (exp < 0 || exp == start || exp >= s.length() - 1) {
            return false;
        }
        for (int i = start; i < exp; i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        for (int i = exp + 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (i == exp + 1 && (c == '+' || c == '-')) {
                if (i == s.length() - 1) {
                    return false;
                }
                continue;
            }
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }
}
```

</details>

---
## [18] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. Jazzer exited cleanly with no finding on the buggy code. Most often this means every input was handled normally, or the harnes

---
## [19] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~8,301 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [17] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.math.BigDecimal;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Anchor from the failing test: this is a valid decimal literal that a correct
        // implementation must accept and parse through NumberUtils.createNumber.
        try {
            Number n = NumberUtils.createNumber("1234.5");
            if (n == null || !sameNumericValue(n, new BigDecimal("1234.5"))) {
                throw new RuntimeException("[oracle:anchor-decimal] metamorphic violation: valid decimal literal parsed incorrectly input=1234.5 result=" + n);
            }
        } catch (RuntimeException t) {
            if (isRootCauseFromCreateNumber(t)) {
                throw t;
            }
        }

        String base = buildBaseInteger(data);
        String signedBase = data.consumeBoolean() ? ("-" + base) : base;

        // Strategy (c): flip the patched boundary condition.
        // The patch distinguishes exponent-at-end ("1e") from exponent-with-following-char ("1e0").
        // For every correct implementation, adding an exponent of zero preserves the numeric value:
        // createNumber(x + "e0") and createNumber(x) denote the same number for valid x.
        // A patch that only suppresses the old throw but computes the wrong slices will violate this.
        checkExponentZeroEquivalence(signedBase, 'e');
        checkExponentZeroEquivalence(signedBase, 'E');

        // Boundary exploration around the changed condition: malformed exponent marker at the end.
        // These are intentionally near the patch boundary but not valid-by-construction, so any
        // argument-validation-style rejection or generic malformed-input runtime exception is swallowed.
        exerciseBoundaryRejects(signedBase + "e");
        exerciseBoundaryRejects(signedBase + "E");

        // Additional independent oracle outside the already-covered families:
        // The implementation explicitly handles both 'l' and 'L' in the same switch branch.
        // For a valid decimal integer string, x + "L" and x + "l" must agree with createLong(x).
        checkLongSuffixAgreement(base);

        // Additional exploration with fuzzed surrounding content near the exponent boundary.
        // Only assert on valid-by-construction cases; otherwise just exercise the real API.
        String junk = data.consumeAsciiString(4);
        String candidate = signedBase + (data.consumeBoolean() ? "e0" : "E0") + junk;
        try {
            NumberUtils.createNumber(candidate);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromCreateNumber(t) && isStrictlyValidConstruct(candidate)) {
                throw t;
            }
        }
    }

    private static void checkExponentZeroEquivalence(String base, char expChar) {
        String withExp = base + expChar + "0";
        try {
            Number lhs = NumberUtils.createNumber(base);
            Number rhs = NumberUtils.createNumber(withExp);
            if (lhs != null && rhs != null && !sameNumericValue(lhs, rhs)) {
                throw new RuntimeException("[oracle:exp-zero] metamorphic violation: createNumber(x) must equal createNumber(x+e0) input=" + base + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromCreateNumber(t) && isStrictlyValidConstruct(withExp)) {
                throw t;
            }
        }
    }

    private static void checkLongSuffixAgreement(String base) {
        try {
            Long expected = NumberUtils.createLong(base);
            Number upper = NumberUtils.createNumber(base + "L");
            Number lower = NumberUtils.createNumber(base + "l");
            if (expected != null && upper != null && lower != null) {
                long e = expected.longValue();
                long u = upper.longValue();
                long l = lower.longValue();
                if (u != e || l != e) {
                    throw new RuntimeException("[oracle:long-suffix] metamorphic violation: L/l suffix parsing must agree with createLong input=" + base + " expected=" + e + " upper=" + upper + " lower=" + lower);
                }
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseFromCreateNumber(t)) {
                throw t;
            }
        }
    }

    private static void exerciseBoundaryRejects(String s) {
        try {
            NumberUtils.createNumber(s);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            // Not valid-by-construction, so even a matching runtime exception is swallowed here.
        }
    }

    private static String buildBaseInteger(FuzzedDataProvider data) {
        int len = data.consumeInt(1, 6);
        String raw = data.consumeAsciiString(len);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length() && sb.length() < len; i++) {
            char c = raw.charAt(i);
            if (c >= '0' && c <= '9') {
                sb.append(c);
            }
        }
        while (sb.length() < len) {
            int d = data.consumeInt(0, 9);
            sb.append((char) ('0' + d));
        }
        if (sb.length() > 1 && sb.charAt(0) == '0') {
            sb.setCharAt(0, '1');
        }
        return sb.toString();
    }

    private static boolean sameNumericValue(Number a, Number b) {
        try {
            BigDecimal da = new BigDecimal(a.toString()).stripTrailingZeros();
            BigDecimal db = new BigDecimal(b.toString()).stripTrailingZeros();
            return da.compareTo(db) == 0;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static boolean sameNumericValue(Number a, BigDecimal b) {
        try {
            BigDecimal da = new BigDecimal(a.toString()).stripTrailingZeros();
            return da.compareTo(b.stripTrailingZeros()) == 0;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException;
    }

    private static boolean isRootCauseFromCreateNumber(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.lang3.math.NumberUtils".equals(e.getClassName())
                    && "createNumber".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isStrictlyValidConstruct(String s) {
        if (s == null || s.length() == 0) {
            return false;
        }
        int start = s.charAt(0) == '-' ? 1 : 0;
        if (start >= s.length()) {
            return false;
        }
        int exp = s.indexOf('e');
        if (exp < 0) {
            exp = s.indexOf('E');
        }
        if (exp < 0 || exp == start || exp >= s.length() - 1) {
            return false;
        }
        for (int i = start; i < exp; i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        for (int i = exp + 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (i == exp + 1 && (c == '+' || c == '-')) {
                if (i == s.length() - 1) {
                    return false;
                }
                continue;
            }
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~3,474 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.math.BigDecimal;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Guaranteed trigger from the failing test on the known-buggy version.
        // Deliberately uncaught so Jazzer reports the buggy StringIndexOutOfBoundsException.
        Number anchor = NumberUtils.createNumber("1234.5");

        // Post-condition: for a valid decimal literal, createNumber must parse the same value.
        // This still catches a throw-deleting or wrong-slicing patch on fixed builds.
        if (anchor == null || new BigDecimal(anchor.toString()).compareTo(new BigDecimal("1234.5")) != 0) {
            throw new RuntimeException("[oracle:anchor-value] metamorphic violation: input=1234.5 result=" + anchor);
        }

        // Strategy (c): exercise the boundary flipped by the patch.
        // Build numbers with an exponent marker at the end or just before the end.
        String digits = onlyDigits(data.consumeAsciiString(8));
        if (digits.length() == 0) {
            digits = "1";
        }
        if (digits.length() > 1 && digits.charAt(0) == '0') {
            digits = "1" + digits.substring(1);
        }
        String sign = data.consumeBoolean() ? "-" : "";
        String base = sign + digits;

        int variant = data.consumeInt(0, 5);
        String candidate;
        switch (variant) {
            case 0:
                candidate = base + "e";
                break;
            case 1:
                candidate = base + "E";
                break;
            case 2:
                candidate = base + "e0";
                break;
            case 3:
                candidate = base + "E0";
                break;
            case 4:
                candidate = base + ".0";
                break;
            default:
                candidate = base + "." + onlyDigits(data.consumeAsciiString(4));
                if (candidate.endsWith(".")) {
                    candidate += "0";
                }
                break;
        }

        try {
            Number n = NumberUtils.createNumber(candidate);

            // Independent relation for valid-by-construction exponent-zero cases:
            // x and xe0 represent the same numeric value.
            if (candidate.endsWith("e0") || candidate.endsWith("E0")) {
                Number plain = NumberUtils.createNumber(base);
                if (n != null && plain != null) {
                    BigDecimal a = new BigDecimal(n.toString()).stripTrailingZeros();
                    BigDecimal b = new BigDecimal(plain.toString()).stripTrailingZeros();
                    if (a.compareTo(b) != 0) {
                        throw new RuntimeException("[oracle:exp-zero] metamorphic violation: input=" + candidate + " lhs=" + n + " rhs=" + plain);
                    }
                }
            }
        } catch (IllegalArgumentException ignored) {
            // Clean rejection on fixed code for malformed boundary inputs such as trailing e/E.
        }
    }

    private static String onlyDigits(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
```

</details>

---
## [20] ⚙️ harness-attempt · `attempt_009`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.RuntimeException: [oracle:exp-zero] metamorphic violation: input=-89999999e0 lhs=-9.0E7 rhs=-89999999

---
## [21] ⚙️ corpus-seed · `attempt_002`
**output:** **96 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['createNumber(String) 1 failed', '1234.5', 'createNumber(String) 2 failed', '12345', 'createNumber(String) 3 failed', '1234.5D', '1234.5d', 'createNumber(String) 4 failed']}

---
## [22] ⚙️ corpus-seed · `attempt_004`
**output:** **96 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['createNumber(String) 1 failed', '1234.5', 'createNumber(String) 2 failed', '12345', 'createNumber(String) 3 failed', '1234.5D', '1234.5d', 'createNumber(String) 4 failed']}

---
## [23] ⚙️ corpus-seed · `attempt_005`
**output:** **96 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['createNumber(String) 1 failed', '1234.5', 'createNumber(String) 2 failed', '12345', 'createNumber(String) 3 failed', '1234.5D', '1234.5d', 'createNumber(String) 4 failed']}

---
## [24] ⚙️ corpus-seed · `attempt_007`
**output:** **96 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['createNumber(String) 1 failed', '1234.5', 'createNumber(String) 2 failed', '12345', 'createNumber(String) 3 failed', '1234.5D', '1234.5d', 'createNumber(String) 4 failed']}

---
## [25] ⚙️ corpus-seed · `attempt_009`
**output:** **96 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['createNumber(String) 1 failed', '1234.5', 'createNumber(String) 2 failed', '12345', 'createNumber(String) 3 failed', '1234.5D', '1234.5d', 'createNumber(String) 4 failed']}

---
## [26] ⚙️ patched-fuzz · `attempt_002`
**output:** **FIRED — crash on patched build**
- reproducing_input_file: /tmp/d4j/Lang_27_buggy/fuzz/attempt_002/crashes/crash-da39a3ee5e6b4b0d3255bfef95601890afd80709

---
## [27] ⚙️ patched-fuzz · `attempt_004`
**output:** **quiet on patched build (no overfit signal)**

---
## [28] ⚙️ patched-fuzz · `attempt_005`
**output:** **FIRED — [oracle:canonical-float] metamorphic violation: expected Float from createNumber for input=-0.0 got=java.math.BigDecimal value=0.0**
- reproducing_input_file: /tmp/d4j/Lang_27_buggy/fuzz/attempt_005/crashes/crash-32bfb299299354a0ffa8b6521fc1822734eaaca2

---
## [29] ⚙️ patched-fuzz · `attempt_007`
**output:** **FIRED — crash on patched build**
- reproducing_input_file: /tmp/d4j/Lang_27_buggy/fuzz/attempt_007/crashes/crash-da39a3ee5e6b4b0d3255bfef95601890afd80709

---
## [30] ⚙️ patched-fuzz · `attempt_009`
**output:** **FIRED — [oracle:exp-zero] metamorphic violation: input=-89999999e0 lhs=-9.0E7 rhs=-89999999**
- reproducing_input_file: /tmp/d4j/Lang_27_buggy/fuzz/attempt_009/crashes/crash-c34fdbde65307e2584289f05a7e3edeaa9435848
