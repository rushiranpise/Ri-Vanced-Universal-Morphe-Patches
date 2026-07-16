@file:Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")

package app.morphe.patcher

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Field
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.MethodParameter
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.Reference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

private val currentClassDef = ThreadLocal<ClassDef?>()
private val currentMutableClassDef = ThreadLocal<MutableClass?>()
private val methodClassMap = java.util.concurrent.ConcurrentHashMap<String, MutableClass>()

typealias IndexedMatcherPredicate<T> = T.(ClassDef, Method, Int) -> Boolean
typealias BytecodePatchContextDeclarativePredicateCompositeBuilder = MutablePredicateList<Method>.() -> Unit

class IndexedMatcher(private val items: InstructionFilter) {
    operator fun invoke(instructions: Iterable<Instruction>): Boolean {
        val method = currentClassDef.get()?.methods?.firstOrNull() ?: return false
        return instructions.any { instruction -> items.matches(method, instruction) }
    }
}

fun indexedMatcher(items: InstructionFilter): IndexedMatcher = IndexedMatcher(items)

fun indexedMatcher(predicate: IndexedMatcherPredicate<Instruction>): InstructionFilter = object : InstructionFilter {
    override fun matches(method: Method, instruction: Instruction): Boolean =
        instruction.predicate(currentClassDef.get() ?: method.immutableClassDef, method, 0)
}

data class CompositeMatch(
    val classDef: MutableClass,
    val method: MutableMethod,
    val indices: List<List<Int>> = emptyList(),
) {
    val immutableClassDef: ClassDef
        get() = classDef

    val immutableMethod: Method
        get() = method

    operator fun get(index: Int): Int =
        indices.flatten().let { if (index < 0) it[it.size + index] else it[index] }
}

object ClassDefComposing {
    fun composingFirstMethod(
        vararg strings: String,
        build: MutablePredicateList<Method>.() -> Unit = {},
    ): ReadOnlyProperty<ClassDef, CompositeMatch> =
        ReadOnlyProperty { thisRef, _ -> thisRef.firstMethodComposite(*strings, build = build) }
}

class MutablePredicateList<T> {
    internal val predicates = mutableListOf<T.() -> Boolean>()

    fun add(predicate: T.() -> Boolean) {
        predicates += predicate
    }

    fun matches(value: T): Boolean = predicates.all { value.it() }
}

@JvmName("methodCustom")
fun MutablePredicateList<Method>.custom(predicate: Method.() -> Boolean) = add(predicate)

@JvmName("methodName")
fun MutablePredicateList<Method>.name(name: String) = custom { this.name == name }

fun MutablePredicateList<Method>.name(predicate: String.() -> Boolean) = custom { name.predicate() }

fun MutablePredicateList<Method>.returnType(type: String) = custom { returnType == type || returnType.startsWith(type) }

fun MutablePredicateList<Method>.returnType(predicate: String.() -> Boolean) = custom { returnType.predicate() }

@JvmName("methodParameterTypes")
fun MutablePredicateList<Method>.parameterTypes(vararg types: String) = custom {
    parameters.matchesDescriptors(types.asList())
}

@JvmName("methodParameterTypePrefixes")
fun MutablePredicateList<Method>.parameterTypes(parameterTypePrefixes: Array<String>) = custom {
    val actual = parameters.map { it.type }
    actual.size == parameterTypePrefixes.size &&
        actual.zip(parameterTypePrefixes).all { (parameter, prefix) -> parameter.startsWith(prefix) }
}

fun MutablePredicateList<Method>.accessFlags(vararg flags: AccessFlags) = custom {
    flags.all { accessFlags and it.value != 0 }
}

fun MutablePredicateList<Method>.definingClass(type: String) = custom {
    definingClass.matchesDescriptor(type)
}

fun MutablePredicateList<Method>.definingClass(predicate: String.() -> Boolean) = custom {
    definingClass.predicate()
}

fun MutablePredicateList<Method>.strings(vararg strings: String) = custom {
    implementation?.instructions?.hasStrings(strings.asList()) == true
}

