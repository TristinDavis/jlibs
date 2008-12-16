package jlibs.core.graph.visitors;

import jlibs.core.graph.Sequence;
import jlibs.core.graph.Visitor;
import jlibs.core.graph.sequences.CollectionSequence;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Santhosh Kumar T
 */
public abstract class ReflectionVisitor<E, R> implements Visitor<E, R>{
    private Map<Class<?>, Method> methodMap = new HashMap<Class<?>, Method>();
    private Sequence<Class<?>> seq;

    private void sort(){
        for(Method method: getClass().getDeclaredMethods()){
            if(method.getName().equals("process")){
                Class<?>[] params = method.getParameterTypes();
                if(params.length==1){
                    methodMap.put(params[0], method);
                    method.setAccessible(true);
                }
            }
        }

        seq = new CollectionSequence<Class<?>>(ClassSorter.sort(methodMap.keySet()));
    }

    public void generateCode(){
        if(seq==null)
            sort();
        else
            seq.reset();
        
        for(Class<?> clazz; (clazz=seq.next())!=null;){
            System.out.print("        ");
            if(seq.index()!=0)
                System.out.print("else ");
            System.out.format("if(elem instanceof %s)%n", clazz.getSimpleName());
            System.out.format("            return process((%s)elem);%n", clazz.getSimpleName());
        }
        System.out.println("        else");
        System.out.println("           return getDefault(elem);");
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public R visit(E elem){
        if(seq==null)
            sort();
        else
            seq.reset();

        for(Class<?> clazz; (clazz=seq.next())!=null;){
            if(clazz.isAssignableFrom(elem.getClass())){
                try{
                    return (R)methodMap.get(clazz).invoke(this, elem);
                }catch(Exception ex){
                    throw new RuntimeException(ex);
                }
            }
        }
        return getDefault(elem);
    }

    protected abstract R getDefault(Object elem);
}