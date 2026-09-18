# Pipeline trace — Closure-18

**Code:** `9b63217`

**Patch label:** overfitting  *(the patch under analysis is a known-OVERFIT fix — the pipeline is not told this)*


---
## [2] ⚙️ analysis (TargetAnalyzer)
<details><summary>▸ output — modified_files": [ (~35,953 chars)</summary>

**output:** 
```json
{
  "modified_files": [
    "src/com/google/javascript/jscomp/Compiler.java"
  ],
  "patch_text": "--- /src/com/google/javascript/jscomp/Compiler.java\t\n+++ /src/com/google/javascript/jscomp/Compiler.java\n@@ -1285,7 +1285,7 @@\n \n       // Check if the sources need to be re-ordered.\n       boolean staleInputs = false;\n-      if (options.dependencyOptions.needsManagement() && options.closurePass) {\n+      if((options.dependencyOptions.needsManagement())||(options.closurePass)){\n         for (CompilerInput input : inputs) {\n           // Forward-declare all the provided types, so that they\n           // are not flagged even if they are dropped from the process.\n",
  "functions": [
    {
      "func_name": "parseInputs",
      "func_signature": "Node parseInputs()",
      "func_source": "  Node parseInputs() {\n    boolean devMode = options.devMode != DevMode.OFF;\n\n    // If old roots exist (we are parsing a second time), detach each of the\n    // individual file parse trees.\n    if (externsRoot != null) {\n      externsRoot.detachChildren();\n    }\n    if (jsRoot != null) {\n      jsRoot.detachChildren();\n    }\n\n    // Parse main JS sources.\n    jsRoot = IR.block();\n    jsRoot.setIsSyntheticBlock(true);\n\n    externsRoot = IR.block();\n    externsRoot.setIsSyntheticBlock(true);\n\n    externAndJsRoot = IR.block(externsRoot, jsRoot);\n    externAndJsRoot.setIsSyntheticBlock(true);\n\n    if (options.tracer.isOn()) {\n      tracker = new PerformanceTracker(jsRoot, options.tracer);\n      addChangeHandler(tracker.getCodeChangeHandler());\n    }\n\n    Tracer tracer = newTracer(\"parseInputs\");\n\n    try {\n      // Parse externs sources.\n      for (CompilerInput input : externs) {\n        Node n = input.getAstRoot(this);\n        if (hasErrors()) {\n          return null;\n        }\n        externsRoot.addChildToBack(n);\n      }\n\n      // Modules inferred in ProcessCommonJS pass.\n      if (options.transformAMDToCJSModules || options.processCommonJSModules) {\n        processAMDAndCommonJSModules();\n      }\n\n      hoistExterns(externsRoot);\n\n      // Check if the sources need to be re-ordered.\n      boolean staleInputs = false;\n      if (options.dependencyOptions.needsManagement() && options.closurePass) {\n        for (CompilerInput input : inputs) {\n          // Forward-declare all the provided types, so that they\n          // are not flagged even if they are dropped from the process.\n          for (String provide : input.getProvides()) {\n            getTypeRegistry().forwardDeclareType(provide);\n          }\n        }\n\n        try {\n          inputs =\n              (moduleGraph == null ? new JSModuleGraph(modules) : moduleGraph)\n              .manageDependencies(options.dependencyOptions, inputs);\n          staleInputs = true;\n        } catch (CircularDependencyException e) {\n          report(JSError.make(\n              JSModule.CIRCULAR_DEPENDENCY_ERROR, e.getMessage()));\n\n          // If in IDE mode, we ignore the error and keep going.\n          if (hasErrors()) {\n            return null;\n          }\n        } catch (MissingProvideException e) {\n          report(JSError.make(\n              MISSING_ENTRY_ERROR, e.getMessage()));\n\n          // If in IDE mode, we ignore the error and keep going.\n          if (hasErrors()) {\n            return null;\n          }\n        }\n      }\n\n      hoistNoCompileFiles();\n\n      if (staleInputs) {\n        repartitionInputs();\n      }\n\n      // Build the AST.\n      for (CompilerInput input : inputs) {\n        Node n = input.getAstRoot(this);\n        if (n == null) {\n          continue;\n        }\n\n        if (devMode) {\n          runSanityCheck();\n          if (hasErrors()) {\n            return null;\n          }\n        }\n\n        if (options.sourceMapOutputPath != null ||\n            options.nameReferenceReportPath != null) {\n\n          // Annotate the nodes in the tree with information from the\n          // input file. This information is used to construct the SourceMap.\n          SourceInformationAnnotator sia =\n              new SourceInformationAnnotator(\n                  input.getName(), options.devMode != DevMode.OFF);\n          NodeTraversal.traverse(this, n, sia);\n        }\n\n        jsRoot.addChildToBack(n);\n      }\n\n      if (hasErrors()) {\n        return null;\n      }\n      return externAndJsRoot;\n    } finally {\n      stopTracer(tracer, \"parseInputs\");\n    }\n  }",
      "func_class": "Compiler",
      "func_class_fq": "com.google.javascript.jscomp.Compiler",
      "func_param_types": [],
      "fi_name": "[com.google.javascript.jscomp.Compiler].parseInputs()",
      "overload_types": [
        []
      ],
      "xrefs": [
        "public void parse() {\n    parseInputs();\n  }",
        "public void helperInlineReferenceToFunction(\n      String code, final String expectedResult,\n      final String fnName, final InliningMode mode,\n      final boolean decompose) {\n    final Compiler compiler = new Compiler();\n    final FunctionInjector injector = new FunctionInjector(\n        compiler, compiler.getUniqueNameIdSupplier(), decompose,\n        assumeStrictThis,\n        assumeMinimumCapture);\n\n    List<SourceFile> externsInputs = Lists.newArrayList(\n        SourceFile.fromCode(\"externs\", \"\"));\n\n    CompilerOptions options = new CompilerOptions();\n    options.setCodingConvention(new GoogleCodingConvention());\n    compiler.init(externsInputs, Lists.newArrayList(\n        SourceFile.fromCode(\"code\", code)), options);\n    Node parseRoot = compiler.parseInputs();\n    Node externsRoot = parseRoot.getFirstChild();\n    final Node tree = parseRoot.getLastChild();\n    assertNotNull(tree);\n    assertTrue(tree != externsRoot);\n\n    final Node expectedRoot = parseExpected(new Compiler(), expectedResult);\n\n    Node mainRoot = tree;\n    MarkNoSideEffectCalls mark = new MarkNoSideEffectCalls(compiler);\n    mark.process(externsRoot, mainRoot);\n\n    Normalize normalize = new Normalize(compiler, false);\n    normalize.process(externsRoot, mainRoot);\n    compiler.setLifeCycleStage(LifeCycleStage.NORMALIZED);\n\n    final Node fnNode = findFunction(tree, fnName);\n    assertNotNull(fnNode);\n    final Set<String> unsafe =\n        FunctionArgumentInjector.findModifiedParameters(fnNode);\n    assertNotNull(fnNode);\n\n    // inline tester\n    Method tester = new Method() {\n      @Override\n      public boolean call(NodeTraversal t, Node n, Node parent) {\n\n        CanInlineResult canInline = injector.canInlineReferenceToFunction(\n            t, n, fnNode, unsafe, mode,\n            NodeUtil.referencesThis(fnNode),\n            NodeUtil.containsFunction(NodeUtil.getFunctionBody(fnNode)));\n        assertTrue(\"canInlineReferenceToFunction should not be CAN_NOT_INLINE\",\n            CanInlineResult.NO != canInline);\n        if (decompose) {\n          assertTrue(\"canInlineReferenceToFunction \" +\n              \"should be CAN_INLINE_AFTER_DECOMPOSITION\",\n              CanInlineResult.AFTER_PREPARATION == canInline);\n\n          Set<String> knownConstants = Sets.newHashSet();\n          ExpressionDecomposer decomposer = new ExpressionDecomposer(\n              compiler, compiler.getUniqueNameIdSupplier(), knownConstants);\n          injector.setKnownConstants(knownConstants);\n          injector.maybePrepareCall(n);\n\n          assertTrue(\"canInlineReferenceToFunction \" +\n              \"should be CAN_INLINE\",\n              CanInlineResult.YES != canInline);\n        }\n\n        Node result = injector.inline(\n            t, n, fnName, fnNode, mode);\n        validateSourceInfo(compiler, result);\n        String explanation = expectedRoot.checkTreeEquals(tree.getFirstChild());\n        assertNull(\"\\nExpected: \" + toSource(expectedRoot) +\n            \"\\nResult: \" + toSource(tree.getFirstChild()) +\n            \"\\n\" + explanation, explanation);\n        return true;\n      }\n    };\n\n    compiler.resetUniqueNameId();\n    TestCallback test = new TestCallback(fnName, tester);\n    NodeTraversal.traverse(compiler, tree, test);\n  }",
        "protected Node parse(String[] original, CompilerOptions options) {\n    Compiler compiler = new Compiler();\n    List<SourceFile> inputs = Lists.newArrayList();\n    for (int i = 0; i < original.length; i++) {\n      inputs.add(SourceFile.fromCode(\"input\" + i, original[i]));\n    }\n    compiler.init(externs, inputs, options);\n    checkUnexpectedErrorsOrWarnings(compiler, 0);\n    Node all = compiler.parseInputs();\n    checkUnexpectedErrorsOrWarnings(compiler, 0);\n    Node n = all.getLastChild();\n    Node externs = all.getFirstChild();\n\n    (new CreateSyntheticBlocks(\n        compiler, \"synStart\", \"synEnd\")).process(externs, n);\n    (new Normalize(compiler, false)).process(externs, n);\n    (MakeDeclaredNamesUnique.getContextualRenameInverter(compiler)).process(\n        externs, n);\n    (new Denormalize(compiler)).process(externs, n);\n    return n;\n  }",
        "@SuppressWarnings(\"unchecked\")\n  private void testSets(boolean runTightenTypes, String js, String fieldTypes) {\n    this.runTightenTypes = runTightenTypes;\n    Compiler compiler = new Compiler();\n    CompilerOptions options = new CompilerOptions();\n    compiler.init(\n        ImmutableList.of(SourceFile.fromCode(\"externs\", \"\")),\n        ImmutableList.of(SourceFile.fromCode(\"testcode\", js)),\n        options);\n\n    Node root = compiler.parseInputs();\n    assertTrue(\"Unexpected parse error(s): \" +\n        Joiner.on(\"\\n\").join(compiler.getErrors()), root != null);\n\n    Node externsRoot = root.getFirstChild();\n    Node mainRoot = externsRoot.getNext();\n    getProcessor(compiler).process(externsRoot, mainRoot);\n\n    assertEquals(fieldTypes, mapToString(lastPass.getRenamedTypesForTesting()));\n  }",
        "public void checkSynthesizedExtern(\n      String extern, String input, String expectedExtern) {\n    Compiler compiler = new Compiler();\n    CompilerOptions options = new CompilerOptions();\n    options.setWarningLevel(\n        DiagnosticGroup.forType(VarCheck.UNDEFINED_VAR_ERROR),\n        CheckLevel.OFF);\n    compiler.init(\n        ImmutableList.of(SourceFile.fromCode(\"extern\", extern)),\n        ImmutableList.of(SourceFile.fromCode(\"input\", input)),\n        options);\n    compiler.parseInputs();\n    assertFalse(compiler.hasErrors());\n\n    Node externsAndJs = compiler.getRoot();\n    Node root = externsAndJs.getLastChild();\n\n    Node rootOriginal = root.cloneTree();\n    Node externs = externsAndJs.getFirstChild();\n\n    Node expected = compiler.parseTestCode(expectedExtern);\n    assertFalse(compiler.hasErrors());\n\n    (new VarCheck(compiler, sanityCheck))\n        .process(externs, root);\n    if (!sanityCheck) {\n      (new VariableTestCheck(compiler)).process(externs, root);\n    }\n\n    String externsCode = compiler.toSource(externs);\n    String expectedCode = compiler.toSource(expected);\n\n    assertEquals(expectedCode, externsCode);\n  }"
      ],
      "reachable": [
        "[com.google.javascript.rhino.Node].detachChildren()",
        "[com.google.javascript.rhino.IR].block()",
        "[com.google.javascript.rhino.Node].setIsSyntheticBlock(boolean)",
        "[com.google.javascript.rhino.IR].block(com.google.javascript.rhino.Node,com.google.javascript.rhino.Node)",
        "[TracerMode].isOn()",
        "[com.google.javascript.jscomp.PerformanceTracker].<init>(com.google.javascript.rhino.Node,TracerMode)",
        "[com.google.javascript.jscomp.PerformanceTracker].getCodeChangeHandler()",
        "[com.google.javascript.jscomp.Compiler].addChangeHandler(com.google.javascript.jscomp.CodeChangeHandler)",
        "[com.google.javascript.jscomp.Compiler].newTracer(String)",
        "input.getAstRoot()",
        "[com.google.javascript.jscomp.Compiler].hasErrors()",
        "[com.google.javascript.rhino.Node].addChildToBack(com.google.javascript.jscomp.Compiler)",
        "[com.google.javascript.jscomp.Compiler].processAMDAndCommonJSModules()",
        "[com.google.javascript.jscomp.Compiler].hoistExterns(com.google.javascript.rhino.Node)",
        "[com.google.javascript.jscomp.DependencyOptions].needsManagement()",
        "input.getProvides()",
        "[com.google.javascript.jscomp.Compiler].getTypeRegistry()",
        "[com.google.javascript.rhino.jstype.JSTypeRegistry].forwardDeclareType(com.google.javascript.jscomp.Compiler)",
        "(moduleGraph == null ? new JSModuleGraph(modules) : moduleGraph).manageDependencies(com.google.javascript.jscomp.DependencyOptions,java.util.List<com.google.javascript.jscomp.CompilerInput>)",
        "e.getMessage()",
        "[com.google.javascript.jscomp.JSError].make(com.google.javascript.jscomp.DiagnosticType,com.google.javascript.jscomp.Compiler)",
        "[com.google.javascript.jscomp.Compiler].report(com.google.javascript.jscomp.JSError)",
        "[com.google.javascript.jscomp.Compiler].hoistNoCompileFiles()",
        "[com.google.javascript.jscomp.Compiler].repartitionInputs()",
        "[com.google.javascript.jscomp.Compiler].runSanityCheck()",
        "input.getName()",
        "[com.google.javascript.jscomp.SourceInformationAnnotator].<init>(com.google.javascript.jscomp.Compiler,com.google.javascript.jscomp.Compiler)",
        "[com.google.javascript.jscomp.NodeTraversal].traverse(com.google.javascript.jscomp.Compiler,com.google.javascript.jscomp.SourceInformationAnnotator)",
        "[com.google.javascript.jscomp.Compiler].stopTracer(com.google.javascript.jscomp.Tracer,String)",
        "child.getNext()",
        "[com.google.javascript.rhino.IR].mayBeStatement(com.google.javascript.rhino.IR)",
        "[com.google.common.base.Preconditions].checkState(boolean)",
        "[com.google.javascript.rhino.Node].addChildToBack(com.google.javascript.rhino.IR)",
        "[com.google.javascript.rhino.Node].putBooleanProp(com.google.javascript.rhino.Node,boolean)",
        "[java.util.List<com.google.javascript.jscomp.CodeChangeHandler>].add(com.google.javascript.jscomp.CodeChangeHandler)",
        "[com.google.javascript.jscomp.CodeChangeHandler.RecentChange].hasCodeChanged()",
        "[com.google.javascript.jscomp.PerformanceTracker].recordPassStart(String)",
        "[com.google.javascript.jscomp.Tracer].<init>(String,com.google.javascript.jscomp.Compiler)",
        "[com.google.javascript.jscomp.Compiler].hasHaltingErrors()",
        "[com.google.common.collect.Maps].newLinkedHashMap()",
        "input.setCompiler()",
        "[com.google.javascript.jscomp.TransformAMDToCJSModule].<init>()",
        "[com.google.javascript.jscomp.TransformAMDToCJSModule].process(null,com.google.javascript.jscomp.Compiler)",
        "[com.google.javascript.jscomp.ProcessCommonJSModules].<init>(String)",
        "[com.google.javascript.jscomp.ProcessCommonJSModules].process(null,com.google.javascript.jscomp.Compiler)",
        "[com.google.javascript.jscomp.ProcessCommonJSModules].getModule()",
        "[com.google.javascript.jscomp.JSModule].getName()",
        "[com.google.javascript.jscomp.Compiler].put(String,com.google.javascript.jscomp.JSModule)",
        "[com.google.javascript.jscomp.Compiler].put(com.google.javascript.jscomp.Compiler,com.google.javascript.jscomp.JSModule)",
        "[com.google.javascript.jscomp.Compiler].values()",
        "[com.google.common.collect.Lists].newArrayList(com.google.javascript.jscomp.Compiler)",
        "[com.google.javascript.jscomp.Compiler].isEmpty()",
        "[com.google.javascript.jscomp.JSModuleGraph].<init>(java.util.List<com.google.javascript.jscomp.JSModule>)",
        "module.getInputs()",
        "input.getRequires()",
        "[com.google.javascript.jscomp.Compiler].get(com.google.javascript.jscomp.Compiler)",
        "module.addDependency(com.google.javascript.jscomp.Compiler)",
        "[com.google.common.collect.Lists].newArrayList()",
        "[com.google.javascript.jscomp.JSModuleGraph].manageDependencies(com.google.javascript.jscomp.DependencyOptions,java.util.List<com.google.javascript.jscomp.CompilerInput>)",
        "[com.google.javascript.jscomp.Compiler].add(com.google.javascript.jscomp.Compiler)",
        "[com.google.javascript.jscomp.JSModuleGraph].<init>(com.google.javascript.jscomp.Compiler)",
        "[com.google.common.base.Throwables].propagate(com.google.javascript.jscomp.Compiler)",
        "[com.google.javascript.jscomp.Compiler].getJSDocInfo()",
        "[com.google.javascript.jscomp.Compiler].isExterns()",
        "input.setIsExtern(boolean)",
        "input.getModule()",
        "[com.google.javascript.jscomp.Compiler].remove(com.google.javascript.jscomp.Compiler)",
        "[java.util.List<com.google.javascript.jscomp.CompilerInput>].add(com.google.javascript.jscomp.Compiler)",
        "[com.google.javascript.rhino.jstype.JSTypeRegistry].<init>(com.google.javascript.rhino.ErrorReporter,boolean)",
        "[com.google.javascript.jscomp.JSError].getDefaultLevel()",
        "[com.google.javascript.jscomp.WarningsGuard].level(com.google.javascript.jscomp.JSError)",
        "level.isOn()",
        "[com.google.javascript.jscomp.Compiler].getOptions()",
        "[com.google.javascript.jscomp.ErrorHandler].report(com.google.javascript.jscomp.Compiler,com.google.javascript.jscomp.JSError)",
        "[com.google.javascript.jscomp.ErrorManager].report(com.google.javascript.jscomp.Compiler,com.google.javascript.jscomp.JSError)",
        "[com.google.javascript.jscomp.Compiler].isNoCompile()",
        "[com.google.javascript.jscomp.Compiler].fillEmptyModules(java.util.List<com.google.javascript.jscomp.JSModule>)",
        "[com.google.javascript.jscomp.Compiler].rebuildInputsFromModules()",
        "[com.google.javascript.jscomp.PassFactory].create()",
        "[com.google.javascript.jscomp.CompilerPass].process(com.google.javascript.rhino.Node,com.google.javascript.rhino.Node)",
        "[com.google.javascript.jscomp.Tracer].stop()",
        "[com.google.javascript.jscomp.PerformanceTracker].recordPassStop(String,long)",
        "[java.util.Deque<String>].push(String)",
        "[com.google.javascript.jscomp.CodeChangeHandler.RecentChange].reset()",
        "[com.google.javascript.jscomp.Compiler].isIdeMode()",
        "[com.google.javascript.jscomp.Compiler].getErrorCount()",
        "[com.google.common.collect.Maps].newHashMap()",
        "[java.util.List<com.google.javascript.jscomp.JSModule>].size()",
        "[com.google.common.collect.Sets].newHashSet(java.util.List<com.google.javascript.jscomp.JSModule>)",
        "[com.google.javascript.jscomp.JSModuleGraph].size()",
        "[com.google.common.base.Preconditions].checkState(com.google.javascript.jscomp.JSModuleGraph,String)",
        "[com.google.common.collect.ImmutableList].copyOf(java.util.List<com.google.javascript.jscomp.JSModule>)",
        "module.getDependencies()",
        "dep.getDepth()",
        "module.getName()",
        "dep.getName()",
        "String.format(String,com.google.javascript.jscomp.JSModuleGraph,com.google.javascript.jscomp.JSModuleGraph)",
        "[ModuleDependenceException].<init>(com.google.javascript.jscomp.JSModuleGraph,com.google.javascript.jscomp.JSModuleGraph,com.google.javascript.jscomp.JSModuleGraph)",
        "Math.max(com.google.javascript.jscomp.JSModuleGraph,int)",
        "module.setDepth(com.google.javascript.jscomp.JSModuleGraph)",
        "[java.util.ArrayList].<init>()",
        "[com.google.javascript.jscomp.JSModuleGraph].add(java.util.ArrayList)",
        "[com.google.javascript.jscomp.JSModuleGraph].get(com.google.javascript.jscomp.JSModuleGraph)",
        "[com.google.javascript.jscomp.JSModuleGraph].add(com.google.javascript.jscomp.JSModuleGraph)",
        "[com.google.javascript.jscomp.deps.SortedDependencies].<init>(java.util.List<com.google.javascript.jscomp.CompilerInput>)",
        "[com.google.common.collect.Sets].newLinkedHashSet()",
        "[com.google.javascript.jscomp.DependencyOptions].shouldPruneDependencies()",
        "[com.google.javascript.jscomp.DependencyOptions].shouldDropMoochers()",
        "[com.google.javascript.jscomp.deps.SortedDependencies].getInputsWithoutProvides()",
        "[com.google.javascript.jscomp.JSModuleGraph].addAll(java.util.List<INPUT>)",
        "[com.google.javascript.jscomp.DependencyOptions].getEntryPoints()",
        "[com.google.javascript.jscomp.deps.SortedDependencies].getInputProviding(com.google.javascript.jscomp.JSModuleGraph)",
        "[com.google.javascript.jscomp.JSModuleGraph].add(INPUT)",
        "[com.google.javascript.jscomp.deps.SortedDependencies].maybeGetInputProviding(String)",
        "[com.google.javascript.jscomp.JSModuleGraph].addAll(java.util.List<com.google.javascript.jscomp.CompilerInput>)",
        "[com.google.javascript.jscomp.DependencyOptions].shouldSortDependencies()",
        "[com.google.javascript.jscomp.deps.SortedDependencies].getDependenciesOf(java.util.List<com.google.javascript.jscomp.CompilerInput>,boolean)",
        "[com.google.common.collect.LinkedListMultimap].create()",
        "[com.google.common.base.Preconditions].checkNotNull(com.google.javascript.jscomp.JSModuleGraph)",
        "[com.google.javascript.jscomp.JSModuleGraph].put(com.google.javascript.jscomp.JSModuleGraph,com.google.javascript.jscomp.JSModuleGraph)",
        "[com.google.javascript.jscomp.JSModuleGraph].getAllModules()",
        "[com.google.javascript.jscomp.JSModuleGraph].removeAll()",
        "[com.google.javascript.jscomp.JSModuleGraph].keySet()",
        "[com.google.javascript.jscomp.deps.SortedDependencies].getDependenciesOf(com.google.javascript.jscomp.JSModuleGraph,boolean)",
        "input.setModule(com.google.javascript.jscomp.JSModuleGraph)",
        "input.setModule(null)",
        "[com.google.javascript.jscomp.JSModuleGraph].getDeepestCommonDependencyInclusive(com.google.javascript.jscomp.JSModuleGraph,com.google.javascript.jscomp.JSModuleGraph)",
        "input.setModule(com.google.javascript.jscomp.JSModule)",
        "[com.google.javascript.jscomp.JSModuleGraph].getInputs()",
        "[com.google.javascript.jscomp.JSModuleGraph].addAll(com.google.javascript.jscomp.JSModuleGraph)",
        "[java.util.HashSet].<init>()",
        "[com.google.common.collect.LinkedHashMultimap].create()",
        "[com.google.common.collect.ArrayListMultimap].create()",
        "JSTypeNative.values()",
        "[java.util.HashMap].<init>()",
        "[com.google.javascript.rhino.jstype.JSTypeRegistry].resetForTypeCheck()",
        "[com.google.javascript.jscomp.Compiler].createFillFileName(com.google.javascript.jscomp.Compiler)",
        "[com.google.javascript.jscomp.SourceFile].fromCode(String,String)",
        "module.add(com.google.javascript.jscomp.SourceFile)",
        "[com.google.javascript.jscomp.Compiler].getAllInputsFromModules(java.util.List<com.google.javascript.jscomp.JSModule>)",
        "[com.google.javascript.jscomp.Compiler].initInputsByIdMap()",
        "[java.util.Deque<String>].pop()",
        "[String].equals(com.google.javascript.jscomp.PerformanceTracker)",
        "[RuntimeException].<init>(String)",
        "[com.google.javascript.jscomp.PerformanceTracker].estimateCodeSize(com.google.javascript.rhino.Node)",
        "[com.google.javascript.jscomp.PerformanceTracker.Stats].<init>(com.google.javascript.jscomp.PerformanceTracker)",
        "[java.util.List<Stats>].add(com.google.javascript.jscomp.PerformanceTracker.Stats)",
        "[com.google.javascript.jscomp.PerformanceTracker].updateStats(com.google.javascript.jscomp.PerformanceTracker.Stats,long,CodeSizeEstimatePrinter)",
        "[java.util.Map<String,Stats>].get(String)",
        "[com.google.javascript.jscomp.PerformanceTracker.Stats].<init>(String)",
        "[java.util.Map<String,Stats>].put(String,com.google.javascript.jscomp.PerformanceTracker.Stats)",
        "[CodeSizeEstimatePrinter].calcSize()",
        "[CodeSizeEstimatePrinter].calcZippedSize()",
        "[com.google.javascript.rhino.jstype.JSTypeRegistry].forwardDeclareType(String)"
      ],
      "related_callees": [
        {
          "name": "isOn",
          "source_file": "CheckLevel.java",
          "signature": "boolean isOn()",
          "source": "  boolean isOn() {\n    return this != OFF;\n  }",
          "is_abstract": false,
          "impls": [
            [
              "CheckLevel.java",
              "  boolean isOn() {\n    return this != OFF;\n  }"
            ],
            [
              "CompilerOptions.java",
              "    boolean isOn() {\n      return this != OFF;\n    }"
            ],
            [
              "CompilerOptions.java",
              "    public boolean isOn() {\n      return this != OFF;\n    }"
            ]
          ]
        },
        {
          "name": "addChangeHandler",
          "source_file": "AbstractCompiler.java",
          "signature": "abstract void addChangeHandler(CodeChangeHandler handler)",
          "source": "  abstract void addChangeHandler(CodeChangeHandler handler);",
          "is_abstract": true,
          "impls": [
            [
              "Compiler.java",
              "  void addChangeHandler(CodeChangeHandler handler) {\n    codeChangeHandlers.add(handler);\n  }"
            ]
          ]
        },
        {
          "name": "getCodeChangeHandler",
          "source_file": "PerformanceTracker.java",
          "signature": "CodeChangeHandler getCodeChangeHandler()",
          "source": "  CodeChangeHandler getCodeChangeHandler() {\n    return codeChange;\n  }",
          "is_abstract": false,
          "impls": []
        },
        {
          "name": "newTracer",
          "source_file": "Compiler.java",
          "signature": "Tracer newTracer(String passName)",
          "source": "  Tracer newTracer(String passName) {\n    String comment = passName\n        + (recentChange.hasCodeChanged() ? \" on recently changed AST\" : \"\");\n    if (options.tracer.isOn()) {\n      tracker.recordPassStart(passName);\n    }\n    return new Tracer(\"Compiler\", comment);\n  }",
          "is_abstract": false,
          "impls": [
            [
              "Compiler.java",
              "  Tracer newTracer(String passName) {\n    String comment = passName\n        + (recentChange.hasCodeChanged() ? \" on recently changed AST\" : \"\");\n    if (options.tracer.isOn()) {\n      tracker.recordPassStart(passName);\n    }\n    return new Tracer(\"Compiler\", comment);\n  }"
            ],
            [
              "PhaseOptimizer.java",
              "  private Tracer newTracer(String passName) {\n    String comment = passName +\n        (recentChange.hasCodeChanged() ? \" on recently changed AST\" : \"\");\n    if (tracker != null) {\n      tracker.recordPassStart(passName);\n    }\n    return new Tracer(\"JSCompiler\", comment);\n  }"
            ]
          ]
        },
        {
          "name": "getAstRoot",
          "source_file": "SourceAst.java",
          "signature": "public Node getAstRoot(AbstractCompiler compiler)",
          "source": "  public Node getAstRoot(AbstractCompiler compiler);",
          "is_abstract": true,
          "impls": [
            [
              "CompilerInput.java",
              "  public Node getAstRoot(AbstractCompiler compiler) {\n    Node root = ast.getAstRoot(compiler);\n    // The root maybe null if the AST can not be created.\n    if (root != null) {\n      Preconditions.checkState(root.isScript());\n      Preconditions.checkNotNull(root.getInputId());\n    }\n    return root;\n  }"
            ],
            [
              "JsAst.java",
              "  public Node getAstRoot(AbstractCompiler compiler) {\n    if (root == null) {\n      parse(compiler);\n      root.setInputId(inputId);\n    }\n    return root;\n  }"
            ],
            [
              "SyntheticAst.java",
              "  public Node getAstRoot(AbstractCompiler compiler) {\n    return root;\n  }"
            ]
          ]
        },
        {
          "name": "hasErrors",
          "source_file": "Compiler.java",
          "signature": "public boolean hasErrors()",
          "source": "  public boolean hasErrors() {\n    return hasHaltingErrors();\n  }",
          "is_abstract": false,
          "impls": [
            [
              "Compiler.java",
              "  public boolean hasErrors() {\n    return hasHaltingErrors();\n  }"
            ],
            [
              "ScopedAliases.java",
              "    boolean hasErrors() {\n      return hasErrors;\n    }"
            ]
          ]
        }
      ],
      "field_siblings": [
        {
          "name": "getState",
          "signature": "public IntermediateState getState()",
          "shared_fields": [
            "externs",
            "externsRoot",
            "inputs",
            "jsRoot",
            "modules"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "Returns the current internal state, excluding the input files and modules."
        },
        {
          "name": "setState",
          "signature": "public void setState(IntermediateState state)",
          "shared_fields": [
            "externs",
            "externsRoot",
            "inputs",
            "jsRoot",
            "modules"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "Sets the internal state to the capture given.  Note that this assumes that the input files are already set up."
        },
        {
          "name": "initModules",
          "signature": "public void initModules(List<T> externs, List<JSModule> modules, CompilerOptions options)",
          "shared_fields": [
            "externs",
            "inputs",
            "moduleGraph",
            "modules"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": "Initializes the instance state needed for a compile job if the sources are in modules."
        },
        {
          "name": "check",
          "signature": "public void check()",
          "shared_fields": [
            "externsRoot",
            "jsRoot",
            "tracker"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": null
        },
        {
          "name": "optimize",
          "signature": "public void optimize()",
          "shared_fields": [
            "externsRoot",
            "jsRoot",
            "tracker"
          ],
          "is_constructor": false,
          "source": null,
          "javadoc": null
        }
      ]
    }
  ],
  "package": "com.google.javascript.jscomp",
  "root_cause_reachable": [
    "Node.detachChildren",
    "IR.block()",
    "Node.setIsSyntheticBlock",
    "IR.block(Node, Node)",
    "PerformanceTracker.<init>",
    "PerformanceTracker.getCodeChangeHandler",
    "Compiler.addChangeHandler",
    "Compiler.newTracer",
    "Compiler.hasErrors",
    "Node.addChildToBack",
    "Compiler.processAMDAndCommonJSModules",
    "Compiler.hoistExterns",
    "DependencyOptions.needsManagement",
    "Compiler.getTypeRegistry",
    "JSTypeRegistry.forwardDeclareType",
    "JSError.make",
    "Compiler.report",
    "Compiler.hoistNoCompileFiles",
    "Compiler.repartitionInputs",
    "Compiler.runSanityCheck",
    "SourceInformationAnnotator.<init>",
    "NodeTraversal.traverse",
    "Compiler.stopTracer",
    "IR.mayBeStatement",
    "Node.putBooleanProp",
    "CodeChangeHandler>.add",
    "RecentChange.hasCodeChanged",
    "PerformanceTracker.recordPassStart",
    "Tracer.<init>",
    "Compiler.hasHaltingErrors",
    "TransformAMDToCJSModule.<init>",
    "TransformAMDToCJSModule.process",
    "ProcessCommonJSModules.<init>",
    "ProcessCommonJSModules.process",
    "ProcessCommonJSModules.getModule",
    "JSModule.getName",
    "Compiler.put",
    "Compiler.values",
    "Compiler.isEmpty",
    "JSModuleGraph.<init>",
    "Compiler.get",
    "JSModuleGraph.manageDependencies",
    "Compiler.add",
    "Compiler.getJSDocInfo",
    "Compiler.isExterns",
    "Compiler.remove",
    "CompilerInput>.add",
    "JSTypeRegistry.<init>",
    "JSError.getDefaultLevel",
    "WarningsGuard.level",
    "Compiler.getOptions",
    "ErrorHandler.report",
    "ErrorManager.report",
    "Compiler.isNoCompile",
    "Compiler.fillEmptyModules",
    "Compiler.rebuildInputsFromModules",
    "PassFactory.create",
    "CompilerPass.process",
    "Tracer.stop",
    "PerformanceTracker.recordPassStop",
    "RecentChange.reset",
    "Compiler.isIdeMode",
    "Compiler.getErrorCount",
    "JSModule>.size",
    "JSModuleGraph.size",
    "JSModuleGraph.add",
    "JSModuleGraph.get",
    "SortedDependencies.<init>",
    "DependencyOptions.shouldPruneDependencies",
    "DependencyOptions.shouldDropMoochers",
    "SortedDependencies.getInputsWithoutProvides",
    "JSModuleGraph.addAll",
    "DependencyOptions.getEntryPoints",
    "SortedDependencies.getInputProviding",
    "SortedDependencies.maybeGetInputProviding",
    "DependencyOptions.shouldSortDependencies",
    "SortedDependencies.getDependenciesOf",
    "JSModuleGraph.put",
    "JSModuleGraph.getAllModules",
    "JSModuleGraph.removeAll",
    "JSModuleGraph.keySet",
    "JSModuleGraph.getDeepestCommonDependencyInclusive",
    "JSModuleGraph.getInputs",
    "JSTypeRegistry.resetForTypeCheck",
    "Compiler.createFillFileName",
    "SourceFile.fromCode",
    "Compiler.getAllInputsFromModules",
    "Compiler.initInputsByIdMap",
    "PerformanceTracker.estimateCodeSize",
    "Stats.<init>",
    "PerformanceTracker.updateStats"
  ],
  "neighbourhood_notes": [],
  "source_imports": [
    "import com.google.common.annotations.VisibleForTesting;",
    "import com.google.common.base.Charsets;",
    "import com.google.common.base.Preconditions;",
    "import com.google.common.base.Supplier;",
    "import com.google.common.base.Throwables;",
    "import com.google.common.collect.Lists;",
    "import com.google.common.collect.Maps;",
    "import com.google.common.io.CharStreams;",
    "import com.google.javascript.jscomp.CompilerOptions.DevMode;",
    "import com.google.javascript.jscomp.CompilerOptions.LanguageMode;",
    "import com.google.javascript.jscomp.ReferenceCollectingCallback.ReferenceCollection;",
    "import com.google.javascript.jscomp.Scope.Var;",
    "import com.google.javascript.jscomp.deps.SortedDependencies.CircularDependencyException;",
    "import com.google.javascript.jscomp.deps.SortedDependencies.MissingProvideException;",
    "import com.google.javascript.jscomp.parsing.Config;",
    "import com.google.javascript.jscomp.parsing.ParserRunner;",
    "import com.google.javascript.jscomp.type.ChainableReverseAbstractInterpreter;",
    "import com.google.javascript.jscomp.type.ClosureReverseAbstractInterpreter;",
    "import com.google.javascript.jscomp.type.ReverseAbstractInterpreter;",
    "import com.google.javascript.jscomp.type.SemanticReverseAbstractInterpreter;",
    "import com.google.javascript.rhino.IR;",
    "import com.google.javascript.rhino.InputId;",
    "import com.google.javascript.rhino.JSDocInfo;",
    "import com.google.javascript.rhino.Node;",
    "import com.google.javascript.rhino.Token;",
    "import com.google.javascript.rhino.head.ErrorReporter;",
    "import com.google.javascript.rhino.jstype.JSTypeRegistry;",
    "import java.io.IOException;",
    "import java.io.InputStreamReader;",
    "import java.io.PrintStream;",
    "import java.io.Serializable;",
    "import java.nio.charset.Charset;",
    "import java.util.Collections;",
    "import java.util.HashMap;",
    "import java.util.List;",
    "import java.util.Map;",
    "import java.util.ResourceBundle;",
    "import java.util.Set;",
    "import java.util.concurrent.Callable;",
    "import java.util.logging.Level;",
    "import java.util.logging.Logger;",
    "import java.util.regex.Matcher;"
  ]
}
```

</details>

---
