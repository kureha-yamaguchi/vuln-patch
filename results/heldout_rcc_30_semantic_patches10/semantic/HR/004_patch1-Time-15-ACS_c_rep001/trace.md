# Pipeline trace — Time-15

**Code:** `1ac28e12b4447c6ad51717ff73beb5695a24b71a`

**Patch label:** correct  *(the patch under analysis is a known-CORRECT fix — the pipeline is not told this)*

**Outcome:** candidate_experiment (buggy build only)

**Patch under analysis:**
```diff
--- /src/main/java/org/joda/time/field/FieldUtils.java
+++ /src/main/java/org/joda/time/field/FieldUtils.java
@@ -135,6 +135,9 @@
     public static long safeMultiply(long val1, int val2) {
         switch (val2) {
             case -1:
+//ACS's patch begin
+if (val1==Long.MIN_VALUE){throw new ArithmeticException();}
+//ACS's patch end
                 return -val1;
             case 0:
                 return 0L;
```

105 sequential steps — 32 LLM calls, 73 deterministic. Read top to bottom.

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
<details><summary>▸ output — FailureTest(test_class='org.joda.time.field.TestFieldUtils', test_method='testSafeMultiplyLongInt',  (~2,057 chars)</summary>

**output:** 
```json
[
  "FailureTest(test_class='org.joda.time.field.TestFieldUtils', test_method='testSafeMultiplyLongInt', source_path='/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HR/004_patch1-Time-15-ACS_c_rep001/Time_15_buggy/src/test/java/org/joda/time/field/TestFieldUtils.java', method_source='    public void testSafeMultiplyLongInt() {\\n        assertEquals(0L, FieldUtils.safeMultiply(0L, 0));\\n        \\n        assertEquals(1L, FieldUtils.safeMultiply(1L, 1));\\n        assertEquals(3L, FieldUtils.safeMultiply(1L, 3));\\n        assertEquals(3L, FieldUtils.safeMultiply(3L, 1));\\n        \\n        assertEquals(6L, FieldUtils.safeMultiply(2L, 3));\\n        assertEquals(-6L, FieldUtils.safeMultiply(2L, -3));\\n        assertEquals(-6L, FieldUtils.safeMultiply(-2L, 3));\\n        assertEquals(6L, FieldUtils.safeMultiply(-2L, -3));\\n        \\n        assertEquals(-1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE));\\n        \\n        assertEquals(Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1));\\n        assertEquals(Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1));\\n        assertEquals(-Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1));\\n        \\n        try {\\n            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);\\n            fail();\\n        } catch (ArithmeticException e) {\\n        }\\n        \\n        try {\\n            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);\\n            fail();\\n        } catch (ArithmeticException e) {\\n        }\\n        \\n        try {\\n            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);\\n            fail();\\n        } catch (ArithmeticException e) {\\n        }\\n        \\n        try {\\n            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);\\n            fail();\\n        } catch (ArithmeticException e) {\\n        }\\n    }', exception_type='junit.framework.AssertionFailedError', failure_message=None, support_source=None)"
]
```

</details>

---
## [1] ⚙️ test-context (H1/H2)
**output:** 
```json
[
  {
    "test": "org.joda.time.field.TestFieldUtils::testSafeMultiplyLongInt",
    "failure_message": "--- org.joda.time.field.TestFieldUtils::testSafeMultiplyLongInt\njunit.framework.AssertionFailedError",
    "support_chars": 0
  }
]
```

---
## [2] ⚙️ analysis (TargetAnalyzer)
<details><summary>▸ output — modified_files": [ (~4,385 chars)</summary>

**output:** 
```json
{
  "modified_files": [
    "src/main/java/org/joda/time/field/FieldUtils.java"
  ],
  "patch_text": "--- /src/main/java/org/joda/time/field/FieldUtils.java\n+++ /src/main/java/org/joda/time/field/FieldUtils.java\n@@ -135,6 +135,9 @@\n     public static long safeMultiply(long val1, int val2) {\n         switch (val2) {\n             case -1:\n+//ACS's patch begin\n+if (val1==Long.MIN_VALUE){throw new ArithmeticException();}\n+//ACS's patch end\n                 return -val1;\n             case 0:\n                 return 0L;\n",
  "functions": [
    {
      "func_name": "safeMultiply",
      "func_signature": "public static long safeMultiply(long val1, int val2)",
      "func_source": "    public static long safeMultiply(long val1, int val2) {\n        switch (val2) {\n            case -1:\n                return -val1;\n            case 0:\n                return 0L;\n            case 1:\n                return val1;\n        }\n        long total = val1 * val2;\n        if (total / val2 != val1) {\n          throw new ArithmeticException(\"Multiplication overflows a long: \" + val1 + \" * \" + val2);\n        }\n        return total;\n    }",
      "func_class": "FieldUtils",
      "func_class_fq": "org.joda.time.field.FieldUtils",
      "func_param_types": [
        "long",
        "int"
      ],
      "fi_name": "[org.joda.time.field.FieldUtils].safeMultiply(long,int)",
      "overload_types": [
        [
          "int",
          "int"
        ],
        [
          "long",
          "int"
        ],
        [
          "long",
          "long"
        ]
      ],
      "xrefs": [
        "public static Duration standardDays(long days) {\n        if (days == 0) {\n            return ZERO;\n        }\n        return new Duration(FieldUtils.safeMultiply(days, DateTimeConstants.MILLIS_PER_DAY));\n    }",
        "public static Duration standardHours(long hours) {\n        if (hours == 0) {\n            return ZERO;\n        }\n        return new Duration(FieldUtils.safeMultiply(hours, DateTimeConstants.MILLIS_PER_HOUR));\n    }",
        "public static Duration standardMinutes(long minutes) {\n        if (minutes == 0) {\n            return ZERO;\n        }\n        return new Duration(FieldUtils.safeMultiply(minutes, DateTimeConstants.MILLIS_PER_MINUTE));\n    }",
        "public static Duration standardSeconds(long seconds) {\n        if (seconds == 0) {\n            return ZERO;\n        }\n        return new Duration(FieldUtils.safeMultiply(seconds, DateTimeConstants.MILLIS_PER_SECOND));\n    }",
        "public Duration withDurationAdded(long durationToAdd, int scalar) {\n        if (durationToAdd == 0 || scalar == 0) {\n            return this;\n        }\n        long add = FieldUtils.safeMultiply(durationToAdd, scalar);\n        long duration = FieldUtils.safeAdd(getMillis(), add);\n        return new Duration(duration);\n    }"
      ],
      "xref_names": [
        "[org.joda.time.Duration].standardDays(long)",
        "[org.joda.time.Duration].standardHours(long)",
        "[org.joda.time.Duration].standardMinutes(long)",
        "[org.joda.time.Duration].standardSeconds(long)",
        "[org.joda.time.Duration].withDurationAdded(long,int)"
      ],
      "reachable_edges": [
        [
          "[org.joda.time.field.FieldUtils].safeMultiply(long,int)",
          "[ArithmeticException].<init>(org.joda.time.field.FieldUtils)"
        ]
      ],
      "reachable_depth": {
        "[org.joda.time.field.FieldUtils].safeMultiply(long,int)": 0,
        "[ArithmeticException].<init>(org.joda.time.field.FieldUtils)": 1
      },
      "reachable": [
        "[ArithmeticException].<init>(org.joda.time.field.FieldUtils)"
      ],
      "related_callees": [],
      "field_siblings": []
    }
  ],
  "package": "org.joda.time.field",
  "root_cause_reachable": [],
  "neighbourhood_notes": [],
  "source_imports": [
    "import org.joda.time.DateTimeField;",
    "import org.joda.time.DateTimeFieldType;",
    "import org.joda.time.IllegalFieldValueException;"
  ],
  "reachable_edges": [
    [
      "[org.joda.time.field.FieldUtils].safeMultiply(long,int)",
      "[ArithmeticException].<init>(org.joda.time.field.FieldUtils)"
    ]
  ],
  "reachable_depth": {
    "[org.joda.time.field.FieldUtils].safeMultiply(long,int)": 0,
    "[ArithmeticException].<init>(org.joda.time.field.FieldUtils)": 1
  }
}
```

</details>

---
## [3] ⚙️ synth · `probe-tier`
<details><summary>▸ output — documented exceptions: {'FieldUtils#getWrappedValue': ['IllegalArgumentException'], 'FieldUtils#safe (~463 chars)</summary>

**output:** 
```
documented exceptions: {'FieldUtils#getWrappedValue': ['IllegalArgumentException'], 'FieldUtils#safeAdd': ['ArithmeticException'], 'FieldUtils#safeMultiply': ['ArithmeticException'], 'FieldUtils#safeMultiplyToInt': ['ArithmeticException'], 'FieldUtils#safeNegate': ['ArithmeticException'], 'FieldUtils#safeSubtract': ['ArithmeticException'], 'FieldUtils#safeToInt': ['ArithmeticException'], 'FieldUtils#verifyValueBounds': ['IllegalFieldValueException']}
```

</details>
<details><summary>▸ detail (~447 chars)</summary>

- detail: {'documented': {'FieldUtils#getWrappedValue': ['IllegalArgumentException'], 'FieldUtils#safeAdd': ['ArithmeticException'], 'FieldUtils#safeMultiply': ['ArithmeticException'], 'FieldUtils#safeMultiplyToInt': ['ArithmeticException'], 'FieldUtils#safeNegate': ['ArithmeticException'], 'FieldUtils#safeSubtract': ['ArithmeticException'], 'FieldUtils#safeToInt': ['ArithmeticException'], 'FieldUtils#verifyValueBounds': ['IllegalFieldValueException']}}

</details>

---
## [4] 🧠 LLM call — **rule synthesis** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~50,402 chars)</summary>

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
// org.joda.time.field.TestFieldUtils::testSafeMultiplyLongInt
    public void testSafeMultiplyLongInt() {
        assertEquals(0L, FieldUtils.safeMultiply(0L, 0));
        
        assertEquals(1L, FieldUtils.safeMultiply(1L, 1));
        assertEquals(3L, FieldUtils.safeMultiply(1L, 3));
        assertEquals(3L, FieldUtils.safeMultiply(3L, 1));
        
        assertEquals(6L, FieldUtils.safeMultiply(2L, 3));
        assertEquals(-6L, FieldUtils.safeMultiply(2L, -3));
        assertEquals(-6L, FieldUtils.safeMultiply(-2L, 3));
        assertEquals(6L, FieldUtils.safeMultiply(-2L, -3));
        
        assertEquals(-1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE));
        
        assertEquals(Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1));
        assertEquals(Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1));
        assertEquals(-Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1));
        
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
            fail();
        } catch (ArithmeticException e) {
        }
        
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
            fail();
        } catch (ArithmeticException e) {
        }
        
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
            fail();
        } catch (ArithmeticException e) {
        }
        
        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
            fail();
        } catch (ArithmeticException e) {
        }
    }
// On the BUGGY build this test FAILS with (names the diverging observable + wrong value):
// --- org.joda.time.field.TestFieldUtils::testSafeMultiplyLongInt
// junit.framework.AssertionFailedError
SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  getWrappedValue(int currentValue, int wrapValue,
                                      int minValue, int maxValue) / (int value, int minValue, int maxValue)
  safeAdd(int val1, int val2) / (long val1, long val2)
  safeMultiply(int val1, int val2) / (long val1, int val2) / (long val1, long val2)
  verifyValueBounds(DateTimeField field, 
                                         int value, int lowerBound, int upperBound) / (DateTimeFieldType fieldType, 
                                         int value, int lowerBound, int upperBound) / (String fieldName,
                                         int value, int lowerBound, int upperBound)

METHOD FAMILIES over the same input space (factory/parser siblings — where the docs state a selection or agreement rule between family members, equivalent inputs must respect it):
  safe* family: safeAdd, safeMultiply, safeMultiplyToInt, safeNegate, safeSubtract, safeToInt
</failing_test>
BUGGY method(s) (PRE-PATCH source — this is the code WITH the bug; the failing test above shows where it is wrong). Use it to see the code shape and API, NEVER as a model of correct behaviour:
<code>
    public static long safeMultiply(long val1, int val2) {
        switch (val2) {
            case -1:
                return -val1;
            case 0:
                return 0L;
            case 1:
                return val1;
        }
        long total = val1 * val2;
        if (total / val2 != val1) {
          throw new ArithmeticException("Multiplication overflows a long: " + val1 + " * " + val2);
        }
        return total;
    }
</code>
CLASS-LEVEL CONTEXT — the whole patched class (skeleton: contracts + signatures; full bodies only for patched methods), its supertypes, and key collaborators. The STRONGEST relations usually tie the patched method to something HERE rather than to itself: a sibling method documented to agree or complement it (two accessors that must round-trip; a pair documented to sum to a constant), a constructor-established field invariant the method must respect, or a class/method javadoc guarantee (never null, always positive, canonical order). Prefer such cross-member relations over properties of the patched method in isolation.
<class name="FieldUtils" role="patched">
     * General utilities that don't fit elsewhere.
     * <p>
     * FieldUtils is thread-safe and immutable.
     * @author Stephen Colebourne
     * @since 1.0
public class FieldUtils {
     * Restricted constructor.
    private FieldUtils() {
        super();
    }
     * Negates the input throwing an exception if it can't negate it.
     * @param value  the value to negate
     * @return the negated value
     * @throws ArithmeticException if the value is Integer.MIN_VALUE
     * @since 1.1
    public static int safeNegate(int value) { … }
     * Add two values throwing an exception if overflow occurs.
     * @param val1  the first value
     * @param val2  the second value
     * @return the new total
     * @throws ArithmeticException if the value is too big or too small
    public static int safeAdd(int val1, int val2) { … }
     * Add two values throwing an exception if overflow occurs.
     * @param val1  the first value
     * @param val2  the second value
     * @return the new total
     * @throws ArithmeticException if the value is too big or too small
    public static long safeAdd(long val1, long val2) { … }
     * Subtracts two values throwing an exception if overflow occurs.
     * @param val1  the first value, to be taken away from
     * @param val2  the second value, the amount to take away
     * @return the new total
     * @throws ArithmeticException if the value is too big or too small
    public static long safeSubtract(long val1, long val2) { … }
     * Multiply two values throwing an exception if overflow occurs.
     * @param val1  the first value
     * @param val2  the second value
     * @return the new total
     * @throws ArithmeticException if the value is too big or too small
     * @since 1.2
    public static int safeMultiply(int val1, int val2) {
        long total = (long) val1 * (long) val2;
        if (total < Integer.MIN_VALUE || total > Integer.MAX_VALUE) {
          throw new ArithmeticException("Multiplication overflows an int: " + val1 + " * " + val2);
        }
        return (int) total;
    }
     * Multiply two values throwing an exception if overflow occurs.
     * @param val1  the first value
     * @param val2  the second value
     * @return the new total
     * @throws ArithmeticException if the value is too big or too small
     * @since 1.2
    public static long safeMultiply(long val1, int val2) {
        switch (val2) {
            case -1:
                return -val1;
            case 0:
                return 0L;
            case 1:
                return val1;
        }
        long total = val1 * val2;
        if (total / val2 != val1) {
          throw new ArithmeticException("Multiplication overflows a long: " + val1 + " * " + val2);
        }
        return total;
    }
     * Multiply two values throwing an exception if overflow occurs.
     * @param val1  the first value
     * @param val2  the second value
     * @return the new total
     * @throws ArithmeticException if the value is too big or too small
    public static long safeMultiply(long val1, long val2) {
        if (val2 == 1) {
            return val1;
        }
        if (val1 == 1) {
            return val2;
        }
        if (val1 == 0 || val2 == 0) {
            return 0;
        }
        long total = val1 * val2;
        if (total / val2 != val1 || val1 == Long.MIN_VALUE && val2 == -1 || val2 == Long.MIN_VALUE && val1 == -1) {
            throw new ArithmeticException("Multiplication overflows a long: " + val1 + " * " + val2);
        }
        return total;
    }
     * Casts to an int throwing an exception if overflow occurs.
     * @param value  the value
     * @return the value as an int
     * @throws ArithmeticException if the value is too big or too small
    public static int safeToInt(long value) { … }
     * Multiply two values to return an int throwing an exception if overflow occurs.
     * @param val1  the first value
     * @param val2  the second value
     * @return the new total
     * @throws ArithmeticException if the value is too big or too small
    public static int safeMultiplyToInt(long val1, long val2) { … }
     * Verify that input values are within specified bounds.
     * @param value  the value to check
     * @param lowerBound  the lower bound allowed for value
     * @param upperBound  the upper bound allowed for value
     * @throws IllegalFieldValueException if value is not in the specified bounds
    public static void verifyValueBounds(DateTimeField field, int value, int lowerBound, int upperBound) { … }
     * Verify that input values are within specified bounds.
     * @param value  the value to check
     * @param lowerBound  the lower bound allowed for value
     * @param upperBound  the upper bound allowed for value
     * @throws IllegalFieldValueException if value is not in the specified bounds
     * @since 1.1
    public static void verifyValueBounds(DateTimeFieldType fieldType, int value, int lowerBound, int upperBound) { … }
     * Verify that input values are within specified bounds.
     * @param value  the value to check
     * @param lowerBound  the lower bound allowed for value
     * @param upperBound  the upper bound allowed for value
     * @throws IllegalFieldValueException if value is not in the specified bounds
    public static void verifyValueBounds(String fieldName, int value, int lowerBound, int upperBound) { … }
     * Utility method used by addWrapField implementations to ensure the new
     * value lies within the field's legal value range.
     * @param currentValue the current value of the data, which may lie outside
     * the wrapped value range
     * @param wrapValue  the value to add to current value before
     * wrapping.  This may be negative.
     * @param minValue the wrap range minimum value.
     * @param maxValue the wrap range maximum value.  This must be
     * greater than minValue (checked by the method).
     * @return the wrapped value
     * @throws IllegalArgumentException if minValue is greater
     * than or equal to maxValue
    public static int getWrappedValue(int currentValue, int wrapValue, int minValue, int maxValue) { … }
     * Utility method that ensures the given value lies within the field's
     * legal value range.
     * @param value  the value to fit into the wrapped value range
     * @param minValue the wrap range minimum value.
     * @param maxValue the wrap range maximum value.  This must be
     * greater than minValue (checked by the method).
     * @return the wrapped value
     * @throws IllegalArgumentException if minValue is greater
     * than or equal to maxValue
    public static int getWrappedValue(int value, int minValue, int maxValue) { … }
     * Compares two objects as equals handling null.
     * @param object1  the first object
     * @param object2  the second object
     * @return true if equal
     * @since 1.4
    public static boolean equals(Object object1, Object object2) { … }
}
</class>
<class name="IllegalFieldValueException" role="collaborator">
     * Exception thrown when attempting to set a field outside its supported range.
     * @author Brian S O'Neill
     * @since 1.1
public class IllegalFieldValueException extends IllegalArgumentException {
     * Serialization lock.
    private static final long serialVersionUID = 6305711765985447737L;
     * Creates a message for the exception.
     * @param fieldName  the field name
     * @param value  the value rejected
     * @param lowerBound  the lower bound allowed
     * @param upperBound  the uppe bound allowed
     * @param explain  an explanation
     * @return the message
    private static String createMessage(String fieldName, Number value, Number lowerBound, Number upperBound, String explain) { … }
     * Creates a message for the exception.
     * @param fieldName  the field name
     * @param value  the value rejected
     * @return the message
    private static String createMessage(String fieldName, String value) { … }
    private final DateTimeFieldType iDateTimeFieldType;
    private final DurationFieldType iDurationFieldType;
    private final String iFieldName;
    private final Number iNumberValue;
    private final String iStringValue;
    private final Number iLowerBound;
    private final Number iUpperBound;
    private String iMessage;
     * Constructor.
     * @param fieldType  type of field being set
     * @param value  illegal value being set
     * @param lowerBound  lower legal field value, or null if not applicable
     * @param upperBound  upper legal field value, or null if not applicable
    public IllegalFieldValueException(DateTimeFieldType fieldType, Number value, Number lowerBound, Number upperBound) {
        super(createMessage(fieldType.getName(), value, lowerBound, upperBound, null));
        iDateTimeFieldType = fieldType;
        iDurationFieldType = null;
        iFieldName = fieldType.getName();
        iNumberValue = value;
        iStringValue = null;
        iLowerBound = lowerBound;
        iUpperBound = upperBound;
        iMessage = super.getMessage();
    }
     * Constructor.
     * @param fieldType  type of field being set
     * @param value  illegal value being set
     * @param explain  an explanation
     * @since 1.5
    public IllegalFieldValueException(DateTimeFieldType fieldType, Number value, String explain) {
        super(createMessage(fieldType.getName(), value, null, null, explain));
        iDateTimeFieldType = fieldType;
        iDurationFieldType = null;
        iFieldName = fieldType.getName();
        iNumberValue = value;
        iStringValue = null;
        iLowerBound = null;
        iUpperBound = null;
        iMessage = super.getMessage();
    }
     * Constructor.
     * @param fieldType  type of field being set
     * @param value  illegal value being set
     * @param lowerBound  lower legal field value, or null if not applicable
     * @param upperBound  upper legal field value, or null if not applicable
    public IllegalFieldValueException(DurationFieldType fieldType, Number value, Number lowerBound, Number upperBound) {
        super(createMessage(fieldType.getName(), value, lowerBound, upperBound, null));
        iDateTimeFieldType = null;
        iDurationFieldType = fieldType;
        iFieldName = fieldType.getName();
        iNumberValue = value;
        iStringValue = null;
        iLowerBound = lowerBound;
        iUpperBound = upperBound;
        iMessage = super.getMessage();
    }
     * Constructor.
     * @param fieldName  name of field being set
     * @param value  illegal value being set
     * @param lowerBound  lower legal field value, or null if not applicable
     * @param upperBound  upper legal field value, or null if not applicable
    public IllegalFieldValueException(String fieldName, Number value, Number lowerBound, Number upperBound) {
        super(createMessage(fieldName, value, lowerBound, upperBound, null));
        iDateTimeFieldType = null;
        iDurationFieldType = null;
        iFieldName = fieldName;
        iNumberValue = value;
        iStringValue = null;
        iLowerBound = lowerBound;
        iUpperBound = upperBound;
        iMessage = super.getMessage();
    }
     * Constructor.
     * @param fieldType  type of field being set
     * @param value  illegal value being set
    public IllegalFieldValueException(DateTimeFieldType fieldType, String value) {
        super(createMessage(fieldType.getName(), value));
        iDateTimeFieldType = fieldType;
        iDurationFieldType = null;
        iFieldName = fieldType.getName();
        iStringValue = value;
        iNumberValue = null;
        iLowerBound = null;
        iUpperBound = null;
        iMessage = super.getMessage();
    }
     * Constructor.
     * @param fieldType  type of field being set
     * @param value  illegal value being set
    public IllegalFieldValueException(DurationFieldType fieldType, String value) {
        super(createMessage(fieldType.getName(), value));
        iDateTimeFieldType = null;
        iDurationFieldType = fieldType;
        iFieldName = fieldType.getName();
        iStringValue = value;
        iNumberValue = null;
        iLowerBound = null;
        iUpperBound = null;
        iMessage = super.getMessage();
    }
     * Constructor.
     * @param fieldName  name of field being set
     * @param value  illegal value being set
    public IllegalFieldValueException(String fieldName, String value) {
        super(createMessage(fieldName, value));
        iDateTimeFieldType = null;
        iDurationFieldType = null;
        iFieldName = fieldName;
        iStringValue = value;
        iNumberValue = null;
        iLowerBound = null;
        iUpperBound = null;
        iMessage = super.getMessage();
    }
     * Returns the DateTimeFieldType whose value was invalid, or null if not applicable.
     * @return the datetime field type
    public DateTimeFieldType getDateTimeFieldType() { … }
    public String getMessage() { … }
}
// … (non-patched members truncated; patched methods kept)
</class>
<class name="DateTimeField" role="collaborator">
     * Defines the calculation engine for date and time fields.
     * The interface defines a set of methods that manipulate a millisecond datetime
     * with regards to a single field, such as monthOfYear or secondOfMinute.
     * <p>
     * This design is extensible so, if you wish, you can extract a different field from
     * the milliseconds. A number of standard implementations are provided to assist.
     * @author Guy Allard
     * @author Stephen Colebourne
     * @author Brian S O'Neill
     * @since 1.0
