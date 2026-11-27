# uTube 

Our fork of [SmartTube](https://github.com/yuliskov/SmartTube) for the living-room TV.
Branch `tom` = upstream `master` + our changes. `master` mirrors upstream.

- Package `org.smarttube.utube`, label "uTube" (the `ststable` flavor, edited), green uTube badge
  on the icon/banner. Installs next to upstream SmartTube (`org.smarttube.stable`), own settings and login.
- In-app updater disabled for this package (it would install upstream SmartTube instead).
- CI: `.github/workflows/utube.yml` builds a signed release APK on every push to `tom`.
  Secrets `UTUBE_KEYSTORE_B64` + `UTUBE_KEYSTORE_PASSWORD` (key alias `tomtube`).
- Install: download the armeabi-v7a artifact, `adb -s 10.1.1.235:5555 install -r <apk>`.
- Sync with upstream: `git fetch upstream && git checkout tom && git merge upstream/master`.
