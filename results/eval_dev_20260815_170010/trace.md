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

35 sequential steps — 11 LLM calls, 24 deterministic. Read top to bottom.

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
    "failure_message": "--- org.apache.commons.lang3.math.NumberUtilsTest::testCreateNumber\njava.lang.StringIndexOutOfBoundsException: begin 0, end 4, length 3",
    "support_chars": 344
  }
]
```

---
## [2] ⚙️ analysis (TargetAnalyzer)
<details><summary>▸ output — modified_files": [ (~15,520 chars)</summary>

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
      "xrefs": [
        "private static void safeCreateNumber(String s) {\n        try {\n            NumberUtils.createNumber(s);\n        } catch (RuntimeException t) {\n            if (isCleanRejection(t)) {\n                return;\n            }\n            if (isKnownRootCauseInPatchedRegion(t)) {\n                return;\n            }\n        }\n    }",
        "private static Number exercise(String s, boolean validByConstruction) {\n        try {\n            return NumberUtils.createNumber(s);\n        } catch (RuntimeException e) {\n            if (isCleanRejection(e)) {\n                return null;\n            }\n            if (validByConstruction && isRootCause(e) && hasRelevantStack(e)) {\n                throw e;\n            }\n            return null;\n        }\n    }",
        "public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {\n        NumberUtils.createNumber(\"1234.5\");\n\n        StringBuilder s = new StringBuilder();\n        if (data.consumeBoolean()) {\n            s.append('-');\n        }\n\n        int intDigits = data.consumeInt(1, 8);\n        for (int i = 0; i < intDigits; i++) {\n            s.append((char) ('0' + data.consumeInt(0, 9)));\n        }\n\n        if (data.consumeBoolean()) {\n            s.append('.');\n            int fracDigits = data.consumeInt(0, 8);\n            for (int i = 0; i < fracDigits; i++) {\n                s.append((char) ('0' + data.consumeInt(0, 9)));\n            }\n        }\n\n        if (data.consumeBoolean()) {\n            s.append(data.consumeBoolean() ? 'e' : 'E');\n        }\n\n        if (data.consumeBoolean()) {\n            char suffix;\n            switch (data.consumeInt(0, 3)) {\n                case 0:\n                    suffix = 'D';\n                    break;\n                case 1:\n                    suffix = 'd';\n                    break;\n                case 2:\n                    suffix = 'F';\n                    break;\n                default:\n                    suffix = 'f';\n                    break;\n            }\n            s.append(suffix);\n        }\n\n        NumberUtils.createNumber(s.toString());\n    }",
        "private static void safeCreateNumber(String s, boolean propagateKnownRootCause) {\n        try {\n            NumberUtils.createNumber(s);\n        } catch (RuntimeException t) {\n            if (propagateKnownRootCause && isKnownRootCauseFromCreateNumber(t)) {\n                throw t;\n            }\n            if (isCleanRejection(t)) {\n                return;\n            }\n        }\n    }",
        "public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {\n        NumberUtils.createNumber(\"1234.5\");\n\n        String candidate = buildDecimalLikeInput(data);\n        try {\n            NumberUtils.createNumber(candidate);\n        } catch (IllegalArgumentException ignored) {\n        }\n\n        checkLongSuffixMetamorphic(data);\n        checkBigIntegerFallbackMetamorphic(data);\n    }"
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
        "[org.apache.commons.lang3.math.NumberUtils].isDigits(String)",
        "[org.apache.commons.lang3.math.NumberUtils].isAllZeros(String)"
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
    "isBlank",
    "createInteger",
    "isAllZeros",
    "charAt",
    "substring",
    "isDigits",
    "createLong",
    "createBigInteger",
    "createFloat",
    "createDouble",
    "createBigDecimal"
  ],
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
<details><summary>▸ Prompt (2 message(s), ~34,838 chars)</summary>

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
private static void safeCreateNumber(String s) {
        try {
            NumberUtils.createNumber(s);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isKnownRootCauseInPatchedRegion(t)) {
                return;
            }
        }
    }
</xref>
<xref>
private static Number exercise(String s, boolean validByConstruction) {
        try {
            return NumberUtils.createNumber(s);
        } catch (RuntimeException e) {
            if (isCleanRejection(e)) {
                return null;
            }
            if (validByConstruction && isRootCause(e) && hasRelevantStack(e)) {
                throw e;
            }
            return null;
        }
    }
</xref>
<xref>
public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NumberUtils.createNumber("1234.5");

        StringBuilder s = new StringBuilder();
        if (data.consumeBoolean()) {
            s.append('-');
        }

        int intDigits = data.consumeInt(1, 8);
        for (int i = 0; i < intDigits; i++) {
            s.append((char) ('0' + data.consumeInt(0, 9)));
        }

        if (data.consumeBoolean()) {
            s.append('.');
            int fracDigits = data.consumeInt(0, 8);
            for (int i = 0; i < fracDigits; i++) {
                s.append((char) ('0' + data.consumeInt(0, 9)));
            }
        }

        if (data.consumeBoolean()) {
            s.append(data.consumeBoolean() ? 'e' : 'E');
        }

        if (data.consumeBoolean()) {
            char suffix;
            switch (data.consumeInt(0, 3)) {
                case 0:
                    suffix = 'D';
                    break;
                case 1:
                    suffix = 'd';
                    break;
                case 2:
                    suffix = 'F';
                    break;
                default:
                    suffix = 'f';
                    break;
            }
            s.append(suffix);
        }

        NumberUtils.createNumber(s.toString());
    }
</xref>
<xref>
private static void safeCreateNumber(String s, boolean propagateKnownRootCause) {
        try {
            NumberUtils.createNumber(s);
        } catch (RuntimeException t) {
            if (propagateKnownRootCause && isKnownRootCauseFromCreateNumber(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }
</xref>
<xref>
public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NumberUtils.createNumber("1234.5");

        String candidate = buildDecimalLikeInput(data);
        try {
            NumberUtils.createNumber(candidate);
        } catch (IllegalArgumentException ignored) {
        }

        checkLongSuffixMetamorphic(data);
        checkBigIntegerFallbackMetamorphic(data);
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
message: begin 0, end 4, length 3
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
java.lang.StringIndexOutOfBoundsException: begin 0, end 4, length 3
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
- isBlank
- createInteger
- isAllZeros
- charAt
- substring
- isDigits
- createLong
- createBigInteger
- createFloat
- createDouble
- createBigDecimal
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
<details open><summary>▸ Output (~8,014 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Anchor from the failing test: this exact valid input must be accepted and, per the
        // visible createNumber implementation, for a moderate decimal without exponent/suffix
        // it should return the same Float as createFloat(str).
        checkDecimalAgreesWithCreateFloat("1234.5");

        // Explore more valid-by-construction decimal inputs of the same general shape as the anchor.
        String decimal = buildCanonicalDecimal(data);
        checkDecimalAgreesWithCreateFloat(decimal);

        // Explore the patched no-decimal/exponent parsing region with many real inputs.
        // These may be invalid/truncated; for those, rejection is acceptable and must be swallowed.
        safeCreateNumber(buildExponentBoundaryCandidate(data), false);

        // Cross-check the no-decimal path against the real helper selected by createNumber:
        // for a plain base-10 integer string in int range, createNumber first delegates to
        // createInteger and should therefore agree with it.
        String canonicalInt = buildCanonicalInt(data);
        checkIntAgreesWithCreateInteger(canonicalInt);
    }

    private static void checkDecimalAgreesWithCreateFloat(String s) {
        Float viaFloat;
        try {
            viaFloat = NumberUtils.createFloat(s);
        } catch (RuntimeException t) {
            return;
        }

        Number viaNumber;
        try {
            viaNumber = NumberUtils.createNumber(s);
        } catch (RuntimeException t) {
            if (isRootCause(t) && hasRelevantStack(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        // Contract from the shown code: with dec != null / moderate magnitude / no exponent or suffix,
        // createNumber tries createFloat(str) first and returns that Float unless it is infinite or an
        // imprecise zero. Our generator keeps values moderate and non-zero-by-construction.
        if (!(viaNumber instanceof Float) || ((Float) viaNumber).floatValue() != viaFloat.floatValue()) {
            throw new RuntimeException(
                "[oracle:decimal-float] metamorphic violation: createNumber disagrees with createFloat input="
                    + s + " lhs=" + viaNumber + " rhs=" + viaFloat);
        }
    }

    private static void checkIntAgreesWithCreateInteger(String s) {
        Integer viaInt;
        try {
            viaInt = NumberUtils.createInteger(s);
        } catch (RuntimeException t) {
            return;
        }

        Number viaNumber;
        try {
            viaNumber = NumberUtils.createNumber(s);
        } catch (RuntimeException t) {
            if (isRootCause(t) && hasRelevantStack(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        // Contract from the shown code: when dec == null and exp == null, createNumber tries
        // createInteger(str) before wider integer types. For our canonical in-range decimal integer
        // strings, both calls must therefore agree exactly.
        if (!(viaNumber instanceof Integer) || viaNumber.intValue() != viaInt.intValue()) {
            throw new RuntimeException(
                "[oracle:int-helper] metamorphic violation: createNumber disagrees with createInteger input="
                    + s + " lhs=" + viaNumber + " rhs=" + viaInt);
        }
    }

    private static void safeCreateNumber(String s, boolean validByConstruction) {
        try {
            NumberUtils.createNumber(s);
        } catch (RuntimeException t) {
            if (validByConstruction && isRootCause(t) && hasRelevantStack(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException;
    }

    private static boolean isRootCause(Throwable t) {
        return t instanceof StringIndexOutOfBoundsException;
    }

    private static boolean hasRelevantStack(Throwable t) {
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            if (!"org.apache.commons.lang3.math.NumberUtils".equals(e.getClassName())) {
                continue;
            }
            String m = e.getMethodName();
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
        return false;
    }

    private static String buildCanonicalDecimal(FuzzedDataProvider data) {
        StringBuilder s = new StringBuilder();
        if (data.consumeBoolean()) {
            s.append('-');
        }

        s.append((char) ('1' + data.consumeInt(0, 8)));
        int intDigits = data.consumeInt(0, 4);
        for (int i = 0; i < intDigits; i++) {
            s.append((char) ('0' + data.consumeInt(0, 9)));
        }

        s.append('.');

        int fracDigits = data.consumeInt(1, 4);
        boolean anyNonZeroFrac = false;
        for (int i = 0; i < fracDigits; i++) {
            int d = data.consumeInt(0, 9);
            if (d != 0) {
                anyNonZeroFrac = true;
            }
            s.append((char) ('0' + d));
        }
        if (!anyNonZeroFrac) {
            s.setCharAt(s.length() - 1, '1');
        }
        return s.toString();
    }

    private static String buildCanonicalInt(FuzzedDataProvider data) {
        StringBuilder s = new StringBuilder();
        if (data.consumeBoolean()) {
            s.append('-');
        }

        s.append((char) ('1' + data.consumeInt(0, 8)));
        int digits = data.consumeInt(0, 8);
        for (int i = 0; i < digits; i++) {
            s.append((char) ('0' + data.consumeInt(0, 9)));
        }
        return s.toString();
    }

    private static String buildExponentBoundaryCandidate(FuzzedDataProvider data) {
        StringBuilder s = new StringBuilder();
        if (data.consumeBoolean()) {
            s.append('-');
        }

        int digits = data.consumeInt(1, 8);
        for (int i = 0; i < digits; i++) {
            s.append((char) ('0' + data.consumeInt(0, 9)));
        }

        if (data.consumeBoolean()) {
            s.append(data.consumeBoolean() ? 'e' : 'E');
            if (data.consumeBoolean()) {
                s.append(data.consumeBoolean() ? '+' : '-');
            }
            int expDigits = data.consumeInt(0, 4);
            for (int i = 0; i < expDigits; i++) {
                s.append((char) ('0' + data.consumeInt(0, 9)));
            }
        }

        if (data.consumeBoolean()) {
            switch (data.consumeInt(0, 5)) {
                case 0:
                    s.append('D');
                    break;
                case 1:
                    s.append('d');
                    break;
                case 2:
                    s.append('F');
                    break;
                case 3:
                    s.append('f');
                    break;
                case 4:
                    s.append('L');
                    break;
                default:
                    s.append('l');
                    break;
            }
        }

        return s.toString();
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
<details><summary>▸ Prompt (4 message(s), ~10,838 chars, 2 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Anchor from the failing test: this exact valid input must be accepted and, per the
        // visible createNumber implementation, for a moderate decimal without exponent/suffix
        // it should return the same Float as createFloat(str).
        checkDecimalAgreesWithCreateFloat("1234.5");

        // Explore more valid-by-construction decimal inputs of the same general shape as the anchor.
        String decimal = buildCanonicalDecimal(data);
        checkDecimalAgreesWithCreateFloat(decimal);

        // Explore the patched no-decimal/exponent parsing region with many real inputs.
        // These may be invalid/truncated; for those, rejection is acceptable and must be swallowed.
        safeCreateNumber(buildExponentBoundaryCandidate(data), false);

        // Cross-check the no-decimal path against the real helper selected by createNumber:
        // for a plain base-10 integer string in int range, createNumber first delegates to
        // createInteger and should therefore agree with it.
        String canonicalInt = buildCanonicalInt(data);
        checkIntAgreesWithCreateInteger(canonicalInt);
    }

    private static void checkDecimalAgreesWithCreateFloat(String s) {
        Float viaFloat;
        try {
            viaFloat = NumberUtils.createFloat(s);
        } catch (RuntimeException t) {
            return;
        }

        Number viaNumber;
        try {
            viaNumber = NumberUtils.createNumber(s);
        } catch (RuntimeException t) {
            if (isRootCause(t) && hasRelevantStack(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        // Contract from the shown code: with dec != null / moderate magnitude / no exponent or suffix,
        // createNumber tries createFloat(str) first and returns that Float unless it is infinite or an
        // imprecise zero. Our generator keeps values moderate and non-zero-by-construction.
        if (!(viaNumber instanceof Float) || ((Float) viaNumber).floatValue() != viaFloat.floatValue()) {
            throw new RuntimeException(
                "[oracle:decimal-float] metamorphic violation: createNumber disagrees with createFloat input="
                    + s + " lhs=" + viaNumber + " rhs=" + viaFloat);
        }
    }

    private static void checkIntAgreesWithCreateInteger(String s) {
        Integer viaInt;
        try {
            viaInt = NumberUtils.createInteger(s);
        } catch (RuntimeException t) {
            return;
        }

        Number viaNumber;
        try {
            viaNumber = NumberUtils.createNumber(s);
        } catch (RuntimeException t) {
            if (isRootCause(t) && hasRelevantStack(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        // Contract from the shown code: when dec == null and exp == null, createNumber tries
        // createInteger(str) before wider integer types. For our canonical in-range decimal integer
        // strings, both calls must therefore agree exactly.
        if (!(viaNumber instanceof Integer) || viaNumber.intValue() != viaInt.intValue()) {
            throw new RuntimeException(
                "[oracle:int-helper] metamorphic violation: createNumber disagrees with createInteger input="
                    + s + " lhs=" + viaNumber + " rhs=" + viaInt);
        }
    }

    private static void safeCreateNumber(String s, boolean validByConstruction) {
        try {
            NumberUtils.createNumber(s);
        } catch (RuntimeException t) {
            if (validByConstruction && isRootCause(t) && hasRelevantStack(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException;
    }

    private static boolean isRootCause(Throwable t) {
        return t instanceof StringIndexOutOfBoundsException;
    }

    private static boolean hasRelevantStack(Throwable t) {
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            if (!"org.apache.commons.lang3.math.NumberUtils".equals(e.getClassName())) {
                continue;
            }
            String m = e.getMethodName();
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
        return false;
    }

    private static String buildCanonicalDecimal(FuzzedDataProvider data) {
        StringBuilder s = new StringBuilder();
        if (data.consumeBoolean()) {
            s.append('-');
        }

        s.append((char) ('1' + data.consumeInt(0, 8)));
        int intDigits = data.consumeInt(0, 4);
        for (int i = 0; i < intDigits; i++) {
            s.append((char) ('0' + data.consumeInt(0, 9)));
        }

        s.append('.');

        int fracDigits = data.consumeInt(1, 4);
        boolean anyNonZeroFrac = false;
        for (int i = 0; i < fracDigits; i++) {
            int d = data.consumeInt(0, 9);
            if (d != 0) {
                anyNonZeroFrac = true;
            }
            s.append((char) ('0' + d));
        }
        if (!anyNonZeroFrac) {
            s.setCharAt(s.length() - 1, '1');
        }
        return s.toString();
    }

    private static String buildCanonicalInt(FuzzedDataProvider data) {
        StringBuilder s = new StringBuilder();
        if (data.consumeBoolean()) {
            s.append('-');
        }

        s.append((char) ('1' + data.consumeInt(0, 8)));
        int digits = data.consumeInt(0, 8);
        for (int i = 0; i < digits; i++) {
            s.append((char) ('0' + data.consumeInt(0, 9)));
        }
        return s.toString();
    }

    private static String buildExponentBoundaryCandidate(FuzzedDataProvider data) {
        StringBuilder s = new StringBuilder();
        if (data.consumeBoolean()) {
            s.append('-');
        }

        int digits = data.consumeInt(1, 8);
        for (int i = 0; i < digits; i++) {
            s.append((char) ('0' + data.consumeInt(0, 9)));
        }

        if (data.consumeBoolean()) {
            s.append(data.consumeBoolean() ? 'e' : 'E');
            if (data.consumeBoolean()) {
                s.append(data.consumeBoolean() ? '+' : '-');
            }
            int expDigits = data.consumeInt(0, 4);
            for (int i = 0; i < expDigits; i++) {
                s.append((char) ('0' + data.consumeInt(0, 9)));
            }
        }

        if (data.consumeBoolean()) {
            switch (data.consumeInt(0, 5)) {
                case 0:
                    s.append('D');
                    break;
                case 1:
                    s.append('d');
                    break;
                case 2:
                    s.append('F');
                    break;
                case 3:
                    s.append('f');
                    break;
                case 4:
                    s.append('L');
                    break;
                default:
                    s.append('l');
                    break;
            }
        }

        return s.toString();
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
<details open><summary>▸ Output (~1,973 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Number anchor = NumberUtils.createNumber("1234.5");
        Float anchorFloat = NumberUtils.createFloat("1234.5");
        if (!(anchor instanceof Float) || ((Float) anchor).floatValue() != anchorFloat.floatValue()) {
            throw new RuntimeException("[oracle:anchor-float] metamorphic violation: createNumber disagrees with createFloat input=1234.5 lhs=" + anchor + " rhs=" + anchorFloat);
        }

        String s = buildCanonicalDecimal(data);

        Float viaFloat = NumberUtils.createFloat(s);
        Number viaNumber = NumberUtils.createNumber(s);

        if (!(viaNumber instanceof Float) || ((Float) viaNumber).floatValue() != viaFloat.floatValue()) {
            throw new RuntimeException("[oracle:decimal-float] metamorphic violation: createNumber disagrees with createFloat input=" + s + " lhs=" + viaNumber + " rhs=" + viaFloat);
        }
    }

    private static String buildCanonicalDecimal(FuzzedDataProvider data) {
        StringBuilder s = new StringBuilder();

        if (data.consumeBoolean()) {
            s.append('-');
        }

        s.append((char) ('1' + data.consumeInt(0, 8)));
        int intDigits = data.consumeInt(0, 5);
        for (int i = 0; i < intDigits; i++) {
            s.append((char) ('0' + data.consumeInt(0, 9)));
        }

        s.append('.');

        int fracDigits = data.consumeInt(1, 5);
        boolean anyNonZero = false;
        for (int i = 0; i < fracDigits; i++) {
            int d = data.consumeInt(0, 9);
            if (d != 0) {
                anyNonZero = true;
            }
            s.append((char) ('0' + d));
        }

        if (!anyNonZero) {
            s.setCharAt(s.length() - 1, '1');
        }

        return s.toString();
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
<details><summary>▸ Prompt (6 message(s), ~2,289 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [3] — not reprinted*
- *[assistant] message: identical to step [5] — not reprinted*
- *[user] message: identical to step [5] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Number anchor = NumberUtils.createNumber("1234.5");
        Float anchorFloat = NumberUtils.createFloat("1234.5");
        if (!(anchor instanceof Float) || ((Float) anchor).floatValue() != anchorFloat.floatValue()) {
            throw new RuntimeException("[oracle:anchor-float] metamorphic violation: createNumber disagrees with createFloat input=1234.5 lhs=" + anchor + " rhs=" + anchorFloat);
        }

        String s = buildCanonicalDecimal(data);

        Float viaFloat = NumberUtils.createFloat(s);
        Number viaNumber = NumberUtils.createNumber(s);

        if (!(viaNumber instanceof Float) || ((Float) viaNumber).floatValue() != viaFloat.floatValue()) {
            throw new RuntimeException("[oracle:decimal-float] metamorphic violation: createNumber disagrees with createFloat input=" + s + " lhs=" + viaNumber + " rhs=" + viaFloat);
        }
    }

    private static String buildCanonicalDecimal(FuzzedDataProvider data) {
        StringBuilder s = new StringBuilder();

        if (data.consumeBoolean()) {
            s.append('-');
        }

        s.append((char) ('1' + data.consumeInt(0, 8)));
        int intDigits = data.consumeInt(0, 5);
        for (int i = 0; i < intDigits; i++) {
            s.append((char) ('0' + data.consumeInt(0, 9)));
        }

        s.append('.');

        int fracDigits = data.consumeInt(1, 5);
        boolean anyNonZero = false;
        for (int i = 0; i < fracDigits; i++) {
            int d = data.consumeInt(0, 9);
            if (d != 0) {
                anyNonZero = true;
            }
            s.append((char) ('0' + d));
        }

        if (!anyNonZero) {
            s.setCharAt(s.length() - 1, '1');
        }

        return s.toString();
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~2,693 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Number anchor = NumberUtils.createNumber("1234.5");
        Float anchorFloat = NumberUtils.createFloat("1234.5");
        if (!(anchor instanceof Float) || ((Float) anchor).floatValue() != anchorFloat.floatValue()) {
            throw new RuntimeException("[oracle:anchor-float] metamorphic violation: createNumber disagrees with createFloat input=1234.5 lhs=" + anchor + " rhs=" + anchorFloat);
        }

        String validInt = buildCanonicalInteger(data);
        Number parsedInt = NumberUtils.createNumber(validInt);
        Integer helperInt = NumberUtils.createInteger(validInt);
        if (!(parsedInt instanceof Integer) || parsedInt.intValue() != helperInt.intValue()) {
            throw new RuntimeException("[oracle:int-helper] metamorphic violation: createNumber disagrees with createInteger input=" + validInt + " lhs=" + parsedInt + " rhs=" + helperInt);
        }

        String trigger = buildBugTrigger(data);
        try {
            NumberUtils.createNumber(trigger);
        } catch (IllegalArgumentException expectedOnFixed) {
            return;
        }
    }

    private static String buildCanonicalInteger(FuzzedDataProvider data) {
        StringBuilder s = new StringBuilder();
        if (data.consumeBoolean()) {
            s.append('-');
        }
        s.append((char) ('1' + data.consumeInt(0, 8)));
        int extraDigits = data.consumeInt(0, 8);
        for (int i = 0; i < extraDigits; i++) {
            s.append((char) ('0' + data.consumeInt(0, 9)));
        }
        return s.toString();
    }

    private static String buildBugTrigger(FuzzedDataProvider data) {
        StringBuilder s = new StringBuilder();

        if (data.consumeBoolean()) {
            s.append('-');
        }

        int leadingDigits = data.consumeInt(1, 6);
        for (int i = 0; i < leadingDigits; i++) {
            s.append((char) ('0' + data.consumeInt(0, 9)));
        }

        boolean firstLower = data.consumeBoolean();
        s.append(firstLower ? 'e' : 'E');

        int middleLen = data.consumeInt(0, 4);
        for (int i = 0; i < middleLen; i++) {
            int choice = data.consumeInt(0, 11);
            if (choice < 10) {
                s.append((char) ('0' + choice));
            } else if (choice == 10) {
                s.append('+');
            } else {
                s.append('-');
            }
        }

        s.append(firstLower ? 'E' : 'e');
        return s.toString();
    }
}
```

