# NEWM Admin Design Guide

Design specifications based on NEWM Studio's visual language.

---

## Color Palette

### Backgrounds
| Name | Hex | Usage |
|------|-----|-------|
| Primary Background | `#000000` | Main app background |
| Surface | `#1a1a1a` | Cards, sidebar |
| Surface Elevated | `#2a2a2a` | Input fields, hover states |
| Surface Border | `#3a3a3a` | Subtle borders |

### Accent Colors
| Name | Hex | Usage |
|------|-----|-------|
| Gradient Start | `#C341F0` | Primary button gradient (bottom-left) |
| Gradient End | `#F53C69` | Primary button gradient (top-right) |
| Secondary Text Accent | `#DC3CAA` | Secondary button text |
| Success | `#22c55e` | Checkmarks, success states |
| Warning | `#eab308` | Pending, awaiting states |
| Error | `#ef4444` | Errors, delete actions |

### Text Colors
| Name | Hex | Usage |
|------|-----|-------|
| Primary Text | `#ffffff` | Headings, important text |
| Secondary Text | `#a1a1aa` | Labels, descriptions |
| Muted Text | `#71717a` | Placeholders, hints |
| Accent Text | `#d946ef` | Links, highlighted text |

---

## Typography

### Font Family
- **Primary**: System UI / Inter / SF Pro (sans-serif stack)

### Font Sizes
| Name | Size | Weight | Usage |
|------|------|--------|-------|
| Page Title | 24px | Bold (700) | "ADD NEW TRACK", "RELEASES" |
| Section Header | 14px | Semibold (600) | "BASIC DETAILS", "COLLABORATORS" |
| Body | 14px | Regular (400) | General text |
| Label | 12px | Medium (500) | Form labels, column headers |
| Caption | 11px | Regular (400) | Helper text, "OPTIONAL" tags |

### Text Transform
- Section headers: **UPPERCASE**
- Navigation items: **Sentence case**

---

## Layout

### Sidebar (Left Panel)
- **Width**: 200px fixed
- **Structure**:
  1. User avatar (circular, ~80px)
  2. Username
  3. Navigation sections with headers
  
### Navigation Sections
```
MY CAREER
  ├── Releases
  └── Collaborators

MY PERFORMANCE
  └── Wallet

MY SETTINGS
  ├── Profile
  └── Settings

SUPPORT
  ├── FAQ
  ├── Ask the Community
  └── Support
```

### Main Content Area
- **Padding**: 32px
- **Max width**: 800-1000px for forms

---

## Components

### Buttons

#### Primary Button (Gradient)
```css
background: linear-gradient(to top right, #C341F0, #F53C69);
color: #ffffff;
border-radius: 4px;
padding: 8px 24px;
font-weight: 500;
```

#### Secondary Button
```css
background: #000000;
border: 1px solid #1c1c1e;
color: #DC3CAA;
border-radius: 2px;
padding: 8px 24px;
```

#### Ghost Button
```css
background: transparent;
border: 1px solid #3a3a3a;
color: #ffffff;
border-radius: 4px;
padding: 8px 24px;
```

### Input Fields

#### Text Input
```css
background: #1a1a1a;
border: 1px solid #3a3a3a;
border-radius: 4px;
color: #ffffff;
padding: 12px 16px;
```

**Placeholder Text:**
```css
color: #71717a;  /* Muted gray */
```

**Note:** Labels appear as placeholder text inside the input, not as separate elements above.

#### Dropdown/Select
- Same styling as text input
- Chevron icon on right
- Options list with same background

### File Upload Zone
```css
border: 1px dashed #3a3a3a;
border-radius: 8px;
background: transparent;
padding: 32px;
text-align: center;
```
- Icon centered
- "Drag and drop or browse your file"
- File format hints below

### Toggle Switch
- Off: Dark gray track, lighter thumb
- On: Magenta track, white thumb
- Size: 40px × 20px

### Cards/Sections
```css
background: #1a1a1a;
border: 1px solid #2a2a2a;
border-radius: 8px;
padding: 24px;
```

---

## Data Tables

### Structure
- No visible outer border
- Row separator: 1px solid #2a2a2a
- Column headers: Uppercase, muted text, 12px

### Row States
- Default: transparent background
- Hover: subtle #1a1a1a background
- Selected: slight highlight

### Action Menu (Three Dots)
- Icon button aligned right
- Dropdown with: View/Edit, Delete (red text)

### Status Indicators
| Status | Color | Icon |
|--------|-------|------|
| Distributed/Released | Green | ✓ checkmark |
| Undistributed | White | × |
| Pending/In Review | Yellow | ⏳ clock |
| Error/Declined | Red | ⚠ warning |

### Pagination
- Compact style at bottom right
- Current page highlighted with background
- Format: "Showing 1 to 10 of 30"

---

## Icons

### Style
- Outline/stroke style (not filled)
- 16-20px size for UI elements
- Muted color (#71717a) by default
- White on hover/active

### Common Icons
- Back arrow: ← (in circle)
- Add/Plus: +
- Delete: Trash
- Edit: Pencil
- Menu: Three vertical dots
- Upload: Cloud with arrow
- Drag handle: Six dots (⋮⋮)

---

## Spacing Scale

| Name | Value |
|------|-------|
| xs | 4px |
| sm | 8px |
| md | 16px |
| lg | 24px |
| xl | 32px |
| 2xl | 48px |

---

## Specific Patterns

### Form Layout
1. Section header (uppercase, bold)
2. Form fields in 2-column grid where appropriate
3. "OPTIONAL" label aligned right for optional fields
4. Generous vertical spacing (24px) between sections

### Two-Panel Layout (Create/Edit)
- Left: Image/file upload + metadata
- Right: List/action items
- Vertical divider line between panels

### Page Header
- Back button (← in circle) + Page title
- Delete button (trash icon) top right for edit pages

---

## Example: Login Screen Styling

```rust
// Background
div().bg(colors::bg_primary())

// Card
div()
    .bg(colors::bg_surface())
    .border_1()
    .border_color(colors::border())
    .rounded_lg()
    .p_8()

// Input Fields
Input::new(&email_state)
    .bg(colors::bg_surface())
    .border_color(colors::border())
    .text_color(colors::text_primary())
    .placeholder("Email")

Input::new(&password_state)
    .bg(colors::bg_surface())
    .border_color(colors::border())
    .text_color(colors::text_primary())
    .placeholder("Password")
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
                this.password_state.update(cx, |state: &mut InputState, cx| {
                    state.set_masked(masked, window, cx);
                });
                cx.notify();
            })),
    )

// Primary Button with Gradient
gradient_button("Log In", move |_, _window, cx| {
    // click handler
})
```

---

## Reference Screenshots

````carousel
![Add New Track Form](file:///home/westbam/.gemini/antigravity/brain/748d3d81-90fb-43be-9276-77d0f1cc9056/uploaded_image_0_1767735756465.png)
<!-- slide -->
![Releases List](file:///home/westbam/.gemini/antigravity/brain/748d3d81-90fb-43be-9276-77d0f1cc9056/uploaded_image_1_1767735756465.png)
<!-- slide -->
![Create Release](file:///home/westbam/.gemini/antigravity/brain/748d3d81-90fb-43be-9276-77d0f1cc9056/uploaded_image_2_1767735756465.png)
<!-- slide -->
![Add Track](file:///home/westbam/.gemini/antigravity/brain/748d3d81-90fb-43be-9276-77d0f1cc9056/uploaded_image_3_1767735756465.png)
````
