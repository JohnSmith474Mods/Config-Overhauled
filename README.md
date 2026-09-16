# Config Overhauled

Welcome to Config Overhauled, a free to use multi-loader configuration library for Minecraft mod development! It provides a structured API for property definition, automated graphical interface generation, and network state synchronization for Fabric, Forge, or NeoForge environments.

The framework replaces manual interface construction and data synchronization with a declarative builder pattern. Properties are constrained by operational scopes (CLIENT, GLOBAL, LEVEL) that dictate data serialization targets and client-server synchronization authority. Built-in utilities handle dynamic GUI rendering and localization key export to eliminate structural boilerplate.

## Repository Structure & Branching

This default branch is the central management hub. It hosts important structural documentation, licensing details, and GitHub workflow configurations, but you won't find any actual source code here.

Because Minecraft modding moves through a linear path with minor API changes between versions, all of our source code lives exclusively in dedicated, version-specific branches. We use a linear progression model: whenever we make a change, we merge it upward through each successive version branch. This makes it a breeze to propagate bug fixes and patches smoothly.

## Contributions & Pull Requests

Talk is cheap, send patches! If you're submitting a pull request, please make sure to target the **earliest active version branch** that your change applies to. Since our branches flow upwards, your changes will naturally make their way to the newer versions.

Please note that all contributions require a signed Contributor License Agreement (CLA). Our automated workflow will prompt you to sign this agreement when you open your first pull request. We highly recommend reviewing the [CLA](CLA.md) before you begin any substantial work.

*A quick note:* Please ensure you do not submit code changes to this management branch, as any such pull requests will be rejected.

## Version Support Matrix

Here is a quick overview of our current branches and their support status. Right now, all listed versions are actively supported!

| Branch                  | Game Version Range   | Support Status |
| ----------------------- | -------------------- | -------------- |
| [1.21](tree/1.21)       | 1.21 - 1.21.1        | Active         |
| [1.21.2](tree/1.21.2)   | 1.21.2 - 1.21.3      | Active         |
| [1.21.4](tree/1.21.4)   | 1.21.4               | Active         |
| [1.21.5](tree/1.21.5)   | 1.21.5               | Active         |
| [1.21.6](tree/1.21.6)   | 1.21.6 - 1.21.8      | Active         |
| [1.21.9](tree/1.21.9)   | 1.21.9 - 1.21.10     | Active         |
| [1.21.11](tree/1.21.11) | 1.21.11              | Active         |
| [26.1](tree/26.1)       | 26.1 - 26.1.2        | Active         |

## Third-Party Licenses & Credits

This project contains or relies on third-party software components. Detailed licensing information, including full license texts for bundled dependencies, is located in the [third-party/](third-party/) directory.

* **[NightConfig](https://github.com/TheElectronWill/night-config)** by [TheElectronWill](https://github.com/TheElectronWill) is bundled under the GNU Lesser General Public License v3.0 (LGPL-3.0).
* **[MultiLoader-Template](https://github.com/jaredlll08/MultiLoader-Template)** by [jaredlll08](https://modrinth.com/user/jaredlll08) is utilized as the structural foundation for the cross-platform build environment under the CC0 1.0 Universal license.