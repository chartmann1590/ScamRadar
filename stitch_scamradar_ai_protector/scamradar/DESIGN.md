---
name: ScamRadar
colors:
  surface: '#fbf8ff'
  surface-dim: '#dad9e5'
  surface-bright: '#fbf8ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f4f2ff'
  surface-container: '#eeedf9'
  surface-container-high: '#e8e7f3'
  surface-container-highest: '#e2e1ed'
  on-surface: '#1a1b24'
  on-surface-variant: '#444655'
  inverse-surface: '#2f3039'
  inverse-on-surface: '#f1effc'
  outline: '#747686'
  outline-variant: '#c4c5d8'
  surface-tint: '#2b4edf'
  primary: '#0033c6'
  on-primary: '#ffffff'
  primary-container: '#2d4fe0'
  on-primary-container: '#d3d8ff'
  inverse-primary: '#b9c3ff'
  secondary: '#006c49'
  on-secondary: '#ffffff'
  secondary-container: '#6cf8bb'
  on-secondary-container: '#00714d'
  tertiary: '#663f00'
  on-tertiary: '#ffffff'
  tertiary-container: '#875400'
  on-tertiary-container: '#ffd29f'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#dee1ff'
  primary-fixed-dim: '#b9c3ff'
  on-primary-fixed: '#001158'
  on-primary-fixed-variant: '#0032c3'
  secondary-fixed: '#6ffbbe'
  secondary-fixed-dim: '#4edea3'
  on-secondary-fixed: '#002113'
  on-secondary-fixed-variant: '#005236'
  tertiary-fixed: '#ffddb8'
  tertiary-fixed-dim: '#ffb95f'
  on-tertiary-fixed: '#2a1700'
  on-tertiary-fixed-variant: '#653e00'
  background: '#fbf8ff'
  on-background: '#1a1b24'
  surface-variant: '#e2e1ed'
typography:
  headline-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  headline-sm:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  title-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-lg:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.1px
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.5px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  margin-main: 20px
  gutter-grid: 16px
  stack-sm: 8px
  stack-md: 16px
  stack-lg: 24px
  stack-xl: 32px
  touch-target: 48px
---

## Brand & Style

The design system is engineered to project unwavering authority and technical sophistication while remaining approachable for everyday users. The brand personality is **vigilant, transparent, and serene**. It aims to replace the anxiety associated with digital threats with a sense of quiet confidence through on-device protection.

The visual style is a refined interpretation of **Material 3 Expressive**. It utilizes generous whitespace to reduce cognitive load and focuses on high-clarity information architecture. The aesthetic avoids complex gradients or decorative clutter, opting for flat surfaces, precise geometry, and soft, natural shadows to create a UI that feels physically grounded and systematically reliable.

## Colors

The palette is anchored by **Deep Trust Blue**, a primary color that signals stability and professional security. Success states and "Safe" designations utilize **Emerald**, chosen for its high visibility and positive psychological reinforcement. Warnings are handled by **Amber**, while active threats utilize **Coral** to provide a distinct, urgent contrast without being overly aggressive.

This design system supports both light and dark modes. In light mode, the background uses a cool-tinted slate to reduce glare, while the dark mode utilizes a deep navy-charcoal to maintain depth and contrast for nighttime vigilance. High-emphasis text always maintains a minimum contrast ratio of 7:1 against its respective surface.

## Typography

**Inter** is the sole typeface, leveraged for its exceptional legibility on mobile displays and its neutral, modern character. 

- **Headlines:** Use Bold (700) weight with slight negative letter-spacing to create a strong, "locked-in" visual presence for status summaries.
- **Titles and Labels:** Utilize Semi-Bold (600) to ensure clear hierarchy in data-heavy views or list headers.
- **Body Text:** Uses Regular (400) weight. Line heights are set generously (1.5x) to ensure that technical descriptions remain readable for all user demographics.

## Layout & Spacing

This design system is optimized for a **360dp width Android viewport**. It utilizes a 4-column fluid grid for mobile, with a consistent **20px side margin** to provide visual breathing room. 

The spacing rhythm is based on an **8px linear scale**. 
- Vertical "stack" spacing of 24px is used between distinct content groups (cards).
- 16px spacing is used between elements within a card.
- 8px spacing is used for related labels and icons.

On larger devices (tablets), the layout should transition to an 8-column grid, centering the content at a maximum width of 600dp to maintain scanning efficiency.

## Elevation & Depth

Hierarchy is established through **Tonal Layers** supplemented by **Ambient Shadows**. 

- **Level 0 (Background):** The base canvas (Slate-50 or Navy-900).
- **Level 1 (Cards):** Surface color with a subtle 1px inner stroke (low opacity) and a soft, diffused shadow (Y: 4px, Blur: 12px, 4% Opacity). This creates a "resting" elevation.
- **Level 2 (Active/Floating):** Higher elevation with a more pronounced shadow (Y: 8px, Blur: 20px, 8% Opacity). Used for bottom sheets and active threat alerts.

Shadows should be slightly tinted with the Primary Blue to maintain color harmony across the system.

## Shapes

The shape language is defined by significant corner radii to evoke friendliness and modern Android conventions. 

- **Primary Cards:** 20dp radius.
- **Main Action Buttons:** 28dp radius (fully pill-shaped for standard button heights).
- **Small Components:** (Chips, Input fields) 12dp radius.

Avoid sharp corners entirely; even progress bars and selection indicators should utilize a minimum of 4dp rounding to remain consistent with the "soft" visual narrative.

## Components

### Buttons
- **Primary CTA:** Solid Deep Trust Blue, 28dp radius. Can feature a subtle radial gradient (Primary to a slightly lighter blue) to denote interactivity.
- **Secondary:** Tonal surface (Light Blue-Grey) with Primary colored text.
- **Danger Button:** Solid Coral background, white text, used sparingly for "Delete" or "Block" actions.

### Cards
- Standard containers for scan results. Use a white/surface-navy background with the 20dp radius. 
- Status-specific cards (e.g., "Scam Detected") may use a subtle 2px left-accent border in the semantic color (Coral/Amber).

### Input Fields
- Outlined style with a 12dp radius. 
- In active state, the border thickens to 2px in Deep Trust Blue.

### Iconography
- Use **Phosphor-style icons** (Rounded join/caps). 
- Icons in headers or status indicators should be 24dp or 32dp. 
- Supportive icons in lists should be 20dp.

### Scanning Radar (Custom Component)
- A central diagnostic element using concentric circles with decreasing opacity. 
- Animation should be a smooth, continuous pulse rather than a sharp sweep to maintain the "Calm" brand pillar.