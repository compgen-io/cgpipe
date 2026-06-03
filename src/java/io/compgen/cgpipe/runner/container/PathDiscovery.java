package io.compgen.cgpipe.runner.container;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Derive the minimal set of host directories that need to be bind-mounted into a container
 * so that the rendered job body can read every file it references.
 *
 * Sources:
 *   1. The working directory (always).
 *   2. The parent directory of every declared input file.
 *   3. The parent directory of every declared output file.
 *   4. Any absolute path the regex scanner finds in the body (after CGPipe substitution).
 *   5. The container body_dir (where the temp script lives) and any explicit user binds.
 *
 * The combined set is collapsed (children of an included parent are dropped) and filtered
 * through a small system-path denylist so we don't try to mount /usr or /etc.
 */
public final class PathDiscovery {

	private PathDiscovery() {}

	private static final Set<String> DENYLIST = new HashSet<String>(Arrays.asList(
		"/", "/bin", "/sbin", "/usr", "/lib", "/lib64",
		"/etc", "/var", "/proc", "/sys", "/dev", "/boot", "/root"
	));

	// Match absolute paths in a shell body. The negative lookbehind avoids picking the
	// middle of `a/b/c` or `host:/tmp` or `key=/path`. The character class is permissive
	// because filenames can contain a wide variety of characters.
	private static final Pattern ABS_PATH = Pattern.compile(
		"(?<![\\w./:=-])(/[\\w./@+%-]+)"
	);

	/**
	 * Collect candidate paths from a shell body. Returns absolute paths *as written* (not
	 * the directories — the caller decides whether to mount the file or its parent).
	 */
	public static Set<String> scanBody(String body) {
		Set<String> found = new LinkedHashSet<String>();
		if (body == null) {
			return found;
		}
		Matcher m = ABS_PATH.matcher(body);
		while (m.find()) {
			found.add(m.group(1));
		}
		return found;
	}

	/**
	 * Resolve mount points from two sources with different semantics.
	 *
	 * @param directDirs paths the caller knows are directories (working directory, body_dir,
	 *                   explicit user binds). Used as-is even if they don't exist on the
	 *                   submit host at render time.
	 * @param discovered paths discovered from inputs/outputs/body-scan. May be file paths
	 *                   (mount their parent) or directory paths (mount as-is). Relative
	 *                   paths are skipped — they resolve against the wd, which is already
	 *                   in directDirs.
	 */
	public static List<String> resolveMounts(Collection<String> directDirs,
	                                         Collection<String> discovered) {
		Set<String> dirs = new LinkedHashSet<String>();
		for (String d : directDirs) {
			if (d == null || d.isEmpty() || !new File(d).isAbsolute()) {
				continue;
			}
			String mount = trimTrailingSlash(d);
			if (!isDenylisted(mount)) {
				dirs.add(mount);
			}
		}
		for (String p : discovered) {
			if (p == null || p.isEmpty() || !new File(p).isAbsolute()) {
				continue;
			}
			File f = new File(p);
			String mount;
			if (f.exists() && f.isDirectory()) {
				mount = trimTrailingSlash(p);
			} else {
				mount = parentDir(p);
			}
			if (mount == null || mount.isEmpty() || isDenylisted(mount)) {
				continue;
			}
			dirs.add(mount);
		}
		return collapse(dirs);
	}

	private static String parentDir(String path) {
		File f = new File(path);
		File parent = f.getParentFile();
		if (parent == null) {
			return null;
		}
		String s = parent.getPath();
		return trimTrailingSlash(s);
	}

	private static String trimTrailingSlash(String s) {
		if (s.length() > 1 && s.endsWith("/")) {
			return s.substring(0, s.length() - 1);
		}
		return s;
	}

	/**
	 * Whether mounting {@code dir} would expose a denylisted prefix. {@code /usr} is
	 * denylisted directly; {@code /usr/share/foo} is denylisted because it's under /usr.
	 */
	private static boolean isDenylisted(String dir) {
		if (DENYLIST.contains(dir)) {
			return true;
		}
		for (String deny : DENYLIST) {
			if (deny.equals("/")) {
				// Don't reject everything just because everything starts with "/".
				continue;
			}
			if (dir.startsWith(deny + "/")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Drop any directory that is a child (proper or equal) of another entry in the set.
	 * Preserves input order otherwise.
	 */
	static List<String> collapse(Collection<String> dirs) {
		List<String> sorted = new ArrayList<String>(new LinkedHashSet<String>(dirs));
		// Sort shortest-first so parents come before their children. Lexicographic order
		// over normalized absolute paths gives parents-first by length, then alphabetical.
		Collections.sort(sorted, new java.util.Comparator<String>() {
			@Override
			public int compare(String a, String b) {
				int la = a.length();
				int lb = b.length();
				if (la != lb) {
					return la - lb;
				}
				return a.compareTo(b);
			}
		});
		List<String> result = new ArrayList<String>();
		for (String d : sorted) {
			boolean covered = false;
			for (String existing : result) {
				if (d.equals(existing) || d.startsWith(existing + "/")) {
					covered = true;
					break;
				}
			}
			if (!covered) {
				result.add(d);
			}
		}
		return result;
	}
}
