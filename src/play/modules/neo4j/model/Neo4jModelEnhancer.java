package play.modules.neo4j.model;

import java.lang.annotation.Annotation;
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
        String entityName = ctClass.getName();
        Logger.debug("Enhance class " + entityName);

        // Only enhance Neo4jModel classes.
        if (!ctClass.subtypeOf(classPool.get("play.modules.neo4j.model.Neo4jModel"))) {
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
                        + "() { super();}", ctClass);
                ctClass.addConstructor(defaultConstructor);
            }
        } catch (Exception e) {
            Logger.error(e, "Error in PropertiesEnhancer");
            throw new UnexpectedException("Error in PropertiesEnhancer", e);
        }

        // for all field, we add getter / setter
        for (CtField ctField : ctClass.getDeclaredFields()) {
            try {
                Logger.debug("Field " + ctField.getName() + " is a property ?");
                if (isProperty(ctField)) {
                    Logger.debug("true");
                    // Property name
                    String propertyName = ctField.getName().substring(0, 1).toUpperCase()
                            + ctField.getName().substring(1);
                    String getter = "get" + propertyName;
                    String setter = "set" + propertyName;

                    try {
                        CtMethod ctMethod = ctClass.getDeclaredMethod(getter);
                        if (!ctMethod.getName().equalsIgnoreCase("getShouldBeSave")) {
                            ctClass.removeMethod(ctMethod);
                            throw new NotFoundException("it's not a true getter !");
                        }
                    } catch (NotFoundException noGetter) {

                        // create getter
                        Logger.debug("Adding getter  " + getter + " for class " + entityName);
                        //@formatter:off
                        String code = "public " + ctField.getType().getName() + " " + getter + "() {" +
                                            "if(this.shouldBeSave == Boolean.FALSE && this.node != null){" +
                                                "return ((" + ctField.getType().getName() + ") this.node.getProperty(\""+ ctField.getName() + "\", null));" +
                                            "}else{" +
                                                "return " + ctField.getName() + ";" +
                                            "}" +
                                        "}";
                        //@formatter:on
                        Logger.debug(code);
                        CtMethod getMethod = CtMethod.make(code, ctClass);
                        ctClass.addMethod(getMethod);
                    }

                    try {
                        CtMethod ctMethod = ctClass.getDeclaredMethod(setter);
                        if (ctMethod.getParameterTypes().length != 1
                                || !ctMethod.getParameterTypes()[0].equals(ctField.getType())
                                || Modifier.isStatic(ctMethod.getModifiers())
                                || hasPlayPropertiesAccessorAnnotation(ctMethod)) {
                            if (hasPlayPropertiesAccessorAnnotation(ctMethod)) {
                                ctClass.removeMethod(ctMethod);
                            }
                            throw new NotFoundException("it's not a true setter !");
                        }
                    } catch (NotFoundException noSetter) {
                        // create setter
                        Logger.debug("Adding setter  " + getter + " for class " + entityName);
                        //@formatter:off
                        CtMethod setMethod = CtMethod
                                .make("public void " + setter + "(" + ctField.getType().getName() + " value) { " +
                                            "this."  + ctField.getName() + " = value;" +
                                            "this.shouldBeSave = Boolean.TRUE;" +
                                      "}", ctClass);
                        //formatter:on
                        ctClass.addMethod(setMethod);
                    }
                }
            } catch (Exception e) {
                Logger.error(e, "Error in PropertiesEnhancer");
                throw new UnexpectedException("Error in PropertiesEnhancer", e);
            }
        }
        
        
        // Adding getByKey() method
        Logger.debug("Adding getByKey() method for class " + entityName);
        //@formatter:off
        String codeGetByKey =  "public static play.modules.neo4j.model.Neo4jModel getByKey(Long key)  throws play.modules.neo4j.exception.Neo4jException  {" +
                                    "return (" + entityName + ")_getByKey(key, \"" + entityName + "\");" +
                                "}";
        //@formatter:on
        Logger.debug(codeGetByKey);
        CtMethod getByKeyMethod = CtMethod.make(codeGetByKey, ctClass);
        ctClass.addMethod(getByKeyMethod);

        // Done.
        applicationClass.enhancedByteCode = ctClass.toBytecode();
        ctClass.defrost();

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

    /**
     * Is this method get PlayPropertiesAccessor annotation ?
     */
    private boolean hasPlayPropertiesAccessorAnnotation(CtMethod ctMethod) {
        for (Object object : ctMethod.getAvailableAnnotations()) {
            Annotation ann = (Annotation) object;
            Logger.debug("Annotation method is " + ann.annotationType().getName());
            if (ann.annotationType().getName()
                    .equals("play.classloading.enhancers.PropertiesEnhancer$PlayPropertyAccessor")) {
                Logger.debug("Method " + ctMethod.getName() + " has be enhanced by propertiesEnhancer");
                return true;
            }
        }
        Logger.debug("Method " + ctMethod.getName() + " has not be enhance by propertiesEnhancer");
        return false;
    }

}
