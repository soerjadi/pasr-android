# Nusantara Android App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Android buyer app for the pickup-only MVP, runnable with no backend (demo flavor) and producing the OpenAPI contract the backend will implement.

**Architecture:** Single Gradle module `app`, package-by-feature, MVVM (stateless screen composable + `ViewModel`), repositories as the only users of the generated API client. Contract-first: `docs/api/openapi.yaml` is written in Task 4, the Retrofit client is generated from it, and shared JSON fixtures in `docs/api/fixtures/` feed a `demo` flavor, the unit tests, and later the backend's contract tests.

**Tech Stack:** Kotlin, Jetpack Compose, Navigation (type-safe routes), Hilt, Retrofit/OkHttp, kotlinx-serialization, openapi-generator (Gradle plugin), Coil, Room, Paging 3, DataStore, FCM; tests: JUnit4, Robolectric + Compose UI test, MockK, Turbine, coroutines-test, MockWebServer. Pin the latest stable versions at implementation time in `libs.versions.toml`.

**Spec:** `docs/superpowers/specs/2026-10-20-nusantara-marketplace-design.md`

**Path shorthand:** `MAIN` = `android/app/src/main/kotlin/id/oemah/nusantara`, `TEST` = `android/app/src/test/kotlin/id/oemah/nusantara`, `DEMO` = `android/app/src/demo/kotlin/id/oemah/nusantara`. Test command: `cd android && ./gradlew :app:testDemoDebugUnitTest --tests "<FQCN>"`.

## Global Constraints

From the spec, verbatim where quoted:
- Android, Kotlin, Jetpack Compose; "Compose UI, MVVM, Hilt, Retrofit/OkHttp, Coil, Room, Paging 3, FCM". No AdMob in the MVP.
- "Money is **integer rupiah** (`long`) everywhere: DB, API, client. No floating point."
- "REST, JSON, `/v1` prefix, OpenAPI spec is the contract"; Android client is generated from the spec.
- "Server recomputes all prices at order creation. Client totals are display-only." `POST /orders` sends an `Idempotency-Key` header.
- "UI language: Indonesian only."
- "Browsing is allowed without login; OTP login is required at checkout, follow, favorite, and review."
- "Cart is stored locally (Room) and reconciled with the server at checkout, so it works offline."
- "Images: request resized variants only; Coil disk cache on."
- Ad placement keys are exactly `home_partner_carousel`, `home_sponsored_card`, `post_purchase`, `wait_state`. "**No ads, ever**" in cart and checkout (their ViewModels and screens must not depend on `AdsRepository`). Every sponsored item shows a visible label ("Kemitraan" / "Iklan").
- Order statuses: `CREATED, SELLER_PROCESSING, READY_FOR_PICKUP, COMPLETED, CANCELLED`. Tracking steps: Pesanan dibuat → Diproses penjual → Siap diambil → Selesai. History tabs: Semua, Diproses, Siap Diambil, Selesai. Payment is cash at the store ("Bayar di toko saat mengambil"); QRIS shown disabled ("Segera hadir"). No delivery, addresses, shipping fees, hub, or courier anywhere in the UI. The "Halal ID" badge shows only when the server says verified.
- "Usable on 2 GB RAM phones": small payloads, "retry with backoff, skeleton loading"; minimum touch target 48dp, content descriptions, scalable text.
- Calls made in this plan (flag in review): `minSdk 24`; package `id.oemah.nusantara`; flavors `demo` (fixtures, no backend) and `live`; fixed location label "Simo, Boyolali"; after login the user returns to the previous screen and repeats the action (no pending-action replay); search covers products only.

## Review Focus

Inputs and conditions the spec implies but does not spell out, most likely to bite first. Each has a pinning test in the owning task.
1. Phone numbers typed as `0812-3456-7890`, `+62 812…`, `62812…`, landlines, or garbage (Tasks 2, 7).
2. Stock or price changing between add-to-cart and checkout: sold out, fewer left, product withdrawn, price drift (Tasks 15, 16).
3. Double tap, timeout, or app kill during "Buat Pesanan" must never create two orders (Task 16).
4. Ad endpoint failing, empty, blank label, or returning an unsafe URL (`javascript:`, `intent:`) must never break or hijack a screen (Task 8).
5. Access token expiring mid-session, refresh failing, and launching fully offline (Tasks 6, 11).

---

### Task 1: Project scaffold, theme, navigation shell

**Files:**
- Create: `.gitignore` (repo root), `android/settings.gradle.kts`, `android/build.gradle.kts`, `android/gradle/libs.versions.toml`, `android/app/build.gradle.kts`, `android/app/src/main/AndroidManifest.xml`, `android/app/src/main/res/values/strings.xml`, `MAIN/NusantaraApp.kt`, `MAIN/MainActivity.kt`, `MAIN/ui/theme/Theme.kt`, `MAIN/ui/nav/Routes.kt`, `MAIN/ui/nav/AppNavHost.kt`
- Test: `TEST/ui/nav/AppNavHostTest.kt`

**Interfaces:**
- Produces: `NusantaraTheme(content)`; `@Serializable sealed interface Route` with `data object Home, Categories, Orders, Favorites, Account`; `enum class BottomTab(val route: Route, @StringRes val label: Int)` in that order; `@Composable fun AppNavHost(navController: NavHostController, modifier: Modifier)`. Later tasks add their own routes to `Routes.kt`: `Login`, `Search`, `ProductList(categoryId: Long?, title: String, featured: Boolean)`, `ProductDetail(productId: Long)`, `Seller(sellerId: Long)`, `Cart`, `Checkout`, `OrderConfirmation(orderId: Long)`, `OrderTracking(orderId: Long)`, `WriteReview(orderItemId: Long, productName: String)`, `Onboarding`.
- Build: flavors `demo`/`live` (dimension `env`); `BuildConfig.API_BASE_URL` from Gradle property `nusantara.apiBaseUrl` (live) / `http://localhost/` (demo); release builds use R8 minify + resource shrinking.

- [ ] **Step 1: Initialise the repo.** Run `git init` at the repo root (project is not yet a git repository) and add `.gitignore` (Gradle, `.idea`, `local.properties`, `build/`, `google-services.json`).
- [ ] **Step 2: Write failing test** `AppNavHostTest` (Robolectric + `createComposeRule`): `bottom_bar_shows_five_tabs_in_order` asserts nodes "Beranda", "Kategori", "Pesanan", "Favorit", "Akun" exist; `tapping_kategori_selects_it` clicks "Kategori" and asserts it `isSelected`.
- [ ] **Step 3: Run it.** Command above with `id.oemah.nusantara.ui.nav.AppNavHostTest`. Expected: FAIL (project does not compile / classes missing).
- [ ] **Step 4: Implement** the Gradle setup, theme (green primary taken from the mockups), `BottomTab`, `AppNavHost` with a placeholder destination per tab, Hilt application and single-activity host.
- [ ] **Step 5: Run it.** Expected: PASS. Also run `./gradlew :app:assembleDemoDebug` (Expected: BUILD SUCCESSFUL).
- [ ] **Step 6: Commit** `git add -A && git commit -m "chore: scaffold android app with nav shell"`

### Task 2: Core primitives (money, phone, errors, UI state)

