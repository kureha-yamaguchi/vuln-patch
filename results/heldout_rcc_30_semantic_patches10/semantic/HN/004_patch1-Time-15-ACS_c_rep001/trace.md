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

103 sequential steps — 32 LLM calls, 71 deterministic. Read top to bottom.

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
  "FailureTest(test_class='org.joda.time.field.TestFieldUtils', test_method='testSafeMultiplyLongInt', source_path='/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/004_patch1-Time-15-ACS_c_rep001/Time_15_buggy/src/test/java/org/joda/time/field/TestFieldUtils.java', method_source='    public void testSafeMultiplyLongInt() {\\n        assertEquals(0L, FieldUtils.safeMultiply(0L, 0));\\n        \\n        assertEquals(1L, FieldUtils.safeMultiply(1L, 1));\\n        assertEquals(3L, FieldUtils.safeMultiply(1L, 3));\\n        assertEquals(3L, FieldUtils.safeMultiply(3L, 1));\\n        \\n        assertEquals(6L, FieldUtils.safeMultiply(2L, 3));\\n        assertEquals(-6L, FieldUtils.safeMultiply(2L, -3));\\n        assertEquals(-6L, FieldUtils.safeMultiply(-2L, 3));\\n        assertEquals(6L, FieldUtils.safeMultiply(-2L, -3));\\n        \\n        assertEquals(-1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE));\\n        \\n        assertEquals(Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1));\\n        assertEquals(Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1));\\n        assertEquals(-Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1));\\n        \\n        try {\\n            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);\\n            fail();\\n        } catch (ArithmeticException e) {\\n        }\\n        \\n        try {\\n            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);\\n            fail();\\n        } catch (ArithmeticException e) {\\n        }\\n        \\n        try {\\n            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);\\n            fail();\\n        } catch (ArithmeticException e) {\\n        }\\n        \\n        try {\\n            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);\\n            fail();\\n        } catch (ArithmeticException e) {\\n        }\\n    }', exception_type='junit.framework.AssertionFailedError', failure_message=None, support_source=None)"
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
<details open><summary>▸ Output (~7,404 chars)</summary>

```
[
  {
    "name": "longInt_minValue_times_minusOne_throws",
    "kind": "invariant",
    "contract": "FieldUtils.safeMultiply(long, int) is documented to throw ArithmeticException if the value is too big or too small; Long.MIN_VALUE * -1 overflows a long.",
    "input": "Use the exact boundary named by the patch: val1 = Long.MIN_VALUE and val2 = -1. This input is valid to call and specifically exercises the added guard.",
    "check": "boolean violated = false;\ntry {\n    FieldUtils.safeMultiply(Long.MIN_VALUE, -1);\n    violated = true;\n} catch (ArithmeticException ok) {\n} catch (Throwable t) {\n    violated = true;\n}\nif (violated) {\n    throw new RuntimeException(\"relation longInt_minValue_times_minusOne_throws violated: safeMultiply(Long.MIN_VALUE, -1) must throw ArithmeticException\");\n}"
  },
  {
    "name": "overload_agreement_boundary_overflow_longInt_longLong",
    "kind": "metamorphic",
    "contract": "The same-name overloads safeMultiply(long, int) and safeMultiply(long, long) have the same documented semantics: multiply two values and throw ArithmeticException on long overflow. On equivalent inputs they must agree, including on overflow.",
    "input": "Use the overflow boundary val1 = Long.MIN_VALUE and val2 = -1, widening the second argument to long for the sibling overload.",
    "check": "boolean longIntThrows = false;\nboolean longLongThrows = false;\ntry {\n    FieldUtils.safeMultiply(Long.MIN_VALUE, -1);\n} catch (ArithmeticException ok) {\n    longIntThrows = true;\n} catch (Throwable t) {\n    throw new RuntimeException(\"relation overload_agreement_boundary_overflow_longInt_longLong violated: wrong exception from safeMultiply(long,int): \" + t.getClass().getName());\n}\ntry {\n    FieldUtils.safeMultiply(Long.MIN_VALUE, -1L);\n} catch (ArithmeticException ok) {\n    longLongThrows = true;\n} catch (Throwable t) {\n    throw new RuntimeException(\"relation overload_agreement_boundary_overflow_longInt_longLong violated: wrong exception from safeMultiply(long,long): \" + t.getClass().getName());\n}\nif (longIntThrows != longLongThrows) {\n    throw new RuntimeException(\"relation overload_agreement_boundary_overflow_longInt_longLong violated: overloads disagree on overflow for Long.MIN_VALUE * -1\");\n}\nif (!longIntThrows) {\n    throw new RuntimeException(\"relation overload_agreement_boundary_overflow_longInt_longLong violated: both overloads should throw on Long.MIN_VALUE * -1\");\n}"
  },
  {
    "name": "overload_agreement_longInt_longLong_small_inputs",
    "kind": "metamorphic",
    "contract": "The overloads safeMultiply(long, int) and safeMultiply(long, long) are documented identically apart from parameter type; on equivalent non-overflow inputs they must return the same long product.",
    "input": "Choose small magnitudes by construction so the product is well within long range: val1 from data.consumeInt(-1000000, 1000000) widened to long, and val2 from data.consumeInt(-1000000, 1000000).",
    "check": "long val1;\nint val2;\ntry {\n    val1 = (long) data.consumeInt(-1000000, 1000000);\n    val2 = data.consumeInt(-1000000, 1000000);\n} catch (Exception e) {\n    return;\n}\nlong r1;\ntry {\n    r1 = FieldUtils.safeMultiply(val1, val2);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation overload_agreement_longInt_longLong_small_inputs violated: unexpected \" + e.getClass().getName() + \" from safeMultiply(long,int) on valid-by-construction input: \" + e.getMessage());\n}\nlong r2;\ntry {\n    r2 = FieldUtils.safeMultiply(val1, (long) val2);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation overload_agreement_longInt_longLong_small_inputs violated: unexpected \" + e.getClass().getName() + \" from safeMultiply(long,long) on valid-by-construction input: \" + e.getMessage());\n}\nif (r1 != r2) {\n    throw new RuntimeException(\"relation overload_agreement_longInt_longLong_small_inputs violated: \" + r1 + \" vs \" + r2 + \" for inputs \" + val1 + \" and \" + val2);\n}"
  },
  {
    "name": "overload_agreement_longInt_intInt_small_inputs",
    "kind": "metamorphic",
    "contract": "The overloads safeMultiply(long, int) and safeMultiply(int, int) both document safe multiplication with overflow detection; when the first argument fits in int and the product fits in int, their results must agree up to widening to long.",
    "input": "Choose int inputs with small magnitude so the product is valid for both overloads: a = data.consumeInt(-46340, 46340), b = data.consumeInt(-46340, 46340).",
    "check": "int a;\nint b;\ntry {\n    a = data.consumeInt(-46340, 46340);\n    b = data.consumeInt(-46340, 46340);\n} catch (Exception e) {\n    return;\n}\nlong r1;\ntry {\n    r1 = FieldUtils.safeMultiply((long) a, b);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation overload_agreement_longInt_intInt_small_inputs violated: unexpected \" + e.getClass().getName() + \" from safeMultiply(long,int) on valid-by-construction input: \" + e.getMessage());\n}\nint r2;\ntry {\n    r2 = FieldUtils.safeMultiply(a, b);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation overload_agreement_longInt_intInt_small_inputs violated: unexpected \" + e.getClass().getName() + \" from safeMultiply(int,int) on valid-by-construction input: \" + e.getMessage());\n}\nif (r1 != (long) r2) {\n    throw new RuntimeException(\"relation overload_agreement_longInt_intInt_small_inputs violated: \" + r1 + \" vs \" + r2 + \" for inputs \" + a + \" and \" + b);\n}"
  },
  {
    "name": "multiplyToInt_composes_with_longInt_and_safeToInt",
    "kind": "metamorphic",
    "contract": "safeMultiplyToInt(long, long) is documented as multiplying two values and returning an int with overflow checks; therefore on inputs whose product fits in int it must equal safeToInt(safeMultiply(long, int)) on equivalent arguments.",
    "input": "Choose a from data.consumeInt(-46340, 46340) widened to long and b from data.consumeInt(-46340, 46340), ensuring the exact product fits in int.",
    "check": "long a;\nint b;\ntry {\n    a = (long) data.consumeInt(-46340, 46340);\n    b = data.consumeInt(-46340, 46340);\n} catch (Exception e) {\n    return;\n}\nlong product;\ntry {\n    product = FieldUtils.safeMultiply(a, b);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation multiplyToInt_composes_with_longInt_and_safeToInt violated: unexpected \" + e.getClass().getName() + \" from safeMultiply(long,int) on valid-by-construction input: \" + e.getMessage());\n}\nint viaComposition;\ntry {\n    viaComposition = FieldUtils.safeToInt(product);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation multiplyToInt_composes_with_longInt_and_safeToInt violated: unexpected \" + e.getClass().getName() + \" from safeToInt on valid-by-construction input: \" + e.getMessage());\n}\nint direct;\ntry {\n    direct = FieldUtils.safeMultiplyToInt(a, (long) b);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation multiplyToInt_composes_with_longInt_and_safeToInt violated: unexpected \" + e.getClass().getName() + \" from safeMultiplyToInt on valid-by-construction input: \" + e.getMessage());\n}\nif (viaComposition != direct) {\n    throw new RuntimeException(\"relation multiplyToInt_composes_with_longInt_and_safeToInt violated: \" + viaComposition + \" vs \" + direct + \" for inputs \" + a + \" and \" + b);\n}"
  }
]
```

</details>

---
## [5] ⚙️ screen · `longInt_minValue_times_minusOne_throws`
**output:** **dropped**
<details><summary>▸ reason (~469 chars)</summary>

- reason: boolean-swallow alarm: a catch block records only the bare flag `violated` (literal assignment, no rethrow, the caught exception is never used) and the alarm near line 10 is conditioned on `violated` — the caught exception's identity is destroyed, so attribution cannot tell WHICH exception fired or whether the unpatched build crashes the same way. Rethrow the exception as the alarm's cause (`..., e`), or assert on the real observable instead of a swallowed boolean.

</details>

---
## [6] ⚙️ screen · `overload_agreement_boundary_overflow_longInt_longLong`
**output:** **dropped**
<details><summary>▸ reason (~479 chars)</summary>

- reason: boolean-swallow alarm: a catch block records only the bare flag `longIntThrows` (literal assignment, no rethrow, the caught exception is never used) and the alarm near line 18 is conditioned on `longIntThrows` — the caught exception's identity is destroyed, so attribution cannot tell WHICH exception fired or whether the unpatched build crashes the same way. Rethrow the exception as the alarm's cause (`..., e`), or assert on the real observable instead of a swallowed boolean.

</details>

---
## [7] ⚙️ screen · `overload_agreement_longInt_longLong_small_inputs`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [8] ⚙️ screen-fuzz-buggy · `overload_agreement_longInt_longLong_small_inputs`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [9] ⚙️ screen · `overload_agreement_longInt_intInt_small_inputs`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [10] ⚙️ screen-fuzz-buggy · `overload_agreement_longInt_intInt_small_inputs`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [11] ⚙️ screen · `multiplyToInt_composes_with_longInt_and_safeToInt`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [12] ⚙️ screen-fuzz-buggy · `multiplyToInt_composes_with_longInt_and_safeToInt`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [13] ⚙️ screen · `overload_agreement_longInt_longLong_small_inputs`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [14] ⚙️ screen · `overload_agreement_longInt_intInt_small_inputs`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [15] ⚙️ screen · `multiplyToInt_composes_with_longInt_and_safeToInt`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [16] ⚙️ screening-survivors
**output:** 
```json
{
  "kept": [
    "overload_agreement_longInt_longLong_small_inputs",
    "overload_agreement_longInt_intInt_small_inputs",
    "multiplyToInt_composes_with_longInt_and_safeToInt"
  ],
  "count": 3
}
```

---
## [17] 🧠 LLM call — **rule synthesis** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [4] — not reprinted*
- *[user] message: identical to step [4] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,159 chars)</summary>

```
[
  {
    "name": "safeMultiplyLongInt_minValueTimesMinusOne_throws",
    "kind": "invariant",
    "contract": "safeMultiply(long, int) is documented to throw ArithmeticException if the value is too big or too small; multiplying Long.MIN_VALUE by -1 overflows a long, and the failing test fixes this exact boundary behavior.",
    "input": "Use the exact changed-condition boundary: val1 = Long.MIN_VALUE and val2 = -1. This input is valid by construction and isolates the documented overflow case governed by the added guard.",
    "check": "long a = Long.MIN_VALUE;\nint b = -1;\nboolean violated = false;\nString outcome = \"completed normally\";\ntry {\n    FieldUtils.safeMultiply(a, b);\n    violated = true;\n} catch (ArithmeticException ok) {\n    // contract honoured\n} catch (Throwable t) {\n    violated = true;\n    outcome = \"wrong exception class \" + t.getClass().getName();\n}\nif (violated) {\n    throw new RuntimeException(\"relation safeMultiplyLongInt_minValueTimesMinusOne_throws violated: \" + outcome);\n}"
  },
  {
    "name": "safeMultiplyLongInt_agreesWithLongLong",
    "kind": "metamorphic",
    "contract": "The overloads safeMultiply(long, int) and safeMultiply(long, long) have the same documented meaning—multiply two values, throwing on overflow—so on equivalent non-overflowing inputs they must return the same long result.",
    "input": "Choose a small long a and a small int b from data so overflow cannot occur, e.g. a in [-1000000,1000000] and b in [-1000,1000]. Compare safeMultiply(a,b) with safeMultiply(a,(long)b).",
    "check": "long a;\nint b;\ntry {\n    a = data.consumeLong(-1000000L, 1000000L);\n    b = data.consumeInt(-1000, 1000);\n} catch (Exception e) {\n    return;\n}\nlong r1;\ntry {\n    r1 = FieldUtils.safeMultiply(a, b);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation safeMultiplyLongInt_agreesWithLongLong violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nlong r2;\ntry {\n    r2 = FieldUtils.safeMultiply(a, (long) b);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation safeMultiplyLongInt_agreesWithLongLong violated: unexpected sibling exception \" + e.getClass().getName() + \" on equivalent valid input: \" + e.getMessage());\n}\nif (r1 != r2) {\n    throw new RuntimeException(\"relation safeMultiplyLongInt_agreesWithLongLong violated: \" + r1 + \" vs \" + r2 + \" for inputs \" + a + \", \" + b);\n}"
  },
  {
    "name": "safeMultiplyLongInt_agreesWithIntInt",
    "kind": "metamorphic",
    "contract": "The overloads safeMultiply(long, int) and safeMultiply(int, int) share the same documented multiplication semantics; when the first argument is an int and the product is chosen to stay within int range, their results must agree exactly.",
    "input": "Choose int a and int b from small ranges, e.g. both in [-30000,30000], so the product is within int range. Use a as both an int and a widened long.",
    "check": "int a;\nint b;\ntry {\n    a = data.consumeInt(-30000, 30000);\n    b = data.consumeInt(-30000, 30000);\n} catch (Exception e) {\n    return;\n}\nlong rLong;\ntry {\n    rLong = FieldUtils.safeMultiply((long) a, b);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation safeMultiplyLongInt_agreesWithIntInt violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nint rInt;\ntry {\n    rInt = FieldUtils.safeMultiply(a, b);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation safeMultiplyLongInt_agreesWithIntInt violated: unexpected sibling exception \" + e.getClass().getName() + \" on equivalent valid input: \" + e.getMessage());\n}\nif (rLong != (long) rInt) {\n    throw new RuntimeException(\"relation safeMultiplyLongInt_agreesWithIntInt violated: \" + rLong + \" vs \" + rInt + \" for inputs \" + a + \", \" + b);\n}"
  },
  {
    "name": "safeMultiplyLongInt_zeroAbsorbing",
    "kind": "invariant",
    "contract": "safeMultiply(long, int) is documented to multiply two values; for every correct multiplication, any value times zero equals zero, with no overflow.",
    "input": "Choose any long a from data and fix b = 0. This is valid by construction and probes the zero boundary explicitly handled by the method.",
    "check": "long a;\ntry {\n    a = data.consumeLong();\n} catch (Exception e) {\n    return;\n}\nlong r;\ntry {\n    r = FieldUtils.safeMultiply(a, 0);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation safeMultiplyLongInt_zeroAbsorbing violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nif (r != 0L) {\n    throw new RuntimeException(\"relation safeMultiplyLongInt_zeroAbsorbing violated: expected 0 but got \" + r + \" for input \" + a);\n}"
  },
  {
    "name": "safeMultiplyLongInt_matchesSafeMultiplyToIntAfterCast",
    "kind": "metamorphic",
    "contract": "safeMultiplyToInt(long, long) is documented as multiplying two values and returning an int with overflow checking; therefore, whenever safeMultiply(long, int) produces a result within int range, safeToInt(safeMultiply(long, int)) must agree with safeMultiplyToInt on the same numeric inputs.",
    "input": "Choose small values a in [-30000,30000] and b in [-30000,30000] so the product is safely within int range. Compare the casted long-result path to the direct int-result sibling path.",
    "check": "long a;\nint b;\ntry {\n    a = data.consumeLong(-30000L, 30000L);\n    b = data.consumeInt(-30000, 30000);\n} catch (Exception e) {\n    return;\n}\nlong prod;\ntry {\n    prod = FieldUtils.safeMultiply(a, b);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation safeMultiplyLongInt_matchesSafeMultiplyToIntAfterCast violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nint viaCast;\ntry {\n    viaCast = FieldUtils.safeToInt(prod);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation safeMultiplyLongInt_matchesSafeMultiplyToIntAfterCast violated: unexpected cast exception \" + e.getClass().getName() + \" on valid intermediate result: \" + e.getMessage());\n}\nint direct;\ntry {\n    direct = FieldUtils.safeMultiplyToInt(a, (long) b);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation safeMultiplyLongInt_matchesSafeMultiplyToIntAfterCast violated: unexpected sibling exception \" + e.getClass().getName() + \" on equivalent valid input: \" + e.getMessage());\n}\nif (viaCast != direct) {\n    throw new RuntimeException(\"relation safeMultiplyLongInt_matchesSafeMultiplyToIntAfterCast violated: \" + viaCast + \" vs \" + direct + \" for inputs \" + a + \", \" + b);\n}"
  },
  {
    "name": "safeMultiplyByTwo_equalsSafeAddSelf",
    "kind": "metamorphic",
    "contract": "safeMultiply and safeAdd are both documented to perform the corresponding arithmetic operation while throwing on overflow; for any long a where both operations are within range, multiplying by 2 must equal adding the value to itself.",
    "input": "Choose a in [-4611686018427387904, 4611686018427387903] (that is, [Long.MIN_VALUE/2, Long.MAX_VALUE/2]) so both 2*a and a+a are valid long results.",
    "check": "long a;\ntry {\n    a = data.consumeLong(-4611686018427387904L, 4611686018427387903L);\n} catch (Exception e) {\n    return;\n}\nlong rMul;\ntry {\n    rMul = FieldUtils.safeMultiply(a, 2);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation safeMultiplyByTwo_equalsSafeAddSelf violated: unexpected \" + e.getClass().getName() + \" from safeMultiply on valid-by-construction input: \" + e.getMessage());\n}\nlong rAdd;\ntry {\n    rAdd = FieldUtils.safeAdd(a, a);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation safeMultiplyByTwo_equalsSafeAddSelf violated: unexpected sibling exception \" + e.getClass().getName() + \" from safeAdd on equivalent valid input: \" + e.getMessage());\n}\nif (rMul != rAdd) {\n    throw new RuntimeException(\"relation safeMultiplyByTwo_equalsSafeAddSelf violated: \" + rMul + \" vs \" + rAdd + \" for input \" + a);\n}"
  }
]
```

</details>

---
## [18] ⚙️ screen-fuzz-buggy · `safeMultiplyLongInt_minValueTimesMinusOne_throws`
**output:** 
```json
{
  "checked": 20000,
  "violated": 20000,
  "ratio": 1.0
}
```

---
## [19] ⚙️ screen · `safeMultiplyLongInt_agreesWithLongLong`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [20] ⚙️ screen-fuzz-buggy · `safeMultiplyLongInt_agreesWithLongLong`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [21] ⚙️ screen · `safeMultiplyLongInt_agreesWithIntInt`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [22] ⚙️ screen-fuzz-buggy · `safeMultiplyLongInt_agreesWithIntInt`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [23] ⚙️ screen · `safeMultiplyLongInt_zeroAbsorbing`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [24] ⚙️ screen-fuzz-buggy · `safeMultiplyLongInt_zeroAbsorbing`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [25] ⚙️ screen · `safeMultiplyLongInt_matchesSafeMultiplyToIntAfterCast`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [26] ⚙️ screen-fuzz-buggy · `safeMultiplyLongInt_matchesSafeMultiplyToIntAfterCast`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [27] ⚙️ screen · `safeMultiplyByTwo_equalsSafeAddSelf`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [28] ⚙️ screen-fuzz-buggy · `safeMultiplyByTwo_equalsSafeAddSelf`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [29] ⚙️ screen · `safeMultiplyLongInt_minValueTimesMinusOne_throws`
**output:** **kept**
- reason: kept: direction-confirmed

---
## [30] ⚙️ screen · `safeMultiplyLongInt_agreesWithLongLong`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [31] ⚙️ screen · `safeMultiplyLongInt_agreesWithIntInt`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [32] ⚙️ screen · `safeMultiplyLongInt_zeroAbsorbing`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [33] ⚙️ screen · `safeMultiplyLongInt_matchesSafeMultiplyToIntAfterCast`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [34] ⚙️ screen · `safeMultiplyByTwo_equalsSafeAddSelf`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [35] ⚙️ reference-widening · `enumerate`
**output:** **nothing to widen**
- reason: the kept checks name no no-argument reader of the patched class — nothing to request a reference for; 12 further name(s) the checks probe are outside the patched class's no-argument readers and were excluded (['safemultiply', 'name', 'message', 'fieldutils', 'safenegate', 'safeadd'])
<details><summary>▸ detail (~2,715 chars)</summary>

