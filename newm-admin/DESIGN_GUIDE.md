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

### Using Icons from gpui-component

The `gpui-component` library provides a curated set of icons via the `IconName` enum. 

**Full documentation:** https://longbridge.github.io/gpui-component/docs/components/icon

#### Basic Usage

```rust
use gpui_component::icon::{Icon, IconName};

// In your element tree
Icon::new(IconName::Search).size(px(16.0))

// With custom color
Icon::new(IconName::Check)
    .size(px(16.0))
    .text_color(colors::success())

// In a button
Button::new("my-button")
    .icon(Icon::new(IconName::Plus).size(px(16.0)))
    .label("Add Item")
```

#### Available IconName Variants

| Category | Icons |
|----------|-------|
| **Navigation** | `ArrowUp`, `ArrowDown`, `ArrowLeft`, `ArrowRight`, `ChevronUp`, `ChevronDown`, `ChevronLeft`, `ChevronRight`, `ChevronsUpDown` |
| **Actions** | `Check`, `Close`, `Plus`, `Minus`, `Copy`, `Delete`, `Search`, `Replace`, `Maximize`, `Minimize`, `WindowRestore` |
| **Files & Folders** | `File`, `Folder`, `FolderOpen`, `FolderClosed`, `BookOpen`, `Inbox` |
| **UI Elements** | `Menu`, `Settings`, `Settings2`, `Ellipsis`, `EllipsisVertical`, `Eye`, `EyeOff`, `Bell`, `Info` |
| **Social & External** | `GitHub`, `Globe`, `ExternalLink`, `Heart`, `HeartOff`, `Star`, `StarOff`, `ThumbsUp`, `ThumbsDown` |
| **Status & Alerts** | `CircleCheck`, `CircleX`, `TriangleAlert`, `Loader`, `LoaderCircle` |
| **Panels & Layout** | `PanelLeft`, `PanelRight`, `PanelBottom`, `PanelLeftOpen`, `PanelRightOpen`, `PanelBottomOpen`, `LayoutDashboard`, `Frame` |
| **Users & Profile** | `User`, `CircleUser`, `Bot` |
| **Other** | `Calendar`, `Map`, `Palette`, `Inspector`, `Sun`, `Moon`, `Building2`, `ChartPie`, `Undo` |

#### Icon Sizes

```rust
Icon::new(IconName::Search).xsmall()     // size_3 (12px)
Icon::new(IconName::Search).small()      // size_3p5 (14px)
Icon::new(IconName::Search).medium()     // size_4 (16px) - default
Icon::new(IconName::Search).large()      // size_6 (24px)
Icon::new(IconName::Search).size(px(20.0)) // custom size
```

---

### Custom SVG Icons

When a suitable icon is not available in `IconName`, you can create custom SVG icons.

#### Step 1: Create the SVG file

Place your SVG file in the `assets/` folder:
```
newm-admin/assets/
├── NEWM_Logo.png
├── NEWM_Logo.svg
├── refresh.svg      ← Custom icon
└── upload.svg       ← Custom icon
```

**SVG Requirements:**
- Monochrome (single color)
- Use `currentColor` for fill/stroke to inherit text color
- 24×24 viewBox recommended
- Outline/stroke style (not filled) for consistency

**Example SVG (`refresh.svg`):**
```xml
<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" 
     fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" 
     stroke-linejoin="round">
  <path d="M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8"/>
  <path d="M21 3v5h-5"/>
  <path d="M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16"/>
  <path d="M3 21v-5h5"/>
</svg>
```

#### Step 2: Use the SVG in code

```rust
use gpui::{img, Arc, Image, ImageFormat};

// Include the SVG bytes at compile time
const REFRESH_SVG: &[u8] = include_bytes!("../assets/refresh.svg");
const UPLOAD_SVG: &[u8] = include_bytes!("../assets/upload.svg");

// Render as an image element
img(Arc::new(Image::from_bytes(ImageFormat::Svg, REFRESH_SVG.to_vec())))
    .size(px(16.0))

// In a button
Button::new("refresh-btn")
    .child(
        img(Arc::new(Image::from_bytes(ImageFormat::Svg, REFRESH_SVG.to_vec())))
            .size(px(16.0))
    )
    .tooltip("Refresh")
    .ghost()
```

#### Icon Sources

For finding SVG icons:
- [Lucide Icons](https://lucide.dev/) - The icon set used by gpui-component
- [Heroicons](https://heroicons.com/) - Tailwind's icon set
- [Feather Icons](https://feathericons.com/) - Simple, clean icons

### Style Guidelines

- **Size**: 16-20px for UI elements
- **Default color**: `#71717a` (muted)
- **Hover/Active color**: `#ffffff` (white)
- **Success**: `#22c55e` (green)
- **Error**: `#ef4444` (red)

### Common Icon Mappings

| Action | Icon |
|--------|------|
| Back | `ArrowLeft` |
| Add/Create | `Plus` |
| Delete | `Close` or `Delete` |
| Edit | Custom pencil SVG |
| Menu | `EllipsisVertical` |
| Upload | Custom SVG (`assets/upload.svg`) - not in library |
| Refresh | Custom SVG (`assets/refresh.svg`) - not in library |
| Search | `Search` |
| Settings | `Settings` |
| Success | `Check` or `CircleCheck` |
| Error | `Close` or `CircleX` |
| Warning | `TriangleAlert` |
| Info | `Info` |

> **Note:** Some common icons like `Upload`, `Refresh`, and `Trash` are not available in the current gpui-component version. For these, use custom SVGs from Lucide or similar icon sets.

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
