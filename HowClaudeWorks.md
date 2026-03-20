# Claude Code & Agent SDK: Comprehensive Deep Internals Guide
## Overview
This comprehensive guide covers Claude Code's internal architecture, the agentic loop, tool system, context management, memory system, and the Agent SDK. All information is sourced from official Anthropic documentation.
---
## 1. THE AGENTIC LOOP: Core Architecture
### How It Works
Claude Code operates on a **three-phase agentic loop**: **gather context → take action → verify results**. This loop repeats until the task is complete, with Claude making decisions at each step based on what it learned from the previous step.
**Loop Flow:**
1. **User Input**: Prompt received from terminal/IDE/API
2. **Claude Evaluation**: Model processes prompt, system prompt, tool definitions, and conversation history
3. **Tool Decisions**: Claude decides which tools to call (or responds with text-only)
4. **Tool Execution**: Built-in SDK executes requested tools concurrently (read-only) or sequentially (write operations)
5. **Result Feedback**: Tool outputs fed back to Claude as next message context
6. **Decision Point**: Claude either calls more tools or produces final text response
7. **Loop End**: When Claude response contains no tool calls, loop terminates
**Key Characteristic**: Turns are one complete round trip (Claude output → tool execution → feedback to Claude). A single user request can spawn dozens of turns, chaining tool calls based on learned context. The loop adapts: a code question might use only 1 turn (context gathering), while a bug fix might use 15+ turns (explore → analyze → fix → test → iterate).
### The Two Core Components
**Models**: Claude 3 family (Haiku, Sonnet, Opus) provides the reasoning. Different models have different tradeoffs:
- **Haiku**: Fast, low-latency, low-cost (for Explore subagent, simple tasks)
- **Sonnet**: Balanced reasoning and speed (default for most tasks)
- **Opus**: Strongest reasoning (complex architectural decisions)
**Tools**: Enable agency. Without tools, Claude only produces text. With tools, it acts. Tools are the bridge between reasoning and execution.
---
## 2. TOOL SYSTEM: Definition, Dispatch, Execution
### Tool Categories (Built-in)
Claude Code provides **five categories** of tools:
**File Operations**
- `Read`: Read any file in working directory
- `Write`: Create new files
- `Edit`: Make precise edits to existing files using diffs (atomic per-file)
**Search**
- `Glob`: Find files by pattern (`**/*.js`, `src/**/*.ts`)
- `Grep`: Search file contents with regex
**Execution**
- `Bash`: Run shell commands, start servers, run git, execute scripts
- Returns stdout/stderr, exit code
**Web**
- `WebSearch`: Search the web for current information
- `WebFetch`: Fetch and parse web page content
**Orchestration**
- `Agent`: Spawn subagents (specialized child agents)
- `Skill`: Invoke project/plugin skills
- `AskUserQuestion`: Ask user clarifying questions with multiple choice
- `TodoWrite`: Create task list items
- `ToolSearch`: Dynamically discover and load tools on-demand (MCP tool search)
**Code Intelligence** (requires plugins)
- Type errors, warnings, jump-to-definition, find references
### Tool Dispatch Mechanism
**Tool Selection**: Claude decides which tools to call based on:
- Current prompt/task
- Conversation history
- What it learned from previous tool results
- System prompt guidance
**Tool Invocation Format**: Claude returns structured JSON in its response:
```json
{
  "type": "tool_use",
  "id": "tool_use_123",
  "name": "Read",
  "input": {
    "file_path": "/path/to/file.py"
  }
}
```
**Parallel Execution**:
- Read-only tools (Read, Glob, Grep, MCP tools marked read-only) run concurrently
- Write/state-modifying tools (Edit, Write, Bash) run sequentially to prevent conflicts
- Custom tools default to sequential; can be marked `readOnly` to enable parallel
### Permission Model: Three-Tier System
Claude Code uses a **three-tier permission system**:
| Tool Type | Approval Required | "Don't Ask Again" Behavior |
|-----------|------------------|---------------------------|
| **Read-only** | No | N/A |
| **Bash commands** | Yes | Permanently per project + command |
| **File modification** | Yes | Until session end |
**Permission Rule Syntax**:
- **Match all uses**: `Bash`, `Read`, `WebFetch`
- **Fine-grained control**: `Bash(npm run build)`, `Read(./.env)`, `WebFetch(domain:example.com)`
- **Wildcard patterns**: `Bash(npm run *)`, `Bash(git * main)`, `Bash(* --version)`
  - Space before `*` enforces word boundary: `Bash(ls *)` matches `ls -la` but not `lsof`
  - No space: `Bash(ls*)` matches both
