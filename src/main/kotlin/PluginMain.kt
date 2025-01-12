package com.github.hatoyuze.restarter

import com.github.hatoyuze.restarter.mirai.RestartLifeCommand
import com.github.hatoyuze.restarter.mirai.config.CommandLimitData
import com.github.hatoyuze.restarter.mirai.config.GameConfig
import com.github.hatoyuze.restarter.mirai.config.GameConfig.ifNull
import com.github.hatoyuze.restarter.mirai.config.GameSaveData
import com.github.hatoyuze.restarter.mirai.config.RegisterEventConfig
import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.extension.PluginComponentStorage
import net.mamoe.mirai.console.permission.AbstractPermitteeId
import net.mamoe.mirai.console.permission.PermissionService
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.utils.info
import java.io.File
import kotlin.io.path.pathString


object PluginMain : KotlinPlugin(
    JvmPluginDescription(
        id = "com.github.hatoyuze.restarter.life-restarter",
        name = "LifeRestarter",
        version = "0.5.0"
    ) {
        author("HatoYuze")
        info("""人生重开器.""".trimIndent())
        // author 和 info 可以删除.
    }
) {

    // runtime dependency
    override fun PluginComponentStorage.onLoad() {
        with(jvmPluginClasspath) {
            val osName = System.getProperty("os.name")
            val targetOs = when {
                osName == "Mac OS X" -> "macos"
                osName.startsWith("Win") -> "windows"
                osName.startsWith("Linux") -> "linux"
                else -> error("Unsupported OS: $osName")
            }
            val osArch = System.getProperty("os.arch")
            val targetArch = when (osArch) {
                "x86_64", "amd64" -> "x64"
                "aarch64" -> "arm64"
                else -> error("Unsupported arch: $osArch")
            }

            downloadAndAddToPath(
                classLoader = pluginIndependentLibrariesClassLoader,
                dependencies = listOf("org.jetbrains.skiko:skiko-awt-runtime-${targetOs}-$targetArch:0.8.18")
            )
        }
    }

    override fun onEnable() {
        logger.info { "Plugin loaded" }
        CommandManager.INSTANCE.registerCommand(
            RestartLifeCommand
        )
        RegisterEventConfig.reload()
        GameConfig.reload()
        RegisterEventConfig.handleEvent()
        CommandLimitData.reload()

        GameSaveData.reload()
        commandPermission
    }

    val commandPermission by lazy {
        PermissionService.INSTANCE.register(permissionId("command-execute"), "允许执行人生重开器指令", parentPermission)
    }


    override fun onDisable() {
        var i = 0
        File(GameConfig.cachePath.ifNull(PluginMain.dataFolderPath.pathString)).listFiles()?.onEach {
            if (!(it.name.endsWith("png") || it.name.endsWith("jpg")))
                return@onEach

            it.delete()
            i++
        } ?: error("缓存目录并不是有效的文件夹")
        logger.info("成功清除 $i 个缓存文件")
    }

    fun hasCustomPermission(sender: User?): Boolean {
        if (sender == null) return true
        return when (sender) {
            is Member -> AbstractPermitteeId.ExactMember(sender.group.id, sender.id)
            else -> AbstractPermitteeId.ExactUser(sender.id)
        }.hasPermission(commandPermission)
    }
    // endregion
}
