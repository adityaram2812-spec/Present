# Present Timetable Vision Backend

This is a standalone, lightweight Node.js + Express HTTP backend designed to extract highly structured timetable data from images using the official Google Gemini Vision API (`gemini-3.8-flash`).

It is built to run locally on your development PC (accessible over Wi-Fi) and is fully ready to be deployed to Google Cloud Run in production.

## 1. Prerequisites
- Node.js (v18+)
- Active Google Gemini API Key

## 2. Installation
1. Open a terminal in this `backend` directory.
2. Install dependencies:
   ```bash
   npm install
   ```

## 3. Configuration
1. Create a `.env` file in this `backend` directory.
2. Add your Gemini API key (NEVER commit this file):
   ```
   GEMINI_API_KEY=your_actual_api_key_here
   PORT=8080
   ```
   (Alternatively, set it in your PC's environment variables).

## 4. Starting the Server
1. Run the server:
   ```bash
   npm start
   ```
2. The server will output exactly which URLs you can use, including your local network IP (e.g., `http://192.168.1.5:8080`).

## 5. Endpoints
The Android app must be updated to interact with this backend instead of processing locally.

### Health Check (GET)
Use this to verify the server is reachable dynamically:
`GET http://<YOUR_IP>:8080/health`

**Response:**
```json
{ "status": "ok", "service": "present-timetable-backend" }
```

### Extraction (POST)
Upload the image file to this endpoint via `multipart/form-data`:
`POST http://<YOUR_IP>:8080/api/extract-timetable`

**Form Data:**
- Key: `image`
- Value: (The raw image file bytes)

**Response Format:**
```json
{
  "division": "A",
  "groups_found": ["A", "B", "C"],
  "classes": [
    {
      "day": "Monday",
      "startTime": "09:00",
      "endTime": "10:00",
      "subject": "CM",
      "teacher": "TN",
      "room": "311",
      "group": "shared"
    }
  ]
}
```
*(The server parses this dynamically from the Vision model without storing the uploaded image permanently).*
