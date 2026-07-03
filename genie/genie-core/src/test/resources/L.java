package test;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class L {
    @JsonProperty("dependsOn")
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "dependency")
    private List<L> dependencies;

    public void addDependency(final L dependency) {
        if (dependencies == null) {
            dependencies = new ArrayList<>();
        }
        boolean found = dependencies.stream().anyMatch(d -> d.getRef().equals(dependency.getRef()));
        if (!found) {
            dependencies.add(dependency);
        }
    }
}
