# Pipeline trace — Lang-58

**Code:** `unknown`

**Patch label:** overfitting  *(the patch under analysis is a known-OVERFIT fix — the pipeline is not told this)*

**Outcome:** overfit MISSED (all harnesses quiet on the patched build). [evaluated; 3 harness(es) fuzzed on the patched build; campaign converged=True]

**Patch under analysis:**
```diff
--- /src/java/org/apache/commons/lang/math/NumberUtils.java	
+++ /src/java/org/apache/commons/lang/math/NumberUtils.java
@@ -451,7 +451,7 @@
                 case 'L' :
                     if (dec == null
                         && exp == null
-                        && isDigits(numeric.substring(1))
+                        || isDigits(numeric.substring(1))
                         && (numeric.charAt(0) == '-' || Character.isDigit(numeric.charAt(0)))) {
                         try {
                             return createLong(numeric);
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
<details><summary>▸ output — FailureTest(test_class='org.apache.commons.lang.math.NumberUtilsTest', test_method='testLang300', so (~499 chars)</summary>

**output:** 
```json
[
  "FailureTest(test_class='org.apache.commons.lang.math.NumberUtilsTest', test_method='testLang300', source_path='/tmp/d4j/Lang_58_buggy/src/test/org/apache/commons/lang/math/NumberUtilsTest.java', method_source='    public void testLang300() {\\n        NumberUtils.createNumber(\"-1l\");\\n        NumberUtils.createNumber(\"01l\");\\n        NumberUtils.createNumber(\"1l\");\\n    }', exception_type='java.lang.NumberFormatException', failure_message=None, support_source=None)"
]
```

</details>

---
## [1] ⚙️ test-context (H1/H2)
**output:** 
```json
[
  {
    "test": "org.apache.commons.lang.math.NumberUtilsTest::testLang300",
    "failure_message": "--- org.apache.commons.lang.math.NumberUtilsTest::testLang300\njava.lang.NumberFormatException: 1l is not a valid number.",
    "support_chars": 0
  }
]
```

---
## [2] ⚙️ analysis (TargetAnalyzer)
<details><summary>▸ output — modified_files": [ (~13,601 chars)</summary>

**output:** 
```json
{
  "modified_files": [
    "src/java/org/apache/commons/lang/math/NumberUtils.java"
  ],
  "patch_text": "--- /src/java/org/apache/commons/lang/math/NumberUtils.java\t\n+++ /src/java/org/apache/commons/lang/math/NumberUtils.java\n@@ -451,7 +451,7 @@\n                 case 'L' :\n                     if (dec == null\n                         && exp == null\n-                        && isDigits(numeric.substring(1))\n+                        || isDigits(numeric.substring(1))\n                         && (numeric.charAt(0) == '-' || Character.isDigit(numeric.charAt(0)))) {\n                         try {\n                             return createLong(numeric);\n",
  "functions": [
    {
      "func_name": "createNumber",
      "func_signature": "public static Number createNumber(String str) throws NumberFormatException",
      "func_source": "    public static Number createNumber(String str) throws NumberFormatException {\n        if (str == null) {\n            return null;\n        }\n        if (StringUtils.isBlank(str)) {\n            throw new NumberFormatException(\"A blank string is not a valid number\");\n        }  \n        if (str.startsWith(\"--\")) {\n            // this is protection for poorness in java.lang.BigDecimal.\n            // it accepts this as a legal value, but it does not appear \n            // to be in specification of class. OS X Java parses it to \n            // a wrong value.\n            return null;\n        }\n        if (str.startsWith(\"0x\") || str.startsWith(\"-0x\")) {\n            return createInteger(str);\n        }   \n        char lastChar = str.charAt(str.length() - 1);\n        String mant;\n        String dec;\n        String exp;\n        int decPos = str.indexOf('.');\n        int expPos = str.indexOf('e') + str.indexOf('E') + 1;\n\n        if (decPos > -1) {\n\n            if (expPos > -1) {\n                if (expPos < decPos) {\n                    throw new NumberFormatException(str + \" is not a valid number.\");\n                }\n                dec = str.substring(decPos + 1, expPos);\n            } else {\n                dec = str.substring(decPos + 1);\n            }\n            mant = str.substring(0, decPos);\n        } else {\n            if (expPos > -1) {\n                mant = str.substring(0, expPos);\n            } else {\n                mant = str;\n            }\n            dec = null;\n        }\n        if (!Character.isDigit(lastChar)) {\n            if (expPos > -1 && expPos < str.length() - 1) {\n                exp = str.substring(expPos + 1, str.length() - 1);\n            } else {\n                exp = null;\n            }\n            //Requesting a specific type..\n            String numeric = str.substring(0, str.length() - 1);\n            boolean allZeros = isAllZeros(mant) && isAllZeros(exp);\n            switch (lastChar) {\n                case 'l' :\n                case 'L' :\n                    if (dec == null\n                        && exp == null\n                        && isDigits(numeric.substring(1))\n                        && (numeric.charAt(0) == '-' || Character.isDigit(numeric.charAt(0)))) {\n                        try {\n                            return createLong(numeric);\n                        } catch (NumberFormatException nfe) {\n                            //Too big for a long\n                        }\n                        return createBigInteger(numeric);\n\n                    }\n                    throw new NumberFormatException(str + \" is not a valid number.\");\n                case 'f' :\n                case 'F' :\n                    try {\n                        Float f = NumberUtils.createFloat(numeric);\n                        if (!(f.isInfinite() || (f.floatValue() == 0.0F && !allZeros))) {\n                            //If it's too big for a float or the float value = 0 and the string\n                            //has non-zeros in it, then float does not have the precision we want\n                            return f;\n                        }\n\n                    } catch (NumberFormatException nfe) {\n                        // ignore the bad number\n                    }\n                    //Fall through\n                case 'd' :\n                case 'D' :\n                    try {\n                        Double d = NumberUtils.createDouble(numeric);\n                        if (!(d.isInfinite() || (d.floatValue() == 0.0D && !allZeros))) {\n                            return d;\n                        }\n                    } catch (NumberFormatException nfe) {\n                        // ignore the bad number\n                    }\n                    try {\n                        return createBigDecimal(numeric);\n                    } catch (NumberFormatException e) {\n                        // ignore the bad number\n                    }\n                    //Fall through\n                default :\n                    throw new NumberFormatException(str + \" is not a valid number.\");\n\n            }\n        } else {\n            //User doesn't have a preference on the return type, so let's start\n            //small and go from there...\n            if (expPos > -1 && expPos < str.length() - 1) {\n                exp = str.substring(expPos + 1, str.length());\n            } else {\n                exp = null;\n            }\n            if (dec == null && exp == null) {\n                //Must be an int,long,bigint\n                try {\n                    return createInteger(str);\n                } catch (NumberFormatException nfe) {\n                    // ignore the bad number\n                }\n                try {\n                    return createLong(str);\n                } catch (NumberFormatException nfe) {\n                    // ignore the bad number\n                }\n                return createBigInteger(str);\n\n            } else {\n                //Must be a float,double,BigDec\n                boolean allZeros = isAllZeros(mant) && isAllZeros(exp);\n                try {\n                    Float f = createFloat(str);\n                    if (!(f.isInfinite() || (f.floatValue() == 0.0F && !allZeros))) {\n                        return f;\n                    }\n                } catch (NumberFormatException nfe) {\n                    // ignore the bad number\n                }\n                try {\n                    Double d = createDouble(str);\n                    if (!(d.isInfinite() || (d.doubleValue() == 0.0D && !allZeros))) {\n                        return d;\n                    }\n                } catch (NumberFormatException nfe) {\n                    // ignore the bad number\n                }\n\n                return createBigDecimal(str);\n\n            }\n        }\n    }",
      "func_class": "NumberUtils",
      "func_class_fq": "org.apache.commons.lang.math.NumberUtils",
      "func_param_types": [
        "String"
      ],
      "fi_name": "[org.apache.commons.lang.math.NumberUtils].createNumber(String)",
      "overload_types": [
        [
          "String"
        ]
      ],
      "xrefs": [],
      "reachable": [
        "[org.apache.commons.lang.StringUtils].isBlank(String)",
        "[NumberFormatException].<init>(String)",
        "[String].startsWith(String)",
        "[org.apache.commons.lang.math.NumberUtils].createInteger(String)",
        "[String].length()",
        "[String].charAt(int)",
        "[String].indexOf(char)",
        "[String].substring(int,org.apache.commons.lang.math.NumberUtils)",
        "[String].substring(int)",
        "Character.isDigit(org.apache.commons.lang.math.NumberUtils)",
        "[String].substring(int,int)",
        "[org.apache.commons.lang.math.NumberUtils].isAllZeros(org.apache.commons.lang.math.NumberUtils)",
        "[org.apache.commons.lang.math.NumberUtils].substring(int)",
        "[org.apache.commons.lang.math.NumberUtils].isDigits(org.apache.commons.lang.math.NumberUtils)",
        "[org.apache.commons.lang.math.NumberUtils].charAt(int)",
        "[org.apache.commons.lang.math.NumberUtils].createLong(org.apache.commons.lang.math.NumberUtils)",
        "[org.apache.commons.lang.math.NumberUtils].createBigInteger(org.apache.commons.lang.math.NumberUtils)",
        "[org.apache.commons.lang.NumberUtils].createFloat(org.apache.commons.lang.math.NumberUtils)",
        "[Float].isInfinite()",
        "[Float].floatValue()",
        "[org.apache.commons.lang.NumberUtils].createDouble(org.apache.commons.lang.math.NumberUtils)",
        "[Double].isInfinite()",
        "[Double].floatValue()",
        "[org.apache.commons.lang.math.NumberUtils].createBigDecimal(org.apache.commons.lang.math.NumberUtils)",
        "[org.apache.commons.lang.math.NumberUtils].createLong(String)",
        "[org.apache.commons.lang.math.NumberUtils].createBigInteger(String)",
        "[org.apache.commons.lang.math.NumberUtils].createFloat(String)",
        "[org.apache.commons.lang.math.NumberUtils].createDouble(String)",
        "[Double].doubleValue()",
        "[org.apache.commons.lang.math.NumberUtils].createBigDecimal(String)",
        "[String].charAt(org.apache.commons.lang.StringUtils)",
        "Character.isWhitespace(org.apache.commons.lang.StringUtils)",
        "Integer.decode(String)",
        "Long.valueOf(String)",
        "[java.math.BigInteger].<init>(String)",
        "Float.valueOf(String)",
        "Double.valueOf(String)",
        "[java.math.BigDecimal].<init>(String)",
        "[org.apache.commons.lang.NumberUtils].createBigDecimal(String)",
        "[org.apache.commons.lang.NumberUtils].createBigInteger(String)",
        "[org.apache.commons.lang.NumberUtils].createDouble(String)",
        "[org.apache.commons.lang.NumberUtils].createFloat(String)",
        "[org.apache.commons.lang.NumberUtils].createInteger(String)",
        "[org.apache.commons.lang.NumberUtils].createLong(String)",
        "[org.apache.commons.lang.NumberUtils].isAllZeros(String)",
        "[org.apache.commons.lang.NumberUtils].isDigits(String)"
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
  "package": "org.apache.commons.lang.math",
  "root_cause_reachable": [
    "StringUtils.isBlank",
    "NumberUtils.createInteger",
    "NumberUtils.isAllZeros",
    "NumberUtils.substring",
    "NumberUtils.isDigits",
    "NumberUtils.charAt",
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
    "import org.apache.commons.lang.StringUtils;"
  ]
}
```

</details>

---
## [3] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~29,718 chars)</summary>

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
- Package: `org.apache.commons.lang.math` (`package org.apache.commons.lang.math;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Lang_58_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/java/org/apache/commons/lang/math/NumberUtils.java	
+++ /src/java/org/apache/commons/lang/math/NumberUtils.java
@@ -451,7 +451,7 @@
                 case 'L' :
                     if (dec == null
                         && exp == null
-                        && isDigits(numeric.substring(1))
+                        || isDigits(numeric.substring(1))
                         && (numeric.charAt(0) == '-' || Character.isDigit(numeric.charAt(0)))) {
                         try {
                             return createLong(numeric);

</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import java.math.BigDecimal;
import java.math.BigInteger;
import org.apache.commons.lang.StringUtils;
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
        if (!Character.isDigit(lastChar)) {
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
                        && isDigits(numeric.substring(1))
                        && (numeric.charAt(0) == '-' || Character.isDigit(numeric.charAt(0)))) {
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
                    //Fall through
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
                    //Fall through
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
message: 1l is not a valid number.
thrown_at: org.apache.commons.lang.math.NumberUtils.createNumber(NumberUtils.java:464)
anchor_input: "-1l"  // hard-code this verbatim as your first call, then fuzz inputs of the same shape
other_observed_literals: "01l", "1l"
</ground_truth_crash>
<failing_test class="org.apache.commons.lang.math.NumberUtilsTest" method="testLang300">
    public void testLang300() {
        NumberUtils.createNumber("-1l");
        NumberUtils.createNumber("01l");
        NumberUtils.createNumber("1l");
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.lang.math.NumberUtilsTest::testLang300
java.lang.NumberFormatException: 1l is not a valid number.
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  compare(double lhs, double rhs) / (float lhs, float rhs)
  equals(byte[] array1, byte[] array2) / (short[] array1, short[] array2) / (int[] array1, int[] array2) / (long[] array1, long[] array2) / (float[] array1, float[] array2) / (double[] array1, double[] array2)
  max(long[] array) / (int[] array) / (short[] array) / (byte[] array) / (double[] array) / (float[] array) / (long a, long b, long c) / (int a, int b, int c) / (short a, short b, short c) / (byte a, byte b, byte c) / (double a, double b, double c) / (float a, float b, float c)
  min(long[] array) / (int[] array) / (short[] array) / (byte[] array) / (double[] array) / (float[] array) / (long a, long b, long c) / (int a, int b, int c) / (short a, short b, short c) / (byte a, byte b, byte c) / (double a, double b, double c) / (float a, float b, float c)
  stringToInt(String str) / (String str, int defaultValue)
  toDouble(String str) / (String str, double defaultValue)
  toFloat(String str) / (String str, float defaultValue)
  toInt(String str) / (String str, int defaultValue)

METHOD FAMILIES over the same input space (factory/parser siblings — where the docs state a selection or agreement rule between family members, equivalent inputs must respect it):
  create* family: createBigDecimal,

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- StringUtils.isBlank
- NumberUtils.createInteger
- NumberUtils.isAllZeros
- NumberUtils.substring
- NumberUtils.isDigits
- NumberUtils.charAt
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
package org.apache.commons.lang.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~10,357 chars)</summary>

```
package org.apache.commons.lang.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.math.BigInteger;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        exerciseValidLongLiteral("-1l", true);
        exerciseValidLongLiteral("01l", true);
        exerciseValidLongLiteral("1l", true);

        String fuzz = buildLongLikeLiteral(data);
        if (fuzz != null) {
            exerciseValidLongLiteral(fuzz, false);
        }

        String ascii = data.consumeAsciiString(Math.min(32, data.remainingBytes()));
        if (ascii.length() > 0) {
            char suffix = data.consumeBoolean() ? 'l' : 'L';
            String candidate = ascii + suffix;
            try {
                Number parsed = NumberUtils.createNumber(candidate);
                String numeric = candidate.substring(0, candidate.length() - 1);
                if (isStrictDecimalIntegerForLongSuffix(numeric)) {
                    Number expected = expectedForLongSuffix(numeric);
                    if (!sameNumericValue(parsed, expected)) {
                        throw new RuntimeException(
                            "[oracle:long-suffix-helper] metamorphic violation: createNumber disagrees with helper family input="
                                + candidate + " lhs=" + parsed + " rhs=" + expected);
                    }
                }
            } catch (RuntimeException t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t) && isValidLongLiteralInput(candidate)) {
                    throw t;
                }
            }
        }
    }

    private static void exerciseValidLongLiteral(String literal, boolean anchor) {
        try {
            Number parsed = NumberUtils.createNumber(literal);
            String numeric = literal.substring(0, literal.length() - 1);

            // Contract visible in createNumber: for 'l'/'L' suffix and a valid decimal integer,
            // createNumber must return createLong(numeric), or createBigInteger(numeric) if too big.
            // A throw-deleting or branch-skipping patch could return the wrong value/type silently, so
            // we cross-check against the real helper family on the same numeric payload.
            Number expected = expectedForLongSuffix(numeric);
            if (!sameNumericValue(parsed, expected)) {
                throw new RuntimeException(
                    "[oracle:long-suffix-helper] metamorphic violation: createNumber disagrees with helper family input="
                        + literal + " lhs=" + parsed + " rhs=" + expected + (anchor ? " anchor=true" : ""));
            }

            // Equivalent-input relation for decimal integer strings: adding redundant leading zeros
            // preserves the numeric value, so the long-suffixed forms must parse to the same number.
            String normalized = normalizeLongLiteral(literal);
            try {
                Number reparsed = NumberUtils.createNumber(normalized);
                if (!sameNumericValue(parsed, reparsed)) {
                    throw new RuntimeException(
                        "[oracle:leading-zero-equivalence] metamorphic violation: equivalent long literals differ input="
                            + literal + " normalized=" + normalized + " lhs=" + parsed + " rhs=" + reparsed
                            + (anchor ? " anchor=true" : ""));
                }
            } catch (RuntimeException t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t) && isValidLongLiteralInput(normalized)) {
                    throw t;
                }
            }
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                if (isRootCause(t) && isValidLongLiteralInput(literal)) {
                    throw t;
                }
                return;
            }
            if (isRootCause(t) && isValidLongLiteralInput(literal)) {
                throw t;
            }
        }
    }

    private static String buildLongLikeLiteral(FuzzedDataProvider data) {
        int style = data.consumeInt(0, 5);
        char suffix = data.consumeBoolean() ? 'l' : 'L';
        boolean negative = data.consumeBoolean();
        int digitsLen = data.consumeInt(1, 24);

        String digits;
        switch (style) {
            case 0:
                digits = "1";
                break;
            case 1:
                digits = repeat('0', data.consumeInt(1, 6)) + digitString(data, Math.max(1, digitsLen - 1), true);
                break;
            case 2:
                digits = digitString(data, digitsLen, true);
                break;
            case 3:
                digits = "0" + digitString(data, digitsLen, true);
                break;
            case 4:
                digits = String.valueOf(Math.abs((long) data.consumeInt()));
                break;
            default:
                String tail = data.consumeAsciiString(Math.min(8, data.remainingBytes()));
                digits = "1" + stripNonDigits(tail);
                if (digits.length() == 0) {
                    digits = "1";
                }
                break;
        }

        if (digits.length() == 0) {
            digits = "0";
        }
        if (!containsOnlyDigits(digits)) {
            return null;
        }
        return (negative ? "-" : "") + digits + suffix;
    }

    private static Number expectedForLongSuffix(String numeric) {
        try {
            return NumberUtils.createLong(numeric);
        } catch (NumberFormatException e) {
            return NumberUtils.createBigInteger(numeric);
        }
    }

    private static boolean sameNumericValue(Number a, Number b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a instanceof Float || a instanceof Double || b instanceof Float || b instanceof Double) {
            return Double.compare(a.doubleValue(), b.doubleValue()) == 0;
        }
        try {
            return new BigInteger(a.toString()).equals(new BigInteger(b.toString()));
        } catch (RuntimeException ignored) {
            return a.toString().equals(b.toString());
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NumberFormatException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (int i = 0; i < st.length; i++) {
            String cls = st[i].getClassName();
            String method = st[i].getMethodName();
            if ("org.apache.commons.lang.math.NumberUtils".equals(cls)) {
                if ("createNumber".equals(method)
                    || "createLong".equals(method)
                    || "createBigInteger".equals(method)
                    || "isDigits".equals(method)
                    || "createInteger".equals(method)
                    || "createFloat".equals(method)
                    || "createDouble".equals(method)
                    || "createBigDecimal".equals(method)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException;
    }

    private static boolean isValidLongLiteralInput(String literal) {
        if (literal == null || literal.length() < 2) {
            return false;
        }
        char last = literal.charAt(literal.length() - 1);
        if (last != 'l' && last != 'L') {
            return false;
        }
        return isStrictDecimalIntegerForLongSuffix(literal.substring(0, literal.length() - 1));
    }

    private static boolean isStrictDecimalIntegerForLongSuffix(String numeric) {
        if (numeric == null || numeric.length() == 0) {
            return false;
        }
        int start = 0;
        char first = numeric.charAt(0);
        if (first == '-') {
            if (numeric.length() == 1) {
                return false;
            }
            start = 1;
        } else if (!Character.isDigit(first)) {
            return false;
        }
        for (int i = start; i < numeric.length(); i++) {
            if (!Character.isDigit(numeric.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static String normalizeLongLiteral(String literal) {
        char suffix = literal.charAt(literal.length() - 1);
        String numeric = literal.substring(0, literal.length() - 1);
        boolean negative = numeric.startsWith("-");
        String digits = negative ? numeric.substring(1) : numeric;
        int i = 0;
        while (i < digits.length() - 1 && digits.charAt(i) == '0') {
            i++;
        }
        digits = digits.substring(i);
        return (negative ? "-" : "") + digits + suffix;
    }

    private static String digitString(FuzzedDataProvider data, int len, boolean allowLeadingZero) {
        if (len <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            int d;
            if (i == 0 && !allowLeadingZero) {
                d = data.consumeInt(1, 9);
            } else {
                d = data.consumeInt(0, 9);
            }
            sb.append((char) ('0' + d));
        }
        return sb.toString();
    }

    private static boolean containsOnlyDigits(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }
        return s.length() > 0;
    }

    private static String stripNonDigits(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isDigit(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
```

</details>

---
## [4] ⚙️ harness-attempt · `attempt_001`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.NumberFormatException: 1l is not a valid number.

---
## [5] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~31,267 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.apache.commons.lang.math` (`package org.apache.commons.lang.math;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Lang_58_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/java/org/apache/commons/lang/math/NumberUtils.java	
+++ /src/java/org/apache/commons/lang/math/NumberUtils.java
@@ -451,7 +451,7 @@
                 case 'L' :
                     if (dec == null
                         && exp == null
-                        && isDigits(numeric.substring(1))
+                        || isDigits(numeric.substring(1))
                         && (numeric.charAt(0) == '-' || Character.isDigit(numeric.charAt(0)))) {
                         try {
                             return createLong(numeric);

</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import java.math.BigDecimal;
import java.math.BigInteger;
import org.apache.commons.lang.StringUtils;
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
        if (!Character.isDigit(lastChar)) {
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
                        && isDigits(numeric.substring(1))
                        && (numeric.charAt(0) == '-' || Character.isDigit(numeric.charAt(0)))) {
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
                    //Fall through
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
                    //Fall through
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
message: 1l is not a valid number.
thrown_at: org.apache.commons.lang.math.NumberUtils.createNumber(NumberUtils.java:464)
anchor_input: "-1l"  // hard-code this verbatim as your first call, then fuzz inputs of the same shape
other_observed_literals: "01l", "1l"
</ground_truth_crash>
<failing_test class="org.apache.commons.lang.math.NumberUtilsTest" method="testLang300">
    public void testLang300() {
        NumberUtils.createNumber("-1l");
        NumberUtils.createNumber("01l");
        NumberUtils.createNumber("1l");
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.lang.math.NumberUtilsTest::testLang300
java.lang.NumberFormatException: 1l is not a valid number.
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  compare(double lhs, double rhs) / (float lhs, float rhs)
  equals(byte[] array1, byte[] array2) / (short[] array1, short[] array2) / (int[] array1, int[] array2) / (long[] array1, long[] array2) / (float[] array1, float[] array2) / (double[] array1, double[] array2)
  max(long[] array) / (int[] array) / (short[] array) / (byte[] array) / (double[] array) / (float[] array) / (long a, long b, long c) / (int a, int b, int c) / (short a, short b, short c) / (byte a, byte b, byte c) / (double a, double b, double c) / (float a, float b, float c)
  min(long[] array) / (int[] array) / (short[] array) / (byte[] array) / (double[] array) / (float[] array) / (long a, long b, long c) / (int a, int b, int c) / (short a, short b, short c) / (byte a, byte b, byte c) / (double a, double b, double c) / (float a, float b, float c)
  stringToInt(String str) / (String str, int defaultValue)
  toDouble(String str) / (String str, double defaultValue)
  toFloat(String str) / (String str, float defaultValue)
  toInt(String str) / (String str, int defaultValue)

METHOD FAMILIES over the same input space (factory/parser siblings — where the docs state a selection or agreement rule between family members, equivalent inputs must respect it):
  create* family: createBigDecimal,

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- StringUtils.isBlank
- NumberUtils.createInteger
- NumberUtils.isAllZeros
- NumberUtils.substring
- NumberUtils.isDigits
- NumberUtils.charAt
- NumberUtils.createLong
- NumberUtils.createBigInteger
- NumberUtils.createFloat
- NumberUtils.createDouble
- NumberUtils.createBigDecimal
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.lang.math.FuzzHarness.exerciseValidLongLiteral
- org.apache.commons.lang.math.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.lang.math.NumberUtils.createNumber
Crashes already found:
- java.lang.NumberFormatException@org.apache.commons.lang.math.NumberUtils.createNumber
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- StringUtils.isBlank
- NumberUtils.createInteger
- NumberUtils.isAllZeros
- NumberUtils.substring
- NumberUtils.isDigits
- NumberUtils.charAt
- NumberUtils.createLong
- NumberUtils.createBigInteger
- NumberUtils.createFloat
- NumberUtils.createDouble
- NumberUtils.createBigDecimal
Check FAMILIES already covered by accepted harnesses: {leading-zero-equivalence, long-suffix-helper}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.NumberFormatException@org.apache.commons.lang.math.NumberUtils.createNumber. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
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
package org.apache.commons.lang.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~6,604 chars)</summary>

```
package org.apache.commons.lang.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        callAnchor("-1l");
        callAnchor("01l");
        callAnchor("1l");

        char digit = (char) ('0' + data.consumeInt(0, 9));
        char suffix = data.consumeBoolean() ? 'l' : 'L';
        String singleDigitLiteral = String.valueOf(digit) + suffix;

        checkValidRequestedLongLiteral(singleDigitLiteral, String.valueOf(digit));

        long base = data.consumeInt(-1_000_000, 1_000_000);
        String numeric = Long.toString(base);
        if (data.consumeBoolean()) {
            int zeros = data.consumeInt(0, 4);
            if (numeric.startsWith("-")) {
                StringBuilder sb = new StringBuilder("-");
                for (int i = 0; i < zeros; i++) {
                    sb.append('0');
                }
                sb.append(numeric.substring(1));
                numeric = sb.toString();
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < zeros; i++) {
                    sb.append('0');
                }
                sb.append(numeric);
                numeric = sb.toString();
            }
        }
        String validLiteral = numeric + suffix;
        checkValidRequestedLongLiteral(validLiteral, numeric);

        String flippedBoundary = buildBoundaryCandidate(data);
        exerciseExplore(flippedBoundary);
    }

    private static void callAnchor(String s) {
        try {
            NumberUtils.createNumber(s);
        } catch (RuntimeException t) {
            if (isValidationLike(t)) {
                return;
            }
        }
    }

    private static void checkValidRequestedLongLiteral(String literal, String numeric) {
        Long expected;
        try {
            expected = NumberUtils.createLong(numeric);
        } catch (RuntimeException t) {
            return;
        }

        Number parsed;
        try {
            parsed = NumberUtils.createNumber(literal);
        } catch (RuntimeException t) {
            if (isValidationLike(t) && isFromCreateNumberRegion(t)) {
                throw new RuntimeException(
                    "[oracle:specific-long-type] metamorphic violation: valid long literal rejected input="
                        + literal + " numeric=" + numeric + " expected=" + expected, t);
            }
            return;
        }

        /* Contract from createNumber's code: suffix 'l'/'L' is "Requesting a specific type..";
           for a valid in-range integral literal with no decimal/exponent, a correct implementation
           must return the requested Long value, not silently reject it or return a different type.
           A throw-deleting patch that returns some other Number would violate this observable. */
        if (!(parsed instanceof Long)) {
            throw new RuntimeException(
                "[oracle:specific-long-type] metamorphic violation: requested long literal did not produce Long input="
                    + literal + " resultClass=" + parsed.getClass().getName() + " result=" + parsed);
        }
        if (((Long) parsed).longValue() != expected.longValue()) {
            throw new RuntimeException(
                "[oracle:specific-long-type] metamorphic violation: requested long literal changed numeric value input="
                    + literal + " lhs=" + parsed + " rhs=" + expected);
        }

        try {
            Number upper = NumberUtils.createNumber(numeric + "L");
            Number lower = NumberUtils.createNumber(numeric + "l");
            if (upper instanceof Long && lower instanceof Long) {
                if (((Long) upper).longValue() != ((Long) lower).longValue()) {
                    throw new RuntimeException(
                        "[oracle:suffix-case] metamorphic violation: l/L disagree input="
                            + numeric + " lhs=" + lower + " rhs=" + upper);
                }
            }
        } catch (RuntimeException t) {
            if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw t;
            }
        }
    }

    private static void exerciseExplore(String candidate) {
        try {
            NumberUtils.createNumber(candidate);
        } catch (RuntimeException t) {
            if (isValidationLike(t)) {
                return;
            }
        }
    }

    private static String buildBoundaryCandidate(FuzzedDataProvider data) {
        int mode = data.consumeInt(0, 5);
        char suffix = data.consumeBoolean() ? 'l' : 'L';
        switch (mode) {
            case 0:
                return String.valueOf((char) ('0' + data.consumeInt(0, 9))) + suffix;
            case 1:
                return "-" + (char) ('0' + data.consumeInt(0, 9)) + suffix;
            case 2:
                return data.consumeAsciiString(3) + suffix;
            case 3:
                return (char) ('0' + data.consumeInt(0, 9)) + data.consumeAsciiString(2) + suffix;
            case 4:
                return data.consumeBoolean() ? "." + suffix : "e" + suffix;
            default:
                return data.consumeRemainingAsString() + suffix;
        }
    }

    private static boolean isValidationLike(Throwable t) {
        return t instanceof IllegalArgumentException;
    }

    private static boolean isFromCreateNumberRegion(Throwable t) {
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String method = ste.getMethodName();
            if ("org.apache.commons.lang.math.NumberUtils".equals(cls)) {
                if ("createNumber".equals(method)
                    || "createInteger".equals(method)
                    || "isAllZeros".equals(method)
                    || "isDigits".equals(method)
                    || "createLong".equals(method)
                    || "createBigInteger".equals(method)
                    || "createFloat".equals(method)
                    || "createDouble".equals(method)
                    || "createBigDecimal".equals(method)
                    || "substring".equals(method)
                    || "charAt".equals(method)) {
                    return true;
                }
            }
            if ("org.apache.commons.lang.StringUtils".equals(cls) && "isBlank".equals(method)) {
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
- trigger: java.lang.RuntimeException: [oracle:specific-long-type] metamorphic violation: valid long literal rejected input=0L numeric=0 expected=0

---
## [7] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~31,575 chars, 1 new)</summary>

- *[system] message: identical to step [3] — not reprinted*
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.apache.commons.lang.math` (`package org.apache.commons.lang.math;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Lang_58_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/java/org/apache/commons/lang/math/NumberUtils.java	
+++ /src/java/org/apache/commons/lang/math/NumberUtils.java
@@ -451,7 +451,7 @@
                 case 'L' :
                     if (dec == null
                         && exp == null
-                        && isDigits(numeric.substring(1))
+                        || isDigits(numeric.substring(1))
                         && (numeric.charAt(0) == '-' || Character.isDigit(numeric.charAt(0)))) {
                         try {
                             return createLong(numeric);

</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import java.math.BigDecimal;
import java.math.BigInteger;
import org.apache.commons.lang.StringUtils;
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
        if (!Character.isDigit(lastChar)) {
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
                        && isDigits(numeric.substring(1))
                        && (numeric.charAt(0) == '-' || Character.isDigit(numeric.charAt(0)))) {
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
                    //Fall through
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
                    //Fall through
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
message: 1l is not a valid number.
thrown_at: org.apache.commons.lang.math.NumberUtils.createNumber(NumberUtils.java:464)
anchor_input: "-1l"  // hard-code this verbatim as your first call, then fuzz inputs of the same shape
other_observed_literals: "01l", "1l"
</ground_truth_crash>
<failing_test class="org.apache.commons.lang.math.NumberUtilsTest" method="testLang300">
    public void testLang300() {
        NumberUtils.createNumber("-1l");
        NumberUtils.createNumber("01l");
        NumberUtils.createNumber("1l");
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.lang.math.NumberUtilsTest::testLang300
java.lang.NumberFormatException: 1l is not a valid number.
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  compare(double lhs, double rhs) / (float lhs, float rhs)
  equals(byte[] array1, byte[] array2) / (short[] array1, short[] array2) / (int[] array1, int[] array2) / (long[] array1, long[] array2) / (float[] array1, float[] array2) / (double[] array1, double[] array2)
  max(long[] array) / (int[] array) / (short[] array) / (byte[] array) / (double[] array) / (float[] array) / (long a, long b, long c) / (int a, int b, int c) / (short a, short b, short c) / (byte a, byte b, byte c) / (double a, double b, double c) / (float a, float b, float c)
  min(long[] array) / (int[] array) / (short[] array) / (byte[] array) / (double[] array) / (float[] array) / (long a, long b, long c) / (int a, int b, int c) / (short a, short b, short c) / (byte a, byte b, byte c) / (double a, double b, double c) / (float a, float b, float c)
  stringToInt(String str) / (String str, int defaultValue)
  toDouble(String str) / (String str, double defaultValue)
  toFloat(String str) / (String str, float defaultValue)
  toInt(String str) / (String str, int defaultValue)

METHOD FAMILIES over the same input space (factory/parser siblings — where the docs state a selection or agreement rule between family members, equivalent inputs must respect it):
  create* family: createBigDecimal,

This harness is ONE of a set probing the root cause of the vulnerability the patch under analysis is meant to fix. The patched lines sit at the head of the reachable region below. A valid sibling bug is one that:
  (a) lives in this region (same method or call graph), AND
  (b) stems from the SAME root cause
<root_cause_reachable>
- StringUtils.isBlank
- NumberUtils.createInteger
- NumberUtils.isAllZeros
- NumberUtils.substring
- NumberUtils.isDigits
- NumberUtils.charAt
- NumberUtils.createLong
- NumberUtils.createBigInteger
- NumberUtils.createFloat
- NumberUtils.createDouble
- NumberUtils.createBigDecimal
</root_cause_reachable>
Already covered by earlier harnesses — target something different:
Functions covered:
- org.apache.commons.lang.math.FuzzHarness.checkValidRequestedLongLiteral
- org.apache.commons.lang.math.FuzzHarness.exerciseValidLongLiteral
- org.apache.commons.lang.math.FuzzHarness.fuzzerTestOneInput
- org.apache.commons.lang.math.NumberUtils.createNumber
Crashes already found:
- java.lang.NumberFormatException@org.apache.commons.lang.math.NumberUtils.createNumber
- java.lang.RuntimeException@org.apache.commons.lang.math.FuzzHarness.checkValidRequestedLongLiteral
If the crash you plan to reproduce has the SAME signature as one already listed above, this harness must instead win through a post-condition / metamorphic assertion (see the MANDATORY check below) — re-triggering an already-found signature adds no new evidence, and a campaign of identical crash reproducers is blind to patches that merely delete the throw.
Uncovered functions to steer toward:
- StringUtils.isBlank
- NumberUtils.createInteger
- NumberUtils.isAllZeros
- NumberUtils.substring
- NumberUtils.isDigits
- NumberUtils.charAt
- NumberUtils.createLong
- NumberUtils.createBigInteger
- NumberUtils.createFloat
- NumberUtils.createDouble
- NumberUtils.createBigDecimal
Check FAMILIES already covered by accepted harnesses: {leading-zero-equivalence, long-suffix-helper, specific-long-type, suffix-case}. Your harness is REJECTED unless it fires at least one check OUTSIDE these families — cover a different observable, sibling method, or receiver-state axis.
INDEPENDENT ORACLE REQUIRED: the set has already found trigger(s): java.lang.NumberFormatException@org.apache.commons.lang.math.NumberUtils.createNumber; java.lang.RuntimeException@org.apache.commons.lang.math.FuzzHarness.checkValidRequestedLongLiteral. A band-aid patch silences exactly these known symptoms, so a harness that only re-checks them cannot tell a real fix from a cover-up. Include at least ONE additional check that would still fire if the known symptom disappeared but a related quantity stayed wrong (see the consistency checks below).
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
package org.apache.commons.lang.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~5,107 chars)</summary>

```
package org.apache.commons.lang.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Contract used for the oracle:
        // In NumberUtils.createNumber, when the last char is 'l'/'L' and the input is a valid
        // decimal integer with no decimal point or exponent, the implementation is supposed to
        // return createLong(numeric), or createBigInteger(numeric) only if the value is too big
        // for long. For the small literals we construct here, createLong and createInteger both
        // accept the same numeric string, so createNumber(literal) must accept too and must
        // represent the same numeric value. A patch that merely suppresses the throw or returns
        // an unrelated value will violate this relation.

        checkRequestedLongLiteral("-1l", "-1");
        checkRequestedLongLiteral("01l", "01");
        checkRequestedLongLiteral("1l", "1");

        int iterations = 1 + data.consumeInt(0, 8);
        for (int i = 0; i < iterations; i++) {
            String numeric = buildSingleDigitNumeric(data);
            char suffix = data.consumeBoolean() ? 'l' : 'L';
            checkRequestedLongLiteral(numeric + suffix, numeric);
        }

        try {
            NumberUtils.createNumber(data.consumeAsciiString(32));
        } catch (RuntimeException t) {
            if (isValidationFamily(t)) {
                return;
            }
        }
    }

    private static String buildSingleDigitNumeric(FuzzedDataProvider data) {
        int mode = data.consumeInt(0, 5);
        int digit = data.consumeInt(0, 9);
        char c = (char) ('0' + digit);
        switch (mode) {
            case 0:
                return String.valueOf(c);
            case 1:
                return "0" + c;
            case 2:
                return "00" + c;
            case 3:
                return digit == 0 ? "0" : String.valueOf(c);
            case 4:
                return digit == 0 ? "0" : "000" + c;
            default:
                return digit == 0 ? "-0" : "-" + c;
        }
    }

    private static void checkRequestedLongLiteral(String literal, String numeric) {
        Long expectedLong;
        Integer expectedInt;
        try {
            expectedLong = NumberUtils.createLong(numeric);
            expectedInt = NumberUtils.createInteger(numeric);
        } catch (RuntimeException t) {
            return;
        }

        if (expectedLong == null || expectedInt == null) {
            return;
        }
        if (expectedLong.longValue() != expectedInt.longValue()) {
            throw new RuntimeException("[oracle:int-long-agree] metamorphic violation: equivalent decimal parsers disagree input="
                    + numeric + " long=" + expectedLong + " int=" + expectedInt);
        }

        final Number actual;
        try {
            actual = NumberUtils.createNumber(literal);
        } catch (RuntimeException t) {
            if (isValidationFamily(t)) {
                throw new RuntimeException("[oracle:requested-long-value] metamorphic violation: valid requested-long literal rejected input="
                        + literal + " numeric=" + numeric + " expected=" + expectedLong, t);
            }
            return;
        }

        if (actual == null) {
            throw new RuntimeException("[oracle:requested-long-value] metamorphic violation: valid requested-long literal returned null input="
                    + literal + " numeric=" + numeric + " expected=" + expectedLong);
        }

        long actualValue = actual.longValue();
        long expectedValue = expectedLong.longValue();
        if (actualValue != expectedValue) {
            throw new RuntimeException("[oracle:requested-long-value] metamorphic violation: numeric value mismatch input="
                    + literal + " numeric=" + numeric + " expected=" + expectedValue + " actual=" + actualValue
                    + " actualClass=" + actual.getClass().getName());
        }

        try {
            Number reparsed = NumberUtils.createNumber(actual.toString());
            if (reparsed == null || reparsed.longValue() != expectedValue) {
                throw new RuntimeException("[oracle:roundtrip-createNumber] metamorphic violation: parse/toString/parse changed value input="
                        + literal + " numeric=" + numeric + " expected=" + expectedValue + " actual=" + actual
                        + " reparsed=" + reparsed);
            }
        } catch (RuntimeException t) {
            if (isValidationFamily(t)) {
                throw new RuntimeException("[oracle:roundtrip-createNumber] metamorphic violation: round-trip rejected canonical numeric form input="
                        + literal + " numeric=" + numeric + " actual=" + actual, t);
            }
        }
    }

    private static boolean isValidationFamily(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }
}
```

</details>

---
## [8] ⚙️ harness-attempt · `attempt_003`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.RuntimeException: [oracle:requested-long-value] metamorphic violation: valid requested-long literal rejected input=1l numeric=1 expected=1

---
## [9] ⚙️ corpus-seed · `attempt_001`
**output:** **25 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['-1l', '01l', '1l', '-1L', '-1', '-1F', '-1f', '-1D']}

---
## [10] ⚙️ corpus-seed · `attempt_002`
**output:** **25 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['-1l', '01l', '1l', '-1L', '-1', '-1F', '-1f', '-1D']}

---
## [11] ⚙️ corpus-seed · `attempt_003`
**output:** **25 literal-variation seeds into the patched-side fuzz corpus**
- detail: {'sample': ['-1l', '01l', '1l', '-1L', '-1', '-1F', '-1f', '-1D']}

---
## [12] ⚙️ patched-fuzz · `attempt_001`
**output:** **quiet on patched build (no overfit signal)**

---
## [13] ⚙️ patched-fuzz · `attempt_002`
**output:** **quiet on patched build (no overfit signal)**

---
## [14] ⚙️ patched-fuzz · `attempt_003`
**output:** **quiet on patched build (no overfit signal)**
