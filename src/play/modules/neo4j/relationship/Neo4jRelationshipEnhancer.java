package play.modules.neo4j.relationship;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtField;
import play.Logger;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.modules.neo4j.exception.Neo4jPlayException;
import play.modules.neo4j.model.Neo4jEnhancer;

import java.lang.reflect.Modifier;

/**
 * Enhance <code>Neo4jRelationship</code> to add setter wich are delegate operation to the underlying node (@see
 * Neo4jRelationship.class).
 *
 * @author Karl COSSE
 */
public class Neo4jRelationshipEnhancer extends Neo4jEnhancer {

    @Override
    public void enhanceThisClass(ApplicationClass applicationClass) throws Exception {
        CtClass ctClass = makeClass(applicationClass);
        String entityName = ctClass.getName();

        // Only enhance Neo4jRelationship classes.
        if (!ctClass.subtypeOf(classPool.get("play.modules.neo4j.model.Neo4jRelationship"))) {
            return;
        }
        Logger.debug(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        Logger.debug("Enhance Relationship class " + entityName);

        boolean isStartNodeDefined = false;
        boolean isEndNodeDefined = false;

        for (CtField ctField : ctClass.getDeclaredFields()) {
            Object[] annotations = ctField.getAnnotations();
            for (Object info : annotations) {
                String propertyName = ctField.getName().substring(0, 1).toUpperCase()
                        + ctField.getName().substring(1);
                String methodName = "set" + propertyName;

                if (info.toString().contains("@play.modules.neo4j.annotation.StartNode") && Modifier.isPublic(ctField.getModifiers())) {
                    Logger.debug("##### Field " + ctField.getName() + " > StartNode");
                    String code = "public void " + methodName + "(" + ctField.getType().getName() + " value) { " +
                            "this." + ctField.getName() + " = value;" +
                            "this.setStartNode(value);" +
                            "}";
                    createMethod(ctClass, entityName, code, methodName);
                    isStartNodeDefined = true;
                } else if (info.toString().contains("@play.modules.neo4j.annotation.EndNode") && Modifier.isPublic(ctField.getModifiers())) {
                    Logger.debug("##### Field " + ctField.getName() + " > EndNode");
                    String code = "public void " + methodName + "(" + ctField.getType().getName() + " value) { " +
                            "this." + ctField.getName() + " = value;" +
                            "this.setEndNode(value);" +
                            "}";
                    createMethod(ctClass, entityName, code, methodName);
                    isEndNodeDefined = true;
                }
            }
        }

        if (!isStartNodeDefined) {
            throw new Neo4jPlayException("Please annotate in your class " + ctClass.getName() + " a public field with @StartNode");
        }
        if (!isEndNodeDefined) {
            throw new Neo4jPlayException("Please annotate in your class " + ctClass.getName() + " a public field with @EndNode");
        }

        getByKeyMethod(ctClass, entityName);

        // Done.
        applicationClass.enhancedByteCode = ctClass.toBytecode();
        ctClass.defrost();
        Logger.debug("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
    }

    private void getByKeyMethod(CtClass ctClass, String entityName) throws CannotCompileException {
        String code = "public static play.modules.neo4j.model.Neo4jRelationship getByKey(Long key) {" +
                "return (" + entityName + ") _getByKey(key, \"" + entityName + "\");" +
                "}";
        createMethod(ctClass, entityName, code, "getByKey");
    }

}
