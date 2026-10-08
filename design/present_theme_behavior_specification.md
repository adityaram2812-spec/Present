# Present — Theme Behavior & Appearance Specification

## Core Rule: Default Appearance is Dark Mode

### 1. First-Launch & Onboarding Behavior
- **Default Theme:** **Dark Mode** is the unequivocal default appearance for all new installations and first-time users.
- **First Launch:** Present must always open in Dark Mode upon initial launch.
- **Onboarding Flow:** The complete onboarding experience (Welcome, Attendance Requirement, Timetable Setup, Add Class, Review Timetable, and Onboarding Complete) executes exclusively in Dark Mode on first run.
- **System Setting Independence:** The host Android device/system theme setting (e.g., system-wide Light Mode) **must not override** Present’s first-launch default. The app ignores system UI day/night flags on first install.

### 2. Post-Onboarding User Preference & Switching
- **User Control:** After onboarding is complete, the user may explicitly toggle between **Dark Mode** and **Light Mode** at any time via:
  `More → Appearance`
- **Persistence:** Once a user explicitly chooses an appearance setting, that preference is stored locally on-device and strictly persisted across all subsequent app launches and process restarts.
- **No System Auto-Switching in V1:** In V1, Present will **not** automatically switch themes based on system time, ambient lighting, or Android system theme triggers (`Follow System` is disabled/excluded in V1). Theme transitions occur only via explicit user selection in Settings.

### 3. Product Positioning & Hierarchy
- **Dark Mode as Flagship:** Dark Mode remains the primary, defining brand aesthetic of Present.
- **Light Mode as User-Selected Alternative:** Light Mode exists as a fully realized, 1:1 visual parity theme for users who prefer higher contrast or daytime reading clarity, accessible purely by user choice.

### 4. Visual Parity Guarantee
- All features, typography hierarchies (Manrope), 8pt spatial rhythm, semantic status margins (Safe `#63C48B`, Warning `#E5AD5C`, Critical `#E36D76`), and metric calculation engines remain identical across both Dark and Light mode implementations.
