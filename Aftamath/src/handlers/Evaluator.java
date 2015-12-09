package handlers;

import main.Main;
import scenes.Script;

import com.badlogic.gdx.utils.Array;

import entities.Entity;
import entities.Mob;

public class Evaluator {
	
	private Main main;
	private Script script;

	public Evaluator(Main main) {
		 this.main = main;
	}
	
	public boolean evaluate(String statement){
		return evaluate(statement, null);
	}
	
	//can be pretty taxing to compute
	//computes boolean algebra from left to right
	public boolean evaluate(String origStatement, Script script){
		if(origStatement==null) return true;
		if(origStatement.isEmpty()) return true;
		this.script = script;
		String statement = origStatement;
			
		//split by and/or operators
		Array<String> arguments = new Array<>();
		while(statement.contains(" and ") || statement.contains(" or ")){
			int and = statement.indexOf(" and ");
			int or = statement.indexOf(" or ");
			int first = statement.indexOf("(");
			int last = statement.indexOf(")");
			boolean split = false;
			
			if(first>last || (first==-1 && last!=-1) || (first!=-1 && last==-1)){
				System.out.println("Mismatch parenthesis; \""+origStatement+"\"");
				if(this.script!=null) System.out.println("Line: "+(script.index+1)+"\tScript: "+script.ID);
				return false;
			}
			
			if(and < or){
				if(and > -1 && !(and>first && and<last)){
					arguments.add(statement.substring(0, statement.indexOf(" and ")));
					statement = statement.substring(and+ " and ".length());
					arguments.add("and");
					split = true;
				} 
				or = statement.indexOf(" or ");
				if(or > -1 && !(or>first && or<last)){
					arguments.add(statement.substring(0, statement.indexOf(" or ")));
					statement = statement.substring(or+ " or ".length());
					arguments.add("or");
					split = true;
				}
			} else {
				if(or > -1 && !(or>first && or<last)){
					arguments.add(statement.substring(0, statement.indexOf(" or ")));
					statement = statement.substring(or+ " or ".length());
					arguments.add("or");
					split = true;
				} 
				and = statement.indexOf(" and ");
				if(and > -1 && !(and>first && and<last)){
					arguments.add(statement.substring(0, statement.indexOf(" and ")));
					statement = statement.substring(and+ " and ".length());
					arguments.add("and");
					split = true;
				}
			}
			if(!split) break;
		}
		arguments.add(statement);
		
		if(arguments.size%2!=1){
			System.out.println("Invalid number of arguments; "+origStatement);
			if(this.script!=null) System.out.println("Line: "+(script.index+1)+"\tScript: "+script.ID);
		}
		
		String s; boolean not, result;
		//evaluate individual arguments
		for(int i = 0; i<arguments.size; i+=2){
			not = false;
			s = arguments.get(i).trim();
			if(s.startsWith("!")){
				not = true;
				s = s.substring(1);
			}
			
			//evaluate comparisons
			if(s.contains("[")){
				if(s.lastIndexOf("]")==-1){
					System.out.println("Mismatch braces; "+origStatement);
					if(this.script!=null) System.out.println("Line: "+(script.index+1)+"\tScript: "+script.ID);
					
					return false;
				}
				result = evaluateComparison(s.substring(s.indexOf("[")+1, 
						s.lastIndexOf("]")));
			//evaluate parentheticals by calling this method
			} else if(s.contains("(")) {
				result = evaluate(s.substring(s.indexOf("(")+1, s.lastIndexOf(")")), script);
			//evaluate boolean variables
			} else{
				String val = null;
				if(script!=null){
					val = String.valueOf(script.getVariable(s));
					if(val.equals("null")) val = null;
				}
				
				if(val==null||val.isEmpty()){
					if(main.history.getFlag(s)!=null){
						val = String.valueOf(main.history.getFlag(s));
					} else if(main.history.findEvent(s)){
						val = "true";
					} 
				}
				if(val!=null)
					result = Boolean.parseBoolean(val);
				else
					result = false;
			}
			
			if(not) result = !result;
			arguments.set(i, String.valueOf(result));
		}
		
		//combine by operators from left to right
		while(arguments.size>=3){
			if(arguments.get(2).equals("or")){
				arguments.set(1, String.valueOf(Boolean.parseBoolean(arguments.get(1)) ||
						Boolean.parseBoolean(arguments.get(3))));
			} else if(arguments.get(2).equals("and")){
				arguments.set(1, String.valueOf(Boolean.parseBoolean(arguments.get(1)) &&
						Boolean.parseBoolean(arguments.get(3))));
			}
			
			arguments.removeIndex(1);
			arguments.removeIndex(1);
		}
		return Boolean.parseBoolean(arguments.get(0));
	}
	
