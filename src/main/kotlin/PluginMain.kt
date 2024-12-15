package com.github.hatoyuze.restarter

import com.github.hatoyuze.restarter.mirai.RestartLifeCommand
import com.github.hatoyuze.restarter.mirai.config.GameConfig
import com.github.hatoyuze.restarter.mirai.config.RegisterEventConfig
import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.permission.AbstractPermitteeId
import net.mamoe.mirai.console.permission.PermissionService
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.utils.info


object PluginMain : KotlinPlugin(
    JvmPluginDescription(
        id = "com.github.hatoyuze.restarter.life-restarter",
        name = "LifeRestarter",
        version = "0.1.1"
    ) {
        author("HatoYuze")
        info("""人生重开器.""".trimIndent())
        // author 和 info 可以删除.
    }
) {
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

    fun hasCustomPermission(sender: User?): Boolean {
        if (sender == null) return true
        return when (sender) {
            is Member -> AbstractPermitteeId.ExactMember(sender.group.id, sender.id)
            else -> AbstractPermitteeId.ExactUser(sender.id)
        }.hasPermission(commandPermission)
    }
    // endregion
}
