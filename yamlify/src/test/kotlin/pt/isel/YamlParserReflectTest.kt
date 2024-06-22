package pt.isel

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import pt.isel.TestConverter.Companion.nameCounter
import java.io.File
import java.nio.file.Path
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestConverter : YamlConverter<String> {
    companion object {
        var nameCounter = 0

        fun clearCounter() {
            nameCounter = 0
        }
    }

    override fun convertToObject(input: String): String {
        nameCounter++
        return input
    }
}

class YamlParserReflectTest {

    @Test
    fun parseStudentWithMissingProperties() {
        val yaml = """
                name: Maria Candida
                from: Oleiros"""
        assertThrows<IllegalArgumentException> {
            YamlParserReflect.yamlParser(Student::class).parseObject(yaml.reader())
        }
    }

    @Test
    fun parseStudent() {
        val yaml = """
                name: Maria Candida
                nr: 873435
                from: Oleiros"""
        val st = YamlParserReflect.yamlParser(Student::class).parseObject(yaml.reader())
        assertEquals("Maria Candida", st.name)
        assertEquals(873435, st.nr)
        assertEquals("Oleiros", st.from)
    }

    @Test
    fun parseStudentWithAddress() {
        val yaml = """
                name: Maria Candida
                nr: 873435
                address:
                  street: Rua Rosa
                  nr: 78
                  city: Lisbon
                from: Oleiros"""
        val st = YamlParserReflect.yamlParser(Student::class).parseObject(yaml.reader())
        assertEquals("Maria Candida", st.name)
        assertEquals(873435, st.nr)
        assertEquals("Oleiros", st.from)
        assertEquals("Rua Rosa", st.address?.street)
        assertEquals(78, st.address?.nr)
        assertEquals("Lisbon", st.address?.city)
    }

    @Test
    fun parseSequenceOfStrings() {
        val yaml = """
            - Ola
            - Maria Carmen
            - Lisboa Capital
        """
        val seq = YamlParserReflect.yamlParser(String::class)
            .parseList(yaml.reader())
            .iterator()
        assertEquals("Ola", seq.next())
        assertEquals("Maria Carmen", seq.next())
        assertEquals("Lisboa Capital", seq.next())
        assertFalse { seq.hasNext() }
    }

    @Test
    fun parseSequenceOfInts() {
        val yaml = """
            - 1
            - 2
            - 3
        """
        val seq = YamlParserReflect.yamlParser(Int::class)
            .parseList(yaml.reader())
            .iterator()
        assertEquals(1, seq.next())
        assertEquals(2, seq.next())
        assertEquals(3, seq.next())
        assertFalse { seq.hasNext() }
    }

    @Test
    fun parseSequenceOfStudents() {
        val yaml = """
            -
              name: Maria Candida
              nr: 873435
              from: Oleiros
            - 
              name: Jose Carioca
              nr: 1214398
              from: Tamega
        """
        val seq = YamlParserReflect.yamlParser(Student::class)
            .parseList(yaml.reader())
            .iterator()
        val st1 = seq.next()
        assertEquals("Maria Candida", st1.name)
        assertEquals(873435, st1.nr)
        assertEquals("Oleiros", st1.from)
        val st2 = seq.next()
        assertEquals("Jose Carioca", st2.name)
        assertEquals(1214398, st2.nr)
        assertEquals("Tamega", st2.from)
        assertFalse { seq.hasNext() }
    }