fun MutablePredicateList<Method>.instructions(vararg filters: Any) = custom {
    val instructions = implementation?.instructions?.toList() ?: return@custom false
    val classDef = currentClassDef.get() ?: immutableClassDef
    filters.all { filter ->
        instructions.withIndex().any { (index, instruction) ->
            when (filter) {
                is InstructionFilter -> filter.matches(this, instruction)
                is Function4<*, *, *, *, *> ->
                    (filter as IndexedMatcherPredicate<Instruction>).invoke(instruction, classDef, this, index)
                else -> false
            }
        }
    }
}

fun MutablePredicateList<Method>.instructions(predicates: InstructionFilter) {
    instructions(*arrayOf<Any>(predicates))
}

fun MutablePredicateList<Method>.opcodes(vararg opcodes: Opcode) = custom {
    val instructions = implementation?.instructions?.toList() ?: return@custom false
    opcodes.all { opcode -> instructions.any { it.opcode == opcode } }
}

@JvmName("classDefType")
fun MutablePredicateList<ClassDef>.type(type: String) = add { this.type.matchesDescriptor(type) }

@JvmName("classDefCustom")
fun MutablePredicateList<ClassDef>.custom(predicate: ClassDef.() -> Boolean) = add(predicate)

fun MutablePredicateList<ClassDef>.methods(build: MutablePredicateList<Method>.() -> Unit) = add {
    methods.any { it.matches(emptyList(), build) }
}

fun MutablePredicateList<ClassDef>.fields(build: MutablePredicateList<Field>.() -> Unit) = add {
    val predicates = MutablePredicateList<Field>().apply(build)
    fields.any { predicates.matches(it) }
}

@JvmName("fieldCustom")
fun MutablePredicateList<Field>.custom(predicate: Field.() -> Boolean) = add(predicate)

@JvmName("fieldName")
fun MutablePredicateList<Field>.name(name: String) = custom { this.name == name }

@JvmName("fieldType")
fun MutablePredicateList<Field>.type(type: String) = custom { this.type.matchesDescriptor(type) }

fun BytecodePatchContext.firstClassDef(type: String): MutableClass = mutableClassDefBy {
    it.type.matchesDescriptor(type)
}

fun BytecodePatchContext.firstClassDef(predicate: ClassDef.() -> Boolean): MutableClass =
    mutableClassDefBy { it.predicate() }

fun BytecodePatchContext.firstClassDefOrNull(type: String): MutableClass? = mutableClassDefByOrNull {
    it.type.matchesDescriptor(type)
}

fun gettingFirstClassDef(type: String): ReadOnlyProperty<BytecodePatchContext, MutableClass> =
    ReadOnlyProperty { thisRef, _ -> thisRef.firstClassDef(type) }

fun gettingFirstClassDefDeclaratively(
    build: MutablePredicateList<ClassDef>.() -> Unit,
): ReadOnlyProperty<BytecodePatchContext, MutableClass> =
    ReadOnlyProperty { thisRef, _ ->
        var result: MutableClass? = null
        thisRef.classDefForEach { classDef ->
            if (result == null && MutablePredicateList<ClassDef>().apply(build).matches(classDef)) {
                result = thisRef.mutableClassDefBy(classDef.type)
            }
        }
        result ?: throw PatchException("No matching class found")
    }

fun BytecodePatchContext.firstMethod(method: Method): MutableMethod = mutableClassDefBy(method.definingClass)
    .methods
    .first { it.name == method.name && it.returnType == method.returnType && it.parameters.matchesDescriptors(method.parameters.map { parameter -> parameter.type }) }
    .also { registerMethodClass(it, mutableClassDefBy(method.definingClass)) }

fun BytecodePatchContext.firstMethod(reference: MethodReference): MutableMethod = mutableClassDefBy(reference.definingClass)
    .methods
    .first { it.name == reference.name && it.returnType == reference.returnType && it.parameters.matchesDescriptors(reference.parameterTypes.map(CharSequence::toString)) }
    .also { registerMethodClass(it, mutableClassDefBy(reference.definingClass)) }

