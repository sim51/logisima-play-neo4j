package play.modules.neo4j.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import play.Logger;
import play.classloading.enhancers.PropertiesEnhancer.PlayPropertyAccessor;
import play.modules.neo4j.exception.Neo4jPlayException;
import play.modules.neo4j.model.Neo4jModel;

public class Binder {

    final static int           notaccessibleMethod = Modifier.NATIVE | Modifier.STATIC;

    private Class              clazz;
    public Map<String, Method> properties          = new HashMap<String, Method>();

    /**
     * Binder constructor. Ths constructor take the class and populate all other attributes. Class must be an assignable
     * class of <code>Neo4jModel</code> (but there is no check nto this class, it must be done upper).
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

    /**
     * Binding method !
     * 
     * @param name
     * @param params
     * @return
     */
    public Object bind(String name, Map<String, String[]> params) {
        return this.bind(name, null, params);
    }

    /**
     * Binding method !
     * 
     * @param name
     * @param params
     * @return
     */
    public Object bind(String name, Object o, Map<String, String[]> params) {
        try {
            Neo4jModel model = (Neo4jModel) o;
            if (model == null) {
                if (params.containsKey(name + ".key")) {
                    Long key = Long.valueOf(params.get(name + ".key")[0]);
                    // searching getByKey Model method and invoke it
                    Method getByKey = clazz.getMethod("getByKey", Long.class);
                    model = (Neo4jModel) getByKey.invoke(null, key);
                    Logger.debug("Bind object " + name + " is already on database");
                }
                else {
                    // we search the object default constructor, and we simply call it
                    Constructor constructor = clazz.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    model = (Neo4jModel) constructor.newInstance();
                    Logger.debug("Bind object " + name + " is a new objects");
                }
            }
            // We iterate on all params and search the setter into the class
            for (String param : params.keySet()) {
                String paramName = param.replace(name + ".", "");
                if (this.properties.containsKey(paramName)) {
                    Method setter = this.properties.get(paramName);
                    if (setter != null) {
                        Logger.debug("Invoke setter " + paramName + "for bind object " + name);
                        setter.invoke(model, params.get(param));
                    }
                    else {
                        throw new Neo4jPlayException("Setter for " + paramName + " can't be found into Neo4jModel "
                                + clazz.getSimpleName());
                    }
                }
                else {
                    throw new Neo4jPlayException("Property " + paramName + " can't be found into Neo4jModel "
                            + clazz.getSimpleName());
                }
            }
            return model;
        } catch (Exception e) {
            throw new Neo4jPlayException(e.getMessage());
        }
    }

}