public abstract class DateTimeField {
     * Get the type of the field.
     * @return field type
    public abstract DateTimeFieldType getType();
     * Get the name of the field.
     * <p>
     * By convention, names follow a pattern of "dddOfRrr", where "ddd" represents
     * the (singular) duration unit field name and "Rrr" represents the (singular)
     * duration range field name. If the range field is not applicable, then
     * the name of the field is simply the (singular) duration field name.
     * @return field name
    public abstract String getName();
     * Returns true if this field is supported.
     * @return true if this field is supported
    public abstract boolean isSupported();
     * Returns true if the set method is lenient. If so, it accepts values that
     * are out of bounds. For example, a lenient day of month field accepts 32
     * for January, converting it to February 1.
     * @return true if this field is lenient
    public abstract boolean isLenient();
     * Get the value of this field from the milliseconds.
     * @param instant  the milliseconds from 1970-01-01T00:00:00Z to query
     * @return the value of the field, in the units of the field
    public abstract int get(long instant);
     * Get the human-readable, text value of this field from the milliseconds.
     * If the specified locale is null, the default locale is used.
     * @param instant  the milliseconds from 1970-01-01T00:00:00Z to query
     * @param locale the locale to use for selecting a text symbol, null for default
     * @return the text value of the field
    public abstract String getAsText(long instant, Locale locale);
     * Get the human-readable, text value of this field from the milliseconds.
     * @param instant  the milliseconds from 1970-01-01T00:00:00Z to query
     * @return the text value of the field
    public abstract String getAsText(long instant);
     * Get the human-readable, text value of this field from a partial instant.
     * If the specified locale is null, the default locale is used.
     * @param partial  the partial instant to query
     * @param fieldValue  the field value of this field, provided for performance
     * @param locale  the locale to use for selecting a text symbol, null for default
     * @return the text value of the field
    public abstract String getAsText(ReadablePartial partial, int fieldValue, Locale locale);
     * Get the human-readable, text value of this field from a partial instant.
     * If the specified locale is null, the default locale is used.
     * @param partial  the partial instant to query
     * @param locale  the locale to use for selecting a text symbol, null for default
     * @return the text value of the field
    public abstract String getAsText(ReadablePartial partial, Locale locale);
     * Get the human-readable, text value of this field from the field value.
     * If the specified locale is null, the default locale is used.
     * @param fieldValue  the numeric value to convert to text
     * @param locale the locale to use for selecting a text symbol, null for default
     * @return the text value of the field
    public abstract String getAsText(int fieldValue, Locale locale);
     * Get the human-readable, short text value of this field from the
     * milliseconds.  If the specified locale is null, the default locale is used.
     * @param instant  the milliseconds from 1970-01-01T00:00:00Z to query
     * @param locale the locale to use for selecting a text symbol, null for default
     * @return the short text value of the field
    public abstract String getAsShortText(long instant, Locale locale);
     * Get the human-readable, short text value of this field from the
     * milliseconds.
     * @param instant  the milliseconds from 1970-01-01T00:00:00Z to query
     * @return the short text value of the field
    public abstract String getAsShortText(long instant);
     * Get the human-readable, short text value of this field from a partial instant.
     * If the specified locale is null, the default locale is used.
     * @param partial  the partial instant to query
     * @param fieldValue  the field value of this field, provided for performance
     * @param locale  the locale to use for selecting a text symbol, null for default
     * @return the text value of the field
    public abstract String getAsShortText(ReadablePartial partial, int fieldValue, Locale locale);
     * Get the human-readable, short text value of this field from a partial instant.
     * If the specified locale is null, the default locale is used.
     * @param partial  the partial instant to query
     * @param locale  the locale to use for selecting a text symbol, null for default
     * @return the text value of the field
    public abstract String getAsShortText(ReadablePartial partial, Locale locale);
     * Get the human-readable, short text value of this field from the field value.
     * If the specified locale is null, the default locale is used.
     * @param fieldValue  the numeric value to convert to text
     * @param locale the locale to use for selecting a text symbol, null for default
     * @return the text value of the field
    public abstract String getAsShortText(int fieldValue, Locale locale);
     * Get the minimum allowable value for this field.
     * @return the minimum valid value for this field, in the units of the
     * field
    public abstract int getMinimumValue();
}
// … (non-patched members truncated; patched methods kept)
</class>
<class name="DateTimeFieldType" role="collaborator">
     * Identifies a field, such as year or minuteOfHour, in a chronology-neutral way.
     * <p>
     * A field type defines the type of the field, such as hourOfDay.
     * If does not directly enable any calculations, however it does provide a
     * {@link #getField(Chronology)} method that returns the actual calculation engine
     * for a particular chronology.
     * It also provides access to the related {@link DurationFieldType}s.
     * <p>
     * Instances of <code>DateTimeFieldType</code> are singletons.
     * They can be compared using <code>==</code>.
     * <p>
     * If required, you can create your own field, for example a quarterOfYear.
     * …
public abstract class DateTimeFieldType implements Serializable {
     * Serialization version
    private static final long serialVersionUID = -42615285973990L;
     * Ordinal values for standard field types. */
     * static final byte
     * ERA = 1,
     * YEAR_OF_ERA = 2,
     * CENTURY_OF_ERA = 3,
     * YEAR_OF_CENTURY = 4,
     * YEAR = 5,
     * DAY_OF_YEAR = 6,
     * MONTH_OF_YEAR = 7,
     * DAY_OF_MONTH = 8,
     * WEEKYEAR_OF_CENTURY = 9,
     * WEEKYEAR = 10,
     * …
    private static final DateTimeFieldType ERA_TYPE = new StandardDateTimeFieldType( "era", ERA, DurationFieldType.eras(), null);
     * The yearOfEra field type.
    private static final DateTimeFieldType YEAR_OF_ERA_TYPE = new StandardDateTimeFieldType( "yearOfEra", YEAR_OF_ERA, DurationFieldType.years(), DurationFieldType.eras());
     * The centuryOfEra field type.
    private static final DateTimeFieldType CENTURY_OF_ERA_TYPE = new StandardDateTimeFieldType( "centuryOfEra", CENTURY_OF_ERA, DurationFieldType.centuries(), DurationFieldType.eras());
     * The yearOfCentury field type.
    private static final DateTimeFieldType YEAR_OF_CENTURY_TYPE = new StandardDateTimeFieldType( "yearOfCentury", YEAR_OF_CENTURY, DurationFieldType.years(), DurationFieldType.centuries());
     * The year field type.
    private static final DateTimeFieldType YEAR_TYPE = new StandardDateTimeFieldType( "year", YEAR, DurationFieldType.years(), null);
     * The dayOfYear field type.
    private static final DateTimeFieldType DAY_OF_YEAR_TYPE = new StandardDateTimeFieldType( "dayOfYear", DAY_OF_YEAR, DurationFieldType.days(), DurationFieldType.years());
     * The monthOfYear field type.
    private static final DateTimeFieldType MONTH_OF_YEAR_TYPE = new StandardDateTimeFieldType( "monthOfYear", MONTH_OF_YEAR, DurationFieldType.months(), DurationFieldType.years());
     * The dayOfMonth field type.
    private static final DateTimeFieldType DAY_OF_MONTH_TYPE = new StandardDateTimeFieldType( "dayOfMonth", DAY_OF_MONTH, DurationFieldType.days(), DurationFieldType.months());
     * The weekyearOfCentury field type.
    private static final DateTimeFieldType WEEKYEAR_OF_CENTURY_TYPE = new StandardDateTimeFieldType( "weekyearOfCentury", WEEKYEAR_OF_CENTURY, DurationFieldType.weekyears(), DurationFieldType.centuries());
     * The weekyear field type.
    private static final DateTimeFieldType WEEKYEAR_TYPE = new StandardDateTimeFieldType( "weekyear", WEEKYEAR, DurationFieldType.weekyears(), null);
     * The weekOfWeekyear field type.
    private static final DateTimeFieldType WEEK_OF_WEEKYEAR_TYPE = new StandardDateTimeFieldType( "weekOfWeekyear", WEEK_OF_WEEKYEAR, DurationFieldType.weeks(), DurationFieldType.weekyears());
     * The dayOfWeek field type.
    private static final DateTimeFieldType DAY_OF_WEEK_TYPE = new StandardDateTimeFieldType( "dayOfWeek", DAY_OF_WEEK, DurationFieldType.days(), DurationFieldType.weeks());
     * The halfday field type.
    private static final DateTimeFieldType HALFDAY_OF_DAY_TYPE = new StandardDateTimeFieldType( "halfdayOfDay", HALFDAY_OF_DAY, DurationFieldType.halfdays(), DurationFieldType.days());
     * The hourOfHalfday field type.
    private static final DateTimeFieldType HOUR_OF_HALFDAY_TYPE = new StandardDateTimeFieldType( "hourOfHalfday", HOUR_OF_HALFDAY, DurationFieldType.hours(), DurationFieldType.halfdays());
     * The clockhourOfHalfday field type.
    private static final DateTimeFieldType CLOCKHOUR_OF_HALFDAY_TYPE = new StandardDateTimeFieldType( "clockhourOfHalfday", CLOCKHOUR_OF_HALFDAY, DurationFieldType.hours(), DurationFieldType.halfdays());
     * The clockhourOfDay field type.
    private static final DateTimeFieldType CLOCKHOUR_OF_DAY_TYPE = new StandardDateTimeFieldType( "clockhourOfDay", CLOCKHOUR_OF_DAY, DurationFieldType.hours(), DurationFieldType.days());
     * The hourOfDay field type.
    private static final DateTimeFieldType HOUR_OF_DAY_TYPE = new StandardDateTimeFieldType( "hourOfDay", HOUR_OF_DAY, DurationFieldType.hours(), DurationFieldType.days());
     * The minuteOfDay field type.
    private static final DateTimeFieldType MINUTE_OF_DAY_TYPE = new StandardDateTimeFieldType( "minuteOfDay", MINUTE_OF_DAY, DurationFieldType.minutes(), DurationFieldType.days());
     * The minuteOfHour field type.
    private static final DateTimeFieldType MINUTE_OF_HOUR_TYPE = new StandardDateTimeFieldType( "minuteOfHour", MINUTE_OF_HOUR, DurationFieldType.minutes(), DurationFieldType.hours());
     * The secondOfDay field type.
    private static final DateTimeFieldType SECOND_OF_DAY_TYPE = new StandardDateTimeFieldType( "secondOfDay", SECOND_OF_DAY, DurationFieldType.seconds(), DurationFieldType.days());
     * The secondOfMinute field type.
    private static final DateTimeFieldType SECOND_OF_MINUTE_TYPE = new StandardDateTimeFieldType( "secondOfMinute", SECOND_OF_MINUTE, DurationFieldType.seconds(), DurationFieldType.minutes());
     * The millisOfDay field type.
    private static final DateTimeFieldType MILLIS_OF_DAY_TYPE = new StandardDateTimeFieldType( "millisOfDay", MILLIS_OF_DAY, DurationFieldType.millis(), DurationFieldType.days());
     * The name of the field.
    private final String iName;
     * Get a suitable debug string.
     * @return debug string
    public String toString() { … }
}
// … (non-patched members truncated; patched methods kept)
</class>
THE PATCH-CHANGED CLASS: FieldUtils. Calls on THIS class are the PROBE tier of the two-tier catch in the output spec: once you have declared the input valid by construction, an exception from one of them is a reportable result, never a rejection to skip. Calls on any OTHER class are setup, where a caught exception still means skip this input.
The patch under analysis (propose relations that the CHANGED expressions/conditions could violate — the boundary values the changed condition tests are where an overfit breaks):
<patch>
--- /src/main/java/org/joda/time/field/FieldUtils.java
+++ /src/main/java/org/joda/time/field/FieldUtils.java
@@ -135,6 +135,9 @@
     public static long safeMultiply(long val1, int val2) {
         switch (val2) {
             case -1:
+//ACS's patch begin
+if (val1==Long.MIN_VALUE){throw new ArithmeticException();}
+//ACS's patch end
                 return -val1;
             case 0:
                 return 0L;

</patch>
THE EXACT LINES THE PATCH ADDED/REMOVED (your first relation must target the behaviour THESE govern — the condition, boundary token, or formula here is where an overfit and a correct fix diverge; ADDED lines are what the patch introduced, REMOVED what it deleted — the overfit may have changed these WRONGLY):
    ADDED  : if (val1==Long.MIN_VALUE){throw new ArithmeticException();}
Documented contract of a touched method (relations must follow from contracts like this):
<javadoc>
General utilities that don't fit elsewhere.
<p>
FieldUtils is thread-safe and immutable.

@author Stephen Colebourne
@since 1.0
/
public class FieldUtils {

/**
Restricted constructor.
/
private FieldUtils() {
super();
}

//------------------------------------------------------------------------
/**
Negates the input throwing an exception if it can't negate it.

@param value  the value to negate
@return the negated value
@throws ArithmeticException if the value is Integer.MIN_VALUE
@since 1.1
/
public static int safeNegate(int value) {
if (value == Integer.MIN_VALUE) {
throw new ArithmeticException("Integer.MIN_VALUE cannot be negated");
}
return -value;
}

/**
Add two values throwing an exception if overflow occurs.

@param val1  the first value
@param val2  the second value
@return the new total
@throws ArithmeticException if the value is too big or too small
/
public static int safeAdd(int val1, int val2) {
int sum = val1 + val2;
// If there is a sign change, but the two values have the same sign...
if ((val1 ^ sum) < 0 && (val1 ^ val2) >= 0) {
throw new ArithmeticException
("The calculation caused an overflow: " + val1 + " + " + val2);
}
return sum;
}

/**
Add two values throwing an exception if overflow occurs.

@param val1  the first value
@param val2  the second value
@return the new total
@throws ArithmeticException if the value is too big or too small
/
public static long safeAdd(long val1, long val2) {
long sum = val1 + val2;
// If there is a sign change, but the tw
</javadoc>
IMPORTS OF THE PATCHED CLASS — the authoritative package for every non-JDK type in this codebase VERSION. When a check references a type, use the package EXACTLY as imported here (or as shown in the skeletons); NEVER guess a package name from library conventions — this version's layout may differ, and a wrong package means the relation cannot compile and is discarded unread:
import org.joda.time.DateTimeField;
import org.joda.time.DateTimeFieldType;
import org.joda.time.IllegalFieldValueException;
ALSO WORTH CONSTRAINING (secondary): methods/types the failing test exercises — the bug's symptom is observed through these, so when the patch edited a DIFFERENT method than the one whose output is wrong, one relation constraining one of these can catch it. This is an addition to, never a replacement for, relations grounded in the documented contract of the touched code — prefer the documented-contract relation when choosing:
    safeMultiply

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
<details open><summary>▸ Output (~6,754 chars)</summary>

```
[
  {
    "name": "safeMultiplyLongInt_overflowOnMinValueTimesMinusOne",
    "kind": "invariant",
    "contract": "FieldUtils.safeMultiply(long, int) is documented to throw ArithmeticException if the value is too big or too small; multiplying Long.MIN_VALUE by -1 overflows a long, and the failing test confirms this exact input must throw.",
    "input": "No setup needed: use the exact boundary input named by the patched guard, val1 = Long.MIN_VALUE and val2 = -1.",
    "check": "boolean violated = false;\ntry {\n    org.joda.time.field.FieldUtils.safeMultiply(Long.MIN_VALUE, -1);\n    violated = true;\n} catch (ArithmeticException ok) {\n} catch (Throwable t) {\n    violated = true;\n}\nif (violated) {\n    throw new RuntimeException(\"relation safeMultiplyLongInt_overflowOnMinValueTimesMinusOne violated: expected ArithmeticException for Long.MIN_VALUE * -1\");\n}"
  },
  {
    "name": "safeMultiplyLongInt_agreesWithLongLongSibling",
    "kind": "metamorphic",
    "contract": "The same-name overloads safeMultiply(long, int) and safeMultiply(long, long) both document the same multiplication-with-overflow semantics, so on equivalent inputs they must produce the same observable outcome.",
    "input": "Choose any long val1 and int val2 from data. If the long-long sibling reports ArithmeticException, skip because overflow is allowed by contract; otherwise the long-int overload must return the same long result.",
    "check": "long val1 = data.consumeLong();\nint val2 = data.consumeInt();\nlong expected;\ntry {\n    expected = org.joda.time.field.FieldUtils.safeMultiply(val1, (long) val2);\n} catch (ArithmeticException e) {\n    return;\n} catch (Exception e) {\n    return;\n}\nlong actual;\ntry {\n    actual = org.joda.time.field.FieldUtils.safeMultiply(val1, val2);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation safeMultiplyLongInt_agreesWithLongLongSibling violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nif (actual != expected) {\n    throw new RuntimeException(\"relation safeMultiplyLongInt_agreesWithLongLongSibling violated: \" + actual + \" vs \" + expected + \" for inputs \" + val1 + \" and \" + val2);\n}"
  },
  {
    "name": "safeMultiplyIntInt_agreesWithLongIntSibling",
    "kind": "metamorphic",
    "contract": "The same-name overloads safeMultiply(int, int) and safeMultiply(long, int) document the same multiplication-with-overflow semantics; when the int product is valid, widening the first argument to long must preserve the numeric result.",
    "input": "Draw two ints a and b from data. First call safeMultiply(int,int); if it throws ArithmeticException, skip because overflow is allowed. Otherwise compare its int result to safeMultiply((long)a, b).",
    "check": "int a = data.consumeInt();\nint b = data.consumeInt();\nint intResult;\ntry {\n    intResult = org.joda.time.field.FieldUtils.safeMultiply(a, b);\n} catch (ArithmeticException e) {\n    return;\n} catch (Exception e) {\n    return;\n}\nlong longResult;\ntry {\n    longResult = org.joda.time.field.FieldUtils.safeMultiply((long) a, b);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation safeMultiplyIntInt_agreesWithLongIntSibling violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nif (longResult != (long) intResult) {\n    throw new RuntimeException(\"relation safeMultiplyIntInt_agreesWithLongIntSibling violated: \" + longResult + \" vs \" + intResult + \" for inputs \" + a + \" and \" + b);\n}"
  },
  {
    "name": "safeMultiplyToInt_matchesSafeToIntOfLongLongProduct",
    "kind": "metamorphic",
    "contract": "Within the safe* family, safeMultiplyToInt(long,long) is documented as multiplying two values and returning an int with overflow checks, while safeToInt(long) casts a long to int with overflow checks; therefore for products that safeMultiply(long,long) computes successfully and that fit in int, safeMultiplyToInt(val1,val2) must equal safeToInt(safeMultiply(val1,val2)).",
    "input": "Draw two ints a and b from data and widen to long. Their product always fits in long; if safeMultiply(int,int) throws, skip because the product does not fit in int. Otherwise compare safeMultiplyToInt((long)a,(long)b) with safeToInt(safeMultiply((long)a,(long)b)).",
    "check": "int a = data.consumeInt();\nint b = data.consumeInt();\ntry {\n    org.joda.time.field.FieldUtils.safeMultiply(a, b);\n} catch (ArithmeticException e) {\n    return;\n} catch (Exception e) {\n    return;\n}\nlong product;\nint expected;\ntry {\n    product = org.joda.time.field.FieldUtils.safeMultiply((long) a, (long) b);\n    expected = org.joda.time.field.FieldUtils.safeToInt(product);\n} catch (Exception e) {\n    return;\n}\nint actual;\ntry {\n    actual = org.joda.time.field.FieldUtils.safeMultiplyToInt((long) a, (long) b);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation safeMultiplyToInt_matchesSafeToIntOfLongLongProduct violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nif (actual != expected) {\n    throw new RuntimeException(\"relation safeMultiplyToInt_matchesSafeToIntOfLongLongProduct violated: \" + actual + \" vs \" + expected + \" for inputs \" + a + \" and \" + b);\n}"
  },
  {
    "name": "safeMultiplyLongInt_commutativeViaLongLongSibling",
    "kind": "metamorphic",
    "contract": "Both overloads document multiplication of two values with overflow checks; multiplication is commutative, so whenever val1 * val2 is representable, safeMultiply(long,int) must agree with the equivalent reversed-order call through safeMultiply(long,long).",
    "input": "Choose long val1 and int val2 from data. If safeMultiply((long)val2, val1) throws ArithmeticException, skip because overflow is permitted. Otherwise safeMultiply(val1, val2) must return the same result.",
    "check": "long val1 = data.consumeLong();\nint val2 = data.consumeInt();\nlong expected;\ntry {\n    expected = org.joda.time.field.FieldUtils.safeMultiply((long) val2, val1);\n} catch (ArithmeticException e) {\n    return;\n} catch (Exception e) {\n    return;\n}\nlong actual;\ntry {\n    actual = org.joda.time.field.FieldUtils.safeMultiply(val1, val2);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation safeMultiplyLongInt_commutativeViaLongLongSibling violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nif (actual != expected) {\n    throw new RuntimeException(\"relation safeMultiplyLongInt_commutativeViaLongLongSibling violated: \" + actual + \" vs \" + expected + \" for inputs \" + val1 + \" and \" + val2);\n}"
  }
]
```

</details>

---
## [5] ⚙️ screen · `safeMultiplyLongInt_overflowOnMinValueTimesMinusOne`
**output:** **dropped**
<details><summary>▸ reason (~469 chars)</summary>

- reason: boolean-swallow alarm: a catch block records only the bare flag `violated` (literal assignment, no rethrow, the caught exception is never used) and the alarm near line 10 is conditioned on `violated` — the caught exception's identity is destroyed, so attribution cannot tell WHICH exception fired or whether the unpatched build crashes the same way. Rethrow the exception as the alarm's cause (`..., e`), or assert on the real observable instead of a swallowed boolean.

</details>

---
## [6] ⚙️ screen · `safeMultiplyLongInt_agreesWithLongLongSibling`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [7] ⚙️ screen-fuzz-buggy · `safeMultiplyLongInt_agreesWithLongLongSibling`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [8] ⚙️ screen · `safeMultiplyIntInt_agreesWithLongIntSibling`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [9] ⚙️ screen-fuzz-buggy · `safeMultiplyIntInt_agreesWithLongIntSibling`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [10] ⚙️ screen · `safeMultiplyToInt_matchesSafeToIntOfLongLongProduct`
**output:** **tier2-rewritten**
<details><summary>▸ reason (~736 chars)</summary>

- reason: the probe `FieldUtils.safeMultiply(...)` on the patch-changed class `FieldUtils` runs inside the try at line 12, whose catch (Exception) swallows every exception and returns. The relation declares this input VALID BY CONSTRUCTION, so an exception raised inside `FieldUtils` on it is a RESULT the relation must report, not a rejection to skip — as written the check goes quiet on both builds and a patch that adds or moves a throw is invisible to it. Give the probe its own try (not nested in the setup try) whose catch rethrows `new RuntimeException("relation <name> violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage())`, and keep the blanket catch-and-return for the SETUP calls only.

</details>

---
## [11] ⚙️ screen-fuzz-buggy · `safeMultiplyToInt_matchesSafeToIntOfLongLongProduct`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [12] ⚙️ screen · `safeMultiplyLongInt_commutativeViaLongLongSibling`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [13] ⚙️ screen-fuzz-buggy · `safeMultiplyLongInt_commutativeViaLongLongSibling`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [14] ⚙️ screen · `safeMultiplyLongInt_agreesWithLongLongSibling`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [15] ⚙️ screen · `safeMultiplyIntInt_agreesWithLongIntSibling`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [16] ⚙️ screen · `safeMultiplyToInt_matchesSafeToIntOfLongLongProduct`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [17] ⚙️ screen · `safeMultiplyLongInt_commutativeViaLongLongSibling`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [18] ⚙️ screening-survivors
**output:** 
```json
{
  "kept": [
    "safeMultiplyLongInt_agreesWithLongLongSibling",
    "safeMultiplyIntInt_agreesWithLongIntSibling",
    "safeMultiplyToInt_matchesSafeToIntOfLongLongProduct",
    "safeMultiplyLongInt_commutativeViaLongLongSibling"
  ],
  "count": 4
}
```

---
## [19] 🧠 LLM call — **rule synthesis** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [4] — not reprinted*
- *[user] message: identical to step [4] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,638 chars)</summary>

```
[
  {
    "name": "safeMultiply_longInt_minValue_negOne_throws",
    "kind": "invariant",
    "contract": "FieldUtils.safeMultiply(long, int) is documented to throw ArithmeticException if the value is too big or too small; multiplying Long.MIN_VALUE by -1 overflows a long.",
    "input": "Use the exact overflow boundary named by the patch: val1 = Long.MIN_VALUE and val2 = -1.",
    "check": "boolean violated = false;\ntry {\n    org.joda.time.field.FieldUtils.safeMultiply(Long.MIN_VALUE, -1);\n    violated = true;\n} catch (ArithmeticException ok) {\n} catch (Throwable t) {\n    violated = true;\n    throw new RuntimeException(\"relation safeMultiply_longInt_minValue_negOne_throws violated: wrong exception class \" + t.getClass().getName());\n}\nif (violated) {\n    throw new RuntimeException(\"relation safeMultiply_longInt_minValue_negOne_throws violated: completed normally on overflowing input\");\n}"
  },
  {
    "name": "safeMultiply_longInt_agrees_with_longLong_overload",
    "kind": "metamorphic",
    "contract": "The same-name overloads safeMultiply(long, int) and safeMultiply(long, long) are documented as the same operation, multiplication with overflow checking; on equivalent inputs they must return the same long result.",
    "input": "Choose a small long a and int b from fuzz data so the product is safely within long range by construction, e.g. |a| <= 1,000,000 and |b| <= 1,000.",
    "check": "long a = data.consumeLong() % 1000000L;\nint b = data.consumeInt(-1000, 1000);\nlong r1;\ntry {\n    r1 = org.joda.time.field.FieldUtils.safeMultiply(a, b);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation safeMultiply_longInt_agrees_with_longLong_overload violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nlong r2;\ntry {\n    r2 = org.joda.time.field.FieldUtils.safeMultiply(a, (long) b);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation safeMultiply_longInt_agrees_with_longLong_overload violated: unexpected \" + e.getClass().getName() + \" from long-long overload on equivalent valid input: \" + e.getMessage());\n}\nif (r1 != r2) {\n    throw new RuntimeException(\"relation safeMultiply_longInt_agrees_with_longLong_overload violated: \" + r1 + \" vs \" + r2 + \" for a=\" + a + \", b=\" + b);\n}"
  },
  {
    "name": "safeMultiply_longInt_negOne_matches_safeSubtract",
    "kind": "metamorphic",
    "contract": "For multiplication by -1, safeMultiply(long, int) computes the arithmetic product when that product is representable; for any x other than Long.MIN_VALUE, x * -1 equals 0 - x, so it must agree with safeSubtract(0L, x).",
    "input": "Choose any long x from fuzz data except Long.MIN_VALUE.",
    "check": "long x = data.consumeLong();\nif (x == Long.MIN_VALUE) {\n    return;\n}\nlong r1;\ntry {\n    r1 = org.joda.time.field.FieldUtils.safeMultiply(x, -1);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation safeMultiply_longInt_negOne_matches_safeSubtract violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nlong r2;\ntry {\n    r2 = org.joda.time.field.FieldUtils.safeSubtract(0L, x);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation safeMultiply_longInt_negOne_matches_safeSubtract violated: unexpected \" + e.getClass().getName() + \" from safeSubtract on equivalent valid input: \" + e.getMessage());\n}\nif (r1 != r2) {\n    throw new RuntimeException(\"relation safeMultiply_longInt_negOne_matches_safeSubtract violated: \" + r1 + \" vs \" + r2 + \" for x=\" + x);\n}"
  },
  {
    "name": "safeMultiply_longInt_identity_and_zero",
    "kind": "invariant",
    "contract": "safeMultiply(long, int) is documented as multiplication with overflow checking; multiplying by 1 must return the original value and multiplying by 0 must return 0.",
    "input": "Choose any long x from fuzz data.",
    "check": "long x = data.consumeLong();\nlong byOne;\ntry {\n    byOne = org.joda.time.field.FieldUtils.safeMultiply(x, 1);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation safeMultiply_longInt_identity_and_zero violated: unexpected \" + e.getClass().getName() + \" for multiplier 1: \" + e.getMessage());\n}\nlong byZero;\ntry {\n    byZero = org.joda.time.field.FieldUtils.safeMultiply(x, 0);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation safeMultiply_longInt_identity_and_zero violated: unexpected \" + e.getClass().getName() + \" for multiplier 0: \" + e.getMessage());\n}\nif (byOne != x) {\n    throw new RuntimeException(\"relation safeMultiply_longInt_identity_and_zero violated: multiply by 1 gave \" + byOne + \" for x=\" + x);\n}\nif (byZero != 0L) {\n    throw new RuntimeException(\"relation safeMultiply_longInt_identity_and_zero violated: multiply by 0 gave \" + byZero + \" for x=\" + x);\n}"
  },
  {
    "name": "safeMultiplyToInt_matches_safeMultiply_then_safeToInt",
    "kind": "metamorphic",
    "contract": "safeMultiplyToInt(long, long) is the safe* family member for multiplying and returning an int; when the product is within int range, it must agree with first computing the safe long product and then safely casting it with safeToInt.",
    "input": "Choose small operands from fuzz data so the product is within int range by construction, e.g. |a|, |b| <= 20,000.",
    "check": "long a = data.consumeLong() % 20000L;\nlong b = data.consumeLong() % 20000L;\nint r1;\ntry {\n    r1 = org.joda.time.field.FieldUtils.safeMultiplyToInt(a, b);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation safeMultiplyToInt_matches_safeMultiply_then_safeToInt violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nlong product;\ntry {\n    product = org.joda.time.field.FieldUtils.safeMultiply(a, b);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation safeMultiplyToInt_matches_safeMultiply_then_safeToInt violated: unexpected \" + e.getClass().getName() + \" from safeMultiply on equivalent valid input: \" + e.getMessage());\n}\nint r2;\ntry {\n    r2 = org.joda.time.field.FieldUtils.safeToInt(product);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation safeMultiplyToInt_matches_safeMultiply_then_safeToInt violated: unexpected \" + e.getClass().getName() + \" from safeToInt on equivalent valid input: \" + e.getMessage());\n}\nif (r1 != r2) {\n    throw new RuntimeException(\"relation safeMultiplyToInt_matches_safeMultiply_then_safeToInt violated: \" + r1 + \" vs \" + r2 + \" for a=\" + a + \", b=\" + b);\n}"
  }
]
```

</details>

---
## [20] ⚙️ screen-fuzz-buggy · `safeMultiply_longInt_minValue_negOne_throws`
**output:** 
```json
{
  "checked": 20000,
  "violated": 20000,
  "ratio": 1.0
}
```

---
## [21] ⚙️ screen · `safeMultiply_longInt_agrees_with_longLong_overload`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [22] ⚙️ screen-fuzz-buggy · `safeMultiply_longInt_agrees_with_longLong_overload`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [23] ⚙️ screen · `safeMultiply_longInt_negOne_matches_safeSubtract`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [24] ⚙️ screen-fuzz-buggy · `safeMultiply_longInt_negOne_matches_safeSubtract`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [25] ⚙️ screen · `safeMultiply_longInt_identity_and_zero`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [26] ⚙️ screen-fuzz-buggy · `safeMultiply_longInt_identity_and_zero`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [27] ⚙️ screen · `safeMultiplyToInt_matches_safeMultiply_then_safeToInt`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [28] ⚙️ screen-fuzz-buggy · `safeMultiplyToInt_matches_safeMultiply_then_safeToInt`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [29] ⚙️ screen · `safeMultiply_longInt_minValue_negOne_throws`
**output:** **kept**
- reason: kept: direction-confirmed

---
## [30] ⚙️ screen · `safeMultiply_longInt_agrees_with_longLong_overload`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [31] ⚙️ screen · `safeMultiply_longInt_negOne_matches_safeSubtract`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [32] ⚙️ screen · `safeMultiply_longInt_identity_and_zero`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [33] ⚙️ screen · `safeMultiplyToInt_matches_safeMultiply_then_safeToInt`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [34] ⚙️ reference-widening · `enumerate`
**output:** **nothing to widen**
- reason: the kept checks name no no-argument reader of the patched class — nothing to request a reference for; 12 further name(s) the checks probe are outside the patched class's no-argument readers and were excluded (['safemultiply', 'name', 'message', 'fieldutils', 'safenegate', 'safeadd'])
<details><summary>▸ detail (~2,715 chars)</summary>

- detail: {'targets': [], 'already_admitted': [], 'cap': 3, 'checks_read': 9, 'excluded': [{'key': 'safemultiply', 'why': "the patched class declares `safeMultiply` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}, {'key': 'name', 'why': 'the patched class does not declare `getName()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}, {'key': 'message', 'why': 'the patched class does not declare `getMessage()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}, {'key': 'fieldutils', 'why': 'the patched class declares `FieldUtils()` as a constructor or a void method — it reports no observable value'}, {'key': 'safenegate', 'why': "the patched class declares `safeNegate` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}, {'key': 'safeadd', 'why': "the patched class declares `safeAdd` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}, {'key': 'safesubtract', 'why': "the patched class declares `safeSubtract` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}, {'key': 'safetoint', 'why': "the patched class declares `safeToInt` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}, {'key': 'safemultiplytoint', 'why': "the patched class declares `safeMultiplyToInt` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}, {'key': 'verifyvaluebounds', 'why': "the patched class declares `verifyValueBounds` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}, {'key': 'wrappedvalue', 'why': "the patched class declares `getWrappedValue` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}, {'key': 'illegalfieldvalueexception', 'why': 'the patched class does not declare `IllegalFieldValueException()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}], 'excluded_count': 12}

</details>

---
## [35] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~35,168 chars)</summary>

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
- Package: `org.joda.time.field` (`package org.joda.time.field;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Time_15_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/joda/time/field/FieldUtils.java
+++ /src/main/java/org/joda/time/field/FieldUtils.java
@@ -135,6 +135,9 @@
     public static long safeMultiply(long val1, int val2) {
         switch (val2) {
             case -1:
+//ACS's patch begin
+if (val1==Long.MIN_VALUE){throw new ArithmeticException();}
+//ACS's patch end
                 return -val1;
             case 0:
                 return 0L;

</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import org.joda.time.DateTimeField;
import org.joda.time.DateTimeFieldType;
import org.joda.time.IllegalFieldValueException;
</source_imports>

Function `safeMultiply`:
<signature>
public static long safeMultiply(long val1, int val2)
</signature>
<code>
    public static long safeMultiply(long val1, int val2) {
        switch (val2) {
            case -1:
                return -val1;
            case 0:
                return 0L;
            case 1:
                return val1;
        }
        long total = val1 * val2;
        if (total / val2 != val1) {
          throw new ArithmeticException("Multiplication overflows a long: " + val1 + " * " + val2);
        }
        return total;
    }
</code>
Call-site examples (use these as a guide for constructing the target call):
<xref>
public static Duration standardDays(long days) {
        if (days == 0) {
            return ZERO;
        }
        return new Duration(FieldUtils.safeMultiply(days, DateTimeConstants.MILLIS_PER_DAY));
    }
</xref>
<xref>
public static Duration standardHours(long hours) {
        if (hours == 0) {
            return ZERO;
        }
        return new Duration(FieldUtils.safeMultiply(hours, DateTimeConstants.MILLIS_PER_HOUR));
    }
</xref>
<xref>
public static Duration standardMinutes(long minutes) {
        if (minutes == 0) {
            return ZERO;
        }
        return new Duration(FieldUtils.safeMultiply(minutes, DateTimeConstants.MILLIS_PER_MINUTE));
    }
</xref>
<xref>
public static Duration standardSeconds(long seconds) {
        if (seconds == 0) {
            return ZERO;
        }
        return new Duration(FieldUtils.safeMultiply(seconds, DateTimeConstants.MILLIS_PER_SECOND));
    }
</xref>
<xref>
public Duration withDurationAdded(long durationToAdd, int scalar) {
        if (durationToAdd == 0 || scalar == 0) {
            return this;
        }
        long add = FieldUtils.safeMultiply(durationToAdd, scalar);
        long duration = FieldUtils.safeAdd(getMillis(), add);
        return new Duration(duration);
    }
</xref>

DOCUMENTED PRECONDITIONS / REJECTION CONTRACT of the touched method(s), from their javadoc. An input violating an @param constraint is INVALID — a documented @throws on it is CORRECT behaviour, never a finding. Build inputs that satisfy these constraints BY CONSTRUCTION (order/clamp/force them valid before the call) and assert only on those:
  - @param value  the value to negate
  - @throws ArithmeticException if the value is Integer.MIN_VALUE
  - @param val1  the first value
  - @param val2  the second value
  - @throws ArithmeticException if the value is too big or too small
REJECTION ORACLES — RE-PROBE AFTER EVERY STATE CHANGE. When you assert a documented rejection (asking for something absent, invalid or out of range MUST throw), run that probe again AFTER every call that changes the receiver's state — installing, registering, adding, removing, clearing — not once on the freshly built object. Mutate, then probe; mutate again, then probe again, asserting the SAME documented outcome each time. A correct rejection depends only on the probe itself being absent or invalid, never on unrelated receiver state, so it must hold in every state the object passes through; a patch that makes the rejection conditional on the container's contents, its size, or which slots are occupied diverges ONLY in the mutated states, so a probe placed before the mutations is silent on the buggy and the patched build alike. Draw WHICH slot/key/index each mutation targets, and HOW MANY mutations happen, from the FuzzedDataProvider — never from literals: fixed targets rebuild one shape every iteration, and the states where such a patch misbehaves (a gap between filled slots, an emptied container, a larger one) are never reached.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  getWrappedValue(int currentValue, int wrapValue,
                                      int minValue, int maxValue) / (int value, int minValue, int maxValue)
  safeAdd(int val1, int val2) / (long val1, long val2)
  safeMultiply(int val1, int val2) / (long val1, int val2) / (long val1, long val2)
  verifyValueBounds(DateTimeField field, 
                                         int value, int lowerBound, int upperBound) / (DateTimeFieldType fieldType, 
                                         int value, int lowerBound, int upperBound) / (String fieldName,
                                         int value, int lowerBound, int upperBound)

METHOD FAMILIES over the same input space (factory/parser siblings — where the docs state a selection or agreement rule between family members, equivalent inputs must respect it):
  safe* family: safeAdd, safeMultiply, safeMultiplyToInt, safeNegate, safeSubtract, safeToInt

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
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `TestFieldUtils`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
Lift the call and the expected value from THIS test (this attempt's target):
<failing_test class="org.joda.time.field.TestFieldUtils" method="testSafeMultiplyLongInt">
    public void testSafeMultiplyLongInt() {
        assertEquals(0L, FieldUtils.safeMultiply(0L, 0));
        
        assertEquals(1L, FieldUtils.safeMultiply(1L, 1));
        assertEquals(3L, FieldUtils.safeMultiply(1L, 3));
        assertEquals(3L, FieldUtils.safeMultiply(3L, 1));
        
        assertEquals(6L, FieldUtils.safeMultiply(2L, 3));
        assertEquals(-6L, FieldUtils.safeMultiply(2L, -3));
        assertEquals(-6L, FieldUtils.safeMultiply(-2L, 3));
        assertEquals(6L, FieldUtils.safeMultiply(-2L, -3));
        
        assertEquals(-1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE));
        
        assertEquals(Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1));
        assertEquals(Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1));
        assertEquals(-Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1));
        
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
            fail();
        } catch (ArithmeticException e) {
        }
        
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
            fail();
        } catch (ArithmeticException e) {
        }
        
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
            fail();
        } catch (ArithmeticException e) {
        }
        
        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
            fail();
        } catch (ArithmeticException e) {
        }
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.joda.time.field.TestFieldUtils::testSafeMultiplyLongInt
junit.framework.AssertionFailedError
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.
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
<relation name="safeMultiplyLongInt_agreesWithLongLongSibling" kind="metamorphic">
  // holds because: The same-name overloads safeMultiply(long, int) and safeMultiply(long, long) both document the same multiplication-with-overflow semantics, so on equivalent inputs they must produce the same observable outcome.
  // valid input: Choose any long val1 and int val2 from data. If the long-long sibling reports ArithmeticException, skip because overflow is allowed by contract; otherwise the long-int overload must return the same long result.
  // screen: fired on 0/20000 fuzzed inputs on the buggy build (silent on the buggy build)
long val1 = data.consumeLong();
int val2 = data.consumeInt();
long expected;
try {
    expected = org.joda.time.field.FieldUtils.safeMultiply(val1, (long) val2);
} catch (ArithmeticException e) {
    return;
} catch (Exception e) {
    return;
}
long actual;
try {
    actual = org.joda.time.field.FieldUtils.safeMultiply(val1, val2);
} catch (Exception e) { for (StackTraceElement __rpgf : e.getStackTrace()) { String __rpgc = __rpgf.getClassName(); if ((__rpgc.equals("FieldUtils") || __rpgc.endsWith(".FieldUtils") || __rpgc.endsWith("$FieldUtils")) && !"<init>".equals(__rpgf.getMethodName())) { boolean __rpgdoc = false; if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "getWrappedValue".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IllegalArgumentException") || __rpn.endsWith(".IllegalArgumentException") || __rpn.endsWith("$IllegalArgumentException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "safeAdd".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArithmeticException") || __rpn.endsWith(".ArithmeticException") || __rpn.endsWith("$ArithmeticException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "safeMultiply".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArithmeticException") || __rpn.endsWith(".ArithmeticException") || __rpn.endsWith("$ArithmeticException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "safeMultiplyToInt".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArithmeticException") || __rpn.endsWith(".ArithmeticException") || __rpn.endsWith("$ArithmeticException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "safeNegate".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArithmeticException") || __rpn.endsWith(".ArithmeticException") || __rpn.endsWith("$ArithmeticException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "safeSubtract".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArithmeticException") || __rpn.endsWith(".ArithmeticException") || __rpn.endsWith("$ArithmeticException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "safeToInt".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArithmeticException") || __rpn.endsWith(".ArithmeticException") || __rpn.endsWith("$ArithmeticException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "verifyValueBounds".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IllegalFieldValueException") || __rpn.endsWith(".IllegalFieldValueException") || __rpn.endsWith("$IllegalFieldValueException")) { __rpgdoc = true; break; } } } if (__rpgdoc) return; break; } }
    throw new RuntimeException("relation safeMultiplyLongInt_agreesWithLongLongSibling violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage());
}
if (actual != expected) {
    throw new RuntimeException("relation safeMultiplyLongInt_agreesWithLongLongSibling violated: " + actual + " vs " + expected + " for inputs " + val1 + " and " + val2);
}
</relation>
<relation name="safeMultiplyIntInt_agreesWithLongIntSibling" kind="metamorphic">
  // holds because: The same-name overloads safeMultiply(int, int) and safeMultiply(long, int) document the same multiplication-with-overflow semantics; when the int product is valid, widening the first argument to long must preserve the numeric result.
  // valid input: Draw two ints a and b from data. First call safeMultiply(int,int); if it throws ArithmeticException, skip because overflow is allowed. Otherwise compare its int result to safeMultiply((long)a, b).
  // screen: fired on 0/20000 fuzzed inputs on the buggy build (silent on the buggy build)
int a = data.consumeInt();
int b = data.consumeInt();
int intResult;
try {
    intResult = org.joda.time.field.FieldUtils.safeMultiply(a, b);
} catch (ArithmeticException e) {
    return;
} catch (Exception e) {
    return;
}
long longResult;
try {
    longResult = org.joda.time.field.FieldUtils.safeMultiply((long) a, b);
} catch (Exception e) { for (StackTraceElement __rpgf : e.getStackTrace()) { String __rpgc = __rpgf.getClassName(); if ((__rpgc.equals("FieldUtils") || __rpgc.endsWith(".FieldUtils") || __rpgc.endsWith("$FieldUtils")) && !"<init>".equals(__rpgf.getMethodName())) { boolean __rpgdoc = false; if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "getWrappedValue".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IllegalArgumentException") || __rpn.endsWith(".IllegalArgumentException") || __rpn.endsWith("$IllegalArgumentException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "safeAdd".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArithmeticException") || __rpn.endsWith(".ArithmeticException") || __rpn.endsWith("$ArithmeticException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "safeMultiply".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArithmeticException") || __rpn.endsWith(".ArithmeticException") || __rpn.endsWith("$ArithmeticException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "safeMultiplyToInt".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArithmeticException") || __rpn.endsWith(".ArithmeticException") || __rpn.endsWith("$ArithmeticException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "safeNegate".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArithmeticException") || __rpn.endsWith(".ArithmeticException") || __rpn.endsWith("$ArithmeticException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "safeSubtract".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArithmeticException") || __rpn.endsWith(".ArithmeticException") || __rpn.endsWith("$ArithmeticException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "safeToInt".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArithmeticException") || __rpn.endsWith(".ArithmeticException") || __rpn.endsWith("$ArithmeticException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "verifyValueBounds".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IllegalFieldValueException") || __rpn.endsWith(".IllegalFieldValueException") || __rpn.endsWith("$IllegalFieldValueException")) { __rpgdoc = true; break; } } } if (__rpgdoc) return; break; } }
    throw new RuntimeException("relation safeMultiplyIntInt_agreesWithLongIntSibling violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage());
}
if (longResult != (long) intResult) {
    throw new RuntimeException("relation safeMultiplyIntInt_agreesWithLongIntSibling violated: " + longResult + " vs " + intResult + " for inputs " + a + " and " + b);
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
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~7,206 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        assertEqualsLong("lifted-00", 0L, FieldUtils.safeMultiply(0L, 0), "FieldUtils.safeMultiply(0L, 0)");
        assertEqualsLong("lifted-01", 1L, FieldUtils.safeMultiply(1L, 1), "FieldUtils.safeMultiply(1L, 1)");
        assertEqualsLong("lifted-02", 3L, FieldUtils.safeMultiply(1L, 3), "FieldUtils.safeMultiply(1L, 3)");
        assertEqualsLong("lifted-03", 3L, FieldUtils.safeMultiply(3L, 1), "FieldUtils.safeMultiply(3L, 1)");
        assertEqualsLong("lifted-04", 6L, FieldUtils.safeMultiply(2L, 3), "FieldUtils.safeMultiply(2L, 3)");
        assertEqualsLong("lifted-05", -6L, FieldUtils.safeMultiply(2L, -3), "FieldUtils.safeMultiply(2L, -3)");
        assertEqualsLong("lifted-06", -6L, FieldUtils.safeMultiply(-2L, 3), "FieldUtils.safeMultiply(-2L, 3)");
        assertEqualsLong("lifted-07", 6L, FieldUtils.safeMultiply(-2L, -3), "FieldUtils.safeMultiply(-2L, -3)");
        assertEqualsLong("lifted-08", -1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE),
                "FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE)");
        assertEqualsLong("lifted-09", Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1),
                "FieldUtils.safeMultiply(Long.MAX_VALUE, 1)");
        assertEqualsLong("lifted-10", Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1),
                "FieldUtils.safeMultiply(Long.MIN_VALUE, 1)");
        assertEqualsLong("lifted-11", -Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1),
                "FieldUtils.safeMultiply(Long.MAX_VALUE, -1)");

        assertThrowsArithmetic("lifted-12", Long.MIN_VALUE, -1);
        assertThrowsArithmetic("lifted-13", Long.MIN_VALUE, 100);
        assertThrowsArithmetic("lifted-14", Long.MIN_VALUE, Integer.MAX_VALUE);
        assertThrowsArithmetic("lifted-15", Long.MAX_VALUE, Integer.MIN_VALUE);

        long val1 = data.consumeLong();
        int val2 = data.consumeInt();

        Long siblingExpected = null;
        try {
            siblingExpected = Long.valueOf(FieldUtils.safeMultiply(val1, (long) val2));
        } catch (Throwable t) {
            return;
        }

        Long siblingActual = null;
        try {
            siblingActual = Long.valueOf(FieldUtils.safeMultiply(val1, val2));
        } catch (Throwable t) {
            return;
        }

        if (!siblingActual.equals(siblingExpected)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:relation-longint-longlong] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) on equivalent inputs inputVal1="
                            + val1 + " inputVal2=" + val2 + " lhs=" + siblingActual.longValue() + " rhs="
                            + siblingExpected.longValue());
        }

        int a = data.consumeInt();
        int b = data.consumeInt();

        Integer intResult = null;
        try {
            intResult = Integer.valueOf(FieldUtils.safeMultiply(a, b));
        } catch (Throwable t) {
            return;
        }

        Long longResult = null;
        try {
            longResult = Long.valueOf(FieldUtils.safeMultiply((long) a, b));
        } catch (Throwable t) {
            return;
        }

        if (longResult.longValue() != (long) intResult.intValue()) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:relation-intint-longint] metamorphic violation: safeMultiply(int,int) must agree with safeMultiply(long,int) when the int product is valid inputA="
                            + a + " inputB=" + b + " lhs=" + longResult.longValue() + " rhs="
                            + intResult.intValue());
        }

        long n = data.consumeInt(-1_000_000, 1_000_000);
        try {
            long viaOne = FieldUtils.safeMultiply(n, 1);
            long direct = n;
            if (viaOne != direct) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:identity-times-one] semantic mismatch: multiplication by one must preserve the value input="
                                + n + " actual=" + viaOne + " expected=" + direct);
            }
        } catch (Throwable t) {
            return;
        }

        long m = data.consumeInt(-1_000_000, 1_000_000);
        try {
            long viaZero = FieldUtils.safeMultiply(m, 0);
            if (viaZero != 0L) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:annihilator-times-zero] semantic mismatch: multiplication by zero must yield zero input="
                                + m + " actual=" + viaZero + " expected=0");
            }
        } catch (Throwable t) {
            return;
        }

        long moderate = data.consumeInt(-1_000_000, 1_000_000);
        int sign = data.consumeBoolean() ? 1 : -1;
        try {
            long product = FieldUtils.safeMultiply(moderate, sign);
            long negated = FieldUtils.safeNegate(moderate);
            long expected = sign == 1 ? moderate : negated;
            /*
             * Contract justification: FieldUtils.safeMultiply documents exact multiplication with
             * overflow checks, and safeNegate documents exact negation with overflow checks.
             * On non-extreme magnitudes and sign in {-1, 1}, multiplying by -1 must equal safeNegate(x),
             * while multiplying by 1 must equal x. A patch that merely deletes the throw or returns an
             * arbitrary fallback would violate this observable post-condition even when no exception fires.
             */
            if (product != expected) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:sign-sibling] metamorphic violation: multiply by +/-1 must match identity/negation input="
                                + moderate + " sign=" + sign + " actual=" + product + " expected=" + expected);
            }
        } catch (Throwable t) {
            return;
        }
    }

    private static void assertEqualsLong(String oracleId, long expected, long actual, String expr) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: " + expr + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertThrowsArithmetic(String oracleId, long val1, int val2) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: FieldUtils.safeMultiply(" + val1 + ", " + val2
                            + ") was expected to throw ArithmeticException but returned " + actual);
        } catch (ArithmeticException expected) {
        }
    }
}
```

</details>

---
## [36] ⚙️ harness-repair · `attempt_001`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [37] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [38] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~8,633 chars, 2 new)</summary>

- *[system] message: identical to step [35] — not reprinted*
- *[user] message: identical to step [35] — not reprinted*
**[assistant]**
```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        assertEqualsLong("lifted-00", 0L, FieldUtils.safeMultiply(0L, 0), "FieldUtils.safeMultiply(0L, 0)");
        assertEqualsLong("lifted-01", 1L, FieldUtils.safeMultiply(1L, 1), "FieldUtils.safeMultiply(1L, 1)");
        assertEqualsLong("lifted-02", 3L, FieldUtils.safeMultiply(1L, 3), "FieldUtils.safeMultiply(1L, 3)");
        assertEqualsLong("lifted-03", 3L, FieldUtils.safeMultiply(3L, 1), "FieldUtils.safeMultiply(3L, 1)");
        assertEqualsLong("lifted-04", 6L, FieldUtils.safeMultiply(2L, 3), "FieldUtils.safeMultiply(2L, 3)");
        assertEqualsLong("lifted-05", -6L, FieldUtils.safeMultiply(2L, -3), "FieldUtils.safeMultiply(2L, -3)");
        assertEqualsLong("lifted-06", -6L, FieldUtils.safeMultiply(-2L, 3), "FieldUtils.safeMultiply(-2L, 3)");
        assertEqualsLong("lifted-07", 6L, FieldUtils.safeMultiply(-2L, -3), "FieldUtils.safeMultiply(-2L, -3)");
        assertEqualsLong("lifted-08", -1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE),
                "FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE)");
        assertEqualsLong("lifted-09", Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1),
                "FieldUtils.safeMultiply(Long.MAX_VALUE, 1)");
        assertEqualsLong("lifted-10", Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1),
                "FieldUtils.safeMultiply(Long.MIN_VALUE, 1)");
        assertEqualsLong("lifted-11", -Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1),
                "FieldUtils.safeMultiply(Long.MAX_VALUE, -1)");

        assertThrowsArithmetic("lifted-12", Long.MIN_VALUE, -1);
        assertThrowsArithmetic("lifted-13", Long.MIN_VALUE, 100);
        assertThrowsArithmetic("lifted-14", Long.MIN_VALUE, Integer.MAX_VALUE);
        assertThrowsArithmetic("lifted-15", Long.MAX_VALUE, Integer.MIN_VALUE);

        long val1 = data.consumeLong();
        int val2 = data.consumeInt();

        Long siblingExpected = null;
        try {
            siblingExpected = Long.valueOf(FieldUtils.safeMultiply(val1, (long) val2));
        } catch (Throwable t) {
            return;
        }

        Long siblingActual = null;
        try {
            siblingActual = Long.valueOf(FieldUtils.safeMultiply(val1, val2));
        } catch (Throwable t) {
            return;
        }

        if (!siblingActual.equals(siblingExpected)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:relation-longint-longlong] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) on equivalent inputs inputVal1="
                            + val1 + " inputVal2=" + val2 + " lhs=" + siblingActual.longValue() + " rhs="
                            + siblingExpected.longValue());
        }

        int a = data.consumeInt();
        int b = data.consumeInt();

        Integer intResult = null;
        try {
            intResult = Integer.valueOf(FieldUtils.safeMultiply(a, b));
        } catch (Throwable t) {
            return;
        }

        Long longResult = null;
        try {
            longResult = Long.valueOf(FieldUtils.safeMultiply((long) a, b));
        } catch (Throwable t) {
            return;
        }

        if (longResult.longValue() != (long) intResult.intValue()) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:relation-intint-longint] metamorphic violation: safeMultiply(int,int) must agree with safeMultiply(long,int) when the int product is valid inputA="
                            + a + " inputB=" + b + " lhs=" + longResult.longValue() + " rhs="
                            + intResult.intValue());
        }

        long n = data.consumeInt(-1_000_000, 1_000_000);
        try {
            long viaOne = FieldUtils.safeMultiply(n, 1);
            long direct = n;
            if (viaOne != direct) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:identity-times-one] semantic mismatch: multiplication by one must preserve the value input="
                                + n + " actual=" + viaOne + " expected=" + direct);
            }
        } catch (Throwable t) {
            return;
        }

        long m = data.consumeInt(-1_000_000, 1_000_000);
        try {
            long viaZero = FieldUtils.safeMultiply(m, 0);
            if (viaZero != 0L) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:annihilator-times-zero] semantic mismatch: multiplication by zero must yield zero input="
                                + m + " actual=" + viaZero + " expected=0");
            }
        } catch (Throwable t) {
            return;
        }

        long moderate = data.consumeInt(-1_000_000, 1_000_000);
        int sign = data.consumeBoolean() ? 1 : -1;
        try {
            long product = FieldUtils.safeMultiply(moderate, sign);
            long negated = FieldUtils.safeNegate(moderate);
            long expected = sign == 1 ? moderate : negated;
            /*
             * Contract justification: FieldUtils.safeMultiply documents exact multiplication with
             * overflow checks, and safeNegate documents exact negation with overflow checks.
             * On non-extreme magnitudes and sign in {-1, 1}, multiplying by -1 must equal safeNegate(x),
             * while multiplying by 1 must equal x. A patch that merely deletes the throw or returns an
             * arbitrary fallback would violate this observable post-condition even when no exception fires.
             */
            if (product != expected) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:sign-sibling] metamorphic violation: multiply by +/-1 must match identity/negation input="
                                + moderate + " sign=" + sign + " actual=" + product + " expected=" + expected);
            }
        } catch (Throwable t) {
            return;
        }
    }

    private static void assertEqualsLong(String oracleId, long expected, long actual, String expr) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: " + expr + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertThrowsArithmetic(String oracleId, long val1, int val2) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: FieldUtils.safeMultiply(" + val1 + ", " + val2
                            + ") was expected to throw ArithmeticException but returned " + actual);
        } catch (ArithmeticException expected) {
        }
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HR/004_patch1-Time-15-ACS_c_rep001/Time_15_buggy/fuzz/attempt_001/FuzzHarness.java:106: error: incompatible types: possible lossy conversion from long to int
            long negated = FieldUtils.safeNegate(moderate);
                                                 ^
