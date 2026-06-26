package com.example.tests;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Feature("UI")
@ExtendWith(ScreenshotOnFailureExtension.class)
class UiTest {

    static Playwright playwright;
    static Browser browser;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    }

    @AfterAll
    static void closeBrowser() {
        browser.close();
        playwright.close();
    }

    @Test
    void homePageHasExpectedTitle() {
        Page page = browser.newPage();
        ScreenshotOnFailureExtension.attachPage(page);
        page.navigate("https://playwright.dev");

        assertTrue(page.title().contains("Playwright"));
        page.close();
    }

    @Test
    void searchNavigatesToDocsPage() {
        Page page = browser.newPage();
        ScreenshotOnFailureExtension.attachPage(page);
        page.navigate("https://playwright.dev");
        page.click("text=Get started");

        assertTrue(page.url().contains("/docs/"));
        page.close();
    }
}