	//determines the boolean result of a comparison between expressions,
	//properties, and variables
	private boolean evaluateComparison(String statement){
		String obj, property = null;
		boolean result=false;

		if(statement.contains(">") || statement.contains("<") || statement.contains("=")){
			String value = "", val = "";

			//separate value and condition from tmp
			obj = "";
			String condition = "";
			String tmp = Vars.remove(statement, " "), index;
			int first=-1;
			for(int i = 0; i < tmp.length() - 1; i++){
				index = tmp.substring(i,i+1);
				if((index.equals(">") || index.equals("<") || index.equals("="))
						&& condition.length()<2){
					if(first==-1){
						condition += index;
						first = i;
					} else if (i-first==1)
						condition+=index;
				}
			}

			if(tmp.indexOf(condition)<1){
				System.out.println("No object found to compare with in statement: "+statement);
				if(script!=null)
					System.out.println("Line: "+(script.index+1)+"\tScript: "+script.ID);
				else
					return false;
			}

			obj = tmp.substring(0, tmp.indexOf(condition));
			val = tmp.substring(tmp.indexOf(condition)+condition.length());

			if(obj.contains("+")||obj.contains("-")||obj.contains("*")||obj.contains("/"))
				property = evaluateExpression(obj);
			else
				property = determineValue(obj, false);

			if(val.contains("+")||val.contains("-")||val.contains("*")||val.contains("/"))
				value = evaluateExpression(val);
			else
				value = determineValue(val, false);

//			System.out.println(statement);
//			System.out.println("p: " + property + "\tc: " + condition + "\tv: " + value);

			//actual comparator
			try{
				switch (condition){
				case "=":
					if (Vars.isNumeric(property) && Vars.isNumeric(value))
						result =(Double.parseDouble(property) == Double.parseDouble(value));
					else
						result = property.equals(value);
					break;
				case ">":
					if (Vars.isNumeric(property) && Vars.isNumeric(value))
						result = (Double.parseDouble(property) > Double.parseDouble(value));
					break;
				case ">=":
				case "=>":
					if (Vars.isNumeric(property) && Vars.isNumeric(value))
						result = (Double.parseDouble(property) >= Double.parseDouble(value));
					break;
				case "<":
					if (Vars.isNumeric(property) && Vars.isNumeric(value))
						result = (Double.parseDouble(property) < Double.parseDouble(value));
					break;
				case "<=":
				case "=<":
					if (Vars.isNumeric(property) && Vars.isNumeric(value))
						result = (Double.parseDouble(property) <= Double.parseDouble(value));
					break;
				default:
					System.out.println("\""+condition+"\" is not a vaild operator; Line: "+(script.index+1)+"\tScript: "+script.ID);
				}

//				System.out.println("result: "+result);
				return result;
			} catch(Exception e){
				System.out.println("Could not compare \""+property+"\" with \""+value+"\" by condition \""+condition+"\"; Line: "+(script.index+1)+"\tScript: "+script.ID);
				e.printStackTrace();
				return false;
			}
		} else
			return false;
	}
	
