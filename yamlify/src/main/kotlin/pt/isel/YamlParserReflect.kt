package pt.isel

import kotlin.reflect.*
import kotlin.reflect.full.createInstance

/**
 * A YamlParser that uses reflection to parse objects.
 */
class YamlParserReflect<T : Any> private constructor(type: KClass<T>) : AbstractYamlParser<T>(type) {

    private val constructor = type.constructors.first()

    private val constructorArgs = getConstructorArgConverters()

    private val simpleTypeInstance : ((Any) -> Any)? = castSimpleType(type)

    private fun getConstructorArgConverters() : Map<String, Pair<KParameter, (Any) -> Any>> {
        val args = mutableMapOf<String, Pair<KParameter, (Any) -> Any>>()
        constructor.parameters.forEach { param ->
            val converter = param to getTypeConverter(param)
            if (param.annotations.any { it is YamlArg }) {
                val yamlArg = param.annotations.first { it is YamlArg } as YamlArg
                args[yamlArg.yamlName] = converter
            }
            args[param.name!!] = converter
        }
        return args
    }

    @Suppress("UNCHECKED_CAST")
    private fun getTypeConverter(param: KParameter): (Any) -> Any {
        if (param.annotations.any { it is YamlConvert }) {
            val customParser = param.annotations.first { it is YamlConvert } as YamlConvert
            val customParserInstance = customParser.parser.createInstance()
            return  { str : Any -> customParserInstance.convertToObject(str.toString())!! }
        }
        val simpleType = castSimpleType(param.type.classifier as KClass<T>)
        if (simpleType != null) {
            return simpleType
        }
        return when (param.type.classifier) {
            List::class -> getIterableValue(param.type)
            else -> {
                val parser = yamlParser(param.type.classifier as KClass<*>)
                return { map : Any -> parser.newInstance(map as Map<String, Any>) }
            }
        }
    }

    companion object {
        /**
         *Internal cache of YamlParserReflect instances.
         */
        private val yamlParsers: MutableMap<KClass<*>, YamlParserReflect<*>> = mutableMapOf()
        /**
         * Creates a YamlParser for the given type using reflection if it does not already exist.
         * Keep it in an internal cache of YamlParserReflect instances.
         */
        @Suppress("UNCHECKED_CAST")
        fun <T : Any> yamlParser(type: KClass<T>): AbstractYamlParser<T> {
            return yamlParsers.getOrPut(type) { YamlParserReflect(type) } as YamlParserReflect<T>
        }
    }
    /**
     * Used to get a parser for other Type using the same parsing approach.
     */
    override fun <T : Any> yamlParser(type: KClass<T>) = YamlParserReflect.yamlParser(type)

    /**
     * Creates a new instance of T through the first constructor
     * that has all the mandatory parameters in the map and optional parameters for the rest.
     */
    @Suppress("UNCHECKED_CAST")
    override fun newInstance(args: Map<String, Any>): T {
        val props = mutableMapOf<KParameter, Any?>()
        if (simpleTypeInstance != null) {
            return simpleTypeInstance.invoke(args[""]!!) as T
        }
        args.keys.forEach { srcProp ->
            val constructorArg = constructorArgs[srcProp] ?: throw IllegalArgumentException("Unknown property $srcProp")
            if (props.containsKey(constructorArg.first))
                throw IllegalArgumentException("Duplicate property $srcProp in constructor.")
            props[constructorArg.first] = constructorArg.second(args[srcProp]!!)
        }
        return constructor.callBy(props)
    }

    @Suppress("UNCHECKED_CAST")
    private fun castSimpleType(type: KClass<T>): ((Any) -> Any)? {
        return when (type) {
            String::class -> { value : String -> value }
            Char::class -> { value : String   -> value.first() }
            Int::class -> { value : String -> value.toInt() }
            Long::class -> { value : String -> value.toLong() }
            Double::class -> { value : String -> value.toDouble() }
            Float::class -> { value : String -> value.toFloat() }
            Boolean::class -> { value : String -> value.toBoolean() }
            Byte::class -> { value : String -> value.toByte() }
            Short::class -> { value : String -> value.toShort() }
            UByte::class -> { value : String -> value.toUByte() }
            UShort::class -> { value : String -> value.toUShort() }
            UInt::class -> { value : String -> value.toUInt() }
            ULong::class -> { value : String -> value.toULong() }
            else -> null
        } as ((Any) -> Any)?
    }

    @Suppress("UNCHECKED_CAST")
    private fun getIterableValue(type: KType): (Any) -> Any {
        val simpleType = castSimpleType(type.arguments.first().type?.classifier as KClass<T>)
        if (simpleType != null) {
            return { list : Any -> (list as List<*>).map { val map = it as Map<*,*>; simpleType(map[""]!!) } }
        }
        if (type.arguments.first().type?.classifier == List::class) {
            val nextList = type.arguments.first().type!!
            val nextConverter = getIterableValue(nextList)
            return { list : Any -> (list as List<*>).map { nextConverter(it!!) } }
        }
        val parser = yamlParser(type.arguments.first().type?.classifier as KClass<*>)
        return { list : Any -> (list as List<*>).map { parser.newInstance(it as Map<String, Any>) } }
    }

}