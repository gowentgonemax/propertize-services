# GSAP UI/UX Enhancement Guide — Propertize Platform

> **Source**: [github.com/greensock/gsap-skills](https://github.com/greensock/gsap-skills)  
> **Library**: GreenSock Animation Platform (GSAP) — industry-standard JavaScript animation engine  
> **Status**: Not yet installed (Framer Motion `^12.29.2` is currently in use)

---

## 1. What is GSAP?

GSAP is the **#1 JavaScript animation library** used by millions of sites. It animates anything
JavaScript can touch — DOM, SVG, Canvas, WebGL — with near-perfect cross-browser consistency.

The `gsap-skills` repo is GreenSock's **official AI skills pack**: a structured set of SKILL.md
files that teach AI coding agents (Copilot, Claude, Cursor) exactly how to write correct GSAP
code, covering core API, timelines, ScrollTrigger, plugins, and React patterns.

### Available Skills

| Skill                | What It Teaches                                                        |
| -------------------- | ---------------------------------------------------------------------- |
| `gsap-core`          | `gsap.to()`, `gsap.from()`, `gsap.fromTo()`, easing, duration, stagger |
| `gsap-timeline`      | Sequencing, position parameter `+=`, labels, nesting, playback         |
| `gsap-scrolltrigger` | Scroll-linked animations, pinning, scrub, viewport triggers            |
| `gsap-plugins`       | Flip, Draggable, SplitText, ScrambleText, MotionPath, CustomEase       |
| `gsap-utils`         | `clamp`, `mapRange`, `interpolate`, `random`, `snap`, `pipe`           |
| `gsap-react`         | `useGSAP` hook, refs, `gsap.context()`, SSR-safe cleanup               |
| `gsap-performance`   | `will-change`, transform aliases, batching, ScrollTrigger tips         |

---

## 2. GSAP vs Framer Motion (Propertize Already Has Framer Motion)

Propertize currently uses **Framer Motion `^12.29.2`**. These libraries are complementary,
not mutually exclusive. Use each where it shines:

| Concern                                   | Framer Motion (keep) | GSAP (add)                        |
| ----------------------------------------- | -------------------- | --------------------------------- |
| Page transitions, route changes           | ✅ Best choice       |                                   |
| Layout animations (`AnimatePresence`)     | ✅ Best choice       |                                   |
| Simple `animate={{ opacity }}` one-liners | ✅ Simpler syntax    |                                   |
| **Number count-up animations**            | Clunky               | ✅ Native support                 |
| **Scroll-linked animations** (scrub)      | Limited              | ✅ ScrollTrigger is industry-best |
| **Complex sequenced timelines**           | Hard to control      | ✅ Timeline API is purpose-built  |
| **SVG animation** (charts, paths)         | Limited              | ✅ Full SVG support               |
| **Stagger entrance for lists/grids**      | Possible             | ✅ Built-in `stagger` param       |
| **Physics / inertia**                     | No                   | ✅ InertiaPlugin, Draggable       |
| **Flip animations** (list reorder)        | Possible             | ✅ FlipPlugin                     |

**Recommended strategy**: Keep Framer Motion for layout/presence animations; add GSAP for
number counters, scroll-driven effects, complex timelines, and SVG enhancements.

---

## 3. How GSAP Improves Propertize UI/UX

### 3.1 Dashboard Stat Card — Number Count-Up

Currently, stat cards (`Organizations: 42`, `Revenue: $128K`) just **snap** to their value on
load. With GSAP, numbers animate from 0 → actual value, giving the UI an instant sense of
momentum and making the data feel "live".

```tsx
// gsap.to on a JS object; update DOM on each frame
gsap.to(counter, {
  value: 42,
  duration: 1.2,
  ease: "power2.out",
  snap: { value: 1 },
  onUpdate: () => setDisplayValue(Math.round(counter.value)),
});
```

### 3.2 Staggered Card Entrance

Instead of all 4 stat cards appearing simultaneously, stagger them in sequence
(left-to-right, 80ms apart) so the eye naturally follows the data hierarchy.

```tsx
gsap.fromTo(
  ".stat-card",
  { opacity: 0, y: 16 },
  { opacity: 1, y: 0, duration: 0.45, stagger: 0.08, ease: "power2.out" },
);
```

### 3.3 HealthBar Progress Animation

The System Health progress bars currently render at their final width instantly.
GSAP can animate them from `width: 0%` → final, communicating the "filling up" of capacity.

```tsx
gsap.fromTo(
  barEl,
  { width: "0%" },
  { width: `${value}%`, duration: 0.9, ease: "power2.out" },
);
```

### 3.4 ScrollTrigger — Animate On Viewport Enter

The "Organization Pipeline", "Revenue Summary", and "Recent Activity" sections can animate
in as the user scrolls down, making long dashboards feel alive rather than static.

```tsx
gsap.fromTo(
  ".dashboard-card",
  { opacity: 0, y: 30 },
  {
    opacity: 1,
    y: 0,
    duration: 0.6,
    stagger: 0.1,
    scrollTrigger: {
      trigger: ".dashboard-card",
      start: "top 85%",
      toggleActions: "play none none none",
    },
  },
);
```

### 3.5 Quick Action Hover Lift

Currently using Tailwind `hover:scale-*`. GSAP gives smoother, physics-based hover:

```tsx
el.addEventListener("mouseenter", () =>
  gsap.to(el, { y: -4, scale: 1.03, duration: 0.2, ease: "power1.out" }),
);
el.addEventListener("mouseleave", () =>
  gsap.to(el, { y: 0, scale: 1, duration: 0.25, ease: "power1.inOut" }),
);
```

### 3.6 Activity Feed — Cascade Entry

New activity items in "Recent Activity" can slide in from the right with a cascade,
making new events feel important and real-time.

```tsx
gsap.fromTo(
  ".activity-item",
  { x: 20, opacity: 0 },
  { x: 0, opacity: 1, stagger: 0.06, duration: 0.35, ease: "back.out(1.2)" },
);
```

### 3.7 Loading Skeleton → Content Transition

Replace the raw CSS `animate-pulse` skeleton with a GSAP-orchestrated fade:

```tsx
const tl = gsap.timeline();
tl.to(".skeleton", { opacity: 0, duration: 0.3 }).fromTo(
  ".content",
  { opacity: 0 },
  { opacity: 1, duration: 0.4 },
  "-=0.1",
);
```

---

## 4. Installation

```bash
cd propertize-front-end
npm install gsap @gsap/react
```

### Register in `app/layout.tsx` or a `providers/GSAPProvider.tsx`

```tsx
import { gsap } from "gsap";
import { ScrollTrigger } from "gsap/ScrollTrigger";
import { useGSAP } from "@gsap/react";

// Register once globally (browser-only)
if (typeof window !== "undefined") {
  gsap.registerPlugin(ScrollTrigger, useGSAP);
}
```

---

## 5. Sample Enhancement — Platform Dashboard Stat Cards with Count-Up

See the complete implementation in:
`src/components/dashboard/gsap/AnimatedStatCard.tsx`

This component wraps the existing `StatCard` pattern from the platform dashboard with:

- Number count-up animation (0 → value on mount)
- Staggered entrance via GSAP timeline (parent orchestrates children)
- Hover lift with physics-based ease
- Full `useGSAP` cleanup (no memory leaks on unmount)

---

## 6. Quick Reference — Correct GSAP Patterns for This Project

```tsx
// 1. Import and plugin registration (once per app)
import { gsap } from "gsap";
import { ScrollTrigger } from "gsap/ScrollTrigger";
import { useGSAP } from "@gsap/react";
gsap.registerPlugin(ScrollTrigger, useGSAP);

// 2. Always use useGSAP in React (handles cleanup automatically)
const containerRef = useRef<HTMLDivElement>(null);
useGSAP(
  () => {
    gsap.from(".card", { opacity: 0, y: 20, stagger: 0.1 });
  },
  { scope: containerRef },
); // scope limits selector to this component

// 3. Count-up pattern
const counter = { value: 0 };
useGSAP(() => {
  gsap.to(counter, {
    value: targetNumber,
    duration: 1.2,
    ease: "power2.out",
    snap: { value: 1 },
    onUpdate: () => setDisplay(Math.round(counter.value)),
  });
}, [targetNumber]);

// 4. ScrollTrigger — always call refresh after layout changes
gsap.fromTo(
  ".section",
  { opacity: 0 },
  {
    opacity: 1,
    scrollTrigger: { trigger: ".section", start: "top 80%" },
  },
);
// After dynamic content loads:
ScrollTrigger.refresh();
```

---

## 7. Risk Assessment

- **Risk Level**: LOW — GSAP is an animation library with no security surface area
- **Bundle impact**: GSAP core ~25KB gzipped; plugins are tree-shaken individually
- **SSR compatibility**: Safe with Next.js 14 App Router — use `typeof window !== "undefined"` for
  plugin registration or `useGSAP` hook (which is client-only by nature)
- **Conflict with Framer Motion**: None — they operate on separate elements independently

---

## 8. Priority Enhancements for Propertize

| Priority  | Component                     | Enhancement                | Impact                         |
| --------- | ----------------------------- | -------------------------- | ------------------------------ |
| 🔴 High   | Platform Dashboard stat cards | Number count-up animation  | High — first thing users see   |
| 🔴 High   | System Health bars            | Width animate-in from 0%   | High — makes metrics feel live |
| 🟡 Medium | Org Pipeline cards            | Staggered entrance         | Medium — polish                |
| 🟡 Medium | Recent Activity feed          | Cascade slide-in           | Medium — real-time feel        |
| 🟢 Low    | Quick Actions                 | Physics hover lift         | Low — subtle delight           |
| 🟢 Low    | Loading → content             | Orchestrated skeleton fade | Low — smoothness               |
