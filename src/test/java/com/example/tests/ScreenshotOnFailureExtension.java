package com.example.tests;

import com.microsoft.playwright.Page;
import io.qameta.allure.Allure;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import java.io.ByteArrayInputStream;

class ScreenshotOnFailureExtension implements TestWatcher {

    private static final ThreadLocal<Page> CURRENT_PAGE = new ThreadLocal<>();

    static void attachPage(Page page) {
        CURRENT_PAGE.set(page);
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        Page page = CURRENT_PAGE.get();
        if (page == null) {
            return;
        }
        byte[] screenshot = page.screenshot();
        Allure.addAttachment(context.getDisplayName() + " - screenshot", "image/png",
                new ByteArrayInputStream(screenshot), "png");
    }
}
