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
package play.modules.neo4j.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;

import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.NotFoundException;

import org.neo4j.graphdb.Node;

import play.Logger;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.classloading.enhancers.Enhancer;
import play.exceptions.UnexpectedException;
import play.modules.neo4j.annotation.Neo4jRelatedTo;
import play.modules.neo4j.annotation.Neo4jUniqueRelation;

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
            for (CtConstructor constructor : ctClass.getDeclaredConstructors()) {
                if (constructor.getParameterTypes().length == 0) {
                    ctClass.removeConstructor(constructor);
                }
                if (constructor.getParameterTypes().length == 1
                        && constructor.getParameterTypes()[0].getClass().isInstance(Node.class)) {
                    ctClass.removeConstructor(constructor);
                }
            }
            if (!ctClass.isInterface()) {
                Logger.debug("Adding default constructor");
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
                // Property name
                String propertyName = ctField.getName().substring(0, 1).toUpperCase() + ctField.getName().substring(1);
                String getter = "get" + propertyName;
                String setter = "set" + propertyName;

                Logger.debug("Field " + ctField.getName() + " is a property ?");
                if (isProperty(ctField)) {
                    Logger.debug("true");

                    // ~~~~~~~~~
                    // GETTER
                    // ~~~~~~~
                    try {
                        CtMethod ctMethod = ctClass.getDeclaredMethod(getter);
                        if (!ctMethod.getName().equalsIgnoreCase("getShouldBeSave")) {
                            ctClass.removeMethod(ctMethod);
                            throw new NotFoundException("it's not a true getter !");
                        }
                    } catch (NotFoundException noGetter) {
                        // create getter
                        Logger.debug("Adding getter " + getter + " for class " + entityName);
                        //@formatter:off
                        String code = "public " + ctField.getType().getName() + " " + getter + "() {" +
                                            "if(this.shouldBeSave == Boolean.FALSE && this.node != null){" +
                                                "return ((" + ctField.getType().getName() + ") play.modules.neo4j.util.Binder.bindFromNeo4jFormat(this.node.getProperty(\""+ ctField.getName() + "\", null)," + ctField.getType().getName() +".class ));" +
                                            "}else{" +
                                                "return " + ctField.getName() + ";" +
                                            "}" +
                                        "}";
                        //@formatter:on
                        Logger.debug(code);
                        CtMethod getMethod = CtMethod.make(code, ctClass);
                        ctClass.addMethod(getMethod);
                    }

                    // ~~~~~~~~~
                    // SETTER
                    // ~~~~~~~
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
                        Logger.debug("Adding setter " + getter + " for class " + entityName);
                        //@formatter:off
                        String code = "public void " + setter + "(" + ctField.getType().getName() + " value) { " +
                                            "this." + ctField.getName() + " = value;" +
                                            "this.shouldBeSave = Boolean.TRUE;" +
                                      "}";
                          //formatter:on
                        CtMethod setMethod = CtMethod
                                .make(code, ctClass);
                        Logger.debug(code);
                        ctClass.addMethod(setMethod);
                    }
                }
                else{
                    // ~~~~~~~~~
                    // GETTER for neo4j relation property 
                    // ~~~~~~~
                    if(hasNeo4jRelationAnnotation(ctField)){
                        // test for related annotation
                        Neo4jRelatedTo relatedTo = getRelatedAnnotation(ctField);
                        if(relatedTo != null){
                            CtMethod ctMethod = ctClass.getDeclaredMethod(getter);
                            ctClass.removeMethod(ctMethod);
                            String code;
                            if(relatedTo.lazy()){
                            //@formatter:off
                            code = "public " + ctField.getType().getName() + " " + getter + "() {" +
                                                "if(this." + ctField.getName() + " == null){" +
                                                    "java.lang.reflect.Field field = this.getClass().getField(\"" +ctField.getName() + "\");" +
                                                    "this." + ctField.getName() + "=play.modules.neo4j.relationship.Neo4jRelationFactory.getModelsFromRelation(\"" + relatedTo.value() + "\", \"" + relatedTo.direction() + "\", field, this.node);" +
                                                "}" +
                                                 "return " + ctField.getName() + ";" +
                                          "}";
                            //@formatter:on
                            }
                            else {
                                //@formatter:off
                                code = "public " + ctField.getType().getName() + " " + getter + "() {" +
                                            "return " + ctField.getName() + ";" +
                                       "}";
                                //@formatter:on
                            }
                            Logger.debug(code);
                            CtMethod method = CtMethod.make(code, ctClass);
                            ctClass.addMethod(method);
                        }
                        // test for unique relation annotation
                        Neo4jUniqueRelation uniqueRelation = getUniqueRelationAnnotation(ctField);
                        if (uniqueRelation != null) {
                            CtMethod ctMethod = ctClass.getDeclaredMethod(getter);
                            ctClass.removeMethod(ctMethod);
                            String code;
                            //@formatter:off
                            code = "public " + ctField.getType().getName() + " " + getter + "() {" +
                                    "return (" + ctField.getType().getName() + ")" + ctField.getName() + ";" +
                                   "}";
                            //@formatter:on
                            Logger.debug(code);
                            CtMethod method = CtMethod.make(code, ctClass);
                            ctClass.addMethod(method);
                        }
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
        String codeGetByKey = "public static play.modules.neo4j.model.Neo4jModel getByKey(Long key) throws play.modules.neo4j.exception.Neo4jException {" +
                                    "return (" + entityName + ")_getByKey(key, \"" + entityName + "\");" +
                                "}";
        //@formatter:on
        Logger.debug(codeGetByKey);
        CtMethod getByKeyMethod = CtMethod.make(codeGetByKey, ctClass);
        ctClass.addMethod(getByKeyMethod);

        // ~~~~~~~~~~~~~~~
        // Adding findAll() method
        //@formatter:off
        String codeFindAll = "public static java.util.List findAll() {" +
                                "return " + entityName + "._findAll(\"" + entityName + "\");" +
                             "}";
        //@formatter:on
        Logger.debug(codeFindAll);
        CtMethod findAllMethod = CtMethod.make(codeFindAll, ctClass);
        ctClass.addMethod(findAllMethod);

        // ~~~~~~~~~~~~~~~
        // Adding queryIndex() method
        //@formatter:off
        String queryIndex = "public static java.util.List queryIndex(String indexname, String query) {" +
                                "return " + entityName + "._queryIndex(indexname, query);" +
                             "}";
        //@formatter:on
        Logger.debug(queryIndex);
        CtMethod queryIndexMethod = CtMethod.make(queryIndex, ctClass);
        ctClass.addMethod(queryIndexMethod);

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
        if (hasNeo4jRelationAnnotation(ctField)) {
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

    /**
     * Is filed has neo4j related annotation ?
     * 
     * @param ctField
     * @return
     */
    private boolean hasNeo4jRelationAnnotation(CtField ctField) {
        for (Object info : ctField.getAvailableAnnotations()) {
            String annotationName = info.toString();
            if (annotationName.startsWith("@play.modules.neo4j.annotation.Neo4jRelatedTo")
                    || annotationName.startsWith("@play.modules.neo4j.annotation.Neo4jRelatedToVia")
                    || annotationName.startsWith("@play.modules.neo4j.annotation.Neo4jUniqueRelation")) {
                return true;
            }

        }
        return false;
    }

    /**
     * Return the neo4jRelatedTo annotation of a field if exist, null otherwise.
     * 
     * @param ctField
     * @return
     */
    private Neo4jRelatedTo getRelatedAnnotation(CtField ctField) {
        for (Object annotation : ctField.getAvailableAnnotations()) {
            if (annotation.toString().startsWith("@play.modules.neo4j.annotation.Neo4jRelatedTo(")) {
                return (Neo4jRelatedTo) annotation;
            }
        }
        return null;
    }

    /**
     * Return the neo4jUniqueRelation annotation of a field if exist, null otherwise.
     * 
     * @param ctField
     * @return
     */
    private Neo4jUniqueRelation getUniqueRelationAnnotation(CtField ctField) {
        for (Object annotation : ctField.getAvailableAnnotations()) {
            if (annotation.toString().startsWith("@play.modules.neo4j.annotation.Neo4jUniqueRelation(")) {
                return (Neo4jUniqueRelation) annotation;
            }
        }
        return null;
    }

}
