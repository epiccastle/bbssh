# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [Unreleased]

## [0.2.0] - 2022-10-10

### Fixed
- Replace JNI calls with GraalVM C interop - #3
- NoClassDefFoundError: org/bouncycastle/crypto/params/Ed25519PrivateKeyParameters - #5

### Added
- Windows support (partial) - #4
- Support tcp port forwarding - #2
- Support ssh conf files - #1

## [0.1.0] - 2022-9-28
Initial release.

[Unreleased]: https://github.com/epiccastle/bbssh/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/epiccastle/bbssh/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/epiccastle/bbssh/tree/v0.1.0