	//returns the solution to a set of mathematical operators
	private String evaluateExpression(String obj){
		Array<String> arguments;
		String result, res, val;
		String tmp=Vars.remove(obj," ");
		tmp=tmp.replace("-", "&");

		for(int i = 0; i<tmp.length()-1; i++){
			if(tmp.substring(i, i+1).equals("(")){
				if(tmp.lastIndexOf(")")==-1){
					System.out.println("Error evaluating: \"" +tmp +
							"\"\nMissing a \")\"; Line: "+(script.index+1)+"\tScript: "+script.ID);
					return null;
				}
				String e =evaluateExpression(tmp.substring(i+1, tmp.lastIndexOf(")")));
				tmp = tmp.substring(0,i)+e
						+tmp.substring(tmp.lastIndexOf(")")+1);
				i=tmp.lastIndexOf(")");
			}
		}

		//sort expressions
		//not programmed, sorry

		//evaluate
		//separates all arguments and contstants from operators
		arguments = new Array<>(tmp.split("(?<=[&+*/])|(?=[&+*/])"));
		if(arguments.size<3||arguments.contains("", false)) return obj;

		result = arguments.get(0);
		if(!Vars.isNumeric(result)){
			res = determineValue(result, false);
			if (res != null)
				result = new String(res);
		}

		//continuously evaluate operations until there aren't enough
		//left to perform a single operation
		while(arguments.size>=3){
			val = arguments.get(2);
			switch(arguments.get(1)){
			case"+":
				if(Vars.isNumeric(result)&& Vars.isNumeric(val)){
					result = String.valueOf(Float.parseFloat(result)+Float.parseFloat(val));
				}else{
					res = determineValue(result, false);
					if (res != null)
						result = new String(res);

					res = determineValue(val, false);
					if (res != null)
						val = new String(res);

					if(Vars.isNumeric(result)&& Vars.isNumeric(val))
						result = String.valueOf(Float.parseFloat(result)+Float.parseFloat(val));
					else result += val;
				}
				break;
			case"&":
				if(Vars.isNumeric(result)&& Vars.isNumeric(val)){
					result = String.valueOf(Float.parseFloat(result)-Float.parseFloat(val));
				}else{
					res = determineValue(result, false);
					if (res != null)
						result = new String(res);

					res = determineValue(val, false);
					if (res != null)
						val = new String(res);

					if(Vars.isNumeric(result)&& Vars.isNumeric(val))
						result = String.valueOf(Float.parseFloat(result)-Float.parseFloat(val));
					else System.out.println("Conversion error: res: \""+result+"\" op: - val: \""+val);
				}break;
			case"*":
				if(Vars.isNumeric(result)&& Vars.isNumeric(val)){
					result = String.valueOf(Float.parseFloat(result)*Float.parseFloat(val));
				}else{
					res = determineValue(result, false);
					if (res != null)
						result = new String(res);

					res = determineValue(val, false);
					if (res != null)
						val = new String(res);

					if(Vars.isNumeric(result)&& Vars.isNumeric(val))
						result = String.valueOf(Float.parseFloat(result)*Float.parseFloat(val));
					else System.out.println("Conversion error: res: \""+result+"\" op: - val: \""+val);
				}break;
			case"/":
				if(Vars.isNumeric(result)&& Vars.isNumeric(val)){
					result = String.valueOf(Float.parseFloat(result)/Float.parseFloat(val));
				}else{
					res = determineValue(result, false);
					if (res != null)
						result = new String(res);

					res = determineValue(val, false);
					if (res != null)
						val = new String(res);

					if(Vars.isNumeric(result)&& Vars.isNumeric(val))
						result = String.valueOf(Float.parseFloat(result)/Float.parseFloat(val));
					else System.out.println("Conversion error: res: \""+result+"\" op: - val: \""+val);
				}break;
			}

			arguments.removeIndex(1);
			arguments.removeIndex(1);
		}

		return result;
	}

