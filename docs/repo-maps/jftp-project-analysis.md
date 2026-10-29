# JFTP project analysis

## Scope and evidence

- Repository: `jftp`
- Revision examined: `14e62ce` (`master`)
- Direct source scope: tracked files under `src/main`, `src/test`, `pom.xml`, `README.md`, assembly and launcher scripts
- Inventory: 576 tracked files; 182 Java files and about 29,330 Java source lines; 57 base resource bundles, 57 German bundles, 58 Traditional Chinese bundles, 89 HTML help topics, and 50 image assets
- Tests: `src/test/java/.gitkeep` and `src/test/resources/.gitkeep` only; no executable test classes were found

The native `legacy-repo-map` could not be installed in this environment. The pinned setup first failed because the default cache was not writable (`WinError 5`), then the task-local retry was blocked while requesting:
`https://github.com/EdukeyTeam/agent-toolbox/releases/download/legacy-tools-v0.3.1/legacy-tools-release.json`
with Windows `WinError 10013` (socket access denied). This report therefore uses direct source inspection and recorded repository inventory rather than the native map. The Python fallback was not used because the skill requires explicit acknowledgement before selecting it.

Line references and inventory counts are observed from this revision. Security, reliability and modernization consequences are engineering inferences from those observations; protocol behavior that depends on the external `ftpapi` binary was not executed here.

## Implemented in this working copy

The approved first implementation batch is now present: Java 17 Maven settings and CI scaffolding, a tracked `.gitignore`, encrypted versioned preference and favorite stores with legacy migration, filtered legacy deserialization, ZIP extraction containment checks, TLS protocol and SAN hostname improvements, bounded HTTPS update checks, removal of `finally` returns, and focused ZIP/encrypted-store regression tests. The original risk descriptions below describe the pre-change baseline where they are explicitly marked as legacy behavior. Full Maven verification is still blocked by the unavailable Maven executable and external `ftpapi`/JavaHelp artifacts.

## System shape

JFTP is a Swing desktop FTP/FTPS client with a legacy applet entry point. `JFTPApplication` is the executable entry point (`src/main/java/com/myjavaworld/jftp/JFTPApplication.java:65-67`), `JFTP` owns the main window, tabs, preferences, menus and global registries, and `FTPSession` combines connection, directory browsing, transfer, filtering, progress, file monitoring and status handling (`src/main/java/com/myjavaworld/jftp/FTPSession.java:63-72`). The UI is split into local and remote panes, dialogs, menus and action classes. Utilities cover persistence, resources, file watching, encoding and ZIP operations; the SSL package manages key stores and certificate prompts.

The FTP client and list parser are selected by class name and instantiated reflectively during connection (`src/main/java/com/myjavaworld/jftp/FTPSession.java:617-637`). This gives the product protocol and parser variation without recompiling the UI, but also makes wiring and failure modes runtime-only.

## Strong points

### Product coverage and user-facing completeness

The repository contains a broad, coherent desktop product rather than a thin protocol wrapper: multiple sessions, local and remote browsing, recursive transfers, filtered transfers, ZIP upload/download flows, proxy support, certificate management, favorites, permissions, raw commands, transfer-mode detection and localized UI/help. The README documents this feature surface, while the source contains dedicated action, dialog, pane and SSL packages. The resource inventory also shows substantial localization and help coverage.

### Reasonable package-level separation

The code is organized into application/UI (`com.myjavaworld.jftp`), reusable Swing widgets (`com.myjavaworld.gui`), utilities, ZIP support and SSL support. That is a workable seam for incremental modernization: transfer and persistence behavior can be extracted without rewriting every dialog first.

### Background work is recognized as a UI concern

Network and file operations are generally dispatched through the project’s `SwingWorker`, which runs `construct()` on a worker thread and posts `finished()` back with `SwingUtilities.invokeLater` (`src/main/java/com/myjavaworld/gui/SwingWorker.java:132-146`). `FTPSession` uses that worker around connect, list and transfer operations (`src/main/java/com/myjavaworld/jftp/FTPSession.java:617-698`). This is an important foundation for preserving Swing responsiveness during refactoring.

### Packaging and launch metadata exist

The Maven build creates an executable JAR with `JFTPApplication` as its main class and a `lib/` classpath (`pom.xml:111-125`). The assembly descriptor emits ZIP, directory and tar.gz distributions and includes launch scripts (`src/main/assembly/binary.xml:5-53`, `src/main/scripts/jftp.bat`, `src/main/scripts/jftp.sh`). That gives a clear place to add reproducible builds, checksums and SBOMs later.

### Existing compatibility and release history

