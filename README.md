# 👋🧩 Ample Patches

&nbsp;
## 🩹 Patches list

<!-- PATCHES_START -->
> **[v1.4.0](https://github.com/AmpleReVanced/revanced-patches/releases/tag/v1.4.0)**&nbsp;&nbsp;•&nbsp;&nbsp;`main`&nbsp;&nbsp;•&nbsp;&nbsp;117 patches total
<details>
<summary>📦 Kakao Talk&nbsp;&nbsp;•&nbsp;&nbsp;71 patches</summary>
<br>

**🎯 Supported versions:**

| 26.7.2 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Add Packet Handler](#add-packet-handler) | Adding the Loco Packet Handler allows external applications to handle the app's packets, which may compromise security.<br>Conflicts may occur on some systems. |  |
| [Add Pine Lib](#add-pine-lib) | add libpine.so to the app's native libraries |  |
| [Add settings resources](#add-settings-resources) | Adds Morphe settings layout resources to the app. |  |
| [Add settings tab](#add-settings-tab) | Adds a settings tab to the app. |  |
| [Allow Hide on Any Chat](#allow-hide-on-any-chat) | Users with hiding privileges can hide any chat, including their own messages. |  |
| [Allow Open Chat Managers To Block Members](#allow-open-chat-managers-to-block-members) | Allows open chat room hosts and co-hosts to block regular members from their profiles. |  |
| [Allow direct thread reply editing](#allow-direct-thread-reply-editing) | Shows the edit action when long-pressing your own thread replies in the main chat. |  |
| [Allow invisible characters](#allow-invisible-characters) | Stops zero width and other invisible characters from being stripped out of the chat input and out of the messages shown in a chat room. |  |
| [Allow open chat media bundle](#allow-open-chat-media-bundle) | Removes the open chat restriction that stops webp, gif and other media from being bundled into a single multi-photo message. |  |
| [Allow profile media download](#allow-profile-media-download) | Adds the save option of the profile media viewer to the profile pictures and background images of other people. |  |
| [Allow reply to feed](#allow-reply-to-feed) | Adds a setting to allow swiping feed messages to reply, or to comment where chat room comments are enabled. |  |
| [Always Show Kick Button](#always-show-kick-button) | Always shows the kick button in group member management. |  |
| [Block reactions on deleted or hidden messages](#block-reactions-on-deleted-or-hidden-messages) | Stops reactions, including the double tap gesture, from being sent on messages that the server considers deleted or hidden and that are only still visible because they are kept by a patch. |  |
| [Block replies on deleted or hidden messages](#block-replies-on-deleted-or-hidden-messages) | Stops the swipe gesture, and any reply or comment send, from targeting messages that the server considers deleted or hidden and that are only still visible because they are kept by a patch. |  |
| [Bypass Moat check](#bypass-moat-check) | Add a setting to bypass the KakaoPay Moat integrity check. It stops the native scan from running, so the tamper/root/hook verdict is never computed or reported and KakaoPay is not force-closed. Payments on a modified build are still risky. |  |
| [Bypass input mention limit in non-multichat](#bypass-input-mention-limit-in-non-multichat) | Bypass the limit of input mentions in non-multichat rooms |  |
| [Change model](#change-model) | Changes the device model to supporting subdevice features |  |
| [Change package name](#change-package-name) | Appends ".revanced" to the package name by default. For KakaoTalk only | • Package name<br>• Update permissions<br>• Update providers |
| [Custom branding](#custom-branding) | Customize app branding (name) | • Custom app name |
| [Default external browser](#default-external-browser) | Sets the default external browser for KakaoTalk to the system's default browser. |  |
| [Disable 300+ unread limit](#disable-300-unread-limit) | Always show the real unread count instead of '300+' in chatroom list |  |
| [Disable 99 unread limit](#disable-99-unread-limit) | Skip the 99-cap so unread count shows full value |  |
| [Disable AdFit environment detection](#disable-adfit-environment-detection) | Stops the AdFit ad SDK from reporting a root or emulator verdict to its telemetry and ad request endpoints. |  |
| [Disable ChatRoomAdController](#disable-chatroomadcontroller) | Disables the open-link chat room BizBoard ad controller. |  |
| [Disable Collapse Button](#disable-collapse-button) | Disable collapse button on OpenChatList |  |
| [Disable Community Tab](#disable-community-tab) | Disables Community Tab |  |
| [Disable Friend Feed tab](#disable-friend-feed-tab) | Disables the Friend Feed tab in KakaoTalk. |  |
| [Disable Friend Lists ad](#disable-friend-lists-ad) | Disables the friend tab BizBoard and global-region ads in KakaoTalk. |  |
| [Disable OpenChat feed ad](#disable-openchat-feed-ad) | Disables the OpenChat tab feed ad load and render paths. |  |
| [Disable Pay banner ad](#disable-pay-banner-ad) | Disables the KakaoPay banner ad load and render paths. |  |
| [Disable S2Event](#disable-s2event) | Disables Tracker |  |
| [Disable SDK Tracker](#disable-sdk-tracker) | Disables the SDK Tracker in KakaoTalk. |  |
| [Disable Sentry](#disable-sentry) | Disables Sentry error reporting in KakaoTalk. |  |
| [Disable ShortForm ad](#disable-shortform-ad) | Disables the shortform ad load and render paths. |  |
| [Disable Talk Share Log](#disable-talk-share-log) | Disable talk share log |  |
| [Disable abuse detection report](#disable-abuse-detection-report) | Answers the startup and login integrity challenge with an empty token and a benign Play Integrity failure code instead of a Google-signed token that would expose the re-signed certificate. The report itself is still sent, so the client does not go conspicuously silent on the server's nonce the way suppressing it entirely would. |  |
| [Disable chat room list ad](#disable-chat-room-list-ad) | Disables native and global-region ads in the chat room list. |  |
| [Disable open chat room comments](#disable-open-chat-room-comments) | Adds a setting to disable comments in open chat rooms. |  |
| [Disable verifying signature](#disable-verifying-signature) | Disables the signature verification check that prevents the app from running. |  |
| [Enable Markdown](#enable-markdown) | Render plain text messages with markdown-style formatting. |  |
| [Enable send big text](#enable-send-big-text) | Allows sending big text messages in KakaoTalk. |  |
| [Force enable debug mode](#force-enable-debug-mode) | Enables debug mode in the app. |  |
| [Force enable emoticon plus feature](#force-enable-emoticon-plus-feature) | Force enable emoticon plus feature (Unpurchased emoticon can be sent once per day) |  |
| [Ghost Mode](#ghost-mode) | Don't expose your typing status to the other party. |  |
| [Hide More tab Game tab](#hide-more-tab-game-tab) | Hides the Game tab from the More tab. |  |
| [Hide More tab components](#hide-more-tab-components) | Adds options to hide components from the More tab. |  |
| [Hook Package Manager](#hook-package-manager) | Hooks the Package Manager to bypass integrity checks. |  |
| [Ignore forward restriction](#ignore-forward-restriction) | Ignores the restrictions that stop messages such as KakaoLink from being forwarded to another chatroom. |  |
| [Open profile from open chat feed](#open-profile-from-open-chat-feed) | Opens open chat member profiles from join and leave feed messages. |  |
| [Override feature flag](#override-feature-flag) | Overrides the feature flag to enable the feature. | • Feature flag overrides |
| [Register settings activity](#register-settings-activity) | Registers the Morphe settings activity in the app manifest. |  |
| [Remove BizBoard ads](#remove-bizboard-ads) | Removes the BizBoard ad by forcing its dimensions to 0x0 and visibility to GONE in onMeasure. |  |
| [Remove More tab ad](#remove-more-tab-ad) | Removes the ad from the More tab. |  |
| [Remove OpenLink chat room list ad](#remove-openlink-chat-room-list-ad) | Removes the OpenLink chat room list ad. |  |
| [Remove Short-form Tab](#remove-short-form-tab) | Removes the Short-form tab from the now fragment. |  |
| [Remove feed ad](#remove-feed-ad) | Removes the feed ad from the app. |  |
| [Remove focus ad](#remove-focus-ad) | Removes the focus ad from the app. |  |
| [Remove native ad](#remove-native-ad) | Removes the native ad from the app. |  |
| [Remove shop tab](#remove-shop-tab) | Removes the shop tab from the bottom navigation bar. |  |
| [Restore keyword notification log](#restore-keyword-notification-log) | Restores the removed keyword notification collection view along with its chat list entry. |  |
| [Show chatroom channel ID](#show-chatroom-channel-id) | Shows the channel ID in chatroom settings and copies it when tapping the chat side title. |  |
| [Show deleted, hidden, or edited messages](#show-deleted-hidden-or-edited-messages) | Allows you to see deleted, hidden, and edited message history in chat logs. | • Deleted color<br>• Hidden color |
| [Show message read receipts](#show-message-read-receipts) | Shows the members who have or have not read each chat message. |  |
| [Show messages restricted to mobile](#show-messages-restricted-to-mobile) | Shows the content of AlimTalk, Leverage and Universal messages that are replaced with a placeholder asking to open them on mobile while signed in on a sub device. |  |
| [Spoof App ID](#spoof-app-id) | Spoofs the App ID to bypass integrity checks. (to bypass biometric auth etc.) |  |
| [Spoof apk checksums](#spoof-apk-checksums) | Spoofs the apk checksums to pass integrity checks. |  |
| [Spoof attestation package name](#spoof-attestation-package-name) | Reports the original package name in the attestation report, which otherwise reveals a renamed install. Only the report is affected, so the app keeps using its real package name everywhere else. |  |
| [Spoof installer package name](#spoof-installer-package-name) | Reports Google Play as the install source in the integrity report, which otherwise reveals a sideloaded install. |  |
| [Spoof signature](#spoof-signature) | Spoofs the app signature to pass integrity checks. |  |
| [Strip image metadata](#strip-image-metadata) | Removes EXIF metadata, including the GPS capture location, from images sent in original quality, while keeping the picture and its orientation intact. |  |
| [Version info patch](#version-info-patch) | Patches the version info to include '(ReVanced)' in the version string. |  |

</details>

<details>
<summary>📦 dcinside&nbsp;&nbsp;•&nbsp;&nbsp;26 patches</summary>
<br>

**🎯 Supported versions:**

| 5.3.4 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Add notification shortcut](#add-notification-shortcut) | Adds a notification shortcut button above quick write in the post list. |  |
| [Add settings](#add-settings) | Adds a Morphe settings entry to the DCInside settings screen. |  |
| [Add settings resources](#add-settings-resources) | Adds Morphe settings resources to the app. |  |
| [Adjust image download menu](#adjust-image-download-menu) | Moves the image download menu buttons upward by adding bottom padding. | • Bottom padding |
| [Bypass getText](#bypass-gettext) | Bypasses the integrity check for getText. |  |
| [Change package name](#change-package-name) | Appends ".revanced" to the package name by default. For DCinside only | • Package name<br>• Update permissions<br>• Update providers |
| [Custom branding](#custom-branding) | Customize app branding (name and icon) | • Custom app name<br>• Custom app icon |
| [Dimens Patch](#dimens-patch) | reassigns ad_minimum_height to 0dp to remove ads from the app. |  |
| [Disable Comment Ad](#disable-comment-ad) | Disables the comment ad in the app. |  |
| [Disable DCCon loading](#disable-dccon-loading) | Adds settings to block DCCon image loading in posts and replies. |  |
| [Disable Image Ad](#disable-image-ad) | Disables the image ad in the app. |  |
| [Disable ad controller](#disable-ad-controller) | Disables the ad controller that manages ads in the app. |  |
| [Disable update check](#disable-update-check) | Disables the app's update check. |  |
| [Enable OneStore feature](#enable-onestore-feature) | Enables the OneStore feature in DC Inside app. |  |
| [Filter recently viewed posts by gallery](#filter-recently-viewed-posts-by-gallery) | Adds a gallery filter below the recently viewed posts filters, which narrows the list down to the posts of a single gallery. |  |
| [Gallery watch mode](#gallery-watch-mode) | Keeps gallery auto-refresh active, highlights newly arrived posts, and optionally alerts with sound or vibration while the gallery is open. |  |
| [Hide DC official notices](#hide-dc-official-notices) | Hides official DCInside operator notices from gallery notice lists. |  |
| [Hide Main Ad](#hide-main-ad) | Hides the main ad in the app. |  |
| [Hide home components](#hide-home-components) | Adds settings to hide selected DCInside home screen components. |  |
| [Hide mini gallery cover image](#hide-mini-gallery-cover-image) | Adds a setting to hide the mini gallery cover image above post lists. |  |
| [Hide post list page indicators](#hide-post-list-page-indicators) | Adds a setting to hide the Page N indicators between pages in post lists. |  |
| [Register settings activity](#register-settings-activity) | Registers the Morphe settings activity in the app manifest. |  |
| [Render big DCCon as normal size](#render-big-dccon-as-normal-size) | Adds a setting to render big DCCon at normal DCCon size. |  |
| [Restore old post icons](#restore-old-post-icons) | Adds a setting to restore DCInside 4.7.x post list and read footer icons. |  |
| [Show author identifier](#show-author-identifier) | Adds options to show the author identifier next to the nickname in posts, post lists, comments, and the recently-viewed posts list. The recently-viewed list only shows it for posts opened after this patch is installed. |  |
| [Spoof Signature](#spoof-signature) | Spoofs the app signature to bypass integrity checks. |  |

</details>

<details>
<summary>📦 Chzzk&nbsp;&nbsp;•&nbsp;&nbsp;5 patches</summary>
<br>

**🎯 Supported versions:**

| 3.12.1 | 3.11.1 |
| :---: | :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Auto claim TongPow](#auto-claim-tongpow) | Automatically claims CHZZK TongPow rewards when they become available while watching streams. |  |
| [Disable P2P](#disable-p2p) | Disables CHZZK peer-assisted grid streaming and forces CDN playback. |  |
| [Disable ads](#disable-ads) | Disables CHZZK advertisements, including live stream pre-roll, mid-roll and post-roll ads and clip feed ads. |  |
| [Hide home banners](#hide-home-banners) | Hides promotional banners on the CHZZK home recommend tab, including the top banner carousel and the in-feed event and image banners. |  |
| [Unlock cheat key](#unlock-cheat-key) | Unlocks CHZZK cheat key subscription benefits without an active subscription. |  |

</details>

<details>
<summary>📦 Flexcil&nbsp;&nbsp;•&nbsp;&nbsp;2 patches</summary>
<br>

**🎯 Supported versions:**

| 1.5.0.9 | 1.5.0.8 | 1.5.0.7 | 1.4.4.14 | 1.4.4.12 | 1.4.4.10 | 1.4.4.9 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Bypass login](#bypass-login) | Skips the mandatory account sign-in on launch and opens the app directly. |  |
| [Unlock Premium features](#unlock-premium-features) | Enables app features locked behind the subscription paywall. |  |

</details>

<details>
<summary>📦 SOOP&nbsp;&nbsp;•&nbsp;&nbsp;10 patches</summary>
<br>

**🎯 Supported versions:**

| 8.29.3 | 8.28.5 |
| :---: | :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Disable P2P](#disable-p2p) | Disables SOOP peer-assisted grid streaming. |  |
| [Remove Catch ads](#remove-catch-ads) | Removes the ads inserted between clips in the Catch feed by reporting no ad slots. |  |
| [Remove VOD ads](#remove-vod-ads) | Removes VOD pre-roll ads by reporting every stream as QuickView Plus to the player's own ad-skip check. |  |
| [Remove banner ads](#remove-banner-ads) | Removes the VOD companion and search banner ads. |  |
| [Remove exit dialog ad](#remove-exit-dialog-ad) | Removes the ad area shown in the app-exit confirmation dialog, keeping the dialog itself. |  |
| [Remove intro ads](#remove-intro-ads) | Removes the image, video and cinema intro ad banners shown on launch. |  |
| [Remove list banner ads](#remove-list-banner-ads) | Removes the native banner ads under the live player and in feeds by stopping their ad request, so no banner is fetched or shown. |  |
| [Remove live ads](#remove-live-ads) | Removes the live pre-roll ad by routing every ad decision into the app's own no-video path so the stream starts immediately. |  |
| [Remove live banner ads](#remove-live-banner-ads) | Removes the banner ads shown under the live player by stopping their driver from attaching the banner fragments. |  |
| [Remove main board ads](#remove-main-board-ads) | Removes the native "main board" banner ad shown in feeds and under the live player. |  |

</details>

<details>
<summary>📦 Samsung Keyboard&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

**🎯 Supported versions:**

| 5.9.40.48 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Enable non-One UI ROM support](#enable-non-one-ui-rom-support) | Makes Samsung Keyboard installable and usable on non-One UI ROMs. |  |

</details>

<details>
<summary>🌐 Universal&nbsp;&nbsp;•&nbsp;&nbsp;2 patches</summary>
<br>

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Add shared user ID](#add-shared-user-id) | Adds or updates the sharedUserId attribute in the app's manifest. This allows multiple apps with the same sharedUserId to share data and run in the same process. Use with caution as this can affect app permissions and data isolation. | • Shared User ID |
| [Disable Sentry telemetry](#disable-sentry-telemetry) | Disables Sentry telemetry. See https://sentry.io/for/android/ for more information. |  |

</details>

<!-- PATCHES_END -->

&nbsp;
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
