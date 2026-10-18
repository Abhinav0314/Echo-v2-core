# Echo shared modules

Shared application logic and services used by Echo for Android. The parent repository records the exact core revision for each release.

- `common`: public configuration, logging and shared utilities.
- `domain`: models, repository contracts and playback interfaces.
- `data`: repositories, Room storage, account/library adapters and data providers.
- `media`: Android playback/download services and retained desktop bindings.
- `service`: music providers, lyrics, audio tools and Listen Together.

Implementation packages use `com.abhinav` and `org.abhinav.echo`. Android media component bridges keep their old qualified names for installed-client compatibility. Published third-party SDK coordinates and API identities remain unchanged.

Run validation through the parent Echo Gradle build. Preserve the complete Room schema history and existing migration fixtures when changing storage. Do not change database filenames, preference keys, external service identifiers or legacy component names as cosmetic branding edits.

This core derives from SimpMusic core by its original authors. Original copyright and licence notices remain in the source and [licence](LICENSE).
