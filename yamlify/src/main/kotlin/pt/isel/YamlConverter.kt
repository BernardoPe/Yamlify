package pt.isel

interface YamlConverter<T> {
    fun convertToObject(input: String): T
}