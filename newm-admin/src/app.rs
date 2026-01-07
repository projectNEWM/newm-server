use gpui::*;

use crate::views::login::LoginView;

pub struct AdminApp {
    login_view: Entity<LoginView>,
}

impl AdminApp {
    pub fn new(window: &mut Window, cx: &mut Context<Self>) -> Self {
        let login_view = cx.new(|cx| LoginView::new(window, cx));
        Self { login_view }
    }
}

impl Render for AdminApp {
    fn render(&mut self, _window: &mut Window, _cx: &mut Context<Self>) -> impl IntoElement {
        div().size_full().child(self.login_view.clone())
    }
}
