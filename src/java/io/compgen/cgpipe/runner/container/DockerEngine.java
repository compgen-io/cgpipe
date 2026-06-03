package io.compgen.cgpipe.runner.container;

import java.util.List;

final class DockerEngine implements ContainerEngine {

	@Override
	public String render(String image, List<String> mounts, String workingDir,
	                     List<String> envPassThrough, boolean userMap,
	                     List<String> extraOpts, String bodyVar) {
		StringBuilder sb = new StringBuilder();
		sb.append("docker run --rm");
		for (String mount : mounts) {
			sb.append(" \\\n    -v ").append(mount).append(":").append(mount);
		}
		if (workingDir != null && !workingDir.isEmpty()) {
			sb.append(" \\\n    -w ").append(workingDir);
		}
		if (userMap) {
			sb.append(" \\\n    -u $(id -u):$(id -g)");
		}
		if (envPassThrough != null) {
			for (String env : envPassThrough) {
				sb.append(" \\\n    -e ").append(env);
			}
		}
		if (extraOpts != null) {
			for (String opt : extraOpts) {
				sb.append(" \\\n    ").append(opt);
			}
		}
		sb.append(" \\\n    ").append(normalizeImage(image));
		sb.append(" \\\n    bash \"$").append(bodyVar).append("\"");
		return sb.toString();
	}

	@Override
	public String normalizeImage(String image) {
		// Docker takes references as-is.
		return image;
	}
}
