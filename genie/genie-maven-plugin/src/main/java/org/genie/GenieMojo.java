package org.genie;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/** A wrapper Mojo class for the entire Genie process. */
@Mojo(name = "genie", requiresDependencyResolution = ResolutionScope.TEST)
public class GenieMojo extends ReducerMojo {
    /** Executes the entire Genie process. */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();
    }
}
