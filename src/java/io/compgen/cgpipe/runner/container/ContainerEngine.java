package io.compgen.cgpipe.runner.container;

import java.util.List;

/**
 * Renders the engine-specific portion of a wrapped job body: the command that takes
 * the temp body file (written by the wrapper preamble) and executes it inside a container.
 * Implementations differ only in command name and flag syntax (docker {@code -v}, singularity
 * {@code -B}, etc).
 */
interface ContainerEngine {

	/**
	 * @param image          The image reference, normalised per engine (e.g. singularity
	 *                       gets a {@code docker://} prefix for bare Docker Hub references).
	 * @param mounts         Host directories to bind into the container. Always non-empty —
	 *                       the wrapper guarantees at least the body_dir and wd are included.
	 * @param workingDir     Absolute path to set as the container's working directory.
	 * @param envPassThrough Names of host environment variables to pass through.
	 * @param userMap        When true and the engine supports it, map the container user
	 *                       to the host UID/GID (docker only).
	 * @param extraOpts      Engine-specific raw flags appended verbatim before the image.
	 * @param shell          Shell binary used to execute the body inside the container
	 *                       (e.g. {@code "sh"}, {@code "bash"}).
	 * @param gpuSpec        Resolved GPU spec ({@code "1"}, {@code "2"}, {@code "all"},
	 *                       {@code "device=0,1"}, …) or {@code null} for "no GPU." Engines
	 *                       that don't support granular control (singularity) treat any
	 *                       non-null value as "enable GPU access."
	 * @param bodyVar        Shell variable name (without leading {@code $}) that holds the
	 *                       path to the temp body file written by the wrapper preamble.
	 */
	String render(String image, List<String> mounts, String workingDir,
	              List<String> envPassThrough, boolean userMap,
	              List<String> extraOpts, String shell, String gpuSpec, String bodyVar);

	/**
	 * Engine-specific normalisation of an image reference. Most engines accept the raw string;
	 * singularity prepends {@code docker://} to Docker Hub-style references so they pull
	 * automatically.
	 */
	String normalizeImage(String image);
}
