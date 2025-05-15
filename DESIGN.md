## The City And The Bike (TCATB)- Application Design Document

**Author:** Brandon Martin-Anderson

**1. Introduction**
TCATB is a mobile application designed for users to discover, photograph, and catalogue graffiti tags found on the rear fenders of rental bikes (initially focusing on Lime bikes in Seattle). Users scan a bike's QR code, capture an image of its fender art using a guided interface, and the app processes the image for uniformity. The platform allows users to explore their own collections, view all art associated with a specific bike, and browse a global feed of all submissions. The project aims to celebrate ephemeral urban art and create a simple, engaging spotting game.

**2. Goals and Objectives**
*   **Primary Goal:** To provide a platform for users to easily capture and share standardized images of bike fender graffiti.
*   **Secondary Goals:**
    *   To create a visual database of fender art, linked to specific bikes.
    *   To foster a community of urban art enthusiasts.
    *   To recognize the ephemeral nature of street art through digital preservation.
    *   To lay the groundwork for future gamification and advanced filtering features.
*   **Non-Goals (for V1):**
    *   Tag identification, keyword entry for filtering, or advanced search by tag content.
    *   Complex social networking features beyond basic feeds.
    *   Monetization.
    *   Leaderboards or streaks.

**3. Target Audience**
*   Urban explorers and residents interested in street art.
*   Individuals who enjoy "spotting" or "collecting" unique items.
*   Photography hobbyists looking for novel subjects.
*   Users of rental bike services (initially Lime users in Seattle).

**4. Core Features (MVP V1.0)**

*   **4.1. User Authentication:**
    *   **UAC-001:** Users can register for a new account using email and password. (Accounts can be pseudonymous via username selection).
    *   **UAC-002:** Registered users can log in to their account.
    *   **UAC-003:** Basic profile (username, email).

*   **4.2. Bike Identification & Art Capture:**
    *   **BIC-001:** Users can initiate a scan using the phone's camera to read a bike's QR code.
    *   **BIC-002:** The app decodes the QR code to retrieve the unique bike identifier.
    *   **BIC-003:** After successful QR scan, the app opens a camera interface for photographing the rear fender. Short instructions will be provided on-screen.
    *   **BIC-004:** The camera interface displays a semi-transparent overlay matching the shape of a standard Lime bike fender to guide user framing and angle.
    *   **BIC-005:** Upon photo capture, the app automatically warps and crops the image based on the fender overlay geometry to create a standardized view of the tag.
    *   **BIC-006:** The user is shown a preview of the processed (warped/cropped) image and can choose to retake the photo or accept it for submission.
    *   **BIC-007:** The accepted photo, bike ID, user ID, timestamp, and GPS location (if permission granted) are submitted to the backend. An optional text field for a user's private note/caption about the submission can be included.

*   **4.3. Viewing & Discovery:**
    *   **VID-001 (Personal Grid):** Logged-in users can view a gallery of all fender art images they have personally submitted, sorted chronologically.
    *   **VID-002 (Per-Bike Grid):** Users can view all submitted fender art images associated with a specific bike ID (e.g., by searching for a bike ID or tapping on a bike in another view).
    *   **VID-003 (Global Feed / City-Wide Grid):** Users can view a chronological feed (grid display) of all fender art images submitted by all users.

*   **4.4. Data Storage & Synchronization:**
    *   **DSS-001:** All submitted data (images, metadata) is stored on the backend.
    *   **DSS-002:** The mobile app synchronizes data with the backend for display and submission.

**5. Technical Architecture**

*   **5.1. Mobile Application (Android - Native Kotlin/Java):**
    *   **UI/UX:** Android Material Design principles.
    *   **QR Code Scanning:** ZXing library (or similar).
    *   **Camera:** Android CameraX or Camera2 API for fine-grained control and preview.
    *   **Image Processing:**
        *   On-device using OpenCV for Android (or custom matrix transformations).
        *   The overlay will define known points. `getPerspectiveTransform` and `warpPerspective` (OpenCV) can be used for warping. Cropping will follow based on defined output dimensions.
    *   **Location Services:** Android Location Services for GPS coordinates.
    *   **Networking:** Retrofit2 and OkHttp for API communication.
    *   **Local Storage:** SQLite (Room Persistence Library) for caching user data, drafts (if implemented).

*   **5.2. Backend (Python with Flask):**
    *   **API:** RESTful API for communication with the mobile app and future web frontend.
    *   **Authentication:** JWT (JSON Web Tokens) for stateless session management.
    *   **Database:** PostgreSQL.
        *   GIS extensions (PostGIS) can be considered for future location-based queries (e.g., neighborhood definitions using shapefiles).
    *   **Image Storage:** Cloud-based object storage (e.g., AWS S3, Google Cloud Storage, Azure Blob Storage). Store both original and processed images.
    *   **Deployment:** Dockerized containers on a cloud platform (e.g., AWS ECS, Google Kubernetes Engine, Heroku).

*   **5.3. Web Frontend (Future - Phase 2):**
    *   Technology: React, Vue.js, or Angular.
    *   Functionality: Primarily for browsing the global gallery, bike-specific galleries. User profiles and potentially map views.

**6. Data Model (Conceptual - PostgreSQL for V1)**

*   **`Users` Table:**
    *   `user_id` (PK, UUID or SERIAL)
    *   `username` (VARCHAR, UNIQUE, NOT NULL)
    *   `email` (VARCHAR, UNIQUE, NOT NULL)
    *   `password_hash` (VARCHAR, NOT NULL)
    *   `created_at` (TIMESTAMP, DEFAULT NOW())
    *   `updated_at` (TIMESTAMP, DEFAULT NOW())

