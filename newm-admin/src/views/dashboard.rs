//! Admin Dashboard View
//!
//! Main dashboard with sidebar navigation and work area content panels.

use async_compat::Compat;
use gpui::prelude::FluentBuilder;
use gpui::*;
use gpui_component::button::{Button, ButtonVariants};
use gpui_component::calendar::{Calendar, CalendarState, Date};
use std::sync::Arc;

use chrono::Datelike;
use gpui::InteractiveElement;
use gpui_component::input::*;
use gpui_component::popover::Popover;
use gpui_component::table::{Column, ColumnSort, Table, TableDelegate, TableState};
use gpui_component::tooltip::Tooltip;
use gpui_component::*;

use crate::colors;
const REFRESH_SVG: &[u8] = include_bytes!("../../assets/refresh.svg");
const UPLOAD_SVG: &[u8] = include_bytes!("../../assets/upload.svg");
use crate::csv_import::{CsvImportSummary, CsvResult, parse_csv, write_results};
use crate::earnings::{Earning, EarningsClient, EarningsError, usd_to_amount};
use crate::session::{Session, SessionExpiredEvent};
use crate::toast;

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

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum SortDirection {
    Ascending,
    Descending,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum SortColumn {
    Amount,
    CreatedAt,
    Claimed,
}

/// Selection state for the table header checkbox
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum SelectionState {
    /// No rows selected
    None,
    /// Some rows selected (indeterminate)
    Some,
    /// All rows selected
    All,
}

/// Format 6-decimal integer amount with commas (e.g., 1000000 -> "1.000000", 1234567890 -> "1,234.567890")
fn format_amount(amount: i64) -> String {
    let sign = if amount < 0 { "-" } else { "" };
    let abs_amount = amount.abs();
    let integer_part = abs_amount / 1_000_000;
    let decimal_part = abs_amount % 1_000_000;

    let integer_str = integer_part.to_string();
    let mut formatted_integer = String::new();
    for (i, c) in integer_str.chars().rev().enumerate() {
        if i > 0 && i % 3 == 0 {
            formatted_integer.push(',');
        }
        formatted_integer.push(c);
    }
    let formatted_integer: String = formatted_integer.chars().rev().collect();

    format!("{}{}.{:06}", sign, formatted_integer, decimal_part)
}

// -----------------------------------------------------------------------------
// Earnings Table Delegate
// -----------------------------------------------------------------------------

#[derive(Clone)]
struct EarningsTableDelegate {
    earnings: Vec<Earning>,
    selected_ids: std::collections::HashSet<String>,
    sort_column: SortColumn,
    sort_direction: SortDirection,
    columns: Vec<Column>,
}

impl EarningsTableDelegate {
    fn new() -> Self {
        Self {
            earnings: Vec::new(),
            selected_ids: std::collections::HashSet::new(),
            sort_column: SortColumn::CreatedAt,
            sort_direction: SortDirection::Descending,
            columns: vec![
                Column::new("select", "").width(px(50.)),
                Column::new("song_id", "Song ID").width(px(250.)),
                Column::new("stake", "Stake Address").width(px(250.)),
                Column::new("memo", "Memo").width(px(500.)),
                Column::new("amount", "Amount").width(px(180.)).sortable(),
                Column::new("claimed", "Claimed").width(px(120.)).sortable(),
                Column::new("created_at", "Created At")
                    .width(px(150.))
                    .sortable(),
            ],
        }
    }

    fn set_data(&mut self, data: Vec<Earning>) {
        self.earnings = data;
        // Clear selection when data changes
        self.selected_ids.clear();
        self.sort_data();
    }

    fn sort_data(&mut self) {
        let col = self.sort_column;
        let dir = self.sort_direction;

        self.earnings.sort_by(|a, b| {
            let cmp = match col {
                SortColumn::Amount => a.amount.cmp(&b.amount),
                SortColumn::Claimed => a.claimed.cmp(&b.claimed),
                SortColumn::CreatedAt => a.created_at.cmp(&b.created_at),
            };
            match dir {
                SortDirection::Ascending => cmp,
                SortDirection::Descending => cmp.reverse(),
            }
        });
    }

    #[allow(dead_code)]
    fn set_memo_width(&mut self, width: f32) {
        if let Some(col) = self.columns.get_mut(3) {
            col.width = px(width);
        }
    }

    fn toggle_selection(&mut self, id: &str) {
        if self.selected_ids.contains(id) {
            self.selected_ids.remove(id);
        } else {
            self.selected_ids.insert(id.to_string());
        }
    }

    fn select_all(&mut self) {
        for earning in &self.earnings {
            if let Some(id) = &earning.id {
                self.selected_ids.insert(id.clone());
            }
        }
    }

    fn deselect_all(&mut self) {
        self.selected_ids.clear();
    }

    fn is_all_selected(&self) -> bool {
        if self.earnings.is_empty() {
            return false;
        }
        self.earnings.iter().all(|e| {
            e.id.as_ref()
                .is_some_and(|id| self.selected_ids.contains(id))
        })
    }

    fn selected_count(&self) -> usize {
        self.selected_ids.len()
    }

    fn get_selected_ids(&self) -> Vec<String> {
        self.selected_ids.iter().cloned().collect()
    }

    /// Returns the selection state: None selected, Some selected, or All selected
    fn selection_state(&self) -> SelectionState {
        if self.earnings.is_empty() {
            return SelectionState::None;
        }
        let selected_in_current = self
            .earnings
            .iter()
            .filter(|e| {
                e.id.as_ref()
                    .is_some_and(|id| self.selected_ids.contains(id))
            })
            .count();
        if selected_in_current == 0 {
            SelectionState::None
        } else if selected_in_current == self.earnings.len() {
            SelectionState::All
        } else {
            SelectionState::Some
        }
    }
}

impl TableDelegate for EarningsTableDelegate {
    fn columns_count(&self, _cx: &App) -> usize {
        7
    }

    fn rows_count(&self, _cx: &App) -> usize {
        self.earnings.len()
    }

    fn column(&self, col_ix: usize, _cx: &App) -> &Column {
        &self.columns[col_ix]
    }

    fn perform_sort(
        &mut self,
        col_ix: usize,
        sort: ColumnSort,
        _window: &mut Window,
        cx: &mut Context<TableState<Self>>,
    ) {
        self.sort_direction = match sort {
            ColumnSort::Ascending => SortDirection::Ascending,
            ColumnSort::Descending => SortDirection::Descending,
            _ => SortDirection::Descending,
        };

        self.sort_column = match col_ix {
            4 => SortColumn::Amount,
            5 => SortColumn::Claimed,
            6 => SortColumn::CreatedAt,
            _ => self.sort_column,
        };

        self.sort_data();
        cx.notify();
    }

    fn render_th(
        &mut self,
        col_ix: usize,
        _window: &mut Window,
        cx: &mut Context<TableState<Self>>,
    ) -> impl IntoElement {
        // For the checkbox column (column 0), render a select-all checkbox
        if col_ix == 0 {
            let selection_state = self.selection_state();
            let (icon_name, is_checked) = match selection_state {
                SelectionState::None => (None, false),
                SelectionState::Some => (Some(IconName::Minus), true),
                SelectionState::All => (Some(IconName::Check), true),
            };

            let bg_color = if is_checked {
                cx.theme().primary
            } else {
                cx.theme().background
            };

            let border_color = if is_checked {
                cx.theme().primary
            } else {
                cx.theme().input
            };

            return div()
                .id("select-all-header")
                .size_full()
                .flex()
                .items_center()
                .justify_center()
                .child(
                    div()
                        .id("select-all-checkbox")
                        .relative()
                        .size_4()
                        .flex_shrink_0()
                        .border_1()
                        .border_color(border_color)
                        .rounded(px(4.))
                        .bg(bg_color)
                        .cursor_pointer()
                        .when(icon_name.is_some(), |this| {
                            this.child(
                                Icon::new(icon_name.unwrap())
                                    .size(px(14.))
                                    .text_color(cx.theme().primary_foreground)
                                    .absolute()
                                    .top_px()
                                    .left_px(),
                            )
                        })
                        .on_click(cx.listener(move |table_state, _, _window, cx| {
                            let delegate = table_state.delegate_mut();
                            if delegate.is_all_selected() {
                                delegate.deselect_all();
                            } else {
                                delegate.select_all();
                            }
                            cx.notify();
                        }))
                        .tooltip(move |window, cx| Tooltip::new("Select All").build(window, cx)),
                )
                .into_any_element();
        }

        // For other columns, render the column name (default behavior)
        div()
            .size_full()
            .child(self.column(col_ix, cx).name.clone())
            .into_any_element()
    }

    fn render_td(
        &mut self,
        row_ix: usize,
        col_ix: usize,
        _window: &mut Window,
        cx: &mut Context<TableState<Self>>,
    ) -> impl IntoElement {
        let earning = &self.earnings[row_ix];
        match col_ix {
            // Checkbox column
            0 => {
                let id = earning.id.clone().unwrap_or_default();
                let is_checked = self.selected_ids.contains(&id);
                div()
                    .size_full()
                    .flex()
                    .items_center()
                    .justify_center()
                    .child(
                        gpui_component::checkbox::Checkbox::new(SharedString::from(format!(
                            "select-{}",
                            row_ix
                        )))
                        .checked(is_checked)
                        .on_click(cx.listener({
                            let id = id.clone();
                            move |table_state, _checked, _window, cx| {
                                table_state.delegate_mut().toggle_selection(&id);
                                cx.notify();
                            }
                        })),
                    )
            }
            1 => div().size_full().overflow_hidden().child(
                div()
                    .id(SharedString::from(format!("copy-song-{}", row_ix)))
                    .size_full()
                    .flex()
                    .items_center()
                    .justify_start()
                    .px_2()
                    .cursor_pointer()
                    .hover(|s| s.bg(colors::bg_elevated()))
                    .active(|s| s.bg(colors::border()))
                    .child(
                        div()
                            .size_full()
                            .overflow_hidden()
                            .whitespace_nowrap()
                            .text_ellipsis()
                            .child(earning.song_id.clone().unwrap_or_default()),
                    )
                    .tooltip({
                        let text = earning.song_id.clone().unwrap_or_default();
                        move |window, cx| Tooltip::new(text.clone()).build(window, cx)
                    })
                    .on_click(cx.listener({
                        let text = earning.song_id.clone().unwrap_or_default();
                        move |_, _, window, cx| {
                            cx.write_to_clipboard(ClipboardItem::new_string(text.clone()));
                            toast::show_info(window, cx, "Song ID copied to clipboard");
                            cx.notify();
                        }
                    })),
            ),
            2 => div().size_full().overflow_hidden().child(
                div()
                    .id(SharedString::from(format!("copy-stake-{}", row_ix)))
                    .size_full()
                    .flex()
                    .items_center()
                    .justify_start()
                    .px_2()
                    .cursor_pointer()
                    .hover(|s| s.bg(colors::bg_elevated()))
                    .active(|s| s.bg(colors::border()))
                    .child(
                        div()
                            .size_full()
                            .overflow_hidden()
                            .whitespace_nowrap()
                            .text_ellipsis()
                            .child(earning.stake_address.clone()),
                    )
                    .tooltip({
                        let text = earning.stake_address.clone();
                        move |window, cx| Tooltip::new(text.clone()).build(window, cx)
                    })
                    .on_click(cx.listener({
                        let text = earning.stake_address.clone();
                        move |_, _, window, cx| {
                            cx.write_to_clipboard(ClipboardItem::new_string(text.clone()));
                            toast::show_info(window, cx, "Stake Address copied to clipboard");
                            cx.notify();
                        }
                    })),
            ),
            3 => div().size_full().overflow_hidden().child(
                div()
                    .id(SharedString::from(format!("copy-memo-{}", row_ix)))
                    .size_full()
                    .flex()
                    .items_center()
                    .justify_start()
                    .px_2()
                    .cursor_pointer()
                    .hover(|s| s.bg(colors::bg_elevated()))
                    .active(|s| s.bg(colors::border()))
                    .child(
                        div()
                            .size_full()
                            .overflow_hidden()
                            .whitespace_nowrap()
                            .text_ellipsis()
                            .child(earning.memo.clone().unwrap_or_default()),
                    )
                    .tooltip({
                        let text = earning.memo.clone().unwrap_or_default();
                        move |window, cx| Tooltip::new(text.clone()).build(window, cx)
                    })
                    .on_click(cx.listener({
                        let text = earning.memo.clone().unwrap_or_default();
                        move |_, _, window, cx| {
                            cx.write_to_clipboard(ClipboardItem::new_string(text.clone()));
                            toast::show_info(window, cx, "Memo copied to clipboard");
                            cx.notify();
                        }
                    })),
            ),
            4 => div().child(format!("Ɲ {}", format_amount(earning.amount))),
            5 => div().child(if earning.claimed {
                Icon::new(IconName::Check)
                    .size(px(16.0))
                    .text_color(colors::success())
            } else {
                Icon::new(IconName::Close)
                    .size(px(16.0))
                    .text_color(colors::text_muted())
            }),
            6 => div().child(
                earning
                    .created_at
                    .split('T')
                    .next()
                    .unwrap_or(&earning.created_at)
                    .to_string(),
            ),
            _ => div(),
        }
    }
}

pub struct DashboardView {
    selected_menu: MenuItem,
    session: Option<Session>,

    // Add Earnings panel state
    show_add_earnings: bool,
    song_id_input: Entity<InputState>,
    usd_amount_input: Entity<InputState>,
    is_submitting: bool,
    form_error: Option<String>,
    clear_form_on_open: bool,

    // CSV Import state
    is_importing_csv: bool,
    csv_import_progress: Option<(usize, usize)>, // (current, total)

    // Delete Earnings state
    is_deleting: bool,
    show_delete_confirmation: bool,

    // Earnings Table state
    earnings: Option<Vec<Earning>>,
    filtered_earnings: Option<Vec<Earning>>,
    is_loading_earnings: bool,
    // Filtering
    search_input: Entity<InputState>,
    calendar_state: Entity<CalendarState>,
    date_picker_open: bool,
    table: Entity<TableState<EarningsTableDelegate>>,
}

impl DashboardView {
    pub fn new(window: &mut Window, cx: &mut Context<Self>) -> Self {
        let song_id_input = cx.new(|cx| InputState::new(window, cx).placeholder("Song ID or ISRC"));
        let search_input = cx.new(|cx| {
            InputState::new(window, cx).placeholder("Search by Song ID, Stake Address, or Memo")
        });

        // Initialize Calendar State
        let calendar_state = cx.new(|cx| {
            let mut state = CalendarState::new(window, cx);
            // Set empty range to switch to Range mode
            state.set_date(Date::Range(None, None), window, cx);
            state
        });

        let usd_amount_input = cx.new(|cx| {
            InputState::new(window, cx)
                .placeholder("Amount in USD (e.g., 10.50)")
                .validate(|text, _cx| {
                    // Allow empty input
                    if text.is_empty() {
                        return true;
                    }
                    // Check decimal places
                    if let Some(dot_pos) = text.find('.') {
                        let decimals = text.len() - dot_pos - 1;
                        if decimals > 6 {
                            return false; // Reject: too many decimal places
                        }
                    }
                    // Allow digits and single dot only (no negative values)
                    text.chars().all(|c| c.is_ascii_digit() || c == '.')
                })
        });

        let delegate = EarningsTableDelegate::new();
        // Create table state
        let table = cx.new(|cx| TableState::new(delegate, window, cx));

        cx.observe(&search_input, |this: &mut Self, _, cx| {
            this.update_table(cx);
        })
        .detach();

        cx.observe(&calendar_state, |this: &mut Self, _, cx| {
            this.update_table(cx);
        })
        .detach();

        Self {
            selected_menu: MenuItem::default(),
            session: None,
            show_add_earnings: false,
            song_id_input,
            usd_amount_input,
            is_submitting: false,
            form_error: None,
            clear_form_on_open: false,
            is_importing_csv: false,
            csv_import_progress: None,
            is_deleting: false,
            show_delete_confirmation: false,
            earnings: None,
            filtered_earnings: None,
            is_loading_earnings: false,
            search_input,
            calendar_state,
            date_picker_open: false,
            table,
        }
    }

    fn update_table(&mut self, cx: &mut Context<Self>) {
        if let Some(earnings) = &self.earnings {
            let search_query = self.search_input.read(cx).text().to_string().to_lowercase();
            let date_range = self.calendar_state.read(cx).date();

            let filtered_earnings: Vec<Earning> = earnings
                .iter()
                .filter(|earning| {
                    let matches_search = if search_query.is_empty() {
                        true
                    } else {
                        earning
                            .song_id
                            .as_deref()
                            .unwrap_or("")
                            .to_lowercase()
                            .contains(&search_query)
                            || earning.stake_address.to_lowercase().contains(&search_query)
                            || earning
                                .memo
                                .as_deref()
                                .unwrap_or("")
                                .to_lowercase()
                                .contains(&search_query)
                    };

                    let matches_date = match date_range {
                        Date::Range(Some(start), Some(end)) => {
                            if let Ok(created_date) = chrono::NaiveDate::parse_from_str(
                                earning.created_at.split('T').next().unwrap_or(""),
                                "%Y-%m-%d",
                            ) {
                                // Convert start/end to NaiveDate
                                let start_date = chrono::NaiveDate::from_ymd_opt(
                                    start.year(),
                                    start.month(),
                                    start.day(),
                                )
                                .unwrap();
                                let end_date = chrono::NaiveDate::from_ymd_opt(
                                    end.year(),
                                    end.month(),
                                    end.day(),
                                )
                                .unwrap();
                                created_date >= start_date && created_date <= end_date
                            } else {
                                true
                            }
                        }
                        _ => true,
                    };

                    matches_search && matches_date
                })
                .cloned()
                .collect();

            self.filtered_earnings = Some(filtered_earnings.clone());

            self.table.update(cx, |table, cx| {
                table.delegate_mut().set_data(filtered_earnings);
                cx.notify();
            });
            cx.notify();
        }
    }

    /// Set the session (called from AdminApp after login)
    pub fn set_session(&mut self, session: Option<Session>, cx: &mut Context<Self>) {
        self.session = session;
        if self.session.is_some() {
            self.fetch_earnings(cx);
        }
    }

    /// Refresh data for the current view
    pub fn refresh_data(&mut self, cx: &mut Context<Self>) {
        if self.session.is_some() && self.selected_menu == MenuItem::Earnings {
            self.fetch_earnings(cx);
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
                this.refresh_data(cx);
                cx.notify();
            }))
    }

    /// Render the work area content based on selected menu
    fn work_area_content(&self, cx: &mut Context<Self>) -> AnyElement {
        match self.selected_menu {
            MenuItem::Earnings => self.earnings_panel(cx).into_any_element(),
            MenuItem::Refunds => self.refunds_panel().into_any_element(),
        }
    }

    /// Earnings work area panel
    fn earnings_panel(&self, cx: &mut Context<Self>) -> impl IntoElement {
        let filtered_rows = self.filtered_earnings.clone().unwrap_or_default();

        // Calculate stats specific to the filtered view
        let total: i64 = filtered_rows.iter().map(|e| e.amount).sum();
        let claimed: i64 = filtered_rows
            .iter()
            .filter(|e| e.claimed)
            .map(|e| e.amount)
            .sum();
        let unclaimed = total - claimed;

        div()
            .v_flex()
            .size_full()
            .overflow_hidden()
            .gap_6()
            .child(
                div()
                    .h_flex()
                    .items_center()
                    .justify_between()
                    .child(
                        div()
                            .h_flex()
                            .gap_4()
                            .items_center()
                            .child(
                                div()
                                    .text_2xl()
                                    .font_weight(FontWeight::BOLD)
                                    .text_color(colors::text_primary())
                                    .child("Earnings"),
                            )
                            .child(
                                div().w(px(300.0)).child(
                                    Input::new(&self.search_input).prefix(
                                        Icon::new(IconName::Search)
                                            .size(px(16.0))
                                            .text_color(colors::text_secondary()),
                                    ),
                                ),
                            )
                            .child(
                                div().w(px(240.0)).child(
                                    Popover::new("date-range-picker")
                                        .trigger(
                                            Button::new("date-summary")
                                                .label(
                                                    if let Date::Range(Some(start), Some(end)) =
                                                        self.calendar_state.read(cx).date()
                                                    {
                                                        format!(
                                                            "{} - {}",
                                                            start.format("%b %d"),
                                                            end.format("%b %d")
                                                        )
                                                    } else {
                                                        "Select Date Range".to_string()
                                                    },
                                                )
                                                .icon(Icon::new(IconName::Calendar).size(px(16.0)))
                                                .w_full()
                                                .on_click(cx.listener(|this, _, _, cx| {
                                                    this.date_picker_open = !this.date_picker_open;
                                                    cx.notify();
                                                })),
                                        )
                                        .content({
                                            let calendar_state = self.calendar_state.clone();
                                            move |_, _, _| {
                                                div()
                                                    .w(px(300.0))
                                                    .child(Calendar::new(&calendar_state))
                                            }
                                        }),
                                ),
                            ),
                    )
                    // Toolbar with Add Earnings button
                    .child(
                        div()
                            .h_flex()
                            .gap_2()
                            .child(
                                Button::new("refresh-earnings-btn")
                                    .child(
                                        img(Arc::new(Image::from_bytes(
                                            ImageFormat::Svg,
                                            REFRESH_SVG.to_vec(),
                                        )))
                                        .size(px(16.0)),
                                    )
                                    .tooltip("Refresh")
                                    .ghost()
                                    .on_click(cx.listener(|this, _, _window, cx| {
                                        this.fetch_earnings(cx);
                                    })),
                            )
                            .child(
                                Button::new("add-earnings-btn")
                                    .label("Add Earnings")
                                    .icon(Icon::new(IconName::Plus).size(px(16.0)))
                                    .disabled(self.is_importing_csv)
                                    .on_click(cx.listener(|this, _, window, cx| {
                                        // Clear form if flag is set
                                        if this.clear_form_on_open {
                                            this.song_id_input.update(cx, |state, cx| {
                                                state.set_value("", window, cx);
                                            });
                                            this.usd_amount_input.update(cx, |state, cx| {
                                                state.set_value("", window, cx);
                                            });
                                            this.clear_form_on_open = false;
                                        }
                                        this.show_add_earnings = true;
                                        this.form_error = None;
                                        cx.notify();
                                    })),
                            )
                            .child(
                                Button::new("upload-csv-btn")
                                    .child(
                                        img(Arc::new(Image::from_bytes(
                                            ImageFormat::Svg,
                                            UPLOAD_SVG.to_vec(),
                                        )))
                                        .size(px(16.0)),
                                    )
                                    .label(if self.is_importing_csv {
                                        if let Some((current, total)) = self.csv_import_progress {
                                            format!("Importing {}/{}...", current, total)
                                        } else {
                                            "Importing...".to_string()
                                        }
                                    } else {
                                        "Upload CSV".to_string()
                                    })
                                    .disabled(self.is_importing_csv)
                                    .on_click(cx.listener(|this, _, _window, cx| {
                                        this.upload_csv(cx);
                                    })),
                            )
                            .child({
                                let selected_count =
                                    self.table.read(cx).delegate().selected_count();
                                Button::new("delete-selected-btn")
                                    .label(if self.is_deleting {
                                        "Deleting...".to_string()
                                    } else if selected_count > 0 {
                                        format!("Delete Selected ({})", selected_count)
                                    } else {
                                        "Delete Selected".to_string()
                                    })
                                    .icon(Icon::new(IconName::Delete).size(px(16.0)))
                                    .danger()
                                    .disabled(
                                        selected_count == 0
                                            || self.is_deleting
                                            || self.is_importing_csv,
                                    )
                                    .on_click(cx.listener(|this, _, _window, cx| {
                                        this.show_delete_confirmation = true;
                                        cx.notify();
                                    }))
                            }),
                    ),
            )
            .child(
                div()
                    .text_color(colors::text_secondary())
                    .child("View and manage earnings across all artists and songs."),
            )
            // Summary cards
            .child(
                div()
                    .h_flex()
                    .gap_4()
                    .child(self.stat_card(
                        "Total Earnings",
                        format!("Ɲ {}", format_amount(total)),
                        colors::success(),
                    ))
                    .child(self.stat_card(
                        "Claimed",
                        format!("Ɲ {}", format_amount(claimed)),
                        colors::text_primary(),
                    ))
                    .child(self.stat_card(
                        "Unclaimed",
                        format!("Ɲ {}", format_amount(unclaimed)),
                        colors::text_secondary(),
                    )),
            )
            // Table content
            .child(
                div()
                    .h_full()
                    .w_full()
                    .flex_1()
                    .mt_4()
                    .rounded_lg()
                    .bg(colors::bg_surface())
                    .border_1()
                    .border_color(colors::border())
                    .overflow_hidden()
                    .child(self.earnings_table_content(cx)),
            )
    }

    fn earnings_table_content(&self, _cx: &mut Context<Self>) -> impl IntoElement {
        div()
            .h_full()
            .w_full()
            .overflow_hidden()
            .child(Table::new(&self.table))
            .into_any_element()
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
    /// Create a statistics card
    fn stat_card(
        &self,
        label: impl Into<SharedString>,
        value: impl Into<SharedString>,
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
            .min_w(px(180.0))
            .child(
                div()
                    .text_xs()
                    .text_color(colors::text_secondary())
                    .child(label.into()),
            )
            .child(
                div()
                    .text_xl()
                    .font_weight(FontWeight::SEMIBOLD)
                    .text_color(value_color)
                    .child(value.into()),
            )
    }

    /// Render the Add Earnings slide-out panel
    fn add_earnings_panel(&self, cx: &mut Context<Self>) -> impl IntoElement {
        let song_id_input = self.song_id_input.clone();
        let usd_amount_input = self.usd_amount_input.clone();

        // Slide-out panel from right
        div()
            .absolute()
            .top_0()
            .right_0()
            .h_full()
            .w(px(400.0))
            .bg(colors::bg_surface())
            .border_l_1()
            .border_color(colors::border())
            .shadow_lg()
            .v_flex()
            .p_6()
            .gap_4()
            // Header
            .child(
                div()
                    .h_flex()
                    .items_center()
                    .justify_between()
                    .child(
                        div()
                            .text_xl()
                            .font_weight(FontWeight::BOLD)
                            .text_color(colors::text_primary())
                            .child("Add Earnings"),
                    )
                    .child(
                        div()
                            .id("close-add-earnings")
                            .cursor_pointer()
                            .child(
                                Icon::new(IconName::Close)
                                    .size(px(20.0))
                                    .text_color(colors::text_secondary()),
                            )
                            .on_click(cx.listener(|this, _, _window, cx| {
                                this.show_add_earnings = false;
                                cx.notify();
                            })),
                    ),
            )
            .child(div().text_sm().text_color(colors::text_secondary()).child(
                "Create new earnings for a song. The USD amount will be converted to NEWM tokens.",
            ))
            // Error message
            .when_some(self.form_error.clone(), |this, error_msg| {
                this.child(
                    div()
                        .text_sm()
                        .text_color(colors::error())
                        .p_2()
                        .rounded(px(4.0))
                        .bg(rgba(0xff000020))
                        .child(error_msg),
                )
            })
            // Song ID / ISRC field
            .child(
                div()
                    .v_flex()
                    .gap_1()
                    .child(
                        div()
                            .text_sm()
                            .font_weight(FontWeight::MEDIUM)
                            .text_color(colors::text_primary())
                            .child("Song ID or ISRC"),
                    )
                    .child(
                        Input::new(&song_id_input)
                            .bg(colors::bg_surface())
                            .border_color(colors::border())
                            .text_color(colors::text_primary()),
                    ),
            )
            // USD Amount field
            .child(
                div()
                    .v_flex()
                    .gap_1()
                    .child(
                        div()
                            .text_sm()
                            .font_weight(FontWeight::MEDIUM)
                            .text_color(colors::text_primary())
                            .child("Amount (USD)"),
                    )
                    .child(
                        Input::new(&usd_amount_input)
                            .bg(colors::bg_surface())
                            .border_color(colors::border())
                            .text_color(colors::text_primary()),
                    ),
            )
            // Spacer to push buttons to bottom
            .child(div().flex_1())
            // Buttons
            .child(
                div()
                    .h_flex()
                    .gap_3()
                    .justify_end()
                    .child(
                        Button::new("cancel-add-earnings")
                            .label("Cancel")
                            .disabled(self.is_submitting)
                            .on_click(cx.listener(|this, _, _window, cx| {
                                this.show_add_earnings = false;
                                cx.notify();
                            })),
                    )
                    .child(
                        Button::new("submit-add-earnings")
                            .primary()
                            .label(if self.is_submitting {
                                "Submitting..."
                            } else {
                                "Submit"
                            })
                            .disabled(self.is_submitting)
                            .on_click(cx.listener(|this, _, _window, cx| {
                                this.submit_add_earnings(cx);
                            })),
                    ),
            )
    }

    /// Submit the Add Earnings form
    fn submit_add_earnings(&mut self, cx: &mut Context<Self>) {
        let song_id = self.song_id_input.read(cx).value().to_string();
        let usd_str = self.usd_amount_input.read(cx).value().to_string();

        // Clear previous error
        self.form_error = None;

        // Validate inputs
        if song_id.trim().is_empty() {
            self.form_error = Some("Please enter a Song ID or ISRC".to_string());
            cx.notify();
            return;
        }

        if usd_str.trim().is_empty() {
            self.form_error = Some("Please enter a USD amount".to_string());
            cx.notify();
            return;
        }

        // Convert USD to 6-decimal amount
        let usd_amount = match usd_to_amount(&usd_str) {
            Ok(amount) => amount,
            Err(e) => {
                self.form_error = Some(format!("Invalid amount: {}", e));
                cx.notify();
                return;
            }
        };

        let Some(session) = self.session.clone() else {
            self.form_error = Some("No active session".to_string());
            cx.notify();
            return;
        };

        self.is_submitting = true;
        cx.notify();

        // Spawn async API call
        cx.spawn(async move |this, cx| {
            let client = EarningsClient::new();

            let result =
                Compat::new(async { client.add_earnings(&session, &song_id, usd_amount).await })
                    .await;

            cx.update(|cx| {
                this.update(cx, |view, cx| {
                    view.is_submitting = false;

                    match result {
                        Ok(()) => {
                            tracing::info!("Earnings added successfully for {}", song_id);
                            // Close the panel - inputs will be cleared when form reopens
                            view.show_add_earnings = false;
                            view.form_error = None;
                            // Set a flag to clear inputs on next open
                            view.clear_form_on_open = true;
                        }
                        Err(EarningsError::SessionExpired(msg)) => {
                            tracing::warn!("Session expired: {}", msg);
                            // Emit session expired event for app to handle logout
                            cx.emit(SessionExpiredEvent { message: msg });
                        }
                        Err(e) => {
                            tracing::warn!("Failed to add earnings: {}", e);
                            view.form_error = Some(e.to_string());
                        }
                    }

                    cx.notify();
                })
            })
        })
        .detach();
    }

    /// Handle CSV upload button click
    fn upload_csv(&mut self, cx: &mut Context<Self>) {
        let Some(session) = self.session.clone() else {
            toast::show_error_async(cx, "No active session".to_string());
            return;
        };

        self.is_importing_csv = true;
        self.csv_import_progress = None;
        cx.notify();

        // Spawn async file dialog and processing
        cx.spawn(async move |this, cx| {
            // Open file dialog
            let file_handle = rfd::AsyncFileDialog::new()
                .add_filter("CSV Files", &["csv"])
                .set_title("Select Earnings CSV")
                .pick_file()
                .await;

            let Some(file_handle) = file_handle else {
                // User cancelled
                cx.update(|cx| {
                    this.update(cx, |view, cx| {
                        view.is_importing_csv = false;
                        view.csv_import_progress = None;
                        cx.notify();
                    })
                })
                .ok();
                return;
            };

            let file_path = file_handle.path().to_path_buf();
            tracing::info!("Selected CSV file: {:?}", file_path);

            // Parse CSV
            let rows = match parse_csv(&file_path) {
                Ok(rows) => rows,
                Err(e) => {
                    let error_msg = e.to_string();
                    cx.update(|cx| {
                        this.update(cx, |view, cx| {
                            view.is_importing_csv = false;
                            view.csv_import_progress = None;
                            cx.notify();
                        })
                    })
                    .ok();
                    cx.update(|cx| {
                        toast::show_error_async(cx, format!("Failed to parse CSV: {}", error_msg));
                    })
                    .ok();
                    return;
                }
            };

            let total = rows.len();
            let mut results: Vec<CsvResult> = Vec::with_capacity(total);
            let mut succeeded = 0usize;
            let mut failed = 0usize;

            // Update progress
            cx.update(|cx| {
                this.update(cx, |view, cx| {
                    view.csv_import_progress = Some((0, total));
                    cx.notify();
                })
            })
            .ok();

            let client = EarningsClient::new();

            // Process each row sequentially
            for (i, row) in rows.into_iter().enumerate() {
                // Convert USD to 6-decimal amount
                let result_msg = match usd_to_amount(&row.amount_usd) {
                    Ok(amount) => {
                        // Call API
                        match Compat::new(async {
                            client
                                .add_earnings(&session, &row.song_id_or_isrc, amount)
                                .await
                        })
                        .await
                        {
                            Ok(()) => {
                                succeeded += 1;
                                "Success".to_string()
                            }
                            Err(EarningsError::SessionExpired(msg)) => {
                                // Session expired - abort processing
                                cx.update(|cx| {
                                    this.update(cx, |view, cx| {
                                        view.is_importing_csv = false;
                                        view.csv_import_progress = None;
                                        cx.emit(SessionExpiredEvent {
                                            message: msg.clone(),
                                        });
                                        cx.notify();
                                    })
                                })
                                .ok();
                                return;
                            }
                            Err(e) => {
                                failed += 1;
                                format!("Error: {}", e)
                            }
                        }
                    }
                    Err(e) => {
                        failed += 1;
                        format!("Error: Invalid amount - {}", e)
                    }
                };

                results.push(CsvResult {
                    row,
                    result: result_msg,
                });

                // Update progress
                cx.update(|cx| {
                    this.update(cx, |view, cx| {
                        view.csv_import_progress = Some((i + 1, total));
                        cx.notify();
                    })
                })
                .ok();
            }

            // Write results CSV
            let output_path = match write_results(&file_path, &results) {
                Ok(path) => path,
                Err(e) => {
                    let error_msg = e.to_string();
                    cx.update(|cx| {
                        this.update(cx, |view, cx| {
                            view.is_importing_csv = false;
                            view.csv_import_progress = None;
                            cx.notify();
                        })
                    })
                    .ok();
                    cx.update(|cx| {
                        toast::show_error_async(
                            cx,
                            format!("Failed to write results: {}", error_msg),
                        );
                    })
                    .ok();
                    return;
                }
            };

            // Complete - show summary
            let _summary = CsvImportSummary {
                total,
                succeeded,
                failed,
                output_path: output_path.clone(),
            };

            cx.update(|cx| {
                this.update(cx, |view, cx| {
                    view.is_importing_csv = false;
                    view.csv_import_progress = None;
                    // Refresh earnings table
                    view.fetch_earnings(cx);
                    cx.notify();
                })
            })
            .ok();

            // Show toast
            if failed > 0 {
                cx.update(|cx| {
                    toast::show_warning_async(
                        cx,
                        format!(
                            "Imported {}/{} earnings ({} failed). Results saved to {}",
                            succeeded,
                            total,
                            failed,
                            output_path
                                .file_name()
                                .unwrap_or_default()
                                .to_string_lossy()
                        ),
                    );
                })
                .ok();
            } else {
                cx.update(|cx| {
                    toast::show_success_async(
                        cx,
                        format!(
                            "Successfully imported {} earnings. Results saved to {}",
                            succeeded,
                            output_path
                                .file_name()
                                .unwrap_or_default()
                                .to_string_lossy()
                        ),
                    );
                })
                .ok();
            }
        })
        .detach();
    }

    /// Fetch earnings from the API
    fn fetch_earnings(&mut self, cx: &mut Context<Self>) {
        if let Some(session) = self.session.clone() {
            self.is_loading_earnings = true;
            cx.notify();

            cx.spawn(async move |this, cx| {
                let client = EarningsClient::new();
                let result = Compat::new(async { client.get_earnings(&session).await }).await;

                cx.update(|cx| {
                    this.update(cx, |view, cx| {
                        view.is_loading_earnings = false;
                        match result {
                            Ok(earnings) => {
                                view.earnings = Some(earnings);
                                view.update_table(cx);
                            }
                            Err(EarningsError::SessionExpired(msg)) => {
                                cx.emit(SessionExpiredEvent { message: msg });
                            }
                            Err(e) => {
                                tracing::error!("Failed to fetch earnings: {}", e);
                            }
                        }
                        cx.notify();
                    })
                })
            })
            .detach();
        }
    }

    /// Delete selected earnings
    fn delete_selected_earnings(&mut self, cx: &mut Context<Self>) {
        if let Some(session) = self.session.clone() {
            // Get selected IDs from the table delegate
            let selected_ids = self.table.read(cx).delegate().get_selected_ids();

            if selected_ids.is_empty() {
                return;
            }

            let count = selected_ids.len();
            self.is_deleting = true;
            self.show_delete_confirmation = false;
            cx.notify();

            cx.spawn(async move |this, cx| {
                let client = EarningsClient::new();
                let result =
                    Compat::new(async { client.delete_earnings(&session, selected_ids).await })
                        .await;

                cx.update(|cx| {
                    this.update(cx, |view, cx| {
                        view.is_deleting = false;
                        match result {
                            Ok(()) => {
                                toast::show_success_async(
                                    cx,
                                    format!("Successfully deleted {} earnings", count),
                                );
                                // Clear selection and refresh data
                                view.table.update(
                                    cx,
                                    |table: &mut TableState<EarningsTableDelegate>, cx| {
                                        table.delegate_mut().deselect_all();
                                        cx.notify();
                                    },
                                );
                                view.fetch_earnings(cx);
                            }
                            Err(EarningsError::SessionExpired(msg)) => {
                                cx.emit(SessionExpiredEvent { message: msg });
                            }
                            Err(e) => {
                                tracing::error!("Failed to delete earnings: {}", e);
                                toast::show_error_async(
                                    cx,
                                    format!("Failed to delete earnings: {}", e),
                                );
                            }
                        }
                        cx.notify();
                    })
                })
            })
            .detach();
        }
    }
}

