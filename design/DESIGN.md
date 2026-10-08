---
name: Present
colors:
  surface: '#131315'
  surface-dim: '#131315'
  surface-bright: '#39393b'
  surface-container-lowest: '#0e0e10'
  surface-container-low: '#1b1b1d'
  surface-container: '#201f21'
  surface-container-high: '#2a2a2c'
  surface-container-highest: '#353437'
  on-surface: '#e5e1e4'
  on-surface-variant: '#c9c4d5'
  inverse-surface: '#e5e1e4'
  inverse-on-surface: '#303032'
  outline: '#928f9f'
  outline-variant: '#474553'
  surface-tint: '#c7bfff'
  primary: '#c7bfff'
  on-primary: '#2c0e94'
  primary-container: '#8f80fa'
  on-primary-container: '#25008c'
  inverse-primary: '#5c4bc3'
  secondary: '#cac1e8'
  on-secondary: '#312c4b'
  secondary-container: '#4a4565'
  on-secondary-container: '#bbb3da'
  tertiary: '#f9bc45'
  on-tertiary: '#422c00'
  tertiary-container: '#bd8708'
  on-tertiary-container: '#392600'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#e5deff'
  primary-fixed-dim: '#c7bfff'
  on-primary-fixed: '#180065'
  on-primary-fixed-variant: '#4330aa'
  secondary-fixed: '#e6deff'
  secondary-fixed-dim: '#cac1e8'
  on-secondary-fixed: '#1c1735'
  on-secondary-fixed-variant: '#484363'
  tertiary-fixed: '#ffdea9'
  tertiary-fixed-dim: '#f9bc45'
  on-tertiary-fixed: '#271900'
  on-tertiary-fixed-variant: '#5e4100'
  background: '#131315'
  on-background: '#e5e1e4'
  surface-variant: '#353437'
typography:
  display-lg:
    fontFamily: Manrope
    fontSize: 44px
    fontWeight: '700'
    lineHeight: 52px
    letterSpacing: -0.03em
  display-sm:
    fontFamily: Manrope
    fontSize: 36px
    fontWeight: '700'
    lineHeight: 44px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Manrope
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Manrope
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.015em
  headline-sm:
    fontFamily: Manrope
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
    letterSpacing: -0.01em
  body-lg:
    fontFamily: Manrope
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: -0.01em
  body-md:
    fontFamily: Manrope
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0em
  body-sm:
    fontFamily: Manrope
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
    letterSpacing: 0.01em
  label-lg:
    fontFamily: Manrope
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0em
  label-md:
    fontFamily: Manrope
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.01em
  label-sm:
    fontFamily: Manrope
    fontSize: 11px
    fontWeight: '600'
    lineHeight: 14px
    letterSpacing: 0.04em
  numeric-stat:
    fontFamily: Manrope
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 36px
    letterSpacing: -0.03em
  numeric-compact:
    fontFamily: Manrope
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 24px
    letterSpacing: -0.02em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  margin: 1.25rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

The design system embodies a calm, precise, and quiet confidence tailored for university students balancing rigorous academic schedules. Rejecting the noisy gamification, loud badge systems, and neon candy gradients prevalent in campus software, this interface treats personal attendance as vital metrics—drawing philosophical inspiration from Apple Health and Linear. The emotional objective is composure: replacing the chronic anxiety of academic thresholds with clarity, agency, and grounded awareness.

The visual style is **Tactile Minimalism**. It relies on deep monochromatic surfaces, meticulously balanced optical hierarchy, deliberate typographic rhythm, and whisper-quiet structural borders. Interface interactions simulate physical instruments—solid toggles, tactile haptics, and instant state feedback. The visual atmosphere is understated and respectful of user focus: metrics are presented with analytical sobriety, surfaces stay matte and controlled, and communicative color appears exclusively to signify structural state changes or attendance margins.

## Colors

The palette operates in a dedicated, refined dark mode designed for battery efficiency on OLED displays and seamless operation in dim lecture halls or late-night study spaces.

