# Locket Android

Native Android MVP for Locket.

## Setup

Install JDK 17 and the Android SDK, then create local configuration:

```bash
cp local.properties.example local.properties
```

Set:

```properties
SUPABASE_URL=https://YOUR_PROJECT_ID.supabase.co
SUPABASE_ANON_KEY=YOUR_SUPABASE_ANON_KEY
```

## Commands

```bash
./gradlew test
./gradlew assembleDebug
```

The local `gradlew` script downloads Gradle 9.4.1 when it is not already available.

## MVP Scope

- Email/password Supabase auth
- Session restore and refresh
- Notes/archive list with search
- Text and checklist note create/edit/delete
- Pin/archive toggles
- Labels as comma-separated text
- Plain text storage for note and checklist content
