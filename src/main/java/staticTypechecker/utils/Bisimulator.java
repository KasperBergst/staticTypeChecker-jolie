package staticTypechecker.utils;

import java.util.IdentityHashMap;
import java.util.Set;
import java.util.stream.Collectors;

import jolie.lang.NativeType;
import staticTypechecker.typeStructures.ChoiceType;
import staticTypechecker.typeStructures.InlineType;
import staticTypechecker.typeStructures.Type;

/**
 * Class for checking equality and subtype status of two types
 * 
 * @author Kasper Bergstedt (kberg18@student.sdu.dk)
 */
public class Bisimulator {
	public static boolean isSubtypeOf(Type t1, Type t2){
		return isSubtypeOfRec(t1, t2, new IdentityHashMap<>());
	}

	public static boolean equivalent(Type t1, Type t2){
		return isSubtypeOf(t1, t2) && isSubtypeOf(t2, t1);
	}

	private static boolean isSubtypeOfRec(Type t1, Type t2, IdentityHashMap<Type, Type> R){
		if(isInRelation(t1, t2, R)){
			return true;
		}

		R.put(t1, t2);
		if(t1 instanceof InlineType && t2 instanceof InlineType){
			InlineType x = (InlineType)t1;
			InlineType y = (InlineType)t2;

			Set<String> xLabels; 
			Set<String> yLabels;

			if(false){ // feature toggle
				// new
				// filters out all void children and maps the remaining to their name (key)
				xLabels = x.children().entrySet().stream().filter(ent -> ent.getValue().equals(Type.VOID())).map(ent -> ent.getKey()).collect(Collectors.toSet());
				yLabels = y.children().entrySet().stream().filter(ent -> ent.getValue().equals(Type.VOID())).map(ent -> ent.getKey()).collect(Collectors.toSet());
			}
			else{
				// old
				xLabels = x.children().keySet();
				yLabels = y.children().keySet();
			}

			if(!basicSubtype(x, y)){
				return false;
			}
			if(!isSubSetOf(yLabels, xLabels)){
				return false;
			}
			for(String label : yLabels){
				Type xChild = x.getChild(label);
				Type yChild = y.getChild(label);

				if(!isSubtypeOfRec(xChild, yChild, R)){
					return false;
				}
			}
			if(y.isClosed()){
				if(!isSubSetOf(xLabels, yLabels) || x.isOpen()){
					return false;
				}
			}
			return true;
		}
		else if(t1 instanceof InlineType && t2 instanceof ChoiceType){
			InlineType x = (InlineType)t1;
			ChoiceType y = (ChoiceType)t2;

			for(InlineType choice : y.choices()){
				if(isSubtypeOfRec(x, choice, R)){
					return true;
				}
			}
			return false;
		}
		else if(t1 instanceof ChoiceType && t2 instanceof InlineType){
			ChoiceType x = (ChoiceType)t1;
			InlineType y = (InlineType)t2;

			for(InlineType choice : x.choices()){
				if(!isSubtypeOfRec(choice, y, R)){
					return false;
				}
			}

			return true;
		}
		else{ // both choice types
			ChoiceType x = (ChoiceType)t1;
			ChoiceType y = (ChoiceType)t2;

			for(InlineType xChoice : x.choices()){
				boolean xChoiceIsSubtype = false;

				for(InlineType yChoice : y.choices()){
					if(isSubtypeOfRec(xChoice, yChoice, R)){
						xChoiceIsSubtype = true;
						break;
					}
				}

				if(!xChoiceIsSubtype){
					return false;
				}
			}

			return true;
		}
	}

	private static boolean isInRelation(Type t1, Type t2, IdentityHashMap<Type, Type> R){
		return R.containsKey(t1) && R.get(t1) == t2;
	}

	/**
	 * @param t1
	 * @param t2
	 * @return true if the basic type of t1 is a subtype of the basic type of t2
	 */
	private static boolean basicSubtype(InlineType t1, InlineType t2){
		if(t2.basicType().nativeType().equals(NativeType.ANY)){ // everything is a subtype of ANY, so return true
			return true;
		}

		// otherwise, it can only be subtype if they are equal
		return t1.basicType().equals(t2.basicType());
	}

	private static boolean isSubSetOf(Set<String> s1, Set<String> s2){
		for(String s : s1){
			if(!s2.contains(s)){
				return false;
			}
		}
		
		return true;
	}
}
