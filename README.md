# Present

> **Attendance, made simple.**

Present is a modern Android attendance tracker designed for college students.

It brings attendance, subjects, timetables, analytics, planning, notifications, and timetable import into one focused experience — so students can spend less time calculating attendance and more time actually attending college.

## ✨ Features

### 📊 Attendance Tracking

- Track attendance for individual subjects
- Record attended and missed lectures
- View current attendance status
- Calculate attendance thresholds
- Plan leave without losing track of attendance

### 🗓️ Timetable Management

- Create and manage class schedules
- Organize subjects and lectures
- Support timetable changes
- Resolve effective schedules from timetable data

### 🤖 AI-Powered Timetable Import

Present supports multiple timetable extraction approaches:

- Local OCR using ML Kit
- Structured timetable parsing
- Spreadsheet timetable import
- AI-assisted timetable extraction

Import a timetable and turn it into structured classes instead of entering everything manually.

### 📈 Analytics

Understand attendance across subjects with dedicated analytics and insights.

### 🔔 Notifications

- Scheduled attendance-related notifications
- Boot-aware notification scheduling
- Timetable-aware notification support

### 📱 Home Screen Widget

Quickly access relevant Present information directly from the Android home screen.

### 👤 Accounts & Cloud Support

Present includes account functionality and a backend integration layer for features that require cloud services.

---

## 🛠️ Tech Stack

| Area | Technology |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | MVVM / Repository-based architecture |
| Local Database | Room |
| Preferences | DataStore |
| OCR | Google ML Kit |
| AI Extraction | Gemini / backend AI pipeline |
| Backend | Node.js |
| Testing | JUnit / Android testing |
| Build System | Gradle |
| Platform | Android |

---

## 🏗️ Architecture

Present is structured around a layered Android architecture.

```text
                    ┌─────────────────────┐
                    │    Jetpack Compose  │
                    │         UI          │
                    └──────────┬──────────┘
                               │
                    ┌──────────▼──────────┐
                    │     ViewModels      │
                    └──────────┬──────────┘
                               │
                    ┌──────────▼──────────┐
                    │      Domain         │
                    │ Repositories/Logic  │
                    └──────────┬──────────┘
                               │
              ┌────────────────┼────────────────┐
              │                │                │
       ┌──────▼──────┐  ┌──────▼──────┐  ┌──────▼──────┐
       │    Room     │  │  Timetable  │  │   Import     │
       │   Database  │  │   Engine    │  │   Pipeline   │
       └─────────────┘  └─────────────┘  └──────┬───────┘
                                                │
                                      ┌─────────▼─────────┐
                                      │ OCR / AI / Backend│
                                      └───────────────────┘

The project separates UI, domain logic, data access, timetable processing, and import pipelines to keep the application maintainable as it grows.
🎨 Design
Present follows a minimal, modern visual system designed around clarity and quick access to attendance information.
The design system and behavioral specifications are documented in:
- [`design/DESIGN.md`](design/DESIGN.md)
- [`design/present_theme_behavior_specification.md`](design/present_theme_behavior_specification.md)
Preview

📂 Project Structure
Present/
├── app/
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── com/adityaram/present/
│       │   │       ├── data/
│       │   │       ├── domain/
│       │   │       ├── notifications/
│       │   │       ├── glance/
│       │   │       └── ui/
│       │   ├── res/
│       │   └── AndroidManifest.xml
│       │
│       ├── debug/
│       ├── release/
│       └── test/
│
├── backend/
│   ├── index.js
│   ├── aiEntitlement.js
│   └── package.json
│
├── design/
│   ├── DESIGN.md
│   ├── present_theme_behavior_specification.md
│   └── screen.png
│
├── docs/
│   └── DEVELOPMENT_HANDOFF.md
│
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md

🚀 Getting Started
Requirements
- Android Studio
- Android SDK
- JDK compatible with the project's Gradle configuration
- Android device or emulator
Clone
git clone https://github.com/adityaram2812-spec/Present.git
cd Present

Configure local properties
Create a local local.properties file containing your Android SDK path.
Any API keys or environment-specific configuration should remain local and must not be committed to the repository.
Build
On Windows:
.\gradlew.bat assembleDebug

On macOS/Linux:
./gradlew assembleDebug

Run tests
.\gradlew.bat test

🧪 Testing
Present contains automated tests covering areas including:
- Timetable parsing
- Timetable grid inference
- Timetable semantic processing
- Schedule resolution
- AI timetable import
- Leave planner logic
The project also contains debug tooling for evaluating timetable extraction and OCR behavior.
🔐 Security
Secrets and machine-specific configuration are intentionally excluded from version control.
Examples include:
local.properties
.env
google-services.json
*.apk
*.aab

Never commit API keys, Firebase credentials, signing keys, or other private credentials.
🗺️ Roadmap
Present is actively evolving.
Potential future work includes:
- Improved timetable import accuracy
- More powerful attendance insights
- Additional widgets
- Better cloud synchronization
- More automation around attendance planning
- Expanded notification capabilities
- Continued UI and performance refinement
📄 License
This project currently does not include an open-source license.
👨‍💻 Author
Aditya Ram
Built with Kotlin, Jetpack Compose, and a lot of experimentation.