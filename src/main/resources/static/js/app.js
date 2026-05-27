// ==========================================================================
// APPLICATION GLOBAL STATE
// ==========================================================================
const API_BASE = "http://localhost:8080/api";
let allQuestions = [];
let filteredQuestions = [];
let activeQuestion = null;
let currentFilter = "all";

// Speech Recognition Variables
let recognition = null;
let isRecording = false;
let speechTranscript = "";

// Speech Synthesis Variables
let synth = window.speechSynthesis;
let activeUtterance = null;

// ==========================================================================
// INITIALIZATION AND ROUTING
// ==========================================================================
document.addEventListener("DOMContentLoaded", () => {
    // Nav Items Event Listeners
    document.getElementById("nav-dashboard").addEventListener("click", () => showView("dashboard"));
    document.getElementById("nav-mock").addEventListener("click", () => {
        if (activeQuestion) {
            showView("mock");
        } else {
            // If no question is active, prompt to select Q1
            if (allQuestions.length > 0) {
                startMockSession(allQuestions[0].id);
            } else {
                showView("dashboard");
                alert("Please select a question from the board first!");
            }
        }
    });
    document.getElementById("nav-custom").addEventListener("click", () => showView("custom"));

    // Mock Input Mode Tabs
    const tabSpeech = document.getElementById("tab-speech");
    const tabKeyboard = document.getElementById("tab-keyboard");
    const panelSpeech = document.getElementById("panel-speech-input");
    const panelKeyboard = document.getElementById("panel-keyboard-input");

    tabSpeech.addEventListener("click", () => {
        tabSpeech.classList.add("active");
        tabKeyboard.classList.remove("active");
        panelSpeech.classList.remove("hidden");
        panelKeyboard.classList.add("hidden");
    });

    tabKeyboard.addEventListener("click", () => {
        tabKeyboard.classList.add("active");
        tabSpeech.classList.remove("active");
        panelKeyboard.classList.remove("hidden");
        panelSpeech.classList.add("hidden");
    });

    // Voice Recording Listeners
    document.getElementById("btn-record-toggle").addEventListener("click", toggleRecording);

    // TTS Reader Listener
    document.getElementById("btn-read-question").addEventListener("click", speakActiveQuestion);

    // Submit Answer Listener
    document.getElementById("btn-submit-answer").addEventListener("click", submitAnswerForEvaluation);

    // Bootstrap app data
    fetchQuestions();
    initSpeechRecognition();
});

/**
 * Handle screen state transitions (Multi-View)
 */
function showView(viewId) {
    // Update active nav-item highlight
    document.querySelectorAll(".nav-item").forEach(item => item.classList.remove("active"));
    
    if (viewId === "dashboard") {
        document.getElementById("nav-dashboard").classList.add("active");
        document.getElementById("page-title").textContent = "Interview Dashboard";
        document.getElementById("page-subtitle").textContent = "Welcome back, future engineer. Ready to practice?";
    } else if (viewId === "mock") {
        document.getElementById("nav-mock").classList.add("active");
        document.getElementById("page-title").textContent = "Active Interview Simulator";
        document.getElementById("page-subtitle").textContent = "Mimic a live speaking arena. Review and speak your response.";
    } else if (viewId === "custom") {
        document.getElementById("nav-custom").classList.add("active");
        document.getElementById("page-title").textContent = "Add Custom Challenge";
        document.getElementById("page-subtitle").textContent = "Expand your technical or core soft skills bank.";
    } else if (viewId === "results") {
        document.getElementById("page-title").textContent = "AI Performance Evaluation";
        document.getElementById("page-subtitle").textContent = "Detailed analytical scorecard processed locally.";
    }

    // Toggle active classes on view containers
    document.querySelectorAll(".content-view").forEach(view => {
        view.classList.remove("active");
    });
    
    const targetView = document.getElementById(`view-${viewId}`);
    targetView.classList.add("active");

    // Clear background speech reader when exiting mock room
    if (viewId !== "mock" && synth.speaking) {
        synth.cancel();
        setInterviewerSphereState("neutral");
    }
}