- detail: {'targets': [], 'already_admitted': [], 'cap': 3, 'checks_read': 9, 'excluded': [{'key': 'safemultiply', 'why': "the patched class declares `safeMultiply` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}, {'key': 'name', 'why': 'the patched class does not declare `getName()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}, {'key': 'message', 'why': 'the patched class does not declare `getMessage()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}, {'key': 'fieldutils', 'why': 'the patched class declares `FieldUtils()` as a constructor or a void method — it reports no observable value'}, {'key': 'safenegate', 'why': "the patched class declares `safeNegate` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}, {'key': 'safeadd', 'why': "the patched class declares `safeAdd` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}, {'key': 'safesubtract', 'why': "the patched class declares `safeSubtract` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}, {'key': 'safetoint', 'why': "the patched class declares `safeToInt` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}, {'key': 'safemultiplytoint', 'why': "the patched class declares `safeMultiplyToInt` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}, {'key': 'verifyvaluebounds', 'why': "the patched class declares `verifyValueBounds` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}, {'key': 'wrappedvalue', 'why': "the patched class declares `getWrappedValue` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}, {'key': 'illegalfieldvalueexception', 'why': 'the patched class does not declare `IllegalFieldValueException()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}], 'excluded_count': 12}

</details>

---
## [36] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~42,549 chars)</summary>

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
<relation name="overload_agreement_longInt_longLong_small_inputs" kind="metamorphic">
  // holds because: The overloads safeMultiply(long, int) and safeMultiply(long, long) are documented identically apart from parameter type; on equivalent non-overflow inputs they must return the same long product.
  // valid input: Choose small magnitudes by construction so the product is well within long range: val1 from data.consumeInt(-1000000, 1000000) widened to long, and val2 from data.consumeInt(-1000000, 1000000).
  // screen: fired on 0/20000 fuzzed inputs on the buggy build (silent on the buggy build)
long val1;
int val2;
try {
    val1 = (long) data.consumeInt(-1000000, 1000000);
    val2 = data.consumeInt(-1000000, 1000000);
} catch (Exception e) {
    return;
}
long r1;
try {
    r1 = FieldUtils.safeMultiply(val1, val2);
} catch (Exception e) { for (StackTraceElement __rpgf : e.getStackTrace()) { String __rpgc = __rpgf.getClassName(); if ((__rpgc.equals("FieldUtils") || __rpgc.endsWith(".FieldUtils") || __rpgc.endsWith("$FieldUtils")) && !"<init>".equals(__rpgf.getMethodName())) { boolean __rpgdoc = false; if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "getWrappedValue".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IllegalArgumentException") || __rpn.endsWith(".IllegalArgumentException") || __rpn.endsWith("$IllegalArgumentException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "safeAdd".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArithmeticException") || __rpn.endsWith(".ArithmeticException") || __rpn.endsWith("$ArithmeticException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "safeMultiply".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArithmeticException") || __rpn.endsWith(".ArithmeticException") || __rpn.endsWith("$ArithmeticException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "safeMultiplyToInt".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArithmeticException") || __rpn.endsWith(".ArithmeticException") || __rpn.endsWith("$ArithmeticException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "safeNegate".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArithmeticException") || __rpn.endsWith(".ArithmeticException") || __rpn.endsWith("$ArithmeticException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "safeSubtract".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArithmeticException") || __rpn.endsWith(".ArithmeticException") || __rpn.endsWith("$ArithmeticException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "safeToInt".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArithmeticException") || __rpn.endsWith(".ArithmeticException") || __rpn.endsWith("$ArithmeticException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "verifyValueBounds".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IllegalFieldValueException") || __rpn.endsWith(".IllegalFieldValueException") || __rpn.endsWith("$IllegalFieldValueException")) { __rpgdoc = true; break; } } } if (__rpgdoc) return; break; } }
    throw new RuntimeException("relation overload_agreement_longInt_longLong_small_inputs violated: unexpected " + e.getClass().getName() + " from safeMultiply(long,int) on valid-by-construction input: " + e.getMessage());
}
long r2;
try {
    r2 = FieldUtils.safeMultiply(val1, (long) val2);
} catch (Exception e) { for (StackTraceElement __rpgf : e.getStackTrace()) { String __rpgc = __rpgf.getClassName(); if ((__rpgc.equals("FieldUtils") || __rpgc.endsWith(".FieldUtils") || __rpgc.endsWith("$FieldUtils")) && !"<init>".equals(__rpgf.getMethodName())) { boolean __rpgdoc = false; if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "getWrappedValue".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IllegalArgumentException") || __rpn.endsWith(".IllegalArgumentException") || __rpn.endsWith("$IllegalArgumentException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "safeAdd".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArithmeticException") || __rpn.endsWith(".ArithmeticException") || __rpn.endsWith("$ArithmeticException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "safeMultiply".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArithmeticException") || __rpn.endsWith(".ArithmeticException") || __rpn.endsWith("$ArithmeticException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "safeMultiplyToInt".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArithmeticException") || __rpn.endsWith(".ArithmeticException") || __rpn.endsWith("$ArithmeticException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "safeNegate".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArithmeticException") || __rpn.endsWith(".ArithmeticException") || __rpn.endsWith("$ArithmeticException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "safeSubtract".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArithmeticException") || __rpn.endsWith(".ArithmeticException") || __rpn.endsWith("$ArithmeticException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "safeToInt".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArithmeticException") || __rpn.endsWith(".ArithmeticException") || __rpn.endsWith("$ArithmeticException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "verifyValueBounds".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IllegalFieldValueException") || __rpn.endsWith(".IllegalFieldValueException") || __rpn.endsWith("$IllegalFieldValueException")) { __rpgdoc = true; break; } } } if (__rpgdoc) return; break; } }
    throw new RuntimeException("relation overload_agreement_longInt_longLong_small_inputs violated: unexpected " + e.getClass().getName() + " from safeMultiply(long,long) on valid-by-construction input: " + e.getMessage());
}
if (r1 != r2) {
    throw new RuntimeException("relation overload_agreement_longInt_longLong_small_inputs violated: " + r1 + " vs " + r2 + " for inputs " + val1 + " and " + val2);
}
</relation>
<relation name="overload_agreement_longInt_intInt_small_inputs" kind="metamorphic">
  // holds because: The overloads safeMultiply(long, int) and safeMultiply(int, int) both document safe multiplication with overflow detection; when the first argument fits in int and the product fits in int, their results must agree up to widening to long.
  // valid input: Choose int inputs with small magnitude so the product is valid for both overloads: a = data.consumeInt(-46340, 46340), b = data.consumeInt(-46340, 46340).
  // screen: fired on 0/20000 fuzzed inputs on the buggy build (silent on the buggy build)
int a;
int b;
try {
    a = data.consumeInt(-46340, 46340);
    b = data.consumeInt(-46340, 46340);
} catch (Exception e) {
    return;
}
long r1;
try {
    r1 = FieldUtils.safeMultiply((long) a, b);
} catch (Exception e) { for (StackTraceElement __rpgf : e.getStackTrace()) { String __rpgc = __rpgf.getClassName(); if ((__rpgc.equals("FieldUtils") || __rpgc.endsWith(".FieldUtils") || __rpgc.endsWith("$FieldUtils")) && !"<init>".equals(__rpgf.getMethodName())) { boolean __rpgdoc = false; if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "getWrappedValue".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IllegalArgumentException") || __rpn.endsWith(".IllegalArgumentException") || __rpn.endsWith("$IllegalArgumentException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "safeAdd".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArithmeticException") || __rpn.endsWith(".ArithmeticException") || __rpn.endsWith("$ArithmeticException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "safeMultiply".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArithmeticException") || __rpn.endsWith(".ArithmeticException") || __rpn.endsWith("$ArithmeticException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "safeMultiplyToInt".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArithmeticException") || __rpn.endsWith(".ArithmeticException") || __rpn.endsWith("$ArithmeticException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "safeNegate".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArithmeticException") || __rpn.endsWith(".ArithmeticException") || __rpn.endsWith("$ArithmeticException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "safeSubtract".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArithmeticException") || __rpn.endsWith(".ArithmeticException") || __rpn.endsWith("$ArithmeticException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "safeToInt".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArithmeticException") || __rpn.endsWith(".ArithmeticException") || __rpn.endsWith("$ArithmeticException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "verifyValueBounds".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IllegalFieldValueException") || __rpn.endsWith(".IllegalFieldValueException") || __rpn.endsWith("$IllegalFieldValueException")) { __rpgdoc = true; break; } } } if (__rpgdoc) return; break; } }
    throw new RuntimeException("relation overload_agreement_longInt_intInt_small_inputs violated: unexpected " + e.getClass().getName() + " from safeMultiply(long,int) on valid-by-construction input: " + e.getMessage());
}
int r2;
try {
    r2 = FieldUtils.safeMultiply(a, b);
} catch (Exception e) { for (StackTraceElement __rpgf : e.getStackTrace()) { String __rpgc = __rpgf.getClassName(); if ((__rpgc.equals("FieldUtils") || __rpgc.endsWith(".FieldUtils") || __rpgc.endsWith("$FieldUtils")) && !"<init>".equals(__rpgf.getMethodName())) { boolean __rpgdoc = false; if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "getWrappedValue".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IllegalArgumentException") || __rpn.endsWith(".IllegalArgumentException") || __rpn.endsWith("$IllegalArgumentException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "safeAdd".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArithmeticException") || __rpn.endsWith(".ArithmeticException") || __rpn.endsWith("$ArithmeticException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "safeMultiply".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArithmeticException") || __rpn.endsWith(".ArithmeticException") || __rpn.endsWith("$ArithmeticException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "safeMultiplyToInt".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArithmeticException") || __rpn.endsWith(".ArithmeticException") || __rpn.endsWith("$ArithmeticException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "safeNegate".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArithmeticException") || __rpn.endsWith(".ArithmeticException") || __rpn.endsWith("$ArithmeticException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "safeSubtract".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArithmeticException") || __rpn.endsWith(".ArithmeticException") || __rpn.endsWith("$ArithmeticException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "safeToInt".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("ArithmeticException") || __rpn.endsWith(".ArithmeticException") || __rpn.endsWith("$ArithmeticException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("FieldUtils") || __rpgf.getClassName().endsWith(".FieldUtils") || __rpgf.getClassName().endsWith("$FieldUtils")) && "verifyValueBounds".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("IllegalFieldValueException") || __rpn.endsWith(".IllegalFieldValueException") || __rpn.endsWith("$IllegalFieldValueException")) { __rpgdoc = true; break; } } } if (__rpgdoc) return; break; } }
    throw new RuntimeException("relation overload_agreement_longInt_intInt_small_inputs violated: unexpected " + e.getClass().getName() + " from safeMultiply(int,int) on valid-by-construction input: " + e.getMessage());
}
if (r1 != (long) r2) {
    throw new RuntimeException("relation overload_agreement_longInt_intInt_small_inputs violated: " + r1 + " vs " + r2 + " for inputs " + a + " and " + b);
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
<details open><summary>▸ Output (~5,618 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedOraclePairs();
        checkLiftedOracleRejections();
        checkMetamorphicNegationAgreement(data);
        checkOverloadAgreementLongIntLongLong(data);
        checkOverloadAgreementLongIntIntInt(data);
    }

    private static void checkLiftedOraclePairs() {
        assertEqualsLong("[oracle:lifted-0x0]", 0L, FieldUtils.safeMultiply(0L, 0), "FieldUtils.safeMultiply(0L, 0)");
        assertEqualsLong("[oracle:lifted-1x1]", 1L, FieldUtils.safeMultiply(1L, 1), "FieldUtils.safeMultiply(1L, 1)");
        assertEqualsLong("[oracle:lifted-1x3]", 3L, FieldUtils.safeMultiply(1L, 3), "FieldUtils.safeMultiply(1L, 3)");
        assertEqualsLong("[oracle:lifted-3x1]", 3L, FieldUtils.safeMultiply(3L, 1), "FieldUtils.safeMultiply(3L, 1)");
        assertEqualsLong("[oracle:lifted-2x3]", 6L, FieldUtils.safeMultiply(2L, 3), "FieldUtils.safeMultiply(2L, 3)");
        assertEqualsLong("[oracle:lifted-2x-3]", -6L, FieldUtils.safeMultiply(2L, -3), "FieldUtils.safeMultiply(2L, -3)");
        assertEqualsLong("[oracle:lifted--2x3]", -6L, FieldUtils.safeMultiply(-2L, 3), "FieldUtils.safeMultiply(-2L, 3)");
        assertEqualsLong("[oracle:lifted--2x-3]", 6L, FieldUtils.safeMultiply(-2L, -3), "FieldUtils.safeMultiply(-2L, -3)");
        assertEqualsLong("[oracle:lifted--1xminint]", -1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE), "FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE)");
        assertEqualsLong("[oracle:lifted-maxx1]", Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1), "FieldUtils.safeMultiply(Long.MAX_VALUE, 1)");
        assertEqualsLong("[oracle:lifted-minx1]", Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1), "FieldUtils.safeMultiply(Long.MIN_VALUE, 1)");
        assertEqualsLong("[oracle:lifted-maxx-1]", -Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1), "FieldUtils.safeMultiply(Long.MAX_VALUE, -1)");
    }

    private static void checkLiftedOracleRejections() {
        assertThrowsArithmetic("[oracle:lifted-minx-1-throws]", Long.MIN_VALUE, -1);
        assertThrowsArithmetic("[oracle:lifted-minx100-throws]", Long.MIN_VALUE, 100);
        assertThrowsArithmetic("[oracle:lifted-minxmaxint-throws]", Long.MIN_VALUE, Integer.MAX_VALUE);
        assertThrowsArithmetic("[oracle:lifted-maxxminint-throws]", Long.MAX_VALUE, Integer.MIN_VALUE);
    }

    private static void checkMetamorphicNegationAgreement(FuzzedDataProvider data) {
        long val = (long) data.consumeInt(-1_000_000, 1_000_000);
        try {
            long lhs = FieldUtils.safeMultiply(val, -1);
            long rhs = (long) FieldUtils.safeNegate((int) val);
            if (lhs != rhs) {
                throw new FuzzerSecurityIssueLow("[oracle:negation-agreement] metamorphic violation: safeMultiply(v,-1) must agree with safeNegate(v) on int-range non-overflow input; input=" + val + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Exception e) {
            return;
        }
    }

    private static void checkOverloadAgreementLongIntLongLong(FuzzedDataProvider data) {
        long val1;
        int val2;
        try {
            val1 = (long) data.consumeInt(-1_000_000, 1_000_000);
            val2 = data.consumeInt(-1_000_000, 1_000_000);
        } catch (Exception e) {
            return;
        }

        long r1;
        try {
            r1 = FieldUtils.safeMultiply(val1, val2);
        } catch (Exception e) {
            return;
        }

        long r2;
        try {
            r2 = FieldUtils.safeMultiply(val1, (long) val2);
        } catch (Exception e) {
            return;
        }

        if (r1 != r2) {
            throw new FuzzerSecurityIssueLow("relation overload_agreement_longInt_longLong_small_inputs violated: inputs=" + val1 + "," + val2 + " lhs=" + r1 + " rhs=" + r2);
        }
    }

    private static void checkOverloadAgreementLongIntIntInt(FuzzedDataProvider data) {
        int a;
        int b;
        try {
            a = data.consumeInt(-46340, 46340);
            b = data.consumeInt(-46340, 46340);
        } catch (Exception e) {
            return;
        }

        long r1;
        try {
            r1 = FieldUtils.safeMultiply((long) a, b);
        } catch (Exception e) {
            return;
        }

        int r2;
        try {
            r2 = FieldUtils.safeMultiply(a, b);
        } catch (Exception e) {
            return;
        }

        if (r1 != (long) r2) {
            throw new FuzzerSecurityIssueLow("relation overload_agreement_longInt_intInt_small_inputs violated: inputs=" + a + "," + b + " lhs=" + r1 + " rhs=" + r2);
        }
    }

    private static void assertEqualsLong(String oracleId, long expected, long actual, String expr) {
        if (expected != actual) {
            throw new FuzzerSecurityIssueLow(oracleId + " semantic mismatch: " + expr + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertThrowsArithmetic(String oracleId, long val1, int val2) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new FuzzerSecurityIssueLow(oracleId + " semantic mismatch: expected ArithmeticException from FieldUtils.safeMultiply(" + val1 + ", " + val2 + ") but returned " + actual);
        } catch (ArithmeticException expected) {
        }
    }
}
```

</details>

---
## [37] ⚙️ harness-repair · `attempt_001`
**output:** **REPAIRED: swallowed-alarm, missing-alarm-id**
- detail: {'applied': ['swallowed-alarm', 'missing-alarm-id'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [38] ⚙️ harness-attempt · `attempt_001`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm, missing-alarm-id]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm', 'missing-alarm-id'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:unnamed-check] [oracle:lifted-minx-1-throws] semantic mismatch: expected ArithmeticException from FieldUtils.safeMultiply(-922337203685…

---
## [39] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~46,124 chars, 1 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
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
<details open><summary>▸ Output (~3,834 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkEquals("t0", 0L, 0, 0L);
        checkEquals("t1", 1L, 1, 1L);
        checkEquals("t2", 1L, 3, 3L);
        checkEquals("t3", 3L, 1, 3L);

        checkEquals("t4", 2L, 3, 6L);
        checkEquals("t5", 2L, -3, -6L);
        checkEquals("t6", -2L, 3, -6L);
        checkEquals("t7", -2L, -3, 6L);

        checkEquals("t8", -1L, Integer.MIN_VALUE, -1L * Integer.MIN_VALUE);

        checkEquals("t9", Long.MAX_VALUE, 1, Long.MAX_VALUE);
        checkEquals("t10", Long.MIN_VALUE, 1, Long.MIN_VALUE);
        checkEquals("t11", Long.MAX_VALUE, -1, -Long.MAX_VALUE);

        checkThrowsArithmetic("t12", Long.MIN_VALUE, -1);
        checkThrowsArithmetic("t13", Long.MIN_VALUE, 100);
        checkThrowsArithmetic("t14", Long.MIN_VALUE, Integer.MAX_VALUE);
        checkThrowsArithmetic("t15", Long.MAX_VALUE, Integer.MIN_VALUE);

        long fuzzVal = data.consumeInt(-1_000_000, 1_000_000);
        int fuzzMul = data.consumeInt(-1_000_000, 1_000_000);

        try {
            long lhs = FieldUtils.safeMultiply(fuzzVal, fuzzMul);
            long rhs = FieldUtils.safeMultiply(fuzzVal, (long) fuzzMul);
            if (lhs != rhs) {
                throw new RuntimeException(
                    "[oracle:overload-long-long] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) on equivalent inputs per their shared documented contract input=("
                        + fuzzVal + "," + fuzzMul + ") lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Throwable ignored) {
            return;
        }

        int fuzzIntVal = data.consumeInt(-46340, 46340);
        int fuzzIntMul = data.consumeInt(-46340, 46340);

        try {
            long lhs = FieldUtils.safeMultiply((long) fuzzIntVal, fuzzIntMul);
            int rhsInt = FieldUtils.safeMultiply(fuzzIntVal, fuzzIntMul);
            long rhs = (long) rhsInt;
            if (lhs != rhs) {
                throw new RuntimeException(
                    "[oracle:overload-int-longint] metamorphic violation: safeMultiply(int,int) and safeMultiply(long,int) must agree when the long input is exactly the same int-valued number input=("
                        + fuzzIntVal + "," + fuzzIntMul + ") lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Throwable ignored) {
            return;
        }
    }

    private static void checkEquals(String id, long val1, int val2, long expected) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            if (actual != expected) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + id + "] semantic mismatch: FieldUtils.safeMultiply(" + val1 + ", " + val2
                        + ") expected=" + expected + " actual=" + actual);
            }
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: FieldUtils.safeMultiply(" + val1 + ", " + val2
                    + ") unexpectedly threw ArithmeticException");
        }
    }

    private static void checkThrowsArithmetic(String id, long val1, int val2) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: FieldUtils.safeMultiply(" + val1 + ", " + val2
                    + ") expected ArithmeticException but returned=" + actual);
        } catch (ArithmeticException expected) {
        }
    }
}
```

</details>

---
## [40] ⚙️ harness-attempt · `attempt_002`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:t12] semantic mismatch: FieldUtils.safeMultiply(-9223372036854775808, -1) expected ArithmeticException but returned=-922337203685477580…

---
## [41] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [36] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,467 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedOraclePairs();
        checkLiftedOracleExpectedRejections();
        checkMetamorphicOverloadAgreementLongIntLongLong(data);
        checkMetamorphicOverloadAgreementLongIntIntInt(data);
        checkMetamorphicNegationRelation(data);
    }

    private static void checkLiftedOraclePairs() {
        assertLongEquals("lifted-0", 0L, FieldUtils.safeMultiply(0L, 0), "FieldUtils.safeMultiply(0L, 0)");
        assertLongEquals("lifted-1", 1L, FieldUtils.safeMultiply(1L, 1), "FieldUtils.safeMultiply(1L, 1)");
        assertLongEquals("lifted-2", 3L, FieldUtils.safeMultiply(1L, 3), "FieldUtils.safeMultiply(1L, 3)");
        assertLongEquals("lifted-3", 3L, FieldUtils.safeMultiply(3L, 1), "FieldUtils.safeMultiply(3L, 1)");
        assertLongEquals("lifted-4", 6L, FieldUtils.safeMultiply(2L, 3), "FieldUtils.safeMultiply(2L, 3)");
        assertLongEquals("lifted-5", -6L, FieldUtils.safeMultiply(2L, -3), "FieldUtils.safeMultiply(2L, -3)");
        assertLongEquals("lifted-6", -6L, FieldUtils.safeMultiply(-2L, 3), "FieldUtils.safeMultiply(-2L, 3)");
        assertLongEquals("lifted-7", 6L, FieldUtils.safeMultiply(-2L, -3), "FieldUtils.safeMultiply(-2L, -3)");
        assertLongEquals("lifted-8", -1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE),
                "FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE)");
        assertLongEquals("lifted-9", Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1),
                "FieldUtils.safeMultiply(Long.MAX_VALUE, 1)");
        assertLongEquals("lifted-10", Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1),
                "FieldUtils.safeMultiply(Long.MIN_VALUE, 1)");
        assertLongEquals("lifted-11", -Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1),
                "FieldUtils.safeMultiply(Long.MAX_VALUE, -1)");
    }

    private static void checkLiftedOracleExpectedRejections() {
        assertArithmeticException("lifted-ex-0", Long.MIN_VALUE, -1);
        assertArithmeticException("lifted-ex-1", Long.MIN_VALUE, 100);
        assertArithmeticException("lifted-ex-2", Long.MIN_VALUE, Integer.MAX_VALUE);
        assertArithmeticException("lifted-ex-3", Long.MAX_VALUE, Integer.MIN_VALUE);
    }

    private static void checkMetamorphicOverloadAgreementLongIntLongLong(FuzzedDataProvider data) {
        long val1;
        int val2;
        try {
            val1 = (long) data.consumeInt(-1000000, 1000000);
            val2 = data.consumeInt(-1000000, 1000000);
        } catch (Throwable t) {
            return;
        }

        long r1;
        try {
            r1 = FieldUtils.safeMultiply(val1, val2);
        } catch (Throwable t) {
            return;
        }

        long r2;
        try {
            r2 = FieldUtils.safeMultiply(val1, (long) val2);
        } catch (Throwable t) {
            return;
        }

        // Contract justification: safeMultiply(long,int) and safeMultiply(long,long) both compute the same mathematical
        // product with overflow checking; on valid small inputs the observable result must agree. A patch that merely
        // deletes the throw or returns a wrong value in one overload violates this post-condition without crashing.
        if (r1 != r2) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:overload-agreement-longInt-longLong] metamorphic violation: FieldUtils.safeMultiply(long,int) != FieldUtils.safeMultiply(long,long) inputVal1="
                            + val1 + " inputVal2=" + val2 + " lhs=" + r1 + " rhs=" + r2);
        }
    }

    private static void checkMetamorphicOverloadAgreementLongIntIntInt(FuzzedDataProvider data) {
        int a;
        int b;
        try {
            a = data.consumeInt(-46340, 46340);
            b = data.consumeInt(-46340, 46340);
        } catch (Throwable t) {
            return;
        }

        long r1;
        try {
            r1 = FieldUtils.safeMultiply((long) a, b);
        } catch (Throwable t) {
            return;
        }

        int r2;
        try {
            r2 = FieldUtils.safeMultiply(a, b);
        } catch (Throwable t) {
            return;
        }

        // Contract justification: the int/int and long/int overloads both implement safe multiplication; when both inputs
        // and result fit in int, widening the int result to long must equal the long/int result. This detects silent
        // wrong-value fixes even when no exception is involved.
        if (r1 != (long) r2) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:overload-agreement-longInt-intInt] metamorphic violation: FieldUtils.safeMultiply(long,int) != widened FieldUtils.safeMultiply(int,int) inputA="
                            + a + " inputB=" + b + " lhs=" + r1 + " rhs=" + r2);
        }
    }

    private static void checkMetamorphicNegationRelation(FuzzedDataProvider data) {
        int v1;
        int v2;
        try {
            v1 = data.consumeInt(-1000000, 1000000);
            v2 = data.consumeInt(-1000000, 1000000);
        } catch (Throwable t) {
            return;
        }

        long product;
        try {
            product = FieldUtils.safeMultiply((long) v1, v2);
        } catch (Throwable t) {
            return;
        }

        long negatedProduct;
        try {
            negatedProduct = FieldUtils.safeMultiply(product, -1);
        } catch (Throwable t) {
            return;
        }

        long productWithNegatedFactor;
        try {
            productWithNegatedFactor = FieldUtils.safeMultiply((long) v1, -v2);
        } catch (Throwable t) {
            return;
        }

        // Contract justification: multiplication by -1 is negation, so for any non-overflowing calls
        // safeMultiply(safeMultiply(x,y), -1) must equal safeMultiply(x, -y). This is a real-library metamorphic
        // post-condition that a throw-deleting or wrong-value patch in the val2 == -1 branch would break observably.
        if (negatedProduct != productWithNegatedFactor) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:negation-relation] metamorphic violation: safeMultiply(safeMultiply(x,y),-1) != safeMultiply(x,-y) x="
                            + v1 + " y=" + v2 + " lhs=" + negatedProduct + " rhs=" + productWithNegatedFactor);
        }
    }

    private static void assertArithmeticException(String oracleId, long val1, int val2) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: expected ArithmeticException from FieldUtils.safeMultiply("
                            + val1 + ", " + val2 + ") but returned " + actual);
        } catch (ArithmeticException expected) {
            return;
        }
    }

    private static void assertLongEquals(String oracleId, long expected, long actual, String expr) {
        if (expected != actual) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: " + expr + " expected=" + expected + " actual=" + actual);
        }
    }
}
```

</details>

---
## [42] ⚙️ harness-attempt · `attempt_003`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-ex-0] semantic mismatch: expected ArithmeticException from FieldUtils.safeMultiply(-9223372036854775808, -1) but returned -92233…

---
## [43] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [39] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~4,584 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedTestOracles();

        long fuzzVal1 = data.consumeInt(-1_000_000, 1_000_000);
        int fuzzVal2 = data.consumeInt(-1_000, 1_000);

        try {
            long lhs = FieldUtils.safeMultiply(fuzzVal1, fuzzVal2);
            long rhs = FieldUtils.safeMultiply(fuzzVal1, (long) fuzzVal2);
            // Contract: same-name overloads safeMultiply(long,int) and safeMultiply(long,long)
            // both "Multiply two values throwing an exception if overflow occurs"; for the same
            // mathematical operands on inputs where both return normally, they must produce the
            // same product. A throw-deleting or wrong-value patch in the long,int overload breaks this.
            if (lhs != rhs) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: safeMultiply(long,int) != safeMultiply(long,long)"
                        + " input=(" + fuzzVal1 + "," + fuzzVal2 + ") lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (ArithmeticException e) {
            return;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        int a = data.consumeInt(-1_000_000, 1_000_000);
        int b = data.consumeInt(-1_000_000, 1_000_000);
        try {
            long product = FieldUtils.safeMultiply((long) a, b);
            if (b != 0) {
                long recovered = product / b;
                // Oracle from the input itself: we first choose moderate a and b; for non-zero b,
                // if safeMultiply returns normally then the returned product must satisfy exact
                // integer division back to the original multiplicand.
                if (recovered != a) {
                    throw new RuntimeException(
                        "[oracle:division-inverse] metamorphic violation: product/b != original"
                            + " input=(" + a + "," + b + ") product=" + product + " recovered=" + recovered);
                }
            }
        } catch (ArithmeticException e) {
            return;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }

    private static void checkLiftedTestOracles() {
        assertEqualsLong("seed-0", 0L, FieldUtils.safeMultiply(0L, 0));
        assertEqualsLong("seed-1", 1L, FieldUtils.safeMultiply(1L, 1));
        assertEqualsLong("seed-2", 3L, FieldUtils.safeMultiply(1L, 3));
        assertEqualsLong("seed-3", 3L, FieldUtils.safeMultiply(3L, 1));

        assertEqualsLong("seed-4", 6L, FieldUtils.safeMultiply(2L, 3));
        assertEqualsLong("seed-5", -6L, FieldUtils.safeMultiply(2L, -3));
        assertEqualsLong("seed-6", -6L, FieldUtils.safeMultiply(-2L, 3));
        assertEqualsLong("seed-7", 6L, FieldUtils.safeMultiply(-2L, -3));

        assertEqualsLong("seed-8", -1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE));

        assertEqualsLong("seed-9", Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1));
        assertEqualsLong("seed-10", Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1));
        assertEqualsLong("seed-11", -Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1));

        assertThrowsArithmetic("seed-12", Long.MIN_VALUE, -1);
        assertThrowsArithmetic("seed-13", Long.MIN_VALUE, 100);
        assertThrowsArithmetic("seed-14", Long.MIN_VALUE, Integer.MAX_VALUE);
        assertThrowsArithmetic("seed-15", Long.MAX_VALUE, Integer.MIN_VALUE);
    }

    private static void assertEqualsLong(String id, long expected, long actual) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertThrowsArithmetic(String id, long val1, int val2) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: expected ArithmeticException for input=("
                    + val1 + "," + val2 + ") actualReturn=" + actual);
        } catch (ArithmeticException expected) {
        }
    }
}
```

</details>

---
## [44] ⚙️ harness-attempt · `attempt_004`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:seed-12] semantic mismatch: expected ArithmeticException for input=(-9223372036854775808,-1) actualReturn=-9223372036854775808

