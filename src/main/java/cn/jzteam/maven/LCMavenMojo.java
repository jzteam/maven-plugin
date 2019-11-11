package cn.jzteam.maven;

import com.google.common.base.Strings;
import com.google.common.io.CharStreams;
import com.google.common.io.LineProcessor;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 统计代码行数
 */
@Mojo(name = "lc", defaultPhase = LifecyclePhase.VERIFY)
public class LCMavenMojo extends AbstractMojo {
    private static final List DEFAULT_FILES = Arrays.asList("java", "xml", "properties");
    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    private File baseDir;
    @Parameter(defaultValue = "${project.build.sourceDirectory}", readonly = true)
    private File srcDir;
    @Parameter(defaultValue = "${project.build.testSourceDirectory}", readonly = true)
    private File testSrcDir;
    @Parameter(defaultValue = "${project.build.resources}", readonly = true)
    private List<Resource> resources;
    @Parameter(defaultValue = "${project.build.testResources}", readonly = true)
    private List<Resource> testResources;
    /**
     * 优先使用property指定配置
     * <properties>
     *     <lc.file.includes>xml,java</lc.file.includes>
     * </properties>
     *
     * 如果没有则使用默认设置，即：plugin为属性设置参数
     * <configuration>
     *     <includes>
     *         <include>xml</include>
     *         <include>java</include>
     *     </includes>
     * </configuration>
     */
    @Parameter(property = "lc.file.includes")
    private Set<String> includes = new HashSet<>();
    private Log logger = getLog();
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (includes.isEmpty()) {
            logger.debug("includes/lc.file.includes is empty!");
            includes.addAll(DEFAULT_FILES);
        }
        logger.info("includes: " + includes);
        try {
            long lines = 0;
            lines += countDir(srcDir);
            lines += countDir(testSrcDir);
            for (Resource resource : resources) {
                lines += countDir(new File(resource.getDirectory()));
            }
            for (Resource resource : testResources) {
                lines += countDir(new File(resource.getDirectory()));
            }
            logger.info("total lines: " + lines);
        } catch (IOException e) {
            logger.error("error: ", e);
            throw new MojoFailureException("execute failure: ", e);
        }
    }
    private LineProcessor<Long> lp = new LineProcessor<Long>() {
        private long line = 0;
        @Override
        public boolean processLine(String fileLine) throws IOException {
            if (!Strings.isNullOrEmpty(fileLine)) {
                ++this.line;
            }
            return true;
        }
        @Override
        public Long getResult() {
            long result = line;
            this.line = 0;
            return result;
        }
    };
    private long countDir(File directory) throws IOException {
        long lines = 0;
        if (directory.exists()) {
            Set<File> files = new HashSet<>();
            collectFiles(files, directory);
            for (File file : files) {
                lines += CharStreams.readLines(new FileReader(file), lp);
            }
            String path = directory.getAbsolutePath().substring(baseDir.getAbsolutePath().length());
            logger.info("path: " + path + ", file count: " + files.size() + ", total line: " + lines);
            logger.info("\t-> files: " + files.toString());
        }
        return lines;
    }
    private void collectFiles(Set files, File file) {
        if (file.isFile()) {
            String fileName = file.getName();
            int index = fileName.lastIndexOf(".");
            if (index != -1 && includes.contains(fileName.substring(index + 1))) {
                files.add(file);
            }
        } else {
            File[] subFiles = file.listFiles();
            for (int i = 0; subFiles != null && i < subFiles.length; ++i) {
                collectFiles(files, subFiles[i]);
            }
        }
    }
}
