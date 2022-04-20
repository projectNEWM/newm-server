package io.projectnewm.server.database

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.CustomStringFunction
import org.jetbrains.exposed.sql.EqOp
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.ExpressionWithColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.doubleLiteral
import org.jetbrains.exposed.sql.floatLiteral
import org.jetbrains.exposed.sql.intLiteral
import org.jetbrains.exposed.sql.longLiteral
import org.jetbrains.exposed.sql.statements.api.PreparedStatementApi
import org.jetbrains.exposed.sql.statements.jdbc.JdbcPreparedStatementImpl
import org.jetbrains.exposed.sql.stringLiteral
import org.jetbrains.exposed.sql.vendors.currentDialect
import java.io.Serializable
import java.util.UUID
import kotlin.Array
import java.sql.Array as SQLArray

// Based on https://gist.github.com/DRSchlaubi/cb146ee2b4d94d1c89b17b358187c612

/**
 * Creates array column with [name].
 *
 * @param size an optional size of the array
 */
fun Table.integerArray(name: String, size: Int? = null): Column<Array<Int>> =
    array(name, currentDialect.dataTypeProvider.integerType(), size)

fun Table.longArray(name: String, size: Int? = null): Column<Array<Int>> =
    array(name, currentDialect.dataTypeProvider.longType(), size)

fun Table.floatArray(name: String, size: Int? = null): Column<Array<Float>> =
    array(name, currentDialect.dataTypeProvider.floatType(), size)

fun Table.doubleArray(name: String, size: Int? = null): Column<Array<Double>> =
    array(name, currentDialect.dataTypeProvider.doubleType(), size)

fun Table.uuidArray(name: String, size: Int? = null): Column<Array<UUID>> =
    array(name, currentDialect.dataTypeProvider.uuidType(), size)

fun Table.textArray(name: String, size: Int? = null): Column<Array<String>> =
    array(name, currentDialect.dataTypeProvider.textType(), size)

private fun <T : Serializable> Table.array(name: String, underlyingType: String, size: Int?) =
    registerColumn<Array<T>>(name, ArrayColumnType<T>(underlyingType, size))

/**
 * Invokes the `ANY` function on [expression].
 */
fun <T : Serializable> any(
    expression: Expression<Array<T>>,
): ExpressionWithColumnType<String?> = CustomStringFunction("ANY", expression)

/**
 * Checks whether this string is in the [other] expression.
 *
 * Example:
 * ```kotlin
 * productService.find { "tag" eqAny ProductsTable.tags }
 * ```
 *
 * @see any
 */
infix fun Int.eqAny(other: Expression<Array<Int>>): EqOp = intLiteral(this).eqAny(other)

infix fun Long.eqAny(other: Expression<Array<Long>>): EqOp = longLiteral(this).eqAny(other)

infix fun Float.eqAny(other: Expression<Array<Float>>): EqOp = floatLiteral(this).eqAny(other)

infix fun Double.eqAny(other: Expression<Array<Double>>): EqOp = doubleLiteral(this).eqAny(other)

infix fun String.eqAny(other: Expression<Array<String>>): EqOp = stringLiteral(this).eqAny(other)

private fun <T : Serializable> Expression<T>.eqAny(other: Expression<Array<T>>): EqOp = EqOp(this, any(other))

/**
 * Implementation of [ColumnType] for the SQL `ARRAY` type.
 *
 * @property underlyingType the type of the array
 * @property size an optional size of the array
 */
class ArrayColumnType<T : Serializable>(
    private val underlyingType: String,
    private val size: Int?
) : ColumnType() {
    override fun sqlType(): String = "$underlyingType ARRAY${size?.let { "[$it]" } ?: ""}"

    override fun notNullValueToDB(value: Any): Any = when (value) {
        is Array<*> -> value
        is Collection<*> -> value.toTypedArray()
        else -> error("Got unexpected array value of type: ${value::class.qualifiedName} ($value)")
    }

    override fun valueFromDB(value: Any): Any = when (value) {
        is SQLArray -> value.array as Array<*>
        is Array<*> -> value
        is Collection<*> -> value.toTypedArray()
        else -> error("Got unexpected array value of type: ${value::class.qualifiedName} ($value)")
    }

    override fun setParameter(stmt: PreparedStatementApi, index: Int, value: Any?) {
        if (value == null) {
            stmt.setNull(index, this)
        } else {
            val preparedStatement = stmt as? JdbcPreparedStatementImpl ?: error("Currently only JDBC is supported")
            val array = preparedStatement.statement.connection.createArrayOf(underlyingType, value as Array<*>)
            stmt[index] = array
        }
    }
}
