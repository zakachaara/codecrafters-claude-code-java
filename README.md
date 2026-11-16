# Claude Code in Java

This project is a Java solution for the
[Build Your Own Claude Code challenge](https://codecrafters.io/challenges/claude-code)
by [CodeCrafters](https://codecrafters.io/).

The challenge builds an AI coding assistant that can understand a request,
call tools, inspect files, make edits, and continue until the task is complete.

## Challenge source

- [Challenge overview](https://codecrafters.io/challenges/claude-code)
- [CodeCrafters challenge repository](https://github.com/codecrafters-io/build-your-own-claude-code)
- [LSP exercise](https://github.com/codecrafters-io/build-your-own-claude-code/tree/main/extension_designs/lsp_exercise)

## Key notes

- Java application using the OpenAI-compatible SDK through OpenRouter.
- The agent keeps a conversation history while it works.
- Tools are registered behind a common registry, so new capabilities can be added without expanding the entry point.
- Built-in tools include `Read`, `Write`, `Bash`, `Skill`, and `LSP`.
- Skills are loaded from `.claude/skills`.
- LSP lookups support definitions, references, synchronized edits, and diagnostics.
- The LSP tool is always advertised; it reports configuration or server errors when unavailable.

## Extension architecture

The core agent does not need to know how a tool works. An extension only needs
to provide a description for the model and an execution path.

```mermaid
flowchart LR
    User[User request] --> Agent[Agent loop]
    Agent --> Registry[Tool registry]
    Registry --> Read[Read]
    Registry --> Write[Write]
    Registry --> Bash[Bash]
    Registry --> Skills[Skills extension]
    Registry --> LSP[LSP extension]
    Read --> Result[Tool result]
    Write --> Result
    Bash --> Result
    Skills --> Result
    LSP --> Result
    Result --> Agent
```

## General overview

The model decides what to do, while extensions provide focused knowledge and
actions. Results travel back through the same conversation.

```mermaid
flowchart LR
    User((User)):::user --> App[Claude Code app]:::core
    App <--> Model[(Language model)]:::model
    App --> Registry[Extension registry]:::core

    Registry --> Skills[Skills]:::skills
    Skills --> SkillFiles[(.claude/skills)]:::data
    Skills --> SubAgent[Optional sub-agent]:::agent
    SubAgent --> Model

    Registry --> LSP[LSP extension]:::lsp
    LSP --> Sync[Document sync]:::sync
    Sync --> Server[(Language server)]:::server
    Server --> LSP
    LSP --> Diagnostics[Diagnostics]:::feedback
    Diagnostics --> Model

    Registry --> Tools[Read / Write / Bash]:::tools
    Tools --> Files[(Project files)]:::data
    Files --> Tools
    Tools --> Model

    classDef user fill:#fef3c7,stroke:#d97706,color:#78350f
    classDef model fill:#dbeafe,stroke:#2563eb,color:#1e3a8a
    classDef core fill:#e0e7ff,stroke:#4f46e5,color:#312e81
    classDef skills fill:#dcfce7,stroke:#16a34a,color:#14532d
    classDef lsp fill:#f3e8ff,stroke:#9333ea,color:#581c87
    classDef sync fill:#fae8ff,stroke:#c026d3,color:#701a75
    classDef server fill:#fce7f3,stroke:#db2777,color:#831843
    classDef feedback fill:#fee2e2,stroke:#dc2626,color:#7f1d1d
    classDef tools fill:#e2e8f0,stroke:#475569,color:#1e293b
    classDef data fill:#f8fafc,stroke:#64748b,color:#334155
    classDef agent fill:#ccfbf1,stroke:#0f766e,color:#134e4a
```

Colors show the role of each part: blue is the model, green is Skills,
purple is LSP, and gray is direct file tooling.

## Skills extension

Skills turn a short command such as `/review` into reusable instructions.
They can receive arguments and optionally run in a separate sub-agent context.

```mermaid
flowchart TD
    Files[".claude/skills/*/SKILL.md"] --> Loader[Skill loader]
    Loader --> Catalog[Skill catalog]
    Catalog --> Prompt[System prompt]
    Prompt --> Model[Model]
    Model -->|Skill tool| Lookup[Find skill + arguments]
    Lookup --> Body[Load instructions]
    Body --> Fork{context: fork?}
    Fork -->|No| Main[Continue in main agent]
    Fork -->|Yes| Sub[Run in sub-agent]
    Main --> Result[Return instructions or result]
    Sub --> Result
    Result --> Model
```

## LSP extension

LSP gives the model semantic knowledge instead of simple text matching.
The shared document layer keeps the server aligned with files changed by
`Write`.

```mermaid
sequenceDiagram
    participant Model
    participant LSPTool
    participant Sync as Document sync
    participant Server as Language server

    Model->>LSPTool: definition / references
    LSPTool->>Sync: Open current file if needed
    Sync->>Server: didOpen(version 1)
    LSPTool->>Server: semantic request
    Server-->>LSPTool: locations
    LSPTool-->>Model: relative paths + positions

    Model->>Write: Save edited file
    Write->>Sync: Synchronize saved text
    Sync->>Server: didChange(next version)
    Server-->>Sync: publishDiagnostics
    Sync-->>Model: New diagnostics on next turn
```

## Running

The challenge runner invokes the program with:

```bash
./your_program.sh -p "Your request"
```

Required environment variable:

```text
OPENROUTER_API_KEY
```

Optional variables:

```text
OPENROUTER_BASE_URL
CODECRAFTERS_LSP_URL
```

## Visual references

- [Language Server Protocol overview](https://microsoft.github.io/language-server-protocol/overviews/lsp/overview/) shows how editors, clients, and language servers cooperate.
- [LSP exercise](https://github.com/codecrafters-io/build-your-own-claude-code/tree/main/extension_designs/lsp_exercise) provides a concrete project for definitions, references, and diagnostics.
- [Claude Code skills](https://docs.anthropic.com/en/docs/claude-code/skills) explains how reusable instruction extensions work.