**Files:**
- Create: `MAIN/core/Rupiah.kt`, `MAIN/core/IndonesianPhone.kt`, `MAIN/core/AppError.kt`, `MAIN/core/UiState.kt`
- Test: `TEST/core/RupiahTest.kt`, `TEST/core/IndonesianPhoneTest.kt`

**Interfaces:**
- Produces:
  - `@JvmInline value class Rupiah(val amount: Long)` with `init { require(amount >= 0) }`, `operator fun plus(o: Rupiah): Rupiah`, `operator fun times(qty: Int): Rupiah` (`Math.multiplyExact`), `fun format(): String`.
  - `object IndonesianPhone { fun normalize(input: String): String?; fun toWhatsAppUrl(e164: String): String? }`.
  - `sealed class AppError(message: String? = null) : Exception(message)` with `Offline`, `Unauthorized`, `Conflict(code: String, message: String?)`, `Rejected(code: String, message: String?)`, `Server`, `Unknown(cause: Throwable?)`.
  - `sealed interface UiState<out T> { Loading; Content(T); Error(AppError) }`; `sealed interface UiEvent { data object LoginRequired; data class Message(val text: String) }`.

- [ ] **Step 1: Write failing tests.**
  - `RupiahTest`: `Rupiah(15_000).format() == "Rp 15.000"`; `Rupiah(1_500).format() == "Rp 1.500"`; `Rupiah(0).format() == "Rp 0"`; `Rupiah(1_234_567).format() == "Rp 1.234.567"`; `Rupiah(15_000) * 3 == Rupiah(45_000)`; `Rupiah(1) + Rupiah(2) == Rupiah(3)`; `Rupiah(-1)` throws `IllegalArgumentException`.
  - `IndonesianPhoneTest`: `normalize` returns `"+6281234567890"` for `"0812-3456-7890"`, `"+62 812 3456 7890"`, `"6281234567890"`, `" (0812) 3456 7890 "`; returns `null` for `""`, `"081"`, `"0212345678"` (landline), `"abc"`, `"0812345678901234"` (too long). `toWhatsAppUrl("+6281234567890") == "https://wa.me/6281234567890"`; `toWhatsAppUrl("0812")` is `null`.
- [ ] **Step 2: Run both test classes.** Expected: FAIL (symbols missing).
- [ ] **Step 3: Implement.** Mobile rule: after normalising to `+62`, the national number starts with `8` and has 9–12 digits. Grouping uses `.` regardless of device locale.
- [ ] **Step 4: Run both.** Expected: PASS.
- [ ] **Step 5: Commit** `feat: add money, phone, error and ui-state primitives`

### Task 3: Shared UI components

**Files:**
- Create: `MAIN/ui/components/PrimaryButton.kt`, `MAIN/ui/components/QuantityStepper.kt`, `MAIN/ui/components/StateHost.kt`, `MAIN/ui/components/ErrorText.kt`
- Test: `TEST/ui/components/QuantityStepperTest.kt`, `TEST/ui/components/StateHostTest.kt`

**Interfaces:**
- Consumes: `UiState`, `AppError` (Task 2).
- Produces: `@Composable fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true)`; `@Composable fun QuantityStepper(value: Int, min: Int, max: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier)`; `@Composable fun <T> StateHost(state: UiState<T>, onRetry: () -> Unit, content: @Composable (T) -> Unit)` (skeleton on `Loading`); `fun AppError.userMessage(): String`.

- [ ] **Step 1: Write failing tests.**
  - `QuantityStepperTest`: at `value == min` the "Kurangi jumlah" button is disabled; at `value == max` "Tambah jumlah" is disabled; both buttons satisfy `assertHeightIsAtLeast(48.dp)` and `assertWidthIsAtLeast(48.dp)`; clicking "Tambah jumlah" at `value = 2` calls `onChange(3)`.
  - `StateHostTest`: `Error(AppError.Offline)` shows `"Tidak ada koneksi internet. Periksa sambungan Anda lalu coba lagi."` and a "Coba lagi" button that calls `onRetry`; `Content("x")` renders content; `userMessage()` for `Server` is `"Terjadi gangguan pada server. Coba lagi nanti."`, `Unauthorized` is `"Sesi Anda berakhir. Silakan masuk lagi."`, `Unknown` is `"Terjadi kesalahan. Coba lagi."`.
- [ ] **Step 2: Run them.** Expected: FAIL.
- [ ] **Step 3: Implement** with string resources in `values/strings.xml` and content descriptions on stepper buttons.
- [ ] **Step 4: Run them.** Expected: PASS.
- [ ] **Step 5: Commit** `feat: add shared ui components`

### Task 4: API contract and generated client

**Files:**
- Create: `docs/api/openapi.yaml`, `MAIN/core/Images.kt`
- Modify: `android/app/build.gradle.kts` (openapi-generator-gradle-plugin: generator `kotlin`, library `jvm-retrofit2`, `useCoroutines=true`, serialization `kotlinx_serialization`, `apiPackage=id.oemah.nusantara.api`, `modelPackage=id.oemah.nusantara.api.model`, input `../docs/api/openapi.yaml`, output `build/generated/openapi` added to the main source set and wired before `compileKotlin`), `android/gradle/libs.versions.toml`
- Test: `TEST/api/ContractTest.kt`, `TEST/core/ImagesTest.kt`

**Interfaces:**
- Produces generated interfaces by tag: `AuthApi`, `MeApi`, `CatalogApi`, `SocialApi`, `OrdersApi`, `AdsApi`, and `fun ImageSet.pick(targetPx: Int): String` (≤320 → `small`, ≤640 → `medium`, else `large`).
- The contract (this table *is* the list of what the backend must provide). All money fields are `int64` rupiah. Lists use `cursor` (opaque) + `limit` (default 20, max 50) and return `{items, nextCursor?}`. Errors are `Error{code, message}`. Auth is Bearer unless marked public.