---
## [45] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [36] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,783 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedOracles();
        checkOverloadAgreementLongIntLongLong(data);
        checkOverloadAgreementLongIntIntInt(data);
        checkMetamorphicNegationIdentity(data);
    }

    private static void checkLiftedOracles() {
        assertEqualsLong("lifted-0", 0L, FieldUtils.safeMultiply(0L, 0), "FieldUtils.safeMultiply(0L, 0)");
        assertEqualsLong("lifted-1", 1L, FieldUtils.safeMultiply(1L, 1), "FieldUtils.safeMultiply(1L, 1)");
        assertEqualsLong("lifted-2", 3L, FieldUtils.safeMultiply(1L, 3), "FieldUtils.safeMultiply(1L, 3)");
        assertEqualsLong("lifted-3", 3L, FieldUtils.safeMultiply(3L, 1), "FieldUtils.safeMultiply(3L, 1)");

        assertEqualsLong("lifted-4", 6L, FieldUtils.safeMultiply(2L, 3), "FieldUtils.safeMultiply(2L, 3)");
        assertEqualsLong("lifted-5", -6L, FieldUtils.safeMultiply(2L, -3), "FieldUtils.safeMultiply(2L, -3)");
        assertEqualsLong("lifted-6", -6L, FieldUtils.safeMultiply(-2L, 3), "FieldUtils.safeMultiply(-2L, 3)");
        assertEqualsLong("lifted-7", 6L, FieldUtils.safeMultiply(-2L, -3), "FieldUtils.safeMultiply(-2L, -3)");

        assertEqualsLong("lifted-8", -1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE),
                "FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE)");

        assertEqualsLong("lifted-9", Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1),
                "FieldUtils.safeMultiply(Long.MAX_VALUE, 1)");
        assertEqualsLong("lifted-10", Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1),
                "FieldUtils.safeMultiply(Long.MIN_VALUE, 1)");
        assertEqualsLong("lifted-11", -Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1),
                "FieldUtils.safeMultiply(Long.MAX_VALUE, -1)");

        assertArithmeticException("lifted-12", Long.MIN_VALUE, -1);
        assertArithmeticException("lifted-13", Long.MIN_VALUE, 100);
        assertArithmeticException("lifted-14", Long.MIN_VALUE, Integer.MAX_VALUE);
        assertArithmeticException("lifted-15", Long.MAX_VALUE, Integer.MIN_VALUE);
    }

    private static void checkOverloadAgreementLongIntLongLong(FuzzedDataProvider data) {
        long val1;
        int val2;
        try {
            val1 = (long) data.consumeInt(-1000000, 1000000);
            val2 = data.consumeInt(-1000000, 1000000);
        } catch (Exception e) {
            return;
        }

        long r1;
        try {
            r1 = FieldUtils.safeMultiply(val1, val2);
        } catch (Exception e) {
            return;
        }

        long r2;
        try {
            r2 = FieldUtils.safeMultiply(val1, (long) val2);
        } catch (Exception e) {
            return;
        }

        if (r1 != r2) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:overload-agreement-longInt-longLong] semantic mismatch: safeMultiply(long,int) != safeMultiply(long,long) for equivalent inputs val1="
                            + val1 + " val2=" + val2 + " lhs=" + r1 + " rhs=" + r2);
        }
    }

    private static void checkOverloadAgreementLongIntIntInt(FuzzedDataProvider data) {
        int a;
        int b;
        try {
            a = data.consumeInt(-46340, 46340);
            b = data.consumeInt(-46340, 46340);
        } catch (Exception e) {
            return;
        }

        long r1;
        try {
            r1 = FieldUtils.safeMultiply((long) a, b);
        } catch (Exception e) {
            return;
        }

        int r2;
        try {
            r2 = FieldUtils.safeMultiply(a, b);
        } catch (Exception e) {
            return;
        }

        if (r1 != (long) r2) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:overload-agreement-longInt-intInt] semantic mismatch: safeMultiply(long,int) != widened safeMultiply(int,int) for inputs a="
                            + a + " b=" + b + " lhs=" + r1 + " rhs=" + r2);
        }
    }

    private static void checkMetamorphicNegationIdentity(FuzzedDataProvider data) {
        long x;
        int y;
        try {
            x = (long) data.consumeInt(-1000000, 1000000);
            y = data.consumeInt(-1000, 1000);
        } catch (Exception e) {
            return;
        }

        long product;
        try {
            product = FieldUtils.safeMultiply(x, y);
        } catch (Exception e) {
            return;
        }

        long negatedInputProduct;
        try {
            negatedInputProduct = FieldUtils.safeMultiply(-x, y);
        } catch (Exception e) {
            return;
        }

        long negatedProduct;
        try {
            negatedProduct = FieldUtils.safeNegate(product);
        } catch (Exception e) {
            return;
        }

        /* Contract justification:
         * safeMultiply performs exact multiplication with overflow detection, and safeNegate performs exact negation
         * with overflow detection. For valid-by-construction moderate inputs where all three calls succeed,
         * (-x) * y must equal -(x * y). A patch that merely suppresses the exceptional branch or returns the wrong
         * sign/value on multiplication would violate this observable post-condition without necessarily throwing.
         */
        if (negatedInputProduct != negatedProduct) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:negation-identity] metamorphic violation: safeMultiply(-x, y) must equal safeNegate(safeMultiply(x, y)) for x="
                            + x + " y=" + y + " lhs=" + negatedInputProduct + " rhs=" + negatedProduct);
        }
    }

    private static void assertEqualsLong(String oracleId, long expected, long actual, String expr) {
        if (expected != actual) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: " + expr + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertArithmeticException(String oracleId, long val1, int val2) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: FieldUtils.safeMultiply(" + val1 + ", " + val2
                            + ") was required by the documented contract and lifted test to throw ArithmeticException, but returned "
                            + actual);
        } catch (ArithmeticException expected) {
        }
    }
}
```

</details>

---
## [46] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [47] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~8,213 chars, 2 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [36] — not reprinted*
**[assistant]**
```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedOracles();
        checkOverloadAgreementLongIntLongLong(data);
        checkOverloadAgreementLongIntIntInt(data);
        checkMetamorphicNegationIdentity(data);
    }

    private static void checkLiftedOracles() {
        assertEqualsLong("lifted-0", 0L, FieldUtils.safeMultiply(0L, 0), "FieldUtils.safeMultiply(0L, 0)");
        assertEqualsLong("lifted-1", 1L, FieldUtils.safeMultiply(1L, 1), "FieldUtils.safeMultiply(1L, 1)");
        assertEqualsLong("lifted-2", 3L, FieldUtils.safeMultiply(1L, 3), "FieldUtils.safeMultiply(1L, 3)");
        assertEqualsLong("lifted-3", 3L, FieldUtils.safeMultiply(3L, 1), "FieldUtils.safeMultiply(3L, 1)");

        assertEqualsLong("lifted-4", 6L, FieldUtils.safeMultiply(2L, 3), "FieldUtils.safeMultiply(2L, 3)");
        assertEqualsLong("lifted-5", -6L, FieldUtils.safeMultiply(2L, -3), "FieldUtils.safeMultiply(2L, -3)");
        assertEqualsLong("lifted-6", -6L, FieldUtils.safeMultiply(-2L, 3), "FieldUtils.safeMultiply(-2L, 3)");
        assertEqualsLong("lifted-7", 6L, FieldUtils.safeMultiply(-2L, -3), "FieldUtils.safeMultiply(-2L, -3)");

        assertEqualsLong("lifted-8", -1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE),
                "FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE)");

        assertEqualsLong("lifted-9", Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1),
                "FieldUtils.safeMultiply(Long.MAX_VALUE, 1)");
        assertEqualsLong("lifted-10", Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1),
                "FieldUtils.safeMultiply(Long.MIN_VALUE, 1)");
        assertEqualsLong("lifted-11", -Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1),
                "FieldUtils.safeMultiply(Long.MAX_VALUE, -1)");

        assertArithmeticException("lifted-12", Long.MIN_VALUE, -1);
        assertArithmeticException("lifted-13", Long.MIN_VALUE, 100);
        assertArithmeticException("lifted-14", Long.MIN_VALUE, Integer.MAX_VALUE);
        assertArithmeticException("lifted-15", Long.MAX_VALUE, Integer.MIN_VALUE);
    }

    private static void checkOverloadAgreementLongIntLongLong(FuzzedDataProvider data) {
        long val1;
        int val2;
        try {
            val1 = (long) data.consumeInt(-1000000, 1000000);
            val2 = data.consumeInt(-1000000, 1000000);
        } catch (Exception e) {
            return;
        }

        long r1;
        try {
            r1 = FieldUtils.safeMultiply(val1, val2);
        } catch (Exception e) {
            return;
        }

        long r2;
        try {
            r2 = FieldUtils.safeMultiply(val1, (long) val2);
        } catch (Exception e) {
            return;
        }

        if (r1 != r2) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:overload-agreement-longInt-longLong] semantic mismatch: safeMultiply(long,int) != safeMultiply(long,long) for equivalent inputs val1="
                            + val1 + " val2=" + val2 + " lhs=" + r1 + " rhs=" + r2);
        }
    }

    private static void checkOverloadAgreementLongIntIntInt(FuzzedDataProvider data) {
        int a;
        int b;
        try {
            a = data.consumeInt(-46340, 46340);
            b = data.consumeInt(-46340, 46340);
        } catch (Exception e) {
            return;
        }

        long r1;
        try {
            r1 = FieldUtils.safeMultiply((long) a, b);
        } catch (Exception e) {
            return;
        }

        int r2;
        try {
            r2 = FieldUtils.safeMultiply(a, b);
        } catch (Exception e) {
            return;
        }

        if (r1 != (long) r2) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:overload-agreement-longInt-intInt] semantic mismatch: safeMultiply(long,int) != widened safeMultiply(int,int) for inputs a="
                            + a + " b=" + b + " lhs=" + r1 + " rhs=" + r2);
        }
    }

    private static void checkMetamorphicNegationIdentity(FuzzedDataProvider data) {
        long x;
        int y;
        try {
            x = (long) data.consumeInt(-1000000, 1000000);
            y = data.consumeInt(-1000, 1000);
        } catch (Exception e) {
            return;
        }

        long product;
        try {
            product = FieldUtils.safeMultiply(x, y);
        } catch (Exception e) {
            return;
        }

        long negatedInputProduct;
        try {
            negatedInputProduct = FieldUtils.safeMultiply(-x, y);
        } catch (Exception e) {
            return;
        }

        long negatedProduct;
        try {
            negatedProduct = FieldUtils.safeNegate(product);
        } catch (Exception e) {
            return;
        }

        /* Contract justification:
         * safeMultiply performs exact multiplication with overflow detection, and safeNegate performs exact negation
         * with overflow detection. For valid-by-construction moderate inputs where all three calls succeed,
         * (-x) * y must equal -(x * y). A patch that merely suppresses the exceptional branch or returns the wrong
         * sign/value on multiplication would violate this observable post-condition without necessarily throwing.
         */
        if (negatedInputProduct != negatedProduct) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:negation-identity] metamorphic violation: safeMultiply(-x, y) must equal safeNegate(safeMultiply(x, y)) for x="
                            + x + " y=" + y + " lhs=" + negatedInputProduct + " rhs=" + negatedProduct);
        }
    }

    private static void assertEqualsLong(String oracleId, long expected, long actual, String expr) {
        if (expected != actual) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: " + expr + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertArithmeticException(String oracleId, long val1, int val2) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: FieldUtils.safeMultiply(" + val1 + ", " + val2
                            + ") was required by the documented contract and lifted test to throw ArithmeticException, but returned "
                            + actual);
        } catch (ArithmeticException expected) {
        }
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/004_patch1-Time-15-ACS_c_rep001/Time_15_buggy/fuzz/attempt_005/FuzzHarness.java:129: error: incompatible types: possible lossy conversion from long to int
            negatedProduct = FieldUtils.safeNegate(product);
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
<details open><summary>▸ Output (~6,536 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedOracles();
        checkOverloadAgreementLongIntLongLong(data);
        checkOverloadAgreementLongIntIntInt(data);
        checkMetamorphicSignRelation(data);
    }

    private static void checkLiftedOracles() {
        assertEqualsLong("lifted-0", 0L, FieldUtils.safeMultiply(0L, 0), "FieldUtils.safeMultiply(0L, 0)");
        assertEqualsLong("lifted-1", 1L, FieldUtils.safeMultiply(1L, 1), "FieldUtils.safeMultiply(1L, 1)");
        assertEqualsLong("lifted-2", 3L, FieldUtils.safeMultiply(1L, 3), "FieldUtils.safeMultiply(1L, 3)");
        assertEqualsLong("lifted-3", 3L, FieldUtils.safeMultiply(3L, 1), "FieldUtils.safeMultiply(3L, 1)");
        assertEqualsLong("lifted-4", 6L, FieldUtils.safeMultiply(2L, 3), "FieldUtils.safeMultiply(2L, 3)");
        assertEqualsLong("lifted-5", -6L, FieldUtils.safeMultiply(2L, -3), "FieldUtils.safeMultiply(2L, -3)");
        assertEqualsLong("lifted-6", -6L, FieldUtils.safeMultiply(-2L, 3), "FieldUtils.safeMultiply(-2L, 3)");
        assertEqualsLong("lifted-7", 6L, FieldUtils.safeMultiply(-2L, -3), "FieldUtils.safeMultiply(-2L, -3)");
        assertEqualsLong("lifted-8", -1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE),
                "FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE)");
        assertEqualsLong("lifted-9", Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1),
                "FieldUtils.safeMultiply(Long.MAX_VALUE, 1)");
        assertEqualsLong("lifted-10", Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1),
                "FieldUtils.safeMultiply(Long.MIN_VALUE, 1)");
        assertEqualsLong("lifted-11", -Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1),
                "FieldUtils.safeMultiply(Long.MAX_VALUE, -1)");

        assertArithmeticException("lifted-12", Long.MIN_VALUE, -1);
        assertArithmeticException("lifted-13", Long.MIN_VALUE, 100);
        assertArithmeticException("lifted-14", Long.MIN_VALUE, Integer.MAX_VALUE);
        assertArithmeticException("lifted-15", Long.MAX_VALUE, Integer.MIN_VALUE);
    }

    private static void checkOverloadAgreementLongIntLongLong(FuzzedDataProvider data) {
        long val1;
        int val2;
        try {
            val1 = (long) data.consumeInt(-1000000, 1000000);
            val2 = data.consumeInt(-1000000, 1000000);
        } catch (Exception e) {
            return;
        }

        long r1;
        try {
            r1 = FieldUtils.safeMultiply(val1, val2);
        } catch (Exception e) {
            return;
        }

        long r2;
        try {
            r2 = FieldUtils.safeMultiply(val1, (long) val2);
        } catch (Exception e) {
            return;
        }

        if (r1 != r2) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:overload-agreement-longInt-longLong] semantic mismatch: safeMultiply(long,int) != safeMultiply(long,long) for equivalent inputs val1="
                            + val1 + " val2=" + val2 + " lhs=" + r1 + " rhs=" + r2);
        }
    }

    private static void checkOverloadAgreementLongIntIntInt(FuzzedDataProvider data) {
        int a;
        int b;
        try {
            a = data.consumeInt(-46340, 46340);
            b = data.consumeInt(-46340, 46340);
        } catch (Exception e) {
            return;
        }

        long r1;
        try {
            r1 = FieldUtils.safeMultiply((long) a, b);
        } catch (Exception e) {
            return;
        }

        int r2;
        try {
            r2 = FieldUtils.safeMultiply(a, b);
        } catch (Exception e) {
            return;
        }

        if (r1 != (long) r2) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:overload-agreement-longInt-intInt] semantic mismatch: safeMultiply(long,int) != widened safeMultiply(int,int) for inputs a="
                            + a + " b=" + b + " lhs=" + r1 + " rhs=" + r2);
        }
    }

    private static void checkMetamorphicSignRelation(FuzzedDataProvider data) {
        long x;
        int y;
        try {
            x = (long) data.consumeInt(-1000000, 1000000);
            y = data.consumeInt(-1000, 1000);
        } catch (Exception e) {
            return;
        }

        long lhs;
        try {
            lhs = FieldUtils.safeMultiply(-x, y);
        } catch (Exception e) {
            return;
        }

        long rhs;
        try {
            rhs = FieldUtils.safeMultiply(FieldUtils.safeMultiply(x, y), -1);
        } catch (Exception e) {
            return;
        }

        /* Contract justification:
         * safeMultiply computes the exact mathematical product or throws ArithmeticException on overflow.
         * For moderate valid-by-construction inputs where both sides succeed, (-x) * y must equal (x * y) * (-1).
         * This is an observable post-condition over only real library calls; a patch that suppresses/changes the
         * special-case handling for multiplier -1 or returns a wrong value instead of the exact product breaks it.
         */
        if (lhs != rhs) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:sign-relation] metamorphic violation: safeMultiply(-x, y) must equal safeMultiply(safeMultiply(x, y), -1) for x="
                            + x + " y=" + y + " lhs=" + lhs + " rhs=" + rhs);
        }
    }

    private static void assertEqualsLong(String oracleId, long expected, long actual, String expr) {
        if (expected != actual) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: " + expr + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertArithmeticException(String oracleId, long val1, int val2) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: FieldUtils.safeMultiply(" + val1 + ", " + val2
                            + ") was required by the documented contract and lifted test to throw ArithmeticException, but returned "
                            + actual);
        } catch (ArithmeticException expected) {
        }
    }
}
```

</details>

---
## [48] ⚙️ harness-attempt · `attempt_006`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-12] semantic mismatch: FieldUtils.safeMultiply(-9223372036854775808, -1) was required by the documented contract and lifted test…

---
## [49] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [39] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~10,601 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actualLong;
        boolean threw;

        try {
            actualLong = FieldUtils.safeMultiply(0L, 0);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-0x0] semantic mismatch: expected=0 actual=ArithmeticException");
        }
        if (actualLong != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-0x0] semantic mismatch: expected=0 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(1L, 1);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-1x1] semantic mismatch: expected=1 actual=ArithmeticException");
        }
        if (actualLong != 1L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-1x1] semantic mismatch: expected=1 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(1L, 3);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-1x3] semantic mismatch: expected=3 actual=ArithmeticException");
        }
        if (actualLong != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-1x3] semantic mismatch: expected=3 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(3L, 1);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-3x1] semantic mismatch: expected=3 actual=ArithmeticException");
        }
        if (actualLong != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-3x1] semantic mismatch: expected=3 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(2L, 3);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-2x3] semantic mismatch: expected=6 actual=ArithmeticException");
        }
        if (actualLong != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-2x3] semantic mismatch: expected=6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(2L, -3);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-2xneg3] semantic mismatch: expected=-6 actual=ArithmeticException");
        }
        if (actualLong != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-2xneg3] semantic mismatch: expected=-6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(-2L, 3);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-neg2x3] semantic mismatch: expected=-6 actual=ArithmeticException");
        }
        if (actualLong != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-neg2x3] semantic mismatch: expected=-6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(-2L, -3);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-neg2xneg3] semantic mismatch: expected=6 actual=ArithmeticException");
        }
        if (actualLong != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-neg2xneg3] semantic mismatch: expected=6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-neg1xintmin] semantic mismatch: expected=" + (-1L * Integer.MIN_VALUE) + " actual=ArithmeticException");
        }
        if (actualLong != (-1L * Integer.MIN_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-neg1xintmin] semantic mismatch: expected=" + (-1L * Integer.MIN_VALUE) + " actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-maxx1] semantic mismatch: expected=" + Long.MAX_VALUE + " actual=ArithmeticException");
        }
        if (actualLong != Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-maxx1] semantic mismatch: expected=" + Long.MAX_VALUE + " actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-minx1] semantic mismatch: expected=" + Long.MIN_VALUE + " actual=ArithmeticException");
        }
        if (actualLong != Long.MIN_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-minx1] semantic mismatch: expected=" + Long.MIN_VALUE + " actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-maxxneg1] semantic mismatch: expected=" + (-Long.MAX_VALUE) + " actual=ArithmeticException");
        }
        if (actualLong != -Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-maxxneg1] semantic mismatch: expected=" + (-Long.MAX_VALUE) + " actual=" + actualLong);
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
        } catch (ArithmeticException e) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-minxneg1-throws] semantic mismatch: expected=ArithmeticException actual=" +
                FieldUtils.safeMultiply(Long.MIN_VALUE, -1));
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
        } catch (ArithmeticException e) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-minx100-throws] semantic mismatch: expected=ArithmeticException actual=" +
                FieldUtils.safeMultiply(Long.MIN_VALUE, 100));
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
        } catch (ArithmeticException e) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-minxintmax-throws] semantic mismatch: expected=ArithmeticException actual=" +
                FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE));
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
        } catch (ArithmeticException e) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-maxxintmin-throws] semantic mismatch: expected=ArithmeticException actual=" +
                FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE));
        }

        int fuzzVal1 = data.consumeInt(-1_000_000, 1_000_000);
        int fuzzVal2 = data.consumeInt(-1_000_000, 1_000_000);

        try {
            long lhs = FieldUtils.safeMultiply((long) fuzzVal1, fuzzVal2);
            long rhs = FieldUtils.safeMultiply((long) fuzzVal1, (long) fuzzVal2);
            // Documented guarantee: these are same-name overloads with the same javadoc
            // ("Multiply two values throwing an exception if overflow occurs"), so for the
            // same mathematical inputs and successful completion they must return the same
            // product. A patch that suppresses the special-case overflow throw or returns a
            // wrapped value breaks this observable post-condition even when no exception fires.
            if (lhs != rhs) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) input=(" +
                    fuzzVal1 + "," + fuzzVal2 + ") lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (ArithmeticException e) {
            return;
        }

        int canonical = data.consumeInt(-1_000_000, 1_000_000);
        try {
            long viaOne = FieldUtils.safeMultiply((long) canonical, 1);
            // Documented contract plus the method body special-case say multiplying by 1 returns the input.
            // This directly observes the result after the call; a patch that reroutes the branch incorrectly
            // or changes bookkeeping so the wrong value is returned would violate it without needing a throw.
            if (viaOne != (long) canonical) {
                throw new RuntimeException(
                    "[oracle:identity-by-one] metamorphic violation: safeMultiply(x,1)==x input=" +
                    canonical + " lhs=" + viaOne + " rhs=" + canonical);
            }
        } catch (ArithmeticException e) {
            return;
        }
    }
}
```

</details>

---
## [50] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `threw` (literal assignment, no rethrow, the caught exceptio

---
## [51] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~11,746 chars, 2 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [39] — not reprinted*
**[assistant]**
```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actualLong;
        boolean threw;

        try {
            actualLong = FieldUtils.safeMultiply(0L, 0);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-0x0] semantic mismatch: expected=0 actual=ArithmeticException");
        }
        if (actualLong != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-0x0] semantic mismatch: expected=0 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(1L, 1);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-1x1] semantic mismatch: expected=1 actual=ArithmeticException");
        }
        if (actualLong != 1L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-1x1] semantic mismatch: expected=1 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(1L, 3);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-1x3] semantic mismatch: expected=3 actual=ArithmeticException");
        }
        if (actualLong != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-1x3] semantic mismatch: expected=3 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(3L, 1);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-3x1] semantic mismatch: expected=3 actual=ArithmeticException");
        }
        if (actualLong != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-3x1] semantic mismatch: expected=3 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(2L, 3);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-2x3] semantic mismatch: expected=6 actual=ArithmeticException");
        }
        if (actualLong != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-2x3] semantic mismatch: expected=6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(2L, -3);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-2xneg3] semantic mismatch: expected=-6 actual=ArithmeticException");
        }
        if (actualLong != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-2xneg3] semantic mismatch: expected=-6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(-2L, 3);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-neg2x3] semantic mismatch: expected=-6 actual=ArithmeticException");
        }
        if (actualLong != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-neg2x3] semantic mismatch: expected=-6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(-2L, -3);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-neg2xneg3] semantic mismatch: expected=6 actual=ArithmeticException");
        }
        if (actualLong != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-neg2xneg3] semantic mismatch: expected=6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-neg1xintmin] semantic mismatch: expected=" + (-1L * Integer.MIN_VALUE) + " actual=ArithmeticException");
        }
        if (actualLong != (-1L * Integer.MIN_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-neg1xintmin] semantic mismatch: expected=" + (-1L * Integer.MIN_VALUE) + " actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-maxx1] semantic mismatch: expected=" + Long.MAX_VALUE + " actual=ArithmeticException");
        }
        if (actualLong != Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-maxx1] semantic mismatch: expected=" + Long.MAX_VALUE + " actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-minx1] semantic mismatch: expected=" + Long.MIN_VALUE + " actual=ArithmeticException");
        }
        if (actualLong != Long.MIN_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-minx1] semantic mismatch: expected=" + Long.MIN_VALUE + " actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-maxxneg1] semantic mismatch: expected=" + (-Long.MAX_VALUE) + " actual=ArithmeticException");
        }
        if (actualLong != -Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-maxxneg1] semantic mismatch: expected=" + (-Long.MAX_VALUE) + " actual=" + actualLong);
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
        } catch (ArithmeticException e) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-minxneg1-throws] semantic mismatch: expected=ArithmeticException actual=" +
                FieldUtils.safeMultiply(Long.MIN_VALUE, -1));
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
        } catch (ArithmeticException e) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-minx100-throws] semantic mismatch: expected=ArithmeticException actual=" +
                FieldUtils.safeMultiply(Long.MIN_VALUE, 100));
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
        } catch (ArithmeticException e) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-minxintmax-throws] semantic mismatch: expected=ArithmeticException actual=" +
                FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE));
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
        } catch (ArithmeticException e) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-maxxintmin-throws] semantic mismatch: expected=ArithmeticException actual=" +
                FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE));
        }

        int fuzzVal1 = data.consumeInt(-1_000_000, 1_000_000);
        int fuzzVal2 = data.consumeInt(-1_000_000, 1_000_000);

        try {
            long lhs = FieldUtils.safeMultiply((long) fuzzVal1, fuzzVal2);
            long rhs = FieldUtils.safeMultiply((long) fuzzVal1, (long) fuzzVal2);
            // Documented guarantee: these are same-name overloads with the same javadoc
            // ("Multiply two values throwing an exception if overflow occurs"), so for the
            // same mathematical inputs and successful completion they must return the same
            // product. A patch that suppresses the special-case overflow throw or returns a
            // wrapped value breaks this observable post-condition even when no exception fires.
            if (lhs != rhs) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) input=(" +
                    fuzzVal1 + "," + fuzzVal2 + ") lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (ArithmeticException e) {
            return;
        }

        int canonical = data.consumeInt(-1_000_000, 1_000_000);
        try {
            long viaOne = FieldUtils.safeMultiply((long) canonical, 1);
            // Documented contract plus the method body special-case say multiplying by 1 returns the input.
            // This directly observes the result after the call; a patch that reroutes the branch incorrectly
            // or changes bookkeeping so the wrong value is returned would violate it without needing a throw.
            if (viaOne != (long) canonical) {
                throw new RuntimeException(
                    "[oracle:identity-by-one] metamorphic violation: safeMultiply(x,1)==x input=" +
                    canonical + " lhs=" + viaOne + " rhs=" + canonical);
            }
        } catch (ArithmeticException e) {
            return;
        }
    }
}
```
**[user]**
```
Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `threw` (literal assignment, no rethrow, the caught exception is never used) and the alarm near line 149 is conditioned on `threw` — the caught exception's identity is destroyed, so attribution cannot tell WHICH exception fired or whether the unpatched build crashes the same way. Rethrow the exception as the alarm's cause (`..., e`), or assert on the real observable instead of a swallowed boolean.
A caught exception is a REJECTION, never a violation, and flattening it into a flag destroys the identity attribution needs. Either assert on the real observable the test pins (compute the actual value and compare it), or if you must re-report a caught crash, re-throw it with the original as the cause: `new FuzzerSecurityIssueLow("[oracle:<id>] ...", e)`. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~10,125 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actualLong;

        try {
            actualLong = FieldUtils.safeMultiply(0L, 0);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-0x0] semantic mismatch: expected=0 actual=ArithmeticException", e);
        }
        if (actualLong != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-0x0] semantic mismatch: expected=0 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(1L, 1);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-1x1] semantic mismatch: expected=1 actual=ArithmeticException", e);
        }
        if (actualLong != 1L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-1x1] semantic mismatch: expected=1 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(1L, 3);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-1x3] semantic mismatch: expected=3 actual=ArithmeticException", e);
        }
        if (actualLong != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-1x3] semantic mismatch: expected=3 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(3L, 1);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-3x1] semantic mismatch: expected=3 actual=ArithmeticException", e);
        }
        if (actualLong != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-3x1] semantic mismatch: expected=3 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(2L, 3);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-2x3] semantic mismatch: expected=6 actual=ArithmeticException", e);
        }
        if (actualLong != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-2x3] semantic mismatch: expected=6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(2L, -3);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-2xneg3] semantic mismatch: expected=-6 actual=ArithmeticException", e);
        }
        if (actualLong != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-2xneg3] semantic mismatch: expected=-6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(-2L, 3);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-neg2x3] semantic mismatch: expected=-6 actual=ArithmeticException", e);
        }
        if (actualLong != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-neg2x3] semantic mismatch: expected=-6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(-2L, -3);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-neg2xneg3] semantic mismatch: expected=6 actual=ArithmeticException", e);
        }
        if (actualLong != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-neg2xneg3] semantic mismatch: expected=6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-neg1xintmin] semantic mismatch: expected=" + (-1L * Integer.MIN_VALUE) + " actual=ArithmeticException", e);
        }
        if (actualLong != (-1L * Integer.MIN_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-neg1xintmin] semantic mismatch: expected=" + (-1L * Integer.MIN_VALUE) + " actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-maxx1] semantic mismatch: expected=" + Long.MAX_VALUE + " actual=ArithmeticException", e);
        }
        if (actualLong != Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-maxx1] semantic mismatch: expected=" + Long.MAX_VALUE + " actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-minx1] semantic mismatch: expected=" + Long.MIN_VALUE + " actual=ArithmeticException", e);
        }
        if (actualLong != Long.MIN_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-minx1] semantic mismatch: expected=" + Long.MIN_VALUE + " actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-maxxneg1] semantic mismatch: expected=" + (-Long.MAX_VALUE) + " actual=ArithmeticException", e);
        }
        if (actualLong != -Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-maxxneg1] semantic mismatch: expected=" + (-Long.MAX_VALUE) + " actual=" + actualLong);
        }

        try {
            long unexpected = FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-minxneg1-throws] semantic mismatch: expected=ArithmeticException actual=" + unexpected);
        } catch (ArithmeticException expected) {
        }

        try {
            long unexpected = FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-minx100-throws] semantic mismatch: expected=ArithmeticException actual=" + unexpected);
        } catch (ArithmeticException expected) {
        }

        try {
            long unexpected = FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-minxintmax-throws] semantic mismatch: expected=ArithmeticException actual=" + unexpected);
        } catch (ArithmeticException expected) {
        }

        try {
            long unexpected = FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-maxxintmin-throws] semantic mismatch: expected=ArithmeticException actual=" + unexpected);
        } catch (ArithmeticException expected) {
        }

        int fuzzVal1 = data.consumeInt(-1_000_000, 1_000_000);
        int fuzzVal2 = data.consumeInt(-1_000_000, 1_000_000);

        try {
            long lhs = FieldUtils.safeMultiply((long) fuzzVal1, fuzzVal2);
            long rhs = FieldUtils.safeMultiply((long) fuzzVal1, (long) fuzzVal2);
            // Same-name overloads share the same documented contract ("Multiply two values throwing
            // an exception if overflow occurs"), so on the same mathematical inputs and successful
            // completion they must return the same product. A patch that removes the exceptional
            // behavior on a special case and instead returns a wrapped value violates this relation.
            if (lhs != rhs) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) input=(" +
                    fuzzVal1 + "," + fuzzVal2 + ") lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (ArithmeticException e) {
            return;
        }

        int canonical = data.consumeInt(-1_000_000, 1_000_000);
        try {
            long viaOne = FieldUtils.safeMultiply((long) canonical, 1);
            // The documented contract for multiplication plus the visible special-case branch for
            // multiplier 1 guarantee that multiplying by 1 preserves the input exactly. This is an
            // observable post-condition on the returned value, so a throw-deleting/wrong-value patch
            // is caught even if no exception fires.
            if (viaOne != (long) canonical) {
                throw new RuntimeException(
                    "[oracle:identity-by-one] metamorphic violation: safeMultiply(x,1)==x input=" +
                    canonical + " lhs=" + viaOne + " rhs=" + canonical);
            }
        } catch (ArithmeticException e) {
            return;
        }
    }
}
```

</details>

---
## [52] ⚙️ harness-attempt · `attempt_008`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:seed-minxneg1-throws] semantic mismatch: expected=ArithmeticException actual=-9223372036854775808