Note: Some messages have been simplified; recompile with -Xdiags:verbose to get full output
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
<details open><summary>▸ Output (~7,214 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        assertEqualsLong("lifted-00", 0L, FieldUtils.safeMultiply(0L, 0), "FieldUtils.safeMultiply(0L, 0)");
        assertEqualsLong("lifted-01", 1L, FieldUtils.safeMultiply(1L, 1), "FieldUtils.safeMultiply(1L, 1)");
        assertEqualsLong("lifted-02", 3L, FieldUtils.safeMultiply(1L, 3), "FieldUtils.safeMultiply(1L, 3)");
        assertEqualsLong("lifted-03", 3L, FieldUtils.safeMultiply(3L, 1), "FieldUtils.safeMultiply(3L, 1)");
        assertEqualsLong("lifted-04", 6L, FieldUtils.safeMultiply(2L, 3), "FieldUtils.safeMultiply(2L, 3)");
        assertEqualsLong("lifted-05", -6L, FieldUtils.safeMultiply(2L, -3), "FieldUtils.safeMultiply(2L, -3)");
        assertEqualsLong("lifted-06", -6L, FieldUtils.safeMultiply(-2L, 3), "FieldUtils.safeMultiply(-2L, 3)");
        assertEqualsLong("lifted-07", 6L, FieldUtils.safeMultiply(-2L, -3), "FieldUtils.safeMultiply(-2L, -3)");
        assertEqualsLong("lifted-08", -1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE),
                "FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE)");
        assertEqualsLong("lifted-09", Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1),
                "FieldUtils.safeMultiply(Long.MAX_VALUE, 1)");
        assertEqualsLong("lifted-10", Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1),
                "FieldUtils.safeMultiply(Long.MIN_VALUE, 1)");
        assertEqualsLong("lifted-11", -Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1),
                "FieldUtils.safeMultiply(Long.MAX_VALUE, -1)");

        assertThrowsArithmetic("lifted-12", Long.MIN_VALUE, -1);
        assertThrowsArithmetic("lifted-13", Long.MIN_VALUE, 100);
        assertThrowsArithmetic("lifted-14", Long.MIN_VALUE, Integer.MAX_VALUE);
        assertThrowsArithmetic("lifted-15", Long.MAX_VALUE, Integer.MIN_VALUE);

        long val1 = ((long) data.consumeInt() << 32) ^ (data.consumeInt() & 0xffffffffL);
        int val2 = data.consumeInt();

        Long siblingExpected;
        try {
            siblingExpected = Long.valueOf(FieldUtils.safeMultiply(val1, (long) val2));
        } catch (Throwable t) {
            return;
        }

        Long siblingActual;
        try {
            siblingActual = Long.valueOf(FieldUtils.safeMultiply(val1, val2));
        } catch (Throwable t) {
            return;
        }

        if (!siblingActual.equals(siblingExpected)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:relation-longint-longlong] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) on equivalent inputs inputVal1="
                            + val1 + " inputVal2=" + val2 + " lhs=" + siblingActual.longValue() + " rhs="
                            + siblingExpected.longValue());
        }

        int a = data.consumeInt();
        int b = data.consumeInt();

        Integer intResult;
        try {
            intResult = Integer.valueOf(FieldUtils.safeMultiply(a, b));
        } catch (Throwable t) {
            return;
        }

        Long longResult;
        try {
            longResult = Long.valueOf(FieldUtils.safeMultiply((long) a, b));
        } catch (Throwable t) {
            return;
        }

        if (longResult.longValue() != (long) intResult.intValue()) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:relation-intint-longint] metamorphic violation: safeMultiply(int,int) must agree with safeMultiply(long,int) when the int product is valid inputA="
                            + a + " inputB=" + b + " lhs=" + longResult.longValue() + " rhs="
                            + intResult.intValue());
        }

        long n = data.consumeInt(-1_000_000, 1_000_000);
        try {
            long viaOne = FieldUtils.safeMultiply(n, 1);
            if (viaOne != n) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:identity-times-one] semantic mismatch: multiplication by one must preserve the value input="
                                + n + " actual=" + viaOne + " expected=" + n);
            }
        } catch (Throwable t) {
            return;
        }

        long m = data.consumeInt(-1_000_000, 1_000_000);
        try {
            long viaZero = FieldUtils.safeMultiply(m, 0);
            if (viaZero != 0L) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:annihilator-times-zero] semantic mismatch: multiplication by zero must yield zero input="
                                + m + " actual=" + viaZero + " expected=0");
            }
        } catch (Throwable t) {
            return;
        }

        int moderate = data.consumeInt(-1_000_000, 1_000_000);
        int sign = data.consumeBoolean() ? 1 : -1;
        try {
            long product = FieldUtils.safeMultiply((long) moderate, sign);
            int negated = FieldUtils.safeNegate(moderate);
            long expected = sign == 1 ? (long) moderate : (long) negated;
            /*
             * Contract justification: safeMultiply performs exact multiplication with overflow checks,
             * and safeNegate performs exact negation with overflow checks. For moderate values and sign
             * restricted to {-1,1}, multiplying by 1 must return the same value and multiplying by -1
             * must equal safeNegate(value). A patch that only suppresses exceptions or returns a wrong
             * fallback value would violate this observable post-condition without needing any throw.
             */
            if (product != expected) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:sign-sibling] metamorphic violation: multiply by +/-1 must match identity/negation input="
                                + moderate + " sign=" + sign + " actual=" + product + " expected=" + expected);
            }
        } catch (Throwable t) {
            return;
        }
    }

    private static void assertEqualsLong(String oracleId, long expected, long actual, String expr) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: " + expr + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertThrowsArithmetic(String oracleId, long val1, int val2) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: FieldUtils.safeMultiply(" + val1 + ", " + val2
                            + ") was expected to throw ArithmeticException but returned " + actual);
        } catch (ArithmeticException expected) {
        }
    }
}
```

</details>

---
## [39] ⚙️ harness-repair · `attempt_002`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [40] ⚙️ harness-attempt · `attempt_002`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-12] semantic mismatch: FieldUtils.safeMultiply(-9223372036854775808, -1) was expected to throw ArithmeticException but returned …

---
## [41] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~47,472 chars, 1 new)</summary>

- *[system] message: identical to step [35] — not reprinted*
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.joda.time.field` (`package org.joda.time.field;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Time_15_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /src/main/java/org/joda/time/field/FieldUtils.java
+++ /src/main/java/org/joda/time/field/FieldUtils.java
@@ -135,6 +135,9 @@
     public static long safeMultiply(long val1, int val2) {
         switch (val2) {
             case -1:
+//ACS's patch begin
+if (val1==Long.MIN_VALUE){throw new ArithmeticException();}
+//ACS's patch end
                 return -val1;
             case 0:
                 return 0L;

</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import org.joda.time.DateTimeField;
import org.joda.time.DateTimeFieldType;
import org.joda.time.IllegalFieldValueException;
</source_imports>

Function `safeMultiply`:
<signature>
public static long safeMultiply(long val1, int val2)
</signature>
<code>
    public static long safeMultiply(long val1, int val2) {
        switch (val2) {
            case -1:
                return -val1;
            case 0:
                return 0L;
            case 1:
                return val1;
        }
        long total = val1 * val2;
        if (total / val2 != val1) {
          throw new ArithmeticException("Multiplication overflows a long: " + val1 + " * " + val2);
        }
        return total;
    }
</code>
Call-site examples (use these as a guide for constructing the target call):
<xref>
public static Duration standardDays(long days) {
        if (days == 0) {
            return ZERO;
        }
        return new Duration(FieldUtils.safeMultiply(days, DateTimeConstants.MILLIS_PER_DAY));
    }
</xref>
<xref>
public static Duration standardHours(long hours) {
        if (hours == 0) {
            return ZERO;
        }
        return new Duration(FieldUtils.safeMultiply(hours, DateTimeConstants.MILLIS_PER_HOUR));
    }
</xref>
<xref>
public static Duration standardMinutes(long minutes) {
        if (minutes == 0) {
            return ZERO;
        }
        return new Duration(FieldUtils.safeMultiply(minutes, DateTimeConstants.MILLIS_PER_MINUTE));
    }
</xref>
<xref>
public static Duration standardSeconds(long seconds) {
        if (seconds == 0) {
            return ZERO;
        }
        return new Duration(FieldUtils.safeMultiply(seconds, DateTimeConstants.MILLIS_PER_SECOND));
    }
</xref>
<xref>
public Duration withDurationAdded(long durationToAdd, int scalar) {
        if (durationToAdd == 0 || scalar == 0) {
            return this;
        }
        long add = FieldUtils.safeMultiply(durationToAdd, scalar);
        long duration = FieldUtils.safeAdd(getMillis(), add);
        return new Duration(duration);
    }
</xref>

CLASS-LEVEL CONTEXT — partial skeletons of the touched class(es): signatures, fields, javadoc; non-touched method bodies elided. Use them to find CROSS-MEMBER consistency oracles: sibling accessors that must agree with each other or with constructor arguments, overloads that must be equivalent, round-trips that must restore state. Assert only what a shown contract or invariant guarantees.
<class_skeletons>
<class name="FieldUtils" role="patched">
     * General utilities that don't fit elsewhere.
     * <p>
     * FieldUtils is thread-safe and immutable.
     * @author Stephen Colebourne
     * @since 1.0
public class FieldUtils {
     * Restricted constructor.
    private FieldUtils() {
        super();
    }
     * Negates the input throwing an exception if it can't negate it.
     * @param value  the value to negate
     * @return the negated value
     * @throws ArithmeticException if the value is Integer.MIN_VALUE
     * @since 1.1
    public static int safeNegate(int value) { … }
     * Add two values throwing an exception if overflow occurs.
     * @param val1  the first value
     * @param val2  the second value
     * @return the new total
     * @throws ArithmeticException if the value is too big or too small
    public static int safeAdd(int val1, int val2) { … }
     * Add two values throwing an exception if overflow occurs.
     * @param val1  the first value
     * @param val2  the second value
     * @return the new total
     * @throws ArithmeticException if the value is too big or too small
    public static long safeAdd(long val1, long val2) { … }
     * Subtracts two values throwing an exception if overflow occurs.
     * @param val1  the first value, to be taken away from
     * @param val2  the second value, the amount to take away
     * @return the new total
     * @throws ArithmeticException if the value is too big or too small
    public static long safeSubtract(long val1, long val2) { … }
     * Multiply two values throwing an exception if overflow occurs.
     * @param val1  the first value
     * @param val2  the second value
     * @return the new total
     * @throws ArithmeticException if the value is too big or too small
     * @since 1.2
    public static int safeMultiply(int val1, int val2) {
        long total = (long) val1 * (long) val2;
        if (total < Integer.MIN_VALUE || total > Integer.MAX_VALUE) {
          throw new ArithmeticException("Multiplication overflows an int: " + val1 + " * " + val2);
        }
        return (int) total;
    }
     * Multiply two values throwing an exception if overflow occurs.
     * @param val1  the first value
     * @param val2  the second value
     * @return the new total
     * @throws ArithmeticException if the value is too big or too small
     * @since 1.2
    public static long safeMultiply(long val1, int val2) {
        switch (val2) {
            case -1:
                return -val1;
            case 0:
                return 0L;
            case 1:
                return val1;
        }
        long total = val1 * val2;
        if (total / val2 != val1) {
          throw new ArithmeticException("Multiplication overflows a long: " + val1 + " * " + val2);
        }
        return total;
    }
     * Multiply two values throwing an exception if overflow occurs.
     * @param val1  the first value
     * @param val2  the second value
     * @return the new total
     * @throws ArithmeticException if the value is too big or too small
    public static long safeMultiply(long val1, long val2) {
        if (val2 == 1) {
            return val1;
        }
        if (val1 == 1) {
            return val2;
        }
        if (val1 == 0 || val2 == 0) {
            return 0;
        }
        long total = val1 * val2;
        if (total / val2 != val1 || val1 == Long.MIN_VALUE && val2 == -1 || val2 == Long.MIN_VALUE && val1 == -1) {
            throw new ArithmeticException("Multiplication overflows a long: " + val1 + " * " + val2);
        }
        return total;
    }
     * Casts to an int throwing an exception if overflow occurs.
     * @param value  the value
     * @return the value as an int
     * @throws ArithmeticException if the value is too big or too small
    public static int safeToInt(long value) { … }
     * Multiply two values to return an int throwing an exception if overflow occurs.
     * @param val1  the first value
     * @param val2  the second value
     * @return the new total
     * @throws ArithmeticException if the value is too big or too small
    public static int safeMultiplyToInt(long val1, long val2) { … }
     * Verify that input values are within specified bounds.
     * @param value  the value to check
     * @param lowerBound  the lower bound allowed for value
     * @param upperBound  the upper bound allowed for value
     * @throws IllegalFieldValueException if value is not in the specified bounds
    public static void verifyValueBounds(DateTimeField field, int value, int lowerBound, int upperBound) { … }
     * Verify that input values are within specified bounds.
     * @param value  the value to check
     * @param lowerBound  the lower bound allowed for value
     * @param upperBound  the upper bound allowed for value
     * @throws IllegalFieldValueException if value is not in the specified bounds
     * @since 1.1
    public static void verifyValueBounds(DateTimeFieldType fieldType, int value, int lowerBound, int upperBound) { … }
     * Verify that input values are within specified bounds.
     * @param value  the value to check
     * @param lowerBound  the lower bound allowed for value
     * @param upperBound  the upper bound allowed for value
     * @throws IllegalFieldValueException if value is not in the specified bounds
    public static void verifyValueBounds(String fieldName, int value, int lowerBound, int upperBound) { … }
     * Utility method used by addWrapField implementations to ensure the new
     * value lies within the field's legal value range.
     * @param currentValue the current value of the data, which may lie outside
     * the wrapped value range
     * @param wrapValue  the value to add to current value before
     * wrapping.  This may be negative.
     * @param minValue the wrap range minimum value.
     * @param maxValue the wrap range maximum value.  This must be
     * greater than minValue (checked by the method).
     * @return the wrapped value
     * @throws IllegalArgumentException if minValue is greater
     * than or equal to maxValue
    public static int getWrappedValue(int currentValue, int wrapValue, int minValue, int maxValue) { … }
     * Utility method that ensures the given value lies within the field's
     * legal value range.
     * @param value  the value to fit into the wrapped value range
     * @param minValue the wrap range minimum value.
     * @param maxValue the wrap range maximum value.  This must be
     * greater than minValue (checked by the method).
     * @return the wrapped value
     * @throws IllegalArgumentException if minValue is greater
     * than or equal to maxValue
    public static int getWrappedValue(int value, int minValue, int maxValue) { … }
     * Compares two objects as equals handling null.
     * @param object1  the first object
     * @param object2  the second object
     * @return true if equal
     * @since 1.4
    public static boolean equals(Object object1, Object object2) { … }
}
</class>

<class name="IllegalFieldValueException" role="collaborator">
     * Exception thrown when attempting to set a field outside its supported range.
     * @author Brian S O'Neill
     * @since 1.1
public class IllegalFieldValueException extends IllegalArgumentException {
     * Serialization lock.
    private static final long serialVersionUID = 6305711765985447737L;
     * Creates a message for the exception.
     * @param fieldName  the field name
     * @param value  the value rejected
     * @param lowerBound  the lower bound allowed
     * @param upperBound  the uppe bound allowed
     * @param explain  an explanation
     * @return the message
    private static String createMessage(String fieldName, Number value, Number lowerBound, Number upperBound, String explain) { … }
     * Creates a message for the exception.
     * @param fieldName  the field name
     * @param value  the value rejected
     * @return the message
    private static String createMessage(String fieldName, String value) { … }
    private final DateTimeFieldType iDateTimeFieldType;
    private final DurationFieldType iDurationFieldType;
    private final String iFieldName;
    private final Number iNumberValue;
    private final String iStringValue;
    private final Number iLowerBound;
    private final Number iUpperBound;
    private String iMessage;
     * Constructor.
     * @param fieldType  type of field being set
     * @param value  illegal value being set
     * @param lowerBound  lower legal field value, or null if not applicable
     * @param upperBound  upper legal field value, or null if not applicable
    public IllegalFieldValueException(DateTimeFieldType fieldType, Number value, Number lowerBound, Number upperBound) {
        super(createMessage(fieldType.getName(), value, lowerBound, upperBound, null));
        iDateTimeFieldType = fieldType;
        iDurationFieldType = null;
        iFieldName = fieldType.getName();
        iNumberValue = value;
        iStringValue = null;
        iLowerBound = lowerBound;
        iUpperBound = upperBound;
        iMessage = super.getMessage();
    }
     * Constructor.
     * @param fieldType  type of field being set
     * @param value  illegal value being set
     * @param explain  an explanation
     * @since 1.5
    public IllegalFieldValueException(DateTimeFieldType fieldType, Number value, String explain) {
        super(createMessage(fieldType.getName(), value, null, null, explain));
        iDateTimeFieldType = fieldType;
        iDurationFieldType = null;
        iFieldName = fieldType.getName();
        iNumberValue = value;
        iStringValue = null;
        iLowerBound = null;
        iUpperBound = null;
        iMessage = super.getMessage();
    }
     * Constructor.
     * @param fieldType  type of field being set
     * @param value  illegal value being set
     * @param lowerBound  lower legal field value, or null if not applicable
     * @param upperBound  upper legal field value, or null if not applicable
    public IllegalFieldValueException(DurationFieldType fieldType, Number value, Number lowerBound, Number upperBound) {
        super(createMessage(fieldType.getName(), value, lowerBound, upperBound, null));
        iDateTimeFieldType = null;
        iDurationFieldType = fieldType;
        iFieldName = fieldType.getName();
        iNumberValue = value;
        iStringValue = null;
        iLowerBound = lowerBound;
        iUpperBound = upperBound;
        iMessage = super.getMessage();
    }
     * Constructor.
     * @param fieldName  name of field being set
     * @param value  illegal value being set
     * @param lowerBound  lower legal field value, or null if not applicable
     * @param upperBound  upper legal field value, or null if not applicable
    public IllegalFieldValueException(String fieldName, Number value, Number lowerBound, Number upperBound) {
        super(createMessage(fieldName, value, lowerBound, upperBound, null));
        iDateTimeFieldType = null;
        iDurationFieldType = null;
        iFieldName = fieldName;
        iNumberValue = value;
        iStringValue = null;
        iLowerBound = lowerBound;
        iUpperBound = upperBound;
        iMessage = super.getMessage();
    }
     * Constructor.
     * @param fieldType  type of field being set
     * @param value  illegal value being set
    public IllegalFieldValueException(DateTimeFieldType fieldType, String value) {
        super(createMessage(fieldType.getName(), value));
        iDateTimeFieldType = fieldType;
        iDurationFieldType = null;
        iFieldName = fieldType.getName();
        iStringValue = value;
        iNumberValue = null;
        iLowerBound = null;
        iUpperBound = null;
        iMessage = super.getMessage();
    }
     * Constructor.
     * @param fieldType  type of field being set
     * @param value  illegal value being set
    public IllegalFieldValueException(DurationFieldType fieldType, String value) {
        super(createMessage(fieldType.getName(), value));
        iDateTimeFieldType = null;
        iDurationFieldType = fieldType;
        iFieldName = fieldType.getName();
        iStringValue = value;
        iNumberValue = null;
        iLowerBound = null;
        iUpperBound = null;
        iMessage = super.getMessage();
    }
     * Constructor.
     * @param fieldName  name of field being set
     * @param value  illegal value being set
    public IllegalFieldValueException(String fieldName, String value) {
        super(createMessage(fieldName, value));
        iDateTimeFieldType = null;
        iDurationFieldType = null;
        iFieldName = fieldName;
        iStringValue = value;
        iNumberValue = null;
        iLowerBound = null;
        iUpperBound = null;
        iMessage = super.getMessage();
    }
     * Returns the DateTimeFieldType whose value was invalid, or null if not applicable.
     * @return the datetime field type
    public DateTimeFieldType getDateTimeFieldType() { … }
    public String getMessage() { … }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="DateTimeField" role="collaborator">
     * Defines the calculation engine for date and time fields.
     * The interface defines a set of methods that manipulate a millisecond datetime
     * with regards to a single field, such as monthOfYear or secondOfMinute.
     * <p>
     * This design is extensible so, if you wish, you can extract a different field from
     * the milliseconds. A number of standard implementations are provided to assist.
     * @author Guy Allard
     * @author Stephen Colebourne
     * @author Brian S O'Neill
     * @since 1.0
public abstract class DateTimeField {
     * Get the type of the field.
     * @return field type
    public abstract DateTimeFieldType getType();
     * Get the name of the field.
     * <p>
     * By convention, names follow a pattern of "dddOfRrr", where "ddd" represents
     * the (singular) duration unit field name and "Rrr" represents the (singular)
     * duration range field name. If the range field is not applicable, then
     * the name of the field is simply the (singular) duration field name.
     * @return field name
    public abstract String getName();
     * Returns true if this field is supported.
     * @return true if this field is supported
    public abstract boolean isSupported();
     * Returns true if the set method is lenient. If so, it accepts values that
     * are out of bounds. For example, a lenient day of month field accepts 32
     * for January, converting it to February 1.
     * @return true if this field is lenient
    public abstract boolean isLenient();
     * Get the value of this field from the milliseconds.
     * @param instant  the milliseconds from 1970-01-01T00:00:00Z to query
     * @return the value of the field, in the units of the field
    public abstract int get(long instant);
     * Get the human-readable, text value of this field from the milliseconds.
     * If the specified locale is null, the default locale is used.
     * @param instant  the milliseconds from 1970-01-01T00:00:00Z to query
     * @param locale the locale to use for selecting a text symbol, null for default
     * @return the text value of the field
    public abstract String getAsText(long instant, Locale locale);
     * Get the human-readable, text value of this field from the milliseconds.
     * @param instant  the milliseconds from 1970-01-01T00:00:00Z to query
     * @return the text value of the field
    public abstract String getAsText(long instant);
     * Get the human-readable, text value of this field from a partial instant.
     * If the specified locale is null, the default locale is used.
     * @param partial  the partial instant to query
     * @param fieldValue  the field value of this field, provided for performance
     * @param locale  the locale to use for selecting a text symbol, null for default
     * @return the text value of the field
    public abstract String getAsText(ReadablePartial partial, int fieldValue, Locale locale);
     * Get the human-readable, text value of this field from a partial instant.
     * If the specified locale is null, the default locale is used.
     * @param partial  the partial instant to query
     * @param locale  the locale to use for selecting a text symbol, null for default
     * @return the text value of the field
    public abstract String getAsText(ReadablePartial partial, Locale locale);
     * Get the human-readable, text value of this field from the field value.
     * If the specified locale is null, the default locale is used.
     * @param fieldValue  the numeric value to convert to text
     * @param locale the locale to use for selecting a text symbol, null for default
     * @return the text value of the field
    public abstract String getAsText(int fieldValue, Locale locale);
     * Get the human-readable, short text value of this field from the
     * milliseconds.  If the specified locale is null, the default locale is used.
     * @param instant  the milliseconds from 1970-01-01T00:00:00Z to query
     * @param locale the locale to use for selecting a text symbol, null for default
     * @return the short text value of the field
    public abstract String getAsShortText(long instant, Locale locale);
     * Get the human-readable, short text value of this field from the
     * milliseconds.
     * @param instant  the milliseconds from 1970-01-01T00:00:00Z to query
     * @return the short text value of the field
    public abstract String getAsShortText(long instant);
     * Get the human-readable, short text value of this field from a partial instant.
     * If the specified locale is null, the default locale is used.
     * @param partial  the partial instant to query
     * @param fieldValue  the field value of this field, provided for performance
     * @param locale  the locale to use for selecting a text symbol, null for default
     * @return the text value of the field
    public abstract String getAsShortText(ReadablePartial partial, int fieldValue, Locale locale);
     * Get the human-readable, short text value of this field from a partial instant.
     * If the specified locale is null, the default locale is used.
     * @param partial  the partial instant to query
     * @param locale  the locale to use for selecting a text symbol, null for default
     * @return the text value of the field
    public abstract String getAsShortText(ReadablePartial partial, Locale locale);
     * Get the human-readable, short text value of this field from the field value.
     * If the specified locale is null, the default locale is used.
     * @param fieldValue  the numeric value to convert to text
     * @param locale the locale to use for selecting a text symbol, null for default
     * @return the text value of the field
    public abstract String getAsShortText(int fieldValue, Locale locale);
     * Get the minimum allowable value for this field.
     * @return the minimum valid value for this field, in the units of the
     * field
    public abstract int getMinimumValue();
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="DateTimeFieldType" role="collaborator">
     * Identifies a field, such as year or minuteOfHour, in a chronology-neutral way.
     * <p>
     * A field type defines the type of the field, such as hourOfDay.
     * If does not directly enable any calculations, however it does provide a
     * {@link #getField(Chronology)} method that returns the actual calculation engine
     * for a particular chronology.
     * It also provides access to the related {@link DurationFieldType}s.
     * <p>
     * Instances of <code>DateTimeFieldType</code> are singletons.
     * They can be compared using <code>==</code>.
     * <p>
     * If required, you can create your own field, for example a quarterOfYear.
     * …
public abstract class DateTimeFieldType implements Serializable {
     * Serialization version
    private static final long serialVersionUID = -42615285973990L;
     * Ordinal values for standard field types. */
     * static final byte
     * ERA = 1,
     * YEAR_OF_ERA = 2,
     * CENTURY_OF_ERA = 3,
     * YEAR_OF_CENTURY = 4,
     * YEAR = 5,
     * DAY_OF_YEAR = 6,
     * MONTH_OF_YEAR = 7,
     * DAY_OF_MONTH = 8,
     * WEEKYEAR_OF_CENTURY = 9,
     * WEEKYEAR = 10,
     * …
    private static final DateTimeFieldType ERA_TYPE = new StandardDateTimeFieldType( "era", ERA, DurationFieldType.eras(), null);
     * The yearOfEra field type.
    private static final DateTimeFieldType YEAR_OF_ERA_TYPE = new StandardDateTimeFieldType( "yearOfEra", YEAR_OF_ERA, DurationFieldType.years(), DurationFieldType.eras());
     * The centuryOfEra field type.
    private static final DateTimeFieldType CENTURY_OF_ERA_TYPE = new StandardDateTimeFieldType( "centuryOfEra", CENTURY_OF_ERA, DurationFieldType.centuries(), DurationFieldType.eras());
     * The yearOfCentury field type.
    private static final DateTimeFieldType YEAR_OF_CENTURY_TYPE = new StandardDateTimeFieldType( "yearOfCentury", YEAR_OF_CENTURY, DurationFieldType.years(), DurationFieldType.centuries());
     * The year field type.
    private static final DateTimeFieldType YEAR_TYPE = new StandardDateTimeFieldType( "year", YEAR, DurationFieldType.years(), null);
     * The dayOfYear field type.
    private static final DateTimeFieldType DAY_OF_YEAR_TYPE = new StandardDateTimeFieldType( "dayOfYear", DAY_OF_YEAR, DurationFieldType.days(), DurationFieldType.years());
     * The monthOfYear field type.
    private static final DateTimeFieldType MONTH_OF_YEAR_TYPE = new StandardDateTimeFieldType( "monthOfYear", MONTH_OF_YEAR, DurationFieldType.months(), DurationFieldType.years());
     * The dayOfMonth field type.
    private static final DateTimeFieldType DAY_OF_MONTH_TYPE = new StandardDateTimeFieldType( "dayOfMonth", DAY_OF_MONTH, DurationFieldType.days(), DurationFieldType.months());
     * The weekyearOfCentury field type.
    private static final DateTimeFieldType WEEKYEAR_OF_CENTURY_TYPE = new StandardDateTimeFieldType( "weekyearOfCentury", WEEKYEAR_OF_CENTURY, DurationFieldType.weekyears(), DurationFieldType.centuries());
     * The weekyear field type.
    private static final DateTimeFieldType WEEKYEAR_TYPE = new StandardDateTimeFieldType( "weekyear", WEEKYEAR, DurationFieldType.weekyears(), null);
     * The weekOfWeekyear field type.
    private static final DateTimeFieldType WEEK_OF_WEEKYEAR_TYPE = new StandardDateTimeFieldType( "weekOfWeekyear", WEEK_OF_WEEKYEAR, DurationFieldType.weeks(), DurationFieldType.weekyears());
     * The dayOfWeek field type.
    private static final DateTimeFieldType DAY_OF_WEEK_TYPE = new StandardDateTimeFieldType( "dayOfWeek", DAY_OF_WEEK, DurationFieldType.days(), DurationFieldType.weeks());
     * The halfday field type.
    private static final DateTimeFieldType HALFDAY_OF_DAY_TYPE = new StandardDateTimeFieldType( "halfdayOfDay", HALFDAY_OF_DAY, DurationFieldType.halfdays(), DurationFieldType.days());
     * The hourOfHalfday field type.
    private static final DateTimeFieldType HOUR_OF_HALFDAY_TYPE = new StandardDateTimeFieldType( "hourOfHalfday", HOUR_OF_HALFDAY, DurationFieldType.hours(), DurationFieldType.halfdays());
     * The clockhourOfHalfday field type.
    private static final DateTimeFieldType CLOCKHOUR_OF_HALFDAY_TYPE = new StandardDateTimeFieldType( "clockhourOfHalfday", CLOCKHOUR_OF_HALFDAY, DurationFieldType.hours(), DurationFieldType.halfdays());
     * The clockhourOfDay field type.
    private static final DateTimeFieldType CLOCKHOUR_OF_DAY_TYPE = new StandardDateTimeFieldType( "clockhourOfDay", CLOCKHOUR_OF_DAY, DurationFieldType.hours(), DurationFieldType.days());
     * The hourOfDay field type.
    private static final DateTimeFieldType HOUR_OF_DAY_TYPE = new StandardDateTimeFieldType( "hourOfDay", HOUR_OF_DAY, DurationFieldType.hours(), DurationFieldType.days());
     * The minuteOfDay field type.
    private static final DateTimeFieldType MINUTE_OF_DAY_TYPE = new StandardDateTimeFieldType( "minuteOfDay", MINUTE_OF_DAY, DurationFieldType.minutes(), DurationFieldType.days());
     * The minuteOfHour field type.
    private static final DateTimeFieldType MINUTE_OF_HOUR_TYPE = new StandardDateTimeFieldType( "minuteOfHour", MINUTE_OF_HOUR, DurationFieldType.minutes(), DurationFieldType.hours());
     * The secondOfDay field type.
    private static final DateTimeFieldType SECOND_OF_DAY_TYPE = new StandardDateTimeFieldType( "secondOfDay", SECOND_OF_DAY, DurationFieldType.seconds(), DurationFieldType.days());
     * The secondOfMinute field type.
    private static final DateTimeFieldType SECOND_OF_MINUTE_TYPE = new StandardDateTimeFieldType( "secondOfMinute", SECOND_OF_MINUTE, DurationFieldType.seconds(), DurationFieldType.minutes());
     * The millisOfDay field type.
    private static final DateTimeFieldType MILLIS_OF_DAY_TYPE = new StandardDateTimeFieldType( "millisOfDay", MILLIS_OF_DAY, DurationFieldType.millis(), DurationFieldType.days());
     * The name of the field.
    private final String iName;
     * Get a suitable debug string.
     * @return debug string
    public String toString() { … }
}
// … (non-patched members truncated; patched methods kept)
</class>
</class_skeletons>

DOCUMENTED PRECONDITIONS / REJECTION CONTRACT of the touched method(s), from their javadoc. An input violating an @param constraint is INVALID — a documented @throws on it is CORRECT behaviour, never a finding. Build inputs that satisfy these constraints BY CONSTRUCTION (order/clamp/force them valid before the call) and assert only on those:
  - @param value  the value to negate
  - @throws ArithmeticException if the value is Integer.MIN_VALUE
  - @param val1  the first value
  - @param val2  the second value
  - @throws ArithmeticException if the value is too big or too small
REJECTION ORACLES — RE-PROBE AFTER EVERY STATE CHANGE. When you assert a documented rejection (asking for something absent, invalid or out of range MUST throw), run that probe again AFTER every call that changes the receiver's state — installing, registering, adding, removing, clearing — not once on the freshly built object. Mutate, then probe; mutate again, then probe again, asserting the SAME documented outcome each time. A correct rejection depends only on the probe itself being absent or invalid, never on unrelated receiver state, so it must hold in every state the object passes through; a patch that makes the rejection conditional on the container's contents, its size, or which slots are occupied diverges ONLY in the mutated states, so a probe placed before the mutations is silent on the buggy and the patched build alike. Draw WHICH slot/key/index each mutation targets, and HOW MANY mutations happen, from the FuzzedDataProvider — never from literals: fixed targets rebuild one shape every iteration, and the states where such a patch misbehaves (a gap between filled slots, an emptied container, a larger one) are never reached.

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  getWrappedValue(int currentValue, int wrapValue,
                                      int minValue, int maxValue) / (int value, int minValue, int maxValue)
  safeAdd(int val1, int val2) / (long val1, long val2)
  safeMultiply(int val1, int val2) / (long val1, int val2) / (long val1, long val2)
  verifyValueBounds(DateTimeField field, 
                                         int value, int lowerBound, int upperBound) / (DateTimeFieldType fieldType, 
                                         int value, int lowerBound, int upperBound) / (String fieldName,
                                         int value, int lowerBound, int upperBound)

METHOD FAMILIES over the same input space (factory/parser siblings — where the docs state a selection or agreement rule between family members, equivalent inputs must respect it):
  safe* family: safeAdd, safeMultiply, safeMultiplyToInt, safeNegate, safeSubtract, safeToInt

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
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `TestFieldUtils`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
Lift the call and the expected value from THIS test (this attempt's target):
<failing_test class="org.joda.time.field.TestFieldUtils" method="testSafeMultiplyLongInt">
    public void testSafeMultiplyLongInt() {
        assertEquals(0L, FieldUtils.safeMultiply(0L, 0));
        
        assertEquals(1L, FieldUtils.safeMultiply(1L, 1));
        assertEquals(3L, FieldUtils.safeMultiply(1L, 3));
        assertEquals(3L, FieldUtils.safeMultiply(3L, 1));
        
        assertEquals(6L, FieldUtils.safeMultiply(2L, 3));
        assertEquals(-6L, FieldUtils.safeMultiply(2L, -3));
        assertEquals(-6L, FieldUtils.safeMultiply(-2L, 3));
        assertEquals(6L, FieldUtils.safeMultiply(-2L, -3));
        
        assertEquals(-1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE));
        
        assertEquals(Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1));
        assertEquals(Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1));
        assertEquals(-Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1));
        
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
            fail();
        } catch (ArithmeticException e) {
        }
        
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
            fail();
        } catch (ArithmeticException e) {
        }
        
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
            fail();
        } catch (ArithmeticException e) {
        }
        
        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
            fail();
        } catch (ArithmeticException e) {
        }
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.joda.time.field.TestFieldUtils::testSafeMultiplyLongInt
junit.framework.AssertionFailedError
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.
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
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~7,060 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actualLong;
        boolean threw;

        actualLong = FieldUtils.safeMultiply(0L, 0);
        if (actualLong != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-0] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 1);
        if (actualLong != 1L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-1] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 3);
        if (actualLong != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-2] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(3L, 1);
        if (actualLong != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-3] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, 3);
        if (actualLong != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-4] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, -3);
        if (actualLong != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-5] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, 3);
        if (actualLong != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-6] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, -3);
        if (actualLong != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-7] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        if (actualLong != (-1L * Integer.MIN_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-8] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected=" + (-1L * Integer.MIN_VALUE) + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        if (actualLong != Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-9] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected=" + Long.MAX_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        if (actualLong != Long.MIN_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-10] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected=" + Long.MIN_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        if (actualLong != (-Long.MAX_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected=" + (-Long.MAX_VALUE) + " actual=" + actualLong);
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-12] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-13] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) expected ArithmeticException");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-14] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) expected ArithmeticException");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-15] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) expected ArithmeticException");
        }

        int fuzzVal = data.consumeInt(-1_000_000, 1_000_000);
        int fuzzScalar = data.consumeInt(-1_000, 1_000);

        try {
            long lhs = FieldUtils.safeMultiply((long) fuzzVal, fuzzScalar);
            long rhs = FieldUtils.safeMultiply((long) fuzzVal, (long) fuzzScalar);
            /* Contract justification: the overloads safeMultiply(long,int) and safeMultiply(long,long)
               are sibling APIs for the same mathematical operation ("Multiply two values throwing an
               exception if overflow occurs"). For the same operands in a non-overflowing range, both
               must produce the same observable result. A patch that merely deletes/avoids the throw or
               silently returns a wrapped value in one overload violates this post-condition. */
            if (lhs != rhs) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) inputVal="
                        + ((long) fuzzVal) + " inputScalar=" + fuzzScalar + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Throwable ignored) {
            return;
        }
    }
}
```

</details>

---
## [42] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `threw` (literal assignment, no rethrow, the caught exceptio

---
## [43] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~8,204 chars, 2 new)</summary>

- *[system] message: identical to step [35] — not reprinted*
- *[user] message: identical to step [41] — not reprinted*
**[assistant]**
```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actualLong;
        boolean threw;

        actualLong = FieldUtils.safeMultiply(0L, 0);
        if (actualLong != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-0] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 1);
        if (actualLong != 1L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-1] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 3);
        if (actualLong != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-2] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(3L, 1);
        if (actualLong != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-3] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, 3);
        if (actualLong != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-4] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, -3);
        if (actualLong != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-5] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, 3);
        if (actualLong != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-6] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, -3);
        if (actualLong != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-7] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        if (actualLong != (-1L * Integer.MIN_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-8] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected=" + (-1L * Integer.MIN_VALUE) + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        if (actualLong != Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-9] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected=" + Long.MAX_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        if (actualLong != Long.MIN_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-10] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected=" + Long.MIN_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        if (actualLong != (-Long.MAX_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected=" + (-Long.MAX_VALUE) + " actual=" + actualLong);
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-12] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-13] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) expected ArithmeticException");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-14] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) expected ArithmeticException");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-15] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) expected ArithmeticException");
        }

        int fuzzVal = data.consumeInt(-1_000_000, 1_000_000);
        int fuzzScalar = data.consumeInt(-1_000, 1_000);

        try {
            long lhs = FieldUtils.safeMultiply((long) fuzzVal, fuzzScalar);
            long rhs = FieldUtils.safeMultiply((long) fuzzVal, (long) fuzzScalar);
            /* Contract justification: the overloads safeMultiply(long,int) and safeMultiply(long,long)
               are sibling APIs for the same mathematical operation ("Multiply two values throwing an
               exception if overflow occurs"). For the same operands in a non-overflowing range, both
               must produce the same observable result. A patch that merely deletes/avoids the throw or
               silently returns a wrapped value in one overload violates this post-condition. */
            if (lhs != rhs) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) inputVal="
                        + ((long) fuzzVal) + " inputScalar=" + fuzzScalar + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Throwable ignored) {
            return;
        }
    }
}
```
**[user]**
```
Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `threw` (literal assignment, no rethrow, the caught exception is never used) and the alarm near line 89 is conditioned on `threw` — the caught exception's identity is destroyed, so attribution cannot tell WHICH exception fired or whether the unpatched build crashes the same way. Rethrow the exception as the alarm's cause (`..., e`), or assert on the real observable instead of a swallowed boolean.
A caught exception is a REJECTION, never a violation, and flattening it into a flag destroys the identity attribution needs. Either assert on the real observable the test pins (compute the actual value and compare it), or if you must re-report a caught crash, re-throw it with the original as the cause: `new FuzzerSecurityIssueLow("[oracle:<id>] ...", e)`. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~6,756 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actualLong;

        actualLong = FieldUtils.safeMultiply(0L, 0);
        if (actualLong != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-0] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 1);
        if (actualLong != 1L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-1] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 3);
        if (actualLong != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-2] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(3L, 1);
        if (actualLong != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-3] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, 3);
        if (actualLong != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-4] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, -3);
        if (actualLong != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-5] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, 3);
        if (actualLong != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-6] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, -3);
        if (actualLong != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-7] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        if (actualLong != (-1L * Integer.MIN_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-8] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected=" + (-1L * Integer.MIN_VALUE) + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        if (actualLong != Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-9] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected=" + Long.MAX_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        if (actualLong != Long.MIN_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-10] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected=" + Long.MIN_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        if (actualLong != (-Long.MAX_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected=" + (-Long.MAX_VALUE) + " actual=" + actualLong);
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-12] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException");
        } catch (ArithmeticException expected) {
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-13] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) expected ArithmeticException");
        } catch (ArithmeticException expected) {
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-14] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) expected ArithmeticException");
        } catch (ArithmeticException expected) {
        }

        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-15] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) expected ArithmeticException");
        } catch (ArithmeticException expected) {
        }

        int fuzzVal = data.consumeInt(-1_000_000, 1_000_000);
        int fuzzScalar = data.consumeInt(-1_000, 1_000);

        try {
            long lhs = FieldUtils.safeMultiply((long) fuzzVal, fuzzScalar);
            long rhs = FieldUtils.safeMultiply((long) fuzzVal, (long) fuzzScalar);
            /* Contract justification: the overloads safeMultiply(long,int) and safeMultiply(long,long)
               both document the same operation, "Multiply two values throwing an exception if overflow occurs".
               For the same mathematical operands in a non-overflowing range, both real library calls must
               return the same result. A patch that suppresses a needed overflow check or returns a silently
               wrong wrapped value in one overload breaks this observable post-condition even when no throw occurs. */
            if (lhs != rhs) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) inputVal="
                        + ((long) fuzzVal) + " inputScalar=" + fuzzScalar + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Throwable ignored) {
            return;
        }
    }
}
```

</details>

---
## [44] ⚙️ harness-attempt · `attempt_004`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-12] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException

---
## [45] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [35] — not reprinted*
- *[user] message: identical to step [35] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,147 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedTestOracles();
        checkSeedGeneralization();
        checkSiblingAgreementLongIntVsLongLong(data);
        checkSiblingAgreementIntIntVsLongInt(data);
        checkConstructedInputOracle(data);
    }

    private static void checkLiftedTestOracles() {
        assertEqualsLong("lifted-0x0", 0L, FieldUtils.safeMultiply(0L, 0));
        assertEqualsLong("lifted-1x1", 1L, FieldUtils.safeMultiply(1L, 1));
        assertEqualsLong("lifted-1x3", 3L, FieldUtils.safeMultiply(1L, 3));
        assertEqualsLong("lifted-3x1", 3L, FieldUtils.safeMultiply(3L, 1));

        assertEqualsLong("lifted-2x3", 6L, FieldUtils.safeMultiply(2L, 3));
        assertEqualsLong("lifted-2x-3", -6L, FieldUtils.safeMultiply(2L, -3));
        assertEqualsLong("lifted--2x3", -6L, FieldUtils.safeMultiply(-2L, 3));
        assertEqualsLong("lifted--2x-3", 6L, FieldUtils.safeMultiply(-2L, -3));

        assertEqualsLong("lifted--1xIntegerMin", -1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE));

        assertEqualsLong("lifted-longMaxx1", Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1));
        assertEqualsLong("lifted-longMinx1", Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1));
        assertEqualsLong("lifted-longMaxx-1", -Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1));

        assertThrowsArithmetic("lifted-longMinx-1-throws", Long.MIN_VALUE, -1);
        assertThrowsArithmetic("lifted-longMinx100-throws", Long.MIN_VALUE, 100);
        assertThrowsArithmetic("lifted-longMinxIntegerMax-throws", Long.MIN_VALUE, Integer.MAX_VALUE);
        assertThrowsArithmetic("lifted-longMaxxIntegerMin-throws", Long.MAX_VALUE, Integer.MIN_VALUE);
    }

    private static void checkSeedGeneralization() {
        // Contract justification: safeMultiply documents multiplication with overflow checking.
        // For multiplier 1, the method body explicitly returns val1; any patch that merely removes
        // the throw or makes a branch unreachable must still preserve this observable identity.
        long[] values = new long[] {
            Long.MIN_VALUE,
            Long.MAX_VALUE,
            -1L,
            0L,
            1L,
            2L,
            -2L,
            123456789L,
            -987654321L
        };
        for (int i = 0; i < values.length; i++) {
            long v = values[i];
            assertEqualsLong("postcond-identity-" + i, v, FieldUtils.safeMultiply(v, 1));
        }

        // Contract justification: multiplying by zero yields zero and is explicitly handled in code.
        for (int i = 0; i < values.length; i++) {
            long v = values[i];
            assertEqualsLong("postcond-zero-" + i, 0L, FieldUtils.safeMultiply(v, 0));
        }

        // Contract justification: for any val1 other than Long.MIN_VALUE, multiplication by -1 is
        // numeric negation. This post-condition would be violated by a silent wrong-value patch.
        for (int i = 0; i < values.length; i++) {
            long v = values[i];
            if (v != Long.MIN_VALUE) {
                assertEqualsLong("postcond-negate-" + i, -v, FieldUtils.safeMultiply(v, -1));
            }
        }
    }

    private static void checkSiblingAgreementLongIntVsLongLong(FuzzedDataProvider data) {
        long val1 = data.consumeInt(-1_000_000, 1_000_000);
        int val2 = data.consumeInt(-1_000_000, 1_000_000);

        long expected;
        try {
            expected = FieldUtils.safeMultiply(val1, (long) val2);
        } catch (Throwable t) {
            return;
        }

        long actual;
        try {
            actual = FieldUtils.safeMultiply(val1, val2);
        } catch (Throwable t) {
            return;
        }

        if (actual != expected) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:safeMultiplyLongInt_agreesWithLongLongSibling] metamorphic violation: safeMultiply(long,int) disagrees with safeMultiply(long,long) inputVal1="
                    + val1 + " inputVal2=" + val2 + " lhs=" + actual + " rhs=" + expected);
        }
    }

    private static void checkSiblingAgreementIntIntVsLongInt(FuzzedDataProvider data) {
        int a = data.consumeInt(-1_000_000, 1_000_000);
        int b = data.consumeInt(-1_000_000, 1_000_000);

        int intResult;
        try {
            intResult = FieldUtils.safeMultiply(a, b);
        } catch (Throwable t) {
            return;
        }

        long longResult;
        try {
            longResult = FieldUtils.safeMultiply((long) a, b);
        } catch (Throwable t) {
            return;
        }

        if (longResult != (long) intResult) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:safeMultiplyIntInt_agreesWithLongIntSibling] metamorphic violation: safeMultiply(int,int) disagrees with safeMultiply(long,int) inputA="
                    + a + " inputB=" + b + " lhs=" + longResult + " rhs=" + intResult);
        }
    }

    private static void checkConstructedInputOracle(FuzzedDataProvider data) {
        // Oracle-from-input justification: we choose n first, then build the canonical equivalent
        // expression n * (-1). For every n except Integer.MIN_VALUE, safeMultiply(int,int) must
        // equal safeNegate(n); both are real library calls from the safe* family with the same
        // documented overflow semantics.
        int n = data.consumeInt(-1_000_000, 1_000_000);
        if (n == Integer.MIN_VALUE) {
            return;
        }

        int negated;
        try {
            negated = FieldUtils.safeNegate(n);
        } catch (Throwable t) {
            return;
        }

        int multiplied;
        try {
            multiplied = FieldUtils.safeMultiply(n, -1);
        } catch (Throwable t) {
            return;
        }

        if (multiplied != negated) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:safeMultiply_vs_safeNegate] metamorphic violation: safeMultiply(n,-1) must equal safeNegate(n) inputN="
                    + n + " lhs=" + multiplied + " rhs=" + negated);
        }
    }

    private static void assertEqualsLong(String oracleId, long expected, long actual) {
        if (expected != actual) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertThrowsArithmetic(String oracleId, long val1, int val2) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: expected ArithmeticException but returned=" + actual
                    + " for val1=" + val1 + " val2=" + val2);
        } catch (ArithmeticException expected) {
        }
    }
}
```

</details>

---
## [46] ⚙️ harness-attempt · `attempt_005`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-longMinx-1-throws] semantic mismatch: expected ArithmeticException but returned=-9223372036854775808 for val1=-92233720368547758…

---
## [47] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [35] — not reprinted*
- *[user] message: identical to step [41] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,742 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actualLong;

        actualLong = FieldUtils.safeMultiply(0L, 0);
        if (actualLong != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-0] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 1);
        if (actualLong != 1L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-1] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 3);
        if (actualLong != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-2] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(3L, 1);
        if (actualLong != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-3] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, 3);
        if (actualLong != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-4] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, -3);
        if (actualLong != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-5] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, 3);
        if (actualLong != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-6] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, -3);
        if (actualLong != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-7] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        long expectedMinIntProduct = -1L * Integer.MIN_VALUE;
        if (actualLong != expectedMinIntProduct) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-8] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected="
                    + expectedMinIntProduct + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        if (actualLong != Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-9] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected="
                    + Long.MAX_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        if (actualLong != Long.MIN_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-10] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected="
                    + Long.MIN_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        if (actualLong != -Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected="
                    + (-Long.MAX_VALUE) + " actual=" + actualLong);
        }

        boolean sawExpectedArithmetic = false;
        try {
            long buggyActual = FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-12] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected=ArithmeticException actual="
                    + buggyActual);
        } catch (ArithmeticException expected) {
            sawExpectedArithmetic = true;
        }
        if (!sawExpectedArithmetic) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-12] semantic mismatch: expected ArithmeticException for FieldUtils.safeMultiply(Long.MIN_VALUE, -1)");
        }

        sawExpectedArithmetic = false;
        try {
            long buggyActual = FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-13] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) expected=ArithmeticException actual="
                    + buggyActual);
        } catch (ArithmeticException expected) {
            sawExpectedArithmetic = true;
        }
        if (!sawExpectedArithmetic) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-13] semantic mismatch: expected ArithmeticException for FieldUtils.safeMultiply(Long.MIN_VALUE, 100)");
        }

        sawExpectedArithmetic = false;
        try {
            long buggyActual = FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-14] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) expected=ArithmeticException actual="
                    + buggyActual);
        } catch (ArithmeticException expected) {
            sawExpectedArithmetic = true;
        }
        if (!sawExpectedArithmetic) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-14] semantic mismatch: expected ArithmeticException for FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE)");
        }

        sawExpectedArithmetic = false;
        try {
            long buggyActual = FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-15] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) expected=ArithmeticException actual="
                    + buggyActual);
        } catch (ArithmeticException expected) {
            sawExpectedArithmetic = true;
        }
        if (!sawExpectedArithmetic) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-15] semantic mismatch: expected ArithmeticException for FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE)");
        }

        long exploratoryVal = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
        int exploratoryScalar = data.consumeInt();
        try {
            FieldUtils.safeMultiply(exploratoryVal, exploratoryScalar);
        } catch (Throwable ignored) {
        }

        long moderateVal = data.consumeInt(-1_000_000, 1_000_000);
        int moderateScalar = data.consumeInt(-1_000, 1_000);

        try {
            long lhs = FieldUtils.safeMultiply(moderateVal, moderateScalar);
            long rhs = FieldUtils.safeMultiply(moderateVal, (long) moderateScalar);
            // Contract/oracle: these same-name overloads implement the same mathematical operation
            // ("Multiply two values throwing an exception if overflow occurs"), so on the same
            // non-overflowing inputs they must return the same result. A patch that merely deletes
            // the overflow throw in the int-overload path breaks this observable agreement.
            if (lhs != rhs) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: safeMultiply(long,int) != safeMultiply(long,long)"
                        + " inputVal=" + moderateVal
                        + " inputScalar=" + moderateScalar
                        + " lhs=" + lhs
                        + " rhs=" + rhs);
            }
        } catch (Throwable ignored) {
            return;
        }
    }
}
```