| tag | operationId | request | response | notes |
|---|---|---|---|---|
| auth | `requestOtp` | `POST /v1/auth/otp/request {phone}` | 204 | public; 429 `OTP_RATE_LIMITED` |
| auth | `verifyOtp` | `POST /v1/auth/otp/verify {phone, code}` | `Session` | public; 400 `OTP_INVALID` |
| auth | `refreshSession` | `POST /v1/auth/refresh {refreshToken}` | `Session` | public; 401 on invalid |
| auth | `logout` | `POST /v1/auth/logout {refreshToken}` | 204 | revokes refresh token |
| me | `getMe` / `updateMe` | `GET` / `PATCH /v1/me {name}` | `User` | |
| me | `registerDevice` | `PUT /v1/me/devices {fcmToken}` | 204 | idempotent upsert |
| catalog | `listCategories` | `GET /v1/categories` | `CategoryList` | public |
| catalog | `listProducts` | `GET /v1/products?categoryId&q&featured&sellerId&cursor&limit` | `ProductPage` | public |
| catalog | `getProduct` | `GET /v1/products/{productId}` | `ProductDetail` | public; `isFavorite` null when anonymous |
| catalog | `getSeller` | `GET /v1/sellers/{sellerId}` | `SellerDetail` | public; `isFollowing` null when anonymous |
| catalog | `listSellerReviews` | `GET /v1/sellers/{sellerId}/reviews` | `ReviewPage` | public |
| social | `listFavorites` | `GET /v1/me/favorites` | `ProductPage` | |
| social | `addFavorite` / `removeFavorite` | `PUT` / `DELETE /v1/me/favorites/{productId}` | 204 | |
| social | `followSeller` / `unfollowSeller` | `PUT` / `DELETE /v1/me/following/{sellerId}` | 204 | |
| social | `submitReview` | `POST /v1/order-items/{orderItemId}/reviews {rating 1..5, text?}` | `Review` 201 | 409 if not COMPLETED or already reviewed |
| orders | `validateCart` | `POST /v1/cart/validate {lines:[{productId, quantity}]}` | `CartValidation` | public |
| orders | `createOrder` | `POST /v1/orders` + header `Idempotency-Key` `{lines}` | `Order` 201 | same key returns same order; 409 `INSUFFICIENT_STOCK` / `PRODUCT_UNAVAILABLE` |
| orders | `listOrders` | `GET /v1/orders?tab=ALL\|PROCESSING\|READY\|DONE&cursor&limit` | `OrderPage` | PROCESSING = CREATED..SELLER_PROCESSING, READY = READY_FOR_PICKUP, DONE = COMPLETED+CANCELLED |
| orders | `getOrder` | `GET /v1/orders/{orderId}` | `Order` | owner only |
| ads | `listAds` | `GET /v1/ads?placement=<key>` | `AdList` | public; ordered by priority |

Schemas (field → type):
- `ImageSet{small, medium, large}` URLs. `Category{id, name, iconKey}`. `User{id, phone, name?}`. `Session{accessToken, refreshToken, user}`.
- `SellerSummary{id, name, avatar: ImageSet?, location, rating: double, ratingCount: int32, pickupAddress, openingHours, whatsapp}`. `SellerDetail{seller, description, followerCount: int32, isFollowing: bool?}`.
- `ProductSummary{id, name, sellerId, sellerName, price, image: ImageSet, rating, ratingCount, stock: int32, halalVerified: bool}`. `ProductDetail{product: ProductSummary, description, images: ImageSet[], seller: SellerSummary, isFavorite: bool?}`.
- `CartLineInput{productId, quantity}`. `CartValidation{groups:[{seller: SellerSummary, lines: ValidatedLine[]}], total}`. `ValidatedLine{productId, name, image, quantity, unitPrice, lineTotal, issue: LineIssue?, availableStock: int32}`; `LineIssue = OUT_OF_STOCK | INSUFFICIENT_STOCK | PRODUCT_UNAVAILABLE`.
- `Order{id, number, status, createdAt, total, paymentMethod: CASH_AT_PICKUP, sellerOrders: SellerOrder[], statusHistory: [{status, at}]}`. `SellerOrder{id, sellerId, sellerName, whatsapp, pickupAddress, openingHours, status, note?, subtotal, items: OrderItem[]}` (pickup fields are the order-time snapshot). `OrderItem{id, productId, name, image, quantity, unitPrice, lineTotal, reviewed: bool}`.
- `Review{id, rating, text?, authorName, createdAt}`. `Ad{id, placement, title, subtitle?, image, targetUrl, label}`.
- Product tiers and add-ons are **not** in this v1 contract; they arrive with the phase-2 UI.

- [ ] **Step 1: Write failing tests.** `ContractTest.generated_client_exposes_every_expected_operation`: reflect over the six generated `*Api` interfaces and assert the set of Kotlin method names equals the 23 operationIds above. `ImagesTest`: `pick(300)` → `small`, `pick(320)` → `small`, `pick(321)` → `medium`, `pick(700)` → `large`.
- [ ] **Step 2: Run them.** Expected: FAIL (no generated client).
- [ ] **Step 3: Write `docs/api/openapi.yaml`** exactly per the tables, then wire the generator. Operation ids and schema names are what later tasks reference; do not rename.
- [ ] **Step 4: Implement `ImageSet.pick`** in `Images.kt`.
- [ ] **Step 5: Run** `./gradlew :app:openApiValidate` (Expected: validation succeeds) and both test classes (Expected: PASS).
- [ ] **Step 6: Commit** `feat: add v1 api contract and generated client`

### Task 5: Fixtures and demo flavor

**Files:**
- Create: `docs/api/fixtures/<operationId>.json` (one per operation that returns a body), `DEMO/network/FixtureInterceptor.kt`, `DEMO/network/DemoNetworkModule.kt`, `MAIN/core/network/InterceptorsModule.kt`
- Modify: `android/app/build.gradle.kts` (demo flavor adds `../docs/api` `fixtures/**` as Java resources)
- Test: `android/app/src/test/kotlin/id/oemah/nusantara/network/FixtureTest.kt`

**Interfaces:**
- Produces: `class FixtureInterceptor(openResource: (String) -> InputStream?) : Interceptor` serving `fixtures/<operationId>.json`; an empty `@Multibinds Set<Interceptor>` in `InterceptorsModule` (main) filled by the demo module with `FixtureInterceptor`. Fixture world: product 1 Keripik Tempe (Rp 15.000, seller 1 Bu Siti), product 2 Madu Hutan (Rp 45.000, seller 2 Pak Slamet), product 3 Dodol Simo (Rp 20.000, seller 3 Bu Rini); `validateCart` and `createOrder` return those three with total `80000`; order number `NM-20261015-001`; `listAds` returns one ad with label "Kemitraan".

- [ ] **Step 1: Write failing tests** in `FixtureTest`: `every_generated_operation_has_a_fixture_or_is_bodyless` (scan Retrofit annotations on generated APIs; operations returning `Unit` are exempt); `fixtures_decode_into_generated_models` (decode each fixture into the generated method's return type with the app's `Json`); `interceptor_resolves_templated_path` (`GET /v1/products/42` serves `getProduct`, status 200); `unknown_route_returns_404`; `bodyless_operation_returns_204`.
- [ ] **Step 2: Run it.** Expected: FAIL.
- [ ] **Step 3: Write the fixtures** in Indonesian, consistent with the mockups, then implement the interceptor: map request → operation by matching Retrofit method + path template regex.
- [ ] **Step 4: Run it.** Expected: PASS.
- [ ] **Step 5: Commit** `feat: add demo fixtures and fixture interceptor`

### Task 6: Network layer and session

**Files:**
- Create: `MAIN/core/session/SessionStore.kt`, `MAIN/core/session/DataStoreSessionStore.kt`, `MAIN/core/session/TokenCipher.kt`, `MAIN/core/session/KeystoreTokenCipher.kt`, `MAIN/core/network/AuthInterceptor.kt`, `MAIN/core/network/TokenAuthenticator.kt`, `MAIN/core/network/RetryGetInterceptor.kt`, `MAIN/core/network/ApiCall.kt`, `MAIN/core/network/NetworkModule.kt`
- Test: `TEST/core/network/TokenAuthenticatorTest.kt`, `TEST/core/network/RetryGetInterceptorTest.kt`, `TEST/core/network/ApiCallTest.kt`, `TEST/core/session/DataStoreSessionStoreTest.kt`

