package com.interview.prep.service;

import com.interview.prep.model.EvaluationResult;
import com.interview.prep.util.StopWords;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NLPEngine {

    // Speech disfluencies / filler words
    private static final List<String> FILLER_WORDS = Arrays.asList(
        "um", "uh", "like", "actually", "basically", "you know", "sort of", "kind of", "well"
    );

    // Lexicon for Tone and Sentiment Analysis
    private static final Set<String> CONFIDENT_WORDS = new HashSet<>(Arrays.asList(
        "lead", "led", "managed", "designed", "built", "implemented", "solved", "created", 
        "spearheaded", "achieved", "successfully", "strong", "optimized", "structured", 
        "collaborated", "delivered", "resolved", "executed", "improved"
    ));

    private static final Set<String> HESITANT_WORDS = new HashSet<>(Arrays.asList(
        "maybe", "probably", "guess", "just", "try", "hope", "think", "perhaps", 
        "sorta", "kinda", "possibly", "somewhat"
    ));

    private static final Set<String> POSITIVE_WORDS = new HashSet<>(Arrays.asList(
        "good", "great", "excellent", "positive", "helpful", "benefit", "growth", 
        "learn", "passionate", "enjoy", "valuable", "opportunity", "excited", "happy"
    ));

    /**
     * Tokenizes a text string into a list of cleaned, lowercase words.
     */
    private static List<String> tokenize(String text) {
        if (text == null) return new ArrayList<>();
        List<String> tokens = new ArrayList<>();
        // Split by non-word characters, preserving words and numbers
        String[] words = text.toLowerCase().split("[^a-zA-Z0-9]+");
        for (String w : words) {
            if (!w.trim().isEmpty()) {
                tokens.add(w.trim());
            }
        }
        return tokens;
    }

    /**
     * Calculates the Term Frequency (TF) map of tokens, excluding stop words.
     */
    private static Map<String, Integer> getTermFrequencies(List<String> tokens) {
        Map<String, Integer> freqMap = new HashMap<>();
        for (String token : tokens) {
            if (!StopWords.isStopWord(token)) {
                freqMap.put(token, freqMap.getOrDefault(token, 0) + 1);
            }
        }
        return freqMap;
    }

    /**
     * Calculates Cosine Similarity between two term-frequency maps.
     */
    private static double calculateCosineSimilarity(Map<String, Integer> userFreq, Map<String, Integer> idealFreq) {
        if (userFreq.isEmpty() || idealFreq.isEmpty()) {
            return 0.0;
        }

        // Get union of all terms
        Set<String> allTerms = new HashSet<>();
        allTerms.addAll(userFreq.keySet());
        allTerms.addAll(idealFreq.keySet());

        double dotProduct = 0.0;
        double normUser = 0.0;
        double normIdeal = 0.0;

        for (String term : allTerms) {
            int uVal = userFreq.getOrDefault(term, 0);
            int iVal = idealFreq.getOrDefault(term, 0);

            dotProduct += uVal * iVal;
            normUser += Math.pow(uVal, 2);
            normIdeal += Math.pow(iVal, 2);
        }

        if (normUser == 0.0 || normIdeal == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normUser) * Math.sqrt(normIdeal));
    }

    /**
     * Evaluates a user answer against the target question.
     */
    public EvaluationResult evaluateAnswer(String userAnswer, String idealAnswer, List<String> expectedKeywords) {
        if (userAnswer == null) userAnswer = "";
        
        List<String> rawTokens = tokenize(userAnswer);
        List<String> idealTokens = tokenize(idealAnswer);

        // 1. Semantic Cosine Similarity (TF-IDF-like match on content words)
        Map<String, Integer> userFreq = getTermFrequencies(rawTokens);
        Map<String, Integer> idealFreq = getTermFrequencies(idealTokens);
        double similarity = calculateCosineSimilarity(userFreq, idealFreq);

        // 2. Keyword Matching (supports substrings to capture plurals/stems, e.g. "thread" matches "threads")
        List<String> matchedKeywords = new ArrayList<>();
        List<String> missingKeywords = new ArrayList<>();
        String answerLower = userAnswer.toLowerCase();
        
        for (String keyword : expectedKeywords) {
            String kwLower = keyword.toLowerCase();
            // Basic regex boundary matching to prevent matching inside unrelated words,
            // while allowing standard endings (s, ed, ing)
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(kwLower) + "(s|ed|ing|es)?\\b");
            Matcher matcher = pattern.matcher(answerLower);
            
            if (matcher.find() || answerLower.contains(kwLower)) {
                matchedKeywords.add(keyword);
            } else {
                missingKeywords.add(keyword);
            }
        }

        double keywordCoverage = expectedKeywords.isEmpty() ? 1.0 : (double) matchedKeywords.size() / expectedKeywords.size();

        // 3. Filler Word & Hesitation Detection
        Map<String, Integer> fillerCounts = new HashMap<>();
        int totalFillerCount = 0;
        
        // Scan for filler words in raw tokens
        for (String token : rawTokens) {
            if (FILLER_WORDS.contains(token)) {
                fillerCounts.put(token, fillerCounts.getOrDefault(token, 0) + 1);
                totalFillerCount++;
            }
        }
        
        // Add double-word fillers like "you know", "sort of", "kind of"
        String rawLower = userAnswer.toLowerCase();
        List<String> multiWordFillers = Arrays.asList("you know", "sort of", "kind of");
        for (String mwf : multiWordFillers) {
            int count = 0;
            int idx = 0;
            while ((idx = rawLower.indexOf(mwf, idx)) != -1) {
                count++;
                idx += mwf.length();
            }
            if (count > 0) {
                fillerCounts.put(mwf, fillerCounts.getOrDefault(mwf, 0) + count);
                totalFillerCount += count;
            }
        }

        // 4. Tone and Sentiment Analysis
        int confidentCount = 0;
        int hesitantCount = 0;
        int positiveCount = 0;

        for (String token : rawTokens) {
            if (CONFIDENT_WORDS.contains(token)) confidentCount++;
            if (HESITANT_WORDS.contains(token)) hesitantCount++;
            if (POSITIVE_WORDS.contains(token)) positiveCount++;
        }

        String sentiment;
        if (positiveCount > hesitantCount && confidentCount > hesitantCount) {
            sentiment = "Positive & Confident";
        } else if (hesitantCount > confidentCount) {
            sentiment = "Hesitant / Tentative";
        } else {
            sentiment = "Objective & Neutral";
        }

        String tone;
        if (confidentCount > 3 && hesitantCount <= 1) {
            tone = "Structured & Authoritative";
        } else if (totalFillerCount > 4 || hesitantCount > 3) {
            tone = "Conversational but Unprepared";
        } else if (rawTokens.size() > 0 && rawTokens.size() < 20) {
            tone = "Brief & Abrupt";
        } else {
            tone = "Clear & Professional";
        }

        // 5. Suggestions and Feedback Generation
        List<String> suggestions = new ArrayList<>();
        
        if (rawTokens.size() < 25) {
            suggestions.add("Your answer is extremely brief (" + rawTokens.size() + " words). In interviews, aim for structured, detailed responses using the **STAR Method** (Situation, Task, Action, Result) to fully explain your points (ideally 80-150 words).");
        } else if (rawTokens.size() > 250) {
            suggestions.add("Your response is quite wordy (" + rawTokens.size() + " words). Try to edit down redundant phrases to keep the interviewer engaged and avoid rambling.");
        }

        if (!missingKeywords.isEmpty()) {
            suggestions.add("Incorporate more industry-specific concepts. Try mentioning: **" + String.join(", ", missingKeywords) + "** to demonstrate your subject matter mastery.");
        }

        if (totalFillerCount > 3) {
            suggestions.add("You relied on filler expressions (like, um, you know) **" + totalFillerCount + " times**. Pause silently for a second when organizing your thoughts rather than filling the silence; this projects executive presence.");
        }

        if (hesitantCount > 2) {
            suggestions.add("Replace tentative phrasing (e.g., 'I think', 'maybe', 'just') with strong action verbs (e.g., 'I implemented', 'I optimized', 'I resolved') to take clear ownership of your achievements.");
        }

        if (similarity < 0.25 && rawTokens.size() >= 25) {
            suggestions.add("The core ideas in your explanation differ significantly from recommended best practices. Structure your answer by clearly naming the technical concepts or sharing a specific situational problem.");
        }

        // Compile main feedback statement
        String feedback;
        if (similarity >= 0.5 && keywordCoverage >= 0.7 && totalFillerCount <= 2) {
            feedback = "Excellent response! You demonstrated strong technical command, covered the essential concepts, and spoke with excellent clarity and fluency. Keep up this high standard.";
        } else if (similarity >= 0.3 && keywordCoverage >= 0.4) {
            feedback = "Good foundation. You understand the core principles, but your answer could be structured more cleanly. Try to incorporate the missing keywords and reduce hesitations to sound more authoritative.";
        } else {
            feedback = "This area needs practice. Focus on structuring your answer beforehand, learning the key technical keywords, and practicing speaking without filler words.";
        }

        // 6. Score Calculation
        // Base score elements
        double semanticWeight = similarity * 40.0;       // Up to 40 pts
        double keywordWeight = keywordCoverage * 40.0;    // Up to 40 pts
        
        double tonePoints = 20.0;                         // Up to 20 pts
        tonePoints -= (hesitantCount * 3.0);
        tonePoints += (confidentCount * 2.0);
        tonePoints = Math.max(0, Math.min(20.0, tonePoints));

        double baseScore = semanticWeight + keywordWeight + tonePoints;

        // Apply penalties
        double penalty = 0.0;
        
        // Filler words penalty
        penalty += (totalFillerCount * 3.0); // -3 pts per filler
        
        // Word count penalty
        if (rawTokens.size() < 15) {
            penalty += 35.0; // Severe penalty for ultra-short answers
        } else if (rawTokens.size() < 40) {
            penalty += 15.0; // Moderate penalty for brief answers
        }

        int finalScore = (int) Math.round(baseScore - penalty);
        finalScore = Math.max(0, Math.min(100, finalScore)); // Clamp 0 to 100

        return new EvaluationResult(
            finalScore,
            similarity,
            matchedKeywords,
            missingKeywords,
            fillerCounts,
            totalFillerCount,
            sentiment,
            tone,
            suggestions,
            feedback
        );
    }
}
