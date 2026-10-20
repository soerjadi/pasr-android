# Nusantara Marketplace — Design

Status: draft for approval, revised 2026-10-20 after owner answers on scope.
Working title "Nusantara" is taken from the mockups; the repo directory is `pasr`.

Conventions in this document:
- **[Decided]** = stated by the project owner.
- **[Call]** = a choice made while writing this document. Not agreed yet; the owner should check these.
- **[Open]** = only the owner can answer; it changes what gets built. Collected in section 14.

## 1. Intent

A free-to-list marketplace app for UMKM (small local businesses) in Simo, Boyolali. Buyers browse and order local products (snacks, honey, crafts, produce) and **pick them up at the seller's store** [Decided]. Sellers list for free. The business earns from **in-app ads and partner placements** and from **features still being developed** [Decided].

The mockups (`docs/mockup/`) show two things:
1. A conventional marketplace flow: splash, onboarding, home, category, product list, product detail, cart, checkout, order confirmation, tracking, order history, seller storefront, profile.
2. A monetization concept organized as three "pillars":
   - **Pillar 1: shipping hub.** A hub consolidates items from several sellers into one parcel. **Not real; deferred past the MVP** [Decided].
   - **Pillar 2: wholesale and B2B.** Retail vs "Grosir (min. 20 pcs)" pricing, a "Custom Label & Logo" paid add-on, B2B hampers (gift hampers). Data model only in the MVP.
   - **Pillar 3: partners and ads.** "Mitra Unggulan" carousel, a sponsored "Kemitraan" card (KUR Bank BRI), a post-purchase ad (J&T) on the confirmation screen, and a wait-state ad (Indosat) on the tracking screen. Cart and checkout are explicitly ad-free ("Tanpa Iklan"). **Design placeholders in the MVP; no real advertiser deals yet** [Decided].

The mockups are AI-generated. Some text is garbled, dates and totals are inconsistent (e.g. cart "Total Rp 8.000", dates in 2028). Treat them as **visual and flow direction, not a spec**. Where they disagree with this document, this document wins once approved. Section 3 lists the screens that change because there is no delivery.

## 2. MVP scope in one paragraph

Buyers in Simo browse, search, order, and track. Every order is **pickup at the seller's store**, paid **in cash at the store** [Decided]. There is **no delivery, no shipping fee, no courier, no hub, no seller self-service, no seller onboarding flow** [Decided]. The owner/operator enters sellers and products and moves orders through their states from a small operator-only admin; the seller portal is separate work in progress that will use this same backend later [Decided]. Ad slots exist in the app and show **placeholder partner content** [Decided]. It runs on a **small homelab server** at **hundreds of orders per month** [Decided].

## 3. Mockup changes caused by pickup [Call]

| Mockup screen | Change |
|---|---|
| Cart | Remove the "Ongkir (estimasi)" line. Total = items subtotal. |
| Checkout | Replace "Alamat Pengiriman" and "Metode Pengiriman" with an **"Ambil di Toko"** block per seller: pickup address and opening hours. Payment is "Bayar di toko saat mengambil" (QRIS stays "phase 2"). Remove shipping lines. |
| Product detail | Remove the "Ongkir / Estimasi 1-2 hari" cells. Keep stock. Show the seller's pickup location instead. Remove the "Hemat Ongkir via Hub" banner. |
| Order confirmation | Payment copy becomes "bayar di toko saat mengambil". Add each seller's pickup address and hours. |
| Tracking | Steps become: Pesanan dibuat → Diproses penjual → Siap diambil → Selesai. Remove "Dalam perjalanan" and "Pesanan diterima". |
| Order history | Tabs: Semua, Diproses, **Siap Diambil**, Selesai (replaces "Dikirim"). |
| Profile | Remove "Alamat Pengiriman". |
| Onboarding 2 and footer strip | Replace "pengiriman aman / estimasi 1-2 hari" copy with pickup copy. |
| Cart with several sellers | Stays; the buyer collects from each seller's store. The cart shows one pickup line per seller. |

## 4. Goals, non-goals, anti-goals

Goals
- A buyer in Simo can find a product, order it, know when it is ready, and pick it up, on a low-end Android phone with weak connectivity.
- Ad slots are in the screens the mockups show and their content is changeable without an app release.
- The data model leaves room for wholesale/custom-label pricing and, later, delivery and a hub, so those are additions, not rewrites.
- Cheap and boring to run at home.

Non-goals (MVP)
- iOS, web storefront, multi-region, in-app chat, QRIS/online payments, delivery of any kind (addresses, shipping fees, couriers, aggregator), hub, seller portal or self-registration, seller payouts, ad impression/click tracking and advertiser reporting, AdMob, recommendations.

