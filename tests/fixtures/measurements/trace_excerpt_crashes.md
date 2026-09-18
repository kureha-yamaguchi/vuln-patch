<!-- Excerpt of runs-archive/runs/final30B_20260729_145001/11_patch1-Chart-26-Jaid_c/trace.md (Chart-26). -->
<!-- Two verifier/judge sections, cut down to the evidence blocks: header line 10936 + lines 11138-11175 and 11206-11209; header line 12496 + lines 12693-12730 and 12765-12768. -->

## [87] 🧠 LLM call — **verifier / judge** — model `gpt-5.4`
</harness>

The assertion that ACTUALLY fired on the patched code is:
    com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:axislabel-entity-metadata] consistency violation: recorded AxisLabelEntity metadata disagrees with axis getters; label=Axis tooltip=tip…
Judge ONLY this assertion's soundness. Ignore other assertions in the file that did not fire — they cannot make THIS finding a false positive.

CONCRETE EVIDENCE captured at the firing (observed behaviour of the actual run — weigh this over hypotheticals):
<evidence>
== Java Exception: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:axislabel-entity-metadata] consistency violation: recorded AxisLabelEntity metadata disagrees with axis getters; label=Axis tooltip=tip url=u
	at org.jfree.chart.axis.FuzzHarness.fuzzerTestOneInput(FuzzHarness.java:152)
== libFuzzer crashing input ==
MS: 0 ; base unit: 0000000000000000000000000000000000000000


artifact_prefix='/home/code/scratch/co/final30B_20260729_145001/11_patch1-Chart-26-Jaid_c/Chart_26_buggy/fuzz/attempt_006/crashes/'; Test unit written to /home/code/scratch/co/final30B_20260729_145001/11_patch1-Chart-26-Jaid_c/Chart_26_buggy/fuzz/attempt_006/crashes/crash-da39a3ee5e6b4b0d3255bfef95601890afd80709
Base64: 
reproducer_path='/home/code/scratch/co/final30B_20260729_145001/11_patch1-Chart-26-Jaid_c/Chart_26_buggy/fuzz/attempt_006/crashes'; Java reproducer written to /home/code/scratch/co/final30B_20260729_145001/11_patch1-Chart-26-Jaid_c/Chart_26_buggy/fuzz/attempt_006/crashes/Crash_da39a3ee5e6b4b0d3255bfef95601890afd80709.java

INFO: seed corpus: files: 99 min: 1b max: 53b total: 517b rss: 1192Mb

== Java Exception: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:gantt-null-info] semantic mismatch: expected draw(..., null, null) to succeed as in GanttChartTests.testDrawWithNullInfo
	at org.jfree.chart.axis.FuzzHarness.fuzzerTestOneInput(FuzzHarness.java:39)
Caused by: java.lang.StringIndexOutOfBoundsException: begin 5, end 3, length 7
	at java.base/java.lang.String.checkBoundsBeginEnd(String.java:3319)
	at java.base/java.lang.String.substring(String.java:1874)
	at org.jfree.chart.text.G2TextMeasurer.getStringWidth(G2TextMeasurer.java:78)
	at org.jfree.chart.text.TextUtilities.nextLineBreak(TextUtilities.java:253)
	at org.jfree.chart.text.TextUtilities.createTextBlock(TextUtilities.java:195)
	at org.jfree.chart.text.TextUtilities.createTextBlock(TextUtilities.java:167)
[differential replay] laundering check: alarm wraps generic cause java.lang.StringIndexOutOfBoundsException@org.jfree.chart.text.G2TextMeasurer.getStringWidth; buggy-build replay gives headline=com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow@org.jfree.chart.axis.FuzzHarness.fuzzerTestOneInput, cause=java.lang.NullPointerException@org.jfree.chart.axis.Axis.drawLabel — NOT the same pre-existing crash
[symmetric firing] check(s) axislabel-entity-metadata ALSO fired on the buggy build during the acceptance scan — the patch did NOT change this behaviour. Keep this finding only if the violated contract is the very behaviour the failing test shows is wrong (the same observable, not merely the same method or class — a method has many independent behaviours and sharing one proves nothing about the patch); if it concerns a different observable or a setup-dependent one that would fire on any build, it is pre-existing surface — dismiss it.
[buggy-replay fact] on this exact input a DIFFERENT check fired first on the buggy build (gantt-null-info), so whether THIS check fires there is UNKNOWN — the replay is shadowed, not confirming. The screening DIRECTION-CONFIRMED fact was established on screening inputs, which may lie in a different input regime than this firing; it does NOT by itself establish the buggy build violates this check at THIS input. With no per-input attribution fact, judge on soundness alone, sceptically: to keep, the check's expected value must be justified by a shown contract or trusted value that covers THIS input's regime. [muted-replay fact] [fact:fires-on-both-confirmed] [fact:not-compared] with the shadowing check(s) gantt-null-info silenced, THIS check fires on the BUGGY build at this exact input — the same check fires on both builds (observed values were not compared, so no identical-value claim is made); judge the check's soundness on the shown contract.
[fire-rate fact] buggy build 999/1000 = 100% of random valid inputs. [fact:rate-indiscriminate] fires on essentially every input on the buggy build (100%) — the firing is intrinsic to the check/setup construction, not a detection of the defect.
[REAL FAILING TEST org.jfree.chart.junit.AreaChartTests::testDrawWithNullInfo — trust source #1, verbatim]
    public void testDrawWithNullInfo() {
        boolean success = false;
        try {
            BufferedImage image = new BufferedImage(200 , 100, 
    }
On the BUGGY build this test fails with: --- org.jfree.chart.junit.BarChart3DTests::testDrawWithNullInfo
junit.framework.AssertionFailedError
</evidence>

## [103] 🧠 LLM call — **verifier / judge** — model `gpt-5.4`
    }
}
</harness>

The assertion that ACTUALLY fired on the patched code is:
    com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:gantt-null-info] semantic mismatch: expected draw(..., null, null) to succeed as in GanttChartTests.testDrawWithNullInfo
