import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;

public class Main {
    public static void main(String[] args) throws XmlSerializingException,
            IllegalAccessException,
            InvocationTargetException {
        try {

        XmlSerializer xmlSerializer = new XmlSerializer();
        Person person = new Child("Sergey", "RUS", 32);
        Document document = xmlSerializer.serializeObjectAsClass(Child.class, person);
        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setSuppressDeclaration(true);
        XMLWriter writer;
        writer = new XMLWriter(new FileWriter("result.txt"), format);
        writer.write(document);
        writer.close();
        } catch (IOException | InstantiationException | ClassIncompatibilityException e) {
            e.printStackTrace();
        }
    }
}