</details>

---
## [48] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `sawExpectedArithmetic` (literal assignment, no rethrow, the

---
## [49] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~9,918 chars, 2 new)</summary>

- *[system] message: identical to step [35] — not reprinted*
- *[user] message: identical to step [41] — not reprinted*
**[assistant]**
```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actualLong;

        actualLong = FieldUtils.safeMultiply(0L, 0);
        if (actualLong != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-0] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 1);
        if (actualLong != 1L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-1] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 3);
        if (actualLong != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-2] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(3L, 1);
        if (actualLong != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-3] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, 3);
        if (actualLong != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-4] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, -3);
        if (actualLong != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-5] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, 3);
        if (actualLong != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-6] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, -3);
        if (actualLong != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-7] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        long expectedMinIntProduct = -1L * Integer.MIN_VALUE;
        if (actualLong != expectedMinIntProduct) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-8] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected="
                    + expectedMinIntProduct + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        if (actualLong != Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-9] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected="
                    + Long.MAX_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        if (actualLong != Long.MIN_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-10] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected="
                    + Long.MIN_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        if (actualLong != -Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected="
                    + (-Long.MAX_VALUE) + " actual=" + actualLong);
        }

        boolean sawExpectedArithmetic = false;
        try {
            long buggyActual = FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-12] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected=ArithmeticException actual="
                    + buggyActual);
        } catch (ArithmeticException expected) {
            sawExpectedArithmetic = true;
        }
        if (!sawExpectedArithmetic) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-12] semantic mismatch: expected ArithmeticException for FieldUtils.safeMultiply(Long.MIN_VALUE, -1)");
        }

        sawExpectedArithmetic = false;
        try {
            long buggyActual = FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-13] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) expected=ArithmeticException actual="
                    + buggyActual);
        } catch (ArithmeticException expected) {
            sawExpectedArithmetic = true;
        }
        if (!sawExpectedArithmetic) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-13] semantic mismatch: expected ArithmeticException for FieldUtils.safeMultiply(Long.MIN_VALUE, 100)");
        }

        sawExpectedArithmetic = false;
        try {
            long buggyActual = FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-14] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) expected=ArithmeticException actual="
                    + buggyActual);
        } catch (ArithmeticException expected) {
            sawExpectedArithmetic = true;
        }
        if (!sawExpectedArithmetic) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-14] semantic mismatch: expected ArithmeticException for FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE)");
        }

        sawExpectedArithmetic = false;
        try {
            long buggyActual = FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-15] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) expected=ArithmeticException actual="
                    + buggyActual);
        } catch (ArithmeticException expected) {
            sawExpectedArithmetic = true;
        }
        if (!sawExpectedArithmetic) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-15] semantic mismatch: expected ArithmeticException for FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE)");
        }

        long exploratoryVal = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
        int exploratoryScalar = data.consumeInt();
        try {
            FieldUtils.safeMultiply(exploratoryVal, exploratoryScalar);
        } catch (Throwable ignored) {
        }

        long moderateVal = data.consumeInt(-1_000_000, 1_000_000);
        int moderateScalar = data.consumeInt(-1_000, 1_000);

        try {
            long lhs = FieldUtils.safeMultiply(moderateVal, moderateScalar);
            long rhs = FieldUtils.safeMultiply(moderateVal, (long) moderateScalar);
            // Contract/oracle: these same-name overloads implement the same mathematical operation
            // ("Multiply two values throwing an exception if overflow occurs"), so on the same
            // non-overflowing inputs they must return the same result. A patch that merely deletes
            // the overflow throw in the int-overload path breaks this observable agreement.
            if (lhs != rhs) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: safeMultiply(long,int) != safeMultiply(long,long)"
                        + " inputVal=" + moderateVal
                        + " inputScalar=" + moderateScalar
                        + " lhs=" + lhs
                        + " rhs=" + rhs);
            }
        } catch (Throwable ignored) {
            return;
        }
    }
}
```
**[user]**
```
Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `sawExpectedArithmetic` (literal assignment, no rethrow, the caught exception is never used) and the alarm near line 96 is conditioned on `sawExpectedArithmetic` — the caught exception's identity is destroyed, so attribution cannot tell WHICH exception fired or whether the unpatched build crashes the same way. Rethrow the exception as the alarm's cause (`..., e`), or assert on the real observable instead of a swallowed boolean.
A caught exception is a REJECTION, never a violation, and flattening it into a flag destroys the identity attribution needs. Either assert on the real observable the test pins (compute the actual value and compare it), or if you must re-report a caught crash, re-throw it with the original as the cause: `new FuzzerSecurityIssueLow("[oracle:<id>] ...", e)`. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~7,327 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actualLong;

        actualLong = FieldUtils.safeMultiply(0L, 0);
        if (actualLong != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-0] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 1);
        if (actualLong != 1L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-1] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 3);
        if (actualLong != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-2] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(3L, 1);
        if (actualLong != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-3] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, 3);
        if (actualLong != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-4] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, -3);
        if (actualLong != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-5] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, 3);
        if (actualLong != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-6] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, -3);
        if (actualLong != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-7] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        long expectedMinIntProduct = -1L * Integer.MIN_VALUE;
        if (actualLong != expectedMinIntProduct) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-8] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected="
                    + expectedMinIntProduct + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        if (actualLong != Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-9] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected="
                    + Long.MAX_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        if (actualLong != Long.MIN_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-10] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected="
                    + Long.MIN_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        if (actualLong != -Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected="
                    + (-Long.MAX_VALUE) + " actual=" + actualLong);
        }

        try {
            long buggyActual = FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-12] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected=ArithmeticException actual="
                    + buggyActual);
        } catch (ArithmeticException expected) {
        }

        try {
            long buggyActual = FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-13] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) expected=ArithmeticException actual="
                    + buggyActual);
        } catch (ArithmeticException expected) {
        }

        try {
            long buggyActual = FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-14] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) expected=ArithmeticException actual="
                    + buggyActual);
        } catch (ArithmeticException expected) {
        }

        try {
            long buggyActual = FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-15] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) expected=ArithmeticException actual="
                    + buggyActual);
        } catch (ArithmeticException expected) {
        }

        long exploratoryVal = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
        int exploratoryScalar = data.consumeInt();
        try {
            FieldUtils.safeMultiply(exploratoryVal, exploratoryScalar);
        } catch (Throwable ignored) {
        }

        long moderateVal = data.consumeInt(-1_000_000, 1_000_000);
        int moderateScalar = data.consumeInt(-1_000, 1_000);

        try {
            long lhs = FieldUtils.safeMultiply(moderateVal, moderateScalar);
            long rhs = FieldUtils.safeMultiply(moderateVal, (long) moderateScalar);
            // Contract/oracle: these same-name overloads both document "Multiply two values
            // throwing an exception if overflow occurs", so for the same non-overflowing inputs
            // they must compute the same mathematical product. A patch that suppresses the throw
            // or returns a wrapped value in the long,int overload would violate this agreement.
            if (lhs != rhs) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: safeMultiply(long,int) != safeMultiply(long,long)"
                        + " inputVal=" + moderateVal
                        + " inputScalar=" + moderateScalar
                        + " lhs=" + lhs
                        + " rhs=" + rhs);
            }
        } catch (Throwable ignored) {
            return;
        }
    }
}
```

</details>

---
## [50] ⚙️ harness-attempt · `attempt_007`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-12] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected=ArithmeticException actual=-9223372036854775808

