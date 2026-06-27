# Klink Frontend Skill — World Class Church App UI

## WHEN TO USE THIS SKILL
Use this skill before building ANY screen, component, 
animation, or visual element in the Klink mobile app.
Read this file completely before writing a single line 
of frontend code.

## TECH STACK
- React Native with Expo
- react-native-reanimated for all animations
- react-native-gesture-handler for all gestures
- expo-linear-gradient for all gradients
- expo-blur for frosted glass effects
- expo-haptics for tactile feedback
- react-native-fast-image for all images
- @shopify/flash-list for all lists
- lottie-react-native for complex animations
- react-native-svg for vector graphics
- expo-splash-screen for splash animation

## DESIGN LANGUAGE

### Colors
Primary:
- Deep Royal Purple: #2D1B69
- Gold/Amber: #F4A429
- Pure White: #FFFFFF
- Soft Cream: #FDF8F0

Accent:
- Rose Gold: #C9797A
- Deep Navy: #0A0F2E
- Electric Blue: #4A90D9
- Forest Green: #2D6A4F

Dark Mode:
- Background: #0A0F2E
- Surface: #1A1F3E
- Cards: #252A4A
- Text: #F5F5F5
- Muted: #8B8FA8

Gradients:
- Worship: linear #2D1B69 → #6B3FA0
- Glory: linear #F4A429 → #FFD700
- Sunrise: linear #FF6B6B → #F4A429
- Heaven: linear #667eea → #764ba2
- Dark Worship: linear #0A0F2E → #2D1B69

### Typography
- Hero: Bold 48px, letterSpacing -2
- Headers: SemiBold 28px, letterSpacing -1
- Body: Regular 16px, lineHeight 1.7
- Caption: 12px, letterSpacing 0.5
- Heading font: Playfair Display or Cormorant Garamond
- Body font: Inter or Plus Jakarta Sans

### Spacing
- xs: 4px
- sm: 8px
- md: 16px
- lg: 24px
- xl: 32px
- xxl: 48px
- xxxl: 64px

---

## PHOTO AND MOTION BLEND TECHNIQUES

These techniques must be applied to every hero section
and featured image in the app:

### 1. Scene Planning with Photo and Motion Blends
- Always plan screens as layered scenes not flat layouts
- Every hero image must have at least 3 layers:
  Layer 1 Background: full bleed worship photo
  Layer 2 Middle: subtle overlay or light effect
  Layer 3 Foreground: text and interactive elements
- Motion is added to each layer at different speeds
  to create cinematic depth

### 2. Cut Out Photos with Foreground and Background Depth
- Separate subjects from backgrounds when possible
- Place cut out congregation members in foreground
- Keep full worship scene in background
- Add depth blur to background (expo-blur)
- Foreground subject remains sharp and crisp
- Creates a 3D window effect on flat mobile screens
- Use react-native-svg masks for clean cutouts

### 3. Animate Images with Pan, Zoom, Tilt, and Parallax
Pan animation:
- Images slowly pan left or right at 20px over 8 seconds
- Creates a living photo effect on static images
- Use react-native-reanimated withRepeat and withTiming

Zoom animation:
- Subtle Ken Burns effect: scale from 1.0 to 1.08 over 10 seconds
- Applied to hero images on home and splash screens
- Easing: Easing.inOut(Easing.ease)

Tilt animation:
- Cards tilt on the X and Y axis based on touch position
- Max tilt: 15 degrees on each axis
- Returns to flat with spring animation on release
- Creates realistic 3D card interaction

Parallax animation:
- Background moves at 0.3x scroll speed
- Middle layer moves at 0.6x scroll speed  
- Foreground content moves at 1x scroll speed
- Implemented with Animated.event on ScrollView

### 4. Smooth Masks, Fades, and Transitions Between Sections
- Every section transition uses a gradient mask fade
- Bottom of one section fades into next using LinearGradient
- Mask goes from fully opaque to transparent over 80px
- Colors match the sections being connected
- No hard edges anywhere in the app
- Crossfade duration: 300ms minimum

