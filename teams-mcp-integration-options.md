# Microsoft Teams MCP Integration — Two Approaches

There is no Jira-style personal API token for Teams. All programmatic access goes through
Microsoft's OAuth-based identity platform (Entra ID). Below are the two viable architectures
for connecting an MCP server (and therefore Claude / the scrum-agent) to Teams.

---

## Option A — Bot in a Channel (Bot Framework + RSC)

> Reference implementation: [InditexTech/mcp-teams-server](https://github.com/InditexTech/mcp-teams-server) (Python)

### Architecture

```
Claude ──MCP──> mcp-teams-server ──Bot Framework API──> one Teams channel
```

The MCP server authenticates **as an application (bot)** using a client secret —
no user login, fully headless. Messages appear in the channel **as the bot**, not as a person.

### How authentication works

1. **Entra ID app registration** → yields `TEAMS_APP_ID` (client ID) + `TEAMS_APP_PASSWORD` (client secret).
   This is the closest thing to a "generated token": client-credentials flow, no interactive login.
2. **Azure Bot resource** → linked to the app registration, with the Teams channel enabled.
3. **Teams app manifest** → packaged and installed into *one specific team* by a team owner.

### The key trick: RSC (Resource-Specific Consent)

Normally, app-only (no user) access to Teams messages is blocked or requires tenant-wide
admin consent. RSC permissions (`ChannelMessage.Send.Group`, `ChannelMessage.Read.Group`,
`TeamMember.Read.Group`, …) are instead **granted by the team owner at install time and
scoped to that single team**. No org-wide admin consent needed.

### Configuration (env vars)

| Variable | Purpose |
|---|---|
| `TEAMS_APP_ID` | Entra ID application (client) ID |
| `TEAMS_APP_PASSWORD` | Client secret |
| `TEAMS_APP_TYPE` | `SingleTenant` or `MultiTenant` |
| `TEAMS_APP_TENANT_ID` | Tenant ID (single-tenant only) |
| `TEAM_ID` | The one team the bot is installed in |
| `TEAMS_CHANNEL_ID` | The one channel it operates in |

### Capabilities

| Feature | Supported |
|---|---|
| Start threads in the channel (with @mentions) | ✅ |
| Reply to / update threads | ✅ |
| Read channel messages & thread replies | ✅ |
| List channel members | ✅ |
| 1:1 / group chats | ❌ channel-only |
| Unread tracking | ❌ |
| Edit sent messages | ❌ |
| Multiple teams/channels | ❌ one per server instance |

### Pros / Cons

**Pros**
- Fully headless — no user ever has to log in; ideal for automation / CI / scheduled agents
- Clear bot identity (audit-friendly: "ScrumBot said X")
- No tenant-wide admin consent (RSC = team-owner consent only)
- Existing open-source implementation, Docker image available

**Cons**
- Locked to one team + one channel per configuration
- Cannot touch personal chats at all
- Setup involves three moving parts (app registration, Azure Bot, manifest upload)
- Requires "upload custom apps" to be allowed in the tenant's Teams admin policy —
  often disabled in locked-down corporate tenants
- Messages come from the bot, never from a real person

### Best for

Posting standup reminders, sprint summaries, and threaded scrum discussions into a
dedicated team channel — the natural fit for a scrum-agent's *broadcast* duties.

---

## Option B — Personal Graph Agent (Microsoft Graph + Delegated Auth)

### Architecture

```
Claude ──MCP──> custom MCP server ──Microsoft Graph API──> acts AS YOU in Teams
```

The MCP server authenticates **as a signed-in user** (delegated permissions).
Messages are sent from *your* account; reads see *your* chats.

### How authentication works

1. **Entra ID app registration** — public client, delegated permissions
   (`Chat.ReadWrite`, `ChannelMessage.Read.All`, `User.ReadBasic.All`).
2. **Device code flow** on first run: the server prints
   `To sign in, visit https://microsoft.com/devicelogin and enter code XYZ-123`.
   You log in once in a browser.
3. **MSAL token cache** — access tokens (~1 h) are refreshed silently forever after.
   After the first login it *feels* like a static token.

Graph deliberately blocks app-only (bot-style) sending of personal chat messages —
that's why this route requires a real user sign-in.

### Capabilities (Graph endpoints)

| MCP tool | Graph endpoint | Notes |
|---|---|---|
| `send_chat(user, message)` | `POST /chats` → `POST /chats/{id}/messages` | Sent as you |
| `get_chat_messages(user)` | `GET /chats/{id}/messages` | Full 1:1 / group history |
| `get_unread_chats()` | `GET /me/chats?$expand=lastMessagePreview` | Compare `viewpoint.lastMessageReadDateTime` vs `lastMessagePreview.createdDateTime` |
| `get_channel_messages(team, channel)` | `GET /teams/{id}/channels/{id}/messages` | Any team you belong to |
| `find_user(name)` | `GET /users?$filter=…` | For resolving names → chat targets |

**Unread caveat:** Graph has no unread flag.
- *Chats*: derivable from the `viewpoint` timestamps above — works well.
- *Channels*: no viewpoint data at all; the server must persist its own
  "last seen" timestamp per channel and diff against new messages.

### Implementation sketch (fits the existing Spring stack)

- Dependencies: `com.microsoft.graph:microsoft-graph` + `com.azure:azure-identity`
  (`DeviceCodeCredential` handles the flow + caching)
- Expose the tools above from a Spring AI MCP server, mirroring `scrum-mcp-server`

### Pros / Cons

**Pros**
- Full access: 1:1 chats, group chats, any channel in any team you're in
- Unread detection possible for chats
- Simplest Azure setup (one app registration, no Bot resource, no manifest)
- `Chat.ReadWrite` usually needs **no admin consent**

**Cons**
- Requires one interactive login (not fully headless; token cache mitigates)
- Acts as *you* — automated messages look like you typed them (impersonation risk / awkwardness)
- `ChannelMessage.Read.All` delegated typically **does** need admin consent
- No off-the-shelf server for this exact tool set — you build it
- Token cache is a sensitive credential on disk

### Best for

The *assistant* side of a scrum-agent: reading your unread chats, summarizing what you
missed, DMing teammates on your behalf, pulling channel context into Claude.

---

## Side-by-side summary

| | **A: Bot in channel** | **B: Personal Graph agent** |
|---|---|---|
| Identity | Bot ("ScrumBot") | You |
| Login required | Never | Once (device code) |
| 1:1 chats | ❌ | ✅ |
| Channel post/read | ✅ (one channel) | post via chat only*, read ✅ (all your teams) |
| Unread detection | ❌ | ✅ chats / ⚠️ channels (DIY) |
| Admin involvement | Team owner installs app; custom-app upload must be allowed | Usually none for chats; admin consent for channel read |
| Azure resources | App registration + Azure Bot + manifest | App registration only |
| Off-the-shelf | ✅ InditexTech server | ❌ build your own |
| Headless automation | ✅ | ⚠️ (after first login) |

\* Delegated `ChannelMessage.Send` exists too, so posting to channels as yourself is possible —
but for bot-style announcements Option A is cleaner.

## Recommendation

The two options are **complementary, not competing**:

- **Option A** for the scrum-agent's outbound automation (standup threads, sprint reports) —
  headless, clearly bot-branded.
- **Option B** for the interactive assistant experience (catch me up on unread chats,
  DM the team) — personal scope.

If only one is feasible: start with **B** if the goal is a personal assistant,
**A** if the goal is team automation. If neither Azure setup is possible and you only
need to *push* notifications to a channel, a **Power Automate Workflows incoming webhook**
(plain URL, POST JSON, no Entra app at all) is the zero-friction fallback.
