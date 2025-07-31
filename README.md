# Changelog Plugin

This plugin provides a simple changelog system with a paged GUI. Entries are stored in a database (SQLite or MySQL).

## Commands
- `/changelog` – open the changelog GUI.
- `/changelog add <text>` – add a new entry. Requires `changelog.admin`.
- `/changelog remove <id>` – remove an entry. Requires `changelog.admin`.
- `/changelog list` – list all entries in chat/console. Requires `changelog.admin`.

The GUI shows up to 36 entries per page with arrows to navigate and a close button.
