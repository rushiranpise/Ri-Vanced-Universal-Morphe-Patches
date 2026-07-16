# Contributing To RIVanced

RIVanced ports ReVanced patches to the Morphe ecosystem. Contributions should
respect the original ReVanced work while keeping this repository buildable,
Morphe-compatible, and free of copied ReVanced branding assets.

## Before Opening An Issue

- Search existing issues first.
- Include the target app name and version.
- Include the patch list you used.
- Include logs when reporting patcher or runtime failures.

## Before Opening A Pull Request

- Base changes on the development branch used by this repository.
- Keep changes focused and easy to review.
- Preserve upstream ReVanced credits when porting upstream work.
- Do not add ReVanced logos or other protected branding assets.
- Verify the package with `./gradlew buildAndroid`.

## Patch Guidelines

Accepted changes should improve Morphe compatibility, fix broken ports, update
patches from upstream ReVanced, or add clearly useful app patches.

Avoid patches whose only purpose is payment circumvention or malicious behavior.

## Credits

Most patch logic originates from ReVanced contributors. RIVanced contribution
work is about adapting that work for Morphe and keeping the port healthy.
