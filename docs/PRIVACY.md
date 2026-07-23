# Privacy Policy

**Klink** · Last updated: July 2026

This Privacy Policy explains what information Klink collects from churches and their members, how it is used, and how it is protected. Klink is a multi-church management platform — each church that registers gets its own isolated workspace, and every query to our database is scoped to that church's ID, so no church can see another church's data.

> This document describes Klink's actual data practices. It is written for transparency with churches and members, not as a substitute for legal advice — have it reviewed by a lawyer familiar with your jurisdiction before publishing it as a binding policy.

## 1. Who this applies to

- **Churches** that register a workspace on Klink (the "Pastor" who signs up, and any Elders/Managers/Financial Secretaries they appoint)
- **Members** who join a church's workspace using its join code

## 2. Information we collect

### Provided directly by users
- Name, phone number, email (phone or email — not both required), profile photo
- Attendance check-ins (via QR scan or leader marking)
- Giving records: tithe, offering, welfare, pledges, project contributions, and store purchases
- Prayer requests (public or private)
- Poll votes
- Content leaders post: announcements, events, sermons, devotionals, gallery photos, church files

### Collected automatically
- Login sessions and device tokens (for push notifications via Firebase)
- Basic audit logs of sensitive actions (e.g. role changes, church deletion) for security purposes

### From third parties
- **Paystack**: payment confirmation data when a member gives online (card or Mobile Money). Klink never stores full card numbers — Paystack's hosted payment page handles card details directly, and our backend only receives and verifies a transaction reference.

## 3. How we use information

| Data | Purpose |
|---|---|
| Name, phone, email | Account creation, login, identifying members within their church |
| Attendance | Service/event attendance tracking, reporting to leaders |
| Giving & pledges | Financial record-keeping, receipts, welfare-defaulter reminders |
| Prayer requests | Routed to the church's Pastor/Elders for response; private requests are visible only to the author and leaders |
| Photos | Profile display, galleries, facility/project records |
| Device tokens | Push notifications (announcements, reminders, receipts) |
| Audit logs | Security and accountability for sensitive actions |

We do not sell member data, and we do not use member data for advertising.

## 4. Who can see what

Access is role-based and enforced at the database query level:

- **Regular members** can see the public member directory (name, phone, photo only), their own attendance/giving/pledge history, and public prayer requests.
- **Leaders** (Pastor, Elder, Manager, Financial Secretary) see more, scoped strictly to their role — e.g. only the Financial Secretary and church leadership see financial summaries; only Pastors/Elders see private prayer requests.
- **Group Admins/Fin-Secs** see only their group's roster and group-level finances, which never mix with church-wide finances.
- No member or leader from one church can see another church's data. Every query is filtered by the church ID from the authenticated user's login token — never from data supplied in the request.

## 5. Data storage & security

- Data is stored in a PostgreSQL database and file storage hosted by **Supabase**.
- Passwords are hashed with BCrypt (cost factor 12); refresh tokens and verification codes are also hashed, never stored in plain text.
- Authentication uses short-lived (15-minute) access tokens and rotating 30-day refresh tokens; a stolen refresh token is automatically invalidated across the whole "family" of tokens.
- Uploaded files are validated by their actual content (magic bytes), not just their claimed file type, to prevent disguised malicious uploads.
- All traffic is encrypted (HTTPS/TLS); security headers (HSTS, CSP, frame-deny) are enforced.
- Login attempts are rate-limited and accounts can be locked after repeated failures; login responses are designed so an attacker can't tell whether an email is registered.

## 6. Third-party services we use

| Service | What it receives |
|---|---|
| Supabase | All database records and uploaded files |
| Paystack | Payment amount, type, and the giving member's identifier — for processing online payments |
| Twilio | Phone number — for SMS verification codes and reminders |
| Gmail (Google) | Email address — for email verification, password resets, and receipts |
| Firebase (Google) | Device push token — for notifications |

Each of these providers has its own privacy policy governing how they handle the data passed to them.

## 7. Data retention & deletion

- **Church deletion**: Only an Elder can delete a church. Deleted churches enter a 30-day grace period during which the deletion can be reversed. After 30 days, all associated data — database records and stored files — is permanently and automatically purged.
- **Member accounts**: Members can leave a church at any time from within the app. Leaders can deactivate a member, which restricts their access without necessarily deleting their historical records (attendance/giving history may be retained for the church's own record-keeping).
- **Soft deletes**: Some records are soft-deleted (marked inactive rather than immediately erased) to preserve financial and historical integrity, but they are excluded from normal views and directory listings.

## 8. Your choices

- You can edit your profile information at any time.
- You can submit prayer requests privately or publicly, at your choice.
- You can leave a church at any time.
- You can request account/data deletion by contacting your church's leadership or Klink support (see Contact, below).

## 9. Children's privacy

Klink is intended for use by church members generally, including participation by minors under a parent's or guardian's supervision as part of normal church life (e.g. attendance, event sign-ups). Churches and leaders are responsible for obtaining any consent required by local law before registering a minor's information.

## 10. Changes to this policy

We may update this policy as Klink's features evolve. Material changes will be communicated to church leaders, who are responsible for informing their members.

## 11. Contact

Questions about this policy or requests regarding your data can be directed to your church's Pastor or Elder, who can escalate to the Klink team as needed.