    @Test
    fun parseSequenceOfStudentsWithAddresses() {
        val yaml = """
            -
              name: Maria Candida
              nr: 873435
              address:
                street: Rua Rosa
                nr: 78
                city: Lisbon
              from: Oleiros
            - 
              name: Jose Carioca
              nr: 1214398
              address:
                street: Rua Azul
                nr: 12
                city: Porto
              from: Tamega
        """
        val seq = YamlParserReflect.yamlParser(Student::class)
            .parseList(yaml.reader())
            .iterator()
        val st1 = seq.next()
        assertEquals("Maria Candida", st1.name)
        assertEquals(873435, st1.nr)
        assertEquals("Oleiros", st1.from)
        assertEquals("Rua Rosa", st1.address?.street)
        assertEquals(78, st1.address?.nr)
        assertEquals("Lisbon", st1.address?.city)
        val st2 = seq.next()
        assertEquals("Jose Carioca", st2.name)
        assertEquals(1214398, st2.nr)
        assertEquals("Tamega", st2.from)
        assertEquals("Rua Azul", st2.address?.street)
        assertEquals(12, st2.address?.nr)
        assertEquals("Porto", st2.address?.city)
        assertFalse { seq.hasNext() }
    }

    @Test
    fun parseSequenceOfStudentsWithAddressesAndGrades() {
        val seq = YamlParserReflect.yamlParser(Student::class)
            .parseList(yamlSequenceOfStudents.reader())
            .iterator()
        assertStudentsInSequence(seq)
    }

    @Test
    fun parseClassroom() {
        val yaml = """
          id: i45
          students: $yamlSequenceOfStudents
        """.trimIndent()
        val cr = YamlParserReflect.yamlParser(Classroom::class)
            .parseObject(yaml.reader())
        assertEquals("i45", cr.id)
        assertStudentsInSequence(cr.students.iterator())
    }

    private fun assertStudentsInSequence(seq: Iterator<Student>) {
        val st1 = seq.next()
        assertEquals("Maria Candida", st1.name)
        assertEquals(873435, st1.nr)
        assertEquals("Oleiros", st1.from)
        assertEquals("Rua Rosa", st1.address?.street)
        assertEquals(78, st1.address?.nr)
        assertEquals("Lisbon", st1.address?.city)
        val grades1 = st1.grades.iterator()
        val g1 = grades1.next()
        assertEquals("LAE", g1.subject)
        assertEquals(18, g1.classification)
        val g2 = grades1.next()
        assertEquals("PDM", g2.subject)
        assertEquals(15, g2.classification)
        val g3 = grades1.next()
        assertEquals("PC", g3.subject)
        assertEquals(19, g3.classification)
        assertFalse { grades1.hasNext() }
        val st2 = seq.next()
        assertEquals("Jose Carioca", st2.name)
        assertEquals(1214398, st2.nr)
        assertEquals("Tamega", st2.from)
        assertEquals("Rua Azul", st2.address?.street)
        assertEquals(12, st2.address?.nr)
        assertEquals("Porto", st2.address?.city)
        val grades2 = st2.grades.iterator()
        val g4 = grades2.next()
        assertEquals("TDS", g4.subject)
        assertEquals(20, g4.classification)
        val g5 = grades2.next()
        assertEquals("LAE", g5.subject)
        assertEquals(18, g5.classification)
        assertFalse { grades2.hasNext() }
        assertFalse { seq.hasNext() }
    }

    @Test
    fun parseScalarListObject() {
        val yaml = """
            list:
              - 1
              - 2
              - 3
        """.trimIndent()
        val list = YamlParserReflect.yamlParser(IntList::class)
            .parseObject(yaml.reader())
            .list
        assertEquals(1, list[0])
        assertEquals(2, list[1])
        assertEquals(3, list[2])
    }

    @Test
    fun `parse student incorrect indentation at address object`() {
        val yaml = """
                name: Maria Candida
                nr: 873435
                address:
                  street: Rua Rosa
                  nr: 78
                   city: Lisbon
                from: Oleiros"""
        assertThrows<IllegalArgumentException> {
            YamlParserReflect.yamlParser(Student::class).parseObject(yaml.reader())
        }
    }

    @Test
    fun `parse student incorrect indentation at student object`() {
        val yaml = """
                name: Maria Candida
                nr: 873435
              address:
                  street: Rua Rosa
                  nr: 78
                  city: Lisbon
                from: Oleiros""".trimIndent()
        assertThrows<IllegalArgumentException> {
            YamlParserReflect.yamlParser(Student::class).parseObject(yaml.reader())
        }
    }

