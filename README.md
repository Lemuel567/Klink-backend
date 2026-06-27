# Klink Backend

Klink is a comprehensive church management platform built for modern churches. It empowers pastors, financial secretaries, and church managers to digitize their operations — from member registration and attendance tracking to tithe recording, project fundraising, sermon libraries, and real-time announcements. Built with security and multi-tenancy at its core, Klink supports multiple churches on a single platform while keeping each church's data completely isolated and protected.

## Tech Stack
- Java 17
- Spring Boot 3
- Spring Security 6
- PostgreSQL (Supabase)
- JWT Authentication
- Firebase FCM
- Twilio SMS

## Features
- Multi-tenant architecture
- JWT authentication with refresh token rotation
- Role-based access control: PASTOR, ELDER, MANAGER, FINANCIAL_SECRETARY, GROUP_ADMIN, GROUP_FINANCIAL_SECRETARY, MEMBER
- Member management
- Giving and tithe tracking
- Project fundraising
- Sermon library
- Announcements
- Events and attendance
- Push notifications via Firebase FCM
- SMS verification via Twilio
- Church projects and facilities management

## Required Environment Variables
Create a `.env` file with these variables:
```
DB_URL=your_supabase_database_url
DB_USERNAME=your_database_username
DB_PASSWORD=your_database_password
JWT_SECRET=your_jwt_secret_key
SUPABASE_URL=your_supabase_url
SUPABASE_SERVICE_KEY=your_supabase_service_key
MAIL_USERNAME=your_gmail_address
MAIL_PASSWORD=your_gmail_app_password
TWILIO_ACCOUNT_SID=your_twilio_account_sid
TWILIO_AUTH_TOKEN=your_twilio_auth_token
TWILIO_FROM_NUMBER=your_twilio_phone_number
SMS_ENABLED=true
CORS_ALLOWED_ORIGINS=your_allowed_origins
```

## How to Run
```
./mvnw spring-boot:run
```

## Firebase Setup
Place `firebase-service-account.json` in `src/main/resources/` (not committed to git).
