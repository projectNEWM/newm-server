use gpui::*;
use gpui_component::Root;

use crate::auth::{Environment, LoginResponse};
use crate::session::{Session, SessionExpiredEvent};
use crate::views::dashboard::DashboardView;
use crate::views::login::LoginView;

/// Current view state of the application
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum AppView {
    #[default]
    Login,
    Dashboard,
}

pub struct AdminApp {
    current_view: AppView,
    login_view: Entity<LoginView>,
    dashboard_view: Entity<DashboardView>,
    session: Option<Session>,
}

impl AdminApp {
    pub fn new(window: &mut Window, cx: &mut Context<Self>) -> Self {
        let login_view = cx.new(|cx| LoginView::new(window, cx));
        let dashboard_view = cx.new(|cx| DashboardView::new(window, cx));

        // Subscribe to login success events
        cx.subscribe(
            &login_view,
            |this, _login, event: &LoginSuccessEvent, cx| {
                // Create session from login response
                this.session = Some(Session::new(
                    event.login_response.clone(),
                    event.environment,
                ));

                // Update dashboard with session
                this.dashboard_view.update(cx, |dashboard, cx| {
                    dashboard.set_session(this.session.clone(), cx);
                });

                this.current_view = AppView::Dashboard;
                cx.notify();
            },
        )
        .detach();

        // Subscribe to session expired events from dashboard
        cx.subscribe(
            &dashboard_view,
            |this, _dashboard, event: &SessionExpiredEvent, cx| {
                tracing::warn!("Session expired: {}", event.message);

                // Clear session and return to login
                this.session = None;
                this.current_view = AppView::Login;

                cx.notify();
            },
        )
        .detach();

        Self {
            current_view: AppView::default(),
            login_view,
            dashboard_view,
            session: None,
        }
    }
}

impl Render for AdminApp {
    fn render(&mut self, window: &mut Window, cx: &mut Context<Self>) -> impl IntoElement {
        div()
            .size_full()
            .child(match self.current_view {
                AppView::Login => self.login_view.clone().into_any_element(),
                AppView::Dashboard => self.dashboard_view.clone().into_any_element(),
            })
            // Render notification layer on top
            .children(Root::render_notification_layer(window, cx))
    }
}

/// Event emitted when login succeeds, contains tokens and environment
pub struct LoginSuccessEvent {
    pub login_response: LoginResponse,
    pub environment: Environment,
}

impl EventEmitter<LoginSuccessEvent> for LoginView {}
impl EventEmitter<SessionExpiredEvent> for DashboardView {}
