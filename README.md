# Vocal Craft: AI-Powered Content Creation

## Overview

Vocal Craft is an Android application that leverages AI to empower users in creating visual content, such as posters, social media graphics, and other visual materials. The application accepts voice and text prompts, allows users to upload their own images, and utilizes AI for generating and editing the content.

## Core Features

*   **Authentication:**
    *   Google Sign-In
    *   Email/Password Sign-In & Sign-Up
*   **Image Input:**
    *   Users can upload images accompanied by captions.
    *   Images are stored on Dropbox.
    *   Image metadata is stored on Firebase.
*   **Prompt-Based Generation:**
    *   Accepts direct text prompts.
    *   Accepts voice prompts, which are transcribed using an API, and the resulting text is used.
    *   Sends prompts to an API for Named Entity Recognition (NER) and to establish an initial content structure.
*   **Dynamic Content Editing (NER Form):**
    *   Displays NER results in an editable form.
    *   Allows users to modify the extracted information.
    *   Saves the refined content structure to Firebase.
*   **Visual Canvas (SVG/Image Editor):**
    *   Generates a visual composition based on the refined NER data and a matched uploaded image.
    *   Allows dragging, resizing, and rotating of text elements.
    *   Allows editing of text content, font, size, and color.
    *   Allows dragging and resizing of a primary object image.
    *   Supports object replacement via inpainting:
        *   The user provides a caption.
        *   The system finds a matching uploaded image.
        *   An API provides a masked version of the image to replace the object.
*   **Image Resizing & Output:**
    *   Resizes the final creation to various aspect ratios (e.g., Square, Portrait, Landscape) using an API.
    *   Saves the final image to the device gallery.
    *   Uploads the final image to Dropbox.
*   **Prompt History:**
    *   Saves past text prompts for easy reuse.
*   **User Profile (Partial Implementation):**
    *   UI for setting company name and theme colors.
    *   Saving functionality is not fully implemented.
*   **Templates (Read-Only):**
    *   Displays a sample template structure.

## Application Workflow

1.  **Authentication:** The user signs in or signs up.
2.  **(Optional) Image Upload:** The user uploads images with captions via `ImageInsertActivity`.
3.  **Prompting:** The user enters a text or voice prompt in `PromptActivity`. Voice prompts are transcribed.
4.  **NER & Refinement:** The prompt is sent for Named Entity Recognition. The results are displayed in `NERActivity` for user review and refinement. The refined data is saved.
5.  **Image Generation & Editing:** The refined data, along with a matched image (from `ImageInsertActivity`, based on caption/item name), is used to generate a visual in `SvgActivity`. The user can edit text elements, manipulate objects, and perform inpainting if desired.
6.  **Finalization:** The user selects a desired aspect ratio. The composite image is sent to an API for resizing. The final image is saved to the device's gallery and uploaded to Dropbox.
7.  **Other Features:** Users can access `ProfileActivity` (for profile settings) and `TemplateActivity` (to view templates) from `PromptActivity`.

## Code Structure

