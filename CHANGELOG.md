## 1.0.3

### Fixed
- Fixed `Color.ARGB_CODEC` and `Color.RGB_CODEC` failing to parse opaque/unsigned hex colors (`alpha >= 0x80`, such as `#FFFFFFFF` or `#80000000`) and registered `Color.RGB_CODEC` / `Color.ARGB_CODEC` on `DynamicPropertyTypeRegistry.RGB_COLOR` and `ARGB_COLOR`.
- Removed redundant NightConfig `.autosave()` calls on `CommentedFileConfig` instances to eliminate per-property disk rewrite amplification during scope saves.
- Preserved deterministic registration order for config categories/tabs and indexed properties by unique ID for `O(1)` `findProperty()` lookups.
- Fixed `DefaultProperty.onDisconnect()` clearing listeners on static properties; listeners now remain attached and are notified when a synced runtime value reverts to disk state, while dynamic `LEVEL` properties continue to be invalidated and removed.
- Added a default `ConfigManager.detachServerConfig()` lifecycle hook to close and detach `LEVEL` config file handles on server stop and before re-attaching, and wired `GameShuttingDownEvent` to `ClientEventHandler.onClientStopping()` on Forge and NeoForge.
- Fixed `DefaultWidgetRegistry.register()` silently ignoring overrides of pre-registered default widgets.
- Cached the JSON preview tooltip in `ListEntry` instead of re-encoding via `JsonOps` every frame while hovered.
- Fixed `Group.IOptionalStep.withTranslationKey(String)` being ignored so custom translation keys are now honored by `DefaultProperty.translationKey()` and `descriptionTranslationKey()`.
- Fixed `RegistryListSelectionScreen.updateSelected()` reading the Available list's scroll offset instead of the Selected list's, preserving the Selected pane's scroll position when transferring or reordering items.
- Fixed `EnumEntry.createWidget()` throwing a `NullPointerException` for enums whose constants declare class bodies by resolving enum constants via `getDeclaringClass()` instead of `getClass()`.

