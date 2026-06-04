package io.compgen.cgpipe.runner.container;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import io.compgen.cgpipe.parser.context.RootContext;
import io.compgen.cgpipe.parser.variable.VarValue;
import io.compgen.cgpipe.runner.JobDef;

/**
 * Wraps a job body so it executes inside a container (Docker / Singularity).
 *
 * Wiring point: callers (TemplateRunner, JobRunner.shexec, ShellScriptRunner) ask
 * {@link #maybeWrap(JobDef, RootContext)} for the body. When containers are configured
 * for the run AND the job declares an image, the body is wrapped; otherwise the original
 * body is returned unchanged so non-container pipelines pay nothing.
 */
public final class ContainerWrapper {

	private static final String HEREDOC_MARKER_BASE = "__CGPIPE_BODY__";
	private static final String DEFAULT_BODY_DIR = "/tmp";
	// Default container shell. sh is almost universally available; users with bash-specific
	// body syntax opt up via cgpipe.container.shell / job.container.shell.
	private static final String DEFAULT_SHELL = "sh";

	private ContainerWrapper() {}

	/**
	 * Wrap (or return as-is) the body for a caller that will substitute the result into a
	 * CGPipe template via {@code ${job._body}} and let {@link io.compgen.cgpipe.parser.Eval}
	 * re-process it. The injected shell `$(...)` substitutions are escaped so they survive
	 * {@code evalStringShell}; the user body inside the HEREDOC is left untouched (it has
	 * no `$(...)` of its own — those were resolved at body-capture time).
	 */
	public static String maybeWrapForTemplate(JobDef jobdef, RootContext globalCtx) {
		String wrapped = maybeWrap(jobdef, globalCtx);
		if (wrapped == null || wrapped.equals(jobdef.getBody())) {
			return wrapped;
		}
		return escapeForTemplate(wrapped);
	}

	/**
	 * Replace `$(` with `\$(` so it survives {@link io.compgen.cgpipe.parser.Eval#evalString}
	 * untouched and reaches the shell verbatim.
	 */
	static String escapeForTemplate(String body) {
		// Only `$(...)` is at risk — `$VAR` (no braces) is not handled by evalStringVar,
		// and `${VAR}` already requires a defined cgpipe variable so we don't generate it.
		return body.replace("$(", "\\$(");
	}

	/**
	 * @return the body to actually execute. If no engine / no image, this is exactly
	 *         {@code jobdef.getBody()}; otherwise it's the container-wrapped form. Raw shell
	 *         syntax — no template-engine escapes. Callers that pipe directly to bash
	 *         ({@link io.compgen.cgpipe.runner.JobRunner#shexec},
	 *         {@link io.compgen.cgpipe.runner.ShellScriptRunner}) use this directly;
	 *         template-runners use {@link #maybeWrapForTemplate} instead.
	 */
	public static String maybeWrap(JobDef jobdef, RootContext globalCtx) {
		String body = jobdef.getBody();
		if (body == null || body.isEmpty()) {
			return body;
		}

		String engineName = configString(globalCtx, "cgpipe.container.engine");
		String image = jobdef.getSetting("job.container");
		if (engineName == null || engineName.isEmpty() || image == null || image.isEmpty()) {
			return body;
		}

		ContainerEngine engine = engineFor(engineName);
		if (engine == null) {
			return body;
		}

		String bodyDir = jobdef.getSetting("cgpipe.container.body_dir");
		if (bodyDir == null) {
			bodyDir = configString(globalCtx, "cgpipe.container.body_dir");
		}
		if (bodyDir == null || bodyDir.isEmpty()) {
			bodyDir = DEFAULT_BODY_DIR;
		}
		// Resolve symlinks: on macOS /tmp is a symlink to /private/tmp; mktemp on the host
		// would write to the canonical path, but if we passed the symlinked path to mktemp
		// the resulting shell variable would also point at the symlink, and the docker mount
		// (which targets the canonical path) wouldn't expose that name inside the container.
		bodyDir = PathDiscovery.canonicalize(bodyDir);

		String workingDir = jobdef.getSetting("job.wd");
		if (workingDir == null || workingDir.isEmpty()) {
			try {
				workingDir = new File(".").getCanonicalPath();
			} catch (Exception e) {
				workingDir = ".";
			}
		}

		List<String> mounts = computeMounts(jobdef, globalCtx, body, bodyDir, workingDir);
		List<String> envList = combineList(globalCtx, jobdef, "cgpipe.container.env", "job.container.env");
		List<String> extraOpts = computeExtraOpts(globalCtx, jobdef, engineName);

		boolean userMap = configBool(globalCtx, "cgpipe.container.user_map", true);

		// Per-target overrides global, which overrides the built-in sh default.
		String shell = jobdef.getSetting("job.container.shell");
		if (shell == null || shell.isEmpty()) {
			shell = configString(globalCtx, "cgpipe.container.shell");
		}
		if (shell == null || shell.isEmpty()) {
			shell = DEFAULT_SHELL;
		}

		String marker = pickHeredocMarker(body);
		String bodyVar = "__cgpipe_body";

		// Emit raw shell `$(...)` here. Some callers (template runners) need these escaped
		// to `\$(...)` so they survive Eval.evalStringShell at template-substitution time;
		// they call escapeForTemplate() on the result. Direct-execution callers
		// (JobRunner.shexec, ShellScriptRunner) use the raw form and bash evaluates the
		// substitutions at job-run time.
		StringBuilder sb = new StringBuilder();
		sb.append(bodyVar).append("=$(mktemp \"").append(bodyDir).append("/cgpipe-body.XXXXXX\")\n");
		sb.append("trap 'rm -f \"$").append(bodyVar).append("\"' EXIT\n");
		sb.append("cat > \"$").append(bodyVar).append("\" <<'").append(marker).append("'\n");
		sb.append(body);
		if (!body.endsWith("\n")) {
			sb.append('\n');
		}
		sb.append(marker).append("\n");
		sb.append("\n");
		sb.append(engine.render(image, mounts, workingDir, envList, userMap, extraOpts, shell, bodyVar));
		sb.append("\n");
		return sb.toString();
	}

