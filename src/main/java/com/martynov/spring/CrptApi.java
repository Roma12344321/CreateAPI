package com.martynov.spring;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private final Semaphore rateLimiter;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.rateLimiter = new Semaphore(requestLimit);
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();

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

    public void createDocument(Document document) {
        try {
            rateLimiter.acquire();
            String requestBody = objectMapper.writeValueAsString(document);

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
        } catch (JsonProcessingException e) {
            System.out.println("Error serializing document to JSON: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            rateLimiter.release();
        }
    }

    public static class Document {
        public String description;
        public String doc_id;
        public String doc_status;
        public String doc_type;
        public boolean importRequest;
        public String owner_inn;
        public String participant_inn;
        public String producer_inn;
        public String production_date;
        public String production_type;
        public List<Product> products;
        public String reg_date;
        public String reg_number;

        public Document(String description, String doc_id, String doc_status, String doc_type, boolean importRequest, String owner_inn, String participant_inn, String producer_inn, String production_date, String production_type, List<Product> products, String reg_date, String reg_number) {
            this.description = description;
            this.doc_id = doc_id;
            this.doc_status = doc_status;
            this.doc_type = doc_type;
            this.importRequest = importRequest;
            this.owner_inn = owner_inn;
            this.participant_inn = participant_inn;
            this.producer_inn = producer_inn;
            this.production_date = production_date;
            this.production_type = production_type;
            this.products = products;
            this.reg_date = reg_date;
            this.reg_number = reg_number;
        }
        public Document(){}
    }

    public static class Product {
        public String certificate_document;
        public String certificate_document_date;
        public String certificate_document_number;
        public String owner_inn;
        public String producer_inn;
        public String production_date;
        public String tnved_code;
        public String uit_code;
        public String uitu_code;
        public Product(){}

        public Product(String certificate_document, String certificate_document_date, String certificate_document_number, String owner_inn, String producer_inn, String production_date, String tnved_code, String uit_code, String uitu_code) {
            this.certificate_document = certificate_document;
            this.certificate_document_date = certificate_document_date;
            this.certificate_document_number = certificate_document_number;
            this.owner_inn = owner_inn;
            this.producer_inn = producer_inn;
            this.production_date = production_date;
            this.tnved_code = tnved_code;
            this.uit_code = uit_code;
            this.uitu_code = uitu_code;
        }
    }

    public static void main(String[] args) {
        CrptApi api = new CrptApi(TimeUnit.SECONDS, 10);
        Document document = new Document();
        api.createDocument(document);
    }
}