### 5. Custom Animation Timing for Realistic Movement
- Never use linear easing for anything visible
- Entry animations: Easing.out(Easing.back(1.5))
- Exit animations: Easing.in(Easing.ease)
- Bouncy interactions: spring with damping 10, stiffness 100
- Floating elements: Easing.inOut(Easing.sine)
- Page transitions: Easing.out(Easing.cubic)
- All timings feel physical and weighted, never robotic

---

## 3D ANIMATIONS

### 3D Card Flip
- Cards flip 180 degrees on Y axis to reveal back
- Front: photo, name, summary
- Back: details, actions, contact info
- Use perspective: 1000 in transform style
- Front visible when rotateY is 0 to 90 degrees
- Back visible when rotateY is 90 to 180 degrees
- Duration: 400ms with ease-in-out

### 3D Button Press
- Primary buttons push down 4px on press
- Shadow reduces from 8px to 2px during press
- Gold shimmer sweeps left to right on release
- Spring back with overshoot: damping 8, stiffness 200
- Haptic impact: light on press, medium on release

### 3D Floating Logo
- Church logo on splash and header floats in 3D space
- Rotates slowly on Y axis: 0 to 5 degrees and back
- Scale breathes: 1.0 to 1.03 and back
- Casts pulsing glow shadow in gold
- Loop duration: 3 seconds, easing: sine in-out

### 3D Page Transitions
- Entering screen rotates from -15 to 0 degrees on Y axis
- Exiting screen rotates from 0 to 15 degrees on Y axis
- Combined with opacity: 0 to 1 entering, 1 to 0 exiting
- Perspective: 800 for dramatic 3D effect
- Duration: 400ms

### 3D Giving Thermometer
- Cylindrical progress bar for project fundraising
- Fills with animated gold liquid from bottom to top
- Glows brighter gold as percentage increases
- Particle burst celebration when 100% reached
- Real-time update animation when new contribution added

### 3D Tilt Cards
- All feature cards respond to device gyroscope
- Card tilts to follow device orientation subtly
- Max tilt: 8 degrees any direction
- Inner content shifts slightly opposite direction
- Creates convincing 3D depth on flat cards
- Fallback to touch-based tilt if gyroscope unavailable

---

## SCROLL EFFECTS

### Sticky Header Morph
- Header starts: fully transparent, logo large
- Scrolls past 80px: transitions to solid deep purple
- Logo shrinks from 48px to 32px during transition
- Navigation items fade in as header solidifies
- All transitions smooth over 80px scroll distance
- Use Animated.interpolate on scroll position

### Scroll Reveal Animations
- Every card and section animates in on viewport entry
- Default reveal: translateY from 30 to 0, opacity 0 to 1
- Stagger delay: 100ms between each item in a list
- Duration: 500ms with ease-out-cubic
- Items stay visible once revealed — no re-hiding
- Use react-native-reanimated useAnimatedScrollHandler

### Horizontal Scroll with Snap
- Sermon series, events, announcements scroll horizontally
- Snap to center of each card
- Cards scale: 0.92 when not centered, 1.0 when centered
- Shadow increases on centered card
- Peek of next card visible at edge (show 85% of last card)
- Smooth momentum scrolling with decelerationRate snap

### Pull to Refresh
- Custom animation: golden cross appears and spins
- Cross glows brighter as user pulls further
- Release triggers heaven ray burst expanding outward
- Loading: three gold dots pulse in sequence
- Completion: green checkmark draws itself in

### Infinite Scroll
- Detect when user is 5 cards from end
- Load next page silently in background
- New cards fade in from below seamlessly
- No jarring position jumps ever
- Skeleton cards show while loading

### Momentum Scrolling
- All scroll views have decelerationRate fast
- Feels native and physical on both iOS and Android
- Overscroll bounce preserved on iOS
- Smooth rubber band effect at list edges

