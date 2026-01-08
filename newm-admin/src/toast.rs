//! Toast Notification Helpers
//!
//! Convenience functions for showing toast notifications to users.
//! Uses gpui-component's Notification system.

use gpui::*;
use gpui_component::WindowExt;
use gpui_component::notification::Notification;

/// Show a success toast (auto-dismisses after ~5 seconds)
#[allow(dead_code)]
pub fn show_success(window: &mut Window, cx: &mut App, message: impl Into<SharedString>) {
    window.push_notification(Notification::success(message).autohide(true), cx);
}

/// Show a warning toast (requires manual dismissal)
#[allow(dead_code)]
pub fn show_warning(window: &mut Window, cx: &mut App, message: impl Into<SharedString>) {
    window.push_notification(Notification::warning(message).autohide(false), cx);
}

/// Show an error toast (requires manual dismissal)
#[allow(dead_code)]
pub fn show_error(window: &mut Window, cx: &mut App, message: impl Into<SharedString>) {
    window.push_notification(Notification::error(message).autohide(false), cx);
}

/// Show an info toast (auto-dismisses after ~5 seconds)
pub fn show_info(window: &mut Window, cx: &mut App, message: impl Into<SharedString>) {
    window.push_notification(Notification::info(message).autohide(true), cx);
}

// =============================================================================
// Async-friendly versions (no Window ref needed)
// =============================================================================

fn with_top_window<F>(cx: &mut App, f: F)
where
    F: FnOnce(&mut Window, &mut App),
{
    let windows = cx.windows();
    if let Some(any_window) = windows.first() {
        // AnyWindow::update takes a closure with 3 args: (root_view, window, cx)
        // We ignore the root view since we just need window/cx for notifications
        any_window
            .update(cx, |_root, window, cx| f(window, cx))
            .ok();
    }
}

/// Show a success toast from async context (auto-dismisses after ~5 seconds)
pub fn show_success_async(cx: &mut App, message: impl Into<SharedString> + Clone) {
    let msg = message.into();
    with_top_window(cx, |window, cx| {
        window.push_notification(Notification::success(msg).autohide(true), cx);
    });
}

/// Show a warning toast from async context (requires manual dismissal)
pub fn show_warning_async(cx: &mut App, message: impl Into<SharedString> + Clone) {
    let msg = message.into();
    with_top_window(cx, |window, cx| {
        window.push_notification(Notification::warning(msg).autohide(false), cx);
    });
}

/// Show an error toast from async context (requires manual dismissal)
pub fn show_error_async(cx: &mut App, message: impl Into<SharedString> + Clone) {
    let msg = message.into();
    with_top_window(cx, |window, cx| {
        window.push_notification(Notification::error(msg).autohide(false), cx);
    });
}

/// Show an info toast from async context (auto-dismisses after ~5 seconds)
#[allow(dead_code)]
pub fn show_info_async(cx: &mut App, message: impl Into<SharedString> + Clone) {
    let msg = message.into();
    with_top_window(cx, |window, cx| {
        window.push_notification(Notification::info(msg).autohide(true), cx);
    });
}
