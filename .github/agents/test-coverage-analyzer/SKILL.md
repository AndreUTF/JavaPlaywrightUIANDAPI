---
name: test-coverage-analyzer
description: >
  Analyzes a Java + Maven + JUnit 5 test suite to measure test coverage and surface concrete,
  actionable gaps. Wires up JaCoCo for real line/branch coverage when application code
  (src/main/java) exists, and separately scans Playwright-style UI/API test methods to map which
  HTTP endpoints, methods, pages, and user flows are actually exercised versus only partially or
  not tested at all (missing error-path/4xx tests, untested CRUD verbs, UI flows never clicked
  through, assertions that only check a status code). Produces a coverage report plus a
  prioritized list of concrete new test cases to write. Use this whenever the user asks about test
  coverage, code coverage, a coverage report, JaCoCo, what's untested, test gaps, what to test
  next, or wants help expanding a Java/Maven test suite -- even if they don't mention JaCoCo or
  "coverage" by name, e.g. "are we missing any tests", "what else should I test here", "is this
  API test suite thorough enough".
---

# Test Coverage Analyzer

## Why this skill has two tracks

Code coverage tools like JaCoCo measure which lines/branches of *your own* compiled source
(`src/main/java`) get executed by tests. That's the right metric when there's application logic to
instrument. But plenty of Java test suites -- especially Playwright UI/API suites -- have no
`src/main/java` at all; the "system under test" is an external site or API, not code in the repo.
In that case JaCoCo has nothing to instrument and will silently skip ("Skipping JaCoCo execution
due to missing classes directory") rather than report 0% -- a real report file just won't appear.

So treat this as two complementary tracks and run whichever ones apply:

1. **Code coverage (JaCoCo)** -- meaningful only when `src/main/java` has classes that get loaded
   during the test run.
2. **Scenario coverage** -- always meaningful for UI/API tests: which endpoints/HTTP methods/pages/
   flows are actually exercised, versus the gaps an experienced tester would expect to see covered.

Don't report a misleadingly tiny JaCoCo percentage as "the" coverage number when there's no
application code -- that number is an artifact, not a finding. Lead with scenario coverage in that
situation instead.

## Step 1: Detect the project

Find the relevant pieces before assuming a layout -- don't hardcode paths from any one project:

- Locate `pom.xml` (Maven project root). If there's no `pom.xml` anywhere, this skill's tooling
  (JaCoCo via Maven, the bundled scripts) doesn't apply -- say so rather than forcing it.
- Locate the test source root, typically `src/test/java`, by checking `pom.xml` for a custom
  `<testSourceDirectory>` first, falling back to the Maven default.
- Check whether `src/main/java` exists and contains any `.java` files. This decides whether Track 1
  is in play.

## Step 2: Code coverage (Track 1) -- only if src/main/java has classes

Check `pom.xml` for an existing `jacoco-maven-plugin` entry. If missing, add it with the Edit tool
(don't blindly script-inject XML -- pom.xml structures vary, read the file first and place it
sensibly inside the existing `<build><plugins>` block):

```xml
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <version>0.8.12</version>
  <executions>
    <execution>
      <goals><goal>prepare-agent</goal></goals>
    </execution>
    <execution>
      <id>report</id>
      <phase>test</phase>
      <goals><goal>report</goal></goals>
    </execution>
  </executions>
</plugin>
```

Check Maven Central for a newer `jacoco-maven-plugin` version before pinning 0.8.12 if the project
runs on a very recent JDK (25+) -- 0.8.12 prints noisy `IllegalArgumentException: Unsupported class
file major version 69` stack traces while instrumenting unrelated JDK-internal classes (e.g.
`java/sql/Timestamp`, locale data) on JDK 25. This is cosmetic and does not fail the build or
corrupt the report -- confirmed by running it end-to-end -- but a newer plugin release may have
quieted it, so it's worth a quick check.

Run `mvn test` (the `report` execution is bound to the `test` phase above, so one command does
both). Then summarize the result with the bundled parser rather than reading raw XML by hand:

```
python scripts/parse_jacoco_report.py [path/to/jacoco.xml]
```

Default path is `target/site/jacoco/jacoco.xml`. The script prints one of:
- `NO_REPORT: ...` -- no report file exists. The message explains the likely cause (no
  `src/main/java`, or tests haven't run with the plugin attached yet).
- `NO_CLASSES: ...` -- report exists but lists no instrumented classes.
- An overall line/branch coverage summary plus a per-class table sorted worst-first, with missed
  line/branch counts.

For each class with non-trivial missed lines/branches, open the source file and look at *which*
lines are missed -- jacoco.xml's `<line>` elements give exact line numbers. A missed branch
inside an `if` almost always means one side of a condition (e.g. the error path) was never
exercised by any test. That's your most concrete, highest-confidence test suggestion: name the
exact method and the exact condition that needs a test pointed at it.

## Step 3: Scenario coverage (Track 2) -- always run this for UI/API suites

Scan the test sources for Playwright-style calls and assertions:

```
python scripts/scan_test_scenarios.py [src/test/java]
```

This prints JSON: one entry per `@Test` method, with `kind` (API/UI/unknown), the HTTP calls or UI
navigate/click/fill/etc. actions detected inside it, and the assertion lines found in its body.

This is a regex heuristic, not a real Java parser -- it will miss calls hidden behind a shared
helper method (e.g. a test that calls `login()` instead of inlining `page.fill(...)`), and it
can't tell you about a URL built from a variable rather than a string literal. Treat its output as
raw material for your own reasoning, not a complete inventory. If a class looks suspiciously
empty in the scan output, open the file and check by eye before concluding it's untested.

With that raw material in hand, reason about gaps the way an experienced test engineer would:

- **API tests**: group calls by resource/endpoint. For each resource that has a GET, is there also
  a POST/PUT/PATCH/DELETE if the API supports them? Is there a test for a 404 (invalid ID), a 4xx
  (bad input), or only the happy-path 200/201? Do assertions check response *body* fields, or only
  the status code -- a status-code-only assertion is a weak test worth flagging even if the
  endpoint is technically "tested."
- **UI tests**: what flows exist beyond the ones exercised? Form validation and error states,
  navigation via menus/links not yet clicked, behavior on a 404/not-found page, search if the site
  has it, mobile/narrow-viewport rendering. Use judgment about what's plausible and valuable for
  *this* site/app, not a generic checklist applied blindly -- a marketing site and a SaaS dashboard
  warrant different flows.

## Step 4: Report

Present findings in this structure (skip a section entirely if it doesn't apply, e.g. no Track 1
section when there's no application code -- say one sentence why instead of an empty table):

```
# Test Coverage Report -- <project name>

## Code coverage (JaCoCo)
<Overall %, worst-covered classes, or a one-line note on why this section is N/A>

## API endpoint coverage
| Resource/Endpoint | Methods tested | Gaps |
|---|---|---|

## UI flow coverage
| Flow/Page | Tested? | Existing test | Gaps |
|---|---|---|---|

## Suggested new test cases
1. **<proposed test name>** (API|UI|unit) -- what it would cover, why it matters, rough
   assertion sketch
2. ...
```

Keep the suggested test cases concrete and prioritized (most valuable/highest-risk gaps first),
not an exhaustive wishlist. Each one should be specific enough that the user could hand it to
someone and have them write the test without guessing -- name the method/endpoint/flow, not just
"add more error handling tests."

Unless the user has asked for stubs to be scaffolded, stop at the report -- don't write test files
unprompted. If they like a suggestion and ask you to implement it, that's a natural follow-up, not
part of this skill's default output.