fun ClassDef.firstMethodDeclaratively(
    vararg strings: String,
    build: MutablePredicateList<Method>.() -> Unit = {},
): MutableMethod {
    val method = methods.first { it.matches(strings.asList(), build) }
    val mutableMethod = method as? MutableMethod ?: method.toMutable()
    (this as? MutableClass)?.let { registerMethodClass(mutableMethod, it) }
    return mutableMethod
}

fun ClassDef.firstMethod(predicate: Method.() -> Boolean): MutableMethod =
    methods.first(predicate).let { method ->
        val mutableMethod = method as? MutableMethod ?: method.toMutable()
        (this as? MutableClass)?.let { registerMethodClass(mutableMethod, it) }
        mutableMethod
    }

fun ClassDef.firstMethod(method: Method): MutableMethod =
    methods.first {
        it.name == method.name &&
            it.returnType == method.returnType &&
            it.parameters.matchesDescriptors(method.parameters.map(MethodParameter::getType))
    }.let {
        val mutableMethod = it as? MutableMethod ?: it.toMutable()
        (this as? MutableClass)?.let { mutableClass -> registerMethodClass(mutableMethod, mutableClass) }
        mutableMethod
    }

fun ClassDef.firstMethod(
    vararg strings: String,
    build: MutablePredicateList<Method>.() -> Unit = {},
): MutableMethod = firstMethodDeclaratively(*strings, build = build)

fun Iterable<MutableMethod>.firstMethod(predicate: MutableMethod.() -> Boolean): MutableMethod =
    first(predicate)

fun ClassDef.firstMethodComposite(
    vararg strings: String,
    build: MutablePredicateList<Method>.() -> Unit = {},
): CompositeMatch {
    val method = firstMethodDeclaratively(*strings, build = build)
    return CompositeMatch(this as? MutableClass ?: throw PatchException("Class is not mutable: $type"), method)
}

fun BytecodePatchContext.firstMethodDeclaratively(
    vararg strings: String,
    build: MutablePredicateList<Method>.() -> Unit = {},
): MutableMethod = findMethodOrNull(strings.asList(), build) ?: throw PatchException("No matching method found")

fun BytecodePatchContext.firstMethodDeclaratively(
    vararg strings: String,
    parameterTypePrefixes: List<String>,
    predicateBuilder: MutablePredicateList<Method>.() -> Unit = {},
): MutableMethod = firstMethodDeclaratively(*strings) {
    custom {
        parameters.map { it.type }.zip(parameterTypePrefixes).all { (actual, prefix) -> actual.startsWith(prefix) }
    }
    predicateBuilder()
}

fun gettingFirstMethodDeclaratively(
    vararg strings: String,
    build: MutablePredicateList<Method>.() -> Unit = {},
): ReadOnlyProperty<BytecodePatchContext, MutableMethod> =
    ReadOnlyProperty { thisRef, _ -> thisRef.firstMethodDeclaratively(*strings, build = build) }

fun gettingFirstMethodDeclarativelyOrNull(
    vararg strings: String,
    build: MutablePredicateList<Method>.() -> Unit = {},
): ReadOnlyProperty<BytecodePatchContext, MutableMethod?> =
    ReadOnlyProperty { thisRef, _ -> thisRef.findMethodOrNull(strings.asList(), build) }

fun gettingFirstImmutableMethodDeclaratively(
    vararg strings: String,
    build: MutablePredicateList<Method>.() -> Unit = {},
): ReadOnlyProperty<BytecodePatchContext, Method> =
    ReadOnlyProperty { thisRef, _ -> thisRef.findImmutableMethodOrNull(strings.asList(), build) ?: throw PatchException("No matching method found") }

fun gettingFirstImmutableMethodDeclarativelyOrNull(
    vararg strings: String,
    build: MutablePredicateList<Method>.() -> Unit = {},
): ReadOnlyProperty<BytecodePatchContext, Method?> =
    ReadOnlyProperty { thisRef, _ -> thisRef.findImmutableMethodOrNull(strings.asList(), build) }