impl Render for DashboardView {
    fn render(&mut self, _window: &mut Window, cx: &mut Context<Self>) -> impl IntoElement {
        // NOTE: Dynamic column sizing removed to debug table visibility bug
        // The Memo column now uses a fixed width of 350px

        div()
            .size_full()
            .h_flex()
            .bg(colors::bg_primary())
            .relative()
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
                    .child(self.work_area_content(cx)),
            )
            // Add Earnings slide-out panel (conditional)
            .when(self.show_add_earnings, |this| {
                this.child(self.add_earnings_panel(cx))
            })
            // Delete confirmation modal
            .when(self.show_delete_confirmation, |this| {
                let selected_count = self.table.read(cx).delegate().selected_count();
                this.child(
                    div()
                        .absolute()
                        .inset_0()
                        .flex()
                        .items_center()
                        .justify_center()
                        .bg(gpui::Rgba { r: 0.0, g: 0.0, b: 0.0, a: 0.5 })
                        .child(
                            div()
                                .v_flex()
                                .gap_4()
                                .p_6()
                                .rounded_lg()
                                .bg(colors::bg_surface())
                                .border_1()
                                .border_color(colors::border())
                                .shadow_lg()
                                .min_w(px(400.0))
                                .child(
                                    div()
                                        .text_xl()
                                        .font_weight(gpui::FontWeight::BOLD)
                                        .text_color(colors::text_primary())
                                        .child("Confirm Delete")
                                )
                                .child(
                                    div()
                                        .text_color(colors::text_secondary())
                                        .child(format!(
                                            "Are you sure you want to delete {} selected earning{}? This action cannot be undone.",
                                            selected_count,
                                            if selected_count == 1 { "" } else { "s" }
                                        ))
                                )
                                .child(
                                    div()
                                        .h_flex()
                                        .gap_3()
                                        .justify_end()
                                        .child(
                                            Button::new("cancel-delete-btn")
                                                .label("Cancel")
                                                .ghost()
                                                .on_click(cx.listener(|this, _, _window, cx| {
                                                    this.show_delete_confirmation = false;
                                                    cx.notify();
                                                }))
                                        )
                                        .child(
                                            Button::new("confirm-delete-btn")
                                                .label("Delete")
                                                .danger()
                                                .on_click(cx.listener(|this, _, _window, cx| {
                                                    this.delete_selected_earnings(cx);
                                                }))
                                        )
                                )
                        )
                )
            })
    }
}
