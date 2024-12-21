package com.github.hatoyuze.restarter

import com.github.hatoyuze.restarter.mirai.RestartLifeCommand
import com.github.hatoyuze.restarter.mirai.config.GameConfig
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
import org.jetbrains.skiko.Version
import org.jetbrains.skiko.hostId
import java.io.File


object PluginMain : KotlinPlugin(
    JvmPluginDescription(
        id = "com.github.hatoyuze.restarter.life-restarter",
        name = "LifeRestarter",
        version = "0.3.0"
    ) {
        author("HatoYuze")
        info("""人生重开器.""".trimIndent())
        // author 和 info 可以删除.
    }
) {

    // runtime dependency
    override fun PluginComponentStorage.onLoad() {
        with(jvmPluginClasspath) {
            downloadAndAddToPath(
                classLoader = pluginIndependentLibrariesClassLoader,
                dependencies = listOf("org.jetbrains.skiko:skiko-awt-runtime-${hostId}:${Version.skiko}")
            )
        }
    }

    override fun onEnable() {
        logger.info { "Plugin loaded" }
        CommandManager.INSTANCE.registerCommand(
            RestartLifeCommand
        )
        GameConfig.reload()
        RegisterEventConfig.reload()
        RegisterEventConfig.handleEvent()

        commandPermission
    }

    private val commandPermission by lazy {
        PermissionService.INSTANCE.register(permissionId("command-execute"), "允许执行人生重开器指令", parentPermission)
    }


    override fun onDisable() {
        var i = 0
        File(GameConfig.cachePath).listFiles()?.onEach {
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