---
## [53] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [36] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,376 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedOracles();

        relationOverloadAgreementLongIntLongLongSmallInputs(data);
        relationOverloadAgreementLongIntIntIntSmallInputs(data);
        relationSafeMultiplyByMinusOneAgreesWithSafeNegate(data);

        exercisePatchedBranch(data);
    }

    private static void checkLiftedOracles() {
        assertEqualsLong("[oracle:lifted-0x0]", 0L, FieldUtils.safeMultiply(0L, 0));
        assertEqualsLong("[oracle:lifted-1x1]", 1L, FieldUtils.safeMultiply(1L, 1));
        assertEqualsLong("[oracle:lifted-1x3]", 3L, FieldUtils.safeMultiply(1L, 3));
        assertEqualsLong("[oracle:lifted-3x1]", 3L, FieldUtils.safeMultiply(3L, 1));

        assertEqualsLong("[oracle:lifted-2x3]", 6L, FieldUtils.safeMultiply(2L, 3));
        assertEqualsLong("[oracle:lifted-2x-3]", -6L, FieldUtils.safeMultiply(2L, -3));
        assertEqualsLong("[oracle:lifted--2x3]", -6L, FieldUtils.safeMultiply(-2L, 3));
        assertEqualsLong("[oracle:lifted--2x-3]", 6L, FieldUtils.safeMultiply(-2L, -3));

        assertEqualsLong("[oracle:lifted--1xintmin]", -1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE));

        assertEqualsLong("[oracle:lifted-longmaxx1]", Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1));
        assertEqualsLong("[oracle:lifted-longminx1]", Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1));
        assertEqualsLong("[oracle:lifted-longmaxx-1]", -Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1));

        assertThrowsArithmetic("[oracle:lifted-longminx-1]", Long.MIN_VALUE, -1);
        assertThrowsArithmetic("[oracle:lifted-longminx100]", Long.MIN_VALUE, 100);
        assertThrowsArithmetic("[oracle:lifted-longminxintmax]", Long.MIN_VALUE, Integer.MAX_VALUE);
        assertThrowsArithmetic("[oracle:lifted-longmaxxintmin]", Long.MAX_VALUE, Integer.MIN_VALUE);
    }

    private static void relationOverloadAgreementLongIntLongLongSmallInputs(FuzzedDataProvider data) {
        long val1;
        int val2;
        try {
            val1 = (long) data.consumeInt(-1000000, 1000000);
            val2 = data.consumeInt(-1000000, 1000000);
        } catch (Exception e) {
            return;
        }

        long r1;
        try {
            r1 = FieldUtils.safeMultiply(val1, val2);
        } catch (Exception e) {
            return;
        }

        long r2;
        try {
            r2 = FieldUtils.safeMultiply(val1, (long) val2);
        } catch (Exception e) {
            return;
        }

        if (r1 != r2) {
            throw new FuzzerSecurityIssueLow("relation overload_agreement_longInt_longLong_small_inputs violated: input=" + val1 + "," + val2 + " lhs=" + r1 + " rhs=" + r2);
        }
    }

    private static void relationOverloadAgreementLongIntIntIntSmallInputs(FuzzedDataProvider data) {
        int a;
        int b;
        try {
            a = data.consumeInt(-46340, 46340);
            b = data.consumeInt(-46340, 46340);
        } catch (Exception e) {
            return;
        }

        long r1;
        try {
            r1 = FieldUtils.safeMultiply((long) a, b);
        } catch (Exception e) {
            return;
        }

        int r2;
        try {
            r2 = FieldUtils.safeMultiply(a, b);
        } catch (Exception e) {
            return;
        }

        if (r1 != (long) r2) {
            throw new FuzzerSecurityIssueLow("relation overload_agreement_longInt_intInt_small_inputs violated: input=" + a + "," + b + " lhs=" + r1 + " rhs=" + r2);
        }
    }

    private static void relationSafeMultiplyByMinusOneAgreesWithSafeNegate(FuzzedDataProvider data) {
        long val;
        try {
            if (data.consumeBoolean()) {
                val = (long) data.consumeInt(-1000000, 1000000);
            } else {
                val = Long.MIN_VALUE;
            }
        } catch (Exception e) {
            return;
        }

        Long mul = null;
        Throwable mulThrown = null;
        try {
            mul = Long.valueOf(FieldUtils.safeMultiply(val, -1));
        } catch (Throwable t) {
            mulThrown = t;
        }

        Long neg = null;
        Throwable negThrown = null;
        try {
            neg = Long.valueOf(FieldUtils.safeNegate(val));
        } catch (Throwable t) {
            negThrown = t;
        }

        if (mulThrown != null || negThrown != null) {
            if (mulThrown != null && negThrown != null &&
                mulThrown instanceof ArithmeticException && negThrown instanceof ArithmeticException) {
                return;
            }
            return;
        }

        /* Contract guarantee asserted: both safeMultiply(value, -1) and safeNegate(value)
           are members of the documented safe* family performing checked negation; for any
           correct implementation on inputs where neither rejects, they must produce the same
           observable long result. A patch that merely removes or bypasses the throw at
           val=Long.MIN_VALUE,-1, or silently returns a wrong negated value, violates this. */
        if (mul.longValue() != neg.longValue()) {
            throw new FuzzerSecurityIssueLow("relation safeMultiply_minusOne_agrees_with_safeNegate violated: input=" + val + " lhs=" + mul + " rhs=" + neg);
        }
    }

    private static void exercisePatchedBranch(FuzzedDataProvider data) {
        long val1;
        int val2;
        try {
            boolean chooseSentinel = data.consumeBoolean();
            if (chooseSentinel) {
                val1 = Long.MIN_VALUE;
                val2 = -1;
            } else {
                int hi = data.consumeInt();
                int lo = data.consumeInt();
                val1 = (((long) hi) << 32) ^ (((long) lo) & 0xffffffffL);
                int selector = data.consumeInt(0, 4);
                if (selector == 0) {
                    val2 = -1;
                } else if (selector == 1) {
                    val2 = 0;
                } else if (selector == 2) {
                    val2 = 1;
                } else {
                    val2 = data.consumeInt();
                }
            }
        } catch (Exception e) {
            return;
        }

        try {
            FieldUtils.safeMultiply(val1, val2);
        } catch (ArithmeticException expected) {
            return;
        } catch (Throwable t) {
            return;
        }
    }

    private static void assertEqualsLong(String oracleId, long expected, long actual) {
        if (expected != actual) {
            throw new FuzzerSecurityIssueLow(oracleId + " semantic mismatch: expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertThrowsArithmetic(String oracleId, long val1, int val2) {
        boolean threw = false;
        try {
            FieldUtils.safeMultiply(val1, val2);
        } catch (ArithmeticException e) {
            threw = true;
        }
        if (!threw) {
            throw new FuzzerSecurityIssueLow(oracleId + " semantic mismatch: expected ArithmeticException for input=" + val1 + "," + val2);
        }
    }
}
```

</details>

---
## [54] ⚙️ harness-repair · `attempt_009`
**output:** **REPAIRED: missing-alarm-id**
- detail: {'applied': ['missing-alarm-id'], 'still_failing': ['boolean-swallow']}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [55] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `threw` (literal assignment, no rethrow, the caught exceptio

---
## [56] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~8,521 chars, 2 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [36] — not reprinted*
**[assistant]**
```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedOracles();

        relationOverloadAgreementLongIntLongLongSmallInputs(data);
        relationOverloadAgreementLongIntIntIntSmallInputs(data);
        relationSafeMultiplyByMinusOneAgreesWithSafeNegate(data);

        exercisePatchedBranch(data);
    }

    private static void checkLiftedOracles() {
        assertEqualsLong("[oracle:lifted-0x0]", 0L, FieldUtils.safeMultiply(0L, 0));
        assertEqualsLong("[oracle:lifted-1x1]", 1L, FieldUtils.safeMultiply(1L, 1));
        assertEqualsLong("[oracle:lifted-1x3]", 3L, FieldUtils.safeMultiply(1L, 3));
        assertEqualsLong("[oracle:lifted-3x1]", 3L, FieldUtils.safeMultiply(3L, 1));

        assertEqualsLong("[oracle:lifted-2x3]", 6L, FieldUtils.safeMultiply(2L, 3));
        assertEqualsLong("[oracle:lifted-2x-3]", -6L, FieldUtils.safeMultiply(2L, -3));
        assertEqualsLong("[oracle:lifted--2x3]", -6L, FieldUtils.safeMultiply(-2L, 3));
        assertEqualsLong("[oracle:lifted--2x-3]", 6L, FieldUtils.safeMultiply(-2L, -3));

        assertEqualsLong("[oracle:lifted--1xintmin]", -1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE));

        assertEqualsLong("[oracle:lifted-longmaxx1]", Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1));
        assertEqualsLong("[oracle:lifted-longminx1]", Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1));
        assertEqualsLong("[oracle:lifted-longmaxx-1]", -Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1));

        assertThrowsArithmetic("[oracle:lifted-longminx-1]", Long.MIN_VALUE, -1);
        assertThrowsArithmetic("[oracle:lifted-longminx100]", Long.MIN_VALUE, 100);
        assertThrowsArithmetic("[oracle:lifted-longminxintmax]", Long.MIN_VALUE, Integer.MAX_VALUE);
        assertThrowsArithmetic("[oracle:lifted-longmaxxintmin]", Long.MAX_VALUE, Integer.MIN_VALUE);
    }

    private static void relationOverloadAgreementLongIntLongLongSmallInputs(FuzzedDataProvider data) {
        long val1;
        int val2;
        try {
            val1 = (long) data.consumeInt(-1000000, 1000000);
            val2 = data.consumeInt(-1000000, 1000000);
        } catch (Exception e) {
            return;
        }

        long r1;
        try {
            r1 = FieldUtils.safeMultiply(val1, val2);
        } catch (Exception e) {
            return;
        }

        long r2;
        try {
            r2 = FieldUtils.safeMultiply(val1, (long) val2);
        } catch (Exception e) {
            return;
        }

        if (r1 != r2) {
            throw new FuzzerSecurityIssueLow("relation overload_agreement_longInt_longLong_small_inputs violated: input=" + val1 + "," + val2 + " lhs=" + r1 + " rhs=" + r2);
        }
    }

    private static void relationOverloadAgreementLongIntIntIntSmallInputs(FuzzedDataProvider data) {
        int a;
        int b;
        try {
            a = data.consumeInt(-46340, 46340);
            b = data.consumeInt(-46340, 46340);
        } catch (Exception e) {
            return;
        }

        long r1;
        try {
            r1 = FieldUtils.safeMultiply((long) a, b);
        } catch (Exception e) {
            return;
        }

        int r2;
        try {
            r2 = FieldUtils.safeMultiply(a, b);
        } catch (Exception e) {
            return;
        }

        if (r1 != (long) r2) {
            throw new FuzzerSecurityIssueLow("relation overload_agreement_longInt_intInt_small_inputs violated: input=" + a + "," + b + " lhs=" + r1 + " rhs=" + r2);
        }
    }

    private static void relationSafeMultiplyByMinusOneAgreesWithSafeNegate(FuzzedDataProvider data) {
        long val;
        try {
            if (data.consumeBoolean()) {
                val = (long) data.consumeInt(-1000000, 1000000);
            } else {
                val = Long.MIN_VALUE;
            }
        } catch (Exception e) {
            return;
        }

        Long mul = null;
        Throwable mulThrown = null;
        try {
            mul = Long.valueOf(FieldUtils.safeMultiply(val, -1));
        } catch (Throwable t) {
            mulThrown = t;
        }

        Long neg = null;
        Throwable negThrown = null;
        try {
            neg = Long.valueOf(FieldUtils.safeNegate(val));
        } catch (Throwable t) {
            negThrown = t;
        }

        if (mulThrown != null || negThrown != null) {
            if (mulThrown != null && negThrown != null &&
                mulThrown instanceof ArithmeticException && negThrown instanceof ArithmeticException) {
                return;
            }
            return;
        }

        /* Contract guarantee asserted: both safeMultiply(value, -1) and safeNegate(value)
           are members of the documented safe* family performing checked negation; for any
           correct implementation on inputs where neither rejects, they must produce the same
           observable long result. A patch that merely removes or bypasses the throw at
           val=Long.MIN_VALUE,-1, or silently returns a wrong negated value, violates this. */
        if (mul.longValue() != neg.longValue()) {
            throw new FuzzerSecurityIssueLow("relation safeMultiply_minusOne_agrees_with_safeNegate violated: input=" + val + " lhs=" + mul + " rhs=" + neg);
        }
    }

    private static void exercisePatchedBranch(FuzzedDataProvider data) {
        long val1;
        int val2;
        try {
            boolean chooseSentinel = data.consumeBoolean();
            if (chooseSentinel) {
                val1 = Long.MIN_VALUE;
                val2 = -1;
            } else {
                int hi = data.consumeInt();
                int lo = data.consumeInt();
                val1 = (((long) hi) << 32) ^ (((long) lo) & 0xffffffffL);
                int selector = data.consumeInt(0, 4);
                if (selector == 0) {
                    val2 = -1;
                } else if (selector == 1) {
                    val2 = 0;
                } else if (selector == 2) {
                    val2 = 1;
                } else {
                    val2 = data.consumeInt();
                }
            }
        } catch (Exception e) {
            return;
        }

        try {
            FieldUtils.safeMultiply(val1, val2);
        } catch (ArithmeticException expected) {
            return;
        } catch (Throwable t) {
            return;
        }
    }

    private static void assertEqualsLong(String oracleId, long expected, long actual) {
        if (expected != actual) {
            throw new FuzzerSecurityIssueLow(oracleId + " semantic mismatch: expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertThrowsArithmetic(String oracleId, long val1, int val2) {
        boolean threw = false;
        try {
            FieldUtils.safeMultiply(val1, val2);
        } catch (ArithmeticException e) {
            threw = true;
        }
        if (!threw) {
            throw new FuzzerSecurityIssueLow(oracleId + " semantic mismatch: expected ArithmeticException for input=" + val1 + "," + val2);
        }
    }
}
```
**[user]**
```
Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `threw` (literal assignment, no rethrow, the caught exception is never used) and the alarm near line 190 is conditioned on `threw` — the caught exception's identity is destroyed, so attribution cannot tell WHICH exception fired or whether the unpatched build crashes the same way. Rethrow the exception as the alarm's cause (`..., e`), or assert on the real observable instead of a swallowed boolean.
A caught exception is a REJECTION, never a violation, and flattening it into a flag destroys the identity attribution needs. Either assert on the real observable the test pins (compute the actual value and compare it), or if you must re-report a caught crash, re-throw it with the original as the cause: `new FuzzerSecurityIssueLow("[oracle:<id>] ...", e)`. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~7,303 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedOracles();

        relationOverloadAgreementLongIntLongLongSmallInputs(data);
        relationOverloadAgreementLongIntIntIntSmallInputs(data);
        relationSafeMultiplyByMinusOneAgreesWithSafeNegate(data);

        exercisePatchedBranch(data);
    }

    private static void checkLiftedOracles() {
        assertEqualsLong("[oracle:lifted-0x0]", 0L, FieldUtils.safeMultiply(0L, 0));
        assertEqualsLong("[oracle:lifted-1x1]", 1L, FieldUtils.safeMultiply(1L, 1));
        assertEqualsLong("[oracle:lifted-1x3]", 3L, FieldUtils.safeMultiply(1L, 3));
        assertEqualsLong("[oracle:lifted-3x1]", 3L, FieldUtils.safeMultiply(3L, 1));

        assertEqualsLong("[oracle:lifted-2x3]", 6L, FieldUtils.safeMultiply(2L, 3));
        assertEqualsLong("[oracle:lifted-2x-3]", -6L, FieldUtils.safeMultiply(2L, -3));
        assertEqualsLong("[oracle:lifted--2x3]", -6L, FieldUtils.safeMultiply(-2L, 3));
        assertEqualsLong("[oracle:lifted--2x-3]", 6L, FieldUtils.safeMultiply(-2L, -3));

        assertEqualsLong("[oracle:lifted--1xintmin]", -1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE));

        assertEqualsLong("[oracle:lifted-longmaxx1]", Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1));
        assertEqualsLong("[oracle:lifted-longminx1]", Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1));
        assertEqualsLong("[oracle:lifted-longmaxx-1]", -Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1));

        assertThrowsArithmetic("[oracle:lifted-longminx-1]", Long.MIN_VALUE, -1);
        assertThrowsArithmetic("[oracle:lifted-longminx100]", Long.MIN_VALUE, 100);
        assertThrowsArithmetic("[oracle:lifted-longminxintmax]", Long.MIN_VALUE, Integer.MAX_VALUE);
        assertThrowsArithmetic("[oracle:lifted-longmaxxintmin]", Long.MAX_VALUE, Integer.MIN_VALUE);
    }

    private static void relationOverloadAgreementLongIntLongLongSmallInputs(FuzzedDataProvider data) {
        long val1;
        int val2;
        try {
            val1 = (long) data.consumeInt(-1000000, 1000000);
            val2 = data.consumeInt(-1000000, 1000000);
        } catch (Exception e) {
            return;
        }

        long r1;
        try {
            r1 = FieldUtils.safeMultiply(val1, val2);
        } catch (Exception e) {
            return;
        }

        long r2;
        try {
            r2 = FieldUtils.safeMultiply(val1, (long) val2);
        } catch (Exception e) {
            return;
        }

        if (r1 != r2) {
            throw new FuzzerSecurityIssueLow("relation overload_agreement_longInt_longLong_small_inputs violated: input=" + val1 + "," + val2 + " lhs=" + r1 + " rhs=" + r2);
        }
    }

    private static void relationOverloadAgreementLongIntIntIntSmallInputs(FuzzedDataProvider data) {
        int a;
        int b;
        try {
            a = data.consumeInt(-46340, 46340);
            b = data.consumeInt(-46340, 46340);
        } catch (Exception e) {
            return;
        }

        long r1;
        try {
            r1 = FieldUtils.safeMultiply((long) a, b);
        } catch (Exception e) {
            return;
        }

        int r2;
        try {
            r2 = FieldUtils.safeMultiply(a, b);
        } catch (Exception e) {
            return;
        }

        if (r1 != (long) r2) {
            throw new FuzzerSecurityIssueLow("relation overload_agreement_longInt_intInt_small_inputs violated: input=" + a + "," + b + " lhs=" + r1 + " rhs=" + r2);
        }
    }

    private static void relationSafeMultiplyByMinusOneAgreesWithSafeNegate(FuzzedDataProvider data) {
        long val;
        try {
            if (data.consumeBoolean()) {
                val = (long) data.consumeInt(-1000000, 1000000);
            } else {
                val = Long.MIN_VALUE;
            }
        } catch (Exception e) {
            return;
        }

        Long mul = null;
        Throwable mulThrown = null;
        try {
            mul = Long.valueOf(FieldUtils.safeMultiply(val, -1));
        } catch (Throwable t) {
            mulThrown = t;
        }

        Long neg = null;
        Throwable negThrown = null;
        try {
            neg = Long.valueOf(FieldUtils.safeNegate(val));
        } catch (Throwable t) {
            negThrown = t;
        }

        if (mulThrown != null || negThrown != null) {
            if (mulThrown != null && negThrown != null &&
                mulThrown instanceof ArithmeticException && negThrown instanceof ArithmeticException) {
                return;
            }
            return;
        }

        /* Contract guarantee asserted: both safeMultiply(value, -1) and safeNegate(value)
           are members of the documented safe* family performing checked negation; for any
           correct implementation on inputs where neither rejects, they must produce the same
           observable long result. A patch that merely removes or bypasses the throw at
           val=Long.MIN_VALUE,-1, or silently returns a wrong negated value, violates this. */
        if (mul.longValue() != neg.longValue()) {
            throw new FuzzerSecurityIssueLow("relation safeMultiply_minusOne_agrees_with_safeNegate violated: input=" + val + " lhs=" + mul + " rhs=" + neg);
        }
    }

    private static void exercisePatchedBranch(FuzzedDataProvider data) {
        long val1;
        int val2;
        try {
            boolean chooseSentinel = data.consumeBoolean();
            if (chooseSentinel) {
                val1 = Long.MIN_VALUE;
                val2 = -1;
            } else {
                int hi = data.consumeInt();
                int lo = data.consumeInt();
                val1 = (((long) hi) << 32) ^ (((long) lo) & 0xffffffffL);
                int selector = data.consumeInt(0, 4);
                if (selector == 0) {
                    val2 = -1;
                } else if (selector == 1) {
                    val2 = 0;
                } else if (selector == 2) {
                    val2 = 1;
                } else {
                    val2 = data.consumeInt();
                }
            }
        } catch (Exception e) {
            return;
        }

        try {
            FieldUtils.safeMultiply(val1, val2);
        } catch (ArithmeticException expected) {
            return;
        } catch (Throwable t) {
            return;
        }
    }

    private static void assertEqualsLong(String oracleId, long expected, long actual) {
        if (expected != actual) {
            throw new FuzzerSecurityIssueLow(oracleId + " semantic mismatch: expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertThrowsArithmetic(String oracleId, long val1, int val2) {
        try {
            FieldUtils.safeMultiply(val1, val2);
        } catch (ArithmeticException e) {
            return;
        }
        throw new FuzzerSecurityIssueLow(oracleId + " semantic mismatch: expected ArithmeticException for input=" + val1 + "," + val2);
    }
}
```

</details>

---
## [57] ⚙️ harness-repair · `attempt_010`
**output:** **REPAIRED: missing-alarm-id**
- detail: {'applied': ['missing-alarm-id'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [58] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [59] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~8,855 chars, 2 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [36] — not reprinted*
- *[assistant] message: identical to step [56] — not reprinted*
- *[user] message: identical to step [56] — not reprinted*
**[assistant]**
```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedOracles();

        relationOverloadAgreementLongIntLongLongSmallInputs(data);
        relationOverloadAgreementLongIntIntIntSmallInputs(data);
        relationSafeMultiplyByMinusOneAgreesWithSafeNegate(data);

        exercisePatchedBranch(data);
    }

    private static void checkLiftedOracles() {
        assertEqualsLong("[oracle:lifted-0x0]", 0L, FieldUtils.safeMultiply(0L, 0));
        assertEqualsLong("[oracle:lifted-1x1]", 1L, FieldUtils.safeMultiply(1L, 1));
        assertEqualsLong("[oracle:lifted-1x3]", 3L, FieldUtils.safeMultiply(1L, 3));
        assertEqualsLong("[oracle:lifted-3x1]", 3L, FieldUtils.safeMultiply(3L, 1));

        assertEqualsLong("[oracle:lifted-2x3]", 6L, FieldUtils.safeMultiply(2L, 3));
        assertEqualsLong("[oracle:lifted-2x-3]", -6L, FieldUtils.safeMultiply(2L, -3));
        assertEqualsLong("[oracle:lifted--2x3]", -6L, FieldUtils.safeMultiply(-2L, 3));
        assertEqualsLong("[oracle:lifted--2x-3]", 6L, FieldUtils.safeMultiply(-2L, -3));

        assertEqualsLong("[oracle:lifted--1xintmin]", -1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE));

        assertEqualsLong("[oracle:lifted-longmaxx1]", Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1));
        assertEqualsLong("[oracle:lifted-longminx1]", Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1));
        assertEqualsLong("[oracle:lifted-longmaxx-1]", -Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1));

        assertThrowsArithmetic("[oracle:lifted-longminx-1]", Long.MIN_VALUE, -1);
        assertThrowsArithmetic("[oracle:lifted-longminx100]", Long.MIN_VALUE, 100);
        assertThrowsArithmetic("[oracle:lifted-longminxintmax]", Long.MIN_VALUE, Integer.MAX_VALUE);
        assertThrowsArithmetic("[oracle:lifted-longmaxxintmin]", Long.MAX_VALUE, Integer.MIN_VALUE);
    }

    private static void relationOverloadAgreementLongIntLongLongSmallInputs(FuzzedDataProvider data) {
        long val1;
        int val2;
        try {
            val1 = (long) data.consumeInt(-1000000, 1000000);
            val2 = data.consumeInt(-1000000, 1000000);
        } catch (Exception e) {
            return;
        }

        long r1;
        try {
            r1 = FieldUtils.safeMultiply(val1, val2);
        } catch (Exception e) {
            return;
        }

        long r2;
        try {
            r2 = FieldUtils.safeMultiply(val1, (long) val2);
        } catch (Exception e) {
            return;
        }

        if (r1 != r2) {
            throw new FuzzerSecurityIssueLow("relation overload_agreement_longInt_longLong_small_inputs violated: input=" + val1 + "," + val2 + " lhs=" + r1 + " rhs=" + r2);
        }
    }

    private static void relationOverloadAgreementLongIntIntIntSmallInputs(FuzzedDataProvider data) {
        int a;
        int b;
        try {
            a = data.consumeInt(-46340, 46340);
            b = data.consumeInt(-46340, 46340);
        } catch (Exception e) {
            return;
        }

        long r1;
        try {
            r1 = FieldUtils.safeMultiply((long) a, b);
        } catch (Exception e) {
            return;
        }

        int r2;
        try {
            r2 = FieldUtils.safeMultiply(a, b);
        } catch (Exception e) {
            return;
        }

        if (r1 != (long) r2) {
            throw new FuzzerSecurityIssueLow("relation overload_agreement_longInt_intInt_small_inputs violated: input=" + a + "," + b + " lhs=" + r1 + " rhs=" + r2);
        }
    }

    private static void relationSafeMultiplyByMinusOneAgreesWithSafeNegate(FuzzedDataProvider data) {
        long val;
        try {
            if (data.consumeBoolean()) {
                val = (long) data.consumeInt(-1000000, 1000000);
            } else {
                val = Long.MIN_VALUE;
            }
        } catch (Exception e) {
            return;
        }

        Long mul = null;
        Throwable mulThrown = null;
        try {
            mul = Long.valueOf(FieldUtils.safeMultiply(val, -1));
        } catch (Throwable t) {
            mulThrown = t;
        }

        Long neg = null;
        Throwable negThrown = null;
        try {
            neg = Long.valueOf(FieldUtils.safeNegate(val));
        } catch (Throwable t) {
            negThrown = t;
        }

        if (mulThrown != null || negThrown != null) {
            if (mulThrown != null && negThrown != null &&
                mulThrown instanceof ArithmeticException && negThrown instanceof ArithmeticException) {
                return;
            }
            return;
        }

        /* Contract guarantee asserted: both safeMultiply(value, -1) and safeNegate(value)
           are members of the documented safe* family performing checked negation; for any
           correct implementation on inputs where neither rejects, they must produce the same
           observable long result. A patch that merely removes or bypasses the throw at
           val=Long.MIN_VALUE,-1, or silently returns a wrong negated value, violates this. */
        if (mul.longValue() != neg.longValue()) {
            throw new FuzzerSecurityIssueLow("relation safeMultiply_minusOne_agrees_with_safeNegate violated: input=" + val + " lhs=" + mul + " rhs=" + neg);
        }
    }

    private static void exercisePatchedBranch(FuzzedDataProvider data) {
        long val1;
        int val2;
        try {
            boolean chooseSentinel = data.consumeBoolean();
            if (chooseSentinel) {
                val1 = Long.MIN_VALUE;
                val2 = -1;
            } else {
                int hi = data.consumeInt();
                int lo = data.consumeInt();
                val1 = (((long) hi) << 32) ^ (((long) lo) & 0xffffffffL);
                int selector = data.consumeInt(0, 4);
                if (selector == 0) {
                    val2 = -1;
                } else if (selector == 1) {
                    val2 = 0;
                } else if (selector == 2) {
                    val2 = 1;
                } else {
                    val2 = data.consumeInt();
                }
            }
        } catch (Exception e) {
            return;
        }

        try {
            FieldUtils.safeMultiply(val1, val2);
        } catch (ArithmeticException expected) {
            return;
        } catch (Throwable t) {
            return;
        }
    }

    private static void assertEqualsLong(String oracleId, long expected, long actual) {
        if (expected != actual) {
            throw new FuzzerSecurityIssueLow(oracleId + " semantic mismatch: expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertThrowsArithmetic(String oracleId, long val1, int val2) {
        try {
            FieldUtils.safeMultiply(val1, val2);
        } catch (ArithmeticException e) {
            return;
        }
        throw new FuzzerSecurityIssueLow(oracleId + " semantic mismatch: expected ArithmeticException for input=" + val1 + "," + val2);
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/004_patch1-Time-15-ACS_c_rep001/Time_15_buggy/fuzz/attempt_010/FuzzHarness.java:121: error: incompatible types: possible lossy conversion from long to int
            neg = Long.valueOf(FieldUtils.safeNegate(val));
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
<details open><summary>▸ Output (~7,295 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedOracles();
        relationOverloadAgreementLongIntLongLongSmallInputs(data);
        relationOverloadAgreementLongIntIntIntSmallInputs(data);
        relationSafeMultiplyByMinusOneAgreesWithSafeNegate(data);
        exercisePatchedBranch(data);
    }

    private static void checkLiftedOracles() {
        assertEqualsLong("[oracle:lifted-0x0]", 0L, FieldUtils.safeMultiply(0L, 0));
        assertEqualsLong("[oracle:lifted-1x1]", 1L, FieldUtils.safeMultiply(1L, 1));
        assertEqualsLong("[oracle:lifted-1x3]", 3L, FieldUtils.safeMultiply(1L, 3));
        assertEqualsLong("[oracle:lifted-3x1]", 3L, FieldUtils.safeMultiply(3L, 1));

        assertEqualsLong("[oracle:lifted-2x3]", 6L, FieldUtils.safeMultiply(2L, 3));
        assertEqualsLong("[oracle:lifted-2x-3]", -6L, FieldUtils.safeMultiply(2L, -3));
        assertEqualsLong("[oracle:lifted--2x3]", -6L, FieldUtils.safeMultiply(-2L, 3));
        assertEqualsLong("[oracle:lifted--2x-3]", 6L, FieldUtils.safeMultiply(-2L, -3));

        assertEqualsLong("[oracle:lifted--1xintmin]", -1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE));

        assertEqualsLong("[oracle:lifted-longmaxx1]", Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1));
        assertEqualsLong("[oracle:lifted-longminx1]", Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1));
        assertEqualsLong("[oracle:lifted-longmaxx-1]", -Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1));

        assertThrowsArithmetic("[oracle:lifted-longminx-1]", Long.MIN_VALUE, -1);
        assertThrowsArithmetic("[oracle:lifted-longminx100]", Long.MIN_VALUE, 100);
        assertThrowsArithmetic("[oracle:lifted-longminxintmax]", Long.MIN_VALUE, Integer.MAX_VALUE);
        assertThrowsArithmetic("[oracle:lifted-longmaxxintmin]", Long.MAX_VALUE, Integer.MIN_VALUE);
    }

    private static void relationOverloadAgreementLongIntLongLongSmallInputs(FuzzedDataProvider data) {
        long val1;
        int val2;
        try {
            val1 = (long) data.consumeInt(-1000000, 1000000);
            val2 = data.consumeInt(-1000000, 1000000);
        } catch (Exception e) {
            return;
        }

        long r1;
        try {
            r1 = FieldUtils.safeMultiply(val1, val2);
        } catch (Exception e) {
            return;
        }

        long r2;
        try {
            r2 = FieldUtils.safeMultiply(val1, (long) val2);
        } catch (Exception e) {
            return;
        }

        if (r1 != r2) {
            throw new FuzzerSecurityIssueLow("relation overload_agreement_longInt_longLong_small_inputs violated: input=" + val1 + "," + val2 + " lhs=" + r1 + " rhs=" + r2);
        }
    }

    private static void relationOverloadAgreementLongIntIntIntSmallInputs(FuzzedDataProvider data) {
        int a;
        int b;
        try {
            a = data.consumeInt(-46340, 46340);
            b = data.consumeInt(-46340, 46340);
        } catch (Exception e) {
            return;
        }

        long r1;
        try {
            r1 = FieldUtils.safeMultiply((long) a, b);
        } catch (Exception e) {
            return;
        }

        int r2;
        try {
            r2 = FieldUtils.safeMultiply(a, b);
        } catch (Exception e) {
            return;
        }

        if (r1 != (long) r2) {
            throw new FuzzerSecurityIssueLow("relation overload_agreement_longInt_intInt_small_inputs violated: input=" + a + "," + b + " lhs=" + r1 + " rhs=" + r2);
        }
    }

    private static void relationSafeMultiplyByMinusOneAgreesWithSafeNegate(FuzzedDataProvider data) {
        int val;
        try {
            if (data.consumeBoolean()) {
                val = data.consumeInt(-1000000, 1000000);
            } else {
                val = Integer.MIN_VALUE;
            }
        } catch (Exception e) {
            return;
        }

        Long mul = null;
        Throwable mulThrown = null;
        try {
            mul = Long.valueOf(FieldUtils.safeMultiply((long) val, -1));
        } catch (Throwable t) {
            mulThrown = t;
        }

        Integer neg = null;
        Throwable negThrown = null;
        try {
            neg = Integer.valueOf(FieldUtils.safeNegate(val));
        } catch (Throwable t) {
            negThrown = t;
        }

        if (mulThrown != null || negThrown != null) {
            if (mulThrown != null && negThrown != null &&
                mulThrown instanceof ArithmeticException && negThrown instanceof ArithmeticException) {
                return;
            }
            return;
        }

        /* Contract guarantee asserted: safeMultiply((long)value, -1) and safeNegate(value)
           are both checked negation in the safe* family; for any correct implementation on
           int inputs where neither rejects, they must yield the same numeric result. A patch
           that removes or bypasses the -1 overflow rejection, or returns a wrong negated
           value instead of throwing, violates this post-condition. */
        if (mul.longValue() != (long) neg.intValue()) {
            throw new FuzzerSecurityIssueLow("relation safeMultiply_minusOne_agrees_with_safeNegate violated: input=" + val + " lhs=" + mul + " rhs=" + neg);
        }
    }

    private static void exercisePatchedBranch(FuzzedDataProvider data) {
        long val1;
        int val2;
        try {
            boolean chooseSentinel = data.consumeBoolean();
            if (chooseSentinel) {
                val1 = Long.MIN_VALUE;
                val2 = -1;
            } else {
                int hi = data.consumeInt();
                int lo = data.consumeInt();
                val1 = (((long) hi) << 32) ^ (((long) lo) & 0xffffffffL);
                int selector = data.consumeInt(0, 4);
                if (selector == 0) {
                    val2 = -1;
                } else if (selector == 1) {
                    val2 = 0;
                } else if (selector == 2) {
                    val2 = 1;
                } else {
                    val2 = data.consumeInt();
                }
            }
        } catch (Exception e) {
            return;
        }

        try {
            FieldUtils.safeMultiply(val1, val2);
        } catch (ArithmeticException expected) {
            return;
        } catch (Throwable t) {
            return;
        }
    }

    private static void assertEqualsLong(String oracleId, long expected, long actual) {
        if (expected != actual) {
            throw new FuzzerSecurityIssueLow(oracleId + " semantic mismatch: expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertThrowsArithmetic(String oracleId, long val1, int val2) {
        try {
            FieldUtils.safeMultiply(val1, val2);
        } catch (ArithmeticException e) {
            return;
        }
        throw new FuzzerSecurityIssueLow(oracleId + " semantic mismatch: expected ArithmeticException for input=" + val1 + "," + val2);
    }
}
```

</details>

---
## [60] ⚙️ harness-repair · `attempt_011`
**output:** **REPAIRED: missing-alarm-id**
- detail: {'applied': ['missing-alarm-id'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [61] ⚙️ harness-attempt · `attempt_011`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: missing-alarm-id]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['missing-alarm-id'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:unnamed-check] [oracle:lifted-longminx-1] semantic mismatch: expected ArithmeticException for input=-9223372036854775808,-1

---
## [62] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [39] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,530 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actualLong;
        int actualInt;

        actualLong = FieldUtils.safeMultiply(0L, 0);
        if (actualLong != 0L) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-0] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 1);
        if (actualLong != 1L) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-1] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 3);
        if (actualLong != 3L) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-2] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(3L, 1);
        if (actualLong != 3L) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-3] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, 3);
        if (actualLong != 6L) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-4] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, -3);
        if (actualLong != -6L) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-5] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, 3);
        if (actualLong != -6L) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-6] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, -3);
        if (actualLong != 6L) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-7] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        if (actualLong != (-1L * Integer.MIN_VALUE)) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-8] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected=" + (-1L * Integer.MIN_VALUE) + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        if (actualLong != Long.MAX_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-9] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected=" + Long.MAX_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        if (actualLong != Long.MIN_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-10] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected=" + Long.MIN_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        if (actualLong != (-Long.MAX_VALUE)) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected=" + (-Long.MAX_VALUE) + " actual=" + actualLong);
        }

        boolean threw = false;
        try {
            actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-exc-0] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) was expected to throw ArithmeticException but returned=" + actualLong);
        }

        threw = false;
        try {
            actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-exc-1] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) was expected to throw ArithmeticException but returned=" + actualLong);
        }

        threw = false;
        try {
            actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-exc-2] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) was expected to throw ArithmeticException but returned=" + actualLong);
        }

        threw = false;
        try {
            actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-exc-3] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) was expected to throw ArithmeticException but returned=" + actualLong);
        }

        int a = data.consumeInt(-46340, 46340);
        int b = data.consumeInt(-46340, 46340);

        try {
            long li = FieldUtils.safeMultiply((long) a, b);
            long ll = FieldUtils.safeMultiply((long) a, (long) b);
            actualInt = FieldUtils.safeMultiply(a, b);

            // Contract justification: the three safeMultiply overloads all document the same operation,
            // "Multiply two values throwing an exception if overflow occurs". For inputs chosen so no
            // overflow can occur in any overload, all real overloads must agree on the exact product.
            // A patch that silently changes the -1/1 fast path or returns a wrong value instead of the
            // mathematical product would violate this post-condition even without throwing.
            if (li != ll) {
                throw new RuntimeException("[oracle:overload-long] metamorphic violation: safeMultiply(long,int) != safeMultiply(long,long) inputA=" + a + " inputB=" + b + " lhs=" + li + " rhs=" + ll);
            }
            if (li != (long) actualInt) {
                throw new RuntimeException("[oracle:overload-int] metamorphic violation: safeMultiply(long,int) != safeMultiply(int,int) inputA=" + a + " inputB=" + b + " lhs=" + li + " rhs=" + actualInt);
            }

            if (b == 1 && li != a) {
                throw new RuntimeException("[oracle:identity-one] metamorphic violation: multiplication by one changed value inputA=" + a + " inputB=" + b + " lhs=" + li + " rhs=" + a);
            }
            if (b == 0 && li != 0L) {
                throw new RuntimeException("[oracle:identity-zero] metamorphic violation: multiplication by zero was non-zero inputA=" + a + " inputB=" + b + " lhs=" + li + " rhs=0");
            }
            if (b == -1) {
                long expectedNeg = -((long) a);
                if (li != expectedNeg) {
                    throw new RuntimeException("[oracle:identity-negone] metamorphic violation: multiplication by minus one did not negate inputA=" + a + " inputB=" + b + " lhs=" + li + " rhs=" + expectedNeg);
                }
            }
        } catch (ArithmeticException ignored) {
            return;
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
<details><summary>▸ Prompt (4 message(s), ~8,674 chars, 2 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [39] — not reprinted*
**[assistant]**
```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actualLong;
        int actualInt;

        actualLong = FieldUtils.safeMultiply(0L, 0);
        if (actualLong != 0L) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-0] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 1);
        if (actualLong != 1L) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-1] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 3);
        if (actualLong != 3L) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-2] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(3L, 1);
        if (actualLong != 3L) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-3] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, 3);
        if (actualLong != 6L) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-4] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, -3);
        if (actualLong != -6L) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-5] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, 3);
        if (actualLong != -6L) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-6] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, -3);
        if (actualLong != 6L) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-7] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        if (actualLong != (-1L * Integer.MIN_VALUE)) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-8] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected=" + (-1L * Integer.MIN_VALUE) + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        if (actualLong != Long.MAX_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-9] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected=" + Long.MAX_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        if (actualLong != Long.MIN_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-10] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected=" + Long.MIN_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        if (actualLong != (-Long.MAX_VALUE)) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected=" + (-Long.MAX_VALUE) + " actual=" + actualLong);
        }

        boolean threw = false;
        try {
            actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-exc-0] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) was expected to throw ArithmeticException but returned=" + actualLong);
        }

        threw = false;
        try {
            actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-exc-1] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) was expected to throw ArithmeticException but returned=" + actualLong);
        }

        threw = false;
        try {
            actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-exc-2] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) was expected to throw ArithmeticException but returned=" + actualLong);
        }

        threw = false;
        try {
            actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-exc-3] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) was expected to throw ArithmeticException but returned=" + actualLong);
        }

        int a = data.consumeInt(-46340, 46340);
        int b = data.consumeInt(-46340, 46340);

        try {
            long li = FieldUtils.safeMultiply((long) a, b);
            long ll = FieldUtils.safeMultiply((long) a, (long) b);
            actualInt = FieldUtils.safeMultiply(a, b);

            // Contract justification: the three safeMultiply overloads all document the same operation,
            // "Multiply two values throwing an exception if overflow occurs". For inputs chosen so no
            // overflow can occur in any overload, all real overloads must agree on the exact product.
            // A patch that silently changes the -1/1 fast path or returns a wrong value instead of the
            // mathematical product would violate this post-condition even without throwing.
            if (li != ll) {
                throw new RuntimeException("[oracle:overload-long] metamorphic violation: safeMultiply(long,int) != safeMultiply(long,long) inputA=" + a + " inputB=" + b + " lhs=" + li + " rhs=" + ll);
            }
            if (li != (long) actualInt) {
                throw new RuntimeException("[oracle:overload-int] metamorphic violation: safeMultiply(long,int) != safeMultiply(int,int) inputA=" + a + " inputB=" + b + " lhs=" + li + " rhs=" + actualInt);
            }

            if (b == 1 && li != a) {
                throw new RuntimeException("[oracle:identity-one] metamorphic violation: multiplication by one changed value inputA=" + a + " inputB=" + b + " lhs=" + li + " rhs=" + a);
            }
            if (b == 0 && li != 0L) {
                throw new RuntimeException("[oracle:identity-zero] metamorphic violation: multiplication by zero was non-zero inputA=" + a + " inputB=" + b + " lhs=" + li + " rhs=0");
            }
            if (b == -1) {
                long expectedNeg = -((long) a);
                if (li != expectedNeg) {
                    throw new RuntimeException("[oracle:identity-negone] metamorphic violation: multiplication by minus one did not negate inputA=" + a + " inputB=" + b + " lhs=" + li + " rhs=" + expectedNeg);
                }
            }
        } catch (ArithmeticException ignored) {
            return;
        }
    }
}
```
**[user]**
```
Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `threw` (literal assignment, no rethrow, the caught exception is never used) and the alarm near line 78 is conditioned on `threw` — the caught exception's identity is destroyed, so attribution cannot tell WHICH exception fired or whether the unpatched build crashes the same way. Rethrow the exception as the alarm's cause (`..., e`), or assert on the real observable instead of a swallowed boolean.
A caught exception is a REJECTION, never a violation, and flattening it into a flag destroys the identity attribution needs. Either assert on the real observable the test pins (compute the actual value and compare it), or if you must re-report a caught crash, re-throw it with the original as the cause: `new FuzzerSecurityIssueLow("[oracle:<id>] ...", e)`. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~6,449 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actualLong;
        int actualInt;

        actualLong = FieldUtils.safeMultiply(0L, 0);
        if (actualLong != 0L) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-0] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 1);
        if (actualLong != 1L) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-1] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 3);
        if (actualLong != 3L) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-2] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(3L, 1);
        if (actualLong != 3L) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-3] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, 3);
        if (actualLong != 6L) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-4] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, -3);
        if (actualLong != -6L) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-5] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, 3);
        if (actualLong != -6L) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-6] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, -3);
        if (actualLong != 6L) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-7] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        if (actualLong != (-1L * Integer.MIN_VALUE)) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-8] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected=" + (-1L * Integer.MIN_VALUE) + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        if (actualLong != Long.MAX_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-9] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected=" + Long.MAX_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        if (actualLong != Long.MIN_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-10] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected=" + Long.MIN_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        if (actualLong != (-Long.MAX_VALUE)) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected=" + (-Long.MAX_VALUE) + " actual=" + actualLong);
        }

        expectArithmetic(Long.MIN_VALUE, -1, "seed-exc-0");
        expectArithmetic(Long.MIN_VALUE, 100, "seed-exc-1");
        expectArithmetic(Long.MIN_VALUE, Integer.MAX_VALUE, "seed-exc-2");
        expectArithmetic(Long.MAX_VALUE, Integer.MIN_VALUE, "seed-exc-3");

        int a = data.consumeInt(-46340, 46340);
        int b = data.consumeInt(-46340, 46340);

        try {
            long li = FieldUtils.safeMultiply((long) a, b);
            long ll = FieldUtils.safeMultiply((long) a, (long) b);
            actualInt = FieldUtils.safeMultiply(a, b);

            // Contract justification: all safeMultiply overloads document the same mathematical
            // multiplication with overflow checking. For bounded inputs where no overflow occurs,
            // the real overloads must agree exactly on the result. A patch that suppresses a throw
            // by returning a wrong value, or that breaks a fast path like val2 == -1/0/1, violates
            // this observable post-condition even if no exception is thrown.
            if (li != ll) {
                throw new RuntimeException("[oracle:overload-long] metamorphic violation: safeMultiply(long,int) != safeMultiply(long,long) inputA=" + a + " inputB=" + b + " lhs=" + li + " rhs=" + ll);
            }
            if (li != (long) actualInt) {
                throw new RuntimeException("[oracle:overload-int] metamorphic violation: safeMultiply(long,int) != safeMultiply(int,int) inputA=" + a + " inputB=" + b + " lhs=" + li + " rhs=" + actualInt);
            }

            if (b == 1 && li != a) {
                throw new RuntimeException("[oracle:identity-one] metamorphic violation: multiplication by one changed value inputA=" + a + " inputB=" + b + " lhs=" + li + " rhs=" + a);
            }
            if (b == 0 && li != 0L) {
                throw new RuntimeException("[oracle:identity-zero] metamorphic violation: multiplication by zero was non-zero inputA=" + a + " inputB=" + b + " lhs=" + li + " rhs=0");
            }
            if (b == -1) {
                long expectedNeg = -((long) a);
                if (li != expectedNeg) {
                    throw new RuntimeException("[oracle:identity-negone] metamorphic violation: multiplication by minus one did not negate inputA=" + a + " inputB=" + b + " lhs=" + li + " rhs=" + expectedNeg);
                }
            }
        } catch (ArithmeticException ignored) {
            return;
        }
    }

    private static void expectArithmetic(long val1, int val2, String oracleId) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new FuzzerSecurityIssueLow("[oracle:" + oracleId + "] semantic mismatch: FieldUtils.safeMultiply(" + val1 + ", " + val2 + ") was expected to throw ArithmeticException but returned=" + actual);
        } catch (ArithmeticException expected) {
            return;
        }
    }
}
```

</details>

---
## [65] ⚙️ harness-attempt · `attempt_013`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:seed-exc-0] semantic mismatch: FieldUtils.safeMultiply(-9223372036854775808, -1) was expected to throw ArithmeticException but returned…

---
## [66] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [36] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~6,690 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static void assertLongEquals(String oracleId, long expected, long actual, String detail) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + detail +
                " expected=" + expected + " actual=" + actual
            );
        }
    }

    private static void assertArithmeticException(String oracleId, long val1, int val2) {
        boolean threw = false;
        try {
            FieldUtils.safeMultiply(val1, val2);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: expected ArithmeticException from FieldUtils.safeMultiply(" +
                val1 + ", " + val2 + ")"
            );
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        assertLongEquals("fixed-0", 0L, FieldUtils.safeMultiply(0L, 0), "FieldUtils.safeMultiply(0L, 0)");
        assertLongEquals("fixed-1", 1L, FieldUtils.safeMultiply(1L, 1), "FieldUtils.safeMultiply(1L, 1)");
        assertLongEquals("fixed-2", 3L, FieldUtils.safeMultiply(1L, 3), "FieldUtils.safeMultiply(1L, 3)");
        assertLongEquals("fixed-3", 3L, FieldUtils.safeMultiply(3L, 1), "FieldUtils.safeMultiply(3L, 1)");
        assertLongEquals("fixed-4", 6L, FieldUtils.safeMultiply(2L, 3), "FieldUtils.safeMultiply(2L, 3)");
        assertLongEquals("fixed-5", -6L, FieldUtils.safeMultiply(2L, -3), "FieldUtils.safeMultiply(2L, -3)");
        assertLongEquals("fixed-6", -6L, FieldUtils.safeMultiply(-2L, 3), "FieldUtils.safeMultiply(-2L, 3)");
        assertLongEquals("fixed-7", 6L, FieldUtils.safeMultiply(-2L, -3), "FieldUtils.safeMultiply(-2L, -3)");
        assertLongEquals(
            "fixed-8",
            -1L * Integer.MIN_VALUE,
            FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE),
            "FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE)"
        );
        assertLongEquals(
            "fixed-9",
            Long.MAX_VALUE,
            FieldUtils.safeMultiply(Long.MAX_VALUE, 1),
            "FieldUtils.safeMultiply(Long.MAX_VALUE, 1)"
        );
        assertLongEquals(
            "fixed-10",
            Long.MIN_VALUE,
            FieldUtils.safeMultiply(Long.MIN_VALUE, 1),
            "FieldUtils.safeMultiply(Long.MIN_VALUE, 1)"
        );
        assertLongEquals(
            "fixed-11",
            -Long.MAX_VALUE,
            FieldUtils.safeMultiply(Long.MAX_VALUE, -1),
            "FieldUtils.safeMultiply(Long.MAX_VALUE, -1)"
        );

        assertArithmeticException("throws-min-neg1", Long.MIN_VALUE, -1);
        assertArithmeticException("throws-min-100", Long.MIN_VALUE, 100);
        assertArithmeticException("throws-min-intmax", Long.MIN_VALUE, Integer.MAX_VALUE);
        assertArithmeticException("throws-max-intmin", Long.MAX_VALUE, Integer.MIN_VALUE);

        int rel1Val1Int = data.consumeInt(-1000000, 1000000);
        int rel1Val2 = data.consumeInt(-1000000, 1000000);
        long rel1Left;
        long rel1Right;
        try {
            rel1Left = FieldUtils.safeMultiply((long) rel1Val1Int, rel1Val2);
            rel1Right = FieldUtils.safeMultiply((long) rel1Val1Int, (long) rel1Val2);
        } catch (Exception e) {
            rel1Left = 0L;
            rel1Right = 0L;
            return;
        }
        if (rel1Left != rel1Right) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:overload-agreement-longint-longlong] metamorphic violation: " +
                "safeMultiply(long,int) must agree with safeMultiply(long,long) on equivalent small non-overflow inputs " +
                "input1=" + rel1Val1Int + " input2=" + rel1Val2 +
                " lhs=" + rel1Left + " rhs=" + rel1Right
            );
        }

        int rel2A = data.consumeInt(-46340, 46340);
        int rel2B = data.consumeInt(-46340, 46340);
        long rel2Left;
        int rel2Right;
        try {
            rel2Left = FieldUtils.safeMultiply((long) rel2A, rel2B);
            rel2Right = FieldUtils.safeMultiply(rel2A, rel2B);
        } catch (Exception e) {
            rel2Left = 0L;
            rel2Right = 0;
            return;
        }
        if (rel2Left != (long) rel2Right) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:overload-agreement-longint-intint] metamorphic violation: " +
                "safeMultiply(long,int) must agree with safeMultiply(int,int) when both inputs and product fit in int " +
                "input1=" + rel2A + " input2=" + rel2B +
                " lhs=" + rel2Left + " rhs=" + rel2Right
            );
        }

        int rel3N = data.consumeInt(-1000000, 1000000);
        long rel3Mul;
        int rel3Neg;
        try {
            rel3Mul = FieldUtils.safeMultiply((long) rel3N, -1);
            rel3Neg = FieldUtils.safeNegate(rel3N);
        } catch (Exception e) {
            rel3Mul = 0L;
            rel3Neg = 0;
            return;
        }
        if (rel3Mul != (long) rel3Neg) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:negate-agreement] metamorphic violation: " +
                "the safe* family documents safe arithmetic with the same mathematical result, so multiplying by -1 must match safeNegate for int-range inputs " +
                "input=" + rel3N + " lhs=" + rel3Mul + " rhs=" + rel3Neg
            );
        }

        int rel4N = data.consumeInt(-1000000, 1000000);
        long rel4Once;
        long rel4Twice;
        try {
            rel4Once = FieldUtils.safeMultiply((long) rel4N, -1);
            rel4Twice = FieldUtils.safeMultiply(rel4Once, -1);
        } catch (Exception e) {
            rel4Once = 0L;
            rel4Twice = 0L;
            return;
        }
        if (rel4Twice != (long) rel4N) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:double-negation] metamorphic violation: " +
                "for any non-overflowing value, multiplying by -1 twice must recover the original value; a patch that merely deletes the throw or returns the wrong sign breaks this post-condition " +
                "input=" + rel4N + " intermediate=" + rel4Once + " result=" + rel4Twice
            );
        }
    }
}
```