---

## CHURCH AND WORSHIP VISUAL STANDARDS

### Image Requirements
- All congregation images: warm lighting, golden hour tones
- Hands raised in worship preferred for hero images
- High resolution minimum: 1080x1920 for heroes
- No stock photo feel — authentic worship moments only
- Faces showing genuine emotion and spiritual connection
- Diverse congregation representing all ages and backgrounds

### Image Treatment
- All hero images: subtle warm overlay at 20% opacity
- Color: rgba(244, 164, 41, 0.2) — soft gold tint
- Enhances worship atmosphere without washing out image
- Dark gradient from bottom: rgba(0,0,0,0) to rgba(0,0,0,0.7)
- Ensures text always readable over any image
- Never stretch or distort images — always cover mode

### Placeholder Images
While real images are loading always show:
- Gradient placeholder in worship colors
- Subtle shimmer animation sweeping left to right
- Fades smoothly to real image when loaded
- No jarring pop or flash when image arrives

### Particle Effects
Ember particles (use on splash and hero screens):
- Small golden dots rise upward slowly
- Randomized size: 2px to 6px
- Randomized opacity: 0.3 to 0.8
- Randomized speed: 3 to 8 seconds to rise off screen
- Spawn at bottom of screen continuously
- Maximum 30 particles at once for performance

Light rays (use on special moments):
- Radiate from top center of screen
- Soft white at 15% opacity
- Rotate very slowly: 1 degree per 5 seconds
- Used on: login success, check in success, giving success

---

## MICRO INTERACTIONS

### Prayer Reaction
- Tap announcement to react with prayer hands 🙏
- Emoji spawns at touch point
- Floats upward 80px while fading out
- Multiple taps spawn multiple floating emojis
- Counter increments with spring bounce
- Haptic: light impact on tap

### Attendance Check In
- Large pulse animation on check in button
- Ripple expands outward from button center on tap
- Success: screen flashes gold briefly
- Confetti burst: purple and gold pieces fall from top
- Checkmark draws itself with path animation
- Text bounces in: "You are checked in!"
- Haptic: success notification pattern

### Notification Bell
- Bell shakes with 3 degree oscillation on new notification
- 4 shakes then settles
- Badge count bounces in with spring overshoot
- Red badge pulses gently when unread items exist
- Haptic: light impact when notification arrives

### Bottom Navigation
- Active indicator: rounded pill slides between tabs
- Pill color: gold gradient
- Selected icon: scales to 1.2x with spring
- Icon color: transitions gray to gold in 200ms
- Haptic: selection feedback on each tab switch
- Tab label fades in below active icon only

