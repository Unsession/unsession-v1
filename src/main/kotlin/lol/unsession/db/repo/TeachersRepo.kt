package lol.unsession.db.repo

import lol.unsession.db.UnsessionSchema.*
import lol.unsession.db.UnsessionSchema.Teacher.create
import lol.unsession.db.models.TeacherDto
import lol.unsession.db.models.client.Review
import lol.unsession.findColumn
import lol.unsession.plugins.DataSelectParameters
import lol.unsession.plugins.PagingFilterParameters
import lol.unsession.plugins.Sorter
import lol.unsession.plugins.configureDatabasesLocalhost
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.wrap
import org.jetbrains.exposed.sql.transactions.transaction

interface TeachersRepo {
    suspend fun getTeacher(id: Int): TeacherDto? // single object
    suspend fun getTeachers(paging: PagingFilterParameters): List<TeacherDto>
    suspend fun addTeacher(teacher: TeacherDto): TeacherDto?
    suspend fun searchTeachers(prompt: String, paging: PagingFilterParameters): List<TeacherDto>
}

interface ReviewsRepo {
    suspend fun getReview(id: Int): Review? // single object
    suspend fun getReviews(paging: PagingFilterParameters): List<Review>
    suspend fun getReviewsByTeacher(teacherId: Int, paging: PagingFilterParameters): List<Review>
    suspend fun getReviewsByUser(userId: Int, paging: PagingFilterParameters): List<Review>
    suspend fun addReview(review: Review): Review?
}

sealed class Repository {

    object Teachers : TeachersRepo {
        override suspend fun getTeacher(id: Int): TeacherDto? {
            return Teacher.select { Teacher.id eq id }.map { Teacher.fromRow(it) }.firstOrNull()
        }

        override suspend fun getTeachers(paging: PagingFilterParameters): List<TeacherDto> {
            TODO("Not yet")//return selectData(Teacher, paging).map { Teacher.fromRow(it) }
        }

        override suspend fun addTeacher(teacher: TeacherDto): TeacherDto? {
            return create(teacher)
        }

        override suspend fun searchTeachers(prompt: String, paging: PagingFilterParameters): List<TeacherDto> {
            TODO("Not yet implemented")
        }
    }

    object Reviews : ReviewsRepo {
        override suspend fun getReview(id: Int): Review? {
            val review = TeacherReview.select { TeacherReview.id eq id }.map { TeacherReview.fromRow(it) }.firstOrNull()
                ?: return null
            val user = UsersRepositoryImpl.getUser(review.userId)
                ?: return null
            val teacher = Teacher.select { Teacher.id eq review.teacherId }.map { Teacher.fromRow(it) }.firstOrNull()
                ?: return null
            return Review.fromReviewAndUser(review, user, teacher)
        }

        override suspend fun getReviews(paging: PagingFilterParameters): List<Review> {
            TODO("Not yet implemented")
        }

        override suspend fun getReviewsByTeacher(teacherId: Int, paging: PagingFilterParameters): List<Review> {
            TODO("Not yet implemented")
        }

        override suspend fun getReviewsByUser(userId: Int, paging: PagingFilterParameters): List<Review> {
            TODO("Not yet implemented")
        }

        override suspend fun addReview(review: Review): Review? {
            return TeacherReview.create(review.toReviewDto())?.toReview()
        }
    }

}

fun main() {
    configureDatabasesLocalhost()
    println(
        selectData(
            Teacher, PagingFilterParameters(
                page = 0,
                pageSize = 2,
                dataSelectParameters = DataSelectParameters(
                    filters = hashMapOf(
                        "full_name" to "Елизавета",
                        "department" to "КТиУ",
                    ),
                    sort = Sorter(
                        field = "department",
                        a = true,
                    )

                )
            )
        )
    )
}

fun selectData(table: Table, pagingParameters: PagingFilterParameters): List<ResultRow> {
    return transaction {
        // if params.filtering null => select all
        // else select with filtering
        // if params.sorting null => do not sort
        // else sorting
        // then limit by paging parameters
        val params = pagingParameters.dataSelectParameters
        fun filter(q: Query, c: String, v: Any) {
            table.findColumn(c)?.let { column ->
                q.adjustWhere {
                    column.eq(column.wrap(v))
                }
            }
        }
        fun andFilter(q: Query, c: String, v: Any) {
            table.findColumn(c)?.let { column ->
                q.andWhere {
                    column.eq(column.wrap(v))
                }
            }
        }
        val query = table.selectAll()
        params?.filters?.let {
            params.filters.entries.toList()[0].let { (k, v) ->
                filter(query, k, v)
            }
            if (params.filters.size > 1) {
                for (i in 1 until params.filters.size) {
                    params.filters.entries.toList()[i].let { (k, v) ->
                        andFilter(query, k, v)
                    }
                }
            }
        }
        query.limit(pagingParameters.pageSize, (pagingParameters.page * pagingParameters.pageSize).toLong())
        params?.sort?.let { sorter ->
            if (sorter.a) {
                query.sortedBy {
                    table.findColumn(sorter.field)
                }
            } else {
                query.sortedByDescending {
                    table.findColumn(sorter.field)
                }
            }
        }
        return@transaction query.toList()
    }
}