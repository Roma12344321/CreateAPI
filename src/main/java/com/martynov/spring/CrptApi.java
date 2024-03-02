package com.martynov.spring;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private final Semaphore rateLimiter;
    private final HttpClient httpClient;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.rateLimiter = new Semaphore(requestLimit);
        this.httpClient = HttpClient.newHttpClient();
        new Thread(() -> {
            while (true) {
                try {
                    timeUnit.sleep(1);
                    rateLimiter.release(requestLimit - rateLimiter.availablePermits());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }

    public void createDocument(Document document, String signature) {
        try {
            rateLimiter.acquire();
            String requestBody = document.toJson() + ", \"signature\": \"" + signature + "\"";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                    .header("Content-Type", "application/json")
                    .POST(BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());

            System.out.println("Response status code: " + response.statusCode());
            System.out.println("Response body: " + response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            rateLimiter.release();
        }
    }

    public static class Document {
        private final String description;
        public Document(String description) {
            this.description = description;
        }
        public String toJson() {
            return String.format("{\"description\": \"%s\"}", this.description);
        }
    }

    public static void main(String[] args) {
        CrptApi api = new CrptApi(TimeUnit.SECONDS, 10);
        Document document = new Document("Тестовое описание");
        api.createDocument(document, "test_signature");
    }
}
