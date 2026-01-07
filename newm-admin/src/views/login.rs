use std::sync::Arc;

use async_compat::Compat;
use gpui::prelude::FluentBuilder;
use gpui::*;
use gpui_component::{input::*, radio::*, *};

use crate::app::LoginSuccessEvent;
use crate::auth::{AuthClient, AuthError, Environment};
use crate::colors;

// Embed the NEWM logo at compile time
const LOGO_BYTES: &[u8] = include_bytes!("../NEWM_Logo.png");

// Custom gradient button component using GPUI's native linear_gradient
fn gradient_button(
    id: impl Into<ElementId>,
    label: impl Into<SharedString>,
    disabled: bool,
    on_click: impl Fn(&ClickEvent, &mut Window, &mut App) + 'static,
) -> impl IntoElement {
    let label = label.into();

    // 45 degrees = bottom-left to top-right diagonal
    // Convert Rgba to Hsla for LinearColorStop
    let start_color: Hsla = colors::gradient_start().into();
    let end_color: Hsla = colors::gradient_end().into();

    let gradient = linear_gradient(
        45.0,
        LinearColorStop {
            color: start_color,
            percentage: 0.0,
        },
        LinearColorStop {
            color: end_color,
            percentage: 1.0,
        },
    );

    let opacity = if disabled { 0.5 } else { 1.0 };

    let base = div()
        .id(id)
        .h_flex()
        .items_center()
        .justify_center()
        .w_full()
        .px_6()
        .py_2()
        .rounded(px(4.0))
        .bg(gradient)
        .text_color(colors::text_primary())
        .font_weight(FontWeight::MEDIUM)
        .cursor(if disabled {
            CursorStyle::default()
        } else {
            CursorStyle::PointingHand
        })
        .opacity(opacity)
        .child(label);

    if disabled {
        base
    } else {
        base.on_click(on_click)
    }
}

/// Login status for tracking async operations
#[derive(Debug, Clone, Default)]
pub enum LoginStatus {
    #[default]
    Idle,
    Loading,
    Error(String),
    Success,
}

pub struct LoginView {
    username_state: Entity<InputState>,
    password_state: Entity<InputState>,
    password_masked: bool,
    selected_environment: usize,
    login_status: LoginStatus,
}

impl LoginView {
    pub fn new(window: &mut Window, cx: &mut Context<Self>) -> Self {
        let email_state = cx.new(|cx| InputState::new(window, cx).placeholder("Email"));

        let password_state = cx.new(|cx| {
            InputState::new(window, cx)
                .placeholder("Password")
                .masked(true)
        });

        Self {
            username_state: email_state,
            password_state,
            password_masked: true,
            selected_environment: 0, // Default to Garage
            login_status: LoginStatus::Idle,
        }
    }

    fn perform_login(&mut self, cx: &mut Context<Self>) {
        let email = self.username_state.read(cx).value().to_string();
        let password = self.password_state.read(cx).value().to_string();
        let environment = Environment::from_index(self.selected_environment);

        // Validate inputs
        if email.is_empty() || password.is_empty() {
            self.login_status = LoginStatus::Error("Please enter email and password".to_string());
            cx.notify();
            return;
        }

        self.login_status = LoginStatus::Loading;
        cx.notify();

        // Spawn async task with Compat wrapper for Tokio compatibility
        cx.spawn(async move |this, cx| {
            let client = AuthClient::new();

            // Wrap the async reqwest call with Compat to enable Tokio context
            let result =
                Compat::new(async { client.login(&email, &password, environment).await }).await;

            cx.update(|cx| {
                this.update(cx, |view, cx| {
                    match result {
                        Ok(response) => {
                            tracing::info!(
                                "Login successful! Access token: {}...",
                                &response.access_token[..20.min(response.access_token.len())]
                            );
                            tracing::info!(
                                "Refresh token: {}...",
                                &response.refresh_token[..20.min(response.refresh_token.len())]
                            );
                            view.login_status = LoginStatus::Success;
                            // Emit event to switch to dashboard
                            cx.emit(LoginSuccessEvent);
                        }
                        Err(AuthError::Http {
                            status: 401,
                            message,
                        }) => {
                            view.login_status = LoginStatus::Error(message);
                        }
                        Err(e) => {
                            view.login_status = LoginStatus::Error(e.to_string());
                        }
                    }
                    cx.notify();
                })
            })
        })
        .detach();
    }
}

