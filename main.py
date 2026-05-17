from flask import Flask, request, jsonify
import re
import ollama
 
app = Flask(__name__)
 
MODEL = "qwen2.5:3b"
 
 
def call_llm(prompt: str, max_tokens: int = 200) -> str:
    response = ollama.chat(
        model=MODEL,
        messages=[{"role": "user", "content": prompt}],
        options={"num_predict": max_tokens, "temperature": 0.3}
    )
    return response["message"]["content"]
 
 

def fetchQuizFromLlama(student_topic: str) -> str:
    # Use only the first topic if multiple were selected
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
    result = call_llm(query, max_tokens=800)
    print(result)
    return result
 
 
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
 

@app.route("/", methods=["GET"])
def home():
    return jsonify({"message": "Welcome to the Flask API!"}), 200
 
 
@app.route("/getQuiz", methods=["GET"])
def get_quiz():
    print("getQuiz request received")
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
    print("getHint request received")
    question = request.args.get("question")
    if not question:
        return jsonify({"error": "Missing question parameter"}), 400
    hint = fetchHintFromLlama(question)
    return jsonify({"hint": hint}), 200
 
 
@app.route("/getExplanation", methods=["GET"])
def get_explanation():
    print("getExplanation request received")
    question = request.args.get("question")
    answer = request.args.get("answer")
    if not question or not answer:
        return jsonify({"error": "Missing question or answer parameter"}), 400
    explanation = fetchExplanationFromLlama(question, answer)
    return jsonify({"explanation": explanation}), 200
 
 
@app.route("/test", methods=["GET"])
def run_test():
    return jsonify({"quiz": "test"}), 200
 
 
if __name__ == "__main__":
    port_num = 5000
    print(f"App running on port {port_num}")
    app.run(port=port_num, host="0.0.0.0")
 