**Interfaces:**
- Consumes: `AppError`, generated `AuthApi`, `Session`.
- Produces: `interface SessionStore { val session: StateFlow<Session?>; fun current(): Session?; suspend fun save(session: Session); suspend fun clear() }`; `interface TokenCipher { fun encrypt(plain: String): String; fun decrypt(cipher: String): String }` (Keystore AES-GCM impl; tests use an identity fake because the Keystore is unavailable on the JVM); `suspend fun <T> apiCall(block: suspend () -> retrofit2.Response<T>): Result<T>` (adapt to the generated return type if it differs); `fun interface Sleeper { fun sleep(ms: Long) }`.

- [ ] **Step 1: Write failing tests (MockWebServer).**
  - `TokenAuthenticatorTest`: a 401 triggers exactly one `POST /v1/auth/refresh` then retries with the new access token and succeeds; two concurrent 401s cause **one** refresh; refresh answering 401 calls `SessionStore.clear()` and returns the original 401; requests under `/v1/auth/` are never refreshed.
  - `RetryGetInterceptorTest`: a `GET` failing with `IOException` is retried at most 2 times with backoff 300 ms then 900 ms (recorded via a fake `Sleeper`); a `503` on `GET` is retried; a `POST` is never retried; a 4xx is never retried.
  - `ApiCallTest`: `IOException` → `AppError.Offline`; 401 → `Unauthorized`; 409 `{"code":"INSUFFICIENT_STOCK","message":"x"}` → `Conflict("INSUFFICIENT_STOCK","x")`; 400/422 → `Rejected(code, message)`; 500 → `Server`; malformed JSON → `Unknown`.
  - `DataStoreSessionStoreTest`: `save` then a new store instance reads the same session; `clear` empties it; the stored file does not contain the raw refresh token (fake cipher that prefixes `enc:`).
- [ ] **Step 2: Run them.** Expected: FAIL.
- [ ] **Step 3: Implement** with `Json { ignoreUnknownKeys = true }`, OkHttp client built from the `Set<Interceptor>` multibinding (Task 5) plus Auth/Retry interceptors and the authenticator; `Retrofit` created per generated API in `NetworkModule`; callers get `Result<T>` only.
- [ ] **Step 4: Run them.** Expected: PASS.
- [ ] **Step 5: Commit** `feat: add network layer, token refresh and session store`

### Task 7: Auth (OTP login)

**Files:**
- Create: `MAIN/feature/auth/AuthRepository.kt`, `MAIN/feature/auth/LoginViewModel.kt`, `MAIN/feature/auth/LoginScreen.kt`
- Modify: `MAIN/ui/nav/Routes.kt`, `MAIN/ui/nav/AppNavHost.kt`
- Test: `TEST/feature/auth/LoginViewModelTest.kt`

**Interfaces:**
- Consumes: `AuthApi`, `SessionStore`, `apiCall`, `IndonesianPhone`.
- Produces: `interface AuthRepository { suspend fun requestOtp(phoneE164: String): Result<Unit>; suspend fun verifyOtp(phoneE164: String, code: String): Result<Unit> /* saves session */; suspend fun logout() }`; `sealed interface LoginUiState { data class Phone(val error: String?); data class Code(val phoneE164: String, val resendInSeconds: Int, val error: String?) }`; `LoginViewModel` exposes `state: StateFlow<LoginUiState>`, `events: Flow<LoginEvent.LoggedIn>`, `submitPhone(raw: String)`, `submitCode(code: String)`, `resend()`. Route `Login`.

- [ ] **Step 1: Write failing tests** (fake repository, `TestScope`): invalid phone yields `Phone(error = "Nomor HP tidak valid")` and no request; `"0812-3456-7890"` calls `requestOtp("+6281234567890")` and moves to `Code`; `OTP_INVALID` shows `"Kode salah. Coba lagi."`; `OTP_RATE_LIMITED` shows `"Terlalu banyak percobaan. Coba lagi nanti."`; resend is disabled (`resendInSeconds` counts 60 → 0) and `resend()` before 0 sends nothing; successful verify emits `LoggedIn`; `Offline` shows `AppError.userMessage()`.
- [ ] **Step 2: Run it.** Expected: FAIL.
- [ ] **Step 3: Implement** repository (`verifyOtp` saves the session via `SessionStore`), view model and a two-step screen; navigation pops back after `LoggedIn`.
- [ ] **Step 4: Run it.** Expected: PASS.
- [ ] **Step 5: Commit** `feat: add otp login`

### Task 8: Ads

**Files:**
- Create: `MAIN/feature/ads/AdPlacement.kt`, `MAIN/feature/ads/AdsRepository.kt`, `MAIN/feature/ads/AdSlot.kt`, `MAIN/feature/ads/SafeLinks.kt`
- Test: `TEST/feature/ads/AdPlacementTest.kt`, `TEST/feature/ads/SafeLinksTest.kt`, `TEST/feature/ads/AdSlotTest.kt`, `TEST/feature/ads/AdsRepositoryTest.kt`

**Interfaces:**
- Consumes: `AdsApi`, `Ad`, `apiCall`.
- Produces: `enum class AdPlacement(val key: String) { HOME_PARTNER_CAROUSEL("home_partner_carousel"), HOME_SPONSORED_CARD("home_sponsored_card"), POST_PURCHASE("post_purchase"), WAIT_STATE("wait_state") }`; `interface AdsRepository { suspend fun ads(placement: AdPlacement): Result<List<Ad>> }` plus `suspend fun AdsRepository.adsOrEmpty(placement: AdPlacement): List<Ad>` (never throws); `@Composable fun AdSlot(ads: List<Ad>, modifier: Modifier = Modifier, onOpen: (String) -> Unit)` (stateless); `object SafeLinks { fun isOpenable(url: String): Boolean }`.

- [ ] **Step 1: Write failing tests.** `AdPlacementTest`: exactly 4 entries with exactly the four keys (a cart/checkout slot cannot exist). `SafeLinksTest`: `https://a.id/x` and `http://a.id` true; `javascript:alert(1)`, `intent://x`, `file:///etc/passwd`, `""`, `https` (no host) false. `AdSlotTest`: empty list renders nothing; a shown ad always displays its `label`, and a blank label falls back to "Iklan"; clicking an ad with an unsafe `targetUrl` does not call `onOpen`. `AdsRepositoryTest`: `adsOrEmpty` returns an empty list when the API fails.
- [ ] **Step 2: Run them.** Expected: FAIL.
- [ ] **Step 3: Implement** the slot as a banner/card layout using `ImageSet.pick`; carousel when more than one ad.
- [ ] **Step 4: Run them.** Expected: PASS.
- [ ] **Step 5: Commit** `feat: add ad placements and ad slot`

### Task 9: Local cart (Room)

**Files:**
- Create: `MAIN/feature/cart/data/CartDatabase.kt`, `MAIN/feature/cart/data/CartDao.kt`, `MAIN/feature/cart/CartRepository.kt`
- Test: `TEST/feature/cart/CartRepositoryTest.kt`

**Interfaces:**
- Consumes: `Rupiah`.
- Produces: `data class CartLine(val id: Long, val productId: Long, val sellerId: Long, val sellerName: String, val name: String, val imageUrl: String, val unitPrice: Rupiah, val quantity: Int)`; `data class CartProductSnapshot(productId, sellerId, sellerName, name, imageUrl, unitPrice: Rupiah)`; `interface CartRepository { val lines: Flow<List<CartLine>>; suspend fun add(product: CartProductSnapshot, quantity: Int = 1); suspend fun setQuantity(lineId: Long, quantity: Int); suspend fun remove(lineId: Long); suspend fun removeAll(lineIds: Collection<Long>); suspend fun updateSnapshot(productId: Long, name: String, unitPrice: Rupiah) }`. `imageUrl` stores the `small` variant.

