# 👋🧩 Ample Patches

## ❓ About

Ample Patches is a collection of patches mainly developed for apps such as KakaoTalk and DCinside, with a focus on customization and additional functionality.

This project is built on [Morphe](https://morphe.software), which is based on prior work from [ReVanced](https://revanced.app). All modifications made in this repository, together with their dates, are available in the Git history.

## ❕Copyright Notice

This app uses code from Morphe. To learn more, visit https://morphe.software

It also uses code from ReVanced. To learn more, visit https://revanced.app

## 🚀 Get started

To start using this template, follow these steps:

1. [Create a new repository using this template](https://github.com/new?template_name=morphe-patches-template&template_owner=MorpheApp)
2. Set up the [build.gradle.kts](patches/build.gradle.kts) file (Specifically, the [group of the project](patches/build.gradle.kts#L1),
   and the [About](patches/build.gradle.kts#L5-L11))
3. Set up the [README.md](README.md) file[^1] (e.g, title, description, license, summary of the patches
   that are included in the repository), the [issue templates](.github/ISSUE_TEMPLATE)[^2]  and the [contribution guidelines](CONTRIBUTING.md)[^3]
4. Choose a name for your patches project. Keep in mind you must use a unique name that does not imply or suggest authorship by the Morphe open source project.
   See the [NOTICE](NOTICE) for details.
5. (Optional): Add `patches-bundle.png` to the project if you want a custom icon to show in
   Morphe Manager instead of your GitHub profile avatar.

🎉 You are now ready to start creating patches!

## 🧑‍💻 Usage

To develop and release Morphe Patches using this template, some things need to be considered:

- Development starts in feature branches. Once a feature branch is ready, it is squashed and merged into the `dev` branch
- The `dev` branch is merged into the `main` branch once it is ready for release
- Semantic versioning is used to version Morphe Patches.
- [Semantic commit](https://kapeli.com/cheat_sheets/Semantic_Commits.docset/Contents/Resources/Documents/index) messages are used for commits
- Commits on the `dev` branch and `main` branch are automatically released
  via the [release.yml](.github/workflows/release.yml) workflow, which is also responsible for generating the changelog
  and updating the version of Morphe Patches. It is triggered by pushing to the `dev` or `main` branch.
  The workflow uses the `publish` task to publish the release of Morphe Patches
- The `buildAndroid` task is used to build Morphe Patches so that it can be used on Android.
  The `publish` task depends on the `buildAndroid` task, so it will be run automatically when publishing a release.

## 📚 Everything else

Optionally you can include a button/link in this readme that users can click to add your
patches to Morphe (update the links below after creating your new patches repo):

#### How to use these patches

Click here to add these patches to Morphe: https://morphe.software/add-source?github=AmpleReVanced/revanced-patches

Or manually add this repository url as a patch source in Morphe: https://github.com/AmpleReVanced/revanced-patches

### 🛠️ Building

To build Ample Patches,
you can follow the [Morphe documentation](https://github.com/MorpheApp/morphe-documentation).

## 📜 License

Ample Patches are licensed under the [GNU General Public License v3.0](LICENSE)
