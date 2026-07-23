# Deployment Guide

How to take Klink from local development to a live, publicly-reachable app.

Klink has two deployable pieces:

1. **Backend** — Java 17 + Spring Boot 3.5 REST API (`Klink-backend`)
2. **Frontend** — React Native + Expo SDK 54 mobile app (`Klink-frontend`)

Supabase (Postgres + Storage), Twilio, Gmail SMTP, Paystack, and Firebase are already hosted third-party services — nothing to deploy there, just production credentials to configure (see `docs/CONFIGURATION.md` / section 03 of the overview).

---

## 1. Backend hosting

The backend is a stateless Spring Boot JAR with no local file storage (all uploads go to Supabase Storage), so it can run on almost any container/PaaS platform. Recommended options, cheapest/simplest first:

| Platform | Why | Cost |
|---|---|---|
| **Render** | Push a Dockerfile or connect the GitHub repo, zero-config SSL, easiest for a first deploy | Free tier (cold starts after inactivity) → ~$7/mo for always-on |
| **Railway** | GitHub-native deploys, clean UI, good logs | $5/mo usage credit, pay-as-you-go after |
| **Fly.io** | Deploys close to users, good for low-latency global access | Free allowance, then usage-based |
| **DigitalOcean App Platform / Droplet** | More control, predictable pricing | From ~$4–6/mo |

**Recommendation for launch:** start on **Render** — it's the fastest path from GitHub repo to a live HTTPS URL and needs no server management. Move to Railway/Fly/DigitalOcean later if traffic or cold-start latency becomes a problem.

### Steps (Render)

1. Push a `Dockerfile` to the repo root (or use Render's native Java/Gradle-Maven build — a Dockerfile gives more control over the JVM flags below).
2. In Render: **New → Web Service → connect `Lemuel567/Klink-backend`**.
3. Set the build/start command (if not using Docker):
   - Build: `./mvnw clean package -DskipTests`
   - Start: `java -jar target/*.jar`
4. Add all environment variables from section 03 of the overview doc (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `SUPABASE_URL`, `SUPABASE_SERVICE_KEY`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_FROM_NUMBER`, `PAYSTACK_SECRET_KEY`, `PAYSTACK_PUBLIC_KEY`, `PAYSTACK_CALLBACK_URL`, `PAYSTACK_WEBHOOK_SECRET`, `FIREBASE_CREDENTIALS_JSON`, `CORS_ALLOWED_ORIGINS`) — **with production values, not the dev/test ones**.
5. Set `HIBERNATE_DDL_AUTO=validate` in production (Flyway owns the schema; Hibernate should never auto-alter it live).
6. Set `PAYSTACK_CALLBACK_URL` to the deployed backend's public URL, and switch `PAYSTACK_SECRET_KEY` / `PAYSTACK_PUBLIC_KEY` from sandbox to live keys once payments testing is complete.
7. Deploy. Render gives a `*.onrender.com` HTTPS URL — this becomes the app's `API_BASE_URL`.
8. Point Paystack's webhook URL (in the Paystack dashboard) at `https://<your-domain>/api/v1/payments/webhook` (or whatever the actual webhook route is).
9. Confirm the `/health` endpoint (mentioned in section 06 of the overview) returns OK — wire it into Render's health check settings so it auto-restarts on failure.

### Memory note

Spring Boot needs ~512MB–1GB RAM minimum. Render's free tier (512MB) is tight but workable for a demo; the paid starter tier is safer for anything beyond a presentation.

### JAR minification / cold start

If using the free tier and cold starts (10–30s) are a problem for the demo, keep the service "warm" with a scheduled ping (e.g. a free [cron-job.org](https://cron-job.org) hit every 10 minutes) during the demo window, or upgrade to an always-on paid instance for the presentation itself.

---

## 2. Frontend (mobile app) deployment

The frontend is built and shipped with **EAS (Expo Application Services)** — Expo's own build/submit pipeline. No traditional "hosting" is needed since it's a native app, not a website.

### One-time setup

```bash
npm install -g eas-cli
eas login
eas build:configure
```

This generates `eas.json` with build profiles (`development`, `preview`, `production`).

### Build profiles

- **development** — for internal testing (Expo Dev Client)
- **preview** — internal distribution build (installable `.apk`/`.ipa` without going through the app stores) — good for the presentation/demo
- **production** — the store-ready build

### Building

```bash
eas build --platform android --profile preview   # quick installable APK for demo devices
eas build --platform all --profile production     # store-ready builds
```

### Submitting to the stores

```bash
eas submit --platform android
eas submit --platform ios
```

Requires a Google Play Developer account ($25 one-time) and an Apple Developer account ($99/yr). For the presentation itself, a **preview build installed directly on demo phones** is enough — store submission can happen after launch review.

### Environment configuration

Set `ENV=prod` (see `src/utils/constants.ts`, section 03 of the overview) and the production backend's URL before building the production profile, so the app talks to the deployed Render backend instead of the local dev tunnel.

### Over-the-air updates

Once live, `eas update` pushes JS/asset-only fixes to users instantly without a new store review — useful for fast post-launch bug fixes.

---

## 3. CI/CD

GitHub Actions already runs the 66 backend unit tests and the frontend's strict TypeScript check on every push (section 07 of the overview). Recommended addition: extend the backend workflow so a merge to `main` also triggers a Render deploy (Render supports auto-deploy on push out of the box — no extra Action needed, just enable "Auto-Deploy" on the service).

---

## 4. Pre-launch checklist

- [ ] All `.env` values rotated from dev/test to production credentials (Supabase, Twilio, Gmail, Paystack, Firebase)
- [ ] `PAYSTACK_SECRET_KEY` / `PAYSTACK_PUBLIC_KEY` switched from sandbox to live
- [ ] `HIBERNATE_DDL_AUTO=validate` in production
- [ ] `CORS_ALLOWED_ORIGINS` restricted to the real app's origin(s), not dev tunnels
- [ ] Backend health check wired into the hosting platform's monitoring
- [ ] Frontend `ENV` and API URL pointed at the production backend before building
- [ ] Preview build installed on demo devices ahead of the presentation