- [ ] **Step 1: Write failing tests** (Robolectric, in-memory Room): adding the same product twice yields one line with quantity 2; `setQuantity(id, 0)` removes the line; `setQuantity(id, 500)` clamps to 99; `add` beyond 99 clamps; `removeAll` removes only the given lines; `updateSnapshot` changes name and price of existing lines; lines survive re-opening the database.
- [ ] **Step 2: Run it.** Expected: FAIL.
- [ ] **Step 3: Implement** Room entity, DAO and repository; unit price stored as `Long`.
- [ ] **Step 4: Run it.** Expected: PASS.
- [ ] **Step 5: Commit** `feat: add local cart`

### Task 10: Catalog and social repositories

**Files:**
- Create: `MAIN/feature/catalog/CatalogRepository.kt`, `MAIN/feature/catalog/ProductPagingSource.kt`, `MAIN/feature/social/SocialRepository.kt`
- Test: `TEST/feature/catalog/CatalogRepositoryTest.kt`

**Interfaces:**
- Consumes: `CatalogApi`, `SocialApi`, `apiCall`.
- Produces: `data class ProductQuery(val categoryId: Long? = null, val query: String? = null, val featured: Boolean = false, val sellerId: Long? = null)` (`query` is trimmed and blank becomes `null`); `interface CatalogRepository { suspend fun categories(): Result<List<Category>>; fun products(query: ProductQuery): Flow<PagingData<ProductSummary>>; suspend fun product(id: Long): Result<ProductDetail>; suspend fun seller(id: Long): Result<SellerDetail>; fun sellerReviews(sellerId: Long): Flow<PagingData<Review>> }`; `interface SocialRepository { fun favorites(): Flow<PagingData<ProductSummary>>; suspend fun setFavorite(productId: Long, favorite: Boolean): Result<Unit>; suspend fun setFollowing(sellerId: Long, following: Boolean): Result<Unit>; suspend fun submitReview(orderItemId: Long, rating: Int, text: String?): Result<Unit> }`.

- [ ] **Step 1: Write failing tests** (MockK API, `TestPager`/`asSnapshot`): pages follow `nextCursor` until null; an API failure surfaces as `LoadResult.Error` carrying an `AppError`; `ProductQuery(query = "  tempe ")` sends `q = "tempe"` and `ProductQuery(query = "   ")` sends no `q`; `setFavorite(1, true)` calls `addFavorite`, `false` calls `removeFavorite`.
- [ ] **Step 2: Run it.** Expected: FAIL.
- [ ] **Step 3: Implement** with page size 20.
- [ ] **Step 4: Run it.** Expected: PASS.
- [ ] **Step 5: Commit** `feat: add catalog and social repositories`

### Task 11: Home and categories

**Files:**
- Create: `MAIN/ui/components/ProductCard.kt`, `MAIN/feature/home/HomeViewModel.kt`, `MAIN/feature/home/HomeScreen.kt`, `MAIN/feature/categories/CategoriesViewModel.kt`, `MAIN/feature/categories/CategoriesScreen.kt`
- Modify: `MAIN/ui/nav/Routes.kt`, `MAIN/ui/nav/AppNavHost.kt`
- Test: `TEST/feature/home/HomeViewModelTest.kt`, `TEST/ui/components/ProductCardTest.kt`

**Interfaces:**
- Consumes: `CatalogRepository`, `AdsRepository.adsOrEmpty`, `AdSlot`, `CartRepository`, `ProductSummary`, `Category`.
- Produces: `@Composable fun ProductCard(product: ProductSummary, onOpen: () -> Unit, onAdd: () -> Unit, modifier: Modifier = Modifier)` (shows "Habis" and a disabled add button when `stock == 0`); `HomeViewModel.state: StateFlow<HomeUiState>` with independent sections `categories: UiState<List<Category>>`, `featured: UiState<List<ProductSummary>>`, `partnerAds: List<Ad>`, `sponsoredAds: List<Ad>`; `fun addToCart(product: ProductSummary)`.
- Home layout (per mockup): fixed chip "Simo, Boyolali", search field (opens `Search`), static hero banner "Oleh-oleh Khas Simo", up to 4 category shortcuts + "Lainnya" (opens `Categories`), `HOME_PARTNER_CAROUSEL` slot titled "Mitra Unggulan", "Produk Pilihan" grid, `HOME_SPONSORED_CARD` slot. Search placeholder text is "Cari produk…".

- [ ] **Step 1: Write failing tests.** `HomeViewModelTest`: a failing categories call leaves `featured` loaded (sections fail independently); both ads calls failing still yields loaded content; an empty featured list exposes `Content(emptyList())`; `addToCart` stores a snapshot with `unitPrice = Rupiah(product.price)` and the `small` image URL. `ProductCardTest`: `stock = 0` shows "Habis" and the add button is disabled; add button and card meet 48dp touch target.
- [ ] **Step 2: Run them.** Expected: FAIL.
- [ ] **Step 3: Implement** both screens; Categories shows the grid from `categories()` and opens `ProductList(categoryId, name, false)`.
- [ ] **Step 4: Run them.** Expected: PASS.
- [ ] **Step 5: Commit** `feat: add home and categories`

### Task 12: Product list and search

**Files:**
- Create: `MAIN/feature/products/ProductListViewModel.kt`, `MAIN/feature/products/ProductListScreen.kt`, `MAIN/feature/products/SearchViewModel.kt`, `MAIN/feature/products/SearchScreen.kt`
- Modify: `MAIN/ui/nav/Routes.kt`, `MAIN/ui/nav/AppNavHost.kt`
- Test: `TEST/feature/products/SearchViewModelTest.kt`, `TEST/feature/products/ProductListViewModelTest.kt`

**Interfaces:**
- Consumes: `CatalogRepository`, `ProductQuery`, `ProductCard`, `CartRepository`.
- Produces: routes `ProductList(categoryId: Long?, title: String, featured: Boolean)` and `Search`; `SearchViewModel` with `fun onQueryChange(text: String)` and `val results: Flow<PagingData<ProductSummary>>`; `ProductListViewModel(savedStateHandle)` reading the route.

- [ ] **Step 1: Write failing tests.** `SearchViewModelTest`: typing "t", "te", "tem" within 300 ms issues one request for `"tem"`; blank or whitespace-only input makes no request and shows the idle state; a changed query cancels the previous one (`flatMapLatest`). `ProductListViewModelTest`: `ProductList(categoryId = 5, …)` queries `ProductQuery(categoryId = 5)`; `featured = true` queries `featured = true`.
- [ ] **Step 2: Run them.** Expected: FAIL.
- [ ] **Step 3: Implement** list/grid with Paging 3 append loading and an empty state "Produk tidak ditemukan".
- [ ] **Step 4: Run them.** Expected: PASS.
- [ ] **Step 5: Commit** `feat: add product list and search`

### Task 13: Product detail

