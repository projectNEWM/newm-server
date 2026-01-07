//! NEWM Studio Design Colors
//!
//! Shared color palette based on NEWM Studio's visual language.
//! See DESIGN_GUIDE.md for full specifications.

use gpui::Rgba;
use gpui::rgb;

// Backgrounds
pub fn bg_primary() -> Rgba {
    rgb(0x000000)
} // Pure black
pub fn bg_surface() -> Rgba {
    rgb(0x1a1a1a)
} // Cards, sidebar
pub fn bg_elevated() -> Rgba {
    rgb(0x2a2a2a)
} // Input fields
pub fn border() -> Rgba {
    rgb(0x3a3a3a)
} // Subtle borders

// Button Gradient
pub fn gradient_start() -> Rgba {
    rgb(0xC341F0)
} // Bottom-left
pub fn gradient_end() -> Rgba {
    rgb(0xF53C69)
} // Top-right

// Secondary Button
#[allow(dead_code)]
pub fn secondary_border() -> Rgba {
    rgb(0x1c1c1e)
}
#[allow(dead_code)]
pub fn secondary_text() -> Rgba {
    rgb(0xDC3CAA)
}

// Text
pub fn text_primary() -> Rgba {
    rgb(0xffffff)
} // Headings
pub fn text_secondary() -> Rgba {
    rgb(0xa1a1aa)
} // Labels, descriptions
pub fn text_muted() -> Rgba {
    rgb(0x71717a)
} // Placeholders

// Status
pub fn error() -> Rgba {
    rgb(0xef4444)
} // Error text
pub fn success() -> Rgba {
    rgb(0x22c55e)
} // Success text
