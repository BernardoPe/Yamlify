package pt.isel

import java.io.Reader
import java.time.LocalDate

class Student @JvmOverloads constructor (
    val name: String,
    val nr: Int,
    @YamlArg("city of birth")
    val from: String,
    val address: Address? = null,
    val grades: List<Grade> = emptyList(),
    @YamlConvert(YamlToDate::class)
    val birth: LocalDate? = null,
)


class YamlToDate : YamlConverter<LocalDate> {
    override fun convertToObject(input: String): LocalDate {
        return LocalDate.parse(input)
    }
}