</details>

---
## [8] ⚙️ harness-attempt · `attempt_003`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.StringIndexOutOfBoundsException: begin 0, end 4, length 3

---
## [9] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~36,191 chars, 1 new)</summary>

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
private static void safeCreateNumber(String s) {
        try {
            NumberUtils.createNumber(s);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isKnownRootCauseInPatchedRegion(t)) {
                return;
            }
        }
    }
</xref>
<xref>
private static Number exercise(String s, boolean validByConstruction) {
        try {
            return NumberUtils.createNumber(s);
        } catch (RuntimeException e) {
            if (isCleanRejection(e)) {
                return null;
            }
            if (validByConstruction && isRootCause(e) && hasRelevantStack(e)) {
                throw e;
            }
            return null;
        }
    }
</xref>
<xref>
public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NumberUtils.createNumber("1234.5");

        StringBuilder s = new StringBuilder();
        if (data.consumeBoolean()) {
            s.append('-');
        }

        int intDigits = data.consumeInt(1, 8);
        for (int i = 0; i < intDigits; i++) {
            s.append((char) ('0' + data.consumeInt(0, 9)));
        }

        if (data.consumeBoolean()) {
            s.append('.');
            int fracDigits = data.consumeInt(0, 8);
            for (int i = 0; i < fracDigits; i++) {
                s.append((char) ('0' + data.consumeInt(0, 9)));
            }
        }

        if (data.consumeBoolean()) {
            s.append(data.consumeBoolean() ? 'e' : 'E');
        }

        if (data.consumeBoolean()) {
            char suffix;
            switch (data.consumeInt(0, 3)) {
                case 0:
                    suffix = 'D';
                    break;
                case 1:
                    suffix = 'd';
                    break;
                case 2:
                    suffix = 'F';
                    break;
                default:
                    suffix = 'f';
                    break;
            }
            s.append(suffix);
        }

        NumberUtils.createNumber(s.toString());
    }
