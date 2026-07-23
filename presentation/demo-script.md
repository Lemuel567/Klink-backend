# Klink — Demo Script

Companion to `klink-presentation.pptx`. Target run time: **8–10 minutes** of live app demo, following the slide deck's narrative. Use two devices logged in as different roles (a leader account and a member account) so role differences are visible without switching accounts mid-demo.

**Setup before you present:**
- One phone logged in as **Pastor** (or Elder/Manager)
- One phone logged in as a **Member**
- Wi-Fi/hotspot confirmed working; backend awake (ping it a few minutes beforehand if hosted on a free tier with cold starts)
- A test service/event already created so attendance has something to check into
- Paystack sandbox ready (test card: `4084 0840 8408 4081` · CVV `408` · any future expiry · PIN `0000` · OTP `123456`)

---

## 1. Open — the pitch (30 sec)

> "Klink is one app that runs a church's entire congregational life — membership, attendance, finances, giving, sermons, events, groups, and communication — with every church's data completely isolated from every other church."

Show the **login screen** briefly, then move straight into the app.

## 2. Registration & join code (1 min)

- On the **leader phone**: show the church's join code (Settings → join code).
- On the **member phone**: show self-registration with that code — email or phone only, 6-digit verification.
- Callout: *"Ghanaian numbers auto-convert to +233 format automatically."*

## 3. Roles in action (1.5 min)

- On the leader phone, open the **member directory** — show role badges (Pastor, Elder, Manager, Financial Secretary, Member).
- Briefly explain the checks-and-balances design: *"A Manager can demote a Pastor or Elder if needed, but can't touch money. Only the Financial Secretary can record finances."*
- No need to demo every role — name-drop the two or three most interesting constraints (immutable polls, group money isolation, 30-day church-deletion grace period) rather than walking through all seven.

## 4. Attendance (1.5 min)

- Leader phone: generate the **service QR code**.
- Member phone: **scan to check in** — let the celebration animation play, it's a nice "wow" beat.
- Leader phone: show the **live attendance list** updating.
- Mention: automatic absent-marking after the session expires; manual marking for non-smartphone members.

## 5. Online giving (2 min) — the centerpiece

- Member phone: **Give Now** → choose amount + type (tithe/offering/welfare/etc.).
- Walk through the **Paystack hosted page** using the sandbox test card.
- Point out: *"The backend verifies every transaction server-side and writes the ledger exactly once — no duplicate or lost payments even on a flaky connection."*
- Show the **e-receipt** (email/push) and the giving history screen.
- Leader phone: show the **finance summary** the Financial Secretary sees.

## 6. Communication & worship content (1.5 min)

- Leader phone: post an **announcement** targeted at a specific group or role.
- Member phone: show the **push notification** arriving, then the **read receipt** back on the leader phone.
- Quickly show the **sermon player** (note it pauses the background worship music) and the **daily devotional** on Home.

## 7. Groups & polls (1 min)

- Leader phone: create a quick **poll** — emphasize it's immutable once created.
- Member phone: **vote**, then show live-updating results with percentage bars.
- One line on groups: *"Each ministry group runs its own dues and finances, fully isolated from church-wide money."*

## 8. Close — security & scale (30 sec)

Return to the deck's Security slide (or state it live):

> "Every one of those screens is scoped to the church's ID from the login token — never from the request — so no church can ever see another church's data. That's backed by 66 automated tests running in CI on every push."

## 9. Q&A

Keep the leader and member phones open on the **Home** screen for follow-up questions.

---

### If something breaks live

- **Cold backend / slow first request**: acknowledge it, mention the free-tier cold-start tradeoff (see `docs/DEPLOYMENT.md`), and continue — it'll be fast on the second request.
- **Payment sandbox hiccup**: fall back to showing a **previously completed** giving history entry and describe the flow verbally.
- **Push notification delay**: continue talking through the next section; circle back if it arrives.