The project has a serial version identifier for preferences (`src/main/java/com/myjavaworld/jftp/JFTPPreferences.java:38-46`), explicit release tags in Git, and an Apache 2.0 license. Those are useful anchors for a staged migration and for preserving existing user data while changing storage formats.

## Weak points and risks

### Build and reproducibility risk — high priority

Before this change, the POM pinned Maven compiler plugin 2.4 and Java source/target 1.5. The working copy now targets Java 17 with current compiler, Surefire and Enforcer plugins, JUnit 5, a tracked `.gitignore`, and a GitHub Actions matrix. The remaining reproducibility blocker is the dated external dependency set (`ftpapi:3.0.0`, JavaHelp 2.0.05; `pom.xml:65-80`) and its legacy repository configuration (`pom.xml:39-57`).

### Verification gap — high priority

The baseline test tree contained only placeholder `.gitkeep` files. The working copy now adds focused tests for ZIP traversal/empty entries and encrypted property round trips. Connection lifecycle, transfer cancellation, persistence migration, certificate decisions and UI startup still need integration coverage.

### Credential and persistence design — high priority

The baseline favorites and preferences formats used Java serialization and a hard-coded AES key. New writes now use versioned properties encrypted with AES-GCM and a generated per-user key (`src/main/java/com/myjavaworld/util/EncryptedPropertiesStore.java`, `src/main/java/com/myjavaworld/jftp/PreferencesStore.java`, `src/main/java/com/myjavaworld/jftp/FavoritesManager.java`). Legacy `.ser` files are accepted only through a filtered migration reader; the compatibility key remains solely for reading old favorites and should be removed after a documented migration window.

The default certificate-store password is the well-known `changeit` value (`src/main/java/com/myjavaworld/jftp/JFTPPreferences.java:41-46`, `135-138`). Connection passwords are held as `String` values and copied from password fields into immutable strings (`src/main/java/com/myjavaworld/jftp/ConnectionDlg.java:614-615`). These choices make secret lifetime and storage harder to control.

### TLS validation is bespoke and dated — high priority

The working copy now requests `TLS`, checks the standard trust manager first, and evaluates SAN DNS/IP names with wildcard handling before the legacy CN fallback (`src/main/java/com/myjavaworld/jftp/ssl/JFTPSSLContext.java:37-43`, `src/main/java/com/myjavaworld/jftp/ssl/JFTPTrustManager.java:76-97`, `130-175`). The user-approved certificate exception flow remains and still needs dedicated certificate test coverage.

### ZIP extraction had a path traversal flaw — fixed in this batch

The baseline `Unzip` implementation concatenated entry names directly to the target directory. It now canonicalizes the extraction root, rejects traversal and symbolic-link escapes, resets its close state, reports the extracted target, and handles zero-byte entries (`src/main/java/com/myjavaworld/zip/Unzip.java:134-217`). Regression tests cover traversal and empty entries.

### Error handling hides failures — medium/high priority

The code still contains many broad `catch (Exception)`/`catch (Throwable)` blocks and direct `printStackTrace()` calls. This batch removes the exception-suppressing `finally` returns from preference loading and the session worker, but logging and error categorization remain follow-up work.

The auto-updater now uses HTTPS, bounded `HttpClient` timeouts, a daemon thread, bounded retries, numeric version comparison and EDT handoff (`src/main/java/com/myjavaworld/jftp/AutoUpdater.java:31-124`). Signed metadata and structured logging remain follow-up work.

### Coupling and maintainability — medium priority

`FTPSession` is a 1,667-line coordinator implementing connection, control/data listeners, UI actions, file watching and ZIP progress. `JFTP` also owns global preferences, class registries, serialization and window lifecycle. The system can be evolved, but these classes are expensive seams for testing and invite regressions when a feature crosses UI and protocol concerns.

The code uses raw collections, deprecated boxed constructors, reflection via `Class.forName(...).newInstance()`, and a home-grown worker abstraction. These are manageable in isolation but compound the cost of moving to a supported Java release.

### Platform and product lifecycle risk — medium priority

The applet entry point (`JFTPApplet`) depends on a browser technology removed from modern JDK/browser environments. The product should define a desktop-only support position and remove or isolate applet code before a Java upgrade. The current auto-update endpoint and product URLs also point to legacy `jMethods.com` HTTP/FTP locations.

## Tools that will be useful

Use the following in this order:

