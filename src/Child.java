@XmlObject
public class Child extends Person {
    public Child(String name, String lang, int age) {
        super(name, lang, age);
    }

    @XmlTag
    private String test = "test1";
}
