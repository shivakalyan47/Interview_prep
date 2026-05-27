package com.interview.prep.service;

import com.interview.prep.model.Question;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class QuestionService {
    private final List<Question> questions = new ArrayList<>();

    public QuestionService() {
        populateQuestions();
    }

    private void populateQuestions() {
        // --- Question 1 (HR - Behavioral) ---
        questions.add(new Question(
            1,
            "Tell me about yourself and walk me through your background.",
            "HR",
            "Behavioral",
            Arrays.asList("experience", "passionate", "skills", "background", "project", "degree", "solved", "team"),
            "Sure! I am a software engineering graduate with strong foundations in full-stack web development and core Java. " +
            "During my academic years, I spearheaded several key projects, including a microservices-based web application that optimized " +
            "data loading times. I am highly passionate about coding clean, testable logic and collaborating with cross-functional teams " +
            "to build products that solve real-world problems. In my next role, I am eager to apply my analytical skills and grow " +
            "as a software engineer in an agile environment."
        ));

        // --- Question 2 (HR - Conflict Resolution) ---
        questions.add(new Question(
            2,
            "Describe a time when you had a disagreement with a team member. How did you resolve it?",
            "HR",
            "Behavioral",
            Arrays.asList("disagreement", "perspective", "compromise", "communication", "listen", "resolve", "collaborated", "result"),
            "In a recent group project, we had a major disagreement regarding our database schema design. A team member wanted " +
            "to use a NoSQL database for rapid setup, while I advocated for a relational database due to complex transactional requirements. " +
            "To resolve this, I scheduled a brief call. I actively listened to their perspective and then laid out a clean comparison " +
            "of both designs based on project constraints. We reached a compromise: we used a SQL database for core transactions, " +
            "and utilized a NoSQL cache for high-volume logs. This open communication helped us deliver the project successfully on time."
        ));

        // --- Question 3 (Technical - Data Structures) ---
        questions.add(new Question(
            3,
            "Explain how a HashMap works under the hood in Java.",
            "Technical",
            "Data Structures",
            Arrays.asList("hashcode", "equals", "collision", "bucket", "index", "linked list", "treeify", "O(1)", "key", "value"),
            "In Java, a HashMap works on the principle of hashing. It uses a bucket array internally to store key-value pairs. " +
            "When we call put(), the HashMap invokes the hashCode() method on the key to calculate a hash value, which is then mapped to a bucket index. " +
            "If two different keys map to the same index, a collision occurs. In older Java versions, collisions were resolved using a Linked List. " +
            "In Java 8, if a bucket size exceeds a threshold of 8, it is treeified into a balanced Red-Black Tree, improving collision lookup from O(N) to O(log N). " +
            "When fetching elements via get(), the hashCode() locates the bucket, and the equals() method is used to pinpoint the exact key-value match."
        ));

        // --- Question 4 (Technical - Software Engineering Architecture) ---
        questions.add(new Question(
            4,
            "What is the difference between SQL and NoSQL databases, and when would you use each?",
            "Technical",
            "Databases",
            Arrays.asList("relational", "schema", "scaling", "join", "structured", "document", "horizontal", "vertical", "acid"),
            "SQL databases are relational and use structured, pre-defined schemas. They scale vertically by upgrading hardware and " +
            "support complex JOIN operations and ACID transactions, making them ideal for systems requiring high integrity like financial transactions. " +
            "NoSQL databases are non-relational, schema-less, and scale horizontally across distributed clusters. They store data as documents, " +
            "key-value pairs, or graphs. NoSQL is preferred for high-volume, unstructured big data, real-time analytics, or rapid development " +
            "where schemas evolve quickly."
        ));

        // --- Question 5 (Technical - Java OOP / Design Patterns) ---
        questions.add(new Question(
            5,
            "What is a Singleton design pattern? How do you implement a thread-safe Singleton in Java?",
            "Technical",
            "Design Patterns",
            Arrays.asList("single instance", "private constructor", "global access", "thread safe", "synchronized", "double checked locking", "volatile"),
            "The Singleton design pattern ensures a class has only one single instance and provides a global point of access to it. " +
            "To implement a thread-safe Singleton, we must define a private constructor to prevent direct instantiation, a static reference " +
            "variable, and a public static access method. To make it thread-safe and efficient, we use double-checked locking inside " +
            "the access method with a synchronized block, and declare the instance variable as volatile. This prevents multiple threads " +
            "from creating duplicate instances simultaneously while avoiding heavy synchronization overhead on subsequent calls."
        ));

        // --- Question 6 (Technical - Web Services) ---
        questions.add(new Question(
            6,
            "What is RESTful API? Explain the core HTTP methods and status codes.",
            "Technical",
            "Web Architecture",
            Arrays.asList("stateless", "http methods", "resource", "get", "post", "put", "delete", "status codes", "200 OK", "404 not found", "json"),
            "A RESTful API is a stateless web service architecture that allows communication using standard HTTP protocols. " +
            "It centers around resources identified by URIs. The core HTTP methods are GET to retrieve data, POST to create resources, " +
            "PUT to update existing resources, and DELETE to remove them. Standard HTTP status codes indicate outcomes: 200 OK for success, " +
            "201 Created for successful creation, 400 Bad Request for client errors, 401 Unauthorized, 404 Not Found if resources don't exist, " +
            "and 500 Internal Server Error for server issues. Data is typically exchanged in JSON format."
        ));
    }

    public List<Question> getAllQuestions() {
        return questions;
    }

    public Optional<Question> getQuestionById(int id) {
        return questions.stream().filter(q -> q.getId() == id).findFirst();
    }
    
    public void addCustomQuestion(String text, String type, String category, List<String> keywords, String idealAnswer) {
        int nextId = questions.stream().mapToInt(Question::getId).max().orElse(0) + 1;
        questions.add(new Question(nextId, text, type, category, keywords, idealAnswer));
    }
}
