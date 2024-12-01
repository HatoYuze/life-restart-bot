package com.github.hatoyuze.restarter.mirai


import com.github.hatoyuze.restarter.PluginMain
import kotlinx.coroutines.withTimeoutOrNull
import net.mamoe.mirai.console.command.CommandContext
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.event.EventPriority
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.syncFromEvent
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import java.io.File

object ResourceManager {
    internal var isTesting = false

    fun getResource(path: String): String? {
        return if (isTesting) File(System.getProperty("user.dir") + "/src/main/resources/$path").readText()
        else PluginMain.getResource(path)
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
        if (this.subject != it.subject || this.sender != it.sender) return@mapping null
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