*   **`Bikes` Table:**
    *   `bike_qr_id` (PK, VARCHAR, extracted from QR code, UNIQUE, NOT NULL)
    *   `bike_brand` (VARCHAR, e.g., "Lime", "Bird" - for future expansion)
    *   `first_seen_at` (TIMESTAMP)
    *   `last_seen_at` (TIMESTAMP)
    *   `notes` (TEXT, optional, e.g., "Retired")

*   **`FenderSubmissions` Table:**
    *   `submission_id` (PK, UUID or SERIAL)
    *   `user_id` (FK, references `Users.user_id`, NOT NULL)
    *   `bike_qr_id` (FK, references `Bikes.bike_qr_id`, NOT NULL)
    *   `image_url_original` (VARCHAR, path to original image in cloud storage)
    *   `image_url_processed` (VARCHAR, path to warped/cropped image in cloud storage)
    *   `latitude` (DECIMAL, optional)
    *   `longitude` (DECIMAL, optional)
    *   `captured_at` (TIMESTAMP, NOT NULL, from device or EXIF)
    *   `uploaded_at` (TIMESTAMP, DEFAULT NOW())
    *   `user_caption` (TEXT, optional, user's private notes about the submission, not for public filtering in V1)

**7. User Interface (UI) / User Experience (UX) Considerations**

*   **Onboarding:** Simple tutorial explaining the capture process (QR scan, overlay usage, preview/retake).
*   **Capture Flow:**
    1.  Main screen: "Scan Bike" button, "My Folio", "Global Feed".
    2.  Tap "Scan Bike" -> QR Scanner activates.
    3.  On successful scan -> Camera view with fender overlay and brief instructions.
    4.  User aligns and captures -> Image is automatically processed.
    5.  Preview of *processed* image shown -> User can "Accept & Submit" or "Retake."
    6.  (Optional) User adds a private caption.
    7.  On "Accept & Submit" -> Image and data are uploaded.
*   **Image Display:** Clean, grid-based galleries. Tapping an image shows it larger with details (bike ID, date, user).
*   **Performance:** Fast loading of images and feeds. Image compression and optimized queries.
*   **Offline Support:** For V1, online-only for submission. Future versions could allow offline capture and queuing.
*   **Permissions:** Clearly request camera and location permissions with explanations.

**8. Non-Functional Requirements**

*   **Performance:** API response times < 500ms for most operations. Image loading should be perceptionally fast.
*   **Scalability:** Backend designed to handle growth in users and submissions. Use of cloud services aids this.
*   **Reliability:** Aim for >99.9% uptime for backend services.
*   **Security:** Secure storage of user credentials (hashing passwords). HTTPS for all communications. Input validation to prevent common web vulnerabilities.
*   **Maintainability:** Well-structured, documented code. Adherence to Android and Python/Flask best practices.

**9. Future Enhancements (Post-MVP)**

*   **Tagging & Advanced Filtering:**
    *   Allow users to add keywords/tags to submissions.
    *   Implement filtering based on these user-defined tags.
    *   Explore AI/Machine Vision for automatic tag suggestion or recognition.
*   **Gamification:**
    *   Leaderboards (city-wide, per-neighborhood). Neighborhoods defined by pre-existing shapefiles.
    *   User points and achievement badges.
    *   Submission streaks.
*   **Social Features:**
    *   Commenting on submissions.
    *   Following users.
*   **Advanced Mapping:** Displaying submissions on an interactive map.
*   **iOS Application.**
*   **Reporting/Moderation:** System for users to report inappropriate content.

**10. Open Questions & Risks**

*   **Risk: Image Processing Consistency:** Achieving perfect automatic warping/cropping across different phone cameras and lighting conditions might be challenging. The overlay and preview/retake option will help mitigate this but may require iteration.
*   **Risk: User Adoption & Engagement:** Attracting and retaining users for a niche application. Future gamification will be important for long-term engagement.
*   **Risk: Content Moderation:** Potential for users to upload non-fender images or offensive content. A reporting mechanism will be needed relatively soon post-V1.
*   **Risk: Bike Churn:** Rental bikes are frequently moved, decommissioned, or replaced. The app focuses on the art *on* the bike at a point in time. `Bikes` table `last_seen_at` can track activity.
*   **Question: Data Privacy:** Ensure compliance with GPDR/CCPA if applicable, especially concerning location data. Clear privacy policy needed.

**11. Milestones (High-Level for V1)**

1.  **Phase 1: Backend Setup & Core API**
    *   Database schema implementation (Users, Bikes, FenderSubmissions).
    *   User authentication endpoints.
    *   Bike registration and submission endpoints (including image upload handling to cloud storage).
2.  **Phase 2: Android App - Core Functionality**
    *   User registration/login UI.
    *   QR scanning integration.
    *   Camera interface with overlay and instructions.
    *   On-device image processing (warp/crop).
    *   Preview & Retake/Submit flow (image + optional caption).
3.  **Phase 3: Android App - Viewing & Discovery**
    *   "My Photos" screen (Personal Grid).
    *   "Bike's Photos" screen (Per-Bike Grid, accessible via search or tap).
    *   "Global Feed" screen (City-Wide Grid).
4.  **Phase 4: Testing & Refinement**
    *   Internal testing.
    *   Alpha/Beta testing with a small group of users in Seattle.
    *   Bug fixing and performance optimization.
5.  **Phase 5: V1 Launch**
    *   Deployment to Google Play Store.
    *   Initial marketing/outreach.