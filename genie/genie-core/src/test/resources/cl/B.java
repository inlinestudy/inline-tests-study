package cl;

public class B {
    public void addDependency(final Dependency dependency) {
        if (dependencies == null) {
            dependencies = new ArrayList<>();
        }
        boolean found = dependencies.stream().anyMatch(d -> d.getRef().equals(dependency.getRef()));
        if (!found) {
            dependencies.add(dependency);
        }
    }
}