	//determine wether argument is and object property, variable, flag, or event
	//by default, if no object can be found it is automatically assumed to be an event
	//should possibly change in the future;
	private String determineValue(String obj, boolean boolPossible){
		String prop = "", not = "", property = null;
		Object object=null;
		
		//ensure that invalid characters do not change the outcome
		obj = (obj.replace("[", "")).replace("]", "");

		if(Vars.isNumeric(obj))
			property = obj;
		//the object is a string and must be parsed out
		else if(obj.contains("{") && obj.contains("}"))
			return getSubstitutions(obj.substring(obj.indexOf("{")+1, obj.lastIndexOf("{")));
		//the value is an object's property
		else if(obj.contains(".")){
			prop = obj.substring(obj.indexOf(".")+1);
			obj = obj.substring(0, obj.indexOf("."));
			if(obj.startsWith("!")){
				obj = obj.substring(1);
				not = "!";
			}

			object = findObject(obj);
			if (object==null){
				System.out.println("Could not find object with name \"" +obj+"\"; Line: "+(script.index+1)+"\tScript: "+script.ID);
				return null;
			}

			property = findProperty(prop, object);

			if(property == null){
				System.out.println("\""+prop+"\" is an invalid object property for object \""+ obj+"\"; Line: "+(script.index+1)+"\tScript: "+script.ID);
				return null;
			}

			property = not + property;
		} else {
			//find variable
			if(script!=null)
				object = script.getVariable(obj);
			
			if (object==null)
				object = main.history.getVariable(obj);
			
			if (object!=null){
				if(object instanceof String)
					property = (String) object;
				else if(object instanceof Float)
					property = String.valueOf((Float) object);
				else if(object instanceof Integer)
					property = String.valueOf((Integer) object);
				else
					property = null;
			} else if (boolPossible){ 
				if(obj.startsWith("!")){
					not = "!";
					obj = obj.substring(1);
				}

				//find flag or event
				if(main.history.getFlag(obj)!=null){
					property = not+String.valueOf(main.history.getFlag(obj));
				} else 
					property = not + main.history.findEvent(obj);
			} else
				System.out.println("Could not determine value for \""+obj+"\"; Line: "+(script.index+1)+"\tScript: "+script.ID);
		}

		return property;
	}
	
	private Entity findObject(String objectName){
		Entity object = null;

		if(Vars.isNumeric(objectName)){
			for(Entity d : main.getObjects())
				if (d.getSceneID() == Integer.parseInt(objectName)) return d;
		} else switch(objectName) {
		case "player":
			object = main.character;
			break;
		case "partner":
			if(main.player.getPartner().getName() != null)
				object = main.player.getPartner();
			break;
		case "narrator":
			object = main.narrator;
			break;
		case "this":
			if(script!=null)
				object = script.getOwner();
			break;
		default:
			for(Entity d : main.getObjects()){
				if(d instanceof Mob){
					if (((Mob)d).getName().toLowerCase().equals(objectName.toLowerCase()))
						return d;
				} else
					if (d.ID.toLowerCase().equals(objectName.toLowerCase()))
						return d;
			}
		}
		return object;
	}
	
	private String findProperty(String prop, Object object){
		String property = null;
		switch(prop.toLowerCase()){
		case "name":
			if(object instanceof Mob)
				property = ((Mob)object).getName();
			break;
		case "health":
			if(object instanceof Mob) 
				property = String.valueOf(((Mob)object).getHealth());
			break;
		case "money":
			property = String.valueOf(Double.valueOf(main.player.getMoney()).longValue());
			break;
		case "gender":
			if(object instanceof Mob) 
				property = ((Mob)object).getGender();
			break;
		case "love":
		case "relationship":
			property = String.valueOf(main.player.getRelationship());
			break;
		case "niceness": 
			property = String.valueOf(main.player.getNiceness());
			break;
		case "bravery": 
			property = String.valueOf(main.player.getBravery());
			break; 
		case "lovescale":
			property = String.valueOf(main.player.getLoveScale());
			break;
		case "nicenessscale": 
			property = String.valueOf(main.player.getNicenessScale());
			break;
		case "braveryscale":
			property = String.valueOf(main.player.getBraveryScale());
			break;
		case "house": 
			property = (main.player.getHome().getType());
			break;
//		case "haspartner":
//			if(object instanceof Player)
//				if (player.getPartner()!=null){
//					if(player.getPartner().getName()!=null)
//						if(!player.getPartner().getName().equals(""))
//							property = String.valueOf(true);
//				} else
//					property = String.valueOf(false);
//			break;
		case "location":
			if(object instanceof Entity)
				property = String.valueOf(((Entity)object).getPosition().x);
			break;
		case "power":
		case "level":
			if(object instanceof Mob)
				property = String.valueOf(((Mob)object).getLevel());
			break;
		case "powertype":
			if(object instanceof Mob)
				property = String.valueOf(((Mob)object).getPowerType());
			break;
		}
		return property;
	}
	