	// ------------------------------------------------------------------
	// Mount / option assembly
	// ------------------------------------------------------------------

	private static List<String> computeMounts(JobDef jobdef, RootContext globalCtx,
	                                          String body, String bodyDir, String workingDir) {
		// Directories we know are directories — mount them as-is. These don't have to
		// exist on the submit host at render time (they may live on a per-node filesystem
		// or be created by __setup__).
		Set<String> directDirs = new LinkedHashSet<String>();
		if (bodyDir != null && !bodyDir.isEmpty()) {
			directDirs.add(bodyDir);
		}
		if (workingDir != null && !workingDir.isEmpty()) {
			directDirs.add(workingDir);
		}
		directDirs.addAll(combineList(globalCtx, jobdef, "cgpipe.container.bind", "job.container.bind"));

		// Paths discovered from the body / inputs / outputs — these may be file paths
		// (mount their parent) or directory paths (mount them as-is).
		Set<String> discovered = new LinkedHashSet<String>();
		if (jobdef.getInputs() != null) {
			discovered.addAll(jobdef.getInputs());
		}
		if (jobdef.getOutputs() != null) {
			discovered.addAll(jobdef.getOutputs());
		}
		discovered.addAll(PathDiscovery.scanBody(body));

		return PathDiscovery.resolveMounts(directDirs, discovered);
	}

	private static List<String> computeExtraOpts(RootContext globalCtx, JobDef jobdef, String engineName) {
		List<String> opts = new ArrayList<String>();
		String engineLower = engineName.toLowerCase();
		// Engine-specific global opts.
		if (engineLower.equals("docker")) {
			opts.addAll(asList(globalCtx, "cgpipe.container.docker_opts"));
		} else if (engineLower.equals("singularity") || engineLower.equals("apptainer")) {
			opts.addAll(asList(globalCtx, "cgpipe.container.singularity_opts"));
		}
		// Per-target opts, applied last so they win.
		opts.addAll(asList(jobdef, "job.container.opts"));
		return opts;
	}

	// ------------------------------------------------------------------
	// Heredoc marker selection
	// ------------------------------------------------------------------

	private static String pickHeredocMarker(String body) {
		if (!body.contains(HEREDOC_MARKER_BASE)) {
			return HEREDOC_MARKER_BASE;
		}
		// Body contains the default marker — append a numeric suffix until it doesn't.
		int i = 1;
		while (true) {
			String candidate = HEREDOC_MARKER_BASE + "_" + i;
			if (!body.contains(candidate)) {
				return candidate;
			}
			i++;
		}
	}

	// ------------------------------------------------------------------
	// Engine lookup
	// ------------------------------------------------------------------

	private static ContainerEngine engineFor(String name) {
		String lower = name.toLowerCase();
		if (lower.equals("docker")) {
			return new DockerEngine();
		}
		if (lower.equals("singularity") || lower.equals("apptainer")) {
			return new SingularityEngine();
		}
		return null;
	}

	// ------------------------------------------------------------------
	// Settings helpers
	// ------------------------------------------------------------------

	private static String configString(RootContext cxt, String key) {
		if (cxt == null) {
			return null;
		}
		try {
			return cxt.getString(key);
		} catch (Exception e) {
			return null;
		}
	}

	private static boolean configBool(RootContext cxt, String key, boolean defval) {
		if (cxt == null || !cxt.contains(key)) {
			return defval;
		}
		try {
			VarValue v = cxt.get(key);
			return v.toBoolean();
		} catch (Exception e) {
			return defval;
		}
	}

	/**
	 * Concatenate a global list-valued setting with a per-target list-valued setting.
	 * Either may be missing or be a scalar (treated as a single-element list).
	 */
	private static List<String> combineList(RootContext globalCtx, JobDef jobdef,
	                                        String globalKey, String jobKey) {
		List<String> out = new ArrayList<String>();
		out.addAll(asList(globalCtx, globalKey));
		out.addAll(asList(jobdef, jobKey));
		return out;
	}

	private static List<String> asList(RootContext cxt, String key) {
		List<String> out = new ArrayList<String>();
		if (cxt == null || !cxt.contains(key)) {
			return out;
		}
		VarValue v = cxt.get(key);
		if (v == null) {
			return out;
		}
		if (v.isList()) {
			for (VarValue item : v.iterate()) {
				out.add(item.toString());
			}
		} else {
			out.add(v.toString());
		}
		return out;
	}

	private static List<String> asList(JobDef jobdef, String key) {
		List<String> out = new ArrayList<String>();
		if (jobdef == null || !jobdef.hasSetting(key)) {
			return out;
		}
		VarValue v = jobdef.getSettingsMap().get(key);
		if (v == null) {
			return out;
		}
		if (v.isList()) {
			for (VarValue item : v.iterate()) {
				out.add(item.toString());
			}
		} else {
			out.add(v.toString());
		}
		return out;
	}
}