    @Test
    fun `parse Int list incorrect indentation at scalar sequence`() {
        val yaml = """
             - 1
             - 2
              - 3
        """.trimIndent()
        assertThrows<IllegalArgumentException> {
            YamlParserReflect.yamlParser(Int::class).parseList(yaml.reader())
        }
    }

    @Test
    fun `parse List of List of str`() {
        val yaml = """
            - 
              - 1
              - 2
              - 3
            - 
              - 4
              - 5
              - 6
        """.trimIndent()
        val seq = YamlParserReflect.yamlParser(String::class)
            .parseList(yaml.reader()) as List<List<String>>
        assertEquals("1", seq[0][0])
        assertEquals("2", seq[0][1])
        assertEquals("3", seq[0][2])
        assertEquals("4", seq[1][0])
        assertEquals("5", seq[1][1])
        assertEquals("6", seq[1][2])
    }

    @Test
    fun `parse intListList`() {
        val yaml = """
         list:
           - 
             - 1
             - 2
             - 3
           - 
             - 4
             - 5
             - 6
            
        """.trimIndent()
        val seq = YamlParserReflect.yamlParser(IntListList::class)
            .parseObject(yaml.reader())
            .list
        assertEquals(1, seq[0][0])
        assertEquals(2, seq[0][1])
        assertEquals(3, seq[0][2])
        assertEquals(4, seq[1][0])
        assertEquals(5, seq[1][1])
        assertEquals(6, seq[1][2])
    }

    @Test
    fun `parse List of List of List of int`() {
        val yaml = """
            - 
              - 
                - 1
                - 2
                - 3
              - 
                - 4
                - 5
                - 6
            - 
              - 
                - 7
                - 8
                - 9
              - 
                - 10
                - 11
                - 12
        """.trimIndent()
        val seq = YamlParserReflect.yamlParser(Int::class)
            .parseList(yaml.reader()) as List<List<List<Int>>>
        assertEquals(1, seq[0][0][0])
        assertEquals(2, seq[0][0][1])
        assertEquals(3, seq[0][0][2])
        assertEquals(4, seq[0][1][0])
        assertEquals(5, seq[0][1][1])
        assertEquals(6, seq[0][1][2])
        assertEquals(7, seq[1][0][0])
        assertEquals(8, seq[1][0][1])
        assertEquals(9, seq[1][0][2])
        assertEquals(10, seq[1][1][0])
        assertEquals(11, seq[1][1][1])
        assertEquals(12, seq[1][1][2])
    }

    @Test
    fun `test parse student with duplicate properties`() {
        val yaml = """
                name: Maria Candida
                nr: 873435
                from: Oleiros
                from: Lisboa
            """.trimIndent()
        assertThrows<IllegalArgumentException> {
            YamlParserReflect.yamlParser(Student::class).parseObject(yaml.reader())
        }
    }

    @Test
    fun `test parse student with duplicate properties in address`() {
        val yaml = """
                name: Maria Candida
                nr: 873435
                from: Oleiros
                address:
                  street: Rua Rosa
                  nr: 78
                  city: Lisbon
                  city: Porto
            """.trimIndent()
        assertThrows<IllegalArgumentException> {
            YamlParserReflect.yamlParser(Student::class).parseObject(yaml.reader())
        }
    }


    @Test
    fun `test parse student with renamed properties duplicated`() {
        val yaml = """
                name: Maria Candida
                nr: 873435
                from: Oleiros
                city of birth: Lisboa
            """.trimIndent()
        assertThrows<IllegalArgumentException> {
            YamlParserReflect.yamlParser(Student::class).parseObject(yaml.reader())
        }
    }

    @Test
    fun `test parse student with renamed from to city of birth`() {
        val yaml = """
                name: Maria Candida
                nr: 873435
                city of birth: Lisboa
            """.trimIndent()
        val st = YamlParserReflect.yamlParser(Student::class).parseObject(yaml.reader())
        assertEquals("Maria Candida", st.name)
        assertEquals(873435, st.nr)
        assertEquals("Lisboa", st.from)
    }

