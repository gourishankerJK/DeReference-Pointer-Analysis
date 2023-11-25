import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import soot.tagkit.AttributeValueException;
import soot.tagkit.Tag;

public class HashMapTag implements Tag {
    Map<String, List<String>> p;

    HashMapTag(Map<String, List<String>> test) {
        this.p = test;
    }

    HashMapTag(String callId, String test) {
        Map<String, List<String>> m = new HashMap();
        m.put(callId, Arrays.asList(test));
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
        return this.p.toString();
    }

    @Override
    public byte[] getValue() throws AttributeValueException {
        return this.p.toString().getBytes();
    }

    public List<String> getTag(String value) {
        return this.p.getOrDefault(value, Arrays.asList());
    }

}