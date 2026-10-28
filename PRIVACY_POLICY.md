# Privacy Policy / Pravidlá ochrany súkromia

**Application Name:** Discover News (Discover)  
**Package Name:** `com.aistudio.cleandiscover.xwqz`  
**Effective Date:** October 28, 2026  
**Developer / Prevádzkovateľ:** JWorks s.r.o.  
**Contact Email:** jworks.s.r.o@gmail.com  
**Phone:** +421 951 039 735  

---

[English Version](#english-version) | [Slovenská verzia](#slovenská-verzia)

---

<a name="english-version"></a>
## Privacy Policy (English)

### 1. Introduction & Overview
This Privacy Policy governs the use of the **Discover News** mobile application ("the Application", "we", "our") developed for Android by **JWorks s.r.o.** We respect your privacy and are committed to protecting it. 

**Discover News is designed with a strict privacy-first philosophy.** The Application does not require user registration, does not create user profiles, does not sell or transfer user data, and does not use any third-party advertising or commercial tracking frameworks.

### 2. Information We Process and Store
All Application data is stored **locally on your device** in a private, sandboxed SQLite/Room database and local SharedPreferences. None of this data is transmitted to our servers or third parties.

- **Reading History:** Titles, snippets, and reading timestamps of articles you open in the reader mode. This enables you to revisit previously read content.
- **Bookmarks & Saved Articles:** Articles you explicitly choose to save for offline reading.
- **Source Preferences:** Enabled and disabled news sources, custom RSS feeds you manually add, and category selections.
- **Display & Reader Settings:** Chosen app theme, font choice, font size, accent color, and interface language.

### 3. Data Transmission & External Network Requests
The Application connects to the internet strictly to perform user-requested functions:
1. **Fetching News via RSS/Atom Feeds:** The Application downloads public RSS XML feeds directly from the news publishers you choose to follow (e.g., Aktuality.sk, SME.sk, Denník N, ČT24, etc.). When connecting to these external web servers, standard HTTP requests are made containing your IP address and standard user-agent header. These connections are subject to each respective publisher's privacy policy.
2. **Article Reader Mode (Content Extraction):** When you tap an article to read it, the Application fetches the web page content directly from the original source URL to extract clean, ad-free text and hero images for local display.
3. **Application Updates:** The Application may query the public GitHub API repository (`frodo21/Discover_public`) to check for newer releases and download APK updates if approved by the user.

### 4. Device Permissions
The Application requests only the minimal permissions required for its functionality:
- `android.permission.INTERNET`: Required to download public news feeds, article content, and check for app updates.
- `android.permission.ACCESS_NETWORK_STATE`: Used to detect whether an active Wi-Fi or mobile data connection exists before initiating network requests.
- `android.permission.POST_NOTIFICATIONS`: Used on Android 13+ solely to inform the user of completed background updates.

The Application **does NOT request or access:**
- Personal identity, Google account details, or phone number
- Precise or coarse GPS location
- Device contacts, calendar, or call logs
- Camera, microphone, or biometric sensors
- External photo/media storage (no `READ_EXTERNAL_STORAGE` or `READ_MEDIA_IMAGES`)
- Android Advertising ID (AAID)

### 5. Third-Party Trackers & Advertising
Discover News is **completely free of advertisements and tracking SDKs**:
- No Google AdMob or banner advertising networks.
- No analytics SDKs (no Firebase Analytics, Google Analytics, or Facebook Pixel).
- No cross-app user tracking or behavioral profiling.

### 6. Data Retention & Deletion Rights
Because all data is stored exclusively on your device, you retain total control over your information at all times:
- **Clearing Reading History:** You can permanently delete individual entries or clear your entire reading history via the "Vymazať históriu" button in the Reading History screen.
- **Deleting Cached Content:** You can clear read articles and downloaded content from the overflow menu in the main feed.
- **Complete Deletion:** Uninstalling the Application or using Android system settings (*Settings > Apps > Discover > Storage > Clear Data*) permanently and immediately destroys all databases, caches, preferences, and saved items from your device.

### 7. Children's Privacy
The Application does not knowingly collect or solicit any personal information from children under the age of 13.

### 8. Changes to This Privacy Policy
We may periodically update this Privacy Policy. The latest version will always be available at this public repository URL.

### 9. Contact Us
If you have questions, feedback, or concerns regarding this Privacy Policy or your privacy while using Discover News, please contact us at:  
**Company:** JWorks s.r.o.  
**Email:** `jworks.s.r.o@gmail.com`  
**Phone:** `+421 951 039 735`  

---

<a name="slovenská-verzia"></a>
## Pravidlá ochrany súkromia (Slovenská verzia)

### 1. Úvod a základné zásady
Tieto Pravidlá ochrany súkromia upravujú používanie mobilnej aplikácie **Discover News** (ďalej len „Aplikácia“) pre systém Android spoločnosťou **JWorks s.r.o.** Rešpektujeme vaše súkromie a kladieme dôraz na maximálnu ochranu vašich osobných údajov.

**Aplikácia Discover News je postavená na prísnom princípe ochrany súkromia (Privacy by Design).** Aplikácia nevyžaduje žiadnu registráciu ani prihlasovanie, nevytvára používateľské profily, nepredáva ani neposkytuje žiadne údaje tretím stranám a neobsahuje žiadne reklamné ani analytické sledovacie nástroje (trackery).

### 2. Spracúvané a ukladané údaje
Všetky údaje aplikácie sú ukladané **výhradne lokálne vo vašom zariadení** v izolovanom úložisku (SQLite/Room databáza a SharedPreferences). Žiadne z týchto údajov sa neodosielajú na naše servery ani tretím stranám.

- **História čítania:** Názvy, perexy a časové značky článkov, ktoré ste otvorili v režime čítačky. Slúžia výhradne na to, aby ste sa k nim mohli kedykoľvek vrátiť.
- **Záložky a uložené články:** Články, ktoré ste si sami uložili na neskoršie alebo offline čítanie.
- **Nastavenia zdrojov:** Zoznam zapnutých a vypnutých RSS zdrojov, vlastné RSS feedy pridané používateľom a zvolené kategórie.
- **Predvoľby vzhľadu:** Vybraná téma (svetlá, tmavá, sepia, OLED), typ a veľkosť písma, akcentová farba a jazyk aplikácie.

### 3. Sieťové požiadavky a externé spojenia
Aplikácia využíva pripojenie na internet výhradne na vykonanie funkcií požadovaných používateľom:
1. **Sťahovanie RSS správ:** Aplikácia sťahuje verejné RSS/Atom kanály priamo zo serverov vami zvolených spravodajských portálov (napr. Aktuality.sk, SME.sk, Denník N, ČT24 a ďalšie). Pri tomto spojení vzniká štandardná HTTP požiadavka voči danému vydavateľovi, ktorá podlieha pravidlám ochrany súkromia príslušného portálu.
2. **Režim čítania (extrakcia textu):** Pri otvorení článku aplikácia stiahne obsah z pôvodnej webovej adresy článku a očistí ho od reklám a rušivých prvkov pre pohodlné čítanie.
3. **Aktualizácie aplikácie:** Aplikácia môže kontrolovať dostupnosť novších verzií prostredníctvom verejného GitHub repozitára (`frodo21/Discover_public`) a umožniť stiahnutie inštalačného balíčka APK so súhlasom používateľa.

### 4. Povolenia aplikácie v systéme Android
Aplikácia požaduje len minimálne systémové oprávnenia nevyhnutné na jej fungovanie:
- `android.permission.INTERNET`: Nevyhnutné na sťahovanie verejných RSS správ, textu článkov a kontrolu aktualizácií.
- `android.permission.ACCESS_NETWORK_STATE`: Na zistenie dostupnosti siete (Wi-Fi / mobilné dáta) pred začatím sťahovania.
- `android.permission.POST_NOTIFICATIONS`: V systéme Android 13+ slúži výlučne na informovanie o dokončení sťahovania aktualizácie.

Aplikácia **nepožaduje ani nepristupuje k:**
- Polohe používateľa (GPS)
- Kontaktom, hovorom ani SMS správam
- Fotoaparátu ani mikrofónu
- Úložisku osobných fotografií a médií
- Reklamnému identifikátoru (Google Advertising ID)

### 5. Reklamy a sledovacie kódy tretích strán
Discover News **neobsahuje žiadne reklamy ani sledovacie moduly**:
- Žiadne reklamné bannery ani systémy ako Google AdMob.
- Žiadne analytické balíčky (žiaden Google Analytics, Firebase Analytics, Facebook SDK).
- Žiadne sledovanie používateľského správania naprieč inými aplikáciami.

### 6. Uchovávanie a vymazanie údajov
Keďže sú všetky dáta uložené len vo vašom zariadení, máte nad nimi plnú kontrolu:
- **Vymazanie histórie čítania:** Kedykoľvek môžete odstrániť jednotlivé záznamy alebo vymazať celú históriu jedným kliknutím cez tlačidlo „Vymazať históriu“ na obrazovke História čítania.
- **Správa záložiek:** Uložené články môžete kedykoľvek jednotlivo odobrať.
- **Úplné odstránenie dát:** Odinštalovaním aplikácie alebo vymazaním dát aplikácie v nastaveniach systému Android (*Nastavenia > Aplikácie > Discover > Úložisko > Vymazať údaje*) sa natrvalo a okamžite vymažú všetky databázy, vyrovnávacia pamäť aj nastavenia.

### 7. Ochrana súkromia detí
Aplikácia nezhromažďuje žiadne osobné údaje a je bezpečná pre používateľov všetkých vekových kategórií vrátane detí.

### 8. Kontakt a otázky
V prípade akýchkoľvek otázok alebo nejasností týkajúcich sa týchto pravidiel ochrany súkromia nás môžete kontaktovať na:  
**Spoločnosť:** JWorks s.r.o.  
**E-mail:** `jworks.s.r.o@gmail.com`  
**Telefón:** `+421 951 039 735`  
