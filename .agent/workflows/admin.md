---
description: NEWM Admin desktop application development (Rust + GPUI)
---

# NEWM Admin Workflow

Development workflow for the Rust-based NEWM Admin desktop application using the GPUI framework.

## Overview

The admin app uses:
- **Rust** — Primary language
- **GPUI** — GPU-accelerated UI framework (from Zed editor)
- **gpui-component** — Pre-built UI components
- **reqwest** — HTTP client (async with async-compat)
- **Cargo** — Build system

---

## Project Location

```
newm-server/newm-admin/
├── src/
│   ├── main.rs          # Entry point, window setup
│   ├── app.rs           # Main application component
│   ├── auth.rs          # Authentication client
│   └── views/           # UI views
│       └── login.rs     # Login page
├── Cargo.toml           # Dependencies
├── DESIGN_GUIDE.md      # UI styling reference
└── README.md
```

---

## Quick Commands

### Build

// turbo
```bash
cd newm-admin && cargo build
```

### Run (Development)

// turbo
```bash
cd newm-admin && cargo run
```

### Build Release

```bash
cd newm-admin && cargo build --release
```

### Format Code Check
// turbo
```bash
cd newm-admin && cargo fmt --all -- --check
```

### Format Code

// turbo
```bash
cd newm-admin && cargo fmt --all
```

### Lint

// turbo
```bash
cd newm-admin && cargo clippy --all-targets --all-features -- -D warnings
```

---

## Key Concepts

### GPUI Component Pattern

```rust
pub struct MyView {
    state: SomeState,
}

impl MyView {
    pub fn new(window: &mut Window, cx: &mut Context<Self>) -> Self {
        Self { state: SomeState::default() }
    }
}

impl Render for MyView {
    fn render(&mut self, _window: &mut Window, cx: &mut Context<Self>) -> impl IntoElement {
        div()
            .size_full()
            .child("Hello World")
    }
}
```

### Async HTTP with GPUI

GPUI uses its own async executor, not Tokio. Use `async_compat::Compat` to wrap Tokio-based futures:

```rust
use async_compat::Compat;

cx.spawn(async move |this, cx| {
    let result = Compat::new(async {
        client.post(&url).json(&body).send().await
    }).await;
    
    cx.update(|cx| {
        this.update(cx, |view, cx| {
            // Update UI state
            cx.notify();
        })
    })
}).detach();
```

### Input State Management

```rust
let input_state = cx.new(|cx| {
    InputState::new(window, cx)
        .placeholder("Email")
        .masked(true)  // For passwords
});

// Read value
let value = input_state.read(cx).value().to_string();
```

---

## Design System

See `DESIGN_GUIDE.md` for full styling reference.

### Colors (Dark Theme)

```rust
mod colors {
    pub fn bg_primary() -> Rgba { rgb(0x000000) }    // Pure black
    pub fn bg_surface() -> Rgba { rgb(0x1a1a1a) }    // Cards
    pub fn text_primary() -> Rgba { rgb(0xffffff) }  // White
    pub fn gradient_start() -> Rgba { rgb(0xC341F0) } // Purple
    pub fn gradient_end() -> Rgba { rgb(0xF53C69) }   // Pink
}
```

---

## Dependencies

Key crates in `Cargo.toml`:

| Crate | Purpose |
|-------|---------|
| `gpui` | UI framework |
| `gpui-component` | Pre-built components |
| `gpui-component-assets` | Icons, fonts |
| `async-compat` | Tokio/GPUI async bridge |
| `reqwest` | HTTP client |
| `serde` | JSON serialization |
| `tracing` | Logging |

---

## Troubleshooting

### Runtime Error: "no reactor running"

**Problem:** Using async reqwest without Tokio context  
**Solution:** Wrap async calls with `Compat::new()`:
```rust
Compat::new(async { /* tokio async code */ }).await
```

### Icon Not Rendering

**Problem:** Missing icon assets  
**Solution:** Ensure `gpui-component-assets` is initialized in `main.rs`:
```rust
gpui_component::init(cx);
```

### Build Fails on Linux

**Problem:** Missing system dependencies  
**Solution:**
```bash
sudo apt install libxkbcommon-dev libwayland-dev
```

---

## Visual Verification

This is a **desktop application**, not a web app. You cannot use browser tools to verify UI changes.

When visual verification of UI changes is needed:

1. **Run the application** using `cargo run`
2. **Ask the user to take a screenshot** of the relevant window/view
3. **Request the user provide the screenshot** so you can verify the UI appears correctly
4. Document what specific UI elements should be visible in the screenshot

Example prompt to user:
> "Please run the application and take a screenshot of [specific view]. I need to verify that [specific UI element] appears correctly. Attach the screenshot to your reply."