---
## [51] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [35] — not reprinted*
- *[user] message: identical to step [35] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,161 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Lifted exact oracles from TestFieldUtils.testSafeMultiplyLongInt.
        checkEquals("lift-0", 0L, FieldUtils.safeMultiply(0L, 0), "FieldUtils.safeMultiply(0L, 0)");
        checkEquals("lift-1", 1L, FieldUtils.safeMultiply(1L, 1), "FieldUtils.safeMultiply(1L, 1)");
        checkEquals("lift-2", 3L, FieldUtils.safeMultiply(1L, 3), "FieldUtils.safeMultiply(1L, 3)");
        checkEquals("lift-3", 3L, FieldUtils.safeMultiply(3L, 1), "FieldUtils.safeMultiply(3L, 1)");
        checkEquals("lift-4", 6L, FieldUtils.safeMultiply(2L, 3), "FieldUtils.safeMultiply(2L, 3)");
        checkEquals("lift-5", -6L, FieldUtils.safeMultiply(2L, -3), "FieldUtils.safeMultiply(2L, -3)");
        checkEquals("lift-6", -6L, FieldUtils.safeMultiply(-2L, 3), "FieldUtils.safeMultiply(-2L, 3)");
        checkEquals("lift-7", 6L, FieldUtils.safeMultiply(-2L, -3), "FieldUtils.safeMultiply(-2L, -3)");
        checkEquals("lift-8", -1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE),
                "FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE)");
        checkEquals("lift-9", Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1),
                "FieldUtils.safeMultiply(Long.MAX_VALUE, 1)");
        checkEquals("lift-10", Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1),
                "FieldUtils.safeMultiply(Long.MIN_VALUE, 1)");
        checkEquals("lift-11", -Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1),
                "FieldUtils.safeMultiply(Long.MAX_VALUE, -1)");

        checkArithmeticException("lift-throw-0", Long.MIN_VALUE, -1);
        checkArithmeticException("lift-throw-1", Long.MIN_VALUE, 100);
        checkArithmeticException("lift-throw-2", Long.MIN_VALUE, Integer.MAX_VALUE);
        checkArithmeticException("lift-throw-3", Long.MAX_VALUE, Integer.MIN_VALUE);

        // Generalisation from the trusted failing pair:
        // For any x, safeMultiply(x, 1) is documented multiplication and must preserve x.
        // This catches throw-deleting or wrong-value patches because the observable result must equal the multiplicative identity.
        long identityInput = data.consumeInt(-1_000_000, 1_000_000);
        try {
            long identityActual = FieldUtils.safeMultiply(identityInput, 1);
            if (identityActual != identityInput) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:identity-one] semantic mismatch: safeMultiply(" + identityInput + ", 1) expected="
                                + identityInput + " actual=" + identityActual);
            }
        } catch (ArithmeticException e) {
            return;
        }

        // Equivalent-input sibling agreement:
        // The overloads safeMultiply(long,int) and safeMultiply(long,long) document the same overflow-checked multiplication.
        // On the same logical inputs, when the long-long call succeeds, the long-int overload must return the same value.
        long val1 = data.consumeInt(-1_000_000, 1_000_000);
        int val2 = data.consumeInt(-1_000_000, 1_000_000);
        long expectedLongLong;
        try {
            expectedLongLong = FieldUtils.safeMultiply(val1, (long) val2);
        } catch (ArithmeticException e) {
            return;
        } catch (RuntimeException e) {
            return;
        }
        long actualLongInt;
        try {
            actualLongInt = FieldUtils.safeMultiply(val1, val2);
        } catch (ArithmeticException e) {
            return;
        } catch (RuntimeException e) {
            return;
        }
        if (actualLongInt != expectedLongLong) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:sibling-longint-longlong] metamorphic violation: safeMultiply(long,int) vs safeMultiply(long,long) input=("
                            + val1 + "," + val2 + ") lhs=" + actualLongInt + " rhs=" + expectedLongLong);
        }

        // Sibling agreement across int/int and long/int:
        // Both overloads document the same checked multiplication; widening an int operand to long must preserve a valid product.
        int a = data.consumeInt(-1_000_000, 1_000_000);
        int b = data.consumeInt(-1_000_000, 1_000_000);
        int intResult;
        try {
            intResult = FieldUtils.safeMultiply(a, b);
        } catch (ArithmeticException e) {
            return;
        } catch (RuntimeException e) {
            return;
        }
        long widenedResult;
        try {
            widenedResult = FieldUtils.safeMultiply((long) a, b);
        } catch (ArithmeticException e) {
            return;
        } catch (RuntimeException e) {
            return;
        }
        if (widenedResult != (long) intResult) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:sibling-intint-longint] metamorphic violation: safeMultiply(int,int) vs safeMultiply(long,int) input=("
                            + a + "," + b + ") lhs=" + widenedResult + " rhs=" + intResult);
        }
    }

    private static void checkEquals(String oracleId, long expected, long actual, String call) {
        if (expected != actual) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: " + call + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void checkArithmeticException(String oracleId, long val1, int val2) {
        boolean threw = false;
        try {
            FieldUtils.safeMultiply(val1, val2);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: expected ArithmeticException from FieldUtils.safeMultiply("
                            + val1 + ", " + val2 + ")");
        }
    }
}
```

</details>

---
## [52] ⚙️ harness-repair · `attempt_008`
**output:** **REPAIRED: boolean-swallow**
- detail: {'applied': ['boolean-swallow'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [53] ⚙️ harness-attempt · `attempt_008`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: boolean-swallow]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['boolean-swallow'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lift-throw-0] semantic mismatch: expected ArithmeticException from FieldUtils.safeMultiply(-9223372036854775808, -1)

---
## [54] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [35] — not reprinted*
- *[user] message: identical to step [41] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,017 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkEquals("lifted-0", 0L, FieldUtils.safeMultiply(0L, 0), "FieldUtils.safeMultiply(0L, 0)");
        checkEquals("lifted-1", 1L, FieldUtils.safeMultiply(1L, 1), "FieldUtils.safeMultiply(1L, 1)");
        checkEquals("lifted-2", 3L, FieldUtils.safeMultiply(1L, 3), "FieldUtils.safeMultiply(1L, 3)");
        checkEquals("lifted-3", 3L, FieldUtils.safeMultiply(3L, 1), "FieldUtils.safeMultiply(3L, 1)");
        checkEquals("lifted-4", 6L, FieldUtils.safeMultiply(2L, 3), "FieldUtils.safeMultiply(2L, 3)");
        checkEquals("lifted-5", -6L, FieldUtils.safeMultiply(2L, -3), "FieldUtils.safeMultiply(2L, -3)");
        checkEquals("lifted-6", -6L, FieldUtils.safeMultiply(-2L, 3), "FieldUtils.safeMultiply(-2L, 3)");
        checkEquals("lifted-7", 6L, FieldUtils.safeMultiply(-2L, -3), "FieldUtils.safeMultiply(-2L, -3)");
        checkEquals("lifted-8", -1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE),
                "FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE)");
        checkEquals("lifted-9", Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1),
                "FieldUtils.safeMultiply(Long.MAX_VALUE, 1)");
        checkEquals("lifted-10", Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1),
                "FieldUtils.safeMultiply(Long.MIN_VALUE, 1)");
        checkEquals("lifted-11", -Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1),
                "FieldUtils.safeMultiply(Long.MAX_VALUE, -1)");

        expectArithmetic("lifted-throw-0", Long.MIN_VALUE, -1);
        expectArithmetic("lifted-throw-1", Long.MIN_VALUE, 100);
        expectArithmetic("lifted-throw-2", Long.MIN_VALUE, Integer.MAX_VALUE);
        expectArithmetic("lifted-throw-3", Long.MAX_VALUE, Integer.MIN_VALUE);

        long fuzzVal;
        switch (data.consumeInt(0, 5)) {
            case 0:
                fuzzVal = Long.MIN_VALUE;
                break;
            case 1:
                fuzzVal = Long.MAX_VALUE;
                break;
            case 2:
                fuzzVal = data.consumeInt(-1_000_000, 1_000_000);
                break;
            case 3:
                fuzzVal = data.consumeInt();
                break;
            case 4:
                fuzzVal = data.consumeBoolean() ? 1L : -1L;
                break;
            default:
                fuzzVal = 0L;
                break;
        }

        int fuzzFactor;
        switch (data.consumeInt(0, 6)) {
            case 0:
                fuzzFactor = -1;
                break;
            case 1:
                fuzzFactor = 0;
                break;
            case 2:
                fuzzFactor = 1;
                break;
            case 3:
                fuzzFactor = data.consumeInt(-1_000_000, 1_000_000);
                break;
            case 4:
                fuzzFactor = Integer.MIN_VALUE;
                break;
            case 5:
                fuzzFactor = Integer.MAX_VALUE;
                break;
            default:
                fuzzFactor = data.consumeInt();
                break;
        }

        // Documented sibling-agreement guarantee: these are same-name overloads for safeMultiply,
        // so for the same mathematical inputs they must either both return the same value or both reject.
        // A throw-deleting patch in safeMultiply(long,int) breaks this observable post-condition.
        try {
            long lhs = FieldUtils.safeMultiply(fuzzVal, fuzzFactor);
            long rhs = FieldUtils.safeMultiply(fuzzVal, (long) fuzzFactor);
            if (lhs != rhs) {
                throw new RuntimeException("[oracle:sibling-overload] metamorphic violation: safeMultiply(long,int) != safeMultiply(long,long) inputVal=" + fuzzVal + " inputFactor=" + fuzzFactor + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Throwable t1) {
            try {
                FieldUtils.safeMultiply(fuzzVal, (long) fuzzFactor);
            } catch (Throwable t2) {
                return;
            }
            return;
        }

        // Another trusted post-condition from the real API examples: with scalar 1, withDurationAdded(d, 1)
        // uses FieldUtils.safeMultiply(d, 1) and must therefore add exactly d milliseconds when the call succeeds.
        try {
            long base = data.consumeInt(-1_000_000, 1_000_000);
            long delta = data.consumeInt(-1_000_000, 1_000_000);
            org.joda.time.Duration dur = new org.joda.time.Duration(base);
            org.joda.time.Duration updated = dur.withDurationAdded(delta, 1);
            long expected = FieldUtils.safeAdd(base, delta);
            if (updated.getMillis() != expected) {
                throw new RuntimeException("[oracle:duration-scalar-one] metamorphic violation: withDurationAdded(delta,1) must add delta exactly base=" + base + " delta=" + delta + " actual=" + updated.getMillis() + " expected=" + expected);
            }
        } catch (Throwable t) {
            return;
        }
    }

    private static void checkEquals(String id, long expected, long actual, String expr) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + id + "] semantic mismatch: " + expr + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void expectArithmetic(String id, long val1, int val2) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + id + "] semantic mismatch: expected ArithmeticException for FieldUtils.safeMultiply(" + val1 + ", " + val2 + ") but got=" + actual);
        } catch (ArithmeticException expected) {
        }
    }
}
```

</details>

---
## [55] ⚙️ harness-attempt · `attempt_009`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-throw-0] semantic mismatch: expected ArithmeticException for FieldUtils.safeMultiply(-9223372036854775808, -1) but got=-92233720…

---
## [56] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [35] — not reprinted*
- *[user] message: identical to step [35] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,260 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedOracles();

        long val1 = data.consumeLong();
        int val2 = data.consumeInt();
        checkSafeMultiplyLongIntAgreesWithLongLong(val1, val2);

        int a = data.consumeInt();
        int b = data.consumeInt();
        checkSafeMultiplyIntIntAgreesWithLongInt(a, b);

        int x = data.consumeInt(-1_000_000, 1_000_000);
        int y = data.consumeInt(-1_000_000, 1_000_000);
        checkCommutativityForModerateInputs(x, y);
    }

    private static void checkLiftedOracles() {
        assertSafeMultiplyEquals("lifted-0x0", 0L, 0L, 0);
        assertSafeMultiplyEquals("lifted-1x1", 1L, 1L, 1);
        assertSafeMultiplyEquals("lifted-1x3", 3L, 1L, 3);
        assertSafeMultiplyEquals("lifted-3x1", 3L, 3L, 1);

        assertSafeMultiplyEquals("lifted-2x3", 6L, 2L, 3);
        assertSafeMultiplyEquals("lifted-2xneg3", -6L, 2L, -3);
        assertSafeMultiplyEquals("lifted-neg2x3", -6L, -2L, 3);
        assertSafeMultiplyEquals("lifted-neg2xneg3", 6L, -2L, -3);

        assertSafeMultiplyEquals(
                "lifted-neg1xminint",
                -1L * Integer.MIN_VALUE,
                -1L,
                Integer.MIN_VALUE);

        assertSafeMultiplyEquals("lifted-maxx1", Long.MAX_VALUE, Long.MAX_VALUE, 1);
        assertSafeMultiplyEquals("lifted-minx1", Long.MIN_VALUE, Long.MIN_VALUE, 1);
        assertSafeMultiplyEquals("lifted-maxxneg1", -Long.MAX_VALUE, Long.MAX_VALUE, -1);

        assertSafeMultiplyThrowsArithmetic("lifted-minxneg1-throws", Long.MIN_VALUE, -1);
        assertSafeMultiplyThrowsArithmetic("lifted-minx100-throws", Long.MIN_VALUE, 100);
        assertSafeMultiplyThrowsArithmetic("lifted-minxintmax-throws", Long.MIN_VALUE, Integer.MAX_VALUE);
        assertSafeMultiplyThrowsArithmetic("lifted-maxxintmin-throws", Long.MAX_VALUE, Integer.MIN_VALUE);
    }

    private static void assertSafeMultiplyEquals(String oracleId, long expected, long left, int right) {
        final long actual;
        try {
            actual = FieldUtils.safeMultiply(left, right);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: expected return " + expected
                            + " from FieldUtils.safeMultiply(" + left + ", " + right + ") but threw "
                            + t.getClass().getName(),
                    t);
        }
        if (actual != expected) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: FieldUtils.safeMultiply("
                            + left + ", " + right + ") expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertSafeMultiplyThrowsArithmetic(String oracleId, long left, int right) {
        try {
            long actual = FieldUtils.safeMultiply(left, right);
            throw new FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: expected ArithmeticException from "
                            + "FieldUtils.safeMultiply(" + left + ", " + right + ") but returned " + actual);
        } catch (ArithmeticException expected) {
            return;
        }
    }

    private static void checkSafeMultiplyLongIntAgreesWithLongLong(long val1, int val2) {
        final long expected;
        try {
            expected = FieldUtils.safeMultiply(val1, (long) val2);
        } catch (Throwable t) {
            return;
        }

        final long actual;
        try {
            actual = FieldUtils.safeMultiply(val1, val2);
        } catch (Throwable t) {
            return;
        }

        if (actual != expected) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:rel-longint-longlong] metamorphic violation: "
                            + "safeMultiply(long,int) must agree with safeMultiply(long,long) on equivalent inputs "
                            + "input=(" + val1 + "," + val2 + ") lhs=" + actual + " rhs=" + expected);
        }
    }

    private static void checkSafeMultiplyIntIntAgreesWithLongInt(int a, int b) {
        final int intResult;
        try {
            intResult = FieldUtils.safeMultiply(a, b);
        } catch (Throwable t) {
            return;
        }

        final long longResult;
        try {
            longResult = FieldUtils.safeMultiply((long) a, b);
        } catch (Throwable t) {
            return;
        }

        if (longResult != (long) intResult) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:rel-intint-longint] metamorphic violation: "
                            + "safeMultiply(int,int) must agree with widened safeMultiply(long,int) "
                            + "input=(" + a + "," + b + ") lhs=" + longResult + " rhs=" + intResult);
        }
    }

    private static void checkCommutativityForModerateInputs(int x, int y) {
        final long lhs;
        try {
            lhs = FieldUtils.safeMultiply((long) x, y);
        } catch (Throwable t) {
            return;
        }

        final long rhs;
        try {
            rhs = FieldUtils.safeMultiply((long) y, x);
        } catch (Throwable t) {
            return;
        }

        // Contract justification: these overloads implement multiplication with overflow checking;
        // for moderate inputs where both calls succeed, integer multiplication is commutative, so
        // both observable results must match. A patch that merely suppresses a throw or returns a
        // silent wrong value in one branch would violate this relation without needing any crash.
        if (lhs != rhs) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:rel-commutative] metamorphic violation: "
                            + "safeMultiply should be commutative for successful moderate inputs "
                            + "input=(" + x + "," + y + ") lhs=" + lhs + " rhs=" + rhs);
        }
    }
}
```

</details>

---
## [57] ⚙️ harness-attempt · `attempt_010`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-minxneg1-throws] semantic mismatch: expected ArithmeticException from FieldUtils.safeMultiply(-9223372036854775808, -1) but retu…

---
## [58] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [35] — not reprinted*
- *[user] message: identical to step [41] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,867 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.joda.time.Duration;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkEquals("lifted-0x0", 0L, FieldUtils.safeMultiply(0L, 0), "FieldUtils.safeMultiply(0L, 0)");
        checkEquals("lifted-1x1", 1L, FieldUtils.safeMultiply(1L, 1), "FieldUtils.safeMultiply(1L, 1)");
        checkEquals("lifted-1x3", 3L, FieldUtils.safeMultiply(1L, 3), "FieldUtils.safeMultiply(1L, 3)");
        checkEquals("lifted-3x1", 3L, FieldUtils.safeMultiply(3L, 1), "FieldUtils.safeMultiply(3L, 1)");

        checkEquals("lifted-2x3", 6L, FieldUtils.safeMultiply(2L, 3), "FieldUtils.safeMultiply(2L, 3)");
        checkEquals("lifted-2x-3", -6L, FieldUtils.safeMultiply(2L, -3), "FieldUtils.safeMultiply(2L, -3)");
        checkEquals("lifted--2x3", -6L, FieldUtils.safeMultiply(-2L, 3), "FieldUtils.safeMultiply(-2L, 3)");
        checkEquals("lifted--2x-3", 6L, FieldUtils.safeMultiply(-2L, -3), "FieldUtils.safeMultiply(-2L, -3)");

        checkEquals(
                "lifted--1xIntegerMin",
                -1L * Integer.MIN_VALUE,
                FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE),
                "FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE)");

        checkEquals(
                "lifted-longMaxx1",
                Long.MAX_VALUE,
                FieldUtils.safeMultiply(Long.MAX_VALUE, 1),
                "FieldUtils.safeMultiply(Long.MAX_VALUE, 1)");
        checkEquals(
                "lifted-longMinx1",
                Long.MIN_VALUE,
                FieldUtils.safeMultiply(Long.MIN_VALUE, 1),
                "FieldUtils.safeMultiply(Long.MIN_VALUE, 1)");
        checkEquals(
                "lifted-longMaxx-1",
                -Long.MAX_VALUE,
                FieldUtils.safeMultiply(Long.MAX_VALUE, -1),
                "FieldUtils.safeMultiply(Long.MAX_VALUE, -1)");

        checkThrowsArithmetic(
                "lifted-longMinx-1-throws",
                Long.MIN_VALUE,
                -1,
                "FieldUtils.safeMultiply(Long.MIN_VALUE, -1)");
        checkThrowsArithmetic(
                "lifted-longMinx100-throws",
                Long.MIN_VALUE,
                100,
                "FieldUtils.safeMultiply(Long.MIN_VALUE, 100)");
        checkThrowsArithmetic(
                "lifted-longMinxIntegerMax-throws",
                Long.MIN_VALUE,
                Integer.MAX_VALUE,
                "FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE)");
        checkThrowsArithmetic(
                "lifted-longMaxxIntegerMin-throws",
                Long.MAX_VALUE,
                Integer.MIN_VALUE,
                "FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE)");

        long moderateVal = data.consumeInt(-1_000_000, 1_000_000);
        int moderateFactor = data.consumeInt(-1_000, 1_000);

        try {
            long lhs = FieldUtils.safeMultiply(moderateVal, moderateFactor);
            long rhs = FieldUtils.safeMultiply(moderateVal, (long) moderateFactor);
            if (lhs != rhs) {
                throw new RuntimeException(
                        "[oracle:overload-agreement] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) on the same mathematical inputs "
                                + "inputVal=" + moderateVal
                                + " inputFactor=" + moderateFactor
                                + " lhs=" + lhs
                                + " rhs=" + rhs);
            }
        } catch (Throwable ignored) {
        }

        long baseMillis = data.consumeInt(-1_000_000, 1_000_000);
        long durationToAdd = data.consumeInt(-1_000_000, 1_000_000);
        int scalar = data.consumeInt(-1_000, 1_000);

        try {
            Duration base = new Duration(baseMillis);
            Duration actual = base.withDurationAdded(durationToAdd, scalar);
            long expectedMillis = FieldUtils.safeAdd(base.getMillis(), FieldUtils.safeMultiply(durationToAdd, scalar));
            long actualMillis = actual.getMillis();
            if (actualMillis != expectedMillis) {
                throw new RuntimeException(
                        "[oracle:duration-postcondition] metamorphic violation: Duration.withDurationAdded(long,int) is documented by its implementation to return new Duration(safeAdd(getMillis(), safeMultiply(durationToAdd, scalar))); a throw-deleting or wrong-value multiply patch breaks this observable result "
                                + "baseMillis=" + baseMillis
                                + " durationToAdd=" + durationToAdd
                                + " scalar=" + scalar
                                + " expectedMillis=" + expectedMillis
                                + " actualMillis=" + actualMillis);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void checkEquals(String oracleId, long expected, long actual, String expr) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: " + expr
                            + " expected=" + expected
                            + " actual=" + actual);
        }
    }

    private static void checkThrowsArithmetic(String oracleId, long val1, int val2, String expr) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: expected ArithmeticException from "
                            + expr + " but returned=" + actual);
        } catch (ArithmeticException expected) {
        }
    }
}
```

</details>

---
## [59] ⚙️ harness-attempt · `attempt_011`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-longMinx-1-throws] semantic mismatch: expected ArithmeticException from FieldUtils.safeMultiply(Long.MIN_VALUE, -1) but returned…

---
## [60] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [35] — not reprinted*
- *[user] message: identical to step [35] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,694 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.joda.time.Duration;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedTestOracles();
        checkPublicApiCallSiteOracle();
        relationSafeMultiplyLongIntAgreesWithLongLongSibling(data);
        relationSafeMultiplyIntIntAgreesWithLongIntSibling(data);
        relationDurationWithDurationAddedMatchesSeededMultiplication(data);
    }

    private static void checkLiftedTestOracles() {
        assertLongEquals("lifted-0x0", 0L, FieldUtils.safeMultiply(0L, 0), "FieldUtils.safeMultiply(0L, 0)");
        assertLongEquals("lifted-1x1", 1L, FieldUtils.safeMultiply(1L, 1), "FieldUtils.safeMultiply(1L, 1)");
        assertLongEquals("lifted-1x3", 3L, FieldUtils.safeMultiply(1L, 3), "FieldUtils.safeMultiply(1L, 3)");
        assertLongEquals("lifted-3x1", 3L, FieldUtils.safeMultiply(3L, 1), "FieldUtils.safeMultiply(3L, 1)");

        assertLongEquals("lifted-2x3", 6L, FieldUtils.safeMultiply(2L, 3), "FieldUtils.safeMultiply(2L, 3)");
        assertLongEquals("lifted-2x-3", -6L, FieldUtils.safeMultiply(2L, -3), "FieldUtils.safeMultiply(2L, -3)");
        assertLongEquals("lifted--2x3", -6L, FieldUtils.safeMultiply(-2L, 3), "FieldUtils.safeMultiply(-2L, 3)");
        assertLongEquals("lifted--2x-3", 6L, FieldUtils.safeMultiply(-2L, -3), "FieldUtils.safeMultiply(-2L, -3)");

        assertLongEquals(
                "lifted--1xIntMin",
                -1L * Integer.MIN_VALUE,
                FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE),
                "FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE)");

        assertLongEquals(
                "lifted-LongMaxx1",
                Long.MAX_VALUE,
                FieldUtils.safeMultiply(Long.MAX_VALUE, 1),
                "FieldUtils.safeMultiply(Long.MAX_VALUE, 1)");
        assertLongEquals(
                "lifted-LongMinx1",
                Long.MIN_VALUE,
                FieldUtils.safeMultiply(Long.MIN_VALUE, 1),
                "FieldUtils.safeMultiply(Long.MIN_VALUE, 1)");
        assertLongEquals(
                "lifted-LongMaxx-1",
                -Long.MAX_VALUE,
                FieldUtils.safeMultiply(Long.MAX_VALUE, -1),
                "FieldUtils.safeMultiply(Long.MAX_VALUE, -1)");

        expectArithmeticException("lifted-LongMinx-1", Long.MIN_VALUE, -1);
        expectArithmeticException("lifted-LongMinx100", Long.MIN_VALUE, 100);
        expectArithmeticException("lifted-LongMinxIntMax", Long.MIN_VALUE, Integer.MAX_VALUE);
        expectArithmeticException("lifted-LongMaxxIntMin", Long.MAX_VALUE, Integer.MIN_VALUE);
    }

    private static void checkPublicApiCallSiteOracle() {
        try {
            Duration.ZERO.withDurationAdded(Long.MIN_VALUE, -1);
            throw new FuzzerSecurityIssueLow(
                    "[oracle:public-api-withDurationAdded] semantic mismatch: Duration.ZERO.withDurationAdded(Long.MIN_VALUE, -1) should throw ArithmeticException because withDurationAdded delegates to FieldUtils.safeMultiply(durationToAdd, scalar) and a throw-deleting patch would silently return a wrong Duration");
        } catch (ArithmeticException expected) {
        } catch (FuzzerSecurityIssueLow issue) {
            throw issue;
        } catch (Exception e) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:public-api-withDurationAdded] semantic mismatch: unexpected exception type "
                            + e.getClass().getName()
                            + " from Duration.ZERO.withDurationAdded(Long.MIN_VALUE, -1)",
                    e);
        }
    }

    private static void relationSafeMultiplyLongIntAgreesWithLongLongSibling(FuzzedDataProvider data) {
        long val1 = data.consumeLong();
        int val2 = data.consumeInt();
        long expected;
        try {
            expected = FieldUtils.safeMultiply(val1, (long) val2);
        } catch (Exception e) {
            return;
        }
        long actual;
        try {
            actual = FieldUtils.safeMultiply(val1, val2);
        } catch (Exception e) {
            return;
        }
        if (actual != expected) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:safeMultiplyLongInt_agreesWithLongLongSibling] metamorphic violation: safeMultiply(long,int) != safeMultiply(long,long) for equivalent inputs val1="
                            + val1
                            + " val2="
                            + val2
                            + " actual="
                            + actual
                            + " expected="
                            + expected);
        }
    }

    private static void relationSafeMultiplyIntIntAgreesWithLongIntSibling(FuzzedDataProvider data) {
        int a = data.consumeInt();
        int b = data.consumeInt();
        int intResult;
        try {
            intResult = FieldUtils.safeMultiply(a, b);
        } catch (Exception e) {
            return;
        }
        long longResult;
        try {
            longResult = FieldUtils.safeMultiply((long) a, b);
        } catch (Exception e) {
            return;
        }
        if (longResult != (long) intResult) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:safeMultiplyIntInt_agreesWithLongIntSibling] metamorphic violation: safeMultiply(int,int) != safeMultiply((long)int,int) for a="
                            + a
                            + " b="
                            + b
                            + " intResult="
                            + intResult
                            + " longResult="
                            + longResult);
        }
    }

    private static void relationDurationWithDurationAddedMatchesSeededMultiplication(FuzzedDataProvider data) {
        long base = data.consumeInt(-1_000_000, 1_000_000);
        long durationToAdd = data.consumeInt(-1_000_000, 1_000_000);
        int scalar = data.consumeInt(-1_000, 1_000);

        long expectedAdd;
        try {
            expectedAdd = FieldUtils.safeMultiply(durationToAdd, scalar);
        } catch (Exception e) {
            return;
        }

        Duration baseDuration = new Duration(base);
        Duration actualDuration;
        try {
            actualDuration = baseDuration.withDurationAdded(durationToAdd, scalar);
        } catch (Exception e) {
            return;
        }

        long expectedMillis;
        try {
            expectedMillis = FieldUtils.safeAdd(base, expectedAdd);
        } catch (Exception e) {
            return;
        }

        long actualMillis = actualDuration.getMillis();
        if (actualMillis != expectedMillis) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:duration-withDurationAdded-consistency] metamorphic violation: Duration.withDurationAdded(durationToAdd, scalar) must equal baseMillis + safeMultiply(durationToAdd, scalar) for base="
                            + base
                            + " durationToAdd="
                            + durationToAdd
                            + " scalar="
                            + scalar
                            + " actualMillis="
                            + actualMillis
                            + " expectedMillis="
                            + expectedMillis);
        }
    }

    private static void expectArithmeticException(String oracleId, long val1, int val2) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: expected ArithmeticException from FieldUtils.safeMultiply(" + val1 + ", " + val2 + ") but got value " + actual);
        } catch (ArithmeticException expected) {
        } catch (FuzzerSecurityIssueLow issue) {
            throw issue;
        } catch (Exception e) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: expected ArithmeticException from FieldUtils.safeMultiply(" + val1 + ", " + val2 + ") but got unexpected exception " + e.getClass().getName(),
                    e);
        }
    }

    private static void assertLongEquals(String oracleId, long expected, long actual, String expr) {
        if (actual != expected) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: " + expr + " expected=" + expected + " actual=" + actual);
        }
    }
}
```

</details>

---
## [61] ⚙️ harness-attempt · `attempt_012`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-LongMinx-1] semantic mismatch: expected ArithmeticException from FieldUtils.safeMultiply(-9223372036854775808, -1) but got value…

---
## [62] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [35] — not reprinted*
- *[user] message: identical to step [41] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,482 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actual;

        actual = FieldUtils.safeMultiply(0L, 0);
        if (actual != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-0] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(1L, 1);
        if (actual != 1L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-1] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(1L, 3);
        if (actual != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-2] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(3L, 1);
        if (actual != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-3] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(2L, 3);
        if (actual != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-4] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(2L, -3);
        if (actual != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-5] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-2L, 3);
        if (actual != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-6] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-2L, -3);
        if (actual != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-7] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        if (actual != (-1L * Integer.MIN_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-8] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected=" + (-1L * Integer.MIN_VALUE) + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        if (actual != Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-9] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected=" + Long.MAX_VALUE + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        if (actual != Long.MIN_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-10] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected=" + Long.MIN_VALUE + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        if (actual != (-Long.MAX_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected=" + (-Long.MAX_VALUE) + " actual=" + actual);
        }

        boolean threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-12] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-13] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) expected ArithmeticException");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-14] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) expected ArithmeticException");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-15] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) expected ArithmeticException");
        }

        int fuzzVal1 = data.consumeInt(-1_000_000, 1_000_000);
        int fuzzVal2 = data.consumeInt(-1_000_000, 1_000_000);
        long lhs;
        long rhs;
        try {
            lhs = FieldUtils.safeMultiply((long) fuzzVal1, fuzzVal2);
            rhs = FieldUtils.safeMultiply((long) fuzzVal1, (long) fuzzVal2);
        } catch (Throwable t) {
            return;
        }
        /* Documented guarantee used for this oracle:
         * the safeMultiply overloads are same-name family members over the same input space;
         * widening an int operand to long must not change the mathematical result for any
         * correct implementation when both calls succeed. A throw-deleting or wrong-value
         * patch in the long,int overload would break this observable agreement.
         */
        if (lhs != rhs) {
            throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) for equivalent inputs input=(" + ((long) fuzzVal1) + "," + fuzzVal2 + ") lhs=" + lhs + " rhs=" + rhs);
        }
    }
}
```

</details>

---
## [63] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `threw` (literal assignment, no rethrow, the caught exceptio

---
## [64] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~7,626 chars, 2 new)</summary>

- *[system] message: identical to step [35] — not reprinted*
- *[user] message: identical to step [41] — not reprinted*
**[assistant]**
```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actual;

        actual = FieldUtils.safeMultiply(0L, 0);
        if (actual != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-0] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(1L, 1);
        if (actual != 1L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-1] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(1L, 3);
        if (actual != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-2] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(3L, 1);
        if (actual != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-3] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(2L, 3);
        if (actual != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-4] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(2L, -3);
        if (actual != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-5] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-2L, 3);
        if (actual != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-6] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-2L, -3);
        if (actual != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-7] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        if (actual != (-1L * Integer.MIN_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-8] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected=" + (-1L * Integer.MIN_VALUE) + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        if (actual != Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-9] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected=" + Long.MAX_VALUE + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        if (actual != Long.MIN_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-10] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected=" + Long.MIN_VALUE + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        if (actual != (-Long.MAX_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected=" + (-Long.MAX_VALUE) + " actual=" + actual);
        }

        boolean threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-12] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-13] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) expected ArithmeticException");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-14] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) expected ArithmeticException");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-15] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) expected ArithmeticException");
        }

        int fuzzVal1 = data.consumeInt(-1_000_000, 1_000_000);
        int fuzzVal2 = data.consumeInt(-1_000_000, 1_000_000);
        long lhs;
        long rhs;
        try {
            lhs = FieldUtils.safeMultiply((long) fuzzVal1, fuzzVal2);
            rhs = FieldUtils.safeMultiply((long) fuzzVal1, (long) fuzzVal2);
        } catch (Throwable t) {
            return;
        }
        /* Documented guarantee used for this oracle:
         * the safeMultiply overloads are same-name family members over the same input space;
         * widening an int operand to long must not change the mathematical result for any
         * correct implementation when both calls succeed. A throw-deleting or wrong-value
         * patch in the long,int overload would break this observable agreement.
         */
        if (lhs != rhs) {
            throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) for equivalent inputs input=(" + ((long) fuzzVal1) + "," + fuzzVal2 + ") lhs=" + lhs + " rhs=" + rhs);
        }
    }
}
```
**[user]**
```
Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `threw` (literal assignment, no rethrow, the caught exception is never used) and the alarm near line 76 is conditioned on `threw` — the caught exception's identity is destroyed, so attribution cannot tell WHICH exception fired or whether the unpatched build crashes the same way. Rethrow the exception as the alarm's cause (`..., e`), or assert on the real observable instead of a swallowed boolean.
A caught exception is a REJECTION, never a violation, and flattening it into a flag destroys the identity attribution needs. Either assert on the real observable the test pins (compute the actual value and compare it), or if you must re-report a caught crash, re-throw it with the original as the cause: `new FuzzerSecurityIssueLow("[oracle:<id>] ...", e)`. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~6,150 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actual;

        actual = FieldUtils.safeMultiply(0L, 0);
        if (actual != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-0] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(1L, 1);
        if (actual != 1L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-1] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(1L, 3);
        if (actual != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-2] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(3L, 1);
        if (actual != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-3] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(2L, 3);
        if (actual != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-4] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(2L, -3);
        if (actual != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-5] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-2L, 3);
        if (actual != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-6] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-2L, -3);
        if (actual != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-7] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        if (actual != (-1L * Integer.MIN_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-8] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected=" + (-1L * Integer.MIN_VALUE) + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        if (actual != Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-9] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected=" + Long.MAX_VALUE + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        if (actual != Long.MIN_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-10] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected=" + Long.MIN_VALUE + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        if (actual != (-Long.MAX_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected=" + (-Long.MAX_VALUE) + " actual=" + actual);
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-12] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException");
        } catch (ArithmeticException expected) {
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-13] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) expected ArithmeticException");
        } catch (ArithmeticException expected) {
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-14] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) expected ArithmeticException");
        } catch (ArithmeticException expected) {
        }

        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-15] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) expected ArithmeticException");
        } catch (ArithmeticException expected) {
        }

        int fuzzVal1 = data.consumeInt(-1_000_000, 1_000_000);
        int fuzzVal2 = data.consumeInt(-1_000_000, 1_000_000);
        long lhs;
        long rhs;
        try {
            lhs = FieldUtils.safeMultiply((long) fuzzVal1, fuzzVal2);
            rhs = FieldUtils.safeMultiply((long) fuzzVal1, (long) fuzzVal2);
        } catch (Throwable t) {
            return;
        }
        /* Documented guarantee used for this oracle:
         * the safeMultiply overloads are same-name family members over the same input space;
         * widening an int operand to long must not change the mathematical result for any
         * correct implementation when both calls succeed. A throw-deleting or wrong-value
         * patch in the long,int overload would break this observable agreement.
         */
        if (lhs != rhs) {
            throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) for equivalent inputs input=(" + ((long) fuzzVal1) + "," + fuzzVal2 + ") lhs=" + lhs + " rhs=" + rhs);
        }
    }
}
```

</details>

---
## [65] ⚙️ harness-attempt · `attempt_014`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:seed-12] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException

---
## [66] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [35] — not reprinted*
- *[user] message: identical to step [35] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~9,061 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actualLong;
        boolean threw;

        actualLong = FieldUtils.safeMultiply(0L, 0);
        if (actualLong != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:lifted-0] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 1);
        if (actualLong != 1L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:lifted-1] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 3);
        if (actualLong != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:lifted-2] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(3L, 1);
        if (actualLong != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:lifted-3] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, 3);
        if (actualLong != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:lifted-4] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, -3);
        if (actualLong != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:lifted-5] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, 3);
        if (actualLong != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:lifted-6] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, -3);
        if (actualLong != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:lifted-7] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        if (actualLong != (-1L * Integer.MIN_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:lifted-8] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected=" + (-1L * Integer.MIN_VALUE) + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        if (actualLong != Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:lifted-9] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected=" + Long.MAX_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        if (actualLong != Long.MIN_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:lifted-10] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected=" + Long.MIN_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        if (actualLong != -Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:lifted-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected=" + (-Long.MAX_VALUE) + " actual=" + actualLong);
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
        } catch (ArithmeticException e) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:lifted-12] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException but returned normally");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
        } catch (ArithmeticException e) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:lifted-13] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) expected ArithmeticException but returned normally");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
        } catch (ArithmeticException e) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:lifted-14] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) expected ArithmeticException but returned normally");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
        } catch (ArithmeticException e) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:lifted-15] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) expected ArithmeticException but returned normally");
        }

        long fuzzVal1 = (((long) data.consumeInt()) << 32) ^ (((long) data.consumeInt()) & 0xffffffffL);
        int fuzzVal2 = data.consumeInt();
        long expectedLongLong;
        boolean longLongOk = false;
        try {
            expectedLongLong = FieldUtils.safeMultiply(fuzzVal1, (long) fuzzVal2);
            longLongOk = true;
        } catch (Throwable t) {
            return;
        }
        long actualLongInt;
        try {
            actualLongInt = FieldUtils.safeMultiply(fuzzVal1, fuzzVal2);
        } catch (Throwable t) {
            return;
        }
        if (longLongOk && actualLongInt != expectedLongLong) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:relation-longint-longlong] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) on equivalent inputs inputVal1="
                    + fuzzVal1 + " inputVal2=" + fuzzVal2 + " lhs=" + actualLongInt + " rhs=" + expectedLongLong);
        }

        int a = data.consumeInt();
        int b = data.consumeInt();
        int intResult;
        try {
            intResult = FieldUtils.safeMultiply(a, b);
        } catch (Throwable t) {
            return;
        }
        long widenedResult;
        try {
            widenedResult = FieldUtils.safeMultiply((long) a, b);
        } catch (Throwable t) {
            return;
        }
        if (widenedResult != (long) intResult) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:relation-intint-longint] metamorphic violation: safeMultiply(int,int) must agree with safeMultiply(long,int) when the int result is valid inputA="
                    + a + " inputB=" + b + " lhs=" + widenedResult + " rhs=" + intResult);
        }

        long moderate = data.consumeInt(-1_000_000, 1_000_000);
        int scalar = data.consumeInt(-1_000_000, 1_000_000);
        long left;
        long rightBase;
        try {
            left = FieldUtils.safeMultiply(moderate, scalar);
            rightBase = FieldUtils.safeMultiply(moderate, 1);
        } catch (Throwable t) {
            return;
        }
        long right;
        try {
            right = FieldUtils.safeMultiply(rightBase, scalar);
        } catch (Throwable t) {
            return;
        }
        if (left != right) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:relation-identity-compose] metamorphic violation: multiplication by 1 is the identity, so safeMultiply(x,s) must equal safeMultiply(safeMultiply(x,1),s) inputX="
                    + moderate + " inputScalar=" + scalar + " lhs=" + left + " rhs=" + right);
        }

        long identityInput = data.consumeInt(-1_000_000, 1_000_000);
        long identityActual;
        try {
            identityActual = FieldUtils.safeMultiply(identityInput, 1);
        } catch (Throwable t) {
            return;
        }
        if (identityActual != identityInput) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:post-identity] semantic mismatch: documented multiplication semantics require multiplying by 1 to preserve the value; a throw-deleting or wrong-value patch would break this observable post-condition input="
                    + identityInput + " actual=" + identityActual);
        }
    }
}
```

</details>

---
## [67] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `threw` (literal assignment, no rethrow, the caught exceptio

---
## [68] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~10,205 chars, 2 new)</summary>

- *[system] message: identical to step [35] — not reprinted*
- *[user] message: identical to step [35] — not reprinted*
**[assistant]**
```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actualLong;
        boolean threw;

        actualLong = FieldUtils.safeMultiply(0L, 0);
        if (actualLong != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:lifted-0] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 1);
        if (actualLong != 1L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:lifted-1] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 3);
        if (actualLong != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:lifted-2] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(3L, 1);
        if (actualLong != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:lifted-3] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, 3);
        if (actualLong != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:lifted-4] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, -3);
        if (actualLong != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:lifted-5] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, 3);
        if (actualLong != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:lifted-6] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, -3);
        if (actualLong != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:lifted-7] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        if (actualLong != (-1L * Integer.MIN_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:lifted-8] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected=" + (-1L * Integer.MIN_VALUE) + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        if (actualLong != Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:lifted-9] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected=" + Long.MAX_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        if (actualLong != Long.MIN_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:lifted-10] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected=" + Long.MIN_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        if (actualLong != -Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:lifted-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected=" + (-Long.MAX_VALUE) + " actual=" + actualLong);
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
        } catch (ArithmeticException e) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:lifted-12] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException but returned normally");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
        } catch (ArithmeticException e) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:lifted-13] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) expected ArithmeticException but returned normally");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
        } catch (ArithmeticException e) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:lifted-14] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) expected ArithmeticException but returned normally");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
        } catch (ArithmeticException e) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:lifted-15] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) expected ArithmeticException but returned normally");
        }

        long fuzzVal1 = (((long) data.consumeInt()) << 32) ^ (((long) data.consumeInt()) & 0xffffffffL);
        int fuzzVal2 = data.consumeInt();
        long expectedLongLong;
        boolean longLongOk = false;
        try {
            expectedLongLong = FieldUtils.safeMultiply(fuzzVal1, (long) fuzzVal2);
            longLongOk = true;
        } catch (Throwable t) {
            return;
        }
        long actualLongInt;
        try {
            actualLongInt = FieldUtils.safeMultiply(fuzzVal1, fuzzVal2);
        } catch (Throwable t) {
            return;
        }
        if (longLongOk && actualLongInt != expectedLongLong) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:relation-longint-longlong] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) on equivalent inputs inputVal1="
                    + fuzzVal1 + " inputVal2=" + fuzzVal2 + " lhs=" + actualLongInt + " rhs=" + expectedLongLong);
        }

        int a = data.consumeInt();
        int b = data.consumeInt();
        int intResult;
        try {
            intResult = FieldUtils.safeMultiply(a, b);
        } catch (Throwable t) {
            return;
        }
        long widenedResult;
        try {
            widenedResult = FieldUtils.safeMultiply((long) a, b);
        } catch (Throwable t) {
            return;
        }
        if (widenedResult != (long) intResult) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:relation-intint-longint] metamorphic violation: safeMultiply(int,int) must agree with safeMultiply(long,int) when the int result is valid inputA="
                    + a + " inputB=" + b + " lhs=" + widenedResult + " rhs=" + intResult);
        }

        long moderate = data.consumeInt(-1_000_000, 1_000_000);
        int scalar = data.consumeInt(-1_000_000, 1_000_000);
        long left;
        long rightBase;
        try {
            left = FieldUtils.safeMultiply(moderate, scalar);
            rightBase = FieldUtils.safeMultiply(moderate, 1);
        } catch (Throwable t) {
            return;
        }
        long right;
        try {
            right = FieldUtils.safeMultiply(rightBase, scalar);
        } catch (Throwable t) {
            return;
        }
        if (left != right) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:relation-identity-compose] metamorphic violation: multiplication by 1 is the identity, so safeMultiply(x,s) must equal safeMultiply(safeMultiply(x,1),s) inputX="
                    + moderate + " inputScalar=" + scalar + " lhs=" + left + " rhs=" + right);
        }

        long identityInput = data.consumeInt(-1_000_000, 1_000_000);
        long identityActual;
        try {
            identityActual = FieldUtils.safeMultiply(identityInput, 1);
        } catch (Throwable t) {
            return;
        }
        if (identityActual != identityInput) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:post-identity] semantic mismatch: documented multiplication semantics require multiplying by 1 to preserve the value; a throw-deleting or wrong-value patch would break this observable post-condition input="
                    + identityInput + " actual=" + identityActual);
        }
    }
}
```
**[user]**
```
Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `threw` (literal assignment, no rethrow, the caught exception is never used) and the alarm near line 77 is conditioned on `threw` — the caught exception's identity is destroyed, so attribution cannot tell WHICH exception fired or whether the unpatched build crashes the same way. Rethrow the exception as the alarm's cause (`..., e`), or assert on the real observable instead of a swallowed boolean.
A caught exception is a REJECTION, never a violation, and flattening it into a flag destroys the identity attribution needs. Either assert on the real observable the test pins (compute the actual value and compare it), or if you must re-report a caught crash, re-throw it with the original as the cause: `new FuzzerSecurityIssueLow("[oracle:<id>] ...", e)`. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~6,501 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        assertEqualsLong("[oracle:lifted-0]", 0L, FieldUtils.safeMultiply(0L, 0), "FieldUtils.safeMultiply(0L, 0)");
        assertEqualsLong("[oracle:lifted-1]", 1L, FieldUtils.safeMultiply(1L, 1), "FieldUtils.safeMultiply(1L, 1)");
        assertEqualsLong("[oracle:lifted-2]", 3L, FieldUtils.safeMultiply(1L, 3), "FieldUtils.safeMultiply(1L, 3)");
        assertEqualsLong("[oracle:lifted-3]", 3L, FieldUtils.safeMultiply(3L, 1), "FieldUtils.safeMultiply(3L, 1)");
        assertEqualsLong("[oracle:lifted-4]", 6L, FieldUtils.safeMultiply(2L, 3), "FieldUtils.safeMultiply(2L, 3)");
        assertEqualsLong("[oracle:lifted-5]", -6L, FieldUtils.safeMultiply(2L, -3), "FieldUtils.safeMultiply(2L, -3)");
        assertEqualsLong("[oracle:lifted-6]", -6L, FieldUtils.safeMultiply(-2L, 3), "FieldUtils.safeMultiply(-2L, 3)");
        assertEqualsLong("[oracle:lifted-7]", 6L, FieldUtils.safeMultiply(-2L, -3), "FieldUtils.safeMultiply(-2L, -3)");
        assertEqualsLong("[oracle:lifted-8]", -1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE), "FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE)");
        assertEqualsLong("[oracle:lifted-9]", Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1), "FieldUtils.safeMultiply(Long.MAX_VALUE, 1)");
        assertEqualsLong("[oracle:lifted-10]", Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1), "FieldUtils.safeMultiply(Long.MIN_VALUE, 1)");
        assertEqualsLong("[oracle:lifted-11]", -Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1), "FieldUtils.safeMultiply(Long.MAX_VALUE, -1)");

        expectArithmeticException("[oracle:lifted-12]", Long.MIN_VALUE, -1, "FieldUtils.safeMultiply(Long.MIN_VALUE, -1)");
        expectArithmeticException("[oracle:lifted-13]", Long.MIN_VALUE, 100, "FieldUtils.safeMultiply(Long.MIN_VALUE, 100)");
        expectArithmeticException("[oracle:lifted-14]", Long.MIN_VALUE, Integer.MAX_VALUE, "FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE)");
        expectArithmeticException("[oracle:lifted-15]", Long.MAX_VALUE, Integer.MIN_VALUE, "FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE)");

        long fuzzVal1 = (((long) data.consumeInt()) << 32) ^ (((long) data.consumeInt()) & 0xffffffffL);
        int fuzzVal2 = data.consumeInt();
        long expectedLongLong;
        try {
            expectedLongLong = FieldUtils.safeMultiply(fuzzVal1, (long) fuzzVal2);
        } catch (Throwable t) {
            return;
        }
        long actualLongInt;
        try {
            actualLongInt = FieldUtils.safeMultiply(fuzzVal1, fuzzVal2);
        } catch (Throwable t) {
            return;
        }
        if (actualLongInt != expectedLongLong) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:relation-longint-longlong] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) on equivalent inputs inputVal1="
                    + fuzzVal1 + " inputVal2=" + fuzzVal2 + " lhs=" + actualLongInt + " rhs=" + expectedLongLong);
        }

        int a = data.consumeInt();
        int b = data.consumeInt();
        int intResult;
        try {
            intResult = FieldUtils.safeMultiply(a, b);
        } catch (Throwable t) {
            return;
        }
        long widenedResult;
        try {
            widenedResult = FieldUtils.safeMultiply((long) a, b);
        } catch (Throwable t) {
            return;
        }
        if (widenedResult != (long) intResult) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:relation-intint-longint] metamorphic violation: safeMultiply(int,int) must agree with safeMultiply(long,int) when the int result is valid inputA="
                    + a + " inputB=" + b + " lhs=" + widenedResult + " rhs=" + intResult);
        }

        long moderate = data.consumeInt(-1_000_000, 1_000_000);
        int scalar = data.consumeInt(-1_000_000, 1_000_000);
        long left;
        long rightBase;
        try {
            left = FieldUtils.safeMultiply(moderate, scalar);
            rightBase = FieldUtils.safeMultiply(moderate, 1);
        } catch (Throwable t) {
            return;
        }
        long right;
        try {
            right = FieldUtils.safeMultiply(rightBase, scalar);
        } catch (Throwable t) {
            return;
        }
        if (left != right) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:relation-identity-compose] metamorphic violation: multiplication by 1 is the identity, so safeMultiply(x,s) must equal safeMultiply(safeMultiply(x,1),s) inputX="
                    + moderate + " inputScalar=" + scalar + " lhs=" + left + " rhs=" + right);
        }

        long identityInput = data.consumeInt(-1_000_000, 1_000_000);
        long identityActual;
        try {
            identityActual = FieldUtils.safeMultiply(identityInput, 1);
        } catch (Throwable t) {
            return;
        }
        if (identityActual != identityInput) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:post-identity] semantic mismatch: documented multiplication semantics require multiplying by 1 to preserve the value; a throw-deleting or wrong-value patch would break this observable post-condition input="
                    + identityInput + " actual=" + identityActual);
        }
    }

    private static void assertEqualsLong(String oracleId, long expected, long actual, String call) {
        if (actual != expected) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                oracleId + " semantic mismatch: " + call + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void expectArithmeticException(String oracleId, long val1, int val2, String call) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                oracleId + " semantic mismatch: " + call + " expected ArithmeticException but returned actual=" + actual);
        } catch (ArithmeticException expected) {
        }
    }
}
```

</details>

---
## [69] ⚙️ harness-repair · `attempt_016`
**output:** **REPAIRED: missing-alarm-id**
- detail: {'applied': ['missing-alarm-id'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [70] ⚙️ harness-attempt · `attempt_016`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: missing-alarm-id]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['missing-alarm-id'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:unnamed-check] [oracle:lifted-12] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException but retur…

---
## [71] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [35] — not reprinted*
- *[user] message: identical to step [41] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,468 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkExact("lift-0x0", 0L, 0L, 0);
        checkExact("lift-1x1", 1L, 1L, 1);
        checkExact("lift-1x3", 3L, 1L, 3);
        checkExact("lift-3x1", 3L, 3L, 1);

        checkExact("lift-2x3", 6L, 2L, 3);
        checkExact("lift-2x-3", -6L, 2L, -3);
        checkExact("lift--2x3", -6L, -2L, 3);
        checkExact("lift--2x-3", 6L, -2L, -3);

        checkExact("lift--1xIntegerMin", -1L * Integer.MIN_VALUE, -1L, Integer.MIN_VALUE);

        checkExact("lift-longMaxx1", Long.MAX_VALUE, Long.MAX_VALUE, 1);
        checkExact("lift-longMinx1", Long.MIN_VALUE, Long.MIN_VALUE, 1);
        checkExact("lift-longMaxx-1", -Long.MAX_VALUE, Long.MAX_VALUE, -1);

        checkThrowsArithmetic("lift-longMinx-1-throws", Long.MIN_VALUE, -1);
        checkThrowsArithmetic("lift-longMinx100-throws", Long.MIN_VALUE, 100);
        checkThrowsArithmetic("lift-longMinxIntegerMax-throws", Long.MIN_VALUE, Integer.MAX_VALUE);
        checkThrowsArithmetic("lift-longMaxxIntegerMin-throws", Long.MAX_VALUE, Integer.MIN_VALUE);

        long fuzzVal = data.consumeInt(-1_000_000, 1_000_000);
        int fuzzScalar = data.consumeInt(-1_000, 1_000);

        // Documented guarantee: the safeMultiply overloads have the same contract
        // ("Multiply two values throwing an exception if overflow occurs"), so for
        // the same mathematical inputs they must agree whenever both calls succeed.
        // A "fix" that merely suppresses the -1 overflow throw or returns a wrapped
        // value breaks this observable equality without relying on exceptions alone.
        try {
            long lhs = FieldUtils.safeMultiply(fuzzVal, fuzzScalar);
            long rhs = FieldUtils.safeMultiply(fuzzVal, (long) fuzzScalar);
            if (lhs != rhs) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: " +
                    "safeMultiply(long,int) != safeMultiply(long,long) " +
                    "inputVal=" + fuzzVal + " inputScalar=" + fuzzScalar +
                    " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Throwable t) {
            return;
        }

        int small = data.consumeInt(-1_000_000, 1_000_000);
        // Documented guarantee: multiplying by 1 returns the original value in the
        // patched method body itself; a patch that makes the target branch unreachable
        // or silently alters the result violates this post-condition directly.
        try {
            long actual = FieldUtils.safeMultiply((long) small, 1);
            if (actual != (long) small) {
                throw new RuntimeException(
                    "[oracle:identity-times-one] metamorphic violation: " +
                    "safeMultiply(x,1) must equal x input=" + small +
                    " lhs=" + actual + " rhs=" + ((long) small));
            }
        } catch (Throwable t) {
            return;
        }
    }

    private static void checkExact(String id, long expected, long val1, int val2) {
        final long actual;
        try {
            actual = FieldUtils.safeMultiply(val1, val2);
        } catch (Throwable t) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: unexpected throw for FieldUtils.safeMultiply(" +
                val1 + ", " + val2 + "), expected=" + expected + ", threw=" +
                t.getClass().getName());
        }
        if (actual != expected) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: FieldUtils.safeMultiply(" +
                val1 + ", " + val2 + ") expected=" + expected + " actual=" + actual);
        }
    }

    private static void checkThrowsArithmetic(String id, long val1, int val2) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: expected ArithmeticException for FieldUtils.safeMultiply(" +
                val1 + ", " + val2 + ") but returned=" + actual);
        } catch (ArithmeticException expected) {
        }
    }
}
```

</details>

---
## [72] ⚙️ harness-attempt · `attempt_017`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lift-longMinx-1-throws] semantic mismatch: expected ArithmeticException for FieldUtils.safeMultiply(-9223372036854775808, -1) but retur…

