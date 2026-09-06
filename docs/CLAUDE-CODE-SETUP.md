# Setting Up Claude Code Locally (Windows + IntelliJ)

One-time environment setup to get Claude Code running inside IntelliJ for
this project. Written up after doing it once and hitting several real
issues along the way — follow this to skip them.

## Prerequisites

### 1. Node.js — must be v22+ (Claude Code's hard requirement)
Check what you have:
```powershell
node -v
npm -v
```
If Node is missing or older than v22, install the current **Active LTS**
release (not the newest "Current" release, which isn't yet LTS):
```powershell
winget install OpenJS.NodeJS.LTS
```
Close and reopen PowerShell afterward — PATH changes don't apply to an
already-open terminal.

**Known issue — stale npm after upgrading an old Node install:**
If `npm -v` still reports an old version after installing a new Node (e.g.,
still `8.2.0` after installing Node 24), the npm *shim* (`npm.cmd`) can be
left over from the old install if it was locked during the upgrade, even
though the correct npm files exist on disk. Confirm this by checking:
```powershell
Get-Content "C:\Program Files\nodejs\node_modules\npm\package.json" | Select-String '"version"'
```
If that shows a newer version than `npm -v` reports, force npm to
reinstall itself over its own broken shim:
```powershell
& "C:\Program Files\nodejs\node.exe" "C:\Program Files\nodejs\node_modules\npm\bin\npm-cli.js" install -g npm@<version-from-above>
```
(Note the `&` call operator — required in PowerShell whenever running a
quoted executable path.) Reopen PowerShell and re-check `npm -v`.

### 2. Install the Claude Code CLI
```powershell
npm install -g @anthropic-ai/claude-code
```
Verify:
```powershell
claude --version
```

## IntelliJ plugin

1. `File → Settings → Plugins → Marketplace`
2. Search **"Claude Code"**
3. **Install the one published by "Anthropic PBC"** — the marketplace has
   several similarly-named third-party plugins (CC GUI, Claude Code with
   GUI/Swttch, Claude Code Chat, etc.). Only the Anthropic PBC one is
   official; the others are independent projects of unknown reliability.
4. Restart IntelliJ.

## Connecting the CLI to IntelliJ

1. Open IntelliJ's **integrated terminal** (not a separate PowerShell
   window), in the project's root directory — Claude Code needs to be
   started from the same directory as the IDE project root to see the same
   files IntelliJ sees.
2. Run:
```powershell
claude
```
3. On first run, a banner offers **"Set up IntelliJ IDEA MCP"** — take it
   rather than doing this manually via `/ide` later; it's the same
   connection, offered earlier.
4. Choose **global** scope, not project scope, when prompted. This
   connection is IDE-tooling wiring (how the plugin and CLI talk to each
   other) — the same in every project, no per-project decision being made —
   so it belongs at the user level, not committed into any one repo.
5. Complete login: choose **"Claude account with subscription"** if you're
   using the same account as claude.ai (Pro/Max/Team/Enterprise) — this
   folds Claude Code usage into that subscription, no separate billing.
6. Verify the connection:
```powerhsell
/mcp
```
Look for the `idea` server showing `✓ connected`, along with a tool
count. (A separate `claude.ai Notion` entry may show "needs
authentication" — unrelated to this setup, safe to ignore.)

## Verifying it actually works

Ask it something that requires reading real project files, not something
guessable from general knowledge:

```text
What Maven modules exist in this project?
```
A correct, specific answer (naming your actual modules) confirms it's
reading the live project through the IDE connection, not just generating
plausible text.