// ==========================================================================
// REST SERVICES - QUESTIONS HANDLING
// ==========================================================================
async function fetchQuestions() {
    try {
        const response = await fetch(`${API_BASE}/questions`);
        if (!response.ok) throw new Error("Could not retrieve question repository.");
        
        allQuestions = await response.json();
        filterQuestions(currentFilter);
    } catch (err) {
        console.error(err);
        // Fallback demo questions if server is compiling offline
        allQuestions = getOfflineDemoQuestions();
        filterQuestions(currentFilter);
    }
}

function filterQuestions(category) {
    currentFilter = category;
    
    // Highlight correct filter button
    document.querySelectorAll(".filter-btn").forEach(btn => btn.classList.remove("active"));
    if (category === "all") document.getElementById("btn-filter-all").classList.add("active");
    else if (category === "HR") document.getElementById("btn-filter-hr").classList.add("active");
    else if (category === "Technical") document.getElementById("btn-filter-tech").classList.add("active");

    if (category === "all") {
        filteredQuestions = allQuestions;
    } else {
        filteredQuestions = allQuestions.filter(q => q.type === category);
    }
    
    renderQuestionsTable();
}

function renderQuestionsTable() {
    const listBody = document.getElementById("questions-list");
    listBody.innerHTML = "";

    if (filteredQuestions.length === 0) {
        listBody.innerHTML = `<tr><td colspan="5" style="text-align: center; color: var(--text-muted); padding: 2rem;">No questions available in this category.</td></tr>`;
        return;
    }

    filteredQuestions.forEach(q => {
        const tr = document.createElement("tr");

        // Format keywords list
        const keywordTags = q.keywords.map(kw => `<span class="keyword-tag-pill">${kw}</span>`).slice(0, 3).join("");
        const keywordDots = q.keywords.length > 3 ? '<span class="keyword-tag-pill">...</span>' : "";

        tr.innerHTML = `
            <td class="td-q-text" title="${q.text}">${q.text}</td>
            <td><span class="table-category-badge">${q.category}</span></td>
            <td><span class="table-type-badge ${q.type === 'HR' ? 'type-hr' : 'type-tech'}">${q.type === 'HR' ? 'Behavioral' : 'Technical'}</span></td>
            <td>${keywordTags}${keywordDots}</td>
            <td>
                <button class="table-play-btn" onclick="startMockSession(${q.id})" title="Start Mock Interview"><i class="fa-solid fa-play"></i></button>
            </td>
        `;
        listBody.appendChild(tr);
    });
}

function updateFormCategoryPlaceholder() {
    const typeSelect = document.getElementById("form-question-type");
    const catInput = document.getElementById("form-question-category");
    
    if (typeSelect.value === "HR") {
        catInput.placeholder = "e.g., Leadership, Teamwork, Background";
    } else {
        catInput.placeholder = "e.g., Multithreading, Databases, MVC, JVM";
    }
}

