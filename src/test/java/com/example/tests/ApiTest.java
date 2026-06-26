package com.example.tests;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Playwright;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Feature("API")
class ApiTest {

    static Playwright playwright;
    static APIRequestContext request;

    @BeforeAll
    static void createRequestContext() {
        playwright = Playwright.create();
        request = playwright.request().newContext();
    }

    @AfterAll
    static void closeRequestContext() {
        request.dispose();
        playwright.close();
    }

    @Test
    void getPostReturnsExpectedFields() {
        APIResponse response = request.get("https://jsonplaceholder.typicode.com/posts/1");
        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();

        assertEquals(200, response.status());
        assertEquals(1, body.get("id").getAsInt());
    }

    @Test
    void postCreatesNewResource() {
        APIResponse response = request.post("https://jsonplaceholder.typicode.com/posts",
                com.microsoft.playwright.options.RequestOptions.create()
                        .setData("{\"title\":\"foo\",\"body\":\"bar\",\"userId\":1}")
                        .setHeader("Content-Type", "application/json"));
        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();

        assertEquals(201, response.status());
        assertTrue(body.has("id"));
    }

    @Test
    void getPostWithNonExistentIdReturns404() {
        APIResponse response = request.get("https://jsonplaceholder.typicode.com/posts/99999");
        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();

        assertEquals(404, response.status());
        assertTrue(body.entrySet().isEmpty());
    }

    @Test
    void deletePostReturns200() {
        APIResponse response = request.delete("https://jsonplaceholder.typicode.com/posts/1");
        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();

        assertEquals(200, response.status());
        assertTrue(body.entrySet().isEmpty());
    }

    @Test
    void putPostReplacesResourceAndEchoesUpdatedFields() {
        APIResponse response = request.put("https://jsonplaceholder.typicode.com/posts/1",
                com.microsoft.playwright.options.RequestOptions.create()
                        .setData("{\"id\":1,\"title\":\"updated title\",\"body\":\"updated body\",\"userId\":2}")
                        .setHeader("Content-Type", "application/json"));
        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();

        assertEquals(200, response.status());
        assertEquals(1, body.get("id").getAsInt());
        assertEquals("updated title", body.get("title").getAsString());
        assertEquals("updated body", body.get("body").getAsString());
        assertEquals(2, body.get("userId").getAsInt());
    }

    @Test
    void postWithEmptyBodyStillReturns201WithoutValidation() {
        APIResponse response = request.post("https://jsonplaceholder.typicode.com/posts",
                com.microsoft.playwright.options.RequestOptions.create()
                        .setData("{}")
                        .setHeader("Content-Type", "application/json"));
        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();

        assertEquals(201, response.status());
        assertTrue(body.has("id"));
        assertTrue(!body.has("title"));
    }
}
