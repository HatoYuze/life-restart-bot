package com.github.hatoyuze.restarter.mirai


import com.github.hatoyuze.restarter.PluginMain
import com.github.hatoyuze.restarter.mirai.config.GameConfig
import kotlinx.coroutines.withTimeoutOrNull
import net.mamoe.mirai.console.command.CommandContext
import net.mamoe.mirai.console.plugin.ResourceContainer
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.event.EventPriority
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.syncFromEvent
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import java.io.File
import java.io.InputStream

object ResourceManager : ResourceContainer {
    internal var isTesting = false

    override fun getResource(path: String): String? {
        return if (isTesting) File(System.getProperty("user.dir") + "/src/main/resources/$path").readText()
        else PluginMain.getResource(path)
    }

    override fun getResourceAsStream(path: String): InputStream {
        return if (isTesting) File(System.getProperty("user.dir") + "/src/main/resources/$path").inputStream()
        else PluginMain.getResourceAsStream(path)!!
    }

    fun newCacheFile(name: String): File {
        val cacheFile =
            if (isTesting) System.getProperty("java.io.tmpdir") else GameConfig.cachePath

        val dirFile = File(cacheFile)
        if (!dirFile.exists()) {
            dirFile.mkdirs()
        }
        val newFile = File(dirFile, name)
        newFile.createNewFile()
        return newFile
    }
}


suspend fun CommandContext.quote(message: Message) =
    sender.subject?.sendMessage(message + originalMessage.quote()) ?: sender.sendMessage(message)

suspend fun CommandContext.quote(message: String) =
    sender.subject?.sendMessage(PlainText(message) + originalMessage.quote()) ?: sender.sendMessage(message)


/**
 * 构造一条 [ForwardMessage]
 *
 * @see ForwardMessageBuilder 查看 DSL 帮助
 * @see ForwardMessage 查看转发消息说明
 */
@JvmSynthetic
suspend inline fun CommandContext.sendForwardMessage(
    displayStrategy: ForwardMessage.DisplayStrategy = ForwardMessage.DisplayStrategy,
    block: ForwardMessageBuilder.() -> Unit,
) = subject.sendMessage(buildForwardMessage(subject, displayStrategy, block))


suspend fun CommandContext.nextMessageMemberOrNull(
    timeoutMillis: Long = -1,
    priority: EventPriority = EventPriority.NORMAL,
    filter: (MessageEvent) -> Boolean,
): MessageChain? {
    val eventChannel = GlobalEventChannel.parentScope(PluginMain)
    val mapping: suspend (MessageEvent) -> MessageEvent? = mapping@{
        val isSameContext = this.sender.user?.id == it.sender.id && this.subject.id == it.subject.id
        if (!isSameContext) return@mapping null
        if (!filter(it)) return@mapping null
        it
    }
    return if (timeoutMillis == -1L) {
        eventChannel.syncFromEvent<MessageEvent, MessageEvent>(priority, mapping)
    } else {
        withTimeoutOrNull(timeoutMillis) {
            eventChannel.syncFromEvent<MessageEvent, MessageEvent>(priority, mapping)
        }
    }?.message
}

val CommandContext.fromContact get() = (sender.subject as Contact).id
val CommandContext.from get() = sender.user
val CommandContext.subject: Contact get() = sender.subject!!


