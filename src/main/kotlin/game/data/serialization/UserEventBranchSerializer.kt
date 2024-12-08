package com.github.hatoyuze.restarter.game.data.serialization

import com.github.hatoyuze.restarter.game.data.ConditionExpression
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

typealias ReferEventId = Int

internal object UserEventBranchSerializer :
    KSerializer<List<Pair<ConditionExpression, ReferEventId>>> by ListSerializer(BranchElementSerializer) {
    object BranchElementSerializer : KSerializer<Pair<ConditionExpression, ReferEventId>> {
        override val descriptor: SerialDescriptor
            get() = PrimitiveSerialDescriptor("com.github.hatoyuze.restarter.game.data.\$Branch", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): Pair<ConditionExpression, ReferEventId> {
            val content = decoder.decodeString()
            val referSymbol = content.lastIndexOf(':')
            return if (referSymbol == -1) {
                ConditionExpression.parseExpression(content) to -1

            } else {
                ConditionExpression.parseExpression(
                    content.substring(0, referSymbol)
                ) to content.substring(referSymbol + 1).toInt()
            }
        }

        override fun serialize(encoder: Encoder, value: Pair<ConditionExpression, ReferEventId>) {
            encoder.encodeString("${ConditionExpressionSerializer.buildOriginString(value.first)}:${value.second}")
        }

    }
}


