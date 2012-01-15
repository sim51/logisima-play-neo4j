package play.modules.neo4j.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Transaction;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.scanner.ScannerException;

import play.Play;
import play.data.binding.types.DateBinder;
import play.exceptions.YAMLException;
import play.modules.neo4j.exception.Neo4jException;
import play.modules.neo4j.exception.Neo4jPlayException;
import play.modules.neo4j.model.Neo4jModel;
import play.templates.TemplateLoader;
import play.vfs.VirtualFile;

/**
 * Util class to help unit test for loading data and export it.
 * 
 * @author bsimard
 * 
 */
public class Fixtures {

    private final static Pattern       keyPattern              = Pattern.compile("([^(]+)\\(([^)]+)\\)");
    private static final String        RELATION_FROM_KEY       = "from";
    private static final String        RELATION_TO_KEY         = "to";
    private static final String        RELATION_TYPE_KEY       = "type";
    private static final String        RELATION_TYPE_CLASS_KEY = "class";
    private static final String        RELATION_TYPE_VALUE_KEY = "value";
    private static Map<String, Object> idCache                 = new HashMap<String, Object>();

    public static void deleteDatabase() {
        Neo4j.clear();
    }

    /**
     * Method to load an YML file into neo4j database.
     * 
     * @param name file name into play java path (like conf directory).
     */
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

                        if (type.equals("Relation")) {
                            // Get params from YML object
                            Map<String, String> params = serializeRelation(objects.get(key));
                            if (idCache.containsKey(params.get(RELATION_FROM_KEY))
                                    && idCache.containsKey(params.get(RELATION_TO_KEY))) {
                                Neo4jModel modelFrom = (Neo4jModel) idCache.get(params.get(RELATION_FROM_KEY));
                                Neo4jModel modelTo = (Neo4jModel) idCache.get(params.get(RELATION_TO_KEY));
                                Transaction tx = Neo4j.db().beginTx();
                                try {
                                    modelFrom.getNode().createRelationshipTo(modelTo.getNode(),
                                            DynamicRelationshipType.withName(params.get(RELATION_TYPE_VALUE_KEY)));
                                    tx.success();
                                } finally {
                                    tx.finish();
                                }

                            }
                            else {
                                throw new Neo4jException("Relation dependency not valid : unabled to find "
                                        + params.get(RELATION_FROM_KEY) + " and " + params.get(RELATION_TO_KEY)
                                        + " from already processing object !");
                            }
                        }
                        else {
                            // All type that are not 'Relation' and don't start with 'models', are in fact 'Model', so
                            // we
                            // adding the 'models' package
                            if (!type.startsWith("models.")) {
                                type = "models." + type;
                            }

                            // we look at "cache" if the object as already be processed, if so we throw an exception
                            // because
                            // it can't have to object with the same id in yml file.
                            if (idCache.containsKey(id)) {
                                throw new RuntimeException("Cannot load fixture " + name + ", duplicate id '" + id
                                        + "' for type " + type);
                            }

                            // Serialize YML attribute into an Hasmap that correspond to http params to use the same
                            // bind
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
                            idCache.put(id, model);
                        }
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
     * Copy of playframework method, @see <code>Fixtures.serialized</code>. It serialized into the
     * <code>serialized</code> params all Yml attributes.
     * 
     * @param values
     * @param prefix
     * @param serialized
     */
    private static void serialize(Map<?, ?> values, String prefix, Map<String, String[]> serialized) {
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
     * Method to parse Map object that represent an YML Relation object. We return a well formed Map with goods keys.
     * 
     * @param values
     * @return
     */
    private static Map<String, String> serializeRelation(Map<?, ?> values) {
        Map<String, String> params = new HashMap<String, String>();
        for (Object key : values.keySet()) {
            String value = (String) values.get(key);
            if (key.equals(RELATION_FROM_KEY)) {
                params.put(RELATION_FROM_KEY, value);
            }
            else if (key.equals(RELATION_TO_KEY)) {
                params.put(RELATION_TO_KEY, value);
            }
            else if (key.equals(RELATION_TYPE_KEY)) {
                String[] tab = value.split("\\.");
                String enumValue = tab[tab.length - 1];
                String clazz = value.replace("." + enumValue, "");
                // default package for relatonship enumeration is model.reltionship, so if package is not present, we
                // add the package
                if (tab.length == 2 && Character.isUpperCase(value.subSequence(0, 1).charAt(0))) {
                    clazz = "models.relationship." + clazz;
                }
                params.put(RELATION_TYPE_CLASS_KEY, clazz);
                params.put(RELATION_TYPE_VALUE_KEY, enumValue);
            }
            else {
                throw new Neo4jPlayException("Unkhnow attribute " + key + " [" + value + "] for Relation");
            }
        }
        if (params.size() != 4) {
            throw new Neo4jPlayException(
                    "Number of attributed for relation is not good ! Relation has to get to,from and type atributes, and only those");
        }
        return params;
    }

    /**
     * Method to bind a Neo4jModel from params attributes.
     * 
     * @param clazz
     * @param params
     * @return
     */
    private static Object bind(String name, Class clazz, Map<String, String[]> params) {
        Binder binder = new Binder(clazz);
        return binder.bind(name, params);
    }
}
