package test;

import java.util.List;
import java.util.stream.Collectors;

public class T {
    private List<SubscriptionField> fields;

    private List<String> compatFields;

    private void convertCompatFields() {
        if (compatFields != null && fields == null) {
            fields = compatFields.stream().map(SubscriptionField::new).collect(Collectors.toList());
        }
    }

    static class SubscriptionField extends AbstractFacebookType {
        String name;

        private SubscriptionField(String name) {
            this.name = name;
        }
    }
}
