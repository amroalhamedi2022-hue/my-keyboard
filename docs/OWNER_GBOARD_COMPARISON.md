# Zero Keyboard — owner screenshot comparison and acceptance gates
2026-11-28 | 15 owner screenshots: Zero engine preview vs Gboard on tablet portrait.
No private screenshot or message content is included in the repository.

## Observed baseline and targets
- Both apps were captured at 960×1536. Zero keyboard top at approximately y=870; Gboard Arabic at y=988. Including system navigation, their bottom regions occupy approximately 43.4% and 35.7% of screen height respectively. The references use different languages, so these are layout observations, not a controlled benchmark. Aim for a tablet portrait region near 36% with number row enabled; validate on identical language, editor, font scale and orientation. New default height scale 0.82 is a starting value, not a verified pixel match. Preserve explicit preferences and phone heights.
- Zero English and symbols have flat, gray gradient background, tall rows, rectangular white spacebar and numerous toolbar actions. Target cohesive light/dark surfaces, clearly differentiated action keys, rounded spacebar, predictable hit areas, adaptive toolbar and suggestions without excessive empty space. Gboard's app-editor formatting toolbar belongs to the host, not the IME; never duplicate it.
- Gboard Arabic: visible localized number row, Arabic alphabet, globe, emoji, directional arrows and action key. Test Arabic and English independently, symbols pages, punctuation, long press alternatives, Arabic diacritics, bidirectional cursor movement and numbers. No layout may silently drop characters.
- Gboard toolbar: expandable/reorderable shortcuts, microphone, clipboard, editing, emoji, resize, floating/split. Existing mature toolbar has editing shortcuts; test overflow, reorder/pin and suggestion mode before claiming parity. GIF and translation are excluded from core scope, per owner.
- Gboard settings: tablet two-pane navigation, clean groups and consistent switches. Target adaptive two-pane settings, working search, previews, voice, glide, clipboard, language, correction, themes, privacy. Existing HeliBoard preferences are not evidence of equivalent appearance or behavior.
- Owner reference preferences: number row on, suggestions and next-word on, auto-correction off, auto-capitalization on, double-space period off, hints off, sound on at 19%, long-press 300 ms; glide/trail/delete/spacebar cursor enabled. Fresh defaults updated for number row, clean hints, sound and double-space period. Existing explicit preferences preserved. Privacy-first clipboard retention and personalized learning remain opt-in.
- Clipboard: Gboard shows recent text/images, extracted items, screenshots. Zero mature history currently restricts to safe plain text; image clipboard and screenshot support require explicit opt-in, content URI grants, sensitive flags, expiration, no broad media permission and device validation. Never read private editor data.
- Voice: microphone opened settings; selected models missing. Source fix packages verified Tiny model and requests microphone permission when needed. Verify fresh install, existing upgrade, permission granted/denied, private fields, first and repeated taps, offline AR/EN/mixed recognition, silence endpoint, pause/stop/discard and text insertion. No claim of actual accuracy or working capture until tested.
- Glide: experimental geometric decoder, not equivalent to Gboard. Test real Arabic and English trajectories, taps vs glides, cursor/delete gestures, candidate selection and responsiveness. Avoid automatic enable until reliability is established.
- Suggestions: test next-word, manual candidate acceptance, Arabic dialect words, English casing, local personalized dictionary import/export, no forced autocorrection, no contact access. Verify learning is blocked in private fields and can be deleted.
- Themes: coherent neutral light/dark, adaptive contrast, optional borderless keys and custom themes; no expensive blur or animation in input hot path. Actual screenshots required in both orientations.
- Release: single deliberate build after coherent changes; verify existing package/signing identity and increase version code before owner upgrade. Test actual editor interaction and Android 13 tablet layouts; preserve old model imports and preferences. No debug APK delivered as owner release.

## Evidence status
Voice source changes committed separately. Tablet compact/default preference patch authored, not compiled or measured. Visual redesign, Arabic geometry, adaptive settings, clipboard images, real glide/voice accuracy and owner-signed release remain open. Compare fresh screenshots against owner references before claiming completion.

## Candidate 105 review

All 15 references were reviewed together. The Zero references show English in
two host editors, symbols and the legacy voice setup. The Gboard references
show Arabic letters and symbols, toolbar overflow and seven settings pages
covering languages, preferences, theme, correction, glide, voice and clipboard,
plus an emoji settings page.

- The settings reference uses a persistent category column with a selected
  state. Keep the current settings navigation until the adaptive replacement
  can exercise the same controls on both phone and tablet.
- Compare Arabic symbols independently from Arabic letters. The reference
  exposes page switching and frequent emoji above the symbols; the Zero
  symbols reference instead keeps the editing toolbar. Verify easy access to
  both surfaces and keep the host formatting controls outside the IME.
- The voice settings reference puts simple switches first. Candidate 105
  moves model selection, CPU threads, silence threshold and imports behind an
  advanced section. First-run system permission has a separate quick path.
- The clipboard reference includes configurable suggestions and recent
  images. Assess text retention and image insertion separately under the Zero
  privacy rules. Existing owner reference images stay private.
- Candidate 105 adds adaptive light and dark Zero Glass palettes and a rounded
  default. These use the existing renderer. Run matched-language geometry and
  contrast checks before judging the change against a reference.