1. **Build baseline:** Maven Wrapper, Maven Enforcer, `maven-dependency-plugin`, `jdeps`, `jdeprscan`, and a JDK matrix (start with the oldest supported JDK for characterization, then current LTS). Add `maven-source-plugin` and reproducible-archive settings after the first green build.
2. **Unit and integration tests:** JUnit 5 plus a temporary-profile filesystem harness; Apache MINA FTPServer or MockFtpServer for protocol tests; AssertJ for assertions; Mockito only at seams that cannot be made pure. Add AssertJ Swing or a small headless startup smoke test for the desktop shell.
3. **Static quality and security:** SpotBugs with FindSecBugs, PMD, Checkstyle or Spotless, Error Prone, and OWASP Dependency-Check (or CycloneDX for an SBOM). Add a Semgrep rule for unsafe ZIP extraction, Java serialization, hard-coded keys, and `printStackTrace`.
4. **TLS and credential testing:** `keytool` plus generated test certificates, hostname/SAN cases, expired certificates and trust-store migration tests. Use `ObjectInputFilter` only as a migration guard; do not treat it as the long-term persistence format.
5. **Runtime diagnosis:** Java Flight Recorder and thread/heap dumps for transfer throughput, cancellation and Swing event-dispatch stalls. Use a packet capture only in controlled test environments when validating FTP/FTPS data-channel behavior.
6. **Architecture and documentation:** Javadoc for public seams, Mermaid or PlantUML for the connection/transfer lifecycle, and ArchUnit tests to keep UI, protocol, persistence and security dependencies directional.

## Recommended improvement sequence

### 1. Make the build repeatable

Add Maven Wrapper and CI, document supported JDKs, move all repository URLs to HTTPS, and update the compiler/plugin baseline. Characterize the existing application on a legacy-compatible JDK before changing bytecode level; then target a supported LTS (preferably Java 17 or 21) and remove applet-only code. Add a dependency report and SBOM to every release artifact.

### 2. Establish safety tests before broad refactoring

Start with pure tests for `RemoteHost`, transfer-type selection, local/remote filters, `FileUtilities`, ZIP round trips, and preference defaults. Add regression tests for ZIP-slip paths, zero-byte entries, close/reopen behavior, corrupted profiles and cancellation. Use an embedded FTP server to cover connect/login/list/upload/download/abort flows without external infrastructure.

### 3. Fix the immediate security issues

Implement canonical extraction-root checks and safe resource handling in `Unzip`. Replace Java serialization with a versioned, validated format. For stored secrets, prefer the operating system credential store; if a portable encrypted file is required, use a user-provided key and authenticated encryption such as AES-GCM with a versioned envelope and rotation path. Migrate old `.ser` files once, then stop accepting arbitrary serialized objects.

Replace the bespoke trust manager with the platform trust manager plus RFC-compliant hostname verification, enabled modern TLS protocols, and an explicit certificate exception model. Make update checks HTTPS-only with bounded timeouts, closed streams, signed metadata or a pinned update manifest, and no UI work from a raw background thread.

### 4. Reduce coupling around `JFTP` and `FTPSession`

Extract `PreferencesStore`, `FavoritesStore`, `CertificateStore`, `UpdateChecker`, `ConnectionService` and `TransferService`. Inject those services into the UI instead of reading global static state. Replace class-name strings with a typed registry or service-provider interface that validates implementations at startup. Keep `FTPSession` as a view/controller coordinator after protocol and transfer state machines move into testable services.

### 5. Improve observability and release discipline

Replace `printStackTrace` and swallowed exceptions with structured logging and user-facing error categories. Add cancellation and shutdown semantics to the executor used for transfers and update checks. Publish a support matrix, migration notes, threat model, build instructions, checksums and a signed release archive. Track the generated `docs/repo-maps/artifacts/` directory separately from the primary report if future maps or source packs are added.

## Priority summary

| Priority | Work | Why it comes first |
| --- | --- | --- |
| P0 | Repeatable build, CI, JDK support decision | The current Java 5 target is rejected by current JDKs, and there is no automated safety net. |
| P0 | ZIP-slip fix and persistence/credential redesign | These are concrete local-file and secret-handling risks. |
| P1 | TLS validation and HTTPS update path | Security behavior is central to an FTP client and currently depends on bespoke legacy code. |
| P1 | Characterization and embedded-FTP tests | Enables safe changes to connection and transfer code. |
| P2 | Split `JFTP`/`FTPSession`, replace raw types/reflection | Reduces long-term change cost after behavior is protected. |
| P2 | Packaging, observability, docs and desktop modernization | Improves supportability once the build and runtime are stable. |

## Suggested repository instruction pointer

If this report becomes part of the normal maintenance workflow, add a concise `AGENTS.md` pointer that tells future agents to consult `docs/repo-maps/jftp-project-analysis.md` for architecture, persistence, TLS, transfer workflows and known risks; refresh it after build-system, protocol or storage changes. No `AGENTS.md` was added automatically because the user requested analysis rather than repository instruction changes.