Anti-goals (a failure even if it technically works)
- Ads in cart or checkout, or ads that look like products without a visible "sponsored/Kemitraan" label.
- Targeting ads with personal data.
- Infrastructure sized for scale that does not exist (microservices, queues, search cluster, Kubernetes).
- Money bugs: wrong totals, double-created orders, rounding errors.
- Losing the database: a homelab with no tested off-site backup is a failure.
- A buyer travelling to a store for an order the seller never prepared or cannot fulfil.

## 5. Decisions

| Area | Decision | Source |
|---|---|---|
| Client | Android, Kotlin, Jetpack Compose | [Decided] |
| Backend language | Java (current LTS), Spring Boot | [Decided] (owner was ~50% on Java; accepted after discussion) |
| Revenue | In-app ads + features in development; listing is free | [Decided] |
| Fulfilment | Customer picks up at the seller's store | [Decided] |
| Payment | Cash at the store at pickup | [Decided] |
| Ads in MVP | Placeholder content in real slots | [Decided] |
| Seller onboarding/portal | Skipped in MVP; portal is separate work, same backend later | [Decided] |
| Hosting | Small homelab server | [Decided] |
| Scale target | Hundreds of orders/month | [Decided] |
| Backend shape | Modular monolith, one deployable | [Call] |
| Database | PostgreSQL only (search via `pg_trgm`) | [Call] |
| Schema migrations | Flyway | [Call] |
| API | REST + OpenAPI; Android client generated from the spec | [Call] |
| Auth | Phone number + OTP | [Call] |
| Admin | Operator-only web admin in the same backend | [Call] |
| Public access to homelab | Cloudflare Tunnel (no open inbound ports, survives changing home IP) | [Call] |
| Images | S3-compatible object storage, Cloudflare R2 by default, resized variants | [Call] |

Later phases (not MVP): QRIS via Midtrans/Xendit, AdMob fallback, seller portal, delivery with courier aggregator (Biteship / RajaOngkir), hub.

## 6. Repository layout

```
android/   Kotlin / Compose app
core/      Java / Spring Boot backend (incl. admin web UI)
docs/      mockups, specs
```

Both `android/` and `core/` are currently empty.

## 7. Architecture

```
Android app ──HTTPS/JSON──▶ Cloudflare ──Tunnel──▶ homelab: core (Spring Boot) ──▶ PostgreSQL
                                                          │
                                                          ├──▶ R2 (images, nightly DB dump)
                                                          ├──▶ OTP provider (WhatsApp/SMS)
                                                          └──▶ FCM (push)
Operator admin web ─────────────────────────────────────▶ core
Seller portal (future, separate work) ──────────────────▶ core
```

### 7.1 Backend modules (packages inside `core`)

Each module owns its tables and exposes a Java interface to the others; no module reads another's tables directly.

| Module | Owns |
|---|---|
| `identity` | users, OTP, sessions/tokens |
| `catalog` | sellers, storefronts, categories, products, price tiers, add-ons, images, search |
| `ordering` | orders, seller-orders, items, order state machine, reviews |
| `ads` | placements, campaigns, creatives |
| `admin` | operator web UI and endpoints |

No `fulfilment` module and no address table in the MVP. Add them when delivery arrives.

### 7.2 Android app

- Compose UI, MVVM, Hilt, Retrofit/OkHttp, Coil, Room, Paging 3, FCM.
- Browsing is allowed without login; OTP login is required at checkout, follow, favorite, and review [Call].
- Cart is stored locally (Room) and reconciled with the server at checkout, so it works offline [Call].
- Images: request resized variants only; Coil disk cache on.
- UI language: Indonesian only.

### 7.3 API conventions

- REST, JSON, `/v1` prefix, OpenAPI spec is the contract.
- Money is **integer rupiah** (`long`) everywhere: DB, API, client. No floating point.
- Cursor pagination for lists.
- `POST /orders` requires an `Idempotency-Key` header; the same key returns the same order.
- Server recomputes all prices at order creation. Client totals are display-only.
- Access token (short-lived) + refresh token. Every admin endpoint requires the operator role.

## 8. Domain model

Entities and the invariants the builder must preserve. Column-level design is left to the builder.

**User**: phone (unique, verified), name, role (buyer, operator). A `seller` role and the link from a user to a seller are added with the seller portal, not before.

**Seller**: UMKM profile created by the operator; name, description, **pickup address (text), opening hours (text)**, WhatsApp number, status (active/suspended). Buyers use the address and hours to collect their order; the WhatsApp number lets them reach the seller (deep link; no in-app chat). A seller is not a login user in the MVP [Decided].

