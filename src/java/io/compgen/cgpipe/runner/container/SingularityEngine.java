package io.compgen.cgpipe.runner.container;

import java.util.List;

final class SingularityEngine implements ContainerEngine {

	@Override
	public String render(String image, List<String> mounts, String workingDir,
	                     List<String> envPassThrough, boolean userMap,
	                     List<String> extraOpts, String shell, String bodyVar) {
		// userMap is ignored — singularity already runs as the submitting user.
		StringBuilder sb = new StringBuilder();
		sb.append("singularity exec");
		for (String mount : mounts) {
			sb.append(" \\\n    -B ").append(mount).append(":").append(mount);
		}
		if (workingDir != null && !workingDir.isEmpty()) {
			sb.append(" \\\n    --pwd ").append(workingDir);
		}
		if (envPassThrough != null) {
			for (String env : envPassThrough) {
				// SINGULARITYENV_<NAME> is the canonical way to forward host env into the container.
				// Forwarding the *value* requires a host-side expansion; we pass through via the
				// engine's --env flag, which exists in modern singularity/apptainer.
				sb.append(" \\\n    --env ").append(env).append("=\"$").append(env).append("\"");
			}
		}
		if (extraOpts != null) {
			for (String opt : extraOpts) {
				sb.append(" \\\n    ").append(opt);
			}
		}
		sb.append(" \\\n    ").append(normalizeImage(image));
		sb.append(" \\\n    ").append(shell).append(" \"$").append(bodyVar).append("\"");
		return sb.toString();
	}

	@Override
	public String normalizeImage(String image) {
		// If the user already specified a scheme (docker://, library://, shub://, oras://,
		// http://, https://) or pointed at a local SIF file, leave it alone. Otherwise prepend
		// docker:// so a bare Docker Hub reference like "biocontainers/bwa:0.7.17" pulls from
		// Docker Hub automatically.
		if (image.contains("://")) {
			return image;
		}
		if (image.endsWith(".sif") || image.startsWith("/") || image.startsWith("./")) {
			return image;
		}
		return "docker://" + image;
	}
}
