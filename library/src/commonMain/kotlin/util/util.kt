package moe.him188.assembly.interpreter.util

import kotlin.reflect.KClass

@Suppress("FunctionName")
internal expect fun <K : Enum<K>, V> EnumMap(clazz: KClass<K>): MutableMap<K, V>