**Category**: the nine in the mockup (Makanan & Minuman, Fashion & Aksesoris, Kerajinan & Dekorasi, Pertanian & Hasil Bumi, Kecantikan & Perawatan, Elektronik & Aksesoris, Rumah & Dapur, Otomotif, Lainnya). Seeded, editable by operator.

**Product**: seller, category, name, description, retail price, stock, images, active flag. No weight in the MVP (only needed for shipping).
- **Price tiers** (Pillar 2): zero or more `(min_qty, unit_price)` rows. Quantity ≥ `min_qty` uses that tier price. Retail is the default. Table exists in the MVP; the "Grosir" UI is phase 2 [Call].
- **Add-ons** (Pillar 2): optional paid extras like "Custom Label & Logo +Rp 1.500", per-unit price. Same phasing.
- **Halal badge**: optional certificate number plus an operator-verified flag. The "Halal ID" badge shows **only when verified**; showing it on seller self-declared data is a trust and legal risk [Call].

**Cart** (local on device, server-validated at checkout): lines of `(product, qty, selected add-ons)`, possibly from several sellers (the mockup cart mixes Bu Siti and Bu Rini).

**Order** (buyer-facing, e.g. `#NM-20261015-001`):
- Buyer, payment method (always cash at pickup), total = sum of line totals.
- Price snapshot per line at order time; later price changes never alter an existing order. The seller's pickup address and hours are also snapshotted on the seller-order so the confirmation stays correct if the seller changes them.
- Display number `NM-YYYYMMDD-NNN`; unique and generated safely under concurrency (do not derive it from `count(*)`). The order number is what the buyer shows at the store.
- **SellerOrder**: one per seller in the order; the unit that is fulfilled. Carries that seller's items, its own status, the pickup snapshot, and an optional free-text note from the operator (e.g. "siap diambil mulai jam 14.00"). A cart with N sellers produces N seller-orders.

**SellerOrder state machine** (matches the tracking screen):

```
CREATED → SELLER_PROCESSING → READY_FOR_PICKUP → COMPLETED
   └────────────┴──────────────────┘→ CANCELLED
```

- Transitions are enforced in one place (the `ordering` module); invalid transitions are rejected. The transition operation takes the acting role as input and is role-agnostic otherwise, so the seller portal can call it later without moving logic [Call].
- Stock is decremented when the order is created and restored on cancel.
- `COMPLETED` means the buyer collected and paid.
- The buyer-facing Order status is derived: the least-advanced non-cancelled SellerOrder, or CANCELLED if all are. The tracking screen shows the Order timeline and lists each seller's status. The rule lives in one function and is tested.
- Order-history tabs map to: Diproses (CREATED..SELLER_PROCESSING), Siap Diambil (READY_FOR_PICKUP), Selesai (COMPLETED/CANCELLED).
- State changes are made by the operator in the admin in the MVP; the seller portal takes this over later. Each state change triggers a push notification to the buyer, especially "Siap diambil".
- **No-shows** [Call]: the MVP does not auto-expire uncollected orders. The operator cancels with a reason (e.g. "tidak diambil") and stock is restored. At hundreds of orders per month this is manageable by hand; revisit if it is not.

**Payment**: cash at the store. The platform holds no money, so there is no reconciliation or payout feature.

**Review**: rating 1–5 plus optional text; only the buyer of a COMPLETED seller-order, one per order item. Product and seller ratings shown in the app are aggregates updated on write [Call].

**Follow / Favorite**: simple join tables (user–seller, user–product).

## 9. Ads and partner placements (Pillar 3)

Placements seen in the mockups:

| Placement key | Screen | Format |
|---|---|---|
| `home_partner_carousel` | Home, "Mitra Unggulan" | Banner carousel |
| `home_sponsored_card` | Home, "Produk Pilihan" | Card labeled "Kemitraan" |
| `post_purchase` | Order confirmation | Banner |
| `wait_state` | Order tracking | Card |
| *(none)* | Cart, Checkout | **No ads, ever** |

MVP model, intentionally minimal [Call]:
- **Placement** (fixed set above) → **Campaigns** (advertiser name, dates, priority, active) → **Creatives** (image, title, deep link/URL, label text).
- Seeded with **placeholder campaigns** so the slots look and behave like the mockup. Replacing a placeholder with a real deal is a data change in the admin, not a release.
- Selection: highest-priority active campaign for the slot. A plain query; no ad server.
- Every sponsored item carries a visible label ("Kemitraan" / "Iklan").
- **Not in the MVP**: impression/click tracking, advertiser reporting, AdMob. Add them when the first real advertiser or AdMob arrives.
- **Consequence to be aware of**: with placeholders only, the MVP earns nothing. Revenue starts when real partners or AdMob (phase 2) are switched on.
- **Privacy**: contextual only (by slot and screen), no personal-data targeting. Before AdMob is enabled, consent text compliant with Indonesia's personal data protection law (UU PDP) is needed.