fun firstImmutableMethodDeclaratively(
    vararg strings: String,
    build: MutablePredicateList<Method>.() -> Unit = {},
): ReadOnlyProperty<ClassDef, Method> =
    ReadOnlyProperty { thisRef, _ ->
        currentClassDef.set(thisRef)
        try {
            thisRef.methods.firstOrNull { it.matches(strings.asList(), build) }
                ?: throw PatchException("No matching method found")
        } finally {
            currentClassDef.remove()
        }
    }

fun ClassDef.firstImmutableMethodDeclaratively(
    vararg strings: String,
    build: MutablePredicateList<Method>.() -> Unit = {},
): Method {
    currentClassDef.set(this)
    return try {
        methods.firstOrNull { it.matches(strings.asList(), build) }
            ?: throw PatchException("No matching method found")
    } finally {
        currentClassDef.remove()
    }
}

fun ClassDef.firstImmutableMethod(
    vararg strings: String,
    build: MutablePredicateList<Method>.() -> Unit = {},
): Method = firstImmutableMethodDeclaratively(*strings, build = build)

fun BytecodePatchContext.firstImmutableMethod(
    vararg strings: String,
    build: MutablePredicateList<Method>.() -> Unit = {},
): Method = findImmutableMethodOrNull(strings.asList(), build) ?: throw PatchException("No matching method found")

fun BytecodePatchContext.firstImmutableMethodDeclaratively(
    vararg strings: String,
    build: MutablePredicateList<Method>.() -> Unit = {},
): Method = firstImmutableMethod(*strings, build = build)

fun firstImmutableMethod(
    vararg strings: String,
    build: MutablePredicateList<Method>.() -> Unit = {},
): ReadOnlyProperty<ClassDef, Method> = firstImmutableMethodDeclaratively(*strings, build = build)

fun gettingFirstMethod(
    fingerprint: Fingerprint,
): ReadOnlyProperty<BytecodePatchContext, MutableMethod> =
    ReadOnlyProperty { _, _ -> throw fingerprint.patchException() }

fun gettingFirstMethod(
    vararg strings: String,
    build: MutablePredicateList<Method>.() -> Unit = {},
): ReadOnlyProperty<BytecodePatchContext, MutableMethod> =
    gettingFirstMethodDeclaratively(*strings, build = build)

fun gettingFirstMethod(
    predicate: Method.() -> Boolean,
): ReadOnlyProperty<BytecodePatchContext, MutableMethod> =
    gettingFirstMethodDeclaratively { custom(predicate) }

fun gettingFirstImmutableMethod(
    fingerprint: Fingerprint,
): ReadOnlyProperty<BytecodePatchContext, Method> =
    ReadOnlyProperty { _, _ -> throw fingerprint.patchException() }

fun gettingFirstImmutableMethod(
    vararg strings: String,
    build: MutablePredicateList<Method>.() -> Unit = {},
): ReadOnlyProperty<BytecodePatchContext, Method> =
    gettingFirstImmutableMethodDeclaratively(*strings, build = build)

fun firstMethodComposite(build: MutablePredicateList<Method>.() -> Unit): Method.() -> Boolean = {
    matches(emptyList(), build)
}

fun BytecodePatchContext.firstMethodComposite(
    vararg strings: String,
    build: MutablePredicateList<Method>.() -> Unit = {},
): CompositeMatch = findCompositeMatchOrNull(strings.asList(), build) ?: throw PatchException("No matching method found")

fun BytecodePatchContext.firstMethodComposite(
    vararg strings: String,
    parameterTypePrefixes: List<String>,
    build: MutablePredicateList<Method>.() -> Unit = {},
): CompositeMatch = firstMethodComposite(*strings) {
    custom {
        parameters.map { it.type }.zip(parameterTypePrefixes).all { (actual, prefix) -> actual.startsWith(prefix) }
    }
    build()
}

fun composingFirstMethod(
    vararg strings: String,
    build: MutablePredicateList<Method>.() -> Unit = {},
): ReadOnlyProperty<BytecodePatchContext, CompositeMatch> =
    ReadOnlyProperty { thisRef, _ -> thisRef.firstMethodComposite(*strings, build = build) }