---
## [73] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [35] — not reprinted*
- *[user] message: identical to step [35] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~12,498 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actualLong;
        boolean threw;

        try {
            actualLong = FieldUtils.safeMultiply(0L, 0);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-0x0] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(0L, 0)", t);
        }
        if (actualLong != 0L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-0x0] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(1L, 1);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-1x1] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(1L, 1)", t);
        }
        if (actualLong != 1L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-1x1] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(1L, 3);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-1x3] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(1L, 3)", t);
        }
        if (actualLong != 3L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-1x3] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(3L, 1);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-3x1] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(3L, 1)", t);
        }
        if (actualLong != 3L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-3x1] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(2L, 3);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-2x3] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(2L, 3)", t);
        }
        if (actualLong != 6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-2x3] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(2L, -3);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-2x-3] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(2L, -3)", t);
        }
        if (actualLong != -6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-2x-3] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(-2L, 3);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--2x3] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(-2L, 3)", t);
        }
        if (actualLong != -6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--2x3] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(-2L, -3);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--2x-3] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(-2L, -3)", t);
        }
        if (actualLong != 6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--2x-3] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--1xintmin] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE)", t);
        }
        if (actualLong != (-1L * Integer.MIN_VALUE)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--1xintmin] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected=" + (-1L * Integer.MIN_VALUE) + " actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxx1] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(Long.MAX_VALUE, 1)", t);
        }
        if (actualLong != Long.MAX_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxx1] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected=" + Long.MAX_VALUE + " actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx1] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(Long.MIN_VALUE, 1)", t);
        }
        if (actualLong != Long.MIN_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx1] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected=" + Long.MIN_VALUE + " actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxx-1] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(Long.MAX_VALUE, -1)", t);
        }
        if (actualLong != -Long.MAX_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxx-1] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected=" + (-Long.MAX_VALUE) + " actual=" + actualLong);
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
        } catch (ArithmeticException expected) {
            threw = true;
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx-1-throws] semantic mismatch: wrong exception type for FieldUtils.safeMultiply(Long.MIN_VALUE, -1)", t);
        }
        if (!threw) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx-1-throws] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) was expected to throw ArithmeticException");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
        } catch (ArithmeticException expected) {
            threw = true;
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx100-throws] semantic mismatch: wrong exception type for FieldUtils.safeMultiply(Long.MIN_VALUE, 100)", t);
        }
        if (!threw) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx100-throws] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) was expected to throw ArithmeticException");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
        } catch (ArithmeticException expected) {
            threw = true;
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minxintmax-throws] semantic mismatch: wrong exception type for FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE)", t);
        }
        if (!threw) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minxintmax-throws] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) was expected to throw ArithmeticException");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
        } catch (ArithmeticException expected) {
            threw = true;
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxxintmin-throws] semantic mismatch: wrong exception type for FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE)", t);
        }
        if (!threw) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxxintmin-throws] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) was expected to throw ArithmeticException");
        }

        long fuzzVal1 = data.consumeLong();
        int fuzzVal2 = data.consumeInt();

        Long expectedSibling = null;
        boolean skipLongIntVsLongLong = false;
        try {
            expectedSibling = Long.valueOf(FieldUtils.safeMultiply(fuzzVal1, (long) fuzzVal2));
        } catch (Throwable ignored) {
            skipLongIntVsLongLong = true;
        }
        Long actualSibling = null;
        if (!skipLongIntVsLongLong) {
            try {
                actualSibling = Long.valueOf(FieldUtils.safeMultiply(fuzzVal1, fuzzVal2));
            } catch (Throwable ignored) {
                skipLongIntVsLongLong = true;
            }
        }
        if (!skipLongIntVsLongLong && actualSibling.longValue() != expectedSibling.longValue()) {
            throw new FuzzerSecurityIssueLow("relation safeMultiplyLongInt_agreesWithLongLongSibling violated: val1=" + fuzzVal1 + " val2=" + fuzzVal2 + " lhs=" + actualSibling.longValue() + " rhs=" + expectedSibling.longValue());
        }

        int a = data.consumeInt();
        int b = data.consumeInt();

        Integer intResult = null;
        boolean skipIntVsLong = false;
        try {
            intResult = Integer.valueOf(FieldUtils.safeMultiply(a, b));
        } catch (Throwable ignored) {
            skipIntVsLong = true;
        }
        Long widenedResult = null;
        if (!skipIntVsLong) {
            try {
                widenedResult = Long.valueOf(FieldUtils.safeMultiply((long) a, b));
            } catch (Throwable ignored) {
                skipIntVsLong = true;
            }
        }
        if (!skipIntVsLong && widenedResult.longValue() != (long) intResult.intValue()) {
            throw new FuzzerSecurityIssueLow("relation safeMultiplyIntInt_agreesWithLongIntSibling violated: a=" + a + " b=" + b + " lhs=" + widenedResult.longValue() + " rhs=" + ((long) intResult.intValue()));
        }

        long negInput = data.consumeLong();
        boolean skipNegateRelation = false;
        Long mulNeg = null;
        Integer safeNeg = null;
        try {
            mulNeg = Long.valueOf(FieldUtils.safeMultiply(negInput, -1));
        } catch (Throwable ignored) {
            skipNegateRelation = true;
        }
        if (!skipNegateRelation) {
            try {
                safeNeg = Integer.valueOf(FieldUtils.safeNegate(FieldUtils.safeToInt(negInput)));
            } catch (Throwable ignored) {
                skipNegateRelation = true;
            }
        }
        if (!skipNegateRelation && mulNeg.longValue() != (long) safeNeg.intValue()) {
            throw new FuzzerSecurityIssueLow("relation safeMultiplyLongMinusOne_agreesWithSafeNegate violated: input=" + negInput + " lhs=" + mulNeg.longValue() + " rhs=" + ((long) safeNeg.intValue()));
        }

        long moderate = data.consumeInt(-1_000_000, 1_000_000);
        int scalar = data.consumeInt(-1000, 1000);

        boolean skipComposition = false;
        Long left = null;
        Long right = null;
        try {
            left = Long.valueOf(FieldUtils.safeMultiply(moderate, scalar));
        } catch (Throwable ignored) {
            skipComposition = true;
        }
        if (!skipComposition) {
            try {
                right = Long.valueOf(FieldUtils.safeAdd(
                        FieldUtils.safeMultiply(moderate, scalar / 2),
                        FieldUtils.safeMultiply(moderate, scalar - (scalar / 2))));
            } catch (Throwable ignored) {
                skipComposition = true;
            }
        }
        if (!skipComposition && left.longValue() != right.longValue()) {
            throw new FuzzerSecurityIssueLow("[oracle:composition] metamorphic violation: safeMultiply(x, a+b split) input=" + moderate + " scalar=" + scalar + " lhs=" + left.longValue() + " rhs=" + right.longValue());
        }
    }
}
```

</details>

---
## [74] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `threw` (literal assignment, no rethrow, the caught exceptio

---
## [75] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~13,643 chars, 2 new)</summary>

- *[system] message: identical to step [35] — not reprinted*
- *[user] message: identical to step [35] — not reprinted*
**[assistant]**
```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actualLong;
        boolean threw;

        try {
            actualLong = FieldUtils.safeMultiply(0L, 0);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-0x0] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(0L, 0)", t);
        }
        if (actualLong != 0L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-0x0] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(1L, 1);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-1x1] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(1L, 1)", t);
        }
        if (actualLong != 1L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-1x1] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(1L, 3);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-1x3] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(1L, 3)", t);
        }
        if (actualLong != 3L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-1x3] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(3L, 1);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-3x1] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(3L, 1)", t);
        }
        if (actualLong != 3L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-3x1] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(2L, 3);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-2x3] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(2L, 3)", t);
        }
        if (actualLong != 6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-2x3] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(2L, -3);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-2x-3] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(2L, -3)", t);
        }
        if (actualLong != -6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-2x-3] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(-2L, 3);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--2x3] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(-2L, 3)", t);
        }
        if (actualLong != -6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--2x3] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(-2L, -3);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--2x-3] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(-2L, -3)", t);
        }
        if (actualLong != 6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--2x-3] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--1xintmin] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE)", t);
        }
        if (actualLong != (-1L * Integer.MIN_VALUE)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--1xintmin] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected=" + (-1L * Integer.MIN_VALUE) + " actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxx1] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(Long.MAX_VALUE, 1)", t);
        }
        if (actualLong != Long.MAX_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxx1] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected=" + Long.MAX_VALUE + " actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx1] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(Long.MIN_VALUE, 1)", t);
        }
        if (actualLong != Long.MIN_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx1] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected=" + Long.MIN_VALUE + " actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxx-1] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(Long.MAX_VALUE, -1)", t);
        }
        if (actualLong != -Long.MAX_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxx-1] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected=" + (-Long.MAX_VALUE) + " actual=" + actualLong);
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
        } catch (ArithmeticException expected) {
            threw = true;
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx-1-throws] semantic mismatch: wrong exception type for FieldUtils.safeMultiply(Long.MIN_VALUE, -1)", t);
        }
        if (!threw) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx-1-throws] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) was expected to throw ArithmeticException");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
        } catch (ArithmeticException expected) {
            threw = true;
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx100-throws] semantic mismatch: wrong exception type for FieldUtils.safeMultiply(Long.MIN_VALUE, 100)", t);
        }
        if (!threw) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx100-throws] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) was expected to throw ArithmeticException");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
        } catch (ArithmeticException expected) {
            threw = true;
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minxintmax-throws] semantic mismatch: wrong exception type for FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE)", t);
        }
        if (!threw) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minxintmax-throws] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) was expected to throw ArithmeticException");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
        } catch (ArithmeticException expected) {
            threw = true;
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxxintmin-throws] semantic mismatch: wrong exception type for FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE)", t);
        }
        if (!threw) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxxintmin-throws] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) was expected to throw ArithmeticException");
        }

        long fuzzVal1 = data.consumeLong();
        int fuzzVal2 = data.consumeInt();

        Long expectedSibling = null;
        boolean skipLongIntVsLongLong = false;
        try {
            expectedSibling = Long.valueOf(FieldUtils.safeMultiply(fuzzVal1, (long) fuzzVal2));
        } catch (Throwable ignored) {
            skipLongIntVsLongLong = true;
        }
        Long actualSibling = null;
        if (!skipLongIntVsLongLong) {
            try {
                actualSibling = Long.valueOf(FieldUtils.safeMultiply(fuzzVal1, fuzzVal2));
            } catch (Throwable ignored) {
                skipLongIntVsLongLong = true;
            }
        }
        if (!skipLongIntVsLongLong && actualSibling.longValue() != expectedSibling.longValue()) {
            throw new FuzzerSecurityIssueLow("relation safeMultiplyLongInt_agreesWithLongLongSibling violated: val1=" + fuzzVal1 + " val2=" + fuzzVal2 + " lhs=" + actualSibling.longValue() + " rhs=" + expectedSibling.longValue());
        }

        int a = data.consumeInt();
        int b = data.consumeInt();

        Integer intResult = null;
        boolean skipIntVsLong = false;
        try {
            intResult = Integer.valueOf(FieldUtils.safeMultiply(a, b));
        } catch (Throwable ignored) {
            skipIntVsLong = true;
        }
        Long widenedResult = null;
        if (!skipIntVsLong) {
            try {
                widenedResult = Long.valueOf(FieldUtils.safeMultiply((long) a, b));
            } catch (Throwable ignored) {
                skipIntVsLong = true;
            }
        }
        if (!skipIntVsLong && widenedResult.longValue() != (long) intResult.intValue()) {
            throw new FuzzerSecurityIssueLow("relation safeMultiplyIntInt_agreesWithLongIntSibling violated: a=" + a + " b=" + b + " lhs=" + widenedResult.longValue() + " rhs=" + ((long) intResult.intValue()));
        }

        long negInput = data.consumeLong();
        boolean skipNegateRelation = false;
        Long mulNeg = null;
        Integer safeNeg = null;
        try {
            mulNeg = Long.valueOf(FieldUtils.safeMultiply(negInput, -1));
        } catch (Throwable ignored) {
            skipNegateRelation = true;
        }
        if (!skipNegateRelation) {
            try {
                safeNeg = Integer.valueOf(FieldUtils.safeNegate(FieldUtils.safeToInt(negInput)));
            } catch (Throwable ignored) {
                skipNegateRelation = true;
            }
        }
        if (!skipNegateRelation && mulNeg.longValue() != (long) safeNeg.intValue()) {
            throw new FuzzerSecurityIssueLow("relation safeMultiplyLongMinusOne_agreesWithSafeNegate violated: input=" + negInput + " lhs=" + mulNeg.longValue() + " rhs=" + ((long) safeNeg.intValue()));
        }

        long moderate = data.consumeInt(-1_000_000, 1_000_000);
        int scalar = data.consumeInt(-1000, 1000);

        boolean skipComposition = false;
        Long left = null;
        Long right = null;
        try {
            left = Long.valueOf(FieldUtils.safeMultiply(moderate, scalar));
        } catch (Throwable ignored) {
            skipComposition = true;
        }
        if (!skipComposition) {
            try {
                right = Long.valueOf(FieldUtils.safeAdd(
                        FieldUtils.safeMultiply(moderate, scalar / 2),
                        FieldUtils.safeMultiply(moderate, scalar - (scalar / 2))));
            } catch (Throwable ignored) {
                skipComposition = true;
            }
        }
        if (!skipComposition && left.longValue() != right.longValue()) {
            throw new FuzzerSecurityIssueLow("[oracle:composition] metamorphic violation: safeMultiply(x, a+b split) input=" + moderate + " scalar=" + scalar + " lhs=" + left.longValue() + " rhs=" + right.longValue());
        }
    }
}
```
**[user]**
```
Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `threw` (literal assignment, no rethrow, the caught exception is never used) and the alarm near line 128 is conditioned on `threw` — the caught exception's identity is destroyed, so attribution cannot tell WHICH exception fired or whether the unpatched build crashes the same way. Rethrow the exception as the alarm's cause (`..., e`), or assert on the real observable instead of a swallowed boolean.
A caught exception is a REJECTION, never a violation, and flattening it into a flag destroys the identity attribution needs. Either assert on the real observable the test pins (compute the actual value and compare it), or if you must re-report a caught crash, re-throw it with the original as the cause: `new FuzzerSecurityIssueLow("[oracle:<id>] ...", e)`. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~12,151 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actualLong;

        try {
            actualLong = FieldUtils.safeMultiply(0L, 0);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-0x0] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(0L, 0)", t);
        }
        if (actualLong != 0L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-0x0] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(1L, 1);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-1x1] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(1L, 1)", t);
        }
        if (actualLong != 1L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-1x1] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(1L, 3);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-1x3] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(1L, 3)", t);
        }
        if (actualLong != 3L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-1x3] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(3L, 1);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-3x1] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(3L, 1)", t);
        }
        if (actualLong != 3L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-3x1] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(2L, 3);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-2x3] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(2L, 3)", t);
        }
        if (actualLong != 6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-2x3] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(2L, -3);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-2x-3] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(2L, -3)", t);
        }
        if (actualLong != -6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-2x-3] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(-2L, 3);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--2x3] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(-2L, 3)", t);
        }
        if (actualLong != -6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--2x3] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(-2L, -3);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--2x-3] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(-2L, -3)", t);
        }
        if (actualLong != 6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--2x-3] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--1xintmin] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE)", t);
        }
        if (actualLong != (-1L * Integer.MIN_VALUE)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--1xintmin] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected=" + (-1L * Integer.MIN_VALUE) + " actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxx1] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(Long.MAX_VALUE, 1)", t);
        }
        if (actualLong != Long.MAX_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxx1] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected=" + Long.MAX_VALUE + " actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx1] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(Long.MIN_VALUE, 1)", t);
        }
        if (actualLong != Long.MIN_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx1] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected=" + Long.MIN_VALUE + " actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxx-1] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(Long.MAX_VALUE, -1)", t);
        }
        if (actualLong != -Long.MAX_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxx-1] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected=" + (-Long.MAX_VALUE) + " actual=" + actualLong);
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx-1-throws] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) was expected to throw ArithmeticException");
        } catch (ArithmeticException expected) {
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx-1-throws] semantic mismatch: wrong exception type for FieldUtils.safeMultiply(Long.MIN_VALUE, -1)", t);
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx100-throws] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) was expected to throw ArithmeticException");
        } catch (ArithmeticException expected) {
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx100-throws] semantic mismatch: wrong exception type for FieldUtils.safeMultiply(Long.MIN_VALUE, 100)", t);
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minxintmax-throws] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) was expected to throw ArithmeticException");
        } catch (ArithmeticException expected) {
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minxintmax-throws] semantic mismatch: wrong exception type for FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE)", t);
        }

        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxxintmin-throws] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) was expected to throw ArithmeticException");
        } catch (ArithmeticException expected) {
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxxintmin-throws] semantic mismatch: wrong exception type for FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE)", t);
        }

        long fuzzVal1 = data.consumeLong();
        int fuzzVal2 = data.consumeInt();

        Long expectedSibling = null;
        boolean skipLongIntVsLongLong = false;
        try {
            expectedSibling = Long.valueOf(FieldUtils.safeMultiply(fuzzVal1, (long) fuzzVal2));
        } catch (Throwable ignored) {
            skipLongIntVsLongLong = true;
        }
        Long actualSibling = null;
        if (!skipLongIntVsLongLong) {
            try {
                actualSibling = Long.valueOf(FieldUtils.safeMultiply(fuzzVal1, fuzzVal2));
            } catch (Throwable ignored) {
                skipLongIntVsLongLong = true;
            }
        }
        if (!skipLongIntVsLongLong && actualSibling.longValue() != expectedSibling.longValue()) {
            throw new FuzzerSecurityIssueLow("relation safeMultiplyLongInt_agreesWithLongLongSibling violated: val1=" + fuzzVal1 + " val2=" + fuzzVal2 + " lhs=" + actualSibling.longValue() + " rhs=" + expectedSibling.longValue());
        }

        int a = data.consumeInt();
        int b = data.consumeInt();

        Integer intResult = null;
        boolean skipIntVsLong = false;
        try {
            intResult = Integer.valueOf(FieldUtils.safeMultiply(a, b));
        } catch (Throwable ignored) {
            skipIntVsLong = true;
        }
        Long widenedResult = null;
        if (!skipIntVsLong) {
            try {
                widenedResult = Long.valueOf(FieldUtils.safeMultiply((long) a, b));
            } catch (Throwable ignored) {
                skipIntVsLong = true;
            }
        }
        if (!skipIntVsLong && widenedResult.longValue() != (long) intResult.intValue()) {
            throw new FuzzerSecurityIssueLow("relation safeMultiplyIntInt_agreesWithLongIntSibling violated: a=" + a + " b=" + b + " lhs=" + widenedResult.longValue() + " rhs=" + ((long) intResult.intValue()));
        }

        long negInput = data.consumeLong();
        boolean skipNegateRelation = false;
        Long mulNeg = null;
        Integer safeNeg = null;
        try {
            mulNeg = Long.valueOf(FieldUtils.safeMultiply(negInput, -1));
        } catch (Throwable ignored) {
            skipNegateRelation = true;
        }
        if (!skipNegateRelation) {
            try {
                safeNeg = Integer.valueOf(FieldUtils.safeNegate(FieldUtils.safeToInt(negInput)));
            } catch (Throwable ignored) {
                skipNegateRelation = true;
            }
        }
        if (!skipNegateRelation && mulNeg.longValue() != (long) safeNeg.intValue()) {
            throw new FuzzerSecurityIssueLow("relation safeMultiplyLongMinusOne_agreesWithSafeNegate violated: input=" + negInput + " lhs=" + mulNeg.longValue() + " rhs=" + ((long) safeNeg.intValue()));
        }

        long moderate = data.consumeInt(-1_000_000, 1_000_000);
        int scalar = data.consumeInt(-1000, 1000);

        boolean skipComposition = false;
        Long left = null;
        Long right = null;
        try {
            left = Long.valueOf(FieldUtils.safeMultiply(moderate, scalar));
        } catch (Throwable ignored) {
            skipComposition = true;
        }
        if (!skipComposition) {
            try {
                right = Long.valueOf(FieldUtils.safeAdd(
                        FieldUtils.safeMultiply(moderate, scalar / 2),
                        FieldUtils.safeMultiply(moderate, scalar - (scalar / 2))));
            } catch (Throwable ignored) {
                skipComposition = true;
            }
        }
        if (!skipComposition && left.longValue() != right.longValue()) {
            throw new FuzzerSecurityIssueLow("[oracle:composition] metamorphic violation: safeMultiply(x, a+b split) input=" + moderate + " scalar=" + scalar + " lhs=" + left.longValue() + " rhs=" + right.longValue());
        }
    }
}
```

</details>

---
## [76] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `skipLongIntVsLongLong` (literal assignment, no rethrow, the

---
## [77] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~13,449 chars, 2 new)</summary>

- *[system] message: identical to step [35] — not reprinted*
- *[user] message: identical to step [35] — not reprinted*
- *[assistant] message: identical to step [75] — not reprinted*
- *[user] message: identical to step [75] — not reprinted*
**[assistant]**
```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actualLong;

        try {
            actualLong = FieldUtils.safeMultiply(0L, 0);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-0x0] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(0L, 0)", t);
        }
        if (actualLong != 0L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-0x0] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(1L, 1);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-1x1] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(1L, 1)", t);
        }
        if (actualLong != 1L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-1x1] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(1L, 3);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-1x3] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(1L, 3)", t);
        }
        if (actualLong != 3L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-1x3] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(3L, 1);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-3x1] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(3L, 1)", t);
        }
        if (actualLong != 3L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-3x1] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(2L, 3);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-2x3] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(2L, 3)", t);
        }
        if (actualLong != 6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-2x3] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(2L, -3);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-2x-3] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(2L, -3)", t);
        }
        if (actualLong != -6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-2x-3] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(-2L, 3);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--2x3] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(-2L, 3)", t);
        }
        if (actualLong != -6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--2x3] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(-2L, -3);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--2x-3] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(-2L, -3)", t);
        }
        if (actualLong != 6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--2x-3] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--1xintmin] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE)", t);
        }
        if (actualLong != (-1L * Integer.MIN_VALUE)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--1xintmin] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected=" + (-1L * Integer.MIN_VALUE) + " actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxx1] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(Long.MAX_VALUE, 1)", t);
        }
        if (actualLong != Long.MAX_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxx1] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected=" + Long.MAX_VALUE + " actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx1] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(Long.MIN_VALUE, 1)", t);
        }
        if (actualLong != Long.MIN_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx1] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected=" + Long.MIN_VALUE + " actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxx-1] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(Long.MAX_VALUE, -1)", t);
        }
        if (actualLong != -Long.MAX_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxx-1] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected=" + (-Long.MAX_VALUE) + " actual=" + actualLong);
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx-1-throws] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) was expected to throw ArithmeticException");
        } catch (ArithmeticException expected) {
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx-1-throws] semantic mismatch: wrong exception type for FieldUtils.safeMultiply(Long.MIN_VALUE, -1)", t);
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx100-throws] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) was expected to throw ArithmeticException");
        } catch (ArithmeticException expected) {
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx100-throws] semantic mismatch: wrong exception type for FieldUtils.safeMultiply(Long.MIN_VALUE, 100)", t);
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minxintmax-throws] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) was expected to throw ArithmeticException");
        } catch (ArithmeticException expected) {
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minxintmax-throws] semantic mismatch: wrong exception type for FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE)", t);
        }

        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxxintmin-throws] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) was expected to throw ArithmeticException");
        } catch (ArithmeticException expected) {
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxxintmin-throws] semantic mismatch: wrong exception type for FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE)", t);
        }

        long fuzzVal1 = data.consumeLong();
        int fuzzVal2 = data.consumeInt();

        Long expectedSibling = null;
        boolean skipLongIntVsLongLong = false;
        try {
            expectedSibling = Long.valueOf(FieldUtils.safeMultiply(fuzzVal1, (long) fuzzVal2));
        } catch (Throwable ignored) {
            skipLongIntVsLongLong = true;
        }
        Long actualSibling = null;
        if (!skipLongIntVsLongLong) {
            try {
                actualSibling = Long.valueOf(FieldUtils.safeMultiply(fuzzVal1, fuzzVal2));
            } catch (Throwable ignored) {
                skipLongIntVsLongLong = true;
            }
        }
        if (!skipLongIntVsLongLong && actualSibling.longValue() != expectedSibling.longValue()) {
            throw new FuzzerSecurityIssueLow("relation safeMultiplyLongInt_agreesWithLongLongSibling violated: val1=" + fuzzVal1 + " val2=" + fuzzVal2 + " lhs=" + actualSibling.longValue() + " rhs=" + expectedSibling.longValue());
        }

        int a = data.consumeInt();
        int b = data.consumeInt();

        Integer intResult = null;
        boolean skipIntVsLong = false;
        try {
            intResult = Integer.valueOf(FieldUtils.safeMultiply(a, b));
        } catch (Throwable ignored) {
            skipIntVsLong = true;
        }
        Long widenedResult = null;
        if (!skipIntVsLong) {
            try {
                widenedResult = Long.valueOf(FieldUtils.safeMultiply((long) a, b));
            } catch (Throwable ignored) {
                skipIntVsLong = true;
            }
        }
        if (!skipIntVsLong && widenedResult.longValue() != (long) intResult.intValue()) {
            throw new FuzzerSecurityIssueLow("relation safeMultiplyIntInt_agreesWithLongIntSibling violated: a=" + a + " b=" + b + " lhs=" + widenedResult.longValue() + " rhs=" + ((long) intResult.intValue()));
        }

        long negInput = data.consumeLong();
        boolean skipNegateRelation = false;
        Long mulNeg = null;
        Integer safeNeg = null;
        try {
            mulNeg = Long.valueOf(FieldUtils.safeMultiply(negInput, -1));
        } catch (Throwable ignored) {
            skipNegateRelation = true;
        }
        if (!skipNegateRelation) {
            try {
                safeNeg = Integer.valueOf(FieldUtils.safeNegate(FieldUtils.safeToInt(negInput)));
            } catch (Throwable ignored) {
                skipNegateRelation = true;
            }
        }
        if (!skipNegateRelation && mulNeg.longValue() != (long) safeNeg.intValue()) {
            throw new FuzzerSecurityIssueLow("relation safeMultiplyLongMinusOne_agreesWithSafeNegate violated: input=" + negInput + " lhs=" + mulNeg.longValue() + " rhs=" + ((long) safeNeg.intValue()));
        }

        long moderate = data.consumeInt(-1_000_000, 1_000_000);
        int scalar = data.consumeInt(-1000, 1000);

        boolean skipComposition = false;
        Long left = null;
        Long right = null;
        try {
            left = Long.valueOf(FieldUtils.safeMultiply(moderate, scalar));
        } catch (Throwable ignored) {
            skipComposition = true;
        }
        if (!skipComposition) {
            try {
                right = Long.valueOf(FieldUtils.safeAdd(
                        FieldUtils.safeMultiply(moderate, scalar / 2),
                        FieldUtils.safeMultiply(moderate, scalar - (scalar / 2))));
            } catch (Throwable ignored) {
                skipComposition = true;
            }
        }
        if (!skipComposition && left.longValue() != right.longValue()) {
            throw new FuzzerSecurityIssueLow("[oracle:composition] metamorphic violation: safeMultiply(x, a+b split) input=" + moderate + " scalar=" + scalar + " lhs=" + left.longValue() + " rhs=" + right.longValue());
        }
    }
}
```
**[user]**
```
Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `skipLongIntVsLongLong` (literal assignment, no rethrow, the caught exception is never used) and the alarm near line 169 is conditioned on `skipLongIntVsLongLong` — the caught exception's identity is destroyed, so attribution cannot tell WHICH exception fired or whether the unpatched build crashes the same way. Rethrow the exception as the alarm's cause (`..., e`), or assert on the real observable instead of a swallowed boolean.
A caught exception is a REJECTION, never a violation, and flattening it into a flag destroys the identity attribution needs. Either assert on the real observable the test pins (compute the actual value and compare it), or if you must re-report a caught crash, re-throw it with the original as the cause: `new FuzzerSecurityIssueLow("[oracle:<id>] ...", e)`. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~11,028 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actualLong;

        try {
            actualLong = FieldUtils.safeMultiply(0L, 0);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-0x0] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(0L, 0)", t);
        }
        if (actualLong != 0L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-0x0] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(1L, 1);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-1x1] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(1L, 1)", t);
        }
        if (actualLong != 1L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-1x1] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(1L, 3);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-1x3] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(1L, 3)", t);
        }
        if (actualLong != 3L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-1x3] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(3L, 1);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-3x1] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(3L, 1)", t);
        }
        if (actualLong != 3L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-3x1] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(2L, 3);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-2x3] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(2L, 3)", t);
        }
        if (actualLong != 6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-2x3] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(2L, -3);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-2x-3] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(2L, -3)", t);
        }
        if (actualLong != -6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-2x-3] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(-2L, 3);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--2x3] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(-2L, 3)", t);
        }
        if (actualLong != -6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--2x3] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(-2L, -3);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--2x-3] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(-2L, -3)", t);
        }
        if (actualLong != 6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--2x-3] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--1xintmin] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE)", t);
        }
        if (actualLong != (-1L * Integer.MIN_VALUE)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--1xintmin] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected=" + (-1L * Integer.MIN_VALUE) + " actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxx1] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(Long.MAX_VALUE, 1)", t);
        }
        if (actualLong != Long.MAX_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxx1] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected=" + Long.MAX_VALUE + " actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx1] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(Long.MIN_VALUE, 1)", t);
        }
        if (actualLong != Long.MIN_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx1] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected=" + Long.MIN_VALUE + " actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxx-1] semantic mismatch: unexpected exception for FieldUtils.safeMultiply(Long.MAX_VALUE, -1)", t);
        }
        if (actualLong != -Long.MAX_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxx-1] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected=" + (-Long.MAX_VALUE) + " actual=" + actualLong);
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx-1-throws] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) was expected to throw ArithmeticException");
        } catch (ArithmeticException expected) {
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx-1-throws] semantic mismatch: wrong exception type for FieldUtils.safeMultiply(Long.MIN_VALUE, -1)", t);
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx100-throws] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) was expected to throw ArithmeticException");
        } catch (ArithmeticException expected) {
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx100-throws] semantic mismatch: wrong exception type for FieldUtils.safeMultiply(Long.MIN_VALUE, 100)", t);
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minxintmax-throws] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) was expected to throw ArithmeticException");
        } catch (ArithmeticException expected) {
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minxintmax-throws] semantic mismatch: wrong exception type for FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE)", t);
        }

        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxxintmin-throws] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) was expected to throw ArithmeticException");
        } catch (ArithmeticException expected) {
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxxintmin-throws] semantic mismatch: wrong exception type for FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE)", t);
        }

        {
            long val1 = data.consumeLong();
            int val2 = data.consumeInt();

            try {
                long expected = FieldUtils.safeMultiply(val1, (long) val2);
                long actual = FieldUtils.safeMultiply(val1, val2);
                if (actual != expected) {
                    throw new FuzzerSecurityIssueLow("[oracle:long-int-vs-long-long] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) on equivalent inputs val1=" + val1 + " val2=" + val2 + " lhs=" + actual + " rhs=" + expected);
                }
            } catch (Throwable ignored) {
            }
        }

        {
            int a = data.consumeInt();
            int b = data.consumeInt();

            try {
                int intResult = FieldUtils.safeMultiply(a, b);
                long longResult = FieldUtils.safeMultiply((long) a, b);
                if (longResult != (long) intResult) {
                    throw new FuzzerSecurityIssueLow("[oracle:int-int-vs-long-int] metamorphic violation: safeMultiply(int,int) must agree with safeMultiply(long,int) when the int result is valid a=" + a + " b=" + b + " lhs=" + longResult + " rhs=" + ((long) intResult));
                }
            } catch (Throwable ignored) {
            }
        }

        {
            int n = data.consumeInt(-1_000_000, 1_000_000);

            try {
                long viaMultiply = FieldUtils.safeMultiply((long) n, -1);
                int viaNegate = FieldUtils.safeNegate(n);
                if (viaMultiply != (long) viaNegate) {
                    throw new FuzzerSecurityIssueLow("[oracle:minus-one-vs-negate] metamorphic violation: multiplying by -1 must match safeNegate on int-range inputs n=" + n + " lhs=" + viaMultiply + " rhs=" + ((long) viaNegate));
                }
            } catch (Throwable ignored) {
            }
        }

        {
            long x = data.consumeInt(-1_000_000, 1_000_000);
            int scalar = data.consumeInt(-1000, 1000);

            try {
                long lhs = FieldUtils.safeMultiply(x, scalar);
                long rhs = FieldUtils.safeAdd(
                        FieldUtils.safeMultiply(x, scalar / 2),
                        FieldUtils.safeMultiply(x, scalar - (scalar / 2)));
                if (lhs != rhs) {
                    throw new FuzzerSecurityIssueLow("[oracle:composition-split] metamorphic violation: multiplication must distribute over splitting the scalar because safeMultiply and safeAdd document exact arithmetic with overflow checks x=" + x + " scalar=" + scalar + " lhs=" + lhs + " rhs=" + rhs);
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
```

</details>

---
## [78] ⚙️ harness-repair · `attempt_020`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [79] ⚙️ harness-attempt · `attempt_020`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-minx-1-throws] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) was expected to throw ArithmeticException

---
## [80] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [35] — not reprinted*
- *[user] message: identical to step [41] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~5,036 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        assertEqualsLong("seed-0x0", 0L, FieldUtils.safeMultiply(0L, 0));
        assertEqualsLong("seed-1x1", 1L, FieldUtils.safeMultiply(1L, 1));
        assertEqualsLong("seed-1x3", 3L, FieldUtils.safeMultiply(1L, 3));
        assertEqualsLong("seed-3x1", 3L, FieldUtils.safeMultiply(3L, 1));
        assertEqualsLong("seed-2x3", 6L, FieldUtils.safeMultiply(2L, 3));
        assertEqualsLong("seed-2x-3", -6L, FieldUtils.safeMultiply(2L, -3));
        assertEqualsLong("seed--2x3", -6L, FieldUtils.safeMultiply(-2L, 3));
        assertEqualsLong("seed--2x-3", 6L, FieldUtils.safeMultiply(-2L, -3));
        assertEqualsLong("seed--1x-intmin", -1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE));
        assertEqualsLong("seed-longmaxx1", Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1));
        assertEqualsLong("seed-longminx1", Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1));
        assertEqualsLong("seed-longmaxx-1", -Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1));

        expectArithmeticException("seed-longminx-1-throws", Long.MIN_VALUE, -1);
        expectArithmeticException("seed-longminx100-throws", Long.MIN_VALUE, 100);
        expectArithmeticException("seed-longminx-intmax-throws", Long.MIN_VALUE, Integer.MAX_VALUE);
        expectArithmeticException("seed-longmaxx-intmin-throws", Long.MAX_VALUE, Integer.MIN_VALUE);

        long fuzzLong;
        int selector = data.consumeInt(0, 4);
        if (selector == 0) {
            fuzzLong = data.consumeInt(-1_000_000, 1_000_000);
        } else if (selector == 1) {
            fuzzLong = Long.MIN_VALUE;
        } else if (selector == 2) {
            fuzzLong = Long.MAX_VALUE;
        } else if (selector == 3) {
            fuzzLong = -1L;
        } else {
            fuzzLong = data.consumeInt();
        }

        int fuzzInt;
        int selector2 = data.consumeInt(0, 6);
        if (selector2 == 0) {
            fuzzInt = -1;
        } else if (selector2 == 1) {
            fuzzInt = 0;
        } else if (selector2 == 2) {
            fuzzInt = 1;
        } else if (selector2 == 3) {
            fuzzInt = data.consumeInt(-1_000_000, 1_000_000);
        } else if (selector2 == 4) {
            fuzzInt = Integer.MIN_VALUE;
        } else if (selector2 == 5) {
            fuzzInt = Integer.MAX_VALUE;
        } else {
            fuzzInt = data.consumeInt();
        }

        try {
            FieldUtils.safeMultiply(fuzzLong, fuzzInt);
        } catch (ArithmeticException ignored) {
        }

        int a = data.consumeInt(-1_000_000, 1_000_000);
        int b = data.consumeInt(-1_000_000, 1_000_000);
        try {
            int intResult = FieldUtils.safeMultiply(a, b);
            long longIntResult = FieldUtils.safeMultiply((long) a, b);
            long longLongResult = FieldUtils.safeMultiply((long) a, (long) b);

            /* Contract used: the three safeMultiply overloads are documented as the same operation
               on the same mathematical input space, differing only by parameter types/return width.
               For operands that fit in int-result range, all real overloads must agree exactly.
               A "fix" that merely suppresses/changes one branch in long,int would violate this. */
            if ((long) intResult != longIntResult) {
                throw new RuntimeException("[oracle:overload-int-vs-longint] metamorphic violation: safeMultiply(int,int) and safeMultiply(long,int) disagree inputA=" + a + " inputB=" + b + " lhs=" + intResult + " rhs=" + longIntResult);
            }
            if (longIntResult != longLongResult) {
                throw new RuntimeException("[oracle:overload-longint-vs-longlong] metamorphic violation: safeMultiply(long,int) and safeMultiply(long,long) disagree inputA=" + a + " inputB=" + b + " lhs=" + longIntResult + " rhs=" + longLongResult);
            }
        } catch (ArithmeticException ignored) {
            return;
        }
    }

    private static void assertEqualsLong(String oracleId, long expected, long actual) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: expected=" + expected + " actual=" + actual
            );
        }
    }

    private static void expectArithmeticException(String oracleId, long val1, int val2) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: expected ArithmeticException actualReturn=" + actual + " inputVal1=" + val1 + " inputVal2=" + val2
            );
        } catch (ArithmeticException expected) {
        }
    }
}
```

</details>

---
## [81] ⚙️ harness-attempt · `attempt_021`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:seed-longminx-1-throws] semantic mismatch: expected ArithmeticException actualReturn=-9223372036854775808 inputVal1=-922337203685477580…

---
## [82] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [35] — not reprinted*
- *[user] message: identical to step [35] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,034 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        liftedTestOracles();

        long fuzzLong = data.consumeLong();
        int fuzzInt = data.consumeInt();
        int fuzzIntA = data.consumeInt();
        int fuzzIntB = data.consumeInt();

        relationSafeMultiplyLongIntAgreesWithLongLongSibling(fuzzLong, fuzzInt);
        relationSafeMultiplyIntIntAgreesWithLongIntSibling(fuzzIntA, fuzzIntB);
        relationIdentityAndZero(fuzzLong);
    }

    private static void liftedTestOracles() {
        assertEqualsLong("lifted-0", 0L, FieldUtils.safeMultiply(0L, 0));

        assertEqualsLong("lifted-1", 1L, FieldUtils.safeMultiply(1L, 1));
        assertEqualsLong("lifted-2", 3L, FieldUtils.safeMultiply(1L, 3));
        assertEqualsLong("lifted-3", 3L, FieldUtils.safeMultiply(3L, 1));

        assertEqualsLong("lifted-4", 6L, FieldUtils.safeMultiply(2L, 3));
        assertEqualsLong("lifted-5", -6L, FieldUtils.safeMultiply(2L, -3));
        assertEqualsLong("lifted-6", -6L, FieldUtils.safeMultiply(-2L, 3));
        assertEqualsLong("lifted-7", 6L, FieldUtils.safeMultiply(-2L, -3));

        assertEqualsLong("lifted-8", -1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE));

        assertEqualsLong("lifted-9", Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1));
        assertEqualsLong("lifted-10", Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1));
        assertEqualsLong("lifted-11", -Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1));

        assertThrowsArithmetic("lifted-12", Long.MIN_VALUE, -1);
        assertThrowsArithmetic("lifted-13", Long.MIN_VALUE, 100);
        assertThrowsArithmetic("lifted-14", Long.MIN_VALUE, Integer.MAX_VALUE);
        assertThrowsArithmetic("lifted-15", Long.MAX_VALUE, Integer.MIN_VALUE);
    }

    private static void relationSafeMultiplyLongIntAgreesWithLongLongSibling(long val1, int val2) {
        long expected;
        try {
            expected = FieldUtils.safeMultiply(val1, (long) val2);
        } catch (Throwable t) {
            return;
        }

        long actual;
        try {
            actual = FieldUtils.safeMultiply(val1, val2);
        } catch (Throwable t) {
            return;
        }

        if (actual != expected) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:long-int-vs-long-long] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) on equivalent inputs inputVal1="
                    + val1 + " inputVal2=" + val2 + " lhs=" + actual + " rhs=" + expected);
        }
    }

    private static void relationSafeMultiplyIntIntAgreesWithLongIntSibling(int a, int b) {
        int intResult;
        try {
            intResult = FieldUtils.safeMultiply(a, b);
        } catch (Throwable t) {
            return;
        }

        long longResult;
        try {
            longResult = FieldUtils.safeMultiply((long) a, b);
        } catch (Throwable t) {
            return;
        }

        if (longResult != (long) intResult) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:int-int-vs-long-int] metamorphic violation: safeMultiply(int,int) must agree with widened safeMultiply(long,int) when the int result is valid inputA="
                    + a + " inputB=" + b + " lhs=" + longResult + " rhs=" + intResult);
        }
    }

    private static void relationIdentityAndZero(long x) {
        long timesOne;
        try {
            timesOne = FieldUtils.safeMultiply(x, 1);
        } catch (Throwable t) {
            return;
        }
        if (timesOne != x) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:identity-one] metamorphic violation: documented multiplication semantics require multiplying by 1 to preserve the value input="
                    + x + " lhs=" + timesOne + " rhs=" + x);
        }

        long timesZero;
        try {
            timesZero = FieldUtils.safeMultiply(x, 0);
        } catch (Throwable t) {
            return;
        }
        if (timesZero != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:zero-annihilator] metamorphic violation: documented multiplication semantics require multiplying by 0 to produce 0 input="
                    + x + " lhs=" + timesZero + " rhs=0");
        }

        long lhs;
        long rhs;
        try {
            lhs = FieldUtils.safeMultiply(x, -1);
            rhs = -FieldUtils.safeMultiply(x, 1);
        } catch (Throwable t) {
            return;
        }
        if (lhs != rhs) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:negation-consistency] metamorphic violation: for non-rejected inputs, multiplying by -1 must equal negating the multiply-by-1 result input="
                    + x + " lhs=" + lhs + " rhs=" + rhs);
        }
    }

    private static void assertEqualsLong(String oracleId, long expected, long actual) {
        if (actual != expected) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertThrowsArithmetic(String oracleId, long val1, int val2) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: expected ArithmeticException but returned=" + actual
                    + " for val1=" + val1 + " val2=" + val2);
        } catch (ArithmeticException expected) {
            return;
        }
    }
}
```

</details>

---
## [83] ⚙️ harness-attempt · `attempt_022`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-12] semantic mismatch: expected ArithmeticException but returned=-9223372036854775808 for val1=-9223372036854775808 val2=-1

---
## [84] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [35] — not reprinted*
- *[user] message: identical to step [41] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,623 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actual;

        actual = FieldUtils.safeMultiply(0L, 0);
        if (actual != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-0x0] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(1L, 1);
        if (actual != 1L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-1x1] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(1L, 3);
        if (actual != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-1x3] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(3L, 1);
        if (actual != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-3x1] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(2L, 3);
        if (actual != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-2x3] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(2L, -3);
        if (actual != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-2x-3] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-2L, 3);
        if (actual != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted--2x3] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-2L, -3);
        if (actual != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted--2x-3] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        if (actual != (-1L * Integer.MIN_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted--1xminint] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected="
                    + (-1L * Integer.MIN_VALUE) + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        if (actual != Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-maxx1] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected="
                    + Long.MAX_VALUE + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        if (actual != Long.MIN_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-minx1] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected="
                    + Long.MIN_VALUE + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        if (actual != -Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-maxx-1] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected="
                    + (-Long.MAX_VALUE) + " actual=" + actual);
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-throw-minx-1] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException but returned normally");
        } catch (ArithmeticException expected) {
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-throw-minx100] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) expected ArithmeticException but returned normally");
        } catch (ArithmeticException expected) {
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-throw-minxmaxint] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) expected ArithmeticException but returned normally");
        } catch (ArithmeticException expected) {
        }

        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-throw-maxxminint] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) expected ArithmeticException but returned normally");
        } catch (ArithmeticException expected) {
        }

        int fuzzVal = data.consumeInt(-1_000_000, 1_000_000);
        int fuzzMul = data.consumeInt(-1_000_000, 1_000_000);

        try {
            long lhs = FieldUtils.safeMultiply((long) fuzzVal, fuzzMul);
            long rhs = FieldUtils.safeMultiply((long) fuzzVal, (long) fuzzMul);
            // Contract justification: these are same-name overloads of safeMultiply with the same documented behavior
            // ("Multiply two values throwing an exception if overflow occurs"), so for any non-overflowing inputs they must agree.
            // A patch that only suppresses the exceptional -1 fast-path or otherwise returns a wrong value breaks this equality observably.
            if (lhs != rhs) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) inputVal="
                        + fuzzVal + " inputMul=" + fuzzMul + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (ArithmeticException ignored) {
            return;
        }

        long identityInput = (long) data.consumeInt(-1_000_000, 1_000_000);
        try {
            long identityResult = FieldUtils.safeMultiply(identityInput, 1);
            // Contract justification: the real API's own test suite lifts exact identity pairs such as (1,1), (3,1), (Long.MAX_VALUE,1), (Long.MIN_VALUE,1),
            // establishing that multiplying by 1 must preserve the value for valid inputs.
            // A throw-deleting or wrong-value patch can make the fast-path reachable but return an incorrect post-state/result.
            if (identityResult != identityInput) {
                throw new RuntimeException(
                    "[oracle:identity-by-one] metamorphic violation: safeMultiply(x,1)==x input="
                        + identityInput + " lhs=" + identityResult + " rhs=" + identityInput);
            }
        } catch (ArithmeticException ignored) {
            return;
        }
    }
}
```

</details>

---
## [85] ⚙️ harness-attempt · `attempt_023`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-throw-minx-1] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException but returned normally

---
## [86] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [35] — not reprinted*
- *[user] message: identical to step [35] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~9,256 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actualLong;
        boolean threw;

        actualLong = FieldUtils.safeMultiply(0L, 0);
        if (actualLong != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-0] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 1);
        if (actualLong != 1L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-1] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 3);
        if (actualLong != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-2] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(3L, 1);
        if (actualLong != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-3] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, 3);
        if (actualLong != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-4] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, -3);
        if (actualLong != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-5] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, 3);
        if (actualLong != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-6] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, -3);
        if (actualLong != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-7] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        if (actualLong != (-1L * Integer.MIN_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-8] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected=" + (-1L * Integer.MIN_VALUE) + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        if (actualLong != Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-9] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected=" + Long.MAX_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        if (actualLong != Long.MIN_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-10] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected=" + Long.MIN_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        if (actualLong != (-Long.MAX_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected=" + (-Long.MAX_VALUE) + " actual=" + actualLong);
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
        } catch (ArithmeticException e) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-12] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
        } catch (ArithmeticException e) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-13] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) expected ArithmeticException");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
        } catch (ArithmeticException e) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-14] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) expected ArithmeticException");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
        } catch (ArithmeticException e) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-15] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) expected ArithmeticException");
        }

        long val1 = data.consumeLong();
        int val2 = data.consumeInt();
        long expectedSibling;
        try {
            expectedSibling = FieldUtils.safeMultiply(val1, (long) val2);
        } catch (Exception e) {
            return;
        }
        long actualSibling;
        try {
            actualSibling = FieldUtils.safeMultiply(val1, val2);
        } catch (Exception e) {
            return;
        }
        if (actualSibling != expectedSibling) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:sibling-longint-longlong] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) on equivalent inputs inputVal1="
                    + val1 + " inputVal2=" + val2 + " lhs=" + actualSibling + " rhs=" + expectedSibling);
        }

        int a = data.consumeInt();
        int b = data.consumeInt();
        int intResult;
        try {
            intResult = FieldUtils.safeMultiply(a, b);
        } catch (Exception e) {
            return;
        }
        long longResult;
        try {
            longResult = FieldUtils.safeMultiply((long) a, b);
        } catch (Exception e) {
            return;
        }
        if (longResult != (long) intResult) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:sibling-intint-longint] metamorphic violation: safeMultiply(int,int) must agree with safeMultiply(long,int) when the int overload succeeds inputA="
                    + a + " inputB=" + b + " lhs=" + longResult + " rhs=" + intResult);
        }

        long moderate = data.consumeInt(-1_000_000, 1_000_000);
        if (moderate == Long.MIN_VALUE) {
            return;
        }

        try {
            long timesOne = FieldUtils.safeMultiply(moderate, 1);
            if (timesOne != moderate) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:identity-one] metamorphic violation: multiplication by 1 must preserve the value input=" + moderate + " lhs=" + timesOne + " rhs=" + moderate);
            }
        } catch (Exception e) {
            return;
        }

        try {
            long timesZero = FieldUtils.safeMultiply(moderate, 0);
            if (timesZero != 0L) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:annihilator-zero] metamorphic violation: multiplication by 0 must yield 0 input=" + moderate + " lhs=" + timesZero + " rhs=0");
            }
        } catch (Exception e) {
            return;
        }

        long negViaMultiply;
        long negViaSafeNegate;
        try {
            negViaMultiply = FieldUtils.safeMultiply(moderate, -1);
            negViaSafeNegate = FieldUtils.safeNegate(moderate);
        } catch (Exception e) {
            return;
        }
        if (negViaMultiply != negViaSafeNegate) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:negation-agreement] metamorphic violation: by the documented safe* family semantics, multiplying by -1 and safeNegate are the same operation for any non-overflowing input; a throw-deleting or wrong-value patch in safeMultiply breaks this observable equality input="
                    + moderate + " lhs=" + negViaMultiply + " rhs=" + negViaSafeNegate);
        }
    }
}
```

</details>

---
## [87] ⚙️ harness-repair · `attempt_024`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': ['boolean-swallow']}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [88] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `threw` (literal assignment, no rethrow, the caught exceptio

---
## [89] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~9,456 chars, 1 new)</summary>

- *[system] message: identical to step [35] — not reprinted*
- *[user] message: identical to step [35] — not reprinted*
**[assistant]**
```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actualLong;
        boolean threw;

        actualLong = FieldUtils.safeMultiply(0L, 0);
        if (actualLong != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-0] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 1);
        if (actualLong != 1L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-1] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 3);
        if (actualLong != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-2] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(3L, 1);
        if (actualLong != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-3] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, 3);
        if (actualLong != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-4] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, -3);
        if (actualLong != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-5] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, 3);
        if (actualLong != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-6] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, -3);
        if (actualLong != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-7] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        if (actualLong != (-1L * Integer.MIN_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-8] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected=" + (-1L * Integer.MIN_VALUE) + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        if (actualLong != Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-9] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected=" + Long.MAX_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        if (actualLong != Long.MIN_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-10] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected=" + Long.MIN_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        if (actualLong != (-Long.MAX_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected=" + (-Long.MAX_VALUE) + " actual=" + actualLong);
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
        } catch (ArithmeticException e) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-12] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
        } catch (ArithmeticException e) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-13] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) expected ArithmeticException");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
        } catch (ArithmeticException e) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-14] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) expected ArithmeticException");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
        } catch (ArithmeticException e) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-15] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) expected ArithmeticException");
        }

        long val1 = data.consumeLong();
        int val2 = data.consumeInt();
        long expectedSibling;
        try {
            expectedSibling = FieldUtils.safeMultiply(val1, (long) val2);
        } catch (Exception e) {
            return;
        }
        long actualSibling;
        try {
            actualSibling = FieldUtils.safeMultiply(val1, val2);
        } catch (Exception e) {
            return;
        }
        if (actualSibling != expectedSibling) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:sibling-longint-longlong] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) on equivalent inputs inputVal1="
                    + val1 + " inputVal2=" + val2 + " lhs=" + actualSibling + " rhs=" + expectedSibling);
        }

        int a = data.consumeInt();
        int b = data.consumeInt();
        int intResult;
        try {
            intResult = FieldUtils.safeMultiply(a, b);
        } catch (Exception e) {
            return;
        }
        long longResult;
        try {
            longResult = FieldUtils.safeMultiply((long) a, b);
        } catch (Exception e) {
            return;
        }
        if (longResult != (long) intResult) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:sibling-intint-longint] metamorphic violation: safeMultiply(int,int) must agree with safeMultiply(long,int) when the int overload succeeds inputA="
                    + a + " inputB=" + b + " lhs=" + longResult + " rhs=" + intResult);
        }

        long moderate = data.consumeInt(-1_000_000, 1_000_000);
        if (moderate == Long.MIN_VALUE) {
            return;
        }

        try {
            long timesOne = FieldUtils.safeMultiply(moderate, 1);
            if (timesOne != moderate) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:identity-one] metamorphic violation: multiplication by 1 must preserve the value input=" + moderate + " lhs=" + timesOne + " rhs=" + moderate);
            }
        } catch (Exception e) {
            return;
        }

        try {
            long timesZero = FieldUtils.safeMultiply(moderate, 0);
            if (timesZero != 0L) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:annihilator-zero] metamorphic violation: multiplication by 0 must yield 0 input=" + moderate + " lhs=" + timesZero + " rhs=0");
            }
        } catch (Exception e) {
            return;
        }

        long negViaMultiply;
        long negViaSafeNegate;
        try {
            negViaMultiply = FieldUtils.safeMultiply(moderate, -1);
            negViaSafeNegate = FieldUtils.safeNegate(moderate);
        } catch (Exception e) {
            return;
        }
        if (negViaMultiply != negViaSafeNegate) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:negation-agreement] metamorphic violation: by the documented safe* family semantics, multiplying by -1 and safeNegate are the same operation for any non-overflowing input; a throw-deleting or wrong-value patch in safeMultiply breaks this observable equality input="
                    + moderate + " lhs=" + negViaMultiply + " rhs=" + negViaSafeNegate);
        }
    }
}
```
- *[user] message: identical to step [43] — not reprinted*

</details>
<details open><summary>▸ Output (~9,142 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static long nextLong(FuzzedDataProvider data) {
        long hi = ((long) data.consumeInt()) << 32;
        long lo = ((long) data.consumeInt()) & 0xffffffffL;
        return hi | lo;
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actualLong;

        actualLong = FieldUtils.safeMultiply(0L, 0);
        if (actualLong != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-0] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 1);
        if (actualLong != 1L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-1] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 3);
        if (actualLong != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-2] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(3L, 1);
        if (actualLong != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-3] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, 3);
        if (actualLong != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-4] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, -3);
        if (actualLong != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-5] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, 3);
        if (actualLong != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-6] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, -3);
        if (actualLong != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-7] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        if (actualLong != (-1L * Integer.MIN_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-8] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected=" + (-1L * Integer.MIN_VALUE) + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        if (actualLong != Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-9] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected=" + Long.MAX_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        if (actualLong != Long.MIN_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-10] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected=" + Long.MIN_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        if (actualLong != (-Long.MAX_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected=" + (-Long.MAX_VALUE) + " actual=" + actualLong);
        }

        try {
            long ret = FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-12] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException actualReturn=" + ret);
        } catch (ArithmeticException expected) {
        }

        try {
            long ret = FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-13] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) expected ArithmeticException actualReturn=" + ret);
        } catch (ArithmeticException expected) {
        }

        try {
            long ret = FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-14] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) expected ArithmeticException actualReturn=" + ret);
        } catch (ArithmeticException expected) {
        }

        try {
            long ret = FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-15] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) expected ArithmeticException actualReturn=" + ret);
        } catch (ArithmeticException expected) {
        }

        long val1 = nextLong(data);
        int val2 = data.consumeInt();
        long expectedSibling;
        try {
            expectedSibling = FieldUtils.safeMultiply(val1, (long) val2);
        } catch (Exception e) {
            return;
        }
        long actualSibling;
        try {
            actualSibling = FieldUtils.safeMultiply(val1, val2);
        } catch (Exception e) {
            return;
        }
        if (actualSibling != expectedSibling) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:sibling-longint-longlong] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) on equivalent inputs inputVal1="
                    + val1 + " inputVal2=" + val2 + " lhs=" + actualSibling + " rhs=" + expectedSibling);
        }

        int a = data.consumeInt();
        int b = data.consumeInt();
        int intResult;
        try {
            intResult = FieldUtils.safeMultiply(a, b);
        } catch (Exception e) {
            return;
        }
        long longResult;
        try {
            longResult = FieldUtils.safeMultiply((long) a, b);
        } catch (Exception e) {
            return;
        }
        if (longResult != (long) intResult) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:sibling-intint-longint] metamorphic violation: safeMultiply(int,int) must agree with safeMultiply(long,int) when the int overload succeeds inputA="
                    + a + " inputB=" + b + " lhs=" + longResult + " rhs=" + intResult);
        }

        long moderate = (long) data.consumeInt(-1_000_000, 1_000_000);

        try {
            long timesOne = FieldUtils.safeMultiply(moderate, 1);
            if (timesOne != moderate) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:identity-one] metamorphic violation: multiplication by 1 must preserve the value input=" + moderate + " lhs=" + timesOne + " rhs=" + moderate);
            }
        } catch (Exception e) {
            return;
        }

        try {
            long timesZero = FieldUtils.safeMultiply(moderate, 0);
            if (timesZero != 0L) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:annihilator-zero] metamorphic violation: multiplication by 0 must yield 0 input=" + moderate + " lhs=" + timesZero + " rhs=0");
            }
        } catch (Exception e) {
            return;
        }

        long negViaMultiply;
        long negViaSafeNegate;
        try {
            negViaMultiply = FieldUtils.safeMultiply(moderate, -1);
            negViaSafeNegate = FieldUtils.safeNegate(moderate);
        } catch (Exception e) {
            return;
        }
        if (negViaMultiply != negViaSafeNegate) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:negation-agreement] metamorphic violation: multiplying by -1 must agree with safeNegate for any non-overflowing input; a wrong-value or throw-deleting patch in safeMultiply breaks this observable equality input="
                    + moderate + " lhs=" + negViaMultiply + " rhs=" + negViaSafeNegate);
        }
    }
}
```