## 10. Operator admin

The mockups show no operator screens, but the catalogue has to be populated and orders have to move. Operator-only web admin in the same backend (server-rendered or small SPA; builder's choice) [Call]:
- Create and edit sellers (including pickup address and hours), products, images, stock, categories.
- Verify halal badges.
- See orders; advance a SellerOrder's status; cancel with reason; edit the pickup note.
- Manage ad campaigns and creatives.

The seller portal is separate work in progress and will be a client of this same backend [Decided]. The MVP builds no seller login, registration, role, or seller-scoped endpoints. Order and catalog changes go through the `ordering` and `catalog` service methods, not admin-only controller code, so seller endpoints can be added later without moving logic.

## 11. Fulfilment: pickup

- The buyer collects from each seller's store; the platform does no delivery, takes no shipping fee, and integrates with no courier [Decided].
- Checkout and confirmation show each seller's pickup address and opening hours. The product page shows the pickup location.
- "Siap diambil" is the key buyer notification: the buyer should not travel until the seller has prepared the order.
- When delivery is added later, it brings back addresses, shipping fees, and a Shipment entity (also the basis for a hub). That is additive.

## 12. Non-functional requirements

- **Low-end devices and weak networks**: small payloads, resized images, local cart cache, retry with backoff, skeleton loading. Usable on 2 GB RAM phones.
- **Homelab operations** [Call]:
  - Run via Docker Compose (core + PostgreSQL), JVM heap capped, restart-on-boot.
  - Public access only through Cloudflare Tunnel; the home IP and ports are not exposed.
  - Images and image traffic go through R2, so the home upload link only carries API traffic.
  - **Backups**: nightly `pg_dump` to off-site storage (R2 or another cloud bucket), retention of at least 14 days, and a **restore tested before launch**. A single home disk is not a backup.
  - Power/network outage is expected sometimes; use a UPS if possible, an external uptime monitor that alerts the owner, and let the app show a friendly offline/maintenance state.
  - Server OS and Docker image updates are the owner's routine; schedule them.
- **Security**: OTP rate limiting and attempt caps; operator-role check on all admin endpoints; bean validation on all inputs; no PII in logs; HTTPS only; secrets from environment, not repo; admin additionally protected behind Cloudflare Access or a strong password with lockout.
- **Data safety**: Flyway migrations are forward-only and reviewed. The only buyer personal data is phone number and name.
- **Accessibility**: Compose semantics/content descriptions, minimum touch target 48dp, text scalable.
- **Observability**: structured logs, health endpoint, uptime check. Nothing heavier.

## 13. Testing (required, not optional)

- Unit tests: price calculation (retail, tiers, add-ons), SellerOrder state transitions, derived Order status, review eligibility, ad slot selection.
- Integration tests with a real PostgreSQL (Testcontainers): order creation idempotency, stock decrement under concurrent orders, order number uniqueness.
- Android: ViewModel tests for cart and checkout; one end-to-end smoke path (browse → cart → checkout → track) against a local backend.
- Contract: CI fails if the generated OpenAPI client and the server spec diverge.
- Restore test: restoring the latest backup into a fresh PostgreSQL succeeds (run once before launch, then periodically).

## 14. Phasing and open questions

| Phase | Contents |
|---|---|
| 1 (MVP) | OTP login, browse, search, category, product detail, cart, pickup checkout (pay at store), confirmation, tracking, order history, seller storefront, follow/favorite, reviews, profile, push on status change; operator admin; four ad slots with placeholder content |
| 2 | Seller portal on this backend (seller role, seller-scoped endpoints), real advertisers with impression/click tracking and reporting, AdMob fallback, QRIS, wholesale tier UI and custom-label add-on, B2B hampers |
| 3 | Delivery (addresses, shipping fees, courier aggregator), hub (Pillar 1) with consolidated shipping, payouts as needed, in-app chat, additional regions |

Resolved by the owner on 2026-10-20: no hub in the MVP; seller onboarding skipped; ads are placeholders; pickup at the seller's store with cash payment; seller portal is separate work on the same backend; hundreds of orders/month on a small homelab server.

**Open questions**: none that block the plan. The no-show rule and pickup-note handling in section 8 are my calls; change them if they do not match how the sellers actually operate.

## 15. Left to the builder

Column-level schema, exact REST resource shapes, admin UI technology, Compose navigation and theming (use the green palette from the mockups), error and empty states, notification copy, CI setup, OTP provider choice after comparing cost, and backup tooling details.
