# HelpHub: Cloud-Connected AI Learning Platform

HelpHub is an Android application designed to speed up mobile learning. Built as an expansion of a foundational quizzing app (Task 6.1D), this version introduces cloud data persistence, LLM analytics, and secure payment using modern Android development practices.

## Key Features 
* **Cloud Data Persistence:** Fully integrated with **MongoDB Atlas** via a Python Flask REST API to securely save user profiles, track aggregate performance, and maintain a detailed history of past sessions.
* **Smart Mistake Analysis:** Aggregates a user's recent incorrect answers and executes a secondary query to the LLM to generate a custom, real-time study summary highlighting specific areas for improvement.
* **Premium Upgrades (Stripe API):** Integrates the modern Stripe `PaymentSheet` for a secure checkout experience. 
* **Android Sharing:** Utilizes `Intent.ACTION_SEND` to allow users to securely export their learning statistics via native OS share sheets.
* **Dynamic UI Generation:** Screens like the Detailed History parse complex JSON arrays from the cloud and dynamically construct Android UI components programmatically.
* **AI Quiz Generation (Basic):** Retains the core functionality of generating custom multiple-choice questions on the fly using a local LLM (Qwen2.5:3b via Ollama).

**Backend (REST API)**
* **Framework:** Python Flask
* **Database:** MongoDB Atlas (via `pymongo`)
* **AI Integration:** Local LLM via Ollama (Qwen2.5:3b)
* **Payments:** Stripe Python SDK

## Local Setup Instructions

### Backend (Python/Flask)
1. Ensure [Ollama](https://ollama.com/) is installed and running the `qwen2.5:3b` model locally.
2. Navigate to the backend directory
3. Add your Stripe Secret Key and MongoDB URI to the main.py file
4. Run the server
5. Open the project in Android Studio.
6. Navigate to UpgradeActivity.java and insert your Stripe Publishable Key
7. Build and run the application on an Android Emulator (API 24+)