val Method.immutableClassDef: ClassDef
    get() = currentClassDef.get() ?: throw PatchException("No class context is available for method $this")

val MutableMethod.immutableClassDef: ClassDef
    get() = currentClassDef.get() ?: throw PatchException("No class context is available for method $this")

val MutableMethod.classDef: MutableClass
    get() = currentMutableClassDef.get()
        ?: methodClassMap[methodKey(this)]
        ?: throw PatchException("No mutable class context is available for method $this")

val Method.classDef: MutableClass
    get() = methodClassMap[methodKey(this)]
        ?: throw PatchException("No mutable class context is available for method $this")

val MutableMethod.method: MutableMethod
    get() = this

val Method.method: Method
    get() = this

val Match.immutableClassDef: ClassDef
    get() = originalClassDef

val Match.classDef: MutableClass
    get() = classDef

val CompositeMatch.methodOrNull: MutableMethod?
    get() = method

fun ClassDef.anyField(predicate: Field.() -> Boolean): Boolean = fields.any { it.predicate() }

fun ClassDef.anyStaticField(predicate: Field.() -> Boolean): Boolean = staticFields.any { it.predicate() }

fun BytecodePatchContext.firstImmutableClassDef(type: String): ClassDef = classDefBy { it.type.matchesDescriptor(type) }

fun BytecodePatchContext.firstImmutableClassDef(predicate: ClassDef.() -> Boolean): ClassDef = classDefBy { it.predicate() }

fun firstImmutableClassDef(type: String): ReadOnlyProperty<BytecodePatchContext, ClassDef> =
    ReadOnlyProperty { thisRef, _ -> thisRef.firstImmutableClassDef(type) }

fun gettingFirstImmutableClassDef(
    predicate: ClassDef.() -> Boolean,
): ReadOnlyProperty<BytecodePatchContext, ClassDef> =
    ReadOnlyProperty { thisRef, _ -> thisRef.firstImmutableClassDef(predicate) }

val BytecodePatchContext.classDefs: MutableClassDefCollection
    get() = MutableClassDefCollection(this)

class MutableClassDefCollection(private val context: BytecodePatchContext) : Iterable<ClassDef> {
    override fun iterator(): Iterator<ClassDef> = buildList {
        context.classDefForEach { add(it) }
    }.iterator()

    operator fun get(type: String): MutableClass? = context.mutableClassDefByOrNull(type)

    fun find(predicate: (ClassDef) -> Boolean): ClassDef? = iterator().asSequence().find(predicate)

    fun filter(predicate: (ClassDef) -> Boolean): List<ClassDef> = iterator().asSequence().filter(predicate).toList()

    fun forEach(action: (ClassDef) -> Unit) {
        iterator().forEach(action)
    }

    fun getOrReplaceMutable(classDef: ClassDef): MutableClass = context.mutableClassDefBy(classDef.type)

    fun add(classDef: ClassDef) {
        val patchClasses = context.javaClass.getMethod("getPatchClasses\$morphe_patcher").invoke(context)
        patchClasses.javaClass
            .getMethod("addClass\$morphe_patcher", ClassDef::class.java)
            .invoke(patchClasses, classDef)
    }
}

val ClassDef.virtualMethods: Iterable<Method>
    get() = methods.filter { it.accessFlags and AccessFlags.STATIC.value == 0 }

fun MutableClass.setSuperClass(type: String?) {
    if (type != null) setSuperClass(type)
}

fun allOf(vararg filters: Any): InstructionFilter = object : InstructionFilter {
    override fun matches(method: Method, instruction: Instruction): Boolean =
        filters.all { it.asInstructionFilter().matches(method, instruction) }
}

@JvmName("unorderedAllOfVararg")
fun unorderedAllOf(vararg filters: InstructionFilter): InstructionFilter = allOf(*filters)

@JvmName("unorderedAllOfArray")
fun unorderedAllOf(predicates: Array<InstructionFilter>): InstructionFilter = allOf(*predicates)

fun anyOf(vararg filters: Any): InstructionFilter = object : InstructionFilter {
    override fun matches(method: Method, instruction: Instruction): Boolean =
        filters.any { it.asInstructionFilter().matches(method, instruction) }
}

