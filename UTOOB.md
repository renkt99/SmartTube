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
- Rocket Launcher's "uToob subscriptions" Home row: on every screen pause uToob saves the person's subscribed channels and watch
  progress to files/utube_profiles/share/<person>.json (`UtoobShare`); `UtoobShareProvider` (`${applicationId}.share`, own
  `:share` process where MainApplication starts nothing, answers com.tomrenk.rocket only) hands them over. Rocket fetches the RSS itself.
- Sidebar: rows sit closer to the top/bottom (padding 18/14dp), a highlighted row grows rightwards and isn't clipped, the open page's
  name is red (`BrowseSupportFragment.onCurrentRowChanged` -> `ExtendedHeadersSupportFragment.setCurrentRow`).
- Video grid pages use `UtoobGridCardPresenter`: 3 big cards across, one-line title, "Channel · date" under it (no views).