**Files:**
- Create: `MAIN/feature/products/detail/ProductDetailViewModel.kt`, `MAIN/feature/products/detail/ProductDetailScreen.kt`
- Modify: `MAIN/ui/nav/Routes.kt`, `MAIN/ui/nav/AppNavHost.kt`
- Test: `TEST/feature/products/detail/ProductDetailViewModelTest.kt`, `TEST/feature/products/detail/ProductDetailScreenTest.kt`

**Interfaces:**
- Consumes: `CatalogRepository.product`, `SocialRepository.setFavorite`, `CartRepository.add`, `SessionStore`, `QuantityStepper`, `UiEvent`.
- Produces: route `ProductDetail(productId: Long)`; `ProductDetailViewModel` with `state: StateFlow<UiState<ProductDetail>>`, `quantity: StateFlow<Int>`, `events: Flow<UiEvent>`, `setQuantity(n)`, `addToCart()`, `toggleFavorite()`.
- Screen content (per mockup, no shipping): image pager with "1/N", name, seller row (opens `Seller`), rating, price, stock, **pickup block** (address and opening hours of the seller), description, quantity stepper, "Tambah ke Keranjang", heart toggle. "Halal ID" badge only when `product.halalVerified`.

- [ ] **Step 1: Write failing tests.** ViewModel: quantity clamps to `1..min(stock, 99)`; `stock = 0` makes `addToCart` a no-op and surfaces "Stok habis"; `addToCart` stores a snapshot with the right seller and price; `toggleFavorite` while logged out emits `UiEvent.LoginRequired` and makes no API call; when logged in it flips optimistically and rolls back on failure. Screen: badge node absent when `halalVerified = false`; present when `true`; "Stok habis" button disabled at `stock = 0`; pickup address and hours are displayed.
- [ ] **Step 2: Run them.** Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run them.** Expected: PASS.
- [ ] **Step 5: Commit** `feat: add product detail`

### Task 14: Seller storefront

**Files:**
- Create: `MAIN/feature/seller/SellerViewModel.kt`, `MAIN/feature/seller/SellerScreen.kt`
- Modify: `MAIN/ui/nav/Routes.kt`, `MAIN/ui/nav/AppNavHost.kt`
- Test: `TEST/feature/seller/SellerViewModelTest.kt`

**Interfaces:**
- Consumes: `CatalogRepository.seller/products/sellerReviews`, `SocialRepository.setFollowing`, `IndonesianPhone.toWhatsAppUrl`, `SessionStore`, `ProductCard`.
- Produces: route `Seller(sellerId: Long)`; `SellerViewModel` with `state`, `events`, `toggleFollow()`, `val products: Flow<PagingData<ProductSummary>>`, `val reviews: Flow<PagingData<Review>>`, `val whatsappUrl: String?`.
- Screen: header (name, location, rating, "Follow"), tabs "Produk" / "Ulasan", pickup address and hours, "Chat dengan penjual" button.

- [ ] **Step 1: Write failing tests.** `toggleFollow` logged out emits `LoginRequired` and calls nothing; logged in flips optimistically and rolls back on failure; `whatsappUrl` is `https://wa.me/6281234567890` for a valid number and `null` for a malformed one (button hidden); products load with `ProductQuery(sellerId = id)`.
- [ ] **Step 2: Run it.** Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run it.** Expected: PASS.
- [ ] **Step 5: Commit** `feat: add seller storefront`

### Task 15: Cart screen and validation

**Files:**
- Create: `MAIN/feature/checkout/CheckoutRepository.kt`, `MAIN/feature/cart/CartViewModel.kt`, `MAIN/feature/cart/CartScreen.kt`
- Modify: `MAIN/ui/nav/Routes.kt`, `MAIN/ui/nav/AppNavHost.kt`
- Test: `TEST/feature/cart/CartViewModelTest.kt`

**Interfaces:**
- Consumes: `CartRepository`, `OrdersApi.validateCart`, `CartValidation`, `ValidatedLine`, `LineIssue`, `SessionStore`, `QuantityStepper`, `UiEvent`.
- Produces: `interface CheckoutRepository { suspend fun validate(lines: List<CartLine>): Result<CartValidation>; suspend fun placeOrder(lines: List<CartLine>): Result<Order> }` (`placeOrder` implemented in Task 16); route `Cart`; `CartViewModel` with `state: StateFlow<CartUiState>` (`groups` by seller, `total: Rupiah`, `notice: String?`, `canCheckout: Boolean`, `validating: Boolean`), `events`, `setQuantity(lineId, qty)`, `remove(lineId)`, `adjustToAvailable(lineId)`, `checkout()`.
- No ad slot and no shipping line on this screen.

- [ ] **Step 1: Write failing tests.** Lines are grouped by seller; validation reruns (debounced 500 ms) after any cart change; server `unitPrice` that differs from the local snapshot updates the snapshot via `updateSnapshot` and sets `notice = "Harga diperbarui"`; `OUT_OF_STOCK` and `PRODUCT_UNAVAILABLE` lines set `canCheckout = false` until removed; `INSUFFICIENT_STOCK` offers `adjustToAvailable`, which sets quantity to `availableStock`; `total` equals the sum of validated `lineTotal` values as `Rupiah`; while offline `canCheckout = false` with the offline message; `checkout()` logged out emits `LoginRequired`; empty cart shows an empty state.
- [ ] **Step 2: Run it.** Expected: FAIL.
- [ ] **Step 3: Implement** `validate` (maps `CartLine` → `CartLineInput`) and the screen.
- [ ] **Step 4: Run it.** Expected: PASS.
- [ ] **Step 5: Commit** `feat: add cart screen with server validation`

### Task 16: Checkout and order placement

**Files:**
- Create: `MAIN/feature/checkout/IdempotencyKeyStore.kt`, `MAIN/feature/checkout/CheckoutViewModel.kt`, `MAIN/feature/checkout/CheckoutScreen.kt`
- Modify: `MAIN/feature/checkout/CheckoutRepository.kt`, `MAIN/ui/nav/Routes.kt`, `MAIN/ui/nav/AppNavHost.kt`
- Test: `TEST/feature/checkout/CheckoutViewModelTest.kt`, `TEST/feature/checkout/IdempotencyKeyStoreTest.kt`

**Interfaces:**
- Consumes: `CheckoutRepository`, `CartRepository`, `SessionStore`, `CartLine`, `Order`.
- Produces: `class IdempotencyKeyStore(dataStore)` with `suspend fun keyFor(lines: List<CartLine>): String` (persisted, keyed by SHA-256 of the sorted `productId:quantity` list; same fingerprint returns the same key, new fingerprint creates a new one) and `suspend fun clear()`; route `Checkout`; `CheckoutViewModel` with `state: StateFlow<CheckoutUiState>` (`Review`, `Submitting`, `Failed(AppError)`, `Placed(Order)`), `events`, `placeOrder()`.
- Screen (per mockup, pickup): per-seller "Ambil di Toko" block (pickup address and hours), order summary, total, payment "Bayar di toko saat mengambil" selected, QRIS row disabled "Segera hadir", button "Buat Pesanan". No ad slot.

