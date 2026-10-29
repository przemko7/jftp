# JFTP code graph

Open [jftp-architecture.html](jftp-architecture.html) directly in a browser. Click any node to see its source file and evidence link.

On Windows PowerShell, run:

    Start-Process (Resolve-Path docs/code-graph/jftp-architecture.html)

You can also open the HTML file in VS Code and use its built-in browser or any
local browser. It has no network or JavaScript package dependency.

The portable Mermaid source is [jftp-architecture.mmd](jftp-architecture.mmd). Markdown editors that support Mermaid can render it:

~~~mermaid
flowchart LR
    App["JFTPApplication main()"] --> Shell["JFTP window and tabs"]
    Shell --> Session["FTPSession connection and transfer"]
    Shell --> Prefs["PreferencesStore encrypted settings"]
    Shell --> Fav["FavoritesManager encrypted favorites"]
    Prefs --> Store["EncryptedPropertiesStore AES-GCM"]
    Fav --> Store
    Shell --> Update["AutoUpdater HTTPS metadata"]
    Session --> Worker["SwingWorker background operations"]
    Session --> Local["LocalPane"]
    Session --> Remote["RemotePane"]
    Session -. reflective client/parser .-> FTP["ftpapi external dependency"]
    Session --> SSL["JFTPSSLContext TLS"]
    SSL --> Trust["JFTPTrustManager"]
    Trust --> Keys["KeyStoreManager"]
    Session --> Zip["Zip / Unzip"]
~~~

This is a source-reviewed architecture view, not a complete type-resolved call graph. Reflection, callbacks, resource wiring and the external `ftpapi` implementation remain boundaries.
