/**
 * This file is part of logisima-play-neo4j.
 *
 * logisima-play-neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * logisima-play-neo4j is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with logisima-play-neo4j. If not, see <http://www.gnu.org/licenses/>.
 * 
 * @See https://github.com/sim51/logisima-play-neo4j
 */
package play.modules.neo4j.util;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import play.Logger;
import play.classloading.enhancers.PropertiesEnhancer.PlayPropertyAccessor;
import play.db.jpa.Blob;
import play.modules.neo4j.exception.Neo4jPlayException;
import play.modules.neo4j.model.Neo4jModel;

/**
 * Binder class for play neo4j module. Transform a map of properties into an Neo4j Model object.
 * 
 * @author bsimard
 * 
 */
public class Binder {

    final static int           notaccessibleMethod = Modifier.NATIVE | Modifier.STATIC;

    /**
     * The class of the object that we want to bind. It must be an assignable class of <code>Neo4jModel</code>.
     */
    private Class              clazz;

    /**
     * Map that represent all class ttributes.
     */
    public Map<String, Method> properties          = new HashMap<String, Method>();

    /**
     * Binder constructor. Ths constructor take the class and populate all other attributes. Class must be an assignable
     * class of <code>Neo4jModel</code> (but there is no check into this class, it must be done upper).
     * 
     * @param clazz
     */
    public Binder(Class clazz) {
        // setting clazz
        this.clazz = clazz;

        // setting class attributs
        Method[] methods = clazz.getMethods();
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
    @SuppressWarnings("unchecked")
    public Object bind(String name, Object o, Map<String, String[]> params) {
        try {
            Neo4jModel model = (Neo4jModel) o;
            if (model == null) {
                if (params.containsKey(name + ".key") && !params.get(name + ".key")[0].isEmpty()) {
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
                // if the params is a model property
                if (param.startsWith(name + ".") && !param.equals(name + ".key")) {
                    String paramName = param.replace(name + ".", "");
                    if (this.properties.containsKey(paramName)) {
                        Method setter = this.properties.get(paramName);
                        if (setter != null) {
                            Logger.debug("Invoke setter " + paramName + "for bind object " + name);
                            Object value = play.data.binding.Binder.directBind(params.get(param)[0],
                                    setter.getParameterTypes()[0]);
                            setter.invoke(model, value);
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
            }
            return model;
        } catch (Exception e) {
            throw new Neo4jPlayException(e.getMessage());
        }
    }

    /**
     * For some play! type, we have to cast them into neo4j format, because neo4j database only support a few kind of
     * type . @see http://docs.neo4j.org/chunked/snapshot/graphdb-neo4j-properties.html.
     * 
     * @param value
     * @param type
     * @return
     */
    public static Object bindToNeo4jFormat(Object value, Class type) {
        try {
            if (type.isAssignableFrom(Date.class)) {
                Date date = (Date) value;
                String neo4jValue = "@@Date@@" + date.getTime();
                return neo4jValue;
            }
            else if (type.isAssignableFrom(Blob.class)) {
                Blob blob = (Blob) value;
                String format = blob.type();
                String uuid = blob.getFile().getName();
                return "@@File@@" + uuid + "|" + format;
            }
            else {
                return value;
            }
        } catch (Exception e) {
            throw new Neo4jPlayException(e);
        }
    }

    /**
     * Bijection method of <code>bindToNeo4jFormat</code>.
     * 
     * @param value
     * @param type
     * @return
     */
    public static Object bindFromNeo4jFormat(Object value, Class type) {
        try {
            if (value != null) {
                if (type.isAssignableFrom(String.class)) {
                    String temp = (String) value;
                    if (temp.startsWith("@@Date@@")) {
                        String dateString = temp.replaceFirst("@@Date@@", "");
                        if (dateString != null) {
                            Long dateLong = Long.valueOf(dateString);
                            if (dateLong != null) {
                                Date date = new Date(dateLong);
                                Logger.debug("Object from neo4j is " + date.toString());
                                return date;
                            }
                        }
                    }
                    else if (temp.startsWith("@@File@@")) {
                        String fileString = temp.replaceFirst("@@File@@", "");
                        String UUID = fileString.split("|")[0];
                        String format = fileString.split("|")[1];
                        if (UUID != null && !UUID.isEmpty() && format != null && !format.isEmpty()) {
                            Blob blob = new Blob();
                            File file = new File(Blob.getStore(), UUID);
                            InputStream is = Binder.class.getResourceAsStream(file.getAbsoluteFile().toString());
                            blob.set(is, format);
                            Logger.debug("Object from neo4j is " + blob.toString());
                            return blob;
                        }
                    }
                    else {
                        Logger.debug("Object from neo4j is " + value.toString());
                        return type.cast(value);
                    }
                }
            }
            Logger.debug("Object from neo4j is " + value.toString());
            return type.cast(value);
        } catch (Exception e) {
            throw new Neo4jPlayException(e);
        }
    }

}
