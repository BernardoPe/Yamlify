package pt.isel

import java.io.File
import java.io.Reader
import kotlin.reflect.KClass

abstract class AbstractYamlParser<T : Any>(private val type: KClass<T>) : YamlParser<T> {
    /**
     * Used to get a parser for other Type using this same parsing approach.
     */
    abstract fun <T : Any> yamlParser(type: KClass<T>): AbstractYamlParser<T>

    /**
     * Creates a new instance of T through the first constructor
     * that has all the mandatory parameters in the map and optional parameters for the rest.
     */
    abstract fun newInstance(args: Map<String, Any>): T

    final override fun parseFolderEager(folder: String): List<T> {
        return File(folder)
            .listFiles()
            ?.filter { it.extension == "yaml"}
            ?.sortedBy { it.name }
            ?.map { parseObject(it.reader()) } ?: emptyList()
    }



    final override fun parseFolderLazy(folder: String): Sequence<T> {
        return sequence {
            File(folder)
                .listFiles()
                ?.filter { it.extension == "yaml"}
                ?.sortedBy { it.name }
                ?.forEach { yield(parseObject(it.reader())) }
            }
    }

    @Suppress("UNCHECKED_CAST")
    final override fun parseSequence(yaml: Reader): Sequence<T> = sequence {
        val iter = yaml.buffered().lines().iterator()
        var listIndentation: Int? = null
        var start = true

        while (iter.hasNext()) {
            val lines = mutableListOf<String>()
            var next: T? = null

            while (iter.hasNext()) {
                val line = iter.next()

                if (line.isBlank()) continue

                if (listIndentation == null) {
                    listIndentation = getIndentation(line)
                }

                if (line[listIndentation] == '-') {
                    if (isScalar(line)) {
                        next = newInstance(mapOf("" to line.split("-").last().fastTrim()))
                        break
                    }
                    if (start) { // Skip the first separator
                        start = false
                        continue
                    } else {
                        break
                    }
                }
                lines.add(line)
            }

            if (lines.isNotEmpty()) {
                next = if (isList(lines)) {
                    createObjectsFromList(getListValues(lines)) as T
                } else {
                    newInstance(getObjectValues(lines))
                }
            }

            if (next != null) {
                yield(next)
            }
        }
    }

    final override fun parseObject(yaml: Reader): T =
        yaml.useLines {
            newInstance(
                getObjectValues(
                    it.filter { line ->
                        line.isNotBlank()
                    }.toList()
                )
            )
        }

    final override fun parseList(yaml: Reader): List<T> =
        yaml.useLines {
            createObjectsFromList(
                getListValues(
                    it.filter { line ->
                        line.isNotBlank()
                    }.toList()
                )
            )
        }

    @Suppress("UNCHECKED_CAST")
    private fun createObjectsFromList(objectsList: List<Any>): List<T> {
        return objectsList.map {
            if (it is Map<*, *>) {
                newInstance(it as Map<String, Any>)
            } else {
                createObjectsFromList(it as List<Any>) as T
            }
        }
    }

    // Get the list of objects from yaml text
    private fun getListValues(input: List<String>): List<Any> {
        if (input.isEmpty()) return emptyList()
        return getObjectList(input).map { lines ->
            if (isList(lines)) {
                getListValues(lines)
            } else {
                getObjectValues(lines)
            }
        }
    }

    private fun isList(lines: List<String>): Boolean {
        val objIndentation = getIndentation(lines.first())
        return lines.count { it[objIndentation] == '-' } > 1
    }

    private fun getObjectValues(lines: List<String>): Map<String, Any> {
        if (lines.isEmpty()) throw IllegalArgumentException("Empty object")
        val map = mutableMapOf<String, Any>()
        var i = 0
        val objIndentation = getIndentation(lines.first())

        while (i < lines.size) {
            val line = lines[i++]

            val indentation = getIndentation(line)

            if (indentation != objIndentation) throw IllegalArgumentException("Invalid indentation at: ${line.fastTrim()}")

            val parts = getLineParts(line)

            when {
                map.containsKey(parts[0]) -> throw IllegalArgumentException("Duplicate key ${parts[0]} for ${type.simpleName}")
                parts[1].isNotEmpty() -> map[parts[0]] = parts[1]
                isScalar(line) -> map[""] = line.split("-", limit = 2).last().fastTrim()
                else -> {
                    val nextLine = lines[i]
                    val indentedLines = getLinesSequence(lines, i, objIndentation)
                    map[parts[0]] = if (nextLine.contains("-")) {
                        getListValues(indentedLines)
                    } else {
                        getObjectValues(indentedLines)
                    }
                    i += indentedLines.size
                }
            }
        }
        return map
    }

    private fun getLineParts(line: String): List<String> {
        val parts = line.split(":", limit = 2)
        return if (parts.size == 1) {
            listOf(parts[0].fastTrim(), "")
        } else {
            listOf(parts[0].fastTrim(), parts[1].fastTrim())
        }
    }

    private fun String.fastTrim(): String {
        var start = 0
        var end = length - 1
        while (start <= end && this[start] == ' ') start++
        while (end >= start && this[end] == ' ') end--
        return this.substring(start, end + 1)
    }

    private fun isScalar(line: String): Boolean {
        var dashFound = false
        for (i in line.indices) {
            if (line[i] == ' ') {
                continue
            }
            if (line[i] == '-') {
                dashFound = true
            } else {
                return dashFound
            }
        }
        return false
    }

    private fun getLinesSequence(lines: List<String>, start: Int, indentation: Int): List<String> {
        val result = mutableListOf<String>()
        var i = start
        while (i < lines.size) {
            val line = lines[i++]
            if (getIndentation(line) == indentation) {
                return result
            }
            result.add(line)
        }
        return result
    }

    private fun getObjectList(lines: List<String>): List<List<String>> {

        val objIndentation = getIndentation(lines.first())
        val objects = mutableListOf<List<String>>()
        var currObject = mutableListOf<String>()

        lines.forEach { line ->

            val lineIndentation = getIndentation(line)

            if ((lineIndentation - objIndentation) % 2 != 0) {
                throw IllegalArgumentException("Invalid indentation at $line")
            }

            if (line[objIndentation] == '-') {
                if (isScalar(line)) {
                    objects.add(listOf(line))
                } else {
                    if (currObject.isNotEmpty()) {
                        objects.add(currObject.toList())
                        currObject = mutableListOf()
                    }
                }
            } else {
                currObject.add(line)
            }
        }

        if (currObject.isNotEmpty()) {
            objects.add(currObject.toList())
        }

        return objects
    }

    private fun getIndentation(line: String): Int {
        for (i in line.indices) {
            if (line[i] != ' ') {
                return i
            }
        }
        return line.length
    }

}