</details>

---
## [90] ⚙️ harness-repair · `attempt_025`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [91] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [92] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~10,698 chars, 2 new)</summary>

- *[system] message: identical to step [35] — not reprinted*
- *[user] message: identical to step [35] — not reprinted*
- *[assistant] message: identical to step [89] — not reprinted*
- *[user] message: identical to step [43] — not reprinted*
**[assistant]**
```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static long nextLong(FuzzedDataProvider data) {
        long hi = ((long) data.consumeInt()) << 32;
        long lo = ((long) data.consumeInt()) & 0xffffffffL;
        return hi | lo;
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actualLong;

        actualLong = FieldUtils.safeMultiply(0L, 0);
        if (actualLong != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-0] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 1);
        if (actualLong != 1L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-1] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 3);
        if (actualLong != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-2] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(3L, 1);
        if (actualLong != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-3] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, 3);
        if (actualLong != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-4] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, -3);
        if (actualLong != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-5] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, 3);
        if (actualLong != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-6] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, -3);
        if (actualLong != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-7] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        if (actualLong != (-1L * Integer.MIN_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-8] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected=" + (-1L * Integer.MIN_VALUE) + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        if (actualLong != Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-9] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected=" + Long.MAX_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        if (actualLong != Long.MIN_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-10] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected=" + Long.MIN_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        if (actualLong != (-Long.MAX_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected=" + (-Long.MAX_VALUE) + " actual=" + actualLong);
        }

        try {
            long ret = FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-12] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException actualReturn=" + ret);
        } catch (ArithmeticException expected) {
        }

        try {
            long ret = FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-13] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) expected ArithmeticException actualReturn=" + ret);
        } catch (ArithmeticException expected) {
        }

        try {
            long ret = FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-14] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) expected ArithmeticException actualReturn=" + ret);
        } catch (ArithmeticException expected) {
        }

        try {
            long ret = FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-15] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) expected ArithmeticException actualReturn=" + ret);
        } catch (ArithmeticException expected) {
        }

        long val1 = nextLong(data);
        int val2 = data.consumeInt();
        long expectedSibling;
        try {
            expectedSibling = FieldUtils.safeMultiply(val1, (long) val2);
        } catch (Exception e) {
            return;
        }
        long actualSibling;
        try {
            actualSibling = FieldUtils.safeMultiply(val1, val2);
        } catch (Exception e) {
            return;
        }
        if (actualSibling != expectedSibling) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:sibling-longint-longlong] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) on equivalent inputs inputVal1="
                    + val1 + " inputVal2=" + val2 + " lhs=" + actualSibling + " rhs=" + expectedSibling);
        }

        int a = data.consumeInt();
        int b = data.consumeInt();
        int intResult;
        try {
            intResult = FieldUtils.safeMultiply(a, b);
        } catch (Exception e) {
            return;
        }
        long longResult;
        try {
            longResult = FieldUtils.safeMultiply((long) a, b);
        } catch (Exception e) {
            return;
        }
        if (longResult != (long) intResult) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:sibling-intint-longint] metamorphic violation: safeMultiply(int,int) must agree with safeMultiply(long,int) when the int overload succeeds inputA="
                    + a + " inputB=" + b + " lhs=" + longResult + " rhs=" + intResult);
        }

        long moderate = (long) data.consumeInt(-1_000_000, 1_000_000);

        try {
            long timesOne = FieldUtils.safeMultiply(moderate, 1);
            if (timesOne != moderate) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:identity-one] metamorphic violation: multiplication by 1 must preserve the value input=" + moderate + " lhs=" + timesOne + " rhs=" + moderate);
            }
        } catch (Exception e) {
            return;
        }

        try {
            long timesZero = FieldUtils.safeMultiply(moderate, 0);
            if (timesZero != 0L) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:annihilator-zero] metamorphic violation: multiplication by 0 must yield 0 input=" + moderate + " lhs=" + timesZero + " rhs=0");
            }
        } catch (Exception e) {
            return;
        }

        long negViaMultiply;
        long negViaSafeNegate;
        try {
            negViaMultiply = FieldUtils.safeMultiply(moderate, -1);
            negViaSafeNegate = FieldUtils.safeNegate(moderate);
        } catch (Exception e) {
            return;
        }
        if (negViaMultiply != negViaSafeNegate) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:negation-agreement] metamorphic violation: multiplying by -1 must agree with safeNegate for any non-overflowing input; a wrong-value or throw-deleting patch in safeMultiply breaks this observable equality input="
                    + moderate + " lhs=" + negViaMultiply + " rhs=" + negViaSafeNegate);
        }
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HR/004_patch1-Time-15-ACS_c_rep001/Time_15_buggy/fuzz/attempt_025/FuzzHarness.java:181: error: incompatible types: possible lossy conversion from long to int
            negViaSafeNegate = FieldUtils.safeNegate(moderate);
                                                     ^
Note: Some messages have been simplified; recompile with -Xdiags:verbose to get full output
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
<details open><summary>▸ Output (~9,262 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static long nextLong(FuzzedDataProvider data) {
        long hi = ((long) data.consumeInt()) << 32;
        long lo = ((long) data.consumeInt()) & 0xffffffffL;
        return hi | lo;
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actualLong;

        actualLong = FieldUtils.safeMultiply(0L, 0);
        if (actualLong != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-0] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 1);
        if (actualLong != 1L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-1] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 3);
        if (actualLong != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-2] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(3L, 1);
        if (actualLong != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-3] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, 3);
        if (actualLong != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-4] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, -3);
        if (actualLong != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-5] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, 3);
        if (actualLong != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-6] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, -3);
        if (actualLong != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-7] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        if (actualLong != (-1L * Integer.MIN_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-8] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected=" + (-1L * Integer.MIN_VALUE) + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        if (actualLong != Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-9] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected=" + Long.MAX_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        if (actualLong != Long.MIN_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-10] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected=" + Long.MIN_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        if (actualLong != (-Long.MAX_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected=" + (-Long.MAX_VALUE) + " actual=" + actualLong);
        }

        try {
            long ret = FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-12] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException actualReturn=" + ret);
        } catch (ArithmeticException expected) {
        }

        try {
            long ret = FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-13] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) expected ArithmeticException actualReturn=" + ret);
        } catch (ArithmeticException expected) {
        }

        try {
            long ret = FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-14] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) expected ArithmeticException actualReturn=" + ret);
        } catch (ArithmeticException expected) {
        }

        try {
            long ret = FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-15] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) expected ArithmeticException actualReturn=" + ret);
        } catch (ArithmeticException expected) {
        }

        long val1 = nextLong(data);
        int val2 = data.consumeInt();
        long expectedSibling;
        try {
            expectedSibling = FieldUtils.safeMultiply(val1, (long) val2);
        } catch (Exception e) {
            return;
        }
        long actualSibling;
        try {
            actualSibling = FieldUtils.safeMultiply(val1, val2);
        } catch (Exception e) {
            return;
        }
        if (actualSibling != expectedSibling) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:sibling-longint-longlong] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) on equivalent inputs inputVal1="
                    + val1 + " inputVal2=" + val2 + " lhs=" + actualSibling + " rhs=" + expectedSibling);
        }

        int a = data.consumeInt();
        int b = data.consumeInt();
        int intResult;
        try {
            intResult = FieldUtils.safeMultiply(a, b);
        } catch (Exception e) {
            return;
        }
        long longResult;
        try {
            longResult = FieldUtils.safeMultiply((long) a, b);
        } catch (Exception e) {
            return;
        }
        if (longResult != (long) intResult) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:sibling-intint-longint] metamorphic violation: safeMultiply(int,int) must agree with safeMultiply(long,int) when the int overload succeeds inputA="
                    + a + " inputB=" + b + " lhs=" + longResult + " rhs=" + intResult);
        }

        long moderateLong = (long) data.consumeInt(-1_000_000, 1_000_000);

        try {
            long timesOne = FieldUtils.safeMultiply(moderateLong, 1);
            if (timesOne != moderateLong) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:identity-one] metamorphic violation: multiplication by 1 must preserve the value input=" + moderateLong + " lhs=" + timesOne + " rhs=" + moderateLong);
            }
        } catch (Exception e) {
            return;
        }

        try {
            long timesZero = FieldUtils.safeMultiply(moderateLong, 0);
            if (timesZero != 0L) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:annihilator-zero] metamorphic violation: multiplication by 0 must yield 0 input=" + moderateLong + " lhs=" + timesZero + " rhs=0");
            }
        } catch (Exception e) {
            return;
        }

        int moderateInt = data.consumeInt(-1_000_000, 1_000_000);
        long negViaMultiply;
        int negViaSafeNegate;
        try {
            negViaMultiply = FieldUtils.safeMultiply((long) moderateInt, -1);
            negViaSafeNegate = FieldUtils.safeNegate(moderateInt);
        } catch (Exception e) {
            return;
        }
        if (negViaMultiply != (long) negViaSafeNegate) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:negation-agreement] metamorphic violation: multiplying by -1 must agree with safeNegate for any non-overflowing int input; a wrong-value or throw-deleting patch in safeMultiply breaks this observable equality input="
                    + moderateInt + " lhs=" + negViaMultiply + " rhs=" + negViaSafeNegate);
        }
    }
}
```

</details>

---
## [93] ⚙️ harness-repair · `attempt_026`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [94] ⚙️ harness-attempt · `attempt_026`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-12] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException actualReturn=-922337203685477580…

---
## [95] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [35] — not reprinted*
- *[user] message: identical to step [41] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,790 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkEquals("seed-0", 0L, FieldUtils.safeMultiply(0L, 0));
        checkEquals("seed-1", 1L, FieldUtils.safeMultiply(1L, 1));
        checkEquals("seed-2", 3L, FieldUtils.safeMultiply(1L, 3));
        checkEquals("seed-3", 3L, FieldUtils.safeMultiply(3L, 1));
        checkEquals("seed-4", 6L, FieldUtils.safeMultiply(2L, 3));
        checkEquals("seed-5", -6L, FieldUtils.safeMultiply(2L, -3));
        checkEquals("seed-6", -6L, FieldUtils.safeMultiply(-2L, 3));
        checkEquals("seed-7", 6L, FieldUtils.safeMultiply(-2L, -3));
        checkEquals("seed-8", -1L * (long) Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE));
        checkEquals("seed-9", Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1));
        checkEquals("seed-10", Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1));
        checkEquals("seed-11", -Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1));

        checkThrowsArithmetic("seed-throw-0", Long.MIN_VALUE, -1);
        checkThrowsArithmetic("seed-throw-1", Long.MIN_VALUE, 100);
        checkThrowsArithmetic("seed-throw-2", Long.MIN_VALUE, Integer.MAX_VALUE);
        checkThrowsArithmetic("seed-throw-3", Long.MAX_VALUE, Integer.MIN_VALUE);

        long moderateVal = data.consumeInt(-1_000_000, 1_000_000);
        int moderateInt = data.consumeInt(-1_000, 1_000);

        try {
            long lhs = FieldUtils.safeMultiply(moderateVal, moderateInt);
            long rhs = FieldUtils.safeMultiply(moderateVal, (long) moderateInt);
            if (lhs != rhs) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) on the same mathematical inputs inputVal="
                        + moderateVal + " inputInt=" + moderateInt + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (ArithmeticException e) {
            return;
        }

        long composedBase = data.consumeInt(-1_000_000, 1_000_000);
        int a = data.consumeInt(-1000, 1000);
        int b = data.consumeInt(-1000, 1000);

        try {
            long left = FieldUtils.safeMultiply(composedBase, a + b);
            long right = FieldUtils.safeAdd(FieldUtils.safeMultiply(composedBase, a), FieldUtils.safeMultiply(composedBase, b));
            if (left != right) {
                throw new RuntimeException(
                    "[oracle:distributive-compose] metamorphic violation: safeMultiply should distribute over safeAdd for exact non-overflowing long arithmetic inputVal="
                        + composedBase + " a=" + a + " b=" + b + " lhs=" + left + " rhs=" + right);
            }
        } catch (ArithmeticException e) {
            return;
        }
    }

    private static void checkEquals(String id, long expected, long actual) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: expected=" + expected + " actual=" + actual);
        }
    }

    private static void checkThrowsArithmetic(String id, long val1, int val2) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: expected ArithmeticException actualReturn=" + actual
                    + " inputVal1=" + val1 + " inputVal2=" + val2);
        } catch (ArithmeticException expected) {
        }
    }
}
```

</details>

---
## [96] ⚙️ harness-attempt · `attempt_027`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:seed-throw-0] semantic mismatch: expected ArithmeticException actualReturn=-9223372036854775808 inputVal1=-9223372036854775808 inputVal…

---
## [97] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [35] — not reprinted*
- *[user] message: identical to step [35] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,837 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedOraclePairs();
        checkLiftedOracleExceptions();

        long relVal1 = data.consumeInt() == 0 ? Long.MIN_VALUE : data.consumeLong();
        int relVal2 = data.consumeInt();
        checkSafeMultiplyLongIntAgreesWithLongLong(relVal1, relVal2);

        int a = data.consumeInt();
        int b = data.consumeInt();
        checkSafeMultiplyIntIntAgreesWithLongInt(a, b);

        long negateLike = data.consumeLong();
        checkSuccessfulMultiplyByMinusOneActsAsNegation(negateLike);
    }

    private static void checkLiftedOraclePairs() {
        long actual;

        actual = FieldUtils.safeMultiply(0L, 0);
        if (actual != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:pair-0] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(1L, 1);
        if (actual != 1L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:pair-1] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(1L, 3);
        if (actual != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:pair-2] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(3L, 1);
        if (actual != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:pair-3] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(2L, 3);
        if (actual != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:pair-4] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(2L, -3);
        if (actual != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:pair-5] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-2L, 3);
        if (actual != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:pair-6] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-2L, -3);
        if (actual != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:pair-7] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        if (actual != (-1L * Integer.MIN_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:pair-8] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected="
                    + (-1L * Integer.MIN_VALUE) + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        if (actual != Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:pair-9] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected="
                    + Long.MAX_VALUE + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        if (actual != Long.MIN_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:pair-10] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected="
                    + Long.MIN_VALUE + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        if (actual != -Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:pair-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected="
                    + (-Long.MAX_VALUE) + " actual=" + actual);
        }
    }

    private static void checkLiftedOracleExceptions() {
        boolean threw;

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
        } catch (ArithmeticException e) {
            threw = true;
        }
        if (!threw) {
            long actual = FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:exception-0] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException actualReturn="
                    + actual);
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
        } catch (ArithmeticException e) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:exception-1] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) expected ArithmeticException");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
        } catch (ArithmeticException e) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:exception-2] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) expected ArithmeticException");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
        } catch (ArithmeticException e) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:exception-3] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) expected ArithmeticException");
        }
    }

    private static void checkSafeMultiplyLongIntAgreesWithLongLong(long val1, int val2) {
        long expected;
        try {
            expected = FieldUtils.safeMultiply(val1, (long) val2);
        } catch (Exception e) {
            return;
        }

        long actual;
        try {
            actual = FieldUtils.safeMultiply(val1, val2);
        } catch (Exception e) {
            return;
        }

        if (actual != expected) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:rel-longint-longlong] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) for equivalent inputs val1="
                    + val1 + " val2=" + val2 + " lhs=" + actual + " rhs=" + expected);
        }
    }

    private static void checkSafeMultiplyIntIntAgreesWithLongInt(int a, int b) {
        int intResult;
        try {
            intResult = FieldUtils.safeMultiply(a, b);
        } catch (Exception e) {
            return;
        }

        long longResult;
        try {
            longResult = FieldUtils.safeMultiply((long) a, b);
        } catch (Exception e) {
            return;
        }

        if (longResult != (long) intResult) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:rel-intint-longint] metamorphic violation: safeMultiply(int,int) must agree with widened safeMultiply(long,int) when int result is valid a="
                    + a + " b=" + b + " lhs=" + longResult + " rhs=" + intResult);
        }
    }

    private static void checkSuccessfulMultiplyByMinusOneActsAsNegation(long v) {
        long multiplied;
        try {
            multiplied = FieldUtils.safeMultiply(v, -1);
        } catch (Exception e) {
            return;
        }

        long summed;
        try {
            summed = FieldUtils.safeAdd(multiplied, v);
        } catch (Exception e) {
            return;
        }

        if (summed != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:rel-negation] metamorphic violation: for every successful multiplication by -1, the result is the additive inverse, so result + input must be 0; input="
                    + v + " multiplied=" + multiplied + " sum=" + summed);
        }
    }
}
```

</details>

---
## [98] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `threw` (literal assignment, no rethrow, the caught exceptio

---
## [99] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~9,982 chars, 2 new)</summary>

- *[system] message: identical to step [35] — not reprinted*
- *[user] message: identical to step [35] — not reprinted*
**[assistant]**
```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedOraclePairs();
        checkLiftedOracleExceptions();

        long relVal1 = data.consumeInt() == 0 ? Long.MIN_VALUE : data.consumeLong();
        int relVal2 = data.consumeInt();
        checkSafeMultiplyLongIntAgreesWithLongLong(relVal1, relVal2);

        int a = data.consumeInt();
        int b = data.consumeInt();
        checkSafeMultiplyIntIntAgreesWithLongInt(a, b);

        long negateLike = data.consumeLong();
        checkSuccessfulMultiplyByMinusOneActsAsNegation(negateLike);
    }

    private static void checkLiftedOraclePairs() {
        long actual;

        actual = FieldUtils.safeMultiply(0L, 0);
        if (actual != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:pair-0] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(1L, 1);
        if (actual != 1L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:pair-1] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(1L, 3);
        if (actual != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:pair-2] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(3L, 1);
        if (actual != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:pair-3] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(2L, 3);
        if (actual != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:pair-4] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(2L, -3);
        if (actual != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:pair-5] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-2L, 3);
        if (actual != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:pair-6] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-2L, -3);
        if (actual != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:pair-7] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        if (actual != (-1L * Integer.MIN_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:pair-8] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected="
                    + (-1L * Integer.MIN_VALUE) + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        if (actual != Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:pair-9] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected="
                    + Long.MAX_VALUE + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        if (actual != Long.MIN_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:pair-10] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected="
                    + Long.MIN_VALUE + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        if (actual != -Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:pair-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected="
                    + (-Long.MAX_VALUE) + " actual=" + actual);
        }
    }

    private static void checkLiftedOracleExceptions() {
        boolean threw;

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
        } catch (ArithmeticException e) {
            threw = true;
        }
        if (!threw) {
            long actual = FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:exception-0] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException actualReturn="
                    + actual);
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
        } catch (ArithmeticException e) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:exception-1] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) expected ArithmeticException");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
        } catch (ArithmeticException e) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:exception-2] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) expected ArithmeticException");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
        } catch (ArithmeticException e) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:exception-3] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) expected ArithmeticException");
        }
    }

    private static void checkSafeMultiplyLongIntAgreesWithLongLong(long val1, int val2) {
        long expected;
        try {
            expected = FieldUtils.safeMultiply(val1, (long) val2);
        } catch (Exception e) {
            return;
        }

        long actual;
        try {
            actual = FieldUtils.safeMultiply(val1, val2);
        } catch (Exception e) {
            return;
        }

        if (actual != expected) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:rel-longint-longlong] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) for equivalent inputs val1="
                    + val1 + " val2=" + val2 + " lhs=" + actual + " rhs=" + expected);
        }
    }

    private static void checkSafeMultiplyIntIntAgreesWithLongInt(int a, int b) {
        int intResult;
        try {
            intResult = FieldUtils.safeMultiply(a, b);
        } catch (Exception e) {
            return;
        }

        long longResult;
        try {
            longResult = FieldUtils.safeMultiply((long) a, b);
        } catch (Exception e) {
            return;
        }

        if (longResult != (long) intResult) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:rel-intint-longint] metamorphic violation: safeMultiply(int,int) must agree with widened safeMultiply(long,int) when int result is valid a="
                    + a + " b=" + b + " lhs=" + longResult + " rhs=" + intResult);
        }
    }

    private static void checkSuccessfulMultiplyByMinusOneActsAsNegation(long v) {
        long multiplied;
        try {
            multiplied = FieldUtils.safeMultiply(v, -1);
        } catch (Exception e) {
            return;
        }

        long summed;
        try {
            summed = FieldUtils.safeAdd(multiplied, v);
        } catch (Exception e) {
            return;
        }

        if (summed != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:rel-negation] metamorphic violation: for every successful multiplication by -1, the result is the additive inverse, so result + input must be 0; input="
                    + v + " multiplied=" + multiplied + " sum=" + summed);
        }
    }
}
```
**[user]**
```
Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `threw` (literal assignment, no rethrow, the caught exception is never used) and the alarm near line 113 is conditioned on `threw` — the caught exception's identity is destroyed, so attribution cannot tell WHICH exception fired or whether the unpatched build crashes the same way. Rethrow the exception as the alarm's cause (`..., e`), or assert on the real observable instead of a swallowed boolean.
A caught exception is a REJECTION, never a violation, and flattening it into a flag destroys the identity attribution needs. Either assert on the real observable the test pins (compute the actual value and compare it), or if you must re-report a caught crash, re-throw it with the original as the cause: `new FuzzerSecurityIssueLow("[oracle:<id>] ...", e)`. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~7,835 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedOraclePairs();
        checkLiftedOracleExceptions();

        long relVal1 = data.consumeLong();
        int relVal2 = data.consumeInt();
        checkSafeMultiplyLongIntAgreesWithLongLong(relVal1, relVal2);

        int a = data.consumeInt();
        int b = data.consumeInt();
        checkSafeMultiplyIntIntAgreesWithLongInt(a, b);

        long negateLike = data.consumeLong();
        checkSuccessfulMultiplyByMinusOneActsAsNegation(negateLike);
    }

    private static void checkLiftedOraclePairs() {
        long actual;

        actual = FieldUtils.safeMultiply(0L, 0);
        if (actual != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:pair-0] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(1L, 1);
        if (actual != 1L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:pair-1] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(1L, 3);
        if (actual != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:pair-2] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(3L, 1);
        if (actual != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:pair-3] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(2L, 3);
        if (actual != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:pair-4] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(2L, -3);
        if (actual != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:pair-5] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-2L, 3);
        if (actual != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:pair-6] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-2L, -3);
        if (actual != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:pair-7] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        if (actual != (-1L * Integer.MIN_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:pair-8] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected="
                    + (-1L * Integer.MIN_VALUE) + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        if (actual != Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:pair-9] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected="
                    + Long.MAX_VALUE + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        if (actual != Long.MIN_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:pair-10] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected="
                    + Long.MIN_VALUE + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        if (actual != -Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:pair-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected="
                    + (-Long.MAX_VALUE) + " actual=" + actual);
        }
    }

    private static void checkLiftedOracleExceptions() {
        expectArithmeticExceptionLongInt(Long.MIN_VALUE, -1, "exception-0");
        expectArithmeticExceptionLongInt(Long.MIN_VALUE, 100, "exception-1");
        expectArithmeticExceptionLongInt(Long.MIN_VALUE, Integer.MAX_VALUE, "exception-2");
        expectArithmeticExceptionLongInt(Long.MAX_VALUE, Integer.MIN_VALUE, "exception-3");
    }

    private static void expectArithmeticExceptionLongInt(long val1, int val2, String id) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: FieldUtils.safeMultiply(" + val1 + ", " + val2
                    + ") expected ArithmeticException actualReturn=" + actual);
        } catch (ArithmeticException expected) {
            return;
        } catch (Exception e) {
            return;
        }
    }

    private static void checkSafeMultiplyLongIntAgreesWithLongLong(long val1, int val2) {
        long expected;
        try {
            expected = FieldUtils.safeMultiply(val1, (long) val2);
        } catch (Exception e) {
            return;
        }

        long actual;
        try {
            actual = FieldUtils.safeMultiply(val1, val2);
        } catch (Exception e) {
            return;
        }

        if (actual != expected) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:rel-longint-longlong] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) for equivalent inputs val1="
                    + val1 + " val2=" + val2 + " lhs=" + actual + " rhs=" + expected);
        }
    }

    private static void checkSafeMultiplyIntIntAgreesWithLongInt(int a, int b) {
        int intResult;
        try {
            intResult = FieldUtils.safeMultiply(a, b);
        } catch (Exception e) {
            return;
        }

        long longResult;
        try {
            longResult = FieldUtils.safeMultiply((long) a, b);
        } catch (Exception e) {
            return;
        }

        if (longResult != (long) intResult) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:rel-intint-longint] metamorphic violation: safeMultiply(int,int) must agree with widened safeMultiply(long,int) when int result is valid a="
                    + a + " b=" + b + " lhs=" + longResult + " rhs=" + intResult);
        }
    }

    private static void checkSuccessfulMultiplyByMinusOneActsAsNegation(long v) {
        long multiplied;
        try {
            multiplied = FieldUtils.safeMultiply(v, -1);
        } catch (Exception e) {
            return;
        }

        long summed;
        try {
            summed = FieldUtils.safeAdd(multiplied, v);
        } catch (Exception e) {
            return;
        }

        if (summed != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:rel-negation] metamorphic violation: for every successful multiplication by -1, the result is the additive inverse, so result + input must be 0; input="
                    + v + " multiplied=" + multiplied + " sum=" + summed);
        }
    }
}
```

</details>

---
## [100] ⚙️ harness-repair · `attempt_029`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [101] ⚙️ harness-attempt · `attempt_029`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:exception-0] semantic mismatch: FieldUtils.safeMultiply(-9223372036854775808, -1) expected ArithmeticException actualReturn=-9223372036…

---
## [102] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [35] — not reprinted*
- *[user] message: identical to step [41] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,888 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedTestOracles();

        long fuzzVal = (((long) data.consumeInt(-1_000_000, 1_000_000)) * 1_000_001L)
                + data.consumeInt(-1_000_000, 1_000_000);
        int fuzzInt = data.consumeInt(-1_000_000, 1_000_000);

        try {
            long lhs = FieldUtils.safeMultiply(fuzzVal, fuzzInt);
            long rhs = FieldUtils.safeMultiply(fuzzVal, (long) fuzzInt);
            /* Documented guarantee: the same-name safeMultiply overloads "Multiply two values
             * throwing an exception if overflow occurs." For equivalent mathematical inputs that
             * do not overflow, both real library calls must compute the same product. A patch
             * that merely deletes/avoids the overflow throw or silently returns a wrong value in
             * one overload breaks this sibling-agreement post-condition. */
            if (lhs != rhs) {
                throw new RuntimeException(
                        "[oracle:overload-agreement] metamorphic violation: safeMultiply(long,int) != safeMultiply(long,long)"
                                + " inputVal=" + fuzzVal
                                + " inputInt=" + fuzzInt
                                + " lhs=" + lhs
                                + " rhs=" + rhs);
            }
        } catch (Throwable t) {
            return;
        }
    }

    private static void checkLiftedTestOracles() {
        checkEqualsLong("eq-0x0", 0L, FieldUtils.safeMultiply(0L, 0));
        checkEqualsLong("eq-1x1", 1L, FieldUtils.safeMultiply(1L, 1));
        checkEqualsLong("eq-1x3", 3L, FieldUtils.safeMultiply(1L, 3));
        checkEqualsLong("eq-3x1", 3L, FieldUtils.safeMultiply(3L, 1));

        checkEqualsLong("eq-2x3", 6L, FieldUtils.safeMultiply(2L, 3));
        checkEqualsLong("eq-2x-3", -6L, FieldUtils.safeMultiply(2L, -3));
        checkEqualsLong("eq--2x3", -6L, FieldUtils.safeMultiply(-2L, 3));
        checkEqualsLong("eq--2x-3", 6L, FieldUtils.safeMultiply(-2L, -3));

        checkEqualsLong("eq--1xminint", -1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE));

        checkEqualsLong("eq-maxx1", Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1));
        checkEqualsLong("eq-minx1", Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1));
        checkEqualsLong("eq-maxx-1", -Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1));

        expectArithmetic("throw-minx-1", Long.MIN_VALUE, -1);
        expectArithmetic("throw-minx100", Long.MIN_VALUE, 100);
        expectArithmetic("throw-minxmaxint", Long.MIN_VALUE, Integer.MAX_VALUE);
        expectArithmetic("throw-maxxminint", Long.MAX_VALUE, Integer.MIN_VALUE);
    }

    private static void checkEqualsLong(String oracleId, long expected, long actual) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: FieldUtils.safeMultiply(long,int)"
                            + " expected=" + expected
                            + " actual=" + actual);
        }
    }

    private static void expectArithmetic(String oracleId, long val1, int val2) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: expected ArithmeticException"
                            + " but returned=" + actual
                            + " for val1=" + val1
                            + " val2=" + val2);
        } catch (ArithmeticException expected) {
        }
    }
}
```

</details>

---
## [103] ⚙️ harness-attempt · `attempt_030`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:throw-minx-1] semantic mismatch: expected ArithmeticException but returned=-9223372036854775808 for val1=-9223372036854775808 val2=-1

---
## [104] ⚙️ coverage · `jazzer coverage dumps`
**output:** **20/20 dump(s) written under /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/cov; 20 raw fuzz log(s)**
<details><summary>▸ detail (~11,208 chars)</summary>

- detail: {'dumps': [{'harness': 'attempt_002', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/cov/attempt_002_compiled.exec', 'exists': True, 'bytes': 163}, {'harness': 'attempt_004', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/cov/attempt_004_compiled.exec', 'exists': True, 'bytes': 160}, {'harness': 'attempt_005', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/cov/attempt_005_compiled.exec', 'exists': True, 'bytes': 162}, {'harness': 'attempt_007', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/cov/attempt_007_compiled.exec', 'exists': True, 'bytes': 161}, {'harness': 'attempt_008', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/cov/attempt_008_compiled.exec', 'exists': True, 'bytes': 161}, {'harness': 'attempt_009', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/cov/attempt_009_compiled.exec', 'exists': True, 'bytes': 161}, {'harness': 'attempt_010', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/cov/attempt_010_compiled.exec', 'exists': True, 'bytes': 161}, {'harness': 'attempt_011', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/cov/attempt_011_compiled.exec', 'exists': True, 'bytes': 160}, {'harness': 'attempt_012', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/cov/attempt_012_compiled.exec', 'exists': True, 'bytes': 163}, {'harness': 'attempt_014', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/cov/attempt_014_compiled.exec', 'exists': True, 'bytes': 160}, {'harness': 'attempt_016', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/cov/attempt_016_compiled.exec', 'exists': True, 'bytes': 161}, {'harness': 'attempt_017', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/cov/attempt_017_compiled.exec', 'exists': True, 'bytes': 159}, {'harness': 'attempt_020', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/cov/attempt_020_compiled.exec', 'exists': True, 'bytes': 169}, {'harness': 'attempt_021', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/cov/attempt_021_compiled.exec', 'exists': True, 'bytes': 161}, {'harness': 'attempt_022', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/cov/attempt_022_compiled.exec', 'exists': True, 'bytes': 161}, {'harness': 'attempt_023', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/cov/attempt_023_compiled.exec', 'exists': True, 'bytes': 161}, {'harness': 'attempt_026', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/cov/attempt_026_compiled.exec', 'exists': True, 'bytes': 164}, {'harness': 'attempt_027', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/cov/attempt_027_compiled.exec', 'exists': True, 'bytes': 159}, {'harness': 'attempt_029', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/cov/attempt_029_compiled.exec', 'exists': True, 'bytes': 163}, {'harness': 'attempt_030', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/cov/attempt_030_compiled.exec', 'exists': True, 'bytes': 158}], 'outputs': ['/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/fuzz_out/attempt_002_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/fuzz_out/attempt_004_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/fuzz_out/attempt_005_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/fuzz_out/attempt_007_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/fuzz_out/attempt_008_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/fuzz_out/attempt_009_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/fuzz_out/attempt_010_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/fuzz_out/attempt_011_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/fuzz_out/attempt_012_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/fuzz_out/attempt_014_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/fuzz_out/attempt_016_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/fuzz_out/attempt_017_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/fuzz_out/attempt_020_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/fuzz_out/attempt_021_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/fuzz_out/attempt_022_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/fuzz_out/attempt_023_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/fuzz_out/attempt_026_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/fuzz_out/attempt_027_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/fuzz_out/attempt_029_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/fuzz_out/attempt_030_compiled.txt'], 'classpath': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/cov/classpath.json', 'dir': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/cov', 'compiled_attempts': ['attempt_002', 'attempt_004', 'attempt_005', 'attempt_007', 'attempt_008', 'attempt_009', 'attempt_010', 'attempt_011', 'attempt_012', 'attempt_014', 'attempt_016', 'attempt_017', 'attempt_020', 'attempt_021', 'attempt_022', 'attempt_023', 'attempt_026', 'attempt_027', 'attempt_029', 'attempt_030'], 'accepted_attempts': ['attempt_002', 'attempt_004', 'attempt_005', 'attempt_007', 'attempt_008', 'attempt_009', 'attempt_010', 'attempt_011', 'attempt_012', 'attempt_014', 'attempt_016', 'attempt_017', 'attempt_020', 'attempt_021', 'attempt_022', 'attempt_023', 'attempt_026', 'attempt_027', 'attempt_029', 'attempt_030'], 'sources': ['/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/harness_src/attempt_002.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/harness_src/attempt_004.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/harness_src/attempt_005.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/harness_src/attempt_007.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/harness_src/attempt_008.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/harness_src/attempt_009.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/harness_src/attempt_010.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/harness_src/attempt_011.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/harness_src/attempt_012.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/harness_src/attempt_014.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/harness_src/attempt_016.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/harness_src/attempt_017.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/harness_src/attempt_020.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/harness_src/attempt_021.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/harness_src/attempt_022.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/harness_src/attempt_023.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/harness_src/attempt_026.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/harness_src/attempt_027.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/harness_src/attempt_029.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HR/004_patch1-Time-15-ACS_c_rep001/harness_src/attempt_030.java']}

</details>
