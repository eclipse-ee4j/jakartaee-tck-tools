package tck.conversion.ant.st4;

import org.stringtemplate.v4.Interpreter;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.misc.ObjectModelAdaptor;
import org.stringtemplate.v4.misc.STNoSuchPropertyException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;

/**
 * st4 does not natively support records, so this adaptor add support
 * @param <R> record class
 */
public class RecordAdaptor<R extends Record> extends ObjectModelAdaptor<R> {
    public Object getProperty(Interpreter interpreter, ST self, R rtype, Object property, String propertyName)
            throws STNoSuchPropertyException {
        RecordComponent[] components = rtype.getClass().getRecordComponents();
        for (RecordComponent component : components) {
            if (component.getName().equals(propertyName)) {
                try {
                    return component.getAccessor().invoke(rtype);
                } catch (IllegalAccessException|InvocationTargetException e) {
                    throw new STNoSuchPropertyException(e, rtype, propertyName);
                }
            }
        }
        return super.getProperty(interpreter,self,rtype,property,propertyName);
    }
}