</details>

---
## [67] ⚙️ harness-repair · `attempt_014`
**output:** **REPAIRED: boolean-swallow**
- detail: {'applied': ['boolean-swallow'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [68] ⚙️ harness-attempt · `attempt_014`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: boolean-swallow]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['boolean-swallow'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:throws-min-neg1] semantic mismatch: expected ArithmeticException from FieldUtils.safeMultiply(-9223372036854775808, -1)

---
## [69] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [39] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,126 chars)</summary>

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
                "[oracle:lifted-eq-0] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 1);
        if (actualLong != 1L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-eq-1] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 3);
        if (actualLong != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-eq-2] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(3L, 1);
        if (actualLong != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-eq-3] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, 3);
        if (actualLong != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-eq-4] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, -3);
        if (actualLong != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-eq-5] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, 3);
        if (actualLong != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-eq-6] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, -3);
        if (actualLong != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-eq-7] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        if (actualLong != (-1L * Integer.MIN_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-eq-8] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected="
                    + (-1L * Integer.MIN_VALUE) + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        if (actualLong != Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-eq-9] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected="
                    + Long.MAX_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        if (actualLong != Long.MIN_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-eq-10] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected="
                    + Long.MIN_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        if (actualLong != -Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-eq-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected="
                    + (-Long.MAX_VALUE) + " actual=" + actualLong);
        }

        threw = false;
        try {
            actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-throw-0] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException actual="
                    + actualLong);
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-throw-1] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) expected ArithmeticException");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-throw-2] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) expected ArithmeticException");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-throw-3] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) expected ArithmeticException");
        }

        /*
         * Documented guarantee: the safeMultiply overloads all promise overflow-checked multiplication.
         * For the same mathematical operation (Long.MIN_VALUE * -1), the long,int and long,long overloads
         * must agree on rejection. A patch that deletes the overflow throw in one overload breaks this
         * observable sibling-agreement contract even if it never crashes.
         */
        boolean threwLongInt = false;
        boolean threwLongLong = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
        } catch (ArithmeticException expected) {
            threwLongInt = true;
        }
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1L);
        } catch (ArithmeticException expected) {
            threwLongLong = true;
        }
        if (threwLongInt != threwLongLong) {
            throw new RuntimeException(
                "[oracle:overload-overflow] metamorphic violation: safeMultiply(long,int) and safeMultiply(long,long) disagree on overflow rejection input=Long.MIN_VALUE,-1 longIntThrew="
                    + threwLongInt + " longLongThrew=" + threwLongLong);
        }

        int fuzzVal1 = data.consumeInt(-1_000_000, 1_000_000);
        int fuzzVal2 = data.consumeInt(-1_000, 1_000);
        long fuzzLong = fuzzVal1;

        /*
         * Documented guarantee: same-name overloads with the same operands must produce the same result
         * when both calls are valid; here magnitudes are fenced so the multiplication is safely within range.
         */
        try {
            long lhs = FieldUtils.safeMultiply(fuzzLong, fuzzVal2);
            long rhs = FieldUtils.safeMultiply(fuzzLong, (long) fuzzVal2);
            if (lhs != rhs) {
                throw new RuntimeException(
                    "[oracle:overload-eq] metamorphic violation: safeMultiply(long,int) != safeMultiply(long,long) input="
                        + fuzzLong + "," + fuzzVal2 + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Throwable ignored) {
            return;
        }
    }
}
```

</details>

---
## [70] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `threw` (literal assignment, no rethrow, the caught exceptio

---
## [71] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~9,270 chars, 2 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [39] — not reprinted*
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
                "[oracle:lifted-eq-0] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 1);
        if (actualLong != 1L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-eq-1] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 3);
        if (actualLong != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-eq-2] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(3L, 1);
        if (actualLong != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-eq-3] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, 3);
        if (actualLong != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-eq-4] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, -3);
        if (actualLong != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-eq-5] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, 3);
        if (actualLong != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-eq-6] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, -3);
        if (actualLong != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-eq-7] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        if (actualLong != (-1L * Integer.MIN_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-eq-8] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected="
                    + (-1L * Integer.MIN_VALUE) + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        if (actualLong != Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-eq-9] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected="
                    + Long.MAX_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        if (actualLong != Long.MIN_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-eq-10] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected="
                    + Long.MIN_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        if (actualLong != -Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-eq-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected="
                    + (-Long.MAX_VALUE) + " actual=" + actualLong);
        }

        threw = false;
        try {
            actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-throw-0] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException actual="
                    + actualLong);
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-throw-1] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) expected ArithmeticException");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-throw-2] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) expected ArithmeticException");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-throw-3] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) expected ArithmeticException");
        }

        /*
         * Documented guarantee: the safeMultiply overloads all promise overflow-checked multiplication.
         * For the same mathematical operation (Long.MIN_VALUE * -1), the long,int and long,long overloads
         * must agree on rejection. A patch that deletes the overflow throw in one overload breaks this
         * observable sibling-agreement contract even if it never crashes.
         */
        boolean threwLongInt = false;
        boolean threwLongLong = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
        } catch (ArithmeticException expected) {
            threwLongInt = true;
        }
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1L);
        } catch (ArithmeticException expected) {
            threwLongLong = true;
        }
        if (threwLongInt != threwLongLong) {
            throw new RuntimeException(
                "[oracle:overload-overflow] metamorphic violation: safeMultiply(long,int) and safeMultiply(long,long) disagree on overflow rejection input=Long.MIN_VALUE,-1 longIntThrew="
                    + threwLongInt + " longLongThrew=" + threwLongLong);
        }

        int fuzzVal1 = data.consumeInt(-1_000_000, 1_000_000);
        int fuzzVal2 = data.consumeInt(-1_000, 1_000);
        long fuzzLong = fuzzVal1;

        /*
         * Documented guarantee: same-name overloads with the same operands must produce the same result
         * when both calls are valid; here magnitudes are fenced so the multiplication is safely within range.
         */
        try {
            long lhs = FieldUtils.safeMultiply(fuzzLong, fuzzVal2);
            long rhs = FieldUtils.safeMultiply(fuzzLong, (long) fuzzVal2);
            if (lhs != rhs) {
                throw new RuntimeException(
                    "[oracle:overload-eq] metamorphic violation: safeMultiply(long,int) != safeMultiply(long,long) input="
                        + fuzzLong + "," + fuzzVal2 + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Throwable ignored) {
            return;
        }
    }
}
```
**[user]**
```
Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `threw` (literal assignment, no rethrow, the caught exception is never used) and the alarm near line 93 is conditioned on `threw` — the caught exception's identity is destroyed, so attribution cannot tell WHICH exception fired or whether the unpatched build crashes the same way. Rethrow the exception as the alarm's cause (`..., e`), or assert on the real observable instead of a swallowed boolean.
A caught exception is a REJECTION, never a violation, and flattening it into a flag destroys the identity attribution needs. Either assert on the real observable the test pins (compute the actual value and compare it), or if you must re-report a caught crash, re-throw it with the original as the cause: `new FuzzerSecurityIssueLow("[oracle:<id>] ...", e)`. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~6,552 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static void assertEqualsLong(String oracleId, String expr, long expected, long actual) {
        if (actual != expected) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + expr + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void expectArithmeticException(String oracleId, String expr, long val1, int val2) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + expr + " expected ArithmeticException actual=" + actual);
        } catch (ArithmeticException expected) {
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        assertEqualsLong("lifted-eq-0", "FieldUtils.safeMultiply(0L, 0)", 0L, FieldUtils.safeMultiply(0L, 0));
        assertEqualsLong("lifted-eq-1", "FieldUtils.safeMultiply(1L, 1)", 1L, FieldUtils.safeMultiply(1L, 1));
        assertEqualsLong("lifted-eq-2", "FieldUtils.safeMultiply(1L, 3)", 3L, FieldUtils.safeMultiply(1L, 3));
        assertEqualsLong("lifted-eq-3", "FieldUtils.safeMultiply(3L, 1)", 3L, FieldUtils.safeMultiply(3L, 1));
        assertEqualsLong("lifted-eq-4", "FieldUtils.safeMultiply(2L, 3)", 6L, FieldUtils.safeMultiply(2L, 3));
        assertEqualsLong("lifted-eq-5", "FieldUtils.safeMultiply(2L, -3)", -6L, FieldUtils.safeMultiply(2L, -3));
        assertEqualsLong("lifted-eq-6", "FieldUtils.safeMultiply(-2L, 3)", -6L, FieldUtils.safeMultiply(-2L, 3));
        assertEqualsLong("lifted-eq-7", "FieldUtils.safeMultiply(-2L, -3)", 6L, FieldUtils.safeMultiply(-2L, -3));
        assertEqualsLong(
            "lifted-eq-8",
            "FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE)",
            -1L * Integer.MIN_VALUE,
            FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE));
        assertEqualsLong(
            "lifted-eq-9",
            "FieldUtils.safeMultiply(Long.MAX_VALUE, 1)",
            Long.MAX_VALUE,
            FieldUtils.safeMultiply(Long.MAX_VALUE, 1));
        assertEqualsLong(
            "lifted-eq-10",
            "FieldUtils.safeMultiply(Long.MIN_VALUE, 1)",
            Long.MIN_VALUE,
            FieldUtils.safeMultiply(Long.MIN_VALUE, 1));
        assertEqualsLong(
            "lifted-eq-11",
            "FieldUtils.safeMultiply(Long.MAX_VALUE, -1)",
            -Long.MAX_VALUE,
            FieldUtils.safeMultiply(Long.MAX_VALUE, -1));

        expectArithmeticException("lifted-throw-0", "FieldUtils.safeMultiply(Long.MIN_VALUE, -1)", Long.MIN_VALUE, -1);
        expectArithmeticException("lifted-throw-1", "FieldUtils.safeMultiply(Long.MIN_VALUE, 100)", Long.MIN_VALUE, 100);
        expectArithmeticException(
            "lifted-throw-2",
            "FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE)",
            Long.MIN_VALUE,
            Integer.MAX_VALUE);
        expectArithmeticException(
            "lifted-throw-3",
            "FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE)",
            Long.MAX_VALUE,
            Integer.MIN_VALUE);

        /*
         * Documented guarantee: same-name safeMultiply overloads all perform overflow-checked multiplication.
         * For the same mathematical operation Long.MIN_VALUE * -1, the long,int and long,long overloads must
         * agree on rejecting overflow. A patch that removes the throw in one overload breaks this observable
         * sibling-agreement post-condition.
         */
        Throwable exLongInt = null;
        Throwable exLongLong = null;
        long resLongInt = 0L;
        long resLongLong = 0L;
        try {
            resLongInt = FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
        } catch (Throwable e) {
            exLongInt = e;
        }
        try {
            resLongLong = FieldUtils.safeMultiply(Long.MIN_VALUE, -1L);
        } catch (Throwable e) {
            exLongLong = e;
        }

        if (exLongInt == null && exLongLong == null) {
            if (resLongInt != resLongLong) {
                throw new RuntimeException(
                    "[oracle:overload-overflow] metamorphic violation: safeMultiply(long,int) and safeMultiply(long,long) disagree on result input="
                        + Long.MIN_VALUE + ",-1 lhs=" + resLongInt + " rhs=" + resLongLong);
            }
        } else if (exLongInt instanceof ArithmeticException && exLongLong instanceof ArithmeticException) {
        } else if (exLongInt == null || exLongLong == null
                || !exLongInt.getClass().equals(exLongLong.getClass())) {
            throw new RuntimeException(
                "[oracle:overload-overflow] metamorphic violation: safeMultiply(long,int) and safeMultiply(long,long) disagree on overflow handling input="
                    + Long.MIN_VALUE + ",-1 longIntException="
                    + (exLongInt == null ? "none" : exLongInt.getClass().getName())
                    + " longLongException="
                    + (exLongLong == null ? "none" : exLongLong.getClass().getName()));
        }

        int fuzzVal1 = data.consumeInt(-1_000_000, 1_000_000);
        int fuzzVal2 = data.consumeInt(-1_000, 1_000);
        long fuzzLong = fuzzVal1;

        /*
         * Documented guarantee: these overloads describe the same checked multiplication on equivalent inputs.
         * With fenced magnitudes both calls are valid, so a correct implementation must return the same long.
         */
        Throwable fuzzExLongInt = null;
        Throwable fuzzExLongLong = null;
        long lhs = 0L;
        long rhs = 0L;
        try {
            lhs = FieldUtils.safeMultiply(fuzzLong, fuzzVal2);
        } catch (Throwable e) {
            fuzzExLongInt = e;
        }
        try {
            rhs = FieldUtils.safeMultiply(fuzzLong, (long) fuzzVal2);
        } catch (Throwable e) {
            fuzzExLongLong = e;
        }

        if (fuzzExLongInt != null || fuzzExLongLong != null) {
            return;
        }

        if (lhs != rhs) {
            throw new RuntimeException(
                "[oracle:overload-eq] metamorphic violation: safeMultiply(long,int) != safeMultiply(long,long) input="
                    + fuzzLong + "," + fuzzVal2 + " lhs=" + lhs + " rhs=" + rhs);
        }
    }
}
```

</details>

---
## [72] ⚙️ harness-attempt · `attempt_016`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-throw-0] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException actual=-9223372036854775808