    @Test
    fun `test parse classroom renamed props`() {
        val yaml = """
          classroom_id: i45
          class_students: $yamlSequenceOfStudents
        """.trimIndent()
        val cr = YamlParserReflect.yamlParser(Classroom::class)
            .parseObject(yaml.reader())
        assertEquals("i45", cr.id)
        assertStudentsInSequence(cr.students.iterator())
    }


    @Test
    fun `test parse student with custom parser`() {
        val yaml = """
                name: Maria Candida
                nr: 873435
                from: Oleiros
                birth: 1999-12-15
            """.trimIndent()
        val st = YamlParserReflect.yamlParser(Student::class).parseObject(yaml.reader())
        assertEquals("Maria Candida", st.name)
        assertEquals(873435, st.nr)
        assertEquals("Oleiros", st.from)
        assertEquals(st.birth, LocalDate.of(1999, 12, 15))
    }

    @Test
    fun `test parse student with custom parser and list of grades`() {
        val yaml = """
                name: Maria Candida
                nr: 873435
                from: Oleiros
                birth: 1999-12-15
                grades:
                  - 
                    subject: LAE
                    classification: 18
                  -
                    subject: PDM
                    classification: 15
                  -
                    subject: PC
                    classification: 19
            """.trimIndent()
        val st = YamlParserReflect.yamlParser(Student::class).parseObject(yaml.reader())
        assertEquals("Maria Candida", st.name)
        assertEquals(873435, st.nr)
        assertEquals("Oleiros", st.from)
        val grades = st.grades.iterator()
        val g1 = grades.next()
        assertEquals("LAE", g1.subject)
        assertEquals(18, g1.classification)
        val g2 = grades.next()
        assertEquals("PDM", g2.subject)
        assertEquals(15, g2.classification)
        val g3 = grades.next()
        assertEquals("PC", g3.subject)
        assertEquals(19, g3.classification)
        assertFalse { grades.hasNext() }
        assertEquals(st.birth, LocalDate.of(1999, 12, 15))
    }

    @Test
    fun `test parse with invalid date`() {
        val yaml = """
                name: Maria Candida
                nr: 873435
                from: Oleiros
                birth: 1999-12-a
            """.trimIndent()
        assertThrows<DateTimeParseException> {
            YamlParserReflect.yamlParser(Student::class).parseObject(yaml.reader())
        }
    }

    @Test
    fun `test parse grades with custom parser`() {
        val yaml = yamlSequenceOfGrades.trimIndent()
        val grades = YamlParserReflect.yamlParser(ClassGrades::class)
            .parseObject(yaml.reader())
            .grades.iterator()
        val g1 = grades.next()
        assertEquals("LAE", g1.subject)
        assertEquals(18, g1.classification)
        val g2 = grades.next()
        assertEquals("PDM", g2.subject)
        assertEquals(15, g2.classification)
        val g3 = grades.next()
        assertEquals("PC", g3.subject)
        assertEquals(19, g3.classification)
        assertFalse { grades.hasNext() }
    }


    @Test
    fun `test parseSequence with yamlConvert counter`() {

        class TestStudentClass(
            @YamlConvert(TestConverter::class)
            val name: String,
            val nr: Int,
            val from: String
        )

        val yaml = """
            - 
              name: 1
              nr: 873435
              from: Oleiros
            - 
              name: 2
              nr: 1214398
              from: Tamega
        """.trimIndent()

        val seq = YamlParserReflect.yamlParser(TestStudentClass::class)
            .parseSequence(yaml.reader())
            .iterator()

        val st1 = seq.next()
        assertEquals("1", st1.name)
        assertEquals(873435, st1.nr)
        assertEquals("Oleiros", st1.from)
        val st2 = seq.next()
        assertEquals("2", st2.name)
        assertEquals(1214398, st2.nr)
        assertEquals("Tamega", st2.from)
        assertFalse { seq.hasNext() }
        assertEquals(2, nameCounter)
    }

