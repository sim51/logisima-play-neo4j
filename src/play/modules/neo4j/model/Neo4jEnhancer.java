package play.modules.neo4j.model;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import play.Logger;
import play.classloading.enhancers.Enhancer;

public abstract class Neo4jEnhancer extends Enhancer {
    protected void createMethod(CtClass ctClass, String entityName, String code, String methodName) throws CannotCompileException {
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

    protected void addMethod(CtClass ctClass, String entityName, String codeFindAllByKey, String methodName) throws CannotCompileException {
        Logger.debug("######## Adding " + methodName + "() method for class " + entityName);
        //Logger.debug(codeFindAllByKey);
        CtMethod findAllMethod = CtMethod.make(codeFindAllByKey, ctClass);
        ctClass.addMethod(findAllMethod);
    }


}
