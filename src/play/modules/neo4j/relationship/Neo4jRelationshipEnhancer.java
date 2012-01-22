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
package play.modules.neo4j.relationship;

import java.lang.reflect.Modifier;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;
import play.Logger;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.classloading.enhancers.Enhancer;
import play.modules.neo4j.exception.Neo4jPlayException;

/**
 * Enhance <code>Neo4jRelationship</code> to add setter wich are delegate operation to the underlying node (@see
 * Neo4jRelationship.class).
 * 
 * @author Karl COSSE
 */
public class Neo4jRelationshipEnhancer extends Enhancer {

    @Override
    public void enhanceThisClass(ApplicationClass applicationClass) throws Exception {
        CtClass ctClass = makeClass(applicationClass);
        String entityName = ctClass.getName();

        // Only enhance Neo4jRelationship classes.
        if (!ctClass.subtypeOf(classPool.get("play.modules.neo4j.relationship.Neo4jRelationship"))) {
            return;
        }

        Logger.debug("Enhance Relationship class " + entityName);

        boolean isStartNodeDefined = false;
        boolean isEndNodeDefined = false;

        for (CtField ctField : ctClass.getDeclaredFields()) {
            Object[] annotations = ctField.getAnnotations();
            for (Object info : annotations) {
                String propertyName = ctField.getName().substring(0, 1).toUpperCase() + ctField.getName().substring(1);
                String methodName = "set" + propertyName;

                if (info.toString().contains("@play.modules.neo4j.annotation.Neo4jStartNode")
                        && Modifier.isPublic(ctField.getModifiers())) {
                    Logger.debug("Field " + ctField.getName() + " is the Neo4jStartNode");
                    //@formatter:off
                    String code = "public void " + methodName + "(" + ctField.getType().getName() + " value) { " +
                                        "this." + ctField.getName() + " = value;" + 
                                        "this.setStartNode(value);" + 
                                  "}";
                    //@formatter:on
                    Logger.debug(code);
                    createMethod(ctClass, entityName, code, methodName);
                    isStartNodeDefined = true;
                }
                else {
                    if (info.toString().contains("@play.modules.neo4j.annotation.Neo4jEndNode")
                            && Modifier.isPublic(ctField.getModifiers())) {
                        Logger.debug("Field " + ctField.getName() + " is the Neo4jEndNode");
                        //@formatter:off
                        String code = "public void " + methodName + "(" + ctField.getType().getName() + " value) { "
                                + "this." + ctField.getName() + " = value;" + "this.setEndNode(value);" + "}";
                        createMethod(ctClass, entityName, code, methodName);
                        Logger.debug(code);
                        //@formatter:on
                        isEndNodeDefined = true;
                    }
                }
            }
        }

        if (!isStartNodeDefined) {
            throw new Neo4jPlayException("Please annotate in your class " + ctClass.getName()
                    + " a public field with @Neo4jStartNode");
        }
        if (!isEndNodeDefined) {
            throw new Neo4jPlayException("Please annotate in your class " + ctClass.getName()
                    + " a public field with @Neo4jEndNode");
        }

        // ~~~~~~~~~~
        // Adding getByKey() method
        //@formatter:off
        String codeGetByKey = "public static play.modules.neo4j.relationship.Neo4jRelationship getByKey(Long key) {" + 
                            "return (" + entityName + ") _getByKey(key, \"" + entityName + "\");" + 
                      "}";
        //@formatter:on
        createMethod(ctClass, entityName, codeGetByKey, "getByKey");

        // Done.
        applicationClass.enhancedByteCode = ctClass.toBytecode();
        ctClass.defrost();
    }

    /**
     * Helper method to add a method with javassist, also if t is already declared !
     * 
     * @param ctClass
     * @param entityName
     * @param code
     * @param methodName
     * @throws CannotCompileException
     */
    protected void createMethod(CtClass ctClass, String entityName, String code, String methodName)
            throws CannotCompileException {
        try {
            CtMethod ctMethod = ctClass.getDeclaredMethod(methodName);
            if (ctMethod != null) {
                ctClass.removeMethod(ctMethod);
                throw new NotFoundException("it's not a true " + methodName + " !");
            }
        } catch (NotFoundException noGetter) {
            addMethod(ctClass, entityName, code, methodName);
        }
    }

    /**
     * Helper method to add a method with javassist.
     * 
     * @param ctClass
     * @param entityName
     * @param codeFindAllByKey
     * @param methodName
     * @throws CannotCompileException
     */
    protected void addMethod(CtClass ctClass, String entityName, String codeFindAllByKey, String methodName)
            throws CannotCompileException {
        Logger.debug("Adding " + methodName + "() method for class " + entityName);
        CtMethod findAllMethod = CtMethod.make(codeFindAllByKey, ctClass);
        ctClass.addMethod(findAllMethod);
    }

}