---
## [73] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [36] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,193 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actualLong;
        boolean threw;

        actualLong = FieldUtils.safeMultiply(0L, 0);
        if (actualLong != 0L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-0x0] semantic mismatch: expected=0 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 1);
        if (actualLong != 1L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-1x1] semantic mismatch: expected=1 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 3);
        if (actualLong != 3L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-1x3] semantic mismatch: expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(3L, 1);
        if (actualLong != 3L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-3x1] semantic mismatch: expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, 3);
        if (actualLong != 6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-2x3] semantic mismatch: expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, -3);
        if (actualLong != -6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-2x-3] semantic mismatch: expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, 3);
        if (actualLong != -6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--2x3] semantic mismatch: expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, -3);
        if (actualLong != 6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--2x-3] semantic mismatch: expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        if (actualLong != (-1L * Integer.MIN_VALUE)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--1xintmin] semantic mismatch: expected=" + (-1L * Integer.MIN_VALUE) + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        if (actualLong != Long.MAX_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxx1] semantic mismatch: expected=" + Long.MAX_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        if (actualLong != Long.MIN_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx1] semantic mismatch: expected=" + Long.MIN_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        if (actualLong != -Long.MAX_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxx-1] semantic mismatch: expected=" + (-Long.MAX_VALUE) + " actual=" + actualLong);
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx-1-throws] semantic mismatch: expected ArithmeticException for FieldUtils.safeMultiply(Long.MIN_VALUE, -1)");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx100-throws] semantic mismatch: expected ArithmeticException for FieldUtils.safeMultiply(Long.MIN_VALUE, 100)");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minxintmax-throws] semantic mismatch: expected ArithmeticException for FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE)");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxxintmin-throws] semantic mismatch: expected ArithmeticException for FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE)");
        }

        int fuzzA;
        int fuzzB;
        try {
            fuzzA = data.consumeInt(-46340, 46340);
            fuzzB = data.consumeInt(-46340, 46340);
        } catch (Exception e) {
            return;
        }
        long rel1Left;
        int rel1Right;
        try {
            rel1Left = FieldUtils.safeMultiply((long) fuzzA, fuzzB);
            rel1Right = FieldUtils.safeMultiply(fuzzA, fuzzB);
        } catch (Exception e) {
            return;
        }
        if (rel1Left != (long) rel1Right) {
            throw new FuzzerSecurityIssueLow("relation overload_agreement_longInt_intInt_small_inputs violated: lhs=" + rel1Left + " rhs=" + rel1Right + " a=" + fuzzA + " b=" + fuzzB);
        }

        long fuzzVal1;
        int fuzzVal2;
        try {
            fuzzVal1 = (long) data.consumeInt(-1000000, 1000000);
            fuzzVal2 = data.consumeInt(-1000000, 1000000);
        } catch (Exception e) {
            return;
        }
        long rel2Left;
        long rel2Right;
        try {
            rel2Left = FieldUtils.safeMultiply(fuzzVal1, fuzzVal2);
            rel2Right = FieldUtils.safeMultiply(fuzzVal1, (long) fuzzVal2);
        } catch (Exception e) {
            return;
        }
        if (rel2Left != rel2Right) {
            throw new FuzzerSecurityIssueLow("relation overload_agreement_longInt_longLong_small_inputs violated: lhs=" + rel2Left + " rhs=" + rel2Right + " val1=" + fuzzVal1 + " val2=" + fuzzVal2);
        }

        int identitySeed;
        try {
            identitySeed = data.consumeInt(-1000000, 1000000);
        } catch (Exception e) {
            return;
        }
        long identityValue = (long) identitySeed;
        long timesOne;
        long timesZero;
        try {
            timesOne = FieldUtils.safeMultiply(identityValue, 1);
            timesZero = FieldUtils.safeMultiply(identityValue, 0);
        } catch (Exception e) {
            return;
        }
        /* Contract/post-condition check: multiplying by 1 must return the original value and multiplying by 0 must return 0.
           A patch that merely deletes/reorders the special-case branch or makes it unreachable can silently return the wrong value
           without throwing; these observable results must still hold for every correct implementation. */
        if (timesOne != identityValue) {
            throw new FuzzerSecurityIssueLow("[oracle:identity-x1] metamorphic violation: safeMultiply(x,1)==x input=" + identityValue + " lhs=" + timesOne + " rhs=" + identityValue);
        }
        if (timesZero != 0L) {
            throw new FuzzerSecurityIssueLow("[oracle:annihilator-x0] metamorphic violation: safeMultiply(x,0)==0 input=" + identityValue + " lhs=" + timesZero + " rhs=0");
        }

        int signSeed;
        try {
            signSeed = data.consumeInt(-1000000, 1000000);
        } catch (Exception e) {
            return;
        }
        long signValue = (long) signSeed;
        long pos;
        long neg;
        try {
            pos = FieldUtils.safeMultiply(signValue, 1);
            neg = FieldUtils.safeMultiply(signValue, -1);
        } catch (Exception e) {
            return;
        }
        if (neg != -pos) {
            throw new FuzzerSecurityIssueLow("[oracle:sign-flip] metamorphic violation: safeMultiply(x,-1)==-safeMultiply(x,1) input=" + signValue + " lhs=" + neg + " rhs=" + (-pos));
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
<details><summary>▸ Prompt (4 message(s), ~8,393 chars, 1 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [36] — not reprinted*
**[assistant]**
```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actualLong;
        boolean threw;

        actualLong = FieldUtils.safeMultiply(0L, 0);
        if (actualLong != 0L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-0x0] semantic mismatch: expected=0 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 1);
        if (actualLong != 1L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-1x1] semantic mismatch: expected=1 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 3);
        if (actualLong != 3L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-1x3] semantic mismatch: expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(3L, 1);
        if (actualLong != 3L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-3x1] semantic mismatch: expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, 3);
        if (actualLong != 6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-2x3] semantic mismatch: expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, -3);
        if (actualLong != -6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-2x-3] semantic mismatch: expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, 3);
        if (actualLong != -6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--2x3] semantic mismatch: expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, -3);
        if (actualLong != 6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--2x-3] semantic mismatch: expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        if (actualLong != (-1L * Integer.MIN_VALUE)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--1xintmin] semantic mismatch: expected=" + (-1L * Integer.MIN_VALUE) + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        if (actualLong != Long.MAX_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxx1] semantic mismatch: expected=" + Long.MAX_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        if (actualLong != Long.MIN_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx1] semantic mismatch: expected=" + Long.MIN_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        if (actualLong != -Long.MAX_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxx-1] semantic mismatch: expected=" + (-Long.MAX_VALUE) + " actual=" + actualLong);
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx-1-throws] semantic mismatch: expected ArithmeticException for FieldUtils.safeMultiply(Long.MIN_VALUE, -1)");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx100-throws] semantic mismatch: expected ArithmeticException for FieldUtils.safeMultiply(Long.MIN_VALUE, 100)");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minxintmax-throws] semantic mismatch: expected ArithmeticException for FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE)");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxxintmin-throws] semantic mismatch: expected ArithmeticException for FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE)");
        }

        int fuzzA;
        int fuzzB;
        try {
            fuzzA = data.consumeInt(-46340, 46340);
            fuzzB = data.consumeInt(-46340, 46340);
        } catch (Exception e) {
            return;
        }
        long rel1Left;
        int rel1Right;
        try {
            rel1Left = FieldUtils.safeMultiply((long) fuzzA, fuzzB);
            rel1Right = FieldUtils.safeMultiply(fuzzA, fuzzB);
        } catch (Exception e) {
            return;
        }
        if (rel1Left != (long) rel1Right) {
            throw new FuzzerSecurityIssueLow("relation overload_agreement_longInt_intInt_small_inputs violated: lhs=" + rel1Left + " rhs=" + rel1Right + " a=" + fuzzA + " b=" + fuzzB);
        }

        long fuzzVal1;
        int fuzzVal2;
        try {
            fuzzVal1 = (long) data.consumeInt(-1000000, 1000000);
            fuzzVal2 = data.consumeInt(-1000000, 1000000);
        } catch (Exception e) {
            return;
        }
        long rel2Left;
        long rel2Right;
        try {
            rel2Left = FieldUtils.safeMultiply(fuzzVal1, fuzzVal2);
            rel2Right = FieldUtils.safeMultiply(fuzzVal1, (long) fuzzVal2);
        } catch (Exception e) {
            return;
        }
        if (rel2Left != rel2Right) {
            throw new FuzzerSecurityIssueLow("relation overload_agreement_longInt_longLong_small_inputs violated: lhs=" + rel2Left + " rhs=" + rel2Right + " val1=" + fuzzVal1 + " val2=" + fuzzVal2);
        }

        int identitySeed;
        try {
            identitySeed = data.consumeInt(-1000000, 1000000);
        } catch (Exception e) {
            return;
        }
        long identityValue = (long) identitySeed;
        long timesOne;
        long timesZero;
        try {
            timesOne = FieldUtils.safeMultiply(identityValue, 1);
            timesZero = FieldUtils.safeMultiply(identityValue, 0);
        } catch (Exception e) {
            return;
        }
        /* Contract/post-condition check: multiplying by 1 must return the original value and multiplying by 0 must return 0.
           A patch that merely deletes/reorders the special-case branch or makes it unreachable can silently return the wrong value
           without throwing; these observable results must still hold for every correct implementation. */
        if (timesOne != identityValue) {
            throw new FuzzerSecurityIssueLow("[oracle:identity-x1] metamorphic violation: safeMultiply(x,1)==x input=" + identityValue + " lhs=" + timesOne + " rhs=" + identityValue);
        }
        if (timesZero != 0L) {
            throw new FuzzerSecurityIssueLow("[oracle:annihilator-x0] metamorphic violation: safeMultiply(x,0)==0 input=" + identityValue + " lhs=" + timesZero + " rhs=0");
        }

        int signSeed;
        try {
            signSeed = data.consumeInt(-1000000, 1000000);
        } catch (Exception e) {
            return;
        }
        long signValue = (long) signSeed;
        long pos;
        long neg;
        try {
            pos = FieldUtils.safeMultiply(signValue, 1);
            neg = FieldUtils.safeMultiply(signValue, -1);
        } catch (Exception e) {
            return;
        }
        if (neg != -pos) {
            throw new FuzzerSecurityIssueLow("[oracle:sign-flip] metamorphic violation: safeMultiply(x,-1)==-safeMultiply(x,1) input=" + signValue + " lhs=" + neg + " rhs=" + (-pos));
        }
    }
}
```
- *[user] message: identical to step [64] — not reprinted*

</details>
<details open><summary>▸ Output (~8,842 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actualLong;

        actualLong = FieldUtils.safeMultiply(0L, 0);
        if (actualLong != 0L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-0x0] semantic mismatch: expected=0 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 1);
        if (actualLong != 1L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-1x1] semantic mismatch: expected=1 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 3);
        if (actualLong != 3L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-1x3] semantic mismatch: expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(3L, 1);
        if (actualLong != 3L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-3x1] semantic mismatch: expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, 3);
        if (actualLong != 6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-2x3] semantic mismatch: expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, -3);
        if (actualLong != -6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-2x-3] semantic mismatch: expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, 3);
        if (actualLong != -6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--2x3] semantic mismatch: expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, -3);
        if (actualLong != 6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--2x-3] semantic mismatch: expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        if (actualLong != (-1L * Integer.MIN_VALUE)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--1xintmin] semantic mismatch: expected=" + (-1L * Integer.MIN_VALUE) + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        if (actualLong != Long.MAX_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxx1] semantic mismatch: expected=" + Long.MAX_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        if (actualLong != Long.MIN_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx1] semantic mismatch: expected=" + Long.MIN_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        if (actualLong != -Long.MAX_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxx-1] semantic mismatch: expected=" + (-Long.MAX_VALUE) + " actual=" + actualLong);
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx-1-throws] semantic mismatch: expected ArithmeticException for FieldUtils.safeMultiply(Long.MIN_VALUE, -1)");
        } catch (ArithmeticException expected) {
        } catch (Throwable e) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx-1-throws] semantic mismatch: expected ArithmeticException but got " + e.getClass().getName(), e);
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx100-throws] semantic mismatch: expected ArithmeticException for FieldUtils.safeMultiply(Long.MIN_VALUE, 100)");
        } catch (ArithmeticException expected) {
        } catch (Throwable e) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx100-throws] semantic mismatch: expected ArithmeticException but got " + e.getClass().getName(), e);
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minxintmax-throws] semantic mismatch: expected ArithmeticException for FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE)");
        } catch (ArithmeticException expected) {
        } catch (Throwable e) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minxintmax-throws] semantic mismatch: expected ArithmeticException but got " + e.getClass().getName(), e);
        }

        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxxintmin-throws] semantic mismatch: expected ArithmeticException for FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE)");
        } catch (ArithmeticException expected) {
        } catch (Throwable e) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxxintmin-throws] semantic mismatch: expected ArithmeticException but got " + e.getClass().getName(), e);
        }

        int fuzzA;
        int fuzzB;
        try {
            fuzzA = data.consumeInt(-46340, 46340);
            fuzzB = data.consumeInt(-46340, 46340);
        } catch (Exception e) {
            return;
        }
        long rel1Left;
        int rel1Right;
        try {
            rel1Left = FieldUtils.safeMultiply((long) fuzzA, fuzzB);
            rel1Right = FieldUtils.safeMultiply(fuzzA, fuzzB);
        } catch (Exception e) {
            return;
        }
        if (rel1Left != (long) rel1Right) {
            throw new FuzzerSecurityIssueLow("relation overload_agreement_longInt_intInt_small_inputs violated: lhs=" + rel1Left + " rhs=" + rel1Right + " a=" + fuzzA + " b=" + fuzzB);
        }

        long fuzzVal1;
        int fuzzVal2;
        try {
            fuzzVal1 = (long) data.consumeInt(-1000000, 1000000);
            fuzzVal2 = data.consumeInt(-1000000, 1000000);
        } catch (Exception e) {
            return;
        }
        long rel2Left;
        long rel2Right;
        try {
            rel2Left = FieldUtils.safeMultiply(fuzzVal1, fuzzVal2);
            rel2Right = FieldUtils.safeMultiply(fuzzVal1, (long) fuzzVal2);
        } catch (Exception e) {
            return;
        }
        if (rel2Left != rel2Right) {
            throw new FuzzerSecurityIssueLow("relation overload_agreement_longInt_longLong_small_inputs violated: lhs=" + rel2Left + " rhs=" + rel2Right + " val1=" + fuzzVal1 + " val2=" + fuzzVal2);
        }

        int identitySeed;
        try {
            identitySeed = data.consumeInt(-1000000, 1000000);
        } catch (Exception e) {
            return;
        }
        long identityValue = (long) identitySeed;
        long timesOne;
        long timesZero;
        try {
            timesOne = FieldUtils.safeMultiply(identityValue, 1);
            timesZero = FieldUtils.safeMultiply(identityValue, 0);
        } catch (Exception e) {
            return;
        }
        /* Contract/post-condition check: safeMultiply(x, 1) returns x and safeMultiply(x, 0) returns 0 by the method's documented behavior and implementation.
           A patch that avoids the throw path by deleting or bypassing the special-case logic could still silently return the wrong long; these observable results must hold. */
        if (timesOne != identityValue) {
            throw new FuzzerSecurityIssueLow("[oracle:identity-x1] metamorphic violation: safeMultiply(x,1)==x input=" + identityValue + " lhs=" + timesOne + " rhs=" + identityValue);
        }
        if (timesZero != 0L) {
            throw new FuzzerSecurityIssueLow("[oracle:annihilator-x0] metamorphic violation: safeMultiply(x,0)==0 input=" + identityValue + " lhs=" + timesZero + " rhs=0");
        }

        int signSeed;
        try {
            signSeed = data.consumeInt(-1000000, 1000000);
        } catch (Exception e) {
            return;
        }
        long signValue = (long) signSeed;
        long pos;
        long neg;
        try {
            pos = FieldUtils.safeMultiply(signValue, 1);
            neg = FieldUtils.safeMultiply(signValue, -1);
        } catch (Exception e) {
            return;
        }
        /* Metamorphic relation: for any non-overflowing input, multiplying by -1 must negate the result of multiplying by 1.
           This uses two real library calls and catches silent wrong-value patches that do not throw. */
        if (neg != -pos) {
            throw new FuzzerSecurityIssueLow("[oracle:sign-flip] metamorphic violation: safeMultiply(x,-1)==-safeMultiply(x,1) input=" + signValue + " lhs=" + neg + " rhs=" + (-pos));
        }
    }
}
```

</details>

---
## [76] ⚙️ harness-attempt · `attempt_018`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-minx-1-throws] semantic mismatch: expected ArithmeticException but got com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow

---
## [77] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [39] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,517 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actual;

        actual = FieldUtils.safeMultiply(0L, 0);
        if (actual != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-0x0] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(1L, 1);
        if (actual != 1L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-1x1] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(1L, 3);
        if (actual != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-1x3] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(3L, 1);
        if (actual != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-3x1] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(2L, 3);
        if (actual != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-2x3] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(2L, -3);
        if (actual != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-2x-3] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-2L, 3);
        if (actual != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed--2x3] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-2L, -3);
        if (actual != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed--2x-3] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        if (actual != (-1L * Integer.MIN_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed--1xminint] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected="
                    + (-1L * Integer.MIN_VALUE) + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        if (actual != Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-maxx1] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected="
                    + Long.MAX_VALUE + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        if (actual != Long.MIN_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-minx1] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected="
                    + Long.MIN_VALUE + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        if (actual != (-Long.MAX_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-maxx-1] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected="
                    + (-Long.MAX_VALUE) + " actual=" + actual);
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-minx-1-throws] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException but returned normally");
        } catch (ArithmeticException expected) {
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-minx100-throws] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) expected ArithmeticException but returned normally");
        } catch (ArithmeticException expected) {
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-minxmaxint-throws] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) expected ArithmeticException but returned normally");
        } catch (ArithmeticException expected) {
        }

        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-maxxminint-throws] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) expected ArithmeticException but returned normally");
        } catch (ArithmeticException expected) {
        }

        int fuzzVal = data.consumeInt(-1_000_000, 1_000_000);
        int fuzzMul = data.consumeInt(-1_000, 1_000);
        long lhs;
        long rhs;
        try {
            lhs = FieldUtils.safeMultiply((long) fuzzVal, fuzzMul);
            rhs = FieldUtils.safeMultiply((long) fuzzVal, (long) fuzzMul);
        } catch (RuntimeException e) {
            return;
        }

        // Contract justification: the same-name overloads safeMultiply(long, int) and
        // safeMultiply(long, long) are both documented as "Multiply two values throwing
        // an exception if overflow occurs." For any non-overflowing int multiplier,
        // both real library calls must therefore produce the same product. A patch that
        // merely deletes the throw or returns a wrapped value in one overload breaks this.
        if (lhs != rhs) {
            throw new RuntimeException(
                "[oracle:overload-equivalence] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) inputVal="
                    + fuzzVal + " inputMul=" + fuzzMul + " lhs=" + lhs + " rhs=" + rhs);
        }

        int identityVal = data.consumeInt(-1_000_000, 1_000_000);
        long identityActual;
        try {
            identityActual = FieldUtils.safeMultiply((long) identityVal, 1);
        } catch (RuntimeException e) {
            return;
        }

        // Contract justification: the method's own implementation and documented purpose
        // guarantee multiplication by 1 returns the original value when no overflow occurs;
        // this is a direct observable post-condition on the API result.
        if (identityActual != (long) identityVal) {
            throw new RuntimeException(
                "[oracle:identity-by-one] metamorphic violation: safeMultiply(x,1) must equal x input="
                    + identityVal + " lhs=" + identityActual + " rhs=" + identityVal);
        }
    }
}
```

</details>

---
## [78] ⚙️ harness-attempt · `attempt_019`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:seed-minx-1-throws] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException but returned normally

---
## [79] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [36] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~10,495 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actualLong;

        // Lifted exact oracles from TestFieldUtils.testSafeMultiplyLongInt.
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
                "[oracle:lifted-8] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected="
                    + (-1L * Integer.MIN_VALUE) + " actual=" + actualLong);
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
        if (actualLong != (-Long.MAX_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected="
                    + (-Long.MAX_VALUE) + " actual=" + actualLong);
        }

        boolean sawArithmetic = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
        } catch (ArithmeticException expected) {
            sawArithmetic = true;
        }
        if (!sawArithmetic) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-12] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException");
        }

        sawArithmetic = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
        } catch (ArithmeticException expected) {
            sawArithmetic = true;
        }
        if (!sawArithmetic) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-13] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) expected ArithmeticException");
        }

        sawArithmetic = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
        } catch (ArithmeticException expected) {
            sawArithmetic = true;
        }
        if (!sawArithmetic) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-14] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) expected ArithmeticException");
        }

        sawArithmetic = false;
        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
        } catch (ArithmeticException expected) {
            sawArithmetic = true;
        }
        if (!sawArithmetic) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-15] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) expected ArithmeticException");
        }

        // Mandatory post-condition / sibling-agreement check.
        // Contract justification: safeMultiply(long,int) and safeMultiply(int,int) both implement safe multiplication;
        // for non-overflowing int inputs they must agree up to widening. A patch that merely suppresses/rewrites
        // the -1 branch can silently return a wrong product, which this observable equality catches without relying on exceptions.
        int a;
        int b;
        try {
            a = data.consumeInt(-46340, 46340);
            b = data.consumeInt(-46340, 46340);
        } catch (Exception e) {
            return;
        }
        long rel1Left;
        int rel1Right;
        try {
            rel1Left = FieldUtils.safeMultiply((long) a, b);
            rel1Right = FieldUtils.safeMultiply(a, b);
        } catch (Exception e) {
            return;
        }
        if (rel1Left != (long) rel1Right) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:overload-longint-intint] metamorphic violation: safeMultiply((long)a,b) must equal widened safeMultiply(a,b) for non-overflowing int inputs inputA="
                    + a + " inputB=" + b + " lhs=" + rel1Left + " rhs=" + rel1Right);
        }

        // Contract justification: safeMultiply(long,int) and safeMultiply(long,long) are documented identically
        // except for the second parameter type; on the same small non-overflowing mathematical inputs they must return
        // the same long result. If one overload silently computes a wrong value, this relation exposes it.
        long val1;
        int val2;
        try {
            val1 = (long) data.consumeInt(-1000000, 1000000);
            val2 = data.consumeInt(-1000000, 1000000);
        } catch (Exception e) {
            return;
        }
        long rel2Left;
        long rel2Right;
        try {
            rel2Left = FieldUtils.safeMultiply(val1, val2);
            rel2Right = FieldUtils.safeMultiply(val1, (long) val2);
        } catch (Exception e) {
            return;
        }
        if (rel2Left != rel2Right) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:overload-longint-longlong] metamorphic violation: safeMultiply(long,int) must equal safeMultiply(long,long) on equivalent small inputs val1="
                    + val1 + " val2=" + val2 + " lhs=" + rel2Left + " rhs=" + rel2Right);
        }

        // Self-invented, patch-focused sibling agreement.
        // Contract justification: for any int x except Integer.MIN_VALUE, safeMultiply((long)x, -1) computes the same
        // mathematical negation as safeNegate(x), just widened to long. This directly exercises the patched -1 branch
        // through the real API and catches a throw-deleting or wrong-value patch even when nothing crashes.
        int x;
        try {
            x = data.consumeInt(Integer.MIN_VALUE + 1, Integer.MAX_VALUE);
        } catch (Exception e) {
            return;
        }
        long mulNeg;
        int neg;
        try {
            mulNeg = FieldUtils.safeMultiply((long) x, -1);
            neg = FieldUtils.safeNegate(x);
        } catch (Exception e) {
            return;
        }
        if (mulNeg != (long) neg) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:negation-sibling] metamorphic violation: safeMultiply((long)x,-1) must equal widened safeNegate(x) for x != Integer.MIN_VALUE x="
                    + x + " lhs=" + mulNeg + " rhs=" + neg);
        }

        // Additional trusted post-condition from the documented special cases.
        // Contract justification: multiplying by 1 is an identity case explicitly implemented in FieldUtils.safeMultiply;
        // deleting or misrouting special-case handling would make this visible immediately.
        long moderate = (long) data.consumeInt(-1000000, 1000000);
        long identity;
        try {
            identity = FieldUtils.safeMultiply(moderate, 1);
        } catch (Exception e) {
            return;
        }
        if (identity != moderate) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:identity-times-one] metamorphic violation: safeMultiply(v,1) must equal v v="
                    + moderate + " actual=" + identity);
        }
    }
}
```

</details>

---
## [80] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `sawArithmetic` (literal assignment, no rethrow, the caught 

---
## [81] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~11,655 chars, 2 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [36] — not reprinted*
**[assistant]**
```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actualLong;

        // Lifted exact oracles from TestFieldUtils.testSafeMultiplyLongInt.
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
                "[oracle:lifted-8] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected="
                    + (-1L * Integer.MIN_VALUE) + " actual=" + actualLong);
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
        if (actualLong != (-Long.MAX_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected="
                    + (-Long.MAX_VALUE) + " actual=" + actualLong);
        }

        boolean sawArithmetic = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
        } catch (ArithmeticException expected) {
            sawArithmetic = true;
        }
        if (!sawArithmetic) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-12] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException");
        }

        sawArithmetic = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
        } catch (ArithmeticException expected) {
            sawArithmetic = true;
        }
        if (!sawArithmetic) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-13] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) expected ArithmeticException");
        }

        sawArithmetic = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
        } catch (ArithmeticException expected) {
            sawArithmetic = true;
        }
        if (!sawArithmetic) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-14] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) expected ArithmeticException");
        }

        sawArithmetic = false;
        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
        } catch (ArithmeticException expected) {
            sawArithmetic = true;
        }
        if (!sawArithmetic) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-15] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) expected ArithmeticException");
        }

        // Mandatory post-condition / sibling-agreement check.
        // Contract justification: safeMultiply(long,int) and safeMultiply(int,int) both implement safe multiplication;
        // for non-overflowing int inputs they must agree up to widening. A patch that merely suppresses/rewrites
        // the -1 branch can silently return a wrong product, which this observable equality catches without relying on exceptions.
        int a;
        int b;
        try {
            a = data.consumeInt(-46340, 46340);
            b = data.consumeInt(-46340, 46340);
        } catch (Exception e) {
            return;
        }
        long rel1Left;
        int rel1Right;
        try {
            rel1Left = FieldUtils.safeMultiply((long) a, b);
            rel1Right = FieldUtils.safeMultiply(a, b);
        } catch (Exception e) {
            return;
        }
        if (rel1Left != (long) rel1Right) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:overload-longint-intint] metamorphic violation: safeMultiply((long)a,b) must equal widened safeMultiply(a,b) for non-overflowing int inputs inputA="
                    + a + " inputB=" + b + " lhs=" + rel1Left + " rhs=" + rel1Right);
        }

        // Contract justification: safeMultiply(long,int) and safeMultiply(long,long) are documented identically
        // except for the second parameter type; on the same small non-overflowing mathematical inputs they must return
        // the same long result. If one overload silently computes a wrong value, this relation exposes it.
        long val1;
        int val2;
        try {
            val1 = (long) data.consumeInt(-1000000, 1000000);
            val2 = data.consumeInt(-1000000, 1000000);
        } catch (Exception e) {
            return;
        }
        long rel2Left;
        long rel2Right;
        try {
            rel2Left = FieldUtils.safeMultiply(val1, val2);
            rel2Right = FieldUtils.safeMultiply(val1, (long) val2);
        } catch (Exception e) {
            return;
        }
        if (rel2Left != rel2Right) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:overload-longint-longlong] metamorphic violation: safeMultiply(long,int) must equal safeMultiply(long,long) on equivalent small inputs val1="
                    + val1 + " val2=" + val2 + " lhs=" + rel2Left + " rhs=" + rel2Right);
        }

        // Self-invented, patch-focused sibling agreement.
        // Contract justification: for any int x except Integer.MIN_VALUE, safeMultiply((long)x, -1) computes the same
        // mathematical negation as safeNegate(x), just widened to long. This directly exercises the patched -1 branch
        // through the real API and catches a throw-deleting or wrong-value patch even when nothing crashes.
        int x;
        try {
            x = data.consumeInt(Integer.MIN_VALUE + 1, Integer.MAX_VALUE);
        } catch (Exception e) {
            return;
        }
        long mulNeg;
        int neg;
        try {
            mulNeg = FieldUtils.safeMultiply((long) x, -1);
            neg = FieldUtils.safeNegate(x);
        } catch (Exception e) {
            return;
        }
        if (mulNeg != (long) neg) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:negation-sibling] metamorphic violation: safeMultiply((long)x,-1) must equal widened safeNegate(x) for x != Integer.MIN_VALUE x="
                    + x + " lhs=" + mulNeg + " rhs=" + neg);
        }

        // Additional trusted post-condition from the documented special cases.
        // Contract justification: multiplying by 1 is an identity case explicitly implemented in FieldUtils.safeMultiply;
        // deleting or misrouting special-case handling would make this visible immediately.
        long moderate = (long) data.consumeInt(-1000000, 1000000);
        long identity;
        try {
            identity = FieldUtils.safeMultiply(moderate, 1);
        } catch (Exception e) {
            return;
        }
        if (identity != moderate) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:identity-times-one] metamorphic violation: safeMultiply(v,1) must equal v v="
                    + moderate + " actual=" + identity);
        }
    }
}
```
**[user]**
```
Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `sawArithmetic` (literal assignment, no rethrow, the caught exception is never used) and the alarm near line 93 is conditioned on `sawArithmetic` — the caught exception's identity is destroyed, so attribution cannot tell WHICH exception fired or whether the unpatched build crashes the same way. Rethrow the exception as the alarm's cause (`..., e`), or assert on the real observable instead of a swallowed boolean.
A caught exception is a REJECTION, never a violation, and flattening it into a flag destroys the identity attribution needs. Either assert on the real observable the test pins (compute the actual value and compare it), or if you must re-report a caught crash, re-throw it with the original as the cause: `new FuzzerSecurityIssueLow("[oracle:<id>] ...", e)`. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~9,535 chars)</summary>

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
                "[oracle:lifted-8] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected="
                    + (-1L * Integer.MIN_VALUE) + " actual=" + actualLong);
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
        if (actualLong != (-Long.MAX_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected="
                    + (-Long.MAX_VALUE) + " actual=" + actualLong);
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

        // Contract justification: safeMultiply(long,int) and safeMultiply(int,int) both implement safe multiplication;
        // for non-overflowing int inputs they must agree up to widening. A patch that silently returns a wrong product
        // instead of throwing is observable through this sibling-agreement post-condition.
        int a;
        int b;
        try {
            a = data.consumeInt(-46340, 46340);
            b = data.consumeInt(-46340, 46340);
        } catch (Exception e) {
            return;
        }
        long rel1Left;
        int rel1Right;
        try {
            rel1Left = FieldUtils.safeMultiply((long) a, b);
            rel1Right = FieldUtils.safeMultiply(a, b);
        } catch (Exception e) {
            return;
        }
        if (rel1Left != (long) rel1Right) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:overload-longint-intint] metamorphic violation: safeMultiply((long)a,b) must equal widened safeMultiply(a,b) for non-overflowing int inputs inputA="
                    + a + " inputB=" + b + " lhs=" + rel1Left + " rhs=" + rel1Right);
        }

        // Contract justification: safeMultiply(long,int) and safeMultiply(long,long) are documented identically
        // apart from the second parameter type; on equivalent small non-overflowing inputs they must return the same result.
        long val1;
        int val2;
        try {
            val1 = (long) data.consumeInt(-1000000, 1000000);
            val2 = data.consumeInt(-1000000, 1000000);
        } catch (Exception e) {
            return;
        }
        long rel2Left;
        long rel2Right;
        try {
            rel2Left = FieldUtils.safeMultiply(val1, val2);
            rel2Right = FieldUtils.safeMultiply(val1, (long) val2);
        } catch (Exception e) {
            return;
        }
        if (rel2Left != rel2Right) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:overload-longint-longlong] metamorphic violation: safeMultiply(long,int) must equal safeMultiply(long,long) on equivalent small inputs val1="
                    + val1 + " val2=" + val2 + " lhs=" + rel2Left + " rhs=" + rel2Right);
        }

        // Contract justification: for any int x except Integer.MIN_VALUE, safeMultiply((long)x, -1) computes the same
        // mathematical negation as safeNegate(x), widened to long. This directly exercises the patched -1 branch.
        int x;
        try {
            x = data.consumeInt(Integer.MIN_VALUE + 1, Integer.MAX_VALUE);
        } catch (Exception e) {
            return;
        }
        long mulNeg;
        int neg;
        try {
            mulNeg = FieldUtils.safeMultiply((long) x, -1);
            neg = FieldUtils.safeNegate(x);
        } catch (Exception e) {
            return;
        }
        if (mulNeg != (long) neg) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:negation-sibling] metamorphic violation: safeMultiply((long)x,-1) must equal widened safeNegate(x) for x != Integer.MIN_VALUE x="
                    + x + " lhs=" + mulNeg + " rhs=" + neg);
        }

        // Contract justification: multiplying by 1 is an identity case explicitly implemented by the public API.
        long moderate;
        try {
            moderate = (long) data.consumeInt(-1000000, 1000000);
        } catch (Exception e) {
            return;
        }
        long identity;
        try {
            identity = FieldUtils.safeMultiply(moderate, 1);
        } catch (Exception e) {
            return;
        }
        if (identity != moderate) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:identity-times-one] metamorphic violation: safeMultiply(v,1) must equal v v="
                    + moderate + " actual=" + identity);
        }
    }
}
```

</details>

---
## [82] ⚙️ harness-attempt · `attempt_021`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-12] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException

---
## [83] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [39] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,800 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long fuzzVal = data.consumeInt(-1_000_000, 1_000_000);
        int fuzzMul = data.consumeInt(-1_000_000, 1_000_000);

        try {
            FieldUtils.safeMultiply(fuzzVal, fuzzMul);
        } catch (ArithmeticException ignored) {
        }

        checkEquals("seed-0x0", 0L, 0L, 0);
        checkEquals("seed-1x1", 1L, 1L, 1);
        checkEquals("seed-1x3", 3L, 1L, 3);
        checkEquals("seed-3x1", 3L, 3L, 1);
        checkEquals("seed-2x3", 6L, 2L, 3);
        checkEquals("seed-2x-3", -6L, 2L, -3);
        checkEquals("seed--2x3", -6L, -2L, 3);
        checkEquals("seed--2x-3", 6L, -2L, -3);
        checkEquals("seed--1xIntMin", -1L * Integer.MIN_VALUE, -1L, Integer.MIN_VALUE);
        checkEquals("seed-maxx1", Long.MAX_VALUE, Long.MAX_VALUE, 1);
        checkEquals("seed-minx1", Long.MIN_VALUE, Long.MIN_VALUE, 1);
        checkEquals("seed-maxx-1", -Long.MAX_VALUE, Long.MAX_VALUE, -1);

        checkThrows("seed-minx-1-throws", Long.MIN_VALUE, -1);
        checkThrows("seed-minx100-throws", Long.MIN_VALUE, 100);
        checkThrows("seed-minxIntMax-throws", Long.MIN_VALUE, Integer.MAX_VALUE);
        checkThrows("seed-maxxIntMin-throws", Long.MAX_VALUE, Integer.MIN_VALUE);

        long a = data.consumeInt(-1_000_000, 1_000_000);
        int b = data.consumeInt(-1_000_000, 1_000_000);
        try {
            long lhs = FieldUtils.safeMultiply(a, b);
            long rhs = FieldUtils.safeMultiply(a, (long) b);
            if (lhs != rhs) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) on the same mathematical inputs input=("
                        + a + "," + b + ") lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (ArithmeticException ignored) {
            return;
        }

        long chosen = data.consumeInt(-1_000_000, 1_000_000);
        try {
            long actual = FieldUtils.safeMultiply(chosen, 1);
            if (actual != chosen) {
                throw new RuntimeException(
                    "[oracle:identity-times-one] metamorphic violation: multiplying by one must return the original value input="
                        + chosen + " lhs=" + actual + " rhs=" + chosen);
            }
        } catch (ArithmeticException ignored) {
        }
    }

    private static void checkEquals(String id, long expected, long val1, int val2) {
        long actual;
        try {
            actual = FieldUtils.safeMultiply(val1, val2);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: expected value " + expected + " but threw "
                    + t.getClass().getName() + " for input=(" + val1 + "," + val2 + ")");
        }
        if (actual != expected) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: expected=" + expected + " actual=" + actual
                    + " input=(" + val1 + "," + val2 + ")");
        }
    }

    private static void checkThrows(String id, long val1, int val2) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: expected ArithmeticException but returned "
                    + actual + " for input=(" + val1 + "," + val2 + ")");
        } catch (ArithmeticException expected) {
        }
    }
}
```

