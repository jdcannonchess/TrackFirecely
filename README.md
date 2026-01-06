# Track Fiercely

A native Android habit tracker app built with Kotlin and Jetpack Compose for tracking recurring habits, one-time tasks, and daily wellness metrics with weekly progress reports.

## Features

- **Daily Task Management**: View and complete tasks assigned to each day
- **Recurring Tasks**: Set up habits that repeat on specific days of the week
- **One-Time Tasks**: Add single-occurrence tasks without affecting weekly tracking
- **Categories**: Organize tasks into 6 categories:
  - Health & Fitness (coral)
  - Family (teal)
  - Home & Chores (amber)
  - Hobbies (purple)
  - Personal Growth (green)
  - Work (blue)
- **Daily Wellness Logging**: Track mood (1-10), sleep hours & quality (1-5 stars), weight, and steps
- **Weekly Reports**: See completion grades, highlights, mood trends, and perfect days
- **History**: Browse past weekly snapshots and track long-term patterns
- **Monday Auto-Reset**: Recurring tasks automatically reset each week

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material Design 3
- **Database**: Room (SQLite wrapper)
- **Architecture**: MVVM with Repository pattern
- **Navigation**: Jetpack Navigation Compose
- **Preferences**: DataStore

## Project Structure

```
app/src/main/java/com/trackfiercely/
├── data/
│   ├── database/         # Room database, DAOs, type converters
│   ├── model/            # Data classes (Task, DailyLog, WeeklySnapshot, Category)
│   └── repository/       # TaskRepository, DailyLogRepository, WeeklySnapshotRepository
├── ui/
│   ├── screens/          # Compose screens (Home, TaskList, CreateTask, WeeklyReport, History)
│   ├── components/       # Reusable UI components (DayCard, TaskItem, MoodSlider)
│   ├── theme/            # Material 3 theme, colors, typography
│   └── navigation/       # NavHost setup and routes
├── viewmodel/            # ViewModels for each screen
├── util/                 # Date utilities
├── TrackFiercelyApp.kt   # Application class with weekly setup
└── MainActivity.kt       # Entry point
```

## Building the App

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 34
- JDK 17

### Steps

1. Open the `TrackFiercely` folder in Android Studio
2. Wait for Gradle sync to complete
3. Connect an Android device or start an emulator (API 26+)
4. Click "Run" or press `Shift + F10`

## Color Theme

The app uses an Emerald Green theme representing growth and progress:
- **Primary**: Emerald (#10B981)
- **Category colors**: Each category has its own distinct color
- **Mood colors**: Gradient from red (1) to teal (10)
- **Background**: Light gray (#FAFAFA)

## Usage

1. **Home Screen**: Swipe between days to see tasks, tap tasks to complete them
2. **Add Task**: Tap the + button to create recurring or one-time tasks
3. **Log Wellness**: Expand the "Log My Day" card to track mood, sleep, weight, steps
4. **Weekly Report**: View your progress summary with grades, highlights, and trends
5. **History**: Browse past weeks and track long-term patterns

## How Weekly Tracking Works

- **Week Cycle**: Monday to Sunday
- **Monday Reset**: Recurring tasks automatically repopulate each Monday
- **Snapshots**: Weekly summaries are generated and saved for history
- **Grading**: A+ (95%+), A (90%+), B+ (80%+), etc.

---

*Track fiercely, build habits consistently!*