- [ ] **Step 1: Write failing tests.**
  - `IdempotencyKeyStoreTest`: same lines in any order give the same key; a changed quantity gives a new key; a new store instance over the same DataStore returns the same key; `clear()` makes the next call return a new key.
  - `CheckoutViewModelTest`: a second `placeOrder()` while `Submitting` sends no request; after `AppError.Offline`, retry sends the **same** key; success removes only the ordered cart lines, clears the key, and emits `Placed`; `Conflict("INSUFFICIENT_STOCK")` leaves the cart intact, triggers revalidation and returns to the cart with `Message("Stok berubah. Periksa keranjang Anda.")`; logged out → `LoginRequired`, no request.
- [ ] **Step 2: Run them.** Expected: FAIL.
- [ ] **Step 3: Implement** `placeOrder` in the repository (`createOrder(idempotencyKey = keyStore.keyFor(lines), …)`) and the screen.
- [ ] **Step 4: Run them.** Expected: PASS.
- [ ] **Step 5: Commit** `feat: add checkout with idempotent order placement`

### Task 17: Order confirmation and tracking

**Files:**
- Create: `MAIN/core/DateFormat.kt`, `MAIN/feature/orders/OrderStatusUi.kt`, `MAIN/feature/orders/OrderTimeline.kt`, `MAIN/feature/orders/OrdersRepository.kt`, `MAIN/feature/orders/ConfirmationScreen.kt`, `MAIN/feature/orders/TrackingViewModel.kt`, `MAIN/feature/orders/TrackingScreen.kt`
- Modify: `MAIN/ui/nav/Routes.kt`, `MAIN/ui/nav/AppNavHost.kt`
- Test: `TEST/core/DateFormatTest.kt`, `TEST/feature/orders/OrderTimelineTest.kt`, `TEST/feature/orders/TrackingViewModelTest.kt`

**Interfaces:**
- Consumes: `OrdersApi.getOrder`, `Order`, `OrderStatus`, `SellerOrder`, `AdsRepository.adsOrEmpty`, `IndonesianPhone`.
- Produces: `object DateFormat { fun dateTime(at: Instant): String; fun date(at: Instant): String }` (zone `Asia/Jakarta`, locale `id-ID`, e.g. `"15 Okt 2026, 10:24"`); `fun OrderStatus.label(): String` ("Pesanan dibuat", "Diproses penjual", "Siap diambil", "Selesai", "Dibatalkan"); `data class TimelineStep(val label: String, val state: StepState, val at: Instant?)` with `enum class StepState { DONE, CURRENT, PENDING, CANCELLED }`; `fun OrderTimeline.steps(order: Order): List<TimelineStep>`; `interface OrdersRepository { suspend fun order(id: Long): Result<Order>; fun orders(tab: OrderTab): Flow<PagingData<Order>> }` with `enum class OrderTab(val apiValue: String, val label: String) { ALL("ALL","Semua"), PROCESSING("PROCESSING","Diproses"), READY("READY","Siap Diambil"), DONE("DONE","Selesai") }`; routes `OrderConfirmation(orderId)` and `OrderTracking(orderId)`.
- Confirmation: success mark, `#<number>`, "Pesanan Anda sedang diproses oleh penjual", payment "Bayar di toko saat mengambil", each seller's pickup address and hours, `POST_PURCHASE` ad, buttons "Lihat Pesanan" and "Kembali ke Beranda". Tracking: timeline, per-seller status with note, "Chat dengan penjual", `WAIT_STATE` ad only while the order is not `COMPLETED`/`CANCELLED`, pull-to-refresh (no polling).

- [ ] **Step 1: Write failing tests.** `DateFormatTest`: `Instant.parse("2026-10-15T03:24:00Z")` → `"15 Okt 2026, 10:24"`; `date()` → `"15 Okt 2026"`. `OrderTimelineTest`: for each of `CREATED`, `SELLER_PROCESSING`, `READY_FOR_PICKUP`, `COMPLETED` the steps before the current are `DONE`, the current is `CURRENT` (the last one `DONE` when `COMPLETED`), later ones `PENDING`; `CANCELLED` after `SELLER_PROCESSING` yields two `DONE` steps then a final step `"Dibatalkan"` in state `CANCELLED` and no pending steps; dates come from `statusHistory`. `TrackingViewModelTest`: wait-state ad is not requested for a `COMPLETED` order; an ads failure leaves the order visible; refresh refetches the order.
- [ ] **Step 2: Run them.** Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run them.** Expected: PASS.
- [ ] **Step 5: Commit** `feat: add order confirmation and tracking`

### Task 18: Order history

**Files:**
- Create: `MAIN/feature/orders/history/OrderHistoryViewModel.kt`, `MAIN/feature/orders/history/OrderHistoryScreen.kt`
- Modify: `MAIN/ui/nav/AppNavHost.kt` (Orders tab)
- Test: `TEST/feature/orders/history/OrderHistoryViewModelTest.kt`

**Interfaces:**
- Consumes: `OrdersRepository.orders`, `OrderTab`, `OrderStatus.label()`, `DateFormat`, `SessionStore`.
- Produces: `OrderHistoryViewModel` with `selectedTab: StateFlow<OrderTab>`, `orders: Flow<PagingData<Order>>`, `select(tab)`; logged-out state shows a login prompt.
- Row: `#<number>`, `DateFormat.date`, total as `Rupiah.format()`, up to three item thumbnails, status chip (Diproses / Siap Diambil / Selesai / Dibatalkan).

- [ ] **Step 1: Write failing tests.** Selecting `READY` requests `tab = "READY"`; default tab is `ALL`; logged out does not call the API and exposes the login prompt; an empty page exposes the tab-specific empty state.
- [ ] **Step 2: Run it.** Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run it.** Expected: PASS.
- [ ] **Step 5: Commit** `feat: add order history`

### Task 19: Favorites tab and review submission

**Files:**
- Create: `MAIN/feature/social/FavoritesViewModel.kt`, `MAIN/feature/social/FavoritesScreen.kt`, `MAIN/feature/review/WriteReviewViewModel.kt`, `MAIN/feature/review/WriteReviewScreen.kt`
- Modify: `MAIN/ui/nav/Routes.kt`, `MAIN/ui/nav/AppNavHost.kt`, `MAIN/feature/orders/TrackingScreen.kt` (a "Beri ulasan" button on items of `COMPLETED` seller-orders where `reviewed == false`)
- Test: `TEST/feature/social/FavoritesViewModelTest.kt`, `TEST/feature/review/WriteReviewViewModelTest.kt`

**Interfaces:**
- Consumes: `SocialRepository`, `SessionStore`, `ProductCard`.
- Produces: `FavoritesViewModel` (`favorites: Flow<PagingData<ProductSummary>>`, `remove(productId)` optimistic); route `WriteReview(orderItemId: Long, productName: String)`; `WriteReviewViewModel` with `rating: StateFlow<Int>`, `text: StateFlow<String>`, `canSubmit: StateFlow<Boolean>`, `submit()`, `events`.

- [ ] **Step 1: Write failing tests.** `canSubmit` is false until a rating of 1–5 is chosen; text is trimmed and capped at 500 characters; `Conflict` from `submitReview` shows "Ulasan sudah dikirim." and closes; success closes with a message; `remove` drops the row immediately and restores it on failure; logged-out favorites exposes the login prompt.
- [ ] **Step 2: Run them.** Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run them.** Expected: PASS.
- [ ] **Step 5: Commit** `feat: add favorites and reviews`

