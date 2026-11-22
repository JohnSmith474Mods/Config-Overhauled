# Config Overhauled — v1.1.0

## Added

### Guided Tagged Search & Autocomplete
- **Tagged filtering in the config screen** supporting logical AND combinations with plain-text queries:
  - `#group:<term>` — matches a group's translated name or internal ID.
  - `#id:<term>` — matches a property's resource name or unique ID.
  - `#key:<term>` — matches a property's translation key or resource name.
  - `#word:<term>` — matches words in a property's translated name and description/tooltip.
  - `#scope:<term>` — matches properties by scope (`client`, `global`, `level`).
  - `#modified` / `#modified:<true|false>` — filters properties differing from their default values.
  - `#locked` / `#locked:<true|false>` — filters properties locked by a remote server.
- **Upward autocomplete suggestion popup** attached to the search bar with keyboard navigation (`Up`/`Down` arrows, Numpad `8`/`2` when NumLock is off, mouse wheel) and token completion (`Tab`, `Right` arrow, Numpad `6` when NumLock is off, or click).

### Modal Detail Editors & Popup API
- **Public modal popup framework** (`johnsmith.configoverhauled.api.client.gui.popup.ConfigPopup`, `AbstractConfigPopup<T>`, and default modal host methods on `ConfigScreen`) featuring dimmed backdrops, responsive resizing, header badges, Tab focus cycling, Escape/outside-click dismissal, and Reset/Cancel/Apply footer controls.
- **Copy Property ID / Translation Key action** in every property-backed popup header: click to copy the property's unique ID, or **Shift+Click** to copy its translation key (with brief `Copied!` visual confirmation).
- **Text & Bounded Numeric Detail Popups** (`...` button aligned to the left of text inputs):
  - Wide `EditBox` with clear button, live validation feedback, default/range info, and character counter for strings.
  - Automatic bidirectionally synchronized slider for finite bounded `Integer`, `Long`, `Float`, and `Double` properties.
  - Read-only inspection and clipboard copy support for server-locked properties.
- **Detailed Color Picker Popup** (accessible by clicking any `RGB` or `ARGB` 20×20 color preview swatch):
  - Interactive 2D Saturation/Value square, vertical Hue spectrum bar, and optional vertical Alpha bar over a transparency checkerboard (`ARGB`).
  - Stable HSVA drag state (prevents Hue snapping when dragging to black, white, or grayscale).
  - Side-by-side Current/New comparison swatches (click Current to revert) and bidirectionally synchronized `#RRGGBB`/`#AARRGGBB` Hex and `R`, `G`, `B`, `(A)` numeric fields with unsigned bounds validation.
- **Structured List Editor Popup** for `List<E>` properties:
  - Scrollable, scissor-clipped row editor with dynamic line-number gutter, add (`+ Add`), reorder (Move Up / Move Down), and delete (`X`) controls.
  - Plain-string editing fast path for `Codec.STRING` lists and per-element `Codec` validation with error tooltips for typed lists.
  - Lossless toggle between **Structured** and **Raw JSON** array modes.
- **Reset All Confirmation Popup** displaying the exact count of modified resettable entries in the active tab before resetting.

### Navigation, Readability & Live Updates
- **Collapsible Group Headers**: Flat left-aligned headers with right-aligned `[+]` / `[-]` indicators, bottom underline while expanded, full keyboard/narration support, and independent collapse state during active searches.
- **Expand All / Collapse All Toggle** (`[-]` / `[+]`) positioned beside the search bar to collapse or expand all visible groups in one click.
- **Marquee Text Scrolling** for overflowing property labels and popup titles, ensuring long translated names remain readable without overlapping controls.
- **Modified Entry Indicator**: Subtle 2px vertical amber accent bar in the left gutter of modified entries (muted gray when locked) for quick visual scanning.
- **Live UI Refresh**: Config entries listen to `Property.Listener` updates so external changes (such as server sync packets or operator updates) refresh open UI controls immediately.

### Unified Developer Commands (Fabric, Forge & NeoForge)
- Consolidated client developer commands under the `/config_overhauled` root across all loaders:
  - `/config_overhauled localization <mod_id> [onlyMissingEntries]` — generates localization template JSON files (defaulting to missing entries only).
  - `/config_overhauled dump <mod_id> <format> <scope>` — exports `GLOBAL` or `LEVEL` scope configurations in native NightConfig `toml` (matching standard config files with comments and defaults) or structured `json` format.

## Changed
- Split development test `color_test` properties into `rgb_test` and `argb_test` across all scopes (`client_test`, `global_test`, `level_test`) with updated English translations.
- Added transparency checkerboard rendering to `ARGB` color preview swatches in the config list.
- Added English localization entries for all new search tags, group controls, detail popups, color picker, list editor, and confirmation dialogs.
