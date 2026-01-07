# NEWM Admin

A Rust desktop GUI application for administering the NEWM Server, built with [GPUI](https://www.gpui.rs/) and [gpui-component](https://github.com/longbridgeapp/gpui-component).

## Features

- Modern, cross-platform desktop UI (Linux, macOS, Windows)
- Login form with username/password authentication
- Async HTTP API calls via Reqwest
- Structured logging via tracing

## Prerequisites

### Linux (Debian/Ubuntu)

GPUI requires X11/Wayland development libraries:

```bash
sudo apt-get install -y \
    libxkbcommon-x11-dev \
    libxcb-render0-dev \
    libxcb-shape0-dev \
    libxcb-xfixes0-dev \
    libwayland-dev \
    libxkbcommon-dev
```

### macOS

No additional dependencies required.

### Windows

No additional dependencies required.

## Building

```bash
# Development build
cargo build

# Release build (optimized)
cargo build --release
```

## Running

```bash
# Run development build
cargo run

# Run with debug logging
RUST_LOG=newm_admin=debug cargo run
```

## Project Structure

```
newm-admin/
├── Cargo.toml           # Dependencies and project metadata
├── README.md            # This file
└── src/
    ├── main.rs          # Entry point, tracing + GPUI initialization
    ├── app.rs           # Main application view wrapper
    └── views/
        ├── mod.rs       # Views module
        └── login.rs     # Login form component
```

## Dependencies

| Crate | Purpose |
|-------|---------|
| `gpui` | GPU-accelerated UI framework (from Zed editor) |
| `gpui-component` | Pre-built UI components |
| `tokio` | Async runtime |
| `reqwest` | HTTP client for API calls |
| `tracing` | Structured logging |
| `anyhow` | Error handling |

## Development

### Adding New Views

1. Create a new file in `src/views/` (e.g., `dashboard.rs`)
2. Export it in `src/views/mod.rs`
3. Create a view struct implementing `gpui::Render`
4. Wire it up in `app.rs`

### Logging

Use the `tracing` macros for structured logging:

```rust
tracing::info!("User logged in: {}", username);
tracing::debug!("API response: {:?}", response);
tracing::error!("Failed to connect: {}", error);
```

## License

See the parent project's LICENSE file.
