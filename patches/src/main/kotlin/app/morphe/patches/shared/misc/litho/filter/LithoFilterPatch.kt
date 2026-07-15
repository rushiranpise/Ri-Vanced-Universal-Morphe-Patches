@file:Suppress("SpellCheckingInspection")

package app.morphe.patches.shared.misc.litho.filter

import app.morphe.util.getFreeRegisterProvider
import app.morphe.patcher.util.proxy.mutableTypes.encodedValue.MutableEncodedValue.Companion.toMutable
import app.morphe.patcher.afterAtMost
import app.morphe.patcher.allOf
import app.morphe.patcher.classDef
import app.morphe.patcher.custom
import app.morphe.patcher.extensions.addInstructions
import app.morphe.patcher.extensions.fieldReference
import app.morphe.patcher.extensions.getInstruction
import app.morphe.patcher.extensions.methodReference
import app.morphe.patcher.extensions.removeInstructions
import app.morphe.patcher.extensions.typeReference
import app.morphe.patcher.firstImmutableClassDef
import app.morphe.patcher.firstMethodComposite
import app.morphe.patcher.immutableClassDef
import app.morphe.patcher.instructions
import app.morphe.patcher.invoke
import app.morphe.patcher.method
import app.morphe.patcher.patch.BytecodePatchBuilder
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.returnType
import app.morphe.patches.shared.misc.extension.sharedExtensionPatch
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.findFieldFromToString
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.immutable.value.ImmutableBooleanEncodedValue

/**
 * Used to add a hook point to the extension stub.
 */
lateinit var addLithoFilter: (String) -> Unit
    private set

/**
 * Counts the number of filters added to the static field array.
 */
private var filterCount = 0

internal const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/morphe/extension/shared/patches/litho/LithoFilterPatch;"

/**
 * A patch that allows to filter Litho components based on their identifier or path.
 *
 * @param componentCreateInsertionIndex The index to insert the filtering code in the component create method.
 * @param insertProtobufHook This method injects a setProtoBuffer call in the protobuf decoding logic.
 * @param getConversionContextToStringMethod The getter of the conversion context toString method.
 * @param getExtractIdentifierFromBuffer Whether to extract the identifier from the protobuf buffer.
 * @param executeBlock The additional execution block of the patch.
 * @param block The additional block to build the patch.
 */