async function saveCustomQuestion(event) {
    event.preventDefault();

    const text = document.getElementById("form-question-text").value;
    const type = document.getElementById("form-question-type").value;
    const category = document.getElementById("form-question-category").value;
    const keywords = document.getElementById("form-question-keywords").value;
    const idealAnswer = document.getElementById("form-question-ideal").value;

    const payload = { text, type, category, keywords, idealAnswer };

    try {
        const response = await fetch(`${API_BASE}/questions`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        if (!response.ok) throw new Error("Could not add custom question.");

        document.getElementById("custom-question-form").reset();
        alert("Success! Custom question successfully registered to repository.");
        
        await fetchQuestions(); // Reload board
        showView("dashboard");   // Navigate home
    } catch (err) {
        alert("Failed to submit question. Attempting local registration fallback.");
        console.error(err);
        
        // Local memory fallback
        const nextId = allQuestions.length > 0 ? Math.max(...allQuestions.map(q=>q.id)) + 1 : 1;
        const keywordsList = keywords.split(",").map(k => k.trim()).filter(Boolean);
        allQuestions.push({ id: nextId, text, type, category, keywords: keywordsList, idealAnswer });
        
        filterQuestions(currentFilter);
        document.getElementById("custom-question-form").reset();
        showView("dashboard");
    }
}

// ==========================================================================
// SPEECH RECOGNITION (SPEECH TO TEXT)
// ==========================================================================
function initSpeechRecognition() {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    
    if (!SpeechRecognition) {
        console.warn("Speech recognition is not natively supported by this browser. Text entry works perfectly!");
        document.getElementById("btn-record-toggle").disabled = true;
        document.getElementById("record-status-label").textContent = "Mic input disabled (Use Text Editor)";
        return;
    }

    recognition = new SpeechRecognition();
    recognition.continuous = true;
    recognition.interimResults = true;
    recognition.lang = "en-US";

    recognition.onstart = () => {
        isRecording = true;
        setInterviewerSphereState("listening");
        document.getElementById("btn-record-toggle").classList.add("active");
        
        const label = document.getElementById("record-status-label");
        label.textContent = "Listening... Speak now.";
        label.classList.add("recording");
    };

    recognition.onresult = (event) => {
        let interimTranscript = "";
        let finalTranscript = "";

        for (let i = event.resultIndex; i < event.results.length; ++i) {
            if (event.results[i].isFinal) {
                speechTranscript += event.results[i][0].transcript + " ";
            } else {
                interimTranscript += event.results[i][0].transcript;
            }
        }

        // Render live output text
        const box = document.getElementById("speech-transcript-text");
        box.classList.add("has-content");
        box.innerHTML = `<strong>${speechTranscript}</strong> <span style="color: var(--text-muted);">${interimTranscript}</span>`;
    };

    recognition.onerror = (event) => {
        console.error("Speech Recognition Error:", event.error);
        if (event.error === 'not-allowed') {
            alert("Microphone permission was denied. Please adjust your browser settings and try again.");
            stopRecording();
        }
    };

    recognition.onend = () => {
        isRecording = false;
        setInterviewerSphereState("neutral");
        document.getElementById("btn-record-toggle").classList.remove("active");
        
        const label = document.getElementById("record-status-label");
        label.textContent = "Tap Mic to Resume Dictating";
        label.classList.remove("recording");
    };
}

function toggleRecording() {
    if (!recognition) return;

    if (isRecording) {
        stopRecording();
    } else {
        startRecording();
    }
}

function startRecording() {
    if (!recognition || isRecording) return;
    
    // Stop speaking if question was reading
    if (synth.speaking) {
        synth.cancel();
    }

    // Reset transcription if fresh session
    const box = document.getElementById("speech-transcript-text");
    if (speechTranscript === "" || box.textContent.includes("Your spoken answer")) {
        speechTranscript = "";
        box.textContent = "Adjusting microphone level...";
    }

    recognition.start();
}

function stopRecording() {
    if (!recognition || !isRecording) return;
    recognition.stop();
}

// ==========================================================================
// SPEECH SYNTHESIS (TEXT TO SPEECH)
// ==========================================================================
function speakActiveQuestion() {
    if (!activeQuestion) return;

    // Stop existing speech
    if (synth.speaking) {
        synth.cancel();
        setInterviewerSphereState("neutral");
        document.getElementById("btn-read-question").innerHTML = `<i class="fa-solid fa-volume-high"></i> Speak Aloud`;
        return;
    }

    // Stop recording if speaking
    if (isRecording) {
        stopRecording();
    }

    activeUtterance = new SpeechSynthesisUtterance(activeQuestion.text);
    
    // Choose a premium sounding english speaker voice if available
    const voices = synth.getVoices();
    const englishVoice = voices.find(voice => voice.lang.startsWith("en") && voice.name.includes("Natural")) || 
                         voices.find(voice => voice.lang.startsWith("en") && voice.name.includes("Google")) ||
                         voices.find(voice => voice.lang.startsWith("en"));
    if (englishVoice) {
        activeUtterance.voice = englishVoice;
    }

    activeUtterance.rate = 0.95; // Slightly slower, professional pacing

    activeUtterance.onstart = () => {
        setInterviewerSphereState("speaking");
        document.getElementById("btn-read-question").innerHTML = `<i class="fa-solid fa-circle-stop animate-icon-red"></i> Stop Reading`;
        document.getElementById("interviewer-status").textContent = "Question Reading Aloud...";
    };

    activeUtterance.onend = () => {
        setInterviewerSphereState("neutral");
        document.getElementById("btn-read-question").innerHTML = `<i class="fa-solid fa-volume-high"></i> Speak Aloud`;
        document.getElementById("interviewer-status").textContent = "Dictation Standby. Tap mic below.";
    };

    activeUtterance.onerror = () => {
        setInterviewerSphereState("neutral");
        document.getElementById("btn-read-question").innerHTML = `<i class="fa-solid fa-volume-high"></i> Speak Aloud`;
        document.getElementById("interviewer-status").textContent = "Audio reading failed.";
    };

    synth.speak(activeUtterance);
}

/**
 * Handle Interviewer Sphere Visual States
 */
function setInterviewerSphereState(state) {
    const sphere = document.getElementById("visualizer-sphere");
    const wave = document.getElementById("sound-waves");

    // Remove all classes
    sphere.className = "interviewer-sphere";
    wave.classList.remove("active");

    if (state === "speaking") {
        sphere.classList.add("speaking");
        wave.classList.add("active");
        sphere.innerHTML = `<i class="fa-solid fa-volume-high"></i>`;
    } 
    else if (state === "listening") {
        sphere.classList.add("listening");
        wave.classList.add("active");
        sphere.innerHTML = `<i class="fa-solid fa-microphone-lines"></i>`;
    } 
    else if (state === "thinking") {
        sphere.classList.add("thinking");
        sphere.innerHTML = `<i class="fa-solid fa-circle-notch"></i>`;
    } 
    else {
        // Neutral/Default state
        sphere.innerHTML = `<i class="fa-solid fa-user-tie"></i>`;
        document.getElementById("interviewer-status").textContent = "Click Speak Aloud to Hear Question";
    }
}

// ==========================================================================
// MOCK SESSION GRADERS AND API COMMUNICATOR
// ==========================================================================
function startMockSession(id) {
    const question = allQuestions.find(q => q.id === id);
    if (!question) return;

    activeQuestion = question;

    // Reset view fields
    document.getElementById("active-question-category").textContent = question.category.toUpperCase();
    document.getElementById("active-question-type").textContent = question.type === 'HR' ? 'BEHAVIORAL' : 'TECHNICAL';
    document.getElementById("active-question-text").textContent = question.text;

    // Reset voice transcript box
    speechTranscript = "";
    const box = document.getElementById("speech-transcript-text");
    box.classList.remove("has-content");
    box.textContent = "Your spoken answer will appear here in real-time...";

    // Reset Keyboard Text area
    document.getElementById("keyboard-answer-text").value = "";

    // Show panel tabs defaults
    const tabSpeech = document.getElementById("tab-speech");
    const tabKeyboard = document.getElementById("tab-keyboard");
    const panelSpeech = document.getElementById("panel-speech-input");
    const panelKeyboard = document.getElementById("panel-keyboard-input");

    tabSpeech.classList.add("active");
    tabKeyboard.classList.remove("active");
    panelSpeech.classList.remove("hidden");
    panelKeyboard.classList.add("hidden");

    // Clean Interviewer Visualizer
    setInterviewerSphereState("neutral");
    
    showView("mock");
}

function retryActiveQuestion() {
    if (activeQuestion) {
        startMockSession(activeQuestion.id);
    } else {
        showView("dashboard");
    }
}

async function submitAnswerForEvaluation() {
    if (!activeQuestion) return;

    // Stop recording or speaking if active
    if (isRecording) stopRecording();
    if (synth.speaking) synth.cancel();

    // Capture answer text based on currently active tab
    let answerText = "";
    const isSpeechTabActive = document.getElementById("tab-speech").classList.contains("active");

    if (isSpeechTabActive) {
        answerText = speechTranscript.trim();
        // If they started recording but didn't write anything, read placeholder
        const box = document.getElementById("speech-transcript-text");
        if (box.classList.contains("has-content") && answerText === "") {
            // Check if they typed/finalized anything
            answerText = box.innerText.trim();
        }
    } else {
        answerText = document.getElementById("keyboard-answer-text").value.trim();
    }

    if (!answerText || answerText.startsWith("Your spoken answer") || answerText === "Adjusting microphone level...") {
        alert("Please speak or type an answer before submitting!");
        return;
    }

    // Set sphere to thinking status
    setInterviewerSphereState("thinking");
    document.getElementById("interviewer-status").textContent = "AI Analysis In Progress...";

    const payload = {
        questionId: activeQuestion.id,
        answer: answerText
    };

    try {
        const response = await fetch(`${API_BASE}/evaluate`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        if (!response.ok) throw new Error("Grading process failed on server.");

        const scorecard = await response.json();
        renderScorecard(scorecard, answerText);
    } catch (err) {
        console.error(err);
        alert("Failed to analyze response via remote backend. Simulating local NLP grading...");
        
        // Simulating robust local grading if server offline
        const simulatedScorecard = simulateLocalNlpEngine(answerText);
        renderScorecard(simulatedScorecard, answerText);
    }
}

// ==========================================================================
// RENDER GRADING RESULTS & CHARTS
// ==========================================================================
function renderScorecard(result, userAnswer) {
    showView("results");

    // 1. Render Circle progress score
    const scoreNum = document.getElementById("results-score-num");
    const ringProgress = document.getElementById("results-ring-progress");
    
    scoreNum.textContent = result.score;
    
    // Set circle dashoffset:
    // Max circumference = 326.7. ProgressOffset = Circumference - (Percent * Circumference)
    const percent = result.score / 100;
    const offset = 326.7 - (percent * 326.7);
    ringProgress.style.strokeDashoffset = offset;

    // Apply color highlights based on score
    if (result.score >= 80) {
        ringProgress.style.stroke = "var(--success)";
        document.getElementById("results-badge-verdict").className = "results-badge";
        document.getElementById("results-badge-verdict").textContent = "Strong Confidence";
        document.getElementById("results-badge-verdict").style.color = "var(--success)";
        document.getElementById("results-badge-verdict").style.borderColor = "rgba(16, 185, 129, 0.25)";
        document.getElementById("results-badge-verdict").style.backgroundColor = "rgba(16, 185, 129, 0.1)";
    } else if (result.score >= 55) {
        ringProgress.style.stroke = "var(--warning)";
        document.getElementById("results-badge-verdict").className = "results-badge";
        document.getElementById("results-badge-verdict").textContent = "Solid Foundation";
        document.getElementById("results-badge-verdict").style.color = "var(--warning)";
        document.getElementById("results-badge-verdict").style.borderColor = "rgba(245, 158, 11, 0.25)";
        document.getElementById("results-badge-verdict").style.backgroundColor = "rgba(245, 158, 11, 0.1)";
    } else {
        ringProgress.style.stroke = "var(--danger)";
        document.getElementById("results-badge-verdict").className = "results-badge";
        document.getElementById("results-badge-verdict").textContent = "Needs Revision";
        document.getElementById("results-badge-verdict").style.color = "var(--danger)";
        document.getElementById("results-badge-verdict").style.borderColor = "rgba(239, 68, 68, 0.25)";
        document.getElementById("results-badge-verdict").style.backgroundColor = "rgba(239, 68, 68, 0.1)";
    }

    // 2. Overview data text strings
    document.getElementById("results-feedback-summary").innerHTML = `<strong>${result.feedback}</strong>`;
    document.getElementById("results-stat-tone").textContent = result.tone;
    document.getElementById("results-stat-sentiment").textContent = result.sentiment;
    
    const simPercent = Math.round(result.semanticSimilarity * 100);
    document.getElementById("results-stat-similarity").textContent = `${simPercent}% Semantic Alignment`;

    // 3. Fluency Section
    const fillersTotal = document.getElementById("results-fillers-total");
    fillersTotal.textContent = result.totalFillerCount;

    const fillerNum = document.getElementById("results-fillers-total");
    if (result.totalFillerCount === 0) {
        fillerNum.className = "filler-large-num low";
        document.getElementById("results-filler-verdict-text").textContent = "Flawless speaking! Excellent verbal control.";
    } else if (result.totalFillerCount <= 3) {
        fillerNum.className = "filler-large-num";
        fillerNum.style.color = "var(--warning)";
        document.getElementById("results-filler-verdict-text").textContent = "Good control. A few minor hesitations.";
    } else {
        fillerNum.className = "filler-large-num";
        fillerNum.style.color = "var(--danger)";
        document.getElementById("results-filler-verdict-text").textContent = "Frequent fillers. Focus on intentional pauses.";
    }

    const fillerListDiv = document.getElementById("results-fillers-list");
    fillerListDiv.innerHTML = "";
    
    const keys = Object.keys(result.fillerWords);
    if (keys.length === 0) {
        fillerListDiv.innerHTML = `<span style="font-size: 0.82rem; color: var(--text-muted); font-style: italic;">No filler words flagged. Outstanding fluency!</span>`;
    } else {
        keys.forEach(filler => {
            const count = result.fillerWords[filler];
            const span = document.createElement("span");
            span.className = "filler-count-tag";
            span.innerHTML = `"${filler}": <strong>${count}</strong>`;
            fillerListDiv.appendChild(span);
        });
    }

    // 4. Terminology Cloud
    const keywordCloud = document.getElementById("results-keywords-cloud");
    keywordCloud.innerHTML = "";

    if (result.matchedKeywords.length === 0 && result.missingKeywords.length === 0) {
        keywordCloud.innerHTML = `<span style="font-size: 0.82rem; color: var(--text-muted); font-style: italic;">No core vocabulary constraints mapped.</span>`;
    } else {
        result.matchedKeywords.forEach(kw => {
            const span = document.createElement("span");
            span.className = "keyword-item-badge matched";
            span.innerHTML = `<i class="fa-solid fa-circle-check"></i> ${kw}`;
            keywordCloud.appendChild(span);
        });

        result.missingKeywords.forEach(kw => {
            const span = document.createElement("span");
            span.className = "keyword-item-badge missing";
            span.innerHTML = `<i class="fa-solid fa-circle-exmark"></i> ${kw}`;
            keywordCloud.appendChild(span);
        });
    }

    // 5. AI Suggestions Block
    const suggestionsUl = document.getElementById("results-suggestions-list");
    suggestionsUl.innerHTML = "";
    
    if (result.suggestions.length === 0) {
        const li = document.createElement("li");
        li.textContent = "Your response is highly complete. No critical improvements recommended! Focus on replicating this depth in live environments.";
        li.style.borderColor = "var(--success)";
        suggestionsUl.appendChild(li);
    } else {
        result.suggestions.forEach(tip => {
            const li = document.createElement("li");
            li.innerHTML = tip;
            suggestionsUl.appendChild(li);
        });
    }

    // 6. Side-by-Side Comparison Box
    document.getElementById("results-user-transcript").textContent = userAnswer;
    document.getElementById("results-ideal-text").textContent = activeQuestion.idealAnswer;
}

// ==========================================================================
// OFFLINE FALLBACK IMPLEMENTATION
// ==========================================================================
function getOfflineDemoQuestions() {
    return [
        {
            id: 1,
            text: "Tell me about yourself and walk me through your background.",
            type: "HR",
            category: "Behavioral",
            keywords: ["experience", "passionate", "skills", "background", "project", "degree", "solved", "team"],
            idealAnswer: "Sure! I am a software engineering graduate with strong foundations in full-stack web development and core Java. During my academic years, I spearheaded several key projects, including a microservices-based web application that optimized data loading times. I am highly passionate about coding clean, testable logic and collaborating with cross-functional teams to build products that solve real-world problems. In my next role, I am eager to apply my analytical skills and grow as a software engineer in an agile environment."
        },
        {
            id: 3,
            text: "Explain how a HashMap works under the hood in Java.",
            type: "Technical",
            category: "Data Structures",
            keywords: ["hashcode", "equals", "collision", "bucket", "index", "linked list", "treeify", "O(1)", "key", "value"],
            idealAnswer: "In Java, a HashMap works on the principle of hashing. It uses a bucket array internally to store key-value pairs. When we call put(), the HashMap invokes the hashCode() method on the key to calculate a hash value, which is then mapped to a bucket index. If two different keys map to the same index, a collision occurs. In older Java versions, collisions were resolved using a Linked List. In Java 8, if a bucket size exceeds a threshold of 8, it is treeified into a balanced Red-Black Tree, improving collision lookup from O(N) to O(log N). When fetching elements via get(), the hashCode() locates the bucket, and the equals() method is used to pinpoint the exact key-value match."
        }
    ];
}

/**
 * Simulates the NLP engine client-side if server endpoints compile offline
 */
function simulateLocalNlpEngine(answerText) {
    if (!activeQuestion) return null;
    
    const ansLower = answerText.toLowerCase();
    
    // Check keywords
    const matchedKeywords = [];
    const missingKeywords = [];
    activeQuestion.keywords.forEach(kw => {
        if (ansLower.includes(kw.toLowerCase())) matchedKeywords.add(kw);
        else missingKeywords.push(kw);
    });

    const fillerWordsMap = {};
    let totalFillers = 0;
    const fillers = ["um", "uh", "like", "actually", "basically", "you know", "sort of"];
    fillers.forEach(f => {
        const regex = new RegExp(`\\b${f}\\b`, 'gi');
        const matches = answerText.match(regex);
        if (matches) {
            fillerWordsMap[f] = matches.length;
            totalFillers += matches.length;
        }
    });

    // Score heuristic
    let matchedRatio = activeQuestion.keywords.length > 0 ? (matchedKeywords.length / activeQuestion.keywords.length) : 0.8;
    let baseScore = 40 + (matchedRatio * 45) - (totalFillers * 3.5);
    
    // Length penalty
    const wordCount = answerText.split(/\s+/).length;
    if (wordCount < 20) baseScore -= 20;

    const score = Math.max(0, Math.min(100, Math.round(baseScore)));

    const feedback = score >= 80 ? "Superb response. Your local dictation and vocabulary structure are excellent!" 
                    : (score >= 60 ? "Solid base. Work on decreasing speech fillers and including more core vocabulary." 
                    : "Focus on adding core technical terminology and lengthening your speaking response.");

    return {
        score,
        semanticSimilarity: 0.4 + (matchedRatio * 0.4),
        matchedKeywords,
        missingKeywords,
        fillerWords: fillerWordsMap,
        totalFillerCount: totalFillers,
        sentiment: "Objective & Confident",
        tone: totalFillers > 3 ? "Conversational but Hesitant" : "Clear & Professional",
        suggestions: score < 80 ? ["Add technical terminology: **" + missingKeywords.join(", ") + "**", "Reduce filler word frequency."] : [],
        feedback
    };
}
