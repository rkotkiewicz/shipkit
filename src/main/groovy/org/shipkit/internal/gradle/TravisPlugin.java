package org.shipkit.internal.gradle;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.shipkit.gradle.ReleaseNeededTask;
import org.shipkit.gradle.git.IdentifyGitBranchTask;
import org.shipkit.internal.gradle.configuration.BasicValidator;
import org.shipkit.internal.gradle.configuration.LazyConfiguration;
import org.shipkit.internal.gradle.git.GitBranchPlugin;
import org.shipkit.internal.gradle.util.StringUtil;

import static org.shipkit.internal.gradle.GitSetupPlugin.CHECKOUT_TASK;
import static org.shipkit.internal.gradle.git.GitBranchPlugin.IDENTIFY_GIT_BRANCH;

/**
 * Configures the release automation to be used with Travis CI.
 * <ul>
 * <li>Configures {@link GitBranchPlugin}/{@link IdentifyGitBranchTask}
 *      so that the branch information is taken from 'TRAVIS_BRANCH' env variable.</li>
 * <li>Configures {@link GitSetupPlugin}/{@link GitCheckOutTask}
 *      so that it checks out the branch specified in env variable.</li>
 * <li>Configures {@link ReleaseNeededPlugin}/{@link ReleaseNeededTask}
 *      so that it uses information from 'TRAVIS_PULL_REQUEST' and 'TRAVIS_COMMIT_MESSAGE' env variables.</li>
 * </ul>
 */
public class TravisPlugin implements Plugin<Project> {

    private final static Logger LOG = Logging.getLogger(TravisPlugin.class);

    @Override
    public void apply(final Project project) {
        project.getPlugins().apply(GitSetupPlugin.class);

        final String branch = System.getenv("TRAVIS_BRANCH");
        LOG.info("Branch from 'TRAVIS_BRANCH' env variable: {}", branch);

        project.getPlugins().withType(GitBranchPlugin.class, new Action<GitBranchPlugin>() {
            @Override
            public void execute(GitBranchPlugin p) {
                IdentifyGitBranchTask identifyBranch = (IdentifyGitBranchTask) project.getTasks().getByName(IDENTIFY_GIT_BRANCH);
                if(!StringUtil.isEmpty(branch)) {
                    identifyBranch.setBranch(branch);
                }
            }
        });

        project.getPlugins().withType(GitSetupPlugin.class, new Action<GitSetupPlugin>() {
            @Override
            public void execute(GitSetupPlugin p) {
                final GitCheckOutTask checkout = (GitCheckOutTask) project.getTasks().getByName(CHECKOUT_TASK);
                checkout.setRev(branch);
                LazyConfiguration.lazyConfiguration(checkout, new Runnable() {
                    public void run() {
                        BasicValidator.notNull(checkout.getRev(),
                                "Task " + checkout.getPath() + " does not know the target revision to check out.\n" +
                                        "In Travis CI builds, it is automatically configured from 'TRAVIS_BRANCH' environment variable.\n" +
                                        "If you are trying to run this task outside Travis, you can export the environment variable.\n" +
                                        "Alternatively, you can set the task's 'rev' property explicitly.");
                    }
                });
            }
        });

        project.getPlugins().withType(ReleaseNeededPlugin.class, new Action<ReleaseNeededPlugin>() {
            public void execute(ReleaseNeededPlugin p) {
                String pr = System.getenv("TRAVIS_PULL_REQUEST");
                LOG.info("Pull request from 'TRAVIS_PULL_REQUEST' env variable: {}", pr);
                final boolean isPullRequest = pr != null && !pr.trim().isEmpty() && !pr.equals("false");
                LOG.info("Pull request build: {}", isPullRequest);

                project.getTasks().withType(ReleaseNeededTask.class, new Action<ReleaseNeededTask>() {
                    public void execute(ReleaseNeededTask t) {
                        t.setCommitMessage(System.getenv("TRAVIS_COMMIT_MESSAGE"));
                        t.setPullRequest(isPullRequest);
                    }
                });
            }
        });
    }
}
