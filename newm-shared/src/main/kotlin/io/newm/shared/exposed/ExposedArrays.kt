package io.newm.shared.exposed

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.api.PreparedStatementApi
import org.jetbrains.exposed.sql.statements.jdbc.JdbcPreparedStatementImpl
import java.util.*
import kotlin.Array
import java.sql.Array as SQLArray

//
// Exposed support for arrays
//

/**
 * Creates array column with [name].
 *
 * @param size an optional size of the array
 */
fun Table.integerArray(name: String, size: Int? = null): Column<Array<Int>> =
    array(name, IntegerColumnType(), size)

fun Table.longArray(name: String, size: Int? = null): Column<Array<Int>> =
    array(name, LongColumnType(), size)

fun Table.floatArray(name: String, size: Int? = null): Column<Array<Float>> =
    array(name, FloatColumnType(), size)

fun Table.doubleArray(name: String, size: Int? = null): Column<Array<Double>> =
    array(name, DoubleColumnType(), size)

fun Table.uuidArray(name: String, size: Int? = null): Column<Array<UUID>> =
    array(name, UUIDColumnType(), size)

fun Table.textArray(name: String, size: Int? = null): Column<Array<String>> =
    array(name, TextColumnType(), size)

fun <T> Table.array(name: String, elementType: IColumnType, size: Int?) =
    registerColumn<Array<T>>(name, ArrayColumnType(elementType, size))

/**
 * Implementation of [ColumnType] for the SQL `ARRAY` type.
 *
 * @property elementType the type of the array element
 * @property size an optional size of the array
 */
// https://gist.github.com/DRSchlaubi/cb146ee2b4d94d1c89b17b358187c612
class ArrayColumnType(
    private val elementType: IColumnType,
    private val size: Int?
) : ColumnType() {
    override fun sqlType(): String = "${elementType.sqlType()} ARRAY${size?.let { "[$it]" } ?: ""}"

    override fun notNullValueToDB(value: Any): Any = when (value) {
        is Array<*> -> value
        is Collection<*> -> value.toTypedArray()
        else -> error("Got unexpected array value of type: ${value::class.qualifiedName} ($value)")
    }

    override fun valueFromDB(value: Any): Any = when (value) {
        is SQLArray -> value.array as Array<*>
        is Collection<*> -> value.toTypedArray()
        else -> value
    }

    override fun setParameter(stmt: PreparedStatementApi, index: Int, value: Any?) {
        if (value == null) {
            stmt.setNull(index, this)
        } else {
            val preparedStatement = stmt as? JdbcPreparedStatementImpl ?: error("Currently only JDBC is supported")
            val array = preparedStatement.statement.connection.createArrayOf(elementType.sqlType(), value as Array<*>)
            stmt[index] = array
        }
    }
}

/**
 * Invokes the `ANY` function on an expression.
 */
fun <T> ExpressionWithColumnType<Array<T>>.any(): ExpressionWithColumnType<T> =
    CustomFunction("UNNEST", columnType, this)

/**
 * Checks whether this type is in the [other] expression.
 *
 * Example:
 * ```kotlin
 * SongEntity.find { "Rock" eqAny SongsTable.genres }
 * ```
 *
 * @see any
 */
infix fun Int.eqAny(other: ExpressionWithColumnType<Array<Int>>): EqOp = intLiteral(this).eqAny(other)

infix fun Long.eqAny(other: ExpressionWithColumnType<Array<Long>>): EqOp = longLiteral(this).eqAny(other)

infix fun Float.eqAny(other: ExpressionWithColumnType<Array<Float>>): EqOp = floatLiteral(this).eqAny(other)

infix fun Double.eqAny(other: ExpressionWithColumnType<Array<Double>>): EqOp = doubleLiteral(this).eqAny(other)

infix fun String.eqAny(other: ExpressionWithColumnType<Array<String>>): EqOp = stringLiteral(this).eqAny(other)

infix fun <T> ExpressionWithColumnType<T>.eqAny(other: ExpressionWithColumnType<Array<T>>): EqOp =
    EqOp(this, other.any())

/***
 * Invokes the `UNNEST` function on an expression.
 */
fun <T> ExpressionWithColumnType<Array<T>>.unnest(): ExpressionWithColumnType<T> =
    CustomFunction("UNNEST", columnType, this)

/***
 * Invokes the `@>` (contains) operator
 * https://www.postgresql.org/docs/current/functions-array.html
 */
infix fun <T> ExpressionWithColumnType<Array<T>>.contains(array: Array<T>): Op<Boolean> = arrayOp(array, "@>")

/***
 * Invokes the `<@` (contained) operator
 * https://www.postgresql.org/docs/current/functions-array.html
 */
infix fun <T> ExpressionWithColumnType<Array<T>>.contained(array: Array<T>): Op<Boolean> = arrayOp(array, "<@")

/***
 * Invokes the `&&` (overlap) operator
 * https://www.postgresql.org/docs/current/functions-array.html
 */
infix fun <T> ExpressionWithColumnType<Array<T>>.overlaps(array: Array<T>): Op<Boolean> = arrayOp(array, "&&")

private fun <T> ExpressionWithColumnType<Array<T>>.arrayOp(array: Array<T>, opSign: String): Op<Boolean> =
    object : ComparisonOp(this@arrayOp, QueryParameter(array, columnType), opSign) {}
