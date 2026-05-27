package com.interview.prep.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.interview.prep.model.EvaluationResult;
import com.interview.prep.model.Question;
import com.interview.prep.service.NLPEngine;
import com.interview.prep.service.QuestionService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class InterviewController {

    private final QuestionService questionService;
    private final NLPEngine nlpEngine;
    private final Gson gson;

    public InterviewController(QuestionService questionService, NLPEngine nlpEngine) {
        this.questionService = questionService;
        this.nlpEngine = nlpEngine;
        this.gson = new Gson();
    }

    /**
     * Handler for serving static frontend resources (HTML, CSS, JS).
     */
    public HttpHandler getStaticResourceHandler() {
        return exchange -> {
            String path = exchange.getRequestURI().getPath();
            
            // Normalize default path to index.html
            if (path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            // Define physical location path for development vs built jar
            // We search in src/main/resources/static first, then try classloader resources
            byte[] fileBytes = null;
            String contentType = getContentType(path);

            try {
                // 1. Try reading directly from filesystem (useful for development hot-reloads)
                Path filePath = Paths.get("src/main/resources/static" + path);
                if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                    fileBytes = Files.readAllBytes(filePath);
                } else {
                    // 2. Fallback to resource classpath stream
                    InputStream is = getClass().getResourceAsStream("/static" + path);
                    if (is != null) {
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            bos.write(buffer, 0, bytesRead);
                        }
                        fileBytes = bos.toByteArray();
                    }
                }
            } catch (Exception e) {
                System.err.println("Error reading static file: " + path + " - " + e.getMessage());
            }

            if (fileBytes != null) {
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, fileBytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(fileBytes);
                os.close();
            } else {
                // File not found response
                String responseText = "404 - Static asset not found: " + path;
                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                exchange.sendResponseHeaders(404, responseText.length());
                OutputStream os = exchange.getResponseBody();
                os.write(responseText.getBytes());
                os.close();
            }
        };
    }

    /**
     * Handles API requests to /api/questions.
     * GET: Lists all questions.
     * POST: Adds a new custom question.
     */
    public HttpHandler getQuestionsHandler() {
        return exchange -> {
            String method = exchange.getRequestMethod();
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            
            // Allow CORS for easy testing
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            if (method.equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (method.equalsIgnoreCase("GET")) {
                List<Question> list = questionService.getAllQuestions();
                String json = gson.toJson(list);
                byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(bytes);
                os.close();
            } 
            else if (method.equalsIgnoreCase("POST")) {
                try {
                    String body = getRequestBody(exchange);
                    JsonObject jsonObject = JsonParser.parseString(body).getAsJsonObject();
                    
                    String text = jsonObject.get("text").getAsString();
                    String type = jsonObject.get("type").getAsString();
                    String category = jsonObject.get("category").getAsString();
                    String idealAnswer = jsonObject.get("idealAnswer").getAsString();
                    
                    String keywordsStr = jsonObject.get("keywords").getAsString();
                    List<String> keywords = Arrays.stream(keywordsStr.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toList());

                    questionService.addCustomQuestion(text, type, category, keywords, idealAnswer);

                    JsonObject response = new JsonObject();
                    response.addProperty("status", "success");
                    response.addProperty("message", "Custom question added successfully.");
                    
                    byte[] bytes = gson.toJson(response).getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(200, bytes.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(bytes);
                    os.close();
                } catch (Exception e) {
                    sendErrorResponse(exchange, 400, "Invalid JSON structure or missing parameters: " + e.getMessage());
                }
            } 
            else {
                sendErrorResponse(exchange, 405, "Method not allowed. Use GET or POST.");
            }
        };
    }

    /**
     * Handles API requests to /api/evaluate.
     * POST: Analyzes a user answer and returns the scorecard.
     */
    public HttpHandler getEvaluateHandler() {
        return exchange -> {
            String method = exchange.getRequestMethod();
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            
            // Allow CORS
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            if (method.equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (method.equalsIgnoreCase("POST")) {
                try {
                    String body = getRequestBody(exchange);
                    JsonObject jsonObject = JsonParser.parseString(body).getAsJsonObject();
                    
                    int questionId = jsonObject.get("questionId").getAsInt();
                    String userAnswer = jsonObject.get("answer").getAsString();

                    Optional<Question> questionOpt = questionService.getQuestionById(questionId);

                    if (questionOpt.isPresent()) {
                        Question q = questionOpt.get();
                        
                        // Perform NLP evaluation
                        EvaluationResult result = nlpEngine.evaluateAnswer(
                            userAnswer,
                            q.getIdealAnswer(),
                            q.getKeywords()
                        );

                        String json = gson.toJson(result);
                        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                        exchange.sendResponseHeaders(200, bytes.length);
                        OutputStream os = exchange.getResponseBody();
                        os.write(bytes);
                        os.close();
                    } else {
                        sendErrorResponse(exchange, 404, "Question ID " + questionId + " not found.");
                    }
                } catch (Exception e) {
                    sendErrorResponse(exchange, 400, "Invalid request structure: " + e.getMessage());
                }
            } else {
                sendErrorResponse(exchange, 405, "Method not allowed. Use POST.");
            }
        };
    }

    // Helper functions
    private String getRequestBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        JsonObject errorJson = new JsonObject();
        errorJson.addProperty("status", "error");
        errorJson.addProperty("message", message);
        byte[] bytes = gson.toJson(errorJson).getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private String getContentType(String path) {
        String pathLower = path.toLowerCase();
        if (pathLower.endsWith(".html") || pathLower.endsWith(".htm")) return "text/html; charset=utf-8";
        if (pathLower.endsWith(".css")) return "text/css; charset=utf-8";
        if (pathLower.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (pathLower.endsWith(".json")) return "application/json; charset=utf-8";
        if (pathLower.endsWith(".png")) return "image/png";
        if (pathLower.endsWith(".jpg") || pathLower.endsWith(".jpeg")) return "image/jpeg";
        if (pathLower.endsWith(".svg")) return "image/svg+xml";
        if (pathLower.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }
}
