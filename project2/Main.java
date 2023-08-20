import syntaxtree.*;
import visitor.*;

import java.util.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {
        if(args.length < 1){
            System.err.println("Usage: java Main <file1> <file2> ... <fileN>");
            System.exit(1);
        }

        FileInputStream fis = null;
        for(int i = 0; i < args.length; i++)
        {
            try{
            
                fis = new FileInputStream(args[i]);
                MiniJavaParser parser = new MiniJavaParser(fis);
    
                Goal root = parser.Goal();
    
                System.err.println(args[i] + " parsed successfully.");
    
                MyVisitor eval = new MyVisitor();
                root.accept(eval, null);

            }
            catch(ParseException ex){
                System.out.println(ex.getMessage());
            }
            catch(FileNotFoundException ex){
                System.err.println(ex.getMessage());
            }
            finally{
                try{
                    if(fis != null) fis.close();
                }
                catch(IOException ex){
                    System.err.println(ex.getMessage());
                }
            }

            System.out.println();
        }
        
        
    }
}


class MyVisitor extends GJDepthFirst<String, Void>{

    // List that stores all class names of a program
    List<String> classes;
    // map that stores a variable with the function it is included in (scoping)
    Map<String, String> funcVariable;
    // map that stores a class field with its class (scoping)
    Map<String, String> classObject;
    // map that stores a class function with its class (scoping)
    Map<String, String> classFunc;
    // map that stores a class with its parent class (inheritance)
    Map<String, String> inheritance;
    // map that stores a variable with its type
    Map<String, String> objectType;
    // map that stores a variable inside a function with its type
    Map<String, String> objectTypeforFunc;
    // map that stores a function with its return type
    Map<String, String> funcType;
    // map that stores a function with the type of all the arguments needed
    Map<String, String> func_argtypes;
    // temporary list that stores parameter types of current function
    List<String> param_types;
    // temporary list that stores argument types of current function call
    List<String> arg_types;
    // checks whether the variable is inside a function or not
    boolean in_func;
    // stores current scope
    String scope;
    // stores current function
    String func_scope;
    // overall offset of class fields
    int field_offset;
    // overall offset of class methods
    int func_offset;

