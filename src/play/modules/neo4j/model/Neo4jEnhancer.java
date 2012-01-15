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

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import play.Logger;
import play.classloading.enhancers.Enhancer;

public abstract class Neo4jEnhancer extends Enhancer {

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

    protected void addMethod(CtClass ctClass, String entityName, String codeFindAllByKey, String methodName)
            throws CannotCompileException {
        Logger.debug("######## Adding " + methodName + "() method for class " + entityName);
        // Logger.debug(codeFindAllByKey);
        CtMethod findAllMethod = CtMethod.make(codeFindAllByKey, ctClass);
        ctClass.addMethod(findAllMethod);
    }

}
