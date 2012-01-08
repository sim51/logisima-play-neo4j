package play.modules.neo4j.util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import play.classloading.enhancers.PropertiesEnhancer.PlayPropertyAccessor;

public class Binder {

    final static int           notaccessibleMethod = Modifier.NATIVE | Modifier.STATIC;

    private Class              clazz;
    public Map<String, Method> properties          = new HashMap<String, Method>();

    /**
     * Binder constructor. Ths constructor take the class and populate all other attributes.
     * 
     * @param clazz
     */
    public Binder(Class clazz) {
        super();
        this.clazz = clazz;
        // setting class attributs
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            String propertyname;
            if (!isSetter(method)) {
                continue;
            }
            propertyname = method.getName().substring(3, 4).toLowerCase() + method.getName().substring(4);
            properties.put(propertyname, method);
        }
    }

    /**
     * Duplicate method from <code>BeanWrapper.isSetter</code>.
     * 
     * @param method
     * @return
     */
    private boolean isSetter(Method method) {
        return (!method.isAnnotationPresent(PlayPropertyAccessor.class) && method.getName().startsWith("set")
                && method.getName().length() > 3 && method.getParameterTypes().length == 1 && (method.getModifiers() & notaccessibleMethod) == 0);
    }

}
