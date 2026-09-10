## [1.2.0-dev.1](https://github.com/logm1lo/logm1lo-patches/compare/v1.1.0...v1.2.0-dev.1) (2026-09-10)

### Bug Fixes

* default empty feed paths to /storage/emulated/0 in MtTools extension ([aeaa753](https://github.com/logm1lo/logm1lo-patches/commit/aeaa753d19fa98deff650a526c891d369899e905))
* file browser now functional - real rows + working clicks ([4d88c57](https://github.com/logm1lo/logm1lo-patches/commit/4d88c574a8c0903e1c15caca6db2e93383b9f529))
* force User Agreement flag to true so EULA never re-shows ([bec69bd](https://github.com/logm1lo/logm1lo-patches/commit/bec69bdb36ce86d42751d1588f15c2105d06ee4e))
* make file-browser feed patch apply standalone with splice ([46123cb](https://github.com/logm1lo/logm1lo-patches/commit/46123cb4562339eee0b355c5f397f9f979ab31cf))
* make MT Manager tool reimplementation work on-device ([d1f3d97](https://github.com/logm1lo/logm1lo-patches/commit/d1f3d97497b3ef36e37ce26dd12d56d4818e0168)), closes [PKCS#12](https://github.com/logm1lo/PKCS/issues/12)
* prevent Purchase-VIP / account screen NPE from null native Map getter ([fd5cc86](https://github.com/logm1lo/logm1lo-patches/commit/fd5cc8632b6669775840be565e00d737a2c28989))
* remove stub-native dependencies + clear native flags on patched getters ([7e34900](https://github.com/logm1lo/logm1lo-patches/commit/7e34900ad64902b35bc2d57326162072fcf17852))
* status-counter feed now fires + diagnostic logging (warm-state shortcut removed) ([85d3225](https://github.com/logm1lo/logm1lo-patches/commit/85d32253b0fa7a31e8886faf950ed17aa238638e))
* synchronous file-list feed + flat-browser adapter notify ([edc855f](https://github.com/logm1lo/logm1lo-patches/commit/edc855f70426eb6aa7f23b24a4674e12ef7cb0a3))

### New Features

* add feedFlatIndex helper + flat-browser feed patch (diagnostic) ([dbc3c45](https://github.com/logm1lo/logm1lo-patches/commit/dbc3c454c9346780f8f73d22f093c2a96f52941d))
* add Locket Gold unlock patch (Hermes bytecode override) ([db7ed74](https://github.com/logm1lo/logm1lo-patches/commit/db7ed746b8a80da17efd2f2649eda3a67a9ea9d7))
* add MT Manager Morphe patches - native stubbing, key patching, anti-tamper, VIP unlock ([d8d5d75](https://github.com/logm1lo/logm1lo-patches/commit/d8d5d75711259b5993c7684c7b725ac7b293ad0a))
* add MT Manager tool reimplementation extension (.mpe) ([be69774](https://github.com/logm1lo/logm1lo-patches/commit/be697746a858bcd778714aafb6da580a953d86c0)), closes [PKCS#12](https://github.com/logm1lo/PKCS/issues/12)
* add splice-safe VIP wrapper patch (default=false, blocked by resource crash) ([e3e8e1d](https://github.com/logm1lo/logm1lo-patches/commit/e3e8e1dec2d4b7cf829ca47202dcd282ea070e50)), closes [#0xffffffff](https://github.com/logm1lo/logm1lo-patches/issues/0xffffffff)
* **calistree:** support 5.9.1 - versioned Dart hex tables + stdlib reshift ([e1a1476](https://github.com/logm1lo/logm1lo-patches/commit/e1a14766b0b8590da1ef52f79b2abda3265caab7))
* fix MT Manager file-listing data pipeline (loader returns real entries) ([772625b](https://github.com/logm1lo/logm1lo-patches/commit/772625ba37fed30718b4e59939f5c2f1697d852c))
* fix MT Manager white page (feed file browser list + force row drawing) ([e3bec33](https://github.com/logm1lo/logm1lo-patches/commit/e3bec33bd64c371c37ffb6c79adefbc1c9b429d4))
* unlock all 7 MT Manager VIP getters including ۘ() via list-variant fix ([7e70158](https://github.com/logm1lo/logm1lo-patches/commit/7e70158597f99f49c79924e84f1d18c4a38e631b))

## [1.2.0-dev.1](https://github.com/logm1lo/logm1lo-patches/compare/v1.1.0...v1.2.0-dev.1) (2026-09-10)

### Bug Fixes

* default empty feed paths to /storage/emulated/0 in MtTools extension ([fc30463](https://github.com/logm1lo/logm1lo-patches/commit/fc30463132eeb987caad3da4f0ae127d39005240))
* file browser now functional — real rows + working clicks ([ad7d7b4](https://github.com/logm1lo/logm1lo-patches/commit/ad7d7b4efe7864b6455215e634bae7963b23031b))
* force User Agreement flag to true so EULA never re-shows ([7dddc01](https://github.com/logm1lo/logm1lo-patches/commit/7dddc01ffef443a448a48344d1b7109968b16e79))
* make file-browser feed patch apply standalone with splice ([5a166e9](https://github.com/logm1lo/logm1lo-patches/commit/5a166e953ed39a71aa13db7b2663eb7f94d59838))
* make MT Manager tool reimplementation work on-device ([08d634a](https://github.com/logm1lo/logm1lo-patches/commit/08d634acb3d46ec9c2325b6fa96cda421e96ac2a)), closes [PKCS#12](https://github.com/logm1lo/PKCS/issues/12)
* prevent Purchase-VIP / account screen NPE from null native Map getter ([185642e](https://github.com/logm1lo/logm1lo-patches/commit/185642e2a45bb20d5a35858c336e252f67e5bf2d))
* remove stub-native dependencies + clear native flags on patched getters ([f4c154f](https://github.com/logm1lo/logm1lo-patches/commit/f4c154fad7800a8cfa0d243e53c80fa98306d991))
* status-counter feed now fires + diagnostic logging (warm-state shortcut removed) ([ef98b9a](https://github.com/logm1lo/logm1lo-patches/commit/ef98b9a5405711b149ba18c80fd5720fe5a173fd))
* synchronous file-list feed + flat-browser adapter notify ([9ea6815](https://github.com/logm1lo/logm1lo-patches/commit/9ea6815bc21709a6271ca90a299d72123bad8fa0))

### New Features

* add feedFlatIndex helper + flat-browser feed patch (diagnostic) ([28e6e22](https://github.com/logm1lo/logm1lo-patches/commit/28e6e2233038ae7a6f34b62d7355123086c6b7c6))
* add Locket Gold unlock patch (Hermes bytecode override) ([4270412](https://github.com/logm1lo/logm1lo-patches/commit/4270412d5190a1eaf3781055a6ba81dc0efc89b8))
* add MT Manager Morphe patches — native stubbing, key patching, anti-tamper, VIP unlock ([758659c](https://github.com/logm1lo/logm1lo-patches/commit/758659c56a4bf0dcb687c5a4741bc0c476ac8301))
* add MT Manager tool reimplementation extension (.mpe) ([1603c12](https://github.com/logm1lo/logm1lo-patches/commit/1603c127f3376471eface3153a84b4ff9105c27b)), closes [PKCS#12](https://github.com/logm1lo/PKCS/issues/12)
* add splice-safe VIP wrapper patch (default=false, blocked by resource crash) ([5a6a6e1](https://github.com/logm1lo/logm1lo-patches/commit/5a6a6e1c5ec16e6c0036749429377a0fbb6d06eb)), closes [#0xffffffff](https://github.com/logm1lo/logm1lo-patches/issues/0xffffffff)
* **calistree:** support 5.9.1 — versioned Dart hex tables + stdlib reshift ([21d3ac5](https://github.com/logm1lo/logm1lo-patches/commit/21d3ac584bc1020e50a1aa6c2b5efdbf9141a72b))
* fix MT Manager file-listing data pipeline (loader returns real entries) ([c68376b](https://github.com/logm1lo/logm1lo-patches/commit/c68376b0e8ce03bb7b9bc45cde63afdbeef8242b))
* fix MT Manager white page (feed file browser list + force row drawing) ([2324999](https://github.com/logm1lo/logm1lo-patches/commit/23249993f7a1965769ae599e1b8fc7fa8197ff08))
* unlock all 7 MT Manager VIP getters including ۘ() via list-variant fix ([14174e9](https://github.com/logm1lo/logm1lo-patches/commit/14174e99a04c04f54c7d2d0adaf0bc858641a7f8))

## [1.1.0](https://github.com/logm1lo/logm1lo-patches/compare/v1.0.10...v1.1.0) (2026-08-08)

### New Features

* add Cube Solver patches, fix Calistree Dart unlock ([a0d978a](https://github.com/logm1lo/logm1lo-patches/commit/a0d978a743dd6701077d45d264c1cab038bec4ea))

## [1.1.0-dev.1](https://github.com/logm1lo/logm1lo-patches/compare/v1.0.10...v1.1.0-dev.1) (2026-08-08)

### New Features

* add Cube Solver patches, fix Calistree Dart unlock ([a0d978a](https://github.com/logm1lo/logm1lo-patches/commit/a0d978a743dd6701077d45d264c1cab038bec4ea))

## [1.0.10](https://github.com/logm1lo/logm1lo-patches/compare/v1.0.9...v1.0.10) (2026-08-05)

### 🐛 Bug Fixes

* sync lockfile with @semantic-release/git v11 ([772e93d](https://github.com/logm1lo/logm1lo-patches/commit/772e93deb690b6adbfb5e9c624e24467a21783e7))

## [1.0.10-dev.1](https://github.com/logm1lo/logm1lo-patches/compare/v1.0.9...v1.0.10-dev.1) (2026-08-05)

### 🐛 Bug Fixes

* sync lockfile with @semantic-release/git v11 ([772e93d](https://github.com/logm1lo/logm1lo-patches/commit/772e93deb690b6adbfb5e9c624e24467a21783e7))

## [1.0.9-dev.2](https://github.com/logm1lo/logm1lo-patches/compare/v1.0.9-dev.1...v1.0.9-dev.2) (2026-08-05)

### 🐛 Bug Fixes

* sync lockfile with @semantic-release/git v11 ([772e93d](https://github.com/logm1lo/logm1lo-patches/commit/772e93deb690b6adbfb5e9c624e24467a21783e7))

## [1.0.9-dev.1](https://github.com/logm1lo/logm1lo-patches/compare/v1.0.8...v1.0.9-dev.1) (2026-08-05)

### 🐛 Bug Fixes

* add permissions to open_pull_request workflow ([3a4bcdb](https://github.com/logm1lo/logm1lo-patches/commit/3a4bcdb0daeb7ee8e435597d1834a286b44e6afa))
* remove invalid html comments from template yaml files ([7cbeb21](https://github.com/logm1lo/logm1lo-patches/commit/7cbeb21734a2badb00767d5536484e8062537080))

## [1.0.8-dev.3](https://github.com/logm1lo/logm1lo-patches/compare/v1.0.8-dev.2...v1.0.8-dev.3) (2026-08-05)

### 🐛 Bug Fixes

* remove invalid html comments from template yaml files ([7cbeb21](https://github.com/logm1lo/logm1lo-patches/commit/7cbeb21734a2badb00767d5536484e8062537080))

## [1.0.8-dev.2](https://github.com/logm1lo/logm1lo-patches/compare/v1.0.8-dev.1...v1.0.8-dev.2) (2026-08-05)

### 🐛 Bug Fixes

* add permissions to open_pull_request workflow ([3a4bcdb](https://github.com/logm1lo/logm1lo-patches/commit/3a4bcdb0daeb7ee8e435597d1834a286b44e6afa))

## [1.0.8-dev.1](https://github.com/logm1lo/logm1lo-patches/compare/v1.0.7...v1.0.8-dev.1) (2026-08-05)

### 🐛 Bug Fixes

* allow git npm deps and commit lockfile ([e7373d4](https://github.com/logm1lo/logm1lo-patches/commit/e7373d4f03e57990907874f3354e8e30dda07bd6))

# Changelog