    @Test
    fun `parseSequence should parse valid sequence of integers`() {
        val yaml = """
            - 1
            - 2
            - 3
        """.trimIndent()
        val parser = YamlParserReflect.yamlParser(Int::class)
        val sequence = parser.parseSequence(yaml.reader())
        val iterator = sequence.iterator()
        assertEquals(1, iterator.next())
        assertEquals(2, iterator.next())
        assertEquals(3, iterator.next())
        assert(!iterator.hasNext())
    }

    @Test
    fun `parseSequence should throw exception for invalid sequence`() {
        val yaml = """
            - 1
            - a
            - 3
        """.trimIndent()
        val parser = YamlParserReflect.yamlParser(Int::class)
        assertThrows<NumberFormatException> {
            parser.parseSequence(yaml.reader()).toList()
        }
    }

    @Test
    fun `parse sequence with list of list`() {
        val yaml = """
            - 
              - 1
              - 2
              - 3
            - 
              - 4
              - 5
              - 6
        """.trimIndent()
        val parser = YamlParserReflect.yamlParser(Int::class)
        val sequence = parser.parseSequence(yaml.reader())
        val iterator = sequence.iterator()
        val list1 = iterator.next() as List<Int>
        assertEquals(1, list1[0])
        assertEquals(2, list1[1])
        assertEquals(3, list1[2])
        val list2 = iterator.next() as List<Int>
        assertEquals(4, list2[0])
        assertEquals(5, list2[1])
        assertEquals(6, list2[2])
        assert(!iterator.hasNext())
    }

    @Test
    fun `parse sequence list of list of students`() {
        val yaml = """
            - 
              - 
                name: Maria Candida
                nr: 873435
                from: Oleiros
              - 
                name: Jose Carioca
                nr: 1214398
                from: Tamega
            - 
              - 
                name: Maria Candida
                nr: 873435
                from: Oleiros
              - 
                name: Jose Carioca
                nr: 1214398
                from: Tamega
        """.trimIndent()
        val parser = YamlParserReflect.yamlParser(Student::class)
        val sequence = parser.parseSequence(yaml.reader())
        val iterator = sequence.iterator()
        val list1 = iterator.next() as List<Student>
        val st1 = list1[0]
        assertEquals("Maria Candida", st1.name)
        assertEquals(873435, st1.nr)
        assertEquals("Oleiros", st1.from)
        val st2 = list1[1]
        assertEquals("Jose Carioca", st2.name)
        assertEquals(1214398, st2.nr)
        assertEquals("Tamega", st2.from)
        val list2 = iterator.next() as List<Student>
        val st3 = list2[0]
        assertEquals("Maria Candida", st3.name)
        assertEquals(873435, st3.nr)
        assertEquals("Oleiros", st3.from)
        val st4 = list2[1]
        assertEquals("Jose Carioca", st4.name)
        assertEquals(1214398, st4.nr)
        assertEquals("Tamega", st4.from)
        assert(!iterator.hasNext())
    }

    @Test
    fun `parseSequence should parse valid sequence of strings`() {
        val yaml = """
            - hello
            - world
            
        """.trimIndent()
        val parser = YamlParserReflect.yamlParser(String::class)
        val sequence = parser.parseSequence(yaml.reader())
        val iterator = sequence.iterator()
        assertEquals("hello", iterator.next())
        assertEquals("world", iterator.next())
        assert(!iterator.hasNext())
    }

    @Test
    fun `parseSequence should handle empty sequence`() {
        val yaml = ""
        val parser = YamlParserReflect.yamlParser(String::class)
        assertThrows<NoSuchElementException> {
            parser.parseSequence(yaml.reader()).iterator().next()
        }
    }

    @Test
    fun `parseSequence of students`() {
        val yaml = yamlSequenceOfStudents
        val seq = YamlParserReflect.yamlParser(Student::class)
            .parseSequence(yaml.reader())
            .iterator()
        assertStudentsInSequence(seq)
    }

