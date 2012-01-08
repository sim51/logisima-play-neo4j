package play.modules.neo4j.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.scanner.ScannerException;

import play.Play;
import play.data.binding.types.DateBinder;
import play.exceptions.YAMLException;
import play.modules.neo4j.exception.Neo4jException;
import play.modules.neo4j.model.Neo4jModel;
import play.templates.TemplateLoader;
import play.vfs.VirtualFile;

public class Fixtures {

    static Pattern             keyPattern = Pattern.compile("([^(]+)\\(([^)]+)\\)");
    static Map<String, Object> idCache    = new HashMap<String, Object>();

    public static void loadYml(String name) {
        VirtualFile yamlFile = null;
        try {
            for (VirtualFile vf : Play.javaPath) {
                yamlFile = vf.child(name);
                if (yamlFile != null && yamlFile.exists()) {
                    break;
                }
            }
            if (yamlFile == null) {
                throw new RuntimeException("Cannot load fixture " + name + ", the file was not found");
            }

            String renderedYaml = TemplateLoader.load(yamlFile).render();

            Yaml yaml = new Yaml();
            Object o = yaml.load(renderedYaml);
            if (o instanceof LinkedHashMap<?, ?>) {
                @SuppressWarnings("unchecked")
                LinkedHashMap<Object, Map<?, ?>> objects = (LinkedHashMap<Object, Map<?, ?>>) o;

                // for all object that are in YML file
                for (Object key : objects.keySet()) {
                    // we retrieve the definition line with the ID and type of the object
                    Matcher matcher = keyPattern.matcher(key.toString().trim());
                    if (matcher.matches()) {

                        String type = matcher.group(1);
                        String id = matcher.group(2);

                        // All type that are not 'Relation' and don't start with 'models', are in fact 'Model', so we
                        // adding the 'models' package
                        if (!type.equals("Relation") && !type.startsWith("models.")) {
                            type = "models." + type;
                        }

                        // we look at "cache" if the object as already be processed, if so we throw an exception because
                        // it can't have to object with the same id in yml file.
                        if (idCache.containsKey(type + "-" + id)) {
                            throw new RuntimeException("Cannot load fixture " + name + ", duplicate id '" + id
                                    + "' for type " + type);
                        }

                        // Serialize YML attribute into an Hasmap that correspond to http params to use the same bind
                        // function
                        Map<String, String[]> params = new HashMap<String, String[]>();
                        if (objects.get(key) == null) {
                            objects.put(key, new HashMap<Object, Object>());
                        }
                        serialize(objects.get(key), "object", params);

                        // Bind & save he model
                        @SuppressWarnings("unchecked")
                        Class<Neo4jModel> cType = (Class<Neo4jModel>) Play.classloader.loadClass(type);
                        Neo4jModel model = (Neo4jModel) bind("object", cType, params);
                        model.save();

                        // we put in cache the object by its id because it is processed and we could need it for
                        // relation !
                        idCache.put(type + "-" + id, model);
                    }
                }
            }
        } catch (ScannerException e) {
            throw new YAMLException(e, yamlFile);
        } catch (Throwable e) {
            throw new RuntimeException("Cannot load fixture " + name + ": " + e.getMessage(), e);
        }

    }

    /**
     * Copy of playframework metho, @see <code>Fixtures.serialized</code>. It serialized into the
     * <code>serialized</code> params all Yml attribute.
     * 
     * @param values
     * @param prefix
     * @param serialized
     */
    static void serialize(Map<?, ?> values, String prefix, Map<String, String[]> serialized) {
        for (Object key : values.keySet()) {
            Object value = values.get(key);
            if (value == null) {
                continue;
            }
            if (value instanceof Map<?, ?>) {
                serialize((Map<?, ?>) value, prefix + "." + key, serialized);
            }
            else if (value instanceof Date) {
                serialized.put(prefix + "." + key.toString(),
                        new String[] { new SimpleDateFormat(DateBinder.ISO8601).format(((Date) value)) });
            }
            else if (value instanceof List<?>) {
                List<?> l = (List<?>) value;
                String[] r = new String[l.size()];
                int i = 0;
                for (Object el : l) {
                    r[i++] = el.toString();
                }
                serialized.put(prefix + "." + key.toString(), r);
            }
            else if (value instanceof String && value.toString().matches("<<<\\s*\\{[^}]+}\\s*")) {
                Matcher m = Pattern.compile("<<<\\s*\\{([^}]+)}\\s*").matcher(value.toString());
                m.find();
                String file = m.group(1);
                VirtualFile f = Play.getVirtualFile(file);
                if (f != null && f.exists()) {
                    serialized.put(prefix + "." + key.toString(), new String[] { f.contentAsString() });
                }
            }
            else {
                serialized.put(prefix + "." + key.toString(), new String[] { value.toString() });
            }
        }
    }

    /**
     * Method to bind a Neo4jModel from the params attributes.
     * 
     * @param clazz
     * @param params
     * @return
     * @throws Exception
     */
    private static Object bind(String name, Class clazz, Map<String, String[]> params) throws Exception {
        Binder binder = new Binder(clazz);

        // we search the object default constructor, and we simply call it
        Constructor constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        Neo4jModel model = (Neo4jModel) constructor.newInstance();

        // We iterate on all params and search the setter into the class
        for (String param : params.keySet()) {
            String paramName = param.replace(name + ".", "");
            if (binder.properties.containsKey(paramName)) {
                Method setter = binder.properties.get(paramName);
                if (setter != null) {
                    setter.invoke(model, params.get(param));
                }
                else {
                    throw new Neo4jException("Setter for " + paramName + " can't be found into Neo4jModel "
                            + clazz.getSimpleName());
                }
            }
            else {
                throw new Neo4jException("Property " + paramName + " can't be found into Neo4jModel "
                        + clazz.getSimpleName());
            }
        }
        return model;
    }
}
