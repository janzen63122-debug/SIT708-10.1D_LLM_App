from flask import Flask, request, jsonify
from pymongo import MongoClient
import certifi
import re
import ollama
import stripe
 
app = Flask(__name__)
stripe.api_key = "sk_test_51TVQmLHv1LjUfcrTAbEL3601bC037n4NTMUBsNCRavKRhxduhm2pXvcTvP99FREwsKutKTakWZsU3b9coB9O7pst00FEUJH5nF"
 
MONGO_URI = "mongodb+srv://janzen63122_db_user:Tqa2lRYwRJsSfEQ6@helphubcluster.3rvdbf8.mongodb.net/?appName=HelpHubCluster"

try:
    client = MongoClient(MONGO_URI, tlsCAFile=certifi.where())
    
    
    db = client["helphub_db"]
    
    history_collection = db["quiz_history"]
    
    
    client.admin.command('ping')
    print("✅ Successfully connected to MongoDB Atlas!")
except Exception as e:
    print("❌ Failed to connect to MongoDB:", e)

# --- OLLAMA AI SETUP ---
MODEL = "qwen2.5:3b"
 
def call_llm(prompt: str, max_tokens: int = 200) -> str:
    response = ollama.chat(
        model=MODEL,
        messages=[{"role": "user", "content": prompt}],
        options={"num_predict": max_tokens, "temperature": 0.3}
    )
    return response["message"]["content"]
 
def fetchQuizFromLlama(student_topic: str) -> str:
    if ',' in student_topic:
        student_topic = student_topic.split(',')[0].strip()
    print(f"Fetching quiz for topic: {student_topic}")
 
    query = (
        f"Write 3 multiple choice questions about {student_topic}.\n"
        f"Use this EXACT format for every question, nothing else:\n\n"
        f"QUESTION: question text here\n"
        f"OPTION A: first option\n"
        f"OPTION B: second option\n"
        f"OPTION C: third option\n"
        f"OPTION D: fourth option\n"
        f"ANS: A\n\n"
        f"Now write all 3 questions using only the format above:"
    )
    return call_llm(query, max_tokens=800)
 
def process_quiz(quiz_text: str) -> list:
    questions = []
    pattern = re.compile(
        r'(?:QUESTION:\s*)?(.+?\?)\s*\n'
        r'\s*(?:OPTION\s+)?A[).:\s]+(.+?)\n'
        r'\s*(?:OPTION\s+)?B[).:\s]+(.+?)\n'
        r'\s*(?:OPTION\s+)?C[).:\s]+(.+?)\n'
        r'\s*(?:OPTION\s+)?D[).:\s]+(.+?)\n'
        r'(?:\s*ANS(?:WER)?:\s*([A-D]))?',
        re.IGNORECASE
    )
    seen = set()
    for match in pattern.finditer(quiz_text):
        key = match.group(1).strip()[:60]
        if key in seen:
            continue
        seen.add(key)
        questions.append({
            "question":       match.group(1).strip(),
            "options":        [match.group(2).strip(), match.group(3).strip(),
                               match.group(4).strip(), match.group(5).strip()],
            "correct_answer": (match.group(6) or "A").strip().upper()
        })
    return questions[:3]
 
def fetchHintFromLlama(question: str) -> str:
    print(f"Fetching hint for: {question}")
    query = (
        f"A student is answering this multiple-choice question. "
        f"Give a short hint (1-2 sentences) that nudges them toward the answer without revealing it.\n"
        f"Question: {question}"
    )
    return call_llm(query, max_tokens=150)
 
def fetchExplanationFromLlama(question: str, answer: str) -> str:
    print(f"Fetching explanation for q='{question}', a='{answer}'")
    query = (
        f"In 2-3 sentences, explain whether this answer is correct or incorrect and why.\n"
        f"Question: {question}\n"
        f"Student's answer: {answer}"
    )
    return call_llm(query, max_tokens=200)

def fetchSummaryFromLlama(mistakes: str) -> str:
    print("Fetching summary for incorrect answers...")
    query = (
        f"A student got the following questions wrong:\n{mistakes}\n"
        f"In exactly 1 or 2 sentences, briefly summarize the core topics they struggle with and offer an encouraging tip."
    )
    return call_llm(query, max_tokens=150)


@app.route("/", methods=["GET"])
def home():
    return jsonify({"message": "Welcome to the Flask API!"}), 200
 
@app.route("/getQuiz", methods=["GET"])
def get_quiz():
    topic = request.args.get("topic")
    if not topic:
        return jsonify({"error": "Missing topic parameter"}), 400
    raw = fetchQuizFromLlama(topic)
    parsed = process_quiz(raw)
    if not parsed:
        return jsonify({"error": "Could not parse quiz.", "raw": raw}), 500
    return jsonify({"quiz": parsed}), 200
 
@app.route("/getHint", methods=["GET"])
def get_hint():
    question = request.args.get("question")
    if not question:
        return jsonify({"error": "Missing question parameter"}), 400
    hint = fetchHintFromLlama(question)
    return jsonify({"hint": hint}), 200
 
@app.route("/getExplanation", methods=["GET"])
def get_explanation():
    question = request.args.get("question")
    answer = request.args.get("answer")
    if not question or not answer:
        return jsonify({"error": "Missing parameters"}), 400
    explanation = fetchExplanationFromLlama(question, answer)
    return jsonify({"explanation": explanation}), 200

# FOR DATABASE ---

@app.route("/saveHistory", methods=["POST"])
def save_history():
    """Saves a completed quiz result to MongoDB"""
    data = request.json
    if not data or "username" not in data or "results" not in data:
        return jsonify({"error": "Missing username or results data"}), 400
    
    try:
        
        history_collection.insert_one(data)
        return jsonify({"message": "History saved successfully!"}), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route("/getHistory", methods=["GET"])
def get_history():
    """Fetches a user's past quizzes from MongoDB"""
    username = request.args.get("username")
    if not username:
        return jsonify({"error": "Missing username parameter"}), 400
    
    try:
        
        user_history = list(history_collection.find({"username": username}, {"_id": 0}))
        return jsonify({"history": user_history}), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route("/getSummary", methods=["POST"])
def get_summary():
    """Generates a brief summary of the user's incorrect answers"""
    data = request.json
    mistakes = data.get("mistakes", "")
    
    if not mistakes:
        return jsonify({"summary": "Perfect score! Keep up the great work."}), 200
        
    try:
        summary = fetchSummaryFromLlama(mistakes)
        return jsonify({"summary": summary}), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500
    
@app.route("/create-payment-intent", methods=["POST"])
def create_payment():
    try:
        data = request.json
        
        amount = data.get("amount", 499) 

        
        intent = stripe.PaymentIntent.create(
            amount=amount,
            currency="aud", # Australian Dollars
            automatic_payment_methods={
                'enabled': True,
            },
        )
        
        return jsonify({'clientSecret': intent['client_secret']}), 200
    except Exception as e:
        return jsonify(error=str(e)), 403
 
if __name__ == "__main__":
    port_num = 5000
    print(f"App running on port {port_num}")
    app.run(port=port_num, host="0.0.0.0")