    @Test
    fun `parseSequence of grades`() {
        val yaml = """
            -
              subject: LAE
              classification: 18
            -
              subject: PDM
              classification: 15
            -
              subject: PC
              classification: 19
        """.trimIndent()
        val seq = YamlParserReflect.yamlParser(Grade::class)
            .parseSequence(yaml.reader())
            .iterator()
        val g1 = seq.next()
        assertEquals("LAE", g1.subject)
        assertEquals(18, g1.classification)
        val g2 = seq.next()
        assertEquals("PDM", g2.subject)
        assertEquals(15, g2.classification)
        val g3 = seq.next()
        assertEquals("PC", g3.subject)
        assertEquals(19, g3.classification)
        assertFalse { seq.hasNext() }
    }

    @Test
    fun `parseSequence should handle sequence with empty elements`() {
        val yaml = """
            - 
            - 
        """.trimIndent()
        val parser = YamlParserReflect.yamlParser(String::class)
        val sequence = parser.parseSequence(yaml.reader())
        val iterator = sequence.iterator()
        assertThrows<NoSuchElementException> {
            iterator.next()
        }
    }

    @Test
    fun `parseSequence with student class should parse valid sequence of students`() {
        val yaml = """
            -
              name: Maria Candida
              nr: 873435
              from: Oleiros
            - 
              name: Jose Carioca
              nr: 1214398
              from: Tamega
        """.trimIndent()
        val parser = YamlParserReflect.yamlParser(Student::class)
        val sequence = parser.parseSequence(yaml.reader())
        val iterator = sequence.iterator()
        val st1 = iterator.next()
        assertEquals("Maria Candida", st1.name)
        assertEquals(873435, st1.nr)
        assertEquals("Oleiros", st1.from)
        val st2 = iterator.next()
        assertEquals("Jose Carioca", st2.name)
        assertEquals(1214398, st2.nr)
        assertEquals("Tamega", st2.from)
        assert(!iterator.hasNext())
    }

    @Test
    fun `test parseFolderEager with file change`(@TempDir tempDir: Path) {
        val firstYamlFile = File(tempDir.toFile(), "test.yaml")
        firstYamlFile.writeText(yamlSequenceOfGrades.trimIndent())
        val secondYamlFile = File(tempDir.toFile(), "test2.yaml")
        secondYamlFile.writeText(yamlSequenceOfGradesChanged.trimIndent())
        val thirdYamlFile = File(tempDir.toFile(), "test3.yaml")
        thirdYamlFile.writeText(yamlSequenceOfGrades.trimIndent())

        // Create the parser
        val grades = YamlParserReflect.yamlParser(ClassGrades::class)
        val result = grades.parseFolderEager(tempDir.toString())

        secondYamlFile.writeText(yamlSequenceOfGrades.trimIndent())
        thirdYamlFile.writeText(yamlSequenceOfGradesChanged.trimIndent())

        assertEquals(3, result.size)
        val parsedFile = result[0]
        assertEquals("LAE", parsedFile.grades.first().subject)
        assertEquals(18, parsedFile.grades.first().classification)
        assertEquals("PDM", parsedFile.grades.elementAt(1).subject)
        assertEquals(15, parsedFile.grades.elementAt(1).classification)
        assertEquals("PC", parsedFile.grades.elementAt(2).subject)
        assertEquals(19, parsedFile.grades.elementAt(2).classification)
        val parsedFile2 = result[1]
        assertEquals("PSC", parsedFile2.grades.first().subject)
        assertEquals(10, parsedFile2.grades.first().classification)
        assertEquals("PG", parsedFile2.grades.elementAt(1).subject)
        assertEquals(13, parsedFile2.grades.elementAt(1).classification)
        assertEquals("LIC", parsedFile2.grades.elementAt(2).subject)
        assertEquals(19, parsedFile2.grades.elementAt(2).classification)
        val parsedFile3 = result[2]
        assertEquals("LAE", parsedFile3.grades.first().subject)
        assertEquals(18, parsedFile3.grades.first().classification)
        assertEquals("PDM", parsedFile3.grades.elementAt(1).subject)
        assertEquals(15, parsedFile3.grades.elementAt(1).classification)
        assertEquals("PC", parsedFile3.grades.elementAt(2).subject)
        assertEquals(19, parsedFile3.grades.elementAt(2).classification)

    }

