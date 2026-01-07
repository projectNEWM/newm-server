use gpui::*;

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
}

impl AdminApp {
    pub fn new(window: &mut Window, cx: &mut Context<Self>) -> Self {
        let login_view = cx.new(|cx| LoginView::new(window, cx));
        let dashboard_view = cx.new(|cx| DashboardView::new(window, cx));

        // Subscribe to login success events
        cx.subscribe(
            &login_view,
            |this, _login, _event: &LoginSuccessEvent, cx| {
                this.current_view = AppView::Dashboard;
                cx.notify();
            },
        )
        .detach();

        Self {
            current_view: AppView::default(),
            login_view,
            dashboard_view,
        }
    }
}

impl Render for AdminApp {
    fn render(&mut self, _window: &mut Window, _cx: &mut Context<Self>) -> impl IntoElement {
        div().size_full().child(match self.current_view {
            AppView::Login => self.login_view.clone().into_any_element(),
            AppView::Dashboard => self.dashboard_view.clone().into_any_element(),
        })
    }
}

/// Event emitted when login succeeds
pub struct LoginSuccessEvent;

impl EventEmitter<LoginSuccessEvent> for LoginView {}
