# Pipeline trace — Chart-8

**Code:** `1ac28e12b4447c6ad51717ff73beb5695a24b71a`

**Patch label:** correct  *(the patch under analysis is a known-CORRECT fix — the pipeline is not told this)*

**Outcome:** candidate_experiment (buggy build only)

**Patch under analysis:**
```diff
--- /source/org/jfree/data/time/Week.java
+++ /source/org/jfree/data/time/Week.java
@@ -172,7 +172,7 @@
      */
     public Week(Date time, TimeZone zone) {
         // defer argument checking...
-        this(time, RegularTimePeriod.DEFAULT_TIME_ZONE, Locale.getDefault());
+       this(time, zone, Locale.getDefault());
     }
 
     /**
```

148 sequential steps — 36 LLM calls, 112 deterministic. Read top to bottom.

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
<details><summary>▸ output — FailureTest(test_class='org.jfree.data.time.junit.WeekTests', test_method='testConstructor', source_ (~1,998 chars)</summary>

**output:** 
```json
[
  "FailureTest(test_class='org.jfree.data.time.junit.WeekTests', test_method='testConstructor', source_path='/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/tests/org/jfree/data/time/junit/WeekTests.java', method_source='    public void testConstructor() {\\n        Locale savedLocale = Locale.getDefault();\\n        TimeZone savedZone = TimeZone.getDefault();\\n        Locale.setDefault(new Locale(\"da\", \"DK\"));\\n        TimeZone.setDefault(TimeZone.getTimeZone(\"Europe/Copenhagen\"));\\n        GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(\\n                TimeZone.getDefault(), Locale.getDefault());\\n\\n        // first day of week is monday\\n        assertEquals(Calendar.MONDAY, cal.getFirstDayOfWeek());\\n        cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);\\n        cal.set(Calendar.MILLISECOND, 0);\\n        Date t = cal.getTime();\\n        Week w = new Week(t, TimeZone.getTimeZone(\"Europe/Copenhagen\"));\\n        assertEquals(34, w.getWeek());\\n\\n        Locale.setDefault(Locale.US);\\n        TimeZone.setDefault(TimeZone.getTimeZone(\"US/Detroit\"));\\n        cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());\\n        // first day of week is Sunday\\n        assertEquals(Calendar.SUNDAY, cal.getFirstDayOfWeek());\\n        cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);\\n        cal.set(Calendar.MILLISECOND, 0);\\n\\n        t = cal.getTime();\\n        w = new Week(t, TimeZone.getTimeZone(\"Europe/Copenhagen\"));\\n        assertEquals(35, w.getWeek());\\n        w = new Week(t, TimeZone.getTimeZone(\"Europe/Copenhagen\"),\\n                new Locale(\"da\", \"DK\"));\\n        assertEquals(34, w.getWeek());\\n\\n        Locale.setDefault(savedLocale);\\n        TimeZone.setDefault(savedZone);\\n    }', exception_type='junit.framework.AssertionFailedError', failure_message=None, support_source=None)"
]
```

</details>

---
## [1] ⚙️ test-context (H1/H2)
**output:** 
```json
[
  {
    "test": "org.jfree.data.time.junit.WeekTests::testConstructor",
    "failure_message": "--- org.jfree.data.time.junit.WeekTests::testConstructor\njunit.framework.AssertionFailedError: expected:<35> but was:<34>",
    "support_chars": 377
  }
]
```

---
## [2] ⚙️ analysis (TargetAnalyzer)
<details><summary>▸ output — modified_files": [ (~1,825 chars)</summary>

**output:** 
```json
{
  "modified_files": [
    "source/org/jfree/data/time/Week.java"
  ],
  "patch_text": "--- /source/org/jfree/data/time/Week.java\n+++ /source/org/jfree/data/time/Week.java\n@@ -172,7 +172,7 @@\n      */\n     public Week(Date time, TimeZone zone) {\n         // defer argument checking...\n-        this(time, RegularTimePeriod.DEFAULT_TIME_ZONE, Locale.getDefault());\n+       this(time, zone, Locale.getDefault());\n     }\n \n     /**\n",
  "functions": [
    {
      "func_name": "Week",
      "func_signature": "public Week(Date time, TimeZone zone)",
      "func_source": "    public Week(Date time, TimeZone zone) {\n        // defer argument checking...\n        this(time, RegularTimePeriod.DEFAULT_TIME_ZONE, Locale.getDefault());\n    }",
      "func_class": "Week",
      "func_class_fq": "org.jfree.data.time.Week",
      "func_param_types": [
        "Date",
        "TimeZone"
      ],
      "fi_name": null,
      "overload_types": [
        [],
        [
          "int",
          "int"
        ],
        [
          "int",
          "Year"
        ],
        [
          "Date"
        ],
        [
          "Date",
          "TimeZone"
        ],
        [
          "Date",
          "TimeZone",
          "Locale"
        ]
      ],
      "xrefs": [],
      "xref_names": [],
      "reachable_edges": [],
      "reachable_depth": {},
      "reachable": [],
      "related_callees": [],
      "field_siblings": []
    }
  ],
  "package": "org.jfree.data.time",
  "root_cause_reachable": [],
  "neighbourhood_notes": [
    "unresolved:Week"
  ],
  "source_imports": [
    "import java.io.Serializable;",
    "import java.util.Calendar;",
    "import java.util.Date;",
    "import java.util.Locale;",
    "import java.util.TimeZone;"
  ],
  "reachable_edges": [],
  "reachable_depth": {}
}
```

</details>

---
## [3] ⚙️ synth · `probe-tier`
**output:** **documented exceptions: {'Week#getFirstMillisecond': ['NullPointerException'], 'Week#getLastMillisecond': ['NullPointerException']}**
- detail: {'documented': {'Week#getFirstMillisecond': ['NullPointerException'], 'Week#getLastMillisecond': ['NullPointerException']}}

---
## [4] 🧠 LLM call — **rule synthesis** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~53,416 chars)</summary>

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
// org.jfree.data.time.junit.WeekTests::testConstructor
    public void testConstructor() {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        Locale.setDefault(new Locale("da", "DK"));
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
        GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                TimeZone.getDefault(), Locale.getDefault());

        // first day of week is monday
        assertEquals(Calendar.MONDAY, cal.getFirstDayOfWeek());
        cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date t = cal.getTime();
        Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
        assertEquals(34, w.getWeek());

        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
        cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());
        // first day of week is Sunday
        assertEquals(Calendar.SUNDAY, cal.getFirstDayOfWeek());
        cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);

        t = cal.getTime();
        w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
        assertEquals(35, w.getWeek());
        w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                new Locale("da", "DK"));
        assertEquals(34, w.getWeek());

        Locale.setDefault(savedLocale);
        TimeZone.setDefault(savedZone);
    }
// On the BUGGY build this test FAILS with (names the diverging observable + wrong value):
// --- org.jfree.data.time.junit.WeekTests::testConstructor
// junit.framework.AssertionFailedError: expected:<35> but was:<34>
// The test DEPENDS on this setup from its test class (helpers, fields, fixtures). A relation that reconstructs the test's scenario must REPLICATE this setup exactly — a relation that cannot must assert only setup-independent properties (counts, kinds, contract formulas), or it will fire on every build including correct ones:
// --- setUp() (test setup) ---
    protected void setUp() {
        this.w1Y1900 = new Week(1, 1900);
        this.w2Y1900 = new Week(2, 1900);
        this.w51Y9999 = new Week(51, 9999);
        this.w52Y9999 = new Week(52, 9999);
    }

// --- class fields/constants the test uses ---
private Week w1Y1900;
private Week w2Y1900;
private Week w51Y9999;
private Week w52Y9999;
SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  getFirstMillisecond() / (Calendar calendar)
  getLastMillisecond() / (Calendar calendar)

METHOD FAMILIES over the same input space (factory/parser siblings — where the docs state a selection or agreement rule between family members, equivalent inputs must respect it):
  get* family: getFirstMillisecond, getLastMillisecond, getSerialIndex, getWeek, getYear, getYearValue

STATE YOU CAN READ (public no-argument readers — capture them BEFORE and AFTER a call documented as read-only or non-mutating; an unexplained change is a hidden-state violation): getFirstMillisecond, getLastMillisecond, getSerialIndex, getWeek, getYear, getYearValue, hashCode, next, previous, toString
</failing_test>
BUGGY method(s) (PRE-PATCH source — this is the code WITH the bug; the failing test above shows where it is wrong). Use it to see the code shape and API, NEVER as a model of correct behaviour:
<code>
    public Week(Date time, TimeZone zone) {
        // defer argument checking...
        this(time, RegularTimePeriod.DEFAULT_TIME_ZONE, Locale.getDefault());
    }
</code>
CLASS-LEVEL CONTEXT — the whole patched class (skeleton: contracts + signatures; full bodies only for patched methods), its supertypes, and key collaborators. The STRONGEST relations usually tie the patched method to something HERE rather than to itself: a sibling method documented to agree or complement it (two accessors that must round-trip; a pair documented to sum to a constant), a constructor-established field invariant the method must respect, or a class/method javadoc guarantee (never null, always positive, canonical order). Prefer such cross-member relations over properties of the patched method in isolation.
<class name="Week" role="patched">
     * A calendar week.  All years are considered to have 53 weeks, numbered from 1
     * to 53, although in many cases the 53rd week is empty.  Most of the time, the
     * 1st week of the year *begins* in the previous calendar year, but it always
     * finishes in the current year (this behaviour matches the workings of the
     * <code>GregorianCalendar</code> class).
     * <P>
     * This class is immutable, which is a requirement for all
     * {@link RegularTimePeriod} subclasses.
public class Week extends RegularTimePeriod implements Serializable {
     * For serialization.
    private static final long serialVersionUID = 1856387786939865061L;
     * Constant for the first week in the year.
    public static final int FIRST_WEEK_IN_YEAR = 1;
     * Constant for the last week in the year.
    public static final int LAST_WEEK_IN_YEAR = 53;
     * The year in which the week falls.
    private short year;
     * The week (1-53).
    private byte week;
     * The first millisecond.
    private long firstMillisecond;
     * The last millisecond.
    private long lastMillisecond;
     * Creates a new time period for the week in which the current system
     * date/time falls.
    public Week() {
        this(new Date());
    }
     * Creates a time period representing the week in the specified year.
     * @param week  the week (1 to 53).
     * @param year  the year (1900 to 9999).
    public Week(int week, int year) {
        if ((week < FIRST_WEEK_IN_YEAR) && (week > LAST_WEEK_IN_YEAR)) {
            throw new IllegalArgumentException(
                    "The 'week' argument must be in the range 1 - 53.");
        }
        this.week = (byte) week;
        this.year = (short) year;
        peg(Calendar.getInstance());
    }
     * Creates a time period representing the week in the specified year.
     * @param week  the week (1 to 53).
     * @param year  the year (1900 to 9999).
    public Week(int week, Year year) {
        if ((week < FIRST_WEEK_IN_YEAR) && (week > LAST_WEEK_IN_YEAR)) {
            throw new IllegalArgumentException(
                    "The 'week' argument must be in the range 1 - 53.");
        }
        this.week = (byte) week;
        this.year = (short) year.getYear();
        peg(Calendar.getInstance());
   }
     * Creates a time period for the week in which the specified date/time
     * falls.
     * @param time  the time (<code>null</code> not permitted).
    public Week(Date time) {
        // defer argument checking...
        this(time, RegularTimePeriod.DEFAULT_TIME_ZONE, Locale.getDefault());
    }
     * Creates a time period for the week in which the specified date/time
     * falls, calculated relative to the specified time zone.
     * @param time  the date/time (<code>null</code> not permitted).
     * @param zone  the time zone (<code>null</code> not permitted).
     * @deprecated As of 1.0.7, use {@link #Week(Date, TimeZone, Locale)}.
    public Week(Date time, TimeZone zone) {
        // defer argument checking...
        this(time, RegularTimePeriod.DEFAULT_TIME_ZONE, Locale.getDefault());
    }
     * Creates a time period for the week in which the specified date/time
     * falls, calculated relative to the specified time zone.
     * @param time  the date/time (<code>null</code> not permitted).
     * @param zone  the time zone (<code>null</code> not permitted).
     * @param locale  the locale (<code>null</code> not permitted).
     * @since 1.0.7
    public Week(Date time, TimeZone zone, Locale locale) {
        if (time == null) {
            throw new IllegalArgumentException("Null 'time' argument.");
        }
        if (zone == null) {
            throw new IllegalArgumentException("Null 'zone' argument.");
        }
        if (locale == null) {
            throw new IllegalArgumentException("Null 'locale' argument.");
        }
        Calendar calendar = Calendar.getInstance(zone, locale);
        calendar.setTime(time);

        // sometimes the last few days of the year are considered to fall in
        // the *first* week of the following year.  Refer to the Javadocs for
        // GregorianCalendar.
        int tempWeek = calendar.get(Calendar.WEEK_OF_YEAR);
        if (tempWeek == 1
                && calendar.get(Calendar.MONTH) == Calendar.DECEMBER) {
            this.week = 1;
            this.year = (short) (calendar.get(Calendar.YEAR) + 1);
        }
        else {
            this.week = (byte) Math.min(tempWeek, LAST_WEEK_IN_YEAR);
            int yyyy = calendar.get(Calendar.YEAR);
            // alternatively, sometimes the first few days of the year are
            // considered to fall in the *last* week of the previous year...
            if (calendar.get(Calendar.MONTH) == Calendar.JANUARY
                    && this.week >= 52) {
                yyyy--;
            }
            this.year = (short) yyyy;
        }
        peg(calendar);
    }
     * Returns the year in which the week falls.
     * @return The year (never <code>null</code>).
    public Year getYear() { … }
     * Returns the year in which the week falls, as an integer value.
     * @return The year.
    public int getYearValue() { … }
     * Returns the week.
     * @return The week.
    public int getWeek() { … }
     * Returns the first millisecond of the week.  This will be determined
     * relative to the time zone specified in the constructor, or in the
     * calendar instance passed in the most recent call to the
     * {@link #peg(Calendar)} method.
     * @return The first millisecond of the week.
     * @see #getLastMillisecond()
    public long getFirstMillisecond() { … }
     * Returns the last millisecond of the week.  This will be
     * determined relative to the time zone specified in the constructor, or
     * in the calendar instance passed in the most recent call to the
     * {@link #peg(Calendar)} method.
     * @return The last millisecond of the week.
     * @see #getFirstMillisecond()
    public long getLastMillisecond() { … }
     * Recalculates the start date/time and end date/time for this time period
     * relative to the supplied calendar (which incorporates a time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @since 1.0.3
    public void peg(Calendar calendar) { … }
     * Returns the week preceding this one.  This method will return
     * <code>null</code> for some lower limit on the range of weeks (currently
     * week 1, 1900).  For week 1 of any year, the previous week is always week
     * 53, but week 53 may not contain any days (you should check for this).
     * @return The preceding week (possibly <code>null</code>).
    public RegularTimePeriod previous() { … }
     * Returns the week following this one.  This method will return
     * <code>null</code> for some upper limit on the range of weeks (currently
     * week 53, 9999).  For week 52 of any year, the following week is always
     * week 53, but week 53 may not contain any days (you should check for
     * this).
     * @return The following week (possibly <code>null</code>).
    public RegularTimePeriod next() { … }
     * Returns a serial index number for the week.
     * @return The serial index number.
    public long getSerialIndex() { … }
     * Returns the first millisecond of the week, evaluated using the supplied
     * calendar (which determines the time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @return The first millisecond of the week.
     * @throws NullPointerException if <code>calendar</code> is
     * <code>null</code>.
    public long getFirstMillisecond(Calendar calendar) { … }
     * Returns a string representing the week (e.g. "Week 9, 2002").
     * TODO: look at internationalisation.
     * @return A string representing the week.
    public String toString() { … }
     * Tests the equality of this Week object to an arbitrary object.  Returns
     * true if the target is a Week instance representing the same week as this
     * object.  In all other cases, returns false.
     * @param obj  the object (<code>null</code> permitted).
     * @return <code>true</code> if week and year of this and object are the
     * same.
    public boolean equals(Object obj) { … }
     * Returns a hash code for this object instance.  The approach described by
     * Joshua Bloch in "Effective Java" has been used here:
     * <p>
     * <code>http://developer.java.sun.com/developer/Books/effectivejava
     * /Chapter3.pdf</code>
     * @return A hash code.
    public int hashCode() { … }
     * Returns an integer indicating the order of this Week object relative to
     * the specified object:
     * negative == before, zero == same, positive == after.
     * @param o1  the object to compare.
     * @return negative == before, zero == same, positive == after.
    public int compareTo(Object o1) { … }
     * Parses the string argument as a week.
     * <P>
     * This method is required to accept the format "YYYY-Wnn".  It will also
     * accept "Wnn-YYYY". Anything else, at the moment, is a bonus.
     * @param s  string to parse.
     * @return <code>null</code> if the string is not parseable, the week
     * otherwise.
    public static Week parseWeek(String s) { … }
     * Finds the first occurrence of ' ', '-', ',' or '.'
     * @param s  the string to parse.
     * @return <code>-1</code> if none of the characters was found, the
     * index of the first occurrence otherwise.
    private static int findSeparator(String s) { … }
     * Creates a year from a string, or returns null (format exceptions
     * suppressed).
     * @param s  string to parse.
     * @return <code>null</code> if the string is not parseable, the year
     * otherwise.
    private static Year evaluateAsYear(String s) { … }
     * Converts a string to a week.
     * @param s  the string to parse.
     * @return <code>-1</code> if the string does not contain a week number,
     * the number of the week otherwise.
    private static int stringToWeek(String s) { … }
}
</class>
<class name="RegularTimePeriod" role="supertype-contract">
     * An abstract class representing a unit of time.  Convenient methods are
     * provided for calculating the next and previous time periods.  Conversion
     * methods are defined that return the first and last milliseconds of the time
     * period.  The results from these methods are timezone dependent.
     * <P>
     * This class is immutable, and all subclasses should be immutable also.
public abstract class RegularTimePeriod implements TimePeriod, Comparable,
                                                   MonthConstants {
     * Creates a time period that includes the specified millisecond, assuming
     * the given time zone.
     * @param c  the time period class.
     * @param millisecond  the time.
     * @param zone  the time zone.
     * @return The time period.
    public static RegularTimePeriod createInstance(Class c, Date millisecond, TimeZone zone) { … }
     * Returns a subclass of {@link RegularTimePeriod} that is smaller than
     * the specified class.
     * @param c  a subclass of {@link RegularTimePeriod}.
     * @return A class.
    public static Class downsize(Class c) { … }
     * Returns the time period preceding this one, or <code>null</code> if some
     * lower limit has been reached.
     * @return The previous time period (possibly <code>null</code>).
    public abstract RegularTimePeriod previous();
     * Returns the time period following this one, or <code>null</code> if some
     * limit has been reached.
     * @return The next time period (possibly <code>null</code>).
    public abstract RegularTimePeriod next();
     * Returns a serial index number for the time unit.
     * @return The serial index number.
    public abstract long getSerialIndex();
     * The default time zone.
    public static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getDefault();
     * Recalculates the start date/time and end date/time for this time period
     * relative to the supplied calendar (which incorporates a time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @since 1.0.3
    public abstract void peg(Calendar calendar);
     * Returns the date/time that marks the start of the time period.  This
     * method returns a new <code>Date</code> instance every time it is called.
     * @return The start date/time.
     * @see #getFirstMillisecond()
    public Date getStart() { … }
     * Returns the date/time that marks the end of the time period.  This
     * method returns a new <code>Date</code> instance every time it is called.
     * @return The end date/time.
     * @see #getLastMillisecond()
    public Date getEnd() { … }
     * Returns the first millisecond of the time period.  This will be
     * determined relative to the time zone specified in the constructor, or
     * in the calendar instance passed in the most recent call to the
     * {@link #peg(Calendar)} method.
     * @return The first millisecond of the time period.
     * @see #getLastMillisecond()
    public abstract long getFirstMillisecond();
     * Returns the first millisecond of the time period, evaluated using the
     * supplied calendar (which incorporates a timezone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @return The first millisecond of the time period.
     * @throws NullPointerException if <code>calendar,/code> is
     * </code>null</code>.
     * @see #getLastMillisecond(Calendar)
    public abstract long getFirstMillisecond(Calendar calendar);
     * Returns the last millisecond of the time period.  This will be
     * determined relative to the time zone specified in the constructor, or
     * in the calendar instance passed in the most recent call to the
     * {@link #peg(Calendar)} method.
     * @return The last millisecond of the time period.
     * @see #getFirstMillisecond()
    public abstract long getLastMillisecond();
     * Returns the last millisecond of the time period, evaluated using the
     * supplied calendar (which incorporates a timezone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @return The last millisecond of the time period.
     * @see #getFirstMillisecond(Calendar)
    public abstract long getLastMillisecond(Calendar calendar);
     * Returns the millisecond closest to the middle of the time period.
     * @return The middle millisecond.
    public long getMiddleMillisecond() { … }
     * Returns the millisecond closest to the middle of the time period,
     * evaluated using the supplied calendar (which incorporates a timezone).
     * @param calendar  the calendar.
     * @return The middle millisecond.
    public long getMiddleMillisecond(Calendar calendar) { … }
     * Returns a string representation of the time period.
     * @return The string.
    public String toString() { … }
}
</class>
<class name="Year" role="collaborator">
     * Represents a year in the range 1900 to 9999.  This class is immutable, which
     * is a requirement for all {@link RegularTimePeriod} subclasses.
public class Year extends RegularTimePeriod implements Serializable {
     * For serialization.
    private static final long serialVersionUID = -7659990929736074836L;
     * The year.
    private short year;
     * The first millisecond.
    private long firstMillisecond;
     * The last millisecond.
    private long lastMillisecond;
     * Creates a new <code>Year</code>, based on the current system date/time.
    public Year() {
        this(new Date());
    }
     * Creates a time period representing a single year.
     * @param year  the year.
    public Year(int year) {
        if ((year < SerialDate.MINIMUM_YEAR_SUPPORTED)
            || (year > SerialDate.MAXIMUM_YEAR_SUPPORTED)) {

            throw new IllegalArgumentException(
                "Year constructor: year (" + year + ") outside valid range.");
        }
        this.year = (short) year;
        peg(Calendar.getInstance());
    }
     * Creates a new <code>Year</code>, based on a particular instant in time,
     * using the default time zone.
     * @param time  the time (<code>null</code> not permitted).
    public Year(Date time) {
        this(time, RegularTimePeriod.DEFAULT_TIME_ZONE);
    }
     * Constructs a year, based on a particular instant in time and a time zone.
     * @param time  the time.
     * @param zone  the time zone.
    public Year(Date time, TimeZone zone) {
        Calendar calendar = Calendar.getInstance(zone);
        calendar.setTime(time);
        this.year = (short) calendar.get(Calendar.YEAR);
        peg(calendar);
    }
     * Returns the year.
     * @return The year.
    public int getYear() { … }
     * Returns the first millisecond of the year.  This will be determined
     * relative to the time zone specified in the constructor, or in the
     * calendar instance passed in the most recent call to the
     * {@link #peg(Calendar)} method.
     * @return The first millisecond of the year.
     * @see #getLastMillisecond()
    public long getFirstMillisecond() { … }
     * Returns the last millisecond of the year.  This will be
     * determined relative to the time zone specified in the constructor, or
     * in the calendar instance passed in the most recent call to the
     * {@link #peg(Calendar)} method.
     * @return The last millisecond of the year.
     * @see #getFirstMillisecond()
    public long getLastMillisecond() { … }
     * Recalculates the start date/time and end date/time for this time period
     * relative to the supplied calendar (which incorporates a time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @since 1.0.3
    public void peg(Calendar calendar) { … }
     * Returns the year preceding this one.
     * @return The year preceding this one (or <code>null</code> if the
     * current year is 1900).
    public RegularTimePeriod previous() { … }
     * Returns the year following this one.
     * @return The year following this one (or <code>null</code> if the current
     * year is 9999).
    public RegularTimePeriod next() { … }
     * Returns a serial index number for the year.
     * <P>
     * The implementation simply returns the year number (e.g. 2002).
     * @return The serial index number.
    public long getSerialIndex() { … }
     * Returns the first millisecond of the year, evaluated using the supplied
     * calendar (which determines the time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @return The first millisecond of the year.
     * @throws NullPointerException if <code>calendar</code> is
     * <code>null</code>.
    public long getFirstMillisecond(Calendar calendar) { … }
     * Returns the last millisecond of the year, evaluated using the supplied
     * calendar (which determines the time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @return The last millisecond of the year.
     * @throws NullPointerException if <code>calendar</code> is
     * <code>null</code>.
    public long getLastMillisecond(Calendar calendar) { … }
     * Tests the equality of this <code>Year</code> object to an arbitrary
     * object.  Returns <code>true</code> if the target is a <code>Year</code>
     * instance representing the same year as this object.  In all other cases,
     * returns <code>false</code>.
     * @param object  the object (<code>null</code> permitted).
     * @return <code>true</code> if the year of this and the object are the
     * same.
    public boolean equals(Object object) { … }
     * Returns a hash code for this object instance.  The approach described by
     * Joshua Bloch in "Effective Java" has been used here:
     * <p>
     * <code>http://developer.java.sun.com/developer/Books/effectivejava
     * /Chapter3.pdf</code>
     * @return A hash code.
    public int hashCode() { … }
     * Returns an integer indicating the order of this <code>Year</code> object
     * relative to the specified object:
     * negative == before, zero == same, positive == after.
     * @param o1  the object to compare.
     * @return negative == before, zero == same, positive == after.
    public int compareTo(Object o1) { … }
     * Returns a string representing the year..
     * @return A string representing the year.
    public String toString() { … }
     * Parses the string argument as a year.
     * <P>
     * The string format is YYYY.
     * @param s  a string representing the year.
     * @return <code>null</code> if the string is not parseable, the year
     * otherwise.
    public static Year parseYear(String s) { … }
}
</class>
<class name="TimePeriodFormatException" role="collaborator">
     * An exception that indicates an invalid format in a string representing a
     * time period.
public class TimePeriodFormatException extends IllegalArgumentException {
     * Creates a new exception.
     * @param message  a message describing the exception.
    public TimePeriodFormatException(String message) {
        super(message);
    }
}
</class>
<class name="TimePeriod" role="collaborator">
     * A period of time measured to millisecond precision using two instances of
     * <code>java.util.Date</code>.
public interface TimePeriod extends Comparable {
     * Returns the start date/time.  This will always be on or before the
     * end date.
     * @return The start date/time (never <code>null</code>).
    public Date getStart();
     * Returns the end date/time.  This will always be on or after the
     * start date.
     * @return The end date/time (never <code>null</code>).
    public Date getEnd();
}
</class>
<class name="JFreeChart" role="collaborator">
     * A chart class implemented using the Java 2D APIs.  The current version
     * supports bar charts, line charts, pie charts and xy plots (including time
     * series data).
     * <P>
     * JFreeChart coordinates several objects to achieve its aim of being able to
     * draw a chart on a Java 2D graphics device: a list of {@link Title} objects
     * (which often includes the chart's legend), a {@link Plot} and a
     * {@link org.jfree.data.general.Dataset} (the plot in turn manages a
     * domain axis and a range axis).
     * <P>
     * You should use a {@link ChartPanel} to display a chart in a GUI.
     * <P>
     * …
public class JFreeChart implements Drawable,
                                   TitleChangeListener,
                                   PlotChangeListener,
                                   Serializable,
                                   Cloneable {
     * For serialization.
    private static final long serialVersionUID = -3470703747817429120L;
     * Information about the project.
    public static final ProjectInfo INFO = new JFreeChartInfo();
     * The default font for titles.
    public static final Font DEFAULT_TITLE_FONT = new Font("SansSerif", Font.BOLD, 18);
     * The default background color.
    public static final Paint DEFAULT_BACKGROUND_PAINT = Color.WHITE;
     * The default background image.
    public static final Image DEFAULT_BACKGROUND_IMAGE = null;
     * The default background image alignment.
    public static final int DEFAULT_BACKGROUND_IMAGE_ALIGNMENT = Align.FIT;
     * The default background image alpha.
    public static final float DEFAULT_BACKGROUND_IMAGE_ALPHA = 0.5f;
     * Rendering hints that will be used for chart drawing.  This should never
     * be <code>null</code>.
    private transient RenderingHints renderingHints;
     * A flag that controls whether or not the chart border is drawn.
    private boolean borderVisible;
     * The stroke used to draw the chart border (if visible).
    private transient Stroke borderStroke;
     * The paint used to draw the chart border (if visible).
    private transient Paint borderPaint;
     * The padding between the chart border and the chart drawing area.
    private RectangleInsets padding;
     * The chart title (optional).
    private TextTitle title;
     * The chart subtitles (zero, one or many).  This field should never be
     * <code>null</code>.
    private List subtitles;
     * Draws the visual representation of the data.
    private Plot plot;
     * Paint used to draw the background of the chart.
    private transient Paint backgroundPaint;
     * An optional background image for the chart.
    private transient Image backgroundImage;
     * The alignment for the background image.
    private int backgroundImageAlignment = Align.FIT;
     * The alpha transparency for the background image.
    private float backgroundImageAlpha = 0.5f;
     * Storage for registered change listeners.
    private transient EventListenerList changeListeners;
     * Storage for registered progress listeners.
    private transient EventListenerList progressListeners;
     * A flag that can be used to enable/disable notification of chart change
     * events.
    private boolean notify;
     * Creates a new chart based on the supplied plot.  The chart will have
     * a legend added automatically, but no title (although you can easily add
     * one later).
     * <br><br>
     * Note that the  {@link ChartFactory} class contains a range
     * of static methods that will return ready-made charts, and often this
     * is a more convenient way to create charts than using this constructor.
     * @param plot  the plot (<code>null</code> not permitted).
    public JFreeChart(Plot plot) {
        this(null, null, plot, true);
    }
     * Creates a new chart with the given title and plot.  A default font
     * (@link DEFAULT_TITLE_FONT) is used for the title, and the chart will
     * have a legend added automatically.
     * <br><br>
     * Note that the  {@link ChartFactory} class contains a range
     * of static methods that will return ready-made charts, and often this
     * is a more convenient way to create charts than using this constructor.
     * @param title  the chart title (<code>null</code> permitted).
     * @param plot  the plot (<code>null</code> not permitted).
    public JFreeChart(String title, Plot plot) {
        this(title, JFreeChart.DEFAULT_TITLE_FONT, plot, true);
    }
     * Creates a new chart with the given title and plot.  The
     * <code>createLegend</code> argument specifies whether or not a legend
     * should be added to the chart.
     * <br><br>
     * Note that the  {@link ChartFactory} class contains a range
     * of static methods that will return ready-made charts, and often this
     * is a more convenient way to create charts than using this constructor.
     * @param title  the chart title (<code>null</code> permitted).
     * @param titleFont  the font for displaying the chart title
     * (<code>null</code> permitted).
     * @param plot  controller of the visual representation of the data
     * (<code>null</code> not permitted).
     * …
    public JFreeChart(String title, Font titleFont, Plot plot, boolean createLegend) { … }
     * Returns the collection of rendering hints for the chart.
     * @return The rendering hints for the chart (never <code>null</code>).
     * @see #setRenderingHints(RenderingHints)
    public RenderingHints getRenderingHints() { … }
     * Sets the rendering hints for the chart.  These will be added (using the
     * Graphics2D.addRenderingHints() method) near the start of the
     * JFreeChart.draw() method.
     * @param renderingHints  the rendering hints (<code>null</code> not
     * permitted).
     * @see #getRenderingHints()
    public void setRenderingHints(RenderingHints renderingHints) { … }
}
// … (non-patched members truncated; patched methods kept)
</class>
<class name="Contributor" role="collaborator">
     * A simple class representing a contributor to a software project.
public class Contributor {
     * The name.
    private String name;
     * The e-mail address.
    private String email;
     * Creates a new contributor.
     * @param name  the name.
     * @param email  the e-mail address.
    public Contributor(String name, String email) {
        this.name = name;
        this.email = email;
    }
     * Returns the contributor's name.
     * @return the contributor's name.
    public String getName() { … }
     * Returns the contributor's e-mail address.
     * @return the contributor's e-mail address.
    public String getEmail() { … }
}
</class>
THE PATCH-CHANGED CLASS: Week. Calls on THIS class are the PROBE tier of the two-tier catch in the output spec: once you have declared the input valid by construction, an exception from one of them is a reportable result, never a rejection to skip. Calls on any OTHER class are setup, where a caught exception still means skip this input.
The patch under analysis (propose relations that the CHANGED expressions/conditions could violate — the boundary values the changed condition tests are where an overfit breaks):
<patch>
--- /source/org/jfree/data/time/Week.java
+++ /source/org/jfree/data/time/Week.java
@@ -172,7 +172,7 @@
      */
     public Week(Date time, TimeZone zone) {
         // defer argument checking...
-        this(time, RegularTimePeriod.DEFAULT_TIME_ZONE, Locale.getDefault());
+       this(time, zone, Locale.getDefault());
     }
 
     /**

</patch>
THE EXACT LINES THE PATCH ADDED/REMOVED (your first relation must target the behaviour THESE govern — the condition, boundary token, or formula here is where an overfit and a correct fix diverge; ADDED lines are what the patch introduced, REMOVED what it deleted — the overfit may have changed these WRONGLY):
    REMOVED: this(time, RegularTimePeriod.DEFAULT_TIME_ZONE, Locale.getDefault());
    ADDED  : this(time, zone, Locale.getDefault());
Documented contract of a touched method (relations must follow from contracts like this):
<javadoc>
A calendar week.  All years are considered to have 53 weeks, numbered from 1
to 53, although in many cases the 53rd week is empty.  Most of the time, the
1st week of the year *begins* in the previous calendar year, but it always
finishes in the current year (this behaviour matches the workings of the
<code>GregorianCalendar</code> class).
<P>
This class is immutable, which is a requirement for all
{@link RegularTimePeriod} subclasses.
/
public class Week extends RegularTimePeriod implements Serializable {

/** For serialization. */
private static final long serialVersionUID = 1856387786939865061L;

/** Constant for the first week in the year. */
public static final int FIRST_WEEK_IN_YEAR = 1;

/** Constant for the last week in the year. */
public static final int LAST_WEEK_IN_YEAR = 53;

/** The year in which the week falls. */
private short year;

/** The week (1-53). */
private byte week;

/** The first millisecond. */
private long firstMillisecond;

/** The last millisecond. */
private long lastMillisecond;

/**
Creates a new time period for the week in which the current system
date/time falls.
</javadoc>
IMPORTS OF THE PATCHED CLASS — the authoritative package for every non-JDK type in this codebase VERSION. When a check references a type, use the package EXACTLY as imported here (or as shown in the skeletons); NEVER guess a package name from library conventions — this version's layout may differ, and a wrong package means the relation cannot compile and is discarded unread:
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
ALSO WORTH CONSTRAINING (secondary): methods/types the failing test exercises — the bug's symptom is observed through these, so when the patch edited a DIFFERENT method than the one whose output is wrong, one relation constraining one of these can catch it. This is an addition to, never a replacement for, relations grounded in the documented contract of the touched code — prefer the documented-contract relation when choosing:
    getDefault, setDefault, getTimeZone, getInstance, getFirstDayOfWeek, set, getTime, getWeek, Locale, Week

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
<details open><summary>▸ Output (~14,569 chars)</summary>

```
[
  {
    "name": "ctor_zone_overload_agrees_with_explicit_locale",
    "kind": "metamorphic",
    "contract": "The deprecated constructor `Week(Date, TimeZone)` is documented to create the week for the specified date/time \"calculated relative to the specified time zone\"; `Week(Date, TimeZone, Locale)` is the non-deprecated sibling with the same semantics plus an explicit locale. Therefore, with `Locale.getDefault()` supplied explicitly, both constructors must produce the same week/year.",
    "input": "Choose a concrete instant by building a `Date` through a `GregorianCalendar` in a fuzz-chosen source time zone, and choose a separate fuzz-chosen target time zone. Set the default locale before probing, because the 2-arg constructor uses `Locale.getDefault()` by contract. All inputs are valid by construction: non-null `Date`, non-null `TimeZone`, non-null default `Locale`.",
    "check": "Locale savedLocale = Locale.getDefault();\nTimeZone savedZone = TimeZone.getDefault();\nDate t;\nTimeZone sourceZone;\nTimeZone targetZone;\nLocale defLocale;\ntry {\n    String[] zoneIds = TimeZone.getAvailableIDs();\n    if (zoneIds.length == 0) {\n        return;\n    }\n    sourceZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);\n    targetZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);\n    defLocale = data.consumeBoolean() ? Locale.US : new Locale(\"da\", \"DK\");\n    Locale.setDefault(defLocale);\n    TimeZone.setDefault(sourceZone);\n    Calendar cal = Calendar.getInstance(sourceZone, defLocale);\n    cal.clear();\n    cal.set(data.consumeInt(1900, 9999), data.consumeInt(0, 11), data.consumeInt(1, 28), data.consumeInt(0, 23), data.consumeInt(0, 59), data.consumeInt(0, 59));\n    cal.set(Calendar.MILLISECOND, data.consumeInt(0, 999));\n    t = cal.getTime();\n} catch (Exception e) {\n    Locale.setDefault(savedLocale);\n    TimeZone.setDefault(savedZone);\n    return;\n}\nWeek a;\ntry {\n    a = new Week(t, targetZone);\n} catch (Exception e) {\n    Locale.setDefault(savedLocale);\n    TimeZone.setDefault(savedZone);\n    throw new RuntimeException(\"relation ctor_zone_overload_agrees_with_explicit_locale violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nWeek b;\ntry {\n    b = new Week(t, targetZone, Locale.getDefault());\n} catch (Exception e) {\n    Locale.setDefault(savedLocale);\n    TimeZone.setDefault(savedZone);\n    throw new RuntimeException(\"relation ctor_zone_overload_agrees_with_explicit_locale violated: unexpected \" + e.getClass().getName() + \" from 3-arg constructor on valid-by-construction input: \" + e.getMessage());\n}\ntry {\n    if (a.getWeek() != b.getWeek() || a.getYearValue() != b.getYearValue()) {\n        throw new RuntimeException(\"relation ctor_zone_overload_agrees_with_explicit_locale violated: 2-arg gave week/year \" + a.getWeek() + \"/\" + a.getYearValue() + \" but 3-arg gave \" + b.getWeek() + \"/\" + b.getYearValue());\n    }\n} finally {\n    Locale.setDefault(savedLocale);\n    TimeZone.setDefault(savedZone);\n}"
  },
  {
    "name": "first_millisecond_matches_calendar_overload_after_peg",
    "kind": "metamorphic",
    "contract": "For `getFirstMillisecond() / getFirstMillisecond(Calendar)`, the no-arg form is documented to be determined relative to the time zone from the constructor or from the calendar passed in the most recent `peg(Calendar)` call, while the overload is evaluated using the supplied calendar. Therefore, after `peg(c)`, both must agree for that same calendar.",
    "input": "Build any valid `Week` from week number 1..53 and year 1900..9999. Build a non-null `Calendar` using a fuzz-chosen available time-zone ID and either `Locale.US` or `da_DK`. Then call `peg(calendar)` before comparing the two getters.",
    "check": "Week w;\nCalendar cal;\ntry {\n    w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));\n    String[] zoneIds = TimeZone.getAvailableIDs();\n    if (zoneIds.length == 0) {\n        return;\n    }\n    TimeZone zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);\n    Locale locale = data.consumeBoolean() ? Locale.US : new Locale(\"da\", \"DK\");\n    cal = Calendar.getInstance(zone, locale);\n    w.peg(cal);\n} catch (Exception e) {\n    return;\n}\nlong a;\ntry {\n    a = w.getFirstMillisecond();\n} catch (Exception e) {\n    throw new RuntimeException(\"relation first_millisecond_matches_calendar_overload_after_peg violated: unexpected \" + e.getClass().getName() + \" from getFirstMillisecond() on valid-by-construction input: \" + e.getMessage());\n}\nlong b;\ntry {\n    b = w.getFirstMillisecond(cal);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation first_millisecond_matches_calendar_overload_after_peg violated: unexpected \" + e.getClass().getName() + \" from getFirstMillisecond(Calendar) on valid-by-construction input: \" + e.getMessage());\n}\nif (a != b) {\n    throw new RuntimeException(\"relation first_millisecond_matches_calendar_overload_after_peg violated: no-arg=\" + a + \" calendar-overload=\" + b);\n}"
  },
  {
    "name": "last_millisecond_matches_calendar_overload_after_peg",
    "kind": "metamorphic",
    "contract": "For `getLastMillisecond() / getLastMillisecond(Calendar)`, the no-arg form is documented to be determined relative to the time zone from the constructor or from the calendar passed in the most recent `peg(Calendar)` call, while the overload is evaluated using the supplied calendar. Therefore, after `peg(c)`, both must agree for that same calendar.",
    "input": "Build any valid `Week` from week number 1..53 and year 1900..9999. Build a non-null `Calendar` using a fuzz-chosen available time-zone ID and either `Locale.US` or `da_DK`. Then call `peg(calendar)` before comparing the two getters.",
    "check": "Week w;\nCalendar cal;\ntry {\n    w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));\n    String[] zoneIds = TimeZone.getAvailableIDs();\n    if (zoneIds.length == 0) {\n        return;\n    }\n    TimeZone zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);\n    Locale locale = data.consumeBoolean() ? Locale.US : new Locale(\"da\", \"DK\");\n    cal = Calendar.getInstance(zone, locale);\n    w.peg(cal);\n} catch (Exception e) {\n    return;\n}\nlong a;\ntry {\n    a = w.getLastMillisecond();\n} catch (Exception e) {\n    throw new RuntimeException(\"relation last_millisecond_matches_calendar_overload_after_peg violated: unexpected \" + e.getClass().getName() + \" from getLastMillisecond() on valid-by-construction input: \" + e.getMessage());\n}\nlong b;\ntry {\n    b = w.getLastMillisecond(cal);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation last_millisecond_matches_calendar_overload_after_peg violated: unexpected \" + e.getClass().getName() + \" from getLastMillisecond(Calendar) on valid-by-construction input: \" + e.getMessage());\n}\nif (a != b) {\n    throw new RuntimeException(\"relation last_millisecond_matches_calendar_overload_after_peg violated: no-arg=\" + a + \" calendar-overload=\" + b);\n}"
  },
  {
    "name": "week_number_within_documented_range",
    "kind": "invariant",
    "contract": "The class defines calendar weeks as numbered from 1 to 53, with constants `FIRST_WEEK_IN_YEAR = 1` and `LAST_WEEK_IN_YEAR = 53`, and the `week` field is documented as \"The week (1-53).\" Therefore `getWeek()` must always return a value in that range for any successfully constructed `Week`.",
    "input": "Construct a `Week` either from a valid `(week, year)` pair or from a valid `Date` plus time zone and locale, chosen by fuzz data. All constructor arguments are within documented valid ranges and non-null.",
    "check": "Week w;\ntry {\n    if (data.consumeBoolean()) {\n        w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));\n    } else {\n        String[] zoneIds = TimeZone.getAvailableIDs();\n        if (zoneIds.length == 0) {\n            return;\n        }\n        TimeZone zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);\n        Locale locale = data.consumeBoolean() ? Locale.US : new Locale(\"da\", \"DK\");\n        Calendar cal = Calendar.getInstance(zone, locale);\n        cal.clear();\n        cal.set(data.consumeInt(1900, 9999), data.consumeInt(0, 11), data.consumeInt(1, 28), data.consumeInt(0, 23), data.consumeInt(0, 59), data.consumeInt(0, 59));\n        cal.set(Calendar.MILLISECOND, data.consumeInt(0, 999));\n        w = new Week(cal.getTime(), zone, locale);\n    }\n} catch (Exception e) {\n    return;\n}\nint week;\ntry {\n    week = w.getWeek();\n} catch (Exception e) {\n    throw new RuntimeException(\"relation week_number_within_documented_range violated: unexpected \" + e.getClass().getName() + \" from getWeek() on valid-by-construction input: \" + e.getMessage());\n}\nif (week < Week.FIRST_WEEK_IN_YEAR || week > Week.LAST_WEEK_IN_YEAR) {\n    throw new RuntimeException(\"relation week_number_within_documented_range violated: getWeek()=\" + week);\n}"
  },
  {
    "name": "year_and_yearValue_agree",
    "kind": "metamorphic",
    "contract": "`getYear()` returns the year in which the week falls as a `Year` object, and `getYearValue()` returns the year as an integer value. For the same `Week`, these are two representations of the same documented year and must agree.",
    "input": "Construct any valid `Week` from a fuzzed valid week/year pair or from a valid date-time/zone/locale combination. Then compare `getYear().getYear()` with `getYearValue()`.",
    "check": "Week w;\ntry {\n    if (data.consumeBoolean()) {\n        w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));\n    } else {\n        String[] zoneIds = TimeZone.getAvailableIDs();\n        if (zoneIds.length == 0) {\n            return;\n        }\n        TimeZone zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);\n        Locale locale = data.consumeBoolean() ? Locale.US : new Locale(\"da\", \"DK\");\n        Calendar cal = Calendar.getInstance(zone, locale);\n        cal.clear();\n        cal.set(data.consumeInt(1900, 9999), data.consumeInt(0, 11), data.consumeInt(1, 28), data.consumeInt(0, 23), data.consumeInt(0, 59), data.consumeInt(0, 59));\n        cal.set(Calendar.MILLISECOND, data.consumeInt(0, 999));\n        w = new Week(cal.getTime(), zone, locale);\n    }\n} catch (Exception e) {\n    return;\n}\nint y1;\ntry {\n    y1 = w.getYearValue();\n} catch (Exception e) {\n    throw new RuntimeException(\"relation year_and_yearValue_agree violated: unexpected \" + e.getClass().getName() + \" from getYearValue() on valid-by-construction input: \" + e.getMessage());\n}\nYear yObj;\ntry {\n    yObj = w.getYear();\n} catch (Exception e) {\n    throw new RuntimeException(\"relation year_and_yearValue_agree violated: unexpected \" + e.getClass().getName() + \" from getYear() on valid-by-construction input: \" + e.getMessage());\n}\nif (yObj == null) {\n    throw new RuntimeException(\"relation year_and_yearValue_agree violated: getYear() returned null\");\n}\nif (yObj.getYear() != y1) {\n    throw new RuntimeException(\"relation year_and_yearValue_agree violated: getYear().getYear()=\" + yObj.getYear() + \" but getYearValue()=\" + y1);\n}"
  },
  {
    "name": "readers_do_not_mutate_week_state",
    "kind": "invariant",
    "contract": "The class is documented as immutable, and the listed no-argument readers (`getWeek`, `getYearValue`, `getFirstMillisecond`, `getLastMillisecond`, `hashCode`, `toString`) are observational methods, so invoking them must not change the represented week/year or the stored millisecond endpoints.",
    "input": "Construct any valid `Week` from a fuzzed valid week/year pair or from a valid date-time/zone/locale combination. Snapshot the publicly readable state, invoke several read-only methods, then snapshot again.",
    "check": "Week w;\ntry {\n    if (data.consumeBoolean()) {\n        w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));\n    } else {\n        String[] zoneIds = TimeZone.getAvailableIDs();\n        if (zoneIds.length == 0) {\n            return;\n        }\n        TimeZone zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);\n        Locale locale = data.consumeBoolean() ? Locale.US : new Locale(\"da\", \"DK\");\n        Calendar cal = Calendar.getInstance(zone, locale);\n        cal.clear();\n        cal.set(data.consumeInt(1900, 9999), data.consumeInt(0, 11), data.consumeInt(1, 28), data.consumeInt(0, 23), data.consumeInt(0, 59), data.consumeInt(0, 59));\n        cal.set(Calendar.MILLISECOND, data.consumeInt(0, 999));\n        w = new Week(cal.getTime(), zone, locale);\n    }\n} catch (Exception e) {\n    return;\n}\nint weekBefore;\nint yearBefore;\nlong firstBefore;\nlong lastBefore;\ntry {\n    weekBefore = w.getWeek();\n    yearBefore = w.getYearValue();\n    firstBefore = w.getFirstMillisecond();\n    lastBefore = w.getLastMillisecond();\n} catch (Exception e) {\n    throw new RuntimeException(\"relation readers_do_not_mutate_week_state violated: unexpected \" + e.getClass().getName() + \" while reading initial state on valid-by-construction input: \" + e.getMessage());\n}\ntry {\n    w.getWeek();\n    w.getYearValue();\n    w.hashCode();\n    w.toString();\n    w.getFirstMillisecond();\n    w.getLastMillisecond();\n} catch (Exception e) {\n    throw new RuntimeException(\"relation readers_do_not_mutate_week_state violated: unexpected \" + e.getClass().getName() + \" from reader on valid-by-construction input: \" + e.getMessage());\n}\nint weekAfter;\nint yearAfter;\nlong firstAfter;\nlong lastAfter;\ntry {\n    weekAfter = w.getWeek();\n    yearAfter = w.getYearValue();\n    firstAfter = w.getFirstMillisecond();\n    lastAfter = w.getLastMillisecond();\n} catch (Exception e) {\n    throw new RuntimeException(\"relation readers_do_not_mutate_week_state violated: unexpected \" + e.getClass().getName() + \" while reading final state on valid-by-construction input: \" + e.getMessage());\n}\nif (weekBefore != weekAfter || yearBefore != yearAfter || firstBefore != firstAfter || lastBefore != lastAfter) {\n    throw new RuntimeException(\"relation readers_do_not_mutate_week_state violated: state changed from week/year/first/last=\" + weekBefore + \"/\" + yearBefore + \"/\" + firstBefore + \"/\" + lastBefore + \" to \" + weekAfter + \"/\" + yearAfter + \"/\" + firstAfter + \"/\" + lastAfter);\n}"
  }
]
```

</details>

---
## [5] ⚙️ screen · `ctor_zone_overload_agrees_with_explicit_locale`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [6] ⚙️ screen-fuzz-buggy · `ctor_zone_overload_agrees_with_explicit_locale`
**output:** 
```json
{
  "checked": 20000,
  "violated": 1502,
  "ratio": 0.0751
}
```

---
## [7] ⚙️ screen · `first_millisecond_matches_calendar_overload_after_peg`
**output:** **tier2-rewritten**
<details><summary>▸ reason (~705 chars)</summary>

- reason: the probe `w.peg(...)` on the patch-changed class `Week` runs inside the try at line 3, whose catch (Exception) swallows every exception and returns. The relation declares this input VALID BY CONSTRUCTION, so an exception raised inside `Week` on it is a RESULT the relation must report, not a rejection to skip — as written the check goes quiet on both builds and a patch that adds or moves a throw is invisible to it. Give the probe its own try (not nested in the setup try) whose catch rethrows `new RuntimeException("relation <name> violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage())`, and keep the blanket catch-and-return for the SETUP calls only.

</details>

---
## [8] ⚙️ screen-fuzz-buggy · `first_millisecond_matches_calendar_overload_after_peg`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [9] ⚙️ screen · `last_millisecond_matches_calendar_overload_after_peg`
**output:** **tier2-rewritten**
<details><summary>▸ reason (~705 chars)</summary>

- reason: the probe `w.peg(...)` on the patch-changed class `Week` runs inside the try at line 3, whose catch (Exception) swallows every exception and returns. The relation declares this input VALID BY CONSTRUCTION, so an exception raised inside `Week` on it is a RESULT the relation must report, not a rejection to skip — as written the check goes quiet on both builds and a patch that adds or moves a throw is invisible to it. Give the probe its own try (not nested in the setup try) whose catch rethrows `new RuntimeException("relation <name> violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage())`, and keep the blanket catch-and-return for the SETUP calls only.

</details>

---
## [10] ⚙️ screen-fuzz-buggy · `last_millisecond_matches_calendar_overload_after_peg`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [11] ⚙️ screen · `week_number_within_documented_range`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [12] ⚙️ screen-fuzz-buggy · `week_number_within_documented_range`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [13] ⚙️ screen · `year_and_yearValue_agree`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [14] ⚙️ screen-fuzz-buggy · `year_and_yearValue_agree`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [15] ⚙️ screen · `readers_do_not_mutate_week_state`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [16] ⚙️ screen-fuzz-buggy · `readers_do_not_mutate_week_state`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [17] ⚙️ screen · `ctor_zone_overload_agrees_with_explicit_locale`
**output:** **kept**
- reason: kept: direction-confirmed

---
## [18] ⚙️ screen · `first_millisecond_matches_calendar_overload_after_peg`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [19] ⚙️ screen · `last_millisecond_matches_calendar_overload_after_peg`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [20] ⚙️ screen · `week_number_within_documented_range`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [21] ⚙️ screen · `year_and_yearValue_agree`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [22] ⚙️ screen · `readers_do_not_mutate_week_state`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [23] ⚙️ screening-survivors
**output:** 
```json
{
  "kept": [
    "ctor_zone_overload_agrees_with_explicit_locale",
    "first_millisecond_matches_calendar_overload_after_peg",
    "last_millisecond_matches_calendar_overload_after_peg",
    "week_number_within_documented_range",
    "year_and_yearValue_agree",
    "readers_do_not_mutate_week_state"
  ],
  "count": 6
}
```

---
## [24] 🧠 LLM call — **rule synthesis** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~160 chars, 0 new)</summary>

- *[system] message: identical to step [4] — not reprinted*
- *[user] message: identical to step [4] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~11,918 chars)</summary>

```
[
  {
    "name": "ctor_date_zone_matches_three_arg_with_default_locale",
    "kind": "metamorphic",
    "contract": "The deprecated constructor `Week(Date, TimeZone)` creates a week for the specified date/time calculated relative to the specified time zone; the non-deprecated sibling `Week(Date, TimeZone, Locale)` does the same with an explicit locale, so with `Locale.getDefault()` they must agree on the represented week and year.",
    "input": "Build a real `Date` whose week number depends on locale/week-definition, and choose a probe time zone distinct from the process default time zone. Replicate the failing-test setup exactly around defaults: first set default locale/time zone to US/Detroit, then construct a `Date` for 2007-08-26 01:00:00 in that default zone, and use `Europe/Copenhagen` as the constructor's explicit zone. This input is valid by construction and directly exercises the changed `zone` argument forwarding.",
    "check": "Locale savedLocale = Locale.getDefault();\nTimeZone savedZone = TimeZone.getDefault();\nDate t;\nTimeZone probeZone;\nWeek w2;\ntry {\n    Locale.setDefault(Locale.US);\n    TimeZone.setDefault(TimeZone.getTimeZone(\"US/Detroit\"));\n    GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());\n    cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);\n    cal.set(Calendar.MILLISECOND, 0);\n    t = cal.getTime();\n    probeZone = TimeZone.getTimeZone(\"Europe/Copenhagen\");\n    w2 = new Week(t, probeZone, Locale.getDefault());\n} catch (Exception e) {\n    Locale.setDefault(savedLocale);\n    TimeZone.setDefault(savedZone);\n    return;\n}\nWeek w1;\ntry {\n    w1 = new Week(t, probeZone);\n} catch (Exception e) {\n    Locale.setDefault(savedLocale);\n    TimeZone.setDefault(savedZone);\n    throw new RuntimeException(\"relation ctor_date_zone_matches_three_arg_with_default_locale violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\ntry {\n    if (w1.getWeek() != w2.getWeek() || w1.getYearValue() != w2.getYearValue()) {\n        throw new RuntimeException(\"relation ctor_date_zone_matches_three_arg_with_default_locale violated: two-arg gave week/year \" + w1.getWeek() + \"/\" + w1.getYearValue() + \" but three-arg gave \" + w2.getWeek() + \"/\" + w2.getYearValue());\n    }\n} finally {\n    Locale.setDefault(savedLocale);\n    TimeZone.setDefault(savedZone);\n}"
  },
  {
    "name": "first_millisecond_overloads_agree",
    "kind": "metamorphic",
    "contract": "The same-name overloads `getFirstMillisecond()` and `getFirstMillisecond(Calendar)` are documented to return the first millisecond of the same week; using a calendar with the same time zone as used to construct the week, the results must agree.",
    "input": "Construct a valid `Week` via `new Week(Date, TimeZone, Locale)` from a fuzzed epoch millisecond, a chosen real time zone (`UTC` or `Europe/Copenhagen`), and a chosen locale (`Locale.US` or `new Locale(\"da\",\"DK\")`). Then evaluate both overloads with a `Calendar` created from that same zone and locale.",
    "check": "Date t;\nTimeZone zone;\nLocale locale;\nWeek w;\nCalendar cal;\ntry {\n    t = new Date(data.consumeLong());\n    zone = TimeZone.getTimeZone(data.consumeBoolean() ? \"UTC\" : \"Europe/Copenhagen\");\n    locale = data.consumeBoolean() ? Locale.US : new Locale(\"da\", \"DK\");\n    w = new Week(t, zone, locale);\n    cal = Calendar.getInstance(zone, locale);\n} catch (Exception e) {\n    return;\n}\nlong a;\ntry {\n    a = w.getFirstMillisecond();\n} catch (Exception e) {\n    throw new RuntimeException(\"relation first_millisecond_overloads_agree violated: unexpected \" + e.getClass().getName() + \" from getFirstMillisecond() on valid-by-construction input: \" + e.getMessage());\n}\nlong b;\ntry {\n    b = w.getFirstMillisecond(cal);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation first_millisecond_overloads_agree violated: unexpected \" + e.getClass().getName() + \" from getFirstMillisecond(Calendar) on valid-by-construction input: \" + e.getMessage());\n}\nif (a != b) {\n    throw new RuntimeException(\"relation first_millisecond_overloads_agree violated: \" + a + \" vs \" + b);\n}"
  },
  {
    "name": "last_millisecond_overloads_agree",
    "kind": "metamorphic",
    "contract": "The same-name overloads `getLastMillisecond()` and `getLastMillisecond(Calendar)` are documented to return the last millisecond of the same week; using a calendar with the same time zone as used to construct the week, the results must agree.",
    "input": "Construct a valid `Week` via `new Week(Date, TimeZone, Locale)` from a fuzzed epoch millisecond, a chosen real time zone (`UTC` or `Europe/Copenhagen`), and a chosen locale (`Locale.US` or `new Locale(\"da\",\"DK\")`). Then evaluate both overloads with a `Calendar` created from that same zone and locale.",
    "check": "Date t;\nTimeZone zone;\nLocale locale;\nWeek w;\nCalendar cal;\ntry {\n    t = new Date(data.consumeLong());\n    zone = TimeZone.getTimeZone(data.consumeBoolean() ? \"UTC\" : \"Europe/Copenhagen\");\n    locale = data.consumeBoolean() ? Locale.US : new Locale(\"da\", \"DK\");\n    w = new Week(t, zone, locale);\n    cal = Calendar.getInstance(zone, locale);\n} catch (Exception e) {\n    return;\n}\nlong a;\ntry {\n    a = w.getLastMillisecond();\n} catch (Exception e) {\n    throw new RuntimeException(\"relation last_millisecond_overloads_agree violated: unexpected \" + e.getClass().getName() + \" from getLastMillisecond() on valid-by-construction input: \" + e.getMessage());\n}\nlong b;\ntry {\n    b = w.getLastMillisecond(cal);\n} catch (Exception e) {\n    throw new RuntimeException(\"relation last_millisecond_overloads_agree violated: unexpected \" + e.getClass().getName() + \" from getLastMillisecond(Calendar) on valid-by-construction input: \" + e.getMessage());\n}\nif (a != b) {\n    throw new RuntimeException(\"relation last_millisecond_overloads_agree violated: \" + a + \" vs \" + b);\n}"
  },
  {
    "name": "week_number_in_documented_range",
    "kind": "invariant",
    "contract": "Class documentation states that weeks are numbered from 1 to 53, with constants `FIRST_WEEK_IN_YEAR = 1` and `LAST_WEEK_IN_YEAR = 53`; therefore `getWeek()` must always be in that range for any valid `Week` instance.",
    "input": "Construct a valid `Week` either from `(week, year)` using fuzzed `week` in `[1,53]` and `year` in `[1900,9999]`, or from `(Date, TimeZone, Locale)` using fuzzed millisecond plus a real zone/locale choice.",
    "check": "Week w;\ntry {\n    if (data.consumeBoolean()) {\n        int week = data.consumeInt(Week.FIRST_WEEK_IN_YEAR, Week.LAST_WEEK_IN_YEAR);\n        int year = data.consumeInt(1900, 9999);\n        w = new Week(week, year);\n    } else {\n        Date t = new Date(data.consumeLong());\n        TimeZone zone = TimeZone.getTimeZone(data.consumeBoolean() ? \"UTC\" : \"Europe/Copenhagen\");\n        Locale locale = data.consumeBoolean() ? Locale.US : new Locale(\"da\", \"DK\");\n        w = new Week(t, zone, locale);\n    }\n} catch (Exception e) {\n    return;\n}\nint weekValue;\ntry {\n    weekValue = w.getWeek();\n} catch (Exception e) {\n    throw new RuntimeException(\"relation week_number_in_documented_range violated: unexpected \" + e.getClass().getName() + \" on valid-by-construction input: \" + e.getMessage());\n}\nif (weekValue < Week.FIRST_WEEK_IN_YEAR || weekValue > Week.LAST_WEEK_IN_YEAR) {\n    throw new RuntimeException(\"relation week_number_in_documented_range violated: getWeek()=\" + weekValue);\n}"
  },
  {
    "name": "year_accessors_agree",
    "kind": "metamorphic",
    "contract": "`getYear()` returns the year in which the week falls and `getYearValue()` returns the year as an integer value, so they must denote the same year.",
    "input": "Construct a valid `Week` from a fuzzed date/time, time zone, and locale, or from fuzzed valid `(week, year)` bounds.",
    "check": "Week w;\ntry {\n    if (data.consumeBoolean()) {\n        Date t = new Date(data.consumeLong());\n        TimeZone zone = TimeZone.getTimeZone(data.consumeBoolean() ? \"UTC\" : \"Europe/Copenhagen\");\n        Locale locale = data.consumeBoolean() ? Locale.US : new Locale(\"da\", \"DK\");\n        w = new Week(t, zone, locale);\n    } else {\n        int week = data.consumeInt(Week.FIRST_WEEK_IN_YEAR, Week.LAST_WEEK_IN_YEAR);\n        int year = data.consumeInt(1900, 9999);\n        w = new Week(week, year);\n    }\n} catch (Exception e) {\n    return;\n}\nYear y;\ntry {\n    y = w.getYear();\n} catch (Exception e) {\n    throw new RuntimeException(\"relation year_accessors_agree violated: unexpected \" + e.getClass().getName() + \" from getYear() on valid-by-construction input: \" + e.getMessage());\n}\nint yv;\ntry {\n    yv = w.getYearValue();\n} catch (Exception e) {\n    throw new RuntimeException(\"relation year_accessors_agree violated: unexpected \" + e.getClass().getName() + \" from getYearValue() on valid-by-construction input: \" + e.getMessage());\n}\nif (y == null || y.getYear() != yv) {\n    throw new RuntimeException(\"relation year_accessors_agree violated: getYear()=\" + (y == null ? \"null\" : String.valueOf(y.getYear())) + \", getYearValue()=\" + yv);\n}"
  },
  {
    "name": "reader_calls_do_not_mutate_week",
    "kind": "invariant",
    "contract": "The class is documented as immutable; therefore read-only observer calls such as `toString()`, `next()`, and `previous()` must not change the receiver's observable state.",
    "input": "Construct a valid `Week` from fuzzed valid `(week, year)` bounds, including boundary years 1900..9999 and weeks 1..53.",
    "check": "Week w;\ntry {\n    int week = data.consumeInt(Week.FIRST_WEEK_IN_YEAR, Week.LAST_WEEK_IN_YEAR);\n    int year = data.consumeInt(1900, 9999);\n    w = new Week(week, year);\n} catch (Exception e) {\n    return;\n}\nint beforeWeek;\nint beforeYear;\nlong beforeFirst;\nlong beforeLast;\nlong beforeSerial;\nint beforeHash;\ntry {\n    beforeWeek = w.getWeek();\n    beforeYear = w.getYearValue();\n    beforeFirst = w.getFirstMillisecond();\n    beforeLast = w.getLastMillisecond();\n    beforeSerial = w.getSerialIndex();\n    beforeHash = w.hashCode();\n} catch (Exception e) {\n    throw new RuntimeException(\"relation reader_calls_do_not_mutate_week violated: unexpected \" + e.getClass().getName() + \" while reading initial state: \" + e.getMessage());\n}\ntry {\n    w.toString();\n    w.next();\n    w.previous();\n} catch (Exception e) {\n    throw new RuntimeException(\"relation reader_calls_do_not_mutate_week violated: unexpected \" + e.getClass().getName() + \" from observer call on valid-by-construction input: \" + e.getMessage());\n}\nint afterWeek;\nint afterYear;\nlong afterFirst;\nlong afterLast;\nlong afterSerial;\nint afterHash;\ntry {\n    afterWeek = w.getWeek();\n    afterYear = w.getYearValue();\n    afterFirst = w.getFirstMillisecond();\n    afterLast = w.getLastMillisecond();\n    afterSerial = w.getSerialIndex();\n    afterHash = w.hashCode();\n} catch (Exception e) {\n    throw new RuntimeException(\"relation reader_calls_do_not_mutate_week violated: unexpected \" + e.getClass().getName() + \" while reading final state: \" + e.getMessage());\n}\nif (beforeWeek != afterWeek || beforeYear != afterYear || beforeFirst != afterFirst || beforeLast != afterLast || beforeSerial != afterSerial || beforeHash != afterHash) {\n    throw new RuntimeException(\"relation reader_calls_do_not_mutate_week violated: state changed from [week=\" + beforeWeek + \", year=\" + beforeYear + \", first=\" + beforeFirst + \", last=\" + beforeLast + \", serial=\" + beforeSerial + \", hash=\" + beforeHash + \"] to [week=\" + afterWeek + \", year=\" + afterYear + \", first=\" + afterFirst + \", last=\" + afterLast + \", serial=\" + afterSerial + \", hash=\" + afterHash + \"]\");\n}"
  }
]
```

</details>

---
## [25] ⚙️ screen · `ctor_date_zone_matches_three_arg_with_default_locale`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [26] 🧠 LLM call — **compile-repair** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~4,668 chars)</summary>

**[system]**
```
You fix a Java snippet that failed to compile. Return ONLY the corrected snippet (the body that runs inside fuzzerTestOneInput, drawing inputs from `data`), no prose, no fences. Keep the SAME property being checked; change only what is needed to compile. Throw the violation with `throw new RuntimeException("relation <name> violated: "+...)` OUTSIDE any catch. Use only real API on the classpath. KEEP THE TWO-TIER CATCH the snippet uses and do NOT introduce a blanket `catch (Exception e) { return; }` around a call on the patch-changed class: setup exceptions are rejections to skip, but an exception from the probe on a valid-by-construction input is the violation and must be re-thrown as `new RuntimeException("relation <name> violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage())`.
```
**[user]**
```
Property (keep it): The deprecated constructor `Week(Date, TimeZone)` creates a week for the specified date/time calculated relative to the specified time zone; the non-deprecated sibling `Week(Date, TimeZone, Locale)` does the same with an explicit locale, so with `Locale.getDefault()` they must agree on the represented week and year.
Available imports:
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

Snippet that failed:
Locale savedLocale = Locale.getDefault();
TimeZone savedZone = TimeZone.getDefault();
Date t;
TimeZone probeZone;
Week w2;
try {
    Locale.setDefault(Locale.US);
    TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
    GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());
    cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
    cal.set(Calendar.MILLISECOND, 0);
    t = cal.getTime();
    probeZone = TimeZone.getTimeZone("Europe/Copenhagen");
    w2 = new Week(t, probeZone, Locale.getDefault());
} catch (Exception e) {
    Locale.setDefault(savedLocale);
    TimeZone.setDefault(savedZone);
    return;
}
Week w1;
try {
    w1 = new Week(t, probeZone);
} catch (Exception e) { for (StackTraceElement __rpgf : e.getStackTrace()) { String __rpgc = __rpgf.getClassName(); if ((__rpgc.equals("Week") || __rpgc.endsWith(".Week") || __rpgc.endsWith("$Week")) && !"<init>".equals(__rpgf.getMethodName())) { boolean __rpgdoc = false; if (!__rpgdoc && (__rpgf.getClassName().equals("Week") || __rpgf.getClassName().endsWith(".Week") || __rpgf.getClassName().endsWith("$Week")) && "getFirstMillisecond".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("NullPointerException") || __rpn.endsWith(".NullPointerException") || __rpn.endsWith("$NullPointerException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("Week") || __rpgf.getClassName().endsWith(".Week") || __rpgf.getClassName().endsWith("$Week")) && "getLastMillisecond".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("NullPointerException") || __rpn.endsWith(".NullPointerException") || __rpn.endsWith("$NullPointerException")) { __rpgdoc = true; break; } } } if (__rpgdoc) return; break; } }
    Locale.setDefault(savedLocale);
    TimeZone.setDefault(savedZone);
    throw new RuntimeException("relation ctor_date_zone_matches_three_arg_with_default_locale violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage());
}
try {
    if (w1.getWeek() != w2.getWeek() || w1.getYearValue() != w2.getYearValue()) {
        throw new RuntimeException("relation ctor_date_zone_matches_three_arg_with_default_locale violated: two-arg gave week/year " + w1.getWeek() + "/" + w1.getYearValue() + " but three-arg gave " + w2.getWeek() + "/" + w2.getYearValue());
    }
} finally {
    Locale.setDefault(savedLocale);
    TimeZone.setDefault(savedZone);
}

javac error:
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/relscreen_0/RelScreen0.java:26: error: cannot find symbol
    GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());
    ^
  symbol:   class GregorianCalendar
  location: class RelScreen0
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/relscreen_0/RelScreen0.java:26: error: cannot find symbol
    GregorianCalendar cal = (GregorianCa

Corrected snippet:
```

</details>
<details open><summary>▸ Output (~2,616 chars)</summary>

```
Locale savedLocale = Locale.getDefault();
TimeZone savedZone = TimeZone.getDefault();
Date t;
TimeZone probeZone;
Week w2;
try {
    Locale.setDefault(Locale.US);
    TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
    Calendar cal = Calendar.getInstance(TimeZone.getDefault());
    cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
    cal.set(Calendar.MILLISECOND, 0);
    t = cal.getTime();
    probeZone = TimeZone.getTimeZone("Europe/Copenhagen");
    w2 = new Week(t, probeZone, Locale.getDefault());
} catch (Exception e) {
    Locale.setDefault(savedLocale);
    TimeZone.setDefault(savedZone);
    return;
}
Week w1;
try {
    w1 = new Week(t, probeZone);
} catch (Exception e) { for (StackTraceElement __rpgf : e.getStackTrace()) { String __rpgc = __rpgf.getClassName(); if ((__rpgc.equals("Week") || __rpgc.endsWith(".Week") || __rpgc.endsWith("$Week")) && !"<init>".equals(__rpgf.getMethodName())) { boolean __rpgdoc = false; if (!__rpgdoc && (__rpgf.getClassName().equals("Week") || __rpgf.getClassName().endsWith(".Week") || __rpgf.getClassName().endsWith("$Week")) && "getFirstMillisecond".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("NullPointerException") || __rpn.endsWith(".NullPointerException") || __rpn.endsWith("$NullPointerException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("Week") || __rpgf.getClassName().endsWith(".Week") || __rpgf.getClassName().endsWith("$Week")) && "getLastMillisecond".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("NullPointerException") || __rpn.endsWith(".NullPointerException") || __rpn.endsWith("$NullPointerException")) { __rpgdoc = true; break; } } } if (__rpgdoc) return; break; } }
    Locale.setDefault(savedLocale);
    TimeZone.setDefault(savedZone);
    throw new RuntimeException("relation ctor_date_zone_matches_three_arg_with_default_locale violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage());
}
try {
    if (w1.getWeek() != w2.getWeek() || w1.getYearValue() != w2.getYearValue()) {
        throw new RuntimeException("relation ctor_date_zone_matches_three_arg_with_default_locale violated: two-arg gave week/year " + w1.getWeek() + "/" + w1.getYearValue() + " but three-arg gave " + w2.getWeek() + "/" + w2.getYearValue());
    }
} finally {
    Locale.setDefault(savedLocale);
    TimeZone.setDefault(savedZone);
}
```

</details>

---
## [27] ⚙️ screen-fuzz-buggy · `ctor_date_zone_matches_three_arg_with_default_locale`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [28] ⚙️ screen · `first_millisecond_overloads_agree`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [29] ⚙️ screen-fuzz-buggy · `first_millisecond_overloads_agree`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [30] ⚙️ screen · `last_millisecond_overloads_agree`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [31] ⚙️ screen-fuzz-buggy · `last_millisecond_overloads_agree`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [32] ⚙️ screen · `week_number_in_documented_range`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [33] ⚙️ screen-fuzz-buggy · `week_number_in_documented_range`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [34] ⚙️ screen · `year_accessors_agree`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [35] ⚙️ screen-fuzz-buggy · `year_accessors_agree`
**output:** 
```json
{
  "checked": 20000,
  "violated": 9667,
  "ratio": 0.4834
}
```

---
## [36] ⚙️ screen · `reader_calls_do_not_mutate_week`
**output:** **tier2-doc-guarded**
- reason: documented exceptions of the patched class return as rejections before the tier-2 rethrow

---
## [37] ⚙️ screen-fuzz-buggy · `reader_calls_do_not_mutate_week`
**output:** 
```json
{
  "checked": 20000,
  "violated": 0,
  "ratio": 0.0
}
```

---
## [38] ⚙️ screen · `year_accessors_agree`
**output:** **kept**
- reason: kept: above-ratio-cap / inverted (replay-only)

---
## [39] ⚙️ screen · `ctor_date_zone_matches_three_arg_with_default_locale`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [40] ⚙️ screen · `first_millisecond_overloads_agree`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [41] ⚙️ screen · `last_millisecond_overloads_agree`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [42] ⚙️ screen · `week_number_in_documented_range`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [43] ⚙️ screen · `reader_calls_do_not_mutate_week`
**output:** **kept**
- reason: silent on buggy (tripwire)

---
## [44] ⚙️ reference-widening · `enumerate`
**output:** **3 observable(s) to widen onto**
- reason: the kept checks probe ['week', 'yearvalue', 'year', 'firstmillisecond', 'lastmillisecond', 'serialindex'] among the patched class's no-argument readers; 8 of those have no admitted reference and 3 fit under the per-leg cap of 3 (the leg already holds []); 2 further name(s) the checks probe are outside the patched class's no-argument readers and were excluded (['name', 'peg'])
<details><summary>▸ detail (~532 chars)</summary>

- detail: {'targets': ['week', 'yearvalue', 'year'], 'already_admitted': [], 'cap': 3, 'checks_read': 12, 'excluded': [{'key': 'name', 'why': 'the patched class does not declare `getName()` — it is a type name, a name declared by another class in the context, or a call the check makes on a collaborator'}, {'key': 'peg', 'why': "the patched class declares `peg` with parameters — the reference chain is generated per NO-ARGUMENT reader, and a reference for it cannot be called from the twin at the firing's own state"}], 'excluded_count': 2}

</details>

---
## [45] ⚙️ reference-widening · `getWeek`
**output:** **reference REQUESTED for observable `week`**
- reason: a kept relation probes this observable and the leg holds no reference for it; the existing generation + screening + admission chain runs unchanged, aimed at this method

---
## [46] ⚙️ reference-impl · `getWeek`
**output:** **widening target requested**
- detail: {'candidates': ['getWeek'], 'attempting': 'getWeek'}

---
## [47] ⚙️ reference-impl · `getWeek`
**output:** **screening surface resolved**
- reason: 6 computed sibling observable(s) on the receiver's own type (stored settings excluded — they agree for free)
- detail: {'siblings': ['getFirstMillisecond', 'getLastMillisecond', 'getSerialIndex', 'getYearValue', 'hashCode', 'toString'], 'patch_touched_exempted': [], 'screened_siblings': ['getFirstMillisecond', 'getLastMillisecond', 'getSerialIndex', 'getYearValue', 'hashCode', 'toString'], 'declaring_types': ['Week']}

---
## [48] 🧠 LLM call — **LLM** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~45,103 chars)</summary>

**[system]**
```
You implement a single method from its SPECIFICATION. You are given documentation, tests, and signatures — never an existing implementation of the method itself. Write the method as the specification says it should behave, using standard knowledge of any algorithm it names. Return one compilable Java class and nothing else.
```
**[user]**
```
Implement `getWeek` from its specification.

You are NOT shown any existing implementation of it, and you must not try to reconstruct one from memory of this project. Derive the behaviour from the documentation and the tests below. Where the documentation names a standard algorithm or formula, use the standard definition of it.

=== DOCUMENTATION (class, method, siblings, inherited) ===
<class name="Week" role="patched">
     * A calendar week.  All years are considered to have 53 weeks, numbered from 1
     * to 53, although in many cases the 53rd week is empty.  Most of the time, the
     * 1st week of the year *begins* in the previous calendar year, but it always
     * finishes in the current year (this behaviour matches the workings of the
     * <code>GregorianCalendar</code> class).
     * <P>
     * This class is immutable, which is a requirement for all
     * {@link RegularTimePeriod} subclasses.
public class Week extends RegularTimePeriod implements Serializable {
     * For serialization.
    private static final long serialVersionUID = 1856387786939865061L;
     * Constant for the first week in the year.
    public static final int FIRST_WEEK_IN_YEAR = 1;
     * Constant for the last week in the year.
    public static final int LAST_WEEK_IN_YEAR = 53;
     * The year in which the week falls.
    private short year;
     * The week (1-53).
    private byte week;
     * The first millisecond.
    private long firstMillisecond;
     * The last millisecond.
    private long lastMillisecond;
     * Creates a new time period for the week in which the current system
     * date/time falls.
    public Week() { /* body withheld */ }
     * Creates a time period representing the week in the specified year.
     * @param week  the week (1 to 53).
     * @param year  the year (1900 to 9999).
    public Week(int week, int year) { /* body withheld */ }
     * Creates a time period representing the week in the specified year.
     * @param week  the week (1 to 53).
     * @param year  the year (1900 to 9999).
    public Week(int week, Year year) { /* body withheld */ }
     * Creates a time period for the week in which the specified date/time
     * falls.
     * @param time  the time (<code>null</code> not permitted).
    public Week(Date time) { /* body withheld */ }
     * Creates a time period for the week in which the specified date/time
     * falls, calculated relative to the specified time zone.
     * @param time  the date/time (<code>null</code> not permitted).
     * @param zone  the time zone (<code>null</code> not permitted).
     * @deprecated As of 1.0.7, use { /* body withheld */ }.
    public Week(Date time, TimeZone zone) { /* body withheld */ }
     * Creates a time period for the week in which the specified date/time
     * falls, calculated relative to the specified time zone.
     * @param time  the date/time (<code>null</code> not permitted).
     * @param zone  the time zone (<code>null</code> not permitted).
     * @param locale  the locale (<code>null</code> not permitted).
     * @since 1.0.7
    public Week(Date time, TimeZone zone, Locale locale) { /* body withheld */ }
     * Returns the year in which the week falls.
     * @return The year (never <code>null</code>).
    public Year getYear() { /* body withheld */ }
     * Returns the year in which the week falls, as an integer value.
     * @return The year.
    public int getYearValue() { /* body withheld */ }
     * Returns the week.
     * @return The week.
    public int getWeek() { /* body withheld */ }
     * Returns the first millisecond of the week.  This will be determined
     * relative to the time zone specified in the constructor, or in the
     * calendar instance passed in the most recent call to the
     * { /* body withheld */ } method.
     * @return The first millisecond of the week.
     * @see #getLastMillisecond()
    public long getFirstMillisecond() { /* body withheld */ }
     * Returns the last millisecond of the week.  This will be
     * determined relative to the time zone specified in the constructor, or
     * in the calendar instance passed in the most recent call to the
     * { /* body withheld */ } method.
     * @return The last millisecond of the week.
     * @see #getFirstMillisecond()
    public long getLastMillisecond() { /* body withheld */ }
     * Recalculates the start date/time and end date/time for this time period
     * relative to the supplied calendar (which incorporates a time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @since 1.0.3
    public void peg(Calendar calendar) { /* body withheld */ }
     * Returns the week preceding this one.  This method will return
     * <code>null</code> for some lower limit on the range of weeks (currently
     * week 1, 1900).  For week 1 of any year, the previous week is always week
     * 53, but week 53 may not contain any days (you should check for this).
     * @return The preceding week (possibly <code>null</code>).
    public RegularTimePeriod previous() { /* body withheld */ }
     * Returns the week following this one.  This method will return
     * <code>null</code> for some upper limit on the range of weeks (currently
     * week 53, 9999).  For week 52 of any year, the following week is always
     * week 53, but week 53 may not contain any days (you should check for
     * this).
     * @return The following week (possibly <code>null</code>).
    public RegularTimePeriod next() { /* body withheld */ }
     * Returns a serial index number for the week.
     * @return The serial index number.
    public long getSerialIndex() { /* body withheld */ }
     * Returns the first millisecond of the week, evaluated using the supplied
     * calendar (which determines the time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @return The first millisecond of the week.
     * @throws NullPointerException if <code>calendar</code> is
     * <code>null</code>.
    public long getFirstMillisecond(Calendar calendar) { /* body withheld */ }
     * Returns a string representing the week (e.g. "Week 9, 2002").
     * TODO: look at internationalisation.
     * @return A string representing the week.
    public String toString() { /* body withheld */ }
     * Tests the equality of this Week object to an arbitrary object.  Returns
     * true if the target is a Week instance representing the same week as this
     * object.  In all other cases, returns false.
     * @param obj  the object (<code>null</code> permitted).
     * @return <code>true</code> if week and year of this and object are the
     * same.
    public boolean equals(Object obj) { /* body withheld */ }
     * Returns a hash code for this object instance.  The approach described by
     * Joshua Bloch in "Effective Java" has been used here:
     * <p>
     * <code>http://developer.java.sun.com/developer/Books/effectivejava
     * /Chapter3.pdf</code>
     * @return A hash code.
    public int hashCode() { /* body withheld */ }
     * Returns an integer indicating the order of this Week object relative to
     * the specified object:
     * negative == before, zero == same, positive == after.
     * @param o1  the object to compare.
     * @return negative == before, zero == same, positive == after.
    public int compareTo(Object o1) { /* body withheld */ }
     * Parses the string argument as a week.
     * <P>
     * This method is required to accept the format "YYYY-Wnn".  It will also
     * accept "Wnn-YYYY". Anything else, at the moment, is a bonus.
     * @param s  string to parse.
     * @return <code>null</code> if the string is not parseable, the week
     * otherwise.
    public static Week parseWeek(String s) { /* body withheld */ }
     * Finds the first occurrence of ' ', '-', ',' or '.'
     * @param s  the string to parse.
     * @return <code>-1</code> if none of the characters was found, the
     * index of the first occurrence otherwise.
    private static int findSeparator(String s) { /* body withheld */ }
     * Creates a year from a string, or returns null (format exceptions
     * suppressed).
     * @param s  string to parse.
     * @return <code>null</code> if the string is not parseable, the year
     * otherwise.
    private static Year evaluateAsYear(String s) { /* body withheld */ }
     * Converts a string to a week.
     * @param s  the string to parse.
     * @return <code>-1</code> if the string does not contain a week number,
     * the number of the week otherwise.
    private static int stringToWeek(String s) { /* body withheld */ }
}
</class>

<class name="RegularTimePeriod" role="supertype-contract">
     * An abstract class representing a unit of time.  Convenient methods are
     * provided for calculating the next and previous time periods.  Conversion
     * methods are defined that return the first and last milliseconds of the time
     * period.  The results from these methods are timezone dependent.
     * <P>
     * This class is immutable, and all subclasses should be immutable also.
public abstract class RegularTimePeriod implements TimePeriod, Comparable,
                                                   MonthConstants {
     * Creates a time period that includes the specified millisecond, assuming
     * the given time zone.
     * @param c  the time period class.
     * @param millisecond  the time.
     * @param zone  the time zone.
     * @return The time period.
    public static RegularTimePeriod createInstance(Class c, Date millisecond, TimeZone zone) { /* body withheld */ }
     * Returns a subclass of { /* body withheld */ } that is smaller than
     * the specified class.
     * @param c  a subclass of {@link RegularTimePeriod}.
     * @return A class.
    public static Class downsize(Class c) { … }
     * Returns the time period preceding this one, or <code>null</code> if some
     * lower limit has been reached.
     * @return The previous time period (possibly <code>null</code>).
    public abstract RegularTimePeriod previous();
     * Returns the time period following this one, or <code>null</code> if some
     * limit has been reached.
     * @return The next time period (possibly <code>null</code>).
    public abstract RegularTimePeriod next();
     * Returns a serial index number for the time unit.
     * @return The serial index number.
    public abstract long getSerialIndex();
     * The default time zone.
    public static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getDefault();
     * Recalculates the start date/time and end date/time for this time period
     * relative to the supplied calendar (which incorporates a time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @since 1.0.3
    public abstract void peg(Calendar calendar);
     * Returns the date/time that marks the start of the time period.  This
     * method returns a new <code>Date</code> instance every time it is called.
     * @return The start date/time.
     * @see #getFirstMillisecond()
    public Date getStart() { /* body withheld */ }
     * Returns the date/time that marks the end of the time period.  This
     * method returns a new <code>Date</code> instance every time it is called.
     * @return The end date/time.
     * @see #getLastMillisecond()
    public Date getEnd() { /* body withheld */ }
     * Returns the first millisecond of the time period.  This will be
     * determined relative to the time zone specified in the constructor, or
     * in the calendar instance passed in the most recent call to the
     * { /* body withheld */ } method.
     * @return The first millisecond of the time period.
     * @see #getLastMillisecond()
    public abstract long getFirstMillisecond();
     * Returns the first millisecond of the time period, evaluated using the
     * supplied calendar (which incorporates a timezone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @return The first millisecond of the time period.
     * @throws NullPointerException if <code>calendar,/code> is
     * </code>null</code>.
     * @see #getLastMillisecond(Calendar)
    public abstract long getFirstMillisecond(Calendar calendar);
     * Returns the last millisecond of the time period.  This will be
     * determined relative to the time zone specified in the constructor, or
     * in the calendar instance passed in the most recent call to the
     * { /* body withheld */ } method.
     * @return The last millisecond of the time period.
     * @see #getFirstMillisecond()
    public abstract long getLastMillisecond();
     * Returns the last millisecond of the time period, evaluated using the
     * supplied calendar (which incorporates a timezone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @return The last millisecond of the time period.
     * @see #getFirstMillisecond(Calendar)
    public abstract long getLastMillisecond(Calendar calendar);
     * Returns the millisecond closest to the middle of the time period.
     * @return The middle millisecond.
    public long getMiddleMillisecond() { /* body withheld */ }
     * Returns the millisecond closest to the middle of the time period,
     * evaluated using the supplied calendar (which incorporates a timezone).
     * @param calendar  the calendar.
     * @return The middle millisecond.
    public long getMiddleMillisecond(Calendar calendar) { /* body withheld */ }
     * Returns a string representation of the time period.
     * @return The string.
    public String toString() { /* body withheld */ }
}
</class>

<class name="Year" role="collaborator">
     * Represents a year in the range 1900 to 9999.  This class is immutable, which
     * is a requirement for all {@link RegularTimePeriod} subclasses.
public class Year extends RegularTimePeriod implements Serializable {
     * For serialization.
    private static final long serialVersionUID = -7659990929736074836L;
     * The year.
    private short year;
     * The first millisecond.
    private long firstMillisecond;
     * The last millisecond.
    private long lastMillisecond;
     * Creates a new <code>Year</code>, based on the current system date/time.
    public Year() { /* body withheld */ }
     * Creates a time period representing a single year.
     * @param year  the year.
    public Year(int year) { /* body withheld */ }
     * Creates a new <code>Year</code>, based on a particular instant in time,
     * using the default time zone.
     * @param time  the time (<code>null</code> not permitted).
    public Year(Date time) { /* body withheld */ }
     * Constructs a year, based on a particular instant in time and a time zone.
     * @param time  the time.
     * @param zone  the time zone.
    public Year(Date time, TimeZone zone) { /* body withheld */ }
     * Returns the year.
     * @return The year.
    public int getYear() { /* body withheld */ }
     * Returns the first millisecond of the year.  This will be determined
     * relative to the time zone specified in the constructor, or in the
     * calendar instance passed in the most recent call to the
     * { /* body withheld */ } method.
     * @return The first millisecond of the year.
     * @see #getLastMillisecond()
    public long getFirstMillisecond() { /* body withheld */ }
     * Returns the last millisecond of the year.  This will be
     * determined relative to the time zone specified in the constructor, or
     * in the calendar instance passed in the most recent call to the
     * { /* body withheld */ } method.
     * @return The last millisecond of the year.
     * @see #getFirstMillisecond()
    public long getLastMillisecond() { /* body withheld */ }
     * Recalculates the start date/time and end date/time for this time period
     * relative to the supplied calendar (which incorporates a time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @since 1.0.3
    public void peg(Calendar calendar) { /* body withheld */ }
     * Returns the year preceding this one.
     * @return The year preceding this one (or <code>null</code> if the
     * current year is 1900).
    public RegularTimePeriod previous() { /* body withheld */ }
     * Returns the year following this one.
     * @return The year following this one (or <code>null</code> if the current
     * year is 9999).
    public RegularTimePeriod next() { /* body withheld */ }
     * Returns a serial index number for the year.
     * <P>
     * The implementation simply returns the year number (e.g. 2002).
     * @return The serial index number.
    public long getSerialIndex() { /* body withheld */ }
     * Returns the first millisecond of the year, evaluated using the supplied
     * calendar (which determines the time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @return The first millisecond of the year.
     * @throws NullPointerException if <code>calendar</code> is
     * <code>null</code>.
    public long getFirstMillisecond(Calendar calendar) { /* body withheld */ }
     * Returns the last millisecond of the year, evaluated using the supplied
     * calendar (which determines the time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @return The last millisecond of the year.
     * @throws NullPointerException if <code>calendar</code> is
     * <code>null</code>.
    public long getLastMillisecond(Calendar calendar) { /* body withheld */ }
     * Tests the equality of this <code>Year</code> object to an arbitrary
     * object.  Returns <code>true</code> if the target is a <code>Year</code>
     * instance representing the same year as this object.  In all other cases,
     * returns <code>false</code>.
     * @param object  the object (<code>null</code> permitted).
     * @return <code>true</code> if the year of this and the object are the
     * same.
    public boolean equals(Object object) { /* body withheld */ }
     * Returns a hash code for this object instance.  The approach described by
     * Joshua Bloch in "Effective Java" has been used here:
     * <p>
     * <code>http://developer.java.sun.com/developer/Books/effectivejava
     * /Chapter3.pdf</code>
     * @return A hash code.
    public int hashCode() { /* body withheld */ }
     * Returns an integer indicating the order of this <code>Year</code> object
     * relative to the specified object:
     * negative == before, zero == same, positive == after.
     * @param o1  the object to compare.
     * @return negative == before, zero == same, positive == after.
    public int compareTo(Object o1) { /* body withheld */ }
     * Returns a string representing the year..
     * @return A string representing the year.
    public String toString() { /* body withheld */ }
     * Parses the string argument as a year.
     * <P>
     * The string format is YYYY.
     * @param s  a string representing the year.
     * @return <code>null</code> if the string is not parseable, the year
     * otherwise.
    public static Year parseYear(String s) { /* body withheld */ }
}
</class>

<class name="TimePeriodFormatException" role="collaborator">
     * An exception that indicates an invalid format in a string representing a
     * time period.
public class TimePeriodFormatException extends IllegalArgumentException {
     * Creates a new exception.
     * @param message  a message describing the exception.
    public TimePeriodFormatException(String message) { /* body withheld */ }
}
</class>

=== THE FAILING TEST, in full — it pins correct behaviour at its own inputs ===
    public void testConstructor() {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        Locale.setDefault(new Locale("da", "DK"));
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
        GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                TimeZone.getDefault(), Locale.getDefault());

        // first day of week is monday
        assertEquals(Calendar.MONDAY, cal.getFirstDayOfWeek());
        cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date t = cal.getTime();
        Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
        assertEquals(34, w.getWeek());

        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
        cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());
        // first day of week is Sunday
        assertEquals(Calendar.SUNDAY, cal.getFirstDayOfWeek());
        cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);

        t = cal.getTime();
        w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
        assertEquals(35, w.getWeek());
        w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                new Locale("da", "DK"));
        assertEquals(34, w.getWeek());

        Locale.setDefault(savedLocale);
        TimeZone.setDefault(savedZone);
    }

=== SKELETON (signatures and fields; bodies are withheld) ===
<class name="Week" role="patched">
     * A calendar week.  All years are considered to have 53 weeks, numbered from 1
     * to 53, although in many cases the 53rd week is empty.  Most of the time, the
     * 1st week of the year *begins* in the previous calendar year, but it always
     * finishes in the current year (this behaviour matches the workings of the
     * <code>GregorianCalendar</code> class).
     * <P>
     * This class is immutable, which is a requirement for all
     * {@link RegularTimePeriod} subclasses.
public class Week extends RegularTimePeriod implements Serializable {
     * For serialization.
    private static final long serialVersionUID = 1856387786939865061L;
     * Constant for the first week in the year.
    public static final int FIRST_WEEK_IN_YEAR = 1;
     * Constant for the last week in the year.
    public static final int LAST_WEEK_IN_YEAR = 53;
     * The year in which the week falls.
    private short year;
     * The week (1-53).
    private byte week;
     * The first millisecond.
    private long firstMillisecond;
     * The last millisecond.
    private long lastMillisecond;
     * Creates a new time period for the week in which the current system
     * date/time falls.
    public Week() { /* body withheld */ }
     * Creates a time period representing the week in the specified year.
     * @param week  the week (1 to 53).
     * @param year  the year (1900 to 9999).
    public Week(int week, int year) { /* body withheld */ }
     * Creates a time period representing the week in the specified year.
     * @param week  the week (1 to 53).
     * @param year  the year (1900 to 9999).
    public Week(int week, Year year) { /* body withheld */ }
     * Creates a time period for the week in which the specified date/time
     * falls.
     * @param time  the time (<code>null</code> not permitted).
    public Week(Date time) { /* body withheld */ }
     * Creates a time period for the week in which the specified date/time
     * falls, calculated relative to the specified time zone.
     * @param time  the date/time (<code>null</code> not permitted).
     * @param zone  the time zone (<code>null</code> not permitted).
     * @deprecated As of 1.0.7, use { /* body withheld */ }.
    public Week(Date time, TimeZone zone) { /* body withheld */ }
     * Creates a time period for the week in which the specified date/time
     * falls, calculated relative to the specified time zone.
     * @param time  the date/time (<code>null</code> not permitted).
     * @param zone  the time zone (<code>null</code> not permitted).
     * @param locale  the locale (<code>null</code> not permitted).
     * @since 1.0.7
    public Week(Date time, TimeZone zone, Locale locale) { /* body withheld */ }
     * Returns the year in which the week falls.
     * @return The year (never <code>null</code>).
    public Year getYear() { /* body withheld */ }
     * Returns the year in which the week falls, as an integer value.
     * @return The year.
    public int getYearValue() { /* body withheld */ }
     * Returns the week.
     * @return The week.
    public int getWeek() { /* body withheld */ }
     * Returns the first millisecond of the week.  This will be determined
     * relative to the time zone specified in the constructor, or in the
     * calendar instance passed in the most recent call to the
     * { /* body withheld */ } method.
     * @return The first millisecond of the week.
     * @see #getLastMillisecond()
    public long getFirstMillisecond() { /* body withheld */ }
     * Returns the last millisecond of the week.  This will be
     * determined relative to the time zone specified in the constructor, or
     * in the calendar instance passed in the most recent call to the
     * { /* body withheld */ } method.
     * @return The last millisecond of the week.
     * @see #getFirstMillisecond()
    public long getLastMillisecond() { /* body withheld */ }
     * Recalculates the start date/time and end date/time for this time period
     * relative to the supplied calendar (which incorporates a time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @since 1.0.3
    public void peg(Calendar calendar) { /* body withheld */ }
     * Returns the week preceding this one.  This method will return
     * <code>null</code> for some lower limit on the range of weeks (currently
     * week 1, 1900).  For week 1 of any year, the previous week is always week
     * 53, but week 53 may not contain any days (you should check for this).
     * @return The preceding week (possibly <code>null</code>).
    public RegularTimePeriod previous() { /* body withheld */ }
     * Returns the week following this one.  This method will return
     * <code>null</code> for some upper limit on the range of weeks (currently
     * week 53, 9999).  For week 52 of any year, the following week is always
     * week 53, but week 53 may not contain any days (you should check for
     * this).
     * @return The following week (possibly <code>null</code>).
    public RegularTimePeriod next() { /* body withheld */ }
     * Returns a serial index number for the week.
     * @return The serial index number.
    public long getSerialIndex() { /* body withheld */ }
     * Returns the first millisecond of the week, evaluated using the supplied
     * calendar (which determines the time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @return The first millisecond of the week.
     * @throws NullPointerException if <code>calendar</code> is
     * <code>null</code>.
    public long getFirstMillisecond(Calendar calendar) { /* body withheld */ }
     * Returns a string representing the week (e.g. "Week 9, 2002").
     * TODO: look at internationalisation.
     * @return A string representing the week.
    public String toString() { /* body withheld */ }
     * Tests the equality of this Week object to an arbitrary object.  Returns
     * true if the target is a Week instance representing the same week as this
     * object.  In all other cases, returns false.
     * @param obj  the object (<code>null</code> permitted).
     * @return <code>true</code> if week and year of this and object are the
     * same.
    public boolean equals(Object obj) { /* body withheld */ }
     * Returns a hash code for this object instance.  The approach described by
     * Joshua Bloch in "Effective Java" has been used here:
     * <p>
     * <code>http://developer.java.sun.com/developer/Books/effectivejava
     * /Chapter3.pdf</code>
     * @return A hash code.
    public int hashCode() { /* body withheld */ }
     * Returns an integer indicating the order of this Week object relative to
     * the specified object:
     * negative == before, zero == same, positive == after.
     * @param o1  the object to compare.
     * @return negative == before, zero == same, positive == after.
    public int compareTo(Object o1) { /* body withheld */ }
     * Parses the string argument as a week.
     * <P>
     * This method is required to accept the format "YYYY-Wnn".  It will also
     * accept "Wnn-YYYY". Anything else, at the moment, is a bonus.
     * @param s  string to parse.
     * @return <code>null</code> if the string is not parseable, the week
     * otherwise.
    public static Week parseWeek(String s) { /* body withheld */ }
     * Finds the first occurrence of ' ', '-', ',' or '.'
     * @param s  the string to parse.
     * @return <code>-1</code> if none of the characters was found, the
     * index of the first occurrence otherwise.
    private static int findSeparator(String s) { /* body withheld */ }
     * Creates a year from a string, or returns null (format exceptions
     * suppressed).
     * @param s  string to parse.
     * @return <code>null</code> if the string is not parseable, the year
     * otherwise.
    private static Year evaluateAsYear(String s) { /* body withheld */ }
     * Converts a string to a week.
     * @param s  the string to parse.
     * @return <code>-1</code> if the string does not contain a week number,
     * the number of the week otherwise.
    private static int stringToWeek(String s) { /* body withheld */ }
}
</class>

<class name="RegularTimePeriod" role="supertype-contract">
     * An abstract class representing a unit of time.  Convenient methods are
     * provided for calculating the next and previous time periods.  Conversion
     * methods are defined that return the first and last milliseconds of the time
     * period.  The results from these methods are timezone dependent.
     * <P>
     * This class is immutable, and all subclasses should be immutable also.
public abstract class RegularTimePeriod implements TimePeriod, Comparable,
                                                   MonthConstants {
     * Creates a time period that includes the specified millisecond, assuming
     * the given time zone.
     * @param c  the time period class.
     * @param millisecond  the time.
     * @param zone  the time zone.
     * @return The time period.
    public static RegularTimePeriod createInstance(Class c, Date millisecond, TimeZone zone) { /* body withheld */ }
     * Returns a subclass of { /* body withheld */ } that is smaller than
     * the specified class.
     * @param c  a subclass of {@link RegularTimePeriod}.
     * @return A class.
    public static Class downsize(Class c) { … }
     * Returns the time period preceding this one, or <code>null</code> if some
     * lower limit has been reached.
     * @return The previous time period (possibly <code>null</code>).
    public abstract RegularTimePeriod previous();
     * Returns the time period following this one, or <code>null</code> if some
     * limit has been reached.
     * @return The next time period (possibly <code>null</code>).
    public abstract RegularTimePeriod next();
     * Returns a serial index number for the time unit.
     * @return The serial index number.
    public abstract long getSerialIndex();
     * The default time zone.
    public static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getDefault();
     * Recalculates the start date/time and end date/time for this time period
     * relative to the supplied calendar (which incorporates a time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @since 1.0.3
    public abstract void peg(Calendar calendar);
     * Returns the date/time that marks the start of the time period.  This
     * method returns a new <code>Date</code> instance every time it is called.
     * @return The start date/time.
     * @see #getFirstMillisecond()
    public Date getStart() { /* body withheld */ }
     * Returns the date/time that marks the end of the time period.  This
     * method returns a new <code>Date</code> instance every time it is called.
     * @return The end date/time.
     * @see #getLastMillisecond()
    public Date getEnd() { /* body withheld */ }
     * Returns the first millisecond of the time period.  This will be
     * determined relative to the time zone specified in the constructor, or
     * in the calendar instance passed in the most recent call to the
     * { /* body withheld */ } method.
     * @return The first millisecond of the time period.
     * @see #getLastMillisecond()
    public abstract long getFirstMillisecond();
     * Returns the first millisecond of the time period, evaluated using the
     * supplied calendar (which incorporates a timezone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @return The first millisecond of the time period.
     * @throws NullPointerException if <code>calendar,/code> is
     * </code>null</code>.
     * @see #getLastMillisecond(Calendar)
    public abstract long getFirstMillisecond(Calendar calendar);
     * Returns the last millisecond of the time period.  This will be
     * determined relative to the time zone specified in the constructor, or
     * in the calendar instance passed in the most recent call to the
     * { /* body withheld */ } method.
     * @return The last millisecond of the time period.
     * @see #getFirstMillisecond()
    public abstract long getLastMillisecond();
     * Returns the last millisecond of the time period, evaluated using the
     * supplied calendar (which incorporates a timezone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @return The last millisecond of the time period.
     * @see #getFirstMillisecond(Calendar)
    public abstract long getLastMillisecond(Calendar calendar);
     * Returns the millisecond closest to the middle of the time period.
     * @return The middle millisecond.
    public long getMiddleMillisecond() { /* body withheld */ }
     * Returns the millisecond closest to the middle of the time period,
     * evaluated using the supplied calendar (which incorporates a timezone).
     * @param calendar  the calendar.
     * @return The middle millisecond.
    public long getMiddleMillisecond(Calendar calendar) { /* body withheld */ }
     * Returns a string representation of the time period.
     * @return The string.
    public String toString() { /* body withheld */ }
}
</class>

<class name="Year" role="collaborator">
     * Represents a year in the range 1900 to 9999.  This class is immutable, which
     * is a requirement for all {@link RegularTimePeriod} subclasses.
public class Year extends RegularTimePeriod implements Serializable {
     * For serialization.
    private static final long serialVersionUID = -7659990929736074836L;
     * The year.
    private short year;
     * The first millisecond.
    private long firstMillisecond;
     * The last millisecond.
    private long lastMillisecond;
     * Creates a new <code>Year</code>, based on the current system date/time.
    public Year() { /* body withheld */ }
     * Creates a time period representing a single year.
     * @param year  the year.
    public Year(int year) { /* body withheld */ }
     * Creates a new <code>Year</code>, based on a particular instant in time,
     * using the default time zone.
     * @param time  the time (<code>null</code> not permitted).
    public Year(Date time) { /* body withheld */ }
     * Constructs a year, based on a particular instant in time and a time zone.
     * @param time  the time.
     * @param zone  the time zone.
    public Year(Date time, TimeZone zone) { /* body withheld */ }
     * Returns the year.
     * @return The year.
    public int getYear() { /* body withheld */ }
     * Returns the first millisecond of the year.  This will be determined
     * relative to the time zone specified in the constructor, or in the
     * calendar instance passed in the most recent call to the
     * { /* body withheld */ } method.
     * @return The first millisecond of the year.
     * @see #getLastMillisecond()
    public long getFirstMillisecond() { /* body withheld */ }
     * Returns the last millisecond of the year.  This will be
     * determined relative to the time zone specified in the constructor, or
     * in the calendar instance passed in the most recent call to the
     * { /* body withheld */ } method.
     * @return The last millisecond of the year.
     * @see #getFirstMillisecond()
    public long getLastMillisecond() { /* body withheld */ }
     * Recalculates the start date/time and end date/time for this time period
     * relative to the supplied calendar (which incorporates a time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @since 1.0.3
    public void peg(Calendar calendar) { /* body withheld */ }
     * Returns the year preceding this one.
     * @return The year preceding this one (or <code>null</code> if the
     * current year is 1900).
    public RegularTimePeriod previous() { /* body withheld */ }
     * Returns the year following this one.
     * @return The year following this one (or <code>null</code> if the current
     * year is 9999).
    public RegularTimePeriod next() { /* body withheld */ }
     * Returns a serial index number for the year.
     * <P>
     * The implementation simply returns the year number (e.g. 2002).
     * @return The serial index number.
    public long getSerialIndex() { /* body withheld */ }
     * Returns the first millisecond of the year, evaluated using the supplied
     * calendar (which determines the time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @return The first millisecond of the year.
     * @throws NullPointerException if <code>calendar</code> is
     * <code>null</code>.
    public long getFirstMillisecond(Calendar calendar) { /* body withheld */ }
     * Returns the last millisecond of the year, evaluated using the supplied
     * calendar (which determines the time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @return The last millisecond of the year.
     * @throws NullPointerException if <code>calendar</code> is
     * <code>null</code>.
    public long getLastMillisecond(Calendar calendar) { /* body withheld */ }
     * Tests the equality of this <code>Year</code> object to an arbitrary
     * object.  Returns <code>true</code> if the target is a <code>Year</code>
     * instance representing the same year as this object.  In all other cases,
     * returns <code>false</code>.
     * @param object  the object (<code>null</code> permitted).
     * @return <code>true</code> if the year of this and the object are the
     * same.
    public boolean equals(Object object) { /* body withheld */ }
     * Returns a hash code for this object instance.  The approach described by
     * Joshua Bloch in "Effective Java" has been used here:
     * <p>
     * <code>http://developer.java.sun.com/developer/Books/effectivejava
     * /Chapter3.pdf</code>
     * @return A hash code.
    public int hashCode() { /* body withheld */ }
     * Returns an integer indicating the order of this <code>Year</code> object
     * relative to the specified object:
     * negative == before, zero == same, positive == after.
     * @param o1  the object to compare.
     * @return negative == before, zero == same, positive == after.
    public int compareTo(Object o1) { /* body withheld */ }
     * Returns a string representing the year..
     * @return A string representing the year.
    public String toString() { /* body withheld */ }
     * Parses the string argument as a year.
     * <P>
     * The string format is YYYY.
     * @param s  a string representing the year.
     * @return <code>null</code> if the string is not parseable, the year
     * otherwise.
    public static Year parseYear(String s) { /* body withheld */ }
}
</class>

<class name="TimePeriodFormatException" role="collaborator">
     * An exception that indicates an invalid format in a string representing a
     * time period.
public class TimePeriodFormatException extends IllegalArgumentException {
     * Creates a new exception.
     * @param message  a message describing the exception.
    public TimePeriodFormatException(String message) { /* body withheld */ }
}
</class>

<class name="TimePeriod" role="collaborator">
     * A period of time measured to millisecond precision using two instances of
     * <code>java.util.Date</code>.
public interface TimePeriod extends Comparable {
     * Returns the start date/time.  This will always be on or before the
     * end date.
     * @return The start date/time (never <code>null</code>).
    public Date getStart();
     * Returns the end date/time.  This will always be on or after the
     * start date.
     * @return The end date/time (never <code>null</code>).
    public Date getEnd();
}
</class>

<class name="JFreeChart" role="collaborator">
     * A chart class implemented using the Java 2D APIs.  The current version
     * supports bar charts, line charts, pie charts and xy plots (including time
     * series data).
     * <P>
     * JFreeChart coordinates several objects to achieve its aim of being able to
     * draw a chart on a Java 2D graphics device: a list of { /* body withheld */ } objects
     * (which often includes the chart's legend), a { /* body withheld */ } and a
     * { /* body withheld */ } (the plot in turn manages a
     * domain axis and a range axis).
     * <P>
     * You should use a { /* body withheld */ } to display a chart in a GUI.
     * <P>
     * …
public class JFreeChart implements Drawable,
                                   TitleChangeListener,
                                   PlotChangeListener,
                                   Serializable,
                                   Cloneable { /* body withheld */ }
// … (non-patched members truncated; patched methods kept)
</class>

<class name="Contributor" role="collaborator">
     * A simple class representing a contributor to a software project.
public class Contributor {
     * The name.
    private String name;
     * The e-mail address.
    private String email;
     * Creates a new contributor.
     * @param name  the name.
     * @param email  the e-mail address.
    public Contributor(String name, String email) { /* body withheld */ }
     * Returns the contributor's name.
     * @return the contributor's name.
    public String getName() { /* body withheld */ }
     * Returns the contributor's e-mail address.
     * @return the contributor's e-mail address.
    public String getEmail() { /* body withheld */ }
}
</class>

Write ONE self-contained Java class named `ReferenceImpl` whose PUBLIC STATIC methods are named `compute_<observable>`.

FUNCTIONALIZE IT. These methods may read object state rather than take arguments; each `compute_<name>` must take that state as PARAMETERS and be pure — same inputs, same result, no fields, no I/O. Declare exactly the data the computation needs, in the SAME parameter order for every one, and return the documented type. A reference that reads no input cannot be run on different inputs, and one that cannot be varied cannot be checked.

NAME each parameter after the state it represents, using the name the class uses for it, and repeat those names in the declaration line below. Take only state the computation actually consumes: a parameter for something the quantity does not depend on cannot be supplied and the reference will be discarded unused.

IMPLEMENT THE SIBLINGS TOO, not only the method named above. The documentation defines several quantities over the same state; write `compute_<name>` for each one you can derive from the documentation. They are how this reference earns its standing: the named method is where the disagreement under review lives, so it cannot also be the evidence that the reference is trustworthy. The siblings are.

The siblings that count are exactly: `getFirstMillisecond`, `getLastMillisecond`, `getSerialIndex`, `getYearValue`, `hashCode`, `toString`. At least 3 of them must be implemented and agree with the class's real behaviour for this reference to be admitted, so implement every one whose value the documentation lets you derive from the declared state. A counter of internal bookkeeping (how many times something was evaluated or iterated) is not derivable from state — skip those rather than invent them.

First line of your reply must be a comment listing what you wrote, exactly: `// compute(<shared parameter types>) : <name1>, <name2>, ...`. Then the class. No markdown fences, no prose.
```

</details>
<details open><summary>▸ Output (~984 chars)</summary>

```
// compute(byte, short, long, long) : getWeek, getYearValue, getFirstMillisecond, getLastMillisecond, getSerialIndex, hashCode, toString
public class ReferenceImpl {

    public static int compute_getWeek(byte week) {
        return week;
    }

    public static int compute_getYearValue(short year) {
        return year;
    }

    public static long compute_getFirstMillisecond(long firstMillisecond) {
        return firstMillisecond;
    }

    public static long compute_getLastMillisecond(long lastMillisecond) {
        return lastMillisecond;
    }

    public static long compute_getSerialIndex(byte week, short year) {
        return year * 53L + week;
    }

    public static int compute_hashCode(byte week, short year) {
        int result = 17;
        result = 37 * result + week;
        result = 37 * result + year;
        return result;
    }

    public static String compute_toString(byte week, short year) {
        return "Week " + week + ", " + year;
    }
}
```

</details>

---
## [49] ⚙️ reference-impl · `getWeek`
**output:** **reference generated**
- detail: {'chars': 984}

---
## [50] ⚙️ reference-impl · `getWeek`
**output:** **signature declared**
- detail: {'signature': 'byte, short, long, long'}

---
## [51] ⚙️ reference-impl · `getWeek`
**output:** **reference observables matched**
- detail: {'matched': {'getWeek': 'getWeek', 'getFirstMillisecond': 'getFirstMillisecond', 'getLastMillisecond': 'getLastMillisecond', 'getSerialIndex': 'getSerialIndex', 'getYearValue': 'getYearValue', 'hashCode': 'hashCode'}, 'declared_only': []}

---
## [52] ⚙️ reference-impl · `getWeek`
**output:** **screen bar reachable**
- reason: 6 shared sibling observable(s) — enough for the screen to decide

---
## [53] ⚙️ reference-impl · `getWeek`
**output:** **signature unmappable — DISCARDED**
- reason: parameter `byte` matches no canonical state field. Fields read by the method: []; all fields (31): ['serialVersionUID', 'FIRST_WEEK_IN_YEAR', 'LAST_WEEK_IN_YEAR', 'year', 'week', 'firstMillisecond', 'lastMillisecond', 'DEFAULT_TIME_ZONE', 'INFO', 'DEFAULT_TITLE_FONT', 'DEFAULT_BACKGROUND_PAINT', 'DEFAULT_BACKGROUND_IMAGE']… — signature unmappable, DISCARDED

---
## [54] ⚙️ reference-widening · `getWeek`
**output:** **widening REJECTED for observable `week`**
- reason: signature unmappable — DISCARDED — parameter `byte` matches no canonical state field. Fields read by the method: []; all fields (31): ['serialVersionUID', 'FIRST_WEEK_IN_YEAR', 'LAST_WEEK_IN_YEAR', 'year', 'week', 'firstMillisecond', 'lastMillisecond', 'DEFAULT_TIME_ZONE', 'INFO', 'DEFAULT_TITLE_FONT', 'DEFAULT_BACKGROUND_PAINT', 'DEFAULT_BACKGROUND_IMAGE']… — signature unmappable, DISCARDED
- detail: {'admitted_observables': [], 'last_step': 'signature unmappable — DISCARDED'}

---
## [55] ⚙️ reference-widening · `getYearValue`
**output:** **reference REQUESTED for observable `yearvalue`**
- reason: a kept relation probes this observable and the leg holds no reference for it; the existing generation + screening + admission chain runs unchanged, aimed at this method

---
## [56] ⚙️ reference-impl · `getYearValue`
**output:** **widening target requested**
- detail: {'candidates': ['getYearValue'], 'attempting': 'getYearValue'}

---
## [57] ⚙️ reference-impl · `getYearValue`
**output:** **screening surface resolved**
- reason: 6 computed sibling observable(s) on the receiver's own type (stored settings excluded — they agree for free); 1 of them PATCH-TOUCHED (['getWeek']) and therefore not screened on — the buggy build is the wrong answer key where the defect lives
- detail: {'siblings': ['getFirstMillisecond', 'getLastMillisecond', 'getSerialIndex', 'getWeek', 'hashCode', 'toString'], 'patch_touched_exempted': ['getWeek'], 'screened_siblings': ['getFirstMillisecond', 'getLastMillisecond', 'getSerialIndex', 'hashCode', 'toString'], 'declaring_types': ['Week']}

---
## [58] 🧠 LLM call — **LLM** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~44,817 chars, 1 new)</summary>

- *[system] message: identical to step [48] — not reprinted*
**[user]**
```
Implement `getYearValue` from its specification.

You are NOT shown any existing implementation of it, and you must not try to reconstruct one from memory of this project. Derive the behaviour from the documentation and the tests below. Where the documentation names a standard algorithm or formula, use the standard definition of it.

=== DOCUMENTATION (class, method, siblings, inherited) ===
<class name="Week" role="patched">
     * A calendar week.  All years are considered to have 53 weeks, numbered from 1
     * to 53, although in many cases the 53rd week is empty.  Most of the time, the
     * 1st week of the year *begins* in the previous calendar year, but it always
     * finishes in the current year (this behaviour matches the workings of the
     * <code>GregorianCalendar</code> class).
     * <P>
     * This class is immutable, which is a requirement for all
     * {@link RegularTimePeriod} subclasses.
public class Week extends RegularTimePeriod implements Serializable {
     * For serialization.
    private static final long serialVersionUID = 1856387786939865061L;
     * Constant for the first week in the year.
    public static final int FIRST_WEEK_IN_YEAR = 1;
     * Constant for the last week in the year.
    public static final int LAST_WEEK_IN_YEAR = 53;
     * The year in which the week falls.
    private short year;
     * The week (1-53).
    private byte week;
     * The first millisecond.
    private long firstMillisecond;
     * The last millisecond.
    private long lastMillisecond;
     * Creates a new time period for the week in which the current system
     * date/time falls.
    public Week() { /* body withheld */ }
     * Creates a time period representing the week in the specified year.
     * @param week  the week (1 to 53).
     * @param year  the year (1900 to 9999).
    public Week(int week, int year) { /* body withheld */ }
     * Creates a time period representing the week in the specified year.
     * @param week  the week (1 to 53).
     * @param year  the year (1900 to 9999).
    public Week(int week, Year year) { /* body withheld */ }
     * Creates a time period for the week in which the specified date/time
     * falls.
     * @param time  the time (<code>null</code> not permitted).
    public Week(Date time) { /* body withheld */ }
     * Creates a time period for the week in which the specified date/time
     * falls, calculated relative to the specified time zone.
     * @param time  the date/time (<code>null</code> not permitted).
     * @param zone  the time zone (<code>null</code> not permitted).
     * @deprecated As of 1.0.7, use { /* body withheld */ }.
    public Week(Date time, TimeZone zone) { /* body withheld */ }
     * Creates a time period for the week in which the specified date/time
     * falls, calculated relative to the specified time zone.
     * @param time  the date/time (<code>null</code> not permitted).
     * @param zone  the time zone (<code>null</code> not permitted).
     * @param locale  the locale (<code>null</code> not permitted).
     * @since 1.0.7
    public Week(Date time, TimeZone zone, Locale locale) { /* body withheld */ }
     * Returns the year in which the week falls.
     * @return The year (never <code>null</code>).
    public Year getYear() { /* body withheld */ }
     * Returns the year in which the week falls, as an integer value.
     * @return The year.
    public int getYearValue() { /* body withheld */ }
     * Returns the week.
     * @return The week.
    public int getWeek() { /* body withheld */ }
     * Returns the first millisecond of the week.  This will be determined
     * relative to the time zone specified in the constructor, or in the
     * calendar instance passed in the most recent call to the
     * { /* body withheld */ } method.
     * @return The first millisecond of the week.
     * @see #getLastMillisecond()
    public long getFirstMillisecond() { /* body withheld */ }
     * Returns the last millisecond of the week.  This will be
     * determined relative to the time zone specified in the constructor, or
     * in the calendar instance passed in the most recent call to the
     * { /* body withheld */ } method.
     * @return The last millisecond of the week.
     * @see #getFirstMillisecond()
    public long getLastMillisecond() { /* body withheld */ }
     * Recalculates the start date/time and end date/time for this time period
     * relative to the supplied calendar (which incorporates a time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @since 1.0.3
    public void peg(Calendar calendar) { /* body withheld */ }
     * Returns the week preceding this one.  This method will return
     * <code>null</code> for some lower limit on the range of weeks (currently
     * week 1, 1900).  For week 1 of any year, the previous week is always week
     * 53, but week 53 may not contain any days (you should check for this).
     * @return The preceding week (possibly <code>null</code>).
    public RegularTimePeriod previous() { /* body withheld */ }
     * Returns the week following this one.  This method will return
     * <code>null</code> for some upper limit on the range of weeks (currently
     * week 53, 9999).  For week 52 of any year, the following week is always
     * week 53, but week 53 may not contain any days (you should check for
     * this).
     * @return The following week (possibly <code>null</code>).
    public RegularTimePeriod next() { /* body withheld */ }
     * Returns a serial index number for the week.
     * @return The serial index number.
    public long getSerialIndex() { /* body withheld */ }
     * Returns the first millisecond of the week, evaluated using the supplied
     * calendar (which determines the time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @return The first millisecond of the week.
     * @throws NullPointerException if <code>calendar</code> is
     * <code>null</code>.
    public long getFirstMillisecond(Calendar calendar) { /* body withheld */ }
     * Returns a string representing the week (e.g. "Week 9, 2002").
     * TODO: look at internationalisation.
     * @return A string representing the week.
    public String toString() { /* body withheld */ }
     * Tests the equality of this Week object to an arbitrary object.  Returns
     * true if the target is a Week instance representing the same week as this
     * object.  In all other cases, returns false.
     * @param obj  the object (<code>null</code> permitted).
     * @return <code>true</code> if week and year of this and object are the
     * same.
    public boolean equals(Object obj) { /* body withheld */ }
     * Returns a hash code for this object instance.  The approach described by
     * Joshua Bloch in "Effective Java" has been used here:
     * <p>
     * <code>http://developer.java.sun.com/developer/Books/effectivejava
     * /Chapter3.pdf</code>
     * @return A hash code.
    public int hashCode() { /* body withheld */ }
     * Returns an integer indicating the order of this Week object relative to
     * the specified object:
     * negative == before, zero == same, positive == after.
     * @param o1  the object to compare.
     * @return negative == before, zero == same, positive == after.
    public int compareTo(Object o1) { /* body withheld */ }
     * Parses the string argument as a week.
     * <P>
     * This method is required to accept the format "YYYY-Wnn".  It will also
     * accept "Wnn-YYYY". Anything else, at the moment, is a bonus.
     * @param s  string to parse.
     * @return <code>null</code> if the string is not parseable, the week
     * otherwise.
    public static Week parseWeek(String s) { /* body withheld */ }
     * Finds the first occurrence of ' ', '-', ',' or '.'
     * @param s  the string to parse.
     * @return <code>-1</code> if none of the characters was found, the
     * index of the first occurrence otherwise.
    private static int findSeparator(String s) { /* body withheld */ }
     * Creates a year from a string, or returns null (format exceptions
     * suppressed).
     * @param s  string to parse.
     * @return <code>null</code> if the string is not parseable, the year
     * otherwise.
    private static Year evaluateAsYear(String s) { /* body withheld */ }
     * Converts a string to a week.
     * @param s  the string to parse.
     * @return <code>-1</code> if the string does not contain a week number,
     * the number of the week otherwise.
    private static int stringToWeek(String s) { /* body withheld */ }
}
</class>

<class name="RegularTimePeriod" role="supertype-contract">
     * An abstract class representing a unit of time.  Convenient methods are
     * provided for calculating the next and previous time periods.  Conversion
     * methods are defined that return the first and last milliseconds of the time
     * period.  The results from these methods are timezone dependent.
     * <P>
     * This class is immutable, and all subclasses should be immutable also.
public abstract class RegularTimePeriod implements TimePeriod, Comparable,
                                                   MonthConstants {
     * Creates a time period that includes the specified millisecond, assuming
     * the given time zone.
     * @param c  the time period class.
     * @param millisecond  the time.
     * @param zone  the time zone.
     * @return The time period.
    public static RegularTimePeriod createInstance(Class c, Date millisecond, TimeZone zone) { /* body withheld */ }
     * Returns a subclass of { /* body withheld */ } that is smaller than
     * the specified class.
     * @param c  a subclass of {@link RegularTimePeriod}.
     * @return A class.
    public static Class downsize(Class c) { … }
     * Returns the time period preceding this one, or <code>null</code> if some
     * lower limit has been reached.
     * @return The previous time period (possibly <code>null</code>).
    public abstract RegularTimePeriod previous();
     * Returns the time period following this one, or <code>null</code> if some
     * limit has been reached.
     * @return The next time period (possibly <code>null</code>).
    public abstract RegularTimePeriod next();
     * Returns a serial index number for the time unit.
     * @return The serial index number.
    public abstract long getSerialIndex();
     * The default time zone.
    public static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getDefault();
     * Recalculates the start date/time and end date/time for this time period
     * relative to the supplied calendar (which incorporates a time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @since 1.0.3
    public abstract void peg(Calendar calendar);
     * Returns the date/time that marks the start of the time period.  This
     * method returns a new <code>Date</code> instance every time it is called.
     * @return The start date/time.
     * @see #getFirstMillisecond()
    public Date getStart() { /* body withheld */ }
     * Returns the date/time that marks the end of the time period.  This
     * method returns a new <code>Date</code> instance every time it is called.
     * @return The end date/time.
     * @see #getLastMillisecond()
    public Date getEnd() { /* body withheld */ }
     * Returns the first millisecond of the time period.  This will be
     * determined relative to the time zone specified in the constructor, or
     * in the calendar instance passed in the most recent call to the
     * { /* body withheld */ } method.
     * @return The first millisecond of the time period.
     * @see #getLastMillisecond()
    public abstract long getFirstMillisecond();
     * Returns the first millisecond of the time period, evaluated using the
     * supplied calendar (which incorporates a timezone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @return The first millisecond of the time period.
     * @throws NullPointerException if <code>calendar,/code> is
     * </code>null</code>.
     * @see #getLastMillisecond(Calendar)
    public abstract long getFirstMillisecond(Calendar calendar);
     * Returns the last millisecond of the time period.  This will be
     * determined relative to the time zone specified in the constructor, or
     * in the calendar instance passed in the most recent call to the
     * { /* body withheld */ } method.
     * @return The last millisecond of the time period.
     * @see #getFirstMillisecond()
    public abstract long getLastMillisecond();
     * Returns the last millisecond of the time period, evaluated using the
     * supplied calendar (which incorporates a timezone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @return The last millisecond of the time period.
     * @see #getFirstMillisecond(Calendar)
    public abstract long getLastMillisecond(Calendar calendar);
     * Returns the millisecond closest to the middle of the time period.
     * @return The middle millisecond.
    public long getMiddleMillisecond() { /* body withheld */ }
     * Returns the millisecond closest to the middle of the time period,
     * evaluated using the supplied calendar (which incorporates a timezone).
     * @param calendar  the calendar.
     * @return The middle millisecond.
    public long getMiddleMillisecond(Calendar calendar) { /* body withheld */ }
     * Returns a string representation of the time period.
     * @return The string.
    public String toString() { /* body withheld */ }
}
</class>

<class name="Year" role="collaborator">
     * Represents a year in the range 1900 to 9999.  This class is immutable, which
     * is a requirement for all {@link RegularTimePeriod} subclasses.
public class Year extends RegularTimePeriod implements Serializable {
     * For serialization.
    private static final long serialVersionUID = -7659990929736074836L;
     * The year.
    private short year;
     * The first millisecond.
    private long firstMillisecond;
     * The last millisecond.
    private long lastMillisecond;
     * Creates a new <code>Year</code>, based on the current system date/time.
    public Year() { /* body withheld */ }
     * Creates a time period representing a single year.
     * @param year  the year.
    public Year(int year) { /* body withheld */ }
     * Creates a new <code>Year</code>, based on a particular instant in time,
     * using the default time zone.
     * @param time  the time (<code>null</code> not permitted).
    public Year(Date time) { /* body withheld */ }
     * Constructs a year, based on a particular instant in time and a time zone.
     * @param time  the time.
     * @param zone  the time zone.
    public Year(Date time, TimeZone zone) { /* body withheld */ }
     * Returns the year.
     * @return The year.
    public int getYear() { /* body withheld */ }
     * Returns the first millisecond of the year.  This will be determined
     * relative to the time zone specified in the constructor, or in the
     * calendar instance passed in the most recent call to the
     * { /* body withheld */ } method.
     * @return The first millisecond of the year.
     * @see #getLastMillisecond()
    public long getFirstMillisecond() { /* body withheld */ }
     * Returns the last millisecond of the year.  This will be
     * determined relative to the time zone specified in the constructor, or
     * in the calendar instance passed in the most recent call to the
     * { /* body withheld */ } method.
     * @return The last millisecond of the year.
     * @see #getFirstMillisecond()
    public long getLastMillisecond() { /* body withheld */ }
     * Recalculates the start date/time and end date/time for this time period
     * relative to the supplied calendar (which incorporates a time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @since 1.0.3
    public void peg(Calendar calendar) { /* body withheld */ }
     * Returns the year preceding this one.
     * @return The year preceding this one (or <code>null</code> if the
     * current year is 1900).
    public RegularTimePeriod previous() { /* body withheld */ }
     * Returns the year following this one.
     * @return The year following this one (or <code>null</code> if the current
     * year is 9999).
    public RegularTimePeriod next() { /* body withheld */ }
     * Returns a serial index number for the year.
     * <P>
     * The implementation simply returns the year number (e.g. 2002).
     * @return The serial index number.
    public long getSerialIndex() { /* body withheld */ }
     * Returns the first millisecond of the year, evaluated using the supplied
     * calendar (which determines the time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @return The first millisecond of the year.
     * @throws NullPointerException if <code>calendar</code> is
     * <code>null</code>.
    public long getFirstMillisecond(Calendar calendar) { /* body withheld */ }
     * Returns the last millisecond of the year, evaluated using the supplied
     * calendar (which determines the time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @return The last millisecond of the year.
     * @throws NullPointerException if <code>calendar</code> is
     * <code>null</code>.
    public long getLastMillisecond(Calendar calendar) { /* body withheld */ }
     * Tests the equality of this <code>Year</code> object to an arbitrary
     * object.  Returns <code>true</code> if the target is a <code>Year</code>
     * instance representing the same year as this object.  In all other cases,
     * returns <code>false</code>.
     * @param object  the object (<code>null</code> permitted).
     * @return <code>true</code> if the year of this and the object are the
     * same.
    public boolean equals(Object object) { /* body withheld */ }
     * Returns a hash code for this object instance.  The approach described by
     * Joshua Bloch in "Effective Java" has been used here:
     * <p>
     * <code>http://developer.java.sun.com/developer/Books/effectivejava
     * /Chapter3.pdf</code>
     * @return A hash code.
    public int hashCode() { /* body withheld */ }
     * Returns an integer indicating the order of this <code>Year</code> object
     * relative to the specified object:
     * negative == before, zero == same, positive == after.
     * @param o1  the object to compare.
     * @return negative == before, zero == same, positive == after.
    public int compareTo(Object o1) { /* body withheld */ }
     * Returns a string representing the year..
     * @return A string representing the year.
    public String toString() { /* body withheld */ }
     * Parses the string argument as a year.
     * <P>
     * The string format is YYYY.
     * @param s  a string representing the year.
     * @return <code>null</code> if the string is not parseable, the year
     * otherwise.
    public static Year parseYear(String s) { /* body withheld */ }
}
</class>

<class name="TimePeriodFormatException" role="collaborator">
     * An exception that indicates an invalid format in a string representing a
     * time period.
public class TimePeriodFormatException extends IllegalArgumentException {
     * Creates a new exception.
     * @param message  a message describing the exception.
    public TimePeriodFormatException(String message) { /* body withheld */ }
}
</class>

=== THE FAILING TEST, in full — it pins correct behaviour at its own inputs ===
    public void testConstructor() {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        Locale.setDefault(new Locale("da", "DK"));
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
        GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                TimeZone.getDefault(), Locale.getDefault());

        // first day of week is monday
        assertEquals(Calendar.MONDAY, cal.getFirstDayOfWeek());
        cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date t = cal.getTime();
        Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
        assertEquals(34, w.getWeek());

        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
        cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());
        // first day of week is Sunday
        assertEquals(Calendar.SUNDAY, cal.getFirstDayOfWeek());
        cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);

        t = cal.getTime();
        w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
        assertEquals(35, w.getWeek());
        w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                new Locale("da", "DK"));
        assertEquals(34, w.getWeek());

        Locale.setDefault(savedLocale);
        TimeZone.setDefault(savedZone);
    }

=== SKELETON (signatures and fields; bodies are withheld) ===
<class name="Week" role="patched">
     * A calendar week.  All years are considered to have 53 weeks, numbered from 1
     * to 53, although in many cases the 53rd week is empty.  Most of the time, the
     * 1st week of the year *begins* in the previous calendar year, but it always
     * finishes in the current year (this behaviour matches the workings of the
     * <code>GregorianCalendar</code> class).
     * <P>
     * This class is immutable, which is a requirement for all
     * {@link RegularTimePeriod} subclasses.
public class Week extends RegularTimePeriod implements Serializable {
     * For serialization.
    private static final long serialVersionUID = 1856387786939865061L;
     * Constant for the first week in the year.
    public static final int FIRST_WEEK_IN_YEAR = 1;
     * Constant for the last week in the year.
    public static final int LAST_WEEK_IN_YEAR = 53;
     * The year in which the week falls.
    private short year;
     * The week (1-53).
    private byte week;
     * The first millisecond.
    private long firstMillisecond;
     * The last millisecond.
    private long lastMillisecond;
     * Creates a new time period for the week in which the current system
     * date/time falls.
    public Week() { /* body withheld */ }
     * Creates a time period representing the week in the specified year.
     * @param week  the week (1 to 53).
     * @param year  the year (1900 to 9999).
    public Week(int week, int year) { /* body withheld */ }
     * Creates a time period representing the week in the specified year.
     * @param week  the week (1 to 53).
     * @param year  the year (1900 to 9999).
    public Week(int week, Year year) { /* body withheld */ }
     * Creates a time period for the week in which the specified date/time
     * falls.
     * @param time  the time (<code>null</code> not permitted).
    public Week(Date time) { /* body withheld */ }
     * Creates a time period for the week in which the specified date/time
     * falls, calculated relative to the specified time zone.
     * @param time  the date/time (<code>null</code> not permitted).
     * @param zone  the time zone (<code>null</code> not permitted).
     * @deprecated As of 1.0.7, use { /* body withheld */ }.
    public Week(Date time, TimeZone zone) { /* body withheld */ }
     * Creates a time period for the week in which the specified date/time
     * falls, calculated relative to the specified time zone.
     * @param time  the date/time (<code>null</code> not permitted).
     * @param zone  the time zone (<code>null</code> not permitted).
     * @param locale  the locale (<code>null</code> not permitted).
     * @since 1.0.7
    public Week(Date time, TimeZone zone, Locale locale) { /* body withheld */ }
     * Returns the year in which the week falls.
     * @return The year (never <code>null</code>).
    public Year getYear() { /* body withheld */ }
     * Returns the year in which the week falls, as an integer value.
     * @return The year.
    public int getYearValue() { /* body withheld */ }
     * Returns the week.
     * @return The week.
    public int getWeek() { /* body withheld */ }
     * Returns the first millisecond of the week.  This will be determined
     * relative to the time zone specified in the constructor, or in the
     * calendar instance passed in the most recent call to the
     * { /* body withheld */ } method.
     * @return The first millisecond of the week.
     * @see #getLastMillisecond()
    public long getFirstMillisecond() { /* body withheld */ }
     * Returns the last millisecond of the week.  This will be
     * determined relative to the time zone specified in the constructor, or
     * in the calendar instance passed in the most recent call to the
     * { /* body withheld */ } method.
     * @return The last millisecond of the week.
     * @see #getFirstMillisecond()
    public long getLastMillisecond() { /* body withheld */ }
     * Recalculates the start date/time and end date/time for this time period
     * relative to the supplied calendar (which incorporates a time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @since 1.0.3
    public void peg(Calendar calendar) { /* body withheld */ }
     * Returns the week preceding this one.  This method will return
     * <code>null</code> for some lower limit on the range of weeks (currently
     * week 1, 1900).  For week 1 of any year, the previous week is always week
     * 53, but week 53 may not contain any days (you should check for this).
     * @return The preceding week (possibly <code>null</code>).
    public RegularTimePeriod previous() { /* body withheld */ }
     * Returns the week following this one.  This method will return
     * <code>null</code> for some upper limit on the range of weeks (currently
     * week 53, 9999).  For week 52 of any year, the following week is always
     * week 53, but week 53 may not contain any days (you should check for
     * this).
     * @return The following week (possibly <code>null</code>).
    public RegularTimePeriod next() { /* body withheld */ }
     * Returns a serial index number for the week.
     * @return The serial index number.
    public long getSerialIndex() { /* body withheld */ }
     * Returns the first millisecond of the week, evaluated using the supplied
     * calendar (which determines the time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @return The first millisecond of the week.
     * @throws NullPointerException if <code>calendar</code> is
     * <code>null</code>.
    public long getFirstMillisecond(Calendar calendar) { /* body withheld */ }
     * Returns a string representing the week (e.g. "Week 9, 2002").
     * TODO: look at internationalisation.
     * @return A string representing the week.
    public String toString() { /* body withheld */ }
     * Tests the equality of this Week object to an arbitrary object.  Returns
     * true if the target is a Week instance representing the same week as this
     * object.  In all other cases, returns false.
     * @param obj  the object (<code>null</code> permitted).
     * @return <code>true</code> if week and year of this and object are the
     * same.
    public boolean equals(Object obj) { /* body withheld */ }
     * Returns a hash code for this object instance.  The approach described by
     * Joshua Bloch in "Effective Java" has been used here:
     * <p>
     * <code>http://developer.java.sun.com/developer/Books/effectivejava
     * /Chapter3.pdf</code>
     * @return A hash code.
    public int hashCode() { /* body withheld */ }
     * Returns an integer indicating the order of this Week object relative to
     * the specified object:
     * negative == before, zero == same, positive == after.
     * @param o1  the object to compare.
     * @return negative == before, zero == same, positive == after.
    public int compareTo(Object o1) { /* body withheld */ }
     * Parses the string argument as a week.
     * <P>
     * This method is required to accept the format "YYYY-Wnn".  It will also
     * accept "Wnn-YYYY". Anything else, at the moment, is a bonus.
     * @param s  string to parse.
     * @return <code>null</code> if the string is not parseable, the week
     * otherwise.
    public static Week parseWeek(String s) { /* body withheld */ }
     * Finds the first occurrence of ' ', '-', ',' or '.'
     * @param s  the string to parse.
     * @return <code>-1</code> if none of the characters was found, the
     * index of the first occurrence otherwise.
    private static int findSeparator(String s) { /* body withheld */ }
     * Creates a year from a string, or returns null (format exceptions
     * suppressed).
     * @param s  string to parse.
     * @return <code>null</code> if the string is not parseable, the year
     * otherwise.
    private static Year evaluateAsYear(String s) { /* body withheld */ }
     * Converts a string to a week.
     * @param s  the string to parse.
     * @return <code>-1</code> if the string does not contain a week number,
     * the number of the week otherwise.
    private static int stringToWeek(String s) { /* body withheld */ }
}
</class>

<class name="RegularTimePeriod" role="supertype-contract">
     * An abstract class representing a unit of time.  Convenient methods are
     * provided for calculating the next and previous time periods.  Conversion
     * methods are defined that return the first and last milliseconds of the time
     * period.  The results from these methods are timezone dependent.
     * <P>
     * This class is immutable, and all subclasses should be immutable also.
public abstract class RegularTimePeriod implements TimePeriod, Comparable,
                                                   MonthConstants {
     * Creates a time period that includes the specified millisecond, assuming
     * the given time zone.
     * @param c  the time period class.
     * @param millisecond  the time.
     * @param zone  the time zone.
     * @return The time period.
    public static RegularTimePeriod createInstance(Class c, Date millisecond, TimeZone zone) { /* body withheld */ }
     * Returns a subclass of { /* body withheld */ } that is smaller than
     * the specified class.
     * @param c  a subclass of {@link RegularTimePeriod}.
     * @return A class.
    public static Class downsize(Class c) { … }
     * Returns the time period preceding this one, or <code>null</code> if some
     * lower limit has been reached.
     * @return The previous time period (possibly <code>null</code>).
    public abstract RegularTimePeriod previous();
     * Returns the time period following this one, or <code>null</code> if some
     * limit has been reached.
     * @return The next time period (possibly <code>null</code>).
    public abstract RegularTimePeriod next();
     * Returns a serial index number for the time unit.
     * @return The serial index number.
    public abstract long getSerialIndex();
     * The default time zone.
    public static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getDefault();
     * Recalculates the start date/time and end date/time for this time period
     * relative to the supplied calendar (which incorporates a time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @since 1.0.3
    public abstract void peg(Calendar calendar);
     * Returns the date/time that marks the start of the time period.  This
     * method returns a new <code>Date</code> instance every time it is called.
     * @return The start date/time.
     * @see #getFirstMillisecond()
    public Date getStart() { /* body withheld */ }
     * Returns the date/time that marks the end of the time period.  This
     * method returns a new <code>Date</code> instance every time it is called.
     * @return The end date/time.
     * @see #getLastMillisecond()
    public Date getEnd() { /* body withheld */ }
     * Returns the first millisecond of the time period.  This will be
     * determined relative to the time zone specified in the constructor, or
     * in the calendar instance passed in the most recent call to the
     * { /* body withheld */ } method.
     * @return The first millisecond of the time period.
     * @see #getLastMillisecond()
    public abstract long getFirstMillisecond();
     * Returns the first millisecond of the time period, evaluated using the
     * supplied calendar (which incorporates a timezone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @return The first millisecond of the time period.
     * @throws NullPointerException if <code>calendar,/code> is
     * </code>null</code>.
     * @see #getLastMillisecond(Calendar)
    public abstract long getFirstMillisecond(Calendar calendar);
     * Returns the last millisecond of the time period.  This will be
     * determined relative to the time zone specified in the constructor, or
     * in the calendar instance passed in the most recent call to the
     * { /* body withheld */ } method.
     * @return The last millisecond of the time period.
     * @see #getFirstMillisecond()
    public abstract long getLastMillisecond();
     * Returns the last millisecond of the time period, evaluated using the
     * supplied calendar (which incorporates a timezone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @return The last millisecond of the time period.
     * @see #getFirstMillisecond(Calendar)
    public abstract long getLastMillisecond(Calendar calendar);
     * Returns the millisecond closest to the middle of the time period.
     * @return The middle millisecond.
    public long getMiddleMillisecond() { /* body withheld */ }
     * Returns the millisecond closest to the middle of the time period,
     * evaluated using the supplied calendar (which incorporates a timezone).
     * @param calendar  the calendar.
     * @return The middle millisecond.
    public long getMiddleMillisecond(Calendar calendar) { /* body withheld */ }
     * Returns a string representation of the time period.
     * @return The string.
    public String toString() { /* body withheld */ }
}
</class>

<class name="Year" role="collaborator">
     * Represents a year in the range 1900 to 9999.  This class is immutable, which
     * is a requirement for all {@link RegularTimePeriod} subclasses.
public class Year extends RegularTimePeriod implements Serializable {
     * For serialization.
    private static final long serialVersionUID = -7659990929736074836L;
     * The year.
    private short year;
     * The first millisecond.
    private long firstMillisecond;
     * The last millisecond.
    private long lastMillisecond;
     * Creates a new <code>Year</code>, based on the current system date/time.
    public Year() { /* body withheld */ }
     * Creates a time period representing a single year.
     * @param year  the year.
    public Year(int year) { /* body withheld */ }
     * Creates a new <code>Year</code>, based on a particular instant in time,
     * using the default time zone.
     * @param time  the time (<code>null</code> not permitted).
    public Year(Date time) { /* body withheld */ }
     * Constructs a year, based on a particular instant in time and a time zone.
     * @param time  the time.
     * @param zone  the time zone.
    public Year(Date time, TimeZone zone) { /* body withheld */ }
     * Returns the year.
     * @return The year.
    public int getYear() { /* body withheld */ }
     * Returns the first millisecond of the year.  This will be determined
     * relative to the time zone specified in the constructor, or in the
     * calendar instance passed in the most recent call to the
     * { /* body withheld */ } method.
     * @return The first millisecond of the year.
     * @see #getLastMillisecond()
    public long getFirstMillisecond() { /* body withheld */ }
     * Returns the last millisecond of the year.  This will be
     * determined relative to the time zone specified in the constructor, or
     * in the calendar instance passed in the most recent call to the
     * { /* body withheld */ } method.
     * @return The last millisecond of the year.
     * @see #getFirstMillisecond()
    public long getLastMillisecond() { /* body withheld */ }
     * Recalculates the start date/time and end date/time for this time period
     * relative to the supplied calendar (which incorporates a time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @since 1.0.3
    public void peg(Calendar calendar) { /* body withheld */ }
     * Returns the year preceding this one.
     * @return The year preceding this one (or <code>null</code> if the
     * current year is 1900).
    public RegularTimePeriod previous() { /* body withheld */ }
     * Returns the year following this one.
     * @return The year following this one (or <code>null</code> if the current
     * year is 9999).
    public RegularTimePeriod next() { /* body withheld */ }
     * Returns a serial index number for the year.
     * <P>
     * The implementation simply returns the year number (e.g. 2002).
     * @return The serial index number.
    public long getSerialIndex() { /* body withheld */ }
     * Returns the first millisecond of the year, evaluated using the supplied
     * calendar (which determines the time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @return The first millisecond of the year.
     * @throws NullPointerException if <code>calendar</code> is
     * <code>null</code>.
    public long getFirstMillisecond(Calendar calendar) { /* body withheld */ }
     * Returns the last millisecond of the year, evaluated using the supplied
     * calendar (which determines the time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @return The last millisecond of the year.
     * @throws NullPointerException if <code>calendar</code> is
     * <code>null</code>.
    public long getLastMillisecond(Calendar calendar) { /* body withheld */ }
     * Tests the equality of this <code>Year</code> object to an arbitrary
     * object.  Returns <code>true</code> if the target is a <code>Year</code>
     * instance representing the same year as this object.  In all other cases,
     * returns <code>false</code>.
     * @param object  the object (<code>null</code> permitted).
     * @return <code>true</code> if the year of this and the object are the
     * same.
    public boolean equals(Object object) { /* body withheld */ }
     * Returns a hash code for this object instance.  The approach described by
     * Joshua Bloch in "Effective Java" has been used here:
     * <p>
     * <code>http://developer.java.sun.com/developer/Books/effectivejava
     * /Chapter3.pdf</code>
     * @return A hash code.
    public int hashCode() { /* body withheld */ }
     * Returns an integer indicating the order of this <code>Year</code> object
     * relative to the specified object:
     * negative == before, zero == same, positive == after.
     * @param o1  the object to compare.
     * @return negative == before, zero == same, positive == after.
    public int compareTo(Object o1) { /* body withheld */ }
     * Returns a string representing the year..
     * @return A string representing the year.
    public String toString() { /* body withheld */ }
     * Parses the string argument as a year.
     * <P>
     * The string format is YYYY.
     * @param s  a string representing the year.
     * @return <code>null</code> if the string is not parseable, the year
     * otherwise.
    public static Year parseYear(String s) { /* body withheld */ }
}
</class>

<class name="TimePeriodFormatException" role="collaborator">
     * An exception that indicates an invalid format in a string representing a
     * time period.
public class TimePeriodFormatException extends IllegalArgumentException {
     * Creates a new exception.
     * @param message  a message describing the exception.
    public TimePeriodFormatException(String message) { /* body withheld */ }
}
</class>

<class name="TimePeriod" role="collaborator">
     * A period of time measured to millisecond precision using two instances of
     * <code>java.util.Date</code>.
public interface TimePeriod extends Comparable {
     * Returns the start date/time.  This will always be on or before the
     * end date.
     * @return The start date/time (never <code>null</code>).
    public Date getStart();
     * Returns the end date/time.  This will always be on or after the
     * start date.
     * @return The end date/time (never <code>null</code>).
    public Date getEnd();
}
</class>

<class name="JFreeChart" role="collaborator">
     * A chart class implemented using the Java 2D APIs.  The current version
     * supports bar charts, line charts, pie charts and xy plots (including time
     * series data).
     * <P>
     * JFreeChart coordinates several objects to achieve its aim of being able to
     * draw a chart on a Java 2D graphics device: a list of { /* body withheld */ } objects
     * (which often includes the chart's legend), a { /* body withheld */ } and a
     * { /* body withheld */ } (the plot in turn manages a
     * domain axis and a range axis).
     * <P>
     * You should use a { /* body withheld */ } to display a chart in a GUI.
     * <P>
     * …
public class JFreeChart implements Drawable,
                                   TitleChangeListener,
                                   PlotChangeListener,
                                   Serializable,
                                   Cloneable { /* body withheld */ }
// … (non-patched members truncated; patched methods kept)
</class>

<class name="Contributor" role="collaborator">
     * A simple class representing a contributor to a software project.
public class Contributor {
     * The name.
    private String name;
     * The e-mail address.
    private String email;
     * Creates a new contributor.
     * @param name  the name.
     * @param email  the e-mail address.
    public Contributor(String name, String email) { /* body withheld */ }
     * Returns the contributor's name.
     * @return the contributor's name.
    public String getName() { /* body withheld */ }
     * Returns the contributor's e-mail address.
     * @return the contributor's e-mail address.
    public String getEmail() { /* body withheld */ }
}
</class>

Write ONE self-contained Java class named `ReferenceImpl` whose PUBLIC STATIC methods are named `compute_<observable>`.

FUNCTIONALIZE IT. These methods may read object state rather than take arguments; each `compute_<name>` must take that state as PARAMETERS and be pure — same inputs, same result, no fields, no I/O. Declare exactly the data the computation needs, in the SAME parameter order for every one, and return the documented type. A reference that reads no input cannot be run on different inputs, and one that cannot be varied cannot be checked.

NAME each parameter after the state it represents, using the name the class uses for it, and repeat those names in the declaration line below. Take only state the computation actually consumes: a parameter for something the quantity does not depend on cannot be supplied and the reference will be discarded unused.

IMPLEMENT THE SIBLINGS TOO, not only the method named above. The documentation defines several quantities over the same state; write `compute_<name>` for each one you can derive from the documentation. They are how this reference earns its standing: the named method is where the disagreement under review lives, so it cannot also be the evidence that the reference is trustworthy. The siblings are.

The siblings that count are exactly: `getFirstMillisecond`, `getLastMillisecond`, `getSerialIndex`, `getWeek`, `hashCode`, `toString`. At least 3 of them must be implemented and agree with the class's real behaviour for this reference to be admitted, so implement every one whose value the documentation lets you derive from the declared state. A counter of internal bookkeeping (how many times something was evaluated or iterated) is not derivable from state — skip those rather than invent them.

First line of your reply must be a comment listing what you wrote, exactly: `// compute(<shared parameter types>) : <name1>, <name2>, ...`. Then the class. No markdown fences, no prose.
```

</details>
<details open><summary>▸ Output (~1,027 chars)</summary>

```
// compute(short year, byte week, long firstMillisecond, long lastMillisecond) : getYearValue, getFirstMillisecond, getLastMillisecond, getSerialIndex, getWeek, hashCode, toString
public class ReferenceImpl {

    public static int compute_getYearValue(short year) {
        return year;
    }

    public static long compute_getFirstMillisecond(long firstMillisecond) {
        return firstMillisecond;
    }

    public static long compute_getLastMillisecond(long lastMillisecond) {
        return lastMillisecond;
    }

    public static long compute_getSerialIndex(short year, byte week) {
        return year * 53L + week;
    }

    public static int compute_getWeek(byte week) {
        return week;
    }

    public static int compute_hashCode(short year, byte week) {
        int result = 17;
        result = 37 * result + week;
        result = 37 * result + year;
        return result;
    }

    public static String compute_toString(short year, byte week) {
        return "Week " + week + ", " + year;
    }
}
```

</details>

---
## [59] ⚙️ reference-impl · `getYearValue`
**output:** **reference generated**
- detail: {'chars': 1027}

---
## [60] ⚙️ reference-impl · `getYearValue`
**output:** **signature declared**
- detail: {'signature': 'short year, byte week, long firstMillisecond, long lastMillisecond'}

---
## [61] ⚙️ reference-impl · `getYearValue`
**output:** **reference observables matched**
- detail: {'matched': {'getYearValue': 'getYearValue', 'getFirstMillisecond': 'getFirstMillisecond', 'getLastMillisecond': 'getLastMillisecond', 'getSerialIndex': 'getSerialIndex', 'getWeek': 'getWeek', 'hashCode': 'hashCode'}, 'declared_only': []}

---
## [62] ⚙️ reference-impl · `getYearValue`
**output:** **screen bar reachable**
- reason: 5 shared sibling observable(s) — enough for the screen to decide

---
## [63] ⚙️ reference-impl · `getYearValue`
**output:** **signature mapped**
- reason: matched 4 parameter(s) to state fields

---
## [64] ⚙️ reference-impl · `getYearValue`
**output:** **twin setup extracted**
- reason: setup extracted; receiver `w`
- detail: {'declaring_types': ['Week']}

---
## [65] ⚙️ reference-impl · `getYearValue`
**output:** **twin built**
- detail: {'helpers': 0, 'imports': 13, 'package': 'org.jfree.data.time.junit', 'receiver': 'w'}

---
## [66] ⚙️ reference-impl · `getYearValue`
**output:** **buggy twin FAILED — DISCARDED**
<details><summary>▸ reason (~1,256 chars)</summary>

- reason: twin did not compile — DISCARDED. javac: /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/reference_twin/StateTwinDriver.java:86: error: variable t is | /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/reference_twin/StateTwinDriver.java:92: error: variable t is | /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/reference_twin/StateTwinDriver.java:98: error: variable t is | /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/reference_twin/StateTwinDriver.java:104: error: variable t i | /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/reference_twin/StateTwinDriver.java:110: error: variable t i | /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/reference_twin/StateTwinDriver.java:116: error: variable t i

</details>

---
## [67] ⚙️ reference-widening · `getYearValue`
**output:** **widening REJECTED for observable `yearvalue`**
<details><summary>▸ reason (~1,288 chars)</summary>

- reason: buggy twin FAILED — DISCARDED — twin did not compile — DISCARDED. javac: /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/reference_twin/StateTwinDriver.java:86: error: variable t is | /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/reference_twin/StateTwinDriver.java:92: error: variable t is | /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/reference_twin/StateTwinDriver.java:98: error: variable t is | /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/reference_twin/StateTwinDriver.java:104: error: variable t i | /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/reference_twin/StateTwinDriver.java:110: error: variable t i | /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/reference_twin/StateTwinDriver.java:116: error: variable t i

</details>
- detail: {'admitted_observables': [], 'last_step': 'buggy twin FAILED — DISCARDED'}

---
## [68] ⚙️ reference-widening · `getYear`
**output:** **reference REQUESTED for observable `year`**
- reason: a kept relation probes this observable and the leg holds no reference for it; the existing generation + screening + admission chain runs unchanged, aimed at this method

---
## [69] ⚙️ reference-impl · `getYear`
**output:** **widening target requested**
- detail: {'candidates': ['getYear'], 'attempting': 'getYear'}

---
## [70] ⚙️ reference-impl · `getYear`
**output:** **screening surface resolved**
- reason: 7 computed sibling observable(s) on the receiver's own type (stored settings excluded — they agree for free); 1 of them PATCH-TOUCHED (['getWeek']) and therefore not screened on — the buggy build is the wrong answer key where the defect lives
- detail: {'siblings': ['getFirstMillisecond', 'getLastMillisecond', 'getSerialIndex', 'getWeek', 'getYearValue', 'hashCode', 'toString'], 'patch_touched_exempted': ['getWeek'], 'screened_siblings': ['getFirstMillisecond', 'getLastMillisecond', 'getSerialIndex', 'getYearValue', 'hashCode', 'toString'], 'declaring_types': ['Week', 'Year']}

---
## [71] 🧠 LLM call — **LLM** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~44,828 chars, 1 new)</summary>

- *[system] message: identical to step [48] — not reprinted*
**[user]**
```
Implement `getYear` from its specification.

You are NOT shown any existing implementation of it, and you must not try to reconstruct one from memory of this project. Derive the behaviour from the documentation and the tests below. Where the documentation names a standard algorithm or formula, use the standard definition of it.

=== DOCUMENTATION (class, method, siblings, inherited) ===
<class name="Week" role="patched">
     * A calendar week.  All years are considered to have 53 weeks, numbered from 1
     * to 53, although in many cases the 53rd week is empty.  Most of the time, the
     * 1st week of the year *begins* in the previous calendar year, but it always
     * finishes in the current year (this behaviour matches the workings of the
     * <code>GregorianCalendar</code> class).
     * <P>
     * This class is immutable, which is a requirement for all
     * {@link RegularTimePeriod} subclasses.
public class Week extends RegularTimePeriod implements Serializable {
     * For serialization.
    private static final long serialVersionUID = 1856387786939865061L;
     * Constant for the first week in the year.
    public static final int FIRST_WEEK_IN_YEAR = 1;
     * Constant for the last week in the year.
    public static final int LAST_WEEK_IN_YEAR = 53;
     * The year in which the week falls.
    private short year;
     * The week (1-53).
    private byte week;
     * The first millisecond.
    private long firstMillisecond;
     * The last millisecond.
    private long lastMillisecond;
     * Creates a new time period for the week in which the current system
     * date/time falls.
    public Week() { /* body withheld */ }
     * Creates a time period representing the week in the specified year.
     * @param week  the week (1 to 53).
     * @param year  the year (1900 to 9999).
    public Week(int week, int year) { /* body withheld */ }
     * Creates a time period representing the week in the specified year.
     * @param week  the week (1 to 53).
     * @param year  the year (1900 to 9999).
    public Week(int week, Year year) { /* body withheld */ }
     * Creates a time period for the week in which the specified date/time
     * falls.
     * @param time  the time (<code>null</code> not permitted).
    public Week(Date time) { /* body withheld */ }
     * Creates a time period for the week in which the specified date/time
     * falls, calculated relative to the specified time zone.
     * @param time  the date/time (<code>null</code> not permitted).
     * @param zone  the time zone (<code>null</code> not permitted).
     * @deprecated As of 1.0.7, use { /* body withheld */ }.
    public Week(Date time, TimeZone zone) { /* body withheld */ }
     * Creates a time period for the week in which the specified date/time
     * falls, calculated relative to the specified time zone.
     * @param time  the date/time (<code>null</code> not permitted).
     * @param zone  the time zone (<code>null</code> not permitted).
     * @param locale  the locale (<code>null</code> not permitted).
     * @since 1.0.7
    public Week(Date time, TimeZone zone, Locale locale) { /* body withheld */ }
     * Returns the year in which the week falls.
     * @return The year (never <code>null</code>).
    public Year getYear() { /* body withheld */ }
     * Returns the year in which the week falls, as an integer value.
     * @return The year.
    public int getYearValue() { /* body withheld */ }
     * Returns the week.
     * @return The week.
    public int getWeek() { /* body withheld */ }
     * Returns the first millisecond of the week.  This will be determined
     * relative to the time zone specified in the constructor, or in the
     * calendar instance passed in the most recent call to the
     * { /* body withheld */ } method.
     * @return The first millisecond of the week.
     * @see #getLastMillisecond()
    public long getFirstMillisecond() { /* body withheld */ }
     * Returns the last millisecond of the week.  This will be
     * determined relative to the time zone specified in the constructor, or
     * in the calendar instance passed in the most recent call to the
     * { /* body withheld */ } method.
     * @return The last millisecond of the week.
     * @see #getFirstMillisecond()
    public long getLastMillisecond() { /* body withheld */ }
     * Recalculates the start date/time and end date/time for this time period
     * relative to the supplied calendar (which incorporates a time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @since 1.0.3
    public void peg(Calendar calendar) { /* body withheld */ }
     * Returns the week preceding this one.  This method will return
     * <code>null</code> for some lower limit on the range of weeks (currently
     * week 1, 1900).  For week 1 of any year, the previous week is always week
     * 53, but week 53 may not contain any days (you should check for this).
     * @return The preceding week (possibly <code>null</code>).
    public RegularTimePeriod previous() { /* body withheld */ }
     * Returns the week following this one.  This method will return
     * <code>null</code> for some upper limit on the range of weeks (currently
     * week 53, 9999).  For week 52 of any year, the following week is always
     * week 53, but week 53 may not contain any days (you should check for
     * this).
     * @return The following week (possibly <code>null</code>).
    public RegularTimePeriod next() { /* body withheld */ }
     * Returns a serial index number for the week.
     * @return The serial index number.
    public long getSerialIndex() { /* body withheld */ }
     * Returns the first millisecond of the week, evaluated using the supplied
     * calendar (which determines the time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @return The first millisecond of the week.
     * @throws NullPointerException if <code>calendar</code> is
     * <code>null</code>.
    public long getFirstMillisecond(Calendar calendar) { /* body withheld */ }
     * Returns a string representing the week (e.g. "Week 9, 2002").
     * TODO: look at internationalisation.
     * @return A string representing the week.
    public String toString() { /* body withheld */ }
     * Tests the equality of this Week object to an arbitrary object.  Returns
     * true if the target is a Week instance representing the same week as this
     * object.  In all other cases, returns false.
     * @param obj  the object (<code>null</code> permitted).
     * @return <code>true</code> if week and year of this and object are the
     * same.
    public boolean equals(Object obj) { /* body withheld */ }
     * Returns a hash code for this object instance.  The approach described by
     * Joshua Bloch in "Effective Java" has been used here:
     * <p>
     * <code>http://developer.java.sun.com/developer/Books/effectivejava
     * /Chapter3.pdf</code>
     * @return A hash code.
    public int hashCode() { /* body withheld */ }
     * Returns an integer indicating the order of this Week object relative to
     * the specified object:
     * negative == before, zero == same, positive == after.
     * @param o1  the object to compare.
     * @return negative == before, zero == same, positive == after.
    public int compareTo(Object o1) { /* body withheld */ }
     * Parses the string argument as a week.
     * <P>
     * This method is required to accept the format "YYYY-Wnn".  It will also
     * accept "Wnn-YYYY". Anything else, at the moment, is a bonus.
     * @param s  string to parse.
     * @return <code>null</code> if the string is not parseable, the week
     * otherwise.
    public static Week parseWeek(String s) { /* body withheld */ }
     * Finds the first occurrence of ' ', '-', ',' or '.'
     * @param s  the string to parse.
     * @return <code>-1</code> if none of the characters was found, the
     * index of the first occurrence otherwise.
    private static int findSeparator(String s) { /* body withheld */ }
     * Creates a year from a string, or returns null (format exceptions
     * suppressed).
     * @param s  string to parse.
     * @return <code>null</code> if the string is not parseable, the year
     * otherwise.
    private static Year evaluateAsYear(String s) { /* body withheld */ }
     * Converts a string to a week.
     * @param s  the string to parse.
     * @return <code>-1</code> if the string does not contain a week number,
     * the number of the week otherwise.
    private static int stringToWeek(String s) { /* body withheld */ }
}
</class>

<class name="RegularTimePeriod" role="supertype-contract">
     * An abstract class representing a unit of time.  Convenient methods are
     * provided for calculating the next and previous time periods.  Conversion
     * methods are defined that return the first and last milliseconds of the time
     * period.  The results from these methods are timezone dependent.
     * <P>
     * This class is immutable, and all subclasses should be immutable also.
public abstract class RegularTimePeriod implements TimePeriod, Comparable,
                                                   MonthConstants {
     * Creates a time period that includes the specified millisecond, assuming
     * the given time zone.
     * @param c  the time period class.
     * @param millisecond  the time.
     * @param zone  the time zone.
     * @return The time period.
    public static RegularTimePeriod createInstance(Class c, Date millisecond, TimeZone zone) { /* body withheld */ }
     * Returns a subclass of { /* body withheld */ } that is smaller than
     * the specified class.
     * @param c  a subclass of {@link RegularTimePeriod}.
     * @return A class.
    public static Class downsize(Class c) { … }
     * Returns the time period preceding this one, or <code>null</code> if some
     * lower limit has been reached.
     * @return The previous time period (possibly <code>null</code>).
    public abstract RegularTimePeriod previous();
     * Returns the time period following this one, or <code>null</code> if some
     * limit has been reached.
     * @return The next time period (possibly <code>null</code>).
    public abstract RegularTimePeriod next();
     * Returns a serial index number for the time unit.
     * @return The serial index number.
    public abstract long getSerialIndex();
     * The default time zone.
    public static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getDefault();
     * Recalculates the start date/time and end date/time for this time period
     * relative to the supplied calendar (which incorporates a time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @since 1.0.3
    public abstract void peg(Calendar calendar);
     * Returns the date/time that marks the start of the time period.  This
     * method returns a new <code>Date</code> instance every time it is called.
     * @return The start date/time.
     * @see #getFirstMillisecond()
    public Date getStart() { /* body withheld */ }
     * Returns the date/time that marks the end of the time period.  This
     * method returns a new <code>Date</code> instance every time it is called.
     * @return The end date/time.
     * @see #getLastMillisecond()
    public Date getEnd() { /* body withheld */ }
     * Returns the first millisecond of the time period.  This will be
     * determined relative to the time zone specified in the constructor, or
     * in the calendar instance passed in the most recent call to the
     * { /* body withheld */ } method.
     * @return The first millisecond of the time period.
     * @see #getLastMillisecond()
    public abstract long getFirstMillisecond();
     * Returns the first millisecond of the time period, evaluated using the
     * supplied calendar (which incorporates a timezone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @return The first millisecond of the time period.
     * @throws NullPointerException if <code>calendar,/code> is
     * </code>null</code>.
     * @see #getLastMillisecond(Calendar)
    public abstract long getFirstMillisecond(Calendar calendar);
     * Returns the last millisecond of the time period.  This will be
     * determined relative to the time zone specified in the constructor, or
     * in the calendar instance passed in the most recent call to the
     * { /* body withheld */ } method.
     * @return The last millisecond of the time period.
     * @see #getFirstMillisecond()
    public abstract long getLastMillisecond();
     * Returns the last millisecond of the time period, evaluated using the
     * supplied calendar (which incorporates a timezone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @return The last millisecond of the time period.
     * @see #getFirstMillisecond(Calendar)
    public abstract long getLastMillisecond(Calendar calendar);
     * Returns the millisecond closest to the middle of the time period.
     * @return The middle millisecond.
    public long getMiddleMillisecond() { /* body withheld */ }
     * Returns the millisecond closest to the middle of the time period,
     * evaluated using the supplied calendar (which incorporates a timezone).
     * @param calendar  the calendar.
     * @return The middle millisecond.
    public long getMiddleMillisecond(Calendar calendar) { /* body withheld */ }
     * Returns a string representation of the time period.
     * @return The string.
    public String toString() { /* body withheld */ }
}
</class>

<class name="Year" role="collaborator">
     * Represents a year in the range 1900 to 9999.  This class is immutable, which
     * is a requirement for all {@link RegularTimePeriod} subclasses.
public class Year extends RegularTimePeriod implements Serializable {
     * For serialization.
    private static final long serialVersionUID = -7659990929736074836L;
     * The year.
    private short year;
     * The first millisecond.
    private long firstMillisecond;
     * The last millisecond.
    private long lastMillisecond;
     * Creates a new <code>Year</code>, based on the current system date/time.
    public Year() { /* body withheld */ }
     * Creates a time period representing a single year.
     * @param year  the year.
    public Year(int year) { /* body withheld */ }
     * Creates a new <code>Year</code>, based on a particular instant in time,
     * using the default time zone.
     * @param time  the time (<code>null</code> not permitted).
    public Year(Date time) { /* body withheld */ }
     * Constructs a year, based on a particular instant in time and a time zone.
     * @param time  the time.
     * @param zone  the time zone.
    public Year(Date time, TimeZone zone) { /* body withheld */ }
     * Returns the year.
     * @return The year.
    public int getYear() { /* body withheld */ }
     * Returns the first millisecond of the year.  This will be determined
     * relative to the time zone specified in the constructor, or in the
     * calendar instance passed in the most recent call to the
     * { /* body withheld */ } method.
     * @return The first millisecond of the year.
     * @see #getLastMillisecond()
    public long getFirstMillisecond() { /* body withheld */ }
     * Returns the last millisecond of the year.  This will be
     * determined relative to the time zone specified in the constructor, or
     * in the calendar instance passed in the most recent call to the
     * { /* body withheld */ } method.
     * @return The last millisecond of the year.
     * @see #getFirstMillisecond()
    public long getLastMillisecond() { /* body withheld */ }
     * Recalculates the start date/time and end date/time for this time period
     * relative to the supplied calendar (which incorporates a time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @since 1.0.3
    public void peg(Calendar calendar) { /* body withheld */ }
     * Returns the year preceding this one.
     * @return The year preceding this one (or <code>null</code> if the
     * current year is 1900).
    public RegularTimePeriod previous() { /* body withheld */ }
     * Returns the year following this one.
     * @return The year following this one (or <code>null</code> if the current
     * year is 9999).
    public RegularTimePeriod next() { /* body withheld */ }
     * Returns a serial index number for the year.
     * <P>
     * The implementation simply returns the year number (e.g. 2002).
     * @return The serial index number.
    public long getSerialIndex() { /* body withheld */ }
     * Returns the first millisecond of the year, evaluated using the supplied
     * calendar (which determines the time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @return The first millisecond of the year.
     * @throws NullPointerException if <code>calendar</code> is
     * <code>null</code>.
    public long getFirstMillisecond(Calendar calendar) { /* body withheld */ }
     * Returns the last millisecond of the year, evaluated using the supplied
     * calendar (which determines the time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @return The last millisecond of the year.
     * @throws NullPointerException if <code>calendar</code> is
     * <code>null</code>.
    public long getLastMillisecond(Calendar calendar) { /* body withheld */ }
     * Tests the equality of this <code>Year</code> object to an arbitrary
     * object.  Returns <code>true</code> if the target is a <code>Year</code>
     * instance representing the same year as this object.  In all other cases,
     * returns <code>false</code>.
     * @param object  the object (<code>null</code> permitted).
     * @return <code>true</code> if the year of this and the object are the
     * same.
    public boolean equals(Object object) { /* body withheld */ }
     * Returns a hash code for this object instance.  The approach described by
     * Joshua Bloch in "Effective Java" has been used here:
     * <p>
     * <code>http://developer.java.sun.com/developer/Books/effectivejava
     * /Chapter3.pdf</code>
     * @return A hash code.
    public int hashCode() { /* body withheld */ }
     * Returns an integer indicating the order of this <code>Year</code> object
     * relative to the specified object:
     * negative == before, zero == same, positive == after.
     * @param o1  the object to compare.
     * @return negative == before, zero == same, positive == after.
    public int compareTo(Object o1) { /* body withheld */ }
     * Returns a string representing the year..
     * @return A string representing the year.
    public String toString() { /* body withheld */ }
     * Parses the string argument as a year.
     * <P>
     * The string format is YYYY.
     * @param s  a string representing the year.
     * @return <code>null</code> if the string is not parseable, the year
     * otherwise.
    public static Year parseYear(String s) { /* body withheld */ }
}
</class>

<class name="TimePeriodFormatException" role="collaborator">
     * An exception that indicates an invalid format in a string representing a
     * time period.
public class TimePeriodFormatException extends IllegalArgumentException {
     * Creates a new exception.
     * @param message  a message describing the exception.
    public TimePeriodFormatException(String message) { /* body withheld */ }
}
</class>

=== THE FAILING TEST, in full — it pins correct behaviour at its own inputs ===
    public void testConstructor() {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        Locale.setDefault(new Locale("da", "DK"));
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
        GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                TimeZone.getDefault(), Locale.getDefault());

        // first day of week is monday
        assertEquals(Calendar.MONDAY, cal.getFirstDayOfWeek());
        cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date t = cal.getTime();
        Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
        assertEquals(34, w.getWeek());

        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
        cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());
        // first day of week is Sunday
        assertEquals(Calendar.SUNDAY, cal.getFirstDayOfWeek());
        cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);

        t = cal.getTime();
        w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
        assertEquals(35, w.getWeek());
        w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                new Locale("da", "DK"));
        assertEquals(34, w.getWeek());

        Locale.setDefault(savedLocale);
        TimeZone.setDefault(savedZone);
    }

=== SKELETON (signatures and fields; bodies are withheld) ===
<class name="Week" role="patched">
     * A calendar week.  All years are considered to have 53 weeks, numbered from 1
     * to 53, although in many cases the 53rd week is empty.  Most of the time, the
     * 1st week of the year *begins* in the previous calendar year, but it always
     * finishes in the current year (this behaviour matches the workings of the
     * <code>GregorianCalendar</code> class).
     * <P>
     * This class is immutable, which is a requirement for all
     * {@link RegularTimePeriod} subclasses.
public class Week extends RegularTimePeriod implements Serializable {
     * For serialization.
    private static final long serialVersionUID = 1856387786939865061L;
     * Constant for the first week in the year.
    public static final int FIRST_WEEK_IN_YEAR = 1;
     * Constant for the last week in the year.
    public static final int LAST_WEEK_IN_YEAR = 53;
     * The year in which the week falls.
    private short year;
     * The week (1-53).
    private byte week;
     * The first millisecond.
    private long firstMillisecond;
     * The last millisecond.
    private long lastMillisecond;
     * Creates a new time period for the week in which the current system
     * date/time falls.
    public Week() { /* body withheld */ }
     * Creates a time period representing the week in the specified year.
     * @param week  the week (1 to 53).
     * @param year  the year (1900 to 9999).
    public Week(int week, int year) { /* body withheld */ }
     * Creates a time period representing the week in the specified year.
     * @param week  the week (1 to 53).
     * @param year  the year (1900 to 9999).
    public Week(int week, Year year) { /* body withheld */ }
     * Creates a time period for the week in which the specified date/time
     * falls.
     * @param time  the time (<code>null</code> not permitted).
    public Week(Date time) { /* body withheld */ }
     * Creates a time period for the week in which the specified date/time
     * falls, calculated relative to the specified time zone.
     * @param time  the date/time (<code>null</code> not permitted).
     * @param zone  the time zone (<code>null</code> not permitted).
     * @deprecated As of 1.0.7, use { /* body withheld */ }.
    public Week(Date time, TimeZone zone) { /* body withheld */ }
     * Creates a time period for the week in which the specified date/time
     * falls, calculated relative to the specified time zone.
     * @param time  the date/time (<code>null</code> not permitted).
     * @param zone  the time zone (<code>null</code> not permitted).
     * @param locale  the locale (<code>null</code> not permitted).
     * @since 1.0.7
    public Week(Date time, TimeZone zone, Locale locale) { /* body withheld */ }
     * Returns the year in which the week falls.
     * @return The year (never <code>null</code>).
    public Year getYear() { /* body withheld */ }
     * Returns the year in which the week falls, as an integer value.
     * @return The year.
    public int getYearValue() { /* body withheld */ }
     * Returns the week.
     * @return The week.
    public int getWeek() { /* body withheld */ }
     * Returns the first millisecond of the week.  This will be determined
     * relative to the time zone specified in the constructor, or in the
     * calendar instance passed in the most recent call to the
     * { /* body withheld */ } method.
     * @return The first millisecond of the week.
     * @see #getLastMillisecond()
    public long getFirstMillisecond() { /* body withheld */ }
     * Returns the last millisecond of the week.  This will be
     * determined relative to the time zone specified in the constructor, or
     * in the calendar instance passed in the most recent call to the
     * { /* body withheld */ } method.
     * @return The last millisecond of the week.
     * @see #getFirstMillisecond()
    public long getLastMillisecond() { /* body withheld */ }
     * Recalculates the start date/time and end date/time for this time period
     * relative to the supplied calendar (which incorporates a time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @since 1.0.3
    public void peg(Calendar calendar) { /* body withheld */ }
     * Returns the week preceding this one.  This method will return
     * <code>null</code> for some lower limit on the range of weeks (currently
     * week 1, 1900).  For week 1 of any year, the previous week is always week
     * 53, but week 53 may not contain any days (you should check for this).
     * @return The preceding week (possibly <code>null</code>).
    public RegularTimePeriod previous() { /* body withheld */ }
     * Returns the week following this one.  This method will return
     * <code>null</code> for some upper limit on the range of weeks (currently
     * week 53, 9999).  For week 52 of any year, the following week is always
     * week 53, but week 53 may not contain any days (you should check for
     * this).
     * @return The following week (possibly <code>null</code>).
    public RegularTimePeriod next() { /* body withheld */ }
     * Returns a serial index number for the week.
     * @return The serial index number.
    public long getSerialIndex() { /* body withheld */ }
     * Returns the first millisecond of the week, evaluated using the supplied
     * calendar (which determines the time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @return The first millisecond of the week.
     * @throws NullPointerException if <code>calendar</code> is
     * <code>null</code>.
    public long getFirstMillisecond(Calendar calendar) { /* body withheld */ }
     * Returns a string representing the week (e.g. "Week 9, 2002").
     * TODO: look at internationalisation.
     * @return A string representing the week.
    public String toString() { /* body withheld */ }
     * Tests the equality of this Week object to an arbitrary object.  Returns
     * true if the target is a Week instance representing the same week as this
     * object.  In all other cases, returns false.
     * @param obj  the object (<code>null</code> permitted).
     * @return <code>true</code> if week and year of this and object are the
     * same.
    public boolean equals(Object obj) { /* body withheld */ }
     * Returns a hash code for this object instance.  The approach described by
     * Joshua Bloch in "Effective Java" has been used here:
     * <p>
     * <code>http://developer.java.sun.com/developer/Books/effectivejava
     * /Chapter3.pdf</code>
     * @return A hash code.
    public int hashCode() { /* body withheld */ }
     * Returns an integer indicating the order of this Week object relative to
     * the specified object:
     * negative == before, zero == same, positive == after.
     * @param o1  the object to compare.
     * @return negative == before, zero == same, positive == after.
    public int compareTo(Object o1) { /* body withheld */ }
     * Parses the string argument as a week.
     * <P>
     * This method is required to accept the format "YYYY-Wnn".  It will also
     * accept "Wnn-YYYY". Anything else, at the moment, is a bonus.
     * @param s  string to parse.
     * @return <code>null</code> if the string is not parseable, the week
     * otherwise.
    public static Week parseWeek(String s) { /* body withheld */ }
     * Finds the first occurrence of ' ', '-', ',' or '.'
     * @param s  the string to parse.
     * @return <code>-1</code> if none of the characters was found, the
     * index of the first occurrence otherwise.
    private static int findSeparator(String s) { /* body withheld */ }
     * Creates a year from a string, or returns null (format exceptions
     * suppressed).
     * @param s  string to parse.
     * @return <code>null</code> if the string is not parseable, the year
     * otherwise.
    private static Year evaluateAsYear(String s) { /* body withheld */ }
     * Converts a string to a week.
     * @param s  the string to parse.
     * @return <code>-1</code> if the string does not contain a week number,
     * the number of the week otherwise.
    private static int stringToWeek(String s) { /* body withheld */ }
}
</class>

<class name="RegularTimePeriod" role="supertype-contract">
     * An abstract class representing a unit of time.  Convenient methods are
     * provided for calculating the next and previous time periods.  Conversion
     * methods are defined that return the first and last milliseconds of the time
     * period.  The results from these methods are timezone dependent.
     * <P>
     * This class is immutable, and all subclasses should be immutable also.
public abstract class RegularTimePeriod implements TimePeriod, Comparable,
                                                   MonthConstants {
     * Creates a time period that includes the specified millisecond, assuming
     * the given time zone.
     * @param c  the time period class.
     * @param millisecond  the time.
     * @param zone  the time zone.
     * @return The time period.
    public static RegularTimePeriod createInstance(Class c, Date millisecond, TimeZone zone) { /* body withheld */ }
     * Returns a subclass of { /* body withheld */ } that is smaller than
     * the specified class.
     * @param c  a subclass of {@link RegularTimePeriod}.
     * @return A class.
    public static Class downsize(Class c) { … }
     * Returns the time period preceding this one, or <code>null</code> if some
     * lower limit has been reached.
     * @return The previous time period (possibly <code>null</code>).
    public abstract RegularTimePeriod previous();
     * Returns the time period following this one, or <code>null</code> if some
     * limit has been reached.
     * @return The next time period (possibly <code>null</code>).
    public abstract RegularTimePeriod next();
     * Returns a serial index number for the time unit.
     * @return The serial index number.
    public abstract long getSerialIndex();
     * The default time zone.
    public static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getDefault();
     * Recalculates the start date/time and end date/time for this time period
     * relative to the supplied calendar (which incorporates a time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @since 1.0.3
    public abstract void peg(Calendar calendar);
     * Returns the date/time that marks the start of the time period.  This
     * method returns a new <code>Date</code> instance every time it is called.
     * @return The start date/time.
     * @see #getFirstMillisecond()
    public Date getStart() { /* body withheld */ }
     * Returns the date/time that marks the end of the time period.  This
     * method returns a new <code>Date</code> instance every time it is called.
     * @return The end date/time.
     * @see #getLastMillisecond()
    public Date getEnd() { /* body withheld */ }
     * Returns the first millisecond of the time period.  This will be
     * determined relative to the time zone specified in the constructor, or
     * in the calendar instance passed in the most recent call to the
     * { /* body withheld */ } method.
     * @return The first millisecond of the time period.
     * @see #getLastMillisecond()
    public abstract long getFirstMillisecond();
     * Returns the first millisecond of the time period, evaluated using the
     * supplied calendar (which incorporates a timezone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @return The first millisecond of the time period.
     * @throws NullPointerException if <code>calendar,/code> is
     * </code>null</code>.
     * @see #getLastMillisecond(Calendar)
    public abstract long getFirstMillisecond(Calendar calendar);
     * Returns the last millisecond of the time period.  This will be
     * determined relative to the time zone specified in the constructor, or
     * in the calendar instance passed in the most recent call to the
     * { /* body withheld */ } method.
     * @return The last millisecond of the time period.
     * @see #getFirstMillisecond()
    public abstract long getLastMillisecond();
     * Returns the last millisecond of the time period, evaluated using the
     * supplied calendar (which incorporates a timezone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @return The last millisecond of the time period.
     * @see #getFirstMillisecond(Calendar)
    public abstract long getLastMillisecond(Calendar calendar);
     * Returns the millisecond closest to the middle of the time period.
     * @return The middle millisecond.
    public long getMiddleMillisecond() { /* body withheld */ }
     * Returns the millisecond closest to the middle of the time period,
     * evaluated using the supplied calendar (which incorporates a timezone).
     * @param calendar  the calendar.
     * @return The middle millisecond.
    public long getMiddleMillisecond(Calendar calendar) { /* body withheld */ }
     * Returns a string representation of the time period.
     * @return The string.
    public String toString() { /* body withheld */ }
}
</class>

<class name="Year" role="collaborator">
     * Represents a year in the range 1900 to 9999.  This class is immutable, which
     * is a requirement for all {@link RegularTimePeriod} subclasses.
public class Year extends RegularTimePeriod implements Serializable {
     * For serialization.
    private static final long serialVersionUID = -7659990929736074836L;
     * The year.
    private short year;
     * The first millisecond.
    private long firstMillisecond;
     * The last millisecond.
    private long lastMillisecond;
     * Creates a new <code>Year</code>, based on the current system date/time.
    public Year() { /* body withheld */ }
     * Creates a time period representing a single year.
     * @param year  the year.
    public Year(int year) { /* body withheld */ }
     * Creates a new <code>Year</code>, based on a particular instant in time,
     * using the default time zone.
     * @param time  the time (<code>null</code> not permitted).
    public Year(Date time) { /* body withheld */ }
     * Constructs a year, based on a particular instant in time and a time zone.
     * @param time  the time.
     * @param zone  the time zone.
    public Year(Date time, TimeZone zone) { /* body withheld */ }
     * Returns the year.
     * @return The year.
    public int getYear() { /* body withheld */ }
     * Returns the first millisecond of the year.  This will be determined
     * relative to the time zone specified in the constructor, or in the
     * calendar instance passed in the most recent call to the
     * { /* body withheld */ } method.
     * @return The first millisecond of the year.
     * @see #getLastMillisecond()
    public long getFirstMillisecond() { /* body withheld */ }
     * Returns the last millisecond of the year.  This will be
     * determined relative to the time zone specified in the constructor, or
     * in the calendar instance passed in the most recent call to the
     * { /* body withheld */ } method.
     * @return The last millisecond of the year.
     * @see #getFirstMillisecond()
    public long getLastMillisecond() { /* body withheld */ }
     * Recalculates the start date/time and end date/time for this time period
     * relative to the supplied calendar (which incorporates a time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @since 1.0.3
    public void peg(Calendar calendar) { /* body withheld */ }
     * Returns the year preceding this one.
     * @return The year preceding this one (or <code>null</code> if the
     * current year is 1900).
    public RegularTimePeriod previous() { /* body withheld */ }
     * Returns the year following this one.
     * @return The year following this one (or <code>null</code> if the current
     * year is 9999).
    public RegularTimePeriod next() { /* body withheld */ }
     * Returns a serial index number for the year.
     * <P>
     * The implementation simply returns the year number (e.g. 2002).
     * @return The serial index number.
    public long getSerialIndex() { /* body withheld */ }
     * Returns the first millisecond of the year, evaluated using the supplied
     * calendar (which determines the time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @return The first millisecond of the year.
     * @throws NullPointerException if <code>calendar</code> is
     * <code>null</code>.
    public long getFirstMillisecond(Calendar calendar) { /* body withheld */ }
     * Returns the last millisecond of the year, evaluated using the supplied
     * calendar (which determines the time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @return The last millisecond of the year.
     * @throws NullPointerException if <code>calendar</code> is
     * <code>null</code>.
    public long getLastMillisecond(Calendar calendar) { /* body withheld */ }
     * Tests the equality of this <code>Year</code> object to an arbitrary
     * object.  Returns <code>true</code> if the target is a <code>Year</code>
     * instance representing the same year as this object.  In all other cases,
     * returns <code>false</code>.
     * @param object  the object (<code>null</code> permitted).
     * @return <code>true</code> if the year of this and the object are the
     * same.
    public boolean equals(Object object) { /* body withheld */ }
     * Returns a hash code for this object instance.  The approach described by
     * Joshua Bloch in "Effective Java" has been used here:
     * <p>
     * <code>http://developer.java.sun.com/developer/Books/effectivejava
     * /Chapter3.pdf</code>
     * @return A hash code.
    public int hashCode() { /* body withheld */ }
     * Returns an integer indicating the order of this <code>Year</code> object
     * relative to the specified object:
     * negative == before, zero == same, positive == after.
     * @param o1  the object to compare.
     * @return negative == before, zero == same, positive == after.
    public int compareTo(Object o1) { /* body withheld */ }
     * Returns a string representing the year..
     * @return A string representing the year.
    public String toString() { /* body withheld */ }
     * Parses the string argument as a year.
     * <P>
     * The string format is YYYY.
     * @param s  a string representing the year.
     * @return <code>null</code> if the string is not parseable, the year
     * otherwise.
    public static Year parseYear(String s) { /* body withheld */ }
}
</class>

<class name="TimePeriodFormatException" role="collaborator">
     * An exception that indicates an invalid format in a string representing a
     * time period.
public class TimePeriodFormatException extends IllegalArgumentException {
     * Creates a new exception.
     * @param message  a message describing the exception.
    public TimePeriodFormatException(String message) { /* body withheld */ }
}
</class>

<class name="TimePeriod" role="collaborator">
     * A period of time measured to millisecond precision using two instances of
     * <code>java.util.Date</code>.
public interface TimePeriod extends Comparable {
     * Returns the start date/time.  This will always be on or before the
     * end date.
     * @return The start date/time (never <code>null</code>).
    public Date getStart();
     * Returns the end date/time.  This will always be on or after the
     * start date.
     * @return The end date/time (never <code>null</code>).
    public Date getEnd();
}
</class>

<class name="JFreeChart" role="collaborator">
     * A chart class implemented using the Java 2D APIs.  The current version
     * supports bar charts, line charts, pie charts and xy plots (including time
     * series data).
     * <P>
     * JFreeChart coordinates several objects to achieve its aim of being able to
     * draw a chart on a Java 2D graphics device: a list of { /* body withheld */ } objects
     * (which often includes the chart's legend), a { /* body withheld */ } and a
     * { /* body withheld */ } (the plot in turn manages a
     * domain axis and a range axis).
     * <P>
     * You should use a { /* body withheld */ } to display a chart in a GUI.
     * <P>
     * …
public class JFreeChart implements Drawable,
                                   TitleChangeListener,
                                   PlotChangeListener,
                                   Serializable,
                                   Cloneable { /* body withheld */ }
// … (non-patched members truncated; patched methods kept)
</class>

<class name="Contributor" role="collaborator">
     * A simple class representing a contributor to a software project.
public class Contributor {
     * The name.
    private String name;
     * The e-mail address.
    private String email;
     * Creates a new contributor.
     * @param name  the name.
     * @param email  the e-mail address.
    public Contributor(String name, String email) { /* body withheld */ }
     * Returns the contributor's name.
     * @return the contributor's name.
    public String getName() { /* body withheld */ }
     * Returns the contributor's e-mail address.
     * @return the contributor's e-mail address.
    public String getEmail() { /* body withheld */ }
}
</class>

Write ONE self-contained Java class named `ReferenceImpl` whose PUBLIC STATIC methods are named `compute_<observable>`.

FUNCTIONALIZE IT. These methods may read object state rather than take arguments; each `compute_<name>` must take that state as PARAMETERS and be pure — same inputs, same result, no fields, no I/O. Declare exactly the data the computation needs, in the SAME parameter order for every one, and return the documented type. A reference that reads no input cannot be run on different inputs, and one that cannot be varied cannot be checked.

NAME each parameter after the state it represents, using the name the class uses for it, and repeat those names in the declaration line below. Take only state the computation actually consumes: a parameter for something the quantity does not depend on cannot be supplied and the reference will be discarded unused.

IMPLEMENT THE SIBLINGS TOO, not only the method named above. The documentation defines several quantities over the same state; write `compute_<name>` for each one you can derive from the documentation. They are how this reference earns its standing: the named method is where the disagreement under review lives, so it cannot also be the evidence that the reference is trustworthy. The siblings are.

The siblings that count are exactly: `getFirstMillisecond`, `getLastMillisecond`, `getSerialIndex`, `getWeek`, `getYearValue`, `hashCode`, `toString`. At least 3 of them must be implemented and agree with the class's real behaviour for this reference to be admitted, so implement every one whose value the documentation lets you derive from the declared state. A counter of internal bookkeeping (how many times something was evaluated or iterated) is not derivable from state — skip those rather than invent them.

First line of your reply must be a comment listing what you wrote, exactly: `// compute(<shared parameter types>) : <name1>, <name2>, ...`. Then the class. No markdown fences, no prose.
```

</details>
<details open><summary>▸ Output (~1,566 chars)</summary>

```
// compute(short year, byte week) : getYear, getYearValue, getWeek, getSerialIndex, hashCode, toString
public class ReferenceImpl {

    public static final class Year {
        private final short year;

        public Year(int year) {
            this.year = (short) year;
        }

        public int getYear() {
            return this.year;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Year)) {
                return false;
            }
            Year that = (Year) obj;
            return this.year == that.year;
        }

        public int hashCode() {
            return this.year;
        }

        public String toString() {
            return String.valueOf(this.year);
        }
    }

    public static Year compute_getYear(short year, byte week) {
        return new Year(year);
    }

    public static int compute_getYearValue(short year, byte week) {
        return year;
    }

    public static int compute_getWeek(short year, byte week) {
        return week;
    }

    public static long compute_getSerialIndex(short year, byte week) {
        return (long) year * 53L + (long) week;
    }

    public static int compute_hashCode(short year, byte week) {
        int result = 17;
        result = 37 * result + week;
        result = 37 * result + year;
        return result;
    }

    public static String compute_toString(short year, byte week) {
        return "Week " + ((int) week) + ", " + ((int) year);
    }
}
```

</details>

---
## [72] ⚙️ reference-impl · `getYear`
**output:** **reference generated**
- detail: {'chars': 1566}

---
## [73] ⚙️ reference-impl · `getYear`
**output:** **signature declared**
- detail: {'signature': 'short year, byte week'}

---
## [74] ⚙️ reference-impl · `getYear`
**output:** **reference observables matched**
- detail: {'matched': {'getYear': 'getYear', 'getSerialIndex': 'getSerialIndex', 'getWeek': 'getWeek', 'getYearValue': 'getYearValue', 'hashCode': 'hashCode', 'toString': 'toString'}, 'declared_only': []}

---
## [75] ⚙️ reference-impl · `getYear`
**output:** **screen bar reachable**
- reason: 4 shared sibling observable(s) — enough for the screen to decide

---
## [76] ⚙️ reference-impl · `getYear`
**output:** **signature mapped**
- reason: matched 2 parameter(s) to state fields

---
## [77] ⚙️ reference-impl · `getYear`
**output:** **twin setup extracted**
- reason: setup extracted; receiver `w`
- detail: {'declaring_types': ['Week', 'Year']}

---
## [78] ⚙️ reference-impl · `getYear`
**output:** **twin built**
- detail: {'helpers': 0, 'imports': 13, 'package': 'org.jfree.data.time.junit', 'receiver': 'w'}

---
## [79] ⚙️ reference-impl · `getYear`
**output:** **buggy twin FAILED — DISCARDED**
<details><summary>▸ reason (~1,256 chars)</summary>

- reason: twin did not compile — DISCARDED. javac: /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/reference_twin/StateTwinDriver.java:86: error: variable t is | /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/reference_twin/StateTwinDriver.java:92: error: variable t is | /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/reference_twin/StateTwinDriver.java:98: error: variable t is | /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/reference_twin/StateTwinDriver.java:104: error: variable t i | /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/reference_twin/StateTwinDriver.java:110: error: variable t i | /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/reference_twin/StateTwinDriver.java:116: error: variable t i

</details>

---
## [80] ⚙️ reference-widening · `getYear`
**output:** **widening REJECTED for observable `year`**
<details><summary>▸ reason (~1,288 chars)</summary>

- reason: buggy twin FAILED — DISCARDED — twin did not compile — DISCARDED. javac: /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/reference_twin/StateTwinDriver.java:86: error: variable t is | /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/reference_twin/StateTwinDriver.java:92: error: variable t is | /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/reference_twin/StateTwinDriver.java:98: error: variable t is | /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/reference_twin/StateTwinDriver.java:104: error: variable t i | /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/reference_twin/StateTwinDriver.java:110: error: variable t i | /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/reference_twin/StateTwinDriver.java:116: error: variable t i

</details>
- detail: {'admitted_observables': [], 'last_step': 'buggy twin FAILED — DISCARDED'}

---
## [81] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~30,161 chars)</summary>

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
- Package: `org.jfree.data.time` (`package org.jfree.data.time;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Chart_8_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /source/org/jfree/data/time/Week.java
+++ /source/org/jfree/data/time/Week.java
@@ -172,7 +172,7 @@
      */
     public Week(Date time, TimeZone zone) {
         // defer argument checking...
-        this(time, RegularTimePeriod.DEFAULT_TIME_ZONE, Locale.getDefault());
+       this(time, zone, Locale.getDefault());
     }
 
     /**

</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
</source_imports>

Function `Week`:
<signature>
public Week(Date time, TimeZone zone)
</signature>
<code>
    public Week(Date time, TimeZone zone) {
        // defer argument checking...
        this(time, RegularTimePeriod.DEFAULT_TIME_ZONE, Locale.getDefault());
    }
</code>

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  getFirstMillisecond() / (Calendar calendar)
  getLastMillisecond() / (Calendar calendar)

METHOD FAMILIES over the same input space (factory/parser siblings — where the docs state a selection or agreement rule between family members, equivalent inputs must respect it):
  get* family: getFirstMillisecond, getLastMillisecond, getSerialIndex, getWeek, getYear, getYearValue

STATE YOU CAN READ (public no-argument readers — capture them BEFORE and AFTER a call documented as read-only or non-mutating; an unexplained change is a hidden-state violation): getFirstMillisecond, getLastMillisecond, getSerialIndex, getWeek, getYear, getYearValue, hashCode, next, previous, toString

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
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `WeekTests`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
Lift the call and the expected value from THIS test (this attempt's target):
<failing_test class="org.jfree.data.time.junit.WeekTests" method="testConstructor">
    public void testConstructor() {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        Locale.setDefault(new Locale("da", "DK"));
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
        GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                TimeZone.getDefault(), Locale.getDefault());

        // first day of week is monday
        assertEquals(Calendar.MONDAY, cal.getFirstDayOfWeek());
        cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date t = cal.getTime();
        Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
        assertEquals(34, w.getWeek());

        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
        cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());
        // first day of week is Sunday
        assertEquals(Calendar.SUNDAY, cal.getFirstDayOfWeek());
        cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);

        t = cal.getTime();
        w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
        assertEquals(35, w.getWeek());
        w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                new Locale("da", "DK"));
        assertEquals(34, w.getWeek());

        Locale.setDefault(savedLocale);
        TimeZone.setDefault(savedZone);
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.jfree.data.time.junit.WeekTests::testConstructor
junit.framework.AssertionFailedError: expected:<35> but was:<34>
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.
The test method depends on the following from its test class — setUp, helper methods, constants, fixture files. Replicate this environment EXACTLY (register the same files, wire the same providers, use the same constants); do NOT improvise setup the test performs differently, and DROP any check whose setup you cannot replicate:
<test_support class="org.jfree.data.time.junit.WeekTests">
// --- setUp() (test setup) ---
    protected void setUp() {
        this.w1Y1900 = new Week(1, 1900);
        this.w2Y1900 = new Week(2, 1900);
        this.w51Y9999 = new Week(51, 9999);
        this.w52Y9999 = new Week(52, 9999);
    }

// --- class fields/constants the test uses ---
private Week w1Y1900;
private Week w2Y1900;
private Week w51Y9999;
private Week w52Y9999;
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
<relation name="ctor_zone_overload_agrees_with_explicit_locale" kind="metamorphic">
  // holds because: The deprecated constructor `Week(Date, TimeZone)` is documented to create the week for the specified date/time "calculated relative to the specified time zone"; `Week(Date, TimeZone, Locale)` is the non-deprecated sibling with the same semantics plus an explicit locale. Therefore, with `Locale.getDefault()` supplied explicitly, both constructors must produce the same week/year.
  // valid input: Choose a concrete instant by building a `Date` through a `GregorianCalendar` in a fuzz-chosen source time zone, and choose a separate fuzz-chosen target time zone. Set the default locale before probing, because the 2-arg constructor uses `Locale.getDefault()` by contract. All inputs are valid by construction: non-null `Date`, non-null `TimeZone`, non-null default `Locale`.
  // screen: DIRECTION-CONFIRMED: fires on the failing test's own inputs (buggy violates the pinned behaviour there); 1502/20000 on random inputs
Locale savedLocale = Locale.getDefault();
TimeZone savedZone = TimeZone.getDefault();
Date t;
TimeZone sourceZone;
TimeZone targetZone;
Locale defLocale;
try {
    String[] zoneIds = TimeZone.getAvailableIDs();
    if (zoneIds.length == 0) {
        return;
    }
    sourceZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
    targetZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
    defLocale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
    Locale.setDefault(defLocale);
    TimeZone.setDefault(sourceZone);
    Calendar cal = Calendar.getInstance(sourceZone, defLocale);
    cal.clear();
    cal.set(data.consumeInt(1900, 9999), data.consumeInt(0, 11), data.consumeInt(1, 28), data.consumeInt(0, 23), data.consumeInt(0, 59), data.consumeInt(0, 59));
    cal.set(Calendar.MILLISECOND, data.consumeInt(0, 999));
    t = cal.getTime();
} catch (Exception e) {
    Locale.setDefault(savedLocale);
    TimeZone.setDefault(savedZone);
    return;
}
Week a;
try {
    a = new Week(t, targetZone);
} catch (Exception e) { for (StackTraceElement __rpgf : e.getStackTrace()) { String __rpgc = __rpgf.getClassName(); if ((__rpgc.equals("Week") || __rpgc.endsWith(".Week") || __rpgc.endsWith("$Week")) && !"<init>".equals(__rpgf.getMethodName())) { boolean __rpgdoc = false; if (!__rpgdoc && (__rpgf.getClassName().equals("Week") || __rpgf.getClassName().endsWith(".Week") || __rpgf.getClassName().endsWith("$Week")) && "getFirstMillisecond".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("NullPointerException") || __rpn.endsWith(".NullPointerException") || __rpn.endsWith("$NullPointerException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("Week") || __rpgf.getClassName().endsWith(".Week") || __rpgf.getClassName().endsWith("$Week")) && "getLastMillisecond".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("NullPointerException") || __rpn.endsWith(".NullPointerException") || __rpn.endsWith("$NullPointerException")) { __rpgdoc = true; break; } } } if (__rpgdoc) return; break; } }
    Locale.setDefault(savedLocale);
    TimeZone.setDefault(savedZone);
    throw new RuntimeException("relation ctor_zone_overload_agrees_with_explicit_locale violated: unexpected " + e.getClass().getName() + " on valid-by-construction input: " + e.getMessage());
}
Week b;
try {
    b = new Week(t, targetZone, Locale.getDefault());
} catch (Exception e) { for (StackTraceElement __rpgf : e.getStackTrace()) { String __rpgc = __rpgf.getClassName(); if ((__rpgc.equals("Week") || __rpgc.endsWith(".Week") || __rpgc.endsWith("$Week")) && !"<init>".equals(__rpgf.getMethodName())) { boolean __rpgdoc = false; if (!__rpgdoc && (__rpgf.getClassName().equals("Week") || __rpgf.getClassName().endsWith(".Week") || __rpgf.getClassName().endsWith("$Week")) && "getFirstMillisecond".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("NullPointerException") || __rpn.endsWith(".NullPointerException") || __rpn.endsWith("$NullPointerException")) { __rpgdoc = true; break; } } } if (!__rpgdoc && (__rpgf.getClassName().equals("Week") || __rpgf.getClassName().endsWith(".Week") || __rpgf.getClassName().endsWith("$Week")) && "getLastMillisecond".equals(__rpgf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("NullPointerException") || __rpn.endsWith(".NullPointerException") || __rpn.endsWith("$NullPointerException")) { __rpgdoc = true; break; } } } if (__rpgdoc) return; break; } }
    Locale.setDefault(savedLocale);
    TimeZone.setDefault(savedZone);
    throw new RuntimeException("relation ctor_zone_overload_agrees_with_explicit_locale violated: unexpected " + e.getClass().getName() + " from 3-arg constructor on valid-by-construction input: " + e.getMessage());
}
try {
    if (a.getWeek() != b.getWeek() || a.getYearValue() != b.getYearValue()) {
        throw new RuntimeException("relation ctor_zone_overload_agrees_with_explicit_locale violated: 2-arg gave week/year " + a.getWeek() + "/" + a.getYearValue() + " but 3-arg gave " + b.getWeek() + "/" + b.getYearValue());
    }
} finally {
    Locale.setDefault(savedLocale);
    TimeZone.setDefault(savedZone);
}
</relation>
<relation name="first_millisecond_matches_calendar_overload_after_peg" kind="metamorphic">
  // holds because: For `getFirstMillisecond() / getFirstMillisecond(Calendar)`, the no-arg form is documented to be determined relative to the time zone from the constructor or from the calendar passed in the most recent `peg(Calendar)` call, while the overload is evaluated using the supplied calendar. Therefore, after `peg(c)`, both must agree for that same calendar.
  // valid input: Build any valid `Week` from week number 1..53 and year 1900..9999. Build a non-null `Calendar` using a fuzz-chosen available time-zone ID and either `Locale.US` or `da_DK`. Then call `peg(calendar)` before comparing the two getters.
  // screen: fired on 0/20000 fuzzed inputs on the buggy build (silent on the buggy build)
Week w;
Calendar cal;
try {
    w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
    String[] zoneIds = TimeZone.getAvailableIDs();
    if (zoneIds.length == 0) {
        return;
    }
    TimeZone zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
    Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
    cal = Calendar.getInstance(zone, locale);
    w.peg(cal);
} catch (Exception e) { for (StackTraceElement __rpf : e.getStackTrace()) { String __rpc = __rpf.getClassName(); if ((__rpc.equals("Week") || __rpc.endsWith(".Week") || __rpc.endsWith("$Week")) && !"<init>".equals(__rpf.getMethodName())) { boolean __rpdoc = false; if (!__rpdoc && (__rpf.getClassName().equals("Week") || __rpf.getClassName().endsWith(".Week") || __rpf.getClassName().endsWith("$Week")) && "getFirstMillisecond".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("NullPointerException") || __rpn.endsWith(".NullPointerException") || __rpn.endsWith("$NullPointerException")) { __rpdoc = true; break; } } } if (!__rpdoc && (__rpf.getClassName().equals("Week") || __rpf.getClassName().endsWith(".Week") || __rpf.getClassName().endsWith("$Week")) && "getLastMillisecond".equals(__rpf.getMethodName())) { for (Class __rpk = e.getClass(); __rpk != null; __rpk = __rpk.getSuperclass()) { String __rpn = __rpk.getName(); if (__rpn.equals("NullPointerException") || __rpn.endsWith(".NullPointerException") || __rpn.endsWith("$NullPointerException")) { __rpdoc = true; break; } } } if (!__rpdoc) throw new RuntimeException("relation first_millisecond_matches_calendar_overload_after_peg violated: unexpected " + e.getClass().getName() + " thrown by the patch-changed class on a valid-by-construction input: " + e.getMessage()); break; } }
    return;
}
long a;
try {
    a = w.getFirstMillisecond();
} catch (Exception e) {
    throw new RuntimeException("relation first_millisecond_matches_calendar_overload_after_peg violated: unexpected " + e.getClass().getName() + " from getFirstMillisecond() on valid-by-construction input: " + e.getMessage());
}
long b;
try {
    b = w.getFirstMillisecond(cal);
} catch (Exception e) {
    throw new RuntimeException("relation first_millisecond_matches_calendar_overload_after_peg violated: unexpected " + e.getClass().getName() + " from getFirstMillisecond(Calendar) on valid-by-construction input: " + e.getMessage());
}
if (a != b) {
    throw new RuntimeException("relation first_millisecond_matches_calendar_overload_after_peg violated: no-arg=" + a + " calendar-overload=" + b);
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
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~12,210 chars)</summary>

```
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedWeekTestsOracle();
        runCtorZoneOverloadAgreement(data);
        runFirstMillisecondPegAgreement(data);
        runHiddenStateReadOnlyChecks(data);
    }

    private static void runLiftedWeekTestsOracle() {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-da-first-day] semantic mismatch: expected firstDayOfWeek="
                                + Calendar.MONDAY + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            int actualWeek = w.getWeek();
            if (actualWeek != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-da-week] semantic mismatch: call=new Week(t, Europe/Copenhagen).getWeek() expected=34 actual="
                                + actualWeek);
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-first-day] semantic mismatch: expected firstDayOfWeek="
                                + Calendar.SUNDAY + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            actualWeek = w.getWeek();
            if (actualWeek != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-to-copenhagen-week] semantic mismatch: call=new Week(t, Europe/Copenhagen).getWeek() expected=35 actual="
                                + actualWeek);
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            actualWeek = w.getWeek();
            if (actualWeek != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-explicit-locale-week] semantic mismatch: call=new Week(t, Europe/Copenhagen, da_DK).getWeek() expected=34 actual="
                                + actualWeek);
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void runCtorZoneOverloadAgreement(FuzzedDataProvider data) {
        String[] zoneIds = TimeZone.getAvailableIDs();
        if (zoneIds == null || zoneIds.length == 0) {
            return;
        }

        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();

        Week a;
        Week b;
        try {
            TimeZone sourceZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            TimeZone targetZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            Locale defLocale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");

            Locale.setDefault(defLocale);
            TimeZone.setDefault(sourceZone);

            Calendar cal = Calendar.getInstance(sourceZone, defLocale);
            cal.clear();
            cal.set(
                    data.consumeInt(1900, 9999),
                    data.consumeInt(0, 11),
                    data.consumeInt(1, 28),
                    data.consumeInt(0, 23),
                    data.consumeInt(0, 59),
                    data.consumeInt(0, 59));
            cal.set(Calendar.MILLISECOND, data.consumeInt(0, 999));
            Date t = cal.getTime();

            try {
                a = new Week(t, targetZone);
            } catch (Throwable e) {
                return;
            }
            try {
                b = new Week(t, targetZone, Locale.getDefault());
            } catch (Throwable e) {
                return;
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }

        int aWeek;
        int bWeek;
        int aYear;
        int bYear;
        try {
            aWeek = a.getWeek();
            bWeek = b.getWeek();
            aYear = a.getYearValue();
            bYear = b.getYearValue();
        } catch (Throwable e) {
            return;
        }

        if (aWeek != bWeek || aYear != bYear) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:ctor-zone-overload-agrees] metamorphic violation: Week(Date, TimeZone) must agree with Week(Date, TimeZone, Locale.getDefault()) on week/year; lhs="
                            + aWeek + "/" + aYear + " rhs=" + bWeek + "/" + bYear);
        }
    }

    private static void runFirstMillisecondPegAgreement(FuzzedDataProvider data) {
        String[] zoneIds = TimeZone.getAvailableIDs();
        if (zoneIds == null || zoneIds.length == 0) {
            return;
        }

        Week w;
        Calendar cal;
        try {
            w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
            TimeZone zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            cal = Calendar.getInstance(zone, locale);
            w.peg(cal);
        } catch (Throwable e) {
            return;
        }

        long noArgFirst;
        long calFirst;
        try {
            noArgFirst = w.getFirstMillisecond();
            calFirst = w.getFirstMillisecond(cal);
        } catch (Throwable e) {
            return;
        }

        if (noArgFirst != calFirst) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:first-ms-after-peg] metamorphic violation: after peg(calendar), getFirstMillisecond() must equal getFirstMillisecond(calendar); lhs="
                            + noArgFirst + " rhs=" + calFirst);
        }
    }

    private static void runHiddenStateReadOnlyChecks(FuzzedDataProvider data) {
        String[] zoneIds = TimeZone.getAvailableIDs();
        if (zoneIds == null || zoneIds.length == 0) {
            return;
        }

        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        Week w;
        try {
            TimeZone sourceZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            TimeZone targetZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            Locale.setDefault(locale);
            TimeZone.setDefault(sourceZone);

            Calendar cal = Calendar.getInstance(sourceZone, locale);
            cal.clear();
            cal.set(
                    data.consumeInt(1900, 9999),
                    data.consumeInt(0, 11),
                    data.consumeInt(1, 28),
                    data.consumeInt(0, 23),
                    data.consumeInt(0, 59),
                    data.consumeInt(0, 59));
            cal.set(Calendar.MILLISECOND, data.consumeInt(0, 999));
            Date t = cal.getTime();

            try {
                w = new Week(t, targetZone, locale);
            } catch (Throwable e) {
                return;
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }

        int beforeWeek;
        int beforeYearValue;
        long beforeFirst;
        long beforeLast;
        long beforeSerial;
        int beforeHash;
        String beforeString;
        String beforePrev;
        String beforeNext;
        try {
            beforeWeek = w.getWeek();
            beforeYearValue = w.getYearValue();
            beforeFirst = w.getFirstMillisecond();
            beforeLast = w.getLastMillisecond();
            beforeSerial = w.getSerialIndex();
            beforeHash = w.hashCode();
            beforeString = String.valueOf(w.toString());
            beforePrev = String.valueOf(w.previous());
            beforeNext = String.valueOf(w.next());
        } catch (Throwable e) {
            return;
        }

        try {
            w.getWeek();
            w.getYear();
            w.getYearValue();
            w.getSerialIndex();
            w.getFirstMillisecond();
            w.getLastMillisecond();
            w.hashCode();
            w.toString();
            w.previous();
            w.next();
        } catch (Throwable e) {
            return;
        }

        int afterWeek;
        int afterYearValue;
        long afterFirst;
        long afterLast;
        long afterSerial;
        int afterHash;
        String afterString;
        String afterPrev;
        String afterNext;
        try {
            afterWeek = w.getWeek();
            afterYearValue = w.getYearValue();
            afterFirst = w.getFirstMillisecond();
            afterLast = w.getLastMillisecond();
            afterSerial = w.getSerialIndex();
            afterHash = w.hashCode();
            afterString = String.valueOf(w.toString());
            afterPrev = String.valueOf(w.previous());
            afterNext = String.valueOf(w.next());
        } catch (Throwable e) {
            return;
        }

        /* Contract justification: these are public no-argument readers/question methods.
           A correct implementation must not silently mutate the Week's observable state when
           answering them. A throw-deleting or bookkeeping-skipping patch that mutates cached
           fields during reads would violate this hidden-state invariant. */
        if (beforeWeek != afterWeek
                || beforeYearValue != afterYearValue
                || beforeFirst != afterFirst
                || beforeLast != afterLast
                || beforeSerial != afterSerial
                || beforeHash != afterHash
                || !safeEquals(beforeString, afterString)
                || !safeEquals(beforePrev, afterPrev)
                || !safeEquals(beforeNext, afterNext)) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:read-only-hidden-state] metamorphic violation: read-only getters changed observable state: week "
                            + beforeWeek + "->" + afterWeek
                            + " yearValue " + beforeYearValue + "->" + afterYearValue
                            + " firstMs " + beforeFirst + "->" + afterFirst
                            + " lastMs " + beforeLast + "->" + afterLast
                            + " serial " + beforeSerial + "->" + afterSerial
                            + " hash " + beforeHash + "->" + afterHash
                            + " toString " + beforeString + "->" + afterString
                            + " previous " + beforePrev + "->" + afterPrev
                            + " next " + beforeNext + "->" + afterNext);
        }
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
```

</details>

---
## [82] ⚙️ harness-attempt · `attempt_001`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:ctor-zone-overload-agrees] metamorphic violation: Week(Date, TimeZone) must agree with Week(Date, TimeZone, Locale.getDefault()) on wee…

---
## [83] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~47,695 chars, 1 new)</summary>

- *[system] message: identical to step [81] — not reprinted*
**[user]**
```
Write a Jazzer harness. Rules:

- Class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Package: `org.jfree.data.time` (`package org.jfree.data.time;` at the top).
  Same-package placement gives direct access to package-private members — no reflection.
- Output raw Java only: no markdown fences, no prose. A leading `/* ... */` comment is allowed. Must compile with `javac` against the project classpath plus jazzer-api.jar.
- Reach the fault through the library's REAL code, not a hand-built stand-in. You may construct and use classes that already exist in the library, but do NOT write your own subclass, anonymous class, mock, or stub of the patched class or any of its callees to force the crash. A harness that manufactures the crash with a custom implementation proves nothing about real usage and is rejected.

Codebase: `Chart_8_buggy`. The patch below touches the functions listed. Your harness must call those functions so the patched behaviour is reachable from the fuzz entrypoint.

Patch under analysis:
<patch>
--- /source/org/jfree/data/time/Week.java
+++ /source/org/jfree/data/time/Week.java
@@ -172,7 +172,7 @@
      */
     public Week(Date time, TimeZone zone) {
         // defer argument checking...
-        this(time, RegularTimePeriod.DEFAULT_TIME_ZONE, Locale.getDefault());
+       this(time, zone, Locale.getDefault());
     }
 
     /**

</patch>

Available imports from the modified file (copy exactly when you need these types):
<source_imports>
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
</source_imports>

Function `Week`:
<signature>
public Week(Date time, TimeZone zone)
</signature>
<code>
    public Week(Date time, TimeZone zone) {
        // defer argument checking...
        this(time, RegularTimePeriod.DEFAULT_TIME_ZONE, Locale.getDefault());
    }
</code>

CLASS-LEVEL CONTEXT — partial skeletons of the touched class(es): signatures, fields, javadoc; non-touched method bodies elided. Use them to find CROSS-MEMBER consistency oracles: sibling accessors that must agree with each other or with constructor arguments, overloads that must be equivalent, round-trips that must restore state. Assert only what a shown contract or invariant guarantees.
<class_skeletons>
<class name="Week" role="patched">
     * A calendar week.  All years are considered to have 53 weeks, numbered from 1
     * to 53, although in many cases the 53rd week is empty.  Most of the time, the
     * 1st week of the year *begins* in the previous calendar year, but it always
     * finishes in the current year (this behaviour matches the workings of the
     * <code>GregorianCalendar</code> class).
     * <P>
     * This class is immutable, which is a requirement for all
     * {@link RegularTimePeriod} subclasses.
public class Week extends RegularTimePeriod implements Serializable {
     * For serialization.
    private static final long serialVersionUID = 1856387786939865061L;
     * Constant for the first week in the year.
    public static final int FIRST_WEEK_IN_YEAR = 1;
     * Constant for the last week in the year.
    public static final int LAST_WEEK_IN_YEAR = 53;
     * The year in which the week falls.
    private short year;
     * The week (1-53).
    private byte week;
     * The first millisecond.
    private long firstMillisecond;
     * The last millisecond.
    private long lastMillisecond;
     * Creates a new time period for the week in which the current system
     * date/time falls.
    public Week() {
        this(new Date());
    }
     * Creates a time period representing the week in the specified year.
     * @param week  the week (1 to 53).
     * @param year  the year (1900 to 9999).
    public Week(int week, int year) {
        if ((week < FIRST_WEEK_IN_YEAR) && (week > LAST_WEEK_IN_YEAR)) {
            throw new IllegalArgumentException(
                    "The 'week' argument must be in the range 1 - 53.");
        }
        this.week = (byte) week;
        this.year = (short) year;
        peg(Calendar.getInstance());
    }
     * Creates a time period representing the week in the specified year.
     * @param week  the week (1 to 53).
     * @param year  the year (1900 to 9999).
    public Week(int week, Year year) {
        if ((week < FIRST_WEEK_IN_YEAR) && (week > LAST_WEEK_IN_YEAR)) {
            throw new IllegalArgumentException(
                    "The 'week' argument must be in the range 1 - 53.");
        }
        this.week = (byte) week;
        this.year = (short) year.getYear();
        peg(Calendar.getInstance());
   }
     * Creates a time period for the week in which the specified date/time
     * falls.
     * @param time  the time (<code>null</code> not permitted).
    public Week(Date time) {
        // defer argument checking...
        this(time, RegularTimePeriod.DEFAULT_TIME_ZONE, Locale.getDefault());
    }
     * Creates a time period for the week in which the specified date/time
     * falls, calculated relative to the specified time zone.
     * @param time  the date/time (<code>null</code> not permitted).
     * @param zone  the time zone (<code>null</code> not permitted).
     * @deprecated As of 1.0.7, use {@link #Week(Date, TimeZone, Locale)}.
    public Week(Date time, TimeZone zone) {
        // defer argument checking...
        this(time, RegularTimePeriod.DEFAULT_TIME_ZONE, Locale.getDefault());
    }
     * Creates a time period for the week in which the specified date/time
     * falls, calculated relative to the specified time zone.
     * @param time  the date/time (<code>null</code> not permitted).
     * @param zone  the time zone (<code>null</code> not permitted).
     * @param locale  the locale (<code>null</code> not permitted).
     * @since 1.0.7
    public Week(Date time, TimeZone zone, Locale locale) {
        if (time == null) {
            throw new IllegalArgumentException("Null 'time' argument.");
        }
        if (zone == null) {
            throw new IllegalArgumentException("Null 'zone' argument.");
        }
        if (locale == null) {
            throw new IllegalArgumentException("Null 'locale' argument.");
        }
        Calendar calendar = Calendar.getInstance(zone, locale);
        calendar.setTime(time);

        // sometimes the last few days of the year are considered to fall in
        // the *first* week of the following year.  Refer to the Javadocs for
        // GregorianCalendar.
        int tempWeek = calendar.get(Calendar.WEEK_OF_YEAR);
        if (tempWeek == 1
                && calendar.get(Calendar.MONTH) == Calendar.DECEMBER) {
            this.week = 1;
            this.year = (short) (calendar.get(Calendar.YEAR) + 1);
        }
        else {
            this.week = (byte) Math.min(tempWeek, LAST_WEEK_IN_YEAR);
            int yyyy = calendar.get(Calendar.YEAR);
            // alternatively, sometimes the first few days of the year are
            // considered to fall in the *last* week of the previous year...
            if (calendar.get(Calendar.MONTH) == Calendar.JANUARY
                    && this.week >= 52) {
                yyyy--;
            }
            this.year = (short) yyyy;
        }
        peg(calendar);
    }
     * Returns the year in which the week falls.
     * @return The year (never <code>null</code>).
    public Year getYear() { … }
     * Returns the year in which the week falls, as an integer value.
     * @return The year.
    public int getYearValue() { … }
     * Returns the week.
     * @return The week.
    public int getWeek() { … }
     * Returns the first millisecond of the week.  This will be determined
     * relative to the time zone specified in the constructor, or in the
     * calendar instance passed in the most recent call to the
     * {@link #peg(Calendar)} method.
     * @return The first millisecond of the week.
     * @see #getLastMillisecond()
    public long getFirstMillisecond() { … }
     * Returns the last millisecond of the week.  This will be
     * determined relative to the time zone specified in the constructor, or
     * in the calendar instance passed in the most recent call to the
     * {@link #peg(Calendar)} method.
     * @return The last millisecond of the week.
     * @see #getFirstMillisecond()
    public long getLastMillisecond() { … }
     * Recalculates the start date/time and end date/time for this time period
     * relative to the supplied calendar (which incorporates a time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @since 1.0.3
    public void peg(Calendar calendar) { … }
     * Returns the week preceding this one.  This method will return
     * <code>null</code> for some lower limit on the range of weeks (currently
     * week 1, 1900).  For week 1 of any year, the previous week is always week
     * 53, but week 53 may not contain any days (you should check for this).
     * @return The preceding week (possibly <code>null</code>).
    public RegularTimePeriod previous() { … }
     * Returns the week following this one.  This method will return
     * <code>null</code> for some upper limit on the range of weeks (currently
     * week 53, 9999).  For week 52 of any year, the following week is always
     * week 53, but week 53 may not contain any days (you should check for
     * this).
     * @return The following week (possibly <code>null</code>).
    public RegularTimePeriod next() { … }
     * Returns a serial index number for the week.
     * @return The serial index number.
    public long getSerialIndex() { … }
     * Returns the first millisecond of the week, evaluated using the supplied
     * calendar (which determines the time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @return The first millisecond of the week.
     * @throws NullPointerException if <code>calendar</code> is
     * <code>null</code>.
    public long getFirstMillisecond(Calendar calendar) { … }
     * Returns a string representing the week (e.g. "Week 9, 2002").
     * TODO: look at internationalisation.
     * @return A string representing the week.
    public String toString() { … }
     * Tests the equality of this Week object to an arbitrary object.  Returns
     * true if the target is a Week instance representing the same week as this
     * object.  In all other cases, returns false.
     * @param obj  the object (<code>null</code> permitted).
     * @return <code>true</code> if week and year of this and object are the
     * same.
    public boolean equals(Object obj) { … }
     * Returns a hash code for this object instance.  The approach described by
     * Joshua Bloch in "Effective Java" has been used here:
     * <p>
     * <code>http://developer.java.sun.com/developer/Books/effectivejava
     * /Chapter3.pdf</code>
     * @return A hash code.
    public int hashCode() { … }
     * Returns an integer indicating the order of this Week object relative to
     * the specified object:
     * negative == before, zero == same, positive == after.
     * @param o1  the object to compare.
     * @return negative == before, zero == same, positive == after.
    public int compareTo(Object o1) { … }
     * Parses the string argument as a week.
     * <P>
     * This method is required to accept the format "YYYY-Wnn".  It will also
     * accept "Wnn-YYYY". Anything else, at the moment, is a bonus.
     * @param s  string to parse.
     * @return <code>null</code> if the string is not parseable, the week
     * otherwise.
    public static Week parseWeek(String s) { … }
     * Finds the first occurrence of ' ', '-', ',' or '.'
     * @param s  the string to parse.
     * @return <code>-1</code> if none of the characters was found, the
     * index of the first occurrence otherwise.
    private static int findSeparator(String s) { … }
     * Creates a year from a string, or returns null (format exceptions
     * suppressed).
     * @param s  string to parse.
     * @return <code>null</code> if the string is not parseable, the year
     * otherwise.
    private static Year evaluateAsYear(String s) { … }
     * Converts a string to a week.
     * @param s  the string to parse.
     * @return <code>-1</code> if the string does not contain a week number,
     * the number of the week otherwise.
    private static int stringToWeek(String s) { … }
}
</class>

<class name="RegularTimePeriod" role="supertype-contract">
     * An abstract class representing a unit of time.  Convenient methods are
     * provided for calculating the next and previous time periods.  Conversion
     * methods are defined that return the first and last milliseconds of the time
     * period.  The results from these methods are timezone dependent.
     * <P>
     * This class is immutable, and all subclasses should be immutable also.
public abstract class RegularTimePeriod implements TimePeriod, Comparable,
                                                   MonthConstants {
     * Creates a time period that includes the specified millisecond, assuming
     * the given time zone.
     * @param c  the time period class.
     * @param millisecond  the time.
     * @param zone  the time zone.
     * @return The time period.
    public static RegularTimePeriod createInstance(Class c, Date millisecond, TimeZone zone) { … }
     * Returns a subclass of {@link RegularTimePeriod} that is smaller than
     * the specified class.
     * @param c  a subclass of {@link RegularTimePeriod}.
     * @return A class.
    public static Class downsize(Class c) { … }
     * Returns the time period preceding this one, or <code>null</code> if some
     * lower limit has been reached.
     * @return The previous time period (possibly <code>null</code>).
    public abstract RegularTimePeriod previous();
     * Returns the time period following this one, or <code>null</code> if some
     * limit has been reached.
     * @return The next time period (possibly <code>null</code>).
    public abstract RegularTimePeriod next();
     * Returns a serial index number for the time unit.
     * @return The serial index number.
    public abstract long getSerialIndex();
     * The default time zone.
    public static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getDefault();
     * Recalculates the start date/time and end date/time for this time period
     * relative to the supplied calendar (which incorporates a time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @since 1.0.3
    public abstract void peg(Calendar calendar);
     * Returns the date/time that marks the start of the time period.  This
     * method returns a new <code>Date</code> instance every time it is called.
     * @return The start date/time.
     * @see #getFirstMillisecond()
    public Date getStart() { … }
     * Returns the date/time that marks the end of the time period.  This
     * method returns a new <code>Date</code> instance every time it is called.
     * @return The end date/time.
     * @see #getLastMillisecond()
    public Date getEnd() { … }
     * Returns the first millisecond of the time period.  This will be
     * determined relative to the time zone specified in the constructor, or
     * in the calendar instance passed in the most recent call to the
     * {@link #peg(Calendar)} method.
     * @return The first millisecond of the time period.
     * @see #getLastMillisecond()
    public abstract long getFirstMillisecond();
     * Returns the first millisecond of the time period, evaluated using the
     * supplied calendar (which incorporates a timezone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @return The first millisecond of the time period.
     * @throws NullPointerException if <code>calendar,/code> is
     * </code>null</code>.
     * @see #getLastMillisecond(Calendar)
    public abstract long getFirstMillisecond(Calendar calendar);
     * Returns the last millisecond of the time period.  This will be
     * determined relative to the time zone specified in the constructor, or
     * in the calendar instance passed in the most recent call to the
     * {@link #peg(Calendar)} method.
     * @return The last millisecond of the time period.
     * @see #getFirstMillisecond()
    public abstract long getLastMillisecond();
     * Returns the last millisecond of the time period, evaluated using the
     * supplied calendar (which incorporates a timezone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @return The last millisecond of the time period.
     * @see #getFirstMillisecond(Calendar)
    public abstract long getLastMillisecond(Calendar calendar);
     * Returns the millisecond closest to the middle of the time period.
     * @return The middle millisecond.
    public long getMiddleMillisecond() { … }
     * Returns the millisecond closest to the middle of the time period,
     * evaluated using the supplied calendar (which incorporates a timezone).
     * @param calendar  the calendar.
     * @return The middle millisecond.
    public long getMiddleMillisecond(Calendar calendar) { … }
     * Returns a string representation of the time period.
     * @return The string.
    public String toString() { … }
}
</class>

<class name="Year" role="collaborator">
     * Represents a year in the range 1900 to 9999.  This class is immutable, which
     * is a requirement for all {@link RegularTimePeriod} subclasses.
public class Year extends RegularTimePeriod implements Serializable {
     * For serialization.
    private static final long serialVersionUID = -7659990929736074836L;
     * The year.
    private short year;
     * The first millisecond.
    private long firstMillisecond;
     * The last millisecond.
    private long lastMillisecond;
     * Creates a new <code>Year</code>, based on the current system date/time.
    public Year() {
        this(new Date());
    }
     * Creates a time period representing a single year.
     * @param year  the year.
    public Year(int year) {
        if ((year < SerialDate.MINIMUM_YEAR_SUPPORTED)
            || (year > SerialDate.MAXIMUM_YEAR_SUPPORTED)) {

            throw new IllegalArgumentException(
                "Year constructor: year (" + year + ") outside valid range.");
        }
        this.year = (short) year;
        peg(Calendar.getInstance());
    }
     * Creates a new <code>Year</code>, based on a particular instant in time,
     * using the default time zone.
     * @param time  the time (<code>null</code> not permitted).
    public Year(Date time) {
        this(time, RegularTimePeriod.DEFAULT_TIME_ZONE);
    }
     * Constructs a year, based on a particular instant in time and a time zone.
     * @param time  the time.
     * @param zone  the time zone.
    public Year(Date time, TimeZone zone) {
        Calendar calendar = Calendar.getInstance(zone);
        calendar.setTime(time);
        this.year = (short) calendar.get(Calendar.YEAR);
        peg(calendar);
    }
     * Returns the year.
     * @return The year.
    public int getYear() { … }
     * Returns the first millisecond of the year.  This will be determined
     * relative to the time zone specified in the constructor, or in the
     * calendar instance passed in the most recent call to the
     * {@link #peg(Calendar)} method.
     * @return The first millisecond of the year.
     * @see #getLastMillisecond()
    public long getFirstMillisecond() { … }
     * Returns the last millisecond of the year.  This will be
     * determined relative to the time zone specified in the constructor, or
     * in the calendar instance passed in the most recent call to the
     * {@link #peg(Calendar)} method.
     * @return The last millisecond of the year.
     * @see #getFirstMillisecond()
    public long getLastMillisecond() { … }
     * Recalculates the start date/time and end date/time for this time period
     * relative to the supplied calendar (which incorporates a time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @since 1.0.3
    public void peg(Calendar calendar) { … }
     * Returns the year preceding this one.
     * @return The year preceding this one (or <code>null</code> if the
     * current year is 1900).
    public RegularTimePeriod previous() { … }
     * Returns the year following this one.
     * @return The year following this one (or <code>null</code> if the current
     * year is 9999).
    public RegularTimePeriod next() { … }
     * Returns a serial index number for the year.
     * <P>
     * The implementation simply returns the year number (e.g. 2002).
     * @return The serial index number.
    public long getSerialIndex() { … }
     * Returns the first millisecond of the year, evaluated using the supplied
     * calendar (which determines the time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @return The first millisecond of the year.
     * @throws NullPointerException if <code>calendar</code> is
     * <code>null</code>.
    public long getFirstMillisecond(Calendar calendar) { … }
     * Returns the last millisecond of the year, evaluated using the supplied
     * calendar (which determines the time zone).
     * @param calendar  the calendar (<code>null</code> not permitted).
     * @return The last millisecond of the year.
     * @throws NullPointerException if <code>calendar</code> is
     * <code>null</code>.
    public long getLastMillisecond(Calendar calendar) { … }
     * Tests the equality of this <code>Year</code> object to an arbitrary
     * object.  Returns <code>true</code> if the target is a <code>Year</code>
     * instance representing the same year as this object.  In all other cases,
     * returns <code>false</code>.
     * @param object  the object (<code>null</code> permitted).
     * @return <code>true</code> if the year of this and the object are the
     * same.
    public boolean equals(Object object) { … }
     * Returns a hash code for this object instance.  The approach described by
     * Joshua Bloch in "Effective Java" has been used here:
     * <p>
     * <code>http://developer.java.sun.com/developer/Books/effectivejava
     * /Chapter3.pdf</code>
     * @return A hash code.
    public int hashCode() { … }
     * Returns an integer indicating the order of this <code>Year</code> object
     * relative to the specified object:
     * negative == before, zero == same, positive == after.
     * @param o1  the object to compare.
     * @return negative == before, zero == same, positive == after.
    public int compareTo(Object o1) { … }
     * Returns a string representing the year..
     * @return A string representing the year.
    public String toString() { … }
     * Parses the string argument as a year.
     * <P>
     * The string format is YYYY.
     * @param s  a string representing the year.
     * @return <code>null</code> if the string is not parseable, the year
     * otherwise.
    public static Year parseYear(String s) { … }
}
</class>

<class name="TimePeriodFormatException" role="collaborator">
     * An exception that indicates an invalid format in a string representing a
     * time period.
public class TimePeriodFormatException extends IllegalArgumentException {
     * Creates a new exception.
     * @param message  a message describing the exception.
    public TimePeriodFormatException(String message) {
        super(message);
    }
}
</class>

<class name="TimePeriod" role="collaborator">
     * A period of time measured to millisecond precision using two instances of
     * <code>java.util.Date</code>.
public interface TimePeriod extends Comparable {
     * Returns the start date/time.  This will always be on or before the
     * end date.
     * @return The start date/time (never <code>null</code>).
    public Date getStart();
     * Returns the end date/time.  This will always be on or after the
     * start date.
     * @return The end date/time (never <code>null</code>).
    public Date getEnd();
}
</class>

<class name="JFreeChart" role="collaborator">
     * A chart class implemented using the Java 2D APIs.  The current version
     * supports bar charts, line charts, pie charts and xy plots (including time
     * series data).
     * <P>
     * JFreeChart coordinates several objects to achieve its aim of being able to
     * draw a chart on a Java 2D graphics device: a list of {@link Title} objects
     * (which often includes the chart's legend), a {@link Plot} and a
     * {@link org.jfree.data.general.Dataset} (the plot in turn manages a
     * domain axis and a range axis).
     * <P>
     * You should use a {@link ChartPanel} to display a chart in a GUI.
     * <P>
     * …
public class JFreeChart implements Drawable,
                                   TitleChangeListener,
                                   PlotChangeListener,
                                   Serializable,
                                   Cloneable {
     * For serialization.
    private static final long serialVersionUID = -3470703747817429120L;
     * Information about the project.
    public static final ProjectInfo INFO = new JFreeChartInfo();
     * The default font for titles.
    public static final Font DEFAULT_TITLE_FONT = new Font("SansSerif", Font.BOLD, 18);
     * The default background color.
    public static final Paint DEFAULT_BACKGROUND_PAINT = Color.WHITE;
     * The default background image.
    public static final Image DEFAULT_BACKGROUND_IMAGE = null;
     * The default background image alignment.
    public static final int DEFAULT_BACKGROUND_IMAGE_ALIGNMENT = Align.FIT;
     * The default background image alpha.
    public static final float DEFAULT_BACKGROUND_IMAGE_ALPHA = 0.5f;
     * Rendering hints that will be used for chart drawing.  This should never
     * be <code>null</code>.
    private transient RenderingHints renderingHints;
     * A flag that controls whether or not the chart border is drawn.
    private boolean borderVisible;
     * The stroke used to draw the chart border (if visible).
    private transient Stroke borderStroke;
     * The paint used to draw the chart border (if visible).
    private transient Paint borderPaint;
     * The padding between the chart border and the chart drawing area.
    private RectangleInsets padding;
     * The chart title (optional).
    private TextTitle title;
     * The chart subtitles (zero, one or many).  This field should never be
     * <code>null</code>.
    private List subtitles;
     * Draws the visual representation of the data.
    private Plot plot;
     * Paint used to draw the background of the chart.
    private transient Paint backgroundPaint;
     * An optional background image for the chart.
    private transient Image backgroundImage;
     * The alignment for the background image.
    private int backgroundImageAlignment = Align.FIT;
     * The alpha transparency for the background image.
    private float backgroundImageAlpha = 0.5f;
     * Storage for registered change listeners.
    private transient EventListenerList changeListeners;
     * Storage for registered progress listeners.
    private transient EventListenerList progressListeners;
     * A flag that can be used to enable/disable notification of chart change
     * events.
    private boolean notify;
     * Creates a new chart based on the supplied plot.  The chart will have
     * a legend added automatically, but no title (although you can easily add
     * one later).
     * <br><br>
     * Note that the  {@link ChartFactory} class contains a range
     * of static methods that will return ready-made charts, and often this
     * is a more convenient way to create charts than using this constructor.
     * @param plot  the plot (<code>null</code> not permitted).
    public JFreeChart(Plot plot) {
        this(null, null, plot, true);
    }
     * Creates a new chart with the given title and plot.  A default font
     * (@link DEFAULT_TITLE_FONT) is used for the title, and the chart will
     * have a legend added automatically.
     * <br><br>
     * Note that the  {@link ChartFactory} class contains a range
     * of static methods that will return ready-made charts, and often this
     * is a more convenient way to create charts than using this constructor.
     * @param title  the chart title (<code>null</code> permitted).
     * @param plot  the plot (<code>null</code> not permitted).
    public JFreeChart(String title, Plot plot) {
        this(title, JFreeChart.DEFAULT_TITLE_FONT, plot, true);
    }
     * Creates a new chart with the given title and plot.  The
     * <code>createLegend</code> argument specifies whether or not a legend
     * should be added to the chart.
     * <br><br>
     * Note that the  {@link ChartFactory} class contains a range
     * of static methods that will return ready-made charts, and often this
     * is a more convenient way to create charts than using this constructor.
     * @param title  the chart title (<code>null</code> permitted).
     * @param titleFont  the font for displaying the chart title
     * (<code>null</code> permitted).
     * @param plot  controller of the visual representation of the data
     * (<code>null</code> not permitted).
     * …
    public JFreeChart(String title, Font titleFont, Plot plot, boolean createLegend) { … }
     * Returns the collection of rendering hints for the chart.
     * @return The rendering hints for the chart (never <code>null</code>).
     * @see #setRenderingHints(RenderingHints)
    public RenderingHints getRenderingHints() { … }
     * Sets the rendering hints for the chart.  These will be added (using the
     * Graphics2D.addRenderingHints() method) near the start of the
     * JFreeChart.draw() method.
     * @param renderingHints  the rendering hints (<code>null</code> not
     * permitted).
     * @see #getRenderingHints()
    public void setRenderingHints(RenderingHints renderingHints) { … }
}
// … (non-patched members truncated; patched methods kept)
</class>

<class name="Contributor" role="collaborator">
     * A simple class representing a contributor to a software project.
public class Contributor {
     * The name.
    private String name;
     * The e-mail address.
    private String email;
     * Creates a new contributor.
     * @param name  the name.
     * @param email  the e-mail address.
    public Contributor(String name, String email) {
        this.name = name;
        this.email = email;
    }
     * Returns the contributor's name.
     * @return the contributor's name.
    public String getName() { … }
     * Returns the contributor's e-mail address.
     * @return the contributor's e-mail address.
    public String getEmail() { … }
}
</class>
</class_skeletons>

SAME-NAME OVERLOADS (documented to agree where their docs match — a sibling-agreement check compares them on equivalent inputs):
  getFirstMillisecond() / (Calendar calendar)
  getLastMillisecond() / (Calendar calendar)

METHOD FAMILIES over the same input space (factory/parser siblings — where the docs state a selection or agreement rule between family members, equivalent inputs must respect it):
  get* family: getFirstMillisecond, getLastMillisecond, getSerialIndex, getWeek, getYear, getYearValue

STATE YOU CAN READ (public no-argument readers — capture them BEFORE and AFTER a call documented as read-only or non-mutating; an unexplained change is a hidden-state violation): getFirstMillisecond, getLastMillisecond, getSerialIndex, getWeek, getYear, getYearValue, hashCode, next, previous, toString

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
REAL ENTRY POINT: in production the bug is reached through the public API the failing test drives (here: `WeekTests`). Drive your fuzzed input through that public API so it flows along the real call chain to the patched line. Prefer this over calling the patched method directly, and never reach it via a custom implementation of a library type.
Lift the call and the expected value from THIS test (this attempt's target):
<failing_test class="org.jfree.data.time.junit.WeekTests" method="testConstructor">
    public void testConstructor() {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        Locale.setDefault(new Locale("da", "DK"));
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
        GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                TimeZone.getDefault(), Locale.getDefault());

        // first day of week is monday
        assertEquals(Calendar.MONDAY, cal.getFirstDayOfWeek());
        cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date t = cal.getTime();
        Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
        assertEquals(34, w.getWeek());

        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
        cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());
        // first day of week is Sunday
        assertEquals(Calendar.SUNDAY, cal.getFirstDayOfWeek());
        cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);

        t = cal.getTime();
        w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
        assertEquals(35, w.getWeek());
        w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                new Locale("da", "DK"));
        assertEquals(34, w.getWeek());

        Locale.setDefault(savedLocale);
        TimeZone.setDefault(savedZone);
    }
</failing_test>
On the BUGGY build this exact test FAILS with:
<real_failure_message>
--- org.jfree.data.time.junit.WeekTests::testConstructor
junit.framework.AssertionFailedError: expected:<35> but was:<34>
</real_failure_message>
This is ground truth: it names the observable that diverges and the wrong value the buggy build produces. A faithful copy of this test MUST observe exactly this wrong value on the buggy build — if your check observes a different value, your setup diverges from the test's and the harness will be rejected.
The test method depends on the following from its test class — setUp, helper methods, constants, fixture files. Replicate this environment EXACTLY (register the same files, wire the same providers, use the same constants); do NOT improvise setup the test performs differently, and DROP any check whose setup you cannot replicate:
<test_support class="org.jfree.data.time.junit.WeekTests">
// --- setUp() (test setup) ---
    protected void setUp() {
        this.w1Y1900 = new Week(1, 1900);
        this.w2Y1900 = new Week(2, 1900);
        this.w51Y9999 = new Week(51, 9999);
        this.w52Y9999 = new Week(52, 9999);
    }

// --- class fields/constants the test uses ---
private Week w1Y1900;
private Week w2Y1900;
private Week w51Y9999;
private Week w52Y9999;
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
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // >>> YOUR CODE HERE <<<
    }
}
</skeleton>
```

</details>
<details open><summary>▸ Output (~8,634 chars)</summary>

```
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-da-firstday] semantic mismatch: expected=" + Calendar.MONDAY
                                + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-da-week] semantic mismatch: expected=34 actual=" + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-firstday] semantic mismatch: expected=" + Calendar.SUNDAY
                                + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-week] semantic mismatch: expected=35 actual=" + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-explicit-locale-week] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            TimeZone[] zones = new TimeZone[] {
                    TimeZone.getTimeZone("Europe/Copenhagen"),
                    TimeZone.getTimeZone("US/Detroit"),
                    TimeZone.getTimeZone("UTC"),
                    TimeZone.getTimeZone("Asia/Tokyo")
            };
            Locale[] locales = new Locale[] {
                    Locale.US,
                    new Locale("da", "DK"),
                    Locale.UK,
                    Locale.GERMANY
            };

            TimeZone chosenZone = zones[Math.abs(data.consumeInt()) % zones.length];
            Locale chosenDefaultLocale = locales[Math.abs(data.consumeInt()) % locales.length];
            Locale.setDefault(chosenDefaultLocale);

            int seconds = data.consumeInt(-1_000_000, 1_000_000);
            Date fuzzDate = new Date(seconds * 1000L);

            try {
                Week fromDeprecatedOverload = new Week(fuzzDate, chosenZone);
                Week fromExplicitLocale = new Week(fuzzDate, chosenZone, Locale.getDefault());

                /* Contract: Week(Date, TimeZone) is documented as the same operation
                 * "calculated relative to the specified time zone", and the deprecation
                 * points callers to Week(Date, TimeZone, Locale). For any correct
                 * implementation under a fixed default locale, these two real API calls
                 * must agree on observable state. A patch that ignores the supplied zone
                 * or otherwise silently computes the wrong week breaks this relation
                 * without throwing.
                 */
                if (fromDeprecatedOverload.getWeek() != fromExplicitLocale.getWeek()
                        || fromDeprecatedOverload.getYearValue() != fromExplicitLocale.getYearValue()
                        || fromDeprecatedOverload.getFirstMillisecond() != fromExplicitLocale.getFirstMillisecond()
                        || fromDeprecatedOverload.getLastMillisecond() != fromExplicitLocale.getLastMillisecond()
                        || fromDeprecatedOverload.getSerialIndex() != fromExplicitLocale.getSerialIndex()
                        || !fromDeprecatedOverload.equals(fromExplicitLocale)) {
                    throw new RuntimeException(
                            "[oracle:overload-equivalence] metamorphic violation: Week(Date,TimeZone) != Week(Date,TimeZone,Locale.getDefault())"
                                    + " inputMillis=" + fuzzDate.getTime()
                                    + " zone=" + chosenZone.getID()
                                    + " defaultLocale=" + Locale.getDefault()
                                    + " lhsWeek=" + fromDeprecatedOverload.getWeek()
                                    + " rhsWeek=" + fromExplicitLocale.getWeek()
                                    + " lhsYear=" + fromDeprecatedOverload.getYearValue()
                                    + " rhsYear=" + fromExplicitLocale.getYearValue()
                                    + " lhsFirstMs=" + fromDeprecatedOverload.getFirstMillisecond()
                                    + " rhsFirstMs=" + fromExplicitLocale.getFirstMillisecond()
                                    + " lhsLastMs=" + fromDeprecatedOverload.getLastMillisecond()
                                    + " rhsLastMs=" + fromExplicitLocale.getLastMillisecond()
                                    + " lhsSerial=" + fromDeprecatedOverload.getSerialIndex()
                                    + " rhsSerial=" + fromExplicitLocale.getSerialIndex()
                                    + " lhsEqualsRhs=" + fromDeprecatedOverload.equals(fromExplicitLocale));
                }

                Calendar sameCalendar = Calendar.getInstance(chosenZone, Locale.getDefault());
                /* Documented sibling guarantee: getFirstMillisecond()/getLastMillisecond()
                 * are determined relative to the time zone specified in the constructor,
                 * or the most recent peg(Calendar). Before any peg() call, evaluating with
                 * an equivalent Calendar should match the no-arg readers.
                 */
                if (fromExplicitLocale.getFirstMillisecond() != fromExplicitLocale.getFirstMillisecond(sameCalendar)) {
                    throw new RuntimeException(
                            "[oracle:first-ms-sibling] metamorphic violation: getFirstMillisecond() disagrees with getFirstMillisecond(Calendar)"
                                    + " inputMillis=" + fuzzDate.getTime()
                                    + " zone=" + chosenZone.getID()
                                    + " locale=" + Locale.getDefault()
                                    + " lhs=" + fromExplicitLocale.getFirstMillisecond()
                                    + " rhs=" + fromExplicitLocale.getFirstMillisecond(sameCalendar));
                }
                if (fromExplicitLocale.getLastMillisecond() != fromExplicitLocale.getLastMillisecond(sameCalendar)) {
                    throw new RuntimeException(
                            "[oracle:last-ms-sibling] metamorphic violation: getLastMillisecond() disagrees with getLastMillisecond(Calendar)"
                                    + " inputMillis=" + fuzzDate.getTime()
                                    + " zone=" + chosenZone.getID()
                                    + " locale=" + Locale.getDefault()
                                    + " lhs=" + fromExplicitLocale.getLastMillisecond()
                                    + " rhs=" + fromExplicitLocale.getLastMillisecond(sameCalendar));
                }
            } catch (Throwable ignored) {
                return;
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}
```

</details>

---
## [84] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Your harness computes an array/list index that can go NEGATIVE: near line 77: `Math.abs(data.consumeInt()) %` — Math.abs(Integer.MIN_VALUE) is NEGATIVE, so this index eventually goes out of bounds and

---
## [85] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~9,199 chars, 2 new)</summary>

- *[system] message: identical to step [81] — not reprinted*
- *[user] message: identical to step [83] — not reprinted*
**[assistant]**
```
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-da-firstday] semantic mismatch: expected=" + Calendar.MONDAY
                                + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-da-week] semantic mismatch: expected=34 actual=" + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-firstday] semantic mismatch: expected=" + Calendar.SUNDAY
                                + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-week] semantic mismatch: expected=35 actual=" + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-explicit-locale-week] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            TimeZone[] zones = new TimeZone[] {
                    TimeZone.getTimeZone("Europe/Copenhagen"),
                    TimeZone.getTimeZone("US/Detroit"),
                    TimeZone.getTimeZone("UTC"),
                    TimeZone.getTimeZone("Asia/Tokyo")
            };
            Locale[] locales = new Locale[] {
                    Locale.US,
                    new Locale("da", "DK"),
                    Locale.UK,
                    Locale.GERMANY
            };

            TimeZone chosenZone = zones[Math.abs(data.consumeInt()) % zones.length];
            Locale chosenDefaultLocale = locales[Math.abs(data.consumeInt()) % locales.length];
            Locale.setDefault(chosenDefaultLocale);

            int seconds = data.consumeInt(-1_000_000, 1_000_000);
            Date fuzzDate = new Date(seconds * 1000L);

            try {
                Week fromDeprecatedOverload = new Week(fuzzDate, chosenZone);
                Week fromExplicitLocale = new Week(fuzzDate, chosenZone, Locale.getDefault());

                /* Contract: Week(Date, TimeZone) is documented as the same operation
                 * "calculated relative to the specified time zone", and the deprecation
                 * points callers to Week(Date, TimeZone, Locale). For any correct
                 * implementation under a fixed default locale, these two real API calls
                 * must agree on observable state. A patch that ignores the supplied zone
                 * or otherwise silently computes the wrong week breaks this relation
                 * without throwing.
                 */
                if (fromDeprecatedOverload.getWeek() != fromExplicitLocale.getWeek()
                        || fromDeprecatedOverload.getYearValue() != fromExplicitLocale.getYearValue()
                        || fromDeprecatedOverload.getFirstMillisecond() != fromExplicitLocale.getFirstMillisecond()
                        || fromDeprecatedOverload.getLastMillisecond() != fromExplicitLocale.getLastMillisecond()
                        || fromDeprecatedOverload.getSerialIndex() != fromExplicitLocale.getSerialIndex()
                        || !fromDeprecatedOverload.equals(fromExplicitLocale)) {
                    throw new RuntimeException(
                            "[oracle:overload-equivalence] metamorphic violation: Week(Date,TimeZone) != Week(Date,TimeZone,Locale.getDefault())"
                                    + " inputMillis=" + fuzzDate.getTime()
                                    + " zone=" + chosenZone.getID()
                                    + " defaultLocale=" + Locale.getDefault()
                                    + " lhsWeek=" + fromDeprecatedOverload.getWeek()
                                    + " rhsWeek=" + fromExplicitLocale.getWeek()
                                    + " lhsYear=" + fromDeprecatedOverload.getYearValue()
                                    + " rhsYear=" + fromExplicitLocale.getYearValue()
                                    + " lhsFirstMs=" + fromDeprecatedOverload.getFirstMillisecond()
                                    + " rhsFirstMs=" + fromExplicitLocale.getFirstMillisecond()
                                    + " lhsLastMs=" + fromDeprecatedOverload.getLastMillisecond()
                                    + " rhsLastMs=" + fromExplicitLocale.getLastMillisecond()
                                    + " lhsSerial=" + fromDeprecatedOverload.getSerialIndex()
                                    + " rhsSerial=" + fromExplicitLocale.getSerialIndex()
                                    + " lhsEqualsRhs=" + fromDeprecatedOverload.equals(fromExplicitLocale));
                }

                Calendar sameCalendar = Calendar.getInstance(chosenZone, Locale.getDefault());
                /* Documented sibling guarantee: getFirstMillisecond()/getLastMillisecond()
                 * are determined relative to the time zone specified in the constructor,
                 * or the most recent peg(Calendar). Before any peg() call, evaluating with
                 * an equivalent Calendar should match the no-arg readers.
                 */
                if (fromExplicitLocale.getFirstMillisecond() != fromExplicitLocale.getFirstMillisecond(sameCalendar)) {
                    throw new RuntimeException(
                            "[oracle:first-ms-sibling] metamorphic violation: getFirstMillisecond() disagrees with getFirstMillisecond(Calendar)"
                                    + " inputMillis=" + fuzzDate.getTime()
                                    + " zone=" + chosenZone.getID()
                                    + " locale=" + Locale.getDefault()
                                    + " lhs=" + fromExplicitLocale.getFirstMillisecond()
                                    + " rhs=" + fromExplicitLocale.getFirstMillisecond(sameCalendar));
                }
                if (fromExplicitLocale.getLastMillisecond() != fromExplicitLocale.getLastMillisecond(sameCalendar)) {
                    throw new RuntimeException(
                            "[oracle:last-ms-sibling] metamorphic violation: getLastMillisecond() disagrees with getLastMillisecond(Calendar)"
                                    + " inputMillis=" + fuzzDate.getTime()
                                    + " zone=" + chosenZone.getID()
                                    + " locale=" + Locale.getDefault()
                                    + " lhs=" + fromExplicitLocale.getLastMillisecond()
                                    + " rhs=" + fromExplicitLocale.getLastMillisecond(sameCalendar));
                }
            } catch (Throwable ignored) {
                return;
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}
```
**[user]**
```
Your harness computes an array/list index that can go NEGATIVE: near line 77: `Math.abs(data.consumeInt()) %` — Math.abs(Integer.MIN_VALUE) is NEGATIVE, so this index eventually goes out of bounds and crashes the harness itself. Use Math.floorMod(value, n) or a bounded consume (e.g. data.consumeInt(0, n - 1)) instead
Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~8,622 chars)</summary>

```
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-da-firstday] semantic mismatch: expected=" + Calendar.MONDAY
                                + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-da-week] semantic mismatch: expected=34 actual=" + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-firstday] semantic mismatch: expected=" + Calendar.SUNDAY
                                + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-week] semantic mismatch: expected=35 actual=" + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-explicit-locale-week] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            TimeZone[] zones = new TimeZone[] {
                    TimeZone.getTimeZone("Europe/Copenhagen"),
                    TimeZone.getTimeZone("US/Detroit"),
                    TimeZone.getTimeZone("UTC"),
                    TimeZone.getTimeZone("Asia/Tokyo")
            };
            Locale[] locales = new Locale[] {
                    Locale.US,
                    new Locale("da", "DK"),
                    Locale.UK,
                    Locale.GERMANY
            };

            TimeZone chosenZone = zones[data.consumeInt(0, zones.length - 1)];
            Locale chosenDefaultLocale = locales[data.consumeInt(0, locales.length - 1)];
            Locale.setDefault(chosenDefaultLocale);

            int seconds = data.consumeInt(-1_000_000, 1_000_000);
            Date fuzzDate = new Date(seconds * 1000L);

            try {
                Week fromDeprecatedOverload = new Week(fuzzDate, chosenZone);
                Week fromExplicitLocale = new Week(fuzzDate, chosenZone, Locale.getDefault());

                /* Contract: Week(Date, TimeZone) is documented as the same operation
                 * "calculated relative to the specified time zone", and the deprecation
                 * points callers to Week(Date, TimeZone, Locale). For any correct
                 * implementation under a fixed default locale, these two real API calls
                 * must agree on observable state. A patch that ignores the supplied zone
                 * or otherwise silently computes the wrong week breaks this relation
                 * without throwing.
                 */
                if (fromDeprecatedOverload.getWeek() != fromExplicitLocale.getWeek()
                        || fromDeprecatedOverload.getYearValue() != fromExplicitLocale.getYearValue()
                        || fromDeprecatedOverload.getFirstMillisecond() != fromExplicitLocale.getFirstMillisecond()
                        || fromDeprecatedOverload.getLastMillisecond() != fromExplicitLocale.getLastMillisecond()
                        || fromDeprecatedOverload.getSerialIndex() != fromExplicitLocale.getSerialIndex()
                        || !fromDeprecatedOverload.equals(fromExplicitLocale)) {
                    throw new RuntimeException(
                            "[oracle:overload-equivalence] metamorphic violation: Week(Date,TimeZone) != Week(Date,TimeZone,Locale.getDefault())"
                                    + " inputMillis=" + fuzzDate.getTime()
                                    + " zone=" + chosenZone.getID()
                                    + " defaultLocale=" + Locale.getDefault()
                                    + " lhsWeek=" + fromDeprecatedOverload.getWeek()
                                    + " rhsWeek=" + fromExplicitLocale.getWeek()
                                    + " lhsYear=" + fromDeprecatedOverload.getYearValue()
                                    + " rhsYear=" + fromExplicitLocale.getYearValue()
                                    + " lhsFirstMs=" + fromDeprecatedOverload.getFirstMillisecond()
                                    + " rhsFirstMs=" + fromExplicitLocale.getFirstMillisecond()
                                    + " lhsLastMs=" + fromDeprecatedOverload.getLastMillisecond()
                                    + " rhsLastMs=" + fromExplicitLocale.getLastMillisecond()
                                    + " lhsSerial=" + fromDeprecatedOverload.getSerialIndex()
                                    + " rhsSerial=" + fromExplicitLocale.getSerialIndex()
                                    + " lhsEqualsRhs=" + fromDeprecatedOverload.equals(fromExplicitLocale));
                }

                Calendar sameCalendar = Calendar.getInstance(chosenZone, Locale.getDefault());
                /* Documented sibling guarantee: getFirstMillisecond()/getLastMillisecond()
                 * are determined relative to the time zone specified in the constructor,
                 * or the most recent peg(Calendar). Before any peg() call, evaluating with
                 * an equivalent Calendar should match the no-arg readers.
                 */
                if (fromExplicitLocale.getFirstMillisecond() != fromExplicitLocale.getFirstMillisecond(sameCalendar)) {
                    throw new RuntimeException(
                            "[oracle:first-ms-sibling] metamorphic violation: getFirstMillisecond() disagrees with getFirstMillisecond(Calendar)"
                                    + " inputMillis=" + fuzzDate.getTime()
                                    + " zone=" + chosenZone.getID()
                                    + " locale=" + Locale.getDefault()
                                    + " lhs=" + fromExplicitLocale.getFirstMillisecond()
                                    + " rhs=" + fromExplicitLocale.getFirstMillisecond(sameCalendar));
                }
                if (fromExplicitLocale.getLastMillisecond() != fromExplicitLocale.getLastMillisecond(sameCalendar)) {
                    throw new RuntimeException(
                            "[oracle:last-ms-sibling] metamorphic violation: getLastMillisecond() disagrees with getLastMillisecond(Calendar)"
                                    + " inputMillis=" + fuzzDate.getTime()
                                    + " zone=" + chosenZone.getID()
                                    + " locale=" + Locale.getDefault()
                                    + " lhs=" + fromExplicitLocale.getLastMillisecond()
                                    + " rhs=" + fromExplicitLocale.getLastMillisecond(sameCalendar));
                }
            } catch (Throwable ignored) {
                return;
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}
```

</details>

---
## [86] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. Jazzer exited cleanly with no finding on the buggy code. Most often this means every input was handled normally, or the harnes

---
## [87] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~11,498 chars, 2 new)</summary>

- *[system] message: identical to step [81] — not reprinted*
- *[user] message: identical to step [83] — not reprinted*
- *[assistant] message: identical to step [85] — not reprinted*
- *[user] message: identical to step [85] — not reprinted*
**[assistant]**
```
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-da-firstday] semantic mismatch: expected=" + Calendar.MONDAY
                                + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-da-week] semantic mismatch: expected=34 actual=" + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-firstday] semantic mismatch: expected=" + Calendar.SUNDAY
                                + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-week] semantic mismatch: expected=35 actual=" + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-explicit-locale-week] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            TimeZone[] zones = new TimeZone[] {
                    TimeZone.getTimeZone("Europe/Copenhagen"),
                    TimeZone.getTimeZone("US/Detroit"),
                    TimeZone.getTimeZone("UTC"),
                    TimeZone.getTimeZone("Asia/Tokyo")
            };
            Locale[] locales = new Locale[] {
                    Locale.US,
                    new Locale("da", "DK"),
                    Locale.UK,
                    Locale.GERMANY
            };

            TimeZone chosenZone = zones[data.consumeInt(0, zones.length - 1)];
            Locale chosenDefaultLocale = locales[data.consumeInt(0, locales.length - 1)];
            Locale.setDefault(chosenDefaultLocale);

            int seconds = data.consumeInt(-1_000_000, 1_000_000);
            Date fuzzDate = new Date(seconds * 1000L);

            try {
                Week fromDeprecatedOverload = new Week(fuzzDate, chosenZone);
                Week fromExplicitLocale = new Week(fuzzDate, chosenZone, Locale.getDefault());

                /* Contract: Week(Date, TimeZone) is documented as the same operation
                 * "calculated relative to the specified time zone", and the deprecation
                 * points callers to Week(Date, TimeZone, Locale). For any correct
                 * implementation under a fixed default locale, these two real API calls
                 * must agree on observable state. A patch that ignores the supplied zone
                 * or otherwise silently computes the wrong week breaks this relation
                 * without throwing.
                 */
                if (fromDeprecatedOverload.getWeek() != fromExplicitLocale.getWeek()
                        || fromDeprecatedOverload.getYearValue() != fromExplicitLocale.getYearValue()
                        || fromDeprecatedOverload.getFirstMillisecond() != fromExplicitLocale.getFirstMillisecond()
                        || fromDeprecatedOverload.getLastMillisecond() != fromExplicitLocale.getLastMillisecond()
                        || fromDeprecatedOverload.getSerialIndex() != fromExplicitLocale.getSerialIndex()
                        || !fromDeprecatedOverload.equals(fromExplicitLocale)) {
                    throw new RuntimeException(
                            "[oracle:overload-equivalence] metamorphic violation: Week(Date,TimeZone) != Week(Date,TimeZone,Locale.getDefault())"
                                    + " inputMillis=" + fuzzDate.getTime()
                                    + " zone=" + chosenZone.getID()
                                    + " defaultLocale=" + Locale.getDefault()
                                    + " lhsWeek=" + fromDeprecatedOverload.getWeek()
                                    + " rhsWeek=" + fromExplicitLocale.getWeek()
                                    + " lhsYear=" + fromDeprecatedOverload.getYearValue()
                                    + " rhsYear=" + fromExplicitLocale.getYearValue()
                                    + " lhsFirstMs=" + fromDeprecatedOverload.getFirstMillisecond()
                                    + " rhsFirstMs=" + fromExplicitLocale.getFirstMillisecond()
                                    + " lhsLastMs=" + fromDeprecatedOverload.getLastMillisecond()
                                    + " rhsLastMs=" + fromExplicitLocale.getLastMillisecond()
                                    + " lhsSerial=" + fromDeprecatedOverload.getSerialIndex()
                                    + " rhsSerial=" + fromExplicitLocale.getSerialIndex()
                                    + " lhsEqualsRhs=" + fromDeprecatedOverload.equals(fromExplicitLocale));
                }

                Calendar sameCalendar = Calendar.getInstance(chosenZone, Locale.getDefault());
                /* Documented sibling guarantee: getFirstMillisecond()/getLastMillisecond()
                 * are determined relative to the time zone specified in the constructor,
                 * or the most recent peg(Calendar). Before any peg() call, evaluating with
                 * an equivalent Calendar should match the no-arg readers.
                 */
                if (fromExplicitLocale.getFirstMillisecond() != fromExplicitLocale.getFirstMillisecond(sameCalendar)) {
                    throw new RuntimeException(
                            "[oracle:first-ms-sibling] metamorphic violation: getFirstMillisecond() disagrees with getFirstMillisecond(Calendar)"
                                    + " inputMillis=" + fuzzDate.getTime()
                                    + " zone=" + chosenZone.getID()
                                    + " locale=" + Locale.getDefault()
                                    + " lhs=" + fromExplicitLocale.getFirstMillisecond()
                                    + " rhs=" + fromExplicitLocale.getFirstMillisecond(sameCalendar));
                }
                if (fromExplicitLocale.getLastMillisecond() != fromExplicitLocale.getLastMillisecond(sameCalendar)) {
                    throw new RuntimeException(
                            "[oracle:last-ms-sibling] metamorphic violation: getLastMillisecond() disagrees with getLastMillisecond(Calendar)"
                                    + " inputMillis=" + fuzzDate.getTime()
                                    + " zone=" + chosenZone.getID()
                                    + " locale=" + Locale.getDefault()
                                    + " lhs=" + fromExplicitLocale.getLastMillisecond()
                                    + " rhs=" + fromExplicitLocale.getLastMillisecond(sameCalendar));
                }
            } catch (Throwable ignored) {
                return;
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}
```
**[user]**
```
That compiled, but it did NOT trigger the bug on the known-buggy version. Jazzer exited cleanly with no finding on the buggy code. Most often this means every input was handled normally, or the harness itself caught and swallowed the exception that should have propagated.

The patch under analysis is below. The buggy version is the code BEFORE this patch is applied (the '+' lines are the FIXED code; the buggy code has the '-' lines instead):
--- /source/org/jfree/data/time/Week.java
+++ /source/org/jfree/data/time/Week.java
@@ -172,7 +172,7 @@
      */
     public Week(Date time, TimeZone zone) {
         // defer argument checking...
-        this(time, RegularTimePeriod.DEFAULT_TIME_ZONE, Locale.getDefault());
+       this(time, zone, Locale.getDefault());
     }
 
     /**


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
<details open><summary>▸ Output (~8,173 chars)</summary>

```
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-da-firstday] semantic mismatch: expected=" + Calendar.MONDAY
                                + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-da-week] semantic mismatch: expected=34 actual=" + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-firstday] semantic mismatch: expected=" + Calendar.SUNDAY
                                + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-week] semantic mismatch: expected=35 actual=" + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"), new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-explicit-locale-week] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            TimeZone[] zones = new TimeZone[] {
                    TimeZone.getTimeZone("Europe/Copenhagen"),
                    TimeZone.getTimeZone("US/Detroit"),
                    TimeZone.getTimeZone("UTC"),
                    TimeZone.getTimeZone("Asia/Tokyo")
            };
            Locale[] locales = new Locale[] {
                    Locale.US,
                    new Locale("da", "DK"),
                    Locale.UK,
                    Locale.GERMANY
            };

            TimeZone chosenZone = zones[data.consumeInt(0, zones.length - 1)];
            Locale chosenLocale = locales[data.consumeInt(0, locales.length - 1)];
            long millis = data.consumeInt(-1_000_000, 1_000_000) * 1000L;
            Date fuzzDate = new Date(millis);

            Locale.setDefault(chosenLocale);

            Week deprecatedOverload = new Week(fuzzDate, chosenZone);
            Week explicitLocale = new Week(fuzzDate, chosenZone, Locale.getDefault());

            /* Contract: both constructors are documented as creating "a time period
             * for the week in which the specified date/time falls, calculated relative
             * to the specified time zone". The deprecated overload delegates to the
             * 3-arg overload, so for the same Date, TimeZone, and default Locale they
             * must expose the same week/year/millisecond state. A buggy patch that
             * ignores the supplied zone violates this observable post-condition even
             * when no exception is thrown.
             */
            if (deprecatedOverload.getWeek() != explicitLocale.getWeek()
                    || deprecatedOverload.getYearValue() != explicitLocale.getYearValue()
                    || deprecatedOverload.getFirstMillisecond() != explicitLocale.getFirstMillisecond()
                    || deprecatedOverload.getLastMillisecond() != explicitLocale.getLastMillisecond()
                    || deprecatedOverload.getSerialIndex() != explicitLocale.getSerialIndex()
                    || !deprecatedOverload.equals(explicitLocale)) {
                throw new RuntimeException(
                        "[oracle:overload-equivalence] metamorphic violation: Week(Date,TimeZone) != Week(Date,TimeZone,Locale.getDefault())"
                                + " inputMillis=" + fuzzDate.getTime()
                                + " zone=" + chosenZone.getID()
                                + " locale=" + Locale.getDefault()
                                + " lhsWeek=" + deprecatedOverload.getWeek()
                                + " rhsWeek=" + explicitLocale.getWeek()
                                + " lhsYear=" + deprecatedOverload.getYearValue()
                                + " rhsYear=" + explicitLocale.getYearValue()
                                + " lhsFirstMs=" + deprecatedOverload.getFirstMillisecond()
                                + " rhsFirstMs=" + explicitLocale.getFirstMillisecond()
                                + " lhsLastMs=" + deprecatedOverload.getLastMillisecond()
                                + " rhsLastMs=" + explicitLocale.getLastMillisecond()
                                + " lhsSerial=" + deprecatedOverload.getSerialIndex()
                                + " rhsSerial=" + explicitLocale.getSerialIndex()
                                + " lhsEqualsRhs=" + deprecatedOverload.equals(explicitLocale));
            }

            Calendar equivalentCalendar = Calendar.getInstance(chosenZone, Locale.getDefault());

            /* Documented sibling guarantee: getFirstMillisecond()/getLastMillisecond()
             * are timezone-dependent and may be evaluated either from constructor state
             * or using the supplied Calendar. For the same zone+locale basis, the
             * no-arg reader and Calendar-taking sibling must agree.
             */
            long lhsFirst = explicitLocale.getFirstMillisecond();
            long rhsFirst = explicitLocale.getFirstMillisecond(equivalentCalendar);
            if (lhsFirst != rhsFirst) {
                throw new RuntimeException(
                        "[oracle:first-ms-sibling] metamorphic violation: getFirstMillisecond() disagrees with getFirstMillisecond(Calendar)"
                                + " inputMillis=" + fuzzDate.getTime()
                                + " zone=" + chosenZone.getID()
                                + " locale=" + Locale.getDefault()
                                + " lhs=" + lhsFirst
                                + " rhs=" + rhsFirst);
            }

            long lhsLast = explicitLocale.getLastMillisecond();
            long rhsLast = explicitLocale.getLastMillisecond(equivalentCalendar);
            if (lhsLast != rhsLast) {
                throw new RuntimeException(
                        "[oracle:last-ms-sibling] metamorphic violation: getLastMillisecond() disagrees with getLastMillisecond(Calendar)"
                                + " inputMillis=" + fuzzDate.getTime()
                                + " zone=" + chosenZone.getID()
                                + " locale=" + Locale.getDefault()
                                + " lhs=" + lhsLast
                                + " rhs=" + rhsLast);
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}
```

</details>

---
## [88] ⚙️ harness-attempt · `attempt_004`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.RuntimeException: [oracle:overload-equivalence] metamorphic violation: Week(Date,TimeZone) != Week(Date,TimeZone,Locale.getDefault()) inputMillis=-1000000000 zone=Asia/Tokyo locale=en_US lhs…

---
## [89] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [81] — not reprinted*
- *[user] message: identical to step [81] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~9,638 chars)</summary>

```
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        liftWeekTestsTestConstructor();

        relationCtorZoneOverloadAgreesWithExplicitLocale(data);
        relationFirstMillisecondMatchesCalendarOverloadAfterPeg(data);
        relationLastMillisecondMatchesCalendarOverloadAfterPeg(data);
    }

    private static void liftWeekTestsTestConstructor() {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            int actualFirstDay = cal.getFirstDayOfWeek();
            if (actualFirstDay != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-da-firstday] semantic mismatch: expected="
                                + Calendar.MONDAY + " actual=" + actualFirstDay);
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            int actualWeek = w.getWeek();
            if (actualWeek != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-da-week] semantic mismatch: expected=34 actual="
                                + actualWeek);
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            actualFirstDay = cal.getFirstDayOfWeek();
            if (actualFirstDay != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-firstday] semantic mismatch: expected="
                                + Calendar.SUNDAY + " actual=" + actualFirstDay);
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            actualWeek = w.getWeek();
            if (actualWeek != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-to-copenhagen-2arg] semantic mismatch: expected=35 actual="
                                + actualWeek);
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            actualWeek = w.getWeek();
            if (actualWeek != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-to-copenhagen-3arg] semantic mismatch: expected=34 actual="
                                + actualWeek);
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void relationCtorZoneOverloadAgreesWithExplicitLocale(FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        String violation = null;
        try {
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds.length == 0) {
                return;
            }

            TimeZone sourceZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            TimeZone targetZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            Locale defLocale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");

            Date t;
            try {
                Locale.setDefault(defLocale);
                TimeZone.setDefault(sourceZone);
                Calendar cal = Calendar.getInstance(sourceZone, defLocale);
                cal.clear();
                cal.set(data.consumeInt(1900, 9999),
                        data.consumeInt(0, 11),
                        data.consumeInt(1, 28),
                        data.consumeInt(0, 23),
                        data.consumeInt(0, 59),
                        data.consumeInt(0, 59));
                cal.set(Calendar.MILLISECOND, data.consumeInt(0, 999));
                t = cal.getTime();
            } catch (Throwable e) {
                return;
            }

            Week a;
            try {
                a = new Week(t, targetZone);
            } catch (Throwable e) {
                return;
            }

            Week b;
            try {
                b = new Week(t, targetZone, Locale.getDefault());
            } catch (Throwable e) {
                return;
            }

            int aWeek;
            int bWeek;
            int aYear;
            int bYear;
            try {
                aWeek = a.getWeek();
                bWeek = b.getWeek();
                aYear = a.getYearValue();
                bYear = b.getYearValue();
            } catch (Throwable e) {
                return;
            }

            if (aWeek != bWeek || aYear != bYear) {
                violation = "[oracle:ctor-zone-overload] metamorphic violation: 2-arg and 3-arg constructors must agree when the 3-arg locale is Locale.getDefault(); week/year 2-arg="
                        + aWeek + "/" + aYear + " 3-arg=" + bWeek + "/" + bYear;
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }

        if (violation != null) {
            throw new FuzzerSecurityIssueLow(violation);
        }
    }

    private static void relationFirstMillisecondMatchesCalendarOverloadAfterPeg(FuzzedDataProvider data) {
        String violation = null;
        Week w;
        Calendar cal;
        try {
            w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds.length == 0) {
                return;
            }
            TimeZone zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            cal = Calendar.getInstance(zone, locale);
            w.peg(cal);
        } catch (Throwable e) {
            return;
        }

        long a;
        long b;
        try {
            a = w.getFirstMillisecond();
            b = w.getFirstMillisecond(cal);
        } catch (Throwable e) {
            return;
        }

        if (a != b) {
            violation = "[oracle:first-ms-after-peg] metamorphic violation: after peg(calendar), getFirstMillisecond() must equal getFirstMillisecond(calendar); noArg="
                    + a + " overload=" + b;
        }

        if (violation != null) {
            throw new FuzzerSecurityIssueLow(violation);
        }
    }

    private static void relationLastMillisecondMatchesCalendarOverloadAfterPeg(FuzzedDataProvider data) {
        String violation = null;
        Week w;
        Calendar cal;
        try {
            w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds.length == 0) {
                return;
            }
            TimeZone zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            cal = Calendar.getInstance(zone, locale);
            w.peg(cal);
        } catch (Throwable e) {
            return;
        }

        long beforeSerial;
        int beforeWeek;
        int beforeYear;
        long a;
        long b;
        long afterSerial;
        int afterWeek;
        int afterYear;
        try {
            beforeSerial = w.getSerialIndex();
            beforeWeek = w.getWeek();
            beforeYear = w.getYearValue();

            a = w.getLastMillisecond();
            b = w.getLastMillisecond(cal);

            afterSerial = w.getSerialIndex();
            afterWeek = w.getWeek();
            afterYear = w.getYearValue();
        } catch (Throwable e) {
            return;
        }

        if (a != b) {
            violation = "[oracle:last-ms-after-peg] metamorphic violation: after peg(calendar), getLastMillisecond() must equal getLastMillisecond(calendar); noArg="
                    + a + " overload=" + b;
        }

        if (violation == null
                && (beforeSerial != afterSerial || beforeWeek != afterWeek || beforeYear != afterYear)) {
            violation = "[oracle:read-only-getters] semantic mismatch: getLastMillisecond* are readers and must not change observable week identity; before serial/week/year="
                    + beforeSerial + "/" + beforeWeek + "/" + beforeYear
                    + " after=" + afterSerial + "/" + afterWeek + "/" + afterYear;
        }

        if (violation != null) {
            throw new FuzzerSecurityIssueLow(violation);
        }
    }
}
```

</details>

---
## [90] ⚙️ harness-attempt · `attempt_005`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:ctor-zone-overload] metamorphic violation: 2-arg and 3-arg constructors must agree when the 3-arg locale is Locale.getDefault(); week/y…

---
## [91] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [81] — not reprinted*
- *[user] message: identical to step [83] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~11,508 chars)</summary>

```
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    private static final TimeZone[] ZONES = new TimeZone[] {
        TimeZone.getTimeZone("Europe/Copenhagen"),
        TimeZone.getTimeZone("US/Detroit"),
        TimeZone.getTimeZone("UTC"),
        TimeZone.getTimeZone("Asia/Tokyo"),
        TimeZone.getTimeZone("Europe/London")
    };

    private static final Locale[] LOCALES = new Locale[] {
        new Locale("da", "DK"),
        Locale.US,
        Locale.UK,
        Locale.GERMANY,
        Locale.JAPAN
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            runLiftedConstructorTest();
            runMetamorphicChecks(data);
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void runLiftedConstructorTest() {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-monday] semantic mismatch: expected=2 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-dk-week] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-sunday] semantic mismatch: expected=1 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-default-locale] semantic mismatch: expected=35 actual="
                                + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-explicit-dk-locale] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void runMetamorphicChecks(FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale chosenLocale = LOCALES[Math.abs(data.consumeInt()) % LOCALES.length];
            TimeZone chosenDefaultZone = ZONES[Math.abs(data.consumeInt()) % ZONES.length];
            TimeZone ctorZone = ZONES[Math.abs(data.consumeInt()) % ZONES.length];
            Date t = new Date(data.consumeInt());

            Locale.setDefault(chosenLocale);
            TimeZone.setDefault(chosenDefaultZone);

            Week lhs;
            Week rhs;
            try {
                lhs = new Week(t, ctorZone);
                rhs = new Week(t, ctorZone, Locale.getDefault());
            } catch (Throwable ignored) {
                return;
            }

            /* Contract justification:
             * Week(Date, TimeZone) is documented as "calculated relative to the specified time zone".
             * Week(Date, TimeZone, Locale) is the corresponding overload with explicit locale.
             * Therefore, when the explicit locale is Locale.getDefault(), the two overloads must
             * describe the same week for the same Date and TimeZone. A patch that ignores the
             * supplied zone/locale or silently delegates to the wrong overload breaks this without throwing.
             */
            if (lhs.getWeek() != rhs.getWeek()) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:equiv-week] metamorphic violation: Week(Date,TimeZone) must agree with Week(Date,TimeZone,Locale.getDefault())"
                                + " inputMillis=" + t.getTime()
                                + " zone=" + ctorZone.getID()
                                + " defaultLocale=" + Locale.getDefault()
                                + " lhsWeek=" + lhs.getWeek()
                                + " rhsWeek=" + rhs.getWeek());
            }
            if (lhs.getYearValue() != rhs.getYearValue()) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:equiv-year] metamorphic violation: equivalent overloads returned different years"
                                + " inputMillis=" + t.getTime()
                                + " zone=" + ctorZone.getID()
                                + " defaultLocale=" + Locale.getDefault()
                                + " lhsYear=" + lhs.getYearValue()
                                + " rhsYear=" + rhs.getYearValue());
            }

            Calendar cal = Calendar.getInstance(ctorZone, Locale.getDefault());

            /* Contract justification:
             * getFirstMillisecond() / getFirstMillisecond(Calendar) and
             * getLastMillisecond() / getLastMillisecond(Calendar) are documented as same-family
             * accessors differing only in where the timezone comes from. After construction with
             * the same zone/locale, evaluating with an equivalent Calendar must match the stored values.
             */
            long lhsFirstNoArg;
            long lhsFirstWithCal;
            long lhsLastNoArg;
            long lhsLastWithCal;
            long rhsFirstNoArg;
            long rhsFirstWithCal;
            long rhsLastNoArg;
            long rhsLastWithCal;
            try {
                lhsFirstNoArg = lhs.getFirstMillisecond();
                lhsFirstWithCal = lhs.getFirstMillisecond(cal);
                lhsLastNoArg = lhs.getLastMillisecond();
                lhsLastWithCal = lhs.getLastMillisecond(cal);

                rhsFirstNoArg = rhs.getFirstMillisecond();
                rhsFirstWithCal = rhs.getFirstMillisecond(cal);
                rhsLastNoArg = rhs.getLastMillisecond();
                rhsLastWithCal = rhs.getLastMillisecond(cal);
            } catch (Throwable ignored) {
                return;
            }

            if (lhsFirstNoArg != lhsFirstWithCal) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:firstmillis-lhs] metamorphic violation: getFirstMillisecond() disagrees with getFirstMillisecond(Calendar)"
                                + " inputMillis=" + t.getTime()
                                + " zone=" + ctorZone.getID()
                                + " locale=" + Locale.getDefault()
                                + " lhsNoArg=" + lhsFirstNoArg
                                + " lhsWithCalendar=" + lhsFirstWithCal);
            }
            if (lhsLastNoArg != lhsLastWithCal) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lastmillis-lhs] metamorphic violation: getLastMillisecond() disagrees with getLastMillisecond(Calendar)"
                                + " inputMillis=" + t.getTime()
                                + " zone=" + ctorZone.getID()
                                + " locale=" + Locale.getDefault()
                                + " lhsNoArg=" + lhsLastNoArg
                                + " lhsWithCalendar=" + lhsLastWithCal);
            }
            if (rhsFirstNoArg != rhsFirstWithCal) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:firstmillis-rhs] metamorphic violation: getFirstMillisecond() disagrees with getFirstMillisecond(Calendar)"
                                + " inputMillis=" + t.getTime()
                                + " zone=" + ctorZone.getID()
                                + " locale=" + Locale.getDefault()
                                + " rhsNoArg=" + rhsFirstNoArg
                                + " rhsWithCalendar=" + rhsFirstWithCal);
            }
            if (rhsLastNoArg != rhsLastWithCal) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lastmillis-rhs] metamorphic violation: getLastMillisecond() disagrees with getLastMillisecond(Calendar)"
                                + " inputMillis=" + t.getTime()
                                + " zone=" + ctorZone.getID()
                                + " locale=" + Locale.getDefault()
                                + " rhsNoArg=" + rhsLastNoArg
                                + " rhsWithCalendar=" + rhsLastWithCal);
            }

            if (lhsFirstNoArg != rhsFirstNoArg || lhsLastNoArg != rhsLastNoArg) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:equiv-bounds] metamorphic violation: equivalent overloads returned different time bounds"
                                + " inputMillis=" + t.getTime()
                                + " zone=" + ctorZone.getID()
                                + " defaultLocale=" + Locale.getDefault()
                                + " lhsFirst=" + lhsFirstNoArg
                                + " rhsFirst=" + rhsFirstNoArg
                                + " lhsLast=" + lhsLastNoArg
                                + " rhsLast=" + rhsLastNoArg);
            }
            if (lhs.getSerialIndex() != rhs.getSerialIndex()) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:equiv-serial] metamorphic violation: equivalent overloads returned different serial indexes"
                                + " inputMillis=" + t.getTime()
                                + " zone=" + ctorZone.getID()
                                + " defaultLocale=" + Locale.getDefault()
                                + " lhsSerial=" + lhs.getSerialIndex()
                                + " rhsSerial=" + rhs.getSerialIndex());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}
```

</details>

---
## [92] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Your harness computes an array/list index that can go NEGATIVE: near line 103: `Math.abs(data.consumeInt()) %` — Math.abs(Integer.MIN_VALUE) is NEGATIVE, so this index eventually goes out of bounds an

---
## [93] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~12,074 chars, 2 new)</summary>

- *[system] message: identical to step [81] — not reprinted*
- *[user] message: identical to step [83] — not reprinted*
**[assistant]**
```
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    private static final TimeZone[] ZONES = new TimeZone[] {
        TimeZone.getTimeZone("Europe/Copenhagen"),
        TimeZone.getTimeZone("US/Detroit"),
        TimeZone.getTimeZone("UTC"),
        TimeZone.getTimeZone("Asia/Tokyo"),
        TimeZone.getTimeZone("Europe/London")
    };

    private static final Locale[] LOCALES = new Locale[] {
        new Locale("da", "DK"),
        Locale.US,
        Locale.UK,
        Locale.GERMANY,
        Locale.JAPAN
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            runLiftedConstructorTest();
            runMetamorphicChecks(data);
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void runLiftedConstructorTest() {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-monday] semantic mismatch: expected=2 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-dk-week] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-sunday] semantic mismatch: expected=1 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-default-locale] semantic mismatch: expected=35 actual="
                                + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-explicit-dk-locale] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void runMetamorphicChecks(FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale chosenLocale = LOCALES[Math.abs(data.consumeInt()) % LOCALES.length];
            TimeZone chosenDefaultZone = ZONES[Math.abs(data.consumeInt()) % ZONES.length];
            TimeZone ctorZone = ZONES[Math.abs(data.consumeInt()) % ZONES.length];
            Date t = new Date(data.consumeInt());

            Locale.setDefault(chosenLocale);
            TimeZone.setDefault(chosenDefaultZone);

            Week lhs;
            Week rhs;
            try {
                lhs = new Week(t, ctorZone);
                rhs = new Week(t, ctorZone, Locale.getDefault());
            } catch (Throwable ignored) {
                return;
            }

            /* Contract justification:
             * Week(Date, TimeZone) is documented as "calculated relative to the specified time zone".
             * Week(Date, TimeZone, Locale) is the corresponding overload with explicit locale.
             * Therefore, when the explicit locale is Locale.getDefault(), the two overloads must
             * describe the same week for the same Date and TimeZone. A patch that ignores the
             * supplied zone/locale or silently delegates to the wrong overload breaks this without throwing.
             */
            if (lhs.getWeek() != rhs.getWeek()) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:equiv-week] metamorphic violation: Week(Date,TimeZone) must agree with Week(Date,TimeZone,Locale.getDefault())"
                                + " inputMillis=" + t.getTime()
                                + " zone=" + ctorZone.getID()
                                + " defaultLocale=" + Locale.getDefault()
                                + " lhsWeek=" + lhs.getWeek()
                                + " rhsWeek=" + rhs.getWeek());
            }
            if (lhs.getYearValue() != rhs.getYearValue()) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:equiv-year] metamorphic violation: equivalent overloads returned different years"
                                + " inputMillis=" + t.getTime()
                                + " zone=" + ctorZone.getID()
                                + " defaultLocale=" + Locale.getDefault()
                                + " lhsYear=" + lhs.getYearValue()
                                + " rhsYear=" + rhs.getYearValue());
            }

            Calendar cal = Calendar.getInstance(ctorZone, Locale.getDefault());

            /* Contract justification:
             * getFirstMillisecond() / getFirstMillisecond(Calendar) and
             * getLastMillisecond() / getLastMillisecond(Calendar) are documented as same-family
             * accessors differing only in where the timezone comes from. After construction with
             * the same zone/locale, evaluating with an equivalent Calendar must match the stored values.
             */
            long lhsFirstNoArg;
            long lhsFirstWithCal;
            long lhsLastNoArg;
            long lhsLastWithCal;
            long rhsFirstNoArg;
            long rhsFirstWithCal;
            long rhsLastNoArg;
            long rhsLastWithCal;
            try {
                lhsFirstNoArg = lhs.getFirstMillisecond();
                lhsFirstWithCal = lhs.getFirstMillisecond(cal);
                lhsLastNoArg = lhs.getLastMillisecond();
                lhsLastWithCal = lhs.getLastMillisecond(cal);

                rhsFirstNoArg = rhs.getFirstMillisecond();
                rhsFirstWithCal = rhs.getFirstMillisecond(cal);
                rhsLastNoArg = rhs.getLastMillisecond();
                rhsLastWithCal = rhs.getLastMillisecond(cal);
            } catch (Throwable ignored) {
                return;
            }

            if (lhsFirstNoArg != lhsFirstWithCal) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:firstmillis-lhs] metamorphic violation: getFirstMillisecond() disagrees with getFirstMillisecond(Calendar)"
                                + " inputMillis=" + t.getTime()
                                + " zone=" + ctorZone.getID()
                                + " locale=" + Locale.getDefault()
                                + " lhsNoArg=" + lhsFirstNoArg
                                + " lhsWithCalendar=" + lhsFirstWithCal);
            }
            if (lhsLastNoArg != lhsLastWithCal) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lastmillis-lhs] metamorphic violation: getLastMillisecond() disagrees with getLastMillisecond(Calendar)"
                                + " inputMillis=" + t.getTime()
                                + " zone=" + ctorZone.getID()
                                + " locale=" + Locale.getDefault()
                                + " lhsNoArg=" + lhsLastNoArg
                                + " lhsWithCalendar=" + lhsLastWithCal);
            }
            if (rhsFirstNoArg != rhsFirstWithCal) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:firstmillis-rhs] metamorphic violation: getFirstMillisecond() disagrees with getFirstMillisecond(Calendar)"
                                + " inputMillis=" + t.getTime()
                                + " zone=" + ctorZone.getID()
                                + " locale=" + Locale.getDefault()
                                + " rhsNoArg=" + rhsFirstNoArg
                                + " rhsWithCalendar=" + rhsFirstWithCal);
            }
            if (rhsLastNoArg != rhsLastWithCal) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lastmillis-rhs] metamorphic violation: getLastMillisecond() disagrees with getLastMillisecond(Calendar)"
                                + " inputMillis=" + t.getTime()
                                + " zone=" + ctorZone.getID()
                                + " locale=" + Locale.getDefault()
                                + " rhsNoArg=" + rhsLastNoArg
                                + " rhsWithCalendar=" + rhsLastWithCal);
            }

            if (lhsFirstNoArg != rhsFirstNoArg || lhsLastNoArg != rhsLastNoArg) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:equiv-bounds] metamorphic violation: equivalent overloads returned different time bounds"
                                + " inputMillis=" + t.getTime()
                                + " zone=" + ctorZone.getID()
                                + " defaultLocale=" + Locale.getDefault()
                                + " lhsFirst=" + lhsFirstNoArg
                                + " rhsFirst=" + rhsFirstNoArg
                                + " lhsLast=" + lhsLastNoArg
                                + " rhsLast=" + rhsLastNoArg);
            }
            if (lhs.getSerialIndex() != rhs.getSerialIndex()) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:equiv-serial] metamorphic violation: equivalent overloads returned different serial indexes"
                                + " inputMillis=" + t.getTime()
                                + " zone=" + ctorZone.getID()
                                + " defaultLocale=" + Locale.getDefault()
                                + " lhsSerial=" + lhs.getSerialIndex()
                                + " rhsSerial=" + rhs.getSerialIndex());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}
```
**[user]**
```
Your harness computes an array/list index that can go NEGATIVE: near line 103: `Math.abs(data.consumeInt()) %` — Math.abs(Integer.MIN_VALUE) is NEGATIVE, so this index eventually goes out of bounds and crashes the harness itself. Use Math.floorMod(value, n) or a bounded consume (e.g. data.consumeInt(0, n - 1)) instead
Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~11,490 chars)</summary>

```
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    private static final TimeZone[] ZONES = new TimeZone[] {
        TimeZone.getTimeZone("Europe/Copenhagen"),
        TimeZone.getTimeZone("US/Detroit"),
        TimeZone.getTimeZone("UTC"),
        TimeZone.getTimeZone("Asia/Tokyo"),
        TimeZone.getTimeZone("Europe/London")
    };

    private static final Locale[] LOCALES = new Locale[] {
        new Locale("da", "DK"),
        Locale.US,
        Locale.UK,
        Locale.GERMANY,
        Locale.JAPAN
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            runLiftedConstructorTest();
            runMetamorphicChecks(data);
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void runLiftedConstructorTest() {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-monday] semantic mismatch: expected=2 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-dk-week] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-sunday] semantic mismatch: expected=1 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-default-locale] semantic mismatch: expected=35 actual="
                                + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-explicit-dk-locale] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void runMetamorphicChecks(FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale chosenLocale = LOCALES[data.consumeInt(0, LOCALES.length - 1)];
            TimeZone chosenDefaultZone = ZONES[data.consumeInt(0, ZONES.length - 1)];
            TimeZone ctorZone = ZONES[data.consumeInt(0, ZONES.length - 1)];
            Date t = new Date(data.consumeInt());

            Locale.setDefault(chosenLocale);
            TimeZone.setDefault(chosenDefaultZone);

            Week lhs;
            Week rhs;
            try {
                lhs = new Week(t, ctorZone);
                rhs = new Week(t, ctorZone, Locale.getDefault());
            } catch (Throwable ignored) {
                return;
            }

            /* Contract justification:
             * Week(Date, TimeZone) is documented as "calculated relative to the specified time zone".
             * Week(Date, TimeZone, Locale) is the corresponding overload with explicit locale.
             * Therefore, when the explicit locale is Locale.getDefault(), the two overloads must
             * describe the same week for the same Date and TimeZone. A patch that ignores the
             * supplied zone/locale or silently delegates to the wrong overload breaks this without throwing.
             */
            if (lhs.getWeek() != rhs.getWeek()) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:equiv-week] metamorphic violation: Week(Date,TimeZone) must agree with Week(Date,TimeZone,Locale.getDefault())"
                                + " inputMillis=" + t.getTime()
                                + " zone=" + ctorZone.getID()
                                + " defaultLocale=" + Locale.getDefault()
                                + " lhsWeek=" + lhs.getWeek()
                                + " rhsWeek=" + rhs.getWeek());
            }
            if (lhs.getYearValue() != rhs.getYearValue()) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:equiv-year] metamorphic violation: equivalent overloads returned different years"
                                + " inputMillis=" + t.getTime()
                                + " zone=" + ctorZone.getID()
                                + " defaultLocale=" + Locale.getDefault()
                                + " lhsYear=" + lhs.getYearValue()
                                + " rhsYear=" + rhs.getYearValue());
            }

            Calendar cal = Calendar.getInstance(ctorZone, Locale.getDefault());

            /* Contract justification:
             * getFirstMillisecond() / getFirstMillisecond(Calendar) and
             * getLastMillisecond() / getLastMillisecond(Calendar) are documented as same-family
             * accessors differing only in where the timezone comes from. After construction with
             * the same zone/locale, evaluating with an equivalent Calendar must match the stored values.
             */
            long lhsFirstNoArg;
            long lhsFirstWithCal;
            long lhsLastNoArg;
            long lhsLastWithCal;
            long rhsFirstNoArg;
            long rhsFirstWithCal;
            long rhsLastNoArg;
            long rhsLastWithCal;
            try {
                lhsFirstNoArg = lhs.getFirstMillisecond();
                lhsFirstWithCal = lhs.getFirstMillisecond(cal);
                lhsLastNoArg = lhs.getLastMillisecond();
                lhsLastWithCal = lhs.getLastMillisecond(cal);

                rhsFirstNoArg = rhs.getFirstMillisecond();
                rhsFirstWithCal = rhs.getFirstMillisecond(cal);
                rhsLastNoArg = rhs.getLastMillisecond();
                rhsLastWithCal = rhs.getLastMillisecond(cal);
            } catch (Throwable ignored) {
                return;
            }

            if (lhsFirstNoArg != lhsFirstWithCal) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:firstmillis-lhs] metamorphic violation: getFirstMillisecond() disagrees with getFirstMillisecond(Calendar)"
                                + " inputMillis=" + t.getTime()
                                + " zone=" + ctorZone.getID()
                                + " locale=" + Locale.getDefault()
                                + " lhsNoArg=" + lhsFirstNoArg
                                + " lhsWithCalendar=" + lhsFirstWithCal);
            }
            if (lhsLastNoArg != lhsLastWithCal) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lastmillis-lhs] metamorphic violation: getLastMillisecond() disagrees with getLastMillisecond(Calendar)"
                                + " inputMillis=" + t.getTime()
                                + " zone=" + ctorZone.getID()
                                + " locale=" + Locale.getDefault()
                                + " lhsNoArg=" + lhsLastNoArg
                                + " lhsWithCalendar=" + lhsLastWithCal);
            }
            if (rhsFirstNoArg != rhsFirstWithCal) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:firstmillis-rhs] metamorphic violation: getFirstMillisecond() disagrees with getFirstMillisecond(Calendar)"
                                + " inputMillis=" + t.getTime()
                                + " zone=" + ctorZone.getID()
                                + " locale=" + Locale.getDefault()
                                + " rhsNoArg=" + rhsFirstNoArg
                                + " rhsWithCalendar=" + rhsFirstWithCal);
            }
            if (rhsLastNoArg != rhsLastWithCal) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lastmillis-rhs] metamorphic violation: getLastMillisecond() disagrees with getLastMillisecond(Calendar)"
                                + " inputMillis=" + t.getTime()
                                + " zone=" + ctorZone.getID()
                                + " locale=" + Locale.getDefault()
                                + " rhsNoArg=" + rhsLastNoArg
                                + " rhsWithCalendar=" + rhsLastWithCal);
            }

            if (lhsFirstNoArg != rhsFirstNoArg || lhsLastNoArg != rhsLastNoArg) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:equiv-bounds] metamorphic violation: equivalent overloads returned different time bounds"
                                + " inputMillis=" + t.getTime()
                                + " zone=" + ctorZone.getID()
                                + " defaultLocale=" + Locale.getDefault()
                                + " lhsFirst=" + lhsFirstNoArg
                                + " rhsFirst=" + rhsFirstNoArg
                                + " lhsLast=" + lhsLastNoArg
                                + " rhsLast=" + rhsLastNoArg);
            }
            if (lhs.getSerialIndex() != rhs.getSerialIndex()) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:equiv-serial] metamorphic violation: equivalent overloads returned different serial indexes"
                                + " inputMillis=" + t.getTime()
                                + " zone=" + ctorZone.getID()
                                + " defaultLocale=" + Locale.getDefault()
                                + " lhsSerial=" + lhs.getSerialIndex()
                                + " rhsSerial=" + rhs.getSerialIndex());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}
```

</details>

---
## [94] ⚙️ harness-attempt · `attempt_007`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:firstmillis-lhs] metamorphic violation: getFirstMillisecond() disagrees with getFirstMillisecond(Calendar) inputMillis=0 zone=GMT local…

---
## [95] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [81] — not reprinted*
- *[user] message: identical to step [81] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~11,272 chars)</summary>

```
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            runLiftedWeekTests();
            runCtorOverloadAgreement(data);
            runFirstMillisecondAgreementAfterPeg(data);
            runReadOnlyHiddenStateCheck(data);
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void runLiftedWeekTests() {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-constructor-da-firstday] semantic mismatch: expected="
                                + Calendar.MONDAY + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-constructor-da-week34] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-constructor-us-firstday] semantic mismatch: expected="
                                + Calendar.SUNDAY + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-constructor-us-detroit-to-copenhagen-week35] semantic mismatch: expected=35 actual="
                                + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"), new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-constructor-explicit-da-week34] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void runCtorOverloadAgreement(FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        Date t;
        TimeZone sourceZone;
        TimeZone targetZone;
        Locale defLocale;
        try {
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds.length == 0) {
                return;
            }
            sourceZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            targetZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            defLocale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            Locale.setDefault(defLocale);
            TimeZone.setDefault(sourceZone);

            Calendar cal = Calendar.getInstance(sourceZone, defLocale);
            cal.clear();
            cal.set(
                    data.consumeInt(1900, 9999),
                    data.consumeInt(0, 11),
                    data.consumeInt(1, 28),
                    data.consumeInt(0, 23),
                    data.consumeInt(0, 59),
                    data.consumeInt(0, 59));
            cal.set(Calendar.MILLISECOND, data.consumeInt(0, 999));
            t = cal.getTime();
        } catch (Exception e) {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        }

        Week a;
        Week b;
        try {
            a = new Week(t, targetZone);
            b = new Week(t, targetZone, Locale.getDefault());
        } catch (Exception e) {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }

        if (a.getWeek() != b.getWeek() || a.getYearValue() != b.getYearValue()) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:ctor-zone-overload-agrees-with-explicit-locale] metamorphic violation: 2-arg and 3-arg constructors must agree when the 3-arg locale is Locale.getDefault(); week/year lhs="
                            + a.getWeek() + "/" + a.getYearValue()
                            + " rhs=" + b.getWeek() + "/" + b.getYearValue());
        }
    }

    private static void runFirstMillisecondAgreementAfterPeg(FuzzedDataProvider data) {
        Week w;
        Calendar cal;
        try {
            w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds.length == 0) {
                return;
            }
            TimeZone zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            cal = Calendar.getInstance(zone, locale);
            w.peg(cal);
        } catch (Exception e) {
            return;
        }

        long noArg;
        long withCal;
        try {
            noArg = w.getFirstMillisecond();
            withCal = w.getFirstMillisecond(cal);
        } catch (Exception e) {
            return;
        }

        if (noArg != withCal) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:first-millisecond-after-peg] metamorphic violation: getFirstMillisecond() must equal getFirstMillisecond(calendar) after peg(calendar); lhs="
                            + noArg + " rhs=" + withCal);
        }
    }

    private static void runReadOnlyHiddenStateCheck(FuzzedDataProvider data) {
        Week w;
        Calendar cal;
        try {
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds.length == 0) {
                return;
            }
            TimeZone zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            cal = Calendar.getInstance(zone, locale);
            cal.clear();
            cal.set(
                    data.consumeInt(1900, 9999),
                    data.consumeInt(0, 11),
                    data.consumeInt(1, 28),
                    data.consumeInt(0, 23),
                    data.consumeInt(0, 59),
                    data.consumeInt(0, 59));
            cal.set(Calendar.MILLISECOND, data.consumeInt(0, 999));
            Date t = cal.getTime();

            Locale savedLocale = Locale.getDefault();
            TimeZone savedZone = TimeZone.getDefault();
            try {
                Locale.setDefault(locale);
                TimeZone.setDefault(zone);
                w = new Week(t, zone);
            } finally {
                Locale.setDefault(savedLocale);
                TimeZone.setDefault(savedZone);
            }
        } catch (Exception e) {
            return;
        }

        int weekBefore;
        int yearValueBefore;
        long serialBefore;
        long firstBefore;
        long lastBefore;
        int hashBefore;
        String stringBefore;
        try {
            weekBefore = w.getWeek();
            yearValueBefore = w.getYearValue();
            serialBefore = w.getSerialIndex();
            firstBefore = w.getFirstMillisecond();
            lastBefore = w.getLastMillisecond();
            hashBefore = w.hashCode();
            stringBefore = w.toString();
        } catch (Exception e) {
            return;
        }

        try {
            w.getWeek();
            w.getYear();
            w.getYearValue();
            w.getSerialIndex();
            w.getFirstMillisecond();
            w.getLastMillisecond();
            w.toString();
            w.hashCode();
        } catch (Exception e) {
            return;
        }

        int weekAfter;
        int yearValueAfter;
        long serialAfter;
        long firstAfter;
        long lastAfter;
        int hashAfter;
        String stringAfter;
        try {
            weekAfter = w.getWeek();
            yearValueAfter = w.getYearValue();
            serialAfter = w.getSerialIndex();
            firstAfter = w.getFirstMillisecond();
            lastAfter = w.getLastMillisecond();
            hashAfter = w.hashCode();
            stringAfter = w.toString();
        } catch (Exception e) {
            return;
        }

        /* Contract justification: these are question/read methods (get*/hashCode/toString) with no
           documented mutation; a patch that silently changes internal state instead of computing the
           correct week could break these stable observables without throwing. */
        if (weekBefore != weekAfter
                || yearValueBefore != yearValueAfter
                || serialBefore != serialAfter
                || firstBefore != firstAfter
                || lastBefore != lastAfter
                || hashBefore != hashAfter
                || (stringBefore == null ? stringAfter != null : !stringBefore.equals(stringAfter))) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:hidden-state-read-only] metamorphic violation: read-only accessors changed observable state; before="
                            + weekBefore + "/" + yearValueBefore + "/" + serialBefore + "/"
                            + firstBefore + "/" + lastBefore + "/" + hashBefore + "/"
                            + String.valueOf(stringBefore)
                            + " after="
                            + weekAfter + "/" + yearValueAfter + "/" + serialAfter + "/"
                            + firstAfter + "/" + lastAfter + "/" + hashAfter + "/"
                            + String.valueOf(stringAfter));
        }
    }
}
```

</details>

---
## [96] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [97] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~16,263 chars, 2 new)</summary>

- *[system] message: identical to step [81] — not reprinted*
- *[user] message: identical to step [81] — not reprinted*
**[assistant]**
```
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            runLiftedWeekTests();
            runCtorOverloadAgreement(data);
            runFirstMillisecondAgreementAfterPeg(data);
            runReadOnlyHiddenStateCheck(data);
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void runLiftedWeekTests() {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-constructor-da-firstday] semantic mismatch: expected="
                                + Calendar.MONDAY + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-constructor-da-week34] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-constructor-us-firstday] semantic mismatch: expected="
                                + Calendar.SUNDAY + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-constructor-us-detroit-to-copenhagen-week35] semantic mismatch: expected=35 actual="
                                + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"), new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-constructor-explicit-da-week34] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void runCtorOverloadAgreement(FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        Date t;
        TimeZone sourceZone;
        TimeZone targetZone;
        Locale defLocale;
        try {
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds.length == 0) {
                return;
            }
            sourceZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            targetZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            defLocale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            Locale.setDefault(defLocale);
            TimeZone.setDefault(sourceZone);

            Calendar cal = Calendar.getInstance(sourceZone, defLocale);
            cal.clear();
            cal.set(
                    data.consumeInt(1900, 9999),
                    data.consumeInt(0, 11),
                    data.consumeInt(1, 28),
                    data.consumeInt(0, 23),
                    data.consumeInt(0, 59),
                    data.consumeInt(0, 59));
            cal.set(Calendar.MILLISECOND, data.consumeInt(0, 999));
            t = cal.getTime();
        } catch (Exception e) {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        }

        Week a;
        Week b;
        try {
            a = new Week(t, targetZone);
            b = new Week(t, targetZone, Locale.getDefault());
        } catch (Exception e) {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }

        if (a.getWeek() != b.getWeek() || a.getYearValue() != b.getYearValue()) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:ctor-zone-overload-agrees-with-explicit-locale] metamorphic violation: 2-arg and 3-arg constructors must agree when the 3-arg locale is Locale.getDefault(); week/year lhs="
                            + a.getWeek() + "/" + a.getYearValue()
                            + " rhs=" + b.getWeek() + "/" + b.getYearValue());
        }
    }

    private static void runFirstMillisecondAgreementAfterPeg(FuzzedDataProvider data) {
        Week w;
        Calendar cal;
        try {
            w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds.length == 0) {
                return;
            }
            TimeZone zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            cal = Calendar.getInstance(zone, locale);
            w.peg(cal);
        } catch (Exception e) {
            return;
        }

        long noArg;
        long withCal;
        try {
            noArg = w.getFirstMillisecond();
            withCal = w.getFirstMillisecond(cal);
        } catch (Exception e) {
            return;
        }

        if (noArg != withCal) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:first-millisecond-after-peg] metamorphic violation: getFirstMillisecond() must equal getFirstMillisecond(calendar) after peg(calendar); lhs="
                            + noArg + " rhs=" + withCal);
        }
    }

    private static void runReadOnlyHiddenStateCheck(FuzzedDataProvider data) {
        Week w;
        Calendar cal;
        try {
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds.length == 0) {
                return;
            }
            TimeZone zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            cal = Calendar.getInstance(zone, locale);
            cal.clear();
            cal.set(
                    data.consumeInt(1900, 9999),
                    data.consumeInt(0, 11),
                    data.consumeInt(1, 28),
                    data.consumeInt(0, 23),
                    data.consumeInt(0, 59),
                    data.consumeInt(0, 59));
            cal.set(Calendar.MILLISECOND, data.consumeInt(0, 999));
            Date t = cal.getTime();

            Locale savedLocale = Locale.getDefault();
            TimeZone savedZone = TimeZone.getDefault();
            try {
                Locale.setDefault(locale);
                TimeZone.setDefault(zone);
                w = new Week(t, zone);
            } finally {
                Locale.setDefault(savedLocale);
                TimeZone.setDefault(savedZone);
            }
        } catch (Exception e) {
            return;
        }

        int weekBefore;
        int yearValueBefore;
        long serialBefore;
        long firstBefore;
        long lastBefore;
        int hashBefore;
        String stringBefore;
        try {
            weekBefore = w.getWeek();
            yearValueBefore = w.getYearValue();
            serialBefore = w.getSerialIndex();
            firstBefore = w.getFirstMillisecond();
            lastBefore = w.getLastMillisecond();
            hashBefore = w.hashCode();
            stringBefore = w.toString();
        } catch (Exception e) {
            return;
        }

        try {
            w.getWeek();
            w.getYear();
            w.getYearValue();
            w.getSerialIndex();
            w.getFirstMillisecond();
            w.getLastMillisecond();
            w.toString();
            w.hashCode();
        } catch (Exception e) {
            return;
        }

        int weekAfter;
        int yearValueAfter;
        long serialAfter;
        long firstAfter;
        long lastAfter;
        int hashAfter;
        String stringAfter;
        try {
            weekAfter = w.getWeek();
            yearValueAfter = w.getYearValue();
            serialAfter = w.getSerialIndex();
            firstAfter = w.getFirstMillisecond();
            lastAfter = w.getLastMillisecond();
            hashAfter = w.hashCode();
            stringAfter = w.toString();
        } catch (Exception e) {
            return;
        }

        /* Contract justification: these are question/read methods (get*/hashCode/toString) with no
           documented mutation; a patch that silently changes internal state instead of computing the
           correct week could break these stable observables without throwing. */
        if (weekBefore != weekAfter
                || yearValueBefore != yearValueAfter
                || serialBefore != serialAfter
                || firstBefore != firstAfter
                || lastBefore != lastAfter
                || hashBefore != hashAfter
                || (stringBefore == null ? stringAfter != null : !stringBefore.equals(stringAfter))) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:hidden-state-read-only] metamorphic violation: read-only accessors changed observable state; before="
                            + weekBefore + "/" + yearValueBefore + "/" + serialBefore + "/"
                            + firstBefore + "/" + lastBefore + "/" + hashBefore + "/"
                            + String.valueOf(stringBefore)
                            + " after="
                            + weekAfter + "/" + yearValueAfter + "/" + serialAfter + "/"
                            + firstAfter + "/" + lastAfter + "/" + hashAfter + "/"
                            + String.valueOf(stringAfter));
        }
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/attempt_008/FuzzHarness.java:261: error: not a statement
        /* Contract justification: these are question/read methods (get*/hashCode/toString) with no
                                                                                 ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/attempt_008/FuzzHarness.java:261: error: ';' expected
        /* Contract justification: these are question/read methods (get*/hashCode/toString) with no
                                                                                          ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/attempt_008/FuzzHarness.java:261: error: ';' expected
        /* Contract justification: these are question/read methods (get*/hashCode/toString) with no
                                                                                                   ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/attempt_008/FuzzHarness.java:262: error: ';' expected
           documented mutation; a patch that silently changes internal state instead of computing the
                                       ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/attempt_008/FuzzHarness.java:262: error: ';' expected
           documented mutation; a patch that silently changes internal state instead of computing the
                                                     ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/attempt_008/FuzzHarness.java:262: error: ';' expected
           documented mutation; a patch that silently changes internal state instead of computing the
                                                                      ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/attempt_008/FuzzHarness.java:262: error: ';' expected
           documented mutation; a patch that silently changes internal state instead of computing the
                                                                                    ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/attempt_008/FuzzHarness.java:262: error: ';' expected
           documented mutation; a patch that silently changes internal state instead of computing the
                                                                                                 ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/attempt_008/FuzzHarness.java:263: error: ';' expected
           correct week could break these stable observables without throwing. */
                  ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/attempt_008/FuzzHarness.java:263: error: ';' expected
           correct week could break these stable observables without throwing. */
                             ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/attempt_008/FuzzHarness.java:263: error: ';' expected
           correct week could break these stable observables without throwing. */
                                         ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/H
... (truncated)

Return the full corrected FuzzHarness.java. Rules:
- Raw Java source only. No markdown fences. No prose. No explanations. The file starts with a comment or package statement and ends with a closing brace.
- Public class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Use only the FuzzedDataProvider methods listed in the original instructions. Do NOT invent methods like getInt(), consumeDouble(), getRemainingSize(), or consumeIntInRange() — they do not exist.
- Do NOT use classes or methods that are not on the project classpath. If javac says 'cannot find symbol', remove that import and use only classes visible in the source_imports block or java.* / java.awt.*.
```

</details>
<details open><summary>▸ Output (~11,559 chars)</summary>

```
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            runLiftedWeekTests();
            runCtorOverloadAgreement(data);
            runFirstMillisecondAgreementAfterPeg(data);
            runReadOnlyHiddenStateCheck(data);
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void runLiftedWeekTests() {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-constructor-da-firstday] semantic mismatch: expected="
                                + Calendar.MONDAY + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-constructor-da-week34] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-constructor-us-firstday] semantic mismatch: expected="
                                + Calendar.SUNDAY + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-constructor-us-detroit-to-copenhagen-week35] semantic mismatch: expected=35 actual="
                                + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"), new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-constructor-explicit-da-week34] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void runCtorOverloadAgreement(FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        Date t;
        TimeZone sourceZone;
        TimeZone targetZone;
        Locale defLocale;
        try {
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds.length == 0) {
                return;
            }
            sourceZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            targetZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            defLocale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            Locale.setDefault(defLocale);
            TimeZone.setDefault(sourceZone);

            Calendar cal = Calendar.getInstance(sourceZone, defLocale);
            cal.clear();
            cal.set(
                    data.consumeInt(1900, 9999),
                    data.consumeInt(0, 11),
                    data.consumeInt(1, 28),
                    data.consumeInt(0, 23),
                    data.consumeInt(0, 59),
                    data.consumeInt(0, 59));
            cal.set(Calendar.MILLISECOND, data.consumeInt(0, 999));
            t = cal.getTime();
        } catch (Exception e) {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        }

        Week a;
        Week b;
        try {
            a = new Week(t, targetZone);
            b = new Week(t, targetZone, Locale.getDefault());
        } catch (Exception e) {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }

        if (a.getWeek() != b.getWeek() || a.getYearValue() != b.getYearValue()) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:ctor-zone-overload-agrees-with-explicit-locale] metamorphic violation: 2-arg and 3-arg constructors must agree when the 3-arg locale is Locale.getDefault(); lhsWeek="
                            + a.getWeek() + " lhsYear=" + a.getYearValue()
                            + " rhsWeek=" + b.getWeek() + " rhsYear=" + b.getYearValue());
        }
    }

    private static void runFirstMillisecondAgreementAfterPeg(FuzzedDataProvider data) {
        Week w;
        Calendar cal;
        try {
            w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds.length == 0) {
                return;
            }
            TimeZone zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            cal = Calendar.getInstance(zone, locale);
            w.peg(cal);
        } catch (Exception e) {
            return;
        }

        long noArg;
        long withCal;
        try {
            noArg = w.getFirstMillisecond();
            withCal = w.getFirstMillisecond(cal);
        } catch (Exception e) {
            return;
        }

        if (noArg != withCal) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:first-millisecond-after-peg] metamorphic violation: getFirstMillisecond() must equal getFirstMillisecond(calendar) after peg(calendar); lhs="
                            + noArg + " rhs=" + withCal);
        }
    }

    private static void runReadOnlyHiddenStateCheck(FuzzedDataProvider data) {
        Week w;
        Calendar cal;
        try {
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds.length == 0) {
                return;
            }
            TimeZone zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            cal = Calendar.getInstance(zone, locale);
            cal.clear();
            cal.set(
                    data.consumeInt(1900, 9999),
                    data.consumeInt(0, 11),
                    data.consumeInt(1, 28),
                    data.consumeInt(0, 23),
                    data.consumeInt(0, 59),
                    data.consumeInt(0, 59));
            cal.set(Calendar.MILLISECOND, data.consumeInt(0, 999));
            Date t = cal.getTime();

            Locale savedLocale = Locale.getDefault();
            TimeZone savedZone = TimeZone.getDefault();
            try {
                Locale.setDefault(locale);
                TimeZone.setDefault(zone);
                w = new Week(t, zone);
            } finally {
                Locale.setDefault(savedLocale);
                TimeZone.setDefault(savedZone);
            }
        } catch (Exception e) {
            return;
        }

        int weekBefore;
        int yearValueBefore;
        long serialBefore;
        long firstBefore;
        long lastBefore;
        int hashBefore;
        String stringBefore;
        try {
            weekBefore = w.getWeek();
            yearValueBefore = w.getYearValue();
            serialBefore = w.getSerialIndex();
            firstBefore = w.getFirstMillisecond();
            lastBefore = w.getLastMillisecond();
            hashBefore = w.hashCode();
            stringBefore = w.toString();
        } catch (Exception e) {
            return;
        }

        try {
            w.getWeek();
            w.getYear();
            w.getYearValue();
            w.getSerialIndex();
            w.getFirstMillisecond();
            w.getLastMillisecond();
            w.toString();
            w.hashCode();
        } catch (Exception e) {
            return;
        }

        int weekAfter;
        int yearValueAfter;
        long serialAfter;
        long firstAfter;
        long lastAfter;
        int hashAfter;
        String stringAfter;
        try {
            weekAfter = w.getWeek();
            yearValueAfter = w.getYearValue();
            serialAfter = w.getSerialIndex();
            firstAfter = w.getFirstMillisecond();
            lastAfter = w.getLastMillisecond();
            hashAfter = w.hashCode();
            stringAfter = w.toString();
        } catch (Exception e) {
            return;
        }

        // Contract justification: getter-style methods, hashCode(), and toString() are observational reads.
        // A correct implementation should not mutate the Week's visible state merely by answering queries.
        if (weekBefore != weekAfter
                || yearValueBefore != yearValueAfter
                || serialBefore != serialAfter
                || firstBefore != firstAfter
                || lastBefore != lastAfter
                || hashBefore != hashAfter
                || (stringBefore == null ? stringAfter != null : !stringBefore.equals(stringAfter))) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:hidden-state-read-only] metamorphic violation: read-only accessors changed observable state; beforeWeek="
                            + weekBefore
                            + " beforeYear=" + yearValueBefore
                            + " beforeSerial=" + serialBefore
                            + " beforeFirst=" + firstBefore
                            + " beforeLast=" + lastBefore
                            + " beforeHash=" + hashBefore
                            + " beforeString=" + String.valueOf(stringBefore)
                            + " afterWeek=" + weekAfter
                            + " afterYear=" + yearValueAfter
                            + " afterSerial=" + serialAfter
                            + " afterFirst=" + firstAfter
                            + " afterLast=" + lastAfter
                            + " afterHash=" + hashAfter
                            + " afterString=" + String.valueOf(stringAfter));
        }
    }
}
```

</details>

---
## [98] ⚙️ harness-attempt · `attempt_009`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:ctor-zone-overload-agrees-with-explicit-locale] metamorphic violation: 2-arg and 3-arg constructors must agree when the 3-arg locale is…

---
## [99] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [81] — not reprinted*
- *[user] message: identical to step [83] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,064 chars)</summary>

```
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale daDK = new Locale("da", "DK");
            Locale us = Locale.US;
            TimeZone copenhagen = TimeZone.getTimeZone("Europe/Copenhagen");
            TimeZone detroit = TimeZone.getTimeZone("US/Detroit");

            Locale.setDefault(daDK);
            TimeZone.setDefault(copenhagen);
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:gt-da-firstday] semantic mismatch: expected=2 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:gt-da-week] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            Locale.setDefault(us);
            TimeZone.setDefault(detroit);
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:gt-us-firstday] semantic mismatch: expected=1 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:gt-us-week-2arg] semantic mismatch: expected=35 actual="
                                + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"), new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:gt-us-week-3arg] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            try {
                TimeZone defaultZone = data.consumeBoolean() ? copenhagen : detroit;
                TimeZone explicitZone = defaultZone.equals(copenhagen) ? detroit : copenhagen;
                Locale defaultLocale = data.consumeBoolean() ? daDK : us;
                Locale.setDefault(defaultLocale);
                TimeZone.setDefault(defaultZone);

                int offsetSeconds = data.consumeInt(-1_000_000, 1_000_000);
                Date fuzzTime = new Date(offsetSeconds * 1000L);

                Week twoArg = new Week(fuzzTime, explicitZone);
                Week threeArg = new Week(fuzzTime, explicitZone, Locale.getDefault());

                /* Contract: Week(Date, TimeZone) and Week(Date, TimeZone, Locale.getDefault())
                   are documented as calculating the week for the specified date/time relative
                   to the specified time zone; the 2-arg overload is the default-locale sibling.
                   A patch that simply ignores the supplied zone or silently uses the default
                   time zone breaks this observable equivalence without throwing. */
                if (!twoArg.equals(threeArg)
                        || twoArg.getWeek() != threeArg.getWeek()
                        || twoArg.getYearValue() != threeArg.getYearValue()
                        || twoArg.getSerialIndex() != threeArg.getSerialIndex()) {
                    throw new RuntimeException(
                            "[oracle:overload-equivalence] metamorphic violation: Week(Date,TimeZone) != Week(Date,TimeZone,Locale.getDefault())"
                                    + " inputMillis=" + fuzzTime.getTime()
                                    + " defaultZone=" + TimeZone.getDefault().getID()
                                    + " explicitZone=" + explicitZone.getID()
                                    + " defaultLocale=" + Locale.getDefault()
                                    + " lhsWeek=" + twoArg.getWeek()
                                    + " rhsWeek=" + threeArg.getWeek()
                                    + " lhsYear=" + twoArg.getYearValue()
                                    + " rhsYear=" + threeArg.getYearValue()
                                    + " lhsSerial=" + twoArg.getSerialIndex()
                                    + " rhsSerial=" + threeArg.getSerialIndex()
                                    + " lhsEqualsRhs=" + twoArg.equals(threeArg));
                }

                Calendar cmpCal = Calendar.getInstance(explicitZone, Locale.getDefault());
                /* Contract: getFirstMillisecond() / getFirstMillisecond(Calendar) and
                   getLastMillisecond() / getLastMillisecond(Calendar) are same-name overloads
                   over the same state, and the javadocs say the no-arg values are determined
                   relative to the constructor time zone or the most recent peg(Calendar).
                   When evaluated with an equivalent Calendar for that zone/locale, results must agree. */
                if (threeArg.getFirstMillisecond() != threeArg.getFirstMillisecond(cmpCal)) {
                    throw new RuntimeException(
                            "[oracle:first-ms-overload] metamorphic violation: getFirstMillisecond overload disagreement"
                                    + " inputMillis=" + fuzzTime.getTime()
                                    + " zone=" + explicitZone.getID()
                                    + " locale=" + Locale.getDefault()
                                    + " lhs=" + threeArg.getFirstMillisecond()
                                    + " rhs=" + threeArg.getFirstMillisecond(cmpCal));
                }
                if (threeArg.getLastMillisecond() != threeArg.getLastMillisecond(cmpCal)) {
                    throw new RuntimeException(
                            "[oracle:last-ms-overload] metamorphic violation: getLastMillisecond overload disagreement"
                                    + " inputMillis=" + fuzzTime.getTime()
                                    + " zone=" + explicitZone.getID()
                                    + " locale=" + Locale.getDefault()
                                    + " lhs=" + threeArg.getLastMillisecond()
                                    + " rhs=" + threeArg.getLastMillisecond(cmpCal));
                }
            } catch (Throwable ignored) {
                if (ignored instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) {
                    throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ignored;
                }
                if (ignored instanceof RuntimeException
                        && ignored.getMessage() != null
                        && ignored.getMessage().startsWith("[oracle:")) {
                    throw (RuntimeException) ignored;
                }
                return;
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}
```

</details>

---
## [100] ⚙️ harness-attempt · `attempt_010`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.RuntimeException: [oracle:overload-equivalence] metamorphic violation: Week(Date,TimeZone) != Week(Date,TimeZone,Locale.getDefault()) inputMillis=343503000 defaultZone=Europe/Copenhagen expl…

---
## [101] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [81] — not reprinted*
- *[user] message: identical to step [81] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~10,653 chars)</summary>

```
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedWeekTestsConstructorOracles();
        checkCtorZoneOverloadAgreesWithExplicitLocale(data);
        checkFirstMillisecondMatchesCalendarOverloadAfterPeg(data);
        checkReadOnlyGettersDoNotMutateVisibleState(data);
    }

    private static void checkLiftedWeekTestsConstructorOracles() {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            int firstDay = cal.getFirstDayOfWeek();
            if (firstDay != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:weektests-constructor-firstday-dk] semantic mismatch: expected=" +
                        Calendar.MONDAY + " actual=" + firstDay);
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            int week = w.getWeek();
            if (week != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:weektests-constructor-dk-week] semantic mismatch: expected=34 actual=" +
                        week);
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            firstDay = cal.getFirstDayOfWeek();
            if (firstDay != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:weektests-constructor-firstday-us] semantic mismatch: expected=" +
                        Calendar.SUNDAY + " actual=" + firstDay);
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            week = w.getWeek();
            if (week != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:weektests-constructor-us-default-locale] semantic mismatch: expected=35 actual=" +
                        week);
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"), new Locale("da", "DK"));
            week = w.getWeek();
            if (week != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:weektests-constructor-explicit-dk-locale] semantic mismatch: expected=34 actual=" +
                        week);
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void checkCtorZoneOverloadAgreesWithExplicitLocale(FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        Week a;
        Week b;
        Date t;
        try {
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds.length == 0) {
                return;
            }
            TimeZone sourceZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            TimeZone targetZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            Locale defLocale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            Locale.setDefault(defLocale);
            TimeZone.setDefault(sourceZone);

            Calendar cal = Calendar.getInstance(sourceZone, defLocale);
            cal.clear();
            cal.set(
                    data.consumeInt(1900, 9999),
                    data.consumeInt(0, 11),
                    data.consumeInt(1, 28),
                    data.consumeInt(0, 23),
                    data.consumeInt(0, 59),
                    data.consumeInt(0, 59));
            cal.set(Calendar.MILLISECOND, data.consumeInt(0, 999));
            t = cal.getTime();

            try {
                a = new Week(t, targetZone);
            } catch (Exception e) {
                return;
            }
            try {
                b = new Week(t, targetZone, Locale.getDefault());
            } catch (Exception e) {
                return;
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }

        // Contract: Week(Date, TimeZone) is the sibling of Week(Date, TimeZone, Locale)
        // and should use Locale.getDefault(); deleting or bypassing the patched zone/locale
        // logic breaks this observable agreement on week/year.
        if (a.getWeek() != b.getWeek() || a.getYearValue() != b.getYearValue()) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:ctor-zone-overload-agrees-with-explicit-locale] metamorphic violation: " +
                    "2argWeek=" + a.getWeek() + " 2argYear=" + a.getYearValue() +
                    " 3argWeek=" + b.getWeek() + " 3argYear=" + b.getYearValue());
        }
    }

    private static void checkFirstMillisecondMatchesCalendarOverloadAfterPeg(FuzzedDataProvider data) {
        Week w;
        Calendar cal;
        long a;
        long b;
        try {
            w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds.length == 0) {
                return;
            }
            TimeZone zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            cal = Calendar.getInstance(zone, locale);
            w.peg(cal);

            try {
                a = w.getFirstMillisecond();
            } catch (Exception e) {
                return;
            }
            try {
                b = w.getFirstMillisecond(cal);
            } catch (Exception e) {
                return;
            }
        } catch (Exception e) {
            return;
        }

        // Contract: after peg(calendar), the no-arg getter is pegged to that calendar,
        // so getFirstMillisecond() must equal getFirstMillisecond(calendar).
        if (a != b) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:first-millisecond-after-peg] metamorphic violation: noArg=" + a +
                    " calendarOverload=" + b);
        }
    }

    private static void checkReadOnlyGettersDoNotMutateVisibleState(FuzzedDataProvider data) {
        try {
            Week w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds.length == 0) {
                return;
            }
            TimeZone zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            Calendar cal = Calendar.getInstance(zone, locale);
            w.peg(cal);

            int weekBefore = w.getWeek();
            int yearBefore = w.getYearValue();
            long firstBefore = w.getFirstMillisecond();
            long lastBefore = w.getLastMillisecond();
            long serialBefore = w.getSerialIndex();
            int hashBefore = w.hashCode();
            String strBefore = w.toString();

            // Contract: these are public readers (get*/hashCode/toString) with no documented mutation.
            // A patch that silently mutates state instead of correctly computing a value would violate this.
            int weekRead = w.getWeek();
            int yearRead = w.getYearValue();
            long firstRead = w.getFirstMillisecond();
            long lastRead = w.getLastMillisecond();
            long serialRead = w.getSerialIndex();
            int hashRead = w.hashCode();
            String strRead = w.toString();

            int weekAfter = w.getWeek();
            int yearAfter = w.getYearValue();
            long firstAfter = w.getFirstMillisecond();
            long lastAfter = w.getLastMillisecond();
            long serialAfter = w.getSerialIndex();
            int hashAfter = w.hashCode();
            String strAfter = w.toString();

            if (weekBefore != weekRead || weekRead != weekAfter ||
                yearBefore != yearRead || yearRead != yearAfter ||
                firstBefore != firstRead || firstRead != firstAfter ||
                lastBefore != lastRead || lastRead != lastAfter ||
                serialBefore != serialRead || serialRead != serialAfter ||
                hashBefore != hashRead || hashRead != hashAfter ||
                !safeEquals(strBefore, strRead) || !safeEquals(strRead, strAfter)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:read-only-getters-stable] metamorphic violation: " +
                        "week=" + weekBefore + "/" + weekRead + "/" + weekAfter +
                        " year=" + yearBefore + "/" + yearRead + "/" + yearAfter +
                        " first=" + firstBefore + "/" + firstRead + "/" + firstAfter +
                        " last=" + lastBefore + "/" + lastRead + "/" + lastAfter +
                        " serial=" + serialBefore + "/" + serialRead + "/" + serialAfter +
                        " hash=" + hashBefore + "/" + hashRead + "/" + hashAfter +
                        " toString=" + escape(strBefore) + "/" + escape(strRead) + "/" + escape(strAfter));
            }
        } catch (Exception e) {
            return;
        }
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
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
## [102] ⚙️ harness-repair · `attempt_011`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [103] ⚙️ harness-attempt · `attempt_011`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:ctor-zone-overload-agrees-with-explicit-locale] metamorphic violation: 2argWeek=1 2argYear=1900 3argWeek=52 3argYear=1899

---
## [104] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [81] — not reprinted*
- *[user] message: identical to step [83] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,313 chars)</summary>

```
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            int firstDay = cal.getFirstDayOfWeek();
            if (firstDay != Calendar.MONDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-setup-dk] semantic mismatch: expected firstDayOfWeek="
                                + Calendar.MONDAY + " actual=" + firstDay);
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            int actualWeek = w.getWeek();
            if (actualWeek != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-dk-week] semantic mismatch: expected=34 actual=" + actualWeek);
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            firstDay = cal.getFirstDayOfWeek();
            if (firstDay != Calendar.SUNDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-setup-us] semantic mismatch: expected firstDayOfWeek="
                                + Calendar.SUNDAY + " actual=" + firstDay);
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            actualWeek = w.getWeek();
            if (actualWeek != 35) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-week] semantic mismatch: expected=35 actual=" + actualWeek);
            }

            Week w3 = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            int actualWeek3 = w3.getWeek();
            if (actualWeek3 != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-week-locale] semantic mismatch: expected=34 actual=" + actualWeek3);
            }

            try {
                String[] zoneIds = new String[] {
                        "UTC",
                        "Europe/Copenhagen",
                        "US/Detroit",
                        "Asia/Tokyo",
                        "Australia/Sydney"
                };
                Locale[] locales = new Locale[] {
                        Locale.US,
                        new Locale("da", "DK"),
                        Locale.UK,
                        Locale.FRANCE,
                        Locale.JAPAN
                };

                long millis = (long) data.consumeInt();
                TimeZone ctorZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
                Locale defaultLocale = locales[data.consumeInt(0, locales.length - 1)];
                Locale.setDefault(defaultLocale);

                Date fuzzDate = new Date(millis);

                /* Contract justification:
                 * Week(Date, TimeZone) is the deprecated sibling of Week(Date, TimeZone, Locale),
                 * and its javadoc says the week is "calculated relative to the specified time zone";
                 * the implementation is intended to delegate using Locale.getDefault(). Therefore,
                 * with the same Date, TimeZone, and current default Locale, both constructors must
                 * produce the same observable week/year state. A patch that silently ignores the
                 * supplied zone breaks this relation without throwing.
                 */
                Week a = new Week(fuzzDate, ctorZone);
                Week b = new Week(fuzzDate, ctorZone, Locale.getDefault());

                if (a.getWeek() != b.getWeek()
                        || a.getYearValue() != b.getYearValue()
                        || a.getSerialIndex() != b.getSerialIndex()
                        || a.getFirstMillisecond() != b.getFirstMillisecond()
                        || a.getLastMillisecond() != b.getLastMillisecond()) {
                    throw new RuntimeException(
                            "[oracle:ctor-equivalence] metamorphic violation: Week(Date,TimeZone) must match Week(Date,TimeZone,Locale.getDefault())"
                                    + " inputMillis=" + millis
                                    + " zone=" + ctorZone.getID()
                                    + " defaultLocale=" + Locale.getDefault()
                                    + " lhsWeek=" + a.getWeek()
                                    + " rhsWeek=" + b.getWeek()
                                    + " lhsYear=" + a.getYearValue()
                                    + " rhsYear=" + b.getYearValue()
                                    + " lhsSerial=" + a.getSerialIndex()
                                    + " rhsSerial=" + b.getSerialIndex()
                                    + " lhsFirst=" + a.getFirstMillisecond()
                                    + " rhsFirst=" + b.getFirstMillisecond()
                                    + " lhsLast=" + a.getLastMillisecond()
                                    + " rhsLast=" + b.getLastMillisecond());
                }

                /* Contract justification:
                 * Week exposes both getYear() and getYearValue(); the docs say both return the
                 * year in which the week falls, one as a Year object and one as an int. They must
                 * agree for every correctly constructed Week.
                 */
                if (a.getYear().getYear() != a.getYearValue()) {
                    throw new RuntimeException(
                            "[oracle:year-accessor-agreement] metamorphic violation: getYear().getYear() must equal getYearValue()"
                                    + " inputMillis=" + millis
                                    + " zone=" + ctorZone.getID()
                                    + " locale=" + Locale.getDefault()
                                    + " lhs=" + a.getYear().getYear()
                                    + " rhs=" + a.getYearValue());
                }
            } catch (Throwable ignored) {
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}
```

</details>

---
## [105] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. Jazzer exited cleanly with no finding on the buggy code. Most often this means every input was handled normally, or the harnes

---
## [106] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~7,513 chars, 1 new)</summary>

- *[system] message: identical to step [81] — not reprinted*
- *[user] message: identical to step [83] — not reprinted*
**[assistant]**
```
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            int firstDay = cal.getFirstDayOfWeek();
            if (firstDay != Calendar.MONDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-setup-dk] semantic mismatch: expected firstDayOfWeek="
                                + Calendar.MONDAY + " actual=" + firstDay);
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            int actualWeek = w.getWeek();
            if (actualWeek != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-dk-week] semantic mismatch: expected=34 actual=" + actualWeek);
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            firstDay = cal.getFirstDayOfWeek();
            if (firstDay != Calendar.SUNDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-setup-us] semantic mismatch: expected firstDayOfWeek="
                                + Calendar.SUNDAY + " actual=" + firstDay);
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            actualWeek = w.getWeek();
            if (actualWeek != 35) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-week] semantic mismatch: expected=35 actual=" + actualWeek);
            }

            Week w3 = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            int actualWeek3 = w3.getWeek();
            if (actualWeek3 != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-week-locale] semantic mismatch: expected=34 actual=" + actualWeek3);
            }

            try {
                String[] zoneIds = new String[] {
                        "UTC",
                        "Europe/Copenhagen",
                        "US/Detroit",
                        "Asia/Tokyo",
                        "Australia/Sydney"
                };
                Locale[] locales = new Locale[] {
                        Locale.US,
                        new Locale("da", "DK"),
                        Locale.UK,
                        Locale.FRANCE,
                        Locale.JAPAN
                };

                long millis = (long) data.consumeInt();
                TimeZone ctorZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
                Locale defaultLocale = locales[data.consumeInt(0, locales.length - 1)];
                Locale.setDefault(defaultLocale);

                Date fuzzDate = new Date(millis);

                /* Contract justification:
                 * Week(Date, TimeZone) is the deprecated sibling of Week(Date, TimeZone, Locale),
                 * and its javadoc says the week is "calculated relative to the specified time zone";
                 * the implementation is intended to delegate using Locale.getDefault(). Therefore,
                 * with the same Date, TimeZone, and current default Locale, both constructors must
                 * produce the same observable week/year state. A patch that silently ignores the
                 * supplied zone breaks this relation without throwing.
                 */
                Week a = new Week(fuzzDate, ctorZone);
                Week b = new Week(fuzzDate, ctorZone, Locale.getDefault());

                if (a.getWeek() != b.getWeek()
                        || a.getYearValue() != b.getYearValue()
                        || a.getSerialIndex() != b.getSerialIndex()
                        || a.getFirstMillisecond() != b.getFirstMillisecond()
                        || a.getLastMillisecond() != b.getLastMillisecond()) {
                    throw new RuntimeException(
                            "[oracle:ctor-equivalence] metamorphic violation: Week(Date,TimeZone) must match Week(Date,TimeZone,Locale.getDefault())"
                                    + " inputMillis=" + millis
                                    + " zone=" + ctorZone.getID()
                                    + " defaultLocale=" + Locale.getDefault()
                                    + " lhsWeek=" + a.getWeek()
                                    + " rhsWeek=" + b.getWeek()
                                    + " lhsYear=" + a.getYearValue()
                                    + " rhsYear=" + b.getYearValue()
                                    + " lhsSerial=" + a.getSerialIndex()
                                    + " rhsSerial=" + b.getSerialIndex()
                                    + " lhsFirst=" + a.getFirstMillisecond()
                                    + " rhsFirst=" + b.getFirstMillisecond()
                                    + " lhsLast=" + a.getLastMillisecond()
                                    + " rhsLast=" + b.getLastMillisecond());
                }

                /* Contract justification:
                 * Week exposes both getYear() and getYearValue(); the docs say both return the
                 * year in which the week falls, one as a Year object and one as an int. They must
                 * agree for every correctly constructed Week.
                 */
                if (a.getYear().getYear() != a.getYearValue()) {
                    throw new RuntimeException(
                            "[oracle:year-accessor-agreement] metamorphic violation: getYear().getYear() must equal getYearValue()"
                                    + " inputMillis=" + millis
                                    + " zone=" + ctorZone.getID()
                                    + " locale=" + Locale.getDefault()
                                    + " lhs=" + a.getYear().getYear()
                                    + " rhs=" + a.getYearValue());
                }
            } catch (Throwable ignored) {
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}
```
- *[user] message: identical to step [87] — not reprinted*

</details>
<details open><summary>▸ Output (~6,532 chars)</summary>

```
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-first-day-dk] semantic mismatch: expected="
                                + Calendar.MONDAY + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-copenhagen-dk-default] semantic mismatch: expected=34 actual=" + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-first-day-us] semantic mismatch: expected="
                                + Calendar.SUNDAY + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-detroit-default-copenhagen-zone] semantic mismatch: expected=35 actual=" + w.getWeek());
            }

            Week wWithLocale = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (wWithLocale.getWeek() != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-detroit-explicit-dk-locale] semantic mismatch: expected=34 actual=" + wWithLocale.getWeek());
            }

            /* Contract justification:
             * Week(Date, TimeZone) and Week(Date, TimeZone, Locale.getDefault()) are documented
             * for the same date/time and specified time zone; the 2-arg constructor is intended
             * to defer to the 3-arg constructor with Locale.getDefault(). Therefore all public
             * observable state derived from week/year must agree. A "fix" that merely suppresses
             * an exception or returns some arbitrary week would still violate this post-condition.
             */
            Week w2 = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            Week w3 = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"), Locale.getDefault());
            if (w2.getWeek() != w3.getWeek()
                    || w2.getYearValue() != w3.getYearValue()
                    || w2.getSerialIndex() != w3.getSerialIndex()) {
                throw new RuntimeException(
                        "[oracle:constructor-equivalence] metamorphic violation: Week(Date,TimeZone) must match Week(Date,TimeZone,Locale.getDefault())"
                                + " date=" + t.getTime()
                                + " zone=Europe/Copenhagen"
                                + " defaultLocale=" + Locale.getDefault()
                                + " lhsWeek=" + w2.getWeek()
                                + " rhsWeek=" + w3.getWeek()
                                + " lhsYear=" + w2.getYearValue()
                                + " rhsYear=" + w3.getYearValue()
                                + " lhsSerial=" + w2.getSerialIndex()
                                + " rhsSerial=" + w3.getSerialIndex());
            }

            if (data.remainingBytes() > 0) {
                Locale[] locales = new Locale[] {
                        Locale.US, new Locale("da", "DK"), Locale.UK, Locale.FRANCE
                };
                String[] zones = new String[] {
                        "Europe/Copenhagen", "US/Detroit", "UTC", "Asia/Tokyo"
                };

                Locale fuzzLocale = locales[data.consumeInt(0, locales.length - 1)];
                TimeZone fuzzZone = TimeZone.getTimeZone(zones[data.consumeInt(0, zones.length - 1)]);
                long fuzzMillis = data.consumeInt();

                Locale.setDefault(fuzzLocale);
                TimeZone.setDefault(fuzzZone);
                Date fuzzDate = new Date(fuzzMillis);

                /* Same documented constructor-delegation guarantee as above. */
                Week a = new Week(fuzzDate, fuzzZone);
                Week b = new Week(fuzzDate, fuzzZone, Locale.getDefault());
                if (a.getWeek() != b.getWeek() || a.getYearValue() != b.getYearValue()) {
                    throw new RuntimeException(
                            "[oracle:fuzz-constructor-equivalence] metamorphic violation: Week(Date,TimeZone) must match Week(Date,TimeZone,Locale.getDefault())"
                                    + " millis=" + fuzzMillis
                                    + " zone=" + fuzzZone.getID()
                                    + " locale=" + Locale.getDefault()
                                    + " lhsWeek=" + a.getWeek()
                                    + " rhsWeek=" + b.getWeek()
                                    + " lhsYear=" + a.getYearValue()
                                    + " rhsYear=" + b.getYearValue());
                }
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}
```

</details>

---
## [107] ⚙️ harness-attempt · `attempt_013`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.RuntimeException: [oracle:fuzz-constructor-equivalence] metamorphic violation: Week(Date,TimeZone) must match Week(Date,TimeZone,Locale.getDefault()) millis=-268487937 zone=Asia/Tokyo locale…

---
## [108] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [81] — not reprinted*
- *[user] message: identical to step [81] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~10,743 chars)</summary>

```
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        liftedWeekConstructorOracles();
        relationCtorZoneOverloadAgreesWithExplicitLocale(data);
        relationFirstMillisecondMatchesCalendarOverloadAfterPeg(data);
        hiddenStateReadOnlyGetterCheck(data);
    }

    private static void liftedWeekConstructorOracles() {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-first-day-da] semantic mismatch: expected="
                                + Calendar.MONDAY + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-da-week] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-first-day-us] semantic mismatch: expected="
                                + Calendar.SUNDAY + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-to-copenhagen-week] semantic mismatch: expected=35 actual="
                                + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-explicit-locale-week] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void relationCtorZoneOverloadAgreesWithExplicitLocale(FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        Date t;
        TimeZone sourceZone;
        TimeZone targetZone;
        Locale defLocale;

        try {
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds == null || zoneIds.length == 0) {
                return;
            }
            sourceZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            targetZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            defLocale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            Locale.setDefault(defLocale);
            TimeZone.setDefault(sourceZone);

            Calendar cal = Calendar.getInstance(sourceZone, defLocale);
            cal.clear();
            cal.set(data.consumeInt(1900, 9999),
                    data.consumeInt(0, 11),
                    data.consumeInt(1, 28),
                    data.consumeInt(0, 23),
                    data.consumeInt(0, 59),
                    data.consumeInt(0, 59));
            cal.set(Calendar.MILLISECOND, data.consumeInt(0, 999));
            t = cal.getTime();
        } catch (Throwable e) {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        }

        Week a;
        try {
            a = new Week(t, targetZone);
        } catch (Throwable e) {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        }

        Week b;
        try {
            b = new Week(t, targetZone, Locale.getDefault());
        } catch (Throwable e) {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        }

        try {
            // Contract: Week(Date, TimeZone) and Week(Date, TimeZone, Locale.getDefault()) are sibling
            // constructors for the same calculation, so week/year must agree. A patch that silently
            // ignores the provided zone breaks this without throwing.
            if (a.getWeek() != b.getWeek() || a.getYearValue() != b.getYearValue()) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:ctor-zone-overload] metamorphic violation: 2-arg and 3-arg constructors disagree inputDate="
                                + t.getTime()
                                + " sourceZone=" + sourceZone.getID()
                                + " targetZone=" + targetZone.getID()
                                + " defaultLocale=" + Locale.getDefault()
                                + " lhsWeek=" + a.getWeek()
                                + " lhsYear=" + a.getYearValue()
                                + " rhsWeek=" + b.getWeek()
                                + " rhsYear=" + b.getYearValue());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void relationFirstMillisecondMatchesCalendarOverloadAfterPeg(FuzzedDataProvider data) {
        Week w;
        Calendar cal;

        try {
            w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds == null || zoneIds.length == 0) {
                return;
            }
            TimeZone zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            cal = Calendar.getInstance(zone, locale);
            w.peg(cal);
        } catch (Throwable e) {
            return;
        }

        long noArg;
        try {
            noArg = w.getFirstMillisecond();
        } catch (Throwable e) {
            return;
        }

        long viaCalendar;
        try {
            viaCalendar = w.getFirstMillisecond(cal);
        } catch (Throwable e) {
            return;
        }

        // Contract: after peg(calendar), the parameterless getter is defined relative to that same
        // calendar, so it must equal the calendar overload. A silent wrong-value patch violates this.
        if (noArg != viaCalendar) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:first-ms-after-peg] metamorphic violation: getFirstMillisecond() != getFirstMillisecond(Calendar) noArg="
                            + noArg + " viaCalendar=" + viaCalendar
                            + " week=" + w.getWeek() + " year=" + w.getYearValue()
                            + " zone=" + cal.getTimeZone().getID() + " locale="
                            + cal.getLocale());
        }
    }

    private static void hiddenStateReadOnlyGetterCheck(FuzzedDataProvider data) {
        try {
            Week w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));

            int beforeWeek = w.getWeek();
            int beforeYear = w.getYearValue();
            long beforeFirst = w.getFirstMillisecond();
            long beforeLast = w.getLastMillisecond();
            long beforeSerial = w.getSerialIndex();
            int beforeHash = w.hashCode();
            String beforeString = w.toString();

            int observedWeek = w.getWeek();

            int afterWeek = w.getWeek();
            int afterYear = w.getYearValue();
            long afterFirst = w.getFirstMillisecond();
            long afterLast = w.getLastMillisecond();
            long afterSerial = w.getSerialIndex();
            int afterHash = w.hashCode();
            String afterString = w.toString();

            // Contract: getWeek() is a read-only query. Re-reading the object's public observable
            // state before and after the getter must show no mutation; a patch that "fixes" behaviour
            // by mutating cached state during a question method would violate this.
            if (beforeWeek != afterWeek
                    || beforeYear != afterYear
                    || beforeFirst != afterFirst
                    || beforeLast != afterLast
                    || beforeSerial != afterSerial
                    || beforeHash != afterHash
                    || (beforeString == null ? afterString != null : !beforeString.equals(afterString))) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:hidden-state-getWeek] semantic mismatch: getWeek() changed observable state observedWeek="
                                + observedWeek
                                + " beforeWeek=" + beforeWeek + " afterWeek=" + afterWeek
                                + " beforeYear=" + beforeYear + " afterYear=" + afterYear
                                + " beforeFirst=" + beforeFirst + " afterFirst=" + afterFirst
                                + " beforeLast=" + beforeLast + " afterLast=" + afterLast
                                + " beforeSerial=" + beforeSerial + " afterSerial=" + afterSerial
                                + " beforeHash=" + beforeHash + " afterHash=" + afterHash
                                + " beforeString=" + beforeString + " afterString=" + afterString);
            }
        } catch (Throwable e) {
            return;
        }
    }
}
```

</details>

---
## [109] ⚙️ harness-repair · `attempt_014`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [110] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [111] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~12,346 chars, 2 new)</summary>

- *[system] message: identical to step [81] — not reprinted*
- *[user] message: identical to step [81] — not reprinted*
**[assistant]**
```
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        liftedWeekConstructorOracles();
        relationCtorZoneOverloadAgreesWithExplicitLocale(data);
        relationFirstMillisecondMatchesCalendarOverloadAfterPeg(data);
        hiddenStateReadOnlyGetterCheck(data);
    }

    private static void liftedWeekConstructorOracles() {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-first-day-da] semantic mismatch: expected="
                                + Calendar.MONDAY + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-da-week] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-first-day-us] semantic mismatch: expected="
                                + Calendar.SUNDAY + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-to-copenhagen-week] semantic mismatch: expected=35 actual="
                                + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-explicit-locale-week] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void relationCtorZoneOverloadAgreesWithExplicitLocale(FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        Date t;
        TimeZone sourceZone;
        TimeZone targetZone;
        Locale defLocale;

        try {
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds == null || zoneIds.length == 0) {
                return;
            }
            sourceZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            targetZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            defLocale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            Locale.setDefault(defLocale);
            TimeZone.setDefault(sourceZone);

            Calendar cal = Calendar.getInstance(sourceZone, defLocale);
            cal.clear();
            cal.set(data.consumeInt(1900, 9999),
                    data.consumeInt(0, 11),
                    data.consumeInt(1, 28),
                    data.consumeInt(0, 23),
                    data.consumeInt(0, 59),
                    data.consumeInt(0, 59));
            cal.set(Calendar.MILLISECOND, data.consumeInt(0, 999));
            t = cal.getTime();
        } catch (Throwable e) {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        }

        Week a;
        try {
            a = new Week(t, targetZone);
        } catch (Throwable e) {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        }

        Week b;
        try {
            b = new Week(t, targetZone, Locale.getDefault());
        } catch (Throwable e) {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        }

        try {
            // Contract: Week(Date, TimeZone) and Week(Date, TimeZone, Locale.getDefault()) are sibling
            // constructors for the same calculation, so week/year must agree. A patch that silently
            // ignores the provided zone breaks this without throwing.
            if (a.getWeek() != b.getWeek() || a.getYearValue() != b.getYearValue()) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:ctor-zone-overload] metamorphic violation: 2-arg and 3-arg constructors disagree inputDate="
                                + t.getTime()
                                + " sourceZone=" + sourceZone.getID()
                                + " targetZone=" + targetZone.getID()
                                + " defaultLocale=" + Locale.getDefault()
                                + " lhsWeek=" + a.getWeek()
                                + " lhsYear=" + a.getYearValue()
                                + " rhsWeek=" + b.getWeek()
                                + " rhsYear=" + b.getYearValue());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void relationFirstMillisecondMatchesCalendarOverloadAfterPeg(FuzzedDataProvider data) {
        Week w;
        Calendar cal;

        try {
            w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds == null || zoneIds.length == 0) {
                return;
            }
            TimeZone zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            cal = Calendar.getInstance(zone, locale);
            w.peg(cal);
        } catch (Throwable e) {
            return;
        }

        long noArg;
        try {
            noArg = w.getFirstMillisecond();
        } catch (Throwable e) {
            return;
        }

        long viaCalendar;
        try {
            viaCalendar = w.getFirstMillisecond(cal);
        } catch (Throwable e) {
            return;
        }

        // Contract: after peg(calendar), the parameterless getter is defined relative to that same
        // calendar, so it must equal the calendar overload. A silent wrong-value patch violates this.
        if (noArg != viaCalendar) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:first-ms-after-peg] metamorphic violation: getFirstMillisecond() != getFirstMillisecond(Calendar) noArg="
                            + noArg + " viaCalendar=" + viaCalendar
                            + " week=" + w.getWeek() + " year=" + w.getYearValue()
                            + " zone=" + cal.getTimeZone().getID() + " locale="
                            + cal.getLocale());
        }
    }

    private static void hiddenStateReadOnlyGetterCheck(FuzzedDataProvider data) {
        try {
            Week w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));

            int beforeWeek = w.getWeek();
            int beforeYear = w.getYearValue();
            long beforeFirst = w.getFirstMillisecond();
            long beforeLast = w.getLastMillisecond();
            long beforeSerial = w.getSerialIndex();
            int beforeHash = w.hashCode();
            String beforeString = w.toString();

            int observedWeek = w.getWeek();

            int afterWeek = w.getWeek();
            int afterYear = w.getYearValue();
            long afterFirst = w.getFirstMillisecond();
            long afterLast = w.getLastMillisecond();
            long afterSerial = w.getSerialIndex();
            int afterHash = w.hashCode();
            String afterString = w.toString();

            // Contract: getWeek() is a read-only query. Re-reading the object's public observable
            // state before and after the getter must show no mutation; a patch that "fixes" behaviour
            // by mutating cached state during a question method would violate this.
            if (beforeWeek != afterWeek
                    || beforeYear != afterYear
                    || beforeFirst != afterFirst
                    || beforeLast != afterLast
                    || beforeSerial != afterSerial
                    || beforeHash != afterHash
                    || (beforeString == null ? afterString != null : !beforeString.equals(afterString))) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:hidden-state-getWeek] semantic mismatch: getWeek() changed observable state observedWeek="
                                + observedWeek
                                + " beforeWeek=" + beforeWeek + " afterWeek=" + afterWeek
                                + " beforeYear=" + beforeYear + " afterYear=" + afterYear
                                + " beforeFirst=" + beforeFirst + " afterFirst=" + afterFirst
                                + " beforeLast=" + beforeLast + " afterLast=" + afterLast
                                + " beforeSerial=" + beforeSerial + " afterSerial=" + afterSerial
                                + " beforeHash=" + beforeHash + " afterHash=" + afterHash
                                + " beforeString=" + beforeString + " afterString=" + afterString);
            }
        } catch (Throwable e) {
            return;
        }
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/attempt_014/FuzzHarness.java:193: error: cannot find symbol
                            + cal.getLocale());
                                 ^
  symbol:   method getLocale()
  location: variable cal of type Calendar
Note: /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/attempt_014/FuzzHarness.java uses or overrides a deprecated API.
Note: Recompile with -Xlint:deprecation for details.
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
<details open><summary>▸ Output (~9,968 chars)</summary>

```
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        liftedWeekConstructorOracles();
        relationCtorZoneOverloadAgreesWithExplicitLocale(data);
        relationFirstMillisecondMatchesCalendarOverloadAfterPeg(data);
        hiddenStateReadOnlyGetterCheck(data);
    }

    private static void liftedWeekConstructorOracles() {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-first-day-da] semantic mismatch: expected="
                                + Calendar.MONDAY + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-da-week] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-first-day-us] semantic mismatch: expected="
                                + Calendar.SUNDAY + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-to-copenhagen-week] semantic mismatch: expected=35 actual="
                                + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-explicit-locale-week] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void relationCtorZoneOverloadAgreesWithExplicitLocale(FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        Date t;
        TimeZone sourceZone;
        TimeZone targetZone;
        Locale defLocale;

        try {
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds == null || zoneIds.length == 0) {
                return;
            }
            sourceZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            targetZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            defLocale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            Locale.setDefault(defLocale);
            TimeZone.setDefault(sourceZone);

            Calendar cal = Calendar.getInstance(sourceZone, defLocale);
            cal.clear();
            cal.set(data.consumeInt(1900, 9999),
                    data.consumeInt(0, 11),
                    data.consumeInt(1, 28),
                    data.consumeInt(0, 23),
                    data.consumeInt(0, 59),
                    data.consumeInt(0, 59));
            cal.set(Calendar.MILLISECOND, data.consumeInt(0, 999));
            t = cal.getTime();
        } catch (Throwable e) {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        }

        Week a;
        try {
            a = new Week(t, targetZone);
        } catch (Throwable e) {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        }

        Week b;
        try {
            b = new Week(t, targetZone, Locale.getDefault());
        } catch (Throwable e) {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        }

        try {
            if (a.getWeek() != b.getWeek() || a.getYearValue() != b.getYearValue()) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:ctor-zone-overload] metamorphic violation: 2-arg and 3-arg constructors disagree inputDate="
                                + t.getTime()
                                + " sourceZone=" + sourceZone.getID()
                                + " targetZone=" + targetZone.getID()
                                + " defaultLocale=" + Locale.getDefault()
                                + " lhsWeek=" + a.getWeek()
                                + " lhsYear=" + a.getYearValue()
                                + " rhsWeek=" + b.getWeek()
                                + " rhsYear=" + b.getYearValue());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void relationFirstMillisecondMatchesCalendarOverloadAfterPeg(FuzzedDataProvider data) {
        Week w;
        Calendar cal;
        TimeZone zone;
        Locale locale;

        try {
            w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds == null || zoneIds.length == 0) {
                return;
            }
            zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            cal = Calendar.getInstance(zone, locale);
            w.peg(cal);
        } catch (Throwable e) {
            return;
        }

        long noArg;
        try {
            noArg = w.getFirstMillisecond();
        } catch (Throwable e) {
            return;
        }

        long viaCalendar;
        try {
            viaCalendar = w.getFirstMillisecond(cal);
        } catch (Throwable e) {
            return;
        }

        if (noArg != viaCalendar) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:first-ms-after-peg] metamorphic violation: getFirstMillisecond() != getFirstMillisecond(Calendar) noArg="
                            + noArg + " viaCalendar=" + viaCalendar
                            + " week=" + w.getWeek() + " year=" + w.getYearValue()
                            + " zone=" + zone.getID() + " locale=" + locale.toString());
        }
    }

    private static void hiddenStateReadOnlyGetterCheck(FuzzedDataProvider data) {
        try {
            Week w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));

            int beforeWeek = w.getWeek();
            int beforeYear = w.getYearValue();
            long beforeFirst = w.getFirstMillisecond();
            long beforeLast = w.getLastMillisecond();
            long beforeSerial = w.getSerialIndex();
            int beforeHash = w.hashCode();
            String beforeString = w.toString();

            int observedWeek = w.getWeek();

            int afterWeek = w.getWeek();
            int afterYear = w.getYearValue();
            long afterFirst = w.getFirstMillisecond();
            long afterLast = w.getLastMillisecond();
            long afterSerial = w.getSerialIndex();
            int afterHash = w.hashCode();
            String afterString = w.toString();

            if (beforeWeek != afterWeek
                    || beforeYear != afterYear
                    || beforeFirst != afterFirst
                    || beforeLast != afterLast
                    || beforeSerial != afterSerial
                    || beforeHash != afterHash
                    || (beforeString == null ? afterString != null : !beforeString.equals(afterString))) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:hidden-state-getWeek] semantic mismatch: getWeek() changed observable state observedWeek="
                                + observedWeek
                                + " beforeWeek=" + beforeWeek + " afterWeek=" + afterWeek
                                + " beforeYear=" + beforeYear + " afterYear=" + afterYear
                                + " beforeFirst=" + beforeFirst + " afterFirst=" + afterFirst
                                + " beforeLast=" + beforeLast + " afterLast=" + afterLast
                                + " beforeSerial=" + beforeSerial + " afterSerial=" + afterSerial
                                + " beforeHash=" + beforeHash + " afterHash=" + afterHash
                                + " beforeString=" + beforeString + " afterString=" + afterString);
            }
        } catch (Throwable e) {
            return;
        }
    }
}
```

</details>

---
## [112] ⚙️ harness-repair · `attempt_015`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [113] ⚙️ harness-attempt · `attempt_015`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:ctor-zone-overload] metamorphic violation: 2-arg and 3-arg constructors disagree inputDate=-2208992400000 sourceZone=Africa/Tunis targe…

---
## [114] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [81] — not reprinted*
- *[user] message: identical to step [83] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,806 chars)</summary>

```
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            int firstDay = cal.getFirstDayOfWeek();
            if (firstDay != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-da-setup] semantic mismatch: expected firstDayOfWeek="
                                + Calendar.MONDAY + " actual=" + firstDay);
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            int weekValue = w.getWeek();
            if (weekValue != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-da-week] semantic mismatch: expected week=34 actual="
                                + weekValue);
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            firstDay = cal.getFirstDayOfWeek();
            if (firstDay != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-setup] semantic mismatch: expected firstDayOfWeek="
                                + Calendar.SUNDAY + " actual=" + firstDay);
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);

            t = cal.getTime();
            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            weekValue = w.getWeek();
            if (weekValue != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-week] semantic mismatch: expected week=35 actual="
                                + weekValue);
            }

            Week wWithLocale = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            int weekWithLocale = wWithLocale.getWeek();
            if (weekWithLocale != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-week-explicit-locale] semantic mismatch: expected week=34 actual="
                                + weekWithLocale);
            }

            try {
                Week lhs = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
                Week rhs = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"), Locale.getDefault());
                if (!lhs.equals(rhs) || lhs.getWeek() != rhs.getWeek()
                        || lhs.getYearValue() != rhs.getYearValue()
                        || lhs.getFirstMillisecond() != rhs.getFirstMillisecond()
                        || lhs.getLastMillisecond() != rhs.getLastMillisecond()
                        || lhs.getSerialIndex() != rhs.getSerialIndex()) {
                    throw new RuntimeException(
                            "[oracle:deprecated-ctor-equivalence] metamorphic violation: Week(Date, TimeZone) must agree with Week(Date, TimeZone, Locale.getDefault()) for the same input because the deprecated overload's contract says it calculates the week relative to the specified time zone and the 3-arg constructor is its documented replacement inputMillis="
                                    + t.getTime()
                                    + " zone=Europe/Copenhagen defaultLocale=" + Locale.getDefault()
                                    + " lhsWeek=" + lhs.getWeek()
                                    + " rhsWeek=" + rhs.getWeek()
                                    + " lhsYear=" + lhs.getYearValue()
                                    + " rhsYear=" + rhs.getYearValue()
                                    + " lhsFirstMs=" + lhs.getFirstMillisecond()
                                    + " rhsFirstMs=" + rhs.getFirstMillisecond()
                                    + " lhsLastMs=" + lhs.getLastMillisecond()
                                    + " rhsLastMs=" + rhs.getLastMillisecond()
                                    + " lhsSerial=" + lhs.getSerialIndex()
                                    + " rhsSerial=" + rhs.getSerialIndex());
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable ignored) {
                return;
            }

            try {
                String[] ids = TimeZone.getAvailableIDs();
                TimeZone fuzzZone = ids.length == 0
                        ? TimeZone.getTimeZone("UTC")
                        : TimeZone.getTimeZone(ids[data.consumeInt(0, ids.length - 1)]);
                int year = data.consumeInt(1900, 9999);
                int month = data.consumeInt(0, 11);
                int day = data.consumeInt(1, 28);
                int hour = data.consumeInt(0, 23);
                int minute = data.consumeInt(0, 59);
                int second = data.consumeInt(0, 59);

                GregorianCalendar gc = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.US);
                gc.clear();
                gc.set(year, month, day, hour, minute, second);
                gc.set(Calendar.MILLISECOND, 0);
                Date fuzzDate = gc.getTime();

                Week a = new Week(fuzzDate, fuzzZone);
                Week b = new Week(fuzzDate, fuzzZone, Locale.getDefault());

                if (!a.equals(b) || a.getWeek() != b.getWeek()
                        || a.getYearValue() != b.getYearValue()
                        || a.hashCode() != b.hashCode()
                        || a.getSerialIndex() != b.getSerialIndex()) {
                    throw new RuntimeException(
                            "[oracle:fuzzed-ctor-equivalence] metamorphic violation: equivalent constructor overloads disagreed inputMillis="
                                    + fuzzDate.getTime()
                                    + " zone=" + fuzzZone.getID()
                                    + " defaultLocale=" + Locale.getDefault()
                                    + " lhsWeek=" + a.getWeek()
                                    + " rhsWeek=" + b.getWeek()
                                    + " lhsYear=" + a.getYearValue()
                                    + " rhsYear=" + b.getYearValue()
                                    + " lhsHash=" + a.hashCode()
                                    + " rhsHash=" + b.hashCode()
                                    + " lhsSerial=" + a.getSerialIndex()
                                    + " rhsSerial=" + b.getSerialIndex());
                }

                Calendar cmpCal = Calendar.getInstance(fuzzZone, Locale.getDefault());
                long fm0 = a.getFirstMillisecond();
                long fm1 = a.getFirstMillisecond(cmpCal);
                long lm0 = a.getLastMillisecond();
                long lm1 = a.getLastMillisecond(cmpCal);
                if (fm0 != fm1 || lm0 != lm1) {
                    throw new RuntimeException(
                            "[oracle:millisecond-overload-agreement] metamorphic violation: no-arg and Calendar overloads must agree when evaluated with a Calendar using the same time zone/locale embodied by the constructed period zone="
                                    + fuzzZone.getID()
                                    + " defaultLocale=" + Locale.getDefault()
                                    + " inputMillis=" + fuzzDate.getTime()
                                    + " firstNoArg=" + fm0
                                    + " firstWithCal=" + fm1
                                    + " lastNoArg=" + lm0
                                    + " lastWithCal=" + lm1);
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable ignored) {
                return;
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}
```

</details>

---
## [115] ⚙️ harness-attempt · `attempt_016`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.RuntimeException: [oracle:millisecond-overload-agreement] metamorphic violation: no-arg and Calendar overloads must agree when evaluated with a Calendar using the same time zone/locale embod…

---
## [116] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [81] — not reprinted*
- *[user] message: identical to step [81] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~11,901 chars)</summary>

```
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedWeekTests();
        runConstructorSiblingAgreement(data);
        runPegAndGetterAgreement(data);
        runReadOnlyHiddenStateCheck(data);
    }

    private static void runLiftedWeekTests() {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            int firstDay = cal.getFirstDayOfWeek();
            if (firstDay != Calendar.MONDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-da-firstday] semantic mismatch: expected=" + Calendar.MONDAY
                                + " actual=" + firstDay);
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            int week = w.getWeek();
            if (week != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-da-week] semantic mismatch: expected=34 actual=" + week);
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            firstDay = cal.getFirstDayOfWeek();
            if (firstDay != Calendar.SUNDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-firstday] semantic mismatch: expected=" + Calendar.SUNDAY
                                + " actual=" + firstDay);
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);

            t = cal.getTime();
            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            week = w.getWeek();
            if (week != 35) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-week-twoarg] semantic mismatch: expected=35 actual=" + week);
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"), new Locale("da", "DK"));
            week = w.getWeek();
            if (week != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-week-threearg] semantic mismatch: expected=34 actual=" + week);
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void runConstructorSiblingAgreement(FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        Date t;
        TimeZone sourceZone;
        TimeZone targetZone;
        Locale defLocale;
        try {
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds.length == 0) {
                return;
            }
            sourceZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            targetZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            defLocale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            Locale.setDefault(defLocale);
            TimeZone.setDefault(sourceZone);
            Calendar cal = Calendar.getInstance(sourceZone, defLocale);
            cal.clear();
            cal.set(
                    data.consumeInt(1900, 9999),
                    data.consumeInt(0, 11),
                    data.consumeInt(1, 28),
                    data.consumeInt(0, 23),
                    data.consumeInt(0, 59),
                    data.consumeInt(0, 59));
            cal.set(Calendar.MILLISECOND, data.consumeInt(0, 999));
            t = cal.getTime();
        } catch (Exception e) {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        }

        Week a;
        try {
            a = new Week(t, targetZone);
        } catch (Exception e) {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        }

        Week b;
        try {
            b = new Week(t, targetZone, Locale.getDefault());
        } catch (Exception e) {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        }

        try {
            if (a.getWeek() != b.getWeek() || a.getYearValue() != b.getYearValue()) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:ctor-zone-overload] metamorphic violation: Week(Date,TimeZone) must agree with "
                                + "Week(Date,TimeZone,Locale.getDefault()) for week/year on the same instant; "
                                + "twoArgWeek=" + a.getWeek() + " twoArgYear=" + a.getYearValue()
                                + " threeArgWeek=" + b.getWeek() + " threeArgYear=" + b.getYearValue());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void runPegAndGetterAgreement(FuzzedDataProvider data) {
        Week w;
        Calendar cal;
        try {
            w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds.length == 0) {
                return;
            }
            TimeZone zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            cal = Calendar.getInstance(zone, locale);
            w.peg(cal);
        } catch (Exception e) {
            return;
        }

        long a;
        try {
            a = w.getFirstMillisecond();
        } catch (Exception e) {
            return;
        }

        long b;
        try {
            b = w.getFirstMillisecond(cal);
        } catch (Exception e) {
            return;
        }

        if (a != b) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:first-ms-after-peg] metamorphic violation: after peg(calendar), "
                            + "getFirstMillisecond() must equal getFirstMillisecond(calendar); noArg=" + a
                            + " calendarOverload=" + b);
        }
    }

    private static void runReadOnlyHiddenStateCheck(FuzzedDataProvider data) {
        try {
            Week w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
            long firstBefore = w.getFirstMillisecond();
            long lastBefore = w.getLastMillisecond();
            long serialBefore = w.getSerialIndex();
            int weekBefore = w.getWeek();
            int yearValueBefore = w.getYearValue();
            Year yearObjBefore = w.getYear();
            int hashBefore = w.hashCode();
            RegularTimePeriod prevBefore = w.previous();
            RegularTimePeriod nextBefore = w.next();
            String stringBefore = w.toString();

            w.getWeek();
            w.getYear();
            w.getYearValue();
            w.getSerialIndex();
            w.getFirstMillisecond();
            w.getLastMillisecond();
            w.hashCode();
            w.previous();
            w.next();
            w.toString();

            long firstAfter = w.getFirstMillisecond();
            long lastAfter = w.getLastMillisecond();
            long serialAfter = w.getSerialIndex();
            int weekAfter = w.getWeek();
            int yearValueAfter = w.getYearValue();
            Year yearObjAfter = w.getYear();
            int hashAfter = w.hashCode();
            RegularTimePeriod prevAfter = w.previous();
            RegularTimePeriod nextAfter = w.next();
            String stringAfter = w.toString();

            /* Contract justification: these are reader/query methods (get*/hashCode/toString/next/previous)
               and repeated reads on an unchanged Week should not silently mutate its observable state.
               A throw-deleting or bookkeeping-skipping patch that corrupts cached state would violate this. */
            if (firstBefore != firstAfter
                    || lastBefore != lastAfter
                    || serialBefore != serialAfter
                    || weekBefore != weekAfter
                    || yearValueBefore != yearValueAfter
                    || !sameYear(yearObjBefore, yearObjAfter)
                    || hashBefore != hashAfter
                    || !samePeriod(prevBefore, prevAfter)
                    || !samePeriod(nextBefore, nextAfter)
                    || !sameString(stringBefore, stringAfter)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:hidden-state-readers] metamorphic violation: repeated read-only queries changed observable state; "
                                + "firstBefore=" + firstBefore + " firstAfter=" + firstAfter
                                + " lastBefore=" + lastBefore + " lastAfter=" + lastAfter
                                + " serialBefore=" + serialBefore + " serialAfter=" + serialAfter
                                + " weekBefore=" + weekBefore + " weekAfter=" + weekAfter
                                + " yearValueBefore=" + yearValueBefore + " yearValueAfter=" + yearValueAfter
                                + " yearBefore=" + String.valueOf(yearObjBefore)
                                + " yearAfter=" + String.valueOf(yearObjAfter)
                                + " hashBefore=" + hashBefore + " hashAfter=" + hashAfter
                                + " prevBefore=" + String.valueOf(prevBefore)
                                + " prevAfter=" + String.valueOf(prevAfter)
                                + " nextBefore=" + String.valueOf(nextBefore)
                                + " nextAfter=" + String.valueOf(nextAfter)
                                + " toStringBefore=" + escapeOneLine(stringBefore)
                                + " toStringAfter=" + escapeOneLine(stringAfter));
            }
        } catch (Exception e) {
            return;
        }
    }

    private static boolean sameYear(Year a, Year b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.getYear() == b.getYear();
    }

    private static boolean samePeriod(RegularTimePeriod a, RegularTimePeriod b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b);
    }

    private static boolean sameString(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String escapeOneLine(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}
```

</details>

---
## [117] ⚙️ harness-repair · `attempt_017`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [118] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That did not compile. javac reported:

---
## [119] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~16,892 chars, 2 new)</summary>

- *[system] message: identical to step [81] — not reprinted*
- *[user] message: identical to step [81] — not reprinted*
**[assistant]**
```
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedWeekTests();
        runConstructorSiblingAgreement(data);
        runPegAndGetterAgreement(data);
        runReadOnlyHiddenStateCheck(data);
    }

    private static void runLiftedWeekTests() {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            int firstDay = cal.getFirstDayOfWeek();
            if (firstDay != Calendar.MONDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-da-firstday] semantic mismatch: expected=" + Calendar.MONDAY
                                + " actual=" + firstDay);
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            int week = w.getWeek();
            if (week != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-da-week] semantic mismatch: expected=34 actual=" + week);
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            firstDay = cal.getFirstDayOfWeek();
            if (firstDay != Calendar.SUNDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-firstday] semantic mismatch: expected=" + Calendar.SUNDAY
                                + " actual=" + firstDay);
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);

            t = cal.getTime();
            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            week = w.getWeek();
            if (week != 35) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-week-twoarg] semantic mismatch: expected=35 actual=" + week);
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"), new Locale("da", "DK"));
            week = w.getWeek();
            if (week != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-week-threearg] semantic mismatch: expected=34 actual=" + week);
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void runConstructorSiblingAgreement(FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        Date t;
        TimeZone sourceZone;
        TimeZone targetZone;
        Locale defLocale;
        try {
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds.length == 0) {
                return;
            }
            sourceZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            targetZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            defLocale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            Locale.setDefault(defLocale);
            TimeZone.setDefault(sourceZone);
            Calendar cal = Calendar.getInstance(sourceZone, defLocale);
            cal.clear();
            cal.set(
                    data.consumeInt(1900, 9999),
                    data.consumeInt(0, 11),
                    data.consumeInt(1, 28),
                    data.consumeInt(0, 23),
                    data.consumeInt(0, 59),
                    data.consumeInt(0, 59));
            cal.set(Calendar.MILLISECOND, data.consumeInt(0, 999));
            t = cal.getTime();
        } catch (Exception e) {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        }

        Week a;
        try {
            a = new Week(t, targetZone);
        } catch (Exception e) {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        }

        Week b;
        try {
            b = new Week(t, targetZone, Locale.getDefault());
        } catch (Exception e) {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        }

        try {
            if (a.getWeek() != b.getWeek() || a.getYearValue() != b.getYearValue()) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:ctor-zone-overload] metamorphic violation: Week(Date,TimeZone) must agree with "
                                + "Week(Date,TimeZone,Locale.getDefault()) for week/year on the same instant; "
                                + "twoArgWeek=" + a.getWeek() + " twoArgYear=" + a.getYearValue()
                                + " threeArgWeek=" + b.getWeek() + " threeArgYear=" + b.getYearValue());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void runPegAndGetterAgreement(FuzzedDataProvider data) {
        Week w;
        Calendar cal;
        try {
            w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds.length == 0) {
                return;
            }
            TimeZone zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            cal = Calendar.getInstance(zone, locale);
            w.peg(cal);
        } catch (Exception e) {
            return;
        }

        long a;
        try {
            a = w.getFirstMillisecond();
        } catch (Exception e) {
            return;
        }

        long b;
        try {
            b = w.getFirstMillisecond(cal);
        } catch (Exception e) {
            return;
        }

        if (a != b) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:first-ms-after-peg] metamorphic violation: after peg(calendar), "
                            + "getFirstMillisecond() must equal getFirstMillisecond(calendar); noArg=" + a
                            + " calendarOverload=" + b);
        }
    }

    private static void runReadOnlyHiddenStateCheck(FuzzedDataProvider data) {
        try {
            Week w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
            long firstBefore = w.getFirstMillisecond();
            long lastBefore = w.getLastMillisecond();
            long serialBefore = w.getSerialIndex();
            int weekBefore = w.getWeek();
            int yearValueBefore = w.getYearValue();
            Year yearObjBefore = w.getYear();
            int hashBefore = w.hashCode();
            RegularTimePeriod prevBefore = w.previous();
            RegularTimePeriod nextBefore = w.next();
            String stringBefore = w.toString();

            w.getWeek();
            w.getYear();
            w.getYearValue();
            w.getSerialIndex();
            w.getFirstMillisecond();
            w.getLastMillisecond();
            w.hashCode();
            w.previous();
            w.next();
            w.toString();

            long firstAfter = w.getFirstMillisecond();
            long lastAfter = w.getLastMillisecond();
            long serialAfter = w.getSerialIndex();
            int weekAfter = w.getWeek();
            int yearValueAfter = w.getYearValue();
            Year yearObjAfter = w.getYear();
            int hashAfter = w.hashCode();
            RegularTimePeriod prevAfter = w.previous();
            RegularTimePeriod nextAfter = w.next();
            String stringAfter = w.toString();

            /* Contract justification: these are reader/query methods (get*/hashCode/toString/next/previous)
               and repeated reads on an unchanged Week should not silently mutate its observable state.
               A throw-deleting or bookkeeping-skipping patch that corrupts cached state would violate this. */
            if (firstBefore != firstAfter
                    || lastBefore != lastAfter
                    || serialBefore != serialAfter
                    || weekBefore != weekAfter
                    || yearValueBefore != yearValueAfter
                    || !sameYear(yearObjBefore, yearObjAfter)
                    || hashBefore != hashAfter
                    || !samePeriod(prevBefore, prevAfter)
                    || !samePeriod(nextBefore, nextAfter)
                    || !sameString(stringBefore, stringAfter)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:hidden-state-readers] metamorphic violation: repeated read-only queries changed observable state; "
                                + "firstBefore=" + firstBefore + " firstAfter=" + firstAfter
                                + " lastBefore=" + lastBefore + " lastAfter=" + lastAfter
                                + " serialBefore=" + serialBefore + " serialAfter=" + serialAfter
                                + " weekBefore=" + weekBefore + " weekAfter=" + weekAfter
                                + " yearValueBefore=" + yearValueBefore + " yearValueAfter=" + yearValueAfter
                                + " yearBefore=" + String.valueOf(yearObjBefore)
                                + " yearAfter=" + String.valueOf(yearObjAfter)
                                + " hashBefore=" + hashBefore + " hashAfter=" + hashAfter
                                + " prevBefore=" + String.valueOf(prevBefore)
                                + " prevAfter=" + String.valueOf(prevAfter)
                                + " nextBefore=" + String.valueOf(nextBefore)
                                + " nextAfter=" + String.valueOf(nextAfter)
                                + " toStringBefore=" + escapeOneLine(stringBefore)
                                + " toStringAfter=" + escapeOneLine(stringAfter));
            }
        } catch (Exception e) {
            return;
        }
    }

    private static boolean sameYear(Year a, Year b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.getYear() == b.getYear();
    }

    private static boolean samePeriod(RegularTimePeriod a, RegularTimePeriod b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b);
    }

    private static boolean sameString(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String escapeOneLine(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}
```
**[user]**
```
That did not compile. javac reported:
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/attempt_017/FuzzHarness.java:219: error: not a statement
            /* Contract justification: these are reader/query methods (get*/hashCode/toString/next/previous)
                                                                                                  ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/attempt_017/FuzzHarness.java:219: error: ';' expected
            /* Contract justification: these are reader/query methods (get*/hashCode/toString/next/previous)
                                                                                                           ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/attempt_017/FuzzHarness.java:220: error: ';' expected
               and repeated reads on an unchanged Week should not silently mutate its observable state.
                           ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/attempt_017/FuzzHarness.java:220: error: ';' expected
               and repeated reads on an unchanged Week should not silently mutate its observable state.
                                    ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/attempt_017/FuzzHarness.java:220: error: ';' expected
               and repeated reads on an unchanged Week should not silently mutate its observable state.
                                                 ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/attempt_017/FuzzHarness.java:220: error: ';' expected
               and repeated reads on an unchanged Week should not silently mutate its observable state.
                                                             ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/attempt_017/FuzzHarness.java:220: error: ';' expected
               and repeated reads on an unchanged Week should not silently mutate its observable state.
                                                                          ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/attempt_017/FuzzHarness.java:220: error: ';' expected
               and repeated reads on an unchanged Week should not silently mutate its observable state.
                                                                                     ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/attempt_017/FuzzHarness.java:220: error: ';' expected
               and repeated reads on an unchanged Week should not silently mutate its observable state.
                                                                                                      ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/attempt_017/FuzzHarness.java:221: error: not a statement
               A throw-deleting or bookkeeping-skipping patch that corrupts cached state would violate this. */
               ^
/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/checkouts/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/Chart_8_buggy/fuzz/attempt_017/FuzzHarness.java:221: error: ';' expected
               A throw-deleting or bookkeeping-skipping patch that corrupts cached state would violate this. */
                
... (truncated)

Return the full corrected FuzzHarness.java. Rules:
- Raw Java source only. No markdown fences. No prose. No explanations. The file starts with a comment or package statement and ends with a closing brace.
- Public class named exactly `FuzzHarness`.
- Entrypoint exactly:
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)
- Use only the FuzzedDataProvider methods listed in the original instructions. Do NOT invent methods like getInt(), consumeDouble(), getRemainingSize(), or consumeIntInRange() — they do not exist.
- Do NOT use classes or methods that are not on the project classpath. If javac says 'cannot find symbol', remove that import and use only classes visible in the source_imports block or java.* / java.awt.*.
```

</details>
<details open><summary>▸ Output (~11,867 chars)</summary>

```
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedWeekTests();
        runConstructorSiblingAgreement(data);
        runPegAndGetterAgreement(data);
        runReadOnlyHiddenStateCheck(data);
    }

    private static void runLiftedWeekTests() {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            int firstDay = cal.getFirstDayOfWeek();
            if (firstDay != Calendar.MONDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-da-firstday] semantic mismatch: expected=" + Calendar.MONDAY
                                + " actual=" + firstDay);
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            int week = w.getWeek();
            if (week != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-da-week] semantic mismatch: expected=34 actual=" + week);
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            firstDay = cal.getFirstDayOfWeek();
            if (firstDay != Calendar.SUNDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-firstday] semantic mismatch: expected=" + Calendar.SUNDAY
                                + " actual=" + firstDay);
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);

            t = cal.getTime();
            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            week = w.getWeek();
            if (week != 35) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-week-twoarg] semantic mismatch: expected=35 actual=" + week);
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"), new Locale("da", "DK"));
            week = w.getWeek();
            if (week != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-week-threearg] semantic mismatch: expected=34 actual=" + week);
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void runConstructorSiblingAgreement(FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        Date t;
        TimeZone sourceZone;
        TimeZone targetZone;
        Locale defLocale;
        try {
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds.length == 0) {
                return;
            }
            sourceZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            targetZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            defLocale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            Locale.setDefault(defLocale);
            TimeZone.setDefault(sourceZone);
            Calendar cal = Calendar.getInstance(sourceZone, defLocale);
            cal.clear();
            cal.set(
                    data.consumeInt(1900, 9999),
                    data.consumeInt(0, 11),
                    data.consumeInt(1, 28),
                    data.consumeInt(0, 23),
                    data.consumeInt(0, 59),
                    data.consumeInt(0, 59));
            cal.set(Calendar.MILLISECOND, data.consumeInt(0, 999));
            t = cal.getTime();
        } catch (Exception e) {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        }

        Week a;
        try {
            a = new Week(t, targetZone);
        } catch (Exception e) {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        }

        Week b;
        try {
            b = new Week(t, targetZone, Locale.getDefault());
        } catch (Exception e) {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        }

        try {
            if (a.getWeek() != b.getWeek() || a.getYearValue() != b.getYearValue()) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:ctor-zone-overload] metamorphic violation: Week(Date,TimeZone) must agree with "
                                + "Week(Date,TimeZone,Locale.getDefault()) for week/year on the same instant; "
                                + "twoArgWeek=" + a.getWeek() + " twoArgYear=" + a.getYearValue()
                                + " threeArgWeek=" + b.getWeek() + " threeArgYear=" + b.getYearValue());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void runPegAndGetterAgreement(FuzzedDataProvider data) {
        Week w;
        Calendar cal;
        try {
            w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds.length == 0) {
                return;
            }
            TimeZone zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            cal = Calendar.getInstance(zone, locale);
            w.peg(cal);
        } catch (Exception e) {
            return;
        }

        long a;
        try {
            a = w.getFirstMillisecond();
        } catch (Exception e) {
            return;
        }

        long b;
        try {
            b = w.getFirstMillisecond(cal);
        } catch (Exception e) {
            return;
        }

        if (a != b) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:first-ms-after-peg] metamorphic violation: after peg(calendar), "
                            + "getFirstMillisecond() must equal getFirstMillisecond(calendar); noArg=" + a
                            + " calendarOverload=" + b);
        }
    }

    private static void runReadOnlyHiddenStateCheck(FuzzedDataProvider data) {
        try {
            Week w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
            long firstBefore = w.getFirstMillisecond();
            long lastBefore = w.getLastMillisecond();
            long serialBefore = w.getSerialIndex();
            int weekBefore = w.getWeek();
            int yearValueBefore = w.getYearValue();
            Year yearObjBefore = w.getYear();
            int hashBefore = w.hashCode();
            RegularTimePeriod prevBefore = w.previous();
            RegularTimePeriod nextBefore = w.next();
            String stringBefore = w.toString();

            w.getWeek();
            w.getYear();
            w.getYearValue();
            w.getSerialIndex();
            w.getFirstMillisecond();
            w.getLastMillisecond();
            w.hashCode();
            w.previous();
            w.next();
            w.toString();

            long firstAfter = w.getFirstMillisecond();
            long lastAfter = w.getLastMillisecond();
            long serialAfter = w.getSerialIndex();
            int weekAfter = w.getWeek();
            int yearValueAfter = w.getYearValue();
            Year yearObjAfter = w.getYear();
            int hashAfter = w.hashCode();
            RegularTimePeriod prevAfter = w.previous();
            RegularTimePeriod nextAfter = w.next();
            String stringAfter = w.toString();

            // Contract justification: these are reader/query methods and repeated reads on an unchanged
            // Week should not silently mutate its observable state. A bookkeeping-skipping patch that
            // corrupts cached state would violate this observable post-condition.
            if (firstBefore != firstAfter
                    || lastBefore != lastAfter
                    || serialBefore != serialAfter
                    || weekBefore != weekAfter
                    || yearValueBefore != yearValueAfter
                    || !sameYear(yearObjBefore, yearObjAfter)
                    || hashBefore != hashAfter
                    || !samePeriod(prevBefore, prevAfter)
                    || !samePeriod(nextBefore, nextAfter)
                    || !sameString(stringBefore, stringAfter)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:hidden-state-readers] metamorphic violation: repeated read-only queries changed observable state; "
                                + "firstBefore=" + firstBefore + " firstAfter=" + firstAfter
                                + " lastBefore=" + lastBefore + " lastAfter=" + lastAfter
                                + " serialBefore=" + serialBefore + " serialAfter=" + serialAfter
                                + " weekBefore=" + weekBefore + " weekAfter=" + weekAfter
                                + " yearValueBefore=" + yearValueBefore + " yearValueAfter=" + yearValueAfter
                                + " yearBefore=" + String.valueOf(yearObjBefore)
                                + " yearAfter=" + String.valueOf(yearObjAfter)
                                + " hashBefore=" + hashBefore + " hashAfter=" + hashAfter
                                + " prevBefore=" + String.valueOf(prevBefore)
                                + " prevAfter=" + String.valueOf(prevAfter)
                                + " nextBefore=" + String.valueOf(nextBefore)
                                + " nextAfter=" + String.valueOf(nextAfter)
                                + " toStringBefore=" + escapeOneLine(stringBefore)
                                + " toStringAfter=" + escapeOneLine(stringAfter));
            }
        } catch (Exception e) {
            return;
        }
    }

    private static boolean sameYear(Year a, Year b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.getYear() == b.getYear();
    }

    private static boolean samePeriod(RegularTimePeriod a, RegularTimePeriod b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b);
    }

    private static boolean sameString(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String escapeOneLine(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}
```

</details>

---
## [120] ⚙️ harness-repair · `attempt_018`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [121] ⚙️ harness-attempt · `attempt_018`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:ctor-zone-overload] metamorphic violation: Week(Date,TimeZone) must agree with Week(Date,TimeZone,Locale.getDefault()) for week/year on…

---
## [122] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [81] — not reprinted*
- *[user] message: identical to step [83] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,543 chars)</summary>

```
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            runLiftedConstructorTest();
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }

        try {
            runMetamorphicChecks(data);
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void runLiftedConstructorTest() {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            int firstDay = cal.getFirstDayOfWeek();
            if (firstDay != Calendar.MONDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-da-firstDay] semantic mismatch: expected="
                                + Calendar.MONDAY + " actual=" + firstDay);
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            int week = w.getWeek();
            if (week != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-da-week] semantic mismatch: expected=34 actual=" + week);
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            firstDay = cal.getFirstDayOfWeek();
            if (firstDay != Calendar.SUNDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-firstDay] semantic mismatch: expected="
                                + Calendar.SUNDAY + " actual=" + firstDay);
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);

            t = cal.getTime();
            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            week = w.getWeek();
            if (week != 35) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-week] semantic mismatch: expected=35 actual=" + week);
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            week = w.getWeek();
            if (week != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-explicitLocale-week] semantic mismatch: expected=34 actual=" + week);
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void runMetamorphicChecks(FuzzedDataProvider data) {
        String[] zoneIds = new String[] {
                "Europe/Copenhagen",
                "US/Detroit",
                "UTC",
                "Asia/Tokyo",
                "Europe/London"
        };
        Locale[] locales = new Locale[] {
                new Locale("da", "DK"),
                Locale.US,
                Locale.UK,
                Locale.JAPAN
        };

        TimeZone zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
        Locale locale = locales[data.consumeInt(0, locales.length - 1)];
        long millis = data.consumeInt(-1_000_000, 1_000_000) * 1000L;
        Date time = new Date(millis);

        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(locale);
            TimeZone.setDefault(TimeZone.getTimeZone(zone.getID()));

            Week implicit;
            Week explicit;
            try {
                implicit = new Week(time, zone);
                explicit = new Week(time, zone, Locale.getDefault());
            } catch (Throwable ignored) {
                return;
            }

            /* Contract basis:
             * Week(Date, TimeZone) is documented as "calculated relative to the specified time zone",
             * and the 3-arg overload adds the locale parameter. When the explicit locale passed to the
             * 3-arg overload is exactly Locale.getDefault(), both constructors describe the same setup.
             * A patch that ignores the supplied/default locale or silently substitutes another one breaks
             * this observable equivalence even though no exception is thrown.
             */
            if (implicit.getWeek() != explicit.getWeek()
                    || implicit.getYearValue() != explicit.getYearValue()) {
                throw new RuntimeException(
                        "[oracle:default-locale-equivalence] metamorphic violation: "
                                + "Week(Date,TimeZone) must agree with Week(Date,TimeZone,Locale.getDefault()) "
                                + "inputMillis=" + millis
                                + " zone=" + zone.getID()
                                + " defaultLocale=" + Locale.getDefault()
                                + " lhsWeek=" + implicit.getWeek()
                                + " rhsWeek=" + explicit.getWeek()
                                + " lhsYear=" + implicit.getYearValue()
                                + " rhsYear=" + explicit.getYearValue());
            }

            Calendar cal = Calendar.getInstance(zone, locale);
            try {
                cal.setTime(time);
                long lhsFirst = explicit.getFirstMillisecond();
                long rhsFirst = explicit.getFirstMillisecond(cal);
                long lhsLast = explicit.getLastMillisecond();
                long rhsLast = explicit.getLastMillisecond(cal);

                /* Contract basis:
                 * getFirstMillisecond()/getLastMillisecond() return values determined relative to the
                 * time zone specified in the constructor, or the most recent peg(Calendar). Immediately
                 * after construction with the same zone/locale-backed calendar, the no-arg getters must
                 * agree with the Calendar-taking siblings on that equivalent calendar.
                 */
                if (lhsFirst != rhsFirst) {
                    throw new RuntimeException(
                            "[oracle:first-millisecond-overload] metamorphic violation: "
                                    + "getFirstMillisecond() must equal getFirstMillisecond(calendar) for equivalent calendar "
                                    + "inputMillis=" + millis
                                    + " zone=" + zone.getID()
                                    + " locale=" + locale
                                    + " lhs=" + lhsFirst
                                    + " rhs=" + rhsFirst);
                }
                if (lhsLast != rhsLast) {
                    throw new RuntimeException(
                            "[oracle:last-millisecond-overload] metamorphic violation: "
                                    + "getLastMillisecond() must equal getLastMillisecond(calendar) for equivalent calendar "
                                    + "inputMillis=" + millis
                                    + " zone=" + zone.getID()
                                    + " locale=" + locale
                                    + " lhs=" + lhsLast
                                    + " rhs=" + rhsLast);
                }
            } catch (Throwable ignored) {
                return;
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}
```

</details>

---
## [123] ⚙️ harness-attempt · `attempt_019`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.RuntimeException: [oracle:default-locale-equivalence] metamorphic violation: Week(Date,TimeZone) must agree with Week(Date,TimeZone,Locale.getDefault()) inputMillis=-970747000 zone=Asia/Toky…

---
## [124] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [81] — not reprinted*
- *[user] message: identical to step [81] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~14,141 chars)</summary>

```
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-monday] semantic mismatch: expected firstDayOfWeek=2 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-da-copenhagen] semantic mismatch: expected week=34 actual="
                                + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-sunday] semantic mismatch: expected firstDayOfWeek=1 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-detroit-to-copenhagen] semantic mismatch: expected week=35 actual="
                                + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"), new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-explicit-da-locale] semantic mismatch: expected week=34 actual="
                                + w.getWeek());
            }

            /* Read-only getters are documented as queries over the period. A correct implementation
               must not change the object's observable state merely because callers ask for week/year
               boundaries. This catches a "fix" that suppresses the bad path by silently mutating state. */
            int beforeWeek = w.getWeek();
            int beforeYearValue = w.getYearValue();
            long beforeFirst = w.getFirstMillisecond();
            long beforeLast = w.getLastMillisecond();
            long beforeSerial = w.getSerialIndex();
            int beforeHash = w.hashCode();
            String beforeString = w.toString();

            int afterWeek = w.getWeek();
            int afterYearValue = w.getYearValue();
            long afterFirst = w.getFirstMillisecond();
            long afterLast = w.getLastMillisecond();
            long afterSerial = w.getSerialIndex();
            int afterHash = w.hashCode();
            String afterString = w.toString();

            if (beforeWeek != afterWeek
                    || beforeYearValue != afterYearValue
                    || beforeFirst != afterFirst
                    || beforeLast != afterLast
                    || beforeSerial != afterSerial
                    || beforeHash != afterHash
                    || (beforeString == null ? afterString != null : !beforeString.equals(afterString))) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:hidden-state-getters] semantic mismatch: observable state changed across read-only getters"
                                + " beforeWeek=" + beforeWeek + " afterWeek=" + afterWeek
                                + " beforeYearValue=" + beforeYearValue + " afterYearValue=" + afterYearValue
                                + " beforeFirst=" + beforeFirst + " afterFirst=" + afterFirst
                                + " beforeLast=" + beforeLast + " afterLast=" + afterLast
                                + " beforeSerial=" + beforeSerial + " afterSerial=" + afterSerial
                                + " beforeHash=" + beforeHash + " afterHash=" + afterHash
                                + " beforeString=" + beforeString + " afterString=" + afterString);
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }

        {
            Locale relSavedLocale = Locale.getDefault();
            TimeZone relSavedZone = TimeZone.getDefault();
            Week a;
            Week b;
            int aWeek;
            int aYear;
            int bWeek;
            int bYear;
            try {
                String[] zoneIds = TimeZone.getAvailableIDs();
                if (zoneIds.length == 0) {
                    return;
                }
                TimeZone sourceZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
                TimeZone targetZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
                Locale defLocale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
                Locale.setDefault(defLocale);
                TimeZone.setDefault(sourceZone);
                Calendar cal = Calendar.getInstance(sourceZone, defLocale);
                cal.clear();
                cal.set(
                        data.consumeInt(1900, 9999),
                        data.consumeInt(0, 11),
                        data.consumeInt(1, 28),
                        data.consumeInt(0, 23),
                        data.consumeInt(0, 59),
                        data.consumeInt(0, 59));
                cal.set(Calendar.MILLISECOND, data.consumeInt(0, 999));
                Date t = cal.getTime();

                try {
                    a = new Week(t, targetZone);
                    b = new Week(t, targetZone, Locale.getDefault());
                    aWeek = a.getWeek();
                    aYear = a.getYearValue();
                    bWeek = b.getWeek();
                    bYear = b.getYearValue();
                } catch (Exception e) {
                    return;
                }
            } finally {
                Locale.setDefault(relSavedLocale);
                TimeZone.setDefault(relSavedZone);
            }

            if (aWeek != bWeek || aYear != bYear) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:ctor_zone_overload_agrees_with_explicit_locale] metamorphic violation: 2-arg and 3-arg constructors disagree for explicit default locale"
                                + " lhsWeek=" + aWeek + " lhsYear=" + aYear
                                + " rhsWeek=" + bWeek + " rhsYear=" + bYear);
            }
        }

        {
            Week w;
            Calendar cal;
            long noArgFirst;
            long calFirst;
            try {
                w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
                String[] zoneIds = TimeZone.getAvailableIDs();
                if (zoneIds.length == 0) {
                    return;
                }
                TimeZone zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
                Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
                cal = Calendar.getInstance(zone, locale);
                w.peg(cal);
                noArgFirst = w.getFirstMillisecond();
                calFirst = w.getFirstMillisecond(cal);
            } catch (Exception e) {
                return;
            }

            /* After peg(calendar), the no-arg getter is supposed to reflect that calendar basis,
               so getFirstMillisecond() must agree with getFirstMillisecond(calendar). */
            if (noArgFirst != calFirst) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:first_millisecond_matches_calendar_overload_after_peg] metamorphic violation: no-arg and Calendar overload disagree"
                                + " noArg=" + noArgFirst + " calendar=" + calFirst);
            }
        }

        {
            Week w;
            Calendar cal;
            long noArgLast;
            long calLast;
            try {
                w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
                String[] zoneIds = TimeZone.getAvailableIDs();
                if (zoneIds.length == 0) {
                    return;
                }
                TimeZone zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
                Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
                cal = Calendar.getInstance(zone, locale);
                w.peg(cal);
                noArgLast = w.getLastMillisecond();
                calLast = w.getLastMillisecond(cal);
            } catch (Exception e) {
                return;
            }

            /* Same sibling-agreement guarantee as for first millisecond: after peg(calendar),
               the stored interpretation and the explicit-calendar overload must represent the same week end. */
            if (noArgLast != calLast) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:last_millisecond_matches_calendar_overload_after_peg] metamorphic violation: no-arg and Calendar overload disagree"
                                + " noArg=" + noArgLast + " calendar=" + calLast);
            }
        }

        {
            Locale relSavedLocale = Locale.getDefault();
            TimeZone relSavedZone = TimeZone.getDefault();
            try {
                String[] zoneIds = TimeZone.getAvailableIDs();
                if (zoneIds.length == 0) {
                    return;
                }
                TimeZone sourceZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
                TimeZone targetZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
                Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
                Locale.setDefault(locale);
                TimeZone.setDefault(sourceZone);

                Calendar sourceCal = Calendar.getInstance(sourceZone, locale);
                sourceCal.clear();
                int year = data.consumeInt(1900, 9999);
                int month = data.consumeInt(0, 11);
                int day = data.consumeInt(1, 28);
                int hour = data.consumeInt(0, 23);
                int minute = data.consumeInt(0, 59);
                int second = data.consumeInt(0, 59);
                int millis = data.consumeInt(0, 999);
                sourceCal.set(year, month, day, hour, minute, second);
                sourceCal.set(Calendar.MILLISECOND, millis);
                Date t = sourceCal.getTime();

                Week fromCtor;
                Week fromCanonical;
                int ctorWeek;
                int ctorYear;
                int canWeek;
                int canYear;
                try {
                    fromCtor = new Week(t, targetZone, locale);
                    Calendar targetCal = Calendar.getInstance(targetZone, locale);
                    targetCal.setTime(t);
                    int expectedWeek = targetCal.get(Calendar.WEEK_OF_YEAR);
                    int expectedYear = targetCal.get(Calendar.YEAR);
                    if (month == Calendar.JANUARY && expectedWeek >= 52) {
                        expectedYear--;
                    } else if (month == Calendar.DECEMBER && expectedWeek == 1) {
                        expectedYear++;
                    }
                    fromCanonical = new Week(expectedWeek, expectedYear);
                    ctorWeek = fromCtor.getWeek();
                    ctorYear = fromCtor.getYearValue();
                    canWeek = fromCanonical.getWeek();
                    canYear = fromCanonical.getYearValue();
                } catch (Exception e) {
                    return;
                }

                /* Oracle-from-input: we first ask a real Calendar in the same target zone/locale which
                   week-of-year/year the instant belongs to, then construct that canonical Week directly.
                   A correct Week(Date, zone, locale) must recover the same logical week/year pair. */
                if (ctorWeek != canWeek || ctorYear != canYear) {
                    throw new FuzzerSecurityIssueLow(
                            "[oracle:ctor_matches_calendar_canonicalization] metamorphic violation: constructor result disagrees with canonical week/year built from Calendar"
                                    + " ctorWeek=" + ctorWeek + " ctorYear=" + ctorYear
                                    + " canonicalWeek=" + canWeek + " canonicalYear=" + canYear
                                    + " sourceZone=" + sourceZone.getID()
                                    + " targetZone=" + targetZone.getID()
                                    + " locale=" + locale.toString());
                }
            } finally {
                Locale.setDefault(relSavedLocale);
                TimeZone.setDefault(relSavedZone);
            }
        }
    }
}
```

</details>

---
## [125] ⚙️ harness-attempt · `attempt_020`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:ctor_zone_overload_agrees_with_explicit_locale] metamorphic violation: 2-arg and 3-arg constructors disagree for explicit default local…

---
## [126] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [81] — not reprinted*
- *[user] message: identical to step [83] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,955 chars)</summary>

```
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            int firstDay = cal.getFirstDayOfWeek();
            if (firstDay != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:dk-first-day] semantic mismatch: expected=2 actual=" + firstDay);
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:dk-week] semantic mismatch: expected=34 actual=" + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            firstDay = cal.getFirstDayOfWeek();
            if (firstDay != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:us-first-day] semantic mismatch: expected=1 actual=" + firstDay);
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();
            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:us-week-2arg] semantic mismatch: expected=35 actual=" + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:us-week-3arg] semantic mismatch: expected=34 actual=" + w.getWeek());
            }

            try {
                Locale.setDefault(Locale.US);
                TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));

                GregorianCalendar detroit = new GregorianCalendar(
                        TimeZone.getTimeZone("US/Detroit"), Locale.getDefault());
                detroit.set(2007, Calendar.AUGUST, 25, 23, 0, 0);
                detroit.set(Calendar.MILLISECOND, 0);
                Date boundary = detroit.getTime();
                TimeZone copenhagen = TimeZone.getTimeZone("Europe/Copenhagen");

                Week twoArg = new Week(boundary, copenhagen);
                Week threeArg = new Week(boundary, copenhagen, Locale.getDefault());

                /* Contract guarantee: Week(Date, TimeZone) is the deprecated sibling of
                 * Week(Date, TimeZone, Locale) and its javadoc says it calculates the week
                 * relative to the specified time zone; therefore it must agree with the
                 * 3-arg overload when passed Locale.getDefault(). A patch that ignores the
                 * explicit zone or silently substitutes another zone breaks this observable
                 * post-condition without throwing.
                 */
                if (!twoArg.equals(threeArg)
                        || twoArg.getWeek() != threeArg.getWeek()
                        || twoArg.getYearValue() != threeArg.getYearValue()) {
                    throw new RuntimeException(
                            "[oracle:overload-equivalence] metamorphic violation: Week(Date, TimeZone) != Week(Date, TimeZone, Locale.getDefault())"
                                    + " inputMillis=" + boundary.getTime()
                                    + " zone=" + copenhagen.getID()
                                    + " lhsWeek=" + twoArg.getWeek()
                                    + " rhsWeek=" + threeArg.getWeek()
                                    + " lhsYear=" + twoArg.getYearValue()
                                    + " rhsYear=" + threeArg.getYearValue());
                }

                Calendar checkCal = Calendar.getInstance(copenhagen, Locale.getDefault());
                /* Documented sibling agreement: getFirstMillisecond()/getLastMillisecond()
                 * return values relative to the constructor's time zone, and the overloads
                 * taking a Calendar evaluate using the supplied calendar. With a calendar
                 * built from the same zone/locale, both forms must agree.
                 */
                if (threeArg.getFirstMillisecond() != threeArg.getFirstMillisecond(checkCal)) {
                    throw new RuntimeException(
                            "[oracle:first-ms-agreement] metamorphic violation: getFirstMillisecond() disagrees with getFirstMillisecond(Calendar)"
                                    + " week=" + threeArg.getWeek()
                                    + " year=" + threeArg.getYearValue()
                                    + " lhs=" + threeArg.getFirstMillisecond()
                                    + " rhs=" + threeArg.getFirstMillisecond(checkCal));
                }
                if (threeArg.getLastMillisecond() != threeArg.getLastMillisecond(checkCal)) {
                    throw new RuntimeException(
                            "[oracle:last-ms-agreement] metamorphic violation: getLastMillisecond() disagrees with getLastMillisecond(Calendar)"
                                    + " week=" + threeArg.getWeek()
                                    + " year=" + threeArg.getYearValue()
                                    + " lhs=" + threeArg.getLastMillisecond()
                                    + " rhs=" + threeArg.getLastMillisecond(checkCal));
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable ignored) {
                return;
            }

            try {
                int year = 2000 + data.consumeInt(0, 20);
                int month = data.consumeInt(0, 11);
                int day = data.consumeInt(1, 28);
                int hour = data.consumeInt(0, 23);
                int minute = data.consumeInt(0, 59);
                TimeZone explicitZone = TimeZone.getTimeZone(
                        data.consumeBoolean() ? "Europe/Copenhagen" : "US/Detroit");
                Locale explicitLocale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");

                GregorianCalendar source = new GregorianCalendar(
                        TimeZone.getTimeZone("UTC"), explicitLocale);
                source.set(year, month, day, hour, minute, 0);
                source.set(Calendar.MILLISECOND, 0);
                Date fuzzDate = source.getTime();

                new Week(fuzzDate, explicitZone);
                Week wf = new Week(fuzzDate, explicitZone, explicitLocale);
                wf.getWeek();
                wf.getYearValue();
                wf.getSerialIndex();
                wf.getFirstMillisecond();
                wf.getLastMillisecond();
                wf.toString();
            } catch (Throwable ignored) {
                return;
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}
```

</details>

---
## [127] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. Jazzer exited cleanly with no finding on the buggy code. Most often this means every input was handled normally, or the harnes

---
## [128] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~8,155 chars, 1 new)</summary>

- *[system] message: identical to step [81] — not reprinted*
- *[user] message: identical to step [83] — not reprinted*
**[assistant]**
```
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            int firstDay = cal.getFirstDayOfWeek();
            if (firstDay != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:dk-first-day] semantic mismatch: expected=2 actual=" + firstDay);
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:dk-week] semantic mismatch: expected=34 actual=" + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            firstDay = cal.getFirstDayOfWeek();
            if (firstDay != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:us-first-day] semantic mismatch: expected=1 actual=" + firstDay);
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();
            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:us-week-2arg] semantic mismatch: expected=35 actual=" + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:us-week-3arg] semantic mismatch: expected=34 actual=" + w.getWeek());
            }

            try {
                Locale.setDefault(Locale.US);
                TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));

                GregorianCalendar detroit = new GregorianCalendar(
                        TimeZone.getTimeZone("US/Detroit"), Locale.getDefault());
                detroit.set(2007, Calendar.AUGUST, 25, 23, 0, 0);
                detroit.set(Calendar.MILLISECOND, 0);
                Date boundary = detroit.getTime();
                TimeZone copenhagen = TimeZone.getTimeZone("Europe/Copenhagen");

                Week twoArg = new Week(boundary, copenhagen);
                Week threeArg = new Week(boundary, copenhagen, Locale.getDefault());

                /* Contract guarantee: Week(Date, TimeZone) is the deprecated sibling of
                 * Week(Date, TimeZone, Locale) and its javadoc says it calculates the week
                 * relative to the specified time zone; therefore it must agree with the
                 * 3-arg overload when passed Locale.getDefault(). A patch that ignores the
                 * explicit zone or silently substitutes another zone breaks this observable
                 * post-condition without throwing.
                 */
                if (!twoArg.equals(threeArg)
                        || twoArg.getWeek() != threeArg.getWeek()
                        || twoArg.getYearValue() != threeArg.getYearValue()) {
                    throw new RuntimeException(
                            "[oracle:overload-equivalence] metamorphic violation: Week(Date, TimeZone) != Week(Date, TimeZone, Locale.getDefault())"
                                    + " inputMillis=" + boundary.getTime()
                                    + " zone=" + copenhagen.getID()
                                    + " lhsWeek=" + twoArg.getWeek()
                                    + " rhsWeek=" + threeArg.getWeek()
                                    + " lhsYear=" + twoArg.getYearValue()
                                    + " rhsYear=" + threeArg.getYearValue());
                }

                Calendar checkCal = Calendar.getInstance(copenhagen, Locale.getDefault());
                /* Documented sibling agreement: getFirstMillisecond()/getLastMillisecond()
                 * return values relative to the constructor's time zone, and the overloads
                 * taking a Calendar evaluate using the supplied calendar. With a calendar
                 * built from the same zone/locale, both forms must agree.
                 */
                if (threeArg.getFirstMillisecond() != threeArg.getFirstMillisecond(checkCal)) {
                    throw new RuntimeException(
                            "[oracle:first-ms-agreement] metamorphic violation: getFirstMillisecond() disagrees with getFirstMillisecond(Calendar)"
                                    + " week=" + threeArg.getWeek()
                                    + " year=" + threeArg.getYearValue()
                                    + " lhs=" + threeArg.getFirstMillisecond()
                                    + " rhs=" + threeArg.getFirstMillisecond(checkCal));
                }
                if (threeArg.getLastMillisecond() != threeArg.getLastMillisecond(checkCal)) {
                    throw new RuntimeException(
                            "[oracle:last-ms-agreement] metamorphic violation: getLastMillisecond() disagrees with getLastMillisecond(Calendar)"
                                    + " week=" + threeArg.getWeek()
                                    + " year=" + threeArg.getYearValue()
                                    + " lhs=" + threeArg.getLastMillisecond()
                                    + " rhs=" + threeArg.getLastMillisecond(checkCal));
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable ignored) {
                return;
            }

            try {
                int year = 2000 + data.consumeInt(0, 20);
                int month = data.consumeInt(0, 11);
                int day = data.consumeInt(1, 28);
                int hour = data.consumeInt(0, 23);
                int minute = data.consumeInt(0, 59);
                TimeZone explicitZone = TimeZone.getTimeZone(
                        data.consumeBoolean() ? "Europe/Copenhagen" : "US/Detroit");
                Locale explicitLocale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");

                GregorianCalendar source = new GregorianCalendar(
                        TimeZone.getTimeZone("UTC"), explicitLocale);
                source.set(year, month, day, hour, minute, 0);
                source.set(Calendar.MILLISECOND, 0);
                Date fuzzDate = source.getTime();

                new Week(fuzzDate, explicitZone);
                Week wf = new Week(fuzzDate, explicitZone, explicitLocale);
                wf.getWeek();
                wf.getYearValue();
                wf.getSerialIndex();
                wf.getFirstMillisecond();
                wf.getLastMillisecond();
                wf.toString();
            } catch (Throwable ignored) {
                return;
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}
```
- *[user] message: identical to step [87] — not reprinted*

</details>
<details open><summary>▸ Output (~6,414 chars)</summary>

```
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-dk-firstday] semantic mismatch: expected=2 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-dk-week] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-firstday] semantic mismatch: expected=1 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-2arg] semantic mismatch: expected=35 actual="
                                + w.getWeek());
            }

            Week w3 = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (w3.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-3arg-da] semantic mismatch: expected=34 actual="
                                + w3.getWeek());
            }

            /* Contract: the 2-arg constructor says it calculates the week relative to the
             * specified time zone; its fixed implementation delegates to the 3-arg overload
             * with Locale.getDefault(). Therefore, for the same Date and zone under the
             * current default locale, Week(Date, TimeZone) must equal
             * Week(Date, TimeZone, Locale.getDefault()). A buggy patch that ignores the
             * explicit zone breaks this observable relation without throwing.
             */
            Week wDefaultLocale = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    Locale.getDefault());
            if (!w.equals(wDefaultLocale)
                    || w.getWeek() != wDefaultLocale.getWeek()
                    || w.getYearValue() != wDefaultLocale.getYearValue()) {
                throw new RuntimeException(
                        "[oracle:ctor-overload-equivalence] metamorphic violation: "
                                + "Week(Date, TimeZone) != Week(Date, TimeZone, Locale.getDefault())"
                                + " inputMillis=" + t.getTime()
                                + " zone=Europe/Copenhagen"
                                + " defaultLocale=" + Locale.getDefault()
                                + " lhsWeek=" + w.getWeek()
                                + " rhsWeek=" + wDefaultLocale.getWeek()
                                + " lhsYear=" + w.getYearValue()
                                + " rhsYear=" + wDefaultLocale.getYearValue());
            }

            int year = 2000 + data.consumeInt(0, 20);
            int month = data.consumeInt(0, 11);
            int day = data.consumeInt(1, 28);
            int hour = data.consumeInt(0, 23);
            int minute = data.consumeInt(0, 59);
            TimeZone zone = TimeZone.getTimeZone(
                    data.consumeBoolean() ? "Europe/Copenhagen" : "US/Detroit");
            Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");

            GregorianCalendar extra = new GregorianCalendar(TimeZone.getTimeZone("UTC"), locale);
            extra.set(year, month, day, hour, minute, 0);
            extra.set(Calendar.MILLISECOND, 0);
            Date extraDate = extra.getTime();

            Week a = new Week(extraDate, zone);
            Week b = new Week(extraDate, zone, Locale.getDefault());

            /* Same documented constructor-delegation guarantee as above, generalized to
             * additional valid dates built by construction.
             */
            if (!a.equals(b)
                    || a.getWeek() != b.getWeek()
                    || a.getYearValue() != b.getYearValue()) {
                throw new RuntimeException(
                        "[oracle:fuzzed-ctor-overload-equivalence] metamorphic violation: "
                                + "Week(Date, TimeZone) != Week(Date, TimeZone, Locale.getDefault())"
                                + " inputMillis=" + extraDate.getTime()
                                + " zone=" + zone.getID()
                                + " defaultLocale=" + Locale.getDefault()
                                + " lhsWeek=" + a.getWeek()
                                + " rhsWeek=" + b.getWeek()
                                + " lhsYear=" + a.getYearValue()
                                + " rhsYear=" + b.getYearValue());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}
```

</details>

---
## [129] ⚙️ harness-attempt · `attempt_022`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.RuntimeException: [oracle:fuzzed-ctor-overload-equivalence] metamorphic violation: Week(Date, TimeZone) != Week(Date, TimeZone, Locale.getDefault()) inputMillis=1175990040000 zone=GMT defaul…

---
## [130] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [81] — not reprinted*
- *[user] message: identical to step [81] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~10,904 chars)</summary>

```
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        liftedWeekTestsConstructorOracle();
        relationCtorZoneOverloadAgreesWithExplicitLocale(data);
        relationFirstMillisecondMatchesCalendarOverloadAfterPeg(data);
        hiddenStateGetterCheck(data);
    }

    private static void liftedWeekTestsConstructorOracle() {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-first-day-daDK] semantic mismatch: expected="
                                + Calendar.MONDAY + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-copenhagen-default-daDK] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-first-day-us] semantic mismatch: expected="
                                + Calendar.SUNDAY + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-copenhagen-default-us] semantic mismatch: expected=35 actual="
                                + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-copenhagen-explicit-daDK] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void relationCtorZoneOverloadAgreesWithExplicitLocale(FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();

        Date t;
        TimeZone sourceZone;
        TimeZone targetZone;
        Locale defLocale;
        try {
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds.length == 0) {
                return;
            }
            sourceZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            targetZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            defLocale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            Locale.setDefault(defLocale);
            TimeZone.setDefault(sourceZone);
            Calendar cal = Calendar.getInstance(sourceZone, defLocale);
            cal.clear();
            cal.set(
                    data.consumeInt(1900, 9999),
                    data.consumeInt(0, 11),
                    data.consumeInt(1, 28),
                    data.consumeInt(0, 23),
                    data.consumeInt(0, 59),
                    data.consumeInt(0, 59));
            cal.set(Calendar.MILLISECOND, data.consumeInt(0, 999));
            t = cal.getTime();
        } catch (Exception e) {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        }

        Week a;
        Week b;
        try {
            a = new Week(t, targetZone);
            b = new Week(t, targetZone, Locale.getDefault());
        } catch (Exception e) {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }

        // Contract: the 2-arg ctor is the sibling of the 3-arg ctor and should behave
        // the same when the explicit locale is Locale.getDefault(); a patch that ignores
        // the provided zone or silently substitutes another environment value breaks this.
        if (a.getWeek() != b.getWeek() || a.getYearValue() != b.getYearValue()) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:ctor_zone_overload_agrees_with_explicit_locale] metamorphic violation: 2-arg week/year="
                            + a.getWeek() + "/" + a.getYearValue()
                            + " 3-arg week/year=" + b.getWeek() + "/" + b.getYearValue());
        }
    }

    private static void relationFirstMillisecondMatchesCalendarOverloadAfterPeg(FuzzedDataProvider data) {
        Week w;
        Calendar cal;
        try {
            w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds.length == 0) {
                return;
            }
            TimeZone zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            cal = Calendar.getInstance(zone, locale);
            w.peg(cal);
        } catch (Exception e) {
            return;
        }

        long a;
        long b;
        try {
            a = w.getFirstMillisecond();
            b = w.getFirstMillisecond(cal);
        } catch (Exception e) {
            return;
        }

        // Contract: after peg(calendar), the no-arg getter and the Calendar overload are
        // two views of the same first-millisecond computation for that calendar.
        if (a != b) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:first_millisecond_matches_calendar_overload_after_peg] metamorphic violation: noArg="
                            + a + " calendarOverload=" + b);
        }
    }

    private static void hiddenStateGetterCheck(FuzzedDataProvider data) {
        try {
            Week w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds.length == 0) {
                return;
            }
            TimeZone zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            Calendar cal = Calendar.getInstance(zone, locale);

            int weekBefore = w.getWeek();
            int yearBefore = w.getYearValue();
            long serialBefore = w.getSerialIndex();
            String stringBefore = w.toString();
            int hashBefore = w.hashCode();
            RegularTimePeriod prevBefore = w.previous();
            RegularTimePeriod nextBefore = w.next();
            long prevSerialBefore = prevBefore == null ? Long.MIN_VALUE : prevBefore.getSerialIndex();
            long nextSerialBefore = nextBefore == null ? Long.MIN_VALUE : nextBefore.getSerialIndex();

            try {
                w.getLastMillisecond(cal);
            } catch (Exception e) {
                return;
            }

            int weekAfter = w.getWeek();
            int yearAfter = w.getYearValue();
            long serialAfter = w.getSerialIndex();
            String stringAfter = w.toString();
            int hashAfter = w.hashCode();
            RegularTimePeriod prevAfter = w.previous();
            RegularTimePeriod nextAfter = w.next();
            long prevSerialAfter = prevAfter == null ? Long.MIN_VALUE : prevAfter.getSerialIndex();
            long nextSerialAfter = nextAfter == null ? Long.MIN_VALUE : nextAfter.getSerialIndex();

            // Contract: getLastMillisecond(Calendar) is a read-only question method; it
            // should not mutate the week identity or neighboring-period relations. A
            // throw-deleting or bookkeeping-skipping patch that corrupts internal state
            // would violate these stable observable properties.
            if (weekBefore != weekAfter
                    || yearBefore != yearAfter
                    || serialBefore != serialAfter
                    || hashBefore != hashAfter
                    || !safeEquals(stringBefore, stringAfter)
                    || prevSerialBefore != prevSerialAfter
                    || nextSerialBefore != nextSerialAfter) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:hidden_state_getLastMillisecond] metamorphic violation: before="
                                + weekBefore + "/" + yearBefore + "/" + serialBefore
                                + " after=" + weekAfter + "/" + yearAfter + "/" + serialAfter
                                + " hashBefore=" + hashBefore + " hashAfter=" + hashAfter
                                + " toStringBefore=" + escape(stringBefore)
                                + " toStringAfter=" + escape(stringAfter)
                                + " prevBefore=" + prevSerialBefore + " prevAfter=" + prevSerialAfter
                                + " nextBefore=" + nextSerialBefore + " nextAfter=" + nextSerialAfter);
            }
        } catch (Exception e) {
            return;
        }
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
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
## [131] ⚙️ harness-repair · `attempt_023`
**output:** **REPAIRED: swallowed-alarm**
- detail: {'applied': ['swallowed-alarm'], 'still_failing': []}
- reason: mechanically repaired before the structural gates — this harness would otherwise have been discarded

---
## [132] ⚙️ harness-attempt · `attempt_023`
**output:** **ACCEPTED (compiles + crashes the buggy build) [FROM REPAIRED ATTEMPT: swallowed-alarm]**
- detail: {'from_repaired_attempt': True, 'repairs_applied': ['swallowed-alarm'], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:ctor_zone_overload_agrees_with_explicit_locale] metamorphic violation: 2-arg week/year=1/1900 3-arg week/year=52/1899

---
## [133] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [81] — not reprinted*
- *[user] message: identical to step [83] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~8,600 chars)</summary>

```
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale daDK = new Locale("da", "DK");
            TimeZone copenhagen = TimeZone.getTimeZone("Europe/Copenhagen");
            TimeZone detroit = TimeZone.getTimeZone("US/Detroit");

            Locale.setDefault(daDK);
            TimeZone.setDefault(copenhagen);
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-da-firstday] semantic mismatch: expected=2 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-da-week] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(detroit);
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-firstday] semantic mismatch: expected=1 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-week] semantic mismatch: expected=35 actual="
                                + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-explicit-locale-week] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            try {
                Locale chosenLocale = data.consumeBoolean() ? Locale.US : daDK;
                TimeZone defaultZone = data.consumeBoolean() ? detroit : copenhagen;
                TimeZone ctorZone = defaultZone.getID().equals(detroit.getID())
                        ? copenhagen : detroit;

                Locale.setDefault(chosenLocale);
                TimeZone.setDefault(defaultZone);

                GregorianCalendar fuzzCal = (GregorianCalendar) Calendar.getInstance(
                        TimeZone.getDefault(), Locale.getDefault());
                int year = data.consumeInt(2000, 2010);
                int month = data.consumeInt(0, 11);
                int day = data.consumeInt(1, 28);
                int hour = data.consumeInt(0, 23);
                int minute = data.consumeInt(0, 59);
                int second = data.consumeInt(0, 59);
                fuzzCal.set(year, month, day, hour, minute, second);
                fuzzCal.set(Calendar.MILLISECOND, 0);
                Date fuzzDate = fuzzCal.getTime();

                Week twoArg = new Week(fuzzDate, ctorZone);
                Week threeArg = new Week(fuzzDate, ctorZone, Locale.getDefault());

                /* Contract justification: both constructors are documented as
                   "calculated relative to the specified time zone"; the 2-arg overload
                   just omits the locale and therefore must agree with the 3-arg overload
                   when passed Locale.getDefault(). A patch that silently ignores the
                   supplied zone or otherwise changes bookkeeping breaks this observable
                   equivalence without throwing. */
                if (twoArg.getWeek() != threeArg.getWeek()
                        || twoArg.getYearValue() != threeArg.getYearValue()
                        || twoArg.getFirstMillisecond() != threeArg.getFirstMillisecond()
                        || twoArg.getLastMillisecond() != threeArg.getLastMillisecond()
                        || !twoArg.equals(threeArg)) {
                    throw new RuntimeException(
                            "[oracle:overload-equivalence] metamorphic violation: "
                                    + "Week(Date,TimeZone) != Week(Date,TimeZone,Locale.getDefault())"
                                    + " inputYear=" + year
                                    + " inputMonth=" + month
                                    + " inputDay=" + day
                                    + " inputHour=" + hour
                                    + " inputMinute=" + minute
                                    + " inputSecond=" + second
                                    + " defaultLocale=" + Locale.getDefault()
                                    + " defaultZone=" + TimeZone.getDefault().getID()
                                    + " ctorZone=" + ctorZone.getID()
                                    + " lhsWeek=" + twoArg.getWeek()
                                    + " lhsYear=" + twoArg.getYearValue()
                                    + " lhsFirst=" + twoArg.getFirstMillisecond()
                                    + " lhsLast=" + twoArg.getLastMillisecond()
                                    + " rhsWeek=" + threeArg.getWeek()
                                    + " rhsYear=" + threeArg.getYearValue()
                                    + " rhsFirst=" + threeArg.getFirstMillisecond()
                                    + " rhsLast=" + threeArg.getLastMillisecond());
                }

                Calendar evalCal = Calendar.getInstance(ctorZone, Locale.getDefault());
                /* Contract justification: getFirstMillisecond() and
                   getFirstMillisecond(Calendar) are same-name overloads over the same
                   state, and the class javadoc says the no-arg form is determined
                   relative to the constructor time zone; supplying a Calendar with that
                   same zone should therefore yield the same observable value. */
                long firstNoArg = threeArg.getFirstMillisecond();
                long firstWithCal = threeArg.getFirstMillisecond(evalCal);
                if (firstNoArg != firstWithCal) {
                    throw new RuntimeException(
                            "[oracle:first-millis-overload] metamorphic violation: "
                                    + "getFirstMillisecond() != getFirstMillisecond(Calendar)"
                                    + " zone=" + ctorZone.getID()
                                    + " lhs=" + firstNoArg
                                    + " rhs=" + firstWithCal);
                }

                /* Contract justification: likewise for getLastMillisecond() and
                   getLastMillisecond(Calendar). */
                long lastNoArg = threeArg.getLastMillisecond();
                long lastWithCal = threeArg.getLastMillisecond(evalCal);
                if (lastNoArg != lastWithCal) {
                    throw new RuntimeException(
                            "[oracle:last-millis-overload] metamorphic violation: "
                                    + "getLastMillisecond() != getLastMillisecond(Calendar)"
                                    + " zone=" + ctorZone.getID()
                                    + " lhs=" + lastNoArg
                                    + " rhs=" + lastWithCal);
                }
            } catch (Throwable ignored) {
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}
```

</details>

---
## [134] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: That compiled, but it did NOT trigger the bug on the known-buggy version. Jazzer exited cleanly with no finding on the buggy code. Most often this means every input was handled normally, or the harnes

---
## [135] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~8,800 chars, 1 new)</summary>

- *[system] message: identical to step [81] — not reprinted*
- *[user] message: identical to step [83] — not reprinted*
**[assistant]**
```
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale daDK = new Locale("da", "DK");
            TimeZone copenhagen = TimeZone.getTimeZone("Europe/Copenhagen");
            TimeZone detroit = TimeZone.getTimeZone("US/Detroit");

            Locale.setDefault(daDK);
            TimeZone.setDefault(copenhagen);
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-da-firstday] semantic mismatch: expected=2 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-da-week] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(detroit);
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-firstday] semantic mismatch: expected=1 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-week] semantic mismatch: expected=35 actual="
                                + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-explicit-locale-week] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            try {
                Locale chosenLocale = data.consumeBoolean() ? Locale.US : daDK;
                TimeZone defaultZone = data.consumeBoolean() ? detroit : copenhagen;
                TimeZone ctorZone = defaultZone.getID().equals(detroit.getID())
                        ? copenhagen : detroit;

                Locale.setDefault(chosenLocale);
                TimeZone.setDefault(defaultZone);

                GregorianCalendar fuzzCal = (GregorianCalendar) Calendar.getInstance(
                        TimeZone.getDefault(), Locale.getDefault());
                int year = data.consumeInt(2000, 2010);
                int month = data.consumeInt(0, 11);
                int day = data.consumeInt(1, 28);
                int hour = data.consumeInt(0, 23);
                int minute = data.consumeInt(0, 59);
                int second = data.consumeInt(0, 59);
                fuzzCal.set(year, month, day, hour, minute, second);
                fuzzCal.set(Calendar.MILLISECOND, 0);
                Date fuzzDate = fuzzCal.getTime();

                Week twoArg = new Week(fuzzDate, ctorZone);
                Week threeArg = new Week(fuzzDate, ctorZone, Locale.getDefault());

                /* Contract justification: both constructors are documented as
                   "calculated relative to the specified time zone"; the 2-arg overload
                   just omits the locale and therefore must agree with the 3-arg overload
                   when passed Locale.getDefault(). A patch that silently ignores the
                   supplied zone or otherwise changes bookkeeping breaks this observable
                   equivalence without throwing. */
                if (twoArg.getWeek() != threeArg.getWeek()
                        || twoArg.getYearValue() != threeArg.getYearValue()
                        || twoArg.getFirstMillisecond() != threeArg.getFirstMillisecond()
                        || twoArg.getLastMillisecond() != threeArg.getLastMillisecond()
                        || !twoArg.equals(threeArg)) {
                    throw new RuntimeException(
                            "[oracle:overload-equivalence] metamorphic violation: "
                                    + "Week(Date,TimeZone) != Week(Date,TimeZone,Locale.getDefault())"
                                    + " inputYear=" + year
                                    + " inputMonth=" + month
                                    + " inputDay=" + day
                                    + " inputHour=" + hour
                                    + " inputMinute=" + minute
                                    + " inputSecond=" + second
                                    + " defaultLocale=" + Locale.getDefault()
                                    + " defaultZone=" + TimeZone.getDefault().getID()
                                    + " ctorZone=" + ctorZone.getID()
                                    + " lhsWeek=" + twoArg.getWeek()
                                    + " lhsYear=" + twoArg.getYearValue()
                                    + " lhsFirst=" + twoArg.getFirstMillisecond()
                                    + " lhsLast=" + twoArg.getLastMillisecond()
                                    + " rhsWeek=" + threeArg.getWeek()
                                    + " rhsYear=" + threeArg.getYearValue()
                                    + " rhsFirst=" + threeArg.getFirstMillisecond()
                                    + " rhsLast=" + threeArg.getLastMillisecond());
                }

                Calendar evalCal = Calendar.getInstance(ctorZone, Locale.getDefault());
                /* Contract justification: getFirstMillisecond() and
                   getFirstMillisecond(Calendar) are same-name overloads over the same
                   state, and the class javadoc says the no-arg form is determined
                   relative to the constructor time zone; supplying a Calendar with that
                   same zone should therefore yield the same observable value. */
                long firstNoArg = threeArg.getFirstMillisecond();
                long firstWithCal = threeArg.getFirstMillisecond(evalCal);
                if (firstNoArg != firstWithCal) {
                    throw new RuntimeException(
                            "[oracle:first-millis-overload] metamorphic violation: "
                                    + "getFirstMillisecond() != getFirstMillisecond(Calendar)"
                                    + " zone=" + ctorZone.getID()
                                    + " lhs=" + firstNoArg
                                    + " rhs=" + firstWithCal);
                }

                /* Contract justification: likewise for getLastMillisecond() and
                   getLastMillisecond(Calendar). */
                long lastNoArg = threeArg.getLastMillisecond();
                long lastWithCal = threeArg.getLastMillisecond(evalCal);
                if (lastNoArg != lastWithCal) {
                    throw new RuntimeException(
                            "[oracle:last-millis-overload] metamorphic violation: "
                                    + "getLastMillisecond() != getLastMillisecond(Calendar)"
                                    + " zone=" + ctorZone.getID()
                                    + " lhs=" + lastNoArg
                                    + " rhs=" + lastWithCal);
                }
            } catch (Throwable ignored) {
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}
```
- *[user] message: identical to step [87] — not reprinted*

</details>
<details open><summary>▸ Output (~7,796 chars)</summary>

```
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale daDK = new Locale("da", "DK");
            Locale us = Locale.US;
            TimeZone copenhagen = TimeZone.getTimeZone("Europe/Copenhagen");
            TimeZone detroit = TimeZone.getTimeZone("US/Detroit");

            // Lifted exactly from WeekTests.testConstructor().
            Locale.setDefault(daDK);
            TimeZone.setDefault(copenhagen);
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-first-day-da] semantic mismatch: expected=2 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();

            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-da-week] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            Locale.setDefault(us);
            TimeZone.setDefault(detroit);
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-first-day-us] semantic mismatch: expected=1 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-week] semantic mismatch: expected=35 actual="
                                + w.getWeek());
            }

            Week wExplicit = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (wExplicit.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-explicit-locale-week] semantic mismatch: expected=34 actual="
                                + wExplicit.getWeek());
            }

            /* Contract guarantee: the 2-arg constructor says the week is calculated
               relative to the specified time zone, and the 3-arg constructor says the
               same but with an explicit locale. Therefore, under Locale.getDefault(),
               Week(Date, zone) must agree with Week(Date, zone, Locale.getDefault()).
               A "fix" that suppresses or bypasses the changed behavior would violate
               this observable equivalence without throwing. */
            Week wDefaultLocale = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            Week wThreeArgDefaultLocale = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    Locale.getDefault());
            if (wDefaultLocale.getWeek() != wThreeArgDefaultLocale.getWeek()
                    || wDefaultLocale.getYearValue() != wThreeArgDefaultLocale.getYearValue()
                    || wDefaultLocale.getFirstMillisecond() != wThreeArgDefaultLocale.getFirstMillisecond()
                    || wDefaultLocale.getLastMillisecond() != wThreeArgDefaultLocale.getLastMillisecond()
                    || !wDefaultLocale.equals(wThreeArgDefaultLocale)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:constructor-equivalence] semantic mismatch: "
                                + "Week(Date,zone) and Week(Date,zone,Locale.getDefault()) disagree"
                                + " week2=" + wDefaultLocale.getWeek()
                                + " week3=" + wThreeArgDefaultLocale.getWeek()
                                + " year2=" + wDefaultLocale.getYearValue()
                                + " year3=" + wThreeArgDefaultLocale.getYearValue()
                                + " first2=" + wDefaultLocale.getFirstMillisecond()
                                + " first3=" + wThreeArgDefaultLocale.getFirstMillisecond()
                                + " last2=" + wDefaultLocale.getLastMillisecond()
                                + " last3=" + wThreeArgDefaultLocale.getLastMillisecond());
            }

            // Extra real-code coverage on the patched method with valid, non-degenerate inputs.
            // No assertions beyond trusted equivalence relations.
            TimeZone fuzzZone = data.consumeBoolean() ? copenhagen : detroit;
            Locale fuzzLocale = data.consumeBoolean() ? daDK : us;
            Locale.setDefault(fuzzLocale);
            TimeZone.setDefault(data.consumeBoolean() ? copenhagen : detroit);

            GregorianCalendar fuzzCal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());
            int year = 2000 + Math.abs(data.consumeInt()) % 20;
            int month = data.consumeInt(0, 11);
            int day = data.consumeInt(1, 28);
            int hour = data.consumeInt(0, 23);
            int minute = data.consumeInt(0, 59);
            int second = data.consumeInt(0, 59);
            fuzzCal.set(year, month, day, hour, minute, second);
            fuzzCal.set(Calendar.MILLISECOND, 0);
            Date fuzzDate = fuzzCal.getTime();

            Week a = new Week(fuzzDate, fuzzZone);
            Week b = new Week(fuzzDate, fuzzZone, Locale.getDefault());

            /* Same documented guarantee as above, generalized to fuzzed valid dates. */
            if (a.getWeek() != b.getWeek()
                    || a.getYearValue() != b.getYearValue()
                    || a.getFirstMillisecond() != b.getFirstMillisecond()
                    || a.getLastMillisecond() != b.getLastMillisecond()
                    || !a.equals(b)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:fuzzed-constructor-equivalence] semantic mismatch: "
                                + "date=" + fuzzDate.getTime()
                                + " defaultLocale=" + Locale.getDefault()
                                + " defaultZone=" + TimeZone.getDefault().getID()
                                + " ctorZone=" + fuzzZone.getID()
                                + " week2=" + a.getWeek()
                                + " week3=" + b.getWeek()
                                + " year2=" + a.getYearValue()
                                + " year3=" + b.getYearValue()
                                + " first2=" + a.getFirstMillisecond()
                                + " first3=" + b.getFirstMillisecond()
                                + " last2=" + a.getLastMillisecond()
                                + " last3=" + b.getLastMillisecond());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}
```

</details>

---
## [136] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Your harness computes an array/list index that can go NEGATIVE: near line 104: `Math.abs(data.consumeInt()) %` — Math.abs(Integer.MIN_VALUE) is NEGATIVE, so this index eventually goes out of bounds an

---
## [137] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (6 message(s), ~8,484 chars, 2 new)</summary>

- *[system] message: identical to step [81] — not reprinted*
- *[user] message: identical to step [83] — not reprinted*
- *[assistant] message: identical to step [135] — not reprinted*
- *[user] message: identical to step [87] — not reprinted*
**[assistant]**
```
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale daDK = new Locale("da", "DK");
            Locale us = Locale.US;
            TimeZone copenhagen = TimeZone.getTimeZone("Europe/Copenhagen");
            TimeZone detroit = TimeZone.getTimeZone("US/Detroit");

            // Lifted exactly from WeekTests.testConstructor().
            Locale.setDefault(daDK);
            TimeZone.setDefault(copenhagen);
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-first-day-da] semantic mismatch: expected=2 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();

            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-da-week] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            Locale.setDefault(us);
            TimeZone.setDefault(detroit);
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-first-day-us] semantic mismatch: expected=1 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-week] semantic mismatch: expected=35 actual="
                                + w.getWeek());
            }

            Week wExplicit = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (wExplicit.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-explicit-locale-week] semantic mismatch: expected=34 actual="
                                + wExplicit.getWeek());
            }

            /* Contract guarantee: the 2-arg constructor says the week is calculated
               relative to the specified time zone, and the 3-arg constructor says the
               same but with an explicit locale. Therefore, under Locale.getDefault(),
               Week(Date, zone) must agree with Week(Date, zone, Locale.getDefault()).
               A "fix" that suppresses or bypasses the changed behavior would violate
               this observable equivalence without throwing. */
            Week wDefaultLocale = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            Week wThreeArgDefaultLocale = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    Locale.getDefault());
            if (wDefaultLocale.getWeek() != wThreeArgDefaultLocale.getWeek()
                    || wDefaultLocale.getYearValue() != wThreeArgDefaultLocale.getYearValue()
                    || wDefaultLocale.getFirstMillisecond() != wThreeArgDefaultLocale.getFirstMillisecond()
                    || wDefaultLocale.getLastMillisecond() != wThreeArgDefaultLocale.getLastMillisecond()
                    || !wDefaultLocale.equals(wThreeArgDefaultLocale)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:constructor-equivalence] semantic mismatch: "
                                + "Week(Date,zone) and Week(Date,zone,Locale.getDefault()) disagree"
                                + " week2=" + wDefaultLocale.getWeek()
                                + " week3=" + wThreeArgDefaultLocale.getWeek()
                                + " year2=" + wDefaultLocale.getYearValue()
                                + " year3=" + wThreeArgDefaultLocale.getYearValue()
                                + " first2=" + wDefaultLocale.getFirstMillisecond()
                                + " first3=" + wThreeArgDefaultLocale.getFirstMillisecond()
                                + " last2=" + wDefaultLocale.getLastMillisecond()
                                + " last3=" + wThreeArgDefaultLocale.getLastMillisecond());
            }

            // Extra real-code coverage on the patched method with valid, non-degenerate inputs.
            // No assertions beyond trusted equivalence relations.
            TimeZone fuzzZone = data.consumeBoolean() ? copenhagen : detroit;
            Locale fuzzLocale = data.consumeBoolean() ? daDK : us;
            Locale.setDefault(fuzzLocale);
            TimeZone.setDefault(data.consumeBoolean() ? copenhagen : detroit);

            GregorianCalendar fuzzCal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());
            int year = 2000 + Math.abs(data.consumeInt()) % 20;
            int month = data.consumeInt(0, 11);
            int day = data.consumeInt(1, 28);
            int hour = data.consumeInt(0, 23);
            int minute = data.consumeInt(0, 59);
            int second = data.consumeInt(0, 59);
            fuzzCal.set(year, month, day, hour, minute, second);
            fuzzCal.set(Calendar.MILLISECOND, 0);
            Date fuzzDate = fuzzCal.getTime();

            Week a = new Week(fuzzDate, fuzzZone);
            Week b = new Week(fuzzDate, fuzzZone, Locale.getDefault());

            /* Same documented guarantee as above, generalized to fuzzed valid dates. */
            if (a.getWeek() != b.getWeek()
                    || a.getYearValue() != b.getYearValue()
                    || a.getFirstMillisecond() != b.getFirstMillisecond()
                    || a.getLastMillisecond() != b.getLastMillisecond()
                    || !a.equals(b)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:fuzzed-constructor-equivalence] semantic mismatch: "
                                + "date=" + fuzzDate.getTime()
                                + " defaultLocale=" + Locale.getDefault()
                                + " defaultZone=" + TimeZone.getDefault().getID()
                                + " ctorZone=" + fuzzZone.getID()
                                + " week2=" + a.getWeek()
                                + " week3=" + b.getWeek()
                                + " year2=" + a.getYearValue()
                                + " year3=" + b.getYearValue()
                                + " first2=" + a.getFirstMillisecond()
                                + " first3=" + b.getFirstMillisecond()
                                + " last2=" + a.getLastMillisecond()
                                + " last3=" + b.getLastMillisecond());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}
```
**[user]**
```
Your harness computes an array/list index that can go NEGATIVE: near line 104: `Math.abs(data.consumeInt()) %` — Math.abs(Integer.MIN_VALUE) is NEGATIVE, so this index eventually goes out of bounds and crashes the harness itself. Use Math.floorMod(value, n) or a bounded consume (e.g. data.consumeInt(0, n - 1)) instead
Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~7,643 chars)</summary>

```
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale daDK = new Locale("da", "DK");
            Locale us = Locale.US;
            TimeZone copenhagen = TimeZone.getTimeZone("Europe/Copenhagen");
            TimeZone detroit = TimeZone.getTimeZone("US/Detroit");

            Locale.setDefault(daDK);
            TimeZone.setDefault(copenhagen);
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-first-day-da] semantic mismatch: expected=2 actual="
                                + cal.getFirstDayOfWeek());
            }

            if (cal.getMinimalDaysInFirstWeek() <= 0) {
                return;
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();

            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-da-week] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            Locale.setDefault(us);
            TimeZone.setDefault(detroit);
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-first-day-us] semantic mismatch: expected=1 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-us-week] semantic mismatch: expected=35 actual="
                                + w.getWeek());
            }

            Week wExplicit = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (wExplicit.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:lifted-explicit-locale-week] semantic mismatch: expected=34 actual="
                                + wExplicit.getWeek());
            }

            /* Contract guarantee: the 2-arg constructor says the week is calculated
               relative to the specified time zone, and the 3-arg constructor says the
               same but with an explicit locale. Therefore, under Locale.getDefault(),
               Week(Date, zone) must agree with Week(Date, zone, Locale.getDefault()).
               A patch that silently bypasses the supplied zone would violate this
               observable equivalence without throwing. */
            Week wDefaultLocale = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            Week wThreeArgDefaultLocale = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    Locale.getDefault());
            if (wDefaultLocale.getWeek() != wThreeArgDefaultLocale.getWeek()
                    || wDefaultLocale.getYearValue() != wThreeArgDefaultLocale.getYearValue()
                    || wDefaultLocale.getFirstMillisecond() != wThreeArgDefaultLocale.getFirstMillisecond()
                    || wDefaultLocale.getLastMillisecond() != wThreeArgDefaultLocale.getLastMillisecond()
                    || !wDefaultLocale.equals(wThreeArgDefaultLocale)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:constructor-equivalence] semantic mismatch: "
                                + "Week(Date,zone) and Week(Date,zone,Locale.getDefault()) disagree"
                                + " week2=" + wDefaultLocale.getWeek()
                                + " week3=" + wThreeArgDefaultLocale.getWeek()
                                + " year2=" + wDefaultLocale.getYearValue()
                                + " year3=" + wThreeArgDefaultLocale.getYearValue()
                                + " first2=" + wDefaultLocale.getFirstMillisecond()
                                + " first3=" + wThreeArgDefaultLocale.getFirstMillisecond()
                                + " last2=" + wDefaultLocale.getLastMillisecond()
                                + " last3=" + wThreeArgDefaultLocale.getLastMillisecond());
            }

            TimeZone fuzzZone = data.consumeBoolean() ? copenhagen : detroit;
            Locale fuzzLocale = data.consumeBoolean() ? daDK : us;
            Locale.setDefault(fuzzLocale);
            TimeZone.setDefault(data.consumeBoolean() ? copenhagen : detroit);

            GregorianCalendar fuzzCal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());
            int year = data.consumeInt(2000, 2019);
            int month = data.consumeInt(0, 11);
            int day = data.consumeInt(1, 28);
            int hour = data.consumeInt(0, 23);
            int minute = data.consumeInt(0, 59);
            int second = data.consumeInt(0, 59);
            fuzzCal.set(year, month, day, hour, minute, second);
            fuzzCal.set(Calendar.MILLISECOND, 0);
            Date fuzzDate = fuzzCal.getTime();

            Week a = new Week(fuzzDate, fuzzZone);
            Week b = new Week(fuzzDate, fuzzZone, Locale.getDefault());

            /* Same documented guarantee as above, generalized to fuzzed valid dates. */
            if (a.getWeek() != b.getWeek()
                    || a.getYearValue() != b.getYearValue()
                    || a.getFirstMillisecond() != b.getFirstMillisecond()
                    || a.getLastMillisecond() != b.getLastMillisecond()
                    || !a.equals(b)) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:fuzzed-constructor-equivalence] semantic mismatch: "
                                + "date=" + fuzzDate.getTime()
                                + " defaultLocale=" + Locale.getDefault()
                                + " defaultZone=" + TimeZone.getDefault().getID()
                                + " ctorZone=" + fuzzZone.getID()
                                + " week2=" + a.getWeek()
                                + " week3=" + b.getWeek()
                                + " year2=" + a.getYearValue()
                                + " year3=" + b.getYearValue()
                                + " first2=" + a.getFirstMillisecond()
                                + " first3=" + b.getFirstMillisecond()
                                + " last2=" + a.getLastMillisecond()
                                + " last3=" + b.getLastMillisecond());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}
```

</details>

---
## [138] ⚙️ harness-attempt · `attempt_026`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:fuzzed-constructor-equivalence] semantic mismatch: date=946684800000 defaultLocale=en_US defaultZone=GMT ctorZone=GMT week2=1 week3=1 y…

---
## [139] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [81] — not reprinted*
- *[user] message: identical to step [81] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~9,090 chars)</summary>

```
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            // Lifted verbatim from WeekTests.testConstructor: in da_DK / Europe/Copenhagen,
            // Calendar reports Monday as first day of week.
            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-da-firstday] semantic mismatch: expected=" + Calendar.MONDAY
                                + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));

            // Lifted verbatim from the failing test: this exact call must yield week 34.
            if (w.getWeek() != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-copenhagen] semantic mismatch: expected=34 actual=" + w.getWeek());
            }

            // Hidden-state/read-only check: get* readers are observational queries and should not mutate
            // week/year/serial/string identity of the already-constructed Week. A "fix" that mutates state
            // while answering a getter would violate the API contract even if no exception is thrown.
            int beforeWeek = w.getWeek();
            int beforeYearValue = w.getYearValue();
            long beforeSerial = w.getSerialIndex();
            String beforeString = w.toString();
            int beforeHash = w.hashCode();
            w.getWeek();
            if (beforeWeek != w.getWeek()
                    || beforeYearValue != w.getYearValue()
                    || beforeSerial != w.getSerialIndex()
                    || beforeHash != w.hashCode()
                    || (beforeString == null ? w.toString() != null : !beforeString.equals(w.toString()))) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:getter-hidden-state] semantic mismatch: read-only getter changed observable state");
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            // Lifted verbatim from WeekTests.testConstructor: in Locale.US / US/Detroit,
            // Calendar reports Sunday as first day of week.
            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-firstday] semantic mismatch: expected=" + Calendar.SUNDAY
                                + " actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();
            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));

            // Ground-truth failing pair from the bug report: exact setup must produce 35 here.
            if (w.getWeek() != 35) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-detroit-to-copenhagen] semantic mismatch: expected=35 actual=" + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));

            // Lifted verbatim from WeekTests.testConstructor: explicit da_DK locale must yield 34.
            if (w.getWeek() != 34) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-explicit-locale] semantic mismatch: expected=34 actual=" + w.getWeek());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }

        relationCtorZoneOverloadAgreesWithExplicitLocale(data);
        relationFirstMillisecondMatchesCalendarOverloadAfterPeg(data);
    }

    private static void relationCtorZoneOverloadAgreesWithExplicitLocale(FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();

        Date t;
        TimeZone sourceZone;
        TimeZone targetZone;
        Locale defLocale;
        try {
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds == null || zoneIds.length == 0) {
                return;
            }
            sourceZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            targetZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            defLocale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");

            Locale.setDefault(defLocale);
            TimeZone.setDefault(sourceZone);

            Calendar cal = Calendar.getInstance(sourceZone, defLocale);
            cal.clear();
            cal.set(
                    data.consumeInt(1900, 9999),
                    data.consumeInt(0, 11),
                    data.consumeInt(1, 28),
                    data.consumeInt(0, 23),
                    data.consumeInt(0, 59),
                    data.consumeInt(0, 59));
            cal.set(Calendar.MILLISECOND, data.consumeInt(0, 999));
            t = cal.getTime();
        } catch (Exception e) {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        }

        Week a;
        try {
            a = new Week(t, targetZone);
        } catch (Exception e) {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        }

        Week b;
        try {
            b = new Week(t, targetZone, Locale.getDefault());
        } catch (Exception e) {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }

        // Documented sibling-agreement relation: Week(Date, TimeZone) and
        // Week(Date, TimeZone, Locale.getDefault()) describe the same semantics,
        // so they must compute the same week/year for the same instant and target zone.
        if (a.getWeek() != b.getWeek() || a.getYearValue() != b.getYearValue()) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:ctor_zone_overload_agrees_with_explicit_locale] metamorphic violation: 2-arg gave week/year "
                            + a.getWeek() + "/" + a.getYearValue()
                            + " but 3-arg gave " + b.getWeek() + "/" + b.getYearValue());
        }
    }

    private static void relationFirstMillisecondMatchesCalendarOverloadAfterPeg(FuzzedDataProvider data) {
        Week w;
        Calendar cal;
        try {
            w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds == null || zoneIds.length == 0) {
                return;
            }
            TimeZone zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            cal = Calendar.getInstance(zone, locale);
            w.peg(cal);
        } catch (Exception e) {
            return;
        }

        long a;
        try {
            a = w.getFirstMillisecond();
        } catch (Exception e) {
            return;
        }

        long b;
        try {
            b = w.getFirstMillisecond(cal);
        } catch (Exception e) {
            return;
        }

        // Overload-agreement contract: after peg(calendar), the no-arg getter and the
        // calendar overload must describe the same first millisecond for that calendar.
        if (a != b) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:first_millisecond_matches_calendar_overload_after_peg] metamorphic violation: no-arg="
                            + a + " calendar-overload=" + b);
        }
    }
}
```

</details>

---
## [140] ⚙️ harness-attempt · `attempt_027`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:ctor_zone_overload_agrees_with_explicit_locale] metamorphic violation: 2-arg gave week/year 1/1900 but 3-arg gave 52/1899

---
## [141] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [81] — not reprinted*
- *[user] message: identical to step [83] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~7,860 chars)</summary>

```
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-first-day-da] semantic mismatch: expected=2 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-da-week] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-first-day-us] semantic mismatch: expected=1 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-week] semantic mismatch: expected=35 actual="
                                + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-week-with-locale] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }

        Locale[] locales = Locale.getAvailableLocales();
        String[] ids = TimeZone.getAvailableIDs();
        if (locales.length == 0 || ids.length == 0) {
            return;
        }

        int localeIndex = Math.abs(data.consumeInt()) % locales.length;
        int zoneIndex = Math.abs(data.consumeInt()) % ids.length;
        Locale fuzzLocale = locales[localeIndex];
        TimeZone fuzzZone = TimeZone.getTimeZone(ids[zoneIndex]);
        Date fuzzDate = new Date((long) data.consumeInt());

        savedLocale = Locale.getDefault();
        savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(fuzzLocale);
            TimeZone.setDefault(fuzzZone);

            Week twoArg;
            Week threeArg;
            try {
                twoArg = new Week(fuzzDate, fuzzZone);
                threeArg = new Week(fuzzDate, fuzzZone, Locale.getDefault());
            } catch (Throwable ignored) {
                return;
            }

            /* Contract/oracle: Week(Date, TimeZone) and Week(Date, TimeZone, Locale)
               both document that the week is calculated relative to the specified
               time zone; with Locale.getDefault() installed as the default locale,
               the 2-arg overload should be equivalent to the 3-arg overload with
               that locale. A "fix" that merely avoids the buggy branch but keeps
               using the wrong zone/locale would violate these observable readers. */
            if (twoArg.getWeek() != threeArg.getWeek()
                    || twoArg.getYearValue() != threeArg.getYearValue()
                    || twoArg.getFirstMillisecond() != threeArg.getFirstMillisecond()
                    || twoArg.getLastMillisecond() != threeArg.getLastMillisecond()
                    || !twoArg.equals(threeArg)
                    || twoArg.hashCode() != threeArg.hashCode()) {
                throw new RuntimeException(
                        "[oracle:ctor-equiv] metamorphic violation: Week(Date,TimeZone) != Week(Date,TimeZone,Locale.getDefault())"
                                + " inputDate=" + fuzzDate.getTime()
                                + " zone=" + fuzzZone.getID()
                                + " locale=" + Locale.getDefault()
                                + " lhsWeek=" + twoArg.getWeek()
                                + " rhsWeek=" + threeArg.getWeek()
                                + " lhsYear=" + twoArg.getYearValue()
                                + " rhsYear=" + threeArg.getYearValue()
                                + " lhsFirst=" + twoArg.getFirstMillisecond()
                                + " rhsFirst=" + threeArg.getFirstMillisecond()
                                + " lhsLast=" + twoArg.getLastMillisecond()
                                + " rhsLast=" + threeArg.getLastMillisecond()
                                + " lhsEqualsRhs=" + twoArg.equals(threeArg)
                                + " lhsHash=" + twoArg.hashCode()
                                + " rhsHash=" + threeArg.hashCode());
            }

            Calendar cmpCal = Calendar.getInstance(fuzzZone, fuzzLocale);
            long firstWithCalendar;
            long lastWithCalendar;
            try {
                firstWithCalendar = threeArg.getFirstMillisecond(cmpCal);
                lastWithCalendar = threeArg.getLastMillisecond(cmpCal);
            } catch (Throwable ignored) {
                return;
            }

            /* Contract/oracle: getFirstMillisecond() / getFirstMillisecond(Calendar)
               and getLastMillisecond() / getLastMillisecond(Calendar) are documented
               as sibling accessors over the same time period; after construction with
               this zone+locale, supplying an equivalent Calendar must yield the same
               boundaries. A patch that skips/changes the internal bookkeeping would
               break these post-conditions without throwing. */
            if (threeArg.getFirstMillisecond() != firstWithCalendar
                    || threeArg.getLastMillisecond() != lastWithCalendar) {
                throw new RuntimeException(
                        "[oracle:millis-sibling] metamorphic violation: no-arg/calendar millisecond accessors disagree"
                                + " inputDate=" + fuzzDate.getTime()
                                + " zone=" + fuzzZone.getID()
                                + " locale=" + fuzzLocale
                                + " lhsFirst=" + threeArg.getFirstMillisecond()
                                + " rhsFirst=" + firstWithCalendar
                                + " lhsLast=" + threeArg.getLastMillisecond()
                                + " rhsLast=" + lastWithCalendar);
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}
```

</details>

---
## [142] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: Your harness computes an array/list index that can go NEGATIVE: near line 76: `Math.abs(data.consumeInt()) %` — Math.abs(Integer.MIN_VALUE) is NEGATIVE, so this index eventually goes out of bounds and

---
## [143] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (4 message(s), ~8,425 chars, 2 new)</summary>

- *[system] message: identical to step [81] — not reprinted*
- *[user] message: identical to step [83] — not reprinted*
**[assistant]**
```
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-first-day-da] semantic mismatch: expected=2 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-da-week] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-first-day-us] semantic mismatch: expected=1 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-week] semantic mismatch: expected=35 actual="
                                + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-week-with-locale] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }

        Locale[] locales = Locale.getAvailableLocales();
        String[] ids = TimeZone.getAvailableIDs();
        if (locales.length == 0 || ids.length == 0) {
            return;
        }

        int localeIndex = Math.abs(data.consumeInt()) % locales.length;
        int zoneIndex = Math.abs(data.consumeInt()) % ids.length;
        Locale fuzzLocale = locales[localeIndex];
        TimeZone fuzzZone = TimeZone.getTimeZone(ids[zoneIndex]);
        Date fuzzDate = new Date((long) data.consumeInt());

        savedLocale = Locale.getDefault();
        savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(fuzzLocale);
            TimeZone.setDefault(fuzzZone);

            Week twoArg;
            Week threeArg;
            try {
                twoArg = new Week(fuzzDate, fuzzZone);
                threeArg = new Week(fuzzDate, fuzzZone, Locale.getDefault());
            } catch (Throwable ignored) {
                return;
            }

            /* Contract/oracle: Week(Date, TimeZone) and Week(Date, TimeZone, Locale)
               both document that the week is calculated relative to the specified
               time zone; with Locale.getDefault() installed as the default locale,
               the 2-arg overload should be equivalent to the 3-arg overload with
               that locale. A "fix" that merely avoids the buggy branch but keeps
               using the wrong zone/locale would violate these observable readers. */
            if (twoArg.getWeek() != threeArg.getWeek()
                    || twoArg.getYearValue() != threeArg.getYearValue()
                    || twoArg.getFirstMillisecond() != threeArg.getFirstMillisecond()
                    || twoArg.getLastMillisecond() != threeArg.getLastMillisecond()
                    || !twoArg.equals(threeArg)
                    || twoArg.hashCode() != threeArg.hashCode()) {
                throw new RuntimeException(
                        "[oracle:ctor-equiv] metamorphic violation: Week(Date,TimeZone) != Week(Date,TimeZone,Locale.getDefault())"
                                + " inputDate=" + fuzzDate.getTime()
                                + " zone=" + fuzzZone.getID()
                                + " locale=" + Locale.getDefault()
                                + " lhsWeek=" + twoArg.getWeek()
                                + " rhsWeek=" + threeArg.getWeek()
                                + " lhsYear=" + twoArg.getYearValue()
                                + " rhsYear=" + threeArg.getYearValue()
                                + " lhsFirst=" + twoArg.getFirstMillisecond()
                                + " rhsFirst=" + threeArg.getFirstMillisecond()
                                + " lhsLast=" + twoArg.getLastMillisecond()
                                + " rhsLast=" + threeArg.getLastMillisecond()
                                + " lhsEqualsRhs=" + twoArg.equals(threeArg)
                                + " lhsHash=" + twoArg.hashCode()
                                + " rhsHash=" + threeArg.hashCode());
            }

            Calendar cmpCal = Calendar.getInstance(fuzzZone, fuzzLocale);
            long firstWithCalendar;
            long lastWithCalendar;
            try {
                firstWithCalendar = threeArg.getFirstMillisecond(cmpCal);
                lastWithCalendar = threeArg.getLastMillisecond(cmpCal);
            } catch (Throwable ignored) {
                return;
            }

            /* Contract/oracle: getFirstMillisecond() / getFirstMillisecond(Calendar)
               and getLastMillisecond() / getLastMillisecond(Calendar) are documented
               as sibling accessors over the same time period; after construction with
               this zone+locale, supplying an equivalent Calendar must yield the same
               boundaries. A patch that skips/changes the internal bookkeeping would
               break these post-conditions without throwing. */
            if (threeArg.getFirstMillisecond() != firstWithCalendar
                    || threeArg.getLastMillisecond() != lastWithCalendar) {
                throw new RuntimeException(
                        "[oracle:millis-sibling] metamorphic violation: no-arg/calendar millisecond accessors disagree"
                                + " inputDate=" + fuzzDate.getTime()
                                + " zone=" + fuzzZone.getID()
                                + " locale=" + fuzzLocale
                                + " lhsFirst=" + threeArg.getFirstMillisecond()
                                + " rhsFirst=" + firstWithCalendar
                                + " lhsLast=" + threeArg.getLastMillisecond()
                                + " rhsLast=" + lastWithCalendar);
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}
```
**[user]**
```
Your harness computes an array/list index that can go NEGATIVE: near line 76: `Math.abs(data.consumeInt()) %` — Math.abs(Integer.MIN_VALUE) is NEGATIVE, so this index eventually goes out of bounds and crashes the harness itself. Use Math.floorMod(value, n) or a bounded consume (e.g. data.consumeInt(0, n - 1)) instead
Return the full corrected FuzzHarness.java. Raw Java source only. No markdown fences.
```

</details>
<details open><summary>▸ Output (~7,848 chars)</summary>

```
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-first-day-da] semantic mismatch: expected=2 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-da-week] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-first-day-us] semantic mismatch: expected=1 actual="
                                + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-week] semantic mismatch: expected=35 actual="
                                + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"),
                    new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:testConstructor-us-week-with-locale] semantic mismatch: expected=34 actual="
                                + w.getWeek());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }

        Locale[] locales = Locale.getAvailableLocales();
        String[] ids = TimeZone.getAvailableIDs();
        if (locales.length == 0 || ids.length == 0) {
            return;
        }

        int localeIndex = data.consumeInt(0, locales.length - 1);
        int zoneIndex = data.consumeInt(0, ids.length - 1);
        Locale fuzzLocale = locales[localeIndex];
        TimeZone fuzzZone = TimeZone.getTimeZone(ids[zoneIndex]);
        Date fuzzDate = new Date((long) data.consumeInt());

        savedLocale = Locale.getDefault();
        savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(fuzzLocale);
            TimeZone.setDefault(fuzzZone);

            Week twoArg;
            Week threeArg;
            try {
                twoArg = new Week(fuzzDate, fuzzZone);
                threeArg = new Week(fuzzDate, fuzzZone, Locale.getDefault());
            } catch (Throwable ignored) {
                return;
            }

            /* Contract/oracle: Week(Date, TimeZone) and Week(Date, TimeZone, Locale)
               both document that the week is calculated relative to the specified
               time zone; with Locale.getDefault() installed as the default locale,
               the 2-arg overload should be equivalent to the 3-arg overload with
               that locale. A "fix" that merely avoids the buggy branch but keeps
               using the wrong zone/locale would violate these observable readers. */
            if (twoArg.getWeek() != threeArg.getWeek()
                    || twoArg.getYearValue() != threeArg.getYearValue()
                    || twoArg.getFirstMillisecond() != threeArg.getFirstMillisecond()
                    || twoArg.getLastMillisecond() != threeArg.getLastMillisecond()
                    || !twoArg.equals(threeArg)
                    || twoArg.hashCode() != threeArg.hashCode()) {
                throw new RuntimeException(
                        "[oracle:ctor-equiv] metamorphic violation: Week(Date,TimeZone) != Week(Date,TimeZone,Locale.getDefault())"
                                + " inputDate=" + fuzzDate.getTime()
                                + " zone=" + fuzzZone.getID()
                                + " locale=" + Locale.getDefault()
                                + " lhsWeek=" + twoArg.getWeek()
                                + " rhsWeek=" + threeArg.getWeek()
                                + " lhsYear=" + twoArg.getYearValue()
                                + " rhsYear=" + threeArg.getYearValue()
                                + " lhsFirst=" + twoArg.getFirstMillisecond()
                                + " rhsFirst=" + threeArg.getFirstMillisecond()
                                + " lhsLast=" + twoArg.getLastMillisecond()
                                + " rhsLast=" + threeArg.getLastMillisecond()
                                + " lhsEqualsRhs=" + twoArg.equals(threeArg)
                                + " lhsHash=" + twoArg.hashCode()
                                + " rhsHash=" + threeArg.hashCode());
            }

            Calendar cmpCal = Calendar.getInstance(fuzzZone, fuzzLocale);
            long firstWithCalendar;
            long lastWithCalendar;
            try {
                firstWithCalendar = threeArg.getFirstMillisecond(cmpCal);
                lastWithCalendar = threeArg.getLastMillisecond(cmpCal);
            } catch (Throwable ignored) {
                return;
            }

            /* Contract/oracle: getFirstMillisecond() / getFirstMillisecond(Calendar)
               and getLastMillisecond() / getLastMillisecond(Calendar) are documented
               as sibling accessors over the same time period; after construction with
               this zone+locale, supplying an equivalent Calendar must yield the same
               boundaries. A patch that skips/changes the internal bookkeeping would
               break these post-conditions without throwing. */
            if (threeArg.getFirstMillisecond() != firstWithCalendar
                    || threeArg.getLastMillisecond() != lastWithCalendar) {
                throw new RuntimeException(
                        "[oracle:millis-sibling] metamorphic violation: no-arg/calendar millisecond accessors disagree"
                                + " inputDate=" + fuzzDate.getTime()
                                + " zone=" + fuzzZone.getID()
                                + " locale=" + fuzzLocale
                                + " lhsFirst=" + threeArg.getFirstMillisecond()
                                + " rhsFirst=" + firstWithCalendar
                                + " lhsLast=" + threeArg.getLastMillisecond()
                                + " rhsLast=" + lastWithCalendar);
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }
}
```

</details>

---
## [144] ⚙️ harness-attempt · `attempt_029`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: java.lang.RuntimeException: [oracle:ctor-equiv] metamorphic violation: Week(Date,TimeZone) != Week(Date,TimeZone,Locale.getDefault()) inputDate=0 zone=Africa/Abidjan locale= lhsWeek=1 rhsWeek=1 lhsYea…

---
## [145] 🧠 LLM call — **harness generation** — model `gpt-5.4`
<details><summary>▸ Prompt (2 message(s), ~162 chars, 0 new)</summary>

- *[system] message: identical to step [81] — not reprinted*
- *[user] message: identical to step [81] — not reprinted*
*(every message identical to earlier steps)*

</details>
<details open><summary>▸ Output (~10,141 chars)</summary>

```
package org.jfree.data.time;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        liftedTestConstructorOracle();
        relationCtorZoneOverloadAgreesWithExplicitLocale(data);
        relationFirstMillisecondMatchesCalendarOverloadAfterPeg(data);
        relationLastMillisecondMatchesCalendarOverloadAfterPeg(data);
        hiddenStateReadOnlyGettersDoNotMutate(data);
    }

    private static void liftedTestConstructorOracle() {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();
        try {
            Locale.setDefault(new Locale("da", "DK"));
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Copenhagen"));
            GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance(
                    TimeZone.getDefault(), Locale.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.MONDAY) {
                throw new FuzzerSecurityIssueLow("[oracle:testConstructor-da-firstDay] semantic mismatch: expected=2 actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date t = cal.getTime();
            Week w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow("[oracle:testConstructor-da-week] semantic mismatch: expected=34 actual=" + w.getWeek());
            }

            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Detroit"));
            cal = (GregorianCalendar) Calendar.getInstance(TimeZone.getDefault());

            if (cal.getFirstDayOfWeek() != Calendar.SUNDAY) {
                throw new FuzzerSecurityIssueLow("[oracle:testConstructor-us-firstDay] semantic mismatch: expected=1 actual=" + cal.getFirstDayOfWeek());
            }

            cal.set(2007, Calendar.AUGUST, 26, 1, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            t = cal.getTime();

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"));
            if (w.getWeek() != 35) {
                throw new FuzzerSecurityIssueLow("[oracle:testConstructor-us-2arg] semantic mismatch: expected=35 actual=" + w.getWeek());
            }

            w = new Week(t, TimeZone.getTimeZone("Europe/Copenhagen"), new Locale("da", "DK"));
            if (w.getWeek() != 34) {
                throw new FuzzerSecurityIssueLow("[oracle:testConstructor-us-3arg] semantic mismatch: expected=34 actual=" + w.getWeek());
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }
    }

    private static void relationCtorZoneOverloadAgreesWithExplicitLocale(FuzzedDataProvider data) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedZone = TimeZone.getDefault();

        Date t;
        TimeZone sourceZone;
        TimeZone targetZone;
        Locale defLocale;

        try {
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds.length == 0) {
                return;
            }
            sourceZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            targetZone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            defLocale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            Locale.setDefault(defLocale);
            TimeZone.setDefault(sourceZone);

            Calendar cal = Calendar.getInstance(sourceZone, defLocale);
            cal.clear();
            cal.set(data.consumeInt(1900, 9999), data.consumeInt(0, 11), data.consumeInt(1, 28),
                    data.consumeInt(0, 23), data.consumeInt(0, 59), data.consumeInt(0, 59));
            cal.set(Calendar.MILLISECOND, data.consumeInt(0, 999));
            t = cal.getTime();
        } catch (Exception e) {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        }

        Week a;
        Week b;
        try {
            a = new Week(t, targetZone);
            b = new Week(t, targetZone, Locale.getDefault());
        } catch (Exception e) {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
            return;
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedZone);
        }

        if (a.getWeek() != b.getWeek() || a.getYearValue() != b.getYearValue()) {
            throw new FuzzerSecurityIssueLow("[oracle:ctor-zone-overload] metamorphic violation: Week(Date,TimeZone) must agree with Week(Date,TimeZone,Locale.getDefault()) on week/year; lhsWeek=" + a.getWeek() + " lhsYear=" + a.getYearValue() + " rhsWeek=" + b.getWeek() + " rhsYear=" + b.getYearValue());
        }
    }

    private static void relationFirstMillisecondMatchesCalendarOverloadAfterPeg(FuzzedDataProvider data) {
        Week w;
        Calendar cal;
        try {
            w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds.length == 0) {
                return;
            }
            TimeZone zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            cal = Calendar.getInstance(zone, locale);
            w.peg(cal);
        } catch (Exception e) {
            return;
        }

        long noArg;
        long withCal;
        try {
            noArg = w.getFirstMillisecond();
            withCal = w.getFirstMillisecond(cal);
        } catch (Exception e) {
            return;
        }

        if (noArg != withCal) {
            throw new FuzzerSecurityIssueLow("[oracle:first-ms-after-peg] metamorphic violation: getFirstMillisecond() must equal getFirstMillisecond(Calendar) after peg(calendar); lhs=" + noArg + " rhs=" + withCal);
        }
    }

    private static void relationLastMillisecondMatchesCalendarOverloadAfterPeg(FuzzedDataProvider data) {
        Week w;
        Calendar cal;
        try {
            w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds.length == 0) {
                return;
            }
            TimeZone zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            cal = Calendar.getInstance(zone, locale);
            w.peg(cal);
        } catch (Exception e) {
            return;
        }

        long noArg;
        long withCal;
        try {
            noArg = w.getLastMillisecond();
            withCal = w.getLastMillisecond(cal);
        } catch (Exception e) {
            return;
        }

        if (noArg != withCal) {
            throw new FuzzerSecurityIssueLow("[oracle:last-ms-after-peg] metamorphic violation: getLastMillisecond() must equal getLastMillisecond(Calendar) after peg(calendar); lhs=" + noArg + " rhs=" + withCal);
        }
    }

    private static void hiddenStateReadOnlyGettersDoNotMutate(FuzzedDataProvider data) {
        Week w;
        Calendar cal;
        try {
            w = new Week(data.consumeInt(1, 53), data.consumeInt(1900, 9999));
            String[] zoneIds = TimeZone.getAvailableIDs();
            if (zoneIds.length == 0) {
                return;
            }
            TimeZone zone = TimeZone.getTimeZone(zoneIds[data.consumeInt(0, zoneIds.length - 1)]);
            Locale locale = data.consumeBoolean() ? Locale.US : new Locale("da", "DK");
            cal = Calendar.getInstance(zone, locale);
            w.peg(cal);
        } catch (Exception e) {
            return;
        }

        int weekBefore;
        int yearBefore;
        long serialBefore;
        String strBefore;
        int hashBefore;
        try {
            weekBefore = w.getWeek();
            yearBefore = w.getYearValue();
            serialBefore = w.getSerialIndex();
            strBefore = w.toString();
            hashBefore = w.hashCode();
        } catch (Exception e) {
            return;
        }

        try {
            w.getFirstMillisecond();
            w.getFirstMillisecond(cal);
            w.getLastMillisecond();
            w.getLastMillisecond(cal);
        } catch (Exception e) {
            return;
        }

        int weekAfter;
        int yearAfter;
        long serialAfter;
        String strAfter;
        int hashAfter;
        try {
            weekAfter = w.getWeek();
            yearAfter = w.getYearValue();
            serialAfter = w.getSerialIndex();
            strAfter = w.toString();
            hashAfter = w.hashCode();
        } catch (Exception e) {
            return;
        }

        if (weekBefore != weekAfter || yearBefore != yearAfter || serialBefore != serialAfter
                || hashBefore != hashAfter || (strBefore == null ? strAfter != null : !strBefore.equals(strAfter))) {
            throw new FuzzerSecurityIssueLow("[oracle:read-only-getters-stable] metamorphic violation: read-only getters must not mutate observable week state; before=week:" + weekBefore + ",year:" + yearBefore + ",serial:" + serialBefore + ",hash:" + hashBefore + ",str:" + escape(strBefore) + " after=week:" + weekAfter + ",year:" + yearAfter + ",serial:" + serialAfter + ",hash:" + hashAfter + ",str:" + escape(strAfter));
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
## [146] ⚙️ harness-attempt · `attempt_030`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- detail: {'from_repaired_attempt': False, 'repairs_applied': [], 'repaired_source_reconstructable': True}
- trigger: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:ctor-zone-overload] metamorphic violation: Week(Date,TimeZone) must agree with Week(Date,TimeZone,Locale.getDefault()) on week/year; lh…

---
## [147] ⚙️ coverage · `jazzer coverage dumps`
**output:** **23/23 dump(s) written under /datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/cov; 23 raw fuzz log(s)**
<details><summary>▸ detail (~12,990 chars)</summary>

- detail: {'dumps': [{'harness': 'attempt_001', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/cov/attempt_001_compiled.exec', 'exists': True, 'bytes': 282}, {'harness': 'attempt_003', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/cov/attempt_003_compiled.exec', 'exists': True, 'bytes': 228}, {'harness': 'attempt_004', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/cov/attempt_004_compiled.exec', 'exists': True, 'bytes': 228}, {'harness': 'attempt_005', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/cov/attempt_005_compiled.exec', 'exists': True, 'bytes': 235}, {'harness': 'attempt_007', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/cov/attempt_007_compiled.exec', 'exists': True, 'bytes': 234}, {'harness': 'attempt_009', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/cov/attempt_009_compiled.exec', 'exists': True, 'bytes': 239}, {'harness': 'attempt_010', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/cov/attempt_010_compiled.exec', 'exists': True, 'bytes': 229}, {'harness': 'attempt_011', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/cov/attempt_011_compiled.exec', 'exists': True, 'bytes': 236}, {'harness': 'attempt_012', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/cov/attempt_012_compiled.exec', 'exists': True, 'bytes': 269}, {'harness': 'attempt_013', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/cov/attempt_013_compiled.exec', 'exists': True, 'bytes': 226}, {'harness': 'attempt_015', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/cov/attempt_015_compiled.exec', 'exists': True, 'bytes': 236}, {'harness': 'attempt_016', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/cov/attempt_016_compiled.exec', 'exists': True, 'bytes': 231}, {'harness': 'attempt_018', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/cov/attempt_018_compiled.exec', 'exists': True, 'bytes': 282}, {'harness': 'attempt_019', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/cov/attempt_019_compiled.exec', 'exists': True, 'bytes': 227}, {'harness': 'attempt_020', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/cov/attempt_020_compiled.exec', 'exists': True, 'bytes': 240}, {'harness': 'attempt_021', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/cov/attempt_021_compiled.exec', 'exists': True, 'bytes': 229}, {'harness': 'attempt_022', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/cov/attempt_022_compiled.exec', 'exists': True, 'bytes': 227}, {'harness': 'attempt_023', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/cov/attempt_023_compiled.exec', 'exists': True, 'bytes': 237}, {'harness': 'attempt_024', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/cov/attempt_024_compiled.exec', 'exists': True, 'bytes': 229}, {'harness': 'attempt_026', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/cov/attempt_026_compiled.exec', 'exists': True, 'bytes': 230}, {'harness': 'attempt_027', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/cov/attempt_027_compiled.exec', 'exists': True, 'bytes': 232}, {'harness': 'attempt_029', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/cov/attempt_029_compiled.exec', 'exists': True, 'bytes': 229}, {'harness': 'attempt_030', 'build': 'compiled', 'path': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/cov/attempt_030_compiled.exec', 'exists': True, 'bytes': 235}], 'outputs': ['/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/fuzz_out/attempt_001_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/fuzz_out/attempt_003_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/fuzz_out/attempt_004_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/fuzz_out/attempt_005_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/fuzz_out/attempt_007_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/fuzz_out/attempt_009_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/fuzz_out/attempt_010_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/fuzz_out/attempt_011_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/fuzz_out/attempt_012_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/fuzz_out/attempt_013_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/fuzz_out/attempt_015_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/fuzz_out/attempt_016_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/fuzz_out/attempt_018_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/fuzz_out/attempt_019_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/fuzz_out/attempt_020_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/fuzz_out/attempt_021_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/fuzz_out/attempt_022_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/fuzz_out/attempt_023_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/fuzz_out/attempt_024_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/fuzz_out/attempt_026_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/fuzz_out/attempt_027_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/fuzz_out/attempt_029_compiled.txt', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/fuzz_out/attempt_030_compiled.txt'], 'classpath': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/cov/classpath.json', 'dir': '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/cov', 'compiled_attempts': ['attempt_001', 'attempt_003', 'attempt_004', 'attempt_005', 'attempt_007', 'attempt_009', 'attempt_010', 'attempt_011', 'attempt_012', 'attempt_013', 'attempt_015', 'attempt_016', 'attempt_018', 'attempt_019', 'attempt_020', 'attempt_021', 'attempt_022', 'attempt_023', 'attempt_024', 'attempt_026', 'attempt_027', 'attempt_029', 'attempt_030'], 'accepted_attempts': ['attempt_001', 'attempt_004', 'attempt_005', 'attempt_007', 'attempt_009', 'attempt_010', 'attempt_011', 'attempt_013', 'attempt_015', 'attempt_016', 'attempt_018', 'attempt_019', 'attempt_020', 'attempt_022', 'attempt_023', 'attempt_026', 'attempt_027', 'attempt_029', 'attempt_030'], 'sources': ['/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/harness_src/attempt_001.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/harness_src/attempt_003.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/harness_src/attempt_004.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/harness_src/attempt_005.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/harness_src/attempt_007.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/harness_src/attempt_009.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/harness_src/attempt_010.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/harness_src/attempt_011.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/harness_src/attempt_012.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/harness_src/attempt_013.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/harness_src/attempt_015.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/harness_src/attempt_016.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/harness_src/attempt_018.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/harness_src/attempt_019.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/harness_src/attempt_020.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/harness_src/attempt_021.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/harness_src/attempt_022.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/harness_src/attempt_023.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/harness_src/attempt_024.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/harness_src/attempt_026.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/harness_src/attempt_027.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/harness_src/attempt_029.java', '/datadrive/vuln-patch/results/heldout_rcc_30_semantic_patches10/semantic/HN/007_patch1-Chart-8-CapGen_c_rep001/harness_src/attempt_030.java']}

</details>
