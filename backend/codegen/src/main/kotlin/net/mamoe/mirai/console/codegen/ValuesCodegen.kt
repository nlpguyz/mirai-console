/*
 * Copyright 2020 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/master/LICENSE
 */
@file:Suppress("ClassName", "unused")

package net.mamoe.mirai.console.codegen

import org.intellij.lang.annotations.Language
import java.io.File

fun main() {
    println(File("").absolutePath) // default project base dir

    File("backend/mirai-console/src/main/kotlin/net/mamoe/mirai/console/setting/_Value.kt").apply {
        createNewFile()
    }.writeText(genPublicApi())
}

internal val COPYRIGHT = """
/*
 * Copyright 2020 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/master/LICENSE
 */
""".trim()

internal val NUMBERS = listOf(
    "Int",
    "Short",
    "Byte",
    "Long",
    "Float",
    "Double"
)

internal val UNSIGNED_NUMBERS = listOf(
    "UInt",
    "UShort",
    "UByte",
    "ULong"
)

internal val OTHER_PRIMITIVES = listOf(
    "Boolean",
    "Char",
    "String"
)

fun genPublicApi() = buildString {
    fun appendln(@Language("kt") code: String) {
        this.appendln(code.trimIndent())
    }

    appendln(COPYRIGHT.trim())
    appendln()
    appendln(
        """
            package net.mamoe.mirai.console.setting

            import kotlinx.serialization.KSerializer
            import kotlin.properties.ReadWriteProperty
            import kotlin.reflect.KProperty
        """
    )
    appendln()
    appendln(
        """
            /**
             * !!! This file is auto-generated by backend/codegen/src/main/kotlin/net.mamoe.mirai.console.codegen.ValuesCodegen.kt 
             * !!! for better performance
             * !!! DO NOT MODIFY THIS FILE MANUALLY
             */
        """
    )
    appendln()

    appendln(
        """
sealed class Value<T : Any> : ReadWriteProperty<Setting, T> {
    abstract var value: T

    /**
     * 用于更新 [value] 的序列化器
     */
    abstract val serializer: KSerializer<T>
    override fun getValue(thisRef: Setting, property: KProperty<*>): T = value
    override fun setValue(thisRef: Setting, property: KProperty<*>, value: T) {
        this.value = value
    }

    override fun equals(other: Any?): Boolean {
        if (other==null)return false
        if (other::class != this::class) return  false
        other as Value<*>
        return other.value == this.value
    }

    override fun hashCode(): Int = value.hashCode()
}
        """
    )
    appendln()

    // PRIMITIVES

    appendln(
        """
            sealed class PrimitiveValue<T : Any> : Value<T>()

            sealed class NumberValue<T : Number> : Value<T>()
        """
    )

    for (number in NUMBERS) {
        val template = """
        abstract class ${number}Value internal constructor() : NumberValue<${number}>()
    """

        appendln(template)
    }

    appendln()

    for (number in OTHER_PRIMITIVES) {
        val template = """
        abstract class ${number}Value internal constructor() : PrimitiveValue<${number}>()
    """

        appendln(template)
    }

    appendln()

    // ARRAYS

    appendln(
        """
            // T can be primitive array or typed Array 
            sealed class ArrayValue<T : Any> : Value<T>()
    """
    )

    //   PRIMITIVE ARRAYS
    appendln(
        """
            sealed class PrimitiveArrayValue<T : Any> : ArrayValue<T>()
        """
    )
    appendln()

    for (number in (NUMBERS + OTHER_PRIMITIVES).filterNot { it == "String" }) {
        appendln(
            """
            abstract class ${number}ArrayValue internal constructor() : PrimitiveArrayValue<${number}Array>(), Iterable<${number}> {
                override fun iterator(): Iterator<${number}> = this.value.iterator()
            }
    """
        )
        appendln()
    }

    appendln()

    //   TYPED ARRAYS

    appendln(
        """
            sealed class TypedPrimitiveArrayValue<E> : ArrayValue<Array<E>>() , Iterable<E>{
                override fun iterator() = this.value.iterator()
            }
    """
    )
    appendln()

    for (number in (NUMBERS + OTHER_PRIMITIVES)) {
        appendln(
            """
            abstract class Typed${number}ArrayValue internal constructor() : TypedPrimitiveArrayValue<${number}>()
    """
        )
    }

    appendln()

    //   TYPED LISTS / SETS
    for (collectionName in listOf("List", "Set")) {

        appendln(
            """
            sealed class ${collectionName}Value<E> : Value<${collectionName}<E>>(), ${collectionName}<E>
    """
        )

        for (number in (NUMBERS + OTHER_PRIMITIVES)) {
            val template = """
            abstract class ${number}${collectionName}Value internal constructor() : ${collectionName}Value<${number}>()
    """

            appendln(template)
        }

        appendln()
        // SETTING
        appendln(
            """
        abstract class Setting${collectionName}Value<T: Setting> internal constructor() : Value<${collectionName}<T>>(), ${collectionName}<T>
    """
        )
        appendln()
    }

    // SETTING VALUE

    appendln(
        """
            abstract class SettingValue<T : Setting> internal constructor() : Value<T>()
        """
    )

    appendln()

    // MUTABLE LIST / MUTABLE SET
    for (collectionName in listOf("List", "Set")) {
        appendln(
            """
        abstract class Mutable${collectionName}Value<T : Any> internal constructor() : Value<Mutable${collectionName}<Value<T>>>(), Mutable${collectionName}<T>
    """
        )

        appendln()

        for (number in (NUMBERS + OTHER_PRIMITIVES)) {
            appendln(
                """
        abstract class Mutable${number}${collectionName}Value internal constructor() : Value<Mutable${collectionName}<${number}>>(), Mutable${collectionName}<${number}>
    """
            )
        }

        appendln()
        // SETTING
        appendln(
            """
        abstract class MutableSetting${collectionName}Value<T: Setting> internal constructor() : Value<Mutable${collectionName}<T>>(), Mutable${collectionName}<T>
    """
        )
        appendln()
    }

    appendln()
    // DYNAMIC

    appendln(
        """
        /**
         * 只引用这个对象, 而不跟踪其成员.
         * 仅适用于基础类型, 用于 mutable list/map 等情况; 或标注了 [Serializable] 的类.
         */
        abstract class DynamicReferenceValue<T : Any> : Value<T>()
    """
    )
}