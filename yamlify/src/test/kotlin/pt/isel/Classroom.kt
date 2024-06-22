package pt.isel

class Classroom(
    @YamlArg("classroom_id")
    val id: String,
    @YamlArg("class_students")
    val students: List<Student>
)