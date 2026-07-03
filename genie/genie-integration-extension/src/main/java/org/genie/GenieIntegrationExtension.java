package org.genie;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;

import javax.inject.Named;
import javax.inject.Singleton;

@Named("genie")
@Singleton
public class GenieIntegrationExtension extends AbstractMavenLifecycleParticipant {
    public static final String GMP_GROUP_ID = "org.genie";
    public static final String GMP_ARTIFACT_ID = "genie-maven-plugin";
    public static final String GMP_VERSION = "1.0-SNAPSHOT";
    public static final String INLINETEST_GROUP_ID = "org.inlinetest";
    public static final String INLINETEST_ARTIFACT_ID = "inlinetest";
    public static final String INLINETEST_VERSION = "1.0";
    public static final String RANINLINE_GROUP_ID = "org.raninline";
    public static final String RANINLINE_ARTIFACT_ID = "raninline";
    public static final String RANINLINE_VERSION = "1.0-SNAPSHOT";

    @Override
    public void afterSessionStart(MavenSession session) throws MavenExecutionException {}

    private void insertGMPPlugin(MavenProject project) {
        boolean hasGenie = false;
        for (Plugin plugin : project.getBuildPlugins()) {
            if (plugin.getArtifactId().equals(GMP_ARTIFACT_ID)) {
                hasGenie = true;
                break;
            }
        }
        if (!hasGenie) {
            System.out.println("Adding Genie Maven plugin.");
            Build build = project.getBuild();
            Plugin gmp = new Plugin();
            gmp.setGroupId(GMP_GROUP_ID);
            gmp.setArtifactId(GMP_ARTIFACT_ID);
            gmp.setVersion(GMP_VERSION);
            build.addPlugin(gmp);
        }
    }

    private void insertInlinetestDependency(MavenProject project) {
        boolean hasInlinetest = false;
        for (Dependency dependency : project.getDependencies()) {
            if (dependency.getArtifactId().equals(INLINETEST_ARTIFACT_ID)) {
                hasInlinetest = true;
                break;
            }
        }
        if (!hasInlinetest) {
            System.out.println("Adding Inlinetest dependency.");
            Model model = project.getModel();
            Dependency inlinetest = new Dependency();
            inlinetest.setGroupId(INLINETEST_GROUP_ID);
            inlinetest.setArtifactId(INLINETEST_ARTIFACT_ID);
            inlinetest.setVersion(INLINETEST_VERSION);
            model.addDependency(inlinetest);
        }
    }

    private void insertRaninlineDependency(MavenProject project) {
        boolean hasRaninline = false;
        for (Dependency dependency : project.getDependencies()) {
            if (dependency.getArtifactId().equals(RANINLINE_ARTIFACT_ID)) {
                hasRaninline = true;
                break;
            }
        }
        if (!hasRaninline) {
            System.out.println("Adding Raninline dependency.");
            Model model = project.getModel();
            Dependency raninline = new Dependency();
            raninline.setGroupId(RANINLINE_GROUP_ID);
            raninline.setArtifactId(RANINLINE_ARTIFACT_ID);
            raninline.setVersion(RANINLINE_VERSION);
            model.addDependency(raninline);
        }
    }

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        System.out.println("Running Genie integration extension to inject necessary dependencies.");
        for (MavenProject project : session.getProjects()) {
            if (project.getGroupId().equals(INLINETEST_GROUP_ID)
                    || project.getGroupId().equals(RANINLINE_GROUP_ID)
                    || project.getGroupId().equals(GMP_GROUP_ID)) {
                System.out.println("Skipping ITest/Raninline/Genie.");
                continue;
            }
            insertGMPPlugin(project);
            insertInlinetestDependency(project);
            insertRaninlineDependency(project);
        }
    }
}