- **Canvas & Surfaces**: Base background sits at `#101012` (near-black), creating an expansive, non-glare foundation. Flat container surfaces rest at `#18181B` (Surface Primary), stepping up to `#202024` (Surface Elevated) for modal bottom sheets, floating navigation shelves, and interactive states.
- **Structural Separation**: Outlines and separators utilize `#2A2A2F` (Border Subtle), applied as single-pixel hairline boundaries to establish clean geometric containment without harsh visual contrast.
- **Typography**: Information hierarchy is governed by strict tonal tiering: `#F5F4F7` (Text Primary) for bold metrics and active headers; `#A6A4AC` (Text Secondary) for course identifiers, instructors, and body copy; and `#6F6D75` (Text Muted) for auxiliary metadata, timestamps, and inactive iconography.
- **Accents**: `#8B7CF6` (Accent Violet) delivers targeted brand intention for primary triggers and focus markers, paired with `#26213F` (Soft Violet Background) for non-disruptive selection states and active day pills.
- **State Semantics**: Functional status is isolated and strictly semantic. `#63C48B` (Positive Green) communicates safe attendance buffers (e.g., ≥85%); `#E5AD5C` (Warning Amber) alerts to thinning safety margins (e.g., 75%–84%); and `#E36D76` (Critical Red) warns of statutory attendance violations (e.g., <75%). These colors are never decorative; they serve solely as diagnostic signals.

## Typography

Manrope drives the entire system across display, body, and UI labels. Its geometric foundations and humanist details allow both clinical data precision and warmth.

- **Monospace Alignment**: Apply OpenType tabular figures (`font-variant-numeric: tabular-nums; tnum`) to all percentage metrics, class counters, and timetable displays to prevent visual jitter when attendance values increment.
- **Letter Spacing**: Maintain slightly compressed tracking on display scales (`-0.03em` down to `-0.015em`) for a dense, editorial silhouette. Over-index tracking on small labels (`label-sm`: `+0.04em`) when rendering uppercase status indicators or course code tags.
- **Hierarchical Anchoring**: Headline levels prioritize heavy weights (Bold/SemiBold) with conservative vertical line-heights, ensuring cards remain compact without sacrificing scannability.

## Layout & Spacing

The layout is constructed on an uncompromising 8-point base grid (subdivided by 4pt for micro-alignments such as chip padding and icon-to-label inline gaps).

