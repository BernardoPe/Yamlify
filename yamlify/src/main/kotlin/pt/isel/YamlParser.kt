package pt.isel

import java.io.Reader

interface YamlParser<T> {
    fun parseObject(yaml: Reader): T
    fun parseList(yaml: Reader): List<T>
    fun parseSequence(yaml: Reader): Sequence<T>
    fun parseFolderEager(folder: String): List<T>
    fun parseFolderLazy(folder: String): Sequence<T>
}