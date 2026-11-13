# CS4530Project_u1511875_u1458335_u1422313

Ryan Hoo - u1511875
Connor Kolodziej - u1458335
Luke Stansbury - u1422313

## Google Cloud Vision API setup

Images imported from the gallery's **Import Image** button (or opened with a tile's **Analyze** button) are sent to the
Google Cloud Vision API for object localization, labels, text detection and face detection.

1. In the Google Cloud console, create or pick a project, link a billing account (required even for the free tier;
   the first 1,000 units per feature each month are free), and enable the **Cloud Vision API**.
2. Under **APIs & Services > Credentials**, create an API key. Restricting it to the Cloud Vision API is recommended.
3. Add it to `Drawing_App/local.properties` (this file is git-ignored, so the key is never committed):
   ```
   CLOUD_VISION_API_KEY=your-key-here
   ```
   You can also set a `CLOUD_VISION_API_KEY` environment variable instead.
4. Sync Gradle and rebuild. The key is read into `BuildConfig` at build time.