- **Margins & Safe Boundaries**: Root canvas margins are locked to `20px` (`1.25rem`), preserving a structural, edge-to-edge breathing room tailored for contemporary Android gestures and bezel-less displays.
- **Vertical Rhythm**: Section assemblies observe `24px` (`1.5rem`) gaps between topical clusters (e.g., jumping from Today's Schedule to Master Course List). Intra-card element stacks conform strictly to `8px` (`0.5rem`) or `12px` steps.
- **Component Densities**: Form components and standard interactive modules hold a fixed height of `48px` (compact) or `56px` (comfortable) to provide immediate touch targets exceeding the standard 48dp Android accessibility guideline.
- **Adaptive Reflow**: Single-column vertical stacking is mandatory across mobile screen widths (<600dp). For large foldable unfolded screens or tablet states (≥600dp), shift to an asymmetric 2-column view (38% left diagnostic overview, 62% right modular class roster) anchored by a persistent `16px` gutter.

## Elevation & Depth

Visual hierarchy is constructed through **Tonal Stacking and Hairline Inset Delimitation** rather than diffuse, artificial drop shadows. Shadows in standard dark interfaces often create muddy muddy contrast gradients; here, clarity is achieved through pure material layering.

- **Ground Level (z:0)**: Canvas at `#101012`. Non-interactive background backdrop.
- **Base Level (z:1)**: Structural content cards sit at `#18181B`. Outlined with a continuous, razor-sharp 1px border of `#2A2A2F`. Surface elevation is felt, not cast.
- **Elevated Level (z:2)**: Floating action controls, active modal bottom sheets, contextual popovers, and sticky date selectors sit at `#202024` with an intensified hairline border (`#3F3F46` or `#2A2A2F` depending on priority). An ultra-subtle, non-directional dark ambient contact drop (`0px 8px 24px rgba(0, 0, 0, 0.45)`) may be used exclusively for floating surfaces to softly decouple them from cards underneath.
- **Active Touch States**: When an interactive tile or class item is depressed, its surface subtly transitions to `#242429` with no transform shift, pairing with system haptics for an authentic, calibrated mechanical feeling.

## Shapes

The geometric signature balances friendly contemporary curves with structural rigor. Curvature explicitly denotes scale and purpose:

- **Hero & Summary Containers**: Large top-level structural metric banners and daily status headers use a large `24px` radius to gracefully frame high-density information.
- **Standard Course Cards & Modals**: Everyday course listings, attendance breakdown cards, and bottom sheet containers utilize a standardized `18px` radius.
- **Buttons & Interactive Blocks**: Primary actions, inputs, and form controls hold a uniform `12px` to `14px` radius, anchoring the finger.
- **Tags, Status Badges & Date Pickers**: Micro pills and calendar date selectors adopt fully rounded `9999px` capsular forms to clearly differentiate metadata from tappable structural cards.

## Components

### Buttons
- **Primary Action**: Surface colored in `#8B7CF6` with text in `#101012` (Bold, `label-lg`). Height 52px, border-radius 14px. No drop shadow. Active press states apply an opacity transition to `0.9` paired with an instantaneous micro-scale compression (`scale(0.985)`).
- **Secondary / Ghost Action**: Surface in `#18181B` with 1px outline in `#2A2A2F`. Text in `#F5F4F7`. On touch, surface shifts to `#202024`.
- **Destructive Action**: Surface in transparent fill, bordered with a subtle `rgba(227, 109, 118, 0.35)`. Text in `#E36D76`.

### Cards & Course Tiles
- **Standard Attendance Tile**: Surface `#18181B`, border 1px solid `#2A2A2F`, radius 18px, internal padding 16px. Internal hierarchy hosts Course Code + Name (top), Attendance Ratio with Tabular Percentage Metric (right-aligned or large numeric stat), and bottom contextual margin alert (e.g., *"Can miss 2 more classes"* in `#63C48B` or *"Must attend next 3 classes"* in `#E36D76`).
- **Absence Log Item**: Nested compact sub-cards inside class detail screens; surface `#202024`, border-radius 12px, padding 12px 14px.

### Badges & Chips
- **Status Indicator Pill**: Height 26px, padding 4px 10px, radius 9999px. Composed of a low-opacity contextual tint (e.g., `#63C48B` at 12% alpha) containing an inline 6px solid circular status dot alongside a 11px uppercase label (`label-sm`).

### Checkboxes, Steppers & Attendance Triggers
- **Tactile Quick-Log Segment**: Three-part inline attendance logger for each class session (Present / Absent / Cancelled). Present toggles to `#63C48B` (15% background tint, solid green icon), Absent activates `#E36D76` (15% tint, solid red icon). 
- **Checkboxes**: 20px squares with 6px rounded corners, 1.5px border `#2A2A2F`. Checked state fills with `#8B7CF6`, presenting a stark `#101012` checkmark icon.

### Form Inputs
- **Input Fields**: Height 52px, surface `#18181B`, border 1px solid `#2A2A2F`, radius 12px, padding 0 16px. Text `#F5F4F7` (`body-md`), placeholder text `#6F6D75`. Focused state replaces border with `#8B7CF6` (1.5px) without glowing drop shadows.

### Icons
- **Iconography System**: Lucide-style rounded outlines, strictly rendered with a 1.75px to 2px stroke width, matching the active text color context (`#F5F4F7` or `#A6A4AC`). Bounding box standard is 20px or 24px.