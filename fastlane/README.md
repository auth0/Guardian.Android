fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

## Android

### android test

```sh
[bundle exec] fastlane android test
```

Runs all the unit tests

### android lint

```sh
[bundle exec] fastlane android lint
```

Runs Android Lint on the :guardian library module

### android coverage

```sh
[bundle exec] fastlane android coverage
```

Generates a JaCoCo coverage report for :guardian (informational only, no threshold/gate).

Accepts an inert `build_type:` for contract-parity with the App; the SDK has a single JaCoCo task.

### android build

```sh
[bundle exec] fastlane android build
```

Assembles the :guardian library's release AAR (guardian-release.aar).

Accepts an inert `build_type:` for contract-parity; a library ships a single release AAR.

### android publish_maven

```sh
[bundle exec] fastlane android publish_maven
```

Uploads :guardian to Maven Central staging via com.vanniktech.maven.publish.

Requires RELEASE_CONTEXT=true — only set inside the `publish` job of release.yml.

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
