package io.newm.shared.exposed

import java.util.UUID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ComparisonOp
import org.jetbrains.exposed.sql.CustomFunction
import org.jetbrains.exposed.sql.DoubleColumnType
import org.jetbrains.exposed.sql.EqOp
import org.jetbrains.exposed.sql.ExpressionWithColumnType
import org.jetbrains.exposed.sql.FloatColumnType
import org.jetbrains.exposed.sql.IntegerColumnType
import org.jetbrains.exposed.sql.LongColumnType
import org.jetbrains.exposed.sql.NotOp
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.QueryParameter
import org.jetbrains.exposed.sql.SqlExpressionBuilder.asLiteral
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.TextColumnType
import org.jetbrains.exposed.sql.UUIDColumnType

//
// Exposed support for arrays
//

/**
 * Creates array column with [name].
 *
 * @param size an optional size of the array
 */
fun Table.integerArray(
    name: String,
    size: Int? = null
): Column<List<Int>> = array(name, IntegerColumnType(), size)

fun Table.longArray(
    name: String,
    size: Int? = null
): Column<List<Long>> = array(name, LongColumnType(), size)

fun Table.floatArray(
    name: String,
    size: Int? = null
): Column<List<Float>> = array(name, FloatColumnType(), size)

fun Table.doubleArray(
    name: String,
    size: Int? = null
): Column<List<Double>> = array(name, DoubleColumnType(), size)

fun Table.uuidArray(
    name: String,
    size: Int? = null
): Column<List<UUID>> = array(name, UUIDColumnType(), size)

fun Table.textArray(
    name: String,
    size: Int? = null
): Column<List<String>> = array(name, TextColumnType(), size)

/**
 * Invokes the `ANY` function on an expression.
 */
@JvmName("any")
fun <T> ExpressionWithColumnType<List<T>>.any(): ExpressionWithColumnType<List<T>> = anyFunc()

@JvmName("any2")
fun <T> ExpressionWithColumnType<List<T>?>.any(): ExpressionWithColumnType<List<T>> = anyFunc()

private fun <A> ExpressionWithColumnType<A>.anyFunc(): ExpressionWithColumnType<A & Any> = CustomFunction("ANY", this.columnType, this)

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
@JvmName("eqAny")
infix fun <T> T.eqAny(other: ExpressionWithColumnType<List<T>>): EqOp = EqOp(other.asLiteral(this), other.any())

@JvmName("eqAny2")
infix fun <T> T.eqAny(other: ExpressionWithColumnType<List<T>?>): EqOp = EqOp(other.asLiteral(this), other.any())

/***
 * Invokes the `@>` (contains) operator
 * https://www.postgresql.org/docs/current/functions-array.html
 */
@JvmName("contains")
infix fun <T> ExpressionWithColumnType<List<T>>.contains(array: List<T>): Op<Boolean> = arrayOp(array, "@>")

@JvmName("contains2")
infix fun <T> ExpressionWithColumnType<List<T>?>.contains(array: List<T>): Op<Boolean> = arrayOp(array, "@>")

/***
 * Invokes the `<@` (contained) operator
 * https://www.postgresql.org/docs/current/functions-array.html
 */
@JvmName("contained")
infix fun <T> ExpressionWithColumnType<List<T>>.contained(array: List<T>): Op<Boolean> = arrayOp(array, "<@")

@JvmName("contained2")
infix fun <T> ExpressionWithColumnType<List<T>?>.contained(array: List<T>): Op<Boolean> = arrayOp(array, "<@")

/***
 * Invokes the `&&` (overlap) operator
 * https://www.postgresql.org/docs/current/functions-array.html
 */
@JvmName("overlaps")
infix fun <T> ExpressionWithColumnType<List<T>>.overlaps(array: List<T>): Op<Boolean> = arrayOp(array, "&&")

@JvmName("overlaps2")
infix fun <T> ExpressionWithColumnType<List<T>?>.overlaps(array: List<T>): Op<Boolean> = arrayOp(array, "&&")

@JvmName("notOverlaps")
infix fun <T> ExpressionWithColumnType<List<T>>.notOverlaps(array: List<T>): Op<Boolean> = NotOp(overlaps(array))

@JvmName("notOverlaps2")
infix fun <T> ExpressionWithColumnType<List<T>?>.notOverlaps(array: List<T>): Op<Boolean> = NotOp(overlaps(array))

private fun <A> ExpressionWithColumnType<A>.arrayOp(
    array: A?,
    opSign: String
): Op<Boolean> = object : ComparisonOp(this@arrayOp, QueryParameter(array, columnType), opSign) {}
