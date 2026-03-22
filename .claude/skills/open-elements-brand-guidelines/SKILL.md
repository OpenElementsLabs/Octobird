---
name: open-elements-brand-guidelines
license: Apache-2.0
metadata:
  source: https://github.com/open-elements/claude-base
  author: Open Elements
description: Applies the official brand colors and typography of Open Elements to any sort of artifact that may benefit from having the look-and-feel of Open Elements. Use it when brand colors or style guidelines, visual formatting, or company design standards apply.
---

# Open Elements Brand Styling

## Overview

To access the official brand identity and style resources of Open Elements, use this skill.

## Brand Guidelines

### Colors

**Main Colors:**

- Dark: `#020144` - Can be used as backgrounds in header footer or for example as headline text color.
  In diagrams / technical illustrations often used for note texts or arrows.
- Black: `#000000` - Normal text color on bright backgrounds
- White: `#ffffff` - Normal background color
- Mid Gray: `#b0aea5` - Secondary elements
- Light Gray: `#e8e6dc` - Subtle backgrounds
- Primary Green: `#5CBA9E` - Can be used in texts, for highlighting or as background color for example in diagrams
- Primary Red: `#E63277` - Can be used in texts, for highlighting or as background color for example in diagrams

**Accent Colors:**

- Blue: `#5DB9F5` - Primary accent
- Yellow: `#F1E34B` - Secondary accent

**Light variants:**
(green) #BEE3D8, (red) #F5ADC9, (blue) #BEE3FB, (yellow) #F9F4B7

**Lighter variants:**
(green) #DEF1EC, (red) #FAD6E4, (blue) #DFF1FD, (yellow) #FCF9DB

**Dark variants:**
(green) #3E9279, (red) #BB1756, (blue) #2496EF, (yellow) #DCCB12

### CSS Custom Properties

When using the brand colors in web projects, define them as CSS custom properties:

```css
:root {
  --oe-dark: #020144;
  --oe-black: #000000;
  --oe-white: #ffffff;
  --oe-gray-mid: #b0aea5;
  --oe-gray-light: #e8e6dc;
  --oe-green: #5CBA9E;
  --oe-green-light: #BEE3D8;
  --oe-green-lighter: #DEF1EC;
  --oe-green-dark: #3E9279;
  --oe-red: #E63277;
  --oe-red-light: #F5ADC9;
  --oe-red-lighter: #FAD6E4;
  --oe-red-dark: #BB1756;
  --oe-blue: #5DB9F5;
  --oe-blue-light: #BEE3FB;
  --oe-blue-lighter: #DFF1FD;
  --oe-blue-dark: #2496EF;
  --oe-yellow: #F1E34B;
  --oe-yellow-light: #F9F4B7;
  --oe-yellow-lighter: #FCF9DB;
  --oe-yellow-dark: #DCCB12;
}
```

### Typography

- **Headings**: Montserrat (or Lato)
- **Body Text**: Lato
- **Source Code**: Source_Code_Pro
- In diagrams notes can be written in Permanent_Marker
- **Note**: Fonts should be pre-installed in your environment for best results.
  All fonts are available in the [Google Fonts](https://fonts.google.com/) library.

### Logo

The open elements logo is available in the folder of this skill.
It is provided in PNG and SVG format.
All PNGs have a transparent background.
If possible, use the SVG version.

Here is an overview of the SVG logo files.
All PNG files are available in the same folder and have the same name as the SVG files.
- **open-elements-logo/logo-landscape-dark-background.svg**: Landscape logo with dark background
- **open-elements-logo/logo-landscape-light-background.svg**: Landscape logo with light background
- **open-elements-logo/logo-portrait-dark-background.svg**: Portrait logo with dark background. This is preferred for square logo placements.
- **open-elements-logo/logo-portrait-light-background.svg**: Portrait logo with light background. This is preferred for square logo placements.

On a dark backround only the *-dark-background.* logos must be used.
On a light background only the *-light-background.* logos must be used.

Next to that the graphic part of the logo without the "Open Elements" text is available (for example for use as fav-icon)
- **open-elements-logo/logo-icon.png**: 1024x1024 PNG file
- **open-elements-logo/logo-icon@0,5x.png**: 0,5x PNG file
- **open-elements-logo/logo-icon@0,25x.png**: 0,25x PNG file
- **open-elements-logo/logo-icon@0,33x.png**: 0,33x PNG file
- **open-elements-logo/logo-icon@0,75x.png**: 0,75x PNG file
