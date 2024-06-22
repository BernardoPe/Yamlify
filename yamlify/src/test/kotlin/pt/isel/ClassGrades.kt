package pt.isel

import java.time.LocalDate

class ClassGrades(@YamlConvert(YamlToListGrade::class)val grades: List<Grade> = emptyList())

class YamlToListGrade : YamlConverter<List<Grade>> {
    override fun convertToObject(input: String):List<Grade> {
        return input.removeSurrounding("[", "]").removeSurrounding("{", "}").split("}, {").map{
            val parts = it.split(", ")
            val subject = parts[0].split("=")[1]
            val classification = parts[1].split("=")[1].toInt()
            Grade(subject,classification)
        }
    }
}