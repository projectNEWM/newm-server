mod app;
mod auth;
mod views;

use gpui::*;
use gpui_component::*;
use gpui_component::theme::{Theme, ThemeMode};
use gpui_component_assets::Assets;
use tracing_subscriber::{layer::SubscriberExt, util::SubscriberInitExt};

fn main() {
    // ... tracing init ...
    tracing_subscriber::registry()
        .with(tracing_subscriber::fmt::layer())
        .with(
            tracing_subscriber::EnvFilter::from_default_env()
                .add_directive("newm_admin=debug".parse().unwrap()),
        )
        .init();

    tracing::info!("Starting NEWM Admin");

    // Initialize GPUI application
    let app = Application::new().with_assets(Assets);
    app.run(move |cx| {
        // Initialize gpui-component system
        gpui_component::init(cx);
        
        // Set dark theme mode
        Theme::change(ThemeMode::Dark, None, cx);

        cx.spawn(async move |cx| {
            cx.open_window(
                WindowOptions {
                    window_bounds: Some(WindowBounds::Windowed(Bounds::new(
                        point(px(100.0), px(100.0)),
                        size(px(800.0), px(600.0)),
                    ))),
                    ..Default::default()
                },
                |window, cx| {
                    let view = cx.new(|cx| app::AdminApp::new(window, cx));
                    cx.new(|cx| Root::new(view, window, cx))
                },
            )?;
            Ok::<_, anyhow::Error>(())
        })
        .detach();
    });
}