**Evaluation Order** (deny → ask → allow):
1. Deny rules always win (prevent bypass)
2. Ask rules trigger prompts
3. Allow rules auto-approve
4. Unmatched tools follow current permission mode
**Permission Modes**:
| Mode | Behavior |
|------|----------|
| `default` | Prompts for permission on first use of each tool |
| `acceptEdits` | Auto-accepts file edits, still asks for Bash |
| `plan` | Read-only only (no file edits, no Bash) |
| `dontAsk` | Auto-denies unless pre-approved via allow rules |
| `bypassPermissions` | Skips prompts (except `.git`, `.claude`, `.vscode` to prevent corruption) |
**Three-Tier Scope System**:
1. **Managed settings** (can't be overridden)
2. **CLI flags** (temporary)
3. **Project settings** `.claude/settings.json`
4. **Local settings** `.claude/settings.local.json`
5. **User settings** `~/.claude/settings.json`
---
## 3. CONTEXT WINDOW MANAGEMENT
### What Consumes Context
Each request to the model includes:
| Component | Load Timing | Impact | Cached? |
|-----------|-------------|--------|---------|
| System prompt | Every request | Small, fixed | Yes (prompt cache) |
| CLAUDE.md files | Session start | Full content in every request | Yes (prompt cache) |
| Tool definitions | Every request | Each tool adds schema; MCP tools especially expensive | Yes (partial via prompt cache) |
| Conversation history | Accumulates over turns | Grows with each turn (prompts, responses, tool inputs/outputs) | No |
| Skill descriptions | Session start | Short summaries only; full content loads on invocation | Yes (prompt cache) |
| MCP server schemas | Every request | All tools from all servers preloaded unless using ToolSearch | Yes (prompt cache) |
**Prompt Caching** dramatically reduces cost: identical prefixes (system prompt, CLAUDE.md, tool definitions) are cached and reused across requests. Only the first request pays full cost; subsequent requests pay ~10% of cached tokens.
### Context Filling and Automatic Compaction
**When It Fills Up**:
- Claude Code monitors context usage in real time
- Auto-compaction triggers at approximately 95% capacity (configurable with `CLAUDE_AUTOCOMPACT_PCT_OVERRIDE`)
- Before compaction, older tool outputs are cleared first
- If still insufficient, conversation gets summarized
**Compaction Process**:
1. Older messages summarized using Claude (preserves key decisions, file paths, test results)
2. Summary replaces original messages in context
3. Full original messages preserved in transcript for reference
4. Detailed instructions from early conversation may be lost
5. `SystemMessage` with subtype `"compact_boundary"` emitted in stream
**Compact Instructions**: Add a "Compact Instructions" section to CLAUDE.md to tell the compactor what to preserve:
```markdown
# Compact Instructions
When summarizing, always preserve:
- Current task objective and acceptance criteria
- File paths read or modified
- Test results and error messages
```
### Managing Context Proactively
**Strategies**:
1. **Use CLAUDE.md for persistent rules** (survives compaction, fully re-injected after each compaction)
2. **Use subagents for high-output tasks** (exploration results stay isolated, only summaries return)
3. **Be selective with tools** (restrict subagent tool access to minimum needed)
4. **Use MCP tool search** (load tools on-demand instead of preloading all)
5. **Use skills for reusable workflows** (load only when invoked)
6. **Manage MCP servers** (each server's tools add to every request)
7. **Lower effort for simple tasks** (`effort: "low"` for file lookups reduces tokens)
**Commands**:
- `/context` - See what's using space
- `/compact` - Manually trigger compaction
- `/mcp` - Check per-server context costs
---
## 4. MESSAGE FLOW AND CONVERSATION LIFECYCLE
### Message Types in SDK
The SDK yields messages representing the state of the loop:
**SystemMessage**: Session lifecycle events
- `subtype: "init"` - First message with session metadata, session ID, model
- `subtype: "compact_boundary"` - Emitted when compaction occurs
**AssistantMessage**: Claude's response after each turn
- Contains text content blocks and/or tool call blocks
- Emitted after Claude responds, including the final text-only response
- Yields `tool_use` blocks with tool name, input, and ID
**UserMessage**: Tool results fed back to Claude
- Contains one or more tool result blocks
- Each block has the tool use ID and result content (success or error)
- Also emitted for any user inputs streamed mid-loop
**StreamEvent**: Real-time streaming (partial messages enabled only)
- Raw API streaming events (text deltas, tool input chunks)
- Allows building real-time progress UI
**ResultMessage**: Final message, always emitted
- `subtype: "success"` - Task completed; `result` field has final text
- `subtype: "error_max_turns"` - Hit turn limit
- `subtype: "error_max_budget_usd"` - Hit budget limit
- `subtype: "error_during_execution"` - API error or cancelled
- Includes: total cost, token usage, turn count, session ID, stop reason
### Session Continuity
**Session ID**: Uniquely identifies a conversation. Available in:
- `ResultMessage.session_id`
- `SystemMessage` with subtype "init"
**Persistence**: Full context from previous turns restored when resuming:
- Files read, analysis performed, actions taken all remembered
- Conversation history fully intact
- Permissions reset (session-scoped permissions not inherited)
**Resume/Continue**: Use same session ID to continue with full context
**Fork**: `--fork-session` flag creates new session ID with prior conversation history preserved, enabling divergent paths without affecting original
---
## 5. HOOKS SYSTEM: Lifecycle Automation
### What Hooks Do
Hooks are shell commands or HTTP requests that execute at specific lifecycle points. They provide **deterministic control** over Claude Code's behavior, automating tasks that always need to happen (not relying on Claude to choose).
### Hook Events and Lifecycle
| Event | When it fires | Use cases |
|-------|---------------|-----------|
| `SessionStart` | Session begins or resumes | Inject context, setup environment |
| `InstructionsLoaded` | CLAUDE.md/rules loaded | Debug instruction loading, re-inject context after compaction |
| `UserPromptSubmit` | User submits prompt (before processing) | Add context before Claude sees it |
| `PreToolUse` | Before tool execution (can block) | Validate commands, enforce restrictions |
| `PermissionRequest` | Permission dialog about to appear | Auto-approve/deny, change mode |
| `PostToolUse` | After tool succeeds | Format code, log audit trail, trigger side effects |
| `PostToolUseFailure` | After tool fails | Handle errors, retry logic |
| `Notification` | Claude needs attention | Desktop notification when waiting for input |
| `SubagentStart` | Subagent spawned | Setup isolated context, logging |
| `SubagentStop` | Subagent completes | Cleanup, aggregate results |
| `Stop` | Claude finishes responding | Validate result, save state |
| `StopFailure` | Turn ends due to API error | Logging (can't change behavior) |
| `TeammateIdle` | Agent team teammate idle (agent teams only) | Assign next task |
| `TaskCompleted` | Task marked complete | Logging, notifications |
| `ConfigChange` | Config file changes during session | Audit changes, block unauthorized mods |
| `WorktreeCreate` | Worktree being created | Custom worktree setup (replaces git behavior) |
| `WorktreeRemove` | Worktree being removed | Cleanup worktrees |
| `PreCompact` | Before compaction | Archive transcript before summarizing |
| `PostCompact` | After compaction | Re-inject critical context |
| `Elicitation` | MCP server requests user input | Handle elicitation during tool calls |
| `ElicitationResult` | User responds to elicitation | Transform response before sending back |
| `SessionEnd` | Session terminates | Cleanup, final logging |
### Hook Implementation Types
**Command hooks** (`type: "command"`):
- Shell command that executes with event data on stdin
- Exit codes control behavior:
  - `0` - Allow action, inject stdout into context (if supported by event)
  - `2` - Block action, stderr becomes error feedback to Claude
  - Other - Allow action, stderr logged only
- Supports `jq` for JSON parsing
**HTTP hooks** (`type: "http"`):
- POST event data to HTTP endpoint
- Endpoint returns JSON response with same format as command output
- Useful for shared audit services, external validators
**Prompt-based hooks** (`type: "prompt"`):
- Single LLM call to Haiku (configurable) to make yes/no decision
- JSON response: `{"ok": true}` or `{"ok": false, "reason": "..."}`
- Use when decision requires only hook input data, no file access
**Agent-based hooks** (`type: "agent"`):
- Spawns a subagent with tool access to verify conditions
- Same `{"ok": true/false}` response format
- 60-second timeout, up to 50 tool-use turns
- Use when need to inspect actual codebase state
### Hook Input/Output
**Input Format** (JSON on stdin):
```json
{
  "session_id": "abc123",
  "cwd": "/path/to/project",
  "hook_event_name": "PreToolUse",
  "tool_name": "Bash",
  "tool_input": { "command": "npm test" }
}
```
**Output Formats**:
1. **Exit code only** (command hooks):
   - Exit 0 = allow
   - Exit 2 = block (include stderr message)
2. **JSON structured output** (command hooks, HTTP):
```json
{
  "hookSpecificOutput": {
    "hookEventName": "PreToolUse",
    "permissionDecision": "allow|deny|ask",
    "permissionDecisionReason": "explanation"
  }
}
```
3. **For PermissionRequest events** (auto-approval):
```json
{
  "hookSpecificOutput": {
    "hookEventName": "PermissionRequest",
    "decision": {
      "behavior": "allow",
      "updatedPermissions": [
        { "type": "setMode", "mode": "acceptEdits", "destination": "session" }
      ]
    }
  }
}
```
### Hook Matchers
Hooks can be filtered with regex matchers on event-specific fields:
| Event | Matcher filters on | Examples |
|-------|-------------------|----------|
| PreToolUse, PostToolUse, etc. | Tool name | `"Bash"`, `"Edit\|Write"`, `"mcp__.*"` |
| SessionStart | How session started | `"startup"`, `"resume"`, `"compact"` |
| SubagentStart | Agent type | `"Explore"`, `"code-reviewer"` |
| ConfigChange | Config source | `"user_settings"`, `"skills"` |
### Hook Locations and Scope
| Location | Scope | Shareable? |
|----------|-------|-----------|
| `~/.claude/settings.json` | All projects | No (local to machine) |
| `.claude/settings.json` | Single project | Yes (version control) |
| `.claude/settings.local.json` | Single project | No (gitignored) |
| Managed policy | Organization | Yes (admin-controlled) |
| Plugin manifest | Plugin-scoped | Yes (bundled) |
| Skill/agent frontmatter | While active | Yes (defined inline) |
### Common Hook Patterns
1. **Notifications** - `osascript`/`notify-send` when Claude waiting for input
2. **Auto-format** - Run Prettier/linter after file edits (PostToolUse matcher)
3. **Protect files** - Block edits to `.env`, `package-lock.json` (PreToolUse, exit 2)
4. **Re-inject context** - SessionStart with "compact" matcher echoes reminders
5. **Audit logging** - Log all tool uses to file (PostToolUse)
6. **Validate commands** - PreToolUse hook script checks Bash commands for dangerous patterns
7. **Auto-approve ExitPlanMode** - Skip plan mode approval dialog
---
## 6. MEMORY SYSTEM: Persistence Across Sessions
### Two Memory Mechanisms
Claude Code has two complementary systems:
| Aspect | CLAUDE.md | Auto Memory |
|--------|-----------|------------|
| **Writer** | You | Claude |
| **Content** | Instructions, rules | Learnings, patterns |
| **Scope** | Project/user/org | Per worktree (git-aware) |
| **Load timing** | Every session | Every session (first 200 lines) |
| **Use for** | Coding standards, architecture | Build commands, debugging insights |
### CLAUDE.md Files
**Locations and Precedence**:
1. **Managed policy** - Organization-wide, cannot be excluded
   - macOS: `/Library/Application Support/ClaudeCode/CLAUDE.md`
   - Linux/WSL: `/etc/claude-code/CLAUDE.md`
   - Windows: `C:\Program Files\ClaudeCode\CLAUDE.md`
2. **Project** - Version controlled
   - `./CLAUDE.md` or `./.claude/CLAUDE.md`
   - Loaded in full
   - Applies to all collaborators
3. **User** - Personal preferences
   - `~/.claude/CLAUDE.md`
   - Applies to all projects on machine
4. **Subdirectory rules** - Path-specific (loaded on demand)
   - `.claude/rules/*.md`
   - Lazy-loaded when Claude reads files matching `paths` frontmatter
   - Saves context by not preloading
**How They Load**:
- Claude walks up directory tree from cwd, loading CLAUDE.md from each ancestor
- Discovers CLAUDE.md in subdirectories (lazy-loaded when needed)
- Higher-precedence locations override lower ones
- All loaded files merged into session context at start
**CLAUDE.md Best Practices**:
1. **Size**: Target under 200 lines per file (consumes context)
2. **Structure**: Use markdown headers and bullets for scannability
3. **Specificity**: "Use 2-space indentation" beats "Format code properly"
4. **Imports**: Use `@path` syntax to include external files without duplication
   ```markdown
   See @README.md for overview.
   Use conventions from @docs/standards.md
   ```
5. **Path-specific rules**: Use `.claude/rules/` with frontmatter:
   ```markdown
   ---
   paths:
     - "src/api/**/*.ts"
     - "src/**/*.tsx"
   ---
   # API Development Rules
   ```
### Auto Memory
**How It Works**:
- Claude automatically saves learnings as it discovers patterns
- Stored in `~/.claude/projects/<project-hash>/memory/`
- Shared across all worktrees in same git repo
- Only first 200 lines of `MEMORY.md` loaded at session start
- Topic files (e.g., `debugging.md`) loaded on-demand when needed
**Storage Structure**:
```
~/.claude/projects/<project>/memory/
├── MEMORY.md          # Index, first 200 lines loaded at start
├── debugging.md       # Topic files (loaded on demand)
├── patterns.md
└── api-conventions.md
```
**Configuration**:
- Enable/disable with `autoMemoryEnabled` setting
- Custom directory: `autoMemoryDirectory` in settings
- Environment variable: `CLAUDE_CODE_DISABLE_AUTO_MEMORY=1`
**What Gets Saved**:
- Build commands discovered
- Debugging insights and patterns
- Architecture notes
- Code style preferences
- Workflow habits Claude identifies
---
## 7. SUBAGENTS: Specialized Child Agents
### Architecture
Subagents are specialized AI assistants that run in **isolated context windows** with:
- Custom system prompts
- Restricted tool access
- Independent permissions
- Own conversation history (not visible to parent)
- Optional persistent memory
**When Claude delegates**:
1. New context window created
2. Subagent's system prompt injected (replaces default)
3. Subagent works autonomously
4. Only summary returned to parent
5. Parent's context grows by summary, not full subagent transcript
### Built-in Subagents
**Explore** (Haiku, read-only):
- Fast, low-latency codebase research
- Denied: Write, Edit tools
- Uses three thoroughness levels: quick, medium, very thorough
**Plan** (inherits model, read-only):
- Used in plan mode for research before presenting plan
- Prevents infinite nesting (subagents can't spawn subagents)
**General-purpose** (inherits model, all tools):
- Complex multi-step tasks requiring both exploration and action
- Intelligent delegation for heavy lifting
**Bash** (inherits model):
- Run terminal commands in separate context
**statusline-setup** (Sonnet):
- Configure status line (special purpose)
**Claude Code Guide** (Haiku):
- Answer questions about Claude Code features
### Subagent Configuration
**Definition Format** (YAML frontmatter + markdown body):
```markdown
---
name: code-reviewer
description: Expert code reviewer for quality and security
model: sonnet
tools: Read, Grep, Glob, Bash
disallowedTools: Edit, Write
permissionMode: default
maxTurns: 20
memory: project
effort: high
background: false
isolation: worktree
skills: [api-conventions, error-handling]
---
You are a senior code reviewer. Analyze code quality and suggest improvements.
```
**Frontmatter Fields**:
| Field | Purpose | Values |
|-------|---------|--------|
| `name` | Unique identifier | lowercase + hyphens |
| `description` | When Claude should delegate | Clear trigger description |
| `model` | Which model to use | `sonnet`, `opus`, `haiku`, full ID, or `inherit` |
| `tools` | Allowed tools (allowlist) | Comma or array: `Read, Grep, Glob` |
| `disallowedTools` | Tools to deny | Removed from inherited list |
| `permissionMode` | Permission handling | `default`, `acceptEdits`, `dontAsk`, `bypassPermissions`, `plan` |
| `maxTurns` | Turn limit | Integer (no limit if omitted) |
| `memory` | Persistent memory | `user`, `project`, `local`, or omitted |
| `effort` | Reasoning level | `low`, `medium`, `high`, `max` |
| `background` | Always run in background | `true` or `false` |
| `isolation` | Execution isolation | `worktree` (separate git checkout) or omitted |
| `skills` | Inject skills into context | Array of skill names |
| `mcpServers` | MCP servers available | Inline definitions or references |
| `hooks` | Lifecycle hooks | Hook configuration |
### Subagent Scopes
| Location | Scope | Priority |
|----------|-------|----------|
| `--agents` CLI flag | Current session | 1 (highest) |
| `.claude/agents/` | Current project | 2 |
| `~/.claude/agents/` | All projects | 3 |
| Plugin's `agents/` | Where plugin enabled | 4 (lowest) |
### Tool Restrictions
**Allowlist** (tools field):
- Only listed tools available
- All tools inherited if omitted
- Example: `tools: Read, Glob, Grep` (read-only research agent)
**Denylist** (disallowedTools field):
- Remove specific tools from inherited pool
- Example: `disallowedTools: Edit, Write` (read-only agent)
**Agent-specific restrictions**:
- `Agent(worker, researcher)` - Only allow spawning these subagents
- `Agent` without parens - Allow any subagent
- Omit `Agent` entirely - No subagent spawning allowed
### Persistent Memory
**Scope options**:
| Scope | Location | Use when |
|-------|----------|----------|
| `user` | `~/.claude/agent-memory/<name>/` | Knowledge applies across projects |
| `project` | `.claude/agent-memory/<name>/` | Project-specific, shareable via git |
| `local` | `.claude/agent-memory-local/<name>/` | Project-specific, not in version control |
When enabled:
- System prompt includes memory instructions
- First 200 lines of `MEMORY.md` loaded
- Read, Write, Edit tools auto-enabled
- Subagent can manage its own memory files
### Foreground vs Background Execution
**Foreground** (default, blocking):
- Blocks main conversation until complete
- Permission prompts shown to user
- Clarifying questions passed through
**Background** (concurrent):
- Runs while user continues working
- Pre-approves all permissions upfront
- Auto-denies unapproved tools (no prompting mid-task)
- Use Ctrl+B to background a running task
- Disable all with `CLAUDE_CODE_DISABLE_BACKGROUND_TASKS=1`
---
## 8. MCP (Model Context Protocol) INTEGRATION
### What MCP Enables
MCP is a standardized protocol for exposing tools and resources from external services. Claude Code connects to MCP servers to gain access to:
- Databases (PostgreSQL, MySQL, MongoDB)
- APIs (GitHub, Slack, Jira)
- Browsers (Playwright, Puppeteer)
- File systems and cloud storage
- Development tools and services
### MCP Server Types
**Local stdio servers**:
- Run locally as child process
- `type: "stdio"` with command and args
- Example: Playwright, file system access
**Remote HTTP servers**:
- Server running elsewhere
- `type: "http"` with URL
- Example: Cloud-hosted services
**Remote SSE (Server-Sent Events)**:
- Server sends events to client
- `type: "sse"` with URL
- Persistent connection
**WebSocket servers**:
- Bidirectional communication
- `type: "ws"` with URL
### Configuration (`.mcp.json`)
```json
{
  "mcpServers": {
    "postgres": {
      "type": "stdio",
      "command": "node",
      "args": ["postgres-mcp-server.js"]
    },
    "github": {
      "type": "http",
      "url": "https://mcp-github-server.example.com"
    },
    "playwright": {
      "type": "stdio",
      "command": "npx",
      "args": ["@playwright/mcp@latest"]
    }
  }
}
```
### Scopes
| Scope | Location | Who sees |
|-------|----------|----------|
| **Local** | `~/.mcp.json` | Only you, all projects |
| **Project** | `.mcp.json` | Team members (version control) |
| **Subagent** | Frontmatter or inline | Only that subagent |
### Tool Context Cost
Each MCP server adds all its tool schemas to every API request (before work starts). With many tools or many servers:
- Context consumed before agent does any work
- Can significantly reduce usable context for actual task
**Optimization**: Use **MCP Tool Search** to load tools on-demand instead of preloading all.
### MCP Tool Search
When `toolSearch` enabled:
- Only a lightweight index of available tools is preloaded
- Tools loaded dynamically when Claude decides to use one
- Dramatic context savings for servers with many tools
- No performance impact (lookup is fast)
Configuration:
```json
{
  "mcpServers": {
    "github": {
      "type": "http",
      "url": "https://...",
      "toolSearch": true
    }
  }
}
```
### Authenticating Remote Servers
**Pre-configured OAuth**:
- Server config includes OAuth metadata
- Claude Code handles flow automatically
- Uses fixed callback port
**Fixed API keys**:
- Pass in environment variables
- Reference with `$ENV_VAR` syntax in config
**Custom headers**:
- Add headers with `allowedEnvVars` filter
- Only listed environment variables interpolated
---
## 9. CONFIGURATION SYSTEM: Settings, CLAUDE.md, Hooks
### Configuration Scopes (Precedence Order)
1. **Managed settings** (cannot be overridden)
   - Server-managed, plist/registry, system-level `managed-settings.json`
   - Enforced by IT/DevOps
2. **CLI flags** (temporary session override)
   - `claude --model opus`, `--allowedTools Read,Edit`, etc.
3. **Local project** (`.claude/settings.local.json`)
   - Gitignored, personal overrides in project
4. **Shared project** (`.claude/settings.json`)
   - Version controlled, team settings
5. **User** (`~/.claude/settings.json`)
   - Your machine, all projects
**Merge behavior**: Arrays merge across layers, later layers override earlier ones. Managed settings always win.
### Settings File Structure
```json
{
  "defaultMode": "default",
  "model": "claude-sonnet-4-6",
  "effort": "high",
  "autoMemoryEnabled": true,
  "permissions": {
    "allow": ["Bash(npm run build)", "Bash(npm test *)"],
    "ask": ["Bash"],
    "deny": ["Bash(rm -rf *)"]
  },
  "hooks": {
    "PostToolUse": [
      {
        "matcher": "Edit|Write",
        "hooks": [{ "type": "command", "command": "prettier --write ..." }]
      }
    ]
  },
  "mcpServers": {
    "github": { "type": "http", "url": "..." }
  },
  "plugins": { "enabledPlugins": ["plugin-name"] },
  "additionalDirectories": ["/path/to/shared"],
  "agent": "code-reviewer"
}
```
### Special Settings
**Managed-only settings** (only effective in managed policy):
- `disableBypassPermissionsMode` - Prevent `bypassPermissions` mode
- `allowManagedPermissionRulesOnly` - Only managed rules apply
- `allowManagedHooksOnly` - Only managed hooks apply
- `allowManagedMcpServersOnly` - Only managed MCP servers
- `blockedMarketplaces` - Marketplace sources to reject
- `strictKnownMarketplaces` - Restrict plugin marketplaces
- `allow_remote_sessions` - Allow Remote Control and web sessions
---
## 10. PLAN MODE: Safe Analysis
**What it does**: Claude can read and analyze code but cannot modify files or execute commands. Useful for planning before implementation.
**How to use**:
- `Shift+Tab` twice to enter plan mode (cycle through modes)
- Or set `defaultMode: "plan"` in settings
**Behavior**:
- Read, Grep, Glob tools available
- Edit, Write, Bash tools denied
- Claude produces a plan for review
- You can approve and exit plan mode, or refine the plan
- When exiting plan mode, returns to previous mode
---
## 11. CLAUDE AGENT SDK: Programmatic Usage
### Overview
The Claude Agent SDK provides programmatic access to the same agentic loop that powers Claude Code. Available as:
- **CLI** (via `-p` flag, formerly "headless mode")
- **Python SDK** (`claude_agent_sdk`)
- **TypeScript SDK** (`@anthropic-ai/claude-agent-sdk`)
### SDK Architecture
**Same Loop, Different Interface**:
- Same tool system, same agentic loop
- Programmatic instead of interactive
- Returns structured messages instead of terminal output
- Hooks run in your application process
**Built-in Tools** (included without implementation):
- Read, Write, Edit (file operations)
- Bash, Glob, Grep (execution/search)
- WebSearch, WebFetch (web)
- AskUserQuestion (user input)
- Agent, Skill, TodoWrite (orchestration)
- ToolSearch (dynamic tool discovery)
### Message Stream (Python/TypeScript)
```python
# Python
async for message in query(prompt="Find the bug", options=...):
    if isinstance(message, SystemMessage):
        print(f"Session ID: {message.session_id}")
    elif isinstance(message, AssistantMessage):
        print(f"Claude responded: {message.content}")
    elif isinstance(message, UserMessage):
        print(f"Tool result: {message.content}")
    elif isinstance(message, ResultMessage):
        if message.subtype == "success":
            print(f"Result: {message.result}")
            print(f"Cost: ${message.total_cost_usd:.4f}")
```
**Message Types**:
- **SystemMessage** - Session init, compaction events
- **AssistantMessage** - Claude's response (text + tool calls)
- **UserMessage** - Tool results
- **StreamEvent** - Real-time streaming (partial messages enabled)
- **ResultMessage** - Final result, cost, session ID
### Custom Tools
Define custom tool implementations:
```python
@tool
def my_custom_tool(param: str) -> str:
    """Detailed description visible to Claude"""
    return f"Result for {param}"
query(
    prompt="...",
    options=ClaudeAgentOptions(
        custom_tools=[my_custom_tool],
        allowed_tools=["my_custom_tool"]
    )
)
```
### Hooks in SDK
Hooks run as Python/TypeScript callbacks in your application:
```python
async def validate_command(input_data, tool_use_id, context):
    command = input_data.get("tool_input", {}).get("command")
    if "rm -rf" in command:
        return {"decision": "deny", "reason": "Dangerous command blocked"}
    return {}
query(
    prompt="...",
    options=ClaudeAgentOptions(
        hooks={
            "PreToolUse": [
                HookMatcher(matcher="Bash", hooks=[validate_command])
            ]
        }
    )
)
```
### Session Management
```python
# First query - capture session ID
session_id = None
async for message in query(prompt="Read auth module"):
    if isinstance(message, SystemMessage) and message.subtype == "init":
        session_id = message.session_id
# Later - resume with full context
async for message in query(
    prompt="Now find all callers",
    options=ClaudeAgentOptions(resume=session_id)
):
    ...
```
### Cost Tracking
```python
if isinstance(message, ResultMessage):
    print(f"Total cost: ${message.total_cost_usd}")
    print(f"Input tokens: {message.usage.input_tokens}")
    print(f"Output tokens: {message.usage.output_tokens}")
```
### CLI Usage (`-p` flag)
```bash
# Simple query
claude -p "What files are in this directory?"
# With tool approval
claude -p "Fix the bug" --allowedTools "Read,Edit,Bash"
# Structured output
claude -p "Extract functions" \
  --output-format json \
  --json-schema '{"type":"object","properties":{"functions":{"type":"array","items":{"type":"string"}}}}'
# Stream responses
claude -p "Write a poem" --output-format stream-json --include-partial-messages | \
  jq -rj 'select(.type == "stream_event" and .event.delta.type? == "text_delta") | .event.delta.text'
# Resume session
session_id=$(claude -p "Start task" --output-format json | jq -r '.session_id')
claude -p "Continue..." --resume "$session_id"
```
---
## 12. STREAMING: Real-time Output
### Streaming in CLI
```bash
# Stream with partial messages
claude "Write a poem" --include-partial-messages
```
### Streaming in SDK
**Python**:
```python
async for message in query(
    prompt="...",
    options=ClaudeAgentOptions(include_partial_messages=True)
):
    if isinstance(message, StreamEvent):
        if message.event.type == "content_block_delta":
            print(message.event.delta.text, end="", flush=True)
```
**TypeScript**:
```typescript
for await (const message of query({
  prompt: "...",
  options: { includePartialMessages: true }
})) {
  if (message.type === "stream_event") {
    if (message.event.type === "content_block_delta") {
      process.stdout.write(message.event.delta.text);
    }
  }
}
```
### Streaming Behavior
- Text deltas received in real time (token-by-token)
- Tool calls streamed as input chunks
- Allows building real-time UIs without waiting for full response
- Partial messages enabled with `--include-partial-messages` or `includePartialMessages: true`
---
## 13. GIT INTEGRATION: Worktrees and Branch Management
### How Claude Code Sees Git
Claude Code automatically detects:
- Current branch
- Uncommitted changes (diffs)
- Recent commit history
- Git status
**Git operations**: Available via Bash tool (`git commit`, `git push`, etc.)
### Parallel Sessions with Worktrees
**Git worktrees**: Create isolated directories for different branches, enabling parallel Claude Code sessions.
```bash
# Create worktree for feature branch
git worktree add ../auth-refactor feature-branch
# In new directory, run parallel session
cd ../auth-refactor
claude  # Independent session, independent context
# Both sessions exist simultaneously
```
**Subagent worktrees**: Subagents can run in isolated worktrees:
```yaml
---
name: parallel-researcher
isolation: worktree
---
```
When subagent finishes, worktree auto-cleaned up.
**Cleanup**:
```bash
git worktree remove ../auth-refactor
```
---
## 14. COST TRACKING AND TOKEN MANAGEMENT
### Viewing Costs
```bash
# In interactive session
/cost  # Shows session cost breakdown
# Cumulative tracking
/cost --all  # All sessions in project
```
### SDK Cost Tracking
```python
if isinstance(message, ResultMessage):
    print(f"Total cost: ${message.total_cost_usd:.4f}")
    print(f"Usage: {message.usage}")  # input_tokens, output_tokens, cache_read, cache_creation
```
### Budget Limits
```python
# Stop if spending exceeds $5
options=ClaudeAgentOptions(max_budget_usd=5.00)
# Stop if more than 30 turns
options=ClaudeAgentOptions(max_turns=30)
```
### Cost Reduction Strategies
1. **Use lower effort** (`effort: "low"` for simple tasks)
2. **Manage context proactively** (use subagents for high-output tasks)
3. **Be selective with tools** (restrict subagent tools)
4. **Use MCP tool search** (load tools on-demand)
5. **Install code intelligence plugins** (reduced context for typed languages)
6. **Move instructions to CLAUDE.md** (survives compaction, prompt-cached)
7. **Use fast mode** (lower cost for less complex tasks)
8. **Delegate to subagents** (isolation keeps main context clean)
9. **Write specific prompts** (less exploration needed)
### Prompt Caching
Claude Code automatically caches:
- System prompt (reused across turns)
- CLAUDE.md (full content, reused across turns)
- Tool definitions (reused across turns)
**Cost**: Cached tokens cost ~10% of normal tokens for input. Saves significant cost on longer sessions.
---
## 15. EXECUTION ENVIRONMENTS
### Local Execution
- Code runs on your machine
- Full access to files and tools
- Default for CLI
### Cloud Execution
- Code runs on Anthropic-managed VMs
- Access to repos you don't have locally
- Set up via desktop app or web interface
- Network restricted to whitelisted domains
- Default allowed domains include GitHub, package managers, cloud platforms
### Remote Control
- Browser interface (claude.ai/code)
- Code executes locally on your machine
- Secure connection from browser to local service
- Useful for working from different devices
---
## 16. DEPLOYMENT AND INTEGRATION
### GitHub Actions
Claude Code can run in GitHub workflows:
```yaml
name: Claude Code Analysis
on: [pull_request]
jobs:
  claude:
    runs-on: ubuntu-latest
    steps:
      - uses: anthropics/claude-code-action@v1
        with:
          prompt: "Review this PR for security issues"
          api_key: ${{ secrets.ANTHROPIC_API_KEY }}
```
### GitLab CI/CD
Similar integration for GitLab pipelines.
### Third-party API Providers
Claude Code supports routing through:
- **Amazon Bedrock** (AWS)
- **Google Vertex AI** (Google Cloud)
- **Microsoft Foundry** (Azure)
- **Custom LLM gateways** (LiteLLM, etc.)
---
## 17. SECURITY AND ISOLATION
### Sandboxing
Optional OS-level enforcement (Linux/macOS) restricts Bash subprocess:
- Filesystem isolation (read-only except whitelisted paths)
- Network isolation (only whitelisted domains)
- Transparent to application code
### Permission Layers
1. **Tool selection** - Claude decides what to call
2. **Permission rules** - Allow/ask/deny specific tools
3. **Sandbox** - OS-level restrictions on Bash subprocess
4. **Hooks** - Custom validation logic
### Prompt Injection Protection
- File content is not treated as instructions (isolated from system prompt)
- Tool results are labeled as data, not instructions
- CLAUDE.md is isolated (not editable by Claude)
- Hooks validate inputs before execution
---
## SUMMARY TABLE: All Core Components
| Component | Purpose | Access | Config Location |
|-----------|---------|--------|-----------------|
| **Agentic Loop** | Main reasoning → tool execution cycle | Built-in | N/A |
| **Tools** | Agency (read files, run commands, etc.) | Auto | Permission rules |
| **Permissions** | Control tool access | Three-tier system | settings.json |
| **Hooks** | Lifecycle automation | Shell/HTTP/prompt/agent | settings.json |
| **CLAUDE.md** | Persistent instructions | Every session | ./ or ~/.claude/ |
| **Auto Memory** | Claude's learnings | Every session (200 lines) | ~/.claude/projects/<hash>/memory/ |
| **Subagents** | Specialized isolated agents | Delegated | .claude/agents/ |
| **MCP** | External tool integration | Scoped | .mcp.json |
| **Settings** | Configuration | Merged precedence | Multiple locations |
| **Sessions** | Conversation persistence | Resume/fork | Disk storage |
| **Context Window** | Available information | Managed + compaction | N/A |
| **Checkpoints** | Rewind capability | File edits only | Automatic |
| **Agent SDK** | Programmatic interface | Python/TS/CLI | Code/flags |
---


1. The Computational Model
At its core, Claude Code is a fixed-point computation over a tool-augmented LLM. The system repeatedly applies a function until it converges (i.e., produces no more tool calls):
state₀ = (system_prompt, user_message, tool_definitions)
stateₙ₊₁ = f(stateₙ) = LLM(stateₙ) → tool_calls → execute(tool_calls) → append_results
terminate when: tool_calls(LLM(stateₙ)) = ∅


This is the agentic loop — a three-phase cycle: gather context → take action → verify results. Without tools, Claude is a pure function from text to text. Tools give it agency — the ability to observe and modify the environment.
The loop is essentially a REPL with an LLM as the evaluator: read user intent, evaluate via reasoning + tool execution, print results, loop.

2. The Agentic Loop in Detail
Loop Flow
User Input
    ↓
┌─────────────────────────────┐
│  Claude API Call             │ ← system prompt + conversation history + tool schemas
│  (reasoning + tool selection)│
└─────────┬───────────────────┘
          ↓
    ┌─ Text only? ──→ Return to user (loop terminates)
    │
    └─ Tool calls present?
          ↓
    ┌─────────────────────────┐
    │  Permission Check        │ ← 3-tier: deny → ask → allow
    │  (hooks fire here too)   │
    └─────────┬───────────────┘
          ↓
    ┌─────────────────────────┐
    │  Tool Execution          │ ← read-only tools run in parallel
    │  (sandbox enforced)      │    write tools run sequentially
    └─────────┬───────────────┘
          ↓
    ┌─────────────────────────┐
    │  Append results to       │
    │  conversation history    │
    └─────────┬───────────────┘
          ↓
    (loop back to Claude API call)


A single user request can spawn dozens of turns. A simple question might be 1 turn; a complex bug fix might be 15+ turns of explore → analyze → fix → test → iterate.
Turn Structure
Each "turn" is one complete round trip:
Claude produces output (text blocks + tool_use blocks)
Tool_use blocks are dispatched
Tool results are packaged as a UserMessage with tool_result blocks
This becomes the next input to Claude
The model's output is structured JSON:
{
  "content": [
    {"type": "text", "text": "Let me check that file."},
    {"type": "tool_use", "id": "toolu_01X", "name": "Read", "input": {"file_path": "/foo/bar.py"}}
  ]
}


Multiple tool_use blocks in one response → parallel or sequential execution depending on tool type.

3. The Tool System
Tool Taxonomy
Tools fall into categories based on their side effects:
Category
Tools
Side Effects
Execution
Read-only
Read, Glob, Grep, WebSearch, WebFetch
None
Parallel
State-modifying
Edit, Write, Bash
File system, processes
Sequential
Orchestration
Agent, Skill, TodoWrite
Spawn subprocesses
Sequential
Interactive
AskUserQuestion
Blocks for user input
Sequential
Dynamic
ToolSearch
Loads tool schemas
Sequential

Tool Dispatch
When Claude returns N tool_use blocks:
Classify each tool as read-only or write
Read-only tools → dispatch concurrently (Promise.all / asyncio.gather)
Write tools → dispatch sequentially (order preserved)
Collect all results → package as single UserMessage
This is a key optimization: reading 5 files simultaneously is ~5x faster than sequentially.
Permission Model: Evaluation as a Priority Queue
Permissions follow a strict evaluation order (short-circuit):
deny rules  →  if match → BLOCK (always wins)
    ↓
ask rules   →  if match → PROMPT user
    ↓
allow rules →  if match → AUTO-APPROVE
    ↓
default     →  follow current permission mode


This is analogous to firewall rules — first match wins, deny takes priority.
Permission rule syntax supports pattern matching:
Bash(npm run *) — glob on command arguments
Bash(git * main) — wildcard in the middle
Space-aware: Bash(ls *) matches ls -la but NOT lsof (space enforces word boundary)
Permission modes control the default behavior:
Mode
Behavior
default
Prompt on first use of each tool
acceptEdits
Auto-accept file edits, still ask for Bash
plan
Read-only only — no file edits, no Bash
dontAsk
Auto-deny unless explicitly in allow list
bypassPermissions
Skip all prompts (except protected dirs: .git, .claude, .vscode)


4. Context Window Management
This is where the CS gets interesting. The context window is a bounded buffer — it has a fixed token capacity (model-dependent). Every API call must fit within it.
What Consumes Context
Every request to Claude includes:
┌─────────────────────────────────────────┐
│ System Prompt (fixed, ~small)            │ ← prompt-cached
│ CLAUDE.md files (all loaded)             │ ← prompt-cached
│ Tool schemas (all registered tools)      │ ← prompt-cached
│ Skill descriptions (summaries only)      │ ← prompt-cached
│ ─────────────────────────────────────── │
│ Conversation history (grows each turn)   │ ← NOT cached
│   - User messages                        │
│   - Assistant messages                   │
│   - Tool inputs and outputs              │
└─────────────────────────────────────────┘


Prompt caching is critical: the prefix (system prompt + CLAUDE.md + tool schemas) is identical across turns. Anthropic's API caches this prefix — subsequent requests pay ~10% of the cached token cost. Only the conversation tail (new messages) costs full price.
Automatic Compaction
When context usage hits ~95% capacity:
Phase 1: Clear older tool outputs (replace with summaries)
Phase 2: If still insufficient, summarize the entire conversation using a Claude call
A SystemMessage with subtype: "compact_boundary" is emitted
CLAUDE.md files are re-injected in full after compaction (they survive)
This is essentially garbage collection for conversation history — old, less-relevant information is compressed to make room for new work. The compaction itself is an LLM call that produces a summary preserving key decisions, file paths, and test results.
You can guide compaction with "Compact Instructions" in CLAUDE.md:
# Compact Instructions
When summarizing, always preserve:
- Current task objective
- File paths modified
- Test results and error messages


The MCP Tool Schema Problem
Each MCP server's tools add their full JSON schemas to every request. With many servers/tools, this can consume significant context before any work begins. The solution is MCP Tool Search — a lazy-loading mechanism where only a lightweight index is preloaded, and full schemas are fetched on-demand when Claude decides to use a tool.

5. The Subagent System
Subagents are the process isolation mechanism. They're fundamentally about context window management and parallelism.
Why Subagents Exist
Problem: A research task that reads 50 files dumps all that content into the main context window, leaving little room for the actual work.
Solution: Spawn a subagent with its own context window. It does the research, and only a summary returns to the parent.
Parent Context Window              Subagent Context Window
┌──────────────────┐              ┌──────────────────────┐
│ System prompt     │              │ Subagent system prompt│
│ Conversation      │   spawn →   │ Task description      │
│ ...               │              │ Tool results (50 files│
│                   │   ← return  │  worth of content)    │
│ + summary (small) │              │ Analysis              │
└──────────────────┘              └──────────────────────┘
                                   (discarded after return)


This is analogous to fork() in Unix — the child has its own address space, does work, and returns a result.
Subagent Types
Type
Model
Tools
Purpose
Explore
Haiku (fast, cheap)
Read-only
Quick codebase research
Plan
Inherited
Read-only
Architecture design
General-purpose
Inherited
All
Complex multi-step tasks
claude-code-guide
Haiku
Read + Web
Answer questions about Claude Code
Custom (.claude/agents/)
Configurable
Configurable
Domain-specific

Worktree Isolation
For subagents that modify files, git worktrees provide filesystem isolation:
# Under the hood:
git worktree add /tmp/worktree-abc123 -b temp-branch
# Subagent works in /tmp/worktree-abc123/
# If no changes made → auto-cleanup
# If changes made → worktree path + branch returned to parent


This prevents subagents from creating merge conflicts with the parent's work.
Foreground vs Background
Foreground (blocking): Parent waits. Permission prompts shown. Clarifying questions forwarded.
Background (concurrent): Parent continues working. All permissions pre-approved upfront. Unapproved tools auto-denied (no blocking prompts mid-task). User notified on completion.

6. The Hooks System
Hooks are deterministic lifecycle callbacks — they fire at specific events regardless of what Claude decides to do. This contrasts with tools, which Claude chooses to use.
The Event Model
Session Start ──→ User Prompt Submit ──→ [Agentic Loop Begins]
                                              │
                                     PreToolUse (can block!)
                                              │
                                     PermissionRequest (can auto-approve)
                                              │
                                     [Tool Executes]
                                              │
                                     PostToolUse / PostToolUseFailure
                                              │
                                     [Loop continues or...]
                                              │
                                         Stop ──→ Session End


Hook Implementation Types
Type
Mechanism
Use Case
command
Shell command, event data on stdin
Auto-formatting, file protection
http
POST to HTTP endpoint
Audit logging, external validation
prompt
Single LLM call (Haiku)
Yes/no decisions from context
agent
Full subagent with tool access
Complex validation requiring codebase inspection

PreToolUse: The Gate
PreToolUse is the most powerful hook — it can block tool execution:
Exit code 0 → allow
Exit code 2 → block (stderr becomes error feedback to Claude)
Other → allow (stderr logged only)
Example: Block dangerous commands:
{
  "hooks": {
    "PreToolUse": [{
      "matcher": "Bash",
      "hooks": [{
        "type": "command",
        "command": "jq -r '.tool_input.command' | grep -q 'rm -rf' && exit 2"
      }]
    }]
  }
}



7. Memory System
Two Persistence Mechanisms
CLAUDE.md (human-written instructions):
Loaded every session, injected into system prompt
Survives compaction (re-injected in full)
Prompt-cached (cheap to include)
Analogous to a .bashrc — always sourced
Auto Memory (Claude-written learnings):
Stored at ~/.claude/projects/<project-hash>/memory/
MEMORY.md is an index file — first 200 lines loaded every session
Individual memory files loaded on-demand
Think of it as a key-value store with MEMORY.md as the index
Memory File Format
---
name: user_role
description: User is a senior backend engineer
type: user
---

Deep expertise in distributed systems and Go.
New to React and frontend development.
Frame frontend explanations in terms of backend analogues.


Configuration Hierarchy
Managed policy  (org-wide, can't override)
    ↓
CLI flags       (temporary session)
    ↓
.claude/settings.local.json  (project, gitignored)
    ↓
.claude/settings.json        (project, version-controlled)
    ↓
~/.claude/settings.json      (user-global)


Arrays merge across layers. Managed settings always win (like !important in CSS, but actually enforced).

8. MCP (Model Context Protocol)
MCP is a standardized protocol for tool integration — think of it as USB for AI tools. Any service that speaks MCP can expose tools to Claude Code.
Transport Types
Type
Mechanism
Typical Use
stdio
Child process, communicate via stdin/stdout
Local tools (Playwright, file systems)
http
HTTP POST requests
Cloud services
sse
Server-Sent Events
Streaming updates
ws
WebSocket
Bidirectional communication

Configuration (.mcp.json)
{
  "mcpServers": {
    "postgres": {
      "type": "stdio",
      "command": "node",
      "args": ["postgres-mcp-server.js"]
    },
    "github": {
      "type": "http",
      "url": "https://mcp-github-server.example.com",
      "toolSearch": true
    }
  }
}


MCP tools appear to Claude identically to built-in tools — the dispatch mechanism is uniform. Tool names get namespaced: mcp__<server>__<tool>.

9. The Agent SDK
The SDK exposes the same agentic loop programmatically (Python and TypeScript):
Message Stream Types
async for message in query(prompt="Fix the bug", options=...):
    match message:
        case SystemMessage(subtype="init"):
            # Session started, session_id available
        case AssistantMessage(content=blocks):
            # Claude's response — text + tool_use blocks
        case UserMessage(content=results):
            # Tool results fed back
        case StreamEvent(event=delta):
            # Real-time token streaming
        case ResultMessage(subtype="success"):
            # Terminal state — cost, tokens, session_id


Session Continuity
Sessions have unique IDs. Resuming a session restores full context:
# First query
result = await query(prompt="Analyze auth module")
session_id = result.session_id

# Later — full context preserved
result = await query(prompt="Now fix the bug you found",
                     options=ClaudeAgentOptions(resume=session_id))


Forking creates a new session from an existing one's history — useful for exploring divergent approaches without affecting the original.
Budget Controls
options = ClaudeAgentOptions(
    max_budget_usd=5.00,   # Hard cost limit
    max_turns=30,           # Turn limit
)


The loop terminates with ResultMessage(subtype="error_max_budget_usd") or "error_max_turns" when limits are hit.

10. Streaming
Responses stream token-by-token from the API. The SDK exposes this via StreamEvent messages:
StreamEvent(content_block_start) → "Let"
StreamEvent(content_block_delta) → " me"
StreamEvent(content_block_delta) → " check"
StreamEvent(content_block_delta) → " that"
StreamEvent(content_block_delta) → " file."
StreamEvent(content_block_stop)


For tool calls, the input JSON is streamed as chunks too — the SDK buffers these and dispatches the tool only when the full input is received.

11. Security Model
Layered Defense
Layer 1: Tool Selection     (Claude decides what to call)
Layer 2: Permission Rules   (allow/ask/deny per tool)
Layer 3: Hooks             (custom validation logic, can block)
Layer 4: OS Sandbox        (filesystem + network isolation for Bash)
Layer 5: Protected Dirs    (.git, .claude, .vscode — always protected)


Prompt Injection Resistance
File content and tool results are labeled as data, not instructions
CLAUDE.md is injected as system prompt (privileged), not as user content
Claude is instructed to flag suspected injection attempts

12. Plan Mode
Plan mode is a constraint on the agentic loop — it restricts the tool set to read-only:
Allowed: Read, Glob, Grep, Agent (Explore/Plan types only)
Denied: Edit, Write, Bash, and all state-modifying tools
Output: A plan file (the only writable file) at a designated path
The plan is written incrementally through phases: exploration → design → review → final plan. This forces deliberation before execution — useful for complex tasks where the cost of a wrong approach is high.

Key Design Principles
Tools are the bridge between reasoning and action — without them, it's just text generation
Context is the scarcest resource — every design decision (subagents, compaction, lazy loading, prompt caching) optimizes for context efficiency
Permissions are defense-in-depth — deny rules, user prompts, hooks, and OS sandbox all layer
The loop is convergent — it terminates when Claude has no more tool calls to make (the fixed-point)
Isolation enables parallelism — subagents with separate context windows can work concurrently without interference
The whole system is essentially a theorem prover with side effects — Claude reasons about what to do, takes actions, observes results, and iterates until the goal state is reached or it determines it can't proceed.

