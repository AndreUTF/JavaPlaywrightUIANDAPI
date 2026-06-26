# JavaPlaywrightUIANDAPI

Simple Java + Maven project with Playwright tests covering both UI and API.

## Stack

- Java 17, Maven
- [Playwright for Java](https://playwright.dev/java/) for browser (UI) automation and HTTP (API) requests
- JUnit 5
- [Allure](https://allurereport.org/) for HTML test reports, with automatic screenshot attachments on UI test failures

## Setup

Install the Chromium browser binary used by the UI tests (one-time):

```sh
mvn exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium" -Dexec.classpathScope=test
```

## Run tests

```sh
mvn test
```

## Structure

- [src/test/java/com/example/tests/UiTest.java](src/test/java/com/example/tests/UiTest.java) — launches headless Chromium and verifies page title/navigation on playwright.dev
- [src/test/java/com/example/tests/ApiTest.java](src/test/java/com/example/tests/ApiTest.java) — sends GET/POST requests to the public JSONPlaceholder API and checks status codes and response bodies
- [src/test/java/com/example/tests/ScreenshotOnFailureExtension.java](src/test/java/com/example/tests/ScreenshotOnFailureExtension.java) — JUnit 5 extension that attaches a screenshot to the Allure report whenever a UI test fails

## Test reports (Allure)

Raw results are written to `target/allure-results` on every `mvn test` run. To view them as an HTML report locally:

```sh
mvn io.qameta.allure:allure-maven:2.15.2:serve
```

In CI ([.github/workflows/tests.yml](.github/workflows/tests.yml)), the report is built and published to the `gh-pages` branch on every push to `master`, with history carried over between runs. **One-time setup**: enable GitHub Pages in the repo settings (Settings → Pages → Source: deploy from branch `gh-pages`) so the published report is viewable.
