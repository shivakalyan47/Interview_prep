package com.interview.prep.model;

import java.util.List;

public class Question {
    private int id;
    private String text;
    private String type; // "HR" or "Technical"
    private String category; // e.g., "Behavioral", "Data Structures", "Java Core", "System Design"
    private List<String> keywords; // Expected technical/HR concepts
    private String idealAnswer; // High-quality baseline reference answer

    public Question(int id, String text, String type, String category, List<String> keywords, String idealAnswer) {
        this.id = id;
        this.text = text;
        this.type = type;
        this.category = category;
        this.keywords = keywords;
        this.idealAnswer = idealAnswer;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public String getIdealAnswer() {
        return idealAnswer;
    }

    public void setIdealAnswer(String idealAnswer) {
        this.idealAnswer = idealAnswer;
    }
}
