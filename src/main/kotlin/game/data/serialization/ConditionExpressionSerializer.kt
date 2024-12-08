package com.github.hatoyuze.restarter.game.data.serialization

import com.github.hatoyuze.restarter.game.data.ComplexConditionExpression
import com.github.hatoyuze.restarter.game.data.ConditionExpression
import com.github.hatoyuze.restarter.game.data.NoCondition
import com.github.hatoyuze.restarter.game.data.SimpleConditionExpression
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ConditionExpressionSerializer : KSerializer<ConditionExpression> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor(
            "com.github.hatoyuze.restarter.game.data.ConditionExpression",
            PrimitiveKind.STRING
        )

    override fun deserialize(decoder: Decoder): ConditionExpression {
        return ConditionExpression.parseExpression(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: ConditionExpression) {
        when (value) {
            is ComplexConditionExpression -> {
                System.err.println("You should not serialize a ComplexConditionExpression instance")
                encoder.encodeString(
                    value.expressions.joinToString(
                        separator = value.operator?.symbol ?: ""
                    ) { "($it)" })
            }

            is SimpleConditionExpression -> SimpleConditionExpression.serializer().serialize(encoder, value)
            NoCondition -> encoder.encodeString("")
        }
    }

}