fun string(string: String, predicate: (String, String) -> Boolean): InstructionFilter =
    string.invoke(predicate)

fun string(string: String, predicate: String.() -> Boolean): InstructionFilter =
    string.invoke(predicate)

fun string(predicate: String.() -> Boolean): InstructionFilter = object : InstructionFilter {
    override fun matches(method: Method, instruction: Instruction): Boolean =
        ((instruction as? ReferenceInstruction)?.reference as? StringReference)?.string?.predicate() == true
}

fun after(filter: InstructionFilter): InstructionFilter = filter

fun after(opcode: Opcode): InstructionFilter = opcode()

fun afterAtMost(@Suppress("UNUSED_PARAMETER") count: Int, filter: InstructionFilter): InstructionFilter = filter

fun method(predicate: MethodReference.() -> Boolean): InstructionFilter = reference { (this as? MethodReference)?.predicate() == true }

fun method(): InstructionFilter = method { true }

fun method(name: String): InstructionFilter = method { this.name == name }

fun method(reference: MethodReference): InstructionFilter = method { this == reference || toString() == reference.toString() }

fun field(predicate: FieldReference.() -> Boolean): InstructionFilter = reference { (this as? FieldReference)?.predicate() == true }

fun field(): InstructionFilter = field { true }

fun field(name: String): InstructionFilter = field { this.name == name }

fun type(type: String): InstructionFilter = reference { (this as? TypeReference)?.type?.matchesDescriptor(type) == true }

fun type(predicate: String.() -> Boolean): InstructionFilter =
    reference { (this as? TypeReference)?.type?.predicate() == true }

fun reference(reference: String): InstructionFilter = reference { toString() == reference || toString().contains(reference) }

fun reference(predicate: Reference.() -> Boolean): InstructionFilter = object : InstructionFilter {
    override fun matches(method: Method, instruction: Instruction): Boolean {
        val reference = (instruction as? ReferenceInstruction)?.reference ?: return false
        return reference.predicate()
    }
}

operator fun Opcode.invoke(): InstructionFilter = object : InstructionFilter {
    override fun matches(method: Method, instruction: Instruction): Boolean = instruction.opcode == this@invoke
}

operator fun String.invoke(): InstructionFilter = object : InstructionFilter {
    override fun matches(method: Method, instruction: Instruction): Boolean =
        ((instruction as? ReferenceInstruction)?.reference as? StringReference)?.string == this@invoke
}

operator fun String.invoke(predicate: String.() -> Boolean): InstructionFilter = object : InstructionFilter {
    override fun matches(method: Method, instruction: Instruction): Boolean =
        ((instruction as? ReferenceInstruction)?.reference as? StringReference)?.string?.predicate() == true
}

operator fun String.invoke(predicate: (String, String) -> Boolean): InstructionFilter = object : InstructionFilter {
    override fun matches(method: Method, instruction: Instruction): Boolean {
        val actual = ((instruction as? ReferenceInstruction)?.reference as? StringReference)?.string ?: return false
        return predicate(actual, this@invoke)
    }
}

operator fun Long.invoke(): InstructionFilter = object : InstructionFilter {
    override fun matches(method: Method, instruction: Instruction): Boolean =
        (instruction as? com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction)?.wideLiteral == this@invoke
}

private fun Any.asInstructionFilter(): InstructionFilter = when (this) {
    is InstructionFilter -> this
    is Function4<*, *, *, *, *> -> object : InstructionFilter {
        override fun matches(method: Method, instruction: Instruction): Boolean =
            (this@asInstructionFilter as IndexedMatcherPredicate<Instruction>)
                .invoke(instruction, currentClassDef.get() ?: method.immutableClassDef, method, 0)
    }
    else -> object : InstructionFilter {
        override fun matches(method: Method, instruction: Instruction): Boolean = false
    }
}