*   **`MainActivity.kt`**: Splash screen that displays the app logo and transitions to `SigninMenuActivity`.
*   **`SigninMenuActivity.kt`**: Handles user authentication. Provides options for Google Sign-In, email/password sign-in (navigating to `SignInActivity`), and email/password sign-up (navigating to `SignUpActivity`). Manages transitions to `ImageInsertActivity` upon successful Google login.
*   **`SignInActivity.kt`**: Handles user sign-in with email and password using Firebase Authentication.
*   **`SignUpActivity.kt`**: Handles new user registration with email and password using Firebase Authentication.
*   **`ImageInsertActivity.kt`**: Allows users to select multiple images from their device, add captions to each, and upload them. Images are uploaded to a pre-configured Dropbox account, and the image metadata (filename, caption, Dropbox URL) is stored in Firebase Realtime Database under the `/input_image` path.
*   **`PromptActivity.kt`**: Main screen for user input. Users can type text prompts or record voice prompts. Voice is sent to an external API for transcription. Text prompts are sent to an external API for Named Entity Recognition (NER). Displays a history of prompts from Firebase, and allows navigation to `ProfileActivity` and `TemplateActivity`.
*   **`NERActivity.kt`**: Receives JSON data from the NER API (via `PromptActivity`). Dynamically generates an editable form based on this JSON. Users can review and modify the extracted entities. The refined JSON is saved back to Firebase (updating the corresponding prompt's `nerText`). It then attempts to match an item name from the JSON to an uploaded image's caption in Firebase to find an `image_link` for the next step, navigating to `SvgActivity`.
*   **`SvgActivity.kt`**: The main visual editing canvas.
    *   Receives JSON data, the original prompt, and a matched image URL from `NERActivity`.
    *   Sends this data to an external API to generate an initial visual composition (background image, a primary object image with position, and text elements with style).
    *   Displays the background, object, and text elements.
    *   Allows users to drag and resize the object image (with pinch-to-zoom).
    *   Allows users to drag, resize (pinch-to-zoom), and edit text elements (content, font, size, color via a dedicated panel).
    *   Supports object replacement via an inpainting API: user enters a caption, a matching image is found in Firebase, and its URL is sent to the API to get a new version of the object.
    *   Finally, captures the entire composition, sends it to a resizing API with a selected aspect ratio, and the resulting image is saved to the device gallery and uploaded to Dropbox.
*   **`ProfileActivity.kt`**: Allows users to input a company name and pick primary/secondary theme colors using a color picker. Currently, these settings are not persistently saved.
*   **`TemplateActivity.kt`**: Displays a read-only, dynamically generated form based on a hardcoded JSON string representing a sample template (e.g., a menu structure).

## Setup Instructions

### Prerequisites
*   Android Studio (latest stable version recommended)
*   Firebase Account ([https://firebase.google.com/](https://firebase.google.com/))
*   Dropbox Account ([https://www.dropbox.com/](https://www.dropbox.com/)) for image storage.
*   Access to the external APIs (currently hosted at `http://35.171.159.55:8000`). Ensure this endpoint is accessible.

### 1. Clone the Repository
```bash
git clone <repository_url>
cd <repository_directory>
```
(Replace `<repository_url>` and `<repository_directory>` with actual values if known, otherwise use placeholders.)

### 2. Firebase Setup
1.  Go to the [Firebase console](https://console.firebase.google.com/).
2.  Create a new project (or use an existing one).
3.  Add an Android app to your Firebase project:
    *   **Package name:** `com.aliashraf.vocalcraft` (You can find this in `app/build.gradle.kts` -> `namespace` or `applicationId`).
    *   Provide a nickname (optional).
    *   Download the `google-services.json` file.
4.  Place the downloaded `google-services.json` file into the `app/` directory of your cloned project.
5.  In the Firebase console, enable the following services:
    *   **Authentication:** Enable "Google" and "Email/Password" sign-in methods.
    *   **Realtime Database:** Create a database (choose appropriate rules for development, e.g., test mode, or secure rules for production).
    *   **Storage (Optional but Recommended):** While the app currently uses Dropbox for primary image uploads, Firebase Storage might be used or integrated later. It's good practice to enable it.

### 3. Dropbox Setup
1.  Go to the [Dropbox App Console](https://www.dropbox.com/developers/apps) and create a new app.
    *   Choose "Scoped access."
    *   Choose "Full Dropbox" or "App folder" access depending on your preference (App folder is generally safer).
    *   Give your app a unique name.
2.  Once the app is created, go to the "Permissions" tab and ensure the following are checked:
    *   `files.content.write`
    *   `files.content.read`
    *   `sharing.write` (to create shared links)
3.  Go to the "Settings" tab and find the "Generated access token". Generate one if it's not already there.
4.  **Important:** This access token needs to be placed into the application code where Dropbox API calls are made. Currently, it's hardcoded in:
    *   `app/src/main/java/com/aliashraf/vocalcraft/ImageInsertActivity.kt`
    *   `app/src/main/java/com/aliashraf/vocalcraft/SvgActivity.kt`
    *   Replace the placeholder `dropboxAccessToken = "YOUR_ACCESS_TOKEN"` with your actual generated token in these files.
    *   **Security Note:** For production apps, hardcoding access tokens is not recommended. Consider using a secure backend to manage tokens or using Android's Keystore system.

### 4. API Endpoint Configuration
The application relies on external APIs for transcription, NER, image generation, inpainting, and resizing. These are currently hardcoded to `http://35.171.159.55:8000`.
*   Ensure this endpoint is correct and accessible from your development environment/device.
*   If the API endpoint changes, you will need to update it in the following files:
    *   `app/src/main/java/com/aliashraf/vocalcraft/PromptActivity.kt` (for `/transcribe` and `/ner`)
    *   `app/src/main/java/com/aliashraf/vocalcraft/SvgActivity.kt` (for `/generate_image`, `/inpainting`, and `/resizer`)

### 5. Build and Run
1.  Open the cloned project in Android Studio.
2.  Let Android Studio sync Gradle files and download dependencies. This might take a few minutes.
3.  If prompted, ensure you have the correct Android SDK versions installed (target SDK is 34, min SDK is 24, compile SDK is 34, as per `app/build.gradle.kts`).
4.  Connect an Android device or start an emulator.
5.  Click the "Run" button (green play icon) in Android Studio.

### Additional Notes
*   **Dependencies:** The project uses several third-party libraries (e.g., Firebase, OkHttp, Glide, Picasso, AmbilWarnaDialog). These are managed by Gradle and listed in `app/build.gradle.kts` and `gradle/libs.versions.toml`.
*   **Permissions:** The app requests permissions like `RECORD_AUDIO`, `INTERNET`, and storage permissions. Ensure these are granted on the device when prompted.
*   **API Keys Security:** The current setup includes hardcoded API keys (Dropbox token) and IP addresses. For a production environment, these should be secured appropriately (e.g., using environment variables, a secure backend proxy, or Android Keystore).
