package com.interview.prep.model;

import java.util.List;
import java.util.Map;

public class EvaluationResult {
    private int score; // 0 - 100
    private double semanticSimilarity; // 0.0 - 1.0 (TF-IDF Cosine Similarity)
    private List<String> matchedKeywords;
    private List<String> missingKeywords;
    private Map<String, Integer> fillerWords; // Filler word -> count
    private int totalFillerCount;
    private String sentiment; // e.g., "Positive", "Neutral", "Uncertain"
    private String tone; // e.g., "Confident & Structured", "Hesitant & Wordy"
    private List<String> suggestions;
    private String feedback;

    public EvaluationResult(int score, double semanticSimilarity, List<String> matchedKeywords,
                            List<String> missingKeywords, Map<String, Integer> fillerWords,
                            int totalFillerCount, String sentiment, String tone,
                            List<String> suggestions, String feedback) {
        this.score = score;
        this.semanticSimilarity = semanticSimilarity;
        this.matchedKeywords = matchedKeywords;
        this.missingKeywords = missingKeywords;
        this.fillerWords = fillerWords;
        this.totalFillerCount = totalFillerCount;
        this.sentiment = sentiment;
        this.tone = tone;
        this.suggestions = suggestions;
        this.feedback = feedback;
    }

    // Getters
    public int getScore() {
        return score;
    }

    public double getSemanticSimilarity() {
        return semanticSimilarity;
    }

    public List<String> getMatchedKeywords() {
        return matchedKeywords;
    }

    public List<String> getMissingKeywords() {
        return missingKeywords;
    }

    public Map<String, Integer> getFillerWords() {
        return fillerWords;
    }

    public int getTotalFillerCount() {
        return totalFillerCount;
    }

    public String getSentiment() {
        return sentiment;
    }

    public String getTone() {
        return tone;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public String getFeedback() {
        return feedback;
    }
}