    @Test
    fun `test parseFolderLazy with file change`(@TempDir tempDir: Path) {
        val firstYamlFile = File(tempDir.toFile(), "test.yaml")
        firstYamlFile.writeText(yamlSequenceOfGrades.trimIndent())
        val secondYamlFile = File(tempDir.toFile(), "test2.yaml")
        secondYamlFile.writeText(yamlSequenceOfGradesChanged.trimIndent())
        val thirdYamlFile = File(tempDir.toFile(), "test3.yaml")
        thirdYamlFile.writeText(yamlSequenceOfGrades.trimIndent())

        // Create the parser
        val grades = YamlParserReflect.yamlParser(ClassGrades::class)
        val result = grades.parseFolderLazy(tempDir.toString()).iterator()

        secondYamlFile.writeText(yamlSequenceOfGrades.trimIndent())
        thirdYamlFile.writeText(yamlSequenceOfGradesChanged.trimIndent())


        assertTrue(result.hasNext())
        val parsedFile = result.next()
        assertEquals("LAE", parsedFile.grades.first().subject)
        assertEquals(18, parsedFile.grades.first().classification)
        assertEquals("PDM", parsedFile.grades.elementAt(1).subject)
        assertEquals(15, parsedFile.grades.elementAt(1).classification)
        assertEquals("PC", parsedFile.grades.elementAt(2).subject)
        assertEquals(19, parsedFile.grades.elementAt(2).classification)
        assertTrue(result.hasNext())
        val parsedFile2 = result.next()
        assertEquals("LAE", parsedFile2.grades.first().subject)
        assertEquals(18, parsedFile2.grades.first().classification)
        assertEquals("PDM", parsedFile2.grades.elementAt(1).subject)
        assertEquals(15, parsedFile2.grades.elementAt(1).classification)
        assertEquals("PC", parsedFile2.grades.elementAt(2).subject)
        assertEquals(19, parsedFile2.grades.elementAt(2).classification)
        val parsedFile3 = result.next()
        assertEquals("PSC", parsedFile3.grades.first().subject)
        assertEquals(10, parsedFile3.grades.first().classification)
        assertEquals("PG", parsedFile3.grades.elementAt(1).subject)
        assertEquals(13, parsedFile3.grades.elementAt(1).classification)
        assertEquals("LIC", parsedFile3.grades.elementAt(2).subject)
        assertEquals(19, parsedFile3.grades.elementAt(2).classification)
    }

    @BeforeEach
    fun clearCounter() {
        TestConverter.clearCounter()
    }

}

const val yamlSequenceOfStudents = """
            -
              name: Maria Candida
              nr: 873435
              address:
                street: Rua Rosa
                nr: 78
                city: Lisbon
              from: Oleiros
              grades:
                - 
                  subject: LAE
                  classification: 18
                -
                  subject: PDM
                  classification: 15
                -
                  subject: PC
                  classification: 19
            - 
              name: Jose Carioca
              nr: 1214398
              address:
                street: Rua Azul
                nr: 12
                city: Porto
              from: Tamega
              grades:
                -
                  subject: TDS
                  classification: 20
                - 
                  subject: LAE
                  classification: 18
        """

const val yamlSequenceOfGrades = """
               grades:
                 - 
                   subject: LAE
                   classification: 18
                 -
                   subject: PDM
                   classification: 15
                 -
                   subject: PC
                   classification: 19
        """

const val yamlSequenceOfGradesChanged = """
               grades:
                 - 
                   subject: PSC
                   classification: 10
                 -
                   subject: PG
                   classification: 13
                 -
                   subject: LIC
                   classification: 19
        """
