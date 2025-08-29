# 📋 Changelog Plugin

Ein modernes Changelog Plugin für Minecraft Server mit GUI-Unterstützung, entwickelt von **DerGamer09**.

## ✨ Features

### 🎯 Hauptfunktionen
- **Moderne GUI** - Schöne, benutzerfreundliche Oberfläche mit Pagination
- **Datenbank-Unterstützung** - SQLite und MySQL Support
- **Async-Operationen** - Keine Server-Lags durch Datenbankoperationen
- **Umfangreiche Befehle** - Vollständige Verwaltung über Commands
- **Berechtigung-System** - Granulare Permissions für verschiedene Funktionen
- **Mehrsprachig** - Deutsche Lokalisierung mit Emojis
- **Memory-Safe** - Korrekte Ressourcenverwaltung ohne Memory Leaks

### 🎨 GUI Features
- Elegante Glasrand-Dekoration
- Farbige Changelog-Einträge mit detaillierter Anzeige
- Intuitive Navigation mit Vor/Zurück-Buttons
- Info-Panel mit Statistiken
- Responsive Design mit bis zu 28 Einträgen pro Seite

### 🔧 Technische Features
- **Thread-Safe** - Sichere Concurrent-Operationen
- **Auto-Reconnect** - Automatische Datenbankverbindungs-Wiederherstellung
- **Konfigurierbar** - Umfangreiche Anpassungsmöglichkeiten
- **Error-Handling** - Robuste Fehlerbehandlung mit detaillierten Logs
- **Resource Management** - Automatische Bereinigung von Datenbankressourcen

## 📦 Installation

1. **Plugin herunterladen** - Lade die `Changelog-1.0.0.jar` herunter
2. **In plugins/ Ordner** - Kopiere die JAR-Datei in den plugins/ Ordner deines Servers
3. **Server starten** - Starte den Server neu
4. **Konfiguration anpassen** - Bearbeite `plugins/Changelog/config.yml` nach Bedarf

## ⚙️ Konfiguration

### Datenbank-Setup

**SQLite (Empfohlen - Standard):**
```yaml
database:
  type: sqlite
```

**MySQL:**
```yaml
database:
  type: mysql
  mysql:
    host: localhost
    port: 3306
    database: changelog
    username: root
    password: 'dein_passwort'
```

### GUI-Anpassung
```yaml
gui:
  entriesPerPage: 28
  title: "&5&lChangelog"
  borderItem: GRAY_STAINED_GLASS_PANE
```

### Nachrichten anpassen
```yaml
messages:
  prefix: "&8[&6Changelog&8] "
  entryAdded: "&a✓ Changelog-Eintrag erfolgreich hinzugefügt!"
  noPermission: "&c❌ Keine Berechtigung."
```

## 🎮 Befehle

### Für alle Spieler:
- `/changelog` - Öffnet das Changelog-GUI
- `/changelog help` - Zeigt die Hilfe an

### Für Administratoren:
- `/changelog add <nachricht>` - Fügt einen neuen Changelog-Eintrag hinzu
- `/changelog remove <id>` - Entfernt einen Changelog-Eintrag
- `/changelog list` - Listet alle Changelog-Einträge auf
- `/changelog reload` - Lädt die Konfiguration neu

### Aliases:
- `/cl` - Kurz für `/changelog`
- `/changes` - Alternative für `/changelog`
- `/updates` - Alternative für `/changelog`

## 🔐 Berechtigungen

| Permission | Beschreibung | Standard |
|------------|-------------|----------|
| `changelog.*` | Alle Changelog-Berechtigungen | OP |
| `changelog.view` | Changelog anzeigen | Alle Spieler |
| `changelog.admin` | Alle Admin-Funktionen | OP |
| `changelog.add` | Einträge hinzufügen | OP |
| `changelog.remove` | Einträge entfernen | OP |
| `changelog.list` | Einträge auflisten | OP |
| `changelog.reload` | Konfiguration neuladen | OP |

## 🛠️ Entwickler-Informationen

### Technische Details:
- **Java Version:** 21
- **Bukkit API:** 1.21
- **Datenbank:** SQLite/MySQL
- **Threading:** Async database operations
- **Memory Management:** Automatic resource cleanup

### Architektur:
```
ChangelogPlugin (Main)
├── DatabaseManager (Database operations)
├── ChangelogCommand (Command handling + Tab completion)
└── ChangelogGUI (GUI management + Event handling)
```

### Verbesserungen in dieser Version:

#### 🎨 GUI Verbesserungen:
- Vollständig überarbeitetes Design mit Glasrand-Dekoration
- Bessere Farbgebung und Emoji-Integration
- Verbesserte Pagination mit korrekter Seitenberechnung
- Info-Panel mit Statistiken
- Responsive Layout für verschiedene Bildschirmgrößen

#### 🐛 Bug-Fixes:
- **Memory Leak Fix:** Korrekte Schließung von ResultSet-Objekten
- **Threading Fix:** Entfernung problematischer async GUI-Erstellung
- **Navigation Bugs:** Sichere Seitenberechnung und -navigation
- **Database Locks:** Thread-sichere Datenbankoperationen
- **Error Handling:** Robuste Fehlerbehandlung mit detailliertem Logging

#### 🔧 Technische Verbesserungen:
- **Auto-Reconnect:** Automatische Datenbankverbindungs-Wiederherstellung
- **Connection Validation:** Überprüfung der Datenbankverbindung vor Operationen
- **Resource Management:** Automatische Bereinigung mit try-with-resources
- **Thread Safety:** ReentrantReadWriteLock für sichere Concurrent-Operationen
- **Configuration Validation:** Validierung der Konfigurationswerte

#### 📝 Benutzerfreundlichkeit:
- Deutsche Lokalisierung mit Emojis
- Detaillierte Fehlermeldungen
- Tab-Completion für alle Befehle
- Umfangreiche Hilfe-Seiten
- Bessere Permissions-Struktur

## 📋 Changelog

### Version 1.0.0
- ✅ Komplett überarbeitetes GUI-Design
- ✅ Memory Leak Fixes
- ✅ Verbesserte Datenbankoperationen
- ✅ Thread-sichere Implementierung
- ✅ Deutsche Lokalisierung
- ✅ Tab-Completion
- ✅ Umfangreiche Konfigurationsmöglichkeiten
- ✅ Robuste Fehlerbehandlung

## 🤝 Support

Bei Problemen oder Fragen:
1. Überprüfe die Konsole auf Fehlermeldungen
2. Stelle sicher, dass alle Permissions korrekt gesetzt sind
3. Kontrolliere die `config.yml` auf Syntaxfehler
4. Aktiviere Debug-Logging für detaillierte Informationen

## 📄 Lizenz

Dieses Plugin wurde von **DerGamer09** entwickelt. Alle Rechte vorbehalten.

---

**Entwickelt mit ❤️ für die Minecraft-Community**