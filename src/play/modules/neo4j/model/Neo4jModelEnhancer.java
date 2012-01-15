package play.modules.neo4j.model;

import javassist.*;
import play.Logger;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.exceptions.UnexpectedException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;

/**
 * Enhance <code>Neo4jModel</code> to add getter & setter wich are delegate operation to the underlying node (@see
 * Neo4jModel.class).
 *
 * @author bsimard
 */
public class Neo4jModelEnhancer extends Neo4jEnhancer {

    @Override
    public void enhanceThisClass(ApplicationClass applicationClass) throws Exception {
        CtClass ctClass = makeClass(applicationClass);
        String entityName = ctClass.getName();

        // Only enhance Neo4jModel classes.
        if (!ctClass.subtypeOf(classPool.get("play.modules.neo4j.model.Neo4jModel"))) {
            return;
        }
        Logger.debug(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        Logger.debug("Enhance Neo4jModel class " + entityName);

        // Add a default constructor if needed
        addDefaultConstructor(ctClass);

        // for all field, we add getter / setter
        for (CtField ctField : ctClass.getDeclaredFields()) {

            try {
                String propertyName = ctField.getName().substring(0, 1).toUpperCase()
                        + ctField.getName().substring(1);
                if (isProperty(ctField)) {
                    Logger.debug("##### Field " + ctField.getName() + " is a property");
                    getterMethod(ctClass, entityName, ctField, "get" + propertyName);
                    setterMethod(ctClass, entityName, ctField, "set" + propertyName);
                } else if (isAnnotatedWithRelatedToVia(ctField)) {
                    getterMethodForRelatedToVia(ctClass, entityName, ctField, "get" + propertyName);
                } else {
                    Logger.debug("##### Field " + ctField.getName() + " skipped");
                }
            } catch (Exception e) {
                Logger.error(e, "Error while adding Getter/Setter in Neo4jModelEnhancer");
                throw new UnexpectedException("Error in PropertiesEnhancer", e);
            }
        }

        getByKeyMethod(ctClass, entityName);
        findAllMethod(ctClass, entityName);
        cleanUpMethod(ctClass, entityName);

        // Done.
        applicationClass.enhancedByteCode = ctClass.toBytecode();
        ctClass.defrost();
        Logger.debug("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");

    }

    private void addDefaultConstructor(CtClass ctClass) {
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
    }

<<<<<<< HEAD
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
=======
    private void setterMethod(CtClass ctClass, String entityName, CtField ctField, String setter) throws NotFoundException, CannotCompileException {
        try {
            CtMethod ctMethod = ctClass.getDeclaredMethod(setter);
            if (ctMethod.getParameterTypes().length != 1
                    || !ctMethod.getParameterTypes()[0].equals(ctField.getType())
                    || Modifier.isStatic(ctMethod.getModifiers())
                    || hasPlayPropertiesAccessorAnnotation(ctMethod)) {
                if (hasPlayPropertiesAccessorAnnotation(ctMethod)) {
                    ctClass.removeMethod(ctMethod);
>>>>>>> aa293e3863d449c8622672a2fc1730b89627ac5e
                }
                throw new NotFoundException("it's not a true setter !");
            }
        } catch (NotFoundException noSetter) {
            String code = "public void " + setter + "(" + ctField.getType().getName() + " value) { " +
                    "this." + ctField.getName() + " = value;" +
                    "this.shouldBeSave = Boolean.TRUE;" +
                    "}";
            addMethod(ctClass, entityName, code, setter);
        }
    }

    private void getterMethod(CtClass ctClass, String entityName, CtField ctField, String getter) throws NotFoundException, CannotCompileException {
        try {
            CtMethod ctMethod = ctClass.getDeclaredMethod(getter);
            if (!ctMethod.getName().equalsIgnoreCase("getShouldBeSave")) {
                ctClass.removeMethod(ctMethod);
                throw new NotFoundException("it's not a true getter !");
            }
        } catch (NotFoundException noGetter) {

            String code = "public " + ctField.getType().getName() + " " + getter + "() {" +
                    "if(this.shouldBeSave == Boolean.FALSE && this.node != null){" +
                    "return ((" + ctField.getType().getName() + ") this.node.getProperty(\"" + ctField.getName() + "\", null));" +
                    "}else{" +
                    "return " + ctField.getName() + ";" +
                    "}" +
                    "}";
            addMethod(ctClass, entityName, code, getter);
        }
    }


    private void getterMethodForRelatedToVia(CtClass ctClass, String entityName, CtField ctField, String getter) throws NotFoundException, CannotCompileException {
        try {
            CtMethod ctMethod = ctClass.getDeclaredMethod(getter);
            if (!ctMethod.getName().equalsIgnoreCase("getShouldBeSave")) {
                ctClass.removeMethod(ctMethod);
                throw new NotFoundException("it's not a true getter !");
            }
        } catch (NotFoundException noGetter) {

            String code = "public " + ctField.getType().getName() + " " + getter + "() {" +
                    "if(this." + ctField.getName() + " == null){" +
                    "return getIterator(\"" + entityName + "\");" +
                    "}else{" +
                    "return " + ctField.getName() + ";" +
                    "}" +
                    "}";
            addMethod(ctClass, entityName, code, getter);
        }
    }

    private void getByKeyMethod(CtClass ctClass, String entityName) throws CannotCompileException {
        String codeGetByKey = "public static play.modules.neo4j.model.Neo4jModel getByKey(Long key)  throws play.modules.neo4j.exception.Neo4jException  {" +
                "return (" + entityName + ")_getByKey(key, \"" + entityName + "\");" +
                "}";
        createMethod(ctClass, entityName, codeGetByKey, "getByKey");
    }

    private void findAllMethod(CtClass ctClass, String entityName) throws CannotCompileException {
        String code = "public static java.util.List findAll() {" +
                "return " + entityName + "._findAll(\"" + entityName + "\");" +
                "}";
        createMethod(ctClass, entityName, code, "findAll");
    }

    private void cleanUpMethod(CtClass ctClass, String entityName) throws CannotCompileException {
        String code = "public static void cleanUp() {" +
                "return " + entityName + "._cleanUp(\"" + entityName + "\");" +
                "}";
        createMethod(ctClass, entityName, code, "cleanUp");
    }

    /**
     * A javabean property must be public, not static and not final
     * Ignore : Transient, RelatedTo and RelatedToVia field if they are annotated
     */
    private boolean isProperty(CtField ctField) throws ClassNotFoundException {
        if (ctField.getName().equals(ctField.getName().toUpperCase())
                || ctField.getName().substring(0, 1).equals(ctField.getName().substring(0, 1).toUpperCase())) {
            return false;
        }
        for (Object info : ctField.getAvailableAnnotations()) {
            String annotationName = info.toString();
            if (annotationName.startsWith("@javax.persistence.Transient") ||
                    annotationName.startsWith("@play.modules.neo4j.annotation.RelatedTo") ||
                    annotationName.startsWith("@play.modules.neo4j.annotation.RelatedToVia")
                    ) {
                return false;
            }

        }
        return Modifier.isPublic(ctField.getModifiers()) && !Modifier.isFinal(ctField.getModifiers())
                && !Modifier.isStatic(ctField.getModifiers());
    }

    private boolean isAnnotatedWithRelatedToVia(CtField ctField) throws ClassNotFoundException {
        for (Object info : ctField.getAvailableAnnotations()) {
            String annotationName = info.toString();
            if (annotationName.startsWith("@play.modules.neo4j.annotation.RelatedToVia")) {
                return true;
            }

        }
        return false;
    }

    /**
     * Is this method get PlayPropertiesAccessor annotation ?
     */
    private boolean hasPlayPropertiesAccessorAnnotation(CtMethod ctMethod) {
        for (Object object : ctMethod.getAvailableAnnotations()) {
            Annotation ann = (Annotation) object;
            //Logger.debug("Annotation method is " + ann.annotationType().getName());
            if (ann.annotationType().getName()
                    .equals("play.classloading.enhancers.PropertiesEnhancer$PlayPropertyAccessor")) {
                //Logger.debug("Method " + ctMethod.getName() + " has be enhanced by propertiesEnhancer");
                return true;
            }
        }
        //Logger.debug("Method " + ctMethod.getName() + " has not be enhance by propertiesEnhancer");
        return false;
    }


}
