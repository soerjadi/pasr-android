# Nusantara API — handoff for the backend

`openapi.yaml` is the contract the Android app is built against (the Retrofit client is generated from it).
`fixtures/` holds one example response per operation that returns a body; the app's `demo` flavor serves
them as a fake server and the Android tests decode every one of them against the generated models, so they
are valid examples of what the backend must return. Backend contract tests can reuse them.

Design: `docs/superpowers/specs/2026-10-20-nusantara-marketplace-design.md`.

## Conventions the app relies on

- Base path `/v1`. Money is integer rupiah (`int64`). Timestamps are ISO-8601 UTC strings (`2026-10-15T03:02:00Z`).
- Auth: `Authorization: Bearer <accessToken>`. The app refreshes once on a 401 via `POST /v1/auth/refresh`; if that is
  refused (401/4xx) the user is logged out. Auth endpoints are never sent a bearer token.
- Lists: `cursor` + `limit` (the app sends `limit=20`, max 50), response `{items, nextCursor}`; `nextCursor` null on the last page.
- Errors: `{code, message}`. The app reacts to these codes:

| code | status | operation | app behaviour |
|---|---|---|---|
| `OTP_INVALID` | 400 | `verifyOtp` | "Kode salah. Coba lagi." |
| `OTP_RATE_LIMITED` | 429 | `requestOtp`, `verifyOtp` | "Terlalu banyak percobaan. Coba lagi nanti." (60 s resend cooldown restarts) |
| `INSUFFICIENT_STOCK` | 409 | `createOrder` | back to the cart, which re-validates |
| `PRODUCT_UNAVAILABLE` | 409 | `createOrder` | same |
| any 409 | 409 | `submitReview` | "Ulasan sudah dikirim." and closes (already reviewed / not completed) |

- `createOrder` must honour `Idempotency-Key`: the same key returns the same order (the app reuses a key per distinct cart,
  persisted on the device until the order succeeds, so retries after timeouts or app kills never double-order).
  Prices are recomputed on the server; the client's totals are display only.
- `validateCart` is public (cart is shown before login) and must return every requested line, with `issue`
  (`OUT_OF_STOCK`, `INSUFFICIENT_STOCK`, `PRODUCT_UNAVAILABLE`) and `availableStock` set per line, grouped by seller with
  each seller's pickup address and opening hours.
- `Order` carries a per-seller `SellerOrder` with a **snapshot** of pickup address/hours/WhatsApp from order time.
  The buyer-facing `Order.status` is the least-advanced non-cancelled seller-order status (or CANCELLED if all are).
  `statusHistory` should contain one entry per status reached (the app shows their dates).
- `listOrders?tab=`: `ALL`; `PROCESSING` = CREATED..SELLER_PROCESSING; `READY` = READY_FOR_PICKUP; `DONE` = COMPLETED + CANCELLED. Newest first.
- `halalVerified` on products is true only when an operator verified the certificate; the app shows the "Halal ID" badge solely on this flag.
- Phone numbers are E.164 (`+62812…`). The app normalises `08…`, `62…`, `+62…` and only accepts Indonesian mobile numbers
  (national number starts with 8, 9–12 digits).

## Operation → screen

| operationId | Screen / use |
|---|---|
| `requestOtp`, `verifyOtp` | Login (phone → code) |
| `refreshSession`, `logout` | Silent token refresh; Profile → Keluar |
| `getMe`, `updateMe` | Profile (name 1–50 chars) |
| `registerDevice` | Push token upsert after login / token refresh |
| `listCategories` | Home shortcuts, Kategori tab |
| `listProducts` | Home "Produk Pilihan" (`featured=true&limit=8`), category list, search (`q`), seller storefront (`sellerId`) |
| `getProduct` | Product detail (`isFavorite` null when anonymous) |
| `getSeller`, `listSellerReviews` | Seller storefront (`isFollowing` null when anonymous) |
| `listFavorites`, `addFavorite`, `removeFavorite` | Favorit tab, heart on product detail |
| `followSeller`, `unfollowSeller` | Seller storefront follow button |
| `submitReview` | Beri ulasan on a COMPLETED order's items |
| `validateCart` | Cart screen (debounced), Checkout review |
| `createOrder` | Checkout → Buat Pesanan |
| `getOrder` | Order confirmation, Tracking |
| `listOrders` | Pesanan tab (4 tabs) |
| `listAds` | Home carousel + sponsored card, order confirmation, order tracking |

## Seed data the app expects

- The nine categories, with these `iconKey`s (the app maps them to icons; unknown keys get a generic one):
  Makanan & Minuman `food`, Fashion & Aksesoris `fashion`, Kerajinan & Dekorasi `craft`, Pertanian & Hasil Bumi `farm`,
  Kecantikan & Perawatan `beauty`, Elektronik & Aksesoris `electronics`, Rumah & Dapur `home`, Otomotif `auto`, Lainnya `other`.
- Ads: the four placements `home_partner_carousel`, `home_sponsored_card`, `post_purchase`, `wait_state`, each with at least one
  placeholder campaign (MVP). Every ad needs a visible `label` ("Kemitraan" or "Iklan"). The app opens only `http(s)` links.
  There is deliberately no placement for cart or checkout. `listAds` returns an empty list when a placement has nothing.
- Image fields are `ImageSet {small, medium, large}` (about 320 / 640 / 1080 px wide).

## Push notifications (FCM)

Data-only messages: `{ "orderId": "7", "status": "READY_FOR_PICKUP", "title"?: "...", "body"?: "..." }`.
Send on every status change, especially `READY_FOR_PICKUP` ("Siap diambil") — buyers should not travel before it.
Tapping opens `nusantara://order/<orderId>` (the order's tracking screen).

## Running the app

- **Demo flavor** (no backend): `cd android && ./gradlew :app:installDemoDebug`. Serves `fixtures/` as responses; any phone/code logs in.
- **Live flavor**: set Gradle properties (e.g. in `~/.gradle/gradle.properties`):
  `nusantara.apiBaseUrl` (must end with `/`), `nusantara.supportWhatsapp` (operator number for "Bantuan"),
  and, for push, `nusantara.firebase.appId`, `.apiKey`, `.projectId`, `.senderId` (push is simply off while any is blank).
- Tests: `cd android && ./gradlew :app:testDemoDebugUnitTest` (includes an end-to-end purchase smoke test over the fixtures).
- Changing the contract: edit `openapi.yaml`, then update the matching fixture; the build regenerates the client and
  `ContractTest` / `FixtureTest` fail if the two drift.