internal fun lithoFilterPatch(
    componentCreateInsertionIndex: Method.() -> Int,
    insertProtobufHook: BytecodePatchContext.() -> Unit,
    executeBlock: BytecodePatchContext.() -> Unit = {},
    getConversionContextToStringMethod: BytecodePatchContext.() -> Method,
    getExtractIdentifierFromBuffer: () -> Boolean = { false },
    block: BytecodePatchBuilder.() -> Unit = {},
) = bytecodePatch(
    description = "Hooks the method which parses the bytes into a ComponentContext to filter components.",
) {
    dependsOn(
        sharedExtensionPatch(),
    )

    /**
     * The following patch inserts a hook into the method that parses the bytes into a ComponentContext.
     * This method contains a StringBuilder object that represents the pathBuilder of the component.
     * The pathBuilder is used to filter components by their path.
     *
     * Additionally, the method contains a reference to the component's identifier.
     * The identifier is used to filter components by their identifier.
     *
     * The protobuf buffer is passed along from a different injection point before the filtering occurs.
     * The buffer is a large byte array that represents the component tree.
     * This byte array is searched for strings that indicate the current component.
     *
     * All modifications done here must allow all the original code to still execute
     * even when filtering, otherwise memory leaks or poor app performance may occur.
     *
     * The following pseudocode shows how this patch works:
     *
     * class SomeOtherClass {
     *    // Called before ComponentContextParser.parseComponent() method.
     *    public void someOtherMethod(ByteBuffer byteBuffer) {
     *        ExtensionClass.setProtoBuffer(byteBuffer); // Inserted by this patch.
     *        ...
     *   }
     * }
     *
     * class CreateComponentClass {
     *    public Component createComponent() {
     *        ...
     *
     *        if (extensionClass.shouldFilter(identifier, path)) {  // Inserted by this patch.
     *            return emptyComponent;
     *        }
     *        return originalUnpatchedComponent; // Original code.
     *    }
     * }
     */
    execute {
        // Remove dummy filter from extenion static field
        // and add the filters included during patching.
        lithoFilterInitMethod.apply {
            // Remove the array initialization with the dummy filter.
            removeInstructions(6)

            addInstructions(
                0,
                "new-array v1, v1, [Lapp/morphe/extension/shared/patches/litho/Filter;"
            )

            // Fill the array with the filters added during patching.
            addLithoFilter = { classDescriptor ->
                addInstructions(
                    1,
                    """
                        new-instance v0, $classDescriptor
                        invoke-direct { v0 }, $classDescriptor-><init>()V
                        const/16 v2, ${filterCount++}
                        aput-object v0, v1, v2
                    """,
                )
            }
        }

        // Tell the extension whether to extract the identifier from the buffer.
        if (getExtractIdentifierFromBuffer()) {
            lithoFilterInitMethod.apply {
                val index = indexOfFirstInstruction {
                    fieldReference?.name == "EXTRACT_IDENTIFIER_FROM_BUFFER"
                }

                val freeRegister = getFreeRegisterProvider(index, 1)
                    .getFreeRegister()

                addInstructions(
                    index + 1,
                    """
                        const/4 v$freeRegister, 0x1
                        sput-boolean v$freeRegister, ${EXTENSION_CLASS_DESCRIPTOR}->EXTRACT_IDENTIFIER_FROM_BUFFER:Z
                    """
                )
            }
        }

        // Add an interceptor to steal the protobuf of our component.
        insertProtobufHook()

        // Hook the method that parses bytes into a ComponentContext.
        // Allow the method to run to completion, and override the
        // return value with an empty component if it should be filtered.
        // It is important to allow the original code to always run to completion,
        // otherwise high memory usage and poor app performance can occur.

        val conversionContextToStringMethod = getConversionContextToStringMethod()

        // Find the identifier/path fields of the conversion context.
        val conversionContextIdentifierField = conversionContextToStringMethod
            .findFieldFromToString("identifierProperty=")

        val conversionContextPathBuilderField = conversionContextToStringMethod.immutableClassDef
            .fields.single { field -> field.type == "Ljava/lang/StringBuilder;" }

        // Find class and methods to create an empty component.
        val builderMethodDescriptor = emptyComponentMethod.immutableClassDef.methods.single {
            // The only static method in the class.
                method ->
            AccessFlags.STATIC.isSet(method.accessFlags)
        }

        val emptyComponentField = firstImmutableClassDef {
            // Only one field that matches.
            type == builderMethodDescriptor.returnType
        }.fields.single()

        // Find the method call that gets the value of 'buttonViewModel.accessibilityId'.
        val accessibilityIdMethod = accessibilityIdMethodMatch.let {
            it.immutableMethod.getInstruction<ReferenceInstruction>(it[0]).methodReference!!
        }

        // There's a method in the same class that gets the value of 'buttonViewModel.accessibilityText'.
        // As this class is abstract, another method that uses a method call is used.
        val accessibilityTextMethod = getAccessibilityTextMethodMatch(accessibilityIdMethod).let {
            // Find the method call that gets the value of 'buttonViewModel.accessibilityText'.
            it.method.getInstruction<ReferenceInstruction>(it[0]).methodReference
        }

        componentCreateMethod.apply {
            val insertIndex = componentCreateInsertionIndex()

            // Directly access the class related with the buttonViewModel from this method.
            // This is within 10 lines of insertIndex.
            val buttonViewModelIndex = indexOfFirstInstructionReversedOrThrow(insertIndex) {
                opcode == Opcode.CHECK_CAST &&
                        typeReference?.type == accessibilityIdMethod.definingClass
            }
            val buttonViewModelRegister =
                getInstruction<OneRegisterInstruction>(buttonViewModelIndex).registerA
            val accessibilityIdIndex = buttonViewModelIndex + 2

            // This is an index that checks if there is accessibility-related text.
            // This is within 10 lines of buttonViewModelIndex.
            val nullCheckIndex = indexOfFirstInstructionReversedOrThrow(
                buttonViewModelIndex, Opcode.IF_EQZ
            )

            val registerProvider = getFreeRegisterProvider(
                insertIndex, 3, buttonViewModelRegister
            )
            val freeRegister = registerProvider.getFreeRegister()
            val identifierRegister = registerProvider.getFreeRegister()
            val pathRegister = registerProvider.getFreeRegister()

            // Find a free register to store the accessibilityId and accessibilityText.
            // This is before the insertion index.
            val accessibilityRegisterProvider = getFreeRegisterProvider(
                nullCheckIndex,
                2,
                registerProvider.getUsedAndUnAvailableRegisters()
            )
            val accessibilityIdRegister = accessibilityRegisterProvider.getFreeRegister()
            val accessibilityTextRegister = accessibilityRegisterProvider.getFreeRegister()

            addInstructionsAtControlFlowLabel(
                insertIndex,
                """
                    move-object/from16 v$freeRegister, p2 # ConversionContext parameter
                    
                    # In YouTube 20.41 the field is the abstract superclass.
                    # Verify it's the expected subclass just in case. 
                    instance-of v$identifierRegister, v$freeRegister, ${conversionContextToStringMethod.immutableClassDef.type}
                    if-eqz v$identifierRegister, :unfiltered
                    
                    iget-object v$identifierRegister, v$freeRegister, $conversionContextIdentifierField
                    iget-object v$pathRegister, v$freeRegister, $conversionContextPathBuilderField
                    invoke-static { v$identifierRegister, v$accessibilityIdRegister, v$accessibilityTextRegister, v$pathRegister }, ${EXTENSION_CLASS_DESCRIPTOR}->isFiltered(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/StringBuilder;)Z
                    move-result v$freeRegister
                    if-eqz v$freeRegister, :unfiltered
                    
                    # Return an empty component
                    move-object/from16 v$freeRegister, p1
                    invoke-static { v$freeRegister }, $builderMethodDescriptor
                    move-result-object v$freeRegister
                    iget-object v$freeRegister, v$freeRegister, $emptyComponentField
                    return-object v$freeRegister
        
                    :unfiltered
                    nop
                """
            )

            // If there is text related to accessibility, get the accessibilityId and accessibilityText.
            addInstructions(
                accessibilityIdIndex,
                """
                    # Get accessibilityId
                    invoke-interface { v$buttonViewModelRegister }, $accessibilityIdMethod
                    move-result-object v$accessibilityIdRegister
                    
                    # Get accessibilityText
                    invoke-interface { v$buttonViewModelRegister }, $accessibilityTextMethod
                    move-result-object v$accessibilityTextRegister
                """
            )

            // If there is no accessibility-related text,
            // both accessibilityId and accessibilityText use empty values.
            addInstructions(
                nullCheckIndex,
                """
                    const-string v$accessibilityIdRegister, ""
                    const-string v$accessibilityTextRegister, ""
                """
            )
        }

        // TODO: Check if needed in music.
        // Change Litho thread executor to 1 thread to fix layout issue in unpatched YouTube.
        lithoThreadExecutorMethod.addInstructions(
            0,
            """
                invoke-static { p1 }, $EXTENSION_CLASS_DESCRIPTOR->getExecutorCorePoolSize(I)I
                move-result p1
                invoke-static { p2 }, $EXTENSION_CLASS_DESCRIPTOR->getExecutorMaxThreads(I)I
                move-result p2
            """,
        )

        executeBlock()
    }

    finalize {
        // Set the array size to the actual filter count of the array
        // initialized at the beginning of the patch.
        lithoFilterInitMethod.addInstructions(0, "const/16 v1, $filterCount")
    }

    block()
}
