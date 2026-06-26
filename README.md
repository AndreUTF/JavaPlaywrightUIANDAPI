# JavaPlaywrightUIANDAPI

Simple Java + Maven project with Playwright tests covering both UI and API.

## Stack

- Java 17, Maven
- [Playwright for Java](https://playwright.dev/java/) for browser (UI) automation and HTTP (API) requests
- JUnit 5

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