</details>

---
## [84] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Your harness re-throws a caught exception as its own alarm without preserving the original: a FuzzerSecurityIssueLow is thrown inside `catch (Throwable t)` without passing `t` as its cause argument — 

---
## [85] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~4,395 chars, 2 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [39] — not reprinted*
**[assistant]**
```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long fuzzVal = data.consumeInt(-1_000_000, 1_000_000);
        int fuzzMul = data.consumeInt(-1_000_000, 1_000_000);

        try {
            FieldUtils.safeMultiply(fuzzVal, fuzzMul);
        } catch (ArithmeticException ignored) {
        }

        checkEquals("seed-0x0", 0L, 0L, 0);
        checkEquals("seed-1x1", 1L, 1L, 1);
        checkEquals("seed-1x3", 3L, 1L, 3);
        checkEquals("seed-3x1", 3L, 3L, 1);
        checkEquals("seed-2x3", 6L, 2L, 3);
        checkEquals("seed-2x-3", -6L, 2L, -3);
        checkEquals("seed--2x3", -6L, -2L, 3);
        checkEquals("seed--2x-3", 6L, -2L, -3);
        checkEquals("seed--1xIntMin", -1L * Integer.MIN_VALUE, -1L, Integer.MIN_VALUE);
        checkEquals("seed-maxx1", Long.MAX_VALUE, Long.MAX_VALUE, 1);
        checkEquals("seed-minx1", Long.MIN_VALUE, Long.MIN_VALUE, 1);
        checkEquals("seed-maxx-1", -Long.MAX_VALUE, Long.MAX_VALUE, -1);

        checkThrows("seed-minx-1-throws", Long.MIN_VALUE, -1);
        checkThrows("seed-minx100-throws", Long.MIN_VALUE, 100);
        checkThrows("seed-minxIntMax-throws", Long.MIN_VALUE, Integer.MAX_VALUE);
        checkThrows("seed-maxxIntMin-throws", Long.MAX_VALUE, Integer.MIN_VALUE);

        long a = data.consumeInt(-1_000_000, 1_000_000);
        int b = data.consumeInt(-1_000_000, 1_000_000);
        try {
            long lhs = FieldUtils.safeMultiply(a, b);
            long rhs = FieldUtils.safeMultiply(a, (long) b);
            if (lhs != rhs) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) on the same mathematical inputs input=("
                        + a + "," + b + ") lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (ArithmeticException ignored) {
            return;
        }

        long chosen = data.consumeInt(-1_000_000, 1_000_000);
        try {
            long actual = FieldUtils.safeMultiply(chosen, 1);
            if (actual != chosen) {
                throw new RuntimeException(
                    "[oracle:identity-times-one] metamorphic violation: multiplying by one must return the original value input="
                        + chosen + " lhs=" + actual + " rhs=" + chosen);
            }
        } catch (ArithmeticException ignored) {
        }
    }

    private static void checkEquals(String id, long expected, long val1, int val2) {
        long actual;
        try {
            actual = FieldUtils.safeMultiply(val1, val2);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: expected value " + expected + " but threw "
                    + t.getClass().getName() + " for input=(" + val1 + "," + val2 + ")");
        }
        if (actual != expected) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: expected=" + expected + " actual=" + actual
                    + " input=(" + val1 + "," + val2 + ")");
        }
    }

    private static void checkThrows(String id, long val1, int val2) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: expected ArithmeticException but returned "
                    + actual + " for input=(" + val1 + "," + val2 + ")");
        } catch (ArithmeticException expected) {
        }
    }
}
```
**[user]**
```
Your harness re-throws a caught exception as its own alarm without preserving the original: a FuzzerSecurityIssueLow is thrown inside `catch (Throwable t)` without passing `t` as its cause argument — the caught crash's identity is erased and attribution cannot check it against the unpatched build. Use `new FuzzerSecurityIssueLow("<message>", t)`.
Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~3,803 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long fuzzVal = data.consumeInt(-1_000_000, 1_000_000);
        int fuzzMul = data.consumeInt(-1_000_000, 1_000_000);

        try {
            FieldUtils.safeMultiply(fuzzVal, fuzzMul);
        } catch (ArithmeticException ignored) {
        }

        checkEquals("seed-0x0", 0L, 0L, 0);
        checkEquals("seed-1x1", 1L, 1L, 1);
        checkEquals("seed-1x3", 3L, 1L, 3);
        checkEquals("seed-3x1", 3L, 3L, 1);
        checkEquals("seed-2x3", 6L, 2L, 3);
        checkEquals("seed-2x-3", -6L, 2L, -3);
        checkEquals("seed--2x3", -6L, -2L, 3);
        checkEquals("seed--2x-3", 6L, -2L, -3);
        checkEquals("seed--1xIntMin", -1L * Integer.MIN_VALUE, -1L, Integer.MIN_VALUE);
        checkEquals("seed-maxx1", Long.MAX_VALUE, Long.MAX_VALUE, 1);
        checkEquals("seed-minx1", Long.MIN_VALUE, Long.MIN_VALUE, 1);
        checkEquals("seed-maxx-1", -Long.MAX_VALUE, Long.MAX_VALUE, -1);

        checkThrows("seed-minx-1-throws", Long.MIN_VALUE, -1);
        checkThrows("seed-minx100-throws", Long.MIN_VALUE, 100);
        checkThrows("seed-minxIntMax-throws", Long.MIN_VALUE, Integer.MAX_VALUE);
        checkThrows("seed-maxxIntMin-throws", Long.MAX_VALUE, Integer.MIN_VALUE);

        long a = data.consumeInt(-1_000_000, 1_000_000);
        int b = data.consumeInt(-1_000_000, 1_000_000);
        try {
            long lhs = FieldUtils.safeMultiply(a, b);
            long rhs = FieldUtils.safeMultiply(a, (long) b);
            if (lhs != rhs) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) on the same mathematical inputs input=("
                        + a + "," + b + ") lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (ArithmeticException ignored) {
            return;
        }

        long chosen = data.consumeInt(-1_000_000, 1_000_000);
        try {
            long actual = FieldUtils.safeMultiply(chosen, 1);
            if (actual != chosen) {
                throw new RuntimeException(
                    "[oracle:identity-times-one] metamorphic violation: multiplying by one must return the original value input="
                        + chosen + " lhs=" + actual + " rhs=" + chosen);
            }
        } catch (ArithmeticException ignored) {
        }
    }

    private static void checkEquals(String id, long expected, long val1, int val2) {
        long actual;
        try {
            actual = FieldUtils.safeMultiply(val1, val2);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: expected value " + expected + " but threw "
                    + t.getClass().getName() + " for input=(" + val1 + "," + val2 + ")", t);
        }
        if (actual != expected) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: expected=" + expected + " actual=" + actual
                    + " input=(" + val1 + "," + val2 + ")");
        }
    }

    private static void checkThrows(String id, long val1, int val2) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: expected ArithmeticException but returned "
                    + actual + " for input=(" + val1 + "," + val2 + ")");
        } catch (ArithmeticException expected) {
        }
    }
}
```

</details>

---
## [86] ⚙️ harness-attempt · `attempt_023`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:seed-minx-1-throws] semantic mismatch: expected ArithmeticException but returned -9223372036854775808 for input=(-9223372036854775808,-…

---
## [87] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [36] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~11,380 chars)</summary>

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
            throw new FuzzerSecurityIssueLow("[oracle:lifted-0] semantic mismatch: FieldUtils.safeMultiply(0L, 0) threw " + t.getClass().getName(), t);
        }
        if (actualLong != 0L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-0] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(1L, 1);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-1] semantic mismatch: FieldUtils.safeMultiply(1L, 1) threw " + t.getClass().getName(), t);
        }
        if (actualLong != 1L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-1] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(1L, 3);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-2] semantic mismatch: FieldUtils.safeMultiply(1L, 3) threw " + t.getClass().getName(), t);
        }
        if (actualLong != 3L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-2] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(3L, 1);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-3] semantic mismatch: FieldUtils.safeMultiply(3L, 1) threw " + t.getClass().getName(), t);
        }
        if (actualLong != 3L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-3] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(2L, 3);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-4] semantic mismatch: FieldUtils.safeMultiply(2L, 3) threw " + t.getClass().getName(), t);
        }
        if (actualLong != 6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-4] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(2L, -3);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-5] semantic mismatch: FieldUtils.safeMultiply(2L, -3) threw " + t.getClass().getName(), t);
        }
        if (actualLong != -6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-5] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(-2L, 3);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-6] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) threw " + t.getClass().getName(), t);
        }
        if (actualLong != -6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-6] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(-2L, -3);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-7] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) threw " + t.getClass().getName(), t);
        }
        if (actualLong != 6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-7] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-8] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) threw " + t.getClass().getName(), t);
        }
        if (actualLong != (-1L * Integer.MIN_VALUE)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-8] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected=" + (-1L * Integer.MIN_VALUE) + " actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-9] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) threw " + t.getClass().getName(), t);
        }
        if (actualLong != Long.MAX_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-9] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected=" + Long.MAX_VALUE + " actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-10] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) threw " + t.getClass().getName(), t);
        }
        if (actualLong != Long.MIN_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-10] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected=" + Long.MIN_VALUE + " actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) threw " + t.getClass().getName(), t);
        }
        if (actualLong != (-Long.MAX_VALUE)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected=" + (-Long.MAX_VALUE) + " actual=" + actualLong);
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
            throw new FuzzerSecurityIssueLow("[oracle:lifted-reject-0] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) was expected to throw ArithmeticException");
        } catch (ArithmeticException expected) {
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-reject-0] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) threw " + t.getClass().getName() + " instead of ArithmeticException", t);
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
            throw new FuzzerSecurityIssueLow("[oracle:lifted-reject-1] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) was expected to throw ArithmeticException");
        } catch (ArithmeticException expected) {
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-reject-1] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) threw " + t.getClass().getName() + " instead of ArithmeticException", t);
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
            throw new FuzzerSecurityIssueLow("[oracle:lifted-reject-2] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) was expected to throw ArithmeticException");
        } catch (ArithmeticException expected) {
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-reject-2] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) threw " + t.getClass().getName() + " instead of ArithmeticException", t);
        }

        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
            throw new FuzzerSecurityIssueLow("[oracle:lifted-reject-3] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) was expected to throw ArithmeticException");
        } catch (ArithmeticException expected) {
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-reject-3] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) threw " + t.getClass().getName() + " instead of ArithmeticException", t);
        }

        long val1Relation1;
        int val2Relation1;
        try {
            val1Relation1 = (long) data.consumeInt(-1000000, 1000000);
            val2Relation1 = data.consumeInt(-1000000, 1000000);
        } catch (Throwable t) {
            return;
        }
        long relation1Left;
        long relation1Right;
        try {
            relation1Left = FieldUtils.safeMultiply(val1Relation1, val2Relation1);
            relation1Right = FieldUtils.safeMultiply(val1Relation1, (long) val2Relation1);
        } catch (Throwable t) {
            return;
        }
        if (relation1Left != relation1Right) {
            throw new FuzzerSecurityIssueLow("relation overload_agreement_longInt_longLong_small_inputs violated: lhs=" + relation1Left + " rhs=" + relation1Right + " val1=" + val1Relation1 + " val2=" + val2Relation1);
        }

        int aRelation2;
        int bRelation2;
        try {
            aRelation2 = data.consumeInt(-46340, 46340);
            bRelation2 = data.consumeInt(-46340, 46340);
        } catch (Throwable t) {
            return;
        }
        long relation2Left;
        int relation2Right;
        try {
            relation2Left = FieldUtils.safeMultiply((long) aRelation2, bRelation2);
            relation2Right = FieldUtils.safeMultiply(aRelation2, bRelation2);
        } catch (Throwable t) {
            return;
        }
        if (relation2Left != (long) relation2Right) {
            throw new FuzzerSecurityIssueLow("relation overload_agreement_longInt_intInt_small_inputs violated: lhs=" + relation2Left + " rhs=" + relation2Right + " a=" + aRelation2 + " b=" + bRelation2);
        }

        long metamorphicValue;
        try {
            metamorphicValue = (long) data.consumeInt(-1000000, 1000000);
        } catch (Throwable t) {
            return;
        }
        long negResult;
        long posResult;
        long sumResult;
        try {
            /* Contract asserted: for valid non-overflow inputs, multiplying by -1 computes the additive inverse,
               and multiplying by 1 returns the original value. A patch that merely deletes the documented
               ArithmeticException branch or silently returns the wrong value in the -1 case violates this relation
               observably because safeMultiply(x, -1) + safeMultiply(x, 1) must equal 0 for every moderate x. */
            negResult = FieldUtils.safeMultiply(metamorphicValue, -1);
            posResult = FieldUtils.safeMultiply(metamorphicValue, 1);
            sumResult = FieldUtils.safeAdd(negResult, posResult);
        } catch (Throwable t) {
            return;
        }
        if (sumResult != 0L) {
            throw new FuzzerSecurityIssueLow("[oracle:negation-additive-inverse] metamorphic violation: safeMultiply(x,-1) and safeMultiply(x,1) are not additive inverses input=" + metamorphicValue + " lhs=" + negResult + " rhs=" + posResult + " sum=" + sumResult);
        }
    }
}
```

</details>

---
## [88] ⚙️ harness-attempt · `attempt_024`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lifted-reject-0] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) threw com.code_intelligence.jazzer.api.FuzzerSecurityIs…

---
## [89] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [39] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,767 chars)</summary>

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
        if (actual != -Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected=" + (-Long.MAX_VALUE) + " actual=" + actual);
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-12] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException but returned normally");
        } catch (ArithmeticException expected) {
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-13] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) expected ArithmeticException but returned normally");
        } catch (ArithmeticException expected) {
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-14] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) expected ArithmeticException but returned normally");
        } catch (ArithmeticException expected) {
        }

        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-15] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) expected ArithmeticException but returned normally");
        } catch (ArithmeticException expected) {
        }

        long fuzzVal1 = data.consumeInt(-1000000, 1000000);
        int fuzzVal2 = data.consumeInt(-1000000, 1000000);

        try {
            long lhs = FieldUtils.safeMultiply(fuzzVal1, fuzzVal2);
            long rhs = FieldUtils.safeMultiply(fuzzVal1, (long) fuzzVal2);
            if (lhs != rhs) {
                throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) on the same mathematical inputs; both overloads are documented as multiplying two values with overflow checks. input=(" + fuzzVal1 + "," + fuzzVal2 + ") lhs=" + lhs + " rhs=" + rhs);
            }

            if (fuzzVal2 == 1 && lhs != fuzzVal1) {
                throw new RuntimeException("[oracle:identity-one] metamorphic violation: multiplying by 1 must preserve the input per the documented multiplication contract. input=(" + fuzzVal1 + "," + fuzzVal2 + ") lhs=" + lhs + " rhs=" + fuzzVal1);
            }
            if (fuzzVal2 == 0 && lhs != 0L) {
                throw new RuntimeException("[oracle:identity-zero] metamorphic violation: multiplying by 0 must yield 0 per the documented multiplication contract. input=(" + fuzzVal1 + "," + fuzzVal2 + ") lhs=" + lhs + " rhs=0");
            }
            if (fuzzVal2 == -1) {
                long negated = FieldUtils.safeMultiply(-1L, fuzzVal1);
                if (lhs != negated) {
                    throw new RuntimeException("[oracle:commute-neg1] metamorphic violation: multiplication is the same real library operation under operand reordering for the shared long,long overload, so x*(-1) must equal (-1)*x when both calls succeed. input=(" + fuzzVal1 + "," + fuzzVal2 + ") lhs=" + lhs + " rhs=" + negated);
                }
            }
        } catch (ArithmeticException ignored) {
            return;
        }

        int smallA = data.consumeInt(-46340, 46340);
        int smallB = data.consumeInt(-46340, 46340);
        try {
            long viaLongInt = FieldUtils.safeMultiply((long) smallA, smallB);
            int viaIntInt = FieldUtils.safeMultiply(smallA, smallB);
            if (viaLongInt != (long) viaIntInt) {
                throw new RuntimeException("[oracle:int-long-agreement] metamorphic violation: safeMultiply(int,int) and safeMultiply(long,int) are same-name overloads with the same documented semantics, so for moderate inputs where int multiplication cannot overflow they must agree. input=(" + smallA + "," + smallB + ") lhs=" + viaLongInt + " rhs=" + viaIntInt);
            }
        } catch (ArithmeticException ignored) {
            return;
        }
    }
}
```

</details>

---
## [90] ⚙️ harness-attempt · `attempt_025`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:seed-12] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException but returned normally

---
## [91] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [36] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,976 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedOraclePairs();
        checkLiftedOracleRejections();

        // Metamorphic check from equivalent overloads on valid-by-construction small inputs.
        relationOverloadAgreementLongIntLongLongSmallInputs(data);
        relationOverloadAgreementLongIntIntIntSmallInputs(data);

        // Additional trusted metamorphic relation:
        // For non-overflowing small inputs, safe multiplication distributes over safe addition:
        // safeMultiply(x, y + z) == safeAdd(safeMultiply(x, y), safeMultiply(x, z)).
        // This uses only real library calls, keeps magnitudes small by construction,
        // and would catch a patch that avoids throwing by silently returning a wrong product.
        relationDistributiveSmallInputs(data);

        // Drive the patched branch with fuzzed input as well.
        // Trusted oracle by construction:
        // if val2 is 1 or 0, the documented implementation returns val1 or 0 respectively;
        // if val2 is -1 and val1 != Long.MIN_VALUE, the result must be -val1;
        // if val2 is -1 and val1 == Long.MIN_VALUE, overflow must throw.
        long val1 = data.consumeBoolean() ? Long.MIN_VALUE : (long) data.consumeInt();
        int selector = data.consumeInt(0, 3);
        int val2;
        if (selector == 0) {
            val2 = -1;
        } else if (selector == 1) {
            val2 = 0;
        } else if (selector == 2) {
            val2 = 1;
        } else {
            val2 = data.consumeInt(-1000000, 1000000);
        }

        if (val2 == -1) {
            boolean threw = false;
            long actual = 0L;
            try {
                actual = FieldUtils.safeMultiply(val1, val2);
            } catch (ArithmeticException e) {
                threw = true;
            }
            if (val1 == Long.MIN_VALUE) {
                if (!threw) {
                    throw new FuzzerSecurityIssueLow(
                        "[oracle:fuzzed-minus-one-overflow] semantic mismatch: safeMultiply(Long.MIN_VALUE, -1) must throw ArithmeticException but returned " + actual);
                }
            } else {
                if (threw) {
                    return;
                }
                long expected = -val1;
                if (actual != expected) {
                    throw new FuzzerSecurityIssueLow(
                        "[oracle:fuzzed-minus-one-overflow] semantic mismatch: safeMultiply(" + val1 + ", -1) expected " + expected + " but got " + actual);
                }
            }
        } else if (val2 == 0) {
            try {
                long actual = FieldUtils.safeMultiply(val1, 0);
                if (actual != 0L) {
                    throw new FuzzerSecurityIssueLow(
                        "[oracle:fuzzed-zero] semantic mismatch: safeMultiply(" + val1 + ", 0) expected 0 but got " + actual);
                }
            } catch (ArithmeticException e) {
                return;
            }
        } else if (val2 == 1) {
            try {
                long actual = FieldUtils.safeMultiply(val1, 1);
                if (actual != val1) {
                    throw new FuzzerSecurityIssueLow(
                        "[oracle:fuzzed-one] semantic mismatch: safeMultiply(" + val1 + ", 1) expected " + val1 + " but got " + actual);
                }
            } catch (ArithmeticException e) {
                return;
            }
        } else {
            try {
                FieldUtils.safeMultiply(val1, val2);
            } catch (ArithmeticException e) {
                return;
            }
        }
    }

    private static void checkLiftedOraclePairs() {
        assertEqualsLong("[oracle:lifted-0x0]", 0L, FieldUtils.safeMultiply(0L, 0));
        assertEqualsLong("[oracle:lifted-1x1]", 1L, FieldUtils.safeMultiply(1L, 1));
        assertEqualsLong("[oracle:lifted-1x3]", 3L, FieldUtils.safeMultiply(1L, 3));
        assertEqualsLong("[oracle:lifted-3x1]", 3L, FieldUtils.safeMultiply(3L, 1));
        assertEqualsLong("[oracle:lifted-2x3]", 6L, FieldUtils.safeMultiply(2L, 3));
        assertEqualsLong("[oracle:lifted-2x-3]", -6L, FieldUtils.safeMultiply(2L, -3));
        assertEqualsLong("[oracle:lifted--2x3]", -6L, FieldUtils.safeMultiply(-2L, 3));
        assertEqualsLong("[oracle:lifted--2x-3]", 6L, FieldUtils.safeMultiply(-2L, -3));
        assertEqualsLong("[oracle:lifted--1xIntegerMin]", -1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE));
        assertEqualsLong("[oracle:lifted-longMaxx1]", Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1));
        assertEqualsLong("[oracle:lifted-longMinx1]", Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1));
        assertEqualsLong("[oracle:lifted-longMaxx-1]", -Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1));
    }

    private static void checkLiftedOracleRejections() {
        assertArithmeticException("[oracle:lifted-longMinx-1-throws]", Long.MIN_VALUE, -1);
        assertArithmeticException("[oracle:lifted-longMinx100-throws]", Long.MIN_VALUE, 100);
        assertArithmeticException("[oracle:lifted-longMinxIntMax-throws]", Long.MIN_VALUE, Integer.MAX_VALUE);
        assertArithmeticException("[oracle:lifted-longMaxxIntMin-throws]", Long.MAX_VALUE, Integer.MIN_VALUE);
    }

    private static void relationOverloadAgreementLongIntLongLongSmallInputs(FuzzedDataProvider data) {
        long val1;
        int val2;
        try {
            val1 = (long) data.consumeInt(-1000000, 1000000);
            val2 = data.consumeInt(-1000000, 1000000);
        } catch (RuntimeException e) {
            return;
        }

        long r1;
        try {
            r1 = FieldUtils.safeMultiply(val1, val2);
        } catch (Exception e) {
            return;
        }

        long r2;
        try {
            r2 = FieldUtils.safeMultiply(val1, (long) val2);
        } catch (Exception e) {
            return;
        }

        if (r1 != r2) {
            throw new FuzzerSecurityIssueLow(
                "relation overload_agreement_longInt_longLong_small_inputs violated: lhs=" + r1 + " rhs=" + r2 +
                " val1=" + val1 + " val2=" + val2);
        }
    }

    private static void relationOverloadAgreementLongIntIntIntSmallInputs(FuzzedDataProvider data) {
        int a;
        int b;
        try {
            a = data.consumeInt(-46340, 46340);
            b = data.consumeInt(-46340, 46340);
        } catch (RuntimeException e) {
            return;
        }

        long r1;
        try {
            r1 = FieldUtils.safeMultiply((long) a, b);
        } catch (Exception e) {
            return;
        }

        int r2;
        try {
            r2 = FieldUtils.safeMultiply(a, b);
        } catch (Exception e) {
            return;
        }

        if (r1 != (long) r2) {
            throw new FuzzerSecurityIssueLow(
                "relation overload_agreement_longInt_intInt_small_inputs violated: lhs=" + r1 + " rhs=" + r2 +
                " a=" + a + " b=" + b);
        }
    }

    private static void relationDistributiveSmallInputs(FuzzedDataProvider data) {
        long x;
        int y;
        int z;
        try {
            x = (long) data.consumeInt(-10000, 10000);
            y = data.consumeInt(-10000, 10000);
            z = data.consumeInt(-10000, 10000);
        } catch (RuntimeException e) {
            return;
        }

        long lhs;
        long rhs;
        try {
            int yz = FieldUtils.safeAdd(y, z);
            lhs = FieldUtils.safeMultiply(x, yz);
            rhs = FieldUtils.safeAdd(FieldUtils.safeMultiply(x, y), FieldUtils.safeMultiply(x, z));
        } catch (Exception e) {
            return;
        }

        if (lhs != rhs) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:distributive-small] metamorphic violation: safeMultiply(x, safeAdd(y, z)) == safeAdd(safeMultiply(x, y), safeMultiply(x, z)) input=x:" +
                x + ",y:" + y + ",z:" + z + " lhs=" + lhs + " rhs=" + rhs);
        }
    }

    private static void assertEqualsLong(String oracleId, long expected, long actual) {
        if (expected != actual) {
            throw new FuzzerSecurityIssueLow(
                oracleId + " semantic mismatch: expected " + expected + " but got " + actual);
        }
    }

    private static void assertArithmeticException(String oracleId, long val1, int val2) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new FuzzerSecurityIssueLow(
                oracleId + " semantic mismatch: expected ArithmeticException but got return value " + actual);
        } catch (ArithmeticException expected) {
        }
    }
}
```

</details>

---
## [92] ⚙️ harness-repair · `attempt_026`
**output:** **REPAIRED: missing-alarm-id, boolean-swallow**
- detail: {'applied': ['missing-alarm-id', 'boolean-swallow'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [93] ⚙️ harness-attempt · `attempt_026`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: missing-alarm-id, boolean-swallow]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['missing-alarm-id', 'boolean-swallow'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:unnamed-check] [oracle:lifted-longMinx-1-throws] semantic mismatch: expected ArithmeticException but got return value -9223372036854775…

---
## [94] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [39] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~3,455 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkEqual("lift-0x0", 0L, FieldUtils.safeMultiply(0L, 0));
        checkEqual("lift-1x1", 1L, FieldUtils.safeMultiply(1L, 1));
        checkEqual("lift-1x3", 3L, FieldUtils.safeMultiply(1L, 3));
        checkEqual("lift-3x1", 3L, FieldUtils.safeMultiply(3L, 1));
        checkEqual("lift-2x3", 6L, FieldUtils.safeMultiply(2L, 3));
        checkEqual("lift-2x-3", -6L, FieldUtils.safeMultiply(2L, -3));
        checkEqual("lift--2x3", -6L, FieldUtils.safeMultiply(-2L, 3));
        checkEqual("lift--2x-3", 6L, FieldUtils.safeMultiply(-2L, -3));
        checkEqual("lift--1xminInt", -1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE));
        checkEqual("lift-maxx1", Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1));
        checkEqual("lift-minx1", Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1));
        checkEqual("lift-maxx-1", -Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1));

        checkThrowsArithmetic("lift-minx-1-throws", Long.MIN_VALUE, -1);
        checkThrowsArithmetic("lift-minx100-throws", Long.MIN_VALUE, 100);
        checkThrowsArithmetic("lift-minxmaxInt-throws", Long.MIN_VALUE, Integer.MAX_VALUE);
        checkThrowsArithmetic("lift-maxxminInt-throws", Long.MAX_VALUE, Integer.MIN_VALUE);

        int a = data.consumeInt(-1_000_000, 1_000_000);
        int b = data.consumeInt(-1_000_000, 1_000_000);

        try {
            long lhs = FieldUtils.safeMultiply((long) a, b);
            long rhs = FieldUtils.safeMultiply((long) a, (long) b);
            if (lhs != rhs) {
                throw new RuntimeException("[oracle:overload-long-int-vs-long-long] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) on the same mathematical inputs inputA=" + a + " inputB=" + b + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Throwable ignored) {
            return;
        }

        try {
            int lhs = FieldUtils.safeMultiply(a, b);
            long rhs = FieldUtils.safeMultiply((long) a, b);
            if (lhs != rhs) {
                throw new RuntimeException("[oracle:overload-int-vs-long-int] metamorphic violation: safeMultiply(int,int) must agree with safeMultiply(long,int) when the int result is defined inputA=" + a + " inputB=" + b + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Throwable ignored) {
            return;
        }
    }

    private static void checkEqual(String oracleId, long expected, long actual) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: expected=" + expected + " actual=" + actual
            );
        }
    }

    private static void checkThrowsArithmetic(String oracleId, long val1, int val2) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: expected ArithmeticException but returned=" + actual + " for val1=" + val1 + " val2=" + val2
            );
        } catch (ArithmeticException expected) {
        }
    }
}
```

