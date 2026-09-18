# Pipeline trace — Lang-24

**Code:** `1ac28e12b4447c6ad51717ff73beb5695a24b71a`

**Patch label:** correct  *(the patch under analysis is a known-CORRECT fix — the pipeline is not told this)*

**Outcome:** candidate_experiment (buggy build only)

**Patch under analysis:**
```diff
--- /src/main/java/org/apache/commons/lang3/math/NumberUtils.java
+++ /src/main/java/org/apache/commons/lang3/math/NumberUtils.java
@@ -1409,6 +1409,9 @@
             }
             if (chars[i] == 'l'
                 || chars[i] == 'L') {
+//ACS's patch begin
+	if (hasDecPoint==true){return false;}
+//ACS's patch end
                 // not allowing L with an exponent or decimal point
                 return foundDigit && !hasExp;
             }
@@ -1420,4 +1423,4 @@
         return !allowSigns && foundDigit;
     }
     
-}
+}
\ No newline at end of file
```

98 sequential steps — 32 LLM calls, 66 deterministic. Read top to bottom.

<details><summary>Legend — what each step is</summary>

- **failing-tests-found** — the project's own tests that expose the bug (extracted from Defects4J)
- **analysis (TargetAnalyzer)** — parses the patch and builds the code context the model reasons over — the touched method(s), their documented contract, the call-graph reachable set + sibling members (via fuzz-introspector), and imports
- **rule synthesis (LLM)** — proposes candidate RELATIONS — invariants / metamorphic properties a correct implementation must satisfy — from the documented contract
- **screen-fuzz-buggy** — compiles each candidate rule and fuzzes it many times on the BUGGY build; the output is checked/violated/fire-ratio
- **screen** — the keep/drop decision for each rule (direction-confirmed, selective, silent, or dropped by a lint/compile/cap)
- **screening-survivors** — the final set of rules kept — these are passed to replay / harness generation
- **harness generation (LLM)** — writes a Jazzer fuzzing harness that embeds the surviving rules plus oracles lifted from the failing test
- **harness-attempt** — accept/reject of one generated harness — ACCEPTED = it compiles AND crashes the BUGGY build (with the triggering input shown)

</details>

*Viewing: every ▸ line is a click-to-expand toggle (VS Code markdown preview / GitHub). Prompts are collapsed by default; LLM outputs start expanded. The raw file stays fully greppable.*


---
## [0] ⚙️ failing-tests-found
<details><summary>▸ output — FailureTest(test_class='org.apache.commons.lang3.math.NumberUtilsTest', test_method='testIsNumber',  (~10,100 chars)</summary>

**output:** 
```json
[
  "FailureTest(test_class='org.apache.commons.lang3.math.NumberUtilsTest', test_method='testIsNumber', source_path='/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/Lang_24_buggy/src/test/java/org/apache/commons/lang3/math/NumberUtilsTest.java', method_source='    public void testIsNumber() {\\n        String val = \"12345\";\\n        assertTrue(\"isNumber(String) 1 failed\", NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 1 failed\", checkCreateNumber(val));\\n        val = \"1234.5\";\\n        assertTrue(\"isNumber(String) 2 failed\", NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 2 failed\", checkCreateNumber(val));\\n        val = \".12345\";\\n        assertTrue(\"isNumber(String) 3 failed\", NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 3 failed\", checkCreateNumber(val));\\n        val = \"1234E5\";\\n        assertTrue(\"isNumber(String) 4 failed\", NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 4 failed\", checkCreateNumber(val));\\n        val = \"1234E+5\";\\n        assertTrue(\"isNumber(String) 5 failed\", NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 5 failed\", checkCreateNumber(val));\\n        val = \"1234E-5\";\\n        assertTrue(\"isNumber(String) 6 failed\", NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 6 failed\", checkCreateNumber(val));\\n        val = \"123.4E5\";\\n        assertTrue(\"isNumber(String) 7 failed\", NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 7 failed\", checkCreateNumber(val));\\n        val = \"-1234\";\\n        assertTrue(\"isNumber(String) 8 failed\", NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 8 failed\", checkCreateNumber(val));\\n        val = \"-1234.5\";\\n        assertTrue(\"isNumber(String) 9 failed\", NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 9 failed\", checkCreateNumber(val));\\n        val = \"-.12345\";\\n        assertTrue(\"isNumber(String) 10 failed\", NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 10 failed\", checkCreateNumber(val));\\n        val = \"-1234E5\";\\n        assertTrue(\"isNumber(String) 11 failed\", NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 11 failed\", checkCreateNumber(val));\\n        val = \"0\";\\n        assertTrue(\"isNumber(String) 12 failed\", NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 12 failed\", checkCreateNumber(val));\\n        val = \"-0\";\\n        assertTrue(\"isNumber(String) 13 failed\", NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 13 failed\", checkCreateNumber(val));\\n        val = \"01234\";\\n        assertTrue(\"isNumber(String) 14 failed\", NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 14 failed\", checkCreateNumber(val));\\n        val = \"-01234\";\\n        assertTrue(\"isNumber(String) 15 failed\", NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 15 failed\", checkCreateNumber(val));\\n        val = \"0xABC123\";\\n        assertTrue(\"isNumber(String) 16 failed\", NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 16 failed\", checkCreateNumber(val));\\n        val = \"0x0\";\\n        assertTrue(\"isNumber(String) 17 failed\", NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 17 failed\", checkCreateNumber(val));\\n        val = \"123.4E21D\";\\n        assertTrue(\"isNumber(String) 19 failed\", NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 19 failed\", checkCreateNumber(val));\\n        val = \"-221.23F\";\\n        assertTrue(\"isNumber(String) 20 failed\", NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 20 failed\", checkCreateNumber(val));\\n        val = \"22338L\";\\n        assertTrue(\"isNumber(String) 21 failed\", NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 21 failed\", checkCreateNumber(val));\\n        val = null;\\n        assertTrue(\"isNumber(String) 1 Neg failed\", !NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 1 Neg failed\", !checkCreateNumber(val));\\n        val = \"\";\\n        assertTrue(\"isNumber(String) 2 Neg failed\", !NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 2 Neg failed\", !checkCreateNumber(val));\\n        val = \"--2.3\";\\n        assertTrue(\"isNumber(String) 3 Neg failed\", !NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 3 Neg failed\", !checkCreateNumber(val));\\n        val = \".12.3\";\\n        assertTrue(\"isNumber(String) 4 Neg failed\", !NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 4 Neg failed\", !checkCreateNumber(val));\\n        val = \"-123E\";\\n        assertTrue(\"isNumber(String) 5 Neg failed\", !NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 5 Neg failed\", !checkCreateNumber(val));\\n        val = \"-123E+-212\";\\n        assertTrue(\"isNumber(String) 6 Neg failed\", !NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 6 Neg failed\", !checkCreateNumber(val));\\n        val = \"-123E2.12\";\\n        assertTrue(\"isNumber(String) 7 Neg failed\", !NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 7 Neg failed\", !checkCreateNumber(val));\\n        val = \"0xGF\";\\n        assertTrue(\"isNumber(String) 8 Neg failed\", !NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 8 Neg failed\", !checkCreateNumber(val));\\n        val = \"0xFAE-1\";\\n        assertTrue(\"isNumber(String) 9 Neg failed\", !NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 9 Neg failed\", !checkCreateNumber(val));\\n        val = \".\";\\n        assertTrue(\"isNumber(String) 10 Neg failed\", !NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 10 Neg failed\", !checkCreateNumber(val));\\n        val = \"-0ABC123\";\\n        assertTrue(\"isNumber(String) 11 Neg failed\", !NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 11 Neg failed\", !checkCreateNumber(val));\\n        val = \"123.4E-D\";\\n        assertTrue(\"isNumber(String) 12 Neg failed\", !NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 12 Neg failed\", !checkCreateNumber(val));\\n        val = \"123.4ED\";\\n        assertTrue(\"isNumber(String) 13 Neg failed\", !NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 13 Neg failed\", !checkCreateNumber(val));\\n        val = \"1234E5l\";\\n        assertTrue(\"isNumber(String) 14 Neg failed\", !NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 14 Neg failed\", !checkCreateNumber(val));\\n        val = \"11a\";\\n        assertTrue(\"isNumber(String) 15 Neg failed\", !NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 15 Neg failed\", !checkCreateNumber(val)); \\n        val = \"1a\";\\n        assertTrue(\"isNumber(String) 16 Neg failed\", !NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 16 Neg failed\", !checkCreateNumber(val)); \\n        val = \"a\";\\n        assertTrue(\"isNumber(String) 17 Neg failed\", !NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 17 Neg failed\", !checkCreateNumber(val)); \\n        val = \"11g\";\\n        assertTrue(\"isNumber(String) 18 Neg failed\", !NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 18 Neg failed\", !checkCreateNumber(val)); \\n        val = \"11z\";\\n        assertTrue(\"isNumber(String) 19 Neg failed\", !NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 19 Neg failed\", !checkCreateNumber(val)); \\n        val = \"11def\";\\n        assertTrue(\"isNumber(String) 20 Neg failed\", !NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 20 Neg failed\", !checkCreateNumber(val)); \\n        val = \"11d11\";\\n        assertTrue(\"isNumber(String) 21 Neg failed\", !NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 21 Neg failed\", !checkCreateNumber(val)); \\n        val = \"11 11\";\\n        assertTrue(\"isNumber(String) 22 Neg failed\", !NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 22 Neg failed\", !checkCreateNumber(val));\\n        val = \" 1111\";\\n        assertTrue(\"isNumber(String) 23 Neg failed\", !NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 23 Neg failed\", !checkCreateNumber(val));\\n        val = \"1111 \";\\n        assertTrue(\"isNumber(String) 24 Neg failed\", !NumberUtils.isNumber(val));\\n        assertTrue(\"isNumber(String)/createNumber(String) 24 Neg failed\", !checkCreateNumber(val));\\n\\n        // LANG-521\\n        val = \"2.\";\\n        assertTrue(\"isNumber(String) LANG-521 failed\", NumberUtils.isNumber(val));\\n\\n        // LANG-664\\n        val = \"1.1L\";\\n        assertFalse(\"isNumber(String) LANG-664 failed\", NumberUtils.isNumber(val));\\n    }', exception_type='junit.framework.AssertionFailedError', failure_message=None, support_source=None)"
]
```

</details>

---
## [1] ⚙️ test-context (H1/H2)
**output:** 
```json
[
  {
    "test": "org.apache.commons.lang3.math.NumberUtilsTest::testIsNumber",
    "failure_message": "--- org.apache.commons.lang3.math.NumberUtilsTest::testIsNumber\njunit.framework.AssertionFailedError: isNumber(String) LANG-664 failed",
    "support_chars": 344
  }
]
```

---
## [2] ⚙️ analysis (TargetAnalyzer)
<details><summary>▸ output — modified_files": [ (~6,687 chars)</summary>

**output:** 
```json
{
  "modified_files": [
    "src/main/java/org/apache/commons/lang3/math/NumberUtils.java"
  ],
  "patch_text": "--- /src/main/java/org/apache/commons/lang3/math/NumberUtils.java\n+++ /src/main/java/org/apache/commons/lang3/math/NumberUtils.java\n@@ -1409,6 +1409,9 @@\n             }\n             if (chars[i] == 'l'\n                 || chars[i] == 'L') {\n+//ACS's patch begin\n+\tif (hasDecPoint==true){return false;}\n+//ACS's patch end\n                 // not allowing L with an exponent or decimal point\n                 return foundDigit && !hasExp;\n             }\n@@ -1420,4 +1423,4 @@\n         return !allowSigns && foundDigit;\n     }\n     \n-}\n+}\n\\ No newline at end of file\n",
  "functions": [
    {
      "func_name": "isNumber",
      "func_signature": "public static boolean isNumber(String str)",
      "func_source": "    public static boolean isNumber(String str) {\n        if (StringUtils.isEmpty(str)) {\n            return false;\n        }\n        char[] chars = str.toCharArray();\n        int sz = chars.length;\n        boolean hasExp = false;\n        boolean hasDecPoint = false;\n        boolean allowSigns = false;\n        boolean foundDigit = false;\n        // deal with any possible sign up front\n        int start = (chars[0] == '-') ? 1 : 0;\n        if (sz > start + 1) {\n            if (chars[start] == '0' && chars[start + 1] == 'x') {\n                int i = start + 2;\n                if (i == sz) {\n                    return false; // str == \"0x\"\n                }\n                // checking hex (it can't be anything else)\n                for (; i < chars.length; i++) {\n                    if ((chars[i] < '0' || chars[i] > '9')\n                        && (chars[i] < 'a' || chars[i] > 'f')\n                        && (chars[i] < 'A' || chars[i] > 'F')) {\n                        return false;\n                    }\n                }\n                return true;\n            }\n        }\n        sz--; // don't want to loop to the last char, check it afterwords\n              // for type qualifiers\n        int i = start;\n        // loop to the next to last char or to the last char if we need another digit to\n        // make a valid number (e.g. chars[0..5] = \"1234E\")\n        while (i < sz || (i < sz + 1 && allowSigns && !foundDigit)) {\n            if (chars[i] >= '0' && chars[i] <= '9') {\n                foundDigit = true;\n                allowSigns = false;\n\n            } else if (chars[i] == '.') {\n                if (hasDecPoint || hasExp) {\n                    // two decimal points or dec in exponent   \n                    return false;\n                }\n                hasDecPoint = true;\n            } else if (chars[i] == 'e' || chars[i] == 'E') {\n                // we've already taken care of hex.\n                if (hasExp) {\n                    // two E's\n                    return false;\n                }\n                if (!foundDigit) {\n                    return false;\n                }\n                hasExp = true;\n                allowSigns = true;\n            } else if (chars[i] == '+' || chars[i] == '-') {\n                if (!allowSigns) {\n                    return false;\n                }\n                allowSigns = false;\n                foundDigit = false; // we need a digit after the E\n            } else {\n                return false;\n            }\n            i++;\n        }\n        if (i < chars.length) {\n            if (chars[i] >= '0' && chars[i] <= '9') {\n                // no type qualifier, OK\n                return true;\n            }\n            if (chars[i] == 'e' || chars[i] == 'E') {\n                // can't have an E at the last byte\n                return false;\n            }\n            if (chars[i] == '.') {\n                if (hasDecPoint || hasExp) {\n                    // two decimal points or dec in exponent\n                    return false;\n                }\n                // single trailing decimal point after non-exponent is ok\n                return foundDigit;\n            }\n            if (!allowSigns\n                && (chars[i] == 'd'\n                    || chars[i] == 'D'\n                    || chars[i] == 'f'\n                    || chars[i] == 'F')) {\n                return foundDigit;\n            }\n            if (chars[i] == 'l'\n                || chars[i] == 'L') {\n                // not allowing L with an exponent or decimal point\n                return foundDigit && !hasExp;\n            }\n            // last character is illegal\n            return false;\n        }\n        // allowSigns is true iff the val ends in 'E'\n        // found digit it to make sure weird stuff like '.' and '1E-' doesn't pass\n        return !allowSigns && foundDigit;\n    }",
      "func_class": "NumberUtils",
      "func_class_fq": "org.apache.commons.lang3.math.NumberUtils",
      "func_param_types": [
        "String"
      ],
      "fi_name": "[org.apache.commons.lang3.math.NumberUtils].isNumber(String)",
      "overload_types": [
        [
          "String"
        ]
      ],
      "xrefs": [],
      "xref_names": [],
      "reachable_edges": [
        [
          "[org.apache.commons.lang3.math.NumberUtils].isNumber(String)",
          "[org.apache.commons.lang3.StringUtils].isEmpty(String)"
        ],
        [
          "[org.apache.commons.lang3.math.NumberUtils].isNumber(String)",
          "[String].toCharArray()"
        ]
      ],
      "reachable_depth": {
        "[org.apache.commons.lang3.math.NumberUtils].isNumber(String)": 0,
        "[org.apache.commons.lang3.StringUtils].isEmpty(String)": 1,
        "[String].toCharArray()": 1
      },
      "reachable": [
        "[org.apache.commons.lang3.StringUtils].isEmpty(String)",
        "[String].toCharArray()"
      ],
      "related_callees": [],
      "field_siblings": []
    }
  ],
  "package": "org.apache.commons.lang3.math",
  "root_cause_reachable": [
    "StringUtils.isEmpty"
  ],
  "neighbourhood_notes": [],
  "source_imports": [
    "import java.math.BigDecimal;",
    "import java.math.BigInteger;",
    "import org.apache.commons.lang3.StringUtils;"
  ],
  "reachable_edges": [
    [
      "[org.apache.commons.lang3.math.NumberUtils].isNumber(String)",
      "[org.apache.commons.lang3.StringUtils].isEmpty(String)"
    ],
    [
      "[org.apache.commons.lang3.math.NumberUtils].isNumber(String)",
      "[String].toCharArray()"
    ]
  ],
  "reachable_depth": {
    "[org.apache.commons.lang3.math.NumberUtils].isNumber(String)": 0,
    "[org.apache.commons.lang3.StringUtils].isEmpty(String)": 1,
    "[String].toCharArray()": 1
  }
}
```

</details>

---
## [3] ⚙️ synth · `probe-tier`
<details><summary>▸ output — documented exceptions: {'NumberUtils#createBigDecimal': ['NumberFormatException'], 'NumberUtils#crea (~521 chars)</summary>

**output:** 
```
documented exceptions: {'NumberUtils#createBigDecimal': ['NumberFormatException'], 'NumberUtils#createBigInteger': ['NumberFormatException'], 'NumberUtils#createDouble': ['NumberFormatException'], 'NumberUtils#createFloat': ['NumberFormatException'], 'NumberUtils#createInteger': ['NumberFormatException'], 'NumberUtils#createLong': ['NumberFormatException'], 'NumberUtils#createNumber': ['NumberFormatException'], 'NumberUtils#max': ['IllegalArgumentException'], 'NumberUtils#min': ['IllegalArgumentException']}
```

</details>
<details><summary>▸ detail (~505 chars)</summary>

- detail: {'documented': {'NumberUtils#createBigDecimal': ['NumberFormatException'], 'NumberUtils#createBigInteger': ['NumberFormatException'], 'NumberUtils#createDouble': ['NumberFormatException'], 'NumberUtils#createFloat': ['NumberFormatException'], 'NumberUtils#createInteger': ['NumberFormatException'], 'NumberUtils#createLong': ['NumberFormatException'], 'NumberUtils#createNumber': ['NumberFormatException'], 'NumberUtils#max': ['IllegalArgumentException'], 'NumberUtils#min': ['IllegalArgumentException']}}

</details>

---
## [4] 🧠 LLM call — **rule synthesis** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~58,217 chars)</summary>

**[system]**
```
You are a software-verification expert. Given a patched Java method and its API, you propose RELATIONS that must hold for EVERY correct implementation — never merely for the reference or for the tested inputs. Two kinds only:
  * METAMORPHIC: two real API calls whose results must agree (or relate in a fixed way) for any valid input — e.g. parse(format(x)) == x; f(a,b) == f(b,a) when documented commutative; reduce(reduce(x)) == reduce(x).
  * INVARIANT: a property the output must satisfy for every valid input, read straight off the documented contract — e.g. a reduced fraction has gcd(numerator,denominator)==1; a returned index is within bounds.
Each relation must be SOUND: if any correct implementation could violate it, do not propose it. Avoid comparisons to a different library, and avoid boundary-fragile claims (division by zero, NaN, overflow, empty/null) unless you explicitly exclude those inputs.
```
**[user]**
```
THE BUG'S OWN FAILING TEST (most trusted — this is ground truth for the correct behaviour on its inputs). The patch under analysis MAY be an overfit; where this test pins a direction, the TEST is right and the patched code is not evidence. Read the exact call it makes and the value it expects, and make your relations agree with it:
<failing_test>
// org.apache.commons.lang3.math.NumberUtilsTest::testIsNumber
    public void testIsNumber() {
        String val = "12345";
        assertTrue("isNumber(String) 1 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 1 failed", checkCreateNumber(val));
        val = "1234.5";
        assertTrue("isNumber(String) 2 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 2 failed", checkCreateNumber(val));
        val = ".12345";
        assertTrue("isNumber(String) 3 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 3 failed", checkCreateNumber(val));
        val = "1234E5";
        assertTrue("isNumber(String) 4 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 4 failed", checkCreateNumber(val));
        val = "1234E+5";
        assertTrue("isNumber(String) 5 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 5 failed", checkCreateNumber(val));
        val = "1234E-5";
        assertTrue("isNumber(String) 6 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 6 failed", checkCreateNumber(val));
        val = "123.4E5";
        assertTrue("isNumber(String) 7 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 7 failed", checkCreateNumber(val));
        val = "-1234";
        assertTrue("isNumber(String) 8 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 8 failed", checkCreateNumber(val));
        val = "-1234.5";
        assertTrue("isNumber(String) 9 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 9 failed", checkCreateNumber(val));
        val = "-.12345";
        assertTrue("isNumber(String) 10 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 10 failed", checkCreateNumber(val));
        val = "-1234E5";
        assertTrue("isNumber(String) 11 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 11 failed", checkCreateNumber(val));
        val = "0";
        assertTrue("isNumber(String) 12 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 12 failed", checkCreateNumber(val));
        val = "-0";
        assertTrue("isNumber(String) 13 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 13 failed", checkCreateNumber(val));
        val = "01234";
        assertTrue("isNumber(String) 14 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 14 failed", checkCreateNumber(val));
        val = "-01234";
        assertTrue("isNumber(String) 15 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 15 failed", checkCreateNumber(val));
        val = "0xABC123";
        assertTrue("isNumber(String) 16 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 16 failed", checkCreateNumber(val));
        val = "0x0";
        assertTrue("isNumber(String) 17 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 17 failed", checkCreateNumber(val));
        val = "123.4E21D";
        assertTrue("isNumber(String) 19 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 19 failed", checkCreateNumber(val));
        val = "-221.23F";
        assertTrue("isNumber(String) 20 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 20 failed", checkCreateNumber(val));
        val = "22338L";
        assertTrue("isNumber(String) 21 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 21 failed", checkCreateNumber(val));
        val = null;
        assertTrue("isNumber(String) 1 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 1 Neg failed", !checkCreateNumber(val));
        val = "";
        assertTrue("isNumber(String) 2 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 2 Neg failed", !checkCreateNumber(val));
        val = "--2.3";
        assertTrue("isNumber(String) 3 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 3 Neg failed", !checkCreateNumber(val));
        val = ".12.3";
        assertTrue("isNumber(String) 4 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 4 Neg failed", !checkCreateNumber(val));
        val = "-123E";
        assertTrue("isNumber(String) 5 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 5 Neg failed", !checkCreateNumber(val));
        val = "-123E+-212";
        assertTrue("isNumber(String) 6 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 6 Neg failed", !checkCreateNumber(val));
        val = "-123E2.12";
        assertTrue("isNumber(String) 7 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 7 Neg failed", !checkCreateNumber(val));
        val = "0xGF";
        assertTrue("isNumber(String) 8 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 8 Neg failed", !checkCreateNumber(val));
        val = "0xFAE-1";
        assertTrue("isNumber(String) 9 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 9 Neg failed", !checkCreateNumber(val));
        val = ".";
        assertTrue("isNumber(String) 10 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 10 Neg failed", !checkCreateNumber(val));
        val = "-0ABC123";
        assertTrue("isNumber(String) 11 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 11 Neg failed", !checkCreateNumber(val));
        val = "123.4E-D";
        assertTrue("isNumber(String) 12 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 12 Neg failed", !checkCreateNumber(val));
        val = "123.4ED";
        assertTrue("isNumber(String) 13 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 13 Neg failed", !checkCreateNumber(val));
        val = "1234E5l";
        assertTrue("isNumber(String) 14 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 14 Neg failed", !checkCreateNumber(val));
        val = "11a";
        assertTrue("isNumber(String) 15 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 15 Neg failed", !checkCreateNumber(val)); 
        val = "1a";
        assertTrue("isNumber(String) 16 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 16 Neg failed", !checkCreateNumber(val)); 
        val = "a";
        assertTrue("isNumber(String) 17 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 17 Neg failed", !checkCreateNumber(val)); 
        val = "11g";
        assertTrue("isNumber(String) 18 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 18 Neg failed", !checkCreateNumber(val)); 
        val = "11z";
        assertTrue("isNumber(String) 19 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 19 Neg failed", !checkCreateNumber(val)); 
        val = "11def";
        assertTrue("isNumber(String) 20 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 20 Neg failed", !checkCreateNumber(val)); 
        val = "11d11";
        assertTrue("isNumber(String) 21 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 21 Neg failed", !checkCreateNumber(val)); 
        val = "11 11";
        assertTrue("isNumber(String) 22 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 22 Neg failed", !checkCreateNumber(val));
        val = " 1111";
        assertTrue("isNumber(String) 23 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 23 Neg failed", !checkCreateNumber(val));
        val = "1111 ";
        assertTrue("isNumber(String) 24 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 24 Neg failed", !checkCreateNumber(val));

        // LANG-521
        val = "2.";
        assertTrue("isNumber(String) LANG-521 failed", NumberUtils.isNumber(val));

        // LANG-664
        val = "1.1L";
        assertFalse("isNumber(String) LANG-664 failed", NumberUtils.isNumber(val));
    }
// values this test asserts as CORRECT (the patched code must reproduce these): isNumber(String) 1 faile, isNumber(String)/createNumber(String) 1 faile, isNumber(String) 2 faile, isNumber(String)/createNumber(String) 2 faile, isNumber(String) 3 faile, isNumber(String)/createNumber(String) 3 faile, isNumber(String) 4 faile, isNumber(String)/createNumber(String) 4 faile, isNumber(String) 5 faile, isNumber(String)/createNumber(String) 5 faile, isNumber(String) 6 faile, isNumber(String)/createNumber(String) 6 faile
// On the BUGGY build this test FAILS with (names the diverging observable + wrong value):
// --- org.apache.commons.lang3.math.NumberUtilsTest::testIsNumber
// junit.framework.AssertionFailedError: isNumber(String) LANG-664 failed
// The test DEPENDS on this setup from its test class (helpers, fields, fixtures). A relation that reconstructs the test's scenario must REPLICATE this setup exactly — a relation that cannot must assert only setup-independent properties (counts, kinds, contract formulas), or it will fire on every build including correct ones:
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
</failing_test>
BUGGY method(s) (PRE-PATCH source — this is the code WITH the bug; the failing test above shows where it is wrong). Use it to see the code shape and API, NEVER as a model of correct behaviour:
<code>
    public static boolean isNumber(String str) {
        if (StringUtils.isEmpty(str)) {
            return false;
        }
        char[] chars = str.toCharArray();
        int sz = chars.length;
        boolean hasExp = false;
        boolean hasDecPoint = false;
        boolean allowSigns = false;
        boolean foundDigit = false;
        // deal with any possible sign up front
        int start = (chars[0] == '-') ? 1 : 0;
        if (sz > start + 1) {
            if (chars[start] == '0' && chars[start + 1] == 'x') {
                int i = start + 2;
                if (i == sz) {
                    return false; // str == "0x"
                }
                // checking hex (it can't be anything else)
                for (; i < chars.length; i++) {
                    if ((chars[i] < '0' || chars[i] > '9')
                        && (chars[i] < 'a' || chars[i] > 'f')
                        && (chars[i] < 'A' || chars[i] > 'F')) {
                        return false;
                    }
                }
                return true;
            }
        }
        sz--; // don't want to loop to the last char, check it afterwords
              // for type qualifiers
        int i = start;
        // loop to the next to last char or to the last char if we need another digit to
        // make a valid number (e.g. chars[0..5] = "1234E")
        while (i < sz || (i < sz + 1 && allowSigns && !foundDigit)) {
            if (chars[i] >= '0' && chars[i] <= '9') {
                foundDigit = true;
                allowSigns = false;

            } else if (chars[i] == '.') {
                if (hasDecPoint || hasExp) {
                    // two decimal points or dec in exponent   
                    return false;
                }
                hasDecPoint = true;
            } else if (chars[i] == 'e' || chars[i] == 'E') {
                // we've already taken care of hex.
                if (hasExp) {
                    // two E's
                    return false;
                }
                if (!foundDigit) {
                    return false;
                }
                hasExp = true;
                allowSigns = true;
            } else if (chars[i] == '+' || chars[i] == '-') {
                if (!allowSigns) {
                    return false;
                }
                allowSigns = false;
                foundDigit = false; // we need a digit after the E
            } else {
                return false;
            }
            i++;
        }
        if (i < chars.length) {
            if (chars[i] >= '0' && chars[i] <= '9') {
                // no type qualifier, OK
                return true;
            }
            if (chars[i] == 'e' || chars[i] == 'E') {
                // can't have an E at the last byte
                return false;
            }
            if (chars[i] == '.') {
                if (hasDecPoint || hasExp) {
                    // two decimal points or dec in exponent
                    return false;
                }
                // single trailing decimal point after non-exponent is ok
                return foundDigit;
            }
            if (!allowSigns
                && (chars[i] == 'd'
                    || chars[i] == 'D'
                    || chars[i] == 'f'
                    || chars[i] == 'F')) {
                return foundDigit;
            }
            if (chars[i] == 'l'
                || chars[i] == 'L') {
                // not allowing L with an exponent or decimal point
                return foundDigit && !hasExp;
            }
            // last character is illegal
            return false;
        }
        // allowSigns is true iff the val ends in 'E'
        // found digit it to make sure weird stuff like '.' and '1E-' doesn't pass
        return !allowSigns && foundDigit;
    }
</code>
CLASS-LEVEL CONTEXT — the whole patched class (skeleton: contracts + signatures; full bodies only for patched methods), its supertypes, and key collaborators. The STRONGEST relations usually tie the patched method to something HERE rather than to itself: a sibling method documented to agree or complement it (two accessors that must round-trip; a pair documented to sum to a constant), a constructor-established field invariant the method must respect, or a class/method javadoc guarantee (never null, always positive, canonical order). Prefer such cross-member relations over properties of the patched method in isolation.
<class name="NumberUtils" role="patched">
     * <p>Provides extra functionality for Java Number classes.</p>
     * @author Apache Software Foundation
     * @author <a href="mailto:rand_mcneely@yahoo.com">Rand McNeely</a>
     * @author <a href="mailto:steve.downey@netfolio.com">Steve Downey</a>
     * @author Eric Pugh
     * @author Phil Steitz
     * @author Matthew Hawthorne
     * @author <a href="mailto:ggregory@seagullsw.com">Gary Gregory</a>
     * @author <a href="mailto:fredrik@westermarck.com">Fredrik Westermarck</a>
     * @since 2.0
     * @version $Id$
public class NumberUtils {
     * Reusable Long constant for zero.
    public static final Long LONG_ZERO = new Long(0L);
     * Reusable Long constant for one.
    public static final Long LONG_ONE = new Long(1L);
     * Reusable Long constant for minus one.
    public static final Long LONG_MINUS_ONE = new Long(-1L);
     * Reusable Integer constant for zero.
    public static final Integer INTEGER_ZERO = new Integer(0);
     * Reusable Integer constant for one.
    public static final Integer INTEGER_ONE = new Integer(1);
     * Reusable Integer constant for minus one.
    public static final Integer INTEGER_MINUS_ONE = new Integer(-1);
     * Reusable Short constant for zero.
    public static final Short SHORT_ZERO = new Short((short) 0);
     * Reusable Short constant for one.
    public static final Short SHORT_ONE = new Short((short) 1);
     * Reusable Short constant for minus one.
    public static final Short SHORT_MINUS_ONE = new Short((short) -1);
     * Reusable Byte constant for zero.
    public static final Byte BYTE_ZERO = Byte.valueOf((byte) 0);
     * Reusable Byte constant for one.
    public static final Byte BYTE_ONE = Byte.valueOf((byte) 1);
     * Reusable Byte constant for minus one.
    public static final Byte BYTE_MINUS_ONE = Byte.valueOf((byte) -1);
     * Reusable Double constant for zero.
    public static final Double DOUBLE_ZERO = new Double(0.0d);
     * Reusable Double constant for one.
    public static final Double DOUBLE_ONE = new Double(1.0d);
     * Reusable Double constant for minus one.
    public static final Double DOUBLE_MINUS_ONE = new Double(-1.0d);
     * Reusable Float constant for zero.
    public static final Float FLOAT_ZERO = new Float(0.0f);
     * Reusable Float constant for one.
    public static final Float FLOAT_ONE = new Float(1.0f);
     * Reusable Float constant for minus one.
    public static final Float FLOAT_MINUS_ONE = new Float(-1.0f);
     * <p><code>NumberUtils</code> instances should NOT be constructed in standard programming.
     * Instead, the class should be used as <code>NumberUtils.toInt("6");</code>.</p>
     * <p>This constructor is public to permit tools that require a JavaBean instance
     * to operate.</p>
    public NumberUtils() {
        super();
    }
     * <p>Convert a <code>String</code> to an <code>int</code>, returning
     * <code>zero</code> if the conversion fails.</p>
     * <p>If the string is <code>null</code>, <code>zero</code> is returned.</p>
     * <pre>
     * NumberUtils.toInt(null) = 0
     * NumberUtils.toInt("")   = 0
     * NumberUtils.toInt("1")  = 1
     * </pre>
     * @param str  the string to convert, may be null
     * @return the int represented by the string, or <code>zero</code> if
     * conversion fails
     * @since 2.1
    public static int toInt(String str) { … }
     * <p>Convert a <code>String</code> to an <code>int</code>, returning a
     * default value if the conversion fails.</p>
     * <p>If the string is <code>null</code>, the default value is returned.</p>
     * <pre>
     * NumberUtils.toInt(null, 1) = 1
     * NumberUtils.toInt("", 1)   = 1
     * NumberUtils.toInt("1", 0)  = 1
     * </pre>
     * @param str  the string to convert, may be null
     * @param defaultValue  the default value
     * @return the int represented by the string, or the default if conversion fails
     * @since 2.1
    public static int toInt(String str, int defaultValue) { … }
     * <p>Convert a <code>String</code> to a <code>long</code>, returning
     * <code>zero</code> if the conversion fails.</p>
     * <p>If the string is <code>null</code>, <code>zero</code> is returned.</p>
     * <pre>
     * NumberUtils.toLong(null) = 0L
     * NumberUtils.toLong("")   = 0L
     * NumberUtils.toLong("1")  = 1L
     * </pre>
     * @param str  the string to convert, may be null
     * @return the long represented by the string, or <code>0</code> if
     * conversion fails
     * @since 2.1
    public static long toLong(String str) { … }
     * <p>Convert a <code>String</code> to a <code>long</code>, returning a
     * default value if the conversion fails.</p>
     * <p>If the string is <code>null</code>, the default value is returned.</p>
     * <pre>
     * NumberUtils.toLong(null, 1L) = 1L
     * NumberUtils.toLong("", 1L)   = 1L
     * NumberUtils.toLong("1", 0L)  = 1L
     * </pre>
     * @param str  the string to convert, may be null
     * @param defaultValue  the default value
     * @return the long represented by the string, or the default if conversion fails
     * @since 2.1
    public static long toLong(String str, long defaultValue) { … }
     * <p>Convert a <code>String</code> to a <code>float</code>, returning
     * <code>0.0f</code> if the conversion fails.</p>
     * <p>If the string <code>str</code> is <code>null</code>,
     * <code>0.0f</code> is returned.</p>
     * <pre>
     * NumberUtils.toFloat(null)   = 0.0f
     * NumberUtils.toFloat("")     = 0.0f
     * NumberUtils.toFloat("1.5")  = 1.5f
     * </pre>
     * @param str the string to convert, may be <code>null</code>
     * @return the float represented by the string, or <code>0.0f</code>
     * if conversion fails
     * …
    public static float toFloat(String str) { … }
     * <p>Convert a <code>String</code> to a <code>float</code>, returning a
     * default value if the conversion fails.</p>
     * <p>If the string <code>str</code> is <code>null</code>, the default
     * value is returned.</p>
     * <pre>
     * NumberUtils.toFloat(null, 1.1f)   = 1.0f
     * NumberUtils.toFloat("", 1.1f)     = 1.1f
     * NumberUtils.toFloat("1.5", 0.0f)  = 1.5f
     * </pre>
     * @param str the string to convert, may be <code>null</code>
     * @param defaultValue the default value
     * @return the float represented by the string, or defaultValue
     * …
    public static float toFloat(String str, float defaultValue) { … }
     * <p>Convert a <code>String</code> to a <code>double</code>, returning
     * <code>0.0d</code> if the conversion fails.</p>
     * <p>If the string <code>str</code> is <code>null</code>,
     * <code>0.0d</code> is returned.</p>
     * <pre>
     * NumberUtils.toDouble(null)   = 0.0d
     * NumberUtils.toDouble("")     = 0.0d
     * NumberUtils.toDouble("1.5")  = 1.5d
     * </pre>
     * @param str the string to convert, may be <code>null</code>
     * @return the double represented by the string, or <code>0.0d</code>
     * if conversion fails
     * …
    public static double toDouble(String str) { … }
     * <p>Convert a <code>String</code> to a <code>double</code>, returning a
     * default value if the conversion fails.</p>
     * <p>If the string <code>str</code> is <code>null</code>, the default
     * value is returned.</p>
     * <pre>
     * NumberUtils.toDouble(null, 1.1d)   = 1.1d
     * NumberUtils.toDouble("", 1.1d)     = 1.1d
     * NumberUtils.toDouble("1.5", 0.0d)  = 1.5d
     * </pre>
     * @param str the string to convert, may be <code>null</code>
     * @param defaultValue the default value
     * @return the double represented by the string, or defaultValue
     * …
    public static double toDouble(String str, double defaultValue) { … }
     * <p>Convert a <code>String</code> to a <code>byte</code>, returning
     * <code>zero</code> if the conversion fails.</p>
     * <p>If the string is <code>null</code>, <code>zero</code> is returned.</p>
     * <pre>
     * NumberUtils.toByte(null) = 0
     * NumberUtils.toByte("")   = 0
     * NumberUtils.toByte("1")  = 1
     * </pre>
     * @param str  the string to convert, may be null
     * @return the byte represented by the string, or <code>zero</code> if
     * conversion fails
     * @since 2.5
    public static byte toByte(String str) { … }
     * <p>Convert a <code>String</code> to a <code>byte</code>, returning a
     * default value if the conversion fails.</p>
     * <p>If the string is <code>null</code>, the default value is returned.</p>
     * <pre>
     * NumberUtils.toByte(null, 1) = 1
     * NumberUtils.toByte("", 1)   = 1
     * NumberUtils.toByte("1", 0)  = 1
     * </pre>
     * @param str  the string to convert, may be null
     * @param defaultValue  the default value
     * @return the byte represented by the string, or the default if conversion fails
     * @since 2.5
    public static byte toByte(String str, byte defaultValue) { … }
     * <p>Convert a <code>String</code> to a <code>short</code>, returning
     * <code>zero</code> if the conversion fails.</p>
     * <p>If the string is <code>null</code>, <code>zero</code> is returned.</p>
     * <pre>
     * NumberUtils.toShort(null) = 0
     * NumberUtils.toShort("")   = 0
     * NumberUtils.toShort("1")  = 1
     * </pre>
     * @param str  the string to convert, may be null
     * @return the short represented by the string, or <code>zero</code> if
     * conversion fails
     * @since 2.5
    public static short toShort(String str) { … }
     * <p>Convert a <code>String</code> to an <code>short</code>, returning a
     * default value if the conversion fails.</p>
     * <p>If the string is <code>null</code>, the default value is returned.</p>
     * <pre>
     * NumberUtils.toShort(null, 1) = 1
     * NumberUtils.toShort("", 1)   = 1
     * NumberUtils.toShort("1", 0)  = 1
     * </pre>
     * @param str  the string to convert, may be null
     * @param defaultValue  the default value
     * @return the short represented by the string, or the default if conversion fails
     * @since 2.5
    public static short toShort(String str, short defaultValue) { … }
}
</class>
<class name="IEEE754rUtils" role="collaborator">
     * <p>Provides IEEE-754r variants of NumberUtils methods. </p>
     * <p>See: <a href="http://en.wikipedia.org/wiki/IEEE_754r">http://en.wikipedia.org/wiki/IEEE_754r</a></p>
     * @since 2.4
     * @author Apache Software Foundation
     * @version $Id$
public class IEEE754rUtils {
     * <p>Returns the minimum value in an array.</p>
     * @param array  an array, must not be null or empty
     * @return the minimum value in the array
     * @throws IllegalArgumentException if <code>array</code> is <code>null</code>
     * @throws IllegalArgumentException if <code>array</code> is empty
    public static double min(double[] array) { … }
     * <p>Returns the minimum value in an array.</p>
     * @param array  an array, must not be null or empty
     * @return the minimum value in the array
     * @throws IllegalArgumentException if <code>array</code> is <code>null</code>
     * @throws IllegalArgumentException if <code>array</code> is empty
    public static float min(float[] array) { … }
     * <p>Gets the minimum of three <code>double</code> values.</p>
     * <p>NaN is only returned if all numbers are NaN as per IEEE-754r. </p>
     * @param a  value 1
     * @param b  value 2
     * @param c  value 3
     * @return  the smallest of the values
    public static double min(double a, double b, double c) { … }
     * <p>Gets the minimum of two <code>double</code> values.</p>
     * <p>NaN is only returned if all numbers are NaN as per IEEE-754r. </p>
     * @param a  value 1
     * @param b  value 2
     * @return  the smallest of the values
    public static double min(double a, double b) { … }
     * <p>Gets the minimum of three <code>float</code> values.</p>
     * <p>NaN is only returned if all numbers are NaN as per IEEE-754r. </p>
     * @param a  value 1
     * @param b  value 2
     * @param c  value 3
     * @return  the smallest of the values
    public static float min(float a, float b, float c) { … }
     * <p>Gets the minimum of two <code>float</code> values.</p>
     * <p>NaN is only returned if all numbers are NaN as per IEEE-754r. </p>
     * @param a  value 1
     * @param b  value 2
     * @return  the smallest of the values
    public static float min(float a, float b) { … }
     * <p>Returns the maximum value in an array.</p>
     * @param array  an array, must not be null or empty
     * @return the minimum value in the array
     * @throws IllegalArgumentException if <code>array</code> is <code>null</code>
     * @throws IllegalArgumentException if <code>array</code> is empty
    public static double max(double[] array) { … }
     * <p>Returns the maximum value in an array.</p>
     * @param array  an array, must not be null or empty
     * @return the minimum value in the array
     * @throws IllegalArgumentException if <code>array</code> is <code>null</code>
     * @throws IllegalArgumentException if <code>array</code> is empty
    public static float max(float[] array) { … }
     * <p>Gets the maximum of three <code>double</code> values.</p>
     * <p>NaN is only returned if all numbers are NaN as per IEEE-754r. </p>
     * @param a  value 1
     * @param b  value 2
     * @param c  value 3
     * @return  the largest of the values
    public static double max(double a, double b, double c) { … }
     * <p>Gets the maximum of two <code>double</code> values.</p>
     * <p>NaN is only returned if all numbers are NaN as per IEEE-754r. </p>
     * @param a  value 1
     * @param b  value 2
     * @return  the largest of the values
    public static double max(double a, double b) { … }
     * <p>Gets the maximum of three <code>float</code> values.</p>
     * <p>NaN is only returned if all numbers are NaN as per IEEE-754r. </p>
     * @param a  value 1
     * @param b  value 2
     * @param c  value 3
     * @return  the largest of the values
    public static float max(float a, float b, float c) { … }
     * <p>Gets the maximum of two <code>float</code> values.</p>
     * <p>NaN is only returned if all numbers are NaN as per IEEE-754r. </p>
     * @param a  value 1
     * @param b  value 2
     * @return  the largest of the values
    public static float max(float a, float b) { … }
}
</class>
<class name="StringUtils" role="collaborator">
public class StringUtils {
     * The empty String <code>""</code>.
     * @since 2.0
    public static final String EMPTY = "";
     * Represents a failed index search.
     * @since 2.1
    public static final int INDEX_NOT_FOUND = -1;
     * <p>The maximum size to which the padding constant(s) can expand.</p>
    private static final int PAD_LIMIT = 8192;
     * A regex pattern for recognizing blocks of whitespace characters.
    private static final Pattern WHITESPACE_BLOCK = Pattern.compile("\\s+");
     * <p><code>StringUtils</code> instances should NOT be constructed in
     * standard programming. Instead, the class should be used as
     * <code>StringUtils.trim(" foo ");</code>.</p>
     * <p>This constructor is public to permit tools that require a JavaBean
     * instance to operate.</p>
    public StringUtils() {
        super();
    }
     * <p>Checks if a CharSequence is empty ("") or null.</p>
     * <pre>
     * StringUtils.isEmpty(null)      = true
     * StringUtils.isEmpty("")        = true
     * StringUtils.isEmpty(" ")       = false
     * StringUtils.isEmpty("bob")     = false
     * StringUtils.isEmpty("  bob  ") = false
     * </pre>
     * <p>NOTE: This method changed in Lang version 2.0.
     * It no longer trims the CharSequence.
     * That functionality is available in isBlank().</p>
     * @param cs  the CharSequence to check, may be null
     * …
    public static boolean isEmpty(CharSequence cs) { … }
     * <p>Checks if a CharSequence is not empty ("") and not null.</p>
     * <pre>
     * StringUtils.isNotEmpty(null)      = false
     * StringUtils.isNotEmpty("")        = false
     * StringUtils.isNotEmpty(" ")       = true
     * StringUtils.isNotEmpty("bob")     = true
     * StringUtils.isNotEmpty("  bob  ") = true
     * </pre>
     * @param cs  the CharSequence to check, may be null
     * @return <code>true</code> if the CharSequence is not empty and not null
     * @since 3.0 Changed signature from isNotEmpty(String) to isNotEmpty(CharSequence)
    public static boolean isNotEmpty(CharSequence cs) { … }
     * <p>Checks if a CharSequence is whitespace, empty ("") or null.</p>
     * <pre>
     * StringUtils.isBlank(null)      = true
     * StringUtils.isBlank("")        = true
     * StringUtils.isBlank(" ")       = true
     * StringUtils.isBlank("bob")     = false
     * StringUtils.isBlank("  bob  ") = false
     * </pre>
     * @param cs  the CharSequence to check, may be null
     * @return <code>true</code> if the CharSequence is null, empty or whitespace
     * @since 2.0
     * @since 3.0 Changed signature from isBlank(String) to isBlank(CharSequence)
    public static boolean isBlank(CharSequence cs) { … }
     * <p>Checks if a CharSequence is not empty (""), not null and not whitespace only.</p>
     * <pre>
     * StringUtils.isNotBlank(null)      = false
     * StringUtils.isNotBlank("")        = false
     * StringUtils.isNotBlank(" ")       = false
     * StringUtils.isNotBlank("bob")     = true
     * StringUtils.isNotBlank("  bob  ") = true
     * </pre>
     * @param cs  the CharSequence to check, may be null
     * @return <code>true</code> if the CharSequence is
     * not empty and not null and not whitespace
     * @since 2.0
     * …
    public static boolean isNotBlank(CharSequence cs) { … }
     * <p>Removes control characters (char &lt;= 32) from both
     * ends of this String, handling <code>null</code> by returning
     * <code>null</code>.</p>
     * <p>The String is trimmed using {@link String#trim()}.
     * Trim removes start and end characters &lt;= 32.
     * To strip whitespace use {@link #strip(String)}.</p>
     * <p>To trim your choice of characters, use the
     * {@link #strip(String, String)} methods.</p>
     * <pre>
     * StringUtils.trim(null)          = null
     * StringUtils.trim("")            = ""
     * StringUtils.trim("     ")       = ""
     * …
    public static String trim(String str) { … }
     * <p>Removes control characters (char &lt;= 32) from both
     * ends of this String returning <code>null</code> if the String is
     * empty ("") after the trim or if it is <code>null</code>.
     * <p>The String is trimmed using {@link String#trim()}.
     * Trim removes start and end characters &lt;= 32.
     * To strip whitespace use {@link #stripToNull(String)}.</p>
     * <pre>
     * StringUtils.trimToNull(null)          = null
     * StringUtils.trimToNull("")            = null
     * StringUtils.trimToNull("     ")       = null
     * StringUtils.trimToNull("abc")         = "abc"
     * StringUtils.trimToNull("    abc    ") = "abc"
     * …
    public static String trimToNull(String str) { … }
     * <p>Removes control characters (char &lt;= 32) from both
     * ends of this String returning an empty String ("") if the String
     * is empty ("") after the trim or if it is <code>null</code>.
     * <p>The String is trimmed using {@link String#trim()}.
     * Trim removes start and end characters &lt;= 32.
     * To strip whitespace use {@link #stripToEmpty(String)}.</p>
     * <pre>
     * StringUtils.trimToEmpty(null)          = ""
     * StringUtils.trimToEmpty("")            = ""
     * StringUtils.trimToEmpty("     ")       = ""
     * StringUtils.trimToEmpty("abc")         = "abc"
     * StringUtils.trimToEmpty("    abc    ") = "abc"
     * …
    public static String trimToEmpty(String str) { … }
     * <p>Removes the accents from a string. </p>
     * <p>NOTE: This is a JDK 1.6 method, it will fail on JDK 1.5. </p>
     * <pre>
     * StringUtils.stripAccents(null)                = null
     * StringUtils.stripAccents("")                  = ""
     * StringUtils.stripAccents("control")           = "control"
     * StringUtils.stripAccents("&ecute;clair")      = "eclair"
     * </pre>
     * @param input String to be stripped
     * @return String without accents on the text
     * @since 3.0
    public static String stripAccents(String input) { … }
}
// … (non-patched members truncated; patched methods kept)
</class>
THE PATCH-CHANGED CLASS: NumberUtils. Calls on THIS class are the PROBE tier of the two-tier catch in the output spec: once you have declared the input valid by construction, an exception from one of them is a reportable result, never a rejection to skip. Calls on any OTHER class are setup, where a caught exception still means skip this input.
The patch under analysis (propose relations that the CHANGED expressions/conditions could violate — the boundary values the changed condition tests are where an overfit breaks):
<patch>
--- /src/main/java/org/apache/commons/lang3/math/NumberUtils.java
+++ /src/main/java/org/apache/commons/lang3/math/NumberUtils.java
@@ -1409,6 +1409,9 @@
             }
             if (chars[i] == 'l'
                 || chars[i] == 'L') {
+//ACS's patch begin
+	if (hasDecPoint==true){return false;}
+//ACS's patch end
                 // not allowing L with an exponent or decimal point
                 return foundDigit && !hasExp;
             }
@@ -1420,4 +1423,4 @@
         return !allowSigns && foundDigit;
     }
     
-}
+}
\ No newline at end of file

</patch>
THE EXACT LINES THE PATCH ADDED/REMOVED (your first relation must target the behaviour THESE govern — the condition, boundary token, or formula here is where an overfit and a correct fix diverge; ADDED lines are what the patch introduced, REMOVED what it deleted — the overfit may have changed these WRONGLY):
    ADDED  : if (hasDecPoint==true){return false;}
    REMOVED: }
    ADDED  : }
Documented contract of a touched method (relations must follow from contracts like this):
<javadoc>
<p>Provides extra functionality for Java Number classes.</p>

@author Apache Software Foundation
@author <a href="mailto:rand_mcneely@yahoo.com">Rand McNeely</a>
@author <a href="mailto:steve.downey@netfolio.com">Steve Downey</a>
@author Eric Pugh
@author Phil Steitz
@author Matthew Hawthorne
@author <a href="mailto:ggregory@seagullsw.com">Gary Gregory</a>
@author <a href="mailto:fredrik@westermarck.com">Fredrik Westermarck</a>
@since 2.0
@version $Id$
/
public class NumberUtils {

/** Reusable Long constant for zero. */
public static final Long LONG_ZERO = new Long(0L);
/** Reusable Long constant for one. */
public static final Long LONG_ONE = new Long(1L);
/** Reusable Long constant for minus one. */
public static final Long LONG_MINUS_ONE = new Long(-1L);
/** Reusable Integer constant for zero. */
public static final Integer INTEGER_ZERO = new Integer(0);
/** Reusable Integer constant for one. */
public static final Integer INTEGER_ONE = new Integer(1);
/** Reusable Integer constant for minus one. */
public static final Integer INTEGER_MINUS_ONE = new Integer(-1);
/** Reusable Short constant for zero. */
public static final Short SHORT_ZERO = new Short((short) 0);
/** Reusable Short constant for one. */
public static final Short SHORT_ONE = new Short((short) 1);
/** Reusable Short constant for minus one. */
public static final Short SHORT_MINUS_ONE = new Short((short) -1);
/** Reusable Byte constant for zero. */
public static final Byte BYTE_ZERO = Byte.valueOf((byte) 0);

</javadoc>
IMPORTS OF THE PATCHED CLASS — the authoritative package for every non-JDK type in this codebase VERSION. When a check references a type, use the package EXACTLY as imported here (or as shown in the skeletons); NEVER guess a package name from library conventions — this version's layout may differ, and a wrong package means the relation cannot compile and is discarded unread:
import java.math.BigDecimal;
import java.math.BigInteger;
import org.apache.commons.lang3.StringUtils;
ALSO WORTH CONSTRAINING (secondary): methods/types the failing test exercises — the bug's symptom is observed through these, so when the patch edited a DIFFERENT method than the one whose output is wrong, one relation constraining one of these can catch it. This is an addition to, never a replacement for, relations grounded in the documented contract of the touched code — prefer the documented-contract relation when choosing:
    isNumber

ANCHOR REQUIREMENT — READ FIRST. The patch changed a SPECIFIC expression in a SPECIFIC method. An overfit usually differs from a correct implementation WHERE the code was changed (the one exception: a method the FAILING TEST reads can stay wrong when the patch edited elsewhere — that is what the secondary list below is for). Your FIRST relation MUST directly constrain the OUTPUT of a changed method, exercising the exact input the change is about — the prefix / sign / boundary value / edge case named in the changed condition — and asserting only what that method's OWN documented contract guarantees there. (E.g. if the change adds a guard on inputs starting with a token, your relation must feed such inputs and assert the documented result; if the change rewrites a formula the javadoc states, assert the method's output equals that formula recomputed independently.) Do NOT fill the slots with generic round-trip or bound properties of untouched sibling methods.
If the changed expression is a NUMERIC formula, a correct fix may reassociate it (a*(b/c) vs (a*b)/c) and differ by rounding, so assert APPROXIMATE equality with a GENEROUS relative tolerance — never exact ==, or you will falsely flag a correct reassociation. Use `Math.abs(a-b) <= tol * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)))` with tol AT LEAST 1e-9; and when the computation multiplies or sums large integer/long intermediates (products near or above 2^31, or billion-scale inputs), use tol = 1e-6 — at those magnitudes the library's OWN double rounding exceeds 1e-9, so a tighter tolerance false-fires on a correct patch. A too-tight tolerance is as unsound as exact ==; the real bug you are hunting diverges by a LARGE amount (e.g. a sign flip or a value 100x off), not by rounding.
STANDING STRATEGY — DOCUMENTED FORMULAS (checked first, before anything else): scan the javadoc of the touched class's numeric getters AND of every class shown with role="test-subject" (the classes the failing test itself constructs and reads — the defect's observable surface often lives there, not in the patched class) for a stated closed-form formula ('the value equals a * b / c of the constructor parameters', 'returns x * (1 - x)', ...). If ANY such formula exists, your FIRST relation MUST be that formula: recompute it independently from the object's own parameters and compare with the generous magnitude-scaled tolerance above. A documented formula is the strongest relation class there is — it is deterministic, it holds for every correct implementation by definition, and a patch that leaves the value wrong anywhere in the domain cannot pass it. Only when no documented formula exists does the anchor requirement below decide your first relation.
STANDING STRATEGY — DOCUMENTED @THROWS (checked second): scan the touched method's javadoc (and its parent declaration's, if shown) for declared exceptions — '@throws X if/when <condition>'. For EACH such declaration whose triggering input you can construct (a null argument, a malformed string, an out-of-range value), one relation MUST construct exactly that input and assert the documented throw: call the method, and if it COMPLETES NORMALLY (or throws a different, non-subclass exception), throw the violation. MANDATORY check shape for these documented-throw relations — catching broadly and returning DEFEATS the relation (a patch whose reordered guard throws the WRONG exception class then silently skips instead of firing):
    try { obj.method(rejectedInput); violated = true; /* completed normally */ }
    catch (TheDocumentedException ok) { /* contract honoured */ }
    catch (Throwable t) { violated = true; /* wrong exception class — also a contract violation */ }
  then throw the violation OUTSIDE the try when `violated` is set, naming what actually happened (completed vs wrong class <type>). This is a tripwire: it stays silent on any build that honours the contract and fires on a patch whose added or reordered guard swallows the documented rejection — a classic overfit shape (a guard inserted BEFORE an argument check silently returns where the contract says throw). Use ONLY declared @throws with their stated conditions — never invent an undocumented rejection; a subclass of the declared exception counts as honouring the contract. ISOLATE THE ONE VIOLATION: build the input so it breaks ONLY this @throws condition and satisfies every OTHER documented precondition — when several are broken at once a correct implementation may throw a different documented exception first, making a wrong-class assertion unsound (it fires on correct code). IMPORTANT — vary the RECEIVER and the OTHER arguments across their special states while holding the rejected input fixed: an overfit often reorders the check behind a guard that only triggers for a special receiver state (a NaN/empty/zero receiver), so a plain rejected input alone misses it — assert the documented throw on an ORDINARY receiver AND on each special-state receiver the class admits (empty, extreme, identity, NaN), since a patch may return early only in the special-state case.
STANDING STRATEGY — REJECTION INDEPENDENCE (receiver-state variation): for ANY relation that asserts a rejection/error contract — asking for something absent, invalid, or out of range MUST throw — also emit a companion relation that holds the probe input FIXED and instead varies the RECEIVER / CONTAINER state around it: its contents, its size, which elements are installed / registered / present, whether it is empty or populated, freshly built or already mutated. A correct rejection depends only on the probe itself being absent or invalid, never on unrelated receiver state, so the documented throw MUST fire identically across every such state; a patch that makes the rejection conditional on the container's contents or size is exactly the overfit this companion catches. Build each varied state by construction through the real public API (draw the variation from `data`) and assert the SAME documented throw on each — one extra relation per rejection contract.
    ORDERING (mandatory, and the half most checks get wrong): re-run the rejection probe AFTER EVERY state-changing call in the check body, not once on the freshly built receiver. Mutate, then probe; mutate again, then probe again — asserting the same documented outcome each time. A probe that runs before the mutations has only ever observed the state a state-conditional patch still gets right, so the check is silent on both builds no matter how well the container is fuzzed; the divergence lives in the state AFTER the container has been filled, emptied, or left with a gap. Draw WHICH slot/key/index each mutation targets, and how many mutations happen, from `data` (never from literals), so the sequence of states the probe sees varies across iterations. This is checked mechanically at screening: a rejection probe with no probe after the receiver's last state change is reported as an ordering blind spot.
STANDING STRATEGY — STRUCTURE FROM DATA (how the varied state is built): whenever a relation's property depends on the RECEIVER / CONTAINER state — a rejection contract, an index/position/lookup contract, a size/emptiness contract — the container's STRUCTURE must be drawn from `data`, not written as compile-time constants. Structure means: HOW MANY elements it holds, AT WHICH indices or keys they are installed, whether there are GAPS/HOLES between them, whether it is EMPTY or populated, whether it is freshly built or already mutated, and in what ORDER those operations happened. Concretely, the element count, every install index, every loop bound and every branch that decides whether an element is added at all must come from a `data.consume*` draw (e.g. `int n = data.consumeInt(0, 4);` then install at `data.consumeInt(0, n)`), NOT from literals. Fuzzing only the labels/names/values INSIDE a fixed structure is COSMETIC: every iteration rebuilds the same shape, so a check that looks fuzzed is really a single hard-coded scenario, and a patch that misbehaves only for a different shape — a container with a hole in it, an empty one, a larger one, an element installed at a different position — is never exercised, on either build. This is checked mechanically at screening: a receiver-state relation whose construction consumes no fuzz bytes in any index, count or loop bound is reported as a structural blind spot.
Propose up to 6 relations. COVERAGE REQUIREMENT (R2): the set must cover DISTINCT documented observables — after the documented-formula and @throws slots above, spread the remaining slots across different contract sources (a formula, a throws clause, a state/read-only guarantee, a sibling-agreement rule) rather than proposing several variants of one observable. Do NOT anchor every relation on the patched method alone: a patch to one method can leave a DOCUMENTED observable of the test-subject class wrong away from the trigger, and a set of six relations about the patched method's own edge cases will all stay quiet while that documented observable diverges. At least ONE relation must assert a documented guarantee of a test-subject class when any is shown. And when the context lists SAME-NAME OVERLOADS or a METHOD FAMILY that contains the patched method, write one agreement relation PER SIBLING PAIR (patched method, sibling) for up to two siblings — prioritising the siblings that share the most behaviour with the patched method (same output shape, documented shared semantics), NOT just any family member: a patch that changes one member while a close sibling keeps the shared behaviour breaks exactly that pairwise agreement, and an agreement relation aimed at a distant family member can hold on a patch that a close-sibling relation convicts. Each pair relation: the two members given equivalent inputs must produce the documented consistent outcome — identical results up to the documented difference between them.
DEEP-DIVE PROTOCOL — do this enumeration BEFORE proposing: (1) LIST every documented observable in scope — each stated formula, each declared @throws, each documented range/format, each documented family agreement, each read-only/state guarantee — from the patched class AND every test-subject class shown, each with the doc line that states it; (2) MARK which of these the patch text could plausibly affect, directly or through shared state; (3) SPEND your slots on the marked ones per the coverage requirement above. Relations aimed at observables no documentation states are last-resort fills, never first choices. For each relation, give:
  - name: a short slug
  - kind: metamorphic | invariant
  - contract: the ONE documented guarantee that makes it hold for every correct implementation (javadoc sentence or a visible code invariant)
  - input: how to build a VALID input by construction (magnitudes, signs, shapes) that a correct implementation MUST accept — so a violation is a real defect, not a rejection of bad input.
    From the changed condition in the patch, the documented ranges (@param/@throws), and any bounds/size accessors this class exposes, identify the input-domain BOUNDARIES this code defines, and construct each check's inputs to PROBE at and just past them — but assert only what the contract guarantees there. Probing a boundary is free; asserting an unspecified boundary value is the classic unsound oracle.
  - check: a self-contained Java snippet that builds the input from an `int`/`long`/`String` chosen with the FuzzedDataProvider `data`, computes both sides (or the property) via the REAL public API, and `throw new RuntimeException("relation <name> violated: "+...)` on disagreement.
    TWO-TIER CATCH (mandatory; checked mechanically). The two tiers treat an exception differently, because they mean different things:
      TIER 1 — SETUP (constructing the inputs and the receivers): `try { ... } catch (Exception e) { return; }`. An input or receiver that will not build is an input this relation skips — there a caught exception IS a rejection.
      TIER 2 — THE PROBE (the call(s) on the PATCH-CHANGED class named in the context above): its OWN separate try, NOT nested inside the setup try, whose catch RE-THROWS instead of returning:
        try { r = changed.method(validInput); }
        catch (Exception e) { throw new RuntimeException("relation <name> violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage()); }
      You have already declared this input VALID BY CONSTRUCTION, so an exception raised inside the changed code on it is a RESULT the relation must report, not a rejection to skip. A blanket catch-and-return around the probe makes the check quiet on BOTH builds, so a patch that ADDS or MOVES a throw — refusing an input the contract requires it to accept — is invisible to every relation shaped that way. Setup exceptions stay rejections; probe exceptions are reportable.
      EXCEPTION TO TIER 2: a relation whose CONTRACT IS a rejection (the documented-@throws shapes above) keeps its TARGETED `catch (TheDocumentedException ok) { }` — that catch names the ONE exception the contract promises, which is evidence, not a swallow. Never a blanket catch-and-return around a probe either way. And tier 2 covers only exceptions the documentation does NOT permit: an exception the probed method DECLARES or documents (@throws — e.g. a solver's documented non-convergence exception on a hard input) is a legitimate outcome of a correct implementation — treat it exactly like a setup rejection (catch that named type and return), and reserve the tier-2 rethrow for exceptions no documentation permits.
    STRUCTURE RULE (checked mechanically; violations are rejected): the setup try/catch goes ONLY around the API calls that build/compute the values. The comparison and its `throw new RuntimeException(...) violated ...` must sit AFTER/OUTSIDE that try block — a violation thrown inside a catch-everything block that returns is caught by it and silently discarded, making the whole check dead (this is why the tier-2 try must not be nested inside the tier-1 try: its rethrow would land in the setup catch and die there). Pattern:
      T a, b;
      try { a = api(...); b = api2(...); } catch (Exception e) { return; }
      try { b = changed.method(a); } catch (Exception e) { throw new RuntimeException("relation <name> violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage()); }
      if (!close(a, b)) throw new RuntimeException("relation <name> violated: " + a + " vs " + b);
Draw inputs ONLY with the Jazzer FuzzedDataProvider API on `data`: data.consumeInt(), data.consumeInt(lo,hi), data.consumeLong(), data.consumeBoolean(), data.consumeAsciiString(n), data.consumeRemainingAsString() — there is NO drawInt/nextInt. The check body runs INSIDE fuzzerTestOneInput, so use the real library types directly (no wrapper class, no imports block).
IMPLEMENTABILITY (checked mechanically; violations are dropped): the check may NOT define an anonymous or local SUBCLASS of a library class to reach a code path (e.g. `new AbstractIntegerDistribution(...){...}`) — a hand-built stand-in is forbidden and the relation will be discarded. If the behaviour you want lives on an abstract base, reach it through a REAL concrete library subclass that already exists (e.g. use `new UniformIntegerDistribution(min, max)` to get an integer distribution over a chosen support, rather than subclassing the abstract base yourself). If no real subclass can construct the input you need, choose a different relation.
Return ONLY a JSON array of objects with keys name, kind, contract, input, check. No prose outside the JSON.
```

</details>
<details open><summary>▸ Output (~7,566 chars)</summary>

```
[
  {
    "name": "decimal_with_long_suffix_is_rejected",
    "kind": "metamorphic",
    "contract": "For NumberUtils.isNumber(String), a decimal form with a decimal point is accepted as a number, but adding an 'L'/'l' long type qualifier to such a decimal form is invalid. This is the parser rule exercised by the changed branch and pinned by the trusted failing test case LANG-664: isNumber(\"1.1L\") must be false.",
    "input": "Construct a valid decimal string of the form optional '-' + digits + '.' + digits, with no exponent and no existing type qualifier. Then compare it with the same string plus an 'L' suffix.",
    "check": "String base;\ntry {\n    int whole = data.consumeInt(0, 1000000);\n    int frac = data.consumeInt(0, 1000000);\n    boolean neg = data.consumeBoolean();\n    base = (neg ? \"-\" : \"\") + whole + \".\" + frac;\n} catch (Exception e) {\n    return;\n}\nboolean plain;\ntry {\n    plain = NumberUtils.isNumber(base);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation decimal_with_long_suffix_is_rejected violated: unexpected \" + e.getClass().getName() + \" on valid decimal input \" + base + \": \" + e.getMessage());\n}\nboolean withLong;\ntry {\n    withLong = NumberUtils.isNumber(base + \"L\");\n} catch (Exception e) {\n    throw new RuntimeException(\"relation decimal_with_long_suffix_is_rejected violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction suffixed input \" + base + \"L: \" + e.getMessage());\n}\nif (!plain) {\n    throw new RuntimeException(\"relation decimal_with_long_suffix_is_rejected violated: plain decimal should be accepted but isNumber(\" + base + \") was false\");\n}\nif (withLong) {\n    throw new RuntimeException(\"relation decimal_with_long_suffix_is_rejected violated: decimal with long suffix should be rejected but isNumber(\" + base + \"L) was true\");\n}"
  },
  {
    "name": "accepted_plain_decimal_has_creatable_number",
    "kind": "invariant",
    "contract": "The trusted test couples NumberUtils.isNumber(String) with NumberUtils.createNumber(String): for accepted numeric strings such as plain decimal forms, createNumber must succeed and return a non-null Number.",
    "input": "Construct a valid plain decimal string of the form optional '-' + digits + '.' + digits, with no exponent and no type qualifier.",
    "check": "String s;\ntry {\n    int whole = data.consumeInt(0, 1000000);\n    int frac = data.consumeInt(0, 1000000);\n    s = (data.consumeBoolean() ? \"-\" : \"\") + whole + \".\" + frac;\n} catch (Exception e) {\n    return;\n}\nboolean ok;\ntry {\n    ok = NumberUtils.isNumber(s);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation accepted_plain_decimal_has_creatable_number violated: unexpected \" + e.getClass().getName() + \" from isNumber(\" + s + \"): \" + e.getMessage());\n}\nif (!ok) {\n    throw new RuntimeException(\"relation accepted_plain_decimal_has_creatable_number violated: valid plain decimal was rejected by isNumber(\" + s + \")\");\n}\nNumber n;\ntry {\n    n = NumberUtils.createNumber(s);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation accepted_plain_decimal_has_creatable_number violated: unexpected \" + e.getClass().getName() + \" from createNumber(\" + s + \"): \" + e.getMessage());\n}\nif (n == null) {\n    throw new RuntimeException(\"relation accepted_plain_decimal_has_creatable_number violated: createNumber(\" + s + \") returned null for an accepted number string\");\n}"
  },
  {
    "name": "toInt_zero_default_overload_agreement",
    "kind": "metamorphic",
    "contract": "NumberUtils.toInt(String) is documented as returning zero on conversion failure, while NumberUtils.toInt(String, int) returns the supplied default on conversion failure; therefore, using default 0 must agree exactly with the zero-default overload for every input.",
    "input": "Use any remaining fuzz string, including null-equivalent impossible here, empty, numeric, or malformed text; both overloads are defined on all such strings.",
    "check": "String s;\nint a;\nint b;\ntry {\n    s = data.consumeRemainingAsString();\n} catch (Exception e) {\n    return;\n}\ntry {\n    a = NumberUtils.toInt(s);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation toInt_zero_default_overload_agreement violated: unexpected \" + e.getClass().getName() + \" from toInt(String): \" + e.getMessage());\n}\ntry {\n    b = NumberUtils.toInt(s, 0);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation toInt_zero_default_overload_agreement violated: unexpected \" + e.getClass().getName() + \" from toInt(String,int): \" + e.getMessage());\n}\nif (a != b) {\n    throw new RuntimeException(\"relation toInt_zero_default_overload_agreement violated: toInt results differ for input '\" + s + \"': \" + a + \" vs \" + b);\n}"
  },
  {
    "name": "toLong_zero_default_overload_agreement",
    "kind": "metamorphic",
    "contract": "NumberUtils.toLong(String) is documented as returning 0L on conversion failure, while NumberUtils.toLong(String, long) returns the supplied default on conversion failure; therefore, using default 0L must agree exactly with the zero-default overload for every input.",
    "input": "Use any remaining fuzz string; both overloads define behavior for parseable and non-parseable strings alike.",
    "check": "String s;\nlong a;\nlong b;\ntry {\n    s = data.consumeRemainingAsString();\n} catch (Exception e) {\n    return;\n}\ntry {\n    a = NumberUtils.toLong(s);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation toLong_zero_default_overload_agreement violated: unexpected \" + e.getClass().getName() + \" from toLong(String): \" + e.getMessage());\n}\ntry {\n    b = NumberUtils.toLong(s, 0L);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation toLong_zero_default_overload_agreement violated: unexpected \" + e.getClass().getName() + \" from toLong(String,long): \" + e.getMessage());\n}\nif (a != b) {\n    throw new RuntimeException(\"relation toLong_zero_default_overload_agreement violated: toLong results differ for input '\" + s + \"': \" + a + \" vs \" + b);\n}"
  },
  {
    "name": "toDouble_zero_default_overload_agreement",
    "kind": "metamorphic",
    "contract": "NumberUtils.toDouble(String) is documented as returning 0.0d on conversion failure, while NumberUtils.toDouble(String, double) returns the supplied default on conversion failure; therefore, using default 0.0d must agree exactly with the zero-default overload for every input.",
    "input": "Use any remaining fuzz string; the documented behavior covers valid numeric strings and conversion failures.",
    "check": "String s;\ndouble a;\ndouble b;\ntry {\n    s = data.consumeRemainingAsString();\n} catch (Exception e) {\n    return;\n}\ntry {\n    a = NumberUtils.toDouble(s);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation toDouble_zero_default_overload_agreement violated: unexpected \" + e.getClass().getName() + \" from toDouble(String): \" + e.getMessage());\n}\ntry {\n    b = NumberUtils.toDouble(s, 0.0d);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation toDouble_zero_default_overload_agreement violated: unexpected \" + e.getClass().getName() + \" from toDouble(String,double): \" + e.getMessage());\n}\nif (Double.doubleToLongBits(a) != Double.doubleToLongBits(b)) {\n    throw new RuntimeException(\"relation toDouble_zero_default_overload_agreement violated: toDouble results differ for input '\" + s + \"': \" + a + \" vs \" + b);\n}"
  }
]
```

</details>

---
## [5] ⚙️ screen · `decimal_with_long_suffix_is_rejected`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [6] ⚙️ screen-fuzz-buggy · `decimal_with_long_suffix_is_rejected`
**output:** 
```json
{
  "checked": 20000,
  "violated": 20000,
  "ratio": 1.0
}
```

---
## [7] ⚙️ screen · `accepted_plain_decimal_has_creatable_number`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [8] ⚙️ screen-fuzz-buggy · `accepted_plain_decimal_has_creatable_number`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [9] ⚙️ screen · `toInt_zero_default_overload_agreement`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [10] ⚙️ screen-fuzz-buggy · `toInt_zero_default_overload_agreement`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [11] ⚙️ screen · `toLong_zero_default_overload_agreement`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [12] ⚙️ screen-fuzz-buggy · `toLong_zero_default_overload_agreement`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [13] ⚙️ screen · `toDouble_zero_default_overload_agreement`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [14] ⚙️ screen-fuzz-buggy · `toDouble_zero_default_overload_agreement`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [15] ⚙️ screen · `decimal_with_long_suffix_is_rejected`
**output:** **kept**
- reason: kept: direction-confirmed

---
## [16] ⚙️ screen · `accepted_plain_decimal_has_creatable_number`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [17] ⚙️ screen · `toInt_zero_default_overload_agreement`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [18] ⚙️ screen · `toLong_zero_default_overload_agreement`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [19] ⚙️ screen · `toDouble_zero_default_overload_agreement`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [20] ⚙️ screening-survivors
**output:** 
```json
{
  "kept": [
    "decimal_with_long_suffix_is_rejected",
    "accepted_plain_decimal_has_creatable_number",
    "toInt_zero_default_overload_agreement",
    "toLong_zero_default_overload_agreement",
    "toDouble_zero_default_overload_agreement"
  ],
  "count": 5
}
```

---
## [21] 🧠 LLM call — **rule synthesis** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [4] — not reprinted*
- *[user] message: identical to step [4] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,716 chars)</summary>

```
[
  {
    "name": "decimalLongSuffixRejected",
    "kind": "invariant",
    "contract": "The numeric recognizer accepts decimal forms such as \"1234.5\" but rejects a long type qualifier on a decimal form; the failing test fixes this boundary explicitly with NumberUtils.isNumber(\"1.1L\") == false.",
    "input": "Construct a decimal literal with at least one digit on each side of the decimal point, optionally preceded by '-', then append 'L'. This directly exercises the patched hasDecPoint/L boundary.",
    "check": "String intPart = String.valueOf(Math.abs(data.consumeInt()) % 100000 + 1);\nString fracPart = String.valueOf(Math.abs(data.consumeInt()) % 100000 + 1);\nString s = (data.consumeBoolean() ? \"-\" : \"\") + intPart + \".\" + fracPart + \"L\";\nboolean r;\ntry {\n    r = NumberUtils.isNumber(s);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation decimalLongSuffixRejected violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input \" + s + \": \" + e.getMessage());\n}\nif (r) {\n    throw new RuntimeException(\"relation decimalLongSuffixRejected violated: expected false for decimal long-qualified literal, got true for \" + s);\n}"
  },
  {
    "name": "trailingDecimalPointAccepted",
    "kind": "invariant",
    "contract": "The recognizer accepts ordinary decimal numbers, and the test explicitly fixes the boundary case NumberUtils.isNumber(\"2.\") == true.",
    "input": "Construct a non-empty sequence of decimal digits, optionally with a leading '-', and append a trailing '.'.",
    "check": "String digits = String.valueOf(Math.abs(data.consumeInt()) % 100000 + 1);\nString s = (data.consumeBoolean() ? \"-\" : \"\") + digits + \".\";\nboolean r;\ntry {\n    r = NumberUtils.isNumber(s);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation trailingDecimalPointAccepted violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input \" + s + \": \" + e.getMessage());\n}\nif (!r) {\n    throw new RuntimeException(\"relation trailingDecimalPointAccepted violated: expected true for trailing-decimal form, got false for \" + s);\n}"
  },
  {
    "name": "leadingOrTrailingSpaceRejected",
    "kind": "invariant",
    "contract": "isNumber does not trim its input; the test fixes that strings like \" 1111\" and \"1111 \" are not numbers.",
    "input": "Construct a non-empty digit string and test it with one leading space and one trailing space.",
    "check": "String core = String.valueOf(Math.abs(data.consumeInt()) % 100000 + 1);\nString left = \" \" + core;\nString right = core + \" \";\nboolean rl;\nboolean rr;\ntry {\n    rl = NumberUtils.isNumber(left);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation leadingOrTrailingSpaceRejected violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input \" + left + \": \" + e.getMessage());\n}\ntry {\n    rr = NumberUtils.isNumber(right);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation leadingOrTrailingSpaceRejected violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input \" + right + \": \" + e.getMessage());\n}\nif (rl || rr) {\n    throw new RuntimeException(\"relation leadingOrTrailingSpaceRejected violated: expected both spaced forms to be false, got left=\" + rl + \", right=\" + rr + \" for core=\" + core);\n}"
  },
  {
    "name": "signedExponentFormsAccepted",
    "kind": "metamorphic",
    "contract": "The recognizer accepts exponent notation with either '+' or '-' after E when digits follow; the test fixes both \"1234E+5\" and \"1234E-5\" as true.",
    "input": "Construct a mantissa from decimal digits, optionally with leading '-', and a non-empty exponent digit sequence; compare the + and - exponent forms.",
    "check": "String mantissa = (data.consumeBoolean() ? \"-\" : \"\") + String.valueOf(Math.abs(data.consumeInt()) % 100000 + 1);\nString exp = String.valueOf(Math.abs(data.consumeInt()) % 100000 + 1);\nString plus = mantissa + \"E+\" + exp;\nString minus = mantissa + \"E-\" + exp;\nboolean rp;\nboolean rm;\ntry {\n    rp = NumberUtils.isNumber(plus);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation signedExponentFormsAccepted violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input \" + plus + \": \" + e.getMessage());\n}\ntry {\n    rm = NumberUtils.isNumber(minus);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation signedExponentFormsAccepted violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input \" + minus + \": \" + e.getMessage());\n}\nif (!(rp && rm)) {\n    throw new RuntimeException(\"relation signedExponentFormsAccepted violated: expected both signed exponent forms to be true, got plus=\" + rp + \", minus=\" + rm + \" for mantissa=\" + mantissa + \", exp=\" + exp);\n}"
  },
  {
    "name": "hexLetterCaseAgreement",
    "kind": "metamorphic",
    "contract": "Hexadecimal input after a 0x prefix accepts digits and letters a-f or A-F; therefore changing those hex letters between lower and upper case must not change recognition.",
    "input": "Construct a non-empty hexadecimal tail using only characters 0-9 and a-f, then compare recognition of the lower-case and upper-case 0x forms.",
    "check": "int n = data.consumeInt(1, 8);\nStringBuilder sb = new StringBuilder();\nfor (int i = 0; i < n; i++) {\n    int v = data.consumeInt(0, 15);\n    sb.append(\"0123456789abcdef\".charAt(v));\n}\nString tail = sb.toString();\nString lower = \"0x\" + tail;\nString upper = \"0x\" + tail.toUpperCase();\nboolean rl;\nboolean ru;\ntry {\n    rl = NumberUtils.isNumber(lower);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation hexLetterCaseAgreement violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input \" + lower + \": \" + e.getMessage());\n}\ntry {\n    ru = NumberUtils.isNumber(upper);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation hexLetterCaseAgreement violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input \" + upper + \": \" + e.getMessage());\n}\nif (rl != ru || !rl) {\n    throw new RuntimeException(\"relation hexLetterCaseAgreement violated: expected both hex case variants to be accepted equally, got lower=\" + rl + \", upper=\" + ru + \" for tail=\" + tail);\n}"
  },
  {
    "name": "signPreservesIntegerRecognition",
    "kind": "metamorphic",
    "contract": "The recognizer allows an optional leading minus sign on ordinary integer literals; the test fixes both \"12345\" and \"-1234\" as numbers.",
    "input": "Construct a non-empty sequence of decimal digits and compare recognition of the unsigned and '-' prefixed forms.",
    "check": "String digits = String.valueOf(Math.abs(data.consumeInt()) % 100000 + 1);\nString plain = digits;\nString neg = \"-\" + digits;\nboolean rp;\nboolean rn;\ntry {\n    rp = NumberUtils.isNumber(plain);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation signPreservesIntegerRecognition violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input \" + plain + \": \" + e.getMessage());\n}\ntry {\n    rn = NumberUtils.isNumber(neg);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation signPreservesIntegerRecognition violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input \" + neg + \": \" + e.getMessage());\n}\nif (!(rp && rn)) {\n    throw new RuntimeException(\"relation signPreservesIntegerRecognition violated: expected both signed and unsigned integer forms to be true, got plain=\" + rp + \", neg=\" + rn + \" for digits=\" + digits);\n}"
  }
]
```

</details>

---
## [22] ⚙️ screen · `decimalLongSuffixRejected`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [23] ⚙️ screen · `decimalLongSuffixRejected`
**output:** **dropped**
- reason: negative-modulo index bug: near line 1: `Math.abs(data.consumeInt()) %` — Math.abs(Integer.MIN_VALUE) is NEGATIVE, so this index eventually goes out of bounds and crashes the harness itself. Use Math.floorMod(value, n) or a bounded consume (e.g. data.consumeInt(0, n - 1)) instead

---
## [24] ⚙️ screen · `trailingDecimalPointAccepted`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [25] ⚙️ screen · `trailingDecimalPointAccepted`
**output:** **dropped**
- reason: negative-modulo index bug: near line 1: `Math.abs(data.consumeInt()) %` — Math.abs(Integer.MIN_VALUE) is NEGATIVE, so this index eventually goes out of bounds and crashes the harness itself. Use Math.floorMod(value, n) or a bounded consume (e.g. data.consumeInt(0, n - 1)) instead

---
## [26] ⚙️ screen · `leadingOrTrailingSpaceRejected`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [27] ⚙️ screen · `leadingOrTrailingSpaceRejected`
**output:** **dropped**
- reason: negative-modulo index bug: near line 1: `Math.abs(data.consumeInt()) %` — Math.abs(Integer.MIN_VALUE) is NEGATIVE, so this index eventually goes out of bounds and crashes the harness itself. Use Math.floorMod(value, n) or a bounded consume (e.g. data.consumeInt(0, n - 1)) instead

---
## [28] ⚙️ screen · `signedExponentFormsAccepted`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [29] ⚙️ screen · `signedExponentFormsAccepted`
**output:** **dropped**
- reason: negative-modulo index bug: near line 1: `Math.abs(data.consumeInt()) %` — Math.abs(Integer.MIN_VALUE) is NEGATIVE, so this index eventually goes out of bounds and crashes the harness itself. Use Math.floorMod(value, n) or a bounded consume (e.g. data.consumeInt(0, n - 1)) instead

---
## [30] ⚙️ screen · `hexLetterCaseAgreement`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [31] ⚙️ screen-fuzz-buggy · `hexLetterCaseAgreement`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [32] ⚙️ screen · `signPreservesIntegerRecognition`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [33] ⚙️ screen · `signPreservesIntegerRecognition`
**output:** **dropped**
- reason: negative-modulo index bug: near line 1: `Math.abs(data.consumeInt()) %` — Math.abs(Integer.MIN_VALUE) is NEGATIVE, so this index eventually goes out of bounds and crashes the harness itself. Use Math.floorMod(value, n) or a bounded consume (e.g. data.consumeInt(0, n - 1)) instead

---
## [34] ⚙️ screen · `hexLetterCaseAgreement`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [35] ⚙️ reference-widening · `enumerate`
**output:** **nothing to widen**
- reason: the kept checks name no no-argument reader of the patched class — nothing to request a reference for; 4 further name(s) the checks probe are outside the patched class's no-argument readers and were excluded (['numberutils', 'toint', 'tolong', 'todouble'])
<details><summary>▸ detail (~908 chars)</summary>

- detail: {'targets': [], 'already_admitted': [], 'cap': 3, 'checks_read': 6, 'excluded': [{'key': 'numberutils', 'why': 'the patched class declares `NumberUtils()` as a constructor or a void method — it reports no observable value'}, {'key': 'toint', 'why': "the patched class declares `toInt` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}, {'key': 'tolong', 'why': "the patched class declares `toLong` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}, {'key': 'todouble', 'why': "the patched class declares `toDouble` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}], 'excluded_count': 4}

</details>

---
## [36] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~55,655 chars)</summary>

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

Codebase: `Lang_24_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/lang3/math/NumberUtils.java
+++ /src/main/java/org/apache/commons/lang3/math/NumberUtils.java
@@ -1409,6 +1409,9 @@
             }
             if (chars[i] == 'l'
                 || chars[i] == 'L') {
+//ACS's patch begin
+	if (hasDecPoint==true){return false;}
+//ACS's patch end
                 // not allowing L with an exponent or decimal point
                 return foundDigit && !hasExp;
             }
@@ -1420,4 +1423,4 @@
         return !allowSigns && foundDigit;
     }
     
-}
+}
\ No newline at end of file

</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import java.math.BigDecimal;
import java.math.BigInteger;
import org.apache.commons.lang3.StringUtils;
</source_imports>

Function `isNumber`:
<signature>
public static boolean isNumber(String str)
</signature>
<code>
    public static boolean isNumber(String str) {
        if (StringUtils.isEmpty(str)) {
            return false;
        }
        char[] chars = str.toCharArray();
        int sz = chars.length;
        boolean hasExp = false;
        boolean hasDecPoint = false;
        boolean allowSigns = false;
        boolean foundDigit = false;
        // deal with any possible sign up front
        int start = (chars[0] == '-') ? 1 : 0;
        if (sz > start + 1) {
            if (chars[start] == '0' && chars[start + 1] == 'x') {
                int i = start + 2;
                if (i == sz) {
                    return false; // str == "0x"
                }
                // checking hex (it can't be anything else)
                for (; i < chars.length; i++) {
                    if ((chars[i] < '0' || chars[i] > '9')
                        && (chars[i] < 'a' || chars[i] > 'f')
                        && (chars[i] < 'A' || chars[i] > 'F')) {
                        return false;
                    }
                }
                return true;
            }
        }
        sz--; // don't want to loop to the last char, check it afterwords
              // for type qualifiers
        int i = start;
        // loop to the next to last char or to the last char if we need another digit to
        // make a valid number (e.g. chars[0..5] = "1234E")
        while (i < sz || (i < sz + 1 && allowSigns && !foundDigit)) {
            if (chars[i] >= '0' && chars[i] <= '9') {
                foundDigit = true;
                allowSigns = false;

            } else if (chars[i] == '.') {
                if (hasDecPoint || hasExp) {
                    // two decimal points or dec in exponent   
                    return false;
                }
                hasDecPoint = true;
            } else if (chars[i] == 'e' || chars[i] == 'E') {
                // we've already taken care of hex.
                if (hasExp) {
                    // two E's
                    return false;
                }
                if (!foundDigit) {
                    return false;
                }
                hasExp = true;
                allowSigns = true;
            } else if (chars[i] == '+' || chars[i] == '-') {
                if (!allowSigns) {
                    return false;
                }
                allowSigns = false;
                foundDigit = false; // we need a digit after the E
            } else {
                return false;
            }
            i++;
        }
        if (i < chars.length) {
            if (chars[i] >= '0' && chars[i] <= '9') {
                // no type qualifier, OK
                return true;
            }
            if (chars[i] == 'e' || chars[i] == 'E') {
                // can't have an E at the last byte
                return false;
            }
            if (chars[i] == '.') {
                if (hasDecPoint || hasExp) {
                    // two decimal points or dec in exponent
                    return false;
                }
                // single trailing decimal point after non-exponent is ok
                return foundDigit;
            }
            if (!allowSigns
                && (chars[i] == 'd'
                    || chars[i] == 'D'
                    || chars[i] == 'f'
                    || chars[i] == 'F')) {
                return foundDigit;
            }
            if (chars[i] == 'l'
                || chars[i] == 'L') {
                // not allowing L with an exponent or decimal point
                return foundDigit && !hasExp;
            }
            // last character is illegal
            return false;
        }
        // allowSigns is true iff the val ends in 'E'
        // found digit it to make sure weird stuff like '.' and '1E-' doesn't pass
        return !allowSigns && foundDigit;
    }
</code>

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

THIS IS A NON-CRASHING (SEMANTIC) BUG. The trigger test does not throw — it fails a JUnit assertion because the code returns a WRONG value. There is therefore no exception to catch. Your harness must SUPPLY the oracle:

1. LIFT EVERY ASSERTION (not just one): the failing test almost always checks SEVERAL input/output pairs (e.g. many assertEquals lines). Each pair is a SEPARATE trusted oracle — the correct code is known to produce that exact expected value for that exact input. Find ALL of them (assertEquals / assertTrue / assertSame / assertNull ...), and for each identify (a) the call it makes on the real API and (b) the EXPECTED value it checks against. Copy both VERBATIM — do not infer or recompute; use the literals the test hard-codes.

2. RECONSTRUCT: in fuzzerTestOneInput, make EACH of those calls through the real public API, exactly as the test does, to get each ACTUAL value. EXACTLY-AS-THE-TEST-DOES includes the test's SETUP: if the expected value depends on environment the test arranges (a registered source file, a Locale or TimeZone, a formatting mode, a fixture object), replicate that setup faithfully — and if you cannot replicate it, DROP that pair rather than assert it, because a half-reconstructed scenario fires on correct code too (that exact mistake caused two false accusations of correct patches).

3. ASSERT ALL OF THEM: if ANY actual value does not equal its expected value, `throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:<short-id>] semantic mismatch: <what differed>")`. Every DISTINCT check gets its own [oracle:<short-id>] prefix (e.g. [oracle:lifted-pairs], [oracle:round-trip]) — alarms without an ID are rejected mechanically; the ID is how the pipeline tells which check fired. Check every lifted pair — an overfitting patch that fixed only the one reported input still fails the others, so the more pairs you assert the more likely it is caught. Jazzer reports the throw as a finding, exactly like a crash, so scoring is unchanged. If all match, continue. WHEN THE EXPECTED VALUE IS A STRING OF CODE OR FORMATTED TEXT (compiler/printer output, rendered source, messages with layout): never compare raw strings — normalize BOTH sides first (collapse all whitespace runs, e.g. s.replaceAll("\\s+", "")) so the check fires only on a CONTENT difference. A correct implementation may legitimately differ in spaces, newlines or statement-separator placement, and a raw-string firing over formatting will be dismissed in review — burying the real content difference the same comparison would have caught. COMPARE NORMALIZED, BUT RECORD RAW: whenever you normalize before comparing, the alarm message MUST also carry the PRE-normalization values, under these exact key names: `expectedNormalized=` `actualNormalized=` `expectedRaw=` `actualRaw=`. Emit the two Raw keys ONLY when you actually normalized, so their absence means no normalization happened. Reason: a downstream check asks whether your reported value equals a value the failing test pins, and the test pins the RAW form — a message carrying only the normalized form can never match it, so that check is dead for your alarm and a real dismissal is lost. Named keys, never positional: the reader must never have to guess which form it is holding. KEEP THE WHOLE MESSAGE ON ONE LINE: the raw form of formatted text usually contains newlines and tabs, and the alarm is read line by line — a literal newline inside a value silently cuts the message there, discarding every key after it. So ESCAPE them: emit `\n` and `\t` as the two-character sequences (backslash then n / t), never as real line breaks. In Java: `s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t")`. This is also the form the failing test's own source literal is written in, so the comparison lines up exactly.

4. THEN GENERALISE — but keep every assertion TRUSTED. The lifted pair tells you the answer for ONE input only. Do not invent an expected value for an arbitrary fuzzed input: an assertion you cannot justify will fire on CORRECT patches too (a false positive, the worst outcome). Extend coverage only in ways where the correct answer is independently known:

  (a) CONSTRUCT THE INPUT FROM A KNOWN ANSWER. Pick a value with FuzzedDataProvider, build the canonical input that must map to it, and assert you recover it — the answer is trusted because you chose it first. E.g. fuzz an int n, format it, parse it back, assert == n.

  (b) EQUIVALENCE TO THE SEED. The seed input has properties that fix its answer. Generate other inputs that SHARE those properties and must therefore share the seed's expected value, and assert they equal it. E.g. if the contract is case-insensitive and foo("abc")==X, then foo("ABC") and foo("Abc") must also ==X. This turns the one lifted value into an oracle for a whole family of inputs.

  (c) METAMORPHIC RELATION between two real calls (see below) — trusted because it holds for ANY correct implementation.

  GUARDRAIL: for any fuzzed input where you cannot justify the expected value by (a), (b), or (c), still CALL the API on it (this exercises the patched path), but do NOT assert on its result — just return. Assert only trusted answers; explore everything else without asserting.
  CALIBRATION: your assertions will be screened by an automated soundness reviewer before any firing counts, so do NOT hedge to trivially-weak checks to be safe. Write the strongest check you can JUSTIFY from the documented contract, and state that justification in a comment next to the check (the reviewer reads it). The reviewer is a safety net, not a licence to guess — it is imperfect, so an assertion still needs a real justification — but a vacuous assertion (isFinite / not-null / no-throw) can never catch a wrong value and guarantees a miss, which nothing downstream can recover.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `NumberUtils`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
Lift the call and the expected value from THIS test (this attempt's target):
<failing_test class="org.apache.commons.lang3.math.NumberUtilsTest" method="testIsNumber">
    public void testIsNumber() {
        String val = "12345";
        assertTrue("isNumber(String) 1 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 1 failed", checkCreateNumber(val));
        val = "1234.5";
        assertTrue("isNumber(String) 2 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 2 failed", checkCreateNumber(val));
        val = ".12345";
        assertTrue("isNumber(String) 3 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 3 failed", checkCreateNumber(val));
        val = "1234E5";
        assertTrue("isNumber(String) 4 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 4 failed", checkCreateNumber(val));
        val = "1234E+5";
        assertTrue("isNumber(String) 5 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 5 failed", checkCreateNumber(val));
        val = "1234E-5";
        assertTrue("isNumber(String) 6 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 6 failed", checkCreateNumber(val));
        val = "123.4E5";
        assertTrue("isNumber(String) 7 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 7 failed", checkCreateNumber(val));
        val = "-1234";
        assertTrue("isNumber(String) 8 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 8 failed", checkCreateNumber(val));
        val = "-1234.5";
        assertTrue("isNumber(String) 9 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 9 failed", checkCreateNumber(val));
        val = "-.12345";
        assertTrue("isNumber(String) 10 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 10 failed", checkCreateNumber(val));
        val = "-1234E5";
        assertTrue("isNumber(String) 11 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 11 failed", checkCreateNumber(val));
        val = "0";
        assertTrue("isNumber(String) 12 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 12 failed", checkCreateNumber(val));
        val = "-0";
        assertTrue("isNumber(String) 13 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 13 failed", checkCreateNumber(val));
        val = "01234";
        assertTrue("isNumber(String) 14 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 14 failed", checkCreateNumber(val));
        val = "-01234";
        assertTrue("isNumber(String) 15 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 15 failed", checkCreateNumber(val));
        val = "0xABC123";
        assertTrue("isNumber(String) 16 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 16 failed", checkCreateNumber(val));
        val = "0x0";
        assertTrue("isNumber(String) 17 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 17 failed", checkCreateNumber(val));
        val = "123.4E21D";
        assertTrue("isNumber(String) 19 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 19 failed", checkCreateNumber(val));
        val = "-221.23F";
        assertTrue("isNumber(String) 20 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 20 failed", checkCreateNumber(val));
        val = "22338L";
        assertTrue("isNumber(String) 21 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 21 failed", checkCreateNumber(val));
        val = null;
        assertTrue("isNumber(String) 1 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 1 Neg failed", !checkCreateNumber(val));
        val = "";
        assertTrue("isNumber(String) 2 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 2 Neg failed", !checkCreateNumber(val));
        val = "--2.3";
        assertTrue("isNumber(String) 3 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 3 Neg failed", !checkCreateNumber(val));
        val = ".12.3";
        assertTrue("isNumber(String) 4 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 4 Neg failed", !checkCreateNumber(val));
        val = "-123E";
        assertTrue("isNumber(String) 5 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 5 Neg failed", !checkCreateNumber(val));
        val = "-123E+-212";
        assertTrue("isNumber(String) 6 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 6 Neg failed", !checkCreateNumber(val));
        val = "-123E2.12";
        assertTrue("isNumber(String) 7 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 7 Neg failed", !checkCreateNumber(val));
        val = "0xGF";
        assertTrue("isNumber(String) 8 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 8 Neg failed", !checkCreateNumber(val));
        val = "0xFAE-1";
        assertTrue("isNumber(String) 9 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 9 Neg failed", !checkCreateNumber(val));
        val = ".";
        assertTrue("isNumber(String) 10 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 10 Neg failed", !checkCreateNumber(val));
        val = "-0ABC123";
        assertTrue("isNumber(String) 11 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 11 Neg failed", !checkCreateNumber(val));
        val = "123.4E-D";
        assertTrue("isNumber(String) 12 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 12 Neg failed", !checkCreateNumber(val));
        val = "123.4ED";
        assertTrue("isNumber(String) 13 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 13 Neg failed", !checkCreateNumber(val));
        val = "1234E5l";
        assertTrue("isNumber(String) 14 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 14 Neg failed", !checkCreateNumber(val));
        val = "11a";
        assertTrue("isNumber(String) 15 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 15 Neg failed", !checkCreateNumber(val)); 
        val = "1a";
        assertTrue("isNumber(String) 16 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 16 Neg failed", !checkCreateNumber(val)); 
        val = "a";
        assertTrue("isNumber(String) 17 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 17 Neg failed", !checkCreateNumber(val)); 
        val = "11g";
        assertTrue("isNumber(String) 18 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 18 Neg failed", !checkCreateNumber(val)); 
        val = "11z";
        assertTrue("isNumber(String) 19 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 19 Neg failed", !checkCreateNumber(val)); 
        val = "11def";
        assertTrue("isNumber(String) 20 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 20 Neg failed", !checkCreateNumber(val)); 
        val = "11d11";
        assertTrue("isNumber(String) 21 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 21 Neg failed", !checkCreateNumber(val)); 
        val = "11 11";
        assertTrue("isNumber(String) 22 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 22 Neg failed", !checkCreateNumber(val));
        val = " 1111";
        assertTrue("isNumber(String) 23 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 23 Neg failed", !checkCreateNumber(val));
        val = "1111 ";
        assertTrue("isNumber(String) 24 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 24 Neg failed", !checkCreateNumber(val));

        // LANG-521
        val = "2.";
        assertTrue("isNumber(String) LANG-521 failed", NumberUtils.isNumber(val));

        // LANG-664
        val = "1.1L";
        assertFalse("isNumber(String) LANG-664 failed", NumberUtils.isNumber(val));
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.lang3.math.NumberUtilsTest::testIsNumber
junit.framework.AssertionFailedError: isNumber(String) LANG-664 failed
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

SYNTHESIZED RELATION CANDIDATES (mechanically pre-screened) — invariants/metamorphic relations derived from the documented contract of the patched code. Each was compiled and exercised over many fuzzed inputs on the current build; candidates that fired indiscriminately were dropped as out-of-domain. They are STRONG CANDIDATES, not verified ground truth: implement each one, keep its justification comment next to the check, and fence its inputs exactly as instructed below. These generalise BEYOND the tested inputs, so they can catch an overfit that only special-cased the reported input. Implement each one in your harness in addition to the lifted/mined oracles — but these are EXTRAS, not your whole job: KEEP INVENTING YOUR OWN checks beyond them. Two self-invented shapes have repeatedly been the ones that catch what nothing else does: (a) HIDDEN-STATE checks — after any call documented as a question (get*/is*/contains/indexOf/size, no mention of modifying), re-read the object's cheap observable properties (capacity, size, length, later lookups) and assert they did not change; a 'read-only' call that silently mutates internal state is a classic defect no provided relation will mention. (b) SIBLING-AGREEMENT checks — two methods documented to do the same job (overloads taking char vs String, Class vs class name, etc.) must agree on the same logical input:
<relation name="decimal_with_long_suffix_is_rejected" kind="metamorphic">
  // holds because: For NumberUtils.isNumber(String), a decimal form with a decimal point is accepted as a number, but adding an 'L'/'l' long type qualifier to such a decimal form is invalid. This is the parser rule exercised by the changed branch and pinned by the trusted failing test case LANG-664: isNumber("1.1L") must be false.
  // valid input: Construct a valid decimal string of the form optional '-' + digits + '.' + digits, with no exponent and no existing type qualifier. Then compare it with the same string plus an 'L' suffix.
  // screen: DIRECTION-CONFIRMED: fires on the failing test's own inputs (buggy violates the pinned behaviour there); 20000/20000 on random inputs
String base;
try {
    int whole = data.consumeInt(0, 1000000);
    int frac = data.consumeInt(0, 1000000);
    boolean neg = data.consumeBoolean();
    base = (neg ? "-" : "") + whole + "." + frac;
} catch (Exception e) {
    return;
}
boolean plain;
try {
    plain = NumberUtils.isNumber(base);
} catch (Exception e) { for (StackTraceElement __rpgf : e.getStackTrace()) { String __rpgc = __rpgf.getClassName(); if ((__rpgc.equals("NumberUtils") || __rpgc.endsWith(".NumberUtils") || __rpgc.endsWith("$NumberUtils")) && !"<init>".equals(__rpgf.getMethodName())) { boolean __rpgdoc = false; if (!__rpgdoc && (__rpgf.getClassName().equals("NumberUtils") || __rpgf.getClassName().endsWith(".NumberUtils") || __rpgf.getClassName().endsWith("$NumberUtils")) && "createBigDecimal".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("NumberFormatException") || __rpn.endsWith(".NumberFormatException") || __rpn.endsWith("$NumberFormatException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("NumberUtils") || __rpgf.getClassName().endsWith(".NumberUtils") || __rpgf.getClassName().endsWith("$NumberUtils")) && "createBigInteger".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("NumberFormatException") || __rpn.endsWith(".NumberFormatException") || __rpn.endsWith("$NumberFormatException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("NumberUtils") || __rpgf.getClassName().endsWith(".NumberUtils") || __rpgf.getClassName().endsWith("$NumberUtils")) && "createDouble".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("NumberFormatException") || __rpn.endsWith(".NumberFormatException") || __rpn.endsWith("$NumberFormatException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("NumberUtils") || __rpgf.getClassName().endsWith(".NumberUtils") || __rpgf.getClassName().endsWith("$NumberUtils")) && "createFloat".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("NumberFormatException") || __rpn.endsWith(".NumberFormatException") || __rpn.endsWith("$NumberFormatException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("NumberUtils") || __rpgf.getClassName().endsWith(".NumberUtils") || __rpgf.getClassName().endsWith("$NumberUtils")) && "createInteger".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("NumberFormatException") || __rpn.endsWith(".NumberFormatException") || __rpn.endsWith("$NumberFormatException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("NumberUtils") || __rpgf.getClassName().endsWith(".NumberUtils") || __rpgf.getClassName().endsWith("$NumberUtils")) && "createLong".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("NumberFormatException") || __rpn.endsWith(".NumberFormatException") || __rpn.endsWith("$NumberFormatException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("NumberUtils") || __rpgf.getClassName().endsWith(".NumberUtils") || __rpgf.getClassName().endsWith("$NumberUtils")) && "createNumber".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("NumberFormatException") || __rpn.endsWith(".NumberFormatException") || __rpn.endsWith("$NumberFormatException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("NumberUtils") || __rpgf.getClassName().endsWith(".NumberUtils") || __rpgf.getClassName().endsWith("$NumberUtils")) && "max".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IllegalArgumentException") || __rpn.endsWith(".IllegalArgumentException") || __rpn.endsWith("$IllegalArgumentException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("NumberUtils") || __rpgf.getClassName().endsWith(".NumberUtils") || __rpgf.getClassName().endsWith("$NumberUtils")) && "min".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IllegalArgumentException") || __rpn.endsWith(".IllegalArgumentException") || __rpn.endsWith("$IllegalArgumentException")) { __rpgdoc = true; break; } } } if (__rpgdoc) return; break; } }
    throw new RuntimeException("relation decimal_with_long_suffix_is_rejected violated: unexpected " + e.getClass().getName() + " on valid decimal input " + base + ": " + e.getMessage());
}
boolean withLong;
try {
    withLong = NumberUtils.isNumber(base + "L");
} catch (Exception e) { for (StackTraceElement __rpgf : e.getStackTrace()) { String __rpgc = __rpgf.getClassName(); if ((__rpgc.equals("NumberUtils") || __rpgc.endsWith(".NumberUtils") || __rpgc.endsWith("$NumberUtils")) && !"<init>".equals(__rpgf.getMethodName())) { boolean __rpgdoc = false; if (!__rpgdoc && (__rpgf.getClassName().equals("NumberUtils") || __rpgf.getClassName().endsWith(".NumberUtils") || __rpgf.getClassName().endsWith("$NumberUtils")) && "createBigDecimal".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("NumberFormatException") || __rpn.endsWith(".NumberFormatException") || __rpn.endsWith("$NumberFormatException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("NumberUtils") || __rpgf.getClassName().endsWith(".NumberUtils") || __rpgf.getClassName().endsWith("$NumberUtils")) && "createBigInteger".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("NumberFormatException") || __rpn.endsWith(".NumberFormatException") || __rpn.endsWith("$NumberFormatException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("NumberUtils") || __rpgf.getClassName().endsWith(".NumberUtils") || __rpgf.getClassName().endsWith("$NumberUtils")) && "createDouble".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("NumberFormatException") || __rpn.endsWith(".NumberFormatException") || __rpn.endsWith("$NumberFormatException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("NumberUtils") || __rpgf.getClassName().endsWith(".NumberUtils") || __rpgf.getClassName().endsWith("$NumberUtils")) && "createFloat".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("NumberFormatException") || __rpn.endsWith(".NumberFormatException") || __rpn.endsWith("$NumberFormatException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("NumberUtils") || __rpgf.getClassName().endsWith(".NumberUtils") || __rpgf.getClassName().endsWith("$NumberUtils")) && "createInteger".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("NumberFormatException") || __rpn.endsWith(".NumberFormatException") || __rpn.endsWith("$NumberFormatException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("NumberUtils") || __rpgf.getClassName().endsWith(".NumberUtils") || __rpgf.getClassName().endsWith("$NumberUtils")) && "createLong".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("NumberFormatException") || __rpn.endsWith(".NumberFormatException") || __rpn.endsWith("$NumberFormatException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("NumberUtils") || __rpgf.getClassName().endsWith(".NumberUtils") || __rpgf.getClassName().endsWith("$NumberUtils")) && "createNumber".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("NumberFormatException") || __rpn.endsWith(".NumberFormatException") || __rpn.endsWith("$NumberFormatException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("NumberUtils") || __rpgf.getClassName().endsWith(".NumberUtils") || __rpgf.getClassName().endsWith("$NumberUtils")) && "max".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IllegalArgumentException") || __rpn.endsWith(".IllegalArgumentException") || __rpn.endsWith("$IllegalArgumentException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("NumberUtils") || __rpgf.getClassName().endsWith(".NumberUtils") || __rpgf.getClassName().endsWith("$NumberUtils")) && "min".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IllegalArgumentException") || __rpn.endsWith(".IllegalArgumentException") || __rpn.endsWith("$IllegalArgumentException")) { __rpgdoc = true; break; } } } if (__rpgdoc) return; break; } }
    throw new RuntimeException("relation decimal_with_long_suffix_is_rejected violated: unexpected " + e.getClass().getName() + " on valid-by-construction suffixed input " + base + "L: " + e.getMessage());
}
if (!plain) {
    throw new RuntimeException("relation decimal_with_long_suffix_is_rejected violated: plain decimal should be accepted but isNumber(" + base + ") was false");
}
if (withLong) {
    throw new RuntimeException("relation decimal_with_long_suffix_is_rejected violated: decimal with long suffix should be rejected but isNumber(" + base + "L) was true");
}
</relation>
<relation name="accepted_plain_decimal_has_creatable_number" kind="invariant">
  // holds because: The trusted test couples NumberUtils.isNumber(String) with NumberUtils.createNumber(String): for accepted numeric strings such as plain decimal forms, createNumber must succeed and return a non-null Number.
  // valid input: Construct a valid plain decimal string of the form optional '-' + digits + '.' + digits, with no exponent and no type qualifier.
  // screen: fired on 0/20000 fuzzed inputs on the buggy build (silent on the buggy build)
String s;
try {
    int whole = data.consumeInt(0, 1000000);
    int frac = data.consumeInt(0, 1000000);
    s = (data.consumeBoolean() ? "-" : "") + whole + "." + frac;
} catch (Exception e) {
    return;
}
boolean ok;
try {
    ok = NumberUtils.isNumber(s);
} catch (Exception e) { for (StackTraceElement __rpgf : e.getStackTrace()) { String __rpgc = __rpgf.getClassName(); if ((__rpgc.equals("NumberUtils") || __rpgc.endsWith(".NumberUtils") || __rpgc.endsWith("$NumberUtils")) && !"<init>".equals(__rpgf.getMethodName())) { boolean __rpgdoc = false; if (!__rpgdoc && (__rpgf.getClassName().equals("NumberUtils") || __rpgf.getClassName().endsWith(".NumberUtils") || __rpgf.getClassName().endsWith("$NumberUtils")) && "createBigDecimal".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("NumberFormatException") || __rpn.endsWith(".NumberFormatException") || __rpn.endsWith("$NumberFormatException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("NumberUtils") || __rpgf.getClassName().endsWith(".NumberUtils") || __rpgf.getClassName().endsWith("$NumberUtils")) && "createBigInteger".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("NumberFormatException") || __rpn.endsWith(".NumberFormatException") || __rpn.endsWith("$NumberFormatException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("NumberUtils") || __rpgf.getClassName().endsWith(".NumberUtils") || __rpgf.getClassName().endsWith("$NumberUtils")) && "createDouble".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("NumberFormatException") || __rpn.endsWith(".NumberFormatException") || __rpn.endsWith("$NumberFormatException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("NumberUtils") || __rpgf.getClassName().endsWith(".NumberUtils") || __rpgf.getClassName().endsWith("$NumberUtils")) && "createFloat".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("NumberFormatException") || __rpn.endsWith(".NumberFormatException") || __rpn.endsWith("$NumberFormatException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("NumberUtils") || __rpgf.getClassName().endsWith(".NumberUtils") || __rpgf.getClassName().endsWith("$NumberUtils")) && "createInteger".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("NumberFormatException") || __rpn.endsWith(".NumberFormatException") || __rpn.endsWith("$NumberFormatException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("NumberUtils") || __rpgf.getClassName().endsWith(".NumberUtils") || __rpgf.getClassName().endsWith("$NumberUtils")) && "createLong".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("NumberFormatException") || __rpn.endsWith(".NumberFormatException") || __rpn.endsWith("$NumberFormatException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("NumberUtils") || __rpgf.getClassName().endsWith(".NumberUtils") || __rpgf.getClassName().endsWith("$NumberUtils")) && "createNumber".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("NumberFormatException") || __rpn.endsWith(".NumberFormatException") || __rpn.endsWith("$NumberFormatException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("NumberUtils") || __rpgf.getClassName().endsWith(".NumberUtils") || __rpgf.getClassName().endsWith("$NumberUtils")) && "max".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IllegalArgumentException") || __rpn.endsWith(".IllegalArgumentException") || __rpn.endsWith("$IllegalArgumentException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("NumberUtils") || __rpgf.getClassName().endsWith(".NumberUtils") || __rpgf.getClassName().endsWith("$NumberUtils")) && "min".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IllegalArgumentException") || __rpn.endsWith(".IllegalArgumentException") || __rpn.endsWith("$IllegalArgumentException")) { __rpgdoc = true; break; } } } if (__rpgdoc) return; break; } }
    throw new RuntimeException("relation accepted_plain_decimal_has_creatable_number violated: unexpected " + e.getClass().getName() + " from isNumber(" + s + "): " + e.getMessage());
}
if (!ok) {
    throw new RuntimeException("relation accepted_plain_decimal_has_creatable_number violated: valid plain decimal was rejected by isNumber(" + s + ")");
}
Number n;
try {
    n = NumberUtils.createNumber(s);
} catch (Exception e) { for (StackTraceElement __rpgf : e.getStackTrace()) { String __rpgc = __rpgf.getClassName(); if ((__rpgc.equals("NumberUtils") || __rpgc.endsWith(".NumberUtils") || __rpgc.endsWith("$NumberUtils")) && !"<init>".equals(__rpgf.getMethodName())) { boolean __rpgdoc = false; if (!__rpgdoc && (__rpgf.getClassName().equals("NumberUtils") || __rpgf.getClassName().endsWith(".NumberUtils") || __rpgf.getClassName().endsWith("$NumberUtils")) && "createBigDecimal".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("NumberFormatException") || __rpn.endsWith(".NumberFormatException") || __rpn.endsWith("$NumberFormatException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("NumberUtils") || __rpgf.getClassName().endsWith(".NumberUtils") || __rpgf.getClassName().endsWith("$NumberUtils")) && "createBigInteger".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("NumberFormatException") || __rpn.endsWith(".NumberFormatException") || __rpn.endsWith("$NumberFormatException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("NumberUtils") || __rpgf.getClassName().endsWith(".NumberUtils") || __rpgf.getClassName().endsWith("$NumberUtils")) && "createDouble".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("NumberFormatException") || __rpn.endsWith(".NumberFormatException") || __rpn.endsWith("$NumberFormatException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("NumberUtils") || __rpgf.getClassName().endsWith(".NumberUtils") || __rpgf.getClassName().endsWith("$NumberUtils")) && "createFloat".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("NumberFormatException") || __rpn.endsWith(".NumberFormatException") || __rpn.endsWith("$NumberFormatException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("NumberUtils") || __rpgf.getClassName().endsWith(".NumberUtils") || __rpgf.getClassName().endsWith("$NumberUtils")) && "createInteger".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("NumberFormatException") || __rpn.endsWith(".NumberFormatException") || __rpn.endsWith("$NumberFormatException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("NumberUtils") || __rpgf.getClassName().endsWith(".NumberUtils") || __rpgf.getClassName().endsWith("$NumberUtils")) && "createLong".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("NumberFormatException") || __rpn.endsWith(".NumberFormatException") || __rpn.endsWith("$NumberFormatException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("NumberUtils") || __rpgf.getClassName().endsWith(".NumberUtils") || __rpgf.getClassName().endsWith("$NumberUtils")) && "createNumber".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("NumberFormatException") || __rpn.endsWith(".NumberFormatException") || __rpn.endsWith("$NumberFormatException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("NumberUtils") || __rpgf.getClassName().endsWith(".NumberUtils") || __rpgf.getClassName().endsWith("$NumberUtils")) && "max".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IllegalArgumentException") || __rpn.endsWith(".IllegalArgumentException") || __rpn.endsWith("$IllegalArgumentException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("NumberUtils") || __rpgf.getClassName().endsWith(".NumberUtils") || __rpgf.getClassName().endsWith("$NumberUtils")) && "min".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IllegalArgumentException") || __rpn.endsWith(".IllegalArgumentException") || __rpn.endsWith("$IllegalArgumentException")) { __rpgdoc = true; break; } } } if (__rpgdoc) return; break; } }
    throw new RuntimeException("relation accepted_plain_decimal_has_creatable_number violated: unexpected " + e.getClass().getName() + " from createNumber(" + s + "): " + e.getMessage());
}
if (n == null) {
    throw new RuntimeException("relation accepted_plain_decimal_has_creatable_number violated: createNumber(" + s + ") returned null for an accepted number string");
}
</relation>
Build each relation's input with the FuzzedDataProvider so it is VALID BY CONSTRUCTION, wrap the API calls in try/catch and SKIP (return) on any caught exception — an exception is a rejection, never a violation. Throw com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("relation <name> violated: ...") only on a genuine disagreement, and include the observed values in the message. Two structural rules (checked mechanically, violations are rejected): (1) the violation throw goes OUTSIDE/AFTER the try/catch that wraps the API calls — thrown inside your own catch-everything block it is caught and discarded, dead code; (2) if you ever convert a CAUGHT exception into your own alarm, pass it as the cause: new FuzzerSecurityIssueLow("...", e) — never bury it in the message text.

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
<details open><summary>▸ Output (~7,987 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String[] positive = new String[] {
            "12345",
            "1234.5",
            ".12345",
            "1234E5",
            "1234E+5",
            "1234E-5",
            "123.4E5",
            "-1234",
            "-1234.5",
            "-.12345",
            "-1234E5",
            "0",
            "-0",
            "01234",
            "-01234",
            "0xABC123",
            "0x0",
            "123.4E21D",
            "-221.23F",
            "22338L"
        };

        String[] negative = new String[] {
            null,
            "",
            "--2.3",
            ".12.3",
            "-123E",
            "-123E+-212",
            "-123E2.12",
            "0xGF",
            "0xFAE-1",
            ".",
            "-0ABC123",
            "123.4E-D",
            "123.4ED",
            "1234E5l",
            "11a",
            "1a",
            "a",
            "11g",
            "11z",
            "11def",
            "11d11",
            "11 11",
            " 1111",
            "1111 "
        };

        for (int i = 0; i < positive.length; i++) {
            String val = positive[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (actualIsNumber != true) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-isNumber-pos-" + (i + 1) + "] semantic mismatch: NumberUtils.isNumber(" + quote(val) + ") expected=true actual=" + actualIsNumber);
            }
            boolean actualCreateNumber = checkCreateNumber(val);
            if (actualCreateNumber != true) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-createNumber-pos-" + (i + 1) + "] semantic mismatch: checkCreateNumber(" + quote(val) + ") expected=true actual=" + actualCreateNumber);
            }
        }

        for (int i = 0; i < negative.length; i++) {
            String val = negative[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (actualIsNumber != false) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-isNumber-neg-" + (i + 1) + "] semantic mismatch: NumberUtils.isNumber(" + quote(val) + ") expected=false actual=" + actualIsNumber);
            }
            boolean actualCreateNumber = checkCreateNumber(val);
            if (actualCreateNumber != false) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-createNumber-neg-" + (i + 1) + "] semantic mismatch: checkCreateNumber(" + quote(val) + ") expected=false actual=" + actualCreateNumber);
            }
        }

        {
            String val = "2.";
            boolean actual = NumberUtils.isNumber(val);
            if (actual != true) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-lang521] semantic mismatch: NumberUtils.isNumber(\"2.\") expected=true actual=" + actual);
            }
        }

        {
            String val = "1.1L";
            boolean actual = NumberUtils.isNumber(val);
            if (actual != false) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-lang664] semantic mismatch: NumberUtils.isNumber(\"1.1L\") expected=false actual=" + actual);
            }
        }

        {
            int whole;
            int frac;
            boolean neg;
            try {
                whole = data.consumeInt(0, 1000000);
                frac = data.consumeInt(0, 1000000);
                neg = data.consumeBoolean();
            } catch (Throwable t) {
                return;
            }
            String base = (neg ? "-" : "") + whole + "." + frac;
            boolean plain;
            boolean withLong;
            try {
                plain = NumberUtils.isNumber(base);
                withLong = NumberUtils.isNumber(base + "L");
            } catch (Throwable t) {
                return;
            }
            // Contract pinned by LANG-664 and the parser code: a valid decimal may be accepted,
            // but adding an L/l long qualifier to a decimal-point form must make it invalid.
            // A patch that merely dodges the buggy branch would violate this observable result.
            if (!plain) {
                throw new FuzzerSecurityIssueLow("[oracle:relation-decimal-long-rejected] relation decimal_with_long_suffix_is_rejected violated: plain decimal should be accepted but isNumber(" + quote(base) + ") was false");
            }
            if (withLong) {
                throw new FuzzerSecurityIssueLow("[oracle:relation-decimal-long-rejected] relation decimal_with_long_suffix_is_rejected violated: decimal with long suffix should be rejected but isNumber(" + quote(base + "L") + ") was true");
            }
        }

        {
            int whole;
            int frac;
            boolean neg;
            try {
                whole = data.consumeInt(0, 1000000);
                frac = data.consumeInt(0, 1000000);
                neg = data.consumeBoolean();
            } catch (Throwable t) {
                return;
            }
            String s = (neg ? "-" : "") + whole + "." + frac;
            boolean ok;
            Number n;
            try {
                ok = NumberUtils.isNumber(s);
                n = NumberUtils.createNumber(s);
            } catch (Throwable t) {
                return;
            }
            // Trusted from the lifted test: for accepted plain decimal forms, createNumber succeeds
            // and returns non-null; deleting or bypassing real parsing work would break this pairing.
            if (!ok) {
                throw new FuzzerSecurityIssueLow("[oracle:relation-accepted-decimal-creatable] relation accepted_plain_decimal_has_creatable_number violated: valid plain decimal was rejected by isNumber(" + quote(s) + ")");
            }
            if (n == null) {
                throw new FuzzerSecurityIssueLow("[oracle:relation-accepted-decimal-creatable] relation accepted_plain_decimal_has_creatable_number violated: createNumber(" + quote(s) + ") returned null for an accepted number string");
            }
        }

        {
            int whole;
            int frac;
            boolean neg;
            try {
                whole = data.consumeInt(0, 1000000);
                frac = data.consumeInt(0, 1000000);
                neg = data.consumeBoolean();
            } catch (Throwable t) {
                return;
            }
            String base = (neg ? "-" : "") + whole + "." + frac;
            boolean upper;
            boolean lower;
            try {
                upper = NumberUtils.isNumber(base + "L");
                lower = NumberUtils.isNumber(base + "l");
            } catch (Throwable t) {
                return;
            }
            // The implementation explicitly treats 'l' and 'L' identically in the same branch,
            // so equivalent inputs differing only in qualifier case must agree.
            if (upper != lower) {
                throw new FuzzerSecurityIssueLow("[oracle:relation-lower-upper-long-agree] metamorphic violation: isNumber(" + quote(base + "L") + ")=" + upper + " but isNumber(" + quote(base + "l") + ")=" + lower);
            }
        }
    }

    private static boolean checkCreateNumber(String val) {
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

    private static String quote(String s) {
        if (s == null) {
            return "null";
        }
        return "\"" + s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t").replace("\"", "\\\"") + "\"";
    }
}
```

</details>

---
## [37] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Every oracle alarm in your harness needs a name: the alarm near line 62 has no oracle ID (`throw new FuzzerSecurityIssueLow("[oracle:lifted-isNumber-pos-" + (i + 1) + "] semantic mismatch: NumberUtils

---
## [38] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~8,728 chars, 2 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [36] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String[] positive = new String[] {
            "12345",
            "1234.5",
            ".12345",
            "1234E5",
            "1234E+5",
            "1234E-5",
            "123.4E5",
            "-1234",
            "-1234.5",
            "-.12345",
            "-1234E5",
            "0",
            "-0",
            "01234",
            "-01234",
            "0xABC123",
            "0x0",
            "123.4E21D",
            "-221.23F",
            "22338L"
        };

        String[] negative = new String[] {
            null,
            "",
            "--2.3",
            ".12.3",
            "-123E",
            "-123E+-212",
            "-123E2.12",
            "0xGF",
            "0xFAE-1",
            ".",
            "-0ABC123",
            "123.4E-D",
            "123.4ED",
            "1234E5l",
            "11a",
            "1a",
            "a",
            "11g",
            "11z",
            "11def",
            "11d11",
            "11 11",
            " 1111",
            "1111 "
        };

        for (int i = 0; i < positive.length; i++) {
            String val = positive[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (actualIsNumber != true) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-isNumber-pos-" + (i + 1) + "] semantic mismatch: NumberUtils.isNumber(" + quote(val) + ") expected=true actual=" + actualIsNumber);
            }
            boolean actualCreateNumber = checkCreateNumber(val);
            if (actualCreateNumber != true) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-createNumber-pos-" + (i + 1) + "] semantic mismatch: checkCreateNumber(" + quote(val) + ") expected=true actual=" + actualCreateNumber);
            }
        }

        for (int i = 0; i < negative.length; i++) {
            String val = negative[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (actualIsNumber != false) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-isNumber-neg-" + (i + 1) + "] semantic mismatch: NumberUtils.isNumber(" + quote(val) + ") expected=false actual=" + actualIsNumber);
            }
            boolean actualCreateNumber = checkCreateNumber(val);
            if (actualCreateNumber != false) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-createNumber-neg-" + (i + 1) + "] semantic mismatch: checkCreateNumber(" + quote(val) + ") expected=false actual=" + actualCreateNumber);
            }
        }

        {
            String val = "2.";
            boolean actual = NumberUtils.isNumber(val);
            if (actual != true) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-lang521] semantic mismatch: NumberUtils.isNumber(\"2.\") expected=true actual=" + actual);
            }
        }

        {
            String val = "1.1L";
            boolean actual = NumberUtils.isNumber(val);
            if (actual != false) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-lang664] semantic mismatch: NumberUtils.isNumber(\"1.1L\") expected=false actual=" + actual);
            }
        }

        {
            int whole;
            int frac;
            boolean neg;
            try {
                whole = data.consumeInt(0, 1000000);
                frac = data.consumeInt(0, 1000000);
                neg = data.consumeBoolean();
            } catch (Throwable t) {
                return;
            }
            String base = (neg ? "-" : "") + whole + "." + frac;
            boolean plain;
            boolean withLong;
            try {
                plain = NumberUtils.isNumber(base);
                withLong = NumberUtils.isNumber(base + "L");
            } catch (Throwable t) {
                return;
            }
            // Contract pinned by LANG-664 and the parser code: a valid decimal may be accepted,
            // but adding an L/l long qualifier to a decimal-point form must make it invalid.
            // A patch that merely dodges the buggy branch would violate this observable result.
            if (!plain) {
                throw new FuzzerSecurityIssueLow("[oracle:relation-decimal-long-rejected] relation decimal_with_long_suffix_is_rejected violated: plain decimal should be accepted but isNumber(" + quote(base) + ") was false");
            }
            if (withLong) {
                throw new FuzzerSecurityIssueLow("[oracle:relation-decimal-long-rejected] relation decimal_with_long_suffix_is_rejected violated: decimal with long suffix should be rejected but isNumber(" + quote(base + "L") + ") was true");
            }
        }

        {
            int whole;
            int frac;
            boolean neg;
            try {
                whole = data.consumeInt(0, 1000000);
                frac = data.consumeInt(0, 1000000);
                neg = data.consumeBoolean();
            } catch (Throwable t) {
                return;
            }
            String s = (neg ? "-" : "") + whole + "." + frac;
            boolean ok;
            Number n;
            try {
                ok = NumberUtils.isNumber(s);
                n = NumberUtils.createNumber(s);
            } catch (Throwable t) {
                return;
            }
            // Trusted from the lifted test: for accepted plain decimal forms, createNumber succeeds
            // and returns non-null; deleting or bypassing real parsing work would break this pairing.
            if (!ok) {
                throw new FuzzerSecurityIssueLow("[oracle:relation-accepted-decimal-creatable] relation accepted_plain_decimal_has_creatable_number violated: valid plain decimal was rejected by isNumber(" + quote(s) + ")");
            }
            if (n == null) {
                throw new FuzzerSecurityIssueLow("[oracle:relation-accepted-decimal-creatable] relation accepted_plain_decimal_has_creatable_number violated: createNumber(" + quote(s) + ") returned null for an accepted number string");
            }
        }

        {
            int whole;
            int frac;
            boolean neg;
            try {
                whole = data.consumeInt(0, 1000000);
                frac = data.consumeInt(0, 1000000);
                neg = data.consumeBoolean();
            } catch (Throwable t) {
                return;
            }
            String base = (neg ? "-" : "") + whole + "." + frac;
            boolean upper;
            boolean lower;
            try {
                upper = NumberUtils.isNumber(base + "L");
                lower = NumberUtils.isNumber(base + "l");
            } catch (Throwable t) {
                return;
            }
            // The implementation explicitly treats 'l' and 'L' identically in the same branch,
            // so equivalent inputs differing only in qualifier case must agree.
            if (upper != lower) {
                throw new FuzzerSecurityIssueLow("[oracle:relation-lower-upper-long-agree] metamorphic violation: isNumber(" + quote(base + "L") + ")=" + upper + " but isNumber(" + quote(base + "l") + ")=" + lower);
            }
        }
    }

    private static boolean checkCreateNumber(String val) {
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

    private static String quote(String s) {
        if (s == null) {
            return "null";
        }
        return "\"" + s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t").replace("\"", "\\\"") + "\"";
    }
}
```
**[user]**
```
Every oracle alarm in your harness needs a name: the alarm near line 62 has no oracle ID (`throw new FuzzerSecurityIssueLow("[oracle:lifted-isNumber-pos-" + (i + 1) + "] semantic mismatch: NumberUtils.isNumber("...`) — start its message with "[oracle:<short-id>]" so acceptance can tell which check earned its place
Start each distinct check's message with [oracle:<short-id>], e.g. `throw new FuzzerSecurityIssueLow("[oracle:round-trip] semantic mismatch: ...")`. Use a DIFFERENT id per check. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~7,893 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String[] positive = new String[] {
            "12345",
            "1234.5",
            ".12345",
            "1234E5",
            "1234E+5",
            "1234E-5",
            "123.4E5",
            "-1234",
            "-1234.5",
            "-.12345",
            "-1234E5",
            "0",
            "-0",
            "01234",
            "-01234",
            "0xABC123",
            "0x0",
            "123.4E21D",
            "-221.23F",
            "22338L"
        };

        String[] negative = new String[] {
            null,
            "",
            "--2.3",
            ".12.3",
            "-123E",
            "-123E+-212",
            "-123E2.12",
            "0xGF",
            "0xFAE-1",
            ".",
            "-0ABC123",
            "123.4E-D",
            "123.4ED",
            "1234E5l",
            "11a",
            "1a",
            "a",
            "11g",
            "11z",
            "11def",
            "11d11",
            "11 11",
            " 1111",
            "1111 "
        };

        for (int i = 0; i < positive.length; i++) {
            String val = positive[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (actualIsNumber != true) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-isNumber-positive] semantic mismatch: caseIndex=" + i + " NumberUtils.isNumber(" + quote(val) + ") expected=true actual=" + actualIsNumber);
            }
            boolean actualCreateNumber = checkCreateNumber(val);
            if (actualCreateNumber != true) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-createNumber-positive] semantic mismatch: caseIndex=" + i + " checkCreateNumber(" + quote(val) + ") expected=true actual=" + actualCreateNumber);
            }
        }

        for (int i = 0; i < negative.length; i++) {
            String val = negative[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (actualIsNumber != false) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-isNumber-negative] semantic mismatch: caseIndex=" + i + " NumberUtils.isNumber(" + quote(val) + ") expected=false actual=" + actualIsNumber);
            }
            boolean actualCreateNumber = checkCreateNumber(val);
            if (actualCreateNumber != false) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-createNumber-negative] semantic mismatch: caseIndex=" + i + " checkCreateNumber(" + quote(val) + ") expected=false actual=" + actualCreateNumber);
            }
        }

        {
            String val = "2.";
            boolean actual = NumberUtils.isNumber(val);
            if (actual != true) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-lang521] semantic mismatch: NumberUtils.isNumber(\"2.\") expected=true actual=" + actual);
            }
        }

        {
            String val = "1.1L";
            boolean actual = NumberUtils.isNumber(val);
            if (actual != false) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-lang664] semantic mismatch: NumberUtils.isNumber(\"1.1L\") expected=false actual=" + actual);
            }
        }

        {
            int whole;
            int frac;
            boolean neg;
            try {
                whole = data.consumeInt(0, 1000000);
                frac = data.consumeInt(0, 1000000);
                neg = data.consumeBoolean();
            } catch (Throwable t) {
                return;
            }
            String base = (neg ? "-" : "") + whole + "." + frac;
            boolean plain;
            boolean withLong;
            try {
                plain = NumberUtils.isNumber(base);
                withLong = NumberUtils.isNumber(base + "L");
            } catch (Throwable t) {
                return;
            }
            // Contract pinned by LANG-664 and the parser code: a valid decimal may be accepted,
            // but adding an L/l long qualifier to a decimal-point form must make it invalid.
            // A patch that merely dodges the buggy branch would violate this observable result.
            if (!plain) {
                throw new FuzzerSecurityIssueLow("[oracle:relation-decimal-long-rejected-plain] semantic mismatch: plain decimal should be accepted but isNumber(" + quote(base) + ") was false");
            }
            if (withLong) {
                throw new FuzzerSecurityIssueLow("[oracle:relation-decimal-long-rejected-suffixed] semantic mismatch: decimal with long suffix should be rejected but isNumber(" + quote(base + "L") + ") was true");
            }
        }

        {
            int whole;
            int frac;
            boolean neg;
            try {
                whole = data.consumeInt(0, 1000000);
                frac = data.consumeInt(0, 1000000);
                neg = data.consumeBoolean();
            } catch (Throwable t) {
                return;
            }
            String s = (neg ? "-" : "") + whole + "." + frac;
            boolean ok;
            Number n;
            try {
                ok = NumberUtils.isNumber(s);
                n = NumberUtils.createNumber(s);
            } catch (Throwable t) {
                return;
            }
            // Trusted from the lifted test: for accepted plain decimal forms, createNumber succeeds
            // and returns non-null; deleting or bypassing real parsing work would break this pairing.
            if (!ok) {
                throw new FuzzerSecurityIssueLow("[oracle:relation-accepted-decimal-creatable-accepted] semantic mismatch: valid plain decimal was rejected by isNumber(" + quote(s) + ")");
            }
            if (n == null) {
                throw new FuzzerSecurityIssueLow("[oracle:relation-accepted-decimal-creatable-nonnull] semantic mismatch: createNumber(" + quote(s) + ") returned null for an accepted number string");
            }
        }

        {
            int whole;
            int frac;
            boolean neg;
            try {
                whole = data.consumeInt(0, 1000000);
                frac = data.consumeInt(0, 1000000);
                neg = data.consumeBoolean();
            } catch (Throwable t) {
                return;
            }
            String base = (neg ? "-" : "") + whole + "." + frac;
            boolean upper;
            boolean lower;
            try {
                upper = NumberUtils.isNumber(base + "L");
                lower = NumberUtils.isNumber(base + "l");
            } catch (Throwable t) {
                return;
            }
            // The implementation explicitly treats 'l' and 'L' identically in the same branch,
            // so equivalent inputs differing only in qualifier case must agree.
            if (upper != lower) {
                throw new FuzzerSecurityIssueLow("[oracle:relation-lower-upper-long-agree] metamorphic violation: isNumber(" + quote(base + "L") + ")=" + upper + " but isNumber(" + quote(base + "l") + ")=" + lower);
            }
        }
    }

    private static boolean checkCreateNumber(String val) {
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

    private static String quote(String s) {
        if (s == null) {
            return "null";
        }
        return "\"" + s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t").replace("\"", "\\\"") + "\"";
    }
}
```

</details>

---
## [39] ⚙️ harness-attempt · `attempt_002`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-lang664] semantic mismatch: NumberUtils.isNumber("1.1L") expected=false actual=true

---
## [40] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~51,783 chars, 1 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
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

Codebase: `Lang_24_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/apache/commons/lang3/math/NumberUtils.java
+++ /src/main/java/org/apache/commons/lang3/math/NumberUtils.java
@@ -1409,6 +1409,9 @@
             }
             if (chars[i] == 'l'
                 || chars[i] == 'L') {
+//ACS's patch begin
+	if (hasDecPoint==true){return false;}
+//ACS's patch end
                 // not allowing L with an exponent or decimal point
                 return foundDigit && !hasExp;
             }
@@ -1420,4 +1423,4 @@
         return !allowSigns && foundDigit;
     }
     
-}
+}
\ No newline at end of file

</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import java.math.BigDecimal;
import java.math.BigInteger;
import org.apache.commons.lang3.StringUtils;
</source_imports>

Function `isNumber`:
<signature>
public static boolean isNumber(String str)
</signature>
<code>
    public static boolean isNumber(String str) {
        if (StringUtils.isEmpty(str)) {
            return false;
        }
        char[] chars = str.toCharArray();
        int sz = chars.length;
        boolean hasExp = false;
        boolean hasDecPoint = false;
        boolean allowSigns = false;
        boolean foundDigit = false;
        // deal with any possible sign up front
        int start = (chars[0] == '-') ? 1 : 0;
        if (sz > start + 1) {
            if (chars[start] == '0' && chars[start + 1] == 'x') {
                int i = start + 2;
                if (i == sz) {
                    return false; // str == "0x"
                }
                // checking hex (it can't be anything else)
                for (; i < chars.length; i++) {
                    if ((chars[i] < '0' || chars[i] > '9')
                        && (chars[i] < 'a' || chars[i] > 'f')
                        && (chars[i] < 'A' || chars[i] > 'F')) {
                        return false;
                    }
                }
                return true;
            }
        }
        sz--; // don't want to loop to the last char, check it afterwords
              // for type qualifiers
        int i = start;
        // loop to the next to last char or to the last char if we need another digit to
        // make a valid number (e.g. chars[0..5] = "1234E")
        while (i < sz || (i < sz + 1 && allowSigns && !foundDigit)) {
            if (chars[i] >= '0' && chars[i] <= '9') {
                foundDigit = true;
                allowSigns = false;

            } else if (chars[i] == '.') {
                if (hasDecPoint || hasExp) {
                    // two decimal points or dec in exponent   
                    return false;
                }
                hasDecPoint = true;
            } else if (chars[i] == 'e' || chars[i] == 'E') {
                // we've already taken care of hex.
                if (hasExp) {
                    // two E's
                    return false;
                }
                if (!foundDigit) {
                    return false;
                }
                hasExp = true;
                allowSigns = true;
            } else if (chars[i] == '+' || chars[i] == '-') {
                if (!allowSigns) {
                    return false;
                }
                allowSigns = false;
                foundDigit = false; // we need a digit after the E
            } else {
                return false;
            }
            i++;
        }
        if (i < chars.length) {
            if (chars[i] >= '0' && chars[i] <= '9') {
                // no type qualifier, OK
                return true;
            }
            if (chars[i] == 'e' || chars[i] == 'E') {
                // can't have an E at the last byte
                return false;
            }
            if (chars[i] == '.') {
                if (hasDecPoint || hasExp) {
                    // two decimal points or dec in exponent
                    return false;
                }
                // single trailing decimal point after non-exponent is ok
                return foundDigit;
            }
            if (!allowSigns
                && (chars[i] == 'd'
                    || chars[i] == 'D'
                    || chars[i] == 'f'
                    || chars[i] == 'F')) {
                return foundDigit;
            }
            if (chars[i] == 'l'
                || chars[i] == 'L') {
                // not allowing L with an exponent or decimal point
                return foundDigit && !hasExp;
            }
            // last character is illegal
            return false;
        }
        // allowSigns is true iff the val ends in 'E'
        // found digit it to make sure weird stuff like '.' and '1E-' doesn't pass
        return !allowSigns && foundDigit;
    }
</code>

CLASS-LEVEL CONTEXT — partial skeletons of the touched class(es): signatures, fields, javadoc; non-touched method bodies elided. Use them to find CROSS-MEMBER consistency oracles: sibling accessors that must agree with each other or with constructor arguments, overloads that must be equivalent, round-trips that must restore state. Assert only what a shown contract or invariant guarantees.
<class_skeletons>
<class name="NumberUtils" role="patched">
     * <p>Provides extra functionality for Java Number classes.</p>
     * @author Apache Software Foundation
     * @author <a href="mailto:rand_mcneely@yahoo.com">Rand McNeely</a>
     * @author <a href="mailto:steve.downey@netfolio.com">Steve Downey</a>
     * @author Eric Pugh
     * @author Phil Steitz
     * @author Matthew Hawthorne
     * @author <a href="mailto:ggregory@seagullsw.com">Gary Gregory</a>
     * @author <a href="mailto:fredrik@westermarck.com">Fredrik Westermarck</a>
     * @since 2.0
     * @version $Id$
public class NumberUtils {
     * Reusable Long constant for zero.
    public static final Long LONG_ZERO = new Long(0L);
     * Reusable Long constant for one.
    public static final Long LONG_ONE = new Long(1L);
     * Reusable Long constant for minus one.
    public static final Long LONG_MINUS_ONE = new Long(-1L);
     * Reusable Integer constant for zero.
    public static final Integer INTEGER_ZERO = new Integer(0);
     * Reusable Integer constant for one.
    public static final Integer INTEGER_ONE = new Integer(1);
     * Reusable Integer constant for minus one.
    public static final Integer INTEGER_MINUS_ONE = new Integer(-1);
     * Reusable Short constant for zero.
    public static final Short SHORT_ZERO = new Short((short) 0);
     * Reusable Short constant for one.
    public static final Short SHORT_ONE = new Short((short) 1);
     * Reusable Short constant for minus one.
    public static final Short SHORT_MINUS_ONE = new Short((short) -1);
     * Reusable Byte constant for zero.
    public static final Byte BYTE_ZERO = Byte.valueOf((byte) 0);
     * Reusable Byte constant for one.
    public static final Byte BYTE_ONE = Byte.valueOf((byte) 1);
     * Reusable Byte constant for minus one.
    public static final Byte BYTE_MINUS_ONE = Byte.valueOf((byte) -1);
     * Reusable Double constant for zero.
    public static final Double DOUBLE_ZERO = new Double(0.0d);
     * Reusable Double constant for one.
    public static final Double DOUBLE_ONE = new Double(1.0d);
     * Reusable Double constant for minus one.
    public static final Double DOUBLE_MINUS_ONE = new Double(-1.0d);
     * Reusable Float constant for zero.
    public static final Float FLOAT_ZERO = new Float(0.0f);
     * Reusable Float constant for one.
    public static final Float FLOAT_ONE = new Float(1.0f);
     * Reusable Float constant for minus one.
    public static final Float FLOAT_MINUS_ONE = new Float(-1.0f);
     * <p><code>NumberUtils</code> instances should NOT be constructed in standard programming.
     * Instead, the class should be used as <code>NumberUtils.toInt("6");</code>.</p>
     * <p>This constructor is public to permit tools that require a JavaBean instance
     * to operate.</p>
    public NumberUtils() {
        super();
    }
     * <p>Convert a <code>String</code> to an <code>int</code>, returning
     * <code>zero</code> if the conversion fails.</p>
     * <p>If the string is <code>null</code>, <code>zero</code> is returned.</p>
     * <pre>
     * NumberUtils.toInt(null) = 0
     * NumberUtils.toInt("")   = 0
     * NumberUtils.toInt("1")  = 1
     * </pre>
     * @param str  the string to convert, may be null
     * @return the int represented by the string, or <code>zero</code> if
     * conversion fails
     * @since 2.1
    public static int toInt(String str) { … }
     * <p>Convert a <code>String</code> to an <code>int</code>, returning a
     * default value if the conversion fails.</p>
     * <p>If the string is <code>null</code>, the default value is returned.</p>
     * <pre>
     * NumberUtils.toInt(null, 1) = 1
     * NumberUtils.toInt("", 1)   = 1
     * NumberUtils.toInt("1", 0)  = 1
     * </pre>
     * @param str  the string to convert, may be null
     * @param defaultValue  the default value
     * @return the int represented by the string, or the default if conversion fails
     * @since 2.1
    public static int toInt(String str, int defaultValue) { … }
     * <p>Convert a <code>String</code> to a <code>long</code>, returning
     * <code>zero</code> if the conversion fails.</p>
     * <p>If the string is <code>null</code>, <code>zero</code> is returned.</p>
     * <pre>
     * NumberUtils.toLong(null) = 0L
     * NumberUtils.toLong("")   = 0L
     * NumberUtils.toLong("1")  = 1L
     * </pre>
     * @param str  the string to convert, may be null
     * @return the long represented by the string, or <code>0</code> if
     * conversion fails
     * @since 2.1
    public static long toLong(String str) { … }
     * <p>Convert a <code>String</code> to a <code>long</code>, returning a
     * default value if the conversion fails.</p>
     * <p>If the string is <code>null</code>, the default value is returned.</p>
     * <pre>
     * NumberUtils.toLong(null, 1L) = 1L
     * NumberUtils.toLong("", 1L)   = 1L
     * NumberUtils.toLong("1", 0L)  = 1L
     * </pre>
     * @param str  the string to convert, may be null
     * @param defaultValue  the default value
     * @return the long represented by the string, or the default if conversion fails
     * @since 2.1
    public static long toLong(String str, long defaultValue) { … }
     * <p>Convert a <code>String</code> to a <code>float</code>, returning
     * <code>0.0f</code> if the conversion fails.</p>
     * <p>If the string <code>str</code> is <code>null</code>,
     * <code>0.0f</code> is returned.</p>
     * <pre>
     * NumberUtils.toFloat(null)   = 0.0f
     * NumberUtils.toFloat("")     = 0.0f
     * NumberUtils.toFloat("1.5")  = 1.5f
     * </pre>
     * @param str the string to convert, may be <code>null</code>
     * @return the float represented by the string, or <code>0.0f</code>
     * if conversion fails
     * …
    public static float toFloat(String str) { … }
     * <p>Convert a <code>String</code> to a <code>float</code>, returning a
     * default value if the conversion fails.</p>
     * <p>If the string <code>str</code> is <code>null</code>, the default
     * value is returned.</p>
     * <pre>
     * NumberUtils.toFloat(null, 1.1f)   = 1.0f
     * NumberUtils.toFloat("", 1.1f)     = 1.1f
     * NumberUtils.toFloat("1.5", 0.0f)  = 1.5f
     * </pre>
     * @param str the string to convert, may be <code>null</code>
     * @param defaultValue the default value
     * @return the float represented by the string, or defaultValue
     * …
    public static float toFloat(String str, float defaultValue) { … }
     * <p>Convert a <code>String</code> to a <code>double</code>, returning
     * <code>0.0d</code> if the conversion fails.</p>
     * <p>If the string <code>str</code> is <code>null</code>,
     * <code>0.0d</code> is returned.</p>
     * <pre>
     * NumberUtils.toDouble(null)   = 0.0d
     * NumberUtils.toDouble("")     = 0.0d
     * NumberUtils.toDouble("1.5")  = 1.5d
     * </pre>
     * @param str the string to convert, may be <code>null</code>
     * @return the double represented by the string, or <code>0.0d</code>
     * if conversion fails
     * …
    public static double toDouble(String str) { … }
     * <p>Convert a <code>String</code> to a <code>double</code>, returning a
     * default value if the conversion fails.</p>
     * <p>If the string <code>str</code> is <code>null</code>, the default
     * value is returned.</p>
     * <pre>
     * NumberUtils.toDouble(null, 1.1d)   = 1.1d
     * NumberUtils.toDouble("", 1.1d)     = 1.1d
     * NumberUtils.toDouble("1.5", 0.0d)  = 1.5d
     * </pre>
     * @param str the string to convert, may be <code>null</code>
     * @param defaultValue the default value
     * @return the double represented by the string, or defaultValue
     * …
    public static double toDouble(String str, double defaultValue) { … }
     * <p>Convert a <code>String</code> to a <code>byte</code>, returning
     * <code>zero</code> if the conversion fails.</p>
     * <p>If the string is <code>null</code>, <code>zero</code> is returned.</p>
     * <pre>
     * NumberUtils.toByte(null) = 0
     * NumberUtils.toByte("")   = 0
     * NumberUtils.toByte("1")  = 1
     * </pre>
     * @param str  the string to convert, may be null
     * @return the byte represented by the string, or <code>zero</code> if
     * conversion fails
     * @since 2.5
    public static byte toByte(String str) { … }
     * <p>Convert a <code>String</code> to a <code>byte</code>, returning a
     * default value if the conversion fails.</p>
     * <p>If the string is <code>null</code>, the default value is returned.</p>
     * <pre>
     * NumberUtils.toByte(null, 1) = 1
     * NumberUtils.toByte("", 1)   = 1
     * NumberUtils.toByte("1", 0)  = 1
     * </pre>
     * @param str  the string to convert, may be null
     * @param defaultValue  the default value
     * @return the byte represented by the string, or the default if conversion fails
     * @since 2.5
    public static byte toByte(String str, byte defaultValue) { … }
     * <p>Convert a <code>String</code> to a <code>short</code>, returning
     * <code>zero</code> if the conversion fails.</p>
     * <p>If the string is <code>null</code>, <code>zero</code> is returned.</p>
     * <pre>
     * NumberUtils.toShort(null) = 0
     * NumberUtils.toShort("")   = 0
     * NumberUtils.toShort("1")  = 1
     * </pre>
     * @param str  the string to convert, may be null
     * @return the short represented by the string, or <code>zero</code> if
     * conversion fails
     * @since 2.5
    public static short toShort(String str) { … }
     * <p>Convert a <code>String</code> to an <code>short</code>, returning a
     * default value if the conversion fails.</p>
     * <p>If the string is <code>null</code>, the default value is returned.</p>
     * <pre>
     * NumberUtils.toShort(null, 1) = 1
     * NumberUtils.toShort("", 1)   = 1
     * NumberUtils.toShort("1", 0)  = 1
     * </pre>
     * @param str  the string to convert, may be null
     * @param defaultValue  the default value
     * @return the short represented by the string, or the default if conversion fails
     * @since 2.5
    public static short toShort(String str, short defaultValue) { … }
}
</class>

<class name="IEEE754rUtils" role="collaborator">
     * <p>Provides IEEE-754r variants of NumberUtils methods. </p>
     * <p>See: <a href="http://en.wikipedia.org/wiki/IEEE_754r">http://en.wikipedia.org/wiki/IEEE_754r</a></p>
     * @since 2.4
     * @author Apache Software Foundation
     * @version $Id$
public class IEEE754rUtils {
     * <p>Returns the minimum value in an array.</p>
     * @param array  an array, must not be null or empty
     * @return the minimum value in the array
     * @throws IllegalArgumentException if <code>array</code> is <code>null</code>
     * @throws IllegalArgumentException if <code>array</code> is empty
    public static double min(double[] array) { … }
     * <p>Returns the minimum value in an array.</p>
     * @param array  an array, must not be null or empty
     * @return the minimum value in the array
     * @throws IllegalArgumentException if <code>array</code> is <code>null</code>
     * @throws IllegalArgumentException if <code>array</code> is empty
    public static float min(float[] array) { … }
     * <p>Gets the minimum of three <code>double</code> values.</p>
     * <p>NaN is only returned if all numbers are NaN as per IEEE-754r. </p>
     * @param a  value 1
     * @param b  value 2
     * @param c  value 3
     * @return  the smallest of the values
    public static double min(double a, double b, double c) { … }
     * <p>Gets the minimum of two <code>double</code> values.</p>
     * <p>NaN is only returned if all numbers are NaN as per IEEE-754r. </p>
     * @param a  value 1
     * @param b  value 2
     * @return  the smallest of the values
    public static double min(double a, double b) { … }
     * <p>Gets the minimum of three <code>float</code> values.</p>
     * <p>NaN is only returned if all numbers are NaN as per IEEE-754r. </p>
     * @param a  value 1
     * @param b  value 2
     * @param c  value 3
     * @return  the smallest of the values
    public static float min(float a, float b, float c) { … }
     * <p>Gets the minimum of two <code>float</code> values.</p>
     * <p>NaN is only returned if all numbers are NaN as per IEEE-754r. </p>
     * @param a  value 1
     * @param b  value 2
     * @return  the smallest of the values
    public static float min(float a, float b) { … }
     * <p>Returns the maximum value in an array.</p>
     * @param array  an array, must not be null or empty
     * @return the minimum value in the array
     * @throws IllegalArgumentException if <code>array</code> is <code>null</code>
     * @throws IllegalArgumentException if <code>array</code> is empty
    public static double max(double[] array) { … }
     * <p>Returns the maximum value in an array.</p>
     * @param array  an array, must not be null or empty
     * @return the minimum value in the array
     * @throws IllegalArgumentException if <code>array</code> is <code>null</code>
     * @throws IllegalArgumentException if <code>array</code> is empty
    public static float max(float[] array) { … }
     * <p>Gets the maximum of three <code>double</code> values.</p>
     * <p>NaN is only returned if all numbers are NaN as per IEEE-754r. </p>
     * @param a  value 1
     * @param b  value 2
     * @param c  value 3
     * @return  the largest of the values
    public static double max(double a, double b, double c) { … }
     * <p>Gets the maximum of two <code>double</code> values.</p>
     * <p>NaN is only returned if all numbers are NaN as per IEEE-754r. </p>
     * @param a  value 1
     * @param b  value 2
     * @return  the largest of the values
    public static double max(double a, double b) { … }
     * <p>Gets the maximum of three <code>float</code> values.</p>
     * <p>NaN is only returned if all numbers are NaN as per IEEE-754r. </p>
     * @param a  value 1
     * @param b  value 2
     * @param c  value 3
     * @return  the largest of the values
    public static float max(float a, float b, float c) { … }
     * <p>Gets the maximum of two <code>float</code> values.</p>
     * <p>NaN is only returned if all numbers are NaN as per IEEE-754r. </p>
     * @param a  value 1
     * @param b  value 2
     * @return  the largest of the values
    public static float max(float a, float b) { … }
}
</class>

<class name="StringUtils" role="collaborator">
public class StringUtils {
     * The empty String <code>""</code>.
     * @since 2.0
    public static final String EMPTY = "";
     * Represents a failed index search.
     * @since 2.1
    public static final int INDEX_NOT_FOUND = -1;
     * <p>The maximum size to which the padding constant(s) can expand.</p>
    private static final int PAD_LIMIT = 8192;
     * A regex pattern for recognizing blocks of whitespace characters.
    private static final Pattern WHITESPACE_BLOCK = Pattern.compile("\\s+");
     * <p><code>StringUtils</code> instances should NOT be constructed in
     * standard programming. Instead, the class should be used as
     * <code>StringUtils.trim(" foo ");</code>.</p>
     * <p>This constructor is public to permit tools that require a JavaBean
     * instance to operate.</p>
    public StringUtils() {
        super();
    }
     * <p>Checks if a CharSequence is empty ("") or null.</p>
     * <pre>
     * StringUtils.isEmpty(null)      = true
     * StringUtils.isEmpty("")        = true
     * StringUtils.isEmpty(" ")       = false
     * StringUtils.isEmpty("bob")     = false
     * StringUtils.isEmpty("  bob  ") = false
     * </pre>
     * <p>NOTE: This method changed in Lang version 2.0.
     * It no longer trims the CharSequence.
     * That functionality is available in isBlank().</p>
     * @param cs  the CharSequence to check, may be null
     * …
    public static boolean isEmpty(CharSequence cs) { … }
     * <p>Checks if a CharSequence is not empty ("") and not null.</p>
     * <pre>
     * StringUtils.isNotEmpty(null)      = false
     * StringUtils.isNotEmpty("")        = false
     * StringUtils.isNotEmpty(" ")       = true
     * StringUtils.isNotEmpty("bob")     = true
     * StringUtils.isNotEmpty("  bob  ") = true
     * </pre>
     * @param cs  the CharSequence to check, may be null
     * @return <code>true</code> if the CharSequence is not empty and not null
     * @since 3.0 Changed signature from isNotEmpty(String) to isNotEmpty(CharSequence)
    public static boolean isNotEmpty(CharSequence cs) { … }
     * <p>Checks if a CharSequence is whitespace, empty ("") or null.</p>
     * <pre>
     * StringUtils.isBlank(null)      = true
     * StringUtils.isBlank("")        = true
     * StringUtils.isBlank(" ")       = true
     * StringUtils.isBlank("bob")     = false
     * StringUtils.isBlank("  bob  ") = false
     * </pre>
     * @param cs  the CharSequence to check, may be null
     * @return <code>true</code> if the CharSequence is null, empty or whitespace
     * @since 2.0
     * @since 3.0 Changed signature from isBlank(String) to isBlank(CharSequence)
    public static boolean isBlank(CharSequence cs) { … }
     * <p>Checks if a CharSequence is not empty (""), not null and not whitespace only.</p>
     * <pre>
     * StringUtils.isNotBlank(null)      = false
     * StringUtils.isNotBlank("")        = false
     * StringUtils.isNotBlank(" ")       = false
     * StringUtils.isNotBlank("bob")     = true
     * StringUtils.isNotBlank("  bob  ") = true
     * </pre>
     * @param cs  the CharSequence to check, may be null
     * @return <code>true</code> if the CharSequence is
     * not empty and not null and not whitespace
     * @since 2.0
     * …
    public static boolean isNotBlank(CharSequence cs) { … }
     * <p>Removes control characters (char &lt;= 32) from both
     * ends of this String, handling <code>null</code> by returning
     * <code>null</code>.</p>
     * <p>The String is trimmed using {@link String#trim()}.
     * Trim removes start and end characters &lt;= 32.
     * To strip whitespace use {@link #strip(String)}.</p>
     * <p>To trim your choice of characters, use the
     * {@link #strip(String, String)} methods.</p>
     * <pre>
     * StringUtils.trim(null)          = null
     * StringUtils.trim("")            = ""
     * StringUtils.trim("     ")       = ""
     * …
    public static String trim(String str) { … }
     * <p>Removes control characters (char &lt;= 32) from both
     * ends of this String returning <code>null</code> if the String is
     * empty ("") after the trim or if it is <code>null</code>.
     * <p>The String is trimmed using {@link String#trim()}.
     * Trim removes start and end characters &lt;= 32.
     * To strip whitespace use {@link #stripToNull(String)}.</p>
     * <pre>
     * StringUtils.trimToNull(null)          = null
     * StringUtils.trimToNull("")            = null
     * StringUtils.trimToNull("     ")       = null
     * StringUtils.trimToNull("abc")         = "abc"
     * StringUtils.trimToNull("    abc    ") = "abc"
     * …
    public static String trimToNull(String str) { … }
     * <p>Removes control characters (char &lt;= 32) from both
     * ends of this String returning an empty String ("") if the String
     * is empty ("") after the trim or if it is <code>null</code>.
     * <p>The String is trimmed using {@link String#trim()}.
     * Trim removes start and end characters &lt;= 32.
     * To strip whitespace use {@link #stripToEmpty(String)}.</p>
     * <pre>
     * StringUtils.trimToEmpty(null)          = ""
     * StringUtils.trimToEmpty("")            = ""
     * StringUtils.trimToEmpty("     ")       = ""
     * StringUtils.trimToEmpty("abc")         = "abc"
     * StringUtils.trimToEmpty("    abc    ") = "abc"
     * …
    public static String trimToEmpty(String str) { … }
     * <p>Removes the accents from a string. </p>
     * <p>NOTE: This is a JDK 1.6 method, it will fail on JDK 1.5. </p>
     * <pre>
     * StringUtils.stripAccents(null)                = null
     * StringUtils.stripAccents("")                  = ""
     * StringUtils.stripAccents("control")           = "control"
     * StringUtils.stripAccents("&ecute;clair")      = "eclair"
     * </pre>
     * @param input String to be stripped
     * @return String without accents on the text
     * @since 3.0
    public static String stripAccents(String input) { … }
}
// … (non-patched members truncated; patched methods kept)
</class>
</class_skeletons>

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

THIS IS A NON-CRASHING (SEMANTIC) BUG. The trigger test does not throw — it fails a JUnit assertion because the code returns a WRONG value. There is therefore no exception to catch. Your harness must SUPPLY the oracle:

1. LIFT EVERY ASSERTION (not just one): the failing test almost always checks SEVERAL input/output pairs (e.g. many assertEquals lines). Each pair is a SEPARATE trusted oracle — the correct code is known to produce that exact expected value for that exact input. Find ALL of them (assertEquals / assertTrue / assertSame / assertNull ...), and for each identify (a) the call it makes on the real API and (b) the EXPECTED value it checks against. Copy both VERBATIM — do not infer or recompute; use the literals the test hard-codes.

2. RECONSTRUCT: in fuzzerTestOneInput, make EACH of those calls through the real public API, exactly as the test does, to get each ACTUAL value. EXACTLY-AS-THE-TEST-DOES includes the test's SETUP: if the expected value depends on environment the test arranges (a registered source file, a Locale or TimeZone, a formatting mode, a fixture object), replicate that setup faithfully — and if you cannot replicate it, DROP that pair rather than assert it, because a half-reconstructed scenario fires on correct code too (that exact mistake caused two false accusations of correct patches).

3. ASSERT ALL OF THEM: if ANY actual value does not equal its expected value, `throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:<short-id>] semantic mismatch: <what differed>")`. Every DISTINCT check gets its own [oracle:<short-id>] prefix (e.g. [oracle:lifted-pairs], [oracle:round-trip]) — alarms without an ID are rejected mechanically; the ID is how the pipeline tells which check fired. Check every lifted pair — an overfitting patch that fixed only the one reported input still fails the others, so the more pairs you assert the more likely it is caught. Jazzer reports the throw as a finding, exactly like a crash, so scoring is unchanged. If all match, continue. WHEN THE EXPECTED VALUE IS A STRING OF CODE OR FORMATTED TEXT (compiler/printer output, rendered source, messages with layout): never compare raw strings — normalize BOTH sides first (collapse all whitespace runs, e.g. s.replaceAll("\\s+", "")) so the check fires only on a CONTENT difference. A correct implementation may legitimately differ in spaces, newlines or statement-separator placement, and a raw-string firing over formatting will be dismissed in review — burying the real content difference the same comparison would have caught. COMPARE NORMALIZED, BUT RECORD RAW: whenever you normalize before comparing, the alarm message MUST also carry the PRE-normalization values, under these exact key names: `expectedNormalized=` `actualNormalized=` `expectedRaw=` `actualRaw=`. Emit the two Raw keys ONLY when you actually normalized, so their absence means no normalization happened. Reason: a downstream check asks whether your reported value equals a value the failing test pins, and the test pins the RAW form — a message carrying only the normalized form can never match it, so that check is dead for your alarm and a real dismissal is lost. Named keys, never positional: the reader must never have to guess which form it is holding. KEEP THE WHOLE MESSAGE ON ONE LINE: the raw form of formatted text usually contains newlines and tabs, and the alarm is read line by line — a literal newline inside a value silently cuts the message there, discarding every key after it. So ESCAPE them: emit `\n` and `\t` as the two-character sequences (backslash then n / t), never as real line breaks. In Java: `s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t")`. This is also the form the failing test's own source literal is written in, so the comparison lines up exactly.

4. THEN GENERALISE — but keep every assertion TRUSTED. The lifted pair tells you the answer for ONE input only. Do not invent an expected value for an arbitrary fuzzed input: an assertion you cannot justify will fire on CORRECT patches too (a false positive, the worst outcome). Extend coverage only in ways where the correct answer is independently known:

  (a) CONSTRUCT THE INPUT FROM A KNOWN ANSWER. Pick a value with FuzzedDataProvider, build the canonical input that must map to it, and assert you recover it — the answer is trusted because you chose it first. E.g. fuzz an int n, format it, parse it back, assert == n.

  (b) EQUIVALENCE TO THE SEED. The seed input has properties that fix its answer. Generate other inputs that SHARE those properties and must therefore share the seed's expected value, and assert they equal it. E.g. if the contract is case-insensitive and foo("abc")==X, then foo("ABC") and foo("Abc") must also ==X. This turns the one lifted value into an oracle for a whole family of inputs.

  (c) METAMORPHIC RELATION between two real calls (see below) — trusted because it holds for ANY correct implementation.

  GUARDRAIL: for any fuzzed input where you cannot justify the expected value by (a), (b), or (c), still CALL the API on it (this exercises the patched path), but do NOT assert on its result — just return. Assert only trusted answers; explore everything else without asserting.
  CALIBRATION: your assertions will be screened by an automated soundness reviewer before any firing counts, so do NOT hedge to trivially-weak checks to be safe. Write the strongest check you can JUSTIFY from the documented contract, and state that justification in a comment next to the check (the reviewer reads it). The reviewer is a safety net, not a licence to guess — it is imperfect, so an assertion still needs a real justification — but a vacuous assertion (isFinite / not-null / no-throw) can never catch a wrong value and guarantees a miss, which nothing downstream can recover.
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `NumberUtils`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
Lift the call and the expected value from THIS test (this attempt's target):
<failing_test class="org.apache.commons.lang3.math.NumberUtilsTest" method="testIsNumber">
    public void testIsNumber() {
        String val = "12345";
        assertTrue("isNumber(String) 1 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 1 failed", checkCreateNumber(val));
        val = "1234.5";
        assertTrue("isNumber(String) 2 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 2 failed", checkCreateNumber(val));
        val = ".12345";
        assertTrue("isNumber(String) 3 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 3 failed", checkCreateNumber(val));
        val = "1234E5";
        assertTrue("isNumber(String) 4 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 4 failed", checkCreateNumber(val));
        val = "1234E+5";
        assertTrue("isNumber(String) 5 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 5 failed", checkCreateNumber(val));
        val = "1234E-5";
        assertTrue("isNumber(String) 6 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 6 failed", checkCreateNumber(val));
        val = "123.4E5";
        assertTrue("isNumber(String) 7 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 7 failed", checkCreateNumber(val));
        val = "-1234";
        assertTrue("isNumber(String) 8 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 8 failed", checkCreateNumber(val));
        val = "-1234.5";
        assertTrue("isNumber(String) 9 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 9 failed", checkCreateNumber(val));
        val = "-.12345";
        assertTrue("isNumber(String) 10 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 10 failed", checkCreateNumber(val));
        val = "-1234E5";
        assertTrue("isNumber(String) 11 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 11 failed", checkCreateNumber(val));
        val = "0";
        assertTrue("isNumber(String) 12 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 12 failed", checkCreateNumber(val));
        val = "-0";
        assertTrue("isNumber(String) 13 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 13 failed", checkCreateNumber(val));
        val = "01234";
        assertTrue("isNumber(String) 14 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 14 failed", checkCreateNumber(val));
        val = "-01234";
        assertTrue("isNumber(String) 15 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 15 failed", checkCreateNumber(val));
        val = "0xABC123";
        assertTrue("isNumber(String) 16 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 16 failed", checkCreateNumber(val));
        val = "0x0";
        assertTrue("isNumber(String) 17 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 17 failed", checkCreateNumber(val));
        val = "123.4E21D";
        assertTrue("isNumber(String) 19 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 19 failed", checkCreateNumber(val));
        val = "-221.23F";
        assertTrue("isNumber(String) 20 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 20 failed", checkCreateNumber(val));
        val = "22338L";
        assertTrue("isNumber(String) 21 failed", NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 21 failed", checkCreateNumber(val));
        val = null;
        assertTrue("isNumber(String) 1 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 1 Neg failed", !checkCreateNumber(val));
        val = "";
        assertTrue("isNumber(String) 2 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 2 Neg failed", !checkCreateNumber(val));
        val = "--2.3";
        assertTrue("isNumber(String) 3 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 3 Neg failed", !checkCreateNumber(val));
        val = ".12.3";
        assertTrue("isNumber(String) 4 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 4 Neg failed", !checkCreateNumber(val));
        val = "-123E";
        assertTrue("isNumber(String) 5 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 5 Neg failed", !checkCreateNumber(val));
        val = "-123E+-212";
        assertTrue("isNumber(String) 6 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 6 Neg failed", !checkCreateNumber(val));
        val = "-123E2.12";
        assertTrue("isNumber(String) 7 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 7 Neg failed", !checkCreateNumber(val));
        val = "0xGF";
        assertTrue("isNumber(String) 8 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 8 Neg failed", !checkCreateNumber(val));
        val = "0xFAE-1";
        assertTrue("isNumber(String) 9 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 9 Neg failed", !checkCreateNumber(val));
        val = ".";
        assertTrue("isNumber(String) 10 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 10 Neg failed", !checkCreateNumber(val));
        val = "-0ABC123";
        assertTrue("isNumber(String) 11 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 11 Neg failed", !checkCreateNumber(val));
        val = "123.4E-D";
        assertTrue("isNumber(String) 12 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 12 Neg failed", !checkCreateNumber(val));
        val = "123.4ED";
        assertTrue("isNumber(String) 13 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 13 Neg failed", !checkCreateNumber(val));
        val = "1234E5l";
        assertTrue("isNumber(String) 14 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 14 Neg failed", !checkCreateNumber(val));
        val = "11a";
        assertTrue("isNumber(String) 15 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 15 Neg failed", !checkCreateNumber(val)); 
        val = "1a";
        assertTrue("isNumber(String) 16 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 16 Neg failed", !checkCreateNumber(val)); 
        val = "a";
        assertTrue("isNumber(String) 17 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 17 Neg failed", !checkCreateNumber(val)); 
        val = "11g";
        assertTrue("isNumber(String) 18 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 18 Neg failed", !checkCreateNumber(val)); 
        val = "11z";
        assertTrue("isNumber(String) 19 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 19 Neg failed", !checkCreateNumber(val)); 
        val = "11def";
        assertTrue("isNumber(String) 20 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 20 Neg failed", !checkCreateNumber(val)); 
        val = "11d11";
        assertTrue("isNumber(String) 21 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 21 Neg failed", !checkCreateNumber(val)); 
        val = "11 11";
        assertTrue("isNumber(String) 22 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 22 Neg failed", !checkCreateNumber(val));
        val = " 1111";
        assertTrue("isNumber(String) 23 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 23 Neg failed", !checkCreateNumber(val));
        val = "1111 ";
        assertTrue("isNumber(String) 24 Neg failed", !NumberUtils.isNumber(val));
        assertTrue("isNumber(String)/createNumber(String) 24 Neg failed", !checkCreateNumber(val));

        // LANG-521
        val = "2.";
        assertTrue("isNumber(String) LANG-521 failed", NumberUtils.isNumber(val));

        // LANG-664
        val = "1.1L";
        assertFalse("isNumber(String) LANG-664 failed", NumberUtils.isNumber(val));
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.apache.commons.lang3.math.NumberUtilsTest::testIsNumber
junit.framework.AssertionFailedError: isNumber(String) LANG-664 failed
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
<details open><summary>▸ Output (~5,786 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final String[] POSITIVE_VALS = new String[] {
        "12345",
        "1234.5",
        ".12345",
        "1234E5",
        "1234E+5",
        "1234E-5",
        "123.4E5",
        "-1234",
        "-1234.5",
        "-.12345",
        "-1234E5",
        "0",
        "-0",
        "01234",
        "-01234",
        "0xABC123",
        "0x0",
        "123.4E21D",
        "-221.23F",
        "22338L"
    };

    private static final String[] NEGATIVE_VALS = new String[] {
        null,
        "",
        "--2.3",
        ".12.3",
        "-123E",
        "-123E+-212",
        "-123E2.12",
        "0xGF",
        "0xFAE-1",
        ".",
        "-0ABC123",
        "123.4E-D",
        "123.4ED",
        "1234E5l",
        "11a",
        "1a",
        "a",
        "11g",
        "11z",
        "11def",
        "11d11",
        "11 11",
        " 1111",
        "1111 "
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        for (int i = 0; i < POSITIVE_VALS.length; i++) {
            String val = POSITIVE_VALS[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (!actualIsNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-isNumber-positive] semantic mismatch: NumberUtils.isNumber(\"" + escape(val) + "\") expected=true actual=false index=" + i);
            }

            boolean actualCheckCreateNumber = checkCreateNumber(val);
            if (!actualCheckCreateNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-createNumber-positive] semantic mismatch: checkCreateNumber(\"" + escape(val) + "\") expected=true actual=false index=" + i);
            }
        }

        for (int i = 0; i < NEGATIVE_VALS.length; i++) {
            String val = NEGATIVE_VALS[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (actualIsNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-isNumber-negative] semantic mismatch: NumberUtils.isNumber(" + printable(val) + ") expected=false actual=true index=" + i);
            }

            boolean actualCheckCreateNumber = checkCreateNumber(val);
            if (actualCheckCreateNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-createNumber-negative] semantic mismatch: checkCreateNumber(" + printable(val) + ") expected=false actual=true index=" + i);
            }
        }

        if (!NumberUtils.isNumber("2.")) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-lang521] semantic mismatch: NumberUtils.isNumber(\"2.\") expected=true actual=false");
        }

        if (NumberUtils.isNumber("1.1L")) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-lang664] semantic mismatch: NumberUtils.isNumber(\"1.1L\") expected=false actual=true");
        }

        int n = data.consumeInt(-1_000_000, 1_000_000);
        String canonicalInt = Integer.toString(n);
        if (!NumberUtils.isNumber(canonicalInt)) {
            throw new RuntimeException(
                "[oracle:meta-canonical-int] metamorphic violation: canonical decimal integer strings must be accepted input=" + canonicalInt + " lhs=" + NumberUtils.isNumber(canonicalInt) + " rhs=true");
        }
        if (!checkCreateNumber(canonicalInt)) {
            throw new RuntimeException(
                "[oracle:meta-canonical-int-create] metamorphic violation: createNumber must parse canonical decimal integer strings input=" + canonicalInt + " lhs=" + checkCreateNumber(canonicalInt) + " rhs=true");
        }

        int whole = data.consumeInt(0, 1_000_000);
        int frac = data.consumeInt(0, 1_000_000);
        String suffix = data.consumeBoolean() ? "L" : "l";
        String decimalLongLike = whole + "." + frac + suffix;

        boolean observed = NumberUtils.isNumber(decimalLongLike);
        // Contract cited from the patched method's own comment: "not allowing L with an exponent or decimal point".
        // A throw-deleting or guard-only patch that leaves decimal-point+L inputs accepted would violate this observable result.
        if (observed) {
            throw new RuntimeException(
                "[oracle:meta-decimal-long] metamorphic violation: decimal-point numbers with L/l suffix must be rejected input=" + decimalLongLike + " lhs=true rhs=false");
        }

        try {
            NumberUtils.createNumber(decimalLongLike);
        } catch (RuntimeException ignored) {
        }

        String arbitrary = data.consumeString(64);
        NumberUtils.isNumber(arbitrary);
        try {
            NumberUtils.createNumber(arbitrary);
        } catch (RuntimeException ignored) {
        }
    }

    private static boolean checkCreateNumber(String val) {
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

    private static String printable(String s) {
        return s == null ? "null" : "\"" + escape(s) + "\"";
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r").replace("\"", "\\\"");
    }
}
```

</details>

---
## [41] ⚙️ harness-attempt · `attempt_003`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-lang664] semantic mismatch: NumberUtils.isNumber("1.1L") expected=false actual=true

---
## [42] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [36] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~11,481 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedTestOracles();
        checkSeedDerivedDecimalWithLongSuffix(data);
        checkAcceptedPlainDecimalHasCreatableNumber(data);
        drivePatchedPathOnFuzzedInput(data);
    }

    private static void checkLiftedTestOracles() {
        assertIsNumberTrue("lifted-pos-1", "12345");
        assertCheckCreateNumberTrue("lifted-pos-cn-1", "12345");
        assertIsNumberTrue("lifted-pos-2", "1234.5");
        assertCheckCreateNumberTrue("lifted-pos-cn-2", "1234.5");
        assertIsNumberTrue("lifted-pos-3", ".12345");
        assertCheckCreateNumberTrue("lifted-pos-cn-3", ".12345");
        assertIsNumberTrue("lifted-pos-4", "1234E5");
        assertCheckCreateNumberTrue("lifted-pos-cn-4", "1234E5");
        assertIsNumberTrue("lifted-pos-5", "1234E+5");
        assertCheckCreateNumberTrue("lifted-pos-cn-5", "1234E+5");
        assertIsNumberTrue("lifted-pos-6", "1234E-5");
        assertCheckCreateNumberTrue("lifted-pos-cn-6", "1234E-5");
        assertIsNumberTrue("lifted-pos-7", "123.4E5");
        assertCheckCreateNumberTrue("lifted-pos-cn-7", "123.4E5");
        assertIsNumberTrue("lifted-pos-8", "-1234");
        assertCheckCreateNumberTrue("lifted-pos-cn-8", "-1234");
        assertIsNumberTrue("lifted-pos-9", "-1234.5");
        assertCheckCreateNumberTrue("lifted-pos-cn-9", "-1234.5");
        assertIsNumberTrue("lifted-pos-10", "-.12345");
        assertCheckCreateNumberTrue("lifted-pos-cn-10", "-.12345");
        assertIsNumberTrue("lifted-pos-11", "-1234E5");
        assertCheckCreateNumberTrue("lifted-pos-cn-11", "-1234E5");
        assertIsNumberTrue("lifted-pos-12", "0");
        assertCheckCreateNumberTrue("lifted-pos-cn-12", "0");
        assertIsNumberTrue("lifted-pos-13", "-0");
        assertCheckCreateNumberTrue("lifted-pos-cn-13", "-0");
        assertIsNumberTrue("lifted-pos-14", "01234");
        assertCheckCreateNumberTrue("lifted-pos-cn-14", "01234");
        assertIsNumberTrue("lifted-pos-15", "-01234");
        assertCheckCreateNumberTrue("lifted-pos-cn-15", "-01234");
        assertIsNumberTrue("lifted-pos-16", "0xABC123");
        assertCheckCreateNumberTrue("lifted-pos-cn-16", "0xABC123");
        assertIsNumberTrue("lifted-pos-17", "0x0");
        assertCheckCreateNumberTrue("lifted-pos-cn-17", "0x0");
        assertIsNumberTrue("lifted-pos-19", "123.4E21D");
        assertCheckCreateNumberTrue("lifted-pos-cn-19", "123.4E21D");
        assertIsNumberTrue("lifted-pos-20", "-221.23F");
        assertCheckCreateNumberTrue("lifted-pos-cn-20", "-221.23F");
        assertIsNumberTrue("lifted-pos-21", "22338L");
        assertCheckCreateNumberTrue("lifted-pos-cn-21", "22338L");

        assertIsNumberFalse("lifted-neg-1", null);
        assertCheckCreateNumberFalse("lifted-neg-cn-1", null);
        assertIsNumberFalse("lifted-neg-2", "");
        assertCheckCreateNumberFalse("lifted-neg-cn-2", "");
        assertIsNumberFalse("lifted-neg-3", "--2.3");
        assertCheckCreateNumberFalse("lifted-neg-cn-3", "--2.3");
        assertIsNumberFalse("lifted-neg-4", ".12.3");
        assertCheckCreateNumberFalse("lifted-neg-cn-4", ".12.3");
        assertIsNumberFalse("lifted-neg-5", "-123E");
        assertCheckCreateNumberFalse("lifted-neg-cn-5", "-123E");
        assertIsNumberFalse("lifted-neg-6", "-123E+-212");
        assertCheckCreateNumberFalse("lifted-neg-cn-6", "-123E+-212");
        assertIsNumberFalse("lifted-neg-7", "-123E2.12");
        assertCheckCreateNumberFalse("lifted-neg-cn-7", "-123E2.12");
        assertIsNumberFalse("lifted-neg-8", "0xGF");
        assertCheckCreateNumberFalse("lifted-neg-cn-8", "0xGF");
        assertIsNumberFalse("lifted-neg-9", "0xFAE-1");
        assertCheckCreateNumberFalse("lifted-neg-cn-9", "0xFAE-1");
        assertIsNumberFalse("lifted-neg-10", ".");
        assertCheckCreateNumberFalse("lifted-neg-cn-10", ".");
        assertIsNumberFalse("lifted-neg-11", "-0ABC123");
        assertCheckCreateNumberFalse("lifted-neg-cn-11", "-0ABC123");
        assertIsNumberFalse("lifted-neg-12", "123.4E-D");
        assertCheckCreateNumberFalse("lifted-neg-cn-12", "123.4E-D");
        assertIsNumberFalse("lifted-neg-13", "123.4ED");
        assertCheckCreateNumberFalse("lifted-neg-cn-13", "123.4ED");
        assertIsNumberFalse("lifted-neg-14", "1234E5l");
        assertCheckCreateNumberFalse("lifted-neg-cn-14", "1234E5l");
        assertIsNumberFalse("lifted-neg-15", "11a");
        assertCheckCreateNumberFalse("lifted-neg-cn-15", "11a");
        assertIsNumberFalse("lifted-neg-16", "1a");
        assertCheckCreateNumberFalse("lifted-neg-cn-16", "1a");
        assertIsNumberFalse("lifted-neg-17", "a");
        assertCheckCreateNumberFalse("lifted-neg-cn-17", "a");
        assertIsNumberFalse("lifted-neg-18", "11g");
        assertCheckCreateNumberFalse("lifted-neg-cn-18", "11g");
        assertIsNumberFalse("lifted-neg-19", "11z");
        assertCheckCreateNumberFalse("lifted-neg-cn-19", "11z");
        assertIsNumberFalse("lifted-neg-20", "11def");
        assertCheckCreateNumberFalse("lifted-neg-cn-20", "11def");
        assertIsNumberFalse("lifted-neg-21", "11d11");
        assertCheckCreateNumberFalse("lifted-neg-cn-21", "11d11");
        assertIsNumberFalse("lifted-neg-22", "11 11");
        assertCheckCreateNumberFalse("lifted-neg-cn-22", "11 11");
        assertIsNumberFalse("lifted-neg-23", " 1111");
        assertCheckCreateNumberFalse("lifted-neg-cn-23", " 1111");
        assertIsNumberFalse("lifted-neg-24", "1111 ");
        assertCheckCreateNumberFalse("lifted-neg-cn-24", "1111 ");

        assertIsNumberTrue("lifted-lang-521", "2.");
        assertIsNumberFalse("lifted-lang-664", "1.1L");
    }

    private static void checkSeedDerivedDecimalWithLongSuffix(FuzzedDataProvider data) {
        String base;
        try {
            int whole = data.consumeInt(0, 1000000);
            int frac = data.consumeInt(0, 1000000);
            boolean neg = data.consumeBoolean();
            base = (neg ? "-" : "") + whole + "." + frac;
        } catch (Throwable t) {
            return;
        }

        boolean plain;
        try {
            plain = NumberUtils.isNumber(base);
        } catch (Throwable t) {
            return;
        }

        boolean withLong;
        try {
            withLong = NumberUtils.isNumber(base + "L");
        } catch (Throwable t) {
            return;
        }

        // Contract/oracle justification: the lifted failing test pins that a decimal form like "1.1"
        // is accepted, while the same decimal with an 'L' suffix "1.1L" is rejected. This directly
        // exercises the patched branch; a throw-deleting or overfitting patch that leaves the parser
        // accepting decimal+'L' would violate this observable post-condition.
        if (!plain) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:decimal-with-long-suffix] semantic mismatch: plain decimal should be accepted but isNumber(\""
                    + base + "\") was false");
        }
        if (withLong) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:decimal-with-long-suffix] semantic mismatch: decimal with long suffix should be rejected but isNumber(\""
                    + base + "L\") was true");
        }
    }

    private static void checkAcceptedPlainDecimalHasCreatableNumber(FuzzedDataProvider data) {
        String s;
        try {
            int whole = data.consumeInt(0, 1000000);
            int frac = data.consumeInt(0, 1000000);
            s = (data.consumeBoolean() ? "-" : "") + whole + "." + frac;
        } catch (Throwable t) {
            return;
        }

        boolean ok;
        try {
            ok = NumberUtils.isNumber(s);
        } catch (Throwable t) {
            return;
        }
        if (!ok) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:accepted-plain-decimal-creatable] semantic mismatch: valid plain decimal was rejected by isNumber(\""
                    + s + "\")");
        }

        Number n;
        try {
            n = NumberUtils.createNumber(s);
        } catch (Throwable t) {
            return;
        }

        // Contract/oracle justification: the trusted test couples isNumber(val) with checkCreateNumber(val)
        // for accepted numeric strings. For valid-by-construction plain decimals, acceptance by isNumber
        // must correspond to createNumber succeeding and returning non-null through the real API.
        if (n == null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:accepted-plain-decimal-creatable] semantic mismatch: createNumber(\""
                    + s + "\") returned null for an accepted number string");
        }
    }

    private static void drivePatchedPathOnFuzzedInput(FuzzedDataProvider data) {
        String s = data.consumeRemainingAsString();
        try {
            NumberUtils.isNumber(s);
        } catch (Throwable t) {
        }
    }

    private static boolean checkCreateNumber(String val) {
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

    private static void assertIsNumberTrue(String oracleId, String val) {
        boolean actual = NumberUtils.isNumber(val);
        if (!actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: NumberUtils.isNumber("
                    + printable(val) + ") expected=true actual=false");
        }
    }

    private static void assertIsNumberFalse(String oracleId, String val) {
        boolean actual = NumberUtils.isNumber(val);
        if (actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: NumberUtils.isNumber("
                    + printable(val) + ") expected=false actual=true");
        }
    }

    private static void assertCheckCreateNumberTrue(String oracleId, String val) {
        boolean actual = checkCreateNumber(val);
        if (!actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: checkCreateNumber("
                    + printable(val) + ") expected=true actual=false");
        }
    }

    private static void assertCheckCreateNumberFalse(String oracleId, String val) {
        boolean actual = checkCreateNumber(val);
        if (actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: checkCreateNumber("
                    + printable(val) + ") expected=false actual=true");
        }
    }

    private static String printable(String s) {
        if (s == null) {
            return "null";
        }
        return "\"" + s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t") + "\"";
    }
}
```

</details>

---
## [43] ⚙️ harness-attempt · `attempt_004`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-lang-664] semantic mismatch: NumberUtils.isNumber("1.1L") expected=false actual=true

---
## [44] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [40] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,861 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static boolean checkCreateNumber(String val) {
        try {
            Object obj = NumberUtils.createNumber(val);
            return obj != null;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static void requireTrue(String oracleId, String description, boolean actual) {
        if (!actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + description + " expected=true actual=false");
        }
    }

    private static void requireFalse(String oracleId, String description, boolean actual) {
        if (actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + description + " expected=false actual=true");
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String[] positiveVals = new String[] {
            "12345",
            "1234.5",
            ".12345",
            "1234E5",
            "1234E+5",
            "1234E-5",
            "123.4E5",
            "-1234",
            "-1234.5",
            "-.12345",
            "-1234E5",
            "0",
            "-0",
            "01234",
            "-01234",
            "0xABC123",
            "0x0",
            "123.4E21D",
            "-221.23F",
            "22338L",
            "2."
        };

        String[] negativeVals = new String[] {
            null,
            "",
            "--2.3",
            ".12.3",
            "-123E",
            "-123E+-212",
            "-123E2.12",
            "0xGF",
            "0xFAE-1",
            ".",
            "-0ABC123",
            "123.4E-D",
            "123.4ED",
            "1234E5l",
            "11a",
            "1a",
            "a",
            "11g",
            "11z",
            "11def",
            "11d11",
            "11 11",
            " 1111",
            "1111 ",
            "1.1L"
        };

        for (int i = 0; i < positiveVals.length; i++) {
            String val = positiveVals[i];
            requireTrue("lifted-isNumber-pos-" + i, "NumberUtils.isNumber(\"" + val + "\")", NumberUtils.isNumber(val));
            requireTrue("lifted-createNumber-pos-" + i, "checkCreateNumber(\"" + val + "\")", checkCreateNumber(val));
        }

        for (int i = 0; i < negativeVals.length; i++) {
            String val = negativeVals[i];
            String shown = String.valueOf(val);
            requireFalse("lifted-isNumber-neg-" + i, "NumberUtils.isNumber(" + shown + ")", NumberUtils.isNumber(val));
            requireFalse("lifted-createNumber-neg-" + i, "checkCreateNumber(" + shown + ")", checkCreateNumber(val));
        }

        String fuzz = data.consumeString(32);
        NumberUtils.isNumber(fuzz);

        String digits1 = data.consumeAsciiString(12);
        String digits2 = data.consumeAsciiString(12);
        if (digits1.length() == 0) {
            digits1 = "1";
        }
        if (digits2.length() == 0) {
            digits2 = "0";
        }
        digits1 = digitsOnly(digits1);
        digits2 = digitsOnly(digits2);
        if (digits1.length() == 0) {
            digits1 = "1";
        }
        if (digits2.length() == 0) {
            digits2 = "0";
        }

        String longLiteral = digits1 + "L";
        try {
            boolean actualLongLiteral = NumberUtils.isNumber(longLiteral);
            boolean actualCreateLongLiteral = checkCreateNumber(longLiteral);
            if (!actualLongLiteral) {
                throw new RuntimeException(
                    "[oracle:constructed-long-literal] metamorphic violation: constructed canonical long literal must be accepted input="
                        + longLiteral + " lhs=" + actualLongLiteral + " rhs=true");
            }
            if (!actualCreateLongLiteral) {
                throw new RuntimeException(
                    "[oracle:constructed-long-create] metamorphic violation: createNumber must accept the same constructed canonical long literal input="
                        + longLiteral + " lhs=" + actualCreateLongLiteral + " rhs=true");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        String decimalLongLiteral = digits1 + "." + digits2 + "L";
        try {
            /*
             * Contract justification: the shown NumberUtils.isNumber implementation documents
             * at the patched branch "not allowing L with an exponent or decimal point".
             * Therefore any constructed non-degenerate decimal literal ending in L must be rejected.
             * A patch that merely deletes or bypasses the patched check would incorrectly return true.
             */
            boolean actualDecimalLong = NumberUtils.isNumber(decimalLongLiteral);
            if (actualDecimalLong) {
                throw new RuntimeException(
                    "[oracle:decimal-long-forbidden] metamorphic violation: decimal literal with trailing L must be rejected input="
                        + decimalLongLiteral + " lhs=" + actualDecimalLong + " rhs=false");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }

    private static String digitsOnly(String s) {
        StringBuffer sb = new StringBuffer();
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
## [45] ⚙️ harness-attempt · `attempt_005`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-isNumber-neg-24] semantic mismatch: NumberUtils.isNumber(1.1L) expected=false actual=true

---
## [46] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [36] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,421 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        assertLifted("pos-1", "12345", true, true);
        assertLifted("pos-2", "1234.5", true, true);
        assertLifted("pos-3", ".12345", true, true);
        assertLifted("pos-4", "1234E5", true, true);
        assertLifted("pos-5", "1234E+5", true, true);
        assertLifted("pos-6", "1234E-5", true, true);
        assertLifted("pos-7", "123.4E5", true, true);
        assertLifted("pos-8", "-1234", true, true);
        assertLifted("pos-9", "-1234.5", true, true);
        assertLifted("pos-10", "-.12345", true, true);
        assertLifted("pos-11", "-1234E5", true, true);
        assertLifted("pos-12", "0", true, true);
        assertLifted("pos-13", "-0", true, true);
        assertLifted("pos-14", "01234", true, true);
        assertLifted("pos-15", "-01234", true, true);
        assertLifted("pos-16", "0xABC123", true, true);
        assertLifted("pos-17", "0x0", true, true);
        assertLifted("pos-19", "123.4E21D", true, true);
        assertLifted("pos-20", "-221.23F", true, true);
        assertLifted("pos-21", "22338L", true, true);

        assertLifted("neg-1", null, false, false);
        assertLifted("neg-2", "", false, false);
        assertLifted("neg-3", "--2.3", false, false);
        assertLifted("neg-4", ".12.3", false, false);
        assertLifted("neg-5", "-123E", false, false);
        assertLifted("neg-6", "-123E+-212", false, false);
        assertLifted("neg-7", "-123E2.12", false, false);
        assertLifted("neg-8", "0xGF", false, false);
        assertLifted("neg-9", "0xFAE-1", false, false);
        assertLifted("neg-10", ".", false, false);
        assertLifted("neg-11", "-0ABC123", false, false);
        assertLifted("neg-12", "123.4E-D", false, false);
        assertLifted("neg-13", "123.4ED", false, false);
        assertLifted("neg-14", "1234E5l", false, false);
        assertLifted("neg-15", "11a", false, false);
        assertLifted("neg-16", "1a", false, false);
        assertLifted("neg-17", "a", false, false);
        assertLifted("neg-18", "11g", false, false);
        assertLifted("neg-19", "11z", false, false);
        assertLifted("neg-20", "11def", false, false);
        assertLifted("neg-21", "11d11", false, false);
        assertLifted("neg-22", "11 11", false, false);
        assertLifted("neg-23", " 1111", false, false);
        assertLifted("neg-24", "1111 ", false, false);

        assertIsNumberOnly("lang-521", "2.", true);
        assertIsNumberOnly("lang-664", "1.1L", false);

        int whole;
        int frac;
        boolean neg;
        try {
            whole = data.consumeInt(0, 1000000);
            frac = data.consumeInt(0, 1000000);
            neg = data.consumeBoolean();
        } catch (Throwable t) {
            return;
        }
        String base = (neg ? "-" : "") + whole + "." + frac;

        boolean plain;
        try {
            plain = NumberUtils.isNumber(base);
        } catch (Throwable t) {
            return;
        }
        boolean withLongUpper;
        try {
            withLongUpper = NumberUtils.isNumber(base + "L");
        } catch (Throwable t) {
            return;
        }
        if (!plain) {
            throw new FuzzerSecurityIssueLow("[oracle:decimal-with-long-suffix] semantic mismatch: isNumber(" + esc(base) + ") expected=true actual=false");
        }
        if (withLongUpper) {
            throw new FuzzerSecurityIssueLow("[oracle:decimal-with-long-suffix] semantic mismatch: isNumber(" + esc(base + "L") + ") expected=false actual=true");
        }

        Number created;
        try {
            created = NumberUtils.createNumber(base);
        } catch (Throwable t) {
            return;
        }
        if (created == null) {
            throw new FuzzerSecurityIssueLow("[oracle:accepted-decimal-creatable] semantic mismatch: createNumber(" + esc(base) + ") returned null");
        }

        boolean withLongLower;
        try {
            withLongLower = NumberUtils.isNumber(base + "l");
        } catch (Throwable t) {
            return;
        }
        /* Contract justification: the implementation explicitly treats 'l' and 'L' in the same branch,
           so a correct implementation must give the same answer for base+'L' and base+'l'. A patch that
           merely skips or misroutes the changed branch can break this observable sibling agreement. */
        if (withLongUpper != withLongLower) {
            throw new FuzzerSecurityIssueLow("[oracle:l-vs-L-agreement] metamorphic violation: isNumber must treat 'l' and 'L' equivalently input=" + esc(base) + " upper=" + withLongUpper + " lower=" + withLongLower);
        }

        String canonical = Integer.toString(data.consumeInt(-1000000, 1000000));
        boolean canonicalAccepted;
        try {
            canonicalAccepted = NumberUtils.isNumber(canonical);
        } catch (Throwable t) {
            return;
        }
        if (!canonicalAccepted) {
            throw new FuzzerSecurityIssueLow("[oracle:canonical-integer-roundtrip] metamorphic violation: canonical decimal integer string must be accepted input=" + esc(canonical) + " actual=false");
        }
        Number canonicalCreated;
        try {
            canonicalCreated = NumberUtils.createNumber(canonical);
        } catch (Throwable t) {
            return;
        }
        if (canonicalCreated == null) {
            throw new FuzzerSecurityIssueLow("[oracle:canonical-integer-roundtrip] metamorphic violation: createNumber(" + esc(canonical) + ") returned null");
        }
    }

    private static void assertLifted(String id, String val, boolean expectedIsNumber, boolean expectedCreateNumber) {
        boolean actualIsNumber = NumberUtils.isNumber(val);
        if (actualIsNumber != expectedIsNumber) {
            throw new FuzzerSecurityIssueLow("[oracle:" + id + "-isNumber] semantic mismatch: input=" + esc(val) + " expected=" + expectedIsNumber + " actual=" + actualIsNumber);
        }
        boolean actualCreateNumber = checkCreateNumber(val);
        if (actualCreateNumber != expectedCreateNumber) {
            throw new FuzzerSecurityIssueLow("[oracle:" + id + "-checkCreateNumber] semantic mismatch: input=" + esc(val) + " expected=" + expectedCreateNumber + " actual=" + actualCreateNumber);
        }
    }

    private static void assertIsNumberOnly(String id, String val, boolean expected) {
        boolean actual = NumberUtils.isNumber(val);
        if (actual != expected) {
            throw new FuzzerSecurityIssueLow("[oracle:" + id + "] semantic mismatch: input=" + esc(val) + " expected=" + expected + " actual=" + actual);
        }
    }

    private static boolean checkCreateNumber(String val) {
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

    private static String esc(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}
```

</details>

---
## [47] ⚙️ harness-attempt · `attempt_006`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lang-664] semantic mismatch: input=1.1L expected=false actual=true

---
## [48] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [40] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,202 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    private static boolean checkCreateNumber(String val) {
        try {
            Object obj = NumberUtils.createNumber(val);
            return obj != null;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static void requireTrue(String oracleId, String label, boolean actual) {
        if (!actual) {
            throw new FuzzerSecurityIssueLow("[oracle:" + oracleId + "] semantic mismatch: expected true for " + label + " actual=false");
        }
    }

    private static void requireFalse(String oracleId, String label, boolean actual) {
        if (actual) {
            throw new FuzzerSecurityIssueLow("[oracle:" + oracleId + "] semantic mismatch: expected false for " + label + " actual=true");
        }
    }

    private static void assertSeedPositive(String oracleId, String val) {
        requireTrue(oracleId, "NumberUtils.isNumber(\"" + val + "\")", NumberUtils.isNumber(val));
        requireTrue(oracleId, "checkCreateNumber(\"" + val + "\")", checkCreateNumber(val));
    }

    private static void assertSeedNegative(String oracleId, String val) {
        requireFalse(oracleId, "NumberUtils.isNumber(" + String.valueOf(val) + ")", NumberUtils.isNumber(val));
        requireFalse(oracleId, "checkCreateNumber(" + String.valueOf(val) + ")", checkCreateNumber(val));
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String[] positives = new String[] {
            "12345",
            "1234.5",
            ".12345",
            "1234E5",
            "1234E+5",
            "1234E-5",
            "123.4E5",
            "-1234",
            "-1234.5",
            "-.12345",
            "-1234E5",
            "0",
            "-0",
            "01234",
            "-01234",
            "0xABC123",
            "0x0",
            "123.4E21D",
            "-221.23F",
            "22338L",
            "2."
        };

        String[] negatives = new String[] {
            null,
            "",
            "--2.3",
            ".12.3",
            "-123E",
            "-123E+-212",
            "-123E2.12",
            "0xGF",
            "0xFAE-1",
            ".",
            "-0ABC123",
            "123.4E-D",
            "123.4ED",
            "1234E5l",
            "11a",
            "1a",
            "a",
            "11g",
            "11z",
            "11def",
            "11d11",
            "11 11",
            " 1111",
            "1111 ",
            "1.1L"
        };

        for (int i = 0; i < positives.length; i++) {
            assertSeedPositive("lifted-pos-" + i, positives[i]);
        }
        for (int i = 0; i < negatives.length; i++) {
            assertSeedNegative("lifted-neg-" + i, negatives[i]);
        }

        String fuzz = data.consumeString(64);
        NumberUtils.isNumber(fuzz);
        try {
            NumberUtils.createNumber(fuzz);
        } catch (RuntimeException ignored) {
        }

        int n = data.consumeInt(-1_000_000, 1_000_000);
        String intAsString = Integer.toString(n);
        String longQualifiedUpper = intAsString + "L";
        String longQualifiedLower = intAsString + "l";
        String fracPart = data.consumeInt(0, 999999) + "";
        String decimalWithUpperL = intAsString + "." + fracPart + "L";
        String decimalWithLowerL = intAsString + "." + fracPart + "l";

        try {
            requireTrue("meta-int-l-upper", "NumberUtils.isNumber(\"" + longQualifiedUpper + "\")", NumberUtils.isNumber(longQualifiedUpper));
            requireTrue("meta-int-l-lower", "NumberUtils.isNumber(\"" + longQualifiedLower + "\")", NumberUtils.isNumber(longQualifiedLower));
            requireTrue("meta-int-l-upper-create", "checkCreateNumber(\"" + longQualifiedUpper + "\")", checkCreateNumber(longQualifiedUpper));
            requireTrue("meta-int-l-lower-create", "checkCreateNumber(\"" + longQualifiedLower + "\")", checkCreateNumber(longQualifiedLower));
        } catch (RuntimeException e) {
            throw e;
        }

        try {
            /* Contract visible in NumberUtils.isNumber source: for final 'L'/'l', "not allowing L with an exponent or decimal point".
               A throw-deleting or over-broad patch that merely suppresses one failing path would violate this observable result. */
            requireFalse("meta-decimal-l-upper", "NumberUtils.isNumber(\"" + decimalWithUpperL + "\")", NumberUtils.isNumber(decimalWithUpperL));
            requireFalse("meta-decimal-l-lower", "NumberUtils.isNumber(\"" + decimalWithLowerL + "\")", NumberUtils.isNumber(decimalWithLowerL));
            requireFalse("meta-decimal-l-upper-create", "checkCreateNumber(\"" + decimalWithUpperL + "\")", checkCreateNumber(decimalWithUpperL));
            requireFalse("meta-decimal-l-lower-create", "checkCreateNumber(\"" + decimalWithLowerL + "\")", checkCreateNumber(decimalWithLowerL));
        } catch (RuntimeException e) {
            throw e;
        }
    }
}
```

</details>

---
## [49] ⚙️ harness-attempt · `attempt_007`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-neg-24] semantic mismatch: expected false for NumberUtils.isNumber(1.1L) actual=true

---
## [50] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [36] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~11,033 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedOracles();

        String fuzz = data.consumeString(64);
        try {
            NumberUtils.isNumber(fuzz);
        } catch (Throwable ignored) {
        }
        try {
            NumberUtils.createNumber(fuzz);
        } catch (Throwable ignored) {
        }

        relationDecimalWithLongSuffixIsRejected(data);
        relationAcceptedPlainDecimalHasCreatableNumber(data);
        relationDecimalWithLowercaseLongSuffixIsRejected(data);
        relationAcceptedIntegerAgreesWithCreateNumber(data);
    }

    private static void runLiftedOracles() {
        final String[] positive = new String[] {
            "12345",
            "1234.5",
            ".12345",
            "1234E5",
            "1234E+5",
            "1234E-5",
            "123.4E5",
            "-1234",
            "-1234.5",
            "-.12345",
            "-1234E5",
            "0",
            "-0",
            "01234",
            "-01234",
            "0xABC123",
            "0x0",
            "123.4E21D",
            "-221.23F",
            "22338L"
        };
        final String[] positiveIds = new String[] {
            "testIsNumber-pos-1",
            "testIsNumber-pos-2",
            "testIsNumber-pos-3",
            "testIsNumber-pos-4",
            "testIsNumber-pos-5",
            "testIsNumber-pos-6",
            "testIsNumber-pos-7",
            "testIsNumber-pos-8",
            "testIsNumber-pos-9",
            "testIsNumber-pos-10",
            "testIsNumber-pos-11",
            "testIsNumber-pos-12",
            "testIsNumber-pos-13",
            "testIsNumber-pos-14",
            "testIsNumber-pos-15",
            "testIsNumber-pos-16",
            "testIsNumber-pos-17",
            "testIsNumber-pos-19",
            "testIsNumber-pos-20",
            "testIsNumber-pos-21"
        };

        final String[] negative = new String[] {
            null,
            "",
            "--2.3",
            ".12.3",
            "-123E",
            "-123E+-212",
            "-123E2.12",
            "0xGF",
            "0xFAE-1",
            ".",
            "-0ABC123",
            "123.4E-D",
            "123.4ED",
            "1234E5l",
            "11a",
            "1a",
            "a",
            "11g",
            "11z",
            "11def",
            "11d11",
            "11 11",
            " 1111",
            "1111 "
        };
        final String[] negativeIds = new String[] {
            "testIsNumber-neg-1",
            "testIsNumber-neg-2",
            "testIsNumber-neg-3",
            "testIsNumber-neg-4",
            "testIsNumber-neg-5",
            "testIsNumber-neg-6",
            "testIsNumber-neg-7",
            "testIsNumber-neg-8",
            "testIsNumber-neg-9",
            "testIsNumber-neg-10",
            "testIsNumber-neg-11",
            "testIsNumber-neg-12",
            "testIsNumber-neg-13",
            "testIsNumber-neg-14",
            "testIsNumber-neg-15",
            "testIsNumber-neg-16",
            "testIsNumber-neg-17",
            "testIsNumber-neg-18",
            "testIsNumber-neg-19",
            "testIsNumber-neg-20",
            "testIsNumber-neg-21",
            "testIsNumber-neg-22",
            "testIsNumber-neg-23",
            "testIsNumber-neg-24"
        };

        for (int i = 0; i < positive.length; i++) {
            boolean actualIsNumber = NumberUtils.isNumber(positive[i]);
            if (actualIsNumber != true) {
                throw new FuzzerSecurityIssueLow("[oracle:" + positiveIds[i] + "-isNumber] semantic mismatch: NumberUtils.isNumber(" + printable(positive[i]) + ") expected=true actual=" + actualIsNumber);
            }
            boolean actualCreate = checkCreateNumber(positive[i]);
            if (actualCreate != true) {
                throw new FuzzerSecurityIssueLow("[oracle:" + positiveIds[i] + "-createNumber] semantic mismatch: checkCreateNumber(" + printable(positive[i]) + ") expected=true actual=" + actualCreate);
            }
        }

        for (int i = 0; i < negative.length; i++) {
            boolean actualIsNumber = NumberUtils.isNumber(negative[i]);
            if (actualIsNumber != false) {
                throw new FuzzerSecurityIssueLow("[oracle:" + negativeIds[i] + "-isNumber] semantic mismatch: NumberUtils.isNumber(" + printable(negative[i]) + ") expected=false actual=" + actualIsNumber);
            }
            boolean actualCreate = checkCreateNumber(negative[i]);
            if (actualCreate != false) {
                throw new FuzzerSecurityIssueLow("[oracle:" + negativeIds[i] + "-createNumber] semantic mismatch: checkCreateNumber(" + printable(negative[i]) + ") expected=false actual=" + actualCreate);
            }
        }

        boolean lang521 = NumberUtils.isNumber("2.");
        if (lang521 != true) {
            throw new FuzzerSecurityIssueLow("[oracle:testIsNumber-LANG-521] semantic mismatch: NumberUtils.isNumber(\"2.\") expected=true actual=" + lang521);
        }

        boolean lang664 = NumberUtils.isNumber("1.1L");
        if (lang664 != false) {
            throw new FuzzerSecurityIssueLow("[oracle:testIsNumber-LANG-664] semantic mismatch: NumberUtils.isNumber(\"1.1L\") expected=false actual=" + lang664);
        }
    }

    private static void relationDecimalWithLongSuffixIsRejected(FuzzedDataProvider data) {
        String base = (data.consumeBoolean() ? "-" : "")
                + data.consumeInt(0, 1000000)
                + "."
                + data.consumeInt(0, 1000000);

        boolean plain;
        boolean withLong;
        try {
            plain = NumberUtils.isNumber(base);
            withLong = NumberUtils.isNumber(base + "L");
        } catch (Throwable e) {
            return;
        }

        // Contract used: the trusted test pins that a plain decimal like "1234.5" is accepted,
        // while a decimal form with a trailing long qualifier like "1.1L" is rejected.
        // A patch that only avoids the bad branch but returns the wrong boolean violates this.
        if (!plain) {
            throw new FuzzerSecurityIssueLow("[oracle:decimal_with_long_suffix_is_rejected-plain] metamorphic violation: isNumber(base) must accept valid plain decimal input=" + base + " actual=" + plain);
        }
        if (withLong) {
            throw new FuzzerSecurityIssueLow("[oracle:decimal_with_long_suffix_is_rejected-suffixed] metamorphic violation: isNumber(base+\"L\") must reject decimal-with-long-suffix input=" + (base + "L") + " actual=" + withLong);
        }
    }

    private static void relationAcceptedPlainDecimalHasCreatableNumber(FuzzedDataProvider data) {
        String s = (data.consumeBoolean() ? "-" : "")
                + data.consumeInt(0, 1000000)
                + "."
                + data.consumeInt(0, 1000000);

        boolean ok;
        Number n;
        try {
            ok = NumberUtils.isNumber(s);
            n = NumberUtils.createNumber(s);
        } catch (Throwable e) {
            return;
        }

        // Contract used: the trusted test couples isNumber(val) and checkCreateNumber(val) on
        // accepted decimal inputs; for a valid plain decimal, isNumber must be true and createNumber
        // must succeed with a non-null Number.
        if (!ok) {
            throw new FuzzerSecurityIssueLow("[oracle:accepted_plain_decimal_has_creatable_number-isNumber] metamorphic violation: valid plain decimal rejected input=" + s + " actual=" + ok);
        }
        if (n == null) {
            throw new FuzzerSecurityIssueLow("[oracle:accepted_plain_decimal_has_creatable_number-createNumber] metamorphic violation: createNumber returned null for accepted decimal input=" + s);
        }
    }

    private static void relationDecimalWithLowercaseLongSuffixIsRejected(FuzzedDataProvider data) {
        String base = (data.consumeBoolean() ? "-" : "")
                + data.consumeInt(0, 1000000)
                + "."
                + data.consumeInt(0, 1000000);

        boolean withUpper;
        boolean withLower;
        try {
            withUpper = NumberUtils.isNumber(base + "L");
            withLower = NumberUtils.isNumber(base + "l");
        } catch (Throwable e) {
            return;
        }

        // Contract used: the implementation and the tested API treat 'l' and 'L' as the same long
        // type qualifier branch; equivalent inputs differing only by qualifier case must agree.
        if (withUpper != withLower) {
            throw new FuzzerSecurityIssueLow("[oracle:decimal_long_suffix_case_agreement] metamorphic violation: equivalent long-suffix case variants disagreed inputUpper=" + (base + "L") + " upper=" + withUpper + " inputLower=" + (base + "l") + " lower=" + withLower);
        }
        if (withUpper) {
            throw new FuzzerSecurityIssueLow("[oracle:decimal_lowercase_long_suffix_is_rejected] metamorphic violation: decimal with lowercase/uppercase long suffix must be rejected inputUpper=" + (base + "L") + " inputLower=" + (base + "l") + " upper=" + withUpper + " lower=" + withLower);
        }
    }

    private static void relationAcceptedIntegerAgreesWithCreateNumber(FuzzedDataProvider data) {
        int v = data.consumeInt(-1000000, 1000000);
        String s = Integer.toString(v);

        boolean ok;
        Number n;
        try {
            ok = NumberUtils.isNumber(s);
            n = NumberUtils.createNumber(s);
        } catch (Throwable e) {
            return;
        }

        // Contract used: the trusted test asserts both NumberUtils.isNumber("12345") and
        // checkCreateNumber("12345"), likewise for "-1234", "0", "-0". By construction s is an
        // ordinary decimal integer string, so both real APIs must accept it consistently.
        if (!ok) {
            throw new FuzzerSecurityIssueLow("[oracle:accepted_integer_agrees_with_createNumber-isNumber] metamorphic violation: canonical integer string rejected input=" + s + " actual=" + ok);
        }
        if (n == null) {
            throw new FuzzerSecurityIssueLow("[oracle:accepted_integer_agrees_with_createNumber-createNumber] metamorphic violation: createNumber returned null for canonical integer input=" + s);
        }
    }

    private static boolean checkCreateNumber(String val) {
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

    private static String printable(String s) {
        if (s == null) {
            return "null";
        }
        return "\"" + s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t") + "\"";
    }
}
```

</details>

---
## [51] ⚙️ harness-attempt · `attempt_008`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:testIsNumber-LANG-664] semantic mismatch: NumberUtils.isNumber("1.1L") expected=false actual=true

---
## [52] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [40] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,804 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String fuzz = data.consumeString(64);
        NumberUtils.isNumber(fuzz);

        String[] positiveVals = new String[] {
            "12345",
            "1234.5",
            ".12345",
            "1234E5",
            "1234E+5",
            "1234E-5",
            "123.4E5",
            "-1234",
            "-1234.5",
            "-.12345",
            "-1234E5",
            "0",
            "-0",
            "01234",
            "-01234",
            "0xABC123",
            "0x0",
            "123.4E21D",
            "-221.23F",
            "22338L",
            "2."
        };

        for (int i = 0; i < positiveVals.length; i++) {
            String val = positiveVals[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (!actualIsNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-isNumber-true-" + i + "] semantic mismatch: NumberUtils.isNumber(\"" + val + "\") expected=true actual=false");
            }

            boolean actualCreateNumber = false;
            try {
                Object obj = NumberUtils.createNumber(val);
                actualCreateNumber = obj != null;
            } catch (NumberFormatException e) {
                actualCreateNumber = false;
            }
            if (!actualCreateNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-createNumber-true-" + i + "] semantic mismatch: checkCreateNumber(\"" + val + "\") expected=true actual=false");
            }
        }

        String[] negativeVals = new String[] {
            null,
            "",
            "--2.3",
            ".12.3",
            "-123E",
            "-123E+-212",
            "-123E2.12",
            "0xGF",
            "0xFAE-1",
            ".",
            "-0ABC123",
            "123.4E-D",
            "123.4ED",
            "1234E5l",
            "11a",
            "1a",
            "a",
            "11g",
            "11z",
            "11def",
            "11d11",
            "11 11",
            " 1111",
            "1111 ",
            "1.1L"
        };

        for (int i = 0; i < negativeVals.length; i++) {
            String val = negativeVals[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (actualIsNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-isNumber-false-" + i + "] semantic mismatch: NumberUtils.isNumber(" + String.valueOf(val) + ") expected=false actual=true");
            }

            boolean actualCreateNumber = false;
            try {
                Object obj = NumberUtils.createNumber(val);
                actualCreateNumber = obj != null;
            } catch (NumberFormatException e) {
                actualCreateNumber = false;
            }
            if (actualCreateNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-createNumber-false-" + i + "] semantic mismatch: checkCreateNumber(" + String.valueOf(val) + ") expected=false actual=true");
            }
        }

        int n = data.consumeInt(-1000000, 1000000);
        String canonicalInt = Integer.toString(n);

        // Contract justification: the lifted test asserts plain decimal integers like "12345", "-1234", "0", "-0" are numbers,
        // and checkCreateNumber succeeds on them. Since we construct the canonical decimal string from a known int first,
        // the expected answer is trusted by construction.
        boolean canonicalIsNumber = NumberUtils.isNumber(canonicalInt);
        if (!canonicalIsNumber) {
            throw new RuntimeException(
                "[oracle:constructed-canonical-int] metamorphic violation: canonical decimal int must be recognized input=" + canonicalInt + " lhs=false rhs=true");
        }
        boolean canonicalCreateNumber = false;
        try {
            Object obj = NumberUtils.createNumber(canonicalInt);
            canonicalCreateNumber = obj != null;
        } catch (Throwable t) {
            return;
        }
        if (!canonicalCreateNumber) {
            throw new RuntimeException(
                "[oracle:constructed-canonical-int-create] metamorphic violation: createNumber must accept canonical decimal int input=" + canonicalInt + " lhs=false rhs=true");
        }

        int m = data.consumeInt(0, 1000000);
        String decimalLongUpper = Integer.toString(m) + ".1L";
        String decimalLongLower = Integer.toString(m) + ".1l";

        // Contract justification: the patched method itself documents "not allowing L with an exponent or decimal point",
        // and the lifted test pins one concrete instance: "1.1L" must be false. Generalizing by construction to any
        // canonical integer prefix plus ".1L"/".1l" is sound because these inputs share the same decisive property:
        // they contain a decimal point and a long type qualifier. A patch that merely deletes or bypasses the check
        // would violate this observable post-condition without throwing.
        boolean upper = NumberUtils.isNumber(decimalLongUpper);
        boolean lower = NumberUtils.isNumber(decimalLongLower);
        if (upper) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-upper] metamorphic violation: decimal point with uppercase long qualifier must be rejected input=" + decimalLongUpper + " lhs=true rhs=false");
        }
        if (lower) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-lower] metamorphic violation: decimal point with lowercase long qualifier must be rejected input=" + decimalLongLower + " lhs=true rhs=false");
        }
        if (upper != lower) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-case] metamorphic violation: equivalent uppercase/lowercase long qualifiers must agree inputUpper=" + decimalLongUpper + " inputLower=" + decimalLongLower + " lhs=" + upper + " rhs=" + lower);
        }

        boolean upperCreate = false;
        boolean lowerCreate = false;
        try {
            Object obj = NumberUtils.createNumber(decimalLongUpper);
            upperCreate = obj != null;
        } catch (NumberFormatException e) {
            upperCreate = false;
        }
        try {
            Object obj = NumberUtils.createNumber(decimalLongLower);
            lowerCreate = obj != null;
        } catch (NumberFormatException e) {
            lowerCreate = false;
        }
        if (upperCreate) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-upper-create] metamorphic violation: createNumber must reject decimal point with uppercase long qualifier input=" + decimalLongUpper + " lhs=true rhs=false");
        }
        if (lowerCreate) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-lower-create] metamorphic violation: createNumber must reject decimal point with lowercase long qualifier input=" + decimalLongLower + " lhs=true rhs=false");
        }
        if (upperCreate != lowerCreate) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-case-create] metamorphic violation: createNumber case-equivalent long qualifiers must agree inputUpper=" + decimalLongUpper + " inputLower=" + decimalLongLower + " lhs=" + upperCreate + " rhs=" + lowerCreate);
        }
    }
}
```

</details>

---
## [53] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Every oracle alarm in your harness needs a name: the alarm near line 38 has no oracle ID (`throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow( "[oracle:lifted-isNumber-true-" + i + "] s

---
## [54] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~8,545 chars, 2 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [40] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String fuzz = data.consumeString(64);
        NumberUtils.isNumber(fuzz);

        String[] positiveVals = new String[] {
            "12345",
            "1234.5",
            ".12345",
            "1234E5",
            "1234E+5",
            "1234E-5",
            "123.4E5",
            "-1234",
            "-1234.5",
            "-.12345",
            "-1234E5",
            "0",
            "-0",
            "01234",
            "-01234",
            "0xABC123",
            "0x0",
            "123.4E21D",
            "-221.23F",
            "22338L",
            "2."
        };

        for (int i = 0; i < positiveVals.length; i++) {
            String val = positiveVals[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (!actualIsNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-isNumber-true-" + i + "] semantic mismatch: NumberUtils.isNumber(\"" + val + "\") expected=true actual=false");
            }

            boolean actualCreateNumber = false;
            try {
                Object obj = NumberUtils.createNumber(val);
                actualCreateNumber = obj != null;
            } catch (NumberFormatException e) {
                actualCreateNumber = false;
            }
            if (!actualCreateNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-createNumber-true-" + i + "] semantic mismatch: checkCreateNumber(\"" + val + "\") expected=true actual=false");
            }
        }

        String[] negativeVals = new String[] {
            null,
            "",
            "--2.3",
            ".12.3",
            "-123E",
            "-123E+-212",
            "-123E2.12",
            "0xGF",
            "0xFAE-1",
            ".",
            "-0ABC123",
            "123.4E-D",
            "123.4ED",
            "1234E5l",
            "11a",
            "1a",
            "a",
            "11g",
            "11z",
            "11def",
            "11d11",
            "11 11",
            " 1111",
            "1111 ",
            "1.1L"
        };

        for (int i = 0; i < negativeVals.length; i++) {
            String val = negativeVals[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (actualIsNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-isNumber-false-" + i + "] semantic mismatch: NumberUtils.isNumber(" + String.valueOf(val) + ") expected=false actual=true");
            }

            boolean actualCreateNumber = false;
            try {
                Object obj = NumberUtils.createNumber(val);
                actualCreateNumber = obj != null;
            } catch (NumberFormatException e) {
                actualCreateNumber = false;
            }
            if (actualCreateNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-createNumber-false-" + i + "] semantic mismatch: checkCreateNumber(" + String.valueOf(val) + ") expected=false actual=true");
            }
        }

        int n = data.consumeInt(-1000000, 1000000);
        String canonicalInt = Integer.toString(n);

        // Contract justification: the lifted test asserts plain decimal integers like "12345", "-1234", "0", "-0" are numbers,
        // and checkCreateNumber succeeds on them. Since we construct the canonical decimal string from a known int first,
        // the expected answer is trusted by construction.
        boolean canonicalIsNumber = NumberUtils.isNumber(canonicalInt);
        if (!canonicalIsNumber) {
            throw new RuntimeException(
                "[oracle:constructed-canonical-int] metamorphic violation: canonical decimal int must be recognized input=" + canonicalInt + " lhs=false rhs=true");
        }
        boolean canonicalCreateNumber = false;
        try {
            Object obj = NumberUtils.createNumber(canonicalInt);
            canonicalCreateNumber = obj != null;
        } catch (Throwable t) {
            return;
        }
        if (!canonicalCreateNumber) {
            throw new RuntimeException(
                "[oracle:constructed-canonical-int-create] metamorphic violation: createNumber must accept canonical decimal int input=" + canonicalInt + " lhs=false rhs=true");
        }

        int m = data.consumeInt(0, 1000000);
        String decimalLongUpper = Integer.toString(m) + ".1L";
        String decimalLongLower = Integer.toString(m) + ".1l";

        // Contract justification: the patched method itself documents "not allowing L with an exponent or decimal point",
        // and the lifted test pins one concrete instance: "1.1L" must be false. Generalizing by construction to any
        // canonical integer prefix plus ".1L"/".1l" is sound because these inputs share the same decisive property:
        // they contain a decimal point and a long type qualifier. A patch that merely deletes or bypasses the check
        // would violate this observable post-condition without throwing.
        boolean upper = NumberUtils.isNumber(decimalLongUpper);
        boolean lower = NumberUtils.isNumber(decimalLongLower);
        if (upper) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-upper] metamorphic violation: decimal point with uppercase long qualifier must be rejected input=" + decimalLongUpper + " lhs=true rhs=false");
        }
        if (lower) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-lower] metamorphic violation: decimal point with lowercase long qualifier must be rejected input=" + decimalLongLower + " lhs=true rhs=false");
        }
        if (upper != lower) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-case] metamorphic violation: equivalent uppercase/lowercase long qualifiers must agree inputUpper=" + decimalLongUpper + " inputLower=" + decimalLongLower + " lhs=" + upper + " rhs=" + lower);
        }

        boolean upperCreate = false;
        boolean lowerCreate = false;
        try {
            Object obj = NumberUtils.createNumber(decimalLongUpper);
            upperCreate = obj != null;
        } catch (NumberFormatException e) {
            upperCreate = false;
        }
        try {
            Object obj = NumberUtils.createNumber(decimalLongLower);
            lowerCreate = obj != null;
        } catch (NumberFormatException e) {
            lowerCreate = false;
        }
        if (upperCreate) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-upper-create] metamorphic violation: createNumber must reject decimal point with uppercase long qualifier input=" + decimalLongUpper + " lhs=true rhs=false");
        }
        if (lowerCreate) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-lower-create] metamorphic violation: createNumber must reject decimal point with lowercase long qualifier input=" + decimalLongLower + " lhs=true rhs=false");
        }
        if (upperCreate != lowerCreate) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-case-create] metamorphic violation: createNumber case-equivalent long qualifiers must agree inputUpper=" + decimalLongUpper + " inputLower=" + decimalLongLower + " lhs=" + upperCreate + " rhs=" + lowerCreate);
        }
    }
}
```
**[user]**
```
Every oracle alarm in your harness needs a name: the alarm near line 38 has no oracle ID (`throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow( "[oracle:lifted-isNumber-true-" + i + "] semantic mis...`) — start its message with "[oracle:<short-id>]" so acceptance can tell which check earned its place
Start each distinct check's message with [oracle:<short-id>], e.g. `throw new FuzzerSecurityIssueLow("[oracle:round-trip] semantic mismatch: ...")`. Use a DIFFERENT id per check. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~7,762 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String fuzz = data.consumeString(64);
        NumberUtils.isNumber(fuzz);

        String[] positiveVals = new String[] {
            "12345",
            "1234.5",
            ".12345",
            "1234E5",
            "1234E+5",
            "1234E-5",
            "123.4E5",
            "-1234",
            "-1234.5",
            "-.12345",
            "-1234E5",
            "0",
            "-0",
            "01234",
            "-01234",
            "0xABC123",
            "0x0",
            "123.4E21D",
            "-221.23F",
            "22338L",
            "2."
        };

        for (int i = 0; i < positiveVals.length; i++) {
            String val = positiveVals[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (!actualIsNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-isNumber-true] semantic mismatch: index=" + i + " input=" + val + " expected=true actual=false");
            }

            boolean actualCreateNumber = false;
            try {
                Object obj = NumberUtils.createNumber(val);
                actualCreateNumber = obj != null;
            } catch (NumberFormatException e) {
                actualCreateNumber = false;
            }
            if (!actualCreateNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-createNumber-true] semantic mismatch: index=" + i + " input=" + val + " expected=true actual=false");
            }
        }

        String[] negativeVals = new String[] {
            null,
            "",
            "--2.3",
            ".12.3",
            "-123E",
            "-123E+-212",
            "-123E2.12",
            "0xGF",
            "0xFAE-1",
            ".",
            "-0ABC123",
            "123.4E-D",
            "123.4ED",
            "1234E5l",
            "11a",
            "1a",
            "a",
            "11g",
            "11z",
            "11def",
            "11d11",
            "11 11",
            " 1111",
            "1111 ",
            "1.1L"
        };

        for (int i = 0; i < negativeVals.length; i++) {
            String val = negativeVals[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (actualIsNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-isNumber-false] semantic mismatch: index=" + i + " input=" + String.valueOf(val) + " expected=false actual=true");
            }

            boolean actualCreateNumber = false;
            try {
                Object obj = NumberUtils.createNumber(val);
                actualCreateNumber = obj != null;
            } catch (NumberFormatException e) {
                actualCreateNumber = false;
            }
            if (actualCreateNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-createNumber-false] semantic mismatch: index=" + i + " input=" + String.valueOf(val) + " expected=false actual=true");
            }
        }

        int n = data.consumeInt(-1000000, 1000000);
        String canonicalInt = Integer.toString(n);

        // Contract justification: the lifted test asserts plain decimal integers like "12345", "-1234", "0", "-0" are numbers,
        // and checkCreateNumber succeeds on them. Since we construct the canonical decimal string from a known int first,
        // the expected answer is trusted by construction.
        boolean canonicalIsNumber = NumberUtils.isNumber(canonicalInt);
        if (!canonicalIsNumber) {
            throw new RuntimeException(
                "[oracle:constructed-canonical-int] metamorphic violation: canonical decimal int must be recognized input=" + canonicalInt + " lhs=false rhs=true");
        }
        boolean canonicalCreateNumber = false;
        try {
            Object obj = NumberUtils.createNumber(canonicalInt);
            canonicalCreateNumber = obj != null;
        } catch (Throwable t) {
            return;
        }
        if (!canonicalCreateNumber) {
            throw new RuntimeException(
                "[oracle:constructed-canonical-int-create] metamorphic violation: createNumber must accept canonical decimal int input=" + canonicalInt + " lhs=false rhs=true");
        }

        int m = data.consumeInt(0, 1000000);
        String decimalLongUpper = Integer.toString(m) + ".1L";
        String decimalLongLower = Integer.toString(m) + ".1l";

        // Contract justification: the patched method itself documents "not allowing L with an exponent or decimal point",
        // and the lifted test pins one concrete instance: "1.1L" must be false. Generalizing by construction to any
        // canonical integer prefix plus ".1L"/".1l" is sound because these inputs share the same decisive property:
        // they contain a decimal point and a long type qualifier. A patch that merely deletes or bypasses the check
        // would violate this observable post-condition without throwing.
        boolean upper = NumberUtils.isNumber(decimalLongUpper);
        boolean lower = NumberUtils.isNumber(decimalLongLower);
        if (upper) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-upper] metamorphic violation: decimal point with uppercase long qualifier must be rejected input=" + decimalLongUpper + " lhs=true rhs=false");
        }
        if (lower) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-lower] metamorphic violation: decimal point with lowercase long qualifier must be rejected input=" + decimalLongLower + " lhs=true rhs=false");
        }
        if (upper != lower) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-case] metamorphic violation: equivalent uppercase/lowercase long qualifiers must agree inputUpper=" + decimalLongUpper + " inputLower=" + decimalLongLower + " lhs=" + upper + " rhs=" + lower);
        }

        boolean upperCreate = false;
        boolean lowerCreate = false;
        try {
            Object obj = NumberUtils.createNumber(decimalLongUpper);
            upperCreate = obj != null;
        } catch (NumberFormatException e) {
            upperCreate = false;
        }
        try {
            Object obj = NumberUtils.createNumber(decimalLongLower);
            lowerCreate = obj != null;
        } catch (NumberFormatException e) {
            lowerCreate = false;
        }
        if (upperCreate) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-upper-create] metamorphic violation: createNumber must reject decimal point with uppercase long qualifier input=" + decimalLongUpper + " lhs=true rhs=false");
        }
        if (lowerCreate) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-lower-create] metamorphic violation: createNumber must reject decimal point with lowercase long qualifier input=" + decimalLongLower + " lhs=true rhs=false");
        }
        if (upperCreate != lowerCreate) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-case-create] metamorphic violation: createNumber case-equivalent long qualifiers must agree inputUpper=" + decimalLongUpper + " inputLower=" + decimalLongLower + " lhs=" + upperCreate + " rhs=" + lowerCreate);
        }
    }
}
```

</details>

---
## [55] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `actualCreateNumber` (literal assignment, no rethrow, the ca

---
## [56] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~9,053 chars, 2 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [40] — not reprinted*
- *[assistant] message: identical to step [54] — not reprinted*
- *[user] message: identical to step [54] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String fuzz = data.consumeString(64);
        NumberUtils.isNumber(fuzz);

        String[] positiveVals = new String[] {
            "12345",
            "1234.5",
            ".12345",
            "1234E5",
            "1234E+5",
            "1234E-5",
            "123.4E5",
            "-1234",
            "-1234.5",
            "-.12345",
            "-1234E5",
            "0",
            "-0",
            "01234",
            "-01234",
            "0xABC123",
            "0x0",
            "123.4E21D",
            "-221.23F",
            "22338L",
            "2."
        };

        for (int i = 0; i < positiveVals.length; i++) {
            String val = positiveVals[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (!actualIsNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-isNumber-true] semantic mismatch: index=" + i + " input=" + val + " expected=true actual=false");
            }

            boolean actualCreateNumber = false;
            try {
                Object obj = NumberUtils.createNumber(val);
                actualCreateNumber = obj != null;
            } catch (NumberFormatException e) {
                actualCreateNumber = false;
            }
            if (!actualCreateNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-createNumber-true] semantic mismatch: index=" + i + " input=" + val + " expected=true actual=false");
            }
        }

        String[] negativeVals = new String[] {
            null,
            "",
            "--2.3",
            ".12.3",
            "-123E",
            "-123E+-212",
            "-123E2.12",
            "0xGF",
            "0xFAE-1",
            ".",
            "-0ABC123",
            "123.4E-D",
            "123.4ED",
            "1234E5l",
            "11a",
            "1a",
            "a",
            "11g",
            "11z",
            "11def",
            "11d11",
            "11 11",
            " 1111",
            "1111 ",
            "1.1L"
        };

        for (int i = 0; i < negativeVals.length; i++) {
            String val = negativeVals[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (actualIsNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-isNumber-false] semantic mismatch: index=" + i + " input=" + String.valueOf(val) + " expected=false actual=true");
            }

            boolean actualCreateNumber = false;
            try {
                Object obj = NumberUtils.createNumber(val);
                actualCreateNumber = obj != null;
            } catch (NumberFormatException e) {
                actualCreateNumber = false;
            }
            if (actualCreateNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-createNumber-false] semantic mismatch: index=" + i + " input=" + String.valueOf(val) + " expected=false actual=true");
            }
        }

        int n = data.consumeInt(-1000000, 1000000);
        String canonicalInt = Integer.toString(n);

        // Contract justification: the lifted test asserts plain decimal integers like "12345", "-1234", "0", "-0" are numbers,
        // and checkCreateNumber succeeds on them. Since we construct the canonical decimal string from a known int first,
        // the expected answer is trusted by construction.
        boolean canonicalIsNumber = NumberUtils.isNumber(canonicalInt);
        if (!canonicalIsNumber) {
            throw new RuntimeException(
                "[oracle:constructed-canonical-int] metamorphic violation: canonical decimal int must be recognized input=" + canonicalInt + " lhs=false rhs=true");
        }
        boolean canonicalCreateNumber = false;
        try {
            Object obj = NumberUtils.createNumber(canonicalInt);
            canonicalCreateNumber = obj != null;
        } catch (Throwable t) {
            return;
        }
        if (!canonicalCreateNumber) {
            throw new RuntimeException(
                "[oracle:constructed-canonical-int-create] metamorphic violation: createNumber must accept canonical decimal int input=" + canonicalInt + " lhs=false rhs=true");
        }

        int m = data.consumeInt(0, 1000000);
        String decimalLongUpper = Integer.toString(m) + ".1L";
        String decimalLongLower = Integer.toString(m) + ".1l";

        // Contract justification: the patched method itself documents "not allowing L with an exponent or decimal point",
        // and the lifted test pins one concrete instance: "1.1L" must be false. Generalizing by construction to any
        // canonical integer prefix plus ".1L"/".1l" is sound because these inputs share the same decisive property:
        // they contain a decimal point and a long type qualifier. A patch that merely deletes or bypasses the check
        // would violate this observable post-condition without throwing.
        boolean upper = NumberUtils.isNumber(decimalLongUpper);
        boolean lower = NumberUtils.isNumber(decimalLongLower);
        if (upper) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-upper] metamorphic violation: decimal point with uppercase long qualifier must be rejected input=" + decimalLongUpper + " lhs=true rhs=false");
        }
        if (lower) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-lower] metamorphic violation: decimal point with lowercase long qualifier must be rejected input=" + decimalLongLower + " lhs=true rhs=false");
        }
        if (upper != lower) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-case] metamorphic violation: equivalent uppercase/lowercase long qualifiers must agree inputUpper=" + decimalLongUpper + " inputLower=" + decimalLongLower + " lhs=" + upper + " rhs=" + lower);
        }

        boolean upperCreate = false;
        boolean lowerCreate = false;
        try {
            Object obj = NumberUtils.createNumber(decimalLongUpper);
            upperCreate = obj != null;
        } catch (NumberFormatException e) {
            upperCreate = false;
        }
        try {
            Object obj = NumberUtils.createNumber(decimalLongLower);
            lowerCreate = obj != null;
        } catch (NumberFormatException e) {
            lowerCreate = false;
        }
        if (upperCreate) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-upper-create] metamorphic violation: createNumber must reject decimal point with uppercase long qualifier input=" + decimalLongUpper + " lhs=true rhs=false");
        }
        if (lowerCreate) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-lower-create] metamorphic violation: createNumber must reject decimal point with lowercase long qualifier input=" + decimalLongLower + " lhs=true rhs=false");
        }
        if (upperCreate != lowerCreate) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-case-create] metamorphic violation: createNumber case-equivalent long qualifiers must agree inputUpper=" + decimalLongUpper + " inputLower=" + decimalLongLower + " lhs=" + upperCreate + " rhs=" + lowerCreate);
        }
    }
}
```
**[user]**
```
Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `actualCreateNumber` (literal assignment, no rethrow, the caught exception is never used) and the alarm near line 50 is conditioned on `actualCreateNumber` — the caught exception's identity is destroyed, so attribution cannot tell WHICH exception fired or whether the unpatched build crashes the same way. Rethrow the exception as the alarm's cause (`..., e`), or assert on the real observable instead of a swallowed boolean.
A caught exception is a REJECTION, never a violation, and flattening it into a flag destroys the identity attribution needs. Either assert on the real observable the test pins (compute the actual value and compare it), or if you must re-report a caught crash, re-throw it with the original as the cause: `new FuzzerSecurityIssueLow("[oracle:<id>] ...", e)`. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~7,314 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static boolean checkCreateNumber(String val) {
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

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String fuzz = data.consumeString(64);
        NumberUtils.isNumber(fuzz);

        String[] positiveVals = new String[] {
            "12345",
            "1234.5",
            ".12345",
            "1234E5",
            "1234E+5",
            "1234E-5",
            "123.4E5",
            "-1234",
            "-1234.5",
            "-.12345",
            "-1234E5",
            "0",
            "-0",
            "01234",
            "-01234",
            "0xABC123",
            "0x0",
            "123.4E21D",
            "-221.23F",
            "22338L",
            "2."
        };

        for (int i = 0; i < positiveVals.length; i++) {
            String val = positiveVals[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (!actualIsNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-isNumber-true] semantic mismatch: index=" + i + " input=" + val + " expected=true actual=false");
            }

            boolean actualCheckCreateNumber = checkCreateNumber(val);
            if (!actualCheckCreateNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-checkCreateNumber-true] semantic mismatch: index=" + i + " input=" + val + " expected=true actual=false");
            }
        }

        String[] negativeVals = new String[] {
            null,
            "",
            "--2.3",
            ".12.3",
            "-123E",
            "-123E+-212",
            "-123E2.12",
            "0xGF",
            "0xFAE-1",
            ".",
            "-0ABC123",
            "123.4E-D",
            "123.4ED",
            "1234E5l",
            "11a",
            "1a",
            "a",
            "11g",
            "11z",
            "11def",
            "11d11",
            "11 11",
            " 1111",
            "1111 ",
            "1.1L"
        };

        for (int i = 0; i < negativeVals.length; i++) {
            String val = negativeVals[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (actualIsNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-isNumber-false] semantic mismatch: index=" + i + " input=" + String.valueOf(val) + " expected=false actual=true");
            }

            boolean actualCheckCreateNumber = checkCreateNumber(val);
            if (actualCheckCreateNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-checkCreateNumber-false] semantic mismatch: index=" + i + " input=" + String.valueOf(val) + " expected=false actual=true");
            }
        }

        int n = data.consumeInt(-1000000, 1000000);
        String canonicalInt = Integer.toString(n);

        // Contract justification: the lifted test asserts plain decimal integers like "12345", "-1234", "0", "-0" are numbers,
        // and checkCreateNumber succeeds on them. Since we construct the canonical decimal string from a known int first,
        // the expected answer is trusted by construction.
        boolean canonicalIsNumber = NumberUtils.isNumber(canonicalInt);
        if (!canonicalIsNumber) {
            throw new RuntimeException(
                "[oracle:constructed-canonical-int] metamorphic violation: canonical decimal int must be recognized input=" + canonicalInt + " lhs=false rhs=true");
        }
        boolean canonicalCheckCreateNumber = checkCreateNumber(canonicalInt);
        if (!canonicalCheckCreateNumber) {
            throw new RuntimeException(
                "[oracle:constructed-canonical-int-checkCreateNumber] metamorphic violation: checkCreateNumber must accept canonical decimal int input=" + canonicalInt + " lhs=false rhs=true");
        }

        int m = data.consumeInt(0, 1000000);
        String decimalLongUpper = Integer.toString(m) + ".1L";
        String decimalLongLower = Integer.toString(m) + ".1l";

        // Contract justification: the patched method itself documents "not allowing L with an exponent or decimal point",
        // and the lifted test pins one concrete instance: "1.1L" must be false. Generalizing by construction to any
        // canonical integer prefix plus ".1L"/".1l" is sound because these inputs share the same decisive property:
        // they contain a decimal point and a long type qualifier. A patch that merely deletes or bypasses the check
        // would violate this observable post-condition without throwing.
        boolean upper = NumberUtils.isNumber(decimalLongUpper);
        boolean lower = NumberUtils.isNumber(decimalLongLower);
        if (upper) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-upper] metamorphic violation: decimal point with uppercase long qualifier must be rejected input=" + decimalLongUpper + " lhs=true rhs=false");
        }
        if (lower) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-lower] metamorphic violation: decimal point with lowercase long qualifier must be rejected input=" + decimalLongLower + " lhs=true rhs=false");
        }
        if (upper != lower) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-case] metamorphic violation: equivalent uppercase/lowercase long qualifiers must agree inputUpper=" + decimalLongUpper + " inputLower=" + decimalLongLower + " lhs=" + upper + " rhs=" + lower);
        }

        boolean upperCheckCreateNumber = checkCreateNumber(decimalLongUpper);
        boolean lowerCheckCreateNumber = checkCreateNumber(decimalLongLower);
        if (upperCheckCreateNumber) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-upper-checkCreateNumber] metamorphic violation: checkCreateNumber must reject decimal point with uppercase long qualifier input=" + decimalLongUpper + " lhs=true rhs=false");
        }
        if (lowerCheckCreateNumber) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-lower-checkCreateNumber] metamorphic violation: checkCreateNumber must reject decimal point with lowercase long qualifier input=" + decimalLongLower + " lhs=true rhs=false");
        }
        if (upperCheckCreateNumber != lowerCheckCreateNumber) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-case-checkCreateNumber] metamorphic violation: checkCreateNumber case-equivalent long qualifiers must agree inputUpper=" + decimalLongUpper + " inputLower=" + decimalLongLower + " lhs=" + upperCheckCreateNumber + " rhs=" + lowerCheckCreateNumber);
        }
    }
}
```

</details>

---
## [57] ⚙️ harness-attempt · `attempt_011`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-isNumber-false] semantic mismatch: index=24 input=1.1L expected=false actual=true

---
## [58] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [36] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,635 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedTestOracles();
        checkDecimalWithLongSuffixIsRejected(data);
        checkAcceptedPlainDecimalHasCreatableNumber(data);
        checkLongSuffixCaseAgreement(data);

        if (data.remainingBytes() > 0) {
            String s = data.consumeRemainingAsString();
            try {
                NumberUtils.isNumber(s);
            } catch (Throwable ignored) {
            }
            try {
                NumberUtils.createNumber(s);
            } catch (Throwable ignored) {
            }
        }
    }

    private static void checkLiftedTestOracles() {
        String[] positives = new String[] {
            "12345",
            "1234.5",
            ".12345",
            "1234E5",
            "1234E+5",
            "1234E-5",
            "123.4E5",
            "-1234",
            "-1234.5",
            "-.12345",
            "-1234E5",
            "0",
            "-0",
            "01234",
            "-01234",
            "0xABC123",
            "0x0",
            "123.4E21D",
            "-221.23F",
            "22338L",
            "2."
        };
        String[] negatives = new String[] {
            null,
            "",
            "--2.3",
            ".12.3",
            "-123E",
            "-123E+-212",
            "-123E2.12",
            "0xGF",
            "0xFAE-1",
            ".",
            "-0ABC123",
            "123.4E-D",
            "123.4ED",
            "1234E5l",
            "11a",
            "1a",
            "a",
            "11g",
            "11z",
            "11def",
            "11d11",
            "11 11",
            " 1111",
            "1111 ",
            "1.1L"
        };

        for (int i = 0; i < positives.length; i++) {
            String val = positives[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (!actualIsNumber) {
                throw new FuzzerSecurityIssueLow("[oracle:isNumber-pos-" + i + "] semantic mismatch: NumberUtils.isNumber(" + quote(val) + ") expected=true actual=false");
            }
            boolean actualCheckCreateNumber = checkCreateNumber(val);
            if (!actualCheckCreateNumber) {
                throw new FuzzerSecurityIssueLow("[oracle:createNumber-pos-" + i + "] semantic mismatch: checkCreateNumber(" + quote(val) + ") expected=true actual=false");
            }
        }

        for (int i = 0; i < negatives.length; i++) {
            String val = negatives[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (actualIsNumber) {
                throw new FuzzerSecurityIssueLow("[oracle:isNumber-neg-" + i + "] semantic mismatch: NumberUtils.isNumber(" + quote(val) + ") expected=false actual=true");
            }
            boolean actualCheckCreateNumber = checkCreateNumber(val);
            if (actualCheckCreateNumber) {
                throw new FuzzerSecurityIssueLow("[oracle:createNumber-neg-" + i + "] semantic mismatch: checkCreateNumber(" + quote(val) + ") expected=false actual=true");
            }
        }
    }

    private static void checkDecimalWithLongSuffixIsRejected(FuzzedDataProvider data) {
        String base;
        boolean plain;
        boolean withLong;
        try {
            int whole = data.consumeInt(0, 1000000);
            int frac = data.consumeInt(0, 1000000);
            boolean neg = data.consumeBoolean();
            base = (neg ? "-" : "") + whole + "." + frac;
        } catch (Throwable t) {
            return;
        }

        try {
            plain = NumberUtils.isNumber(base);
        } catch (Throwable t) {
            return;
        }

        try {
            withLong = NumberUtils.isNumber(base + "L");
        } catch (Throwable t) {
            return;
        }

        // Contract exercised by the trusted test: a valid decimal string is accepted,
        // but appending 'L' makes it invalid because long qualifiers are not allowed with decimal points.
        if (!plain) {
            throw new FuzzerSecurityIssueLow("relation decimal_with_long_suffix_is_rejected violated: plain decimal should be accepted but isNumber(" + quote(base) + ") was false");
        }
        if (withLong) {
            throw new FuzzerSecurityIssueLow("relation decimal_with_long_suffix_is_rejected violated: decimal with long suffix should be rejected but isNumber(" + quote(base + "L") + ") was true");
        }
    }

    private static void checkAcceptedPlainDecimalHasCreatableNumber(FuzzedDataProvider data) {
        String s;
        boolean ok;
        Number n;
        try {
            int whole = data.consumeInt(0, 1000000);
            int frac = data.consumeInt(0, 1000000);
            s = (data.consumeBoolean() ? "-" : "") + whole + "." + frac;
        } catch (Throwable t) {
            return;
        }

        try {
            ok = NumberUtils.isNumber(s);
        } catch (Throwable t) {
            return;
        }
        if (!ok) {
            throw new FuzzerSecurityIssueLow("relation accepted_plain_decimal_has_creatable_number violated: valid plain decimal was rejected by isNumber(" + quote(s) + ")");
        }

        try {
            n = NumberUtils.createNumber(s);
        } catch (Throwable t) {
            return;
        }

        // Contract lifted from the test's paired assertions: for accepted numeric strings,
        // createNumber succeeds and returns a non-null Number rather than silently losing the value.
        if (n == null) {
            throw new FuzzerSecurityIssueLow("relation accepted_plain_decimal_has_creatable_number violated: createNumber(" + quote(s) + ") returned null for an accepted number string");
        }
    }

    private static void checkLongSuffixCaseAgreement(FuzzedDataProvider data) {
        String base;
        boolean upper;
        boolean lower;
        try {
            int whole = data.consumeInt(0, 1000000);
            int frac = data.consumeInt(0, 1000000);
            boolean neg = data.consumeBoolean();
            base = (neg ? "-" : "") + whole + "." + frac;
        } catch (Throwable t) {
            return;
        }

        try {
            upper = NumberUtils.isNumber(base + "L");
        } catch (Throwable t) {
            return;
        }
        try {
            lower = NumberUtils.isNumber(base + "l");
        } catch (Throwable t) {
            return;
        }

        // The implementation tests 'l' and 'L' in the same branch, so equivalent inputs differing
        // only by suffix case must agree; a guard that merely hides one path would break this.
        if (upper != lower) {
            throw new FuzzerSecurityIssueLow("[oracle:long-suffix-case-agreement] metamorphic violation: isNumber should agree for equivalent suffix case input=" + quote(base) + " lhs=" + upper + " rhs=" + lower);
        }
    }

    private static boolean checkCreateNumber(String val) {
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

    private static String quote(String s) {
        return s == null ? "null" : "\"" + s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t").replace("\"", "\\\"") + "\"";
    }
}
```

</details>

---
## [59] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Every oracle alarm in your harness needs a name: the alarm near line 82 has no oracle ID (`throw new FuzzerSecurityIssueLow("[oracle:isNumber-pos-" + i + "] semantic mismatch: NumberUtils.isNumber(" +

---
## [60] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~8,376 chars, 2 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [36] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedTestOracles();
        checkDecimalWithLongSuffixIsRejected(data);
        checkAcceptedPlainDecimalHasCreatableNumber(data);
        checkLongSuffixCaseAgreement(data);

        if (data.remainingBytes() > 0) {
            String s = data.consumeRemainingAsString();
            try {
                NumberUtils.isNumber(s);
            } catch (Throwable ignored) {
            }
            try {
                NumberUtils.createNumber(s);
            } catch (Throwable ignored) {
            }
        }
    }

    private static void checkLiftedTestOracles() {
        String[] positives = new String[] {
            "12345",
            "1234.5",
            ".12345",
            "1234E5",
            "1234E+5",
            "1234E-5",
            "123.4E5",
            "-1234",
            "-1234.5",
            "-.12345",
            "-1234E5",
            "0",
            "-0",
            "01234",
            "-01234",
            "0xABC123",
            "0x0",
            "123.4E21D",
            "-221.23F",
            "22338L",
            "2."
        };
        String[] negatives = new String[] {
            null,
            "",
            "--2.3",
            ".12.3",
            "-123E",
            "-123E+-212",
            "-123E2.12",
            "0xGF",
            "0xFAE-1",
            ".",
            "-0ABC123",
            "123.4E-D",
            "123.4ED",
            "1234E5l",
            "11a",
            "1a",
            "a",
            "11g",
            "11z",
            "11def",
            "11d11",
            "11 11",
            " 1111",
            "1111 ",
            "1.1L"
        };

        for (int i = 0; i < positives.length; i++) {
            String val = positives[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (!actualIsNumber) {
                throw new FuzzerSecurityIssueLow("[oracle:isNumber-pos-" + i + "] semantic mismatch: NumberUtils.isNumber(" + quote(val) + ") expected=true actual=false");
            }
            boolean actualCheckCreateNumber = checkCreateNumber(val);
            if (!actualCheckCreateNumber) {
                throw new FuzzerSecurityIssueLow("[oracle:createNumber-pos-" + i + "] semantic mismatch: checkCreateNumber(" + quote(val) + ") expected=true actual=false");
            }
        }

        for (int i = 0; i < negatives.length; i++) {
            String val = negatives[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (actualIsNumber) {
                throw new FuzzerSecurityIssueLow("[oracle:isNumber-neg-" + i + "] semantic mismatch: NumberUtils.isNumber(" + quote(val) + ") expected=false actual=true");
            }
            boolean actualCheckCreateNumber = checkCreateNumber(val);
            if (actualCheckCreateNumber) {
                throw new FuzzerSecurityIssueLow("[oracle:createNumber-neg-" + i + "] semantic mismatch: checkCreateNumber(" + quote(val) + ") expected=false actual=true");
            }
        }
    }

    private static void checkDecimalWithLongSuffixIsRejected(FuzzedDataProvider data) {
        String base;
        boolean plain;
        boolean withLong;
        try {
            int whole = data.consumeInt(0, 1000000);
            int frac = data.consumeInt(0, 1000000);
            boolean neg = data.consumeBoolean();
            base = (neg ? "-" : "") + whole + "." + frac;
        } catch (Throwable t) {
            return;
        }

        try {
            plain = NumberUtils.isNumber(base);
        } catch (Throwable t) {
            return;
        }

        try {
            withLong = NumberUtils.isNumber(base + "L");
        } catch (Throwable t) {
            return;
        }

        // Contract exercised by the trusted test: a valid decimal string is accepted,
        // but appending 'L' makes it invalid because long qualifiers are not allowed with decimal points.
        if (!plain) {
            throw new FuzzerSecurityIssueLow("relation decimal_with_long_suffix_is_rejected violated: plain decimal should be accepted but isNumber(" + quote(base) + ") was false");
        }
        if (withLong) {
            throw new FuzzerSecurityIssueLow("relation decimal_with_long_suffix_is_rejected violated: decimal with long suffix should be rejected but isNumber(" + quote(base + "L") + ") was true");
        }
    }

    private static void checkAcceptedPlainDecimalHasCreatableNumber(FuzzedDataProvider data) {
        String s;
        boolean ok;
        Number n;
        try {
            int whole = data.consumeInt(0, 1000000);
            int frac = data.consumeInt(0, 1000000);
            s = (data.consumeBoolean() ? "-" : "") + whole + "." + frac;
        } catch (Throwable t) {
            return;
        }

        try {
            ok = NumberUtils.isNumber(s);
        } catch (Throwable t) {
            return;
        }
        if (!ok) {
            throw new FuzzerSecurityIssueLow("relation accepted_plain_decimal_has_creatable_number violated: valid plain decimal was rejected by isNumber(" + quote(s) + ")");
        }

        try {
            n = NumberUtils.createNumber(s);
        } catch (Throwable t) {
            return;
        }

        // Contract lifted from the test's paired assertions: for accepted numeric strings,
        // createNumber succeeds and returns a non-null Number rather than silently losing the value.
        if (n == null) {
            throw new FuzzerSecurityIssueLow("relation accepted_plain_decimal_has_creatable_number violated: createNumber(" + quote(s) + ") returned null for an accepted number string");
        }
    }

    private static void checkLongSuffixCaseAgreement(FuzzedDataProvider data) {
        String base;
        boolean upper;
        boolean lower;
        try {
            int whole = data.consumeInt(0, 1000000);
            int frac = data.consumeInt(0, 1000000);
            boolean neg = data.consumeBoolean();
            base = (neg ? "-" : "") + whole + "." + frac;
        } catch (Throwable t) {
            return;
        }

        try {
            upper = NumberUtils.isNumber(base + "L");
        } catch (Throwable t) {
            return;
        }
        try {
            lower = NumberUtils.isNumber(base + "l");
        } catch (Throwable t) {
            return;
        }

        // The implementation tests 'l' and 'L' in the same branch, so equivalent inputs differing
        // only by suffix case must agree; a guard that merely hides one path would break this.
        if (upper != lower) {
            throw new FuzzerSecurityIssueLow("[oracle:long-suffix-case-agreement] metamorphic violation: isNumber should agree for equivalent suffix case input=" + quote(base) + " lhs=" + upper + " rhs=" + lower);
        }
    }

    private static boolean checkCreateNumber(String val) {
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

    private static String quote(String s) {
        return s == null ? "null" : "\"" + s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t").replace("\"", "\\\"") + "\"";
    }
}
```
**[user]**
```
Every oracle alarm in your harness needs a name: the alarm near line 82 has no oracle ID (`throw new FuzzerSecurityIssueLow("[oracle:isNumber-pos-" + i + "] semantic mismatch: NumberUtils.isNumber(" + quote(val)...`) — start its message with "[oracle:<short-id>]" so acceptance can tell which check earned its place
Start each distinct check's message with [oracle:<short-id>], e.g. `throw new FuzzerSecurityIssueLow("[oracle:round-trip] semantic mismatch: ...")`. Use a DIFFERENT id per check. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~7,613 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedTestOracles();
        checkDecimalWithLongSuffixIsRejected(data);
        checkAcceptedPlainDecimalHasCreatableNumber(data);
        checkLongSuffixCaseAgreement(data);

        if (data.remainingBytes() > 0) {
            String s = data.consumeRemainingAsString();
            try {
                NumberUtils.isNumber(s);
            } catch (Throwable ignored) {
            }
            try {
                NumberUtils.createNumber(s);
            } catch (Throwable ignored) {
            }
        }
    }

    private static void checkLiftedTestOracles() {
        String[] positives = new String[] {
            "12345",
            "1234.5",
            ".12345",
            "1234E5",
            "1234E+5",
            "1234E-5",
            "123.4E5",
            "-1234",
            "-1234.5",
            "-.12345",
            "-1234E5",
            "0",
            "-0",
            "01234",
            "-01234",
            "0xABC123",
            "0x0",
            "123.4E21D",
            "-221.23F",
            "22338L",
            "2."
        };
        String[] negatives = new String[] {
            null,
            "",
            "--2.3",
            ".12.3",
            "-123E",
            "-123E+-212",
            "-123E2.12",
            "0xGF",
            "0xFAE-1",
            ".",
            "-0ABC123",
            "123.4E-D",
            "123.4ED",
            "1234E5l",
            "11a",
            "1a",
            "a",
            "11g",
            "11z",
            "11def",
            "11d11",
            "11 11",
            " 1111",
            "1111 ",
            "1.1L"
        };

        for (int i = 0; i < positives.length; i++) {
            String val = positives[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (!actualIsNumber) {
                throw new FuzzerSecurityIssueLow("[oracle:isNumber-pos-" + i + "] semantic mismatch: NumberUtils.isNumber(" + quote(val) + ") expected=true actual=false");
            }
            boolean actualCheckCreateNumber = checkCreateNumber(val);
            if (!actualCheckCreateNumber) {
                throw new FuzzerSecurityIssueLow("[oracle:createNumber-pos-" + i + "] semantic mismatch: checkCreateNumber(" + quote(val) + ") expected=true actual=false");
            }
        }

        for (int i = 0; i < negatives.length; i++) {
            String val = negatives[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (actualIsNumber) {
                throw new FuzzerSecurityIssueLow("[oracle:isNumber-neg-" + i + "] semantic mismatch: NumberUtils.isNumber(" + quote(val) + ") expected=false actual=true");
            }
            boolean actualCheckCreateNumber = checkCreateNumber(val);
            if (actualCheckCreateNumber) {
                throw new FuzzerSecurityIssueLow("[oracle:createNumber-neg-" + i + "] semantic mismatch: checkCreateNumber(" + quote(val) + ") expected=false actual=true");
            }
        }
    }

    private static void checkDecimalWithLongSuffixIsRejected(FuzzedDataProvider data) {
        String base;
        boolean plain;
        boolean withLong;
        try {
            int whole = data.consumeInt(0, 1000000);
            int frac = data.consumeInt(0, 1000000);
            boolean neg = data.consumeBoolean();
            base = (neg ? "-" : "") + whole + "." + frac;
        } catch (Throwable t) {
            return;
        }

        try {
            plain = NumberUtils.isNumber(base);
        } catch (Throwable t) {
            return;
        }

        try {
            withLong = NumberUtils.isNumber(base + "L");
        } catch (Throwable t) {
            return;
        }

        // Contract exercised by the trusted test: a valid decimal string is accepted,
        // but appending 'L' makes it invalid because long qualifiers are not allowed with decimal points.
        if (!plain) {
            throw new FuzzerSecurityIssueLow("[oracle:decimal-long-rejected-plain] semantic mismatch: plain decimal should be accepted but isNumber(" + quote(base) + ") was false");
        }
        if (withLong) {
            throw new FuzzerSecurityIssueLow("[oracle:decimal-long-rejected-suffix] semantic mismatch: decimal with long suffix should be rejected but isNumber(" + quote(base + "L") + ") was true");
        }
    }

    private static void checkAcceptedPlainDecimalHasCreatableNumber(FuzzedDataProvider data) {
        String s;
        boolean ok;
        Number n;
        try {
            int whole = data.consumeInt(0, 1000000);
            int frac = data.consumeInt(0, 1000000);
            s = (data.consumeBoolean() ? "-" : "") + whole + "." + frac;
        } catch (Throwable t) {
            return;
        }

        try {
            ok = NumberUtils.isNumber(s);
        } catch (Throwable t) {
            return;
        }
        if (!ok) {
            throw new FuzzerSecurityIssueLow("[oracle:plain-decimal-accepted] semantic mismatch: valid plain decimal was rejected by isNumber(" + quote(s) + ")");
        }

        try {
            n = NumberUtils.createNumber(s);
        } catch (Throwable t) {
            return;
        }

        // Contract lifted from the test's paired assertions: for accepted numeric strings,
        // createNumber succeeds and returns a non-null Number rather than silently losing the value.
        if (n == null) {
            throw new FuzzerSecurityIssueLow("[oracle:plain-decimal-creatable] semantic mismatch: createNumber(" + quote(s) + ") returned null for an accepted number string");
        }
    }

    private static void checkLongSuffixCaseAgreement(FuzzedDataProvider data) {
        String base;
        boolean upper;
        boolean lower;
        try {
            int whole = data.consumeInt(0, 1000000);
            int frac = data.consumeInt(0, 1000000);
            boolean neg = data.consumeBoolean();
            base = (neg ? "-" : "") + whole + "." + frac;
        } catch (Throwable t) {
            return;
        }

        try {
            upper = NumberUtils.isNumber(base + "L");
        } catch (Throwable t) {
            return;
        }
        try {
            lower = NumberUtils.isNumber(base + "l");
        } catch (Throwable t) {
            return;
        }

        // The implementation tests 'l' and 'L' in the same branch, so equivalent inputs differing
        // only by suffix case must agree; a guard that merely hides one path would break this.
        if (upper != lower) {
            throw new FuzzerSecurityIssueLow("[oracle:long-suffix-case-agreement] metamorphic violation: isNumber should agree for equivalent suffix case input=" + quote(base) + " lhs=" + upper + " rhs=" + lower);
        }
    }

    private static boolean checkCreateNumber(String val) {
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

    private static String quote(String s) {
        return s == null ? "null" : "\"" + s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t").replace("\"", "\\\"") + "\"";
    }
}
```

</details>

---
## [61] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Every oracle alarm in your harness needs a name: the alarm near line 82 has no oracle ID (`throw new FuzzerSecurityIssueLow("[oracle:isNumber-pos-" + i + "] semantic mismatch: NumberUtils.isNumber(" +

---
## [62] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~7,934 chars, 1 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [36] — not reprinted*
- *[assistant] message: identical to step [60] — not reprinted*
- *[user] message: identical to step [60] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedTestOracles();
        checkDecimalWithLongSuffixIsRejected(data);
        checkAcceptedPlainDecimalHasCreatableNumber(data);
        checkLongSuffixCaseAgreement(data);

        if (data.remainingBytes() > 0) {
            String s = data.consumeRemainingAsString();
            try {
                NumberUtils.isNumber(s);
            } catch (Throwable ignored) {
            }
            try {
                NumberUtils.createNumber(s);
            } catch (Throwable ignored) {
            }
        }
    }

    private static void checkLiftedTestOracles() {
        String[] positives = new String[] {
            "12345",
            "1234.5",
            ".12345",
            "1234E5",
            "1234E+5",
            "1234E-5",
            "123.4E5",
            "-1234",
            "-1234.5",
            "-.12345",
            "-1234E5",
            "0",
            "-0",
            "01234",
            "-01234",
            "0xABC123",
            "0x0",
            "123.4E21D",
            "-221.23F",
            "22338L",
            "2."
        };
        String[] negatives = new String[] {
            null,
            "",
            "--2.3",
            ".12.3",
            "-123E",
            "-123E+-212",
            "-123E2.12",
            "0xGF",
            "0xFAE-1",
            ".",
            "-0ABC123",
            "123.4E-D",
            "123.4ED",
            "1234E5l",
            "11a",
            "1a",
            "a",
            "11g",
            "11z",
            "11def",
            "11d11",
            "11 11",
            " 1111",
            "1111 ",
            "1.1L"
        };

        for (int i = 0; i < positives.length; i++) {
            String val = positives[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (!actualIsNumber) {
                throw new FuzzerSecurityIssueLow("[oracle:isNumber-pos-" + i + "] semantic mismatch: NumberUtils.isNumber(" + quote(val) + ") expected=true actual=false");
            }
            boolean actualCheckCreateNumber = checkCreateNumber(val);
            if (!actualCheckCreateNumber) {
                throw new FuzzerSecurityIssueLow("[oracle:createNumber-pos-" + i + "] semantic mismatch: checkCreateNumber(" + quote(val) + ") expected=true actual=false");
            }
        }

        for (int i = 0; i < negatives.length; i++) {
            String val = negatives[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (actualIsNumber) {
                throw new FuzzerSecurityIssueLow("[oracle:isNumber-neg-" + i + "] semantic mismatch: NumberUtils.isNumber(" + quote(val) + ") expected=false actual=true");
            }
            boolean actualCheckCreateNumber = checkCreateNumber(val);
            if (actualCheckCreateNumber) {
                throw new FuzzerSecurityIssueLow("[oracle:createNumber-neg-" + i + "] semantic mismatch: checkCreateNumber(" + quote(val) + ") expected=false actual=true");
            }
        }
    }

    private static void checkDecimalWithLongSuffixIsRejected(FuzzedDataProvider data) {
        String base;
        boolean plain;
        boolean withLong;
        try {
            int whole = data.consumeInt(0, 1000000);
            int frac = data.consumeInt(0, 1000000);
            boolean neg = data.consumeBoolean();
            base = (neg ? "-" : "") + whole + "." + frac;
        } catch (Throwable t) {
            return;
        }

        try {
            plain = NumberUtils.isNumber(base);
        } catch (Throwable t) {
            return;
        }

        try {
            withLong = NumberUtils.isNumber(base + "L");
        } catch (Throwable t) {
            return;
        }

        // Contract exercised by the trusted test: a valid decimal string is accepted,
        // but appending 'L' makes it invalid because long qualifiers are not allowed with decimal points.
        if (!plain) {
            throw new FuzzerSecurityIssueLow("[oracle:decimal-long-rejected-plain] semantic mismatch: plain decimal should be accepted but isNumber(" + quote(base) + ") was false");
        }
        if (withLong) {
            throw new FuzzerSecurityIssueLow("[oracle:decimal-long-rejected-suffix] semantic mismatch: decimal with long suffix should be rejected but isNumber(" + quote(base + "L") + ") was true");
        }
    }

    private static void checkAcceptedPlainDecimalHasCreatableNumber(FuzzedDataProvider data) {
        String s;
        boolean ok;
        Number n;
        try {
            int whole = data.consumeInt(0, 1000000);
            int frac = data.consumeInt(0, 1000000);
            s = (data.consumeBoolean() ? "-" : "") + whole + "." + frac;
        } catch (Throwable t) {
            return;
        }

        try {
            ok = NumberUtils.isNumber(s);
        } catch (Throwable t) {
            return;
        }
        if (!ok) {
            throw new FuzzerSecurityIssueLow("[oracle:plain-decimal-accepted] semantic mismatch: valid plain decimal was rejected by isNumber(" + quote(s) + ")");
        }

        try {
            n = NumberUtils.createNumber(s);
        } catch (Throwable t) {
            return;
        }

        // Contract lifted from the test's paired assertions: for accepted numeric strings,
        // createNumber succeeds and returns a non-null Number rather than silently losing the value.
        if (n == null) {
            throw new FuzzerSecurityIssueLow("[oracle:plain-decimal-creatable] semantic mismatch: createNumber(" + quote(s) + ") returned null for an accepted number string");
        }
    }

    private static void checkLongSuffixCaseAgreement(FuzzedDataProvider data) {
        String base;
        boolean upper;
        boolean lower;
        try {
            int whole = data.consumeInt(0, 1000000);
            int frac = data.consumeInt(0, 1000000);
            boolean neg = data.consumeBoolean();
            base = (neg ? "-" : "") + whole + "." + frac;
        } catch (Throwable t) {
            return;
        }

        try {
            upper = NumberUtils.isNumber(base + "L");
        } catch (Throwable t) {
            return;
        }
        try {
            lower = NumberUtils.isNumber(base + "l");
        } catch (Throwable t) {
            return;
        }

        // The implementation tests 'l' and 'L' in the same branch, so equivalent inputs differing
        // only by suffix case must agree; a guard that merely hides one path would break this.
        if (upper != lower) {
            throw new FuzzerSecurityIssueLow("[oracle:long-suffix-case-agreement] metamorphic violation: isNumber should agree for equivalent suffix case input=" + quote(base) + " lhs=" + upper + " rhs=" + lower);
        }
    }

    private static boolean checkCreateNumber(String val) {
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

    private static String quote(String s) {
        return s == null ? "null" : "\"" + s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t").replace("\"", "\\\"") + "\"";
    }
}
```
- *[user] message: identical to step [60] — not reprinted*

</details>
<details open><summary>▸ Output (~7,685 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedTestOracles();
        checkDecimalWithLongSuffixIsRejected(data);
        checkAcceptedPlainDecimalHasCreatableNumber(data);
        checkLongSuffixCaseAgreement(data);

        if (data.remainingBytes() > 0) {
            String s = data.consumeRemainingAsString();
            try {
                NumberUtils.isNumber(s);
            } catch (Throwable ignored) {
            }
            try {
                NumberUtils.createNumber(s);
            } catch (Throwable ignored) {
            }
        }
    }

    private static void checkLiftedTestOracles() {
        String[] positives = new String[] {
            "12345",
            "1234.5",
            ".12345",
            "1234E5",
            "1234E+5",
            "1234E-5",
            "123.4E5",
            "-1234",
            "-1234.5",
            "-.12345",
            "-1234E5",
            "0",
            "-0",
            "01234",
            "-01234",
            "0xABC123",
            "0x0",
            "123.4E21D",
            "-221.23F",
            "22338L",
            "2."
        };
        String[] negatives = new String[] {
            null,
            "",
            "--2.3",
            ".12.3",
            "-123E",
            "-123E+-212",
            "-123E2.12",
            "0xGF",
            "0xFAE-1",
            ".",
            "-0ABC123",
            "123.4E-D",
            "123.4ED",
            "1234E5l",
            "11a",
            "1a",
            "a",
            "11g",
            "11z",
            "11def",
            "11d11",
            "11 11",
            " 1111",
            "1111 ",
            "1.1L"
        };

        for (int i = 0; i < positives.length; i++) {
            String val = positives[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (!actualIsNumber) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-isNumber-positive] semantic mismatch: index=" + i + " NumberUtils.isNumber(" + quote(val) + ") expected=true actual=false");
            }
            boolean actualCheckCreateNumber = checkCreateNumber(val);
            if (!actualCheckCreateNumber) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-createNumber-positive] semantic mismatch: index=" + i + " checkCreateNumber(" + quote(val) + ") expected=true actual=false");
            }
        }

        for (int i = 0; i < negatives.length; i++) {
            String val = negatives[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (actualIsNumber) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-isNumber-negative] semantic mismatch: index=" + i + " NumberUtils.isNumber(" + quote(val) + ") expected=false actual=true");
            }
            boolean actualCheckCreateNumber = checkCreateNumber(val);
            if (actualCheckCreateNumber) {
                throw new FuzzerSecurityIssueLow("[oracle:lifted-createNumber-negative] semantic mismatch: index=" + i + " checkCreateNumber(" + quote(val) + ") expected=false actual=true");
            }
        }
    }

    private static void checkDecimalWithLongSuffixIsRejected(FuzzedDataProvider data) {
        String base;
        boolean plain;
        boolean withLong;
        try {
            int whole = data.consumeInt(0, 1000000);
            int frac = data.consumeInt(0, 1000000);
            boolean neg = data.consumeBoolean();
            base = (neg ? "-" : "") + whole + "." + frac;
        } catch (Throwable t) {
            return;
        }

        try {
            plain = NumberUtils.isNumber(base);
        } catch (Throwable t) {
            return;
        }

        try {
            withLong = NumberUtils.isNumber(base + "L");
        } catch (Throwable t) {
            return;
        }

        // Contract exercised by the trusted test: a valid decimal string is accepted,
        // but appending 'L' makes it invalid because long qualifiers are not allowed with decimal points.
        if (!plain) {
            throw new FuzzerSecurityIssueLow("[oracle:decimal-long-rejected-plain] semantic mismatch: plain decimal should be accepted but isNumber(" + quote(base) + ") was false");
        }
        if (withLong) {
            throw new FuzzerSecurityIssueLow("[oracle:decimal-long-rejected-suffix] semantic mismatch: decimal with long suffix should be rejected but isNumber(" + quote(base + "L") + ") was true");
        }
    }

    private static void checkAcceptedPlainDecimalHasCreatableNumber(FuzzedDataProvider data) {
        String s;
        boolean ok;
        Number n;
        try {
            int whole = data.consumeInt(0, 1000000);
            int frac = data.consumeInt(0, 1000000);
            s = (data.consumeBoolean() ? "-" : "") + whole + "." + frac;
        } catch (Throwable t) {
            return;
        }

        try {
            ok = NumberUtils.isNumber(s);
        } catch (Throwable t) {
            return;
        }
        if (!ok) {
            throw new FuzzerSecurityIssueLow("[oracle:plain-decimal-accepted] semantic mismatch: valid plain decimal was rejected by isNumber(" + quote(s) + ")");
        }

        try {
            n = NumberUtils.createNumber(s);
        } catch (Throwable t) {
            return;
        }

        // Contract lifted from the test's paired assertions: for accepted numeric strings,
        // createNumber succeeds and returns a non-null Number rather than silently losing the value.
        if (n == null) {
            throw new FuzzerSecurityIssueLow("[oracle:plain-decimal-creatable] semantic mismatch: createNumber(" + quote(s) + ") returned null for an accepted number string");
        }
    }

    private static void checkLongSuffixCaseAgreement(FuzzedDataProvider data) {
        String base;
        boolean upper;
        boolean lower;
        try {
            int whole = data.consumeInt(0, 1000000);
            int frac = data.consumeInt(0, 1000000);
            boolean neg = data.consumeBoolean();
            base = (neg ? "-" : "") + whole + "." + frac;
        } catch (Throwable t) {
            return;
        }

        try {
            upper = NumberUtils.isNumber(base + "L");
        } catch (Throwable t) {
            return;
        }
        try {
            lower = NumberUtils.isNumber(base + "l");
        } catch (Throwable t) {
            return;
        }

        // The implementation tests 'l' and 'L' in the same branch, so equivalent inputs differing
        // only by suffix case must agree; a guard that merely hides one path would break this.
        if (upper != lower) {
            throw new FuzzerSecurityIssueLow("[oracle:long-suffix-case-agreement] metamorphic violation: isNumber should agree for equivalent suffix case input=" + quote(base) + " lhs=" + upper + " rhs=" + lower);
        }
    }

    private static boolean checkCreateNumber(String val) {
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

    private static String quote(String s) {
        return s == null ? "null" : "\"" + s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t").replace("\"", "\\\"") + "\"";
    }
}
```

</details>

---
## [63] ⚙️ harness-attempt · `attempt_014`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-isNumber-negative] semantic mismatch: index=24 NumberUtils.isNumber("1.1L") expected=false actual=true

---
## [64] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [40] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,683 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    private static boolean checkCreateNumber(String val) {
        try {
            Object obj = NumberUtils.createNumber(val);
            return obj != null;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static void assertBooleanEquals(String oracleId, String what, boolean expected, boolean actual, String input) {
        if (expected != actual) {
            throw new FuzzerSecurityIssueLow("[oracle:" + oracleId + "] semantic mismatch: " + what
                    + " input=" + String.valueOf(input)
                    + " expected=" + expected
                    + " actual=" + actual);
        }
    }

    private static void checkPair(String suffix, String val, boolean expectedIsNumber, boolean expectedCreateNumber) {
        boolean actualIsNumber = NumberUtils.isNumber(val);
        assertBooleanEquals("lifted-isNumber-" + suffix, "NumberUtils.isNumber", expectedIsNumber, actualIsNumber, val);

        boolean actualCreateNumber = checkCreateNumber(val);
        assertBooleanEquals("lifted-checkCreateNumber-" + suffix, "checkCreateNumber", expectedCreateNumber, actualCreateNumber, val);
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkPair("1", "12345", true, true);
        checkPair("2", "1234.5", true, true);
        checkPair("3", ".12345", true, true);
        checkPair("4", "1234E5", true, true);
        checkPair("5", "1234E+5", true, true);
        checkPair("6", "1234E-5", true, true);
        checkPair("7", "123.4E5", true, true);
        checkPair("8", "-1234", true, true);
        checkPair("9", "-1234.5", true, true);
        checkPair("10", "-.12345", true, true);
        checkPair("11", "-1234E5", true, true);
        checkPair("12", "0", true, true);
        checkPair("13", "-0", true, true);
        checkPair("14", "01234", true, true);
        checkPair("15", "-01234", true, true);
        checkPair("16", "0xABC123", true, true);
        checkPair("17", "0x0", true, true);
        checkPair("19", "123.4E21D", true, true);
        checkPair("20", "-221.23F", true, true);
        checkPair("21", "22338L", true, true);

        checkPair("1-Neg", null, false, false);
        checkPair("2-Neg", "", false, false);
        checkPair("3-Neg", "--2.3", false, false);
        checkPair("4-Neg", ".12.3", false, false);
        checkPair("5-Neg", "-123E", false, false);
        checkPair("6-Neg", "-123E+-212", false, false);
        checkPair("7-Neg", "-123E2.12", false, false);
        checkPair("8-Neg", "0xGF", false, false);
        checkPair("9-Neg", "0xFAE-1", false, false);
        checkPair("10-Neg", ".", false, false);
        checkPair("11-Neg", "-0ABC123", false, false);
        checkPair("12-Neg", "123.4E-D", false, false);
        checkPair("13-Neg", "123.4ED", false, false);
        checkPair("14-Neg", "1234E5l", false, false);
        checkPair("15-Neg", "11a", false, false);
        checkPair("16-Neg", "1a", false, false);
        checkPair("17-Neg", "a", false, false);
        checkPair("18-Neg", "11g", false, false);
        checkPair("19-Neg", "11z", false, false);
        checkPair("20-Neg", "11def", false, false);
        checkPair("21-Neg", "11d11", false, false);
        checkPair("22-Neg", "11 11", false, false);
        checkPair("23-Neg", " 1111", false, false);
        checkPair("24-Neg", "1111 ", false, false);

        assertBooleanEquals("lifted-isNumber-LANG-521", "NumberUtils.isNumber", true, NumberUtils.isNumber("2."), "2.");
        assertBooleanEquals("lifted-isNumber-LANG-664", "NumberUtils.isNumber", false, NumberUtils.isNumber("1.1L"), "1.1L");

        String fuzz = data.consumeString(64);
        NumberUtils.isNumber(fuzz);
        checkCreateNumber(fuzz);

        int n = data.consumeInt(-1000000, 1000000);
        String canonicalInt = Integer.toString(n);
        boolean intIsNumber = NumberUtils.isNumber(canonicalInt);
        boolean intCreateNumber = checkCreateNumber(canonicalInt);
        if (!intIsNumber) {
            throw new RuntimeException("[oracle:canonical-int] metamorphic violation: canonical decimal integer must be recognized input="
                    + canonicalInt + " lhs=" + intIsNumber + " rhs=true");
        }
        if (!intCreateNumber) {
            throw new RuntimeException("[oracle:canonical-int-create] metamorphic violation: createNumber must accept canonical decimal integer input="
                    + canonicalInt + " lhs=" + intCreateNumber + " rhs=true");
        }

        int m = data.consumeInt(0, 1000000);
        String digits = Integer.toString(m);
        String longQualified = digits + "L";
        String decimalLongQualified = digits + ".0L";

        NumberUtils.isNumber(longQualified);
        NumberUtils.isNumber(decimalLongQualified);
        checkCreateNumber(longQualified);
        checkCreateNumber(decimalLongQualified);

        /* Contract from NumberUtils.isNumber code shown above:
           "not allowing L with an exponent or decimal point".
           Therefore for any non-negative canonical digit string, adding 'L' preserves numeric status,
           while adding ".0L" must make it non-numeric. A patch that merely bypasses the buggy branch
           or deletes the decimal-point guard would violate this observable post-condition without throwing. */
        boolean longOk = NumberUtils.isNumber(longQualified);
        if (!longOk) {
            throw new RuntimeException("[oracle:long-suffix-accepted] metamorphic violation: integer with L suffix should be numeric input="
                    + longQualified + " lhs=" + longOk + " rhs=true");
        }
        boolean decimalLongRejected = NumberUtils.isNumber(decimalLongQualified);
        if (decimalLongRejected) {
            throw new RuntimeException("[oracle:decimal-long-rejected] metamorphic violation: decimal with L suffix must be rejected input="
                    + decimalLongQualified + " lhs=" + decimalLongRejected + " rhs=false");
        }

        String exponentLongQualified = digits + "E1L";
        boolean exponentLongRejected = NumberUtils.isNumber(exponentLongQualified);
        if (exponentLongRejected) {
            throw new RuntimeException("[oracle:exponent-long-rejected] metamorphic violation: exponent form with L suffix must be rejected input="
                    + exponentLongQualified + " lhs=" + exponentLongRejected + " rhs=false");
        }
    }
}
```

</details>

---
## [65] ⚙️ harness-attempt · `attempt_015`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-isNumber-LANG-664] semantic mismatch: NumberUtils.isNumber input=1.1L expected=false actual=true

---
## [66] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [36] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,038 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedOracles();

        String arbitrary = data.consumeString(64);
        try {
            NumberUtils.isNumber(arbitrary);
            checkCreateNumber(arbitrary);
        } catch (Throwable t) {
            return;
        }

        String base;
        try {
            int whole = data.consumeInt(0, 1000000);
            int frac = data.consumeInt(0, 1000000);
            boolean neg = data.consumeBoolean();
            base = (neg ? "-" : "") + whole + "." + frac;
        } catch (Throwable t) {
            return;
        }

        boolean plain;
        boolean withLong;
        Number created;
        try {
            plain = NumberUtils.isNumber(base);
            withLong = NumberUtils.isNumber(base + "L");
            created = NumberUtils.createNumber(base);
        } catch (Throwable t) {
            return;
        }

        /* Contract/oracle from the trusted failing test and patched branch:
           a valid decimal form with a decimal point is accepted, but the same form
           with an 'L' suffix is invalid. A patch that merely skips the patched branch
           or returns the wrong value silently violates this observable result. */
        if (!plain) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:decimal-with-long-suffix] semantic mismatch: isNumber(\"" + escape(base) + "\") expected=true actual=false");
        }
        if (withLong) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:decimal-with-long-suffix] semantic mismatch: isNumber(\"" + escape(base + "L") + "\") expected=false actual=true");
        }

        /* Trusted invariant lifted from the test's repeated pairing of isNumber(val)
           with checkCreateNumber(val): for accepted plain decimals, createNumber must
           succeed and return a non-null Number. */
        if (created == null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:accepted-decimal-creatable] semantic mismatch: createNumber(\"" + escape(base) + "\") expected non-null actual=null");
        }

        /* Hidden-state/read-only check: isNumber is a pure question over a String and
           must not depend on prior calls or mutate hidden parser state. Repeating the
           same call must yield the same boolean. A patch that corrupts bookkeeping can
           violate this without throwing. */
        boolean again;
        try {
            again = NumberUtils.isNumber(base + "L");
        } catch (Throwable t) {
            return;
        }
        if (again != withLong) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:repeatability] semantic mismatch: repeated isNumber(\"" + escape(base + "L") + "\") changed result first=" + withLong + " second=" + again);
        }
    }

    private static void checkLiftedOracles() {
        String[] values = new String[] {
            "12345",
            "1234.5",
            ".12345",
            "1234E5",
            "1234E+5",
            "1234E-5",
            "123.4E5",
            "-1234",
            "-1234.5",
            "-.12345",
            "-1234E5",
            "0",
            "-0",
            "01234",
            "-01234",
            "0xABC123",
            "0x0",
            "123.4E21D",
            "-221.23F",
            "22338L",
            null,
            "",
            "--2.3",
            ".12.3",
            "-123E",
            "-123E+-212",
            "-123E2.12",
            "0xGF",
            "0xFAE-1",
            ".",
            "-0ABC123",
            "123.4E-D",
            "123.4ED",
            "1234E5l",
            "11a",
            "1a",
            "a",
            "11g",
            "11z",
            "11def",
            "11d11",
            "11 11",
            " 1111",
            "1111 ",
            "2.",
            "1.1L"
        };

        boolean[] expectedIsNumber = new boolean[] {
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
```

</details>

---
## [67] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [68] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~6,276 chars, 2 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [36] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedOracles();

        String arbitrary = data.consumeString(64);
        try {
            NumberUtils.isNumber(arbitrary);
            checkCreateNumber(arbitrary);
        } catch (Throwable t) {
            return;
        }

        String base;
        try {
            int whole = data.consumeInt(0, 1000000);
            int frac = data.consumeInt(0, 1000000);
            boolean neg = data.consumeBoolean();
            base = (neg ? "-" : "") + whole + "." + frac;
        } catch (Throwable t) {
            return;
        }

        boolean plain;
        boolean withLong;
        Number created;
        try {
            plain = NumberUtils.isNumber(base);
            withLong = NumberUtils.isNumber(base + "L");
            created = NumberUtils.createNumber(base);
        } catch (Throwable t) {
            return;
        }

        /* Contract/oracle from the trusted failing test and patched branch:
           a valid decimal form with a decimal point is accepted, but the same form
           with an 'L' suffix is invalid. A patch that merely skips the patched branch
           or returns the wrong value silently violates this observable result. */
        if (!plain) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:decimal-with-long-suffix] semantic mismatch: isNumber(\"" + escape(base) + "\") expected=true actual=false");
        }
        if (withLong) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:decimal-with-long-suffix] semantic mismatch: isNumber(\"" + escape(base + "L") + "\") expected=false actual=true");
        }

        /* Trusted invariant lifted from the test's repeated pairing of isNumber(val)
           with checkCreateNumber(val): for accepted plain decimals, createNumber must
           succeed and return a non-null Number. */
        if (created == null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:accepted-decimal-creatable] semantic mismatch: createNumber(\"" + escape(base) + "\") expected non-null actual=null");
        }

        /* Hidden-state/read-only check: isNumber is a pure question over a String and
           must not depend on prior calls or mutate hidden parser state. Repeating the
           same call must yield the same boolean. A patch that corrupts bookkeeping can
           violate this without throwing. */
        boolean again;
        try {
            again = NumberUtils.isNumber(base + "L");
        } catch (Throwable t) {
            return;
        }
        if (again != withLong) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:repeatability] semantic mismatch: repeated isNumber(\"" + escape(base + "L") + "\") changed result first=" + withLong + " second=" + again);
        }
    }

    private static void checkLiftedOracles() {
        String[] values = new String[] {
            "12345",
            "1234.5",
            ".12345",
            "1234E5",
            "1234E+5",
            "1234E-5",
            "123.4E5",
            "-1234",
            "-1234.5",
            "-.12345",
            "-1234E5",
            "0",
            "-0",
            "01234",
            "-01234",
            "0xABC123",
            "0x0",
            "123.4E21D",
            "-221.23F",
            "22338L",
            null,
            "",
            "--2.3",
            ".12.3",
            "-123E",
            "-123E+-212",
            "-123E2.12",
            "0xGF",
            "0xFAE-1",
            ".",
            "-0ABC123",
            "123.4E-D",
            "123.4ED",
            "1234E5l",
            "11a",
            "1a",
            "a",
            "11g",
            "11z",
            "11def",
            "11d11",
            "11 11",
            " 1111",
            "1111 ",
            "2.",
            "1.1L"
        };

        boolean[] expectedIsNumber = new boolean[] {
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
   
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/Lang_24_buggy/fuzz/attempt_016/FuzzHarness.java:165: error: reached end of file while parsing
            false,
                  ^
1 error


Return the full corrected FuzzHarness.java. Rules:
- Raw Java source only. No markdown fences. No prose. No explanations. The file starts with a comment or package statement and ends with a closing brace.
- Public class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Use only the FuzzedDataProvider methods listed in the original instructions. Do NOT invent methods like getInt(), consumeDouble(), getRemainingSize(), or consumeIntInRange() — they do not exist.
- Do NOT use classes or methods that are not on the project classpath. If javac says 'cannot find symbol', remove that import and use only classes visible in the source_imports block or java.* / java.awt.*.
```

</details>
<details open><summary>▸ Output (~8,452 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedOracles();

        String arbitrary = data.consumeString(64);
        try {
            NumberUtils.isNumber(arbitrary);
            checkCreateNumber(arbitrary);
        } catch (Throwable t) {
            return;
        }

        String base;
        try {
            int whole = data.consumeInt(0, 1000000);
            int frac = data.consumeInt(0, 1000000);
            boolean neg = data.consumeBoolean();
            base = (neg ? "-" : "") + whole + "." + frac;
        } catch (Throwable t) {
            return;
        }

        boolean plain;
        boolean withLong;
        Number created;
        try {
            plain = NumberUtils.isNumber(base);
            withLong = NumberUtils.isNumber(base + "L");
            created = NumberUtils.createNumber(base);
        } catch (Throwable t) {
            return;
        }

        /* Contract/oracle from the trusted failing test and patched branch:
           a valid decimal form with a decimal point is accepted, but the same form
           with an 'L' suffix is invalid. */
        if (!plain) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:decimal-with-long-suffix] semantic mismatch: isNumber(\"" + escape(base) + "\") expected=true actual=false");
        }
        if (withLong) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:decimal-with-long-suffix] semantic mismatch: isNumber(\"" + escape(base + "L") + "\") expected=false actual=true");
        }

        /* Trusted invariant lifted from the test's repeated pairing of isNumber(val)
           with checkCreateNumber(val): for accepted plain decimals, createNumber must
           succeed and return a non-null Number. */
        if (created == null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:accepted-decimal-creatable] semantic mismatch: createNumber(\"" + escape(base) + "\") expected non-null actual=null");
        }

        /* Read-only/repeatability post-condition: isNumber(String) is a pure question
           over its input String, so repeating the same call must yield the same result. */
        boolean again;
        try {
            again = NumberUtils.isNumber(base + "L");
        } catch (Throwable t) {
            return;
        }
        if (again != withLong) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:repeatability] semantic mismatch: repeated isNumber(\"" + escape(base + "L") + "\") changed result first=" + withLong + " second=" + again);
        }
    }

    private static void checkLiftedOracles() {
        String[] values = new String[] {
            "12345",
            "1234.5",
            ".12345",
            "1234E5",
            "1234E+5",
            "1234E-5",
            "123.4E5",
            "-1234",
            "-1234.5",
            "-.12345",
            "-1234E5",
            "0",
            "-0",
            "01234",
            "-01234",
            "0xABC123",
            "0x0",
            "123.4E21D",
            "-221.23F",
            "22338L",
            null,
            "",
            "--2.3",
            ".12.3",
            "-123E",
            "-123E+-212",
            "-123E2.12",
            "0xGF",
            "0xFAE-1",
            ".",
            "-0ABC123",
            "123.4E-D",
            "123.4ED",
            "1234E5l",
            "11a",
            "1a",
            "a",
            "11g",
            "11z",
            "11def",
            "11d11",
            "11 11",
            " 1111",
            "1111 ",
            "2.",
            "1.1L"
        };

        boolean[] expectedIsNumber = new boolean[] {
            true,  /* 12345 */
            true,  /* 1234.5 */
            true,  /* .12345 */
            true,  /* 1234E5 */
            true,  /* 1234E+5 */
            true,  /* 1234E-5 */
            true,  /* 123.4E5 */
            true,  /* -1234 */
            true,  /* -1234.5 */
            true,  /* -.12345 */
            true,  /* -1234E5 */
            true,  /* 0 */
            true,  /* -0 */
            true,  /* 01234 */
            true,  /* -01234 */
            true,  /* 0xABC123 */
            true,  /* 0x0 */
            true,  /* 123.4E21D */
            true,  /* -221.23F */
            true,  /* 22338L */
            false, /* null */
            false, /* "" */
            false, /* --2.3 */
            false, /* .12.3 */
            false, /* -123E */
            false, /* -123E+-212 */
            false, /* -123E2.12 */
            false, /* 0xGF */
            false, /* 0xFAE-1 */
            false, /* . */
            false, /* -0ABC123 */
            false, /* 123.4E-D */
            false, /* 123.4ED */
            false, /* 1234E5l */
            false, /* 11a */
            false, /* 1a */
            false, /* a */
            false, /* 11g */
            false, /* 11z */
            false, /* 11def */
            false, /* 11d11 */
            false, /* 11 11 */
            false, /*  1111 */
            false, /* 1111  */
            true,  /* 2. */
            false  /* 1.1L */
        };

        boolean[] expectedCheckCreateNumber = new boolean[] {
            true,  /* 12345 */
            true,  /* 1234.5 */
            true,  /* .12345 */
            true,  /* 1234E5 */
            true,  /* 1234E+5 */
            true,  /* 1234E-5 */
            true,  /* 123.4E5 */
            true,  /* -1234 */
            true,  /* -1234.5 */
            true,  /* -.12345 */
            true,  /* -1234E5 */
            true,  /* 0 */
            true,  /* -0 */
            true,  /* 01234 */
            true,  /* -01234 */
            true,  /* 0xABC123 */
            true,  /* 0x0 */
            true,  /* 123.4E21D */
            true,  /* -221.23F */
            true,  /* 22338L */
            false, /* null */
            false, /* "" */
            false, /* --2.3 */
            false, /* .12.3 */
            false, /* -123E */
            false, /* -123E+-212 */
            false, /* -123E2.12 */
            false, /* 0xGF */
            false, /* 0xFAE-1 */
            false, /* . */
            false, /* -0ABC123 */
            false, /* 123.4E-D */
            false, /* 123.4ED */
            false, /* 1234E5l */
            false, /* 11a */
            false, /* 1a */
            false, /* a */
            false, /* 11g */
            false, /* 11z */
            false, /* 11def */
            false, /* 11d11 */
            false, /* 11 11 */
            false, /*  1111 */
            false, /* 1111  */
            true,  /* 2. */
            false  /* 1.1L */
        };

        for (int i = 0; i < values.length; i++) {
            String val = values[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (actualIsNumber != expectedIsNumber[i]) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-isNumber-" + i + "] semantic mismatch: isNumber(\"" + escape(val) + "\") expected=" + expectedIsNumber[i] + " actual=" + actualIsNumber);
            }

            boolean actualCheckCreate = checkCreateNumber(val);
            if (actualCheckCreate != expectedCheckCreateNumber[i]) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-checkCreateNumber-" + i + "] semantic mismatch: checkCreateNumber(\"" + escape(val) + "\") expected=" + expectedCheckCreateNumber[i] + " actual=" + actualCheckCreate);
            }
        }
    }

    private static boolean checkCreateNumber(String val) {
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

    private static String escape(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}
```

</details>

---
## [69] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Every oracle alarm in your harness needs a name: the alarm near line 219 has no oracle ID (`throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow( "[oracle:lifted-isNumber-" + i + "] seman

---
## [70] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~9,315 chars, 2 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [36] — not reprinted*
- *[assistant] message: identical to step [68] — not reprinted*
- *[user] message: identical to step [68] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedOracles();

        String arbitrary = data.consumeString(64);
        try {
            NumberUtils.isNumber(arbitrary);
            checkCreateNumber(arbitrary);
        } catch (Throwable t) {
            return;
        }

        String base;
        try {
            int whole = data.consumeInt(0, 1000000);
            int frac = data.consumeInt(0, 1000000);
            boolean neg = data.consumeBoolean();
            base = (neg ? "-" : "") + whole + "." + frac;
        } catch (Throwable t) {
            return;
        }

        boolean plain;
        boolean withLong;
        Number created;
        try {
            plain = NumberUtils.isNumber(base);
            withLong = NumberUtils.isNumber(base + "L");
            created = NumberUtils.createNumber(base);
        } catch (Throwable t) {
            return;
        }

        /* Contract/oracle from the trusted failing test and patched branch:
           a valid decimal form with a decimal point is accepted, but the same form
           with an 'L' suffix is invalid. */
        if (!plain) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:decimal-with-long-suffix] semantic mismatch: isNumber(\"" + escape(base) + "\") expected=true actual=false");
        }
        if (withLong) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:decimal-with-long-suffix] semantic mismatch: isNumber(\"" + escape(base + "L") + "\") expected=false actual=true");
        }

        /* Trusted invariant lifted from the test's repeated pairing of isNumber(val)
           with checkCreateNumber(val): for accepted plain decimals, createNumber must
           succeed and return a non-null Number. */
        if (created == null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:accepted-decimal-creatable] semantic mismatch: createNumber(\"" + escape(base) + "\") expected non-null actual=null");
        }

        /* Read-only/repeatability post-condition: isNumber(String) is a pure question
           over its input String, so repeating the same call must yield the same result. */
        boolean again;
        try {
            again = NumberUtils.isNumber(base + "L");
        } catch (Throwable t) {
            return;
        }
        if (again != withLong) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:repeatability] semantic mismatch: repeated isNumber(\"" + escape(base + "L") + "\") changed result first=" + withLong + " second=" + again);
        }
    }

    private static void checkLiftedOracles() {
        String[] values = new String[] {
            "12345",
            "1234.5",
            ".12345",
            "1234E5",
            "1234E+5",
            "1234E-5",
            "123.4E5",
            "-1234",
            "-1234.5",
            "-.12345",
            "-1234E5",
            "0",
            "-0",
            "01234",
            "-01234",
            "0xABC123",
            "0x0",
            "123.4E21D",
            "-221.23F",
            "22338L",
            null,
            "",
            "--2.3",
            ".12.3",
            "-123E",
            "-123E+-212",
            "-123E2.12",
            "0xGF",
            "0xFAE-1",
            ".",
            "-0ABC123",
            "123.4E-D",
            "123.4ED",
            "1234E5l",
            "11a",
            "1a",
            "a",
            "11g",
            "11z",
            "11def",
            "11d11",
            "11 11",
            " 1111",
            "1111 ",
            "2.",
            "1.1L"
        };

        boolean[] expectedIsNumber = new boolean[] {
            true,  /* 12345 */
            true,  /* 1234.5 */
            true,  /* .12345 */
            true,  /* 1234E5 */
            true,  /* 1234E+5 */
            true,  /* 1234E-5 */
            true,  /* 123.4E5 */
            true,  /* -1234 */
            true,  /* -1234.5 */
            true,  /* -.12345 */
            true,  /* -1234E5 */
            true,  /* 0 */
            true,  /* -0 */
            true,  /* 01234 */
            true,  /* -01234 */
            true,  /* 0xABC123 */
            true,  /* 0x0 */
            true,  /* 123.4E21D */
            true,  /* -221.23F */
            true,  /* 22338L */
            false, /* null */
            false, /* "" */
            false, /* --2.3 */
            false, /* .12.3 */
            false, /* -123E */
            false, /* -123E+-212 */
            false, /* -123E2.12 */
            false, /* 0xGF */
            false, /* 0xFAE-1 */
            false, /* . */
            false, /* -0ABC123 */
            false, /* 123.4E-D */
            false, /* 123.4ED */
            false, /* 1234E5l */
            false, /* 11a */
            false, /* 1a */
            false, /* a */
            false, /* 11g */
            false, /* 11z */
            false, /* 11def */
            false, /* 11d11 */
            false, /* 11 11 */
            false, /*  1111 */
            false, /* 1111  */
            true,  /* 2. */
            false  /* 1.1L */
        };

        boolean[] expectedCheckCreateNumber = new boolean[] {
            true,  /* 12345 */
            true,  /* 1234.5 */
            true,  /* .12345 */
            true,  /* 1234E5 */
            true,  /* 1234E+5 */
            true,  /* 1234E-5 */
            true,  /* 123.4E5 */
            true,  /* -1234 */
            true,  /* -1234.5 */
            true,  /* -.12345 */
            true,  /* -1234E5 */
            true,  /* 0 */
            true,  /* -0 */
            true,  /* 01234 */
            true,  /* -01234 */
            true,  /* 0xABC123 */
            true,  /* 0x0 */
            true,  /* 123.4E21D */
            true,  /* -221.23F */
            true,  /* 22338L */
            false, /* null */
            false, /* "" */
            false, /* --2.3 */
            false, /* .12.3 */
            false, /* -123E */
            false, /* -123E+-212 */
            false, /* -123E2.12 */
            false, /* 0xGF */
            false, /* 0xFAE-1 */
            false, /* . */
            false, /* -0ABC123 */
            false, /* 123.4E-D */
            false, /* 123.4ED */
            false, /* 1234E5l */
            false, /* 11a */
            false, /* 1a */
            false, /* a */
            false, /* 11g */
            false, /* 11z */
            false, /* 11def */
            false, /* 11d11 */
            false, /* 11 11 */
            false, /*  1111 */
            false, /* 1111  */
            true,  /* 2. */
            false  /* 1.1L */
        };

        for (int i = 0; i < values.length; i++) {
            String val = values[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (actualIsNumber != expectedIsNumber[i]) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-isNumber-" + i + "] semantic mismatch: isNumber(\"" + escape(val) + "\") expected=" + expectedIsNumber[i] + " actual=" + actualIsNumber);
            }

            boolean actualCheckCreate = checkCreateNumber(val);
            if (actualCheckCreate != expectedCheckCreateNumber[i]) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-checkCreateNumber-" + i + "] semantic mismatch: checkCreateNumber(\"" + escape(val) + "\") expected=" + expectedCheckCreateNumber[i] + " actual=" + actualCheckCreate);
            }
        }
    }

    private static boolean checkCreateNumber(String val) {
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

    private static String escape(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}
```
**[user]**
```
Every oracle alarm in your harness needs a name: the alarm near line 219 has no oracle ID (`throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow( "[oracle:lifted-isNumber-" + i + "] semantic mismatch...`) — start its message with "[oracle:<short-id>]" so acceptance can tell which check earned its place
Start each distinct check's message with [oracle:<short-id>], e.g. `throw new FuzzerSecurityIssueLow("[oracle:round-trip] semantic mismatch: ...")`. Use a DIFFERENT id per check. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~4,519 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedOracles();

        String arbitrary = data.consumeString(64);
        try {
            NumberUtils.isNumber(arbitrary);
            checkCreateNumber(arbitrary);
        } catch (Throwable t) {
            return;
        }

        String base;
        try {
            int whole = data.consumeInt(0, 1000000);
            int frac = data.consumeInt(0, 1000000);
            boolean neg = data.consumeBoolean();
            base = (neg ? "-" : "") + whole + "." + frac;
        } catch (Throwable t) {
            return;
        }

        boolean plain;
        boolean withLong;
        Number created;
        try {
            plain = NumberUtils.isNumber(base);
            withLong = NumberUtils.isNumber(base + "L");
            created = NumberUtils.createNumber(base);
        } catch (Throwable t) {
            return;
        }

        /* Contract/oracle from the trusted failing test and patched branch:
           a valid decimal form with a decimal point is accepted, but the same form
           with an 'L' suffix is invalid. */
        if (!plain) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:decimal-with-long-suffix] semantic mismatch: isNumber(\"" + escape(base) + "\") expected=true actual=false");
        }
        if (withLong) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:decimal-with-long-suffix] semantic mismatch: isNumber(\"" + escape(base + "L") + "\") expected=false actual=true");
        }

        /* Trusted invariant lifted from the test's repeated pairing of isNumber(val)
           with checkCreateNumber(val): for accepted plain decimals, createNumber must
           succeed and return a non-null Number. */
        if (created == null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:accepted-decimal-creatable] semantic mismatch: createNumber(\"" + escape(base) + "\") expected non-null actual=null");
        }

        /* Read-only/repeatability post-condition: isNumber(String) is a pure question
           over its input String, so repeating the same call must yield the same result. */
        boolean again;
        try {
            again = NumberUtils.isNumber(base + "L");
        } catch (Throwable t) {
            return;
        }
        if (again != withLong) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:repeatability] semantic mismatch: repeated isNumber(\"" + escape(base + "L") + "\") changed result first=" + withLong + " second=" + again);
        }
    }

    private static void checkLiftedOracles() {
        String[] values = new String[] {
            "12345",
            "1234.5",
            ".12345",
            "1234E5",
            "1234E+5",
            "1234E-5",
            "123.4E5",
            "-1234",
            "-1234.5",
            "-.12345",
            "-1234E5",
            "0",
            "-0",
            "01234",
            "-01234",
            "0xABC123",
            "0x0",
            "123.4E21D",
            "-221.23F",
            "22338L",
            null,
            "",
            "--2.3",
            ".12.3",
            "-123E",
            "-123E+-212",
            "-123E2.12",
            "0xGF",
            "0xFAE-1",
            ".",
            "-0ABC123",
            "123.4E-D",
            "123.4ED",
            "1234E5l",
            "11a",
            "1a",
            "a",
            "11g",
            "11z",
            "11def",
            "11d11",
            "11 11",
            " 1111",
            "1111 ",
            "2.",
            "1.1L"
        };

        boolean[] expectedIsNumber = new boolean[] {
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            false,
            false,
            false,
            false,
            false,
            false,
```

</details>

---
## [71] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [72] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (8 message(s), ~5,999 chars, 2 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [36] — not reprinted*
- *[assistant] message: identical to step [68] — not reprinted*
- *[user] message: identical to step [68] — not reprinted*
- *[assistant] message: identical to step [70] — not reprinted*
- *[user] message: identical to step [70] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedOracles();

        String arbitrary = data.consumeString(64);
        try {
            NumberUtils.isNumber(arbitrary);
            checkCreateNumber(arbitrary);
        } catch (Throwable t) {
            return;
        }

        String base;
        try {
            int whole = data.consumeInt(0, 1000000);
            int frac = data.consumeInt(0, 1000000);
            boolean neg = data.consumeBoolean();
            base = (neg ? "-" : "") + whole + "." + frac;
        } catch (Throwable t) {
            return;
        }

        boolean plain;
        boolean withLong;
        Number created;
        try {
            plain = NumberUtils.isNumber(base);
            withLong = NumberUtils.isNumber(base + "L");
            created = NumberUtils.createNumber(base);
        } catch (Throwable t) {
            return;
        }

        /* Contract/oracle from the trusted failing test and patched branch:
           a valid decimal form with a decimal point is accepted, but the same form
           with an 'L' suffix is invalid. */
        if (!plain) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:decimal-with-long-suffix] semantic mismatch: isNumber(\"" + escape(base) + "\") expected=true actual=false");
        }
        if (withLong) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:decimal-with-long-suffix] semantic mismatch: isNumber(\"" + escape(base + "L") + "\") expected=false actual=true");
        }

        /* Trusted invariant lifted from the test's repeated pairing of isNumber(val)
           with checkCreateNumber(val): for accepted plain decimals, createNumber must
           succeed and return a non-null Number. */
        if (created == null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:accepted-decimal-creatable] semantic mismatch: createNumber(\"" + escape(base) + "\") expected non-null actual=null");
        }

        /* Read-only/repeatability post-condition: isNumber(String) is a pure question
           over its input String, so repeating the same call must yield the same result. */
        boolean again;
        try {
            again = NumberUtils.isNumber(base + "L");
        } catch (Throwable t) {
            return;
        }
        if (again != withLong) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:repeatability] semantic mismatch: repeated isNumber(\"" + escape(base + "L") + "\") changed result first=" + withLong + " second=" + again);
        }
    }

    private static void checkLiftedOracles() {
        String[] values = new String[] {
            "12345",
            "1234.5",
            ".12345",
            "1234E5",
            "1234E+5",
            "1234E-5",
            "123.4E5",
            "-1234",
            "-1234.5",
            "-.12345",
            "-1234E5",
            "0",
            "-0",
            "01234",
            "-01234",
            "0xABC123",
            "0x0",
            "123.4E21D",
            "-221.23F",
            "22338L",
            null,
            "",
            "--2.3",
            ".12.3",
            "-123E",
            "-123E+-212",
            "-123E2.12",
            "0xGF",
            "0xFAE-1",
            ".",
            "-0ABC123",
            "123.4E-D",
            "123.4ED",
            "1234E5l",
            "11a",
            "1a",
            "a",
            "11g",
            "11z",
            "11def",
            "11d11",
            "11 11",
            " 1111",
            "1111 ",
            "2.",
            "1.1L"
        };

        boolean[] expectedIsNumber = new boolean[] {
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            false,
            false,
            false,
            false,
            false,
            false,
   
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/Lang_24_buggy/fuzz/attempt_018/FuzzHarness.java:148: error: reached end of file while parsing
            false,
                  ^
1 error


Return the full corrected FuzzHarness.java. Rules:
- Raw Java source only. No markdown fences. No prose. No explanations. The file starts with a comment or package statement and ends with a closing brace.
- Public class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Use only the FuzzedDataProvider methods listed in the original instructions. Do NOT invent methods like getInt(), consumeDouble(), getRemainingSize(), or consumeIntInRange() — they do not exist.
- Do NOT use classes or methods that are not on the project classpath. If javac says 'cannot find symbol', remove that import and use only classes visible in the source_imports block or java.* / java.awt.*.
```

</details>
<details open><summary>▸ Output (~8,708 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedOracles();

        String arbitrary = data.consumeString(64);
        try {
            NumberUtils.isNumber(arbitrary);
            checkCreateNumber(arbitrary);
        } catch (Throwable t) {
            return;
        }

        String base;
        try {
            int whole = data.consumeInt(0, 1000000);
            int frac = data.consumeInt(0, 1000000);
            boolean neg = data.consumeBoolean();
            base = (neg ? "-" : "") + whole + "." + frac;
        } catch (Throwable t) {
            return;
        }

        boolean plain;
        boolean withLong;
        Number created;
        try {
            plain = NumberUtils.isNumber(base);
            withLong = NumberUtils.isNumber(base + "L");
            created = NumberUtils.createNumber(base);
        } catch (Throwable t) {
            return;
        }

        /* Trusted by the failing test LANG-664: a valid decimal with a decimal point
           becomes invalid when suffixed with L/l. */
        if (!plain) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:decimal-plain-accepted] semantic mismatch: isNumber(\"" + escape(base) + "\") expected=true actual=false");
        }
        if (withLong) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:decimal-long-suffix-rejected] semantic mismatch: isNumber(\"" + escape(base + "L") + "\") expected=false actual=true");
        }

        /* Trusted by the test's repeated pairing of isNumber(val) with checkCreateNumber(val):
           an accepted plain decimal must be creatable as a non-null Number by createNumber. */
        if (created == null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:accepted-decimal-creatable] semantic mismatch: createNumber(\"" + escape(base) + "\") expected non-null actual=null");
        }

        /* Post-condition for a read-only query: repeating isNumber on the same String must
           yield the same answer; a patch that corrupts internal bookkeeping would violate this. */
        boolean again;
        try {
            again = NumberUtils.isNumber(base + "L");
        } catch (Throwable t) {
            return;
        }
        if (again != withLong) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:isNumber-repeatable] semantic mismatch: repeated isNumber(\"" + escape(base + "L") + "\") changed result first=" + withLong + " second=" + again);
        }
    }

    private static void checkLiftedOracles() {
        checkPair("lifted-isNumber-1", "lifted-create-1", "12345", true, true);
        checkPair("lifted-isNumber-2", "lifted-create-2", "1234.5", true, true);
        checkPair("lifted-isNumber-3", "lifted-create-3", ".12345", true, true);
        checkPair("lifted-isNumber-4", "lifted-create-4", "1234E5", true, true);
        checkPair("lifted-isNumber-5", "lifted-create-5", "1234E+5", true, true);
        checkPair("lifted-isNumber-6", "lifted-create-6", "1234E-5", true, true);
        checkPair("lifted-isNumber-7", "lifted-create-7", "123.4E5", true, true);
        checkPair("lifted-isNumber-8", "lifted-create-8", "-1234", true, true);
        checkPair("lifted-isNumber-9", "lifted-create-9", "-1234.5", true, true);
        checkPair("lifted-isNumber-10", "lifted-create-10", "-.12345", true, true);
        checkPair("lifted-isNumber-11", "lifted-create-11", "-1234E5", true, true);
        checkPair("lifted-isNumber-12", "lifted-create-12", "0", true, true);
        checkPair("lifted-isNumber-13", "lifted-create-13", "-0", true, true);
        checkPair("lifted-isNumber-14", "lifted-create-14", "01234", true, true);
        checkPair("lifted-isNumber-15", "lifted-create-15", "-01234", true, true);
        checkPair("lifted-isNumber-16", "lifted-create-16", "0xABC123", true, true);
        checkPair("lifted-isNumber-17", "lifted-create-17", "0x0", true, true);
        checkPair("lifted-isNumber-19", "lifted-create-19", "123.4E21D", true, true);
        checkPair("lifted-isNumber-20", "lifted-create-20", "-221.23F", true, true);
        checkPair("lifted-isNumber-21", "lifted-create-21", "22338L", true, true);

        checkPair("lifted-isNumber-neg-1", "lifted-create-neg-1", null, false, false);
        checkPair("lifted-isNumber-neg-2", "lifted-create-neg-2", "", false, false);
        checkPair("lifted-isNumber-neg-3", "lifted-create-neg-3", "--2.3", false, false);
        checkPair("lifted-isNumber-neg-4", "lifted-create-neg-4", ".12.3", false, false);
        checkPair("lifted-isNumber-neg-5", "lifted-create-neg-5", "-123E", false, false);
        checkPair("lifted-isNumber-neg-6", "lifted-create-neg-6", "-123E+-212", false, false);
        checkPair("lifted-isNumber-neg-7", "lifted-create-neg-7", "-123E2.12", false, false);
        checkPair("lifted-isNumber-neg-8", "lifted-create-neg-8", "0xGF", false, false);
        checkPair("lifted-isNumber-neg-9", "lifted-create-neg-9", "0xFAE-1", false, false);
        checkPair("lifted-isNumber-neg-10", "lifted-create-neg-10", ".", false, false);
        checkPair("lifted-isNumber-neg-11", "lifted-create-neg-11", "-0ABC123", false, false);
        checkPair("lifted-isNumber-neg-12", "lifted-create-neg-12", "123.4E-D", false, false);
        checkPair("lifted-isNumber-neg-13", "lifted-create-neg-13", "123.4ED", false, false);
        checkPair("lifted-isNumber-neg-14", "lifted-create-neg-14", "1234E5l", false, false);
        checkPair("lifted-isNumber-neg-15", "lifted-create-neg-15", "11a", false, false);
        checkPair("lifted-isNumber-neg-16", "lifted-create-neg-16", "1a", false, false);
        checkPair("lifted-isNumber-neg-17", "lifted-create-neg-17", "a", false, false);
        checkPair("lifted-isNumber-neg-18", "lifted-create-neg-18", "11g", false, false);
        checkPair("lifted-isNumber-neg-19", "lifted-create-neg-19", "11z", false, false);
        checkPair("lifted-isNumber-neg-20", "lifted-create-neg-20", "11def", false, false);
        checkPair("lifted-isNumber-neg-21", "lifted-create-neg-21", "11d11", false, false);
        checkPair("lifted-isNumber-neg-22", "lifted-create-neg-22", "11 11", false, false);
        checkPair("lifted-isNumber-neg-23", "lifted-create-neg-23", " 1111", false, false);
        checkPair("lifted-isNumber-neg-24", "lifted-create-neg-24", "1111 ", false, false);

        checkIsNumberOnly("lifted-isNumber-lang-521", "2.", true);
        checkIsNumberOnly("lifted-isNumber-lang-664", "1.1L", false);
    }

    private static void checkPair(String isNumberId, String createId, String value, boolean expectedIsNumber, boolean expectedCreate) {
        boolean actualIsNumber = NumberUtils.isNumber(value);
        if (actualIsNumber != expectedIsNumber) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + isNumberId + "] semantic mismatch: isNumber(\"" + escape(value) + "\") expected=" + expectedIsNumber + " actual=" + actualIsNumber);
        }

        boolean actualCreate = checkCreateNumber(value);
        if (actualCreate != expectedCreate) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + createId + "] semantic mismatch: checkCreateNumber(\"" + escape(value) + "\") expected=" + expectedCreate + " actual=" + actualCreate);
        }
    }

    private static void checkIsNumberOnly(String isNumberId, String value, boolean expectedIsNumber) {
        boolean actualIsNumber = NumberUtils.isNumber(value);
        if (actualIsNumber != expectedIsNumber) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + isNumberId + "] semantic mismatch: isNumber(\"" + escape(value) + "\") expected=" + expectedIsNumber + " actual=" + actualIsNumber);
        }
    }

    private static boolean checkCreateNumber(String val) {
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

    private static String escape(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}
```

</details>

---
## [73] ⚙️ harness-attempt · `attempt_019`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-isNumber-lang-664] semantic mismatch: isNumber("1.1L") expected=false actual=true

---
## [74] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [40] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,771 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String[] positiveVals = new String[] {
            "12345",
            "1234.5",
            ".12345",
            "1234E5",
            "1234E+5",
            "1234E-5",
            "123.4E5",
            "-1234",
            "-1234.5",
            "-.12345",
            "-1234E5",
            "0",
            "-0",
            "01234",
            "-01234",
            "0xABC123",
            "0x0",
            "123.4E21D",
            "-221.23F",
            "22338L",
            "2."
        };

        String[] positiveIds = new String[] {
            "pos-1","pos-2","pos-3","pos-4","pos-5","pos-6","pos-7","pos-8","pos-9","pos-10",
            "pos-11","pos-12","pos-13","pos-14","pos-15","pos-16","pos-17","pos-19","pos-20","pos-21","lang-521"
        };

        for (int i = 0; i < positiveVals.length; i++) {
            String val = positiveVals[i];
            boolean actual = NumberUtils.isNumber(val);
            if (!actual) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + positiveIds[i] + "-isNumber] semantic mismatch: input=" + quote(val) + " expected=true actual=" + actual);
            }
            boolean actualCreate = checkCreateNumber(val);
            if (!actualCreate) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + positiveIds[i] + "-createNumber] semantic mismatch: input=" + quote(val) + " expected=true actual=" + actualCreate);
            }
        }

        String[] negativeVals = new String[] {
            null,
            "",
            "--2.3",
            ".12.3",
            "-123E",
            "-123E+-212",
            "-123E2.12",
            "0xGF",
            "0xFAE-1",
            ".",
            "-0ABC123",
            "123.4E-D",
            "123.4ED",
            "1234E5l",
            "11a",
            "1a",
            "a",
            "11g",
            "11z",
            "11def",
            "11d11",
            "11 11",
            " 1111",
            "1111 ",
            "1.1L"
        };

        String[] negativeIds = new String[] {
            "neg-1","neg-2","neg-3","neg-4","neg-5","neg-6","neg-7","neg-8","neg-9","neg-10",
            "neg-11","neg-12","neg-13","neg-14","neg-15","neg-16","neg-17","neg-18","neg-19","neg-20",
            "neg-21","neg-22","neg-23","neg-24","lang-664"
        };

        for (int i = 0; i < negativeVals.length; i++) {
            String val = negativeVals[i];
            boolean actual = NumberUtils.isNumber(val);
            if (actual) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + negativeIds[i] + "-isNumber] semantic mismatch: input=" + quote(val) + " expected=false actual=" + actual);
            }
            boolean actualCreate = checkCreateNumber(val);
            if (actualCreate) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + negativeIds[i] + "-createNumber] semantic mismatch: input=" + quote(val) + " expected=false actual=" + actualCreate);
            }
        }

        String fuzz = data.consumeString(32);
        NumberUtils.isNumber(fuzz);

        int n = data.consumeInt(-1_000_000, 1_000_000);
        String longUpper = Integer.toString(n) + "L";
        String longLower = Integer.toString(n) + "l";
        String decimalLong = Integer.toString(n) + ".0L";
        String decimalLongLower = Integer.toString(n) + ".0l";

        boolean upper = NumberUtils.isNumber(longUpper);
        if (!upper) {
            throw new RuntimeException(
                "[oracle:long-qualifier-valid] metamorphic violation: integer with long qualifier must be accepted input="
                    + quote(longUpper) + " actual=" + upper);
        }

        boolean lower = NumberUtils.isNumber(longLower);
        if (!lower) {
            throw new RuntimeException(
                "[oracle:long-qualifier-valid-lower] metamorphic violation: integer with lowercase long qualifier must be accepted input="
                    + quote(longLower) + " actual=" + lower);
        }

        boolean decimalUpper = NumberUtils.isNumber(decimalLong);
        if (decimalUpper) {
            throw new RuntimeException(
                "[oracle:long-qualifier-decimal-invalid] metamorphic violation: method comment says 'not allowing L with an exponent or decimal point', so any correct implementation must reject decimal long qualifiers input="
                    + quote(decimalLong) + " actual=" + decimalUpper);
        }

        boolean decimalLower = NumberUtils.isNumber(decimalLongLower);
        if (decimalLower) {
            throw new RuntimeException(
                "[oracle:long-qualifier-decimal-invalid-lower] metamorphic violation: method comment says 'not allowing L with an exponent or decimal point', so any correct implementation must reject decimal long qualifiers input="
                    + quote(decimalLongLower) + " actual=" + decimalLower);
        }

        boolean createLongUpper = checkCreateNumber(longUpper);
        if (!createLongUpper) {
            throw new RuntimeException(
                "[oracle:createNumber-long-valid] metamorphic violation: helper from the test treats successful NumberUtils.createNumber as true, and integer long-qualified inputs used by the test (e.g. \"22338L\") must succeed input="
                    + quote(longUpper) + " actual=" + createLongUpper);
        }

        boolean createDecimalLong = checkCreateNumber(decimalLong);
        if (createDecimalLong) {
            throw new RuntimeException(
                "[oracle:createNumber-decimal-long-invalid] metamorphic violation: createNumber must agree with the isNumber classification exercised by the test for invalid decimal long qualifiers input="
                    + quote(decimalLong) + " actual=" + createDecimalLong);
        }
    }

    private static boolean checkCreateNumber(String val) {
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

    private static String quote(String s) {
        if (s == null) {
            return "null";
        }
        return "\"" + s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t") + "\"";
    }
}
```

</details>

---
## [75] ⚙️ harness-attempt · `attempt_020`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lang-664-isNumber] semantic mismatch: input="1.1L" expected=false actual=true

---
## [76] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [36] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~10,470 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    private static boolean checkCreateNumber(String val) {
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

    private static void assertOracle(String id, boolean expected, boolean actual, String call) {
        if (expected != actual) {
            throw new FuzzerSecurityIssueLow("[oracle:" + id + "] semantic mismatch: " + call
                    + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void runLiftedOracles() {
        String[] positives = new String[] {
                "12345",
                "1234.5",
                ".12345",
                "1234E5",
                "1234E+5",
                "1234E-5",
                "123.4E5",
                "-1234",
                "-1234.5",
                "-.12345",
                "-1234E5",
                "0",
                "-0",
                "01234",
                "-01234",
                "0xABC123",
                "0x0",
                "123.4E21D",
                "-221.23F",
                "22338L",
                "2."
        };
        String[] positiveIds = new String[] {
                "isnum-pos-1",
                "isnum-pos-2",
                "isnum-pos-3",
                "isnum-pos-4",
                "isnum-pos-5",
                "isnum-pos-6",
                "isnum-pos-7",
                "isnum-pos-8",
                "isnum-pos-9",
                "isnum-pos-10",
                "isnum-pos-11",
                "isnum-pos-12",
                "isnum-pos-13",
                "isnum-pos-14",
                "isnum-pos-15",
                "isnum-pos-16",
                "isnum-pos-17",
                "isnum-pos-19",
                "isnum-pos-20",
                "isnum-pos-21",
                "isnum-lang-521"
        };
        String[] positiveCreateIds = new String[] {
                "create-pos-1",
                "create-pos-2",
                "create-pos-3",
                "create-pos-4",
                "create-pos-5",
                "create-pos-6",
                "create-pos-7",
                "create-pos-8",
                "create-pos-9",
                "create-pos-10",
                "create-pos-11",
                "create-pos-12",
                "create-pos-13",
                "create-pos-14",
                "create-pos-15",
                "create-pos-16",
                "create-pos-17",
                "create-pos-19",
                "create-pos-20",
                "create-pos-21"
        };

        for (int i = 0; i < positives.length - 1; i++) {
            String val = positives[i];
            assertOracle(positiveIds[i], true, NumberUtils.isNumber(val), "NumberUtils.isNumber(\"" + val + "\")");
            assertOracle(positiveCreateIds[i], true, checkCreateNumber(val), "checkCreateNumber(\"" + val + "\")");
        }
        assertOracle(positiveIds[positives.length - 1], true, NumberUtils.isNumber(positives[positives.length - 1]),
                "NumberUtils.isNumber(\"2.\")");

        String[] negatives = new String[] {
                null,
                "",
                "--2.3",
                ".12.3",
                "-123E",
                "-123E+-212",
                "-123E2.12",
                "0xGF",
                "0xFAE-1",
                ".",
                "-0ABC123",
                "123.4E-D",
                "123.4ED",
                "1234E5l",
                "11a",
                "1a",
                "a",
                "11g",
                "11z",
                "11def",
                "11d11",
                "11 11",
                " 1111",
                "1111 ",
                "1.1L"
        };
        String[] negativeIds = new String[] {
                "isnum-neg-1",
                "isnum-neg-2",
                "isnum-neg-3",
                "isnum-neg-4",
                "isnum-neg-5",
                "isnum-neg-6",
                "isnum-neg-7",
                "isnum-neg-8",
                "isnum-neg-9",
                "isnum-neg-10",
                "isnum-neg-11",
                "isnum-neg-12",
                "isnum-neg-13",
                "isnum-neg-14",
                "isnum-neg-15",
                "isnum-neg-16",
                "isnum-neg-17",
                "isnum-neg-18",
                "isnum-neg-19",
                "isnum-neg-20",
                "isnum-neg-21",
                "isnum-neg-22",
                "isnum-neg-23",
                "isnum-neg-24",
                "isnum-lang-664"
        };
        String[] negativeCreateIds = new String[] {
                "create-neg-1",
                "create-neg-2",
                "create-neg-3",
                "create-neg-4",
                "create-neg-5",
                "create-neg-6",
                "create-neg-7",
                "create-neg-8",
                "create-neg-9",
                "create-neg-10",
                "create-neg-11",
                "create-neg-12",
                "create-neg-13",
                "create-neg-14",
                "create-neg-15",
                "create-neg-16",
                "create-neg-17",
                "create-neg-18",
                "create-neg-19",
                "create-neg-20",
                "create-neg-21",
                "create-neg-22",
                "create-neg-23",
                "create-neg-24"
        };

        for (int i = 0; i < negatives.length - 1; i++) {
            String val = negatives[i];
            String rendered = val == null ? "null" : "\"" + val + "\"";
            assertOracle(negativeIds[i], false, NumberUtils.isNumber(val), "NumberUtils.isNumber(" + rendered + ")");
            assertOracle(negativeCreateIds[i], false, checkCreateNumber(val), "checkCreateNumber(" + rendered + ")");
        }
        assertOracle(negativeIds[negatives.length - 1], false, NumberUtils.isNumber("1.1L"),
                "NumberUtils.isNumber(\"1.1L\")");
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedOracles();

        String arbitrary = data.consumeString(64);
        try {
            NumberUtils.isNumber(arbitrary);
        } catch (Throwable t) {
            return;
        }

        String base;
        try {
            int whole = data.consumeInt(0, 1000000);
            int frac = data.consumeInt(0, 1000000);
            boolean neg = data.consumeBoolean();
            base = (neg ? "-" : "") + whole + "." + frac;
        } catch (Throwable t) {
            return;
        }

        boolean plain;
        try {
            plain = NumberUtils.isNumber(base);
        } catch (Throwable t) {
            return;
        }

        boolean withLong;
        try {
            withLong = NumberUtils.isNumber(base + "L");
        } catch (Throwable t) {
            return;
        }

        // Contract pinned by the trusted LANG-664 test: a valid decimal form is accepted,
        // but the same decimal with an 'L' suffix is invalid. A patch that merely deletes
        // the failing branch or makes it unreachable would still return the wrong boolean here.
        if (!plain) {
            throw new FuzzerSecurityIssueLow("[oracle:decimal-with-long-suffix] semantic mismatch: NumberUtils.isNumber(\""
                    + base + "\") expected=true actual=false");
        }
        if (withLong) {
            throw new FuzzerSecurityIssueLow("[oracle:decimal-with-long-suffix] semantic mismatch: NumberUtils.isNumber(\""
                    + base + "L\") expected=false actual=true");
        }

        boolean ok;
        try {
            ok = NumberUtils.isNumber(base);
        } catch (Throwable t) {
            return;
        }
        if (!ok) {
            throw new FuzzerSecurityIssueLow("[oracle:accepted-decimal-recognized] semantic mismatch: NumberUtils.isNumber(\""
                    + base + "\") expected=true actual=false");
        }

        Number n;
        try {
            n = NumberUtils.createNumber(base);
        } catch (Throwable t) {
            return;
        }

        // Trusted by the original test's repeated coupling of isNumber(val) with checkCreateNumber(val):
        // for accepted plain decimals, createNumber must succeed and return non-null.
        if (n == null) {
            throw new FuzzerSecurityIssueLow("[oracle:accepted-decimal-creatable] semantic mismatch: NumberUtils.createNumber(\""
                    + base + "\") expected non-null actual=null");
        }

        int intVal;
        try {
            intVal = data.consumeInt(-1000000, 1000000);
        } catch (Throwable t) {
            return;
        }
        String canonicalInt = Integer.toString(intVal);
        int parsedInt;
        try {
            parsedInt = NumberUtils.toInt(canonicalInt, intVal + 1);
        } catch (Throwable t) {
            return;
        }

        // Oracle from known answer: the harness chooses intVal first, then builds its canonical decimal string.
        // Parsing that exact canonical string with toInt(String,int) must recover intVal, never the default.
        if (parsedInt != intVal) {
            throw new FuzzerSecurityIssueLow("[oracle:toint-roundtrip] semantic mismatch: NumberUtils.toInt(\""
                    + canonicalInt + "\", " + (intVal + 1) + ") expected=" + intVal + " actual=" + parsedInt);
        }

        String equivalent1 = data.consumeBoolean() ? "1.1L" : "1.1l";
        boolean eqResult;
        try {
            eqResult = NumberUtils.isNumber(equivalent1);
        } catch (Throwable t) {
            return;
        }

        // Equivalent-input relation: the implementation checks both 'l' and 'L' in the same suffix branch,
        // so case must not change the accept/reject result for the same decimal-with-long-suffix form.
        if (eqResult != false) {
            throw new FuzzerSecurityIssueLow("[oracle:long-suffix-case-equivalence] semantic mismatch: NumberUtils.isNumber(\""
                    + equivalent1 + "\") expected=false actual=true");
        }
    }
}
```

</details>

---
## [77] ⚙️ harness-attempt · `attempt_021`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:isnum-lang-664] semantic mismatch: NumberUtils.isNumber("1.1L") expected=false actual=true

---
## [78] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [40] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,814 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String[] positives = new String[] {
            "12345",
            "1234.5",
            ".12345",
            "1234E5",
            "1234E+5",
            "1234E-5",
            "123.4E5",
            "-1234",
            "-1234.5",
            "-.12345",
            "-1234E5",
            "0",
            "-0",
            "01234",
            "-01234",
            "0xABC123",
            "0x0",
            "123.4E21D",
            "-221.23F",
            "22338L"
        };

        String[] negatives = new String[] {
            null,
            "",
            "--2.3",
            ".12.3",
            "-123E",
            "-123E+-212",
            "-123E2.12",
            "0xGF",
            "0xFAE-1",
            ".",
            "-0ABC123",
            "123.4E-D",
            "123.4ED",
            "1234E5l",
            "11a",
            "1a",
            "a",
            "11g",
            "11z",
            "11def",
            "11d11",
            "11 11",
            " 1111",
            "1111 "
        };

        for (int i = 0; i < positives.length; i++) {
            String val = positives[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (!actualIsNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-isnumber-pos-" + (i + 1) + "] semantic mismatch: input=" + String.valueOf(val) + " expected=true actual=false");
            }

            boolean actualCheckCreateNumber;
            try {
                Object obj = NumberUtils.createNumber(val);
                actualCheckCreateNumber = obj != null;
            } catch (NumberFormatException e) {
                actualCheckCreateNumber = false;
            }
            if (!actualCheckCreateNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-createnumber-pos-" + (i + 1) + "] semantic mismatch: input=" + String.valueOf(val) + " expected=true actual=false");
            }
        }

        for (int i = 0; i < negatives.length; i++) {
            String val = negatives[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (actualIsNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-isnumber-neg-" + (i + 1) + "] semantic mismatch: input=" + String.valueOf(val) + " expected=false actual=true");
            }

            boolean actualCheckCreateNumber;
            try {
                Object obj = NumberUtils.createNumber(val);
                actualCheckCreateNumber = obj != null;
            } catch (NumberFormatException e) {
                actualCheckCreateNumber = false;
            }
            if (actualCheckCreateNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-createnumber-neg-" + (i + 1) + "] semantic mismatch: input=" + String.valueOf(val) + " expected=false actual=true");
            }
        }

        if (!NumberUtils.isNumber("2.")) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-lang521] semantic mismatch: input=2. expected=true actual=false");
        }

        if (NumberUtils.isNumber("1.1L")) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-lang664] semantic mismatch: input=1.1L expected=false actual=true");
        }

        String fuzzed = data.consumeString(64);
        NumberUtils.isNumber(fuzzed);
        try {
            NumberUtils.createNumber(fuzzed);
        } catch (RuntimeException ignored) {
        }

        int left = Math.abs(data.consumeInt(0, 1_000_000));
        int right = Math.abs(data.consumeInt(0, 1_000_000));
        String decimal = Integer.toString(left) + "." + Integer.toString(right);

        try {
            boolean decimalOk = NumberUtils.isNumber(decimal);
            boolean longUpperOk = NumberUtils.isNumber(decimal + "L");
            boolean longLowerOk = NumberUtils.isNumber(decimal + "l");

            if (!decimalOk) {
                throw new RuntimeException(
                    "[oracle:metamorphic-decimal-base] metamorphic violation: canonical decimal literal should be accepted input=" + decimal + " actual=false");
            }

            // Contract visible in NumberUtils.isNumber source: "not allowing L with an exponent or decimal point".
            // A throw-deleting/branch-skipping patch would violate this observable post-condition by accepting decimal literals suffixed with L/l.
            if (longUpperOk) {
                throw new RuntimeException(
                    "[oracle:metamorphic-decimal-long-upper] metamorphic violation: appending 'L' to a canonical decimal literal with a decimal point must make isNumber false input="
                        + decimal + " lhs=isNumber(" + decimal + "L)=true rhs=expectedFalse");
            }
            if (longLowerOk) {
                throw new RuntimeException(
                    "[oracle:metamorphic-decimal-long-lower] metamorphic violation: appending 'l' to a canonical decimal literal with a decimal point must make isNumber false input="
                        + decimal + " lhs=isNumber(" + decimal + "l)=true rhs=expectedFalse");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable ignored) {
            return;
        }
    }
}
```

</details>

---
## [79] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Every oracle alarm in your harness needs a name: the alarm near line 61 has no oracle ID (`throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow( "[oracle:lifted-isnumber-pos-" + (i + 1) +

---
## [80] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~6,555 chars, 2 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [40] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String[] positives = new String[] {
            "12345",
            "1234.5",
            ".12345",
            "1234E5",
            "1234E+5",
            "1234E-5",
            "123.4E5",
            "-1234",
            "-1234.5",
            "-.12345",
            "-1234E5",
            "0",
            "-0",
            "01234",
            "-01234",
            "0xABC123",
            "0x0",
            "123.4E21D",
            "-221.23F",
            "22338L"
        };

        String[] negatives = new String[] {
            null,
            "",
            "--2.3",
            ".12.3",
            "-123E",
            "-123E+-212",
            "-123E2.12",
            "0xGF",
            "0xFAE-1",
            ".",
            "-0ABC123",
            "123.4E-D",
            "123.4ED",
            "1234E5l",
            "11a",
            "1a",
            "a",
            "11g",
            "11z",
            "11def",
            "11d11",
            "11 11",
            " 1111",
            "1111 "
        };

        for (int i = 0; i < positives.length; i++) {
            String val = positives[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (!actualIsNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-isnumber-pos-" + (i + 1) + "] semantic mismatch: input=" + String.valueOf(val) + " expected=true actual=false");
            }

            boolean actualCheckCreateNumber;
            try {
                Object obj = NumberUtils.createNumber(val);
                actualCheckCreateNumber = obj != null;
            } catch (NumberFormatException e) {
                actualCheckCreateNumber = false;
            }
            if (!actualCheckCreateNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-createnumber-pos-" + (i + 1) + "] semantic mismatch: input=" + String.valueOf(val) + " expected=true actual=false");
            }
        }

        for (int i = 0; i < negatives.length; i++) {
            String val = negatives[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (actualIsNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-isnumber-neg-" + (i + 1) + "] semantic mismatch: input=" + String.valueOf(val) + " expected=false actual=true");
            }

            boolean actualCheckCreateNumber;
            try {
                Object obj = NumberUtils.createNumber(val);
                actualCheckCreateNumber = obj != null;
            } catch (NumberFormatException e) {
                actualCheckCreateNumber = false;
            }
            if (actualCheckCreateNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-createnumber-neg-" + (i + 1) + "] semantic mismatch: input=" + String.valueOf(val) + " expected=false actual=true");
            }
        }

        if (!NumberUtils.isNumber("2.")) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-lang521] semantic mismatch: input=2. expected=true actual=false");
        }

        if (NumberUtils.isNumber("1.1L")) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-lang664] semantic mismatch: input=1.1L expected=false actual=true");
        }

        String fuzzed = data.consumeString(64);
        NumberUtils.isNumber(fuzzed);
        try {
            NumberUtils.createNumber(fuzzed);
        } catch (RuntimeException ignored) {
        }

        int left = Math.abs(data.consumeInt(0, 1_000_000));
        int right = Math.abs(data.consumeInt(0, 1_000_000));
        String decimal = Integer.toString(left) + "." + Integer.toString(right);

        try {
            boolean decimalOk = NumberUtils.isNumber(decimal);
            boolean longUpperOk = NumberUtils.isNumber(decimal + "L");
            boolean longLowerOk = NumberUtils.isNumber(decimal + "l");

            if (!decimalOk) {
                throw new RuntimeException(
                    "[oracle:metamorphic-decimal-base] metamorphic violation: canonical decimal literal should be accepted input=" + decimal + " actual=false");
            }

            // Contract visible in NumberUtils.isNumber source: "not allowing L with an exponent or decimal point".
            // A throw-deleting/branch-skipping patch would violate this observable post-condition by accepting decimal literals suffixed with L/l.
            if (longUpperOk) {
                throw new RuntimeException(
                    "[oracle:metamorphic-decimal-long-upper] metamorphic violation: appending 'L' to a canonical decimal literal with a decimal point must make isNumber false input="
                        + decimal + " lhs=isNumber(" + decimal + "L)=true rhs=expectedFalse");
            }
            if (longLowerOk) {
                throw new RuntimeException(
                    "[oracle:metamorphic-decimal-long-lower] metamorphic violation: appending 'l' to a canonical decimal literal with a decimal point must make isNumber false input="
                        + decimal + " lhs=isNumber(" + decimal + "l)=true rhs=expectedFalse");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable ignored) {
            return;
        }
    }
}
```
**[user]**
```
Every oracle alarm in your harness needs a name: the alarm near line 61 has no oracle ID (`throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow( "[oracle:lifted-isnumber-pos-" + (i + 1) + "] semanti...`) — start its message with "[oracle:<short-id>]" so acceptance can tell which check earned its place
Start each distinct check's message with [oracle:<short-id>], e.g. `throw new FuzzerSecurityIssueLow("[oracle:round-trip] semantic mismatch: ...")`. Use a DIFFERENT id per check. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~5,812 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String[] positives = new String[] {
            "12345",
            "1234.5",
            ".12345",
            "1234E5",
            "1234E+5",
            "1234E-5",
            "123.4E5",
            "-1234",
            "-1234.5",
            "-.12345",
            "-1234E5",
            "0",
            "-0",
            "01234",
            "-01234",
            "0xABC123",
            "0x0",
            "123.4E21D",
            "-221.23F",
            "22338L"
        };

        String[] negatives = new String[] {
            null,
            "",
            "--2.3",
            ".12.3",
            "-123E",
            "-123E+-212",
            "-123E2.12",
            "0xGF",
            "0xFAE-1",
            ".",
            "-0ABC123",
            "123.4E-D",
            "123.4ED",
            "1234E5l",
            "11a",
            "1a",
            "a",
            "11g",
            "11z",
            "11def",
            "11d11",
            "11 11",
            " 1111",
            "1111 "
        };

        for (int i = 0; i < positives.length; i++) {
            String val = positives[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (!actualIsNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-isnumber-positive] semantic mismatch: caseIndex=" + i + " input=" + String.valueOf(val) + " expected=true actual=false");
            }

            boolean actualCheckCreateNumber;
            try {
                Object obj = NumberUtils.createNumber(val);
                actualCheckCreateNumber = obj != null;
            } catch (NumberFormatException e) {
                actualCheckCreateNumber = false;
            }
            if (!actualCheckCreateNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-createnumber-positive] semantic mismatch: caseIndex=" + i + " input=" + String.valueOf(val) + " expected=true actual=false");
            }
        }

        for (int i = 0; i < negatives.length; i++) {
            String val = negatives[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (actualIsNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-isnumber-negative] semantic mismatch: caseIndex=" + i + " input=" + String.valueOf(val) + " expected=false actual=true");
            }

            boolean actualCheckCreateNumber;
            try {
                Object obj = NumberUtils.createNumber(val);
                actualCheckCreateNumber = obj != null;
            } catch (NumberFormatException e) {
                actualCheckCreateNumber = false;
            }
            if (actualCheckCreateNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-createnumber-negative] semantic mismatch: caseIndex=" + i + " input=" + String.valueOf(val) + " expected=false actual=true");
            }
        }

        if (!NumberUtils.isNumber("2.")) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-lang521] semantic mismatch: input=2. expected=true actual=false");
        }

        if (NumberUtils.isNumber("1.1L")) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-lang664] semantic mismatch: input=1.1L expected=false actual=true");
        }

        String fuzzed = data.consumeString(64);
        NumberUtils.isNumber(fuzzed);
        try {
            NumberUtils.createNumber(fuzzed);
        } catch (RuntimeException ignored) {
        }

        int left = Math.abs(data.consumeInt(0, 1000000));
        int right = Math.abs(data.consumeInt(0, 1000000));
        String decimal = Integer.toString(left) + "." + Integer.toString(right);

        try {
            boolean decimalOk = NumberUtils.isNumber(decimal);
            boolean longUpperOk = NumberUtils.isNumber(decimal + "L");
            boolean longLowerOk = NumberUtils.isNumber(decimal + "l");

            if (!decimalOk) {
                throw new RuntimeException(
                    "[oracle:metamorphic-decimal-base] metamorphic violation: canonical decimal literal should be accepted input=" + decimal + " actual=false");
            }

            // Contract in NumberUtils.isNumber source: "not allowing L with an exponent or decimal point".
            // Therefore, if a valid decimal number contains a decimal point, adding L/l must make isNumber return false.
            if (longUpperOk) {
                throw new RuntimeException(
                    "[oracle:metamorphic-decimal-long-upper] metamorphic violation: appending 'L' to a canonical decimal literal with a decimal point must make isNumber false input="
                        + decimal + " lhs=isNumber(" + decimal + "L)=true rhs=expectedFalse");
            }
            if (longLowerOk) {
                throw new RuntimeException(
                    "[oracle:metamorphic-decimal-long-lower] metamorphic violation: appending 'l' to a canonical decimal literal with a decimal point must make isNumber false input="
                        + decimal + " lhs=isNumber(" + decimal + "l)=true rhs=expectedFalse");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable ignored) {
            return;
        }
    }
}
```

</details>

---
## [81] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `actualCheckCreateNumber` (literal assignment, no rethrow, t

---
## [82] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~7,113 chars, 2 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [40] — not reprinted*
- *[assistant] message: identical to step [80] — not reprinted*
- *[user] message: identical to step [80] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String[] positives = new String[] {
            "12345",
            "1234.5",
            ".12345",
            "1234E5",
            "1234E+5",
            "1234E-5",
            "123.4E5",
            "-1234",
            "-1234.5",
            "-.12345",
            "-1234E5",
            "0",
            "-0",
            "01234",
            "-01234",
            "0xABC123",
            "0x0",
            "123.4E21D",
            "-221.23F",
            "22338L"
        };

        String[] negatives = new String[] {
            null,
            "",
            "--2.3",
            ".12.3",
            "-123E",
            "-123E+-212",
            "-123E2.12",
            "0xGF",
            "0xFAE-1",
            ".",
            "-0ABC123",
            "123.4E-D",
            "123.4ED",
            "1234E5l",
            "11a",
            "1a",
            "a",
            "11g",
            "11z",
            "11def",
            "11d11",
            "11 11",
            " 1111",
            "1111 "
        };

        for (int i = 0; i < positives.length; i++) {
            String val = positives[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (!actualIsNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-isnumber-positive] semantic mismatch: caseIndex=" + i + " input=" + String.valueOf(val) + " expected=true actual=false");
            }

            boolean actualCheckCreateNumber;
            try {
                Object obj = NumberUtils.createNumber(val);
                actualCheckCreateNumber = obj != null;
            } catch (NumberFormatException e) {
                actualCheckCreateNumber = false;
            }
            if (!actualCheckCreateNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-createnumber-positive] semantic mismatch: caseIndex=" + i + " input=" + String.valueOf(val) + " expected=true actual=false");
            }
        }

        for (int i = 0; i < negatives.length; i++) {
            String val = negatives[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (actualIsNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-isnumber-negative] semantic mismatch: caseIndex=" + i + " input=" + String.valueOf(val) + " expected=false actual=true");
            }

            boolean actualCheckCreateNumber;
            try {
                Object obj = NumberUtils.createNumber(val);
                actualCheckCreateNumber = obj != null;
            } catch (NumberFormatException e) {
                actualCheckCreateNumber = false;
            }
            if (actualCheckCreateNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-createnumber-negative] semantic mismatch: caseIndex=" + i + " input=" + String.valueOf(val) + " expected=false actual=true");
            }
        }

        if (!NumberUtils.isNumber("2.")) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-lang521] semantic mismatch: input=2. expected=true actual=false");
        }

        if (NumberUtils.isNumber("1.1L")) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-lang664] semantic mismatch: input=1.1L expected=false actual=true");
        }

        String fuzzed = data.consumeString(64);
        NumberUtils.isNumber(fuzzed);
        try {
            NumberUtils.createNumber(fuzzed);
        } catch (RuntimeException ignored) {
        }

        int left = Math.abs(data.consumeInt(0, 1000000));
        int right = Math.abs(data.consumeInt(0, 1000000));
        String decimal = Integer.toString(left) + "." + Integer.toString(right);

        try {
            boolean decimalOk = NumberUtils.isNumber(decimal);
            boolean longUpperOk = NumberUtils.isNumber(decimal + "L");
            boolean longLowerOk = NumberUtils.isNumber(decimal + "l");

            if (!decimalOk) {
                throw new RuntimeException(
                    "[oracle:metamorphic-decimal-base] metamorphic violation: canonical decimal literal should be accepted input=" + decimal + " actual=false");
            }

            // Contract in NumberUtils.isNumber source: "not allowing L with an exponent or decimal point".
            // Therefore, if a valid decimal number contains a decimal point, adding L/l must make isNumber return false.
            if (longUpperOk) {
                throw new RuntimeException(
                    "[oracle:metamorphic-decimal-long-upper] metamorphic violation: appending 'L' to a canonical decimal literal with a decimal point must make isNumber false input="
                        + decimal + " lhs=isNumber(" + decimal + "L)=true rhs=expectedFalse");
            }
            if (longLowerOk) {
                throw new RuntimeException(
                    "[oracle:metamorphic-decimal-long-lower] metamorphic violation: appending 'l' to a canonical decimal literal with a decimal point must make isNumber false input="
                        + decimal + " lhs=isNumber(" + decimal + "l)=true rhs=expectedFalse");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable ignored) {
            return;
        }
    }
}
```
**[user]**
```
Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `actualCheckCreateNumber` (literal assignment, no rethrow, the caught exception is never used) and the alarm near line 73 is conditioned on `actualCheckCreateNumber` — the caught exception's identity is destroyed, so attribution cannot tell WHICH exception fired or whether the unpatched build crashes the same way. Rethrow the exception as the alarm's cause (`..., e`), or assert on the real observable instead of a swallowed boolean.
A caught exception is a REJECTION, never a violation, and flattening it into a flag destroys the identity attribution needs. Either assert on the real observable the test pins (compute the actual value and compare it), or if you must re-report a caught crash, re-throw it with the original as the cause: `new FuzzerSecurityIssueLow("[oracle:<id>] ...", e)`. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~5,430 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static boolean checkCreateNumber(String val) {
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

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String[] positives = new String[] {
            "12345",
            "1234.5",
            ".12345",
            "1234E5",
            "1234E+5",
            "1234E-5",
            "123.4E5",
            "-1234",
            "-1234.5",
            "-.12345",
            "-1234E5",
            "0",
            "-0",
            "01234",
            "-01234",
            "0xABC123",
            "0x0",
            "123.4E21D",
            "-221.23F",
            "22338L"
        };

        String[] negatives = new String[] {
            null,
            "",
            "--2.3",
            ".12.3",
            "-123E",
            "-123E+-212",
            "-123E2.12",
            "0xGF",
            "0xFAE-1",
            ".",
            "-0ABC123",
            "123.4E-D",
            "123.4ED",
            "1234E5l",
            "11a",
            "1a",
            "a",
            "11g",
            "11z",
            "11def",
            "11d11",
            "11 11",
            " 1111",
            "1111 "
        };

        for (int i = 0; i < positives.length; i++) {
            String val = positives[i];
            if (!NumberUtils.isNumber(val)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-isnumber-positive] semantic mismatch: caseIndex=" + i + " input=" + String.valueOf(val) + " expected=true actual=false");
            }

            if (!checkCreateNumber(val)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-createnumber-positive] semantic mismatch: caseIndex=" + i + " input=" + String.valueOf(val) + " expected=true actual=false");
            }
        }

        for (int i = 0; i < negatives.length; i++) {
            String val = negatives[i];
            if (NumberUtils.isNumber(val)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-isnumber-negative] semantic mismatch: caseIndex=" + i + " input=" + String.valueOf(val) + " expected=false actual=true");
            }

            if (checkCreateNumber(val)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-createnumber-negative] semantic mismatch: caseIndex=" + i + " input=" + String.valueOf(val) + " expected=false actual=true");
            }
        }

        if (!NumberUtils.isNumber("2.")) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-lang521] semantic mismatch: input=2. expected=true actual=false");
        }

        if (NumberUtils.isNumber("1.1L")) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-lang664] semantic mismatch: input=1.1L expected=false actual=true");
        }

        String fuzzed = data.consumeString(64);
        NumberUtils.isNumber(fuzzed);
        try {
            NumberUtils.createNumber(fuzzed);
        } catch (RuntimeException ignored) {
        }

        int left = data.consumeInt(0, 1000000);
        int right = data.consumeInt(0, 1000000);
        String decimal = Integer.toString(left) + "." + Integer.toString(right);

        try {
            boolean decimalOk = NumberUtils.isNumber(decimal);
            boolean longUpperOk = NumberUtils.isNumber(decimal + "L");
            boolean longLowerOk = NumberUtils.isNumber(decimal + "l");

            if (!decimalOk) {
                throw new RuntimeException(
                    "[oracle:metamorphic-decimal-base] metamorphic violation: canonical decimal literal should be accepted input=" + decimal + " actual=false");
            }

            // NumberUtils.isNumber documents in-code that it is "not allowing L with an exponent or decimal point".
            // So for any non-degenerate canonical decimal constructed here, adding L/l must change acceptance to false.
            if (longUpperOk) {
                throw new RuntimeException(
                    "[oracle:metamorphic-decimal-long-upper] metamorphic violation: appending 'L' to a canonical decimal literal with a decimal point must make isNumber false input="
                        + decimal + " lhs=isNumber(" + decimal + "L)=true rhs=expectedFalse");
            }
            if (longLowerOk) {
                throw new RuntimeException(
                    "[oracle:metamorphic-decimal-long-lower] metamorphic violation: appending 'l' to a canonical decimal literal with a decimal point must make isNumber false input="
                        + decimal + " lhs=isNumber(" + decimal + "l)=true rhs=expectedFalse");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable ignored) {
            return;
        }
    }
}
```

</details>

---
## [83] ⚙️ harness-attempt · `attempt_024`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-lang664] semantic mismatch: input=1.1L expected=false actual=true

---
## [84] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [36] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,612 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String arbitrary = data.consumeString(64);
        try {
            NumberUtils.isNumber(arbitrary);
        } catch (Throwable ignored) {
        }
        try {
            checkCreateNumber(arbitrary);
        } catch (Throwable ignored) {
        }

        final String[] positiveVals = new String[] {
            "12345",
            "1234.5",
            ".12345",
            "1234E5",
            "1234E+5",
            "1234E-5",
            "123.4E5",
            "-1234",
            "-1234.5",
            "-.12345",
            "-1234E5",
            "0",
            "-0",
            "01234",
            "-01234",
            "0xABC123",
            "0x0",
            "123.4E21D",
            "-221.23F",
            "22338L"
        };

        final String[] negativeVals = new String[] {
            null,
            "",
            "--2.3",
            ".12.3",
            "-123E",
            "-123E+-212",
            "-123E2.12",
            "0xGF",
            "0xFAE-1",
            ".",
            "-0ABC123",
            "123.4E-D",
            "123.4ED",
            "1234E5l",
            "11a",
            "1a",
            "a",
            "11g",
            "11z",
            "11def",
            "11d11",
            "11 11",
            " 1111",
            "1111 "
        };

        for (int i = 0; i < positiveVals.length; i++) {
            String val = positiveVals[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (!actualIsNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:isNumber-positive-" + (i + 1) + "] semantic mismatch: NumberUtils.isNumber(" + String.valueOf(val) + ") expected=true actual=false");
            }
            boolean actualCreate = checkCreateNumber(val);
            if (!actualCreate) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:createNumber-positive-" + (i + 1) + "] semantic mismatch: checkCreateNumber(" + String.valueOf(val) + ") expected=true actual=false");
            }
        }

        for (int i = 0; i < negativeVals.length; i++) {
            String val = negativeVals[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (actualIsNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:isNumber-negative-" + (i + 1) + "] semantic mismatch: NumberUtils.isNumber(" + String.valueOf(val) + ") expected=false actual=true");
            }
            boolean actualCreate = checkCreateNumber(val);
            if (actualCreate) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:createNumber-negative-" + (i + 1) + "] semantic mismatch: checkCreateNumber(" + String.valueOf(val) + ") expected=false actual=true");
            }
        }

        boolean lang521 = NumberUtils.isNumber("2.");
        if (!lang521) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lang-521] semantic mismatch: NumberUtils.isNumber(2.) expected=true actual=false");
        }

        boolean lang664 = NumberUtils.isNumber("1.1L");
        if (lang664) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lang-664] semantic mismatch: NumberUtils.isNumber(1.1L) expected=false actual=true");
        }

        int whole = data.consumeInt(0, 1_000_000);
        int frac = data.consumeInt(0, 1_000_000);
        boolean neg = data.consumeBoolean();
        String base = (neg ? "-" : "") + whole + "." + frac;

        boolean plain;
        try {
            plain = NumberUtils.isNumber(base);
        } catch (Throwable ignored) {
            return;
        }
        boolean withLong;
        try {
            withLong = NumberUtils.isNumber(base + "L");
        } catch (Throwable ignored) {
            return;
        }
        if (!plain) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:decimal-with-long-suffix-is-rejected] metamorphic violation: plain decimal should be accepted but isNumber(" + base + ") was false");
        }
        if (withLong) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:decimal-with-long-suffix-is-rejected] metamorphic violation: decimal with long suffix should be rejected but isNumber(" + base + "L) was true");
        }

        Number created;
        try {
            created = NumberUtils.createNumber(base);
        } catch (Throwable ignored) {
            return;
        }
        if (created == null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:accepted-plain-decimal-has-creatable-number] metamorphic violation: createNumber(" + base + ") returned null for an accepted plain decimal");
        }

        // Contract/invariant from the parser rule in NumberUtils.isNumber: both 'l' and 'L' are handled by the same branch,
        // so for any valid-by-construction decimal with a decimal point, both suffixed forms must be rejected.
        boolean withLowerLong;
        try {
            withLowerLong = NumberUtils.isNumber(base + "l");
        } catch (Throwable ignored) {
            return;
        }
        if (withLowerLong != withLong) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:long-suffix-case-agreement] metamorphic violation: isNumber(" + base + "l)=" + withLowerLong + " disagrees with isNumber(" + base + "L)=" + withLong);
        }
        if (withLowerLong) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:long-suffix-case-agreement] metamorphic violation: decimal with lowercase long suffix should be rejected but isNumber(" + base + "l) was true");
        }
    }

    private static boolean checkCreateNumber(String val) {
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
}
```

</details>

---
## [85] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Every oracle alarm in your harness needs a name: the alarm near line 71 has no oracle ID (`throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow( "[oracle:isNumber-positive-" + (i + 1) + "

---
## [86] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~7,353 chars, 2 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [36] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String arbitrary = data.consumeString(64);
        try {
            NumberUtils.isNumber(arbitrary);
        } catch (Throwable ignored) {
        }
        try {
            checkCreateNumber(arbitrary);
        } catch (Throwable ignored) {
        }

        final String[] positiveVals = new String[] {
            "12345",
            "1234.5",
            ".12345",
            "1234E5",
            "1234E+5",
            "1234E-5",
            "123.4E5",
            "-1234",
            "-1234.5",
            "-.12345",
            "-1234E5",
            "0",
            "-0",
            "01234",
            "-01234",
            "0xABC123",
            "0x0",
            "123.4E21D",
            "-221.23F",
            "22338L"
        };

        final String[] negativeVals = new String[] {
            null,
            "",
            "--2.3",
            ".12.3",
            "-123E",
            "-123E+-212",
            "-123E2.12",
            "0xGF",
            "0xFAE-1",
            ".",
            "-0ABC123",
            "123.4E-D",
            "123.4ED",
            "1234E5l",
            "11a",
            "1a",
            "a",
            "11g",
            "11z",
            "11def",
            "11d11",
            "11 11",
            " 1111",
            "1111 "
        };

        for (int i = 0; i < positiveVals.length; i++) {
            String val = positiveVals[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (!actualIsNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:isNumber-positive-" + (i + 1) + "] semantic mismatch: NumberUtils.isNumber(" + String.valueOf(val) + ") expected=true actual=false");
            }
            boolean actualCreate = checkCreateNumber(val);
            if (!actualCreate) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:createNumber-positive-" + (i + 1) + "] semantic mismatch: checkCreateNumber(" + String.valueOf(val) + ") expected=true actual=false");
            }
        }

        for (int i = 0; i < negativeVals.length; i++) {
            String val = negativeVals[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (actualIsNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:isNumber-negative-" + (i + 1) + "] semantic mismatch: NumberUtils.isNumber(" + String.valueOf(val) + ") expected=false actual=true");
            }
            boolean actualCreate = checkCreateNumber(val);
            if (actualCreate) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:createNumber-negative-" + (i + 1) + "] semantic mismatch: checkCreateNumber(" + String.valueOf(val) + ") expected=false actual=true");
            }
        }

        boolean lang521 = NumberUtils.isNumber("2.");
        if (!lang521) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lang-521] semantic mismatch: NumberUtils.isNumber(2.) expected=true actual=false");
        }

        boolean lang664 = NumberUtils.isNumber("1.1L");
        if (lang664) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lang-664] semantic mismatch: NumberUtils.isNumber(1.1L) expected=false actual=true");
        }

        int whole = data.consumeInt(0, 1_000_000);
        int frac = data.consumeInt(0, 1_000_000);
        boolean neg = data.consumeBoolean();
        String base = (neg ? "-" : "") + whole + "." + frac;

        boolean plain;
        try {
            plain = NumberUtils.isNumber(base);
        } catch (Throwable ignored) {
            return;
        }
        boolean withLong;
        try {
            withLong = NumberUtils.isNumber(base + "L");
        } catch (Throwable ignored) {
            return;
        }
        if (!plain) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:decimal-with-long-suffix-is-rejected] metamorphic violation: plain decimal should be accepted but isNumber(" + base + ") was false");
        }
        if (withLong) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:decimal-with-long-suffix-is-rejected] metamorphic violation: decimal with long suffix should be rejected but isNumber(" + base + "L) was true");
        }

        Number created;
        try {
            created = NumberUtils.createNumber(base);
        } catch (Throwable ignored) {
            return;
        }
        if (created == null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:accepted-plain-decimal-has-creatable-number] metamorphic violation: createNumber(" + base + ") returned null for an accepted plain decimal");
        }

        // Contract/invariant from the parser rule in NumberUtils.isNumber: both 'l' and 'L' are handled by the same branch,
        // so for any valid-by-construction decimal with a decimal point, both suffixed forms must be rejected.
        boolean withLowerLong;
        try {
            withLowerLong = NumberUtils.isNumber(base + "l");
        } catch (Throwable ignored) {
            return;
        }
        if (withLowerLong != withLong) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:long-suffix-case-agreement] metamorphic violation: isNumber(" + base + "l)=" + withLowerLong + " disagrees with isNumber(" + base + "L)=" + withLong);
        }
        if (withLowerLong) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:long-suffix-case-agreement] metamorphic violation: decimal with lowercase long suffix should be rejected but isNumber(" + base + "l) was true");
        }
    }

    private static boolean checkCreateNumber(String val) {
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
}
```
**[user]**
```
Every oracle alarm in your harness needs a name: the alarm near line 71 has no oracle ID (`throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow( "[oracle:isNumber-positive-" + (i + 1) + "] semantic ...`) — start its message with "[oracle:<short-id>]" so acceptance can tell which check earned its place
Start each distinct check's message with [oracle:<short-id>], e.g. `throw new FuzzerSecurityIssueLow("[oracle:round-trip] semantic mismatch: ...")`. Use a DIFFERENT id per check. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~6,766 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        String arbitrary = data.consumeString(64);
        try {
            NumberUtils.isNumber(arbitrary);
        } catch (Throwable ignored) {
        }
        try {
            checkCreateNumber(arbitrary);
        } catch (Throwable ignored) {
        }

        final String[] positiveVals = new String[] {
            "12345",
            "1234.5",
            ".12345",
            "1234E5",
            "1234E+5",
            "1234E-5",
            "123.4E5",
            "-1234",
            "-1234.5",
            "-.12345",
            "-1234E5",
            "0",
            "-0",
            "01234",
            "-01234",
            "0xABC123",
            "0x0",
            "123.4E21D",
            "-221.23F",
            "22338L"
        };

        final String[] negativeVals = new String[] {
            null,
            "",
            "--2.3",
            ".12.3",
            "-123E",
            "-123E+-212",
            "-123E2.12",
            "0xGF",
            "0xFAE-1",
            ".",
            "-0ABC123",
            "123.4E-D",
            "123.4ED",
            "1234E5l",
            "11a",
            "1a",
            "a",
            "11g",
            "11z",
            "11def",
            "11d11",
            "11 11",
            " 1111",
            "1111 "
        };

        for (int i = 0; i < positiveVals.length; i++) {
            String val = positiveVals[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (!actualIsNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-positive-isNumber] semantic mismatch: NumberUtils.isNumber(" + String.valueOf(val) + ") expected=true actual=false");
            }
            boolean actualCreate = checkCreateNumber(val);
            if (!actualCreate) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-positive-createNumber] semantic mismatch: checkCreateNumber(" + String.valueOf(val) + ") expected=true actual=false");
            }
        }

        for (int i = 0; i < negativeVals.length; i++) {
            String val = negativeVals[i];
            boolean actualIsNumber = NumberUtils.isNumber(val);
            if (actualIsNumber) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-negative-isNumber] semantic mismatch: NumberUtils.isNumber(" + String.valueOf(val) + ") expected=false actual=true");
            }
            boolean actualCreate = checkCreateNumber(val);
            if (actualCreate) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-negative-createNumber] semantic mismatch: checkCreateNumber(" + String.valueOf(val) + ") expected=false actual=true");
            }
        }

        boolean lang521 = NumberUtils.isNumber("2.");
        if (!lang521) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lang-521] semantic mismatch: NumberUtils.isNumber(2.) expected=true actual=false");
        }

        boolean lang664 = NumberUtils.isNumber("1.1L");
        if (lang664) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lang-664] semantic mismatch: NumberUtils.isNumber(1.1L) expected=false actual=true");
        }

        int whole = data.consumeInt(0, 1_000_000);
        int frac = data.consumeInt(0, 1_000_000);
        boolean neg = data.consumeBoolean();
        String base = (neg ? "-" : "") + whole + "." + frac;

        boolean plain;
        try {
            plain = NumberUtils.isNumber(base);
        } catch (Throwable ignored) {
            return;
        }
        boolean withLong;
        try {
            withLong = NumberUtils.isNumber(base + "L");
        } catch (Throwable ignored) {
            return;
        }

        // NumberUtils.isNumber accepts a valid decimal string with digits on both sides of '.'.
        // The failing test pins that adding an 'L' suffix to such a decimal is invalid; a patch that
        // merely makes the buggy branch unreachable would violate this observable parser result.
        if (!plain) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:decimal-with-long-suffix-plain-accepted] metamorphic violation: plain decimal should be accepted but isNumber(" + base + ") was false");
        }
        if (withLong) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:decimal-with-long-suffix-rejected] metamorphic violation: decimal with long suffix should be rejected but isNumber(" + base + "L) was true");
        }

        // Trusted by the lifted test pattern: for accepted numeric strings, createNumber succeeds and returns non-null.
        Number created;
        try {
            created = NumberUtils.createNumber(base);
        } catch (Throwable ignored) {
            return;
        }
        if (created == null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:accepted-plain-decimal-creatable] metamorphic violation: createNumber(" + base + ") returned null for an accepted plain decimal");
        }

        // The implementation explicitly treats 'l' and 'L' in the same branch, so case must not change acceptance.
        boolean withLowerLong;
        try {
            withLowerLong = NumberUtils.isNumber(base + "l");
        } catch (Throwable ignored) {
            return;
        }
        if (withLowerLong != withLong) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:long-suffix-case-agreement] metamorphic violation: isNumber(" + base + "l)=" + withLowerLong + " disagrees with isNumber(" + base + "L)=" + withLong);
        }
        if (withLowerLong) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lowercase-long-suffix-rejected] metamorphic violation: decimal with lowercase long suffix should be rejected but isNumber(" + base + "l) was true");
        }
    }

    private static boolean checkCreateNumber(String val) {
        try {
            Object obj = NumberUtils.createNumber(val);
            return obj != null;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
```

</details>

---
## [87] ⚙️ harness-attempt · `attempt_026`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lang-664] semantic mismatch: NumberUtils.isNumber(1.1L) expected=false actual=true

---
## [88] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [40] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,962 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static boolean checkCreateNumber(String val) {
        try {
            Object obj = NumberUtils.createNumber(val);
            return obj != null;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static void requireTrue(String oracleId, String what, boolean actual) {
        if (!actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: expected true for " + what + " actual=false");
        }
    }

    private static void requireFalse(String oracleId, String what, boolean actual) {
        if (actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: expected false for " + what + " actual=true");
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String[] positiveVals = new String[] {
            "12345",
            "1234.5",
            ".12345",
            "1234E5",
            "1234E+5",
            "1234E-5",
            "123.4E5",
            "-1234",
            "-1234.5",
            "-.12345",
            "-1234E5",
            "0",
            "-0",
            "01234",
            "-01234",
            "0xABC123",
            "0x0",
            "123.4E21D",
            "-221.23F",
            "22338L",
            "2."
        };

        String[] negativeVals = new String[] {
            null,
            "",
            "--2.3",
            ".12.3",
            "-123E",
            "-123E+-212",
            "-123E2.12",
            "0xGF",
            "0xFAE-1",
            ".",
            "-0ABC123",
            "123.4E-D",
            "123.4ED",
            "1234E5l",
            "11a",
            "1a",
            "a",
            "11g",
            "11z",
            "11def",
            "11d11",
            "11 11",
            " 1111",
            "1111 ",
            "1.1L"
        };

        for (int i = 0; i < positiveVals.length; i++) {
            String val = positiveVals[i];
            requireTrue("lifted-isNumber-pos-" + (i + 1), "NumberUtils.isNumber(\"" + val + "\")", NumberUtils.isNumber(val));
            requireTrue("lifted-createNumber-pos-" + (i + 1), "checkCreateNumber(\"" + val + "\")", checkCreateNumber(val));
        }

        for (int i = 0; i < negativeVals.length; i++) {
            String val = negativeVals[i];
            String rendered = val == null ? "null" : "\"" + val + "\"";
            requireFalse("lifted-isNumber-neg-" + (i + 1), "NumberUtils.isNumber(" + rendered + ")", NumberUtils.isNumber(val));
            requireFalse("lifted-createNumber-neg-" + (i + 1), "checkCreateNumber(" + rendered + ")", checkCreateNumber(val));
        }

        String fuzz = data.consumeString(64);
        NumberUtils.isNumber(fuzz);

        String fuzzAscii = data.consumeAsciiString(64);
        NumberUtils.isNumber(fuzzAscii);

        int n = data.consumeInt(-1_000_000, 1_000_000);
        String canonicalInt = Integer.toString(n);
        String upperLong = canonicalInt + "L";
        String lowerLong = canonicalInt + "l";

        boolean upperIsNumber;
        boolean lowerIsNumber;
        try {
            upperIsNumber = NumberUtils.isNumber(upperLong);
            lowerIsNumber = NumberUtils.isNumber(lowerLong);
        } catch (Throwable t) {
            return;
        }

        /* Contract/invariant used: the shown implementation explicitly accepts both 'l' and 'L'
           in the same branch (`chars[i] == 'l' || chars[i] == 'L'`), so case of the long suffix
           must not change the result. A throw-deleting or guard-only patch in that branch can make
           one suffix behave differently or allow/disallow the wrong form silently. */
        if (upperIsNumber != lowerIsNumber) {
            throw new RuntimeException(
                "[oracle:metamorphic-suffix-case] metamorphic violation: isNumber must treat l/L equivalently input=" +
                canonicalInt + " lhs=" + upperLong + ":" + upperIsNumber + " rhs=" + lowerLong + ":" + lowerIsNumber);
        }

        /* Documented by the in-code comment in the shown method: 'not allowing L with an exponent or decimal point'.
           Therefore a plain canonical integer with an L suffix is allowed, while adding a decimal point before L is not. */
        if (!upperIsNumber) {
            throw new RuntimeException(
                "[oracle:metamorphic-canonical-long] metamorphic violation: canonical integer plus L must be recognized input=" +
                upperLong + " lhs=" + upperIsNumber + " rhs=true");
        }

        String decimalLong = canonicalInt + ".0L";
        boolean decimalLongIsNumber;
        try {
            decimalLongIsNumber = NumberUtils.isNumber(decimalLong);
        } catch (Throwable t) {
            return;
        }
        if (decimalLongIsNumber) {
            throw new RuntimeException(
                "[oracle:metamorphic-decimal-long] metamorphic violation: decimal form with L must be rejected input=" +
                decimalLong + " lhs=" + decimalLongIsNumber + " rhs=false");
        }

        try {
            Object parsedUpper = NumberUtils.createNumber(upperLong);
            Object parsedLower = NumberUtils.createNumber(lowerLong);
            if (parsedUpper == null || parsedLower == null) {
                throw new RuntimeException(
                    "[oracle:metamorphic-createNumber-long] metamorphic violation: createNumber must parse canonical long suffix forms input=" +
                    canonicalInt + " lhs=" + parsedUpper + " rhs=" + parsedLower);
            }
        } catch (Throwable t) {
            return;
        }
    }
}
```

</details>

---
## [89] ⚙️ harness-attempt · `attempt_027`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-isNumber-neg-25] semantic mismatch: expected false for NumberUtils.isNumber("1.1L") actual=true

---
## [90] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [36] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~9,096 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static boolean checkCreateNumber(String val) {
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

    private static void assertBool(String oracleId, String what, boolean expected, boolean actual) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + what
                    + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void runLiftedOracles() {
        String[] positive = new String[] {
            "12345",
            "1234.5",
            ".12345",
            "1234E5",
            "1234E+5",
            "1234E-5",
            "123.4E5",
            "-1234",
            "-1234.5",
            "-.12345",
            "-1234E5",
            "0",
            "-0",
            "01234",
            "-01234",
            "0xABC123",
            "0x0",
            "123.4E21D",
            "-221.23F",
            "22338L"
        };
        String[] positiveIds = new String[] {
            "testIsNumber-1",
            "testIsNumber-2",
            "testIsNumber-3",
            "testIsNumber-4",
            "testIsNumber-5",
            "testIsNumber-6",
            "testIsNumber-7",
            "testIsNumber-8",
            "testIsNumber-9",
            "testIsNumber-10",
            "testIsNumber-11",
            "testIsNumber-12",
            "testIsNumber-13",
            "testIsNumber-14",
            "testIsNumber-15",
            "testIsNumber-16",
            "testIsNumber-17",
            "testIsNumber-19",
            "testIsNumber-20",
            "testIsNumber-21"
        };

        for (int i = 0; i < positive.length; i++) {
            String val = positive[i];
            assertBool(positiveIds[i], "NumberUtils.isNumber(\"" + val + "\")", true, NumberUtils.isNumber(val));
            assertBool(positiveIds[i] + "-create", "checkCreateNumber(\"" + val + "\")", true, checkCreateNumber(val));
        }

        String[] negative = new String[] {
            null,
            "",
            "--2.3",
            ".12.3",
            "-123E",
            "-123E+-212",
            "-123E2.12",
            "0xGF",
            "0xFAE-1",
            ".",
            "-0ABC123",
            "123.4E-D",
            "123.4ED",
            "1234E5l",
            "11a",
            "1a",
            "a",
            "11g",
            "11z",
            "11def",
            "11d11",
            "11 11",
            " 1111",
            "1111 "
        };
        String[] negativeIds = new String[] {
            "testIsNumber-neg-1",
            "testIsNumber-neg-2",
            "testIsNumber-neg-3",
            "testIsNumber-neg-4",
            "testIsNumber-neg-5",
            "testIsNumber-neg-6",
            "testIsNumber-neg-7",
            "testIsNumber-neg-8",
            "testIsNumber-neg-9",
            "testIsNumber-neg-10",
            "testIsNumber-neg-11",
            "testIsNumber-neg-12",
            "testIsNumber-neg-13",
            "testIsNumber-neg-14",
            "testIsNumber-neg-15",
            "testIsNumber-neg-16",
            "testIsNumber-neg-17",
            "testIsNumber-neg-18",
            "testIsNumber-neg-19",
            "testIsNumber-neg-20",
            "testIsNumber-neg-21",
            "testIsNumber-neg-22",
            "testIsNumber-neg-23",
            "testIsNumber-neg-24"
        };

        for (int i = 0; i < negative.length; i++) {
            String val = negative[i];
            assertBool(negativeIds[i], "NumberUtils.isNumber(" + String.valueOf(val) + ")", false, NumberUtils.isNumber(val));
            assertBool(negativeIds[i] + "-create", "checkCreateNumber(" + String.valueOf(val) + ")", false, checkCreateNumber(val));
        }

        assertBool("testIsNumber-LANG-521", "NumberUtils.isNumber(\"2.\")", true, NumberUtils.isNumber("2."));
        assertBool("testIsNumber-LANG-664", "NumberUtils.isNumber(\"1.1L\")", false, NumberUtils.isNumber("1.1L"));
    }

    private static void relationDecimalWithLongSuffixIsRejected(FuzzedDataProvider data) {
        String base;
        try {
            int whole = data.consumeInt(0, 1000000);
            int frac = data.consumeInt(0, 1000000);
            boolean neg = data.consumeBoolean();
            base = (neg ? "-" : "") + whole + "." + frac;
        } catch (Exception e) {
            return;
        }

        boolean plain;
        try {
            plain = NumberUtils.isNumber(base);
        } catch (Exception e) {
            return;
        }

        boolean withLong;
        try {
            withLong = NumberUtils.isNumber(base + "L");
        } catch (Exception e) {
            return;
        }

        /* Contract/ground truth: the trusted failing test pins that a decimal form is accepted,
           but the same decimal with an L suffix is invalid ("1.1L" must be false). A patch that
           merely makes the buggy branch unreachable or silently returns the wrong boolean breaks this. */
        if (!plain) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:decimal-with-long-suffix-is-rejected] semantic mismatch: plain decimal should be accepted but isNumber(\""
                    + base + "\") was false");
        }
        if (withLong) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:decimal-with-long-suffix-is-rejected] semantic mismatch: decimal with long suffix should be rejected but isNumber(\""
                    + base + "L\") was true");
        }
    }

    private static void relationAcceptedPlainDecimalHasCreatableNumber(FuzzedDataProvider data) {
        String s;
        try {
            int whole = data.consumeInt(0, 1000000);
            int frac = data.consumeInt(0, 1000000);
            s = (data.consumeBoolean() ? "-" : "") + whole + "." + frac;
        } catch (Exception e) {
            return;
        }

        boolean ok;
        try {
            ok = NumberUtils.isNumber(s);
        } catch (Exception e) {
            return;
        }

        if (!ok) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:accepted-plain-decimal-has-creatable-number] semantic mismatch: valid plain decimal was rejected by isNumber(\""
                    + s + "\")");
        }

        Number n;
        try {
            n = NumberUtils.createNumber(s);
        } catch (Exception e) {
            return;
        }

        /* Contract from the lifted test/helper pairing: for accepted numeric strings,
           createNumber must succeed and return a non-null Number; deleting bookkeeping or
           returning an arbitrary boolean from isNumber would violate this sibling agreement. */
        if (n == null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:accepted-plain-decimal-has-creatable-number] semantic mismatch: createNumber(\""
                    + s + "\") returned null for an accepted number string");
        }
    }

    private static void relationQuestionMethodIsStable(FuzzedDataProvider data) {
        String s = data.consumeString(64);
        boolean first;
        try {
            first = NumberUtils.isNumber(s);
        } catch (Exception e) {
            return;
        }
        boolean second;
        try {
            second = NumberUtils.isNumber(s);
        } catch (Exception e) {
            return;
        }

        /* isNumber is a pure question method over its String input; repeating the same call on the
           same immutable input must yield the same observable result, so a patch that silently
           mutates hidden state or uses stale bookkeeping is caught here. */
        if (first != second) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:question-method-stability] metamorphic violation: repeated isNumber call disagreed input="
                    + String.valueOf(s) + " first=" + first + " second=" + second);
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String exploratory = data.consumeString(64);
        try {
            NumberUtils.isNumber(exploratory);
        } catch (Exception ignored) {
        }
        try {
            checkCreateNumber(exploratory);
        } catch (Exception ignored) {
        }

        runLiftedOracles();
        relationDecimalWithLongSuffixIsRejected(data);
        relationAcceptedPlainDecimalHasCreatableNumber(data);
        relationQuestionMethodIsStable(data);
    }
}
```

</details>

---
## [91] ⚙️ harness-attempt · `attempt_028`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:testIsNumber-LANG-664] semantic mismatch: NumberUtils.isNumber("1.1L") expected=false actual=true

---
## [92] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [40] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,370 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String[] positiveVals = new String[] {
            "12345",
            "1234.5",
            ".12345",
            "1234E5",
            "1234E+5",
            "1234E-5",
            "123.4E5",
            "-1234",
            "-1234.5",
            "-.12345",
            "-1234E5",
            "0",
            "-0",
            "01234",
            "-01234",
            "0xABC123",
            "0x0",
            "123.4E21D",
            "-221.23F",
            "22338L",
            "2."
        };

        String[] negativeVals = new String[] {
            null,
            "",
            "--2.3",
            ".12.3",
            "-123E",
            "-123E+-212",
            "-123E2.12",
            "0xGF",
            "0xFAE-1",
            ".",
            "-0ABC123",
            "123.4E-D",
            "123.4ED",
            "1234E5l",
            "11a",
            "1a",
            "a",
            "11g",
            "11z",
            "11def",
            "11d11",
            "11 11",
            " 1111",
            "1111 ",
            "1.1L"
        };

        for (int i = 0; i < positiveVals.length; i++) {
            String val = positiveVals[i];
            boolean actual = NumberUtils.isNumber(val);
            if (!actual) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-isNumber-pos-" + i + "] semantic mismatch: NumberUtils.isNumber(\"" + val + "\") expected=true actual=false");
            }
            boolean actualCreate = checkCreateNumber(val);
            if (!actualCreate) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-createNumber-pos-" + i + "] semantic mismatch: checkCreateNumber(\"" + val + "\") expected=true actual=false");
            }
        }

        for (int i = 0; i < negativeVals.length; i++) {
            String val = negativeVals[i];
            boolean actual = NumberUtils.isNumber(val);
            if (actual) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-isNumber-neg-" + i + "] semantic mismatch: NumberUtils.isNumber(" + quote(val) + ") expected=false actual=true");
            }
            boolean actualCreate = checkCreateNumber(val);
            if (actualCreate) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-createNumber-neg-" + i + "] semantic mismatch: checkCreateNumber(" + quote(val) + ") expected=false actual=true");
            }
        }

        // Contract visible in NumberUtils.isNumber: "not allowing L with an exponent or decimal point".
        // Therefore any canonically constructed decimal-with-long-qualifier string must be rejected.
        int intPart = data.consumeInt(0, 1_000_000);
        int fracPart = data.consumeInt(0, 1_000_000);
        String decimalLongUpper = Integer.toString(intPart) + "." + Integer.toString(fracPart) + "L";
        String decimalLongLower = Integer.toString(intPart) + "." + Integer.toString(fracPart) + "l";
        if (NumberUtils.isNumber(decimalLongUpper)) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-upper] metamorphic violation: decimal-point number with 'L' qualifier must be rejected input="
                    + quote(decimalLongUpper) + " actual=true");
        }
        if (NumberUtils.isNumber(decimalLongLower)) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-lower] metamorphic violation: decimal-point number with 'l' qualifier must be rejected input="
                    + quote(decimalLongLower) + " actual=true");
        }

        // Same documented guarantee as above, exercised with an exponent as well.
        int mantissa = data.consumeInt(0, 1_000_000);
        int exponent = data.consumeInt(0, 1_000_000);
        String expLong = Integer.toString(mantissa) + "E" + Integer.toString(exponent) + "L";
        if (NumberUtils.isNumber(expLong)) {
            throw new RuntimeException(
                "[oracle:exponent-long-suffix] metamorphic violation: exponent number with 'L' qualifier must be rejected input="
                    + quote(expLong) + " actual=true");
        }

        // Exercise the patched path on arbitrary fuzz input through the real public API without asserting
        // when we do not have an independently justified expected value.
        String arbitrary = data.consumeRemainingAsString();
        NumberUtils.isNumber(arbitrary);
    }

    private static boolean checkCreateNumber(String val) {
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

    private static String quote(String s) {
        return s == null ? "null" : "\"" + s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t").replace("\"", "\\\"") + "\"";
    }
}
```

</details>

---
## [93] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Every oracle alarm in your harness needs a name: the alarm near line 63 has no oracle ID (`throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow( "[oracle:lifted-isNumber-pos-" + i + "] se

---
## [94] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~6,111 chars, 2 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [40] — not reprinted*
**[assistant]**
```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String[] positiveVals = new String[] {
            "12345",
            "1234.5",
            ".12345",
            "1234E5",
            "1234E+5",
            "1234E-5",
            "123.4E5",
            "-1234",
            "-1234.5",
            "-.12345",
            "-1234E5",
            "0",
            "-0",
            "01234",
            "-01234",
            "0xABC123",
            "0x0",
            "123.4E21D",
            "-221.23F",
            "22338L",
            "2."
        };

        String[] negativeVals = new String[] {
            null,
            "",
            "--2.3",
            ".12.3",
            "-123E",
            "-123E+-212",
            "-123E2.12",
            "0xGF",
            "0xFAE-1",
            ".",
            "-0ABC123",
            "123.4E-D",
            "123.4ED",
            "1234E5l",
            "11a",
            "1a",
            "a",
            "11g",
            "11z",
            "11def",
            "11d11",
            "11 11",
            " 1111",
            "1111 ",
            "1.1L"
        };

        for (int i = 0; i < positiveVals.length; i++) {
            String val = positiveVals[i];
            boolean actual = NumberUtils.isNumber(val);
            if (!actual) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-isNumber-pos-" + i + "] semantic mismatch: NumberUtils.isNumber(\"" + val + "\") expected=true actual=false");
            }
            boolean actualCreate = checkCreateNumber(val);
            if (!actualCreate) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-createNumber-pos-" + i + "] semantic mismatch: checkCreateNumber(\"" + val + "\") expected=true actual=false");
            }
        }

        for (int i = 0; i < negativeVals.length; i++) {
            String val = negativeVals[i];
            boolean actual = NumberUtils.isNumber(val);
            if (actual) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-isNumber-neg-" + i + "] semantic mismatch: NumberUtils.isNumber(" + quote(val) + ") expected=false actual=true");
            }
            boolean actualCreate = checkCreateNumber(val);
            if (actualCreate) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lifted-createNumber-neg-" + i + "] semantic mismatch: checkCreateNumber(" + quote(val) + ") expected=false actual=true");
            }
        }

        // Contract visible in NumberUtils.isNumber: "not allowing L with an exponent or decimal point".
        // Therefore any canonically constructed decimal-with-long-qualifier string must be rejected.
        int intPart = data.consumeInt(0, 1_000_000);
        int fracPart = data.consumeInt(0, 1_000_000);
        String decimalLongUpper = Integer.toString(intPart) + "." + Integer.toString(fracPart) + "L";
        String decimalLongLower = Integer.toString(intPart) + "." + Integer.toString(fracPart) + "l";
        if (NumberUtils.isNumber(decimalLongUpper)) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-upper] metamorphic violation: decimal-point number with 'L' qualifier must be rejected input="
                    + quote(decimalLongUpper) + " actual=true");
        }
        if (NumberUtils.isNumber(decimalLongLower)) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-lower] metamorphic violation: decimal-point number with 'l' qualifier must be rejected input="
                    + quote(decimalLongLower) + " actual=true");
        }

        // Same documented guarantee as above, exercised with an exponent as well.
        int mantissa = data.consumeInt(0, 1_000_000);
        int exponent = data.consumeInt(0, 1_000_000);
        String expLong = Integer.toString(mantissa) + "E" + Integer.toString(exponent) + "L";
        if (NumberUtils.isNumber(expLong)) {
            throw new RuntimeException(
                "[oracle:exponent-long-suffix] metamorphic violation: exponent number with 'L' qualifier must be rejected input="
                    + quote(expLong) + " actual=true");
        }

        // Exercise the patched path on arbitrary fuzz input through the real public API without asserting
        // when we do not have an independently justified expected value.
        String arbitrary = data.consumeRemainingAsString();
        NumberUtils.isNumber(arbitrary);
    }

    private static boolean checkCreateNumber(String val) {
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

    private static String quote(String s) {
        return s == null ? "null" : "\"" + s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t").replace("\"", "\\\"") + "\"";
    }
}
```
**[user]**
```
Every oracle alarm in your harness needs a name: the alarm near line 63 has no oracle ID (`throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow( "[oracle:lifted-isNumber-pos-" + i + "] semantic mism...`) — start its message with "[oracle:<short-id>]" so acceptance can tell which check earned its place
Start each distinct check's message with [oracle:<short-id>], e.g. `throw new FuzzerSecurityIssueLow("[oracle:round-trip] semantic mismatch: ...")`. Use a DIFFERENT id per check. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~11,581 chars)</summary>

```
package org.apache.commons.lang3.math;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String[] positiveVals = new String[] {
            "12345",
            "1234.5",
            ".12345",
            "1234E5",
            "1234E+5",
            "1234E-5",
            "123.4E5",
            "-1234",
            "-1234.5",
            "-.12345",
            "-1234E5",
            "0",
            "-0",
            "01234",
            "-01234",
            "0xABC123",
            "0x0",
            "123.4E21D",
            "-221.23F",
            "22338L",
            "2."
        };

        String[] negativeVals = new String[] {
            null,
            "",
            "--2.3",
            ".12.3",
            "-123E",
            "-123E+-212",
            "-123E2.12",
            "0xGF",
            "0xFAE-1",
            ".",
            "-0ABC123",
            "123.4E-D",
            "123.4ED",
            "1234E5l",
            "11a",
            "1a",
            "a",
            "11g",
            "11z",
            "11def",
            "11d11",
            "11 11",
            " 1111",
            "1111 ",
            "1.1L"
        };

        for (int i = 0; i < positiveVals.length; i++) {
            String val = positiveVals[i];
            boolean actual = NumberUtils.isNumber(val);
            if (!actual) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    oracleIdForPositiveIsNumber(i) + " semantic mismatch: NumberUtils.isNumber(\"" + val + "\") expected=true actual=false");
            }
            boolean actualCreate = checkCreateNumber(val);
            if (!actualCreate) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    oracleIdForPositiveCreateNumber(i) + " semantic mismatch: checkCreateNumber(\"" + val + "\") expected=true actual=false");
            }
        }

        for (int i = 0; i < negativeVals.length; i++) {
            String val = negativeVals[i];
            boolean actual = NumberUtils.isNumber(val);
            if (actual) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    oracleIdForNegativeIsNumber(i) + " semantic mismatch: NumberUtils.isNumber(" + quote(val) + ") expected=false actual=true");
            }
            boolean actualCreate = checkCreateNumber(val);
            if (actualCreate) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    oracleIdForNegativeCreateNumber(i) + " semantic mismatch: checkCreateNumber(" + quote(val) + ") expected=false actual=true");
            }
        }

        // Contract visible in NumberUtils.isNumber: "not allowing L with an exponent or decimal point".
        // Therefore any canonically constructed decimal-with-long-qualifier string must be rejected.
        int intPart = data.consumeInt(0, 1_000_000);
        int fracPart = data.consumeInt(0, 1_000_000);
        String decimalLongUpper = Integer.toString(intPart) + "." + Integer.toString(fracPart) + "L";
        String decimalLongLower = Integer.toString(intPart) + "." + Integer.toString(fracPart) + "l";
        if (NumberUtils.isNumber(decimalLongUpper)) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-upper] metamorphic violation: decimal-point number with 'L' qualifier must be rejected input="
                    + quote(decimalLongUpper) + " actual=true");
        }
        if (NumberUtils.isNumber(decimalLongLower)) {
            throw new RuntimeException(
                "[oracle:decimal-long-suffix-lower] metamorphic violation: decimal-point number with 'l' qualifier must be rejected input="
                    + quote(decimalLongLower) + " actual=true");
        }

        // Same documented guarantee as above, exercised with an exponent as well.
        int mantissa = data.consumeInt(0, 1_000_000);
        int exponent = data.consumeInt(0, 1_000_000);
        String expLong = Integer.toString(mantissa) + "E" + Integer.toString(exponent) + "L";
        if (NumberUtils.isNumber(expLong)) {
            throw new RuntimeException(
                "[oracle:exponent-long-suffix] metamorphic violation: exponent number with 'L' qualifier must be rejected input="
                    + quote(expLong) + " actual=true");
        }

        String arbitrary = data.consumeRemainingAsString();
        NumberUtils.isNumber(arbitrary);
    }

    private static boolean checkCreateNumber(String val) {
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

    private static String oracleIdForPositiveIsNumber(int i) {
        switch (i) {
            case 0: return "[oracle:lifted-isNumber-1]";
            case 1: return "[oracle:lifted-isNumber-2]";
            case 2: return "[oracle:lifted-isNumber-3]";
            case 3: return "[oracle:lifted-isNumber-4]";
            case 4: return "[oracle:lifted-isNumber-5]";
            case 5: return "[oracle:lifted-isNumber-6]";
            case 6: return "[oracle:lifted-isNumber-7]";
            case 7: return "[oracle:lifted-isNumber-8]";
            case 8: return "[oracle:lifted-isNumber-9]";
            case 9: return "[oracle:lifted-isNumber-10]";
            case 10: return "[oracle:lifted-isNumber-11]";
            case 11: return "[oracle:lifted-isNumber-12]";
            case 12: return "[oracle:lifted-isNumber-13]";
            case 13: return "[oracle:lifted-isNumber-14]";
            case 14: return "[oracle:lifted-isNumber-15]";
            case 15: return "[oracle:lifted-isNumber-16]";
            case 16: return "[oracle:lifted-isNumber-17]";
            case 17: return "[oracle:lifted-isNumber-19]";
            case 18: return "[oracle:lifted-isNumber-20]";
            case 19: return "[oracle:lifted-isNumber-21]";
            case 20: return "[oracle:lifted-isNumber-LANG-521]";
            default: return "[oracle:lifted-isNumber-pos]";
        }
    }

    private static String oracleIdForPositiveCreateNumber(int i) {
        switch (i) {
            case 0: return "[oracle:lifted-createNumber-1]";
            case 1: return "[oracle:lifted-createNumber-2]";
            case 2: return "[oracle:lifted-createNumber-3]";
            case 3: return "[oracle:lifted-createNumber-4]";
            case 4: return "[oracle:lifted-createNumber-5]";
            case 5: return "[oracle:lifted-createNumber-6]";
            case 6: return "[oracle:lifted-createNumber-7]";
            case 7: return "[oracle:lifted-createNumber-8]";
            case 8: return "[oracle:lifted-createNumber-9]";
            case 9: return "[oracle:lifted-createNumber-10]";
            case 10: return "[oracle:lifted-createNumber-11]";
            case 11: return "[oracle:lifted-createNumber-12]";
            case 12: return "[oracle:lifted-createNumber-13]";
            case 13: return "[oracle:lifted-createNumber-14]";
            case 14: return "[oracle:lifted-createNumber-15]";
            case 15: return "[oracle:lifted-createNumber-16]";
            case 16: return "[oracle:lifted-createNumber-17]";
            case 17: return "[oracle:lifted-createNumber-19]";
            case 18: return "[oracle:lifted-createNumber-20]";
            case 19: return "[oracle:lifted-createNumber-21]";
            case 20: return "[oracle:lifted-createNumber-LANG-521]";
            default: return "[oracle:lifted-createNumber-pos]";
        }
    }

    private static String oracleIdForNegativeIsNumber(int i) {
        switch (i) {
            case 0: return "[oracle:lifted-isNumber-1-Neg]";
            case 1: return "[oracle:lifted-isNumber-2-Neg]";
            case 2: return "[oracle:lifted-isNumber-3-Neg]";
            case 3: return "[oracle:lifted-isNumber-4-Neg]";
            case 4: return "[oracle:lifted-isNumber-5-Neg]";
            case 5: return "[oracle:lifted-isNumber-6-Neg]";
            case 6: return "[oracle:lifted-isNumber-7-Neg]";
            case 7: return "[oracle:lifted-isNumber-8-Neg]";
            case 8: return "[oracle:lifted-isNumber-9-Neg]";
            case 9: return "[oracle:lifted-isNumber-10-Neg]";
            case 10: return "[oracle:lifted-isNumber-11-Neg]";
            case 11: return "[oracle:lifted-isNumber-12-Neg]";
            case 12: return "[oracle:lifted-isNumber-13-Neg]";
            case 13: return "[oracle:lifted-isNumber-14-Neg]";
            case 14: return "[oracle:lifted-isNumber-15-Neg]";
            case 15: return "[oracle:lifted-isNumber-16-Neg]";
            case 16: return "[oracle:lifted-isNumber-17-Neg]";
            case 17: return "[oracle:lifted-isNumber-18-Neg]";
            case 18: return "[oracle:lifted-isNumber-19-Neg]";
            case 19: return "[oracle:lifted-isNumber-20-Neg]";
            case 20: return "[oracle:lifted-isNumber-21-Neg]";
            case 21: return "[oracle:lifted-isNumber-22-Neg]";
            case 22: return "[oracle:lifted-isNumber-23-Neg]";
            case 23: return "[oracle:lifted-isNumber-24-Neg]";
            case 24: return "[oracle:lifted-isNumber-LANG-664]";
            default: return "[oracle:lifted-isNumber-neg]";
        }
    }

    private static String oracleIdForNegativeCreateNumber(int i) {
        switch (i) {
            case 0: return "[oracle:lifted-createNumber-1-Neg]";
            case 1: return "[oracle:lifted-createNumber-2-Neg]";
            case 2: return "[oracle:lifted-createNumber-3-Neg]";
            case 3: return "[oracle:lifted-createNumber-4-Neg]";
            case 4: return "[oracle:lifted-createNumber-5-Neg]";
            case 5: return "[oracle:lifted-createNumber-6-Neg]";
            case 6: return "[oracle:lifted-createNumber-7-Neg]";
            case 7: return "[oracle:lifted-createNumber-8-Neg]";
            case 8: return "[oracle:lifted-createNumber-9-Neg]";
            case 9: return "[oracle:lifted-createNumber-10-Neg]";
            case 10: return "[oracle:lifted-createNumber-11-Neg]";
            case 11: return "[oracle:lifted-createNumber-12-Neg]";
            case 12: return "[oracle:lifted-createNumber-13-Neg]";
            case 13: return "[oracle:lifted-createNumber-14-Neg]";
            case 14: return "[oracle:lifted-createNumber-15-Neg]";
            case 15: return "[oracle:lifted-createNumber-16-Neg]";
            case 16: return "[oracle:lifted-createNumber-17-Neg]";
            case 17: return "[oracle:lifted-createNumber-18-Neg]";
            case 18: return "[oracle:lifted-createNumber-19-Neg]";
            case 19: return "[oracle:lifted-createNumber-20-Neg]";
            case 20: return "[oracle:lifted-createNumber-21-Neg]";
            case 21: return "[oracle:lifted-createNumber-22-Neg]";
            case 22: return "[oracle:lifted-createNumber-23-Neg]";
            case 23: return "[oracle:lifted-createNumber-24-Neg]";
            case 24: return "[oracle:lifted-createNumber-LANG-664]";
            default: return "[oracle:lifted-createNumber-neg]";
        }
    }

    private static String quote(String s) {
        return s == null ? "null" : "\"" + s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t").replace("\"", "\\\"") + "\"";
    }
}
```

</details>

---
## [95] ⚙️ harness-repair · `attempt_030`
**output:** **REPAIRED: missing-alarm-id**
- detail: {'applied': ['missing-alarm-id'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [96] ⚙️ harness-attempt · `attempt_030`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: missing-alarm-id]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['missing-alarm-id'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:unnamed-check] [oracle:lifted-isNumber-LANG-664] semantic mismatch: NumberUtils.isNumber("1.1L") expected=false actual=true

---
## [97] ⚙️ coverage · `jazzer coverage dumps`
**output:** **18/18 dump(s) written under /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/cov; 18 raw fuzz log(s)**
<details><summary>▸ detail (~10,122 chars)</summary>

- detail: {'dumps': [{'harness': 'attempt_002', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/cov/attempt_002_compiled.exec', 'exists': True, 'bytes': 424}, {'harness': 'attempt_003', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/cov/attempt_003_compiled.exec', 'exists': True, 'bytes': 422}, {'harness': 'attempt_004', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/cov/attempt_004_compiled.exec', 'exists': True, 'bytes': 435}, {'harness': 'attempt_005', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/cov/attempt_005_compiled.exec', 'exists': True, 'bytes': 423}, {'harness': 'attempt_006', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/cov/attempt_006_compiled.exec', 'exists': True, 'bytes': 427}, {'harness': 'attempt_007', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/cov/attempt_007_compiled.exec', 'exists': True, 'bytes': 421}, {'harness': 'attempt_008', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/cov/attempt_008_compiled.exec', 'exists': True, 'bytes': 426}, {'harness': 'attempt_011', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/cov/attempt_011_compiled.exec', 'exists': True, 'bytes': 421}, {'harness': 'attempt_014', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/cov/attempt_014_compiled.exec', 'exists': True, 'bytes': 425}, {'harness': 'attempt_015', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/cov/attempt_015_compiled.exec', 'exists': True, 'bytes': 426}, {'harness': 'attempt_019', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/cov/attempt_019_compiled.exec', 'exists': True, 'bytes': 427}, {'harness': 'attempt_020', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/cov/attempt_020_compiled.exec', 'exists': True, 'bytes': 422}, {'harness': 'attempt_021', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/cov/attempt_021_compiled.exec', 'exists': True, 'bytes': 423}, {'harness': 'attempt_024', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/cov/attempt_024_compiled.exec', 'exists': True, 'bytes': 421}, {'harness': 'attempt_026', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/cov/attempt_026_compiled.exec', 'exists': True, 'bytes': 422}, {'harness': 'attempt_027', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/cov/attempt_027_compiled.exec', 'exists': True, 'bytes': 421}, {'harness': 'attempt_028', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/cov/attempt_028_compiled.exec', 'exists': True, 'bytes': 423}, {'harness': 'attempt_030', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/cov/attempt_030_compiled.exec', 'exists': True, 'bytes': 434}], 'outputs': ['/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/fuzz_out/attempt_002_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/fuzz_out/attempt_003_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/fuzz_out/attempt_004_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/fuzz_out/attempt_005_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/fuzz_out/attempt_006_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/fuzz_out/attempt_007_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/fuzz_out/attempt_008_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/fuzz_out/attempt_011_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/fuzz_out/attempt_014_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/fuzz_out/attempt_015_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/fuzz_out/attempt_019_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/fuzz_out/attempt_020_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/fuzz_out/attempt_021_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/fuzz_out/attempt_024_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/fuzz_out/attempt_026_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/fuzz_out/attempt_027_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/fuzz_out/attempt_028_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/fuzz_out/attempt_030_compiled.txt'], 'classpath': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/cov/classpath.json', 'dir': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/cov', 'compiled_attempts': ['attempt_002', 'attempt_003', 'attempt_004', 'attempt_005', 'attempt_006', 'attempt_007', 'attempt_008', 'attempt_011', 'attempt_014', 'attempt_015', 'attempt_019', 'attempt_020', 'attempt_021', 'attempt_024', 'attempt_026', 'attempt_027', 'attempt_028', 'attempt_030'], 'accepted_attempts': ['attempt_002', 'attempt_003', 'attempt_004', 'attempt_005', 'attempt_006', 'attempt_007', 'attempt_008', 'attempt_011', 'attempt_014', 'attempt_015', 'attempt_019', 'attempt_020', 'attempt_021', 'attempt_024', 'attempt_026', 'attempt_027', 'attempt_028', 'attempt_030'], 'sources': ['/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/harness_src/attempt_002.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/harness_src/attempt_003.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/harness_src/attempt_004.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/harness_src/attempt_005.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/harness_src/attempt_006.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/harness_src/attempt_007.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/harness_src/attempt_008.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/harness_src/attempt_011.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/harness_src/attempt_014.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/harness_src/attempt_015.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/harness_src/attempt_019.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/harness_src/attempt_020.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/harness_src/attempt_021.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/harness_src/attempt_024.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/harness_src/attempt_026.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/harness_src/attempt_027.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/harness_src/attempt_028.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/001_patch1-Lang-24-ACS_c_rep001/harness_src/attempt_030.java']}

</details>
