# ParkHere — Monetization Strategy

## Revenue Models (by priority)

### 1. Freemium — ParkHere Pro (R$9.90/month)

**Free tier:**
- Basic map, search, view spots
- 3 reports/day
- Standard filters
- Ads on map and between list results

**Pro tier:**
- Unlimited reports
- Availability predictions (ML-based, using analytics heatmap data)
- Ad-free experience
- Favorite spots alerts (push notification when someone reports near a favorite)
- Historical analytics access (full heatmap for any spot)
- Priority support
- Exclusive badge ("Apoiador")
- Export favorite spots list

**Why this works:** Daily drivers who park in the same areas repeatedly get the most value. Removing the report limit + ads is an easy sell.

**Implementation needed:**
- User subscription tier (FREE/PRO) field + migration
- Report rate limiting for free users (infrastructure exists)
- Ad integration (Google AdMob / AdSense)
- Gate analytics tab behind Pro
- Push notification system for favorite spot alerts

---

### 2. Verified Partner Listings (B2B — R$99-299/month)

Formal parking lots, malls, and garages pay to be "verified partners" on the map.

**What partners get:**
- Highlighted pin (gold/branded) on the map
- Logo and photos displayed on their spot detail page
- "Verificado" badge with partner branding
- Real-time availability sync (API integration)
- Analytics dashboard showing how many users viewed/navigated to their lot
- Priority placement in search results within radius

**What ParkHere gets:**
- Monthly subscription fee per location
- Commission on future booking integration (phase 2)
- Guaranteed data quality for partner spots

**Implementation needed:**
- Partner entity + admin panel for partner management
- Verified badge component + highlighted map pin style
- Partner analytics dashboard (views, navigations, clicks)
- API for partners to push real-time availability
- Spot type: add PARTNER_LOT or a `verified` boolean

---

### 3. Location-Based Advertising

Non-intrusive promoted spots in search results and list view.

**Ad formats:**
- Promoted spot card in list view ("Patrocinado" tag)
- Banner at bottom of spot detail page
- Interstitial after 5th map interaction (free users only)

**Revenue:** CPC (R$0.50-2.00 per click) or CPM

**Implementation needed:**
- Ad slot components (list view, detail page)
- AdMob/AdSense integration for free users
- "Patrocinado" badge on promoted spots
- Pro users skip all ads

---

### 4. Data Licensing (B2B2G)

Anonymized, aggregated parking data sold to cities and urban planners.

**Data products:**
- Occupancy patterns by neighborhood (hourly/daily/weekly)
- Informal charge hotspot maps (flanelinha density)
- Price distribution by zone
- Demand vs. supply analysis per region
- Zona azul effectiveness metrics

**Buyers:**
- Municipal governments (Porto Alegre, Curitiba, etc.)
- Urban planning firms
- Real estate developers (parking demand near new buildings)
- Insurance companies (theft/damage correlation with parking areas)

**Revenue:** Annual data subscription (R$5K-50K per city)

**Implementation needed:**
- Data export/API for aggregated anonymous analytics
- Dashboard for B2B clients
- Privacy compliance (LGPD — anonymization guarantees)
- Sales materials / data sample generator

---

### 5. Zona Azul Digital Integration

Partner with municipal operators to sell zona azul credits in-app.

**Flow:** User finds a zona azul spot -> buys credit in ParkHere -> ParkHere pays operator -> keeps commission

**Revenue:** 5-15% commission per transaction

**Implementation needed:**
- Payment gateway integration (Stripe, PagSeguro, or Pix)
- Zona azul operator API integration
- In-app purchase flow on spot detail for ZONA_AZUL spots
- Transaction history for users

---

## Implementation Roadmap

### Phase 1 — Build User Base (current)
- **Goal:** 1,000 DAU in Porto Alegre
- **Focus:** Free app, maximum virality
- **Features:** Share spot (done), onboarding (done), gamification (done)
- **Monetization:** None — pure growth
- **Key metric:** Weekly active reporters

### Phase 2 — First Revenue (1K DAU)
- **Goal:** Validate willingness to pay
- **Features to add:**
  - Ad integration for free users (AdMob banners)
  - Report daily limit for free users (3/day)
  - Pro subscription with unlimited reports + ad-free
  - Subscription management page
- **Revenue target:** R$500-2K/month (ads + early Pro adopters)

### Phase 3 — B2B Revenue (5K DAU)
- **Goal:** Parking lot partnerships
- **Features to add:**
  - Verified partner badge + highlighted pins
  - Partner analytics dashboard
  - Partner self-service registration portal
  - Real-time availability API for partners
- **Revenue target:** R$5K-15K/month

### Phase 4 — Scale (10K+ DAU)
- **Goal:** Multi-city expansion + data monetization
- **Features to add:**
  - Data licensing dashboard for B2B/government
  - ML-based availability predictions (use heatmap data)
  - Zona azul integration (if partnerships secured)
  - City selector + multi-city support
- **Revenue target:** R$20K-50K/month

---

## Competitive Moat

### Why Waze/Google won't kill this

1. **Focus asymmetry** — parking is our entire product, their 0.1% feature
2. **Local domain knowledge** — flanelinha culture, zona azul rules, Brazilian pricing
3. **Community network effects** — trust scores + gamification create switching costs
4. **Data depth** — hourly availability patterns, informal charge mapping, safety ratings — granularity no general app will match
5. **Speed** — we ship parking features weekly, they ship parking features yearly

### Real risks

1. **Cold start** — need critical mass of reporters per neighborhood
2. **Data decay** — trust score decay system mitigates this
3. **Monetization timing** — too early kills growth, too late kills runway
4. **99/iFood-style competitor** — a Brazilian super-app adding parking vertical
5. **Regulatory** — zona azul integration requires government partnerships

### Defense strategy

- Own Porto Alegre data first (bars, hospitals, nightlife areas)
- Expand city-by-city: Curitiba -> Florianopolis -> Sao Paulo
- Build community loyalty (badges, streaks, leaderboards — done)
- Make data quality unmatchable through GPS-verified crowd-sourcing
- If acquired — that's the ideal exit (Waze model: $1.1B by Google)

---

## Key Metrics to Track

| Metric | Phase 1 Target | Phase 2 Target | Phase 3 Target |
|--------|:--------------:|:--------------:|:--------------:|
| DAU | 100 | 1,000 | 5,000 |
| Weekly reports | 500 | 5,000 | 25,000 |
| Spots with >5 confirmations | 50 | 500 | 2,000 |
| Pro subscribers | — | 50 | 500 |
| Partner listings | — | — | 20 |
| MRR (monthly recurring revenue) | R$0 | R$2K | R$15K |