</details>

---
## [95] ⚙️ harness-attempt · `attempt_027`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:lift-minx-1-throws] semantic mismatch: expected ArithmeticException but returned=-9223372036854775808 for val1=-9223372036854775808 val…

---
## [96] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [36] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~9,763 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actual;

        actual = FieldUtils.safeMultiply(0L, 0);
        if (actual != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-00] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(1L, 1);
        if (actual != 1L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-01] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(1L, 3);
        if (actual != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-02] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(3L, 1);
        if (actual != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-03] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(2L, 3);
        if (actual != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-04] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(2L, -3);
        if (actual != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-05] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-2L, 3);
        if (actual != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-06] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-2L, -3);
        if (actual != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-07] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        if (actual != (-1L * Integer.MIN_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-08] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected=" + (-1L * Integer.MIN_VALUE) + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        if (actual != Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-09] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected=" + Long.MAX_VALUE + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        if (actual != Long.MIN_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-10] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected=" + Long.MIN_VALUE + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        if (actual != -Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected=" + (-Long.MAX_VALUE) + " actual=" + actual);
        }

        boolean threw;

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-12] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException but returned normally");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-13] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) expected ArithmeticException but returned normally");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-14] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) expected ArithmeticException but returned normally");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-15] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) expected ArithmeticException but returned normally");
        }

        // Contract guarantee asserted here: the overloads safeMultiply(long,int) and safeMultiply(long,long)
        // document the same operation; on equivalent small non-overflow inputs they must produce the same long.
        long val1 = (long) data.consumeInt(-1000000, 1000000);
        int val2 = data.consumeInt(-1000000, 1000000);
        boolean relation1Applicable = true;
        long r1 = 0L;
        long r2 = 0L;
        try {
            r1 = FieldUtils.safeMultiply(val1, val2);
            r2 = FieldUtils.safeMultiply(val1, (long) val2);
        } catch (Exception e) {
            relation1Applicable = false;
        }
        if (relation1Applicable && r1 != r2) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:rel-overload-longint-longlong] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) on equivalent valid inputs val1=" + val1 + " val2=" + val2 + " lhs=" + r1 + " rhs=" + r2);
        }

        // Contract guarantee asserted here: when the first argument fits in int and the product fits in int,
        // safeMultiply(long,int) and safeMultiply(int,int) perform the same documented multiplication.
        int a = data.consumeInt(-46340, 46340);
        int b = data.consumeInt(-46340, 46340);
        boolean relation2Applicable = true;
        long rr1 = 0L;
        int rr2 = 0;
        try {
            rr1 = FieldUtils.safeMultiply((long) a, b);
            rr2 = FieldUtils.safeMultiply(a, b);
        } catch (Exception e) {
            relation2Applicable = false;
        }
        if (relation2Applicable && rr1 != (long) rr2) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:rel-overload-longint-intint] metamorphic violation: safeMultiply(long,int) must agree with widened safeMultiply(int,int) on equivalent valid inputs a=" + a + " b=" + b + " lhs=" + rr1 + " rhs=" + rr2);
        }

        // Contract guarantee asserted here: multiplication distributes over addition; a throw-deleting or wrong-value
        // patch in safeMultiply/safeAdd would break this observable relation even when nothing throws.
        int x = data.consumeInt(-1000, 1000);
        int y = data.consumeInt(-1000, 1000);
        int z = data.consumeInt(-1000, 1000);
        boolean relation3Applicable = true;
        long lhs = 0L;
        long rhs = 0L;
        try {
            lhs = FieldUtils.safeMultiply((long) x, y + z);
            rhs = FieldUtils.safeAdd(FieldUtils.safeMultiply((long) x, y), FieldUtils.safeMultiply((long) x, z));
        } catch (Exception e) {
            relation3Applicable = false;
        }
        if (relation3Applicable && lhs != rhs) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:rel-distributive] metamorphic violation: safeMultiply must distribute over addition on valid small inputs x=" + x + " y=" + y + " z=" + z + " lhs=" + lhs + " rhs=" + rhs);
        }

        // Contract guarantee asserted here: multiplying by 1 is identity and multiplying by 0 yields 0.
        // This is an observable post-condition on the returned value, so a patch that merely suppresses the
        // exceptional branch or silently computes the wrong result is still caught.
        int idInput = data.consumeInt(-1000000, 1000000);
        long idValue = (long) idInput;
        boolean relation4Applicable = true;
        long timesOne = 0L;
        long timesZero = 0L;
        try {
            timesOne = FieldUtils.safeMultiply(idValue, 1);
            timesZero = FieldUtils.safeMultiply(idValue, 0);
        } catch (Exception e) {
            relation4Applicable = false;
        }
        if (relation4Applicable && timesOne != idValue) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:rel-identity-one] metamorphic violation: safeMultiply(v,1) must equal v for valid input v=" + idValue + " actual=" + timesOne);
        }
        if (relation4Applicable && timesZero != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:rel-identity-zero] metamorphic violation: safeMultiply(v,0) must equal 0 for valid input v=" + idValue + " actual=" + timesZero);
        }
    }
}
```

</details>

---
## [97] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `threw` (literal assignment, no rethrow, the caught exceptio

---
## [98] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~9,963 chars, 1 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [36] — not reprinted*
**[assistant]**
```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actual;

        actual = FieldUtils.safeMultiply(0L, 0);
        if (actual != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-00] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(1L, 1);
        if (actual != 1L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-01] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(1L, 3);
        if (actual != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-02] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(3L, 1);
        if (actual != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-03] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(2L, 3);
        if (actual != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-04] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(2L, -3);
        if (actual != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-05] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-2L, 3);
        if (actual != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-06] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-2L, -3);
        if (actual != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-07] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        if (actual != (-1L * Integer.MIN_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-08] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected=" + (-1L * Integer.MIN_VALUE) + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        if (actual != Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-09] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected=" + Long.MAX_VALUE + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        if (actual != Long.MIN_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-10] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected=" + Long.MIN_VALUE + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        if (actual != -Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected=" + (-Long.MAX_VALUE) + " actual=" + actual);
        }

        boolean threw;

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-12] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException but returned normally");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-13] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) expected ArithmeticException but returned normally");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-14] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) expected ArithmeticException but returned normally");
        }

        threw = false;
        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
        } catch (ArithmeticException expected) {
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-15] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) expected ArithmeticException but returned normally");
        }

        // Contract guarantee asserted here: the overloads safeMultiply(long,int) and safeMultiply(long,long)
        // document the same operation; on equivalent small non-overflow inputs they must produce the same long.
        long val1 = (long) data.consumeInt(-1000000, 1000000);
        int val2 = data.consumeInt(-1000000, 1000000);
        boolean relation1Applicable = true;
        long r1 = 0L;
        long r2 = 0L;
        try {
            r1 = FieldUtils.safeMultiply(val1, val2);
            r2 = FieldUtils.safeMultiply(val1, (long) val2);
        } catch (Exception e) {
            relation1Applicable = false;
        }
        if (relation1Applicable && r1 != r2) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:rel-overload-longint-longlong] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) on equivalent valid inputs val1=" + val1 + " val2=" + val2 + " lhs=" + r1 + " rhs=" + r2);
        }

        // Contract guarantee asserted here: when the first argument fits in int and the product fits in int,
        // safeMultiply(long,int) and safeMultiply(int,int) perform the same documented multiplication.
        int a = data.consumeInt(-46340, 46340);
        int b = data.consumeInt(-46340, 46340);
        boolean relation2Applicable = true;
        long rr1 = 0L;
        int rr2 = 0;
        try {
            rr1 = FieldUtils.safeMultiply((long) a, b);
            rr2 = FieldUtils.safeMultiply(a, b);
        } catch (Exception e) {
            relation2Applicable = false;
        }
        if (relation2Applicable && rr1 != (long) rr2) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:rel-overload-longint-intint] metamorphic violation: safeMultiply(long,int) must agree with widened safeMultiply(int,int) on equivalent valid inputs a=" + a + " b=" + b + " lhs=" + rr1 + " rhs=" + rr2);
        }

        // Contract guarantee asserted here: multiplication distributes over addition; a throw-deleting or wrong-value
        // patch in safeMultiply/safeAdd would break this observable relation even when nothing throws.
        int x = data.consumeInt(-1000, 1000);
        int y = data.consumeInt(-1000, 1000);
        int z = data.consumeInt(-1000, 1000);
        boolean relation3Applicable = true;
        long lhs = 0L;
        long rhs = 0L;
        try {
            lhs = FieldUtils.safeMultiply((long) x, y + z);
            rhs = FieldUtils.safeAdd(FieldUtils.safeMultiply((long) x, y), FieldUtils.safeMultiply((long) x, z));
        } catch (Exception e) {
            relation3Applicable = false;
        }
        if (relation3Applicable && lhs != rhs) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:rel-distributive] metamorphic violation: safeMultiply must distribute over addition on valid small inputs x=" + x + " y=" + y + " z=" + z + " lhs=" + lhs + " rhs=" + rhs);
        }

        // Contract guarantee asserted here: multiplying by 1 is identity and multiplying by 0 yields 0.
        // This is an observable post-condition on the returned value, so a patch that merely suppresses the
        // exceptional branch or silently computes the wrong result is still caught.
        int idInput = data.consumeInt(-1000000, 1000000);
        long idValue = (long) idInput;
        boolean relation4Applicable = true;
        long timesOne = 0L;
        long timesZero = 0L;
        try {
            timesOne = FieldUtils.safeMultiply(idValue, 1);
            timesZero = FieldUtils.safeMultiply(idValue, 0);
        } catch (Exception e) {
            relation4Applicable = false;
        }
        if (relation4Applicable && timesOne != idValue) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:rel-identity-one] metamorphic violation: safeMultiply(v,1) must equal v for valid input v=" + idValue + " actual=" + timesOne);
        }
        if (relation4Applicable && timesZero != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:rel-identity-zero] metamorphic violation: safeMultiply(v,0) must equal 0 for valid input v=" + idValue + " actual=" + timesZero);
        }
    }
}
```
- *[user] message: identical to step [64] — not reprinted*

</details>
<details open><summary>▸ Output (~9,848 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actual;

        actual = FieldUtils.safeMultiply(0L, 0);
        if (actual != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-00] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(1L, 1);
        if (actual != 1L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-01] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(1L, 3);
        if (actual != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-02] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(3L, 1);
        if (actual != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-03] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(2L, 3);
        if (actual != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-04] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(2L, -3);
        if (actual != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-05] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-2L, 3);
        if (actual != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-06] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-2L, -3);
        if (actual != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-07] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        if (actual != (-1L * Integer.MIN_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-08] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected=" + (-1L * Integer.MIN_VALUE) + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        if (actual != Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-09] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected=" + Long.MAX_VALUE + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        if (actual != Long.MIN_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-10] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected=" + Long.MIN_VALUE + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        if (actual != -Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected=" + (-Long.MAX_VALUE) + " actual=" + actual);
        }

        ArithmeticException expectedArithmetic;

        expectedArithmetic = null;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
        } catch (ArithmeticException e) {
            expectedArithmetic = e;
        }
        if (expectedArithmetic == null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-12] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException but returned normally");
        }

        expectedArithmetic = null;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
        } catch (ArithmeticException e) {
            expectedArithmetic = e;
        }
        if (expectedArithmetic == null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-13] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) expected ArithmeticException but returned normally");
        }

        expectedArithmetic = null;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
        } catch (ArithmeticException e) {
            expectedArithmetic = e;
        }
        if (expectedArithmetic == null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-14] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) expected ArithmeticException but returned normally");
        }

        expectedArithmetic = null;
        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
        } catch (ArithmeticException e) {
            expectedArithmetic = e;
        }
        if (expectedArithmetic == null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-15] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) expected ArithmeticException but returned normally");
        }

        // The overloads safeMultiply(long,int) and safeMultiply(long,long) are documented identically
        // apart from parameter type; on equivalent small, non-overflow inputs they must return the same long.
        long val1 = (long) data.consumeInt(-1000000, 1000000);
        int val2 = data.consumeInt(-1000000, 1000000);
        boolean relation1Applicable = true;
        long r1 = 0L;
        long r2 = 0L;
        try {
            r1 = FieldUtils.safeMultiply(val1, val2);
            r2 = FieldUtils.safeMultiply(val1, (long) val2);
        } catch (Exception e) {
            relation1Applicable = false;
        }
        if (relation1Applicable && r1 != r2) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:rel-overload-longint-longlong] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) on equivalent valid inputs val1=" + val1 + " val2=" + val2 + " lhs=" + r1 + " rhs=" + r2);
        }

        // When the first argument fits in int and the product fits in int, safeMultiply(long,int) and
        // safeMultiply(int,int) perform the same documented multiplication, with the int result widened to long.
        int a = data.consumeInt(-46340, 46340);
        int b = data.consumeInt(-46340, 46340);
        boolean relation2Applicable = true;
        long rr1 = 0L;
        int rr2 = 0;
        try {
            rr1 = FieldUtils.safeMultiply((long) a, b);
            rr2 = FieldUtils.safeMultiply(a, b);
        } catch (Exception e) {
            relation2Applicable = false;
        }
        if (relation2Applicable && rr1 != (long) rr2) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:rel-overload-longint-intint] metamorphic violation: safeMultiply(long,int) must agree with widened safeMultiply(int,int) on equivalent valid inputs a=" + a + " b=" + b + " lhs=" + rr1 + " rhs=" + rr2);
        }

        // Multiplication distributes over addition for all exact arithmetic. Using small magnitudes keeps all calls
        // valid by construction, so a wrong-value patch in safeMultiply or a throw-deleting patch is observable here.
        int x = data.consumeInt(-1000, 1000);
        int y = data.consumeInt(-1000, 1000);
        int z = data.consumeInt(-1000, 1000);
        boolean relation3Applicable = true;
        long lhs = 0L;
        long rhs = 0L;
        try {
            lhs = FieldUtils.safeMultiply((long) x, y + z);
            rhs = FieldUtils.safeAdd(FieldUtils.safeMultiply((long) x, y), FieldUtils.safeMultiply((long) x, z));
        } catch (Exception e) {
            relation3Applicable = false;
        }
        if (relation3Applicable && lhs != rhs) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:rel-distributive] metamorphic violation: safeMultiply must distribute over addition on valid small inputs x=" + x + " y=" + y + " z=" + z + " lhs=" + lhs + " rhs=" + rhs);
        }

        // The documented special cases show multiplying by 1 is identity and multiplying by 0 returns 0.
        // This post-condition remains observable even if a patch simply makes an error path unreachable.
        int idInput = data.consumeInt(-1000000, 1000000);
        long idValue = (long) idInput;
        boolean relation4Applicable = true;
        long timesOne = 0L;
        long timesZero = 0L;
        try {
            timesOne = FieldUtils.safeMultiply(idValue, 1);
            timesZero = FieldUtils.safeMultiply(idValue, 0);
        } catch (Exception e) {
            relation4Applicable = false;
        }
        if (relation4Applicable && timesOne != idValue) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:rel-identity-one] metamorphic violation: safeMultiply(v,1) must equal v for valid input v=" + idValue + " actual=" + timesOne);
        }
        if (relation4Applicable && timesZero != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:rel-identity-zero] metamorphic violation: safeMultiply(v,0) must equal 0 for valid input v=" + idValue + " actual=" + timesZero);
        }
    }
}
```

</details>

---
## [99] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `relation1Applicable` (literal assignment, no rethrow, the c

---
## [100] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~11,142 chars, 2 new)</summary>

- *[system] message: identical to step [36] — not reprinted*
- *[user] message: identical to step [36] — not reprinted*
- *[assistant] message: identical to step [98] — not reprinted*
- *[user] message: identical to step [64] — not reprinted*
**[assistant]**
```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actual;

        actual = FieldUtils.safeMultiply(0L, 0);
        if (actual != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-00] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(1L, 1);
        if (actual != 1L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-01] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(1L, 3);
        if (actual != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-02] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(3L, 1);
        if (actual != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-03] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(2L, 3);
        if (actual != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-04] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(2L, -3);
        if (actual != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-05] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-2L, 3);
        if (actual != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-06] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-2L, -3);
        if (actual != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-07] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        if (actual != (-1L * Integer.MIN_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-08] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected=" + (-1L * Integer.MIN_VALUE) + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        if (actual != Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-09] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected=" + Long.MAX_VALUE + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        if (actual != Long.MIN_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-10] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected=" + Long.MIN_VALUE + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        if (actual != -Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected=" + (-Long.MAX_VALUE) + " actual=" + actual);
        }

        ArithmeticException expectedArithmetic;

        expectedArithmetic = null;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
        } catch (ArithmeticException e) {
            expectedArithmetic = e;
        }
        if (expectedArithmetic == null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-12] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException but returned normally");
        }

        expectedArithmetic = null;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
        } catch (ArithmeticException e) {
            expectedArithmetic = e;
        }
        if (expectedArithmetic == null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-13] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) expected ArithmeticException but returned normally");
        }

        expectedArithmetic = null;
        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
        } catch (ArithmeticException e) {
            expectedArithmetic = e;
        }
        if (expectedArithmetic == null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-14] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) expected ArithmeticException but returned normally");
        }

        expectedArithmetic = null;
        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
        } catch (ArithmeticException e) {
            expectedArithmetic = e;
        }
        if (expectedArithmetic == null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-15] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) expected ArithmeticException but returned normally");
        }

        // The overloads safeMultiply(long,int) and safeMultiply(long,long) are documented identically
        // apart from parameter type; on equivalent small, non-overflow inputs they must return the same long.
        long val1 = (long) data.consumeInt(-1000000, 1000000);
        int val2 = data.consumeInt(-1000000, 1000000);
        boolean relation1Applicable = true;
        long r1 = 0L;
        long r2 = 0L;
        try {
            r1 = FieldUtils.safeMultiply(val1, val2);
            r2 = FieldUtils.safeMultiply(val1, (long) val2);
        } catch (Exception e) {
            relation1Applicable = false;
        }
        if (relation1Applicable && r1 != r2) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:rel-overload-longint-longlong] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) on equivalent valid inputs val1=" + val1 + " val2=" + val2 + " lhs=" + r1 + " rhs=" + r2);
        }

        // When the first argument fits in int and the product fits in int, safeMultiply(long,int) and
        // safeMultiply(int,int) perform the same documented multiplication, with the int result widened to long.
        int a = data.consumeInt(-46340, 46340);
        int b = data.consumeInt(-46340, 46340);
        boolean relation2Applicable = true;
        long rr1 = 0L;
        int rr2 = 0;
        try {
            rr1 = FieldUtils.safeMultiply((long) a, b);
            rr2 = FieldUtils.safeMultiply(a, b);
        } catch (Exception e) {
            relation2Applicable = false;
        }
        if (relation2Applicable && rr1 != (long) rr2) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:rel-overload-longint-intint] metamorphic violation: safeMultiply(long,int) must agree with widened safeMultiply(int,int) on equivalent valid inputs a=" + a + " b=" + b + " lhs=" + rr1 + " rhs=" + rr2);
        }

        // Multiplication distributes over addition for all exact arithmetic. Using small magnitudes keeps all calls
        // valid by construction, so a wrong-value patch in safeMultiply or a throw-deleting patch is observable here.
        int x = data.consumeInt(-1000, 1000);
        int y = data.consumeInt(-1000, 1000);
        int z = data.consumeInt(-1000, 1000);
        boolean relation3Applicable = true;
        long lhs = 0L;
        long rhs = 0L;
        try {
            lhs = FieldUtils.safeMultiply((long) x, y + z);
            rhs = FieldUtils.safeAdd(FieldUtils.safeMultiply((long) x, y), FieldUtils.safeMultiply((long) x, z));
        } catch (Exception e) {
            relation3Applicable = false;
        }
        if (relation3Applicable && lhs != rhs) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:rel-distributive] metamorphic violation: safeMultiply must distribute over addition on valid small inputs x=" + x + " y=" + y + " z=" + z + " lhs=" + lhs + " rhs=" + rhs);
        }

        // The documented special cases show multiplying by 1 is identity and multiplying by 0 returns 0.
        // This post-condition remains observable even if a patch simply makes an error path unreachable.
        int idInput = data.consumeInt(-1000000, 1000000);
        long idValue = (long) idInput;
        boolean relation4Applicable = true;
        long timesOne = 0L;
        long timesZero = 0L;
        try {
            timesOne = FieldUtils.safeMultiply(idValue, 1);
            timesZero = FieldUtils.safeMultiply(idValue, 0);
        } catch (Exception e) {
            relation4Applicable = false;
        }
        if (relation4Applicable && timesOne != idValue) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:rel-identity-one] metamorphic violation: safeMultiply(v,1) must equal v for valid input v=" + idValue + " actual=" + timesOne);
        }
        if (relation4Applicable && timesZero != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:rel-identity-zero] metamorphic violation: safeMultiply(v,0) must equal 0 for valid input v=" + idValue + " actual=" + timesZero);
        }
    }
}
```
**[user]**
```
Your harness catches an exception into a bare boolean/flag and then raises its alarm on that flag: a catch block records only the bare flag `relation1Applicable` (literal assignment, no rethrow, the caught exception is never used) and the alarm near line 125 is conditioned on `relation1Applicable` — the caught exception's identity is destroyed, so attribution cannot tell WHICH exception fired or whether the unpatched build crashes the same way. Rethrow the exception as the alarm's cause (`..., e`), or assert on the real observable instead of a swallowed boolean.
A caught exception is a REJECTION, never a violation, and flattening it into a flag destroys the identity attribution needs. Either assert on the real observable the test pins (compute the actual value and compare it), or if you must re-report a caught crash, re-throw it with the original as the cause: `new FuzzerSecurityIssueLow("[oracle:<id>] ...", e)`. Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~8,921 chars)</summary>

```
package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actual;

        actual = FieldUtils.safeMultiply(0L, 0);
        if (actual != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-00] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(1L, 1);
        if (actual != 1L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-01] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(1L, 3);
        if (actual != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-02] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(3L, 1);
        if (actual != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-03] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(2L, 3);
        if (actual != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-04] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(2L, -3);
        if (actual != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-05] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-2L, 3);
        if (actual != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-06] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-2L, -3);
        if (actual != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-07] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        if (actual != (-1L * Integer.MIN_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-08] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected=" + (-1L * Integer.MIN_VALUE) + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        if (actual != Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-09] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected=" + Long.MAX_VALUE + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        if (actual != Long.MIN_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-10] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected=" + Long.MIN_VALUE + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        if (actual != -Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected=" + (-Long.MAX_VALUE) + " actual=" + actual);
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-12] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException but returned normally");
        } catch (ArithmeticException expected) {
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-13] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) expected ArithmeticException but returned normally");
        } catch (ArithmeticException expected) {
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-14] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) expected ArithmeticException but returned normally");
        } catch (ArithmeticException expected) {
        }

        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-15] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) expected ArithmeticException but returned normally");
        } catch (ArithmeticException expected) {
        }

        // The overloads safeMultiply(long,int) and safeMultiply(long,long) are documented identically
        // apart from parameter type; on equivalent small, non-overflow inputs they must return the same long.
        long val1 = (long) data.consumeInt(-1000000, 1000000);
        int val2 = data.consumeInt(-1000000, 1000000);
        long r1;
        long r2;
        try {
            r1 = FieldUtils.safeMultiply(val1, val2);
            r2 = FieldUtils.safeMultiply(val1, (long) val2);
        } catch (Exception e) {
            return;
        }
        if (r1 != r2) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:rel-overload-longint-longlong] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) on equivalent valid inputs val1=" + val1 + " val2=" + val2 + " lhs=" + r1 + " rhs=" + r2);
        }

        // When the first argument fits in int and the product fits in int, safeMultiply(long,int) and
        // safeMultiply(int,int) perform the same documented multiplication, with the int result widened to long.
        int a = data.consumeInt(-46340, 46340);
        int b = data.consumeInt(-46340, 46340);
        long rr1;
        int rr2;
        try {
            rr1 = FieldUtils.safeMultiply((long) a, b);
            rr2 = FieldUtils.safeMultiply(a, b);
        } catch (Exception e) {
            return;
        }
        if (rr1 != (long) rr2) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:rel-overload-longint-intint] metamorphic violation: safeMultiply(long,int) must agree with widened safeMultiply(int,int) on equivalent valid inputs a=" + a + " b=" + b + " lhs=" + rr1 + " rhs=" + rr2);
        }

        // Multiplication distributes over addition for all exact arithmetic. Using small magnitudes keeps all calls
        // valid by construction, so a wrong-value patch in safeMultiply or a throw-deleting patch is observable here.
        int x = data.consumeInt(-1000, 1000);
        int y = data.consumeInt(-1000, 1000);
        int z = data.consumeInt(-1000, 1000);
        long lhs;
        long rhs;
        try {
            lhs = FieldUtils.safeMultiply((long) x, y + z);
            rhs = FieldUtils.safeAdd(FieldUtils.safeMultiply((long) x, y), FieldUtils.safeMultiply((long) x, z));
        } catch (Exception e) {
            return;
        }
        if (lhs != rhs) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:rel-distributive] metamorphic violation: safeMultiply must distribute over addition on valid small inputs x=" + x + " y=" + y + " z=" + z + " lhs=" + lhs + " rhs=" + rhs);
        }

        // The documented special cases show multiplying by 1 is identity and multiplying by 0 returns 0.
        // This post-condition remains observable even if a patch simply makes an error path unreachable.
        int idInput = data.consumeInt(-1000000, 1000000);
        long idValue = (long) idInput;
        long timesOne;
        long timesZero;
        try {
            timesOne = FieldUtils.safeMultiply(idValue, 1);
            timesZero = FieldUtils.safeMultiply(idValue, 0);
        } catch (Exception e) {
            return;
        }
        if (timesOne != idValue) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:rel-identity-one] metamorphic violation: safeMultiply(v,1) must equal v for valid input v=" + idValue + " actual=" + timesOne);
        }
        if (timesZero != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:rel-identity-zero] metamorphic violation: safeMultiply(v,0) must equal 0 for valid input v=" + idValue + " actual=" + timesZero);
        }
    }
}
```

</details>

---
## [101] ⚙️ harness-attempt · `attempt_030`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:seed-12] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException but returned normally

---
## [102] ⚙️ coverage · `jazzer coverage dumps`
**output:** **19/19 dump(s) written under /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/cov; 19 raw fuzz log(s)**
<details><summary>▸ detail (~10,665 chars)</summary>

- detail: {'dumps': [{'harness': 'attempt_001', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/cov/attempt_001_compiled.exec', 'exists': True, 'bytes': 162}, {'harness': 'attempt_002', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/cov/attempt_002_compiled.exec', 'exists': True, 'bytes': 159}, {'harness': 'attempt_003', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/cov/attempt_003_compiled.exec', 'exists': True, 'bytes': 162}, {'harness': 'attempt_004', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/cov/attempt_004_compiled.exec', 'exists': True, 'bytes': 160}, {'harness': 'attempt_006', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/cov/attempt_006_compiled.exec', 'exists': True, 'bytes': 161}, {'harness': 'attempt_008', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/cov/attempt_008_compiled.exec', 'exists': True, 'bytes': 165}, {'harness': 'attempt_011', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/cov/attempt_011_compiled.exec', 'exists': True, 'bytes': 164}, {'harness': 'attempt_013', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/cov/attempt_013_compiled.exec', 'exists': True, 'bytes': 161}, {'harness': 'attempt_014', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/cov/attempt_014_compiled.exec', 'exists': True, 'bytes': 161}, {'harness': 'attempt_016', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/cov/attempt_016_compiled.exec', 'exists': True, 'bytes': 162}, {'harness': 'attempt_018', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/cov/attempt_018_compiled.exec', 'exists': True, 'bytes': 164}, {'harness': 'attempt_019', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/cov/attempt_019_compiled.exec', 'exists': True, 'bytes': 161}, {'harness': 'attempt_021', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/cov/attempt_021_compiled.exec', 'exists': True, 'bytes': 163}, {'harness': 'attempt_023', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/cov/attempt_023_compiled.exec', 'exists': True, 'bytes': 160}, {'harness': 'attempt_024', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/cov/attempt_024_compiled.exec', 'exists': True, 'bytes': 168}, {'harness': 'attempt_025', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/cov/attempt_025_compiled.exec', 'exists': True, 'bytes': 162}, {'harness': 'attempt_026', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/cov/attempt_026_compiled.exec', 'exists': True, 'bytes': 166}, {'harness': 'attempt_027', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/cov/attempt_027_compiled.exec', 'exists': True, 'bytes': 159}, {'harness': 'attempt_030', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/cov/attempt_030_compiled.exec', 'exists': True, 'bytes': 163}], 'outputs': ['/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/fuzz_out/attempt_001_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/fuzz_out/attempt_002_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/fuzz_out/attempt_003_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/fuzz_out/attempt_004_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/fuzz_out/attempt_006_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/fuzz_out/attempt_008_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/fuzz_out/attempt_011_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/fuzz_out/attempt_013_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/fuzz_out/attempt_014_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/fuzz_out/attempt_016_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/fuzz_out/attempt_018_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/fuzz_out/attempt_019_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/fuzz_out/attempt_021_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/fuzz_out/attempt_023_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/fuzz_out/attempt_024_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/fuzz_out/attempt_025_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/fuzz_out/attempt_026_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/fuzz_out/attempt_027_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/fuzz_out/attempt_030_compiled.txt'], 'classpath': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/cov/classpath.json', 'dir': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/cov', 'compiled_attempts': ['attempt_001', 'attempt_002', 'attempt_003', 'attempt_004', 'attempt_006', 'attempt_008', 'attempt_011', 'attempt_013', 'attempt_014', 'attempt_016', 'attempt_018', 'attempt_019', 'attempt_021', 'attempt_023', 'attempt_024', 'attempt_025', 'attempt_026', 'attempt_027', 'attempt_030'], 'accepted_attempts': ['attempt_001', 'attempt_002', 'attempt_003', 'attempt_004', 'attempt_006', 'attempt_008', 'attempt_011', 'attempt_013', 'attempt_014', 'attempt_016', 'attempt_018', 'attempt_019', 'attempt_021', 'attempt_023', 'attempt_024', 'attempt_025', 'attempt_026', 'attempt_027', 'attempt_030'], 'sources': ['/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/harness_src/attempt_001.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/harness_src/attempt_002.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/harness_src/attempt_003.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/harness_src/attempt_004.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/harness_src/attempt_006.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/harness_src/attempt_008.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/harness_src/attempt_011.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/harness_src/attempt_013.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/harness_src/attempt_014.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/harness_src/attempt_016.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/harness_src/attempt_018.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/harness_src/attempt_019.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/harness_src/attempt_021.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/harness_src/attempt_023.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/harness_src/attempt_024.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/harness_src/attempt_025.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/harness_src/attempt_026.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/harness_src/attempt_027.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/004_patch1-Time-15-ACS_c_rep001/harness_src/attempt_030.java']}

</details>
