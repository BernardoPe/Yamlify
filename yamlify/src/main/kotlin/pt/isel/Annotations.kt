package pt.isel

import kotlin.reflect.KClass

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class YamlArg(val yamlName: String)

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class YamlConvert(val parser: KClass<out YamlConverter<*>>)

