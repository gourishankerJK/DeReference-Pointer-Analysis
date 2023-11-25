package utils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import soot.tagkit.AttributeValueException;
import soot.tagkit.Tag;

public class CustomTag implements Tag {
    Map<String, List<String>> p;
    String name = "CustomTag";
    int lineNumber = 0;
    String stringTag = "Empty";

    public CustomTag(String tagName, Map<String, List<String>> test) {
        this.name = tagName;
        this.p = new HashMap<>(test);

    }

    public CustomTag(String tagName, String callId, String test) {
        this.name = tagName;
        Map<String, List<String>> m = new HashMap<>();
        m.put(callId, Arrays.asList(test));
        this.p = m;
    }

     public CustomTag(String tagName, int lineNumber) {
        this.name = tagName;
        this.lineNumber = lineNumber;
    }

     public CustomTag(String tagName, String stringTag) {
        this.name = tagName;
        this.stringTag = stringTag;
    }

    public void UpdateMapTag(String callId, String test) {
        if (this.p.get(callId) != null) {
            this.p.get(callId).add(test);
        } else {
            this.p.put(callId, Arrays.asList(test));
        }
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public byte[] getValue() throws AttributeValueException {
        return this.p.toString().getBytes();
    }

    public List<String> getHashMapTag(String value) {
        return this.p.getOrDefault(value, Arrays.asList());
    }

    public Map<String, List<String>> getWholeHashMap() {
        return this.p;
    }

    public int getLineNumber() {
        return this.lineNumber;
    }

    public String getStringTag() {
        return this.stringTag;
    }

    @Override
    public String toString() {
        String s = this.p == null ? "Null" : this.p.toString();
        return "{" + this.name + " " + s + "  , lineNumberTag " + this.lineNumber + ", StringTag " + this.stringTag
                + " }";
    }

}