    public MyVisitor()
    {
        classes = new ArrayList<String>();
        classObject = new HashMap<String, String>();
        classFunc = new HashMap<String, String>(); 
        inheritance = new HashMap<String, String>();
        funcVariable = new HashMap<String, String>();
        objectType = new HashMap<String, String>();
        objectTypeforFunc = new HashMap<String, String>();
        funcType = new HashMap<String, String>();
        func_argtypes = new HashMap<String, String>();
        param_types = new ArrayList<String>();
        arg_types = new ArrayList<String>();
        field_offset = 0;
        func_offset = 0;
        in_func = false;
    }


    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "void"
     * f6 -> "main"
     * f7 -> "("
     * f8 -> "String"
     * f9 -> "["
     * f10 -> "]"
     * f11 -> Identifier()
     * f12 -> ")"
     * f13 -> "{"
     * f14 -> ( VarDeclaration() )*
     * f15 -> ( Statement() )*
     * f16 -> "}"
     * f17 -> "}"
     */
    @Override
    public String visit(MainClass n, Void argu) throws Exception {

        // we are inside the main function
        in_func = true;
        func_scope = "main";

        String classname = n.f1.accept(this, null);
        scope = classname;

        String args = n.f11.accept(this, null);

        String vars = "";
        if(n.f7 != null) vars = n.f14.accept(this, null);
        String statement = "";
        if(n.f8 != null) statement = n.f15.accept(this, null);

        System.out.println();

        classes = new ArrayList<String>();
        classObject = new HashMap<String, String>();
        classFunc = new HashMap<String, String>(); 
        inheritance = new HashMap<String, String>();
        funcVariable = new HashMap<String, String>();
        objectType = new HashMap<String, String>();
        objectTypeforFunc = new HashMap<String, String>();
        funcType = new HashMap<String, String>();
        func_argtypes = new HashMap<String, String>();

        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    @Override
    public String visit(ClassDeclaration n, Void argu) throws Exception {

        // we are not inside a function
        in_func = false;
        func_scope = "";

        String classname = n.f1.accept(this, null);
  
        // checking if the class has been declared before or not
        if(classes.contains(classname)) throw new ParseException("Semantic Error, Class: " + classname + " already exists");
        classes.add(classname);
        scope = classname;

        super.visit(n, argu);

        System.out.println();

        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */
    @Override
    public String visit(ClassExtendsDeclaration n, Void argu) throws Exception {

        // we are not inside a function
        in_func = false;
        func_scope = "";

        String classname = n.f1.accept(this, null);

        // checking if the class has been declared before or not
        if(classes.contains(classname)) throw new ParseException("Semantic Error, Class: " + classname + " already exists");
        classes.add(classname);
        scope = classname;

        String parent_class = n.f3.accept(this, null);
        inheritance.put(classname, parent_class); // the program should remember if the class has a parent class or not

        super.visit(n, argu);

        System.out.println();

        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    @Override
    public String visit(VarDeclaration n, Void argu) throws Exception{
        String type = n.f0.accept(this, null);
        String name = n.f1.accept(this, null);

        // checking if the variable has been declared before or not
        if(is_declared(name)) throw new ParseException("Semantic Error, " + name + " has already been declared");

        // if the variable is not inside a function, we will compute its offset
        if(!in_func)
        {
            classObject.put(name, scope); 
            objectType.put(name, type);
            System.out.println(scope + "." + name + " : " + field_offset);
            if(type == "int") field_offset+=4; // integer is 4 bytes
            else if(type == "boolean") field_offset++; // boolean is 1 byte 
            else field_offset+=8; // everything else is a pointer so 8 bytes  
        }
        else
        {
            if(funcVariable.get(name) != func_scope)
            {
                funcVariable.put(name, func_scope);
                objectTypeforFunc.put(name, type);
            }
        }


        return type + " " + name;
    }

    /**
     * f0 -> "public"
     * f1 -> Type()
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( FormalParameterList() )?
     * f5 -> ")"
     * f6 -> "{"
     * f7 -> ( VarDeclaration() )*
     * f8 -> ( Statement() )*
     * f9 -> "return"
     * f10 -> Expression()
     * f11 -> ";"
     * f12 -> "}"
     */
    @Override
    public String visit(MethodDeclaration n, Void argu) throws Exception {

        // we are inside a function
        in_func = true;

        String type = n.f1.accept(this, null);
        String myName = n.f2.accept(this, null);

        funcType.put(myName, type);

        // checking whether the function has been inherited from the parent class 
        boolean exists = false;

        // checking if the class that contains the function is inherited
        if(inheritance.containsKey(scope) == true)
        {
            String parent_class = inheritance.get(scope);

            // iterate each entry of hashmap
            for(Entry<String, String> entry: classFunc.entrySet()) {

                // if give value is equal to value from entry
                if(entry.getKey() == myName && entry.getValue() == parent_class) {
                    
                    // the function is inherited
                    exists = true;
                    break;
                }
            }
        }
        
        func_scope = myName;
        // we will compute the offset of the non inherited functions only
        if(!exists)
        {
            // checking if the function has been declared before or not
            if(classFunc.get(myName) == scope) throw new ParseException("Semantic Error, function " + myName + " has already been declared");
            classFunc.put(myName, scope); // inserting the function inside the scope map for later checks
            System.out.println(scope + "." + myName + " : " + func_offset);
            func_offset+=8; // functions are 8 bytes
        }

        String parameters = n.f4.accept(this, null);

        String vars = "";
        if(n.f7 != null) vars = n.f7.accept(this, null);
        String statement = "";
        if(n.f8 != null) statement = n.f8.accept(this, null);

        String returned = n.f10.accept(this, null);

        // checking if the thing the function returns the same type of thing as it is
        if(!check_return_type(myName, returned)) throw new ParseException("Semantic Error, wrong return type");
        
        // emptying and rearranging the maps for the next function
        objectTypeforFunc = new HashMap<String, String>();
        funcVariable = new HashMap<String, String>();

        return null;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    @Override
    public String visit(FormalParameterList n, Void argu) throws Exception {
        String ret = n.f0.accept(this, null);

        if (n.f1 != null) {
            ret += n.f1.accept(this, null);
        }

        return ret;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    @Override
    public String visit(FormalParameter n, Void argu) throws Exception {
        String type = n.f0.accept(this, null);
        String name = n.f1.accept(this, null);

        funcVariable.put(name, func_scope);
        objectTypeforFunc.put(name, type);

        return type + " " + name;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    public String visit(FormalParameterTerm n, Void argu) throws Exception {
        String first_parameter = n.f0.accept(this, null);

        String rest_of_them = n.f1.accept(this, null);
        return first_parameter + rest_of_them;
    }

    /**
     * f0 -> ","
     * f1 -> FormalParameter()
     */
    @Override
    public String visit(FormalParameterTail n, Void argu) throws Exception {
        String ret = "";
        for ( Node node: n.f0.nodes) {
            String parameter = node.accept(this, null);
            ret += ", " + parameter;
        }

        return ret;
    }

    public String visit(Type n, Void argu) throws Exception {
        
        return n.f0.accept(this, null);
    }

    public String visit(ArrayType n, Void argu) throws Exception{
        
        return n.f0.accept(this, null);
        
    }

    @Override
    public String visit(IntegerArrayType n, Void argu) throws Exception{
        return "int[]";
    }

    public String visit(BooleanArrayType n, Void argu) throws Exception{
        return "boolean[]";
    }

    public String visit(BooleanType n, Void argu) throws Exception{
        return "boolean";
    }

    public String visit(IntegerType n, Void argu) throws Exception{
        return "int";
    }

    public String visit(Statement n, Void argu) throws Exception{

        return n.f0.accept(this, null);
    }

    /**
     * f1 -> "{"
     * f2 -> ( Statement() )*
     * f3 -> "}"
     */
    public String visit(Block n, Void argu) throws Exception{

        return super.visit(n, argu);
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    @Override
    public String visit(AssignmentStatement n, Void argu) throws Exception{
        String to_be_assigned = n.f0.accept(this, null);
        String assignment = n.f2.accept(this, null);
       
        // Checking if the identifiers from both ends of the assignment have the same type
        if(is_identifier(to_be_assigned) && is_identifier(assignment))
        {
            String type_a = get_expression_type(to_be_assigned);
            String type_b = get_expression_type(assignment);
            if(!(type_a.equals(type_b))) throw new ParseException("Semantic Error, wrong assignment type " + to_be_assigned + " and " + assignment);
        }
        

        // Checking if the identifier on the assigned end has been declared in current or parent scopes or not
        if(in_func)
        {
            if(is_identifier(to_be_assigned))
            {
                if(funcVariable.get(to_be_assigned) != func_scope) 
                {
                    if(classObject.get(to_be_assigned) != scope) 
                    {
                        String parent = scope;
                        while(true)
                        {
                            parent = inheritance.get(parent);
                            if(parent == null) throw new ParseException("Semantic Error, " + to_be_assigned + " undeclared");
                            else if(classObject.get(to_be_assigned) != parent) continue;
                            else break;
                        }
                        
                        
                    }
                }
                
            }
        }

        // Checking if the identifier on the assignment end has been declared in current or parent scopes or not
        if(is_identifier(assignment)) 
        {
           if(!is_declared(assignment))
           {
               throw new ParseException("Semantic Error, undeclared");
           } 
        }
        // Checking if we have allocated an array to a non array variable
        else if(is_array_allocation(assignment))
        {
            String type = objectTypeforFunc.get(to_be_assigned);
            if(type == null) type = objectType.get(to_be_assigned);
            if(!(type.equals("int[]")) && !(type.equals("boolean[]"))) throw new ParseException("Semantic Error, wrong allocation");
        }
        return to_be_assigned + "=" + assignment + ";"; 
    }


    /**
     * f0 -> Identifier()
     * f1 -> "["
     * f2 -> Expression()
     * f3 -> "]"
     * f4 -> "="
     * f5 -> Expression()
     * f6 -> ";"
     */
    @Override
    public String visit(ArrayAssignmentStatement n, Void argu) throws Exception{
        String to_be_assigned = n.f0.accept(this, null);

        // Checking if the variable to be assigned is an array variable or not
        String array_type = objectTypeforFunc.get(to_be_assigned);
        if(array_type == null) array_type = objectType.get(to_be_assigned);
        if(!(array_type.equals("int[]")) && !(array_type.equals("boolean[]"))) throw new ParseException("Semantic Error, wrong assignment type " + to_be_assigned);

        String index = n.f2.accept(this, null);
        // Checking if the index is integer or not
        if(!(get_expression_type(index).equals("int"))) throw new ParseException("Semantic Error, " + index + " is not integer"); 
        String assignment = n.f5.accept(this, null);

        // Checking if the identifier on the assigned end has been declared in current or parent scopes or not
        if(in_func)
        {
            if(is_identifier(to_be_assigned))
            {
                if(funcVariable.get(to_be_assigned) != func_scope) 
                {
                    if(classObject.get(to_be_assigned) != scope) 
                    {
                        String parent = scope;
                        while(true)
                        {
                            parent = inheritance.get(parent);
                            if(parent == null) throw new ParseException("Semantic Error, " + to_be_assigned + " undeclared");
                            else if(classObject.get(to_be_assigned) != parent) continue;
                            else break;
                        }
                        
                        
                    }
                }
                
            }
        }

         // Checking if the identifier on the assignment end has been declared in current or parent scopes or not
        if(is_identifier(assignment)) 
        {
           if(!is_declared(assignment))
           {
               throw new ParseException("Semantic Error, undeclared");
           } 
        }
        // Checking if we have allocated an array to a non array variable
        else if(is_array_allocation(assignment))
        {
            String type = objectTypeforFunc.get(to_be_assigned);
            if(type == null) type = objectType.get(to_be_assigned);
            if(!(type.equals("int[]")) && !(type.equals("boolean[]"))) throw new ParseException("Semantic Error, wrong allocation");
        }

        return to_be_assigned + "[" + index + "]=" + assignment + ";"; 
    }

    /**
     * f0 -> "If"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     * f5 -> "else"
     * f6 -> "Statement()"
     */
    @Override
    public String visit(IfStatement n, Void argu) throws Exception{

        String condition = n.f2.accept(this, null);
        String if_statement = n.f4.accept(this, null);
        String else_statement = n.f6.accept(this, null);

        return "if(" + condition + ")" + if_statement + "else" + else_statement; 
    }

    /**
     * f0 -> "While"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     */
    @Override
    public String visit(WhileStatement n, Void argu)  throws Exception{
        String condition = n.f2.accept(this, null);
        String loop = n.f4.accept(this, null);

        return "while(" + condition + ")" + loop; 
    }

    /**
     * f0 -> "System.out.println"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> ";"
     */
    @Override
    public String visit(PrintStatement n, Void argu)  throws Exception{

        String printed = n.f2.accept(this, null);
        
        // checking if identifier is integer
        if(is_identifier(printed))
        {
            String type = get_expression_type(printed);
            if(!(type.equals("int"))) throw new ParseException("Semantic Error, " + printed + " is not integer");
        }
        
        return "System.out.println(" + printed + ");";
    }

    @Override
    public String visit(Expression n, Void argu) throws Exception{

        return n.f0.accept(this, null);
    }

    /**
     * f0 -> Clause()
     * f1 -> "&&"
     * f2 -> Clause()
     */
    public String visit(AndExpression n, Void argu) throws Exception{

        String and1 = n.f0.accept(this, null);
        String and2 = n.f2.accept(this, null);
        return and1 + "&&" + and2;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    public String visit(CompareExpression n, Void argu) throws Exception{

        String small = n.f0.accept(this, null);
        String big = n.f2.accept(this, null);
        return small + "<" + big;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    public String visit(PlusExpression n, Void argu) throws Exception{

        String plus1 = n.f0.accept(this, null);
        String plus2 = n.f2.accept(this, null);
        return plus1 + "+" + plus2;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    public String visit(MinusExpression n, Void argu) throws Exception{

        String minus1 = n.f0.accept(this, null);
        String minus2 = n.f2.accept(this, null);
        return minus1 + "-" + minus2;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    public String visit(TimesExpression n, Void argu) throws Exception{

        String times1 = n.f0.accept(this, null);
        String type1 = get_expression_type(times1);
        String times2 = n.f2.accept(this, null);
        String type2 = get_expression_type(times2);

        // Checking if all multiplication terms are integers
        if(!(type1.equals("int")) || !(type2.equals("int"))) throw new ParseException("Semantic Error, wrong multiplication");
        return times1 + "*" + times2;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    public String visit(ArrayLookup n, Void argu) throws Exception{

        String array = n.f0.accept(this, null);
        String array_type = objectTypeforFunc.get(array);
        if(array_type == null) array_type = objectType.get(array);
        // checking if the identifier is an array or not
        if(!(array_type.equals("int[]")) && !(array_type.equals("boolean[]"))) throw new ParseException("Semantic Error, wrong type " + array );
        String index = n.f2.accept(this, null);
        return array + "[" + index + "]";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    public String visit(ArrayLength n, Void argu) throws Exception{

        String array = n.f0.accept(this, null);
        return array + ".length";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( ExpressionList() )?
     * f5 -> ")"
     */
    public String visit(MessageSend n, Void argu) throws Exception{

        arg_types = new ArrayList<String>();

        String classname = n.f0.accept(this, null);
        String method = n.f2.accept(this, null);
        String arguments = n.f4.accept(this, null);

        return classname + "." + method + "(" + arguments + ")";
    }

    /**
     * f0 -> Expression()
     * f1 -> ExpressionTail()
     */
    public String visit(ExpressionList n, Void argu) throws Exception {

        String first_argument = n.f0.accept(this, null);

        String rest_of_them = n.f1.accept(this, null);

        return first_argument + rest_of_them;
    }

    /**
     * f0 -> ( ExpressionTerm() )*
     */
    public String visit(ExpressionTail n, Void argu) throws Exception {

        String ret = "";
        for ( Node node: n.f0.nodes) {
            ret += node.accept(this, null);
        }
        return ret;
    }

    /**
     * f0 -> ","
     * f1 -> Expression()
     */
    @Override
    public String visit(ExpressionTerm n, Void argu) throws Exception {
        
        String ret = "";
        if(n.f1 != null)
        {
            String argument = n.f1.accept(this, null); 

            ret += ", " + argument;
        } 

        return ret;
    }

    @Override
    public String visit(Clause n, Void argu) throws Exception{
        
        return n.f0.accept(this, null);
    }

    @Override
    public String visit(PrimaryExpression n, Void argu) throws Exception{

        String exp = n.f0.accept(this, null);
        

        return exp;
    }

    public String visit(IntegerLiteral n, Void argu) throws Exception{

        return n.f0.toString();
    }

    public String visit(TrueLiteral n, Void argu) throws Exception{

        return "true";
    }

    public String visit(FalseLiteral n, Void argu) throws Exception{

        return "false";
    }

    @Override
    public String visit(Identifier n, Void argu) throws Exception{
        
        String myName = n.f0.toString();

        return myName;
    }

    public String visit(ThisExpression n, Void argu) throws Exception{

        return "this";
    }

    public String visit(ArrayAllocationExpression n, Void argu) throws Exception{
        
        return n.f0.accept(this, null);
    }

    /**
     * f0 -> "new"
     * f1 -> "boolean"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    @Override
    public String visit(BooleanArrayAllocationExpression n, Void argu) throws Exception{

        String index = n.f3.accept(this, null);

        return "new boolean [" + index + "]";
    }

    /**
     * f0 -> "new"
     * f1 -> "int"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    @Override
    public String visit(IntegerArrayAllocationExpression n, Void argu) throws Exception{

        String index = n.f3.accept(this, null);

        return "new int [" + index + "]";
    }

    /**
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     */
    @Override
    public String visit(AllocationExpression n, Void argu) throws Exception{

        String classname = n.f1.accept(this, null);

        return "new " + classname + "()";
    }

    /**
     * f0 -> "!"
     * f1 -> Clause()
     */
    @Override
    public String visit(NotExpression n, Void argu) throws Exception{

        String oppposite = n.f1.accept(this, null);

        return "!" + oppposite;
    }

    /**
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     */
    public String visit(BracketExpression n, Void argu) throws Exception{

        String expression = n.f1.accept(this, null);

        return "(" + expression + ")";
    }

    // checking if given expression is identifier
    private boolean is_identifier(String s) throws Exception{

        List<String> operators = new ArrayList<>(Arrays.asList("&&", "<", "+", "-", "*", ".", "(", ")", "[", "]"));
        List<String> letters = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"));
        List<String> keywords = new ArrayList<>(Arrays.asList("true", "false", "this", "new"));

        // identifiers have letters, but not operators and are not keywords
        if(operators.stream().anyMatch(s::contains)) return false;
        if(!(letters.stream().anyMatch(s::contains))) return false;
        if(keywords.stream().anyMatch(s::equals)) return false;

        return true;
    }

    // checking if given expression has brackets
    private boolean is_bracket_expression(String s) throws Exception{


        if((s.indexOf("(") == 0) && (s.indexOf(")", s.length()-1) == s.length()-1)) return true;
        else return false;
    }

    // checking if given expression is "this" 
    private boolean is_this(String s) throws Exception{
        if(s.equals("this")) return true;
        else return false;
    }

    // checking if given expression has "this" 
    private boolean is_this_expression(String s) throws Exception{
        if(s.contains("this")) return true;
        else return false;
    }

    // checking if given expression is Class.field
    private boolean is_field(String s) throws Exception{
        if(s.contains(".")) return true;
        else return false;
    }

    // checking if given expression is function call
    private boolean is_function_call(String s) throws Exception{

        List<String> letters = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"));
        List<String> keywords = new ArrayList<>(Arrays.asList("true", "false", "this", "new"));

        // function calls have letters
        if(!(letters.stream().anyMatch(s::contains))) return false;
        // function calls have parenthesis
        if(s.contains("(") && s.contains(")"))
        {
            // function names have letters and are not keywords
            String func_name = s.substring(0, s.indexOf("("));
            if(!(letters.stream().anyMatch(func_name::contains))) return false;
            if((keywords.stream().anyMatch(func_name::equals))) return false;

            return true;
        }

        return false;

    }

    // checking if given expression is array lookup
    private boolean is_array(String s) throws Exception{

        // array lookups have []
        if(s.contains("[") && s.contains("]")) return true;
        else return false;
    }

    // checking if identifier is declared
    private boolean is_declared(String s) throws Exception{

        // if it is in function, it might be declared inside it
        if(in_func)
        {
            for(Map.Entry<String, String> pair: funcVariable.entrySet())
            {
                if((pair.getKey() == s) && (pair.getValue() == func_scope)) return true;
            }
        }

        // or it might be declared inside the class 
        for(Map.Entry<String, String> pair: classObject.entrySet())
        {
            if((pair.getKey() == s) && (pair.getValue() == scope)) return true;
        }

        return false;
    }


    // checking if the expression is new int[<index>] or new boolean[<index>]
    private boolean is_array_allocation(String s) throws Exception{

        if(s.contains("new int [") && s.contains("]")) return true;
        else if(s.contains("new boolean [") && s.contains("]")) return true;
        else return false;
    }

    // checking if the expression is an allocation
    private boolean is_allocation(String s) throws Exception{

        if(s.contains("new")) return true;
        else return false;
    }

    // checking if expression is an arithmetic operation
    private boolean is_arithmetic_expression(String s) throws Exception{

        List<String> arithmetic_operators = new ArrayList<String>(Arrays.asList("+", "-", "*"));

        if((arithmetic_operators.stream().anyMatch(s::contains))) return true;
        else return false;
    }

    // returns a string that describes the type of given expression recursively. 
    private String get_expression_type(String exp) throws Exception{

        // base case if expression is identifier
        if(is_identifier(exp))
        {
            // we already have saved the type for every identifier
            String type;
            if(in_func)
            {
                type = objectTypeforFunc.get(exp);
                if(type == null) type = objectType.get(exp);
            }
            else type = objectType.get(exp);

            return type;
        }
        // base case if expression is array lookup
        else if(is_array(exp))
        {
            // if array is int[] the lookup is int
            // if array is boolean[] the lookup is boolean
            String sub = exp.substring(0, exp.indexOf("["));
            if(get_expression_type(sub).equals("boolean[]")) return "boolean";
            else return "int";
        }
        // case if expression is in brackets
        else if(is_bracket_expression(exp))
        {
            // returns the type of expression inside the brackets
            String sub = exp.substring(1, exp.length() - 1);
            return get_expression_type(sub);
        }
        // base case if the expression is "this"
        else if(is_this(exp))
        {
            // returns the scope we are in
            return scope;
        }
        // case if the expression is this.<field>
        else if(is_this_expression(exp))
        {
            // returns the type of field 
            String sub = exp.substring(5, exp.length()-1);
            return get_expression_type(sub);
        }
        // case if the expression is an allocation
        else if(is_allocation(exp))
        {
            // returns the type of allocation
            String sub = exp.substring(3, exp.length()-1);
            return get_expression_type(sub);
        }
        // case if the expression is <Class>.<field>
        else if(is_field(exp))
        {
            // returns the type of field
            String sub = exp.substring(exp.indexOf('.'), exp.length()-1);
            return get_expression_type(sub);
        }
        // base case if the expression is function call
        else if(is_function_call(exp))
        {
            // returns the type of the function
            String sub = exp.substring(0, exp.indexOf("("));
            return funcType.get(sub);
        }
        // if the expression is arithmetic then it is integer
        else if(is_arithmetic_expression(exp))
        {
            return "int";
        }
        // base case if it is a constant
        else
        {
            List<String> letters = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"));
            // integers have no letters
            if(!(letters.stream().anyMatch(exp::contains))) 
            {
                return "int";
            } 
            // true or false are boolean   
            else if(exp.equals("true") || exp.equals("false"))
            {
                return "boolean";
            }

            return "";
        }

   }

   // checking if the function given returns the correct type given the thing it returns
   private boolean check_return_type(String func_name, String returned) throws Exception 
   {
        List<String> letters = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"));

        if(is_identifier(returned) == true)
        {
            // looking up the returned identifier in the symbol table
            String s1 = objectTypeforFunc.get(returned);
            if(s1 == null)
            s1 = objectType.get(returned);
            String s2 = funcType.get(func_name);
            if(s1 != s2)
            {
                return false;
            } 
        }
        // if it has no letters it is an integer
        else if(!(letters.stream().anyMatch(returned::contains)))
        {
            String func_type = funcType.get(func_name);
            if(func_type.equals("int")) return true;
            else return false;
        }

        return true;
   }
}