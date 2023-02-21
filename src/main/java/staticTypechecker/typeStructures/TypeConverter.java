package staticTypechecker.typeStructures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import jolie.lang.parse.ast.types.TypeChoiceDefinition;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeDefinitionUndefined;
import jolie.lang.parse.ast.types.TypeInlineDefinition;

/**
 * A static converter for the existing Jolie types. Converts them to my custom type used in the static typechecking, namely TypeNameDefinition and TypeStructure.
 * 
 * @author Kasper Bergstedt (kberg18@student.sdu.dk)
 */
public class TypeConverter {
	/**
	 * Creates a base instance of the structure of the specified type. "Base" in this case meaning only the root node have been made, it has no children.
	 * @param type the type to make the base instance from
	 * @return the base instance
	 */
	public static TypeStructure createBaseStructure(TypeDefinition type){
		if(type instanceof TypeInlineDefinition){
			TypeInlineDefinition tmp = (TypeInlineDefinition)type;
			return new TypeInlineStructure(tmp.basicType(), tmp.cardinality(), tmp.context());
		}

		if(type instanceof TypeChoiceDefinition){
			return new TypeChoiceStructure();
		}

		if(type instanceof TypeDefinitionLink){
			TypeDefinitionLink tmp1 = (TypeDefinitionLink)type;
			return TypeConverter.createBaseStructure(tmp1.linkedType());
		}

		if(type instanceof TypeDefinitionUndefined){
			return null;
		}

		return null;
	}

	/**
	 * Finalizes the given structure using the given type. 
	 * NOTE: if the structure is of type TypeInlineStructure, it will be finalized after this method.
	 * @param structure the structure object to finalize
	 * @param type the type definition to use when finalizing the struture
	 */
	public static void finalizeBaseStructure(TypeStructure struct, TypeDefinition type){
		// case where struct is a standard type definition
		if(struct instanceof TypeInlineStructure){
			TypeInlineStructure castedStruct = (TypeInlineStructure)struct;

			if(type instanceof TypeInlineDefinition){ // structure and definition are compatible, finalize the object
				TypeInlineDefinition castedType = (TypeInlineDefinition)type;
				TypeInlineStructure tmpStruct = (TypeInlineStructure)TypeConverter.convert(castedType); // create the structure in order to copy the children into the given structure

				for(Entry<String, TypeStructure> child : tmpStruct.children().entrySet()){
					castedStruct.put(child.getKey(), child.getValue());
				}
			}
			else if(type instanceof TypeDefinitionLink){ // an alias, finalize the linked type definition
				TypeConverter.finalizeBaseStructure(castedStruct, ((TypeDefinitionLink)type).linkedType());
			}
			else{ // struct and type def are incompatible, maybe throw error here TODO
				System.out.println("Incompatible struct and types");
				return;
			}

			castedStruct.finalize();
			return;
		}

		// case where struct is a choice type definition
		if(struct instanceof TypeChoiceStructure){
			TypeChoiceStructure castedStruct = (TypeChoiceStructure)struct;
			
			if(type instanceof TypeChoiceDefinition){ // struct and type def match
				TypeChoiceDefinition castedType = (TypeChoiceDefinition)type;
				TypeChoiceStructure tmpStruct = (TypeChoiceStructure)TypeConverter.convert(castedType);

				castedStruct.setChoices(tmpStruct.choices());
			}
			else if(type instanceof TypeDefinitionLink){ // an alias, finalize the linked type def
				TypeConverter.finalizeBaseStructure(castedStruct, ((TypeDefinitionLink)type).linkedType());
			}
			else{ // struct and type def are incompatible, maybe throw error here TODO
				System.out.println("Incompatible struct and types");
				return;
			}
		}
	}
	
	/**
	 * Creates a structure instance representing the structure of the given type.
	 * @param type the type to create the structure from
	 * @return the structure instance representing the specified type
	 */
	public static TypeStructure convert(TypeDefinition type){
		return TypeConverter.convert(type, true, new HashMap<String, TypeStructure>());
	}

	public static TypeStructure convertNoFinalize(TypeDefinition type){
		return TypeConverter.convert(type, false, new HashMap<String, TypeStructure>());
	}

	private static TypeStructure convert(TypeDefinition type, boolean finalize, HashMap<String, TypeStructure> recursiveTable){
		if(type instanceof TypeInlineDefinition){
			return TypeConverter.convert((TypeInlineDefinition)type, finalize, recursiveTable);
		}

		if(type instanceof TypeChoiceDefinition){
			return TypeConverter.convert((TypeChoiceDefinition)type, finalize, recursiveTable);
		}

		if(type instanceof TypeDefinitionLink){
			return TypeConverter.convert((TypeDefinitionLink)type, finalize, recursiveTable);
		}

		if(type instanceof TypeDefinitionUndefined){
			return TypeConverter.convert((TypeDefinitionUndefined)type, finalize, recursiveTable);
		}

		return null;
	}

	private static TypeInlineStructure convert(TypeInlineDefinition type, boolean finalize, HashMap<String, TypeStructure> recursiveTable){
		TypeInlineStructure structure = new TypeInlineStructure(type.basicType(), type.cardinality(), type.context());

		recursiveTable.put(type.name(), structure);

		if(type.subTypes() != null){ // type has children
			for(Entry<String, TypeDefinition> child : type.subTypes()){
				String childName = child.getKey();
				String typeName = "";
				
				if(child.getValue() instanceof TypeDefinitionLink){ // subtype is an alias for an existing type. In this case, we look for the linked type name instead of the alias
					TypeDefinitionLink subtype = (TypeDefinitionLink)child.getValue();
					typeName = subtype.linkedTypeName();
				}

				if(recursiveTable.containsKey(typeName)){
					structure.put(childName, recursiveTable.get(typeName));
				}
				else{
					TypeStructure subStructure = TypeConverter.convert(child.getValue(), finalize, recursiveTable);
					structure.put(childName, subStructure);
				}
			}
		}

		if(finalize){
			structure.finalize();
		}

		return structure;
	}

	private static TypeStructure convert(TypeChoiceDefinition type, boolean finalize, HashMap<String, TypeStructure> recursiveTable){
		HashSet<TypeInlineStructure> choices = new HashSet<>();
		TypeConverter.getChoices(type, choices, recursiveTable);
		return new TypeChoiceStructure(choices);
	}

	private static void getChoices(TypeDefinition type, HashSet<TypeInlineStructure> list, HashMap<String, TypeStructure> recursiveTable){
		if(type instanceof TypeChoiceDefinition){
			TypeConverter.getChoices(((TypeChoiceDefinition)type).left(), list, recursiveTable);
			TypeConverter.getChoices(((TypeChoiceDefinition)type).right(), list, recursiveTable);
		}
		else if(type instanceof TypeInlineDefinition){
			list.add( TypeConverter.convert((TypeInlineDefinition)type, false, recursiveTable) );
		}
		else if(type instanceof TypeDefinitionLink){
			TypeConverter.getChoices(((TypeDefinitionLink)type).linkedType(), list, recursiveTable);
		}
		else{
			System.out.println("CONVERTION NOT SUPPORTED");
		}
	}

	private static TypeStructure convert(TypeDefinitionLink type, boolean finalize, HashMap<String, TypeStructure> recursiveTable){
		return TypeConverter.convert(type.linkedType(), finalize, recursiveTable);
	}

	private static TypeStructure convert(TypeDefinitionUndefined type, boolean finalize, HashMap<String, TypeStructure> recursiveTable){
		return null;
	}
}
