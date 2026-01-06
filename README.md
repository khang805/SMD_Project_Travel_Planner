# ✈️ Travel Planner

**Travel Planner** is a feature-rich mobile application designed to help users plan, document, and manage their trips seamlessly. Built with **Kotlin** and a modern Android tech stack, the app provides a unified 
experience for creating trip itineraries, managing budgets, and storing memories in a trip-specific gallery.

It features robust offline capabilities, real-time data synchronization, and interactive mapping.

---

## ✨ Core Features

* **🔐 Secure User Authentication:** Effortless sign-up and sign-in using Firebase Authentication, supporting both traditional email/password and one-tap Google Sign-In (`credentials-play-services-auth`).
* **✈️ Trip Management:** Create, view, update, and delete trips. Each trip includes details like name, destination, start/end dates, and budget.
* **🖼️ Trip Photo Gallery:** Upload and view photos for specific trips to create a visual diary. Images are uploaded to a custom backend, and links are stored efficiently.
* **💵 Budget Tracking:** An integrated budget calculator helps users manage travel expenses effectively against their set trip budget.
* **🗺️ Interactive Maps:** Visualize trip locations and points of interest using the integrated **osmdroid** map library (OpenStreetMap).
* **⚡ Offline-First Capabilities:** Trip data is cached locally using the **Room** database. Users can view their created trips and galleries even without an internet connection.
* **🔔 Push Notifications:** Stay updated with relevant information about trips through Firebase Cloud Messaging (FCM).
* **🔔 Push Notifications: Receive timely reminders and alerts about your trips using and galleries even when offline.
* **👤 User Profile:** Manage user profiles and view comprehensive trip history.

---

## 🏗️ Architecture & Tech Stack

This project is built on a foundation of modern Android architecture and industry-leading libraries to ensure scalability and maintainability.

### Architecture
The app adheres to **MVVM (Model-View-ViewModel)** principles for a clean separation of concerns, ensuring the UI code is distinct from the business logic and data handling.

* **Language:** 100% **Kotlin**, leveraging features like Coroutines for asynchronous operations.
* **UI Declaration:** XML Layouts with **ViewBinding** for type-safe, high-performance access to views.

### Key Libraries & Technologies

| Category | Library/Technology | Purpose |
| :--- | :--- | :--- |
| **Backend & Auth** | Firebase (Auth, Realtime DB, Messaging) | Serverless backend for auth, real-time data persistence, and push notifications. |
| **Mapping** | osmdroid | Open-source alternative to Google Maps for displaying interactive maps/markers. |
| **Local Persistence** | Android Room | Caching trip data locally to enable robust offline access. |
| **Networking** | OkHttp | Robust HTTP client used for handling image uploads to the custom backend server. |
| **UI & Imaging** | Material Design, CircleImageView, Glide | Modern UI components and efficient image loading/caching. |
| **Authentication** | Google Identity Services | Enables streamlined "Sign in with Google" functionality. |
| **Asynchronicity** | Kotlin Coroutines | Manages background threads for DB and network operations (non-blocking). |

---

## 🚀 Getting Started

Follow these instructions to set up the project locally for development and testing.

### Prerequisites
* **Android Studio:** Iguana (2023.2.1) or newer.
* **Android Device/Emulator:** Running API 24 (Nougat) or higher.
* **Firebase Project:** You need access to the Firebase Console.
* **Backend:** A running instance of the custom Node.js backend (for image uploads).

### Installation & Setup

1. Clone the Repository
```bash
git clone [https://github.com/khang805/SMD_Project_Travel-Planner.git]

2. Set Up Firebase:

Visit the Firebase Console and create a new project.

Enable Authentication (Email/Password and Google providers) and Realtime Database.

Register your Android application using the package name defined in the project.

Download the google-services.json file and place it in the app/ directory.

Note: The .gitignore file is configured to keep this file private.

3. Configure Backend Server:

The application uploads images to a custom server. Ensure your Node.js backend is deployed.

Navigate to the networking code (where OkHttp is initialized).

Update the Server IP address to point to your deployed backend instance.

4. Sync and Run:

Open the project in Android Studio.

Allow Gradle to sync automatically.

Build and run the application on your selected Android device or emulator.

Conclusion:
This project represents a comprehensive application of Software Methodologies and Design (SMD) principles. By integrating cloud-based features with offline-first persistence, Travel Planner bridges the gap between
theoretical concepts and real-world utility.

It stands as a testament to the power of the modern Android ecosystem—combining Kotlin, Jetpack libraries, and Firebase to create a seamless user experience. We hope this tool not only aids travelers in their journeys
but also serves as a valuable reference for developers exploring Android application architecture.
