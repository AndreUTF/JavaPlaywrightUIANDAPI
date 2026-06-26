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

    @Test
    void siteSearchReturnsResultsAndNavigatesToDocsPage() {
        Page page = browser.newPage();
        ScreenshotOnFailureExtension.attachPage(page);
        page.navigate("https://playwright.dev");

        page.click("button.DocSearch-Button");
        page.waitForSelector(".DocSearch-Modal");
        page.fill(".DocSearch-Modal input", "fixtures");
        page.waitForSelector("li.DocSearch-Hit");

        assertTrue(page.locator("li.DocSearch-Hit").count() > 0);

        page.click("li.DocSearch-Hit >> nth=0");
        page.waitForURL("**/docs/**");

        assertTrue(page.url().contains("/docs/"));
        page.close();
    }

    @Test
    void navigatingToUnknownPathShowsNotFoundPage() {
        Page page = browser.newPage();
        ScreenshotOnFailureExtension.attachPage(page);
        page.navigate("https://playwright.dev/this-page-does-not-exist");
        page.waitForSelector("text=We could not find what you were looking for");

        assertTrue(page.title().contains("Page Not Found"));
        assertTrue(page.url().contains("/this-page-does-not-exist"));
        assertTrue(page.innerText("body").contains("We could not find what you were looking for"));
        page.close();
    }
}
