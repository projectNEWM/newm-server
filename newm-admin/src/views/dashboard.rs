//! Admin Dashboard View
//!
//! Main dashboard with sidebar navigation and work area content panels.

use gpui::prelude::FluentBuilder;
use gpui::*;
use gpui_component::*;

use crate::colors;

/// Currently selected menu item
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum MenuItem {
    #[default]
    Earnings,
    Refunds,
}

impl MenuItem {
    /// Get the display label for this menu item
    pub fn label(&self) -> &'static str {
        match self {
            MenuItem::Earnings => "Earnings",
            MenuItem::Refunds => "Refunds",
        }
    }

    /// Get the icon for this menu item
    pub fn icon(&self) -> IconName {
        match self {
            MenuItem::Earnings => IconName::ChartPie,
            MenuItem::Refunds => IconName::Undo,
        }
    }
}

pub struct DashboardView {
    selected_menu: MenuItem,
}

impl DashboardView {
    pub fn new(_window: &mut Window, _cx: &mut Context<Self>) -> Self {
        Self {
            selected_menu: MenuItem::default(),
        }
    }

    /// Render a sidebar menu button
    fn menu_button(&self, item: MenuItem, cx: &mut Context<Self>) -> impl IntoElement {
        let is_selected = self.selected_menu == item;
        let label = item.label();
        let icon = item.icon();

        div()
            .id(SharedString::from(format!("menu-{}", label.to_lowercase())))
            .h_flex()
            .gap_3()
            .items_center()
            .w_full()
            .px_4()
            .py_2()
            .rounded(px(4.0))
            .cursor_pointer()
            .when(is_selected, |this| this.bg(colors::bg_elevated()))
            .hover(|style| style.bg(colors::bg_elevated()))
            .child(Icon::new(icon).size(px(18.0)).text_color(if is_selected {
                colors::text_primary()
            } else {
                colors::text_secondary()
            }))
            .child(
                div()
                    .text_sm()
                    .text_color(if is_selected {
                        colors::text_primary()
                    } else {
                        colors::text_secondary()
                    })
                    .child(label),
            )
            .on_click(cx.listener(move |this, _, _window, cx| {
                this.selected_menu = item;
                cx.notify();
            }))
    }

    /// Render the work area content based on selected menu
    fn work_area_content(&self) -> AnyElement {
        match self.selected_menu {
            MenuItem::Earnings => self.earnings_panel().into_any_element(),
            MenuItem::Refunds => self.refunds_panel().into_any_element(),
        }
    }

    /// Earnings work area panel
    fn earnings_panel(&self) -> impl IntoElement {
        div()
            .v_flex()
            .gap_6()
            .child(
                div()
                    .text_2xl()
                    .font_weight(FontWeight::BOLD)
                    .text_color(colors::text_primary())
                    .child("Earnings"),
            )
            .child(
                div()
                    .text_color(colors::text_secondary())
                    .child("View and manage earnings across all artists and songs."),
            )
            // Mock earnings summary cards
            .child(
                div()
                    .h_flex()
                    .gap_4()
                    .child(self.stat_card("Total Earnings", "$124,523.45", colors::success()))
                    .child(self.stat_card("This Month", "$12,345.67", colors::text_primary()))
                    .child(self.stat_card("Pending", "$2,345.00", colors::text_secondary())),
            )
            // Mock table placeholder
            .child(
                div()
                    .mt_4()
                    .p_6()
                    .rounded_lg()
                    .bg(colors::bg_surface())
                    .border_1()
                    .border_color(colors::border())
                    .child(
                        div()
                            .text_sm()
                            .text_color(colors::text_muted())
                            .child("Earnings table will display here..."),
                    ),
            )
    }

    /// Refunds work area panel
    fn refunds_panel(&self) -> impl IntoElement {
        div()
            .v_flex()
            .gap_6()
            .child(
                div()
                    .text_2xl()
                    .font_weight(FontWeight::BOLD)
                    .text_color(colors::text_primary())
                    .child("Refunds"),
            )
            .child(
                div()
                    .text_color(colors::text_secondary())
                    .child("Process and track refund requests."),
            )
            // Mock refunds summary cards
            .child(
                div()
                    .h_flex()
                    .gap_4()
                    .child(self.stat_card("Total Refunds", "$3,456.78", colors::error()))
                    .child(self.stat_card("Pending Requests", "12", colors::text_primary()))
                    .child(self.stat_card("Processed Today", "5", colors::success())),
            )
            // Mock table placeholder
            .child(
                div()
                    .mt_4()
                    .p_6()
                    .rounded_lg()
                    .bg(colors::bg_surface())
                    .border_1()
                    .border_color(colors::border())
                    .child(
                        div()
                            .text_sm()
                            .text_color(colors::text_muted())
                            .child("Refunds table will display here..."),
                    ),
            )
    }

    /// Create a statistics card
    fn stat_card(
        &self,
        label: &'static str,
        value: &'static str,
        value_color: Rgba,
    ) -> impl IntoElement {
        div()
            .v_flex()
            .gap_2()
            .p_4()
            .rounded_lg()
            .bg(colors::bg_surface())
            .border_1()
            .border_color(colors::border())
            .min_w(px(150.0))
            .child(
                div()
                    .text_xs()
                    .text_color(colors::text_secondary())
                    .child(label),
            )
            .child(
                div()
                    .text_xl()
                    .font_weight(FontWeight::SEMIBOLD)
                    .text_color(value_color)
                    .child(value),
            )
    }
}

impl Render for DashboardView {
    fn render(&mut self, _window: &mut Window, cx: &mut Context<Self>) -> impl IntoElement {
        div()
            .size_full()
            .h_flex()
            .bg(colors::bg_primary())
            // Sidebar
            .child(
                div()
                    .v_flex()
                    .w(px(200.0))
                    .h_full()
                    .bg(colors::bg_surface())
                    .border_r_1()
                    .border_color(colors::border())
                    .p_4()
                    .gap_2()
                    // Section header
                    .child(
                        div()
                            .text_xs()
                            .font_weight(FontWeight::SEMIBOLD)
                            .text_color(colors::text_muted())
                            .mb_2()
                            .child("ADMIN"),
                    )
                    // Menu items
                    .child(self.menu_button(MenuItem::Earnings, cx))
                    .child(self.menu_button(MenuItem::Refunds, cx)),
            )
            // Work Area
            .child(
                div()
                    .flex_1()
                    .h_full()
                    .p_8()
                    .child(self.work_area_content()),
            )
    }
}