### Form Inputs
- Label: sits inside input at rest
- On focus: label floats up to above input in 200ms
- Underline: grows from center left and right on focus
- Underline color: gold (#F4A429)
- Error: input shakes horizontally 4 times
- Success: green checkmark appears at right of input
- All transitions: 200ms ease-in-out

---

## PERFORMANCE STANDARDS

### Animation Performance
- All animations: 60fps minimum on mid-range devices
- Use useNativeDriver: true for all transform and opacity
- Use react-native-reanimated worklets on UI thread
- Never animate layout properties (width, height, top, left)
- Only animate transform and opacity for GPU acceleration
- Test on low-end Android as baseline

### Image Performance
- Use react-native-fast-image for all images
- Set explicit width and height on every image
- Use resizeMode cover for hero images
- Preload next screen images before navigation
- Cache all images with FastImage cache control
- Progressive loading: blur hash placeholder first

### List Performance
- Use @shopify/flash-list for ALL scrollable lists
- Never use FlatList or ScrollView for long lists
- Set estimatedItemSize accurately for smooth scrolling
- Implement getItemType for mixed content lists
- Memoize all list item components with React.memo
- Remove items that leave viewport to save memory

### App Startup
- Splash screen hides after all critical assets load
- Maximum 2 seconds before first meaningful screen
- Lazy load all non-critical screens
- Preload fonts before showing any text
- Critical images prefetched during splash

---

## DARK MODE STANDARDS

### Colors in Dark Mode
- Background: #0A0F2E — deep navy not pure black
- Surface: #1A1F3E — elevated surfaces
- Cards: #252A4A — card backgrounds
- Border: rgba(255,255,255,0.1) — subtle borders
- Text primary: #F5F5F5 — never pure white
- Text muted: #8B8FA8 — secondary text
- Gold stays the same in dark mode — #F4A429

### Dark Mode Rules
- Detect system preference with useColorScheme
- Apply theme instantly with no flash
- Store manual preference in AsyncStorage
- All images unchanged — no filter in dark mode
- Gradients have dark mode variants pre-defined
- Test every screen in both modes before shipping

---

## ACCESSIBILITY STANDARDS

- All interactive elements minimum 44x44px touch target
- Color contrast ratio minimum 4.5:1 for all text
- All images have accessible labels
- All buttons have descriptive accessibilityLabel
- Support dynamic font sizes
- VoiceOver and TalkBack compatible
- Never rely on color alone to convey information

---

## RULES FOR EVERY SCREEN

1. Read this skill file completely before writing any code
2. Every screen must have at least one parallax or 
   scroll animation
3. Every hero section must use the photo and motion 
   blend techniques
4. Every list must use FlashList not FlatList
5. Every image must use FastImage not Image
6. Every animation must use native driver
7. Every screen must support dark mode
8. Every interactive element must have haptic feedback
9. Every loading state must use skeleton not spinner
10. Every transition must feel physical and weighted
11. Test on low-end Android before marking complete
12. No hard edges between sections — always use 
    gradient masks

---

## INSTALL COMMANDS

Run these before starting any frontend work:

```bash
# Create Expo project (if starting fresh)
npx create-expo-app klink --template blank-typescript
cd klink

# Core animation and gesture libraries
npx expo install react-native-reanimated
npx expo install react-native-gesture-handler

# Expo UI and visual libraries
npx expo install expo-linear-gradient
npx expo install expo-blur
npx expo install expo-haptics
npx expo install expo-splash-screen
npx expo install expo-font
npx expo install expo-image  # preferred Expo-native alternative; see note below

# High-performance list and image
npm install @shopify/flash-list
npm install react-native-fast-image

# Animation and vector
npm install lottie-react-native
npx expo install react-native-svg

# Navigation
npx expo install expo-router
npx expo install react-native-safe-area-context
npx expo install react-native-screens

# Storage and async
npx expo install @react-native-async-storage/async-storage

# Device sensors (for gyroscope tilt on cards)
npx expo install expo-sensors

# After installing reanimated add this to babel.config.js:
# plugins: ['react-native-reanimated/plugin']
# This MUST be the last plugin in the list.

# Run prebuild for native modules (if using bare workflow)
npx expo prebuild

# Start development server
npx expo start
```

> **Note on images:** `react-native-fast-image` requires native build support
> (`npx expo prebuild`). In Expo Go use `expo-image` as a drop-in replacement
> — it supports caching, blur hash placeholders, and all resizeModes.
> Switch to `react-native-fast-image` for production EAS builds.

> **Note on Lottie:** After installing, run `npx expo prebuild` and rebuild
> the native app. Lottie does not work in Expo Go without a custom dev client.
> Use `npx expo run:ios` or `npx expo run:android` for full animation support.

---

## GLASSMORPHISM AND ADVANCED VISUAL EFFECTS

### Glassmorphism
- Frosted glass effect on all modal cards and overlays
- Background blur: 20px using expo-blur BlurView
- Card background: rgba(255, 255, 255, 0.15) light mode
- Card background: rgba(255, 255, 255, 0.05) dark mode
- Border: 1px solid rgba(255, 255, 255, 0.2)
- Always layer over colorful gradient or image background
- Use on: login card, giving card, announcement overlay,
  project detail header, member profile hero

### Gradient Morphing
- Background gradients slowly shift between colors
- Use on splash screen and home screen hero
- Transition between 3 gradient states in a loop
- Each transition takes 4 seconds with ease-in-out
- Colors shift through worship palette:
  State 1: #2D1B69 to #6B3FA0 (deep worship purple)
  State 2: #0A0F2E to #2D1B69 (midnight navy)
  State 3: #6B3FA0 to #F4A429 (purple to gold sunrise)
- Use react-native-reanimated withRepeat and withSequence
- Never loops abruptly — always smooth crossfade

### Aurora Effect
- Moving colorful gradient background on special screens
- Colors: purple, blue, gold, and rose gold
- Slow organic movement — blobs of color drift slowly
- Use on: splash screen, onboarding, prayer screen
- Opacity: 60% so content remains readable
- Implementation: animated radial gradients with SVG
- Performance: run on UI thread with reanimated worklets

### Mesh Gradient
- Multi-point gradient with organic flowing colors
- 4 color points positioned at corners and center
- Each point drifts slowly to adjacent position
- Creates living breathing background effect
- Use as background on home screen hero section
- Colors: deep purple, gold, navy, rose gold

### Neumorphism
- Soft UI with extruded and inset shadow effects
- Light source from top left
- Light shadow: rgba(255, 255, 255, 0.7) offset -4 -4
- Dark shadow: rgba(0, 0, 0, 0.15) offset 4 4
- Use on: stat cards, quick action buttons, toggle switches
- Background must match shadow base color exactly
- Never use on dark mode — switch to glassmorphism instead

### Holographic Shimmer
- Rainbow iridescent shimmer on premium elements
- Animate hue rotation from 0 to 360 degrees
- Use on: giving milestone badges, achievement cards,
  special announcement featured cards
- Subtle — not overwhelming, just a hint of rainbow
- Speed: full rotation every 3 seconds

### Noise Texture Overlay
- Subtle grain texture layered over flat gradients
- Opacity: 3 to 5 percent — barely visible
- Adds tactile depth to otherwise flat surfaces
- Use SVG filter or pre-baked texture image
- Apply to: hero backgrounds, card surfaces, headers
- Gives premium printed material feel to digital surfaces

### Liquid Metal Effect
- Mercury-like flowing silver on premium UI elements
- Use on: PASTOR role badge, special achievement items
- Animated gradient: silver to white to silver
- Slow sweep animation: 2 seconds per cycle
- Combined with subtle 3D extrusion shadow

---

## ADVANCED ANIMATION TECHNIQUES

### Lottie Animations
Always use Lottie for these specific moments:
- Success checkmark after check in
- Loading spinner with cross motif
- Empty state illustrations
- Onboarding illustrations
- Prayer hands animation
- Dove flying animation on peace/success
- Fire animation for Holy Spirit themed sections
- Confetti burst on giving milestone achieved
Source files from: lottiefiles.com
Search terms: church, prayer, worship, cross, dove, fire

### Morphing Shapes
- Buttons morph shape on press and release
- Search bar expands from icon to full width
- FAB morphs into bottom sheet on press
- Loading circle morphs into checkmark on success
- Use react-native-svg with animated path data
- All morphs: 300ms with spring easing

### Physics Based Animations
- All cards respond to real physics when dismissed
- Swipe velocity determines fly-out speed and angle
- Cards bounce off screen edges with friction
- Dragged elements feel heavy and momentum-based
- Spring constants: damping 15, stiffness 150, mass 1
- Use react-native-reanimated withSpring everywhere
- Never use linear or ease for user-driven animations

### Shared Element Transitions
- Member photo expands from directory card to full profile
- Sermon artwork expands from list to player screen
- Project image expands from card to detail hero
- Use react-native-reanimated shared values
- Source and destination elements share same animated value
- Background fades to dark as element expands
- Feels like native iOS photo app transition

### Gesture Driven Animations
- Pull down gesture dismisses modal screens
- Swipe left on announcement to dismiss
- Swipe right on member card to call or message
- Long press on giving card reveals quick give options
- Two finger pinch zooms project photo gallery
- Gesture velocity affects animation exit speed
- All gestures have rubber band resistance at limits

### Ripple Effects
- Every touchable element has ripple on press
- Ripple color: rgba(244, 164, 41, 0.3) gold tint
- Ripple expands from exact touch point
- Duration: 400ms fade out
- Use react-native-gesture-handler TouchableRipple
- Apply to: all buttons, list items, cards, tabs

---

## TYPOGRAPHY EFFECTS

### Gradient Text
- Section headers use gold gradient fill
- Gradient: #F4A429 to #FFD700 left to right
- Use react-native-svg with LinearGradient and Text
- Apply to: home screen welcome text, screen titles,
  scripture references, milestone achievements

### Animated Text Reveal
- Hero text reveals word by word on screen entry
- Each word slides up from 10px below and fades in
- Stagger: 80ms between each word
- Use on: splash tagline, onboarding headlines,
  empty state messages, success screen text

### Typewriter Effect
- Scripture verses type themselves out character by character
- Cursor blinks after each character appears
- Speed: 50ms per character
- Pause 500ms at end before cursor disappears
- Use on: daily verse widget, prayer of the day,
  welcome message on first login

### Highlighted Text
- Important words get animated marker highlight
- Yellow or gold highlight draws from left to right
- Duration: 600ms with ease-out
- Use on: sermon key points, announcement highlights,
  giving call to action text

### Glowing Text
- Pastor name and special titles have subtle text glow
- Glow color matches role: gold for PASTOR,
  silver for CHURCH_MANAGER, white for ELDER
- Glow radius: 8px, opacity: 60%
- Subtle pulse animation: glow brightens and dims slowly

---

## CHURCH SPECIFIC VISUAL EFFECTS

### Cross Particle System
- Golden particles form the shape of a cross
- Particles drift and reform continuously
- Use on: splash screen, prayer screen, check in success
- Particle count: 50 maximum for performance
- Color: gold #F4A429 with varying opacity
- Each particle has unique drift path and speed

### Light Beam Animation
- Rays of light radiate from top of screen downward
- Soft white at 10 to 15 percent opacity
- Rotate very slowly: 0.5 degrees per second
- Use on: login screen, giving success, attendance success
- Layered over worship imagery for heavenly effect
- 6 rays at different angles and lengths

### Stained Glass Overlay
- Geometric colored pattern overlay on images
- Colors: deep purple, royal blue, gold, crimson, green
- Opacity: 15 percent — very subtle
- Use on: sermon artwork that lacks visual interest
- Creates instant cathedral stained glass atmosphere
- Static overlay — no animation needed

### Scripture Reveal
- Bible verse slides in from right side of screen
- Reference fades in below verse after 500ms delay
- Gold left border accent line draws downward
- Use on: daily verse widget, sermon scripture display
- Tap to copy verse triggers golden ripple effect

### Worship Counter
- Live attendance number ticks up as members check in
- Number rolls from previous value to new value
- Duration: 800ms with ease-out cubic
- Color changes: green when above average attendance,
  amber when average, red when below average
- Displayed prominently on pastor dashboard

### Tithe Thermometer
- Vertical cylinder fills with gold liquid from bottom
- Liquid has subtle wave animation at the surface
- Percentage label counts up as liquid fills
- Milestone markers at 25, 50, 75, 100 percent
- Each milestone triggers a small celebration animation
- At 100 percent: full confetti burst and glow

### Prayer Chain Visualization
- Prayer requests connected by glowing gold lines
- Each request is a node that pulses gently
- Lines animate from request to response
- Creates visual sense of community prayer
- Use on: prayer board screen
- Tap any node to expand prayer request detail

### Dove Animation
- White dove flies across screen from left to right
- Gentle wing flap animation using Lottie
- Leaves a brief golden trail as it passes
- Triggered on: successful giving, check in, prayer submission
- Duration: 2 seconds total including fade out
- Does not block interaction — purely decorative overlay

### Holy Fire Effect
- Orange and gold particle fire rising upward
- Use on: special event announcements, revival meetings
- Particles flicker with randomized opacity changes
- Base is wider, narrows as particles rise
- Maximum 40 particles for performance
- Runs at 60fps using reanimated worklets on UI thread

### Water Ripple Effect
- Circular ripples expand from center on baptism content
- Soft blue and white concentric rings
- Three rings expand simultaneously at different speeds
- Fade out as they expand to full screen width
- Use on: baptism announcements and records
- Triggered automatically when baptism content appears

---

## BENTO GRID AND LAYOUT EFFECTS

### Bento Grid Layout
- Asymmetric grid for dashboard and home screen
- Mix of large and small cards in organic arrangement
- Large cards: 2 column span for featured content
- Small cards: 1 column span for quick stats
- Gap: 12px between all cards
- Cards have rounded corners: 16px radius
- Each card has unique content type and visual treatment
- Inspired by Apple website and Notion homepage

### Floating Cards
- Cards appear to hover above background
- Shadow: offset 0 8px, blur 24px, rgba(0,0,0,0.15)
- Subtle parallax: card moves slightly opposite to scroll
- On press: card pushes down 3px and shadow reduces
- On release: springs back with slight overshoot
- Dark mode shadow: rgba(0,0,0,0.4) for visibility

### Collapsible Sections
- Smooth accordion expand and collapse
- Height animates from 0 to full content height
- Chevron rotates 180 degrees when expanded
- Content fades in as section expands
- Duration: 300ms with ease-in-out
- Use on: member details, giving breakdown, FAQ

### Swipeable Row Actions
- Swipe list items left to reveal action buttons
- Actions slide in from right as item moves left
- Delete action: red background with trash icon
- Quick give action: gold background with heart icon
- Message action: blue background with chat icon
- Threshold: 30% of screen width to trigger action
- Haptic feedback when threshold is crossed

---

## LOADING AND SKELETON EFFECTS

### Skeleton Screens
Every screen has a skeleton version that shows while loading:
- Skeleton shapes match exact layout of real content
- Shimmer animation sweeps left to right continuously
- Shimmer color: rgba(255,255,255,0.1) on dark backgrounds
- Shimmer color: rgba(0,0,0,0.06) on light backgrounds
- Duration: 1.5 seconds per sweep, loops infinitely
- Transition: skeleton fades out as real content fades in
- Never show spinners — always use skeleton screens

### Progressive Image Loading
- Show blur hash placeholder while image downloads
- Blur hash generated from image dominant colors
- Transition: blur dissolves to sharp image over 300ms
- Never show empty space or broken image icon
- If image fails: show gradient placeholder with icon

### Content Placeholders
Specific skeleton shapes for each content type:
- Member card skeleton: circle for avatar, 2 lines for name
- Sermon card skeleton: rectangle for thumbnail, 3 lines
- Announcement skeleton: full width rectangle, 2 lines
- Stat card skeleton: small rectangle, large number shape
- Project card skeleton: large rectangle, progress bar shape

---

## HAPTIC FEEDBACK STANDARDS

Apply haptic feedback to every interaction:

Light impact: tap on list items, tab switches, toggles
Medium impact: button press, card selection, form submit
Heavy impact: delete action, destructive confirmation
Success: giving completed, check in success, prayer sent
Warning: form validation error, low balance alert
Error: failed action, network error, auth failure

Import: import * as Haptics from 'expo-haptics'
Light: Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light)
Medium: Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium)
Heavy: Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Heavy)
Success: Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success)
Warning: Haptics.notificationAsync(Haptics.NotificationFeedbackType.Warning)
Error: Haptics.notificationAsync(Haptics.NotificationFeedbackType.Error)
