import io.papermc.hangarpublishplugin.model.Platforms

plugins {
    id("io.papermc.hangar-publish-plugin") version "0.1.0"
    id("com.modrinth.minotaur") version "2.+"
}

dependencies {
    api(projects.viabackwardsCommon)
    api(projects.viabackwardsBukkit)
    api(projects.viabackwardsBungee)
    api(projects.viabackwardsFabric)
    api(projects.viabackwardsSponge)
    api(projects.viabackwardsVelocity)
}

tasks {
    shadowJar {
        archiveFileName.set("ViaBackwards-${project.version}.jar")
        destinationDirectory.set(rootProject.projectDir.resolve("build/libs"))
    }
}

val branch = rootProject.branchName()
val baseVersion = project.version as String
val isRelease = !baseVersion.contains('-')
val isMainBranch = branch == "master"
if (!isRelease || isMainBranch) { // Only publish releases from the main branch
    val suffixedVersion = if (isRelease) baseVersion else baseVersion + "+" + System.getenv("GITHUB_RUN_NUMBER")
    val changelogContent = if (isRelease) {
        "See [GitHub](https://github.com/ViaVersion/ViaBackwards) for release notes."
    } else {
        val commitHash = rootProject.latestCommitHash()
        "[$commitHash](https://github.com/ViaVersion/ViaBackwards/commit/$commitHash) ${rootProject.latestCommitMessage()}"
    }
    modrinth {
        // val snapshotVersion = rootProject.parseMinecraftSnapshotVersion(project.version as String)
        val mcVersions: List<String> = (property("mcVersions") as String)
                .split(",")
                .map { it.trim() }
        //.let { if (snapshotVersion != null) it + snapshotVersion else it } // We're usually too fast for modrinth

        token.set(System.getenv("MODRINTH_TOKEN"))
        projectId.set("viabackwards")
        versionType.set(if (isRelease) "release" else if (isMainBranch) "beta" else "alpha")
        versionNumber.set(suffixedVersion)
        versionName.set(suffixedVersion)
        changelog.set(changelogContent)
        uploadFile.set(tasks.shadowJar.flatMap { it.archiveFile })
        gameVersions.set(mcVersions)
        loaders.add("fabric")
        loaders.add("paper")
        loaders.add("folia")
        loaders.add("velocity")
        loaders.add("bungeecord")
        loaders.add("sponge")
        autoAddDependsOn.set(false)
        detectLoaders.set(false)
        dependencies {
            optional.project("viaversion")
            optional.project("viafabric")
            optional.project("viafabricplus")
        }
    }

    hangarPublish {
        publications.register("plugin") {
            version.set(suffixedVersion)
            id.set("ViaBackwards")
            channel.set(if (isRelease) "Release" else if (isMainBranch) "Snapshot" else "Alpha")
            changelog.set(changelogContent)
            apiKey.set(System.getenv("HANGAR_TOKEN"))
            platforms {
                register(Platforms.PAPER) {
                    jar.set(tasks.shadowJar.flatMap { it.archiveFile })
                    platformVersions.set(listOf(property("mcVersionRange") as String))
                    dependencies {
                        hangar("ViaVersion") {
                            required.set(true)
                        }
                    }
                }
                register(Platforms.VELOCITY) {
                    jar.set(tasks.shadowJar.flatMap { it.archiveFile })
                    platformVersions.set(listOf(property("velocityVersion") as String))
                    dependencies {
                        hangar("ViaVersion") {
                            required.set(true)
                        }
                    }
                }
                register(Platforms.WATERFALL) {
                    jar.set(tasks.shadowJar.flatMap { it.archiveFile })
                    platformVersions.set(listOf(property("waterfallVersion") as String))
                    dependencies {
                        hangar("ViaVersion") {
                            required.set(true)
                        }
                    }
                }
            }
        }
    }
}
