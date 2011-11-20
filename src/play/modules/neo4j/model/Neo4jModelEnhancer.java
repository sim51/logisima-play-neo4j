package play.modules.neo4j.model;

import java.lang.reflect.Modifier;

import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.NotFoundException;
import play.Logger;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.classloading.enhancers.Enhancer;
import play.classloading.enhancers.PropertiesEnhancer.PlayPropertyAccessor;
import play.exceptions.UnexpectedException;

/**
 * Enhance <code>Neo4jModel</code> to add getter & setter wich are delegate operation to the underlying node (@see
 * Neo4jModel.class).
 * 
 * @author bsimard
 * 
 */
public class Neo4jModelEnhancer extends Enhancer {

    @Override
    public void enhanceThisClass(ApplicationClass applicationClass) throws Exception {
        CtClass ctClass = makeClass(applicationClass);

        // Only enhance Neo4jModel classes.
        if (!ctClass.subtypeOf(classPool.get("play.modules.neo4j.Neo4jModel"))) {
            return;
        }

        // Add a default constructor if needed
        try {
            boolean hasDefaultConstructor = false;
            for (CtConstructor constructor : ctClass.getDeclaredConstructors()) {
                if (constructor.getParameterTypes().length == 0) {
                    hasDefaultConstructor = true;
                    break;
                }
            }
            if (!hasDefaultConstructor && !ctClass.isInterface()) {
                CtConstructor defaultConstructor = CtNewConstructor.make("public " + ctClass.getSimpleName()
                        + "(org.neo4j.graphdb.Node underlyingNode ) { this.underlyingNode = underlyingNode;}", ctClass);
                ctClass.addConstructor(defaultConstructor);
            }
        } catch (Exception e) {
            Logger.error(e, "Error in PropertiesEnhancer");
            throw new UnexpectedException("Error in PropertiesEnhancer", e);
        }

        String entityName = ctClass.getName();

        // for all field, we add getter / setter
        for (CtField ctField : ctClass.getDeclaredFields()) {
            try {
                if (isProperty(ctField)) {
                    // Property name
                    String propertyName = ctField.getName().substring(0, 1).toUpperCase()
                            + ctField.getName().substring(1);
                    String getter = "get" + propertyName;
                    String setter = "set" + propertyName;

                    try {
                        CtMethod ctMethod = ctClass.getDeclaredMethod(getter);
                        if (ctMethod.getParameterTypes().length > 0 || Modifier.isStatic(ctMethod.getModifiers())) {
                            throw new NotFoundException("it's not a getter !");
                        }
                    } catch (NotFoundException noGetter) {
                        // Créé le getter
                        String code = "public " + ctField.getType().getName() + " " + getter + "() { return ("
                                + ctField.getType().getName() + ")this.underlyingNode.getProperty(\""
                                + ctField.getName() + "\");}";
                        CtMethod getMethod = CtMethod.make(code, ctClass);
                        ctClass.addMethod(getMethod);
                    }

                    try {
                        CtMethod ctMethod = ctClass.getDeclaredMethod(setter);
                        if (ctMethod.getParameterTypes().length != 1
                                || !ctMethod.getParameterTypes()[0].equals(ctField.getType())
                                || Modifier.isStatic(ctMethod.getModifiers())) {
                            throw new NotFoundException("it's not a setter !");
                        }
                    } catch (NotFoundException noSetter) {
                        // Créé le setter
                        CtMethod setMethod = CtMethod.make("public void " + setter + "(" + ctField.getType().getName()
                                + " value) { this.underlyingNode.setPrpoerty(" + ctField.getName() + ", value); }",
                                ctClass);
                        ctClass.addMethod(setMethod);
                        createAnnotation(getAnnotations(setMethod), PlayPropertyAccessor.class);
                    }
                }
            } catch (Exception e) {
                Logger.error(e, "Error in PropertiesEnhancer");
                throw new UnexpectedException("Error in PropertiesEnhancer", e);
            }
        }

    }

    /**
     * Is this field a valid javabean property ?
     */
    private boolean isProperty(CtField ctField) {
        if (ctField.getName().equals(ctField.getName().toUpperCase())
                || ctField.getName().substring(0, 1).equals(ctField.getName().substring(0, 1).toUpperCase())) {
            return false;
        }
        return Modifier.isPublic(ctField.getModifiers()) && !Modifier.isFinal(ctField.getModifiers())
                && !Modifier.isStatic(ctField.getModifiers());
    }

}