### Task 20: Profile and push notifications

**Files:**
- Create: `MAIN/feature/profile/ProfileRepository.kt`, `MAIN/feature/profile/ProfileViewModel.kt`, `MAIN/feature/profile/ProfileScreen.kt`, `MAIN/push/PushTokenRegistrar.kt`, `MAIN/push/OrderStatusMessagingService.kt`, `MAIN/push/NotificationRoutes.kt`
- Modify: `android/app/build.gradle.kts` (FCM + `google-services` plugin applied to the `live` flavor only; Gradle property `nusantara.supportWhatsapp` exposed as `BuildConfig.SUPPORT_WHATSAPP`), `AndroidManifest.xml`, `MAIN/feature/checkout/CheckoutScreen.kt` (request `POST_NOTIFICATIONS` once after the first `Placed`)
- Test: `TEST/feature/profile/ProfileViewModelTest.kt`, `TEST/push/PushTokenRegistrarTest.kt`, `TEST/push/NotificationRoutesTest.kt`

**Interfaces:**
- Consumes: `MeApi`, `AuthRepository.logout`, `SessionStore`, `IndonesianPhone`.
- Produces: `interface ProfileRepository { suspend fun me(): Result<User>; suspend fun updateName(name: String): Result<User>; suspend fun registerDevice(token: String): Result<Unit> }`; `class PushTokenRegistrar` (`fun onToken(token: String)` and re-registration when a session appears; skips when logged out); `object NotificationRoutes { fun routeFor(data: Map<String, String>): Route? }` mapping FCM data `{orderId, status}` to `OrderTracking(orderId)`; notification channel `order_status` ("Status pesanan").
- Profile menu: header (name, phone, edit name), "Pesanan Saya", "Bantuan & FAQ" (opens `SUPPORT_WHATSAPP`), "Tentang Aplikasi", "Keluar". No address or payment-method rows. Logged out shows "Masuk".

- [ ] **Step 1: Write failing tests.** `ProfileViewModelTest`: name is trimmed and must be 1–50 characters; logout calls `AuthRepository.logout`, clears the session, and leaves the cart untouched. `PushTokenRegistrarTest`: a token received while logged in is registered once; a token received while logged out is held and registered after login; the same token is not registered twice. `NotificationRoutesTest`: `{orderId:"7", status:"READY_FOR_PICKUP"}` → `OrderTracking(7)`; missing or non-numeric `orderId` → `null`.
- [ ] **Step 2: Run them.** Expected: FAIL.
- [ ] **Step 3: Implement.** The demo flavor does not initialise Firebase. The live flavor needs a Firebase project and `google-services.json` from the owner.
- [ ] **Step 4: Run them.** Expected: PASS.
- [ ] **Step 5: Commit** `feat: add profile and push notifications`

### Task 21: Splash and onboarding

**Files:**
- Create: `MAIN/feature/onboarding/OnboardingStore.kt`, `MAIN/feature/onboarding/OnboardingScreen.kt`, `MAIN/feature/onboarding/SplashScreen.kt`
- Modify: `MAIN/ui/nav/AppNavHost.kt`
- Test: `TEST/feature/onboarding/OnboardingStoreTest.kt`

**Interfaces:**
- Produces: `class OnboardingStore(dataStore)` with `val seen: Flow<Boolean>` and `suspend fun markSeen()`; start destination is `Onboarding` until seen, then the tab shell.
- Copy: splash "Dari Lokal, Untuk Semua" with chip "Simo, Boyolali"; onboarding 1 "Temukan Produk UMKM Lokal"; onboarding 2 "Pesan, lalu Ambil di Toko" (describes pickup and cash payment); both have "Lewati" and "Lanjut".

- [ ] **Step 1: Write failing tests.** `seen` is false on a fresh store; after `markSeen()` it is true, including in a new store instance; "Lewati" calls `markSeen` (Compose test on the screen).
- [ ] **Step 2: Run it.** Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run it.** Expected: PASS.
- [ ] **Step 5: Commit** `feat: add splash and onboarding`

### Task 22: End-to-end smoke test, release build, backend handoff

**Files:**
- Create: `TEST/e2e/PurchaseSmokeTest.kt`, `docs/api/README.md`
- Modify: `android/app/proguard-rules.pro` (keep rules for generated models and kotlinx-serialization)

**Interfaces:**
- Consumes: everything above, via Hilt (`@HiltAndroidTest`, `HiltTestApplication`) on the demo flavor.

- [ ] **Step 1: Write the smoke test** `purchase_flow_from_browse_to_tracking`: skip onboarding; tap the first featured product; "Tambah ke Keranjang"; open the cart (Keranjang) and tap checkout; complete login with any phone and code (fixtures accept them); on checkout assert "Bayar di toko saat mengambil" and "Ambil di Toko" are shown and no "Ongkir" text exists; tap "Buat Pesanan"; assert "Pesanan Berhasil!" and `#NM-20261015-001`; tap "Lihat Pesanan"; assert the timeline shows "Siap diambil".
- [ ] **Step 2: Run it.** Expected: FAIL before the missing glue is fixed, then fix glue only (no new features) until PASS.
- [ ] **Step 3: Write `docs/api/README.md`** for the backend team: operation → screen table, required seed data (the nine categories, four ad placements each with at least one placeholder campaign), the FCM payload `{orderId, status}`, the error codes used by the app (`OTP_INVALID`, `OTP_RATE_LIMITED`, `INSUFFICIENT_STOCK`, `PRODUCT_UNAVAILABLE`), and how to run the demo flavor.
- [ ] **Step 4: Verify the whole module.** Run `cd android && ./gradlew :app:testDemoDebugUnitTest :app:lintDemoDebug :app:assembleDemoRelease`. Expected: all tests pass, lint has no errors, and the R8 release build succeeds. A run on a real 2 GB device is a manual check this plan cannot automate.
- [ ] **Step 5: Commit** `test: add purchase smoke test and backend handoff doc`

---

## Self-review

- **Spec coverage:** browsing, search, categories, detail with pickup info and verified-only halal badge (Tasks 11–13); seller storefront, follow, WhatsApp contact (14); local cart plus server validation (9, 15); pickup checkout with cash payment and idempotent order creation (16); confirmation, tracking timeline, history tabs (17, 18); favorites and reviews (19); OTP login gating (7, 13–16, 18, 19); four ad placements with labels and none in cart/checkout (8, 11, 17); push on status change (20); onboarding and splash (21); demo/offline operation, contract, and backend handoff (4, 5, 22). Not in this plan by design: backend, operator admin, seller portal, AdMob, QRIS, wholesale UI.
- **Type consistency:** `CartLine`/`CartProductSnapshot` (Task 9) are used unchanged in 11, 13, 15, 16; `CheckoutRepository` is declared in Task 15 and extended in 16; `OrdersRepository`/`OrderTab` are declared in Task 17 and used in 18; `UiEvent.LoginRequired` (Task 2) is used in 13, 14, 15, 16, 19; operation and schema names come only from Task 4's tables.
- **Gap fixed during review:** `listProductReviews` was dropped from the contract because no screen uses it; ratings on product cards come from `ProductSummary`.