	//	string substitutions
	private String getSubstitutions(String txt){
		while(txt.contains("/")){
			if(txt.contains("/player")){
				txt = txt.substring(0, txt.indexOf("/player")) + main.character.getName() + 
						txt.substring(txt.indexOf("/player") + "/player".length());
			}if(txt.contains("/playerg")){
				String g = "guy";
				if(main.character.getGender().equals("female")) g="girl";
				txt = txt.substring(0, txt.indexOf("/playerg")) + g + 
						txt.substring(txt.indexOf("/playerg") + "/playerg".length());
			}if(txt.contains("/playergps")){
				String g = "his";
				if(main.character.getGender().equals("female")) g="her";
				txt = txt.substring(0, txt.indexOf("/playergps")) +g + 
						txt.substring(txt.indexOf("/playergps") + "/playergps".length());
			}if(txt.contains("/playergp")){
				String g = "his";
				if(main.character.getGender().equals("female")) g="hers";
				txt = txt.substring(0, txt.indexOf("/playergp")) + g + 
						txt.substring(txt.indexOf("/playergp") + "/playergp".length());
			}if(txt.contains("/playergo")){
				String g = "he";
				if(main.character.getGender().equals("female")) g="she";
				txt = txt.substring(0, txt.indexOf("/playergo")) + g + 
						txt.substring(txt.indexOf("/playergo") + "/playergo".length());
			} if(txt.contains("/partner")){
				String s = "";
				if(main.player.getPartner()==null)
					s=main.player.getPartner().getName();
				txt = txt.substring(0, txt.indexOf("/partner")) + s + 
						txt.substring(txt.indexOf("/partner") + "/partner".length());
			}if(txt.contains("/partnerg")){
				String g = "";
				if(main.player.getPartner()!=null){
					if(main.player.getPartner().getGender().equals("female")) g="girl";
					else g = "guy"; 
				}
				txt = txt.substring(0, txt.indexOf("/partnerg")) + g + 
						txt.substring(txt.indexOf("/partnerg") + "/partnerg".length());
			}if(txt.contains("/partnergps")){
				String g = "";
				if(main.player.getPartner()!=null){
					if(main.player.getPartner().getGender().equals("female")) g="her";
					else g = "his"; 
				}
				txt = txt.substring(0, txt.indexOf("/partnergps")) + g + 
						txt.substring(txt.indexOf("/partnergps") + "/partnergps".length());
			}if(txt.contains("/partnergp")){
				String g = "";
				if(main.player.getPartner()!=null){
					if(main.player.getPartner().getGender().equals("female")) g="hers";
					else g = "his"; 
				}
				txt = txt.substring(0, txt.indexOf("/partnergp")) + g + 
						txt.substring(txt.indexOf("/partnergp") + "/partnergp".length());
			}if(txt.contains("/partnergo")){
				String g = "";
				if(main.player.getPartner()!=null){
					if(main.player.getPartner().getGender().equals("female")) g="she";
					else g = "he"; 
				}
				txt = txt.substring(0, txt.indexOf("/partnergo")) + g + 
						txt.substring(txt.indexOf("/partnergo") + "/partnergo".length());
			} if (txt.contains("/partnert")) {
				txt = txt.substring(0, txt.indexOf("/partnert")) + main.player.getPartnerTitle() + 
						txt.substring(txt.indexOf("/partnergt") + "/partnergt".length());
			} if(txt.contains("/house")){
				txt = txt.substring(0, txt.indexOf("/house")) + main.player.getHome().getType() + 
						txt.substring(txt.indexOf("/house") + "/house".length());
			} if(txt.contains("/address")){
				txt = txt.substring(0, txt.indexOf("/address")) + main.player.getHome().getType() + 
						txt.substring(txt.indexOf("/address") + "/address".length());
			} if(txt.contains("/variable[")&& txt.indexOf("]")>=0){
				String varName = txt.substring(txt.indexOf("/variable[")+"/variable[".length(), txt.indexOf("]"));
				Object var = main.history.getVariable(varName);
				if(var!= null) {
					txt = txt.substring(0, txt.indexOf("/variable[")) + var +
							txt.substring(txt.indexOf("/variable[")+"/variable[".length()+ varName.length() + 1);
				}
			}
		}
		
		return txt;
	}
}