private fun BytecodePatchContext.findImmutableMethodOrNull(
    strings: List<String>,
    build: MutablePredicateList<Method>.() -> Unit,
): Method? {
    var result: Method? = null
    classDefForEach { classDef ->
        if (result != null) return@classDefForEach
        currentClassDef.set(classDef)
        result = classDef.methods.firstOrNull { it.matches(strings, build) }
        currentClassDef.remove()
    }
    return result
}

private fun BytecodePatchContext.findMethodOrNull(
    strings: List<String>,
    build: MutablePredicateList<Method>.() -> Unit,
): MutableMethod? {
    var classType: String? = null
    val method = findImmutableMethodOrNull(strings, build) ?: return null
    classType = method.definingClass
    val mutableClass = mutableClassDefBy(classType)
    return mutableClass.methods.firstOrNull {
        it.name == method.name && it.returnType == method.returnType && it.parameters.matchesDescriptors(method.parameters.map(MethodParameter::getType))
    }?.also { registerMethodClass(it, mutableClass) } ?: method.toMutable().also { registerMethodClass(it, mutableClass) }
}

private fun BytecodePatchContext.findCompositeMatchOrNull(
    strings: List<String>,
    build: MutablePredicateList<Method>.() -> Unit,
): CompositeMatch? {
    var result: CompositeMatch? = null
    classDefForEach { classDef ->
        if (result != null) return@classDefForEach
        currentClassDef.set(classDef)
        val method = classDef.methods.firstOrNull { it.matches(strings, build) }
        currentClassDef.remove()
        if (method != null) {
            val mutableClass = mutableClassDefBy(classDef.type)
            val mutableMethod = mutableClass.methods.firstOrNull {
                it.name == method.name && it.returnType == method.returnType && it.parameters.matchesDescriptors(method.parameters.map(MethodParameter::getType))
            } ?: method.toMutable()
            registerMethodClass(mutableMethod, mutableClass)
            currentMutableClassDef.set(mutableClass)
            result = CompositeMatch(mutableClass, mutableMethod, listOf(method.matchingInstructionIndices(strings, build)))
            currentMutableClassDef.remove()
        }
    }
    return result
}

private fun Method.matches(
    strings: List<String>,
    build: MutablePredicateList<Method>.() -> Unit,
): Boolean {
    if (strings.isNotEmpty() && implementation?.instructions?.hasStrings(strings) != true) return false
    val predicates = MutablePredicateList<Method>().apply(build)
    return predicates.matches(this)
}

private fun Method.matchingInstructionIndices(
    strings: List<String>,
    build: MutablePredicateList<Method>.() -> Unit,
): List<Int> {
    val instructions = implementation?.instructions?.toList() ?: return emptyList()
    val predicates = MutablePredicateList<Method>().apply(build)
    val filterMatches = predicates.predicates.flatMap {
        instructions.withIndex().filter { (_, instruction) -> this.it() && instruction != null }.map { (index, _) -> index }
    }
    val stringMatches = strings.flatMap { expected ->
        instructions.withIndex().filter { (_, instruction) ->
            ((instruction as? ReferenceInstruction)?.reference as? StringReference)?.string?.contains(expected) == true
        }.map { (index, _) -> index }
    }
    return (filterMatches + stringMatches).distinct()
}

private fun Iterable<Instruction>.hasStrings(strings: List<String>): Boolean {
    if (strings.isEmpty()) return true
    val stringConstants = mapNotNull { ((it as? ReferenceInstruction)?.reference as? StringReference)?.string }
    return strings.all { expected -> stringConstants.any { it.contains(expected) } }
}

private fun Iterable<MethodParameter>.matchesDescriptors(expected: List<String>): Boolean {
    val actual = map { it.type }
    if (actual.size != expected.size) return false
    return actual.zip(expected).all { (parameter, expectation) -> parameter.matchesDescriptor(expectation) }
}

private fun String.matchesDescriptor(expected: String): Boolean =
    this == expected || startsWith(expected) || endsWith(expected)

private fun registerMethodClass(method: Method, classDef: MutableClass) {
    methodClassMap[methodKey(method)] = classDef
}

private fun methodKey(method: Method): String =
    "${method.definingClass}->${method.name}(${method.parameterTypes.joinToString("")})${method.returnType}"
