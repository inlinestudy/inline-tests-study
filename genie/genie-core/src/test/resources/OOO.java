package test;

import org.symphonyoss.symphony.messageml.exceptions.InvalidInputException;
import java.util.Collection;
import java.util.List;

public class OOO {

    void assertContainsChildOfType(Collection<Class<? extends OOO>> elementTypes) throws InvalidInputException {
        boolean hasPermittedElementAsChild = this.getChildren().stream()
                .anyMatch(element -> elementTypes.contains(element.getClass()));
    }

    public List<OOO> getChildren() {
        return children;
    }
}