Judge ONLY this assertion's soundness. Ignore other assertions in the file that did not fire — they cannot make THIS finding a false positive.

CONCRETE EVIDENCE captured at the firing (observed behaviour of the actual run — weigh this over hypotheticals):
<evidence>
== Java Exception: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:axislabel-entity-metadata] consistency violation: recorded AxisLabelEntity metadata disagrees with axis getters; label=Axis tooltip=tip url=u
	at org.jfree.chart.axis.FuzzHarness.fuzzerTestOneInput(FuzzHarness.java:152)
== libFuzzer crashing input ==
MS: 0 ; base unit: 0000000000000000000000000000000000000000


artifact_prefix='/home/code/scratch/co/final30B_20260729_145001/11_patch1-Chart-26-Jaid_c/Chart_26_buggy/fuzz/attempt_006/crashes/'; Test unit written to /home/code/scratch/co/final30B_20260729_145001/11_patch1-Chart-26-Jaid_c/Chart_26_buggy/fuzz/attempt_006/crashes/crash-da39a3ee5e6b4b0d3255bfef95601890afd80709
Base64: 
reproducer_path='/home/code/scratch/co/final30B_20260729_145001/11_patch1-Chart-26-Jaid_c/Chart_26_buggy/fuzz/attempt_006/crashes'; Java reproducer written to /home/code/scratch/co/final30B_20260729_145001/11_patch1-Chart-26-Jaid_c/Chart_26_buggy/fuzz/attempt_006/crashes/Crash_da39a3ee5e6b4b0d3255bfef95601890afd80709.java

INFO: seed corpus: files: 99 min: 1b max: 53b total: 517b rss: 1192Mb

== Java Exception: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: [oracle:gantt-null-info] semantic mismatch: expected draw(..., null, null) to succeed as in GanttChartTests.testDrawWithNullInfo
	at org.jfree.chart.axis.FuzzHarness.fuzzerTestOneInput(FuzzHarness.java:39)
Caused by: java.lang.StringIndexOutOfBoundsException: begin 5, end 3, length 7
	at java.base/java.lang.String.checkBoundsBeginEnd(String.java:3319)
	at java.base/java.lang.String.substring(String.java:1874)
	at org.jfree.chart.text.G2TextMeasurer.getStringWidth(G2TextMeasurer.java:78)
	at org.jfree.chart.text.TextUtilities.nextLineBreak(TextUtilities.java:253)
	at org.jfree.chart.text.TextUtilities.createTextBlock(TextUtilities.java:195)
	at org.jfree.chart.text.TextUtilities.createTextBlock(TextUtilities.java:167)
[differential replay] laundering check: alarm wraps generic cause java.lang.StringIndexOutOfBoundsException@org.jfree.chart.text.G2TextMeasurer.getStringWidth; buggy-build replay gives headline=com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow@org.jfree.chart.axis.FuzzHarness.fuzzerTestOneInput, cause=java.lang.NullPointerException@org.jfree.chart.axis.Axis.drawLabel — NOT the same pre-existing crash
[symmetric firing] check(s) gantt-null-info ALSO fired on the buggy build during the acceptance scan — the patch did NOT change this behaviour. Keep this finding only if the violated contract is the very behaviour the failing test shows is wrong (the same observable, not merely the same method or class — a method has many independent behaviours and sharing one proves nothing about the patch); if it concerns a different observable or a setup-dependent one that would fire on any build, it is pre-existing surface — dismiss it.
[buggy-replay fact] [fact:fires-on-both-confirmed] [fact:not-compared] the exact firing input fires the SAME check on the BUGGY build (observed values were not compared, so no identical-value claim is made). The REAL failing test was rerun on this patched build and PASSES, so the test's own scenario is settled in the patch's favour. Keep this finding ONLY if it asserts the very behaviour the failing test shows is wrong, at inputs the real test does NOT itself exercise; otherwise it measures pre-existing surface — dismiss. Underlying exception identity — on the buggy replay: com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow@org.jfree.chart.axis.FuzzHarness.fuzzerTestOneInput; root cause: java.lang.NullPointerException@org.jfree.chart.axis.Axis.drawLabel; under the patched firing: StringIndexOutOfBoundsException. An identity different from the reported bug's own failure is a different, pre-existing problem.
[trigger-test lift] this oracle lifts testDrawWithNullInfo; the REAL test passes on this build. Whether this firing replays the test's own scenario is undetermined (no numeric value could be compared) — judge on soundness alone.
[buggy-scan fact] [fact:fires-on-buggy-scan] the acceptance-time buggy keep-going scan recorded this oracle firing on the BUGGY build: gantt-null-info (NullPointerException, StringIndexOutOfBoundsException) — the same check fires on both builds; keep only if the violated observable is the failing test's own.
[fire-rate fact] buggy build 999/1000 = 100% of random valid inputs. [fact:rate-indiscriminate] fires on essentially every input on the buggy build (100%) — the firing is intrinsic to the check/setup construction, not a detection of the defect.
[REAL FAILING TEST org.jfree.chart.junit.AreaChartTests::testDrawWithNullInfo — trust source #1, verbatim]
    }
On the BUGGY build this test fails with: --- org.jfree.chart.junit.BarChart3DTests::testDrawWithNullInfo
junit.framework.AssertionFailedError
</evidence>