impl Render for LoginView {
    fn render(&mut self, _window: &mut Window, cx: &mut Context<Self>) -> impl IntoElement {
        let username_state = self.username_state.clone();
        let password_state = self.password_state.clone();
        let is_loading = matches!(self.login_status, LoginStatus::Loading);

        // Build environment radio options
        let radio_items = vec![
            Radio::new("garage").label("Garage"),
            Radio::new("studio").label("Studio"),
        ];

        div()
            .size_full()
            .v_flex()
            .items_center()
            .justify_center()
            .bg(colors::bg_primary())
            .child(
                div()
                    .v_flex()
                    .gap_6()
                    .p_8()
                    .rounded_lg()
                    .bg(colors::bg_surface())
                    .border_1()
                    .border_color(colors::border())
                    .w(px(400.0))
                    // Logo/Title section
                    .child(
                        div()
                            .v_flex()
                            .gap_2()
                            .items_center()
                            .child(
                                img(Arc::new(Image::from_bytes(
                                    ImageFormat::Png,
                                    LOGO_BYTES.to_vec(),
                                )))
                                .size(px(80.0)),
                            )
                            .child(
                                div()
                                    .text_2xl()
                                    .font_weight(FontWeight::BOLD)
                                    .text_color(colors::text_primary())
                                    .child("Welcome"),
                            ),
                    )
                    // Environment selector
                    .child(
                        div()
                            .v_flex()
                            .gap_2()
                            .child(
                                div()
                                    .text_sm()
                                    .text_color(colors::text_secondary())
                                    .child("Environment"),
                            )
                            .child(
                                RadioGroup::horizontal("env-selector")
                                    .selected_index(Some(self.selected_environment))
                                    .on_click(cx.listener(|this, index: &usize, _window, cx| {
                                        this.selected_environment = *index;
                                        cx.notify();
                                    }))
                                    .children(radio_items),
                            ),
                    )
                    // Email field
                    .child(
                        Input::new(&username_state)
                            .bg(colors::bg_surface())
                            .border_color(colors::border())
                            .text_color(colors::text_primary()),
                    )
                    // Password field
                    .child(
                        Input::new(&password_state)
                            .bg(colors::bg_surface())
                            .border_color(colors::border())
                            .text_color(colors::text_primary())
                            .suffix(
                                div()
                                    .id("password-toggle")
                                    .cursor_pointer()
                                    .child(
                                        Icon::new(if self.password_masked {
                                            IconName::Eye
                                        } else {
                                            IconName::EyeOff
                                        })
                                        .size(px(18.0))
                                        .text_color(colors::text_primary()),
                                    )
                                    .on_click(cx.listener(|this, _, window, cx| {
                                        this.password_masked = !this.password_masked;
                                        let masked = this.password_masked;
                                        this.password_state.update(
                                            cx,
                                            |state: &mut InputState, cx| {
                                                state.set_masked(masked, window, cx);
                                            },
                                        );
                                        cx.notify();
                                    })),
                            ),
                    )
                    // Error message
                    .when_some(
                        match &self.login_status {
                            LoginStatus::Error(msg) => Some(msg.clone()),
                            _ => None,
                        },
                        |this: Div, error_msg| {
                            this.child(div().text_sm().text_color(colors::error()).child(error_msg))
                        },
                    )
                    // Success message
                    .when(
                        matches!(self.login_status, LoginStatus::Success),
                        |this: Div| {
                            this.child(
                                div()
                                    .text_sm()
                                    .text_color(rgb(0x22c55e))
                                    .child("Login successful!"),
                            )
                        },
                    )
                    // Login button
                    .child(div().mt_2().child(gradient_button(
                        "login-btn",
                        if is_loading {
                            "Logging in..."
                        } else {
                            "Log In"
                        },
                        is_loading,
                        cx.listener(|this, _, _window, cx| {
                            this.perform_login(cx);
                        }),
                    ))),
            )
    }
}
