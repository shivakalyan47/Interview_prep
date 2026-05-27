package com.interview.prep;

import com.interview.prep.controller.InterviewController;
import com.interview.prep.service.NLPEngine;
import com.interview.prep.service.QuestionService;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class InterviewPrepApp {
    private static final int PORT = 8080;

    public static void main(String[] args) {
        try {
            System.out.println("=========================================================");
            System.out.println("🚀 Bootstrapping AI-Based Interview Preparation System...");
            System.out.println("=========================================================");
            
            // 1. Initialize core services and controllers
            QuestionService questionService = new QuestionService();
            NLPEngine nlpEngine = new NLPEngine();
            InterviewController controller = new InterviewController(questionService, nlpEngine);

            // 2. Create and bind HttpServer
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            
            // Route for serving frontend pages (HTML, CSS, JS)
            server.createContext("/", controller.getStaticResourceHandler());
            
            // Route for retrieving questions / adding custom questions
            server.createContext("/api/questions", controller.getQuestionsHandler());
            
            // Route for analyzing user answers
            server.createContext("/api/evaluate", controller.getEvaluateHandler());

            // 3. Set multi-threaded executor for handling concurrent requests
            server.setExecutor(Executors.newCachedThreadPool());

            // 4. Start the server
            server.start();

            System.out.println("\n✅ Embedded Server successfully started on port " + PORT + "!");
            System.out.println("👉 Please open your browser and navigate to: http://localhost:" + PORT);
            System.out.println("💬 Press Ctrl+C in the terminal to stop the server.\n");
            System.out.println("=========================================================");
        } catch (Exception e) {
            System.err.println("❌ ERROR: Failed to start the embedded server!");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
