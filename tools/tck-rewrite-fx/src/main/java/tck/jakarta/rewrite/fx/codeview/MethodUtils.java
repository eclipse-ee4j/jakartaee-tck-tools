package tck.jakarta.rewrite.fx.codeview;

import tck.jakarta.platform.ant.api.TestMethodInfo;

import java.lang.reflect.Method;
import java.util.Map;

public class MethodUtils {
    public static void resolveMethodThrows(Class<?> testBaseClass, Map<String, TestMethodInfo> methodInfos) {
        for(Method m : testBaseClass.getMethods()) {
            String methodName = m.getName();
            TestMethodInfo methodInfo = methodInfos.get(methodName);
            if(methodInfo != null) {
                StringBuilder sb = new StringBuilder();
                Class<?>[] etypes = m.getExceptionTypes();
                for (Class<?> etype : etypes) {
                    String typeName = etype.getName();
                    if(typeName.startsWith("java.lang.")) {
                        sb.append(etype.getSimpleName());
                    } else {
                        sb.append(typeName);
                    }
                    sb.append(", ");
                }
                if(sb.length() > 2) {
                    sb.setLength(sb.length()-2);
                }
                methodInfo.setThrowsException(sb.toString());
                //System.out.printf("Updated throws for %s to %s\n%s\n", methodName, sb, methodInfo);
            }
        }

    }
}
