package io.compgen.cgpipe.runner.container;

import java.util.List;

final class DockerEngine implements ContainerEngine {

	@Override
	public String render(String image, List<String> mounts, String workingDir,
	                     List<String> envPassThrough, boolean userMap,
	                     List<String> extraOpts, String shell, String gpuSpec, String bodyVar) {
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
		if (gpuSpec != null && !gpuSpec.isEmpty()) {
			// `--gpus 1` means "1 GPU"; `--gpus all` is the convenience meaning of the
			// integer/boolean inputs we get from cgpipe's `job.gpu = true` / `job.gpu = 1`.
			String spec = gpuSpec.equals("1") ? "all" : gpuSpec;
			sb.append(" \\\n    --gpus ").append(spec);
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
		sb.append(" \\\n    ").append(shell).append(" \"$").append(bodyVar).append("\"");
		return sb.toString();
	}

	@Override
	public String normalizeImage(String image) {
		// Docker takes references as-is.
		return image;
	}
}