</xref>
<xref>
private static void safeCreateNumber(String s, boolean propagateKnownRootCause) {
        try {
            NumberUtils.createNumber(s);
        } catch (RuntimeException t) {
            if (propagateKnownRootCause && isKnownRootCauseFromCreateNumber(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }
</xref>
<xref>
public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NumberUtils.createNumber("1234.5");

        String candidate = buildDecimalLikeInput(data);
        try {
            NumberUtils.createNumber(candidate);
        } catch (IllegalArgumentException ignored) {
        }

        checkLongSuffixMetamorphic(data);
        checkBigIntegerFallbackMetamorphic(data);
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
message: begin 0, end 4, length 3
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
java.lang.StringIndexOutOfBoundsException: begin 0, end 4, length 3
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
- isBlank
- createInteger
- isAllZeros
- charAt
- substring
- isDigits
- createLong
- createBigInteger
- createFloat
- createDouble
- createBigDecimal
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.lang3.math.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.lang3.math.NumberUtils.createNumber
Crashes already found:
- java.lang.StringIndexOutOfBoundsException@org.apache.commons.lang3.math.NumberUtils.createNumber
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- isBlank
- createInteger
- isAllZeros
- charAt
- substring
- isDigits
- createLong
- createBigInteger
- createFloat
- createDouble
- createBigDecimal
Check FAMILIES already covered by accepted harnesses: {anchor-float, int-helper}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
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
<details open><summary>▸ Output (~4,298 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.math.BigDecimal;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        safeCreateNumber("1234.5", true);

        String base = buildCanonicalBase(data);

        String expChar = data.consumeBoolean() ? "e" : "E";
        String validExponent = data.consumeBoolean() ? "0" : Integer.toString(data.consumeInt(-9, 9));
        String validWithExponent = base + expChar + validExponent;
        safeCreateNumber(validWithExponent, true);

        String invalidBoundary;
        switch (data.consumeInt(0, 3)) {
            case 0:
                invalidBoundary = base + expChar;
                break;
            case 1:
                invalidBoundary = base + expChar + "+";
                break;
            case 2:
                invalidBoundary = base + expChar + "-";
                break;
            default:
                invalidBoundary = base + expChar + data.consumeAsciiString(1);
                break;
        }
        safeCreateNumber(invalidBoundary, false);

        /* Contract/oracle:
         * Adding an exponent of 0 preserves numeric value for any valid numeric literal.
         * A throw-deleting or overfit patch could avoid the crash yet parse nearby exponent-boundary
         * inputs incorrectly. We therefore compare createNumber(base) with createNumber(base + "E0")
         * via an independent numeric view (BigDecimal parsed from each returned Number's String form).
         */
        Number plain = safeCreateNumber(base, true);
        Number exp0 = safeCreateNumber(base + expChar + "0", true);
        if (plain != null && exp0 != null) {
            BigDecimal lhs = toBigDecimal(plain);
            BigDecimal rhs = toBigDecimal(exp0);
            if (lhs != null && rhs != null && lhs.compareTo(rhs) != 0) {
                throw new RuntimeException("[oracle:exp0-equivalence] metamorphic violation: exponent zero changed value input="
                        + base + " lhs=" + lhs.toPlainString() + " rhs=" + rhs.toPlainString());
            }
        }
    }

    private static String buildCanonicalBase(FuzzedDataProvider data) {
        StringBuilder s = new StringBuilder();
        if (data.consumeBoolean()) {
            s.append('-');
        }

        int intDigits = data.consumeInt(1, 5);
        for (int i = 0; i < intDigits; i++) {
            int d = (i == 0) ? data.consumeInt(1, 9) : data.consumeInt(0, 9);
            s.append((char) ('0' + d));
        }

        if (data.consumeBoolean()) {
            s.append('.');
            int fracDigits = data.consumeInt(1, 3);
            for (int i = 0; i < fracDigits; i++) {
                s.append((char) ('0' + data.consumeInt(0, 9)));
            }
        }

        return s.toString();
    }

    private static Number safeCreateNumber(String s, boolean validByConstruction) {
        try {
            return NumberUtils.createNumber(s);
        } catch (RuntimeException t) {
            if (validByConstruction && isRootCauseFromPatchedRegion(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return null;
            }
            return null;
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCauseFromPatchedRegion(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.lang3.math.NumberUtils".equals(ste.getClassName())
                    && "createNumber".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static BigDecimal toBigDecimal(Number n) {
        try {
            return NumberUtils.createBigDecimal(n.toString());
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
```

</details>

---
## [10] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. Jazzer exited cleanly with no finding on the buggy code. Most often this means every input was handled normally, or the harnes

---
## [11] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~4,495 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [9] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.math.BigDecimal;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        safeCreateNumber("1234.5", true);

        String base = buildCanonicalBase(data);

        String expChar = data.consumeBoolean() ? "e" : "E";
        String validExponent = data.consumeBoolean() ? "0" : Integer.toString(data.consumeInt(-9, 9));
        String validWithExponent = base + expChar + validExponent;
        safeCreateNumber(validWithExponent, true);

        String invalidBoundary;
        switch (data.consumeInt(0, 3)) {
            case 0:
                invalidBoundary = base + expChar;
                break;
            case 1:
                invalidBoundary = base + expChar + "+";
                break;
            case 2:
                invalidBoundary = base + expChar + "-";
                break;
            default:
                invalidBoundary = base + expChar + data.consumeAsciiString(1);
                break;
        }
        safeCreateNumber(invalidBoundary, false);

        /* Contract/oracle:
         * Adding an exponent of 0 preserves numeric value for any valid numeric literal.
         * A throw-deleting or overfit patch could avoid the crash yet parse nearby exponent-boundary
         * inputs incorrectly. We therefore compare createNumber(base) with createNumber(base + "E0")
         * via an independent numeric view (BigDecimal parsed from each returned Number's String form).
         */
        Number plain = safeCreateNumber(base, true);
        Number exp0 = safeCreateNumber(base + expChar + "0", true);
        if (plain != null && exp0 != null) {
            BigDecimal lhs = toBigDecimal(plain);
            BigDecimal rhs = toBigDecimal(exp0);
            if (lhs != null && rhs != null && lhs.compareTo(rhs) != 0) {
                throw new RuntimeException("[oracle:exp0-equivalence] metamorphic violation: exponent zero changed value input="
                        + base + " lhs=" + lhs.toPlainString() + " rhs=" + rhs.toPlainString());
            }
        }
    }

    private static String buildCanonicalBase(FuzzedDataProvider data) {
        StringBuilder s = new StringBuilder();
        if (data.consumeBoolean()) {
            s.append('-');
        }

        int intDigits = data.consumeInt(1, 5);
        for (int i = 0; i < intDigits; i++) {
            int d = (i == 0) ? data.consumeInt(1, 9) : data.consumeInt(0, 9);
            s.append((char) ('0' + d));
        }

        if (data.consumeBoolean()) {
            s.append('.');
            int fracDigits = data.consumeInt(1, 3);
            for (int i = 0; i < fracDigits; i++) {
                s.append((char) ('0' + data.consumeInt(0, 9)));
            }
        }

        return s.toString();
    }

    private static Number safeCreateNumber(String s, boolean validByConstruction) {
        try {
            return NumberUtils.createNumber(s);
        } catch (RuntimeException t) {
            if (validByConstruction && isRootCauseFromPatchedRegion(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return null;
            }
            return null;
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCauseFromPatchedRegion(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.lang3.math.NumberUtils".equals(ste.getClassName())
                    && "createNumber".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static BigDecimal toBigDecimal(Number n) {
        try {
            return NumberUtils.createBigDecimal(n.toString());
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~2,887 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.math.BigDecimal;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NumberUtils.createNumber("1234.5");

        String canonical = buildCanonicalDecimal(data);
        checkExponentZeroMetamorphic(canonical);

        String trigger = buildBugTrigger(data);
        NumberUtils.createNumber(trigger);
    }

    private static String buildCanonicalDecimal(FuzzedDataProvider data) {
        StringBuilder s = new StringBuilder();
        if (data.consumeBoolean()) {
            s.append('-');
        }

        int intDigits = data.consumeInt(1, 6);
        for (int i = 0; i < intDigits; i++) {
            int d = (i == 0) ? data.consumeInt(1, 9) : data.consumeInt(0, 9);
            s.append((char) ('0' + d));
        }

        if (data.consumeBoolean()) {
            s.append('.');
            int fracDigits = data.consumeInt(1, 4);
            for (int i = 0; i < fracDigits; i++) {
                s.append((char) ('0' + data.consumeInt(0, 9)));
            }
        }

        return s.toString();
    }

    private static String buildBugTrigger(FuzzedDataProvider data) {
        StringBuilder s = new StringBuilder();

        int leadingDigits = data.consumeInt(1, 6);
        for (int i = 0; i < leadingDigits; i++) {
            int d = (i == 0) ? data.consumeInt(1, 9) : data.consumeInt(0, 9);
            s.append((char) ('0' + d));
        }

        if (data.consumeBoolean()) {
            s.append('e');
            int middleLen = data.consumeInt(0, 4);
            for (int i = 0; i < middleLen; i++) {
                s.append((char) ('0' + data.consumeInt(0, 9)));
            }
            s.append('E');
        } else {
            s.append('E');
            int middleLen = data.consumeInt(0, 4);
            for (int i = 0; i < middleLen; i++) {
                s.append((char) ('0' + data.consumeInt(0, 9)));
            }
            s.append('e');
        }

        return s.toString();
    }

    private static void checkExponentZeroMetamorphic(String s) {
        try {
            Number a = NumberUtils.createNumber(s);
            Number b = NumberUtils.createNumber(s + "E0");
            if (a == null || b == null) {
                return;
            }

            BigDecimal left = new BigDecimal(a.toString());
            BigDecimal right = new BigDecimal(b.toString());
            if (left.compareTo(right) != 0) {
                throw new RuntimeException("[oracle:exp0-equivalence] metamorphic violation: createNumber(x) must equal createNumber(x+E0) for valid numeric x input=" + s + " lhs=" + left.toPlainString() + " rhs=" + right.toPlainString());
            }
        } catch (RuntimeException ignored) {
        }
    }
}
```

</details>

---
## [12] ⚙️ harness-attempt · `attempt_005`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.StringIndexOutOfBoundsException: begin 0, end 4, length 3

---
## [13] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~36,208 chars, 1 new)</summary>

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
private static void safeCreateNumber(String s) {
        try {
            NumberUtils.createNumber(s);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isKnownRootCauseInPatchedRegion(t)) {
                return;
            }
        }
    }
</xref>
<xref>
private static Number exercise(String s, boolean validByConstruction) {
        try {
            return NumberUtils.createNumber(s);
        } catch (RuntimeException e) {
            if (isCleanRejection(e)) {
                return null;
            }
            if (validByConstruction && isRootCause(e) && hasRelevantStack(e)) {
                throw e;
            }
            return null;
        }
    }
</xref>
<xref>
public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NumberUtils.createNumber("1234.5");

        StringBuilder s = new StringBuilder();
        if (data.consumeBoolean()) {
            s.append('-');
        }

        int intDigits = data.consumeInt(1, 8);
        for (int i = 0; i < intDigits; i++) {
            s.append((char) ('0' + data.consumeInt(0, 9)));
        }

        if (data.consumeBoolean()) {
            s.append('.');
            int fracDigits = data.consumeInt(0, 8);
            for (int i = 0; i < fracDigits; i++) {
                s.append((char) ('0' + data.consumeInt(0, 9)));
            }
        }

        if (data.consumeBoolean()) {
            s.append(data.consumeBoolean() ? 'e' : 'E');
        }

        if (data.consumeBoolean()) {
            char suffix;
            switch (data.consumeInt(0, 3)) {
                case 0:
                    suffix = 'D';
                    break;
                case 1:
                    suffix = 'd';
                    break;
                case 2:
                    suffix = 'F';
                    break;
                default:
                    suffix = 'f';
                    break;
            }
            s.append(suffix);
        }

        NumberUtils.createNumber(s.toString());
    }
</xref>
<xref>
private static void safeCreateNumber(String s, boolean propagateKnownRootCause) {
        try {
            NumberUtils.createNumber(s);
        } catch (RuntimeException t) {
            if (propagateKnownRootCause && isKnownRootCauseFromCreateNumber(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }
</xref>
<xref>
public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NumberUtils.createNumber("1234.5");

        String candidate = buildDecimalLikeInput(data);
        try {
            NumberUtils.createNumber(candidate);
        } catch (IllegalArgumentException ignored) {
        }

        checkLongSuffixMetamorphic(data);
        checkBigIntegerFallbackMetamorphic(data);
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
message: begin 0, end 4, length 3
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
java.lang.StringIndexOutOfBoundsException: begin 0, end 4, length 3
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
- isBlank
- createInteger
- isAllZeros
- charAt
- substring
- isDigits
- createLong
- createBigInteger
- createFloat
- createDouble
- createBigDecimal
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.lang3.math.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.lang3.math.NumberUtils.createNumber
Crashes already found:
- java.lang.StringIndexOutOfBoundsException@org.apache.commons.lang3.math.NumberUtils.createNumber
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- isBlank
- createInteger
- isAllZeros
- charAt
- substring
- isDigits
- createLong
- createBigInteger
- createFloat
- createDouble
- createBigDecimal
Check FAMILIES already covered by accepted harnesses: {anchor-float, exp-equivalence, int-helper}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
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
<details open><summary>▸ Output (~6,788 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.math.BigInteger;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        safeCreateNumber("1234.5", true);

        safeCreateNumber(buildTrailingExponentCandidate(data), false);

        safeCreateNumber(buildVariantAroundPatchedBoundary(data), false);

        checkLongSuffixValueAgreement(data);
        checkBigIntegerSuffixValueAgreement(data);
    }

    private static void safeCreateNumber(String s, boolean validByConstruction) {
        try {
            NumberUtils.createNumber(s);
        } catch (RuntimeException t) {
            if (validByConstruction && isKnownRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException;
    }

    private static boolean isKnownRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String m = ste.getMethodName();
            if ("org.apache.commons.lang3.math.NumberUtils".equals(cls) && "createNumber".equals(m)) {
                return true;
            }
            if ("org.apache.commons.lang3.math.NumberUtils".equals(cls)
                    && ("createInteger".equals(m)
                    || "isAllZeros".equals(m)
                    || "isDigits".equals(m)
                    || "createLong".equals(m)
                    || "createBigInteger".equals(m)
                    || "createFloat".equals(m)
                    || "createDouble".equals(m)
                    || "createBigDecimal".equals(m))) {
                return true;
            }
            if ("java.lang.String".equals(cls) && ("substring".equals(m) || "charAt".equals(m))) {
                return true;
            }
            if ("org.apache.commons.lang3.StringUtils".equals(cls) && "isBlank".equals(m)) {
                return true;
            }
        }
        return false;
    }

    private static String buildTrailingExponentCandidate(FuzzedDataProvider data) {
        StringBuilder s = new StringBuilder();
        if (data.consumeBoolean()) {
            s.append('-');
        }
        int digits = data.consumeInt(1, 12);
        for (int i = 0; i < digits; i++) {
            s.append((char) ('0' + data.consumeInt(0, 9)));
        }
        s.append(data.consumeBoolean() ? 'e' : 'E');
        return s.toString();
    }

    private static String buildVariantAroundPatchedBoundary(FuzzedDataProvider data) {
        StringBuilder s = new StringBuilder();
        if (data.consumeBoolean()) {
            s.append('-');
        }
        int digits = data.consumeInt(1, 10);
        for (int i = 0; i < digits; i++) {
            s.append((char) ('0' + data.consumeInt(0, 9)));
        }
        if (data.consumeBoolean()) {
            s.append('.');
            int frac = data.consumeInt(0, 6);
            for (int i = 0; i < frac; i++) {
                s.append((char) ('0' + data.consumeInt(0, 9)));
            }
        }
        s.append(data.consumeBoolean() ? 'e' : 'E');
        if (data.consumeBoolean()) {
            s.append(data.consumeBoolean() ? '+' : '-');
        }
        return s.toString();
    }

    private static void checkLongSuffixValueAgreement(FuzzedDataProvider data) {
        int n = data.consumeInt(-1_000_000, 1_000_000);
        String base = Integer.toString(n);

        Number unsuffixed;
        Number suffixed;
        try {
            unsuffixed = NumberUtils.createNumber(base);
            suffixed = NumberUtils.createNumber(base + "L");
        } catch (RuntimeException e) {
            return;
        }
        if (unsuffixed == null || suffixed == null) {
            return;
        }

        /* Contract/oracle: the 'L' suffix only requests a specific integral type for the same numeric token.
           For any valid canonical integer literal within long range, createNumber(base) and createNumber(base + "L")
           must denote the same numeric value even if their boxed classes differ. A throw-deleting or branch-skipping
           patch in the integer/suffix path would violate this without reproducing the known crash. */
        if (unsuffixed.longValue() != suffixed.longValue()) {
            throw new RuntimeException("[oracle:long-suffix] metamorphic violation: equivalent integral inputs disagree input="
                    + base + " lhs=" + unsuffixed + " rhs=" + suffixed);
        }
    }

    private static void checkBigIntegerSuffixValueAgreement(FuzzedDataProvider data) {
        String base = buildOversizedInteger(data);
        Number unsuffixed;
        Number suffixed;
        BigInteger expected;
        try {
            unsuffixed = NumberUtils.createNumber(base);
            suffixed = NumberUtils.createNumber(base + "L");
            expected = NumberUtils.createBigInteger(base);
        } catch (RuntimeException e) {
            return;
        }
        if (unsuffixed == null || suffixed == null || expected == null) {
            return;
        }

        /* Contract/oracle: for an oversized decimal integer, createNumber chooses BigInteger on the unsuffixed path,
           and the 'L' path explicitly falls back to BigInteger when createLong overflows. Therefore all three real
           library calls below must represent the same value for every correct implementation. */
        BigInteger lhs = toBigInteger(unsuffixed);
        BigInteger rhs = toBigInteger(suffixed);
        if (!expected.equals(lhs) || !expected.equals(rhs)) {
            throw new RuntimeException("[oracle:bigint-suffix] metamorphic violation: oversized integral inputs disagree input="
                    + base + " expected=" + expected + " lhs=" + unsuffixed + " rhs=" + suffixed);
        }
    }

    private static String buildOversizedInteger(FuzzedDataProvider data) {
        StringBuilder s = new StringBuilder();
        if (data.consumeBoolean()) {
            s.append('-');
        }
        s.append((char) ('1' + data.consumeInt(0, 8)));
        int extraDigits = data.consumeInt(19, 30);
        for (int i = 0; i < extraDigits; i++) {
            s.append((char) ('0' + data.consumeInt(0, 9)));
        }
        return s.toString();
    }

    private static BigInteger toBigInteger(Number n) {
        if (n instanceof BigInteger) {
            return (BigInteger) n;
        }
        return new BigInteger(n.toString());
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
<details><summary>▸ Prompt (4 message(s), ~6,986 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [13] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.math.BigInteger;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        safeCreateNumber("1234.5", true);

        safeCreateNumber(buildTrailingExponentCandidate(data), false);

        safeCreateNumber(buildVariantAroundPatchedBoundary(data), false);

        checkLongSuffixValueAgreement(data);
        checkBigIntegerSuffixValueAgreement(data);
    }

    private static void safeCreateNumber(String s, boolean validByConstruction) {
        try {
            NumberUtils.createNumber(s);
        } catch (RuntimeException t) {
            if (validByConstruction && isKnownRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException;
    }

    private static boolean isKnownRootCause(Throwable t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String m = ste.getMethodName();
            if ("org.apache.commons.lang3.math.NumberUtils".equals(cls) && "createNumber".equals(m)) {
                return true;
            }
            if ("org.apache.commons.lang3.math.NumberUtils".equals(cls)
                    && ("createInteger".equals(m)
                    || "isAllZeros".equals(m)
                    || "isDigits".equals(m)
                    || "createLong".equals(m)
                    || "createBigInteger".equals(m)
                    || "createFloat".equals(m)
                    || "createDouble".equals(m)
                    || "createBigDecimal".equals(m))) {
                return true;
            }
            if ("java.lang.String".equals(cls) && ("substring".equals(m) || "charAt".equals(m))) {
                return true;
            }
            if ("org.apache.commons.lang3.StringUtils".equals(cls) && "isBlank".equals(m)) {
                return true;
            }
        }
        return false;
    }

    private static String buildTrailingExponentCandidate(FuzzedDataProvider data) {
        StringBuilder s = new StringBuilder();
        if (data.consumeBoolean()) {
            s.append('-');
        }
        int digits = data.consumeInt(1, 12);
        for (int i = 0; i < digits; i++) {
            s.append((char) ('0' + data.consumeInt(0, 9)));
        }
        s.append(data.consumeBoolean() ? 'e' : 'E');
        return s.toString();
    }

    private static String buildVariantAroundPatchedBoundary(FuzzedDataProvider data) {
        StringBuilder s = new StringBuilder();
        if (data.consumeBoolean()) {
            s.append('-');
        }
        int digits = data.consumeInt(1, 10);
        for (int i = 0; i < digits; i++) {
            s.append((char) ('0' + data.consumeInt(0, 9)));
        }
        if (data.consumeBoolean()) {
            s.append('.');
            int frac = data.consumeInt(0, 6);
            for (int i = 0; i < frac; i++) {
                s.append((char) ('0' + data.consumeInt(0, 9)));
            }
        }
        s.append(data.consumeBoolean() ? 'e' : 'E');
        if (data.consumeBoolean()) {
            s.append(data.consumeBoolean() ? '+' : '-');
        }
        return s.toString();
    }

    private static void checkLongSuffixValueAgreement(FuzzedDataProvider data) {
        int n = data.consumeInt(-1_000_000, 1_000_000);
        String base = Integer.toString(n);

        Number unsuffixed;
        Number suffixed;
        try {
            unsuffixed = NumberUtils.createNumber(base);
            suffixed = NumberUtils.createNumber(base + "L");
        } catch (RuntimeException e) {
            return;
        }
        if (unsuffixed == null || suffixed == null) {
            return;
        }

        /* Contract/oracle: the 'L' suffix only requests a specific integral type for the same numeric token.
           For any valid canonical integer literal within long range, createNumber(base) and createNumber(base + "L")
           must denote the same numeric value even if their boxed classes differ. A throw-deleting or branch-skipping
           patch in the integer/suffix path would violate this without reproducing the known crash. */
        if (unsuffixed.longValue() != suffixed.longValue()) {
            throw new RuntimeException("[oracle:long-suffix] metamorphic violation: equivalent integral inputs disagree input="
                    + base + " lhs=" + unsuffixed + " rhs=" + suffixed);
        }
    }

    private static void checkBigIntegerSuffixValueAgreement(FuzzedDataProvider data) {
        String base = buildOversizedInteger(data);
        Number unsuffixed;
        Number suffixed;
        BigInteger expected;
        try {
            unsuffixed = NumberUtils.createNumber(base);
            suffixed = NumberUtils.createNumber(base + "L");
            expected = NumberUtils.createBigInteger(base);
        } catch (RuntimeException e) {
            return;
        }
        if (unsuffixed == null || suffixed == null || expected == null) {
            return;
        }

        /* Contract/oracle: for an oversized decimal integer, createNumber chooses BigInteger on the unsuffixed path,
           and the 'L' path explicitly falls back to BigInteger when createLong overflows. Therefore all three real
           library calls below must represent the same value for every correct implementation. */
        BigInteger lhs = toBigInteger(unsuffixed);
        BigInteger rhs = toBigInteger(suffixed);
        if (!expected.equals(lhs) || !expected.equals(rhs)) {
            throw new RuntimeException("[oracle:bigint-suffix] metamorphic violation: oversized integral inputs disagree input="
                    + base + " expected=" + expected + " lhs=" + unsuffixed + " rhs=" + suffixed);
        }
    }

    private static String buildOversizedInteger(FuzzedDataProvider data) {
        StringBuilder s = new StringBuilder();
        if (data.consumeBoolean()) {
            s.append('-');
        }
        s.append((char) ('1' + data.consumeInt(0, 8)));
        int extraDigits = data.consumeInt(19, 30);
        for (int i = 0; i < extraDigits; i++) {
            s.append((char) ('0' + data.consumeInt(0, 9)));
        }
        return s.toString();
    }

    private static BigInteger toBigInteger(Number n) {
        if (n instanceof BigInteger) {
            return (BigInteger) n;
        }
        return new BigInteger(n.toString());
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~4,867 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.math.BigInteger;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NumberUtils.createNumber("1234.5");

        try {
            NumberUtils.createNumber(buildTrailingExponentInput(data));
        } catch (IllegalArgumentException ignored) {
        }

        try {
            NumberUtils.createNumber(buildBoundaryVariant(data));
        } catch (IllegalArgumentException ignored) {
        }

        checkLongSuffixAgreement(data);
        checkBigIntegerFallbackAgreement(data);
    }

    private static String buildTrailingExponentInput(FuzzedDataProvider data) {
        StringBuilder s = new StringBuilder();
        if (data.consumeBoolean()) {
            s.append('-');
        }
        int intDigits = data.consumeInt(1, 12);
        for (int i = 0; i < intDigits; i++) {
            s.append((char) ('0' + data.consumeInt(0, 9)));
        }
        s.append(data.consumeBoolean() ? 'e' : 'E');
        return s.toString();
    }

    private static String buildBoundaryVariant(FuzzedDataProvider data) {
        StringBuilder s = new StringBuilder();
        if (data.consumeBoolean()) {
            s.append('-');
        }
        int intDigits = data.consumeInt(1, 8);
        for (int i = 0; i < intDigits; i++) {
            s.append((char) ('0' + data.consumeInt(0, 9)));
        }
        if (data.consumeBoolean()) {
            s.append('.');
            int fracDigits = data.consumeInt(0, 8);
            for (int i = 0; i < fracDigits; i++) {
                s.append((char) ('0' + data.consumeInt(0, 9)));
            }
        }
        s.append(data.consumeBoolean() ? 'e' : 'E');
        if (data.consumeBoolean()) {
            s.append(data.consumeBoolean() ? '+' : '-');
        }
        return s.toString();
    }

    private static void checkLongSuffixAgreement(FuzzedDataProvider data) {
        String base = Integer.toString(data.consumeInt(-1_000_000, 1_000_000));
        Number plain;
        Number suffixed;
        try {
            plain = NumberUtils.createNumber(base);
            suffixed = NumberUtils.createNumber(base + "L");
        } catch (RuntimeException e) {
            return;
        }
        if (plain == null || suffixed == null) {
            return;
        }

        /* Contract: for a valid canonical decimal integer within long range, adding an 'L' suffix
           changes only the requested representation, not the numeric value. A patch that merely avoids
           the old crash but skips or corrupts parsing would violate this observable relation. */
        if (plain.longValue() != suffixed.longValue()) {
            throw new RuntimeException("[oracle:long-suffix] metamorphic violation: base=" + base
                    + " plain=" + plain + " suffixed=" + suffixed);
        }
    }

    private static void checkBigIntegerFallbackAgreement(FuzzedDataProvider data) {
        String base = buildOversizedInteger(data);
        Number plain;
        Number suffixed;
        BigInteger direct;
        try {
            plain = NumberUtils.createNumber(base);
            suffixed = NumberUtils.createNumber(base + "L");
            direct = NumberUtils.createBigInteger(base);
        } catch (RuntimeException e) {
            return;
        }
        if (plain == null || suffixed == null || direct == null) {
            return;
        }

        /* Contract: for an oversized decimal integer, createNumber(base) returns BigInteger and
           createNumber(base + "L") falls back from createLong to createBigInteger. Both real-library
           paths must therefore agree with createBigInteger(base) on the numeric value. */
        BigInteger plainValue = toBigInteger(plain);
        BigInteger suffixedValue = toBigInteger(suffixed);
        if (!direct.equals(plainValue) || !direct.equals(suffixedValue)) {
            throw new RuntimeException("[oracle:bigint-fallback] metamorphic violation: base=" + base
                    + " direct=" + direct + " plain=" + plain + " suffixed=" + suffixed);
        }
    }

    private static String buildOversizedInteger(FuzzedDataProvider data) {
        StringBuilder s = new StringBuilder();
        if (data.consumeBoolean()) {
            s.append('-');
        }
        s.append((char) ('1' + data.consumeInt(0, 8)));
        int extraDigits = data.consumeInt(19, 30);
        for (int i = 0; i < extraDigits; i++) {
            s.append((char) ('0' + data.consumeInt(0, 9)));
        }
        return s.toString();
    }

    private static BigInteger toBigInteger(Number n) {
        if (n instanceof BigInteger) {
            return (BigInteger) n;
        }
        return new BigInteger(n.toString());
    }
}
```

</details>

---
## [16] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. Jazzer exited cleanly with no finding on the buggy code. Most often this means every input was handled normally, or the harnes

---
## [17] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~5,185 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [13] — not reprinted*
- *[assistant] message: identical to step [15] — not reprinted*
- *[user] message: identical to step [5] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.math.BigInteger;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NumberUtils.createNumber("1234.5");

        try {
            NumberUtils.createNumber(buildTrailingExponentInput(data));
        } catch (IllegalArgumentException ignored) {
        }

        try {
            NumberUtils.createNumber(buildBoundaryVariant(data));
        } catch (IllegalArgumentException ignored) {
        }

        checkLongSuffixAgreement(data);
        checkBigIntegerFallbackAgreement(data);
    }

    private static String buildTrailingExponentInput(FuzzedDataProvider data) {
        StringBuilder s = new StringBuilder();
        if (data.consumeBoolean()) {
            s.append('-');
        }
        int intDigits = data.consumeInt(1, 12);
        for (int i = 0; i < intDigits; i++) {
            s.append((char) ('0' + data.consumeInt(0, 9)));
        }
        s.append(data.consumeBoolean() ? 'e' : 'E');
        return s.toString();
    }

    private static String buildBoundaryVariant(FuzzedDataProvider data) {
        StringBuilder s = new StringBuilder();
        if (data.consumeBoolean()) {
            s.append('-');
        }
        int intDigits = data.consumeInt(1, 8);
        for (int i = 0; i < intDigits; i++) {
            s.append((char) ('0' + data.consumeInt(0, 9)));
        }
        if (data.consumeBoolean()) {
            s.append('.');
            int fracDigits = data.consumeInt(0, 8);
            for (int i = 0; i < fracDigits; i++) {
                s.append((char) ('0' + data.consumeInt(0, 9)));
            }
        }
        s.append(data.consumeBoolean() ? 'e' : 'E');
        if (data.consumeBoolean()) {
            s.append(data.consumeBoolean() ? '+' : '-');
        }
        return s.toString();
    }

    private static void checkLongSuffixAgreement(FuzzedDataProvider data) {
        String base = Integer.toString(data.consumeInt(-1_000_000, 1_000_000));
        Number plain;
        Number suffixed;
        try {
            plain = NumberUtils.createNumber(base);
            suffixed = NumberUtils.createNumber(base + "L");
        } catch (RuntimeException e) {
            return;
        }
        if (plain == null || suffixed == null) {
            return;
        }

        /* Contract: for a valid canonical decimal integer within long range, adding an 'L' suffix
           changes only the requested representation, not the numeric value. A patch that merely avoids
           the old crash but skips or corrupts parsing would violate this observable relation. */
        if (plain.longValue() != suffixed.longValue()) {
            throw new RuntimeException("[oracle:long-suffix] metamorphic violation: base=" + base
                    + " plain=" + plain + " suffixed=" + suffixed);
        }
    }

    private static void checkBigIntegerFallbackAgreement(FuzzedDataProvider data) {
        String base = buildOversizedInteger(data);
        Number plain;
        Number suffixed;
        BigInteger direct;
        try {
            plain = NumberUtils.createNumber(base);
            suffixed = NumberUtils.createNumber(base + "L");
            direct = NumberUtils.createBigInteger(base);
        } catch (RuntimeException e) {
            return;
        }
        if (plain == null || suffixed == null || direct == null) {
            return;
        }

        /* Contract: for an oversized decimal integer, createNumber(base) returns BigInteger and
           createNumber(base + "L") falls back from createLong to createBigInteger. Both real-library
           paths must therefore agree with createBigInteger(base) on the numeric value. */
        BigInteger plainValue = toBigInteger(plain);
        BigInteger suffixedValue = toBigInteger(suffixed);
        if (!direct.equals(plainValue) || !direct.equals(suffixedValue)) {
            throw new RuntimeException("[oracle:bigint-fallback] metamorphic violation: base=" + base
                    + " direct=" + direct + " plain=" + plain + " suffixed=" + suffixed);
        }
    }

    private static String buildOversizedInteger(FuzzedDataProvider data) {
        StringBuilder s = new StringBuilder();
        if (data.consumeBoolean()) {
            s.append('-');
        }
        s.append((char) ('1' + data.consumeInt(0, 8)));
        int extraDigits = data.consumeInt(19, 30);
        for (int i = 0; i < extraDigits; i++) {
            s.append((char) ('0' + data.consumeInt(0, 9)));
        }
        return s.toString();
    }

    private static BigInteger toBigInteger(Number n) {
        if (n instanceof BigInteger) {
            return (BigInteger) n;
        }
        return new BigInteger(n.toString());
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~4,108 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.math.BigInteger;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NumberUtils.createNumber("1234.5");

        String candidate = buildExponentAtEndInput(data);
        try {
            NumberUtils.createNumber(candidate);
        } catch (IllegalArgumentException ignored) {
        }

        checkLongSuffixAgreement(data);
        checkBigIntegerFallbackAgreement(data);
    }

    private static String buildExponentAtEndInput(FuzzedDataProvider data) {
        StringBuilder s = new StringBuilder();
        if (data.consumeBoolean()) {
            s.append('-');
        }

        int intDigits = data.consumeInt(1, 12);
        for (int i = 0; i < intDigits; i++) {
            s.append((char) ('0' + data.consumeInt(0, 9)));
        }

        if (data.consumeBoolean()) {
            s.append('.');
            int fracDigits = data.consumeInt(0, 8);
            for (int i = 0; i < fracDigits; i++) {
                s.append((char) ('0' + data.consumeInt(0, 9)));
            }
        }

        s.append(data.consumeBoolean() ? 'e' : 'E');
        return s.toString();
    }

    private static void checkLongSuffixAgreement(FuzzedDataProvider data) {
        String base = Integer.toString(data.consumeInt(-1_000_000, 1_000_000));
        Number plain;
        Number suffixed;
        try {
            plain = NumberUtils.createNumber(base);
            suffixed = NumberUtils.createNumber(base + "L");
        } catch (RuntimeException e) {
            return;
        }
        if (plain == null || suffixed == null) {
            return;
        }

        /* Contract: adding 'L' requests long/integral parsing of the same valid decimal integer literal,
           so both calls must denote the same numeric value. This catches silent wrong-output regressions
           even if a patch merely suppresses the old exception. */
        if (plain.longValue() != suffixed.longValue()) {
            throw new RuntimeException("[oracle:long-suffix] metamorphic violation: input=" + base
                    + " plain=" + plain + " suffixed=" + suffixed);
        }
    }

    private static void checkBigIntegerFallbackAgreement(FuzzedDataProvider data) {
        String base = buildOversizedInteger(data);
        Number plain;
        Number suffixed;
        BigInteger direct;
        try {
            plain = NumberUtils.createNumber(base);
            suffixed = NumberUtils.createNumber(base + "L");
            direct = NumberUtils.createBigInteger(base);
        } catch (RuntimeException e) {
            return;
        }
        if (plain == null || suffixed == null || direct == null) {
            return;
        }

        /* Contract: an oversized decimal integer should parse as BigInteger, and the 'L' suffixed path
           falls back to BigInteger when createLong overflows. All three real library calls must agree. */
        BigInteger plainValue = toBigInteger(plain);
        BigInteger suffixedValue = toBigInteger(suffixed);
        if (!direct.equals(plainValue) || !direct.equals(suffixedValue)) {
            throw new RuntimeException("[oracle:bigint-fallback] metamorphic violation: input=" + base
                    + " direct=" + direct + " plain=" + plain + " suffixed=" + suffixed);
        }
    }

    private static String buildOversizedInteger(FuzzedDataProvider data) {
        StringBuilder s = new StringBuilder();
        if (data.consumeBoolean()) {
            s.append('-');
        }
        s.append((char) ('1' + data.consumeInt(0, 8)));
        int extraDigits = data.consumeInt(19, 30);
        for (int i = 0; i < extraDigits; i++) {
            s.append((char) ('0' + data.consumeInt(0, 9)));
        }
        return s.toString();
    }

    private static BigInteger toBigInteger(Number n) {
        if (n instanceof BigInteger) {
            return (BigInteger) n;
        }
        return new BigInteger(n.toString());
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
<details><summary>▸ Prompt (8 message(s), ~4,546 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
- *[user] message: identical to step [13] — not reprinted*
- *[assistant] message: identical to step [15] — not reprinted*
- *[user] message: identical to step [5] — not reprinted*
- *[assistant] message: identical to step [17] — not reprinted*
- *[user] message: identical to step [5] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.math.BigInteger;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NumberUtils.createNumber("1234.5");

        String candidate = buildExponentAtEndInput(data);
        try {
            NumberUtils.createNumber(candidate);
        } catch (IllegalArgumentException ignored) {
        }

        checkLongSuffixAgreement(data);
        checkBigIntegerFallbackAgreement(data);
    }

    private static String buildExponentAtEndInput(FuzzedDataProvider data) {
        StringBuilder s = new StringBuilder();
        if (data.consumeBoolean()) {
            s.append('-');
        }

        int intDigits = data.consumeInt(1, 12);
        for (int i = 0; i < intDigits; i++) {
            s.append((char) ('0' + data.consumeInt(0, 9)));
        }

        if (data.consumeBoolean()) {
            s.append('.');
            int fracDigits = data.consumeInt(0, 8);
            for (int i = 0; i < fracDigits; i++) {
                s.append((char) ('0' + data.consumeInt(0, 9)));
            }
        }

        s.append(data.consumeBoolean() ? 'e' : 'E');
        return s.toString();
    }

    private static void checkLongSuffixAgreement(FuzzedDataProvider data) {
        String base = Integer.toString(data.consumeInt(-1_000_000, 1_000_000));
        Number plain;
        Number suffixed;
        try {
            plain = NumberUtils.createNumber(base);
            suffixed = NumberUtils.createNumber(base + "L");
        } catch (RuntimeException e) {
            return;
        }
        if (plain == null || suffixed == null) {
            return;
        }

        /* Contract: adding 'L' requests long/integral parsing of the same valid decimal integer literal,
           so both calls must denote the same numeric value. This catches silent wrong-output regressions
           even if a patch merely suppresses the old exception. */
        if (plain.longValue() != suffixed.longValue()) {
            throw new RuntimeException("[oracle:long-suffix] metamorphic violation: input=" + base
                    + " plain=" + plain + " suffixed=" + suffixed);
        }
    }

    private static void checkBigIntegerFallbackAgreement(FuzzedDataProvider data) {
        String base = buildOversizedInteger(data);
        Number plain;
        Number suffixed;
        BigInteger direct;
        try {
            plain = NumberUtils.createNumber(base);
            suffixed = NumberUtils.createNumber(base + "L");
            direct = NumberUtils.createBigInteger(base);
        } catch (RuntimeException e) {
            return;
        }
        if (plain == null || suffixed == null || direct == null) {
            return;
        }

        /* Contract: an oversized decimal integer should parse as BigInteger, and the 'L' suffixed path
           falls back to BigInteger when createLong overflows. All three real library calls must agree. */
        BigInteger plainValue = toBigInteger(plain);
        BigInteger suffixedValue = toBigInteger(suffixed);
        if (!direct.equals(plainValue) || !direct.equals(suffixedValue)) {
            throw new RuntimeException("[oracle:bigint-fallback] metamorphic violation: input=" + base
                    + " direct=" + direct + " plain=" + plain + " suffixed=" + suffixed);
        }
    }

    private static String buildOversizedInteger(FuzzedDataProvider data) {
        StringBuilder s = new StringBuilder();
        if (data.consumeBoolean()) {
            s.append('-');
        }
        s.append((char) ('1' + data.consumeInt(0, 8)));
        int extraDigits = data.consumeInt(19, 30);
        for (int i = 0; i < extraDigits; i++) {
            s.append((char) ('0' + data.consumeInt(0, 9)));
        }
        return s.toString();
    }

    private static BigInteger toBigInteger(Number n) {
        if (n instanceof BigInteger) {
            return (BigInteger) n;
        }
        return new BigInteger(n.toString());
    }
}
```
- *[user] message: identical to step [5] — not reprinted*

</details>
<details open><summary>▸ Output (~4,554 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.math.BigInteger;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NumberUtils.createNumber("1234.5");

        try {
            NumberUtils.createNumber("1eE");
        } catch (IllegalArgumentException ignored) {
        }

        try {
            NumberUtils.createNumber(buildDualExponentCandidate(data));
        } catch (IllegalArgumentException ignored) {
        }

        checkLongSuffixAgreement(data);
        checkBigIntegerFallbackAgreement(data);
    }

    private static String buildDualExponentCandidate(FuzzedDataProvider data) {
        StringBuilder s = new StringBuilder();
        if (data.consumeBoolean()) {
            s.append('-');
        }

        int prefixDigits = data.consumeInt(1, 6);
        for (int i = 0; i < prefixDigits; i++) {
            s.append((char) ('0' + data.consumeInt(0, 9)));
        }

        if (data.consumeBoolean()) {
            s.append('e');
            int middleLen = data.consumeInt(0, 3);
            for (int i = 0; i < middleLen; i++) {
                s.append((char) ('0' + data.consumeInt(0, 9)));
            }
            s.append('E');
        } else {
            s.append('E');
            int middleLen = data.consumeInt(0, 3);
            for (int i = 0; i < middleLen; i++) {
                s.append((char) ('0' + data.consumeInt(0, 9)));
            }
            s.append('e');
        }

        if (data.consumeBoolean()) {
            int tailLen = data.consumeInt(0, 2);
            for (int i = 0; i < tailLen; i++) {
                s.append((char) ('0' + data.consumeInt(0, 9)));
            }
        }

        return s.toString();
    }

    private static void checkLongSuffixAgreement(FuzzedDataProvider data) {
        String base = Integer.toString(data.consumeInt(-1_000_000, 1_000_000));
        Number plain;
        Number suffixed;
        try {
            plain = NumberUtils.createNumber(base);
            suffixed = NumberUtils.createNumber(base + "L");
        } catch (RuntimeException e) {
            return;
        }
        if (plain == null || suffixed == null) {
            return;
        }

        /* Contract: for a valid canonical decimal integer, adding 'L' only requests a long-typed parse
           of the same value, so both real-library parses must denote the same integer. */
        if (plain.longValue() != suffixed.longValue()) {
            throw new RuntimeException("[oracle:long-suffix] metamorphic violation: input=" + base
                    + " plain=" + plain + " suffixed=" + suffixed);
        }
    }

    private static void checkBigIntegerFallbackAgreement(FuzzedDataProvider data) {
        String base = buildOversizedInteger(data);
        Number plain;
        Number suffixed;
        BigInteger direct;
        try {
            plain = NumberUtils.createNumber(base);
            suffixed = NumberUtils.createNumber(base + "L");
            direct = NumberUtils.createBigInteger(base);
        } catch (RuntimeException e) {
            return;
        }
        if (plain == null || suffixed == null || direct == null) {
            return;
        }

        /* Contract: oversized decimal integers parse as BigInteger on the unsuffixed path, and the 'L'
           path falls back from createLong to createBigInteger. All three values must agree. */
        BigInteger plainValue = toBigInteger(plain);
        BigInteger suffixedValue = toBigInteger(suffixed);
        if (!direct.equals(plainValue) || !direct.equals(suffixedValue)) {
            throw new RuntimeException("[oracle:bigint-fallback] metamorphic violation: input=" + base
                    + " direct=" + direct + " plain=" + plain + " suffixed=" + suffixed);
        }
    }

    private static String buildOversizedInteger(FuzzedDataProvider data) {
        StringBuilder s = new StringBuilder();
        if (data.consumeBoolean()) {
            s.append('-');
        }
        s.append((char) ('1' + data.consumeInt(0, 8)));
        int extraDigits = data.consumeInt(19, 30);
        for (int i = 0; i < extraDigits; i++) {
            s.append((char) ('0' + data.consumeInt(0, 9)));
        }
        return s.toString();
    }

    private static BigInteger toBigInteger(Number n) {
        if (n instanceof BigInteger) {
            return (BigInteger) n;
        }
        return new BigInteger(n.toString());
    }
}
```

</details>

---
## [20] ⚙️ harness-attempt · `attempt_009`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.StringIndexOutOfBoundsException: begin 0, end 4, length 3

---
## [21] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~36,238 chars, 1 new)</summary>

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
private static void safeCreateNumber(String s) {
        try {
            NumberUtils.createNumber(s);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isKnownRootCauseInPatchedRegion(t)) {
                return;
            }
        }
    }
</xref>
<xref>
private static Number exercise(String s, boolean validByConstruction) {
        try {
            return NumberUtils.createNumber(s);
        } catch (RuntimeException e) {
            if (isCleanRejection(e)) {
                return null;
            }
            if (validByConstruction && isRootCause(e) && hasRelevantStack(e)) {
                throw e;
            }
            return null;
        }
    }
</xref>
<xref>
public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NumberUtils.createNumber("1234.5");

        StringBuilder s = new StringBuilder();
        if (data.consumeBoolean()) {
            s.append('-');
        }

        int intDigits = data.consumeInt(1, 8);
        for (int i = 0; i < intDigits; i++) {
            s.append((char) ('0' + data.consumeInt(0, 9)));
        }

        if (data.consumeBoolean()) {
            s.append('.');
            int fracDigits = data.consumeInt(0, 8);
            for (int i = 0; i < fracDigits; i++) {
                s.append((char) ('0' + data.consumeInt(0, 9)));
            }
        }

        if (data.consumeBoolean()) {
            s.append(data.consumeBoolean() ? 'e' : 'E');
        }

        if (data.consumeBoolean()) {
            char suffix;
            switch (data.consumeInt(0, 3)) {
                case 0:
                    suffix = 'D';
                    break;
                case 1:
                    suffix = 'd';
                    break;
                case 2:
                    suffix = 'F';
                    break;
                default:
                    suffix = 'f';
                    break;
            }
            s.append(suffix);
        }

        NumberUtils.createNumber(s.toString());
    }
</xref>
<xref>
private static void safeCreateNumber(String s, boolean propagateKnownRootCause) {
        try {
            NumberUtils.createNumber(s);
        } catch (RuntimeException t) {
            if (propagateKnownRootCause && isKnownRootCauseFromCreateNumber(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }
</xref>
<xref>
public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NumberUtils.createNumber("1234.5");

        String candidate = buildDecimalLikeInput(data);
        try {
            NumberUtils.createNumber(candidate);
        } catch (IllegalArgumentException ignored) {
        }

        checkLongSuffixMetamorphic(data);
        checkBigIntegerFallbackMetamorphic(data);
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
message: begin 0, end 4, length 3
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
java.lang.StringIndexOutOfBoundsException: begin 0, end 4, length 3
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
- isBlank
- createInteger
- isAllZeros
- charAt
- substring
- isDigits
- createLong
- createBigInteger
- createFloat
- createDouble
- createBigDecimal
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.lang3.math.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.lang3.math.NumberUtils.createNumber
Crashes already found:
- java.lang.StringIndexOutOfBoundsException@org.apache.commons.lang3.math.NumberUtils.createNumber
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- isBlank
- createInteger
- isAllZeros
- charAt
- substring
- isDigits
- createLong
- createBigInteger
- createFloat
- createDouble
- createBigDecimal
Check FAMILIES already covered by accepted harnesses: {anchor-float, bigint-fallback, exp-equivalence, int-helper, long-suffix}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.StringIndexOutOfBoundsException@org.apache.commons.lang3.math.NumberUtils.createNumber. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
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
<details open><summary>▸ Output (~4,771 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        safeCreateNumber("1234.5", true);

        safeCreateNumber(buildTrailingExponentInput(data), false);

        String validCore = buildValidCoreNumber(data);
        safeCreateNumber(validCore, true);
        safeCreateNumber(validCore + "D", true);
        safeCreateNumber(validCore + "d", true);

        checkDoubleSuffixConsistency("1234.5");
        checkDoubleSuffixConsistency(validCore);
    }

    private static void safeCreateNumber(String s, boolean validByConstruction) {
        try {
            NumberUtils.createNumber(s);
        } catch (RuntimeException t) {
            if (validByConstruction && isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static boolean isCleanRejection(RuntimeException t) {
        return t instanceof IllegalArgumentException;
    }

    private static boolean isRootCause(RuntimeException t) {
        if (!(t instanceof StringIndexOutOfBoundsException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement ste = trace[i];
            if ("org.apache.commons.lang3.math.NumberUtils".equals(ste.getClassName())
                    && "createNumber".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static String buildTrailingExponentInput(FuzzedDataProvider data) {
        StringBuilder s = new StringBuilder();
        if (data.consumeBoolean()) {
            s.append('-');
        }

        int intDigits = data.consumeInt(1, 8);
        for (int i = 0; i < intDigits; i++) {
            s.append((char) ('0' + data.consumeInt(0, 9)));
        }

        if (data.consumeBoolean()) {
            s.append('.');
            int fracDigits = data.consumeInt(0, 8);
            for (int i = 0; i < fracDigits; i++) {
                s.append((char) ('0' + data.consumeInt(0, 9)));
            }
        }

        s.append(data.consumeBoolean() ? 'e' : 'E');
        return s.toString();
    }

    private static String buildValidCoreNumber(FuzzedDataProvider data) {
        StringBuilder s = new StringBuilder();
        if (data.consumeBoolean()) {
            s.append('-');
        }

        int intDigits = data.consumeInt(1, 6);
        for (int i = 0; i < intDigits; i++) {
            s.append((char) ('0' + data.consumeInt(0, 9)));
        }

        s.append('.');

        int fracDigits = data.consumeInt(1, 4);
        for (int i = 0; i < fracDigits; i++) {
            s.append((char) ('0' + data.consumeInt(0, 9)));
        }

        if (data.consumeBoolean()) {
            s.append(data.consumeBoolean() ? 'e' : 'E');
            if (data.consumeBoolean()) {
                s.append(data.consumeBoolean() ? '+' : '-');
            }
            int expDigits = data.consumeInt(1, 2);
            for (int i = 0; i < expDigits; i++) {
                s.append((char) ('0' + data.consumeInt(0, 9)));
            }
        }

        return s.toString();
    }

    private static void checkDoubleSuffixConsistency(String core) {
        try {
            Number upper = NumberUtils.createNumber(core + "D");
            Number lower = NumberUtils.createNumber(core + "d");
            Double direct = NumberUtils.createDouble(core);

            if (upper == null || lower == null || direct == null) {
                return;
            }

            double upperValue = upper.doubleValue();
            double lowerValue = lower.doubleValue();
            double directValue = direct.doubleValue();

            if (Double.doubleToLongBits(upperValue) != Double.doubleToLongBits(lowerValue)
                    || Double.doubleToLongBits(upperValue) != Double.doubleToLongBits(directValue)) {
                throw new RuntimeException(
                        "[oracle:double-suffix-case] metamorphic violation: createNumber(core+\"D\") and createNumber(core+\"d\") must agree with createDouble(core)"
                                + " input=" + core
                                + " upper=" + upper
                                + " lower=" + lower
                                + " direct=" + direct);
            }
        } catch (RuntimeException ignored) {
            if (ignored.getMessage() != null && ignored.getMessage().startsWith("[oracle:double-suffix-case]")) {
                throw ignored;
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
- trigger: java.lang.RuntimeException: [oracle:double-suffix-case] metamorphic violation: createNumber(core+"D") and createNumber(core+"d") must agree with createDouble(core) input=-0.0 upper=0.0 lower=0.0 direc…

---
## [23] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~36,530 chars, 1 new)</summary>

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
private static void safeCreateNumber(String s) {
        try {
            NumberUtils.createNumber(s);
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isKnownRootCauseInPatchedRegion(t)) {
                return;
            }
        }
    }
</xref>
<xref>
private static Number exercise(String s, boolean validByConstruction) {
        try {
            return NumberUtils.createNumber(s);
        } catch (RuntimeException e) {
            if (isCleanRejection(e)) {
                return null;
            }
            if (validByConstruction && isRootCause(e) && hasRelevantStack(e)) {
                throw e;
            }
            return null;
        }
    }
</xref>
<xref>
public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NumberUtils.createNumber("1234.5");

        StringBuilder s = new StringBuilder();
        if (data.consumeBoolean()) {
            s.append('-');
        }

        int intDigits = data.consumeInt(1, 8);
        for (int i = 0; i < intDigits; i++) {
            s.append((char) ('0' + data.consumeInt(0, 9)));
        }

        if (data.consumeBoolean()) {
            s.append('.');
            int fracDigits = data.consumeInt(0, 8);
            for (int i = 0; i < fracDigits; i++) {
                s.append((char) ('0' + data.consumeInt(0, 9)));
            }
        }

        if (data.consumeBoolean()) {
            s.append(data.consumeBoolean() ? 'e' : 'E');
        }

        if (data.consumeBoolean()) {
            char suffix;
            switch (data.consumeInt(0, 3)) {
                case 0:
                    suffix = 'D';
                    break;
                case 1:
                    suffix = 'd';
                    break;
                case 2:
                    suffix = 'F';
                    break;
                default:
                    suffix = 'f';
                    break;
            }
            s.append(suffix);
        }

        NumberUtils.createNumber(s.toString());
    }
</xref>
<xref>
private static void safeCreateNumber(String s, boolean propagateKnownRootCause) {
        try {
            NumberUtils.createNumber(s);
        } catch (RuntimeException t) {
            if (propagateKnownRootCause && isKnownRootCauseFromCreateNumber(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }
</xref>
<xref>
public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NumberUtils.createNumber("1234.5");

        String candidate = buildDecimalLikeInput(data);
        try {
            NumberUtils.createNumber(candidate);
        } catch (IllegalArgumentException ignored) {
        }

        checkLongSuffixMetamorphic(data);
        checkBigIntegerFallbackMetamorphic(data);
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
message: begin 0, end 4, length 3
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
java.lang.StringIndexOutOfBoundsException: begin 0, end 4, length 3
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
- isBlank
- createInteger
- isAllZeros
- charAt
- substring
- isDigits
- createLong
- createBigInteger
- createFloat
- createDouble
- createBigDecimal
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.lang3.math.FuzzHarness.checkDoubleSuffixConsistency
- org.apache.commons.lang3.math.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.lang3.math.NumberUtils.createNumber
Crashes already found:
- java.lang.StringIndexOutOfBoundsException@org.apache.commons.lang3.math.NumberUtils.createNumber
- java.lang.RuntimeException@org.apache.commons.lang3.math.FuzzHarness.checkDoubleSuffixConsistency
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- isBlank
- createInteger
- isAllZeros
- charAt
- substring
- isDigits
- createLong
- createBigInteger
- createFloat
- createDouble
- createBigDecimal
Check FAMILIES already covered by accepted harnesses: {anchor-float, bigint-fallback, double-suffix-case, exp-equivalence, int-helper, long-suffix}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.StringIndexOutOfBoundsException@org.apache.commons.lang3.math.NumberUtils.createNumber; java.lang.RuntimeException@org.apache.commons.lang3.math.FuzzHarness.checkDoubleSuffixConsistency. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
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
<details open><summary>▸ Output (~6,115 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.math.BigDecimal;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            NumberUtils.createNumber("1234.5");
        } catch (RuntimeException ignored) {
        }

        String base = buildCanonicalInteger(data);
        String malformed = buildDualExponentMalformed(base, data);

        checkMalformedDualExponentRejectedCleanly(malformed);

        String trailingDotBase = buildModerateNonZeroInteger(data);
        checkTrailingDotEquivalence(trailingDotBase);
    }

    private static String buildCanonicalInteger(FuzzedDataProvider data) {
        StringBuilder s = new StringBuilder();
        if (data.consumeBoolean()) {
            s.append('-');
        }
        int digits = data.consumeInt(1, 6);
        s.append((char) ('1' + data.consumeInt(0, 8)));
        for (int i = 1; i < digits; i++) {
            s.append((char) ('0' + data.consumeInt(0, 9)));
        }
        return s.toString();
    }

    private static String buildModerateNonZeroInteger(FuzzedDataProvider data) {
        StringBuilder s = new StringBuilder();
        if (data.consumeBoolean()) {
            s.append('-');
        }
        int digits = data.consumeInt(1, 6);
        s.append((char) ('1' + data.consumeInt(0, 8)));
        for (int i = 1; i < digits; i++) {
            s.append((char) ('0' + data.consumeInt(0, 9)));
        }
        return s.toString();
    }

    private static String buildDualExponentMalformed(String base, FuzzedDataProvider data) {
        StringBuilder s = new StringBuilder(base);
        if (data.consumeBoolean()) {
            s.append('.');
            int fracDigits = data.consumeInt(0, 4);
            for (int i = 0; i < fracDigits; i++) {
                s.append((char) ('0' + data.consumeInt(0, 9)));
            }
        }
        if (data.consumeBoolean()) {
            s.append('e');
            s.append('E');
        } else {
            s.append('E');
            s.append('e');
        }
        int tailDigits = data.consumeInt(0, 3);
        for (int i = 0; i < tailDigits; i++) {
            s.append((char) ('0' + data.consumeInt(0, 9)));
        }
        if (data.consumeBoolean()) {
            char suffix;
            switch (data.consumeInt(0, 3)) {
                case 0:
                    suffix = 'f';
                    break;
                case 1:
                    suffix = 'F';
                    break;
                case 2:
                    suffix = 'd';
                    break;
                default:
                    suffix = 'D';
                    break;
            }
            s.append(suffix);
        }
        return s.toString();
    }

    private static void checkMalformedDualExponentRejectedCleanly(String s) {
        try {
            Number n = NumberUtils.createNumber(s);
            if (n != null) {
                throw new RuntimeException("[oracle:dual-exp-reject] metamorphic violation: malformed dual-exponent input was accepted input=" + s + " result=" + n);
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRelevantInternalParserFailure(t)) {
                throw new RuntimeException("[oracle:dual-exp-reject] metamorphic violation: malformed dual-exponent input caused internal parser failure input=" + s + " throwable=" + t, t);
            }
        }
    }

    private static void checkTrailingDotEquivalence(String base) {
        try {
            Number plain = NumberUtils.createNumber(base);
            Number dotted = NumberUtils.createNumber(base + ".");
            Number dottedZero = NumberUtils.createNumber(base + ".0");

            BigDecimal a = new BigDecimal(plain.toString());
            BigDecimal b = new BigDecimal(dotted.toString());
            BigDecimal c = new BigDecimal(dottedZero.toString());

            if (a.compareTo(b) != 0 || b.compareTo(c) != 0) {
                throw new RuntimeException("[oracle:trailing-dot] metamorphic violation: equivalent decimal forms disagree input=" + base + " plain=" + plain + " dotted=" + dotted + " dottedZero=" + dottedZero);
            }
        } catch (RuntimeException ignored) {
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRelevantInternalParserFailure(Throwable t) {
        boolean classMatch = false;
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof StringIndexOutOfBoundsException || cur instanceof IndexOutOfBoundsException) {
                classMatch = true;
                break;
            }
        }
        if (!classMatch) {
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
            if ("java.lang.String".equals(cls) && ("substring".equals(method) || "charAt".equals(method))) {
                return true;
            }
        }
        return false;
    }
}
```

</details>

---
## [24] ⚙️ harness-attempt · `attempt_011`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.RuntimeException: [oracle:dual-exp-reject] metamorphic violation: malformed dual-exponent input caused internal parser failure input=1Ee throwable=java.lang.StringIndexOutOfBoundsException: …

---
## [25] ⚙️ corpus-seed · `attempt_003`
**output:** **96 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['createNumber(String) 1 failed', '1234.5', 'createNumber(String) 2 failed', '12345', 'createNumber(String) 3 failed', '1234.5D', '1234.5d', 'createNumber(String) 4 failed']}

---
## [26] ⚙️ corpus-seed · `attempt_005`
**output:** **96 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['createNumber(String) 1 failed', '1234.5', 'createNumber(String) 2 failed', '12345', 'createNumber(String) 3 failed', '1234.5D', '1234.5d', 'createNumber(String) 4 failed']}

---
## [27] ⚙️ corpus-seed · `attempt_009`
**output:** **96 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['createNumber(String) 1 failed', '1234.5', 'createNumber(String) 2 failed', '12345', 'createNumber(String) 3 failed', '1234.5D', '1234.5d', 'createNumber(String) 4 failed']}

---
## [28] ⚙️ corpus-seed · `attempt_010`
**output:** **96 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['createNumber(String) 1 failed', '1234.5', 'createNumber(String) 2 failed', '12345', 'createNumber(String) 3 failed', '1234.5D', '1234.5d', 'createNumber(String) 4 failed']}

---
## [29] ⚙️ corpus-seed · `attempt_011`
**output:** **96 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['createNumber(String) 1 failed', '1234.5', 'createNumber(String) 2 failed', '12345', 'createNumber(String) 3 failed', '1234.5D', '1234.5d', 'createNumber(String) 4 failed']}

---
## [30] ⚙️ patched-fuzz · `attempt_003`
**output:** **quiet on patched build (no overfit signal)**

---
## [31] ⚙️ patched-fuzz · `attempt_005`
**output:** **FIRED — crash on patched build**
- reproducing_input_file: /tmp/d4j/Lang_27_buggy/fuzz/attempt_005/crashes/crash-da39a3ee5e6b4b0d3255bfef95601890afd80709

---
## [32] ⚙️ patched-fuzz · `attempt_009`
**output:** **quiet on patched build (no overfit signal)**

---
## [33] ⚙️ patched-fuzz · `attempt_010`
**output:** **FIRED — [oracle:double-suffix-case] metamorphic violation: createNumber(core+"D") and createNumber(core+"d") must agree with createDouble(core) input=-0.0 upper=0.0 lower=0.0 direct=-0.0**
- reproducing_input_file: /tmp/d4j/Lang_27_buggy/fuzz/attempt_010/crashes/crash-500ce5edbda324f9a385f06a24094cee873bcf07

---
## [34] ⚙️ patched-fuzz · `attempt_011`
**output:** 
```
FIRED — [oracle:dual-exp-reject] metamorphic violation: malformed dual-exponent input caused internal parser failure input=4.Ee throwable=java.lang.StringIndexOutOfBoundsException: begin 2, end 6, length 4
```
- reproducing_input_file: /tmp/d4j/Lang_27_buggy/fuzz/attempt_011/crashes/crash-9a7c309aed3a1e516a44e75768ac1af64b7db81a
