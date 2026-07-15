# RIVanced Universal Patches

RIVanced Universal Patches is a Morphe patch source for universal and
shared patches ported from [ReVanced](https://github.com/ReVanced/revanced-patches).

## About

This repo only focuses on patches that are useful across apps or support
shared patch behavior.

Most patch logic is ported from ReVanced and adapted for Morphe's patcher
APIs, bundle format, and extension layout.

## Universal Patches

<!-- PATCHES_START EXPANDED -->
> **[v1.0.0-dev.1](https://github.com/rushiranpise/RI-Vanced-Universal-Morphe-Patches/releases/tag/v1.0.0-dev.1)**&nbsp;&nbsp;•&nbsp;&nbsp;`dev`&nbsp;&nbsp;•&nbsp;&nbsp;27 patches total
<details open>
<summary>📦 XYZ app&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

**🎯 Supported versions:**

| 2.0.0 | 1.0.2 |
| :---: | :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Example Patch](#example-patch) | Example patch to start with. |  |

</details>

<details open>
<summary>🌐 Universal&nbsp;&nbsp;•&nbsp;&nbsp;26 patches</summary>
<br>

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Change package name](#change-package-name) | Appends ".rivanced" to the package name by default. Changing the package name of the app can lead to unexpected issues. | • Package name<br>• Update permissions<br>• Update providers |
| [Change version code](#change-version-code) | Changes the version code of the app. This will turn off app store updates and allows downgrading an existing app install to an older app version. | • Version code |
| [Custom network security](#custom-network-security) | Allows trusting custom certificate authorities for a specific domain. | • Target domains<br>• Include subdomains<br>• Custom CA file paths<br>• Trust user added CAs<br>• Trust system CAs<br>• Allow cleartext traffic (HTTP)<br>• Override certificate pinning |
| [Disable PairIP license check](#disable-pairip-license-check) | Disable PairIP license and VM checks. | • Enable VM logging |
| [Disable Play Integrity](#disable-play-integrity) | Prevents apps from using Play Integrity by pretending it is not available. |  |
| [Disable Sentry telemetry](#disable-sentry-telemetry) | Disables Sentry telemetry. See https://sentry.io/for/android/ for more information. |  |
| [Enable Android debugging](#enable-android-debugging) | Enables Android debugging capabilities. This can slow down the app. |  |
| [Enable ROM signature spoofing](#enable-rom-signature-spoofing) | Spoofs the signature via the manifest meta-data "fake-signature". This patch only works with ROMs that support signature spoofing. | • Signature or APK file path |
| [Export all activities](#export-all-activities) | Makes all app activities exportable. |  |
| [Export internal data documents provider](#export-internal-data-documents-provider) | Exports a documents provider that grants access to the internal data directory of this app to file managers and other apps that support the Storage Access Framework. |  |
| [Hex](#hex) | Replaces a hexadecimal patterns of bytes of files in an APK. | • Replacements |
| [Hide ADB status](#hide-adb-status) | Hides enabled development settings and/or ADB. |  |
| [Hide app icon](#hide-app-icon) | Hides the app icon from the Android launcher. |  |
| [Hide mock location](#hide-mock-location) | Prevents the app from knowing the device location is being mocked by a third party app. |  |
| [Override certificate pinning](#override-certificate-pinning) | Overrides certificate pinning, allowing to inspect traffic via a proxy. |  |
| [Predictive back gesture](#predictive-back-gesture) | Enables the predictive back gesture introduced on Android 13. |  |
| [Prevent screenshot detection](#prevent-screenshot-detection) | Removes the registration of all screen capture callbacks. This prevents the app from detecting screenshots. |  |
| [Remove screen capture restriction](#remove-screen-capture-restriction) | Removes the restriction of capturing audio from apps that normally wouldn't allow it. |  |
| [Remove screenshot restriction](#remove-screenshot-restriction) | Removes the restriction of taking screenshots in apps that normally wouldn't allow it. |  |
| [Remove share targets](#remove-share-targets) | Removes share targets like directly sharing to a frequent contact. |  |
| [Set target SDK version 34](#set-target-sdk-version-34) | Changes the target SDK to version 34 (Android 14). For devices running Android 15+, this will disable edge-to-edge display. |  |
| [Spoof Play Age Signals](#spoof-play-age-signals) | Spoofs Google Play data about the user's age and verification status. | • Lower age bound<br>• Upper age bound<br>• User status |
| [Spoof SIM provider](#spoof-sim-provider) | Spoofs information about the SIM card provider. | • ISO-3166-1 alpha-2 country code equivalent for the SIM provider's country code.<br>• MCC+MNC network operator code<br>• Network operator name<br>• ISO-3166-1 alpha-2 country code equivalent for the SIM provider's country code.<br>• MCC+MNC SIM operator code<br>• SIM operator name<br>• IMEI value<br>• MEID value<br>• IMSI (Subscriber ID)<br>• ICCID (SIM Serial)<br>• Phone number |
| [Spoof Wi-Fi connection](#spoof-wi-fi-connection) | Spoofs an existing Wi-Fi connection. |  |
| [Spoof keystore security level](#spoof-keystore-security-level) | Forces apps to see Keymaster and Attestation security levels as 'StrongBox' (Level 2). |  |
| [Spoof root of trust](#spoof-root-of-trust) | Spoofs device integrity states (Locked Bootloader, Verified OS) for apps that perform local certificate attestation. |  |

</details>

<!-- PATCHES_END -->

## Get Started

Add this patch source in Morphe:

```text
https://github.com/rushiranpise/RI-Vanced-Universal-Morphe-Patches
```

Or use the quick add link:

```text
https://morphe.software/add-source?github=rushiranpise/RI-Vanced-Universal-Morphe-Patches
```

## Building

Build the patch bundle with:

```bash
./gradlew :patches:buildAndroid
```

The `.mpp` file will be in:

```text
patches/build/libs
```

For setup help, use the
[Morphe documentation](https://github.com/MorpheApp/morphe-documentation).

## Reporting Bugs

These patches are ports, so some patches may not work exactly as expected in
Morphe yet. If something fails, please report it so it can be fixed.

Open a [bug report](https://github.com/rushiranpise/RI-Vanced-Universal-Morphe-Patches/issues/new/choose)
and include the patch name, RIVanced Universal Patches version or commit,
Morphe version, target app version, and the full error or patcher log.

Reports with logs are much easier to fix.

## Credits

Thanks to:

- [Morphe](https://github.com/MorpheApp/)
- [ReVanced](https://github.com/revanced)
- Contributors
- Everyone who reports bugs and tests patches

## License

RIVanced Universal Patches are licensed under the
[GNU General Public License v3.0](LICENSE).
