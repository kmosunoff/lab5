import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Iterator;

public class XmlSerializer {
    public XmlSerializer() {
    }

    private String ignoreGet(String string) {
        if (string.startsWith("get")) {
            return string.substring(3);
        }
        return string;
    }

    private void checkIfMethodIsCorrect(Method method) throws XmlSerializingException {
        if (method.getParameterCount() != 0) {
            throw new XmlSerializingException("The method " + method.getName() + "has some parameters!");
        }
        if (method.getReturnType() == void.class) {
            throw new XmlSerializingException("The method " + method.getName() + "returns void!");
        }
    }

    private Element createTagFromField(Element root, Object object, Field field, XmlTag xmlTag) throws XmlSerializingException,
            IllegalAccessException,
            InvocationTargetException, InstantiationException, ClassIncompatibilityException {
        if (xmlTag != null) {
            Element element = DocumentHelper.createElement(xmlTag.name().equals("")
                    ? ignoreGet(field.getName())
                    : xmlTag.name());
            field.setAccessible(true);
            if (findXmlTag(root, element.getName()) != null) {
                throw new XmlSerializingException("Repeating tags " + element.getName());
            }
            root.add(element);
            if (field.getType().getAnnotation(XmlObject.class) != null) {
                if (field.get(object) != null) {
                    element.add(serialize(field.getType(), field.get(object)));
                }
                else {
                    element.setText("null");
                }
            }
            else {
                element.setText(field.get(object) == null
                        ? "null"
                        : field.get(object).toString());
            }
            return element;
        }
        return null;
    }

    private Element createTagFromMethod(Element root, Object object, Method method, XmlTag xmlTag) throws XmlSerializingException,
            IllegalAccessException,
            InvocationTargetException, InstantiationException, ClassIncompatibilityException {
        if (xmlTag != null) {
            method.setAccessible(true);
            checkIfMethodIsCorrect(method);
            Element element = DocumentHelper.createElement(xmlTag.name().equals("")
                    ? ignoreGet(method.getName())
                    : xmlTag.name());
            if (findXmlTag(root, element.getName()) != null) {
                throw new XmlSerializingException("Repeating tags " + element.getName());
            }
            root.add(element);
            if (method.getReturnType().getAnnotation(XmlObject.class) != null) {
                if (method.invoke(object) != null) {
                    element.add(serialize(method.getReturnType(), method.invoke(object)));
                }
                else {
                    element.setText("null");
                }
            }
            else {
                element.setText(method.invoke(object) == null
                        ? "null"
                        : method.invoke(object).toString());
            }
            return element;
        }
        return null;
    }

    private void createAttribute(Element root, Object object, Member member, XmlAttribute xmlAttribute)
            throws XmlSerializingException,
            InvocationTargetException,
            IllegalAccessException{
        if (xmlAttribute != null) {
            if (member instanceof Method) {
                checkIfMethodIsCorrect((Method) member);
            }
            String tagName = xmlAttribute.tag().equals("")
                    ? root.getName()
                    : xmlAttribute.tag();
            Element element = findXmlTag(root, tagName);
            if (element == null) {
                throw new XmlSerializingException("There's no " + tagName + " tag");
            }
            else {
                String attributeValue;
                if (member instanceof Field) {
                    attributeValue = ((Field) member).get(object) == null
                            ? "null"
                            : ((Field) member).get(object).toString();
                }
                else {
                    attributeValue = ((Method) member).invoke(object) == null
                            ? "null"
                            : ((Method) member).invoke(object).toString();
                }
                element.addAttribute(member.getName(), attributeValue);
            }
        }
    }

    private Element findXmlTag(Element inputElement, String tagName) {
        if (inputElement.getName().equals(tagName)) {
            return inputElement;
        }
        for (Iterator iterator = inputElement.elementIterator(); iterator.hasNext(); ) {
            Element element = (Element) iterator.next();
            if (element.getName().equals(tagName))
                return element;
        }
        return null;
    }

    public Document serializeObjectAsClass(Class clazz, Object object) throws InvocationTargetException, ClassIncompatibilityException, InstantiationException, IllegalAccessException, XmlSerializingException {
        Document document = DocumentHelper.createDocument();
        document.add(serialize(clazz, object));
        return document;
    }

    private Element serialize(Class clazz, Object object) throws XmlSerializingException,
            IllegalAccessException,
            InvocationTargetException,
            InstantiationException, ClassIncompatibilityException {
        if (object == null) {
            throw new XmlSerializingException("Object must not be null");
        }
        if (clazz.isAssignableFrom(object.getClass())) {
            if (clazz.getAnnotation(XmlObject.class) != null) {
                Element root = DocumentHelper.createElement(clazz.getSimpleName());
                Element serializedSuperClass = serialize(clazz.getSuperclass(), object);
                if (serializedSuperClass != null) {
                    root.add(serializedSuperClass);
                }
                for (Field field : clazz.getDeclaredFields()) {
                    field.setAccessible(true);
                    XmlTag xmlTag = field.getAnnotation(XmlTag.class);
                    XmlAttribute xmlAttribute = field.getAnnotation(XmlAttribute.class);

                    if (xmlTag != null && xmlAttribute != null) {
                        throw new XmlSerializingException("Both @XmlTag and @XmlAttribute annotations for " + field.getName());
                    }

                    createTagFromField(root, object, field, xmlTag);

                }
                for (Method method : clazz.getDeclaredMethods()) {
                    method.setAccessible(true);
                    XmlTag xmlTag = method.getAnnotation(XmlTag.class);
                    XmlAttribute xmlAttribute = method.getAnnotation(XmlAttribute.class);

                    if (xmlTag != null && xmlAttribute != null) {
                        throw new XmlSerializingException("Both @XmlTag and @XmlAttribute annotations for " + method.getName());
                    }

                    createTagFromMethod(root, object, method, xmlTag);
                }

                for (Field field : clazz.getDeclaredFields()) {
                    field.setAccessible(true);
                    XmlAttribute xmlAttribute = field.getAnnotation(XmlAttribute.class);
                    if (xmlAttribute != null) {
                        createAttribute(root, object, field, xmlAttribute);
                    }
                }
                for (Method method : clazz.getDeclaredMethods()) {
                    method.setAccessible(true);
                    XmlAttribute xmlAttribute = method.getAnnotation(XmlAttribute.class);
                    if (xmlAttribute != null) {
                        createAttribute(root, object, method, xmlAttribute);
                    }
                }
                return root;
            }
            return null;
        }
        else {
            throw new ClassIncompatibilityException("Given object is not an instance of " + clazz);
        }
    }

}
