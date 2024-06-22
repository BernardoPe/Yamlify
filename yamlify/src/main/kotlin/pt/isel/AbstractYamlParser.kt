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

    final override fun parseFolderEager(path: String): List<T> {
        return File(path)
            .listFiles()
            ?.filter { it.extension == "yaml"}
            ?.sortedBy { it.name }
            ?.map { parseObject(it.reader()) } ?: emptyList()
    }



    final override fun parseFolderLazy(path: String): Sequence<T> {
        return sequence {
            File(path)
                .listFiles()
                ?.filter { it.extension == "yaml"}
                ?.sortedBy { it.name }
                ?.forEach { yield(parseObject(it.reader())) }
            }
    }

    final override fun parseSequence(yaml: Reader): Sequence<T> {
        return object : Sequence<T> {
            override fun iterator(): Iterator<T> {
                return object : Iterator<T> {

                    val iter = yaml.buffered().lines().iterator()
                    var listIndentation: Int? = null
                    var next: T? = null
                    var start = true

                    fun getNextObject() {

                        if (next != null) return

                        val lines = mutableListOf<String>()

                        while (iter.hasNext()) {

                            val line = iter.next()

                            if (line.isBlank()) continue

                            if (listIndentation == null) {
                                listIndentation = getIndentation(line)
                            }

                            if (line[listIndentation!!] == '-') {
                                if (isScalar(line)) {
                                    next = newInstance(mapOf("" to line.split("-").last().fastTrim()))
                                    return
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

                        if (lines.isEmpty()) return

                        next = if (isList(lines)) {
                            createObjectsFromList(getListValues(lines)) as T
                        } else {
                            newInstance(getObjectValues(lines))
                        }

                    }

                    override fun hasNext(): Boolean {
                        getNextObject()
                        return next != null
                    }

                    override fun next(): T {
                        if (!hasNext()) throw NoSuchElementException()
                        val result = next!!
                        next = null
                        return result
                    }
                }
            }
        }
    }

    // Parse the yaml text into an object
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

    // Parse the yaml text into a list of objects
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

    // Creates a list of defined objects from a list of objects
    private fun createObjectsFromList(objectsList: List<Any>): List<T> {
        return objectsList.map {
            // Check if the object is a map
            if (it is Map<*, *>) {
                // If it is a map, create a new instance of the object
                newInstance(it as Map<String, Any>)
            } else {
                // If it is a list, execute the function recursively
                // When it is a nested list
                createObjectsFromList(it as List<Any>) as T
            }
        }
    }

    // Get the list of objects from yaml text
    private fun getListValues(input: List<String>): List<Any> {
        if (input.isEmpty()) return emptyList()
        // Get the list of objects from the yaml text
        val yamlObjects = getObjectList(input)
        // Iterate over the list of objects
        return yamlObjects.map { lines ->
            // Check if the object is a list or a single object
            if (isList(lines))
            // If it is a list, recursively call getListValues
            // to handle the nested list
                getListValues(lines)
            else
            // If it is a single object, call getObjectValues
                getObjectValues(lines)
        }
    }

    private fun isList(lines: List<String>): Boolean {
        val objIndentation = getIndentation(lines.first())
        return lines.count { it[objIndentation] == '-' } > 1
    }

    // Get the values of the object from the yaml text
    private fun getObjectValues(lines: List<String>): Map<String, Any> {

        if (lines.isEmpty()) throw IllegalArgumentException("Empty object")
        // Create a mutable map to hold the results
        val map = mutableMapOf<String, Any>()
        // index to iterate over the lines
        var i = 0
        // Get the indentation of the object
        // In other words, the number of spaces before the first key
        val objIndentation = getIndentation(lines.first())

        while (i < lines.size) {
            // Get the current line
            val line = lines[i++]

            // Get the indentation of the current line
            val indentation = getIndentation(line)

            // Check if the indentation is valid
            if (indentation != objIndentation) throw IllegalArgumentException("Invalid indentation at: ${line.fastTrim()}")

            // Split the line by the colon
            // This returns an array with the key and the value
            val parts = getLineParts(line)

            when {
                // When the map already contains the key
                // Throw an exception due to duplicate keys
                map.containsKey(parts[0]) -> throw IllegalArgumentException("Duplicate key ${parts[0]} for ${type.simpleName}")
                // When the line has two parts (key and value)
                // Add the key and the value to the map
                parts[1].isNotEmpty() -> map[parts[0]] = parts[1]
                // When the line is a scalar
                // Add the value to the map
                isScalar(line) -> map[""] = line.split("-").last().fastTrim()
                // When the line is a list
                else -> {
                    // Get the next line
                    val nextLine = lines[i]
                    // Get the lines that are indented
                    val indentedLines = getLinesSequence(lines, i, objIndentation)
                    // Save as the value of the key,
                    // The result of the recursive call to getObjectValues
                    map[parts[0]] = if (nextLine.contains("-")) {
                        // If the next line contains a dash, it is a list
                        getListValues(indentedLines)
                    } else {
                        // If the next line does not contain a dash, it is an object
                        getObjectValues(indentedLines)
                    }
                    // Increment the index by the number of indented lines to skip over them
                    i += indentedLines.size
                }
            }
        }
        return map
    }

    private fun getLineParts(line: String): List<String> {
        val parts = line.split(":")
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

    // Check if a line is a scalar
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

    // Get the lines that are indented
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

    // Get the list of objects from the yaml text
    private fun getObjectList(lines: List<String>): List<List<String>> {

        // Get the indentation of the object
        val objIndentation = getIndentation(lines.first())
        // Create a mutable list to hold the results
        val objects = mutableListOf<List<String>>()
        // Create a mutable list to hold the current object
        var currObject = mutableListOf<String>()
        // Iterate over the lines
        lines.forEach { line ->
            // Get the indentation of the current line
            val lineIndentation = getIndentation(line)

            // Check if the indentation is valid
            // In other words, if the difference between the current indentation and the object indentation is even
            if ((lineIndentation - objIndentation) % 2 != 0)
            // If it is not valid, throw an exception
                throw IllegalArgumentException("Invalid indentation at $line")

            // Check for list item separators
            if (line[objIndentation] == '-') {
                // If it is a scalar, add the line to the list
                if (isScalar(line)) {
                    objects.add(listOf(line))
                    // If it is not a scalar
                    // Add the current object lines to the list
                } else {
                    if (currObject.isNotEmpty()) {
                        objects.add(currObject.toList())
                        currObject = mutableListOf()
                    }
                }
            } else {
                // If there is no list item separator
                // Add the line to the current object lines
                currObject.add(line)
            }
        }
        // Add the last object to the list
        if (currObject.isNotEmpty())
            objects.add(currObject.toList())
        // End by filtering any empty objects
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
