# uToob 

Our fork of [SmartTube](https://github.com/yuliskov/SmartTube) for the living-room TV.
Branch `tom` = upstream `master` + our changes. `master` mirrors upstream.

- Package `org.smarttube.utube`, label "uToob" (the `ststable` flavor, edited), green uToob badge
  on the icon/banner. Installs next to upstream SmartTube (`org.smarttube.stable`), own settings and login.
- In-app updater disabled for this package (it would install upstream SmartTube instead).
- CI: `.github/workflows/utoob.yml` builds a signed release APK on every push to `tom`.
  Secrets `UTOOB_KEYSTORE_B64` + `UTOOB_KEYSTORE_PASSWORD` (key alias `tomtube`).
- Install: download the armeabi-v7a artifact, `adb -s 10.1.1.235:5555 install -r <apk>`.
- Sync with upstream: `git fetch upstream && git checkout tom && git merge upstream/master`.
- Name history: TomTube -> uTube -> uToob (2026-11-27). The package id stays `org.smarttube.utube` on purpose:
  changing it makes Android treat it as a different app (settings and people lost).
- Watched videos (90%+ through, `Video.isWatched()`) are greyed out on cards (`VideoCardPresenter.utoobGreyOutIfWatched`).
  Long-press menu always has "Mark as watched", or "Mark as unwatched" on a watched video (forced on in `MainUIData`).
